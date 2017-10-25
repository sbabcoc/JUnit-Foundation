package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.file.PathUtils.ReportsDirectory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;

/**
 * This JUnit test runner uses bytecode enhancement to install hooks on test and configuration methods to enable
 * method pre-processing and post-processing. This closely resembles the {@code IInvokedMethodListener} feature
 * of TestNG. Classes that implement the {@link MethodWatcher} interface are attached to these hooks via the
 * {@link MethodWatchers} annotation, which is applied to applicable test classes.
 */
public final class HookInstallingRunner extends BlockJUnit4ClassRunner {
    
    private static Map<Class<?>, Class<?>> proxyMap = new HashMap<>();
    
    public HookInstallingRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object createTest() throws Exception {
        return installHooks(super.createTest());
    }
    
    /**
     * Create an enhanced instance of the specified test class object.
     * 
     * @param testObj test class object to be enhanced
     * @return enhanced test class object
     */
    private synchronized Object installHooks(Object testObj) {
        Class<?> testClass = testObj.getClass();
        MethodInterceptor.attachWatchers(testClass);
        
        if (testObj instanceof Hooked) {
            return testObj;
        }
        
        Class<?> proxyType = proxyMap.get(testClass);
        
        if (proxyType == null) {
            try {
                proxyType = new ByteBuddy()
                        .subclass(testClass)
                        .name(getSubclassName(testObj))
                        .method(isAnnotatedWith(anyOf(Test.class, Before.class, After.class)))
                        .intercept(MethodDelegation.to(MethodInterceptor.class))
                        .implement(Hooked.class)
                        .make()
                        .load(testClass.getClassLoader())
                        .getLoaded();
                proxyMap.put(testClass, proxyType);
            } catch (SecurityException | IllegalArgumentException e) {
                throw UncheckedThrow.throwUnchecked(e);
            }
        }
            
        try {
            return proxyType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }
    
    /**
     * Get class of specified test class instance.
     * 
     * @param instance test class instance
     * @return class of test class instance
     */
    public static Class<?> getInstanceClass(Object instance) {
        Class<?> clazz = instance.getClass();      
        return (instance instanceof Hooked) ? clazz.getSuperclass() : clazz;
    }
    
    /**
     * Get fully-qualified name to use for hooked test class.
     * 
     * @param testObj test class object being hooked
     * @return fully-qualified name for hooked subclass
     */
    private static String getSubclassName(Object testObj) {
        Class<?> testClass = testObj.getClass();
        String testClassName = testClass.getSimpleName();
        String testPackageName = testClass.getPackage().getName();
        ReportsDirectory constant = ReportsDirectory.fromObject(testObj);
        
        switch (constant) {
            case FAILSAFE_2:
            case FAILSAFE_3:
            case SUREFIRE_2:
            case SUREFIRE_3:
            case SUREFIRE_4:
                return testPackageName + ".Hooked" + testClassName;
                
            default:
                return testClass.getCanonicalName() + "Hooked";
        }
        
    }
}
