package com.mediatek.incallui.plugin;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.android.incallui.CallerInfo;
import com.mediatek.incallui.ext.DefaultCallCardExt;
import com.mediatek.common.PluginImpl;

/**
 * Created by mtk54152 on 4/27/15.
 */

@PluginImpl(interfaceName ="com.mediatek.incallui.ext.ICallCardExt")
public class Op03CallCardExt extends DefaultCallCardExt {
	
	  private final static String TAG = "Op03CallCardExt";

    private static final int SLOT_ID1 = com.android.internal.telephony.PhoneConstants.SIM_ID_1;
   	private static final int SLOT_ID2 = com.android.internal.telephony.PhoneConstants.SIM_ID_2;

    private static final int SIM_TYPE_SIM = 0;
    private static final int SIM_TYPE_USIM = 1;
    private static final int SIM_TYPE_UIM = 2;
    
    private static final String SIM_TYPE_USIM_TAG = "USIM";
    private static final String SIM_TYPE_UIM_TAG = "UIM";

    private static final String ACCOUNT_TYPE_SIM = "SIM Account";
    private static final String ACCOUNT_TYPE_USIM = "USIM Account";
    private static final String ACCOUNT_TYPE_UIM = "UIM Account";

    private static final int TYPE_AAS = 101;
    
     	  
    @Override
    public void customizeCallerInfo(Context context, Object callerInfoObj, Cursor cursor) {   	
    	CallerInfo callerInfo = (CallerInfo)callerInfoObj;
 
        int subId = SubscriptionManager.getDefaultSubId();
        int numberType = callerInfo.numberType;
        String numberLabel = callerInfo.numberLabel;
        
        int slotId = SubscriptionManager.getSlotId(subId);                      
        String accountType = getAccountTypeBySubId(slotId, subId);
 
        Log.i(TAG, "[getTypeLabel] accountType=" + accountType + ", slotId=" + slotId + ", numberType=" + numberType + ",numberLabel= " + numberLabel + ", subId=" + subId);
        if (ACCOUNT_TYPE_SIM.equals(accountType)) {// Hide phone type if SIM card
           callerInfo.phoneLabel = "";
           return;
        } else if (ACCOUNT_TYPE_USIM.equals(accountType) && numberType == TYPE_AAS) {
            if (TextUtils.isEmpty(numberLabel)) {
                Log.i(TAG, "[getTypeLabel] Type aas but label index is empty.");
                callerInfo.phoneLabel = "";
                return;
            }
            try {
                Integer aasIdx = Integer.valueOf(numberLabel);                         
                callerInfo.phoneLabel = getAasTypeLabel(context, aasIdx.intValue(), slotId, subId);
                return;
            } catch (NumberFormatException e) {
                Log.e(TAG, "[getTypeLabel] return numberLabel=" + numberLabel);
                callerInfo.phoneLabel = numberLabel;
                return;
            }
        }
        Log.i(TAG, "[getTypeLabel] get default label");
    }


    /**
     * Return AAS label for the USim account.
     */
    private String getAasTypeLabel(Context context, int aasIdx, int slotId, int subId) {
        String aasLabel = "";
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2 || aasIdx < 1) {
            Log.i(TAG, "[getAasTypeLabel] slotId=" + slotId + " aasIdx=" + aasIdx);
            return aasLabel;
        }

        try {
            final IIccPhoneBook iIccPhb = getIIccPhoneBook(slotId);
            if (iIccPhb != null) {
                aasLabel = iIccPhb.getUsimAasById(subId, aasIdx);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[getAasTypeLabel] exception.");
        }

        if (aasLabel == null) {
            aasLabel = "";
        }
        Log.i(TAG, "[getAasTypeLabel] aasLabel=" + aasLabel);
        return aasLabel;
    }

    private IIccPhoneBook getIIccPhoneBook(int slotId) {
        String serviceName = "simphonebook";
        final IIccPhoneBook iIccPhb = IIccPhoneBook.Stub.asInterface(ServiceManager.getService(serviceName));
        return iIccPhb;
    }

    private String getAccountTypeBySubId(int slotId, int subId) {
        Log.i(TAG, "[getAccountTypeBySubId] slotId:" + slotId);
        if (slotId < SLOT_ID1 || slotId > SLOT_ID2) {
            Log.e(TAG, "[getAccountTypeBySubId]Error slotid:" + slotId);
            return null;
        }
        int simtype = SIM_TYPE_SIM;
        String simAccountType = ACCOUNT_TYPE_SIM;

        if (isSimInserted(slotId)) {
            simtype = getSimTypeBySubId(subId);
            if (SIM_TYPE_USIM == simtype) {
                simAccountType = ACCOUNT_TYPE_USIM;
            } else if (SIM_TYPE_UIM == simtype) { // UIM
                simAccountType = ACCOUNT_TYPE_UIM;
            }
        } else {
            Log.e(TAG, "[getAccountTypeBySubId]Error slotId:" + slotId + " no sim inserted!");
            simAccountType = null;
        }
        Log.i(TAG, "[getAccountTypeBySlot] accountType:" + simAccountType);
        return simAccountType;
    }

    private boolean isSimInserted(int slotId) {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        boolean isSimInsert = false;
        try {
            if (iTel != null) {             
                    isSimInsert = iTel.hasIccCardUsingSlotId(slotId);             
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            isSimInsert = false;
        }
        return isSimInsert;
    }

    private int getSimTypeBySubId(int subId) {
        final ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        int simType = SIM_TYPE_SIM;
        try {         
                if (SIM_TYPE_USIM_TAG.equals(iTel.getIccCardType(subId))) {
                    simType = SIM_TYPE_USIM;
                } else if (SIM_TYPE_UIM_TAG.equals(iTel.getIccCardType(subId))) {
                    simType = SIM_TYPE_UIM;
                }
        } catch (RemoteException e) {
            Log.e(TAG, "[getSimTypeBySubId] catched exception.");
            e.printStackTrace();
        }
        Log.i(TAG, "[getSimTypeBySubId] simType=" + simType);
        return simType;
    }
}
