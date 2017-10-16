package com.nordstrom.automation.junit;

import org.junit.Rule;
import org.junit.Test;

public class ArtifactCollectorOnDemand {
    
    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Test// (groups = {"onDemandCapture"})
    public void testOnDemandCapture() {
        System.out.println("onDemandCapture");
        watcher.captureArtifact();
    }
    
}
