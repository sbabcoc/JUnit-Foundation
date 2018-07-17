package com.nordstrom.automation.junit;

import org.junit.runners.model.TestClass;

/**
 * This interface defines the methods implemented by JUnit test object watchers. These watchers are registered via a
 * ServiceLoader provider configuration file. To activate this feature, run with the {@link HookInstallingPlugin}.
 */
public interface TestObjectWatcher {
    
    /**
     * Invoked when a test class instance gets created.
     * 
     * @param testObj test class instance that was just created
     * @param testClass {@link TestClass} object that owns this test class instance
     */
    void testObjectCreated(Object testObj, TestClass testClass);

}
