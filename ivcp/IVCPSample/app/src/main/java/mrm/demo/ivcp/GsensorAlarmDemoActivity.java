package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import mrm.client.IVCPServiceClient;
import mrm.define.IVCP.IVCP_GSENSOR_VALUE;
import mrm.define.MRM_CONSTANTS;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;

public class GsensorAlarmDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - GsenA";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), Process.myTid(), logStr));
    }

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    //Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytGsensorAlarmStatus;
    TextView mTxtPeripheralStatus;
    TextView mTxtGsensorAlarmStatus;
    Button mBtnGsensorAlarmEnable, mBtnGsensorAlarmDisable;
    String mtGsensorAlarmStatus;

    LinearLayout mLytGsensorAlarmThreshold;
    EditText mEtxtGsensorAlarmThreshold;
    Button mBtnGetGsensorAlarmThreshold, mBtnSetGsensorAlarmThreshold;
    String mGsensorAlarmThreshold;

    LinearLayout mLytGsensorAlarmMode;
    TextView mTxtGsensorAlarmMode;
    Button mBtnClearGsensorAlarmHistory;
    Button mBtnGsensorAlarmPollingMode, mBtnGsensorAlarmEventMode;
    ListView mLstGsensorAlarmHistory;
    BaseAdapter mLstGsensorAlarmHistoryDataAdapter;
    ArrayList<String> mGsensorAlarmHistory = new ArrayList<String>();
    String mGsensorAlarmMode;


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_gsensorAlarmDataPolling = false;
    Thread mThreadGsensorAlarmDataPolling;

    //Handlers
    ViewUpdateHandler mViewUpdateHandler = null;
    private static class ViewUpdateHandler extends Handler {
        private WeakReference<GsensorAlarmDemoActivity> mActivity = null;

        public ViewUpdateHandler(GsensorAlarmDemoActivity activity) {
            mActivity = new WeakReference<GsensorAlarmDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GsensorAlarmDemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            activity.updateView(msg.what);
        }
    }

    GsensorAlarmEventHandler mGsensorAlarmHandler = null;
    private static class GsensorAlarmEventHandler extends Handler {
        private WeakReference<GsensorAlarmDemoActivity> mActivity = null;

        public GsensorAlarmEventHandler(GsensorAlarmDemoActivity activity) {
            mActivity = new WeakReference<GsensorAlarmDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            GsensorAlarmDemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            if(msg.what == MRM_CONSTANTS.IVCP_EVENT_ID_GSENSOR_ALARM) {
                activity.log("Got G sensor alarm event !");
                int ret;

                //Temporarily stop listening G sensor alarm event while getting alarm data
                activity.mIvcpAPI.ivcp_gsensor_wait_alarm_event(false);

                //Get all buffered alarm data
                while(true) {
                    ret = activity.operationGetGsensorAlarmData();

                    if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                        //Got alarm data successfully keep getting next data.
                        continue;
                    } else if (ret == ErrorCode.MRM_ERR_IVCP_GSENSOR_DATA_NOT_READY) {
                        //No more alarm data. Stop getting.
                        break;
                    } else {
                        //Get error
                        break;
                    }
                }

                //Resume to listen G sensor alarm event
                activity.mIvcpAPI.ivcp_gsensor_wait_alarm_event(true);
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("\n\n=============== onCreate() ===============\n\n");
        super.onCreate(savedInstanceState);

        mIvcpAPI = EntryActivity.mIvcpAPI;

        mViewUpdateHandler = new ViewUpdateHandler(this);
        mGsensorAlarmHandler = new GsensorAlarmEventHandler(this);
        initView();
        initListener();
        doAsyncTaskRefreshAllFields();

        toggleGsensorAlarmEventMode(true);
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
        toggleGsensorAlarmEventMode(false);
        toggleGsensorAlarmPollingMode(false);
        super.onDestroy();
    }

    private void initView() {
        setContentView(R.layout.activity_gsensor_alarm_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytGsensorAlarmStatus = (LinearLayout) findViewById(R.id.row_gsensor_alarm_status);
        mTxtGsensorAlarmStatus = (TextView) findViewById(R.id.txt_gsensor_alarm_status);
        mBtnGsensorAlarmEnable = (Button) findViewById(R.id.btn_gsensor_alarm_enable);
        mBtnGsensorAlarmDisable = (Button) findViewById(R.id.btn_gsensor_alarm_disable);

        mLytGsensorAlarmThreshold  = (LinearLayout) findViewById(R.id.row_gsensor_alarm_threshold);
        mEtxtGsensorAlarmThreshold = (EditText) findViewById(R.id.etxt_gsensor_alarm_threshold);
        mBtnGetGsensorAlarmThreshold = (Button) findViewById(R.id.btn_get_gsensor_alarm_threshold);
        mBtnSetGsensorAlarmThreshold = (Button) findViewById(R.id.btn_set_gsensor_alarm_threshold);

        mLytGsensorAlarmMode = (LinearLayout) findViewById(R.id.row_gsensor_alarm_data);
        mTxtGsensorAlarmMode = (TextView) findViewById(R.id.txt_gsensor_alarm_mode);
        mBtnGsensorAlarmPollingMode = (Button) findViewById(R.id.btn_gsensor_alarm_polling_mode);
        mBtnGsensorAlarmEventMode = (Button) findViewById(R.id.btn_gsensor_alarm_event_mode);
        mBtnClearGsensorAlarmHistory = (Button) findViewById(R.id.btn_clear_gsensor_alarm_history);

        mLstGsensorAlarmHistory = (ListView) findViewById(R.id.lst_gsensor_alarm_history);
        mLstGsensorAlarmHistoryDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mGsensorAlarmHistory.size();
            }

            @Override
            public Object getItem(int position) {
                return mGsensorAlarmHistory.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = GsensorAlarmDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_gsensor_alarm_demo_lst_row_alarm_history, null);
                String record = mGsensorAlarmHistory.get(position);
                ((TextView) listRow.findViewById(R.id.txt_gsensor_alarm_history)).setText(record);
                return listRow;
            }
        };
        mLstGsensorAlarmHistory.setAdapter(mLstGsensorAlarmHistoryDataAdapter);

        cleanAllFields();
    }


    private void initListener() {

        mBtnGsensorAlarmEnable.setOnClickListener(mBtnOnClickListener);
        mBtnGsensorAlarmDisable.setOnClickListener(mBtnOnClickListener);

        mBtnGetGsensorAlarmThreshold.setOnClickListener(mBtnOnClickListener);
        mBtnSetGsensorAlarmThreshold.setOnClickListener(mBtnOnClickListener);

        mBtnGsensorAlarmPollingMode.setOnClickListener(mBtnOnClickListener);
        mBtnGsensorAlarmEventMode.setOnClickListener(mBtnOnClickListener);

        mBtnClearGsensorAlarmHistory.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.btn_gsensor_alarm_enable:
                    operationSetGsensorAlarmStatus(true);
                    break;
                case R.id.btn_gsensor_alarm_disable:
                    operationSetGsensorAlarmStatus(false);
                    break;

                case R.id.btn_get_gsensor_alarm_threshold:
                    operationGetGsensorAlarmThreshold();
                    break;
                case R.id.btn_set_gsensor_alarm_threshold:
                    operationSetGsensorAlarmThreshold();
                    break;

                case R.id.btn_gsensor_alarm_polling_mode:
                    toggleGsensorAlarmEventMode(false);
                    toggleGsensorAlarmPollingMode(true);
                    break;

                case R.id.btn_gsensor_alarm_event_mode:
                    toggleGsensorAlarmPollingMode(false);
                    toggleGsensorAlarmEventMode(true);
                    break;

                case R.id.btn_clear_gsensor_alarm_history:
                    mGsensorAlarmHistory.clear();
                    mLstGsensorAlarmHistoryDataAdapter.notifyDataSetChanged();
                    break;

            }
        }
    };


    void updateView(int id) {
        switch (id) {
            case R.id.row_gsensor_alarm_status:
                if(mtGsensorAlarmStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorAlarmStatus.setVisibility(View.GONE);
                    mLytGsensorAlarmThreshold.setVisibility(View.GONE);
                    mLytGsensorAlarmMode.setVisibility(View.GONE);
                }
                ViewOperator.setLabelText(mTxtGsensorAlarmStatus, mtGsensorAlarmStatus);
                break;

            case R.id.row_gsensor_alarm_threshold:
                if(mGsensorAlarmThreshold.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorAlarmThreshold.setVisibility(View.GONE);
                } else {
                    mEtxtGsensorAlarmThreshold.setText(mGsensorAlarmThreshold);
                }
                break;

            case R.id.row_gsensor_alarm_data:
                if(mGsensorAlarmMode.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytGsensorAlarmMode.setVisibility(View.GONE);
                } else {
                    mLstGsensorAlarmHistoryDataAdapter.notifyDataSetChanged();
                }
                break;

            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0 = "0";

        ViewOperator.setLabelText(mTxtGsensorAlarmMode, DEFAULT_DISPLAY_VALUE_NA);
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
                operationGetGsensorAlarmStatus();
                operationGetGsensorAlarmThreshold();
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
            while (isRunning_gsensorAlarmDataPolling) {
                operationGetGsensorAlarmData();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    void toggleGsensorAlarmPollingMode(boolean enable) {
        if (enable) {
            ViewOperator.setLabelText(mTxtGsensorAlarmMode, "POLLING MODE");
            isRunning_gsensorAlarmDataPolling = enable;
            mThreadGsensorAlarmDataPolling = new Thread(mThreadRunnableGsensorDataPolling);
            mThreadGsensorAlarmDataPolling.start();
            log("--------- Start polling status --------- ");

        } else {
            try {
                isRunning_gsensorAlarmDataPolling = enable;
                if (mThreadGsensorAlarmDataPolling != null) {
                    if (mThreadGsensorAlarmDataPolling.isAlive()) {
                        mThreadGsensorAlarmDataPolling.join();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log("--------- Stop polling status --------- ");
        }
    }

    void toggleGsensorAlarmEventMode(boolean enable) {
        int ret;

        if(enable) {
            ViewOperator.setLabelText(mTxtGsensorAlarmMode, "EVENT MODE");

            //Set G sensor alarm event handler
            ret = mIvcpAPI.ivcp_gsensor_set_alarm_event_handler(mGsensorAlarmHandler);
            if (ret != ErrorCode.MRM_ERR_NO_ERROR &&
                ret != ErrorCode.MRM_ERR_ANDROID_JNI_EVENT_LISTENING_THREAD_ALREADY_RUNNING) {
                mGsensorAlarmMode = "Set EVENT MODE error. " + ErrorCode.errorCodeToString(ret);
                ViewOperator.setLabelText(mTxtGsensorAlarmMode, mGsensorAlarmMode);
            }
        } else {
            //Unset G sensor alarm event handler
            mIvcpAPI.ivcp_gsensor_unset_alarm_event_handler();
        }
    }


    private void operationGetGsensorAlarmStatus() {
        mtGsensorAlarmStatus = getGsensorAlarmStatus();
        mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_status);
    }


    private void operationSetGsensorAlarmStatus(boolean status) {
        int ret;

        ret = setGsensorAlarmStatus(status);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mtGsensorAlarmStatus = status ? "ENABLE" : "DISABLE";

        } else {
            mtGsensorAlarmStatus = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }
        Toast.makeText(GsensorAlarmDemoActivity.this, mtGsensorAlarmStatus, Toast.LENGTH_SHORT).show();
        mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_status);
    }


    private void operationGetGsensorAlarmThreshold() {
        mGsensorAlarmThreshold = getGsensorAlarmThreshold();
        mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_threshold);
    }


    private void operationSetGsensorAlarmThreshold() {
        try {
            int ret = 0;
            int thr = Integer.valueOf(mEtxtGsensorAlarmThreshold.getText().toString());

            ret = setGsensorAlarmThreshold(thr);

            if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(GsensorAlarmDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();
            } else {
                mGsensorAlarmThreshold = "Set error. " + ErrorCode.errorCodeToString(ret);
                mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_threshold);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            mGsensorAlarmThreshold = "Set error. Wrong input.";
            mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_threshold);
        }
    }

    private int operationGetGsensorAlarmData() {
        int ret;
        IVCP_GSENSOR_VALUE temp = new IVCP_GSENSOR_VALUE();

        ret = getGsensorAlarmData(temp);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            //Got alarm data. Update data to UI.
            String formattedAlarmDataStr =
                    String.format(
                            "%-15s,\t %-15s,\t %-15s",
                            String.format("x = %9d", temp.x_mg),
                            String.format("y = %9d", temp.y_mg),
                            String.format("z = %9d", temp.z_mg));
            //formattedAlarmDataStr = String.format("x = %d,\t y = %d,\t z = %d", temp.x_mg, temp.y_mg, temp.z_mg);

            mGsensorAlarmHistory.add(0, String.format(
                    "%s   -   %s",
                    formatter.format(new Date()),
                    formattedAlarmDataStr));

            mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_data);


        } else if (ret == ErrorCode.MRM_ERR_IVCP_GSENSOR_DATA_NOT_READY) {
            //No alarm data. Do nothing
            return ret;

        } else {
            //Got alarm data error. Update data to UI.
            String errStr = ErrorCode.errorCodeToString(ret);
            mGsensorAlarmHistory.add(0, String.format(
                    "%s   -   %s",
                    formatter.format(new Date()),
                    errStr));
            mViewUpdateHandler.sendEmptyMessage(R.id.row_gsensor_alarm_data);
        }

        return ret;
    }




    private String getGsensorAlarmStatus() {
        int ret;
        boolean[] status = new boolean[1];
        ret = mIvcpAPI.ivcp_gsensor_get_alarm(status);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return (status[0]? "ENABLE" : "DISABLE");
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int setGsensorAlarmStatus(boolean status) {
        int ret;
        ret = mIvcpAPI.ivcp_gsensor_set_alarm(status);
        return ret;
    }

    private int setGsensorAlarmThreshold(int thr) {
        int ret;
        ret = mIvcpAPI.ivcp_gsensor_set_alarm_threshold(thr);
        return ret;
    }


    private String getGsensorAlarmThreshold() {
        int ret;
        int[] tempThr = new int[1];
        ret = mIvcpAPI.ivcp_gsensor_get_alarm_threshold(tempThr);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.format("%d", tempThr[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int getGsensorAlarmData(IVCP_GSENSOR_VALUE data) {
        return mIvcpAPI.ivcp_gsensor_get_alarm_data(data);
    }

}