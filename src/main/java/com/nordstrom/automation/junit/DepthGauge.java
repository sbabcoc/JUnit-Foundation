package com.nordstrom.automation.junit;

public class DepthGauge {
    
    private final ThreadLocal<Integer> counter;
    
    public DepthGauge(ThreadLocal<Integer> counter) {
        this.counter = counter;
    }
    
    /**
     * Determine if the depth is at ground level (i.e. - zero).
     * 
     * @return {@code true} if depth is 0; otherwise {@code false}
     */
    public boolean atGroundLevel() {
        return (0 == currentDepth());
    }
    
    /**
     * Get the current depth count.
     * 
     * @return current depth count
     */
    public int currentDepth() {
        return counter.get().intValue();
    }
    
    /**
     * Increment intercept depth counter
     * 
     * @return updated depth count
     */
    public int increaseDepth() {
        return adjustDepth(1);
    }
    
    /**
     * Decrement intercept depth counter
     * 
     * @return updated depth count
     */
    public int decreaseDepth() {
        if (counter.get().intValue() > 0) {
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
        int i = counter.get().intValue() + delta;
        counter.set(Integer.valueOf(i));
        return i;
    }
    
    /**
     * Get depth counter.
     * 
     * @return thread-local integer with initial value of 0
     */
    public static ThreadLocal<Integer> getCounter() {
        return new InheritableThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return Integer.valueOf(0);
            }
        };
    }
    
}
