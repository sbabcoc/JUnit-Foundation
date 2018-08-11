package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.TestClass;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#createTestClass
 * createTestClass} method.
 */
@SuppressWarnings("squid:S1118")
public class CreateTestClass {
    static final ServiceLoader<TestClassWatcher> classWatcherLoader;
    static final Map<TestClass, Object> CLASS_TO_RUNNER = new ConcurrentHashMap<>();
    
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
    public static TestClass intercept(@This Object runner, @SuperCall Callable<?> proxy) throws Exception {
        TestClass testClass = (TestClass) proxy.call();
        CLASS_TO_RUNNER.put(testClass, runner);
        
        for (TestClassWatcher watcher : classWatcherLoader) {
            watcher.testClassCreated(testClass, runner);
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
            RunnerScheduler scheduler = LifecycleHooks.getFieldValue(runner, "scheduler");
            LifecycleHooks.setFieldValue(runner, "scheduler", createRunnerScheduler(testClass, runner, scheduler));
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
        }
    }
    
    /**
     * Create lifecycle-reporting runner scheduler, which forwards to the previous scheduler if specified.
     * 
     * @param testClass {@link TestClass} object that was just created
     * @param runner {@link ParentRunner} for the specified test class
     * @param scheduler runner scheduler that's currently attached to the specified runner (may be {@code null})
     * @return new lifecycle-reporting runner scheduler
     */
    private static RunnerScheduler createRunnerScheduler(final TestClass testClass, final Object runner, final RunnerScheduler scheduler) {
        final boolean isSuite = runner instanceof Suite;
        
        return new RunnerScheduler() {
            private AtomicBoolean scheduled = new AtomicBoolean(false);
            
            public void schedule(Runnable childStatement) {
                FrameworkMethod childMethod = null;
                
                if (scheduled.compareAndSet(false, true)) {
                    for (TestClassWatcher watcher : classWatcherLoader) {
                        watcher.testClassStarted(testClass, runner);
                    }
                }
                
                if (!isSuite) {
                    try {
                        childMethod = LifecycleHooks.getFieldValue(childStatement, "val$each");
                        for (TestClassWatcher watcher : classWatcherLoader) {
                            watcher.testStarted(childMethod, testClass);
                        }
                    } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                    }
                }
                
                if (scheduler != null) {
                    scheduler.schedule(childStatement);
                } else {
                    childStatement.run();
                }
                
                if (childMethod != null) {
                    for (TestClassWatcher watcher : classWatcherLoader) {
                        watcher.testFinished(childMethod, testClass);
                    }
                }
            }

            public void finished() {
                for (TestClassWatcher watcher : classWatcherLoader) {
                    watcher.testClassFinished(testClass, runner);
                }
            }
        };
    }
}