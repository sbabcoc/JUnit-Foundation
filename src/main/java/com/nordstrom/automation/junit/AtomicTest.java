package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

public class AtomicTest {
    private final FrameworkMethod testMethod;
    private final Object target;

    public AtomicTest(FrameworkMethod testMethod, Object target) {
        this.testMethod = testMethod;
        this.target = target;
    }

    public FrameworkMethod getTestMethod() {
        return testMethod;
    }

    public Object getTarget() {
        return target;
    }
}
