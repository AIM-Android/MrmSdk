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
import mrm.define.VCIL.VCIL_OBD2_MESSAGE;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.HexConverter;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;
import mrm.demo.util.ViewOperator;

public class Obd2DemoActivity extends Activity {

    String TAG = "SDKv4 VCIL_DEMO" + " - OBD2";


    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnObd2PortSettingObd2Port;
    SpinnerCustomAdapter mObd2PortSettingObd2PortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mObd2PortSettingObd2PortSpinnerItemList;
    byte mSelectedObd2PortSettingObd2PortId;

    Spinner mSpnObd2PortSettingObd2Speed;
    SpinnerCustomAdapter mObd2PortSettingObd2SpeedSpinnerDataAdapter;
    ArrayList<SpinnerItem> mObd2PortSettingObd2SpeedSpinnerItemList;
    int mSelectedObd2PortSettingObd2SpeedId;

    TextView mTxtObd2PortSettingObd2PortMode;

    Button mBtnObd2PortSettingGet;
    Button mBtnObd2PortSettingSetNormalMode;
    Button mBtnObd2PortSettingSetListenMode;

    Button mBtnObd2Config;

    Spinner mSpnObd2SendSettingPort;
    SpinnerCustomAdapter mObd2SendSettingPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mOBD2SendSettingPortSpinnerItemList;
    int mSelectedOBD2SendSettingPortId;

    //EditText mEtxtOBD2SendSettingTat;
    Spinner mSpnObd2SendSettingType;
    SpinnerCustomAdapter mObd2SendSettingTypeSpinnerDataAdapter;
    ArrayList<SpinnerItem> mOBD2SendSettingTypeSpinnerItemList;
    byte mSelectedOBD2SendSettingType;

    EditText mEtxtOBD2SendSettingSrc;
    String mOBD2SendSettingSrc = "";

    EditText mEtxtOBD2SendSettingDst;
    String mOBD2SendSettingDst = "";

    EditText mEtxtOBD2SendSettingPri;
    String mOBD2SendSettingPri = "";

    EditText mEtxtOBD2SendSettingMsgData;
    String mOBD2SendSettingMsgData = "";

    EditText mEtxtOBD2SendSettingMsgLength;
    String mOBD2SendSettingMsgLength = "";

    Button mBtnOBD2Send;

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
        VCIL_OBD2_MESSAGE receivedMsg = new VCIL_OBD2_MESSAGE();

        public RecvMsgRecord(long time, VCIL_OBD2_MESSAGE msgObj) {
            receivedTime = time;
            receivedMsg.copyFrom(msgObj);
        }
    }

    public static class RecvMsgStatisticRecord {
        long lastReceivedTime;
        long count;
        VCIL_OBD2_MESSAGE lastReceivedMsg = new VCIL_OBD2_MESSAGE();

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
        private WeakReference<Obd2DemoActivity> mActivity = null;

        public MyViewRefreshHandler(Obd2DemoActivity activity) {
            mActivity = new WeakReference<Obd2DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Obd2DemoActivity activity = mActivity.get();

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
        private WeakReference<Obd2DemoActivity> mActivity = null;

        public MyEventHandler(Obd2DemoActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<Obd2DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Obd2DemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            int eventID = msg.what;

            switch (eventID) {
                case MRM_CONSTANTS.VCIL_EVENT_ID_RECEIVED_MSG_OBD2:

                    activity.mVcilAPI.vcil_obd2_wait_event(false);
                    while (true) {
                        int readRet;
                        /* Read single message every time
                        readRet = operationReadOBD2Msg(activity);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadOBD2MsgMulti(activity);
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
                    activity.mVcilAPI.vcil_obd2_wait_event(true);
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

        mEventHandlerThread = new HandlerThread("OBD2EventHandlerThread");
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
        mObd2PortSettingObd2PortSpinnerItemList = new ArrayList<SpinnerItem>();
        mObd2PortSettingObd2PortSpinnerItemList.clear();
        mObd2PortSettingObd2PortSpinnerItemList.add(new SpinnerItem("0", 0));
        mObd2PortSettingObd2PortSpinnerItemList.add(new SpinnerItem("1", 1));

        mObd2PortSettingObd2SpeedSpinnerItemList = new ArrayList<SpinnerItem>();
        mObd2PortSettingObd2SpeedSpinnerItemList.clear();
        mObd2PortSettingObd2SpeedSpinnerItemList.add(new SpinnerItem("250 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_250K.getValue()));
        mObd2PortSettingObd2SpeedSpinnerItemList.add(new SpinnerItem("500 Kbit/s", MRM_ENUM.VCIL_CAN_SPEED.VCIL_CAN_SPEED_500K.getValue()));

        mOBD2SendSettingPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mOBD2SendSettingPortSpinnerItemList.clear();
        mOBD2SendSettingPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mOBD2SendSettingPortSpinnerItemList.add(new SpinnerItem("1", 1));

        mOBD2SendSettingTypeSpinnerItemList = new ArrayList<SpinnerItem>();
        mOBD2SendSettingTypeSpinnerItemList.clear();
        mOBD2SendSettingTypeSpinnerItemList.add(new SpinnerItem("VCIL_OBD2_TYPE_PHYSICAL", MRM_CONSTANTS.VCIL_OBD2_TYPE_PHYSICAL));
        mOBD2SendSettingTypeSpinnerItemList.add(new SpinnerItem("VCIL_OBD2_TYPE_FUNCTIONAL", MRM_CONSTANTS.VCIL_OBD2_TYPE_FUNCTIONAL));
    }


    private void initView() {
        setContentView(R.layout.activity_obd2_demo);

        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnObd2PortSettingObd2Port = (Spinner) findViewById(R.id.spn_obd2_port_setting_port);
        mSpnObd2PortSettingObd2Speed = (Spinner) findViewById(R.id.spn_obd2_port_setting_speed);
        mTxtObd2PortSettingObd2PortMode = (TextView) findViewById(R.id.spn_obd2_port_setting_port_mode);
        mBtnObd2PortSettingGet = (Button) findViewById(R.id.btn_obd2_port_setting_get);
        mBtnObd2PortSettingSetNormalMode = (Button) findViewById(R.id.btn_obd2_port_setting_set_normal_mode);
        mBtnObd2PortSettingSetListenMode = (Button) findViewById(R.id.btn_obd2_port_setting_set_listen_mode);

        mObd2PortSettingObd2PortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mObd2PortSettingObd2PortSpinnerItemList);
        mSpnObd2PortSettingObd2Port.setAdapter(mObd2PortSettingObd2PortSpinnerDataAdapter);
        mObd2PortSettingObd2SpeedSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mObd2PortSettingObd2SpeedSpinnerItemList);
        mSpnObd2PortSettingObd2Speed.setAdapter(mObd2PortSettingObd2SpeedSpinnerDataAdapter);

        mSpnObd2SendSettingPort = (Spinner) findViewById(R.id.spn_obd2_send_port);
        mSpnObd2SendSettingType = (Spinner) findViewById(R.id.spn_obd2_send_type);
        mEtxtOBD2SendSettingSrc = (EditText) findViewById(R.id.etxt_obd2_send_src);
        mEtxtOBD2SendSettingDst = (EditText) findViewById(R.id.etxt_obd2_send_dst);
        mEtxtOBD2SendSettingPri = (EditText) findViewById(R.id.etxt_obd2_send_pri);
        mEtxtOBD2SendSettingMsgData = (EditText) findViewById(R.id.etxt_obd2_send_data);
        mEtxtOBD2SendSettingMsgLength = (EditText) findViewById(R.id.etxt_obd2_send_length);
        mBtnOBD2Send = (Button) findViewById(R.id.btn_obd2_send);

        mObd2SendSettingPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mOBD2SendSettingPortSpinnerItemList);
        mSpnObd2SendSettingPort.setAdapter(mObd2SendSettingPortSpinnerDataAdapter);
        mSpnObd2SendSettingPort.setSelection(0);

        mObd2SendSettingTypeSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mOBD2SendSettingTypeSpinnerItemList);
        mSpnObd2SendSettingType.setAdapter(mObd2SendSettingTypeSpinnerDataAdapter);
        mSpnObd2SendSettingType.setSelection(0);

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
                    View listRow = Obd2DemoActivity.this.getLayoutInflater().inflate(R.layout.activity_obd2_demo_lst_row_obd2_received_msg, null);

                    RecvMsgStatisticRecord record = mRecvMsgStatisticRecordList.get(position);

                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_time)).setText(DateFormat.format("HH:mm:ss", record.lastReceivedTime));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_port)).setText(String.valueOf(record.lastReceivedMsg.port));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_type)).setText(String.format("%X", record.lastReceivedMsg.type));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_src)).setText(String.format("%X", record.lastReceivedMsg.source));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_dst)).setText(String.format("%X", record.lastReceivedMsg.destination));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_pri)).setText(String.format("%X", record.lastReceivedMsg.priority));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_data)).setText(HexConverter.byteArrayToHexString(record.lastReceivedMsg.data, record.lastReceivedMsg.length));
                    ((TextView) listRow.findViewById(R.id.txt_obd2_received_msg_count)).setText(String.valueOf(record.count));
                    return listRow;
                }
            }
        };
        mLstRecvMsgMsgStaticList.setAdapter(mLstRecvMsgMsgStaticListDataAdapter);
    }


    private void initListener() {
        mBtnObd2PortSettingGet.setOnClickListener(mBtnOnClickListener);
        mBtnObd2PortSettingSetNormalMode.setOnClickListener(mBtnOnClickListener);
        mBtnObd2PortSettingSetListenMode.setOnClickListener(mBtnOnClickListener);

        mBtnOBD2Send.setOnClickListener(mBtnOnClickListener);

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
                case R.id.btn_obd2_port_setting_get:
                    operationGetObd2PortSpeed();
                    break;

                case R.id.btn_obd2_port_setting_set_normal_mode:
                    operationSetObd2PortSpeedNormalMode();
                    break;

                case R.id.btn_obd2_port_setting_set_listen_mode:
                    operationSetObd2PortSpeedListenMode();
                    break;

                case R.id.btn_obd2_send:
                    operationSendOBD2Msg();
                    break;

                case R.id.btn_filter:
                    Intent in = new Intent();
                    in.setClass(Obd2DemoActivity.this, Obd2FilterDemoActivity.class);
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
                                    r.lastReceivedMsg.type == record.receivedMsg.type &&
                                    r.lastReceivedMsg.destination == record.receivedMsg.destination &&
                                    r.lastReceivedMsg.source == record.receivedMsg.source) {
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
            mVcilAPI.vcil_obd2_set_event_handler(mEventHandler);
        } else {
            mVcilAPI.vcil_obd2_unset_event_handler();
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
                        readRet = operationReadOBD2Msg(OBD2DemoActivity.this);
                        //*/

                        //* Read multiple messages every time
                        readRet = operationReadOBD2MsgMulti(Obd2DemoActivity.this);
                        //*/

                        if (readRet == ErrorCode.MRM_ERR_NO_ERROR ||
                            readRet == ErrorCode.MRM_ERR_VCIL_DATA_NOT_READY) {
                            //If message is read correctly or currently no received message,
                            //then keep reading

                        } else {
                            isRunningMsgPolling = false;
                            String errMsg = String.format("Read ERROR(%X).", readRet);
                            log(errMsg);
                            Toast.makeText(Obd2DemoActivity.this.getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    log("STOPPED POLLING..........");
                }
            });
            mThreadMsgPolling.start();
        }
    }


    private void operationGetObd2PortSpeed() {
        int ret;
        mSelectedObd2PortSettingObd2PortId = (byte) mObd2PortSettingObd2PortSpinnerItemList.get(mSpnObd2PortSettingObd2Port.getSelectedItemPosition()).value;
        int[] obd2SpeedId = new int[1];
        int[] obd2PortMode = new int[1];


        ret = getObd2BusSpeed(mSelectedObd2PortSettingObd2PortId, obd2SpeedId, obd2PortMode);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Get OK", Toast.LENGTH_LONG).show();

            //Switch speed spinner
            int position = 0;
            for(int i = 0; i < mObd2PortSettingObd2SpeedSpinnerItemList.size(); i++) {
                if (mObd2PortSettingObd2SpeedSpinnerItemList.get(i).value == obd2SpeedId[0]) {
                    position = i;
                }
            }
            mSpnObd2PortSettingObd2Speed.setSelection(position);

            //Display port mode
            if (obd2PortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_NORMAL_MODE.getValue()) {
                mTxtObd2PortSettingObd2PortMode.setText("NORMAL MODE");
            } else if (obd2PortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_LISTEN_MODE.getValue()) {
                mTxtObd2PortSettingObd2PortMode.setText("LISTEN MODE");
            } else if (obd2PortMode[0] == MRM_ENUM.VCIL_CAN_BUS_MODE.VCIL_CAN_BUS_INIT_MODE.getValue()) {
                mTxtObd2PortSettingObd2PortMode.setText("INIT MODE");
            } else {
                mTxtObd2PortSettingObd2PortMode.setText(String.format("UNKNOWN (%d)", obd2PortMode[0]));
            }

        } else {
            Toast.makeText(this, "Get ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSetObd2PortSpeedNormalMode() {
        int ret;
        mSelectedObd2PortSettingObd2PortId = (byte) mObd2PortSettingObd2PortSpinnerItemList.get(mSpnObd2PortSettingObd2Port.getSelectedItemPosition()).value;
        mSelectedObd2PortSettingObd2SpeedId = mObd2PortSettingObd2SpeedSpinnerItemList.get(mSpnObd2PortSettingObd2Speed.getSelectedItemPosition()).value;

        ret = setObd2PortSpeedNormalMode(mSelectedObd2PortSettingObd2PortId, mSelectedObd2PortSettingObd2SpeedId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSetObd2PortSpeedListenMode() {
        int ret;
        mSelectedObd2PortSettingObd2PortId = (byte) mObd2PortSettingObd2PortSpinnerItemList.get(mSpnObd2PortSettingObd2Port.getSelectedItemPosition()).value;
        mSelectedObd2PortSettingObd2SpeedId = mObd2PortSettingObd2SpeedSpinnerItemList.get(mSpnObd2PortSettingObd2Speed.getSelectedItemPosition()).value;

        ret = setObd2PortSpeedListenMode(mSelectedObd2PortSettingObd2PortId, mSelectedObd2PortSettingObd2SpeedId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationSendOBD2Msg() {
        int ret;
        mSelectedOBD2SendSettingPortId = mOBD2SendSettingPortSpinnerItemList.get(mSpnObd2SendSettingPort.getSelectedItemPosition()).value;
        mSelectedOBD2SendSettingType = (byte) mOBD2SendSettingTypeSpinnerItemList.get(mSpnObd2SendSettingType.getSelectedItemPosition()).value;

        mOBD2SendSettingSrc = mEtxtOBD2SendSettingSrc.getText().toString().toUpperCase();
        if (!mOBD2SendSettingSrc.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "SRC FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mOBD2SendSettingDst = mEtxtOBD2SendSettingDst.getText().toString().toUpperCase();
        if (!mOBD2SendSettingDst.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "DST FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mOBD2SendSettingPri = mEtxtOBD2SendSettingPri.getText().toString().toUpperCase();
        if (!mOBD2SendSettingPri.matches("[0-9]")) {
            Toast.makeText(this, "PRIORITY FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mOBD2SendSettingMsgData = mEtxtOBD2SendSettingMsgData.getText().toString().toUpperCase();
        if (mOBD2SendSettingMsgData.length() % 2 != 0 ||
                !mOBD2SendSettingMsgData.matches("[0-9a-fA-F]+") ||
                mOBD2SendSettingMsgData.length() > MRM_CONSTANTS.VCIL_MAX_OBD2_DATA_SIZE * 2) {
            Toast.makeText(this, "DATA FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        byte[] tempData = HexConverter.hexStringToByteArray(mOBD2SendSettingMsgData);

        byte tempLen = (byte) Integer.parseInt(mEtxtOBD2SendSettingMsgLength.getText().toString());
        if (tempLen < 0 || tempLen > tempData.length) {
            Toast.makeText(this, String.format("LENGTH VALUE ERROR. MUST LESS THEN INPUT DATA LENGTH(%d).", tempData.length), Toast.LENGTH_LONG).show();
            return;
        }


        VCIL_OBD2_MESSAGE msgObj = new VCIL_OBD2_MESSAGE();
        msgObj.port = (byte) mSelectedOBD2SendSettingPortId;
        msgObj.type = mSelectedOBD2SendSettingType;
        msgObj.source = (byte) Integer.parseInt(mOBD2SendSettingSrc, 16);
        msgObj.destination = (byte) Integer.parseInt(mOBD2SendSettingDst, 16);
        msgObj.priority = (byte) Integer.parseInt(mOBD2SendSettingPri);
        msgObj.data = new byte[MRM_CONSTANTS.VCIL_MAX_OBD2_DATA_SIZE];

        for (int i = 0; i < msgObj.data.length; i++) {
            msgObj.data[i] = 0;
        }
        System.arraycopy(tempData, 0, msgObj.data, 0, tempData.length);
        msgObj.length = tempLen;

        ret = sendOBD2Msg(msgObj);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Send OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Send ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    static private int operationReadOBD2Msg(Obd2DemoActivity activity) {
        int readRet;
        VCIL_OBD2_MESSAGE receivedMsg = new VCIL_OBD2_MESSAGE();

        readRet = activity.readOBD2Msg(receivedMsg);

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
            Toast.makeText(activity, String.format("Read message Failed. (0x%04X)",readRet), Toast.LENGTH_SHORT).show();
        }
        return readRet;
    }


    //*
    static private int operationReadOBD2MsgMulti(Obd2DemoActivity activity) {
        int readRet;

        ArrayList<VCIL_OBD2_MESSAGE> receivedMsgList = new ArrayList<VCIL_OBD2_MESSAGE>();
        int desiredReaedNum = 300; //The max number of messages to be read
        int[] resultReadNum = new int[1]; //Actual number of gotten messages


        readRet = activity.readOBD2MsgMulti(receivedMsgList, desiredReaedNum, resultReadNum);

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

    private int getObd2BusSpeed(byte port, int[] obd2SpeedID, int[] mode) {
        int ret;
        ret = mVcilAPI.vcil_can_get_speed(port, obd2SpeedID, mode);
        return ret;
    }

    private int setObd2PortSpeedNormalMode(byte port, int obd2SpeedID) {
        int ret;
        ret = mVcilAPI.vcil_can_set_speed(port, obd2SpeedID);
        return ret;
    }

    private int setObd2PortSpeedListenMode(byte port, int obd2SpeedID) {
        int ret;
        ret = mVcilAPI.vcil_can_set_speed_listen_mode(port, obd2SpeedID);
        return ret;
    }

    private int sendOBD2Msg(VCIL_OBD2_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_obd2_write(msgObj);
        return ret;
    }


    private int readOBD2Msg(VCIL_OBD2_MESSAGE msgObj) {
        int ret;
        ret = mVcilAPI.vcil_obd2_read(msgObj);
        return ret;
    }

    //*
    private int readOBD2MsgMulti(ArrayList<VCIL_OBD2_MESSAGE> msgObjList, int desiredNum, int[] resultReadNum) {
        int ret;
        ret = mVcilAPI.vcil_obd2_read_multi(msgObjList, desiredNum, resultReadNum);
        return ret;
    }
    //*/
}