package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.runners.model.MultipleFailureException;
import org.junit.internal.runners.model.EachTestNotifier;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runner.notification.RunNotifier#fireTestFailure
 * fireTestFailure} and {@link org.junit.runner.notification.RunNotifier#fireTestAssumptionFailed
 * fireTestAssumptionFailed} methods.
 */
public class FireTestFailure {

    /**
     * Interceptor for the {@link org.junit.runner.notification.RunNotifier#fireTestFailure fireTestFailure} and
     * {@link org.junit.runner.notification.RunNotifier#fireTestAssumptionFailed fireTestAssumptionFailed} methods.
     * 
     * @param notifier underlying run notifier
     * @param proxy callable proxy for the intercepted method
     * @param failure the description of the test that failed and the exception thrown
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final EachTestNotifier notifier, @SuperCall final Callable<?> proxy,
            @Argument(0) final Throwable targetException) throws Exception {
        
        if ( ! (targetException instanceof MultipleFailureException)) {
            AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(notifier);
            if (atomicTest != null) {
                atomicTest.setThrowable(targetException);
            }
        }
        
        LifecycleHooks.callProxy(proxy);
    }
}
