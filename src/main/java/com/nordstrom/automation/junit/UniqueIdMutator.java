package com.nordstrom.automation.junit;

import java.io.Serializable;

/**
 * This interface declares the unique ID mutator method for the {@code Description} class.
 */
public interface UniqueIdMutator {

    /**
     * Set the unique ID of this description.
     * 
     * @param uniqueId unique ID object
     */
    void setUniqueId(Serializable uniqueId);
    
}
