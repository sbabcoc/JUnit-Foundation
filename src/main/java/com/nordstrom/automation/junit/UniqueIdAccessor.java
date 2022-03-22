package com.nordstrom.automation.junit;

import java.io.Serializable;

/**
 * This interface declares the unique ID accessor method for the {@code Description} class.
 */
public interface UniqueIdAccessor {
    
    /**
     * Get the unique ID of this description.
     * 
     * @return unique ID object
     */
    Serializable getUniqueId();

}
