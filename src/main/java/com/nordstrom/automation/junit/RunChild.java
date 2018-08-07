package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild}
 * method.
 */
@SuppressWarnings("squid:S1118")
public class RunChild {

    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param method test method to be run
     * @param notifier run notifier through which events are published
     * @throws Exception if something goes wrong
     */
    public static void intercept(@This Object runner, @SuperCall Callable<?> proxy, @Argument(0) FrameworkMethod method, @Argument(1) RunNotifier notifier) throws Exception {
        int count = RetryHandler.getMaxRetry(runner, method);
        
        if (count > 0) {
            RetryHandler.runChildWithRetry(runner, method, notifier, count);
        } else {
            proxy.call();
        }
    }
}