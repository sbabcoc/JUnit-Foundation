package com.nordstrom.automation.junit;

import java.util.Map;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.google.common.base.Optional;

/**
 * This is the base class for test watchers that need access to the test class instance for the current atomic test.
 * For test classes that implement the {@link ArtifactParams} interface, instance parameters can be retrieved via the
 * {@link #getParameters()} method. For test classes that don't implement {@link ArtifactParams}, this method returns
 * an empty array.
 */
public class AtomIdentity extends TestWatcher implements ArtifactParams {

    private final Object instance;
    private Description description;

    public AtomIdentity(Object instance) {
        this.instance = instance;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void starting(Description description) {
        this.description = description;
    }
    
    /**
     * Get the JUnit test class instance associated with this test watcher.
     * 
     * @return JUnit test class instance
     */
    public Object getInstance() {
        return instance;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AtomIdentity getAtomIdentity() {
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Description getDescription() {
        return description;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Map<String, Object>> getParameters() {
        if (instance instanceof ArtifactParams) {
            return ((ArtifactParams) instance).getParameters();
        }
        return Optional.absent();
    }

}
