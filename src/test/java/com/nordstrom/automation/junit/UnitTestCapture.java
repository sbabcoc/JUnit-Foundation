package com.nordstrom.automation.junit;

import java.nio.file.Path;
import java.util.Optional;

public class UnitTestCapture extends ArtifactCollector<UnitTestArtifact> {
    
    private Optional<Path> artifactPath;
    
    public UnitTestCapture(Object instance) {
        super(instance, new UnitTestArtifact());
    }
    
    /**
     * Capture artifact from the current test result context.
     * <br><br>
     * <b>NOTE</b>: This override is here solely to record the artifact path for the benefit of the unit tests,
     * as verification meta-data. It makes no contribution to the actual process of artifact capture
     * 
     * @return path at which the captured artifact was stored
     */
    @Override
    public Optional<Path> captureArtifact() {
        artifactPath = super.captureArtifact();
        return artifactPath;
    }
    
    /**
     * Get the path at which the captured artifact was stored.
     * 
     * @return path at which the captured artifact was stored (may be 'null')
     */
    public Optional<Path> getArtifactPath() {
        return artifactPath;
    }
    
}
