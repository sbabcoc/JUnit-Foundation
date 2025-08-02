package com.nordstrom.automation.junit;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a notification-enhancing extension of the standard {@link RunListener} class. This run
 * announcer is the source of notifications sent to attached implementations of the {@link RunWatcher} interface.
 * Note that <b>RunAnnouncer</b> is attached
 * <a href="https://github.com/sbabcoc/JUnit-Foundation#support-for-standard-junit-runlistener-providers">
 * automatically</a> by <b>JUnit Foundation</b>; attaching this run listener through conventional methods (Maven
 * or Gradle project configuration, {@code JUnitCore.addListener()}) is not only unnecessary, but will likely
 * suppress <b>RunWatcher</b> notifications.
 */
@RunAnnouncer.ThreadSafe
public class RunAnnouncer extends RunListener implements JUnitWatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnnouncer.class);
    
    /**
     * Default constructor
     */
    public RunAnnouncer() { }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        LOGGER.debug("testStarted: {}", description);
        AtomicTest atomicTest = ensureAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testStarted(atomicTest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFinished(Description description) throws Exception {
        LOGGER.debug("testFinished: {}", description);
        AtomicTest atomicTest = ensureAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testFinished(atomicTest);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        LOGGER.debug("testFailure: {}", failure);
        AtomicTest atomicTest = ensureAtomicTestOf(failure);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testFailure(atomicTest, failure.getException());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testAssumptionFailure(Failure failure) {
        LOGGER.debug("testAssumptionFailure: {}", failure);
        AtomicTest atomicTest = ensureAtomicTestOf(failure);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testAssumptionFailure(atomicTest, (AssumptionViolatedException) failure.getException());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testIgnored(Description description) throws Exception {
        LOGGER.debug("testIgnored: {}", description);
        AtomicTest atomicTest = ensureAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testIgnored(atomicTest);
        }
    }
    
    /**
     * Get the atomic test object for the specified method description.
     * <p>
     * <b>NOTE</b>: For ignored tests, this method returns an ephemeral object.
     * 
     * @param description JUnit method description
     * @return {@link AtomicTest} object
     */
    private static AtomicTest ensureAtomicTestOf(Description description) {
        // get atomic test for this description
        AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(description);
        // if none was found
        if (atomicTest == null) {
            // create ephemeral atomic test object
            atomicTest = new AtomicTest(description);
        }
        return atomicTest;
    }
    
    /**
     * Get the atomic test object for the specified failure.
     * <p>
     * <b>NOTE</b>: For suite failures, this method returns an ephemeral object.
     * 
     * @param failure 
     * @return {@link AtomicTest} object
     */
    private static AtomicTest ensureAtomicTestOf(Failure failure) {
        // get atomic test for this description
        AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(failure.getDescription());
        // if none was found
        if (atomicTest == null) {
            // create ephemeral atomic test object
            atomicTest = new AtomicTest(failure.getDescription());
            // set the exception for this atomic test
            atomicTest.setThrowable(failure.getException());
        }
        return atomicTest;
    }
}
