package com.nordstrom.automation.junit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.TestTimedOutException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * This class is a mutable implementation of the {@link Test &#64;Test} annotation interface. It includes a static
 * {@link #proxyFor(Method, long)} method that replaces the immutable annotation attached to a JUnit test method with
 * an instance of this class to apply the global test timeout.
 */
@Ignore
@SuppressWarnings("all")
public class MutableTest implements Test {
    
    private static final String DECLARED_ANNOTATIONS = "declaredAnnotations";

    private final Class<? extends Throwable> expected;
    private final long timeout;
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Test &#64;Test}
     * annotation.
     * 
     * @param annotation {@link Test &#64;Test} annotation specifying desired parameters
     */
    protected MutableTest(Test annotation) {
        this.expected = annotation.expected();
        this.timeout = annotation.timeout();
    }
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Test &#64;Test}
     * annotation.
     * 
     * @param annotation {@link Test &#64;Test} annotation specifying desired parameters
     * @param timeout timeout interval in milliseconds
     */
    private MutableTest(Test annotation, long timeout) {
        this.expected = annotation.expected();
        this.timeout = timeout;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return MutableTest.class;
    }
    
    @Override
    public Class<? extends Throwable> expected() {
        return expected;
    }
    
    @Override
    public long timeout() {
        return timeout;
    }
    
    /**
     * Create a {@link Test &#64;Test} annotation proxy for the specified test method.
     * 
     * @param testMethod test method to which {@code @Test} annotation proxy will be attached
     * @param timeout timeout interval in milliseconds
     * @return mutable proxy for {@code @Test} annotation
     */
    public static MutableTest proxyFor(Method testMethod, long timeout) {
        Test declared = testMethod.getAnnotation(Test.class);
        if (declared instanceof MutableTest) {
            return (MutableTest) declared;
        }
        if (declared != null) {
            try {
                Map<Class<? extends Annotation>, Annotation> map = LifecycleHooks.getFieldValue(testMethod, DECLARED_ANNOTATIONS);
                MutableTest mutable = new MutableTest(declared, timeout);
                map.put(Test.class, mutable);
                return mutable;
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw new UnsupportedOperationException("Failed acquiring annotations map for method: " + testMethod, e);
            }
        }
        throw new IllegalArgumentException("Specified method is not a JUnit @Test: " + testMethod);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expected == null) ? 0 : expected.hashCode());
        result = prime * result + (int) (timeout ^ (timeout >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if ( ! (obj instanceof MutableTest))
            return false;
        MutableTest other = (MutableTest) obj;
        if (expected == null) {
            if (other.expected != null)
                return false;
        } else if (!expected.equals(other.expected))
            return false;
        if (timeout != other.timeout)
            return false;
        return true;
    }
}
