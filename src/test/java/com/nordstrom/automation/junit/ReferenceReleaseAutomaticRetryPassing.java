package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReferenceReleaseAutomaticRetryPassing {
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    @Test
    public void testPassed() {
        System.out.println("testPassed");
        assertTrue(true);
    }
    
    @After
    public void afterMethod() {
    }
    
    @AfterClass
    public static void afterClass() {
    }
    
}
