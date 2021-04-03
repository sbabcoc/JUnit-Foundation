package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.PotentialAssignment.CouldNotGenerateValueException;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import com.google.common.base.Optional;

@RunWith(Theories.class)
public class ArtifactCollectorTheories extends TestBase {

    @Override
    public Optional<Map<String, Object>> getParameters() {
        // get atomic test associated with this instance
        AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(this);
        // get test method parameters
        Class<?>[] paramTypes = atomicTest.getIdentity().getMethod().getParameterTypes();

        try {
            // get parameters assigned to this iteration of the theory
            Assignments assignments = LifecycleHooks.getFieldValue(atomicTest.getRunner(), "val$complete");
            // extract invocation parameters
            Object[] params = assignments.getMethodArguments();

            // allocate named parameters array
            Param[] namedParams = new Param[params.length];
            // populate named parameters array
            for (int i = 0; i < params.length; i++) {
                // create array item with generic name
                namedParams[i] = Param.param("param" + i, paramTypes[i].cast(params[i]));
            }

            // return params map as Optional
            return Param.mapOf(namedParams);
        } catch (IllegalAccessException | NoSuchFieldException | CouldNotGenerateValueException e) {
            return Optional.absent();
        }
    }

    @DataPoints
    public static String[] data() {
        return new String[] { "first test", "second test" };
    }

    @Theory
    public void parameterized(final String input) {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }
}
