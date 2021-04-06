package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#runChildren runChildren} method.
 */
public class RunChildren {

    private static final ThreadLocal<Deque<Object>> RUNNER_STACK;
    private static final Map<String, Object> CHILD_TO_PARENT = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RunChildren.class);
    
    static {
        RUNNER_STACK = new ThreadLocal<Deque<Object>>() {
            @Override
            protected Deque<Object> initialValue() {
                return new ArrayDeque<>();
            }
        };
    }
    
    /**
     * Interceptor for the {@link org.junit.runners.ParentRunner#runChildren runChildren} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param notifier run notifier through which events are published
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
            @Argument(0) final RunNotifier notifier) throws Exception {
        
        try {
            pushThreadRunner(runner);
            LifecycleHooks.callProxy(proxy);
        } finally {
            popThreadRunner();
        }
    }
    
    /**
     * Fire the {@link RunnerWatcher#runStarted(Object)} event for the current runner.
     */
    static void started() {
        fireRunStarted(getThreadRunner());
    }
    
    /**
     * Fire the {@link RunnerWatcher#runStarted(Object)} event for the specified runner.
     * 
     * @param runner JUnit test runner
     */
    static void fireRunStarted(Object runner) {
        for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
            CHILD_TO_PARENT.put(toMapKey(child), runner);
        }
        
        LOGGER.debug("runStarted: {}", runner);
        for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
            watcher.runStarted(runner);
        }
    }

    /**
     * Fire the {@link RunnerWatcher#runFinished(Object)} event for the current runner.
     */
    static void finished() {
        fireRunFinished(getThreadRunner());
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
        
        for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
            CHILD_TO_PARENT.remove(toMapKey(child));
        }
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
    
    static boolean isEmpty() {
        boolean isEmpty = true;
        if (CHILD_TO_PARENT.isEmpty()) {
            LOGGER.debug("CHILD_TO_PARENT is empty");
        } else {
            isEmpty = false;
            LOGGER.debug("CHILD_TO_PARENT is not empty");
        }
        return isEmpty;
    }
}
