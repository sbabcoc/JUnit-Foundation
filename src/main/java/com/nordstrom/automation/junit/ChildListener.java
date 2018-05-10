package com.nordstrom.automation.junit;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * This JUnit run listener captures test failures and assumption failures. If automatic retry of failed tests is
 * activated, <b>JUnit Foundation</b> uses this listener to capture test failures, which are sent for examination
 * to the configured retry analyzers. 
 */
class ChildListener extends RunListener {
    
    private Failure failure;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        this.failure = failure;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testAssumptionFailure(Failure failure) {
        this.failure = failure;
    }
    
    /**
     * Get the captured test failure details object, if any.
     * 
     * @return test failure details object; returns 'null' on test success
     */
    public Failure getFailure() {
        return failure;
    }
    
    /**
     * Reset this run listener for the next test. Captured test failure, if any, will be discarded.
     */
    public void reset() {
        failure = null;
    }

}
