package mrm.demo.ivcp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


//MRM SDK SETTING STEP 1 : Import necessary package ================================================================================================================================
import mrm.client.IVCPServiceClient;
import mrm.client.IVCPServiceConnection;
import mrm.define.MRM_CONSTANTS;
import mrm.demo.util.ErrorCode;





public class EntryActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - ENTRY";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), Process.myTid(), logStr));
    }



    /** MRM SDK SETTING STEP 2: Declare the service client. Implement the callbacks. ================================================================================================= */
    static IVCPServiceClient mIvcpAPI;

    IVCPServiceConnection mIVCPServiceConnection = new IVCPServiceConnection() {
        //This will be triggered when service is succefully bound.
        // !!!! NOTE !!!!
        // 1.The MRM SDK APIs only works after this callback is triggers. You should start you activities work here.
        // 2. DO NOT do long task in this callback.
        @Override
        public void on_service_connected() {
            log("In APP. on_service_connected().  IVCPService connected!!!");

            //DO SOMETHING AFTER SERVICE CONNECTED
            updateViews();
            setMenuEnable(true);
        }

        //This callback will be triggered when the connected service stopped or died.
        // !!!! NOTE !!!!
        // 1. DO NOT do long task in this callback.
        @Override
        public void on_service_disconnected() {
            log("In APP. on_service_disconnected().  IVCPService disconnected ... ");

            //DO SOMETHING AFTER SERVICE DISCONNECTED
            cleanFields();
            setMenuEnable(false);

            //You can also try to re-connect to service
            Thread threadRetryServiceConnection = new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret =  -1;
                    do {
                        try {
                            log("Retry to bind service after 10 sec");
                            Thread.sleep(10*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if( !mIvcpAPI.ivcp_is_service_connected() ) {
                            log("Service is not connected. Retry to bind service ...");
                            ret = mIvcpAPI.ivcp_bind_service(EntryActivity.this.mIVCPServiceConnection);
                        } else {
                            log("Service is connected. Stop retry.");
                            break;
                        }

                    }while(ret != ErrorCode.MRM_ERR_NO_ERROR);
                }
            });
            threadRetryServiceConnection.start();
        }
    };

    /** MRM SDK SETTING STEP 2  END  ================================================================================================================================================= */




    /** MRM SDK SETTING STEP 3: Function to bind/unbind service using service client===================================================================================================*/

    //Call this function to bind service.  The service client callback - "on_service_connected()"  will be triggered when service is connected.
    int bindIVCPService() {
        int ret;

        log("Try binding IVCPService ... ");
        ret = mIvcpAPI.ivcp_bind_service(this.mIVCPServiceConnection);

        if( ret == ErrorCode.MRM_ERR_NO_ERROR ) {
            log("Bind execute OK. Waiting for binding complete ....");
        } else {
            log("Bind execute FAIL. Failed to bind service...........?????");
        }

        return ret;
    }


    //Call this function to unbind service.
    // !!!! NOTE !!!!
    // YOU SHOULD ALWAYS UNBIND SERVICE BEFORE YOUR ACTIVITY IS DESTROYED. Call this  function before  onDestry() is recommended.
    int unbindIVCPService() {
        return mIvcpAPI.ivcp_unbind_service();
    }
    /** MRM SDK SETTING STEP 3  END  ================================================================================================================================================= */



    //Views & Corresponding data object
    TextView mLabFirmware;
    TextView mLabPowerManagement;
    TextView mLabBattery;
    TextView mLabAlarm;
    TextView mLabWatchDog;
    TextView mLabDio;
    TextView mLabPeripheral;
    TextView mLabStorage;
    TextView mLabSpeedCounter;
    TextView mLabGsensor;
    TextView mLabGsensorAlarm;
    TextView mLabPsensor;
    TextView mLabHotkey;
    TextView mLabIgnitionLog;

    TextView mTxtSdkVer;
    String mSdkVer = "-";

    TextView mTxtPlatformName;
    String mPlatformName = "-";

    TextView mTxtDeviceSerialNum;
    String mDeviceSerialNum = "-";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("\n\n=============== onCreate() ===============\n\n");
        super.onCreate(savedInstanceState);

        mIvcpAPI = new IVCPServiceClient(this);

        initView();
        initListener();

        bindIVCPService();
        setMenuEnable(false);
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
        unbindIVCPService();
        super.onDestroy();
    }

    private void initView() {
        setContentView(R.layout.activity_entry);
        mLabFirmware        = (TextView)findViewById(R.id.lab_firmware);
        mLabPowerManagement = (TextView)findViewById(R.id.lab_powermanagement);
        mLabBattery         = (TextView)findViewById(R.id.lab_battery);
        mLabAlarm           = (TextView)findViewById(R.id.lab_alarm);
        mLabWatchDog        = (TextView)findViewById(R.id.lab_watchdog);
        mLabDio             = (TextView)findViewById(R.id.lab_dio);
        mLabPeripheral      = (TextView)findViewById(R.id.lab_peripheral);
        mLabStorage         = (TextView)findViewById(R.id.lab_storage);
        mLabSpeedCounter    = (TextView)findViewById(R.id.lab_speedcounter);
        mLabGsensor         = (TextView)findViewById(R.id.lab_gsensor);
        mLabGsensorAlarm    = (TextView)findViewById(R.id.lab_gsensor_alarm);
        mLabPsensor         = (TextView)findViewById(R.id.lab_psensor);
        mLabHotkey          = (TextView)findViewById(R.id.lab_hotkey);
        mLabIgnitionLog     = (TextView)findViewById(R.id.lab_ignition_log);

        mTxtSdkVer          = (TextView)findViewById(R.id.txt_sdk_version);
        mTxtDeviceSerialNum = (TextView)findViewById(R.id.txt_device_serial_number);
        mTxtPlatformName = (TextView)findViewById(R.id.txt_platform_name);
    }


    private void initListener() {
        mLabFirmware.setOnClickListener(mLabOnClickListener);
        mLabPowerManagement.setOnClickListener(mLabOnClickListener);
        mLabBattery.setOnClickListener(mLabOnClickListener);
        mLabAlarm.setOnClickListener(mLabOnClickListener);
        mLabWatchDog.setOnClickListener(mLabOnClickListener);
        mLabDio.setOnClickListener(mLabOnClickListener);
        mLabPeripheral.setOnClickListener(mLabOnClickListener);
        mLabSpeedCounter.setOnClickListener(mLabOnClickListener);
        mLabStorage.setOnClickListener(mLabOnClickListener);
        mLabGsensor.setOnClickListener(mLabOnClickListener);
        mLabGsensorAlarm.setOnClickListener(mLabOnClickListener);
        mLabPsensor.setOnClickListener(mLabOnClickListener);
        mLabHotkey.setOnClickListener(mLabOnClickListener);
        mLabIgnitionLog.setOnClickListener(mLabOnClickListener);
    }

    View.OnClickListener mLabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.lab_firmware:
                    startFirmwareDemo();
                    break;
                case R.id.lab_powermanagement:
                    startPowerManagmentDemo();
                    break;
                case R.id.lab_battery:
                    startBatteryDemo();
                    break;
                case R.id.lab_alarm:
                    startAalrmDemo();
                    break;
                case R.id.lab_watchdog:
                    startWatchdogDemo();
                    break;
                case R.id.lab_dio:
                    startDioDemo();
                    break;
                case R.id.lab_peripheral:
                    startPeripheralControlDemo();
                    break;
                case R.id.lab_speedcounter:
                    startSpeedCounterDemo();
                    break;
                case R.id.lab_storage:
                    startStorageDemo();
                    break;
                case R.id.lab_gsensor:
                    startGsensorDemo();
                    break;
                case R.id.lab_gsensor_alarm:
                    startGsensorAlarmDemo();
                    break;
                case R.id.lab_psensor:
                    startPsensorDemo();
                    break;
                case R.id.lab_hotkey:
                    startHotkeyDemo();
                    break;
                case R.id.lab_ignition_log:
                    startIgnitionLogDemo();
                    break;
                default:
                    break;
            }
        }
    };


    private void setMenuEnable(boolean status) {
        mLabFirmware.setEnabled(status);
        mLabPowerManagement.setEnabled(status);
        mLabBattery.setEnabled(status);
        mLabAlarm.setEnabled(status);
        mLabWatchDog.setEnabled(status);
        mLabDio.setEnabled(status);
        mLabPeripheral.setEnabled(status);
        mLabStorage.setEnabled(status);
        mLabSpeedCounter.setEnabled(status);
        mLabGsensor.setEnabled(status);
        mLabGsensorAlarm.setEnabled(status);
        mLabPsensor.setEnabled(status);
        mLabHotkey.setEnabled(status);
        mLabIgnitionLog.setEnabled(status);
    }


    private void updateViews() {
        mSdkVer = getSDKVersion();
        mTxtSdkVer.setText("MRM SDK ver:  " + mSdkVer);

        mPlatformName = getPlatformName();
        mTxtPlatformName.setText(mPlatformName);

        mDeviceSerialNum = getDeviceSerialNumber();
        mTxtDeviceSerialNum.setText(mDeviceSerialNum);
    }

    private void cleanFields() {
        mSdkVer = "(Service Dsconnected)";
        mTxtSdkVer.setText(mSdkVer);

        mDeviceSerialNum = "(Service Dsconnected)";
        mTxtDeviceSerialNum.setText(mDeviceSerialNum);
    }

    private String getSDKVersion() {
        int ret;
        String strSDKVersion;
        byte[] version = new byte[MRM_CONSTANTS.IVCP_MAXIMUM_LIBRARY_STRING_LENGTH];

        ret = mIvcpAPI.ivcp_get_version(version);
        strSDKVersion = new String(version);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return strSDKVersion;
        } else {
            return String.format("ERR - 0x%08X", ret);
        }
    }

    private String getPlatformName() {
        int ret;
        String strPlatformName;
        byte[] name = new byte[MRM_CONSTANTS.IVCP_MAXIMUM_PLATFORM_STRING_LENGTH];

        ret = mIvcpAPI.ivcp_get_platform_name(name);
        strPlatformName = new String(name);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return strPlatformName;
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private String getDeviceSerialNumber() {
        int ret;
        String strSerialNum;
        byte[] serialNum = new byte[MRM_CONSTANTS.IVCP_MAXIMUM_DEVICE_SERIAL_NUMBER_STRING_LENGTH];

        ret = mIvcpAPI.ivcp_get_device_serial_number(serialNum);
        strSerialNum = new String(serialNum);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return strSerialNum;
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private void startFirmwareDemo() {
        Intent in = new Intent();
        in.setClass(this, FirmwareDemoActivity.class);
        startActivity(in);
    }

    private void startPowerManagmentDemo() {
        Intent in = new Intent();
        in.setClass(this, PowerManagementDemoActivity.class);
        startActivity(in);
    }

    private void startBatteryDemo() {
        Intent in = new Intent();
        in.setClass(this, BatteryDemoActivity.class);
        startActivity(in);
    }

    private void startAalrmDemo() {
        Intent in = new Intent();
        in.setClass(this, AlarmDemoActivity.class);
        startActivity(in);
    }

    private void startWatchdogDemo() {
        Intent in = new Intent();
        in.setClass(this, WatchdogDemoActivity.class);
        startActivity(in);
    }

    private void startDioDemo() {
        Intent in = new Intent();
        in.setClass(this, DioDemoActivity.class);
        startActivity(in);
    }

    private void startPeripheralControlDemo() {
        Intent in = new Intent();
        in.setClass(this, PeripheralControlDemoActivity.class);
        startActivity(in);
    }

    private void startStorageDemo() {
        Intent in = new Intent();
        in.setClass(this, StorageDemoActivity.class);
        startActivity(in);
    }

    private void startGsensorDemo() {
        Intent in = new Intent();
        in.setClass(this, GsensorDemoActivity.class);
        startActivity(in);
    }

    private void startGsensorAlarmDemo() {
        Intent in = new Intent();
        in.setClass(this, GsensorAlarmDemoActivity.class);
        startActivity(in);
    }

    private void startPsensorDemo() {
        Intent in = new Intent();
        in.setClass(this, PsensorDemoActivity.class);
        startActivity(in);
    }

    private void startSpeedCounterDemo() {
        Intent in = new Intent();
        in.setClass(this, SpeedCounterDemoActivity.class);
        startActivity(in);
    }

    private void startHotkeyDemo() {
        Intent in = new Intent();
        in.setClass(this, HotkeyDemoActivity.class);
        startActivity(in);
    }

    private void startIgnitionLogDemo() {
        Intent in = new Intent();
        in.setClass(this, IgnitionLogDemoActivity.class);
        startActivity(in);
    }

}
