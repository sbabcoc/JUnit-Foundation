package com.nordstrom.automation.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;
import com.nordstrom.common.base.UncheckedThrow;

class TimeoutUtils {
    
    private static final Field fieldsForAnnotations;

    static {
        try {
            fieldsForAnnotations = TestClass.class.getDeclaredField("fieldsForAnnotations");
            fieldsForAnnotations.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }
    
    /**
     * If configured for default test timeout, apply the timeout value to the framework method associated
     * with the underlying test runner, if it doesn't already specify a longer timeout interval.
     * 
     * @param runner underlying test runner
     * @param target test class instance
     */
    @SuppressWarnings("unchecked")
    static void applyTestTimeout(final Object runner, final Object target) {
        long uberTimeout = -1;
        // if default timeout rule interval is defined
        if (LifecycleHooks.getConfig().containsKey(JUnitSettings.TIMEOUT_RULE.key())) {
            // get default timeout rule interval
            uberTimeout = LifecycleHooks.getConfig().getLong(JUnitSettings.TIMEOUT_RULE.key());
        }

        long ruleTimeout = -1;
        // get the test class of the specified runner
        TestClass testClass = LifecycleHooks.getTestClassOf(runner);
        Map<Class<? extends Annotation>, List<FrameworkField>> fieldsMap;
        
        try {
            // get the map that associates class fields with declared annotations
            fieldsMap = (Map<Class<? extends Annotation>, List<FrameworkField>>) fieldsForAnnotations.get(testClass);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
        
        ListIterator<FrameworkField> iterator = null;
        // get the list of @Rule fields declared in this test class
        List<FrameworkField> ruleFields = (List<FrameworkField>) fieldsMap.get(Rule.class);
        
        // if @Rule fields exist
        if (ruleFields != null) {
            // get rule field iterator
            iterator = ruleFields.listIterator();
            // iterate over rule fields
            while (iterator.hasNext()) {
                // get current rule field
                FrameworkField ruleField = iterator.next();
                // if this if a Timeout rule
                if (Timeout.class.isAssignableFrom(ruleField.getType())) {
                    try {
                        // extract Timeout rule interval
                        ruleTimeout = LifecycleHooks.invoke(ruleField.get(target), "getTimeout", TimeUnit.MILLISECONDS);
                        // exit iteration
                        break;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        UncheckedThrow.throwUnchecked(e);
                    }
                }
            }
        }
        
        // get "atomic" test of the specified runner
        AtomicTest<FrameworkMethod> atomicTest = LifecycleHooks.getAtomicTestOf(runner);
        // get identity method for this "atomic" test
        FrameworkMethod identity = atomicTest.getIdentity();
        
        // if timeout disabled by local or global rule
        if ((uberTimeout == 0) || (ruleTimeout == 0)) {
            // disable timeout of @Test annotation
            MutableTest.proxyFor(identity.getMethod()).setTimeout(0);
        } else {
            long testTimeout = -1;
            // if default test timeout is defined
            if (LifecycleHooks.getConfig().containsKey(JUnitSettings.TEST_TIMEOUT.key())) {
                // get default test timeout
                testTimeout = LifecycleHooks.getConfig().getLong(JUnitSettings.TEST_TIMEOUT.key());
            }
            
            // get @Test annotation
            Test annotation = identity.getAnnotation(Test.class);
            // extract value of timeout parameter
            long metaTimeout = annotation.timeout();
            
            // if default timeout is longer
            if (testTimeout > metaTimeout) {
                // override value of timeout parameter
                MutableTest.proxyFor(identity.getMethod()).setTimeout(testTimeout);
            }
        }
    }

    /**
     * If configured for rule-based global timeout, apply timeout to specified test rules list.
     * 
     * @param runner underlying test runner
     * @param testRules test rules of associated test class
     */
    static void applyRuleTimeout(final Object runner, final List<TestRule> testRules) {
        // get atomic test object for target class runner
        AtomicTest<FrameworkMethod> atomicTest = LifecycleHooks.getAtomicTestOf(runner);
        // get "identity" method of atomic test
        FrameworkMethod identity = atomicTest.getIdentity();
        // get "identity" method Test annotation
        Test annotation = identity.getAnnotation(Test.class);
        // get test method timeout interval
        long metaTimeout = annotation.timeout();
        
        long uberTimeout = -1;
        // if default timeout rule interval is defined
        if (LifecycleHooks.getConfig().containsKey(JUnitSettings.TIMEOUT_RULE.key())) {
            // get default timeout rule interval
            uberTimeout = LifecycleHooks.getConfig().getLong(JUnitSettings.TIMEOUT_RULE.key());
        }
        
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
        
        boolean disableTimeout = ((uberTimeout == 0) || (ruleTimeout == 0));
        long timeout = (disableTimeout) ? 0 : Collections.max(Arrays.asList(metaTimeout, uberTimeout, ruleTimeout));
        
        // if Timeout found
        if (ruleIndex != -1) {
            // if rule interval differs
            if (ruleTimeout != timeout) {
                // replace existing rule with Timeout of interval
                testRules.set(ruleIndex, Timeout.millis(timeout));
            }
        // otherwise, if Timeout specified
        } else if (uberTimeout != -1) {
            // add Timeout of longest interval
            testRules.add(Timeout.millis(timeout));
        }
    }
}
