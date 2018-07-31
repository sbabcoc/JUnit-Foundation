package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;

public class HookInstallingPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return ! target.isAssignableTo(Hooked.class);
    }

    @Override
    public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription) {
        return installHook(builder, typeDescription);
    }
    
    public static Builder<?> installHook(Builder<?> builder, TypeDescription typeDescription) {
        return builder.method(isTestOrConfiguration())
                      .intercept(MethodDelegation.to(MethodInterceptor.class))
                      .implement(Hooked.class);
    }
    
    public static <T extends AnnotationSource> ElementMatcher.Junction<T> isTestOrConfiguration() {
        return declaresAnnotation(annotationType(isAnnotatedWith(anyOf(Test.class, Before.class, After.class))
                        .or(isStatic().and(isAnnotatedWith(anyOf(BeforeClass.class, AfterClass.class))))));
    }

}
