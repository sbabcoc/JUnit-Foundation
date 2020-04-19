package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.TestClass;

/**
 * This class represents an atomic JUnit test, which is composed of a core {@link Test &#64;Test} method and
 * the configuration methods that run with it ({@link Before &#64;Before}, {@link org.junit.After &#64;After},
 * {@link org.junit.BeforeClass &#64;BeforeClass}, and {@link AfterClass &#64;AfterClass}).
 */
@Ignore
@SuppressWarnings("all")
public class AtomicTest<T> {
    private final Object runner;
    private final Description description;
    private final T identity;
    private final List<T> particles;
    private Throwable thrown;

    public AtomicTest(Object runner, T identity) {
        this.runner = runner;
        this.identity = identity;
        this.description = invoke(runner, "describeChild", identity);
        this.particles = invoke(runner, "getChildren");
    }
    
    public AtomicTest(AtomicTest<T> original, Description description) {
        this.runner = original.runner;
        this.identity = original.identity;
        this.description = description;
        this.particles = original.particles;
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
    public T getIdentity() {
        return identity;
    }
    
    /**
     * Get the "particle" methods of which this atomic test is composed.
     * 
     * @return list of methods that compose this atomic test
     */
    public List<T> getParticles() {
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
    public boolean includes(T method) {
        return particles.contains(method);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return description.toString();
    }
}
