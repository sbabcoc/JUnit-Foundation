package com.nordstrom.automation.junit;

import com.google.common.base.Optional;

public class ReferenceTracker implements RunnerWatcher {
    
    @Override
    public void runStarted(Object runner) {
        // nothing to do here
    }

    @Override
    public void runFinished(Object runner) {
        // get a reference to the ReferenceChecker run listener
        Optional<ReferenceChecker> checker = LifecycleHooks.getAttachedListener(ReferenceChecker.class);
        // if listener is present
        if (checker.isPresent()) {
            // invoke "run finished" handler
            checker.get().runFinished(runner);
        }
    }
    
}
