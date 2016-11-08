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

package com.mediatek.mms.folder.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.FolderView;
import com.android.mms.ui.MessageUtils;


/** M:
 * This class manages the view for given conversation.
 */
public class FolderViewListItem extends RelativeLayout implements Contact.UpdateListener {
    private static final String TAG = "FolderViewListItem";
    private static final boolean DEBUG = false;

    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private View mAttachmentView;
    private View mErrorIndicator;
    private ImageView mAvatarView;
    private ImageView mPresenceView;
    private TextView mByCard;
    private Context mContext;
    private ImageView mLockedInd;
    private View mMuteView;

    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();

    private FolderView mFview;

    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    /// M:
    private boolean mSubjectSingleLine;

    public FolderViewListItem(Context context) {
        super(context);
        mContext = context;
    }

    public FolderViewListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = (TextView) findViewById(R.id.from);
        mSubjectView = (TextView) findViewById(R.id.subject);
        mPresenceView = (ImageView) findViewById(R.id.presence);
        mDateView = (TextView) findViewById(R.id.date);
        mAttachmentView = findViewById(R.id.attachment);
        mErrorIndicator = findViewById(R.id.error);
        mAvatarView = (ImageView) findViewById(R.id.avatar);
        mByCard = (TextView) findViewById(R.id.by_card);
        mLockedInd = (ImageView) findViewById(R.id.locked_indicator);
        mMuteView = findViewById(R.id.mute);
    }

//    public Conversation getConversation() {
//        return mConversation;
//    }

//    /**
//     * Only used for header binding.
//     */
//    public void bind(String title, String explain) {
//        mFromView.setText(title);
//        mSubjectView.setText(explain);
//    }

    private CharSequence formatMessage() {
        //ContactList recipients = mFview.getmRecipientString();
        //String from = "";
        //if (recipients != null && !recipients.isEmpty()) {
        //   for (Contact contact : recipients) {
        //        contact.reload(true);
        //    }
        //    from = recipients.formatNames(", ");
        //} else {
        //    from = mContext.getString(android.R.string.unknownName);
        //}
        String from = mFview.getmRecipientString().formatNames(", ");
        if (TextUtils.isEmpty(from)) {
            from = mContext.getString(android.R.string.unknownName);
        }
        SpannableStringBuilder buf = new SpannableStringBuilder(from);
        // Unread messages are shown in bold
        if (mFview.getmRead()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return buf;
    }

    public final void bind(Context context, final FolderView fview, Boolean ischecked) {
        //if (DEBUG) Log.v(TAG, "bind()");

        mFview = fview;

        int backgroundId;
        if (ischecked) {
            backgroundId = R.drawable.list_selected_holo_light;
        } else if (mFview.getmRead()) {
            backgroundId = R.drawable.conversation_item_background_unread;
            mPresenceView.setVisibility(View.VISIBLE);
        } else {
            backgroundId = R.drawable.conversation_item_background_read;
            mPresenceView.setVisibility(View.INVISIBLE);
        }
        Drawable background = mContext.getResources().getDrawable(backgroundId);

        setBackgroundDrawable(background);

        if (mFview.getmType() == 1) {
            mAvatarView.setImageResource(R.drawable.ic_sms);
        } else if (mFview.getmType() == 2) {
            mAvatarView.setImageResource(R.drawable.ic_mms);
        } else if (mFview.getmType() == 3) {
            mAvatarView.setImageResource(R.drawable.ic_wappush);
        } else if (mFview.getmType() == 4) {
            mAvatarView.setImageResource(R.drawable.ic_cellbroadcast);
        }

        boolean hasError = mFview.hasError();

        boolean hasAttachment = mFview.getmHasAttachment();
        mAttachmentView.setVisibility(hasAttachment ? VISIBLE : GONE);

        if (DEBUG) {
            Log.v(TAG, "bind: contacts.addListeners " + this);
        }
        Contact.addListener(this);
        Log.d(TAG, "bind mgViewID = " + FolderViewList.mgViewID);
        if (FolderViewList.mgViewID == FolderViewList.OPTION_OUTBOX && !hasError) {
            mDateView.setText(R.string.sending_message);
        } else {
            // Date
            mDateView.setText(MessageUtils.formatTimeStampStringExtend(context, mFview.getmDate()));
        }
        // From.
        mFromView.setText(formatMessage());

        if (mSubjectSingleLine) {
            mSubjectView.setSingleLine(true);
        }
        mSubjectView.setText(mFview.getmSubject());

        // Transmission error indicator.
        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);
        if (FolderViewList.mgViewID == FolderViewList.OPTION_DRAFTBOX) {
            mByCard.setVisibility(View.GONE);
        } else {
            mByCard.setVisibility(View.VISIBLE);
            setSubIconAndLabel(mFview.getmSubId());
        }

        boolean isLocked = mFview.isLocked();
        mLockedInd.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        mMuteView.setVisibility(View.GONE);
        if (mFview.isMute()) {
            mMuteView.setVisibility(View.VISIBLE);
        }

    }

    public final void unbind() {
        if (DEBUG) {
            Log.v(TAG, "unbind: contacts.removeListeners " + this);
        }
        // Unregister contact update callbacks.
        Contact.removeListener(this);
    }

    @Override
    public void onUpdate(Contact updated) {
        mHandler.post(new Runnable() {
            public void run() {
                updateFromView();
            }
        });
    }
    private void updateFromView() {
        mFview.getmRecipientString();
        mFromView.setText(formatMessage());
    }

    private long getKey(int type, long id) {
        if (type == 2) {  //mms
            return -id;
        } else if (type == 1) {
            return id;
        } else if (type == 3) {
            return 100000 + id;
        } else {  //cb
            return -(100000 + id);
        }
    }

    public void setSubjectSingleLineMode(boolean value) {
        mSubjectSingleLine = value;
    }


    @Override
    protected void onDetachedFromWindow() {
        Log.v(TAG, "onDetachedFromWindow");
        super.onDetachedFromWindow();
        Contact.removeListener(this);
    }

    private void setSubIconAndLabel(int subId) {
        Log.i(TAG, "setSubIconAndLabel subId=" +  subId);
        SubscriptionInfo subInfo = SubscriptionManager.from(MmsApp.getApplication())
                .getActiveSubscriptionInfo(subId);
        Log.i(TAG, "subInfo=" + subInfo);
        if (null != subInfo) {
            if (subInfo.getSimSlotIndex() == SubscriptionManager.SIM_NOT_INSERTED ||
                subInfo.getSimSlotIndex() == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                Log.i(TAG, "current not insert sim card");
                mByCard.setVisibility(View.GONE);
            } else {
                mByCard.setVisibility(View.VISIBLE);
                mByCard.setTextColor(subInfo.getIconTint());
                mByCard.setText(subInfo.getDisplayName().toString());
            }
        }
    }
}
