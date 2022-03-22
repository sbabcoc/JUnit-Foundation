package com.nordstrom.automation.junit;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Objects;

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

    private final Throwable thrown;
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Test &#64;Test}
     * annotation.
     * 
     * @param annotation {@link Test &#64;Test} annotation specifying desired parameters
     * @param thrown exception for this failed test
     */
    protected RetriedTest(Test annotation, Throwable thrown) {
        super(annotation);
        this.thrown = Objects.requireNonNull(thrown, "[thrown] must be non-null");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
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
        return super.toString().replaceAll(".$", "") + ", thrown=" + thrown.getClass().getName() + ")";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((thrown == null) ? 0 : thrown.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if ( ! (obj instanceof RetriedTest))
            return false;
        RetriedTest other = (RetriedTest) obj;
        if (thrown == null) {
            if (other.thrown != null)
                return false;
        } else if (!thrown.equals(other.thrown))
            return false;
        return true;
    }
    
    /**
     * Determine if the specified description is for a retried test or theory.
     * 
     * @param description JUnit {@link Description} object
     * @return {@code true} if the specified description indicates a retried test; otherwise {@code false}
     */
    public static boolean isRetriedTest(Description description) {
        Annotation annotation = AtomicTest.getTestAnnotation(description);
        return ((annotation instanceof RetriedTest) || (annotation instanceof RetriedTheory));
    }

    /**
     * Create a {@link Test &#64;Test} or {@link Theory &#64;Theory} annotation proxy for the specified test
     * description.
     * 
     * @param description test description to which {@code @Test} or {@code @Theory} annotation proxy will be attached
     * @param thrown exception for this failed test
     * @return new {@link Description} object for retry attempt
     */
    static Description proxyFor(Description description, Throwable thrown) {
        Annotation proxy = null;
        Annotation annotation = AtomicTest.getTestAnnotation(description);
        if (annotation == null) {
            throw new IllegalArgumentException("Specified method is not a JUnit @Test or @Theory: " + description);
        }
        if (annotation.annotationType().equals(Test.class)) {
            proxy = new RetriedTest((Test) annotation, thrown);
        } else {
            proxy = new RetriedTheory((Theory) annotation, thrown);
        }
        injectProxy(description, proxy);
        return duplicate(description);
    }
    
    /**
     * Inject the specified proxy annotation into the indicated failed test description.
     * 
     * @param description test description to which {@code @Test} or {@code @Theory} annotation proxy will be attached
     * @param proxyAnnotation automatic retry proxy annotation ({@link RetriedTest} or {@link RetriedTheory})
     */
    private static void injectProxy(Description description, Annotation proxyAnnotation) {
        Annotation[] annotations = ((AnnotationsAccessor) description).annotations();
        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().equals(proxyAnnotation.annotationType())) {
                annotations[i] = proxyAnnotation;
                break;
            }
        }
    }
    /**
     * Create a duplicate of the specified description.
     * 
     * @param description {@link Description} object
     * @return new {@link Description} object that matches the original
     */
    private static Description duplicate(Description description) {
        String displayName = description.getDisplayName();
        Serializable uniqueId = ((UniqueIdAccessor) description).getUniqueId();
        Annotation[] annotations = ((AnnotationsAccessor) description).annotations();
        return Description.createSuiteDescription(displayName, uniqueId, annotations);
    }
}
