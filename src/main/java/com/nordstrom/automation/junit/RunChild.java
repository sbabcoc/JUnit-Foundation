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
        int count = RetryHandler.getMaxRetry(runner, method);
        boolean isIgnored = (null != method.getAnnotation(Ignore.class));
        
        if (isIgnored) {
            RunReflectiveCall.fireTestIgnored(method);
        }
        
        if (count == 0) {
            proxy.call();
        } else if (!isIgnored) {
            RetryHandler.runChildWithRetry(runner, method, notifier, count);
        }
    }
    
    /*
        
        if (childMethod != null) {
            for (TestClassWatcher watcher : classWatcherLoader) {
                watcher.testFinished(childMethod, target);
            }
        }
     */
    
}