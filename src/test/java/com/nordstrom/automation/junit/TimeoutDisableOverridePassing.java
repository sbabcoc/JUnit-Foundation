package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

public class TimeoutDisableOverridePassing {
    
    static {
        System.setProperty(JUnitSettings.TIMEOUT_RULE.key(), "500");
    }
    
    @Rule
    public Timeout globalTimeout = Timeout.millis(0);

    @Test
    public void testDisableOverridePassed() throws InterruptedException {
        System.out.println("testDisableOverridePassed");
        TimeUnit.MILLISECONDS.sleep(1100);
        assertTrue(true);
    }
    
    @Test(timeout = 1000)
    public void testDisableOverridePassedWithSpecifiedTimeout() throws InterruptedException {
        System.out.println("testDisableOverridePassedWithSpecifiedTimeout");
        TimeUnit.MILLISECONDS.sleep(1100);
        assertTrue(true);
    }

}
