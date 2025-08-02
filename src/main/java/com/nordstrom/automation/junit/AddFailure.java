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
     * Default constructor
     */
    public AddFailure() { }
    
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
        
        // if this isn't a multi-failure wrapper exception
        if ( ! (targetException instanceof MultipleFailureException)) {
            // get atomic test for this notifier ('null' for suite notifiers)
            AtomicTest atomicTest = EachTestNotifierInit.getAtomicTestOf(notifier);
            // if atomic test exists
            if (atomicTest != null) {
                // set test exception ("throwable")
                atomicTest.setThrowable(targetException);
            }
        }
        
        // invoke intercepted method
        LifecycleHooks.callProxy(proxy);
    }
}
