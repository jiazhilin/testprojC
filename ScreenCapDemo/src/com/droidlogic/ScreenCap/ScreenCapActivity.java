/******************************************************************
*
*Copyright (C) 2012  Amlogic, Inc.
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
******************************************************************/
package com.droidlogic.screencap;

import android.app.Activity;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.os.Environment;
import java.io.File;
import android.view.View;
import android.os.Bundle;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.view.SurfaceControl;
import android.support.annotation.NonNull;
import android.os.Build;
import android.os.IBinder;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.Spinner;

import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.ActivityManager;
import java.util.List;

import android.os.Handler;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjectionManager;
import android.view.Display;
import android.util.DisplayMetrics;
import android.hardware.display.DisplayManager;
import android.media.Image;
import java.text.SimpleDateFormat;
import java.util.Date;
import static android.app.Activity.RESULT_OK;



import java.lang.reflect.Method;

import com.droidlogic.app.ScreenControlManager;

/**
 * @ClassName ScreenControlActivity
 * @Description TODO
 * @Date
 * @Author
 * @Version V1.2
 */
public class ScreenCapActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ScreenCapDemo";
    private static final int STORAGE_REQUEST_CODE = 101;
    private static final int AUDIO_REQUEST_CODE   = 102;
    private static final int RESULT_CODE   = 1;
    private final int permissionCode = 0x1234;
    private static MediaProjection sMediaProjection;
    private MediaProjectionManager mProjectionManager;

    private TextView widthTv;
    private TextView heightTv;
    private TextView leftTv;
    private TextView topTv;
    private TextView rightTv;
    private TextView bottomTv;
    private TextView typeTv;

    private EditText widthEt;
    private EditText heightEt;
    private EditText leftEt;
    private EditText topEt;
    private EditText rightEt;
    private EditText bottomEt;
    //private EditText typeEt;
    private Spinner spn_srcType;

    private Button captureBtn;
    private ImageView imageView;
    private File file;
    private ScreenControlManager mScreenControl;
    private CheckBox cbHideWindow;
    private CheckBox cbCapWithBuf;

    private int width;
    private int height;
    private int top;
    private int left;
    private int right;
    private int bottom;
    private int sourceType = 1;  // 0:video  1:osd+video  2:osd
    private boolean mHideWindow = true;
    private boolean mCapWithBuf = true;
    private ImageReader mImageReader;
    private int mDensity;
    private Handler mHandler;
    private VirtualDisplay mVirtualDisplay;
    private Display mDisplay;
    OnImageCaptureScreenListener listener;
    private String STORE_DIR;
    private String mFileName;
    private boolean mScreenCaped = false;

    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mScreenControl = new ScreenControlManager(this);

        initView();
        captureBtn.setOnClickListener(this);

        // request permission
        if (Build.VERSION.SDK_INT >= 23) {
            if (!hasPermission()) {
                Log.i (TAG, "[onCreate] requestPermissions");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.REORDER_TASKS},
                        permissionCode);
            }
        }
        STORE_DIR = Environment.getExternalStorageDirectory() + "";
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }
    public interface OnImageCaptureScreenListener {
        public void imageCaptured(byte[] image);
    }

    private boolean hasPermission() {
        return !(PackageManager.PERMISSION_DENIED == checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                || (PackageManager.PERMISSION_DENIED == checkSelfPermission(Manifest.permission.REORDER_TASKS)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission,
                       @NonNull int[] grantResults) {
        if (requestCode == permissionCode &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "request permission ok");
        } else {
            Log.e(TAG, "request permission failed");
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        if (null != mScreenControl) {
            mScreenControl.release();
            mScreenControl = null;
        }
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void bring2Front() {
        ActivityManager activtyManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos) {
            if (this.getPackageName().equals(runningTaskInfo.topActivity.getPackageName())) {
                activtyManager.moveTaskToFront(runningTaskInfo.id, 0);
                Log.i(TAG, "bring to front:"+getPackageName());
                return;
            }
        }
    }



    @Override
    public void onClick(View view) {
        if (mScreenControl != null) {
            readCaptureInfo();
            if (mHideWindow) {
                Log.d(TAG, " moveTaskToBack...1111");
                moveTaskToBack(true);
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    file = new File(Environment.getExternalStorageDirectory(), "capture-" + System.currentTimeMillis() + ".jpeg");
                    if (sourceType == 2) { // osd
                        try {
                            Log.d(TAG, " createScreenCaptureIntent...");
                            mProjectionManager = (MediaProjectionManager) getSystemService(
                                    Context.MEDIA_PROJECTION_SERVICE);
                            startActivityForResult(mProjectionManager.createScreenCaptureIntent(),
                                    1000);
                            while(mScreenCaped) {
                                File file1 = new File(mFileName);
                                Bitmap bitmap1 = BitmapFactory.decodeFile(mFileName);
                                Log.d(TAG, "Transform bitmap success 1111");
                                imageView.setImageBitmap(bitmap1);
                                Toast.makeText(getBaseContext(), "Success, save to "+ mFileName, Toast.LENGTH_SHORT).show();

                            }
                            // Class c1 = Class.forName("android.view.SurfaceControl");
                            // Method getInternalDisplayToken = c1.getMethod("getInternalDisplayToken");
                            // IBinder displayToken = (IBinder)getInternalDisplayToken.invoke(null);
                            // final SurfaceControl.DisplayCaptureArgs captureArgs =
                            //         new SurfaceControl.DisplayCaptureArgs.Builder(displayToken)
                            //                 .setSourceCrop(crop)
                            //                 .setSize(width, height)
                            //                 .build();
                            // final SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer =
                            //         SurfaceControl.captureDisplay(captureArgs);
                            // Bitmap mBitmap  = screenshotBuffer.asBitmap();
                            // // Class clz = Class.forName("android.view.SurfaceControl");
                            // // Method screenshot = clz.getMethod("screenshot", Rect.class, int.class, int.class, int.class);
                            // // Bitmap mBitmap = (Bitmap)screenshot.invoke(null, new Rect(left, top, right, bottom), width, height, 0);
                            // // //mBitmap = SurfaceControl.screenshot(new Rect(left, top, right, bottom), width, height, 0);
                            // if (mBitmap != null) {
                            //     // Convert to a software bitmap so it can be set in an ImageView.
                            //     Bitmap swBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            //     saveBitmapAsJPEG(swBitmap, file);
                            //     imageView.setImageBitmap(swBitmap);
                            //     Toast.makeText(getBaseContext(), "Success, save to "+file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            // }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else { // 0:video  1:osd+video  2:osd
                        if (!mCapWithBuf) {
                            Log.d(TAG, "begin startScreenCap");
                            if (0 == mScreenControl.startScreenCap(left, top, right, bottom,
                                  width, height, sourceType, file.getAbsolutePath())) {
                                Log.d(TAG, "startScreenCap finish");
                                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                                Log.d(TAG, "Transform bitmap success");
                                imageView.setImageBitmap(bitmap);
                                Toast.makeText(getBaseContext(), "Success, save to "+file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getBaseContext(), "Fail to capture, please check!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "begin startScreenCapBuffer");
                            byte[] byteArr = mScreenControl.startScreenCapBuffer(left, top, right, bottom, width, height, sourceType);
                            if (null == byteArr) {
                                Log.e(TAG, "get byte buffer error");
                                Toast.makeText(getBaseContext(), "Fail to capture buffer, please check!", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "get byte buffer ok");
                                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteArr));
                                if (null != bitmap) {
                                    Log.d(TAG, "Transform bitmap success");
                                    imageView.setImageBitmap(bitmap);
                                    Toast.makeText(getBaseContext(), "Success capture buffer picture", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e(TAG, "bitmap is empty(null)");
                                    Toast.makeText(getBaseContext(), "Success capture buffer picture, but fail to show", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }

                    // need show hided activity
                    if (mHideWindow) {
                        Log.d(TAG, " bring2Front...2222");
                        bring2Front();
                    }
                }
            }, 1000);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "on result code:" + resultCode + "  requestCode:" + requestCode);
        if (RESULT_OK == resultCode && 1000 == requestCode) {
            if (mHideWindow) {
                Log.d(TAG, " moveTaskToBack...2222");
                moveTaskToBack(true);
                try {
                    Thread.sleep(800); 
                } catch (InterruptedException e) {
        
                }
            }
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            Log.d(TAG, "Start getMediaProjection...");
            if (sMediaProjection != null) {
                Log.d(TAG, "Start capturing...");
                mImageReader = ImageReader.newInstance(width,height, PixelFormat.RGBA_8888, 2);
                WindowManager window = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);
                mDisplay = window.getDefaultDisplay();
                final DisplayMetrics metrics = new DisplayMetrics();
                // use getMetrics is 2030, use getRealMetrics is 2160, the diff is NavigationBar's height
                mDisplay.getRealMetrics(metrics);
                mDensity = metrics.densityDpi;
                 //start capture reader
                mVirtualDisplay = sMediaProjection.createVirtualDisplay(
                        "ScreenShot",
                        width,
                        height,
                        mDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        mImageReader.getSurface(),
                        null,
                        mHandler);
            }
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
    
    
                    Image image = null;
                    FileOutputStream fos = null;
                    Bitmap bitmap = null;
                    Log.d(TAG, "onImageAvailable... 11111");
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
//                            bitmap = ImageUtils.image_ARGB8888_2_bitmap(metrics, image);
                            Log.d(TAG, "image_2_bitmap... 11111");
                            bitmap = image_2_bitmap(image, Bitmap.Config.ARGB_8888);
                            Date currentDate = new Date();
                            SimpleDateFormat date = new SimpleDateFormat("yyyyMMddhhmmss");
                            String fileName = STORE_DIR + "/myScreen_" + date.format(
                                    currentDate) + ".png";
                            fos = new FileOutputStream(fileName);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            Log.d(TAG, "End now!!!!!!  Screenshot saved in " + fileName);
                            mFileName = fileName;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Bitmap bitmap1 = BitmapFactory.decodeFile(fileName);
                                    Log.d(TAG, "Transform bitmap success");
                                    imageView.setImageBitmap(bitmap1);
                                }
                            });
                            Toast.makeText(getBaseContext(), "Screenshot saved in " + fileName,
                                    Toast.LENGTH_LONG);
                            sMediaProjection.stop();
                            
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (null != fos) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (null != bitmap) {
                            bitmap.recycle();
                        }
                        if (null != image) {
                            image.close(); // close it when used and
                        }
                        if (mHideWindow) {
                            Log.d(TAG, " bring2Front...11111");
                            bring2Front();
                        }
                        if (mImageReader != null) {
                            mImageReader.setOnImageAvailableListener(null, null);
                        }
                    }
                }
            }, mHandler);


        }
        
    }

    private void saveBitmapAsJPEG(Bitmap bmp, File f) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Bitmap image_2_bitmap(Image image, Bitmap.Config config) {

        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap bitmap;

        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Log.d(TAG,
                "pixelStride:" + pixelStride + ". rowStride:" + rowStride + ". rowPadding" + rowPadding);

        bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride/*equals: rowStride/pixelStride */
                , height, config);
        bitmap.copyPixelsFromBuffer(buffer);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height);
//        return bitmap;
    }

    public void readCaptureInfo() {
        width = Integer.parseInt(widthEt.getText().toString().trim());
        height = Integer.parseInt(heightEt.getText().toString().trim());
        left = Integer.parseInt(leftEt.getText().toString().trim());
        right = Integer.parseInt(rightEt.getText().toString().trim());
        top = Integer.parseInt(topEt.getText().toString().trim());
        bottom = Integer.parseInt(bottomEt.getText().toString().trim());
        //sourceType = Integer.parseInt(typeEt.getText().toString().trim());
        sourceType = spn_srcType.getSelectedItemPosition();
        mHideWindow = cbHideWindow.isChecked();
        mCapWithBuf = cbCapWithBuf.isChecked();
    }

    public void initView() {
        widthTv = (TextView) findViewById(R.id.width);
        heightTv = (TextView) findViewById(R.id.height);
        topTv = (TextView) findViewById(R.id.top);
        bottomTv = (TextView) findViewById(R.id.bottom);
        rightTv = (TextView) findViewById(R.id.right);
        leftTv = (TextView) findViewById(R.id.left);
        typeTv = (TextView) findViewById(R.id.type);

        widthEt = (EditText) findViewById(R.id.edit_width);
        heightEt = (EditText) findViewById(R.id.edit_height);
        leftEt = (EditText) findViewById(R.id.edit_left);
        topEt = (EditText) findViewById(R.id.edit_top);
        rightEt = (EditText) findViewById(R.id.edit_right);
        bottomEt = (EditText) findViewById(R.id.edit_bottom);
        //typeEt = (EditText) findViewById(R.id.edit_type);
        spn_srcType = (Spinner) findViewById(R.id.spn_srctype);
        
        // set default source type value
        spn_srcType.setSelection(sourceType);

        captureBtn = (Button) findViewById(R.id.capture);
        imageView = (ImageView) findViewById(R.id.imageview);

        cbHideWindow = (CheckBox) findViewById(R.id.cb_hideWindow);
        cbCapWithBuf = (CheckBox) findViewById(R.id.cb_ifBuffer);
    }
}
