package com.nordstrom.automation.junit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTestArtifact implements JUnitArtifactType {
    
    private boolean captureDisabled;
    private boolean captureCrippled;
    private CaptureState captureState;
    
    private static final String EXTENSION = "txt";
    private static final String ARTIFACT = "This text artifact was captured for '%s'";
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitTestArtifact.class);
    
    public enum CaptureState {
        ABLE_TO_CAPTURE, CAN_NOT_CAPTURE, CAPTURE_SUCCESS, CAPTURE_FAILED
    }
    
    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean canGetArtifact(Object instance) {
        if (canGet()) {
            setCaptureState(CaptureState.ABLE_TO_CAPTURE);
            return true;
        } else {
            setCaptureState(CaptureState.CAN_NOT_CAPTURE);
            return false;
        }
    }

    @Override
    public byte[] getArtifact(Object instance) {
        if (willGet()) {
            setCaptureState(CaptureState.CAPTURE_SUCCESS);
            JUnitArtifactParams params = (JUnitArtifactParams) instance;
            return String.format(ARTIFACT, params.getDescription().getMethodName()).getBytes().clone();
        } else {
            setCaptureState(CaptureState.CAPTURE_FAILED);
            return new byte[0];
        }
    }

    @Override
    public String getArtifactExtension() {
        return EXTENSION;
    }
    
    void disableCapture() {
        captureDisabled = true;
    }
    
    boolean canGet() {
        return !captureDisabled;
    }

    void crippleCapture() {
        captureCrippled = true;
    }
    
    boolean willGet() {
        return !captureCrippled;
    }

    void setCaptureState(CaptureState state) {
        captureState = state;
    }
    
    CaptureState getCaptureState() {
        return captureState;
    }

}
