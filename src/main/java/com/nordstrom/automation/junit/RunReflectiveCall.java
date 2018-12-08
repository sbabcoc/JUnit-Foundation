package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;

import com.nordstrom.automation.junit.LifecycleHooks.CreateTest;
import com.nordstrom.automation.junit.LifecycleHooks.Run;
import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
 * runReflectiveCall} method.
 */
@SuppressWarnings("squid:S1118")
public class RunReflectiveCall {
    
    private static final ServiceLoader<MethodWatcher> methodWatcherLoader;
    
    private static final ThreadLocal<Integer> COUNTER;
    private static final DepthGauge DEPTH;
  
    static {
        methodWatcherLoader = ServiceLoader.load(MethodWatcher.class);
        COUNTER = DepthGauge.getCounter();
        DEPTH = new DepthGauge(COUNTER);
    }
    
    /**
     * Interceptor for the {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
     * runReflectiveCall} method.
     * 
     * @param callable {@code ReflectiveCallable} object being intercepted 
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - value returned by the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static Object intercept(@This final Object callable, @SuperCall final Callable<?> proxy)
                    throws Exception {
        
        Object runner = null;
        Object target = null;
        FrameworkMethod method = null;
        Object[] params = null;

        try {
            Object owner = getFieldValue(callable, "this$0");
            if (owner instanceof FrameworkMethod) {
                method = (FrameworkMethod) owner;
                target = getFieldValue(callable, "val$target");
                params = getFieldValue(callable, "val$params");
                
                if (isParticleMethod(method)) {
                    if (target != null) {
                        runner = CreateTest.getRunnerForTarget(target);
                    } else {
                        runner = Run.getThreadRunner();
                    }
                }
            } else {
                runner = owner;
            }
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
        }
        
        if (method == null) {
            return LifecycleHooks.callProxy(proxy);
        }
        
        Object result = null;
        Throwable thrown = null;
        
        if (DEPTH.atGroundLevel()) {
            synchronized(methodWatcherLoader) {
                for (MethodWatcher watcher : methodWatcherLoader) {
                    watcher.beforeInvocation(runner, target, method, params);
                }
            }
        }

        try {
            DEPTH.increaseDepth();
            result = LifecycleHooks.callProxy(proxy);
        } catch (Throwable t) {
            thrown = t;
        } finally {
            if (0 == DEPTH.decreaseDepth()) {
                synchronized(methodWatcherLoader) {
                    for (MethodWatcher watcher : methodWatcherLoader) {
                        watcher.afterInvocation(runner, target, method, thrown);
                    }
                }
            }
        }

        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return result;
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    public static Optional<MethodWatcher> getAttachedWatcher(
                    Class<? extends MethodWatcher> watcherType) {
        Objects.requireNonNull(watcherType, "[watcherType] must be non-null");
        synchronized(methodWatcherLoader) {
            for (MethodWatcher watcher : methodWatcherLoader) {
                if (watcher.getClass() == watcherType) {
                    return Optional.of(watcher);
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Determine if the specified method is a test or configuration method.
     * 
     * @param method method whose type is in question
     * @return {@code true} if specified method is a particle; otherwise {@code false}
     */
    public static boolean isParticleMethod(FrameworkMethod method) {
        return ((null != method.getAnnotation(Test.class)) ||
                (null != method.getAnnotation(Before.class)) ||
                (null != method.getAnnotation(After.class)) ||
                (null != method.getAnnotation(BeforeClass.class)) ||
                (null != method.getAnnotation(AfterClass.class)));
    }
}
