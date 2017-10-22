package com.nordstrom.automation.junit;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

public interface ArtifactType {

    /**
     * Get the SLF4J {@link Logger} for this artifact type.
     * 
     * @return logger for this artifact
     */
    Logger getLogger();
    
    /**
     * Determine if artifact capture is available in the specified context.
     * 
     * @param instance JUnit test class instance
     * @return 'true' if capture is available; otherwise 'false'
     */
    boolean canGetArtifact(Object instance);
    
    /**
     * Capture an artifact from the specified context.
     * 
     * @param instance JUnit test class instance
     * @param reason exception that prompted capture request; specify 'null' for on-demand capture
     * @return byte array containing the captured artifact; if capture fails, an empty array is returned
     */
    byte[] getArtifact(Object instance, Throwable reason);
    
    /**
     * Get the path at which to store artifacts.
     * 
     * @return artifact storage path
     */
    default Path getArtifactPath() {
        return Paths.get("");
    }
    
    /**
     * Get the extension for artifact files of this type.
     * <br><br>
     * <b>NOTE</b>: The returned path can be either relative or absolute.
     * 
     * @return artifact file extension
     */
    String getArtifactExtension();
    
}
