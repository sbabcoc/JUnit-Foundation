package com.nordstrom.automation.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private List<Failure> m_assumptionFailures = Collections.synchronizedList(new ArrayList<Failure>());
    private List<Description> m_failedAssumptions = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_ignoredTests = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_retriedTests = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_passedTests = Collections.synchronizedList(new ArrayList<Description>());
    
    private List<Description> m_allTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Failure> m_theoryFailures = Collections.synchronizedList(new ArrayList<Failure>());
    private List<Description> m_failedTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_ignoredTheories = Collections.synchronizedList(new ArrayList<Description>());
    private List<Description> m_passedTheories = Collections.synchronizedList(new ArrayList<Description>());
    private Optional<UnitTestCapture> watcher;
                
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
        m_assumptionFailures.add(failure);
        m_failedAssumptions.add(failure.getDescription());
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated 
     * with {@link org.junit.Ignore}.
     * 
     * @param description describes the test that will not be run
     */
    @Override
    public void testIgnored(Description description) throws Exception {
        if (isTheory(description)) {
            m_ignoredTheories.add(description);
        } else {
            if (null != description.getAnnotation(RetriedTest.class)) {
                m_retriedTests.add(description);
            } else {
                m_ignoredTests.add(description);
            }
        }
    }
    
    @Override
    public void testFinished(Description description) {
    	watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class);
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
        for (Description description : m_failedTheories) { m_passedTests.remove(description); }
        for (Description description : m_failedAssumptions) { m_passedTests.remove(description); }
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
     * Get list of assumption failures.
     * 
     * @return list of assumption failures
     */
    public List<Failure> getAssumptionFailures() {
        return m_assumptionFailures;
    }
    
    /**
     * Get list of failed assumptions.
     * 
     * @return list of failed assumptions
     */
    public List<Description> getFailedAssumptions() {
        return m_failedAssumptions;
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
    null
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
        for (Description description : m_ignoredTests) { m_passedTheories.remove(description); }
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
     * Get list of ignored theories.
     * 
     * @return list of ignored theories
     */
    public List<Description> getIgnoredTheories() {
        return m_ignoredTheories;
    }
    
    public UnitTestCapture getWatcher() {
    	return (watcher.isPresent()) ? watcher.get() : null;
    }
    
    /**
     * Determine if the specified description represents a theory.
     * 
     * @param description description of test "identity" method
     * @return {@code true} if description represents a theory; otherwise {@code false}
     */
    private boolean isTheory(Description description) {
        AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(description);
        return ((atomicTest != null) && (atomicTest.isTheory()));
    }

}
