package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class Schedule {

    public static void intercept(@This final Object scheduler, @SuperCall final Callable<?> proxy) throws Exception {
        RunChildren.started();
        LifecycleHooks.callProxy(proxy);
    }
}
