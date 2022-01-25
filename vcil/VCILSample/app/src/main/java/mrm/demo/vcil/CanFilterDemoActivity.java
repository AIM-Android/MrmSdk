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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import mrm.VCIL;
import mrm.define.VCIL.VCIL_CAN_MASK;
import mrm.demo.util.ErrorCode;
import mrm.demo.util.SpinnerCustomAdapter;
import mrm.demo.util.SpinnerItem;


public class CanFilterDemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - CAN FILTER";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnCanFilterCanPort;
    SpinnerCustomAdapter mCanFilterCanPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mCanFilterCanPortSpinnerItemList;

    EditText mEtxtCanFilterBank;

    CheckBox mCbCanFilterExt;
    CheckBox mCbCanFilterRtr;


    EditText mEtxtCanFilterMsgId1;
    String mCanFilterMsgId1 = "";

    EditText mEtxtCanFilterMask1;
    String mCanFilterMask1 = "";

    EditText mEtxtCanFilterMsgId2;
    String mCanFilterMsgId2 = "";

    EditText mEtxtCanFilterMask2;
    String mCanFilterMask2 = "";

    Button mBtnCanFilterGet;
    Button mBtnCanFilterSet;
    Button mBtnCanFilterRemove;

    Spinner mSpnCanFilterResetCanPort;
    SpinnerCustomAdapter mCanFilterResetCanPortSpinnerDataAdapter;

    Button mBtnCanFilterReset;

    ListView mLstCanFilterMaskList;
    BaseAdapter mLstCanFilterMaskListDataAdapter;
    ArrayList<VCIL_CAN_MASK> mCanFilterMaskList = new ArrayList<VCIL_CAN_MASK>();
    ArrayList<VCIL_CAN_MASK> mCanFilterMaskListTemp = new ArrayList<VCIL_CAN_MASK>();

    //AsyncTasks
    AsyncTask<Void, Void, String> mTaskRefreshCanFilterMaskList;


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
        operationGetAllCanFilterMask();
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
        mCanFilterCanPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mCanFilterCanPortSpinnerItemList.clear();
        mCanFilterCanPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mCanFilterCanPortSpinnerItemList.add(new SpinnerItem("1", 1));
    }


    private void initView() {
        setContentView(R.layout.activity_can_filter_demo);

        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnCanFilterCanPort = (Spinner) findViewById(R.id.spn_can_filter_port);
        mCanFilterCanPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanFilterCanPortSpinnerItemList);
        mSpnCanFilterCanPort.setAdapter(mCanFilterCanPortSpinnerDataAdapter);
        mSpnCanFilterCanPort.setSelection(0);

        mEtxtCanFilterBank = (EditText) findViewById(R.id.etxt_can_filter_bank);

        mCbCanFilterExt = (CheckBox) findViewById(R.id.cb_can_filter_ext);
        mCbCanFilterRtr = (CheckBox) findViewById(R.id.cb_can_filter_rtr);

        mEtxtCanFilterMsgId1 = (EditText) findViewById(R.id.etxt_can_filter_msg_id_1);
        mEtxtCanFilterMask1 = (EditText) findViewById(R.id.etxt_can_filter_mask_1);

        mEtxtCanFilterMsgId2 = (EditText) findViewById(R.id.etxt_can_filter_msg_id_2);
        mEtxtCanFilterMask2 = (EditText) findViewById(R.id.etxt_can_filter_mask_2);

        mBtnCanFilterGet = (Button) findViewById(R.id.btn_can_filter_get);
        mBtnCanFilterSet = (Button) findViewById(R.id.btn_can_filter_set);
        mBtnCanFilterRemove = (Button) findViewById(R.id.btn_can_filter_remove);

        mSpnCanFilterResetCanPort = (Spinner) findViewById(R.id.spn_can_filter_reset_port);
        mCanFilterResetCanPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mCanFilterCanPortSpinnerItemList);
        mSpnCanFilterResetCanPort.setAdapter(mCanFilterResetCanPortSpinnerDataAdapter);
        mSpnCanFilterResetCanPort.setSelection(0);

        mBtnCanFilterReset = (Button) findViewById(R.id.btn_can_filter_reset);

        mLstCanFilterMaskList = (ListView) findViewById(R.id.lst_can_filter_mask_list);
        mLstCanFilterMaskListDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mCanFilterMaskList.size();
            }

            @Override
            public Object getItem(int position) {
                return mCanFilterMaskList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = CanFilterDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_can_filter_demo_lst_row_can_filter, null);

                VCIL_CAN_MASK maskObj = mCanFilterMaskList.get(position);

                log(String.format("Displaying mask ... bank = %d", maskObj.bank));

                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_bank)).setText(String.format("%d", maskObj.bank));
                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_ext)).setText(String.valueOf(maskObj.extended_frame));
                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_rtr)).setText(String.valueOf(maskObj.remote_request));
                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_id_1)).setText(String.format("%X", maskObj.id1));
                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_mask_1)).setText(String.format("%X", maskObj.mask1));
                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_id_2)).setText(String.format("%X", maskObj.id2));
                ((TextView) listRow.findViewById(R.id.txt_can_filter_list_row_mask_2)).setText(String.format("%X", maskObj.mask2));

                return listRow;
            }
        };
        mLstCanFilterMaskList.setAdapter(mLstCanFilterMaskListDataAdapter);
        mLstCanFilterMaskListDataAdapter.notifyDataSetChanged();
    }


    private void initListener() {
        mBtnCanFilterGet.setOnClickListener(mBtnOnClickListener);
        mBtnCanFilterSet.setOnClickListener(mBtnOnClickListener);
        mBtnCanFilterRemove.setOnClickListener(mBtnOnClickListener);
        mBtnCanFilterReset.setOnClickListener(mBtnOnClickListener);
        mLstCanFilterMaskList.setOnItemClickListener(mOnItemClickedListener);
    }

    AdapterView.OnItemClickListener mOnItemClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VCIL_CAN_MASK tempMask = mCanFilterMaskList.get(position);

            mEtxtCanFilterBank.setText(String.valueOf(tempMask.bank));

            mCbCanFilterExt.setChecked(tempMask.extended_frame);
            mCbCanFilterRtr.setChecked(tempMask.remote_request);

            mEtxtCanFilterMsgId1.setText(Long.toHexString(tempMask.id1).toUpperCase());
            mEtxtCanFilterMask1.setText(Long.toHexString(tempMask.mask1).toUpperCase());

            mEtxtCanFilterMsgId2.setText(Long.toHexString(tempMask.id2).toUpperCase());
            mEtxtCanFilterMask2.setText(Long.toHexString(tempMask.mask2).toUpperCase());
        }
    };

    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_can_filter_get:
                    //operationGetCanFilterMask();
                    operationGetAllCanFilterMask();
                    break;

                case R.id.btn_can_filter_set:
                    operationSetCanFilterMask();
                    operationGetAllCanFilterMask();
                    break;

                case R.id.btn_can_filter_remove:
                    operationRemoveCanFilterMask();
                    operationGetAllCanFilterMask();
                    break;

                case R.id.btn_can_filter_reset:
                    operationResetCanFilterMask();
                    operationGetAllCanFilterMask();
                    break;

                default:
                    break;
            }
        }
    };


    private void operationGetCanFilterMask() {
        int ret;
        byte selectedPort = (byte) (((SpinnerItem)mSpnCanFilterCanPort.getSelectedItem()).value);

        VCIL_CAN_MASK tempMask = new VCIL_CAN_MASK();
        tempMask.bank = (byte) Integer.parseInt(mEtxtCanFilterBank.getText().toString());

        ret = getCanFilterMask(selectedPort, tempMask);

        if (ret != ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(
                    CanFilterDemoActivity.this,
                    String.format("Get mask %d of bus %d error. %s\n", tempMask.bank, selectedPort, ErrorCode.errorCodeToString(ret)),
                    Toast.LENGTH_SHORT
                    ).show();
            return;
        }

        mEtxtCanFilterBank.setText(String.valueOf(tempMask.bank));

        mCbCanFilterExt.setChecked(tempMask.extended_frame);
        mCbCanFilterRtr.setChecked(tempMask.remote_request);

        mEtxtCanFilterMsgId1.setText(Long.toHexString(tempMask.id1).toUpperCase());
        mEtxtCanFilterMask1.setText(Long.toHexString(tempMask.mask1).toUpperCase());

        mEtxtCanFilterMsgId2.setText(Long.toHexString(tempMask.id2).toUpperCase());
        mEtxtCanFilterMask2.setText(Long.toHexString(tempMask.mask2).toUpperCase());
    }



    private void operationGetAllCanFilterMask() {
        mTaskRefreshCanFilterMaskList = new AsyncTask<Void, Void, String>() {

            byte selectedPort;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();

                mCanFilterMaskList.clear();
                mCanFilterMaskListTemp.clear();
                mLstCanFilterMaskListDataAdapter.notifyDataSetChanged();

                selectedPort = (byte) (((SpinnerItem)mSpnCanFilterCanPort.getSelectedItem()).value);
            }

            @Override
            protected String doInBackground(Void... params) {
                int ret = 0;
                byte bank = 0;
                VCIL_CAN_MASK tempMask;
                String errorInfo = "";
                String failedBanks = "";

                for (bank = 1; bank < 14; bank++) {
                    tempMask = new VCIL_CAN_MASK();
                    tempMask.bank = bank;
                    ret = getCanFilterMask(selectedPort, tempMask);
                    if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                        mCanFilterMaskListTemp.add(tempMask);
                    } else {
                        failedBanks += String.format("%2d  (%s)\n", bank, ErrorCode.errorCodeToString(ret));
                    }
                }
                errorInfo += String.format("Can not get the following filters of bus %d.\nFilters : \n%s", selectedPort, failedBanks);

                return errorInfo;
            }

            @Override
            protected void onPostExecute(String errorInfo) {
                mProgressDailog.cancel();
                mCanFilterMaskList = (ArrayList<VCIL_CAN_MASK>) mCanFilterMaskListTemp.clone();
                mLstCanFilterMaskListDataAdapter.notifyDataSetChanged();

                if (errorInfo.compareTo("") != 0) {
                    Toast.makeText(CanFilterDemoActivity.this, errorInfo, Toast.LENGTH_LONG).show();
                }
            }
        };

        mTaskRefreshCanFilterMaskList.execute();
    }


    private void operationSetCanFilterMask() {
        int ret;
        byte selectedPort = (byte) (((SpinnerItem)mSpnCanFilterCanPort.getSelectedItem()).value);

        mCanFilterMsgId1 = mEtxtCanFilterMsgId1.getText().toString().toUpperCase();
        if (!mCanFilterMsgId1.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "ID1 FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mCanFilterMask1 = mEtxtCanFilterMask1.getText().toString().toUpperCase();
        if (!mCanFilterMask1.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "MASK1 FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mCanFilterMsgId2 = mEtxtCanFilterMsgId2.getText().toString().toUpperCase();
        if (!mCanFilterMsgId1.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "ID2 FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        mCanFilterMask2 = mEtxtCanFilterMask2.getText().toString().toUpperCase();
        if (!mCanFilterMask1.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "MASK2 FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }


        VCIL_CAN_MASK tempMask = new VCIL_CAN_MASK();
        tempMask.bank = (byte) Integer.parseInt(mEtxtCanFilterBank.getText().toString());
        tempMask.extended_frame = mCbCanFilterExt.isChecked();
        tempMask.remote_request = mCbCanFilterRtr.isChecked();
        tempMask.id1 = Integer.parseInt(mCanFilterMsgId1, 16);
        tempMask.mask1 = Integer.parseInt(mCanFilterMask1, 16);
        tempMask.id2 = Integer.parseInt(mCanFilterMsgId2, 16);
        tempMask.mask2 = Integer.parseInt(mCanFilterMask2, 16);

        ret = setCanFilterMask(selectedPort, tempMask);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Set OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Set ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private void operationRemoveCanFilterMask() {
        int ret;
        byte selectedPort = (byte) (((SpinnerItem)mSpnCanFilterCanPort.getSelectedItem()).value);
        byte bank = (byte) Integer.parseInt(mEtxtCanFilterBank.getText().toString());

        ret = removeCanFilterMask(selectedPort, bank);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Remove OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Remove ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationResetCanFilterMask() {
        int ret;
        byte selectedPort = (byte) (((SpinnerItem)mSpnCanFilterResetCanPort.getSelectedItem()).value);

        ret = resetCanFilterMask(selectedPort);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Reset OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Reset ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private int resetCanFilterMask(byte port) {
        int ret;
        ret = mVcilAPI.vcil_can_reset_mask(port);
        return ret;
    }


    private int getCanFilterMask(byte port, VCIL_CAN_MASK mask) {
        int ret;
        ret = mVcilAPI.vcil_can_get_mask(port, mask);
        return ret;
    }

    private int setCanFilterMask(byte port, VCIL_CAN_MASK mask) {
        int ret;
        ret = mVcilAPI.vcil_can_set_mask(port, mask);
        return ret;
    }

    private int removeCanFilterMask(byte port, byte bank) {
        int ret;
        ret = mVcilAPI.vcil_can_remove_mask(port, bank);
        return ret;
    }
}
