package com.nordstrom.automation.junit;

import static org.junit.Assert.fail;

import org.junit.Test;

public class ArtifactCollectorFailing extends TestBase {
    
    @Test// (groups = {"testFailed"})
    public void testFailed() {
        System.out.println("testFailed");
        fail("testFailed");
    }
    
}
