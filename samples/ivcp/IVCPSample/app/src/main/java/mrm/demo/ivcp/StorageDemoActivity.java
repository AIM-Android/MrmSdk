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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import mrm.client.IVCPServiceClient;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.HexConverter;
import mrm.demo.util.ViewOperator;

public class StorageDemoActivity extends Activity {

    String TAG = "SDKv4 IVCP DEMO - Stor";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), Process.myTid(), logStr));
    }

    //Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytEepromSize;
    TextView mTxtEepromSize;
    String mEepromSize = "";

    LinearLayout mLytEepromAccessByteData;
    EditText mEtxtEepromAccessByteAddr;
    EditText mEtxtEepromAccessByteData;
    Button mBtnEepromWriteByte, mBtnEepromReadByte;
    String mEepromAccessByteData = "";

    LinearLayout mLytEepromAccessMultiByteData;
    EditText mEtxtEepromAccessMultiByteAddr;
    EditText mEtxtEepromAccessMultiByteData;
    EditText mEtxtEepromAccessMultiByteSize;
    TextView mTxtEepromAccessMultiByteAccessedSize;
    Button mBtnEepromWriteMultiByte, mBtnEepromReadMultiByte;
    String mEepromAccessMultiByteData = "";
    String mEepromAccessMultiByteAccessedSize = "";


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskRefreshAllFields;


    //Handlers
    MyHandler mHandler = null;

    private static class MyHandler extends Handler {
        private WeakReference<StorageDemoActivity> mActivity = null;

        public MyHandler(StorageDemoActivity activity) {
            mActivity = new WeakReference<StorageDemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            StorageDemoActivity activity = mActivity.get();

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

    private void initView() {
        setContentView(R.layout.activity_storage_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytEepromSize = (LinearLayout) findViewById(R.id.row_eeprom_size);
        mTxtEepromSize = (TextView) findViewById(R.id.txt_eeprom_size);

        mLytEepromAccessByteData  = (LinearLayout) findViewById(R.id.row_eeprom_byte_access);
        mEtxtEepromAccessByteAddr = (EditText) findViewById(R.id.etxt_eeprom_access_byte_addr);
        mEtxtEepromAccessByteData = (EditText) findViewById(R.id.etxt_eeprom_access_byte_data);
        mBtnEepromWriteByte = (Button) findViewById(R.id.btn_eeprom_write_byte);
        mBtnEepromReadByte = (Button) findViewById(R.id.btn_eeprom_read_byte);

        mLytEepromAccessMultiByteData  = (LinearLayout) findViewById(R.id.row_eeprom_multibyte_access);
        mEtxtEepromAccessMultiByteAddr = (EditText) findViewById(R.id.etxt_eeprom_access_multibyte_addr);
        mEtxtEepromAccessMultiByteData = (EditText) findViewById(R.id.etxt_eeprom_access_multibyte_data);
        mEtxtEepromAccessMultiByteSize = (EditText) findViewById(R.id.etxt_eeprom_access_multibyte_size);
        mTxtEepromAccessMultiByteAccessedSize = (TextView) findViewById(R.id.txt_eeprom_access_multibyte_accessed_size);
        mBtnEepromWriteMultiByte = (Button) findViewById(R.id.btn_eeprom_write_multibyte);
        mBtnEepromReadMultiByte = (Button) findViewById(R.id.btn_eeprom_read_multibyte);

        cleanAllFields();
    }


    private void initListener() {
        mBtnEepromWriteByte.setOnClickListener(mBtnOnClickListener);
        mBtnEepromReadByte.setOnClickListener(mBtnOnClickListener);

        mBtnEepromWriteMultiByte.setOnClickListener(mBtnOnClickListener);
        mBtnEepromReadMultiByte.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_eeprom_read_byte:
                    operationReadByte();
                    break;
                case R.id.btn_eeprom_write_byte:
                    operationWriteByte();
                    break;

                case R.id.btn_eeprom_read_multibyte:
                    operationReadMultiByte();
                    break;
                case R.id.btn_eeprom_write_multibyte:
                    operationWriteMultiByte();
                    break;

                default:
                    break;
            }
        }
    };


    void updateView(int id) {
        switch (id) {
            case R.id.row_eeprom_size:
                if(mEepromSize.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytEepromSize.setVisibility(View.GONE);
                    mLytEepromAccessByteData.setVisibility(View.GONE);
                    mLytEepromAccessMultiByteData.setVisibility(View.GONE);
                }
                if (mTxtEepromSize != null)
                    ViewOperator.setLabelText(mTxtEepromSize, mEepromSize);
                break;

            case R.id.row_eeprom_byte_access:
                if(mEepromAccessByteData.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytEepromAccessByteData.setVisibility(View.GONE);
                } else {
                    if (mEtxtEepromAccessByteData != null)
                        mEtxtEepromAccessByteData.setText(mEepromAccessByteData);
                }
                break;


            case R.id.row_eeprom_multibyte_access:
                if(mEepromAccessMultiByteData.contains(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION))) {
                    mLytEepromAccessMultiByteData.setVisibility(View.GONE);
                } else {
                    if (mEtxtEepromAccessMultiByteData != null)
                        mEtxtEepromAccessMultiByteData.setText(mEepromAccessMultiByteData);

                    if (mTxtEepromAccessMultiByteAccessedSize != null)
                        ViewOperator.setLabelText(mTxtEepromAccessMultiByteAccessedSize, mEepromAccessMultiByteAccessedSize);
                }
                break;

            default:
                break;
        }
    }


    void cleanAllFields() {
        String DEFAULT_DISPLAY_VALUE_NA = "N/A";
        String DEFAULT_DISPLAY_VALUE_0 = "0";

        ViewOperator.setLabelText(mTxtEepromSize, DEFAULT_DISPLAY_VALUE_NA);
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
                operationGetEepromSize();
                operationReadByte();
                operationReadMultiByte();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();
            }
        }.execute();
    }


    private void operationGetEepromSize() {
        mEepromSize = getEepromSize();
        mHandler.sendEmptyMessage(R.id.row_eeprom_size);
    }

    private void operationReadByte() {
        int addr;

        String addrStr = mEtxtEepromAccessByteAddr.getText().toString();
        if ( addrStr.length() != 2 ||
                !addrStr.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Addr format error", Toast.LENGTH_SHORT).show();
            return;
        }
        addr = Integer.parseInt(addrStr, 16);

        mEepromAccessByteData = readSingleByte(addr);
        mHandler.sendEmptyMessage(R.id.row_eeprom_byte_access);
    }

    private void operationWriteByte() {
        int ret = 0;
        int addr;
        byte data;

        String addrStr = mEtxtEepromAccessByteAddr.getText().toString();
        if (addrStr.length() != 2 ||
                !addrStr.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Addr format error", Toast.LENGTH_SHORT).show();
            return;
        }
        addr = Integer.parseInt(addrStr, 16);

        String dataStr = mEtxtEepromAccessByteData.getText().toString();
        if (dataStr.length() != 2 ||
                !dataStr.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Data format error", Toast.LENGTH_SHORT).show();
            return;
        }
        data = (byte) Integer.parseInt(dataStr, 16);

        ret = writeSingleByte(addr, data);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(StorageDemoActivity.this, "Write OK", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(StorageDemoActivity.this, String.format("Write error (%s)", ErrorCode.errorCodeToString(ret)), Toast.LENGTH_SHORT).show();
        }
    }


    private void operationReadMultiByte() {
        int startAddr;
        int size;
        int[] accessedSize = new int[1];

        String addrStr = mEtxtEepromAccessMultiByteAddr.getText().toString();
        if (addrStr.length() != 2 ||
                !addrStr.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Addr format error", Toast.LENGTH_SHORT).show();
            return;
        }
        startAddr = Integer.parseInt(addrStr, 16);

        try {
            size = Integer.parseInt(mEtxtEepromAccessMultiByteSize.getText().toString());
            log(String.format("%02X", (byte) size));
        } catch (Exception ex) {
            Toast.makeText(this, "Size error", Toast.LENGTH_SHORT).show();
            return;
        }

        mEepromAccessMultiByteData = readMultiByte(startAddr, size, accessedSize);
        mEepromAccessMultiByteAccessedSize = String.format("%d", accessedSize[0]);

        mHandler.sendEmptyMessage(R.id.row_eeprom_multibyte_access);
    }

    private void operationWriteMultiByte() {
        int ret;
        int startAddr;
        int size;
        byte[] data;
        int[] accessedSize = new int[1];

        try {
            startAddr = Integer.parseInt(mEtxtEepromAccessMultiByteAddr.getText().toString(), 16);
        } catch (Exception ex) {
            Toast.makeText(this, "Addr format error", Toast.LENGTH_SHORT).show();
            return;
        }


        mEepromAccessMultiByteData = mEtxtEepromAccessMultiByteData.getText().toString().toUpperCase();
        if (mEepromAccessMultiByteData.length() % 2 != 0 ||
                !mEepromAccessMultiByteData.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "Data format error", Toast.LENGTH_SHORT).show();
            return;
        }
        data = HexConverter.hexStringToByteArray(mEepromAccessMultiByteData);


        try {
            size = Integer.parseInt(mEtxtEepromAccessMultiByteSize.getText().toString());
            log(String.format("%02X", (byte)size));
        } catch (Exception ex) {
            Toast.makeText(this, "Size error", Toast.LENGTH_SHORT).show();
            return;
        }
        if (size > data.length) {
            Toast.makeText(this, "Size value error. Size longer than data", Toast.LENGTH_SHORT).show();
            return;
        }



        ret = writeMultiByte(startAddr, size, data, accessedSize);
        mEepromAccessMultiByteAccessedSize = String.format("%d", accessedSize[0]);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(StorageDemoActivity.this, "Write OK", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(StorageDemoActivity.this, String.format("Write error (%X)", ret), Toast.LENGTH_SHORT).show();
        }

        mHandler.sendEmptyMessage(R.id.row_eeprom_multibyte_access);
    }


    private String getEepromSize() {
        int ret;
        int[] size = new int[1];

        ret = mIvcpAPI.ivcp_storage_get_size(size);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.valueOf(size[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    private String readSingleByte(int addr) {
        int ret;
        byte[] data = new byte[1];

        ret = mIvcpAPI.ivcp_storage_read_byte(addr, data);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return String.format("%02X", data[0]);
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int writeSingleByte(int addr, byte data) {
        int ret;
        ret = mIvcpAPI.ivcp_storage_write_byte(addr, data);
        return ret;
    }

    private String readMultiByte(int startAddr, int size, int[] accessedSize) {
        int ret;
        byte[] data = new byte[size];

        ret = mIvcpAPI.ivcp_storage_read(startAddr, data, size, accessedSize);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            String dataStr = HexConverter.byteArrayToHexString(data, accessedSize[0]);
            return dataStr;

        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    private int writeMultiByte(int startAddr, int size, byte[] data, int[] accessedSize) {
        int ret;

        ret = mIvcpAPI.ivcp_storage_write(startAddr, data, size, accessedSize);

        return ret;
    }
}
