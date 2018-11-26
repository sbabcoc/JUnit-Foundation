package com.nordstrom.automation.junit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.runner.Description;

/**
 * This interface enables implementers to provide method-related parameters to the artifact capture framework.
 */
public interface ArtifactParams {
    
    /**
     * Get the JUnit method description object for the current test class instance.
     * 
     * @return JUnit method description object
     */
    Description getDescription();
    
    /**
     * Get the parameters associated with this test class instance.
     * 
     * @return optional map of named test class parameters
     */
    default Optional<Map<String, Object>> getParameters() {
        return Optional.empty();
    }
    
    /**
     * Assemble a map of test class instance parameters.
     * 
     * @param params array of {@link Param} objects; may be {@code null} or empty
     * @return optional map of parameters (may be empty)
     */
    public static Optional<Map<String, Object>> mapOf(Param... params) {
        if ((params == null) || (params.length == 0)) {
            return Optional.empty();
        }
        Map<String, Object> paramMap = new HashMap<>();
        for (Param param : params) {
            paramMap.put(param.key, param.val);
        }
        return Optional.of(Collections.unmodifiableMap(paramMap));
    }
    
    /**
     * Create a test parameter object for the specified key/value pair.
     * 
     * @param key test parameter key (name)
     * @param val test parameter value
     * @return test parameter object
     */
    public static Param param(String key, Object val) {
        return new Param(key, val);
    }
    
    /**
     * This class defines a test parameter object.
     */
    static class Param {
        
        private final String key;
        private final Object val;
        
        /**
         * Constructor for test parameter object.
         * 
         * @param key test parameter key
         * @param val test parameter value
         */
        public Param(String key, Object val) {
            if ((key == null) || key.isEmpty()) {
                throw new IllegalArgumentException("[key] must be a non-empty string");
            }
            this.key = key;
            this.val = val;
        }
    }

}
