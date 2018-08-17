package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class AutomaticRetryIgnore {
    
    @Test
    @Ignore
    public void testIgnore() {
        System.out.println("testIgnore");
        assertTrue(true);
    }
    
}
