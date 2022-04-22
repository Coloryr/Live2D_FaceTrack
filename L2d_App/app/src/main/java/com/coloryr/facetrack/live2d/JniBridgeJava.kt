/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */
package com.coloryr.facetrack.live2d

import android.annotation.SuppressLint
import android.content.Context
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

@SuppressLint("StaticFieldLeak")
object JniBridgeJava {
    private const val LIBRARY_NAME = "live2d"

    @SuppressLint("StaticFieldLeak")
    private var _context: Context? = null

    init {
        System.loadLibrary(LIBRARY_NAME)
    }

    // Native -----------------------------------------------------------------
    external fun nativeOnStart()
    external fun nativeOnStop()
    external fun nativeOnDestroy()
    external fun nativeOnSurfaceCreated()
    external fun nativeOnSurfaceChanged(width: Int, height: Int)
    external fun nativeOnDrawFrame()
    external fun nativeOnTouchesBegan(pointX: Float, pointY: Float)
    external fun nativeOnTouchesEnded(pointX: Float, pointY: Float)
    external fun nativeOnTouchesMoved(pointX: Float, pointY: Float)
    private external fun nativeStartMotion(group: ByteArray, no: Int, priority: Int)
    private external fun nativeStartExpressions(name: ByteArray)
    external fun nativeGetExpressionSize(): Int
    external fun nativeGetMotionSize(): Int
    external fun nativeGetExpression(index: Int): ByteArray?
    external fun nativeGetMotion(index: Int): ByteArray?
    external fun nativeSetPosX(x: Float)
    external fun nativeSetPosY(y: Float)
    external fun nativeSetPos(x: Float, y: Float)
    external fun nativeSetScale(scale: Float)
    external fun nativeLoadModel(path: ByteArray?, name: ByteArray?)
    external fun nativeEnableRandomMotion(enable: Boolean)
    external fun nativeSetBreath(id: ByteArray?)
    external fun nativeGetCubismParams()
    external fun nativeSetCubismParams(id: ByteArray?, value: Float)
    external fun nativeSetEyeBallX(id: ByteArray?)
    external fun nativeSetEyeBallY(id: ByteArray?)
    external fun nativeSetBodyAngleX(id: ByteArray?)
    external fun nativeSetAngleX(id: ByteArray?)
    external fun nativeSetAngleY(id: ByteArray?)
    external fun nativeSetAngleZ(id: ByteArray?)
    external fun nativeGetParamIds(): Array<String>?
    external fun nativeGetPartValues(): FloatArray
    external fun nativeGetPartIds(): Array<String>?
    external fun nativeGetParamValues(): FloatArray
    external fun nativeGetParamDefaultValues(): FloatArray
    external fun nativeGetParamMinimumValues(): FloatArray
    external fun nativeGetParamMaximumValues(): FloatArray
    external fun nativeSetParamValue(index: ByteArray?, value: Float)

    // Java -----------------------------------------------------------------
    var isLoad = false
        private set
    var name: String? = null
        private set
    private val motions: MutableList<String> = ArrayList()
    private val expressions: MutableList<String> = ArrayList()
    private fun resData() {
        var a = nativeGetMotionSize()
        for (i in 0 until a) {
            val temp = String(nativeGetMotion(i)!!)
            motions.add(temp)
        }
        a = nativeGetExpressionSize()
        for (i in 0 until a) {
            val temp = String(nativeGetExpression(i)!!)
            expressions.add(temp)
        }
    }

    fun getMotions(): List<String> {
        if (motions.size == 0) {
            resData()
        }
        return motions
    }

    fun getExpressions(): List<String> {
        if (expressions.size == 0) {
            resData()
        }
        return expressions
    }

    fun ChangeModel() {
        motions.clear()
        expressions.clear()
    }

    fun StartMotion(name: String, no: Int, priority: Int) {
        if (motions.size == 0) {
            resData()
        }
        if (motions.contains(name + '_' + no)) {
            nativeStartMotion(name.toByteArray(StandardCharsets.UTF_8), no, priority)
        }
    }

    fun StartExpressions(name: String) {
        if (expressions.size == 0) {
            resData()
        }
        if (expressions.contains(name)) {
            nativeStartExpressions(name.toByteArray(StandardCharsets.UTF_8))
        }
    }

    fun SetContext(context: Context?) {
        _context = context
    }

    fun LoadFile(filePath: String?): ByteArray? {
        var fileData: InputStream? = null
        return try {
            fileData = _context!!.assets.open(filePath!!)
            val fileSize = fileData.available()
            val fileBuffer = ByteArray(fileSize)
            fileData.read(fileBuffer, 0, fileSize)
            fileBuffer
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            try {
                fileData?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun LoadModel(path: String, name: String) {
        val path1 = path.toByteArray(StandardCharsets.UTF_8)
        val name1 = name.toByteArray(StandardCharsets.UTF_8)
        nativeLoadModel(path1, name1)
        isLoad = true
    }

    fun onUpdate() {
        nativeSetParamValue("PARAM_ANGLE_X".toByteArray(StandardCharsets.UTF_8), TrackSave.AngleX)
        nativeSetParamValue("PARAM_ANGLE_Y".toByteArray(StandardCharsets.UTF_8), TrackSave.AngleY)
        nativeSetParamValue("PARAM_ANGLE_Z".toByteArray(StandardCharsets.UTF_8), TrackSave.AngleZ)
        nativeSetParamValue("PARAM_MOUTH_OPEN_Y".toByteArray(StandardCharsets.UTF_8), TrackSave.MouthOpenY)
        nativeSetParamValue("PARAM_EYE_BALL_X".toByteArray(StandardCharsets.UTF_8), TrackSave.EyeBallX)
        nativeSetParamValue("PARAM_EYE_BALL_Y".toByteArray(StandardCharsets.UTF_8), TrackSave.EyeBallY)
        nativeSetParamValue("PARAM_BODY_Z".toByteArray(StandardCharsets.UTF_8), TrackSave.BodyZ)
        nativeSetParamValue("PARAM_BODY_Y".toByteArray(StandardCharsets.UTF_8), TrackSave.BodyY)
        nativeSetParamValue("PARAM_EYE_L_OPEN".toByteArray(StandardCharsets.UTF_8), TrackSave.EyeLOpen)
        nativeSetParamValue("PARAM_EYE_R_OPEN".toByteArray(StandardCharsets.UTF_8), TrackSave.EyeROpen)
    }

    fun onLoadModel(name: String) {
        JniBridgeJava.name = name
        if (name == "shizuku.model3.json") {
            nativeSetBreath("PARAM_BREATH".toByteArray(StandardCharsets.UTF_8))
            nativeEnableRandomMotion(false)
        } else {
            nativeEnableRandomMotion(true)
        }
    }

    val cubismParts: Array<CubismPart?>?
        get() {
            val list = nativeGetPartIds() ?: return null
            val list1 = arrayOfNulls<CubismPart>(list.size)
            val values = nativeGetPartValues()
            for (a in list.indices) {
                list1[a] = CubismPart()
                list1[a]!!.id = list[a]
                list1[a]!!.opacities = values[a]
            }
            return list1
        }
    val cubismParams: Array<CubismParam?>?
        get() {
            val list = nativeGetParamIds() ?: return null
            val list1 = arrayOfNulls<CubismParam>(list.size)
            val values = nativeGetParamValues()
            val defaultValues = nativeGetParamDefaultValues()
            val minValues = nativeGetParamMinimumValues()
            val maxValues = nativeGetParamMaximumValues()
            for (a in list.indices) {
                list1[a] = CubismParam()
                list1[a]!!.id = list[a]
                list1[a]!!.value = values[a]
                list1[a]!!.defaultValue = defaultValues[a]
                list1[a]!!.minimumValue = minValues[a]
                list1[a]!!.maximumValue = maxValues[a]
            }
            return list1
        }
}