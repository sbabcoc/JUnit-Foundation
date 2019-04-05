package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RuleThrowsException extends TestBase {
    
    @Rule
    public final TestRule failing = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            throw new RuntimeException("Must be failed");
        }
    };
    
    @Test
    public void happy() {
        assertTrue(true);
    }

}
