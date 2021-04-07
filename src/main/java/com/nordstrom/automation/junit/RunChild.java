package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Ignore;
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
    
    private static final Map<String, Boolean> DID_NOTIFY = new ConcurrentHashMap<>();
    
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
        
        String mapKey = toMapKey(runner);
        if ( ! DID_NOTIFY.containsKey(mapKey)) {
            DID_NOTIFY.put(mapKey, Run.fireRunStarted(runner));
            Run.attachRunListeners(runner, notifier);
        }
        
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
     * Fire the {@link RunnerWatcher#runFinished(Object)} event for the current runner.
     */
    static void finished() {
        Object runner = Run.getThreadRunner();
        if (Boolean.TRUE == DID_NOTIFY.remove(toMapKey(runner))) {
            Run.fireRunFinished(runner);
        }
    }
}
