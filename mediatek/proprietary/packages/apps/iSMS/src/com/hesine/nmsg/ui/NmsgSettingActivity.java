package com.hesine.nmsg.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Statistics;

public class NmsgSettingActivity extends Activity {
    private HeaderView mHeader = null;
    private ListView mListView = null;
    private CheckBox wifiReceiveOnly;
    // private ServiceInfo serviceInfo;
    private ArrayList<ServiceInfo> mServiceInfoList = null;
    private TextView noVendorHint;
    private TextView nmsgVendorIntro;
    private TextView dividerLine;
    private View networkSettingLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nmsg_setting);
        initHeader();
        mServiceInfoList = DBUtils.getServiceInfos();
        mListView = (ListView) findViewById(R.id.accountList);
        wifiReceiveOnly = (CheckBox) findViewById(R.id.setting_account_switch);
        noVendorHint = (TextView) findViewById(R.id.nmsg_setting_noaccount);
        nmsgVendorIntro = (TextView) findViewById(R.id.nmsg_setting_intro);
        dividerLine = (TextView) findViewById(R.id.account_list_layout_line);

        if (EnumConstants.NETWORK_SETTING_SWITCH) {
        final TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
            networkSettingLayout = findViewById(R.id.network_setting);
            networkSettingLayout.setVisibility(View.VISIBLE);
            tabHost.addTab(tabHost.newTabSpec("tab1")
                    .setIndicator(getResources().getString(R.string.nmsg_receive_wifi))
                    .setContent(R.id.network_setting));

            tabHost.addTab(tabHost.newTabSpec("tab2")
                    .setIndicator(getResources().getString(R.string.nmsg_receive_setting))
                    .setContent(R.id.account_list_layout));
            tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                @Override
                public void onTabChanged(String tabId) {
                    updateTabBackground(tabHost);
                }
            });
            updateTabBackground(tabHost);

        } else {
            mHeader.setTitle(this.getResources().getString(R.string.nmsg_receive_setting));
        }

        AccountListAdapter adapter = new AccountListAdapter(this);
        mListView.setAdapter(adapter);

        // String mUserAccount =
        // getIntent().getStringExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT);

        wifiReceiveOnly.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    MLog.info("NmsgSetting wifiCheck change false,get msg from server");
                    CommonUtils.procRequestLatestWifiMsg();
                }
                Config.saveIsWifiChecked(isChecked);
            }
        });
    }

    private void updateTabBackground(final TabHost tabHost) {
        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            View vvv = tabHost.getTabWidget().getChildAt(i);
            TextView tv = (TextView) vvv.findViewById(android.R.id.title);
            if (tabHost.getCurrentTab() == i) {
                vvv.setBackgroundResource(R.drawable.tabhost_tab_bg);
                tv.setTextSize(14);
                tv.setTextColor(Color.parseColor("#0066ff"));
            } else {
                vvv.setBackground(null);
                tv.setTextColor(Color.parseColor("#000000"));
                tv.setTextSize(14);

            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mServiceInfoList.size() == 0) {
            noVendorHint.setVisibility(View.VISIBLE);
            nmsgVendorIntro.setVisibility(View.GONE);
            dividerLine.setVisibility(View.GONE);
        } else {
            noVendorHint.setVisibility(View.GONE);
            nmsgVendorIntro.setVisibility(View.VISIBLE);
            dividerLine.setVisibility(View.VISIBLE);
        }
        if (EnumConstants.NETWORK_SETTING_SWITCH) {
            wifiReceiveOnly.setChecked(Config.getIsWifiChecked());
        }

    }

    private void initHeader() {
        mHeader = (HeaderView) findViewById(R.id.header);
        mHeader.setTitle(this.getResources().getString(R.string.nmsg_setting));
        mHeader.getMoreView().setVisibility(View.GONE);
    }

    private class AccountListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public AccountListAdapter(Context context) {
            super();
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mServiceInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            if ((position >= getCount()) || (position < 0) || mServiceInfoList.isEmpty()) {
                return null;
            } else {
                return mServiceInfoList.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.subscribe_list_item, null);
            TextView account = (TextView) convertView.findViewById(R.id.AccountNameText);
            final Button subscribeButton = (Button) convertView.findViewById(R.id.SubscribeButton);
            final ServiceInfo info = mServiceInfoList.get(position);
            account.setText(info.getName());
            if (info.getStatus() == 0) {
                subscribeButton.setText(R.string.nmsg_setting_receive);
                subscribeButton.setBackgroundResource(R.drawable.button_concern);
                subscribeButton.setTextColor(Color.rgb(255, 255, 255));
            } else {
                subscribeButton.setText(R.string.nmsg_setting_unreceive);
                subscribeButton.setBackgroundResource(R.drawable.button_unconcern);
                subscribeButton.setTextColor(Color.rgb(82, 82, 82));
            }
            subscribeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (info.getStatus() == 1) {
                        // showDialog(info.getName(), info);
                        String sInfoFormat = String.format(
                                getResources().getString(R.string.dialog_receive_text),
                                info.getName());
                        LayoutInflater factory = LayoutInflater.from(NmsgSettingActivity.this);
                        final View view = factory.inflate(R.layout.dialog_unreceive, null);
                        TextView textView = (TextView) view
                                .findViewById(R.id.dialog_unreceive_text);
                        textView.setText(sInfoFormat);

                        TextView cancelView = (TextView) view
                                .findViewById(R.id.dialog_unreceive_cancel);
                        TextView okView = (TextView) view.findViewById(R.id.dialog_unreceive_ok);
                        final Dialog dialog = new Dialog(NmsgSettingActivity.this,
                                R.style.Theme_pop_dialog);
                        dialog.setContentView(view);
                        okView.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                info.setStatus(0);
                                DBUtils.updateServiceInfoStatus(info.getAccount(), false);
                                // 1: on
                                Statistics.getInstance().accountStatus(info.getAccount(),
                                        String.valueOf(0));

                                subscribeButton.setBackgroundResource(R.drawable.button_concern);
                                subscribeButton.setText(R.string.nmsg_setting_receive);
                                subscribeButton.setTextColor(Color.rgb(255, 255, 255));
                                CommonUtils.clearLatestWifiMsgIds();
                                dialog.dismiss();
                            }
                        });

                        cancelView.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    } else {
                        info.setStatus(1);
                        subscribeButton.setText(R.string.nmsg_setting_unreceive);
                        subscribeButton.setBackgroundResource(R.drawable.button_unconcern);
                        subscribeButton.setTextColor(Color.rgb(82, 82, 82));
                        DBUtils.updateServiceInfoStatus(info.getAccount(), true);
                        // 1: on
                        Statistics.getInstance()
                                .accountStatus(info.getAccount(), String.valueOf(1));
                        // if(Config.isWifiChecked&&Config.wifiConnected){
                        // CommonUtils.procRequestLatestWifiMsg();
                        // }
                    }
                }
            });
            return convertView;
        }
    }
}
