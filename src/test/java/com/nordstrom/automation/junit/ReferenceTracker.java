package com.nordstrom.automation.junit;

import com.google.common.base.Optional;

public class ReferenceTracker implements RunnerWatcher {
    
    @Override
    public void runStarted(Object runner) {
        // nothing to do here
    }

    @Override
    public void runFinished(Object runner) {
        Optional<ReferenceChecker> checker = LifecycleHooks.getAttachedListener(ReferenceChecker.class);
        if (checker.isPresent()) {
            checker.get().runFinished(runner);
        }
    }
    
}
