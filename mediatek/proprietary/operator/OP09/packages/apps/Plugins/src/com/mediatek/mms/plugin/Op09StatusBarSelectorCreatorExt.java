package com.mediatek.mms.plugin;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.widget.CustomAccountRemoteViews;
import com.mediatek.mms.ext.DefaultOpStatusBarSelectorCreatorExt;

import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;

public class Op09StatusBarSelectorCreatorExt extends DefaultOpStatusBarSelectorCreatorExt {

    public static final String ACTION_MMS_ACCOUNT_CHANGED =
            "com.android.mms.ui.ACTION_MMS_ACCOUNT_CHANGED";
    Context mContext;
    public Op09StatusBarSelectorCreatorExt(Context context) {
        super(context);
        mContext = context;
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean refreshData(ArrayList<CustomAccountRemoteViews.AccountInfo> list,
            long currentSubId, String filterAction) {
        /// M: Modify for OP09 dual button feature @{
        List<SubscriptionInfo> subList = SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoList();
        if (MessageUtils.isDualSendButtonEnable()) {
            if (!SubscriptionManager.isValidSubscriptionId((int) currentSubId)
                    || SubscriptionManager.from(mContext).getActiveSubscriptionInfo(
                            (int) currentSubId) == null) {
                SubscriptionInfo firstSub = SubscriptionManager.from(mContext)
                        .getActiveSubscriptionInfoForSimSlotIndex(0);
                if (firstSub != null) {
                    currentSubId = firstSub.getSubscriptionId();
                }
            }

            int count = list.size();
            for (int index = 0; index < count; index++) {
                AccountInfo info = list.get(index);
                if (info != null) {
                    if (info.getIntent().getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, -1) ==
                                currentSubId) {
                        info.setActiveStatus(true);
                    } else {
                        info.setActiveStatus(false);
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public AccountInfo createAlwaysAskAccountInfo(int icon, String label,
            Intent intent, boolean isActived) {
        return new AccountInfo(0, null, label, null,intent, false);
    }
}
