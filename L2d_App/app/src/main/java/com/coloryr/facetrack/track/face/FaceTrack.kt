package com.coloryr.facetrack.track.face

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.google.mediapipe.modules.facegeometry.FaceGeometryProto.FaceGeometry

class FaceTrack(context: Context) {
    protected var processor: FrameProcessor
    private val eglManager: EglManager
    private var applicationInfo: ApplicationInfo? = null
    private val effectSelectionLock = Any()
    private val selectedEffectId = 0
    private val converter: ExternalTextureConverter

    init {
        try {
            applicationInfo =
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Cannot find application info: $e")
        }
        eglManager = EglManager(null)
        //AndroidAssetUtil.initializeNativeAssetManager(context);
        processor = FrameProcessor(
            context,
            eglManager.nativeContext,
            "IMAGE_SIZE:image_size",
            applicationInfo!!.metaData.getString("inputVideoStreamName"),
            applicationInfo!!.metaData.getString("outputVideoStreamName")
        )
        converter = ExternalTextureConverter(
            eglManager.context,
            applicationInfo!!.metaData.getInt("converterNumBuffers", NUM_BUFFERS)
        )
        converter.setFlipY(
            applicationInfo!!.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY)
        )
        converter.setConsumer(processor)

        // Pass the USE_FACE_DETECTION_INPUT_SOURCE flag value as an input side packet into the graph.
        val inputSidePackets: MutableMap<String, Packet> = HashMap()
        inputSidePackets[USE_FACE_DETECTION_INPUT_SOURCE_INPUT_SIDE_PACKET_NAME] = processor.packetCreator.createBool(
            USE_FACE_DETECTION_INPUT_SOURCE
        )
        processor.setInputSidePackets(inputSidePackets)

        // This callback demonstrates how the output face geometry packet can be obtained and used
        // in an Android app. As an example, the Z-translation component of the face pose transform
        // matrix is logged for each face being equal to the approximate distance away from the camera
        // in centimeters.
        processor.addPacketCallback(
            OUTPUT_FACE_GEOMETRY_STREAM_NAME
        ) { packet: Packet ->
            Log.d(TAG, "Received a multi face geometry packet.")
            val multiFaceGeometry = PacketGetter.getProtoVector(packet, FaceGeometry.parser())
            val approxDistanceAwayFromCameraLogMessage = StringBuilder()
            for (faceGeometry in multiFaceGeometry) {
                if (approxDistanceAwayFromCameraLogMessage.length > 0) {
                    approxDistanceAwayFromCameraLogMessage.append(' ')
                }
                val poseTransformMatrix = faceGeometry.poseTransformMatrix
                approxDistanceAwayFromCameraLogMessage.append(
                    -poseTransformMatrix.getPackedData(MATRIX_TRANSLATION_Z_INDEX)
                )
            }
            Log.d(
                TAG,
                "[TS:"
                        + packet.timestamp
                        + "] size = "
                        + multiFaceGeometry.size
                        + "; approx. distance away from camera in cm for faces = ["
                        + approxDistanceAwayFromCameraLogMessage
                        + "]"
            )
        }

        // Alongside the input camera frame, we also send the `selected_effect_id` int32 packet to
        // indicate which effect should be rendered on this frame.
        processor.setOnWillAddFrameListener { timestamp: Long ->
            var selectedEffectIdPacket: Packet? = null
            try {
                synchronized(effectSelectionLock) {
                    selectedEffectIdPacket = processor.packetCreator.createInt32(selectedEffectId)
                }
                processor
                    .graph
                    .addPacketToInputStream(
                        SELECTED_EFFECT_ID_INPUT_STREAM_NAME, selectedEffectIdPacket, timestamp
                    )
            } catch (e: RuntimeException) {
                Log.e(
                    TAG, "Exception while adding packet to input stream while switching effects: $e"
                )
            } finally {
                if (selectedEffectIdPacket != null) {
                    selectedEffectIdPacket!!.release()
                }
            }
        }
    }

    fun tick(bitmap: Bitmap?) {
        processor.onNewFrame(bitmap, 0)
    }

    companion object {
        private const val TAG = "FaceTrack"

        // Flips the camera-preview frames vertically by default, before sending them into FrameProcessor
        // to be processed in a MediaPipe graph, and flips the processed frames back when they are
        // displayed. This maybe needed because OpenGL represents images assuming the image origin is at
        // the bottom-left corner, whereas MediaPipe in general assumes the image origin is at the
        // top-left corner.
        // NOTE: use "flipFramesVertically" in manifest metadata to override this behavior.
        private const val FLIP_FRAMES_VERTICALLY = true

        // Number of output frames allocated in ExternalTextureConverter.
        // NOTE: use "converterNumBuffers" in manifest metadata to override number of buffers. For
        // example, when there is a FlowLimiterCalculator in the graph, number of buffers should be at
        // least `max_in_flight + max_in_queue + 1` (where max_in_flight and max_in_queue are used in
        // FlowLimiterCalculator options). That's because we need buffers for all the frames that are in
        // flight/queue plus one for the next frame from the camera.
        private const val NUM_BUFFERS = 2

        // Side packet / stream names.
        private const val USE_FACE_DETECTION_INPUT_SOURCE_INPUT_SIDE_PACKET_NAME = "use_face_detection_input_source"
        private const val SELECTED_EFFECT_ID_INPUT_STREAM_NAME = "selected_effect_id"
        private const val OUTPUT_FACE_GEOMETRY_STREAM_NAME = "multi_face_geometry"
        private const val EFFECT_SWITCHING_HINT_TEXT = "Tap to switch between effects!"
        private const val USE_FACE_DETECTION_INPUT_SOURCE = false
        private const val MATRIX_TRANSLATION_Z_INDEX = 14
        private const val SELECTED_EFFECT_ID_AXIS = 0
        private const val SELECTED_EFFECT_ID_FACEPAINT = 1
        private const val SELECTED_EFFECT_ID_GLASSES = 2
    }
}