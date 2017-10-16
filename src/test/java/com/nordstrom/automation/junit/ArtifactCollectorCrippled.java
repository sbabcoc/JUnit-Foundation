package com.nordstrom.automation.junit;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;

public class ArtifactCollectorCrippled {
    
    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Test// (groups = {"willNotCapture"})
    public void testWillNotCapture() {
        System.out.println("willNotCapture");
        watcher.getArtifactProvider().crippleCapture();
        fail("willNotCapture");
    }
    
}
