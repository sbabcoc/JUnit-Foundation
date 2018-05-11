package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

public class AutomaticRetryTest {
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty(JUnitSettings.MAX_RETRY.key(), "3");
    }
    
    @Test
    public void testHappyPath() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryPassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 1, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 0, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
    }
    
    @Test
    public void testPassOnRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryPassOnRetry.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 1, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 0, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 1, rla.getIgnoredTests().size());
    }
    
    @Test
    public void testFailOnRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryFailing.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 0, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 1, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 3, rla.getIgnoredTests().size());
    }
    
    @Test
    public void testNoRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryNoRetry.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 0, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 1, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
    }
    
    @AfterClass
    public static void afterClass() {
        System.clearProperty(JUnitSettings.MAX_RETRY.key());
    }
    
}
