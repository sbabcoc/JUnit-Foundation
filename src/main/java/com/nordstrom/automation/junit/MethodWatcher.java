package com.nordstrom.automation.junit;

import java.lang.reflect.Method;

/**
 * This interface defines the methods implemented by JUnit method watchers. These watcher are attached to test classes
 * via the {@link MethodWatchers} annotation. To activate this feature, run with the {@code HookInstallingRunner}
 * or {@link HookInstallingPlugin}.
 */
public interface MethodWatcher {

    /**
     * Invoked before each test or configuration method is invoked
     * 
     * @param obj "enhanced" object upon which the method was invoked
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     */
    void beforeInvocation(Object obj, Method method, Object[] args);

    /**
     * Invoked after each test or configuration method is invoked
     * 
     * @param obj "enhanced" object upon which the method was invoked
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     */
    void afterInvocation(Object obj, Method method, Object[] args);
}
