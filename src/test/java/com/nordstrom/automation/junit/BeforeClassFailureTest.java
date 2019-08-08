package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.testng.annotations.Test;

public class BeforeClassFailureTest {

    @Test
    public void verifyBeforeClassExceptionHandling() {
        JUnitCore runner = new JUnitCore();
        Result result = runner.run(BeforeClassThrowsException.class);
        assertFalse(result.wasSuccessful());
        assertEquals(1, result.getFailureCount());
        Failure failure = result.getFailures().get(0);
        assertEquals(RuntimeException.class, failure.getException().getClass());
        assertEquals("Must be failed", failure.getMessage());
    }
    
}
