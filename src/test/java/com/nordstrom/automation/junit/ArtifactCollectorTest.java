package com.nordstrom.automation.junit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.testng.annotations.Test;

import com.nordstrom.automation.junit.UnitTestArtifact.CaptureState;

public class ArtifactCollectorTest {

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
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertNull(watcher.getArtifactProvider().getCaptureState(), "Artifact provider capture state should be 'null'");
        assertNull(watcher.getArtifactPath(), "Artifact capture should not have been requested");
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
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
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
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAN_NOT_CAPTURE, "Incorrect artifact provider capture state");
        assertFalse(watcher.getArtifactPath().isPresent(), "Artifact capture output path should not be present");
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
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_FAILED, "Incorrect artifact provider capture state");
        assertFalse(watcher.getArtifactPath().isPresent(), "Artifact capture output path should not be present");
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
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(watcher.getArtifactProvider().getCaptureState(), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
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
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertNull(watcher.getArtifactProvider().getCaptureState(), "Artifact provider capture state should be 'null'");
        assertNull(watcher.getArtifactPath(), "Artifact capture should not have been requested");
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

        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertNull(watcher.getArtifactProvider().getCaptureState(), "Artifact provider capture state should be 'null'");
        assertNull(watcher.getArtifactPath(), "Artifact capture should not have been requested");
    }

    @Test
    public void verifyTheoriesCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();

        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorTheories.class);
        assertFalse(result.wasSuccessful());

        assertEquals(rla.getPassedTests().size(), 2, "Incorrect passed test count");
        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
        assertEquals(rla.getIgnoredTests().size(), 0, "Incorrect ignored test count");

        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertNull(watcher.getArtifactProvider().getCaptureState(), "Artifact provider capture state should be 'null'");
        assertNull(watcher.getArtifactPath(), "Artifact capture should not have been requested");
    }
}
