package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

public class TimeoutDisablePassing {
    
    static {
        System.setProperty(JUnitSettings.TIMEOUT_RULE.key(), "0");
    }
    
    @Test
    public void testDisablePassed() throws InterruptedException {
        System.out.println("testDisablePassed");
        TimeUnit.MILLISECONDS.sleep(1100);
        assertTrue(true);
    }
    
    @Test(timeout = 1000)
    public void testDisablePassedWithSpecifiedTimeout() throws InterruptedException {
        System.out.println("testDisablePassedWithSpecifiedTimeout");
        TimeUnit.MILLISECONDS.sleep(1100);
        assertTrue(true);
    }

}
