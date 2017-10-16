package com.nordstrom.automation.junit;

import org.junit.runner.Description;

/**
 * This interface enables implementers to provide method-related parameters to the artifact capture framework.
 */
public interface JUnitArtifactParams {
    
    /**
     * Get get JUnit method description object for the current test class instance.
     * 
     * @return JUnit method description object
     */
    Description getDescription();
    
    /**
     * Get the parameters associated with this test class instance.
     * 
     * @return array of test class parameters
     */
    default Object[] getParameters() {
        return new Object[0];
    }
    
}
