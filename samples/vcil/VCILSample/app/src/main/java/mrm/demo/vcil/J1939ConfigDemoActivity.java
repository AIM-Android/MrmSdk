package mrm.demo.vcil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import mrm.VCIL;
import mrm.define.VCIL.VCIL_J1939_CONFIG;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;

public class J1939ConfigDemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - J1939 CONFIG";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnJ1939ConfigPort;
    SpinnerCustomAdapter mJ1939ConfigPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1939ConfigPortSpinnerItemList;
    byte mSelectedJ1939ConfigPortId;

    EditText mEtxtJ1939ConfigSrcAddr;
    String mJ1939ConfigSrcAddr = "";

    EditText mEtxtJ1939ConfigArbitraryAddrCapable;
    EditText mEtxtJ1939ConfigIndustryGroup;
    EditText mEtxtJ1939ConfigVehicleSystemInstance;
    EditText mEtxtJ1939ConfigVehicleSystem;
    EditText mEtxtJ1939ConfigFunction;
    EditText mEtxtJ1939ConfigFunctionInstance;
    EditText mEtxtJ1939ConfigEcuInstance;
    EditText mEtxtJ1939ConfigManufacturerNumber;
    EditText mEtxtJ1939ConfigIdentityNumber;


    String mJ1939ConfigArbitraryAddrCapable = "";
    String mJ1939ConfigIndustryGroup = "";
    String mJ1939ConfigVehicleSystemInstance = "";
    String mJ1939ConfigVehicleSystem = "";
    String mJ1939ConfigFunction = "";
    String mJ1939ConfigFunctionInstance = "";
    String mJ1939ConfigEcuInstance = "";
    String mJ1939ConfigManufacturerNumber = "";
    String mJ1939ConfigIdentityNumber = "";

    Button mBtnJ1939ConfigSet;
    Button mBtnJ1939ConfigGet;


    //AsyncTasks
    AsyncTask<Void, Void, String> mTaskRefreshJ1939ConfigFields;


    //Handlers
    MyViewRefreshHandler mViewRefreshHandler = null;

    private static class MyViewRefreshHandler extends Handler {
        private WeakReference<J1939ConfigDemoActivity> mActivity = null;

        public MyViewRefreshHandler(J1939ConfigDemoActivity activity) {
            mActivity = new WeakReference<J1939ConfigDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            J1939ConfigDemoActivity activity = mActivity.get();

            if (activity == null)
                return;

            switch (msg.what) {
                case R.id.j1939_config:
                    activity.mEtxtJ1939ConfigSrcAddr.setText(activity.mJ1939ConfigSrcAddr);
                    activity.mEtxtJ1939ConfigArbitraryAddrCapable.setText(activity.mJ1939ConfigArbitraryAddrCapable);
                    activity.mEtxtJ1939ConfigIndustryGroup.setText(activity.mJ1939ConfigIndustryGroup);
                    activity.mEtxtJ1939ConfigVehicleSystemInstance.setText(activity.mJ1939ConfigVehicleSystemInstance);
                    activity.mEtxtJ1939ConfigVehicleSystem.setText(activity.mJ1939ConfigVehicleSystem);
                    activity.mEtxtJ1939ConfigFunction.setText(activity.mJ1939ConfigFunction);
                    activity.mEtxtJ1939ConfigFunctionInstance.setText(activity.mJ1939ConfigFunctionInstance);
                    activity.mEtxtJ1939ConfigEcuInstance.setText(activity.mJ1939ConfigEcuInstance);
                    activity.mEtxtJ1939ConfigManufacturerNumber.setText(activity.mJ1939ConfigManufacturerNumber);
                    activity.mEtxtJ1939ConfigIdentityNumber.setText(activity.mJ1939ConfigIdentityNumber);
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
        mViewRefreshHandler = new MyViewRefreshHandler(this);
        this.initView();
        this.initListener();
    }


    @Override
    protected void onResume() {
        log("\n\n=============== onResume() ===============\n\n");
        super.onResume();
        operationGetJ1939Config();
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

    private void setSpinnerItenLists() {
        mJ1939ConfigPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1939ConfigPortSpinnerItemList.clear();
        mJ1939ConfigPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mJ1939ConfigPortSpinnerItemList.add(new SpinnerItem("1", 1));

    }


    private void initView() {
        setContentView(R.layout.activity_j1939_config_demo);

        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnJ1939ConfigPort = (Spinner) findViewById(R.id.spn_j1939_config_port);
        mJ1939ConfigPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1939ConfigPortSpinnerItemList);
        mSpnJ1939ConfigPort.setAdapter(mJ1939ConfigPortSpinnerDataAdapter);
        mSpnJ1939ConfigPort.setSelection(0);

        mEtxtJ1939ConfigSrcAddr = (EditText) findViewById(R.id.etxt_j1939_config_addr);

        mEtxtJ1939ConfigArbitraryAddrCapable = (EditText) findViewById(R.id.etxt_j1939_config_arbitrary_address_capable);
        mEtxtJ1939ConfigIndustryGroup = (EditText) findViewById(R.id.etxt_j1939_config_industry_group);
        mEtxtJ1939ConfigVehicleSystemInstance = (EditText) findViewById(R.id.etxt_j1939_config_vehicle_system_instance);
        mEtxtJ1939ConfigVehicleSystem = (EditText) findViewById(R.id.etxt_j1939_config_vehicle_system);
        mEtxtJ1939ConfigFunction = (EditText) findViewById(R.id.etxt_j1939_config_function);
        mEtxtJ1939ConfigFunctionInstance = (EditText) findViewById(R.id.etxt_j1939_config_function_instance);
        mEtxtJ1939ConfigEcuInstance = (EditText) findViewById(R.id.etxt_j1939_config_ecu_instance);
        mEtxtJ1939ConfigManufacturerNumber = (EditText) findViewById(R.id.etxt_j1939_config_manufacturer_code);
        mEtxtJ1939ConfigIdentityNumber = (EditText) findViewById(R.id.etxt_j1939_config_identity_number);

        mBtnJ1939ConfigSet = (Button) findViewById(R.id.btn_j1939_set_config);
        mBtnJ1939ConfigGet = (Button) findViewById(R.id.btn_j1939_get_config);
    }


    private void initListener() {
        mBtnJ1939ConfigSet.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939ConfigGet.setOnClickListener(mBtnOnClickListener);
    }

    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_j1939_set_config:
                    operationSetJ1939Config();
                    break;

                case R.id.btn_j1939_get_config:
                    operationGetJ1939Config();
                    break;

                default:
                    break;
            }
        }
    };


    private void operationGetJ1939Config() {
        mTaskRefreshJ1939ConfigFields = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();

                mSelectedJ1939ConfigPortId = (byte) (((SpinnerItem) mSpnJ1939ConfigPort.getSelectedItem()).value);
            }

            @Override
            protected String doInBackground(Void... params) {
                String errorInfo = "";
                int ret = 0;

                VCIL_J1939_CONFIG tempConfig = new VCIL_J1939_CONFIG();
                ret = getJ1939Config(mSelectedJ1939ConfigPortId, tempConfig);

                if (ret != ErrorCode.MRM_ERR_NO_ERROR) {
                    errorInfo += String.format("Get config error. %s\n", ErrorCode.errorCodeToString(ret));
                    return errorInfo;
                }




                mJ1939ConfigSrcAddr = String.format("%02X", tempConfig.address);
                mJ1939ConfigArbitraryAddrCapable = String.format("%X", tempConfig.arbitrary_address_capable);
                mJ1939ConfigIndustryGroup = String.format("%X", tempConfig.industry_group);
                mJ1939ConfigVehicleSystemInstance = String.format("%X", tempConfig.vehicle_system_instance);
                mJ1939ConfigVehicleSystem = String.format("%X", tempConfig.vehicle_system);
                mJ1939ConfigFunction = String.format("%X", tempConfig.function);
                mJ1939ConfigFunctionInstance = String.format("%X", tempConfig.function_instance);
                mJ1939ConfigEcuInstance = String.format("%X", tempConfig.ecu_instance);
                mJ1939ConfigManufacturerNumber = String.format("%X", tempConfig.manufacturer_code);
                mJ1939ConfigIdentityNumber = String.format("%X", tempConfig.identity_number);

                mViewRefreshHandler.sendEmptyMessage(R.id.j1939_config);

                return errorInfo;
            }

            @Override
            protected void onPostExecute(String errorInfo) {
                mProgressDailog.cancel();

                if (errorInfo.compareTo("") != 0) {
                    Toast.makeText(J1939ConfigDemoActivity.this, errorInfo, Toast.LENGTH_LONG).show();
                }
            }
        };

        mTaskRefreshJ1939ConfigFields.execute();
    }


    private void operationSetJ1939Config() {

        mSelectedJ1939ConfigPortId = (byte) (((SpinnerItem) mSpnJ1939ConfigPort.getSelectedItem()).value);

        mJ1939ConfigSrcAddr = mEtxtJ1939ConfigSrcAddr.getText().toString().toUpperCase();
        if (!mJ1939ConfigSrcAddr.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Address. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigArbitraryAddrCapable = mEtxtJ1939ConfigArbitraryAddrCapable.getText().toString().toUpperCase();
        if (!mJ1939ConfigArbitraryAddrCapable.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "ArbitraryAddrCapable. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigIndustryGroup = mEtxtJ1939ConfigIndustryGroup.getText().toString().toUpperCase();
        if (!mJ1939ConfigIndustryGroup.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "IndustryGroup. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigVehicleSystemInstance = mEtxtJ1939ConfigVehicleSystemInstance.getText().toString().toUpperCase();
        if (!mJ1939ConfigVehicleSystemInstance.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "VehicleSystenceInstance. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigVehicleSystem = mEtxtJ1939ConfigVehicleSystem.getText().toString().toUpperCase();
        if (!mJ1939ConfigVehicleSystem.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "VehicleSystem. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigFunction = mEtxtJ1939ConfigFunction.getText().toString().toUpperCase();
        if (!mJ1939ConfigFunction.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Function. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigFunctionInstance = mEtxtJ1939ConfigFunctionInstance.getText().toString().toUpperCase();
        if (!mJ1939ConfigFunctionInstance.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "FunctionInstance. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigEcuInstance = mEtxtJ1939ConfigEcuInstance.getText().toString().toUpperCase();
        if (!mJ1939ConfigEcuInstance.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "EcuInstance. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigManufacturerNumber = mEtxtJ1939ConfigManufacturerNumber.getText().toString().toUpperCase();
        if (!mJ1939ConfigManufacturerNumber.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "ManufacturerNumber. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mJ1939ConfigIdentityNumber = mEtxtJ1939ConfigIdentityNumber.getText().toString().toUpperCase();
        if (!mJ1939ConfigIdentityNumber.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "IdentityNumber. FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }


        int ret;
        VCIL_J1939_CONFIG tempConfig = new VCIL_J1939_CONFIG();

        tempConfig.address = (byte) Integer.parseInt(mJ1939ConfigSrcAddr, 16);
        tempConfig.arbitrary_address_capable = (byte) Integer.parseInt(mJ1939ConfigArbitraryAddrCapable, 16);
        tempConfig.industry_group = (byte) Integer.parseInt(mJ1939ConfigIndustryGroup, 16);
        tempConfig.vehicle_system_instance = (byte) Integer.parseInt(mJ1939ConfigVehicleSystemInstance, 16);
        tempConfig.vehicle_system = (byte) Integer.parseInt(mJ1939ConfigVehicleSystem, 16);
        tempConfig.function = (byte) Integer.parseInt(mJ1939ConfigFunction, 16);
        tempConfig.function_instance = (byte) Integer.parseInt(mJ1939ConfigFunctionInstance, 16);
        tempConfig.ecu_instance = (byte) Integer.parseInt(mJ1939ConfigEcuInstance, 16);
        tempConfig.manufacturer_code = Integer.parseInt(mJ1939ConfigManufacturerNumber, 16);
        tempConfig.identity_number = Integer.parseInt(mJ1939ConfigIdentityNumber, 16);

        ret = setJ1939Config(mSelectedJ1939ConfigPortId, tempConfig);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(
                    J1939ConfigDemoActivity.this,
                    "Set OK",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(
                    J1939ConfigDemoActivity.this,
                    String.format("Set ERROR. %s\n",
                            ErrorCode.errorCodeToString(ret)), Toast.LENGTH_SHORT).show();
        }

        return;
    }


    private int getJ1939Config(byte port, VCIL_J1939_CONFIG config) {
        int ret;
        ret = mVcilAPI.vcil_j1939_get_config(port, config);
        return ret;
    }

    private int setJ1939Config(byte port,VCIL_J1939_CONFIG config) {
        int ret;
        ret = mVcilAPI.vcil_j1939_set_config(port, config);
        return ret;
    }
}
