package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
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
    private final FrameworkMethod identity;
    private final List<FrameworkMethod> particles;
    private Exception thrown;

    public AtomicTest(Object runner, FrameworkMethod testMethod) {
        this.runner = runner;
        this.identity = testMethod;
        this.particles = invoke(runner, "getChildren");
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
    void setException(Exception thrown) {
        this.thrown = thrown;
    }
    
    /**
     * Get the exception for this atomic test.
     * 
     * @return exception for this atomic test; {@code null} if test finished normally
     */
    public Exception getException() {
        return thrown;
    }
    
    /**
     * Determine if this atomic test includes the specified method.
     * 
     * @param method {@link FrameworkMethod} object
     * @return {@code true} if this atomic test includes the specified method; otherwise {@code false}
     */
    public boolean includes(FrameworkMethod method) {
        return particles.contains(method);
    }
}
