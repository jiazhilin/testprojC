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
package com.droidlogic.screencontrol;

import android.app.Activity;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import android.view.View;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.widget.Toast;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Spinner;
import com.droidlogic.app.ScreenControlManager;
/**
 * @ClassName ScreenControlActivity
 * @Description TODO
 * @Date
 * @Author
 * @Version V1.1
 */
public class ScreenControlActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ScreenRecordDemo";
    private static final int STORAGE_REQUEST_CODE = 101;
    private static final int AUDIO_REQUEST_CODE   = 102;

    private final int RECORD_STATUS_NONE  = -1;
    private final int RECORD_STATUS_START = 0;
    private final int RECORD_STATUS_STOP  = 2;

    private final int RECORD_TYPE_TS  = 0;
    private final int RECORD_TYPE_YUV = 1;
    private final int RECORD_TYPE_AVC  = 2;

    private TextView widthTv;
    private TextView heightTv;
    private TextView frameRateTv;
    private TextView bitRateTv;
    private TextView bitRate_MTv;
    private TextView timeTv;
    private TextView time_sTv;
    private TextView typeTv;

    private EditText widthEt;
    private EditText heightEt;
    private EditText frameRateEt;
    private EditText bitRateEt;
    private EditText timeEt;
    private EditText typeEt;
    private CheckBox cbIsCrop;
    private Spinner spnRecordtype;

    private EditText leftEt;
    private EditText topEt;
    private EditText rightEt;
    private EditText bottomEt;

    private Button startBtn;
    private Button stopBtn;
    private File file;
    private ScreenControlManager mScreenControl;
    private YuvCallbackListener mYuvCallbackListener = null;
    private AvcCallbackListener mAvcCallbackListener = null;

    private int width;
    private int height;
    private int limitTimes; //15s
    private int bitRate; //4M
    private int frameRate;
    private int sourceType;
    private boolean mIsCrop = false;
    private int recordType = RECORD_TYPE_TS; // 0:ts 1:yuv 2:h264
    private int recordStatus = RECORD_STATUS_NONE;
    FileOutputStream mfos = null;
    private File mfile = null;

    private int top;
    private int left;
    private int right;
    private int bottom;


    public class YuvCallbackListener implements ScreenControlManager.YuvCallbackListener {
		@Override
		public void onYuvAvailable(byte[] data){
			if (null != data) {
	            try {
					long totalMilliSeconds = System.currentTimeMillis();
					long nowtime=totalMilliSeconds*1000;
	                Log.d(TAG, "onYuvAvailable "+" nowtime="+nowtime);
	            } catch (Exception e) {
	                Log.e(TAG, "Write record data error: "+e);
	            }
	        }
		}
		
		@Override
		public void onYuvReceiveOver(){
			Log.d(TAG, "onYuvReceiveOver .....");
			try {
            } catch (Exception e) {
                Log.e(TAG, "onYuvReceiveOver Close file error: "+e);
            }
			recordStatus = RECORD_STATUS_NONE;
		}
	}

    public class AvcCallbackListener implements ScreenControlManager.AvcCallbackListener {
		@Override
		public void onAvcAvailable(byte[] data,int frameType ,long pts){
			if (null != data) {
	            try {
					long totalMilliSeconds = System.currentTimeMillis();
					long nowtime=totalMilliSeconds*1000;
	                Log.d(TAG, "onAvcAvailable "+",frameType="+frameType+ ",pts=" + pts);
	                mfos.write(data);
	            } catch (Exception e) {
	                Log.e(TAG, "Write record data error: "+e);
	            }
	        }
		}
		
		@Override
		public void onAvcReceiveOver(){
			Log.d(TAG, "onAvcReceiveOver .....");
			try {
                mfos.flush();
                mfos.close();
            } catch (Exception e) {
                Log.e(TAG, "Close file error: "+e);
            }
			recordStatus = RECORD_STATUS_NONE;
			
		}
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mScreenControl = new ScreenControlManager(this);

        initView();
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }
    }

    @Override
    public void onClick(View view) {
        int prevStatus = handleRecordStatus(view);
        if (RECORD_STATUS_NONE == prevStatus && RECORD_STATUS_START == recordStatus) {
            Log.d(TAG, "record control begin record");
            new Thread() {
                @Override
                public void run() {
                    super.run();
    
                    if (mScreenControl != null) {
                        Log.d(TAG, "begin startScreenRecord");
                        readRecordInfo();
                        if (recordType == RECORD_TYPE_TS) {
                            file = new File(Environment.getExternalStorageDirectory(), "record-" + System.currentTimeMillis() + ".ts");
                            moveTaskToBack(true);
                            if(!mIsCrop) {
                                mScreenControl.startScreenRecord(width, height, frameRate, bitRate*1000000, limitTimes, sourceType, file.getAbsolutePath());
        
                            }else{
                                mScreenControl.startScreenRecord(left, top, right, bottom, width, height, frameRate, bitRate*1000000, limitTimes, sourceType, file.getAbsolutePath());
        
                            }
                        }else if (recordType == RECORD_TYPE_YUV) {
                            // file = new File(Environment.getExternalStorageDirectory(), "record-" + System.currentTimeMillis() + ".yuv");
                            mYuvCallbackListener = new YuvCallbackListener();
                            mScreenControl.setYuvCallbackListener(mYuvCallbackListener);
                            mScreenControl.startYuvScreenRecord(width, height, frameRate, sourceType);
                        }else if (recordType == RECORD_TYPE_AVC) {

                            mfile = new File(Environment.getExternalStorageDirectory(), "record-" + System.currentTimeMillis() + ".yuv");
                            if (mfile == null )
                            return ;
                            try {
                                mfos = new FileOutputStream(mfile, true);
                            }catch (Exception e) {
                                Log.e(TAG, "File ["+mfile.getName()+"] cannot open: "+e+", exit...");
                                return;
                            }
                            mAvcCallbackListener = new AvcCallbackListener();
                            mScreenControl.setAvcCallbackListener(mAvcCallbackListener);
                            mScreenControl.startAvcScreenRecord(width, height, frameRate, bitRate*1000000, sourceType);
                        }
                    }
                }
            }.start();

        }else if (RECORD_STATUS_STOP == recordStatus) {
            // needs to stop record
            Log.d(TAG, "record control stop");
            Toast.makeText(this, "Record stop...",Toast.LENGTH_SHORT).show();
            mScreenControl.stopRecord();
            recordStatus = RECORD_STATUS_NONE;  //after stop, set status to none for next operation
            startBtn.setText(getString(R.string.start)); // reset start&pause button text
        }
        
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
    public void checkCropArea() {


    }
    /**
     * view: clicked btn
     * return: previous record status, new status save in variable 'recordStatus'
     */
    private int handleRecordStatus(View curView) {
        int prevRecordStatus = recordStatus;
        switch (curView.getId()) {
            case R.id.start:
                switch (recordStatus) {
                    case RECORD_STATUS_NONE:  recordStatus = RECORD_STATUS_START; Log.d(TAG, "record status: NONE -> START"); break;
                    default: break;
                }
                break;
            case R.id.stop:
                if (RECORD_STATUS_START == recordStatus) {
                    recordStatus = RECORD_STATUS_STOP;
                    if ( mYuvCallbackListener != null)
                        mYuvCallbackListener = null;
                    if ( mAvcCallbackListener != null) 
                        mAvcCallbackListener = null;
                    Log.d(TAG, "record status: START -> STOP");
                } else {
                    Log.e(TAG, "Pls begin record first!");
                }
                break;
            default: break;
        }
        return prevRecordStatus;
    }
    public void readRecordInfo() {
        width = Integer.parseInt(widthEt.getText().toString().trim());
        height = Integer.parseInt(heightEt.getText().toString().trim());
        frameRate = Integer.parseInt(frameRateEt.getText().toString().trim());
        bitRate = Integer.parseInt(bitRateEt.getText().toString().trim());
        limitTimes = Integer.parseInt(timeEt.getText().toString().trim());
        sourceType = Integer.parseInt(typeEt.getText().toString().trim());
        mIsCrop = cbIsCrop.isChecked();
        if (mIsCrop) {
            left = Integer.parseInt(leftEt.getText().toString().trim());
            right = Integer.parseInt(rightEt.getText().toString().trim());
            top = Integer.parseInt(topEt.getText().toString().trim());
            bottom = Integer.parseInt(bottomEt.getText().toString().trim());

        }
        recordType = spnRecordtype.getSelectedItemPosition();
    }

    public void initView() {
        widthTv = (TextView) findViewById(R.id.width);
        heightTv = (TextView) findViewById(R.id.height);
        frameRateTv = (TextView) findViewById(R.id.framerate);
        bitRateTv = (TextView) findViewById(R.id.bitrate);
        bitRate_MTv = (TextView) findViewById(R.id.bitrate_m);
        timeTv = (TextView) findViewById(R.id.limittime);
        time_sTv = (TextView) findViewById(R.id.limittime_s);
        typeTv = (TextView) findViewById(R.id.sourcetype);

        widthEt = (EditText) findViewById(R.id.edit_width);
        heightEt = (EditText) findViewById(R.id.edit_height);
        frameRateEt = (EditText) findViewById(R.id.edit_framerate);
        bitRateEt = (EditText) findViewById(R.id.edit_bitrate);
        timeEt = (EditText) findViewById(R.id.edit_limittime);
        typeEt = (EditText) findViewById(R.id.edit_sourcetype);
        cbIsCrop = (CheckBox) findViewById(R.id.cb_isCrop);
        spnRecordtype = (Spinner) findViewById(R.id.spn_recordtype);
        spnRecordtype.setSelection(recordType);
        leftEt = (EditText) findViewById(R.id.edit_left);
        topEt = (EditText) findViewById(R.id.edit_top);
        rightEt = (EditText) findViewById(R.id.edit_right);
        bottomEt = (EditText) findViewById(R.id.edit_bottom);

        startBtn = (Button) findViewById(R.id.start);
        stopBtn = (Button) findViewById(R.id.stop);
    }
}
