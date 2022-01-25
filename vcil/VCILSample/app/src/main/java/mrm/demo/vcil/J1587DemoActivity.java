package mrm.demo.vcil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import mrm.VCIL;
import mrm.define.MRM_CONSTANTS;
import mrm.define.VCIL.VCIL_J1587_MESSAGE;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.HexConverter;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;
import mrm.demo.util.ViewOperator;


public class J1587DemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - J1587";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnJ1587SendSettingJ1587Port;
    SpinnerCustomAdapter mJ1587SendSettingJ1587PortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1587SendSettingJ1587PortSpinnerItemList;
    byte mSelectedJ1587SendSettingJ1587PortID;

    EditText mEtxtJ1587SendSettingMID;
    String mJ1587SendSettingMID = "";

    EditText mEtxtJ1587SendSettingPID;
    String mJ1587SendSettingPID = "";

    EditText mEtxtJ1587SendSettingPriority;
    String mJ1587SendSettingPriority = "";

    EditText mEtxtJ1587SendSettingMsgData;
    String mJ1587SendSettingMsgData = "";

    EditText mEtxtJ1587SendSettingMsgLength;

    Button mBtnJ1587Send;

    Button mBtnFilter;
    Button mBtnClearList;
    Button mBtnRefreshList;
    TextView mTotalRecvMsgCount;
    ListView mLstRecvMsgMsgStaticList;
    BaseAdapter mLstRecvMsgMsgStaticListDataAdapter;
    static ArrayList<RecvMsgStatisticRecord> mRecvMsgStatisticRecordList = new ArrayList<RecvMsgStatisticRecord>();
    static LinkedBlockingQueue<RecvMsgRecord> mRecvMsgQueue = new LinkedBlockingQueue<RecvMsgRecord>();

    TextView mTxtCurrentReceiveMode;
    Button mBtnTogglePollingMode;
    Button mBtnToggleEventMode;
    CheckBox mCbDoStatistic;

    long mRecvMsgCount = 0;

    static final Object SYNC_RECV_MSG_QUEUE = new Object();
    static final Object SYNC_RECV_MSG_STATISTIC_LIST = new Object();

    public static class RecvMsgRecord {
        long receivedTime;
        VCIL_J1587_MESSAGE receivedMsg = new VCIL_J1587_MESSAGE();

        public RecvMsgRecord(long time, VCIL_J1587_MESSAGE msgObj) {
            receivedTime = time;
            receivedMsg.copyFrom(msgObj);
        }
    }

    public static class RecvMsgStatisticRecord {
        long lastReceivedTime;
        long count;
        VCIL_J1587_MESSAGE lastReceivedMsg = new VCIL_J1587_MESSAGE();

        public RecvMsgStatisticRecord() {
            lastReceivedTime = 0;
            count = 0;
        }

        public void updateAndAddCount(RecvMsgRecord record) {
            this.lastReceivedTime = record.receivedTime;
            lastReceivedMsg.copyFrom(record.receivedMsg);
            this.count++;
        }
    }


    //Threads
    public boolean isRunningMsgPolling = false;
    Thread mThreadMsgPolling;

    public boolean isRunningMsgStatistic = false;
    Thread mThreadMsgStatistic;

    public boolean isRunningRecvMsgStaticListUpdate = false;
    Thread mThreadRecvMsgListStatisticUpdate;


    //Handlers
    MyViewRefreshHandler mViewRefreshHandler = null;

    private static class MyViewRefreshHandler extends Handler {
        private WeakReference<J1587DemoActivity> mActivity = null;

        public MyViewRefreshHandler(J1587DemoActivity activity) {
            mActivity = new WeakReference<J1587DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            J1587DemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            switch (msg.what) {
                case R.id.lst_recv_msg_statistc_list:
                    if (activity.isRunningMsgStatistic)
                        activity.mLstRecvMsgMsgStaticListDataAdapter.notifyDataSetChanged();

                    ViewOperator.setLabelText(activity.mTotalRecvMsgCount, String.valueOf(activity.mRecvMsgCount));
                    //activity.log("Receive count = " + activity.mRecvMsgCount);
                    //activity.log("mRecvMsgQueue size = " + activity.mRecvMsgQueue.size());
                    break;

                default:
                    break;
            }
        }
    }


    HandlerThread mEventHandlerThread = null;
    MyEventHandler mEventHandler = null;

    private static class MyEventHandler extends Handler {
        private WeakReference<J1587DemoActivity> mActivity = null;

        public MyEventHandler(J1587DemoActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<J1587DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            J1587DemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            int eventID = msg.what;
            switch (eventID) {
                case MRM_CONSTANTS.VCIL_EVENT_ID_RECEIVED_MSG_J1587:
                    activity.mVcilAPI.vcil_j1587_wait_event(false);
                    while (true) {
                        int readRet;
                        /* Read single message every time
                        readRet = operationReadJ1587Msg(activity);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadJ1587MsgMulti(activity);
                        //*/

                        if (readRet == ErrorCode.MRM_ERR_NO_ERROR) {
                            //If message is read correctly
                            //then keep reading

                        } else if (readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
                            //If currently no received message,
                            //then start waiting for next event
                            break;
                        } else {
                            String errMsg = String.format("Read ERROR(%X).", readRet);
                            activity.log(errMsg);
                            Toast.makeText(activity.getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    activity.mVcilAPI.vcil_j1587_wait_event(true);
                    break;

                default:
                    break;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVcilAPI = EntryActivity.mVcilAPI;
        this.initView();
        this.initListener();
        mViewRefreshHandler = new MyViewRefreshHandler(this);

        mEventHandlerThread = new HandlerThread("J1587EventHandlerThread");
        mEventHandlerThread.start();
        mEventHandler = new MyEventHandler(this, mEventHandlerThread.getLooper());

        toggleEventMode(true); //Start EVENT MODE - Start reading message obj after the handler received event from SDK
        //togglePollingMode(true); //Start POLLING MODE - Keep reading message obj no matter SDK has received message from bus or not
    }


    @Override
    protected void onResume() {
        log("\n\n=============== onResume() ===============\n\n");
        super.onResume();

        toggleMsgStatistic(true); //Start doing statistic for received message
        toggleRefreshRecvMsgStatisticList(true); //Start updating statistic result to UI
    }

    @Override
    protected void onPause() {
        log("\n\n=============== onPause() ===============\n\n");
        toggleRefreshRecvMsgStatisticList(false);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        log("\n\n=============== onDestroy() ===============\n\n");
        toggleEventMode(false);
        togglePollingMode(false);

        toggleMsgStatistic(false);
        toggleRefreshRecvMsgStatisticList(false);

        mEventHandlerThread.quit();

        mRecvMsgQueue.clear();
        mRecvMsgCount = 0;
        mRecvMsgStatisticRecordList.clear();

        super.onDestroy();
    }


    private void initView() {
        setContentView(R.layout.activity_j1587_demo);

        mProgressDailog = new ProgressDialog(this);

        mSpnJ1587SendSettingJ1587Port = (Spinner) findViewById(R.id.spn_j1587_send_port);
        mEtxtJ1587SendSettingMID = (EditText) findViewById(R.id.etxt_j1587_send_mid);
        mEtxtJ1587SendSettingPID = (EditText) findViewById(R.id.etxt_j1587_send_pid);
        mEtxtJ1587SendSettingPriority = (EditText) findViewById(R.id.etxt_j1587_send_priority);
        mEtxtJ1587SendSettingMsgData = (EditText) findViewById(R.id.etxt_j1587_send_data);
        mEtxtJ1587SendSettingMsgLength = (EditText) findViewById(R.id.etxt_j1587_send_length);
        mBtnJ1587Send = (Button) findViewById(R.id.btn_j1587_send);

        mJ1587SendSettingJ1587PortSpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1587SendSettingJ1587PortSpinnerItemList.clear();
        mJ1587SendSettingJ1587PortSpinnerItemList.add(new SpinnerItem("0", 0));
        mJ1587SendSettingJ1587PortSpinnerItemList.add(new SpinnerItem("1", 1));

        mJ1587SendSettingJ1587PortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1587SendSettingJ1587PortSpinnerItemList);
        mSpnJ1587SendSettingJ1587Port.setAdapter(mJ1587SendSettingJ1587PortSpinnerDataAdapter);
        mSpnJ1587SendSettingJ1587Port.setSelection(0);

        mCbDoStatistic = (CheckBox) findViewById(R.id.cb_do_statistic);
        mBtnTogglePollingMode = (Button) findViewById(R.id.btn_toggle_polling_mode);
        mBtnToggleEventMode = (Button) findViewById(R.id.btn_toggle_event_mode);
        mTxtCurrentReceiveMode = (TextView) findViewById(R.id.txt_current_receive_mode);

        mBtnRefreshList = (Button) findViewById(R.id.btn_refresh_list);
        mBtnClearList = (Button) findViewById(R.id.btn_clear_list);
        mBtnFilter = (Button) findViewById(R.id.btn_filter);

        mTotalRecvMsgCount = (TextView) findViewById(R.id.txt_total_recv_msg_count);
        mLstRecvMsgMsgStaticList = (ListView) findViewById(R.id.lst_recv_msg_statistc_list);
        mLstRecvMsgMsgStaticListDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mRecvMsgStatisticRecordList.size();
            }

            @Override
            public Object getItem(int position) {
                return mRecvMsgStatisticRecordList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = J1587DemoActivity.this.getLayoutInflater().inflate(R.layout.activity_j1587_demo_lst_row_j1587_received_msg, null);

                RecvMsgStatisticRecord record = mRecvMsgStatisticRecordList.get(position);

                ((TextView) listRow.findViewById(R.id.txt_j1587_received_msg_time)).setText(DateFormat.format("HH:mm:ss", record.lastReceivedTime));
                ((TextView) listRow.findViewById(R.id.txt_j1587_received_msg_mid)).setText(String.format("%02X", record.lastReceivedMsg.mid));
                ((TextView) listRow.findViewById(R.id.txt_j1587_received_msg_pid)).setText(String.format("%04X", record.lastReceivedMsg.pid));
                ((TextView) listRow.findViewById(R.id.txt_j1587_received_msg_data)).setText(HexConverter.byteArrayToHexString(record.lastReceivedMsg.data, record.lastReceivedMsg.length));
                ((TextView) listRow.findViewById(R.id.txt_j1587_received_msg_data_len)).setText(String.valueOf(record.lastReceivedMsg.length));
                ((TextView) listRow.findViewById(R.id.txt_j1587_received_msg_count)).setText(String.valueOf(record.count));


                return listRow;
            }
        };
        mLstRecvMsgMsgStaticList.setAdapter(mLstRecvMsgMsgStaticListDataAdapter);
    }


    private void initListener() {
        mBtnJ1587Send.setOnClickListener(mBtnOnClickListener);
        mCbDoStatistic.setOnCheckedChangeListener(mCbOnCheckedChangeListener);
        mBtnTogglePollingMode.setOnClickListener(mBtnOnClickListener);
        mBtnToggleEventMode.setOnClickListener(mBtnOnClickListener);
        mBtnRefreshList.setOnClickListener(mBtnOnClickListener);
        mBtnClearList.setOnClickListener(mBtnOnClickListener);
        mBtnFilter.setOnClickListener(mBtnOnClickListener);
    }


    CheckBox.OnCheckedChangeListener mCbOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.cb_do_statistic:
                    toggleMsgStatistic(mCbDoStatistic.isChecked());
                    break;
            }
        }
    };


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_j1587_send:
                    operationSendJ1587Msg();
                    break;
                case R.id.btn_filter:
                    Intent in = new Intent();
                    in.setClass(J1587DemoActivity.this, J1587FilterDemoActivity.class);
                    startActivity(in);
                    break;
                case R.id.btn_refresh_list:
                    mViewRefreshHandler.sendEmptyMessage(R.id.lst_recv_msg_statistc_list);
                    break;
                case R.id.btn_clear_list:
                    mRecvMsgCount = 0;
                    mRecvMsgQueue.clear();
                    mRecvMsgStatisticRecordList.clear();
                    mViewRefreshHandler.sendEmptyMessage(R.id.lst_recv_msg_statistc_list);
                    break;

                case R.id.btn_toggle_event_mode:
                    togglePollingMode(false);
                    toggleEventMode(true);
                    break;

                case R.id.btn_toggle_polling_mode:
                    toggleEventMode(false);
                    togglePollingMode(true);
                    break;

                default:
                    break;
            }
        }
    };


    private void toggleRefreshRecvMsgStatisticList(boolean isEnable) {
        if (!isEnable) {
            isRunningRecvMsgStaticListUpdate = false;
            return;
        }

        if (isEnable) {
            if (isRunningRecvMsgStaticListUpdate && mThreadRecvMsgListStatisticUpdate != null) {
                return;
            }

            isRunningRecvMsgStaticListUpdate = true;
            mThreadRecvMsgListStatisticUpdate = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRunningRecvMsgStaticListUpdate) {
                        //log("Received count = " + mRecvMsgCount +"   Current deque size = " + mRecvMsgQueue.size());
                        try {
                            mViewRefreshHandler.sendEmptyMessage(R.id.lst_recv_msg_statistc_list);
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            mThreadRecvMsgListStatisticUpdate.start();
        }
    }


    private void toggleMsgStatistic(boolean isEnable) {
        log("toggleMsgStatistic -> " + isEnable);

        if (!isEnable) {
            isRunningMsgStatistic = false;
            return;
        }

        if (isEnable) {
            if (isRunningMsgStatistic && mThreadMsgStatistic != null) {
                return;
            }

            isRunningMsgStatistic = true;
            mThreadMsgStatistic = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isMsgReceivedOnce;
                    RecvMsgRecord record = null;

                    while (isRunningMsgStatistic) {
                        synchronized (SYNC_RECV_MSG_QUEUE) {
                            try {
                                if (mRecvMsgQueue.size() == 0) {
                                    //log("mRecvMsgQueue empty. waiting ...");
                                    SYNC_RECV_MSG_QUEUE.wait();
                                }

                                if (mRecvMsgQueue.size() == 0) //if notified but still no message in queue, skip this loop
                                    continue;

                                record = mRecvMsgQueue.remove();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                        synchronized (SYNC_RECV_MSG_STATISTIC_LIST) {
                            isMsgReceivedOnce = false;
                            for (RecvMsgStatisticRecord r : mRecvMsgStatisticRecordList) {
                                if (r.lastReceivedMsg.mid == record.receivedMsg.mid &&
                                    r.lastReceivedMsg.pid == record.receivedMsg.pid) {
                                    //log("Udpating the item already in mRecvMsgStatisticRecordList ...");
                                    isMsgReceivedOnce = true;
                                    r.updateAndAddCount(record);
                                    break;
                                }
                            }
                            if (isMsgReceivedOnce == false) {
                                //log("Adding new item in mRecvMsgStatisticRecordList ...");
                                RecvMsgStatisticRecord r = new RecvMsgStatisticRecord();
                                r.updateAndAddCount(record);
                                mRecvMsgStatisticRecordList.add(0, r);
                            }
                        }
                    }
                }
            });
            mThreadMsgStatistic.start();
        }
    }


    private void toggleEventMode(boolean isEnable) {
        if (isEnable) {
            ViewOperator.setLabelText(mTxtCurrentReceiveMode, "EVENT MODE");
            mVcilAPI.vcil_j1587_set_event_handler(mEventHandler);
        } else {
            mVcilAPI.vcil_j1587_unset_event_handler();
        }
        return;
    }

    private void togglePollingMode(boolean isEnable) {

        if (!isEnable) {
            isRunningMsgPolling = false;
            return;
        }

        if (isEnable) {
            if (isRunningMsgPolling && mThreadMsgPolling != null) {
                return;
            }
            ViewOperator.setLabelText(mTxtCurrentReceiveMode, "POLLING MODE");

            isRunningMsgPolling = true;
            mThreadMsgPolling = new Thread(new Runnable() {
                @Override
                public void run() {
                    //DO NOT do unnecessary work in receive thread(ex: debug log). It may impact the read performance
                    while (isRunningMsgPolling) {
                        int readRet;

                        /* Read single message every time
                        readRet = operationReadJ1587Msg(J1587DemoActivity.this);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadJ1587MsgMulti(J1587DemoActivity.this);
                        //*/

                        if (readRet == ErrorCode.MRM_ERR_NO_ERROR ||
                            readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
                            //If message is read correctly or currently no received message,
                            //then keep reading

                        } else {
                            String errMsg = String.format("Read ERROR(%X).", readRet);
                            log(errMsg);
                            Toast.makeText(J1587DemoActivity.this.getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
                        }
                        //*/
                    }
                    log("STOPPED POLLING..........");
                }
            });
            mThreadMsgPolling.start();
        }
    }


    private void operationSendJ1587Msg() {
        int ret;
        mSelectedJ1587SendSettingJ1587PortID = (byte)mJ1587SendSettingJ1587PortSpinnerItemList.get(mSpnJ1587SendSettingJ1587Port.getSelectedItemPosition()).value;
        mJ1587SendSettingMID = mEtxtJ1587SendSettingMID.getText().toString().toUpperCase();
        if (!mJ1587SendSettingMID.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "MID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1587SendSettingPID = mEtxtJ1587SendSettingPID.getText().toString().toUpperCase();
        if (!mJ1587SendSettingPID.matches("[0-9a-fA-F]{4}")) {
            Toast.makeText(this, "PID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1587SendSettingPriority = mEtxtJ1587SendSettingPriority.getText().toString().toUpperCase();
        if (!mJ1587SendSettingPriority.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Priority FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1587SendSettingMsgData = mEtxtJ1587SendSettingMsgData.getText().toString().toUpperCase();
        if (mJ1587SendSettingMsgData.length() % 2 != 0 ||
                !mJ1587SendSettingMsgData.matches("[0-9a-fA-F]+") ||
                mJ1587SendSettingMsgData.length() > MRM_CONSTANTS.VCIL_MAX_J1587_DATA_SIZE * 2) {
            Toast.makeText(this, "DATA FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }
        byte[] tempData = HexConverter.hexStringToByteArray(mJ1587SendSettingMsgData);

        byte tempLen = (byte) Integer.parseInt(mEtxtJ1587SendSettingMsgLength.getText().toString());
        if (tempLen < 0 || tempLen > tempData.length) {
            Toast.makeText(this, "LENGTH VALUE ERROR.", Toast.LENGTH_LONG).show();
            return;
        }


        VCIL_J1587_MESSAGE msgObj = new VCIL_J1587_MESSAGE();
        msgObj.mid = (byte) Integer.parseInt(mJ1587SendSettingMID, 16);
        msgObj.pid = Integer.parseInt(mJ1587SendSettingPID, 16);
        msgObj.priority = (byte) Integer.parseInt(mJ1587SendSettingPriority, 16);
        msgObj.data = new byte[MRM_CONSTANTS.VCIL_MAX_J1587_DATA_SIZE];
        System.arraycopy(tempData, 0, msgObj.data, 0, tempData.length);
        msgObj.length = tempLen;
        msgObj.port = mSelectedJ1587SendSettingJ1587PortID;

        ret = sendJ1587Msg(msgObj);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Send OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Send ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    static private int operationReadJ1587Msg(J1587DemoActivity activity) {
        int readRet;
        VCIL_J1587_MESSAGE receivedMsg = new VCIL_J1587_MESSAGE();

        readRet = activity.readJ1587Msg(receivedMsg);

        if (readRet == ErrorCode.MRM_ERR_NO_ERROR) {
            //Put the gotten message to queue. Let statistic thread consume the queue.
            synchronized (SYNC_RECV_MSG_QUEUE) {
                if (activity.isRunningMsgStatistic) {
                    mRecvMsgQueue.add(new RecvMsgRecord(new Date().getTime(), receivedMsg));
                }
                activity.mRecvMsgCount++;
                SYNC_RECV_MSG_QUEUE.notify();
            }

        } else if (readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
            //Read empty. Do nothing
        } else {
            //Read error.Do nothing
        }
        return readRet;
    }

    static private int operationReadJ1587MsgMulti(J1587DemoActivity activity) {
        int readRet;

        ArrayList<VCIL_J1587_MESSAGE> receivedMsgList = new ArrayList<VCIL_J1587_MESSAGE>();
        int desiredReaedNum = 300; //The max number of messages to be read
        int[] resultReadNum = new int[1]; //Actual number of gotten messages


        readRet = activity.readJ1587MsgMulti(receivedMsgList, desiredReaedNum, resultReadNum);

        if (readRet == ErrorCode.MRM_ERR_NO_ERROR) {
            //Put all gotten message to queue. Let statistic thread consume the queue.
            synchronized (SYNC_RECV_MSG_QUEUE) {
                for (int i = 0; i < receivedMsgList.size(); i++) {
                    if (activity.isRunningMsgStatistic) {
                        synchronized (SYNC_RECV_MSG_QUEUE) {
                            mRecvMsgQueue.add(new RecvMsgRecord(new Date().getTime(), receivedMsgList.get(i)));
                        }
                    }
                    activity.mRecvMsgCount++;
                }
                SYNC_RECV_MSG_QUEUE.notify();
            }

        } else if (readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
            //Read empty. Do nothing
        } else {
            //Read error. Do nothing
        }
        return readRet;
    }


    private int sendJ1587Msg(VCIL_J1587_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_j1587_write(msgObj);
        return ret;
    }


    private int readJ1587Msg(VCIL_J1587_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_j1587_read(msgObj);
        return ret;
    }

    private int readJ1587MsgMulti(ArrayList<VCIL_J1587_MESSAGE> msgObjList, int desiredNum, int[] resultReadNum) {
        int ret;
        ret = mVcilAPI.vcil_j1587_read_multi(msgObjList, desiredNum, resultReadNum);
        return ret;
    }
}