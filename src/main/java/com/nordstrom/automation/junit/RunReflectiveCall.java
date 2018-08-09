package com.nordstrom.automation.junit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.runners.model.FrameworkMethod;

import com.nordstrom.common.base.UncheckedThrow;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.internal.runners.model.ReflectiveCallable#runReflectiveCall
 * runReflectiveCall} method.
 */
@SuppressWarnings("squid:S1118")
public class RunReflectiveCall {
    
    private static final Set<Class<?>> markedClasses = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Class<? extends MethodWatcher>> watcherSet =
                    Collections.synchronizedSet(new HashSet<>());

    private static final List<MethodWatcher> methodWatchers = new ArrayList<>();
  
    @RuntimeType
    public static Object intercept(@This final Object obj, @SuperCall final Callable<?> proxy) throws Exception {
        FrameworkMethod method = null;
        Object target = null;
        Object[] params = null;

        try {
            Object owner = getFieldValue(obj, "this$0");
            if (owner instanceof FrameworkMethod) {
                method = (FrameworkMethod) owner;
                target = getFieldValue(obj, "val$target");
                params = getFieldValue(obj, "val$params");
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                        | IllegalAccessException e) {
            // handled below
        }
        
        if (method == null) {
            return proxy.call();
        }

        attachWatchers(method.getDeclaringClass());

        Object result = null;
        Throwable thrown = null;
        synchronized (methodWatchers) {
            for (MethodWatcher watcher : methodWatchers) {
                watcher.beforeInvocation(target, method, params);
            }
        }

        try {
            result = proxy.call();
        } catch (Throwable t) {
            thrown = t;
        } finally {
            synchronized (methodWatchers) {
                for (MethodWatcher watcher : methodWatchers) {
                    watcher.afterInvocation(target, method, thrown);
                }
            }
        }

        if (thrown != null) {
            throw UncheckedThrow.throwUnchecked(thrown);
        }

        return result;
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    public static Optional<MethodWatcher> getAttachedWatcher(
                    Class<? extends MethodWatcher> watcherType) {
        Objects.requireNonNull(watcherType, "[watcherType] must be non-null");
        for (MethodWatcher watcher : methodWatchers) {
            if (watcher.getClass() == watcherType) {
                return Optional.of(watcher);
            }
        }
        return Optional.empty();
    }

    /**
     * Attach watchers that are active on the specified test class.
     * 
     * @param testClass test class
     */
    static void attachWatchers(Class<?> testClass) {
        MethodWatchers annotation = testClass.getAnnotation(MethodWatchers.class);
        if (null != annotation) {
            Class<?> markedClass = testClass;
            while (null == markedClass.getDeclaredAnnotation(MethodWatchers.class)) {
                markedClass = markedClass.getSuperclass();
            }
            if (!markedClasses.contains(markedClass)) {
                markedClasses.add(markedClass);
                for (Class<? extends MethodWatcher> watcher : annotation.value()) {
                    attachWatcher(watcher);
                }
            }
        }
    }

    /**
     * Wrap the current watcher chain with an instance of the specified watcher class.<br>
     * <b>NOTE</b>: The order in which watcher methods are invoked is determined by the order in
     * which watcher objects are added to the chain. Listener <i>before</i> methods are invoked in
     * last-added-first-called order. Listener <i>after</i> methods are invoked in
     * first-added-first-called order.<br>
     * <b>NOTE</b>: Only one instance of any given watcher class will be included in the chain.
     * 
     * @param watcher watcher class to add to the chain
     */
    private static void attachWatcher(Class<? extends MethodWatcher> watcher) {
        if (!watcherSet.contains(watcher)) {
            watcherSet.add(watcher);
            try {
                synchronized (methodWatchers) {
                    methodWatchers.add(watcher.newInstance());
                }
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to instantiate watcher: " + watcher.getName(),
                                e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFieldValue(Object obj, String name) throws NoSuchFieldException,
                    SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

}
