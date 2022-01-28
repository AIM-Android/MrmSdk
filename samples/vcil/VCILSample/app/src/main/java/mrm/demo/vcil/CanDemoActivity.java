package mrm.demo.vcil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
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
import mrm.define.MRM_ENUM;
import mrm.define.VCIL.VCIL_CAN_ERROR_STATUS;
import mrm.define.VCIL.VCIL_CAN_MESSAGE;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.HexConverter;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;
import mrm.demo.util.ViewOperator;

public class CanDemoActivity extends Activity {

    static String TAG = "SDKv4 VCIL_DEMO" + " - CAN";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnCanPortSettingCanPort;
    SpinnerCustomAdapter mCanPortSettingCanPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanPortSettingCanPortSpinnerItemList;
    byte mSelectedCanPortSettingCanPortId;

    Spinner mSpnCanPortSettingCanSpeed;
    SpinnerCustomAdapter mCanPortSettingCanSpeedSpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanPortSettingCanSpeedSpinnerItemList;
    int mSelectedCanPortSettingCanSpeedId;

    TextView mTxtCanPortSettingCanPortMode;

    Button mBtnCanPortSettingGet;
    Button mBtnCanPortSettingSetNormalMode;
    Button mBtnCanPortSettingSetListenMode;


    Spinner mSpnCanPortErrorCanPort;
    SpinnerCustomAdapter mCanPortErrorCanPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanPortErrorCanPortSpinnerItemList;
    byte mSelectedCanPortErrorCanPortId;

    TextView mTxtCanPortErrorRec;
    TextView mTxtCanPortErrorTec;
    TextView mTxtCanPortErrorLastErrorCode;
    TextView mTxtCanPortErrorFlag;
    Button mBtnCanPortErrorGet;


    Spinner mSpnCanSendSettingCanPort;
    SpinnerCustomAdapter mCanSendSettingCanBusSpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanSendSettingCanPortSpinnerItemList;
    byte mSelectedCanSendSettingCanBusID;

    CheckBox mCbCanSendExtFrame;
    CheckBox mCbCanSendRTR;

    EditText mEtxtCanSendSettingMsgID;
    String mCanSendSettingMsgID = "";

    EditText mEtxtCanSendSettingMsgData;
    String mCanSendSettingMsgData = "";

    EditText mEtxtCanSendSettingMsgLength;

    Button mBtnCanSend;

    Button mBtnCanFilter;
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
        VCIL_CAN_MESSAGE receivedMsg = new VCIL_CAN_MESSAGE();

        public RecvMsgRecord(long time, VCIL_CAN_MESSAGE msgObj) {
            receivedTime = time;
            receivedMsg.copyFrom(msgObj);
        }
    }

    public static class RecvMsgStatisticRecord {
        long lastReceivedTime;
        long count;
        VCIL_CAN_MESSAGE lastReceivedMsg = new VCIL_CAN_MESSAGE();

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
        private WeakReference<CanDemoActivity> mActivity = null;

        public MyViewRefreshHandler(CanDemoActivity activity) {
            mActivity = new WeakReference<CanDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CanDemoActivity activity = mActivity.get();

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
        private WeakReference<CanDemoActivity> mActivity = null;

        public MyEventHandler(CanDemoActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<CanDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CanDemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            int eventID = msg.what;

            switch (eventID) {
                case MRM_CONSTANTS.VCIL_EVENT_ID_RECEIVED_MSG_CAN:

                    activity.mVcilAPI.vcil_can_wait_event(false);

                    while (true) {
                        int readRet;
                        /* Read single message every time
                        readRet = operationReadCanMsg(activity);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadCanMsgMulti(activity);
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
                    activity.mVcilAPI.vcil_can_wait_event(true);
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

        mEventHandlerThread = new HandlerThread("CANEventHandlerThread");
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

    private void setSpinnerItenLists() {
        mCanPortSettingCanPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mCanPortSettingCanPortSpinnerItemList.clear();
        mCanPortSettingCanPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mCanPortSettingCanPortSpinnerItemList.add(new SpinnerItem("1", 1));

        mCanPortErrorCanPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mCanPortErrorCanPortSpinnerItemList.clear();
        mCanPortErrorCanPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mCanPortErrorCanPortSpinnerItemList.add(new SpinnerItem("1", 1));

        mCanPortSettingCanSpeedSpinnerItemList = new ArrayList<SpinnerItem>();
        mCanPortSettingCanSpeedSpinnerItemList.clear();
        mCanPortSettingCanSpeedSpinnerItemList.add(new SpinnerItem("100 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_100K.getValue()));
        mCanPortSettingCanSpeedSpinnerItemList.add(new SpinnerItem("125 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_125K.getValue()));
        mCanPortSettingCanSpeedSpinnerItemList.add(new SpinnerItem("200 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_200K.getValue()));
        mCanPortSettingCanSpeedSpinnerItemList.add(new SpinnerItem("250 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_250K.getValue()));
        mCanPortSettingCanSpeedSpinnerItemList.add(new SpinnerItem("500 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_500K.getValue()));
        mCanPortSettingCanSpeedSpinnerItemList.add(new SpinnerItem("1 Mbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_1M.getValue()));

        mCanSendSettingCanPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mCanSendSettingCanPortSpinnerItemList.clear();
        mCanSendSettingCanPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mCanSendSettingCanPortSpinnerItemList.add(new SpinnerItem("1", 1));
    }


    private void initView() {
        setContentView(R.layout.activity_can_demo);

        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnCanPortSettingCanPort = (Spinner) findViewById(R.id.spn_can_port_setting_port);
        mSpnCanPortSettingCanSpeed = (Spinner) findViewById(R.id.spn_can_port_setting_speed);
        mTxtCanPortSettingCanPortMode = (TextView) findViewById(R.id.spn_can_port_setting_port_mode);
        mBtnCanPortSettingGet = (Button) findViewById(R.id.btn_can_port_setting_get);
        mBtnCanPortSettingSetNormalMode = (Button) findViewById(R.id.btn_can_port_setting_set_normal_mode);
        mBtnCanPortSettingSetListenMode = (Button) findViewById(R.id.btn_can_port_setting_set_listen_mode);

        mCanPortSettingCanPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanPortSettingCanPortSpinnerItemList);
        mSpnCanPortSettingCanPort.setAdapter(mCanPortSettingCanPortSpinnerDataAdapter);
        mCanPortSettingCanSpeedSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanPortSettingCanSpeedSpinnerItemList);
        mSpnCanPortSettingCanSpeed.setAdapter(mCanPortSettingCanSpeedSpinnerDataAdapter);


        mSpnCanPortErrorCanPort = (Spinner) findViewById(R.id.spn_can_port_error_port);
        mTxtCanPortErrorRec = (TextView) findViewById(R.id.spn_can_port_err_rec);
        mTxtCanPortErrorTec = (TextView) findViewById(R.id.spn_can_port_err_tec);
        mTxtCanPortErrorLastErrorCode = (TextView) findViewById(R.id.spn_can_port_err_last_error_code);
        mTxtCanPortErrorFlag = (TextView) findViewById(R.id.spn_can_port_err_flag);
        mBtnCanPortErrorGet = (Button) findViewById(R.id.btn_can_port_error_get);

        mCanPortErrorCanPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanPortErrorCanPortSpinnerItemList);
        mSpnCanPortErrorCanPort.setAdapter(mCanPortErrorCanPortSpinnerDataAdapter);


        mSpnCanSendSettingCanPort = (Spinner) findViewById(R.id.spn_can_send_port);
        mCbCanSendExtFrame = (CheckBox) findViewById(R.id.cb_can_send_ext_frame);
        mCbCanSendRTR = (CheckBox) findViewById(R.id.cb_can_send_remote_request);
        mEtxtCanSendSettingMsgID = (EditText) findViewById(R.id.etxt_can_send_id);
        mEtxtCanSendSettingMsgData = (EditText) findViewById(R.id.etxt_can_send_data);
        mEtxtCanSendSettingMsgLength = (EditText) findViewById(R.id.etxt_can_send_length);
        mBtnCanSend = (Button) findViewById(R.id.btn_can_send);

        mCanSendSettingCanBusSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanSendSettingCanPortSpinnerItemList);
        mSpnCanSendSettingCanPort.setAdapter(mCanSendSettingCanBusSpinnerDataAdapter);
        mSpnCanSendSettingCanPort.setSelection(0);

        mCbDoStatistic = (CheckBox) findViewById(R.id.cb_do_statistic);
        mBtnTogglePollingMode = (Button) findViewById(R.id.btn_toggle_polling_mode);
        mBtnToggleEventMode = (Button) findViewById(R.id.btn_toggle_event_mode);
        mTxtCurrentReceiveMode = (TextView) findViewById(R.id.txt_current_receive_mode);

        mBtnRefreshList = (Button) findViewById(R.id.btn_refresh_list);
        mBtnClearList = (Button) findViewById(R.id.btn_clear_list);
        mBtnCanFilter = (Button) findViewById(R.id.btn_can_filter);

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
                synchronized (SYNC_RECV_MSG_STATISTIC_LIST) {
                    View listRow = CanDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_can_demo_lst_row_can_received_msg, null);

                    RecvMsgStatisticRecord record = mRecvMsgStatisticRecordList.get(position);

                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_time)).setText(DateFormat.format("HH:mm:ss", record.lastReceivedTime));
                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_bus)).setText(String.valueOf(record.lastReceivedMsg.port));
                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_ext)).setText(String.valueOf(record.lastReceivedMsg.extended_frame));
                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_rtr)).setText(String.valueOf(record.lastReceivedMsg.remote_request));
                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_id)).setText(String.format("%X", record.lastReceivedMsg.id));
                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_data)).setText(HexConverter.byteArrayToHexString(record.lastReceivedMsg.data, record.lastReceivedMsg.length));
                    ((TextView) listRow.findViewById(R.id.txt_can_received_msg_count)).setText(String.valueOf(record.count));
                    return listRow;
                }
            }
        };
        mLstRecvMsgMsgStaticList.setAdapter(mLstRecvMsgMsgStaticListDataAdapter);
    }


    private void initListener() {
        mBtnCanPortSettingGet.setOnClickListener(mBtnOnClickListener);
        mBtnCanPortSettingSetNormalMode.setOnClickListener(mBtnOnClickListener);
        mBtnCanPortSettingSetListenMode.setOnClickListener(mBtnOnClickListener);

        mBtnCanPortErrorGet.setOnClickListener(mBtnOnClickListener);

        mBtnCanSend.setOnClickListener(mBtnOnClickListener);

        mCbDoStatistic.setOnCheckedChangeListener(mCbOnCheckedChangeListener);
        mBtnTogglePollingMode.setOnClickListener(mBtnOnClickListener);
        mBtnToggleEventMode.setOnClickListener(mBtnOnClickListener);

        mBtnRefreshList.setOnClickListener(mBtnOnClickListener);
        mBtnClearList.setOnClickListener(mBtnOnClickListener);
        mBtnCanFilter.setOnClickListener(mBtnOnClickListener);
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
                case R.id.btn_can_port_setting_get:
                    operationGetCanPortSpeed();
                    break;

                case R.id.btn_can_port_setting_set_normal_mode:
                    operationSetCanPortSpeedNormalMode();
                    break;
                case R.id.btn_can_port_setting_set_listen_mode:
                    operationSetCanPortSpeedListenMode();
                    break;

                case R.id.btn_can_port_error_get:
                    operationGetCanPortErrorStatus();
                    break;

                case R.id.btn_can_send:
                    operationSendCanMsg();
                    break;

                case R.id.btn_can_filter:
                    Intent in = new Intent();
                    in.setClass(CanDemoActivity.this, CanFilterDemoActivity.class);
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
                            Thread.sleep(500);
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
            synchronized (SYNC_RECV_MSG_QUEUE) {
                SYNC_RECV_MSG_QUEUE.notify();
            }
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
                                if (r.lastReceivedMsg.port == record.receivedMsg.port &&
                                        r.lastReceivedMsg.extended_frame == record.receivedMsg.extended_frame &&
                                        r.lastReceivedMsg.remote_request == record.receivedMsg.remote_request &&
                                        r.lastReceivedMsg.id == record.receivedMsg.id) {

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
            mVcilAPI.vcil_can_set_event_handler(mEventHandler);
        } else {
            mVcilAPI.vcil_can_unset_event_handler();
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
                        readRet = operationReadCanMsg(CanDemoActivity.this);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadCanMsgMulti(CanDemoActivity.this);
                        //*/

                        if (readRet == ErrorCode.MRM_ERR_NO_ERROR ||
                            readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
                            //If message is read correctly or currently no received message,
                            //then keep reading

                        } else {
                            isRunningMsgPolling = false;
                            String errMsg = String.format("Read Message ERROR(%08X).  Stop polling.", readRet);
                            log(errMsg);
                            Toast.makeText(CanDemoActivity.this.getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    log("STOPPED POLLING..........");
                }
            });
            mThreadMsgPolling.start();
        }
    }

    private void operationGetCanPortSpeed() {
        int ret;
        mSelectedCanPortSettingCanPortId = (byte) mCanPortSettingCanPortSpinnerItemList.get(mSpnCanPortSettingCanPort.getSelectedItemPosition()).value;
        int[] canSpeedId = new int[1];
        int[] canPortMode = new int[1];


        ret = getCanBusSpeed(mSelectedCanPortSettingCanPortId, canSpeedId, canPortMode);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Get OK", Toast.LENGTH_LONG).show();

            //Switch speed spinner
            int position = 0;
            for(int i = 0; i < mCanPortSettingCanSpeedSpinnerItemList.size(); i++) {
                if (mCanPortSettingCanSpeedSpinnerItemList.get(i).value == canSpeedId[0]) {
                    position = i;
                }
            }
            mSpnCanPortSettingCanSpeed.setSelection(position);

            //Display port mode
            if (canPortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_NORMAL_MODE.getValue()) {
                mTxtCanPortSettingCanPortMode.setText("NORMAL MODE");
            } else if (canPortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_LISTEN_MODE.getValue()) {
                mTxtCanPortSettingCanPortMode.setText("LISTEN MODE");
            } else if (canPortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_INIT_MODE.getValue()) {
                mTxtCanPortSettingCanPortMode.setText("INIT MODE");
            } else {
                mTxtCanPortSettingCanPortMode.setText(String.format("UNKNOWN (%d)", canPortMode[0]));
            }

        } else {
            Toast.makeText(this, "Get ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSetCanPortSpeedNormalMode() {
        int ret;
        mSelectedCanPortSettingCanPortId = (byte) mCanPortSettingCanPortSpinnerItemList.get(mSpnCanPortSettingCanPort.getSelectedItemPosition()).value;
        mSelectedCanPortSettingCanSpeedId = mCanPortSettingCanSpeedSpinnerItemList.get(mSpnCanPortSettingCanSpeed.getSelectedItemPosition()).value;

        ret = setCanPortSpeedNormalMode(mSelectedCanPortSettingCanPortId, mSelectedCanPortSettingCanSpeedId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSetCanPortSpeedListenMode() {
        int ret;
        mSelectedCanPortSettingCanPortId = (byte) mCanPortSettingCanPortSpinnerItemList.get(mSpnCanPortSettingCanPort.getSelectedItemPosition()).value;
        mSelectedCanPortSettingCanSpeedId = mCanPortSettingCanSpeedSpinnerItemList.get(mSpnCanPortSettingCanSpeed.getSelectedItemPosition()).value;

        ret = setCanPortSpeedListenMode(mSelectedCanPortSettingCanPortId, mSelectedCanPortSettingCanSpeedId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private void operationGetCanPortErrorStatus() {
        int ret;
        mSelectedCanPortErrorCanPortId = (byte) mCanPortErrorCanPortSpinnerItemList.get(mSpnCanPortErrorCanPort.getSelectedItemPosition()).value;
        VCIL_CAN_ERROR_STATUS errorStatus = new VCIL_CAN_ERROR_STATUS();

        ret = getCanErrorStatus(mSelectedCanPortErrorCanPortId, errorStatus);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Get OK", Toast.LENGTH_LONG).show();


            //Display error status
            mTxtCanPortErrorRec.setText(String.valueOf(errorStatus.rec));
            mTxtCanPortErrorTec.setText(String.valueOf(errorStatus.tec));

            String lastErrCodeTranslation = "";
            switch(errorStatus.last_error_code)
            {
                case 0:
                    lastErrCodeTranslation = "No error";
                    break;
                case 1:
                    lastErrCodeTranslation = "Stuff Error";
                    break;
                case 2:
                    lastErrCodeTranslation = "Form Error";
                    break;
                case 3:
                    lastErrCodeTranslation = "Acknowledgment Error";
                    break;
                case 4:
                    lastErrCodeTranslation = "Bit recessive Error";
                    break;
                case 5:
                    lastErrCodeTranslation = "Bit dominant Error";
                    break;
                case 6:
                    lastErrCodeTranslation = "CRC Error";
                    break;
                case 7:
                    lastErrCodeTranslation = "Set by software";
                    break;
                default:
                    lastErrCodeTranslation = "-";
                    break;

            }
            mTxtCanPortErrorLastErrorCode.setText(
                    String.format("%d ( %s )", errorStatus.last_error_code, lastErrCodeTranslation ));


            String flagTranslation = "";
            if( (errorStatus.error_flag & 0x01) != 0)
                flagTranslation += "Error warning flag, ";
            if( (errorStatus.error_flag & 0x02) != 0)
                flagTranslation += "Error passive flag, ";
            if( (errorStatus.error_flag & 0x04) != 0)
                flagTranslation += "Bus-off flag, ";
            mTxtCanPortErrorFlag.setText(
                    String.format("0x%08X ( %s )", errorStatus.error_flag, flagTranslation ));

        } else {
            Toast.makeText(this, "Get ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSendCanMsg() {
        int ret;
        mSelectedCanSendSettingCanBusID = (byte)mCanSendSettingCanPortSpinnerItemList.get(mSpnCanSendSettingCanPort.getSelectedItemPosition()).value;
        mCanSendSettingMsgID = mEtxtCanSendSettingMsgID.getText().toString().toUpperCase();
        if (!mCanSendSettingMsgID.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "ID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mCanSendSettingMsgData = mEtxtCanSendSettingMsgData.getText().toString().toUpperCase();
        if (mCanSendSettingMsgData.length() % 2 != 0 ||
                !mCanSendSettingMsgData.matches("[0-9a-fA-F]+") ||
                mCanSendSettingMsgData.length() > MRM_CONSTANTS.VCIL_MAX_CAN_DATA_SIZE * 2) {
            Toast.makeText(this, "DATA FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }
        byte[] tempData = HexConverter.hexStringToByteArray(mCanSendSettingMsgData);

        byte tempLen = (byte) Integer.parseInt(mEtxtCanSendSettingMsgLength.getText().toString());
        if (tempLen < 0 || tempLen > tempData.length) {
            Toast.makeText(this, "LENGTH VALUE ERROR.", Toast.LENGTH_LONG).show();
            return;
        }


        VCIL_CAN_MESSAGE msgObj = new VCIL_CAN_MESSAGE();
        msgObj.port = mSelectedCanSendSettingCanBusID;
        msgObj.extended_frame = mCbCanSendExtFrame.isChecked();
        msgObj.remote_request = mCbCanSendRTR.isChecked();
        msgObj.id = Integer.parseInt(mCanSendSettingMsgID, 16);
        msgObj.data = new byte[MRM_CONSTANTS.VCIL_MAX_CAN_DATA_SIZE];
        System.arraycopy(tempData, 0, msgObj.data, 0, tempData.length);
        msgObj.length = tempLen;

        ret = sendCanMsg(msgObj);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Send OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Send ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    static private int operationReadCanMsg(CanDemoActivity activity) {
        int readRet;
        VCIL_CAN_MESSAGE receivedMsg = new VCIL_CAN_MESSAGE();

        readRet = activity.readCanMsg(receivedMsg);

        if (readRet == ErrorCode.MRM_ERR_NO_ERROR) {
            //Put the gotten message to queue. Let statistic thread consume the queue.
            synchronized (SYNC_RECV_MSG_QUEUE) {
                if (activity.isRunningMsgStatistic) {
                    mRecvMsgQueue.add(new RecvMsgRecord(new Date().getTime(), receivedMsg));
                }
                activity.mRecvMsgCount++;
                SYNC_RECV_MSG_QUEUE.notify();
            }

        } else if (readRet != ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
            //Read empty. Do nothing

        } else {
            //Read error.Do nothing
            Toast.makeText(activity, String.format("Read message Failed. (0x%04X)",readRet), Toast.LENGTH_SHORT).show();
        }
        return readRet;
    }


    static private int operationReadCanMsgMulti(CanDemoActivity activity) {
        int readRet;

        ArrayList<VCIL_CAN_MESSAGE> receivedMsgList = new ArrayList<VCIL_CAN_MESSAGE>();
        int desiredReaedNum = 300; //The max number of messages to be read
        int[] resultReadNum = new int[1]; //Actual number of gotten messages


        readRet = activity.readCanMsgMulti(receivedMsgList, desiredReaedNum, resultReadNum);

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
            Toast.makeText(activity, String.format("Read message Failed. (0x%04X)",readRet), Toast.LENGTH_SHORT).show();
        }
        return readRet;
    }


    private int getCanBusSpeed(byte port, int[] canSpeedID, int[] mode) {
        int ret;
        ret = mVcilAPI.vcil_can_get_speed(port, canSpeedID, mode);
        return ret;
    }

    private int setCanPortSpeedNormalMode(byte port, int canSpeedID) {
        int ret;
        ret = mVcilAPI.vcil_can_set_speed(port, canSpeedID);
        return ret;
    }

    private int setCanPortSpeedListenMode(byte port, int canSpeedID) {
        int ret;
        ret = mVcilAPI.vcil_can_set_speed_listen_mode(port, canSpeedID);
        return ret;
    }

    private int getCanErrorStatus(byte port, VCIL_CAN_ERROR_STATUS errStatus) {
        int ret;
        ret = mVcilAPI.vcil_can_get_bus_error_status(port, errStatus);
        return ret;
    }



    private int sendCanMsg(VCIL_CAN_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_can_write(msgObj);
        return ret;
    }


    private int readCanMsg(VCIL_CAN_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_can_read(msgObj);
        return ret;
    }

    private int readCanMsgMulti(ArrayList<VCIL_CAN_MESSAGE> msgObjList, int desiredNum, int[] resultReadNum) {
        /*
        vcil_can_read_multi() can help you to read multiple message at once.

        You can set the max number of messages you want to read at once by using the argument desiredNum.
        Instead of the calling vcil_can_read() multiple times,
        this vcil_can_read_multi() can read data more efficiently when the traffic on bus is heavy.

        However, if you set the max number to 1,
        it will do the same as vcil_can_read
        () but with more overhead which results in longer execution time.

        You can adjust the max number base on the actual data traffic on bus.
        //*/

        int ret;
        ret = mVcilAPI.vcil_can_read_multi(msgObjList, desiredNum, resultReadNum);
        return ret;
    }
}
