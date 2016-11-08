package com.mediatek.settings.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.op09.plugin.R;
/**
* Show sim alert dialog.
*/
public class SimDialogService extends Service {

    private static final String TAG = "SimDialogService";
    public static final String ACTION_START_SELF =
        "com.mediatek.intent.action.STARTSELF_SIM_DIALOG_SERVICE";

    public static final String EXTRA_NAME = "extra_name";
    public static final String TEXT = "text";

    public static final String ACTION_NAME = "com.mediatek.OP09.SIM_DIALOG_SERVICE";
    private static final String UIM_CHANGE_ALERT_ACTIVITY_NAME
        = "com.mediatek.OP09.UIM_CHANGE_ALERT";
    private boolean mSimtype = false;
    private boolean mUserpresent = false;
    private String mText;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (intent != null) {
            Intent receiverIntent = (Intent) intent.getParcelableExtra(EXTRA_NAME);
            String action = receiverIntent.getAction();
            Log.d(TAG, "receiverIntent action = " + action);
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserpresent = true;
                Log.d(TAG, "PRESENT mSimtype = " + mSimtype + "mUserpresent =" + mUserpresent);
                showAlert();
            } else if (TelephonyIntents.ACTION_CDMA_CARD_TYPE.equals(action)) {
                CardType cardType = (CardType) receiverIntent
                        .getExtra(TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
                Log.i(TAG, "cardType = " + cardType.toString());
                Log.d(TAG, "CARDTYPE mSimtype = " + mSimtype + "mUserpresent =" + mUserpresent);
                if (IccCardConstants.CardType.CARD_NOT_INSERTED.equals(cardType)) {
                    String sim2Info = null;
                    sim2Info = SystemProperties.get("gsm.ril.fulluicctype.2");
                    Log.i(TAG, "sim2Info = +" + sim2Info + "+");
                    if ((sim2Info == null) || (sim2Info.equals(""))) {
                        Log.i(TAG, "not insert sim2");
                        mText = getString(R.string.no_sim_dialog_message);
                        mSimtype = true;
                    }

                } else if (!(IccCardConstants.CardType.PIN_LOCK_CARD).equals(cardType)
                        && !(IccCardConstants.CardType.CT_4G_UICC_CARD).equals(cardType)) {
                    mText = getString(R.string.lte_sim_dialog_message);
                    mSimtype = true;
                } else if ((IccCardConstants.CardType.CT_4G_UICC_CARD).equals(cardType)) {
                    mText = null;
                    mSimtype = false;
                }
                Log.d(TAG, "mText = " + mText);
                showAlert();
            } else if (action.equals(SimDialogService.ACTION_START_SELF)) {
                Log.d(TAG, "recv  ACTION_START_SELF");
                mSimtype = false;
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showAlert() {
        Log.d(TAG, "showAlert mSimtype = " + mSimtype + "mUserpresent="
            + mUserpresent + " mText = " + mText);
        if (mText != null && mSimtype && mUserpresent) {
            Intent launchIntent = new Intent(UIM_CHANGE_ALERT_ACTIVITY_NAME);
            launchIntent.setPackage(getPackageName());
            launchIntent.putExtra(TEXT, mText);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        }
    }
}
