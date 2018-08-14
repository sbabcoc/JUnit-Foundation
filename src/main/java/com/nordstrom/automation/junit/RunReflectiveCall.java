package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
 * runReflectiveCall} method.
 */
@SuppressWarnings("squid:S1118")
public class RunReflectiveCall {
    
    private static final ServiceLoader<MethodWatcher> methodWatcherLoader;
    private static final ServiceLoader<RunWatcher> runWatcherLoader;
    
    private static final Map<FrameworkMethod, Object> METHOD_TO_TARGET = new ConcurrentHashMap<>();
    private static final Map<TestClass, AtomicTest> TESTCLASS_TO_ATOMICTEST = new ConcurrentHashMap<>();
  
    static {
        methodWatcherLoader = ServiceLoader.load(MethodWatcher.class);
        runWatcherLoader = ServiceLoader.load(RunWatcher.class);
    }
    
    /**
     * Interceptor for the {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
     * runReflectiveCall} method.
     * 
     * @param callable {@code ReflectiveCallable} object being intercepted 
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - value returned by the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static Object intercept(@This final Object callable, @SuperCall final Callable<?> proxy)
                    throws Exception {
        
        FrameworkMethod method = null;
        Object target = null;
        Object[] params = null;

        try {
            Object owner = getFieldValue(callable, "this$0");
            if (owner instanceof FrameworkMethod) {
                method = (FrameworkMethod) owner;
                target = getFieldValue(callable, "val$target");
                params = getFieldValue(callable, "val$params");
                
                // if not static
                if (target != null) {
                    METHOD_TO_TARGET.put(method, target);
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
        }
        
        if (method == null) {
            return proxy.call();
        }

        Object result = null;
        Throwable thrown = null;
        for (MethodWatcher watcher : methodWatcherLoader) {
            watcher.beforeInvocation(target, method, params);
        }

        try {
            result = proxy.call();
        } catch (Throwable t) {
            thrown = t;
        } finally {
            for (MethodWatcher watcher : methodWatcherLoader) {
                watcher.afterInvocation(target, method, thrown);
            }
        }

        if (thrown != null) {
            getAtomicTestFor(method).setThrowable(thrown);
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return result;
    }
    
    /**
     * Invoke to tell listeners that an atomic test is about to start.
     * 
     * @param testClass {@link TestClass} object for the atomic test
     * @param runnable {@link Runnable} object that wraps the atomic test
     */
    static void fireTestStarted(TestClass testClass, Runnable runnable) {
        AtomicTest atomicTest = createAtomicTest(testClass, runnable);
        if (atomicTest != null) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testStarted(atomicTest.getTestMethod(), atomicTest.getTestClass());
            }
        }
    }
    
    /**
     * Invoke to tell listeners that an atomic test has finished.
     * 
     * @param testClass {@link TestClass} object for the atomic test
     */
    static void fireTestFinished(TestClass testClass) {
        AtomicTest atomicTest = TESTCLASS_TO_ATOMICTEST.get(testClass);
        if (atomicTest != null) {
            for (RunWatcher watcher : runWatcherLoader) {
                notifyIfTestFailed(watcher, atomicTest);
                watcher.testFinished(atomicTest.getTestMethod(), atomicTest.getTestClass());
            }
        }
    }
    
    /**
     * Notify the indicated method watcher if the specified atomic test failed.
     * 
     * @param watcher {@link RunWatcher} object
     * @param atomicTest {@link AtomicTest} object
     */
    private static void notifyIfTestFailed(RunWatcher watcher, AtomicTest atomicTest) {
        Throwable thrown = atomicTest.getThrowable();
        if (thrown != null) {
            if (thrown instanceof AssumptionViolatedException) {
                watcher.testAssumptionFailure(atomicTest.getTestMethod(), atomicTest.getTestClass(),
                                (AssumptionViolatedException) thrown);
            } else {
                watcher.testFailure(atomicTest.getTestMethod(), atomicTest.getTestClass(), thrown);
            }
        }
    }
    
    /**
     * Invoke to tell listeners that an atomic test was ignored.
     * 
     * @param runnable {@link Runnable} object that wraps the atomic test
     */
    static void fireTestIgnored(FrameworkMethod method) {
        Object target = getTargetFor(method);
        TestClass testClass = LifecycleHooks.getTestClassFor(target);
        for (RunWatcher watcher : runWatcherLoader) {
            watcher.testIgnored(method, testClass);
        }
    }
    
    /**
     * Get the target test class instance for the specified method.
     * 
     * @param method {@link FrameworkMethod} object
     * @return target test class instance for the specified method
     */
    static Object getTargetFor(FrameworkMethod method) {
        Object target = METHOD_TO_TARGET.get(method);
        if (target != null) {
            return target;
        }
        throw new IllegalArgumentException("No associated test class instance was found for the specified method");
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    public static Optional<MethodWatcher> getAttachedWatcher(
                    Class<? extends MethodWatcher> watcherType) {
        Objects.requireNonNull(watcherType, "[watcherType] must be non-null");
        for (MethodWatcher watcher : methodWatcherLoader) {
            if (watcher.getClass() == watcherType) {
                return Optional.of(watcher);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Create an atomic test object from the specified runnable object.
     * 
     * @param testClass {@link TestClass} object for the atomic test
     * @param runnable {@link Runnable} object that wraps the atomic test
     * @return {@link AtomicTest} object; {@code null} if the specified runnable is a suite
     */
    static AtomicTest createAtomicTest(TestClass testClass, Runnable runnable) {
        Object runner = null;
        Object child = null;
        AtomicTest atomicTest = null;
        
        try {
            runner = LifecycleHooks.getFieldValue(runnable, "this$0");
            child = LifecycleHooks.getFieldValue(runnable, "val$each");
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            // nothing to do here
        }
        
        if (child instanceof FrameworkMethod) {
            atomicTest = new AtomicTest(runner, testClass, (FrameworkMethod) child);
            TESTCLASS_TO_ATOMICTEST.put(testClass, atomicTest);
        }
        
        return atomicTest;
    }
    
    /**
     * Get the atomic test associated with the specified test class.
     * 
     * @param testClass {@link TestClass} object
     * @return {@link AtomicTest} object for the specified test class
     */
    public static AtomicTest getAtomicTestFor(TestClass testClass) {
        AtomicTest atomicTest = TESTCLASS_TO_ATOMICTEST.get(testClass);
        if (atomicTest != null) {
            return atomicTest;
        }
        throw new IllegalArgumentException("No associated atomic test was found for the specified test class");
    }
    
    /**
     * Get the atomic test associated with the specified method.
     * 
     * @param method {@link FrameworkMethod} object
     * @return {@link AtomicTest} object for the specified method
     */
    public static AtomicTest getAtomicTestFor(FrameworkMethod method) {
        for (AtomicTest atomicTest : TESTCLASS_TO_ATOMICTEST.values()) {
            if (atomicTest.contains(method)) {
                return atomicTest;
            }
        }
        throw new IllegalArgumentException("No associated atomic test was found for the specified method");
    }
}
