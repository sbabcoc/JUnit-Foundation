package com.nordstrom.automation.junit;

import java.lang.reflect.Method;

/**
 * This interface defines the methods implemented by JUnit method watchers. These watcher are attached to test classes
 * via the {@link JUnitMethodWatchers} annotation. To activate this feature, run with the {@link HookInstallingRunner}.
 */
public interface JUnitMethodWatcher2 extends JUnitMethodWatcher {

    /**
     * Invoked before each class-level configuration method is invoked
     * 
     * @param clazz "enhanced" class upon which the method was invoked
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     */
    void beforeInvocation(Method method, Object[] args);

    /**
     * Invoked after each class-level configuration method is invoked
     * 
     * @param clazz "enhanced" class upon which the method was invoked
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     */
    void afterInvocation(Method method, Object[] args);
}
