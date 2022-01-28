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
import mrm.define.MRM_ENUM;
import mrm.define.VCIL.VCIL_J1939_MESSAGE;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.HexConverter;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;
import mrm.demo.util.ViewOperator;

public class J1939DemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - J1939";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnJ1939PortSettingJ1939Port;
    SpinnerCustomAdapter mJ1939PortSettingJ1939PortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1939PortSettingJ1939PortSpinnerItemList;
    byte mSelectedJ1939PortSettingJ1939PortId;

    Spinner mSpnJ1939PortSettingJ1939Speed;
    SpinnerCustomAdapter mJ1939PortSettingJ1939SpeedSpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1939PortSettingJ1939SpeedSpinnerItemList;
    int mSelectedJ1939PortSettingJ1939SpeedId;

    TextView mTxtJ1939PortSettingJ1939PortMode;

    Button mBtnJ1939PortSettingGet;
    Button mBtnJ1939PortSettingSetNormalMode;
    Button mBtnJ1939PortSettingSetListenMode;

    Button mBtnJ1939Config;

    Spinner mSpnJ1939SendSettingPort;
    SpinnerCustomAdapter mJ1939SendSettingPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1939SendSettingPortSpinnerItemList;
    int mSelectedJ1939SendSettingPortId;

    EditText mEtxtJ1939SendSettingPgn;
    String mJ1939SendSettingPgn = "";

    EditText mEtxtJ1939SendSettingSrc;
    String mJ1939SendSettingSrc = "";

    EditText mEtxtJ1939SendSettingDst;
    String mJ1939SendSettingDst = "";

    EditText mEtxtJ1939SendSettingPri;
    String mJ1939SendSettingPri = "";

    EditText mEtxtJ1939SendSettingMsgData;
    String mJ1939SendSettingMsgData = "";

    EditText mEtxtJ1939SendSettingMsgLength;
    String mJ1939SendSettingMsgLength = "";

    Button mBtnJ1939Send;

    Button mBtnFilter;
    Button mBtnClearList;
    Button mBtnRefreshList;
    TextView mTotalRecvMsgCount;
    ListView mLstRecvMsgMsgStaticList;
    BaseAdapter mLstRecvMsgMsgStaticListDataAdapter;
    static ArrayList<RecvMsgStatisticRecord> mRecvMsgStatisticRecordList = new ArrayList<RecvMsgStatisticRecord>();
    static LinkedBlockingQueue<RecvMsgRecord> mRecvMsgQueue = new LinkedBlockingQueue<RecvMsgRecord>();
    static final Object SYNC_RECV_MSG_QUEUE = new Object();
    static final Object SYNC_RECV_MSG_STATISTIC_LIST = new Object();

    TextView mTxtCurrentReceiveMode;
    Button mBtnTogglePollingMode;
    Button mBtnToggleEventMode;
    CheckBox mCbDoStatistic;

    long mRecvMsgCount = 0;

    public static class RecvMsgRecord {
        long receivedTime;
        VCIL_J1939_MESSAGE receivedMsg = new VCIL_J1939_MESSAGE();

        public RecvMsgRecord(long time, VCIL_J1939_MESSAGE msgObj) {
            receivedTime = time;
            receivedMsg.copyFrom(msgObj);
        }
    }

    public static class RecvMsgStatisticRecord {
        long lastReceiveTime;
        VCIL_J1939_MESSAGE lastReceivedMsg = new VCIL_J1939_MESSAGE();
        long count;

        public RecvMsgStatisticRecord() {
            lastReceiveTime = 0;
            count = 0;
        }

        public void updateAndAddCount(RecvMsgRecord record) {
            this.lastReceiveTime = record.receivedTime;
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
        private WeakReference<J1939DemoActivity> mActivity = null;

        public MyViewRefreshHandler(J1939DemoActivity activity) {
            mActivity = new WeakReference<J1939DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            J1939DemoActivity activity = mActivity.get();

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
        private WeakReference<J1939DemoActivity> mActivity = null;

        public MyEventHandler(J1939DemoActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<J1939DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            J1939DemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            int eventID = msg.what;

            switch (eventID) {
                case MRM_CONSTANTS.VCIL_EVENT_ID_RECEIVED_MSG_J1939:

                    activity.mVcilAPI.vcil_j1939_wait_event(false);
                    while (true) {
                        int readRet;
                        /* Read single message every time
                        readRet = operationReadJ1939Msg(activity);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadJ1939MsgMulti(activity);
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
                    activity.mVcilAPI.vcil_j1939_wait_event(true);
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

        mEventHandlerThread = new HandlerThread("J1939EventHandlerThread");
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
        mJ1939PortSettingJ1939PortSpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1939PortSettingJ1939PortSpinnerItemList.clear();
        mJ1939PortSettingJ1939PortSpinnerItemList.add(new SpinnerItem("0", 0));
        mJ1939PortSettingJ1939PortSpinnerItemList.add(new SpinnerItem("1", 1));

        mJ1939PortSettingJ1939SpeedSpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1939PortSettingJ1939SpeedSpinnerItemList.clear();
        mJ1939PortSettingJ1939SpeedSpinnerItemList.add(new SpinnerItem("250 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_250K.getValue()));
        mJ1939PortSettingJ1939SpeedSpinnerItemList.add(new SpinnerItem("500 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_500K.getValue()));

        mJ1939SendSettingPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1939SendSettingPortSpinnerItemList.clear();
        mJ1939SendSettingPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mJ1939SendSettingPortSpinnerItemList.add(new SpinnerItem("1", 1));
    }


    private void initView() {
        setContentView(R.layout.activity_j1939_demo);

        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnJ1939PortSettingJ1939Port = (Spinner) findViewById(R.id.spn_j1939_port_setting_port);
        mSpnJ1939PortSettingJ1939Speed = (Spinner) findViewById(R.id.spn_j1939_port_setting_speed);
        mTxtJ1939PortSettingJ1939PortMode = (TextView) findViewById(R.id.spn_j1939_port_setting_port_mode);
        mBtnJ1939PortSettingGet = (Button) findViewById(R.id.btn_j1939_port_setting_get);
        mBtnJ1939PortSettingSetNormalMode = (Button) findViewById(R.id.btn_j1939_port_setting_set_normal_mode);
        mBtnJ1939PortSettingSetListenMode = (Button) findViewById(R.id.btn_j1939_port_setting_set_listen_mode);

        mJ1939PortSettingJ1939PortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1939PortSettingJ1939PortSpinnerItemList);
        mSpnJ1939PortSettingJ1939Port.setAdapter(mJ1939PortSettingJ1939PortSpinnerDataAdapter);
        mJ1939PortSettingJ1939SpeedSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1939PortSettingJ1939SpeedSpinnerItemList);
        mSpnJ1939PortSettingJ1939Speed.setAdapter(mJ1939PortSettingJ1939SpeedSpinnerDataAdapter);

        mBtnJ1939Config = (Button) findViewById(R.id.btn_j1939_config);

        mSpnJ1939SendSettingPort = (Spinner) findViewById(R.id.spn_j1939_send_port);
        mEtxtJ1939SendSettingPgn = (EditText) findViewById(R.id.etxt_j1939_send_pgn);
        mEtxtJ1939SendSettingSrc = (EditText) findViewById(R.id.etxt_j1939_send_src);
        mEtxtJ1939SendSettingDst = (EditText) findViewById(R.id.etxt_j1939_send_dst);
        mEtxtJ1939SendSettingPri = (EditText) findViewById(R.id.etxt_j1939_send_pri);
        mEtxtJ1939SendSettingMsgData = (EditText) findViewById(R.id.etxt_j1939_send_data);
        mEtxtJ1939SendSettingMsgLength = (EditText) findViewById(R.id.etxt_j1939_send_length);
        mBtnJ1939Send = (Button) findViewById(R.id.btn_j1939_send);

        mJ1939SendSettingPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1939SendSettingPortSpinnerItemList);
        mSpnJ1939SendSettingPort.setAdapter(mJ1939SendSettingPortSpinnerDataAdapter);
        mSpnJ1939SendSettingPort.setSelection(0);

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
                synchronized (SYNC_RECV_MSG_STATISTIC_LIST) {
                    View listRow = J1939DemoActivity.this.getLayoutInflater().inflate(R.layout.activity_j1939_demo_lst_row_j1939_received_msg, null);

                    RecvMsgStatisticRecord record = mRecvMsgStatisticRecordList.get(position);

                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_time)).setText(DateFormat.format("HH:mm:ss", record.lastReceiveTime));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_port)).setText(String.valueOf(record.lastReceivedMsg.port));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_pgn)).setText(String.format("%X", record.lastReceivedMsg.pgn));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_src)).setText(String.format("%X", record.lastReceivedMsg.source));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_dst)).setText(String.format("%X", record.lastReceivedMsg.destination));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_pri)).setText(String.format("%X", record.lastReceivedMsg.priority));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_data)).setText(HexConverter.byteArrayToHexString(record.lastReceivedMsg.data, record.lastReceivedMsg.length));
                    ((TextView) listRow.findViewById(R.id.txt_j1939_received_msg_count)).setText(String.valueOf(record.count));
                    return listRow;
                }
            }
        };
        mLstRecvMsgMsgStaticList.setAdapter(mLstRecvMsgMsgStaticListDataAdapter);
    }


    private void initListener() {
        mBtnJ1939PortSettingGet.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939PortSettingSetNormalMode.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939PortSettingSetListenMode.setOnClickListener(mBtnOnClickListener);

        mBtnJ1939Config.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939Send.setOnClickListener(mBtnOnClickListener);

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
                case R.id.btn_j1939_port_setting_get:
                    operationGetJ1939PortSpeed();
                    break;

                case R.id.btn_j1939_port_setting_set_normal_mode:
                    operationSetJ1939PortSpeedNormalMode();
                    break;

                case R.id.btn_j1939_port_setting_set_listen_mode:
                    operationSetJ1939PortSpeedListenMode();
                    break;

                case R.id.btn_j1939_config:
                    Intent intentConfig = new Intent();
                    intentConfig.setClass(J1939DemoActivity.this, J1939ConfigDemoActivity.class);
                    startActivity(intentConfig);
                    break;

                case R.id.btn_j1939_send:
                    operationSendJ1939Msg();
                    break;

                case R.id.btn_filter:
                    Intent in = new Intent();
                    in.setClass(J1939DemoActivity.this, J1939FilterDemoActivity.class);
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
                                    r.lastReceivedMsg.pgn == record.receivedMsg.pgn) {
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
            mVcilAPI.vcil_j1939_set_event_handler(mEventHandler);
        } else {
            mVcilAPI.vcil_j1939_unset_event_handler();
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
                        readRet = operationReadJ1939Msg(J1939DemoActivity.this);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadJ1939MsgMulti(J1939DemoActivity.this);
                        //*/

                        if (readRet == ErrorCode.MRM_ERR_NO_ERROR ||
                                readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
                            //If message is read correctly or currently no received message,
                            //then keep reading

                        } else {
                            isRunningMsgPolling = false;
                            String errMsg = String.format("Read Message ERROR(%08X).  Stop polling.", readRet);
                            log(errMsg);
                            Toast.makeText(J1939DemoActivity.this.getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    log("STOPPED POLLING..........");
                }
            });
            mThreadMsgPolling.start();
        }
    }

    private void operationGetJ1939PortSpeed() {
        int ret;
        mSelectedJ1939PortSettingJ1939PortId = (byte) mJ1939PortSettingJ1939PortSpinnerItemList.get(mSpnJ1939PortSettingJ1939Port.getSelectedItemPosition()).value;
        int[] j1939SpeedId = new int[1];
        int[] j1939PortMode = new int[1];


        ret = getJ1939BusSpeed(mSelectedJ1939PortSettingJ1939PortId, j1939SpeedId, j1939PortMode);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Get OK", Toast.LENGTH_LONG).show();

            //Switch speed spinner
            int position = 0;
            for(int i = 0; i < mJ1939PortSettingJ1939SpeedSpinnerItemList.size(); i++) {
                if (mJ1939PortSettingJ1939SpeedSpinnerItemList.get(i).value == j1939SpeedId[0]) {
                    position = i;
                }
            }
            mSpnJ1939PortSettingJ1939Speed.setSelection(position);

            //Display port mode
            if (j1939PortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_NORMAL_MODE.getValue()) {
                mTxtJ1939PortSettingJ1939PortMode.setText("NORMAL MODE");
            } else if (j1939PortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_LISTEN_MODE.getValue()) {
                mTxtJ1939PortSettingJ1939PortMode.setText("LISTEN MODE");
            } else if (j1939PortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_INIT_MODE.getValue()) {
                mTxtJ1939PortSettingJ1939PortMode.setText("INIT MODE");
            } else {
                mTxtJ1939PortSettingJ1939PortMode.setText(String.format("UNKNOWN (%d)", j1939PortMode[0]));
            }

        } else {
            Toast.makeText(this, "Get ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSetJ1939PortSpeedNormalMode() {
        int ret;
        mSelectedJ1939PortSettingJ1939PortId = (byte) mJ1939PortSettingJ1939PortSpinnerItemList.get(mSpnJ1939PortSettingJ1939Port.getSelectedItemPosition()).value;
        mSelectedJ1939PortSettingJ1939SpeedId = mJ1939PortSettingJ1939SpeedSpinnerItemList.get(mSpnJ1939PortSettingJ1939Speed.getSelectedItemPosition()).value;

        ret = setJ1939PortSpeedNormalMode(mSelectedJ1939PortSettingJ1939PortId, mSelectedJ1939PortSettingJ1939SpeedId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSetJ1939PortSpeedListenMode() {
        int ret;
        mSelectedJ1939PortSettingJ1939PortId = (byte) mJ1939PortSettingJ1939PortSpinnerItemList.get(mSpnJ1939PortSettingJ1939Port.getSelectedItemPosition()).value;
        mSelectedJ1939PortSettingJ1939SpeedId = mJ1939PortSettingJ1939SpeedSpinnerItemList.get(mSpnJ1939PortSettingJ1939Speed.getSelectedItemPosition()).value;

        ret = setJ1939PortSpeedListenMode(mSelectedJ1939PortSettingJ1939PortId, mSelectedJ1939PortSettingJ1939SpeedId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSendJ1939Msg() {
        int ret;
        mSelectedJ1939SendSettingPortId = mJ1939SendSettingPortSpinnerItemList.get(mSpnJ1939SendSettingPort.getSelectedItemPosition()).value;

        mJ1939SendSettingPgn = mEtxtJ1939SendSettingPgn.getText().toString().toUpperCase();
        if (!mJ1939SendSettingPgn.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "PGN FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939SendSettingSrc = mEtxtJ1939SendSettingSrc.getText().toString().toUpperCase();
        if (!mJ1939SendSettingSrc.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "SRC FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939SendSettingDst = mEtxtJ1939SendSettingDst.getText().toString().toUpperCase();
        if (!mJ1939SendSettingDst.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "DST FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939SendSettingPri = mEtxtJ1939SendSettingPri.getText().toString().toUpperCase();
        if (!mJ1939SendSettingPri.matches("[0-9]")) {
            Toast.makeText(this, "PRIORITY FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939SendSettingMsgData = mEtxtJ1939SendSettingMsgData.getText().toString().toUpperCase();
        if (mJ1939SendSettingMsgData.length() % 2 != 0 ||
                !mJ1939SendSettingMsgData.matches("[0-9a-fA-F]+") ||
                mJ1939SendSettingMsgData.length() > MRM_CONSTANTS.VCIL_MAX_J1939_DATA_SIZE * 2) {
            Toast.makeText(this, "DATA FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        byte[] tempData = HexConverter.hexStringToByteArray(mJ1939SendSettingMsgData);

        byte tempLen = (byte) Integer.parseInt(mEtxtJ1939SendSettingMsgLength.getText().toString());
        if (tempLen < 0 || tempLen > tempData.length) {
            Toast.makeText(this, String.format("LENGTH VALUE ERROR. MUST LESS THEN INPUT DATA LENGTH(%d).", tempData.length), Toast.LENGTH_LONG).show();
            return;
        }


        VCIL_J1939_MESSAGE msgObj = new VCIL_J1939_MESSAGE();
        msgObj.port = (byte) mSelectedJ1939SendSettingPortId;
        msgObj.pgn = Integer.parseInt(mJ1939SendSettingPgn, 16);
        msgObj.source = (byte) Integer.parseInt(mJ1939SendSettingSrc, 16);
        msgObj.destination = (byte) Integer.parseInt(mJ1939SendSettingDst, 16);
        msgObj.priority = (byte) Integer.parseInt(mJ1939SendSettingPri);
        msgObj.data = new byte[MRM_CONSTANTS.VCIL_MAX_J1939_DATA_SIZE];
        for (int i = 0; i < msgObj.data.length; i++) {
            msgObj.data[i] = 0;
        }
        System.arraycopy(tempData, 0, msgObj.data, 0, tempData.length);
        msgObj.length = tempLen;

        ret = sendJ1939Msg(msgObj);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Send OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Send ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    static private int operationReadJ1939Msg(J1939DemoActivity activity) {
        int readRet;
        VCIL_J1939_MESSAGE receivedMsg = new VCIL_J1939_MESSAGE();

        readRet = activity.readJ1939Msg(receivedMsg);

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
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            //Read error.Do nothing
            Toast.makeText(activity, String.format("Read message Failed. (0x%04X)",readRet), Toast.LENGTH_SHORT).show();
        }
        return readRet;
    }


    //*
    static private int operationReadJ1939MsgMulti(J1939DemoActivity activity) {
        int readRet;

        ArrayList<VCIL_J1939_MESSAGE> receivedMsgList = new ArrayList<VCIL_J1939_MESSAGE>();
        int desiredReaedNum = 300; //The max number of messages to be read
        int[] resultReadNum = new int[1]; //Actual number of gotten messages


        readRet = activity.readJ1939MsgMulti(receivedMsgList, desiredReaedNum, resultReadNum);

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
    //*/

    private int getJ1939BusSpeed(byte port, int[] j1939SpeedID, int[] mode) {
        int ret;
        ret = mVcilAPI.vcil_can_get_speed(port, j1939SpeedID, mode);
        return ret;
    }

    private int setJ1939PortSpeedNormalMode(byte port, int j1939SpeedID) {
        int ret;
        ret = mVcilAPI.vcil_can_set_speed(port, j1939SpeedID);
        return ret;
    }

    private int setJ1939PortSpeedListenMode(byte port, int j1939SpeedID) {
        int ret;
        ret = mVcilAPI.vcil_can_set_speed_listen_mode(port, j1939SpeedID);
        return ret;
    }

    private int sendJ1939Msg(VCIL_J1939_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_j1939_write(msgObj);
        return ret;
    }


    private int readJ1939Msg(VCIL_J1939_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_j1939_read(msgObj);
        return ret;
    }

    //*
    private int readJ1939MsgMulti(ArrayList<VCIL_J1939_MESSAGE> msgObjList, int desiredNum, int[] resultReadNum) {
        int ret;
        ret = mVcilAPI.vcil_j1939_read_multi(msgObjList, desiredNum, resultReadNum);
        return ret;
    }
    //*/
}
