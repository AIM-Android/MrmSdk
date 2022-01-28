package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import mrm.client.IVCPServiceClient;
import mrm.define.IVCP.IVCP_GSENSOR_VALUE;
import mrm.define.MRM_ENUM;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;
import mrm.demo.util.ViewOperator;


public class GsensorDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - Gsen";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytGsensorAvailability;
    TextView mTxtGsensorAvailability;
    String mGsensorAvailability = "";

    LinearLayout mLytGsensorStatus;
    TextView mTxtGsensorStatus;
    String mGsensorStatus = "";
    Button mBtnGsensorEnable, mBtnGsensorDisable;

    LinearLayout mLytGsensorResolutionOperationResult;
    TextView mTxtGsensorResolutionOperationResult;
    Spinner mSpnGsensorResolution;
    BaseAdapter mGsensorResolutionSpinnerDataAdapter;
    ArrayList<SpinnerItem> mGsensorResolutionSpinnerItemList;
    Button mBtnGetGsensorResolution, mBtnSetGsensorResolution;
    String mtGsensorResolutionOperationResult;
    int mSelectedGsensorResolutionID;

    LinearLayout mLytGsensorWakeupStatus;
    TextView mTxtGsensorWakeupStatus;
    String mGsensorWakeupStatus = "";
    Button mBtnGsensorWakeupEnable, mBtnGsensorWakeupDisable;

    LinearLayout mLytGsensorWakeupThreshold;
    EditText mEtxtGsensorWakeupThreshold;
    Button mBtnGetGsensorWakeupThreshold, mBtnSetGsensorWakeupThreshold;
    String mGsensorWakeupThreshold;

    LinearLayout mLytGsensorData;
    TextView mTxtGsensorData;
    String mGsensorData;

    LinearLayout mLytGsensorOffsetOperationResult;
    TextView mTxtGsensorOffsetValueX;
    TextView mTxtGsensorOffsetValueY;
    TextView mTxtGsensorOffsetValueZ;
    TextView mTxtGsensorOffsetOperationResult;
    String mGsensorOffsetValueX;
    String mGsensorOffsetValueY;
    String mGsensorOffsetValueZ;
    String mGsensorOffsetOperationResult;
    Button mBtnGetGsensorOffset, mBtnSetGsensorOffset, mBtnResetGsensorOffset, mBtnCalibrationGsensorOffset;

    LinearLayout mLytGsensorCalibrationOrientation;
    Spinner mSpnGsensorCalibrationOrientation;
    BaseAdapter mGsensorCalibrationOrientationSpinnerDataAdapter;
    ArrayList<SpinnerItem> mGsensorCalibrationOrientationSpinnerItemList;
    int mSelectedGsensorCalibrationOrientationID;



    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_gsensorDataPolling = false;
    Thread mThreadGsensorDataPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<GsensorDemoActivity> mActivity = null;

        public MyHandler(GsensorDemoActivity activity) {
            mActivity = new WeakReference<GsensorDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GsensorDemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            activity.updateView(msg.what);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("\n\n=============== onCreate() ===============\n\n");
        super.onCreate(savedInstanceState);

        mIvcpAPI = EntryActivity.mIvcpAPI;

        mHandler = new MyHandler(this);
        initView();
        initListener();

        doAsyncTaskRefreshAllFields();
    }

    @Override
    protected void onResume() {
        log("\n\n=============== onResume() ===============\n\n");
        super.onResume();
        toggleGsensorDataPolling(true);

    }

    @Override
    protected void onPause() {
        log("\n\n=============== onPause() ===============\n\n");
        super.onPause();
        toggleGsensorDataPolling(false);

    }

    @Override
    protected void onDestroy() {
        log("\n\n=============== onDestroy() ===============\n\n");
        super.onDestroy();
    }

    private void setGsensorResolutionSpinnerItemList() {
        mGsensorResolutionSpinnerItemList = new ArrayList<SpinnerItem>();
        mGsensorResolutionSpinnerItemList.clear();

        mGsensorResolutionSpinnerItemList.add(new SpinnerItem("2G",  MRM_ENUM.IVCP_GSENSOR_RES.IVCP_GSENSOR_RES_2G.getValue()));
        mGsensorResolutionSpinnerItemList.add(new SpinnerItem("4G", MRM_ENUM.IVCP_GSENSOR_RES.IVCP_GSENSOR_RES_4G.getValue()));
        mGsensorResolutionSpinnerItemList.add(new SpinnerItem("8G",  MRM_ENUM.IVCP_GSENSOR_RES.IVCP_GSENSOR_RES_8G.getValue()));
        mGsensorResolutionSpinnerItemList.add(new SpinnerItem("16G", MRM_ENUM.IVCP_GSENSOR_RES.IVCP_GSENSOR_RES_16G.getValue()));
    }

    private void setGsensorCalibrationOrientationSpinnerItemList() {
        mGsensorCalibrationOrientationSpinnerItemList = new ArrayList<SpinnerItem>();
        mGsensorCalibrationOrientationSpinnerItemList.clear();

        mGsensorCalibrationOrientationSpinnerItemList.add(new SpinnerItem("Front",  0));
        mGsensorCalibrationOrientationSpinnerItemList.add(new SpinnerItem("Back",   1));
    }

    private void initView() {
        setContentView(R.layout.activity_gsensor_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytGsensorAvailability = (LinearLayout) findViewById(R.id.row_gsensor_available);
        mTxtGsensorAvailability = (TextView) findViewById(R.id.txt_gsensor_availability);

        mLytGsensorStatus = (LinearLayout) findViewById(R.id.row_gsensor_status);
        mTxtGsensorStatus = (TextView) findViewById(R.id.txt_gsensor_status);
        mBtnGsensorEnable = (Button)   findViewById(R.id.btn_gsensor_enable);
        mBtnGsensorDisable = (Button)   findViewById(R.id.btn_gsensor_disable);

        mLytGsensorResolutionOperationResult      = (LinearLayout) findViewById(R.id.row_gsensor_resolution);
        mTxtGsensorResolutionOperationResult      = (TextView) findViewById(R.id.txt_gsensor_resolution_operation_result);
        mSpnGsensorResolution                     = (Spinner)  findViewById(R.id.spn_gsensor_resolution);
        mBtnGetGsensorResolution                  = (Button)   findViewById(R.id.btn_get_gsensor_resolution);
        mBtnSetGsensorResolution                  = (Button)   findViewById(R.id.btn_set_gsensor_resolution);

        mLytGsensorWakeupStatus = (LinearLayout) findViewById(R.id.row_gsensor_wakeup_status);
        mTxtGsensorWakeupStatus = (TextView) findViewById(R.id.txt_gsensor_wakeup_status);
        mBtnGsensorWakeupEnable = (Button)   findViewById(R.id.btn_gsensor_wakeup_enable);
        mBtnGsensorWakeupDisable = (Button)   findViewById(R.id.btn_gsensor_wakeup_disable);

        mLytGsensorWakeupThreshold    = (LinearLayout) findViewById(R.id.row_gsensor_wakeup_threshold);
        mEtxtGsensorWakeupThreshold   = (EditText) findViewById(R.id.etxt_gsensor_wakeup_threshold);
        mBtnGetGsensorWakeupThreshold = (Button)   findViewById(R.id.btn_get_gsensor_wakeup_threshold);
        mBtnSetGsensorWakeupThreshold = (Button)   findViewById(R.id.btn_set_gsensor_wakeup_threshold);

        mLytGsensorData      = (LinearLayout) findViewById(R.id.row_gsensor_data);
        mTxtGsensorData = (TextView) findViewById(R.id.txt_gsensor_data);

        mLytGsensorOffsetOperationResult = (LinearLayout) findViewById(R.id.row_gsensor_offset);
        mTxtGsensorOffsetOperationResult = (TextView) findViewById(R.id.etxt_gsensor_offset_operation_result);
        mTxtGsensorOffsetValueX = (TextView) findViewById(R.id.etxt_gsensor_offset_value_x);
        mTxtGsensorOffsetValueY = (TextView) findViewById(R.id.etxt_gsensor_offset_value_y);
        mTxtGsensorOffsetValueZ = (TextView) findViewById(R.id.etxt_gsensor_offset_value_z);
        mBtnGetGsensorOffset    = (Button)   findViewById(R.id.btn_get_gsensor_offset);
        mBtnSetGsensorOffset    = (Button)   findViewById(R.id.btn_set_gsensor_offset);
        mBtnResetGsensorOffset  = (Button)   findViewById(R.id.btn_reset_gsensor_offset);

        mLytGsensorCalibrationOrientation = (LinearLayout) findViewById(R.id.row_gsensor_calibration);
        mBtnCalibrationGsensorOffset = (Button)   findViewById(R.id.btn_calibration_gsensor_offset);
        mSpnGsensorCalibrationOrientation = (Spinner)  findViewById(R.id.spn_gsensor_calibration_orientation);

        setGsensorResolutionSpinnerItemList();
        mGsensorResolutionSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mGsensorResolutionSpinnerItemList);
        mSpnGsensorResolution.setAdapter(mGsensorResolutionSpinnerDataAdapter);

        setGsensorCalibrationOrientationSpinnerItemList();
        mGsensorCalibrationOrientationSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mGsensorCalibrationOrientationSpinnerItemList);
        mSpnGsensorCalibrationOrientation.setAdapter(mGsensorCalibrationOrientationSpinnerDataAdapter);

        cleanAllFields();
    }


    private void initListener() {
        mBtnGsensorEnable.setOnClickListener(mBtnOnClickListener);
        mBtnGsensorDisable.setOnClickListener(mBtnOnClickListener);
        mSpnGsensorResolution.setOnItemSelectedListener(mSpnRearviewOnItemSelectedListener);
        mBtnGetGsensorResolution.setOnClickListener(mBtnOnClickListener);
        mBtnSetGsensorResolution.setOnClickListener(mBtnOnClickListener);

        mBtnGsensorWakeupEnable.setOnClickListener(mBtnOnClickListener);
        mBtnGsensorWakeupDisable.setOnClickListener(mBtnOnClickListener);
        mBtnGetGsensorWakeupThreshold.setOnClickListener(mBtnOnClickListener);
        mBtnSetGsensorWakeupThreshold.setOnClickListener(mBtnOnClickListener);

        mBtnGetGsensorOffset.setOnClickListener(mBtnOnClickListener);
        mBtnSetGsensorOffset.setOnClickListener(mBtnOnClickListener);
        mBtnResetGsensorOffset.setOnClickListener(mBtnOnClickListener);
        mBtnCalibrationGsensorOffset.setOnClickListener(mBtnOnClickListener);
        mSpnGsensorCalibrationOrientation.setOnItemSelectedListener(mSpnGsensorCalibrationOrientationOnItemSelectedListener);
    }


    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_gsensor_enable:
                    operationSetGsensorStatus(true);
                    break;
                case R.id.btn_gsensor_disable:
                    operationSetGsensorStatus(false);
                    break;

                case R.id.btn_get_gsensor_resolution:
                    operationGetGsensorResolution();
                    break;
                case R.id.btn_set_gsensor_resolution:
                    operationSetGsensorResolution(mSelectedGsensorResolutionID);
                    break;

                case R.id.btn_gsensor_wakeup_enable:
                    operationSetGsensorWakeupStatus(true);
                    break;
                case R.id.btn_gsensor_wakeup_disable:
                    operationSetGsensorWakeupStatus(false);
                    break;
                case R.id.btn_get_gsensor_wakeup_threshold:
                    operationGetGsensorWakeupThreshold();
                    break;
                case R.id.btn_set_gsensor_wakeup_threshold:
                    operationSetGsensorWakeupThreshold();
                    break;
                case R.id.btn_get_gsensor_offset:
                    operationGetGsensorOffset();
                    break;
                case R.id.btn_set_gsensor_offset:
                    operationSetGsensorOffset();
                    break;
                case R.id.btn_reset_gsensor_offset:
                    operationResetGsensorOffset();
                    break;
                case R.id.btn_calibration_gsensor_offset:
                    operationCalibrationtGsensorOffset();
                    break;

            }
        }
    };


    AdapterView.OnItemSelectedListener mSpnRearviewOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedGsensorResolutionID = (byte) mGsensorResolutionSpinnerItemList.get(position).value;
            log("Resolution Seleted item " + position + ", ID = " + mSelectedGsensorResolutionID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    AdapterView.OnItemSelectedListener mSpnGsensorCalibrationOrientationOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedGsensorCalibrationOrientationID = (byte) mGsensorCalibrationOrientationSpinnerItemList.get(position).value;
            log("Calibration Orientation Seleted item " + position + ", ID = " + mSelectedGsensorCalibrationOrientationID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    void updateView(int id) {
        switch( id ) {
            case R.id.row_gsensor_available:
                if(mGsensorAvailability.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorAvailability.setVisibility(View.GONE);
                    mLytGsensorStatus.setVisibility(View.GONE);
                    mLytGsensorResolutionOperationResult.setVisibility(View.GONE);
                    mLytGsensorWakeupStatus.setVisibility(View.GONE);
                    mLytGsensorWakeupThreshold.setVisibility(View.GONE);
                    mLytGsensorData.setVisibility(View.GONE);
                    mLytGsensorOffsetOperationResult.setVisibility(View.GONE);
                    mLytGsensorCalibrationOrientation.setVisibility(View.GONE);
                } else {
                    ViewOperator.setLabelText(mTxtGsensorAvailability, mGsensorAvailability);
                }
                break;

            case R.id.row_gsensor_status:
                if(mGsensorStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorStatus.setVisibility(View.GONE);
                } else {
                    ViewOperator.setLabelText(mTxtGsensorStatus, mGsensorStatus);
                }
                break;

            case R.id.row_gsensor_resolution:
                if (mtGsensorResolutionOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))
                        && mtGsensorResolutionOperationResult.contains("Get error")) {
                    mLytGsensorResolutionOperationResult.setVisibility(View.GONE);
                } else if(mtGsensorResolutionOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                    if (mTxtGsensorResolutionOperationResult != null) {
                        mTxtGsensorResolutionOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtGsensorResolutionOperationResult, mtGsensorResolutionOperationResult);
                    }
                } else {
                    boolean foundGsensorResolutionItem = false;
                    for(int i = 0 ; i < mGsensorResolutionSpinnerItemList.size() ; i++) {
                        if( mSelectedGsensorResolutionID == mGsensorResolutionSpinnerItemList.get(i).value ) {
                            mSpnGsensorResolution.setSelection(i);
                            foundGsensorResolutionItem = true;
                        }
                    }
                    if( foundGsensorResolutionItem ) {
                        mTxtGsensorResolutionOperationResult.setVisibility(View.GONE);
                    } else {
                        mtGsensorResolutionOperationResult = String.format("Get unknown Gsensor resolution id( %d )", mSelectedGsensorResolutionID);
                        mTxtGsensorResolutionOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtGsensorResolutionOperationResult, mtGsensorResolutionOperationResult);
                    }
                }
                break;

            case R.id.row_gsensor_wakeup_status:
                if(mGsensorWakeupStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorWakeupStatus.setVisibility(View.GONE);
                } else {
                    ViewOperator.setLabelText(mTxtGsensorWakeupStatus, mGsensorWakeupStatus);
                }
                break;

            case R.id.row_gsensor_wakeup_threshold:
                if(mGsensorWakeupThreshold.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorWakeupThreshold.setVisibility(View.GONE);
                } else {
                    log("update wakeup thres .... " + mGsensorWakeupThreshold);
                    mEtxtGsensorWakeupThreshold.setText(mGsensorWakeupThreshold);
                }
                break;

            case R.id.row_gsensor_data:
                if(mGsensorData.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorData.setVisibility(View.GONE);
                } else {
                    ViewOperator.setLabelText(mTxtGsensorData, mGsensorData);
                }
                break;

            case R.id.row_gsensor_offset:
                if(mGsensorOffsetOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorOffsetOperationResult.setVisibility(View.GONE);
                    mLytGsensorCalibrationOrientation.setVisibility(View.GONE);
                } else {
                    if (mTxtGsensorOffsetOperationResult != null)
                        ViewOperator.setLabelText(mTxtGsensorOffsetOperationResult, mGsensorOffsetOperationResult);
                    ViewOperator.setLabelText(mTxtGsensorOffsetValueX, mGsensorOffsetValueX);
                    ViewOperator.setLabelText(mTxtGsensorOffsetValueY, mGsensorOffsetValueY);
                    ViewOperator.setLabelText(mTxtGsensorOffsetValueZ, mGsensorOffsetValueZ);
                }
                break;


            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtGsensorData, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtGsensorOffsetOperationResult, "-");
    }


    void doAsyncTaskRefreshAllFields() {
        mTaskRefreshAllFields = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading Current Status ...");
                mProgressDailog.show();

                mSelectedGsensorResolutionID = (byte) ((SpinnerItem) mSpnGsensorResolution.getSelectedItem()).value;
            }

            @Override
            protected Void doInBackground(Void... params) {
                operationGetGsensorAvailability();
                operationGetGsensorStatus();
                operationGetGsensorResolution();
                operationGetGsensorWakeupStatus();
                operationGetGsensorWakeupThreshold();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        }.execute();
    }


    Runnable mThreadRunnableGsensorDataPolling = new Runnable() {
        @Override
        public void run() {
            while(isRunning_gsensorDataPolling) {
                operationGetGsensorData();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    void toggleGsensorDataPolling(boolean enable) {
        if(enable) {
            isRunning_gsensorDataPolling = enable;
            mThreadGsensorDataPolling = new Thread(mThreadRunnableGsensorDataPolling);
            mThreadGsensorDataPolling.start();
            log("--------- Start polling status --------- ");

        } else {
            try {
                isRunning_gsensorDataPolling = enable;
                if(mThreadGsensorDataPolling != null) {
                    if( mThreadGsensorDataPolling.isAlive() ) {
                        mThreadGsensorDataPolling.join();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log("--------- Stop polling status --------- ");
        }
    }



    private void operationGetGsensorAvailability() {
        mGsensorAvailability = getGsensorAvailability();
        mHandler.sendEmptyMessage(R.id.row_gsensor_available);
    }


    private void operationSetGsensorStatus(boolean status) {
        int ret;

        ret = setGsensorStatus(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mGsensorStatus = (status ? "ENABLE" : "DISABLE");
        } else {
            mGsensorStatus = "Set error. " + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.row_gsensor_status);
    }

    private void operationGetGsensorStatus() {
        mGsensorStatus = getGsensorStatus();
        mHandler.sendEmptyMessage(R.id.row_gsensor_status);
    }

    private void operationGetGsensorResolution() {
        int ret;
        int[] tempID = new int[1];

        ret = getGsensorResolution(tempID);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mtGsensorResolutionOperationResult = "Get OK";
            for(int i=0; i < mGsensorResolutionSpinnerItemList.size() ; i++) {
                if(tempID[0] == mGsensorResolutionSpinnerItemList.get(i).value) {
                    mSelectedGsensorResolutionID = tempID[0];
                    break;
                }
            }

        } else {
            mtGsensorResolutionOperationResult = "Get error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mHandler.sendEmptyMessage(R.id.row_gsensor_resolution);
    }

    private void operationSetGsensorResolution(int resolutionID) {
        int ret;
        ret = setGsensorResolution(resolutionID);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            for(int i=0; i < mGsensorResolutionSpinnerItemList.size() ; i++) {
                if(resolutionID == mGsensorResolutionSpinnerItemList.get(i).value) {
                    mSelectedGsensorResolutionID = resolutionID;
                    break;
                }
            }
            mtGsensorResolutionOperationResult = "Set OK.";

        } else {
            mtGsensorResolutionOperationResult = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }
        Toast.makeText(GsensorDemoActivity.this, mtGsensorResolutionOperationResult, Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessage(R.id.row_gsensor_resolution);
    }

    private void operationSetGsensorWakeupStatus(boolean status) {
        int ret;

        ret = setGsensorWakeupStatus(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mGsensorWakeupStatus = (status ? "ENABLE" : "DISABLE");
        } else {
            mGsensorWakeupStatus = "Set error. " + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.row_gsensor_wakeup_status);
    }

    private void operationGetGsensorWakeupStatus() {
        mGsensorWakeupStatus = getGsensorWakeupStatus();
        mHandler.sendEmptyMessage(R.id.row_gsensor_wakeup_status);
    }

    private void operationGetGsensorWakeupThreshold() {
        mGsensorWakeupThreshold = getGsensorWakeupThreshold();
        mHandler.sendEmptyMessage(R.id.row_gsensor_wakeup_threshold);
    }

    private void operationSetGsensorWakeupThreshold() {
        try {
            int ret = 0;
            int thr = Integer.valueOf(mEtxtGsensorWakeupThreshold.getText().toString());

            ret = setGsensorWakeupThreshold(thr);

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(GsensorDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();
            } else {
                mGsensorWakeupThreshold = "Set error. " + ErrorCode.errorCodeToString(ret);
                mHandler.sendEmptyMessage(R.id.row_gsensor_wakeup_threshold);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mGsensorWakeupThreshold = "Set error. Wrong input.";
            mHandler.sendEmptyMessage(R.id.row_gsensor_wakeup_threshold);
        }
    }



    private void operationGetGsensorData() {
        mGsensorData = getGsensorData();

        mHandler.sendEmptyMessage(R.id.row_gsensor_data);
    }


    private void operationGetGsensorOffset() {
        int ret;
        IVCP_GSENSOR_VALUE offset = new IVCP_GSENSOR_VALUE();
        ret = getGsensorOffset(offset);

        if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
            mGsensorOffsetOperationResult = "Get error. " + ErrorCode.errorCodeToString(ret);
            mGsensorOffsetValueX    = "";
            mGsensorOffsetValueY    = "";
            mGsensorOffsetValueZ    = "";
        } else {
            mGsensorOffsetOperationResult = "Get OK";
            mGsensorOffsetValueX    = String.format("%d", offset.x_mg);
            mGsensorOffsetValueY    = String.format("%d", offset.y_mg);
            mGsensorOffsetValueZ    = String.format("%d", offset.z_mg);
        }
        log("Get offset.x_mg:" + offset.x_mg + " , offset.y_mg: " + offset.y_mg + " , offset.z_mg:" + offset.z_mg);
        mHandler.sendEmptyMessage(R.id.row_gsensor_offset);
    }

    private void operationSetGsensorOffset() {
        int ret;
        IVCP_GSENSOR_VALUE offset = new IVCP_GSENSOR_VALUE();

        try {
            offset.x_mg = Integer.valueOf( mTxtGsensorOffsetValueX.getText().toString()    );
            offset.y_mg = Integer.valueOf( mTxtGsensorOffsetValueY.getText().toString()    );
            offset.z_mg = Integer.valueOf( mTxtGsensorOffsetValueZ.getText().toString()    );

            ret = setGsensorOffset(offset);
            if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                mGsensorOffsetOperationResult = "Set error. " + ErrorCode.errorCodeToString(ret);
            } else {
                mGsensorOffsetOperationResult = "Set OK";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
                mGsensorOffsetOperationResult = "Set error. Wrong input.";

        }
        finally {
            Toast.makeText(GsensorDemoActivity.this, mGsensorOffsetOperationResult, Toast.LENGTH_SHORT ).show();
        }
    }

    private void operationResetGsensorOffset() {
        int ret;
        IVCP_GSENSOR_VALUE offset = new IVCP_GSENSOR_VALUE();

        try {
            offset.x_mg = 0 ;
            offset.y_mg = 0 ;
            offset.z_mg = 0 ;
            ret = setGsensorOffset(offset);
            if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                mGsensorOffsetOperationResult = "Reset error. " + ErrorCode.errorCodeToString(ret);
            } else {
                mGsensorOffsetOperationResult = "Reset OK";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            mGsensorOffsetOperationResult = "Set error. Wrong input.";

        }
        finally {
            Toast.makeText(GsensorDemoActivity.this, mGsensorOffsetOperationResult, Toast.LENGTH_SHORT ).show();
        }
    }

    private void operationCalibrationtGsensorOffset() {
        int ret;
        int sample_time = 10;
        IVCP_GSENSOR_VALUE mean_value = new IVCP_GSENSOR_VALUE();

        try {
            mean_value.x_mg = 0 ;
            mean_value.y_mg = 0 ;
            mean_value.z_mg = 0 ;

            //Get gsensor data 10 times
            for(int i=0;i<sample_time;i++)
            {
                IVCP_GSENSOR_VALUE data = new IVCP_GSENSOR_VALUE();

                ret = mIvcpAPI.ivcp_gsensor_read(data);
                if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                    mGsensorOffsetOperationResult = "Get error. " + ErrorCode.errorCodeToString(ret);
                    break;
                } else {
                    mGsensorOffsetOperationResult = "Get OK";
                }
                mean_value.x_mg += data.x_mg;
                mean_value.y_mg += data.y_mg;
                mean_value.z_mg += data.z_mg;

                Thread.sleep(200);
            }

            //G sensor calibration (Average value)
            IVCP_GSENSOR_VALUE offset = new IVCP_GSENSOR_VALUE();
            offset.x_mg = -1 * (mean_value.x_mg / sample_time);
            offset.y_mg = -1 * (mean_value.y_mg / sample_time);
            if(mSelectedGsensorCalibrationOrientationID == 0){
                offset.z_mg =  1 * (1000 - Math.abs((mean_value.z_mg / sample_time)) ); // calibration to +1g z-axis
            }else{
                offset.z_mg = -1 * (1000 - Math.abs((mean_value.z_mg / sample_time)) ); // calibration to -1g z-axis
            }

            if( offset.x_mg < -2000 || offset.x_mg > 1996 ||
                offset.y_mg < -2000 || offset.y_mg > 1996 ||
                offset.z_mg < -2000 || offset.z_mg > 1996)
            {
                mGsensorOffsetOperationResult = "Keep your device steady during calibration. Please try again.";
                return;
            }

            ret = setGsensorOffset(offset);
            if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                mGsensorOffsetOperationResult = "Calibration error. " + ErrorCode.errorCodeToString(ret);
            } else {
                mGsensorOffsetOperationResult = "Calibration Done";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            mGsensorOffsetOperationResult = "Set error. Wrong input.";

        }
        finally {
            Toast.makeText(GsensorDemoActivity.this, mGsensorOffsetOperationResult, Toast.LENGTH_SHORT ).show();
        }
    }




    private String getGsensorAvailability() {
        int ret;
        boolean[] status = new boolean[1];
        ret = mIvcpAPI.ivcp_gsensor_available(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (status[0]? "AVAILABLE" : "UNAVAILABLE");
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setGsensorStatus(boolean enable) {
        int ret = 0;

        if(enable) {
            ret = mIvcpAPI.ivcp_gsensor_enable();
        } else {
            ret = mIvcpAPI.ivcp_gsensor_disable();
        }

        return ret;
    }

    private String getGsensorStatus() {
        int ret;
        boolean[] status = new boolean[1];
        ret = mIvcpAPI.ivcp_gsensor_get_status(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (status[0]? "ENABLE" : "DISABLE");
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setGsensorResolution(int id) {
        int ret;
        ret = mIvcpAPI.ivcp_gsensor_set_resolution(id);
        return ret;
    }

    private int getGsensorResolution(int[] id) {
        int ret;
        ret = mIvcpAPI.ivcp_gsensor_get_resolution(id);
        return ret;
    }


    private int setGsensorWakeupStatus(boolean enable) {
        int ret = 0;

        if(enable) {
            ret = mIvcpAPI.ivcp_gsensor_wakeup_enable();
        } else {
            ret = mIvcpAPI.ivcp_gsensor_wakeup_disable();
        }

        return ret;
    }

    private String getGsensorWakeupStatus() {
        int ret;
        boolean[] status = new boolean[1];
        ret = mIvcpAPI.ivcp_gsensor_get_wakeup_status(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (status[0]? "ENABLE" : "DISABLE");
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setGsensorWakeupThreshold(int thr) {
        int ret;
        ret = mIvcpAPI.ivcp_gsensor_set_wakeup_threshold(thr);
        return ret;
    }


    private String getGsensorWakeupThreshold() {
        int ret;
        int[] tempThr = new int[1];
        ret = mIvcpAPI.ivcp_gsensor_get_wakeup_threshold(tempThr);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.format("%d",tempThr[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    private String getGsensorData() {
        int ret;
        IVCP_GSENSOR_VALUE temp = new IVCP_GSENSOR_VALUE();
        ret = mIvcpAPI.ivcp_gsensor_read(temp);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.format("x = %d, y = %d, z = %d",temp.x_mg, temp.y_mg, temp.z_mg);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    private int getGsensorOffset(IVCP_GSENSOR_VALUE offset) {
        return mIvcpAPI.ivcp_gsensor_get_offset(offset);
    }

    private int setGsensorOffset(IVCP_GSENSOR_VALUE offset) {
        return mIvcpAPI.ivcp_gsensor_set_offset(offset);
    }

}
