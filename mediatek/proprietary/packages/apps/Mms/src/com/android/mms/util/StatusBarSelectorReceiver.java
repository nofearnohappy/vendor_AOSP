package com.android.mms.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Mms;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import android.widget.Toast;
import android.telephony.RadioAccessFamily;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.android.internal.telephony.PhoneConstants;
import com.android.mms.MmsApp;
import com.android.mms.ui.MessageUtils;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import com.android.mms.R;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.telephony.TelephonyManagerEx;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import java.util.Iterator;
import java.util.List;
import android.telecom.PhoneAccount;
import android.text.TextUtils;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;

public class StatusBarSelectorReceiver extends BroadcastReceiver {
    private static final String TAG = "[StatusBarSelectorCreator]StatusBarSelectorReceiver";
    public static final String ACTION_MMS_ACCOUNT_CHANGED = "com.android.mms.ui.ACTION_MMS_ACCOUNT_CHANGED";
    private Activity mActivity;
    private static final String PROPERTY_3G_SIM = "persist.radio.simswitch";

    public StatusBarSelectorReceiver(Activity activity) {
        mActivity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (StatusBarSelectorCreator.ACTION_MMS_ACCOUNT_CHANGED.equals(intent.getAction())) {
            final int currentSubId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            Log.d(TAG, "onReceive, currentSubId = " + currentSubId);
            StatusBarSelectorCreator creator = StatusBarSelectorCreator.getInstance(mActivity);
            creator.hideNotification();
            if (!MessageUtils.isC2KSolution2Support() && isSwitchFromGsmCardToCdmaCard(currentSubId)) {
                if (isReadyToSetRadioCapability()) {
                    if (setRadioCapability(currentSubId)) {
                        SubscriptionManager.from(MmsApp.getApplication()).setDefaultSmsSubId(currentSubId);
                        creator.updateStatusBarData();
                    } else {
                        Log.d(TAG, "onReceive, setCapability failed!");
                    }
                } else {
                    Log.d(TAG, "onReceive, current state can't setCapability");
                }
            } else if (!MessageUtils.isC2KSolution2Support()
                    && isMainCapabilityOnGsmCard()
                    && hasCdmaCardInHomeNetwork()
                    && MessageUtils.isAirplaneModeOn()) {
                showAlertToast(R.string.can_not_switch_account_temporarily);
            } else {
                SubscriptionManager.from(MmsApp.getApplication()).setDefaultSmsSubId(currentSubId);
                creator.updateStatusBarData();
            }
        }
    }

    /**
     * For C2K solution1.5, judge whether changing account from GSM card to CDMA card,
     * need check the following three items:
     * @return true if switch from GSM card to CDMA card
     */
    private boolean isSwitchFromGsmCardToCdmaCard(int currentSubId) {
        int mainCapabilitySlotId = MessageUtils.getMainCapabilitySlotId();
        int currentSlotId = SubscriptionManager.getSlotId(currentSubId);
        int mainCapabilitySlotType = SvlteUiccUtils.getInstance().getSimType(mainCapabilitySlotId);
        int currentSlotType = SvlteUiccUtils.getInstance().getSimType(currentSlotId);
        Log.d(TAG, "isSwitchFromGsmCardToCdmaCard, mainCapabilitySlotType = "
                + mainCapabilitySlotType + " currentSlotType = " + currentSlotType
                + " isInhomeNetwork = " + TelephonyManagerEx.getDefault().isInHomeNetwork(currentSubId));
        if (mainCapabilitySlotType == SvlteUiccUtils.SIM_TYPE_GSM
                && currentSlotType == SvlteUiccUtils.SIM_TYPE_CDMA
                && TelephonyManagerEx.getDefault().isInHomeNetwork(currentSubId)) {
            return true;
        }
        return false;
    }

    private boolean hasCdmaCardInHomeNetwork() {
        List<SubscriptionInfo> list = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfoList();
        if (list == null) {
            return false;
        }
        for (SubscriptionInfo subInfo : list) {
            int subId = subInfo.getSubscriptionId();
            int slotId = SubscriptionManager.getSlotId(subId);
            int slotType = SvlteUiccUtils.getInstance().getSimType(slotId);
            if (slotType == SvlteUiccUtils.SIM_TYPE_CDMA &&
                    TelephonyManagerEx.getDefault().isInHomeNetwork(subId)) {
                Log.d(TAG, "hasCdmaCardInHomeNetwork true");
                return true;
            }
        }
        return false;
    }

    private boolean isMainCapabilityOnGsmCard() {
        int mainCapabilitySlotId = MessageUtils.getMainCapabilitySlotId();
        int mainCapabilitySlotType = SvlteUiccUtils.getInstance().getSimType(mainCapabilitySlotId);
        Log.d(TAG, "canSwithWhen4GOnGsmCard, mainCapabilitySlotType = "
                + mainCapabilitySlotType);
        if (mainCapabilitySlotType == SvlteUiccUtils.SIM_TYPE_GSM) {
            return true;
        }
        return false;
    }

    /**
    * set the 3G/4G capability if that be permitted
    * @return true if switch 3G/4G capability successfully
            */
   private boolean isReadyToSetRadioCapability() {
       ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
               ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
       try {
           if (iTelEx != null && iTelEx.isCapabilitySwitching()) {
               showAlertToast(R.string.can_not_switch_account_temporarily);
               return false;
           }
       } catch (RemoteException e) {
           e.printStackTrace();
           Log.d(TAG, "fail to judge isCapabilitySwitching, RemoteException");
       }

       if (TelecomManager.from(MmsApp.getApplication()).isInCall()) {
           showAlertToast(R.string.can_not_switch_account_during_call);
           return false;
       }
       if (MessageUtils.isAirplaneModeOn()) {
           showAlertToast(R.string.can_not_switch_account_temporarily);
           return false;
       }
       return true;
        }

    private void showAlertToast(int resId) {
        String textErr = MmsApp.getApplication().getResources().getString(resId);
        Toast.makeText(mActivity, textErr, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the 3G/4G capability of the SIM card
     * @return true if switch 3G/4G capability successfully
     */
    private boolean setRadioCapability(int currentSubId) {
        int phoneNum = TelephonyManager.from(MmsApp.getApplication()).getPhoneCount();
        int[] phoneRat = new int[phoneNum];
        boolean isSwitchSuccess = true;
        int phoneId = SubscriptionManager.getPhoneId(currentSubId);

        Log.d(TAG, "setCapability: " + phoneId);

        String curr3GSim = SystemProperties.get(PROPERTY_3G_SIM, "");
        Log.d(TAG, "current 3G Sim = " + curr3GSim);

        if (curr3GSim != null && !curr3GSim.equals("")) {
            int curr3GSlotId = Integer.parseInt(curr3GSim);
            if (curr3GSlotId == (phoneId + 1)) {
                Log.d(TAG, "Current 3G phone equals target phone, don't trigger switch");
                return isSwitchSuccess;
            }
        }

        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));

            if (null == iTel || null == iTelEx) {
                Log.e(TAG, "Can not get phone service");
                return false;
            }

            int currRat = iTel.getRadioAccessFamily(phoneId, MmsApp.getApplication().getPackageName());
            Log.d(TAG, "Current phoneRat:" + currRat);

            RadioAccessFamily[] rat = new RadioAccessFamily[phoneNum];
            for (int i = 0; i < phoneNum; i++) {
                if (phoneId == i) {
                    Log.d(TAG, "SIM switch to Phone" + i);
                    if (MessageUtils.isLteSupport()) {
                        phoneRat[i] = RadioAccessFamily.RAF_LTE
                                | RadioAccessFamily.RAF_UMTS
                                | RadioAccessFamily.RAF_GSM;
                    } else {
                        phoneRat[i] = RadioAccessFamily.RAF_UMTS
                                | RadioAccessFamily.RAF_GSM;
                    }
                } else {
                    phoneRat[i] = RadioAccessFamily.RAF_GSM;
                }
                rat[i] = new RadioAccessFamily(i, phoneRat[i]);
            }
            if (false  == iTelEx.setRadioCapability(rat)) {
                Log.d(TAG, "Set phone rat fail!!!");
                isSwitchSuccess = false;
        }
        } catch (RemoteException ex) {
            Log.d(TAG, "Set phone rat fail!!!");
            ex.printStackTrace();
            isSwitchSuccess = false;
        }
        Log.d(TAG, "setRadioCapability isSwitchSuccess = " + isSwitchSuccess);
        return isSwitchSuccess;
    }
}
