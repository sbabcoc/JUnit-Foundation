package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

/**
 * This interface defines the methods implemented by JUnit method watchers.
 */
public interface MethodWatcher extends JUnitWatcher {

    /**
     * Invoked before each test or configuration method is invoked
     * 
     * @param runner JUnit test runner
     * @param target "enhanced" object upon which the method was invoked
     * @param method {@link FrameworkMethod} object for the invoked method
     * @param params method invocation parameters
     */
    void beforeInvocation(Object runner, Object target, FrameworkMethod method, Object... params);

    /**
     * Invoked after each test or configuration method is invoked
     * 
     * @param runner JUnit test runner
     * @param target "enhanced" object upon which the method was invoked
     * @param method {@link FrameworkMethod} object for the invoked method
     * @param thrown exception thrown by method; {@code null} on normal completion
     */
    void afterInvocation(Object runner, Object target, FrameworkMethod method, Throwable thrown);
}
