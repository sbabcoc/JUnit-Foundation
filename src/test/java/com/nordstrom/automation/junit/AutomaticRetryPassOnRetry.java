package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(HookInstallingRunner.class)
public class AutomaticRetryPassOnRetry {
    
    private static int count;
    
    @Test
    public void testPassOnRetry() {
        System.out.println("testPassOnRetry: " + count);
        assertTrue(count++ > 0);
    }
    
}
