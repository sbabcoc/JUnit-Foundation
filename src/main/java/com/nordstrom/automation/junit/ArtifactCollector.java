package com.nordstrom.automation.junit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runner.Description;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.nordstrom.common.file.PathUtils;

/**
 * This is the base class for implementations of scenario-specific artifact collectors.
 * 
 * @param <T> scenario-specific artifact type
 */
public class ArtifactCollector<T extends ArtifactType> extends AtomIdentity {
    
    private static final ConcurrentHashMap<Description, List<ArtifactCollector<? extends ArtifactType>>> watcherMap;
    private static final Function<Description, List<ArtifactCollector<? extends ArtifactType>>> newInstance;
    
    static {
        watcherMap = new ConcurrentHashMap<>();
        newInstance = new Function<Description, List<ArtifactCollector<? extends ArtifactType>>>() {
            @Override
            public List<ArtifactCollector<? extends ArtifactType>> apply(Description input) {
                return new ArrayList<>();
            }
        };
    }
    
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
                        LifecycleHooks.computeIfAbsent(watcherMap, description, newInstance);
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
            return Optional.absent();
        }
        
        byte[] artifact = provider.getArtifact(getInstance(), reason);
        if ((artifact == null) || (artifact.length == 0)) {
            return Optional.absent();
        }
        
        Path collectionPath = getCollectionPath();
        if (!collectionPath.toFile().exists()) {
            try {
                Files.createDirectories(collectionPath);
            } catch (IOException e) {
                if (provider.getLogger() != null) {
                    String messageTemplate = "Unable to create collection directory ({}); no artifact was captured";
                    provider.getLogger().warn(messageTemplate, collectionPath, e);
                }
                return Optional.absent();
            }
        }
        
        Path artifactPath;
        try {
            artifactPath = PathUtils.getNextPath(
                            collectionPath, 
                            getArtifactBaseName(), 
                            provider.getArtifactExtension());
        } catch (IOException e) {
            if (provider.getLogger() != null) {
                provider.getLogger().warn("Unable to get output path; no artifact was captured", e);
            }
            return Optional.absent();
        }
        
        try {
            if (provider.getLogger() != null) {
                provider.getLogger().info("Saving captured artifact to ({}).", artifactPath);
            }
            Files.write(artifactPath, artifact);
        } catch (IOException e) {
            if (provider.getLogger() != null) {
                provider.getLogger().warn("I/O error saving to ({}); no artifact was captured", artifactPath, e);
            }
            return Optional.absent();
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
        return collectionPath.resolve(getArtifactPath(getInstance()));
    }
    
    /**
     * Get the path at which to store artifacts.
     * <p>
     * <b>NOTE</b>: The returned path can be either relative or absolute.
     * 
     * @param instance JUnit test class instance
     * @return artifact storage path
     */
    private Path getArtifactPath(Object instance) {
        Path artifactPath = provider.getArtifactPath(instance);
        if (artifactPath == null) {
            artifactPath = PathUtils.ReportsDirectory.getPathForObject(instance);
        }
        return artifactPath;
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
        int hashcode = getParameters().hashCode();
        if (hashcode != 0) {
            String hashStr = String.format("%08X", hashcode);
            return getDescription().getMethodName() + "-" + hashStr;
        } else {
            return getDescription().getMethodName();
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
            return Optional.absent();
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
        return Optional.absent();
    }

}
