package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ReferenceReleaseParameterized {
    
    public ReferenceReleaseParameterized(String input) {
        this.input = input;
    }
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    private String input;
    
    @Parameters
    public static Object[] data() {
        return new Object[] { "first test", "second test" };
    }
    
    @Test
    public void parameterized() {
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
