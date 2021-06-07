package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;
import com.nordstrom.automation.junit.UnitTestWatcher.Notification;

public class AutomaticRetryTest {
    
    @BeforeClass
    public static void beforeClass() {
        System.setProperty(JUnitSettings.MAX_RETRY.key(), "3");
    }
    
    @Test
    public void testHappyPath() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryPassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 0, "Incorrect retried test count");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getPassedTests().get(0));
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FINISHED));
    }
    
    @Test
    public void testPassOnRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryPassOnRetry.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 1, "Incorrect retried test count");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getRetriedTests().get(0));
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FINISHED));
    }
    
    @Test
    public void testFailOnRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryFailing.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 3, "Incorrect retried test count");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getRetriedTests().get(0));
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }
    
    @Test
    public void testNoRetry() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryNoRetry.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 0, "Incorrect retried test count");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getFailedTests().get(0));
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }
    
    @Test
    public void testIgnore() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(AutomaticRetryIgnore.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 1, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 0, "Incorrect retried test count");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getIgnoredTests().get(0));
        assertEquals(notifications, Arrays.asList(Notification.IGNORED));
    }
    
    @AfterClass
    public static void afterClass() {
        System.clearProperty(JUnitSettings.MAX_RETRY.key());
    }
    
}
