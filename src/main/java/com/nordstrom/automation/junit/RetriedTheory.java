package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;

/**
 * This class is a mutable implementation of the {@link Theory &#64;Theory} annotation interface.
 */
@SuppressWarnings("all")
public class RetriedTheory implements Theory {
    
    private final boolean nullsAccepted;
    private final Throwable thrown;
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Theory &#64;Theory}
     * annotation.
     * 
     * @param annotation {@link Theory &#64;Theory} annotation specifying desired parameters
     * @param thrown exception for this failed test
     */
    protected RetriedTheory(Theory annotation, Throwable thrown) {
        this.nullsAccepted = annotation.nullsAccepted();
        this.thrown = Objects.requireNonNull(thrown, "[thrown] must be non-null");
    }
    
    @Override
    public Class<? extends Annotation> annotationType() {
        return Theory.class;
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

    @Override
    public String toString() {
        return "@" + getClass().getName() + "(nullsAccepted=" + nullsAccepted +
                ", thrown=" + thrown.getClass().getName() + ")";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (nullsAccepted ? 1231 : 1237);
        result = prime * result + ((thrown == null) ? 0 : thrown.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if ( ! (obj instanceof RetriedTheory))
            return false;
        RetriedTheory other = (RetriedTheory) obj;
        if (nullsAccepted != other.nullsAccepted)
            return false;
        if (thrown == null) {
            if (other.thrown != null)
                return false;
        } else if (!thrown.equals(other.thrown))
            return false;
        return true;
    }
}
