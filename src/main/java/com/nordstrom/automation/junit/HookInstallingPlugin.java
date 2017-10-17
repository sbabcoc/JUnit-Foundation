package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;

public class HookInstallingPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return true;
    }

    @Override
    public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription) {
        return builder.method(isAnnotatedWith(anyOf(Test.class, Before.class, After.class))
                .or(isStatic().and(isAnnotatedWith(anyOf(BeforeClass.class, AfterClass.class)))))
                .intercept(MethodDelegation.to(MethodInterceptor.class))
                .implement(Hooked.class);
    }

}
