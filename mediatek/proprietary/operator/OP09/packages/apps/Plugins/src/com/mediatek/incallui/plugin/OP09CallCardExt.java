package com.mediatek.incallui.plugin;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.common.PluginImpl;
import com.mediatek.incallui.ext.DefaultCallCardExt;
import com.mediatek.op09.plugin.R;

/**
 * callcard extension plugin for op09.
*/
@PluginImpl(interfaceName = "com.mediatek.incallui.ext.ICallCardExt")
public class OP09CallCardExt extends DefaultCallCardExt {

    private static final String TAG = "OP09CallCardExt";

    private TelecomManager mTelecomManager;
    private PhoneAccount mSecondCallAccount;
    private PhoneAccount mThirdCallAccount;
    private TelephonyManager mTelephonyManager;
    /**
     * Return the icon drawable to represent the call provider.
     *
     * @param context for get service.
     * @param account for get icon.
     * @return The icon.
    */
    public Drawable getCallProviderIcon(Context context, PhoneAccount account){
        log("getCallProviderIcon account:" + account);
        mTelephonyManager = getTelephonyManager(context);
        mTelecomManager = getTelecomManager(context);
        if (null != mTelephonyManager &&
                    mTelephonyManager.getDefault().getPhoneCount() < 2) {
            return null;
        }
        Context pluginContext = null;
        try {
            pluginContext = context.createPackageContext("com.mediatek.op09.plugin",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            Log.d(TAG, "no com.mediatek.op09.plugin packages");
        }
        if (account != null && getTelecomManager(
                        context).getCallCapablePhoneAccounts().size() > 0) {
            PhoneAccountHandle handle = account.getAccountHandle();
            String subId = handle.getId();
            if (null != pluginContext && subId != null) {
                PhoneAccount phoneAccount = mTelecomManager.getPhoneAccount(handle);
                int defaultSubId = mTelephonyManager.getSubIdForPhoneAccount(phoneAccount);
                int slotId = SubscriptionManager.getSlotId(defaultSubId);
                if (0 == slotId) {
                    return pluginContext.getResources().getDrawable(R.drawable.ct_sim_indicator_1);
                } else if (1 == slotId) {
                    return pluginContext.getResources().getDrawable(R.drawable.ct_sim_indicator_2);
                }
            }
        }
        return null;
    }

    /**
     * Return the string label to represent the call provider.
     *
     * @param context  for get service.
     * @param account for get lable.
     * @return The lable.
    */
    public String getCallProviderLabel(Context context, PhoneAccount account) {
        mTelephonyManager = getTelephonyManager(context);
        if (null != mTelephonyManager &&
                    mTelephonyManager.getDefault().getPhoneCount() < 2) {
            return null;
        }
        if (account != null && getTelecomManager(
                    context).getCallCapablePhoneAccounts().size() > 0) {
            return account.getLabel().toString();
        }
        return null;
    }

    private TelecomManager getTelecomManager(Context context) {
        if (mTelecomManager == null) {
            mTelecomManager =
                    (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        }
        return mTelecomManager;
    }

    private TelephonyManager getTelephonyManager(Context context) {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) context.
                    getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }

    /**
     * set phone account for call.
     *
     * @param account
     *            call via account.
     */
    public void setPhoneAccountForSecondCall(PhoneAccount account) {
        mSecondCallAccount = account;
    }

    /**
     * set phone account for call.
     *
     * @param account
     *            call via account.
     */
    public void setPhoneAccountForThirdCall(PhoneAccount account) {
        mThirdCallAccount = account;
    }

    /**
     * To get phone icon bitmap object of some call.
     *
     * @return bitmap
     */
    public Bitmap getSecondCallPhoneAccountBitmap() {
        if (null != mSecondCallAccount) {
            Icon icon = mSecondCallAccount.getIcon();
            if (icon != null) {
                return icon.getBitmap();
            }
            log("icon is null in phone account: " + mSecondCallAccount);
        }
        return null;
    }

    /**
     * To get phone icon bitmap object of some call.
     *
     * @return bitmap
     */
    public Bitmap getThirdCallPhoneAccountBitmap() {
        if (null != mThirdCallAccount) {
            Icon icon = mThirdCallAccount.getIcon();
            if (icon != null) {
                return icon.getBitmap();
            }
            log("icon is null in phone account: " + mThirdCallAccount);
        }
        return null;
    }

    /**
     * Called when op09 plug in need to show call account icon.
     *
     * @return true if need to show.
     */
    public boolean shouldShowCallAccountIcon() {
        if (null != mTelephonyManager &&
                (mTelephonyManager.getDefault().getPhoneCount() < 2)) {
            return false;
        }
        return true;
    }

    /**
     * To get provider label for call.
     *
     * @return provider label
     */
    public String getSecondCallProviderLabel() {
        if (null != mSecondCallAccount) {
            return mSecondCallAccount.getLabel().toString();
        }
        return null;
    }

    /**
     * To get provider label for call.
     *
     * @return provider label
     */
    public String getThirdCallProviderLabel() {
        if (null != mThirdCallAccount) {
            return mThirdCallAccount.getLabel().toString();
        }
        return null;
    }

    /**
     * simple log info.
     *
     * @param msg need print out string.
     * @return void.
     */
    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}

