package com.nordstrom.automation.junit;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.junit.experimental.theories.PotentialAssignment.CouldNotGenerateValueException;
import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.runner.Description;
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
        
        Description description = (Description) LifecycleHooks.callProxy(proxy);
        
        // if [uniqueId] can be overridden and is test
        if ((uniqueId != null) && description.isTest()) {
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
			int permutationId = Objects.hash(description.getDisplayName(), assignments.getMethodArguments());
			String theoryId = String.format("%08X", permutationId);
			uniqueId.set(description, theoryId);
		} catch (CouldNotGenerateValueException | SecurityException | IllegalArgumentException
				| IllegalAccessException eaten) {
			// nothing to do here
		}
	}

}
