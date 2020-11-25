package com.nordstrom.automation.junit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bind.annotation.Argument;

public class MethodBlock {
    private static final Set<String> runnerList = new CopyOnWriteArraySet<>();
    private static final Map<Object, Statement> RUNNER_TO_STATEMENT = new ConcurrentHashMap<>();

    public static Statement intercept(@This final Object runner, @SuperCall final Callable<?> proxy,
            @Argument(0) final FrameworkMethod method) throws Exception {

        Statement statement = (Statement) LifecycleHooks.callProxy(proxy);
        
        if (!runnerList.add(runner.toString())) {
            try {
                Object parent = LifecycleHooks.getFieldValue(runner, "this$0");
                if (parent instanceof TheoryAnchor) {
                    RUNNER_TO_STATEMENT.put(runner, statement);
                    statement = new Statement() {
                        final Object threadRunner = runner;
                        
                        @Override
                        public void evaluate() throws Throwable {
                            Run.pushThreadRunner(threadRunner);
                        }
                    };
                }
            } catch (IllegalAccessException | NoSuchFieldException | SecurityException | IllegalArgumentException e) {
                // nothing to do here
            }
        }
        
        return statement;
    }
    
    static Statement getStatementOf(final Object runner) {
        return RUNNER_TO_STATEMENT.get(runner);
    }
}
