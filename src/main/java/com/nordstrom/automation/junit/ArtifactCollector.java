package com.nordstrom.automation.junit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;
import com.nordstrom.common.file.PathUtils;

/**
 * This is the base class for implementations of scenario-specific artifact collectors.
 * 
 * @param <T> scenario-specific artifact type
 */
public class ArtifactCollector<T extends ArtifactType> extends AtomIdentity {
    
    private static final Map<Description, List<ArtifactCollector<? extends ArtifactType>>> watcherMap =
                    new ConcurrentHashMap<>();
    
    private final T provider;
    private final List<Path> artifactPaths = new ArrayList<>();
    
    public ArtifactCollector(Object instance, T provider) {
        super(instance);
        this.provider = provider;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void starting(Description description) {
        super.starting(description);
        List<ArtifactCollector<? extends ArtifactType>> watcherList =
                        watcherMap.computeIfAbsent(description, k -> new ArrayList<>());
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
     * @param reason impetus for capture request; may be 'null'
     * @return (optional) path at which the captured artifact was stored
     */
    public Optional<Path> captureArtifact(Throwable reason) {
        if (! provider.canGetArtifact(getInstance())) {
            return Optional.empty();
        }
        
        byte[] artifact = provider.getArtifact(getInstance(), reason);
        if ((artifact == null) || (artifact.length == 0)) {
            return Optional.empty();
        }
        
        Path collectionPath = getCollectionPath();
        if (!collectionPath.toFile().exists()) {
            try {
                Files.createDirectories(collectionPath);
            } catch (IOException e) {
                String messageTemplate = "Unable to create collection directory ({}); no artifact was captured";
                Optional.ofNullable(provider.getLogger()).ifPresent(l -> l.warn(messageTemplate, collectionPath, e));
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
            Optional.ofNullable(provider.getLogger()).ifPresent(
                            l -> l.warn("Unable to get output path; no artifact was captured", e));
            return Optional.empty();
        }
        
        try {
            Optional.ofNullable(provider.getLogger()).ifPresent(
                            l -> l.info("Saving captured artifact to ({}).", artifactPath));
            Files.write(artifactPath, artifact);
        } catch (IOException e) {
            Optional.ofNullable(provider.getLogger()).ifPresent(
                            l -> l.warn("I/O error saving to ({}); no artifact was captured", artifactPath, e));
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
        Path collectionPath = PathUtils.ReportsDirectory.getPathForObject(getInstance());
        return collectionPath.resolve(provider.getArtifactPath(getInstance()));
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
        if (getParameters().length == 0) {
            return getDescription().getMethodName();
        } else {
            int hashcode = Arrays.deepHashCode(getParameters());
            String hashStr = String.format("%08X", hashcode);
            return getDescription().getMethodName() + "-" + hashStr;
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
