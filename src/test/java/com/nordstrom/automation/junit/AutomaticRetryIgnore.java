package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class AutomaticRetryIgnore {
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    @Test
    @Ignore
    public void testIgnore() {
        System.out.println("testIgnore");
        assertTrue(true);
    }
    
    @After
    public void afterMethod() {
    }
    
    @AfterClass
    public static void afterClass() {
    }
    
}
