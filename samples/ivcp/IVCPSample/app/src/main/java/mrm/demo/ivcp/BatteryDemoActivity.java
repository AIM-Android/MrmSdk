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

public class BatteryDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - Batt";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }


    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytPowerStatus;
    TextView mTxtPowerStatus, mTxtPowerVol;
    String mPowerStatus = "";
    String mPowerVol = "";

    LinearLayout mLytBattStatus;
    TextView mTxtBattStatus, mTxtBattVol, mTxtBattAvgCur;
    String mBattStatus = "";
    String mBattVol = "";
    String mBattAvgCur = "";

    LinearLayout mLytBattTemper;
    TextView mTxtBattTemper;
    String mBattTemper;

    LinearLayout mLytBattTimeToEmpty;
    TextView mTxtBattTimeToEmpty;
    String mBattTimeToEmpty;

    LinearLayout mLytBattStateOfCharge;
    TextView mTxtBattStateOfCharge;
    String mBattStateOfCharge;

    LinearLayout mLytBattChargeThreshold;
    EditText mEtxtBattChargeThreshold;
    Button mBtnGetBattChargeThreshold, mBtnSetBattChargeThreshold;
    String mBattChargeThreshold;


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<BatteryDemoActivity> mActivity = null;

        public MyHandler(BatteryDemoActivity activity) {
            mActivity = new WeakReference<BatteryDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            BatteryDemoActivity activity = mActivity.get();

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
        setContentView(R.layout.activity_battery_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytPowerStatus            = (LinearLayout) findViewById(R.id.row_power_status);
        mTxtPowerStatus            = (TextView) findViewById(R.id.txt_power_status);
        mTxtPowerVol               = (TextView) findViewById(R.id.txt_power_voltage);
        mLytBattStatus             = (LinearLayout) findViewById(R.id.row_battery_status);
        mTxtBattStatus             = (TextView) findViewById(R.id.txt_battery_status);
        mTxtBattVol                = (TextView) findViewById(R.id.txt_battery_voltage);
        mTxtBattAvgCur             = (TextView) findViewById(R.id.txt_battery_avg_current);
        mLytBattTemper             = (LinearLayout) findViewById(R.id.row_battery_temper);
        mTxtBattTemper             = (TextView) findViewById(R.id.txt_battery_temper);
        mLytBattTimeToEmpty        = (LinearLayout) findViewById(R.id.row_battery_time_to_empty);
        mTxtBattTimeToEmpty        = (TextView) findViewById(R.id.txt_battery_time_to_empty);
        mLytBattStateOfCharge      = (LinearLayout) findViewById(R.id.row_battery_state_of_charge);
        mTxtBattStateOfCharge      = (TextView) findViewById(R.id.txt_battery_state_of_charge);
        mLytBattChargeThreshold      = (LinearLayout) findViewById(R.id.row_battery_charge_threshold);
        mEtxtBattChargeThreshold   = (EditText) findViewById(R.id.etxt_battery_charge_threshold);
        mBtnGetBattChargeThreshold = (Button)   findViewById(R.id.btn_get_battery_charge_threshold);
        mBtnSetBattChargeThreshold = (Button)   findViewById(R.id.btn_set_battery_charge_threshold);

        cleanAllFields();
    }


    private void initListener() {
        mBtnGetBattChargeThreshold.setOnClickListener(myOnClickListener);
        mBtnSetBattChargeThreshold.setOnClickListener(myOnClickListener);
    }

    View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btn_set_battery_charge_threshold:
                    operationSetBatteryChargeThreshold();
                    break;

                case R.id.btn_get_battery_charge_threshold:
                    operationGetBatteryChargeThreshold();
                    break;
                default:
                    break;
            }
        }
    };

    void updateView(int id) {
        switch( id ) {
            case R.id.txt_power_status:
                if(mPowerStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytPowerStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtPowerStatus != null)
                        ViewOperator.setLabelText(mTxtPowerStatus, mPowerStatus);
                }
                break;

            case R.id.txt_power_voltage:
                if(mTxtPowerVol != null)
                    ViewOperator.setLabelText(mTxtPowerVol, mPowerVol);
                break;

            case R.id.txt_battery_status:
                if(mBattStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytBattStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtBattStatus != null)
                        ViewOperator.setLabelText(mTxtBattStatus, mBattStatus);
                }
                break;

            case R.id.txt_battery_voltage:
                if(mTxtBattVol != null)
                    ViewOperator.setLabelText(mTxtBattVol, mBattVol);
                break;

            case R.id.txt_battery_avg_current:
                if(mTxtBattVol != null)
                    ViewOperator.setLabelText(mTxtBattAvgCur, mBattAvgCur);
                break;

            case R.id.txt_battery_temper:
                if(mBattTemper.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytBattTemper.setVisibility(View.GONE);
                } else {
                    if (mTxtBattTemper != null)
                        ViewOperator.setLabelText(mTxtBattTemper, mBattTemper);
                }
                break;

            case R.id.txt_battery_time_to_empty:
                if(mBattTimeToEmpty.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytBattTimeToEmpty.setVisibility(View.GONE);
                } else {
                    if (mTxtBattTimeToEmpty != null)
                        ViewOperator.setLabelText(mTxtBattTimeToEmpty, mBattTimeToEmpty);
                }
                break;

            case R.id.txt_battery_state_of_charge:
                if(mBattStateOfCharge.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytBattStateOfCharge.setVisibility(View.GONE);
                } else {
                    if (mTxtBattStateOfCharge != null)
                        ViewOperator.setLabelText(mTxtBattStateOfCharge, mBattStateOfCharge);
                }
                break;

            case R.id.etxt_battery_charge_threshold:
                if(mBattChargeThreshold.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytBattChargeThreshold.setVisibility(View.GONE);
                } else {
                    if (mEtxtBattChargeThreshold != null)
                        mEtxtBattChargeThreshold.setText(mBattChargeThreshold);
                }
                break;

            default:
                break;
        }
    }

    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtPowerStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtPowerVol, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattVol, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattAvgCur, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattTemper, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattTimeToEmpty, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattStateOfCharge, DEFAULT_DISPLAY_VALUE_NA);
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
                operationGetAllPowerStatus();
                operationGetAllBatteryStatus();
                operationGetBatteryChargeThreshold();
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
                operationGetAllPowerStatus();
                operationGetAllBatteryStatus();

                String errorStr = ErrorCode.MSG_TAG_ERROR;
                if(
                        mPowerStatus.contains(errorStr) || mPowerVol.contains(errorStr) ||
                        mBattStatus.contains(errorStr) || mBattVol.contains(errorStr) || mBattAvgCur.contains(errorStr) ||
                        mBattTemper.contains(errorStr) || mBattTimeToEmpty.contains(errorStr) || mBattStateOfCharge.contains(errorStr) ) {
                        if(mPowerStatus.contains(errorStr))
                            log("PowerStatus ERROR code : "+ mPowerStatus);
                        if(mPowerVol.contains(errorStr))
                            log("PowerVoltage ERROR code : "+ mPowerVol);
                        if(mBattStatus.contains(errorStr))
                            log("BatteryStatus ERROR code : "+ mBattStatus);
                        if(mBattVol.contains(errorStr))
                            log("BatteryVoltage ERROR code : "+ mBattVol);
                        if(mBattAvgCur.contains(errorStr))
                            log("BatteryAvgCur ERROR code : "+ mBattAvgCur);
                        if(mBattTemper.contains(errorStr))
                            log("BatteryTemperature ERROR code : "+ mBattTemper);
                        if(mBattTimeToEmpty.contains(errorStr))
                            log("BatteryTimeToEmpty ERROR code : "+ mBattTimeToEmpty);
                        if(mBattStateOfCharge.contains(errorStr))
                            log("BatteryStateOfCharge ERROR code : "+ mBattStateOfCharge);
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






    private void operationGetAllPowerStatus() {
        mPowerStatus = getPowerStatus();
        mHandler.sendEmptyMessage(R.id.txt_power_status);

        mPowerVol = getPowerVoltage();
        mHandler.sendEmptyMessage(R.id.txt_power_voltage);
    }


    private void operationGetAllBatteryStatus() {
        mBattStatus = getBatteryStatus();
        mHandler.sendEmptyMessage(R.id.txt_battery_status);

        mBattVol = getBatteryVoltage();
        mHandler.sendEmptyMessage(R.id.txt_battery_voltage);

        mBattAvgCur = getBatteryAverageCurrent();
        mHandler.sendEmptyMessage(R.id.txt_battery_avg_current);

        mBattTemper = getBatteryTemperature();
        mHandler.sendEmptyMessage(R.id.txt_battery_temper);

        mBattTimeToEmpty = getBatteryTimeToEmpty();
        mHandler.sendEmptyMessage(R.id.txt_battery_time_to_empty);

        mBattStateOfCharge = getBatteryStateOfCharge();
        mHandler.sendEmptyMessage(R.id.txt_battery_state_of_charge);
    }


    private void operationGetBatteryChargeThreshold() {
        mBattChargeThreshold = getBatteryChargeThreshold();
        mHandler.sendEmptyMessage(R.id.etxt_battery_charge_threshold);
    }


    private void operationSetBatteryChargeThreshold() {
        int ret;
        float chargeThres;

        try {
            chargeThres = Float.valueOf(mEtxtBattChargeThreshold.getText().toString());
            ret = setBatteryChargeThreshold(chargeThres);

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(BatteryDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

            } else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mBattChargeThreshold = errStr;
                mHandler.sendEmptyMessage(R.id.etxt_battery_charge_threshold);
                Toast.makeText(BatteryDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }

        } catch(Exception ex) {
            ex.printStackTrace();
            Toast.makeText(BatteryDemoActivity.this, "Wrong input value!", Toast.LENGTH_SHORT).show();

        }
    }






    private String getPowerStatus() {
        int ret;
        boolean[] carPowerStatus = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_power_status(carPowerStatus);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (carPowerStatus[0] ? "ON" : "OFF" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getPowerVoltage() {
        int ret;
        float[] carPowerVol = new float[1];
        ret = mIvcpAPI.ivcp_pm_get_voltage(carPowerVol);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(carPowerVol[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getBatteryStatus() {
        int ret;
        boolean[] batteryStatus = new boolean[1];
        ret = mIvcpAPI.ivcp_battery_get_status(batteryStatus);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (batteryStatus[0] ? "ON" : "OFF" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getBatteryVoltage() {
        int ret;
        int[] batteryVol = new int[1];
        ret = mIvcpAPI.ivcp_battery_get_voltage(batteryVol);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(batteryVol[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getBatteryAverageCurrent() {
        int ret;
        int[] batteryAvgCur = new int[1];
        ret = mIvcpAPI.ivcp_battery_get_average_current(batteryAvgCur);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(batteryAvgCur[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    private String getBatteryTemperature() {
        int ret;
        float[] temper = new float[1];
        ret = mIvcpAPI.ivcp_battery_get_temperature(temper);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(temper[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getBatteryTimeToEmpty() {
        int ret;
        int[] timeMin = new int[1];
        ret = mIvcpAPI.ivcp_battery_get_time_to_empty(timeMin);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(timeMin[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getBatteryStateOfCharge() {
        int ret;
        int[] percentage = new int[1];
        ret = mIvcpAPI.ivcp_battery_get_state_of_charge(percentage);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(percentage[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getBatteryChargeThreshold() {
        int ret;
        float[] chargeThres = new float[1];
        ret = mIvcpAPI.ivcp_battery_get_start_charge_threshold(chargeThres);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(chargeThres[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setBatteryChargeThreshold(float chargeThres) {
        int ret;
        ret = mIvcpAPI.ivcp_battery_set_start_charge_threshold(chargeThres);
        return ret;
    }

}
