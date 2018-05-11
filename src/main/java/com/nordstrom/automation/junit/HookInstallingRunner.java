package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;
import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.file.PathUtils.ReportsDirectory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;

/**
 * This JUnit test runner implements four significant features:
 * <ol>
 *     <li>Invocation hooks for test and configuration methods</li>
 *     <li>Test method timeout management</li>
 *     <li>Automatic retry of failed tests</li>
 *     <li>Dynamic run listener attachment</li>
 * </ol>
 * 
 * <b>Invocation hooks for test and configuration methods</b>
 * <p>
 * This runner uses bytecode enhancement to install hooks on test and configuration methods to enable method
 * pre-processing and post-processing. This closely resembles the {@code IInvokedMethodListener} feature of
 * TestNG. Classes that implement the {@link MethodWatcher} interface are attached to these hooks via the
 * {@link MethodWatchers} annotation, which is applied to applicable test classes.
 * <p>
 * <b>Test method timeout management</b>
 * <p>
 * Test method timeout management is activated by setting the {@link JUnitSettings#TEST_TIMEOUT TEST_TIMEOUT}
 * configuration option to the desired default test timeout interval in milliseconds. This timeout specification
 * is applied to every test method that doesn't explicitly specify a longer interval.
 * <p>
 * <b>Automatic retry of failed tests</b>
 * <p>
 * Automatic retry is activated by setting the {@link JUnitSettings#MAX_RETRY MAX_RETRY} configuration option to the
 * maximum retry attempts that will be made if a test method fails. The automatic retry feature can be disabled on a
 * per-method or per-class basis via the {@code @NoRetry} annotation.
 * <p>
 * <b><i>META-INF/services/com.nordstrom.automation.junit.JUnitRetryAnalyzer</i></b> is the service loader retry
 * analyzer configuration file. By default, this file is absent. To add managed analyzers, create this file and add
 * the fully-qualified names of their classes, one line per item.
 * <p>
 * <b>Dynamic run listener attachment</b>
 * <p>
 * JUnit run notifiers send notifications at specific points in the test execution lifecycle. Run listeners that
 * are attached to notifiers receive these events, enabling them to perform setup, cleanup and monitoring actions
 * that correspond to them. In standard JUnit projects, run listeners must be attached programmatically to the
 * core test runner (e.g. - <b>JUnitCore</b>). For Maven projects, listeners can be specified in the configuration
 * of the <b>Surefire</b> plug-in. Each of these methods imposes specific structural requirements on the client
 * project that may be either undesirable or infeasible.
 * <p>
 * <b>JUnit Foundation</b> enables you to declare run listeners in a service loader configuration file.<br>
 * <b><i>META-INF/services/org.junit.runner.notification.RunListener</i></b> is the service loader run listener
 * configuration file. By default, this file is absent. To add managed listeners, create this file and add the
 * fully-qualified names of their classes, one line per item.
 */
public final class HookInstallingRunner extends BlockJUnit4ClassRunner {
    
    private static Map<Class<?>, Class<?>> proxyMap = new HashMap<>();
    
    private final JUnitConfig config;
    private final ServiceLoader<RunListener> runListenerLoader;
    private final ServiceLoader<JUnitRetryAnalyzer> retryAnalyzerLoader;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * Constructor: Instantiate and initialize a runner for the specified test class. This includes instantiation of
     * service loaders for client-specified run listeners and retry analyzers.
     * 
     * @param klass test class for which this runner is being created
     * @throws InitializationError if the test class is malformed
     */
    public HookInstallingRunner(Class<?> klass) throws InitializationError {
        super(klass);
        config = JUnitConfig.getConfig();
        runListenerLoader = ServiceLoader.load(RunListener.class);
        retryAnalyzerLoader = ServiceLoader.load(JUnitRetryAnalyzer.class);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createTest() throws Exception {
        Object testObj = installHooks(super.createTest());
        applyTimeout(testObj);
        return testObj;
    }
    
    /**
     * If configured for default test timeout, apply this value to every test that doesn't already specify a longer
     * timeout interval.
     * 
     * @param testObj test class object
     */
    private void applyTimeout(Object testObj) {
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
     * {@inheritDoc}
     */
    @Override
    public void run(final RunNotifier notifier) {
        runListenerLoader.forEach(notifier::addListener);
        super.run(notifier);
        runListenerLoader.forEach(notifier::removeListener);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        int count = getMaxRetry(method);
        
        if (count > 0) {
            runChildWithRetry(method, notifier, count);
        } else {
            super.runChild(method, notifier);
        }
    }
    
    /**
     * Run the specified method, retrying on failure.
     * 
     * @param method test method to be run
     * @param notifier run notifier through which events are published
     * @param maxRetry maximum number of retry attempts
     */
    protected void runChildWithRetry(final FrameworkMethod method, RunNotifier notifier, int maxRetry) {
        boolean doRetry = false;
        Statement statement = methodBlock(method);
        Description description = describeChild(method);
        AtomicInteger count = new AtomicInteger(maxRetry);
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        
        do {
            eachNotifier.fireTestStarted();
            try {
                statement.evaluate();
                doRetry = false;
            } catch (AssumptionViolatedException e) {
                doRetry = doRetry(method, e, count);
                if (doRetry) {
                    eachNotifier.fireTestIgnored();
                } else {
                    eachNotifier.addFailedAssumption(e);
                }
            } catch (Throwable e) {
                doRetry = doRetry(method, e, count);
                if (doRetry) {
                    eachNotifier.fireTestIgnored();
                } else {
                    eachNotifier.addFailure(e);
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
    private boolean doRetry(FrameworkMethod method, Throwable thrown, AtomicInteger retryCounter) {
        boolean doRetry = false;
        if ((retryCounter.decrementAndGet() > -1) && isRetriable(method, thrown)) {
            logger.warn("### RETRY ### {}", method);
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
    private int getMaxRetry(final FrameworkMethod method) {
        int maxRetry = 0;
        
        // determine if retry is disabled for this method
        NoRetry noRetryOnMethod = method.getAnnotation(NoRetry.class);
        // determine if retry is disabled for the class that declares this method
        NoRetry noRetryOnClass = method.getDeclaringClass().getAnnotation(NoRetry.class);
        
        if (!isIgnored(method) && (noRetryOnMethod == null) && (noRetryOnClass == null)) {
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
    protected boolean isRetriable(final FrameworkMethod method, final Throwable thrown) {
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
