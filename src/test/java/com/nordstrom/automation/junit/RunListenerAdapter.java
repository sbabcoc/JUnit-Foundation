package com.nordstrom.automation.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class RunListenerAdapter extends RunListener {
    
    private List<Description> m_allTestMethods = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_failedTests = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_failedAssumptions = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_ignoredTests = Collections.synchronizedList(new ArrayList<>());
    private List<Description> m_passedTests = Collections.synchronizedList(new ArrayList<>());
                
    /**
     * Called before any tests have been run.
     * @param description describes the tests to be run
     */
    @Override
    public void testRunStarted(Description description) throws Exception {
    }
    
    /**
     * Called when all tests have finished
     * @param result the summary of the test run, including all the tests that failed
     */
    @Override
    public void testRunFinished(Result result) throws Exception {
    }
    
    /**
     * Called when an atomic test is about to be started.
     * @param description the description of the test that is about to be run 
     * (generally a class and method name)
     */
    @Override
    public void testStarted(Description description) throws Exception {
        m_allTestMethods.add(description);
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     * @param description the description of the test that just ran
     */
    @Override
    public void testFinished(Description description) throws Exception {
    }

    /** 
     * Called when an atomic test fails.
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
    
    public List<Description> getAllTestMethods() {
        return m_allTestMethods;
    }
    
    public List<Description> getPassedTests() {
        m_passedTests.clear();
        m_passedTests.addAll(m_allTestMethods);
        m_passedTests.removeAll(m_failedTests);
        m_passedTests.removeAll(m_failedAssumptions);
        m_passedTests.removeAll(m_ignoredTests);
        return m_passedTests;
    }
    
    public List<Description> getFailedTests() {
        return m_failedTests;
    }
    
    public List<Description> getFailedAssumptions() {
        return m_failedAssumptions;
    }
    
    public List<Description> getIgnoredTests() {
        return m_ignoredTests;
    }
    
}
