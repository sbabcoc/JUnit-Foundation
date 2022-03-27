package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.junit.runners.model.FrameworkField;

/**
 * This interface declares an accessor method for the [ {@code Class<Annotation>} &rarr; {@code List<FrameworkField>} ]
 * map of {@code org.junit.runners.model.TestClass}.
 */
public interface FieldsForAnnotationsAccessor {
    
    /**
     * Get the map that associates annotation types with the fields to which they're attached.
     * 
     * @return mappings from annotation types to lists of annotated fields
     */
    Map<Class<? extends Annotation>, List<FrameworkField>> getFieldsForAnnotations();

}
