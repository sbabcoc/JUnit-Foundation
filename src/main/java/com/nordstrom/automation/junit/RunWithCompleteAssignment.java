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
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares an interceptor for the
 * {@link TheoryAnchor#runWithCompleteAssignment runWithCompleteAssignment}
 * method.
 */
public class RunWithCompleteAssignment {

    /**
     * Interceptor for the {@link TheoryAnchor#runWithCompleteAssignment
     * runWithCompleteAssignment} method.
     * <p>
     * <b>NOTE</b>: This method relies on the "theory catalyst" created by the {@link MethodBlock}
     * class to attach the class runner to the thread and create a new atomic test for the target
     * method. The actual method block statement is retrieved from <b>MethodBlock<b> and executed,
     * publishing a complete set of test lifecycle events.
     * 
     * @param anchor current {@code TheoryAnchor} statement
     * @param proxy callable proxy for the intercepted method
     * @param assignments arguments for this theory permutation
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static void intercept(@This final TheoryAnchor anchor, @SuperCall final Callable<?> proxy,
            @Argument(0) final Assignments assignments) throws Exception {
        
        // grab the current thread runner
        Object parentRunner = Run.getThreadRunner();
        
        LifecycleHooks.callProxy(proxy); // NOTE: This pushes the BlockJUnit4ClassRunner
           
        Throwable thrown = null;
        Object classRunner = Run.getThreadRunner();
        FrameworkMethod method = LifecycleHooks.getFieldValue(anchor, "testMethod");
        
        RunNotifier notifier = Run.getNotifierOf(parentRunner);
        Description description = LifecycleHooks.invoke(classRunner, "describeChild", method);
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        
        eachNotifier.fireTestStarted();
        try {
            MethodBlock.getStatementOf(classRunner).evaluate();
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
    }
}
