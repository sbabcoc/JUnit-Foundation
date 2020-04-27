package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMethod;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import com.google.common.base.Optional;

@RunWith(JUnitParamsRunner.class)
public class ArtifactCollectorJUnitParams extends TestBase {

    @Override
    public Optional<Map<String, Object>> getParameters() {
        // get runner for this target
        Object runner = LifecycleHooks.getRunnerForTarget(this);
        // get atomic test of target runner
        AtomicTest<FrameworkMethod> test = LifecycleHooks.getAtomicTestOf(runner);
        // get "callable" closure of test method
        ReflectiveCallable callable = LifecycleHooks.getCallableOf(runner, test.getIdentity());
        // get test method parameters
        Class<?>[] paramTypes = test.getIdentity().getMethod().getParameterTypes();

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
            return Optional.absent();
        }
    }
    
    @Test
    @Parameters({ "first test", "second test" })
    public void parameterized(String input) {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }
}
