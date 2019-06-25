package com.nordstrom.automation.junit;

import org.junit.internal.runners.model.ReflectiveCallable;

/**
 * This interface defines the methods implemented by JUnit method watchers.
 */
public interface MethodWatcher extends JUnitWatcher {

    /**
     * Invoked before each test or configuration method is invoked.
     * 
     * @param runner JUnit test runner
     * @param child child object of {@code runner} that is being invoked
     * @param callable {@link ReflectiveCallable} object being intercepted
     */
    void beforeInvocation(Object runner, Object child, ReflectiveCallable callable);

    /**
     * Invoked after each test or configuration method is invoked.
     * 
     * @param runner JUnit test runner
     * @param child child object of {@code runner} that was just invoked
     * @param callable {@link ReflectiveCallable} object being intercepted
     * @param thrown exception thrown by method; {@code null} on normal completion
     */
    void afterInvocation(Object runner, Object child, ReflectiveCallable callable, Throwable thrown);
}
