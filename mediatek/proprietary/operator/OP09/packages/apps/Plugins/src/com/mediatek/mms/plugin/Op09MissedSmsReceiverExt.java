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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.util.Log;

import com.mediatek.mms.callback.ISmsReceiverServiceCallback;


/**
 * M: Op09MissedSmsReceiverExt.
 */
public class Op09MissedSmsReceiverExt {
    private static final String TAG = "Mms/Op09MissedSmsReceiverExt";
    private Context mContext;
    private boolean mIsWholeSms;

    private static Op09MissedSmsReceiverExt sOp09MissedSmsReceiverExt;

    public static Op09MissedSmsReceiverExt getIntance(Context context) {
        if (sOp09MissedSmsReceiverExt == null) {
            sOp09MissedSmsReceiverExt = new Op09MissedSmsReceiverExt(context);
        }
        return sOp09MissedSmsReceiverExt;
    }

    /**
     * M: The Constructor.
     * @param context the Context.
     */
    public Op09MissedSmsReceiverExt(Context context) {
        mContext = context;
        mIsWholeSms = true;
    }


    public Uri updateMissedSms(Context context, SmsMessage[] msgs, int error,
            ISmsReceiverServiceCallback callback) {
        Log.d("@M_" + TAG, "MissedSmsReceiverExt.updateMissedSms");

        SmsMessage smsTmp = msgs[0];
        int pduCount = msgs.length;
        int refId = smsTmp.getUserDataHeader().concatRef.refNumber;
        Log.d("@M_" + TAG, "pduCount=" + pduCount + " refId=" + refId);
        Uri missedSmsUri = findMissedSms(refId, pduCount);

        if (missedSmsUri != null) {
            Log.d("@M_" + TAG, "Find missed Sms: " + missedSmsUri.toString());
            return handleUpdate(msgs, pduCount, missedSmsUri);
        } else { // Original SMS may be deleted.
            return callback.callStoreMessage(context, msgs, error);
        }
    }

    /**
     * M: handle update .
     * @param msgs smses.
     * @param pduCount the pdu count.
     * @param missedSmsUri the uri for missed sms.
     * @return the new sms uri.
     */
    private Uri handleUpdate(SmsMessage[] msgs, int pduCount, Uri missedSmsUri) {
        // Build up the body from the parts.
        StringBuilder body = new StringBuilder();
        if (!mIsWholeSms) {
            body.append(MessageUtils.handleMissedParts(msgs));
        } else {
            for (int i = 0; i < pduCount; i++) {
                SmsMessage msg;
                msg = msgs[i];
                body.append(msg.getDisplayMessageBody());
            }
        }

        ContentValues values = new ContentValues(2);
        values.put(Sms.BODY, body.toString());
        values.put(Sms.RECEIVED_LENGTH, pduCount);
        int ret = SqliteWrapper.update(mContext, mContext.getContentResolver(), missedSmsUri,
            values, null, null);
        if (ret == 1) {
            return missedSmsUri;
        } else {
            Log.e("@M_" + TAG, "Update Sms error!");
            return null;
        }
    }

    /**
     * M: find out the sms in database according to the Reference number.
     * @param refId the reference id.
     * @param newPduCount new pdu count.
     * @return the sms uri.
     */
    private Uri findMissedSms(int refId, int newPduCount) {
        int totalCount = 0;

        String where = Sms.REFERENCE_ID + " = ?";
        Cursor cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
            Sms.CONTENT_URI, new String[] {Sms._ID, Sms.TOTAL_LENGTH}, where, new String[] {String
                    .valueOf(refId)}, null);

        if (cursor == null || cursor.getCount() != 1) {
            Log.e("@M_" + TAG, "cursor == " + (cursor == null ? "NULL" : cursor.getCount()));
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                totalCount = cursor.getInt(1);
                Log.d("@M_" + TAG, "totalCount: " + totalCount + " newPduCount: " + newPduCount);

                if (newPduCount > totalCount) {
                    Log.e("@M_" + TAG, "Wrong Pdu Count!");
                    return null;
                }

                if (newPduCount < totalCount) {
                    mIsWholeSms = false;
                    Log.d("@M_" + TAG, "Not whole SMS! totalCount should be: " + totalCount);
                }

                int id = cursor.getInt(0);
                return Uri.parse(Sms.CONTENT_URI + "/" + id);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

}
