package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

public class ReferenceReleaseTest {
    
    @Test
    public void verifyHappyPath() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(AutomaticRetryPassing.class);
        assertTrue(result.wasSuccessful());
        assertEquals(result.getRunCount(), 1);
        assertEquals(result.getFailureCount(), 0);
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyFailure() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(AutomaticRetryFailing.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 1);
        assertEquals(result.getFailureCount(), 1);
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyParameterizedReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseParameterized.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 2);
        assertEquals(result.getFailureCount(), 1);
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyJUnitParamsReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseJUnitParams.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 2);
        assertEquals(result.getFailureCount(), 1);
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyTheoriesReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseTheories.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 3);
        assertEquals(result.getFailureCount(), 2);
        checkLeakReports(checker);
    }
    
    static void checkLeakReports(ReferenceChecker checker) {
        assertEquals(checker.reportNoAtomicTests(), 0);
        assertEquals(checker.reportStatementLeaks(), 0);
        assertEquals(checker.reportWatcherLeaks(), 0);
        assertEquals(checker.reportAtomicTestLeaks(), 0);
        assertEquals(checker.reportTargetLeaks(), 0);
        assertEquals(checker.reportCallableLeaks(), 0);
        assertEquals(checker.reportDescriptionLeaks(), 0);
        assertEquals(checker.reportMethodLeaks(), 0);
        assertEquals(checker.reportRunnerLeaks(), 0);
        assertEquals(checker.reportStartFlagLeaks(), 0);
        assertEquals(checker.reportNotifierLeaks(), 0);
        assertEquals(checker.reportNotifyFlagLeaks(), 0);
        assertEquals(checker.reportParentLeaks(), 0);
        assertEquals(checker.reportRunnerStackLeak(), 0);
    }
    
}
