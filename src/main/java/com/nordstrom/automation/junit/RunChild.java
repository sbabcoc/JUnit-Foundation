package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.Ignore;
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

    private static final ThreadLocal<Boolean> BELOW = new InheritableThreadLocal<Boolean>() {
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param method test method to be run
     * @param notifier run notifier through which events are published
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
                    @Argument(0) final FrameworkMethod method,
                    @Argument(1) final RunNotifier notifier) throws Exception {
        
        if (BELOW.get()) {
            LifecycleHooks.callProxy(proxy);
            return;
        }
        
        int count = RetryHandler.getMaxRetry(runner, method);
        boolean isIgnored = (null != method.getAnnotation(Ignore.class));
        
        if (isIgnored) {
            RunReflectiveCall.fireTestIgnored(runner, method);
        }
        
        if (count == 0) {
            try {
                BELOW.set(Boolean.TRUE);
                LifecycleHooks.callProxy(proxy);
            } finally {
                BELOW.set(Boolean.FALSE);
            }
        } else if (!isIgnored) {
            RetryHandler.runChildWithRetry(runner, method, notifier, count);
        }
    }
}