package com.nordstrom.automation.junit;

import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class RunAnnouncer extends RunListener {
    
    private static final ServiceLoader<RunWatcher> runWatcherLoader;
    
    static {
        runWatcherLoader = ServiceLoader.load(RunWatcher.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        AtomicTest atomicTest = getAtomicTest(description);
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
        AtomicTest atomicTest = getAtomicTest(description);
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
        AtomicTest atomicTest = getAtomicTest(failure);
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
        AtomicTest atomicTest = getAtomicTest(failure);
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
        AtomicTest atomicTest = getAtomicTest(description);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testIgnored(atomicTest);
            }
        }
    }
    
    /**
     * Get the atomic test object from the specified description.
     * 
     * @param description {@link Description} object
     * @return {@link AtomicTest} object
     */
    static AtomicTest getAtomicTest(Description description) {
        return new AtomicTest(Run.getThreadRunner(), description);
    }
    
    static AtomicTest getAtomicTest(Failure failure) {
        AtomicTest atomicTest = getAtomicTest(failure.getDescription());
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
