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
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class RunChildren {

    private static final ThreadLocal<Deque<Object>> RUNNER_STACK;
    private static final Set<String> NOTIFIERS = new CopyOnWriteArraySet<>();
    private static final Map<String, Object> CHILD_TO_PARENT = new ConcurrentHashMap<>();
    private static final Map<Integer, AtomicTest> DESCRIPTION_TO_ATOMICTEST = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RunChildren.class);
    
    static {
        RUNNER_STACK = new ThreadLocal<Deque<Object>>() {
            @Override
            protected Deque<Object> initialValue() {
                return new ArrayDeque<>();
            }
        };
    }
    
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
     * 
     */
    static void started() {
        fireRunStarted(getThreadRunner());
    }
    
    /**
     * Fire the {@link RunnerWatcher#runStarted(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runStarted} for the specified runner has already been fired, do nothing.
     * @param runner JUnit test runner
     */
    static void fireRunStarted(Object runner) {
        for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
            CHILD_TO_PARENT.put(toMapKey(child), runner);
            createMappingsFor(runner, child);
        }
        
        LOGGER.debug("runStarted: {}", runner);
        for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
            watcher.runStarted(runner);
        }
    }

    /**
     * 
     */
    static void finished() {
        fireRunFinished(getThreadRunner());
    }
    
    /**
     * Fire the {@link RunnerWatcher#runFinished(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runFinished} for the specified runner has already been fired, do nothing.
     * 
     * @param runner JUnit test runner
     */
    static void fireRunFinished(Object runner) {
        LOGGER.debug("runFinished: {}", runner);
        for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
            watcher.runFinished(runner);
        }
        
        for (Object child : (List<?>) LifecycleHooks.invoke(runner, "getChildren")) {
            releaseMappingsFor(runner, child);
            CHILD_TO_PARENT.remove(toMapKey(child));
        }
    }
    
    /**
     * 
     * @param runner
     * @param child
     * @return
     */
    static AtomicTest createMappingsFor(Object runner, Object child) {
        return createMappingsFor(LifecycleHooks.describeChild(runner, child));
    }

    /**
     * 
     * @param description
     * @return
     */
    static AtomicTest createMappingsFor(Description description) {
        if (DESCRIPTION_TO_ATOMICTEST.containsKey(description.hashCode())) {
            return DESCRIPTION_TO_ATOMICTEST.get(description.hashCode());
        } else {
            return newAtomicTest(description);
        }
    }
    
    /**
     * Create new atomic test object for the specified description.
     * 
     * @param description description of the test that is about to be run
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest newAtomicTest(Description description) {
        AtomicTest atomicTest = null;
        if (description.isTest()) {
            atomicTest = new AtomicTest(description);
            DESCRIPTION_TO_ATOMICTEST.put(description.hashCode(), atomicTest);
        }
        return atomicTest;
    }
    
    /**
     * Get the atomic test object for the specified method description.
     * 
     * @param description JUnit method description
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest getAtomicTestOf(Description description) {
      AtomicTest atomicTest = null;
      if (description != null) {
          // get atomic test for this description
          atomicTest = DESCRIPTION_TO_ATOMICTEST.get(description.hashCode());
      }
      return atomicTest;
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
    
    private static void releaseMappingsFor(Object runner, Object child) {
        if (child instanceof FrameworkMethod) {
            releaseMappingsFor(getAtomicTestOf(LifecycleHooks.describeChild(runner, child)));
        }
    }

    /**
     * Release the atomic test object for the specified method description.
     * 
     * @param atomicTest atomic test object
     */
    private static void releaseMappingsFor(AtomicTest atomicTest) {
        if (atomicTest != null) {
            CHILD_TO_PARENT.remove(toMapKey(atomicTest.getIdentity()));
            DESCRIPTION_TO_ATOMICTEST.remove(atomicTest.getDescription().hashCode());
            CreateTest.releaseMappingsFor(atomicTest.getRunner(), atomicTest.getIdentity());
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
