package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.runner.Description;
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
    
    private static final Map<String, AtomicTest> TARGET_TO_ATOMICTEST = new ConcurrentHashMap<>();
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
     * @param method {@link FrameworkMethod} for which this test class instance is being created
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
            
            TARGET_TO_ATOMICTEST.put(toMapKey(target), RunChildren.createMappingsFor(runner, method));
            
            for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
                watcher.testObjectCreated(runner, method, target);
            }
        }
        
        return target;
    }
    
    /**
     * Get the atomic test associated with the specified instance.
     * 
     * @param target instance of JUnit test class
     * @return {@link AtomicTest} for specified instance
     */
    static AtomicTest getAtomicTestOf(Object target) {
        return TARGET_TO_ATOMICTEST.get(toMapKey(target));
    }

    /**
     * Release runner/target/method mappings.
     * 
     * @param description 
     */
    static void releaseMappingsFor(Description description) {
        Object target = RunReflectiveCall.getTargetFor(description);
        if (target != null) {
            TARGET_TO_ATOMICTEST.remove(toMapKey(target));
        }
    }
    
    static boolean isEmpty() {
        boolean isEmpty = true;
        if (TARGET_TO_ATOMICTEST.isEmpty()) {
            LOGGER.debug("TARGET_TO_ATOMICTEST is empty");
        } else {
            isEmpty = false;
            LOGGER.debug("TARGET_TO_ATOMICTEST is not empty");
        }
        return isEmpty;
    }
}
