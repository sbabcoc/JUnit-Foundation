package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.model.RunnerScheduler#schedule schedule}
 * method.
 */
public class Schedule {

    /**
     * Interceptor for the {@link org.junit.runners.model.RunnerScheduler#schedule schedule} method.
     * 
     * @param scheduler current {@link org.junit.runners.model.RunnerScheduler RunnerScheduler}
     * @param proxy callable proxy for the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final Object scheduler, @SuperCall final Callable<?> proxy) throws Exception {
        RunChildren.started();
        LifecycleHooks.callProxy(proxy);
    }
}
