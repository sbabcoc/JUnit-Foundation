package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.runner.notification.RunNotifier;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
 */
public class Run {
    private static final Map<String, RunNotifier> RUNNER_TO_NOTIFIER = new ConcurrentHashMap<>();
    
    /**
     * Interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param notifier run notifier through which events are published
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
                    @Argument(0) final RunNotifier notifier) throws Exception {
        
        try {
            RUNNER_TO_NOTIFIER.put(toMapKey(runner), notifier);
            RunChildren.pushThreadRunner(runner);
            // TODO: fireRunStarted(runner) used to be called from here
            LifecycleHooks.callProxy(proxy);
        } finally {
            // TODO: fireRunFinished(runner) used to be called from here
            RunChildren.popThreadRunner();
            RUNNER_TO_NOTIFIER.remove(toMapKey(runner));
        }
    }
    
    /**
     * Get the run notifier associated with the specified parent runner.
     * 
     * @param runner JUnit parent runner
     * @return <b>RunNotifier</b> object (may be {@code null})
     */
    static RunNotifier getNotifierOf(final Object runner) {
        return RUNNER_TO_NOTIFIER.get(toMapKey(runner));
    }
}
