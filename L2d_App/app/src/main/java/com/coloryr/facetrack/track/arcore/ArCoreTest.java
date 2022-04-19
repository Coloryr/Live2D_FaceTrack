package com.coloryr.facetrack.track.arcore;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.util.Log;
import com.coloryr.facetrack.live2d.TrackSave;
import com.coloryr.facetrack.track.IAR;
import com.coloryr.facetrack.track.ar.BackgroundRenderer;
import com.coloryr.facetrack.track.ar.CameraPermissionHelper;
import com.coloryr.facetrack.track.ar.DisplayRotationHelper;
import com.coloryr.facetrack.track.ar.SnackbarHelper;
import com.google.ar.core.*;
import com.google.ar.core.exceptions.*;
import com.coloryr.facetrack.*;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class ArCoreTest implements IAR {
    private Session session;
    private static final String TAG = MainActivity.class.getSimpleName();
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final DisplayRotationHelper displayRotationHelper;
    private final Context context;
    private final Activity activity;
    private boolean installRequested;

    public ArCoreTest(Activity context){
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
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return false;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity);
                    return false;
                }

                // Create the session and configure it to use a front-facing (selfie) camera.
                session = new Session(context, EnumSet.noneOf(Session.Feature.class));
                CameraConfigFilter cameraConfigFilter = new CameraConfigFilter(session);
                cameraConfigFilter.setFacingDirection(CameraConfig.FacingDirection.FRONT);
                List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);
                if (!cameraConfigs.isEmpty()) {
                    // Element 0 contains the camera config that best matches the session feature
                    // and filter settings.
                    session.setCameraConfig(cameraConfigs.get(0));
                } else {
                    message = "This device does not have a front-facing (selfie) camera";
                    exception = new UnavailableDeviceNotCompatibleException(message);
                }
                configureSession();

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return false;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
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
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            Frame frame = session.update();

            Collection<AugmentedFace> faces = session.getAllTrackables(AugmentedFace.class);
            for (AugmentedFace face : faces) {
                if (face.getTrackingState() != TrackingState.TRACKING) {
                    break;
                }

                float[] modelMatrix = new float[16];
                face.getCenterPose().toMatrix(modelMatrix, 0);

                Pose post = face.getCenterPose();
                float x = post.qx();
                float y = post.qy();
                float z = post.qz();

                float x1 = post.tx();
                float y1 = post.ty();
                float z1 = post.tz();

                TrackSave.BodyZ = -x1 * 30;
                TrackSave.BodyY = y1 * 30;

                TrackSave.AngleY = -x * 200;
                TrackSave.AngleX = -y * 200;
                TrackSave.AngleZ = z * 200;
            }

            Image image = frame.acquireCameraImage();
            MainActivity.eye.onCameraFrame(image);
            image.close();
        } catch (NotYetAvailableException ignored) {

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    public void onSurfaceChanged(int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
    }

    private void configureSession() {
        Config config = new Config(session);
        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
        session.configure(config);
    }
}
