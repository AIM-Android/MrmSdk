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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import mrm.client.IVCPServiceClient;
import mrm.define.IVCP.IVCP_ALARM_REAL_TIME;
import mrm.define.IVCP.IVCP_ALARM_WAKEUP_TIME;
import mrm.define.MRM_ENUM;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;

public class AlarmDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - Alarm";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytRealtimeCurrent;
    TextView mTxtRealtimeCurrent;
    String mRealtimeCurrent = "";

    LinearLayout mLytRealtimeOperationResult;
    TextView mTxtRealtimeOperationResult;
    EditText mEtxtRealtimeYear;
    EditText mEtxtRealtimeMonth;
    EditText mEtxtRealtimeDay;
    EditText mEtxtRealtimeWeekday;
    EditText mEtxtRealtimeHour;
    EditText mEtxtRealtimeMin;
    EditText mEtxtRealtimeSec;
    Button   mBtnGetRealtime, mBtnSetRealtime;
    String mRealtimeOperationResult;
    String mRealtimeYear;
    String mRealtimeMonth;
    String mRealtimeDay;
    String mRealtimeWeekday;
    String mRealtimeHour;
    String mRealtimeMin;
    String mRealtimeSec;

    LinearLayout mLytAlarmwakeupStatus;
    TextView mTxtAlarmwakeupStatus;
    Button mBtnAlarmwakeupEnable, mBtnAlarmwakeupDisable;
    String mAlarmwakeupStatus = "";

    LinearLayout mLytAlarmWakeupModeOperationResult;
    TextView mTxtAlarmWakeupModeOperationResult;
    Spinner mSpnAlarmWakeupMode;
    BaseAdapter mAlarmWakeupModeSpinnerDataAdapter;
    ArrayList<AlarmWakeupModeSpinnerItem> mAlarmWakeupModeSpinnerItemList;
    class AlarmWakeupModeSpinnerItem {
        public String displayStr;
        public int    value;

        public AlarmWakeupModeSpinnerItem(String str, int val) {
            displayStr = str;
            value = val;
        }
    }
    Button mBtnGetAlarmWakeupMode, mBtnSetAlarmWakeupMode;

    int mSelectedAlarmWakeupModeID;
    String mAlarmWakeupModeOperationResult = null;

    LinearLayout mLytAlarmwakeuptimeOperationResult;
    TextView mTxtAlarmwakeuptimeOperationResult;
    EditText mEtxtAlarmwakeuptimeWeekday;
    EditText mEtxtAlarmwakeuptimeHour;
    EditText mEtxtAlarmwakeuptimeMin;
    Button mBtnGetAlarmwakeuptime, mBtnSetAlarmwakeuptime;
    String mAlarmwakeuptimeOperationResult;
    String mAlarmwakeuptimeWeekday;
    String mAlarmwakeuptimeHour;
    String mAlarmwakeuptimeMin;





    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<AlarmDemoActivity> mActivity = null;

        public MyHandler(AlarmDemoActivity activity) {
            mActivity = new WeakReference<AlarmDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AlarmDemoActivity activity = mActivity.get();

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

    private void setAlarmWakeupModeList() {
        mAlarmWakeupModeSpinnerItemList = new ArrayList<AlarmWakeupModeSpinnerItem>();
        mAlarmWakeupModeSpinnerItemList.clear();
        mAlarmWakeupModeSpinnerItemList.add(new AlarmWakeupModeSpinnerItem("IVCP_ALARM_MODE_NO_ALARM", MRM_ENUM.IVCP_ALARM_MODE.IVCP_ALARM_MODE_NO_ALARM.getValue()));
        mAlarmWakeupModeSpinnerItemList.add(new AlarmWakeupModeSpinnerItem("IVCP_ALARM_MODE_HOURLY", MRM_ENUM.IVCP_ALARM_MODE.IVCP_ALARM_MODE_HOURLY.getValue()));
        mAlarmWakeupModeSpinnerItemList.add(new AlarmWakeupModeSpinnerItem("IVCP_ALARM_MODE_DAILY", MRM_ENUM.IVCP_ALARM_MODE.IVCP_ALARM_MODE_DAILY.getValue()));
        mAlarmWakeupModeSpinnerItemList.add(new AlarmWakeupModeSpinnerItem("IVCP_ALARM_MODE_WEEKLY", MRM_ENUM.IVCP_ALARM_MODE.IVCP_ALARM_MODE_WEEKLY.getValue()));
    }


    private void initView() {
        setContentView(R.layout.activity_alarm_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytRealtimeCurrent     = (LinearLayout) findViewById(R.id.row_realtime_current);
        mTxtRealtimeCurrent     = (TextView) findViewById(R.id.txt_realtime_current);
        mLytRealtimeOperationResult = (LinearLayout) findViewById(R.id.row_realtime_setting);
        mTxtRealtimeOperationResult = (TextView) findViewById(R.id.etxt_realtime_operation_result);
        mEtxtRealtimeYear       = (EditText) findViewById(R.id.etxt_realtime_year);
        mEtxtRealtimeMonth      = (EditText) findViewById(R.id.etxt_realtime_month);
        mEtxtRealtimeDay        = (EditText) findViewById(R.id.etxt_realtime_day);
        mEtxtRealtimeWeekday    = (EditText) findViewById(R.id.etxt_realtime_weekday);
        mEtxtRealtimeHour       = (EditText) findViewById(R.id.etxt_realtime_hour);
        mEtxtRealtimeMin        = (EditText) findViewById(R.id.etxt_realtime_min);
        mEtxtRealtimeSec        = (EditText) findViewById(R.id.etxt_realtime_sec);
        mBtnGetRealtime         = (Button)   findViewById(R.id.btn_get_realtime);
        mBtnSetRealtime         = (Button)   findViewById(R.id.btn_set_realtime);

        mLytAlarmwakeupStatus   = (LinearLayout) findViewById(R.id.row_alarmwakeup_status);
        mTxtAlarmwakeupStatus   = (TextView) findViewById(R.id.txt_alarmwakeup_status);
        mBtnAlarmwakeupEnable   = (Button)   findViewById(R.id.btn_alarmwakeup_enable);
        mBtnAlarmwakeupDisable  = (Button)   findViewById(R.id.btn_alarmwakeup_disable);

        mLytAlarmWakeupModeOperationResult = (LinearLayout) findViewById(R.id.row_alarmwakeup_mode);
        mTxtAlarmWakeupModeOperationResult = (TextView) findViewById(R.id.etxt_alarmwakeup_mode_operation_result);
        mSpnAlarmWakeupMode      = (Spinner)  findViewById(R.id.spn_alarmwakeup_mode);
        mBtnGetAlarmWakeupMode   = (Button)   findViewById(R.id.btn_get_alarmwakeup_mode);
        mBtnSetAlarmWakeupMode   = (Button)   findViewById(R.id.btn_set_alarmwakeup_mode);

        setAlarmWakeupModeList();
        mAlarmWakeupModeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mAlarmWakeupModeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mAlarmWakeupModeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                AlarmWakeupModeSpinnerItem currentItem = mAlarmWakeupModeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(15.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnAlarmWakeupMode.setAdapter(mAlarmWakeupModeSpinnerDataAdapter);

        mLytAlarmwakeuptimeOperationResult = (LinearLayout) findViewById(R.id.row_alarmwakeuptime_setting);
        mTxtAlarmwakeuptimeOperationResult = (TextView) findViewById(R.id.etxt_alarmwakeuptime_operation_result);
        mEtxtAlarmwakeuptimeWeekday    = (EditText) findViewById(R.id.etxt_alarmwakeuptime_weekday);
        mEtxtAlarmwakeuptimeHour       = (EditText) findViewById(R.id.etxt_alarmwakeuptime_hour);
        mEtxtAlarmwakeuptimeMin        = (EditText) findViewById(R.id.etxt_alarmwakeuptime_min);
        mBtnGetAlarmwakeuptime         = (Button)   findViewById(R.id.btn_get_alarmwakeup_time);
        mBtnSetAlarmwakeuptime         = (Button)   findViewById(R.id.btn_set_alarmwakeup_time);

        cleanAllFields();
    }


    private void initListener() {
        mBtnGetRealtime.setOnClickListener(mBtnOnClickListener);
        mBtnSetRealtime.setOnClickListener(mBtnOnClickListener);

        mBtnAlarmwakeupEnable.setOnClickListener(mBtnOnClickListener);
        mBtnAlarmwakeupDisable.setOnClickListener(mBtnOnClickListener);

        mBtnGetAlarmWakeupMode.setOnClickListener(mBtnOnClickListener);
        mBtnSetAlarmWakeupMode.setOnClickListener(mBtnOnClickListener);
        mSpnAlarmWakeupMode.setOnItemSelectedListener(spnAlarmWakeupModeOnItemSelectedListener);

        mBtnGetAlarmwakeuptime.setOnClickListener(mBtnOnClickListener);
        mBtnSetAlarmwakeuptime.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
             switch (v.getId()) {
                //Real Time
                case R.id.btn_get_realtime:
                    operationGetRealTime();
                    break;
                case R.id.btn_set_realtime:
                    operationSetRealTime();
                    break;

                //Alarm wakeup status
                case R.id.btn_alarmwakeup_enable:
                    operationSetAlarmWakeupStatus(true);
                    break;
                case R.id.btn_alarmwakeup_disable:
                    operationSetAlarmWakeupStatus(false);
                    break;

                //Alarm wakeup mode
                case R.id.btn_get_alarmwakeup_mode:
                    operationGetAlarmMode();
                    break;
                case R.id.btn_set_alarmwakeup_mode:
                    operationSetAlarmMode(mSelectedAlarmWakeupModeID);
                    break;

                //Alarm wakeup time
                case R.id.btn_get_alarmwakeup_time:
                    operationGetAlarmWakeupTime();
                    break;
                case R.id.btn_set_alarmwakeup_time:
                    operationSetAlarmWakeupTime();
                    break;
                //*/
            }
        }
    };



    AdapterView.OnItemSelectedListener spnAlarmWakeupModeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            log("Seleted item " + position);
            mSelectedAlarmWakeupModeID = mAlarmWakeupModeSpinnerItemList.get(position).value;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    void updateView(int id) {
        switch( id ) {
            case R.id.txt_realtime_current:
                if(mRealtimeCurrent.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytRealtimeCurrent.setVisibility(View.GONE);
                } else {
                    if (mTxtRealtimeCurrent != null)
                        ViewOperator.setLabelText(mTxtRealtimeCurrent, mRealtimeCurrent);
                }
                break;

            case R.id.row_realtime_setting:
                if(mRealtimeOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))
                        && mRealtimeOperationResult.contains("Get error")) {
                    mLytRealtimeOperationResult.setVisibility(View.GONE);
                } else {
                    if (mTxtRealtimeOperationResult != null)
                        ViewOperator.setLabelText(mTxtRealtimeOperationResult, mRealtimeOperationResult);
                    if (!mRealtimeOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                        if (mEtxtRealtimeYear != null)
                            mEtxtRealtimeYear.setText(mRealtimeYear);
                        if (mEtxtRealtimeMonth != null)
                            mEtxtRealtimeMonth.setText(mRealtimeMonth);
                        if (mEtxtRealtimeDay != null)
                            mEtxtRealtimeDay.setText(mRealtimeDay);
                        if (mEtxtRealtimeWeekday != null)
                            mEtxtRealtimeWeekday.setText(mRealtimeWeekday);
                        if (mEtxtRealtimeHour != null)
                            mEtxtRealtimeHour.setText(mRealtimeHour);
                        if (mEtxtRealtimeMin != null)
                            mEtxtRealtimeMin.setText(mRealtimeMin);
                        if (mEtxtRealtimeSec != null)
                            mEtxtRealtimeSec.setText(mRealtimeSec);
                    }
                }
                break;

            case R.id.txt_alarmwakeup_status:
                if(mAlarmwakeupStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytAlarmwakeupStatus.setVisibility(View.GONE);
                } else {
                    if (mTxtAlarmwakeupStatus != null)
                        ViewOperator.setLabelText(mTxtAlarmwakeupStatus, mAlarmwakeupStatus);
                }
                break;

            case R.id.row_alarmwakeup_mode:
                if (mAlarmWakeupModeOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytAlarmWakeupModeOperationResult.setVisibility(View.GONE);
                } else if(mAlarmWakeupModeOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                    mTxtAlarmWakeupModeOperationResult.setVisibility(View.VISIBLE);
                    ViewOperator.setLabelText(mTxtAlarmWakeupModeOperationResult, mAlarmWakeupModeOperationResult);
                } else {
                    boolean foundAlarmModeItem = false;
                    for(int i = 0 ; i < mAlarmWakeupModeSpinnerItemList.size() ; i++) {
                        if( mSelectedAlarmWakeupModeID == mAlarmWakeupModeSpinnerItemList.get(i).value ) {
                            mSpnAlarmWakeupMode.setSelection(i);
                            foundAlarmModeItem = true;
                        }
                    }
                    if( foundAlarmModeItem ) {
                        mTxtAlarmWakeupModeOperationResult.setVisibility(View.GONE);
                    } else {
                        mAlarmWakeupModeOperationResult = String.format("Get unknown alarm mdoe( %d )", mSelectedAlarmWakeupModeID);
                        mTxtAlarmWakeupModeOperationResult.setVisibility(View.VISIBLE);
                        mTxtAlarmWakeupModeOperationResult.setText(mAlarmWakeupModeOperationResult);
                    }
                }
                break;
            case R.id.row_alarmwakeuptime_setting:
                if(mAlarmwakeuptimeOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytAlarmwakeuptimeOperationResult.setVisibility(View.GONE);
                } else {
                    if (mTxtAlarmwakeuptimeOperationResult != null)
                        ViewOperator.setLabelText(mTxtAlarmwakeuptimeOperationResult, mAlarmwakeuptimeOperationResult);
                    if (!mRealtimeOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {

                        if (mEtxtAlarmwakeuptimeWeekday != null)
                            mEtxtAlarmwakeuptimeWeekday.setText(mAlarmwakeuptimeWeekday);
                        if (mEtxtAlarmwakeuptimeHour != null)
                            mEtxtAlarmwakeuptimeHour.setText(mAlarmwakeuptimeHour);
                        if (mEtxtAlarmwakeuptimeMin != null)
                            mEtxtAlarmwakeuptimeMin.setText(mAlarmwakeuptimeMin);

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

        ViewOperator.setLabelText(mTxtRealtimeCurrent, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtRealtimeOperationResult, "-");
        ViewOperator.setLabelText(mTxtAlarmwakeupStatus, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtAlarmwakeuptimeOperationResult, "-");
    }


    void doAsyncTaskRefreshAllFields() {
        mTaskRefreshAllFields = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading Current Status ...");
                mProgressDailog.show();

                mSelectedAlarmWakeupModeID = ((AlarmWakeupModeSpinnerItem)mSpnAlarmWakeupMode.getSelectedItem()).value;
            }

            @Override
            protected Void doInBackground(Void... params) {
                operationRefreshRealtimeDisplay();
                operationGetRealTime();
                operationGetAlarmWakeupStatus();
                operationGetAlarmMode();
                operationGetAlarmWakeupTime();
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

                operationRefreshRealtimeDisplay();

                String errorStr = ErrorCode.MSG_TAG_ERROR;
                if( mRealtimeCurrent.contains(errorStr) ) {
                    log("Realtime ERROR code : "+ mRealtimeCurrent);
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







    void operationRefreshRealtimeDisplay() {
        int ret;
        IVCP_ALARM_REAL_TIME realTime = new IVCP_ALARM_REAL_TIME();

        ret = getRealTime(realTime);

        if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
            mRealtimeCurrent = ErrorCode.errorCodeToString(ret);
        } else {

            mRealtimeCurrent = String.format("%04d / %02d / %02d ( %01d )  %02d : %02d : %02d",
                    realTime.year, realTime.month, realTime.day, realTime.week, realTime.hour, realTime.minute, realTime.second);
        }

        mHandler.sendEmptyMessage(R.id.txt_realtime_current);
    }

    void operationGetRealTime() {
        int ret;
        IVCP_ALARM_REAL_TIME realTime = new IVCP_ALARM_REAL_TIME();

        ret = getRealTime(realTime);

        if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
            mRealtimeOperationResult = "Get error. " + ErrorCode.errorCodeToString(ret);
            mRealtimeYear    = "";
            mRealtimeMonth   = "";
            mRealtimeDay     = "";
            mRealtimeWeekday = "";
            mRealtimeHour    = "";
            mRealtimeMin     = "";
            mRealtimeSec     = "";
        } else {
            mRealtimeOperationResult = "Get OK";
            mRealtimeYear    = String.format("%04d", realTime.year);
            mRealtimeMonth   = String.format("%02d", realTime.month);
            mRealtimeDay     = String.format("%02d", realTime.day);
            mRealtimeWeekday = String.format("%01d", realTime.week);
            mRealtimeHour    = String.format("%02d", realTime.hour);
            mRealtimeMin     = String.format("%02d", realTime.minute);
            mRealtimeSec     = String.format("%02d", realTime.second);
        }

        mHandler.sendEmptyMessage(R.id.row_realtime_setting);
    }

    private void operationSetRealTime() {
        int ret;
        IVCP_ALARM_REAL_TIME realTime = new IVCP_ALARM_REAL_TIME();

        try {
            realTime.year    = Integer.valueOf( mEtxtRealtimeYear.getText().toString()    );
            realTime.month   = Byte.valueOf(    mEtxtRealtimeMonth.getText().toString()   );
            realTime.day     = Byte.valueOf(    mEtxtRealtimeDay.getText().toString()     );
            realTime.week    = Byte.valueOf(    mEtxtRealtimeWeekday.getText().toString() );
            realTime.hour    = Byte.valueOf(    mEtxtRealtimeHour.getText().toString()    );
            realTime.minute  = Byte.valueOf(    mEtxtRealtimeMin.getText().toString()     );
            realTime.second = Byte.valueOf(mEtxtRealtimeSec.getText().toString()     );
            mRealtimeYear    = String.format( "%04d", realTime.year);
            mRealtimeMonth   = String.format( "%02d", realTime.month);
            mRealtimeDay     = String.format( "%02d", realTime.day);
            mRealtimeWeekday = String.format( "%01d", realTime.week);
            mRealtimeHour    = String.format( "%02d", realTime.hour);
            mRealtimeMin     = String.format( "%02d", realTime.minute);
            mRealtimeSec     = String.format( "%02d", realTime.second);

            ret = setRealTime(realTime);

            if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                mRealtimeOperationResult = "Set error. " + ErrorCode.errorCodeToString(ret);
            } else {
                mRealtimeOperationResult = "Set OK";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            mRealtimeOperationResult = "Set error. Wrong input.";

        }
        finally {
            Toast.makeText(AlarmDemoActivity.this, mRealtimeOperationResult, Toast.LENGTH_SHORT ).show();
            mHandler.sendEmptyMessage(R.id.row_realtime_setting);
        }
    }


    private void operationGetAlarmWakeupStatus() {
        mAlarmwakeupStatus = getAlarmWakeupStatus();
        mHandler.sendEmptyMessage(R.id.txt_alarmwakeup_status);
    }

    private void operationSetAlarmWakeupStatus(boolean status) {
        int ret = setAlarmWakeupStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR)
            mAlarmwakeupStatus = getAlarmWakeupStatus();
        else
            mAlarmwakeupStatus = (status ? "Enable error. " : "Disable error. ") + ErrorCode.errorCodeToString(ret);

        mHandler.sendEmptyMessage(R.id.txt_alarmwakeup_status);
    }



    private void operationGetAlarmMode() {
        int ret;
        int currentAlarmMode[] = new int[1];

        ret = getAlarmWakeupMode(currentAlarmMode);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mAlarmWakeupModeOperationResult = "Get OK";
            mSelectedAlarmWakeupModeID = currentAlarmMode[0];
        } else {
            mAlarmWakeupModeOperationResult = "Get error. " + ErrorCode.errorCodeToString(ret);
        }
        mHandler.sendEmptyMessage(R.id.row_alarmwakeup_mode);
    }

    private void operationSetAlarmMode(int modeID) {
        int ret;
        ret = setAlarmWakeupMode(modeID);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mAlarmWakeupModeOperationResult = "Set OK";
            mSelectedAlarmWakeupModeID = modeID;
        } else {
            mAlarmWakeupModeOperationResult = "Set error. " + ErrorCode.errorCodeToString(ret);
        }
        Toast.makeText(AlarmDemoActivity.this, mAlarmWakeupModeOperationResult, Toast.LENGTH_SHORT ).show();
        mHandler.sendEmptyMessage(R.id.row_alarmwakeup_mode);
    }




    private void operationGetAlarmWakeupTime() {
        int ret;
        IVCP_ALARM_WAKEUP_TIME wakeupTime = new IVCP_ALARM_WAKEUP_TIME();

        ret = getAlarmWakeupTime(wakeupTime);

        if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
            mAlarmwakeuptimeOperationResult = ErrorCode.errorCodeToString(ret);
            mAlarmwakeuptimeWeekday = "";
            mAlarmwakeuptimeHour    = "";
            mAlarmwakeuptimeMin     = "";
        } else {
            mAlarmwakeuptimeOperationResult = "Get OK";
            mAlarmwakeuptimeWeekday = String.format("%01d", wakeupTime.day_of_week);
            mAlarmwakeuptimeHour    = String.format("%02d", wakeupTime.hour);
            mAlarmwakeuptimeMin     = String.format("%02d", wakeupTime.minute);

        }
        mHandler.sendEmptyMessage(R.id.row_alarmwakeuptime_setting);
    }

    private void operationSetAlarmWakeupTime() {

        int ret;
        IVCP_ALARM_WAKEUP_TIME wakeupTime = new IVCP_ALARM_WAKEUP_TIME();

        try {

            wakeupTime.day_of_week  = Byte.valueOf(mEtxtAlarmwakeuptimeWeekday.getText().toString());
            wakeupTime.hour         = Byte.valueOf(mEtxtAlarmwakeuptimeHour.getText().toString());
            wakeupTime.minute       = Byte.valueOf(mEtxtAlarmwakeuptimeMin.getText().toString());

            mAlarmwakeuptimeWeekday = String.format( "%01d", wakeupTime.day_of_week);
            mAlarmwakeuptimeHour    = String.format( "%02d", wakeupTime.hour);
            mAlarmwakeuptimeMin     = String.format( "%02d", wakeupTime.minute);


            ret = setAlarmWakeupTime(wakeupTime);

            if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
                mAlarmwakeuptimeOperationResult = "Set error. " + ErrorCode.errorCodeToString(ret);
            } else {
                mAlarmwakeuptimeOperationResult = "Set OK";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            mAlarmwakeuptimeOperationResult = "Set error. Wrong input.";

        }
        finally {
            Toast.makeText(AlarmDemoActivity.this, mAlarmwakeuptimeOperationResult, Toast.LENGTH_SHORT ).show();
            mHandler.sendEmptyMessage(R.id.row_alarmwakeuptime_setting);
        }
    }








    private int getRealTime(IVCP_ALARM_REAL_TIME realTime) {
        return mIvcpAPI.ivcp_alarm_get_real_time(realTime);
    }

    private int setRealTime(IVCP_ALARM_REAL_TIME realTime) {
        return mIvcpAPI.ivcp_alarm_set_real_time(realTime);
    }

    private String getAlarmWakeupStatus() {
        int ret;
        boolean[] tempStat = new boolean[1];
        ret = mIvcpAPI.ivcp_alarm_get_wakeup_status(tempStat);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (tempStat[0] ? "ENABLE" : "DISABLE" );
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setAlarmWakeupStatus(boolean enable) {
        int ret;

        if(enable)
            ret = mIvcpAPI.ivcp_alarm_wakeup_enable();
        else
            ret = mIvcpAPI.ivcp_alarm_wakeup_disable();

        return ret;
    }


    private int getAlarmWakeupMode(int[] mode) {
        return mIvcpAPI.ivcp_alarm_get_wakeup_mode(mode);
    }

    private int setAlarmWakeupMode(int mode) {
        return mIvcpAPI.ivcp_alarm_set_wakeup_mode(mode);
    }


    private int getAlarmWakeupTime(IVCP_ALARM_WAKEUP_TIME wakeupTime) {
        return mIvcpAPI.ivcp_alarm_get_wakeup_time(wakeupTime);
    }

    private int setAlarmWakeupTime(IVCP_ALARM_WAKEUP_TIME wakeupTime) {
        return mIvcpAPI.ivcp_alarm_set_wakeup_time(wakeupTime);
    }
}
