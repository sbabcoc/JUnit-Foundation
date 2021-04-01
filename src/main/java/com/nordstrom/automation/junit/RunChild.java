package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

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
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild
 * runChild} method.
 */
public class RunChild {
    
    private static final Set<String> NOTIFIERS = new CopyOnWriteArraySet<>();
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild} method.
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
        
        attachRunListeners(runner, notifier);
        
        if (child instanceof FrameworkMethod) {
            FrameworkMethod method = (FrameworkMethod) child;
            
            int count = RetryHandler.getMaxRetry(runner, method);
            if (count > 0) {
                if (null == method.getAnnotation(Ignore.class)) {
                    RetryHandler.runChildWithRetry(runner, method, notifier, count);
                }
                return;
            }
        }
        
        LifecycleHooks.callProxy(proxy);
    }
    
    /**
     * Attach registered run listeners to the specified run notifier.
     * <p>
     * <b>NOTE</b>: If the specified run notifier has already been seen, do nothing.
     *  
     * @param runner JUnit test runner
     * @param notifier JUnit {@link RunNotifier} object
     * @throws Exception if {@code run-started} notification 
     */
    static void attachRunListeners(Object runner, final RunNotifier notifier) throws Exception {
        if (NOTIFIERS.add(toMapKey(notifier))) {
            Description description = LifecycleHooks.invoke(runner, "getDescription");
            for (RunListener listener : LifecycleHooks.getRunListeners()) {
                // prevent potential duplicates
                notifier.removeListener(listener);
                notifier.addListener(listener);
                listener.testRunStarted(description);
            }
        }
    }
}