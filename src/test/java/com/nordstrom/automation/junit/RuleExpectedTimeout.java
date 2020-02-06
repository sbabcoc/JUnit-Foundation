package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RuleExpectedTimeout {
    
    @Test
    public void testRuleTimeout() throws InterruptedException {
        System.out.println("testRuleTimeout");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }
    
    @Test(timeout = 400)
    public void testRuleTimeoutWithShorterInterval() throws InterruptedException {
        System.out.println("testRuleTimeoutWithShorterInterval");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }

    @Test(timeout = 600)
    public void testRuleTimeoutWithLongerInterval() throws InterruptedException {
        System.out.println("testRuleTimeoutWithLongerInterval");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }

}
