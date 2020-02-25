package com.nordstrom.automation.junit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@code runChild} method.
 */
@SuppressWarnings("squid:S1118")
public class RunChild {
    
    private static final Map<String, Boolean> notifyMap = new HashMap<>();
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild} method.
     * <p>
     * <b>NOTE</b>: If the {@link RunnerWatcher#runStarted(Object)} event hasn't been fired yet for the specified
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
        
        Run.attachRunListeners(runner, notifier);
        
        synchronized(notifyMap) {
            String key = runner.toString();
            if (!notifyMap.containsKey(key)) {
                notifyMap.put(key, Run.fireRunStarted(runner));
            }
        }
        
        try {
            Run.pushThreadRunner(runner);
            RunAnnouncer.newAtomicTest(runner, child);
            
            if (child instanceof FrameworkMethod) {
                FrameworkMethod method = (FrameworkMethod) child;
                
                int count = RetryHandler.getMaxRetry(runner, method);
                boolean isIgnored = (null != method.getAnnotation(Ignore.class));
                
                if (count == 0) {
                    LifecycleHooks.callProxy(proxy);
                } else if (!isIgnored) {
                    RetryHandler.runChildWithRetry(runner, method, notifier, count);
                }
            } else {
                LifecycleHooks.callProxy(proxy);
            }
        } catch (NoSuchMethodException e) {
            LifecycleHooks.callProxy(proxy);
        } finally {
            Run.popThreadRunner();
        }
    }
    
    /**
     * If the {@link RunnerWatcher#runStarted(Object)} event was fired by the {@code runChild} interceptor, fire the 
     * {@link RunnerWatcher#runFinished(Object)} event.
     */
    static void finished() {
        Object runner = Run.getThreadRunner();
        synchronized(notifyMap) {
            if (notifyMap.get(runner.toString())) {
                Run.fireRunFinished(runner);
            }
        }
    }
}