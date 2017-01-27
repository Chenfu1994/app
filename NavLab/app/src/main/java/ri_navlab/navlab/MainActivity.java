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
import android.hardware.camera2.DngCreator;
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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.io.File;
import java.io.FileOutputStream;
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
import static ri_navlab.navlab.R.id.toggleButton;

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
    private String GALLERY_LOCATION = "image gallery";
    private String GALLERY_LOCATION_Raw = "Raw image gallery";
    // private String mImageFileLocation = "";
    private File mGalleryFolder;
    private File mGalleryFolder_raw;
    private int mFlashStatus = CMAERA_FLASH_UNUSED;
    private ToggleButton toggle;
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
    private ImageReader mImageReader;
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
            int format = mImage.getFormat();
            switch (format) {
                case ImageFormat.JPEG:
                    ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);

                    FileOutputStream fileOutputStream = null;
                    try {
                        System.out.println(mImageFileName);
                        fileOutputStream = new FileOutputStream(mImageFileName);
                        fileOutputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case ImageFormat.RAW_SENSOR:

                    System.out.println(mCameraCharacteristics);
                    System.out.println(mCaptureResult);
                    DngCreator dngCreator = new DngCreator(mCameraCharacteristics, mCaptureResult);
                    FileOutputStream rawFileOutputStream = null;
                    try {
                        rawFileOutputStream = new FileOutputStream(mImageFileName_raw);
                        dngCreator.writeImage(rawFileOutputStream, mImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mImage.close();
                        finishedCaptureLocked();
                        //unlockFocus();
                        //startPreview();
                        if (rawFileOutputStream != null) {
                            try {
                                rawFileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                    break;

            }
            mFlashStatus = CMAERA_FLASH_UNUSED;
//            unlockFocus();
        }

    }

    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult captureResult) {
            System.out.println("------------------------mCaptureState-----------------------------");
            System.out.println(mCaptureState);
            switch (mCaptureState) {
                case STATE_PREVIEW:
                    System.out.println("------------in STATE_PREVIEW-----------------------");
                    break;
                //case STATE_WAIT_LOCK:
                case STATE_WAIT_LOCK:
                    System.out.println("------------in STATE_WAIT_LOCK-----------------------");
                    mCaptureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);


                    //if (afState ==CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                    // unlockFocus();
                    //mCaptureState= STATE_PICTURE_CAPTURED;
                    startImageCapture();
                    Toast.makeText(getApplicationContext(), "AF LOCKED", Toast.LENGTH_SHORT).show();
                    //
                    //}


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
                    // mWaitingForFlash = false;
                    System.out.println("I am in falsh,-------------------------------");
                    // do the capture...
                    // mState = STATE_PICTURE_TAKEN;

                    startImageCapture();
                    Toast.makeText(getApplicationContext(), "AF LOCKED", Toast.LENGTH_SHORT).show();
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

    private ImageButton mImageButton;
    private ImageButton mFlashImageButton;
    private File mImageFolder;


    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createImageGallery();
        createImageGallery_Raw();

        mTextureView = (TextureView) findViewById(R.id.textureView);
        mImageButton = (ImageButton) findViewById(R.id.imageButton);
        toggle = (ToggleButton) findViewById(toggleButton);
        toggle.setChecked(false);
        toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                startPreview();
            }
        });

        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFlashStatus = CMAERA_FLASH_UNUSED;
             //   startPreview();
                setup3AControlsLocked(mCaptureRequestBuilder);
                lockFocus();

            }
        });

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mFlashImageButton = (ImageButton) findViewById(R.id.imageButton2);

        mFlashImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // startPreview();
                mFlashStatus = CMAERA_FLASH_USED;
                // mCaptureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 40000);
                //   mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);


                setup3AControlsLocked(mCaptureRequestBuilder);
                lockFocus();
            }
        });

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


    }


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
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "application no camera", Toast.LENGTH_SHORT).show();
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


        System.out.println("in image gallery");
        //File storageDirectory = getFilesDir();
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // mGalleryFolder= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +"/image gallery new/");
        mGalleryFolder = new File(storageDirectory, GALLERY_LOCATION);
        System.out.println(mGalleryFolder);
        if (!mGalleryFolder.exists()) {
            System.out.println("not created");
            if (mGalleryFolder.mkdirs()) {
                Toast.makeText(getApplicationContext(), "new file dir made", Toast.LENGTH_SHORT).show();
            }
        }

    }

    File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String timeStamp =generateTimestamp();
        String imageFileName = "IMAGE_" + timeStamp + "_";
        System.out.println(imageFileName);
        System.out.println(mGalleryFolder);
        File image = File.createTempFile(imageFileName, ".jpg", mGalleryFolder);
        System.out.println("in create image");
        mImageFileName = image.getAbsolutePath();
        System.out.println(mImageFileName);

        return image;

    }

    private void createImageGallery_Raw() {
        String state = Environment.getExternalStorageState();
        System.out.println("in image gallery");
        //File storageDirectory = getFilesDir();
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // mGalleryFolder= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +"/image gallery new/");
        mGalleryFolder_raw = new File(storageDirectory, GALLERY_LOCATION_Raw);
        System.out.println(mGalleryFolder_raw);
        if (!mGalleryFolder_raw.exists()) {
            System.out.println("not created");
            if (mGalleryFolder_raw.mkdirs()) {
                Toast.makeText(getApplicationContext(), "new file dir made", Toast.LENGTH_SHORT).show();
            }
        }

    }


    File createImageFile_Raw() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String timeStamp =generateTimestamp();
        String imageFileName = "IMAGE_" + timeStamp + "_RAW";
        System.out.println(imageFileName);
        System.out.println(mGalleryFolder_raw);
        File image = File.createTempFile(imageFileName, ".dng", mGalleryFolder_raw);
        System.out.println("in create image");
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
                System.out.println("---------------------mFlashAvailable------------------------");
                System.out.println(mFlashAvailable);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();

                mtotalRotation = sensorDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mtotalRotation == 90 || mtotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }


                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageSize_raw = chooseOptimalSize(map.getOutputSizes(ImageFormat.RAW_SENSOR), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 10);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
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
            setup3AControlsLocked(mCaptureRequestBuilder);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface(), mImageReader_raw.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "unable to setup camera preview", Toast.LENGTH_SHORT).show();

                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startImageCapture() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
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
                        //@Override
                        //public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,TotalCaptureResult result ){
                        //  super.onCaptureCompleted(session,request,result);

//                        }
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

        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            // Reset the autofucos trigger
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            setFlash(mCaptureRequestBuilder);
            // After this, the camera will go back to the normal state of preview.
            mCaptureState = STATE_PREVIEW;
            mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setFlash(CaptureRequest.Builder builder) {
        if (toggle != null && toggle.isChecked()) {
            // builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            //builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            // builder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
        } else {
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        }


    }

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

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);

        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO);

        Float minFocusDist =
                mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        mNoAFRun = (minFocusDist == null || minFocusDist == 0);

        if (!mNoAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            if (contains(mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {

                System.out.println("I am in mNoAFRun == true");
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else {
                System.out.println("I am in mNoAFRun == false");
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }


        if (contains(mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            Log.d(TAG, "it gets here A");
            if (toggle != null && toggle.isChecked()) {
                Log.d(TAG, "it gets here C");
                builder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                //builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            } else {
                Log.d(TAG, "it gets here D");
                builder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);

            }
        } else {
            Log.d(TAG, "it gets here B");
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
        }

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d(TAG, "Toggle is on");
                    //builder.set(CaptureRequest.FLASH_MODE,
                    //CaptureRequest.FLASH_MODE_TORCH);

                } else {
                    Log.d(TAG, "Toggle is off");
                    // builder.set(CaptureRequest.FLASH_MODE,
                    // CaptureRequest.FLASH_MODE_OFF);


                }
            }
        });


        // If there is an auto-magical white balance control mode available, use it.
        if (contains(mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }

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





