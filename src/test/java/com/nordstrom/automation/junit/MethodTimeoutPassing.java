package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

@RunWith(HookInstallingRunner.class)
public class MethodTimeoutPassing {
    
    static {
        System.setProperty(JUnitSettings.TEST_TIMEOUT.key(), "500");
    }
    
    @Test
    public void testPassed() {
        System.out.println("testPassed");
        assertTrue(true);
    }
    
    @Test(timeout = 1000)
    public void testPassedWithSpecifiedTimeout() throws InterruptedException {
        System.out.println("testPassedWithSpecifiedTimeout");
        TimeUnit.MILLISECONDS.sleep(600);
        assertTrue(true);
    }

}
