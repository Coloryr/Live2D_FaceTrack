package com.coloryr.facetrack.track.arengine

import android.app.Activity
import android.content.Context
import android.util.Log
import com.coloryr.facetrack.MainActivity
import com.coloryr.facetrack.live2d.TrackSave
import com.coloryr.facetrack.track.IAR
import com.coloryr.facetrack.track.ar.BackgroundRenderer
import com.coloryr.facetrack.track.ar.CameraPermissionHelper
import com.coloryr.facetrack.track.ar.DisplayRotationHelper
import com.coloryr.facetrack.track.ar.SnackbarHelper
import com.huawei.hiar.*
import java.io.IOException

class ArEngineTest(private val activity: Activity) : IAR {
    private var session: ARSession? = null
    private lateinit var mArConfig: ARConfigBase
    private val messageSnackbarHelper = SnackbarHelper()
    private val backgroundRenderer = BackgroundRenderer()
    private val displayRotationHelper: DisplayRotationHelper
    private val context: Context

    init {
        displayRotationHelper = DisplayRotationHelper(activity)
        context = activity.applicationContext
    }

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
                if (!AREnginesApk.isAREngineApkReady(activity)) return false
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity)
                    return false
                }

                // Create the session and configure it to use a front-facing (selfie) camera.
                session = ARSession(activity)
                mArConfig = ARFaceTrackingConfig(session)
                mArConfig.setLightingMode(ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING)
                mArConfig.setPowerMode(ARConfigBase.PowerMode.POWER_SAVING)
                session!!.configure(mArConfig)
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }
            if (message != null) {
                messageSnackbarHelper.showError(activity, message)
                Log.e(TAG, "Exception creating session", exception)
                return false
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
        } catch (e: Exception) {
            messageSnackbarHelper.showError(activity, "Camera not available. Try restarting the app.")
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
        try {
            session!!.setCameraTextureName(backgroundRenderer.textureId)
            val frame = session!!.update()
            val faces = session!!.getAllTrackables(ARFace::class.java)
            for (face in faces) {
                if (face.trackingState != ARTrackable.TrackingState.TRACKING) {
                    break
                }
                val pose = face.pose
                val x = pose.qx()
                val y = pose.qy()
                val z = pose.qz()
                val x1 = pose.tx()
                val y1 = pose.ty()
                val z1 = pose.tz()
                TrackSave.BodyZ = -x1 * 30
                TrackSave.BodyY = y1 * 30
                TrackSave.AngleY = -x * 200
                TrackSave.AngleX = -y * 200
                TrackSave.AngleZ = z * 200
            }
            val image = frame.acquireCameraImage()
            MainActivity.Companion.eye!!.onCameraFrame(image)
            image.close()
        } catch (ignored: Exception) {
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}