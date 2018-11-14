package com.nordstrom.automation.junit;

import static com.nordstrom.automation.junit.LifecycleHooks.invoke;

import java.util.ServiceLoader;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandler.class);
    
    private RetryHandler() {
        throw new AssertionError("RetryHandler is a static utility class that cannot be instantiated");
    }
    
    /**
     * Run the specified method, retrying on failure.
     * 
     * @param runner JUnit test runner
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
                doRetry = doRetry(method, thrown, count);
                if (doRetry) {
                    description = RetriedTest.proxyFor(description, thrown);
                    RunReflectiveCall.fireTestIgnored(runner, method);
                    eachNotifier.fireTestIgnored();
                } else {
                    eachNotifier.addFailedAssumption(thrown);
                }
            } catch (Throwable thrown) {
                doRetry = doRetry(method, thrown, count);
                if (doRetry) {
                    description = RetriedTest.proxyFor(description, thrown);
                    RunReflectiveCall.fireTestIgnored(runner, method);
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
        for (JUnitRetryAnalyzer analyzer : ServiceLoader.load(JUnitRetryAnalyzer.class)) {
            if (analyzer.retry(method, thrown)) {
                return true;
            }
        }
        return false;
    }
    
}
