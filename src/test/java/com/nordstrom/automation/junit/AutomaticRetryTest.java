package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

public class AutomaticRetryTest {
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty(JUnitSettings.MAX_RETRY.key(), "3");
    }
    
    @Test
    public void testHappyPath() throws ClassNotFoundException {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryPassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(1, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(0, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        assertEquals(0, rla.getRetriedTests().size(), "Incorrect retried test count");
    }
    
    @Test(enabled = false)
    public void testPassOnRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryPassOnRetry.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(1, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(0, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        assertEquals(1, rla.getRetriedTests().size(), "Incorrect retried test count");
    }
    
    @Test(enabled = false)
    public void testFailOnRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryFailing.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(0, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(1, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        assertEquals(3, rla.getRetriedTests().size(), "Incorrect retried test count");
    }
    
    @Test
    public void testNoRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryNoRetry.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(0, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(1, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        assertEquals(0, rla.getRetriedTests().size(), "Incorrect retried test count");
    }
    
    @AfterClass
    public static void afterClass() {
        System.clearProperty(JUnitSettings.MAX_RETRY.key());
    }
    
}
