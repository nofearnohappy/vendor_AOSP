package com.mediatek.rcs.message.plugin;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.mediatek.mms.ipmessage.DefaultIpSettingListActivityExt;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.message.R;

/**
 * class RcsSettingListActivity, plugin implements response SettingListActivity.
 *
 */
public class RcsSettingListActivity extends DefaultIpSettingListActivityExt {
    private static String TAG = "RcseSettingListActivity";
    private final int RCS_SETTING_POSITION = 2;
    private boolean mIsSupportIpMsg = false;
    private int mIpMsgState = -1;
    private boolean mIsWithIpMsg = false;
    private ListActivity mHostActivity;
    private Context mPluginContext;

    public RcsSettingListActivity(Context pluginContext) {
        mPluginContext = pluginContext;
    }

    @Override
    public boolean onIpCreate(ListActivity activity) {
        mHostActivity = activity;
        if (RCSServiceManager.getInstance().isServiceEnabled()) {
            mIpMsgState = IpMessageConsts.DisableServiceStatus.ENABLE;
            mIsSupportIpMsg = true;
        }
        return super.onIpCreate(activity);
    }

    @Override
    public String[] setIpAdapter(String[] settingList) {
        boolean isIpDisablePermanent = (mIpMsgState == IpMessageConsts.DisableServiceStatus.DISABLE_PERMANENTLY);
        if (mIsSupportIpMsg && !isIpDisablePermanent) {
            String rcsMessage = mPluginContext.getString(R.string.rcs_message);
            mIsWithIpMsg = true;
            String[] rcsSettingList = new String[5];
            for (int i = 0; i < settingList.length; i++) {
                if (i < RCS_SETTING_POSITION) {
                    rcsSettingList[i] = settingList[i];
                } else {
                    rcsSettingList[i+1] = settingList[i];
                }
            }
            rcsSettingList[RCS_SETTING_POSITION] = rcsMessage;
            return rcsSettingList;
        } else {
            mIsWithIpMsg = false;
            return super.setIpAdapter(settingList);
        }
    }

    @Override
    public boolean handleIpMessage() {
        if (mIsWithIpMsg && mIpMsgState == IpMessageConsts.DisableServiceStatus.DISABLE_TEMPORARY) {
            View ipMsgView = mHostActivity.getListView().getChildAt(RCS_SETTING_POSITION);
            ipMsgView.setEnabled(false);
            ipMsgView.setVisibility(View.GONE);
            Log.d(TAG, " mUpdateViewStateHandler set ipMsgView disabled");
            return true;
        } else {
            return super.handleIpMessage();
        }
    }

    @Override
    public boolean onIpListItemClick(int position) {
        Log.d(TAG, "[onIpListItemClick]: position = " + position);
        if (mIsWithIpMsg) {
            Intent intent = new Intent();

            switch (position) {
            case 1: //sms
                intent.setClassName(mHostActivity, "com.android.mms.ui.SmsPreferenceActivity");
                mHostActivity.startActivity(intent);
                return true;
            case 2: //mms
                intent.setClassName(mHostActivity, "com.android.mms.ui.MmsPreferenceActivity");
                mHostActivity.startActivity(intent);
                return true;
            case 3: //rcs
                intent.setAction("com.mediatek.rcs.message.ui.RcsSettingsActivity");
                intent.setPackage("com.mediatek.rcs.message");
                mHostActivity.startActivity(intent);
                return true;
            case 4: //notification
                intent.setClassName(mHostActivity, "com.android.mms.ui.NotificationPreferenceActivity");
                mHostActivity.startActivity(intent);
                return true;
            case 5: //general
                intent.setClassName(mHostActivity, "com.android.mms.ui.GeneralPreferenceActivity");
                mHostActivity.startActivity(intent);
                return true;
            default:
                break;
            }
        }
        return super.onIpListItemClick(position);
    }
}
