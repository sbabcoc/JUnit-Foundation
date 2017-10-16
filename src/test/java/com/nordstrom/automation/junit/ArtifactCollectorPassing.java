package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;

public class ArtifactCollectorPassing {
    
    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Test// (groups = {"testPassed"})
    public void testPassed() {
        System.out.println("testPassed");
        assertTrue(true);
    }
    
}
