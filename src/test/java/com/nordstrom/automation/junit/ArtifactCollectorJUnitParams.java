package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.internal.runners.model.ReflectiveCallable;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class ArtifactCollectorJUnitParams extends TestBase {

    @Override
    public Optional<Map<String, Object>> getParameters() {
        // get atomic test associated with this instance
        AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(this);
        // get "callable" closure of framework method
        ReflectiveCallable callable = LifecycleHooks.getCallableOf(atomicTest.getDescription());
        // get test method parameters
        Class<?>[] paramTypes = atomicTest.getIdentity().getMethod().getParameterTypes();

        try {
            // extract execution parameters from "callable" closure
            Object[] params = LifecycleHooks.getFieldValue(callable, "val$params");

            // NOTE: The "callable" closure also includes:
            // * this$0 - test case FrameworkMethod object
            // * val$target - test class instance ('this')
            // Only 'val$params' is unavailable elsewhere.

            // allocate named parameters array
            Param[] namedParams = new Param[params.length];
            // populate named parameters array
            for (int i = 0; i < params.length; i++) {
                // create array item with generic name
                namedParams[i] = Param.param("param" + i, paramTypes[i].cast(params[i]));
            }

            // return params map as Optional
            return Param.mapOf(namedParams);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return Optional.empty();
        }
    }
    
    @Test
    @Parameters({ "first test", "second test" })
    public void parameterized(String input) {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }
}
