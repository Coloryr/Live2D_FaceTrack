package com.coloryr.facetrack.track.arcore

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Size
import com.coloryr.facetrack.MainActivity
import com.coloryr.facetrack.live2d.TrackSave
import com.coloryr.facetrack.track.IAR
import com.coloryr.facetrack.track.ar.BackgroundRenderer
import com.coloryr.facetrack.track.ar.CameraPermissionHelper
import com.coloryr.facetrack.track.ar.DisplayRotationHelper
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import java.io.IOException
import java.util.*

class ArCoreTest(private var activity: Activity) : IAR {
    private var session: Session? = null
    private val backgroundRenderer = BackgroundRenderer()
    private val displayRotationHelper: DisplayRotationHelper = DisplayRotationHelper(activity)
    private val context: Context = activity.applicationContext
    private var installRequested = false

    override fun onSurfaceCreated() {
        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onPause() {
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause()
            session!!.pause()
        }
    }

    override fun onResume(): Boolean {
        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return false
                    }
                    InstallStatus.INSTALLED -> {}
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity)
                    return false
                }

                // Create the session and configure it to use a front-facing (selfie) camera.
                session = Session(
                    activity, EnumSet.noneOf(
                        Session.Feature::class.java
                    )
                )
                val cameraConfigFilter = CameraConfigFilter(session)
                cameraConfigFilter.facingDirection = CameraConfig.FacingDirection.FRONT
                val cameraConfigs = session!!.getSupportedCameraConfigs(cameraConfigFilter)
                if (cameraConfigs.isNotEmpty()) {
                    // Element 0 contains the camera config that best matches the session feature
                    // and filter settings.
                    session!!.cameraConfig = cameraConfigs[0]

                } else {
                    message = "This device does not have a front-facing (selfie) camera"
                    exception = UnavailableDeviceNotCompatibleException(message)
                }
                configureSession()
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }
            if (message != null) {
                Log.e(TAG, "Exception creating session", exception)
                return false
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            session = null
            return false
        }
        displayRotationHelper.onResume()
        return true
    }

    override fun onDrawFrame() {
        if (session == null) {
            return
        }
        displayRotationHelper.updateSessionIfNeeded(session!!)
        try {
            session!!.setCameraTextureName(backgroundRenderer.textureId)
            val frame = session!!.update()
            val faces = session!!.getAllTrackables(
                AugmentedFace::class.java
            )
            for (face in faces) {
                if (face.trackingState != TrackingState.TRACKING) {
                    break
                }
                val modelMatrix = FloatArray(16)
                face.centerPose.toMatrix(modelMatrix, 0)
                val post = face.centerPose
                val x = post.qx()
                val y = post.qy()
                val z = post.qz()
                val x1 = post.tx()
                val y1 = post.ty()
                val z1 = post.tz()
                TrackSave.BodyZ = -x1 * 30
                TrackSave.BodyY = y1 * 30
                TrackSave.AngleY = -x * 200
                TrackSave.AngleX = -y * 200
                TrackSave.AngleZ = z * 200
            }
            val image = frame.acquireCameraImage()
            MainActivity.eye.onCameraFrame(image)
            image.close()
        } catch (ignored: NotYetAvailableException) {
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    private fun configureSession() {
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        session!!.configure(config)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}