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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import mrm.client.IVCPServiceClient;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;

public class WatchdogDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - WD";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytWatchdogStatus;
    TextView mTxtWatchdogStatus;
    String mWatchdogStatus = "";
    Button mBtnWatchdogEnable, mBtnWatchdogDisable;

    LinearLayout mLytWatchdogTime;
    EditText mEtxtWatchdogTime;
    Button mBtnGetWatchdogTime, mBtnSetWatchdogTime;
    String mWatchdogTime = "";

    TextView mTxtWatchdogTriggerResult;
    LinearLayout mLytWatchdogCount;
    TextView mTxtWatchdogCount;
    Button mBtnWatchdogTrigger;
    String mWatchdogCount = "";
    String mWatchdogTriggerResult = "";

    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<WatchdogDemoActivity> mActivity = null;

        public MyHandler(WatchdogDemoActivity activity) {
            mActivity = new WeakReference<WatchdogDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchdogDemoActivity activity = mActivity.get();

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
        setContentView(R.layout.activity_watchdog_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytWatchdogStatus  = (LinearLayout) findViewById(R.id.row_watchdog_status);
        mTxtWatchdogStatus  = (TextView) findViewById(R.id.txt_watchdog_status);
        mBtnWatchdogEnable  = (Button)   findViewById(R.id.btn_watchdog_enable);
        mBtnWatchdogDisable = (Button)   findViewById(R.id.btn_watchdog_disable);

        mLytWatchdogTime    = (LinearLayout) findViewById(R.id.row_watchdog_time_setting);
        mEtxtWatchdogTime   = (EditText) findViewById(R.id.etxt_watchdog_time);
        mBtnGetWatchdogTime = (Button)   findViewById(R.id.btn_get_watchdog_time);
        mBtnSetWatchdogTime = (Button)   findViewById(R.id.btn_set_watchdog_time);

        mTxtWatchdogTriggerResult = (TextView) findViewById(R.id.txt_watchdog_trigger_result);
        mLytWatchdogCount         = (LinearLayout) findViewById(R.id.row_watchdog_count);
        mTxtWatchdogCount         = (TextView) findViewById(R.id.txt_watchdog_count);
        mBtnWatchdogTrigger       = (Button)   findViewById(R.id.btn_watchdog_trigger);

        cleanAllFields();
    }


    private void initListener() {
        mBtnWatchdogEnable.setOnClickListener(mBtnOnClickListener);
        mBtnWatchdogDisable.setOnClickListener(mBtnOnClickListener);

        mBtnGetWatchdogTime.setOnClickListener(mBtnOnClickListener);
        mBtnSetWatchdogTime.setOnClickListener(mBtnOnClickListener);

        mBtnWatchdogTrigger.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_watchdog_enable:
                    operationSetWatchdogStatus(true);
                    break;
                case R.id.btn_watchdog_disable:
                    operationSetWatchdogStatus(false);
                    break;

                case R.id.btn_get_watchdog_time:
                    operationGetWatchdogTime();
                    break;
                case R.id.btn_set_watchdog_time:
                    operationSetWatchdogTime();
                    break;

                case R.id.btn_watchdog_trigger:
                    operationGetWatchdogTrigger();
                    break;


                default:
                    break;
            }
        }
    };

    void updateView(int id) {
        switch( id ) {
            case R.id.txt_watchdog_status:
                if(mWatchdogStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytWatchdogStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtWatchdogStatus != null)
                        ViewOperator.setLabelText(mTxtWatchdogStatus, mWatchdogStatus);
                }
                break;

            case R.id.etxt_watchdog_time:
                if(mWatchdogTime.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytWatchdogTime.setVisibility(View.GONE);
                } else {
                    if (mEtxtWatchdogTime != null)
                        mEtxtWatchdogTime.setText(mWatchdogTime);
                }
                break;

            case R.id.txt_watchdog_count:
                if(mWatchdogCount.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytWatchdogCount.setVisibility(View.GONE);
                } else {
                    if (mTxtWatchdogCount != null)
                        mTxtWatchdogCount.setText(mWatchdogCount);
                }
                break;

            case R.id.txt_watchdog_trigger_result:
                if(mWatchdogTriggerResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytWatchdogCount.setVisibility(View.GONE);
                } else {
                    if (mTxtWatchdogTriggerResult != null)
                        if (mWatchdogTriggerResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                            mTxtWatchdogTriggerResult.setVisibility(View.VISIBLE);
                        } else {
                            mTxtWatchdogTriggerResult.setVisibility(View.GONE);
                        }
                    ViewOperator.setLabelText(mTxtWatchdogTriggerResult, mWatchdogTriggerResult);
                }
                break;



            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtWatchdogStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtWatchdogCount, DEFAULT_DISPLAY_VALUE_NA);
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
                operationGetWatchdogTime();
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

                operationGetWatchdogCurrentTime();

                String errorStr = ErrorCode.MSG_TAG_ERROR;
                if( mWatchdogCount.contains(errorStr) ) {
                    log("ERROR while polling!!!  Stop Poliing");
                    isRunning_statusPolling = false;
                }

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

    private void operationSetWatchdogStatus(boolean status) {
        int ret;

        ret = setWatchdogStatus(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mWatchdogStatus = (status ? "ENABLE" : "DISABLE");
        } else {
            mWatchdogStatus = "Set error. " + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.txt_watchdog_status);
    }

    private void operationGetWatchdogTime() {
        mWatchdogTime = getWatchdogTime();
        mHandler.sendEmptyMessage(R.id.etxt_watchdog_time);
    }

    private void operationSetWatchdogTime() {
        try {
            int ret = 0;
            int time = Integer.valueOf(mEtxtWatchdogTime.getText().toString());

            ret = setWatchdogTime( time );

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                mWatchdogTime = "Set OK";
                Toast.makeText(WatchdogDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();
            } else {
                mWatchdogTime = "Set error. " + ErrorCode.errorCodeToString(ret);
                mHandler.sendEmptyMessage(R.id.etxt_watchdog_time);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mWatchdogTime = "Set error. Wrong input.";
            mHandler.sendEmptyMessage(R.id.etxt_watchdog_time);
        }
    }

    private void operationGetWatchdogTrigger() {
        int ret;

        ret = resetWatchdogCurrentTime();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mWatchdogTriggerResult = "Trigger OK";
            Toast.makeText(WatchdogDemoActivity.this, "Trigger OK", Toast.LENGTH_SHORT).show();
        } else {
            String errStr = "Trigger error. " + ErrorCode.errorCodeToString(ret);
            mWatchdogTriggerResult = errStr;
            Toast.makeText(WatchdogDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
        }
        mHandler.sendEmptyMessage(R.id.txt_watchdog_trigger_result);
    }


    private void operationGetWatchdogCurrentTime() {
        mWatchdogCount = getWatchdogCurrentTime();
        mHandler.sendEmptyMessage(R.id.txt_watchdog_count);
    }







    private int setWatchdogStatus(boolean enable) {
        int ret;

        if(enable) {
            ret = mIvcpAPI.ivcp_watchdog_enable();
        } else {
            ret = mIvcpAPI.ivcp_watchdog_disable();
        }

        return ret;
    }

    private String getWatchdogTime() {
        int ret;
        int[] time = new int[1];
        ret = mIvcpAPI.ivcp_watchdog_get_time(time);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(time[0]);
        } else {
            return "Get error. " + ErrorCode.errorCodeToString(ret);
        }
    }


    private int setWatchdogTime(int time) {
        int ret;
        ret = mIvcpAPI.ivcp_watchdog_set_time(time);
        return ret;
    }



    private String getWatchdogCurrentTime() {
        int ret;
        int[] count = new int[1];
        ret = mIvcpAPI.ivcp_watchdog_get_current_time(count);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(count[0]);
        } else {
            return "Get error. " + ErrorCode.errorCodeToString(ret);
        }
    }


    private int resetWatchdogCurrentTime() {
        int ret;
        ret = mIvcpAPI.ivcp_watchdog_trigger();
        return ret;
    }

}
