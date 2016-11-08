package com.mediatek.rcs.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;

public class ContactsReceiver extends BroadcastReceiver {

    private static final String TAG = "ContactsReceiver";
    public static final String RCS_ICC_ID = "curIccId";
    public static final String INTENT_RCS_LOGIN = "com.mediatek.rcs.contacts.INTENT_RCS_LOGIN";

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        Log.d(TAG, "onReceive() action = " + action);

        // if default data sub id changed
        if (TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
            Log.d(TAG, "onReceive(), default data sub id changed");
            checkAccountChange();
        }
    }

    private boolean checkAccountChange() {
        int subId = getCurRcsSubId();
        Log.i(TAG, "checkAccountChange() subId = " + subId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.i(TAG, "checkAccountChange() invalid subId!");
            return false;
        }

        SubscriptionInfo subInfo =
            SubscriptionManager.from(mContext).getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            String iccId = subInfo.getIccId();
            SharedPreferences sh =
                mContext.getSharedPreferences("rcs_icc_id", mContext.MODE_WORLD_READABLE);
            String curIccId = sh.getString(RCS_ICC_ID, "");
            Log.i(TAG, "checkAccountChange() curIccId = " + curIccId + ",newiccId = " + iccId);
            if (iccId != null && !iccId.equals(curIccId)) {
                Editor editor = sh.edit();
                editor.putString(RCS_ICC_ID, iccId);
                editor.commit();

                //notify delete old account data 
                Log.i(TAG, "checkAccountChange(), account changed, start wipe");
                Intent intent = new Intent(INTENT_RCS_LOGIN);
                intent.putExtra("iccId", iccId);
                intent.putExtra("subId", subId);
                mContext.sendBroadcast(intent);
                return true;
            }
        }
        return false;
    }

    private int getCurRcsSubId() {
        int subId = SubscriptionManager.getDefaultDataSubId();
        return subId;
    }

}

