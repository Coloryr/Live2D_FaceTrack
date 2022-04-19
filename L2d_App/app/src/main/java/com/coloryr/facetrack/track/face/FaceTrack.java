package com.coloryr.facetrack.track.face;

import android.util.Log;
import com.coloryr.facetrack.MainActivity;
import com.google.mediapipe.formats.proto.LocationDataProto;
import com.google.mediapipe.solutions.facedetection.FaceDetection;
import com.google.mediapipe.solutions.facedetection.FaceDetectionOptions;
import com.google.mediapipe.solutions.facedetection.FaceKeypoint;

public class FaceTrack {
    public static void Test(){
        FaceDetectionOptions faceDetectionOptions =
                FaceDetectionOptions.builder()
                        .setStaticImageMode(false)
                        .setModelSelection(0).build();
        FaceDetection faceDetection = new FaceDetection(MainActivity.app, faceDetectionOptions);
        faceDetection.setErrorListener(
                (message, e) -> Log.e("MediaPipe", "MediaPipe Face Detection error:" + message));
        faceDetection.setResultListener(
                faceDetectionResult -> {
                    if (faceDetectionResult.multiFaceDetections().isEmpty()) {
                        return;
                    }
                    faceDetectionResult.multiFaceDetections().get(0).getLocationData().getRelativeKeypointsList().get(0).getScore();

                    int width = faceDetectionResult.inputBitmap().getWidth();
                    int height = faceDetectionResult.inputBitmap().getHeight();
                    LocationDataProto.LocationData.RelativeKeypoint noseTip =
                            faceDetectionResult
                                    .multiFaceDetections()
                                    .get(0)
                                    .getLocationData()
                                    .getRelativeKeypoints(FaceKeypoint.NOSE_TIP);
                    Log.i("MediaPipe",
                            String.format(
                                    "MediaPipe Face Detection nose tip coordinates (pixel values): x=%f, y=%f",
                                    noseTip.getX() * width, noseTip.getY() * height));
                });
    }
}
