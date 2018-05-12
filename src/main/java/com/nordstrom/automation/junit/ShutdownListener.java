package com.nordstrom.automation.junit;

/**
 * This interface defines the contract for shutdown listener objects.
 */
public interface ShutdownListener {
    
    /**
     * Perform shutdown processing. This notification is sent when the JVM is preparing to close.
     */
    void onShutdown();

}
