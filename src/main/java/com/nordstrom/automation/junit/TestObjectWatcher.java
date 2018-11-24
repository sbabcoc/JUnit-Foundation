package com.nordstrom.automation.junit;

/**
 * This interface defines the methods implemented by JUnit test object watchers. These watchers are registered via a
 * ServiceLoader provider configuration file.
 */
public interface TestObjectWatcher {
    
    /**
     * Invoked when a test class instance gets created.
     * 
     * @param testObj test class instance that was just created
     * @param runner JUnit runner for this test class instance
     */
    void testObjectCreated(Object testObj, Object runner);

}
