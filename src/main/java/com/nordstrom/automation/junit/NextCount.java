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
     * <p>
     * This interceptor is needed by the automatic retry feature to enable acquisition of a fresh "atomic test"
     * statement for each post-failure re-execution. By default, the JUnitParams implementation of the {@link
     * org.junit.runners.BlockJUnit4ClassRunner#methodBlock methodBlock} method increments its parameter set
     * index each time it's invoked, which always selects the next set of parameters (or exceeds the bounds of
     * the array). The handling provided by this interceptor returns the prior index if the target test method
     * if being retried so that the correct set of parameters is selected.
     * 
     * @param runner current {@link junitparams.internal.ParameterisedTestMethodRunner ParameterisedTestMethodRunner}
     * @param proxy callable proxy for the intercepted method
     * @return if retrying the target test method, return prior parameter set index; otherwise, return next index
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static int intercept(
            @This final ParameterisedTestMethodRunner runner, @SuperCall final Callable<?> proxy) throws Exception {
        
        // get reference to JUnitParams target test method
        TestMethod method = getFieldValue(runner, "method");
        // if this method is being retried
        if (RetryHandler.doRetryFor(method.frameworkMethod())) {
            // get current parameter set index
            int nextCount = invoke(runner, "count");
            // return prior index
            return nextCount - 1;
        // otherwise
        } else {
            // invoke default implementation
            return LifecycleHooks.callProxy(proxy);
        }
    }
}
