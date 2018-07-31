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
        
        assertEquals(1, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(0, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        
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
        
        assertEquals(0, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(1, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(CaptureState.CAPTURE_SUCCESS, watcher.getArtifactProvider().getCaptureState(), "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
    }
    
    @Test
    public void verifyCanNotCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorDisabled.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(0, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(1, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(CaptureState.CAN_NOT_CAPTURE, watcher.getArtifactProvider().getCaptureState(), "Incorrect artifact provider capture state");
        assertFalse(watcher.getArtifactPath().isPresent(), "Artifact capture output path should not be present");
    }
    
    @Test
    public void verifyWillNotCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorCrippled.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals(0, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(1, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(CaptureState.CAPTURE_FAILED, watcher.getArtifactProvider().getCaptureState(), "Incorrect artifact provider capture state");
        assertFalse(watcher.getArtifactPath().isPresent(), "Artifact capture output path should not be present");
    }
    
    @Test
    public void verifyOnDemandCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorOnDemand.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals(1, rla.getPassedTests().size(), "Incorrect passed test count");
        assertEquals(0, rla.getFailedTests().size(), "Incorrect failed test count");
        assertEquals(0, rla.getIgnoredTests().size(), "Incorrect ignored test count");
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals(CaptureState.CAPTURE_SUCCESS, watcher.getArtifactProvider().getCaptureState(), "Incorrect artifact provider capture state");
        assertTrue(watcher.getArtifactPath().isPresent(), "Artifact capture output path is not present");
    }
}
