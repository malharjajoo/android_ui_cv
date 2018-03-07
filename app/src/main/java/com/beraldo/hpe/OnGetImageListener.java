package com.beraldo.hpe;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import org.opencv.android.Utils;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.beraldo.hpe.dlib.HeadPoseDetector;
import com.beraldo.hpe.dlib.HeadPoseGaze;

import com.beraldo.hpe.utils.FileUtils;
import com.beraldo.hpe.utils.ImageUtils;
import com.beraldo.hpe.utilsTF.ImageUtilsTF;

import com.beraldo.hpe.utils.XMLWriter;
import com.beraldo.hpe.view.FloatingCameraWindow;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.Graph;
import org.tensorflow.Operation;



/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with dlib lib.
 */
public class OnGetImageListener implements OnImageAvailableListener {
    private static final int NUM_CLASSES = 1001;
    private static final int INPUT_SIZE = 240;
    private static final int IMAGE_MEAN = 117;
    private static final String TAG = "OnGetImageListener";

    private static final double maxYaw = 20;
    private static final double minYaw = -20;
    private static final double minPitch = -10;

    private enum State {NOT_THERE, NOT_PAYING_ATTENTION, PAYING_ATTENTION};
    private State mState = State.PAYING_ATTENTION;
    private State mPreviousState = State.PAYING_ATTENTION;
    private State mOutputState = State.PAYING_ATTENTION;

    private long tStart;
    private long tDelta;

    private boolean hVFOA = false;
    private boolean eVFOA = false;

    private int mScreenRotation = 0;

    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mRGBrotatedBitmap = null;
    private Bitmap mRGBrotatedBitmapCopy = null;
    //private Bitmap mCroppedBitmap = null;

    private boolean mIsComputing = false;
    private Handler mInferenceHandler;

    private Context mContext;
    private HeadPoseDetector mHeadPoseDetector;
    private TextView mPerformanceView;
    private TextView mResultsView;
    private FloatingCameraWindow mWindow;

    private DecimalFormat df;
    private Document detectionDocument;

    private double overallTime = 0;
    private int valid_cycles = 0;


    // face det and eye tracking

    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

    private File                   mCascadeFile;
    private File                   mCascadeFileEye;
    private CascadeClassifier      mJavaDetector;
    private CascadeClassifier      mJavaDetectorEye;

    public static final int        JAVA_DETECTOR       = 0;
    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize = 0;

    private Mat mRgba;
    private Mat mGray;

    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final Scalar    TF_FACE_RECT_COLOR  = new Scalar(0, 0, 255, 255);

    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    int method = 1;

    private Rect ext_eyes_only_rect;
    private Rect ext_matchLoc;

    private static final String FACE_DETECTION_MODEL_FILE =
            "file:///android_asset/frozen_inference_graph_face.pb";
    private static final int FACE_DETECTION_INPUT_SIZE = 300;

    private static final String FACE_DETECTION_LABELS_FILE = "file:///android_asset/face_labels_list.txt";

    private static final float MINIMUM_CONFIDENCE_FACE = 0.4f;

    private Classifier detector;

    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    int cropSize = FACE_DETECTION_INPUT_SIZE;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private Integer sensorOrientation = 90;

    private List<Classifier.Recognition> TFresults;

    org.opencv.core.Point topLeft = new org.opencv.core.Point();
    org.opencv.core.Point bottomRight = new org.opencv.core.Point();

    Rect[] facesArrayTF;

    private float userBias = 0f;

    private List<Classifier.Recognition> mappedRecognitions;

    private boolean lookingDown = true;

    private org.opencv.core.Point ext_iris = new org.opencv.core.Point();

    private final long timeOutAttention = 5000;
    private final long timeOutPresence = 2000;

    // end

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    static{
        System.loadLibrary("tensorflow_inference");
    }

    public void initialize( final Context context, final float[] intrinsics, final float[] distortions, final TextView mPerformanceView, final TextView mResultsView, final Handler handler) {
        this.mContext = context;
        this.mPerformanceView = mPerformanceView;
        this.mResultsView = mResultsView;
        this.mInferenceHandler = handler;
        mHeadPoseDetector = new HeadPoseDetector();
        mWindow = new FloatingCameraWindow(mContext);

        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        mGray = new Mat();
        mRgba = new Mat();

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

        cropToFrameTransform = new Matrix();

        // Ensure the model file is properly deserialized into destination
        File model = new File(FileUtils.getPreference(mContext, FileUtils.DATA_DIR_PREFS_NAME), FileUtils.PREDICTOR_FILE_NAME);
        if (!model.exists()) {
            Log.d(TAG, "Copying landmark model to " + model.getAbsolutePath());
            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, model.getAbsolutePath());
        }

        // Initialize the headpose detector with its parameters
        mHeadPoseDetector.init(model.getAbsolutePath(), MainActivity.mode, intrinsics, distortions);

        // Initialize the formatter for the strings to be shown
        df = new DecimalFormat("##.##");
        df.setRoundingMode(RoundingMode.DOWN);

        tStart = System.currentTimeMillis();

        if(MainActivity.saveFile) detectionDocument = XMLWriter.newDocument(MainActivity.mode);

        try {
            // load cascade file from application resources
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);

            //InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
            //mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
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

            cascadeDir.delete();
            cascadeDirEye.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }

        int cropSize = FACE_DETECTION_INPUT_SIZE;

        try {
            detector = TensorFlowObjectDetectionAPIModel.create(
                    context.getAssets(), FACE_DETECTION_MODEL_FILE, FACE_DETECTION_LABELS_FILE, FACE_DETECTION_INPUT_SIZE);
            cropSize = FACE_DETECTION_INPUT_SIZE;
            Log.i(TAG, "Successfully loaded Tensorflow model");
        } catch (final IOException e) {
            Toast toast =
                    Toast.makeText(
                            context.getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            Log.e(TAG, "Failed to load Tensorflow model. Exception thrown: " + e);
        }


    }

    public void deInitialize() {
        synchronized (OnGetImageListener.this) {
            if(MainActivity.saveFile) {// Update performance info and save the file
                XMLWriter.addTimePerformance(detectionDocument, overallTime / valid_cycles); // Add performance field
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                XMLWriter.saveDocumentToFile(mContext, detectionDocument, "detection_" + sdf.format(new Date(System.currentTimeMillis())) + ".xml");
            }
            if (mHeadPoseDetector != null) {
                mHeadPoseDetector.deInit();
            }

            if (mWindow != null) {
                mWindow.release();
            }
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;

        if (screen_width < screen_height) { // Screen is in portrait
            mScreenRotation = 0;
        } else { // Screen is in landscape
            mScreenRotation = 90;
        }

        Assert.assertEquals(dst.getWidth(), dst.getHeight()); // Make sure the destination bitmap is square
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        // Set the scale to accomodate the least between height and width of the source
        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    private void drawUnmirroredRotatedBitmap(final Bitmap src, final Bitmap dst, final int rotation) {
        final Matrix matrix = new Matrix();
        //matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
        matrix.postRotate(rotation);
        matrix.setScale(-1, 1);
        matrix.postTranslate(dst.getWidth(), 0);

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }


    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();

        org.opencv.core.Point iris = new org.opencv.core.Point();


        Rect eye_template = new Rect();
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));

            ext_eyes_only_rect=eye_only_rectangle;

            mROI = mGray.submat(eye_only_rectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;

            ext_iris = iris;

            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    private void match_eye(Rect area, Mat mTemplate, int type, boolean right) {
        org.opencv.core.Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return ;
        }
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        org.opencv.core.Point matchLoc_tx = new org.opencv.core.Point(matchLoc.x + area.x, matchLoc.y + area.y);
        org.opencv.core.Point matchLoc_ty = new org.opencv.core.Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);

        Imgproc.rectangle(mRgba, matchLoc_tx, matchLoc_ty, FACE_RECT_COLOR, 1);
        Rect rec = new Rect(matchLoc_tx,matchLoc_ty);

        if(right){
            ext_matchLoc = rec;
        }

    }


    void get_eye_rect(CascadeClassifier clasificator, Rect area) {

        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();

        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            ext_eyes_only_rect=eye_only_rectangle;
            i++;
        }
    }

    private void faceEyeDetect(){

        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 4, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        //Rect[] facesArray = faces.toArray();


        //for (int i = 0; i < facesArray.length; i++)
        for (final Classifier.Recognition result : mappedRecognitions)
        {	final RectF location = result.getLocation();
            final Rect r = new Rect((int) location.left ,
                    (int) location.top, (int) location.width(),
                    (int) location.height());

//            Imgproc.rectangle(mRgba, r.tl(), r.br(),
//                FACE_RECT_COLOR, 3);

            //Rect r = facesArray[i];
            // compute the eye area
            Rect eyearea = new Rect(r.x + r.width / 8,
                    (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
                    (int) (r.height / 3.0));
            // split it
            Rect eyearea_right = new Rect(r.x + r.width / 16,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            Rect eyearea_left = new Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));


            if (learn_frames < 5) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            } else if (learn_frames >= 5 && learn_frames < 10) {
                // Learning finished, use the new templates for template
                // matching
                get_eye_rect(mJavaDetectorEye, eyearea_right);
                //Imgproc.rectangle(mRgba, ext_eyes_only_rect.tl(), ext_eyes_only_rect.br(),
                 //FACE_RECT_COLOR, 2);
                match_eye(eyearea_right, teplateR, method, true);
                match_eye(eyearea_left, teplateL, method, false);
                userBias += (ext_eyes_only_rect.y - ext_iris.y) / 5.0;
                learn_frames++;
            } else {
                // Learning finished, use the new templates for template
                // matching
                get_eye_rect(mJavaDetectorEye, eyearea_right);
                //Imgproc.rectangle(mRgba, ext_eyes_only_rect.tl(), ext_eyes_only_rect.br(),
                //FACE_RECT_COLOR, 2);
                match_eye(eyearea_right, teplateR, method, true);
                match_eye(eyearea_left, teplateL, method, false);
            }
        }

        Log.i(TAG, String.format("Detected %d faces", mappedRecognitions.size()));
        Log.i(TAG, String.format("User bias: %f", userBias));

    }

    private void faceDetectTF(){

        frameToCropTransform =
                ImageUtilsTF.getTransformationMatrix(
                        mPreviewWdith, mPreviewHeight,
                        cropSize, cropSize,
                        sensorOrientation, false);

        frameToCropTransform.invert(cropToFrameTransform);

        Log.d(TAG, String.format("Initializing at size %dx%d", mPreviewWdith, mPreviewHeight));

        Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(mRGBrotatedBitmap, frameToCropTransform, null);

        //final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        //TFresults = detector.recognizeImage(croppedBitmap);

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        //final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_FACE;

        mappedRecognitions = new LinkedList<Classifier.Recognition>();

        //facesArrayTF =

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                Log.i(TAG, String.format("Tensorflow has detected a face. Confidence: %f", result.getConfidence()));
                cropToFrameTransform.mapRect(location);
                topLeft.x = location.left;
                topLeft.y = location.top;

                bottomRight.x = location.right;
                bottomRight.y = location.bottom;

                Rect tmpr = new Rect((int) topLeft.x,
                        (int) topLeft.y, (int) location.width(),
                        (int) location.height());

                //canvas.drawRect(location, paint);
                Imgproc.rectangle(mRgba, topLeft, bottomRight,
                        TF_FACE_RECT_COLOR, 6);

                //cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
            }
        }
    }

        @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                image.close();
                return;
            }
            mIsComputing = true;

            Trace.beginSection("imageAvailable");

            final Plane[] planes = image.getPlanes();

            // Initialize the storage bitmaps once when the resolution is known.
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                Log.d(TAG, String.format("Initializing at size %dx%d", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mRGBrotatedBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mRGBrotatedBitmapCopy = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);

                //mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i) {
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i) {
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888( mYUVBytes[0], mYUVBytes[1], mYUVBytes[2], mRGBBytes, mPreviewWdith, mPreviewHeight, yRowStride, uvRowStride, uvPixelStride, false);

            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.e(TAG, "Exception!", e);
            Trace.endSection();
            return;
        }

        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        drawUnmirroredRotatedBitmap(mRGBframeBitmap, mRGBrotatedBitmap, 0);
        drawUnmirroredRotatedBitmap(mRGBframeBitmap, mRGBrotatedBitmapCopy, 0);
            //drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        eVFOA = true;
                        mPreviousState = mState;
                        Utils.bitmapToMat(mRGBrotatedBitmap, mRgba);
                        faceDetectTF();
                        faceEyeDetect();
                        Utils.matToBitmap(mRgba, mRGBrotatedBitmap);

                        final long startTime = System.currentTimeMillis();
                        ArrayList<HeadPoseGaze> results;
                        synchronized (OnGetImageListener.this) {
                            results = mHeadPoseDetector.bitmapDetection(mRGBrotatedBitmapCopy);
                        }
                        final long endTime = System.currentTimeMillis();

                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPerformanceView.setText("Time cost\n" + String.valueOf((endTime - startTime) / 1000f) + " sec");
                            }
                        });

                        // Update the score textview with info on result
                        if(!results.isEmpty()) {
                            // Update performance timing
                            overallTime += ((endTime - startTime) / 1000f);
                            valid_cycles++;

                            final HeadPoseGaze r = results.get(0);
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mResultsView.setText("Gaze angles\nYaw: " + df.format(r.getYaw()) +
                                            "\nPitch: " + df.format(r.getPitch()) +
                                            "\nRoll: " + df.format(r.getRoll()) +
                                            "\nState: " + mState.toString());
                                }
                            });

                            //Initially assume studying from paper
                            if (ext_matchLoc != null && ext_eyes_only_rect != null) {
                                double match = ext_matchLoc.y;
                                double eye_area = ext_eyes_only_rect.y;// + ext_eyes_only_rect.height / 8;
                                org.opencv.core.Point match_p1 = new org.opencv.core.Point(0, ext_matchLoc.y);
                                org.opencv.core.Point match_p2 = new org.opencv.core.Point(mPreviewWdith, ext_matchLoc.y);

                                org.opencv.core.Point eye_p1 = new org.opencv.core.Point(0, ext_eyes_only_rect.y);
                                org.opencv.core.Point eye_p2 = new org.opencv.core.Point(mPreviewWdith, ext_eyes_only_rect.y);
                                Utils.bitmapToMat(mRGBrotatedBitmap, mRgba);
                                Imgproc.line(mRgba, match_p1, match_p2, FACE_RECT_COLOR, 2);
                                Imgproc.line(mRgba, eye_p1, eye_p2, TF_FACE_RECT_COLOR, 2);
                                Utils.matToBitmap(mRgba, mRGBrotatedBitmap);

                                if (mappedRecognitions.size() > 0) {
                                    if (ext_eyes_only_rect != null) {
                                        eVFOA = !(match < eye_area);
                                        Log.i(TAG, String.format("Setting eVFOA to %b; match = %f; eye_area = %f", eVFOA, match, eye_area));
                                    }
                                }
                            }

                            hVFOA = r.getYaw() < maxYaw && r.getYaw() > minYaw && r.getPitch() > minPitch;

                            Log.i(TAG, String.format("eVFOA: %b", eVFOA));
                            Log.i(TAG, String.format("hVFOA: %b", hVFOA));

                            if (hVFOA && eVFOA)
                                mState = State.PAYING_ATTENTION;
                            else
                                mState = State.NOT_PAYING_ATTENTION;

                            if (MainActivity.saveFile)
                                XMLWriter.addResult(detectionDocument, System.currentTimeMillis(), r.getYaw(), r.getPitch(), r.getRoll());

                        }
                        else {
                            if (mappedRecognitions.size() > 0) {
                                mState = State.NOT_PAYING_ATTENTION;
                                Log.i(TAG, "User not detected by HPE but found by Tensorflow");
                            }
                            else {
                                mState = State.NOT_THERE;
                                Log.i(TAG, "404: User not found");
                            }
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mResultsView.setText("Gaze angles\nYaw: ??"  +
                                            "\nPitch: ??" +
                                            "\nRoll: ??"  +
                                            "\nState: " + mOutputState.toString());
                                }
                            });
                        }

                        if (mState != mPreviousState)
                            tStart = System.currentTimeMillis();

                        tDelta = System.currentTimeMillis() - tStart;
                        Log.i(TAG, String.format("Time delta: %d;", tDelta));

                        if (mState == State.NOT_THERE && tDelta > timeOutPresence) {
                            mOutputState = mState;
                        } else if (mState == State.NOT_PAYING_ATTENTION && tDelta > timeOutAttention) {
                            mOutputState = mState;
                        } else if (mState == State.PAYING_ATTENTION){
                            mOutputState = mState;
                        }

                        mWindow.setRGBBitmap(mRGBrotatedBitmap);
                        mIsComputing = false;
                    }
                });

        Trace.endSection();
    }
}