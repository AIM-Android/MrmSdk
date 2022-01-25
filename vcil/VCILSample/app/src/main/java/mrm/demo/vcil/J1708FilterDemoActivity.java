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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import mrm.VCIL;
import mrm.define.MRM_CONSTANTS;
import mrm.demo.util.ErrorCode;

public class J1708FilterDemoActivity extends Activity {
    String TAG = "SDKv3 VCIL_DEMO" + " - J1708 FILTER";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;
    EditText mEtxtJ1708FilterMid;
    String mJ1708FilterMaskMid = "";

    Button mBtnJ1708FilterAdd;
    Button mBtnJ1708FilterRemove;
    Button mBtnJ1708FilterGet;
    Button mBtnJ1708FilterReset;


    ListView mLstJ1708FilterMaskList;
    BaseAdapter mLstJ1708FilterMaskListDataAdapter;
    ArrayList<Byte> mJ1708FilterMaskList = new ArrayList<Byte>();
    ArrayList<Byte> mJ1708FilterMaskListTemp = new ArrayList<Byte>();


    //AsyncTasks
    AsyncTask<Void, Void, String> mTaskRefreshJ1708FilterMaskList;


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
        operationGetAllJ1708FilterMask();
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
        setContentView(R.layout.activity_j1708_filter_demo);

        mProgressDailog = new ProgressDialog(this);

        mEtxtJ1708FilterMid = (EditText) findViewById(R.id.etxt_j1708_filter_mid);
        mBtnJ1708FilterAdd = (Button) findViewById(R.id.btn_j1708_filter_add);
        mBtnJ1708FilterGet = (Button) findViewById(R.id.btn_j1708_filter_get);
        mBtnJ1708FilterRemove = (Button) findViewById(R.id.btn_j1708_filter_remove);
        mBtnJ1708FilterReset = (Button) findViewById(R.id.btn_j1708_filter_reset);


        mLstJ1708FilterMaskList = (ListView) findViewById(R.id.lst_j1708_filter_mask_list);
        mLstJ1708FilterMaskListDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mJ1708FilterMaskList.size();
            }

            @Override
            public Object getItem(int position) {
                return mJ1708FilterMaskList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = J1708FilterDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_j1708_filter_demo_lst_row_j1708_filter, null);

                Byte maskMid = mJ1708FilterMaskList.get(position);
                ((TextView) listRow.findViewById(R.id.txt_j1708_filter_list_row_mid)).setText(String.format("%02X", maskMid.byteValue()));
                return listRow;
            }
        };
        mLstJ1708FilterMaskList.setAdapter(mLstJ1708FilterMaskListDataAdapter);
        mLstJ1708FilterMaskListDataAdapter.notifyDataSetChanged();
    }


    private void initListener() {
        mBtnJ1708FilterAdd.setOnClickListener(mBtnOnClickListener);
        mBtnJ1708FilterRemove.setOnClickListener(mBtnOnClickListener);
        mBtnJ1708FilterGet.setOnClickListener(mBtnOnClickListener);
        mBtnJ1708FilterReset.setOnClickListener(mBtnOnClickListener);
        mLstJ1708FilterMaskList.setOnItemClickListener(mOnItemClickedListener);
    }

    AdapterView.OnItemClickListener mOnItemClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Byte tempMaskMid = mJ1708FilterMaskList.get(position);
            mEtxtJ1708FilterMid.setText(String.format("%02X", tempMaskMid.byteValue()));
        }
    };


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_j1708_filter_add:
                    operationSetJ1708FilterMask();
                    operationGetAllJ1708FilterMask();
                    break;

                case R.id.btn_j1708_filter_remove:
                    operationRemoveJ1708FilterMask();
                    operationGetAllJ1708FilterMask();
                    break;

                case R.id.btn_j1708_filter_get:
                    operationGetAllJ1708FilterMask();
                    break;

                case R.id.btn_j1708_filter_reset:
                    operationResetJ1708FilterMask();
                    operationGetAllJ1708FilterMask();
                    break;

                default:
                    break;
            }
        }
    };


    private void operationGetAllJ1708FilterMask() {
        mTaskRefreshJ1708FilterMaskList = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();

                mJ1708FilterMaskList.clear();
                mJ1708FilterMaskListTemp.clear();
                mLstJ1708FilterMaskListDataAdapter.notifyDataSetChanged();
            }

            @Override
            protected String doInBackground(Void... params) {
                int ret = 0;
                String errorInfo = "";

                int[] totalMaskNum = new int[1];
                byte[] maskMidList = new byte[MRM_CONSTANTS.VCIL_MAX_J1708_MASK_NUM];

                log("Getting mask num ...");
                ret = getJ1708FilterMaskNum(totalMaskNum);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask number. Mask# = %d", totalMaskNum[0]));
                } else {
                    errorInfo += String.format("Get mask number error. %s\n", ErrorCode.errorCodeToString(ret));
                    return errorInfo;
                }

                log("Getting mask list ...");
                ret = getJ1708FilterMaskList(maskMidList);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask list."));
                    for (int i = 0; i < totalMaskNum[0]; i++) {
                        mJ1708FilterMaskListTemp.add(maskMidList[i]);
                    }

                } else {
                    errorInfo += String.format("Get mask list error. %s\n", ErrorCode.errorCodeToString(ret));
                }

                return errorInfo;
            }

            @Override
            protected void onPostExecute(String errorInfo) {
                mProgressDailog.cancel();
                mJ1708FilterMaskList = (ArrayList<Byte>) mJ1708FilterMaskListTemp.clone();
                mLstJ1708FilterMaskListDataAdapter.notifyDataSetChanged();

                if (errorInfo.compareTo("") != 0) {
                    Toast.makeText(J1708FilterDemoActivity.this, errorInfo, Toast.LENGTH_LONG).show();
                }
            }
        };

        mTaskRefreshJ1708FilterMaskList.execute();
    }


    private void operationSetJ1708FilterMask() {
        int ret;
        mJ1708FilterMaskMid = mEtxtJ1708FilterMid.getText().toString().toUpperCase();
        if (!mJ1708FilterMaskMid.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "MID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = addJ1708FilterMask((byte)Integer.parseInt(mJ1708FilterMaskMid, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Add OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Add ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private void operationRemoveJ1708FilterMask() {
        int ret;

        mJ1708FilterMaskMid = mEtxtJ1708FilterMid.getText().toString().toUpperCase();
        if (!mJ1708FilterMaskMid.matches("[0-9a-fA-F]{2}")) {
            Toast.makeText(this, "MID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = removeJ1708FilterMask((byte)Integer.parseInt(mJ1708FilterMaskMid, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Remove OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Remove ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationResetJ1708FilterMask() {
        int ret;
        ret = resetJ1708FilterMask();

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Reset OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Reset ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private int getJ1708FilterMaskNum(int[] totalMaskNum) {
        int ret;
        ret = mVcilAPI.vcil_j1708_get_mask_number(totalMaskNum);
        return ret;
    }

    private int getJ1708FilterMaskList(byte[] maskList) {
        int ret;
        ret = mVcilAPI.vcil_j1708_get_all_mask(maskList);
        return ret;
    }

    private int addJ1708FilterMask(byte mid) {
        int ret;
        ret = mVcilAPI.vcil_j1708_add_mask(mid);
        return ret;
    }

    private int removeJ1708FilterMask(byte mid) {
        int ret;
        ret = mVcilAPI.vcil_j1708_remove_mask(mid);
        return ret;
    }

    private int resetJ1708FilterMask() {
        int ret;
        ret = mVcilAPI.vcil_j1708_remove_all_mask();
        return ret;
    }
}