package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
 * createTest} method.
 */
@SuppressWarnings("squid:S1118")
public class CreateTest {

    /*Using ConcurrentHashMap as a cheap & fast thread-safe HashSet. Does have a bit of memory overhead.*/
    private static final Map<String, String> TARGETS = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTest.class);
    
    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest createTest} method.
     * 
     * @param runner target {@link org.junit.runners.BlockJUnit4ClassRunner BlockJUnit4ClassRunner} object
     * @param proxy callable proxy for the intercepted method
     * @return {@code anything} - JUnit test class instance
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    @RuntimeType
    public static Object intercept(@This final Object runner,
                    @SuperCall final Callable<?> proxy) throws Exception {
        
        Object target = LifecycleHooks.callProxy(proxy);
        // apply parameter-based global timeout
        TimeoutUtils.applyTestTimeout(runner, target);

        String targetHash = hash(target);
        if (null == TARGETS.put(targetHash, targetHash)) {
            LOGGER.debug("testObjectCreated: {}", target);

            for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
                watcher.testObjectCreated(target, runner);
            }
        }
        
        return target;
    }

    private static String hash(Object o) {
        return o.getClass().getCanonicalName() + "-" + System.identityHashCode(o);
    }
    
}
