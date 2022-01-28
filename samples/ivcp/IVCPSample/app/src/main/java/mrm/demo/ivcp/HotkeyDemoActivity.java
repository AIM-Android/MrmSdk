package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import mrm.client.IVCPServiceClient;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;


public class HotkeyDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - HK";
    private String hotKeyBrightness;

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytHotkeyKeycode;
    Spinner mSpnHotkey;
    Spinner mSpnHotkeyMedia;
    BaseAdapter mHotkeySpinnerDataAdapter;
    ArrayList<SpinnerItem> mHotkeySpinnerItemList;
    ArrayList<SpinnerItem> mHotkeyMediaSpinnerItemList;
    EditText mEtxtHotkeyKeycode;
    Button mBtnGetHotkeyKeycode, mBtnSetHotkeyKeycode;

    LinearLayout mLytHotkeyBrightness;
    EditText mEtxtHotkeyBrightness;
    Button mBtnGetHotkeyBrightness, mBtnSetHotkeyBrightness;

    int mSelectedHotkeyID;
    int mSelectedHotkeyMedia;
    String mHotkeyKeycode = "";
    String mHotkeyBrightness = "";

    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;

    //Threads
    public boolean isRunning_statusPolling = false;
    Thread mThreadStatusPolling;

    //Handlers
    MyHandler mHandler = null;


    private static class MyHandler extends Handler {
        private WeakReference<HotkeyDemoActivity> mActivity = null;

        public MyHandler(HotkeyDemoActivity activity) {
            mActivity = new WeakReference<HotkeyDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HotkeyDemoActivity activity = mActivity.get();

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

    private void setHotkeyList() {
        mHotkeySpinnerItemList = new ArrayList<SpinnerItem>();
        mHotkeySpinnerItemList.clear();
        mHotkeySpinnerItemList.add(new SpinnerItem("Hotkey 0",0));
        mHotkeySpinnerItemList.add(new SpinnerItem("Hotkey 1",1));
        mHotkeySpinnerItemList.add(new SpinnerItem("Hotkey 2",2));
        mHotkeySpinnerItemList.add(new SpinnerItem("Hotkey 3",3));
        mHotkeySpinnerItemList.add(new SpinnerItem("Hotkey 4",4));
    }

    private void setHotkeyMediaList() {
        mHotkeyMediaSpinnerItemList = new ArrayList<SpinnerItem>();
        mHotkeyMediaSpinnerItemList.clear();
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("HOME"                    , 172));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("ENTER"                   , 28 ));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("ESC"                     , 1  ));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("BACK"                    , 158));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("UP"                      , 103));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("Down"                    , 108));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("VOLUME_UP"               , 114));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("VOLUME_DOWN"             , 115));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("VOLUME_MUTE"             , 113));
        mHotkeyMediaSpinnerItemList.add(new SpinnerItem("POWER"                   , 116));
    }

    private void initView() {
        setContentView(R.layout.activity_hotkey_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytHotkeyKeycode       = (LinearLayout) findViewById(R.id.row_hotkey_keycode);
        mSpnHotkey              = (Spinner) findViewById(R.id.spn_hotkey);
        mSpnHotkeyMedia         = (Spinner) findViewById(R.id.spn_hotkey_media);
        mEtxtHotkeyKeycode      = (EditText) findViewById(R.id.etxt_hotkey_keycode);
        mBtnGetHotkeyKeycode    = (Button) findViewById(R.id.btn_get_hotkey_keycode);
        mBtnSetHotkeyKeycode    = (Button) findViewById(R.id.btn_set_hotkey_keycode);

        mLytHotkeyBrightness    = (LinearLayout) findViewById(R.id.row_hotkey_brightness);
        mEtxtHotkeyBrightness   = (EditText) findViewById(R.id.etxt_hotkey_brightness);
        mBtnGetHotkeyBrightness = (Button) findViewById(R.id.btn_get_hotkey_brightness);
        mBtnSetHotkeyBrightness = (Button) findViewById(R.id.btn_set_hotkey_brightness);

        setHotkeyList();
        mSpnHotkey.setAdapter(new SpinnerCustomAdapter(this, mHotkeySpinnerItemList));
        setHotkeyMediaList();
        mSpnHotkeyMedia.setAdapter(new SpinnerCustomAdapter(this, mHotkeyMediaSpinnerItemList));

        cleanAllFields();
    }


    private void initListener() {
        mSpnHotkey.setOnItemSelectedListener(spnHotkeyOnItemSelectedListener);
        mSpnHotkeyMedia.setOnItemSelectedListener(spnHotkeyMediaOnItemSelectedListener);
        mBtnGetHotkeyKeycode.setOnClickListener(mBtnOnClickListener);
        mBtnSetHotkeyKeycode.setOnClickListener(mBtnOnClickListener);
        mBtnGetHotkeyBrightness.setOnClickListener(mBtnOnClickListener);
        mBtnSetHotkeyBrightness.setOnClickListener(mBtnOnClickListener);
    }

    AdapterView.OnItemSelectedListener spnHotkeyOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            log("Seleted item " + position);
            mSelectedHotkeyID = (mHotkeySpinnerItemList.get(position)).value;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    AdapterView.OnItemSelectedListener spnHotkeyMediaOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            log("Media Seleted item " + position);
            mSelectedHotkeyMedia = (mHotkeyMediaSpinnerItemList.get(position)).value;
            mHotkeyKeycode = Integer.toString(mSelectedHotkeyMedia);

            mHandler.sendEmptyMessage(R.id.row_hotkey_keycode);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    View.OnClickListener mBtnOnClickListener  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_get_hotkey_keycode:
                    operationGetHotKeyKeycode();
                    break;
                case R.id.btn_set_hotkey_keycode:
                    operationSetHotKeyKeycode();
                    break;
                case R.id.btn_get_hotkey_brightness:
                    operationGetHotKeyBrightness();
                    break;
                case R.id.btn_set_hotkey_brightness:
                    operationSetHotKeyBrightness();
                    break;
            }
        }
    };


    void updateView(int id) {
        switch( id ) {
            case R.id.row_hotkey_keycode:
                if(mHotkeyKeycode.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytHotkeyKeycode.setVisibility(View.GONE);
                } else {
                    if (mEtxtHotkeyKeycode != null)
                        mEtxtHotkeyKeycode.setText(mHotkeyKeycode);
                }
                break;

            case R.id.row_hotkey_brightness:
                if(mHotkeyBrightness.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytHotkeyBrightness.setVisibility(View.GONE);
                } else {
                    if (mEtxtHotkeyBrightness != null)
                        mEtxtHotkeyBrightness.setText(mHotkeyBrightness);
                }
                break;

            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0  = "0";
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
                operationGetHotKeyKeycode();
                operationGetHotKeyBrightness();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        }.execute();
    }


    private void operationGetHotKeyKeycode() {
        int hotkeyId;

        hotkeyId = ((SpinnerItem)mSpnHotkey.getSelectedItem()).value;

        mHotkeyKeycode = getHotKeyKeycode(hotkeyId);

        mHandler.sendEmptyMessage(R.id.row_hotkey_keycode);
    }


    private void operationSetHotKeyKeycode() {
        int ret;
        int hotkeyId;
        int hotkeyKeycode;
        try {
            hotkeyId = ((SpinnerItem)mSpnHotkey.getSelectedItem()).value;
            hotkeyKeycode = Integer.parseInt(mEtxtHotkeyKeycode.getText().toString(), 10);

            ret = setHotKeyKeycode(hotkeyId, hotkeyKeycode);

            if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                Toast.makeText(HotkeyDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

            } else {
                String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

                mHotkeyKeycode = errStr;
                mHandler.sendEmptyMessage(R.id.row_hotkey_keycode);
                Toast.makeText(HotkeyDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
            }
        } catch(Exception ex) {
            Toast.makeText(HotkeyDemoActivity.this, ex.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void operationGetHotKeyBrightness() {
        mHotkeyBrightness = getHotKeyBrightness();

        mHandler.sendEmptyMessage(R.id.row_hotkey_brightness);
    }

    private void operationSetHotKeyBrightness() {
        int ret;
        int hotkeyBrightness;

        hotkeyBrightness = Integer.parseInt(mEtxtHotkeyBrightness.getText().toString(), 10);

        ret = setHotKeyBrightness(hotkeyBrightness);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(HotkeyDemoActivity.this, "Set OK", Toast.LENGTH_SHORT).show();

        } else {
            String errStr = "Set error. " + ErrorCode.errorCodeToString(ret);

            mHotkeyBrightness = errStr;
            mHandler.sendEmptyMessage(R.id.row_hotkey_brightness);
            Toast.makeText(HotkeyDemoActivity.this, errStr, Toast.LENGTH_SHORT).show();
        }
    }





    private int setHotKeyKeycode(int hotkeyId, int keycode) {
        int ret;
        ret = mIvcpAPI.ivcp_hotkey_set_keycode(hotkeyId, keycode);
        return ret;
    }

    private String getHotKeyKeycode(int hotkeyId) {
        int ret;
        int[] keycode = new int[1];

        ret = mIvcpAPI.ivcp_hotkey_get_keycode(hotkeyId, keycode);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            String keycodeStr = String.valueOf(keycode[0]);
            return keycodeStr;

        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    public int setHotKeyBrightness(int brightness) {
        int ret;
        ret = mIvcpAPI.ivcp_hotkey_set_brightness(brightness);
        return ret;

    }

    public String getHotKeyBrightness() {
        int ret;
        int[] brightness = new int[1];

        ret = mIvcpAPI.ivcp_hotkey_get_brightness(brightness);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(brightness[0]);

        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }
}

