package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

/**
 * This interface defines the methods implemented by JUnit method watchers. These watchers are attached to test classes
 * via the {@link MethodWatchers} annotation. To activate this feature, run with the {@link HookInstallingPlugin}.
 */
public interface MethodWatcher {

    /**
     * Invoked before each test or configuration method is invoked
     * 
     * @param obj "enhanced" object upon which the method was invoked
     * @param method {@link FrameworkMethod} object for the invoked method
     */
    void beforeInvocation(Object obj, FrameworkMethod method);

    /**
     * Invoked after each test or configuration method is invoked
     * 
     * @param obj "enhanced" object upon which the method was invoked
     * @param method {@link FrameworkMethod} object for the invoked method
     * @param thrown exception thrown by method; {@code null} on normal completion
     */
    void afterInvocation(Object obj, FrameworkMethod method, Throwable thrown);
}
