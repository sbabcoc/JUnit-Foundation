package com.nordstrom.automation.junit;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runners.model.FrameworkField;

import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;

public class PhantomTimeout {
    
    private static final Class<? extends FrameworkField> TYPE_PHANTOM;
    
    static Constructor<FrameworkField> ctor;
    static Field field;
    
    @Rule
    public Timeout timeoutRule;
    
    static {
        try {
            ctor = FrameworkField.class.getDeclaredConstructor(Field.class);
            field = PhantomTimeout.class.getDeclaredField("timeoutRule");
        } catch (NoSuchMethodException | SecurityException | NoSuchFieldException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
        
        TYPE_PHANTOM = new ByteBuddy()
        .subclass(FrameworkField.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
        .name("org.junit.runners.model.PhantomTimeout")
        .defineField("timeoutRule", Timeout.class, Visibility.PRIVATE)
        .defineConstructor(Visibility.PUBLIC)
        .withParameters(Timeout.class)
        .intercept(MethodCall.invoke(ctor).with(field)
                        .andThen(FieldAccessor.ofField("timeoutRule").setsArgumentAt(0)))
        .method(named("get")).intercept(FieldAccessor.ofField("timeoutRule"))
        .make()
        .load(FrameworkField.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
        .getLoaded();
    }
    
    public static FrameworkField create(Timeout timeoutRule) {
        try {
            return TYPE_PHANTOM.getConstructor(Timeout.class).newInstance(timeoutRule);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }
}
