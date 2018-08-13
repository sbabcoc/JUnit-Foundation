package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit TestClass watchers. These watchers are registered via a
 * ServiceLoader provider configuration file.
 */
public interface TestClassWatcher {
    
    /**
     * Invoked when a test class object gets created. These are wrappers around runners and test classes.
     * 
     * @param testClass {@link TestClass} object that was just created
     * @param runner {@link org.junit.runners.ParentRunner ParentRunner} object that owns this test class object
     */
    void testClassCreated(TestClass testClass, Object runner);
    
    /**
     * Called when a runner (test class or suite) is about to be started.
     * 
     * @param testClass {@link TestClass} object for the parent runner
     * @param runner {@link org.junit.runners.ParentRunner ParentRunner} object for the parent runner
     */
    void testClassStarted(TestClass testClass, Object runner);
    
    /**
     * Called when a runner (test class or suite) has finished.
     * 
     * @param testClass {@link TestClass} object for the parent runner
     * @param runner {@link org.junit.runners.ParentRunner ParentRunner} object for the parent runner
     */
    void testClassFinished(TestClass testClass, Object runner);
}
