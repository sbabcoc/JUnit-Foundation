package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class FireTestFailure {

    public static void intercept(@This final RunNotifier notifier, @SuperCall final Callable<?> proxy,
            @Argument(0) final Failure failure) throws Exception {
        
        AtomicTest atomicTest = RunChildren.getAtomicTestOf(failure.getDescription());
        if (atomicTest != null) {
            atomicTest.setThrowable(failure.getException());
        }
        LifecycleHooks.callProxy(proxy);
    }
}
