/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */

package com.mediatek.rcs.message.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.provider.Telephony.Sms;

import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

import com.mediatek.mms.ipmessage.DefaultIpConversationListItemExt;
import com.mediatek.mms.ipmessage.IIpContactExt;
import com.mediatek.mms.ipmessage.IIpConversationExt;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.GroupThumbnail;
import com.mediatek.rcs.message.group.PortraitManager.onGroupPortraitChangedListener;
import com.mediatek.rcs.message.plugin.EmojiImpl;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.EmojiShop;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.common.RcsLog;

/**
 * Plugin implements. response ConversationListItem.java in MMS host.
 *
 */
public class RcsConversationListItem extends DefaultIpConversationListItemExt
                                     implements onGroupPortraitChangedListener {
    private static String TAG = "RcsConversationListItem";
    /// M: New feature for rcse, adding IntegrationMode.
    private ImageView mFullIntegrationModeView;
    private Context mContext;
    private QuickContactBadge mAvatarView;
    private long mThreadId;
    private Context mPluginContext;
    private boolean mVisible; //true between onbind and ondetached
    private Handler mHandler;
    private GroupThumbnail mThumbnail;
    private RcsConversation mRcsConversation;

    /**
     * Construction.
     * @param pluginContext Context
     */
    public RcsConversationListItem(Context pluginContext) {
        mPluginContext = pluginContext;
    }

    @Override
    public void onIpSyncView(Context context, ImageView fullIntegrationModeView,
                             QuickContactBadge avatarView) {
        Log.d(TAG, "onIpSyncView");
        mContext = context;
        mFullIntegrationModeView = fullIntegrationModeView;
        mFullIntegrationModeView.setVisibility(View.VISIBLE);
        mAvatarView = avatarView;
    }

    @Override
    public String onIpFormatMessage(IIpContactExt ipContact, long threadId,
                                    String number, String name) {
        String chatId = null;
        if (mRcsConversation != null) {
            chatId = mRcsConversation.mGroupChatId;
        } else {
            chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, threadId);
        }
        Log.d("avatar", "ConvListItem.formatMessage(): threadId = " + threadId
                                            + ", chatId =" +chatId);
        if (!TextUtils.isEmpty(chatId)) {
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(chatId);
            if (info != null) {
                name = info.getNickName();
                Log.d(TAG, "group's nickName: " + name);
                if (TextUtils.isEmpty(name)) {
                    name = info.getSubject();
                }
                Log.d(TAG, "onIpFormatMessage: number = " + number + ", group name = " + name);
            }
        }
        return name;
    }

    @Override
    public boolean updateIpAvatarView(final IIpContactExt ipContact, String number,
                                      QuickContactBadge avatarView, final Uri uri) {
        String chatId = null;
        if (mRcsConversation != null) {
            chatId = mRcsConversation.mGroupChatId;
        } else {
            chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, mThreadId);
        }
        Log.d(TAG, "updateIpAvatarView: chatId = " + chatId);

        if (chatId != null) {
            updateGroupAvataView(chatId);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateIpAvatarView(IIpConversationExt ipConv, QuickContactBadge avatarView,
                    ImageView selectIcon) {
        String chatId = null;
        if (mRcsConversation != null) {
            chatId = mRcsConversation.mGroupChatId;
        } else {
            chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, mThreadId);
        }
        Log.d(TAG, "updateIpAvatarView: chatId = " + chatId);
        if (chatId != null) {
            updateGroupAvataView(chatId);
            mAvatarView.setVisibility(View.VISIBLE);;
            selectIcon.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    @Override
    public boolean onIpBind(IIpConversationExt ipConv, boolean isActionMode, boolean isChecked,
                            int convType, RelativeLayout conversationItem, TextView fromView,
                            TextView subjectView, TextView dateView) {
        mRcsConversation = (RcsConversation) ipConv;
        Log.d(TAG, "onIpBind: ipConv = " + ipConv);
        Log.d(TAG, "onIpBind: isActionMode = " + isActionMode + ", isChecked = "
                    + isChecked);
        mVisible = true;
        mHandler = new Handler();
        if (ipConv instanceof RcsConversation) {
            RcsConversation conv = (RcsConversation) ipConv;
            mThreadId = conv.getThreadId();

            // show sticky conversations as a different background color
            boolean isSticky = conv.isSticky();
            if (isActionMode) {
                if (!isChecked) {
                    if (isSticky) {
                        conversationItem.setBackgroundColor(Color.parseColor("#F5FFF1"));
                    } else {
                        conversationItem.setBackgroundColor(0);
                    }
                }
            } else {
                if (isSticky) {
                    conversationItem.setBackgroundColor(Color.parseColor("#F5FFF1"));
                } else {
                    conversationItem.setBackgroundColor(0);
                }
            }

            String subject = subjectView.getText().toString();
            String body = null;
            int msgClass = conv.getLastMsgType();
            if (msgClass == RcsLog.Class.BURN) {
                body = mPluginContext.getString(R.string.menu_burned_msg);
            } else if (msgClass == RcsLog.Class.NORMAL &&
                    conv.getLastMsgCatogery() == RcsLog.MessageType.FT) {
                body = formatFTShowBody(conv.getFtMimeType());
            } else if (msgClass == RcsLog.Class.EMOTICON) {
                if (EmojiShop.matchEmXml(subject)) {
                    body = EmojiShop.parseEmSmsString(subject);
                }
            }

            if (!TextUtils.isEmpty(body)) {
                subjectView.setText(body);
                return false;
            }

            // format conversation list item subject if has emoji image
            EmojiImpl emoji = EmojiImpl.getInstance(mPluginContext);
            CharSequence cs = emoji.getEmojiExpression(subject, false);
            subjectView.setText(cs);
        }
        return false;
    }

    @Override
    public void onIpUnbind() {
        mVisible = false;
        if (mThumbnail != null) {
            mThumbnail.removeChangedListener(this);
            mThumbnail = null;
        }
    }

    private String formatFTShowBody(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            Log.e(TAG, "[getMsgContent]: mime type is empty");
            return null;
        }
        if (mimeType.contains("image")) {
            return mPluginContext.getString(R.string.ft_snippet_image);
        } else if (mimeType.contains("audio")
                || mimeType.contains("application/ogg")) {
            return mPluginContext.getString(R.string.ft_snippet_voice);
        } else if (mimeType.contains("video")) {
            return mPluginContext.getString(R.string.ft_snippet_video);
        } else if (mimeType.contains("text/x-vcard")) {
            return mPluginContext.getString(R.string.ft_snippet_vcard);
        } else if (mimeType.contains("application/vnd.gsma.rcspushlocation+xml")) {
            return mPluginContext.getString(R.string.ft_snippet_geolocation);
        } else {
            return null;
        }
    }

    private void updateGroupAvataView(String chatId) {
        if (mThumbnail != null) {
            mThumbnail.removeChangedListener(this);
            mThumbnail = null;
        }
        try {
            mThumbnail = PortraitManager.getInstance().getGroupPortrait(chatId);
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG, "updateGroupAvataView: e: " +e);

        }
        if (mThumbnail != null) {
            mThumbnail.addChangedListener(this);
            Drawable roundDrawable = getRoundedBitmapDrawable(mThumbnail.mBitmap);
            mAvatarView.setImageDrawable(roundDrawable);
        } else {
            Drawable drawable = RcsMessageUtils.getGroupDrawable(mThreadId);
            mAvatarView.setImageDrawable(drawable);
        }
        mAvatarView.setVisibility(View.VISIBLE);
        mAvatarView.setOnClickListener(mAvataOnClickListener);
        mAvatarView.assignContactUri(null);
    }

    private OnClickListener mAvataOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // TODO  process avatar click event

        }
    };

    @Override
    public void onChanged(final Bitmap newBitmap) {
        if (mHandler != null && mVisible) {
            final RoundedBitmapDrawable roundedDrawable = getRoundedBitmapDrawable(newBitmap);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (mVisible) {
                        mAvatarView.setImageDrawable(roundedDrawable);
                    }
                }
            });
        }
    }

    private RoundedBitmapDrawable getRoundedBitmapDrawable(final Bitmap originBitMap) {
        final RoundedBitmapDrawable drawable =
                RoundedBitmapDrawableFactory.create(mContext.getResources(), originBitMap);
        drawable.setAntiAlias(true);
        drawable.setCornerRadius(originBitMap.getHeight() / 2);
        return drawable;
    }
}
