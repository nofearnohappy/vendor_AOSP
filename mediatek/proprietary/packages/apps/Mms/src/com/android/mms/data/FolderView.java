/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mms.data;

import java.util.Map;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.R;

import com.android.mms.ui.MessageUtils;
import com.android.mms.util.MuteCache;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduPersister;


/// M:
public class FolderView {
    //add for folderview mode
    private static final String TAG = "FolderView";
    private static final Uri THREAD_URI_FOR_RECEIPIENTS =
        Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();

    private static final Uri DRAFTFOLDER_URI   = Uri.parse("content://mms-sms/draftbox");
    private static final Uri INBOXFOLDER_URI   = Uri.parse("content://mms-sms/inbox");
    private static final Uri OUTBOXFOLDER_URI  = Uri.parse("content://mms-sms/outbox");
    private static final Uri SENDBOXFOLDER_URI = Uri.parse("content://mms-sms/sentbox");


    private static final int ID             = 0;
    private static final int THREAD_ID      = 1;
    private static final int ADDRESS        = 2;
    private static final int SUBJECT        = 3;
    private static final int DATE           = 4;
    private static final int READ           = 5;
    private static final int TYPE           = 6;
    private static final int STATUS         = 7;
    private static final int ATTACHMENT     = 8;
    private static final int M_TYPE         = 9;
    private static final int SUB_ID         = 10;
    private static final int BOXTYPE        = 11;
    private static final int SUB_CS         = 12;
    private static final int LOCKED         = 13;

    private int        mId;                   // The  update id.
    private long       mThreadId;
    private String     mSubject;              // The  update Subject.
    private long       mDate;                 // The  update time.
    private boolean    mHasAttachment;        // True if any message has an attachment.
    private boolean    mHasUnreadMessages;    // True if any message has read.
    private boolean    mHasError;
    private int        mType;
    private int        mStatus;
    private int        mBoxType;
    private ContactList  mRecipientString;
    private static     Context sContext;
    private int        mSubId;
    private boolean    mLocked;
    private boolean    mIsMute;

    private static Map<Long, MuteCache> mMuteCache;
    private static final String SILENT_SELECTION = "(ThreadSettings.NOTIFICATION_ENABLE = 0) OR " +
            "(ThreadSettings.MUTE > 0 and ThreadSettings.MUTE_START > 0)";

    private FolderView(Context context) {
        sContext = context;
        mRecipientString = new ContactList();
    }

    /**
     * Start a query for all conversations in the database on the specified
     * AsyncQueryHandler.
     *
     * @param handler An AsyncQueryHandler that will receive onQueryComplete
     *                upon completion of the query
     * @param token   The token that will be passed to onQueryComplete
     */
    public static void startQueryForDraftboxView(AsyncQueryHandler handler, int token) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        Log.d(TAG, "startQueryForDraftboxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, DRAFTFOLDER_URI,
                        null, null, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, 10);
    }

/// M: @{
    public static void startQueryForDraftboxView(AsyncQueryHandler handler, int token , int mPostTime) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final int postTime = mPostTime;
        final AsyncQueryHandler queryHandler = handler;
        Log.d(TAG, "startQueryForDraftboxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, DRAFTFOLDER_URI,
                        null, null, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, mPostTime);
    }
/// @}

    public static void startQueryForInboxView(AsyncQueryHandler handler, int token, String where) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        final String mWhere = where;
        Log.d(TAG, "startQueryForInboxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, INBOXFOLDER_URI,
                        null, mWhere, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, 10);
    }

    public static void startQueryForInboxView(AsyncQueryHandler handler, int token, String where, int mPostTime) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        final int postTime = mPostTime;
        final String mWhere = where;
        Log.d(TAG, "startQueryForInboxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, INBOXFOLDER_URI,
                        null, mWhere, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, postTime);
    }

    private static void markFailedSmsSeen(Context context) {
        ContentValues values = new ContentValues(1);
        values.put("seen", 1);
        String where = Sms.TYPE + " = " + Sms.MESSAGE_TYPE_OUTBOX + " or " +
                       Sms.TYPE + " = " + Sms.MESSAGE_TYPE_QUEUED + " or " +
                       Sms.TYPE + " = " + Sms.MESSAGE_TYPE_FAILED;
        SqliteWrapper.update(context, context.getContentResolver(), Sms.CONTENT_URI, values, where, null);
    }

    private static void markOutboxMmsSeen(Context context) {
        ContentValues values = new ContentValues(1);
        values.put("seen", 1);
        String where = Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_OUTBOX;
        SqliteWrapper.update(context, context.getContentResolver(), Mms.CONTENT_URI, values, where, null);
    }

    public static void markFailedSmsMmsSeen(final Context context) {
        Log.d(TAG, "markFailedSmsMmsRead");
        new Thread(new Runnable() {
            public void run() {
                markFailedSmsSeen(context);
                markOutboxMmsSeen(context);
            }
        }).start();
    }

    public static void startQueryForOutBoxView(AsyncQueryHandler handler, int token, String where) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        final String mWhere = where;
        Log.d(TAG, "startQueryForOutBoxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, OUTBOXFOLDER_URI,
                        null, mWhere, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, 10);
    }

/// M: @{
    public static void startQueryForOutBoxView(AsyncQueryHandler handler, int token , String where, int mPostTime) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        final String mWhere = where;
        final int postTime = mPostTime;
        Log.d(TAG, "startQueryForOutBoxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, OUTBOXFOLDER_URI,
                        null, mWhere, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, postTime);
    }
/// @}

    public static void startQueryForSentboxView(AsyncQueryHandler handler, int token, String where) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        final String mWhere = where;
        Log.d(TAG, "startQueryForSentboxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, SENDBOXFOLDER_URI,
                        null, mWhere, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, 10);
    }

/// M: @{
    public static void startQueryForSentboxView(AsyncQueryHandler handler, int token, String where, int mPostTime) {
        handler.cancelOperation(token);
        final int queryToken = token;
        final AsyncQueryHandler queryHandler = handler;
        final String mWhere = where;
        final int postTime = mPostTime;
        Log.d(TAG, "startQueryForSentboxView");
        queryHandler.postDelayed(new Runnable() {
            public void run() {
                queryHandler.startQuery(
                        queryToken, null, SENDBOXFOLDER_URI,
                        null, mWhere, null, Conversations.DEFAULT_SORT_ORDER);
            }
        }, mPostTime);
    }
/// @}

    public static void startDeleteBoxMessage(AsyncQueryHandler handler, int token, Uri deleteuri, String where) {
        handler.startDelete(token, null, deleteuri, where, null);
    }


    /**
     * Returns a temporary Conversation (not representing one on disk) wrapping
     * the contents of the provided cursor.  The cursor should be the one
     * returned to your AsyncQueryHandler passed in to {@link #startQueryForAll}.
     * The recipient list of this conversation can be empty if the results
     * were not in cache.
     */
    public static FolderView from(Context context, Cursor cursor) {
        // First look in the cache for the Conversation and return that one. That way, all the
        // people that are looking at the cached copy will get updated when fillFromCursor() is
        // called with this cursor.
        FolderView folderview = new FolderView(context);
        fillFromCursor(context, folderview, cursor, false);
        return folderview;
    }

    private static void fillFromCursor(Context context, FolderView fview,
            Cursor c, boolean allowQuery) {
        synchronized (fview) {
            fview.mId      = c.getInt(ID);
            fview.mThreadId = c.getLong(THREAD_ID);
            fview.mDate    = c.getLong(DATE);
            fview.mHasUnreadMessages    = (c.getInt(READ) == 0);
            fview.mSubject = c.getString(SUBJECT);
            fview.mType    = c.getInt(TYPE);
            fview.mBoxType    = c.getInt(BOXTYPE);
            fview.mStatus     = c.getInt(STATUS);
            fview.mHasError  = (fview.mBoxType == 5 ||  fview.mStatus == 10);
            fview.mHasAttachment = (c.getInt(ATTACHMENT) == 1);
            fview.mSubId    = c.getInt(SUB_ID);
            fview.mLocked   = c.getInt(LOCKED) > 0;
            if (fview.mBoxType == 1) {
                if (MuteCache.getMuteEntry(fview.mThreadId) == null) {
                    fview.mIsMute = !MessageUtils.checkAppSettingsNeedNotify(context);
                    Log.d(TAG, "fview.mIsMute =  " + fview.mIsMute);
                } else {
                    fview.mIsMute = !MessageUtils.checkNeedNotifyForFolderMode(context, fview.mThreadId,
                        MuteCache.getInstance().getMute(fview.mThreadId),
                        MuteCache.getInstance().getMuteStart(fview.mThreadId),
                        MuteCache.getInstance().getMuteEnable(fview.mThreadId));
                }
            } else {
                fview.mIsMute = false;
            }
        }
        //fview.mRecipientString   = c.getString(ADDRESS);
        String recipientIds = c.getString(ADDRESS);
        ContactList recipients;
        //mms or sms draft
        if (fview.mType == 2 || (fview.mType == 1 && fview.mBoxType == 3) || fview.mType == 4) {
            recipients = ContactList.getByIds(recipientIds, allowQuery);
        } else {
            recipients = ContactList.getByNumbers(recipientIds, false, true);
            Log.d(TAG, "recipients " + recipients.toString());
        }
        if (fview.mType == 2) { //mms
             if (!TextUtils.isEmpty(fview.mSubject)) {
                 EncodedStringValue v = new EncodedStringValue(c.getInt(SUB_CS),
                         PduPersister.getBytes(fview.mSubject));
                fview.mSubject = v.getString();
            } else {
                fview.mSubject = context.getString(R.string.no_subject_view);
            }
        }
        synchronized (fview) {
            fview.mRecipientString = recipients;
        }
        Log.d(TAG, "mRecipientString" + fview.mRecipientString);
    }

    public synchronized int getmId() {
        return mId;
    }


    public synchronized String getmSubject() {
        return mSubject;
    }

    public synchronized long getmDate() {
        return mDate;
    }


    public synchronized boolean getmHasAttachment() {
        return mHasAttachment;
    }


    public synchronized boolean getmRead() {
        return mHasUnreadMessages;
    }


    public synchronized int getmType() {
        return mType;
    }

    public synchronized ContactList getmRecipientString() {
        return mRecipientString;
    }

    public synchronized int getmStatus() {
        return mStatus;
    }

    public synchronized boolean hasError() {
        return mHasError;
    }

    public synchronized int getmSubId() {
        return mSubId;
    }

    public synchronized boolean isLocked() {
        return mLocked;
    }

    public synchronized boolean isMute() {
        return mIsMute;
    }

}
