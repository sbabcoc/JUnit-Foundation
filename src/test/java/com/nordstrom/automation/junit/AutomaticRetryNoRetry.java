package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutomaticRetryNoRetry {
    
    private static int count;
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    @Test
    @NoRetry
    public void testNoRetry() {
        System.out.println("testNoRetry: " + count);
        assertTrue("testNoRetry: " + count, count++ > 0);
    }
    
    @After
    public void afterMethod() {
    }
    
    @AfterClass
    public static void afterClass() {
    }
    
}
