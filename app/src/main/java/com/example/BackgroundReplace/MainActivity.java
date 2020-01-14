package com.example.BackgroundReplace;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.huawei.hiai.vision.common.ConnectionCallback;
import com.huawei.hiai.vision.common.VisionBase;
import com.huawei.hiai.vision.image.segmentation.ImageSegmentation;
import com.huawei.hiai.vision.visionkit.common.Frame;
import com.huawei.hiai.vision.visionkit.image.ImageResult;
import com.huawei.hiai.vision.visionkit.image.segmentation.SegmentationConfiguration;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyTag";
    private ImageView mPic;
    private Bitmap mBitmap, newbmp, Background, output;
    private String filePath;
    private static final int MSG_SEGMENTATION_FINISH = 1;
    private static final int MSG_REPLACEMENT_FINISH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        checkPermission();
        mPic = findViewById(R.id.iv_pic);
    }

    /**
     * 动态加载openCV4Android
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * 拍照
     */
    public void takePicture(View view) {
        //Toast.makeText(getApplicationContext(),"拍照..." , Toast.LENGTH_SHORT).show();
        PictureSelector.create(MainActivity.this)
                .openCamera(PictureMimeType.ofImage())
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    /**
     * 打开图库
     *
     * @param view
     */
    public void openGallery(View view) {
        //Toast.makeText(getApplicationContext(),"打开相册..." , Toast.LENGTH_SHORT).show();
        PictureSelector.create(MainActivity.this)
                .openGallery(PictureMimeType.ofImage())
                .isCamera(false)
                .selectionMode(PictureConfig.SINGLE)
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    /**
     * 风格转换
     */
    public void styleTransfer(View view) {

    }

    /**
     * Handler线程间通信
     */
    private Handler mHandler = new Handler(){
        @Override
        public  void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_SEGMENTATION_FINISH:
                    /**Print Portrait segmented; */
                    mPic.setImageBitmap(newbmp);
                    Toast.makeText(getApplicationContext(), "人像分割完成", Toast.LENGTH_SHORT).show();
                    output = BackgroundReplace(mBitmap, newbmp, Background);
                    break;
                case MSG_REPLACEMENT_FINISH:
                    /**Print Background Replaced; */
                    mPic.setImageBitmap(output);
                    Toast.makeText(getApplicationContext(), "背景替换完成", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * 人脸切割
     */
    public void portraitSeg(View view) {
        if (isSupportHiAI()) {
            Toast.makeText(getApplicationContext(), "处理中，请稍候...", Toast.LENGTH_SHORT).show();
            Thread segment = new Thread(new Runnable() {
                @Override
                public void run() {
                    /** Initialize with the VisionBase static class and asynchronously get the connection of the service */
                    VisionBase.init(MainActivity.this, new ConnectionCallback() {
                        @Override
                        public void onServiceConnect() {
                            /** This callback method is invoked when the service connection is successful; you can do the initialization of the detector class, mark the service connection status, and so on */
                        }

                        @Override
                        public void onServiceDisconnect() {
                            /** When the service is disconnected, this callback method is called; you can choose to reconnect the service here, or to handle the exception*/
                        }
                    });
                    /** Define class detector, the context of this project is the input parameter*/
                    ImageSegmentation ssEngine = new ImageSegmentation(MainActivity.this);
                    /** Define the frame, put the bitmap that needs to detect the image into the frame */
                    Frame frame = new Frame();
                    /** BitmapFactory.decodeFile input resource file path*/
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    frame.setBitmap(bitmap);
                    /** Input parameters*/
                    SegmentationConfiguration sc = new SegmentationConfiguration();
                    sc.setSegmentationType(SegmentationConfiguration.TYPE_PORTRAIT);
                    ssEngine.setSegmentationConfiguration(sc);
                    /** Portrait segmentation*/
                    ImageResult srt = ssEngine.doSegmentation(frame, null);
                    /** Convert the result to bitmap format*/
                    newbmp = srt.getBitmap();
                    /** Source release */
                    VisionBase.destroy();
                    mHandler.sendEmptyMessage(MSG_SEGMENTATION_FINISH);
                }
            });
            segment.start();
        } else {
            Toast.makeText(getApplicationContext(), "该手机暂不支持华为HiAI引擎...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 分割边缘平滑处理???未做
     */

    /**
     * 背景替换
     */
    public Bitmap BackgroundReplace(final Bitmap bmp1, final Bitmap bmp2, Bitmap Background){
        int w = bmp1.getWidth();
        int h = bmp1.getHeight();
        final Bitmap bmpPortrait = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Thread MaskReplace = new Thread(new Runnable() {
            @Override
            public void run() {
                Mat img1 = new Mat();
                Mat imgMask = new Mat();
                Utils.bitmapToMat(bmp1, img1);
                Utils.bitmapToMat(bmp2, imgMask);
                Mat imgPortrait = new Mat(img1.size(), CvType.CV_8U, new Scalar((Imgproc.GC_PR_FGD)));
                img1.copyTo(imgPortrait, imgMask);
                Utils.matToBitmap(imgPortrait, bmpPortrait);
                mHandler.sendEmptyMessage(MSG_REPLACEMENT_FINISH);
            }
        });

//        Thread pixelReplace = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                for (int i = 0; i < w; i++) {
//                    for (int j = 0; j < h; j++) {
//                        switch (bmp2.getPixel(i,j)){
//                            case Color.BLACK:{
//                                bmpPortrait.setPixel(i,j, Color.TRANSPARENT);
//                                break;
//                            }
//                            case  Color.WHITE: {
//                                bmpPortrait.setPixel(i, j, bmp1.getPixel(i, j));
//                                break;
//                            }
//                        }
//                    }
//                }
//                mHandler.sendEmptyMessage(MSG_REPLACEMENT_FINISH);
//            }
//        });
//        pixelReplace.start();
        MaskReplace.start();
        return bmpPortrait;
    }


    /**
     * 结果回调，预览图片
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    List<LocalMedia> list = PictureSelector.obtainMultipleResult(data);
                    if (list != null && list.size() > 0) {
                        //Toast.makeText(getApplicationContext(),"正在处理中..." , Toast.LENGTH_SHORT).show();
                        filePath = list.get(0).getPath();
                        Glide.with(this).load(filePath).into(mPic);
                        mBitmap = BitmapFactory.decodeFile(filePath);
                        //mPic.setImageBitmap(mBitmap);
                    }
                    break;
            }
        }
    }

    /**
     * 图片存储
     */
    private String saveImage(Bitmap bmp) {
        //设置保存图片的文件夹
        File appDir = new File(Environment.getExternalStorageDirectory(), "processed_img");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        //用系统时间来命名图片
        String fileName = System.currentTimeMillis() + ".png";
        File outputImage = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(outputImage);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputImage.getAbsolutePath();
    }

    /**
     * 权限申请
     */
    private void checkPermission() {

        if (Build.VERSION.SDK_INT >= 23) {
            int write = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int camera = checkSelfPermission(Manifest.permission.CAMERA);
            //int internet = checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE);


            if (write != PackageManager.PERMISSION_GRANTED ||
                    read != PackageManager.PERMISSION_GRANTED ||
                    camera != PackageManager.PERMISSION_GRANTED
                ///       internet != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                        //            Manifest.permission.ACCESS_WIFI_STATE
                }, 300);
            } else {
                String name = "CrashDirectory";
                File file1 = new File(Environment.getExternalStorageDirectory(), name);
                if (file1.mkdirs()) {
                    Log.i("wytings", "permission -------------> " + file1.getAbsolutePath());
                } else {
                    Log.i("wytings", "permission -------------fail to make file ");
                }
            }
        } else {
            Log.i("wytings", "------------- Build.VERSION.SDK_INT < 23 ------------");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 300) {
            Log.i("wytings", "--------------requestCode == 300->" + requestCode + "," + permissions.length + "," + grantResults.length);
        } else {
            Log.i("wytings", "--------------requestCode != 300->" + requestCode + "," + permissions + "," + grantResults);
        }
    }

    /**
     * 是否支持HiAI
     */
    private boolean isSupportHiAI() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo("com.huawei.hiai", 0);
            return packageInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }



}
