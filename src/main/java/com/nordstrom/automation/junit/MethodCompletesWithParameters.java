package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.AssumptionViolatedException;
import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;

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
        
    	Object parentRunner = Run.getThreadRunner();
    	RunNotifier notifier = Run.getNotifierOf(parentRunner);
        Object classRunner = CreateTest.getRunnerForTarget(target);
        Description description = LifecycleHooks.invoke(classRunner, "describeChild", method);
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        
        Object statement = null;
        Throwable thrown = null;
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
        }
        
        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return statement;
    }
}
