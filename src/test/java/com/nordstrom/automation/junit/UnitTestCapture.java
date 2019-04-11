package com.nordstrom.automation.junit;

import java.nio.file.Path;

import com.google.common.base.Optional;

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
     * @param reason impetus for capture request; may be 'null'
     * @return path at which the captured artifact was stored
     */
    @Override
    public Optional<Path> captureArtifact(Throwable reason) {
        artifactPath = super.captureArtifact(reason);
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
