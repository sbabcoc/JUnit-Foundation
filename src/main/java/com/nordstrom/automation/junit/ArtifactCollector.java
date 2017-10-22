package com.nordstrom.automation.junit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.nordstrom.common.file.PathUtils;

public class ArtifactCollector<T extends ArtifactType> extends TestWatcher {
    
    private static final Map<Description, List<ArtifactCollector<? extends ArtifactType>>> watcherMap =
                    new ConcurrentHashMap<>();
    
    private final T provider;
    private final Object instance;
    private Description description;
    private final List<Path> artifactPaths = new ArrayList<>();
    
    public ArtifactCollector(Object instance, T provider) {
        this.instance = instance;
        this.provider = provider;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void starting(Description description) {
        this.description = description;
        List<ArtifactCollector<? extends ArtifactType>> watcherList = watcherMap.get(description);
        if (watcherList == null) {
            watcherList = new ArrayList<>();
            watcherMap.put(description, watcherList);
        }
        watcherList.add(this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void failed(Throwable e, Description description) {
        captureArtifact(e);
    }
    
    /**
     * Capture artifact from the current test result context.
     * 
     * @param reason exception that prompted capture request; specify 'null' for on-demand capture
     * @return (optional) path at which the captured artifact was stored
     */
    public Optional<Path> captureArtifact(Throwable reason) {
        if (! provider.canGetArtifact(instance)) {
            return Optional.empty();
        }
        
        byte[] artifact = provider.getArtifact(instance, reason);
        if ((artifact == null) || (artifact.length == 0)) {
            return Optional.empty();
        }
        
        Path collectionPath = getCollectionPath();
        if (!collectionPath.toFile().exists()) {
            try {
                Files.createDirectories(collectionPath);
            } catch (IOException e) {
                String messageTemplate = "Unable to create collection directory ({}); no artifact was captured";
                provider.getLogger().warn(messageTemplate, collectionPath, e);
                return Optional.empty();
            }
        }
        
        Path artifactPath;
        try {
            artifactPath = PathUtils.getNextPath(
                            collectionPath, 
                            getArtifactBaseName(), 
                            provider.getArtifactExtension());
        } catch (IOException e) {
            provider.getLogger().warn("Unable to get output path; no artifact was captured", e);
            return Optional.empty();
        }
        
        try {
            provider.getLogger().info("Saving captured artifact to ({}).", artifactPath);
            Files.write(artifactPath, artifact);
        } catch (IOException e) {
            provider.getLogger().warn("I/O error saving to ({}); no artifact was captured", artifactPath, e);
            return Optional.empty();
        }
        
        recordArtifactPath(artifactPath);
        return Optional.of(artifactPath);
    }
    
    /**
     * Get path of directory at which to store artifacts.
     * 
     * @return path of artifact storage directory
     */
    private Path getCollectionPath() {
        Path collectionPath = Paths.get(System.getProperty("user.dir"));
        return collectionPath.resolve(provider.getArtifactPath());
    }
    
    /**
     * Get base name for artifact files for the specified test result.
     * <br><br>
     * <b>NOTE</b>: The base name is derived from the name of the current test.
     * If the method is parameterized, a hash code is computed from the parameter
     * values and appended to the base name as an 8-digit hexadecimal integer.
     * 
     * @return artifact file base name
     */
    private String getArtifactBaseName() {
        Object[] parameters = new Object[0];
        if (instance instanceof ArtifactParams) {
            parameters = ((ArtifactParams) instance).getParameters();
        }
        if (parameters.length == 0) {
            return description.getMethodName();
        } else {
            int hashcode = Arrays.deepHashCode(parameters);
            String hashStr = String.format("%08X", hashcode);
            return description.getMethodName() + "-" + hashStr;
        }
    }
    
    /**
     * Record the path at which the specified artifact was store in the indicated test result.
     * 
     * @param artifactPath path at which the captured artifact was stored 
     */
    private void recordArtifactPath(Path artifactPath) {
        artifactPaths.add(artifactPath);
    }
    
    /**
     * Retrieve the paths of artifacts that were stored in the indicated test result.
     * 
     * @return (optional) list of artifact paths
     */
    public Optional<List<Path>> retrieveArtifactPaths() {
        if (artifactPaths.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(artifactPaths);
        }
    }
    
    /**
     * Get the artifact provider object.
     * 
     * @return artifact provider object
     */
    public T getArtifactProvider() {
        return provider;
    }
    
    /**
     * Get the JUnit {@link Description} object associated with this artifact collector.
     * 
     * @return JUnit method description object
     */
    public Description getDescription() {
        return description;
    }
    
    /**
     * Get reference to an instance of the specified watcher type associated with the described method.
     * 
     * @param <S> type-specific artifact collector class
     * @param description JUnit method description object
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    @SuppressWarnings("unchecked")
    public static <S extends ArtifactCollector<? extends ArtifactType>> Optional<S>
                    getWatcher(Description description, Class<S> watcherType) {
        List<ArtifactCollector<? extends ArtifactType>> watcherList = watcherMap.get(description);
        if (watcherList != null) {
            for (ArtifactCollector<? extends ArtifactType> watcher : watcherList) {
                if (watcher.getClass() == watcherType) {
                    return Optional.of((S) watcher);
                }
            }
        }
        return Optional.empty();
    }

}
