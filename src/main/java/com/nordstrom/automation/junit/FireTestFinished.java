package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.internal.runners.model.EachTestNotifier;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class FireTestFinished {

    /**
     * Interceptor for the {@link org.junit.runner.notification.RunNotifier#fireTestFailure fireTestFailure} and
     * {@link org.junit.runner.notification.RunNotifier#fireTestAssumptionFailed fireTestAssumptionFailed} methods.
     * 
     * @param notifier underlying run notifier
     * @param proxy callable proxy for the intercepted method
     * @param failure the description of the test that failed and the exception thrown
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final EachTestNotifier notifier, @SuperCall final Callable<?> proxy)
            throws Exception {
        
        LifecycleHooks.callProxy(proxy);
        
        EachTestNotifierInit.releaseMappingsFor(notifier);
    }
}
