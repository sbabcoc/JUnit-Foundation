package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
 * createTest} method.
 */
@SuppressWarnings("squid:S1118")
public class CreateTest {
    
    private static final Map<String, Object> TARGET_TO_RUNNER = new ConcurrentHashMap<>();
    private static final Map<String, Object> RUNNER_TO_TARGET = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTest.class);
    
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
        
        Object target = LifecycleHooks.callProxy(proxy);
        // apply parameter-based global timeout
        TimeoutUtils.applyTestTimeout(runner, target);
        
        if (null == TARGET_TO_RUNNER.put(toMapKey(target), runner)) {
            LOGGER.debug("testObjectCreated: {}", target);
            RUNNER_TO_TARGET.put(toMapKey(runner), target);
            
            for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
                watcher.testObjectCreated(target, runner);
            }
        }
        
        return target;
    }
    
    /**
     * Get the class runner associated with the specified instance.
     * 
     * @param target instance of JUnit test class
     * @return {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} for specified instance
     */
    static Object getRunnerForTarget(Object target) {
        return TARGET_TO_RUNNER.get(toMapKey(target));
    }
    
    /**
     * Get the JUnit test class instance for the specified class runner.
     * 
     * @param runner JUnit class runner
     * @return JUnit test class instance for specified runner
     */
    static Object getTargetForRunner(Object runner) {
        return RUNNER_TO_TARGET.get(toMapKey(runner));
    }
    
    /**
     * Release runner/target mappings.
     * 
     * @param runner JUnit class runner
     */
    static void releaseMappingsFor(Object runner) {
        Object target = RUNNER_TO_TARGET.remove(toMapKey(runner));
        if (target != null) {
            TARGET_TO_RUNNER.remove(toMapKey(target));
        }
    }
}
