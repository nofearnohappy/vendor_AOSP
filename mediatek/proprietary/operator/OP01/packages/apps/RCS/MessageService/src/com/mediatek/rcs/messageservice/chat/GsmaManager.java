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

package com.mediatek.rcs.messageservice.chat;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceConfiguration;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.JoynServiceRegistrationListener;
import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.CapabilitiesListener;
import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.chat.ChatService;
import org.gsma.joyn.chat.ChatServiceConfiguration;
import org.gsma.joyn.chat.ConferenceEventData;
import org.gsma.joyn.chat.GroupChatSyncingListener;
import org.gsma.joyn.ft.FileTransferService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.mediatek.rcs.common.binder.RCSServiceManager;
// for global var.
import com.mediatek.rcs.common.utils.Logger;

/**
 * This class manages the Gsma APIs, providing a convenient way for gsma and stack invocations.
 */
public class GsmaManager {
    public static final String TAG = "GsmaManager";
    public static final String BROADCAST_RCS_REGISTRATION_STATUS =
            "com.mediatek.rcs.message.REGISTRATION_STATUS_CHANGE";
    public static final String REGISTRATION_STATUS = "status";

    public final static String BROADCAST_RCS_CONFIGURATION_STATUS =
            "com.orangelabs.rcs.CONFIGURATION_STATUS";
    public final static String CONFIGURATION_STATUS = "status";

    public final static String BROADCAST_RCS_CORESERVICE_DOWN =
            "com.mediatek.rcs.message.CORESERVICE_DOWN";

    public final static String BROADCAST_RCS_CAPABILITY = "com.mediatek.rcs.message.CAPABILITY";
    public final static String CAPABILITY = "capability";
    public final static String CONTACT = "contact";

    private static GsmaManager sInstance = null;
    private static RCSChatServiceBinder sService = null;
    private static Context sContext = null;
    private static RCSJoynServiceStateReceiver sReceiver = null;

    private boolean mRegistrationStatus = false;

    private CapabilityService mCapabilitiesApi = null;
    private ChatService mChatApi = null;
    private FileTransferService mFileTransferApi = null;
    private ChatServiceConfiguration mChatServiceConfigApi = null;

    JoynServiceRegistrationListener mRegistrationListener = null;
    CapabilitiesListener mCapabilitiesListener = null;

    /**
     * This method should only be called from , for gsma APIs initialization.
     *
     * @param context
     *            The Context of this application.
     * @return true If initialize successfully, otherwise false.
     */
    public static synchronized boolean initialize(RCSChatServiceBinder service) {
        Logger.d(TAG, "initialize() entry service: " + service);
        if (null != sInstance) {
            Logger.w(TAG, "initialize() sInstance has existed");
            return true;
        }

        if (null == service.getContext()) {
            Logger.w(TAG, "initialize() context is null");
            return false;
        }

        sService = service;
        sContext = service.getContext();
        sInstance = new GsmaManager();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(JoynService.ACTION_RCS_SERVICE_UP);

        sReceiver = new RCSJoynServiceStateReceiver();
        sContext.registerReceiver(sReceiver, intentFilter);

        if (JoynService.isServiceStarted(sContext)) {
            Logger.d(TAG, "RcsCoreService has been already started yet");
            sInstance.bindCoreService();
            return true;
        }
        return false;
    }

    public static synchronized boolean unInitialize() {
        Logger.d(TAG, "unInitialize()");
        sService = null;

        sContext.unregisterReceiver(sReceiver);
        sContext = null;

        sInstance.unBindCoreService();
        sInstance = null;
        return true;
    }

    /**
     * Get the context
     *
     * @return Context
     */
    public static Context getContext() {
        //Logger.d(TAG, "getContext() : Context = " + sContext);
        return sContext;
    }

    /**
     * Get the instance of GsmaManager
     *
     * @return The instance of GsmaManager, or null if the instance has not been initialized.
     */
    public static GsmaManager getInstance() {
        //Logger.d(TAG, "getInstance() : sInstance = " + sInstance);
        return sInstance;
    }

    public static boolean isServiceAvailable() {
        return sInstance == null ? false : sInstance.getRegistrationStatus();
    }

    /**
     * Get the connected CapabilityApi
     *
     * @return The instance of CapabilityApi, or null if the instance has not connected.
     * @throws JoynServiceException
     */
    public CapabilityService getCapabilityApi() throws JoynServiceException {
        //Logger.d(TAG, "getCapabilityApi() : mCapabilitiesApi = " + mCapabilitiesApi);
        if (mCapabilitiesApi != null)
            return mCapabilitiesApi;
        else
            throw new JoynServiceException("CapabilityService is not connected");
    }

    /**
     * Get the connected ChatService
     *
     * @return The instance of ChatService, or null if the instance has not connected.
     * @throws JoynServiceException
     */
    public ChatService getChatApi() throws JoynServiceException {
        //Logger.d(TAG, "getChatService() : mChatApi = " + mChatApi);
        if (mChatApi != null)
            return mChatApi;
        else
            throw new JoynServiceException("ChatService is not connected");
    }

    /**
     * Get the connected ChatService
     *
     * @return The instance of ChatService, or null if the instance has not connected.
     * @throws JoynServiceException
     */
    public FileTransferService getFileTransferApi() throws JoynServiceException {
        //Logger.d(TAG, "getFileTransferApi() : mFileTransferApi = " + mFileTransferApi);
        if (mFileTransferApi != null)
            return mFileTransferApi;
        else
            throw new JoynServiceException("FileTransferService is not connected");
    }

    public boolean getConfigurationStatus() {
        return new JoynServiceConfiguration().getConfigurationState(sContext);
    }

    public String getMSISDN() {
        String publicUri = new JoynServiceConfiguration().getPublicUri(sContext);
        //Logger.d(TAG, "getMSISDN() : publicUri: " + publicUri);
        // sip:+86xxxxxx@xxxx or tel:+86xxxxxxx
        if (TextUtils.isEmpty(publicUri)) {
            return null;
        }
        String msisdn;
        if (publicUri.indexOf('@') != -1) {
            msisdn = publicUri.substring(publicUri.indexOf(':') + 1, publicUri.indexOf('@'));
        } else {
            msisdn = publicUri.substring(publicUri.indexOf(':') + 1);
        }
        //Logger.d(TAG, "getMSISDN() : msisdn: " + msisdn);
        return msisdn;
    }

    public String getPublicUri() {
        return new JoynServiceConfiguration().getPublicUri(sContext);
    }

    public boolean getRCSStatus() {
//        return new JoynServiceConfiguration().getServiceState(sContext);
//        return new JoynServiceConfiguration().getServiceState(sContext);
        return JoynServiceConfiguration.isServiceActivated(sContext);
    }

    public boolean getRegistrationStatus() {
        Logger.d(TAG, "getRegistrationState() : mRegistrationStatus = " + mRegistrationStatus);
        //if (!getRCSStatus())
        //   return false;
        //if (!getConfigurationStatus())
        //    return false;
        return mRegistrationStatus;
    }

    public void sendGroupConferenceSubscription(String chatId) {
        Logger.d(TAG, "sendGroupConferenceSubscription() :" + chatId);
        try {
            getChatApi().syncGroupChat(chatId, new RCSGroupChatSyncingListener());
        } catch (JoynServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void syncAllGroupChats() {
        try {
            getChatApi().syncAllGroupChats(new RCSGroupChatSyncingListener());
        } catch (JoynServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void handleRegistrationStateChanged(boolean status) {
        mRegistrationStatus = status;
        Intent intent = new Intent(BROADCAST_RCS_REGISTRATION_STATUS);
        intent.putExtra(REGISTRATION_STATUS, mRegistrationStatus);
        sContext.sendBroadcast(intent);

        sService.handleRegistrationStatusChanged(status);
    }

    void handleCoreServiceDown() {
        mRegistrationStatus = false;
        Intent intent = new Intent(BROADCAST_RCS_CORESERVICE_DOWN);
        sContext.sendBroadcast(intent);

        sService.handleCoreServiceDown();
    }

    void handleGroupConferenceNotify(String chatId, ConferenceEventData data) {
        sService.handleGroupConferenceNotify(chatId, data);
    }

    private GsmaManager() {
        Logger.d(TAG, "Constructor()");
        mRegistrationStatus = false;

        mCapabilitiesApi = new CapabilityService(sContext, new MyCapabilitiesServiceListener());
        mChatApi = new ChatService(sContext, new MyChatServiceListener());
        mFileTransferApi = new FileTransferService(sContext, new MyFileTransferServiceListener());

        mRegistrationListener = new RegistrationListener();
        mCapabilitiesListener = new CapabilityServiceListener();
    }

    private void bindCoreService() {
        Logger.d(TAG, "bindCoreService()");

        Logger.d(TAG, "Bind to CapabilityService");
        mCapabilitiesApi.connect();

        Logger.d(TAG, "Bind to ChatService");
        mChatApi.connect();

        Logger.d(TAG, "Bind to FileTransferService");
        mFileTransferApi.connect();
    }

    private void unBindCoreService() {
        Logger.d(TAG, "unBindCoreService()");
        try {
            mChatApi.removeServiceRegistrationListener(mRegistrationListener);
            mCapabilitiesApi.removeCapabilitiesListener(mCapabilitiesListener);
        } catch (JoynServiceException e) {
            e.printStackTrace();
        }

        mCapabilitiesApi.disconnect();
        mChatApi.disconnect();
        mFileTransferApi.disconnect();
    }

    private class MyChatServiceListener implements JoynServiceListener {
        @Override
        public void onServiceConnected() {
            Logger.d(TAG, "ChatService onServiceConnected entry#" + this);
            try {
                mRegistrationStatus = mChatApi.isServiceRegistered();
                handleRegistrationStateChanged(mRegistrationStatus);
                Logger.d(TAG, "mRegistrationStatus init state###!!!: " + mRegistrationStatus);
                mChatApi.addServiceRegistrationListener(mRegistrationListener);
                mChatServiceConfigApi = mChatApi.getConfiguration();
                // TODO: time to remove...Shuo check.
            } catch (JoynServiceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int error) {
            Logger.d(TAG, "MyChatServiceListener onServiceDisconnected entry");
            handleCoreServiceDown(); // RCS Core service down.
        }
    }

    private class MyFileTransferServiceListener implements JoynServiceListener {
        @Override
        public void onServiceConnected() {
            Logger.d(TAG, "FileTransferService onServiceConnected entry");
        }

        @Override
        public void onServiceDisconnected(int error) {
            Logger.d(TAG, "MyFileTransferServiceListener onServiceDisconnected entry");
            mRegistrationStatus = false; // RCS Core service down.
        }
    }

    private class MyCapabilitiesServiceListener implements JoynServiceListener {
        @Override
        public void onServiceConnected() {
            try {
                mCapabilitiesApi.addCapabilitiesListener(mCapabilitiesListener);
            } catch (JoynServiceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Logger.d(TAG, "CapabilityService onServiceConnected entry");
        }

        @Override
        public void onServiceDisconnected(int error) {
            Logger.d(TAG, "MyCapabilitiesServiceListener onServiceDisconnected entry");
            mRegistrationStatus = false; // RCS Core service down.
        }
    }

    private class RegistrationListener extends JoynServiceRegistrationListener {
        @Override
        public void onServiceRegistered() {
            Logger.d(TAG, "RCS Registered###!!!!!!");
            handleRegistrationStateChanged(true);
        }

        @Override
        public void onServiceUnregistered() {
            Logger.d(TAG, "RCS Unregistered###!!!!!!");
            handleRegistrationStateChanged(false);
        }
    }

    private class RCSGroupChatSyncingListener extends GroupChatSyncingListener{
        /**
         */
        @Override
        public void onSyncStart(int goupCount){
            //do nothing
        }

        /**
         */
        @Override
        public void onSyncInfo(String chatId, ConferenceEventData info){
            Logger.d(TAG, "onSyncInfo: chatId=" + chatId);
            handleGroupConferenceNotify(chatId, info);
        }

        /**
         */
        @Override
        public void onSyncDone(int result){
            Intent intent = new Intent(RCSServiceManager.RCS_SYNC_GROUP_CHATS_DONE);
            if (result >= 0) {
                intent.putExtra("result", true);
            } else {
                intent.putExtra("result", false);
            }
            sContext.sendBroadcast(intent);
        }
    }

    private class CapabilityServiceListener extends CapabilitiesListener {
        @Override
        public void onCapabilitiesReceived(String contact, Capabilities capa) {
            Intent intent = new Intent(BROADCAST_RCS_CAPABILITY);
            intent.putExtra(CONTACT, contact);
            intent.putExtra(CAPABILITY, capa);
            sContext.sendBroadcast(intent);

            sService.handleCapabilityChanged(contact, capa);
        }
    }

    private static class RCSJoynServiceStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(TAG, "RCSServiceStateReceiver onReceive() context: " + context);
            String action = intent.getAction();
            if (action.equalsIgnoreCase(JoynService.ACTION_RCS_SERVICE_UP)) {
                Logger.d(TAG, "RCSServiceStateReceiver onReceive() ACTION_RCS_SERVICE_UP");
                if (GsmaManager.getInstance() != null) {
                    GsmaManager.getInstance().bindCoreService();
                }
            }
        }
    }

    public boolean isAutoAcceptGroupInvitation() {
        boolean ret = false;
        if (mChatServiceConfigApi == null) {
            try {
                mChatServiceConfigApi = mChatApi.getConfiguration();
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
        if (mChatServiceConfigApi != null) {
            ret = mChatServiceConfigApi.isGroupChatAutoAcceptMode();
        }
        return ret;
    }

    /**
     * get ChatServiceConfiguration
     * @return  ChatServiceConfiguration
     */
    public ChatServiceConfiguration getChatServiceConfiguration() {
        if (mChatServiceConfigApi == null) {
            try {
                mChatServiceConfigApi = mChatApi.getConfiguration();
            } catch (JoynServiceException e) {
                e.printStackTrace();
            }
        }
        return mChatServiceConfigApi;
    }
}
