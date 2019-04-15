package com.nordstrom.automation.junit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.model.TestTimedOutException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * This class is a mutable implementation of the {@link Test &#64;Test} annotation interface. It includes a static
 * {@link #proxyFor(Method)} method that replaces the immutable annotation attached to a JUnit test method with an
 * instance of this class to apply the global test timeout.
 */
@Ignore
@SuppressWarnings("all")
public class MutableTest implements Test {
    
    private static final String DECLARED_ANNOTATIONS = "declaredAnnotations";

    private Class<? extends Throwable> expected;
    private long timeout;
    
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
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return MutableTest.class;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Throwable> expected() {
        return expected;
    }
    
    /**
     * Specify the class of exception that the annotated test method is expected to throw. If you need to verify the
     * message or properties of the exception, use the {@link ExpectedException} rule instead.
     * 
     * @param expected expected exception class
     * @return this mutable annotation object
     */
    public MutableTest setExpected(Class<? extends Throwable> expected) {
        this.expected = expected;
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long timeout() {
        return timeout;
    }
    
    /**
     * Specify maximum test execution interval in milliseconds. If execution time exceeds this interval, the test will
     * fail with {@link TestTimedOutException}.
     * <p>
     * <b>THREAD SAFETY WARNING</b>: Test methods with a timeout parameter are run in a thread other than the thread
     * which runs the fixture's {@code @Before} and {@code @After} methods. This may yield different behavior
     * for code that is not thread safe when compared to the same test method without a timeout parameter. <b>Consider
     * using the {@link org.junit.rules.Timeout} rule instead</b>, which ensures a test method is run on the same
     * thread as the fixture's {@code @Before} and {@code @After} methods.
     *
     * @param timeout timeout interval in milliseconds
     * @return this mutable annotation object
     */
    public MutableTest setTimeout(long timeout) {
        TestTimedOutException foo;
        this.timeout = timeout;
        return this;
    }
    
    /**
     * Create a {@link Test &#64;Test} annotation proxy for the specified test method.
     * 
     * @param testMethod test method to which {@code @Test} annotation proxy will be attached
     * @return mutable proxy for {@code @Test} annotation
     */
    public static MutableTest proxyFor(Method testMethod) {
        Test declared = testMethod.getAnnotation(Test.class);
        if (declared instanceof MutableTest) {
            return (MutableTest) declared;
        }
        if (declared != null) {
            try {
                Field field = Method.class.getDeclaredField(DECLARED_ANNOTATIONS);
                field.setAccessible(true);
                try {
                    @SuppressWarnings("unchecked")
                    Map<Class<? extends Annotation>, Annotation> map = 
                                    (Map<Class<? extends Annotation>, Annotation>) field.get(testMethod);
                    MutableTest mutable = new MutableTest(declared);
                    map.put(Test.class, mutable);
                    return mutable;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new UnsupportedOperationException("Failed acquiring annotations map for method: " + testMethod, e);
                }
            } catch (NoSuchFieldException | SecurityException e) {
                throw new UnsupportedOperationException("Failed acquiring [" + DECLARED_ANNOTATIONS
                                + "] field of Executable class", e);
            }
        }
        throw new IllegalArgumentException("Specified method is not a JUnit @Test: " + testMethod);
    }
}
