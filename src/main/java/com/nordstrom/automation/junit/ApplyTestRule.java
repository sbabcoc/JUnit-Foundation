package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

@SuppressWarnings("squid:S1118")
public class ApplyTestRule {
    
    private static final Map<Object, Object> STATEMENT_TO_TESTRULE = new ConcurrentHashMap<>();
    private static final Map<Object, Object> STATEMENT_CHAIN = new ConcurrentHashMap<>();
    
    public static Object intercept(@This final Object testRule,
                    @SuperCall final Callable<?> proxy, @Argument(0) final Object prev)
                    throws Exception {
        Object next = (Object) proxy.call();
        STATEMENT_TO_TESTRULE.put(next, testRule);
        STATEMENT_CHAIN.put(next, prev);
        return next;
    }
    
    static boolean isTestRule(Object statement) {
        return STATEMENT_TO_TESTRULE.containsKey(statement);
    }
    
    static Object getTestRuleFor(Object statement) {
        Object testRule = STATEMENT_TO_TESTRULE.get(statement);
        if (testRule != null) {
            return testRule;
        }
        throw new IllegalArgumentException("No associated test rule found for the specified statement");
    }
    
    static Object getPreviousStatement(Object statement) {
        Object previous = STATEMENT_CHAIN.get(statement);
        if (previous != null) {
            return previous;
        }
        throw new IllegalArgumentException("No mapping found for the specified statement");
    }
    
    static Object getUpstreamStatement(Object statement) {
        Object current = statement;
        while (isTestRule(current)) {
            current = STATEMENT_CHAIN.get(statement);
        }
        return current;
    }
}
