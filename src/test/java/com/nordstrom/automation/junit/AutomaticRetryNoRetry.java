package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRetryNoRetry {
    
    private static int count;
    
    @Test
    @NoRetry
    public void testNoRetry() {
        System.out.println("testNoRetry: " + count);
        assertTrue(count++ > 0);
    }
    
}
