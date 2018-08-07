package com.nordstrom.automation.junit;

import org.junit.runners.model.FrameworkMethod;

/**
 * This interface defines the methods implemented by JUnit method watchers. These watcher are attached to test classes
 * via the {@link MethodWatchers} annotation. To activate this feature, run with the {@link HookInstallingPlugin}.
 */
public interface MethodWatcher2 extends MethodWatcher {

    /**
     * Invoked before each class-level configuration method is invoked
     * 
     * @param method {@link FrameworkMethod} object for the invoked method
     */
    void beforeInvocation(FrameworkMethod method);

    /**
     * Invoked after each class-level configuration method is invoked
     * 
     * @param method {@link FrameworkMethod} object for the invoked method
     * @param thrown exception thrown by method; {@code null} on normal completion
     */
    void afterInvocation(FrameworkMethod method, Throwable thrown);
}
