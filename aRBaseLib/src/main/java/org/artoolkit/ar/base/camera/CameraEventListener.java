package org.artoolkit.ar.base.camera;

public interface CameraEventListener {


    void cameraPreviewStarted(int width, int height, int rate, int cameraIndex, boolean cameraIsFrontFacing);


    void cameraPreviewFrame(byte[] frame);

    void cameraPreviewStopped();

}
