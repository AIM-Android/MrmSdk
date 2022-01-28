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

public class SpeedCounterDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - Spd";
    private String speedCounter;

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }


    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytSpeedCounter;
    TextView mTxtSpeedCounter;
    Button mBtnGetSpeedCounter, mBtnResetSpeedCounter, mBtnGetAndResetSpeedCounter;
    String mSpeedCounter;

    LinearLayout mLytSpeedCounterPerSec;
    TextView mTxtSpeedCounterPerSec;
    Button mBtnStartSpeedCounterMonitor, mBtnStopSpeedCounterMonitor;
    String mSpeedCounterPerSec;

    LinearLayout mLytSpeedCounterThreshold;
    EditText mEtxtSpeedCounterThreshold;
    Button mBtnGetSpeedCounterThreshold, mBtnSetSpeedCounterThreshold;
    String mSpeedCounterThreshold = "";

    //Handlers
    MyHandler mHandler = null;

    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;



    private static class MyHandler extends Handler {
        private WeakReference<SpeedCounterDemoActivity> mActivity = null;

        public MyHandler(SpeedCounterDemoActivity activity) {
            mActivity = new WeakReference<SpeedCounterDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SpeedCounterDemoActivity activity = mActivity.get();

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
        toggleStatusPolling(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        log("\n\n=============== onDestroy() ===============\n\n");
        super.onDestroy();
    }


    private void initView() {
        setContentView(R.layout.activity_speed_counter_demo);

        mProgressDailog             = new ProgressDialog(this);

        mLytSpeedCounter            = (LinearLayout) findViewById(R.id.row_speed_counter);
        mTxtSpeedCounter            = (TextView) findViewById(R.id.txt_speed_counter);
        mBtnGetSpeedCounter         = (Button)   findViewById(R.id.btn_get_speed_counter);
        mBtnResetSpeedCounter       = (Button)   findViewById(R.id.btn_reset_speed_counter);
        mBtnGetAndResetSpeedCounter = (Button)   findViewById(R.id.btn_get_and_reset_speed_counter);

        mLytSpeedCounterPerSec       = (LinearLayout) findViewById(R.id.row_speed_counter_monitor);
        mTxtSpeedCounterPerSec       = (TextView) findViewById(R.id.txt_speed_counter_per_second);
        mBtnStartSpeedCounterMonitor = (Button)   findViewById(R.id.btn_start_speed_counter_monitor);
        mBtnStopSpeedCounterMonitor  = (Button)   findViewById(R.id.btn_stop_speed_counter_monitor);

        mLytSpeedCounterThreshold    = (LinearLayout) findViewById(R.id.row_speed_counter_threshold);
        mEtxtSpeedCounterThreshold   = (EditText) findViewById(R.id.etxt_speed_counter_threshold);
        mBtnGetSpeedCounterThreshold = (Button)   findViewById(R.id.btn_get_speed_counter_threshold);
        mBtnSetSpeedCounterThreshold = (Button)   findViewById(R.id.btn_set_speed_counter_threshold);


        cleanAllFields();
    }


    private void initListener() {
        mBtnGetSpeedCounter.setOnClickListener(myOnClickListener);
        mBtnResetSpeedCounter.setOnClickListener(myOnClickListener);
        mBtnGetAndResetSpeedCounter.setOnClickListener(myOnClickListener);
        mBtnStartSpeedCounterMonitor.setOnClickListener(myOnClickListener);
        mBtnStopSpeedCounterMonitor.setOnClickListener(myOnClickListener);
        mBtnGetSpeedCounterThreshold.setOnClickListener(myOnClickListener);
        mBtnSetSpeedCounterThreshold.setOnClickListener(myOnClickListener);
    }

    View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btn_get_speed_counter:
                    operationGetSpeedCounter(false);
                    break;

                case R.id.btn_reset_speed_counter:
                    operationResetSpeedCounter();
                    break;

                case R.id.btn_get_and_reset_speed_counter:
                    operationGetSpeedCounter(true);
                    break;


                case R.id.btn_start_speed_counter_monitor:
                    toggleStatusPolling(true);
                    break;

                case R.id.btn_stop_speed_counter_monitor:
                    toggleStatusPolling(false);
                    break;

                case R.id.btn_get_speed_counter_threshold:
                    operationGetSpeedCounterThreshold();
                    break;

                case R.id.btn_set_speed_counter_threshold:
                    operationSetSpeedCounterThreshold();
                    break;
                default:
                    break;
            }
        }
    };


    void updateView(int id) {
        switch( id ) {
            case R.id.txt_speed_counter:
                if(mSpeedCounter.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytSpeedCounter.setVisibility(View.GONE);
                    mLytSpeedCounterPerSec.setVisibility(View.GONE);
                    mLytSpeedCounterThreshold.setVisibility(View.GONE);
                }
                if (mTxtSpeedCounter != null)
                    ViewOperator.setLabelText(mTxtSpeedCounter, mSpeedCounter);
                break;

            case R.id.txt_speed_counter_per_second:
                if(mSpeedCounterPerSec.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytSpeedCounterPerSec.setVisibility(View.GONE);
                } else {
                    if (mTxtSpeedCounterPerSec != null) {
                        ViewOperator.setLabelText(mTxtSpeedCounterPerSec, mSpeedCounterPerSec);
                    }
                }
                break;

            case R.id.etxt_speed_counter_threshold:
                if(mSpeedCounterThreshold.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytSpeedCounterThreshold.setVisibility(View.GONE);
                } else {
                    if (mEtxtSpeedCounterThreshold != null)
                        mEtxtSpeedCounterThreshold.setText(mSpeedCounterThreshold);
                }
                break;

            default:
                break;
        }
    }

    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtSpeedCounter, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtSpeedCounterPerSec, DEFAULT_DISPLAY_VALUE_NA);
    }

    void doAsyncTaskRefreshAllFields() {
        mTaskRefreshAllFields = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                operationGetSpeedCounter(false);
                operationGetSpeedCounterThreshold();
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
                int ret;
                long countStartTime = 0, countEndTime = 0;
                long monitorPeriodMs = 3000;

                int[] count = new int[1];
                double countPerSec = 0.0d;


                countStartTime = System.currentTimeMillis();

                try {
                    Thread.sleep(monitorPeriodMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    mSpeedCounterPerSec = "ERR - Exception occurred when thread sleep";
                    isRunning_statusPolling = false;
                    mHandler.sendEmptyMessage(R.id.txt_speed_counter_per_second);
                }

                ret = mIvcpAPI.ivcp_speedcounter_get_and_reset_counter(count);

                if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                    mSpeedCounterPerSec = "Get error. " + ErrorCode.errorCodeToString(ret);
                    mHandler.sendEmptyMessage(R.id.txt_speed_counter_per_second);
                    return;
                }

                countEndTime = System.currentTimeMillis();

                double timespanSec = (countEndTime - countStartTime) / 1000.0d;
                countPerSec = count[0] / timespanSec;
                mSpeedCounterPerSec = String.format("%.0f", countPerSec);

                log(String.format("counter = %d,   timespan = %.3f sec.( %d, %d  )", count[0], timespanSec, countStartTime, countEndTime));
                log(String.format("mSpeedCounterPerSec = %s (%f)", mSpeedCounterPerSec, countPerSec));

                mHandler.sendEmptyMessage(R.id.txt_speed_counter_per_second);
            }
        }
    };

    void toggleStatusPolling(boolean enable) {
        if(enable) {
            if(isRunning_statusPolling == false) {
                isRunning_statusPolling = enable;
                mThreadStatusPolling = new Thread(mThreadRunnableStatusPolling);

                operationResetSpeedCounter();
                mThreadStatusPolling.start();
                log("--------- Start polling status --------- ");

            } else {
                log("--------- Already polling status --------- ");

            }

        } else {
            try {
                if(isRunning_statusPolling == true) {
                    isRunning_statusPolling = enable;
                    if(mThreadStatusPolling != null) {
                        log("--------- Stopping polling status --------- ");
                        if( mThreadStatusPolling.isAlive() ) {
                            mThreadStatusPolling.join();
                        }
                    }

                } else {
                    log("--------- Alreay Stopped polling status --------- ");

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    private void operationGetSpeedCounter(boolean doReset) {
        if (doReset) {
            mSpeedCounter = getAndResetSpeedCounter();
            mHandler.sendEmptyMessage(R.id.txt_speed_counter);
        } else {
            mSpeedCounter = getSpeedCounter();
            mHandler.sendEmptyMessage(R.id.txt_speed_counter);
        }
    }

    private void operationResetSpeedCounter() {
        int ret;

        ret = resetSpeedCounter();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mSpeedCounter = String.format("Reset OK.", ret);
        } else {
            mSpeedCounter = "Reset error. " + ErrorCode.errorCodeToString(ret);
        }
        mHandler.sendEmptyMessage(R.id.txt_speed_counter);
    }

    private void operationGetSpeedCounterThreshold() {
        mSpeedCounterThreshold = getSpeedCounterThreshold();
        mHandler.sendEmptyMessage(R.id.etxt_speed_counter_threshold);
    }


    private void operationSetSpeedCounterThreshold() {
        int ret;
        float refVol;

        try {
            refVol = Float.valueOf(mEtxtSpeedCounterThreshold.getText().toString());
            ret = setSpeedCounterThreshold(refVol);

            if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(SpeedCounterDemoActivity.this, "Set Success!", Toast.LENGTH_SHORT).show();
            } else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mSpeedCounterThreshold = errStr;
                mHandler.sendEmptyMessage(R.id.etxt_speed_counter_threshold);
                Toast.makeText(SpeedCounterDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(SpeedCounterDemoActivity.this, "Wrong input value!", Toast.LENGTH_SHORT).show();
        }
    }



    private String getSpeedCounter() {
        int ret;
        int[] counter = new int[1];

        ret = mIvcpAPI.ivcp_speedcounter_get_counter(counter);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(counter[0]);
        } else {
            return "Get error. " + ErrorCode.errorCodeToString(ret);
        }
    }

    private String getAndResetSpeedCounter() {
        int ret;
        int[] counter = new int[1];

        ret = mIvcpAPI.ivcp_speedcounter_get_and_reset_counter(counter);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(counter[0]);
        } else {
            return "Get and reset error. " + ErrorCode.errorCodeToString(ret);
        }
    }

    private int resetSpeedCounter() {
        int ret;
        ret = mIvcpAPI.ivcp_speedcounter_reset_counter();
        return ret;
    }

    private String getSpeedCounterThreshold() {
        int ret;
        float[] voltage = new float[1];
        ret = mIvcpAPI.ivcp_speedcounter_get_threshold(voltage);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(voltage[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    public int setSpeedCounterThreshold(float voltage) {
        int ret;
        ret = mIvcpAPI.ivcp_speedcounter_set_threshold(voltage);
        return ret;
    }
}
