package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final ThreadLocal<Integer> COUNTER;
    private static final DepthGauge DEPTH;
    
    static {
        classWatcherLoader = ServiceLoader.load(TestClassWatcher.class);
        COUNTER = DepthGauge.getCounter();
        DEPTH = new DepthGauge(COUNTER);
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
        
        DEPTH.increaseDepth();
        TestClass testClass = (TestClass) LifecycleHooks.callProxy(proxy);
        DEPTH.decreaseDepth();
        
        if (DEPTH.atGroundLevel()) {
            for (Object method : testClass.getAnnotatedMethods()) {
                METHOD_TO_TESTCLASS.put(method, testClass);
            }
            
            synchronized(classWatcherLoader) {
                for (TestClassWatcher watcher : classWatcherLoader) {
                    watcher.testClassCreated(testClass, runner);
                }
            }
        }
        
        return testClass;
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
