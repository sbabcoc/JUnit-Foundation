package com.nordstrom.automation.junit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.junit.runner.Description;

import com.nordstrom.common.file.PathUtils;

/**
 * This is the base class for implementations of scenario-specific artifact collectors.
 * 
 * @param <T> scenario-specific artifact type
 */
public class ArtifactCollector<T extends ArtifactType> extends AtomIdentity {
    
    private static final ConcurrentHashMap<Integer, List<ArtifactCollector<? extends ArtifactType>>> WATCHER_MAP;
    private static final Function<Integer, List<ArtifactCollector<? extends ArtifactType>>> NEW_INSTANCE;
    
    static {
        WATCHER_MAP = new ConcurrentHashMap<>();
        NEW_INSTANCE = new Function<Integer, List<ArtifactCollector<? extends ArtifactType>>>() {
            @Override
            public List<ArtifactCollector<? extends ArtifactType>> apply(Integer input) {
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
                        LifecycleHooks.computeIfAbsent(WATCHER_MAP, description.hashCode(), NEW_INSTANCE);
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
                if (provider.getLogger() != null) {
                    String messageTemplate = "Unable to create collection directory ({}); no artifact was captured";
                    provider.getLogger().warn(messageTemplate, collectionPath, e);
                }
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
            if (provider.getLogger() != null) {
                provider.getLogger().warn("Unable to get output path; no artifact was captured", e);
            }
            return Optional.empty();
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
        int hashcode = getParameters().hashCode();
        if (hashcode != 0) {
            String hashStr = String.format("%08X", hashcode);
            return getSanitizedName() + "-" + hashStr;
        } else {
            return getSanitizedName();
        }
    }
    
    /**
     * Get the target method name, replacing Windows file name reserved characters with '_'.
     * 
     * @return sanitized target method name
     */
    private String getSanitizedName() {
        return getDescription().getMethodName().replaceAll("[\\/:*?\"<>|]", "_");
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
        List<ArtifactCollector<? extends ArtifactType>> watcherList = WATCHER_MAP.get(description.hashCode());
        if (watcherList != null) {
            for (ArtifactCollector<? extends ArtifactType> watcher : watcherList) {
                if (watcher.getClass() == watcherType) {
                    return Optional.of((S) watcher);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Release the watchers for the specified description.
     *
     * @param description JUnit method description
     */
    static void releaseWatchersOf(Description description) {
        WATCHER_MAP.remove(description.hashCode());
    }

}
