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

package com.mediatek.rcs.message.plugin;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.util.Log;
import android.net.Uri;
import android.text.SpannableString;

import com.mediatek.common.PluginImpl;

import com.mediatek.mms.ipmessage.DefaultIpMessagePluginImplExt;
import com.mediatek.mms.ipmessage.DefaultIpScrollListenerExt;
import com.mediatek.mms.ipmessage.IIpColumnsMapExt;
import com.mediatek.mms.ipmessage.IIpComposeActivityExt;
import com.mediatek.mms.ipmessage.IIpContactExt;
import com.mediatek.mms.ipmessage.IIpConversationExt;
import com.mediatek.mms.ipmessage.IIpConversationListExt;
import com.mediatek.mms.ipmessage.IIpConversationListItemExt;
import com.mediatek.mms.ipmessage.IIpDialogModeActivityExt;
import com.mediatek.mms.ipmessage.IIpEmptyReceiverExt;
import com.mediatek.mms.ipmessage.IIpEmptyServiceExt;
import com.mediatek.mms.ipmessage.IIpMessageItemExt;
import com.mediatek.mms.ipmessage.IIpMessageListAdapterExt;
import com.mediatek.mms.ipmessage.IIpMessageListItemExt;
import com.mediatek.mms.ipmessage.IIpMessagingNotificationExt;
import com.mediatek.mms.ipmessage.IIpMultiDeleteActivityExt;
import com.mediatek.mms.ipmessage.IIpSearchActivityExt;
import com.mediatek.mms.ipmessage.IIpSettingListActivityExt;
import com.mediatek.mms.ipmessage.IIpSpamMsgReceiverExt;
import com.mediatek.mms.ipmessage.IIpStatusBarSelectorExt;
import com.mediatek.mms.ipmessage.IIpUtilsExt;
import com.mediatek.mms.ipmessage.IMmsConfigExt;
import com.mediatek.mms.ipmessage.IIpSuggestionsProviderExt;

import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.message.proxy.RcsProxyReceivers;
import com.mediatek.rcs.message.proxy.RcsProxyServices;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.utils.RcsMessageConfig;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

/**
 * Used to access RCS plugin
 */
 @PluginImpl(interfaceName = "com.mediatek.mms.ipmessage.IIpMessagePluginExt")
public class RCSMessagePluginExt extends DefaultIpMessagePluginImplExt {

    private static final String TAG = "RCSMessagePluginExt";

    private Context mPluginContext;
    private static RCSMessagePluginExt sInstance;
    private RcsComposeActivity mComposeActivity;

    public RCSMessagePluginExt(Context context) {
        super(context);
        ContextCacher.setPluginContext(context);
        mPluginContext = context;
        sInstance = this;
    }

    public static RCSMessagePluginExt getInstance() {
        return sInstance;
    }

    @Override
    public IIpMessageListItemExt getIpMessageListItem() {
        Log.d(TAG, "getIpMessageListItem");
        return new RcsMessageListItem(mPluginContext);
    }

    @Override
    public IIpMessageItemExt getIpMessageItem() {
        Log.d(TAG, "getIpMessageItem");
        return new RcsMessageItem(mContext);
    }

    @Override
    public IIpComposeActivityExt getIpComposeActivity() {
        Log.d(TAG, "getIpComposeActivity");
        mComposeActivity = new RcsComposeActivity(mPluginContext);
        return mComposeActivity;
    }

    @Override
    public IIpDialogModeActivityExt getIpDialogModeActivity() {
        return new RcsDialogModeActivity(RcsUtilsPlugin.sHostContext, mPluginContext);
    }

    @Override
    public IIpMessageListAdapterExt getIpMessageListAdapter() {
        Log.d(TAG, "getIpMessageListAdapter");
        return new RcsMessageListAdapter(mPluginContext);
    }

    @Override
    public IIpMessagingNotificationExt getIpMessagingNotification() {
        return new RcsMessagingNotification(mPluginContext);
    }

    @Override
    public IIpMultiDeleteActivityExt getIpMultiDeleteActivity() {
        return new RcsMultiDeleteActivity();
    }

    @Override
    public IIpSearchActivityExt getIpSearchActivity() {
        return new RcsSearchActivity();
    }

    @Override
    public IIpStatusBarSelectorExt getIpStatusBarSelector() {
        return new RcsStatusBarSelector();
    }

    @Override
    public IIpContactExt getIpContact() {
        return new RcsContact();
    }

    @Override
    public IIpConversationExt getIpConversation() {
        return new RcsConversation();
    }

    @Override
    public IIpConversationListExt getIpConversationList() {
        return new RcsConversationList(mPluginContext);
    }

    @Override
    public IIpConversationListItemExt getIpConversationListItem() {
        return new RcsConversationListItem(mPluginContext);
    }

    @Override
    public IIpUtilsExt getIpUtils() {
        return new RcsUtilsPlugin(mPluginContext);
    }

    @Override
    public IIpSettingListActivityExt getIpSettingListActivity() {
        return new RcsSettingListActivity(mPluginContext);
    }

    @Override
    public IIpEmptyServiceExt getIPEmptyService() {
        Log.d(TAG, "getIPEmptyReceiver");
        return  new RcsProxyServices();
    }

    @Override
    public IIpSpamMsgReceiverExt getIpSpamMsgReceiver() {
        return new RcsSpamMsgReceiver();
    }

    @Override
    public IIpEmptyReceiverExt getIpEmptyReceiver() {
        Log.d(TAG, "getIPEmptyReceiver");
        return  new RcsProxyReceivers();
    }

    @Override
    public DefaultIpScrollListenerExt getIpScrollListener() {
        Log.d(TAG, "getIpScrollListener");
        return new RcsScrollListener();
    }

    @Override
    public IIpColumnsMapExt getIpColumnsMap() {
        Log.d(TAG, "getIpColumnsMap");
        return new RcsMessageListAdapter.RCSColumnsMap();
    }

    @Override
    public IIpSuggestionsProviderExt getIpSuggestionsProvider() {
        return new RcsSuggestionsProvider();
    }
}
