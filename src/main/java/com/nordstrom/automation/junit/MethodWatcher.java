package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit method watchers.
 */
public interface MethodWatcher {

    
    /**
     * Called when an atomic test is about to be started.
     *
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param target test class instance for this atomic test
     */
    public void testStarted(FrameworkMethod method, Object target);

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param target test class instance for this atomic test
     */
    public void testFinished(FrameworkMethod method, Object target);
    
    /**
     * Called when a test will not be run, generally because a test method is annotated with {@link org.junit.Ignore}.
     * 
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param target test class instance for this atomic test
     */
    public void testIgnored(FrameworkMethod method, Object target);
    
    /**
     * Invoked before each test or configuration method is invoked
     * 
     * @param target "enhanced" object upon which the method was invoked
     * @param method {@link FrameworkMethod} object for the invoked method
     * @param params method invocation parameters
     */
    void beforeInvocation(Object target, FrameworkMethod method, Object... params);

    /**
     * Invoked after each test or configuration method is invoked
     * 
     * @param target "enhanced" object upon which the method was invoked
     * @param method {@link FrameworkMethod} object for the invoked method
     * @param thrown exception thrown by method; {@code null} on normal completion
     */
    void afterInvocation(Object target, FrameworkMethod method, Throwable thrown);
}
