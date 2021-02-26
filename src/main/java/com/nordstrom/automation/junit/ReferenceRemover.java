package com.nordstrom.automation.junit;

import org.junit.internal.AssumptionViolatedException;

public class ReferenceRemover implements RunnerWatcher, RunWatcher<Object> {
    
    @Override
    public void runStarted(Object runner) {
    }

    @Override
    public void runFinished(Object runner) {
        // release callables associated with runner
        RunReflectiveCall.releaseCallablesOf(runner);
        
        // release mapping of parent runner to atomic test
        AtomicTest<Object> atomicTest = RunAnnouncer.releaseAtomicTestOf(runner);
        // if mapping existed
        if (atomicTest != null) {
            // release mapping of method description to atomic test
            RunAnnouncer.releaseAtomicTestOf(atomicTest.getDescription());
        }
        
        // release runner/target mappings
        CreateTest.releaseMappingsFor(runner);
    }

    @Override
    public Class<Object> supportedType() {
        return Object.class;
    }

    @Override
    public void testStarted(AtomicTest<Object> atomicTest) {
    }

    @Override
    public void testFinished(AtomicTest<Object> atomicTest) {
        // release mapping of method description to atomic test
        RunAnnouncer.releaseAtomicTestOf(atomicTest.getDescription());
    }

    @Override
    public void testFailure(AtomicTest<Object> atomicTest, Throwable thrown) {
    }

    @Override
    public void testAssumptionFailure(AtomicTest<Object> atomicTest, AssumptionViolatedException thrown) {
    }

    @Override
    public void testIgnored(AtomicTest<Object> atomicTest) {
    }
}
