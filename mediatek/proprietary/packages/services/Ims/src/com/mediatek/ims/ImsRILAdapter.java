/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2014. All rights reserved.
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

import static com.android.internal.telephony.RILConstants.*;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.util.SparseArray;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.LastCallFailCause;
import com.android.internal.telephony.UUSInfo;

/// M: CC053: MoMS [Mobile Managerment] @{
import android.os.IBinder;
import android.os.Binder;
import android.content.pm.PackageManager;
import android.os.ServiceManager;
import com.mediatek.common.mom.IMobileManagerService;
import com.mediatek.common.mom.MobileManagerUtils;
import com.mediatek.common.mom.SubPermissions;
/// @}

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
/// M: CC012: DTMF request special handling @{
import java.util.Vector;
/// @}

/**
 * {@hide}
 */
class RILRequest {
    static final String LOG_TAG = "IMSRILRequest";

    //***** Class Variables
    static Random sRandom = new Random();
    static AtomicInteger sNextSerial = new AtomicInteger(0);
    private static Object sPoolSync = new Object();
    private static RILRequest sPool = null;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 4;
    private Context mContext;

    //***** Instance Variables
    int mSerial;
    int mRequest;
    Message mResult;
    Parcel mParcel;
    RILRequest mNext;

    /**
     * Retrieves a new RILRequest instance from the pool.
     *
     * @param request RIL_REQUEST_*
     * @param result sent when operation completes
     * @return a RILRequest instance from the pool.
     */
    static RILRequest obtain(int request, Message result) {
        RILRequest rr = null;

        synchronized(sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }

        if (rr == null) {
            rr = new RILRequest();
        }

        rr.mSerial = sNextSerial.getAndIncrement();

        rr.mRequest = request;
        rr.mResult = result;
        rr.mParcel = Parcel.obtain();

        if (result != null && result.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }

        // first elements in any RIL Parcel
        rr.mParcel.writeInt(request);
        rr.mParcel.writeInt(rr.mSerial);

        return rr;
    }

    /**
     * Returns a RILRequest instance to the pool.
     *
     * Note: This should only be called once per use.
     */
    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                mNext = sPool;
                sPool = this;
                sPoolSize++;
                mResult = null;
            }
        }
    }

    private RILRequest() {
    }

    static void
    resetSerial() {
        // use a random so that on recovery we probably don't mix old requests
        // with new.
        sNextSerial.set(sRandom.nextInt());
    }

    String
    serialString() {
        //Cheesy way to do %04d
        StringBuilder sb = new StringBuilder(8);
        String sn;

        long adjustedSerial = (((long)mSerial) - Integer.MIN_VALUE)%10000;

        sn = Long.toString(adjustedSerial);

        //sb.append("J[");
        sb.append('[');
        for (int i = 0, s = sn.length() ; i < 4 - s; i++) {
            sb.append('0');
        }

        sb.append(sn);
        sb.append(']');
        return sb.toString();
    }

    void
    onError(int error, Object ret) {
        CommandException ex;

        ex = CommandException.fromRilErrno(error);

        if (ImsRILAdapter.IMS_RILA_LOGD) Rlog.d(LOG_TAG, serialString() + "< "
            + ImsRILAdapter.requestToString(mRequest)
            + " error: " + ex + " ret=" + ImsRILAdapter.retToString(mRequest, ret));

        if (mResult != null) {
            AsyncResult.forMessage(mResult, ret, ex);
            mResult.sendToTarget();
        }

        if (mParcel != null) {
            mParcel.recycle();
            mParcel = null;
        }
    }
}

/**
 * IMS RIL dial normal MO call information.
 *
 * {@hide}
 */
class MoCallInfo {
    String mCallee;
    int mClirMode;
    boolean mIsEmergency;
    boolean mIsVideoCall;
    Message mResult;

    //***** Constructors
    public MoCallInfo(String callee, int clirMode, boolean isEmergency,
            boolean isVideoCall, Message result) {
        mCallee = callee;
        mClirMode = clirMode;
        mIsEmergency = isEmergency;
        mIsVideoCall = isVideoCall;
        mResult = result;
    }
}

/**
 * IMS RIL dial conference call information.
 *
 * {@hide}
 */
class ConferenceCallDialInfo {
    String[] mParticipants;
    int mClirMode;
    boolean mIsVideoCall;
    Message mResult;

    //***** Constructors
    public ConferenceCallDialInfo(String[] participants, int clirMode,
            boolean isVideoCall, Message result) {
        mParticipants = participants;
        mClirMode = clirMode;
        mIsVideoCall = isVideoCall;
        mResult = result;
    }
}

/**
 * IMS RIL call information.
 *
 * {@hide}
 */
class ImsCallInfo {
    /* State is indicated for call status */
    enum State {
        ACTIVE,
        HOLDING,
        ALERTING,   // MO call only
        INCOMING,   // MT call only
        INVALID;
    }

    String mCallNum;
    String mCallId;
    boolean mIsConference;
    State mState;

    //***** Constructors
    public ImsCallInfo(String callId, String callNum, boolean isConference, State state) {
        mCallId = callId;
        mCallNum = callNum;
        mIsConference = isConference;
        mState = state;
    }
}

/**
 * IMS RIL Adapter implementation.
 *
 * {@hide}
 */
public class ImsRILAdapter extends ImsBaseCommands implements ImsCommandsInterface {
    static final String IMS_RILA_LOG_TAG = "IMS_RILA";

    //***** Instance Variables
    Context mContext;
    LocalSocket mSocket;
    HandlerThread mSenderThread;
    ImsRILSender mSender;
    Thread mReceiverThread;
    ImsRILReceiver mReceiver;
    WakeLock mWakeLock;
    final int mWakeLockTimeout;
    // The number of wakelock requests currently active.  Don't release the lock
    // until dec'd to 0
    int mWakeLockCount;

    SparseArray<RILRequest> mRequestList = new SparseArray<RILRequest>();

    /**
     * Property to override DEFAULT_WAKE_LOCK_TIMEOUT
     */
    static final String PROPERTY_WAKE_LOCK_TIMEOUT = "ro.ril.wake_lock_timeout";
    /**
     * Wake lock timeout should be longer than the longest timeout in
     * the vendor ril.
     */
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 60000;

    //***** Events
    static final int EVENT_SEND                 = 1;
    static final int EVENT_WAKE_LOCK_TIMEOUT    = 2;

    // match with constant in ril_ims.c
    static final int RIL_MAX_COMMAND_BYTES = (8 * 1024);
    static final int RESPONSE_SOLICITED = 0;
    static final int RESPONSE_UNSOLICITED = 1;
    static final int SOCKET_OPEN_RETRY_MILLIS = 4 * 1000;
    static final boolean IMS_RILA_LOGD = true;
    static final int MAX_CONNECTIONS = 7;

    private static final int INVALID_CALL_MODE = 0xFF;
    private static final int IMS_VOICE_CALL = 20;
    private static final int IMS_VIDEO_CALL = 21;
    private static final int IMS_VOICE_CONF = 22;
    private static final int IMS_VIDEO_CONF = 23;
    private static final int IMS_VOICE_CONF_PARTS = 24;
    private static final int IMS_VIDEO_CONF_PARTS = 25;

    private static final int  MAX_BYTE_COUNT = 256;

    private MoCallInfo mMoCall = null;
    private ConferenceCallDialInfo mConferenceCallDialInfo = null;

    // Adapter Defined Event
    private static final int EVENT_AT_CMD_DONE = 100;

    private HashMap<String, ImsCallInfo> mCallConnections = new HashMap<String, ImsCallInfo>();

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_AT_CMD_DONE:
                    Rlog.i(IMS_RILA_LOG_TAG, "IMS: Adapter receive EVENT_AT_CMD_DONE");
                    ar = (AsyncResult) msg.obj;
                    handleAtCmdResponseAndDial(ar);
                    break;
                default:
                    break;
            }
        }
    };

    /// M: CC009: DTMF request special handling @{
    /* DTMF request will be ignored when duplicated sending */
    private class dtmfQueueHandler {

        public dtmfQueueHandler() {
            mDtmfStatus = DTMF_STATUS_STOP;
        }

        public void start() {
            mDtmfStatus = DTMF_STATUS_START;
        }

        public void stop() {
            mDtmfStatus = DTMF_STATUS_STOP;
        }

        public boolean isStart() {
            return (mDtmfStatus == DTMF_STATUS_START);
        }

        public void add(RILRequest o) {
            mDtmfQueue.addElement(o);
        }

        public void remove(RILRequest o) {
            mDtmfQueue.remove(o);
        }

        public void remove(int idx) {
            mDtmfQueue.removeElementAt(idx);
        }

        public RILRequest get() {
            return (RILRequest) mDtmfQueue.get(0);
        }

        public int size() {
            return mDtmfQueue.size();
        }

        public void setPendingRequest(RILRequest r) {
            mPendingCHLDRequest = r;
        }

        public RILRequest getPendingRequest() {
            return mPendingCHLDRequest;
        }

        public void setSendChldRequest() {
            mIsSendChldRequest = true;
        }

        public void resetSendChldRequest() {
            mIsSendChldRequest = false;
        }

        public boolean hasSendChldRequest() {
            riljLog("mIsSendChldRequest = " + mIsSendChldRequest);
            return mIsSendChldRequest;
        }

        public final int MAXIMUM_DTMF_REQUEST = 32;
        private final boolean DTMF_STATUS_START = true;
        private final boolean DTMF_STATUS_STOP = false;

        private boolean mDtmfStatus = DTMF_STATUS_STOP;
        private Vector mDtmfQueue = new Vector(MAXIMUM_DTMF_REQUEST);

        private RILRequest mPendingCHLDRequest = null;
        private boolean mIsSendChldRequest = false;
    }

    private dtmfQueueHandler mDtmfReqQueue = new dtmfQueueHandler();
    /// @}

    //***** Constructors
    public ImsRILAdapter(Context context) {
        super(context);
        mContext = context;
        Rlog.i(IMS_RILA_LOG_TAG, "IMS:ImsRILAdapter constructor");

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, IMS_RILA_LOG_TAG);
        mWakeLock.setReferenceCounted(false);
        mWakeLockTimeout = SystemProperties.getInt(PROPERTY_WAKE_LOCK_TIMEOUT,
                DEFAULT_WAKE_LOCK_TIMEOUT);
        mWakeLockCount = 0;

        // initialize IMS RIL sender
        mSenderThread = new HandlerThread("ImsRILSender");
        mSenderThread.start();
        Looper looper = mSenderThread.getLooper();
        mSender = new ImsRILSender(looper);

        // initialize IMS RIL receiver
        mReceiver = new ImsRILReceiver();
        mReceiverThread = new Thread(mReceiver, "ImsRILReceiver");
        mReceiverThread.start();

    }

    public void setMute(boolean enableMute, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_MUTE, response);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + " " + enableMute);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(enableMute ? 1 : 0);

        send(rr);
    }

    /**
    * dial volte or vilte call.
    *
    * @param callee dialing number string
    * @param clirMode to present the dialing number or not
    * @param isEmergency indicate is emergency call or not
    * @param isVideoCall indicate is belong to vilte call or volte call
    * @param result command result
    *
    */
    public void start(String callee, int clirMode, boolean isEmergency,
            boolean isVideoCall, Message result) {
        String atCmdString = "DIALSOURCE_IMS";

        if (mMoCall != null) Rlog.e(IMS_RILA_LOG_TAG, "IMS: mMoCall is not null when dial !!");

        // New Mo Call Wrapper, used when OEM Hook Raw response return
        mMoCall = new MoCallInfo(callee, clirMode, isEmergency, isVideoCall, result);
        executeCommandResponse(atCmdString);
    }

    /**
    * dial volte or vilte conference call.
    *
    * @param participants participants dialing number string
    * @param clirMode to present the dialing number or not
    * @param isVideoCall indicate is belong to vilte call or volte call
    * @param result command result
    *
    */
    public void startConference(String[] participants, int clirMode,
            boolean isVideoCall, Message result) {
        String atCmdString = "DIALSOURCE_IMS";

        if (mConferenceCallDialInfo != null) {
            Rlog.e(IMS_RILA_LOG_TAG, "IMS: ConferenceCallDialInfo is not null when dial !!");
        }

        // New Mo Call Wrapper, used when OEM Hook Raw response return
        mConferenceCallDialInfo = new ConferenceCallDialInfo(participants, clirMode,
                isVideoCall, result);
        executeCommandResponse(atCmdString);
    }

    public void accept() {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_ANSWER, null);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);

    }

    /**
     * accept video call.
     *
     * @param videoMode indicate to the accept video call as video, audio, video_rx, or video_tx.
     * @param callId indicate which call we want to accept.
     *
     */
    public void acceptVideoCall(int videoMode, int callId) {
        if (IMS_RILA_LOGD) {
            riljLog("acceptVideoCall : callId = " + callId + ", videoMode = " + videoMode);
        }

        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_VIDEO_CALL_ACCEPT, null);

        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(videoMode);
        rr.mParcel.writeInt(callId);

        if (IMS_RILA_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                    + " " + videoMode + ", " + callId);
        }
        send(rr);
    }

    public void reject(int callId) {
        if (IMS_RILA_LOGD) riljLog("IMS reject : callId = " + callId);

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP, null);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " +
                callId);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callId);

        send(rr);
    }

    public void terminate(int callId) {
        if (IMS_RILA_LOGD) riljLog("IMS terminate : callId = " + callId);

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP, null);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " +
                callId);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callId);

        send(rr);
    }

    /**
     * To hold the call.
     * @param callId toIndicate which call session to hold.
     * @param result command result.
     */
    public void hold(int callId, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HOLD_CALL, result);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callId);

        if (IMS_RILA_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        }
        send(rr);
    }

    /**
     * To resume the call.
     * @param callId toIndicate which call session to resume.
     * @param result command result.
     */
    public void resume(int callId, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RESUME_CALL, result);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callId);

        if (IMS_RILA_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        }
        send(rr);
    }

    public void merge(Message result) {
        /// M: CC053: MoMS [Mobile Managerment] @{
        // 3. Permission Control for Conference call
        if (MobileManagerUtils.isSupported()) {
            if (!checkMoMSSubPermission(SubPermissions.MAKE_CONFERENCE_CALL)) {
                return;
            }
        }
        /// @}

        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_CONFERENCE, result);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        /// M: CC012: DTMF request special handling @{
        handleChldRelatedRequest(rr);
        ///@}
    }

    public void sendDtmf(char c, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_DTMF, result);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mParcel.writeString(Character.toString(c));

        send(rr);

    }

    /**
     * Start to paly a DTMF tone on the active call. Ignored if there is no active call
     * or there is a playing DTMF tone.
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * @param result is present the command is OK or fail
     */
    public void startDtmf(char c, Message result) {
        /// M: CC012: DTMF request special handling @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        synchronized (mDtmfReqQueue) {
            riljLog("startDtmf: queue size: " + mDtmfReqQueue.size());

            if (!mDtmfReqQueue.hasSendChldRequest() &&
                mDtmfReqQueue.size() < mDtmfReqQueue.MAXIMUM_DTMF_REQUEST) {
                if (!mDtmfReqQueue.isStart()) {
                    RILRequest rr = RILRequest.obtain(RIL_REQUEST_DTMF_START, result);

                    rr.mParcel.writeString(Character.toString(c));
                    mDtmfReqQueue.start();
                    mDtmfReqQueue.add(rr);
                    if (mDtmfReqQueue.size() == 1) {
                        riljLog("send start dtmf");
                        if (IMS_RILA_LOGD) {
                            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                        }
                        send(rr);
                    }
                } else {
                    riljLog("DTMF status conflict, want to start DTMF when status is " +
                        mDtmfReqQueue.isStart());
                }
            }
        }
        /// @}
    }

    /**
     * Stop the playing DTMF tone. Ignored if there is no playing DTMF
     * tone or no active call.
     * @param result is present the command is OK or fail
     */
    public void stopDtmf(Message result) {
        /// M: CC012: DTMF request special handling @{
        /* DTMF request will be ignored when the count of requests reaches 32 */
        synchronized (mDtmfReqQueue) {
            riljLog("stopDtmf: queue size: " + mDtmfReqQueue.size());

            if (!mDtmfReqQueue.hasSendChldRequest() &&
                mDtmfReqQueue.size() < mDtmfReqQueue.MAXIMUM_DTMF_REQUEST) {
                if (mDtmfReqQueue.isStart()) {
                    RILRequest rr = RILRequest.obtain(RIL_REQUEST_DTMF_STOP, result);

                    mDtmfReqQueue.stop();
                    mDtmfReqQueue.add(rr);
                    if (mDtmfReqQueue.size() == 1) {
                        riljLog("send stop dtmf");
                        if (IMS_RILA_LOGD) riljLog(rr.serialString() +
                            "> " + requestToString(rr.mRequest));
                        send(rr);
                    }
                } else {
                    riljLog("DTMF status conflict, want to start DTMF when status is " +
                        mDtmfReqQueue.isStart());
                }
            }
        }
        /// @}
    }
    public void setCallIndication(int mode, int callId, int seqNum) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SET_CALL_INDICATION, null);

        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(mode);
        rr.mParcel.writeInt(callId);
        rr.mParcel.writeInt(seqNum);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + " " + mode + ", " + callId + ", " + seqNum);

        send(rr);

    }

    public void turnOnIms(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_IMS_ENABLE, response);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(1);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void turnOffIms(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_IMS_ENABLE, response);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(0);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);

    }

    /// M: IMS feature(Can't work in 3G domain). @{
    public void addConferenceMember(int confCallId, String address, int callIdToAdd,
        Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER, response);

        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(Integer.toString(confCallId));
        rr.mParcel.writeString(address);
        rr.mParcel.writeString(Integer.toString(callIdToAdd));

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    public void removeConferenceMember(int confCallId, String address, int callIdToRemove,
        Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER, response);

        rr.mParcel.writeInt(3);
        rr.mParcel.writeString(Integer.toString(confCallId));
        rr.mParcel.writeString(address);
        rr.mParcel.writeString(Integer.toString(callIdToRemove));

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }
    /// @}

    /**
    * Add a new member to VoLTE conference call according to the parameter - address.
    *
    * @param confCallId IMS(VoLTE) conference call id
    * @param participant The address(phone number or SIP URI)
    * @param response Command response.
    *
    */
    public void inviteParticipants(int confCallId, String participant, Message response) {
        int participantCallId = -1;
        ImsCallInfo callInfo;
        for (Map.Entry<String, ImsCallInfo> entry : mCallConnections.entrySet()) {
            callInfo = entry.getValue();
            if (participant.equals(callInfo.mCallNum)) {
                participantCallId = Integer.parseInt(callInfo.mCallId);
                break;
            }
        }

        addConferenceMember(confCallId, participant, participantCallId, response);
    }

    /**
    * remove a member from VoLTE conference call according to the parameter - address.
    *
    * @param confCallId IMS(VoLTE) conference call id.
    * @param participant The address(phone number or SIP URI).
    * @param response Command response.
    *
    */
    public void removeParticipants(int confCallId, String participant, Message response) {
        int participantCallId = -1;
        ImsCallInfo callInfo;
        for (Map.Entry<String, ImsCallInfo> entry : mCallConnections.entrySet()) {
            callInfo = entry.getValue();
            if (participant.equals(callInfo.mCallNum)) {
                participantCallId = Integer.parseInt(callInfo.mCallId);
                break;
            }
        }

        removeConferenceMember(confCallId, participant, participantCallId, response);
    }

    /**
    * To get last call fail cause.
    *
    * @param response Command response.
    *
    */
    public void getLastCallFailCause(Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_LAST_CALL_FAIL_CAUSE, response);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
    * To release all calls.
    * @param response Command response.
    */
    public void hangupAllCall(Message response) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_HANGUP_ALL, response);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /**
    * To get callinfo by call Id.
    * @param callId callId.
    * @return reture the specified callinfo
    */
    public ImsCallInfo getCallInfo(String callId) {
        return mCallConnections.get(callId);
    }

    /**
    * To get callinfo by state.
    * @param state call state.
    * @return reture the specified callinfo
    */
    public ImsCallInfo getCallInfo(ImsCallInfo.State state) {
        ImsCallInfo callInfo;
        for (Map.Entry<String, ImsCallInfo> entry : mCallConnections.entrySet()) {
            callInfo = entry.getValue();
            Rlog.d(IMS_RILA_LOG_TAG, "getCallInfo- callID:" + callInfo.mCallId + "call num:" +
                    callInfo.mCallNum + "call State:" + callInfo.mState);
            if (callInfo.mState == state) {
                return callInfo;
            }
        }
        return null;
    }

    /**
    * To swap calls.
    * @param result Command response.
    */
    public void swap(Message result) {
        RILRequest rr
                = RILRequest.obtain(
                        RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE,
                                        result);
        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        /// M: CC012: DTMF request special handling @{
        handleChldRelatedRequest(rr);
        /// @}

    }

    /**
    * To send WFC profile information to modem
    * @param wfcPreference WFC preference selected by User
    */
    public void sendWfcProfileInfo(int wfcPreference) {
        String s[] = new String[2];
        s[0] = "AT+EWFCP=" + wfcPreference;
        s[1] = "";
        Rlog.d(IMS_RILA_LOG_TAG, "At cmnd:" + s[0]);
        invokeOemRilRequestStrings(s, null);
    }

    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_OEM_HOOK_STRINGS, response);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        rr.mParcel.writeStringArray(strings);

        send(rr);
    }
    private static int readRilMessage(InputStream is, byte[] buffer)
            throws IOException {
        int countRead;
        int offset;
        int remaining;
        int messageLength;

        // First, read in the length of the message
        offset = 0;
        remaining = 4;
        do {
            countRead = is.read(buffer, offset, remaining);

            if (countRead < 0) {
                Rlog.e(IMS_RILA_LOG_TAG, "Hit EOS reading message length");
                return -1;
            }

            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);

        messageLength = ((buffer[0] & 0xff) << 24)
                | ((buffer[1] & 0xff) << 16)
                | ((buffer[2] & 0xff) << 8)
                | (buffer[3] & 0xff);

        // Then, re-use the buffer and read in the message itself
        offset = 0;
        remaining = messageLength;
        do {
            countRead = is.read(buffer, offset, remaining);

            if (countRead < 0) {
                Rlog.e(IMS_RILA_LOG_TAG, "Hit EOS reading message.  messageLength=" + messageLength
                        + " remaining=" + remaining);
                return -1;
            }

            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);

        return messageLength;
    }

    class ImsRILSender extends Handler implements Runnable {
        public ImsRILSender(Looper looper) {
            super(looper);
        }

        // Only allocated once
        byte[] dataLength = new byte[4];

        //***** Runnable implementation
        @Override
        public void
        run() {
            //setup if needed
        }


        //***** Handler implementation
        @Override public void
        handleMessage(Message msg) {
            RILRequest rr = (RILRequest)(msg.obj);
            RILRequest req = null;

            switch (msg.what) {
                case EVENT_SEND:
                    try {
                        LocalSocket s;

                        s = mSocket;

                        if (s == null) {
                            rr.onError(RADIO_NOT_AVAILABLE, null);
                            rr.release();
                            decrementWakeLock();
                            return;
                        }

                        byte[] data;
                        data = rr.mParcel.marshall();
                        synchronized (mRequestList) {
                            mRequestList.append(rr.mSerial, rr);
                            rr.mParcel.recycle();
                            rr.mParcel = null;
                        }

                        if (data.length > RIL_MAX_COMMAND_BYTES) {
                            throw new RuntimeException(
                                    "Parcel larger than max bytes allowed! "
                                                          + data.length);
                        }

                        // parcel length in big endian
                        dataLength[0] = dataLength[1] = 0;
                        dataLength[2] = (byte)((data.length >> 8) & 0xff);
                        dataLength[3] = (byte)((data.length) & 0xff);

                        //Rlog.v(IMS_RILA_LOG_TAG, "writing packet: " + data.length + " bytes");

                        s.getOutputStream().write(dataLength);
                        s.getOutputStream().write(data);
                    } catch (IOException ex) {
                        Rlog.e(IMS_RILA_LOG_TAG, "IOException", ex);
                        req = findAndRemoveRequestFromList(rr.mSerial);
                        // make sure this request has not already been handled,
                        // eg, if RILReceiver cleared the list.
                        if (req != null) {
                            rr.onError(RADIO_NOT_AVAILABLE, null);
                            rr.release();
                            decrementWakeLock();
                        }
                    } catch (RuntimeException exc) {
                        Rlog.e(IMS_RILA_LOG_TAG, "Uncaught exception ", exc);
                        req = findAndRemoveRequestFromList(rr.mSerial);
                        // make sure this request has not already been handled,
                        // eg, if RILReceiver cleared the list.
                        if (req != null) {
                            rr.onError(GENERIC_FAILURE, null);
                            rr.release();
                            decrementWakeLock();
                        }
                    }
                    break;

                case EVENT_WAKE_LOCK_TIMEOUT:
                    // Haven't heard back from the last request.  Assume we're
                    // not getting a response and  release the wake lock.

                    // The timer of WAKE_LOCK_TIMEOUT is reset with each
                    // new send request. So when WAKE_LOCK_TIMEOUT occurs
                    // all requests in mRequestList already waited at
                    // least DEFAULT_WAKE_LOCK_TIMEOUT but no response.
                    //
                    // Note: Keep mRequestList so that delayed response
                    // can still be handled when response finally comes.

                    synchronized (mRequestList) {
                        if (clearWakeLock()) {
                            if (IMS_RILA_LOGD) {
                                int count = mRequestList.size();
                                Rlog.d(IMS_RILA_LOG_TAG, "WAKE_LOCK_TIMEOUT " +
                                        " mRequestList=" + count);
                                for (int i = 0; i < count; i++) {
                                    rr = mRequestList.valueAt(i);
                                    Rlog.d(IMS_RILA_LOG_TAG, i + ": [" + rr.mSerial + "] "
                                            + requestToString(rr.mRequest));
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    class ImsRILReceiver implements Runnable {
        byte[] buffer;

        ImsRILReceiver() {
            buffer = new byte[RIL_MAX_COMMAND_BYTES];
        }

        @Override
        public void
        run() {
            int retryCount = 0;
            String imsRilSocket = "rild-ims";

            try { for (;; ) {
                LocalSocket s = null;
                LocalSocketAddress l;

                try {
                    s = new LocalSocket();
                    l = new LocalSocketAddress(imsRilSocket,
                            LocalSocketAddress.Namespace.RESERVED);
                    s.connect(l);
                } catch (IOException ex) {
                    try {
                        if (s != null) {
                            s.close();
                        }
                    } catch (IOException ex2) {
                        //ignore failure to close after failure to connect
                        Rlog.e(IMS_RILA_LOG_TAG, "Failed to close the socket");
                    }

                    // don't print an error message after the the first time
                    // or after the 8th time

                    if (retryCount == 8) {
                        Rlog.e(IMS_RILA_LOG_TAG,
                            "Couldn't find '" + imsRilSocket
                            + "' socket after " + retryCount
                            + " times, continuing to retry silently");
                    } else if (retryCount > 0 && retryCount < 8) {
                        Rlog.i(IMS_RILA_LOG_TAG,
                            "Couldn't find '" + imsRilSocket
                            + "' socket; retrying after timeout");
                    }

                    try {
                        Thread.sleep(SOCKET_OPEN_RETRY_MILLIS);
                    } catch (InterruptedException er) {
                    }

                    retryCount++;
                    continue;
                }

                retryCount = 0;

                mSocket = s;
                Rlog.i(IMS_RILA_LOG_TAG, "Connected to '" + imsRilSocket + "' socket");

                int length = 0;
                try {
                    InputStream is = mSocket.getInputStream();

                    for (;; ) {
                        Parcel p;

                        length = readRilMessage(is, buffer);

                        if (length < 0) {
                            // End-of-stream reached
                            break;
                        }

                        p = Parcel.obtain();
                        p.unmarshall(buffer, 0, length);
                        p.setDataPosition(0);

                        //Rlog.v(IMS_RILA_LOG_TAG, "Read packet: " + length + " bytes");

                        processResponse(p);
                        p.recycle();
                    }
                } catch (java.io.IOException ex) {
                    Rlog.i(IMS_RILA_LOG_TAG, "'" + imsRilSocket + "' socket closed",
                          ex);
                } catch (Throwable tr) {
                    Rlog.e(IMS_RILA_LOG_TAG, "Uncaught exception read length=" + length +
                        "Exception:" + tr.toString());
                }

                Rlog.i(IMS_RILA_LOG_TAG, "Disconnected from '" + imsRilSocket
                      + "' socket");

                setRadioState (RadioState.RADIO_UNAVAILABLE);

                try {
                    mSocket.close();
                } catch (IOException ex) {
                }

                mSocket = null;
                //RILRequest.resetSerial();

                // Clear request list on close
                //clearRequestList(RADIO_NOT_AVAILABLE, false);
            } } catch (Throwable tr) {
                Rlog.e(IMS_RILA_LOG_TAG, "Uncaught exception", tr);
            }

            /* We're disconnected so we don't know the ril version */
            //notifyRegistrantsRilConnectionChanged(-1);
        }
    }

    /**
     * Holds a PARTIAL_WAKE_LOCK whenever
     * a) There is outstanding RIL request sent to RIL deamon and no replied
     * b) There is a request pending to be sent out.
     *
     * There is a WAKE_LOCK_TIMEOUT to release the lock, though it shouldn't
     * happen often.
     */

    private void acquireWakeLock() {
        synchronized (mWakeLock) {
            mWakeLock.acquire();
            mWakeLockCount++;

            mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            Message msg = mSender.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
            mSender.sendMessageDelayed(msg, mWakeLockTimeout);
        }
    }

    private void decrementWakeLock() {
        synchronized (mWakeLock) {
            if (mWakeLockCount > 1) {
                mWakeLockCount--;
            } else {
                mWakeLockCount = 0;
                mWakeLock.release();
                mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            }
        }
    }

    // true if we had the wakelock
    private boolean clearWakeLock() {
        synchronized (mWakeLock) {
            if (mWakeLockCount == 0 && mWakeLock.isHeld() == false) return false;
            Rlog.d(IMS_RILA_LOG_TAG, "NOTE: mWakeLockCount is " + mWakeLockCount +
                "at time of clearing");
            mWakeLockCount = 0;
            mWakeLock.release();
            mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            return true;
        }
    }

    private void send(RILRequest rr) {
        Message msg;

        if (mSocket == null) {
            rr.onError(RADIO_NOT_AVAILABLE, null);
            rr.release();
            return;
        }

        msg = mSender.obtainMessage(EVENT_SEND, rr);

        acquireWakeLock();

        msg.sendToTarget();
    }

    private void processResponse(Parcel p) {
        int type;
        Rlog.i(IMS_RILA_LOG_TAG, " IMS processResponse()");

        type = p.readInt();
        if (type == RESPONSE_UNSOLICITED) {
            processUnsolicited(p);
        } else if (type == RESPONSE_SOLICITED) {
            Rlog.i(IMS_RILA_LOG_TAG, "IMS: receive the RESPONSE_SOLICITED !!");
            RILRequest rr = processSolicited (p);
            if (rr != null) {
                rr.release();
                decrementWakeLock();
            }
        }
    }

   private RILRequest
   processSolicited (Parcel p) {
       int serial, error;
       boolean found = false;

       serial = p.readInt();
       error = p.readInt();

       RILRequest rr;

       rr = findAndRemoveRequestFromList(serial);

       if (rr == null) {
           Rlog.w(IMS_RILA_LOG_TAG, "Unexpected solicited response! sn: "
                           + serial + " error: " + error);
           return null;
       }

       /// M: CC012: DTMF request special handling @{
       /* DTMF request will be ignored when the count of requests reaches 32 */
       if ((rr.mRequest == RIL_REQUEST_DTMF_START) ||
           (rr.mRequest == RIL_REQUEST_DTMF_STOP)) {
           synchronized (mDtmfReqQueue) {
               mDtmfReqQueue.remove(rr);
               riljLog("remove first item in dtmf queue done, size = " + mDtmfReqQueue.size());
               if (mDtmfReqQueue.size() > 0) {
                   RILRequest rr2 = mDtmfReqQueue.get();
                   if (IMS_RILA_LOGD) riljLog(rr2.serialString() + "> " + requestToString(rr2.mRequest));
                   send(rr2);
               } else {
                   if (mDtmfReqQueue.getPendingRequest() != null) {
                       riljLog("send pending switch request");
                       send(mDtmfReqQueue.getPendingRequest());
                       mDtmfReqQueue.setSendChldRequest();
                       mDtmfReqQueue.setPendingRequest(null);
                   }
               }
           }
       }
       /// @}
       Object ret = null;

       /// M: CC012: DTMF request special handling @{
       if (rr.mRequest == RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE ||
           rr.mRequest == RIL_REQUEST_CONFERENCE ||
           rr.mRequest == RIL_REQUEST_SEPARATE_CONNECTION ||
           rr.mRequest == RIL_REQUEST_EXPLICIT_CALL_TRANSFER) {
           riljLog("clear mIsSendChldRequest");
           mDtmfReqQueue.resetSendChldRequest();
       }
       /// @}

       if (error == 0 || p.dataAvail() > 0) {
           // either command succeeds or command fails but with data payload
           try {switch (rr.mRequest) {

           case RIL_REQUEST_DIAL: ret =  responseVoid(p); break;
           case RIL_REQUEST_GET_IMSI: ret =  responseString(p); break;
           case RIL_REQUEST_HANGUP: ret =  responseVoid(p); break;
           case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: ret =  responseVoid(p); break;
           case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE: ret =  responseVoid(p); break;
           case RIL_REQUEST_CONFERENCE: ret =  responseVoid(p); break;
           case RIL_REQUEST_UDUB: ret =  responseVoid(p); break;
           case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: ret =  responseFailCause(p); break;
           case RIL_REQUEST_VOICE_REGISTRATION_STATE: ret =  responseStrings(p); break;
           case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  responseStrings(p); break;
           case RIL_REQUEST_OPERATOR: ret =  responseStrings(p); break;
           case RIL_REQUEST_RADIO_POWER: ret =  responseVoid(p); break;
           case RIL_REQUEST_DTMF: ret =  responseVoid(p); break;
           case RIL_REQUEST_SEND_USSD: ret =  responseVoid(p); break;
           case RIL_REQUEST_CANCEL_USSD: ret =  responseVoid(p); break;
           case RIL_REQUEST_GET_CLIR: ret =  responseInts(p); break;
           case RIL_REQUEST_SET_CLIR: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_CALL_FORWARD: ret =  responseVoid(p); break;
           case RIL_REQUEST_QUERY_CALL_WAITING: ret =  responseInts(p); break;
           case RIL_REQUEST_SET_CALL_WAITING: ret =  responseVoid(p); break;
           case RIL_REQUEST_SMS_ACKNOWLEDGE: ret =  responseVoid(p); break;
           case RIL_REQUEST_GET_IMEI: ret =  responseString(p); break;
           case RIL_REQUEST_GET_IMEISV: ret =  responseString(p); break;
           case RIL_REQUEST_ANSWER: ret =  responseVoid(p); break;
           case RIL_REQUEST_QUERY_FACILITY_LOCK: ret =  responseInts(p); break;
           case RIL_REQUEST_SET_FACILITY_LOCK: ret =  responseInts(p); break;
           case RIL_REQUEST_CHANGE_BARRING_PASSWORD: ret =  responseVoid(p); break;
           case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: ret =  responseInts(p); break;
           case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: ret =  responseVoid(p); break;
           case RIL_REQUEST_ABORT_QUERY_AVAILABLE_NETWORKS: ret =  responseVoid(p); break;
           case RIL_REQUEST_DTMF_START: ret =  responseVoid(p); break;
           case RIL_REQUEST_DTMF_STOP: ret =  responseVoid(p); break;
           case RIL_REQUEST_BASEBAND_VERSION: ret =  responseString(p); break;
           case RIL_REQUEST_SEPARATE_CONNECTION: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_MUTE: ret =  responseVoid(p); break;
           case RIL_REQUEST_GET_MUTE: ret =  responseInts(p); break;
           case RIL_REQUEST_QUERY_CLIP: ret =  responseInts(p); break;
           case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: ret =  responseInts(p); break;
           case RIL_REQUEST_RESET_RADIO: ret =  responseVoid(p); break;
           case RIL_REQUEST_OEM_HOOK_RAW: ret =  responseRaw(p); break;
           case RIL_REQUEST_OEM_HOOK_STRINGS: ret =  responseStrings(p); break;
           case RIL_REQUEST_SCREEN_STATE: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: ret =  responseVoid(p); break;
           case RIL_REQUEST_WRITE_SMS_TO_SIM: ret =  responseInts(p); break;
           case RIL_REQUEST_DELETE_SMS_ON_SIM: ret =  responseVoid(p); break;
           case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: ret =  responseInts(p); break;
           case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_LOCATION_UPDATES: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_TTY_MODE: ret =  responseVoid(p); break;
           case RIL_REQUEST_QUERY_TTY_MODE: ret =  responseInts(p); break;
           case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE: ret = responseVoid(p); break;
           case RIL_REQUEST_VOICE_RADIO_TECH: ret = responseInts(p); break;
           case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE: ret = responseVoid(p); break;
           case RIL_REQUEST_SET_INITIAL_ATTACH_APN: ret = responseVoid(p); break;
           case RIL_REQUEST_SET_DATA_PROFILE: ret = responseVoid(p); break;
           case RIL_REQUEST_IMS_REGISTRATION_STATE: ret = responseInts(p); break;
           /// M: CC010: Add RIL interface @{
           case RIL_REQUEST_HANGUP_ALL: ret =  responseVoid(p); break;
           case RIL_REQUEST_FORCE_RELEASE_CALL: ret = responseVoid(p); break;
           case RIL_REQUEST_SET_CALL_INDICATION: ret = responseVoid(p); break;
           case RIL_REQUEST_EMERGENCY_DIAL: ret =  responseVoid(p); break;
           case RIL_REQUEST_SET_ECC_SERVICE_CATEGORY: ret = responseVoid(p); break;
           case RIL_REQUEST_SET_ECC_LIST: ret = responseVoid(p); break;
           /// @}
           /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
           case RIL_REQUEST_SET_SPEECH_CODEC_INFO: ret = responseVoid(p); break;
           /// @}
           /// M: For 3G VT only @{
           case RIL_REQUEST_VT_DIAL: ret = responseVoid(p); break;
           case RIL_REQUEST_VOICE_ACCEPT: ret = responseVoid(p); break;
           case RIL_REQUEST_REPLACE_VT_CALL: ret = responseVoid(p); break;
           /// @}
           /// M: IMS feature. @{
           case RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER: responseString(p); break;
           case RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER: responseString(p); break;
           case RIL_REQUEST_DIAL_WITH_SIP_URI: ret = responseVoid(p); break;
           case RIL_REQUEST_RESUME_CALL: ret = responseVoid(p); break;
           case RIL_REQUEST_HOLD_CALL: ret = responseVoid(p); break;
           /// @}

           //MTK-START SS
           case RIL_REQUEST_GET_COLP: ret = responseInts(p); break;
           case RIL_REQUEST_SET_COLP: ret = responseVoid(p); break;
           case RIL_REQUEST_GET_COLR: ret = responseInts(p); break;
           //MTK-END SS

           // IMS
           case RIL_REQUEST_SET_IMS_ENABLE: ret = responseVoid(p); break;
           case RIL_REQUEST_SIM_GET_ATR: ret = responseString(p); break;
           // M: Fast Dormancy
           case RIL_REQUEST_SET_SCRI: ret = responseVoid(p); break;
           case RIL_REQUEST_SET_FD_MODE: ret = responseInts(p); break;

           /// M: SVLTE Remove access feature
           case RIL_REQUEST_CONFIG_MODEM_STATUS: ret = responseVoid(p); break;

           /// M: IMS VoLTE conference dial feature. @{
           case RIL_REQUEST_CONFERENCE_DIAL: ret =  responseVoid(p); break;
           /// @}
           case RIL_REQUEST_RELOAD_MODEM_TYPE: ret =  responseVoid(p); break;
           /// M: CC010: Add RIL interface @{
           case RIL_REQUEST_SET_IMS_CALL_STATUS: ret = responseVoid(p); break;
           /// @}

           /// M: CC072: Add Customer proprietary-IMS RIL interface. @{
           case RIL_REQUEST_SET_SRVCC_CALL_CONTEXT_TRANSFER: ret = responseVoid(p); break;
           case RIL_REQUEST_UPDATE_IMS_REGISTRATION_STATUS: ret = responseVoid(p); break;
           /// @}
           /// M: IMS ViLTE feature. @{
           case RIL_REQUEST_VIDEO_CALL_ACCEPT: ret = responseVoid(p); break;
           /// @}
           default:
               throw new RuntimeException("Unrecognized solicited response: " + rr.mRequest);
           //break;
           }} catch (Throwable tr) {
               // Exceptions here usually mean invalid RIL responses

               Rlog.w(IMS_RILA_LOG_TAG, rr.serialString() + "< "
                       + requestToString(rr.mRequest)
                       + " exception, possible invalid RIL response", tr);

               if (rr.mResult != null) {
                   AsyncResult.forMessage(rr.mResult, null, tr);
                   rr.mResult.sendToTarget();
               }
               return rr;
           }
       }

       if (rr.mRequest == RIL_REQUEST_SHUTDOWN) {
           // Set RADIO_STATE to RADIO_UNAVAILABLE to continue shutdown process
           // regardless of error code to continue shutdown procedure.
           riljLog("Response to RIL_REQUEST_SHUTDOWN received. Error is " +
                   error + " Setting Radio State to Unavailable regardless of error.");
           setRadioState(RadioState.RADIO_UNAVAILABLE);
       }

       if (error != 0) {
           rr.onError(error, ret);
       } else {

           if (IMS_RILA_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                   + " " + retToString(rr.mRequest, ret));

           if (rr.mResult != null) {
               AsyncResult.forMessage(rr.mResult, ret, null);
               rr.mResult.sendToTarget();
           }
       }
       return rr;
   }

    private RILRequest findAndRemoveRequestFromList(int serial) {
        RILRequest rr = null;
        synchronized (mRequestList) {
            rr = mRequestList.get(serial);
            if (rr != null) {
                mRequestList.remove(serial);
            }
        }

        return rr;
    }

    private Object responseVoid(Parcel p) {
        return null;
    }

    private Object responseString(Parcel p) {
        String response;

        response = p.readString();

        return response;
    }

    private Object responseStrings(Parcel p) {
        int num;
        String response[];

        response = p.readStringArray();

        return response;
    }

    private Object
    responseRaw(Parcel p) {
        int num;
        byte response[];

        response = p.createByteArray();

        return response;
    }

    private Object responseInts(Parcel p) {
        int numInts;
        int response[];

        numInts = p.readInt();

        response = new int[numInts];

        for (int i = 0 ; i < numInts ; i++) {
            response[i] = p.readInt();
        }

        return response;
    }

    private Object
    responseCallRing(Parcel p){
        char response[] = new char[4];

        response[0] = (char) p.readInt();    // isPresent
        response[1] = (char) p.readInt();    // signalType
        response[2] = (char) p.readInt();    // alertPitch
        response[3] = (char) p.readInt();    // signal

        return response;
    }

    private Object
    responseSuppServiceNotification(Parcel p) {
        SuppServiceNotification notification = new SuppServiceNotification();

        notification.notificationType = p.readInt();
        notification.code = p.readInt();
        notification.index = p.readInt();
        notification.type = p.readInt();
        notification.number = p.readString();

        return notification;
    }

    private Object
    responseFailCause(Parcel p) {
        LastCallFailCause failCause = new LastCallFailCause();
        failCause.causeCode = p.readInt();
        if (p.dataAvail() > 0) {
          failCause.vendorCause = p.readString();
        }
        return failCause;
    }

    static String responseToString(int request)
    {
        switch(request) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED: return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED: return "UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_NEW_SMS: return "UNSOL_RESPONSE_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT: return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM: return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            case RIL_UNSOL_ON_USSD: return "UNSOL_ON_USSD";
            case RIL_UNSOL_ON_USSD_REQUEST: return "UNSOL_ON_USSD_REQUEST";
            case RIL_UNSOL_NITZ_TIME_RECEIVED: return "UNSOL_NITZ_TIME_RECEIVED";
            case RIL_UNSOL_SIGNAL_STRENGTH: return "UNSOL_SIGNAL_STRENGTH";
            case RIL_UNSOL_DATA_CALL_LIST_CHANGED: return "UNSOL_DATA_CALL_LIST_CHANGED";
            case RIL_UNSOL_SUPP_SVC_NOTIFICATION: return "UNSOL_SUPP_SVC_NOTIFICATION";
            case RIL_UNSOL_STK_SESSION_END: return "UNSOL_STK_SESSION_END";
            case RIL_UNSOL_STK_PROACTIVE_COMMAND: return "UNSOL_STK_PROACTIVE_COMMAND";
            case RIL_UNSOL_STK_EVENT_NOTIFY: return "UNSOL_STK_EVENT_NOTIFY";
            case RIL_UNSOL_STK_CALL_SETUP: return "UNSOL_STK_CALL_SETUP";
            case RIL_UNSOL_SIM_SMS_STORAGE_FULL: return "UNSOL_SIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_SIM_REFRESH: return "UNSOL_SIM_REFRESH";
            case RIL_UNSOL_CALL_RING: return "UNSOL_CALL_RING";
            case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED: return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS: return "UNSOL_RESPONSE_CDMA_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS: return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
            case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL: return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_RESTRICTED_STATE_CHANGED: return "UNSOL_RESTRICTED_STATE_CHANGED";
            case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE: return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_CDMA_CALL_WAITING: return "UNSOL_CDMA_CALL_WAITING";
            case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS: return "UNSOL_CDMA_OTA_PROVISION_STATUS";
            case RIL_UNSOL_CDMA_INFO_REC: return "UNSOL_CDMA_INFO_REC";
            case RIL_UNSOL_OEM_HOOK_RAW: return "UNSOL_OEM_HOOK_RAW";
            case RIL_UNSOL_RINGBACK_TONE: return "UNSOL_RINGBACK_TONE";
            case RIL_UNSOL_RESEND_INCALL_MUTE: return "UNSOL_RESEND_INCALL_MUTE";
            case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED: return "CDMA_SUBSCRIPTION_SOURCE_CHANGED";
            case RIL_UNSOl_CDMA_PRL_CHANGED: return "UNSOL_CDMA_PRL_CHANGED";
            case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE: return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_RIL_CONNECTED: return "UNSOL_RIL_CONNECTED";
            case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED: return "UNSOL_VOICE_RADIO_TECH_CHANGED";
            case RIL_UNSOL_CELL_INFO_LIST: return "UNSOL_CELL_INFO_LIST";
            case RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED:
                return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED:
                    return "RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED";
            case RIL_UNSOL_SRVCC_STATE_NOTIFY:
                    return "UNSOL_SRVCC_STATE_NOTIFY";
            case RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR:
                    return "UNSOL_SIP_CALL_PROGRESS_INDICATOR";
            case RIL_UNSOL_HARDWARE_CONFIG_CHANGED: return "RIL_UNSOL_HARDWARE_CONFIG_CHANGED";
            /* M: CC part start */
            case RIL_UNSOL_CALL_FORWARDING: return "UNSOL_CALL_FORWARDING";
            case RIL_UNSOL_CRSS_NOTIFICATION: return "UNSOL_CRSS_NOTIFICATION";
            case RIL_UNSOL_INCOMING_CALL_INDICATION: return "UNSOL_INCOMING_CALL_INDICATION";
            case RIL_UNSOL_CIPHER_INDICATION: return "RIL_UNSOL_CIPHER_INDICATION";
            case RIL_UNSOL_CNAP: return "RIL_UNSOL_CNAP";
            case RIL_UNSOL_SPEECH_CODEC_INFO: return "UNSOL_SPEECH_CODEC_INFO";
            /* M: CC part end */
            //MTK-START multiple application support
            case RIL_UNSOL_APPLICATION_SESSION_ID_CHANGED: return "RIL_UNSOL_APPLICATION_SESSION_ID_CHANGED";
            //MTK-END multiple application support
            case RIL_UNSOL_SIM_MISSING: return "UNSOL_SIM_MISSING";
            case RIL_UNSOL_VIRTUAL_SIM_ON: return "UNSOL_VIRTUAL_SIM_ON";
            case RIL_UNSOL_VIRTUAL_SIM_OFF: return "UNSOL_VIRTUAL_SIM_ON_OFF";
            case RIL_UNSOL_SIM_RECOVERY: return "UNSOL_SIM_RECOVERY";
            case RIL_UNSOL_SIM_PLUG_OUT: return "UNSOL_SIM_PLUG_OUT";
            case RIL_UNSOL_SIM_PLUG_IN: return "UNSOL_SIM_PLUG_IN";
            case RIL_UNSOL_SIM_COMMON_SLOT_NO_CHANGED: return "RIL_UNSOL_SIM_COMMON_SLOT_NO_CHANGED";
            case RIL_UNSOL_DATA_ALLOWED: return "RIL_UNSOL_DATA_ALLOWED";
            case RIL_UNSOL_PHB_READY_NOTIFICATION: return "UNSOL_PHB_READY_NOTIFICATION";
            case RIL_UNSOL_IMEI_LOCK: return "UNSOL_IMEI_LOCK";
            case RIL_UNSOL_RESPONSE_ACMT: return "UNSOL_ACMT_INFO";
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED: return "UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_MMRR_STATUS_CHANGED: return "UNSOL_RESPONSE_MMRR_STATUS_CHANGED";
            case RIL_UNSOL_NEIGHBORING_CELL_INFO: return "UNSOL_NEIGHBORING_CELL_INFO";
            case RIL_UNSOL_NETWORK_INFO: return "UNSOL_NETWORK_INFO";
            case RIL_UNSOL_IMS_ENABLE_DONE: return "RIL_UNSOL_IMS_ENABLE_DONE";
            case RIL_UNSOL_IMS_DISABLE_DONE: return "RIL_UNSOL_IMS_DISABLE_DONE";
            case RIL_UNSOL_IMS_REGISTRATION_INFO: return "RIL_UNSOL_IMS_REGISTRATION_INFO";
            case RIL_UNSOL_STK_SETUP_MENU_RESET: return "RIL_UNSOL_STK_SETUP_MENU_RESET";
            case RIL_UNSOL_RESPONSE_PLMN_CHANGED: return "RIL_UNSOL_RESPONSE_PLMN_CHANGED";
            case RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED: return "RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED";
            //Remote SIM ME lock related APIs [Start]
            case RIL_UNSOL_MELOCK_NOTIFICATION: return "RIL_UNSOL_MELOCK_NOTIFICATION";
            //Remote SIM ME lock related APIs [End]
            // M: Fast Dormancy
            case RIL_UNSOL_SCRI_RESULT: return "RIL_UNSOL_SCRI_RESULT";
            case RIL_UNSOL_STK_EVDL_CALL: return "RIL_UNSOL_STK_EVDL_CALL";
            case RIL_UNSOL_STK_CALL_CTRL: return "RIL_UNSOL_STK_CALL_CTRL";

            /// M: IMS feature. @{
            case RIL_UNSOL_ECONF_SRVCC_INDICATION: return "RIL_UNSOL_ECONF_SRVCC_INDICATION";
            //For updating conference call merged/added result.
            case RIL_UNSOL_ECONF_RESULT_INDICATION: return "RIL_UNSOL_ECONF_RESULT_INDICATION";
            //For updating call mode and pau information.
            case RIL_UNSOL_CALL_INFO_INDICATION : return "RIL_UNSOL_CALL_INFO_INDICATION";
            /// @}

            case RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_INFO: return "RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_INFO";
            case RIL_UNSOL_SRVCC_HANDOVER_INFO_INDICATION: return "RIL_UNSOL_SRVCC_HANDOVER_INFO_INDICATION";
            // M: CC33 LTE.
            case RIL_UNSOL_RAC_UPDATE: return "RIL_UNSOL_RAC_UPDATE";
            case RIL_UNSOL_REMOVE_RESTRICT_EUTRAN: return "RIL_UNSOL_REMOVE_RESTRICT_EUTRAN";

            //MTK-START for MD state change
            case RIL_UNSOL_MD_STATE_CHANGE: return "RIL_UNSOL_MD_STATE_CHANGE";
            //MTK-END for MD state change
            case RIL_UNSOL_STK_CC_ALPHA_NOTIFY: return "UNSOL_STK_CC_ALPHA_NOTIFY";
            //IMS
            case RIL_UNSOL_IMS_ENABLE_START: return "RIL_UNSOL_IMS_ENABLE_START";
            case RIL_UNSOL_IMS_DISABLE_START: return "RIL_UNSOL_IMS_DISABLE_START";
            //IMS feature for ViLTE related URC.
            case RIL_UNSOL_CALLMOD_CHANGE_INDICATOR: return "RIL_UNSOL_CALLMOD_CHANGE_INDICATOR";
            case RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR:
                return "RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR";
            default: return "<unknown response>";
        }
    }

    static String requestToString(int request) {
        switch(request) {
            case RIL_REQUEST_GET_SIM_STATUS: return "GET_SIM_STATUS";
            case RIL_REQUEST_ENTER_SIM_PIN: return "ENTER_SIM_PIN";
            case RIL_REQUEST_ENTER_SIM_PUK: return "ENTER_SIM_PUK";
            case RIL_REQUEST_ENTER_SIM_PIN2: return "ENTER_SIM_PIN2";
            case RIL_REQUEST_ENTER_SIM_PUK2: return "ENTER_SIM_PUK2";
            case RIL_REQUEST_CHANGE_SIM_PIN: return "CHANGE_SIM_PIN";
            case RIL_REQUEST_CHANGE_SIM_PIN2: return "CHANGE_SIM_PIN2";
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION:
                return "ENTER_NETWORK_DEPERSONALIZATION";
            case RIL_REQUEST_GET_CURRENT_CALLS: return "GET_CURRENT_CALLS";
            case RIL_REQUEST_DIAL: return "DIAL";
            case RIL_REQUEST_GET_IMSI: return "GET_IMSI";
            case RIL_REQUEST_HANGUP: return "HANGUP";
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND: return "HANGUP_WAITING_OR_BACKGROUND";
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
                return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE:
                return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case RIL_REQUEST_CONFERENCE: return "CONFERENCE";
            case RIL_REQUEST_UDUB: return "UDUB";
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE: return "LAST_CALL_FAIL_CAUSE";
            case RIL_REQUEST_SIGNAL_STRENGTH: return "SIGNAL_STRENGTH";
            case RIL_REQUEST_VOICE_REGISTRATION_STATE: return "VOICE_REGISTRATION_STATE";
            case RIL_REQUEST_DATA_REGISTRATION_STATE: return "DATA_REGISTRATION_STATE";
            case RIL_REQUEST_OPERATOR: return "OPERATOR";
            case RIL_REQUEST_RADIO_POWER: return "RADIO_POWER";
            case RIL_REQUEST_DTMF: return "DTMF";
            case RIL_REQUEST_SEND_SMS: return "SEND_SMS";
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE: return "SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_SETUP_DATA_CALL: return "SETUP_DATA_CALL";
            case RIL_REQUEST_SIM_IO: return "SIM_IO";
            case RIL_REQUEST_SEND_USSD: return "SEND_USSD";
            case RIL_REQUEST_CANCEL_USSD: return "CANCEL_USSD";
            case RIL_REQUEST_GET_CLIR: return "GET_CLIR";
            case RIL_REQUEST_SET_CLIR: return "SET_CLIR";
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS: return "QUERY_CALL_FORWARD_STATUS";
            case RIL_REQUEST_SET_CALL_FORWARD: return "SET_CALL_FORWARD";
            case RIL_REQUEST_QUERY_CALL_WAITING: return "QUERY_CALL_WAITING";
            case RIL_REQUEST_SET_CALL_WAITING: return "SET_CALL_WAITING";
            case RIL_REQUEST_SMS_ACKNOWLEDGE: return "SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GET_IMEI: return "GET_IMEI";
            case RIL_REQUEST_GET_IMEISV: return "GET_IMEISV";
            case RIL_REQUEST_ANSWER: return "ANSWER";
            case RIL_REQUEST_DEACTIVATE_DATA_CALL: return "DEACTIVATE_DATA_CALL";
            case RIL_REQUEST_QUERY_FACILITY_LOCK: return "QUERY_FACILITY_LOCK";
            case RIL_REQUEST_SET_FACILITY_LOCK: return "SET_FACILITY_LOCK";
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD: return "CHANGE_BARRING_PASSWORD";
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE: return "QUERY_NETWORK_SELECTION_MODE";
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC:
                return "SET_NETWORK_SELECTION_AUTOMATIC";
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL: return "SET_NETWORK_SELECTION_MANUAL";
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS : return "QUERY_AVAILABLE_NETWORKS ";
            case RIL_REQUEST_ABORT_QUERY_AVAILABLE_NETWORKS:
                return "ABORT_QUERY_AVAILABLE_NETWORKS";
            case RIL_REQUEST_DTMF_START: return "DTMF_START";
            case RIL_REQUEST_DTMF_STOP: return "DTMF_STOP";
            case RIL_REQUEST_BASEBAND_VERSION: return "BASEBAND_VERSION";
            case RIL_REQUEST_SEPARATE_CONNECTION: return "SEPARATE_CONNECTION";
            case RIL_REQUEST_SET_MUTE: return "SET_MUTE";
            case RIL_REQUEST_GET_MUTE: return "GET_MUTE";
            case RIL_REQUEST_QUERY_CLIP: return "QUERY_CLIP";
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE: return "LAST_DATA_CALL_FAIL_CAUSE";
            case RIL_REQUEST_DATA_CALL_LIST: return "DATA_CALL_LIST";
            case RIL_REQUEST_RESET_RADIO: return "RESET_RADIO";
            case RIL_REQUEST_OEM_HOOK_RAW: return "OEM_HOOK_RAW";
            case RIL_REQUEST_OEM_HOOK_STRINGS: return "OEM_HOOK_STRINGS";
            case RIL_REQUEST_SCREEN_STATE: return "SCREEN_STATE";
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION: return "SET_SUPP_SVC_NOTIFICATION";
            case RIL_REQUEST_WRITE_SMS_TO_SIM: return "WRITE_SMS_TO_SIM";
            case RIL_REQUEST_DELETE_SMS_ON_SIM: return "DELETE_SMS_ON_SIM";
            case RIL_REQUEST_SET_BAND_MODE: return "SET_BAND_MODE";
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE: return "QUERY_AVAILABLE_BAND_MODE";
            case RIL_REQUEST_STK_GET_PROFILE: return "REQUEST_STK_GET_PROFILE";
            case RIL_REQUEST_STK_SET_PROFILE: return "REQUEST_STK_SET_PROFILE";
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND: return "REQUEST_STK_SEND_ENVELOPE_COMMAND";
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE:
                return "REQUEST_STK_SEND_TERMINAL_RESPONSE";
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM:
                return "REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER: return "REQUEST_EXPLICIT_CALL_TRANSFER";
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE:
                return "REQUEST_SET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE:
                return "REQUEST_GET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS: return "REQUEST_GET_NEIGHBORING_CELL_IDS";
            case RIL_REQUEST_SET_LOCATION_UPDATES: return "REQUEST_SET_LOCATION_UPDATES";
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE:
                return "RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE:
                return "RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE";
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE:
                return "RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE";
            case RIL_REQUEST_SET_TTY_MODE: return "RIL_REQUEST_SET_TTY_MODE";
            case RIL_REQUEST_QUERY_TTY_MODE: return "RIL_REQUEST_QUERY_TTY_MODE";
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE:
                return "RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE:
                return "RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_FLASH: return "RIL_REQUEST_CDMA_FLASH";
            case RIL_REQUEST_CDMA_BURST_DTMF: return "RIL_REQUEST_CDMA_BURST_DTMF";
            case RIL_REQUEST_CDMA_SEND_SMS: return "RIL_REQUEST_CDMA_SEND_SMS";
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE: return "RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG:
                return "RIL_REQUEST_GSM_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG:
                return "RIL_REQUEST_GSM_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG:
                return "RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG:
                return "RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION:
                return "RIL_REQUEST_GSM_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY:
                return "RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY";
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION:
                return "RIL_REQUEST_CDMA_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_SUBSCRIPTION: return "RIL_REQUEST_CDMA_SUBSCRIPTION";
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM: return "RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM";
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM: return "RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM";
            case RIL_REQUEST_DEVICE_IDENTITY: return "RIL_REQUEST_DEVICE_IDENTITY";
            case RIL_REQUEST_GET_SMSC_ADDRESS: return "RIL_REQUEST_GET_SMSC_ADDRESS";
            case RIL_REQUEST_SET_SMSC_ADDRESS: return "RIL_REQUEST_SET_SMSC_ADDRESS";
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE:
                return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS:
                return "RIL_REQUEST_REPORT_SMS_MEMORY_STATUS";
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING:
                return "RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING";
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE:
                return "RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_ISIM_AUTHENTICATION: return "RIL_REQUEST_ISIM_AUTHENTICATION";
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU:
                return "RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS:
                return "RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS";
            case RIL_REQUEST_VOICE_RADIO_TECH: return "RIL_REQUEST_VOICE_RADIO_TECH";
            case RIL_REQUEST_GET_CELL_INFO_LIST: return "RIL_REQUEST_GET_CELL_INFO_LIST";
            case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE:
                return "RIL_REQUEST_SET_CELL_INFO_LIST_RATE";
            case RIL_REQUEST_SET_INITIAL_ATTACH_APN: return "RIL_REQUEST_SET_INITIAL_ATTACH_APN";
            case RIL_REQUEST_SET_DATA_PROFILE: return "RIL_REQUEST_SET_DATA_PROFILE";
            case RIL_REQUEST_IMS_REGISTRATION_STATE: return "RIL_REQUEST_IMS_REGISTRATION_STATE";
            case RIL_REQUEST_IMS_SEND_SMS: return "RIL_REQUEST_IMS_SEND_SMS";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC: return "RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC";
            case RIL_REQUEST_SIM_OPEN_CHANNEL: return "RIL_REQUEST_SIM_OPEN_CHANNEL";
            case RIL_REQUEST_SIM_CLOSE_CHANNEL: return "RIL_REQUEST_SIM_CLOSE_CHANNEL";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL:
                return "RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL";
            case RIL_REQUEST_NV_READ_ITEM: return "RIL_REQUEST_NV_READ_ITEM";
            case RIL_REQUEST_NV_WRITE_ITEM: return "RIL_REQUEST_NV_WRITE_ITEM";
            case RIL_REQUEST_NV_WRITE_CDMA_PRL: return "RIL_REQUEST_NV_WRITE_CDMA_PRL";
            case RIL_REQUEST_NV_RESET_CONFIG: return "RIL_REQUEST_NV_RESET_CONFIG";
            case RIL_REQUEST_SET_UICC_SUBSCRIPTION: return "RIL_REQUEST_SET_UICC_SUBSCRIPTION";
            case RIL_REQUEST_ALLOW_DATA: return "RIL_REQUEST_ALLOW_DATA";
            case RIL_REQUEST_GET_HARDWARE_CONFIG: return "GET_HARDWARE_CONFIG";
            case RIL_REQUEST_SIM_AUTHENTICATION: return "RIL_REQUEST_SIM_AUTHENTICATION";
            case RIL_REQUEST_SHUTDOWN: return "RIL_REQUEST_SHUTDOWN";
            case RIL_REQUEST_SET_RADIO_CAPABILITY:
                    return "RIL_REQUEST_SET_RADIO_CAPABILITY";
            case RIL_REQUEST_GET_RADIO_CAPABILITY:
                    return "RIL_REQUEST_GET_RADIO_CAPABILITY";
            /// M: CC010: Add RIL interface @{
            case RIL_REQUEST_HANGUP_ALL: return "HANGUP_ALL";
            case RIL_REQUEST_FORCE_RELEASE_CALL: return "FORCE_RELEASE_CALL";
            case RIL_REQUEST_SET_CALL_INDICATION: return "SET_CALL_INDICATION";
            case RIL_REQUEST_EMERGENCY_DIAL: return "EMERGENCY_DIAL";
            case RIL_REQUEST_SET_ECC_SERVICE_CATEGORY: return "SET_ECC_SERVICE_CATEGORY";
            case RIL_REQUEST_SET_ECC_LIST: return "SET_ECC_LIST";
            /// @}
            /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
            case RIL_REQUEST_SET_SPEECH_CODEC_INFO: return "SET_SPEECH_CODEC_INFO";
            /// @}
            /// M: For 3G VT only @{
            case RIL_REQUEST_VT_DIAL: return "RIL_REQUEST_VT_DIAL";
            case RIL_REQUEST_VOICE_ACCEPT: return "VOICE_ACCEPT";
            case RIL_REQUEST_REPLACE_VT_CALL: return "RIL_REQUEST_REPLACE_VT_CALL";
            /// @}

            /// M: IMS feature. @{
            case RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER:
                return "RIL_REQUEST_ADD_IMS_CONFERENCE_CALL_MEMBER";
            case RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER:
                return "RIL_REQUEST_REMOVE_IMS_CONFERENCE_CALL_MEMBER";
            case RIL_REQUEST_DIAL_WITH_SIP_URI: return "RIL_REQUEST_DIAL_WITH_SIP_URI";
            case RIL_REQUEST_RESUME_CALL: return "RIL_REQUEST_RESUNME_CALL";
            case RIL_REQUEST_HOLD_CALL: return "RIL_REQUEST_HOLD_CALL";
            /// @}

            //MTK-START SS
            case RIL_REQUEST_GET_COLP: return "GET_COLP";
            case RIL_REQUEST_SET_COLP: return "SET_COLP";
            case RIL_REQUEST_GET_COLR: return "GET_COLR";
            //MTK-END SS

            //MTK-START SIM ME lock
            case RIL_REQUEST_QUERY_SIM_NETWORK_LOCK: return "QUERY_SIM_NETWORK_LOCK";
            case RIL_REQUEST_SET_SIM_NETWORK_LOCK: return "SET_SIM_NETWORK_LOCK";
            //MTK-END SIM ME lock
            //ISIM
            case RIL_REQUEST_GENERAL_SIM_AUTH: return "RIL_REQUEST_GENERAL_SIM_AUTH";
            case RIL_REQUEST_OPEN_ICC_APPLICATION: return "RIL_REQUEST_OPEN_ICC_APPLICATION";
            case RIL_REQUEST_GET_ICC_APPLICATION_STATUS:
                return "RIL_REQUEST_GET_ICC_APPLICATION_STATUS";
            case RIL_REQUEST_SIM_IO_EX: return "SIM_IO_EX";

            // PHB Start
            case RIL_REQUEST_QUERY_PHB_STORAGE_INFO: return "RIL_REQUEST_QUERY_PHB_STORAGE_INFO";
            case RIL_REQUEST_WRITE_PHB_ENTRY: return "RIL_REQUEST_WRITE_PHB_ENTRY";
            case RIL_REQUEST_READ_PHB_ENTRY: return "RIL_REQUEST_READ_PHB_ENTRY";
            case RIL_REQUEST_QUERY_UPB_CAPABILITY: return "RIL_REQUEST_QUERY_UPB_CAPABILITY";
            case RIL_REQUEST_EDIT_UPB_ENTRY: return "RIL_REQUEST_EDIT_UPB_ENTRY";
            case RIL_REQUEST_DELETE_UPB_ENTRY: return "RIL_REQUEST_DELETE_UPB_ENTRY";
            case RIL_REQUEST_READ_UPB_GAS_LIST: return "RIL_REQUEST_READ_UPB_GAS_LIST";
            case RIL_REQUEST_READ_UPB_GRP: return "RIL_REQUEST_READ_UPB_GRP";
            case RIL_REQUEST_WRITE_UPB_GRP: return "RIL_REQUEST_WRITE_UPB_GRP";
            case RIL_REQUEST_GET_PHB_STRING_LENGTH: return "RIL_REQUEST_GET_PHB_STRING_LENGTH";
            case RIL_REQUEST_GET_PHB_MEM_STORAGE: return "RIL_REQUEST_GET_PHB_MEM_STORAGE";
            case RIL_REQUEST_SET_PHB_MEM_STORAGE: return "RIL_REQUEST_SET_PHB_MEM_STORAGE";
            case RIL_REQUEST_READ_PHB_ENTRY_EXT: return "RIL_REQUEST_READ_PHB_ENTRY_EXT";
            case RIL_REQUEST_WRITE_PHB_ENTRY_EXT: return "RIL_REQUEST_WRITE_PHB_ENTRY_EXT";
            // PHB End

            /* M: network part start */
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT:
                return "SET_NETWORK_SELECTION_MANUAL_WITH_ACT";
            case RIL_REQUEST_GET_POL_CAPABILITY: return "RIL_REQUEST_GET_POL_CAPABILITY";
            case RIL_REQUEST_GET_POL_LIST: return "RIL_REQUEST_GET_POL_LIST";
            case RIL_REQUEST_SET_POL_ENTRY: return "RIL_REQUEST_SET_POL_ENTRY";
            case RIL_REQUEST_SET_TRM: return "RIL_REQUEST_SET_TRM";
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS_WITH_ACT:
                return "QUERY_AVAILABLE_NETWORKS_WITH_ACT";
            //Femtocell (CSG) feature START
            case RIL_REQUEST_GET_FEMTOCELL_LIST: return "RIL_REQUEST_GET_FEMTOCELL_LIST";
            case RIL_REQUEST_ABORT_FEMTOCELL_LIST: return "RIL_REQUEST_ABORT_FEMTOCELL_LIST";
            case RIL_REQUEST_SELECT_FEMTOCELL: return "RIL_REQUEST_SELECT_FEMTOCELL";
            //Femtocell (CSG) feature END
            /* M: network part end */
            case RIL_REQUEST_STK_EVDL_CALL_BY_AP: return "RIL_REQUEST_STK_EVDL_CALL_BY_AP";
            case RIL_REQUEST_QUERY_MODEM_TYPE: return "RIL_REQUEST_QUERY_MODEM_TYPE";
            case RIL_REQUEST_STORE_MODEM_TYPE: return "RIL_REQUEST_STORE_MODEM_TYPE";
            case RIL_REQUEST_SIM_GET_ATR: return "SIM_GET_ATR";
            case RIL_REQUEST_SIM_OPEN_CHANNEL_WITH_SW: return "SIM_OPEN_CHANNEL_WITH_SW";

            // IMS
            case RIL_REQUEST_SET_IMS_ENABLE: return "RIL_REQUEST_SET_IMS_ENABLE";

            // M: Fast Dormancy
            case RIL_REQUEST_SET_SCRI: return "RIL_REQUEST_SET_SCRI";
            case RIL_REQUEST_SET_FD_MODE: return "RIL_REQUEST_SET_FD_MODE";
            // MTK-START, SMS part
            case RIL_REQUEST_GET_SMS_PARAMS: return "RIL_REQUEST_GET_SMS_PARAMS";
            case RIL_REQUEST_SET_SMS_PARAMS: return "RIL_REQUEST_SET_SMS_PARAMS";
            case RIL_REQUEST_GET_SMS_SIM_MEM_STATUS: return "RIL_REQUEST_GET_SMS_SIM_MEM_STATUS";
            case RIL_REQUEST_SET_ETWS: return "RIL_REQUEST_SET_ETWS";
            case RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO:
                return "RIL_REQUEST_SET_CB_CHANNEL_CONFIG_INFO";
            case RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO:
                return "RIL_REQUEST_SET_CB_LANGUAGE_CONFIG_INFO";
            case RIL_REQUEST_GET_CB_CONFIG_INFO: return "RIL_REQUEST_GET_CB_CONFIG_INFO";
            case RIL_REQUEST_REMOVE_CB_MESSAGE: return "RIL_REQUEST_REMOVE_CB_MESSAGE";
            // MTK-END, SMS part
            case RIL_REQUEST_SET_DATA_CENTRIC: return "RIL_REQUEST_SET_DATA_CENTRIC";

            case RIL_REQUEST_MODEM_POWEROFF: return "MODEM_POWEROFF";
            case RIL_REQUEST_MODEM_POWERON: return "MODEM_POWERON";
            // M: CC33 LTE.
            case RIL_REQUEST_SET_DATA_ON_TO_MD: return "RIL_REQUEST_SET_DATA_ON_TO_MD";
            case RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE:
                return "RIL_REQUEST_SET_REMOVE_RESTRICT_EUTRAN_MODE";
            case RIL_REQUEST_BTSIM_CONNECT: return "RIL_REQUEST_BTSIM_CONNECT";
            case RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF:
                return "RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF";
            case RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM:
                return "RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM";
            case RIL_REQUEST_BTSIM_TRANSFERAPDU: return "RIL_REQUEST_SEND_BTSIM_TRANSFERAPDU";

            /// M: IMS VoLTE conference dial feature. @{
            case RIL_REQUEST_CONFERENCE_DIAL: return "RIL_REQUEST_CONFERENCE_DIAL";
            /// @}
            case RIL_REQUEST_RELOAD_MODEM_TYPE: return "RIL_REQUEST_RELOAD_MODEM_TYPE";
            /// M: CC010: Add RIL interface @{
            case RIL_REQUEST_SET_IMS_CALL_STATUS: return "RIL_REQUEST_SET_IMS_CALL_STATUS";
            /// @}

            /// M: CC072: Add Customer proprietary-IMS RIL interface. @{
            case RIL_REQUEST_SET_SRVCC_CALL_CONTEXT_TRANSFER:
                return "RIL_REQUEST_SET_SRVCC_CALL_CONTEXT_TRANSFER";
            case RIL_REQUEST_UPDATE_IMS_REGISTRATION_STATUS:
                return "RIL_REQUEST_UPDATE_IMS_REGISTRATION_STATUS";
            /// @}

            /// M: SVLTE remote SIM access feature
            case RIL_REQUEST_CONFIG_MODEM_STATUS: return "RIL_REQUEST_CONFIG_MODEM_STATUS";
            /* M: C2K part start */
            case RIL_REQUEST_GET_NITZ_TIME: return "RIL_REQUEST_GET_NITZ_TIME";
            case RIL_REQUEST_QUERY_UIM_INSERTED: return "RIL_REQUEST_QUERY_UIM_INSERTED";
            case RIL_REQUEST_SWITCH_HPF: return "RIL_REQUEST_SWITCH_HPF";
            case RIL_REQUEST_SET_AVOID_SYS: return "RIL_REQUEST_SET_AVOID_SYS";
            case RIL_REQUEST_QUERY_AVOID_SYS: return "RIL_REQUEST_QUERY_AVOID_SYS";
            case RIL_REQUEST_QUERY_CDMA_NETWORK_INFO: return "RIL_REQUEST_QUERY_CDMA_NETWORK_INFO";
            case RIL_REQUEST_GET_LOCAL_INFO: return "RIL_REQUEST_GET_LOCAL_INFO";
            case RIL_REQUEST_UTK_REFRESH: return "RIL_REQUEST_UTK_REFRESH";
            case RIL_REQUEST_QUERY_SMS_AND_PHONEBOOK_STATUS:
                return "RIL_REQUEST_QUERY_SMS_AND_PHONEBOOK_STATUS";
            case RIL_REQUEST_QUERY_NETWORK_REGISTRATION:
                return "RIL_REQUEST_QUERY_NETWORK_REGISTRATION";
            case RIL_REQUEST_AGPS_TCP_CONNIND: return "RIL_REQUEST_AGPS_TCP_CONNIND";
            case RIL_REQUEST_AGPS_SET_MPC_IPPORT: return "RIL_REQUEST_AGPS_SET_MPC_IPPORT";
            case RIL_REQUEST_AGPS_GET_MPC_IPPORT: return "RIL_REQUEST_AGPS_GET_MPC_IPPORT";
            case RIL_REQUEST_SET_MEID: return "RIL_REQUEST_SET_MEID";
            case RIL_REQUEST_SET_ETS_DEV: return "RIL_REQUEST_SET_ETS_DEV";
            case RIL_REQUEST_WRITE_MDN: return "RIL_REQUEST_WRITE_MDN";
            case RIL_REQUEST_SET_VIA_TRM: return "RIL_REQUEST_SET_VIA_TRM";
            case RIL_REQUEST_SET_ARSI_THRESHOLD: return "RIL_REQUEST_SET_ARSI_THRESHOLD";
            case RIL_REQUEST_QUERY_UTK_MENU_FROM_MD: return "RIL_REQUEST_QUERY_UTK_MENU_FROM_MD";
            case RIL_REQUEST_QUERY_STK_MENU_FROM_MD: return "RIL_REQUEST_QUERY_STK_MENU_FROM_MD";
            /* M: C2K part end */
            // M: [C2K][MD IRAT]RIL
            case RIL_REQUEST_SET_ACTIVE_PS_SLOT: return "RIL_REQUEST_SET_ACTIVE_PS_SLOT";
            case RIL_REQUEST_CONFIRM_INTER_3GPP_IRAT_CHANGE:
                return "RIL_REQUEST_CONFIRM_INTER_3GPP_IRAT_CHANGE";
            case RIL_REQUEST_DEACTIVATE_LINK_DOWN_PDN:
                return "RIL_REQUEST_DEACTIVATE_LINK_DOWN_PDN";
            /// @}
            /// M: [C2K][SVLTE] Set the SVLTE RAT mode. @{
            case RIL_REQUEST_SET_SVLTE_RAT_MODE: return "RIL_REQUEST_SET_SVLTE_RAT_MODE";
            /// M: [C2K][SVLTE] Set the SVLTE RAT mode. @}

            /// M: [C2K][IR] Support SVLTE IR feature. @{
            case RIL_REQUEST_SET_REG_SUSPEND_ENABLED: return "RIL_REQUEST_SET_REG_SUSPEND_ENABLED";
            case RIL_REQUEST_RESUME_REGISTRATION: return "RIL_REQUEST_RESUME_REGISTRATION";
            case RIL_REQUEST_SET_REG_SUSPEND_ENABLED_CDMA:
                return "RIL_REQUEST_SET_REG_SUSPEND_ENABLED_CDMA";
            case RIL_REQUEST_RESUME_REGISTRATION_CDMA:
                return "RIL_REQUEST_RESUME_REGISTRATION_CDMA";
            case RIL_REQUEST_CONFIG_EVDO_MODE:
                return "RIL_REQUEST_CONFIG_EVDO_MODE";
            /// M: [C2K][IR] Support SVLTE IR feature. @}

            case RIL_REQUEST_SET_STK_UTK_MODE:
                return "RIL_REQUEST_SET_STK_UTK_MODE";

            // M: Notify RILJ that call fade happened
            case RIL_UNSOL_CDMA_SIGNAL_FADE:
                return "RIL_UNSOL_CDMA_SIGNAL_FADE";
            // M: Notify RILJ that the AT+EFNM was received
            case RIL_UNSOL_CDMA_TONE_SIGNALS:
                return "RIL_UNSOL_CDMA_TONE_SIGNALS";

            case RIL_REQUEST_SWITCH_ANTENNA: return "RIL_REQUEST_SWITCH_ANTENNA";
            /// M: IMS ViLTE feature. @{
            case RIL_REQUEST_VIDEO_CALL_ACCEPT: return "RIL_REQUEST_VIDEO_CALL_ACCEPT";
            /// @}
            default: return "<unknown request>";
        }

    }

    static String retToString(int req, Object ret) {
        if (ret == null) return "";

        StringBuilder sb;
        String s;
        int length;
        if (ret instanceof int[]) {
            int[] intArray = (int[]) ret;
            length = intArray.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(intArray[i++]);
                while (i < length) {
                    sb.append(", ").append(intArray[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else if (ret instanceof String[]) {
            String[] strings = (String[]) ret;
            length = strings.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(strings[i++]);
                while (i < length) {
                    sb.append(", ").append(strings[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else {
            s = ret.toString();
        }
        return s;
    }

    private void riljLog(String msg) {
        Rlog.d(IMS_RILA_LOG_TAG, msg
                + "");
    }

    private void riljLogv(String msg) {
        Rlog.v(IMS_RILA_LOG_TAG, msg
                + "");
    }

    private void unsljLog(int response) {
        riljLog("[UNSL]< " + responseToString(response));
    }

    private void unsljLogMore(int response, String more) {
        riljLog("[UNSL]< " + responseToString(response) + " " + more);
    }

    private void unsljLogRet(int response, Object ret) {
        riljLog("[UNSL]< " + responseToString(response) + " " + retToString(response, ret));
    }

    private void unsljLogvRet(int response, Object ret) {
        riljLogv("[UNSL]< " + responseToString(response) + " " + retToString(response, ret));
    }

    private void processUnsolicited(Parcel p) {
        Rlog.i(IMS_RILA_LOG_TAG, " IMS processUnsolicited !!");
                int response;
                Object ret;
                response = p.readInt();
                try { switch(response) {
                    case RIL_UNSOL_CALL_INFO_INDICATION : ret = responseStrings(p); break;
                    case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED: ret =  responseVoid(p); break;
                    case RIL_UNSOL_IMS_REGISTRATION_INFO: ret = responseInts(p); break;
                    case RIL_UNSOL_INCOMING_CALL_INDICATION: ret = responseStrings(p); break;
                    case RIL_UNSOL_RINGBACK_TONE: ret = responseInts(p); break;
                    case RIL_UNSOL_CIPHER_INDICATION: ret = responseStrings(p); break;
                    case RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_SUPPORT: ret = responseInts(p); break;
                    case RIL_UNSOL_ECONF_SRVCC_INDICATION: ret = responseInts(p); break;
                    case RIL_UNSOL_ECONF_RESULT_INDICATION: ret = responseStrings(p); break;
                    case RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_INFO: ret = responseInts(p); break;
                    case RIL_UNSOL_SRVCC_HANDOVER_INFO_INDICATION:ret = responseInts(p); break;
                    case RIL_UNSOL_SPEECH_CODEC_INFO: ret =  responseInts(p); break;
                    case RIL_UNSOL_CALL_RING: ret =  responseCallRing(p); break;
                    case RIL_UNSOL_SUPP_SVC_NOTIFICATION: ret = responseSuppServiceNotification(p); break;
                    case RIL_UNSOL_SRVCC_STATE_NOTIFY: ret = responseInts(p); break;
                    case RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR: ret = responseInts(p); break;
                    // IMS
                    case RIL_UNSOL_IMS_ENABLE_DONE: ret = responseVoid(p); break;
                    case RIL_UNSOL_IMS_DISABLE_DONE: ret = responseVoid(p); break;
                    case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED: ret =  responseVoid(p); break;
                    // IMS
                    case RIL_UNSOL_IMS_ENABLE_START: ret = responseVoid(p); break;
                    case RIL_UNSOL_IMS_DISABLE_START: ret = responseVoid(p); break;
                    // For ViLTE related URC.
                    case RIL_UNSOL_CALLMOD_CHANGE_INDICATOR: ret = responseStrings(p); break;
                    case RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR: ret = responseStrings(p); break;
                    default:
                        throw new RuntimeException("Unrecognized unsol response: " + response);
                    //break; (implied)
                } } catch (Throwable tr) {
                    Rlog.e(IMS_RILA_LOG_TAG, "Exception processing unsol response: " + response +
                        "Exception:" + tr.toString());
                    return;
                }

                switch(response) {
                    case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
                        /* has bonus radio state int */
                        RadioState newState = getRadioStateFromInt(p.readInt());
                        if (IMS_RILA_LOGD) unsljLogMore(response, newState.toString());

                        switchToRadioState(newState);
                    break;
                    //For updating call mode and pau information.
                    case RIL_UNSOL_CALL_INFO_INDICATION:
                        if (ret == null) {
                            break;
                        }
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mCallInfoRegistrants != null) {
                            mCallInfoRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
                        }

                        String[] callInfo = (String[]) ret;

                        if (callInfo[0] == null || callInfo[1] == null) {
                            riljLog("RIL_UNSOL_CALL_INFO_INDICATION something wrong");
                            break;
                        }

                        int msgType = Integer.parseInt(callInfo[1]);
                        int callId = Integer.parseInt(callInfo[0]);
                        int callMode = INVALID_CALL_MODE;
                        boolean isConferenceCall = false;
                        if ((callInfo[5] != null) && (!callInfo[5].equals(""))) {
                            callMode = Integer.parseInt(callInfo[5]);
                        }

                        if (callMode == IMS_VOICE_CONF ||
                                callMode == IMS_VIDEO_CONF ||
                                callMode == IMS_VOICE_CONF_PARTS ||
                                callMode == IMS_VIDEO_CONF_PARTS) {
                            isConferenceCall = true;
                        }

                        ImsCallInfo imsCallInfo;
                        switch(msgType) {
                            case 0:   //record callstring to mCallConnections for MT case.
                                {
                                    ImsCallInfo.State state = ImsCallInfo.State.INCOMING;
                                    mCallConnections.put(callInfo[0],
                                            new ImsCallInfo(callInfo[0], callInfo[6],
                                                    isConferenceCall, state));
                                    break;
                                }
                            case 130:   //record callstring to mCallConnections for MO case.
                                {
                                    ImsCallInfo.State state = ImsCallInfo.State.ALERTING;
                                    mCallConnections.put(callInfo[0],
                                            new ImsCallInfo(callInfo[0], callInfo[6],
                                                    isConferenceCall, state));
                                    break;
                                }
                            case 2:   //record callstring to mCallConnections for MO case.
                                {
                                    imsCallInfo = mCallConnections.get(callInfo[0]);
                                    imsCallInfo.mIsConference = isConferenceCall;
                                    mCallConnections.put(callInfo[0], imsCallInfo);
                                    break;
                                }
                            case 131:
                                {
                                    imsCallInfo = mCallConnections.get(callInfo[0]);
                                    imsCallInfo.mState = ImsCallInfo.State.HOLDING;
                                    imsCallInfo.mIsConference = isConferenceCall;
                                    mCallConnections.put(callInfo[0], imsCallInfo);
                                    break;
                                }
                            case 132:
                                {
                                    imsCallInfo = mCallConnections.get(callInfo[0]);
                                    imsCallInfo.mState = ImsCallInfo.State.ACTIVE;
                                    imsCallInfo.mIsConference = isConferenceCall;
                                    mCallConnections.put(callInfo[0], imsCallInfo);
                                    break;
                                }
                            case 133: //clear callstring  when call is disconnected.
                                mCallConnections.remove(callInfo[0]);
                                break;
                            default:
                                break;
                        }
                        break;
                    case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mCallStateRegistrants != null) {
                            mCallStateRegistrants
                                    .notifyRegistrants(new AsyncResult(null, null, null));
                        }
                        break;
                    case RIL_UNSOL_IMS_REGISTRATION_INFO:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mImsRegistrationInfoRegistrants != null) {
                            mImsRegistrationInfoRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_INCOMING_CALL_INDICATION:
                        if (IMS_RILA_LOGD) unsljLogvRet(response, ret);
                        if (mIncomingCallIndicationRegistrant != null) {
                            mIncomingCallIndicationRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_RINGBACK_TONE:
                        if (IMS_RILA_LOGD) unsljLogvRet(response, ret);
                        if (mRingbackToneRegistrants != null) {
                            boolean playtone = (((int[]) ret)[0] == 1);
                            mRingbackToneRegistrants.notifyRegistrants(
                                                new AsyncResult(null, playtone, null));
                        }
                        break;
                    case RIL_UNSOL_CIPHER_INDICATION:
                        if (IMS_RILA_LOGD) unsljLogvRet(response, ret);

                        int simCipherStatus = Integer.parseInt(((String[]) ret)[0]);
                        int sessionStatus = Integer.parseInt(((String[]) ret)[1]);
                        int csStatus = Integer.parseInt(((String[]) ret)[2]);
                        int psStatus = Integer.parseInt(((String[]) ret)[3]);

                        riljLog("RIL_UNSOL_CIPHER_INDICATION :" + simCipherStatus + " " + sessionStatus + " " + csStatus + " " + psStatus);

                        int[] cipherResult = new int[3];

                        cipherResult[0] = simCipherStatus;
                        cipherResult[1] = csStatus;
                        cipherResult[2] = psStatus;

                        if (mCipherIndicationRegistrant != null) {
                            mCipherIndicationRegistrant.notifyRegistrants(
                                new AsyncResult(null, cipherResult, null));
                        }
                        break;
                    case RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_SUPPORT:
                        if (IMS_RILA_LOGD) unsljLogvRet(response, ret);
                        if (mEpsNetworkFeatureSupportRegistrants != null) {
                            mEpsNetworkFeatureSupportRegistrants.notifyRegistrants(
                                                new AsyncResult(null, ret, null));
                        }
                        break;
                    /// M: IMS feature. @{
                    //For updating call ids for conference call after SRVCC is done.
                    case RIL_UNSOL_ECONF_SRVCC_INDICATION:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mEconfSrvccRegistrants != null) {
                            mEconfSrvccRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;

                    //For updating conference call merged/added result.
                    case RIL_UNSOL_ECONF_RESULT_INDICATION:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mEconfResultRegistrants != null) {
                             riljLog("Notify ECONF result");
                             String[] econfResult = (String[]) ret;
                             riljLog("ECONF result = " + econfResult[3]);
                             mEconfResultRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_VOLTE_EPS_NETWORK_FEATURE_INFO:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mEpsNetworkFeatureInfoRegistrants != null) {
                           mEpsNetworkFeatureInfoRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;

                    case RIL_UNSOL_SRVCC_HANDOVER_INFO_INDICATION:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mSrvccHandoverInfoIndicationRegistrants != null) {
                            mSrvccHandoverInfoIndicationRegistrants.notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    /// @}
                    case RIL_UNSOL_SPEECH_CODEC_INFO:
                        if (IMS_RILA_LOGD) unsljLogvRet(response, ret);

                        if (mSpeechCodecInfoRegistrant != null) {
                            mSpeechCodecInfoRegistrant.notifyRegistrant(
                                new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_CALL_RING:
                        Rlog.d(IMS_RILA_LOG_TAG, "IMS: receive RIL_UNSOL_CALL_RING");
                        if (IMS_RILA_LOGD) unsljLogRet(response, ret);

                        if (mRingRegistrant != null) {
                            mRingRegistrant.notifyRegistrant(
                                    new AsyncResult (null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_SUPP_SVC_NOTIFICATION:
                        if (IMS_RILA_LOGD) unsljLogRet(response, ret);
                        if (mSsnRegistrant != null) {
                            mSsnRegistrant.notifyRegistrant(
                                                new AsyncResult (null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_SRVCC_STATE_NOTIFY:
                        if (IMS_RILA_LOGD) unsljLogRet(response, ret);
                        if (mSrvccStateRegistrants != null) {
                            mSrvccStateRegistrants
                                    .notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_SIP_CALL_PROGRESS_INDICATOR:
                        if (IMS_RILA_LOGD) unsljLogRet(response, ret);
                        if (mCallProgressIndicatorRegistrants != null) {
                            mCallProgressIndicatorRegistrants
                                    .notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_IMS_ENABLE_DONE:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mImsEnableDoneRegistrants != null) {
                            mImsEnableDoneRegistrants.notifyRegistrants();
                        }
                        break;
                    case RIL_UNSOL_IMS_DISABLE_DONE:
                        if (IMS_RILA_LOGD) unsljLog(response);
                        if (mImsDisableDoneRegistrants != null) {
                            mImsDisableDoneRegistrants.notifyRegistrants();
                        }
                        break;
                    case RIL_UNSOL_IMS_ENABLE_START:
                        if (IMS_RILA_LOGD) {
                            unsljLog(response);
                        }
                        if (mImsEnableStartRegistrants != null) {
                            mImsEnableStartRegistrants.notifyRegistrants();
                        }
                        break;
                    case RIL_UNSOL_IMS_DISABLE_START:
                        if (IMS_RILA_LOGD) {
                            unsljLog(response);
                        }
                        if (mImsDisableStartRegistrants != null) {
                            mImsDisableStartRegistrants.notifyRegistrants();
                        }
                        break;
                    case RIL_UNSOL_CALLMOD_CHANGE_INDICATOR:
                        if (IMS_RILA_LOGD) {
                            unsljLogRet(response, ret);
                        }
                        if (mCallModeChangeIndicatorRegistrants != null) {
                            mCallModeChangeIndicatorRegistrants
                                    .notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    case RIL_UNSOL_VIDEO_CAPABILITY_INDICATOR:
                        if (IMS_RILA_LOGD) {
                            unsljLogRet(response, ret);
                        }
                        if (mVideoCapabilityIndicatorRegistrants != null) {
                            mVideoCapabilityIndicatorRegistrants
                                    .notifyRegistrants(new AsyncResult(null, ret, null));
                        }
                        break;
                    default:
                        break;
                }

    }

    private synchronized String executeCommandResponse(String atCmdLine) {
        String atCmdResult = "";
        String cmd[] = new String[2];
        cmd[0] = atCmdLine;
        cmd[1] = "";

        Rlog.d(IMS_RILA_LOG_TAG, "IMS: invokeOemRilRequestRaw() ");

        try {
            invokeOemRilRequestStrings(cmd,
                mHandler.obtainMessage(EVENT_AT_CMD_DONE));
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return atCmdResult;
    }

    private void handleAtCmdResponseAndDial(AsyncResult ar) {
        // Receive OEM Hook Raw response, then dial
        if (mMoCall == null && mConferenceCallDialInfo == null) {
            Rlog.e(IMS_RILA_LOG_TAG, "IMS: mMoCall is null when calling");
            return;
        }


        if (mConferenceCallDialInfo != null) {
            conferenceDial(mConferenceCallDialInfo.mParticipants,
                    mConferenceCallDialInfo.mClirMode,
                    mConferenceCallDialInfo.mIsVideoCall,
                    mConferenceCallDialInfo.mResult);
        } else {
            if (mMoCall.mIsVideoCall) {
                vtDial(mMoCall.mCallee, mMoCall.mClirMode, null, mMoCall.mResult);
            } else {
                if (mMoCall.mIsEmergency) {
                    int serviceCategory =
                        PhoneNumberUtils.getServiceCategoryFromEcc(mMoCall.mCallee);
                    setEccServiceCategory(serviceCategory);
                    emergencyDial(mMoCall.mCallee, mMoCall.mClirMode,
                        null, mMoCall.mResult);
                } else {
                    dial(mMoCall.mCallee, mMoCall.mClirMode, mMoCall.mResult);
                }
            }
        }

        // Clear MOCall or ConferenceCall information
        mMoCall = null;
        mConferenceCallDialInfo = null;
    }

    /// M: CC053: MoMS [Mobile Managerment] @{
    // 3. Permission Control for Conference call
    /**
    * To check sub-permission for MoMS before using API.
    *
    * @param subPermission  The permission to be checked.
    *
    * @return Return true if the permission is granted else return false.
    */
    private boolean checkMoMSSubPermission(String subPermission) {

        try {
            IMobileManagerService mMobileManager;
            IBinder binder = ServiceManager.getService(Context.MOBILE_SERVICE);
            mMobileManager = IMobileManagerService.Stub.asInterface(binder);
            int result = mMobileManager.checkPermission(subPermission, Binder.getCallingUid());
            if (result != PackageManager.PERMISSION_GRANTED) {
                riljLog("[Error]Subpermission is not granted!!");
                return false;
            }
        } catch (Exception e) {
            riljLog("[Error]Failed to chcek permission: " +  subPermission);
            return false;
        }

        return true;
    }
    /// @}

    /// M: CC012: DTMF request special handling @{
    /*
     * to protect modem status we need to avoid two case :
     * 1. DTMF start -> CHLD request -> DTMF stop
     * 2. CHLD request -> DTMF request
     */
    private void handleChldRelatedRequest(RILRequest rr) {
        synchronized (mDtmfReqQueue) {
            int queueSize = mDtmfReqQueue.size();
            int i, j;
            if (queueSize > 0) {
                RILRequest rr2 = mDtmfReqQueue.get();
                if (rr2.mRequest == RIL_REQUEST_DTMF_START) {
                    // need to send the STOP command
                    if (IMS_RILA_LOGD) {
                        riljLog("DTMF queue isn't 0, send stop dtmf and pending switch");
                    }
                    if (queueSize > 1) {
                        j = 2;
                    } else {
                        // need to create a new STOP command
                        j = 1;
                    }
                    if (IMS_RILA_LOGD) riljLog("queue size  " + mDtmfReqQueue.size());

                    for (i = queueSize - 1; i >= j; i--) {
                        mDtmfReqQueue.remove(i);
                    }
                    if (IMS_RILA_LOGD) {
                        riljLog("queue size  after " + mDtmfReqQueue.size());
                    }
                    if (mDtmfReqQueue.size() == 1) {
                        // only start command, we need to add stop command
                        RILRequest rr3 = RILRequest.obtain(RIL_REQUEST_DTMF_STOP, null);
                        if (IMS_RILA_LOGD) riljLog("add dummy stop dtmf request");
                        mDtmfReqQueue.stop();
                        mDtmfReqQueue.add(rr3);
                    }
                }
                else {
                    // first request is STOP, just remove it and send switch
                    if (IMS_RILA_LOGD) {
                        riljLog("DTMF queue isn't 0, first request is STOP, penging switch");
                    }
                    j = 1;
                    for (i = queueSize - 1; i >= j; i--) {
                        mDtmfReqQueue.remove(i);
                    }
                }
                mDtmfReqQueue.setPendingRequest(rr);
            } else {
                if (IMS_RILA_LOGD) riljLog("DTMF queue is 0, send switch Immediately");
                mDtmfReqQueue.setSendChldRequest();
                send(rr);
            }
        }
    }
    /// @}

    public void
    dial (String address, int clirMode, Message result) {
        dial(address, clirMode, null, result);
    }

    public void
    dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        if (!PhoneNumberUtils.isUriNumber(address)) {
           RILRequest rr = RILRequest.obtain(RIL_REQUEST_DIAL, result);

           rr.mParcel.writeString(address);
           rr.mParcel.writeInt(clirMode);

           if (uusInfo == null) {
              rr.mParcel.writeInt(0); // UUS information is absent
           } else {
              rr.mParcel.writeInt(1); // UUS information is present
              rr.mParcel.writeInt(uusInfo.getType());
              rr.mParcel.writeInt(uusInfo.getDcs());
              rr.mParcel.writeByteArray(uusInfo.getUserData());
           }

           if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

           send(rr);
        } else {
           RILRequest rr = RILRequest.obtain(RIL_REQUEST_DIAL_WITH_SIP_URI, result);

           rr.mParcel.writeString(address);
           if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
           send(rr);
        }
    }

    public void
    emergencyDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_EMERGENCY_DIAL, result);

        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);
        rr.mParcel.writeInt(0); // UUS information is absent

        if (uusInfo == null) {
            rr.mParcel.writeInt(0); // UUS information is absent
        } else {
            rr.mParcel.writeInt(1); // UUS information is present
            rr.mParcel.writeInt(uusInfo.getType());
            rr.mParcel.writeInt(uusInfo.getDcs());
            rr.mParcel.writeByteArray(uusInfo.getUserData());
        }

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    /* M: IMS VoLTE conference dial feature start*/
    /**
     * Dial conference call.
     * @param participants participants' dailing number.
     * @param clirMode indication to present the dialing number or not.
     * @param isVideoCall indicate the call is belong to video call or voice call.
     * @param result the command result.
     */
    public void
    conferenceDial(String[] participants, int clirMode, boolean isVideoCall, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_CONFERENCE_DIAL, result);

        int numberOfParticipants = participants.length;
        /* numberOfStrings is including
         * 1. isvideoCall
         * 2. numberofparticipants
         * 3. participants numbers
         * 4. clirmod
         */
        int numberOfStrings = 1 + 1 + numberOfParticipants + 1 ;
        List<String> participantList = Arrays.asList(participants);

        if (IMS_RILA_LOGD) {
            Rlog.d(IMS_RILA_LOG_TAG, "conferenceDial: numberOfParticipants "
                    + numberOfParticipants + "numberOfStrings:" + numberOfStrings);
        }

        rr.mParcel.writeInt(numberOfStrings);

        if (isVideoCall) {
            rr.mParcel.writeString(Integer.toString(1));
        } else {
            rr.mParcel.writeString(Integer.toString(0));
        }

        rr.mParcel.writeString(Integer.toString(numberOfParticipants));

        for (String dialNumber : participantList) {
            rr.mParcel.writeString(dialNumber);
            if (IMS_RILA_LOGD) {
                Rlog.d(IMS_RILA_LOG_TAG, "conferenceDial: dialnumber " + dialNumber);
            }
        }
        rr.mParcel.writeString(Integer.toString(clirMode));
        if (IMS_RILA_LOGD) {
            Rlog.d(IMS_RILA_LOG_TAG, "conferenceDial: clirMode " + clirMode);
        }

        if (IMS_RILA_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        }

        send(rr);

    }
    /* IMS VoLTE conference dial feature end*/

    public void setEccServiceCategory(int serviceCategory) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_ECC_SERVICE_CATEGORY, null);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(serviceCategory);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
            + " " + serviceCategory);

        send(rr);
    }

    /// M: For 3G VT only @{
    public void
    vtDial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_VT_DIAL, result);

        rr.mParcel.writeString(address);
        rr.mParcel.writeInt(clirMode);

        if (uusInfo == null) {
            rr.mParcel.writeInt(0); // UUS information is absent
        } else {
            rr.mParcel.writeInt(1); // UUS information is present
            rr.mParcel.writeInt(uusInfo.getType());
            rr.mParcel.writeInt(uusInfo.getDcs());
            rr.mParcel.writeByteArray(uusInfo.getUserData());
        }

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }

    public void
    acceptVtCallWithVoiceOnly(int callId, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_VOICE_ACCEPT, result);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " +
                requestToString(rr.mRequest) + " " + callId);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(callId);

        send(rr);
    }

    public void replaceVtCall(int index, Message result) {
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_REPLACE_VT_CALL, result);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(index);

        if (IMS_RILA_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }
    /// @}

    private void switchToRadioState(RadioState newState) {
        setRadioState(newState);
    }

    private RadioState getRadioStateFromInt(int stateInt) {
        RadioState state;

        /* RIL_RadioState ril.h */
        switch(stateInt) {
            case 0: state = RadioState.RADIO_OFF; break;
            case 1: state = RadioState.RADIO_UNAVAILABLE; break;
            case 10: state = RadioState.RADIO_ON; break;

            default:
                throw new RuntimeException(
                            "Unrecognized IMS_RIL_RadioState: " + stateInt);
        }
        return state;
    }

}
