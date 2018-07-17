package com.nordstrom.automation.junit;

import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit TestClass watchers. These watchers are registered via a
 * ServiceLoader provider configuration file. To activate this feature, run with the {@link HookInstallingPlugin}.
 */
public interface TestClassWatcher {
    
    /**
     * Invoked when a test class object gets created. These are wrappers around runners and test classes.
     * 
     * @param testClass {@link TestClass} object that was just created
     * @param runner {@link ParentRunner} object that owns this test class object
     */
    void testClassCreated(TestClass testClass, Object runner);

}
