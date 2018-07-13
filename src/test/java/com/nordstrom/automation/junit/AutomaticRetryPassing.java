package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRetryPassing {
    
    @Test
    public void testPassed() {
        System.out.println("testPassed");
        assertTrue(true);
    }
    
}
