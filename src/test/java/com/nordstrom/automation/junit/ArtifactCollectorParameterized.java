package com.nordstrom.automation.junit;

import static org.junit.Assert.assertArrayEquals;

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
    public Object[] getParameters() {
        return new Object[] { input };
    }
    
    @Test
    public void parameterized() {
        assertArrayEquals(getParameters(), watcher.getParameters());
    }
}
