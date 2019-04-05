package com.nordstrom.automation.junit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
 */
@SuppressWarnings("squid:S1118")
public class Run {
    private static final ThreadLocal<Deque<Object>> runnerStack;
    private static final Set<String> startNotified = new CopyOnWriteArraySet<>();
    private static final Set<String> finishNotified = new CopyOnWriteArraySet<>();
    private static final ServiceLoader<RunListener> runListenerLoader;
    private static final ServiceLoader<RunnerWatcher> runnerWatcherLoader;
    private static final Map<Object, Object> CHILD_TO_PARENT = new ConcurrentHashMap<>();
    private static final Set<RunNotifier> NOTIFIERS = new CopyOnWriteArraySet<>();
    
    static {
        runnerStack = ThreadLocal.withInitial(ArrayDeque::new);
        runListenerLoader = ServiceLoader.load(RunListener.class);
        runnerWatcherLoader = ServiceLoader.load(RunnerWatcher.class);
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
            pushThreadRunner(runner);
            fireRunStarted(runner);
            LifecycleHooks.callProxy(proxy);
        } finally {
            fireRunFinished(runner);
            popThreadRunner();
        }
    }
    
    /**
     * Get the parent runner that owns specified child runner or framework method.
     * 
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @return {@code ParentRunner} object that owns the specified child ({@code null} for root objects)
     */
    static Object getParentOf(final Object child) {
        return CHILD_TO_PARENT.get(child);
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
        if (NOTIFIERS.add(notifier)) {
            Description description = LifecycleHooks.invoke(runner, "getDescription");
            synchronized(runListenerLoader) {
                for (RunListener listener : runListenerLoader) {
                    notifier.addListener(listener);
                    listener.testRunStarted(description);
                }
            }
        }
    }
    
    /**
     * Push the specified JUnit test runner onto the stack for the current thread.
     * 
     * @param runner JUnit test runner
     */
    static void pushThreadRunner(final Object runner) {
        runnerStack.get().push(runner);
    }
    
    /**
     * Pop the top JUnit test runner from the stack for the current thread.
     * 
     * @return {@code ParentRunner} object
     * @throws EmptyStackException if called outside the scope of an active runner
     */
    static Object popThreadRunner() {
        return runnerStack.get().pop();
    }
    
    /**
     * Get the runner that owns the active thread context.
     * 
     * @return active {@code ParentRunner} object
     */
    static Object getThreadRunner() {
        return runnerStack.get().peek();
    }
    
    /**
     * Fire the {@link RunnerWatcher#runStarted(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runStarted} for the specified runner has already been fired, do nothing.
     * @param runner JUnit test runner
     * @return {@code true} if event the {@code runStarted} was fired; otherwise {@code false}
     */
    static boolean fireRunStarted(Object runner) {
        if (startNotified.add(runner.toString())) {
            List<?> grandchildren = LifecycleHooks.invoke(runner, "getChildren");
            for (Object grandchild : grandchildren) {
                CHILD_TO_PARENT.put(grandchild, runner);
            }
            synchronized(runnerWatcherLoader) {
                for (RunnerWatcher watcher : runnerWatcherLoader) {
                    watcher.runStarted(runner);
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Fire the {@link RunnerWatcher#runFinished(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runFinished} for the specified runner has already been fired, do nothing.
     * @param runner JUnit test runner
     * @return {@code true} if event the {@code runFinished} was fired; otherwise {@code false}
     */
    static boolean fireRunFinished(Object runner) {
        if (finishNotified.add(runner.toString())) {
            synchronized(runnerWatcherLoader) {
                for (RunnerWatcher watcher : runnerWatcherLoader) {
                    watcher.runFinished(runner);
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param <T> watcher type
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    @SuppressWarnings("unchecked")
    static <T extends JUnitWatcher> Optional<T> getAttachedWatcher(Class<T> watcherType) {
        if (RunnerWatcher.class.isAssignableFrom(watcherType)) {
            synchronized(runnerWatcherLoader) {
                for (RunnerWatcher watcher : runnerWatcherLoader) {
                    if (watcher.getClass() == watcherType) {
                        return Optional.of((T) watcher);
                    }
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get reference to an instance of the specified listener type.
     * 
     * @param <T> listener type
     * @param listenerType listener type
     * @return optional listener instance
     */
    @SuppressWarnings("unchecked")
    static <T extends RunListener> Optional<T> getAttachedListener(Class<T> listenerType) {
        synchronized(runListenerLoader) {
            for (RunListener listener : runListenerLoader) {
                if (listener.getClass() == listenerType) {
                    return Optional.of((T) listener);
                }
            }
        }
        return Optional.empty();
    }
}
