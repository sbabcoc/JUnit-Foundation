package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
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
            createMappingsFor(runner, child);
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
            releaseMappingsFor(runner, child);
            CHILD_TO_PARENT.remove(toMapKey(child));
        }
    }
    
    /**
     * Create mappings for the specified runner/child pair.
     * 
     * @param runner JUnit test runner
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     */
    static void createMappingsFor(Object runner, Object child) {
        CHILD_TO_PARENT.put(toMapKey(child), runner);
        ensureAtomicTestOf(LifecycleHooks.describeChild(runner, child));
    }

    /**
    * Get the atomic test object for the specified method description; create if absent.
    * 
    * @param description JUnit method description
    * @return {@link AtomicTest} object (may be {@code null})
    */
    static AtomicTest ensureAtomicTestOf(Description description) {
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
    
    /**
     * Release mappings for the specified runner/child pair.
     * 
     * @param runner JUnit test runner
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     */
    private static void releaseMappingsFor(Object runner, Object child) {
        if (child instanceof FrameworkMethod) {
            releaseMappingsFor(getAtomicTestOf(LifecycleHooks.describeChild(runner, child)));
        }
    }

    /**
     * Release mappings for the atomic test object.
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
