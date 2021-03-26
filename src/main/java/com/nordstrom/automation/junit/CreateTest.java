package com.nordstrom.automation.junit;

import java.util.concurrent.Callable;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#createTest
 * createTest} method.
 */
@SuppressWarnings("squid:S1118")
public class CreateTest {
    
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
    public static Object intercept(@This final Object runner, @Argument(0) final FrameworkMethod method,
                    @SuperCall final Callable<?> proxy) throws Exception {
        
        Object target = LifecycleHooks.callProxy(proxy);
        // apply parameter-based global timeout
        TimeoutUtils.applyTestTimeout(runner, method, target);
        
        LOGGER.debug("testObjectCreated: {}", target);
        
        for (TestObjectWatcher watcher : LifecycleHooks.getObjectWatchers()) {
            watcher.testObjectCreated(runner, method, target);
        }
        
        return target;
    }
}
