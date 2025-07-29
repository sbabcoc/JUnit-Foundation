package com.nordstrom.automation.junit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestTimedOutException;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is a mutable implementation of the {@link Test &#64;Test} annotation interface. It includes a static
 * {@link #proxyFor(FrameworkMethod, long)} method that replaces the immutable annotation attached to a JUnit test
 * method with an instance of this class to apply the global test timeout.
 */
@Ignore
@SuppressWarnings("all")
public class MutableTest implements Test {
    
    private final Class<? extends Throwable> expected;
    private final long timeout;
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Test &#64;Test}
     * annotation.
     * 
     * @param annotation {@link Test &#64;Test} annotation specifying desired parameters
     */
    protected MutableTest(Test annotation) {
        this.expected = annotation.expected();
        this.timeout = annotation.timeout();
    }
    
    /**
     * Constructor: Populate the fields of this object from the parameters of the specified {@link Test &#64;Test}
     * annotation.
     * 
     * @param annotation {@link Test &#64;Test} annotation specifying desired parameters
     * @param timeout timeout interval in milliseconds
     */
    private MutableTest(Test annotation, long timeout) {
        this.expected = annotation.expected();
        this.timeout = timeout;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
    
    @Override
    public Class<? extends Throwable> expected() {
        return expected;
    }
    
    @Override
    public long timeout() {
        return timeout;
    }
    
    @Override
    public String toString() {
        return "@" + getClass().getName() + "(timeout=" + timeout + ", expected=" + expected.getName() + ")";
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expected == null) ? 0 : expected.hashCode());
        result = prime * result + (int) (timeout ^ (timeout >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if ( ! (obj instanceof MutableTest))
            return false;
        MutableTest other = (MutableTest) obj;
        if (expected == null) {
            if (other.expected != null)
                return false;
        } else if (!expected.equals(other.expected))
            return false;
        if (timeout != other.timeout)
            return false;
        return true;
    }
    
    /**
     * Create a {@link Test &#64;Test} annotation proxy for the specified test method.
     * 
     * @param method test method to which {@code @Test} annotation proxy will be attached
     * @param timeout timeout interval in milliseconds
     */
    static void proxyFor(final FrameworkMethod method, final long timeout) {
        Test declared = method.getAnnotation(Test.class);
        if (declared != null) {
            GetAnnotation.injectProxy(method, new MutableTest(declared, timeout));
            return;
        }
        throw new IllegalArgumentException("Specified method is not a JUnit @Test: " + method);
    }
}
