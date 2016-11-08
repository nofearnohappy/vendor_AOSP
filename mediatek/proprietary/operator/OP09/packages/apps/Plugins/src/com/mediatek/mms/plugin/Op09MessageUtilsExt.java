package com.mediatek.mms.plugin;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.telephony.TelephonyManagerEx;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.mediatek.mms.ext.DefaultOpMessageUtilsExt;
import com.mediatek.mms.plugin.Op09MmsConfigExt;
import com.mediatek.mms.plugin.Op09MmsUtils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class Op09MessageUtilsExt extends DefaultOpMessageUtilsExt {

    private Context mResourceContext;
    public Op09MessageUtilsExt(Context base) {
        super(base);
        mResourceContext = base;
    }

    private static final String TAG = "Op09MessageUtilsExt";

    @Override
    public String formatDateAndTimeStampString(String dateStr, Context context, long msgDate,
            long msgDateSent, boolean fullFormat) {
        /// M: For OP09 @{
        if (MessageUtils.isFormatDateAndTimeStampEnable()) {
            return Op09MmsUtils.getInstance().formatDateAndTimeStampString(context, msgDate,
                    msgDateSent, fullFormat, dateStr);
        /// @}
        }
        return dateStr;
    }

    @Override
    public String formatTimeStampString(Context context, long time, int formatFlags) {
        /// M: For OP09 @{
        if (MessageUtils.isFormatDateAndTimeStampEnable()) {
            return Op09MmsUtils.getInstance().formatDateTime(context, time, formatFlags);
        }
        /// @}
        return null;
    }

    @Override
    public String formatTimeStampStringExtend(Context context, long time, int formatFlags) {
        /// M: For OP09 @{
        if (MessageUtils.isFormatDateAndTimeStampEnable()) {
            return Op09MmsUtils.getInstance().formatDateTime(context, time, formatFlags);
        }
        /// @}
        return null;
    }

    @Override
    public CharSequence[] getVisualTextName(CharSequence[] visualNames, Context context,
            boolean isSaveLocationChoices) {
        if (MessageUtils.isStringReplaceEnable()) {
            List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoList();
            if (subInfoList != null && subInfoList.size() == 1
                    && isUSimType(subInfoList.get(0).getSubscriptionId())
                    && isSaveLocationChoices) {
                return Op09StringReplacementExt.getInstance(context).getSaveLocationString();
            }
        }
        return visualNames;
    }

    @Override
    public void setExtendedAudioType(ArrayList<String> audioType) {
        /// M: for OP09feature: add extended audio type
        Op09MmsConfigExt.getInstance().setExtendedAudioType(audioType);
    }

    /**
     * M: For EVDO: check the sim is whether UIM or not.
     *
     * @param subId the sim's sub id.
     * @return true: UIM; false: not UIM.
     */
    public static boolean isUSimType(int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            Log.d(TAG, "[isUIMType]: phoneType = null");
            return false;
        }
        Log.d(TAG, "[isUIMType]: phoneType = " + phoneType);
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
                || phoneType.equalsIgnoreCase("RUIM");
    }

    @Override
    public boolean allowSafeDraft(final Activity activity, boolean deviceStorageIsFull,
            boolean isNofityUser, int toastType) {
        return MessageUtils.allowSafeDraft(activity, mResourceContext, deviceStorageIsFull,
                isNofityUser, toastType);
    }

}
