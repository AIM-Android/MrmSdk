package mrm.demo.ivcp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import mrm.client.IVCPServiceClient;
import mrm.define.MRM_CONSTANTS;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.ViewOperator;

public class FirmwareDemoActivity extends Activity {
    String TAG = "SDKv4 IVCP DEMO - FW";
    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    //IVCP Service Client Object. Get the instance  from EntryActivity.
    IVCPServiceClient mIvcpAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    LinearLayout mLytFirmwareVer;
    TextView mTxtFirmwareVer;
    String mFirmwareVer = "-";

    LinearLayout mLytBootLoaderFirmwareVer;
    TextView mTxtBootLoaderFirmwareVer;
    String mBootLoaderFirmwareVer = "-";

    LinearLayout mLytFirmwareSaveDefault;
    TextView mTxtFirmwareSaveDefaultResult;
    Button mBtnFirmwareSaveDefault;
    String mFirmwareSaveDefaultResult = "-";

    LinearLayout mLytFirmwareLoadDefault;
    TextView mTxtFirmwareLoadDefaultResult;
    Button mBtnFirmwareLoadDefault;
    String mFirmwareLoadDefaultResult = "-";


    //AsyncTasks
    AsyncTask<Void, Void, Void> mTaskFirmwareSaveDefault;
    AsyncTask<Void, Void, Void> mTaskFirmwareLoadDefault;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("\n\n=============== onCreate() ===============\n\n");
        super.onCreate(savedInstanceState);

        mIvcpAPI = EntryActivity.mIvcpAPI;

        initView();
        initListener();
        cleanAllFields();
    }

    @Override
    protected void onResume() {
        log("\n\n=============== onResume() ===============\n\n");
        super.onResume();
        updateViews();
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
        setContentView(R.layout.activity_firmware_demo);

        mProgressDailog = new ProgressDialog(this);

        mLytFirmwareVer = (LinearLayout) findViewById(R.id.row_fw_version);
        mTxtFirmwareVer = (TextView)findViewById(R.id.txt_fw_version);

        mLytBootLoaderFirmwareVer = (LinearLayout) findViewById(R.id.row_bootloader_fw_version);
        mTxtBootLoaderFirmwareVer = (TextView)findViewById(R.id.txt_bootloader_fw_version);

        mLytFirmwareSaveDefault = (LinearLayout) findViewById(R.id.row_fw_save_default);
        mTxtFirmwareSaveDefaultResult = (TextView)findViewById(R.id.txt_fw_save_default_result);
        mBtnFirmwareSaveDefault = (Button)findViewById(R.id.btn_firmware_save_default);

        mLytFirmwareLoadDefault = (LinearLayout) findViewById(R.id.row_fw_load_default);
        mTxtFirmwareLoadDefaultResult = (TextView)findViewById(R.id.txt_fw_load_default_result);
        mBtnFirmwareLoadDefault = (Button)findViewById(R.id.btn_firmware_load_default);
    }


    private void initListener() {
        mBtnFirmwareSaveDefault.setOnClickListener(mBtnOnClickListener);
        mBtnFirmwareLoadDefault.setOnClickListener(mBtnOnClickListener);
    }


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btn_firmware_save_default:
                    doAsyncFirmwareSaveDefaultResult();
                    break;

                case R.id.btn_firmware_load_default:
                    doAsyncFirmwareLoadDefaultResult();
                    break;

                default:
                    break;
            }
        }
    };

    private void updateViews() {
        mFirmwareVer = getFirmwareVersion();
        if(mFirmwareVer.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
            mLytFirmwareVer.setVisibility(View.GONE);
        else
            ViewOperator.setLabelText(mTxtFirmwareVer, mFirmwareVer);

        mBootLoaderFirmwareVer = getBootLoaderFirmwareVersion();
        if(mBootLoaderFirmwareVer.equals(ErrorCode.errorCodeToString(ErrorCode.MRM_ERR_UNSUPPORT_OPERATION)))
            mLytBootLoaderFirmwareVer.setVisibility(View.GONE);
        else
            ViewOperator.setLabelText(mTxtBootLoaderFirmwareVer, mBootLoaderFirmwareVer);

        mFirmwareSaveDefaultResult = "-";
        ViewOperator.setLabelText(mTxtFirmwareSaveDefaultResult, mFirmwareSaveDefaultResult);

        mFirmwareLoadDefaultResult = "-";
        ViewOperator.setLabelText(mTxtFirmwareLoadDefaultResult, mFirmwareLoadDefaultResult);
    }

    private void cleanAllFields() {
        mFirmwareVer = "-";
        ViewOperator.setLabelText(mTxtFirmwareVer, mFirmwareVer);

        mBootLoaderFirmwareVer = "-";
        ViewOperator.setLabelText(mTxtBootLoaderFirmwareVer, mBootLoaderFirmwareVer);

        mFirmwareSaveDefaultResult = "-";
        ViewOperator.setLabelText(mTxtFirmwareSaveDefaultResult, mFirmwareSaveDefaultResult);

        mFirmwareLoadDefaultResult = "-";
        ViewOperator.setLabelText(mTxtFirmwareLoadDefaultResult, mFirmwareLoadDefaultResult);
    }


    void doAsyncFirmwareSaveDefaultResult() {
        mTaskFirmwareSaveDefault = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Saving Default ...");
                mProgressDailog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                mFirmwareSaveDefaultResult = firmwareSaveDefaultResult();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();

                ViewOperator.setLabelText(mTxtFirmwareSaveDefaultResult, mFirmwareSaveDefaultResult);
            }
        }.execute();
    }


    void doAsyncFirmwareLoadDefaultResult() {
        mTaskFirmwareLoadDefault = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading Default ...");
                mProgressDailog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                mFirmwareLoadDefaultResult = firmwareLoadDefaultResult();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDailog.cancel();

                ViewOperator.setLabelText(mTxtFirmwareLoadDefaultResult, mFirmwareLoadDefaultResult);
            }
        }.execute();
    }






    String getFirmwareVersion() {
        int ret;
        String strFrimwareVersion;
        byte[] fwVersion = new byte[MRM_CONSTANTS.IVCP_MAXIMUM_FIRMWARE_VERSION_LENGTH];

        ret = mIvcpAPI.ivcp_firmware_get_version(fwVersion);
        strFrimwareVersion = new String(fwVersion);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return strFrimwareVersion;
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }

    String getBootLoaderFirmwareVersion() {
        int ret;
        String strFrimwareVersion;
        byte[] fwVersion = new byte[MRM_CONSTANTS.IVCP_MAXIMUM_FIRMWARE_VERSION_LENGTH];

        ret = mIvcpAPI.ivcp_firmware_get_bootloader_version(fwVersion);
        strFrimwareVersion = new String(fwVersion);

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return strFrimwareVersion;
        } else {
            return ErrorCode.errorCodeToString(ret);
        }
    }


    String firmwareSaveDefaultResult() {
        int ret;

        ret = mIvcpAPI.ivcp_firmware_save_default();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return "Save OK";
        } else {
            return "Save error. "+ ErrorCode.errorCodeToString(ret);
        }
    }


    String firmwareLoadDefaultResult() {
        int ret;

        ret = mIvcpAPI.ivcp_firmware_load_default();

        if(ret == ErrorCode.MRM_ERR_NO_ERROR) {
            return "Load OK";
        } else {
            return "Load error. "+ ErrorCode.errorCodeToString(ret);
        }
    }

}
