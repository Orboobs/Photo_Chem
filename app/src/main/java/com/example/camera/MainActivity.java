package com.example.camera;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public ImageView cartinca;
    //public ImageView ugolLV;
    public ImageView text;
    public TextView cordy;
    public float x0;
    public float x1;
    public float xu;
    public float cx;
    public float x;
    public float y0;
    public float y1;
    public float yu;
    public float cy;
    public float y;
    public float realX;
    public float realY;
    public float deltaX;
    public float deltaY;
    public int width;
    public int heigh;


    public static final String LOG_TAG = "myLogs";


    CameraService[] myCameras = null;

    private CameraManager mCameraManager    = null;
    private final int CAMERA1   = 0;
    private final int CAMERA2   = 1;

    private Button mButtonOpenCamera1 = null;
    private Button mButtonOpenCamera2 = null;
    private Button mButtonToMakeShot = null;
    private TextureView mImageView = null;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;



    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private GestureDetectorCompat GestureDetector;


    //@RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("MissingInflatedId")
   //@SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cartinca = findViewById(R.id.imageView);
        text = findViewById(R.id.imageView2);
        cordy = findViewById(R.id.textView);
        cartinca.setOnTouchListener(touchListener);


        //cartinca.setX(10);
        //cartinca.setY(30);
        //cordy.setY(400);

        //GestureDetector = new GestureDetectorCompat();


        Log.d(LOG_TAG, "Запрашиваем разрешение");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        )
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }


        mButtonOpenCamera1 =  findViewById(R.id.button4);
        mButtonOpenCamera2 =  findViewById(R.id.button5);
        mButtonToMakeShot =findViewById(R.id.button6);
        mImageView = findViewById(R.id.textureView);

        mButtonOpenCamera1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA2].isOpen()) {myCameras[CAMERA2].closeCamera();}
                if (myCameras[CAMERA1] != null) {
                    if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera();
                }
            }
        });

        mButtonOpenCamera2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA1].isOpen()) {myCameras[CAMERA1].closeCamera();}
                if (myCameras[CAMERA2] != null) {
                    if (!myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].openCamera();
                }
            }
        });


        mButtonToMakeShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {




                if (myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].makePhoto();
                if (myCameras[CAMERA2].isOpen()) myCameras[CAMERA2].makePhoto();

                float poxf = cartinca.getX() + cartinca.getWidth() / 2;
                int pox = Math.round(poxf);
                float poyf = cartinca.getY() + cartinca.getHeight() / 2;
                int poy = Math.round(poyf);
                Bitmap originalBitmap = mImageView.getBitmap();
                Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, pox, poy, cartinca.getWidth(), cartinca.getHeight());
                text.setImageBitmap(croppedBitmap);

            }
        });


        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            // Получение списка камер с устройства


            myCameras = new CameraService[mCameraManager.getCameraIdList().length];



            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: "+cameraID);
                int id = Integer.parseInt(cameraID);


                // создаем обработчик для камеры
                myCameras[id] = new CameraService(mCameraManager,cameraID);




            }
        }
        catch(CameraAccessException e){
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }


    }


    public class CameraService {

        private File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test1.jpg");;
        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mCaptureSession;
        private ImageReader mImageReader;




        public CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;

        }

        public void makePhoto (){





            try {
                // This is the CaptureRequest.Builder that we use to take a picture.
                final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(mImageReader.getSurface());
                CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {


                    }
                };

                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, mBackgroundHandler);
            }
            catch (CameraAccessException e) {
                e.printStackTrace();


            }




        }


        private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
                = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {

                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));


            }

        };


        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:"+mCameraDevice.getId());

                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:"+mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:"+camera.getId()+" error:"+error);
            }

        };


        private void createCameraPreviewSession() {

            mImageReader = ImageReader.newInstance(1920,1080, ImageFormat.JPEG,1);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

            SurfaceTexture texture = mImageView.getSurfaceTexture();

            texture.setDefaultBufferSize(1920,1080);
            Surface surface = new Surface(texture);

            try {
                final CaptureRequest.Builder builder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                builder.addTarget(surface);




                mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mCaptureSession = session;
                                try {
                                    mCaptureSession.setRepeatingRequest(builder.build(),null,mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) { }}, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }




        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera() {
            try {

                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {


                    mCameraManager.openCamera(mCameraID,mCameraCallback,mBackgroundHandler);

                }



            } catch (CameraAccessException e) {
                Log.i(LOG_TAG,e.getMessage());

            }
        }

        public void closeCamera() {

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }



    }


    @Override
    public void onPause() {
        if(myCameras[CAMERA1].isOpen()){myCameras[CAMERA1].closeCamera();}
        if(myCameras[CAMERA2].isOpen()){myCameras[CAMERA2].closeCamera();}
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();


    }


    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    final View.OnTouchListener touchListener = new View.OnTouchListener() {
        //@SuppressLint("WrongViewCast")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageView ugolLV = findViewById(R.id.ugolLV);
            ImageView ugolPV = findViewById(R.id.ugolPV);
            ImageView ugolPN = findViewById(R.id.ugolPN);
            ImageView ugolLN = findViewById(R.id.ugolLN);



            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                x = cartinca.getX();
                y = cartinca.getY();
                xu = event.getX();
                yu = event.getY();
                x0 = event.getX();
                y0 = event.getY();
                //x = cartinca.getX();
                cy = cartinca.getY();
                //realX = cartinca.getWidth();
                //realY = cartinca.getHeight();
                //realX = cartinca.getWidth();
                //realY = cartinca.getHeight();
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                cx = cartinca.getX();
                //cy = cartinca.getY();
                realX = cartinca.getWidth();
                realY = cartinca.getHeight();
                deltaX = 0;
                deltaY = 0;
                x1 = event.getX();
                y1 = event.getY();
                //x = event.getX();
                //y = event.getY();
                deltaX = x1 - x0;
                deltaY = y1 - y0;

                //RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) cartinca.getLayoutParams();

                if ((realY <= 400) && (deltaY >= 0)) {

                    //deltaX = (x - x0) / 1;
                    //deltaY = (y - y0) / 1;
                    heigh = (int) (realY + deltaY);
                }
                if ((realY >= 200) && (deltaY <= 0)) {
                    //x = event.getX();
                    //y = event.getY();
                    //deltaX = (x - x0) / 1;
                    //deltaY = (y - y0) / 1;
                    heigh = (int) (realY + deltaY);
                }
                // realY = cartinca.getHeight();

                if ((realX <= 700) && (deltaX >= 0)) {
                    //x = event.getX();
                    //y = event.getY();
                    //deltaX = (x - x0) / 1;
                    //deltaY = (y - y0) / 1;
                    width = (int) (realX + deltaX); //+
                    //va = (int) (realX + deltaX );
                }
                if ((realX >= 200) && (deltaX <= 0)) {
                    //x = event.getX();
                    //y = event.getY();
                    //deltaX = (x - x0) / 1;
                    //deltaY = (y - y0) / 1;
                    width = (int) (realX + deltaX); //+
                }

                cartinca.setLayoutParams(new ViewGroup.LayoutParams(width, heigh)); //new ViewGroup.LayoutParams(width, heigh)
                cartinca.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cartinca.requestLayout();
                cartinca.setY(y);
                cartinca.setX(x);
                x0 = x1;
                y0 = y1;

                //cartinca.setX(451);
                // realX = cartinca.getWidth();
                //} //&& |
                //float sdvig = ();

                //cordy.setText(Float.toString(realX));
                cordy.setY(cordy.getY() + (realY - heigh) / 2); //- heigh + realY
                x = x + (realX - width) / 2;
                y = y + (realY - heigh) / 2;
                cartinca.setX(x); //-width + realX
                cartinca.setY(y);

                ugolLV.setX(cartinca.getX() - 30);
                ugolLV.setY(cartinca.getY() - 15);

                ugolPV.setX(cartinca.getX() + cartinca.getWidth() - 60);
                ugolPV.setY(cartinca.getY() - 15);

                ugolPN.setX(cartinca.getX() + cartinca.getWidth() - 60);
                ugolPN.setY(cartinca.getY() + cartinca.getHeight() - 60);

                ugolLN.setX(cartinca.getX() - 30);
                ugolLN.setY(cartinca.getY() + cartinca.getHeight() - 60);

                //RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                //layoutParams.addRule(RelativeLayout.FOCUS_LEFT);
                //layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                //cartinca.setLayoutParams(layoutParams);
                return true;
            }
            return false;
        }
    };
}