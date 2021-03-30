package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

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
    private final FrameworkMethod identity;
    private final List<FrameworkMethod> particles;
    private Throwable thrown;
    
    private static final Pattern PARAM = Pattern.compile("[(\\[]");

    public AtomicTest(Description description) {
        this.runner = Run.getThreadRunner();
        this.description = description;
        this.particles = getParticles(runner, description);
        this.identity = this.particles.get(0);
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
     * Get the "identity" method for this atomic test - the core {@link Test &#64;Test} method.
     * 
     * @return core method associated with this atomic test
     */
    public FrameworkMethod getIdentity() {
        return identity;
    }
    
    /**
     * Get the "particle" methods of which this atomic test is composed.
     * 
     * @return list of methods that compose this atomic test
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
    
    public boolean isTheory() {
        try {
            String uniqueId = LifecycleHooks.getFieldValue(description, "fUniqueId");
            return ((uniqueId != null) && (uniqueId.startsWith("theory-id: ")));
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return false;
        }
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
        if (o == null || getClass() != o.getClass()) return false;
        AtomicTest that = (AtomicTest) o;
        return Objects.equals(runner, that.runner) &&
                Objects.equals(identity, that.identity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(runner, identity);
    }
    
    private List<FrameworkMethod> getParticles(Object runner, Description description) {
        List<FrameworkMethod> particles = new ArrayList<>();
        if (description.isTest()) {
            TestClass testClass = LifecycleHooks.getTestClassOf(runner);
            
            String methodName = description.getMethodName();
            Matcher matcher = PARAM.matcher(methodName);
            if (matcher.find()) {
                methodName = methodName.substring(0, matcher.start());
            }
            
            FrameworkMethod identity = null;
            for (FrameworkMethod method : testClass.getAnnotatedMethods()) {
                if (method.getName().equals(methodName)) {
                    identity = method;
                    break;
                }
            }
            
            if (identity != null) {
                particles.add(identity);
                particles.addAll(testClass.getAnnotatedMethods(Before.class));
                particles.addAll(testClass.getAnnotatedMethods(After.class));
            } else {
                throw new IllegalStateException("Identity method not found");
            }
        }
        
        return particles;
    }
}
