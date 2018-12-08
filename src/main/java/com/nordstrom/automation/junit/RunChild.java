package com.nordstrom.automation.junit;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
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
    
    private static final ServiceLoader<RunListener> runListenerLoader;
    private static final Set<RunNotifier> NOTIFIERS = new CopyOnWriteArraySet<>();
    private static final ThreadLocal<Integer> COUNTER;
    private static final DepthGauge DEPTH;
    
    static {
        runListenerLoader = ServiceLoader.load(RunListener.class);
        COUNTER = DepthGauge.getCounter();
        DEPTH = new DepthGauge(COUNTER);
    }

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
        
        if (NOTIFIERS.add(notifier)) {
            Description description = LifecycleHooks.invoke(runner, "getDescription");
            synchronized(runListenerLoader) {
                for (RunListener listener : runListenerLoader) {
                    notifier.addListener(listener);
                    listener.testRunStarted(description);
                }
            }
        }
        
        int count = RetryHandler.getMaxRetry(runner, method);
        boolean isIgnored = (null != method.getAnnotation(Ignore.class));
        
        if (count == 0) {
            try {
                DEPTH.increaseDepth();
                LifecycleHooks.callProxy(proxy);
            } finally {
                DEPTH.decreaseDepth();
            }
        } else if (!isIgnored) {
            RetryHandler.runChildWithRetry(runner, method, notifier, count);
        }
    }
}