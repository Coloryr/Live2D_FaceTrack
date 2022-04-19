package com.coloryr.facetrack.track;

public interface IAR {
    void onSurfaceCreated();
    void onPause();
    boolean onResume();
    void onDrawFrame();
    void onSurfaceChanged(int width, int height);
}
