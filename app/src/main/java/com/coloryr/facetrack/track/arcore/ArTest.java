package com.coloryr.facetrack.track.arcore;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.util.Log;
import com.coloryr.facetrack.live2d.TrackSave;
import com.google.ar.core.*;
import com.google.ar.core.exceptions.*;
import com.coloryr.facetrack.*;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class ArTest {
    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private static final String TAG = MainActivity.class.getSimpleName();
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final DisplayRotationHelper displayRotationHelper;
    private final Context context;
    private final Activity activity;
    private boolean installRequested;

    public ArTest(Activity context){
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

    public void onResume() {
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity);
                    return;
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
                messageSnackbarHelper.showError(activity, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(activity, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        displayRotationHelper.onResume();
    }

    public void onDrawFrame() {
        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Get projection matrix.
            float[] projectionMatrix = new float[16];
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewMatrix = new float[16];
            camera.getViewMatrix(viewMatrix, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // If frame is ready, render camera preview image to the GL surface.
            //backgroundRenderer.draw(frame);

            // ARCore's face detection works best on upright faces, relative to gravity.
            // If the device cannot determine a screen side aligned with gravity, face
            // detection may not work optimally.
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

                TrackSave.y = -x * 200;
                TrackSave.x = -y * 200;
                TrackSave.z = z * 200;
            }

            Image image = frame.acquireCameraImage();
            MainActivity.eye.onCameraFrame(image);
            image.close();
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
