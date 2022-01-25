package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import mrm.define.MRM_ENUM;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;


public class PowerManagementDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - PM";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytPowerOff;
    TextView mTxtPowerOffResult;
    Button mBtnPowerOff;
    String mPowerOffResult = "";

    LinearLayout mLytIgnStatus;
    TextView mTxtIgnStatus;
    String mIgnStatus = "";
    LinearLayout mLytIgnWakeup;
    TextView mTxtIgnWakeupStatus;
    Button mBtnIgnWakeupEnable, mBtnIgnWakeupDisable;
    String mIgnWakeupStatus = "";

    LinearLayout mLytPowerMode;
    TextView mTxtPowerMode;
    Button mBtnGetPowerMode, mBtnSetPowerMode12V, mBtnSetPowerMode24V;
    String mPowerMode = "";
    LinearLayout mLytPowerStatus;
    TextView mTxtPowerStatus, mTxtPowerVol;
    String mPowerStatus = "";
    String mPowerVol = "";
    LinearLayout mLytBattStatus;
    TextView mTxtBattStatus, mTxtBattVol, mTxtBattAvgCur;
    String mBattStatus = "";
    String mBattVol    = "";
    String mBattAvgCur = "";

    LinearLayout mLytResetLVPThreshold;
    TextView mTxtResetLVPThresholdResult;
    Button mBtnResetLVPThreshold;
    String mResetLVPThresholdResult = "";

    LinearLayout mLytGetLVPRange;
    TextView mTxtLVPRangeResult;
    Button mBtnGetLVPRange;
    String mLVPRangeResult = "";

    LinearLayout mLytLVPPreboot;
    TextView mTxtLVPPrebootStatus;
    Button mBtnLVPPrebootEnable, mBtnLVPPrebootDisable;
    String mLVPPrebootStatus = "";
    LinearLayout mLytLVPPrebootThreshold;
    EditText mEtxtLVPPrebootThreshold;
    Button mBtnGetLVPPrebootThreshold, mBtnSetLVPPrebootThreshold;
    String mLVPPrebootThreshold= "";

    LinearLayout mLytLVPPostboot;
    TextView mTxtLVPPostbootStatus;
    Button mBtnLVPPostbootEnable, mBtnLVPPostbootDisable;
    String mLVPPostbootStatus = "";
    LinearLayout mLytLVPPostbootThreshold;
    EditText mEtxtLVPPostbootThreshold;
    Button mBtnGetLVPPostbootThreshold, mBtnSetLVPPostbootThreshold;
    String mLVPPostbootThreshold= "";

    LinearLayout mLytEventDelay;
    Spinner mSpnEventDelayType;
    BaseAdapter mEventDelayTypeSpinnerDataAdapter;
    ArrayList<EventDelayTypeSpinnerItem> mEventDelayTypeSpinnerItemList;
    class EventDelayTypeSpinnerItem {
        public String displayStr;
        public int    value;

        public EventDelayTypeSpinnerItem(String str, int val) {
            displayStr = str;
            value = val;
        }
    }
    EditText mEtxtEventDelayValue;
    Button mBtnGetEventDelay, mBtnSetEventDelay;
    String mEventDelay = "";
    int mSelectedEventTypeID;

    LinearLayout mLytKeepAliveMode;
    TextView mTxtKeepAliveModeStatus;
    Button mBtnKeepAliveModeEnable, mBtnKeepAliveModeDisable;
    String mKeepAliveModeStatus = "";

    LinearLayout mLytATMode;
    TextView mTxtATModeStatus;
    Button mBtnATModeEnable, mBtnATModeDisable;
    String mATModeStatus = "";

    LinearLayout mLytLastWakeupSource;
    TextView mTxtLastWakeupSource;
    String mLastWakeupSource = "";

    LinearLayout mLytShutdownMaskStatusIgn;
    TextView mTxtShutdownMaskStatusIgn;
    Button mBtnShutdownMaskStatusIgnEnable, mBtnShutdownMaskStatusIgnDisable;
    String mShutdownMaskStatusIgn = "";

    LinearLayout mLytShutdownMaskStatusPowerbtn;
    TextView mTxtShutdownMaskStatusPowerbtn;
    Button mBtnShutdownMaskStatusPowerbtnEnable, mBtnShutdownMaskStatusPowerbtnDisable;
    String mShutdownMaskStatusPowerbtn = "";

    LinearLayout mLytForceShutdownStatus;
    TextView mTxtForceShutdownStatus;
    Button mBtnForceShutdownStatusEnable, mBtnForceShutdownStatusDisable;
    String mForceShutdownStatus = "";

    LinearLayout mLytForceShutdownDelay;
    EditText mEtxtForceShutdownDelay;
    Button mBtnGetForceShutdownDelay, mBtnSetForceShutdownDelay;
    String mForceShutdownDelay= "";



    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<PowerManagementDemoActivity> mActivity = null;

        public MyHandler(PowerManagementDemoActivity activity) {
            mActivity = new WeakReference<PowerManagementDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PowerManagementDemoActivity activity = mActivity.get();

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

    private void setEventDelatTypeList() {
        mEventDelayTypeSpinnerItemList = new ArrayList<EventDelayTypeSpinnerItem>();
        mEventDelayTypeSpinnerItemList.clear();
        mEventDelayTypeSpinnerItemList.add(new EventDelayTypeSpinnerItem("IGNITION_OFF", MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_IGNITION_OFF_TO_POWER_OFF.getValue()));
        mEventDelayTypeSpinnerItemList.add(new EventDelayTypeSpinnerItem("IGNITION_OFF_HARD", MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_IGNITION_OFF_HARD.getValue()));
        mEventDelayTypeSpinnerItemList.add(new EventDelayTypeSpinnerItem("IGNITION_ON", MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_IGNITION_ON.getValue()));
        mEventDelayTypeSpinnerItemList.add(new EventDelayTypeSpinnerItem("LOW_VOLTAGE", MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_LOW_VOLTAGE.getValue()));
        mEventDelayTypeSpinnerItemList.add(new EventDelayTypeSpinnerItem("LOW_VOLTAGE_HARD", MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_LOW_VOLTAGE_HARD.getValue()));
        mEventDelayTypeSpinnerItemList.add(new EventDelayTypeSpinnerItem("POST_BOOT_POWER_CHECK", MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_POST_BOOT_POWER_CHECK.getValue()));
    }


    private void initView() {
        setContentView(R.layout.activity_power_management_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytPowerOff                = (LinearLayout) findViewById(R.id.row_power_off);
        mTxtPowerOffResult          = (TextView) findViewById(R.id.txt_power_off_result);
        mBtnPowerOff                = (Button)   findViewById(R.id.btn_power_off);

        mLytIgnStatus               = (LinearLayout) findViewById(R.id.row_ign_status);
        mTxtIgnStatus               = (TextView) findViewById(R.id.txt_ignition_status);
        mLytIgnWakeup               = (LinearLayout) findViewById(R.id.row_ign_wakeup);
        mTxtIgnWakeupStatus         = (TextView) findViewById(R.id.txt_ign_wakeup_status);
        mBtnIgnWakeupEnable         = (Button)   findViewById(R.id.btn_ign_wakeup_enable);
        mBtnIgnWakeupDisable        = (Button)   findViewById(R.id.btn_ign_wakeup_disable);

        mLytPowerMode               = (LinearLayout) findViewById(R.id.row_power_mode);
        mTxtPowerMode               = (TextView) findViewById(R.id.txt_power_mode);
        mBtnGetPowerMode            = (Button)   findViewById(R.id.btn_get_power_mode);
        mBtnSetPowerMode12V         = (Button)   findViewById(R.id.btn_set_power_mode_12v);
        mBtnSetPowerMode24V         = (Button)   findViewById(R.id.btn_set_power_mode_24v);

        mLytPowerStatus             = (LinearLayout) findViewById(R.id.row_power_status);
        mTxtPowerStatus             = (TextView) findViewById(R.id.txt_power_status);
        mTxtPowerVol                = (TextView) findViewById(R.id.txt_power_voltage);
        mLytBattStatus              = (LinearLayout) findViewById(R.id.row_battery_status);
        mTxtBattStatus              = (TextView) findViewById(R.id.txt_battery_status);
        mTxtBattVol                 = (TextView) findViewById(R.id.txt_battery_voltage);
        mTxtBattAvgCur              = (TextView) findViewById(R.id.txt_battery_avg_current);

        mLytLVPPreboot              = (LinearLayout) findViewById(R.id.row_lvp_preboot_status);
        mTxtLVPPrebootStatus        = (TextView) findViewById(R.id.txt_lvp_preboot_status);
        mBtnLVPPrebootEnable        = (Button)   findViewById(R.id.btn_lvp_preboot_enable);
        mBtnLVPPrebootDisable       = (Button)   findViewById(R.id.btn_lvp_preboot_disable);
        mLytLVPPrebootThreshold     = (LinearLayout) findViewById(R.id.row_lvp_preboot_threshold);
        mEtxtLVPPrebootThreshold    = (EditText) findViewById(R.id.etxt_lvp_preboot_threshold);
        mBtnGetLVPPrebootThreshold  = (Button)   findViewById(R.id.btn_get_lvp_preboot_threshold);
        mBtnSetLVPPrebootThreshold  = (Button)   findViewById(R.id.btn_set_lvp_preboot_threshold);

        mLytLVPPostboot              = (LinearLayout) findViewById(R.id.row_lvp_postboot_status);
        mTxtLVPPostbootStatus        = (TextView) findViewById(R.id.txt_lvp_postboot_status);
        mBtnLVPPostbootEnable        = (Button)   findViewById(R.id.btn_lvp_postboot_enable);
        mBtnLVPPostbootDisable       = (Button)   findViewById(R.id.btn_lvp_postboot_disable);
        mLytLVPPostbootThreshold     = (LinearLayout) findViewById(R.id.row_lvp_postboot_threshold);
        mEtxtLVPPostbootThreshold    = (EditText) findViewById(R.id.etxt_lvp_postboot_threshold);
        mBtnGetLVPPostbootThreshold  = (Button)   findViewById(R.id.btn_get_lvp_postboot_threshold);
        mBtnSetLVPPostbootThreshold  = (Button)   findViewById(R.id.btn_set_lvp_postboot_threshold);

        mLytResetLVPThreshold       = (LinearLayout) findViewById(R.id.row_reset_lvp_threshold);
        mTxtResetLVPThresholdResult = (TextView) findViewById(R.id.txt_reset_lvp_threshold_result);
        mBtnResetLVPThreshold       = (Button)   findViewById(R.id.btn_reset_lvp_threshold);
        mLytGetLVPRange             = (LinearLayout) findViewById(R.id.row_lvp_range);
        mTxtLVPRangeResult          = (TextView) findViewById(R.id.txt_lvp_range_result);
        mBtnGetLVPRange             = (Button)   findViewById(R.id.btn_get_lvp_range);

        mLytKeepAliveMode            = (LinearLayout) findViewById(R.id.row_keep_alive_mode);
        mTxtKeepAliveModeStatus      = (TextView) findViewById(R.id.txt_alive_mode_status);
        mBtnKeepAliveModeEnable      = (Button)   findViewById(R.id.btn_alive_mode_enable);
        mBtnKeepAliveModeDisable     = (Button)   findViewById(R.id.btn_alive_mode_disable);

        mLytATMode                   = (LinearLayout) findViewById(R.id.row_at_mode);
        mTxtATModeStatus             = (TextView) findViewById(R.id.txt_at_mode_status);
        mBtnATModeEnable             = (Button)   findViewById(R.id.btn_at_mode_enable);
        mBtnATModeDisable            = (Button)   findViewById(R.id.btn_at_mode_disable);

        mLytLastWakeupSource         = (LinearLayout) findViewById(R.id.row_last_wakaup_source);
        mTxtLastWakeupSource         = (TextView) findViewById(R.id.txt_last_wakaup_source);

        mLytShutdownMaskStatusIgn        = (LinearLayout) findViewById(R.id.row_shutdown_mask_status_ign);
        mTxtShutdownMaskStatusIgn        = (TextView) findViewById(R.id.txt_shutdown_mask_status_ign);
        mBtnShutdownMaskStatusIgnEnable  = (Button)   findViewById(R.id.btn_shutdown_mask_status_ign_enable);
        mBtnShutdownMaskStatusIgnDisable = (Button)   findViewById(R.id.btn_shutdown_mask_status_ign_disable);

        mLytShutdownMaskStatusPowerbtn        = (LinearLayout) findViewById(R.id.row_shutdown_mask_status_powerbtn);
        mTxtShutdownMaskStatusPowerbtn        = (TextView) findViewById(R.id.txt_shutdown_mask_status_powerbtn);
        mBtnShutdownMaskStatusPowerbtnEnable  = (Button)   findViewById(R.id.btn_shutdown_mask_status_powerbtn_enable);
        mBtnShutdownMaskStatusPowerbtnDisable = (Button)   findViewById(R.id.btn_shutdown_mask_status_powerbtn_disable);

        mLytForceShutdownStatus        = (LinearLayout) findViewById(R.id.row_force_shutdown_status);
        mTxtForceShutdownStatus        = (TextView) findViewById(R.id.txt_force_shutdown_status);
        mBtnForceShutdownStatusEnable  = (Button)   findViewById(R.id.btn_force_shutdown_enable);
        mBtnForceShutdownStatusDisable = (Button)   findViewById(R.id.btn_force_shutdown_disable);

        mLytEventDelay        = (LinearLayout) findViewById(R.id.row_event_delay);
        mEtxtEventDelayValue  = (EditText) findViewById(R.id.etxt_event_delay_value);
        mSpnEventDelayType    = (Spinner) findViewById(R.id.spn_event_delay_type);
        mBtnGetEventDelay     = (Button) findViewById(R.id.btn_get_event_delay);
        mBtnSetEventDelay     = (Button) findViewById(R.id.btn_set_event_delay);

        mLytForceShutdownDelay    = (LinearLayout) findViewById(R.id.row_force_shutdown_delay);
        mEtxtForceShutdownDelay   = (EditText) findViewById(R.id.etxt_force_shutdown_delay);
        mBtnGetForceShutdownDelay = (Button)   findViewById(R.id.btn_get_force_shutdown_delay);
        mBtnSetForceShutdownDelay = (Button)   findViewById(R.id.btn_set_force_shutdown_delay);

        setEventDelatTypeList();
        mEventDelayTypeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mEventDelayTypeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mEventDelayTypeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                EventDelayTypeSpinnerItem currentItem = mEventDelayTypeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(15.0f);
                textView.setPadding(5,10,0,10);
                textView.setText (currentItem.displayStr );
                textView.setTextColor (Color.BLACK);
                return textView;
            }
        };
        mSpnEventDelayType.setAdapter(mEventDelayTypeSpinnerDataAdapter);

        cleanAllFields();
    }


    private void initListener() {
        mSpnEventDelayType.setOnItemSelectedListener(spnEventDelayTypeOnItemSelectedListener);

        mBtnPowerOff.setOnClickListener(mBtnOnClickListener);

        mBtnGetPowerMode.setOnClickListener(mBtnOnClickListener);
        mBtnSetPowerMode12V.setOnClickListener(mBtnOnClickListener);
        mBtnSetPowerMode24V.setOnClickListener(mBtnOnClickListener);

        mBtnIgnWakeupEnable.setOnClickListener(mBtnOnClickListener);
        mBtnIgnWakeupDisable.setOnClickListener(mBtnOnClickListener);

        mBtnLVPPrebootEnable.setOnClickListener(mBtnOnClickListener);
        mBtnLVPPrebootDisable.setOnClickListener(mBtnOnClickListener);
        mBtnGetLVPPrebootThreshold.setOnClickListener(mBtnOnClickListener);
        mBtnSetLVPPrebootThreshold.setOnClickListener(mBtnOnClickListener);

        mBtnLVPPostbootEnable.setOnClickListener(mBtnOnClickListener);
        mBtnLVPPostbootDisable.setOnClickListener(mBtnOnClickListener);
        mBtnGetLVPPostbootThreshold.setOnClickListener(mBtnOnClickListener);
        mBtnSetLVPPostbootThreshold.setOnClickListener(mBtnOnClickListener);

        mBtnResetLVPThreshold.setOnClickListener(mBtnOnClickListener);
        mBtnGetLVPRange.setOnClickListener(mBtnOnClickListener);

        mBtnGetEventDelay.setOnClickListener(mBtnOnClickListener);
        mBtnSetEventDelay.setOnClickListener(mBtnOnClickListener);

        mBtnKeepAliveModeEnable.setOnClickListener(mBtnOnClickListener);
        mBtnKeepAliveModeDisable.setOnClickListener(mBtnOnClickListener);

        mBtnATModeEnable.setOnClickListener(mBtnOnClickListener);
        mBtnATModeDisable.setOnClickListener(mBtnOnClickListener);

        mBtnShutdownMaskStatusIgnEnable.setOnClickListener(mBtnOnClickListener);
        mBtnShutdownMaskStatusIgnDisable.setOnClickListener(mBtnOnClickListener);

        mBtnShutdownMaskStatusPowerbtnEnable.setOnClickListener(mBtnOnClickListener);
        mBtnShutdownMaskStatusPowerbtnDisable.setOnClickListener(mBtnOnClickListener);

        mBtnForceShutdownStatusEnable.setOnClickListener(mBtnOnClickListener);
        mBtnForceShutdownStatusDisable.setOnClickListener(mBtnOnClickListener);

        mBtnGetForceShutdownDelay.setOnClickListener(mBtnOnClickListener);
        mBtnSetForceShutdownDelay.setOnClickListener(mBtnOnClickListener);
    }

    AdapterView.OnItemSelectedListener spnEventDelayTypeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            log("Seleted item " + position);
            mSelectedEventTypeID = (mEventDelayTypeSpinnerItemList.get(position)).value;

            operationGetEventDelay(mSelectedEventTypeID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                //Power Control
                case R.id.btn_power_off:
                    operationDoPowerOff();
                    break;

                //Iginition
                case R.id.btn_ign_wakeup_enable:
                    operationSetIgnitionWakeupStatus(true);
                    break;
                case R.id.btn_ign_wakeup_disable:
                    operationSetIgnitionWakeupStatus(false);
                    break;


                //PowerMode
                case R.id.btn_get_power_mode:
                    operationGetPowerMode();
                    break;
                case R.id.btn_set_power_mode_12v:
                    operationSetPowerMode((byte) MRM_ENUM.IVCP_PM_POWER_MODE.IVCP_POWER_MODE_12V.getValue());
                    break;
                case R.id.btn_set_power_mode_24v:
                    operationSetPowerMode((byte) MRM_ENUM.IVCP_PM_POWER_MODE.IVCP_POWER_MODE_24V.getValue());
                    break;

                //LVP
                case R.id.btn_lvp_preboot_enable:
                    operationSetLVPPrebootStatus(true);
                    break;
                case R.id.btn_lvp_preboot_disable:
                    operationSetLVPPrebootStatus(false);
                    break;


                case R.id.btn_get_lvp_preboot_threshold:
                    operationGetLVPPrebootThreshold();
                    break;
                case R.id.btn_set_lvp_preboot_threshold:
                    operationSetLVPPrebootThreshold();
                    break;


                case R.id.btn_lvp_postboot_enable:
                    operationSetLVPPostbootStatus(true);
                    break;
                case R.id.btn_lvp_postboot_disable:
                    operationSetLVPPostbootStatus(false);
                    break;

                case R.id.btn_get_lvp_postboot_threshold:
                    operationGetLVPPostbootThreshold();
                    break;
                case R.id.btn_set_lvp_postboot_threshold:
                    operationSetLVPPostbootThreshold();
                    break;

                case R.id.btn_reset_lvp_threshold:
                    operationResetLVPThreshold();
                    break;
                case R.id.btn_get_lvp_range:
                    operationGetLVPRange();
                    break;

                //Keep Alive mode
                case R.id.btn_alive_mode_enable:
                    operationSetKeepAliveModeStatus(true);
                    break;
                case R.id.btn_alive_mode_disable:
                    operationSetKeepAliveModeStatus(false);
                    break;

                //AT mode
                case R.id.btn_at_mode_enable:
                    operationSetATModeStatus(true);
                    break;
                case R.id.btn_at_mode_disable:
                    operationSetATModeStatus(false);
                    break;

                //Event Delay
                case R.id.btn_get_event_delay:
                    operationGetEventDelay(mSelectedEventTypeID);
                    break;
                case R.id.btn_set_event_delay:
                    operationSetEventDelay();
                    break;

                //Shutdown mask status - Ignition
                case R.id.btn_shutdown_mask_status_ign_enable:
                    operationSetShutdownMaskStatusIgn(true);
                    break;
                case R.id.btn_shutdown_mask_status_ign_disable:
                    operationSetShutdownMaskStatusIgn(false);
                    break;

                //Shutdown mask status - Power button
                case R.id.btn_shutdown_mask_status_powerbtn_enable:
                    operationSetShutdownMaskStatusPowerbtn(true);
                    break;
                case R.id.btn_shutdown_mask_status_powerbtn_disable:
                    operationSetShutdownMaskStatusPowerbtn(false);
                    break;

                //Force shutdown status
                case R.id.btn_force_shutdown_enable:
                    operationSetForceShutdownStatus(true);
                    break;
                case R.id.btn_force_shutdown_disable:
                    operationSetForceShutdownStatus(false);
                    break;

                //Force shutdown  Delay
                case R.id.btn_get_force_shutdown_delay:
                    operationGetForceShutdownDelay();
                    break;
                case R.id.btn_set_force_shutdown_delay:
                    operationSetForceShutdownDelay();
                    break;

            }
        }
    };



    void updateView(int id) {
        switch( id ) {
            case R.id.txt_power_off_result:
                if(mPowerOffResult.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytPowerOff.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtPowerOffResult, mPowerOffResult);
                break;

            case R.id.txt_ignition_status:
                if(mIgnStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytIgnStatus.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtIgnStatus, mIgnStatus);
                break;

            case R.id.txt_ign_wakeup_status:
                if(mIgnWakeupStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytIgnWakeup.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtIgnWakeupStatus, mIgnWakeupStatus);
                break;

            case R.id.row_power_mode:
                if(mPowerMode.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytPowerMode.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtPowerMode, mPowerMode);
                break;

            case R.id.row_power_status:
                if(mPowerStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytPowerStatus.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtPowerStatus, mPowerStatus);
                break;

            case R.id.txt_power_voltage:
                ViewOperator.setLabelText(mTxtPowerVol, mPowerVol);
                break;

            case R.id.txt_battery_status:
                if(mBattStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytBattStatus.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtBattStatus, mBattStatus);
                break;

            case R.id.txt_battery_voltage:
                ViewOperator.setLabelText(mTxtBattVol, mBattVol);
                break;

            case R.id.txt_battery_avg_current:
                ViewOperator.setLabelText(mTxtBattAvgCur, mBattAvgCur);
                break;

            case R.id.etxt_event_delay_value:
                if(mEtxtEventDelayValue != null)
                    mEtxtEventDelayValue.setText(mEventDelay);
                break;

            case R.id.txt_reset_lvp_threshold_result:
                if(mResetLVPThresholdResult.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytResetLVPThreshold.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtResetLVPThresholdResult, mResetLVPThresholdResult);
                break;

            case R.id.txt_lvp_range_result:
                if(mLVPRangeResult.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytGetLVPRange.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtLVPRangeResult, mLVPRangeResult);
                break;

            case R.id.txt_lvp_preboot_status:
                if(mLVPPrebootStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytLVPPreboot.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtLVPPrebootStatus, mLVPPrebootStatus);
                break;

            case R.id.etxt_lvp_preboot_threshold:
                if(mLVPPrebootThreshold.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytLVPPrebootThreshold.setVisibility(View.GONE);
                } else {
                    if (mEtxtLVPPrebootThreshold != null)
                        mEtxtLVPPrebootThreshold.setText(mLVPPrebootThreshold);
                }
                break;

            case R.id.txt_lvp_postboot_status:
                if(mLVPPostbootStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytLVPPostboot.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtLVPPostbootStatus, mLVPPostbootStatus);
                break;

            case R.id.etxt_lvp_postboot_threshold:
                if(mLVPPostbootThreshold.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytLVPPostbootThreshold.setVisibility(View.GONE);
                } else {
                    if (mEtxtLVPPostbootThreshold != null)
                        mEtxtLVPPostbootThreshold.setText(mLVPPostbootThreshold);
                }
                break;

            case R.id.txt_alive_mode_status:
                if(mKeepAliveModeStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytKeepAliveMode.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtKeepAliveModeStatus, mKeepAliveModeStatus);
                break;

            case R.id.txt_at_mode_status:
                if(mATModeStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytATMode.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtATModeStatus, mATModeStatus);
                break;

            case R.id.txt_last_wakaup_source:
                if(mLastWakeupSource.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytLastWakeupSource.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtLastWakeupSource, mLastWakeupSource);
                break;

            case R.id.txt_shutdown_mask_status_ign:
                if(mShutdownMaskStatusIgn.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytShutdownMaskStatusIgn.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtShutdownMaskStatusIgn, mShutdownMaskStatusIgn);
                break;

            case R.id.txt_shutdown_mask_status_powerbtn:
                if(mShutdownMaskStatusPowerbtn.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytShutdownMaskStatusPowerbtn.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtShutdownMaskStatusPowerbtn, mShutdownMaskStatusPowerbtn);
                break;

            case R.id.txt_force_shutdown_status:
                if(mForceShutdownStatus.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
                    mLytForceShutdownStatus.setVisibility(View.GONE);
                else
                    ViewOperator.setLabelText(mTxtForceShutdownStatus, mForceShutdownStatus);
                break;

            case R.id.etxt_force_shutdown_delay:
                if(mForceShutdownDelay.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytForceShutdownDelay.setVisibility(View.GONE);
                } else {
                    if (mEtxtForceShutdownDelay != null)
                        mEtxtForceShutdownDelay.setText(mForceShutdownDelay);
                }
                break;


            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtIgnStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtIgnWakeupStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtPowerMode, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtPowerStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtPowerVol, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattVol, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtBattAvgCur, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mEtxtEventDelayValue, DEFAULT_DISPLAY_VALUE_0);
        ViewOperator.setLabelText(mTxtLVPPrebootStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtLVPPostbootStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtKeepAliveModeStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtATModeStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtLastWakeupSource, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtShutdownMaskStatusIgn, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtShutdownMaskStatusPowerbtn, DEFAULT_DISPLAY_VALUE_NA);
    }


    void doAsyncTaskRefreshAllFields() {
        mTaskRefreshAllFields = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading Current Status ...");
                mProgressDailog.show();

                mSelectedEventTypeID = ((EventDelayTypeSpinnerItem)mSpnEventDelayType.getSelectedItem()).value;
            }

            @Override
            protected Void doInBackground(Void... params) {
                operationGetIgnitionStatus();
                operationGetAllPowerStatus();
                operationGetAllBatteryStatus();
                operationGetIgnitionWakeupStatus();
                operationGetPowerMode();
                operationGetEventDelay(mSelectedEventTypeID);
                operationGetLVPRange();
                operationGetLVPPrebootStatus();
                operationGetLVPPrebootThreshold();
                operationGetLVPPostbootStatus();
                operationGetLVPPostbootThreshold();
                operationGetKeepAliveModeStatus();
                operationGetATModeStatus();
                operationGetLastWakeupSource();
                operationGetShutdownMaskStatusIgn();
                operationGetShutdownMaskStatusPowerbtn();
                operationGetForceShutdownStatus();
                operationGetForceShutdownDelay();
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
                operationGetIgnitionStatus();
                operationGetAllPowerStatus();
                operationGetAllBatteryStatus();
                operationGetLastWakeupSource();

                String errorStr = ErrorCode.MSG_TAG_ERROR;
                if(     mIgnStatus.contains(errorStr) ||
                        mPowerStatus.contains(errorStr) || mPowerVol.contains(errorStr) ||
                        mBattStatus.contains(errorStr) || mBattVol.contains(errorStr) || mBattAvgCur.contains(errorStr) ||
                        mLastWakeupSource.contains(errorStr)) {
                    if(mIgnStatus.contains(errorStr))
                        log("IgnitionStatus ERROR code : "+ mIgnStatus);
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
                    if(mLastWakeupSource.contains(errorStr))
                        log("LastWakeupSource ERROR code : "+ mLastWakeupSource);
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







    private void operationDoPowerOff() {
        int ret = doPowerOff();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mPowerOffResult = "Power off OK";
        } else {
            mPowerOffResult = "Power off error. "+ ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.txt_power_off_result);
    }


    private void operationGetPowerMode() {
        mPowerMode = getPowerMode();
        mHandler.sendEmptyMessage(R.id.row_power_mode);
    }

    private void operationSetPowerMode(byte modeID) {
        int ret = setPowerMode(modeID);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mPowerMode = getPowerMode();
        else
            mPowerMode = "Set mode error. " + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.row_power_mode);
    }

    private void operationGetLastWakeupSource() {
        mLastWakeupSource = getLastWakupSource();
        mHandler.sendEmptyMessage(R.id.txt_last_wakaup_source);
    }


    private void operationGetAllPowerStatus() {
        mPowerStatus = getPowerStatus();
        mHandler.sendEmptyMessage(R.id.row_power_status);

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
    }


    private void operationGetIgnitionStatus() {
        mIgnStatus = getIgnitionStatus();
        mHandler.sendEmptyMessage(R.id.txt_ignition_status);
    }

    private void operationGetIgnitionWakeupStatus() {
        mIgnWakeupStatus = getIgnitionWakeupStatus();
        mHandler.sendEmptyMessage(R.id.txt_ign_wakeup_status);
    }

    private void operationSetIgnitionWakeupStatus(boolean status) {
        int ret = setIgnWakeupStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mIgnWakeupStatus = getIgnitionWakeupStatus();
        else
            mIgnWakeupStatus = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_ign_wakeup_status);
    }



    private void operationGetATModeStatus() {
        mATModeStatus = getATModeStatus();
        mHandler.sendEmptyMessage(R.id.txt_at_mode_status);
    }

    private void operationSetATModeStatus(boolean status) {

        int ret = setATModeStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mATModeStatus = getATModeStatus();
        else
            mATModeStatus = (status ? "Enable error. " : "Disable error. " ) + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_at_mode_status);

    }

    private void operationGetKeepAliveModeStatus() {
        mKeepAliveModeStatus = getKeepAliveModeStatus();
        mHandler.sendEmptyMessage(R.id.txt_alive_mode_status);
    }

    private void operationSetKeepAliveModeStatus(boolean status) {

        int ret = setKeepAliveModeStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mKeepAliveModeStatus = getKeepAliveModeStatus();
        else
            mKeepAliveModeStatus = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        log("ALIVE mode = " + mKeepAliveModeStatus);
        mHandler.sendEmptyMessage(R.id.txt_alive_mode_status);

    }

    private void operationResetLVPThreshold() {
        int ret = resetLvpThreshold();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mResetLVPThresholdResult = "Reset LVP Threshold OK";
        } else {
            mResetLVPThresholdResult = "Reset LVP Threshold error. "+ ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.txt_reset_lvp_threshold_result);
    }

    private void operationGetLVPRange() {
        mLVPRangeResult = getLvpRange();
        mHandler.sendEmptyMessage(R.id.txt_lvp_range_result);
    }

    private void operationGetLVPPostbootStatus() {
        mLVPPostbootStatus = getLvpPostbootStatus();
        mHandler.sendEmptyMessage(R.id.txt_lvp_postboot_status);
    }

    private void operationSetLVPPostbootStatus(boolean status) {

        int ret = setLvpPostbootStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mLVPPostbootStatus = getLvpPostbootStatus();
        else
            mLVPPostbootStatus = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_lvp_postboot_status);

    }

    private void operationGetLVPPostbootThreshold() {
        mLVPPostbootThreshold = getLvpPostbootThreshold();
        mHandler.sendEmptyMessage(R.id.etxt_lvp_postboot_threshold);
    }

    private void operationSetLVPPostbootThreshold() {
        int ret;
        float postbootThresVal;

        try {
            postbootThresVal = Float.valueOf(mEtxtLVPPostbootThreshold.getText().toString());
            ret = setLvpPostbootThreshold(postbootThresVal);

            if (ret == ErrorCode.MRM_ERR_NO_ERROR){
                Toast.makeText(PowerManagementDemoActivity.this, "Set Success!", Toast.LENGTH_SHORT).show();
            }
            else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mLVPPostbootThreshold = errStr;
                mHandler.sendEmptyMessage(R.id.etxt_lvp_postboot_threshold);
                Toast.makeText(PowerManagementDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(PowerManagementDemoActivity.this, "Wrong input value!", Toast.LENGTH_SHORT).show();
        }
    }


    private void operationGetLVPPrebootStatus() {
        mLVPPrebootStatus = getLvpPrebootStatus();
        mHandler.sendEmptyMessage(R.id.txt_lvp_preboot_status);
    }

    private void operationSetLVPPrebootStatus(boolean status) {
        int ret = setLvpPrebootStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mLVPPrebootStatus = getLvpPrebootStatus();
        else
            mLVPPrebootStatus = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_lvp_preboot_status);
    }


    private void operationGetLVPPrebootThreshold() {
        mLVPPrebootThreshold = getLvpPrebootThreshold();
        mHandler.sendEmptyMessage(R.id.etxt_lvp_preboot_threshold);
    }

    private void operationSetLVPPrebootThreshold() {
        int ret;
        float prebootThresVal;

        try {
            prebootThresVal = Float.valueOf(mEtxtLVPPrebootThreshold.getText().toString());

            ret = setLvpPrebootThreshold(prebootThresVal);

            if (ret == ErrorCode.MRM_ERR_NO_ERROR){
                Toast.makeText(PowerManagementDemoActivity.this, "Set Success!", Toast.LENGTH_SHORT).show();
            }
            else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mLVPPrebootThreshold = errStr;
                mHandler.sendEmptyMessage(R.id.etxt_lvp_preboot_threshold);
                Toast.makeText(PowerManagementDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(PowerManagementDemoActivity.this, "Wrong input value!", Toast.LENGTH_SHORT).show();
        }
    }



    private void operationGetEventDelay(int eventTypeID) {
        mEventDelay = getEventDelay(eventTypeID);
        mHandler.sendEmptyMessage(R.id.etxt_event_delay_value);
    }

    private void operationSetEventDelay() {
        int ret;
        int eventDelayVal;
        try {
            eventDelayVal = Integer.valueOf(mEtxtEventDelayValue.getText().toString());
            ret = setEventDelay(
                    mSelectedEventTypeID,
                    eventDelayVal
            );

            if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(PowerManagementDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

            } else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mEventDelay = errStr;
                mHandler.sendEmptyMessage(R.id.etxt_event_delay_value);
                Toast.makeText(PowerManagementDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(PowerManagementDemoActivity.this, "Wrong input value!", Toast.LENGTH_SHORT).show();
        }
    }



    private void operationSetShutdownMaskStatusIgn(boolean status) {
        int mask = MRM_ENUM.IVCP_PM_SHUTDOWN_MASK.IVCP_SHUTDOWN_MASK_IGNITION.getValue();
        int ret = setShutdownMaskStatus(mask, status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mShutdownMaskStatusIgn = getShutdownMaskStatus(mask);
        else
            mShutdownMaskStatusIgn = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_shutdown_mask_status_ign);

    }

    private void operationGetShutdownMaskStatusIgn() {
        int mask = MRM_ENUM.IVCP_PM_SHUTDOWN_MASK.IVCP_SHUTDOWN_MASK_IGNITION.getValue();
        mShutdownMaskStatusIgn = getShutdownMaskStatus(mask);
        mHandler.sendEmptyMessage(R.id.txt_shutdown_mask_status_ign);
    }



    private void operationSetShutdownMaskStatusPowerbtn(boolean status) {
        int mask = MRM_ENUM.IVCP_PM_SHUTDOWN_MASK.IVCP_SHUTDOWN_MASK_POWER_BUTTON.getValue();
        int ret = setShutdownMaskStatus(mask, status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mShutdownMaskStatusPowerbtn = getShutdownMaskStatus(mask);
        else
            mShutdownMaskStatusPowerbtn = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_shutdown_mask_status_powerbtn);

    }

    private void operationGetShutdownMaskStatusPowerbtn() {
        int mask = MRM_ENUM.IVCP_PM_SHUTDOWN_MASK.IVCP_SHUTDOWN_MASK_POWER_BUTTON.getValue();
        mShutdownMaskStatusPowerbtn = getShutdownMaskStatus(mask);
        mHandler.sendEmptyMessage(R.id.txt_shutdown_mask_status_powerbtn);
    }

    private void operationSetForceShutdownStatus(boolean status) {
        int ret = setForceShutdownMaskStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mForceShutdownStatus = getForceShutdownStatus();
        else
            mForceShutdownStatus = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_force_shutdown_status);
    }

    private void operationGetForceShutdownStatus() {
        mForceShutdownStatus = getForceShutdownStatus();
        mHandler.sendEmptyMessage(R.id.txt_force_shutdown_status);
    }

    private void operationSetForceShutdownDelay() {
        int ret;
        int eventDelayVal;
        try {
            eventDelayVal = Integer.valueOf(mEtxtForceShutdownDelay.getText().toString());
            ret = setForceShutdownDelay(eventDelayVal);

            if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(PowerManagementDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

            } else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mEventDelay = errStr;
                mHandler.sendEmptyMessage(R.id.etxt_force_shutdown_delay);
                Toast.makeText(PowerManagementDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(PowerManagementDemoActivity.this, "Wrong input value!", Toast.LENGTH_SHORT).show();
        }
    }

    private void operationGetForceShutdownDelay() {
        mForceShutdownDelay = getForceShutdownDelay();
        mHandler.sendEmptyMessage(R.id.etxt_force_shutdown_delay);
    }

    String getIgnitionStatus() {
        int ret;
        boolean[] tempIgnStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_ignition_status(tempIgnStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempIgnStat[0] ? "ON" : "OFF" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    String getIgnitionWakeupStatus() {
        int ret;
        boolean[] tempIgnStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_ignition_wakeup_status(tempIgnStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempIgnStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    int setIgnWakeupStatus(boolean enable) {
        int ret;
        if(enable) {
            ret = mIvcpAPI.ivcp_pm_ignition_wakeup_enable();
        } else {
            ret = mIvcpAPI.ivcp_pm_ignition_wakeup_disable();
        }

        return ret;
    }





    int doPowerOff() {
        int ret;
        ret = mIvcpAPI.ivcp_pm_power_off();
        return ret;
    }

    String getPowerMode() {
        String mode;
        int ret;
        byte[] currentCarPowerMode = new byte[1];
        ret = mIvcpAPI.ivcp_pm_get_power_mode(currentCarPowerMode);

        final byte MODE_12V = (byte)MRM_ENUM.IVCP_PM_POWER_MODE.IVCP_POWER_MODE_12V.getValue();
        final byte MODE_24V = (byte)MRM_ENUM.IVCP_PM_POWER_MODE.IVCP_POWER_MODE_24V.getValue();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            if(currentCarPowerMode[0] == MODE_12V) {
                mode = "12V";
            } else if(currentCarPowerMode[0] == MODE_24V) {
                mode = "24V";
            } else {
                mode = "UNKNOWN mode(" + currentCarPowerMode[0] + ")";
            }
            return mode;

        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    int setPowerMode(byte modeID) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_power_mode(modeID);
        return ret;
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

    private String getEventDelay(int eventTypeID ) {
        int ret;
        int[] eventDelay = new int[1];
        ret = mIvcpAPI.ivcp_pm_get_event_delay(eventTypeID, eventDelay);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(eventDelay[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setEventDelay(int eventTypeID, int eventDelayVal) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_event_delay(eventTypeID, eventDelayVal);
        return ret;
    }




    private int resetLvpThreshold()
    {
        int ret;
        ret = mIvcpAPI.ivcp_pm_reset_lvp_threshold();
        return ret;
    }

    private String getLvpRange() {
        int ret;
        float min[]  = new float[1];
        float max[]  = new float[1];
        float def[]  = new float[1];
        ret = mIvcpAPI.ivcp_pm_get_lvp_range(min,max,def);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.format("Min:%.2f , Max:%.2f , Default:%.2f",  min[0], max[0], def[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getLvpPrebootStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_lvp_preboot_get_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    private int setLvpPrebootStatus(boolean enable) {
        int ret;
        if(enable) {
            ret = mIvcpAPI.ivcp_pm_lvp_preboot_enable();
        } else {
            ret = mIvcpAPI.ivcp_pm_lvp_preboot_disable();
        }
        return ret;
    }


    private String getLvpPrebootThreshold() {
        int ret;
        float[] threshold = new float[1];
        ret = mIvcpAPI.ivcp_pm_get_lvp_preboot_threshold(threshold);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(threshold[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setLvpPrebootThreshold(float value) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_lvp_preboot_threshold(value);
        return ret;
    }

    private String getLvpPostbootStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_lvp_postboot_get_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setLvpPostbootStatus(boolean enable) {
        int ret;
        if(enable) {
            ret = mIvcpAPI.ivcp_pm_lvp_postboot_enable();
        } else {
            ret = mIvcpAPI.ivcp_pm_lvp_postboot_disable();
        }
        return ret;
    }

    private String getLvpPostbootThreshold() {
        int ret;
        float[] threshold = new float[1];
        ret = mIvcpAPI.ivcp_pm_get_lvp_postboot_threshold(threshold);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(threshold[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setLvpPostbootThreshold(float value) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_lvp_postboot_threshold(value);
        return ret;
    }



    private String getKeepAliveModeStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_alive_mode(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setKeepAliveModeStatus(boolean enable) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_alive_mode(enable);
        return ret;
    }


    private String getATModeStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_at_mode(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setATModeStatus(boolean enable) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_at_mode(enable);
        return ret;
    }

    String getLastWakupSource() {
        int ret;
        int[] wakeupSrcID = new int[1];
        String wakeupSrcStr;
        ret = mIvcpAPI.ivcp_pm_get_last_wakeup_source(wakeupSrcID);

        if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
            return ErrorCode.errorCodeToString(ret);
        }

        if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_POWER_BUTTON.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_POWER_BUTTON";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_IGNITION.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_IGNITION";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_WWAN.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_WWAN";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        }
        else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_GSENSOR.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_GSENSOR";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        }
        else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_DI1.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_DI1";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        }
        else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_DI2.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_DI2";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        }
        else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_ALARM.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_ALARM";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_HOTKEY.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_HOTKEY";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_DI3.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_DI3";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_DI4.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_DI4";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_KEEP_ALIVE_MODE.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_KEEP_ALIVE_MODE";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_AT_MODE.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_AT_MODE";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else if(wakeupSrcID[0] == MRM_ENUM.IVCP_PM_WAKEUP_TYPE.IVCP_WAKEUP_TYPE_RESET.getValue()) {
            String tempStr = "IVCP_WAKEUP_TYPE_RESET";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        } else{
            String tempStr = "UNKNOWN ? ";
            wakeupSrcStr = String.format("%s( %d )",  tempStr, wakeupSrcID[0] );
        }

        return wakeupSrcStr;
    }

    private int setShutdownMaskStatus(int mask, boolean status) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_shutdown_mask(mask, status);
        return ret;
    }

    private String getShutdownMaskStatus(int mask) {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_shutdown_mask(mask, tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setForceShutdownMaskStatus(boolean status) {
        int ret;
        if(status == true)
            ret = mIvcpAPI.ivcp_pm_force_shutdown_enable();
        else
            ret = mIvcpAPI.ivcp_pm_force_shutdown_disable();
        return ret;
    }

    private String getForceShutdownStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_pm_get_force_shutdown_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    private int setForceShutdownDelay(int delay) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_set_force_shutdown_delay(delay);
        return ret;
    }

    private String getForceShutdownDelay() {
        int ret;
        int[] delay = new int[1];
        ret = mIvcpAPI.ivcp_pm_get_force_shutdown_delay(delay);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(delay[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }



}

