/**
 * Copyright(c) Live2D Inc. All rights reserved.
 * <p>
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.coloryr.facetrack.live2d;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import com.coloryr.facetrack.MainActivity;
import com.coloryr.facetrack.socket.SocketUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.*;

public class GLRenderer implements GLSurfaceView.Renderer {
    private final int width = 540;
    private final int height = 600;
    private ByteBuffer data;

    private static int fps;

    public static int getFps() {
        int temp = fps;
        fps = 0;
        return temp;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        JniBridgeJava.nativeOnSurfaceCreated();
        MainActivity.ar.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        JniBridgeJava.nativeOnSurfaceChanged(this.width, this.height);
        MainActivity.ar.onSurfaceChanged(this.width, this.height);
        data = ByteBuffer.allocateDirect(this.width * this.height * 4).order(ByteOrder.nativeOrder());
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long start = System.currentTimeMillis();
        MainActivity.ar.onDrawFrame();
        JniBridgeJava.nativeOnDrawFrame();
        SocketUtils.sendImage(data, width, height);
        long end = System.currentTimeMillis();
        if ((end - start) > 100)
            Log.i("time", "time=" + (end - start));
        fps++;
    }

    private void unbindPixelBuffer() {
        // Unmap the buffers
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, GLES30.GL_NONE);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
    }
}
