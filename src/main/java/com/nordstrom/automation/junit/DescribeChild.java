package com.nordstrom.automation.junit;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.junit.experimental.theories.PotentialAssignment.CouldNotGenerateValueException;
import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#describeChild
 * describeChild} method.
 */
public class DescribeChild {
    
    private static final String PERM_TAG = "theory-id: ";

    /**
     * Default constructor
     */
    public DescribeChild() { }
    
    /**
     * Interceptor for the {@link org.junit.runners.ParentRunner#describeChild describeChild} method.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param child child object of the test runner
     * @return a {@link Description} for {@code child}
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static Description intercept(@This final Object runner,
                    @SuperCall final Callable<?> proxy,
                    @Argument(0) final Object child) throws Exception {
        
        Description description = null;
        
        try {
            // invoke original implementation
            description = LifecycleHooks.callProxy(proxy);
        } catch (NullPointerException eaten) { // from JUnitParams
            // JUnitParams choked on a configuration method
            FrameworkMethod method = (FrameworkMethod) child;
            // call JUnit API to create a standard method description
            description = Description.createTestDescription(method.getDeclaringClass(),
                    method.getName(), method.getAnnotations());
        }
        
        // if describing a theory method, but not tagged as a permutation
        if ((description.getAnnotation(Theory.class) != null) && !isPermutation(description)) {
            try {
                // get parent of test runner
                Object parent = LifecycleHooks.getFieldValue(runner, "this$0");
                // if child of TheoryAnchor statement
                if (parent instanceof TheoryAnchor) {
                    // get assignments for this theory permutation
                    Assignments assignments = LifecycleHooks.getFieldValue(runner, "val$complete");
                    // compute permutation ID
                    String permutationId = computePermutationId(description, assignments);
                    // if permutation ID was computed
                    if (permutationId != null) {
                        // inject computed permutation ID
                        ((UniqueIdMutator) description).setUniqueId(permutationId);
                    }
                }
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                // nothing to do here
            }
        }
        return description;
    }
    
    /**
     * Determine if the specified description represents a "theory" permutation.
     * 
     * @param description JUnit {@link Description} object
     * @return {@code true} if permutation is described; otherwise {@code false}
     */
    static boolean isPermutation(final Description description) {
        return ((UniqueIdAccessor) description).getUniqueId().toString().startsWith(PERM_TAG);
    }
    
    /**
     * Compute permutation ID for the specified description and assignments.
     * 
     * @param description description of "theory" method
     * @param assignments arguments for this permutation
     * @return theory method permutation ID (may be {@code null})
     */
    private static String computePermutationId(final Description description, final Assignments assignments) {
        try {
            Object[] args = assignments.getMethodArguments();
            Object[] perm = new Object[args.length + 1];
            perm[0] = description.getDisplayName();
            System.arraycopy(args, 0, perm, 1, args.length);
            int permutationId = Arrays.hashCode(perm);
            return String.format(PERM_TAG + "%08X", permutationId);
        } catch (CouldNotGenerateValueException e) {
            return null;
        }
    }
    
}
