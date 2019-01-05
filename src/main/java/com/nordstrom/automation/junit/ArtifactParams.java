package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.Optional;

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
    
    /**
     * Assemble a map of test class instance parameters.
     * 
     * @param params array of {@link Param} objects; may be {@code null} or empty
     * @return optional map of parameters (may be empty)
     */
    public static Optional<Map<String, Object>> mapOf(Param... params) {
        return Params.mapOf(params);
    }
    
    /**
     * Create a test parameter object for the specified key/value pair.
     * 
     * @param key test parameter key (name)
     * @param val test parameter value
     * @return test parameter object
     */
    public static Param param(String key, Object val) {
        return Params.param(key, val);
    }
}
