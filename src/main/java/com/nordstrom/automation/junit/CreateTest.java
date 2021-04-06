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
    
    private static final Map<Integer, Object> HASHCODE_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<String, FrameworkMethod> TARGET_TO_METHOD = new ConcurrentHashMap<>();
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
        DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(METHOD_DEPTH.get(), hashCode, NEW_INSTANCE);
        depthGauge.increaseDepth();
        
        Object target = LifecycleHooks.callProxy(proxy);
        
        if (0 == depthGauge.decreaseDepth()) {
            METHOD_DEPTH.remove();
            LOGGER.debug("testObjectCreated: {}", target);
            
            TARGET_TO_METHOD.put(toMapKey(target), method);
            
            // apply parameter-based global timeout
            TimeoutUtils.applyTestTimeout(runner, method, target);
            
            // if notifier hasn't been initialized yet
            if ( ! EachTestNotifierInit.setTestTarget(runner, method, target)) {
                HASHCODE_TO_TARGET.put(hashCode, target);
            }
            
            for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
                watcher.testObjectCreated(runner, method, target);
            }
        }
        
        return target;
    }
    
    static Object getTargetFor(Object runner, FrameworkMethod method) {
        return HASHCODE_TO_TARGET.remove(Objects.hash(runner, method));
    }
    
    static FrameworkMethod getMethodFor(Object target) {
        return TARGET_TO_METHOD.get(toMapKey(target));
    }
    
    static void releaseMappingsFor(Object runner, FrameworkMethod method, Object target) {
        HASHCODE_TO_TARGET.remove(Objects.hash(runner, method));
        if (target != null) {
            TARGET_TO_METHOD.remove(toMapKey(target));
        }
    }
    
    static boolean isEmpty() {
        boolean isEmpty = true;
        if (HASHCODE_TO_TARGET.isEmpty()) {
            LOGGER.debug("HASHCODE_TO_TARGET is empty");
        } else {
            isEmpty = false;
            LOGGER.debug("HASHCODE_TO_TARGET is not empty");
        }
        if (TARGET_TO_METHOD.isEmpty()) {
            LOGGER.debug("TARGET_TO_METHOD is empty");
        } else {
            isEmpty = false;
            LOGGER.debug("TARGET_TO_METHOD is not empty");
        }
        return isEmpty;
    }
}
