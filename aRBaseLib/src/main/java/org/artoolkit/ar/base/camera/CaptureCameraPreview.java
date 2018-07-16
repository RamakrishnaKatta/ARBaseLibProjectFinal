package org.artoolkit.ar.base.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.artoolkit.ar.base.EnvconstantsAR;
import org.artoolkit.ar.base.FPSCounter;
import org.artoolkit.ar.base.R;

import java.io.IOException;

@SuppressLint("ViewConstructor")
public class CaptureCameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    /**
     * Android logging tag for this class.
     */
    private static final String TAG = "CaptureCameraPreview";

    /**
     * The Camera doing the capturing.
     */
    public Camera camera = null;
    public int rotation;
    private CameraWrapper cameraWrapper = null;

    /**
     * The camera capture width in pixels.
     */
    private int captureWidth;

    /**
     * The camera capture height in pixels.
     */
    private int captureHeight;

    /**
     * The camera capture rate in frames per second.
     */
    private int captureRate;

    /**
     * Counter to monitor the actual rate at which frames are captured from the camera.
     */
    private FPSCounter fpsCounter = new FPSCounter();

    /**
     * Listener to inform of camera related events: start, frame, and stop.
     */
    private CameraEventListener listener;

    /**
     * Constructor takes a {@link CameraEventListener} which will be called on
     * to handle camera related events.
     *
     * @param cel CameraEventListener to use. Can be null.
     */
    @SuppressWarnings("deprecation")
    public CaptureCameraPreview(Context context, CameraEventListener cel) {
        super(context);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // Deprecated in API level 11. Still required for API levels <= 10.

        setCameraEventListener(cel);
    }

    /**
     * Sets the {@link CameraEventListener} which will be called on to handle camera
     * related events.
     *
     * @param cel CameraEventListener to use. Can be null.
     */
    public void setCameraEventListener(CameraEventListener cel) {
        listener = cel;
    }

    @SuppressLint("NewApi")
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolderInstance) {

        int cameraIndex = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraIndex", "0"));
        Log.e(TAG, "surfaceCreated(): Called, Opening camera " + cameraIndex + ", setting preview surface and orientation.");
        try {
            camera = Camera.open(cameraIndex);
            Log.e(TAG, "surfaceCreated(): Camera open");

        } catch (RuntimeException ex) {
            Log.e(TAG, "surfaceCreated(): RuntimeException " + ex.getMessage() + ".");
            return;
        } catch (Exception ex) {
            Log.e(TAG, "surfaceCreated()): Exception " + ex.getMessage() + ".");
            return;
        }

        //camera.setPreviewDisplay(surfaceHolderInstance);

        if (!setPreviewOrientationAndSurface(surfaceHolderInstance, cameraIndex)) {
            Log.e(TAG, "surfaceCreated(): call to setPreviewOrientationAndSurface() failed.");
        } else {
            Log.e(TAG, "surfaceCreated(): succeeded");
        }
    }

    private boolean setPreviewOrientationAndSurface(SurfaceHolder surfaceHolderInstance, int cameraIndex) {
        Log.e(TAG, "setPreviewOrientationAndSurface(): called");
        boolean success = true;
        try {
            setCameraPreviewDisplayOrientation(cameraIndex, camera);
            camera.setPreviewDisplay(surfaceHolderInstance);
        } catch (IOException ex) {
            Log.e(TAG, "setPreviewOrientationAndSurface(): IOException " + ex.toString());
            success = false;
        } catch (Exception ex) {
            Log.e(TAG, "setPreviewOrientationAndSurface(): Exception " + ex.toString());
            success = false;
        }
        if (!success) {
            if (null != camera) {
                camera.release();
                camera = null;
            }
            Log.e(TAG, "setPreviewOrientationAndSurface(): released camera due to caught exception");
        }
        return success;
    }

    private void setCameraPreviewDisplayOrientation(int cameraId, Camera camera) {

        WindowManager wMgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        assert wMgr != null;
        rotation = wMgr.getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;     // Landscape with camera on left side
            case Surface.ROTATION_90:
                degrees = 90;
                break;   // Portrait with camera on top side
            case Surface.ROTATION_180:
                degrees = 180;
                break; // Landscape with camera on right side
            case Surface.ROTATION_270:
                degrees = 270;
                break; // Portrait with camera on bottom side
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        // Set the clockwise rotation of preview display in degrees. This affects the preview frames and
        // the picture displayed after snapshot.
        camera.setDisplayOrientation(result);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolderInstance) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.

        if (camera != null) {

            camera.setPreviewCallback(null);
            camera.stopPreview();

            camera.release();
            camera = null;
        }

        if (listener != null) listener.cameraPreviewStopped();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (camera == null) {
            // Camera wasn't opened successfully?
            Log.e(TAG, "surfaceChanged(): No camera in surfaceChanged");
            return;
        }

        /**
         * Get the current Resolution of the Phone in the form of Pixels
         * Save then to a String called CameraResolution
         */
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        String cam_height = Integer.toString(height);
        String cam_width = Integer.toString(width);
        String CameraResolution = cam_width + "x" + cam_height;
        Log.e(TAG, "surfaceChanged(): CameraResolution : (Acquired) " + CameraResolution);

        Log.e(TAG, "surfaceChanged(): Surfaced changed, setting up camera and starting preview");

        /**
         * Get the current Resolution of the Camera and set those parameters to the current camera
         * Save then to a String called CameraResolution
         */
        String camResolution = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraResolution", getResources().getString(R.string.pref_defaultValue_cameraResolution));
//        String camResolution = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(CameraResolution, CameraResolution);
        String[] dims = camResolution.split("x", 2);
        Camera.Parameters parameters = camera.getParameters();
        Log.e(TAG, "surfaceChanged(): CameraResolution : (Applied) " + Integer.parseInt(dims[0]) + "x" + Integer.parseInt(dims[1]));
        parameters.setPreviewSize(Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
        parameters.setPreviewFrameRate(30);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);

        parameters = camera.getParameters();
        captureWidth = parameters.getPreviewSize().width;
        captureHeight = parameters.getPreviewSize().height;
        captureRate = parameters.getPreviewFrameRate();
        Log.e(TAG, "surfaceChanged(): Applied Camera parameters will be : " + "CameraCaptureWidth: " + captureWidth + "CameraCaptureHeight: " + captureHeight + " CameraCaptureRate: " + captureRate);

        int pixelformat = parameters.getPreviewFormat(); // android.graphics.imageformat
        PixelFormat pixelinfo = new PixelFormat();
        PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);

        int cameraIndex = 0;
        boolean cameraIsFrontFacing = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraIndex = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_cameraIndex", "0"));
        Camera.getCameraInfo(cameraIndex, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            cameraIsFrontFacing = true;

        int bufSize = captureWidth * captureHeight * pixelinfo.bitsPerPixel / 8; // For the default NV21 format, bitsPerPixel = 12.
        Log.e(TAG, "surfaceChanged(): Camera buffers will be " + captureWidth + "x" + captureHeight + "@" + pixelinfo.bitsPerPixel + "bpp, " + bufSize + "bytes.");
        cameraWrapper = new CameraWrapper(camera);
        cameraWrapper.configureCallback(this, true, 10, bufSize); // For the default NV21 format, bitsPerPixel = 12.

        camera.startPreview();

        if (listener != null)
            listener.cameraPreviewStarted(captureWidth, captureHeight, captureRate, cameraIndex, cameraIsFrontFacing);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (listener != null) listener.cameraPreviewFrame(data);
        cameraWrapper.frameReceived(data);

        if (fpsCounter.frame()) {
            Log.e(TAG, "onPreviewFrame(): Camera capture FPS: " + fpsCounter.getFPS());
        }
    }

    public int SetContinousPicture() {
        int returnval = 0;
        Camera.Parameters parameters = camera.getParameters();
        if (EnvconstantsAR.CONTINOUSPICTURE) {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                returnval = 1;
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                Log.e(TAG, "CameraModes(): Focus Continuous Picture Applied ");
            } else {
                returnval = 0;
                Toast.makeText(getContext(), "This feature is not supported on your Device", Toast.LENGTH_LONG).show();
            }
        }
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);
        camera.startPreview();
        return returnval;
    }

    public int setAutoFocus() {
        int returnval = 0;
        Camera.Parameters parameters = camera.getParameters();

        if (EnvconstantsAR.AUTOFOCUS) {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                returnval = 1;
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                Log.e(TAG, "CameraModes(): Focus Auto Applied ");
            } else {
                returnval = 0;
                Toast.makeText(getContext(), "This feature is not supported on your Device", Toast.LENGTH_LONG).show();
            }
        } else {
            camera.cancelAutoFocus();
        }
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);
        camera.startPreview();
        return returnval;
    }

    public int setAutoScene() {
        int returnval = 0;
        Camera.Parameters parameters = camera.getParameters();
        if (EnvconstantsAR.AUTOSCENE) {
            if (parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                returnval = 1;
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                Log.e(TAG, "CameraModes(): Scene Auto Mode Applied ");
            } else {
                returnval = 0;
                Toast.makeText(getContext(), "This feature is not supported on your Device", Toast.LENGTH_LONG).show();
            }
        }
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);
        camera.startPreview();
        return returnval;
    }

    public int setSteadyShot() {
        int returnval = 0;
        Camera.Parameters parameters = camera.getParameters();
        if (EnvconstantsAR.STEADYSHOT) {

            if (parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_STEADYPHOTO)) {
                returnval = 1;
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
                Log.e(TAG, "CameraModes(): Scene STEADY PHOTO Applied ");
            } else {
                returnval = 0;
                Toast.makeText(getContext(), "This feature is not supported on your Device", Toast.LENGTH_LONG).show();
            }
        }
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);
        camera.startPreview();
        return returnval;
    }

    public int setWhiteBalance() {
        int returnval = 0;
        Camera.Parameters parameters = camera.getParameters();
        if (EnvconstantsAR.WHITEBALANCE) {
            if (parameters.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_INCANDESCENT)) {
                returnval = 1;
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
                Log.e(TAG, "CameraModes(): White Balance Auto Applied ");
            } else {
                returnval = 0;
                Toast.makeText(getContext(), "This feature is not supported on your Device", Toast.LENGTH_LONG).show();
            }
        }
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);
        camera.startPreview();
        return returnval;
    }

    public int setHDR() {
        int returnval = 0;
        Camera.Parameters parameters = camera.getParameters();
        if (EnvconstantsAR.HDR) {
            if (parameters.getSupportedWhiteBalance().contains(Camera.Parameters.SCENE_MODE_HDR)) {
                returnval = 1;
                parameters.setWhiteBalance(Camera.Parameters.SCENE_MODE_HDR);
                Log.e(TAG, "CameraModes(): HDR Applied ");
            } else {
                returnval = 0;
                Toast.makeText(getContext(), "This feature is not supported on your Device", Toast.LENGTH_LONG).show();
            }
        }
        parameters.setExposureCompensation(0);
        camera.setParameters(parameters);
        camera.startPreview();
        return returnval;
    }
}
