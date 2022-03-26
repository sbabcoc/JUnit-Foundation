package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;

/**
 * This interface declares the annotations accessor method for the {@code Description} class.
 */
public interface AnnotationsAccessor {
    
    /**
     * Get the annotations of this description.
     * 
     * @return array of {@link Annotation} objects
     */
    Annotation[] annotations();

}
