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
        JUnitCore runner = new JUnitCore();
        Result result = runner.run(RuleThrowsException.class);
        assertFalse(result.wasSuccessful());
        assertEquals(1, result.getFailureCount());
        Failure failure = result.getFailures().get(0);
        assertEquals(RuntimeException.class, failure.getException().getClass());
        assertEquals("Must be failed", failure.getMessage());
    }
    
}
