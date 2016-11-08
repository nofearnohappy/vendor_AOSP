package com.hesine.nmsg.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.FileEx;

public class MainActivity extends Activity {
    ArrayList<ServiceInfo> mServices = null;
    ListView mList = null;
    MyAdapter mAdapter = null;
    Context mContext = null;
    HeaderView mHeader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (FileEx.getStorageFullStatus()) {
            showSpaceFullDialog();
        }
        mContext = this;
        mHeader = (HeaderView) findViewById(R.id.header);
        mHeader.setTitle("imsi:" + DeviceInfo.getIMSI(this));
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new MyAdapter();
        mServices = (ArrayList<ServiceInfo>) DBUtils.getServiceInfos();
        if (mServices == null || mServices.size() == 0) {
            this.startActivity(new Intent(this, ConversationActivity.class));
            this.finish();
        } else {
            mList.setAdapter(mAdapter);
            mList.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(mContext, ConversationActivity.class);
                    String account = mServices.get(position).getEmail();
                    intent.putExtra("thread_id", DBUtils.getThreadIdViaAccount(account));
                    intent.putExtra("phone_number", account);
                    mContext.startActivity(intent);
                }
            });
        }
    }

    private void showSpaceFullDialog() {
        AlertDialog.Builder builder = new Builder(MainActivity.this);
        builder.setMessage(R.string.space_not_enough);
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    public class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mServices.size();
        }

        @Override
        public Object getItem(int position) {
            return mServices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.chat_item_multipole_subitem, null);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.subject);
            tv.setText(mServices.get(position).getName());
            return convertView;
        }

    }
}
