package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.nordstrom.automation.junit.UnitTestWatcher.Notification;

public class BeforeClassFailureTest {

    @Test
    public void verifyBeforeClassExceptionHandling() {
        ReferenceChecker checker = new ReferenceChecker();

        JUnitCore runner = new JUnitCore();
        runner.addListener(checker);
        Result result = runner.run(BeforeClassThrowsException.class);
        assertFalse(result.wasSuccessful());
        assertEquals(1, result.getFailureCount(), "Incorrect failed test count");
        Failure failure = result.getFailures().get(0);
        assertEquals(RuntimeException.class, failure.getException().getClass(), "Incorrect exception class");
        assertEquals("Must be failed", failure.getMessage(), "Incorrect exception message");
        assertTrue(failure.getDescription().isSuite(), "Failure should describe a suite");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent(), "Unit test watcher not attached");
        UnitTestWatcher watcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = watcher.getNotificationsFor(failure.getDescription());
        assertEquals(notifications, Arrays.asList(Notification.FAILED), "Incorrect event notifications");
        ReferenceReleaseTest.checkLeakReports(checker);
    }
    
}
