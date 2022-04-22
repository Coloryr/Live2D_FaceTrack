package com.coloryr.facetrack.track

interface IAR {
    fun onSurfaceCreated()
    fun onPause()
    fun onResume(): Boolean
    fun onDrawFrame()
    fun onSurfaceChanged(width: Int, height: Int)
}