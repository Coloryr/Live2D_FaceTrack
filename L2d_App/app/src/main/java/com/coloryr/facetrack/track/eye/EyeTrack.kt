package com.coloryr.facetrack.track.eye

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.coloryr.facetrack.live2d.TrackSave
import com.google.android.renderscript.Toolkit.yuvToRgbBitmap
import com.google.android.renderscript.YuvFormat
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import java.nio.ByteBuffer

class EyeTrack(private val context: Context) {
    private var facemesh: FaceMesh? = null
    private fun imageToByteBuffer(image: Image): ByteBuffer {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val rowData = ByteArray(planes[0].rowStride)
        val bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
        val output = ByteBuffer.allocateDirect(bufferSize)
        var channelOffset = 0
        var outputStride = 0
        for (planeIndex in 0..2) {
            if (planeIndex == 0) {
                channelOffset = 0
                outputStride = 1
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1
                outputStride = 2
            } else if (planeIndex == 2) {
                channelOffset = width * height
                outputStride = 2
            }
            val buffer = planes[planeIndex].buffer
            val rowStride = planes[planeIndex].rowStride
            val pixelStride = planes[planeIndex].pixelStride
            val shift = if (planeIndex == 0) 0 else 1
            val widthShifted = width shr shift
            val heightShifted = height shr shift
            buffer.position(rowStride * (0 shr shift) + pixelStride * (0 shr shift))
            for (row in 0 until heightShifted) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted
                    buffer[output.array(), channelOffset, length]
                    channelOffset += length
                } else {
                    length = (widthShifted - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until widthShifted) {
                        output.array()[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return output
    }

    fun getBitmapFromImage(image: Image): Bitmap {
        val yuvBytes = imageToByteBuffer(image)
        return yuvToRgbBitmap(yuvBytes.array(), image.width, image.height, YuvFormat.NV21)
    }

    fun init() {
        facemesh = FaceMesh(
            context,
            FaceMeshOptions.builder()
                .setStaticImageMode(true)
                .setRefineLandmarks(true)
                .setMinDetectionConfidence(0.8f)
                .setRunOnGpu(true)
                .build()
        )
        facemesh!!.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG,
                "MediaPipe Face Mesh error:$message"
            )
        }
        facemesh!!.setResultListener { result: FaceMeshResult -> this.run(result) }
    }

    fun run(result: FaceMeshResult) {
        val numFaces = result.multiFaceLandmarks().size
        if (numFaces == 0) return
        val list = result.multiFaceLandmarks()[0]
        val p13 = list.getLandmark(13)
        val p14 = list.getLandmark(14)
        var mo = dis(p13.x.toDouble(), p13.y.toDouble(), p14.x.toDouble(), p14.y.toDouble())
        if (mo < 0.001) {
            mo = 0.0
        }
        mo *= 20.0
        TrackSave.MouthOpenY = mo.toFloat()
        val p385 = list.getLandmark(385)
        val p386 = list.getLandmark(386)
        val p374 = list.getLandmark(374)
        val p362 = list.getLandmark(362)
        val p263 = list.getLandmark(263)
        val qx = ((p385.x + p386.x) / 2).toDouble()
        val qy = ((p385.y + p386.y) / 2).toDouble()
        val lo1 = dis(qx, qy, p374.x.toDouble(), p374.y.toDouble())
        val lo2 = dis(p362.x.toDouble(), p362.y.toDouble(), p263.x.toDouble(), p374.y.toDouble())
        var lo = lo1 / lo2
        lo = lo - 0.04
        lo = lo / 0.16
        lo = lo - 0.1
        if (lo > 1) {
            lo = 1.0
        } else if (lo < 0) lo = 0.0
        TrackSave.EyeLOpen = lo.toFloat() - 1
        val p159 = list.getLandmark(159)
        val p158 = list.getLandmark(158)
        val p145 = list.getLandmark(145)
        val p33 = list.getLandmark(33)
        val p133 = list.getLandmark(133)
        val qx1 = ((p159.x + p158.x) / 2).toDouble()
        val qy1 = ((p159.y + p158.y) / 2).toDouble()
        val ro1 = dis(qx1, qy1, p145.x.toDouble(), p145.y.toDouble())
        val ro2 = dis(p33.x.toDouble(), p33.y.toDouble(), p133.x.toDouble(), p133.y.toDouble())
        var ro = ro1 / ro2
        ro = ro - 0.04
        ro = ro / 0.16
        ro = ro - 0.05
        if (ro > 1) {
            ro = 1.0
        } else if (ro < 0) ro = 0.0
        TrackSave.EyeROpen = ro.toFloat() - 1
        val p474 = list.getLandmark(474)
        val p475 = list.getLandmark(475)
        val p476 = list.getLandmark(476)
        val p477 = list.getLandmark(477)
        val lx1 = ((p474.x + p476.x) / 2).toDouble()
        val ly1 = ((p475.y + p477.y) / 2).toDouble()
        val p469 = list.getLandmark(469)
        val p470 = list.getLandmark(470)
        val p471 = list.getLandmark(471)
        val p472 = list.getLandmark(472)
        val rx1 = ((p469.x + p471.x) / 2).toDouble()
        val ry1 = ((p470.y + p472.y) / 2).toDouble()
        val ex = (lx1 + rx1) / 2
        val ey = (ly1 + ry1) / 2
        val ex1 = ((p263.x + p133.x) / 2).toDouble()
        val ey1 = ((p263.y + p133.y) / 2).toDouble()
        val ex2 = ((p362.x + p33.x) / 2).toDouble()
        val ey2 = ((p362.y + p33.y) / 2).toDouble()
        val dx1 = dis(ex, ey, ex1, ey1)
        val dx2 = dis(ex, ey, ex2, ey2)
        var dx = dx1 / dx2
        dx -= 1.0
        if (dx < 0) {
            dx *= 2.0
        } else {
            dx /= 2.0
        }
        TrackSave.EyeBallX = -dx.toFloat()
        val ex3 = (qx + qx1) / 2
        val ey3 = (qy + qy1) / 2
        val ex4 = ((p145.x + p374.x) / 2).toDouble()
        val ey4 = ((p145.y + p374.y) / 2).toDouble()
        val dy1 = dis(ex, ey, ex3, ey3)
        val dy2 = dis(ex, ey, ex4, ey4)
        var dy = dy1 / dy2
        dy *= 2.0
        dy -= 1.5
        TrackSave.EyeBallY = dy.toFloat()
    }

    private fun dis(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val temp = x2 - x1
        val temp1 = y2 - y1
        return Math.sqrt(temp * temp + temp1 * temp1)
    }

    fun onCameraFrame(image: Image) {
//        if (test == null) {
//            test = FaceTrack(context)
//        }
        var bitmap = getBitmapFromImage(image)
        bitmap = rotateBitmap(bitmap, ExifInterface.ORIENTATION_ROTATE_270)
        facemesh!!.send(bitmap)
        //test!!.tick(bitmap)
    }

    companion object {
        private const val TAG = "eye"
        private fun rotateBitmap(inputBitmap: Bitmap, orientation: Int): Bitmap {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> matrix.postRotate(0f)
            }
            return Bitmap.createBitmap(inputBitmap, 0, 0, inputBitmap.width, inputBitmap.height, matrix, true)
        }
    }
}