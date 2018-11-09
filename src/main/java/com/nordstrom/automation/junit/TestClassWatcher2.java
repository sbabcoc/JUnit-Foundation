package com.nordstrom.automation.junit;

import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit TestClass watchers. These watchers are registered via a
 * ServiceLoader provider configuration file.
 */
public interface TestClassWatcher2 extends TestClassWatcher {
    
    /**
     * Called when a runner (test class or suite) is about to be started.
     * 
     * @param testClass {@link TestClass} object for the parent runner
     * @param runner JUnit test runner
     */
    void testClassStarted(TestClass testClass, Object runner);
    
    /**
     * Called when a runner (test class or suite) has finished.
     * 
     * @param testClass {@link TestClass} object for the parent runner
     * @param runner JUnit test runner
     */
    void testClassFinished(TestClass testClass, Object runner);
}
