package com.nordstrom.automation.junit;

import java.nio.file.Path;
import org.slf4j.Logger;

import com.nordstrom.common.file.PathUtils;

/**
 * This interface defines the contract fulfilled by artifact capture providers. Instances of this interface supply the
 * scenario-specific implementation for artifact capture through the {@link ArtifactCollector} listener.
 * <p>
 * <b>IMPLEMENTING ARTIFACTTYPE</b>
 * <pre><code>
 * package com.nordstrom.example;
 * 
 * import java.nio.file.Path;
 * 
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 * import org.testng.ITestResult;
 * 
 * import com.nordstrom.automation.junit.ArtifactType;
 * 
 * public class MyArtifactType extends ArtifactType {
 *     
 *     private static final String ARTIFACT_PATH = "artifacts";
 *     private static final String EXTENSION = "txt";
 *     private static final String ARTIFACT = "This text artifact was captured for '%s'";
 *     private static final Logger LOGGER = LoggerFactory.getLogger(MyArtifactType.class);
 * 
 *     &#64;Override
 *     public boolean canGetArtifact(Object instance) {
 *         return true;
 *     }
 * 
 *     &#64;Override
 *     public byte[] getArtifact(Object instance, Throwable reason) {
 *         return String.format(ARTIFACT, instance.getSimpleName()).getBytes().clone();
 *     }
 *     
 *     &#64;Override
 *     public Path getArtifactPath(Object instance) {
 *         return super.getArtifactPath(instance).resolve(ARTIFACT_PATH);
 *     }
 * 
 *     &#64;Override
 *     public String getArtifactExtension() {
 *         return EXTENSION;
 *     }
 *     
 *     &#64;Override
 *     public Logger getLogger() {
 *         return LOGGER;
 *     }
 * }
 * </code></pre>
 * <b>CREATING A TYPE-SPECIFIC ARTIFACT COLLECTOR</b>
 * <pre><code>
 * package com.nordstrom.example;
 * 
 * import com.nordstrom.automation.testng.ArtifactCollector;
 * 
 * public class MyArtifactCapture extends ArtifactCollector&lt;MyArtifactType&gt; {
 *     
 *     public MyArtifactCapture() {
 *         super(new MyArtifactType());
 *     }
 * }
 * </code></pre>
 */
public abstract class ArtifactType {

    /**
     * Default constructor
     */
    public ArtifactType() { }
    
    /**
     * Get the SLF4J {@link Logger} for this artifact type.
     * 
     * @return logger for this artifact (may be {@code null})
     */
    public Logger getLogger() {
        return null;
    }
    
    /**
     * Determine if artifact capture is available in the specified context.
     * 
     * @param instance JUnit test class instance
     * @return 'true' if capture is available; otherwise 'false'
     */
    public abstract boolean canGetArtifact(Object instance);
    
    /**
     * Capture an artifact from the specified context.
     * 
     * @param instance JUnit test class instance
     * @param reason impetus for capture request; may be 'null'
     * @return byte array containing the captured artifact; if capture fails, an empty array is returned
     */
    public abstract byte[] getArtifact(Object instance, Throwable reason);
    
    /**
     * Get the path at which to store artifacts.
     * <p>
     * <b>NOTE</b>: The returned path can be either relative or absolute.
     * 
     * @param instance JUnit test class instance
     * @return artifact storage path
     */
    public Path getArtifactPath(Object instance) {
        return PathUtils.ReportsDirectory.getPathForObject(instance);
    }
    
    /**
     * Get the extension for artifact files of this type.
     * 
     * @return artifact file extension
     */
    public abstract String getArtifactExtension();
    
}
