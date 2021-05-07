package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;
import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

/**
 * This class provided the utility methods used by the <b>JUnit Foundation</b> automatic retry feature.
 */
public class RetryHandler {

    private static final Map<String, Boolean> METHOD_TO_RETRY = new ConcurrentHashMap<>();
    private static final ServiceLoader<JUnitRetryAnalyzer> retryAnalyzerLoader;
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandler.class);
    
    static {
        retryAnalyzerLoader = ServiceLoader.load(JUnitRetryAnalyzer.class);
    }
    
    private RetryHandler() {
        throw new AssertionError("RetryHandler is a static utility class that cannot be instantiated");
    }
    
    /**
     * Run the specified child method, retrying on failure.
     * 
     * @param runner underlying test runner
     * @param method test method to be run
     * @param statement JUnit statement object (the atomic test)
     * @param notifier run notifier through which events are published
     * @param maxRetry maximum number of retry attempts
     * @return exception thrown by child method; {@code null} on normal completion
     */
    public static Throwable runChildWithRetry(final Object runner, final FrameworkMethod method,
            final Statement statement, final RunNotifier notifier, final int maxRetry) {
        
        boolean doRetry = true;
        Throwable thrown = null;
        AtomicTest atomicTest = null;
        Statement iteration = statement; 
        Description description = invoke(runner, "describeChild", method);
        AtomicInteger count = new AtomicInteger(maxRetry);
        
        do {
            EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            atomicTest = EachTestNotifierInit.getAtomicTestOf(description);
            
            if (atomicTest.isTheory()) {
                iteration = MethodBlock.getStatementOf(runner);
            }
            
            eachNotifier.fireTestStarted();
            try {
                iteration.evaluate();
                doRetry = false;
            } catch (AssumptionViolatedException e) {
                doRetry = doRetry(method, e, count);
                if (doRetry) {
                    description = RetriedTest.proxyFor(description, e);
                    eachNotifier.fireTestIgnored();
                } else {
                    thrown = e;
                    eachNotifier.addFailedAssumption(e);
                }
            } catch (Throwable e) {
                doRetry = doRetry(method, e, count);
                if (doRetry) {
                    description = RetriedTest.proxyFor(description, e);
                    eachNotifier.fireTestIgnored();
                } else {
                    thrown = e;
                    eachNotifier.addFailure(e);
                }
            } finally {
                // fire 'test finished' event
                eachNotifier.fireTestFinished();
            }
            
            // if finished, exit
            if (!doRetry) break;
            
            try {
                // retain method to retry for JUnitParams
                METHOD_TO_RETRY.put(toMapKey(method), true);
                // create new "atomic test" for next iteration
                iteration = invoke(runner, "methodBlock", method);
            } finally {
                // release method to retry
                METHOD_TO_RETRY.remove(toMapKey(method));
            }
        } while (true);
        
        return thrown;
    }
    
    /**
     * Determine if the indicated failure should be retried.
     * 
     * @param method failed test method
     * @param thrown exception for this failed test
     * @param retryCounter retry counter (remaining attempts)
     * @return {@code true} if failed test should be retried; otherwise {@code false}
     */
    static boolean doRetry(FrameworkMethod method, Throwable thrown, AtomicInteger retryCounter) {
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
     * @param runner JUnit test runner
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
            maxRetry = JUnitConfig.getConfig().getInteger(JUnitSettings.MAX_RETRY.key(), Integer.valueOf(0));
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
        synchronized(retryAnalyzerLoader) {
            for (JUnitRetryAnalyzer analyzer : retryAnalyzerLoader) {
                if (analyzer.retry(method, thrown)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Determine if the specified method is being retried.
     * 
     * @param method JUnit framework method
     * @return {@code true} if method is being retried; otherwise {@code false}
     */
    static boolean doRetryFor(final FrameworkMethod method) {
        Boolean doRetry = METHOD_TO_RETRY.get(toMapKey(method));
        return (doRetry != null) ? doRetry.booleanValue() : false;
    }
    
}
