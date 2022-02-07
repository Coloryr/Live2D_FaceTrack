package com.live2d.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

@SuppressLint("ViewConstructor")
public class GLView extends GLSurfaceView {
    private GLRenderer _glRenderer;
    private Context context;

    @SuppressLint("ClickableViewAccessibility")
    public final OnTouchListener myTouchListener = (v, event) -> {
        //处理手势事件（根据个人需要去返回和逻辑的处理）
        float pointX = event.getX();
        float pointY = event.getY();
        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    JniBridgeJava.nativeOnTouchesBegan(pointX, pointY);
                    break;
                case MotionEvent.ACTION_UP:
                    JniBridgeJava.nativeOnTouchesEnded(pointX, pointY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    JniBridgeJava.nativeOnTouchesMoved(pointX, pointY);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    };

    public GLView(Context context) {
        super(context);
        this.context = context;
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        setEGLContextClientVersion(2);
        _glRenderer = new GLRenderer();
        setRenderer(_glRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        JniBridgeJava.nativeOnStart();
    }

    public void callAdd(ViewGroup view) {
        JniBridgeJava.nativeOnStart();
        JniBridgeJava.SetContext(context);
        view.addView(this);
        view.setOnTouchListener(myTouchListener);
    }

    public void remove(ViewGroup view) {
        view.removeView(this);
        JniBridgeJava.nativeOnStop();
    }
}
