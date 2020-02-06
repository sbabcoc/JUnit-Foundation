package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TimeoutRulePassing {
    
    @Test
    public void testRulePassed() {
        System.out.println("testRulePassed");
        assertTrue(true);
    }
    
    @Test(timeout = 1000)
    public void testRulePassedWithSpecifiedTimeout() throws InterruptedException {
        System.out.println("testRulePassedWithSpecifiedTimeout");
        TimeUnit.MILLISECONDS.sleep(600);
        assertTrue(true);
    }

}
