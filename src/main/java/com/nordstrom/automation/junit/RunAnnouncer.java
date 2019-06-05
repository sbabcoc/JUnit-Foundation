package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class RunAnnouncer<T> extends RunListener {
    
    private static final ServiceLoader<RunWatcher> runWatcherLoader;
    private static final Map<Object, AtomicTest<?>> RUNNER_TO_ATOMICTEST = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnnouncer.class);
    
    static {
        runWatcherLoader = ServiceLoader.load(RunWatcher.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        LOGGER.debug("testStarted: {}", description);
        AtomicTest<T> atomicTest = getAtomicTestOf(description);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testStarted(atomicTest);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFinished(Description description) throws Exception {
        LOGGER.debug("testFinished: {}", description);
        AtomicTest<T> atomicTest = getAtomicTestOf(description);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testFinished(atomicTest);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        LOGGER.debug("testFailure: {}", failure);
        AtomicTest<T> atomicTest = setTestFailure(failure);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testFailure(atomicTest, failure.getException());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testAssumptionFailure(Failure failure) {
        LOGGER.debug("testAssumptionFailure: {}", failure);
        AtomicTest<T> atomicTest = setTestFailure(failure);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testAssumptionFailure(atomicTest, (AssumptionViolatedException) failure.getException());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testIgnored(Description description) throws Exception {
        LOGGER.debug("testIgnored: {}", description);
        AtomicTest<T> atomicTest = getAtomicTestOf(description);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
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
     * @param testKey JUnit class runner or method description
     * @return {@link AtomicTest} object (may be {@code null})
     */
    @SuppressWarnings("unchecked")
    static <T> AtomicTest<T> getAtomicTestOf(Object testKey) {
        return (AtomicTest<T>) RUNNER_TO_ATOMICTEST.get(testKey);
    }
    
    /**
     * Store the specified failure in the active atomic test.
     * 
     * @param failure {@link Failure} object
     * @return {@link AtomicTest} object
     */
    private static <T> AtomicTest<T> setTestFailure(Failure failure) {
        AtomicTest<T> atomicTest = getAtomicTestOf(Run.getThreadRunner());
        atomicTest.setThrowable(failure.getException());
        return atomicTest;
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param <W> watcher type
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    @SuppressWarnings("unchecked")
    static <W extends JUnitWatcher> Optional<W> getAttachedWatcher(Class<W> watcherType) {
        if (RunWatcher.class.isAssignableFrom(watcherType)) {
            synchronized(runWatcherLoader) {
                for (RunWatcher watcher : runWatcherLoader) {
                    if (watcher.getClass() == watcherType) {
                        return Optional.of((W) watcher);
                    }
                }
            }
        }
        return Optional.absent();
    }
}
