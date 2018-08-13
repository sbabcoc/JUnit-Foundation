package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runners.model.FrameworkMethod;

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
    private static final Map<FrameworkMethod, Object> METHOD_TO_TARGET = new ConcurrentHashMap<>();
  
    static {
        methodWatcherLoader = ServiceLoader.load(MethodWatcher.class);
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
        FrameworkMethod method = null;
        Object target = null;
        Object[] params = null;

        try {
            Object owner = getFieldValue(callable, "this$0");
            if (owner instanceof FrameworkMethod) {
                method = (FrameworkMethod) owner;
                target = getFieldValue(callable, "val$target");
                params = getFieldValue(callable, "val$params");
                
                // if static method
                if (target == null) {
                    target = method.getDeclaringClass();
                }
                
                METHOD_TO_TARGET.put(method, target);
            }
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
        }
        
        if (method == null) {
            return proxy.call();
        }

        Object result = null;
        Throwable thrown = null;
        for (MethodWatcher watcher : methodWatcherLoader) {
            watcher.beforeInvocation(target, method, params);
        }

        try {
            result = proxy.call();
        } catch (Throwable t) {
            thrown = t;
        } finally {
            for (MethodWatcher watcher : methodWatcherLoader) {
                watcher.afterInvocation(target, method, thrown);
            }
        }

        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return result;
    }
    
    static void fireTestStarted(FrameworkMethod method) {
        Object target = getTargetFor(method);
        for (MethodWatcher watcher : methodWatcherLoader) {
            watcher.testStarted(method, target);
        }
    }
    
    static void fireTestFinished(FrameworkMethod method) {
        Object target = getTargetFor(method);
        for (MethodWatcher watcher : methodWatcherLoader) {
            watcher.testFinished(method, target);
        }
    }
    
    static void fireTestIgnored(FrameworkMethod method) {
        Object target = getTargetFor(method);
        for (MethodWatcher watcher : methodWatcherLoader) {
            watcher.testIgnored(method, target);
        }
    }
    
    static Object getTargetFor(FrameworkMethod method) {
        Object target = METHOD_TO_TARGET.get(method);
        if (target != null) {
            return target;
        }
        throw new IllegalArgumentException("No associated test class instance was found for the specified method");
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
        for (MethodWatcher watcher : methodWatcherLoader) {
            if (watcher.getClass() == watcherType) {
                return Optional.of(watcher);
            }
        }
        return Optional.empty();
    }
    
}
