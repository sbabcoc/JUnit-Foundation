package com.nordstrom.automation.junit;

import org.junit.Test;

public class ArtifactCollectorOnDemand extends TestBase {
    
    @Test// (groups = {"onDemandCapture"})
    public void testOnDemandCapture() {
        System.out.println("onDemandCapture");
        watcher.captureArtifact();
    }
    
}
