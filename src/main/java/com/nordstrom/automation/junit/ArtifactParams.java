package com.nordstrom.automation.junit;

import org.junit.runner.Description;

import com.nordstrom.common.params.Params;

/**
 * This interface enables implementers to provide method-related parameters to the artifact capture framework.
 */
public interface ArtifactParams extends Params {
    
    /**
     * Get the JUnit method description object for the current test class instance.
     * 
     * @return JUnit method description object
     */
    Description getDescription();
}
