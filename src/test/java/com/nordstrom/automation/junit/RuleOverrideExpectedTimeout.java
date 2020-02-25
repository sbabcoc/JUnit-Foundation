package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

public class RuleOverrideExpectedTimeout {
    
    @Rule
    public Timeout globalTimeout = Timeout.millis(1000);

    @Test
    public void testRuleOverrideTimeout() throws InterruptedException {
        System.out.println("testRuleOverrideTimeout");
        TimeUnit.MILLISECONDS.sleep(1500);
        assertTrue(true);
    }
    
    @Test(timeout = 500)
    public void testRuleOverrideTimeoutWithSpecifiedTimeout() throws InterruptedException {
        System.out.println("testRuleOverrideTimeoutWithSpecifiedTimeout");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }

}
