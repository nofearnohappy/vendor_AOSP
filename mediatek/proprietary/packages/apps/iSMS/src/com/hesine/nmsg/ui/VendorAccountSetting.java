package com.hesine.nmsg.ui;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.hesine.nmsg.R;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Statistics;

public class VendorAccountSetting extends Activity {

    private TextView accountName;
    private TextView accountIntroduce;
    private TextView clearHistory;
    private TextView personSetting;
    private CircularImage avatar;
    private ImageView accountSwitch;
    private HeaderView mHeader = null;
    private long threadId = 0;
    private ServiceInfo serviceInfo;
    private Bitmap icon;
    private boolean mIsChecked = true;
    private ProgressDialog mPDialog;
    private PopupDialog mDialog = null;
    private String account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vendor_account_setting);
        initView();
        account = getIntent().getExtras().getString(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT);
        threadId = getIntent().getExtras().getLong(EnumConstants.NMSG_INTENT_EXTRA_THREADID);
        if (account == null) {
            MLog.info("user_account == null ");
            this.finish();
            return;
        }
        serviceInfo = DBUtils.getServiceInfo(account);
        if (serviceInfo == null) {
            MLog.info("serviceInfo == null ");
            finish();
        }
        initHeader();
        setView();
    }

    private void initHeader() {
        mHeader = (HeaderView) findViewById(R.id.header);
        mHeader.setTitle(serviceInfo.getName());
        mHeader.getMoreView().setVisibility(View.GONE);
    }

    private void setView() {
        mIsChecked = serviceInfo.getStatus() > 0 ? true : false;
        accountName.setText(serviceInfo.getName());
        accountIntroduce.setText(serviceInfo.getDesc());
        accountSwitch.setImageResource(mIsChecked ? R.drawable.ic_on : R.drawable.ic_off);
        icon = BitmapFactory.decodeFile(serviceInfo.getIcon());
        if (icon != null) {
            avatar.setImageBitmap(icon);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        serviceInfo = DBUtils.getServiceInfo(account);
        mIsChecked = serviceInfo.getStatus() > 0 ? true : false;
        accountSwitch.setImageResource(mIsChecked ? R.drawable.ic_on : R.drawable.ic_off);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<VendorAccountSetting> mActivity;

        public MyHandler(VendorAccountSetting activity) {
            mActivity = new WeakReference<VendorAccountSetting>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            VendorAccountSetting activity = mActivity.get();
            if (activity != null) {
                activity.mPDialog.dismiss();
            }
        }
    }

    public final Handler myHandler = new MyHandler(this);

    private void initView() {
        accountName = (TextView) findViewById(R.id.setting_account_name);
        accountIntroduce = (TextView) findViewById(R.id.setting_introduce_text);
        accountIntroduce.setMovementMethod(ScrollingMovementMethod.getInstance()); 
        clearHistory = (TextView) findViewById(R.id.setting_clear_history);
        accountSwitch = (ImageView) findViewById(R.id.setting_account_switch);
        personSetting = (TextView) findViewById(R.id.person_setting);
        avatar = (CircularImage) findViewById(R.id.setting_account_avatar);

        clearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog = new PopupDialog(VendorAccountSetting.this)
                        .setInfo(R.string.clear_all_messages).setLeft(R.string.btn_cancel)
                        .setRight(R.string.btn_ok).show();
                mDialog.setListener(new ActionListener() {
                    @Override
                    public void doAction(int type) {
                        if (type == PopupDialog.ActionType.ACTION_RIGHT) {
                            mPDialog = ProgressDialog.show(VendorAccountSetting.this, null,
                                    VendorAccountSetting.this
                                            .getString(R.string.delete_msg_wait_dialog), true,
                                    false);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (DBUtils.deleteAllMsgViaThreadId(threadId) != -1) {
                                        Statistics.getInstance().threadsDelete(
                                                serviceInfo.getName(),
                                                Statistics.ThreadDeleteType.CLEAR_MSG);
                                    }
                                    myHandler.sendEmptyMessage(0);
                                }
                            }).start();
                        }
                        mDialog.dismiss();
                    }
                });
            }
        });

        accountSwitch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mIsChecked = !mIsChecked;
                accountSwitch.setImageResource(mIsChecked ? R.drawable.ic_on : R.drawable.ic_off);
                DBUtils.updateServiceInfoStatus(serviceInfo.getAccount(), mIsChecked);// 0:off
                                                                                      // ,
                // 1: on
                Statistics.getInstance().accountStatus(serviceInfo.getAccount(),
                        String.valueOf(mIsChecked ? 1 : 0));
                if (!mIsChecked) {
                    CommonUtils.clearLatestWifiMsgIds();
                }
            }
        });

        personSetting.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(VendorAccountSetting.this, UserInfoActivity.class);
                intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, Config.getUuid());
                VendorAccountSetting.this.startActivity(intent);
            }
        });
    }

}
