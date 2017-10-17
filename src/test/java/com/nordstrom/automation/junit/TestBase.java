package com.nordstrom.automation.junit;

import org.junit.Rule;
import org.junit.runner.Description;

public abstract class TestBase implements ArtifactParams {

    @Rule
    public final UnitTestCapture watcher = new UnitTestCapture(this);
    
    @Override
    public Description getDescription() {
        return watcher.getDescription();
    }
    
}
