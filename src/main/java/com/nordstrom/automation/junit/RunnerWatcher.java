package com.nordstrom.automation.junit;

/**
 * This interface defines the methods implemented by JUnit runner watchers.
 */
public interface RunnerWatcher extends JUnitWatcher {

    /**
     * Called when a test runner is about to run.
     * 
     * @param runner JUnit test runner
     */
    public void runStarted(Object runner);

    /**
     * Called when a test runner has finished running.
     * 
     * @param runner JUnit test runner
     */
    public void runFinished(Object runner);

}
