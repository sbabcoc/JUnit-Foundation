package com.nordstrom.automation.junit;

public interface RunnerWatcher {

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
