package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;
import static com.nordstrom.automation.junit.LifecycleHooks.setFieldValue;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#createTestClass
 * createTestClass} method.
 */
@SuppressWarnings("squid:S1118")
public class CreateTestClass {
    private static final ServiceLoader<TestClassWatcher> classWatcherLoader;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTestClass.class);
    private static final Map<Object, TestClass> METHOD_TO_TESTCLASS = new ConcurrentHashMap<>();
    
    static {
        classWatcherLoader = ServiceLoader.load(TestClassWatcher.class);
    }
    
      /**
     * Interceptor for the {@link org.junit.runners.ParentRunner#createTestClass createTestClass} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @return new {@link TestClass} object
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static TestClass intercept(@This final Object runner, @SuperCall final Callable<?> proxy)
                    throws Exception {
        
        TestClass testClass = (TestClass) LifecycleHooks.callProxy(proxy);
        
        for (Object method : testClass.getAnnotatedMethods()) {
            METHOD_TO_TESTCLASS.put(method, testClass);
        }
        
        synchronized(classWatcherLoader) {
            for (TestClassWatcher watcher : classWatcherLoader) {
                watcher.testClassCreated(testClass, runner);
            }
        }
        
        attachRunnerScheduler(testClass, runner);
        return testClass;
    }
    
    /**
     * Attach lifecycle-reporting runner scheduler to the specified parent runner.
     * 
     * @param testClass {@link TestClass} object that was just created
     * @param runner {@link ParentRunner} for the specified test class
     */
    private static void attachRunnerScheduler(final TestClass testClass, final Object runner) {
        try {
            RunnerScheduler scheduler = getFieldValue(runner, "scheduler");
            setFieldValue(runner, "scheduler", createRunnerScheduler(testClass, runner, scheduler));
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            LOGGER.warn("Unable to attach notifying runner scheduler", e);
        }
    }
    
    /**
     * Create notifying runner scheduler, which forwards to the previous scheduler if specified.
     * 
     * @param testClass {@link TestClass} object that was just created
     * @param runner {@link ParentRunner} for the specified test class
     * @param scheduler runner scheduler that's currently attached to the specified runner (may be {@code null})
     * @return new notifying runner scheduler
     */
    private static RunnerScheduler createRunnerScheduler(final TestClass testClass,
                    final Object runner, final RunnerScheduler scheduler) {
        return new RunnerScheduler() {
            private AtomicBoolean scheduled = new AtomicBoolean(false);
            
            public void schedule(Runnable childStatement) {
                if (scheduled.compareAndSet(false, true)) {
                    synchronized(classWatcherLoader) {
                        for (TestClassWatcher watcher : classWatcherLoader) {
                            watcher.testClassStarted(testClass, runner);
                        }
                    }
                }
                
                RunReflectiveCall.fireTestStarted(childStatement);
                
                if (scheduler != null) {
                    scheduler.schedule(childStatement);
                } else {
                    childStatement.run();
                }
                
                RunReflectiveCall.fireTestFinished(runner);
            }

            public void finished() {
                synchronized(classWatcherLoader) {
                    for (TestClassWatcher watcher : classWatcherLoader) {
                        watcher.testClassFinished(testClass, runner);
                    }
                }
            }
        };
    }
    
    /**
     * Get the test class associated with the specified framework method.
     * 
     * @param method {@code FrameworkMethod} object
     * @return {@link TestClass} object associated with the specified framework method
     */
    static TestClass getTestClassWith(Object method) {
        TestClass testClass = METHOD_TO_TESTCLASS.get(method);
        if (testClass != null) {
            return testClass;
        }
        throw new IllegalArgumentException("No associated test class was found for specified framework method");
    }
}
