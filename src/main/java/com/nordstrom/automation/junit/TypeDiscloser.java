package com.nordstrom.automation.junit;

public interface TypeDiscloser<T> {
    
    /**
     * Publish the child object type supported by this watcher.
     * 
     * @return supported child object type
     */
    Class<T> supportedType();
}
