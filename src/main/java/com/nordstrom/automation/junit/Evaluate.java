package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

@SuppressWarnings("squid:S1118")
public class Evaluate {
    
    private static final Map<Object, Object> STATEMENT_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<Object, FrameworkMethod> TARGET_TO_METHOD = new ConcurrentHashMap<>();
    
    public static void intercept(@This final Object statement,
                    @SuperCall final Callable<?> proxy) throws Exception {
        
        try {
            Object target = getFieldValue(statement, "target");
            FrameworkMethod method = getFieldValue(statement, "testMethod");
            
            // if static method
            if (target == null) {
                target = method.getDeclaringClass();
            }
            
            STATEMENT_TO_TARGET.put(statement, target);
            if (statement instanceof InvokeMethod) {
                TARGET_TO_METHOD.put(target, method);
            }
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            // nothing to do here
        }
        
        proxy.call();
    }
    
    private static Object getTargetFor(Object statement) {
        Object target = STATEMENT_TO_TARGET.get(statement);
        if (target != null) {
            return target;
        }
        throw new IllegalArgumentException("No associated test class instance was found for the specified statement");
    }
    
    private static FrameworkMethod getMethodFor(Object target) {
        FrameworkMethod method = TARGET_TO_METHOD.get(target);
        if (method != null) {
            return method;
        }
        throw new IllegalArgumentException("No associated method was found for the specified test class instance");
    }
    
    static AtomicTest getAtomicTestFor(Object statement) {
        Object current = statement;
        if (ApplyTestRule.isTestRule(current)) {
            current = ApplyTestRule.getUpstreamStatement(current);
        }
        
        if (ApplyMethodRule.isMethodRule(current)) {
            current = ApplyMethodRule.getUpstreamStatement(current);
        }
        
        for (; current != null; current = getNextStatement(current)) {
            if (current instanceof InvokeMethod) {
                return new AtomicTest(getMethodFor(current), getTargetFor(current));
            }
        }
        
        return null;
    }
    
    private static Object getNextStatement(Object statement) {
        try {
            for (Field field : statement.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object candidate;
                    candidate = field.get(statement);
                if (isStatement(candidate)) {
                    return candidate;
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // nothing to do here
        }
        
        return null;
    }
    
    private static boolean isStatement(Object candidate) {
        for (Class<?> current = candidate.getClass(); current != null; current = current.getSuperclass()) {
            if ("org.junit.runners.model.Statement".equals(current.getName())) {
                return true;
            }
        }
        return false;
    }
}
