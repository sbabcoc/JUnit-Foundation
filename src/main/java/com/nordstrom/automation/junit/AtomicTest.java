package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;

import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

public class AtomicTest {
    private final Object runner;
    private final TestClass testClass;
    private final FrameworkMethod identity;
    private final List<FrameworkMethod> particles;
    private final boolean hasConfiguration;
    private Throwable thrown;

    public AtomicTest(Object runner, TestClass testClass, FrameworkMethod testMethod) {
        this.runner = runner;
        this.testClass = testClass;
        this.identity = testMethod;
        this.particles = invoke(runner, "getChildren");
        this.hasConfiguration = (particles.size() > 1);
    }

    public Object getRunner() {
        return runner;
    }

    public TestClass getTestClass() {
        return testClass;
    }

    public FrameworkMethod getTestMethod() {
        return identity;
    }
    
    public List<FrameworkMethod> getParticles() {
        return particles;
    }

    public boolean hasConfiguration() {
        return hasConfiguration;
    }
    
    void setThrowable(Throwable thrown) {
        this.thrown = thrown;
    }
    
    public Throwable getThrowable() {
        return thrown;
    }
    
    public boolean contains(FrameworkMethod method) {
        return particles.contains(method);
    }
}
