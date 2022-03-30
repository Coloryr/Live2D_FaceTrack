package com.coloryr.facetrack.track.eye;

import android.content.Context;
import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import android.media.Image;
import android.util.Log;
import com.coloryr.facetrack.live2d.TrackSave;
import com.google.android.renderscript.Toolkit;
import com.google.android.renderscript.YuvFormat;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.nio.ByteBuffer;

public class EyeTrack {
    private final static String TAG = "eye";
    private final Context context;
    private FaceMesh facemesh;

    public EyeTrack(Context context) {
        this.context = context;
    }

    private ByteBuffer imageToByteBuffer(final Image image) {
        final Rect crop = image.getCropRect();
        final int width = crop.width();
        final int height = crop.height();

        final Image.Plane[] planes = image.getPlanes();
        final byte[] rowData = new byte[planes[0].getRowStride()];
        final int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
        final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

        int channelOffset = 0;
        int outputStride = 0;

        for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
            if (planeIndex == 0) {
                channelOffset = 0;
                outputStride = 1;
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1;
                outputStride = 2;
            } else if (planeIndex == 2) {
                channelOffset = width * height;
                outputStride = 2;
            }

            final ByteBuffer buffer = planes[planeIndex].getBuffer();
            final int rowStride = planes[planeIndex].getRowStride();
            final int pixelStride = planes[planeIndex].getPixelStride();

            final int shift = (planeIndex == 0) ? 0 : 1;
            final int widthShifted = width >> shift;
            final int heightShifted = height >> shift;

            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

            for (int row = 0; row < heightShifted; row++) {
                final int length;

                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted;
                    buffer.get(output.array(), channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (widthShifted - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);

                    for (int col = 0; col < widthShifted; col++) {
                        output.array()[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return output;
    }


    public Bitmap getBitmapFromImage(Image image) {
        ByteBuffer yuvBytes = imageToByteBuffer(image);
        return Toolkit.INSTANCE.yuvToRgbBitmap(yuvBytes.array(), image.getWidth(), image.getHeight(), YuvFormat.NV21);
    }

    public void init() {
        facemesh = new FaceMesh(this.context,
                FaceMeshOptions.builder()
                        .setStaticImageMode(true)
                        .setRefineLandmarks(true)
                        .setRunOnGpu(true)
                        .build());
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        facemesh.setResultListener(this::run);
    }

    public void run(FaceMeshResult result) {
        int numFaces = result.multiFaceLandmarks().size();
        if (numFaces == 0)
            return;

        LandmarkProto.NormalizedLandmarkList list = result.multiFaceLandmarks().get(0);

        LandmarkProto.NormalizedLandmark p13 = list.getLandmark(13);
        LandmarkProto.NormalizedLandmark p14 = list.getLandmark(14);
        double mo = dis(p13.getX(), p13.getY(), p14.getX(), p14.getY());
        if (mo < 0.001) {
            mo = 0;
        }
        mo *= 20;
        TrackSave.MouthOpenY = (float) mo;

        LandmarkProto.NormalizedLandmark p385 = list.getLandmark(385);
        LandmarkProto.NormalizedLandmark p386 = list.getLandmark(386);
        LandmarkProto.NormalizedLandmark p374 = list.getLandmark(374);
        LandmarkProto.NormalizedLandmark p362 = list.getLandmark(362);
        LandmarkProto.NormalizedLandmark p263 = list.getLandmark(263);

        double qx = (p385.getX() + p386.getX()) / 2;
        double qy = (p385.getY() + p386.getY()) / 2;

        double lo1 = dis(qx, qy, p374.getX(), p374.getY());
        double lo2 = dis(p362.getX(), p362.getY(), p263.getX(), p374.getY());
        double lo = lo1 / lo2;
        lo = lo - 0.04;
        lo = lo / 0.16;
        lo = lo - 0.1;
        if (lo > 1) {
            lo = 1;
        } else if (lo < 0)
            lo = 0;

        TrackSave.EyeLOpen = (float) lo - 1;

        LandmarkProto.NormalizedLandmark p159 = list.getLandmark(159);
        LandmarkProto.NormalizedLandmark p158 = list.getLandmark(158);
        LandmarkProto.NormalizedLandmark p145 = list.getLandmark(145);
        LandmarkProto.NormalizedLandmark p33 = list.getLandmark(33);
        LandmarkProto.NormalizedLandmark p133 = list.getLandmark(133);

        double qx1 = (p159.getX() + p158.getX()) / 2;
        double qy1 = (p159.getY() + p158.getY()) / 2;

        double ro1 = dis(qx1, qy1, p145.getX(), p145.getY());
        double ro2 = dis(p33.getX(), p33.getY(), p133.getX(), p133.getY());
        double ro = ro1 / ro2;
        ro = ro - 0.04;
        ro = ro / 0.16;
        ro = ro - 0.05;
        if (ro > 1) {
            ro = 1;
        } else if (ro < 0)
            ro = 0;

        TrackSave.EyeROpen = (float) ro - 1;

        LandmarkProto.NormalizedLandmark p474 = list.getLandmark(474);
        LandmarkProto.NormalizedLandmark p475 = list.getLandmark(475);
        LandmarkProto.NormalizedLandmark p476 = list.getLandmark(476);
        LandmarkProto.NormalizedLandmark p477 = list.getLandmark(477);

        double lx1 = (p474.getX() + p476.getX()) / 2;
        double ly1 = (p475.getY() + p477.getY()) / 2;

        LandmarkProto.NormalizedLandmark p469 = list.getLandmark(469);
        LandmarkProto.NormalizedLandmark p470 = list.getLandmark(470);
        LandmarkProto.NormalizedLandmark p471 = list.getLandmark(471);
        LandmarkProto.NormalizedLandmark p472 = list.getLandmark(472);

        double rx1 = (p469.getX() + p471.getX()) / 2;
        double ry1 = (p470.getY() + p472.getY()) / 2;

        double ex = (lx1 + rx1) / 2;
        double ey = (ly1 + ry1) / 2;

        double ex1 = (p263.getX() + p133.getX()) / 2;
        double ey1 = (p263.getY() + p133.getY()) / 2;
        double ex2 = (p362.getX() + p33.getX()) / 2;
        double ey2 = (p362.getY() + p33.getY()) / 2;

        double dx1 = dis(ex, ey, ex1, ey1);
        double dx2 = dis(ex, ey, ex2, ey2);

        double dx = dx1 / dx2;
        dx -= 1;
        if (dx < 0) {
            dx *= 2;
        } else {
            dx /= 2;
        }
        TrackSave.EyeBallX = (float) -dx;

        double ex3 = (qx + qx1) / 2;
        double ey3 = (qy + qy1) / 2;
        double ex4 = (p145.getX() + p374.getX()) / 2;
        double ey4 = (p145.getY() + p374.getY()) / 2;

        double dy1 = dis(ex, ey, ex3, ey3);
        double dy2 = dis(ex, ey, ex4, ey4);

        double dy = dy1 / dy2;

        dy *= 2;
        dy -= 1.5;

        TrackSave.EyeBallY = (float) dy;
    }

    private double dis(double x1, double y1, double x2, double y2) {
        double temp = (x2 - x1);
        double temp1 = (y2 - y1);
        return Math.sqrt(temp * temp + temp1 * temp1);
    }

    private static Bitmap rotateBitmap(Bitmap inputBitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                matrix.postRotate(0);
        }
        return Bitmap.createBitmap(inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
    }

    public void onCameraFrame(Image image) {
        Bitmap bitmap = getBitmapFromImage(image);

        bitmap = rotateBitmap(bitmap, ExifInterface.ORIENTATION_ROTATE_270);

        facemesh.send(bitmap);
    }
}
