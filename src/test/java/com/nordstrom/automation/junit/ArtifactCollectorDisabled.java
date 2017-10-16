package com.nordstrom.automation.junit;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;

public class ArtifactCollectorDisabled {
    
    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Test// (groups = {"canNotCapture"})
    public void testCanNotCapture() {
        System.out.println("canNotCapture");
        watcher.getArtifactProvider().disableCapture();
        fail("canNotCapture");
    }
    
}
