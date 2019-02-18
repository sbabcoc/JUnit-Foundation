package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class RunAnnouncer extends RunListener {
    
    private static final ServiceLoader<RunWatcher> runWatcherLoader;
    private static final Map<Object, AtomicTest> RUNNER_TO_ATOMICTEST = new ConcurrentHashMap<>();
    
    static {
        runWatcherLoader = ServiceLoader.load(RunWatcher.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        AtomicTest atomicTest = newAtomicTest(description);
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
        AtomicTest atomicTest = getAtomicTestOf(Run.getThreadRunner());
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
        AtomicTest atomicTest = setTestFailure(failure);
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
        AtomicTest atomicTest = setTestFailure(failure);
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
        AtomicTest atomicTest = newAtomicTest(description);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testIgnored(atomicTest);
            }
        }
    }
    
    /**
     * Create new atomic test object for the specified description.
     * 
     * @param description {@link Description} object
     * @return {@link AtomicTest} object
     */
    private static AtomicTest newAtomicTest(Description description) {
        Object runner = Run.getThreadRunner();
        AtomicTest atomicTest = new AtomicTest(runner, description);
        RUNNER_TO_ATOMICTEST.put(runner, atomicTest);
        return atomicTest;
    }
    
    /**
     * Get the atomic test object for the specified class runner.
     * 
     * @param runner JUnit class runner
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest getAtomicTestOf(Object runner) {
        return RUNNER_TO_ATOMICTEST.get(runner);
    }
    
    /**
     * Store the specified failure in the active atomic test.
     * 
     * @param failure {@link Failure} object
     * @return {@link AtomicTest} object
     */
    private static AtomicTest setTestFailure(Failure failure) {
        AtomicTest atomicTest = getAtomicTestOf(Run.getThreadRunner());
        atomicTest.setThrowable(failure.getException());
        return atomicTest;
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param <T> watcher type
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    @SuppressWarnings("unchecked")
    static <T extends JUnitWatcher> Optional<T> getAttachedWatcher(Class<T> watcherType) {
        if (RunWatcher.class.isAssignableFrom(watcherType)) {
            synchronized(runWatcherLoader) {
                for (RunWatcher watcher : runWatcherLoader) {
                    if (watcher.getClass() == watcherType) {
                        return Optional.of((T) watcher);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
