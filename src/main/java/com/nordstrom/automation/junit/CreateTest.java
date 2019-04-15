package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Optional;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
 * createTest} method.
 */
@SuppressWarnings("squid:S1118")
public class CreateTest {
    
    private static final ServiceLoader<TestObjectWatcher> objectWatcherLoader;
    private static final Map<Object, Object> TARGET_TO_RUNNER = new ConcurrentHashMap<>();
    private static final Map<Object, Object> RUNNER_TO_TARGET = new ConcurrentHashMap<>();
    private static final ThreadLocal<DepthGauge> DEPTH;
    
    static {
        objectWatcherLoader = ServiceLoader.load(TestObjectWatcher.class);
        DEPTH = new ThreadLocal<DepthGauge>() {
            @Override
            protected DepthGauge initialValue() {
                return new DepthGauge();
            }
        };
    }
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest createTest} method.
     * 
     * @param runner target {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} object
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - JUnit test class instance
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static Object intercept(@This final Object runner,
                    @SuperCall final Callable<?> proxy) throws Exception {
        
        Object testObj;
        try {
            DEPTH.get().increaseDepth();
            testObj = LifecycleHooks.callProxy(proxy);
        } finally {
            DEPTH.get().decreaseDepth();
        }
        
        TARGET_TO_RUNNER.put(testObj, runner);
        RUNNER_TO_TARGET.put(runner, testObj);
        
        if (DEPTH.get().atGroundLevel()) {
            synchronized(objectWatcherLoader) {
                for (TestObjectWatcher watcher : objectWatcherLoader) {
                    watcher.testObjectCreated(testObj, runner);
                }
            }
        }
        
        return testObj;
    }
    
    /**
     * Get the class runner associated with the specified instance.
     * 
     * @param target instance of JUnit test class
     * @return {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} for specified instance
     */
    static Object getRunnerForTarget(Object target) {
        return TARGET_TO_RUNNER.get(target);
    }
    
    /**
     * Get the JUnit test class instance for the specified class runner.
     * 
     * @param runner JUnit class runner
     * @return JUnit test class instance for specified runner
     */
    static Object getTargetForRunner(Object runner) {
        return RUNNER_TO_TARGET.get(runner);
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param <T> watcher type
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    @SuppressWarnings("unchecked")
    static <T extends JUnitWatcher> Optional<T> getAttachedWatcher(Class<T> watcherType) {
        if (TestObjectWatcher.class.isAssignableFrom(watcherType)) {
            synchronized(objectWatcherLoader) {
                for (TestObjectWatcher watcher : objectWatcherLoader) {
                    if (watcher.getClass() == watcherType) {
                        return Optional.of((T) watcher);
                    }
                }
            }
        }
        return Optional.absent();
    }
}
