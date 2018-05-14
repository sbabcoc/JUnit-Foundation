package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

/**
 * <b>JUnit Foundation</b> retry analyzers implement this interface. 
 */
public interface JUnitRetryAnalyzer {
    
    /**
     * Determine if the specified failed test should be retried.
     * 
     * @param method failed test method
     * @param thrown exception for this failed test
     * @return {@code true} if test should be retried; otherwise {@code false}
     */
    boolean retry(FrameworkMethod method, Throwable thrown);

}
