package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutomaticRetryPassOnRetry {
    
    private static int count;
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    @Test
    public void testPassOnRetry() {
        System.out.println("testPassOnRetry: " + count);
        assertTrue("testPassOnRetry: " + count, count++ > 0);
    }
    
    @After
    public void afterMethod() {
    }
    
    @AfterClass
    public static void afterClass() {
    }
    
}
