package com.nordstrom.automation.junit;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runners.model.FrameworkMethod;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class GetTestRules {
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#getTestRules(Object)}  getTestRules} method.
     *
     * @param runner target {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} object
     * @param proxy  callable proxy for the intercepted method
     * @param target the test case instance
     * @return {@code anything} - JUnit test class instance
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static List<TestRule> intercept(@This final Object runner, @SuperCall final Callable<?> proxy, @Argument(0) final Object target) throws Exception {
        // get atomic test object for target class runner
        AtomicTest<FrameworkMethod> atomicTest = LifecycleHooks.getAtomicTestOf(runner);
        // get "identity" method of atomic test
        FrameworkMethod identity = atomicTest.getIdentity();
        // get "identity" method Test annotation
        Test annotation = identity.getAnnotation(Test.class);
        // get test method timeout interval
        long testTimeout = annotation.timeout();
        // initialize longest interval
        long longestTimeout = testTimeout;
        
        long defaultTimeout = -1;
        // if default timeout rule interval is defined
        if (LifecycleHooks.getConfig().containsKey(JUnitSettings.TIMEOUT_RULE.key())) {
            // get default timeout rule interval
            defaultTimeout = LifecycleHooks.getConfig().getLong(JUnitSettings.TIMEOUT_RULE.key());
            
            // if default rule timeout is longest
            if (defaultTimeout > longestTimeout) {
                // update longest interval
                longestTimeout = defaultTimeout;
            }
        }
        
        @SuppressWarnings("unchecked")
        // get list of test rules for target class runner
        List<TestRule> testRules = (List<TestRule>) LifecycleHooks.callProxy(proxy);
        
        int ruleIndex = -1;
        long ruleTimeout = -1;
        
        // iterate over active test rules collection
        for (int i = 0; i < testRules.size(); i++) {
            // get current test rule
            TestRule testRule = testRules.get(i);
            // if this is a Timeout rule
            if (testRule instanceof Timeout) {
                // save index
                ruleIndex = i;
                // extract Timeout rule interval
                ruleTimeout = LifecycleHooks.invoke(testRule, "getTimeout", TimeUnit.MILLISECONDS);
                break;
            }
        }
        
        // if Timeout found
        if (ruleIndex != -1) {
            // if longest interval exceeds rule
            if (longestTimeout > ruleTimeout) {
                // replace existing rule with Timeout of longest interval
                testRules.set(ruleIndex, Timeout.millis(longestTimeout));
            } else {
                // update longest interval
                longestTimeout = ruleTimeout;
            }
        // otherwise, if Timeout specified
        } else if (defaultTimeout != -1) {
            // update rule index
            ruleIndex = testRules.size();
            // add Timeout of longest interval
            testRules.add(Timeout.millis(longestTimeout));
        }
        
        // if Timeout rule interval was adopted from test timeout
        if ((ruleIndex != -1) && (longestTimeout == testTimeout)) {
            // disable test method timeout
            MutableTest.proxyFor(identity.getMethod()).setTimeout(0L);
        }
        
        return testRules;
    }
}
