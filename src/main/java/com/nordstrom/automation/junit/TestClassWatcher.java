package com.nordstrom.automation.junit;

import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit TestClass watchers. These watchers are registered via a
 * ServiceLoader provider configuration file.
 */
public interface TestClassWatcher extends JUnitWatcher {
    
    /**
     * Invoked when a test class object gets created. These are wrappers around runners and test classes.
     * 
     * @param testClass {@link TestClass} object that was just created
     * @param runner {@link org.junit.runners.ParentRunner ParentRunner} object that owns this test class object
     */
    void testClassCreated(TestClass testClass, Object runner);
    
}
