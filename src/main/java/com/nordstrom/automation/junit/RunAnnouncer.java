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
public class RunAnnouncer extends RunListener implements JUnitWatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnnouncer.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        LOGGER.debug("testStarted: {}", description);
        AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(description);
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
        AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(description);
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
        AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(failure.getDescription());
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
        AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(failure.getDescription());
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
        AtomicTest atomicTest = EachTestNotifierInit.ensureAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testIgnored(atomicTest);
        }
        // if this isn't a retried test
        if ( ! RetriedTest.isRetriedTest(description)) {
            EachTestNotifierInit.releaseAtomicTestOf(description);
        }
    }
}
