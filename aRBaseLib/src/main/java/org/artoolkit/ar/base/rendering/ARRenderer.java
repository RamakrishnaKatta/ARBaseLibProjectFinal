package org.artoolkit.ar.base.rendering;

import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import org.artoolkit.ar.base.ARToolKit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Date;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Base renderer which should be subclassed in the main application and provided
 * to the ARActivity using its {@link supplyRenderer} method.
 * <p/>
 * Subclasses should override {@link configureARScene}, which will be called by
 * the Activity when AR initialisation is complete. The Renderer can use this method
 * to add markers to the scene, and perform other scene initialisation.
 * <p/>
 * The {@link draw} method should also be override to perfom actual rendering. This is
 * in preference to directly overriding {@link onDrawFrame}, because ARRendererGLES20 will check
 * that the ARToolKit is running before calling draw.
 */
public class ARRenderer implements GLSurfaceView.Renderer {

    /**
     * Android logging tag for this class.
     */
    protected final static String TAG = "ARRenderer";
    public boolean printOptionEnable = false;
    public int width_surface, height_surface;
    Bitmap inBitmap = null;

    /**
     * Allows subclasses to load markers and prepare the scene. This is called after
     * initialisation is complete.
     */
    public boolean configureARScene() {
        return true;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Transparent background
        GLES10.glClearColor(0.0f, 0.0f, 0.0f, 0.f);

        GLES10.glDisable(GLES10.GL_DITHER);
        GLES10.glHint(GLES10.GL_PERSPECTIVE_CORRECTION_HINT, GLES10.GL_FASTEST);
        GLES10.glEnable(GLES10.GL_CULL_FACE);
        GLES10.glShadeModel(GLES10.GL_SMOOTH);
        GLES10.glEnable(GLES10.GL_DEPTH_TEST);
    }

    public void onSurfaceChanged(GL10 unused, int w, int h) {
        GLES10.glViewport(0, 0, w, h);

        float ratio = (float) w / h;

        width_surface = w;
        height_surface = h;
    }

    public void onDrawFrame(GL10 gl) {
        if (ARToolKit.getInstance().isRunning()) {
            draw(gl);
            CaptureAugmentScreen();
        }
    }

    /**
     * Should be overridden in subclasses and used to perform rendering.
     */
    public void draw(GL10 gl) {
        GLES10.glClear(GLES10.GL_COLOR_BUFFER_BIT | GLES10.GL_DEPTH_BUFFER_BIT);
    }

    public void CaptureAugmentScreen() {
        try {
            if (printOptionEnable) {
                printOptionEnable = false;
                Log.e(TAG, "printOptionEnable if condition:" + printOptionEnable);
                int w = width_surface;
                int h = height_surface;

                Log.e(TAG, "w:" + w + "-----h:" + h);

                int b[] = new int[w * h];
                int bt[] = new int[w * h];
                IntBuffer buffer = IntBuffer.wrap(b);
                buffer.position(0);
                //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                for (int i = 0; i < h; i++) {
                    //remember, that OpenGL bitmap is incompatible with Android bitmap and so, some correction need.
                    for (int j = 0; j < w; j++) {
                        int pix = b[i * w + j];
                        int pb = (pix >> 16) & 0xff;
                        int pr = (pix << 16) & 0x00ff0000;
                        int pix1 = (pix & 0xff00ff00) | pr | pb;
                        bt[(h - i - 1) * w + j] = pix1;
                    }
                }

                // image naming and path to include sd card appending name you choose for file
//                String sPath = Environment.getExternalStorageDirectory().toString() + "/L_CATALOG/Screenshots";

                if (inBitmap == null || !inBitmap.isMutable() || inBitmap.getWidth() != w || inBitmap.getHeight() != h) {
                    inBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                }

                inBitmap.copyPixelsFromBuffer(buffer);
                inBitmap = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap getReturnbitmap() {
        return inBitmap;
    }
}

