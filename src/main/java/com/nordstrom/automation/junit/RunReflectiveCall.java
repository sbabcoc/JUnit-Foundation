package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.getFieldValue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;

import com.nordstrom.automation.junit.LifecycleHooks.CreateTest;
import com.nordstrom.automation.junit.LifecycleHooks.Run;
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
    
    private static final ThreadLocal<Boolean> BELOW = new InheritableThreadLocal<Boolean>() {
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };
    
    private static final ServiceLoader<MethodWatcher> methodWatcherLoader;
    private static final ServiceLoader<RunWatcher> runWatcherLoader;
    
    private static final Map<Object, AtomicTest> RUNNER_TO_ATOMICTEST = new ConcurrentHashMap<>();
  
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
        
        if (BELOW.get()) {
            return LifecycleHooks.callProxy(proxy);
        }
        
        Object runner = null;
        Object target = null;
        FrameworkMethod method = null;
        Object[] params = null;

        try {
            Object owner = getFieldValue(callable, "this$0");
            if (owner instanceof FrameworkMethod) {
                method = (FrameworkMethod) owner;
                target = getFieldValue(callable, "val$target");
                params = getFieldValue(callable, "val$params");
                
                if (isParticleMethod(method)) {
                    if (target != null) {
                        runner = CreateTest.getRunnerForTarget(target);
                    } else {
                        runner = Run.getThreadRunner();
                    }
                }
            } else {
                runner = owner;
            }
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
            // handled below
        }
        
        if (method == null) {
            return LifecycleHooks.callProxy(proxy);
        }
        
        Object result = null;
        Throwable thrown = null;
        synchronized(methodWatcherLoader) {
            for (MethodWatcher watcher : methodWatcherLoader) {
                watcher.beforeInvocation(runner, target, method, params);
            }
        }

        try {
            BELOW.set(Boolean.TRUE);
            result = LifecycleHooks.callProxy(proxy);
        } catch (Throwable t) {
            thrown = t;
        } finally {
            BELOW.set(Boolean.FALSE);
            synchronized(methodWatcherLoader) {
                for (MethodWatcher watcher : methodWatcherLoader) {
                    watcher.afterInvocation(runner, target, method, thrown);
                }
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
     * @param runnable {@link Runnable} object that wraps the atomic test
     */
    static void fireTestStarted(Runnable runnable) {
        AtomicTest atomicTest = createAtomicTest(runnable);
        if (atomicTest != null) {
            synchronized(runWatcherLoader) {
                for (RunWatcher watcher : runWatcherLoader) {
                    watcher.testStarted(atomicTest);
                }
            }
        }
    }
    
    /**
     * Invoke to tell listeners that an atomic test has finished.
     * 
     * @param runner JUnit test runner
     */
    static void fireTestFinished(Object runner) {
        AtomicTest atomicTest = RUNNER_TO_ATOMICTEST.get(runner);
        if (atomicTest != null) {
            synchronized(runWatcherLoader) {
                for (RunWatcher watcher : runWatcherLoader) {
                    notifyIfTestFailed(watcher, atomicTest);
                    watcher.testFinished(atomicTest);
                }
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
                watcher.testAssumptionFailure(atomicTest, (AssumptionViolatedException) thrown);
            } else {
                watcher.testFailure(atomicTest, thrown);
            }
        }
    }
    
    /**
     * Invoke to tell listeners that an atomic test was ignored.
     * 
     * @param runner JUnit test runner
     * @param method {@link FrameworkMethod} object
     */
    static void fireTestIgnored(Object runner, FrameworkMethod method) {
        AtomicTest atomicTest = createAtomicTest(runner, method);
        synchronized(runWatcherLoader) {
            for (RunWatcher watcher : runWatcherLoader) {
                watcher.testIgnored(atomicTest);
            }
        }
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
        synchronized(methodWatcherLoader) {
            for (MethodWatcher watcher : methodWatcherLoader) {
                if (watcher.getClass() == watcherType) {
                    return Optional.of(watcher);
                }
            }
        }
        return Optional.empty();
    }
    
    /**
     * Create an atomic test object from the specified runnable object.
     * 
     * @param runnable {@link Runnable} object that wraps the atomic test
     * @return {@link AtomicTest} object; {@code null} if the specified runnable is a suite
     */
    static AtomicTest createAtomicTest(Runnable runnable) {
        Object runner = null;
        Object child = null;
        AtomicTest atomicTest = null;
        
        try {
            runner = getFieldValue(runnable, "this$0");
            child = getFieldValue(runnable, "val$each");
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            // nothing to do here
        }
        
        if (child instanceof FrameworkMethod) {
            atomicTest = createAtomicTest(runner, (FrameworkMethod) child);
        }
        
        return atomicTest;
    }
    
    /**
     * Create an atomic test object from the specified runner/method pair.
     * 
     * @param runner JUnit test runner
     * @param method {@link FrameworkMethod} object
     * @return {@link AtomicTest} object
     */
    static AtomicTest createAtomicTest(Object runner, FrameworkMethod method) {
        AtomicTest atomicTest = new AtomicTest(runner, method);
        RUNNER_TO_ATOMICTEST.put(runner, atomicTest);
        return atomicTest;
    }
    
    /**
     * Get the atomic test associated with the specified test runner.
     * 
     * @param runner JUnit test runner
     * @return {@link AtomicTest} object for the specified test runner
     */
    public static AtomicTest getAtomicTestFor(Object runner) {
        AtomicTest atomicTest = RUNNER_TO_ATOMICTEST.get(runner);
        if (atomicTest != null) {
            return atomicTest;
        }
        throw new IllegalArgumentException("No associated atomic test was found for the specified test runner");
    }
    
    /**
     * Get the atomic test associated with the specified method.
     * 
     * @param method {@link FrameworkMethod} object
     * @return {@link AtomicTest} object for the specified method
     */
    public static AtomicTest getAtomicTestFor(FrameworkMethod method) {
        for (AtomicTest atomicTest : RUNNER_TO_ATOMICTEST.values()) {
            if (atomicTest.includes(method)) {
                return atomicTest;
            }
        }
        throw new IllegalArgumentException("No associated atomic test was found for the specified method");
    }
    
    /**
     * Determine if the specified method is a test or configuration method.
     * 
     * @param method method whose type is in question
     * @return {@code true} if specified method is a particle; otherwise {@code false}
     */
    public static boolean isParticleMethod(FrameworkMethod method) {
        return ((null != method.getAnnotation(Test.class)) ||
                (null != method.getAnnotation(Before.class)) ||
                (null != method.getAnnotation(After.class)) ||
                (null != method.getAnnotation(BeforeClass.class)) ||
                (null != method.getAnnotation(AfterClass.class)));
    }
}
