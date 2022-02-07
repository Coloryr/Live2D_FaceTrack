package com.tzutalin.dlib;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.File;

/**
 * Created by darrenl on 2016/4/22.
 */
public final class Constants {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public static void SetContext(Context context) {
        Constants.context = context;
    }

    /**
     * getFaceShapeModelPath
     * @return default face shape model path
     */
    public static String getFaceShapeModelPath() {
        return new File(context.getDir("cascade", Context.MODE_PRIVATE), "shape_predictor_68_face_landmarks.dat").getAbsolutePath();
    }
}
