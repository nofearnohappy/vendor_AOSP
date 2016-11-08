package com.android.mms.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.mediatek.common.MPlugin;
import com.mediatek.widget.CustomAccountRemoteViews;
import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;
import com.mediatek.widget.DefaultAccountSelectionBar;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ext.IOpStatusBarSelectorCreatorExt;
import com.mediatek.mms.ipmessage.IIpStatusBarSelectorExt;
import com.mediatek.opmsg.util.OpMessageUtils;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import com.mediatek.internal.telephony.DefaultSmsSimSettings;

import android.app.StatusBarManager;
import android.telephony.SubscriptionManager;
import android.content.ComponentName;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.telephony.TelephonyManagerEx;

public class StatusBarSelectorCreator {

    private static final String TAG = "[StatusBarSelectorCreator]";
    public static final String ACTION_MMS_ACCOUNT_CHANGED
            = "com.android.mms.ui.ACTION_MMS_ACCOUNT_CHANGED";
    private static StatusBarSelectorCreator sInstance = null;

    private ArrayList<AccountInfo> mData;
    private DefaultAccountSelectionBar mDefaultBar;
    private Context mContext;
    private IOpStatusBarSelectorCreatorExt mOpStatusBarSelectorCreatorExt;
    private IIpStatusBarSelectorExt mIpStatusBarSelector;
    private ComponentName mComponentName;

    public static final StatusBarSelectorCreator getInstance(Activity activity) {
        if (sInstance == null) {
            sInstance = new StatusBarSelectorCreator(activity);
        }
        return sInstance;
    }

    private StatusBarSelectorCreator(Activity activity) {
        mContext = activity.getApplicationContext();
        mComponentName = activity.getComponentName();
        initPlugin(mContext);
        refreshData();
        mDefaultBar = new DefaultAccountSelectionBar(activity, mContext.getPackageName(), mData);
    }

    public void updateStatusBarData() {
        if (mData != null) {
            refreshData();
            mDefaultBar.updateData(mData);
            if (mData.size() > 0) {
                if (isCtOmFratureEnabled()) {
                    mDefaultBar.hide();
                } else {
                    mDefaultBar.show();
                }
                showStatusBarIndicator(true);
            } else {
                mDefaultBar.hide();
                showStatusBarIndicator(false);
            }
        } else {
            Log.d(TAG, "already finished");
        }
    }

    public void showStatusBar() {
        if (mData == null) {
            refreshData();
            mDefaultBar.updateData(mData);
        }
        if (mData.size() > 0) {
            if (isCtOmFratureEnabled()) {
                mDefaultBar.hide();
            } else {
                mDefaultBar.show();
            }
            showStatusBarIndicator(true);
        } else {
            mDefaultBar.hide();
            showStatusBarIndicator(false);
        }
    }

    public void hideStatusBar() {
        mDefaultBar.hide();
        showStatusBarIndicator(false);
        mData = null;
    }

    public void hideNotification() {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mContext.sendBroadcast(intent);
    }

    private void refreshData() {
        if (mData == null) {
            mData = new ArrayList<AccountInfo>();
        }
        int currentSubId = SubscriptionManager.getDefaultSmsSubId();
        Log.d(TAG, "currentSubId: " + currentSubId);
        mData.clear();

        List<SubscriptionInfo> list = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfoList();

        if (list == null || list.size() <= 1) {
            return;
        }
        Log.d(TAG, "sublist size  = " + list.size());

        AccountInfo askInfo = createAlwaysAskAccountInfo(mContext, shouldAlwaysAskSelected());
        mData.add(askInfo);

        for (SubscriptionInfo record : list) {
            Intent intent = new Intent(ACTION_MMS_ACCOUNT_CHANGED);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, record.getSubscriptionId());
            AccountInfo info = new AccountInfo(0, record.createIconBitmap(mContext), record
                    .getDisplayName().toString(), record.getNumber(), intent,
                    record.getSubscriptionId() == currentSubId);
            mData.add(info);
        }

        //operator config
        mOpStatusBarSelectorCreatorExt.refreshData(mData, currentSubId, ACTION_MMS_ACCOUNT_CHANGED);

        // always ask
        mIpStatusBarSelector.onIpRefreshData(mData);
        Log.d(TAG, "mData size  = " + mData.size());
    }

    private boolean shouldAlwaysAskSelected() {
        List<SubscriptionInfo> list = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfoList();
        int defaultSubId = SubscriptionManager.getDefaultSmsSubId();
        if (list == null) {
            return false;
        }
        for (SubscriptionInfo subInfo : list) {
            if (subInfo.getSubscriptionId() == defaultSubId) {
                 return false;
            }
        }
        return true;
    }

    private void showStatusBarIndicator(boolean show) {
        Log.d(TAG, "showStatusBarIndicator show : " + show);
        if (mOpStatusBarSelectorCreatorExt.showStatusBarIndicator(mContext, show)) {
            // operator processed this event, then host should not process it more.
            return;
        }
        StatusBarManager statusbar
                = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
        if (show) {
            Log.d(TAG, "showStatusBarIndicator shouldAlwaysAskSelected : " + shouldAlwaysAskSelected());
            if (shouldAlwaysAskSelected()) {
                statusbar.showDefaultAccountStatus(StatusBarManager.STATUS_ALWAYS_ASK);
            } else {
                statusbar.showDefaultAccountStatus(SubscriptionManager.getDefaultSmsSubId());
            }
        } else {
            statusbar.hideDefaultAccountStatus();
        }
    }
    private AccountInfo createAlwaysAskAccountInfo(Context context, boolean isSelected) {
        Intent intent = new Intent(ACTION_MMS_ACCOUNT_CHANGED).putExtra(
                PhoneConstants.SUBSCRIPTION_KEY, DefaultSmsSimSettings.ASK_USER_SUB_ID);
        String label = context.getString(com.mediatek.R.string.account_always_ask_title);
        AccountInfo info = mOpStatusBarSelectorCreatorExt.createAlwaysAskAccountInfo(
                com.mediatek.R.drawable.account_always_ask_icon, label, intent, isSelected);
        if (info == null) {
            info = new AccountInfo(com.mediatek.R.drawable.account_always_ask_icon, null, label,
                    null,intent, isSelected);
        }
        return info;
    }

    private void initPlugin(Context context) {
        Log.d(TAG, "initPlugin");
        mOpStatusBarSelectorCreatorExt = OpMessageUtils.getOpMessagePlugin()
                .getOpStatusBarSelectorCreatorExt();
        mIpStatusBarSelector = IpMessageUtils.getIpMessagePlugin(context)
                .getIpStatusBarSelector();
        Log.d(TAG, "initPlugin: " + mOpStatusBarSelectorCreatorExt);
    }


    private boolean isCtOmFratureEnabled() {
        List<SubscriptionInfo> list = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfoList();
        if (list == null) {
            return false;
        }
        int inHomeNetworkCtCard = 0;
        for (SubscriptionInfo subInfo : list) {
            int subId = subInfo.getSubscriptionId();
            int slotId = SubscriptionManager.getSlotId(subId);
            int slotType = SvlteUiccUtils.getInstance().getSimType(slotId);
            if (slotType == SvlteUiccUtils.SIM_TYPE_CDMA &&
                    TelephonyManagerEx.getDefault().isInHomeNetwork(subId)) {
                inHomeNetworkCtCard++;
            }
        }
        Log.d(TAG, "active sub size = " + list.size() + " inHomeNetworkCtCard = " + inHomeNetworkCtCard);
        if (inHomeNetworkCtCard >= 2) {
            return true;
        }
        return false;
    }
}
