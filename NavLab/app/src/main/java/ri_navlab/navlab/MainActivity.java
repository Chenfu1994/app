package ri_navlab.navlab;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar.LayoutParams;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public static final int CMAERA_FLASH_USED = 1;
    public static final int CMAERA_FLASH_UNUSED = 0;
    public static final int STATE_WAITING_FOR_3A_CONVERGENCE = 3;
    private static final int STATE_PICTURE_CAPTURED = 2;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private boolean mNoAFRun = false;
    private int mCaptureState = STATE_PREVIEW;
    private CameraCharacteristics mCameraCharacteristics;
    private Boolean mFlashAvailable;
    private CaptureResult mCaptureResult;
    private TextureView mTextureView;
    private String mImageFileName;
    private String mImageFileName_raw;
    private String mImageFileName_raw_txt;
    private String GALLERY_LOCATION = "image gallery";
    private String GALLERY_LOCATION_Raw = "Raw image gallery";
    private ByteBuffer byteBuffer1;
    private ByteBuffer byteBuffer2;
    private byte[] bytes1;
    private byte[] bytes2;
    private Box box;
    private float x_change, y_change;
    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId;
    private float mPosX;
    private float mPosY;
    private static final int INVALID_POINTER_ID = -1;
    // private String mImageFileLocation = "";
    private File mGalleryFolder;
    private File mGalleryFolder_raw;
    private int mFlashStatus = CMAERA_FLASH_UNUSED;
    private int GLOBAL_STATE = 0;
    private short[] dataRed;
    private short[] dataRed2;
    private int START = 0;
    float[] a,b,c;
    private int viewSize_height, viewSize_width,textureSize_width,textureSize_height;
    int ii =0;
    int dist_inupt;
    Mat result;
    Mat redChannel1;
    Mat redChannel2;
    int total_num =0;
    float start_X = 0;
    float start_Y = 0;
    private boolean clicked  = true;
    // private int i = 1;
    private ToggleButton toggle;
    TextView myTextViewBlue_satr;
    TextView myTextViewRed_satr;
    TextView myTextViewGreen_satr;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(getApplicationContext(), "Preview Setted", Toast.LENGTH_SHORT).show();
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

    };
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {

            mCameraDevice = camera;

            startPreview();
            //Toast.makeText(getApplicationContext(),"camera connection made", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private String mCameraId;
    private Size mImageSize;
    //private ImageReader mImageReader;
    private ImageReader mImageReader_raw;
    private Size mImageSize_raw;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), mCaptureResult, mCameraCharacteristics));

                }
            };


    private final ImageReader.OnImageAvailableListener mOnImageRawAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage(), mCaptureResult, mCameraCharacteristics));

                }
            };


    private class ImageSaver implements Runnable {
        private final Image mImage;
        private final CameraCharacteristics mCameraCharacteristics;

        public ImageSaver(Image image, CaptureResult captureResult, CameraCharacteristics cameraCharacteristics) {
            mImage = image;
            mCaptureResult = captureResult;
            mCameraCharacteristics = cameraCharacteristics;
        }

        @Override
        public void run() {
            int i, number;
            int format = mImage.getFormat();
            switch (format) {
                case ImageFormat.RAW_SENSOR:
                    if (mFlashStatus == 0) {
                        byteBuffer1 = mImage.getPlanes()[0].getBuffer();
                        bytes1 = new byte[byteBuffer1.remaining()];
                        byteBuffer1.get(bytes1);
                        System.out.println("---------------byte array in raw image 1 ---------------------");
                        //unlockFocus();
                        mImage.close();
                        //finishedCaptureLocked();

                    }
                    if(mFlashStatus == 1){
                        byteBuffer2 = mImage.getPlanes()[0].getBuffer();
                        bytes2 = new byte[byteBuffer2.remaining()];
                        byteBuffer2.get(bytes2);
                        mFlashStatus = 0;
                        System.out.println("---------------byte array in raw image 2---------------------");
                        mImage.close();
                        //finishedCaptureLocked();

                       // unlockFocus();

                    }


                    // System.out.println(mCameraCharacteristics);
                    // System.out.println(mCaptureResult);
                   /* DngCreator dngCreator = new DngCreator(mCameraCharacteristics, mCaptureResult);
                    FileOutputStream rawFileOutputStream = null;
                    try {

                        rawFileOutputStream = new FileOutputStream(mImageFileName_raw);
                        dngCreator.writeImage(rawFileOutputStream, mImage);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        //finishedCaptureLocked();
                        //unlockFocus();
                        //startPreview();
                        if (rawFileOutputStream != null) {
                            try {
                                rawFileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }*/
                    break;



/*
                    byte[] bytes1 = new byte[byteBuffer1.remaining()];
                    byteBuffer1.get(bytes1);
                    short[] shorts = new short[bytes1.length / 2];


                    for (i = 0; i < bytes1.length / 2; i++)  //changed from i = 0 to bytes1.length/2
                    {
                        int int_1 = toUnsignedInt(bytes1[2 * i + 1]);
                        int int_2 = toUnsignedInt(bytes1[2 * i]);
                        shorts[i] = twoBytesToShort(int_1, int_2);   //problems with doing this linearly, changed indices of shorts array


                    }

                    System.out.println("done1!!!!!!!!!!!!!");
                    Mat mat = new Mat(mImageSize_raw.getHeight(), mImageSize_raw.getWidth(), CvType.CV_16U);
                    mat.put(0, 0, shorts);
                    DataOutputStream fos1 = null;
                    DataOutputStream fos = null;




                    int x0 = mImageSize_raw.getWidth() / 2;
                    int y0 = mImageSize_raw.getHeight() / 2;
                    int dx = mImageSize_raw.getHeight() / 6;
                    int dy = mImageSize_raw.getHeight() / 6;
                    System.out.println(x0);
                    System.out.println(y0);
                    System.out.println(dx);
                    System.out.println(dy);

                    Rect rect = new Rect(x0 -  dx , y0 -  dy , 2 * dx, 2 * dy);
                    System.out.println(x0 -  dx);
                    System.out.println(y0 -  dy);

                    Mat mat_new = mat.submat(rect);
                    int heights = mat_new.height();
                    int widths = mat_new.width();
                    System.out.println(heights);
                    System.out.println(widths);
                    short[] data = new short[heights * widths]; //data of small region
                    mat_new.get(0, 0, data);
                    System.out.println("done2!!!!!!!!!!!!!");





                    //Mat redChannel = new Mat(heights / 2, widths / 2, CvType.CV_16U); //allocate correct size for Green
                    /*for (int k = 0; k < mat_new.size().height; k++) {
                        //System.out.println("---Traversing matrix of pixels----");
                       for (int j = 0; j < mat_new.size().width; j++) {
                           if (k % 2 == 1 && j % 2 == 0) {
                                redChannel.put((k ) / 2, (j ) / 2, mat_new.get(k, j));

                            }
                        }
                    }
                    if (mFlashStatus == 0) {
                        //dataRed = new short[heights * widths / 4];
                        //redChannel.get(0, 0, dataRed);
                        //redChannel1 = redChannel;

                        System.out.println("----red channel 1 get!------");
                        try {
                            System.out.println("----txt file found here------");
                            fos1 = new DataOutputStream(new FileOutputStream(mImageFileName_raw_txt));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            System.out.println("---------------From raw image write image 1---------------------");
                            for (int j = 0; j < data.length; j++) {
                                fos1.writeShort(data[j]);
                            }
                            fos1.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mFlashStatus == 1) {
                       // dataRed2 = new short[heights * widths / 4];
                       // redChannel.get(0, 0, dataRed2);
                       // redChannel2 = redChannel;
                        System.out.println("----red channel 2 get!------");
                        if (mFlashStatus == 1) {
                            mFlashStatus = 0;
                        }

                        try {
                            System.out.println("----txt file found here------");
                            fos = new DataOutputStream(new FileOutputStream(mImageFileName_raw_txt));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            System.out.println("---------------From raw image write image 2---------------------");
                            for (int j = 0; j < data.length; j++) {
                                fos.writeShort(data[j]);
                            }
                            fos.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                    System.out.println("Red data has been written");


                    System.out.println(bytes1.length);*/


                    //break;

            }

//            unlockFocus();
        }

    }

    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mPreviewCaptureSession;
    TextView myTextViewBlue;
    TextView myTextViewRed;
    TextView myTextViewGreen;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult captureResult) {

            //System.out.println(mCaptureState);
            switch (mCaptureState) {
                case STATE_PREVIEW:
                    break;
                case STATE_WAIT_LOCK:
                         System.out.println("------------in STATE_WAIT_LOCK-----------------------");
                    mCaptureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);


                    startImageCapture();


                    break;
            }
        }


        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (toggle != null && toggle.isChecked()) {
                mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                Integer flashState = result.get(CaptureResult.FLASH_STATE);
                if ((flashState != null && flashState == CaptureResult.FLASH_STATE_FIRED)) {
                    System.out.println("in on captureCompleted");

                    startImageCapture();

                    mCaptureResult = result;

                    return;  // don't call process()
                }
            }
            process(result);
            //unlockFocus();
            mCaptureResult = result;




        }
    };


    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private int mtotalRotation;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }


    private Button mImageButton3;
    private Button clickButton;
    private ImageButton mFlashImageButton, mImageButton2;
    private File mImageFolder;
    private int picture_number = 1;

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private BaseLoaderCallback mloaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("opencv", "Opencv loaded successfully");

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
            }

        }
    };

    private  EditText editText;
    private  ImageButton mImageButton1;
    private TextWatcher listenerTextChangedStatus = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {


        }


            @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
                Log.e(TAG, "Please input distance");

        }

        @Override
        public void afterTextChanged(Editable s) {
            boolean test = true;
            try {
                Integer.parseInt(editText.getText().toString());
            } catch(NumberFormatException e) {
                test = false;
                Toast.makeText(getApplicationContext(), "Please input integer distance", Toast.LENGTH_LONG).show();
            }
            if(!editText.getText().toString().isEmpty() && test){
                mImageButton1.setEnabled(true);
                // do action

            }else{
                mImageButton1.setEnabled(false);
                System.out.println("in not enter distance");
                Toast.makeText(getApplicationContext(), "Please input integer distance", Toast.LENGTH_LONG).show();
            }

        }

    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ri_navlab.navlab.R.layout.activity_main);
        //createImageGallery();
        //createImageGallery_Raw();
        mImageButton1 = (ImageButton) findViewById(ri_navlab.navlab.R.id.imageButton1);
        mImageButton1.setEnabled(false);
        mImageButton2 = (ImageButton) findViewById(ri_navlab.navlab.R.id.imageButton2);
        box = new Box(this, 0, 0);
        box.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ViewGroup layout = (ViewGroup) findViewById(ri_navlab.navlab.R.id.frameView);
        layout.addView(box);
        mTextureView = (TextureView) findViewById(ri_navlab.navlab.R.id.textureView);
       // mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        FrameLayout rootView = (FrameLayout) findViewById(R.id.frameView);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float X , Y;
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        start_X = motionEvent.getX();
                        start_Y = motionEvent.getY();
                        System.out.println("in action down");
                        System.out.println("start_X:" + start_X);
                        System.out.println("start_Y:" + start_Y);
                        break;
                    case MotionEvent.ACTION_UP:
                        int[] size_box = box.getValue();
                        System.out.println("in action up");
                         textureSize_width = mTextureView.getWidth();
                         textureSize_height = mTextureView.getHeight();
                        double size = Math.sqrt(Math.pow(size_box[0] , 2) + Math.pow(size_box[1], 2));
                        System.out.println("x");
                        X = motionEvent.getX(0) - start_X;
                        Y = motionEvent.getY(0) - start_Y;
                        int k = 1;
                        if(X < 0)
                        {
                            k = -1;
                        }
                        System.out.println("end_x:" + motionEvent.getX(0));
                        System.out.println("end_y:" + motionEvent.getY(0));
                        System.out.println("start_X:" + start_X);
                        System.out.println("start_Y:" + start_Y);
                        System.out.println("x:" + X);
                        System.out.println("y:" + Y);
                        double distance = Math.sqrt(Math.pow(X , 2) + Math.pow(Y, 2));
                        System.out.println("distance:" + distance);
                        System.out.println("size:" + size);
                        double rate = distance / size;
                        System.out.println("rate:" + rate*2);
                        System.out.println("textureSize_width:" + (int)(textureSize_width * (1+ k * rate*2)));
                        System.out.println("textureSize_height:" + (int)(textureSize_height * (1+ k * rate*2)));
                        updateTextureViewSize_touch((int)(textureSize_width * (1+ k * rate*2)), (int)(textureSize_height * (1 +k * rate*2)));
                        break;
                }
                return true;
            }
        });

        //mTextureView.setOnTouchListener(new CustomTouchListener());
        //System.out.println(mTextureView.getHeight());

        editText = (EditText) findViewById(ri_navlab.navlab.R.id.edit_text);

        String s = editText.getText().toString();

        editText.addTextChangedListener(listenerTextChangedStatus );

        mImageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




            }
        });
        mImageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int inputInt = 0;
                try {
                    setup3AControlsLocked(mCaptureRequestBuilder);
                    System.out.println("I am in createCaptureSession and onConfigured");
                    mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                //mFlashStatus = CMAERA_FLASH_UNUSED;
                //onPause();
                //onResume();
                myTextViewBlue_satr = (TextView) findViewById(R.id.blue_saturated);
                myTextViewRed_satr = (TextView) findViewById(R.id.red_saturated);
                myTextViewGreen_satr = (TextView) findViewById(R.id.green_saturated);

                myTextViewBlue  = (TextView) findViewById(R.id.result_blue);
                myTextViewRed = (TextView) findViewById(R.id.result_red);
                myTextViewGreen = (TextView) findViewById(R.id.result_green);


                //dist_inupt = Integer.valueOf(editText.getText().toString());

                bytes1 = null;
                bytes2 = null;
                System.out.println("taking the first picture");
                mFlashStatus = CMAERA_FLASH_UNUSED;


                lockFocus();
                System.out.println(mFlashStatus);

                 try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("taking the second picture");
                mFlashStatus = CMAERA_FLASH_USED;



                lockFocus();
                System.out.println(mFlashStatus);

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }



                int[] index = box.getValue();
                       /* System.out.println("textureView width");
                        System.out.println(mTextureView.getWidth());
                        System.out.println("textureView height");
                        System.out.println(mTextureView.getHeight());
                        System.out.println("box_size: height");
                        System.out.println((int) index[3]);
                        System.out.println("box_size: width");
                        System.out.println((int) index[2]);
                        System.out.println("textureSize_height");
                        System.out.println(index[1]*2);
                        System.out.println("mImageSize_raw_height");
                        System.out.println(mImageSize_raw.getHeight());
                        System.out.println("index[1]*2");
                        System.out.println(index[1]*2);
                        System.out.println("mImageSize_raw_width");
                        System.out.println(mImageSize_raw.getWidth());
                        System.out.println("index[0]*2");
                        System.out.println(index[0]*2);*/

                float textviewToImage =  (float)mImageSize_raw.getHeight() / ((float)index[0] * 2);
                float rates = (float)mTextureView.getWidth() / (float)(index[0] * 2) ;
                System.out.println("rates");
                System.out.println(rates);
                int box_height_new = (int) (textviewToImage * index[2] * 2 / rates);
                int box_width_new = (int) (textviewToImage * index[3] * 2 / rates);
                if (box_height_new % 2 == 1){
                    box_height_new =  box_height_new +1;
                }
                if (box_width_new % 2 == 1){
                    box_width_new = box_width_new +1;
                }
                        /*System.out.println("box_height_new");
                        System.out.println(box_height_new);
                        System.out.println("box_width_new");
                        System.out.println(box_width_new);*/
                int x_start = mImageSize_raw.getHeight() / 2- box_height_new / 2;
                int y_start = mImageSize_raw.getWidth() / 2 - box_width_new / 2;
                        /*System.out.println("x_start");
                        System.out.println(x_start);
                        System.out.println("y_start");
                        System.out.println(y_start);*/
                RetroReflectivity retro = new RetroReflectivity(bytes1 , bytes2, y_start, x_start, box_width_new, box_height_new, mImageSize_raw.getHeight(), mImageSize_raw.getWidth(), 5);
                System.out.println("begin calculate");
                int test = retro.get_partition();
                System.out.println("partition get!!!!");
                a = retro.retoreFlectivityBlue();
                System.out.println("calculate blue!!!!");
                b = retro.retoreFlectivityRed();
                System.out.println("calculate red!!!!");
                c = retro.retoreFlectivityGreen();
                System.out.println("calculate green!!!!");




                myTextViewBlue.setText(String.valueOf(a[0]));
                myTextViewRed.setText(String.valueOf(b[0]));
                myTextViewGreen.setText(String.valueOf(c[0]));

                if (a[1] == 1) {
                    myTextViewBlue_satr.setText("saturate");
                } else {
                    myTextViewBlue_satr.setText("");
                }
                if (b[1] == 1) {
                    myTextViewRed_satr.setText("saturate");
                } else {
                    myTextViewRed_satr.setText("");
                }
                if (c[1] == 1) {
                    myTextViewGreen_satr.setText("saturate");
                } else {
                    myTextViewGreen_satr.setText("");
                }


                // System.out.println(c[0]);
                //System.out.println(c[1]);
                //retro.clearall();
                retro = null;
                bytes1 = null;
                bytes2 = null;

                onPause();
                onResume();






            }
        });

        return;

    }




    //protected  void cal

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();

        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        }

        if(START == 0){
            if (!OpenCVLoader.initDebug()) {
                Log.d("OpenCv", "lib not found");
                System.out.println("OpenCv lib not found");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mloaderCallback);
            } else {
                Log.d("OpenCV", "opencv found");
                System.out.println("OpenCv found");
                mloaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
            START = 1;
        }

    }


  /*  @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "application no camera", Toast.LENGTH_SHORT).show();
            }
        }
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Application not run without camera services",Toast.LENGTH_SHORT).show();
            }
            if(grantResults[1] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Application will not have an audio on record",Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onPause() {
        closeCamera();
        stopbackgroundThread();

        super.onPause();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        }
    }


    private void createImageGallery() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.d("myAppName", "Error: external storage is unavailable");
            return;
        }
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.d("myAppName", "Error: external storage is read only.");
            return;
        }
        Log.d("myAppName", "External storage is not read only or unavailable");

        if (ContextCompat.checkSelfPermission(this, // request permission when it is not granted.
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("myAppName", "permission:WRITE_EXTERNAL_STORAGE: NOT granted!");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


        //System.out.println("in image gallery");
        //File storageDirectory = getFilesDir();
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // mGalleryFolder= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +"/image gallery new/");
        mGalleryFolder = new File(storageDirectory, GALLERY_LOCATION);
        //System.out.println(mGalleryFolder);
        if (!mGalleryFolder.exists()) {
            //  System.out.println("not created");
            if (mGalleryFolder.mkdirs()) {
                Toast.makeText(getApplicationContext(), "new file dir made", Toast.LENGTH_SHORT).show();
            }
        }

    }

    File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + "_";
        File image = File.createTempFile(imageFileName, ".jpg", mGalleryFolder);

        mImageFileName = image.getAbsolutePath();
        return image;

    }

    private void createImageGallery_Raw() {
        String state = Environment.getExternalStorageState();
       File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // mGalleryFolder= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +"/image gallery new/");
        mGalleryFolder_raw = new File(storageDirectory, GALLERY_LOCATION_Raw);
        //System.out.println(mGalleryFolder_raw);
        if (!mGalleryFolder_raw.exists()) {
            //  System.out.println("not created");
            if (mGalleryFolder_raw.mkdirs()) {
                Toast.makeText(getApplicationContext(), "new file dir made", Toast.LENGTH_SHORT).show();
            }
        }

    }


    File createImageFile_Raw() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String timeStamp =generateTimestamp();
        String imageFileName = "IMAGE_" + timeStamp + "_RAW";
        //System.out.println(imageFileName);
        //System.out.println(mGalleryFolder_raw);
        File image = File.createTempFile(imageFileName, ".dng", mGalleryFolder_raw);
        File text = File.createTempFile(imageFileName, ".txt", mGalleryFolder_raw);
        //System.out.println("in create image");
        mImageFileName_raw_txt = text.getAbsolutePath();
        mImageFileName_raw = image.getAbsolutePath();
        System.out.println(mImageFileName_raw);

        return image;

    }


    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(cameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();

               // mtotalRotation = sensorDeviceRotation(cameraCharacteristics, deviceOrientation);
               // boolean swapRotation = mtotalRotation == 90 || mtotalRotation == 270;
               // int rotatedWidth = width;
               // int rotatedHeight = height;
               // if (swapRotation) {
               //     rotatedWidth = height;
               //     rotatedHeight = width;
               // }


               // mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
               // mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
               // mImageSize_raw = chooseOptimalSize(map.getOutputSizes(ImageFormat.RAW_SENSOR), rotatedWidth, rotatedHeight);
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), width, height);
                mImageSize_raw = chooseOptimalSize(map.getOutputSizes(ImageFormat.RAW_SENSOR), width, height);
                // mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 10);
               // mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                mImageReader_raw = ImageReader.newInstance(mImageSize_raw.getWidth(), mImageSize_raw.getHeight(), ImageFormat.RAW_SENSOR, 10);
                mImageReader_raw.setOnImageAvailableListener(mOnImageRawAvailableListener, mBackgroundHandler);
                mCameraId = cameraId;
                mCameraCharacteristics = cameraCharacteristics;
                return;

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                    System.out.println("camera opened in connect camera");
                } else {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "video_app required access to camera ", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        Surface previewSurface = new Surface(surfaceTexture);
        try {

            //if (mFlashStatus != 0){
            //  mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            //mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            //}
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            //setFlash(mCaptureRequestBuilder);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader_raw.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    try {

                        setup3AControlsLocked2(mCaptureRequestBuilder);



                        System.out.println("I am in createCaptureSession and onConfigured");
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                 //   onResume();
                    Toast.makeText(getApplicationContext(), "unable to setup camera preview", Toast.LENGTH_SHORT).show();

                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




    private void startImageCapture() {
        try {
            List<CaptureRequest> captureList = new ArrayList<CaptureRequest>();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.addTarget(mImageReader_raw.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mtotalRotation);
            // mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CameraCaptureSession.CaptureCallback imageCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                            try {
                                System.out.println("in startImageCapture");
                                createImageFile();
                                createImageFile_Raw();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result ){
                          super.onCaptureCompleted(session, request, result);


                       }
                    };
            //mPreviewCaptureSession.stopRepeating();
            //mState = STATE_WAITING_FOR_3A_CONVERGENCE;
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), imageCaptureCallback, null);

            // unlockFocus();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("raw_image_camera");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());

    }

    private void stopbackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private static int sensorDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width && option.getWidth() >= width &&
                    option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private void lockFocus() {
        mCaptureState = STATE_WAIT_LOCK;
        // if (mFlashStatus != 0){
        //  // mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        // mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
        //mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        //}
        //else{
        //  mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        //}
        // setFlash(mCaptureRequestBuilder);

       setup3AControlsLocked(mCaptureRequestBuilder);
        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

  /*  private void unlockFocus() {
        try {
            // Reset the autofucos trigger
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //setFlash(mCaptureRequestBuilder);
            // After this, the camera will go back to the normal state of preview.
            mCaptureState = STATE_PREVIEW;
            mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/


    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }



    private void setup3AControlsLocked(final CaptureRequest.Builder builder) {
        Log.d(TAG, "setup3AControlsLocked is called");
        System.out.println("setup3AControlsLocked is called");
        // ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);

        // Enable auto-magical 3A run by camera device
       // if(flag == true){
            builder.set(CaptureRequest.CONTROL_MODE,
                    CaptureRequest.CONTROL_MODE_AUTO);
       // builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(50000));
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, 2500);
      //  }else
      /*  {
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));

        }*/


        //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

        Float minFocusDist =
                mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = (minFocusDist == null || minFocusDist == 0);
        //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                System.out.println("I am in mNoAFRun == true");
               // if(flag == true){
                    System.out.println("I am in true");
                    builder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(50000));
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, 2500);

                //    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            //    }

           /*     else
                {
                    System.out.println("I am in false");
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

                     builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                     builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/

            } else {
                System.out.println("I am in mNoAFRun == false");
                //if(flag == true){
                    System.out.println("I am in true");
                    builder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(50000));
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, 2500);

               //     builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
               // }

              /*  else
                {
                    System.out.println("I am in false");
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/
                //builder.set(CaptureRequest.CONTROL_AF_MODE,
                //       CaptureRequest.CONTROL_AF_MODE_AUTO);
            //    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            //    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(900000000));
            //    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 5000);
            }
        }


        if (contains(mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
           // Log.d(TAG, "it gets here A");
            System.out.println("it gets here A");
            //if ((toggle != null && toggle.isChecked())||mFlashStatus == 1) {
            if (mFlashStatus == 1) {
                System.out.println("it gets here C");
                Log.d(TAG, "it gets here C");


               // if(flag == true){
                    System.out.println("I am in true");
                    builder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);

                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(50000));
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, 2500);

                   // builder.set(CaptureRequest.FLASH_MODE,
                    //        CaptureRequest.FLASH_MODE_OFF);
                    //  builder.set(CaptureRequest.CONTROL_AF_MODE,
                    //          CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
             //       builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
             //   }

               /* else
                {
                    System.out.println("I am in false");
                    //builder.set(CaptureRequest.FLASH_MODE,
                   //         CaptureRequest.FLASH_MODE_OFF);
                    builder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/
                //builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            } else {
                Log.d(TAG, "it gets here D");
                System.out.println("it gets here D");

               // if(flag == true){
                    System.out.println("I am in true");
                    builder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(50000));
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, 2500);

                  //  builder.set(CaptureRequest.CONTROL_AF_MODE,
                  //          CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
               // }

              /*  else
                {
                    System.out.println("I am in false");
                    builder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/
              //  builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

                //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(900000000));
                //builder.set(CaptureRequest.SENSOR_SENSITIVITY, 5000);

            }
        } else {
            Log.d(TAG, "it gets here B");
            System.out.println("it gets here b");


                System.out.println("I am in true");
             //   builder.set(CaptureRequest.CONTROL_AE_MODE,
               //         CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(50000));
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, 2500);

            //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long());
            //    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);

          /*  else
            {
                System.out.println("I am in false");
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
            }*/

            //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(900000000));
            //builder.set(CaptureRequest.SENSOR_SENSITIVITY, 5000);
        }


        // If there is an auto-magical white balance control mode available, use it.
      /*  if (contains(mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }*/

/*        try {
            mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }*/

    }
    private void setup3AControlsLocked2(final CaptureRequest.Builder builder) {
        Log.d(TAG, "setup3AControlsLocked2 is called");
        System.out.println("setup3AControlsLocked2 is called");
        // ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);

        // Enable auto-magical 3A run by camera device
        // if(flag == true){
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO);
        // builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

        //  }else
      /*  {
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));

        }*/


        //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

        Float minFocusDist =
                mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = (minFocusDist == null || minFocusDist == 0);
        //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                System.out.println("I am in mNoAFRun == true");
                // if(flag == true){
                System.out.println("I am in true");
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                //    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                //    }

           /*     else
                {
                    System.out.println("I am in false");
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

                     builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                     builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/

            } else {
                System.out.println("I am in mNoAFRun == false");
                //if(flag == true){
                System.out.println("I am in true");
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                //     builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                // }

              /*  else
                {
                    System.out.println("I am in false");
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/
                //builder.set(CaptureRequest.CONTROL_AF_MODE,
                //       CaptureRequest.CONTROL_AF_MODE_AUTO);
                //    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
                //    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(900000000));
                //    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 5000);
            }
        }


        if (contains(mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            // Log.d(TAG, "it gets here A");
            System.out.println("it gets here A");
            //if ((toggle != null && toggle.isChecked())||mFlashStatus == 1) {
            if (mFlashStatus == 1) {
                System.out.println("it gets here C");
                Log.d(TAG, "it gets here C");


                // if(flag == true){
                System.out.println("I am in true");
                builder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);

                // builder.set(CaptureRequest.FLASH_MODE,
                //        CaptureRequest.FLASH_MODE_OFF);
                //  builder.set(CaptureRequest.CONTROL_AF_MODE,
                //          CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //       builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                //   }

               /* else
                {
                    System.out.println("I am in false");
                    //builder.set(CaptureRequest.FLASH_MODE,
                   //         CaptureRequest.FLASH_MODE_OFF);
                    builder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/
                //builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            } else {
                Log.d(TAG, "it gets here D");
                System.out.println("it gets here D");

                // if(flag == true){
                System.out.println("I am in true");
                builder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);


                //  builder.set(CaptureRequest.CONTROL_AF_MODE,
                //          CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                // }

              /*  else
                {
                    System.out.println("I am in false");
                    builder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
                }*/
                //  builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

                //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(900000000));
                //builder.set(CaptureRequest.SENSOR_SENSITIVITY, 5000);

            }
        } else {
            Log.d(TAG, "it gets here B");
            System.out.println("it gets here b");


            System.out.println("I am in true");
              builder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);


            //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long());
            //    builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);

          /*  else
            {
                System.out.println("I am in false");
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(7000000));
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
            }*/

            //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, new Long(900000000));
            //builder.set(CaptureRequest.SENSOR_SENSITIVITY, 5000);
        }


        // If there is an auto-magical white balance control mode available, use it.
      /*  if (contains(mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }*/

/*        try {
            mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }*/

    }


    /*private void finishedCaptureLocked() {
        //try {
        // Reset the auto-focus trigger in case AF didn't run quickly enough.
        if (!mNoAFRun) {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);


            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_IDLE);

        }


    }*/

    private short twoBytesToShort(int b1, int b2) {
        return (short) ((b1 << 8) | b2);
    }

    public int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    /* class AThread implements Runnable {
         Thread t;
         Thread threadToWaitFor;

         AThread(Thread threadToWaitFor) {
             t = new Thread(this);
             this.threadToWaitFor = threadToWaitFor;
         }

         public void run() {
             try {
                 threadToWaitFor.join();
                // t.sleep(5000);

                 System.out.println("taking the second picture");
                 mFlashStatus = CMAERA_FLASH_USED;
                 setup3AControlsLocked(mCaptureRequestBuilder);
                 lockFocus();
                 System.out.println(mFlashStatus);

             } catch (Exception e) {
                 e.printStackTrace();
             }
         }
     }
     class BThread implements Runnable {
             Thread t;

             BThread() {
                 t = new Thread(this);
             }

             public void run() {
                 try {
                     System.out.println("taking the first picture");
                     mFlashStatus = CMAERA_FLASH_UNUSED;
                     setup3AControlsLocked(mCaptureRequestBuilder);
                     lockFocus();
                     System.out.println(mFlashStatus);

                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }

         }*/
    private void updateTextureViewSize_touch(int viewWidth, int viewHeight) {
        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(viewWidth, viewHeight);
        layout.gravity = Gravity.CENTER;
        mTextureView.setLayoutParams(layout);
    }


    private void finishedCaptureLocked() {
        //try {
        // Reset the auto-focus trigger in case AF didn't run quickly enough.
        if (!mNoAFRun) {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);


            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            // }
            //} catch (CameraAccessException e) {
            //    e.printStackTrace();
            //}
        }


    }





}







