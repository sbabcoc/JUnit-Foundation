package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.model.FrameworkMethod#getAnnotation} method.
 */
public class GetAnnotation {
    
    /**
     * Interceptor for the {@link org.junit.runners.model.FrameworkMethod#getAnnotation} method.
     * <p>
     * <b>NOTE</b>: This interceptor does <b>not</b> invoke the original implementation in {@link FrameworkMethod}. It
     * relies instead on the cached annotations collected by the {@link GetAnnotations} class, which injects a proxied
     * replacement for the <b>{@code @Test}</b> annotation that enables global test timeout management.
     * 
     * @param <T> desired annotation type
     * @param method target {@link FrameworkMethod} object
     * @param annotationType desired annotation type
     * @return this element's annotation for the specified annotation type if present on this element, else null
     * @throws NullPointerException if the given annotation class is {@code null}
     */
    public static <T extends Annotation> T intercept(@This final FrameworkMethod method, @Argument(0) final Class<T> annotationType) {
        Objects.requireNonNull(annotationType);
        for (Annotation annotation : GetAnnotations.getAnnotationsFor(method)) {
            if (annotation.annotationType().equals(annotationType)) {
                return annotationType.cast(annotation);
            }
        }
        return null;
    }
    
    /**
     * Inject the specified proxy annotation into the indicated method.
     * 
     * @param method target {@link FrameworkMethod} object
     * @param proxyAnnotation mutable proxy annotation ({@link MutableTest})
     */
    static void injectProxy(FrameworkMethod method, Annotation proxyAnnotation) {
    	Annotation[] annotations = GetAnnotations.getAnnotationsFor(method);
    	for (int i = 0; i < annotations.length; i++) {
    		if (annotations[i].annotationType().equals(proxyAnnotation.annotationType())) {
    			annotations[i] = proxyAnnotation;
    			break;
    		}
    	}
    }
    
}
