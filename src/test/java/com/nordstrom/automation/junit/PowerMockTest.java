package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

public class PowerMockTest {

    @Test
    public void verifyMethodInterception() {
        RunListenerAdapter rla = new RunListenerAdapter();
        ReferenceChecker checker = new ReferenceChecker();

        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        runner.addListener(checker);
        Result result = runner.run(PowerMockCases.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getPassedTests().get(0).getDisplayName(),
                "testHappyPath(com.nordstrom.automation.junit.PowerMockCases)", "Incorrect passed test name");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        ReferenceReleaseTest.checkLeakReports(checker);
    }

}
