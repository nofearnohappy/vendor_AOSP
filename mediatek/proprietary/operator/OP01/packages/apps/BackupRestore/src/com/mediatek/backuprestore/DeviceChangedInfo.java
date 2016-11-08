package com.mediatek.backuprestore;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mediatek.backuprestore.utils.Constants;

public class DeviceChangedInfo extends ListActivity {
    String[] mData;
    String mKey;
    List mDatas = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        mData = i.getStringArrayExtra(Constants.ARRAYDATA);
        mKey = i.getStringExtra(Constants.KEY_SAVED_DATA);
        setContentView(R.layout.device_change_info);
        init();
        getActionBar().setTitle(R.string.change_phone_summary);
    }

    private void init() {
        if (mData != null) {
            for (String data : mData) {
                mDatas.add(data);
                Log.d("TEST", "mDatas size = " + data);
            }
            DataAdapter mDataAdapter = new DataAdapter(this, mData);
            getListView().setAdapter(mDataAdapter);
            getListView().setDividerHeight(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem m = menu.add(0, Menu.FIRST + 1, 0, android.R.string.ok);
        m.setShowAsAction(m.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Menu.FIRST + 1:
            if (mKey.equals(Constants.SDCARD_UNMOUNT_BACKUP)) {
                Intent data = new Intent();
                data.putExtra(Constants.KEY_SAVED_DATA, mKey);
                setResult(RESULT_OK, data);
                finish();
            } else {
                finish();
            }
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mKey.equals(Constants.SDCARD_UNMOUNT_BACKUP)) {
            Intent data = new Intent();
            data.putExtra(Constants.KEY_SAVED_DATA, mKey);
            setResult(RESULT_OK, data);
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    class DataAdapter extends BaseAdapter {

        List mData = null;
        String[] mDeviceData = null;
        Context mContext;
        private LayoutInflater mInflater;

        public DataAdapter(Context c, String[] mDatas) {
            this.mContext = c;
            mDeviceData = mDatas;
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mDeviceData.length;
        }

        @Override
        public Object getItem(int position) {
            return mDeviceData[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.device_info_item, parent, false);
            }

            final TextView text = (TextView) view.findViewById(R.id.text1);
            String re = mDeviceData[position];
            Log.d("TEST", "mDeviceData = " + re);
            text.setText(re);
            return view;
        }

    }
}
