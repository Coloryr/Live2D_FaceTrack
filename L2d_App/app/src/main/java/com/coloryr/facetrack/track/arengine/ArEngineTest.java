package com.coloryr.facetrack.track.arengine;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.util.Log;
import com.coloryr.facetrack.MainActivity;
import com.coloryr.facetrack.live2d.TrackSave;
import com.coloryr.facetrack.track.IAR;
import com.coloryr.facetrack.track.ar.BackgroundRenderer;
import com.coloryr.facetrack.track.ar.CameraPermissionHelper;
import com.coloryr.facetrack.track.ar.DisplayRotationHelper;
import com.coloryr.facetrack.track.ar.SnackbarHelper;
import com.huawei.hiar.*;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class ArEngineTest implements IAR {
    private ARSession session;
    private ARConfigBase mArConfig;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private static final String TAG = MainActivity.class.getSimpleName();
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final DisplayRotationHelper displayRotationHelper;
    private final Context context;
    private final Activity activity;
    private boolean installRequested;

    public ArEngineTest(Activity context){
        this.activity = context;
        this.context  = context;
        displayRotationHelper = new DisplayRotationHelper(context);
    }

    public void onSurfaceCreated() {
        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    public void onPause() {
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            session.pause();
        }
    }

    public boolean onResume() {
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                if(!AREnginesApk.isAREngineApkReady(context))
                    return false;

                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity);
                    return false;
                }

                // Create the session and configure it to use a front-facing (selfie) camera.
                session = new ARSession(context);
                mArConfig = new ARFaceTrackingConfig(session);
                mArConfig.setLightingMode(ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING);
                mArConfig.setPowerMode(ARConfigBase.PowerMode.POWER_SAVING);
                session.configure(mArConfig);

            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(activity, message);
                Log.e(TAG, "Exception creating session", exception);
                return false;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (Exception e) {
            messageSnackbarHelper.showError(activity, "Camera not available. Try restarting the app.");
            session = null;
            return false;
        }

        displayRotationHelper.onResume();

        return true;
    }

    public void onDrawFrame() {
        if (session == null) {
            return;
        }

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            ARFrame frame = session.update();

            Collection<ARFace> faces = session.getAllTrackables(ARFace.class);
            for (ARFace face : faces) {
                if (face.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                    break;
                }

                ARPose pose = face.getPose();
                float x = pose.qx();
                float y = pose.qy();
                float z = pose.qz();

                float x1 = pose.tx();
                float y1 = pose.ty();
                float z1 = pose.tz();

                TrackSave.BodyZ = -x1 * 30;
                TrackSave.BodyY = y1 * 30;

                TrackSave.AngleY = -x * 200;
                TrackSave.AngleX = -y * 200;
                TrackSave.AngleZ = z * 200;
            }
            Image image = frame.acquireCameraImage();
            MainActivity.eye.onCameraFrame(image);
            image.close();
        } catch (Exception ignored) {

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    public void onSurfaceChanged(int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
    }
}
