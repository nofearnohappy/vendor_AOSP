/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.smsreg;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.util.Log;

import com.mediatek.smsreg.ui.SendMessageAlertActivity;

public class SmsRegService extends Service {
    private static final String TAG = "SmsReg/Service";
    private final long DELAY_SERARCH_NET = 90000; // Wait 1.5min for search signal
    private final long DEALY_SHOW_DIALOG = 30000; // Wait 30s to show dialog

    public static final int SUB_NUMBER = PlatformManager.SUB_NUMBER;

    private PlatformManager mPlatformManager;
    private BlackListUnit mBlackListUnit;
    private XmlGenerator mXmlG;
    private String mOperatorId;
    private PendingIntent mPendingIntent;

    private BroadcastReceiver mSimStateReceiver;
    private BroadcastReceiver mSmsReceivedReceiver;
    private PhoneStateListener[] mPhoneStateListener;

    private long mSendSubId = -1;
    private Boolean mIsSendMsg = false;
    private String mSavedImsi = null;

    private int mSubIdLength;    // available subId length
    private long[] mSubIdList;    // available subId list
    private String[] mSimImsi;
    private boolean[] mSimEmpty; // whether slot has no card
    private boolean[] mSimReady; // whether sim card is ready

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        mPlatformManager = SmsRegApplication.getPlatformManager();
        mXmlG = XmlGenerator.getInstance(mPlatformManager.getConfigPath());

        mOperatorId = mXmlG.getOperatorName();
        Log.i(TAG, "The operator Id = " + mOperatorId);

        mSubIdList = new long[SUB_NUMBER];
        mSimImsi = new String[SUB_NUMBER];
        mSimEmpty = new boolean[SUB_NUMBER];
        mSimReady = new boolean[SUB_NUMBER];
        mPhoneStateListener = new CustomPhoneStateListener[SUB_NUMBER];
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBind");
        return null;
    }

    /**
     * 1. BOOT_COMPLETED: prepare receiver & alarm for different operator
     * a.1 DISPLAY_DIALOG: check condition, then show confirm dialog
     * a.2 RESPONSE_DIALOG: get response, send message or block IMSI
     * b.1 SIM_STATE_CHANGED: send message after all sim ready
     * b.2 RETRY_SEND_SMS: retry to send message when timeout
     * 3. FINISH_SEND_SMS: save IMSI if message sent successfully
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand, intent is " + intent);

        if (intent == null) {
            Log.e(TAG, "intent is null!");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "intent action is null!");
            return START_NOT_STICKY;
        }

        if (action.equals(SmsRegConst.ACTION_BOOT_COMPLETED)) {
            processBoot();

        } else if (action.equals(SmsRegConst.ACTION_DISPLAY_DIALOG)) {
            registerReceivedReceiver();
            processDisplayDiag();

        } else if (action.equals(SmsRegConst.ACTION_RESPONSE_DIALOG)) {
            registerReceivedReceiver();
            processResponseDiag(intent);

        } else if (action.equals(SmsRegConst.ACTION_SIM_STATE_CHANGED)) {
            processStateChange(intent);

        } else if (action.equals(SmsRegConst.ACTION_RETRY_SEND_SMS)) {
            registerReceivedReceiver();
            trySendMessage();

        } else if (action.equals(SmsRegConst.ACTION_FINISH_SEND_SMS)) {
            processFinishSendSms(intent);

        } else {
            Log.e(TAG, "Get the wrong intent");
            stopService();
        }

        return START_NOT_STICKY;
    }

    /**
     * After boot complete, prepare receiver & alarm for different operator
     */
    private void processBoot() {

        if (mOperatorId.equalsIgnoreCase("cu")) {
            registerStateReceiver();
            registerReceivedReceiver();

            startRetrySendAlarm();

        } else if (mOperatorId.equalsIgnoreCase("cmcc")) {
            mBlackListUnit = new BlackListUnit(this);
            startDisplayDialogAlarm();

        } else {
            Log.i(TAG, "Unknown operator Id = " + mOperatorId);
            stopService();
        }
    }

    /**
     * Display dialogue, after check savedIMSI & black list
     */
    private void processDisplayDiag() {
        initImsiInfo();

        if (isNeedRegister()) {
            int index = mBlackListUnit.getMinAvailId(mSimImsi);
            if (index != -1) {
                mSendSubId = mSubIdList[index];
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(this, SendMessageAlertActivity.class);
                startActivity(intent);
                return;
            }
            Log.i(TAG, "No sim (or no certain operator sim)");
        } else {
            Log.i(TAG, "No need to register, stop service.");
        }
        stopService();
    }

    /**
     * Extract user response, send registered message or block IMSI
     */
    private void processResponseDiag(Intent intent) {

        boolean isNeedSendMsg = intent.getBooleanExtra(SmsRegConst.EXTRA_IS_NEED_SEND, false);

        Log.i(TAG, "sub Id is " + mSendSubId + ", user agree? " + isNeedSendMsg);

        if (mSendSubId != -1) {
            if (isNeedSendMsg) {
                sendRegisterMessage(mSendSubId);
                return;
            } else {
                mBlackListUnit.blockImsi(mPlatformManager.getSubImsi(mSendSubId), mPlatformManager.getSlotId(mSendSubId));
                stopService();
            }
        } else {
            throw new Error("Error sub id " + mSendSubId);
        }
    }

    // TODO: CU flow, update to using sub
    /**
     * When state of one sim changes, try to send registered message if all sim ready
     */
    private void processStateChange(Intent intent) {

        // 1. Whether state is loaded
        String stateExtra = intent.getStringExtra(SmsRegConst.KEY_ICC_STATE);
        if (!SmsRegConst.VALUE_ICC_LOADED.equals(stateExtra)) {
            Log.i(TAG, "sim state is not loaded");
            return;
        }

        // 2. All sim cards are ready
        if (SUB_NUMBER > 1) {
            if (!isAllSimReady()) {
                return;
            }
        }

        // 3. Whether device is registered already
        if (SUB_NUMBER > 1) {
            mSavedImsi = mPlatformManager.getSavedImsi();
            if (mSavedImsi != null) {
                for (int i = 0; i < SUB_NUMBER; ++i) {
                    String cheImsi = mPlatformManager.getSubImsi(getSimIdFromIndex(i));
                    Log.i(TAG, "Check sim[" + i + "].");
                    if (cheImsi != null && mSavedImsi.equals(cheImsi)) {
                        Log.i(TAG, "Device already registered, stop service.");
                        stopService();
                        return;
                    }
                } // end for
            }
        }

        registerPhoneListener();
        trySendMessage();
    }

    /**
     * After finishing send message, save IMSI (reset black files if needed)
     */
    private void processFinishSendSms(Intent intent) {

        int resultCode = intent.getIntExtra(SmsRegConst.EXTRA_RESULT_CODE, Activity.RESULT_OK);
        if (resultCode == Activity.RESULT_OK) {
            String imsi = intent.getStringExtra(SmsRegConst.EXTRA_IMSI);

            Log.w(TAG, "The IMSI to save is " + imsi + ".");
            if (imsi != null && !(imsi.equals(""))) {
                mPlatformManager.setSavedImsi(imsi);

                // Currently only for cmcc, need to reset black files
                if (mBlackListUnit != null) {
                    mBlackListUnit.resetBlackFile();

                    // Notify MediatekDM to clear permission files
                    sendBroadcast(new Intent(SmsRegConst.DM_SMSREG_MESSAGE_NEW));
                }
            } else {
                Log.e(TAG, "Error imsi info " + imsi);
            }
        } else {
            Log.e(TAG, "Fail to send message, result code is " + resultCode);
        }
        stopService();
    }

    // TODO: CU flow, update to using sub
    /**
     * Whether all sim are ready (slot empty or could read IMSI info).
     *   In a scenario, SIM_STATE_CHANGED sim 1 -> BOOT_COMPLETED -> SIM_STATE_CHANGED sim 2, we will miss the first
     *   intent. So check state of all sims when receiving an "SIM_STATE_CHANGED" intent.
     */
    private boolean isAllSimReady() {
        for (int i = 0; i < SUB_NUMBER; ++i) {
            int simId = getSimIdFromIndex(i);
            if (!mPlatformManager.hasSimCard(simId)) {
                mSimEmpty[i] = true;
                Log.i(TAG, "sim[" + i + "] is empty.");
            } else {
                Log.i(TAG, "sim[" + i + "] is not empty.");
                if (mPlatformManager.getSubImsi(simId) != null) {
                    mSimReady[i] = true;
                    Log.i(TAG, "sim[" + i + "] can read IMSI.");
                }
            }
        }

        for (int i = 0; i < SUB_NUMBER; ++i) {
            if (!mSimEmpty[i] && !mSimReady[i]) {
                Log.i(TAG, "Need Wait for sim " + i + " to ready.");
                return false;
            }
        }
        Log.i(TAG, "All sim are ready.");
        return true;
    }

    /**
     * Init IMSI info, try to send registered message if not registered
     */
    private void trySendMessage() {
        initImsiInfo();

        if (isNeedRegister()) {
            for (int i = 0; i < mSubIdLength; ++i) {
                if (mSimImsi[i] != null) {
                    sendRegisterMessage(mSubIdList[i]);
                    return;
                }
            }
            Log.i(TAG, "No sim (or no certain operator sim)");
        } else {
            Log.i(TAG, "Sim registered already.");
        }

        stopService();
    }

    /**
     * Init sim cards' IMSI info to mImsi array
     */
    private void initImsiInfo() {
        mSubIdLength = mPlatformManager.initSubIdList(mSubIdList);

        for (long id: mSubIdList) {
            Log.i(TAG, "[initSubIdList] id is " + id);
        }

        String[] optrNumber = mXmlG.getNetworkNumber();
        Log.i(TAG, "Operator " + mOperatorId + " has " + optrNumber.length + " number.");

        for (int i = 0; i < mSubIdLength; i++) {
            long subId = mSubIdList[i];

            int simState = mPlatformManager.getSubState(subId);
            if (SmsRegConst.SIM_STATE_READY != simState) {
                Log.i(TAG, "Sim " + subId + " not ready, state is " + simState);
                continue;
            }

            String simOperator = mPlatformManager.getSubOperator(subId);
            Log.i(TAG, "Sim " + subId + " ready, operator is " + simOperator);

            if (simOperator == null || simOperator.trim().equals("")) {
                Log.i(TAG, "operator is null, continue next one. ");
                continue;
            }

            // if operator match, initiate IMSI
            for (int j = 0; j < optrNumber.length; j++) {
                if (optrNumber[j] != null && optrNumber[j].equals(simOperator)) {
                    mSimImsi[i] = mPlatformManager.getSubImsi(subId);
                    Log.i(TAG, "Operator " + optrNumber[j] + " match, init mImsi[" + i + "] " + mSimImsi[i]);
                    break;
                } else {
                    Log.i(TAG, "Operator[" + j + "] " + optrNumber[j] + " not match simOperator " + simOperator);
                }
            }
        }
    }

    /**
     * Get sim id (PhoneConstants.GEMINI_SIM_1, *_2, *3 , *4) from array index (0, 1 ,2, 3)
     */
    private int getSimIdFromIndex(int index) {
        int simId = index + SmsRegConst.GEMSIM[0];
        return simId;
    }

    /**
     * Get array index (0, 1 ,2 3) from sim id (PhoneConstants.GEMINI_SIM_1, *_2, *3 , *4)
     */
    private int getIndexFromSimId(int simId) {
        int index = simId - SmsRegConst.GEMSIM[0];
        return index;
    }

    /**
     * Whether device need to register
     */
    private Boolean isNeedRegister() {
        mSavedImsi = mPlatformManager.getSavedImsi();
        Log.i(TAG, "Saved IMSI is " + mSavedImsi + ".");

        if (mSavedImsi == null) {
            return true;
        }

        for (int i = 0; i < mSubIdLength; ++i) {
            Log.i(TAG, "mImsi[" + i + "] is " + mSimImsi[i] + ".");

            if (mSimImsi[i] != null && mSavedImsi.equals(mSimImsi[i])) {
                Log.w(TAG, "Device registered already.");
                return false;
            }
        }

        Log.i(TAG, "Saved imsi not match, need register.");
        return true;
    }

    /**
     * Send the register message from a certain sim
     */
    private void sendRegisterMessage(long subId) {
        String simCountryIso = mPlatformManager.getSubCountryIso(subId);
        String networkIso = mPlatformManager.getNetworkCountryIso(subId);
        Log.i(TAG, "SimCountryIso = " + simCountryIso + ", networkIso= " + networkIso);

        if (simCountryIso == null || !simCountryIso.equals(networkIso)) {
            Log.w(TAG, "SimCountryIso is not equals with NetworkCountryIso, do nothing");
            return;
        }

        String optAddr = mXmlG.getSmsNumber();
        Short optPort = mXmlG.getSmsPort();
        Short srcPort = mXmlG.getSrcPort();
        Log.i(TAG, "Operator " + mOperatorId + "'s dest number is " + optAddr + ", dest port is " + optPort
                + ", src port = " + srcPort);

        String smsRegMsg = SmsBuilder.getSmsContent(mXmlG, subId);
        if (smsRegMsg != null) {
            if (mIsSendMsg) {
                Log.i(TAG, "Register message sent already. ");
            } else {
                Log.i(TAG, "Send message: " + smsRegMsg + " from sim " + subId);

                Intent intent = new Intent(SmsRegConst.ACTION_FINISH_SEND_SMS);
                intent.putExtra(SmsRegConst.EXTRA_IMSI, mPlatformManager.getSubImsi(subId));
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                mPlatformManager.sendDataMessage(optAddr, optPort, srcPort, smsRegMsg.getBytes(), pendingIntent, subId);
                mIsSendMsg = true;
            }
        } else {
            Log.e(TAG, "Register message is null, stop service");
            stopService();
        }
    }

    /**
     * Start an alarm which retries to send message (in case process killed by system)
     */
    private void startRetrySendAlarm() {
        Intent retryIntent = new Intent(SmsRegConst.ACTION_RETRY_SEND_SMS);
        retryIntent.setClass(this, SmsRegService.class);
        mPendingIntent = PendingIntent.getService(this, 0, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mPlatformManager.startAlarm(this, mPendingIntent, DELAY_SERARCH_NET);
    }

    /**
     * Start an alarm which tells service to display confirm dialogue if needed
     */
    private void startDisplayDialogAlarm() {
        Intent prepareIntent = new Intent(SmsRegConst.ACTION_DISPLAY_DIALOG);
        prepareIntent.setClass(this, SmsRegService.class);
        mPendingIntent = PendingIntent.getService(this, 0, prepareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mPlatformManager.startAlarm(this, mPendingIntent, DEALY_SHOW_DIALOG);
    }

    /**
     * Register a receiver to listen "FINISH_SEND_SMS" when registered message sent successfully
     */
    private void registerReceivedReceiver() {
        if (mSmsReceivedReceiver == null) {
            mSmsReceivedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    intent.setClass(context, SmsRegService.class);
                    intent.putExtra(SmsRegConst.EXTRA_RESULT_CODE, getResultCode());
                    context.startService(intent);
                }
            };
            registerReceiver(mSmsReceivedReceiver, new IntentFilter(SmsRegConst.ACTION_FINISH_SEND_SMS));
        }
    }

    // TODO: CU flow, update to using sub
    /**
     * Register a receiver to listen "SIM_STATE_CHANGED"
     */
    private void registerStateReceiver() {
        mSimStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intent.setClass(context, SmsRegService.class);
                context.startService(intent);
            }
        };
        registerReceiver(mSimStateReceiver, new IntentFilter(SmsRegConst.ACTION_SIM_STATE_CHANGED));
    }

    // TODO: CU flow, update to using sub
    /**
     * Register listeners to listen state of sim
     */
    private void registerPhoneListener() {
        for (int i = 0; i < SUB_NUMBER; ++i) {
            mPhoneStateListener[i] = new CustomPhoneStateListener(i);
            mPlatformManager.listenPhoneState(mPhoneStateListener[i], getSimIdFromIndex(i));
        }
    }

    /**
     * Unregister receivers and listener, stop alarm, then stop service
     */
    private void stopService() {
        Log.i(TAG, "stop service.");

        unRegisterStateReceiver();
        unRegisterReceivedReceiver();
        unRegisterPhoneStateListener();
        stopAlarm();
        stopSelf();
    }

    /**
     * Unregister the receiver listening "FINISH_SEND_SMS"
     */
    private void unRegisterReceivedReceiver() {
        if (mSmsReceivedReceiver != null) {
            Log.i(TAG, "unRegist sms Received receiver.");
            unregisterReceiver(mSmsReceivedReceiver);
            mSmsReceivedReceiver = null;
        }
    }

    // TODO: CU flow, update to using sub
    /**
     * Unregister the receiver listening "SIM_STATE_CHANGED"
     */
    private void unRegisterStateReceiver() {
        if (mSimStateReceiver != null) {
            Log.i(TAG, "unRegist sim state receiver.");
            unregisterReceiver(mSimStateReceiver);
            mSimStateReceiver = null;
        }
    }

    // TODO: CU flow, update to using sub
    /**
     * Stop the prepare/retry alarm before stop
     */
    private void unRegisterPhoneStateListener() {
        for (int i = 0; i < SUB_NUMBER; ++i) {
            if (mPhoneStateListener[i] != null) {
                mPlatformManager.unListenPhoneState(mPhoneStateListener[i], getSimIdFromIndex(i));
            }
        }
    }

    /**
     * Stop the prepare/retry alarm before stop
     */
    private void stopAlarm() {
        if (mPendingIntent != null) {
            Log.i(TAG, "cancel alarm message.");
            mPlatformManager.cancelAlarm(this, mPendingIntent);
            mPendingIntent = null;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mXmlG = null;
        mBlackListUnit = null;
    }

    // TODO: CU flow, update to using sub
    class CustomPhoneStateListener extends PhoneStateListener {
        private int mIndex;

        public CustomPhoneStateListener(int index) {
            mIndex = index;
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.i(TAG, "Service state change, state is " + serviceState.getState());
            if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
                initImsiInfo();

                // The former sims' IMSI are all null
                for (int i = 0; i < mIndex; ++i) {
                    if (mSimImsi[i] != null) {
                        Log.e(TAG, "Should use sim[" + i + "] to register");
                        return;
                    }
                }

                if (mSimImsi[mIndex] != null) {
                    if (isNeedRegister() && !mIsSendMsg) {
                        sendRegisterMessage(getSimIdFromIndex(mIndex));
                    } else {
                        Log.i(TAG, "Registered already or message has sent.");
                    }
                } else {
                    Log.e(TAG, "Sim " + mIndex + " no need or wrong operator");
                }
            }
        }
    }
}
