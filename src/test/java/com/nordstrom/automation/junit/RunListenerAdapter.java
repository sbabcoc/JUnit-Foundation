package com.nordstrom.automation.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class RunListenerAdapter extends RunListener {
    
    private List<Description> m_allTestMethods = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_failedTests = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_failedAssumptions = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_ignoredTests = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_passedTests = Collections.synchronizedList(new ArrayList<>());
                
    /**
     * Called when an atomic test is about to be started.
     * 
     * @param description the description of the test that is about to be run 
     * (generally a class and method name)
     */
    @Override
    public void testStarted(Description description) throws Exception {
        m_allTestMethods.add(description);
    }

    /** 
     * Called when an atomic test fails.
     * 
     * @param failure describes the test that failed and the exception that was thrown
     */
    @Override
    public void testFailure(Failure failure) throws Exception {
        m_failedTests.add(failure.getDescription());
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
        m_ignoredTests.add(description);
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
        m_passedTests.removeAll(m_failedTests);
        m_passedTests.removeAll(m_failedAssumptions);
        m_passedTests.removeAll(m_ignoredTests);
        return m_passedTests;
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
    
}
