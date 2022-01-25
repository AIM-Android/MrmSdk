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


public class J1939FilterDemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - J1939 FILTER";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;

    //Views & Corresponding data object
    ProgressDialog mProgressDailog;

    Spinner mSpnJ1939FilterPort;
    SpinnerCustomAdapter mJ1939FilterPortSpinnerDataAdapter;
    ArrayList<SpinnerItem> mJ1939FilterPortSpinnerItemList;
    byte mSelectedJ1939FilterPortId;

    EditText mEtxtJ1939FilterPgn;
    String mJ1939FilterMaskPgn = "";

    Button mBtnJ1939FilterGet;
    Button mBtnJ1939FilterAdd;
    Button mBtnJ1939FilterRemove;

    Spinner mSpnJ1939FilterResetPort;
    byte mSelectedJ1939FilterResetPortId;
    Button mBtnJ1939FilterReset;


    ListView mLstJ1939FilterMaskList;
    BaseAdapter mLstJ1939FilterMaskListDataAdapter;
    ArrayList<J1939Mask> mJ1939FilterMaskList = new ArrayList<J1939Mask>();
    ArrayList<J1939Mask> mJ1939FilterMaskListTemp = new ArrayList<J1939Mask>();

    class J1939Mask {
        byte bus;
        int pgn;

        public J1939Mask(byte pBus, int pPgn) {
            bus = pBus;
            pgn = pPgn;
        }
    }

    //AsyncTasks
    AsyncTask<Void, Void, String> mTaskRefreshJ1939FilterMaskList;


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
        operationGetAllJ1939FilterMask();
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
        mJ1939FilterPortSpinnerItemList = new ArrayList<SpinnerItem>();
        mJ1939FilterPortSpinnerItemList.clear();
        mJ1939FilterPortSpinnerItemList.add(new SpinnerItem("0", 0));
        mJ1939FilterPortSpinnerItemList.add(new SpinnerItem("1", 1));
    }

    private void initView() {
        setContentView(R.layout.activity_j1939_filter_demo);
        mProgressDailog = new ProgressDialog(this);

        setSpinnerItenLists();
        mSpnJ1939FilterPort = (Spinner) findViewById(R.id.spn_j1939_filter_port);
        mJ1939FilterPortSpinnerDataAdapter = new SpinnerCustomAdapter(this.getApplicationContext(), mJ1939FilterPortSpinnerItemList);
        mSpnJ1939FilterPort.setAdapter(mJ1939FilterPortSpinnerDataAdapter);
        mSpnJ1939FilterPort.setSelection(0);

        mEtxtJ1939FilterPgn = (EditText) findViewById(R.id.etxt_j1939_filter_pgn);
        mBtnJ1939FilterGet = (Button) findViewById(R.id.btn_j1939_filter_get);
        mBtnJ1939FilterAdd = (Button) findViewById(R.id.btn_j1939_filter_add);
        mBtnJ1939FilterRemove = (Button) findViewById(R.id.btn_j1939_filter_remove);

        mSpnJ1939FilterResetPort = (Spinner) findViewById(R.id.spn_j1939_filter_reset_port);
        mSpnJ1939FilterResetPort.setAdapter(mJ1939FilterPortSpinnerDataAdapter);
        mBtnJ1939FilterReset = (Button) findViewById(R.id.btn_j1939_filter_reset);


        mLstJ1939FilterMaskList = (ListView) findViewById(R.id.lst_j1939_filter_mask_list);
        mLstJ1939FilterMaskListDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mJ1939FilterMaskList.size();
            }

            @Override
            public Object getItem(int position) {
                return mJ1939FilterMaskList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = J1939FilterDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_j1939_filter_demo_lst_row_j1939_filter, null);

                J1939Mask mask = mJ1939FilterMaskList.get(position);
                ((TextView) listRow.findViewById(R.id.txt_j1939_filter_list_row_bus)).setText(String.format("%d", mask.bus));
                ((TextView) listRow.findViewById(R.id.txt_j1939_filter_list_row_pgn)).setText(String.format("%08X", mask.pgn));
                return listRow;
            }
        };
        mLstJ1939FilterMaskList.setAdapter(mLstJ1939FilterMaskListDataAdapter);
        mLstJ1939FilterMaskListDataAdapter.notifyDataSetChanged();
    }


    private void initListener() {
        mBtnJ1939FilterGet.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939FilterAdd.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939FilterRemove.setOnClickListener(mBtnOnClickListener);
        mBtnJ1939FilterReset.setOnClickListener(mBtnOnClickListener);
        mLstJ1939FilterMaskList.setOnItemClickListener(mOnItemClickedListener);
    }

    AdapterView.OnItemClickListener mOnItemClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            J1939Mask tempMask = mJ1939FilterMaskList.get(position);

            for (int i = 0; i < mJ1939FilterPortSpinnerItemList.size(); i++) {
                if (tempMask.bus == mJ1939FilterPortSpinnerItemList.get(i).value) {
                    mSpnJ1939FilterPort.setSelection(i);
                    break;
                }
            }

            mEtxtJ1939FilterPgn.setText(String.format("%X", tempMask.pgn));
        }
    };


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_j1939_filter_get:
                    operationGetAllJ1939FilterMask();
                    break;

                case R.id.btn_j1939_filter_add:
                    operationSetJ1939FilterMask();
                    operationGetAllJ1939FilterMask();
                    break;

                case R.id.btn_j1939_filter_remove:
                    operationRemoveJ1939FilterMask();
                    operationGetAllJ1939FilterMask();
                    break;

                case R.id.btn_j1939_filter_reset:
                    operationResetJ1939FilterMask();
                    operationGetAllJ1939FilterMask();
                    break;

                default:
                    break;
            }
        }
    };


    private void operationGetAllJ1939FilterMask() {
        mTaskRefreshJ1939FilterMaskList = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();

                mJ1939FilterMaskList.clear();
                mJ1939FilterMaskListTemp.clear();
                mLstJ1939FilterMaskListDataAdapter.notifyDataSetChanged();

                mSelectedJ1939FilterPortId = (byte) (((SpinnerItem) mSpnJ1939FilterPort.getSelectedItem()).value);
            }

            @Override
            protected String doInBackground(Void... params) {
                int ret = 0;
                String errorInfo = "";

                int[] totalMaskNum;
                int[] maskPgnList;

                totalMaskNum = new int[1];
                maskPgnList = new int[MRM_CONSTANTS.VCIL_MAX_J1939_MASK_NUM];


                log("Getting mask num ...");
                ret = getJ1939FilterMaskNum(mSelectedJ1939FilterPortId, totalMaskNum);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask number. Mask# = %d", mSelectedJ1939FilterPortId, totalMaskNum[0]));
                } else {
                    errorInfo += String.format("Get mask number error(port = %d). %s\n", mSelectedJ1939FilterPortId, ErrorCode.errorCodeToString(ret));
                    return errorInfo;
                }

                log("Getting mask list ...");
                ret = getJ1939FilterMaskList(mSelectedJ1939FilterPortId, maskPgnList);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask list. port = %d", mSelectedJ1939FilterPortId));
                    for (int i = 0; i < totalMaskNum[0]; i++) {
                        mJ1939FilterMaskListTemp.add(new J1939Mask(mSelectedJ1939FilterPortId, maskPgnList[i]));
                    }

                } else {
                    errorInfo += String.format("Get mask list error(BUS = %d). %s\n", mSelectedJ1939FilterPortId, ErrorCode.errorCodeToString(ret));
                }

                return errorInfo;
            }

            @Override
            protected void onPostExecute(String errorInfo) {
                mProgressDailog.cancel();
                mJ1939FilterMaskList = (ArrayList<J1939Mask>) mJ1939FilterMaskListTemp.clone();
                mLstJ1939FilterMaskListDataAdapter.notifyDataSetChanged();

                if (errorInfo.compareTo("") != 0) {
                    Toast.makeText(J1939FilterDemoActivity.this, errorInfo, Toast.LENGTH_LONG).show();
                }
            }
        };

        mTaskRefreshJ1939FilterMaskList.execute();
    }


    private void operationSetJ1939FilterMask() {
        int ret;

        mSelectedJ1939FilterPortId = (byte) (((SpinnerItem) mSpnJ1939FilterPort.getSelectedItem()).value);
        mJ1939FilterMaskPgn = mEtxtJ1939FilterPgn.getText().toString().toUpperCase();
        if (!mJ1939FilterMaskPgn.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "PGN FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = addJ1939FilterMask(
                mSelectedJ1939FilterPortId,
                Integer.parseInt(mJ1939FilterMaskPgn, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Add OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Add ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private void operationRemoveJ1939FilterMask() {
        int ret;

        mSelectedJ1939FilterPortId = (byte) (((SpinnerItem) mSpnJ1939FilterPort.getSelectedItem()).value);
        mJ1939FilterMaskPgn = mEtxtJ1939FilterPgn.getText().toString().toUpperCase();
        if (!mJ1939FilterMaskPgn.matches("[0-9a-fA-F]+")) {
            Toast.makeText(this, "PGN FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = removeJ1939FilterMask(
                mSelectedJ1939FilterPortId,
                Integer.parseInt(mJ1939FilterMaskPgn, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Remove OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Remove ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationResetJ1939FilterMask() {
        int ret;
        mSelectedJ1939FilterResetPortId = (byte) (((SpinnerItem) mSpnJ1939FilterResetPort.getSelectedItem()).value);
        ret = resetJ1939FilterMask(mSelectedJ1939FilterResetPortId);

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Reset OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Reset ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private int getJ1939FilterMaskNum(byte port, int[] totalMaskNum) {
        int ret;
        ret = mVcilAPI.vcil_j1939_get_mask_number(port, totalMaskNum);
        return ret;
    }

    private int getJ1939FilterMaskList(byte port, int[] maskList) {
        int ret;
        ret = mVcilAPI.vcil_j1939_get_all_mask(port, maskList);
        return ret;
    }

    private int addJ1939FilterMask(byte port, int pgn) {
        int ret;
        ret = mVcilAPI.vcil_j1939_add_mask(port, pgn);
        return ret;
    }

    private int removeJ1939FilterMask(byte port, int pgn) {
        int ret;
        ret = mVcilAPI.vcil_j1939_remove_mask(port, pgn);
        return ret;
    }

    private int resetJ1939FilterMask(byte port) {
        int ret;
        ret = mVcilAPI.vcil_j1939_remove_all_mask(port);
        return ret;
    }
}
