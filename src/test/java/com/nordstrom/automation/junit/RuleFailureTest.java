package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.testng.annotations.Test;

public class RuleFailureTest {

    @Test
    public void verifyRuleExceptionHandling() {
        ReferenceChecker checker = new ReferenceChecker();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(RuleThrowsException.class);
        assertFalse(result.wasSuccessful());
        assertEquals(result.getFailureCount(), 1, "Incorrect failed test count");
        Failure failure = result.getFailures().get(0);
        assertEquals(failure.getException().getClass(), RuntimeException.class, "Incorrect failure exception");
        assertEquals(failure.getMessage(), "Must be failed", "Incorrect failure message");
        ReferenceReleaseTest.checkLeakReports(checker);
    }
    
}
