package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.nordstrom.automation.junit.JUnitConfig.JUnitSettings;
import com.nordstrom.automation.junit.UnitTestArtifact.CaptureState;
import com.nordstrom.automation.junit.UnitTestWatcher.Notification;

public class ArtifactCollectorTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(JUnitSettings.MAX_RETRY.key(), "1");
    }
    
    @Test
    public void verifyHappyPath() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorPassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 0, "Incorrect retried test count");
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertNull(watcher.getArtifactProvider().getCaptureState(), "Artifact provider capture state should be 'null'");
        assertNull(watcher.getArtifactPath(), "Artifact capture should not have been requested");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FINISHED));
    }
    
    @Test
    public void verifyCaptureOnFailure() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorFailing.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 1, "Incorrect retried test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }
    
    @Test
    public void verifyCanNotCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorDisabled.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 1, "Incorrect retried test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAN_NOT_CAPTURE, "Incorrect artifact provider capture state");
        assertFalse(watcher.getArtifactPath().isPresent(), "Artifact capture output path should not be present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }
    
    @Test
    public void verifyWillNotCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorCrippled.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 1, "Incorrect retried test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_FAILED, "Incorrect artifact provider capture state");
        assertFalse(watcher.getArtifactPath().isPresent(), "Artifact capture output path should not be present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }
    
    @Test
    public void verifyOnDemandCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorOnDemand.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 0, "Incorrect retried test count");
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FINISHED));
    }
    
    @Test
    public void verifyParameterizedCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorParameterized.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 1, "Incorrect retried test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getPassedTests().get(0));
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FINISHED));
        notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }

    @Test
    public void verifyJUnitParamsCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();

        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorJUnitParams.class);
        assertFalse(result.wasSuccessful());

        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 1, "Incorrect retried test count");

        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getPassedTests().get(0));
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FINISHED));
        notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }

    @Test
    public void verifyTheoriesCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();

        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorTheories.class);
        assertFalse(result.wasSuccessful());

        // NOTE: If any permutation fails, the core theory fails
        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");
        assertEquals(rla.getRetriedTests().size(), 0, "Incorrect retried test count");
        // NOTE: Each permutation is distinct from the core theory
        assertEquals(rla.getPassedTheories().size(), 1, "Incorrect passed theory count");
        assertEquals(rla.getFailedTheories().size(), 1, "Incorrect failed theory count");
        assertEquals(rla.getRetriedTheories().size(), 1, "Incorrect retried theory count");

        Description description = rla.getFailedTheories().get(0);
        assertTrue(rla.isTheory(description), "Theory description lacks unique ID prefix");
        
        UnitTestCapture watcher = rla.getWatcher(description);
        assertNotNull(watcher, "Unit test watcher not registered");
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
        
        Optional<UnitTestWatcher> optWatcher = LifecycleHooks.getAttachedWatcher(UnitTestWatcher.class);
        assertTrue(optWatcher.isPresent());
        UnitTestWatcher testWatcher = (UnitTestWatcher) optWatcher.get();
        List<Notification> notifications = testWatcher.getNotificationsFor(rla.getPassedTheories().get(0));
        assertEquals(notifications, Arrays.asList(Notification.STARTED, Notification.FINISHED));
        notifications = testWatcher.getNotificationsFor(description);
        assertEquals(notifications, Arrays.asList(
                Notification.STARTED, Notification.RETRIED, Notification.FINISHED,
                Notification.STARTED, Notification.FAILED, Notification.FINISHED));
    }
    
    @AfterClass
    public static void afterClass() {
        System.clearProperty(JUnitSettings.MAX_RETRY.key());
    }
    
}
