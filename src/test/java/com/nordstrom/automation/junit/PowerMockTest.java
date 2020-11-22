package com.nordstrom.automation.junit;

import static org.junit.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

public class PowerMockTest {

    @Test
    public void verifyMethodInterception() {
        RunListenerAdapter rla = new RunListenerAdapter();

        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(PowerMockCases.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
    }

}
