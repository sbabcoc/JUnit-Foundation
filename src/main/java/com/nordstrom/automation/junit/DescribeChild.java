package com.nordstrom.automation.junit;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.junit.experimental.theories.PotentialAssignment.CouldNotGenerateValueException;
import org.junit.experimental.theories.Theories.TheoryAnchor;
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
    
    private static final Field uniqueId;
    
    static {
        Field field = null;
        try {
            field = Description.class.getDeclaredField("fUniqueId");
            field.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            field = null;
        }
        uniqueId = field;
    }

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
        
        // if able to override [uniqueId] of test
        if ((uniqueId != null) && AtomicTest.isTest(description)) {
            try {
                // get parent of test runner
                Object parent = LifecycleHooks.getFieldValue(runner, "this$0");
                // if child of TheoryAnchor statement
                if (parent instanceof TheoryAnchor) {
                    // get assignments for this theory permutation
                    Assignments assignments = LifecycleHooks.getFieldValue(runner, "val$complete");
                    // inject permutation ID into description
                    injectPermutationId(description, assignments);
                }
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                // nothing to do here
            }
        }
        return description;
    }
    
    /**
     * Inject permutation ID into the specified description, overriding its default ID.
     *  
     * @param description description of "theory" method
     * @param assignments arguments for this permutation
     */
    private static void injectPermutationId(final Description description, final Assignments assignments) {
        try {
            Object[] args = assignments.getMethodArguments();
            Object[] perm = new Object[args.length + 1];
            perm[0] = description.getDisplayName();
            System.arraycopy(args, 0, perm, 1, args.length);
            int permutationId = Arrays.hashCode(perm);
            String theoryId = String.format("theory-id: %08X", permutationId);
            uniqueId.set(description, theoryId);
        } catch (CouldNotGenerateValueException | SecurityException |
                IllegalArgumentException | IllegalAccessException eaten) {
            // nothing to do here
        }
    }
    
    /**
     * Make a childless copy of the specified description.
     * 
     * @param description JUnit description
     * @return copy of the specified description, including unique ID
     */
    static Description makeChildlessCopyOf(final Description description) {
        Description descripCopy = description.childlessCopy();
        if (uniqueId != null) {
            try {
                uniqueId.set(descripCopy, uniqueId.get(description));
            } catch (IllegalArgumentException | IllegalAccessException eaten) {
                // nothing to do here
            }
        }
        return descripCopy;
    }
    
}
