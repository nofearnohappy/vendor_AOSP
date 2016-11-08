package com.mediatek.mms.plugin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.internal.telephony.IccSmsStorageStatus;
import com.mediatek.mms.callback.ITextSizeAdjustHost;
import com.mediatek.mms.ext.DefaultOpManageSimMessagesExt;
import android.util.Log;

import java.util.Iterator;
import java.util.Map.Entry;

import com.mediatek.telephony.TelephonyManagerEx;

import com.mediatek.op09.plugin.R;

public class Op09ManageSimMessagesExt extends DefaultOpManageSimMessagesExt {

    private static final String TAG = "Mms/Op09ManageSimMessagesExt";

    private Context mContext = null;
    private Op09MmsTextSizeAdjustExt mTextSizeAdjuster;
    private int mCurrentSubId = 0;

    private Activity mActivity;

    /**
     * M: The Constructor.
     *
     * @param context
     *            the Context.
     */
    public Op09ManageSimMessagesExt(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * M: Is whether the activated message or not.
     *
     * @param index
     *            the message's id.
     * @return ture: activated message. false: no.
     */
    private boolean isUnactivatedMessage(int index) {
        int temp = (index & (0x01 << 10));
        return (temp == (0x01 << 10));
    }

    public boolean isInternationalCard(int subId) {
        SubscriptionInfo sir = MessageUtils.getSimInfoBySubId(mContext, subId);
        if (sir == null || sir.getSimSlotIndex() != 0) {
            Log.d("@M_" + TAG, "[isInternationalCard],failed. Just return false.");
            return false;
        }
        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_CDMA_CARD_TYPE);
        Intent intent = mContext.registerReceiver(null, intentFilter);
        if (intent == null) {
            Log.d("@M_" + TAG, "[isInternationalCard]:failed. intent == null;");
            return false;
        }
        Bundle bundle = intent.getExtras();
        IccCardConstants.CardType cardType = (IccCardConstants.CardType) bundle
                .get(TelephonyIntents.INTENT_KEY_CDMA_CARD_TYPE);
        boolean isDualSim = cardType == IccCardConstants.CardType.CT_UIM_SIM_CARD;
        Log.d("@M_" + TAG, "[isInternationalCard]:" + isDualSim);
        return isDualSim;
    }

    public boolean canBeOperated(Cursor cursor) {
        if (cursor == null) {
            return false;
        }
        try {
            int index = cursor.getInt(cursor.getColumnIndex("index_on_icc"));
            Log.d("@M_" + TAG, "canBeOperated: index:" + index);
            return !isUnactivatedMessage(index);
        } catch (SQLiteException e) {
            Log.d("@M_" + TAG, "error to canBeOperated");
        }
        return true;
    }

    public boolean hasIncludeUnoperatedMessage(Iterator<Entry<String, Boolean>> it) {
        if (it == null) {
            return false;
        }
        while (it.hasNext()) {
            Entry<String, Boolean> entry = (Entry<String, Boolean>) it.next();
            if (entry.getValue()) {
                if (isUnactivatedMessage(Integer.parseInt(entry.getKey()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String[] onMultiDelete(String[] simMsgIndex) {
        // TODO Auto-generated method stub
        if (simMsgIndex == null || simMsgIndex.length < 1) {
            return simMsgIndex;
        }
        String[] temp = new String[simMsgIndex.length];
        int index = 0;
        for (String msgIndex : simMsgIndex) {
            if (!isUnactivatedMessage(Integer.parseInt(msgIndex))) {
                temp[index] = msgIndex;
                index++;
            }
        }
        return temp;
    }

    public Uri getAllContentUriForInternationalCard(int subId) {
        int slotId = MessageUtils.getSimInfoBySubId(mContext, subId).getSimSlotIndex();
        if (slotId == 0) {
            return Uri.parse("content://sms/icc_international");
        } else if (slotId == 1) {
            return Uri.parse("content://sms/icc2_international");
        }
        return null;
    }

    @Override
    public String checkSimCapacity(IccSmsStorageStatus simStatus, String defaultMessage) {
        String message = defaultMessage;
        if (simStatus == null) {
            String ctString = getResources().getString(R.string.get_uim_capacity_failed);
            if (ctString != null && isUSimType(mCurrentSubId)) {
                message = ctString;
            }
        } else if (isInternationalCard(mCurrentSubId)) {
            message = message + "\n"
                    + getResources().getString(R.string.capacity_sim_card_due_mode);
        }
        return message;
    }

    @Override
    public void confirmMultiDelete(AlertDialog.Builder builder,
            Iterator<Entry<String, Boolean>> it) {
        if (it != null && hasIncludeUnoperatedMessage(it)) {
            builder.setMessage(getResources().getString(R.string.confirm_delete_selected_messages));
        }

    }

    @Override
    public void confirmDeleteDialog(AlertDialog.Builder builder) {
        if (isUSimType(mCurrentSubId)) {
            String ctString = getResources().getString(R.string.confirm_delete_uim_msg);
            if (isUSimType(mCurrentSubId)) {
                builder.setMessage(ctString);
            }
        }
    }
    public static boolean isUSimType(int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            return false;
        }
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
                || phoneType.equalsIgnoreCase("RUIM");
    }

    @Override
    public boolean onCreateContextMenu(Cursor cursor) {
        if (!canBeOperated(cursor)) {
            Log.d(TAG, "canBeOperated");
            Toast.makeText(this, getResources().getString(R.string.message_cannot_be_operated),
                    Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    @Override
    public Uri startQueryIcc(Uri defaultQueryUri) {
        if (isInternationalCard(mCurrentSubId)) {
            return getAllContentUriForInternationalCard(mCurrentSubId);
        } else {
            return defaultQueryUri;
        }
    }

    @Override
    public void onCreate(ITextSizeAdjustHost host, Activity activity, int subId) {
        mTextSizeAdjuster = Op09MmsTextSizeAdjustExt.getInstance();
        mTextSizeAdjuster.init(host, activity);
        mCurrentSubId = subId;
        mActivity = activity;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mTextSizeAdjuster.dispatchTouchEvent(event);
    }

    @Override
    public void updateListWithCursor(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mTextSizeAdjuster.refresh();
        }
    }

    @Override
    public boolean updateState(int state, TextView view) {
        if (!isUSimType(mCurrentSubId)) {
            return false;
        }
        String title = getResources().getString(R.string.manage_uim_messages_title);
        if (state == 0) {
            mActivity.setTitle(title);
        } else if (state == 1 && view != null) {
            mActivity.setTitle(title);
            view.setText(getResources().getString(R.string.uim_empty));
        }
        return true;
    }

}
