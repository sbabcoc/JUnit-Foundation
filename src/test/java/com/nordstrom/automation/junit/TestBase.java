package com.nordstrom.automation.junit;

import java.util.Map;

import org.junit.Rule;
import org.junit.runner.Description;

import com.google.common.base.Optional;

public abstract class TestBase implements ArtifactParams {

    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Override
    public AtomIdentity getAtomIdentity() {
        return watcher;
    }
    
    @Override
    public Description getDescription() {
        return watcher.getDescription();
    }

    @Override
    public Optional<Map<String, Object>> getParameters() {
        return Optional.absent();
    }
}
