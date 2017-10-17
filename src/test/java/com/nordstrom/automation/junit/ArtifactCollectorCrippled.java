package com.nordstrom.automation.junit;

import static org.junit.Assert.fail;

import org.junit.Test;

public class ArtifactCollectorCrippled extends TestBase {
    
    @Test// (groups = {"willNotCapture"})
    public void testWillNotCapture() {
        System.out.println("willNotCapture");
        watcher.getArtifactProvider().crippleCapture();
        fail("willNotCapture");
    }

}
