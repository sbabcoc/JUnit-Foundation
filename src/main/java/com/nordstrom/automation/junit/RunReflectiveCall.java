package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Map<Integer, ReflectiveCallable> DESCRIPTION_TO_CALLABLE = new ConcurrentHashMap<>();
    private static final ThreadLocal<ConcurrentMap<Integer, DepthGauge>> METHOD_DEPTH;
    private static final Function<Integer, DepthGauge> NEW_INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(RunReflectiveCall.class);
    
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
    public RunReflectiveCall() { }
    
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
        Object runner = null;
        boolean didPush = false;

        try {
            // get child object
            child = LifecycleHooks.getFieldValue(callable, "this$0");
            // get thread runner
            runner = Run.getThreadRunner();
            
            // if runner unknown
            if (runner == null) {
                // get runner for child
                runner = Run.getParentOf(child);
            }
            
            // if runner unknown
            if (runner == null) {
                // get test class instance
                Object target = LifecycleHooks.getFieldValue(callable, "val$target");
                // if target acquired
                if (target != null) {
                    // get runner for target
                    runner = CreateTest.getRunnerFor(target);
                    
                    // if runner resolved
                    if (runner != null) {
                        Run.pushThreadRunner(runner);
                        didPush = true;
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
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
            if (didPush) {
                Run.popThreadRunner();
            }
        }

        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return result;
    }
    
    /**
     * Get the test class instance for the specified description.
     *
     * @param description JUnit method description
     * @return <b>ReflectiveCallable</b> object (may be {@code null})
     */
    static Object getTargetFor(Description description) {
        Object target = null;
        ReflectiveCallable callable = getCallableOf(description);
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
     * Get the {@link ReflectiveCallable} object for the specified description.
     *
     * @param description JUnit method description
     * @return <b>ReflectiveCallable</b> object (may be {@code null})
     */
    static ReflectiveCallable getCallableOf(Description description) {
        return DESCRIPTION_TO_CALLABLE.get(description.hashCode());
    }
    
    /**
     * Release the {@link ReflectiveCallable} object for the specified description.
     *
     * @param description JUnit method description
     */
    static void releaseCallableOf(Description description) {
        DESCRIPTION_TO_CALLABLE.remove(description.hashCode());
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
            DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(METHOD_DEPTH.get(), callable.hashCode(), NEW_INSTANCE);
            if (0 == depthGauge.increaseDepth()) {
                if (child instanceof FrameworkMethod) {
                    FrameworkMethod method = (FrameworkMethod) child;
                    Description description = LifecycleHooks.describeChild(runner, method);
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            LOGGER.debug("beforeInvocation: {}", (description != null) ? description : method);
                        } catch (Throwable t) {
                            // nothing to do here
                        }
                    }
                    if ((description != null) && AtomicTest.isTest(description)) {
                        DESCRIPTION_TO_CALLABLE.put(description.hashCode(), callable);
                        
                        // get target for description
                        Object target = getTargetFor(description);
                        // if target acquired
                        if (target != null) {
                            // ensure that test object creation is tracked
                            CreateTest.createMappingsFor(runner, method, target);
                            // ensure that description has matching atomic test
                            EachTestNotifierInit.newAtomicTestFor(description);
                            // ensure that description <=> target mappings are set
                            EachTestNotifierInit.setTestTarget(runner, method, target);
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
            DepthGauge depthGauge = METHOD_DEPTH.get().get(callable.hashCode());
            if (0 == depthGauge.decreaseDepth()) {
                METHOD_DEPTH.get().remove(callable.hashCode());
                if (child instanceof FrameworkMethod) {
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            Description description = LifecycleHooks.describeChild(runner, child);
                            LOGGER.debug("afterInvocation: {}", (description != null) ? description : child);
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
