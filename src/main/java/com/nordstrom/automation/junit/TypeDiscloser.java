package com.nordstrom.automation.junit;

/**
 * This interface defines the method implemented by classes that disclose their supported type.
 * 
 * @param <T> type of objects to be disclosed
 */
public interface TypeDiscloser<T> {
    
    /**
     * Publish the child object type supported by this watcher.
     * 
     * @return supported child object type
     */
    Class<T> supportedType();
}
