package com.nordstrom.automation.junit;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit run watchers.
 */
public interface RunWatcher {
    
    /**
     * Called when an atomic test is about to be started.
     *
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param testClass {@link TestClass} object for this atomic test
     */
    public void testStarted(FrameworkMethod method, TestClass testClass);

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param testClass {@link TestClass} object for this atomic test
     */
    public void testFinished(FrameworkMethod method, TestClass testClass);
    
    /**
     * Called when an atomic test fails.
     * 
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param testClass {@link TestClass} object for this atomic test
     * @param thrown exception thrown by method
     */
    public void testFailure(FrameworkMethod method, TestClass testClass, Throwable thrown);

    /**
     * Called when an atomic test flags that it assumes a condition that is false
     * 
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param testClass {@link TestClass} object for this atomic test
     * @param thrown {@link AssumptionViolatedException} thrown by method
     */
    public void testAssumptionFailure(FrameworkMethod method, TestClass testClass, AssumptionViolatedException thrown);
    
    /**
     * Called when a test will not be run, generally because a test method is annotated with {@link org.junit.Ignore}.
     * 
     * @param method {@link FrameworkMethod} object for this atomic test
     * @param testClass {@link TestClass} object for this atomic test
     */
    public void testIgnored(FrameworkMethod method, TestClass testClass);
    
}
