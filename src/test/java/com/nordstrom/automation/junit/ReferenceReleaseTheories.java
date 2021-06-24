package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class ReferenceReleaseTheories {
    
    @BeforeClass
    public static void beforeClass() {
    }
    
    @Before
    public void beforeMethod() {
    }
    
    @DataPoints
    public static String[] data() {
        return new String[] { "first test", "second test" };
    }

    @Theory
    public void theory(final String input) {
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
