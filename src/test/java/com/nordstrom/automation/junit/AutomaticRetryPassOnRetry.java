package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRetryPassOnRetry {
    
    private static int count;
    
    @Test
    public void testPassOnRetry() {
        System.out.println("testPassOnRetry: " + count);
        assertTrue("testPassOnRetry: " + count, count++ > 0);
    }
    
}
