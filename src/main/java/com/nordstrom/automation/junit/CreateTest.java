package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
 * createTest} method.
 */
public class CreateTest {
    
    private static final Map<String, Object> TARGET_TO_RUNNER = new ConcurrentHashMap<>();
    private static final Map<String, FrameworkMethod> TARGET_TO_METHOD = new ConcurrentHashMap<>();
    private static final ThreadLocal<ConcurrentMap<Integer, DepthGauge>> methodDepth;
    private static final Function<Integer, DepthGauge> newInstance;
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTest.class);

    static {
        methodDepth = new ThreadLocal<ConcurrentMap<Integer, DepthGauge>>() {
            @Override
            protected ConcurrentMap<Integer, DepthGauge> initialValue() {
                return new ConcurrentHashMap<>();
            }
        };
        newInstance = new Function<Integer, DepthGauge>() {
            @Override
            public DepthGauge apply(Integer input) {
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
    public static Object intercept(@This final Object runner, @Argument(0) final FrameworkMethod method,
                    @SuperCall final Callable<?> proxy) throws Exception {
        
        Integer hashCode = Objects.hash(runner, method);
        DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(methodDepth.get(), hashCode, newInstance);
        depthGauge.increaseDepth();
        
        Object target = LifecycleHooks.callProxy(proxy);
        
        if (0 == depthGauge.decreaseDepth()) {
            methodDepth.remove();
            LOGGER.debug("testObjectCreated: {}", target);
            
            // apply parameter-based global timeout
            TimeoutUtils.applyTestTimeout(runner, method, target);
            
            TARGET_TO_RUNNER.put(toMapKey(target), runner);
            TARGET_TO_METHOD.put(toMapKey(target), method);
            RunAnnouncer.createMappingsFor(runner, method);
            
            for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
                watcher.testObjectCreated(runner, method, target);
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
    static Object getRunnerOf(Object target) {
        return TARGET_TO_RUNNER.get(toMapKey(target));
    }

    /**
     * Get the framework method associated with the specified instance.
     * 
     * @param target instance of JUnit test class
     * @return {@link FrameworkMethod} for specified instance
     */
    static FrameworkMethod getMethodOf(Object target) {
        return TARGET_TO_METHOD.get(toMapKey(target));
    }
    
    /**
     * Release runner/target/method mappings.
     * 
     * @param runner JUnit class runner
     * @param method JUnit framework method
     */
    static void releaseMappingsFor(Object runner, FrameworkMethod method) {
        Object target = RunReflectiveCall.getTargetFor(runner, method);
        if (target != null) {
            TARGET_TO_RUNNER.remove(toMapKey(target));
            TARGET_TO_METHOD.remove(toMapKey(target));
        }
    }
}
