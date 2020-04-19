package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.lang.IllegalAccessException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
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
    
    private static final ThreadLocal<ConcurrentMap<Integer, DepthGauge>> methodDepth;
    private static final Function<Integer, DepthGauge> newInstance;
    private static final Logger LOGGER = LoggerFactory.getLogger(RunReflectiveCall.class);
    
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
     * Interceptor for the {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
     * runReflectiveCall} method.
     * 
     * @param callable {@code ReflectiveCallable} object being intercepted
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - value returned by the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static Object intercept(@This final ReflectiveCallable callable, @SuperCall final Callable<?> proxy)
                    throws Exception {
        
        Object child = null;
        Object target = null;

        try {
            child = getFieldValue(callable, "this$0");
            target = getFieldValue(callable, "val$target");
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
        }
        
        Object runner = Run.getParentOf(child);
        if (runner == null) {
            runner = Run.getThreadRunner();
        }

        if (ArtifactParams.class.isInstance(target)) {
            ((ArtifactParams) target).getAtomIdentity().setCallable(callable);
        }
        
        Object result = null;
        Throwable thrown = null;

        try {
            fireBeforeInvocation(runner, child, callable);
            result = LifecycleHooks.callProxy(proxy);
        } catch (Throwable t) {
            thrown = t;
        } finally {
            fireAfterInvocation(runner, child, callable, thrown);
        }

        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return result;
    }
    
    /**
     * Fire the {@link MethodWatcher#beforeInvocation(Object, Object, ReflectiveCallable) event.
     * <p>
     * If the {@code beforeInvocation} event for the specified method has already been fired, do nothing.
     * 
     * @param runner JUnit test runner
     * @param child child of {@code runner} that is being invoked
     * @param callable {@link ReflectiveCallable} object being intercepted
     * @return {@code true} if event the {@code beforeInvocation} was fired; otherwise {@code false}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean fireBeforeInvocation(Object runner, Object child, ReflectiveCallable callable) {
        if ((runner != null) && (child != null)) {
            DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(methodDepth.get(), callable.hashCode(), newInstance);
            if (0 == depthGauge.increaseDepth()) {
                if (LOGGER.isDebugEnabled()) {
                    try {
                        LOGGER.debug("beforeInvocation: {}", LifecycleHooks.invoke(runner, "describeChild", child));
                    } catch (Throwable t) {
                        // nothing to do here
                    }
                }
                for (MethodWatcher watcher : LifecycleHooks.getMethodWatchers()) {
                    if (watcher.supportedType().isInstance(child)) {
                        watcher.beforeInvocation(runner, child, callable);
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Fire the {@link MethodWatcher#afterInvocation(Object, Object, ReflectiveCallable) event.
     * <p>
     * If the {@code afterInvocation} event for the specified method has already been fired, do nothing.
     * 
     * @param runner JUnit test runner
     * @param child child of {@code runner} that was just invoked
     * @param callable {@link ReflectiveCallable} object being intercepted
     * @param thrown exception thrown by method; null on normal completion
     * @return {@code true} if event the {@code afterInvocation} was fired; otherwise {@code false}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean fireAfterInvocation(Object runner, Object child, ReflectiveCallable callable, Throwable thrown) {
        if ((runner != null) && (child != null)) {
            DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(methodDepth.get(), callable.hashCode(), newInstance);
            if (0 == depthGauge.decreaseDepth()) {
                if (LOGGER.isDebugEnabled()) {
                    try {
                        LOGGER.debug("afterInvocation: {}", LifecycleHooks.invoke(runner, "describeChild", child));
                    } catch (Throwable t) {
                        // nothing to do here
                    }
                }
                for (MethodWatcher watcher : LifecycleHooks.getMethodWatchers()) {
                    if (watcher.supportedType().isInstance(child)) {
                        watcher.afterInvocation(runner, child, callable, thrown);
                    }
                }
                return true;
            }
        }
        return false;
    }
}
