package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(HookInstallingRunner.class)
public class AutomaticRetryFailing {
    
    @Test
    public void testFailed() {
        System.out.println("testFailed");
        assertTrue(false);
    }
    
}
