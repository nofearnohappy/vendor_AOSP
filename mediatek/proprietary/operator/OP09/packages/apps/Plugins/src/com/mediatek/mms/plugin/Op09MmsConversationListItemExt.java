/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mms.plugin;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.op09.plugin.R;

/**
 * M: Op09MmsConversationListItemExt.
 */
public class Op09MmsConversationListItemExt{
    private static final String TAG = "Mms/OP09MmsConversationListItemExt";
    private static final int WAIT_TIME = 1300;
    private int mSimId = -1;
    private int[] mSimInfo = null;
    private Context mContext;

    private static Op09MmsConversationListItemExt sOp09MmsConversationListItemExt;

    public static Op09MmsConversationListItemExt getIntance(Context context) {
        if (sOp09MmsConversationListItemExt == null) {
            sOp09MmsConversationListItemExt = new Op09MmsConversationListItemExt(context);
        }
        return sOp09MmsConversationListItemExt;
    }


    /**
     * Constructor.
     * @param context the Context.
     */
    public Op09MmsConversationListItemExt(Context context) {
        mContext= context;
    }


    public int getMessageCountAndShowSimType(Context context, Uri conversationUri,
            ImageView imageView, int recipientCount, boolean hasDraft) {
        if (context == null || conversationUri == null) {
            return -1;
        }
        int subId = getSubIdByThreadId(context, conversationUri);
        Log.d("@M_" + TAG, "[getMessageCountAndShowSimType], subId:" + subId + ", hasDraft:" + hasDraft);

        SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(
            subId);

        Drawable simTypeDraw = null;
        if (subInfo != null) {
            Bitmap origenBitmap = subInfo.createIconBitmap(context);
            Bitmap bitmap = MessageUtils.resizeBitmap(mContext, origenBitmap);
            Log.d("@M_" + TAG, "showSimType for MessageListItem");
            simTypeDraw = new BitmapDrawable(context.getResources(), bitmap);
            } else {
                simTypeDraw = context.getResources().
                        getDrawable(R.drawable.sim_indicator_no_sim_mms);
            }
            if (imageView != null && (hasDraft || simTypeDraw == null)) {
                imageView.setVisibility(View.GONE);
            } else if (!hasDraft && imageView != null && simTypeDraw != null) {
                imageView.setImageDrawable(simTypeDraw);
                imageView.setVisibility(View.VISIBLE);
        }
        if (recipientCount > 1) {
            int allSms = mSimInfo[0];
            int unreadSms = mSimInfo[1];
            int allMms = mSimInfo[2];
            int unreadMms = mSimInfo[3];
            if (unreadSms > 0) {
                return (int) Math.ceil((double) unreadSms / (double) recipientCount)
                    + (unreadMms > 0 ? unreadMms : 0);
            } else if (unreadMms > 0) {
                return unreadMms;
            } else if (allSms > 0) {
                return (int) Math.ceil((double) allSms / (double) recipientCount) + allMms;
            } else {
                return allSms + allMms;
            }
        }
        return -1;
    }

    /**
     * M: get message's count data.
     *
     * @param context the context.
     * @param conversationUri the converation's uri.
     * @return [0]: all sms count;[1]unread Sms count; [2]: all Mms count; [3]: unread Mms count.
     */
    private int getSubIdByThreadId(final Context context, final Uri conversationUri) {

        mSimInfo = new int[5];
        final Object object = new Object();
        Runnable newRunnable = new Runnable() {
            public void run() {
                if (conversationUri != null) {
                    String newUriStr = "content://mms-sms/conversations/simid/"
                            + Long.parseLong(conversationUri.getLastPathSegment());
                    Cursor cursor = context.getContentResolver().query(Uri.parse(newUriStr),
                        null, null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                mSimId = cursor.getInt(4);
                                mSimInfo[0] = cursor.getInt(0);
                                mSimInfo[1] = cursor.getInt(1);
                                mSimInfo[2] = cursor.getInt(2);
                                mSimInfo[3] = cursor.getInt(3);
                                mSimInfo[4] = cursor.getInt(4);
                            } else {
                                mSimId = -1;
                            }
                        } finally {
                            cursor.close();
                            synchronized (object) {
                                object.notifyAll();
                            }
                        }
                    }
                } else {
                    mSimId = -1;
                    synchronized (object) {
                        object.notifyAll();
                    }
                }
            }
        };

        new Thread(newRunnable).start();
        synchronized (object) {
            try {
                object.wait(WAIT_TIME);
            } catch (InterruptedException e) {
                Log.e("@M_" + TAG, "getSubIdByThreadId Exception", e);
            }
        }
        return mSimId;
    }

    /**
     * M: Get slot Id by sub Id.
     * @param ctx the Context.
     * @param subId the sim's subId.
     * @return the slot of the SIM Card, -1 indicate that the SIM card is missing
     */
    public static int getSlotById(Context ctx, long subId) {
        SubscriptionInfo sunInfoRecord = SubscriptionManager.from(ctx).getActiveSubscriptionInfo(
            (int) subId);
        if (sunInfoRecord != null) {
            return sunInfoRecord.getSimSlotIndex();
        }
        return -1;
    }


    public boolean setViewSize(TextView textView) {
        if (textView == null) {
            return false;
        }
        ViewGroup.LayoutParams lp = textView.getLayoutParams();
        lp.width = mContext.getResources().getDimensionPixelOffset(
            R.dimen.ct_conversation_list_item_subject_max_len);
        textView.setLayoutParams(lp);
        return true;
    }

}
