package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
 * createTest} method.
 */
public class CreateTest {
    
    private static final Map<Integer, Object> HASHCODE_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<String, FrameworkMethod> TARGET_TO_METHOD = new ConcurrentHashMap<>();
    private static final Map<String, Object> TARGET_TO_RUNNER = new ConcurrentHashMap<>();
    private static final ThreadLocal<ConcurrentMap<Integer, DepthGauge>> METHOD_DEPTH;
    private static final Function<Integer, DepthGauge> NEW_INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTest.class);

    static {
        METHOD_DEPTH = new ThreadLocal<ConcurrentMap<Integer, DepthGauge>>() {
            @Override
            protected ConcurrentMap<Integer, DepthGauge> initialValue() {
                return new ConcurrentHashMap<>();
            }
        };
        NEW_INSTANCE = new Function<Integer, DepthGauge>() {
            @Override
            public DepthGauge apply(Integer input) {
                return new DepthGauge();
            }
        };
    }
    
    /**
     * Default constructor
     */
    public CreateTest() { }
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest createTest} method.
     * 
     * @param runner target {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} object
     * @param method {@link FrameworkMethod} for which this test class instance is being created
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - JUnit test class instance
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static Object intercept(@This final Object runner, @Argument(0) final FrameworkMethod method,
                    @SuperCall final Callable<?> proxy) throws Exception {
        
        Integer hashCode = Objects.hash(runner, method.toString());
        DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(METHOD_DEPTH.get(), hashCode, NEW_INSTANCE);
        depthGauge.increaseDepth();
        
        Object target = LifecycleHooks.callProxy(proxy);
        
        if (0 == depthGauge.decreaseDepth()) {
            METHOD_DEPTH.get().remove(hashCode);
            createMappingsFor(runner, method, target);
        }
        
        return target;
    }

    /**
     * Create mappings for the specified test runner/test method/test class instance.
     * 
     * @param runner underlying test runner
     * @param method target test method
     * @param target test class instance
     */
    static void createMappingsFor(final Object runner, final FrameworkMethod method, final Object target) {
        // if mappings haven't been created
        if (getMethodFor(target) == null) {
            LOGGER.debug("testObjectCreated: {}", target);
            TARGET_TO_METHOD.put(toMapKey(target), method);
            TARGET_TO_RUNNER.put(toMapKey(target), runner);
            
            // apply parameter-based global timeout
            TimeoutUtils.applyTestTimeout(runner, method, target);
            
            // if notifier hasn't been initialized yet
            if ( ! EachTestNotifierInit.setTestTarget(runner, method, target)) {
                // store target for subsequent retrieval
                HASHCODE_TO_TARGET.put(Objects.hash(runner, method.toString()), target);
            }
            
            for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
                watcher.testObjectCreated(runner, method, target);
            }
        }
    }
    
    /**
     * Get test class instance associated with the specified runner/method pair.
     * <p>
     * <b>NOTE</b>: This method can only be called once per target, as it removes the mapping.
     * 
     * @param runner JUnit class runner
     * @param method JUnit framework method
     * @return target test class instance
     */
    static Object getTargetFor(Object runner, FrameworkMethod method) {
        return HASHCODE_TO_TARGET.remove(Objects.hash(runner, method.toString()));
    }
    
    /**
     * Get the method for which the specified test class instance was created.
     * 
     * @param target test class instance
     * @return JUnit framework method
     */
    static FrameworkMethod getMethodFor(Object target) {
        return TARGET_TO_METHOD.get(toMapKey(target));
    }
    
    /**
     * Get the runner associated with the specified test class instance.
     * 
     * @param target test class instance
     * @return JUnit class runner
     */
    static Object getRunnerFor(Object target) {
        return TARGET_TO_RUNNER.get(toMapKey(target));
    }
    
    /**
     * Release the mappings associated with the specified runner/method/target group.
     * 
     * @param runner JUnit class runner
     * @param method JUnit framework method
     * @param target test class instance
     */
    static void releaseMappingsFor(Object runner, FrameworkMethod method, Object target) {
        HASHCODE_TO_TARGET.remove(Objects.hash(runner, method.toString()));
        if (target != null) {
            TARGET_TO_METHOD.remove(toMapKey(target));
            TARGET_TO_RUNNER.remove(toMapKey(target));
        }
    }
}
