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

package com.mediatek.ims;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import android.telecom.VideoProfile;

import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import android.text.TextUtils;

import com.android.ims.ImsCallProfile;
import com.android.ims.ImsConferenceState;
import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsStreamMediaProfile;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.ims.internal.ImsCallSession;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.LastCallFailCause;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gsm.CallFailCause;
import com.mediatek.ims.WfcReasonInfo;
import com.mediatek.internal.telephony.ConferenceCallMessageHandler;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

import com.mediatek.ims.internal.CallControlDispatcher;
// For ViLTE feature.
import com.mediatek.ims.internal.ImsVTProvider;

// ALPS02136981. Prints fomatted debug logs.
import com.mediatek.telecom.FormattedLog;

import com.mediatek.wfo.DisconnectCause;
import com.mediatek.wfo.IWifiOffloadListener;
import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.WifiOffloadManager;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import java.lang.Math;

import java.util.Arrays;
import java.util.List;

public class ImsCallSessionProxy extends IImsCallSession.Stub {
    private static final String LOG_TAG = "ImsCallSessionProxy";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true

    private String mCallId;
    private int mState = ImsCallSession.State.IDLE;
    private Context mContext;
    private ImsService mImsService;
    private ImsRILAdapter mImsRILAdapter;
    private ImsCallProfile mCallProfile;
    private IImsCallSessionListener mListener;
    private final Handler mHandler;
    private final Handler mServiceHandler;
    private boolean mHasPendingMo = false;
    private boolean mIsMerging = false;
    private boolean mIsOnTerminated = false;
    private boolean mIsAddRemoveParticipantsCommandOK = false;
    private String[] mPendingParticipantInfo ;
    private int mPendingParticipantInfoIndex = 0;
    private int mPendingParticipantStatistics = 0;
    private boolean mIsHideHoldEventDuringMerging = false;
    private String mMergeCallId = "";
    private ImsCallInfo.State mMergeCallStatus = ImsCallInfo.State.INVALID;
    private String mMergedCallId = "";
    private ImsCallInfo.State mMergedCallStatus = ImsCallInfo.State.INVALID;
    // normal call merge normal call
    private boolean mNormalCallsMerge = false;
    // at least one call is merged successfully
    private boolean mThreeWayMergeSucceeded = false;
    // count for +ECONF number in normal call merge normal call case
    private int mEconfCount = 0;
    private IImsCallSession mConfSession;

    private String mCallNumber;

    // WFC
    private IWifiOffloadService mWfoService;
    private int mRatType = WifiOffloadManager.RAN_TYPE_MOBILE_3GPP;
    private static final int WFC_GET_CAUSE_FAILED = -1;

    // For ViLTE.
    private ImsVTProvider mVTProvider;
    private ImsCallProfile mLocalCallProfile;
    private ImsCallProfile mRemoteCallProfile;

    private enum CallErrorState {
        IDLE, DIAL, DISCONNECT;
    };

    private CallErrorState mCallErrorState = CallErrorState.IDLE;

    private Message mDtmfMsg = null;
    private Messenger mDtmfTarget = null;

    private static final int INVALID_CALL_MODE = 0xFF;
    private static final int IMS_VOICE_CALL = 20;
    private static final int IMS_VIDEO_CALL = 21;
    private static final int IMS_VOICE_CONF = 22;
    private static final int IMS_VIDEO_CONF = 23;
    private static final int IMS_VOICE_CONF_PARTS = 24;
    private static final int IMS_VIDEO_CONF_PARTS = 25;

    //***** Events URC
    private static final int EVENT_POLL_CALLS_RESULT             = 101;
    private static final int EVENT_CALL_INFO_INDICATION          = 102;
    private static final int EVENT_RINGBACK_TONE                 = 103;
    private static final int EVENT_ECONF_RESULT_INDICATION       = 104;
    private static final int EVENT_GET_LAST_CALL_FAIL_CAUSE      = 105;
    private static final int EVENT_CALL_MODE_CHANGE_INDICATION   = 106;
    private static final int EVENT_VIDEO_CAPABILITY_INDICATION   = 107;

    //***** Events Operation result
    private static final int EVENT_DIAL_RESULT                   = 201;
    private static final int EVENT_ACCEPT_RESULT                 = 202;
    private static final int EVENT_HOLD_RESULT                   = 203;
    private static final int EVENT_RESUME_RESULT                 = 204;
    private static final int EVENT_MERGE_RESULT                  = 205;
    private static final int EVENT_ADD_CONFERENCE_RESULT         = 206;
    private static final int EVENT_REMOVE_CONFERENCE_RESULT      = 207;
    private static final int EVENT_SIP_CODE_INDICATION           = 208;
    private static final int EVENT_DIAL_CONFERENCE_RESULT        = 209;
    private static final int EVENT_RETRIEVE_MERGE_FAIL_RESULT    = 211;
    private static final int EVENT_DTMF_DONE    = 212;

    //Constructor for MT call
    ImsCallSessionProxy(Context context, ImsCallProfile profile, IImsCallSessionListener listener, ImsService imsService, 
            Handler handler, ImsRILAdapter imsRILAdapter, String callId) {
        if (DBG) {
            Rlog.d(LOG_TAG, "ImsSessionProxy RILAdapter:" + imsRILAdapter + "imsService:" + imsService + " callID:" + callId);
        }
        mServiceHandler = handler;
        mHandler = new MyHandler(handler.getLooper());
        mContext = context;
        mCallProfile = profile;
        mLocalCallProfile = profile;
        mRemoteCallProfile = profile;
        mListener = listener;
        mImsService = imsService;
        mImsRILAdapter = imsRILAdapter;
        mCallId = callId;
        mImsRILAdapter.registerForCallInfo(mHandler, EVENT_CALL_INFO_INDICATION, null);
        mImsRILAdapter.registerForRingbackTone(mHandler, EVENT_RINGBACK_TONE, null);
        /// M: Register for updating conference call merged/added result.
        mImsRILAdapter.registerForEconfResult(mHandler, EVENT_ECONF_RESULT_INDICATION, null);
        mImsRILAdapter.registerForCallProgressIndicator(mHandler, EVENT_SIP_CODE_INDICATION, null);
        mImsRILAdapter.registerForCallModeChangeIndicator(mHandler,
                EVENT_CALL_MODE_CHANGE_INDICATION, null);
        mImsRILAdapter.registerForVideoCapabilityIndicator(mHandler,
                EVENT_VIDEO_CAPABILITY_INDICATION, null);

        if (SystemProperties.get("ro.mtk_vilte_support").equals("1")) {
            if (mCallId != null) {
                //MT:new VT service
                mVTProvider = new ImsVTProvider(Integer.parseInt(mCallId));
            } else {
                //MO:new VT service
                mVTProvider = new ImsVTProvider();
            }
        }

        /* Register for receiving conference call xml message */
        final IntentFilter filter = new IntentFilter();
        filter.addAction(CallControlDispatcher.ACTION_IMS_CONFERENCE_CALL_INDICATION);
        // WFC: Registers the listener to WifiOffloadService for handover event and get rat type
        // from WifiOffloadService.
        IBinder b = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);
        mWfoService = IWifiOffloadService.Stub.asInterface(b);
        if (mWfoService != null) {
            try {
                mWfoService.registerForHandoverEvent(new IWifiOffloadListenerProxy());
                mRatType = mWfoService.getRatType();
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "RemoteException ImsCallSessionProxy()");
            }
        }
        Rlog.d(LOG_TAG, "[WFC]mRatType is " + mRatType);

        context.registerReceiver(mBroadcastReceiver, filter);
        mConfSession = null;
    }

    //Constructor for MO call
    ImsCallSessionProxy(Context context, ImsCallProfile profile, IImsCallSessionListener listener, ImsService imsService,
            Handler handler, ImsRILAdapter imsRILAdapter) {
        this(context, profile, listener, imsService, handler, imsRILAdapter, null);
        if (DBG) {
            Rlog.d(LOG_TAG, "ImsCallSessionProxy RILAdapter:" + imsRILAdapter);
        }
    }

    @Override
    public void close() {
        if (DBG) {
            Rlog.d(LOG_TAG, "ImsCallSessionProxy is closed!!! ");
        }
        mState = ImsCallSession.State.INVALID;
        mImsRILAdapter.unregisterForCallInfo(mHandler);
        mImsRILAdapter.unregisterForRingbackTone(mHandler);
        mImsRILAdapter.unregisterForEconfResult(mHandler);
        mImsRILAdapter.unregisterForCallProgressIndicator(mHandler);
        mImsRILAdapter.unregisterForCallModeChangeIndicator(mHandler);
        mImsRILAdapter.unregisterForVideoCapabilityIndicator(mHandler);

        if (mContext != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }
    }

    @Override
    public String getCallId() {
        return mCallId;
    }

    @Override
    public ImsCallProfile getCallProfile() {
        return mCallProfile;
    }

    @Override
    public ImsCallProfile getLocalCallProfile() {
        return mLocalCallProfile;
    }

    @Override
    public ImsCallProfile getRemoteCallProfile() {
        return mRemoteCallProfile;
    }

    @Override
    public String getProperty(String name) {
        return mCallProfile.getCallExtra(name);
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public boolean isInCall() {
        return false;
    }

    @Override
    public void setListener(IImsCallSessionListener listener) {
        mListener = listener;
    }

    @Override
    public void setMute(boolean muted) {
        mImsRILAdapter.setMute(muted, null);
    }

    @Override
    public void start(String callee, ImsCallProfile profile) {
        int clirMode = profile.getCallExtraInt(ImsCallProfile.EXTRA_OIR, 0);
        boolean isVideoCall = false;
        boolean isEmergencyNumber = false;
        Message result = mHandler.obtainMessage(EVENT_DIAL_RESULT);

        if (profile.mServiceType == ImsCallProfile.SERVICE_TYPE_EMERGENCY) {
            isEmergencyNumber = true;
        }

        if (profile.getVideoStateFromImsCallProfile(profile) !=
                VideoProfile.STATE_AUDIO_ONLY) {
            isVideoCall = true;
        }
        mImsRILAdapter.start(callee, clirMode, isEmergencyNumber, isVideoCall, result);
        mHasPendingMo = true;
        mCallNumber = callee;
    }

    @Override
    public void startConference(String[] participants, ImsCallProfile profile) {
        int clirMode = profile.getCallExtraInt(ImsCallProfile.EXTRA_OIR, 0);
        boolean isVideoCall = false;
        Message result = mHandler.obtainMessage(EVENT_DIAL_CONFERENCE_RESULT);

        if (profile.getVideoStateFromImsCallProfile(profile) !=
                VideoProfile.STATE_AUDIO_ONLY) {
            isVideoCall = true;
        }
        mImsRILAdapter.startConference(participants, clirMode, isVideoCall, result);
        mHasPendingMo = true;
    }

    @Override
    public void accept(int callType, ImsStreamMediaProfile profile) {
        Rlog.d(LOG_TAG, "accept - original call Type:" + mCallProfile.mCallType
                + "accept as:" + callType);
        if (mCallProfile.mCallType == ImsCallProfile.CALL_TYPE_VOICE) {
            mImsRILAdapter.accept();
        } else {
            int videoMode;
            /* We set the videoMode base on AT+EVTA mode value.
             * AT+EVTA=<mode>,<call id>
             * Mode  =1 , accept as audio
             * Mode  =2 , accept as one way only video (Rx)
             * Mode  =3 , accept as one way only video (Tx)
             * For videoMode = 0, we will use ATA command to accept this video call.
             */
            switch (callType) {
                case ImsCallProfile.CALL_TYPE_VT:
                    videoMode = 0;
                    break;
                case ImsCallProfile.CALL_TYPE_VOICE:
                    videoMode = 1;
                    break;
                case ImsCallProfile.CALL_TYPE_VT_RX:
                    videoMode = 2;
                    break;
                case ImsCallProfile.CALL_TYPE_VT_TX:
                    videoMode = 3;
                    break;
                default:
                    videoMode = 0;
                    break;
            }
            mImsRILAdapter.acceptVideoCall(videoMode, Integer.parseInt(mCallId));
        }
    }

    @Override
    public void reject(int reason) {
        if (mCallId != null) {
            mImsRILAdapter.reject(Integer.parseInt(mCallId));
        } else {
            Rlog.e(LOG_TAG, "Reject Call fail since there is no call ID. Abnormal Case");
        }
    }

    @Override
    public void terminate(int reason) {
        if (mCallId != null) {
            mImsRILAdapter.terminate(Integer.parseInt(mCallId));
        } else {
            Rlog.e(LOG_TAG, "Terminate Call fail since there is no call ID. Abnormal Case");
        }
    }

    @Override
    public void hold(ImsStreamMediaProfile profile) {
        Message result = mHandler.obtainMessage(EVENT_HOLD_RESULT);
        mImsRILAdapter.hold(Integer.parseInt(mCallId), result);
    }

    @Override
    public void resume(ImsStreamMediaProfile profile) {
        Message result = mHandler.obtainMessage(EVENT_RESUME_RESULT);
        mImsRILAdapter.resume(Integer.parseInt(mCallId), result);
    }

    @Override
    public void merge() {
        Message result;
        Rlog.e(LOG_TAG, "Merge callId:" + mCallId);
        ImsCallInfo myCallInfo = mImsRILAdapter.getCallInfo(mCallId);
        ImsCallInfo beMergedCallInfo = null;

        if (myCallInfo == null) {
            Rlog.e(LOG_TAG, "can't find this call callInfo");
            mergeFailed();
            return;
        }

        if (myCallInfo.mState == ImsCallInfo.State.ACTIVE) {
            beMergedCallInfo = mImsRILAdapter.getCallInfo(ImsCallInfo.State.HOLDING);
        } else if (myCallInfo.mState == ImsCallInfo.State.HOLDING) {
            beMergedCallInfo = mImsRILAdapter.getCallInfo(ImsCallInfo.State.ACTIVE);
        }

        if (beMergedCallInfo == null) {
            Rlog.e(LOG_TAG, "can't find another call's callInfo");
            mergeFailed();
            return;
        }

        Rlog.d(LOG_TAG, "merge command- my call: conference type=" + myCallInfo.mIsConference +
                " call status=" + myCallInfo.mState + "beMergedCall: conference type=" +
                beMergedCallInfo.mIsConference + " call status=" + beMergedCallInfo.mState);

        mMergeCallId = myCallInfo.mCallId;
        mMergeCallStatus = myCallInfo.mState;
        mMergedCallId = beMergedCallInfo.mCallId;
        mMergedCallStatus = beMergedCallInfo.mState;

        if (myCallInfo.mIsConference == false && beMergedCallInfo.mIsConference == false) {
            //Case 1: Normal call merge normal call
            result = mHandler.obtainMessage(EVENT_MERGE_RESULT);
            mImsRILAdapter.merge(result);
            mIsHideHoldEventDuringMerging = true;
            mNormalCallsMerge = true;
        } else if (myCallInfo.mIsConference == true && beMergedCallInfo.mIsConference == true) {
            // Case 2: conference call merge conference call
            Rlog.d(LOG_TAG, "conference call merge conference call");
            result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
            mImsRILAdapter.inviteParticipants(Integer.parseInt(mCallId),
                    beMergedCallInfo.mCallNum, result);
            return;
        } else {
            if (myCallInfo.mIsConference) {
                Rlog.d(LOG_TAG, "active conference call merge background normal call");
                result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                mImsRILAdapter.inviteParticipants(Integer.parseInt(mCallId),
                        beMergedCallInfo.mCallNum, result);
            } else {
                Rlog.d(LOG_TAG, "active normal call merge background conference call");
                result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
                mImsRILAdapter.inviteParticipants(Integer.parseInt(beMergedCallInfo.mCallId),
                        myCallInfo.mCallNum, result);
            }
        }
        mIsMerging = true;
    }

    @Override
    public void update(int callType, ImsStreamMediaProfile profile) {
        // currently MD not support for video downgrade or audio upgrade.
    }

    @Override
    public void extendToConference(String[] participants) {
        // currently MD not support to join multiple participants to join conference call.
    }

    @Override
    public void inviteParticipants(String[] participants) {
        Message result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);
        mPendingParticipantInfoIndex = 0;
        mPendingParticipantInfo = participants;
        mPendingParticipantStatistics = participants.length;
        if (mCallId != null || mPendingParticipantStatistics == 0) {
            mImsRILAdapter.inviteParticipants(Integer.parseInt(mCallId),
                    mPendingParticipantInfo[mPendingParticipantInfoIndex], result);
        } else {
            Rlog.e(LOG_TAG, "inviteParticipants fail since no call ID or participants is null" +
                    " CallID=" + mCallId + " Participant number=" + mPendingParticipantStatistics);
            if (mListener != null) {
                try {
                    mListener.callSessionInviteParticipantsRequestFailed(
                            ImsCallSessionProxy.this, new ImsReasonInfo());
                } catch (RemoteException e) {
                    Rlog.e(LOG_TAG, "RemoteException occurs when InviteParticipantsRequestFailed");
                }
            }
        }
    }

    @Override
    public void removeParticipants(String[] participants) {
        Message result = mHandler.obtainMessage(EVENT_REMOVE_CONFERENCE_RESULT);
        mPendingParticipantInfoIndex = 0;
        mPendingParticipantInfo = participants;
        mPendingParticipantStatistics = participants.length;
        if (mCallId != null || mPendingParticipantStatistics == 0) {
            mImsRILAdapter.removeParticipants(Integer.parseInt(mCallId),
                    mPendingParticipantInfo[mPendingParticipantInfoIndex], result);
        } else {
            Rlog.e(LOG_TAG, "removeParticipants fail since no call ID or participants is null" +
                    " CallID=" + mCallId + " Participant number=" + mPendingParticipantStatistics);
            if (mListener != null) {
                try {
                    mListener.callSessionRemoveParticipantsRequestFailed(
                            ImsCallSessionProxy.this, new ImsReasonInfo());
                } catch (RemoteException e) {
                    Rlog.e(LOG_TAG, "RemoteException occurs when RemoveParticipantsRequestFailed");
                }
            }
        }
    }

    @Override
    public void sendDtmf(char c, Message result) {
        mImsRILAdapter.sendDtmf(c, result);
    }

    @Override
    public void startDtmf(char c) {
        mImsRILAdapter.startDtmf(c, null);
    }

    @Override
    public void stopDtmf() {
        mImsRILAdapter.stopDtmf(null);
    }

    // Google issue. Original sendDtmf could not pass Message.target to another process,
    // because Message.writeToParcel didn't write target. Workaround this issue by adding
    // a new API which passes target by Messenger.
    @Override
    public void sendDtmfbyTarget(char c, Message result, Messenger target) {
        mDtmfMsg = result;
        mDtmfTarget = target;
        // Use ImsCallSessionProxy handler to send result back to original Message target.
        Message local_result = mHandler.obtainMessage(EVENT_DTMF_DONE);
        mImsRILAdapter.sendDtmf(c, local_result);
    }

    @Override
    public void sendUssd(String ussdMessage) {
    }

    @Override
    public IImsVideoCallProvider getVideoCallProvider() {
        Rlog.d(LOG_TAG, "getVideoCallProvider: mVTProvider= " + mVTProvider);
        if (mVTProvider != null) {
            return mVTProvider.getInterface();
        } else {
            return null;
        }
    }

    @Override
    public boolean isMultiparty() {
        return mCallProfile.getCallExtraInt(ImsCallProfile.EXTRA_MPTY, 0) == 1;
    }

    @Override
    public boolean isIncomingCallMultiparty() {
        return mCallProfile.getCallExtraInt(ImsCallProfile.EXTRA_INCOMING_MPTY, 0) == 1;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        /**
        * To handle IMS conference call message
        *
        * @param len    The length of data
        * @param data   Conference call message
        */

        private void handleImsConfCallMessage(int len, int callId, String data) {
            try {
                if ((data == null) || (data.equals(""))) {
                    Rlog.e(LOG_TAG, "Failed to handleImsConfCallMessage due to data is empty");
                    return;
                }

                Rlog.d(LOG_TAG, "handleVoLteConfCallMessage, data length = " + data.length() +
                        "callId = " + callId);

                //ALPS02136981. Prints debug messages for ImsPhone.
                logDebugMessagesWithNotifyFormat("CC", "ConfXMLNotify", "conferenceCall", data);

                // Write conference call data to file
                String file = "/sdcard/conferenceCall.xml";
                //For VoLTE testing purpose, mark it temporarily.
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
                out.write(data, 0, data.length());
                out.close();

                //Read conference call file and parse it
                InputStream inStream = new BufferedInputStream(new FileInputStream(file));
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParse = factory.newSAXParser();
                ConferenceCallMessageHandler xmlData = new ConferenceCallMessageHandler();
                if (xmlData == null) {
                    Rlog.e(LOG_TAG, "can't create xmlData object");
                    return;
                }
                saxParse.parse(inStream, xmlData);

                //get user data from xml and fill them into ImsConferenceState data structure.
                List<ConferenceCallMessageHandler.User> users = xmlData.getUsers();
                int i = 0;
                ImsConferenceState imsConferenceState = new ImsConferenceState();


                for (ConferenceCallMessageHandler.User u : users) {
                    i++;
                    Bundle confInfo = new Bundle();
                    confInfo.putString(ImsConferenceState.USER,
                            getUserNameFromSipTelUriString(u.getEntity()));
                    confInfo.putString(ImsConferenceState.DISPLAY_TEXT, u.getDisplayText());
                    confInfo.putString(ImsConferenceState.ENDPOINT, u.getEndPoint());
                    String state = u.getStatus();
                    if (state.equals(ConferenceCallMessageHandler.STATUS_PENDING)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_PENDING);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_DIALING_OUT)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_DIALING_OUT);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_DIALING_IN)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_DIALING_IN);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_ALERTING)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_ALERTING);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_ON_HOLD)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_ON_HOLD);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_CONNECTED)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_CONNECTED);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_DISCONNECTING)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_DISCONNECTING);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_DISCONNECTED)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_DISCONNECTED);
                    } else if (state.equals(ConferenceCallMessageHandler.STATUS_MUTED_VIA_FOCUS)) {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_MUTED_VIA_FOCUS);
                    } else {
                        confInfo.putString(ImsConferenceState.STATUS,
                                ImsConferenceState.STATUS_CONNECT_FAIL);
                    }
                    imsConferenceState.mParticipants.put(Integer.toString(i), confInfo);
                }

                if (mListener != null) {
                    try {
                        mListener.callSessionConferenceStateUpdated(ImsCallSessionProxy.this,
                                imsConferenceState);
                    } catch (RemoteException e) {
                        Rlog.e(LOG_TAG, "RemoteException occurs when callSessionConferenceStateUpdated()");
                    }
                }

            } catch (Exception e) {
                Rlog.d(LOG_TAG, "Failed to handle volte conference call message !!!" + e);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DBG) {
                Rlog.d(LOG_TAG, "received broadcast " + action);
            }
            /* Handle IMS conference call xml message */
            if (mCallId != null) {
                if (CallControlDispatcher.ACTION_IMS_CONFERENCE_CALL_INDICATION.equals(action)) {
                    int callId = intent.getIntExtra(CallControlDispatcher.EXTRA_CALL_ID, 3);

                    if (callId == Integer.parseInt(mCallId)) {
                        String data = intent.getStringExtra(
                                CallControlDispatcher.EXTRA_MESSAGE_CONTENT);
                        if ((data != null) && (!data.equals(""))) {
                            handleImsConfCallMessage(data.length(), callId, data);
                        }
                    }
                }
            } else {
                Rlog.e(LOG_TAG, "can't handle conference message since no call ID. Abnormal Case");
            }
        }
    };

    private class MyHandler extends Handler {

        private static final String PAU_NUMBER_FIELD = "<tel:";
        private static final String PAU_NAME_FIELD = "<name:";
        private static final String PAU_SIP_NUMBER_FIELD = "<sip:";
        private static final String PAU_END_FLAG_FIELD = ">";

        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        private String getFieldValueFromPau(String pau, String field) {
            String value = "";
            if (TextUtils.isEmpty(pau) || TextUtils.isEmpty(field)) {
                Rlog.d(LOG_TAG, "getFieldValueFromPau()... pau or field is null !");
                return value;
            }

            if (!pau.contains(field)) {
                Rlog.d(LOG_TAG, "getFieldValueFromPau()... There is no such field in pau !"
                        + " field / pau :" + field + " / " + pau);
                return value;
            }

            int startIndex = pau.indexOf(field);
            startIndex += field.length();
            int endIndex = pau.indexOf(PAU_END_FLAG_FIELD, startIndex);
            value = pau.substring(startIndex, endIndex);
            return value;
        }

        private int sipCauseFromCode(int causeCode) {
            Rlog.d(LOG_TAG, "sipCauseFromCode: causeCode = " + causeCode);

            switch (causeCode) {
                case CallFailCause.USER_BUSY:
                    return ImsReasonInfo.CODE_SIP_BUSY;

                case CallFailCause.TEMPORARY_FAILURE:
                case CallFailCause.CHANNEL_NOT_AVAIL:
                    return ImsReasonInfo.CODE_SIP_TEMPRARILY_UNAVAILABLE;

                case CallFailCause.QOS_NOT_AVAIL:
                    return ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE;

                case CallFailCause.NO_CIRCUIT_AVAIL:
                case CallFailCause.FACILITY_NOT_IMPLEMENT:
                case CallFailCause.PROTOCOL_ERROR_UNSPECIFIED:
                    return ImsReasonInfo.CODE_SIP_SERVER_INTERNAL_ERROR;

                case CallFailCause.ACM_LIMIT_EXCEEDED:
                    return ImsReasonInfo.CODE_LOCAL_CALL_EXCEEDED;

                case CallFailCause.CALL_BARRED:
                case CallFailCause.FDN_BLOCKED:
                    return ImsReasonInfo.CODE_LOCAL_ILLEGAL_STATE;

                case CallFailCause.BEARER_NOT_AVAIL:
                case CallFailCause.INTERWORKING_UNSPECIFIED:
                /* sip 510 not implemented */
                case CallFailCause.FACILITY_REJECTED:
                /* sip 502 bad gateway */
                case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                    return ImsReasonInfo.CODE_SIP_SERVER_ERROR;

                case CallFailCause.NO_USER_RESPONDING:
                    return ImsReasonInfo.CODE_TIMEOUT_NO_ANSWER;

                case CallFailCause.USER_ALERTING_NO_ANSWER:
                    return ImsReasonInfo.CODE_USER_NOANSWER;

                case CallFailCause.CALL_REJECTED:
                    return ImsReasonInfo.CODE_SIP_USER_REJECTED;

                case CallFailCause.NORMAL_UNSPECIFIED:
                    return ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE;

                case CallFailCause.UNOBTAINABLE_NUMBER:
                case CallFailCause.INVALID_NUMBER_FORMAT:
                    return ImsReasonInfo.CODE_SIP_BAD_ADDRESS;

                case CallFailCause.RESOURCE_UNAVAILABLE:
                case CallFailCause.SWITCHING_CONGESTION:
                case CallFailCause.SERVICE_NOT_AVAILABLE:
                case CallFailCause.NETWORK_OUT_OF_ORDER:
                case CallFailCause.INCOMPATIBLE_DESTINATION:
                    return ImsReasonInfo.CODE_SIP_SERVICE_UNAVAILABLE;

                case CallFailCause.BEARER_NOT_AUTHORIZED:
                case CallFailCause.INCOMING_CALL_BARRED_WITHIN_CUG:
                    return ImsReasonInfo.CODE_SIP_FORBIDDEN;

                case CallFailCause.CHANNEL_UNACCEPTABLE:
                case CallFailCause.BEARER_NOT_IMPLEMENT:
                    return ImsReasonInfo.CODE_SIP_NOT_ACCEPTABLE;

                case CallFailCause.NO_ROUTE_TO_DESTINATION:
                    return ImsReasonInfo.CODE_SIP_NOT_FOUND;

                case CallFailCause.OPERATOR_DETERMINED_BARRING:
                    return ImsReasonInfo.CODE_SIP_REQUEST_CANCELLED;

                case CallFailCause.RECOVERY_ON_TIMER_EXPIRY:
                    return ImsReasonInfo.CODE_SIP_REQUEST_TIMEOUT;

                /* SIP 481: call/transaction doesn't exist */
                case CallFailCause.INVALID_TRANSACTION_ID_VALUE:
                    return ImsReasonInfo.CODE_SIP_CLIENT_ERROR;

                /* [VoLTE]Normal call failed, need to dial as ECC */
                case CallFailCause.IMS_EMERGENCY_REREG:
                    return ImsReasonInfo.CODE_SIP_REDIRECTED_EMERGENCY;

                case CallFailCause.ERROR_UNSPECIFIED:
                case CallFailCause.NORMAL_CLEARING:
                default:
                    // WFC: Because +CEER doesn't carry fail cause for WifiCalling, we need to get
                    // fail cause from WifiOffloadService
                    int wfcReason = getWfcDisconnectCause(causeCode);
                    if (wfcReason != WFC_GET_CAUSE_FAILED) {
                        return wfcReason;
                    }

                    int serviceState = mImsService.getImsServiceState();

                    Rlog.d(LOG_TAG, "serviceState = " + serviceState);

                    if (serviceState == ServiceState.STATE_POWER_OFF) {
                        return ImsReasonInfo.CODE_LOCAL_POWER_OFF;
                    } else if (serviceState == ServiceState.STATE_OUT_OF_SERVICE) {
                        return ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN;
                    } else if (causeCode == CallFailCause.NORMAL_CLEARING) {
                        return ImsReasonInfo.CODE_USER_TERMINATED_BY_REMOTE;
                    } else {
                        // If nothing else matches, report unknown call drop reason
                        // to app, not NORMAL call end.
                        return ImsReasonInfo.CODE_UNSPECIFIED;
                    }
                }
        }

        private boolean isCallModeUpdated(int callMode, int videoState) {
            Rlog.d(LOG_TAG, "updateCallMode- callMode:" + callMode + "videoState:" + videoState);
            boolean isChanged = false;
            int oldCallMode = mCallProfile.mCallType;

            if (callMode == IMS_VIDEO_CALL || callMode == IMS_VIDEO_CONF ||
                    callMode == IMS_VIDEO_CONF_PARTS) {
                switch(videoState) {
                    case 0:  //pause
                        mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT_NODIR;
                        break;
                    case 1:  //send only
                        mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT_TX;
                        break;
                    case 2:  // recv only
                        mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT_RX;
                        break;
                    case 3:  // send and recv
                        mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                        break;
                    default:
                        mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                        break;
                }

                if (mCallProfile.mCallType != oldCallMode) {
                    isChanged = true;
                }
            } else if (callMode == IMS_VOICE_CALL || callMode == IMS_VOICE_CONF ||
                    callMode == IMS_VOICE_CONF_PARTS) {
                mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                if (mCallProfile.mCallType != oldCallMode) {
                    isChanged = true;
                }
            }

            return isChanged;
        }

        private void retrieveMergeFail() {

            ImsCallInfo mergeCallInfo = null;
            ImsCallInfo mergedCallInfo = null;
            boolean     isNotifyMergeFail = false;

            Rlog.d(LOG_TAG, "retrieveMergeFail");
            if (mMergeCallId != null && !mMergeCallId.equals("")) {
                mergeCallInfo = mImsRILAdapter.getCallInfo(mMergeCallId);
            }

            if (mMergedCallId != null && !mMergedCallId.equals("")) {
                mergedCallInfo = mImsRILAdapter.getCallInfo(mMergedCallId);
            }

            if (mergeCallInfo != null && mergedCallInfo != null) {
                Rlog.d(LOG_TAG, "retrieveMergeFail- MergeCallInfo: callId=" + mergeCallInfo.mCallId
                        + " call status=" + mergeCallInfo.mState + " MergedCallInfo: callId=" +
                        mergedCallInfo.mCallId + " call status=" + mergedCallInfo.mState);
                if (mergeCallInfo.mState == ImsCallInfo.State.ACTIVE &&
                        mergedCallInfo.mState == ImsCallInfo.State.HOLDING) {
                    //Nothing Change
                    isNotifyMergeFail = true;
                } else if (mergeCallInfo.mState == ImsCallInfo.State.ACTIVE &&
                        mergedCallInfo.mState == ImsCallInfo.State.ACTIVE) {
                    // 2 active call and hold the merged call
                    Rlog.d(LOG_TAG, "retrieveMergeFail- two active call and hold merged call");
                    Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                    mImsRILAdapter.hold(Integer.parseInt(mMergedCallId), result);
                } else if (mergeCallInfo.mState == ImsCallInfo.State.HOLDING &&
                        mergedCallInfo.mState == ImsCallInfo.State.HOLDING) {
                    // 2 hold call and resume merge call
                    Rlog.d(LOG_TAG, "retrieveMergeFail- two hold call and resume merge call");
                    Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                    mImsRILAdapter.resume(Integer.parseInt(mMergeCallId), result);
                } else {
                    /*
                     *Sincemerge call is become hold and merged call is become active,
                     *we need to swap two calls
                     */
                    Rlog.d(LOG_TAG, "retrieveMergeFail- swap two calls");
                    Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                    mImsRILAdapter.swap(result);
                }
            } else if (mergeCallInfo == null || mergedCallInfo == null) {
                //Only one call is exist and maintain the call state to original state
                if (mergeCallInfo != null) {
                    Rlog.d(LOG_TAG, "retrieveMergeFail- only merge call is left");
                    if (mergeCallInfo.mState != ImsCallInfo.State.ACTIVE) {
                        Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                        mImsRILAdapter.resume(Integer.parseInt(mMergeCallId), result);
                    } else {
                        isNotifyMergeFail = true;
                    }
                } else if (mergedCallInfo != null) {
                    Rlog.d(LOG_TAG, "retrieveMergeFail- only merged call is left");
                    if (mergedCallInfo.mState != ImsCallInfo.State.HOLDING) {
                        Message result = mHandler.obtainMessage(EVENT_RETRIEVE_MERGE_FAIL_RESULT);
                        mImsRILAdapter.hold(Integer.parseInt(mMergedCallId), result);
                    } else {
                        isNotifyMergeFail = true;
                    }
                }
            }

            if (isNotifyMergeFail) {
                mergeFailed();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            int callMode = INVALID_CALL_MODE;
            if (DBG) {
                Rlog.d(LOG_TAG, "receive message by ImsCallSessionProxy - CallId:" + mCallId);
            }

            switch (msg.what) {
                case EVENT_CALL_INFO_INDICATION:
                    /* +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>, <dir>, <call_mode>[, <number>, <toa>], "",<cause>
                     *
                     * if msg_type = DISCONNECT_MSG or ALL_CALLS_DISC_MSG,
                     * +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>,,,"",,"",<cause>
                     *
                     * if others,
                     * +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>, <dir>, <call_mode>[, <number>, <toa>], ""
                     *
                     *      0  O  CSMCC_SETUP_MSG
                     *      1  X  CSMCC_DISCONNECT_MSG
                     *      2  O  CSMCC_ALERT_MSG
                     *      3  X  CSMCC_CALL_PROCESS_MSG
                     *      4  X  CSMCC_SYNC_MSG
                     *      5  X  CSMCC_PROGRESS_MSG
                     *      6  O  CSMCC_CALL_CONNECTED_MSG
                     *   129  X  CSMCC_ALL_CALLS_DISC_MSG
                     *   130  O  CSMCC_MO_CALL_ID_ASSIGN_MSG
                     *   131  O  CSMCC_STATE_CHANGE_HELD
                     *   132  O  CSMCC_STATE_CHANGE_ACTIVE
                     *   133  O  CSMCC_STATE_CHANGE_DISCONNECTED
                     *   134  X  CSMCC_STATE_CHANGE_MO_DISCONNECTING
                     */
                    ar = (AsyncResult) msg.obj;
                    String[] callInfo = (String[]) ar.result;
                    int msgType = 0;
                    boolean isCallProfileUpdated = false;

                    if (DBG) Rlog.d(LOG_TAG, "receive EVENT_CALL_INFO_INDICATION");
                    if ((callInfo[1] != null) && (!callInfo[1].equals(""))) {
                        msgType = Integer.parseInt(callInfo[1]);
                    }

                    if ((callInfo[5] != null) && (!callInfo[5].equals(""))) {
                        callMode = Integer.parseInt(callInfo[5]);
                    }

                    if (mIsMerging && (!callInfo[0].equals(mCallId))) {
                        switch (msgType) {
                            case 130:
                                Rlog.d(LOG_TAG, "IMS: +ECPI : conference assign call id");
                                ImsCallProfile imsCallProfile = new ImsCallProfile();
                                if ((callInfo[6] != null) && (!callInfo[6].equals(""))) {
                                    imsCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI,
                                            callInfo[6]);
                                    /*
                                    *we assume the remote uri information is same as telephone
                                    * number and update the remote ui information after getting pau.
                                    */
                                    imsCallProfile.setCallExtra(ImsCallProfile.EXTRA_REMOTE_URI,
                                            callInfo[6]);
                                    imsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                                            ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
                                } else {
                                    imsCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                                            ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
                                }

                                mConfSession = new ImsCallSessionProxy(mContext,imsCallProfile,
                                        null, mImsService, mServiceHandler, mImsRILAdapter, callInfo[0]);
                                try {
                                    mListener.callSessionMergeStarted(ImsCallSessionProxy.this,
                                            mConfSession, mCallProfile);
                                } catch (RemoteException e) {
                                    Rlog.e(LOG_TAG, "RemoteException when session merged started");
                                }
                                break;
                            default:
                                break;
                        }
                    } else if (mCallId != null && mCallId.equals(callInfo[0])) {
                        switch (msgType) {
                            case 0:
                                mState = ImsCallSession.State.ESTABLISHING;
                                Rlog.d(LOG_TAG, "IMS: +ECPI : incoming call");
                                if ((callInfo[5] != null) && (!callInfo[5].equals(""))) {
                                    callMode = Integer.parseInt(callInfo[5]);
                                }

                                if (callMode == IMS_VIDEO_CALL || callMode == IMS_VIDEO_CONF ||
                                        callMode == IMS_VIDEO_CONF_PARTS) {
                                    mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                                } else {
                                    mCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                                }

                                if (callMode == IMS_VOICE_CONF ||
                                        callMode == IMS_VIDEO_CONF ||
                                        callMode == IMS_VOICE_CONF_PARTS ||
                                        callMode == IMS_VIDEO_CONF_PARTS) {
                                    mCallProfile.setCallExtraInt(
                                            ImsCallProfile.EXTRA_INCOMING_MPTY, 1);

                                    // ALPS02136981. Prints debug messages for ImsPhone.
                                    mCallNumber = "conferenceCall";
                                } else {
                                    mCallProfile.setCallExtraInt(
                                            ImsCallProfile.EXTRA_INCOMING_MPTY, 0);

                                    // ALPS02136981. Prints debug messages for ImsPhone.
                                    mCallNumber = callInfo[6];
                                }

                                if ((callInfo[6] != null) && (!callInfo[6].equals(""))) {
                                    mCallProfile.setCallExtra(ImsCallProfile.EXTRA_OI, callInfo[6]);
                                    /*
                                    * we assume the remote uri information is same as telephone
                                    * number and update the remote ui information after getting pau.
                                    */
                                    mCallProfile.setCallExtra(ImsCallProfile.EXTRA_REMOTE_URI,
                                            callInfo[6]);
                                }
                                mCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_OIR,
                                            ImsCallProfile.OIR_PRESENTATION_NOT_RESTRICTED);
                                /* ServiceID may need to refine in the future */
                                int serviceId = 1;

                                Rlog.d(LOG_TAG, "IMS: sendIncomingCallIntent() call_id = "
                                        + mCallId + " dialString = " +  callInfo[6]);

                                //ALPS02136981. Prints debug messages for ImsPhone.
                                logDebugMessagesWithNotifyFormat("CC", "MT", mCallNumber, "");

                                Intent intent = new Intent(ImsManager.ACTION_IMS_INCOMING_CALL);
                                intent.putExtra(ImsManager.EXTRA_CALL_ID, mCallId);
                                intent.putExtra(ImsManager.EXTRA_DIAL_STRING, callInfo[6]);
                                intent.putExtra(ImsManager.EXTRA_SERVICE_ID, serviceId);
                                mContext.sendBroadcast(intent);
                                break;
                            case 2: // CSMCC_ALERT_MSG
                                int isIbt = 1;

                                if (callInfo[2] != null) {
                                    isIbt = Integer.parseInt(callInfo[2]);
                                }

                                // ALPS02136981. Prints debug messages for ImsPhone.
                                if (callMode == IMS_VOICE_CONF ||
                                        callMode == IMS_VIDEO_CONF ||
                                        callMode == IMS_VOICE_CONF_PARTS ||
                                        callMode == IMS_VIDEO_CONF_PARTS) {
                                    mCallNumber = "conferenceCall";
                                }
                                logDebugMessagesWithNotifyFormat(
                                        "CC", "Alerting", mCallNumber, " isIbt=" + isIbt);

                                if (isIbt == 0) {
                                    mCallProfile.mMediaProfile.mAudioDirection =
                                            ImsStreamMediaProfile.DIRECTION_INACTIVE;
                                }

                                updatePau(callInfo[8]);

                                if (mListener != null) {
                                    try {
                                        mListener.callSessionProgressing(ImsCallSessionProxy.this,
                                                mCallProfile.mMediaProfile);
                                    } catch (RemoteException e) {
                                        Rlog.e(LOG_TAG, "RemoteException callSessionProgressing");
                                    }
                                }
                                mHasPendingMo = false;
                                break;
                            case 6: //CSMCC_CALL_CONNECTED_MSG
                                mState = ImsCallSession.State.ESTABLISHED;
                                mCallProfile.mMediaProfile.mAudioDirection =
                                        ImsStreamMediaProfile.DIRECTION_SEND_RECEIVE;

                                updateMultipartyState(callMode);

                                // ALPS02136981. Prints debug messages for ImsPhone.
                                logDebugMessagesWithNotifyFormat("CC", "Active", mCallNumber, "");

                                updatePau(callInfo[8]);

                                if (mListener != null) {
                                    try {
                                        /* There may not has alerting message while dial
                                         * conference call. We need to reset mHasPendingMO.
                                         */
                                        if (mHasPendingMo) {
                                            mListener.callSessionProgressing(
                                                    ImsCallSessionProxy.this,
                                                    mCallProfile.mMediaProfile);
                                        }
                                        mListener.callSessionStarted(ImsCallSessionProxy.this,
                                                mCallProfile);
                                    } catch (RemoteException e) {
                                        Rlog.e(LOG_TAG, "RemoteException callSessionStarted()");
                                    }
                                }
                                mHasPendingMo = false;
                                break;
                            case 131: //CSMCC_STATE_CHANGE_HELD
                                // ALPS02136981. Prints debug messages for ImsPhone.
                                logDebugMessagesWithNotifyFormat("CC", "Onhold", mCallNumber, "");

                                updatePau(callInfo[8]);

                                if (mListener != null) {
                                    if (mIsHideHoldEventDuringMerging == false) {
                                        try {
                                            mListener.callSessionHeld(ImsCallSessionProxy.this,
                                                    mCallProfile);
                                        } catch (RemoteException e) {
                                            Rlog.e(LOG_TAG, "RemoteException callSessionHeld");
                                        }
                                    } else {
                                        try {
                                            mListener.callSessionPauInfoChanged(
                                                    ImsCallSessionProxy.this, mCallProfile);
                                        } catch (RemoteException e) {
                                            Rlog.e(LOG_TAG,
                                                "RemoteException callSessionPauInfoChanged");
                                        }
                                    }
                                }
                                break;
                            case 132: //CSMCC_STATE_CHANGE_ACTIVE
                                updatePau(callInfo[8]);

                                if (mListener != null) {
                                    if (mState == ImsCallSession.State.ESTABLISHED) {
                                        try {
                                            // ALPS02136981. Prints debug messages for ImsPhone.
                                            logDebugMessagesWithNotifyFormat("CC", "Active", mCallNumber, "");

                                            mListener.callSessionResumed(
                                                    ImsCallSessionProxy.this, mCallProfile);
                                        } catch (RemoteException e) {
                                            Rlog.e(LOG_TAG, "RemoteException SessionResumed");
                                        }
                                    } else {
                                        try {
                                            mListener.callSessionPauInfoChanged(
                                                    ImsCallSessionProxy.this, mCallProfile);
                                        } catch (RemoteException e) {
                                            Rlog.e(LOG_TAG,
                                                "RemoteException callSessionPauInfoChanged");
                                        }
                                    }
                                }
                                break;
                            case 133: //CSMCC_STATE_CHANGE_DISCONNECTED
                                // ALPS02136981. Prints debug messages for ImsPhone.
                                logDebugMessagesWithNotifyFormat("CC", "Disconnected", mCallNumber, "");

                                mIsOnTerminated = true;
                                mState = ImsCallSession.State.TERMINATED;
                                if (mHasPendingMo) {
                                    mHasPendingMo = false;
                                    mCallErrorState = CallErrorState.DIAL;
                                } else {
                                    mCallErrorState = CallErrorState.DISCONNECT;
                                }
                                Message result = mHandler.obtainMessage(
                                        EVENT_GET_LAST_CALL_FAIL_CAUSE);
                                mImsRILAdapter.getLastCallFailCause(result);
                                break;
                            default:
                                break;
                        }
                    } else if (mCallId == null && msgType == 130) {
                        Rlog.d(LOG_TAG, "IMS: receive 130 URC, call_id = " + callInfo[0]);
                        mState = ImsCallSession.State.ESTABLISHING;
                        mCallId = callInfo[0];
                        if (mVTProvider != null) {
                            mVTProvider.setId(Integer.parseInt(mCallId));
                        }
                    }
                    break;
                case EVENT_RINGBACK_TONE:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_RINGBACK_TONE");
                    }
                    break;
                case EVENT_ECONF_RESULT_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    handleEconfIndication((String[]) ar.result);
                    break;
                case EVENT_DIAL_RESULT:
                case EVENT_DIAL_CONFERENCE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive DIAL_RESULT or DIAL_CONFERENCE_RESULT");
                    }
                    if (ar.exception != null) {
                        if (DBG) {
                            Rlog.d(LOG_TAG, "dial call failed!!");
                        }
                        if (mListener != null) {

                            Message result = mHandler.obtainMessage(
                                    EVENT_GET_LAST_CALL_FAIL_CAUSE);
                            mCallErrorState = CallErrorState.DIAL;
                            mImsRILAdapter.getLastCallFailCause(result);
                            mHasPendingMo = false;
                        }
                    }
                    break;
                case EVENT_HOLD_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_HOLD_RESULT");
                    }
                    if (mListener != null) {
                        if (ar.exception != null) {
                            if (DBG) {
                                Rlog.d(LOG_TAG, "hold call failed!!");
                            }
                            try {
                                ImsReasonInfo imsReasonInfo = null;
                                if ((ar.exception instanceof CommandException)
                                        && (((CommandException) (ar.exception)).getCommandError()
                                            == Error.CC_CALL_HOLD_FAILED_CAUSED_BY_TERMINATED)) {
                                    imsReasonInfo = new ImsReasonInfo(
                                            ImsReasonInfo.CODE_LOCAL_CALL_TERMINATED, 0);
                                } else {
                                    imsReasonInfo = new ImsReasonInfo();
                                }
                                mListener.callSessionHoldFailed(ImsCallSessionProxy.this,
                                        imsReasonInfo);
                            } catch (RemoteException e) {
                                Rlog.e(LOG_TAG, "RemoteException callSessionHoldFailed()");
                            }
                        } else {
                            if (DBG) {
                                Rlog.d(LOG_TAG, "hold call successed!!");
                            }
                        }
                    }
                    break;
                case EVENT_RESUME_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_RESUME_RESULT");
                    }
                    if (mListener != null) {
                        if (ar.exception != null) {
                            if (DBG) {
                                Rlog.d(LOG_TAG, "resume call failed!!");
                            }
                            try {
                                mListener.callSessionResumeFailed(ImsCallSessionProxy.this,
                                        new ImsReasonInfo());
                            } catch (RemoteException e) {
                                Rlog.e(LOG_TAG, "RemoteException callSessionResumeFailed()");
                            }
                        } else {
                            if (DBG) {
                                Rlog.d(LOG_TAG, "resume call successed");
                            }
                        }
                    }
                    break;
                case EVENT_MERGE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_MERGE_RESULT");
                    }
                    if (mListener != null) {
                        if (ar.exception != null) {
                            if (DBG) {
                                Rlog.d(LOG_TAG, "merge call failed!!");
                            }
                            // ALPS02136981. Prints debug messages for ImsPhone.
                            logDebugMessagesWithNotifyFormat(
                                    "CC", "ConfCreated", "conferenceCall", " failed");

                            retrieveMergeFail();
                        }
                    }
                    break;
                case EVENT_RETRIEVE_MERGE_FAIL_RESULT:

                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_RETRIEVE_MERGE_FAIL_RESULT");
                    }
                    // Don't care the retrieve result and just notify merge fail to ImsPhone.
                    mergeFailed();
                    break;
                case EVENT_ADD_CONFERENCE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_ADD_CONFERENCE_RESULT");
                    }

                    if (mIsMerging) {
                        if (ar.exception != null) {
                            retrieveMergeFail();
                        } else {
                            /*
                             * We only know the merge command is accepted by server for now.
                             * The merge result will be notified by receiving +ECONF URC.
                             */
                        }
                    } else {
                        if (mIsOnTerminated == true) {
                            break;
                        }

                        if (ar.exception == null) {
                            mIsAddRemoveParticipantsCommandOK = true;
                        }
                        mPendingParticipantInfoIndex ++;

                        if (mPendingParticipantInfoIndex < mPendingParticipantStatistics) {
                            Message result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);

                            mImsRILAdapter.inviteParticipants(Integer.parseInt(mCallId),
                                    mPendingParticipantInfo[mPendingParticipantInfoIndex], result);

                        } else {
                            if (mListener != null) {
                                if (mIsAddRemoveParticipantsCommandOK == false) {
                                    try {
                                        mListener.callSessionInviteParticipantsRequestFailed(
                                                ImsCallSessionProxy.this, new ImsReasonInfo());
                                    } catch (RemoteException e) {
                                        Rlog.e(LOG_TAG, "RemoteException InviteFailed()");
                                    }
                                } else {
                                    try {
                                        mListener.callSessionInviteParticipantsRequestDelivered(
                                                ImsCallSessionProxy.this);
                                    } catch (RemoteException e) {
                                        Rlog.e(LOG_TAG, "RemoteException InviteDelivered()");
                                    }
                                }
                            }
                            mIsAddRemoveParticipantsCommandOK = false;
                        }
                    }
                    break;
                case EVENT_REMOVE_CONFERENCE_RESULT:
                    ar = (AsyncResult) msg.obj;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_REMOVE_CONFERENCE_RESULT");
                    }

                    if (mIsOnTerminated == true) {
                        break;
                    }

                    if (ar.exception == null) {
                        mIsAddRemoveParticipantsCommandOK = true;
                    }

                    mPendingParticipantInfoIndex ++;
                    if (mPendingParticipantInfoIndex < mPendingParticipantStatistics) {
                        Message result = mHandler.obtainMessage(EVENT_ADD_CONFERENCE_RESULT);

                        mImsRILAdapter.removeParticipants(Integer.parseInt(mCallId),
                                mPendingParticipantInfo[mPendingParticipantInfoIndex], result);
                    } else {
                        if (mListener != null) {
                            if (mIsAddRemoveParticipantsCommandOK == false) {
                                try {
                                    mListener.callSessionRemoveParticipantsRequestFailed(
                                            ImsCallSessionProxy.this, new ImsReasonInfo());
                                } catch (RemoteException e) {
                                    Rlog.e(LOG_TAG, "RemoteException RemoveFailed()");
                                }
                            } else {
                                try {
                                    mListener.callSessionRemoveParticipantsRequestDelivered(
                                            ImsCallSessionProxy.this);
                                } catch (RemoteException e) {
                                    Rlog.e(LOG_TAG, "RemoteException RemoveDelivered()");
                                }
                            }
                        }
                        mIsAddRemoveParticipantsCommandOK = false;
                    }
                    break;
                case EVENT_GET_LAST_CALL_FAIL_CAUSE:
                    ar = (AsyncResult) msg.obj;
                    ImsReasonInfo imsReasonInfo;
                    int sipCauseCode = ImsReasonInfo.CODE_UNSPECIFIED;
                    if (DBG) {
                        Rlog.d(LOG_TAG, "receive EVENT_GET_LAST_CALL_FAIL_CAUSE");
                    }

                    if (ar.exception != null) {
                        imsReasonInfo = new ImsReasonInfo();
                    } else {
                        LastCallFailCause failCause = (LastCallFailCause) ar.result;
                        sipCauseCode = sipCauseFromCode(failCause.causeCode);
                        imsReasonInfo = new ImsReasonInfo(sipCauseCode, 0);
                    }

                    switch (mCallErrorState) {
                        case DIAL :
                            if (mListener != null) {
                                try {
                                    mListener.callSessionStartFailed(ImsCallSessionProxy.this,
                                            imsReasonInfo);
                                } catch (RemoteException e) {
                                    Rlog.e(LOG_TAG, "RemoteException callSessionStartFailed()");
                                }
                            }
                            break;
                        case DISCONNECT :
                            if (mListener != null) {
                                try {
                                    mListener.callSessionTerminated(ImsCallSessionProxy.this,
                                            imsReasonInfo);
                                } catch (RemoteException e) {
                                    Rlog.e(LOG_TAG, "RemoteException callSessionTerminated()");
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case EVENT_SIP_CODE_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    int[] sipMessage = (int[]) ar.result;
                    if (DBG) {
                        /* ESIPCPI: <call_id>,<dir>,<SIP_msg_type>,<method>,<response_code> */
                        if (mCallId != null) {
                            if (sipMessage != null && sipMessage[0] == Integer.parseInt(mCallId)) {
                                Rlog.d(LOG_TAG, "receive sip cause =" + sipMessage[4]);
                            }
                        }
                    }
                    break;
                case EVENT_CALL_MODE_CHANGE_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    String[] callModeInfo = (String[]) ar.result;

                    /* +EIMSCMODE: <call id>,<call mode>,<video state>,<audio direction>,<PAU> */
                    if (callModeInfo != null && callModeInfo[0].equals(mCallId)) {
                        int videoState = 2; // assum video state: send_recv is default value
                        if ((callModeInfo[1] != null) && (!callModeInfo[1].equals(""))) {
                            callMode = Integer.parseInt(callModeInfo[1]);
                        }
                        if ((callModeInfo[2] != null) && (!callModeInfo[2].equals(""))) {
                            videoState = Integer.parseInt(callModeInfo[2]);
                        }

                        if (DBG) {
                            Rlog.d(LOG_TAG, "receive EVENT_CALL_MODE_CHANGE_INDICATION mode=" +
                                    callMode + "video state:" + videoState);
                        }
                        if (isCallModeUpdated(callMode, videoState)) {
                            if (mListener != null) {
                                try {
                                    mListener.callSessionUpdated(ImsCallSessionProxy.this,
                                            mCallProfile);
                                } catch (RemoteException e) {
                                    Rlog.e(LOG_TAG, "RemoteException callSessionUpdated()");
                                }
                            }
                        }
                        notifyMultipartyStateChanged(callMode);
                        if (callModeInfo.length >= 5) {
                            notifyPauInfoChanged(callModeInfo[4]);
                        }
                    }
                    break;
                case EVENT_VIDEO_CAPABILITY_INDICATION:
                    ar = (AsyncResult) msg.obj;
                    String[] videoCapabilityInfo = (String[]) ar.result;
                    if (DBG) {
                        //+EIMSVCAP: <call ID>, <local video capability>, <remote video capability>
                        boolean  lVideoCapability = false;
                        boolean  rVideoCapability = false;
                        if (videoCapabilityInfo != null &&
                                videoCapabilityInfo[0].equals(mCallId)) {
                            if ((videoCapabilityInfo[1] != null) &&
                                    (!videoCapabilityInfo[1].equals(""))) {
                                lVideoCapability = Boolean.parseBoolean(videoCapabilityInfo[1]);
                                if (lVideoCapability) {
                                    mLocalCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                                } else {
                                    mLocalCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                                }
                            }
                            if ((videoCapabilityInfo[2] != null) &&
                                    (!videoCapabilityInfo[2].equals(""))) {
                                rVideoCapability = Boolean.parseBoolean(videoCapabilityInfo[2]);
                                if (rVideoCapability) {
                                    mRemoteCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VT;
                                } else {
                                    mRemoteCallProfile.mCallType = ImsCallProfile.CALL_TYPE_VOICE;
                                }
                            }

                            Rlog.d(LOG_TAG, "receive EVENT_VIDEO_CAPABILITY_INDICATION local " +
                                    "video capability:" + lVideoCapability +
                                    " remote video capability:" + rVideoCapability);
                        }
                    }
                    break;
                case EVENT_DTMF_DONE:
                    // Send message to original target handler.
                    if (mDtmfTarget != null && mDtmfMsg != null) {
                        try {
                            mDtmfTarget.send(mDtmfMsg);
                        } catch (RemoteException e) {
                            Rlog.e(LOG_TAG, "RemoteException handleMessge() for DTMF");
                        }
                    }
                    mDtmfTarget = null;
                    mDtmfMsg = null;
                    break;
                default:
                    break;
            }
        }

        private void handleEconfIndication(String[] result) {
            // +ECONF:<conf_call_id>,<op>,<num>,<result>,<cause>[,<joined_call_id>]
            if (DBG) {
                Rlog.d(LOG_TAG, "receive EVENT_ECONF_RESULT_INDICATION mCallId:" + mCallId
                        + ", conf_call_id:" + result[0] + "joined_call_id:" + result[5]);
            }

            // Prevent timing issue in ImsCall.processMergeComplete(), it will check if the
            // session is still alive, by marking this session "terminating"
            // TODO: check which parameter means original call id
            if (mCallId != null && mCallId.equals(result[5]) && result[3].equals("0")) {
                mState = ImsCallSession.State.TERMINATING;
            }

            if (mIsMerging != true) {
                return;
            }

            if (mNormalCallsMerge) {
                // normal call merge normal call
                mEconfCount++;
                if (result[3].equals("0")) {
                    mThreeWayMergeSucceeded = true;
                }
                if (mEconfCount == 2 && mThreeWayMergeSucceeded) {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "3 way conference merge succeeded");
                    }
                    // ALPS02136981. Prints debug messages for ImsPhone.
                    logDebugMessagesWithNotifyFormat(
                            "CC", "ConfCreated", "conferenceCall", " successed");

                    mergeCompleted();
                    mNormalCallsMerge = false;
                } else if (mEconfCount == 2 && mThreeWayMergeSucceeded != true) {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "3 way conference merge failed!!");
                    }
                    // ALPS02136981. Prints debug messages for ImsPhone.
                    logDebugMessagesWithNotifyFormat(
                            "CC", "ConfCreated", "conferenceCall", " failed");

                    retrieveMergeFail();
                    /// ALPS02383993: Terminate the conference if merge failed @{
                    int confCallId = Integer.parseInt(result[0]);
                    mImsRILAdapter.terminate(confCallId);
                    /// @}
                    mNormalCallsMerge = false;
                }
            } else {
                // conference call merge normal call
                if (result[3].equals("0")) {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "conference call merge normal call successed");
                    }
                    // ALPS02136981. Prints debug messages for ImsPhone.
                    logDebugMessagesWithNotifyFormat(
                            "CC", "ConfCreated", "conferenceCall", " successed");

                    mergeCompleted();
                } else {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "conference call merge normal call failed!!");
                    }
                    // ALPS02136981. Prints debug messages for ImsPhone.
                    logDebugMessagesWithNotifyFormat(
                            "CC", "ConfCreated", "conferenceCall", " failed");

                    retrieveMergeFail();
                }
            }
        }

        private boolean updateMultipartyState(int callMode) {
            boolean isMultipartyMode = (callMode == IMS_VOICE_CONF || callMode == IMS_VIDEO_CONF
                    || callMode == IMS_VOICE_CONF_PARTS || callMode == IMS_VIDEO_CONF_PARTS);
            if (isMultiparty() == isMultipartyMode) {
                return false;
            } else if (isMultipartyMode == true) {
                mCallProfile.setCallExtraInt(ImsCallProfile.EXTRA_MPTY, 1);
                mCallNumber = "conferenceCall";
            } else {
                mCallProfile.setCallExtraInt(
                        ImsCallProfile.EXTRA_MPTY, 0);
            }
            return true;
        }

        private void notifyMultipartyStateChanged(int callMode) {
            boolean stateChanged = updateMultipartyState(callMode);
            if (stateChanged == false) {
                return;
            }
            if (DBG) {
                Rlog.d(LOG_TAG, "notifyMultipartyStateChanged isMultiparty(): " + isMultiparty());
            }
            if (mListener != null) {
                try {
                    mListener.callSessionMultipartyStateChanged(ImsCallSessionProxy.this,
                            isMultiparty());
                } catch (RemoteException e) {
                    Rlog.e(LOG_TAG, "RemoteException callSessionMultipartyStateChanged()");
                }
            }
        }

        private boolean updatePau(String pau) {
            if (pau == null || pau.equals("")) {
                return false;
            }
            String sipNumber = getFieldValueFromPau(pau, PAU_SIP_NUMBER_FIELD);
            Rlog.d(LOG_TAG, "updatePau()... sipNumber: " + sipNumber);
            if (!sipNumber.equals(mCallProfile.getCallExtra(ImsCallProfile.EXTRA_REMOTE_URI))) {
                mCallProfile.setCallExtra(ImsCallProfile.EXTRA_REMOTE_URI, sipNumber);
            }
            if (pau.equals(mCallProfile.getCallExtra(ImsCallProfile.EXTRA_PAU))) {
                return false;
            }
            mCallProfile.setCallExtra(ImsCallProfile.EXTRA_PAU, pau);
            return true;
        }

        private void notifyPauInfoChanged(String pau) {
            if (updatePau(pau) == false) {
                return;
            }

            if (mListener == null) {
                return;
            }
            try {
                mListener.callSessionPauInfoChanged(ImsCallSessionProxy.this, mCallProfile);
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG,
                        "RemoteException callSessionPauInfoChanged");
            }
        }

        private int getWfcDisconnectCause(int causeCode) {
            Rlog.d(LOG_TAG, "[WFC] getWfcDisconnectCause mRatType = " + mRatType);
            if (mWfoService == null || mRatType != WifiOffloadManager.RAN_TYPE_WIFI
                    || causeCode == CallFailCause.NORMAL_CLEARING) {
                return WFC_GET_CAUSE_FAILED;
            }

            DisconnectCause disconnectCause = null;
            try {
                disconnectCause = mWfoService.getDisconnectCause();
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "RemoteException in getWfcDisconnectCause()");
            }
            if (disconnectCause == null) {
                return WFC_GET_CAUSE_FAILED;
            }
            int wfcErrorCause = disconnectCause.getErrorCause();
            Rlog.d(LOG_TAG, "[WFC] wfcErrorCause = " + wfcErrorCause);
            if (wfcErrorCause == WfcReasonInfo.CODE_WFC_WIFI_SIGNAL_LOST) {
                return ImsReasonInfo.CODE_SIP_WIFI_SIGNAL_LOST;
            } else if ((wfcErrorCause == WfcReasonInfo.CODE_WFC_UNABLE_TO_COMPLETE_CALL)
                    || (wfcErrorCause == WfcReasonInfo.CODE_WFC_UNABLE_TO_COMPLETE_CALL_CD)) {
                return ImsReasonInfo.CODE_SIP_HANDOVER_WIFI_FAIL;
            } else if (wfcErrorCause ==
                    WfcReasonInfo.CODE_WFC_NO_AVAILABLE_QUALIFIED_MOBILE_NETWORK) {
                return ImsReasonInfo.CODE_SIP_HANDOVER_LTE_FAIL;
            } else {
                return WFC_GET_CAUSE_FAILED;
            }
        }
    }

    private void mergeCompleted() {
        if (mListener != null) {
            try {
                mListener.callSessionMergeComplete(mConfSession);
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "RemoteException callSessionMerged()");
            }
        }
        mIsMerging = false;
        mIsHideHoldEventDuringMerging = false;
    }

    private void mergeFailed() {
        if (mListener != null) {
            try {
                mListener.callSessionMergeFailed(ImsCallSessionProxy.this,
                        new ImsReasonInfo());
            } catch (RemoteException e) {
                Rlog.e(LOG_TAG, "RemoteException callSessionMergeFailed()");
            }
        }
       mMergeCallId = "";
       mMergeCallStatus = ImsCallInfo.State.INVALID;
       mMergedCallId = "";
       mMergedCallStatus = ImsCallInfo.State.INVALID;

       mIsMerging = false;
       mIsHideHoldEventDuringMerging = false;
       if (mConfSession instanceof ImsCallSessionProxy) {
           ((ImsCallSessionProxy) mConfSession).close();
       }
    }

    /**
     * Logs unified debug log messages, for "Notify".
     * Format: [category][Module][Notify][Action][call-number][local-call-ID] Msg. String.
     * P.S. uses the RIL call ID as the local call ID.
     *
     * @param category currently we only have 'CC' category.
     * @param action the action name. (e.q. Active, MT, Onhold, etc.)
     * @param isConf is conference
     * @param msg the optional messages
     * @hide
     */
    void logDebugMessagesWithNotifyFormat(
            String category, String action, String callNumber, String msg) {
        if (category == null || action == null) {
            // return if no mandatory tags.
            return;
        }

        FormattedLog formattedLog = new FormattedLog.Builder()
                .setCategory(category)
                .setServiceName("ImsPhone")
                .setOpType(FormattedLog.OpType.NOTIFY)
                .setActionName(action)
                .setCallNumber(callNumber)
                .setCallId(getCallId())
                .setExtraMessage(msg)
                .buildDebugMsg();

        if (formattedLog != null) {
            Rlog.d(LOG_TAG, formattedLog.toString());
        }
    }

    /**
     * A listener type for receiving notification on WFC handover events.
     */
    private class IWifiOffloadListenerProxy extends IWifiOffloadListener.Stub {
        @Override
        public void onHandover(int stage, int ratType) throws RemoteException {
            if (ratType == mRatType || stage == WifiOffloadManager.HANDOVER_START) {
                return;
            }
            if (mListener != null) {
                try {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "onHandover");
                    }
                    mListener.callSessionHandover(ImsCallSessionProxy.this, mRatType, ratType,
                            new ImsReasonInfo());
                } catch (RemoteException e) {
                    Rlog.e(LOG_TAG, "RemoteException onHandover()");
                }
            }
            mRatType = ratType;
        }
    }

    /**
     * Get the user name (i.e. phone number or Sip account) from a Sip/Tel Uri string.
     * Reference RFC 3966.
     *
     * @param uriString the string of the Sip/Tel URI.
     * @return the string of user name.
     * @hide
     * @internal
     */
    private String getUserNameFromSipTelUriString(String uriString) {
        if (uriString == null) {
            return null;
        }

        Uri uri = Uri.parse(uriString);

        // Gets the address part, i.e. everything between 'sip:' and the fragment separator '#'.
        // ex: '+8618407404132@10.185.184.137:5087;transport=UDP'
        // or '1234;phone-context=munich.example.com;isub=@1134'
        String address = uri.getSchemeSpecificPart();
        if (address == null) {
            return null;
        }

        // Gets user name, i.e. everything before '@'.
        // ex: '+8618407404132' or '1234;phone-context=munich.example.com;isub='
        String userName = PhoneNumberUtils.getUsernameFromUriNumber(address);
        if (userName == null) {
            return null;
        }

        // Gets pure user name part, i.e. everything before ';' or ','.
        // ex: '+8618407404132' or '1234'
        int pIndex = userName.indexOf(';');    //WAIT
        int wIndex = userName.indexOf(',');    //PAUSE

        if (pIndex >= 0 && wIndex >= 0) {
            return userName.substring(0, Math.min(pIndex, wIndex) + 1);
        } else if (pIndex >= 0) {
            return userName.substring(0, pIndex + 1);
        } else if (wIndex >= 0) {
            return userName.substring(0, wIndex + 1);
        } else {
            return userName;
        }
    }
}
