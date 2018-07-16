package org.artoolkit.ar.base;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.artoolkit.ar.base.camera.CameraEventListener;
import org.artoolkit.ar.base.camera.CaptureCameraPreview;
import org.artoolkit.ar.base.rendering.ARRenderer;
import org.artoolkit.ar.base.rendering.gles20.ARRendererGLES20;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

//import android.os.AsyncTask;
//import android.os.AsyncTask.Status;

/**
 * An activity which can be subclassed to create an AR application. ARActivity handles almost all of
 * the required operations to create a simple augmented reality application.
 * <p/>
 * ARActivity automatically creates a camera preview surface and an OpenGL surface view, and
 * arranges these correctly in the user interface.The subclass simply needs to provide a FrameLayout
 * object which will be populated with these UI components, using {@link #supplyFrameLayout() supplyFrameLayout}.
 * <p/>
 * To create a custom AR experience, the subclass should also provide a custom renderer using
 * {@link #supplyRenderer() Renderer}. This allows the subclass to handle OpenGL drawing calls on its own.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public abstract class ARActivity extends Activity implements CameraEventListener, View.OnClickListener {

    /**
     * Android logging tag for this class.
     */
    protected final static String TAG = "ARActivity";

    /**
     * Renderer to use. This is provided by the subclass using {@link #supplyRenderer() Renderer()}.
     */
    protected ARRenderer renderer;

    /**
     * Layout that will be filled with the camera preview and GL views. This is provided by the subclass using {@link #supplyFrameLayout() supplyFrameLayout()}.
     */
    protected FrameLayout mainFrameLayout;

    /**
     * Camera preview which will provide video frames.
     */
    private CaptureCameraPreview preview;

    /**
     * GL surface to render the virtual objects
     */
    private GLSurfaceView mOpenGlSurfaceViewInstance;

    private boolean firstUpdate = false;
    private Context mContext;
    private AppCompatImageButton mOptionsButton;
    private AppCompatImageButton mScreenshotButton;
    private AppCompatImageButton mHdrButton, mAutoSceneButton, mWhiteBalanceButton, mContinuousPictureButton, mAutoFocusButton, mSteadyShotButton, mFlashButton;
    private LinearLayout mHdrButtonArea, mAutoSceneButtonArea, mWhiteBalanceButtonArea, mContinuousPictureButtonArea, mAutoFocusButtonArea, mSteadyShotButtonArea, mFlashButtonArea;

    private TextView arTimer, arProgressText;

    private boolean flashmode = false;
    private boolean camera_options_visibility = false;
    private boolean hdr_toggle = false;
    private boolean steady_toggle = false;
    private boolean autoscene_toggle = false;
    private boolean autofocus_toggle = false;
    private boolean continouspicture_toggle = false;
    private boolean whitebalance_toggle = false;

    private View AugmentScreenLayout, OptionsButtonLayout, CameraOptionsButtonLayout;

    @SuppressWarnings("unused")
    public Context getAppContext() {
        return mContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        // This needs to be done just only the very first time the application is run,
        // or whenever a new preference is added (e.g. after an application upgrade).
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Correctly configures the activity window for running AR in a layer
        // on top of the camera preview. This includes entering
        // fullscreen landscape mode and enabling transparency.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        AndroidUtils.reportDisplayInformation(this);
    }

    /**
     * Allows subclasses to supply a custom {@link Renderer}.
     *
     * @return The {@link Renderer} to use.
     */
    protected abstract ARRenderer supplyRenderer();

    /**
     * Allows subclasses to supply a {@link FrameLayout} which will be populated
     * with a camera preview and GL surface view.
     *
     * @return The {@link FrameLayout} to use.
     */
    protected abstract FrameLayout supplyFrameLayout();

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        super.onStart();

        Log.i(TAG, "onStart(): Activity starting.");

        if (!ARToolKit.getInstance().initialiseNative(this.getCacheDir().getAbsolutePath())) { // Uses cache directory provided by LCatalog for Data files.
            Log.e(TAG, "The native library is not loaded. The application cannot continue.");
            return;
        }

        mainFrameLayout = supplyFrameLayout();
        if (mainFrameLayout == null) {
            Log.e(TAG, "onStart(): Error: supplyFrameLayout did not return a layout.");
            return;
        }

        renderer = supplyRenderer();
        if (renderer == null) {
            Log.e(TAG, "onStart(): Error: supplyRenderer did not return a renderer.");
            // No renderer supplied, use default, which does nothing
            renderer = new ARRenderer();
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();

        // Create the camera preview
        preview = new CaptureCameraPreview(this, this);

        Log.e(TAG, "onResume(): CaptureCameraPreview created");

        // Create the GL view
        mOpenGlSurfaceViewInstance = new GLSurfaceView(this);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo;
        configurationInfo = activityManager.getDeviceConfigurationInfo();
        boolean supportsEs2;
        assert configurationInfo != null;
        supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            Log.e(TAG, "onResume(): OpenGL ES 2.x is supported");

            if (renderer instanceof ARRendererGLES20) {
                // Request an OpenGL ES 2.0 compatible context.
                mOpenGlSurfaceViewInstance.setEGLContextClientVersion(2);
            } else {
                Log.e(TAG, "onResume(): OpenGL ES 2.x is supported but only a OpenGL 1.x renderer is available." +
                        " \n Use ARRendererGLES20 for ES 2.x support. \n Continuing with OpenGL 1.x.");
                mOpenGlSurfaceViewInstance.setEGLContextClientVersion(1);
            }
        } else {
            Log.e(TAG, "onResume(): Only OpenGL ES 1.x is supported");
            if (renderer instanceof ARRendererGLES20)
                throw new RuntimeException("Only OpenGL 1.x available but a OpenGL 2.x renderer was provided.");
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            mOpenGlSurfaceViewInstance.setEGLContextClientVersion(1);
        }

        mOpenGlSurfaceViewInstance.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mOpenGlSurfaceViewInstance.getHolder().setFormat(PixelFormat.TRANSLUCENT); // Needs to be a translucent surface so the camera preview shows through.
        mOpenGlSurfaceViewInstance.setRenderer(renderer);
        mOpenGlSurfaceViewInstance.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // Only render when we have a frame (must call requestRender()).
        mOpenGlSurfaceViewInstance.setZOrderMediaOverlay(true); // Request that GL view's SurfaceView be on top of other SurfaceViews (including CameraPreview's SurfaceView).

        Log.e(TAG, "onResume(): GLSurfaceView created");

        // Add the views to the interface
        mainFrameLayout.addView(preview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mainFrameLayout.addView(mOpenGlSurfaceViewInstance, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Log.e(TAG, "onResume(): Views added to main layout.");

        if (mOpenGlSurfaceViewInstance != null) {
            mOpenGlSurfaceViewInstance.onResume();
        }

        //Load Augment Splash Screen
        AugmentScreenLayout = this.getLayoutInflater().inflate(R.layout.augment_screen, mainFrameLayout, false);
        mainFrameLayout.addView(AugmentScreenLayout);
        arTimer = AugmentScreenLayout.findViewById(R.id.timer_text);
        arProgressText = AugmentScreenLayout.findViewById(R.id.ar_progress_text);

        //Load Capture Options buttons
        OptionsButtonLayout = this.getLayoutInflater().inflate(R.layout.options_buttons_layout, mainFrameLayout, false);
        mScreenshotButton = OptionsButtonLayout.findViewById(R.id.button_screenshot);
        mScreenshotButton.setOnClickListener(this);

        //Load Camera Options button
        CameraOptionsButtonLayout = this.getLayoutInflater().inflate(R.layout.cam_options_button_layout, mainFrameLayout, false);
        mOptionsButton = CameraOptionsButtonLayout.findViewById(R.id.button_options);
        mOptionsButton.setOnClickListener(this);

        mFlashButton = CameraOptionsButtonLayout.findViewById(R.id.button_flash);
        mAutoFocusButton = CameraOptionsButtonLayout.findViewById(R.id.button_auto_focus);
        mAutoSceneButton = CameraOptionsButtonLayout.findViewById(R.id.button_auto_scene);
        mContinuousPictureButton = CameraOptionsButtonLayout.findViewById(R.id.button_continuous_scene_focus);
        mHdrButton = CameraOptionsButtonLayout.findViewById(R.id.button_hdr);
        mWhiteBalanceButton = CameraOptionsButtonLayout.findViewById(R.id.button_white_balance);
        mSteadyShotButton = CameraOptionsButtonLayout.findViewById(R.id.button_steady_on);
        mFlashButton = CameraOptionsButtonLayout.findViewById(R.id.button_flash);

        mFlashButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_flash_area);
        mAutoFocusButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_auto_focus_area);
        mAutoSceneButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_auto_scene_area);
        mContinuousPictureButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_continuous_scene_focus_area);
        mHdrButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_hdr_area);
        mWhiteBalanceButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_white_balance_area);
        mSteadyShotButtonArea = CameraOptionsButtonLayout.findViewById(R.id.button_steady_on_area);

        mFlashButton.setOnClickListener(this);
        mHdrButton.setOnClickListener(this);
        mAutoSceneButton.setOnClickListener(this);
        mWhiteBalanceButton.setOnClickListener(this);
        mContinuousPictureButton.setOnClickListener(this);
        mAutoFocusButton.setOnClickListener(this);
        mSteadyShotButton.setOnClickListener(this);

        mHdrButtonArea.setOnClickListener(this);
        mAutoSceneButtonArea.setOnClickListener(this);
        mWhiteBalanceButtonArea.setOnClickListener(this);
        mContinuousPictureButtonArea.setOnClickListener(this);
        mAutoFocusButtonArea.setOnClickListener(this);
        mSteadyShotButtonArea.setOnClickListener(this);
        mFlashButtonArea.setOnClickListener(this);

        mHdrButtonArea.setVisibility(View.GONE);
        mAutoSceneButtonArea.setVisibility(View.GONE);
        mWhiteBalanceButtonArea.setVisibility(View.GONE);
        mContinuousPictureButtonArea.setVisibility(View.GONE);
        mAutoFocusButtonArea.setVisibility(View.GONE);
        mSteadyShotButtonArea.setVisibility(View.GONE);
        mFlashButtonArea.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        //Log.i(TAG, "onPause()");
        super.onPause();

        if (mOpenGlSurfaceViewInstance != null) {
            mOpenGlSurfaceViewInstance.onPause();
        }

        // System hardware must be released in onPause(), so it's available to
        // any incoming activity. Removing the CameraPreview will do this for the
        // camera. Also do it for the GLSurfaceView, since it serves no purpose
        // with the camera preview gone.
        mainFrameLayout.removeView(mOpenGlSurfaceViewInstance);
        mainFrameLayout.removeView(preview);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop(): Activity stopping.");
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mOptionsButton)) {

//            Toast.makeText(this, "Options are used for development purpose. \n Your current options are \n Resolution : 1280x720 \n Aspect Ratio : 16:9 ", Toast.LENGTH_SHORT).show();
//            v.getContext().startActivity(new Intent(v.getContext(), CameraPreferencesActivity.class));

            if (camera_options_visibility) {
                mHdrButtonArea.setVisibility(View.GONE);
                mAutoSceneButtonArea.setVisibility(View.GONE);
                mWhiteBalanceButtonArea.setVisibility(View.GONE);
                mContinuousPictureButtonArea.setVisibility(View.GONE);
                mAutoFocusButtonArea.setVisibility(View.GONE);
                mSteadyShotButtonArea.setVisibility(View.GONE);
                mFlashButtonArea.setVisibility(View.GONE);
                camera_options_visibility = false;

            } else {
                mHdrButtonArea.setVisibility(View.VISIBLE);
                mAutoSceneButtonArea.setVisibility(View.VISIBLE);
                mWhiteBalanceButtonArea.setVisibility(View.VISIBLE);
                mContinuousPictureButtonArea.setVisibility(View.VISIBLE);
                mAutoFocusButtonArea.setVisibility(View.VISIBLE);
                mSteadyShotButtonArea.setVisibility(View.VISIBLE);
                mFlashButtonArea.setVisibility(View.VISIBLE);
                camera_options_visibility = true;
            }
        }

        if (v.equals(mFlashButton)) {
            if (preview.camera != null) {
                try {
                    Camera.Parameters param = preview.camera.getParameters();
                    param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH
                            : Camera.Parameters.FLASH_MODE_OFF);
                    preview.camera.setParameters(param);
                    flashmode = !flashmode;
                    Toast toast = Toast.makeText(this, "Flash Activated", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();

                } catch (Exception ignored) {
                }
            }
        }

        if (v.equals(mScreenshotButton)) {
            CameraImage();
        }

        if (v.equals(mAutoFocusButton)) {
            if (autofocus_toggle) {
                EnvconstantsAR.AUTOFOCUS = false;
                preview.setAutoFocus();
                autofocus_toggle = false;
                mAutoFocusButton.setImageResource(R.mipmap.ic_auto_focus_on);
            } else {
                EnvconstantsAR.AUTOFOCUS = true;
                int response = preview.setAutoFocus();
                if (response == 1) {
                    autofocus_toggle = true;
                    mAutoFocusButton.setImageResource(R.mipmap.ic_auto_focus_off);
                }
            }
        }

        if (v.equals(mAutoSceneButton)) {
            if (autoscene_toggle) {
                EnvconstantsAR.AUTOSCENE = false;
                preview.setAutoScene();
                autoscene_toggle = false;
                mAutoSceneButton.setImageResource(R.mipmap.ic_auto_scene_off);
            } else {
                EnvconstantsAR.AUTOSCENE = true;
                int response = preview.setAutoScene();
                if (response == 1) {
                    autoscene_toggle = true;
                    mAutoSceneButton.setImageResource(R.mipmap.ic_auto_scene_on);
                }
            }
        }

        if (v.equals(mSteadyShotButton)) {
            if (steady_toggle) {
                EnvconstantsAR.STEADYSHOT = false;
                preview.setSteadyShot();
                steady_toggle = false;
                mSteadyShotButton.setImageResource(R.mipmap.ic_steady_off);
            } else {
                EnvconstantsAR.STEADYSHOT = true;
                int response = preview.setSteadyShot();
                if (response == 1) {
                    steady_toggle = true;
                    mSteadyShotButton.setImageResource(R.mipmap.ic_steady_on);
                }
            }
        }

        if (v.equals(mContinuousPictureButton)) {
            if (continouspicture_toggle) {
                EnvconstantsAR.CONTINOUSPICTURE = false;
                preview.SetContinousPicture();
                continouspicture_toggle = false;
                mContinuousPictureButton.setImageResource(R.mipmap.ic_focus_continuous_picture_on);
            } else {
                int response;
                EnvconstantsAR.CONTINOUSPICTURE = true;
                response = preview.SetContinousPicture();
                if (response == 1) {
                    continouspicture_toggle = true;
                    mContinuousPictureButton.setImageResource(R.mipmap.ic_focus_continuous_picture_off);
                }
            }
        }

        if (v.equals(mHdrButton)) {
            if (hdr_toggle) {
                EnvconstantsAR.HDR = false;
                preview.setHDR();
                hdr_toggle = false;
                mHdrButton.setImageResource(R.mipmap.ic_hdr_on);
            } else {
                int response;
                EnvconstantsAR.HDR = true;
                response = preview.setHDR();
                if (response == 1) {
                    hdr_toggle = true;
                    mHdrButton.setImageResource(R.mipmap.ic_hdr_off);
                }
            }
        }

        if (v.equals(mWhiteBalanceButton)) {
            if (whitebalance_toggle) {
                EnvconstantsAR.WHITEBALANCE = false;
                preview.setWhiteBalance();
                whitebalance_toggle = false;
                mWhiteBalanceButton.setImageResource(R.mipmap.ic_auto_white_balance);
            } else {
                int response;
                EnvconstantsAR.WHITEBALANCE = true;
                response = preview.setWhiteBalance();
                if (response == 1) {
                    whitebalance_toggle = true;
                    mWhiteBalanceButton.setImageResource(R.mipmap.ic_auto_balance_off);
                }
            }
        }
    }

    /**
     * Captures the camera preview frame that is providing the video frames and saves it to the screenshots
     */
    private void CameraImage() {

        preview.camera.takePicture(null, null, new Camera.PictureCallback() {

            private File imageFile;

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {

                    String cPath = Environment.getExternalStorageDirectory() + "/L_CATALOG/Screenshots";
                    // convert byte array into bitmap
                    Bitmap loadedImage;
//                    Bitmap rotatedBitmap = null;
                    Bitmap AugmentImage;
                    renderer.printOptionEnable = true;
                    loadedImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                    AugmentImage = renderer.getReturnbitmap();
                    Bitmap result = Bitmap.createBitmap(loadedImage.getWidth() + 10, loadedImage.getHeight() + 10, Bitmap.Config.RGB_565);

//                    if (result != null && !result.isRecycled()) {
//                        result.recycle();
//                        result = null;
//                    }
//                    assert result != null;
                    Canvas canvas = new Canvas(result);
                    canvas.drawBitmap(loadedImage, 0f, 0f, null);
                    //  canvas.drawBitmap(AugmentImage, 0f, 0f, null);
                    canvas.drawBitmap(AugmentImage, null, new RectF(0, 0, loadedImage.getWidth(), loadedImage.getHeight()), null);

                    // rotate Image
                    Matrix rotateMatrix = new Matrix();
                    rotateMatrix.postRotate(preview.rotation);
//                    rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight(), rotateMatrix, false);

                    File folder;
                    if (Environment.getExternalStorageState().contains(Environment.MEDIA_MOUNTED)) {
                        folder = new File(cPath);
                    } else {
                        folder = new File(cPath);
                    }
                    boolean success = true;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    if (success) {
                        Date now = new Date();
                        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
                        String ImageFileName = folder.getAbsolutePath() + File.separator + now + ".jpg";
                        Log.i(TAG, "ScreenShotFileName : " + ImageFileName);
                        imageFile = new File(ImageFileName);
                        imageFile.createNewFile();

                        Toast toast;
                        toast = Toast.makeText(mContext, "Image Saved to your gallery", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    } else {

                        Toast toast;
                        toast = Toast.makeText(mContext, "Image Not Captured and Saved", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();

                        return;
                    }
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                    /*save image into gallery*/
                    result.compress(Bitmap.CompressFormat.JPEG, 100, ostream);

                    FileOutputStream fout = new FileOutputStream(imageFile);
                    fout.write(ostream.toByteArray());
                    fout.close();
                    ContentValues values = new ContentValues();

                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());

                    mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.startPreview();
            }
        });
    }

    /**
     * Returns the camera preview that is providing the video frames.
     */
    public CaptureCameraPreview getCameraPreview() {
        return preview;
    }

    /**
     * Returns the GL surface view.
     */
    public GLSurfaceView getGLView() {
        return mOpenGlSurfaceViewInstance;
    }

    @Override
    public void cameraPreviewStarted(int width, int height, int rate, int cameraIndex, boolean cameraIsFrontFacing) {

        if (ARToolKit.getInstance().initialiseAR(width, height, "/storage/emulated/0/L_CATALOG/cache/Data/camera_para.dat", cameraIndex, cameraIsFrontFacing)) {
            // Expects Data to be already in the cache dir. This can be done with the AssetUnpacker.

            startTimer(130000);
            Log.e(TAG, "getGLView(): Camera initialised");

        } else {
            // Error
            Log.e(TAG, "getGLView(): Error initialising camera. Cannot continue.");
            finish();
        }

        Log.e(TAG, "Camera settings: " + width + "x" + height + "@" + rate + "fps");

        firstUpdate = true;
    }

    public void startTimer(final long finish) {
        new CountDownTimer(finish, 1000) {

            public void onTick(long millisUntilFinished) {
                long remainedSecs = millisUntilFinished / 1000;
                arTimer.setText(("Estimated TIME LEFT : " + remainedSecs / 60) + " Min : " + (remainedSecs % 60) + " Secs");
            }

            public void onFinish() {
                arProgressText.setText("Camera configured");
                mainFrameLayout.removeView(AugmentScreenLayout);
                mainFrameLayout.addView(OptionsButtonLayout);
                mainFrameLayout.addView(CameraOptionsButtonLayout);
                cancel();
            }
        }.start();
    }

    @Override
    public void cameraPreviewFrame(byte[] frame) {

        if (firstUpdate) {
            // ARToolKit has been initialised. The renderer can now add markers, etc...
            if (renderer.configureARScene()) {

                Log.e(TAG, "cameraPreviewFrame(): Scene configured successfully");

            } else {
                // Error
                Log.e(TAG, "cameraPreviewFrame(): Error configuring scene. Cannot continue.");
                finish();
            }
            firstUpdate = false;
        }

        if (ARToolKit.getInstance().convertAndDetect(frame)) {

            // Update the renderer as the frame has changed
            if (mOpenGlSurfaceViewInstance != null)
                mOpenGlSurfaceViewInstance.requestRender();

            onFrameProcessed();
        }
    }

    public void onFrameProcessed() {
    }

    @Override
    public void cameraPreviewStopped() {
        ARToolKit.getInstance().cleanup();
    }
}
