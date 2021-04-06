package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bind.annotation.Argument;

import static com.nordstrom.automation.junit.LifecycleHooks.toMapKey;

/**
 * This class declares the interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#methodBlock
 * methodBlock} method.
 */
public class MethodBlock {
    private static final ThreadLocal<ConcurrentMap<Integer, DepthGauge>> METHOD_DEPTH;
    private static final Function<Integer, DepthGauge> NEW_INSTANCE;
    private static final Map<String, Statement> RUNNER_TO_STATEMENT = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodBlock.class);
    
    static {
        METHOD_DEPTH = new ThreadLocal<ConcurrentMap<Integer, DepthGauge>>() {
            @Override
            protected ConcurrentMap<Integer, DepthGauge> initialValue() {
                return new ConcurrentHashMap<>();
            }
        };
        NEW_INSTANCE = new Function<Integer, DepthGauge>() {
            @Override
            public DepthGauge apply(Integer input) {
                return new DepthGauge();
            }
        };
    }

    /**
     * Interceptor for the {@link org.junit.runners.BlockJUnit4ClassRunner#methodBlock methodBlock} method.
     * <p>
     * <b>NOTE</b>: For "theory" methods, the actual class runner statement is stored and a
     * "lifecycle catalyst" statement is returned instead. This enables the interceptor declared
     * in the {@link RunWithCompleteAssignment} class to manage the execution of the actual
     * statement, publishing a complete set of test lifecycle events.
     * 
     * @param runner underlying test runner
     * @param proxy callable proxy for the intercepted method
     * @param method framework method that's the "identity" of an atomic test
     * @return {@link Statement} to execute the atomic test
     * @throws Exception {@code anything} (exception thrown by the intercepted method)
     */
    public static Statement intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
            @Argument(0) final FrameworkMethod method) throws Exception {

        DepthGauge depthGauge = LifecycleHooks.computeIfAbsent(METHOD_DEPTH.get(), runner.hashCode(), NEW_INSTANCE);
        depthGauge.increaseDepth();
        
        Statement statement = (Statement) LifecycleHooks.callProxy(proxy);
        
        // if at ground level
        if (0 == depthGauge.decreaseDepth()) {
            METHOD_DEPTH.remove();
            try {
                // get parent of test runner
                Object parent = LifecycleHooks.getFieldValue(runner, "this$0");
                // if child of TheoryAnchor statement
                if (parent instanceof TheoryAnchor) {
                    // store actual statement of test runner
                    RUNNER_TO_STATEMENT.put(toMapKey(runner), statement);
                    // create lifecycle catalyst
                    statement = new Statement() {
                        final Object threadRunner = runner;
                        
                        @Override
                        public void evaluate() throws Throwable {
                            // attach class runner to thread
                            RunChildren.pushThreadRunner(threadRunner);
                        }
                    };
                }
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                // nothing to do here
            }
        }
        
        return statement;
    }
    
    /**
     * Get the statement associated with the specified runner.
     * 
     * @param runner JUnit class runner
     * @return {@link Statement} for the specified runner; may be {@code null}
     */
    static Statement getStatementOf(final Object runner) {
        return RUNNER_TO_STATEMENT.remove(toMapKey(runner));
    }
    
    static boolean isEmpty() {
        boolean isEmpty = true;
        if (RUNNER_TO_STATEMENT.isEmpty()) {
            LOGGER.debug("RUNNER_TO_STATEMENT is empty");
        } else {
            isEmpty = false;
            LOGGER.debug("RUNNER_TO_STATEMENT is not empty");
        }
        return isEmpty;
    }
}
