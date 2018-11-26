package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;
import static com.nordstrom.automation.junit.ArtifactParams.param;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
        return ArtifactParams.mapOf(param("input", input));
    }
    
    @Test
    public void parameterized() {
        System.out.println("parameterized: input = [" + input + "]");
        assertEquals("first test", input);
    }
}
