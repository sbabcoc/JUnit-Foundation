package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * <a href="https://github.com/Nordstrom/JUnit-Foundation#support-for-standard-junit-runlistener-providers">
 * automatically</a> by <b>JUnit Foundation</b>; attaching this run listener through conventional methods (Maven
 * or Gradle project configuration, {@code JUnitCore.addListener()}) is not only unnecessary, but will likely
 * suppress <b>RunWatcher</b> notifications.
 */
public class RunAnnouncer extends RunListener {
    
    private static final Map<Object, AtomicTest<?>> RUNNER_TO_ATOMICTEST = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnnouncer.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testStarted(Description description) throws Exception {
        LOGGER.debug("testStarted: {}", description);
        AtomicTest<?> atomicTest = getAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            if (isSupported(watcher, atomicTest)) {
                watcher.testStarted(atomicTest);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testFinished(Description description) throws Exception {
        LOGGER.debug("testFinished: {}", description);
        AtomicTest<?> atomicTest = getAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            if (isSupported(watcher, atomicTest)) {
                watcher.testFinished(atomicTest);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testFailure(Failure failure) throws Exception {
        LOGGER.debug("testFailure: {}", failure);
        AtomicTest<?> atomicTest = setTestFailure(failure);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            if (isSupported(watcher, atomicTest)) {
                watcher.testFailure(atomicTest, failure.getException());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testAssumptionFailure(Failure failure) {
        LOGGER.debug("testAssumptionFailure: {}", failure);
        AtomicTest<?> atomicTest = setTestFailure(failure);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            if (isSupported(watcher, atomicTest)) {
                watcher.testAssumptionFailure(atomicTest, (AssumptionViolatedException) failure.getException());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testIgnored(Description description) throws Exception {
        LOGGER.debug("testIgnored: {}", description);
        AtomicTest<?> atomicTest = getAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            if (isSupported(watcher, atomicTest)) {
                watcher.testIgnored(atomicTest);
            }
        }
    }
    
    /**
     * Create new atomic test object for the specified runner/child pair.
     * 
     * @param <T> type of children associated with the specified runner
     * @param runner parent runner
     * @param identity identity for this atomic test
     * @return {@link AtomicTest} object
     */
    static <T> AtomicTest<T> newAtomicTest(Object runner, T identity) {
        AtomicTest<T> atomicTest = new AtomicTest<>(runner, identity);
        RUNNER_TO_ATOMICTEST.put(runner, atomicTest);
        RUNNER_TO_ATOMICTEST.put(atomicTest.getDescription(), atomicTest);
        return atomicTest;
    }
    
    /**
     * Get the atomic test object for the specified class runner or method description.
     * 
     * @param <T> atomic test child object type
     * @param testKey JUnit class runner or method description
     * @return {@link AtomicTest} object (may be {@code null})
     */
    @SuppressWarnings("unchecked")
    static <T> AtomicTest<T> getAtomicTestOf(Object testKey) {
        return (testKey == null) ? null : (AtomicTest<T>) RUNNER_TO_ATOMICTEST.get(testKey);
    }
    
    /**
     * Store the specified failure in the active atomic test.
     * 
     * @param <T> atomic test child object type
     * @param failure {@link Failure} object
     * @return {@link AtomicTest} object
     */
    private static <T> AtomicTest<T> setTestFailure(Failure failure) {
        AtomicTest<T> atomicTest = getAtomicTestOf(Run.getThreadRunner());
        if (atomicTest == null) {
            atomicTest = getAtomicTestOf(failure.getDescription());
        }
        if (atomicTest != null) {
            atomicTest.setThrowable(failure.getException());
        }
        return atomicTest;
    }
    
    /**
     * Determine if the run watcher in question supports the data type of specified atomic test.
     * 
     * @param watcher {@link RunWatcher} object
     * @param atomicTest {@link AtomicTest} object
     * @return {@code true} if the specified run watcher supports the indicated data type; otherwise {@code false}
     */
    private static boolean isSupported(RunWatcher<?> watcher, AtomicTest<?> atomicTest) {
        return (atomicTest == null) ? false : watcher.supportedType().isInstance(atomicTest.getIdentity());
    }
}
