package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

public class PeripheralControlDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - PCtrl"
    ;

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    class SpinnerItem {
        public String displayStr;
        public int    value;

        public SpinnerItem(String str, int val) {
            displayStr = str;
            value = val;
        }
    }

    LinearLayout mLytPeripheralAvailableStatus;
    TextView mTxtPeripheralAvailableStatus;
    Spinner mSpnPeripheralType;
    BaseAdapter mPeripheralTypeSpinnerDataAdapter;
    ArrayList<SpinnerItem> mPeripheralTypeSpinnerItemList;
    String mPeripheralAvailableStatus;
    int mSelectedPeripheralTypeID;

    LinearLayout mLytPeripheralPowerStatus;
    TextView mTxtPeripheralPowerStatus;
    Spinner mSpnPeripheralPowerType;
    BaseAdapter mPeripheralPowerTypeSpinnerDataAdapter;
    ArrayList<SpinnerItem> mPeripheralPowerTypeSpinnerItemList;
    Button mBtnOnPeripheralPowerStatus, mBtnOffPeripheralPowerStatus;
    String mPeripheralPowerStatus;
    int mSelectedPeripheralPowerID;

    LinearLayout mLytWWANWakeupStatus;
    TextView mTxtWWANWakeupStatus;
    Button mBtnWWANWakeupEnable, mBtnWWANWakeupDisable;
    String mWWANWakeupStatus;

    LinearLayout mLytRearviewOperationResult;
    TextView mTxtRearviewOperationResult;
    Spinner mSpnRearview;
    BaseAdapter mRearviewSpinnerDataAdapter;
    ArrayList<SpinnerItem> mRearviewSpinnerItemList;
    Button mBtnGetRearview, mBtnSetRearview;
    String mRearviewOperationResult;
    int mSelectedRearviewID;

    LinearLayout mLytAutoRearviewStatus;
    TextView mTxtAutoRearviewStatus;
    Button mBtnAutoRearviewEnable, mBtnAutoRearviewDisable;
    String mAutoRearviewStatus;

    LinearLayout mLytComportModeOperationResult;
    Spinner mSpnComportMode;
    BaseAdapter mComportModeSpinnerDataAdapter;
    ArrayList<SpinnerItem> mComportModeSpinnerItemList;
    CheckBox mCbComportTerminationStatus;
    Button mBtnGetComportMode, mBtnSetComportMode;
    String mComportModeOperationResult;
    int mSelectedComportModeID;
    boolean mComportTerminationStatus;

    LinearLayout mLytGPSAntennaStatus;
    TextView mTxtGPSAntennaStatus;
    String mGPSAntennaStatus;

    LinearLayout mLytGPSLnaStatus;
    TextView mTxtGPSLnaStatus;
    Button mBtnGPSLnaEnable, mBtnGPSLnaDisable;
    String mGPSLnaStatus;

    LinearLayout mLytCANTerminationStatus;
    TextView mTxtCANTerminationStatus;
    Button mBtnCANTerminationEnable, mBtnCANTerminationDisable;
    String mCANTerminationStatus;

    LinearLayout mLytPeripheralAudio;
    TextView mTxtPeripheralAudio;
    Spinner mSpnPeripheralAudioType;
    BaseAdapter mPeripheralAudioTypeSpinnerDataAdapter;
    ArrayList<SpinnerItem> mPeripheralAudioTypeSpinnerItemList;
    Button mBtnOnPeripheralAudio, mBtnOffPeripheralAudio;
    String mPeripheralAudio;
    int mSelectedPeripheralAudioID;

    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    //public boolean isRunning_statusPolling = false;
    //Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<PeripheralControlDemoActivity> mActivity = null;

        public MyHandler(PeripheralControlDemoActivity activity) {
            mActivity = new WeakReference<PeripheralControlDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PeripheralControlDemoActivity activity = mActivity.get();

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

    }

    @Override
    protected void onPause() {
        log("\n\n=============== onPause() ===============\n\n");
        super.onPause();


    }

    @Override
    protected void onDestroy() {
        log("\n\n=============== onDestroy() ===============\n\n");
        super.onDestroy();
    }

    private void setPeripheralTypeSpinnerItemList() {
        mPeripheralTypeSpinnerItemList = new ArrayList<SpinnerItem>();
        mPeripheralTypeSpinnerItemList.clear();
        mPeripheralTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_WWAN_POWER", MRM_ENUM.IVCP_PERIPHERAL_CONTROL_TYPE.IVCP_PERIPHERAL_WWAN_POWER.getValue()));
        mPeripheralTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_WIFI_POWER"     , MRM_ENUM.IVCP_PERIPHERAL_CONTROL_TYPE.IVCP_PERIPHERAL_WIFI_POWER.getValue()));
        mPeripheralTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_GPS_POWER"      , MRM_ENUM.IVCP_PERIPHERAL_CONTROL_TYPE.IVCP_PERIPHERAL_GPS_POWER.getValue()));
        mPeripheralTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_SIM_SWITCH"     , MRM_ENUM.IVCP_PERIPHERAL_CONTROL_TYPE.IVCP_PERIPHERAL_SIM_SWITCH.getValue()));
        mPeripheralTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_REARVIEW_SWITCH", MRM_ENUM.IVCP_PERIPHERAL_CONTROL_TYPE.IVCP_PERIPHERAL_REARVIEW_SWITCH.getValue()));
    }

    private void setPeripheralPowerTypeSpinnerItemList() {
        mPeripheralPowerTypeSpinnerItemList = new ArrayList<SpinnerItem>();
        mPeripheralPowerTypeSpinnerItemList.clear();
        mPeripheralPowerTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_PID_WWAN"    , MRM_ENUM.IVCP_PERIPHERAL_POWER_ID.IVCP_PERIPHERAL_PID_WWAN.getValue()));
        mPeripheralPowerTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_PID_WIFI"    , MRM_ENUM.IVCP_PERIPHERAL_POWER_ID.IVCP_PERIPHERAL_PID_WIFI.getValue()));
        mPeripheralPowerTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_PID_GPS"     , MRM_ENUM.IVCP_PERIPHERAL_POWER_ID.IVCP_PERIPHERAL_PID_GPS.getValue()));
    }

    private void setRearviewSpinnerItemList() {
        mRearviewSpinnerItemList = new ArrayList<SpinnerItem>();
        mRearviewSpinnerItemList.clear();
        mRearviewSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_RID_MAIN"     , MRM_ENUM.IVCP_PERIPHERAL_REARVIEW_ID.IVCP_PERIPHERAL_RID_MAIN.getValue()));
        mRearviewSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_RID_EXTERNEL1", MRM_ENUM.IVCP_PERIPHERAL_REARVIEW_ID.IVCP_PERIPHERAL_RID_EXTERNEL1.getValue()));
        //mRearviewSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_RID_EXTERNEL2", MRM_ENUM.IVCP_PERIPHERAL_REARVIEW_ID.IVCP_PERIPHERAL_RID_EXTERNEL2.getValue()));
        //mRearviewSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_RID_EXTERNEL3", MRM_ENUM.IVCP_PERIPHERAL_REARVIEW_ID.IVCP_PERIPHERAL_RID_EXTERNEL3.getValue()));
    }

    private void setComportModeSpinnerItemList() {
        mComportModeSpinnerItemList = new ArrayList<SpinnerItem>();
        mComportModeSpinnerItemList.clear();
        mComportModeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_COMPORT_MODE_RS232" , MRM_ENUM.IVCP_PERIPHERAL_COMPORT_MODES.IVCP_PERIPHERAL_COMPORT_MODE_RS232.getValue()));
        mComportModeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_COMPORT_MODE_RS485" , MRM_ENUM.IVCP_PERIPHERAL_COMPORT_MODES.IVCP_PERIPHERAL_COMPORT_MODE_RS485.getValue()));
        mComportModeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_COMPORT_MODE_RS422" , MRM_ENUM.IVCP_PERIPHERAL_COMPORT_MODES.IVCP_PERIPHERAL_COMPORT_MODE_RS422.getValue()));
    }

    private void setPeripheralAudioTypeSpinnerItemList() {
        mPeripheralAudioTypeSpinnerItemList = new ArrayList<SpinnerItem>();
        mPeripheralAudioTypeSpinnerItemList.clear();
        mPeripheralAudioTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_AUDIO_INTERNAL_MIC"        , MRM_ENUM.IVCP_PERIPHERAL_AUDIO.IVCP_PERIPHERAL_AUDIO_INTERNAL_MIC.getValue()));
        mPeripheralAudioTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_AUDIO_MIC_IN"              , MRM_ENUM.IVCP_PERIPHERAL_AUDIO.IVCP_PERIPHERAL_AUDIO_MIC_IN.getValue()));
        mPeripheralAudioTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_AUDIO_LINE_IN"             , MRM_ENUM.IVCP_PERIPHERAL_AUDIO.IVCP_PERIPHERAL_AUDIO_LINE_IN.getValue()));
        mPeripheralAudioTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_AUDIO_INTERNAL_SPEAKER"    , MRM_ENUM.IVCP_PERIPHERAL_AUDIO.IVCP_PERIPHERAL_AUDIO_INTERNAL_SPEAKER.getValue()));
        mPeripheralAudioTypeSpinnerItemList.add(new SpinnerItem("IVCP_PERIPHERAL_AUDIO_LINE_OUT"            , MRM_ENUM.IVCP_PERIPHERAL_AUDIO.IVCP_PERIPHERAL_AUDIO_LINE_OUT.getValue()));
    }

    private void initView() {
        setContentView(R.layout.activity_peripheral_control_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytPeripheralAvailableStatus    = (LinearLayout) findViewById(R.id.row_peripheral_available_status);
        mTxtPeripheralAvailableStatus    = (TextView) findViewById(R.id.txt_peripheral_available_status);
        mSpnPeripheralType               = (Spinner)  findViewById(R.id.spn_peripheral_type);

        mLytPeripheralPowerStatus        = (LinearLayout) findViewById(R.id.row_peripheral_power_status);
        mTxtPeripheralPowerStatus        = (TextView) findViewById(R.id.txt_peripheral_power_status);
        mSpnPeripheralPowerType = (Spinner)  findViewById(R.id.spn_peripheral_power_type);

        mBtnOnPeripheralPowerStatus      = (Button)  findViewById(R.id.btn_on_peripheral_power_status);
        mBtnOffPeripheralPowerStatus     = (Button)  findViewById(R.id.btn_off_peripheral_power_status);

        mLytWWANWakeupStatus             = (LinearLayout) findViewById(R.id.row_wwanwakeup_status);
        mTxtWWANWakeupStatus             = (TextView) findViewById(R.id.txt_wwanwakeup_status);
        mBtnWWANWakeupEnable             = (Button)  findViewById(R.id.btn_wwanwakeup_enable);
        mBtnWWANWakeupDisable            = (Button)  findViewById(R.id.btn_wwanwakeup_disable);

        mLytRearviewOperationResult      = (LinearLayout) findViewById(R.id.row_rearview);
        mTxtRearviewOperationResult      = (TextView) findViewById(R.id.txt_reaview_operation_result);
        mSpnRearview                     = (Spinner)  findViewById(R.id.spn_reaview);

        mBtnGetRearview                  = (Button)  findViewById(R.id.btn_get_reaview);
        mBtnSetRearview                  = (Button)  findViewById(R.id.btn_set_reaview);

        mLytAutoRearviewStatus          = (LinearLayout) findViewById(R.id.row_auto_rearview_status);
        mTxtAutoRearviewStatus          = (TextView) findViewById(R.id.txt_auto_rearview_status);
        mBtnAutoRearviewEnable          = (Button)  findViewById(R.id.btn_auto_rearview_enable);
        mBtnAutoRearviewDisable         = (Button)  findViewById(R.id.btn_auto_rearview_disable);

        mLytComportModeOperationResult  = (LinearLayout) findViewById(R.id.row_comport_mode);
        mSpnComportMode                 = (Spinner)  findViewById(R.id.spn_peripheral_comport_mode);
        mCbComportTerminationStatus     = (CheckBox) findViewById(R.id.cb_peripheral_comport_termination_status);
        mBtnGetComportMode              = (Button)  findViewById(R.id.btn_comport_mode_get);
        mBtnSetComportMode              = (Button)  findViewById(R.id.btn_comport_mode_set);

        mLytGPSAntennaStatus            = (LinearLayout) findViewById(R.id.row_gps_antenna_status);
        mTxtGPSAntennaStatus            = (TextView) findViewById(R.id.txt_gps_antenna_status);

        mLytGPSLnaStatus                = (LinearLayout) findViewById(R.id.row_gps_lna_status);
        mTxtGPSLnaStatus                = (TextView) findViewById(R.id.txt_gps_lna_status);
        mBtnGPSLnaEnable                = (Button)  findViewById(R.id.btn_gps_lna_enable);
        mBtnGPSLnaDisable               = (Button)  findViewById(R.id.btn_gps_lna_disable);

        mLytCANTerminationStatus        = (LinearLayout) findViewById(R.id.row_can_termination_status);
        mTxtCANTerminationStatus        = (TextView) findViewById(R.id.txt_can_termination_status);
        mBtnCANTerminationEnable        = (Button)  findViewById(R.id.btn_can_termination_enable);
        mBtnCANTerminationDisable       = (Button)  findViewById(R.id.btn_can_termination_disable);

        mLytPeripheralAudio             = (LinearLayout) findViewById(R.id.row_peripheral_audio);
        mTxtPeripheralAudio             = (TextView) findViewById(R.id.txt_peripheral_audio);
        mSpnPeripheralAudioType         = (Spinner)  findViewById(R.id.spn_peripheral_audio_type);

        mBtnOnPeripheralAudio           = (Button)  findViewById(R.id.btn_on_peripheral_audio);
        mBtnOffPeripheralAudio          = (Button)  findViewById(R.id.btn_off_peripheral_audio);

        setPeripheralTypeSpinnerItemList();
        mPeripheralTypeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mPeripheralTypeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mPeripheralTypeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mPeripheralTypeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(10.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.LEFT);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnPeripheralType.setAdapter(mPeripheralTypeSpinnerDataAdapter);


        setPeripheralPowerTypeSpinnerItemList();
        mPeripheralPowerTypeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mPeripheralPowerTypeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mPeripheralPowerTypeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mPeripheralPowerTypeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(10.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.LEFT);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnPeripheralPowerType.setAdapter(mPeripheralPowerTypeSpinnerDataAdapter);


        setRearviewSpinnerItemList();
        mRearviewSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mRearviewSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mRearviewSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mRearviewSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(10.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnRearview.setAdapter(mRearviewSpinnerDataAdapter);

        setComportModeSpinnerItemList();
        mComportModeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mComportModeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mComportModeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mComportModeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(10.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnComportMode.setAdapter(mComportModeSpinnerDataAdapter);


        setPeripheralAudioTypeSpinnerItemList();
        mPeripheralAudioTypeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mPeripheralAudioTypeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mPeripheralAudioTypeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mPeripheralAudioTypeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(10.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.LEFT);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnPeripheralAudioType.setAdapter(mPeripheralAudioTypeSpinnerDataAdapter);

        cleanAllFields();
    }


    private void initListener() {
        mSpnPeripheralType.setOnItemSelectedListener(mSpnPeripheralTypeOnItemSelectedListener);

        mSpnPeripheralPowerType.setOnItemSelectedListener(mSpnPeripheraPowerTypeOnItemSelectedListener);
        mBtnOnPeripheralPowerStatus.setOnClickListener(mBtnOnClickListener);
        mBtnOffPeripheralPowerStatus.setOnClickListener(mBtnOnClickListener);

        mBtnWWANWakeupEnable.setOnClickListener(mBtnOnClickListener);
        mBtnWWANWakeupDisable.setOnClickListener(mBtnOnClickListener);

        mSpnRearview.setOnItemSelectedListener(mSpnRearviewOnItemSelectedListener);
        mBtnGetRearview.setOnClickListener(mBtnOnClickListener);
        mBtnSetRearview.setOnClickListener(mBtnOnClickListener);

        mBtnAutoRearviewEnable.setOnClickListener(mBtnOnClickListener);
        mBtnAutoRearviewDisable.setOnClickListener(mBtnOnClickListener);

        mBtnSetComportMode.setOnClickListener(mBtnOnClickListener);
        mBtnGetComportMode.setOnClickListener(mBtnOnClickListener);

        mBtnGPSLnaEnable.setOnClickListener(mBtnOnClickListener);
        mBtnGPSLnaDisable.setOnClickListener(mBtnOnClickListener);

        mBtnCANTerminationEnable.setOnClickListener(mBtnOnClickListener);
        mBtnCANTerminationDisable.setOnClickListener(mBtnOnClickListener);

        mSpnPeripheralAudioType.setOnItemSelectedListener(mSpnPeripheraAudioTypeOnItemSelectedListener);
        mBtnOnPeripheralAudio.setOnClickListener(mBtnOnClickListener);
        mBtnOffPeripheralAudio.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.btn_on_peripheral_power_status:
                    operationSetPeripheralPowerStatus(mSelectedPeripheralPowerID, true);
                    break;
                case R.id.btn_off_peripheral_power_status:
                    operationSetPeripheralPowerStatus(mSelectedPeripheralPowerID, false);
                    break;

                case R.id.btn_wwanwakeup_enable:
                    operationSetWWANWakeupStatus(true);
                    break;
                case R.id.btn_wwanwakeup_disable:
                    operationSetWWANWakeupStatus(false);
                    break;

                case R.id.btn_get_reaview:
                    operationGetRearview();
                    break;
                case R.id.btn_set_reaview:
                    operationSetRearView(mSelectedRearviewID);
                    break;

                case R.id.btn_auto_rearview_enable:
                    operationSetAutoRearviewStatus(true);
                    break;
                case R.id.btn_auto_rearview_disable:
                    operationSetAutoRearviewStatus(false);
                    break;

                case R.id.btn_comport_mode_set:
                    operationSetComportMode();
                    break;
                case R.id.btn_comport_mode_get:
                    operationGetComportMode();
                    break;

                case R.id.btn_gps_lna_enable:
                    operationSetGPSLnaStatus(true);
                    break;
                case R.id.btn_gps_lna_disable:
                    operationSetGPSLnaStatus(false);
                    break;

                case R.id.btn_can_termination_enable:
                    operationSetCANTerminationStatus(true);
                    break;
                case R.id.btn_can_termination_disable:
                    operationSetCANTerminationStatus(false);
                    break;

                case R.id.btn_on_peripheral_audio:
                    operationSetPeripheralAudio(mSelectedPeripheralAudioID, true);
                    break;
                case R.id.btn_off_peripheral_audio:
                    operationSetPeripheralAudio(mSelectedPeripheralAudioID, false);
                    break;
            }
        }
    };


    private AdapterView.OnItemSelectedListener mSpnPeripheralTypeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedPeripheralTypeID = mPeripheralTypeSpinnerItemList.get(position).value;
            log("Seleted item " + position + ", ID = " + mSelectedPeripheralTypeID);
            operationGetPeripheralAvailableStatus(mSelectedPeripheralTypeID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener mSpnPeripheraPowerTypeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            mSelectedPeripheralPowerID = mPeripheralPowerTypeSpinnerItemList.get(position).value;
            log("Seleted item " + position + ", ID = " + mSelectedPeripheralPowerID);
            operationGetPeripheralPowerStatus(mSelectedPeripheralPowerID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    AdapterView.OnItemSelectedListener mSpnRearviewOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedRearviewID = mRearviewSpinnerItemList.get(position).value;
            log("Seleted item " + position + ", ID = " + mSelectedRearviewID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private AdapterView.OnItemSelectedListener mSpnPeripheraAudioTypeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            mSelectedPeripheralAudioID = mPeripheralAudioTypeSpinnerItemList.get(position).value;
            log("Seleted item " + position + ", ID = " + mSelectedPeripheralAudioID);
            operationGetPeripheralAudio(mSelectedPeripheralAudioID);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };


    void updateView(int id) {
        switch( id ) {
            case R.id.txt_peripheral_available_status:
                if(mPeripheralAvailableStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytPeripheralAvailableStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtPeripheralAvailableStatus != null) {
                        ViewOperator.setLabelText(mTxtPeripheralAvailableStatus, mPeripheralAvailableStatus);
                    }
                }
                break;

            case R.id.txt_peripheral_power_status:
                if(mPeripheralPowerStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytPeripheralPowerStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtPeripheralPowerStatus != null) {
                        ViewOperator.setLabelText(mTxtPeripheralPowerStatus, mPeripheralPowerStatus);
                    }
                }
                break;

            case R.id.txt_wwanwakeup_status:
                if(mWWANWakeupStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytWWANWakeupStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtWWANWakeupStatus != null) {
                        ViewOperator.setLabelText(mTxtWWANWakeupStatus, mWWANWakeupStatus);
                    }
                }
                break;

            case R.id.row_rearview:
                if(mRearviewOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytRearviewOperationResult.setVisibility(View.GONE);
                } else if (mRearviewOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                    if (mTxtRearviewOperationResult != null) {
                        mTxtRearviewOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtRearviewOperationResult, mRearviewOperationResult);
                    }
                } else {
                    boolean foundRearviewItem = false;
                    for(int i = 0 ; i < mRearviewSpinnerItemList.size() ; i++) {
                        if( mSelectedRearviewID == mRearviewSpinnerItemList.get(i).value ) {
                            mSpnRearview.setSelection(i);
                            foundRearviewItem = true;
                        }
                    }
                    if( foundRearviewItem ) {
                        mTxtRearviewOperationResult.setVisibility(View.GONE);
                    } else {
                        mRearviewOperationResult = String.format("Get unknown rearview ( %d )", mSelectedRearviewID);
                        mTxtRearviewOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtRearviewOperationResult, mRearviewOperationResult);
                    }
                }
                break;

            case R.id.txt_auto_rearview_status:
                if(mAutoRearviewStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytAutoRearviewStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtAutoRearviewStatus != null) {
                        ViewOperator.setLabelText(mTxtAutoRearviewStatus, mAutoRearviewStatus);
                    }
                }
                break;

            case R.id.row_comport_mode:
                if(mComportModeOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                    Toast.makeText(PeripheralControlDemoActivity.this, mComportModeOperationResult, Toast.LENGTH_SHORT).show();

                } else {
                    mCbComportTerminationStatus.setChecked(mComportTerminationStatus);

                    boolean foundItem = false;
                    for(int i = 0 ; i < mComportModeSpinnerItemList.size() ; i++) {
                        if( mSelectedComportModeID == mComportModeSpinnerItemList.get(i).value ) {
                            mSpnComportMode.setSelection(i);
                            foundItem = true;
                        }
                    }
                    if( !foundItem ) {
                        mComportModeOperationResult = String.format("Get unknown comport mode( %d )", mSelectedComportModeID);
                        Toast.makeText(PeripheralControlDemoActivity.this, mComportModeOperationResult, Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.txt_gps_antenna_status:
                if(mGPSAntennaStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGPSAntennaStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtGPSAntennaStatus != null) {
                        ViewOperator.setLabelText(mTxtGPSAntennaStatus, mGPSAntennaStatus);
                    }
                }
                break;

            case R.id.txt_gps_lna_status:
                if(mGPSLnaStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGPSLnaStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtGPSLnaStatus != null) {
                        ViewOperator.setLabelText(mTxtGPSLnaStatus, mGPSLnaStatus);
                    }
                }
                break;

            case R.id.txt_can_termination_status:
                if(mCANTerminationStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytCANTerminationStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtCANTerminationStatus != null) {
                        ViewOperator.setLabelText(mTxtCANTerminationStatus, mCANTerminationStatus);
                    }
                }
                break;

            case R.id.txt_peripheral_audio:
                if(mPeripheralAudio.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
					if(!android.os.Build.MODEL.substring(0,7).equals("TREK734"))
                        mLytPeripheralAudio.setVisibility(View.GONE);
                } else {
                    if (mTxtPeripheralAudio != null) {
                        ViewOperator.setLabelText(mTxtPeripheralAudio, mPeripheralAudio);
                    }
                }
                break;

            default:
                break;


        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";

        ViewOperator.setLabelText(mTxtPeripheralAvailableStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtPeripheralPowerStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtWWANWakeupStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtAutoRearviewStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtGPSLnaStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtCANTerminationStatus, DEFAULT_DISPLAY_VALUE_NA);
    }


    void doAsyncTaskRefreshAllFields() {
        mTaskRefreshAllFields = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading Current Status ...");
                mProgressDailog.show();

                mSelectedPeripheralTypeID   = ((SpinnerItem)mSpnPeripheralType.getSelectedItem()).value;
                mSelectedPeripheralPowerID  = ((SpinnerItem) mSpnPeripheralPowerType.getSelectedItem()).value;
                mSelectedRearviewID         = ((SpinnerItem) mSpnRearview.getSelectedItem()).value;
                mSelectedPeripheralAudioID  = ((SpinnerItem) mSpnPeripheralAudioType.getSelectedItem()).value;
            }

            @Override
            protected Void doInBackground(Void... params) {
                operationGetPeripheralAvailableStatus(mSelectedPeripheralTypeID);
                operationGetPeripheralPowerStatus(mSelectedPeripheralPowerID);
                operationGetPeripheralAudio(mSelectedPeripheralAudioID);
                operationGetWWANWakeupStatus();
                operationGetRearview();
                operationGetAutoRearViewStatus();
                operationGPSAntennaStatus();
                operationGetGPSLnaStatus();
                operationGetCANTerminationStatus();
                operationGetComportMode();
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
                operationGPSAntennaStatus();

                String errorStr = ErrorCode.MSG_TAG_ERROR;
                if(mGPSAntennaStatus.contains(errorStr))
                    log("GPSAntennaStatus ERROR code : "+ mGPSAntennaStatus);

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


    private void operationGetPeripheralAvailableStatus(int peripheralTypeID) {
        mPeripheralAvailableStatus = getPeripheralAvailableStatus(peripheralTypeID);
        mHandler.sendEmptyMessage(R.id.txt_peripheral_available_status);
    }


    private void operationGetPeripheralPowerStatus(int peripheralPowerID) {
        mPeripheralPowerStatus = getPeripheralPowerStatus(peripheralPowerID);
        mHandler.sendEmptyMessage(R.id.txt_peripheral_power_status);
    }

    private void operationSetPeripheralPowerStatus(int peripheralPowerID, boolean status) {
        int ret = setPeripheralPowerStatus(peripheralPowerID, status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mPeripheralPowerStatus = getPeripheralPowerStatus(peripheralPowerID);
            Toast.makeText(PeripheralControlDemoActivity.this, "Set " + String.format((status ? "ON" : "OFF") + "  OK"), Toast.LENGTH_SHORT).show();

        } else {
            mPeripheralPowerStatus = "Set " + (status ? "ON" : "OFF") + " error.\n" +  ErrorCode.errorCodeToString(ret);
            Toast.makeText(PeripheralControlDemoActivity.this, "Set " + mPeripheralPowerStatus, Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.txt_peripheral_power_status);
    }



    private void operationGetWWANWakeupStatus() {
        mWWANWakeupStatus = getWWANWakeupStatus();
        mHandler.sendEmptyMessage(R.id.txt_wwanwakeup_status);
    }

    private void operationSetWWANWakeupStatus(boolean status) {

        int ret = setWWANWakeupStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mWWANWakeupStatus = getWWANWakeupStatus();
            Toast.makeText(PeripheralControlDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

        } else {
            mWWANWakeupStatus = "Set error.\n" + ErrorCode.errorCodeToString(ret);
            Toast.makeText(PeripheralControlDemoActivity.this, mWWANWakeupStatus, Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.txt_wwanwakeup_status);
    }

    private void operationGetRearview() {
        int ret;
        int[] tempID = new int[1];

        ret = getRearview(tempID);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mRearviewOperationResult = "Get OK";
            for(int i=0; i < mRearviewSpinnerItemList.size() ; i++) {
                if(tempID[0] == mRearviewSpinnerItemList.get(i).value) {
                    mSelectedRearviewID = tempID[0];
                    break;
                }
            }

        } else {
            mRearviewOperationResult = "Get rearview error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mHandler.sendEmptyMessage(R.id.row_rearview);
    }

    private void operationSetRearView(int rearviewID) {
        int ret;

        ret = setRearview(rearviewID);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            for(int i=0; i < mRearviewSpinnerItemList.size() ; i++) {
                if(rearviewID == mRearviewSpinnerItemList.get(i).value) {
                    mSelectedRearviewID = rearviewID;
                    break;
                }
            }

            mRearviewOperationResult = String.format("Set OK. rearview( %d )", rearviewID);

            //If swtichted to rearview successm  switch back to IVCP_PERIPHERAL_RID_MAIN after 5 sec.
            if( rearviewID != MRM_ENUM.IVCP_PERIPHERAL_REARVIEW_ID.IVCP_PERIPHERAL_RID_MAIN.getValue() )
            mHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() { operationSetRearView(MRM_ENUM.IVCP_PERIPHERAL_REARVIEW_ID.IVCP_PERIPHERAL_RID_MAIN.getValue()); }
                },
                5000
            );

        } else {
            mRearviewOperationResult = "Set rearview error.\n" + ErrorCode.errorCodeToString(ret);
        }
        Toast.makeText(PeripheralControlDemoActivity.this, mRearviewOperationResult, Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessage(R.id.row_rearview);
    }



    private void operationGetAutoRearViewStatus() {
        mAutoRearviewStatus = getAutoRearviewStatus();
        mHandler.sendEmptyMessage(R.id.txt_auto_rearview_status);
    }


    private void operationSetAutoRearviewStatus(boolean status) {
        int ret = setAutoRearviewStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mAutoRearviewStatus = getAutoRearviewStatus();
            Toast.makeText(PeripheralControlDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

        } else {
            mAutoRearviewStatus = "Set error.\n" + ErrorCode.errorCodeToString(ret);
            Toast.makeText(PeripheralControlDemoActivity.this, mAutoRearviewStatus, Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.txt_auto_rearview_status);
    }

    private void operationGetComportMode() {
        int ret;
        int[] tempMode = new int[1];
        boolean[] tempStatus = new boolean[1];

        ret = getComportMode(tempMode, tempStatus);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mComportModeOperationResult = "Get OK";
            mComportTerminationStatus = tempStatus[0];
            mSelectedComportModeID = tempMode[0];

        } else {
            mComportModeOperationResult = "Get comport mode error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mHandler.sendEmptyMessage(R.id.row_comport_mode);
    }

    private void operationSetComportMode() {
        int ret;
        int mode = ((SpinnerItem)mSpnComportMode.getSelectedItem()).value;
        boolean status = mCbComportTerminationStatus.isChecked();

        ret = setComportMode(mode, status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mComportTerminationStatus = status;
            mSelectedComportModeID = mode;
            mComportModeOperationResult = String.format("Set OK. mode( %d ), termination status( %b )", mode, status);

        } else {
            mComportModeOperationResult = "Set comport mode error.\n" + ErrorCode.errorCodeToString(ret);
        }
        Toast.makeText(PeripheralControlDemoActivity.this, mComportModeOperationResult, Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessage(R.id.row_comport_mode);
    }

    private void operationGPSAntennaStatus() {
        int ret;
        byte[] tempStatus = new byte[1];
        ret = getGPSAntennaStatus(tempStatus);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mGPSAntennaStatus = String.valueOf(tempStatus[0]);
        } else {
            mGPSAntennaStatus = "Get GPS Antenna error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mHandler.sendEmptyMessage(R.id.txt_gps_antenna_status);
    }

    private void operationGetGPSLnaStatus() {
        mGPSLnaStatus = getGPSLnaStatus();
        mHandler.sendEmptyMessage(R.id.txt_gps_lna_status);
    }

    private void operationSetGPSLnaStatus(boolean status) {
        int ret = setGPSLnaStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mGPSLnaStatus = getGPSLnaStatus();
            Toast.makeText(PeripheralControlDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

        } else {
            mGPSLnaStatus = "Set error.\n" + ErrorCode.errorCodeToString(ret);
            Toast.makeText(PeripheralControlDemoActivity.this, mGPSLnaStatus, Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.txt_gps_lna_status);
    }

    private void operationGetCANTerminationStatus() {
        mCANTerminationStatus = getCANTerminationStatus();
        mHandler.sendEmptyMessage(R.id.txt_can_termination_status);
    }

    private void operationSetCANTerminationStatus(boolean status) {
        int ret = setCANTerminationStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mCANTerminationStatus = getCANTerminationStatus();
            Toast.makeText(PeripheralControlDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

        } else {
            mCANTerminationStatus = "Set error.\n" + ErrorCode.errorCodeToString(ret);
            Toast.makeText(PeripheralControlDemoActivity.this, mCANTerminationStatus, Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.txt_can_termination_status);
    }

    private void operationGetPeripheralAudio(int peripheralAudioID) {
        mPeripheralAudio = getPeripheralAudio(peripheralAudioID);
        mHandler.sendEmptyMessage(R.id.txt_peripheral_audio);
    }

    private void operationSetPeripheralAudio(int peripheralAudioID, boolean status) {
        int ret = setPeripheralAudio(peripheralAudioID, status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mPeripheralAudio = getPeripheralAudio(peripheralAudioID);
            Toast.makeText(PeripheralControlDemoActivity.this, "Set " + String.format((status ? "ON" : "OFF") + "  OK"), Toast.LENGTH_SHORT).show();

        } else {
            mPeripheralAudio = "Set " + (status ? "ON" : "OFF") + " error.\n" +  ErrorCode.errorCodeToString(ret);
            Toast.makeText(PeripheralControlDemoActivity.this, "Set " + mPeripheralAudio, Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.txt_peripheral_audio);
    }

    private String getPeripheralAvailableStatus(int id) {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_control_available(id, tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "AVAILABLE" : "NOT AVAILABLE" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private String getPeripheralPowerStatus(int id) {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_get_power_status(id, tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ON" : "OFF" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private int setPeripheralPowerStatus(int id, boolean status) {
        int ret;
        if(status)
            ret = mIvcpAPI.ivcp_peripheral_power_on(id);
        else
            ret = mIvcpAPI.ivcp_peripheral_power_off(id);

        return ret;
    }


    private String getWWANWakeupStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_get_wwan_wakeup_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private int setWWANWakeupStatus(boolean status) {
        int ret;
        if(status) {
            ret = mIvcpAPI.ivcp_peripheral_wwan_wakeup_enable();
        } else {
            ret = mIvcpAPI.ivcp_peripheral_wwan_wakeup_disable();
        }
        return ret;
    }

    private int setRearview(int id) {
        int ret;
        ret = mIvcpAPI.ivcp_peripheral_set_rearview(id);
        return ret;
    }

    private int getRearview(int[] id) {
        int ret;
        ret = mIvcpAPI.ivcp_peripheral_get_rearview(id);
        return ret;
    }

    private String getAutoRearviewStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_get_auto_rearview_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private int setAutoRearviewStatus(boolean status) {
        int ret;
        if(status) {
            ret = mIvcpAPI.ivcp_peripheral_auto_rearview_enable();
        } else {
            ret = mIvcpAPI.ivcp_peripheral_auto_rearview_disable();
        }
        return ret;
    }

    private int getComportMode(int[] mode, boolean[] status) {
        int ret;
        ret = mIvcpAPI.ivcp_peripheral_get_comport_mode(mode, status);
        return ret;
    }

    private int setComportMode(int mode, boolean status) {
        int ret;
        ret = mIvcpAPI.ivcp_peripheral_set_comport_mode(mode, status);
        return ret;
    }

    private int getGPSAntennaStatus(byte[] status) {
        int ret;
        ret = mIvcpAPI.ivcp_peripheral_get_gps_antenna_status(status);
        return ret;
    }

    private String getGPSLnaStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_get_gps_lna_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private int setGPSLnaStatus(boolean status) {
        int ret;
        if(status) {
            ret = mIvcpAPI.ivcp_peripheral_gps_lna_enable();
        } else {
            ret = mIvcpAPI.ivcp_peripheral_gps_lna_disable();
        }
        return ret;
    }

    private String getCANTerminationStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_get_can_termination_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private int setCANTerminationStatus(boolean status) {
        int ret;
        if(status) {
            ret = mIvcpAPI.ivcp_peripheral_can_termination_enable();
        } else {
            ret = mIvcpAPI.ivcp_peripheral_can_termination_disable();
        }
        return ret;
    }

    private String getPeripheralAudio(int id) {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_peripheral_get_audio(id, tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ON" : "OFF" );
        } else {
            return "Get status error.\n" + ErrorCode.errorCodeToString(ret);
        }
    }

    private int setPeripheralAudio(int id, boolean status) {
        int ret;
        ret = mIvcpAPI.ivcp_peripheral_set_audio(id,status);
        return ret;
    }
}
