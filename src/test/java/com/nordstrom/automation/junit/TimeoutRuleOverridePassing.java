package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class TimeoutRuleOverridePassing {
    
    @Rule
    public Timeout globalTimeout = Timeout.millis(1000);

    @Test
    public void testRuleOverridePassed() throws InterruptedException {
        System.out.println("testRuleOverridePassed");
        TimeUnit.MILLISECONDS.sleep(600);
        assertTrue(true);
    }
    
    @Test(timeout = 1500)
    public void testRuleOverridePassedWithSpecifiedTimeout() throws InterruptedException {
        System.out.println("testRuleOverridePassedWithSpecifiedTimeout");
        TimeUnit.MILLISECONDS.sleep(1100);
        assertTrue(true);
    }

}
