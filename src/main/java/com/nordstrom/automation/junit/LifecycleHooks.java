package com.nordstrom.automation.junit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestClass;

import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.file.PathUtils.ReportsDirectory;

/**
 * This class implements the hooks and utility methods that activate the core functionality of <b>JUnit Foundation</b>.
 */
public class LifecycleHooks {

    private static JUnitConfig config;
    private static final List<JUnitWatcher> watchers;
    private static final List<RunListener> runListeners;
    private static final List<RunWatcher> runWatchers;
    private static final List<RunnerWatcher> runnerWatchers;
    private static final List<TestObjectWatcher> objectWatchers;
    private static final List<MethodWatcher<?>> methodWatchers;

    private LifecycleHooks() {
        throw new AssertionError("LifecycleHooks is a static utility class that cannot be instantiated");
    }
    
    /**
     * This static initializer installs a shutdown hook for each specified listener. It also rebases the ParentRunner
     * and BlockJUnit4ClassRunner classes to enable the core functionality of JUnit Foundation.
     */
    static {
        WatcherClassifier classifier = new WatcherClassifier();
        
        for (JUnitWatcher watcher : ServiceLoader.load(JUnitWatcher.class)) {
            classifier.add(watcher);
        }

        for (RunListener listener : ServiceLoader.load(RunListener.class)) {
            classifier.add(listener);
        }

        for (ShutdownListener watcher : ServiceLoader.load(ShutdownListener.class)) {
            classifier.add(watcher);
        }
        
        for (RunWatcher watcher : ServiceLoader.load(RunWatcher.class)) {
            classifier.add(watcher);
        }
        
        for (RunnerWatcher watcher : ServiceLoader.load(RunnerWatcher.class)) {
            classifier.add(watcher);
        }
        
        for (TestObjectWatcher watcher : ServiceLoader.load(TestObjectWatcher.class)) {
            classifier.add(watcher);
        }
        
        for (MethodWatcher<?> watcher : ServiceLoader.load(MethodWatcher.class)) {
            classifier.add(watcher);
        }

        watchers = classifier.watchers;
        runListeners = classifier.listeners;
        
        runWatchers = new WatcherList<>(classifier.runWatcherIndexes);
        runnerWatchers = new WatcherList<>(classifier.runnerWatcherIndexes);
        objectWatchers = new WatcherList<>(classifier.objectWatcherIndexes);
        methodWatchers = new WatcherList<>(classifier.methodWatcherIndexes);
    }
    
    private static class WatcherClassifier {
        int i = 0;

        List<JUnitWatcher> watchers = new ArrayList<>();
        List<Class<? extends JUnitWatcher>> watcherClasses = new ArrayList<>();

        List<RunListener> listeners = new ArrayList<>();
        List<Class<? extends RunListener>> listenerClasses = new ArrayList<>();

        List<Integer> runWatcherIndexes = new ArrayList<>();
        List<Integer> runnerWatcherIndexes = new ArrayList<>();
        List<Integer> objectWatcherIndexes = new ArrayList<>();
        List<Integer> methodWatcherIndexes = new ArrayList<>();

        boolean add(JUnitWatcher watcher) {
            if ( ! watcherClasses.contains(watcher.getClass())) {
                watchers.add(watcher);
                watcherClasses.add(watcher.getClass());

                if (watcher instanceof RunListener) add((RunListener) watcher);

                if (watcher instanceof ShutdownListener) {
                    Runtime.getRuntime().addShutdownHook(getShutdownHook((ShutdownListener) watcher));
                }

                if (watcher instanceof RunWatcher) runWatcherIndexes.add(i);
                if (watcher instanceof RunnerWatcher) runnerWatcherIndexes.add(i);
                if (watcher instanceof TestObjectWatcher) objectWatcherIndexes.add(i);
                if (watcher instanceof MethodWatcher) methodWatcherIndexes.add(i);

                i++;
                return true;
            }
            return false;
        }

        boolean add(RunListener listener) {
            if ( ! listenerClasses.contains(listener.getClass())) {
                listeners.add(listener);
                listenerClasses.add(listener.getClass());
                return true;
            }
            return false;
        }
    }
    
    /**
     * Create a {@link Thread} object that encapsulated the specified shutdown listener.
     * 
     * @param listener shutdown listener object
     * @return shutdown listener thread object
     */
    static Thread getShutdownHook(final ShutdownListener listener) {
        return new Thread() {
            @Override
            public void run() {
                listener.onShutdown();
            }
        };
    }
    
    /**
     * Get the configuration object for JUnit Foundation.
     * 
     * @return JUnit Foundation configuration object
     */
    static synchronized JUnitConfig getConfig() {
        if (config == null) {
            config = JUnitConfig.getConfig();
        }
        return config;
    }
    
    /**
     * Get the atomic test associated with the specified instance.
     * 
     * @param target instance of JUnit test class
     * @return {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} for specified instance
     */
    public static AtomicTest getAtomicTestOf(Object target) {
        return EachTestNotifierInit.getAtomicTestOf(target);
    }
    
    /**
     * Get the parent runner that owns specified child runner or framework method.
     * 
     * @param child {@code ParentRunner} or {@code FrameworkMethod} object
     * @return {@code ParentRunner} object that owns the specified child ({@code null} for root
     *         objects)
     */
    public static Object getParentOf(Object child) {
        return Run.getParentOf(child);
    }

    /**
     * Get the run notifier associated with the specified parent runner.
     * 
     * @param runner JUnit parent runner
     * @return {@link org.junit.runner.notification.RunNotifier RunNotifier} object for the specified parent runner
     *         (may be {@code null})
     */
    public static Object getNotifierOf(final Object runner) {
        return Run.getNotifierOf(runner);
    }
    
    /**
     * Get the runner that owns the active thread context.
     * 
     * @return active {@code ParentRunner} object (may be ({@code null})
     */
    public static Object getThreadRunner() {
        return Run.getThreadRunner();
    }
    
    /**
     * Get the test class object associated with the specified parent runner.
     * 
     * @param runner target {@link org.junit.runners.ParentRunner ParentRunner} object
     * @return {@link TestClass} associated with specified runner
     */
    public static TestClass getTestClassOf(Object runner) {
        return invoke(runner, "getTestClass");
    }
    
    /**
     * Get the atomic test object for the specified method description.
     * 
     * @param description JUnit method description
     * @return {@link AtomicTest} object (may be {@code null})
     */
    public static AtomicTest getAtomicTestOf(Description description) {
        return EachTestNotifierInit.getAtomicTestOf(description);
    }
    
    /**
     * Get the test class instance for the specified method description.
     * 
     * @param description JUnit method description
     * @return test class instance (may be {@code null})
     */
    public static Object getTargetOf(Description description) {
        return EachTestNotifierInit.getTargetOf(description);
    }
    
    /**
     * Get the description for the specified child object.
     * 
     * @param runner target {@link org.junit.runners.ParentRunner ParentRunner} object
     * @param child child object
     * @return {@link Description} for the specified framework method (may be {@code null})
     */
    public static Description describeChild(Object runner, Object child) {
        if (runner != null && child != null) {
            Class<?> runnerType = getSupportedType(runner);
            if (runnerType != null && runnerType.isInstance(child)) {
                return invoke(runner, "describeChild", child);
            }
        }
        return null;
    }
    
    /**
     * Get the type of children supported by the specified runner.
     * 
     * @param runner  target {@link org.junit.runners.ParentRunner ParentRunner} object
     * @return supported child type; {@code null} if undetermined
     */
    private static Class<?> getSupportedType(Object runner) {
        for (Method method : runner.getClass().getDeclaredMethods()) {
            if ("describeChild".equals(method.getName())) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if ((paramTypes.length == 1) && (paramTypes[0] != Object.class)) {
                    return paramTypes[0];
                }
            }
        }
        return null;
    }
    
    /**
     * Get the {@link ReflectiveCallable} object for the specified description.
     *
     * @param description JUnit method description
     * @return <b>ReflectiveCallable</b> object (may be {@code null})
     */
    public static ReflectiveCallable getCallableOf(Description description) {
        return RunReflectiveCall.getCallableOf(description);
    }

    /**
     * Synthesize a {@link ReflectiveCallable} closure with the specified parameters.
     *
     * @param method {@link Method} object to be invoked
     * @param target test class instance to target
     * @param params method invocation parameters
     * @return <b>ReflectiveCallable</b> object as specified
     */
    public static ReflectiveCallable encloseCallable(final Method method, final Object target, final Object... params) {
        return new ReflectiveCallable() {
            @Override
            protected Object runReflectiveCall() throws Throwable {
                return method.invoke(target, params);
            }
        };
    }

    /**
     * Get class of specified test class instance.
     * 
     * @param instance test class instance
     * @return class of test class instance
     */
    public static Class<?> getInstanceClass(Object instance) {
        Class<?> clazz = instance.getClass();      
        return (instance instanceof Hooked) ? clazz.getSuperclass() : clazz;
    }
    
    /**
     * Get fully-qualified name to use for hooked test class.
     * 
     * @param testObj test class object being hooked
     * @return fully-qualified name for hooked subclass
     */
    static String getSubclassName(Object testObj) {
        Class<?> testClass = testObj.getClass();
        String testClassName = testClass.getSimpleName();
        String testPackageName = testClass.getPackage().getName();
        ReportsDirectory constant = ReportsDirectory.fromObject(testObj);
        
        switch (constant) {
            case FAILSAFE_2:
            case FAILSAFE_3:
            case SUREFIRE_2:
            case SUREFIRE_3:
            case SUREFIRE_4:
                return testPackageName + ".Hooked" + testClassName;
                
            default:
                return testClass.getCanonicalName() + "Hooked";
        }
        
    }
    
    /**
     * Invoke the named method with the specified parameters on the specified target object.
     * 
     * @param <T> method return type
     * @param target target object
     * @param methodName name of the desired method
     * @param parameters parameters for the method invocation
     * @return result of method invocation
     */
    @SuppressWarnings("unchecked")
    static <T> T invoke(Object target, String methodName, Object... parameters) {
        try {
            Method method = findMethod(target, methodName, parameters);
            return (T) method.invoke(target, parameters);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }
    
    /**
     * Find the named method compatible with the supplied argument in the target object.
     * 
     * @param target target object
     * @param methodName name of the desired method
     * @param parameters parameters for the method invocation
     * @return {@link Method} object for invocation
     * @throws NoSuchMethodException
     */
    private static Method findMethod(Object target, String methodName, Object... parameters)
            throws NoSuchMethodException {
        Class<?> clazz = target.getClass();
        Class<?>[] paramTypes = new Class<?>[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            paramTypes[i] = parameters[i].getClass();
        }

        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) &&
                        isCompatible(method.getParameterTypes(), paramTypes)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            clazz = clazz.getSuperclass(); // Move up the class hierarchy
        }

        throw new NoSuchMethodException("Failed finding method " + 
                methodName + " " + ArrayUtils.toString(paramTypes) +
                " in target " + target.getClass().getName());
    }

    /**
     * Determine if declared method parameter types are compatible with provided parameters.
     *  
     * @param declaredParams types of parameters declared by discovered method
     * @param providedParams types of provided parameters
     * @return {@code true} if declared parameters are compatible; otherwise {@code false}
     */
    private static boolean isCompatible(Class<?>[] declaredParams, Class<?>[] providedParams) {
        if (declaredParams.length != providedParams.length) {
            return false;
        }
        for (int i = 0; i < declaredParams.length; i++) {
            if (!declaredParams[i].isAssignableFrom(providedParams[i])) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the specified field of the supplied object.
     * 
     * @param target target object
     * @param name field name
     * @return {@link Field} object for the requested field
     * @throws NoSuchFieldException if a field with the specified name is not found
     * @throws SecurityException if the request is denied
     */
    static Field getDeclaredField(Object target, String name) throws NoSuchFieldException {
        Throwable thrown = null;
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                thrown = e;
            } catch (SecurityException e) {
                thrown = e;
                break;
            }
        }
        
        throw UncheckedThrow.throwUnchecked(thrown);
    }

    /**
     * Get the value of the specified field from the supplied object.
     * 
     * @param <T> field value type
     * @param target target object
     * @param name field name
     * @return {@code anything} - the value of the specified field in the supplied object
     * @throws IllegalAccessException if the {@code Field} object is enforcing access control for an inaccessible field
     * @throws NoSuchFieldException if a field with the specified name is not found
     * @throws SecurityException if the request is denied
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object target, String name) throws IllegalAccessException, NoSuchFieldException, SecurityException {
        Field field = getDeclaredField(target, name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    /**
     * Invoke an intercepted method through its callable proxy.
     * <p>
     * <b>NOTE</b>: If the invoked method throws an exception, this method re-throws the original exception.
     * 
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - value returned by the intercepted method
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @SuppressWarnings("unchecked")
    static <T> T callProxy(final Callable<?> proxy) throws Exception {
        try {
            return (T) proxy.call();
        } catch (InvocationTargetException e) {
            throw UncheckedThrow.throwUnchecked(e.getCause());
        }
    }
    
    /**
     * Get reference to an instance of the specified watcher type.
     * 
     * @param <T> watcher type
     * @param watcherType watcher type
     * @return optional watcher instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends JUnitWatcher> Optional<T> getAttachedWatcher(Class<T> watcherType) {
        for (JUnitWatcher watcher : watchers) {
            if (watcher.getClass() == watcherType) {
                return Optional.of((T) watcher);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get reference to an instance of the specified listener type.
     * 
     * @param <T> listener type
     * @param listenerType listener type
     * @return optional listener instance
     */
    public static <T extends RunListener> Optional<T> getAttachedListener(Class<T> listenerType) {
        // search for specified type among loader-attached listeners
        Optional<T> optListener = findListener(listenerType, runListeners);
        // if specified type not found
        if ( ! optListener.isPresent()) {
            // search for specified type among API-attached listeners
            optListener = findListener(listenerType, getAttachedListeners());
        }
        
        return optListener;
    }
    
    /**
     * Retrieve run listener collection from active notifier.
     * 
     * @return run listener collection
     */
    private static List<RunListener> getAttachedListeners() {
        // get active thread runner
        Object runner = getThreadRunner();
        // if runner acquired
        if (runner != null) {
            // get active run notifier
            Object notifier = getNotifierOf(runner);
            // if notifier acquired
            if (notifier != null) {
                try {
                    // get attached run listener collection
                    return getFieldValue(notifier, "listeners");
                } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
                    // nothing to do here
                }
            }
        }
        // default to empty list
        return new ArrayList<>();
    }
    
    /**
     * Get reference to an instance of the specified listener type from the supplied list.
     * 
     * @param <T> listener type
     * @param type listener type
     * @param list listener list
     * @return optional listener instance
     */
    @SuppressWarnings("unchecked")
    private static <T extends RunListener> Optional<T> findListener(Class<T> type, List<RunListener> list) {
        for (RunListener listener : list) {
            if (listener.getClass() == type) {
                return Optional.of((T) listener);
            }
        }
        return Optional.empty();
    }
    
    /**
     * If the specified key is not already associated with a value (or is mapped to {@code null}), attempts
     * to compute its value using the given mapping function and enters it into this map unless {@code null}.
     * 
     * @param <K> data type of map keys
     * @param <T> data type of map values
     * @param map concurrent map to be manipulated
     * @param key key with which the specified value is to be associated
     * @param fun the function to compute a value
     * @return the current (existing or computed) value associated with the specified key;
     *         {@code null} if the computed value is {@code null}
     */
    static <K, T> T computeIfAbsent(ConcurrentMap<K, T> map, K key, Function<K, T> fun) {
        T val = map.get(key);
        if (val == null) {
            T obj = fun.apply(key);
            val = (val = map.putIfAbsent(key, obj)) == null ? obj : val;
        }
        return val;
    }

    /**
     * Get the list of attached {@link RunListener} objects.
     *
     * @return run listener list
     */
    static List<RunListener> getRunListeners() {
        return runListeners;
    }

    /**
     * Get the list of attached {@link RunWatcher} objects.
     * 
     * @return run watcher list
     */
    static List<RunWatcher> getRunWatchers() {
        return runWatchers;
    }
    
    /**
     * Get the list of attached {@link RunnerWatcher} objects.
     * 
     * @return runner watcher list
     */
    static List<RunnerWatcher> getRunnerWatchers() {
        return runnerWatchers;
    }
    
    /**
     * Get the list of attached {@link TestObjectWatcher} objects.
     * 
     * @return test object watcher list
     */
    static List<TestObjectWatcher> getObjectWatchers() {
        return objectWatchers;
    }
    
    /**
     * Get the list of attached {@link MethodWatcher} objects.
     * 
     * @return method watcher list
     */
    static List<MethodWatcher<?>> getMethodWatchers() {
        return methodWatchers;
    }
    
    /**
     * Create a unique map key string to represent the specified object.
     * <p>
     * <b>NOTE</b>: The string returned by this method matches the output of
     *              the default {@link Object#toString()} implementation.
     * 
     * @param obj target object
     * @return map key string
     */
    static String toMapKey(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }
    
    /**
     * This class encapsulates the process of retrieving watcher objects of the target type from the collection of all
     * attached watcher objects. This is a private nested class that directly accesses the main collection. It is also
     * unmodifiable. Any attempts to alter the collection will trigger an {@link UnsupportedOperationException}.
     * 
     * @param <T> subclass of {@link JUnitWatcher} object supplied by this instance
     */
    private static class WatcherList<T extends JUnitWatcher> extends AbstractList<T> {
        
        private int[] indexes;
        
        /**
         * Constructor for a list of watcher objects of the target type retrieved from the collection of all attached
         * {@link JUnitWatcher} objects.
         * 
         * @param indexes indexes of watchers of the requisite type in the main collection
         */
        private WatcherList(List<Integer> indexes) {
            int i = 0;
            this.indexes = new int[indexes.size()];
            for (int index : indexes) {
                this.indexes[i++] = index;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public T get(int index) {
            return (T) watchers.get(indexes[index]);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return indexes.length;
        }
    }
}
