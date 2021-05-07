package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.experimental.theories.internal.Assignments;
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
     * method. The actual method block statement is retrieved from <b>MethodBlock</b> and executed,
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
        
        // invoke proxy, which performs the following "magic":
        // * create parameterized statement, caching it for later retrieval
        // * create/execute "theory catalyst", which pushes the thread runner
        LifecycleHooks.callProxy(proxy);
        
        // get runner from "theory catalyst"
        Object runner = Run.getThreadRunner();
        // extract framework method from theory anchor
        FrameworkMethod method = LifecycleHooks.getFieldValue(anchor, "testMethod");
        // get notifier attached to parent runner
        RunNotifier notifier = Run.getNotifierOf(parentRunner);
        // get configured maximum retry count
        int maxRetry = RetryHandler.getMaxRetry(runner, method);
        
        // execute atomic test, retry on failure
        // NOTE - Cached statement is retrieved via MethodBlock.getStatementOf(runner)
        Throwable thrown = RetryHandler.runChildWithRetry(runner, method, null, notifier, maxRetry);
        // pop thread runner
        Run.popThreadRunner();
        
        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }
    }
}
