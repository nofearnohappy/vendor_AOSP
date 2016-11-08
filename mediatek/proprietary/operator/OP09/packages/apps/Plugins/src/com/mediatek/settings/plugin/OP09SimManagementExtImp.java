package com.mediatek.settings.plugin;

import android.content.Context;
import android.os.SystemProperties;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.PluginImpl;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.settings.ext.DefaultSimManagementExt;

import java.util.List;

/**
 * For settings SIM management feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.ISimManagementExt")
public class OP09SimManagementExtImp extends DefaultSimManagementExt {
    private static final String TAG = "OP09SimManagementExt";

    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_SMS = "sim_sms";

    private Context mContext;
    private int mToCloseSlot = -1;
    private TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;

    /**
     * update the preference screen of sim management.
     * @param context The context
     */
    public OP09SimManagementExtImp(Context context) {
        super();
        mContext = context;
    }

    private void setToClosedSimSlot(int simSlot) {
        Log.d(TAG, "setToClosedSimSlot = " + simSlot);

        SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        TelephonyManager mTelephonyManager;
        int subId = 0;
        int subIdClose;
        int subIdTmp;
        int simCount;
        Boolean result;

        subIdClose = subscriptionManager.getSubIdUsingPhoneId(simSlot);
        //subIdClose = SubscriptionManager.getSubIdUsingSlotId(simSlot);
        mTelephonyManager = TelephonyManager.from(mContext);
        boolean enableBefore = mTelephonyManager.getDataEnabled();
        subId = subscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "setToClosedSimSlot: subId = " + subId + "subId_close=" + subIdClose);
        if (subIdClose != subId) {
            return;
        }
        simCount = mTelephonyManager.getSimCount();
        Log.d(TAG, "setToClosedSimSlot: simCount = " + simCount);

        for (int i = 0; i < simCount; i++) {
            final SubscriptionInfo sir = Utils.findRecordBySlotId(mContext, i);
            if (sir != null) {
                subIdTmp = sir.getSubscriptionId();
                Log.d(TAG, "setToClosedSimSlot: sir subId_t = " + subIdTmp);
                if (subIdTmp != subId) {
                     subscriptionManager.setDefaultDataSubId(subIdTmp);
                     if (enableBefore) {
                         mTelephonyManager.setDataEnabled(subIdTmp, true);
                         mTelephonyManager.setDataEnabled(subId, false);
                     } else {
                         mTelephonyManager.setDataEnabled(subIdTmp, false);
                     }
                }

            }
        }

    }

    /**
     * check all slot radio on.
     * @param context context
     * @return is all slots radio on;
     */
    private boolean isAllSlotRadioOn(Context context) {
        boolean isAllRadioOn = true;
        int[] subs = SubscriptionManager.from(context).getActiveSubscriptionIdList();
        for (int i = 0; i < subs.length; ++i) {
            isAllRadioOn = isAllRadioOn && Utils.isTargetSlotRadioOn(subs[i]);
        }
        Log.d(TAG, "isAllSlotRadioOn(), isAllRadioOn: " + isAllRadioOn);
        return isAllRadioOn;
    }

    /**
     * Called when set radio power state for a specific sub.
     * @param subId  the slot to set radio power state
     * @param turnOn  on or off
     */
    @Override
    public void setRadioPowerState(int subId, boolean turnOn) {
        int slotId = SubscriptionManager.getSlotId(subId);
        TelephonyManager telephonyManager = TelephonyManager.from(mContext);
        Log.d(TAG, "setRadioPowerState, slotId = " + slotId + " subId = " + subId +
              " turnOn = " + turnOn);

        if (telephonyManager.getPhoneCount()
            <= PhoneConstants.MAX_PHONE_COUNT_DUAL_SIM) {
            if (isAllSlotRadioOn(mContext) && (!turnOn)) {
                // Auto open the other card's data connection. when current card is radio off.
                setToClosedSimSlot(slotId);
            }
        }
    }

    @Override
    public void customizeListArray(List<String> strings) {
        Log.i(TAG, "op09 customizeListArray");

        if (strings != null && strings.size() > 1) {
            strings.remove(0);
            Log.i(TAG, "op09 customizeListArray dothings");
        }
    }

    @Override
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> subscriptionInfo) {
        if (subscriptionInfo != null && subscriptionInfo.size() > 1) {
            subscriptionInfo.remove(0);
        }
    }

    @Override
    public int customizeValue(int value) {
        Log.i(TAG, "op09 customizeValue");
        return value + 1;
    }

    @Override
    public void setDataState(int subid) {
        TelephonyManager mTelephonyManager;
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
        mTelephonyManager = TelephonyManager.from(mContext);
        boolean enableBefore = mTelephonyManager.getDataEnabled();
        int mResetSubId = subscriptionManager.getDefaultDataSubId();
        if (subscriptionManager.isValidSubscriptionId(subid) &&
            subid != mResetSubId) {
            subscriptionManager.setDefaultDataSubId(subid);
            if (enableBefore) {
                mTelephonyManager.setDataEnabled(subid, true);
                mTelephonyManager.setDataEnabled(mResetSubId, false);
            }
        }
    }


    @Override
    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo sir, String value) {
        //fragment is detach.
        if (context == null) {
            return sir;
        }
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        SubscriptionInfo sirTmp = sir;
        int type = 0;

        if (value.equals(KEY_CELLULAR_DATA)) {
            type = 2;
        } else if (value.equals(KEY_SMS)) {
            type = 1;
        }

        if (sir == null) {
            List<SubscriptionInfo> subList = subscriptionManager.getActiveSubscriptionInfoList();
            int subCount;
            if (subList == null) {
                subCount = 0;
            } else {
                subCount = subList.size();
            }
            int subId = SubscriptionManager.getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1);
            if (subCount == 1) {
                subId = subList.get(0).getSubscriptionId();
                if (type == 2) {
                    subscriptionManager.setDefaultDataSubId(subId);
                    TelephonyManager.getDefault().setDataEnabled(subId, true);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 2, data sub set to " + subId);
                } else if (type == 1) {
                    //subscriptionManager.setDefaultSmsSubId(subId);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 1, sms sub set to " + subId);
                }
                sirTmp = Utils.findRecordBySubId(context, subId);
            } else if (subCount >= 2) {
                if (type == 2 && SubscriptionManager.getDefaultDataSubId() ==
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    subscriptionManager.setDefaultDataSubId(subId);
                    TelephonyManager.getDefault().setDataEnabled(subId, true);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 2, data sub set to " + subId);
                } else if (type == 1 && SubscriptionManager.getDefaultSmsSubId() ==
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    subscriptionManager.setDefaultSmsSubId(subId);
                    Log.d(TAG, "setDefaultSubId subCount = 1, type = 1, sms sub set to " + subId);
                }
                sirTmp = Utils.findRecordBySubId(context, subId);
            }
        }
        return sirTmp;
    }

    @Override
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccount) {
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        PhoneAccountHandle result = phoneAccount;
        Log.d(TAG, "setDefaultCallValue phoneAccount=" + phoneAccount);
        if (phoneAccount == null) {
            List<PhoneAccountHandle> phoneAccountlist
                = telecomManager.getCallCapablePhoneAccounts();
            int accoutSum = phoneAccountlist.size();

            Log.d(TAG, "setDefaultCallValue accoutSum=" + accoutSum);
            if (accoutSum > 0) {
                 result = phoneAccountlist.get(0);
            }
        }
        Log.d(TAG, "setDefaultCallValue result=" + result);
        return result;
    }

    /**
     * Switch default data sub.
     * @param context context
     * @param subId subscription ID
     * @return true if switch, otherwise false
     */
    @Override
    public boolean switchDefaultDataSub(Context context, int subId) {
        Log.d(TAG, "switchDefaultDataSub subId=" + subId);
        return true;
    }

    /**
     * Called when SIM dialog is about to show for SIM info changed.
     * @return false if plug-in do not need SIM dialog
     */
    @Override
    public boolean isSimDialogNeeded() {
        return false;
    }

    /**
     * Judge if it is CT card.
     * @return true if it is CT card, otherwise false.
     */
    @Override
    public boolean useCtTestcard() {
        return CdmaFeatureOptionUtils.isCTLteTddTestSupport();
    }
}
