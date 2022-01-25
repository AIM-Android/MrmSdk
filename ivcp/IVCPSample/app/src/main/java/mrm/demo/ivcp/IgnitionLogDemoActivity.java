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
import mrm.define.IVCP.IVCP_PM_IGNITION_TIMESTAMP;
import mrm.define.MRM_CONSTANTS;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;

public class IgnitionLogDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - IgnitionLog";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), Process.myTid(), logStr));
    }

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    //Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytInitIgnitionLogResult;
    TextView mTxtInitIgnitionLogResult;
    Button mBtnInitIgnitionLog;
    String mtInitIgnitionLogStatus;

    LinearLayout mLytClearIgnitionLogResult;
    TextView mTxtClearIgnitionLogResult;
    Button mBtnClearIgnitionLog;
    String mClearIgnitionLog;

    LinearLayout mLytIgnitionLogStatus;
    TextView mTxtIgnitionLogStatus;
    Button mBtnClearIgnitionLogHistoryDisplay;
    Button mBtnIgnitionLog;
    String mIgnitionLog;

    ListView mLstIgnitionLogHistory;
    BaseAdapter mLstIgnitionLogHistoryDataAdapter;
    ArrayList<String> mIgnitionLogHistory = new ArrayList<String>();


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    //public boolean isRunning_gsensorAlarmDataPolling = false;
    //Thread mThreadGsensorAlarmDataPolling;

    //Handlers
    ViewUpdateHandler mViewUpdateHandler = null;
    private static class ViewUpdateHandler extends Handler {
        private WeakReference<IgnitionLogDemoActivity> mActivity = null;

        public ViewUpdateHandler(IgnitionLogDemoActivity activity) {
            mActivity = new WeakReference<IgnitionLogDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            IgnitionLogDemoActivity activity = mActivity.get();

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

        mViewUpdateHandler = new ViewUpdateHandler(this);
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

    private void initView() {
        setContentView(R.layout.activity_ignition_log_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytInitIgnitionLogResult = (LinearLayout) findViewById(R.id.row_init_ignition_log);
        mTxtInitIgnitionLogResult = (TextView) findViewById(R.id.txt_init_ignition_log_result);
        mBtnInitIgnitionLog = (Button) findViewById(R.id.btn_init_ignition_log);

        mLytClearIgnitionLogResult = (LinearLayout) findViewById(R.id.row_clean_ignition_log);
        mTxtClearIgnitionLogResult = (TextView) findViewById(R.id.txt_clear_ignition_log_result);
        mBtnClearIgnitionLog = (Button) findViewById(R.id.btn_clear_ignition_log);

        mLytIgnitionLogStatus = (LinearLayout) findViewById(R.id.row_ignition_log_data);
        mTxtIgnitionLogStatus = (TextView) findViewById(R.id.txt_ignition_log_status);
        mBtnIgnitionLog = (Button) findViewById(R.id.btn_ignition_log);
        mBtnClearIgnitionLogHistoryDisplay = (Button) findViewById(R.id.btn_clear_ignition_log_history_display);

        mLstIgnitionLogHistory = (ListView) findViewById(R.id.lst_ignition_log_history);
        mLstIgnitionLogHistoryDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mIgnitionLogHistory.size();
            }

            @Override
            public Object getItem(int position) {
                return mIgnitionLogHistory.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = IgnitionLogDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_ignition_log_demo_lst_row_log_history, null);
                String record = mIgnitionLogHistory.get(position);
                ((TextView) listRow.findViewById(R.id.txt_ignition_log_history)).setText(record);
                return listRow;
            }
        };
        mLstIgnitionLogHistory.setAdapter(mLstIgnitionLogHistoryDataAdapter);

        cleanAllFields();
    }


    private void initListener() {

        mBtnInitIgnitionLog.setOnClickListener(mBtnOnClickListener);

        mBtnIgnitionLog.setOnClickListener(mBtnOnClickListener);
        mBtnClearIgnitionLogHistoryDisplay.setOnClickListener(mBtnOnClickListener);

        mBtnClearIgnitionLog.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.btn_init_ignition_log:
                    operationInitIgnitionLog();
                    break;

                case R.id.btn_ignition_log:
                    operationIgnitionLog();
                    break;

                case R.id.btn_clear_ignition_log_history_display:
                    mIgnitionLogHistory.clear();
                    mLstIgnitionLogHistoryDataAdapter.notifyDataSetChanged();
                    break;

                case R.id.btn_clear_ignition_log:
                    operationClearIgnitionLog();
                    break;
            }
        }
    };


    void updateView(int id) {
        switch (id) {
            case R.id.row_init_ignition_log:
                if(mtInitIgnitionLogStatus.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytInitIgnitionLogResult.setVisibility(View.GONE);
                    mLytIgnitionLogStatus.setVisibility(View.GONE);
                    mLytClearIgnitionLogResult.setVisibility(View.GONE);
                }
                ViewOperator.setLabelText(mTxtInitIgnitionLogResult, mtInitIgnitionLogStatus);
                break;

            case R.id.row_ignition_log_data:
                if(mIgnitionLog.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytIgnitionLogStatus.setVisibility(View.GONE);
                } else {
                    mLstIgnitionLogHistoryDataAdapter.notifyDataSetChanged();
                    ViewOperator.setLabelText(mTxtIgnitionLogStatus, mIgnitionLog);
                }
                break;

            case R.id.row_clean_ignition_log:
                if(mClearIgnitionLog.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytClearIgnitionLogResult.setVisibility(View.GONE);
                } else {
                    ViewOperator.setLabelText(mTxtClearIgnitionLogResult, mClearIgnitionLog);
                }
                break;

            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0 = "0";

        ViewOperator.setLabelText(mTxtIgnitionLogStatus, DEFAULT_DISPLAY_VALUE_NA);
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
                operationInitIgnitionLog();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        }.execute();
    }

    private void operationInitIgnitionLog() {
        int ret;
        Date time = new Date();
        ret = InitIgnitionLog(time);
        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mtInitIgnitionLogStatus = "OK";

        } else {
            mtInitIgnitionLogStatus = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mViewUpdateHandler.sendEmptyMessage(R.id.row_init_ignition_log);
    }


    private int operationIgnitionLog() {
        int ret;
        int size = 50;
        int[] return_number = new int[1];
        IVCP_PM_IGNITION_TIMESTAMP[] log = new IVCP_PM_IGNITION_TIMESTAMP[size];
        for(int i=0;i<size;i++)
            log[i] = new IVCP_PM_IGNITION_TIMESTAMP();

        mIgnitionLogHistory.clear();
        mLstIgnitionLogHistoryDataAdapter.notifyDataSetChanged();

        ret = getIgnitionLog(size, log, return_number);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            //Got Ignition Log data. Update data to UI.
            mIgnitionLog = "OK";

            for(int i =0; i<return_number[0]; i++) {
                mIgnitionLogHistory.add(0, String.format(
                        "%s   -   %s",
                        log[i].timestamp, (log[i].event_type == 1) ? "Ignition ON":"Ignition OFF"));
                mViewUpdateHandler.sendEmptyMessage(R.id.row_ignition_log_data);
            }
        }
        else {
            //Got Ignition Log data error. Update data to UI.
            String errStr = ErrorCode.errorCodeToString(ret);
            mIgnitionLogHistory.add(0, String.format(
                    "%s   -   %s",
                    formatter.format(new Date()),
                    errStr));
            mIgnitionLog = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mViewUpdateHandler.sendEmptyMessage(R.id.row_ignition_log_data);
        Toast.makeText(IgnitionLogDemoActivity.this, "Read Ignition Log finish.", Toast.LENGTH_SHORT).show();
        return ret;
    }

    private void operationClearIgnitionLog() {
        int ret;
        ret = clearIgnitionLog();

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mClearIgnitionLog = "OK";
        } else {
            mClearIgnitionLog = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }
        mViewUpdateHandler.sendEmptyMessage(R.id.row_clean_ignition_log);
    }

    private int InitIgnitionLog(Date timestamp) {
        int ret;
        ret = mIvcpAPI.ivcp_pm_init_ignition_log_time(timestamp);
        return ret;
    }


    private int getIgnitionLog(int size,IVCP_PM_IGNITION_TIMESTAMP[] log, int[] return_number) {
        return mIvcpAPI.ivcp_pm_read_ignition_log(size,log,return_number);
    }

    private int clearIgnitionLog() {
        int ret;
        ret = mIvcpAPI.ivcp_pm_clear_ignition_log();
        return ret;
    }
}