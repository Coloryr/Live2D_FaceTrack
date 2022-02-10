package com.coloryr.facetrack.track.eye;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Build;
import android.util.Log;
import com.coloryr.facetrack.MainActivity;
import com.coloryr.facetrack.R;
import com.coloryr.facetrack.live2d.TrackSave;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class EyeTrack {
    private final static String TAG = "eye";
    private final Context context;
    private File mCascadeFile;
    private File mCascadeFileEye;
    private Mat mRgba;
    private Mat mGray;
    private int mAbsoluteFaceSize = 0;
    private float mRelativeFaceSize = 0.2f;

    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;

    public EyeTrack(Context context) {
        this.context = context;
    }

    public void init() throws Exception {
        // load cascade file from application resources
        InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        FileOutputStream os = new FileOutputStream(mCascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        // load cascade file from application resources
        InputStream ise = context.getResources().openRawResource(R.raw.haarcascade_eye);
        File cascadeDirEye = context.getDir("cascade", Context.MODE_PRIVATE);
        mCascadeFileEye = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");
        FileOutputStream ose = new FileOutputStream(mCascadeFileEye);

        while ((bytesRead = ise.read(buffer)) != -1) {
            ose.write(buffer, 0, bytesRead);
        }
        ise.close();
        ose.close();

        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        if (mJavaDetector.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            mJavaDetector = null;
        } else
            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

        mJavaDetectorEye = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
        if (mJavaDetectorEye.empty()) {
            Log.e(TAG, "Failed to load cascade classifier for eye");
            mJavaDetectorEye = null;
        } else
            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileEye.getAbsolutePath());

        cascadeDir.delete();
        cascadeDirEye.delete();

        mGray = new Mat();
        mRgba = new Mat();

        teplateR = new Mat();
    }

    public Mat gray(Image mImage) {
        Image.Plane[] planes = mImage.getPlanes();
        int w = mImage.getWidth();
        int h = mImage.getHeight();
        assert (planes[0].getPixelStride() == 1);
        ByteBuffer y_plane = planes[0].getBuffer();
        int y_plane_step = planes[0].getRowStride();
        mGray = new Mat(h, w, CvType.CV_8UC1, y_plane, y_plane_step);
        return mGray;
    }

    public Mat rgba(Image mImage) {
        Image.Plane[] planes = mImage.getPlanes();
        int w = mImage.getWidth();
        int h = mImage.getHeight();
        int chromaPixelStride = planes[1].getPixelStride();

        if (chromaPixelStride == 2) { // Chroma channels are interleaved
            assert (planes[0].getPixelStride() == 1);
            assert (planes[2].getPixelStride() == 2);
            ByteBuffer y_plane = planes[0].getBuffer();
            int y_plane_step = planes[0].getRowStride();
            ByteBuffer uv_plane1 = planes[1].getBuffer();
            int uv_plane1_step = planes[1].getRowStride();
            ByteBuffer uv_plane2 = planes[2].getBuffer();
            int uv_plane2_step = planes[2].getRowStride();
            Mat y_mat = new Mat(h, w, CvType.CV_8UC1, y_plane, y_plane_step);
            Mat uv_mat1 = new Mat(h / 2, w / 2, CvType.CV_8UC2, uv_plane1, uv_plane1_step);
            Mat uv_mat2 = new Mat(h / 2, w / 2, CvType.CV_8UC2, uv_plane2, uv_plane2_step);
            long addr_diff = uv_mat2.dataAddr() - uv_mat1.dataAddr();
            if (addr_diff > 0) {
                assert (addr_diff == 1);
                Imgproc.cvtColorTwoPlane(y_mat, uv_mat1, mRgba, Imgproc.COLOR_YUV2RGBA_NV12);
            } else {
                assert (addr_diff == -1);
                Imgproc.cvtColorTwoPlane(y_mat, uv_mat2, mRgba, Imgproc.COLOR_YUV2RGBA_NV21);
            }
            return mRgba;
        } else { // Chroma channels are not interleaved
            byte[] yuv_bytes = new byte[w * (h + h / 2)];
            ByteBuffer y_plane = planes[0].getBuffer();
            ByteBuffer u_plane = planes[1].getBuffer();
            ByteBuffer v_plane = planes[2].getBuffer();

            int yuv_bytes_offset = 0;

            int y_plane_step = planes[0].getRowStride();
            if (y_plane_step == w) {
                y_plane.get(yuv_bytes, 0, w * h);
                yuv_bytes_offset = w * h;
            } else {
                int padding = y_plane_step - w;
                for (int i = 0; i < h; i++) {
                    y_plane.get(yuv_bytes, yuv_bytes_offset, w);
                    yuv_bytes_offset += w;
                    if (i < h - 1) {
                        y_plane.position(y_plane.position() + padding);
                    }
                }
                assert (yuv_bytes_offset == w * h);
            }

            int chromaRowStride = planes[1].getRowStride();
            int chromaRowPadding = chromaRowStride - w / 2;

            if (chromaRowPadding == 0) {
                // When the row stride of the chroma channels equals their width, we can copy
                // the entire channels in one go
                u_plane.get(yuv_bytes, yuv_bytes_offset, w * h / 4);
                yuv_bytes_offset += w * h / 4;
                v_plane.get(yuv_bytes, yuv_bytes_offset, w * h / 4);
            } else {
                // When not equal, we need to copy the channels row by row
                for (int i = 0; i < h / 2; i++) {
                    u_plane.get(yuv_bytes, yuv_bytes_offset, w / 2);
                    yuv_bytes_offset += w / 2;
                    if (i < h / 2 - 1) {
                        u_plane.position(u_plane.position() + chromaRowPadding);
                    }
                }
                for (int i = 0; i < h / 2; i++) {
                    v_plane.get(yuv_bytes, yuv_bytes_offset, w / 2);
                    yuv_bytes_offset += w / 2;
                    if (i < h / 2 - 1) {
                        v_plane.position(v_plane.position() + chromaRowPadding);
                    }
                }
            }

            Mat yuv_mat = new Mat(h + h / 2, w, CvType.CV_8UC1);
            yuv_mat.put(0, 0, yuv_bytes);
            Imgproc.cvtColor(yuv_mat, mRgba, Imgproc.COLOR_YUV2RGBA_I420, 4);
            return mRgba;
        }
    }

    private Mat teplateR;

    public void onCameraFrame(Image image) {
        mRgba = rgba(image);

        Core.transpose(mRgba, mRgba);
        Core.flip(mRgba, mRgba, 0);

        mGray = gray(image);

        Core.transpose(mGray, mGray);
        Core.flip(mGray, mGray, 0);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();
        for (Rect rect : facesArray) {
            MatOfRect eye = new MatOfRect();
            Mat mROI = mGray.submat(rect);
            mROI = mROI.submat(0, mROI.width() / 2, 0, mROI.height() / 2);

            mJavaDetectorEye.detectMultiScale(mROI, eye, 1.15, 2,
                    Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                            | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                    new Size());

            Rect[] eyeArray = eye.toArray();
            int count = eyeArray.length;
            if (count != 0) {
                Rect item = eyeArray[0];
                mROI = mROI.submat(item);
                Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
                Imgproc.circle(mROI, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
                Point iris = mmG.minLoc;

                countX += iris.x;
                countY += iris.y;

                index++;
                if (index == 10) {
                    countX = countX / index;
                    countY = countY / index;
                    index = 0;

                    countX = (countX / mROI.width());
                    countY = (countY / mROI.height());
                    if(countX < 0.5)
                    {
                        countX -= 0.5f;
                        countX *= 10;
                    }
                    else
                    {
                        countX -= 0.5f;
                        countX *= 4;
                    }

                    TrackSave.eyeX = countX;
                    TrackSave.eyeY = (countY * 2 - 1.0f) * 2;
                }
            }

//            Bitmap bitmap = Bitmap.createBitmap(mROI.width(), mROI.height(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(mROI, bitmap);
//
//            MainActivity.app.runOnUiThread(() -> {
//                MainActivity.imageView.setImageBitmap(bitmap);
//            });
        }
    }

    private float countX = 0;
    private float countY = 0;
    private int index = 0;
}
