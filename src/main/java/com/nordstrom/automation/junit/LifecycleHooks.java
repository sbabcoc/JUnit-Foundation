package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestClass;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.file.PathUtils.ReportsDirectory;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.description.method.MethodDescription.SignatureToken;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;

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
        final TypeDescription runReflectiveCall = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.RunReflectiveCall").resolve();
        final TypeDescription finished = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.Finished").resolve();
        final TypeDescription createTest = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.CreateTest").resolve();
        final TypeDescription runChild = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.RunChild").resolve();
        final TypeDescription run = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.Run").resolve();
        final TypeDescription getTestRules = TypePool.Default.ofSystemLoader().describe("com.nordstrom.automation.junit.GetTestRules").resolve();
        
        final TypeDescription runNotifier = TypePool.Default.ofSystemLoader().describe("org.junit.runner.notification.RunNotifier").resolve();
        final SignatureToken runToken = new SignatureToken("run", TypeDescription.VOID, Arrays.asList(runNotifier));
        
        return new AgentBuilder.Default()
                .type(hasSuperType(named("org.junit.internal.runners.model.ReflectiveCallable")))
                .transform(new Transformer() {
                    @Override
                    public Builder<?> transform(Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("runReflectiveCall")).intercept(MethodDelegation.to(runReflectiveCall))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runners.model.RunnerScheduler")))
                .transform(new Transformer() {
                    @Override
                    public Builder<?> transform(Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("finished")).intercept(MethodDelegation.to(finished))
                                      .implement(Hooked.class);
                    }
                })
                .type(hasSuperType(named("org.junit.runners.ParentRunner")))
                .transform(new Transformer() {
                    @Override
                    public Builder<?> transform(Builder<?> builder, TypeDescription type,
                                    ClassLoader classloader, JavaModule module) {
                        return builder.method(named("createTest")).intercept(MethodDelegation.to(createTest))
                                      .method(named("runChild")).intercept(MethodDelegation.to(runChild))
                                      .method(hasSignature(runToken)).intercept(MethodDelegation.to(run))
                                      .method(named("getTestRules")).intercept(MethodDelegation.to(getTestRules))
                                      .implement(Hooked.class);
                    }
                })
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
     * Get the parent runner that owns specified child runner or framework method.
     * 
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @return {@code ParentRunner} object that owns the specified child ({@code null} for root
     *         objects)
     */
    public static Object getParentOf(Object child) {
        return Run.getParentOf(child);
    }

    /**
     * Get the runner that owns the active thread context.
     * 
     * @return active {@code ParentRunner} object (may be ({@code null})
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
     * Get the atomic test object for the specified class runner.
     * 
     * @param runner JUnit class runner
     * @return {@link AtomicTest} object (may be {@code null})
     */
    public static <T> AtomicTest<T> getAtomicTestOf(Object runner) {
        return RunAnnouncer.getAtomicTestOf(runner);
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
        try {
            return (T) MethodUtils.invokeMethod(target, true, methodName, parameters);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
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
    static Field getDeclaredField(Object target, String name) throws NoSuchFieldException {
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
    public static <T> T getFieldValue(Object target, String name) throws IllegalAccessException, NoSuchFieldException, SecurityException {
        Field field = getDeclaredField(target, name);
        field.setAccessible(true);
        return (T) field.get(target);
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
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param <T> watcher type
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    public static <T extends JUnitWatcher> Optional<T> getAttachedWatcher(Class<T> watcherType) {
        Optional<T> watcher = CreateTest.getAttachedWatcher(watcherType);
        if (watcher.isPresent()) return watcher;
        
        watcher = Run.getAttachedWatcher(watcherType);
        if (watcher.isPresent()) return watcher;
        
        watcher = RunAnnouncer.getAttachedWatcher(watcherType);
        if (watcher.isPresent()) return watcher;
        
        return RunReflectiveCall.getAttachedWatcher(watcherType);
    }
    
    /**
     * Get reference to an instance of the specified listener type.
     * 
     * @param <T> listener type
     * @param listenerType listener type
     * @return optional listener instance
     */
    public static <T extends RunListener> Optional<T> getAttachedListener(Class<T> listenerType) {
        return Run.getAttachedListener(listenerType);
    }
    
    /**
     * If the specified key is not already associated with a value (or is mapped to {@code null}), attempts
     * to compute its value using the given mapping function and enters it into this map unless {@code null}.
     * 
     * @param <K> data type of map keys
     * @param <T> data type of map values
     * @param map concurrent map to be manipulated
     * @param key key with which the specified value is to be associated
     * @param fun the function to compute a value
     * @return the current (existing or computed) value associated with the specified key;
     *         {@code null} if the computed value is {@code null}
     */
    static <K, T> T computeIfAbsent(ConcurrentMap<K, T> map, K key, Function<K, T> fun) {
        T val = map.get(key);
        if (val == null) {
            T obj = fun.apply(key);
            val = (val = map.putIfAbsent(key, obj)) == null ? obj : val;
        }
        return val;
    }
}
