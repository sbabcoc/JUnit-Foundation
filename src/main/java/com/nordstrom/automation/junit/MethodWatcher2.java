package com.nordstrom.automation.junit;

import java.lang.reflect.Method;

/**
 * This interface defines the methods implemented by JUnit method watchers. These watcher are attached to test classes
 * via the {@link MethodWatchers} annotation. To activate this feature, run with the {@link HookInstallingPlugin}.
 */
public interface MethodWatcher2 extends MethodWatcher {

    /**
     * Invoked before each class-level configuration method is invoked
     * 
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     */
    void beforeInvocation(Method method, Object[] args);

    /**
     * Invoked after each class-level configuration method is invoked
     * 
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     */
    void afterInvocation(Method method, Object[] args);
}
