package com.nordstrom.automation.junit;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

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
    
    static final ServiceLoader<MethodWatcher> methodWatcherLoader;
  
    static {
        methodWatcherLoader = ServiceLoader.load(MethodWatcher.class);
    }
    
    @RuntimeType
    public static Object intercept(@This final Object obj, @SuperCall final Callable<?> proxy) throws Exception {
        FrameworkMethod method = null;
        Object target = null;
        Object[] params = null;

        try {
            Object owner = LifecycleHooks.getFieldValue(obj, "this$0");
            if (owner instanceof FrameworkMethod) {
                method = (FrameworkMethod) owner;
                target = LifecycleHooks.getFieldValue(obj, "val$target");
                params = LifecycleHooks.getFieldValue(obj, "val$params");
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
