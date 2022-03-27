/**
 * Copyright(c) Live2D Inc. All rights reserved.
 * <p>
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.coloryr.facetrack.live2d;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageReader;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import com.coloryr.facetrack.MainActivity;
import com.coloryr.facetrack.socket.SocketUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.*;

public class GLRenderer implements GLSurfaceView.Renderer {
    private int width;
    private int height;
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
        height = height/2;
        width = width/2;
        this.height = height;
        this.width = width;
        JniBridgeJava.nativeOnSurfaceChanged(width, height);
        MainActivity.ar.onSurfaceChanged(width, height);
        data = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
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
