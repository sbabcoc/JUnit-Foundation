package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.nordstrom.automation.junit.UnitTestArtifact.CaptureState;

public class ArtifactCollectorTest {

    @Test
    public void verifyHappyPath() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorPassing.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 1, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 0, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertNull("Artifact provider capture state should be 'null'", watcher.getArtifactProvider().getCaptureState());
        assertNull("Artifact capture should not have been requested", watcher.getArtifactPath());
    }
    
    @Test
    public void verifyCaptureOnFailure() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorFailing.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 0, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 1, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals("Incorrect artifact provider capture state", CaptureState.CAPTURE_SUCCESS, watcher.getArtifactProvider().getCaptureState());
        assertTrue("Artifact capture output path is not present", watcher.getArtifactPath().isPresent());
    }
    
    @Test
    public void verifyCanNotCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorDisabled.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 0, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 1, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals("Incorrect artifact provider capture state", CaptureState.CAN_NOT_CAPTURE, watcher.getArtifactProvider().getCaptureState());
        assertFalse("Artifact capture output path should not be present", watcher.getArtifactPath().isPresent());
    }
    
    @Test
    public void verifyWillNotCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorCrippled.class);
        assertFalse(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 0, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 1, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
        
        Description description = rla.getFailedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals("Incorrect artifact provider capture state", CaptureState.CAPTURE_FAILED, watcher.getArtifactProvider().getCaptureState());
        assertFalse("Artifact capture output path should not be present", watcher.getArtifactPath().isPresent());
    }
    
    @Test
    public void verifyOnDemandCapture() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorOnDemand.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 1, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 0, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
        
        Description description = rla.getPassedTests().get(0);
        UnitTestCapture watcher = ArtifactCollector.getWatcher(description, UnitTestCapture.class).get();
        assertEquals("Incorrect artifact provider capture state", CaptureState.CAPTURE_SUCCESS, watcher.getArtifactProvider().getCaptureState());
        assertTrue("Artifact capture output path is not present", watcher.getArtifactPath().isPresent());
    }
}
