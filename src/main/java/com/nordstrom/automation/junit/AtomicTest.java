package com.nordstrom.automation.junit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents an atomic JUnit test, which is composed of a core {@link Test &#64;Test} method and
 * the configuration methods that run with it ({@link Before &#64;Before}, {@link org.junit.After &#64;After},
 * {@link org.junit.BeforeClass &#64;BeforeClass}, and {@link AfterClass &#64;AfterClass}).
 */
@Ignore
@SuppressWarnings("all")
public class AtomicTest {
    private final Object runner;
    private final Description description;
    private FrameworkMethod identity;
    private final List<FrameworkMethod> particles;
    private Throwable thrown;

    private static final Pattern PARAM = Pattern.compile("[(\\[]");
    private static final List<Class<? extends Annotation>> TEST_TYPES = Arrays.asList(Test.class, Theory.class);

    public AtomicTest(Description description) {
        this.runner = Run.getThreadRunner();
        this.description = description;
        this.particles = getParticles(runner, description);
        this.identity = particles.isEmpty() ? null : particles.get(0);
    }

    /**
     * Get the runner for this atomic test.
     *
     * @return {@code BlockJUnit4ClassRunner} object
     */
    public Object getRunner() {
        return runner;
    }

    /**
     * Get the description for this atomic test.
     *
     * @return {@link Description} object
     */
    public Description getDescription() {
        return description;
    }

    /**
     * Set the "identity" method for this atomic test - the core {@link Test &#64;Test} method.
     */
    void setIdentity(FrameworkMethod method) {
        identity = method;
        particles.set(0, method);
    }

    /**
     * Get the "identity" method for this atomic test - the core {@link Test &#64;Test} method.
     *
     * @return core method associated with this atomic test (may be {@code null})
     */
    public FrameworkMethod getIdentity() {
        return identity;
    }

    /**
     * Get the "particle" methods of which this atomic test is composed.
     *
     * @return list of methods that compose this atomic test (may be empty)
     */
    public List<FrameworkMethod> getParticles() {
        return particles;
    }

    /**
     * Determine if this atomic test includes configuration methods.
     *
     * @return {@code true} if this atomic test includes configuration; otherwise {@code false}
     */
    public boolean hasConfiguration() {
        return (particles.size() > 1);
    }

    /**
     * Set the exception for this atomic test.
     *
     * @param thrown exception for this atomic test
     */
    void setThrowable(Throwable thrown) {
        this.thrown = thrown;
    }

    /**
     * Get the exception for this atomic test.
     *
     * @return exception for this atomic test; {@code null} if test finished normally
     */
    public Throwable getThrowable() {
        return thrown;
    }

    /**
     * Determine if this atomic test includes the specified method.
     *
     * @param method method object
     * @return {@code true} if this atomic test includes the specified method; otherwise {@code false}
     */
    public boolean includes(FrameworkMethod method) {
        return particles.contains(method);
    }

    /**
     * Determine if this atomic test represents a "theory" method permutation.
     *
     * @return {@code true} if this atomic test represents a permutation; otherwise {@code false}
     */
    public boolean isTheory() {
        return isTheory(description);
    }

    /**
     * Determine if the specified description represents a "theory" method permutation.
     *
     * @param description JUnit method description
     * @return {@code true} if the specified description represents a permutation; otherwise {@code false}
     */
    public static boolean isTheory(Description description) {
        return DescribeChild.isPermutation(description);
    }

    /**
     * Determine if this atomic test represents a test method.
     *
     * @return {@code true} if this atomic test represents a test method; otherwise {@code false}
     */
    public boolean isTest() {
        return isTest(description);
    }

    /**
     * Determine if the specified description represents a test method.
     *
     * @param description JUnit description object
     * @return {@code true} if description represents a test method; otherwise {@code false}
     */
    public static boolean isTest(Description description) {
        return (getTestAnnotation(description) != null);
    }

    /**
     * Get the annotation that marks the specified description as a test method.
     *
     * @param description JUnit description object
     * @return if description represents a test method, the {@link Test} or {@link Theory} annotation;
     * otherwise {@code null}
     */
    public static Annotation getTestAnnotation(Description description) {
        for (Annotation annotation : description.getAnnotations()) {
            if (TEST_TYPES.contains(annotation.annotationType())) return annotation;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return description.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if ( ! (o instanceof AtomicTest)) return false;
        AtomicTest that = (AtomicTest) o;
        return Objects.equals(runner, that.runner) &&
                Objects.equals(identity, that.identity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return description.hashCode();
    }

    /**
     * Get the "particle" method for this atomic test.
     *
     * @param runner JUnit test runner
     * @param description JUnit method description
     * @return list of "particle" methods (may be empty)
     */
    private List<FrameworkMethod> getParticles(Object runner, Description description) {
        List<FrameworkMethod> particles = new ArrayList<>();
        if (isTest(description)) {
            TestClass testClass = LifecycleHooks.getTestClassOf(runner);            String descriptionMethodName = description.getMethodName();
            FrameworkMethod identity = null;
            // Try exact match first
            for (FrameworkMethod method : testClass.getAnnotatedMethods()) {
                if (descriptionMethodName.equals(method.getName())) {
                    identity = method;
                    break;
                }
            }
            // Fall back to parameter stripping if needed
            if (identity == null) {
                String fallbackMethodName = descriptionMethodName;
                Matcher matcher = PARAM.matcher(fallbackMethodName);
                if (matcher.find()) {
                    fallbackMethodName = fallbackMethodName.substring(0, matcher.start());
                }
                for (FrameworkMethod method : testClass.getAnnotatedMethods()) {
                    if (method.getName().equals(fallbackMethodName)) {
                        identity = method;
                        break;
                    }
                }
            }
            if (identity != null) {
                particles.add(identity);
                particles.addAll(testClass.getAnnotatedMethods(Before.class));
                particles.addAll(testClass.getAnnotatedMethods(After.class));
            } else {
                throw new IllegalStateException(
                        "Identity method not found for: " + descriptionMethodName
                );
            }
        }
        return particles;
    }
}
