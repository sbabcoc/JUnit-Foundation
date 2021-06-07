package com.nordstrom.automation.junit;

import static org.testng.Assert.assertTrue;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

public class HookInstallationTest {
    
    @Test
    public void verifyMethodInterception() {
        JUnitCore runner = new JUnitCore();
        Result result = runner.run(HookInstallationTestCases.class);
        assertTrue(result.wasSuccessful());
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
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
