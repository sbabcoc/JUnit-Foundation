package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.internal.runners.model.EachTestNotifier;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.internal.runners.model.EachTestNotifier#fireTestFinished
 * fireTestFinished} method.
 */
public class FireTestFinished {

    /**
     * Default constructor
     */
    public FireTestFinished() { }
    
    /**
     * Interceptor for the {@link org.junit.internal.runners.model.EachTestNotifier#fireTestFinished fireTestFinished}
     * method.
     * 
     * @param notifier underlying run notifier
     * @param proxy callable proxy for the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final EachTestNotifier notifier, @SuperCall final Callable<?> proxy)
            throws Exception {
        
        LifecycleHooks.callProxy(proxy);
        EachTestNotifierInit.releaseMappingsFor(notifier);
    }
}
