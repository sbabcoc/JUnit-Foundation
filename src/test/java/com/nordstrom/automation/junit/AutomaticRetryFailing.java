package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRetryFailing {
    
    @Test
    public void testFailed() {
        System.out.println("testFailed");
        assertTrue("testFailed", false);
    }
    
}
