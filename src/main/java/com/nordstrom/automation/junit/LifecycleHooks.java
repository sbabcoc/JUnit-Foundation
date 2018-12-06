package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestClass;
import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;
import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.file.PathUtils.ReportsDirectory;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.pool.TypePool;

/**
 * This class implements the hooks and utility methods that activate the core functionality of <b>JUnit Foundation</b>.
 */
public class LifecycleHooks {

    private static JUnitConfig config;
    
    private LifecycleHooks() {
        throw new AssertionError("LifecycleHooks is a static utility class that cannot be instantiated");
    }
    
    /**
     * This static initializer installs a shutdown hook for each specified listener. It also rebases the ParentRunner
     * and BlockJUnit4ClassRunner classes to enable the core functionality of JUnit Foundation.
     */
    static {
        for (ShutdownListener listener : ServiceLoader.load(ShutdownListener.class)) {
            Runtime.getRuntime().addShutdownHook(getShutdownHook(listener));
        }
    }
    
    /**
     * This is the main entry point for the Java agent used to transform {@code ParentRunner} and
     * {@code BlockJUnit4ClassRunner}.
     *  
     * @param agentArgs agent options
     * @param instrumentation {@link Instrumentation} object used to transform JUnit core classes
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        installTransformer(instrumentation);
    }
    
    /**
     * Install the {@code Byte Buddy} byte code transformations that provide test fine-grained test lifecycle hooks.
     * 
     * @param instrumentation {@link Instrumentation} object used to transform JUnit core classes
     * @return The installed class file transformer
     */
    public static ClassFileTransformer installTransformer(Instrumentation instrumentation) {
        TypeDescription reflectiveCallable = TypePool.Default.ofSystemLoader().describe("org.junit.internal.runners.model.ReflectiveCallable").resolve();
        TypeDescription parentRunner = TypePool.Default.ofSystemLoader().describe("org.junit.runners.ParentRunner").resolve();
        TypeDescription blockJUnit4ClassRunner = TypePool.Default.ofSystemLoader().describe("org.junit.runners.BlockJUnit4ClassRunner").resolve();
        
        return new AgentBuilder.Default()
                .type(isSubTypeOf(reflectiveCallable))
                .transform((builder, type, classLoader, module) -> 
                        builder.method(named("runReflectiveCall")).intercept(MethodDelegation.to(RunReflectiveCall.class))
                               .implement(Hooked.class))
                .type(is(parentRunner))
                .transform((builder, type, classLoader, module) -> 
                        builder.method(named("createTestClass")).intercept(MethodDelegation.to(CreateTestClass.class))
                               .method(named("run")).intercept(MethodDelegation.to(Run.class))
                               .implement(Hooked.class))
                .type(isSubTypeOf(blockJUnit4ClassRunner))
                .transform((builder, type, classLoader, module) -> 
                        builder.method(named("createTest")).intercept(MethodDelegation.to(CreateTest.class))
                               .method(named("runChild")).intercept(MethodDelegation.to(RunChild.class))
                               .implement(Hooked.class))
                .installOn(instrumentation);
    }
    
    /**
     * Create a {@link Thread} object that encapsulated the specified shutdown listener.
     * 
     * @param listener shutdown listener object
     * @return shutdown listener thread object
     */
    static Thread getShutdownHook(final ShutdownListener listener) {
        return new Thread() {
            @Override
            public void run() {
                listener.onShutdown();
            }
        };
    }
    
    /**
     * Get the configuration object for JUnit Foundation.
     * 
     * @return JUnit Foundation configuration object
     */
    static synchronized JUnitConfig getConfig() {
        if (config == null) {
            config = JUnitConfig.getConfig();
        }
        return config;
    }
    
    /**
     * This class declares the interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
     */
    @SuppressWarnings("squid:S1118")
    public static class Run {
        private static final ThreadLocal<Stack<Object>> runnerStack;
        private static final ServiceLoader<RunListener> runListenerLoader;
        private static final ServiceLoader<RunnerWatcher> runnerWatcherLoader;
        private static final Set<RunNotifier> NOTIFIERS = new CopyOnWriteArraySet<>();
        private static final Map<Object, Object> CHILD_TO_PARENT = new ConcurrentHashMap<>();
        
        static {
            runnerStack = new ThreadLocal<Stack<Object>>() {
                @Override
                protected Stack<Object> initialValue() {
                    return new Stack<>();
                }
            };
            
            runListenerLoader = ServiceLoader.load(RunListener.class);
            runnerWatcherLoader = ServiceLoader.load(RunnerWatcher.class);
        }
        
        /**
         * Interceptor for the {@link org.junit.runners.ParentRunner#run run} method.
         * 
         * @param runner underlying test runner
         * @param proxy callable proxy for the intercepted method
         * @param notifier run notifier through which events are published
         * @throws Exception {@code anything} (exception thrown by the intercepted method)
         */
        public static void intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
                        @Argument(0) final RunNotifier notifier) throws Exception {
            
            List<?> children = invoke(runner, "getChildren");
            for (Object child : children) {
                CHILD_TO_PARENT.put(child, runner);
            }
            
            if (NOTIFIERS.add(notifier)) {
                Description description = invoke(runner, "getDescription");
                synchronized(runListenerLoader) {
                    for (RunListener listener : runListenerLoader) {
                        notifier.addListener(listener);
                        listener.testRunStarted(description);
                    }
                }
            }
            
            synchronized(runnerWatcherLoader) {
                for (RunnerWatcher watcher : runnerWatcherLoader) {
                    watcher.runStarted(runner);
                }
            }
            
            runnerStack.get().push(runner);
            callProxy(proxy);
            runnerStack.get().pop();
            
            synchronized(runnerWatcherLoader) {
                for (RunnerWatcher watcher : runnerWatcherLoader) {
                    watcher.runFinished(runner);
                }
            }
        }
        
        /**
         * Get the parent runner that owns specified child runner or framework method.
         * 
         * @param child {@code ParentRunner} or {@code FrameworkMethod} object
         * @return {@code ParentRunner} object that owns the specified child ({@code null} for root objects)
         */
        static Object getParentOf(Object child) {
            return CHILD_TO_PARENT.get(child);
        }
        
        /**
         * Get the runner that owns the active thread context.
         * 
         * @return active {@code ParentRunner} object
         * @throws EmptyStackException if called outside the scope of an active runner
         */
        static Object getThreadRunner() {
            return runnerStack.get().peek();
        }
    }
    
    /**
     * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
     * createTest} method.
     */
    @SuppressWarnings("squid:S1118")
    public static class CreateTest {
        
        private static final ServiceLoader<TestObjectWatcher> objectWatcherLoader;
        private static final Map<Object, Object> TARGET_TO_RUNNER = new ConcurrentHashMap<>();
        private static final Map<Object, Object> RUNNER_TO_TARGET = new ConcurrentHashMap<>();
        
        static {
            objectWatcherLoader = ServiceLoader.load(TestObjectWatcher.class);
        }
        
        /**
         * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest createTest} method.
         * 
         * @param runner target {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} object
         * @param proxy callable proxy for the intercepted method
         * @return {@code anything} - JUnit test class instance
         * @throws Exception {@code anything} (exception thrown by the intercepted method)
         */
        @RuntimeType
        public static Object intercept(@This final Object runner,
                        @SuperCall final Callable<?> proxy) throws Exception {
            Object testObj = callProxy(proxy);
            TARGET_TO_RUNNER.put(testObj, runner);
            RUNNER_TO_TARGET.put(runner, testObj);
            applyTimeout(testObj);
            
            synchronized(objectWatcherLoader) {
                for (TestObjectWatcher watcher : objectWatcherLoader) {
                    watcher.testObjectCreated(testObj, runner);
                }
            }
            
            return testObj;
        }
        
        /**
         * Get the class runner associated with the specified instance.
         * 
         * @param target instance of JUnit test class
         * @return {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} for specified instance
         */
        static Object getRunnerForTarget(Object target) {
            return TARGET_TO_RUNNER.get(target);
        }
        
        /**
         * Get the JUnit test class instance for the specified class runner.
         * 
         * @param runner JUnit class runner
         * @return JUnit test class instance for specified runner
         */
        static Object getTargetForRunner(Object runner) {
            return RUNNER_TO_TARGET.get(runner);
        }
    }
    
    /**
     * Get the class runner associated with the specified instance.
     * 
     * @param target instance of JUnit test class
     * @return {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} for specified instance
     */
    public static Object getRunnerForTarget(Object target) {
        return CreateTest.getRunnerForTarget(target);
    }
    
    /**
     * Get the JUnit test class instance for the specified class runner.
     * 
     * @param runner JUnit class runner
     * @return JUnit test class instance for specified runner
     */
    public static Object getTargetForRunner(Object runner) {
        return CreateTest.getTargetForRunner(runner);
    }
    
    /**
     * Get the test class associated with the specified framework method.
     * 
     * @param method {@code FrameworkMethod} object
     * @return {@link TestClass} object associated with the specified framework method
     */
    public static TestClass getTestClassWith(Object method) {
        return CreateTestClass.getTestClassWith(method);
    }
    
    /**
     * Get the parent runner that owns specified child runner or framework method.
     * 
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @return {@code ParentRunner} object that owns the specified child ({@code null} for root objects)
     */
    public static Object getParentOf(Object child) {
        return Run.getParentOf(child);
    }
    
    /**
     * Get the runner that owns the active thread context.
     * 
     * @return active {@code ParentRunner} object
     * @throws EmptyStackException if called outside the scope of an active runner
     */
    public static Object getThreadRunner() {
        return Run.getThreadRunner();
    }
    
    /**
     * Get the test class object associated with the specified parent runner.
     * 
     * @param runner target {@link org.junit.runners.ParentRunner ParentRunner} object
     * @return {@link TestClass} associated with specified runner
     */
    public static TestClass getTestClassOf(Object runner) {
        return invoke(runner, "getTestClass");
    }
    
    /**
     * Get the description of the indicated child object from the runner for the specified test class instance.
     * 
     * @param target test class instance
     * @param child child object
     * @return {@link Description} object for the indicated child
     */
    public static Description describeChild(Object target, Object child) {
        Object runner = getRunnerForTarget(target);
        return invoke(runner, "describeChild", child);
    }
    
    /**
     * If configured for default test timeout, apply this value to every test that doesn't already specify a longer
     * timeout interval.
     * 
     * @param testObj test class object
     */
    static void applyTimeout(Object testObj) {
        // if default test timeout is defined
        if (getConfig().containsKey(JUnitSettings.TEST_TIMEOUT.key())) {
            // get default test timeout
            long defaultTimeout = getConfig().getLong(JUnitSettings.TEST_TIMEOUT.key());
            // iterate over test object methods
            for (Method method : testObj.getClass().getDeclaredMethods()) {
                // get @Test annotation
                Test annotation = method.getDeclaredAnnotation(Test.class);
                // if annotation declared and current timeout is less than default
                if ((annotation != null) && (annotation.timeout() < defaultTimeout)) {
                    // set test timeout interval
                    MutableTest.proxyFor(method).setTimeout(defaultTimeout);
                }
            }
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
    static String getSubclassName(Object testObj) {
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
    
    /**
     * Invoke the named method with the specified parameters on the specified target object.
     * 
     * @param <T> method return type
     * @param target target object
     * @param methodName name of the desired method
     * @param parameters parameters for the method invocation
     * @return result of method invocation
     */
    @SuppressWarnings("unchecked")
    static <T> T invoke(Object target, String methodName, Object... parameters) {
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getClass();
        }
        
        Throwable thrown = null;
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return (T) method.invoke(target, parameters);
            } catch (NoSuchMethodException e) {
                thrown = e;
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                thrown = e;
                break;
            }
        }
        
        throw UncheckedThrow.throwUnchecked(thrown);
    }
    
    /**
     * Get the specified field of the supplied object.
     * 
     * @param target target object
     * @param name field name
     * @return {@link Field} object for the requested field
     * @throws NoSuchFieldException if a field with the specified name is not found
     * @throws SecurityException if the request is denied
     */
    static Field getDeclaredField(Object target, String name) throws NoSuchFieldException, SecurityException {
        Throwable thrown = null;
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                thrown = e;
            } catch (SecurityException e) {
                thrown = e;
                break;
            }
        }
        
        throw UncheckedThrow.throwUnchecked(thrown);
    }

    /**
     * Get the value of the specified field from the supplied object.
     * 
     * @param <T> field value type
     * @param target target object
     * @param name field name
     * @return {@code anything} - the value of the specified field in the supplied object
     * @throws IllegalAccessException if the {@code Field} object is enforcing access control for an inaccessible field
     * @throws NoSuchFieldException if a field with the specified name is not found
     * @throws SecurityException if the request is denied
     */
    @SuppressWarnings("unchecked")
    static <T> T getFieldValue(Object target, String name) throws IllegalAccessException, NoSuchFieldException, SecurityException {
        Field field = getDeclaredField(target, name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    /**
     * Set the value of the specified field of the supplied object.
     * 
     * @param target target object
     * @param name field name
     * @param value value to set in the specified field of the supplied object
     * @throws IllegalAccessException if the {@code Field} object is enforcing access control for an inaccessible field
     * @throws NoSuchFieldException if a field with the specified name is not found
     * @throws SecurityException if the request is denied
     */
    static void setFieldValue(Object target, String name, Object value) throws IllegalAccessException, NoSuchFieldException, SecurityException {
        Field field = getDeclaredField(target, name);
        field.setAccessible(true);
        field.set(target, value);
    }
    
    /**
     * Invoke an intercepted method through its callable proxy.
     * <p>
     * <b>NOTE</b>: If the invoked method throws an exception, this method re-throws the original exception.
     * 
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - value returned by the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    static Object callProxy(final Callable<?> proxy) throws Exception {
        try {
            return proxy.call();
        } catch (InvocationTargetException e) {
            throw UncheckedThrow.throwUnchecked(e.getCause());
        }
    }
}
