package org.artoolkit.ar.base.camera;

/**
 * The CameraEventListener interface allows an observer to respond to events
 * from a {@link CaptureCameraPreview}.
 */
public interface CameraEventListener {

    /**
     * Called when the camera preview is started. The video dimensions and frame rate
     * are passed through, along with information about the camera.
     *
     * @param width               The width of the video image in pixels.
     * @param height              The height of the video image in pixels.
     * @param rate                The capture rate in frames per second.
     * @param cameraIndex         Zero-based index of the camera in use. If only one camera is present, will be 0.
     * @param cameraIsFrontFacing false if camera is rear-facing (the default) or true if camera is facing toward the user.
     */
    void cameraPreviewStarted(int width, int height, int rate, int cameraIndex, boolean cameraIsFrontFacing);

    /**
     * Called when the camera preview has a new frame ready.
     *
     * @param frame A byte array from the camera, in the camera's capture format.
     */
    void cameraPreviewFrame(byte[] frame);

    /**
     * Called when the capture preview is stopped. No new frames will be sent.
     */
    void cameraPreviewStopped();

}
