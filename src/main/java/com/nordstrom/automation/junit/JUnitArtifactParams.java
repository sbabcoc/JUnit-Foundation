package com.nordstrom.automation.junit;

public interface JUnitArtifactParams {
    
    /**
     * 
     * @return
     */
    default Object[] getParameters() {
        return new Object[0];
    }
    
}
