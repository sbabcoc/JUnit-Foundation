package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

public class RetryAnalyzer implements JUnitRetryAnalyzer {

    @Override
    public boolean retry(FrameworkMethod method, Throwable thrown) {
        return true;
    }

}
