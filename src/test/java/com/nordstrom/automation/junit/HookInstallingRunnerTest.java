package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

public class HookInstallingRunnerTest {
    
    @Test
    public void verifyMethodInterception() {
        JUnitCore runner = new JUnitCore();
        Result result = runner.run(HookInstallingRunnerTestCases.class);
        assertTrue(result.wasSuccessful());
        Optional<MethodWatcher> optWatcher = MethodInterceptor.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher watcher = (UnitTestWatcher) optWatcher.get();
        assertTrue(watcher.getEnterBeforeClass().contains("unitTestBeforeClass"));
        assertTrue(watcher.getLeaveBeforeClass().contains("unitTestBeforeClass"));
        assertTrue(watcher.getEnterBeforeMethod().contains("unitTestBeforeMethod"));
        assertTrue(watcher.getLeaveBeforeMethod().contains("unitTestBeforeMethod"));
        assertTrue(watcher.getEnterTest().contains("unitTestMethod"));
        assertTrue(watcher.getLeaveTest().contains("unitTestMethod"));
        assertTrue(watcher.getEnterAfterMethod().contains("unitTestAfterMethod"));
        assertTrue(watcher.getLeaveAfterMethod().contains("unitTestAfterMethod"));
        assertTrue(watcher.getEnterAfterClass().contains("unitTestAfterClass"));
        assertTrue(watcher.getLeaveAfterClass().contains("unitTestAfterClass"));
    }

}
