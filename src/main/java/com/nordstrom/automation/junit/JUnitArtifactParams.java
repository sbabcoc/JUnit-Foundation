package com.nordstrom.automation.junit;

/**
 * This interface enables implementers to provide method-related parameters to the artifact capture framework.
 */
public interface JUnitArtifactParams {
    
    /**
     * Get the parameters associated with this test class instance.
     * 
     * @return array of test class parameters
     */
    default Object[] getParameters() {
        return new Object[0];
    }
    
}
