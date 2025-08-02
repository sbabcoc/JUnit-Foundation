package com.nordstrom.automation.junit;

/**
 * This class provides a thread-safe depth counter for method interceptors.
 */
public class DepthGauge {
    
    private int counter = 0;
    
    /**
     * Default constructor
     */
    public DepthGauge() { }
    
    /**
     * Determine if the depth is at ground level (i.e. - zero).
     * 
     * @return {@code true} if depth is 0; otherwise {@code false}
     */
    public synchronized boolean atGroundLevel() {
        return (0 == counter);
    }
    
    /**
     * Get the current depth count.
     * 
     * @return current depth count
     */
    public synchronized int currentDepth() {
        return counter;
    }
    
    /**
     * Increment intercept depth counter
     * 
     * @return depth count prior to update
     */
    public synchronized int increaseDepth() {
        return adjustDepth(1) - 1;
    }
    
    /**
     * Decrement intercept depth counter
     * 
     * @return depth count after update
     */
    public synchronized int decreaseDepth() {
        if (counter > 0) {
            return adjustDepth(-1);
        }
        throw new IllegalStateException("Unbalanced depth management; negative depth is prohibited");
    }
    
    /**
     * Apply the specified delta to intercept depth counter
     * 
     * @param delta depth counter delta
     * @return updated depth count
     */
    private int adjustDepth(final int delta) {
        counter += delta;
        return counter;
    }
}
