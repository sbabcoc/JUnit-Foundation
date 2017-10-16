package com.nordstrom.automation.junit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class implements the method interceptor for <b>JUnitBase</b> test class objects. This interceptor enables
 * classes that implement the {@link MethodWatcher} interface to perform processing before and after methods
 * with the following JUnit annotations: &#64;Test, &#64;Before, &#64;After, &#64;BeforeClass, and &#64;AfterClass.
 */
public final class MethodInterceptor {
    
    private static final Set<Class<?>> markedClasses = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Class<? extends MethodWatcher>> watcherSet = 
                    Collections.synchronizedSet(new HashSet<>());
    
    private static final List<MethodWatcher> watchers = new ArrayList<>();
    private static final List<MethodWatcher> methodWatchers = new ArrayList<>();
    private static final List<MethodWatcher2> methodWatchers2 = new ArrayList<>();
    
    private MethodInterceptor() {
        throw new AssertionError("JUnitMethodInterceptor is a static utility class that cannot be instantiated");
    }
    
    /**
     * This is the method that intercepts annotated methods in "enhanced" JUnit test classes.
     * 
     * @param clazz "enhanced" class upon which the method was invoked
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     * @param proxy call-able proxy for the intercepted method
     * @return {@code anything} (the result of invoking the intercepted method)
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    @BindingPriority(1)
    public static Object intercept(@Origin Class<?> clazz, @Origin Method method, @AllArguments Object[] args,
                    @SuperCall Callable<?> proxy) throws Exception
    {
        Object result;
        attachWatchers(clazz);
        
        synchronized(methodWatchers2) {
            for (MethodWatcher2 watcher : methodWatchers2) {
                watcher.beforeInvocation(method, args);
            }
        }
        
        try {
            result = proxy.call();
        } finally {
            synchronized(methodWatchers2) {
                for (MethodWatcher2 watcher : methodWatchers2) {
                    watcher.afterInvocation(method, args);
                }
            }
        }
        
        return result;
    }
    
    /**
     * This is the method that intercepts annotated methods in "enhanced" JUnit test class objects.
     * 
     * @param obj "enhanced" object upon which the method was invoked
     * @param method {@link Method} object for the invoked method
     * @param args method invocation arguments
     * @param proxy call-able proxy for the intercepted method
     * @return {@code anything} (the result of invoking the intercepted method)
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    @BindingPriority(2)
    public static Object intercept(@This Object obj, @Origin Method method, @AllArguments Object[] args,
                    @SuperCall Callable<?> proxy) throws Exception
    {
        Object result;
        
        synchronized(methodWatchers) {
            for (MethodWatcher watcher : methodWatchers) {
                watcher.beforeInvocation(obj, method, args);
            }
        }
        synchronized(methodWatchers2) {
            for (MethodWatcher2 watcher : methodWatchers2) {
                watcher.beforeInvocation(obj, method, args);
            }
        }
        
        try {
            result = proxy.call();
        } finally {
            synchronized(watchers) {
                synchronized(methodWatchers) {
                    for (MethodWatcher watcher : methodWatchers) {
                        watcher.afterInvocation(obj, method, args);
                    }
                }
                synchronized(methodWatchers2) {
                    for (MethodWatcher2 watcher : methodWatchers2) {
                        watcher.afterInvocation(obj, method, args);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    public static Optional<MethodWatcher> getAttachedWatcher(Class<? extends MethodWatcher> watcherType) {
        Objects.requireNonNull(watcherType, "[watcherType] must be non-null");
        for (MethodWatcher watcher : watchers) {
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
            if ( ! markedClasses.contains(markedClass)) {
                markedClasses.add(markedClass);
                for (Class<? extends MethodWatcher> watcher : annotation.value()) {
                    attachWatcher(watcher);
                }
            }
        }
    }
    
    /**
     * Wrap the current watcher chain with an instance of the specified watcher class.<br>
     * <b>NOTE</b>: The order in which watcher methods are invoked is determined by the
     * order in which watcher objects are added to the chain. Listener <i>before</i> methods
     * are invoked in last-added-first-called order. Listener <i>after</i> methods are invoked
     * in first-added-first-called order.<br>
     * <b>NOTE</b>: Only one instance of any given watcher class will be included in the chain.
     * 
     * @param watcher watcher class to add to the chain
     */
    private static void attachWatcher(Class<? extends MethodWatcher> watcher) {
        if ( ! watcherSet.contains(watcher)) {
            watcherSet.add(watcher);
            try {
                MethodWatcher watcherObj = watcher.newInstance();
                
                synchronized(watchers) {
                    watchers.add(watcherObj);
                }
                
                if (watcherObj instanceof MethodWatcher2) {
                    synchronized(methodWatchers2) {
                        methodWatchers2.add((MethodWatcher2) watcherObj);
                    }
                } else if (watcherObj instanceof MethodWatcher) {
                    synchronized(methodWatchers) {
                        methodWatchers.add(watcherObj);
                    }
                }
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to instantiate watcher: " + watcher.getName(), e);
            }
        }
    }
}
