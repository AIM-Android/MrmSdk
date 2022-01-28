package mrm.demo.vcil;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import mrm.VCIL;
import mrm.define.MRM_CONSTANTS;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;


public class Obd2FilterDemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - OBD2 FILTER";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnObd2FilterPort;
    SpinnerCustomAdapter mObd2FilterPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mObd2FilterPortSpinnerItemList;
    byte mSelectedObd2FilterPortId;


    EditText mEtxtObd2FilterPid;
    String mObd2FilterMaskPid = "";

    Button mBtnObd2FilterAdd;
    Button mBtnObd2FilterRemove;
    Button mBtnObd2FilterGet;


    Spinner mSpnObd2FilterResetBus;
    int mSelectedObd2FilterResetBusID;
    Button mBtnObd2FilterReset;


    ListView mLstObd2FilterMaskList;
    BaseAdapter mLstObd2FilterMaskListDataAdapter;
    ArrayList<Obd2Mask> mObd2FilterMaskList = new ArrayList<Obd2Mask>();
    ArrayList<Obd2Mask> mObd2FilterMaskListTemp = new ArrayList<Obd2Mask>();

    class Obd2Mask {
        byte port;
        int pid;

        public Obd2Mask(byte pPort, int pPid) {
            port = pPort;
            pid = pPid;
        }
    }


    //AsyncTasks
    AsyncTask<Void, Void, String> mTaskRefreshObd2FilterMaskList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVcilAPI = EntryActivity.mVcilAPI;
        this.initView();
        this.initListener();
    }


    @Override
    protected void onResume() {
        log("\n\n=============== onResume() ===============\n\n");
        operationGetAllObd2FilterMask();
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

    private void setSpinnerItenLists() {
        mObd2FilterPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mObd2FilterPortSpinnerItemList.clear();
        mObd2FilterPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mObd2FilterPortSpinnerItemList.add(new SpinnerItem("1", 1));
    }

    private void initView() {
        setContentView(R.layout.activity_obd2_filter_demo);
        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnObd2FilterPort = (Spinner) findViewById(R.id.spn_obd2_filter_port);
        mObd2FilterPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mObd2FilterPortSpinnerItemList);
        mSpnObd2FilterPort.setAdapter(mObd2FilterPortSpinnerDataAdapter);
        mSpnObd2FilterPort.setSelection(0);

        mEtxtObd2FilterPid = (EditText) findViewById(R.id.etxt_obd2_filter_pid);
        mBtnObd2FilterAdd = (Button) findViewById(R.id.btn_obd2_filter_add);
        mBtnObd2FilterGet = (Button) findViewById(R.id.btn_obd2_filter_get);
        mBtnObd2FilterRemove = (Button) findViewById(R.id.btn_obd2_filter_remove);

        mSpnObd2FilterResetBus = (Spinner) findViewById(R.id.spn_obd2_filter_reset_port);
        mSpnObd2FilterResetBus.setAdapter(mObd2FilterPortSpinnerDataAdapter);
        mBtnObd2FilterReset = (Button) findViewById(R.id.btn_obd2_filter_reset);


        mLstObd2FilterMaskList = (ListView) findViewById(R.id.lst_obd2_filter_mask_list);
        mLstObd2FilterMaskListDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mObd2FilterMaskList.size();
            }

            @Override
            public Object getItem(int position) {
                return mObd2FilterMaskList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = Obd2FilterDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_obd2_filter_demo_lst_row_obd2_filter, null);

                Obd2Mask mask = mObd2FilterMaskList.get(position);
                ((TextView) listRow.findViewById(R.id.txt_obd2_filter_list_row_port)).setText(String.format("%d", mask.port));
                ((TextView) listRow.findViewById(R.id.txt_obd2_filter_list_row_pid)).setText(String.format("%08X", mask.pid));
                return listRow;
            }
        };
        mLstObd2FilterMaskList.setAdapter(mLstObd2FilterMaskListDataAdapter);
        mLstObd2FilterMaskListDataAdapter.notifyDataSetChanged();
    }


    private void initListener() {
        mBtnObd2FilterAdd.setOnClickListener(mBtnOnClickListener);
        mBtnObd2FilterRemove.setOnClickListener(mBtnOnClickListener);
        mBtnObd2FilterGet.setOnClickListener(mBtnOnClickListener);
        mBtnObd2FilterReset.setOnClickListener(mBtnOnClickListener);
        mLstObd2FilterMaskList.setOnItemClickListener(mOnItemClickedListener);
    }

    AdapterView.OnItemClickListener mOnItemClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Obd2Mask tempMask = mObd2FilterMaskList.get(position);

            for (int i = 0; i < mObd2FilterPortSpinnerItemList.size(); i++) {
                if (tempMask.port == mObd2FilterPortSpinnerItemList.get(i).value) {
                    mSpnObd2FilterPort.setSelection(i);
                    break;
                }
            }

            mEtxtObd2FilterPid.setText(String.format("%X", tempMask.pid));
        }
    };


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_obd2_filter_add:
                    operationSetObd2FilterMask();
                    operationGetAllObd2FilterMask();
                    break;

                case R.id.btn_obd2_filter_get:
                    operationGetAllObd2FilterMask();
                    break;

                case R.id.btn_obd2_filter_remove:
                    operationRemoveObd2FilterMask();
                    operationGetAllObd2FilterMask();
                    break;

                case R.id.btn_obd2_filter_reset:
                    operationResetObd2FilterMask();
                    operationGetAllObd2FilterMask();
                    break;

                default:
                    break;
            }
        }
    };


    private void operationGetAllObd2FilterMask() {
        mTaskRefreshObd2FilterMaskList = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();

                mObd2FilterMaskList.clear();
                mObd2FilterMaskListTemp.clear();
                mLstObd2FilterMaskListDataAdapter.notifyDataSetChanged();

                mSelectedObd2FilterPortId = (byte) (((SpinnerItem) mSpnObd2FilterPort.getSelectedItem()).value);
            }

            @Override
            protected String doInBackground(Void... params) {
                int ret = 0;
                String errorInfo = "";

                int[] totalMaskNum;
                int[] maskPidList;

                totalMaskNum = new int[1];
                maskPidList = new int[MRM_CONSTANTS.VCIL_MAX_OBD2_MASK_NUM];


                log("Getting mask num ...");
                ret = getObd2FilterMaskNum(mSelectedObd2FilterPortId, totalMaskNum);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask number. Mask# = %d", mSelectedObd2FilterPortId, totalMaskNum[0]));
                } else {
                    errorInfo += String.format("Get mask number error(port = %d). %s\n", mSelectedObd2FilterPortId, ErrorCode.errorCodeToString(ret));
                    return errorInfo;
                }

                log("Getting mask list ...");
                ret = getObd2FilterMaskList(mSelectedObd2FilterPortId, maskPidList);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask list. port = %d", mSelectedObd2FilterPortId));
                    for (int i = 0; i < totalMaskNum[0]; i++) {
                        mObd2FilterMaskListTemp.add(new Obd2Mask(mSelectedObd2FilterPortId, maskPidList[i]));
                    }

                } else {
                    errorInfo += String.format("Get mask list error(BUS = %d). %s\n", mSelectedObd2FilterPortId, ErrorCode.errorCodeToString(ret));
                }

                return errorInfo;
            }

            @Override
            protected void onPostExecute(String errorInfo) {
                mProgressDailog.cancel();
                mObd2FilterMaskList = (ArrayList<Obd2Mask>) mObd2FilterMaskListTemp.clone();
                mLstObd2FilterMaskListDataAdapter.notifyDataSetChanged();

                if (errorInfo.compareTo("") != 0) {
                    Toast.makeText(Obd2FilterDemoActivity.this, errorInfo, Toast.LENGTH_LONG).show();
                }
            }
        };

        mTaskRefreshObd2FilterMaskList.execute();
    }

    private void operationSetObd2FilterMask() {
        int ret;

        mSelectedObd2FilterPortId = (byte) (((SpinnerItem) mSpnObd2FilterPort.getSelectedItem()).value);
        mObd2FilterMaskPid = mEtxtObd2FilterPid.getText().toString().toUpperCase();
        if (!mObd2FilterMaskPid.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "PID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = addObd2FilterMask(
                mSelectedObd2FilterPortId,
                Integer.parseInt(mObd2FilterMaskPid, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Add OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Add ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private void operationRemoveObd2FilterMask() {
        int ret;

        mSelectedObd2FilterPortId = (byte) (((SpinnerItem) mSpnObd2FilterPort.getSelectedItem()).value);
        mObd2FilterMaskPid = mEtxtObd2FilterPid.getText().toString().toUpperCase();
        if (!mObd2FilterMaskPid.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "PID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = removeObd2FilterMask(
                mSelectedObd2FilterPortId,
                Integer.parseInt(mObd2FilterMaskPid, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Remove OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Remove ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationResetObd2FilterMask() {
        int ret;
        mSelectedObd2FilterResetBusID = ((SpinnerItem) mSpnObd2FilterResetBus.getSelectedItem()).value;
        ret = resetObd2FilterMask((byte) mSelectedObd2FilterResetBusID);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Reset OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Reset ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private int getObd2FilterMaskNum(byte bus, int[] totalMaskNum) {
        int ret;
        ret = mVcilAPI.vcil_obd2_get_mask_number(bus, totalMaskNum);
        return ret;
    }

    private int getObd2FilterMaskList(byte bus, int[] maskList) {
        int ret;
        ret = mVcilAPI.vcil_obd2_get_all_mask(bus, maskList);
        return ret;
    }

    private int addObd2FilterMask(byte bus, int pid) {
        int ret;
        ret = mVcilAPI.vcil_obd2_add_mask(bus, pid);
        return ret;
    }

    private int removeObd2FilterMask(byte bus, int pid) {
        int ret;
        ret = mVcilAPI.vcil_obd2_remove_mask(bus, pid);
        return ret;
    }

    private int resetObd2FilterMask(byte bus) {
        int ret;
        ret = mVcilAPI.vcil_obd2_remove_all_mask(bus);
        return ret;
    }
}
