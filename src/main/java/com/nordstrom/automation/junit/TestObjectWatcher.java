package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

/**
 * This interface defines the methods implemented by JUnit test object watchers. These watchers are registered via a
 * ServiceLoader provider configuration file.
 */
public interface TestObjectWatcher extends JUnitWatcher {
    
    /**
     * Invoked when a test class instance gets created.
     * @param runner JUnit runner for this test class instance
     * @param method target method for this test class instance
     * @param testObj test class instance that was just created
     */
    void testObjectCreated(Object runner, FrameworkMethod method, Object testObj);

}
