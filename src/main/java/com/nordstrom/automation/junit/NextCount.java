package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;
import static com.nordstrom.automation.junit.LifecycleHooks.invoke;

import java.util.concurrent.Callable;

import junitparams.internal.ParameterisedTestMethodRunner;
import junitparams.internal.TestMethod;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class NextCount {

    /**
     * Interceptor for the {@link junitparams.internal.ParameterisedTestMethodRunner#nextCount nextCount} method.
     * 
     * @param runner current {@link junitparams.internal.ParameterisedTestMethodRunner ParameterisedTestMethodRunner}
     * @param proxy callable proxy for the intercepted method
     * @return if automatic retry, return prior count; otherwise, return next count
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static int intercept(
            @This final ParameterisedTestMethodRunner runner, @SuperCall final Callable<?> proxy) throws Exception {
        
        TestMethod method = getFieldValue(runner, "method");
        if (RetryHandler.doRetryFor(method.frameworkMethod())) {
            int nextCount = invoke(runner, "count");
            return nextCount - 1;
        } else {
            return LifecycleHooks.callProxy(proxy);
        }
    }
}
