package com.nordstrom.automation.junit;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.junit.AssumptionViolatedException;
import org.junit.experimental.theories.PotentialAssignment.CouldNotGenerateValueException;
import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class MethodCompletesWithParameters {

    @RuntimeType
    public static Object intercept(@This final TheoryAnchor anchor, @SuperCall final Callable<?> proxy,
            @Argument(0) final FrameworkMethod method,
            @Argument(1) final Assignments assignments,
            @Argument(2) final Object target) throws Exception {
        
        Object statement = null;
        Throwable thrown = null;
        
        TestClass testClass = LifecycleHooks.getFieldValue(anchor, "testClass");
        String className = testClass.getJavaClass().getName();
        
        String name = formatDisplayName(method, className);
        Serializable uniqueId = getUniqueID(name, assignments);
        Description description = Description.createTestDescription(className, name, uniqueId);
        
        RunNotifier notifier = Run.getNotifierOf(Run.getThreadRunner());
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        Run.pushThreadRunner(CreateTest.getRunnerForTarget(target));
        
        eachNotifier.fireTestStarted();
        try {
            statement = LifecycleHooks.callProxy(proxy);
        } catch (AssumptionViolatedException e) {
            thrown = e;
            eachNotifier.addFailedAssumption(e);
        } catch (Throwable e) {
            thrown = e;
            eachNotifier.addFailure(e);
        } finally {
            eachNotifier.fireTestFinished();
            Run.popThreadRunner();
        }
        
        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return statement;
    }
    
    private static String formatDisplayName(FrameworkMethod method, String className) {
        return String.format("%s(%s)", method.getName(), className);
    }

    private static String getUniqueID(String displayName, Assignments assignments) {
        return String.format("%s[%08X]", displayName, assignmentsHash(assignments));
    }

    private static int assignmentsHash(Assignments assignments) {
        try {
            return Objects.hash(assignments.getMethodArguments());
        } catch (CouldNotGenerateValueException e) {
            return 0;
        }
    }
}
