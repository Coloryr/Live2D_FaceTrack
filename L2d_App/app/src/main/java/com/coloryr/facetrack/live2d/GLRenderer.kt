/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */
package com.coloryr.facetrack.live2d

import android.opengl.GLSurfaceView
import android.util.Log
import com.coloryr.facetrack.MainActivity
import com.coloryr.facetrack.socket.SocketUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        JniBridgeJava.nativeOnSurfaceCreated()
        MainActivity.ar.onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        JniBridgeJava.nativeOnSurfaceChanged(width, height)
        MainActivity.ar.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        val start = System.currentTimeMillis()
        MainActivity.ar.onDrawFrame()
        JniBridgeJava.nativeOnDrawFrame()
        SocketUtils.send()
        val end = System.currentTimeMillis()
        if (end - start > 100) Log.i("time", "time=" + (end - start))
        fps++
    }

    companion object {
        private var fps = 0
        fun getFps(): Int {
            val temp = fps
            fps = 0
            return temp
        }
    }
}