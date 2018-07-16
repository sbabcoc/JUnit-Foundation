package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;
import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.file.PathUtils.ReportsDirectory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.pool.TypePool;

/**
 * This class implements the hooks and utility methods that activate the core functionality of <b>JUnit Foundation</b>.
 * <p>
 * To activate core features, add the {@link HookInstallingListener} to your project configuration:
 * 
 * <pre><code> &lt;dependencies&gt;
 * [...]
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;com.nordstrom.tools&lt;/groupId&gt;
 *     &lt;artifactId&gt;junit-foundation&lt;/artifactId&gt;
 *     &lt;version&gt;3.2.2&lt;/version&gt;
 *     &lt;scope&gt;test&lt;/scope&gt;
 *   &lt;/dependency&gt;
 * [...]
 * &lt;/dependencies&gt;
 * [...]
 * &lt;plugins&gt;
 * [...]
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
 *     &lt;artifactId&gt;maven-surefire-plugin&lt;/artifactId&gt;
 *     &lt;version&gt;2.22.0&lt;/version&gt;
 *     &lt;configuration&gt;
 *       &lt;properties&gt;
 *         &lt;property&gt;
 *           &lt;name&gt;listener&lt;/name&gt;
 *           &lt;value&gt;com.nordstrom.automation.junit.HookInstallingListener&lt;/value&gt;
 *         &lt;/property&gt;
 *       &lt;/properties&gt;
 *     &lt;/configuration&gt;
 *   &lt;/plugin&gt;
 * [...]
 * &lt;/plugins&gt;</code></pre>
 * 
 * {@link HookInstallingListener} loads this class, whose static initializer performs the operations needed to activate
 * the core functionality of <b>JUnit Foundation</b>.
 */
public class LifecycleHooks {

    private static Map<Class<?>, Class<?>> proxyMap = new HashMap<>();
    
    private static final JUnitConfig config;
    private static final ServiceLoader<JUnitRetryAnalyzer> retryAnalyzerLoader;
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleHooks.class);
    
    private LifecycleHooks() {
        throw new AssertionError("LifecycleHooks is a static utility class that cannot be instantiated");
    }
    
    /**
     * This static initializer installs a shutdown hook for each specified listener. It also rebases the ParentRunner
     * and BlockJUnit4ClassRunner classes to enable the core functionality of JUnit Foundation.
     */
    static {
        config = JUnitConfig.getConfig();
        retryAnalyzerLoader = ServiceLoader.load(JUnitRetryAnalyzer.class);
        for (ShutdownListener listener : ServiceLoader.load(ShutdownListener.class)) {
            Runtime.getRuntime().addShutdownHook(getShutdownHook(listener));
        }
            
        TypeDescription type = 
                        TypePool.Default.ofClassPath().describe("org.junit.runners.ParentRunner").resolve();
        
        new ByteBuddy()
                .rebase(type, ClassFileLocator.ForClassLoader.ofClassPath())
                .method(named("createTestClass")).intercept(MethodDelegation.to(CreateTestClass.class))
                .implement(Hooked.class)
                .make()
                .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION);
        
        type = TypePool.Default.ofClassPath().describe("org.junit.runners.BlockJUnit4ClassRunner").resolve();
                
        new ByteBuddy()
                .rebase(type, ClassFileLocator.ForClassLoader.ofClassPath())
                .method(named("createTest")).intercept(MethodDelegation.to(CreateTest.class))
                .method(named("runChild")).intercept(MethodDelegation.to(RunChild.class))
                .implement(Hooked.class)
                .make()
                .load(ClassLoader.getSystemClassLoader(), ClassLoadingStrategy.Default.INJECTION);
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
    
    @SuppressWarnings("squid:S1118")
    public static class CreateTestClass {
        private static final Map<TestClass, Object> CLASS_TO_RUNNER = new ConcurrentHashMap<>();
        
        public static TestClass intercept(@This Object runner, @SuperCall Callable<?> proxy) throws Exception {
            TestClass testClass = (TestClass) proxy.call();
            CLASS_TO_RUNNER.put(testClass, runner);
            return testClass;
        }
    }
    
    /**
     * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest createTest}
     * method.
     */
    @SuppressWarnings("squid:S1118")
    public static class CreateTest {
        
        private static final Map<Object, TestClass> INSTANCE_TO_CLASS = new ConcurrentHashMap<>();
        
        /**
         * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest createTest} method.
         * 
         * @param proxy callable proxy for the intercepted method
         * @return {@code anything}
         * @throws Exception if something goes wrong
         */
        public static Object intercept(@This Object runner, @SuperCall Callable<?> proxy) throws Exception {
            Object testObj = installHooks(proxy.call());
            INSTANCE_TO_CLASS.put(testObj, invoke(runner, "getTestClass"));
            applyTimeout(testObj);
            return testObj;
        }
    }
    
    /**
     * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild}
     * method.
     */
    @SuppressWarnings("squid:S1118")
    public static class RunChild {
    
        /**
         * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#runChild runChild} method.
         * 
         * @param runner underlying test runner
         * @param proxy callable proxy for the intercepted method
         * @param method test method to be run
         * @param notifier run notifier through which events are published
         * @throws Exception if something goes wrong
         */
        public static void intercept(@This Object runner, @SuperCall Callable<?> proxy, @Argument(0) final FrameworkMethod method, @Argument(1) RunNotifier notifier) throws Exception {
            int count = getMaxRetry(runner, method);
            
            if (count > 0) {
                runChildWithRetry(runner, method, notifier, count);
            } else {
                proxy.call();
            }
        }
    }
    
    /**
     * Get the test class object that wraps the specified instance.
     * 
     * @param instance instance object (either test class or Suite)
     * @return {@link TestClass} associated with specified instance object
     */
    public static TestClass getTestClassFor(Object instance) {
        TestClass testClass = CreateTest.INSTANCE_TO_CLASS.get(instance);
        if (testClass != null) {
            return testClass;
        }
        throw new IllegalStateException("No associated test class was found for specified instance");
    }
    
    /**
     * Get the runner that owns the specified test class object;
     * 
     * @param testClass {@link TestClass} object
     * @return {@link Runner} that owns the specified test class object
     */
    public static Object getRunnerFor(TestClass testClass) {
        Object runner = CreateTestClass.CLASS_TO_RUNNER.get(testClass);
        if (runner != null) {
            return runner;
        }
        throw new IllegalStateException("No associated runner was for for specified test class");
    }
    
    /**
     * If configured for default test timeout, apply this value to every test that doesn't already specify a longer
     * timeout interval.
     * 
     * @param testObj test class object
     */
    static void applyTimeout(Object testObj) {
        // if default test timeout is defined
        if (config.containsKey(JUnitSettings.TEST_TIMEOUT.key())) {
            // get default test timeout
            long defaultTimeout = config.getLong(JUnitSettings.TEST_TIMEOUT.key());
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
     * Run the specified method, retrying on failure.
     * 
     * @param runner underlying test runner
     * @param method test method to be run
     * @param notifier run notifier through which events are published
     * @param maxRetry maximum number of retry attempts
     */
    static void runChildWithRetry(Object runner, final FrameworkMethod method, RunNotifier notifier, int maxRetry) {
        boolean doRetry = false;
        Statement statement = invoke(runner, "methodBlock", method);
        Description description = invoke(runner, "describeChild", method);
        AtomicInteger count = new AtomicInteger(maxRetry);
        
        do {
            EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            
            eachNotifier.fireTestStarted();
            try {
                statement.evaluate();
                doRetry = false;
            } catch (AssumptionViolatedException thrown) {
                doRetry = doRetry(runner, method, thrown, count);
                if (doRetry) {
                    description = RetriedTest.proxyFor(description, thrown);
                    eachNotifier.fireTestIgnored();
                } else {
                    eachNotifier.addFailedAssumption(thrown);
                }
            } catch (Throwable thrown) {
                doRetry = doRetry(runner, method, thrown, count);
                if (doRetry) {
                    description = RetriedTest.proxyFor(description, thrown);
                    eachNotifier.fireTestIgnored();
                } else {
                    eachNotifier.addFailure(thrown);
                }
            } finally {
                eachNotifier.fireTestFinished();
            }
        } while (doRetry);
    }
    
    /**
     * Determine if the indicated failure should be retried.
     * 
     * @param method failed test method
     * @param thrown exception for this failed test
     * @param retryCounter retry counter (remaining attempts)
     * @return {@code true} if failed test should be retried; otherwise {@code false}
     */
    static boolean doRetry(Object runner, FrameworkMethod method, Throwable thrown, AtomicInteger retryCounter) {
        boolean doRetry = false;
        if ((retryCounter.decrementAndGet() > -1) && isRetriable(method, thrown)) {
            LOGGER.warn("### RETRY ### {}", method);
            doRetry = true;
        }
        return doRetry;
    }

    /**
     * Get the configured maximum retry count for failed tests ({@link JUnitSetting#MAX_RETRY MAX_RETRY}).
     * <p>
     * <b>NOTE</b>: If the specified method or the class that declares it are marked with the {@code @NoRetry}
     * annotation, this method returns zero (0).
     * 
     * @param method test method for which retry is being considered
     * @return maximum retry attempts that will be made if the specified method fails
     */
    static int getMaxRetry(Object runner, final FrameworkMethod method) {
        int maxRetry = 0;
        
        // determine if retry is disabled for this method
        NoRetry noRetryOnMethod = method.getAnnotation(NoRetry.class);
        // determine if retry is disabled for the class that declares this method
        NoRetry noRetryOnClass = method.getDeclaringClass().getAnnotation(NoRetry.class);
        
        // if method isn't ignored or excluded from retry attempts
        if (Boolean.FALSE.equals(invoke(runner, "isIgnored", method)) && (noRetryOnMethod == null) && (noRetryOnClass == null)) {
            // get configured maximum retry count
            maxRetry = config.getInteger(JUnitSettings.MAX_RETRY.key(), Integer.valueOf(0));
        }
        
        return maxRetry;
    }
    
    /**
     * Determine if the specified failed test should be retried.
     * 
     * @param method failed test method
     * @param thrown exception for this failed test
     * @return {@code true} if test should be retried; otherwise {@code false}
     */
    static boolean isRetriable(final FrameworkMethod method, final Throwable thrown) {
        for (JUnitRetryAnalyzer analyzer : retryAnalyzerLoader) {
            if (analyzer.retry(method, thrown)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Create an enhanced instance of the specified test class object.
     * 
     * @param testObj test class object to be enhanced
     * @return enhanced test class object
     */
    static synchronized Object installHooks(Object testObj) {
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
}
