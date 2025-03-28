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
        assertEquals(result.getRunCount(), 1, "Incorrect test run count");
        assertEquals(result.getFailureCount(), 0, "Incorrect failed test count");
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyFailure() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(AutomaticRetryFailing.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 1, "Incorrect test run count");
        assertEquals(result.getFailureCount(), 1, "Incorrect failed test count");
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyParameterizedReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseParameterized.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 2, "Incorrect test run count");
        assertEquals(result.getFailureCount(), 1, "Incorrect failed test count");
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyJUnitParamsReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseJUnitParams.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 2, "Incorrect test run count");
        assertEquals(result.getFailureCount(), 1, "Incorrect failed test count");
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyTheoriesReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseTheories.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 3, "Incorrect test run count");
        assertEquals(result.getFailureCount(), 2, "Incorrect failed test count");
        checkLeakReports(checker);
    }
    
    @Test
    public void verifyParamInjectorReferenceRelease() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(ReferenceReleaseParamInjector.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getRunCount(), 2, "Incorrect test run count");
        assertEquals(result.getFailureCount(), 1, "Incorrect failed test count");
        checkLeakReports(checker);
    }
    
    static void checkLeakReports(ReferenceChecker checker) {
        assertEquals(checker.reportNoAtomicTests(), 0, "Missing atomic tests detected");
        assertEquals(checker.reportStatementLeaks(), 0, "Leaked statements detected");
        assertEquals(checker.reportWatcherLeaks(), 0, "Leaked watchers detected");
        assertEquals(checker.reportAtomicTestLeaks(), 0, "Leaked atomic tests detected");
        assertEquals(checker.reportTargetLeaks(), 0, "Leaked target references detected");
        assertEquals(checker.reportCallableLeaks(), 0, "Leaked Callable references detected");
        assertEquals(checker.reportDescriptionLeaks(), 0, "Leaked Description references detected");
        assertEquals(checker.reportMethodLeaks(), 0, "Leaked Method references detected");
        assertEquals(checker.reportRunnerLeaks(), 0, "Leaked Runner references detected");
        assertEquals(checker.reportStartFlagLeaks(), 0, "Leaked 'start' flags detected");
        assertEquals(checker.reportNotifierLeaks(), 0, "Leaked Notifier referenced detected");
        assertEquals(checker.reportNotifyFlagLeaks(), 0, "Leaked 'notify' flags detected");
        assertEquals(checker.reportParentLeaks(), 0, "Leaked parent runner references detected");
        assertEquals(checker.reportRunnerStackLeak(), 0, "Leaked runner stack entries detected");
    }
    
}
