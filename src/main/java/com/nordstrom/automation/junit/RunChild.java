package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;
import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Ignore;
import org.junit.experimental.theories.Theories;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

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
        
        // if child is a framework method (not the theory anchor)
        if (!(runner instanceof Theories) && (child instanceof FrameworkMethod)) {
            FrameworkMethod method = (FrameworkMethod) child;
            // get configured maximum retry count
            int maxRetry = RetryHandler.getMaxRetry(runner, method);
            // if retry enabled
            if (maxRetry > 0) {
                // if this method isn't being ignored
                if (null == method.getAnnotation(Ignore.class)) {
                    // create "atomic test" statement for this method
                    Statement statement = invoke(runner, "methodBlock", method);
                    // execute atomic test, retry on failure 
                    RetryHandler.runChildWithRetry(runner, method, statement, notifier, maxRetry);
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
