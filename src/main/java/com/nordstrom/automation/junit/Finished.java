package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class Finished {
    public static void intercept(@This final Object scheduler, @SuperCall final Callable<?> proxy) throws Exception {
        LifecycleHooks.callProxy(proxy);
        RunChild.finished();
    }
}