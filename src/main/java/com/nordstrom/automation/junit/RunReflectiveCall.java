package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the
 * {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
 * runReflectiveCall} method.
 */
public class RunReflectiveCall {

    private static final Map<Integer, ReflectiveCallable> CHILD_TO_CALLABLE = new ConcurrentHashMap<>();
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

        try {
            // get child object
            child = LifecycleHooks.getFieldValue(callable, "this$0");
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
        }
        
        Object runner = RunChildren.getParentOf(child);
        if (runner == null) {
            runner = RunChildren.getThreadRunner();
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
    
    static Object getTargetFor(Object runner, FrameworkMethod method) {
        Object target = null;
        ReflectiveCallable callable = getCallableOf(runner, method);
        if (callable != null) {
            try {
                // get child object (class runner or framework method)
                target = LifecycleHooks.getFieldValue(callable, "val$target");
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                // handled below
            }
        }
        return target;
    }
    
    /**
     * Get the {@link ReflectiveCallable} object for the specified method description.
     *
     * @param runner target {@link org.junit.runners.ParentRunner ParentRunner} object
     * @param method {@link FrameworkMethod} object
     * @return <b>ReflectiveCallable</b> object (may be {@code null})
     */
    static ReflectiveCallable getCallableOf(Object runner, FrameworkMethod method) {
        return CHILD_TO_CALLABLE.get(Objects.hash(runner, method));
    }
    
    /**
     * Release the {@link ReflectiveCallable} object for the specified class runner or method description.
     *
     * @param runner target {@link org.junit.runners.ParentRunner ParentRunner} object
     * @param method {@link FrameworkMethod} object
     */
    static void releaseCallableOf(Object runner, FrameworkMethod method) {
        CHILD_TO_CALLABLE.remove(Objects.hash(runner, method));
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
                if (child instanceof FrameworkMethod) {
                    FrameworkMethod method = (FrameworkMethod) child;
                    CHILD_TO_CALLABLE.put(Objects.hash(runner, method), callable);
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            LOGGER.debug("beforeInvocation: {}",
                                    (Description) LifecycleHooks.invoke(runner, "describeChild", child));
                        } catch (Throwable t) {
                            // nothing to do here
                        }
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
     * Fire the {@link MethodWatcher#afterInvocation(Object, Object, ReflectiveCallable, Throwable) event.
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
                methodDepth.remove();
                if (child instanceof FrameworkMethod) {
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            LOGGER.debug("afterInvocation: {}",
                                    (Description) LifecycleHooks.invoke(runner, "describeChild", child));
                        } catch (Throwable t) {
                            // nothing to do here
                        }
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
