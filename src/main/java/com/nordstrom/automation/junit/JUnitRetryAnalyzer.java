package com.nordstrom.automation.junit;

import org.junit.runner.notification.Failure;
import org.junit.runners.model.FrameworkMethod;

/**
 * <b>JUnit Foundation</b> retry analyzers implement this interface. 
 */
public interface JUnitRetryAnalyzer {
    
    /**
     * Determine if the specified failed test should be retried.
     * 
     * @param method failed test method
     * @param failure failure details object
     * @return {@code true} if test should be retried; otherwise {@code false}
     */
    boolean retry(FrameworkMethod method, Failure failure);

}
