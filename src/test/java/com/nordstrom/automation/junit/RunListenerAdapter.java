package com.nordstrom.automation.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.google.common.base.Optional;

/**
 * This run listener tracks the results of executed tests.
 */
public class RunListenerAdapter extends RunListener {
    
    private List<Description> m_allTestMethods = Collections.synchronizedList(new ArrayList<Description>());
    private List<Failure> m_testFailures = Collections.synchronizedList(new ArrayList<Failure>());
    private List<Description> m_failedTests = Collections.synchronizedList(new ArrayList<Description>());
    private List<Failure> m_testAssumptionFailures = Collections.synchronizedList(new ArrayList<Failure>());
    private List<Description> m_failedTestAssumptions = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_ignoredTests = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_retriedTests = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_passedTests = Collections.synchronizedList(new ArrayList<Description>());
    
    private List<Description> m_allTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Failure> m_theoryFailures = Collections.synchronizedList(new ArrayList<Failure>());
    private List<Description> m_failedTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Failure> m_theoryAssumptionFailures = Collections.synchronizedList(new ArrayList<Failure>());
    private List<Description> m_failedTheoryAssumptions = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_ignoredTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_retriedTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_passedTheories = Collections.synchronizedList(new ArrayList<Description>());
    private ConcurrentHashMap<Integer, UnitTestCapture> watcherMap = new ConcurrentHashMap<>();
                
    /**
     * Called when an atomic test is about to be started.
     * 
     * @param description the description of the test that is about to be run 
     * (generally a class and method name)
     */
    @Override
    public void testStarted(Description description) throws Exception {
        if (isTheory(description)) {
            m_allTheories.add(description);
        } else {
            m_allTestMethods.add(description);
        }
    }

    /** 
     * Called when an atomic test fails.
     * 
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        if (isTheory(failure.getDescription())) {
            m_theoryFailures.add(failure);
            m_failedTheories.add(failure.getDescription());
        } else {
            m_testFailures.add(failure);
            m_failedTests.add(failure.getDescription());
        }
    }

    /**
     * Called when an atomic test flags that it assumes a condition that is
     * false
     * 
     * @param failure
     *            describes the test that failed and the
     *            {@link AssumptionViolatedException} that was thrown
     */
    @Override
    public void testAssumptionFailure(Failure failure) {
        if (isTheory(failure.getDescription())) {
            m_theoryAssumptionFailures.add(failure);
            m_failedTheoryAssumptions.add(failure.getDescription());
        } else {
            m_testAssumptionFailures.add(failure);
            m_failedTestAssumptions.add(failure.getDescription());
        }
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated 
     * with {@link org.junit.Ignore}.
     * 
     * @param description describes the test that will not be run
     */
    @Override
    public void testIgnored(Description description) throws Exception {
        if (RetriedTest.isRetriedTest(description)) {
            if (isTheory(description)) {
                m_retriedTheories.add(description);
            } else {
                m_retriedTests.add(description);
            }
        } else {
            if (isTheory(description)) {
                m_ignoredTheories.add(description);
            } else {
                m_ignoredTests.add(description);
            }
        }
    }
    
    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     * 
     * @param description the description of the test that just ran
     */
    @Override
    public void testFinished(Description description) {
        Optional<UnitTestCapture> watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class);
        if (watcher.isPresent()) watcherMap.put(description.hashCode(), watcher.get());
    }
    
    /**
     * Get list of all tests that were run.
     * 
     * @return list of all tests
     */
    public List<Description> getAllTestMethods() {
        return m_allTestMethods;
    }
    
    /**
     * Get list of passed tests.
     * 
     * @return list of passed tests
     */
    public List<Description> getPassedTests() {
        m_passedTests.clear();
        m_passedTests.addAll(m_allTestMethods);
        for (Description description : m_failedTests) { m_passedTests.remove(description); }
        for (Description description : m_failedTestAssumptions) { m_passedTests.remove(description); }
        for (Description description : m_ignoredTests) { m_passedTests.remove(description); }
        for (Description description : m_retriedTests) { m_passedTests.remove(description); }
        return m_passedTests;
    }
    
    /**
     * Get list of test failure objects.
     * 
     * @return list of failure objects
     */
    public List<Failure> getTestFailures() {
        return m_testFailures;
    }

    /**
     * Get list of failed tests.
     * 
     * @return list of failed tests
     */
    public List<Description> getFailedTests() {
        return m_failedTests;
    }
    
    /**
     * Get list of test assumption failures.
     * 
     * @return list of test assumption failures
     */
    public List<Failure> getTestAssumptionFailures() {
        return m_testAssumptionFailures;
    }
    
    /**
     * Get list of failed test assumptions.
     * 
     * @return list of failed test assumptions
     */
    public List<Description> getFailedTestAssumptions() {
        return m_failedTestAssumptions;
    }
    
    /**
     * Get list of ignored tests.
     * 
     * @return list of ignored tests
     */
    public List<Description> getIgnoredTests() {
        return m_ignoredTests;
    }
    
    /**
     * Get list of retried tests.
     * 
     * @return list of retried tests
     */
    public List<Description> getRetriedTests() {
        return m_retriedTests;
    }

    /**
     * Get list of all theory methods that were run.
     * 
     * @return list of all theories
     */
    public List<Description> getAllTheories() {
        return m_allTheories;
    }

    /**
     * Get list of passed theories.
     * 
     * @return list of passed theories
     */
    public List<Description> getPassedTheories() {
        m_passedTheories.clear();
        m_passedTheories.addAll(m_allTheories);
        for (Description description : m_failedTheories) { m_passedTheories.remove(description); }
        for (Description description : m_failedTheoryAssumptions) { m_passedTheories.remove(description); }
        for (Description description : m_ignoredTheories) { m_passedTheories.remove(description); }
        for (Description description : m_retriedTheories) { m_passedTheories.remove(description); }
        return m_passedTheories;
    }
    
    /**
     * Get list of theory failure objects.
     * 
     * @return list of theory failure objects
     */
    public List<Failure> getTheoryFailures() {
        return m_theoryFailures;
    }

    /**
     * Get list of failed theories.
     * 
     * @return list of failed theories
     */
    public List<Description> getFailedTheories() {
        return m_failedTheories;
    }

    /**
     * Get list of theory assumption failures.
     * 
     * @return list of theory assumption failures
     */
    public List<Failure> getTheoryAssumptionFailures() {
        return m_theoryAssumptionFailures;
    }
    
    /**
     * Get list of failed theory assumptions.
     * 
     * @return list of failed theory assumptions
     */
    public List<Description> getFailedTheoryAssumptions() {
        return m_failedTheoryAssumptions;
    }
    
    /**
     * Get list of ignored theories.
     * 
     * @return list of ignored theories
     */
    public List<Description> getIgnoredTheories() {
        return m_ignoredTheories;
    }

    /**
     * Get list of retried theories.
     * 
     * @return list of retried theories
     */
    public List<Description> getRetriedTheories() {
        return m_retriedTheories;
    }

    /**
     * Get unit test watcher registered for the specified description.
     * 
     * @param description JUnit method description
     * @return {@link UnitTestCapture} object; may be {@code null}
     */
    public UnitTestCapture getWatcher(Description description) {
        return watcherMap.get(description.hashCode());
    }

    /**
     * Determine if the specified description represents a theory.
     * 
     * @param description description of test "identity" method
     * @return {@code true} if description represents a theory; otherwise {@code false}
     */
    public boolean isTheory(Description description) {
        AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(description);
        return ((atomicTest != null) && (atomicTest.isTheory()));
    }

}
