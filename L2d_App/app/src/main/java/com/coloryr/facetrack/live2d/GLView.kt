package com.coloryr.facetrack.live2d

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup

@SuppressLint("ViewConstructor")
class GLView(context: Context) : GLSurfaceView(context) {
    private val _glRenderer: GLRenderer

    @SuppressLint("ClickableViewAccessibility")
    val myTouchListener = OnTouchListener { v: View?, event: MotionEvent ->
        //处理手势事件（根据个人需要去返回和逻辑的处理）
        val pointX = event.x
        val pointY = event.y
        try {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> JniBridgeJava.nativeOnTouchesBegan(pointX, pointY)
                MotionEvent.ACTION_UP -> JniBridgeJava.nativeOnTouchesEnded(pointX, pointY)
                MotionEvent.ACTION_MOVE -> JniBridgeJava.nativeOnTouchesMoved(pointX, pointY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    init {
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setEGLContextClientVersion(2)
        _glRenderer = GLRenderer()
        setRenderer(_glRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        JniBridgeJava.nativeOnStart()
    }

    fun callAdd(view: ViewGroup?) {
        JniBridgeJava.nativeOnStart()
        JniBridgeJava.SetContext(context)
        view!!.addView(this)
        view.setOnTouchListener(myTouchListener)
    }

    fun remove(view: ViewGroup) {
        view.removeView(this)
        JniBridgeJava.nativeOnStop()
    }
}