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
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.common.PluginImpl;
import com.mediatek.mms.ext.DefaultOpConversationListItemExt;
import com.mediatek.mms.ext.IOpConversationExt;
import com.mediatek.op09.plugin.R;

/**
 * M: Op09MmsConversationListItemExt.
 */
public class Op09ConversationListItemExt extends DefaultOpConversationListItemExt {

    private static final int MAX_UNREAD_MESSAGES_COUNT = 999;
    private static final String MAX_UNREAD_MESSAGES_STRING = "999+";
    private Context mContext;

    public Op09ConversationListItemExt(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean onFinishInflate(TextView textView) {
        if (textView == null) {
            return false;
        }
        ViewGroup.LayoutParams lp = textView.getLayoutParams();
        lp.width = this.getResources().getDimensionPixelOffset(
                R.dimen.ct_conversation_list_item_subject_max_len);
        textView.setLayoutParams(lp);
        return true;
    }

    @Override
    public boolean  bind(Context context, TextView dateView, TextView unreadView, ImageView simType,
            IOpConversationExt opConversation, boolean showDraftIcon, LinearLayout la) {
        OP09ConversationExt op09Conversation = (OP09ConversationExt) opConversation;
        int unreadCount = op09Conversation.mUnreadCount;
        if (unreadCount > 0) {
            int opCount = new Op09MmsConversationListItemExt(mContext)
                    .getMessageCountAndShowSimType(mContext, op09Conversation.mUri, simType,
                            op09Conversation.mRecipSize, op09Conversation.mHasDraft);
            if (opCount > 0) {
                unreadCount = opCount;
            }
            String unreadString = null;
            if (unreadCount > MAX_UNREAD_MESSAGES_COUNT) {
                unreadString = MAX_UNREAD_MESSAGES_STRING;
            } else {
                unreadString = "" + unreadCount;
            }
            unreadView.setVisibility(View.VISIBLE);
            unreadView.setText(unreadString);
        } else {
            unreadView.setVisibility(View.GONE);
        }

        Op09MmsUtils utilsExt = Op09MmsUtils.getInstance();
        String dateStr = utilsExt.formatDateAndTimeStampString(this, op09Conversation.mDate,
                op09Conversation.mDateSent, false,
                utilsExt.formatTimeStampStringExtend(this, op09Conversation.mDate));
        dateView.setVisibility(View.VISIBLE);
        dateView.setText(dateStr);
        return true;
    }

    @Override
    public int formatMessage(ImageView view, IOpConversationExt opConversation, int defaultCount) {
        OP09ConversationExt op09Conversation = (OP09ConversationExt) opConversation;
        int count = defaultCount;
        int opCount = new Op09MmsConversationListItemExt(mContext).getMessageCountAndShowSimType(
                mContext, op09Conversation.mUri, view, op09Conversation.mRecipSize,
                op09Conversation.mHasDraft);
        if (opCount > 0) {
            count = opCount;
        }
        return count;
    }

}