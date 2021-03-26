package com.nordstrom.automation.junit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

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
    
    private static final Set<String> START_NOTIFIED = new CopyOnWriteArraySet<>();
    private static final Set<String> FINISH_NOTIFIED = new CopyOnWriteArraySet<>();
    private static final Map<String, Object> CHILD_TO_PARENT = new ConcurrentHashMap<>();
    private static final Map<String, AtomicTest> DESCRIPTION_TO_ATOMICTEST = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RunAnnouncer.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStarted(Description description) throws Exception {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunFinished(Result result) throws Exception {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testSuiteStarted(Description description) throws Exception {
        LOGGER.debug("testSuiteStarted: {}", description);
        Object runner = Run.getThreadRunner();
        fireRunStarted(runner);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testSuiteFinished(Description description) throws Exception {
        LOGGER.debug("testSuiteFinished: {}", description);
        Object runner = Run.getThreadRunner();
        fireRunFinished(runner);
        
        // release callables associated with runner
        RunReflectiveCall.releaseCallablesOf(runner);
        
        // release runner/child mappings
        releaseChidrenOf(runner);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) throws Exception {
        LOGGER.debug("testStarted: {}", description);
        AtomicTest atomicTest = newAtomicTest(description);
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
        AtomicTest atomicTest = getAtomicTestOf(description);
        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testFinished(atomicTest);
        }
        
        // release atomic test for this description
        RunAnnouncer.releaseAtomicTestOf(description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        LOGGER.debug("testFailure: {}", failure);
        AtomicTest atomicTest = setTestFailure(failure);
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
        AtomicTest atomicTest = setTestFailure(failure);
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
        // determine if retrying a failed invocation
        AtomicTest atomicTest = getAtomicTestOf(description);

        // if actually ignored
        if (atomicTest == null) {
            // create new atomic test object
            atomicTest = newAtomicTest(description);
        }

        for (RunWatcher watcher : LifecycleHooks.getRunWatchers()) {
            watcher.testIgnored(atomicTest);
        }
    }
    
    /**
     * Get the atomic test object for the specified method description.
     * 
     * @param description JUnit method description
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest getAtomicTestOf(Description description) {
      AtomicTest atomicTest = null;
      if (description != null) {
          // get atomic test for this description
          atomicTest = DESCRIPTION_TO_ATOMICTEST.get(toMapKey(description));
      }
      return atomicTest;
    }
    
    /**
     * Get the parent runner that owns specified child runner or framework method.
     * 
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @return {@code ParentRunner} object that owns the specified child ({@code null} for root objects)
     */
    static Object getParentOf(final Object child) {
        return CHILD_TO_PARENT.get(toMapKey(child));
    }
    
    /**
     * Release runner/child mappings.
     * 
     * @param runner JUnit test runner
     */
    static void releaseChidrenOf(Object runner) {
        List<?> children = LifecycleHooks.invoke(runner, "getChildren");
        for (Object child : children) {
            CHILD_TO_PARENT.remove(toMapKey(child));
        }
    }
    
    /**
     * Fire the {@link RunnerWatcher#runStarted(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runStarted} for the specified runner has already been fired, do nothing.
     * @param runner JUnit test runner
     * @return {@code true} if the {@code runStarted} event was fired; otherwise {@code false}
     */
    static boolean fireRunStarted(Object runner) {
        if (START_NOTIFIED.add(toMapKey(runner))) {
            List<?> children = LifecycleHooks.invoke(runner, "getChildren");
            for (Object child : children) {
                CHILD_TO_PARENT.put(toMapKey(child), runner);
            }
            
            LOGGER.debug("runStarted: {}", runner);
            for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
                watcher.runStarted(runner);
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Fire the {@link RunnerWatcher#runFinished(Object)} event for the specified runner.
     * <p>
     * <b>NOTE</b>: If {@code runFinished} for the specified runner has already been fired, do nothing.
     * 
     * @param runner JUnit test runner
     * @return {@code true} if event the {@code runFinished} was fired; otherwise {@code false}
     */
    static boolean fireRunFinished(Object runner) {
        if (FINISH_NOTIFIED.add(toMapKey(runner))) {
            LOGGER.debug("runFinished: {}", runner);
            for (RunnerWatcher watcher : LifecycleHooks.getRunnerWatchers()) {
                watcher.runFinished(runner);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Create new atomic test object for the specified description.
     * 
     * @param description description of the test that is about to be run
     * @return {@link AtomicTest} object (may be {@code null})
     */
    static AtomicTest newAtomicTest(Description description) {
        AtomicTest atomicTest = null;
        if (description.isTest()) {
            atomicTest = new AtomicTest(description);
        }
        return atomicTest;
    }
    
    /**
     * Release the atomic test object for the specified method description.
     * 
     * @param description JUnit method description
     */
    static AtomicTest releaseAtomicTestOf(Description description) {
        AtomicTest atomicTest = null;
        if (description != null) {
            // get atomic test for this runner/description
            atomicTest = DESCRIPTION_TO_ATOMICTEST.remove(toMapKey(description));
        }
        return atomicTest;
    }
    
    /**
     * Store the specified failure in the active atomic test.
     * 
     * @param failure {@link Failure} object
     * @return {@link AtomicTest} object
     */
    private static AtomicTest setTestFailure(Failure failure) {
        AtomicTest atomicTest = getAtomicTestOf(failure.getDescription());
        if (atomicTest != null) {
            atomicTest.setThrowable(failure.getException());
        }
        return atomicTest;
    }
}
