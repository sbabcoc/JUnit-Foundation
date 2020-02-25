package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

public class TimeoutDisableTest {
    
    @Test
    public void verifyTimeoutDisable() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(TimeoutDisablePassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 2, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
    }

    @Test
    public void verifyTimeoutDisableOverride() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(TimeoutDisableOverridePassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 2, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
    }

}
