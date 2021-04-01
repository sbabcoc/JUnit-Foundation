package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;
import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild
 * runChild} method.
 */
public class RunChild {
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild} method.
     * <p>
     * <b>NOTE</b>: If the {@link RunnerWatcher#runStarted runStarted} event hasn't been fired yet for the specified
     * JUnit test runner, the event will be fired by this interceptor.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @param notifier run notifier through which events are published
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
                    @Argument(0) final Object child,
                    @Argument(1) final RunNotifier notifier) throws Exception {
        
        RunChildren.attachRunListeners(runner, notifier);
        
        if (child instanceof FrameworkMethod) {
            FrameworkMethod method = (FrameworkMethod) child;
            
            int count = RetryHandler.getMaxRetry(runner, method);
            if (count > 0) {
                if (null == method.getAnnotation(Ignore.class)) {
                    RetryHandler.runChildWithRetry(runner, method, notifier, count);
                }
                return;
            }
        } else if (child instanceof ParentRunner) {
            TestClass testClass = ((ParentRunner<?>) child).getTestClass();
            if (null != testClass.getJavaClass()) {
                boolean foo = true;
            }
        }
        
        LifecycleHooks.callProxy(proxy);
    }
    
}