package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.runners.model.MultipleFailureException;
import org.junit.internal.runners.model.EachTestNotifier;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.internal.runners.model.EachTestNotifier#addFailure
 * addFailure} method.
 */
public class AddFailure {

    /**
     * Interceptor for the {@link org.junit.internal.runners.model.EachTestNotifier#addFailure addFailure} method.
     * 
     * @param notifier underlying run notifier
     * @param proxy callable proxy for the intercepted method
     * @param targetException the exception thrown by the test
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
