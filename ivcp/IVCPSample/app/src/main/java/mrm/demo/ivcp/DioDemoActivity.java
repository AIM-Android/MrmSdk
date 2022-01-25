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

public class DioDemoActivity extends Activity {

    String TAG = "SDKv4 IVCP DEMO - DIO";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    int[] mDioPinIDList = new int[] {
            MRM_ENUM.IVCP_DIO_PIN_ID.IVCP_DIGITAL_PIN0.getValue(),
            MRM_ENUM.IVCP_DIO_PIN_ID.IVCP_DIGITAL_PIN1.getValue(),
            MRM_ENUM.IVCP_DIO_PIN_ID.IVCP_DIGITAL_PIN2.getValue(),
            MRM_ENUM.IVCP_DIO_PIN_ID.IVCP_DIGITAL_PIN3.getValue(),
            MRM_ENUM.IVCP_DIO_PIN_ID.IVCP_DIGITAL_PIN4.getValue(),
            MRM_ENUM.IVCP_DIO_PIN_ID.IVCP_DIGITAL_PIN5.getValue()
    };
    ArrayList<DIStatus> mDIStatusList;



    class DIStatus {
        public int      pinID = 0;
        public boolean  wakeupStatus = false;
        public boolean  status = false;

        public DIStatus(int pID, boolean stat, boolean wstat) {
            pinID        = pID;
            wakeupStatus = wstat;
            status       = stat;
        }
    }


    ArrayList<DOStatus> mDOStatusList;
    class DOStatus {
        public int      pinID = 0;
        public boolean  status = false;

        public DOStatus(int pID, boolean stat) {
            pinID        = pID;
            status       = stat;
        }
    }

    ProgressDialog mProgressDailog;

    TextView mTxtDiWakeupStatusOperationResult;
    CheckBox[] mCbDIWakeupStatusList;
    Button mBtnGetDiWakeupStatus, mBtnSetDiWakeupStatus;
    String mDiWakeupStatusOperationResult = "";

    LinearLayout mLytDiNum;
    TextView mTxtDiNum;
    String mDiNum = "";

    LinearLayout mLytDiStatusOperationResult;
    TextView mTxtDiStatusOperationResult;
    CheckBox[] mCbDIStatusList;
    String mDiStatusOperationResult = "";

    LinearLayout mLytDoNum;
    TextView mTxtDoNum;
    String mDoNum  = "";

    LinearLayout mLytDoStatusOperationResult;
    TextView mTxtDoStatusOperationResult;
    CheckBox[] mCbDOStatusList;
    Button mBtnGetDoStatus, mBtnSetDoStatus;
    String mDoStatusOperationResult = "";

    LinearLayout mLytDiTypeOperationResult;
    TextView mTxtDiTypeOperationResult;
    Spinner mSpnDiType;
    BaseAdapter mDiTypeSpinnerDataAdapter;
    ArrayList<SpinnerItem> mDiTypeSpinnerItemList;
    Button mBtnGetDiType, mBtnSetDiType;
    String mDiTypeOperationResult = "";
    int mSelectedDiType;

    LinearLayout mLytDiPinTypeOperationResult;
    TextView mTxtDiPinTypeOperationResult;
    Spinner mSpnDiPinType;
    BaseAdapter mDiPinTypeSpinnerDataAdapter;
    Button mBtnGetDiPinType, mBtnSetDiPinType;
    ArrayList<SpinnerItem> mDiPinTypeSpinnerItemList;
    String mDiPinTypeOperationResult = "";
    int mSelectedDiPinType;

    TextView mTxtDiPinOperationResult;
    Spinner mSpnDiPin;
    BaseAdapter mDiPinSpinnerDataAdapter;
    ArrayList<SpinnerItem> mDiPinSpinnerItemList;
    String mDiPinOperationResult = "";
    int mSelectedDiPin;




    class SpinnerItem {
        public String displayStr;
        public int    value;

        public SpinnerItem(String str, int val) {
            displayStr = str;
            value = val;
        }
    }


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;
    private static class MyHandler extends Handler {
        private WeakReference<DioDemoActivity> mActivity = null;

        public MyHandler(DioDemoActivity activity) {
            mActivity = new WeakReference<DioDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DioDemoActivity activity = mActivity.get();

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

        initDioStatusList();
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


    private void initDioStatusList() {
        mDIStatusList = new ArrayList<>();
        for(int i=0; i < mDioPinIDList.length ; i++) {
            mDIStatusList.add( new DIStatus(mDioPinIDList[i], false, false ) ); // DIx  - mDIStatusList[x]
        }

        mDOStatusList = new ArrayList<>();
        for(int i=0; i < mDioPinIDList.length ; i++) {
            mDOStatusList.add( new DOStatus(mDioPinIDList[i], false) ); // DOx  - mDOStatusList[x]
        }
    }


    private void initView() {
        setContentView(R.layout.activity_dio_demo);

        mProgressDailog = new ProgressDialog(this);

        mCbDIWakeupStatusList = new CheckBox[mDIStatusList.size()];
        mCbDIStatusList       = new CheckBox[mDIStatusList.size()];
        mCbDOStatusList       = new CheckBox[mDOStatusList.size()];

        mTxtDiWakeupStatusOperationResult = (TextView) findViewById(R.id.txt_di_wakeup_status_operation_result);
        mCbDIWakeupStatusList[0]          = (CheckBox) findViewById(R.id.cb_wakeup_di1);
        mCbDIWakeupStatusList[1]          = (CheckBox) findViewById(R.id.cb_wakeup_di2);
        mCbDIWakeupStatusList[2]          = (CheckBox) findViewById(R.id.cb_wakeup_di3);
        mCbDIWakeupStatusList[3]          = (CheckBox) findViewById(R.id.cb_wakeup_di4);
        mCbDIWakeupStatusList[4]          = (CheckBox) findViewById(R.id.cb_wakeup_di5);
        mCbDIWakeupStatusList[5]          = (CheckBox) findViewById(R.id.cb_wakeup_di6);
        mBtnGetDiWakeupStatus             = (Button)   findViewById(R.id.btn_get_di_wakeup_status);
        mBtnSetDiWakeupStatus             = (Button)   findViewById(R.id.btn_set_di_wakeup_status);

        mLytDiNum                   = (LinearLayout) findViewById(R.id.row_di_number);
        mTxtDiNum                   = (TextView) findViewById(R.id.txt_di_num);

        mLytDiStatusOperationResult = (LinearLayout) findViewById(R.id.row_di_status);
        mTxtDiStatusOperationResult = (TextView) findViewById(R.id.txt_di_status_operation_result);
        mCbDIStatusList[0]          = (CheckBox) findViewById(R.id.cb_di1);
        mCbDIStatusList[1]          = (CheckBox) findViewById(R.id.cb_di2);
        mCbDIStatusList[2]          = (CheckBox) findViewById(R.id.cb_di3);
        mCbDIStatusList[3]          = (CheckBox) findViewById(R.id.cb_di4);
        mCbDIStatusList[4]          = (CheckBox) findViewById(R.id.cb_di5);
        mCbDIStatusList[5]          = (CheckBox) findViewById(R.id.cb_di6);
        for( CheckBox cb : mCbDIStatusList) {
            cb.setEnabled(false);
        }

        mLytDoNum                   = (LinearLayout) findViewById(R.id.row_do_number);
        mTxtDoNum                   = (TextView) findViewById(R.id.txt_do_num);

        mLytDoStatusOperationResult = (LinearLayout) findViewById(R.id.row_do_status);
        mTxtDoStatusOperationResult = (TextView) findViewById(R.id.txt_do_status_operation_result);
        mCbDOStatusList[0]          = (CheckBox) findViewById(R.id.cb_do1);
        mCbDOStatusList[1]          = (CheckBox) findViewById(R.id.cb_do2);
        mCbDOStatusList[2]          = (CheckBox) findViewById(R.id.cb_do3);
        mCbDOStatusList[3]          = (CheckBox) findViewById(R.id.cb_do4);
        mCbDOStatusList[4]          = (CheckBox) findViewById(R.id.cb_do5);
        mCbDOStatusList[5]          = (CheckBox) findViewById(R.id.cb_do6);
        mBtnGetDoStatus             = (Button)   findViewById(R.id.btn_get_do_status);
        mBtnSetDoStatus             = (Button)   findViewById(R.id.btn_set_do_status);

        mLytDiTypeOperationResult = (LinearLayout) findViewById(R.id.row_di_type);
        mTxtDiTypeOperationResult = (TextView) findViewById(R.id.txt_di_type_operation_result);
        mSpnDiType                = (Spinner)  findViewById(R.id.spn_di_type);
        mBtnGetDiType             = (Button)   findViewById(R.id.btn_get_di_type);
        mBtnSetDiType             = (Button)   findViewById(R.id.btn_set_di_type);

        mLytDiPinTypeOperationResult = (LinearLayout) findViewById(R.id.row_di_pin_type);
        mTxtDiPinTypeOperationResult = (TextView) findViewById(R.id.txt_di_pin_type_operation_result);
        mSpnDiPinType                = (Spinner)  findViewById(R.id.spn_di_pin_type);
        mBtnGetDiPinType             = (Button)   findViewById(R.id.btn_get_di_pin_type);
        mBtnSetDiPinType             = (Button)   findViewById(R.id.btn_set_di_pin_type);

        mTxtDiPinOperationResult     = (TextView) findViewById(R.id.txt_di_pin_operation_result);
        mSpnDiPin                    = (Spinner)  findViewById(R.id.spn_di_pin);



        setDiTypeSpinnerItemList();
        mDiTypeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mDiTypeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mDiTypeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mDiTypeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(15.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnDiType.setAdapter(mDiTypeSpinnerDataAdapter);

        setDiPinTypeSpinnerItemList();
        mDiPinTypeSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mDiPinTypeSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mDiPinTypeSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mDiPinTypeSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(15.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnDiPinType.setAdapter(mDiPinTypeSpinnerDataAdapter);

        setDiPinSpinnerItemList();
        mDiPinSpinnerDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mDiPinSpinnerItemList.size();
            }

            @Override
            public Object getItem(int position) {
                return mDiPinSpinnerItemList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                SpinnerItem currentItem = mDiPinSpinnerItemList.get(position);
                TextView textView = new TextView (getApplicationContext ());
                textView.setTextSize(15.0f);
                textView.setPadding(5, 10, 0, 10);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor (Color.BLACK);
                textView.setText (currentItem.displayStr);
                return textView;
            }
        };
        mSpnDiPin.setAdapter(mDiPinSpinnerDataAdapter);

        cleanAllFields();
    }

    private void setDiTypeSpinnerItemList() {
        mDiTypeSpinnerItemList = new ArrayList<SpinnerItem>();
        mDiTypeSpinnerItemList.clear();
        mDiTypeSpinnerItemList.add(new SpinnerItem("IVCP_DIO_INPUT_TYPE_WET_CONTACT", MRM_ENUM.IVCP_DIO_INPUT_TYPE.IVCP_DIO_INPUT_TYPE_WET_CONTACT.getValue()));
        mDiTypeSpinnerItemList.add(new SpinnerItem("IVCP_DIO_INPUT_TYPE_DRY_CONTACT", MRM_ENUM.IVCP_DIO_INPUT_TYPE.IVCP_DIO_INPUT_TYPE_DRY_CONTACT.getValue()));
    }

    private void setDiPinTypeSpinnerItemList() {
        mDiPinTypeSpinnerItemList = new ArrayList<SpinnerItem>();
        mDiPinTypeSpinnerItemList.clear();
        mDiPinTypeSpinnerItemList.add(new SpinnerItem("IVCP_DIO_INPUT_TYPE_WET_CONTACT", MRM_ENUM.IVCP_DIO_INPUT_TYPE.IVCP_DIO_INPUT_TYPE_WET_CONTACT.getValue()));
        mDiPinTypeSpinnerItemList.add(new SpinnerItem("IVCP_DIO_INPUT_TYPE_DRY_CONTACT", MRM_ENUM.IVCP_DIO_INPUT_TYPE.IVCP_DIO_INPUT_TYPE_DRY_CONTACT.getValue()));
    }

    private void setDiPinSpinnerItemList() {
        mDiPinSpinnerItemList = new ArrayList<SpinnerItem>();
        mDiPinSpinnerItemList.clear();
        mDiPinSpinnerItemList.add(new SpinnerItem("DI 1", 0));
        mDiPinSpinnerItemList.add(new SpinnerItem("DI 2", 1));
        mDiPinSpinnerItemList.add(new SpinnerItem("DI 3", 2));
        mDiPinSpinnerItemList.add(new SpinnerItem("DI 4", 3));
        mDiPinSpinnerItemList.add(new SpinnerItem("DI 5", 4));
        mDiPinSpinnerItemList.add(new SpinnerItem("DI 6", 5));
    }

    private void initListener() {
        mBtnGetDiWakeupStatus.setOnClickListener(myOnClickListener);
        mBtnSetDiWakeupStatus.setOnClickListener(myOnClickListener);
        mBtnGetDoStatus.setOnClickListener(myOnClickListener);
        mBtnSetDoStatus.setOnClickListener(myOnClickListener);
        mSpnDiType.setOnItemSelectedListener(mSpnDiTypeOnItemSelectedListener);
        mSpnDiPinType.setOnItemSelectedListener(mSpnDiPinTypeOnItemSelectedListener);
        mSpnDiPin.setOnItemSelectedListener(mSpnDiPinOnItemSelectedListener);
        mBtnGetDiType.setOnClickListener(myOnClickListener);
        mBtnSetDiType.setOnClickListener(myOnClickListener);
        mBtnGetDiPinType.setOnClickListener(myOnClickListener);
        mBtnSetDiPinType.setOnClickListener(myOnClickListener);
    }

    View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btn_get_di_wakeup_status:
                    AsyncTask<Void, Void, Void> asyncGetAllDiWakeupStatus = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            mProgressDailog.setCancelable(false);
                            mProgressDailog.setMessage("Getting DI Wakeup Status ...");
                            mProgressDailog.show();
                        }

                        @Override
                        protected Void doInBackground(Void... params) {
                            operationGetAllDiWakeupStatus();
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            mProgressDailog.cancel();
                        }
                    };
                    asyncGetAllDiWakeupStatus.execute();

                    break;

                case R.id.btn_set_di_wakeup_status:
                    AsyncTask<Void, Void, Void> asyncSetAllDiWakeupStatus = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            mProgressDailog.setCancelable(false);
                            mProgressDailog.setMessage("Setting DI Wakeup Status ...");
                            mProgressDailog.show();
                        }

                        @Override
                        protected Void doInBackground(Void... params) {
                            operationSetAllDiWakeupStatus();
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            mProgressDailog.cancel();
                        }
                    };
                    asyncSetAllDiWakeupStatus.execute();
                    break;

                case R.id.btn_get_do_status:
                    operationGetAllDoStatus();
                    break;

                case R.id.btn_set_do_status:
                    operationSetAllDoStatus();
                    break;

                case R.id.btn_get_di_type:
                    operationGetDiType();
                    break;

                case R.id.btn_set_di_type:
                    operationSetDiType(mSelectedDiType);
                    break;

                case R.id.btn_get_di_pin_type:
                    operationGetDiPinType(mSelectedDiPin);
                    break;

                case R.id.btn_set_di_pin_type:
                    operationSetDiPinType(mSelectedDiPin, mSelectedDiPinType);
                    break;
                default:
                    break;
            }
        }
    };

    AdapterView.OnItemSelectedListener mSpnDiTypeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedDiType = mDiTypeSpinnerItemList.get(position).value;
            log("Seleted item " + position + ", value = " + mSelectedDiType);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    AdapterView.OnItemSelectedListener mSpnDiPinTypeOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedDiPinType = mDiPinTypeSpinnerItemList.get(position).value;
            log("DI Pin Type Seleted item " + position + ", value = " + mSelectedDiPinType);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    AdapterView.OnItemSelectedListener mSpnDiPinOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mSelectedDiPin = mDiPinSpinnerItemList.get(position).value;
            log("DI Pin Seleted item " + position + ", value = " + mSelectedDiPin);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    void updateView(int id) {
        switch( id ) {
            case R.id.row_di_wakeup:
                if (mTxtDiWakeupStatusOperationResult != null)
                    ViewOperator.setLabelText(mTxtDiWakeupStatusOperationResult, mDiWakeupStatusOperationResult);

                for (int i = 0; i < mCbDIWakeupStatusList.length; i++) {
                    if (mCbDIWakeupStatusList[i] != null) {
                        mCbDIWakeupStatusList[i].setChecked(mDIStatusList.get(i).wakeupStatus);
                    }
                }
                break;

            case R.id.txt_di_num:
                if (mDiNum.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))
                        || mDiNum.equals("0")) {
                    mLytDiNum.setVisibility(View.GONE);
                    mLytDiStatusOperationResult.setVisibility(View.GONE);
                } else {
                    if (mTxtDiNum != null)
                        ViewOperator.setLabelText(mTxtDiNum, mDiNum);
                }
                break;

            case R.id.row_di_status:
                if (mTxtDiStatusOperationResult != null)
                    ViewOperator.setLabelText(mTxtDiStatusOperationResult, mDiStatusOperationResult);

                for (int i = 0; i < mCbDIStatusList.length; i++) {
                    if (mCbDIStatusList[i] != null) {
                        mCbDIStatusList[i].setChecked(mDIStatusList.get(i).status);
                    }
                }
                break;

            case R.id.txt_do_num:
                if (mDoNum.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))
                        || mDoNum.equals("0")) {
                    mLytDoNum.setVisibility(View.GONE);
                    mLytDoStatusOperationResult.setVisibility(View.GONE);
                } else {
                    if (mTxtDoNum != null)
                        ViewOperator.setLabelText(mTxtDoNum, mDoNum);
                }
                break;

            case R.id.row_do_status:
                if (mTxtDoStatusOperationResult != null)
                    ViewOperator.setLabelText(mTxtDoStatusOperationResult, mDoStatusOperationResult);

                for (int i = 0; i < mCbDOStatusList.length; i++) {
                    if (mCbDOStatusList[i] != null) {
                        mCbDOStatusList[i].setChecked(mDOStatusList.get(i).status);
                    }
                }
                break;

            case R.id.row_di_type:
                if (mDiTypeOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytDiTypeOperationResult.setVisibility(View.GONE);
                } else if(mDiTypeOperationResult.contains("ERR")) {
                    if (mTxtDiTypeOperationResult != null) {
                        mTxtDiTypeOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtDiTypeOperationResult, mDiTypeOperationResult);
                    }
                } else {
                    boolean foundDiTypeItem = false;
                    for(int i = 0 ; i < mDiTypeSpinnerItemList.size() ; i++) {
                        if( mSelectedDiType == mDiTypeSpinnerItemList.get(i).value ) {
                            mSpnDiType.setSelection(i);
                            foundDiTypeItem = true;
                        }
                    }
                    if( foundDiTypeItem ) {
                        mTxtDiTypeOperationResult.setVisibility(View.GONE);
                    } else {
                        mDiTypeOperationResult = String.format("Get unknown DI type( %d )", mSelectedDiType);
                        mTxtDiTypeOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtDiTypeOperationResult, mDiTypeOperationResult);
                    }
                }
                break;

            case R.id.row_di_pin_type:
                if (mDiPinTypeOperationResult.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytDiPinTypeOperationResult.setVisibility(View.GONE);
                } else if(mDiPinTypeOperationResult.contains("ERR")) {
                    if (mTxtDiPinTypeOperationResult != null) {
                        mTxtDiPinTypeOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtDiPinTypeOperationResult, mDiPinTypeOperationResult);
                    }
                } else {
                    boolean foundDiTypeItem = false;
                    boolean foundDiItem = false;
                    for(int i = 0 ; i < mDiPinTypeSpinnerItemList.size() ; i++) {
                        if( mSelectedDiPinType == mDiPinTypeSpinnerItemList.get(i).value ) {
                            mSpnDiPinType.setSelection(i);
                            foundDiTypeItem = true;
                        }
                    }
                    for(int i = 0 ; i < mDiPinSpinnerItemList.size() ; i++) {
                        if( mSelectedDiPin == mDiPinSpinnerItemList.get(i).value ) {
                            mSpnDiPin.setSelection(i);
                            foundDiItem = true;
                        }
                    }

                    if( foundDiTypeItem && foundDiItem) {
                        mTxtDiPinTypeOperationResult.setVisibility(View.GONE);
                    } else {
                        mDiPinTypeOperationResult = String.format("Get unknown DI pin type( %d )", mSelectedDiPinType);
                        mTxtDiPinTypeOperationResult.setVisibility(View.VISIBLE);
                        ViewOperator.setLabelText(mTxtDiPinTypeOperationResult, mDiPinTypeOperationResult);
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

        ViewOperator.setLabelText(mTxtDiNum, DEFAULT_DISPLAY_VALUE_NA);
        ViewOperator.setLabelText(mTxtDoNum, DEFAULT_DISPLAY_VALUE_NA);
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
                operationGetAllDiWakeupStatus();
                operationGetDiNum();
                operationGetAllDiStatus();
                operationGetDoNum();
                operationGetAllDoStatus();
                operationGetDiType();
                operationGetDiPinType(mSelectedDiPin);
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
                operationGetAllDiStatus();

                String errorStr = "ERR";
                if( mDiStatusOperationResult.contains(errorStr) ) {
                    log("DiStatusOperationResult ERROR code : "+ mDiStatusOperationResult);
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





    private void operationGetAllDiWakeupStatus() {
        String displayResult = "";

        String failedResult = "\nFailed DI: " + "    ERR - ";
        String failedResultItemFormat = "DI%d(0x%08X), "; //ex: "DI1(0x00000002), "
        int failCount = 0;

        String unsupportResult = "\nUnsupported DI: " + "    ";
        String unsupportResultItemFormat = "DI%d, "; //ex: "DI1, "
        int invalidCount = 0;

        for( int i=0; i < mDIStatusList.size(); i++ ) {
            int ret;
            boolean[] wakeupStat = new boolean[1];

            ret = getDiWakeupStatus(mDIStatusList.get(i).pinID, wakeupStat);

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                mDIStatusList.get(i).wakeupStatus = wakeupStat[0];

            } else if(ret == ErrorCode.MRM_ERR_UNSUPPORT_OPERATION) {
                unsupportResult += String.format(unsupportResultItemFormat, i+1 );
                invalidCount++;

            } else {
                failedResult += String.format(failedResultItemFormat, i+1, ret );
                failCount++;
            }
        }

        if( failCount == 0 ){
            displayResult = "Get OK";

        } else {
            displayResult = "Get Error.";
        }

        displayResult +=
                (invalidCount != 0 ? unsupportResult : "" ) +
                (failCount    != 0 ? failedResult  : "" ) ;

        mDiWakeupStatusOperationResult = displayResult;

        mHandler.sendEmptyMessage(R.id.row_di_wakeup);
    }


    private void operationSetAllDiWakeupStatus() {
        String displayResult;

        String failedResult = "\nFailed DI: " + "    ERR - ";
        String failedResultItemFormat = "DI%d(0x%08X), "; //ex: "DI1(0x00000002), "
        int failCount = 0;

        String unsupportResult = "\nUnsupported DI: " + "    ";
        String unsupportResultItemFormat = "DI%d, "; //ex: "DI1, "
        int invalidCount = 0;

        for( int i=0; i < mDIStatusList.size(); i++ ) {
            int ret;

            ret = setDiWakeupStatus(mDIStatusList.get(i).pinID, mCbDIWakeupStatusList[i].isChecked());

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                mDIStatusList.get(i).wakeupStatus = mCbDIWakeupStatusList[i].isChecked();

            } else if(ret == ErrorCode.MRM_ERR_UNSUPPORT_OPERATION) {
                unsupportResult += String.format(unsupportResultItemFormat, i+1 );
                invalidCount++;

            } else {
                failedResult += String.format(failedResultItemFormat, i+1, ret );
                failCount++;
            }
        }

        if( failCount == 0 ){
            displayResult = "Set OK";
        } else {
            displayResult = "Set Error.";
        }
        displayResult +=
                (invalidCount != 0 ? unsupportResult : "" ) +
                        (failCount    != 0 ? failedResult  : "" ) ;
        mDiWakeupStatusOperationResult = displayResult;

        mHandler.sendEmptyMessage(R.id.row_di_wakeup);
    }

    private void operationGetDiNum() {
        mDiNum = getDiNumber();
        mHandler.sendEmptyMessage(R.id.txt_di_num);
    }

    private void operationGetAllDiStatus() {
        String displayResult = "";
        int ret;
        int[] allStat = new int[1];

        if(mDiNum.equals(""))
            mDiNum = getDiNumber();

        ret = getDiAllStatus(allStat);
        if(ret != ErrorCode.MRM_ERR_NO_ERROR) {
            displayResult = "Get error. " + ErrorCode.errorCodeToString(ret);
            for(int i=0;i<mDIStatusList.size();i++) {
                mDIStatusList.get(i).status = false;
            }
        } else {
            displayResult = "Get OK " + "\nUnsupported DI: " + "    ";
            for(int i=0;i<mDIStatusList.size();i++) {
                mDIStatusList.get(i).status = (allStat[0] >> i & 0x01) != 0;
                if( (i+1) > Integer.valueOf(mDiNum) )
                    displayResult = displayResult + "DI" + (i+1) + ", ";
            }
        }
        mDiStatusOperationResult = displayResult;

        mHandler.sendEmptyMessage(R.id.row_di_status);
    }

    private void operationGetDoNum() {
        mDoNum = getDoNumber();
        mHandler.sendEmptyMessage(R.id.txt_do_num);
    }

    private void operationGetAllDiStatusByID() {
        String displayResult = "";

        String failedResult = "\nFailed DI: " + "    ERR - ";
        String failedResultItemFormat = "DI%d(0x%08X), "; //ex: "DI1(0x00000002), "
        int failCount = 0;

        String unsupportResult = "\nUnsupported DI: " + "    ";
        String unsupportResultItemFormat = "DI%d, "; //ex: "DI1, "
        int invalidCount = 0;

        for( int i=0; i < mDIStatusList.size(); i++ ) {
            int ret;
            boolean[] stat = new boolean[1];

            ret = getDiStatus(mDIStatusList.get(i).pinID, stat);

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                mDIStatusList.get(i).status = stat[0];

            } else if(ret == ErrorCode.MRM_ERR_UNSUPPORT_OPERATION) {
                unsupportResult += String.format(unsupportResultItemFormat, i+1 );
                invalidCount++;

            } else {
                failedResult += String.format(failedResultItemFormat, i+1, ret );
                failCount++;
            }
        }

        if( failCount == 0 ){
            displayResult = "Get OK";

        } else {
            displayResult = "Get Error.";
        }

        displayResult +=
                (invalidCount != 0 ? unsupportResult : "" ) +
                        (failCount    != 0 ? failedResult  : "" ) ;

        mDiStatusOperationResult = displayResult;

        mHandler.sendEmptyMessage(R.id.row_di_status);
    }

    private void operationGetAllDoStatus() {
        String displayResult = "";

        String failedResult = "\nFailed DO: " + "    ERR - ";
        String failedResultItemFormat = "DO%d(0x%08X), "; //ex: "DO1(0000000X), "
        int failCount = 0;

        String unsupportResult = "\nUnsupported DO: " + "    ";
        String unsupportResultItemFormat = "DO%d, "; //ex: "DO1, "
        int invalidCount = 0;

        for( int i=0; i < mDOStatusList.size(); i++ ) {
            int ret;
            boolean[] stat = new boolean[1];

            ret = getDoStatus(mDOStatusList.get(i).pinID, stat);

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                mDOStatusList.get(i).status = stat[0];

            } else if(ret == ErrorCode.MRM_ERR_UNSUPPORT_OPERATION) {
                unsupportResult += String.format(unsupportResultItemFormat, i+1 );
                invalidCount++;

            } else {
                failedResult += String.format(failedResultItemFormat, i+1, ret );
                failCount++;
            }
        }

        if( failCount == 0 ){
            displayResult = "Get OK";

        } else {
            displayResult = "Get Error.";
        }

        displayResult +=
                (invalidCount != 0 ? unsupportResult : "" ) +
                (failCount    != 0 ? failedResult  : "" ) ;

        mDoStatusOperationResult = displayResult;

        mHandler.sendEmptyMessage(R.id.row_do_status);
    }

    private void operationSetAllDoStatus() {
        String displayResult;

        String failedResult = "\nFailed DO: " + "    ERR - ";
        String failedResultItemFormat = "DO%d(0x%08X), "; //ex: "DO1(0x00000002), "
        int failCount = 0;

        String unsupportResult = "\nUnsupported DO: " + "    ";
        String unsupportResultItemFormat = "DO%d, "; //ex: "DO1, "
        int invalidCount = 0;

        for( int i=0; i < mDOStatusList.size(); i++ ) {
            int ret;

            ret = setDoStatus(mDOStatusList.get(i).pinID, mCbDOStatusList[i].isChecked());

            if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
                mDOStatusList.get(i).status = mCbDOStatusList[i].isChecked();

            } else if(ret == ErrorCode.MRM_ERR_UNSUPPORT_OPERATION) {
                unsupportResult += String.format(unsupportResultItemFormat, i+1 );
                invalidCount++;

            } else {
                failedResult += String.format(failedResultItemFormat, i+1, ret );
                failCount++;
            }
        }

        if( failCount == 0 ){
            displayResult = "Set OK";
        } else {
            displayResult = "Set Error.";
        }
        displayResult +=
                (invalidCount != 0 ? unsupportResult : "" ) +
                        (failCount    != 0 ? failedResult  : "" ) ;
        mDoStatusOperationResult = displayResult;

        mHandler.sendEmptyMessage(R.id.row_do_status);
    }

    private void operationGetDiType() {
        int ret;
        int[] tempType = new int[1];
        ret = getDiType(tempType);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mDiTypeOperationResult = "Get OK";
            mSelectedDiType = tempType[0];
        } else {
            mDiTypeOperationResult = "Get error.\n" + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.row_di_type);
    }

    private void operationSetDiType(int type) {
        int ret;

        ret = setDiType(type);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mDiTypeOperationResult = String.format("Set OK. DI Type( %d )", type);
            mSelectedDiType = type;

        } else {
            mDiTypeOperationResult = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }

        Toast.makeText(DioDemoActivity.this, mDiTypeOperationResult, Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessage(R.id.row_di_type);
    }

    private void operationGetDiPinType(int pinID) {
        int ret;
        int[] tempType = new int[1];
        ret = getDiPinType(pinID, tempType);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mDiPinTypeOperationResult = "Get OK";
            mSelectedDiPinType = tempType[0];
        } else {
            mDiPinTypeOperationResult = "Get error.\n" + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.row_di_pin_type);
    }

    private void operationSetDiPinType(int pinID, int type) {
        int ret;
        ret = setDiPinType(pinID, type);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mDiPinTypeOperationResult = String.format("Set OK. DI %d Type( %d )", pinID + 1 , type);
            mSelectedDiPinType = type;
        } else {
            mDiPinTypeOperationResult = "Set error.\n" + ErrorCode.errorCodeToString(ret);
        }

        Toast.makeText(DioDemoActivity.this, mDiPinTypeOperationResult, Toast.LENGTH_SHORT).show();
        mHandler.sendEmptyMessage(R.id.row_di_pin_type);
    }

    private int setDiWakeupStatus(int pinID, boolean enable) {
        int ret;

        if(enable)
            ret = mIvcpAPI.ivcp_dio_di_wakeup_enable(pinID);
        else
            ret = mIvcpAPI.ivcp_dio_di_wakeup_disable(pinID);

        return ret;
    }

    private int getDiWakeupStatus(int diID, boolean[] wakeupStatus) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_get_di_wakeup_status(diID, wakeupStatus);
        return ret;
    }

    private String getDiNumber() {
        int ret;
        byte[] diNum = new byte[1];
        String diNumStr;
        ret = mIvcpAPI.ivcp_dio_get_input_number(diNum);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            diNumStr = String.valueOf(diNum[0]);
        } else {
            diNumStr = ErrorCode.errorCodeToString(ret);
        }

        return diNumStr;
    }

    private int getDiStatus(int pinID, boolean[] stat) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_read_input(pinID, stat);
        return ret;
    }

    private int getDiAllStatus(int[] stat) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_read_input_multiple(stat);
        return ret;
    }

    private String getDoNumber() {
        int ret;
        byte[] doNum = new byte[1];
        String doNumStr;
        ret = mIvcpAPI.ivcp_dio_get_output_number(doNum);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            doNumStr = String.valueOf(doNum[0]);
        } else {
            doNumStr = ErrorCode.errorCodeToString(ret);
        }

        return doNumStr;
    }

    private int getDoStatus(int pinID, boolean[] stat) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_read_output(pinID, stat);
        return ret;
    }

    private int setDoStatus(int pinID, boolean enable) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_write_output(pinID, enable);
        return ret;
    }

    private int getDiType(int[] type) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_get_input_type(type);
        return ret;
    }

    private int setDiType(int type) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_set_input_type(type);
        return ret;
    }

    private int getDiPinType(int pinID, int[] type) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_get_pin_input_type(pinID, type);
        return ret;
    }

    private int setDiPinType(int pinID, int type) {
        int ret;
        ret = mIvcpAPI.ivcp_dio_set_pin_input_type(pinID, type);
        return ret;
    }
}
