package com.nordstrom.automation.junit;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.rules.TestRule;
import org.junit.runners.model.FrameworkMethod;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares an interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#getTestRules
 * getTestRules} method.
 */
public class GetTestRules {
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#getTestRules getTestRules} method.
     *
     * @param runner target {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} object
     * @param proxy callable proxy for the intercepted method
     * @param target test class instance
     * @return {@code anything} - JUnit test class instance
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static List<TestRule> intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
            @Argument(0) final Object target) throws Exception {
        @SuppressWarnings("unchecked")
        // get list of test rules for target class runner
        List<TestRule> testRules = (List<TestRule>) LifecycleHooks.callProxy(proxy);
        // get description for target
        FrameworkMethod method = CreateTest.getAtomicTestOf(target).getIdentity();
        // apply rule-based global timeout
        TimeoutUtils.applyRuleTimeout(runner, method, testRules);
        // return test rules
        return testRules;
    }
}
