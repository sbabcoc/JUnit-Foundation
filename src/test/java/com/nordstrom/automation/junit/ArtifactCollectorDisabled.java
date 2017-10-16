package com.nordstrom.automation.junit;

import static org.junit.Assert.fail;

import org.junit.Test;

public class ArtifactCollectorDisabled extends TestBase {
    
    @Test// (groups = {"canNotCapture"})
    public void testCanNotCapture() {
        System.out.println("canNotCapture");
        watcher.getArtifactProvider().disableCapture();
        fail("canNotCapture");
    }
    
}
