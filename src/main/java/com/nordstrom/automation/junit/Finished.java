package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.model.RunnerScheduler#finished
 * finished} method.
 */
public class Finished {
	
	/**
	 * Interceptor for the {@link org.junit.runners.model.RunnerScheduler#finished finished} method.
	 * 
	 * @param scheduler current {@link org.junit.runners.model.RunnerScheduler RunnerScheduler}
     * @param proxy callable proxy for the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
	 */
    public static void intercept(@This final Object scheduler, @SuperCall final Callable<?> proxy) throws Exception {
        LifecycleHooks.callProxy(proxy);
        RunChild.finished();
    }
}
