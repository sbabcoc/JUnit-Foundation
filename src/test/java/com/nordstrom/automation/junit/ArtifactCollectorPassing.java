package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ArtifactCollectorPassing extends TestBase {
    
    @Test// (groups = {"testPassed"})
    public void testPassed() {
        System.out.println("testPassed");
        assertTrue(true);
    }
    
}
