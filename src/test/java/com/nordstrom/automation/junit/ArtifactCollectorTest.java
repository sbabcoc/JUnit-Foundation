package com.nordstrom.automation.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class ArtifactCollectorTest {

    @Test
    public void verifyHappyPath() {
        RunListenerAdapter rla = new RunListenerAdapter();
        
        JUnitCore runner = new JUnitCore();
        runner.addListener(rla);
        Result result = runner.run(ArtifactCollectorTestCases.class);
        assertTrue(result.wasSuccessful());
        
        assertEquals("Incorrect passed test count", 1, rla.getPassedTests().size());
        assertEquals("Incorrect failed test count", 0, rla.getFailedTests().size());
        assertEquals("Incorrect ignored test count", 0, rla.getIgnoredTests().size());
        
        UnitTestCapture watcher = JUnitArtifactCollector.getWatcher(UnitTestCapture.class).get();
        assertNull("Artifact provider capture state should be 'null'", watcher.getArtifactProvider().getCaptureState());
        assertNull("Artifact capture should not have been requested", watcher.getArtifactPath());
    }
    
//    @Test
//    public void verifyCaptureOnFailure() {
//        
//        ListenerChain lc = new ListenerChain();
//        TestListenerAdapter rla = new TestListenerAdapter();
//        
//        TestNG testNG = new TestNG();
//        testNG.setTestClasses(new Class[]{ArtifactCollectorTestCases.class});
//        testNG.addListener((ITestNGListener) lc);
//        testNG.addListener((ITestNGListener) rla);
//        testNG.setGroups("testFailed");
//        testNG.run();
//        
//        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
//        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
//        assertEquals(rla.getSkippedTests().size(), 0, "Incorrect skipped test count");
//        assertEquals(rla.getFailedButWithinSuccessPercentageTests().size(), 0, "Incorrect curve-graded success count");
//        assertEquals(rla.getConfigurationFailures().size(), 0, "Incorrect configuration method failure count");
//        assertEquals(rla.getConfigurationSkips().size(), 0, "Incorrect configuration method skip count");
//        
//        ITestResult result = rla.getFailedTests().get(0);
//        assertEquals(UnitTestArtifact.getCaptureState(result), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
//        assertTrue(UnitTestCapture.getArtifactPath(result).isPresent(), "Artifact capture output path is not present");
//    }
//    
//    @Test
//    public void verifyCanNotCapture() {
//        
//        ListenerChain lc = new ListenerChain();
//        TestListenerAdapter rla = new TestListenerAdapter();
//        
//        TestNG testNG = new TestNG();
//        testNG.setTestClasses(new Class[]{ArtifactCollectorTestCases.class});
//        testNG.addListener((ITestNGListener) lc);
//        testNG.addListener((ITestNGListener) rla);
//        testNG.setGroups("canNotCapture");
//        testNG.run();
//        
//        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
//        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
//        assertEquals(rla.getSkippedTests().size(), 0, "Incorrect skipped test count");
//        assertEquals(rla.getFailedButWithinSuccessPercentageTests().size(), 0, "Incorrect curve-graded success count");
//        assertEquals(rla.getConfigurationFailures().size(), 0, "Incorrect configuration method failure count");
//        assertEquals(rla.getConfigurationSkips().size(), 0, "Incorrect configuration method skip count");
//        
//        ITestResult result = rla.getFailedTests().get(0);
//        assertEquals(UnitTestArtifact.getCaptureState(result), CaptureState.CAN_NOT_CAPTURE, "Incorrect artifact provider capture state");
//        assertFalse(UnitTestCapture.getArtifactPath(result).isPresent(), "Artifact capture output path should not be present");
//    }
//    
//    @Test
//    public void verifyWillNotCapture() {
//        
//        ListenerChain lc = new ListenerChain();
//        TestListenerAdapter rla = new TestListenerAdapter();
//        
//        TestNG testNG = new TestNG();
//        testNG.setTestClasses(new Class[]{ArtifactCollectorTestCases.class});
//        testNG.addListener((ITestNGListener) lc);
//        testNG.addListener((ITestNGListener) rla);
//        testNG.setGroups("willNotCapture");
//        testNG.run();
//        
//        assertEquals(rla.getPassedTests().size(), 0, "Incorrect passed test count");
//        assertEquals(rla.getFailedTests().size(), 1, "Incorrect failed test count");
//        assertEquals(rla.getSkippedTests().size(), 0, "Incorrect skipped test count");
//        assertEquals(rla.getFailedButWithinSuccessPercentageTests().size(), 0, "Incorrect curve-graded success count");
//        assertEquals(rla.getConfigurationFailures().size(), 0, "Incorrect configuration method failure count");
//        assertEquals(rla.getConfigurationSkips().size(), 0, "Incorrect configuration method skip count");
//        
//        ITestResult result = rla.getFailedTests().get(0);
//        assertEquals(UnitTestArtifact.getCaptureState(result), CaptureState.CAPTURE_FAILED, "Incorrect artifact provider capture state");
//        assertFalse(UnitTestCapture.getArtifactPath(result).isPresent(), "Artifact capture output path should not be present");
//    }
//    
//    @Test
//    public void verifyOnDemandCapture() {
//        
//        ListenerChain lc = new ListenerChain();
//        TestListenerAdapter rla = new TestListenerAdapter();
//        
//        TestNG testNG = new TestNG();
//        testNG.setTestClasses(new Class[]{ArtifactCollectorTestCases.class});
//        testNG.addListener((ITestNGListener) lc);
//        testNG.addListener((ITestNGListener) rla);
//        testNG.setGroups("onDemandCapture");
//        testNG.run();
//        
//        assertEquals(rla.getPassedTests().size(), 1, "Incorrect passed test count");
//        assertEquals(rla.getFailedTests().size(), 0, "Incorrect failed test count");
//        assertEquals(rla.getSkippedTests().size(), 0, "Incorrect skipped test count");
//        assertEquals(rla.getFailedButWithinSuccessPercentageTests().size(), 0, "Incorrect curve-graded success count");
//        assertEquals(rla.getConfigurationFailures().size(), 0, "Incorrect configuration method failure count");
//        assertEquals(rla.getConfigurationSkips().size(), 0, "Incorrect configuration method skip count");
//        
//        ITestResult result = rla.getPassedTests().get(0);
//        assertEquals(UnitTestArtifact.getCaptureState(result), CaptureState.CAPTURE_SUCCESS, "Incorrect artifact provider capture state");
//        assertTrue(UnitTestCapture.getArtifactPath(result).isPresent(), "Artifact capture output path is not present");
//    }
}
