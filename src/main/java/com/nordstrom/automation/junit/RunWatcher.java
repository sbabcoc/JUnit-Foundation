package com.nordstrom.automation.junit;

import org.junit.internal.AssumptionViolatedException;

/**
 * This interface defines the methods implemented by JUnit run watchers.
 */
public interface RunWatcher<T> extends JUnitWatcher {

    /**
     * Called when an atomic test is about to be started.
     *
     * @param atomicTest {@link AtomicTest} object for this atomic test
     */
    public void testStarted(AtomicTest<T> atomicTest);

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     *
     * @param atomicTest {@link AtomicTest} object for this atomic test
     */
    public void testFinished(AtomicTest<T> atomicTest);
    
    /**
     * Called when an atomic test fails.
     * 
     * @param atomicTest {@link AtomicTest} object for this atomic test
     * @param thrown exception thrown by method
     */
    public void testFailure(AtomicTest<T> atomicTest, Throwable thrown);

    /**
     * Called when an atomic test flags that it assumes a condition that is false
     * 
     * @param atomicTest {@link AtomicTest} object for this atomic test
     * @param thrown {@link AssumptionViolatedException} thrown by method
     */
    public void testAssumptionFailure(AtomicTest<T> atomicTest, AssumptionViolatedException thrown);
    
    /**
     * Called when a test will not be run, generally because a test method is annotated with {@link org.junit.Ignore}.
     * 
     * @param atomicTest {@link AtomicTest} object for this atomic test
     */
    public void testIgnored(AtomicTest<T> atomicTest);
    
    /**
     * Publish the child object type supported by this watcher.
     * 
     * @return supported child object type
     */
    Class<T> supportedType();
}
