package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;

public class ArtifactCollectorTestCases {
    
    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Test// (groups = {"testPassed"})
    public void testPassed() {
        System.out.println("testPassed");
        assertTrue(true);
    }
    
//    @Test// (groups = {"testFailed"})
//    public void testFailed() {
//        System.out.println("testFailed");
//        fail("testFailed");
//    }
//    
//    @Test// (groups = {"canNotCapture"})
//    public void testCanNotCapture() {
//        System.out.println("canNotCapture");
//        watcher.getArtifactProvider().disableCapture();
//        fail("canNotCapture");
//    }
//    
//    @Test// (groups = {"willNotCapture"})
//    public void testWillNotCapture() {
//        System.out.println("willNotCapture");
//        watcher.getArtifactProvider().crippleCapture();
//        fail("willNotCapture");
//    }
//    
//    @Test// (groups = {"onDemandCapture"})
//    public void testOnDemandCapture() {
//        System.out.println("onDemandCapture");
//        watcher.captureArtifact();
//    }
//    
}
