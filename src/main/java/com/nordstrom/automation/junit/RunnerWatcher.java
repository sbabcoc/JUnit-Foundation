package com.nordstrom.automation.junit;

import org.junit.runners.model.TestClass;

public interface RunnerWatcher {

    /**
     * Called when a test runner is about to run.
     * 
     * @param testClass {@link TestClass} object for this test runner
     */
    public void runStarted(TestClass testClass);

    /**
     * Called when a test runner has finished running.
     * 
     * @param testClass {@link TestClass} object for this test runner
     */
    public void runFinished(TestClass testClass);

}
