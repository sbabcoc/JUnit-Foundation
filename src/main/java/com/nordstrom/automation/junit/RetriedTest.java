package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;

/**
 * This class is a mutable implementation of the {@link Test &#64;Test} annotation interface. It includes a static
 * {@link #proxyFor(Description, Throwable)} method that replaces the immutable annotation attached to a JUnit test
 * description with an instance of this class for retried tests.
 */
@Ignore
@SuppressWarnings("all")
public class RetriedTest extends MutableTest {

    private static final String ANNOTATIONS = "fAnnotations";
    
    private Throwable thrown;
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Test &#64;Test}
     * annotation.
     * 
     * @param annotation {@link Test &#64;Test} annotation specifying desired parameters
     * @param thrown exception for this failed test
     */
    protected RetriedTest(Test annotation, Throwable thrown) {
        super(annotation);
        this.thrown = thrown;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return RetriedTest.class;
    }
    
    /**
     * Get the exception for this failed test.
     * 
     * @return exception for this failed test
     */
    public Throwable getThrown() {
        return thrown;
    }
    
    /**
     * Create a {@link Test &#64;Test} or {@link Theory &#64;Theory} annotation proxy for the specified test
     * description.
     * 
     * @param description test description to which {@code @Test} or {@code @Theory} annotation proxy will be attached
     * @param thrown exception for this failed test
     * @return new {@link Description} object for retry attempt
     */
    public static Description proxyFor(Description description, Throwable thrown) {
        try {
            Field field = Description.class.getDeclaredField(ANNOTATIONS);
            field.setAccessible(true);
            try {
                Annotation[] annotations = (Annotation[]) field.get(description);
                for (int i = 0; i < annotations.length; i++) {
                    Annotation proxy = null;
                    Annotation annotation = annotations[i];
                    if (annotation instanceof Test) {
                        proxy = new RetriedTest((Test) annotation, thrown);
                    } else if (annotation instanceof Theory) {
                        proxy = new RetriedTheory((Theory) annotation, thrown);
                    }
                    if (proxy != null) {
                        annotations[i] = proxy;
                        return DescribeChild.makeChildlessCopyOf(description);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new UnsupportedOperationException("Failed acquiring annotations map for method: " + description, e);
            }
        } catch (NoSuchFieldException | SecurityException e) {
            throw new UnsupportedOperationException("Failed acquiring [" + ANNOTATIONS
                            + "] field of test method class", e);
        }
        throw new IllegalArgumentException("Specified method is not a JUnit @Test or @Theory: " + description);
    }
    
    /**
     * Determine if the specified description is for a retried test or theory.
     * 
     * @param description JUnit description
     * @return {@code true} if the specified description indicates a retried test; otherwise {@code false}
     */
    public static boolean isRetriedTest(Description description) {
        return ((null != description.getAnnotation(RetriedTest.class)) ||
                (null != description.getAnnotation(RetriedTheory.class)));
    }
}
