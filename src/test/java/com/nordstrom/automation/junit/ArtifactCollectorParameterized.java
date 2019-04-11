package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Optional;

@RunWith(Parameterized.class)
public class ArtifactCollectorParameterized extends TestBase {
    
    private String input;
    
    public ArtifactCollectorParameterized(String input) {
        this.input = input;
    }
    
    @Parameters
    public static Object[] data() {
        return new Object[] { "first test", "second test" };
    }
    
    @Override
    public Optional<Map<String, Object>> getParameters() {
        return Param.mapOf(Param.param("input", input));
    }
    
    @Test
    public void parameterized() {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }
}
