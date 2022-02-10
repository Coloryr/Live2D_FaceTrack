/**
 * Copyright(c) Live2D Inc. All rights reserved.
 * <p>
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.coloryr.facetrack.live2d;

import android.opengl.GLSurfaceView;
import com.coloryr.facetrack.MainActivity;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        JniBridgeJava.nativeOnSurfaceCreated();
        MainActivity.ar.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        JniBridgeJava.nativeOnSurfaceChanged(width, height);
        MainActivity.ar.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        MainActivity.ar.onDrawFrame();
        JniBridgeJava.nativeOnDrawFrame();
    }
}
