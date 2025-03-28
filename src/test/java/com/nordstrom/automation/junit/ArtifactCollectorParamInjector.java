package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameters;

@RunWith(TestParameterInjector.class)
public class ArtifactCollectorParamInjector extends TestBase {
    
    @Override
    public Optional<Map<String, Object>> getParameters() {
        AtomicTest atomicTest = LifecycleHooks.getAtomicTestOf(this);
        FrameworkMethod method = atomicTest.getIdentity();
        
        // get test method parameters
        Class<?>[] paramTypes = method.getMethod().getParameterTypes();
        
        try {
            Object testInfo = LifecycleHooks.getFieldValue(method, "testInfo");
            List<Object> params = LifecycleHooks.getFieldValue(testInfo, "parameters");
            
            // allocate named parameters array
            Param[] namedParams = new Param[params.size()];
            // populate named parameters array
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                Map<String, Object> paramValue = LifecycleHooks.getFieldValue(param, "value");
                Entry<String, Object> paramEntry = paramValue.entrySet().iterator().next();
                namedParams[i] = Param.param(paramEntry.getKey(), paramTypes[i].cast(paramEntry.getValue()));
            }

            // return params map as Optional
            return Param.mapOf(namedParams);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return Optional.empty();
        }
    }
    
    @Test
    @TestParameters("{input: 'first test'}")
    @TestParameters("{input: 'second test'}")
    public void parameterized(String input) {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }
}
