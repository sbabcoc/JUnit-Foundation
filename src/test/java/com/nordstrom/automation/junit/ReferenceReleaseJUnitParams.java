package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class ReferenceReleaseJUnitParams {
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    @Test
    @Parameters({ "first test", "second test" })
    public void parameterized(String input) {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }

    @After
    public void afterMethod() {
    }
    
    @AfterClass
    public static void afterClass() {
    }
    
}
