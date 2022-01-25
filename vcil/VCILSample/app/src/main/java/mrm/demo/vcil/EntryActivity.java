package mrm.demo.vcil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import mrm.MRMConfig;
import mrm.VCIL;
import mrm.define.MRM_CONSTANTS;
import mrm.define.MRM_ENUM;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.SerialPortFinder;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;
import mrm.demo.util.ViewOperator;


public class EntryActivity extends Activity {
    String TAG = "SDKv4 VCIL DEMO" + " - ENTRY";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    static VCIL mVcilAPI = new VCIL();

    LinearLayout mLlInitSdk;
    Spinner mSpnCanDriveNode;
    Button mBtnInitSdk;
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private String canDriveNode;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    TextView mLabCAN;
    TextView mLabJ1939;
    TextView mLabODB2;
    TextView mLabJ1708;
    TextView mLabJ1587;


    TextView mTxtVcilFwVer;
    String mVcilFwVer;
    TextView mTxtVcilModuleSettingsOperationResult;
    String mVcilModuleSettingsOperationResult;

    Spinner mSpnCanPort0;
    SpinnerCustomAdapter mCanPort0SpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanPort0SpinnerItemList;
    int mSelectedCanPort0ModeID = 0;

    Spinner mSpnCanPort1;
    SpinnerCustomAdapter mCanPort1SpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanPort1SpinnerItemList;
    int mSelectedCanPort1ModeID = 0;

    Spinner mSpnJ1708Port0;
    SpinnerCustomAdapter mJ1708Port0SpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1708Port0SpinnerItemList;
    int mSelectedJ1708Port0ModeID = 0;

    Button mBtnGetVcilSetting;
    Button mBtnSetVcilSetting;


    TextView mTxtVcilModuleResetResult;
    String mVcilModuleResetResult;
    Button mBtnVcilModuleReset;


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;
    AsyncTask<Void, Void, Void> mTaskResetVcilModule;


    //Handlers
    MyHandler mHandler = null;

    private static class MyHandler extends Handler {
        private WeakReference<EntryActivity> mActivity = null;

        public MyHandler(EntryActivity activity) {
            mActivity = new WeakReference<EntryActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            EntryActivity activity = mActivity.get();
            if (activity == null)
                return;
            activity.updateView(msg.what);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("\n\n=============== onCreate() ===============\n\n");

        int ret;
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
        initView();
        initListener();

        log("Model Number:" + Build.MODEL);
        if (Build.MODEL.equalsIgnoreCase("AIM8Q")) {
            ret = mVcilAPI.vcil_init(getVcilPath("/sys/bus/usb/drivers/ftdi_sio/3-1.1:1.1"));
        } else if (Build.MODEL.equalsIgnoreCase("AIM8I")) {
            ret = mVcilAPI.vcil_init(getVcilPath("/sys/bus/usb/drivers/ftdi_sio/1-1.1:1.1"));
        } else if (Build.MODEL.contains("AIM75")) {
            ret = mVcilAPI.vcil_init("/dev/ttyUSB105");
        } else {
            mLlInitSdk.setVisibility(View.VISIBLE);
            String[] canNodeValues = mSerialPortFinder.getAllDevicesPath();
            ArrayAdapter<String> canNodeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, canNodeValues);
            mSpnCanDriveNode.setAdapter(canNodeAdapter);
//            ret = mVcilAPI.vcil_init(MRMConfig.VCIL_PATH);
            ret = 0x00000005;
        }
        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            setMenuEnable(true);
            log("VCIL init success");
        } else {
            setMenuEnable(false);
            log(ErrorCode.errorCodeToString(ret));
        }
    }

    @Override
    protected void onResume() {
        log("\n\n=============== onResume() ===============\n\n");
        super.onResume();
        doAsyncTaskRefreshAllFields();
    }

    @Override
    protected void onPause() {
        log("\n\n=============== onPause() ===============\n\n");
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        log("\n\n=============== onDestroy() ===============\n\n");
        mVcilAPI.vcil_deinit();
        super.onDestroy();
    }

    private void initView() {
        mProgressDailog = new ProgressDialog(this);

        setContentView(R.layout.activity_entry);
        mLabCAN = (TextView) findViewById(R.id.lab_can);
        mLabJ1939 = (TextView) findViewById(R.id.lab_j1939);
        mLabODB2 = (TextView) findViewById(R.id.lab_odb2);
        mLabJ1708 = (TextView) findViewById(R.id.lab_j1708);
        mLabJ1587 = (TextView) findViewById(R.id.lab_j1587);


        mLlInitSdk = findViewById(R.id.row_vcil_init_sdk);
        mSpnCanDriveNode = findViewById(R.id.spn_can_drive_node);
        mBtnInitSdk = findViewById(R.id.btn_vcil_sdk_init);
        mSpnCanDriveNode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                canDriveNode = adapterView.getItemAtPosition(i).toString();
                Log.i(TAG, "The selected can node is " + canDriveNode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        mTxtVcilFwVer = (TextView) findViewById(R.id.txt_fw_version);

        mTxtVcilModuleSettingsOperationResult = (TextView) findViewById(R.id.txt_vcil_module_settings_operation_result);
        mSpnCanPort0 = (Spinner) findViewById(R.id.spn_can_port0);
        mSpnCanPort1 = (Spinner) findViewById(R.id.spn_can_port1);
        mSpnJ1708Port0 = (Spinner) findViewById(R.id.spn_j1708_port1);
        mBtnGetVcilSetting = (Button) findViewById(R.id.btn_get_vcil_module_settings);
        mBtnSetVcilSetting = (Button) findViewById(R.id.btn_set_vcil_module_settings);

        mTxtVcilModuleResetResult = (TextView) findViewById(R.id.txt_vcil_module_reset_result);
        mBtnVcilModuleReset = (Button) findViewById(R.id.btn_vcil_module_reset);

        setVcilSettingSpinnerItemList();
        mCanPort0SpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanPort0SpinnerItemList);
        mSpnCanPort0.setAdapter(mCanPort0SpinnerDataAdapter);

        mCanPort1SpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanPort1SpinnerItemList);
        mSpnCanPort1.setAdapter(mCanPort1SpinnerDataAdapter);

        mJ1708Port0SpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1708Port0SpinnerItemList);
        mSpnJ1708Port0.setAdapter(mJ1708Port0SpinnerDataAdapter);

    }

    private boolean initSdk() {
        if (canDriveNode != null && !canDriveNode.isEmpty()) {
            int ret = mVcilAPI.vcil_init(canDriveNode);
            if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                setMenuEnable(true);
                log("VCIL init success");
                doAsyncTaskRefreshAllFields();
                return true;
            } else {
                setMenuEnable(false);
                log(ErrorCode.errorCodeToString(ret));
            }
        } else {
            Toast.makeText(EntryActivity.this, "Please select the drive node first", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void setVcilSettingSpinnerItemList() {
        mCanPort0SpinnerItemList = new ArrayList<SpinnerItem>();
        mCanPort0SpinnerItemList.clear();
        mCanPort0SpinnerItemList.add(new SpinnerItem("CAN", MRM_ENUM.VCIL_MODE.VCIL_MODE_CAN.getValue()));
        mCanPort0SpinnerItemList.add(new SpinnerItem("J1939", MRM_ENUM.VCIL_MODE.VCIL_MODE_J1939.getValue()));
        mCanPort0SpinnerItemList.add(new SpinnerItem("OBD2", MRM_ENUM.VCIL_MODE.VCIL_MODE_OBD2.getValue()));


        mCanPort1SpinnerItemList = new ArrayList<SpinnerItem>();
        mCanPort1SpinnerItemList.clear();
        mCanPort1SpinnerItemList.add(new SpinnerItem("CAN", MRM_ENUM.VCIL_MODE.VCIL_MODE_CAN.getValue()));
        mCanPort1SpinnerItemList.add(new SpinnerItem("J1939", MRM_ENUM.VCIL_MODE.VCIL_MODE_J1939.getValue()));
        mCanPort1SpinnerItemList.add(new SpinnerItem("OBD2", MRM_ENUM.VCIL_MODE.VCIL_MODE_OBD2.getValue()));


        mJ1708Port0SpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1708Port0SpinnerItemList.clear();
        mJ1708Port0SpinnerItemList.add(new SpinnerItem("J1708", MRM_ENUM.VCIL_MODE.VCIL_MODE_J1708.getValue()));
        mJ1708Port0SpinnerItemList.add(new SpinnerItem("J1587", MRM_ENUM.VCIL_MODE.VCIL_MODE_J1587.getValue()));
    }


    private void initListener() {
        mBtnInitSdk.setOnClickListener(mBtnOnClickListener);
        mBtnGetVcilSetting.setOnClickListener(mBtnOnClickListener);
        mBtnSetVcilSetting.setOnClickListener(mBtnOnClickListener);
        mBtnVcilModuleReset.setOnClickListener(mBtnOnClickListener);
        mLabCAN.setOnClickListener(mLabOnClickListener);
        mLabJ1939.setOnClickListener(mLabOnClickListener);
        mLabODB2.setOnClickListener(mLabOnClickListener);
        mLabJ1708.setOnClickListener(mLabOnClickListener);
        mLabJ1587.setOnClickListener(mLabOnClickListener);
    }

    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_vcil_sdk_init:
                    if (initSdk()) {
                        mBtnInitSdk.setEnabled(false);
                    }
                    break;
                case R.id.btn_get_vcil_module_settings:
                    operationGetVcilModuleSettings();
                    break;
                case R.id.btn_set_vcil_module_settings:
                    operationSetVcilModuleSettings();
                    break;
                case R.id.btn_vcil_module_reset:
                    operationResetVcilModules();
                    break;

            }
        }
    };

    View.OnClickListener mLabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.lab_can:
                    startCANDemo();
                    break;
                case R.id.lab_j1939:
                    startJ1939Demo();
                    break;
                case R.id.lab_odb2:
                    startODB2Demo();
                    break;
                case R.id.lab_j1708:
                    startJ1708Demo();
                    break;
                case R.id.lab_j1587:
                    startJ1587Demo();
                    break;

                default:
                    break;
            }
        }
    };


    void updateView(int id) {
        switch (id) {
            case R.id.txt_fw_version:
                ViewOperator.setLabelText(mTxtVcilFwVer, mVcilFwVer);
                break;


            case R.id.row_vcil_module_settings:
                ViewOperator.setLabelText(mTxtVcilModuleSettingsOperationResult, mVcilModuleSettingsOperationResult);

                if (!mVcilModuleSettingsOperationResult.contains(ErrorCode.MSG_TAG_ERROR)) {
                    for (int i = 0; i < mCanPort0SpinnerItemList.size(); i++) {
                        if (mSelectedCanPort0ModeID == mCanPort0SpinnerItemList.get(i).value) {
                            mSpnCanPort0.setSelection(i);
                        }
                    }

                    for (int i = 0; i < mCanPort1SpinnerItemList.size(); i++) {
                        if (mSelectedCanPort1ModeID == mCanPort1SpinnerItemList.get(i).value) {
                            mSpnCanPort1.setSelection(i);
                        }
                    }

                    for (int i = 0; i < mJ1708Port0SpinnerItemList.size(); i++) {
                        if (mSelectedJ1708Port0ModeID == mJ1708Port0SpinnerItemList.get(i).value) {
                            mSpnJ1708Port0.setSelection(i);
                        }
                    }
                }
                break;

            case R.id.row_vcil_module_reset:
                ViewOperator.setLabelText(mTxtVcilModuleResetResult, mVcilModuleResetResult);
            default:
                break;
        }
    }


    private void setMenuEnable(boolean status) {
        mLabCAN.setEnabled(status);
        mLabJ1939.setEnabled(status);
        mLabODB2.setEnabled(status);
        mLabJ1708.setEnabled(status);
        mLabJ1587.setEnabled(status);
    }


    private void startCANDemo() {
        Intent in = new Intent();
        in.setClass(this, CanDemoActivity.class);
        startActivity(in);
    }

    private void startJ1939Demo() {
        Intent in = new Intent();
        in.setClass(this, J1939DemoActivity.class);
        startActivity(in);
    }

    private void startODB2Demo() {
        Intent in = new Intent();
        in.setClass(this, Obd2DemoActivity.class);
        startActivity(in);
    }

    private void startJ1708Demo() {
        Intent in = new Intent();
        in.setClass(this, J1708DemoActivity.class);
        startActivity(in);
    }

    private void startJ1587Demo() {
        Intent in = new Intent();
        in.setClass(this, J1587DemoActivity.class);
        startActivity(in);
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
                operationGetVcilFirmwareVersion();
                operationGetVcilModuleSettings();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        }.execute();
    }


    private void operationGetVcilFirmwareVersion() {
        mVcilFwVer = getVcilFirmwareVersion();
        mHandler.sendEmptyMessage(R.id.txt_fw_version);
    }


    private void operationGetVcilModuleSettings() {
        int ret;
        int[] canPort0Mode = new int[1];
        int[] canPort1Mode = new int[1];
        int[] j1708Port1Mode = new int[1];

        ret = getVcilModuleSettings(canPort0Mode, canPort1Mode, j1708Port1Mode);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mVcilModuleSettingsOperationResult = "Get OK";
            mSelectedCanPort0ModeID = canPort0Mode[0];
            mSelectedCanPort1ModeID = canPort1Mode[0];
            mSelectedJ1708Port0ModeID = j1708Port1Mode[0];

        } else {
            mVcilModuleSettingsOperationResult = "Get ERROR." + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.row_vcil_module_settings);
    }


    private void operationSetVcilModuleSettings() {
        int ret;

        mSelectedCanPort0ModeID = mCanPort0SpinnerItemList.get(mSpnCanPort0.getSelectedItemPosition()).value;
        mSelectedCanPort1ModeID = mCanPort1SpinnerItemList.get(mSpnCanPort1.getSelectedItemPosition()).value;
        mSelectedJ1708Port0ModeID = mJ1708Port0SpinnerItemList.get(mSpnJ1708Port0.getSelectedItemPosition()).value;

        ret = setVcilModuleSettings(mSelectedCanPort0ModeID, mSelectedCanPort1ModeID, mSelectedJ1708Port0ModeID);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            mVcilModuleSettingsOperationResult = "Set OK";
        } else {
            mVcilModuleSettingsOperationResult = "Set ERROR." + ErrorCode.errorCodeToString(ret);
        }

        mHandler.sendEmptyMessage(R.id.row_vcil_module_settings);
    }

    private void operationResetVcilModules() {

        mTaskResetVcilModule = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Reseting ...");
                mProgressDailog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                int ret;

                ret = resetVcilModule();

                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    mVcilModuleResetResult = "Reset OK";
                } else {
                    mVcilModuleResetResult = "Reset ERROR." + ErrorCode.errorCodeToString(ret);
                }
                mHandler.sendEmptyMessage(R.id.row_vcil_module_reset);
                return null;
            }


            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        };
        mTaskResetVcilModule.execute();

    }


    private String getVcilFirmwareVersion() {
        int ret;
        byte[] tempFwVer = new byte[MRM_CONSTANTS.VCIL_MAXIMUM_FIRMWARE_VERSION_LENGTH];

        ret = mVcilAPI.vcil_firmware_get_version(tempFwVer);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return new String(tempFwVer);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int getVcilModuleSettings(int[] can_port0_mode, int[] can_port1_mode, int[] j1708_port0_mode) {
        int ret;
        ret = mVcilAPI.vcil_get_mode(can_port0_mode, can_port1_mode, j1708_port0_mode);
        return ret;
    }

    private int setVcilModuleSettings(int can_port0_mode, int can_port1_mode, int j1708_port0_mode) {
        int ret;
        ret = mVcilAPI.vcil_set_mode(can_port0_mode, can_port1_mode, j1708_port0_mode);
        return ret;
    }

    private int resetVcilModule() {
        int ret;
        ret = mVcilAPI.vcil_firmware_reset();
        return ret;
    }

    private String getVcilPath(String path) {
        String vcilpath = "";
        try {
            File directory = new File(path);
            File[] files = directory.listFiles();

            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().indexOf("ttyUSB") >= 0)
                    vcilpath = "/dev/" + files[i].getName();
            }

            //log("VcilPath :" + vcilpath);
            return vcilpath;
        } catch (Exception e) {
            e.printStackTrace();
            vcilpath = "";
            return vcilpath;
        }
    }
}