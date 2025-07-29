package com.nordstrom.automation.junit;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import junitparams.Parameters;
import junitparams.internal.TestMethod;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link TestMethod#description description} method.
 */
public class TestMethodDescription {
    
    private static final Map<Integer, Description> TESTMETHOD_TO_DESCRIPTION = new ConcurrentHashMap<>();

    /**
     * Default constructor
     */
    public TestMethodDescription() { }
    
    /**
     * Interceptor for the {@link TestMethod#description description} method.
     * 
     * @param testMethod target JUnitParams {@link TestMethod} object
     * @param proxy callable proxy for the intercepted method
     * @return {@link Description} for target test method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static Description intercept(@This final TestMethod testMethod, @SuperCall final Callable<?> proxy) throws Exception {
        
        Description description = TESTMETHOD_TO_DESCRIPTION.get(testMethod.hashCode());
        if (description == null) {
            description = computeDescription(testMethod, proxy);
            TESTMETHOD_TO_DESCRIPTION.put(testMethod.hashCode(), description);
        }
        return description;
        
    }
    
    /**
     * Compute the description for the target test method.
     * 
     * @param testMethod target JUnitParams {@link TestMethod} object
     * @param proxy callable proxy for the intercepted method
     * @return {@link Description} for target test method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    private static Description computeDescription(TestMethod testMethod, Callable<?> proxy) throws Exception {
        // invoke original implementation
        Description original = LifecycleHooks.callProxy(proxy);
        // if original is childless, return it
        if (original.isTest()) return original;
        
        int parentIndex = 0;
        int childIndex = 0;
        List<Description> children = original.getChildren();
        Method method = testMethod.frameworkMethod().getMethod();
        
        int count = method.getAnnotations().length;
        Annotation[] parentAnnotations = new Annotation[count - 1];
        Annotation[] childAnnotations = new Annotation[count - 1];
        
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof Parameters) {
                parentAnnotations[parentIndex++] = annotation;
            } else if (annotation instanceof Test) {
                childAnnotations[childIndex++] = annotation;
            } else {
                parentAnnotations[parentIndex++] = annotation;
                childAnnotations[childIndex++] = annotation;
            }
        }
        
        Description parent = Description.createTestDescription(method.getDeclaringClass(), method.getName(), parentAnnotations);
        
        for (Description description : children) {
            String displayName = description.getDisplayName();
            Serializable uniqueId = ((UniqueIdAccessor) description).getUniqueId();
            parent.addChild(Description.createSuiteDescription(displayName, uniqueId, childAnnotations));
        }
        
        return parent;
    }

    /**
     * Release the description that was cached for the specified method.
     * 
     * @param method JUnit framework method
     */
    static void releaseDescriptionFor(FrameworkMethod method) {
        TESTMETHOD_TO_DESCRIPTION.remove(method.hashCode());
    }

}
