package com.nordstrom.automation.junit;

import java.nio.file.Path;
import org.slf4j.Logger;

import com.nordstrom.common.file.PathUtils;

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
     * @param reason impetus for capture request; may be 'null'
     * @return byte array containing the captured artifact; if capture fails, an empty array is returned
     */
    byte[] getArtifact(Object instance, Throwable reason);
    
    /**
     * Get the path at which to store artifacts.
     * 
     * @param instance JUnit test class instance
     * @return artifact storage path
     */
    default Path getArtifactPath(Object instance) {
        return PathUtils.ReportsDirectory.getPathForObject(instance);
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
