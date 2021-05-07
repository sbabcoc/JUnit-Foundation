package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;

@SuppressWarnings("all")
public class RetriedTheory implements Theory {
    
    private static final String ANNOTATIONS = "fAnnotations";
    
    private boolean nullsAccepted;
    private Throwable thrown;
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Theory &#64;Theory}
     * annotation.
     * 
     * @param annotation {@link Theory &#64;Theory} annotation specifying desired parameters
     * @param thrown exception for this failed test
     */
    protected RetriedTheory(Theory annotation, Throwable thrown) {
        this.nullsAccepted = annotation.nullsAccepted();
        this.thrown = thrown;
    }
    
    @Override
    public Class<? extends Annotation> annotationType() {
        return RetriedTheory.class;
    }

    @Override
    public boolean nullsAccepted() {
        return nullsAccepted;
    }

    /**
     * Get the exception for this failed test.
     * 
     * @return exception for this failed test
     */
    public Throwable getThrown() {
        return thrown;
    }
}
