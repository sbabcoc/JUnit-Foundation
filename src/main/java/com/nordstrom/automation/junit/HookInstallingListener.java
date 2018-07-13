package com.nordstrom.automation.junit;

import org.junit.runner.notification.RunListener;

public class HookInstallingListener extends RunListener {
    
    static {
        try {
            Class.forName("com.nordstrom.automation.junit.LifecycleHooks");
        } catch (ClassNotFoundException e) {
        }
    }

}
