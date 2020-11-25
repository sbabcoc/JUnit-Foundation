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

    public static Description intercept(@This final Object runner,
                    @SuperCall final Callable<?> proxy,
                    @Argument(0) final Object child) throws Exception {
        
        Description description = (Description) LifecycleHooks.callProxy(proxy);
        if (description.isTest() && (uniqueId != null)) {
            try {
                Object parent = LifecycleHooks.getFieldValue(runner, "this$0");
                if (parent instanceof TheoryAnchor) {
                    Assignments assignments = LifecycleHooks.getFieldValue(runner, "val$complete");
                    description = describeTheory(description, assignments);
                }
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                // nothing to do here
            }
        }
        return description;
    }
    
    private static Description describeTheory(final Description description, final Assignments assignments) {
        try {
            int permutationId = Objects.hash(description.getDisplayName(), assignments.getMethodArguments());
            String theoryId = String.format("%08X", permutationId);
            uniqueId.set(description, theoryId);
        } catch (CouldNotGenerateValueException | SecurityException | IllegalArgumentException | IllegalAccessException eaten) {
            // nothing to do here
        }
        return description;
    }

}
