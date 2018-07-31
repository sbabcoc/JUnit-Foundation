package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.model.TestTimedOutException;
import org.testng.annotations.Test;

public class MethodTimeoutTest {
    
    private static final Map<String, String> MESSAGE_MAP;
    
    static {
        MESSAGE_MAP = new HashMap<>();
        MESSAGE_MAP.put("testTimeout", "test timed out after 500 milliseconds");
        MESSAGE_MAP.put("testTimeoutWithShorterInterval", "test timed out after 500 milliseconds");
        MESSAGE_MAP.put("testTimeoutWithLongerInterval", "test timed out after 600 milliseconds");
    }

    @Test
    public void verifyHappyPath() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(MethodTimeoutPassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(2, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(0, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
    }

    @Test(enabled = false)
    public void verifyExpectedTimeout() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(MethodExpectedTimeout.class);
        
        assertEquals(0, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(3, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        
        verifyFailureMessages(result);
    }
    
    private static void verifyFailureMessages(Result result) {
        for (Failure failure : result.getFailures()) {
            String methodName = failure.getDescription().getMethodName();
            String expect = MESSAGE_MAP.get(methodName);
            Throwable thrown = failure.getException();
            assertEquals(TestTimedOutException.class, thrown.getClass(), "Exception class for: " + methodName);
            String actual = thrown.getMessage();
            assertEquals("Failure message for: " + methodName, expect, actual);
        }
    }

}
