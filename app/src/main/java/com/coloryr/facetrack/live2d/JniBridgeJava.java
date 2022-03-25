/**
 * Copyright(c) Live2D Inc. All rights reserved.
 * <p>
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.coloryr.facetrack.live2d;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JniBridgeJava {

    private static final String LIBRARY_NAME = "live2d";
    @SuppressLint("StaticFieldLeak")
    private static Context _context;

    static {
        System.loadLibrary(LIBRARY_NAME);
    }

    // Native -----------------------------------------------------------------

    public static native void nativeOnStart();

    public static native void nativeOnStop();

    public static native void nativeOnDestroy();

    public static native void nativeOnSurfaceCreated();

    public static native void nativeOnSurfaceChanged(int width, int height);

    public static native void nativeOnDrawFrame();

    public static native void nativeOnTouchesBegan(float pointX, float pointY);

    public static native void nativeOnTouchesEnded(float pointX, float pointY);

    public static native void nativeOnTouchesMoved(float pointX, float pointY);

    private static native void nativeStartMotion(byte[] group, int no, int priority);

    private static native void nativeStartExpressions(byte[] name);

    public static native int nativeGetExpressionSize();

    public static native int nativeGetMotionSize();

    public static native byte[] nativeGetExpression(int index);

    public static native byte[] nativeGetMotion(int index);

    public static native void nativeSetPosX(float x);

    public static native void nativeSetPosY(float y);

    public static native void nativeSetPos(float x, float y);

    public static native void nativeSetScale(float scale);

    public static native void nativeLoadModel(byte[] path, byte[] name);

    public static native void nativeEnableRandomMotion(boolean enable);

    public static native void nativeSetBreath(byte[] id);

    public static native void nativeGetCubismParams();

    public static native void nativeSetCubismParams(byte[] id, float value);

    public static native void nativeSetEyeBallX(byte[] id);

    public static native void nativeSetEyeBallY(byte[] id);

    public static native void nativeSetBodyAngleX(byte[] id);

    public static native void nativeSetAngleX(byte[] id);

    public static native void nativeSetAngleY(byte[] id);

    public static native void nativeSetAngleZ(byte[] id);

    public static native String[] nativeGetParamIds();

    public static native float[] nativeGetPartValues();

    public static native String[] nativeGetPartIds();

    public static native float[] nativeGetParamValues();

    public static native float[] nativeGetParamDefaultValues();

    public static native float[] nativeGetParamMinimumValues();

    public static native float[] nativeGetParamMaximumValues();

    public static native void nativeSetParamValue(byte[] index, float value);

    // Java -----------------------------------------------------------------

    private static final List<String> motions = new ArrayList<>();
    private static final List<String> expressions = new ArrayList<>();

    private static void resData() {
        int a = nativeGetMotionSize();
        for (int i = 0; i < a; i++) {
            String temp = new String(nativeGetMotion(i));
            motions.add(temp);
        }
        a = nativeGetExpressionSize();
        for (int i = 0; i < a; i++) {
            String temp = new String(nativeGetExpression(i));
            expressions.add(temp);
        }
    }

    public static List<String> getMotions() {
        if (motions.size() == 0) {
            resData();
        }
        return motions;
    }

    public static List<String> getExpressions() {
        if (expressions.size() == 0) {
            resData();
        }
        return expressions;
    }

    public static void ChangeModel() {
        motions.clear();
        expressions.clear();
    }

    public static void StartMotion(String name, int no, int priority) {
        if (motions.size() == 0) {
            resData();
        }
        if (motions.contains(name + '_' + no)) {
            nativeStartMotion(name.getBytes(StandardCharsets.UTF_8), no, priority);
        }
    }

    public static void StartExpressions(String name) {
        if (expressions.size() == 0) {
            resData();
        }
        if (expressions.contains(name)) {
            nativeStartExpressions(name.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void SetContext(Context context) {
        _context = context;
    }

    public static byte[] LoadFile(String filePath) {
        InputStream fileData = null;
        try {
            fileData = _context.getAssets().open(filePath);
            int fileSize = fileData.available();
            byte[] fileBuffer = new byte[fileSize];
            fileData.read(fileBuffer, 0, fileSize);
            return fileBuffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileData != null) {
                    fileData.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void LoadModel(String path, String name) {
        byte[] path1 = path.getBytes(StandardCharsets.UTF_8);
        byte[] name1 = name.getBytes(StandardCharsets.UTF_8);
        nativeLoadModel(path1, name1);
    }

    static float a = -30;
    static boolean up = true;

    public static void onUpdate(){
        JniBridgeJava.nativeSetParamValue("PARAM_ANGLE_X".getBytes(StandardCharsets.UTF_8), TrackSave.AngleX);
        JniBridgeJava.nativeSetParamValue("PARAM_ANGLE_Y".getBytes(StandardCharsets.UTF_8), TrackSave.AngleY);
        JniBridgeJava.nativeSetParamValue("PARAM_ANGLE_Z".getBytes(StandardCharsets.UTF_8), TrackSave.AngleZ);
        JniBridgeJava.nativeSetParamValue("PARAM_MOUTH_OPEN_Y".getBytes(StandardCharsets.UTF_8), TrackSave.MouthOpenY);
        JniBridgeJava.nativeSetParamValue("PARAM_EYE_BALL_X".getBytes(StandardCharsets.UTF_8), TrackSave.EyeBallX);
        JniBridgeJava.nativeSetParamValue("PARAM_EYE_BALL_Y".getBytes(StandardCharsets.UTF_8), TrackSave.EyeBallY);
        JniBridgeJava.nativeSetParamValue("PARAM_BODY_Z".getBytes(StandardCharsets.UTF_8), TrackSave.BodyZ);
        JniBridgeJava.nativeSetParamValue("PARAM_BODY_Y".getBytes(StandardCharsets.UTF_8), TrackSave.BodyY);
        JniBridgeJava.nativeSetParamValue("PARAM_EYE_L_OPEN".getBytes(StandardCharsets.UTF_8), TrackSave.EyeLOpen);
        JniBridgeJava.nativeSetParamValue("PARAM_EYE_R_OPEN".getBytes(StandardCharsets.UTF_8), TrackSave.EyeROpen);
    }

    public static void onLoadModel(String name) {
        if (name.equals("shizuku.model3.json")) {
            JniBridgeJava.nativeSetBreath("PARAM_BREATH".getBytes(StandardCharsets.UTF_8));
            JniBridgeJava.nativeEnableRandomMotion(false);
        } else {
            JniBridgeJava.nativeEnableRandomMotion(true);
        }
    }

    public static CubismPart[] getCubismParts() {
        String[] list = JniBridgeJava.nativeGetPartIds();
        if (list == null) {
            return null;
        }

        CubismPart[] list1 = new CubismPart[list.length];
        float[] values = nativeGetPartValues();

        for (int a = 0; a < list.length; a++) {
            list1[a] = new CubismPart();
            list1[a].id = list[a];
            list1[a].opacities = values[a];
        }
        return list1;
    }

    public static CubismParam[] getCubismParams() {
        String[] list = JniBridgeJava.nativeGetParamIds();
        if (list == null) {
            return null;
        }

        CubismParam[] list1 = new CubismParam[list.length];
        float[] values = nativeGetParamValues();
        float[] defaultValues = nativeGetParamDefaultValues();
        float[] minValues = nativeGetParamMinimumValues();
        float[] maxValues = nativeGetParamMaximumValues();

        for (int a = 0; a < list.length; a++) {
            list1[a] = new CubismParam();
            list1[a].id = list[a];
            list1[a].value = values[a];
            list1[a].defaultValue = defaultValues[a];
            list1[a].minimumValue = minValues[a];
            list1[a].maximumValue = maxValues[a];
        }
        return list1;
    }
}
