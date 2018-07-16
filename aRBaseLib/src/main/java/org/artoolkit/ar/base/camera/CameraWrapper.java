package org.artoolkit.ar.base.camera;

import android.hardware.Camera;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps camera functionality to handle the differences between Android 2.1 and 2.2.
 */
class CameraWrapper {

    /**
     * Android logging tag for this class.
     */
    private static final String TAG = "CameraWrapper";

    private static final String CAMERA_CLASS_NAME = "android.hardware.Camera";

    private Camera camera;

    private Class<?> cameraClass = null;

    private Method setPreviewCallbackMethod = null;
    private Method setPreviewCallbackWithBufferMethod = null;
    private Method addCallbackBufferMethod = null;

    private boolean usingBuffers = false;

    public CameraWrapper(Camera cam) {

        camera = cam;

        try {
            cameraClass = Class.forName(CAMERA_CLASS_NAME);
            Log.e(TAG, "CameraWrapper(): Found class " + CAMERA_CLASS_NAME);

            setPreviewCallbackMethod = cameraClass.getDeclaredMethod("setPreviewCallback", Camera.PreviewCallback.class);
            Log.e(TAG, "CameraWrapper(): Found method setPreviewCallback");

            setPreviewCallbackWithBufferMethod = cameraClass.getDeclaredMethod("setPreviewCallbackWithBuffer", Camera.PreviewCallback.class);
            Log.e(TAG, "CameraWrapper(): Found method setPreviewCallbackWithBuffer");

            addCallbackBufferMethod = cameraClass.getDeclaredMethod("addCallbackBuffer", byte[].class);
            Log.e(TAG, "CameraWrapper(): Found method addCallbackBuffer");

        } catch (NoSuchMethodException nsme) {
            Log.w(TAG, "CameraWrapper(): Could not find method: " + nsme.getMessage());


        } catch (ClassNotFoundException cnfe) {
            Log.w(TAG, "CameraWrapper(): Could not find class " + CAMERA_CLASS_NAME);
        }

    }

    public boolean configureCallback(Camera.PreviewCallback cb, boolean useBuffersIfAvailable, int numBuffersIfAvailable, int bufferSize) {

        boolean success = true;

        if (useBuffersIfAvailable && setPreviewCallbackWithBufferMethod != null && addCallbackBufferMethod != null) {

            success &= setPreviewCallbackWithBuffer(cb);

            for (int i = 0; i < numBuffersIfAvailable; i++) {
                success &= addCallbackBuffer(new byte[bufferSize]);
            }
            usingBuffers = true;
        } else {

            success &= setPreviewCallback(cb);
            usingBuffers = false;
        }

        if (success) {

            if (usingBuffers) {
                Log.e(TAG, "configureCallback(): Configured camera callback using " + numBuffersIfAvailable + " buffers of " + bufferSize + " bytes");
            } else {
                Log.i(TAG, "configureCallback(): Configured camera callback without buffers");
            }
        }
        return success;
    }

    public boolean frameReceived(byte[] data) {
        //Log.e(TAG, "frameReceived");

        if (usingBuffers) {
            return addCallbackBuffer(data);
        } else {
            return true;
        }
    }

    private boolean setPreviewCallback(Camera.PreviewCallback cb) {

        if (setPreviewCallbackMethod == null) return false;

        try {

            setPreviewCallbackMethod.invoke(camera, cb);

        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean setPreviewCallbackWithBuffer(Camera.PreviewCallback cb) {

        if (setPreviewCallbackMethod == null) return false;

        try {

            setPreviewCallbackWithBufferMethod.invoke(camera, cb);

        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean addCallbackBuffer(byte[] data) {

        if (addCallbackBufferMethod == null) return false;

        try {

            addCallbackBufferMethod.invoke(camera, data);
            //Log.e(TAG, "Returned camera data buffer to pool");

        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}