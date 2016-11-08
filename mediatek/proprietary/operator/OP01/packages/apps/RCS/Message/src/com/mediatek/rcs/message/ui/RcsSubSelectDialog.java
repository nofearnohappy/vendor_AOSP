
package com.mediatek.rcs.message.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.rcs.message.R;

public class RcsSubSelectDialog {

    private int mSelectedSubId;
    private List<SubscriptionInfo> mSubListInfo = new ArrayList<SubscriptionInfo>();
    private List<SubscriptionInfo> mEmptySubListInfo = new ArrayList<SubscriptionInfo>();
    private RcsSubChooseAdapter mSubAdapter;
    private Context mContext;
    private Context hostContext;
//    private SubClickAndDismissListener mListener;

    public RcsSubSelectDialog(Context context, Context mHostContext) {
        mContext = context;
        hostContext = mHostContext;
//        mListener = listener;
    }

    private void updateSubInfoList() {
        int simCount = TelephonyManager.getDefault().getSimCount();
        mSubListInfo.clear();
        mEmptySubListInfo.clear();
        for (int slotId = 0; slotId < simCount; slotId++) {
            SubscriptionInfo subInfoRecordInOneSim = SubscriptionManager.from(
                    hostContext).getActiveSubscriptionInfoForSimSlotIndex(slotId);
            if (subInfoRecordInOneSim != null) {
                mSubListInfo.add(subInfoRecordInOneSim);
            } else {

            }
        }
        if (mSubListInfo == null || mSubListInfo.size() == 0) {
            return;
        }
        for (int i = 0; i < mEmptySubListInfo.size(); i++) {
            mSubListInfo.add(mEmptySubListInfo.get(i));
        }
    }

    public AlertDialog showSubSelectedDialog() {
        final int activeSimCount = getActiveSimCount(mContext);
        if (activeSimCount > 1) {
            // SUB selection, always ask, show select SIM dialog even only 1
            // SIM inserted.
            updateSubInfoList();
            AlertDialog.Builder b = new AlertDialog.Builder(hostContext);
            mSubAdapter = new RcsSubChooseAdapter(mContext, mSubListInfo);

            b.setTitle(mContext.getString(R.string.sim_selected_dialog_title));

            b.setCancelable(true);
            b.setAdapter(mSubAdapter, new DialogInterface.OnClickListener() {
                @SuppressWarnings("unchecked")
                public final void onClick(DialogInterface dialog, int which) {
                    SubscriptionInfo subInfoRecord = mSubListInfo.get(which);
                    if (subInfoRecord != null) {
                        dialog.dismiss();
                        mSelectedSubId = subInfoRecord.getSubscriptionId();
//                        mListener.onDialogClick(mSelectedSubId, intent);
                        Intent intent = new Intent();
                        intent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
                        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSelectedSubId);
                        hostContext.startActivity(intent);

                    }
                }
            });
            b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    mListener.onCancelClick();
                    dialog.dismiss();
                }
            });

//            b.setOnDismissListener(new OnDismissListener() {
//
//                @Override
//                public void onDismiss(DialogInterface dialog) {
////                    mListener.onDialogDismiss();
//                }
//            });
            AlertDialog subSelectDialog = b.create();
            subSelectDialog.show();
            return subSelectDialog;
        } else {
//            mSelectedSubId = SubscriptionManager.getDefaultSmsSubId();
//            mListener.onDialogClick(mSelectedSubId, intent);
            return null;
        }
    }

    public int getActiveSimCount(Context context) {
        List<SubscriptionInfo> subInfoRecords = SubscriptionManager.from(hostContext)
                .getActiveSubscriptionInfoList();
        return subInfoRecords != null ? subInfoRecords.size() : 0;
    }

}
