package com.nordstrom.automation.junit;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;

public class ArtifactCollectorFailing {
    
    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Test// (groups = {"testFailed"})
    public void testFailed() {
        System.out.println("testFailed");
        fail("testFailed");
    }
    
}
