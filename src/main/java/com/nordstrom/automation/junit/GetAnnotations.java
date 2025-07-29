package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.model.FrameworkMethod#getAnnotations} method.
 */
public class GetAnnotations {
    
    private static final Map<String, Annotation[]> ANNOTATIONS = new ConcurrentHashMap<>();

    /**
     * Default constructor
     */
    public GetAnnotations() { }
    
    /**
     * Interceptor for the {@link org.junit.runners.model.FrameworkMethod#getAnnotations} method.
     * 
     * @param method target {@link FrameworkMethod} object
     * @return the annotations attached to the target method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static Annotation[] intercept(@This final FrameworkMethod method) throws Exception {
        return getAnnotationsFor(method);
    }
    
    /**
     * Returns the annotations on the specified method.
     * <p>
     * <b>NOTE</b>: This method caches the annotations attached to the Java {@link Method} wrapped by the specified
     * JUnit framework method and returns this on subsequent calls.
     * 
     * @param method target {@link FrameworkMethod} object
     * @return array of annotations for the specified method
     */
    static Annotation[] getAnnotationsFor(FrameworkMethod method) {
        Annotation[] annotations = ANNOTATIONS.get(method.toString());
        if (annotations == null) {
            annotations = method.getMethod().getAnnotations();
            ANNOTATIONS.put(method.toString(), annotations);
        }
        return annotations;
    }
    
    /**
     * Release the cached annotations for the specified JUnit framework method.
     * 
     * @param method target {@link FrameworkMethod} object
     */
    static void releaseAnnotationsFor(FrameworkMethod method) {
        ANNOTATIONS.remove(method.toString());
    }

}
