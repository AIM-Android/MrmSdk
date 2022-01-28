package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import mrm.client.IVCPServiceClient;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;


public class PsensorDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - Psen";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytPsensorAvailability;
    TextView mTxtPsensorAvailability;
    String mPsensorAvailability = "";

    LinearLayout mLytPsensorStatus;
    TextView mTxtPsensorStatus;
    String mPsensorStatus = "";
    Button mBtnPsensorEnable, mBtnPsensorDisable;

    LinearLayout mLytPsensorPressure;
    TextView mTxtPsensorPressure;
    String mPsensorPressure = "";

    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<PsensorDemoActivity> mActivity = null;

        public MyHandler(PsensorDemoActivity activity) {
            mActivity = new WeakReference<PsensorDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PsensorDemoActivity activity = mActivity.get();

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
        toggleStatusPolling(true);
    }

    @Override
    protected void onPause() {
        log("\n\n=============== onPause() ===============\n\n");
        super.onPause();

        toggleStatusPolling(false);
    }

    @Override
    protected void onDestroy() {
        log("\n\n=============== onDestroy() ===============\n\n");
        super.onDestroy();
    }

    private void initView() {
        setContentView(R.layout.activity_psensor_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytPsensorAvailability = (LinearLayout) findViewById(R.id.row_psensor_available);
        mTxtPsensorAvailability = (TextView) findViewById(R.id.txt_psensor_availability);

        mLytPsensorStatus = (LinearLayout) findViewById(R.id.row_psensor_status);
        mTxtPsensorStatus = (TextView) findViewById(R.id.txt_psensor_status);
        mBtnPsensorEnable = (Button)   findViewById(R.id.btn_psensor_enable);
        mBtnPsensorDisable = (Button)   findViewById(R.id.btn_psensor_disable);

        mLytPsensorPressure = (LinearLayout) findViewById(R.id.row_psensor_pressure);
        mTxtPsensorPressure = (TextView) findViewById(R.id.txt_psensor_pressure);

        cleanAllFields();
    }


    private void initListener() {
        mBtnPsensorEnable.setOnClickListener(mBtnOnClickListener);
        mBtnPsensorDisable.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_psensor_enable:
                    operationSetPsensorStatus(true);
                    break;
                case R.id.btn_psensor_disable:
                    operationSetPsensorStatus(false);
                    break;
                default:
                    break;
            }
        }
    };

    void updateView(int id) {
        switch( id ) {
            case R.id.txt_psensor_availability:
                if(mPsensorAvailability.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytPsensorAvailability.setVisibility(View.GONE);
                    mLytPsensorStatus.setVisibility(View.GONE);
                    mLytPsensorPressure.setVisibility(View.GONE);
                }
                if(mTxtPsensorAvailability != null)
                    ViewOperator.setLabelText(mTxtPsensorAvailability, mPsensorAvailability);
                break;

            case R.id.txt_psensor_status:
                if(mPsensorStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytPsensorStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtPsensorStatus != null)
                        ViewOperator.setLabelText(mTxtPsensorStatus, mPsensorStatus);
                }
                break;

            case R.id.txt_psensor_pressure:
                if(mPsensorPressure.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytPsensorPressure.setVisibility(View.GONE);
                } else {
                    if (mTxtPsensorPressure != null)
                        mTxtPsensorPressure.setText(mPsensorPressure);
                }
                break;

            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtPsensorStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtPsensorPressure, DEFAULT_DISPLAY_VALUE_NA);
    }


    void doAsyncTaskRefreshAllFields() {
        mTaskRefreshAllFields = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading Current Status ...");
                mProgressDailog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                operationGetPsensorAvailability();
                operationGetPsensorStatus();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        }.execute();
    }



    Runnable mThreadRunnableStatusPolling = new Runnable() {
        @Override
        public void run() {
            while(isRunning_statusPolling) {

                operationGetPressure();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    void toggleStatusPolling(boolean enable) {
        if(enable) {
            isRunning_statusPolling = enable;
            mThreadStatusPolling = new Thread(mThreadRunnableStatusPolling);
            mThreadStatusPolling.start();
            log("--------- Start polling status --------- ");

        } else {
            try {
                isRunning_statusPolling = enable;
                if(mThreadStatusPolling != null) {
                    if( mThreadStatusPolling.isAlive() ) {
                        mThreadStatusPolling.join();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log("--------- Stop polling status --------- ");
        }
    }




    private void operationGetPsensorAvailability() {
        mPsensorAvailability = getPsensorAvailability();
        mHandler.sendEmptyMessage(R.id.txt_psensor_availability);
    }


    private void operationSetPsensorStatus(boolean status) {
        int ret;

        ret = setPsensorStatus(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mPsensorStatus = (status ? "ENABLE" : "DISABLE");
        } else {
            mPsensorStatus = "Set error. " + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.txt_psensor_status);
    }

    private void operationGetPsensorStatus() {
        mPsensorStatus = getPsensorStatus();
        mHandler.sendEmptyMessage(R.id.txt_psensor_status);
    }

    private void operationGetPressure() {
        mPsensorPressure = getPressure();
        mHandler.sendEmptyMessage(R.id.txt_psensor_pressure);
    }





    private String getPsensorAvailability() {
        int ret;
        boolean[] status = new boolean[1];
        ret = mIvcpAPI.ivcp_psensor_available(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (status[0]? "AVAILABLE" : "UNAVAILABLE");
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setPsensorStatus(boolean enable) {
        int ret = 0;

        if(enable) {
            ret = mIvcpAPI.ivcp_psensor_enable();
        } else {
            ret = mIvcpAPI.ivcp_psensor_disable();
        }

        return ret;
    }

    private String getPsensorStatus() {
        int ret;
        boolean[] status = new boolean[1];
        ret = mIvcpAPI.ivcp_psensor_get_status(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (status[0]? "ENABLE" : "DISABLE");
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getPressure() {
        int ret;
        int[] pressure = new int[1];
        ret = mIvcpAPI.ivcp_psensor_get_pressure(pressure);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(pressure[0]);
        } else {
            return "Get error. " + ErrorCode.errorCodeToString(ret);
        }
    }
}