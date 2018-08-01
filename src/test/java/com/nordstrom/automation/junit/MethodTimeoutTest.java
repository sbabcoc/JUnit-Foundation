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
        
        assertEquals(rla.getPassedTests().size(), 2, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
    }

    @Test
    public void verifyExpectedTimeout() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(MethodExpectedTimeout.class);
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 3, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        
        verifyFailureMessages(result);
    }
    
    private static void verifyFailureMessages(Result result) {
        for (Failure failure : result.getFailures()) {
            String methodName = failure.getDescription().getMethodName();
            String expect = MESSAGE_MAP.get(methodName);
            Throwable thrown = failure.getException();
            assertEquals(thrown.getClass(), TestTimedOutException.class, "Exception class for: " + methodName);
            assertEquals(thrown.getMessage(), expect, "Failure message for: " + methodName);
        }
    }

}
