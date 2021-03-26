package com.nordstrom.automation.junit;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
 */
public class Run {
    private static final ThreadLocal<Deque<Object>> RUNNER_STACK;
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
        
        attachRunListeners(runner, notifier);
        
        try {
            RUNNER_TO_NOTIFIER.put(toMapKey(runner), notifier);
            pushThreadRunner(runner);
            LifecycleHooks.callProxy(proxy);
        } finally {
            popThreadRunner();
            RUNNER_TO_NOTIFIER.remove(toMapKey(runner));
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
                notifier.addListener(listener);
                listener.testRunStarted(description);
            }
        }
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
}
