package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

@SuppressWarnings("squid:S1118")
public class ApplyMethodRule {
    
    private static final Map<Object, Object> STATEMENT_TO_METHODRULE = new ConcurrentHashMap<>();
    private static final Map<Object, Object> STATEMENT_CHAIN = new ConcurrentHashMap<>();
    private static final Map<Object, FrameworkMethod> METHODRULE_TO_METHOD = new ConcurrentHashMap<>();
    private static final Map<Object, Object> METHODRULE_TO_TARGET = new ConcurrentHashMap<>();
    
    public static Object intercept(@This final Object methodRule,
                    @SuperCall final Callable<?> proxy, @Argument(0) final Object prev,
                    @Argument(1) final FrameworkMethod method, @Argument(2) final Object target)
                    throws Exception {
        Object next = (Object) proxy.call();
        STATEMENT_TO_METHODRULE.put(next, methodRule);
        STATEMENT_CHAIN.put(next, prev);
        return next;
    }
    
    static boolean isMethodRule(Object statement) {
        return STATEMENT_TO_METHODRULE.containsKey(statement);
    }
    
    static FrameworkMethod getMethodFor(Object statement) {
        return METHODRULE_TO_METHOD.get(getMethodRuleFor(statement));
    }
    
    static Object getTargetFor(Object statement) {
        return METHODRULE_TO_TARGET.get(getMethodRuleFor(statement));
    }
    
    static Object getMethodRuleFor(Object statement) {
        Object methodRule = STATEMENT_TO_METHODRULE.get(statement);
        if (methodRule != null) {
            return methodRule;
        }
        throw new IllegalArgumentException("No associated method rule found for the specified statement");
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
        while (isMethodRule(current)) {
            current = STATEMENT_CHAIN.get(statement);
        }
        return current;
    }
}
