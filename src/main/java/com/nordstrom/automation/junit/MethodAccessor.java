package com.nordstrom.automation.junit;

import junitparams.internal.TestMethod;

/**
 * This interface declares an accessor for the [method] field of {@code junitparams.internal.ParameterisedTestMethodRunner}.
 */
public interface MethodAccessor {

    /**
     * Get the JUnitParams test method of this parameterized runner.
     * 
     * @return {@link TestMethod} assigned to this runner
     */
    TestMethod getMethod();
    
}
