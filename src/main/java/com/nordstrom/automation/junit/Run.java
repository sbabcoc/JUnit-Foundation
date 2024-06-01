package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
 */
public class Run {
    private static final ThreadLocal<Deque<Object>> RUNNER_STACK;
    private static final ThreadLocal<ConcurrentMap<String, DepthGauge>> METHOD_DEPTH;
    private static final Function<String, DepthGauge> NEW_INSTANCE;
    private static final Set<String> START_NOTIFIED = new CopyOnWriteArraySet<>();
    private static final Map<String, Object> CHILD_TO_PARENT = new ConcurrentHashMap<>();
    private static final Map<String, RunNotifier> RUNNER_TO_NOTIFIER = new ConcurrentHashMap<>();
    private static final Set<String> NOTIFIERS = new CopyOnWriteArraySet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);
    
    static {
        RUNNER_STACK = new ThreadLocal<Deque<Object>>() {
            @Override
            protected Deque<Object> initialValue() {
                return new ArrayDeque<>();
            }
        };
        METHOD_DEPTH = new ThreadLocal<ConcurrentMap<String, DepthGauge>>() {
            @Override
            protected ConcurrentMap<String, DepthGauge> initialValue() {
                return new ConcurrentHashMap<>();
            }
        };
        NEW_INSTANCE = new Function<String, DepthGauge>() {
            @Override
            public DepthGauge apply(String input) {
                return new DepthGauge();
            }
        };
    }
    
    /**
     * Interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param notifier run notifier through which events are published
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
                    @Argument(0) final RunNotifier notifier) throws Exception {
        
        DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(METHOD_DEPTH.get(), toMapKey(runner), NEW_INSTANCE);
        
        try {
            if (0 == depthGauge.increaseDepth()) {
                RUNNER_TO_NOTIFIER.put(toMapKey(runner), notifier);
                pushThreadRunner(runner);
                attachRunListeners(runner, notifier);
                fireRunStarted(runner);
            }
            
            LifecycleHooks.callProxy(proxy);
        } finally {
            if (0 == depthGauge.decreaseDepth()) {
                METHOD_DEPTH.get().remove(toMapKey(runner));
                fireRunFinished(runner);
                popThreadRunner();
                RUNNER_TO_NOTIFIER.remove(toMapKey(runner));
            }
        }
    }
    
    /**
     * Get the run notifier associated with the specified parent runner.
     * 
     * @param runner JUnit parent runner
     * @return <b>RunNotifier</b> object (may be {@code null})
     */
    static RunNotifier getNotifierOf(final Object runner) {
        return RUNNER_TO_NOTIFIER.get(toMapKey(runner));
    }
    
    /**
     * Get the parent runner that owns specified child runner or framework method.
     * 
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @return {@code ParentRunner} object that owns the specified child ({@code null} for root objects)
     */
    static Object getParentOf(final Object child) {
        return CHILD_TO_PARENT.get(toMapKey(child));
    }
    
    /**
     * Push the specified JUnit test runner onto the stack for the current thread.
     * 
     * @param runner JUnit test runner
     */
    static void pushThreadRunner(final Object runner) {
        RUNNER_STACK.get().push(runner);
    }
    
    /**
     * Pop the top JUnit test runner from the stack for the current thread.
     * 
     * @return {@code ParentRunner} object
     * @throws EmptyStackException if called outside the scope of an active runner
     */
    static Object popThreadRunner() {
        return RUNNER_STACK.get().pop();
    }
    
    /**
     * Get the runner that owns the active thread context.
     * 
     * @return active {@code ParentRunner} object
     */
    static Object getThreadRunner() {
        return RUNNER_STACK.get().peek();
    }
    
    /**
     * Fire the {@link RunnerWatcher#runStarted(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runStarted} for the specified runner has already been fired, do nothing.
     * @param runner JUnit test runner
     * @return {@code true} if the {@code runStarted} event was fired; otherwise {@code false}
     */
    static boolean fireRunStarted(Object runner) {
        if (START_NOTIFIED.add(toMapKey(runner))) {
            for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
                CHILD_TO_PARENT.put(toMapKey(child), runner);
            }
            
            LOGGER.debug("runStarted: {}", runner);
            for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
                watcher.runStarted(runner);
            }
            return true;
        }
        return false;
    }

    /**
     * Fire the {@link RunnerWatcher#runFinished(Object)} event for the specified runner.
     * 
     * @param runner JUnit test runner
     */
    static void fireRunFinished(Object runner) {
        LOGGER.debug("runFinished: {}", runner);
        for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
            watcher.runFinished(runner);
        }
        
        START_NOTIFIED.remove(toMapKey(runner));
        for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
            CHILD_TO_PARENT.remove(toMapKey(child));
        }
    }
    
    /**
     * Attach registered run listeners to the specified run notifier.
     * <p>
     * <b>NOTE</b>: If the specified run notifier has already been seen, do nothing.
     *  
     * @param runner JUnit test runner
     * @param notifier JUnit {@link RunNotifier} object
     * @throws Exception if {@code run-started} notification 
     */
    static void attachRunListeners(Object runner, final RunNotifier notifier) throws Exception {
        if (NOTIFIERS.add(toMapKey(notifier))) {
            Description description = LifecycleHooks.invoke(runner, "getDescription");
            for (RunListener listener : LifecycleHooks.getRunListeners()) {
                // prevent potential duplicates
                notifier.removeListener(listener);
                notifier.addListener(listener);
                listener.testRunStarted(description);
            }
        }
    }
}
