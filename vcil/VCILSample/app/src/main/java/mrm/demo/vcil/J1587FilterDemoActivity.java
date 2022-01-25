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


public class J1587FilterDemoActivity extends Activity {
    String TAG = "SDKv4 VCIL_DEMO" + " - J1587 FILTER";

    void log(String logStr) {
        Log.v(TAG, String.format("PID[%d], TID[%d] :  %s", android.os.Process.myPid(), android.os.Process.myTid(), logStr));
    }

    VCIL mVcilAPI;


    //Views & Corresponding data object
    ProgressDialog mProgressDailog;
    EditText mEtxtJ1587FilterPid;
    String mJ1587FilterMaskPid = "";

    Button mBtnJ1587FilterAdd;
    Button mBtnJ1587FilterRemove;
    Button mBtnJ1587FilterGet;
    Button mBtnJ1587FilterReset;


    ListView mLstJ1587FilterMaskList;
    BaseAdapter mLstJ1587FilterMaskListDataAdapter;
    ArrayList<Integer> mJ1587FilterMaskList = new ArrayList<Integer>();
    ArrayList<Integer> mJ1587FilterMaskListTemp = new ArrayList<Integer>();


    //AsyncTasks
    AsyncTask<Void, Void, String> mTaskRefreshJ1587FilterMaskList;


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
        operationGetAllJ1587FilterMask();
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
        setContentView(R.layout.activity_j1587_filter_demo);

        mProgressDailog = new ProgressDialog(this);

        mEtxtJ1587FilterPid = (EditText) findViewById(R.id.etxt_j1587_filter_pid);
        mBtnJ1587FilterAdd = (Button) findViewById(R.id.btn_j1587_filter_add);
        mBtnJ1587FilterGet = (Button) findViewById(R.id.btn_j1587_filter_get);
        mBtnJ1587FilterRemove = (Button) findViewById(R.id.btn_j1587_filter_remove);
        mBtnJ1587FilterReset = (Button) findViewById(R.id.btn_j1587_filter_reset);


        mLstJ1587FilterMaskList = (ListView) findViewById(R.id.lst_j1587_filter_mask_list);
        mLstJ1587FilterMaskListDataAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mJ1587FilterMaskList.size();
            }

            @Override
            public Object getItem(int position) {
                return mJ1587FilterMaskList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View listRow = J1587FilterDemoActivity.this.getLayoutInflater().inflate(R.layout.activity_j1587_filter_demo_lst_row_j1587_filter, null);

                Integer maskPid = mJ1587FilterMaskList.get(position);
                ((TextView) listRow.findViewById(R.id.txt_j1587_filter_list_row_pid)).setText(String.format("%04X", maskPid.intValue()));
                return listRow;
            }
        };
        mLstJ1587FilterMaskList.setAdapter(mLstJ1587FilterMaskListDataAdapter);
        mLstJ1587FilterMaskListDataAdapter.notifyDataSetChanged();
    }


    private void initListener() {
        mBtnJ1587FilterAdd.setOnClickListener(mBtnOnClickListener);
        mBtnJ1587FilterGet.setOnClickListener(mBtnOnClickListener);
        mBtnJ1587FilterRemove.setOnClickListener(mBtnOnClickListener);
        mBtnJ1587FilterReset.setOnClickListener(mBtnOnClickListener);
        mLstJ1587FilterMaskList.setOnItemClickListener(mOnItemClickedListener);
    }

    AdapterView.OnItemClickListener mOnItemClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Integer tempMaskPid = mJ1587FilterMaskList.get(position);
            mEtxtJ1587FilterPid.setText(String.format("%04X", tempMaskPid.intValue()));
        }
    };


    View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_j1587_filter_add:
                    operationSetJ1587FilterMask();
                    operationGetAllJ1587FilterMask();
                    break;

                case R.id.btn_j1587_filter_get:
                    operationGetAllJ1587FilterMask();
                    break;

                case R.id.btn_j1587_filter_remove:
                    operationRemoveJ1587FilterMask();
                    operationGetAllJ1587FilterMask();
                    break;

                case R.id.btn_j1587_filter_reset:
                    operationResetJ1587FilterMask();
                    operationGetAllJ1587FilterMask();
                    break;

                default:
                    break;
            }
        }
    };


    private void operationGetAllJ1587FilterMask() {
        mTaskRefreshJ1587FilterMaskList = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mProgressDailog.setCancelable(false);
                mProgressDailog.setMessage("Loading ...");
                mProgressDailog.show();

                mJ1587FilterMaskList.clear();
                mJ1587FilterMaskListTemp.clear();
                mLstJ1587FilterMaskListDataAdapter.notifyDataSetChanged();
            }

            @Override
            protected String doInBackground(Void... params) {
                int ret = 0;
                String errorInfo = "";

                int[] totalMaskNum = new int[1];
                int[] maskList = new int[MRM_CONSTANTS.VCIL_MAX_J1587_MASK_NUM];

                log("Getting mask num ...");
                ret = getJ1587FilterMaskNum(totalMaskNum);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask number. Mask# = %d", totalMaskNum[0]));
                } else {
                    errorInfo += String.format("Get mask number error. %s\n", ErrorCode.errorCodeToString(ret));
                    return errorInfo;
                }

                log("Getting mask list ...");
                ret = getJ1587FilterMaskList(maskList);
                if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
                    log(String.format("Got mask list."));
                    for (int i = 0; i < totalMaskNum[0]; i++) {
                        mJ1587FilterMaskListTemp.add(maskList[i]);
                    }

                } else {
                    errorInfo += String.format("Get mask list error. %s\n", ErrorCode.errorCodeToString(ret));
                }

                return errorInfo;
            }

            @Override
            protected void onPostExecute(String errorInfo) {
                mProgressDailog.cancel();
                mJ1587FilterMaskList = (ArrayList<Integer>) mJ1587FilterMaskListTemp.clone();
                mLstJ1587FilterMaskListDataAdapter.notifyDataSetChanged();

                if (errorInfo.compareTo("") != 0) {
                    Toast.makeText(J1587FilterDemoActivity.this, errorInfo, Toast.LENGTH_LONG).show();
                }
            }
        };

        mTaskRefreshJ1587FilterMaskList.execute();
    }


    private void operationSetJ1587FilterMask() {
        int ret;
        mJ1587FilterMaskPid = mEtxtJ1587FilterPid.getText().toString().toUpperCase();
        if (!mJ1587FilterMaskPid.matches("[0-9a-fA-F]{4}")) {
            Toast.makeText(this, "PID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = addJ1587FilterMask(Integer.parseInt(mJ1587FilterMaskPid, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Add OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Add ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private void operationRemoveJ1587FilterMask() {
        int ret;

        mJ1587FilterMaskPid = mEtxtJ1587FilterPid.getText().toString().toUpperCase();
        if (!mJ1587FilterMaskPid.matches("[0-9a-fA-F]{4}")) {
            Toast.makeText(this, "PID FORMAT ERROR.", Toast.LENGTH_LONG).show();
            return;
        }

        ret = removeJ1587FilterMask(Integer.parseInt(mJ1587FilterMaskPid, 16));

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Remove OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Remove ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }

    private void operationResetJ1587FilterMask() {
        int ret;
        ret = resetJ1587FilterMask();

        if (ret == ErrorCode.MRM_ERR_NO_ERROR) {
            Toast.makeText(this, "Reset OK", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Reset ERROR.\n" + ErrorCode.errorCodeToString(ret), Toast.LENGTH_LONG).show();
        }
    }


    private int getJ1587FilterMaskNum(int[] totalMaskNum) {
        int ret;
        ret = mVcilAPI.vcil_j1587_get_mask_number(totalMaskNum);
        return ret;
    }


    private int getJ1587FilterMaskList(int[] maskList) {
        int ret;
        ret = mVcilAPI.vcil_j1587_get_all_mask(maskList);
        return ret;
    }

    private int addJ1587FilterMask(int pid) {
        int ret;
        ret = mVcilAPI.vcil_j1587_add_mask(pid);
        return ret;
    }

    private int removeJ1587FilterMask(int pid) {
        int ret;
        ret = mVcilAPI.vcil_j1587_remove_mask(pid);
        return ret;
    }

    private int resetJ1587FilterMask() {
        int ret;
        ret = mVcilAPI.vcil_j1587_remove_all_mask();
        return ret;
    }


}