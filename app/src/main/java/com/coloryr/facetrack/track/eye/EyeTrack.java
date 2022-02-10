package com.coloryr.facetrack.track.eye;

import android.content.Context;
import android.media.Image;
import android.util.Log;
import com.coloryr.facetrack.R;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class EyeTrack {
    private final static String TAG = "eye";
    private final Context context;
    private File mCascadeFile;
    private File mCascadeFileEye;
    private Mat mRgba;
    private Mat mGray;
    private int mAbsoluteFaceSize = 0;
    private float mRelativeFaceSize = 0.2f;
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    int method = 1;

    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;

    private double xCenter = -1;
    private double yCenter = -1;

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
        InputStream ise = context.getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
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

        cascadeDirEye.delete();

        mGray = new Mat();
        mRgba = new Mat();
    }

    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template;
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; ) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRgba.submat(eye_only_rectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            Imgproc.rectangle(mRgba, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
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

    private void match_eye(Rect area, Mat mTemplate, int type) {
        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return;
        }
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case Imgproc.TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case Imgproc.TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case Imgproc.TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case Imgproc.TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case Imgproc.TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case Imgproc.TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == Imgproc.TM_SQDIFF || type == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);

        Imgproc.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                255));
        Rect rec = new Rect(matchLoc_tx, matchLoc_ty);


    }

    private void CreateAuxiliaryMats() {
        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
                    + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
                    + cols / 10, cols);
        }

    }

    public void onCameraFrame(Image image) {
        mRgba = rgba(image);
        mGray = gray(image);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mZoomWindow == null || mZoomWindow2 == null)
            CreateAuxiliaryMats();

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();
        for (Rect rect : facesArray) {
            xCenter = (rect.x + rect.width + rect.x) / 2;
            yCenter = (rect.y + rect.y + rect.height) / 2;
            Point center = new Point(xCenter, yCenter);

            // compute the eye area
            Rect eyearea = new Rect(rect.x + rect.width / 8,
                    (int) (rect.y + (rect.height / 4.5)), rect.width - 2 * rect.width / 8,
                    (int) (rect.height / 3.0));
            // split it
            Rect eyearea_right = new Rect(rect.x + rect.width / 16,
                    (int) (rect.y + (rect.height / 4.5)),
                    (rect.width - 2 * rect.width / 16) / 2, (int) (rect.height / 3.0));
            Rect eyearea_left = new Rect(rect.x + rect.width / 16
                    + (rect.width - 2 * rect.width / 16) / 2,
                    (int) (rect.y + (rect.height / 4.5)),
                    (rect.width - 2 * rect.width / 16) / 2, (int) (rect.height / 3.0));

            if (learn_frames < 200) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            } else {
                // Learning finished, use the new templates for template
                // matching
                match_eye(eyearea_right, teplateR, method);
                match_eye(eyearea_left, teplateL, method);
            }
        }
    }
}
