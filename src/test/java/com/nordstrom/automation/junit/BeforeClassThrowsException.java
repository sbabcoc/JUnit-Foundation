package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

public class BeforeClassThrowsException extends TestBase {
    
    @BeforeClass
    public static void failing() {
        throw new RuntimeException("Must be failed");
    }
    
    @Test
    public void happy() {
        assertTrue(true);
    }

    @Test
    public void sad() {
        assertTrue(false);
    }

}
