package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HookInstallationTestCases {
    
    @BeforeClass
    public static void unitTestBeforeClass() {
    }
    
    @Before
    public void unitTestBeforeMethod() {
    }
    
    @Test
    public void unitTestMethod() {
        assertTrue(true);
    }
    
    @After
    public void unitTestAfterMethod() {
    }
    
    @AfterClass
    public static void unitTestAfterClass() {
    }
    
}
