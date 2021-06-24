package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.junit.runner.Description;
import junitparams.Parameters;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link
 * junitparams.internal.ParametrizedDescription#parametrizedDescription
 * parametrizedDescription} method.
 */
public class ParameterizedDescription {

    private static final Field testClass;
    private static final Field uniqueId;
    
    static {
        Field field = null;
        try {
            field = Description.class.getDeclaredField("fTestClass");
            field.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            field = null;
        }
        testClass = field;
        
        field = null;
        try {
            field = Description.class.getDeclaredField("fUniqueId");
            field.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            field = null;
        }
        uniqueId = field;
    }

    /**
     * Interceptor for the {@link junitparams.internal.ParametrizedDescription#parametrizedDescription
     * parametrizedDescription} method.
     * <p>
     * This interceptor fills in the blanks of the method descriptions produced by the off-the-shelf implementation.
     * Without this intervention, the returned descriptions lack the declaring class and annotations for the method.
     * 
     * @param internal current {@link junitparams.internal.ParametrizedDescription ParametrizedDescription}
     * @param proxy callable proxy for the intercepted method
     * @param params parameters selected for this invocation of the target method
     * @return new fully specified suite description; if augmentation fails, return the original
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static Description intercept(@This final Object internal, @SuperCall final Callable<?> proxy,
            @Argument(0) final Object[] params) throws Exception {
        
        // invoke original implementation
        Description original = LifecycleHooks.callProxy(proxy);
        
        // get method for this Description
        Method method = getMethod(internal);
        if (method != null) {
            return augmentDescription(method, original);
        }
        
        return original;
    }
    
    /**
     * Augment the incomplete suite description created by JUnitParams runner.
     * <p>
     * <b>NOTE</b>: The description built by JUnitParams lack the test class and annotations.
     * 
     * @param method Java method object
     * @param description JUnit description built by JUnitParams
     * @return new augmented suite description object; if augmentation fails, returns original description
     */
    private static Description augmentDescription(final Method method, final Description description) {
        if (testClass == null) return description;
        
        int count = method.getAnnotations().length;
        Annotation[] parentAnnotations = new Annotation[count - 1];
        Annotation[] childAnnotations = new Annotation[count - 1];
        
        int parentIndex = 0;
        int childIndex = 0;
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
        
        Description augmented = Description.createSuiteDescription(method.getName(), parentAnnotations);
        
        try {
            testClass.set(augmented, method.getDeclaringClass());
        } catch (IllegalArgumentException | IllegalAccessException eaten) {
            return description;
        }
        
        for (Description child : description.getChildren()) {
            augmented.addChild(augmentChild(method.getDeclaringClass(), child, childAnnotations));
        }
        
        return augmented;
    }
    
    /**
     * Augment the specified parameterized child method description.
     * 
     * @param clazz declaring class for the represented Java method
     * @param child parameterized child method description
     * @param annotations child method annotations
     * @return new augmented child method description; if augmentation fails, return original description
     */
    private static Description augmentChild(Class<?> clazz, final Description child, Annotation... annotations) {
        if (uniqueId == null) return child;
        
        Description augmented = Description.createTestDescription(clazz, child.getDisplayName(), annotations);

        try {
            uniqueId.set(augmented, uniqueId.get(child));
            return augmented;
        } catch (IllegalArgumentException | IllegalAccessException eaten) {
            return child;
        }
    }

    /**
     * Get test class associated with the specified parameterized description object.
     * 
     * @param internal JUnitParams parameterized description object
     * @return associated test class; {@code null} if acquisition fails
     */
    private static Class<?> getTestClass(Object internal) {
        try {
            String testClassName = LifecycleHooks.getFieldValue(internal, "testClassName");
            return Class.forName(testClassName);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | ClassNotFoundException eaten) {
            return null;
        }
    }
    
    /**
     * Get test method associated with the specified parameterized description object.
     * 
     * @param internal JUnitParams parameterized description object
     * @return associated test method; {@code null} if acquisition fails
     */
    private static Method getMethod(Object internal) {
        Class<?> testClass = getTestClass(internal);
        if (testClass != null) {
            try {
                String methodName = LifecycleHooks.getFieldValue(internal, "methodName");
                for (Method method : testClass.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        return method;
                    }
                }
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException eaten) {
                // nothing to do here
            }
        }
        return null;
    }
    
}
