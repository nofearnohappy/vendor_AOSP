/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
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

import android.net.Network;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.PowerManager.WakeLock;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallForwardInfoEx;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyProperties;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mediatek.gba.GbaHttpUrlCredential;
import com.mediatek.simservs.client.SimServs;
import com.mediatek.simservs.client.CommunicationDiversion;
import com.mediatek.simservs.client.CommunicationWaiting;
import com.mediatek.simservs.client.IncomingCommunicationBarring;
import com.mediatek.simservs.client.SimservType;
import com.mediatek.simservs.client.OriginatingIdentityPresentation;
import com.mediatek.simservs.client.OriginatingIdentityPresentationRestriction;
import com.mediatek.simservs.client.TerminatingIdentityPresentation;
import com.mediatek.simservs.client.TerminatingIdentityPresentationRestriction;

import com.mediatek.simservs.client.OutgoingCommunicationBarring;
import com.mediatek.simservs.client.policy.Actions;
import com.mediatek.simservs.client.policy.Conditions;
import com.mediatek.simservs.client.policy.ForwardTo;
import com.mediatek.simservs.client.policy.Rule;
import com.mediatek.simservs.client.policy.RuleSet;
import com.mediatek.simservs.xcap.XcapException;
import com.mediatek.xcap.client.uri.XcapUri;

/// For OP01 UT @{
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
/// @}

/**
 * {@hide}
 */
class MMTelSSRequest {
    static final String LOG_TAG = "MMTelSSReq";

    //***** Class Variables
    static int sNextSerial = 0;
    static Object sSerialMonitor = new Object();
    private static Object sPoolSync = new Object();
    private static MMTelSSRequest sPool = null;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 4;

    //***** Instance Variables
    int mSerial;
    int mRequest;
    Message mResult;
    Parcel mp;
    //For complex SS Operation: It can be used to carry the Rule or RuleSet object for
    //MMTelSSTransmitter to parse & compare with remote XCAP server's data
    //Add by mtk01411 2013-0911
    Object requestParm;
    MMTelSSRequest mNext;

    /**
     * Retrieves a new MMTelSSRequest instance from the pool.
     *
     * @param request MMTELSS_REQ_*
     * @param result sent when operation completes
     * @return a MMTelSSRequest instance from the pool.
     */
    static MMTelSSRequest obtain(int request, Message result) {
        MMTelSSRequest rr = null;

        synchronized (sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }

        if (rr == null) {
            rr = new MMTelSSRequest();
        }

        synchronized (sSerialMonitor) {
            rr.mSerial = sNextSerial++;
        }
        rr.mRequest = request;
        rr.mResult = result;
        rr.mp = Parcel.obtain();

        if (result != null && result.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }

        // first elements in any MMTelSSRequest Parcel (Before returning the rr, it already
        // fills two elements into the Parcel)
        rr.mp.writeInt(request);
        rr.mp.writeInt(rr.mSerial);

        return rr;
    }

    /**
     * Returns a MMTelSSRequest instance to the pool.
     *
     * Note: This should only be called once per use.
     */
    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                mResult = null;
            }
        }
    }

    private MMTelSSRequest() {
    }

    static void
    resetSerial() {
        synchronized (sSerialMonitor) {
            sNextSerial = 0;
        }
    }

    String
    serialString() {
        //Cheesy way to do %04d
        StringBuilder sb = new StringBuilder(8);
        String sn;

        sn = Integer.toString(mSerial);

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

        //[TBD] It should modify as XCAP Errno & Exception by mtk01411
        ex = CommandException.fromRilErrno(error);

        if (MMTelSSTransport.DBG) Rlog.d(LOG_TAG, serialString() + "< "
                + MMTelSSTransport.requestToString(mRequest)
                + " error: " + ex);

        if (mResult != null) {
            AsyncResult.forMessage(mResult, ret, ex);
            mResult.sendToTarget();
        }

        if (mp != null) {
            mp.recycle();
            mp = null;
        }
    }
}


/**
 * Implementation for MMTel SS Transport
 *
 * {@hide}
 *
 */
public final class MMTelSSTransport {
    private static final String LOG_TAG = "MMTelSS";
    static final boolean DBG = true;

    // Singleton instance
    private static final MMTelSSTransport INSTANCE = new MMTelSSTransport();
    private PowerManager pm = null;
    HandlerThread mSenderThread;
    MMTelSSTransmitter mSender;
    String mMCC = "";
    String mMNC = "";
    String mXui = "user@chinaTel.com";
    String mXcapRoot = "http://192.168.1.2:8080/";
    String mXIntendedId = "user@chinaTel.com";
    String mUserName = "sip:user@anritsu-cscf.com";
    //[Modify mUserName as Single_TC_xxx by mtk01411 for UT - get local xml tested string instead
    // of getting from XCAP server via http]
    //String mUserName="Single_TC_1";
    String mPassword = "password";
    Context mContext = null;
    private XcapMobileDataNetworkManager mXcapMobileDataNetworkManager = null;
    private Network mNetwork = null;

    //***** MMTelSSRequest
    static final int MMTELSS_REQ_SET_CLIR               = 1;
    static final int MMTELSS_REQ_GET_CLIR               = 2;
    static final int MMTELSS_REQ_GET_CLIP               = 3;
    static final int MMTELSS_REQ_GET_COLP               = 4;
    static final int MMTELSS_REQ_GET_COLR               = 5;
    static final int MMTELSS_REQ_SET_CB                 = 6;
    static final int MMTELSS_REQ_GET_CB                 = 7;
    static final int MMTELSS_REQ_SET_CF                 = 8;
    static final int MMTELSS_REQ_GET_CF                 = 9;
    static final int MMTELSS_REQ_SET_CW                 = 10;
    static final int MMTELSS_REQ_GET_CW                 = 11;
    //[SET OIP/SET TIP/SET TIR are not supported by 2/3G SS feature set]
    static final int MMTELSS_REQ_SET_CLIP               = 12;
    static final int MMTELSS_REQ_SET_COLP               = 13;
    static final int MMTELSS_REQ_SET_COLR               = 14;
    /// For OP01 UT @{
    static final int MMTELSS_REQ_SET_CF_TIME_SLOT       = 15;
    static final int MMTELSS_REQ_GET_CF_TIME_SLOT       = 16;
    /// @}

    //***** Events
    static final int EVENT_SEND                 = 1;
    static final int EVENT_WAKE_LOCK_TIMEOUT    = 2;
    //static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 3;
    //static final int EVENT_RADIO_AVAILABLE = 4;
    //static final int EVENT_RADIO_ON = 5;

    //[TBD] Need to sync the maximun number of bytes with SimServs's capability
    static final int MMTELSS_MAX_COMMAND_BYTES = (8 * 1024);

    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 5000;

    WakeLock mWakeLock;
    int mWakeLockTimeout;
    // The number of requests pending to be sent out, it increases before calling
    // EVENT_SEND and decreases while handling EVENT_SEND. It gets cleared while
    // WAKE_LOCK_TIMEOUT occurs.
    int mRequestMessagesPending = 0;
    // The number of requests sent out but waiting for response. It increases while
    // sending request and decreases while handling response. It should match
    // mRequestList.size() unless there are requests no replied while
    // WAKE_LOCK_TIMEOUT occurs.
    int mRequestMessagesWaiting;

    //(1) mDisableRuleMode=1:Remove the rule when the user disables it (e.g., user disables the
    //  CFB/BAOC)
    //(2) mDisableRuleMode=2:Add <rule-deactivated> into the child node of <conditions> for CF/CB
    //  cases when the user disables it (e.g., user disables the CFB/BAOC)
    static final int DISABLE_MODE_DELETE_RULE = 1;
    static final int DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG = 2;
    //(3) mDisableRuleMode=3:Change <allow> from false(i.e, call is barred) to true(i.e., call is
    // allowed)
    static final int DISABLE_MODE_CHANGE_CB_ALLOW = 3;
    //Currently,it can decide the mDisableRuleMode from system property "ril.ss.disrulemode" - see
    // the following APIs usage
    //handleCreateNewRuleForExistingCF() and handleCreateNewRuleForExistingCB()
    int mDisableRuleMode = DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG;

    static final int RADIO_TEMPSTATE_AVAILABLE = 0;     /* Radio available */
    static final int RADIO_TEMPSTATE_UNAVAILABLE = 1;   /* Radio unavailable temporarily */
    private int radioTemporarilyUnavailable = RADIO_TEMPSTATE_AVAILABLE;

    //I'd rather this be LinkedList or something
    ArrayList<MMTelSSRequest> mRequestsList = new ArrayList<MMTelSSRequest>();

    private static final SimServs mSimservs = SimServs.getInstance();

    //[MMTelSS] For testing purpose (ref. to SimServsTest.java) Add by mtk01411 2013-0830
    static final private String XCAP_ROOT = "http://192.168.1.2:8080/";
    static final private String TEST_USER = "sip:user@anritsu-cscf.com";
    static final private String TEST_DOC = "simservs";

    //Following Constants definition must be same with EngineerMode/ims/ImsActivity.java
    private final static String PROP_SS_MODE = "persist.radio.ss.mode";
    private final static String MODE_SS_XCAP = "Prefer XCAP";
    private final static String MODE_SS_CS = "Prefer CS";
    private final static String PROP_SS_DISABLE_METHOD = "persist.radio.ss.xrdm";
    private final static String PROP_SS_CFNUM = "persist.radio.xcap.cfn";
    private final static String PROPERTY_CS_CURRENT_PHONE_ID = "gsm.radio.ss.phoneid";
    private boolean mUpdateSingleRule = true;
    private int mCsDomainPhoneId = 0;

    private CommunicationDiversion mCdCache = null;
    private int mCdCachePhoneId;
    private long mCdCacheLastQueried = 0;
    private OutgoingCommunicationBarring mOcbCache = null;
    private int mOcbCachePhoneId;
    private long mOcbCacheLastQueried = 0;
    private IncomingCommunicationBarring mIcbCache = null;
    private int mIcbCachePhoneId;
    private long mIcbCacheLastQueried = 0;
    private CommunicationWaiting mCwCache = null;
    private int mCwCachePhoneId;
    private long mCwCacheLastQueried = 0;
    private OriginatingIdentityPresentationRestriction mOirCache = null;
    private int mOirCachePhoneId;
    private long mOirCacheLastQueried = 0;
    private static final long MMTEL_CACHE_VALID_TIME = (2 * 60 * 1000);
    private static final int HTTP_ERROR_CODE_412 = 412;

    public MMTelSSTransport() {
        //pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        //mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        //mWakeLock.setReferenceCounted(false);
        //mWakeLockTimeout = SystemProperties.getInt(TelephonyProperties.PROPERTY_WAKE_LOCK_TIMEOUT,
        //        DEFAULT_WAKE_LOCK_TIMEOUT);

        mSenderThread = new HandlerThread("MMTelSSTransmitter");
        mSenderThread.start();
        Looper looper = mSenderThread.getLooper();
        mSender = new MMTelSSTransmitter(looper);
    }

    public static MMTelSSTransport getInstance() {
        return INSTANCE;
    }

    public static SimServs getSimServs() {
        return mSimservs;
    }

    public void registerUtService(Context context) {
        mContext = context;
        if (mWakeLock == null) {
            pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
            mWakeLock.setReferenceCounted(false);
            mWakeLockTimeout = SystemProperties.getInt(
                    TelephonyProperties.PROPERTY_WAKE_LOCK_TIMEOUT, DEFAULT_WAKE_LOCK_TIMEOUT);
        }

        // Manages XCAP mobile data network connectivity related stuff
        if (mXcapMobileDataNetworkManager == null) {
            mXcapMobileDataNetworkManager = new XcapMobileDataNetworkManager(mContext);
        }
    }

    private void requestXcapNetwork(int phoneId) {
        Rlog.d(LOG_TAG, "requestXcapNetwork(): phoneId = " + phoneId
                + ", mXcapMobileDataNetworkManager = " + mXcapMobileDataNetworkManager);
        mNetwork = null;
        if (mXcapMobileDataNetworkManager != null) {
            mNetwork = mXcapMobileDataNetworkManager.acquireNetwork(phoneId);
        }
    }

    /**
     * Configure Simservs parameters.
     *
     * @param xui XUI String
     * @param xcapRoot XCAP Root URI
     * @param intendedId XIntended Id String
     * @param userName username
     * @param password password
     * @param phoneId phone index
     */
    public void setSimservsInitParameters(String xui, String xcapRoot, String intendedId,
            String userName, String password, int phoneId) {
        mXui = xui;
        mXcapRoot = xcapRoot;
        mXIntendedId = intendedId;
        mUserName = userName;
        mPassword = password;

        mSimservs.setXui(xui);
        mSimservs.setXcapRoot(MMTelSSUtils.addXcapRootPort(xcapRoot, phoneId));
        mSimservs.setIntendedId(intendedId);
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);

        //[MMTelSS UT Purpose]Get userName (i.e,)
        String tc_name = SystemProperties.get("ril.ss.tcname", "Empty");
        Rlog.d(LOG_TAG, "setSimservsInitParameters():tc_name=" + tc_name + ", passed userName="
                + userName);
        if (tc_name != null && tc_name.startsWith("Single_TC_")) {
            userName = tc_name;
            mUserName = userName;
        }

        //[MMTelSS] In setHttpCredential():UserName will be used as the mUsername value and in
        // SimservType.java's constructor():loadConfiguration(userName)
        //If the userName is started with the prefix "Single_TC":It will enter the test mode & use
        // the test cases written in SimservType.java
        Rlog.d(LOG_TAG, "persist.mtk.simserv.username:[" +
                SystemProperties.get("persist.mtk.simserv.username") + "]" +
                "persist.mtk.simserv.password:[" +
                SystemProperties.get("persist.mtk.simserv.password") + "]");

        if (SystemProperties.get("persist.mtk.simserv.username") != null &&
                !SystemProperties.get("persist.mtk.simserv.username").isEmpty() &&
                SystemProperties.get("persist.mtk.simserv.password") != null &&
                !SystemProperties.get("persist.mtk.simserv.password").isEmpty()) {
            //Modify for NSN Lab IOT: Support HTTP Digest
            //Example:username=sip:+18860000018@srnims3.srnnam.nsn-rdnet and password=ims123456
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            SystemProperties.get("persist.mtk.simserv.username"),
                            SystemProperties.get("persist.mtk.simserv.password").toCharArray());
                }
            });
        } else {
            GbaHttpUrlCredential gbaCredential =
                    new GbaHttpUrlCredential(mContext, xcapRoot, subId);
            if (mNetwork != null) {
                gbaCredential.setNetwork(mNetwork);
            }
            Authenticator.setDefault(gbaCredential.getAuthenticator());
        }
    }

    class MMTelSSTransmitter extends Handler implements Runnable {
        public MMTelSSTransmitter(Looper looper) {
            super(looper);
        }

        // Only allocated once
        byte[] dataLength = new byte[4];

        //***** Runnable implementation
        public void
        run() {
            //setup if needed
        }



        public boolean containSpecificMedia(List<String>mediaList, int serviceClass) {
            if (mediaList == null) return true;
            if (mediaList.size() == 0) return true;
            //[Note]Open Question:(1)For a voice call, it only has the "audio" media type
            // (2)For a video call, it only has the "video" media type or have both "audio+video"
            //Another implementation:(1)If (serviceCalss == CommandsInterface.SERVICE_CLASS_VOICE
            // && mediaList.size()==1 && mediaType=="audio") -> return true
            //                       (2)If (serviceCalss == CommandsInterface.SERVICE_CLASS_VIDEO
            // && mediaList.size()==2 && one meidaType is audio the other is video") -> return true
            for (int i = 0; i < mediaList.size(); i++) {
                String mediaType = mediaList.get(i);
                Rlog.d(LOG_TAG, "mediaType=" + mediaType + ",serviceClass=" + serviceClass);
                if (mediaType.equals("audio")
                        && (serviceClass == CommandsInterface.SERVICE_CLASS_VOICE
                        || serviceClass == CommandsInterface.SERVICE_CLASS_NONE)) {
                    return true;
                } else if (mediaType.equals("video")
                        && (serviceClass == CommandsInterface.SERVICE_CLASS_VIDEO
                        || serviceClass == CommandsInterface.SERVICE_CLASS_NONE)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasExtraMedia(List<String>mediaList, int serviceClass) {
            boolean found = false;
            found = containSpecificMedia(mediaList, serviceClass);
            if (found && (mediaList != null) && (mediaList.size() > 1)) {
                return true;
            } else {
                return false;
            }
        }

        public String getMediaType(int serviceClass) {
            if (serviceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                return "audio";
            } else if (serviceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                return "video";
            }
            return "";
        }

        public boolean isBAOC(Conditions cond, int serviceClass) {
            //If the <cp:conditions></cp:conditions> is empty, the result of <cp:condition> is
            // evaluated as true
            if (cond == null)
                return true;
            if (cond.comprehendInternational() == false
                    && cond.comprehendRoaming() == false
                    && containSpecificMedia(cond.getMedias(), serviceClass)) {
                return true;
            } else {
                return false;
            }
        }

        public boolean isBAIC(Conditions cond, int serviceClass) {
            //If the <cp:conditions></cp:conditions> is empty, the result of <cp:condition> is
            // evaluated as true
            if (cond == null)
                return true;
            if (cond.comprehendInternational() == false
                    && cond.comprehendRoaming() == false
                    && cond.comprehendAnonymous() == false
                    && containSpecificMedia(cond.getMedias(), serviceClass)) {
                return true;
            } else {
                return false;
            }
        }


        public void handleGetCLIR(MMTelSSRequest rr) {
            //See ril_ss.c's requestClirOperation() & CLIRListPreference.java
            //1(Permantently provisioned),3(Temporary presentation disallowed),
            //4(Temporary presentation allowed),0(CLIR not provisioned),2(network error)
            int presentation_mode = 1;
            int get_clir_result = CommandsInterface.CLIR_DEFAULT;
            int phoneId = rr.mp.readInt();

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleGetCLIR(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            //
            try {
                OriginatingIdentityPresentationRestriction oir;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleGetCLIR(): mOirCache = " + mOirCache + ", curTime = "
                        + curTime + ", mOirCacheLastQueried = " + mOirCacheLastQueried);
                if ((mOirCache != null) && (phoneId == mOirCachePhoneId) &&
                        (mOirCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleGetCLIR(): using ETAG mOirCache: " + mOirCache);
                    requestXcapNetwork(phoneId);
                    oir = mOirCache;
                    oir.setNetwork(mNetwork);
                    oir.refresh();
                    mOirCacheLastQueried = curTime;
                } else if ((mOirCache != null) && (phoneId == mOirCachePhoneId) &&
                        ((curTime >= mOirCacheLastQueried)
                        && ((curTime - mOirCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleGetCLIR(): using mOirCache: " + mOirCache);
                    oir = mOirCache;
                } else {
                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleGetCLIR(): XcapRoot = " + mXcapRoot);
                        if (null != rr.mResult) {
                            AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                            rr.mResult.sendToTarget();
                        }
                        return;
                    }

                    requestXcapNetwork(phoneId);

                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);
                    oir = mSimservs.getOriginatingIdentityPresentationRestriction(true, mNetwork);
                    mOirCache = oir;
                    mOirCachePhoneId = phoneId;
                    mOirCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleGetCLIR(): new mOirCache = " + mOirCache
                            + ", curTime = " + curTime);
                }

                boolean restricted = oir.isDefaultPresentationRestricted();
                if (restricted == true) {
                    //restrict CLI presentation
                    presentation_mode = 3;
                    get_clir_result = CommandsInterface.CLIR_INVOCATION;
                } else {
                    //allow CLI presentation
                    presentation_mode = 4;
                    get_clir_result = CommandsInterface.CLIR_SUPPRESSION;
                }

            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCLIR(): XcapException");
                mOirCache = null;
                mOirCachePhoneId = -1;
                mOirCacheLastQueried = 0;

                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCLIR(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCLIR(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                //Something is wrong:Set presentation_mode=2 for upper application
                Rlog.d(LOG_TAG, "handleGetCLIR():Start to Print Stack Trace");
                mOirCache = null;
                mOirCachePhoneId = -1;
                mOirCacheLastQueried = 0;

                presentation_mode = 2;
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;

                }
            }
            //

            //[Example]
            //Note that Spec 22.030 AnnexB. - only CF/CB/CW will have service class to be specified
            //For CLIP/CLIR/COLP/COLR will not have service class to be specified
            if (rr.mResult != null) {
                int get_clir_response [] = new int[2];
                get_clir_response[0] = get_clir_result;
                get_clir_response[1] = presentation_mode; //Only 1(Permantently provisioned),
                                                          //3(Temporary presentation disallowed),
                                                          //4(Temporary presentation allowed)
                                                          //are allowed
                AsyncResult.forMessage(rr.mResult, get_clir_response, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public void handleGetCLIP(MMTelSSRequest rr) {
            //GsmMMiCode.java onQueryComplete():0(disabled), 1(enabled)
            int reqNo = -1;
            int serialNo = -1;
            int get_clip_result = 0; //disabled
            int phoneId = rr.mp.readInt();

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleGetCLIP(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            try {
                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleGetCLIP(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return;
                }

                requestXcapNetwork(phoneId);

                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);
                OriginatingIdentityPresentation oip =
                        mSimservs.getOriginatingIdentityPresentation(true, mNetwork);
                Rlog.d(LOG_TAG, "handleGetCLIP():active=" + oip.isActive());
                if (oip.isActive()) {
                    get_clip_result = 1; //enabled
                } else {
                    get_clip_result = 0; //disabled
                }

            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCLIP(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCLIP(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCLIP(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCLIP():Start to Print Stack Trace");
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;

                }
            }

            if (rr.mResult != null) {
                int get_clip_response [] = new int[1];
                get_clip_response[0] = get_clip_result;
                AsyncResult.forMessage(rr.mResult, get_clip_response, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public void handleGetCOLP(MMTelSSRequest rr) {
            //+COLP:n,m (n:0(disabled),1(enabled); m:0(not provision),1(provision),2(unknonw))
            //See GsmMMiCode.java- onGetColpComplete()
            //In IMS/XCAP, SS service should be provisioned to each user let him/her to configure
            // this service
            //Only the service is provisioned to the user, he/she is able to configure

            int reqNo = -1;
            int serialNo = -1;
            int get_colp_response [] = new int[2];
            int phoneId = rr.mp.readInt();

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleGetCOLP(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            try {
                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleGetCOLP(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return;
                }

                requestXcapNetwork(phoneId);

                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);
                TerminatingIdentityPresentation tip =
                        mSimservs.getTerminatingIdentityPresentation(true, mNetwork);
                Rlog.d(LOG_TAG, "handleGetCOLP():active=" + tip.isActive());
                if (tip.isActive()) {
                    //According to TS24.608 Section 4.5.1:The TIP service is activated at
                    // provisioning and deactived at withdrawal
                    get_colp_response[0] = 1; //enabled
                    get_colp_response[1] = 1; //provision
                } else {
                    get_colp_response[0] = 0; //disabled
                    get_colp_response[1] = 0; //Not provision
                }

            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCOLP(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCOLP(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCOLP(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCOLP():Start to Print Stack Trace");
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                           CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            }

            if (rr.mResult != null) {
                //get_colp_response[0] = 0; //disabled
                //2nd parameter: Provios or not (2:un-known In this way, not expect the user will
                // configure COLP)
                //get_colp_response[1] = 2; //But GsmMMiCode.java- onGetColpComplete(): only cares
                // this parameter (provison or not!)
                AsyncResult.forMessage(rr.mResult, get_colp_response, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public void handleGetCOLR(MMTelSSRequest rr) {
            //See GsmMMiCode.java- onGetColrComplete(): Only check result[0] this parameter

            int reqNo = -1;
            int serialNo = -1;
            int get_colr_response [] = new int[1];
            int phoneId = rr.mp.readInt();

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleGetCOLR(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            try {
                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleGetCOLR(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return;
                }

                requestXcapNetwork(phoneId);

                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);
                TerminatingIdentityPresentationRestriction tir =
                        mSimservs.getTerminatingIdentityPresentationRestriction(true, mNetwork);
                Rlog.d(LOG_TAG, "handleGetCOLR():active=" + tir.isActive());
                if (tir.isActive()) {
                    //According to TS24.608 Section 4.5.1:The TIR service is activated at
                    // provisioning and deactived at withdrawal
                    get_colr_response[0] = 1; //enabled/Provision

                } else {
                    get_colr_response[0] = 0; //disabled/Not Provision
                }

            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCOLR(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCOLR(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCOLR(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCOLR():Start to Print Stack Trace");
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            }


            if (rr.mResult != null) {
                //But GsmMMiCode.java- onGetColrComplete(): only cares this parameter
                // (provison or not!)
                //get_colr_response[0] = 2; //unknown (In this way, not expect the user will
                // configure COLR)
                AsyncResult.forMessage(rr.mResult, get_colr_response, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }


        public void handleGetCW(MMTelSSRequest rr) {
            int reqNo = -1;
            int serialNo = -1;
            int cwServiceClass = -1;
            int get_cw_response [] = new int[2];
            int phoneId = 0;

            //As per 3GPP TS 24.083, section 1.6 UE doesn't need to send service class parameter
            // in call waiting interrogation  to network
            //For CW Activation, it can only active for some specific service class but the query
            // result will be returned supported status of all service classes
            //[TODO-Question]It seems that XCAP does not support to enable CW according to
            // different service classes!
            try {
                rr.mp.setDataPosition(0);
                reqNo = rr.mp.readInt();
                serialNo = rr.mp.readInt();
                cwServiceClass = rr.mp.readInt();
                phoneId = rr.mp.readInt();
                Rlog.d(LOG_TAG, "Read GET_CW serviceClass=" + cwServiceClass);

                if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                    Rlog.d(LOG_TAG, "handleGetCW(): !isPreferXcap()");
                    throw new UnknownHostException();
                }

                //Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
                if (cwServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO
                        | CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                    cwServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                }

                CommunicationWaiting cw;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleGetCW(): mCwCache = " + mCwCache + ", curTime = "
                        + curTime + ", mCwCacheLastQueried = " + mCwCacheLastQueried);
                if ((mCwCache != null) && (phoneId == mCwCachePhoneId) &&
                        (mCwCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleGetCW(): using ETAG mCwCache: " + mCwCache);
                    requestXcapNetwork(phoneId);
                    cw = mCwCache;
                    cw.setNetwork(mNetwork);
                    cw.refresh();
                    mCwCacheLastQueried = curTime;
                } else if ((mCwCache != null) && (phoneId == mCwCachePhoneId) &&
                        ((curTime >= mCwCacheLastQueried)
                        && ((curTime - mCwCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleGetCW(): using mCwCache: " + mCwCache);
                    cw = mCwCache;
                } else {
                    requestXcapNetwork(phoneId);

                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleGetCW(): XcapRoot = " + mXcapRoot);
                        throw new UnknownHostException();
                    }

                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);
                    cw = mSimservs.getCommunicationWaiting(true, mNetwork);
                    mCwCache = cw;
                    mCwCachePhoneId = phoneId;
                    mCwCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleGetCW(): new mCwCache = " + mCwCache
                            + ", curTime = " + curTime);
                }
                //[Question] Always support CW in XCAP?
                if (cw.isActive()) {
                    get_cw_response[0] = 1;
                }
                else {
                    get_cw_response[0] = 0;
                }
                Rlog.d(LOG_TAG, "handleGetCW(): isActive = " + get_cw_response[0]);

            } catch (UnknownHostException unknownHostException) {
                mCwCache = null;
                mCwCachePhoneId = -1;
                mCwCacheLastQueried = 0;
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, unknownHostException);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCW(): XcapException");
                mCwCache = null;
                mCwCachePhoneId = -1;
                mCwCacheLastQueried = 0;

                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCW(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCW(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCW():Start to Print Stack Trace");
                mCwCache = null;
                mCwCachePhoneId = -1;
                mCwCacheLastQueried = 0;

                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;

                }
            }
            //

            //[Example]
            if (rr.mResult != null) {
                //get_cw_response[0] = 1; //0:Disabled, 1:Enabled
                //serviceClass:SERVICE_CLASS_VOICE(1), SERVICE_CLASS_VIDEO(512)
                if (get_cw_response[0] == 1) {
                    //Service Class (If get_cw_response[0] is 1, get_cw_response[1] must set bit#1
                    // & enable corresponding serviceClass's bit)
                    //See ril_ss.c's requestCallWaitingOperation()
                    //In CallWaitingCheckBoxPreference.java's handleGetCallWaitingResponse()
                    //-> setChecked(((cwArray[0] == 1) && ((cwArray[1] & 0x01) == 0x01)));
                    //If VIDEO call waiting is enabled, it shoud set: get_cw_response[0]=1
                    // && get_cw_response[1]= 513 (SERVICE_CLASS_VIDEO | 1);
                    //In GsmMmiCode.java's onQueryComplete():
                    //-> createQueryCallWaitingResultMessage(ints[1])
                    //-> Check each bit:
                    // sb.append(serviceClassToCFString(classMask & serviceClass):
                    // Check each bit then append string

                    if (cwServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                        //As per 3GPP TS 24.083, section 1.6 UE doesn't need to send service
                        // class parameter in call waiting interrogation  to network
                        //For CW Activation, it can only active for some specific service class
                        // but the query result will be returned supported status of all service
                        // classes
                        get_cw_response[1] |= CommandsInterface.SERVICE_CLASS_VOICE;
                        get_cw_response[1] |= CommandsInterface.SERVICE_CLASS_VIDEO;
                    } else {
                        get_cw_response[1] |= cwServiceClass;
                        if (cwServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                            //Need to modify:If a user presses MMI string to query CW & only vidoe
                            // call waiting is enabled
                            //In this way, MMI code callback handler will regard as both voice
                            // and video call waitings are enabled
                            get_cw_response[1] |= CommandsInterface.SERVICE_CLASS_VOICE;
                        }
                    }
                    Rlog.d(LOG_TAG, "handleGetCW(): class = " + get_cw_response[1]);
                }
                AsyncResult.forMessage(rr.mResult, get_cw_response, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public void handleGetCB(MMTelSSRequest rr) {
            int reqNo = -1;
            int serialNo = -1;
            int cbServiceClass = -1;
            int phoneId = 0;
            String cBFacility = "";
            int get_cb_response [] = new int[1];
            //default: CB is not disabled!
            get_cb_response[0] = 0;

            try {
                rr.mp.setDataPosition(0);
                reqNo = rr.mp.readInt();
                serialNo = rr.mp.readInt();
                cBFacility = rr.mp.readString();
                cbServiceClass = rr.mp.readInt();
                phoneId = rr.mp.readInt();
                Rlog.d(LOG_TAG, "Read GET_CB Facility=" + cBFacility + ",serviceClass="
                        + cbServiceClass);

                if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                    Rlog.d(LOG_TAG, "handleGetCB(): !isPreferXcap()");
                    throw new UnknownHostException();
                }

                int num_of_comparision = 0;

                //Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
                if (cbServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO
                        | CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                    cbServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                }

                if (cbServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                    cbServiceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                    //one is to match audio (i.e., serviceClass = SERVICE_CLASS_VOICE),
                    // the other is to match video (i.e., SERVICE_CLASS_VIDEO)
                    num_of_comparision = 2;
                    Rlog.d(LOG_TAG, "cbServiceClass==0, try to 1st match by using"
                            + " SERVICE_CLASS_VOICE");
                } else {
                    //Specific serviceClass (i.e., value is not 0) is carried from the upper layer
                    num_of_comparision = 1;
                }


                //According to the cBFacility to choose the XML node to check CB's ruleset
                if (cBFacility.equals(CommandsInterface.CB_FACILITY_BAOC) ||
                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAOIC) ||
                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
                    OutgoingCommunicationBarring ocb;
                    long curTime = System.currentTimeMillis();
                    Rlog.d(LOG_TAG, "handleGetCB(): mOcbCache = " + mOcbCache + ", curTime = "
                            + curTime + ", mOcbCacheLastQueried = " + mOcbCacheLastQueried);
                    if ((mOcbCache != null) && (phoneId == mOcbCachePhoneId) &&
                            (mOcbCache.isSupportEtag())) {
                        Rlog.d(LOG_TAG, "handleGetCB(): using ETAG mOcbCache: " + mOcbCache);
                        requestXcapNetwork(phoneId);
                        ocb = mOcbCache;
                        ocb.setNetwork(mNetwork);
                        ocb.refresh();
                        mOcbCacheLastQueried = curTime;
                    } else if ((mOcbCache != null) && (phoneId == mOcbCachePhoneId) &&
                            ((curTime >= mOcbCacheLastQueried)
                            && ((curTime - mOcbCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                        Rlog.d(LOG_TAG, "handleGetCB(): using mOcbCache: " + mOcbCache);
                        ocb = mOcbCache;
                    } else {
                        requestXcapNetwork(phoneId);

                        mXui = MMTelSSUtils.getXui(phoneId, mContext);
                        mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                        mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                        if (TextUtils.isEmpty(mXcapRoot)) {
                            Rlog.d(LOG_TAG, "handleGetCB(): XcapRoot = " + mXcapRoot);
                            throw new UnknownHostException();
                        }

                        setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName,
                                mPassword, phoneId);

                        ocb = mSimservs.getOutgoingCommunicationBarring(true, mNetwork);
                        mOcbCache = ocb;
                        mOcbCachePhoneId = phoneId;
                        mOcbCacheLastQueried = curTime;
                        Rlog.d(LOG_TAG, "handleGetCB(): new mOcbCache = " + mOcbCache
                                + ", curTime = " + curTime);
                    }

                    RuleSet ruleSet = ocb.getRuleSet();
                    List<Rule> ruleList = null;

                    if (ruleSet != null) {
                        ruleList = ruleSet.getRules();
                        if (ruleList == null) {
                            Rlog.d(LOG_TAG, "Dump Get MO CB XML: ruleset with empty rules");
                        } else {
                            Rlog.d(LOG_TAG, "Dump Get MO CB XML:" + ruleSet.toXmlString());
                        }
                    } else {
                        Rlog.d(LOG_TAG, "No MO related CB rules in remote server");
                    }

                    //Note that: If no ant configuration is stored in XCAP server (e.g., empty
                    // xml string), ruleList will be null
                    if (ruleList != null) {
                        for (int it = 0; it < num_of_comparision; it++) {
                            if (it == 1
                                    && cbServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                                //2nd time to match all rules by using SERVICE_CLASS_VIDEO
                                cbServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                                Rlog.d(LOG_TAG, "cbServiceClass==0, try to 2nd match by using "
                                        + "SERVICE_CLASS_VIDEO");
                            }
                            //Check each rule & its corresponding condition/action
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                Conditions cond = r.getConditions();
                                Actions action = r.getActions();
                                List<String> mediaList = null;

                                Rlog.d(LOG_TAG, "handleGetCB():MO-facility=" + cBFacility
                                        + ",action=" + action.isAllow());
                                if (cond != null) {
                                    Rlog.d(LOG_TAG, "handleGetCB():MO-international="
                                            + cond.comprehendInternational() + ",roaming="
                                            + cond.comprehendRoaming());
                                    mediaList = cond.getMedias();
                                } else {
                                    Rlog.d(LOG_TAG, "handleGetCB():Empty MO cond (cond==null)"
                                            + " for this rule=" + r);
                                }

                                if ((cond != null && cond.comprehendInternational()) &&
                                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAOIC) &&
                                        containSpecificMedia(mediaList, cbServiceClass)) {
                                    if (action.isAllow() == false && (cond != null
                                            && cond.comprehendRuleDeactivated() == false)) {
                                        //BAOIC is enabled
                                        get_cb_response[0] |= cbServiceClass;
                                    } else {
                                        get_cb_response[0] = 0;
                                    }
                                    break;
                                } else if ((cond != null && cond.comprehendInternationalExHc()) &&
                                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAOICxH) &&
                                        containSpecificMedia(mediaList, cbServiceClass)) {
                                    if (action.isAllow() == false && (cond != null
                                            && cond.comprehendRuleDeactivated() == false)) {
                                        //BAOICxH is enabled
                                        get_cb_response[0] |= cbServiceClass;
                                    } else {
                                        get_cb_response[0] = 0;
                                    }
                                } else if (isBAOC(cond, cbServiceClass) &&
                                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAOC)) {
                                    //cond == null:BAOC is enabled
                                    //cond != null && cond.comprehendRuleDeactivated() == false =>
                                    // E.g., <cp:conditions><media>audio</media></cp:conditions>
                                    if (action.isAllow() == false && (cond == null ||
                                            (cond != null
                                            && cond.comprehendRuleDeactivated() == false))) {
                                        //BAOC is enabled
                                        get_cb_response[0] |= cbServiceClass;
                                    } else {
                                        get_cb_response[0] = 0;
                                    }
                                    break;
                                }
                            } //end of for-loop(check each rule)
                        } //end of for-loop(check all possible service classes)
                    } else {
                        //MO Barring Call is disabled
                        Rlog.d(LOG_TAG, "ruleList is null, MO CB is disabled");
                        get_cb_response[0] = 0;
                    }

                } else if (cBFacility.equals(CommandsInterface.CB_FACILITY_BAIC) ||
                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAICr)) {
                    IncomingCommunicationBarring icb;
                    long curTime = System.currentTimeMillis();
                    Rlog.d(LOG_TAG, "handleGetCB(): mIcbCache = " + mIcbCache + ", curTime = "
                            + curTime + ", mIcbCacheLastQueried = " + mIcbCacheLastQueried);
                    if ((mIcbCache != null) && (phoneId == mIcbCachePhoneId) &&
                            (mIcbCache.isSupportEtag())) {
                        Rlog.d(LOG_TAG, "handleGetCB(): using ETAG mIcbCache: " + mIcbCache);
                        requestXcapNetwork(phoneId);
                        icb = mIcbCache;
                        icb.setNetwork(mNetwork);
                        icb.refresh();
                        mIcbCacheLastQueried = curTime;
                    } else if ((mIcbCache != null) && (phoneId == mIcbCachePhoneId) &&
                            ((curTime >= mIcbCacheLastQueried)
                            && ((curTime - mIcbCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                        Rlog.d(LOG_TAG, "handleGetCB(): using mIcbCache: " + mIcbCache);
                        icb = mIcbCache;
                    } else {
                        requestXcapNetwork(phoneId);

                        mXui = MMTelSSUtils.getXui(phoneId, mContext);
                        mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                        mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                        if (TextUtils.isEmpty(mXcapRoot)) {
                            Rlog.d(LOG_TAG, "handleGetCB(): XcapRoot = " + mXcapRoot);
                            throw new UnknownHostException();
                        }

                        setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName,
                                mPassword, phoneId);

                        icb = mSimservs.getIncomingCommunicationBarring(true, mNetwork);
                        mIcbCache = icb;
                        mIcbCachePhoneId = phoneId;
                        mIcbCacheLastQueried = curTime;
                        Rlog.d(LOG_TAG, "handleGetCB(): new mIcbCache = " + mIcbCache
                                + ", curTime = " + curTime);
                    }

                    RuleSet ruleSet = icb.getRuleSet();
                    List<Rule> ruleList = null;

                    if (ruleSet != null) {
                        ruleList = ruleSet.getRules();
                        if (ruleList == null) {
                            Rlog.d(LOG_TAG, "Dump Get MT CB XML: ruleset with empty rules");
                        } else {
                            Rlog.d(LOG_TAG, "Dump Get MT CB XML:" + ruleSet.toXmlString());
                        }
                    } else {
                        Rlog.d(LOG_TAG, "No MT related CB rules in remote server");
                    }

                    //Note that: If no ant configuration is stored in XCAP server (e.g.,
                    // empty xml string), ruleList will be null
                    if (ruleList != null) {
                        for (int it = 0; it < num_of_comparision; it++) {
                            if (it == 1
                                    && cbServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                                //2nd time to match all rules by using SERVICE_CLASS_VIDEO
                                cbServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                                Rlog.d(LOG_TAG, "cbServiceClass==0, try to 2nd match by using"
                                        + " SERVICE_CLASS_VIDEO");
                            }
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                Conditions cond = r.getConditions();
                                Actions action = r.getActions();
                                List<String> mediaList = null;

                                Rlog.d(LOG_TAG, "handleGetCB():MT-facility=" + cBFacility
                                        + ",action=" + action.isAllow());
                                if (cond != null) {
                                    Rlog.d(LOG_TAG, "handleGetCB():MT-international="
                                            + cond.comprehendInternational() + ",roaming="
                                            + cond.comprehendRoaming() + ",anonymous="
                                            + cond.comprehendAnonymous());
                                    mediaList = cond.getMedias();
                                } else {
                                    Rlog.d(LOG_TAG, "handleGetCB():Empty MT cond (cond==null)"
                                            + " for this rule=" + r);
                                }

                                if ((cond != null && cond.comprehendRoaming()) &&
                                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAICr) &&
                                        containSpecificMedia(mediaList, cbServiceClass)) {
                                    if (action.isAllow() == false && (cond != null
                                            && cond.comprehendRuleDeactivated() == false)) {
                                        //BAICr is enabled
                                        get_cb_response[0] |= cbServiceClass;
                                    } else {
                                        get_cb_response[0] = 0;
                                    }
                                } else if (isBAIC(cond, cbServiceClass) &&
                                        cBFacility.equals(CommandsInterface.CB_FACILITY_BAIC)) {
                                    //cond == null:BAIC is enabled
                                    //cond != null && cond.comprehendRuleDeactivated() == false
                                    // => E.g., <cp:conditions><media>audio</media></cp:conditions>
                                    if (action.isAllow() == false && (cond == null || (cond != null
                                            && cond.comprehendRuleDeactivated() == false))) {
                                        //BAIC is enabled
                                        get_cb_response[0] |= cbServiceClass;
                                    } else {
                                        get_cb_response[0] = 0;
                                    }

                                }
                            } //end of for-loop(check each rule)
                        } //end of for-loop(check each possible service class)
                    } else {
                        //MT Barring Call is disabled
                        Rlog.d(LOG_TAG, "ruleList is null, MT CB is disabled");
                        get_cb_response[0] = 0;
                    }
                } else {
                    //Add handling Barring Service for 330(AB)/333(AG)/353(AC) cases - Only allow
                    // to unlock (i.e.,disable)
                    /***
                     * "AO" BAOC (Barr All Outgoing Calls) (refer 3GPP TS 22.088 [6] clause 1)
                     * "OI" BOIC (Barr Outgoing International Calls) (refer 3GPP TS 22.088 [6]
                     * clause 1)
                     * "OX" BOIC exHC (Barr Outgoing International Calls except to Home Country)
                     * (refer 3GPP TS 22.088 [6] clause 1)
                     * "AI" BAIC (Barr All Incoming Calls) (refer 3GPP TS 22.088 [6] clause 2)
                     * "IR" BIC Roam (Barr Incoming Calls when Roaming outside the home country)
                     * (refer 3GPP TS 22.088 [6] clause 2)
                     * "AB" All Barring services (refer 3GPP TS 22.030 [19]) (applicable only for
                     * <mode>=0: i.e.,unlock - ref: +CLCK & 2/3G SS Spec)
                     * "AG" All outGoing barring services (refer 3GPP TS 22.030 [19]) (applicable
                     * only for <mode>=0: i.e.,unlock - ref: +CLCK & 2/3G SS Spec)
                     * "AC" All inComing barring services (refer 3GPP TS 22.030 [19]) (applicable
                     * only for <mode>=0: i.e.,unlock - ref: +CLCK & 2/3G SS Spec)
                     */
                    Rlog.d(LOG_TAG, "handleGetCB(): Not support query for CB Facility="
                            + cBFacility);
                }

            } catch (UnknownHostException unknownHostException) {
                mOcbCache = null;
                mOcbCachePhoneId = -1;
                mOcbCacheLastQueried = 0;
                mIcbCache = null;
                mIcbCachePhoneId = -1;
                mIcbCacheLastQueried = 0;

                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, unknownHostException);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCB(): XcapException");
                mOcbCache = null;
                mOcbCachePhoneId = -1;
                mOcbCacheLastQueried = 0;
                mIcbCache = null;
                mIcbCachePhoneId = -1;
                mIcbCacheLastQueried = 0;

                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCB(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCB(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCB():Start to Print Stack Trace");
                mOcbCache = null;
                mOcbCachePhoneId = -1;
                mOcbCacheLastQueried = 0;
                mIcbCache = null;
                mIcbCachePhoneId = -1;
                mIcbCacheLastQueried = 0;

                get_cb_response[0] = 0;
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;

                }
            }

            //[Example]Fill the fake-result just for testing first by mtk01411
            if (rr.mResult != null) {
                //If response[0] = 0 (means that disable), otherwise response[0] will be set
                // the corresponding serviceClass for this CallBarring
                /*
                if (cBFacility.equals(CommandsInterface.CB_FACILITY_BAOIC)) {
                    get_cb_response[0] = CommandsInterface.SERVICE_CLASS_VOICE;
                } else {
                    //It means that: CB is disabled for this facility
                    get_cb_response[0] = CommandsInterface.SERVICE_CLASS_NONE;
                }
                 */

                //For Call Setting query: In CallBarringBasePreference.java's
                // handleGetCallBarringResponse()
                //-> int value = ints[0]; value = value & mServiceClass;
                //It means that if the query result (e.g., both voice & video calls enable CB):
                // Please set corresponding bit of this servicClass
                //E.g., get_cb_response[0] = 513 (SERVICE_CLASS_VIDEO:512 + SERVICE_CLASS_VOICE:1)
                //For GsmMmiCode query: In GsmMmiCode.java's onQueryComplete()
                //-> createQueryCallBarringResultMessage(ints[0]):
                //-> Check each bit then append corresponding string:
                // sb.append(serviceClassToCFString(classMask & serviceClass));
                AsyncResult.forMessage(rr.mResult, get_cb_response, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public void handleGetCF(MMTelSSRequest rr) {
            int reqNo = -1;
            int serialNo = -1;
            int numInfos = 1;
            CallForwardInfo infos[] = null;
            ArrayList<CallForwardInfo> queriedCallForwardInfoList;
            queriedCallForwardInfoList = new ArrayList<CallForwardInfo>();

            int cfAction = -1;
            int reason = -1;
            int serviceClass = -1;
            int orgServiceClass = -1;
            String cfNumber = "";
            String CFPhoneNum = "";
            int queryStatus = 0; //0: DISABLE, 1: ENABLE
            int noReplyTimer = 20;
            int phoneId = 0;

            try {
                //Solution#1: Parcel:int(request),int(mSerial),int(status),int(reason),
                //int(serviceClass)
                //byte data[];
                //data = rr.mp.marshall();
                //Note that: It must invoke Parcel's recycle() here because in obtain():
                // It gets a Parcel object from pool then writes two int values into this object
                //Now: It already handles this request, it's time to recycle this object back to
                // the pool
                //rr.mp.recycle();
                //rr.mp = null;
                //int reqNo = data[0];
                //int serialNo = data[4];
                //int status = data[8];
                //int reason = data[12];
                //int serviceClass = data[16];

                //Solution#2: Reset Data Position back to 0 (head) to start to read the data
                // filled previously
                rr.mp.setDataPosition(0);
                reqNo = rr.mp.readInt();
                serialNo = rr.mp.readInt();
                cfAction = rr.mp.readInt();
                reason = rr.mp.readInt();
                serviceClass = rr.mp.readInt();
                orgServiceClass = serviceClass;
                cfNumber = rr.mp.readString();
                phoneId = rr.mp.readInt();

                if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                    Rlog.d(LOG_TAG, "handleGetCF(): !isPreferXcap()");
                    throw new UnknownHostException();
                }

                Rlog.d(LOG_TAG, "Read from CF parcel:req=" + requestToString(reqNo) + ",cfAction="
                        + cfAction + ",reason=" + reason + ",serviceClass=" + serviceClass
                        + ",number=" + cfNumber);

                CommunicationDiversion cd;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleGetCF(): mCdCache = " + mCdCache + ", curTime = "
                        + curTime + ", mCdCacheLastQueried = " + mCdCacheLastQueried);
                if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        (mCdCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleGetCF(): using ETAG mCdCache: " + mCdCache);
                    requestXcapNetwork(phoneId);
                    cd = mCdCache;
                    cd.setNetwork(mNetwork);
                    cd.refresh();
                    mCdCacheLastQueried = curTime;
                } else if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        ((curTime >= mCdCacheLastQueried)
                        && ((curTime - mCdCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleGetCF(): using mCdCache: " + mCdCache);
                    cd = mCdCache;
                } else {
                    requestXcapNetwork(phoneId);

                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleGetCF(): XcapRoot = " + mXcapRoot);
                        throw new UnknownHostException();
                    }

                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);

                    cd = mSimservs.getCommunicationDiversion(true, mNetwork);
                    mCdCache = cd;
                    mCdCachePhoneId = phoneId;
                    mCdCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleGetCF(): new mCdCache = " + mCdCache
                            + ", curTime = " + curTime);
                }

                //[M2]Reference to SimServsTest.java
                //XcapDocumentSelector documentSelector = new XcapDocumentSelector(
                //XcapConstants.AUID_RESOURCE_LISTS, TEST_USER, TEST_DOC);
                //Rlog.d(LOG_TAG, "document selector is " + documentSelector.toString());
                //XcapUri xcapUri = new XcapUri();
                //xcapUri.setXcapRoot(XCAP_ROOT).setDocumentSelector(documentSelector);
                //CommunicationDiversion cd =
                //  mSimservs.getCommunicationDiversion(xcapUri, TEST_USER,"password");
                Rlog.d(LOG_TAG, "handleGetCF():GetRuleSet from cd");

                RuleSet ruleSet = cd.getRuleSet();
                //In Communication Diversion's RuleSet, it may have several rules (e.g., rule
                // for CFU, rule for CFB, rule for CFNoAnswer, rule for CFNotReachable)
                List<Rule> ruleList = null;

                if (ruleSet != null) {
                    ruleList = ruleSet.getRules();
                } else {
                    Rlog.d(LOG_TAG, "No CF related rules in remote server");
                }

                //Note that: If no ant configuration is stored in XCAP server (e.g., empty
                // xml string), ruleList will be null
                if (ruleList != null) {

                    int num_of_expansion = 1;
                    if (reason == CommandsInterface.CF_REASON_ALL_CONDITIONAL) {
                        //In this case: User inputs "*#004#"
                        //CF Conditional (BUSY/NO_REPLY/NOT_REACHABLE/NOT_REGISTERED)x4
                        num_of_expansion = 4;
                    } else if (reason == CommandsInterface.CF_REASON_ALL) {
                        //CF Conditional (BUSY/NO_REPLY/NOT_REACHABLE/NOT_REGISTERED)x4 + CFUx1
                        //In this case: User inputs "*#002#"
                        num_of_expansion = 5;
                    }

                    for (int n = 0; n < num_of_expansion; n++) {
                        if (num_of_expansion != 1) {
                            if (n == 0) reason = CommandsInterface.CF_REASON_BUSY;
                            else if (n == 1) reason = CommandsInterface.CF_REASON_NO_REPLY;
                            else if (n == 2) reason = CommandsInterface.CF_REASON_NOT_REACHABLE;
                            else if (n == 3) reason = CommandsInterface.CF_REASON_NOT_REGISTERED;
                            else if (n == 4) reason = CommandsInterface.CF_REASON_UNCONDITIONAL;
                        }

                        Rlog.d(LOG_TAG, "num_of_expansion=" + num_of_expansion + ": with round="
                                + (n + 1) + ",with reason=" + reason);

                        //Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
                        if (orgServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO
                                | CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                            serviceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                        }

                        //E.g.,GsmMmiCode:MMI String can be :*#21#[CFU] or *#67#[CFB] or
                        // *#61#[CF NoAnswer] or *#62#[CF Not Reachable]
                        //=> Without serviceClass information, then serviceClass=0 is passed to
                        // MMTelSS
                        int num_of_comparision = 0;

                        if (orgServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                            serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                            //one is to match audio (i.e., serviceClass = SERVICE_CLASS_VOICE), the
                            // other is to match video (i.e., SERVICE_CLASS_VIDEO)
                            num_of_comparision = 2;
                            Rlog.d(LOG_TAG, "serviceClass==0, try to 1st match by using"
                                    + " SERVICE_CLASS_VOICE");
                        } else {
                            //Specific serviceClass (i.e., value is not 0) is carried from the
                            // upper layer
                            num_of_comparision = 1;
                        }

                        for (int it = 0; it < num_of_comparision; it++) {

                            if (it == 1 && serviceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                                //2nd time to match all rules by using SERVICE_CLASS_VIDEO
                                serviceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                                Rlog.d(LOG_TAG, "serviceClass==0, try to 2nd match by using "
                                        + "SERVICE_CLASS_VIDEO");
                            }

                            Rlog.d(LOG_TAG, "num_of_comparision=" + num_of_comparision
                                    + ": with round=" + (it + 1) + ",with service class="
                                    + serviceClass);

                            //Check each rule & its corresponding condition/action
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                Conditions cond = r.getConditions();
                                Actions action = r.getActions();
                                List<String> mediaList = null;

                                if (cond != null) {
                                    Rlog.d(LOG_TAG, "handleGetCF():busy=" + cond.comprehendBusy()
                                            + ",NoAnswer=" + cond.comprehendNoAnswer()
                                            + ",NoReachable=" + cond.comprehendNotReachable()
                                            + ",NotRegistered=" + cond.comprehendNotRegistered());
                                    mediaList = cond.getMedias();
                                } else {
                                    Rlog.d(LOG_TAG, "handleGetCF():Empty cond (cond==null) "
                                            + "for this rule=" + r);
                                }

                                //See queryCallForwardStatus(): cfAction is always set to 2
                                if (cfAction == 2
                                        && reason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                                        ((cond != null && cond.comprehendBusy() == false &&
                                        cond.comprehendNoAnswer() == false &&
                                        cond.comprehendNotRegistered() == false &&
                                        cond.comprehendNotReachable() == false) &&
                                        cond.comprehendRuleDeactivated() == false || cond == null)
                                        && containSpecificMedia(mediaList, serviceClass)) {
                                    Rlog.d(LOG_TAG, "handleGetCF():CFU is enabled on server");
                                    //[CFU]CFU is enabled, set queryStatus as 1
                                    queryStatus = 1;
                                    if (action.getFowardTo() != null) {
                                        CFPhoneNum = action.getFowardTo().getTarget();
                                    }
                                    //timeSeconds: This field is not required by CFU (Only required
                                    // by CFNoAnswer)
                                    noReplyTimer = cd.getNoReplyTimer();
                                    break;

                                } else if (cfAction == 2
                                        && reason == CommandsInterface.CF_REASON_BUSY &&
                                        (cond != null && cond.comprehendBusy() == true
                                        && cond.comprehendRuleDeactivated() == false) &&
                                        containSpecificMedia(mediaList, serviceClass)) {
                                    Rlog.d(LOG_TAG, "handleGetCF():CFB is enabled on server");
                                    //[CFB]CFB is ensabled, set queryStatus as 0
                                    queryStatus = 1;
                                    if (action.getFowardTo() != null) {
                                        CFPhoneNum = action.getFowardTo().getTarget();
                                    }
                                    noReplyTimer = cd.getNoReplyTimer();
                                    break;

                                } else if (cfAction == 2
                                        && reason == CommandsInterface.CF_REASON_NO_REPLY &&
                                        (cond != null && cond.comprehendNoAnswer() == true &&
                                        cond.comprehendRuleDeactivated() == false) &&
                                        containSpecificMedia(mediaList, serviceClass)) {
                                    Rlog.d(LOG_TAG, "handleGetCF():CFNoAnswer is enabled on "
                                            + "server");
                                    //[CFNoAnswer]CFNoReply is ensabled, set queryStatus as 1
                                    queryStatus = 1;
                                    if (action.getFowardTo() != null) {
                                        CFPhoneNum = action.getFowardTo().getTarget();
                                    }
                                    noReplyTimer = cd.getNoReplyTimer();
                                    break;

                                } else if (cfAction == 2
                                        && reason == CommandsInterface.CF_REASON_NOT_REACHABLE &&
                                        (cond != null && cond.comprehendNotReachable() == true &&
                                        cond.comprehendRuleDeactivated() == false) &&
                                        containSpecificMedia(mediaList, serviceClass)) {
                                    Rlog.d(LOG_TAG, "handleGetCF():CFNotReachable is enabled"
                                            + " on server");
                                    //[CFNotReachable]CFNotReachable is enabled, set queryStatus
                                    // as 1
                                    queryStatus = 1;
                                    if (action.getFowardTo() != null) {
                                        CFPhoneNum = action.getFowardTo().getTarget();
                                    }
                                    noReplyTimer = cd.getNoReplyTimer();
                                    break;

                                } else if (cfAction == 2
                                        && reason == CommandsInterface.CF_REASON_NOT_REGISTERED &&
                                        (cond != null && cond.comprehendNotRegistered() == true &&
                                        cond.comprehendRuleDeactivated() == false) &&
                                        containSpecificMedia(mediaList, serviceClass)) {
                                    Rlog.d(LOG_TAG, "handleGetCF():CFNotRegistered is enabled on "
                                            + "server");
                                    //[CFNotRegistered]CFNotRegistered is enabled, set queryStatus
                                    // as 1
                                    queryStatus = 1;
                                    if (action.getFowardTo() != null) {
                                        CFPhoneNum = action.getFowardTo().getTarget();
                                    }
                                    noReplyTimer = cd.getNoReplyTimer();
                                    break;

                                } else {
                                    //Something wrong!
                                    Rlog.d(LOG_TAG, "handleGetCF()from xcap:Not matched "
                                            + "this rule!");
                                }

                            } //end of for-loop(ruleList.size())
                            //Add this queried & matched result into the matchedCallForwardInfoList
                            CallForwardInfo item = new CallForwardInfo();
                            item.status = queryStatus;
                            item.reason = reason;
                            item.serviceClass = serviceClass;
                            item.toa = 0;
                            item.number = CFPhoneNum;
                            item.timeSeconds = noReplyTimer;
                            Rlog.d(LOG_TAG, "handleGetCF():add one record with reason=" + reason
                                    + ",serviceClass=" + serviceClass + ",queryStatus="
                                    + queryStatus);
                            queriedCallForwardInfoList.add(item);

                            //Reset some variables for this matching result
                            queryStatus = 0; //0: DISABLE, 1: ENABLE
                            CFPhoneNum = "";
                            noReplyTimer = 20;
                        } //end of for-loop(num_of_comparision)

                    } //end of for-loop(num_of_expansion)

                    //After checking all rules in the ruleset, it will update the results for this
                    // serviceClass entry
                    //For Call Setting query, it will be handled by
                    // CallForwardEditPreference.java's handleGetCFResponse()
                    //For GsmMmiCode query, it will be handled by GsmMmiCode.java's
                    // onQueryCfComplete()
                    int queriedSize = queriedCallForwardInfoList.size();

                    infos = new CallForwardInfo[queriedSize];
                    for (int inx = 0; inx < queriedSize; inx++) {
                        infos[inx] = (CallForwardInfo) queriedCallForwardInfoList.get(inx);
                    }

                } else {
                    //Empty XML String:CF is disabled, set queryStatus as 0
                    Rlog.d(LOG_TAG, "handleGetCF():get null ruleList");
                    infos = new CallForwardInfo[0];
                    queryStatus = 0;
                }

            } catch (UnknownHostException unknownHostException) {
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, unknownHostException);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCF(): XcapException");
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCF(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCF(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCF():Start to Print Stack Trace");
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                e.printStackTrace();

                //Add by mtk01411 (2014-0128)
                //If not returns here, it may report infos.size!=0 but both info[0] and info[1] are
                // null -> Cause JE happens in CallForwardEditPreference.java line#379:Null Pointer
                // Exception
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;

                }

            }


            //[Example - According to the reason to fill the queryStatus and CFPhoneNum -> These
            // two fields are checked by application]
            /*
            if (reason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                CFPhoneNum = "";
                //Note that: If CFU's queryStatus=1(active/enabled), then CFB,CFNoAnswer and
                // CFNotReachable will be disabled on UI
                //Because CFU means that call must be forwarded unconditionally -> In this way,
                // UI must also disabled the editable item for others CF
                //If CFU's queryStatus=0(no active/disabled), UI must provide the editable item
                // of CFB,CFNoAnswer and CFNotReachable for users to set phone number as their will
                queryStatus = 0;
            } else {
                queryStatus = 1;
            }
             */

            //[Example - CF Response Structure]
            if (rr.mResult != null) {
                /*
                infos[0] = new CallForwardInfo();
                infos[0].status = queryStatus;
                infos[0].reason = reason;
                infos[0].serviceClass = serviceClass;
                infos[0].toa = 0;
                infos[0].number = CFPhoneNum;
                infos[0].timeSeconds = noReplyTimer;
                 */

                AsyncResult.forMessage(rr.mResult, infos, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        // This API is used to create new CF rule matched the serviceclass & reason specified by
        // the user in user's configuration at this time
        // E.g., User wants to disable CFB for voice call and XCAP server has one rule - CFB for
        // both voice & video calls currently
        // In this way, we can create the rule disabled CFB for voice call first by this API
        // then copy old rule except voice type into new rule set to present that: BAOC for video
        // call is still enabled
        public boolean handleCreateNewRuleForExistingCF(CommunicationDiversion cd,
                RuleSet newRuleSet, Rule r, int setCFReason, int setCFAction,
                int setCFServiceClass, String setCFNumber, int setCFTimeSeconds, String ruleID,
                boolean updateSingleRule, int numExpansion, int phoneId) throws XcapException {
            Conditions cond = r.getConditions();
            Actions action = r.getActions();

            //For testing purpose, it will read XCAP Rule Disable Mode (xrdm) from system property
            String sDisableRuleMode = SystemProperties.get(PROP_SS_DISABLE_METHOD,
                    Integer.toString(DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG));
            Rlog.d(LOG_TAG, "handleCreateNewRuleForExistingCF():sDisableRuleMode="
                    + sDisableRuleMode);
            mDisableRuleMode = Integer.parseInt(sDisableRuleMode);

            //Find this rule for this time's request
            if (setCFAction == CommandsInterface.CF_ACTION_ENABLE
                    || setCFAction == CommandsInterface.CF_ACTION_REGISTRATION) {
                //Create a new rule (C1:Modify existing CFB (e.g.,forwarded number)
                Rule cfRule = newRuleSet.createNewRule(ruleID);
                Conditions cfCond = cfRule.createConditions();
                Actions cfAction = cfRule.createActions();
                Rlog.d(LOG_TAG, "handleCreateNewRuleForExistingCF():Enable CF with reason="
                        + setCFReason + ",serviceClass=" + setCFServiceClass + ",number="
                        + setCFNumber + ",cfTime=" + setCFTimeSeconds);
                //Add media into this new rule
                if (!MMTelSSUtils.isOp03IccCard(phoneId)
                    && !MMTelSSUtils.isOp05IccCard(phoneId)
                    && !MMTelSSUtils.isOp06IccCard(phoneId)
                    && !MMTelSSUtils.isOp07IccCard(phoneId)
                    && !MMTelSSUtils.isOp15IccCard(phoneId)) {
                    if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                        cfCond.addMedia("audio");
                    } else if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                        cfCond.addMedia("video");
                    } else if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                        cfCond.addMedia("audio");
                        cfCond.addMedia("video");
                    }
                }

                if (setCFReason == CommandsInterface.CF_REASON_BUSY) {
                    cfCond.addBusy();
                } else if (setCFReason == CommandsInterface.CF_REASON_NO_REPLY) {
                    cfCond.addNoAnswer();
                } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REACHABLE) {
                    cfCond.addNotReachable();
                } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REGISTERED) {
                    cfCond.addNotRegistered();
                } else if (setCFReason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                    //Not set any conditions -> always evaluate the result as true
                }

                // + [ALPS02292552] If empty, set the value from server.
                if (setCFNumber == null || setCFNumber.isEmpty()) {
                    setCFNumber = action.getFowardTo().getTarget();
                    Rlog.d(LOG_TAG, "Reason: " + setCFReason +
                        ", setCFNumber is empty or null, so update to: " + setCFNumber);
                }
                // - [ALPS02292552]

                if (MMTelSSUtils.isOp01IccCard(phoneId) && MMTelSSUtils.isNotifyCallerTest()) {
                    cfAction.setFowardTo(setCFNumber, false);
                } else {
                    cfAction.setFowardTo(setCFNumber, true);
                }
                cfAction.getFowardTo().setRevealIdentityToCaller(true);
                cfAction.getFowardTo().setRevealIdentityToTarget(true);
                if (updateSingleRule && (1 == numExpansion)) {
                    cd.saveRule(ruleID);
                }
                return true;
            } else {
                //Disable CFB for existing rule (Remove tihs rule -> i.e., Not copy this existing
                // rule to newRuleSet)
                if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                    if (mDisableRuleMode == DISABLE_MODE_DELETE_RULE) {
                        Rlog.d(LOG_TAG, "Disable CF for serviceClass=0 (all media types):neither "
                                + "create new rule nor copy old rule to new rule set");
                        if (updateSingleRule) {
                            Rlog.e(LOG_TAG, "handleCreateNewRuleForExistingCF(): ERROR: "
                                    + "DISABLE_MODE_DELETE_RULE but updateSingleRule");
                        }
                        return false;
                    } else if (mDisableRuleMode == DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG) {
                        Rlog.d(LOG_TAG, "Disable CF for serviceClass=0 (all media types):copy old "
                                + "rule with <rule-deactivated> into new rule set");
                        Rule nr = copyOldRuleToNewRuleSet(r, newRuleSet, setCFAction, setCFReason);
                        nr.getConditions().addRuleDeactivated();
                        if (updateSingleRule && (1 == numExpansion)) {
                            cd.saveRule(nr.mId);
                        }
                        return true;
                    }
                } else if (hasExtraMedia(cond.getMedias(), setCFServiceClass)) {
                    if (updateSingleRule && (1 == numExpansion)) {
                        //We just remove one media type which is marked to disable the Call
                        // Forwarding (but the remaining media types must keep their original rule)
                        Rule newRule = copyOldRuleToNewRuleSetExceptSpecificMedia(r, newRuleSet,
                                    setCFServiceClass, phoneId, setCFAction, setCFReason);
                        if (newRule != null) {
                            cd.saveRule(newRule.mId);
                        }
                        return true;
                    }
                    if (mDisableRuleMode == DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG) {
                        Rlog.d(LOG_TAG, "Disable" + " " + ruleID + ":copy old rule with "
                                + "<rule-deactivated> for this media types to new rule set");
                        Rule nr = copyOldRuleToNewRuleSet(r, newRuleSet, setCFAction, setCFReason);
                        nr.getConditions().addRuleDeactivated();
                    }

                    Rlog.d(LOG_TAG, "Disable" + " " + ruleID
                            + ":copy old rule for remaining media types to new rule set");
                    //We just remove one media type which is marked to disable the Call
                    // Forwarding (but the remaining media types must keep their original rule)
                    copyOldRuleToNewRuleSetExceptSpecificMedia(r, newRuleSet, setCFServiceClass,
                            phoneId, setCFAction, setCFReason);
                    return true;
                } else {
                    //Exactly matched & only one serviceclass!
                    if (mDisableRuleMode == DISABLE_MODE_DELETE_RULE) {
                        Rlog.d(LOG_TAG, "Disable" + " " + ruleID
                                + ":not copy old rule to new rule set");
                        if (updateSingleRule) {
                            Rlog.e(LOG_TAG, "handleCreateNewRuleForExistingCF(): ERROR: "
                                    + "DISABLE_MODE_DELETE_RULE but updateSingleRule");
                        }
                        return false;
                    } else if (mDisableRuleMode == DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG) {
                        Rlog.d(LOG_TAG, "Disable" + " " + ruleID
                                + ":copy old rule with <rule-deactivated> to new rule set");
                        Rule nr = copyOldRuleToNewRuleSet(r, newRuleSet, setCFAction, setCFReason);
                        nr.getConditions().addRuleDeactivated();
                        if (updateSingleRule && (1 == numExpansion)) {
                            cd.saveRule(nr.mId);
                        }
                        return true;
                    }
                }
                return false;
            }
        }

        // This API is used to create new CB rule matched the serviceclass & reason specified
        // by the user in user's configuration at this time
        // E.g., User wants to disable BAOC for voice call and XCAP server has one rule - BAOC
        // for both voice & video calls currently
        // In this way, we can create the rule disabled BAOC for voice call first by this API
        // then copy old rule except voice type into new rule set to present that: BAOC for video
        // call is still enabled
        public boolean handleCreateNewRuleForExistingCB(SimservType ssType, RuleSet newRuleSet,
                Rule r, String facility, int lockState, int setCBServiceClass, String RuleID,
                boolean updateSingleRule, int num_of_expansion, int phoneId) throws XcapException {
            Conditions cond = r.getConditions();
            Actions action = r.getActions();
            boolean cbAllow = true;
            boolean addRuleDeactivatedNode = false;

            //For testing purpose, it will read XCAP Rule Disable Mode (xrdm) from system property
            String sDisableRuleMode = SystemProperties.get(PROP_SS_DISABLE_METHOD,
                    Integer.toString(DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG));
            Rlog.d(LOG_TAG, "handleCreateNewRuleForExistingCB():sDisableRuleMode="
                    + sDisableRuleMode);
            mDisableRuleMode = Integer.parseInt(sDisableRuleMode);

            if (lockState == 1) {
                //Enable CB (Because it always creates a new rule for this service class, it will
                //only have <allow> and no <rule-deactivated>)
                cbAllow = false;
            } else {
                //Disable CB
                if (mDisableRuleMode == DISABLE_MODE_DELETE_RULE) {
                    Rlog.d(LOG_TAG, "Disable CB for serviceClass=" + setCBServiceClass
                            + " ,not create new rule for it to put in the new rule set");
                    if (updateSingleRule) {
                        Rlog.e(LOG_TAG, "handleCreateNewRuleForExistingCB(): ERROR:"
                                + " DISABLE_MODE_DELETE_RULE but updateSingleRule");
                    }
                    return false;
                } else if (mDisableRuleMode == DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG) {
                    // Usage: add <rule-deactivated> to disable this CB rule, then keep <allow>
                    // value as false
                    addRuleDeactivatedNode = true;
                    cbAllow = false;
                } else if (mDisableRuleMode == DISABLE_MODE_CHANGE_CB_ALLOW) {
                    cbAllow = true;
                }
            }

            Rule cbRule = newRuleSet.createNewRule(RuleID);
            Conditions cbCond = cbRule.createConditions();
            Actions cbAction = cbRule.createActions();

            //Add media into this new rule
            if (!MMTelSSUtils.isOp03IccCard(phoneId)
                && !MMTelSSUtils.isOp05IccCard(phoneId)
                && !MMTelSSUtils.isOp06IccCard(phoneId)
                && !MMTelSSUtils.isOp07IccCard(phoneId)
                && !MMTelSSUtils.isOp15IccCard(phoneId)) {
                if (setCBServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                    cbCond.addMedia("audio");
                } else if (setCBServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                    cbCond.addMedia("video");
                } else if (setCBServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                    cbCond.addMedia("audio");
                    cbCond.addMedia("video");
                }
            }

            // Usage: add <rule-deactivated> to disable this CB rule, then keep <allow> value as
            // false
            if (mDisableRuleMode == DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG
                    && addRuleDeactivatedNode == true) {
                cbCond.addRuleDeactivated();
            }

            if (facility.equals(CommandsInterface.CB_FACILITY_BAICr)) {
                cbCond.addRoaming();
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAIC)) {
                //Bar All Incoming Calls (no conditions -> evaluate the result as true directly)
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOIC)) {
                cbCond.addInternational();
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
                cbCond.addInternationalExHc();
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOC)) {
                //Bar All Outgoing Calls (no conditions -> evaluate the result as true directly)
                cbAction.setAllow(cbAllow);
            }

            if (updateSingleRule && (1 == num_of_expansion)) {
                if (ssType instanceof OutgoingCommunicationBarring) {
                    OutgoingCommunicationBarring ocb = (OutgoingCommunicationBarring) ssType;
                    ocb.saveRule(RuleID);
                } else if (ssType instanceof IncomingCommunicationBarring) {
                    IncomingCommunicationBarring icb = (IncomingCommunicationBarring) ssType;
                    icb.saveRule(RuleID);
                }
            }

            return true;
        }

        /* This API is used to add one new rule not occurred & matched in the XCAP server */
        /* E.g., In original XCAP server:Only BAOC, then user enables the BOIC */
        public boolean handleCreateNewRuleForReqCB(SimservType ssType, RuleSet newRuleSet,
                String facility, int lockState, int setCBServiceClass, String RuleID,
                boolean updateSingleRule, int num_of_expansion, int phoneId) throws XcapException {
            boolean cbAllow = true;

            if (lockState == 1) {
                //Enable CB
                cbAllow = false;
            } else {
                //Disable CB:It means that the user disable one existed rule!
                //Note:If the user wants to disable one rule but no any matched one stored in XCAP
                // server,
                //It is not allowed to add this disabled rule into the ruleset: retunr from this
                //API directly
                cbAllow = true;
                Rlog.d(LOG_TAG, "Disable one non-existed rule!Return from "
                        + "handleCreateNewRuleForReqCB() directly!");
                return false;
            }

            Rule cbRule = newRuleSet.createNewRule(RuleID);
            Conditions cbCond = cbRule.createConditions();
            Actions cbAction = cbRule.createActions();

            //Add media into this new rule
            if (!MMTelSSUtils.isOp03IccCard(phoneId)
                && !MMTelSSUtils.isOp05IccCard(phoneId)
                && !MMTelSSUtils.isOp06IccCard(phoneId)
                && !MMTelSSUtils.isOp07IccCard(phoneId)
                && !MMTelSSUtils.isOp15IccCard(phoneId)) {
                if (setCBServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                    cbCond.addMedia("audio");
                } else if (setCBServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                    cbCond.addMedia("video");
                } else if (setCBServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                    cbCond.addMedia("audio");
                    cbCond.addMedia("video");
                }
            }

            if (facility.equals(CommandsInterface.CB_FACILITY_BAICr)) {
                cbCond.addRoaming();
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAIC)) {
                //Bar All Incoming Calls (no conditions -> evaluate the result as true directly)
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOIC)) {
                cbCond.addInternational();
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
                cbCond.addInternationalExHc();
                cbAction.setAllow(cbAllow);
            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOC)) {
                //Bar All Outgoing Calls (no conditions -> evaluate the result as true directly)
                cbAction.setAllow(cbAllow);
            }

            if (updateSingleRule && (1 == num_of_expansion)) {
                if (ssType instanceof OutgoingCommunicationBarring) {
                    OutgoingCommunicationBarring ocb = (OutgoingCommunicationBarring) ssType;
                    ocb.saveRule(RuleID);
                } else if (ssType instanceof IncomingCommunicationBarring) {
                    IncomingCommunicationBarring icb = (IncomingCommunicationBarring) ssType;
                    icb.saveRule(RuleID);
                }
            }

            return true;
        }


        public Rule copyOldRuleToNewRuleSet(Rule oldRule, RuleSet newRuleSet,
            int cfAction, int cfReason) {
            Conditions oldCond = oldRule.getConditions();
            Actions oldAction = oldRule.getActions();

            Rule newRule = newRuleSet.createNewRule(oldRule.mId);
            Conditions newCond = newRule.createConditions();
            Actions newAction = newRule.createActions();

            if (oldCond != null) {
                if (oldCond.comprehendBusy()) {
                    newCond.addBusy();
                }
                if (oldCond.comprehendCommunicationDiverted()) {
                    newCond.addCommunicationDiverted();
                }
                if (oldCond.comprehendInternational()) {
                    newCond.addInternational();
                }
                if (oldCond.comprehendInternationalExHc()) {
                    newCond.addInternationalExHc();
                }
                if (oldCond.comprehendNoAnswer()) {
                    newCond.addNoAnswer();
                }
                if (oldCond.comprehendNotReachable()) {
                    newCond.addNotReachable();
                }
                if (oldCond.comprehendNotRegistered()) {
                    newCond.addNotRegistered();
                }
                if (oldCond.comprehendPresenceStatus()) {
                    newCond.addPresenceStatus();
                }
                if (oldCond.comprehendRoaming()) {
                    newCond.addRoaming();
                }
                if (oldCond.comprehendRuleDeactivated()) {
                    newCond.addRuleDeactivated();
                }
                //Copy Medias
                List<String>oldMediaList = oldCond.getMedias();
                if (oldMediaList != null) {
                    for (int i = 0; i < oldMediaList.size(); i++) {
                        newCond.addMedia(oldMediaList.get(i));
                    }
                }
                // Condition for OP01 UT
                newCond.addTime(oldCond.comprehendTime());
            }

            ForwardTo oldForward = oldAction.getFowardTo();
            if (oldForward != null) {
                if (cfAction == CommandsInterface.CF_ACTION_ERASURE &&
                    cfReason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                    Rlog.i(LOG_TAG, "No need to append the original numebr in Erasure.");
                    newAction.setFowardTo("", oldForward.isNotifyCaller());
                } else {
                    newAction.setFowardTo(oldForward.getTarget(), oldForward.isNotifyCaller());
                }
                newAction.getFowardTo().setRevealIdentityToCaller(
                        oldForward.isRevealIdentityToCaller());
                newAction.getFowardTo().setRevealIdentityToTarget(
                        oldForward.isRevealIdentityToTarget());
            } else {
                 Rlog.i(LOG_TAG, "No need to append the forward number, cfAction: " + cfAction +
                    ", cfReason: " + cfReason);
            }

            newAction.setAllow(oldAction.isAllow());
            return newRule;
        }

        public void copyOldRuleToNewRuleSetWithDisabledCB(Rule oldRule, RuleSet newRuleSet,
                boolean allow, int cfAction, int cfReason) {
            Actions newAction = null;
            Conditions newCond = null;
            Actions oldAction = oldRule.getActions();
            Conditions oldCond = oldRule.getConditions();

            if (mDisableRuleMode == DISABLE_MODE_DELETE_RULE) {
                if (oldAction.isAllow() == false
                        && oldCond.comprehendRuleDeactivated() == false) {
                    //Do nothing:Not copy the old rule (enabled CB) into the new ruleset
                } else {
                    copyOldRuleToNewRuleSet(oldRule, newRuleSet, cfAction, cfReason);
                }
            } else if (mDisableRuleMode == DISABLE_MODE_ADD_RULE_DEACTIVATED_TAG) {
                Rule newRule = copyOldRuleToNewRuleSet(oldRule, newRuleSet, cfAction, cfReason);
                if (newRule != null && oldAction.isAllow() == false
                        && oldCond.comprehendRuleDeactivated() == false) {
                    newCond = newRule.createConditions();
                    newCond.addRuleDeactivated();
                }
            } else if (mDisableRuleMode == DISABLE_MODE_CHANGE_CB_ALLOW) {
                Rule newRule = copyOldRuleToNewRuleSet(oldRule, newRuleSet, cfAction, cfReason);
                if (newRule != null && oldAction.isAllow() == false
                        && oldCond.comprehendRuleDeactivated() == false) {
                    newAction = newRule.createActions();
                    newAction.setAllow(allow);
                }
            }
        }


        public Rule copyOldRuleToNewRuleSetExceptSpecificMedia(Rule oldRule, RuleSet newRuleSet,
                int requestedServiceClass, int phoneId, int cfAction, int cfReason) {
            Conditions oldCond = oldRule.getConditions();
            Actions oldAction = oldRule.getActions();

            //[Example] Both voice call & video call enable CB for All Outgoing International Calls
            // Now, a user wants to disable CB for All Outgoing International Calls of voice call
            // <cond>
            //     internaltional
            //     <media>
            //         audio
            //         video
            //     </media>
            // </cond>
            // <action>
            //     <allow>false</allow>
            // </action>

            // One rule will be split into two rules as follows (one for voice call and the other
            //for video call)

            // Rule#1:
            // <cond>
            //     internaltional
            //     <media>
            //         audio
            //
            //     </media>
            // </cond>
            // <action>
            //     <allow>true</allow>
            // </action>
            // Rule#2: (This API is used to create rule such as Rule#2 -- see the following)
            // <cond>
            //     internaltional
            //     <media>
            //
            //         video
            //     </media>
            // </cond>
            // <action>
            //     <allow>false</allow>
            // </action>

            /*
            int requestedServicClass = -1;
            if (media.equals("audio")) {
                requestedServicClass = CommandsInterface.SERVICE_CLASS_VOICE;
            } else if (media.equals("video")) {
                requestedServicClass = CommandsInterface.SERVICE_CLASS_VIDEO;
            }
             */

            //Add the check: oldCond!=null - because Barring all calls or Forwarding UnConditional
            // => <cp:conditions> can be empty by mtk01411 2014-0122
            if (oldCond != null &&
                    hasExtraMedia(oldCond.getMedias(), requestedServiceClass) == false) {
                //If the original rule has no extra media type, because we have already created a
                // new rule for the requested media type
                //Only for extra media type: we still must create a new rule with original
                // configuration then add into new rule set to keep original rule such media
                return null;
            }

            Rule newRule = newRuleSet.createNewRule(oldRule.mId);
            Conditions newCond = newRule.createConditions();
            Actions newAction = newRule.createActions();

            if (oldCond != null) {
                if (oldCond.comprehendBusy()) {
                    newCond.addBusy();
                }
                if (oldCond.comprehendCommunicationDiverted()) {
                    newCond.addCommunicationDiverted();
                }
                if (oldCond.comprehendInternational()) {
                    newCond.addInternational();
                }
                if (oldCond.comprehendInternationalExHc()) {
                    newCond.addInternationalExHc();
                }
                if (oldCond.comprehendNoAnswer()) {
                    newCond.addNoAnswer();
                }
                if (oldCond.comprehendNotReachable()) {
                    newCond.addNotReachable();
                }
                if (oldCond.comprehendNotRegistered()) {
                    newCond.addNotRegistered();
                }
                if (oldCond.comprehendPresenceStatus()) {
                    newCond.addPresenceStatus();
                }
                if (oldCond.comprehendRoaming()) {
                    newCond.addRoaming();
                }
                if (oldCond.comprehendRuleDeactivated()) {
                    newCond.addRuleDeactivated();
                }
                //Copy Medias
                List<String>oldMediaList = oldCond.getMedias();
                if (oldMediaList != null) {
                    for (int i = 0; i < oldMediaList.size(); i++) {
                        if (!getMediaType(requestedServiceClass).equals(oldMediaList.get(i))) {
                            newCond.addMedia(oldMediaList.get(i));
                        }
                    }
                }
                // Condition for OP01 UT
                newCond.addTime(oldCond.comprehendTime());
            } else {
                //Add by mtk01411: 2014-0123
                //oldCond == null:For BAOC or CFU(Unconditional), so its oldMediaList should mean
                // both for audio & video
                if (!MMTelSSUtils.isOp03IccCard(phoneId)
                    && !MMTelSSUtils.isOp05IccCard(phoneId)
                    && !MMTelSSUtils.isOp06IccCard(phoneId)
                    && !MMTelSSUtils.isOp07IccCard(phoneId)
                    && !MMTelSSUtils.isOp15IccCard(phoneId)) {
                    List<String>oldMediaList = new ArrayList<String>();;
                    oldMediaList.add("audio");
                    oldMediaList.add("video");
                    for (int i = 0; i < oldMediaList.size(); i++) {
                        if (getMediaType(requestedServiceClass).equals(oldMediaList.get(i))
                                == false) {
                            newCond.addMedia(oldMediaList.get(i));
                        }
                    }
                }
            }

            ForwardTo oldForward = oldAction.getFowardTo();
            if (oldForward != null) {
                if (cfAction == CommandsInterface.CF_ACTION_ERASURE &&
                    cfReason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                    Rlog.i(LOG_TAG, "No need to append the original numebr in Erasure.");
                    newAction.setFowardTo("", oldForward.isNotifyCaller());
                } else {
                    newAction.setFowardTo(oldForward.getTarget(), oldForward.isNotifyCaller());
                }
                newAction.getFowardTo().setRevealIdentityToCaller(
                        oldForward.isRevealIdentityToCaller());
                newAction.getFowardTo().setRevealIdentityToTarget(
                        oldForward.isRevealIdentityToTarget());
            } else {
                 Rlog.i(LOG_TAG, "No need to append the forward number, cfAction: " + cfAction +
                    ", cfReason: " + cfReason);
            }
            newAction.setAllow(oldAction.isAllow());

            return newRule;
        }


        public int handleSetCLIR(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();

            int clirMode = rr.mp.readInt();
            int phoneId = rr.mp.readInt();
            Rlog.d(LOG_TAG, "Read from CLIR parcel:clirMode=" + clirMode);

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCLIR(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return 0;
            }

            try {
                OriginatingIdentityPresentationRestriction oir;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleSetCLIR(): mOirCache = " + mOirCache + ", curTime = "
                        + curTime + ", mOirCacheLastQueried = " + mOirCacheLastQueried
                        + ", phoneId = " + phoneId);

                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);
                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleSetCLIR(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return 0;
                }
                requestXcapNetwork(phoneId);
                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);

                if ((mOirCache != null) && (phoneId == mOirCachePhoneId) &&
                        (mOirCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleSetCLIR(): using ETAG mOirCache: " + mOirCache);
                    oir = mOirCache;
                    oir.setNetwork(mNetwork);
                    oir.refresh();
                    mOirCacheLastQueried = curTime;
                } else if ((mOirCache != null) && (phoneId == mOirCachePhoneId) &&
                        ((curTime >= mOirCacheLastQueried)
                        && ((curTime - mOirCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleSetCLIR(): using mOirCache: " + mOirCache);
                    oir = mOirCache;
                    oir.setNetwork(mNetwork);
                } else {
                    oir = mSimservs.getOriginatingIdentityPresentationRestriction(true, mNetwork);
                    mOirCache = oir;
                    mOirCachePhoneId = phoneId;
                    mOirCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleSetCLIR(): new mOirCache = " + mOirCache
                            + ", curTime = " + curTime);
                }

                if (clirMode == CommandsInterface.CLIR_INVOCATION) {
                    oir.setDefaultPresentationRestricted(true);
                } else if (clirMode == CommandsInterface.CLIR_SUPPRESSION) {
                    oir.setDefaultPresentationRestricted(false);
                } else {
                    oir.setDefaultPresentationRestricted(false);
                }
            } catch (XcapException xcapException) {
                mOirCache = null;
                mOirCachePhoneId = -1;
                mOirCacheLastQueried = 0;

                if (xcapException.getHttpErrorCode() == HTTP_ERROR_CODE_412) {
                    Rlog.d(LOG_TAG, "handleSetCLIR(): HTTP_ERROR_CODE_412");
                    return HTTP_ERROR_CODE_412;
                }
                Rlog.d(LOG_TAG, "handleSetCLIR(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCLIR(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCLIR(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            } catch (Exception e) {
                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCLIR():Start to Print Stack Trace");
                mOirCache = null;
                mOirCachePhoneId = -1;
                mOirCacheLastQueried = 0;

                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            }

            //[Notify upper's application about the SET_CLIR result - Success Case
            // without exception]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }
            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }

            return 0;
        }

        public void handleSetCLIP(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();

            int clipEnable = rr.mp.readInt();
            int phoneId = rr.mp.readInt();
            Rlog.d(LOG_TAG, "Read from CLIP parcel:clipMode=" + clipEnable);

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCLIP(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            try {
                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleSetCLIP(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return;
                }

                requestXcapNetwork(phoneId);

                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);
                OriginatingIdentityPresentation oip =
                        mSimservs.getOriginatingIdentityPresentation(true, mNetwork);
                if (clipEnable == 1) {
                    oip.setActive(true);
                } else {
                    oip.setActive(false);
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleSetCLIP(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCLIP(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCLIP(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCLIP():Start to Print Stack Trace");
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            }

            //[Notify upper's application about the SET_CLIP result - Success Case without
            // exception]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }
            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }


        public void handleSetCOLR(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();

            int colrMode = rr.mp.readInt();
            int phoneId = rr.mp.readInt();
            Rlog.d(LOG_TAG, "Read from COLR parcel:clirMode=" + colrMode);

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCOLR(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            try {
                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleSetCOLR(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return;
                }

                requestXcapNetwork(phoneId);

                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);
                TerminatingIdentityPresentationRestriction tir =
                        mSimservs.getTerminatingIdentityPresentationRestriction(true, mNetwork);
                if (colrMode == CommandsInterface.CLIR_INVOCATION) {
                    tir.setDefaultPresentationRestricted(true);
                } else if (colrMode == CommandsInterface.CLIR_SUPPRESSION) {
                    tir.setDefaultPresentationRestricted(false);
                } else {
                    tir.setDefaultPresentationRestricted(false);
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleSetCOLR(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCOLR(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCOLR(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCOLR():Start to Print Stack Trace");
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            }

            //[Notify upper's application about the SET_COLR result - Success Case without
            // exception]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }
            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public void handleSetCOLP(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();

            int colpEnable = rr.mp.readInt();
            int phoneId = rr.mp.readInt();
            Rlog.d(LOG_TAG, "Read from COLP parcel:colpEnable=" + colpEnable);

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCOLP(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return;
            }

            try {
                mXui = MMTelSSUtils.getXui(phoneId, mContext);
                mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                if (TextUtils.isEmpty(mXcapRoot)) {
                    Rlog.d(LOG_TAG, "handleSetCOLP(): XcapRoot = " + mXcapRoot);
                    if (null != rr.mResult) {
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                        rr.mResult.sendToTarget();
                    }
                    return;
                }

                requestXcapNetwork(phoneId);

                setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                        phoneId);
                TerminatingIdentityPresentation tip =
                        mSimservs.getTerminatingIdentityPresentation(true, mNetwork);
                if (colpEnable == 1) {
                    tip.setActive(true);
                } else {
                    tip.setActive(false);
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleSetCOLP(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCOLP(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCOLP(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCOLP():Start to Print Stack Trace");
                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            }

            //[Notify upper's application about the SET_COLP result - Success Case without
            // exception]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }
            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }



        public int handleSetCW(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();
            int enabled = rr.mp.readInt();
            int serviceClass = rr.mp.readInt();
            int phoneId = rr.mp.readInt();

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCW(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return 0;
            }

            try {
                CommunicationWaiting cw;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleSetCW(): mCwCache = " + mCwCache + ", curTime = "
                        + curTime + ", mCwCacheLastQueried = " + mCwCacheLastQueried
                        + ", phoneId = " + phoneId);
                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleSetCW(): XcapRoot = " + mXcapRoot);
                        if (null != rr.mResult) {
                            AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                            rr.mResult.sendToTarget();
                        }
                        return 0;
                    }
                    requestXcapNetwork(phoneId);
                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);
                if ((mCwCache != null) && (phoneId == mCwCachePhoneId) &&
                        (mCwCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleSetCW(): using ETAG mCwCache: " + mCwCache);
                    cw = mCwCache;
                    cw.setNetwork(mNetwork);
                    cw.refresh();
                    mCwCacheLastQueried = curTime;
                } else if ((mCwCache != null) && (phoneId == mCwCachePhoneId) &&
                        ((curTime >= mCwCacheLastQueried)
                        && ((curTime - mCwCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleSetCW(): using mCwCache: " + mCwCache);
                    cw = mCwCache;
                    cw.setNetwork(mNetwork);
                } else {
                    cw = mSimservs.getCommunicationWaiting(true, mNetwork);
                    mCwCache = cw;
                    mCwCachePhoneId = phoneId;
                    mCwCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleSetCW(): new mCwCache = " + mCwCache
                            + ", curTime = " + curTime);
                }
                Rlog.d(LOG_TAG, "handleSetCW(): enabled = " + enabled);
                if (enabled == 1) {
                    cw.setActive(true);
                } else {
                    cw.setActive(false);
                }
            } catch (XcapException xcapException) {
                mCwCache = null;
                mCwCachePhoneId = -1;
                mCwCacheLastQueried = 0;
                if (xcapException.getHttpErrorCode() == HTTP_ERROR_CODE_412) {
                    Rlog.d(LOG_TAG, "handleSetCW(): HTTP_ERROR_CODE_412");
                    return HTTP_ERROR_CODE_412;
                }
                Rlog.d(LOG_TAG, "handleSetCW(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCW(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCW(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            } catch (Exception e) {
                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCW():Start to Print Stack Trace");
                mCwCache = null;
                mCwCachePhoneId = -1;
                mCwCacheLastQueried = 0;

                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            }

            //[Notify upper's application about the SET_CW result - Success Case without
            // exception]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }

            return 0;
        }

        public int handleSetCF(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();

            int setCFAction = rr.mp.readInt();
            int setCFReason = rr.mp.readInt();
            int setCFServiceClass = rr.mp.readInt();
            String setCFNumber = rr.mp.readString();
            int setCFTimeSeconds = rr.mp.readInt();
            int phoneId = rr.mp.readInt();
            int reportFlag = 0;

            CallForwardInfo infos[] = null;

            boolean AddRuleForCFUWithAllMediaType = false;
            boolean AddRuleForCFBWithAllMediaType = false;
            boolean AddRuleForCFNoAnswerWithAllMediaType = false;
            boolean AddRuleForCFNotReachableWithAllMediaType = false;
            boolean AddRuleForCFNotRegisteredWithAllMediaType = false;

            String CFU_RuleID = "CFU";
            String CFB_RuleID = "CFB";
            String CFNoAnswer_RuleID = "CFNoAnswer";
            String CFNotReachable_RuleID = "CFNotReachable";
            String CFNotRegistered_RuleID = "CFNotReachable";

            Rlog.d(LOG_TAG, "Read from CF parcel:req=" + requestToString(reqNo) + ",cfAction="
                    + setCFAction + ",reason=" + setCFReason + ",serviceClass="
                    + setCFServiceClass + ",number=" + setCFNumber + ",timeSec="
                    + setCFTimeSeconds);

            String XcapCFNum = SystemProperties.get(PROP_SS_CFNUM, "");
            if (XcapCFNum.startsWith("sip:") || XcapCFNum.startsWith("sips:")
                    || XcapCFNum.startsWith("tel:")) {
                Rlog.d(LOG_TAG, "handleSetCF():get call forwarding num from EM setting:"
                        + XcapCFNum);
                String ss_mode = SystemProperties.get(PROP_SS_MODE, MODE_SS_XCAP);
                Rlog.d(LOG_TAG, "handleSetCF():ss_mode=" + ss_mode);
                if (MODE_SS_XCAP.equals(ss_mode)) {
                    setCFNumber = XcapCFNum;
                }
            }

            //Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
            if (setCFServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO
                    | CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                setCFServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
            }

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCF(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return 0;
            }

            try {

                int num_of_expansion = 1;
                //Need several interators to check
                if (setCFReason == CommandsInterface.CF_REASON_ALL_CONDITIONAL) {
                    //It means that to check 4
                    // conditions:CFB/CFNoAnser/CFNotReachable/CFNotRegistered
                    num_of_expansion = 4;
                } else if (setCFReason == CommandsInterface.CF_REASON_ALL) {
                    //It means that to check 4
                    // conditions:CFB/CFNoAnser/CFNotReachable/CFNotRegistered
                    // and 1 unconditional:CFU
                    num_of_expansion = 5;
                }

                CommunicationDiversion cd;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleSetCF(): mCdCache = " + mCdCache + ", curTime = "
                        + curTime + ", mCdCacheLastQueried = " + mCdCacheLastQueried
                        + ", phoneId = " + phoneId);

                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleSetCF(): XcapRoot = " + mXcapRoot);
                        if (null != rr.mResult) {
                            AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                            rr.mResult.sendToTarget();
                        }
                        return 0;
                    }
                    requestXcapNetwork(phoneId);
                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);

                if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        (mCdCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleSetCF(): using ETAG mCdCache: " + mCdCache);
                    cd = mCdCache;
                    cd.setNetwork(mNetwork);
                    cd.refresh();
                    mCdCacheLastQueried = curTime;
                } else if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        ((curTime >= mCdCacheLastQueried)
                        && ((curTime - mCdCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleSetCF(): using mCdCache: " + mCdCache);
                    cd = mCdCache;
                    cd.setNetwork(mNetwork);
                } else {
                    cd = mSimservs.getCommunicationDiversion(true, mNetwork);
                    mCdCache = cd;
                    mCdCachePhoneId = phoneId;
                    mCdCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleSetCF(): new mCdCache = " + mCdCache
                            + ", curTime = " + curTime);
                }

                for (int it = 0; it < num_of_expansion; it++) {
                    if (num_of_expansion != 1) {
                        if (it == 0) setCFReason = CommandsInterface.CF_REASON_BUSY;
                        else if (it == 1) setCFReason = CommandsInterface.CF_REASON_NO_REPLY;
                        else if (it == 2) setCFReason = CommandsInterface.CF_REASON_NOT_REACHABLE;
                        else if (it == 3) setCFReason = CommandsInterface.CF_REASON_NOT_REGISTERED;
                        else if (it == 4) setCFReason = CommandsInterface.CF_REASON_UNCONDITIONAL;
                    }

                    //Only report to upper layer application at the last execution
                    if (it == (num_of_expansion - 1)) {
                        reportFlag = 1;
                    }

                    Rlog.d(LOG_TAG, "handleSetCF():it=" + it + ", num_of_expansion="
                            + num_of_expansion + ",cfReason=" + setCFReason);

                    RuleSet ruleSet = cd.getRuleSet();
                    List<Rule> ruleList = null;
                    RuleSet newRuleSet = cd.createNewRuleSet();
                    boolean addedNewRule = false;


                    if (ruleSet != null) {
                        ruleList = ruleSet.getRules();
                    } else {
                        Rlog.d(LOG_TAG, "No CF related rules in remote server");
                    }

                    //Note that: If no ant configuration is stored in XCAP server (e.g., empty xml
                    // string), ruleList will be null
                    if (ruleList != null) {
                        //Check each rule & its corresponding condition/action
                        for (int i = 0; i < ruleList.size(); i++) {
                            Rule r = ruleList.get(i);
                            Conditions cond = r.getConditions();
                            Actions action = r.getActions();
                            List<String> mediaList = null;

                            if (cond != null) {
                                mediaList = cond.getMedias();
                                Rlog.d(LOG_TAG, "handleSetCF():busy=" + cond.comprehendBusy()
                                        + ",NoAnswer=" + cond.comprehendNoAnswer()
                                        + ",NoReachable="
                                        + cond.comprehendNotReachable() + ",NotRegistered="
                                        + cond.comprehendNotRegistered());
                                if (cond.comprehendBusy()) {
                                    CFB_RuleID = r.mId;
                                    Rlog.d(LOG_TAG, "Update CFB_RuleID=" + CFB_RuleID);
                                } else if (cond.comprehendNoAnswer()) {
                                    CFNoAnswer_RuleID = r.mId;
                                    Rlog.d(LOG_TAG, "Update CFNoAnswer_RuleID="
                                            + CFNoAnswer_RuleID);
                                } else if (cond.comprehendNotReachable()) {
                                    CFNotReachable_RuleID = r.mId;
                                    Rlog.d(LOG_TAG, "Update CFNotReachable_RuleID="
                                            + CFNotReachable_RuleID);
                                } else if (cond.comprehendNotRegistered()) {
                                    CFNotRegistered_RuleID = r.mId;
                                    Rlog.d(LOG_TAG, "Update CFNotRegistered_RuleID="
                                            + CFNotRegistered_RuleID);
                                } else {
                                    CFU_RuleID = r.mId;
                                    Rlog.d(LOG_TAG, "Update CFU_RuleID=" + CFU_RuleID);
                                }
                            } else {
                                Rlog.d(LOG_TAG, "handleSetCF():Empty cond (cond==null) for"
                                        + " this rule=" + r);
                                if (CFU_RuleID.equals("CFU")) {
                                    //CFU rule
                                    CFU_RuleID = r.mId;
                                    Rlog.d(LOG_TAG, "Update CFU_RuleID=" + CFU_RuleID);
                                }
                            }

                            //Traverse each rule and check if this time's request is already in the
                            // remote XCAP server
                            if (setCFReason == CommandsInterface.CF_REASON_BUSY &&
                                    (cond != null && cond.comprehendBusy() == true) &&
                                    containSpecificMedia(mediaList, setCFServiceClass)) {

                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE) ||
                                        (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFBWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, setCFReason, setCFAction, setCFServiceClass,
                                            setCFNumber, setCFTimeSeconds, CFB_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFB-addedNewRule="
                                            + addedNewRule);
                                    if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFBWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFBWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFB with serviceClass=0 "
                                            + "case previously");
                                }

                            } else if (setCFReason == CommandsInterface.CF_REASON_NO_REPLY &&
                                    (cond != null && cond.comprehendNoAnswer() == true) &&
                                    containSpecificMedia(mediaList, setCFServiceClass)) {

                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                        || (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFNoAnswerWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, setCFReason, setCFAction, setCFServiceClass,
                                            setCFNumber, setCFTimeSeconds, CFNoAnswer_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFNoAnswer-addedNewRule="
                                            + addedNewRule);

                                    //Add the configuration for NoReplyTimer
                                    if (addedNewRule == true &&
                                            (setCFAction == CommandsInterface.CF_ACTION_ENABLE ||
                                            setCFAction ==
                                            CommandsInterface.CF_ACTION_REGISTRATION)) {
                                        Rlog.d(LOG_TAG, "handleSetCF():[C1]Enable CFNoAnswer with "
                                                + "new_NoReplyTimer=" + setCFTimeSeconds
                                                + "org_NoReplyTimer=" + cd.getNoReplyTimer());
                                        if (setCFTimeSeconds > 0 && cd.getNoReplyTimer() > -1) {
                                            cd.setNoReplyTimer(setCFTimeSeconds);
                                        } else {
                                            Rlog.d(LOG_TAG, "No need to append setCFTimeSeconds: " + 
                                                setCFTimeSeconds);
                                        }
                                    }

                                    if (setCFServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFNoAnswerWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFNoAnswerWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFNoAnswer with"
                                            + " serviceClass=0 case previously");
                                }

                            } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REACHABLE &&
                                    (cond != null && cond.comprehendNotReachable() == true) &&
                                    containSpecificMedia(mediaList, setCFServiceClass)) {

                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                        || (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFNotReachableWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, setCFReason, setCFAction, setCFServiceClass,
                                            setCFNumber, setCFTimeSeconds, CFNotReachable_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFNoReachable-addedNewRule="
                                            + addedNewRule);
                                    if (setCFServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFNotReachableWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFNotReachableWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFNoReachable with "
                                            + "serviceClass=0 case previously");
                                }

                            } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REGISTERED &&
                                    (cond != null && cond.comprehendNotReachable() == true) &&
                                    containSpecificMedia(mediaList, setCFServiceClass) &&
                                    MMTelSSUtils.isOp06IccCard(phoneId)) {
                                // set CFNRc as CFNL for OP06
                                Rlog.d(LOG_TAG, "handleSetCF(): Set CFNRc as CFNL for OP06");
                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                        || (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFNotReachableWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, CommandsInterface.CF_REASON_NOT_REACHABLE,
                                            setCFAction, setCFServiceClass, setCFNumber,
                                            setCFTimeSeconds, CFNotReachable_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFNoReachable-addedNewRule="
                                            + addedNewRule);
                                    if (setCFServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFNotReachableWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFNotReachableWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFNoReachable with "
                                            + "serviceClass=0 case previously");
                                }
                            } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REGISTERED &&
                                    (cond != null && cond.comprehendNotRegistered() == true) &&
                                    containSpecificMedia(mediaList, setCFServiceClass)) {

                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                        || (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFNotRegisteredWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, setCFReason, setCFAction, setCFServiceClass,
                                            setCFNumber, setCFTimeSeconds, CFNotRegistered_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFNoRegistered-addedNewRule="
                                            + addedNewRule);
                                    if (setCFServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFNotRegisteredWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFNotRegisteredWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFNoRegistered with "
                                            + "serviceClass=0 case previously");
                                }
                            } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REACHABLE &&
                                    (cond != null && cond.comprehendNotRegistered() == true) &&
                                    containSpecificMedia(mediaList, setCFServiceClass) &&
                                    MMTelSSUtils.isOp06IccCard(phoneId)) {
                                // set CFNL as CFNRc for OP06
                                Rlog.d(LOG_TAG, "handleSetCF(): Set CFNL as CFNRc for OP06");
                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                        || (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFNotRegisteredWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, CommandsInterface.CF_REASON_NOT_REGISTERED,
                                            setCFAction, setCFServiceClass, setCFNumber,
                                            setCFTimeSeconds, CFNotRegistered_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFNoRegistered-addedNewRule="
                                            + addedNewRule);
                                    if (setCFServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFNotRegisteredWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFNotRegisteredWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFNoRegistered with "
                                            + "serviceClass=0 case previously");
                                }
                            } else if (setCFReason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                                    ((cond != null && cond.comprehendBusy() == false &&
                                    cond.comprehendNoAnswer() == false &&
                                    cond.comprehendNotRegistered() == false &&
                                    cond.comprehendNotReachable() == false) || cond == null) &&
                                    containSpecificMedia(mediaList, setCFServiceClass)) {

                                if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                        || (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE
                                        && AddRuleForCFUWithAllMediaType == false)) {
                                    addedNewRule = handleCreateNewRuleForExistingCF(cd, newRuleSet,
                                            r, setCFReason, setCFAction, setCFServiceClass,
                                            setCFNumber, setCFTimeSeconds, CFU_RuleID,
                                            mUpdateSingleRule, num_of_expansion, phoneId);
                                    Rlog.d(LOG_TAG, "handleSetCF():CFU-addedNewRule="
                                            + addedNewRule);
                                    if (setCFServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE) {
                                        AddRuleForCFUWithAllMediaType = true;
                                    }
                                } else if (setCFServiceClass ==
                                        CommandsInterface.SERVICE_CLASS_NONE &&
                                        AddRuleForCFUWithAllMediaType == true) {
                                    Rlog.d(LOG_TAG, "Already add rule for CFU with serviceClass=0 "
                                            + "case previously");
                                }

                            } else {
                                //Copy old rule into new rule set
                                Rlog.d(LOG_TAG, "handleSetCF():Copy old rule to newRuleSet");
                                copyOldRuleToNewRuleSet(r, newRuleSet, setCFAction, setCFReason);
                            }
                        } //end-of-for-loop (ruleList)
                    }

                    //Check if the new rule user wants to be modified is already added to or not
                    //Remove the check condition "(setCFServiceClass !=
                    //CommandsInterface.SERVICE_CLASS_NONE)" 2014-0709
                    //Scenario:User inputs "**67#" to enable CFB (i.e.,with null phonenumber and
                    // serviceClass is NONE) => Not match any rule, it should be added into the
                    // rule set
                    if (addedNewRule == false && (setCFAction == CommandsInterface.CF_ACTION_ENABLE
                            || setCFAction == CommandsInterface.CF_ACTION_REGISTRATION)
                            /*&& (setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE)*/
                            ) {
                        addedNewRule = true;
                        Rule rule = newRuleSet.createNewRule("");
                        Conditions cond = rule.createConditions();
                        Actions action = rule.createActions();
                        if (MMTelSSUtils.isOp01IccCard(phoneId)
                                && MMTelSSUtils.isNotifyCallerTest()) {
                            action.setFowardTo(setCFNumber, false);
                        } else {
                            action.setFowardTo(setCFNumber, true);
                        }
                        action.getFowardTo().setRevealIdentityToCaller(true);
                        action.getFowardTo().setRevealIdentityToTarget(true);

                        Rlog.d(LOG_TAG, "handleSetCF():Add rule for this time's enable reason="
                                + setCFReason + ",serviceClass=" + setCFServiceClass);

                        if (setCFReason == CommandsInterface.CF_REASON_BUSY) {
                            rule.setId(CFB_RuleID);
                            cond.addBusy();
                        } else if (setCFReason == CommandsInterface.CF_REASON_NO_REPLY) {
                            //Add the configuration for NoReplyTimer
                            Rlog.d(LOG_TAG, "handleSetCF():[C2]Enable CFNoAnswer with "
                                    + "new_NoReplyTimer=" + setCFTimeSeconds + ",org_NoReplyTimer="
                                    + cd.getNoReplyTimer());
                            if (setCFTimeSeconds > 0 && cd.getNoReplyTimer() > -1) {
                                cd.setNoReplyTimer(setCFTimeSeconds);
                            } else {
                                Rlog.d(LOG_TAG, "No need to append setCFTimeSeconds: " + 
                                    setCFTimeSeconds);
                            }
                            rule.setId(CFNoAnswer_RuleID);
                            cond.addNoAnswer();
                        } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REACHABLE) {
                            rule.setId(CFNotReachable_RuleID);
                            cond.addNotReachable();
                        } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REGISTERED) {
                            rule.setId(CFNotRegistered_RuleID);
                            cond.addNotRegistered();
                        } else if (setCFReason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                            rule.setId(CFU_RuleID);
                            //Don't add any condition (always evaluate the result as true)
                        }
                        if (!MMTelSSUtils.isOp03IccCard(phoneId)
                            && !MMTelSSUtils.isOp05IccCard(phoneId)
                            && !MMTelSSUtils.isOp06IccCard(phoneId)
                            && !MMTelSSUtils.isOp07IccCard(phoneId)
                            && !MMTelSSUtils.isOp15IccCard(phoneId)) {
                            if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                                cond.addMedia("audio");
                            } else if (setCFServiceClass
                                    == CommandsInterface.SERVICE_CLASS_VIDEO) {
                                cond.addMedia("video");
                            }
                        }

                        if (mUpdateSingleRule && num_of_expansion == 1) {
                            cd.saveRule(rule.mId);
                        }
                    }

                    //Finally, update the new rule set back to remote XCAP server
                    //Debug:
                    if (newRuleSet.getRules() != null) {
                        Rlog.d(LOG_TAG, "Dump SetCF XML:" + newRuleSet.toXmlString());
                    } else {
                        Rlog.d(LOG_TAG, "Dump SetCF XML: ruleset with empty rules");
                    }

                    if (!mUpdateSingleRule) {
                        cd.saveRuleSet();
                    } else {
                        if (num_of_expansion > 1) {
                            List<Rule> newRuleList = null;
                            newRuleList = newRuleSet.getRules();
                            for (int i = 0; i < newRuleList.size(); i++) {
                                Rule newRule = newRuleList.get(i);
                                cd.saveRule(newRule.mId);
                            }
                        }
                    }

                } //end-of-for-loop (num_of_expansion)

                // Workaround for Op05 to check the result from CFU.
                if (MMTelSSUtils.isOp05IccCard(phoneId)
                    && setCFReason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                    cd.refresh();

                    infos = parseCFUInfoFromCD(cd, setCFReason, setCFServiceClass);
                }

            } catch (XcapException xcapException) {
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;
                if (xcapException.getHttpErrorCode() == HTTP_ERROR_CODE_412) {
                    Rlog.d(LOG_TAG, "handleSetCF(): HTTP_ERROR_CODE_412");
                    return HTTP_ERROR_CODE_412;
                }
                Rlog.d(LOG_TAG, "handleSetCF(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCF(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCF(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            } catch (Exception e) {
                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCF():Start to Print Stack Trace");
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                e.printStackTrace();
                if ((rr.mResult != null) && (reportFlag == 1)) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            }

            //[Notify upper's application about the SET_CF result - Success Case without exception]
            if ((rr.mResult != null) && (reportFlag == 1)) {
                AsyncResult.forMessage(rr.mResult, infos, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }

            return 0;
        }

        private CallForwardInfo[] parseCFUInfoFromCD(CommunicationDiversion cd,
            int cfReason, int cfServiceClass) {

            CallForwardInfo infos[] = null;
            int serviceClass = cfServiceClass;
            int orgServiceClass = serviceClass;
            String CFPhoneNum = "";

            ArrayList<CallForwardInfo> queriedCallForwardInfoList;
            queriedCallForwardInfoList = new ArrayList<CallForwardInfo>();
            int queryStatus = 0;

            RuleSet ruleSet = cd.getRuleSet();

            List<Rule> ruleList = null;

            if (ruleSet != null) {
                ruleList = ruleSet.getRules();
            } else {
                Rlog.d(LOG_TAG, "parseCFUInfoFromCD: No CF related rules in remote server");
            }


            int numOfExpansion = 1;
            if (ruleList != null) {

                numOfExpansion = 1;
                if (cfReason == CommandsInterface.CF_REASON_ALL_CONDITIONAL) {
                    //In this case: User inputs "*#004#"
                    //CF Conditional (BUSY/NO_REPLY/NOT_REACHABLE/NOT_REGISTERED)x4
                    numOfExpansion = 4;
                } else if (cfReason == CommandsInterface.CF_REASON_ALL) {
                    //CF Conditional (BUSY/NO_REPLY/NOT_REACHABLE/NOT_REGISTERED)x4 + CFUx1
                    //In this case: User inputs "*#002#"
                    numOfExpansion = 5;
                }

                for (int n = 0; n < numOfExpansion; n++) {
                    if (numOfExpansion != 1) {
                        if (n == 0) cfReason = CommandsInterface.CF_REASON_BUSY;
                        else if (n == 1) cfReason = CommandsInterface.CF_REASON_NO_REPLY;
                        else if (n == 2) cfReason = CommandsInterface.CF_REASON_NOT_REACHABLE;
                        else if (n == 3) cfReason = CommandsInterface.CF_REASON_NOT_REGISTERED;
                        else if (n == 4) cfReason = CommandsInterface.CF_REASON_UNCONDITIONAL;
                    }

                    Rlog.d(LOG_TAG, "parseCFUInfoFromCD(): numOfExpansion="
                            + numOfExpansion + ": with round="
                            + (n + 1) + ",with reason=" + cfReason);

                    //Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
                    if (orgServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO
                            | CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                        serviceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                    }

                    //E.g.,GsmMmiCode:MMI String can be :*#21#[CFU] or *#67#[CFB] or
                    // *#61#[CF NoAnswer] or *#62#[CF Not Reachable]
                    //=> Without serviceClass information, then serviceClass=0 is passed to
                    // MMTelSS
                    int num_of_comparision = 0;

                    if (orgServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                        serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                        //one is to match audio (i.e., serviceClass = SERVICE_CLASS_VOICE), the
                        // other is to match video (i.e., SERVICE_CLASS_VIDEO)
                        num_of_comparision = 2;
                        Rlog.d(LOG_TAG, "parseCFUInfoFromCD(): "
                                + "serviceClass==0, try to 1st match by using"
                                + " SERVICE_CLASS_VOICE");
                    } else {
                        //Specific serviceClass (i.e., value is not 0) is carried from the
                        // upper layer
                        num_of_comparision = 1;
                    }

                    for (int it = 0; it < num_of_comparision; it++) {

                        if (it == 1 && serviceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                            //2nd time to match all rules by using SERVICE_CLASS_VIDEO
                            serviceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                            Rlog.d(LOG_TAG, "parseCFUInfoFromCD(): "
                                    + "serviceClass==0, try to 2nd match by using "
                                    + "SERVICE_CLASS_VIDEO");
                        }

                        Rlog.d(LOG_TAG, "parseCFUInfoFromCD: num_of_comparision=" + num_of_comparision
                                + ": with round=" + (it + 1) + ",with service class="
                                + serviceClass);

                        //Check each rule & its corresponding condition/action
                        //Current we only support CFU, not consider other CF.
                        for (int i = 0; i < ruleList.size(); i++) {
                            Rule r = ruleList.get(i);
                            Conditions cond = r.getConditions();
                            Actions action = r.getActions();
                            List<String> mediaList = null;

                            if (cond != null) {
                                Rlog.d(LOG_TAG, "parseCFUInfoFromCD():busy=" + cond.comprehendBusy()
                                        + ",NoAnswer=" + cond.comprehendNoAnswer()
                                        + ",NoReachable=" + cond.comprehendNotReachable()
                                        + ",NotRegistered=" + cond.comprehendNotRegistered());
                                mediaList = cond.getMedias();
                            } else {
                                Rlog.d(LOG_TAG, "handleGetCF():Empty cond (cond==null) "
                                        + "for this rule=" + r);
                            }

                            //See queryCallForwardStatus(): cfAction is always set to 2
                            if (cfReason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                                    ((cond != null && cond.comprehendBusy() == false &&
                                    cond.comprehendNoAnswer() == false &&
                                    cond.comprehendNotRegistered() == false &&
                                    cond.comprehendNotReachable() == false) &&
                                    cond.comprehendRuleDeactivated() == false || cond == null)
                                    && containSpecificMedia(mediaList, serviceClass)) {
                                Rlog.d(LOG_TAG, "parseCFUInfoFromCD():CFU is enabled on server");
                                //[CFU]CFU is enabled, set queryStatus as 1
                                queryStatus = 1;
                                if (action.getFowardTo() != null) {
                                    CFPhoneNum = action.getFowardTo().getTarget();
                                }
                                break;

                            } else {
                                //Something wrong!
                                Rlog.d(LOG_TAG, "parseCFUInfoFromCD()from xcap:Not matched "
                                        + "this rule!");
                            }

                        } //end of for-loop(ruleList.size())
                        //Add this queried & matched result into the matchedCallForwardInfoList
                        CallForwardInfo item = new CallForwardInfo();
                        item.status = queryStatus;
                        item.reason = cfReason;
                        item.serviceClass = serviceClass;
                        item.toa = 0;
                        item.number = CFPhoneNum;
                        item.timeSeconds = 0;
                        Rlog.d(LOG_TAG, "parseCFUInfoFromCD():add one record with reason=" + cfReason
                                + ",serviceClass=" + serviceClass + ",queryStatus="
                                + queryStatus);
                        queriedCallForwardInfoList.add(item);

                        //Reset some variables for this matching result
                        queryStatus = 0; //0: DISABLE, 1: ENABLE
                        CFPhoneNum = "";
                    } //end of for-loop(num_of_comparision)

                } //end of for-loop(num_of_expansion)
                int queriedSize = queriedCallForwardInfoList.size();

                infos = new CallForwardInfo[queriedSize];
                for (int inx = 0; inx < queriedSize; inx++) {
                    infos[inx] = (CallForwardInfo) queriedCallForwardInfoList.get(inx);
                }
            } else {
                //Empty XML String:CF is disabled, set queryStatus as 0
                Rlog.d(LOG_TAG, "parseCFUInfoFromCD():get null ruleList");
                infos = new CallForwardInfo[0];
                queryStatus = 0;
            }
            return infos;
        }

        public int handleSetCB(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();
            String facility = rr.mp.readString();
            String original_facility = facility;
            int lockState = rr.mp.readInt();
            int setCBServiceClass = rr.mp.readInt();
            int phoneId = rr.mp.readInt();

            boolean AddRuleForBAOCWithAllMediaType = false;
            boolean AddRuleForBAOICWithAllMediaType = false;
            boolean AddRuleForBAOICxHWithAllMediaType = false;
            boolean AddRuleForBAICWithAllMediaType = false;
            boolean AddRuleForBAICrWithAllMediaType = false;

            String BAOC_RuleID = "AO";
            String BAOIC_RuleID = "OI";
            String BAOICExHC_RuleID = "OX";
            String BAIC_RuleID = "AI";
            String BAICR_RuleID = "IR";

            Rlog.d(LOG_TAG, "Read from CB parcel:req=" + requestToString(reqNo) + ",facility="
                   + facility + ",serviceClass=" + setCBServiceClass + ",lockState(enabled)="
                   + lockState);

            //Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
            if (setCBServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO
                    | CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                setCBServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
            }

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCB(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return 0;
            }

            try {

                //Add handling Barring Service for 330(AB)/333(AG)/353(AC) cases
                /***
                 * "AO" BAOC (Barr All Outgoing Calls) (refer 3GPP TS 22.088 [6] clause 1)
                 * "OI" BOIC (Barr Outgoing International Calls) (refer 3GPP TS 22.088 [6]
                 * clause 1)
                 * "OX" BOIC exHC (Barr Outgoing International Calls except to Home Country)
                 *(refer 3GPP TS 22.088 [6] clause 1)
                 * "AI" BAIC (Barr All Incoming Calls) (refer 3GPP TS 22.088 [6] clause 2)
                 * "IR" BIC Roam (Barr Incoming Calls when Roaming outside the home country)
                 * (refer 3GPP TS 22.088 [6] clause 2)
                 * "AB" All Barring services (refer 3GPP TS 22.030 [19]) (applicable only for
                 * <mode>=0: i.e.,unlock - ref: +CLCK & 2/3G SS Spec)
                 * "AG" All outGoing barring services (refer 3GPP TS 22.030 [19]) (applicable
                 * only for <mode>=0: i.e.,unlock - ref: +CLCK & 2/3G SS Spec)
                 * "AC" All inComing barring services (refer 3GPP TS 22.030 [19]) (applicable
                 * only for <mode>=0: i.e.,unlock - ref: +CLCK & 2/3G SS Spec)
                 */
                int num_of_expansion = 1;
                if (original_facility.equals(CommandsInterface.CB_FACILITY_BA_ALL)
                        && lockState == 0) {
                    num_of_expansion = 5;
                } else if (original_facility.equals(CommandsInterface.CB_FACILITY_BA_MO)
                        && lockState == 0) {
                    num_of_expansion = 3;
                } else if (original_facility.equals(CommandsInterface.CB_FACILITY_BA_MT)
                        && lockState == 0) {
                    num_of_expansion = 2;
                }

                if (facility.equals(CommandsInterface.CB_FACILITY_BA_ALL)
                        || facility.equals(CommandsInterface.CB_FACILITY_BA_MO)
                        || facility.equals(CommandsInterface.CB_FACILITY_BA_MT)) {
                    if (lockState != 0) {
                        //Follow the same behavior with 2/3G CS - AB/AG/AC:Only mode=0
                        // (i.e.,unlock state operation) is allowed
                        Rlog.d(LOG_TAG, "Not allow lockState=1 for AB(330)/AG(333)/AC(353)");
                        //Note that: upper layer application
                        // (CallBarringBasePreference.java:handleSetCallBarringResponse())
                        //Only cast the exception to CommandException is allowed (but it may
                        //happen assertion due to cast failure)
                        //And handle this exception in TimeConsumingPreferenceActivity.java's
                        // onError()
                        if (rr.mResult != null) {
                            CommandException ce =
                                    CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                            AsyncResult.forMessage(rr.mResult, null, ce);
                            rr.mResult.sendToTarget();
                            return 0;
                        }
                    }
                }

                OutgoingCommunicationBarring ocb = null;
                IncomingCommunicationBarring icb = null;
                long curTime = System.currentTimeMillis();
                if (facility.equals(CommandsInterface.CB_FACILITY_BAOC) ||
                        facility.equals(CommandsInterface.CB_FACILITY_BAOIC) ||
                        facility.equals(CommandsInterface.CB_FACILITY_BAOICxH) ||
                        facility.equals(CommandsInterface.CB_FACILITY_BA_ALL)) {
                    Rlog.d(LOG_TAG, "handleSetCB(): mOcbCache = " + mOcbCache + ", curTime = "
                            + curTime + ", mOcbCacheLastQueried = " + mOcbCacheLastQueried
                            + ", facility = " + facility + ", phoneId = " + phoneId);
                        //Outgoing Call Barring
                        mXui = MMTelSSUtils.getXui(phoneId, mContext);
                        mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                        mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);
                        if (TextUtils.isEmpty(mXcapRoot)) {
                            Rlog.d(LOG_TAG, "handleSetCB(): OCB XcapRoot = " + mXcapRoot);
                            if (null != rr.mResult) {
                                AsyncResult.forMessage(rr.mResult, null,
                                        new UnknownHostException());
                                rr.mResult.sendToTarget();
                            }
                            return 0;
                        }
                        requestXcapNetwork(phoneId);
                        setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName,
                                mPassword, phoneId);
                    if ((mOcbCache != null) && (phoneId == mOcbCachePhoneId) &&
                            (mOcbCache.isSupportEtag())) {
                        Rlog.d(LOG_TAG, "handleSetCB(): using ETAG mOcbCache: " + mOcbCache);
                        ocb = mOcbCache;
                        ocb.setNetwork(mNetwork);
                        ocb.refresh();
                        mOcbCacheLastQueried = curTime;
                    } else if ((mOcbCache != null) && (phoneId == mOcbCachePhoneId) &&
                            ((curTime >= mOcbCacheLastQueried)
                            && ((curTime - mOcbCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                        Rlog.d(LOG_TAG, "handleSetCB(): using mOcbCache: " + mOcbCache);
                        ocb = mOcbCache;
                        ocb.setNetwork(mNetwork);
                    } else {
                        ocb = mSimservs.getOutgoingCommunicationBarring(true, mNetwork);
                        mOcbCache = ocb;
                        mOcbCachePhoneId = phoneId;
                        mOcbCacheLastQueried = curTime;
                        Rlog.d(LOG_TAG, "handleSetCB(): new mOcbCache = " + mOcbCache
                                + ", curTime = " + curTime);
                    }
                }

                if (facility.equals(CommandsInterface.CB_FACILITY_BAIC) ||
                        facility.equals(CommandsInterface.CB_FACILITY_BAICr) ||
                        facility.equals(CommandsInterface.CB_FACILITY_BA_ALL)) {
                    Rlog.d(LOG_TAG, "handleSetCB(): mIcbCache = " + mIcbCache + ", curTime = "
                            + curTime + ", mIcbCacheLastQueried = " + mIcbCacheLastQueried
                            + ", facility = " + facility + ", phoneId = " + phoneId);

                        //Incoming Call Barring
                        mXui = MMTelSSUtils.getXui(phoneId, mContext);
                        mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                        mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                        if (TextUtils.isEmpty(mXcapRoot)) {
                            Rlog.d(LOG_TAG, "handleSetCB(): ICB XcapRoot = " + mXcapRoot);
                            if (null != rr.mResult) {
                                AsyncResult.forMessage(rr.mResult, null,
                                        new UnknownHostException());
                                rr.mResult.sendToTarget();
                            }
                            return 0;
                        }
                        requestXcapNetwork(phoneId);
                        setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName,
                                mPassword, phoneId);
                    if ((mIcbCache != null) && (phoneId == mIcbCachePhoneId) &&
                            (mIcbCache.isSupportEtag())) {
                        Rlog.d(LOG_TAG, "handleSetCB(): using ETAG mIcbCache: " + mIcbCache);
                        icb.refresh();
                        mIcbCacheLastQueried = curTime;
                    } else if ((mIcbCache != null) && (phoneId == mIcbCachePhoneId) &&
                            ((curTime >= mIcbCacheLastQueried)
                            && ((curTime - mIcbCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                        Rlog.d(LOG_TAG, "handleSetCB(): using mIcbCache: " + mIcbCache);
                        icb = mIcbCache;
                        icb.setNetwork(mNetwork);
                    } else {
                        icb = mSimservs.getIncomingCommunicationBarring(true, mNetwork);
                        mIcbCache = icb;
                        mIcbCachePhoneId = phoneId;
                        mIcbCacheLastQueried = curTime;
                        Rlog.d(LOG_TAG, "handleSetCB(): new mIcbCache = " + mIcbCache
                                + ", curTime = " + curTime);
                    }
                }


                for (int it = 0; it < num_of_expansion; it++) {
                    if (num_of_expansion != 1) {
                        if (original_facility.equals(CommandsInterface.CB_FACILITY_BA_MO)) {
                            if (it == 0) facility = CommandsInterface.CB_FACILITY_BAOIC;
                            else if (it == 1) facility = CommandsInterface.CB_FACILITY_BAOICxH;
                            else if (it == 2) facility = CommandsInterface.CB_FACILITY_BAOC;

                        } else if (original_facility.equals(CommandsInterface.CB_FACILITY_BA_MT)) {
                            if (it == 0) facility = CommandsInterface.CB_FACILITY_BAICr;
                            else if (it == 1) facility = CommandsInterface.CB_FACILITY_BAIC;

                        } else if (original_facility.equals(CommandsInterface.CB_FACILITY_BA_ALL)) {
                            if (it == 0) facility = CommandsInterface.CB_FACILITY_BAOIC;
                            else if (it == 1) facility = CommandsInterface.CB_FACILITY_BAOICxH;
                            else if (it == 2) facility = CommandsInterface.CB_FACILITY_BAOC;
                            else if (it == 3) facility = CommandsInterface.CB_FACILITY_BAICr;
                            else if (it == 4) facility = CommandsInterface.CB_FACILITY_BAIC;
                        }
                    }

                    Rlog.d(LOG_TAG, "handleSetCB():num_of_expansion=" + num_of_expansion
                            + ", round=" + it + ",for facility=" + facility + ",with lockState="
                            + lockState);

                    if (facility.equals(CommandsInterface.CB_FACILITY_BAOC) ||
                            facility.equals(CommandsInterface.CB_FACILITY_BAOIC) ||
                            facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
                        RuleSet oRuleSet = ocb.getRuleSet();
                        List<Rule> ruleList = null;
                        RuleSet newRuleSet = ocb.createNewRuleSet();
                        boolean addedNewRule = false;

                        if (oRuleSet != null) {
                            ruleList = oRuleSet.getRules();
                        } else {
                            Rlog.d(LOG_TAG, "No MO related CB rules in remote server");
                        }

                        //Note that: If no ant configuration is stored in XCAP server (e.g., empty
                        // xml string), ruleList will be null
                        if (ruleList != null) {
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                Conditions cond = r.getConditions();
                                Actions action = r.getActions();
                                List<String> mediaList = null;

                                if (cond != null) {
                                    Rlog.d(LOG_TAG, "handleSetCB():MO-facility=" + facility
                                            + ",action=" + action.isAllow() + ",international="
                                            + cond.comprehendInternational()
                                            + ",internationalExHC="
                                            + cond.comprehendInternationalExHc());
                                    mediaList = cond.getMedias();
                                    if (cond.comprehendInternational()) {
                                        BAOIC_RuleID = r.mId;
                                        Rlog.d(LOG_TAG, "Update BAOIC_RuleID=" + BAOIC_RuleID);
                                    } else if (cond.comprehendInternationalExHc()) {
                                        BAOICExHC_RuleID = r.mId;
                                        Rlog.d(LOG_TAG, "Update BAOICExHC_RuleID="
                                            + BAOICExHC_RuleID);
                                    } else {
                                        BAOC_RuleID = r.mId;
                                        Rlog.d(LOG_TAG, "Update BAOC_RuleID=" + BAOC_RuleID);
                                    }
                                } else {
                                    //Add by mtk01411: 2014-0123
                                    if (cond == null
                                            && facility.equals(
                                            CommandsInterface.CB_FACILITY_BAOC)) {
                                        Rlog.d(LOG_TAG, "handleSetCB():cond=null"
                                                + " but AO case!MO-facility="
                                                + facility + ",action=" + action.isAllow());
                                        mediaList = null;
                                        if (BAOC_RuleID.equals("AO")) {
                                            BAOC_RuleID = r.mId;
                                            Rlog.d(LOG_TAG, "Update BAOC_RuleID=" + BAOC_RuleID);
                                        }
                                    } else {
                                        Rlog.d(LOG_TAG, "handleSetCB():Empty MO cond "
                                                + "(cond==null) for this rule=" + r);
                                        if (BAOC_RuleID.equals("AO")) {
                                            BAOC_RuleID = r.mId;
                                            Rlog.d(LOG_TAG, "Update BAOC_RuleID=" + BAOC_RuleID);
                                        }
                                    }
                                }

                                if (facility.equals(CommandsInterface.CB_FACILITY_BAOIC) &&
                                        (cond != null && cond.comprehendInternational()) &&
                                        containSpecificMedia(mediaList, setCBServiceClass)) {

                                    if ((setCBServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                             || (setCBServiceClass
                                             == CommandsInterface.SERVICE_CLASS_NONE
                                             && AddRuleForBAOICWithAllMediaType == false)) {
                                        addedNewRule = handleCreateNewRuleForExistingCB(ocb,
                                                newRuleSet, r, facility, lockState,
                                                setCBServiceClass, BAOIC_RuleID, mUpdateSingleRule,
                                                num_of_expansion, phoneId);
                                        Rlog.d(LOG_TAG, "handleSetCB():OI-addedNewRule="
                                                + addedNewRule);
                                        //Add this check by mtk01411: If serviceClass=0, it already
                                        // adds alllow or disallow tag for all mediatype in
                                        // handleCreateNewRuleForExistingCB()
                                        if (setCBServiceClass !=
                                                CommandsInterface.SERVICE_CLASS_NONE) {
                                            Rule newRule =
                                                    copyOldRuleToNewRuleSetExceptSpecificMedia(r,
                                                    newRuleSet, setCBServiceClass, phoneId, -1, -1);
                                            if ((null != newRule) && mUpdateSingleRule
                                                    && (1 == num_of_expansion)) {
                                                ocb.saveRule(newRule.mId);
                                            }
                                        } else {
                                            AddRuleForBAOICWithAllMediaType = true;
                                        }
                                    } else if (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAOICWithAllMediaType == true) {
                                        Rlog.d(LOG_TAG, "Already add rule for BAOIC with"
                                                + " serviceClass=0 case previously");
                                    }

                                } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)
                                        && (cond != null && cond.comprehendInternationalExHc())
                                        && containSpecificMedia(mediaList, setCBServiceClass)) {

                                    if ((setCBServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                            || (setCBServiceClass
                                            == CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAOICxHWithAllMediaType == false)) {
                                        addedNewRule = handleCreateNewRuleForExistingCB(ocb,
                                                newRuleSet, r, facility, lockState,
                                                setCBServiceClass, BAOICExHC_RuleID,
                                                mUpdateSingleRule, num_of_expansion, phoneId);
                                        Rlog.d(LOG_TAG, "handleSetCB():OX-addedNewRule="
                                                + addedNewRule);
                                        //Add this check by mtk01411: If serviceClass=0, it already
                                        // adds alllow or disallow tag for all mediatype in
                                        //handleCreateNewRuleForExistingCB()
                                        if (setCBServiceClass !=
                                                CommandsInterface.SERVICE_CLASS_NONE) {
                                            Rule newRule =
                                                    copyOldRuleToNewRuleSetExceptSpecificMedia(r,
                                                    newRuleSet, setCBServiceClass, phoneId, -1, -1);
                                            if ((null != newRule) && mUpdateSingleRule
                                                    && (1 == num_of_expansion)) {
                                                ocb.saveRule(newRule.mId);
                                            }
                                        } else {
                                            AddRuleForBAOICxHWithAllMediaType = true;
                                        }
                                    } else if (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAOICxHWithAllMediaType == true) {
                                        Rlog.d(LOG_TAG, "Already add rule for BAOICxH with " +
                                                "serviceClass=0 case previously");
                                    }

                                } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOC) &&
                                        isBAOC(cond, setCBServiceClass)) {

                                    if ((setCBServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                            || (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE &&
                                            AddRuleForBAOCWithAllMediaType == false)) {
                                        addedNewRule = handleCreateNewRuleForExistingCB(ocb,
                                                newRuleSet, r, facility, lockState,
                                                setCBServiceClass, BAOC_RuleID, mUpdateSingleRule,
                                                num_of_expansion, phoneId);
                                        Rlog.d(LOG_TAG, "handleSetCB():AO-addedNewRule="
                                                + addedNewRule);
                                        //Add this check by mtk01411: If serviceClass=0, it already
                                        // adds alllow or disallow tag for all mediatype in
                                        // handleCreateNewRuleForExistingCB()
                                        if (setCBServiceClass !=
                                                CommandsInterface.SERVICE_CLASS_NONE) {
                                            Rule newRule =
                                                     copyOldRuleToNewRuleSetExceptSpecificMedia(r,
                                                     newRuleSet, setCBServiceClass, phoneId, -1, -1);
                                            if ((null != newRule) && mUpdateSingleRule
                                                    && (1 == num_of_expansion)) {
                                                ocb.saveRule(newRule.mId);
                                            }
                                        } else {
                                            AddRuleForBAOCWithAllMediaType = true;
                                        }
                                    } else if (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAOCWithAllMediaType == true) {
                                        Rlog.d(LOG_TAG, "Already add rule for BAOC with"
                                                + " serviceClass=0 case previously");
                                    }

                                } else {
                                    //Copy old rule into new rule set
                                    Rlog.d(LOG_TAG, "handleSetCB():MO Copy old rule inot"
                                            + " newRuleSet");
                                    copyOldRuleToNewRuleSet(r, newRuleSet, -1, -1);
                                }
                            }
                        }

                        //Add this new setting into the ruleset
                        //[Scenario#1]For disable CB:Because the mDisableRuleMode is
                        // DISABLE_MODE_DELETE_RULEL: Not necessary to add rule for this
                        // serviceClass into new rule set
                        // -> In handleCreateNewRuleForReqCB():It will return directly if lockState
                        // is 0 (CB DISABLE)
                        //[Scenario#2]For enable CB instead of modification one existing rule's
                        // case: Because there is no any rule is matched in the original rule
                        // set, it must add a new one!
                        if (addedNewRule == false) {
                            //XML stored in remote XCAP server is empty string
                            //Use facility as the RuleID
                            //E.g., XCAP server only BAIC but this time's request is BAOC with
                            // serviceClass=0
                            Rlog.d(LOG_TAG, "handleSetCB():MO add new rule for this time's"
                                    + " request-facility=" + facility + ",lockState="
                                    + lockState + ",serviceClass=" + setCBServiceClass);
                            String newRuleID = "";
                            //According to facility to decide the rule-id
                            if (facility.equals(CommandsInterface.CB_FACILITY_BAOC)) {
                                newRuleID = BAOC_RuleID;
                            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOIC)) {
                                newRuleID = BAOIC_RuleID;
                            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
                                newRuleID = BAOICExHC_RuleID;
                            }
                            Rlog.d(LOG_TAG, "handleSetCB():MO add new rule with id=" + newRuleID);
                            addedNewRule = handleCreateNewRuleForReqCB(ocb, newRuleSet, facility,
                                    lockState, setCBServiceClass, newRuleID, mUpdateSingleRule,
                                    num_of_expansion, phoneId);

                        }

                        //Finally, update the new rule set back to remote XCAP server
                        if (newRuleSet.getRules() != null) {
                            Rlog.d(LOG_TAG, "Dump MO SetCB  XML:" + newRuleSet.toXmlString());
                        } else {
                            Rlog.d(LOG_TAG, "Dump MO SetCB XML: ruleset with empty rules");
                        }

                        if (!mUpdateSingleRule) {
                            ocb.saveRuleSet();
                        } else {
                            if (num_of_expansion > 1) {
                                List<Rule> newRuleList = null;
                                newRuleList = newRuleSet.getRules();
                                for (int i = 0; i < newRuleList.size(); i++) {
                                    Rule newRule = newRuleList.get(i);
                                    ocb.saveRule(newRule.mId);
                                }
                            }
                        }
                    } else if (facility.equals(CommandsInterface.CB_FACILITY_BAIC) ||
                            facility.equals(CommandsInterface.CB_FACILITY_BAICr)) {
                        //Incoming Call Barring
                        RuleSet iRuleSet = icb.getRuleSet();
                        List<Rule> ruleList = null;
                        RuleSet newRuleSet = icb.createNewRuleSet();
                        boolean addedNewRule = false;

                        if (iRuleSet != null) {
                            ruleList = iRuleSet.getRules();
                        } else {
                            Rlog.d(LOG_TAG, "No MT related CB rules in remote server");
                        }

                        //Note that: If no ant configuration is stored in XCAP server (e.g., empty
                        // xml string), ruleList will be null
                        if (ruleList != null) {
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                Conditions cond = r.getConditions();
                                Actions action = r.getActions();
                                List<String> mediaList = null;

                                if (cond != null) {
                                    Rlog.d(LOG_TAG, "handleSetCB():MT-facility=" + facility
                                            + ",action=" + action.isAllow() + ",international="
                                            + cond.comprehendInternational() + ",roaming="
                                            + cond.comprehendRoaming());
                                    mediaList = cond.getMedias();
                                    if (cond.comprehendRoaming()) {
                                        BAICR_RuleID = r.mId;
                                        Rlog.d(LOG_TAG, "Update BAICR_RuleID=" + BAICR_RuleID);
                                    } else {
                                        BAIC_RuleID = r.mId;
                                        Rlog.d(LOG_TAG, "Update BAIC_RuleID=" + BAIC_RuleID);
                                    }
                                } else {
                                    Rlog.d(LOG_TAG, "handleSetCB():Empty MT cond (cond==null)"
                                            + " for this rule=" + r);
                                    if (BAIC_RuleID.equals("AI")) {
                                        BAIC_RuleID = r.mId;
                                        Rlog.d(LOG_TAG, "Update BAIC_RuleID=" + BAIC_RuleID);
                                    }
                                }

                                if (facility.equals(CommandsInterface.CB_FACILITY_BAICr) &&
                                        (cond != null && cond.comprehendRoaming() == true) &&
                                        containSpecificMedia(mediaList, setCBServiceClass)) {

                                    if ((setCBServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                            || (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAICrWithAllMediaType == false)) {
                                        //Change enable BAICr to disable BAICr
                                        addedNewRule = handleCreateNewRuleForExistingCB(icb,
                                                newRuleSet, r, facility, lockState,
                                                setCBServiceClass, BAICR_RuleID, mUpdateSingleRule,
                                                num_of_expansion, phoneId);
                                        Rlog.d(LOG_TAG, "handleSetCB():IR-addedNewRule="
                                                + addedNewRule);
                                        //Add this check by mtk01411: If serviceClass=0, it already
                                        // adds alllow or disallow tag for all mediatype in
                                        // handleCreateNewRuleForExistingCB()
                                        if (setCBServiceClass !=
                                                CommandsInterface.SERVICE_CLASS_NONE) {
                                            Rule newRule =
                                                    copyOldRuleToNewRuleSetExceptSpecificMedia(r,
                                                    newRuleSet, setCBServiceClass, phoneId, -1, -1);
                                            if ((null != newRule) && mUpdateSingleRule
                                                    && (1 == num_of_expansion)) {
                                                icb.saveRule(newRule.mId);
                                            }
                                        } else {
                                            AddRuleForBAICrWithAllMediaType = true;
                                        }
                                    } else if (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAICrWithAllMediaType == true) {
                                        Rlog.d(LOG_TAG, "Already add rule for BAICr with "
                                                + "serviceClass=0 case previously");
                                    }

                                } else if (facility.equals(CommandsInterface.CB_FACILITY_BAIC) &&
                                        isBAIC(cond, setCBServiceClass) &&
                                        containSpecificMedia(mediaList, setCBServiceClass)) {

                                    if ((setCBServiceClass != CommandsInterface.SERVICE_CLASS_NONE)
                                            || (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAICWithAllMediaType == false)) {
                                        addedNewRule = handleCreateNewRuleForExistingCB(icb,
                                                newRuleSet, r, facility, lockState,
                                                setCBServiceClass, BAIC_RuleID, mUpdateSingleRule,
                                                num_of_expansion, phoneId);
                                        Rlog.d(LOG_TAG, "handleSetCB():AI-addedNewRule="
                                                + addedNewRule);
                                        //Add this check by mtk01411: If serviceClass=0, it already
                                        // adds alllow or disallow tag for all mediatype in
                                        // handleCreateNewRuleForExistingCB()
                                        if (setCBServiceClass !=
                                                CommandsInterface.SERVICE_CLASS_NONE) {
                                            Rule newRule =
                                                    copyOldRuleToNewRuleSetExceptSpecificMedia(r,
                                                    newRuleSet,
                                                    setCBServiceClass, phoneId, -1, -1);
                                            if ((null != newRule) && mUpdateSingleRule
                                                    && (1 == num_of_expansion)) {
                                                icb.saveRule(newRule.mId);
                                            }
                                        } else {
                                            AddRuleForBAICWithAllMediaType = true;
                                        }
                                    } else if (setCBServiceClass ==
                                            CommandsInterface.SERVICE_CLASS_NONE
                                            && AddRuleForBAICWithAllMediaType == true) {
                                        Rlog.d(LOG_TAG, "Already add rule for BAIC with"
                                                + " serviceClass=0 case previously");
                                    }

                                } else {
                                    //Copy old rule into new rule set
                                    Rlog.d(LOG_TAG, "handleSetCB():MT Copy old rule inot "
                                            + "newRuleSet");
                                    copyOldRuleToNewRuleSet(r, newRuleSet, -1, -1);

                                }
                            }
                        }

                        //Add this new setting into the ruleset
                        if (addedNewRule == false) {
                            //XML stored in remote XCAP server is empty string
                            //Use facility as the RuleID
                            //E.g., XCAP server only BAICr but this time's request is BAIC with
                            // serviceClass=0
                            Rlog.d(LOG_TAG, "handleSetCB():MT add new rule for this time's "
                                    + "request-facility=" + facility + ",lockState=" + lockState
                                    + ",serviceClass=" + setCBServiceClass);
                            String newRuleID = "";
                            //According to facility to decide the rule-id
                            if (facility.equals(CommandsInterface.CB_FACILITY_BAIC)) {
                                newRuleID = BAIC_RuleID;
                            } else if (facility.equals(CommandsInterface.CB_FACILITY_BAICr)) {
                                newRuleID = BAICR_RuleID;
                            }
                            Rlog.d(LOG_TAG, "handleSetCB():MT add new rule with id=" + newRuleID);
                            addedNewRule = handleCreateNewRuleForReqCB(icb, newRuleSet, facility,
                                    lockState, setCBServiceClass, newRuleID, mUpdateSingleRule,
                                    num_of_expansion, phoneId);

                        }

                        //Finally, update the new rule set back to remote XCAP server
                        if (newRuleSet.getRules() != null) {
                            Rlog.d(LOG_TAG, "Dump MT SetCB XML:" + newRuleSet.toXmlString());
                        } else {
                            Rlog.d(LOG_TAG, "Dump MT SetCB XML: ruleset with empty rules");
                        }

                        if (!mUpdateSingleRule) {
                            icb.saveRuleSet();
                        } else {
                            if (num_of_expansion > 1) {
                                List<Rule> newRuleList = null;
                                newRuleList = newRuleSet.getRules();
                                for (int i = 0; i < newRuleList.size(); i++) {
                                    Rule newRule = newRuleList.get(i);
                                    icb.saveRule(newRule.mId);
                                }
                            }
                        }
                    } else if (facility.equals(CommandsInterface.CB_FACILITY_BA_ALL)
                            && lockState == 0) {
                        //Disable All Call Barring Cases (Triggered by
                        // CallBarringResetPreference.java)

                        RuleSet iNewRuleSet = null;
                        RuleSet oNewRuleSet = null;
                        RuleSet oldRuleSet = null;
                        List<Rule> ruleList = null;

                        //Note that:If no rule in RuleSet: In RuleSet.java's toXmlString(): get
                        // null rule -> Null Pointer Exception
                        //Read original rule first then copy each rule to new rule set but with
                        // allow as true!
                        oldRuleSet = ocb.getRuleSet();
                        if (oldRuleSet != null) {
                            ruleList = oldRuleSet.getRules();
                        } else {
                            Rlog.d(LOG_TAG, "No MO related CB rules in remote server");
                        }

                        if (ruleList != null) {
                            oNewRuleSet = ocb.createNewRuleSet();
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                copyOldRuleToNewRuleSetWithDisabledCB(r, oNewRuleSet, true, -1, -1);
                            }

                            if (oNewRuleSet.getRules() != null) {
                                Rlog.d(LOG_TAG, "Dump MO Disable All CB XML:"
                                        + oNewRuleSet.toXmlString());
                            } else {
                                Rlog.d(LOG_TAG, "Dump MO Disable All CB XML: ruleset with empty "
                                        + "rules");
                            }
                            if (!mUpdateSingleRule) {
                                ocb.saveRuleSet();
                            } else {
                                List<Rule> newRuleList = null;
                                newRuleList = oNewRuleSet.getRules();
                                for (int i = 0; i < newRuleList.size(); i++) {
                                    Rule newRule = newRuleList.get(i);
                                    ocb.saveRule(newRule.mId);
                                }
                            }
                        } else {
                            Rlog.d(LOG_TAG, "No MO related CB rules in remote server");
                        }

                        oldRuleSet = icb.getRuleSet();
                        if (oldRuleSet != null) {
                            ruleList = oldRuleSet.getRules();
                        } else {
                            Rlog.d(LOG_TAG, "No MT related CB rules in remote server");
                        }

                        if (ruleList != null) {
                            iNewRuleSet = icb.createNewRuleSet();
                            for (int i = 0; i < ruleList.size(); i++) {
                                Rule r = ruleList.get(i);
                                copyOldRuleToNewRuleSetWithDisabledCB(r, iNewRuleSet, true, -1, -1);
                            }

                            if (iNewRuleSet.getRules() != null) {
                                Rlog.d(LOG_TAG, "Dump MT Disable All CB XML:"
                                        + iNewRuleSet.toXmlString());
                            } else {
                                Rlog.d(LOG_TAG, "Dump MT Disable All CB XML: ruleset with empty"
                                        + " rules");
                            }
                            if (!mUpdateSingleRule) {
                                icb.saveRuleSet();
                            } else {
                                List<Rule> newRuleList = null;
                                newRuleList = iNewRuleSet.getRules();
                                for (int i = 0; i < newRuleList.size(); i++) {
                                    Rule newRule = newRuleList.get(i);
                                    icb.saveRule(newRule.mId);
                                }
                            }
                        } else {
                            Rlog.d(LOG_TAG, "No MT related CB rules in remote server");
                        }

                    } else {
                        //Not supported request & parameters
                        throw new RuntimeException("Unrecognized SET_CB facility= "
                               + facility + " and its parameters");
                    }
                } //end-of-for-loop (num_of_expansion)

            } catch (XcapException xcapException) {
                mOcbCache = null;
                mOcbCachePhoneId = -1;
                mOcbCacheLastQueried = 0;
                mIcbCache = null;
                mIcbCachePhoneId = -1;
                mIcbCacheLastQueried = 0;

                if (xcapException.getHttpErrorCode() == HTTP_ERROR_CODE_412) {
                    Rlog.d(LOG_TAG, "handleSetCB(): HTTP_ERROR_CODE_412");
                    return HTTP_ERROR_CODE_412;
                }
                Rlog.d(LOG_TAG, "handleSetCB(): XcapException");
                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCB(): xcapException.isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCB(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            } catch (Exception e) {

                //Get XCAP's configuration failed or set new configuration failed
                //Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCB():Start to Print Stack Trace");
                mOcbCache = null;
                mOcbCachePhoneId = -1;
                mOcbCacheLastQueried = 0;
                mIcbCache = null;
                mIcbCachePhoneId = -1;
                mIcbCacheLastQueried = 0;

                e.printStackTrace();

                //Note that: upper layer application
                // (CallBarringBasePreference.java:handleSetCallBarringResponse())
                //Only cast the exception to CommandException is allowed (but it may happen
                // assertion due to cast failure)
                //And handle this exception in TimeConsumingPreferenceActivity.java's onError()
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }

            }

            //[Notify upper's application about the SET_CB result - Success Case without exception]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }

            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }

            return 0;
        }

        /// For OP01 UT @{
        public void handleGetCFInTimeSlot(MMTelSSRequest rr) {
            int reqNo = -1;
            int serialNo = -1;
            CallForwardInfoEx infos[] = null;
            ArrayList<CallForwardInfoEx> queriedCallForwardInfoList =
                    new ArrayList<CallForwardInfoEx>();

            int reason = -1;
            int serviceClass = -1;
            int orgServiceClass = -1;
            String cfPhoneNum = "";
            int queryStatus = 0; // 0: DISABLE, 1: ENABLE
            int noReplyTimer = 20;
            long[] timeSlot = null;
            int phoneId = 0;

            try {
                rr.mp.setDataPosition(0);
                reqNo = rr.mp.readInt();
                serialNo = rr.mp.readInt();
                reason = rr.mp.readInt();
                serviceClass = rr.mp.readInt();
                phoneId = rr.mp.readInt();
                orgServiceClass = serviceClass;

                Rlog.d(LOG_TAG, "Read from CF parcel: req = " + requestToString(reqNo) +
                        ", reason = " + reason + ", serviceClass = " + serviceClass);

                if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                    Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): !isPreferXcap()");
                    throw new UnknownHostException();
                }

                CommunicationDiversion cd;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): mCdCache = " + mCdCache + ", curTime = "
                        + curTime + ", mCdCacheLastQueried = " + mCdCacheLastQueried);
                if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        (mCdCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): using ETAG mCdCache: " + mCdCache);
                    requestXcapNetwork(phoneId);
                    cd = mCdCache;
                    cd.setNetwork(mNetwork);
                    cd.refresh();
                    mCdCacheLastQueried = curTime;
                } else if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        ((curTime >= mCdCacheLastQueried)
                        && ((curTime - mCdCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): using mCdCache: " + mCdCache);
                    cd = mCdCache;
                } else {
                    requestXcapNetwork(phoneId);

                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);

                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): XcapRoot = " + mXcapRoot);
                        throw new UnknownHostException();
                    }

                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);

                    cd = mSimservs.getCommunicationDiversion(true, mNetwork);
                    mCdCache = cd;
                    mCdCachePhoneId = phoneId;
                    mCdCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): new mCdCache = " + mCdCache
                            + ", curTime = " + curTime);
                }

                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): GetRuleSet from cd");

                RuleSet ruleSet = cd.getRuleSet();
                // In Communication Diversion's RuleSet, it may have several rules
                // (e.g., rule for CFU, rule for CFB, rule for CFNoAnswer, rule for CFNotReachable)
                List<Rule> ruleList = null;

                if (ruleSet != null) {
                    ruleList = ruleSet.getRules();
                } else {
                    Rlog.d(LOG_TAG, "No CF related rules in remote server");
                }

                // Note that: If no ant configuration is stored in XCAP server
                // (e.g., empty xml string), ruleList will be null
                if (ruleList != null) {
                    // Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
                    if (orgServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO |
                            CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                        serviceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                    }

                    int numOfComparision = 0;

                    if (orgServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                        serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
                        // one is to match audio (i.e., serviceClass = SERVICE_CLASS_VOICE),
                        // the other is to match video (i.e., SERVICE_CLASS_VIDEO)
                        numOfComparision = 2;
                        Rlog.d(LOG_TAG, "serviceClass == 0, " +
                                "try to 1st match by using SERVICE_CLASS_VOICE");
                    } else {
                        // Specific serviceClass (i.e., value is not 0)
                        // is carried from the upper layer
                        numOfComparision = 1;
                    }

                    for (int it = 0; it < numOfComparision; it++) {
                        if (it == 1 && serviceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                            // 2nd time to match all rules by using SERVICE_CLASS_VIDEO
                            serviceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
                            Rlog.d(LOG_TAG, "serviceClass == 0, " +
                                    "try to 2nd match by using SERVICE_CLASS_VIDEO");
                        }

                        Rlog.d(LOG_TAG, "numOfComparision = " + numOfComparision +
                                ": with round = " + (it + 1) +
                                ", with service class = " + serviceClass);

                        // Check each rule & its corresponding condition/action
                        for (int i = 0; i < ruleList.size(); i++) {
                            Rule r = ruleList.get(i);
                            Conditions cond = r.getConditions();
                            Actions action = r.getActions();
                            List<String> mediaList = null;

                            if (cond != null) {
                                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): " +
                                        "busy = " + cond.comprehendBusy() +
                                        ", NoAnswer = " + cond.comprehendNoAnswer() +
                                        ", NoReachable = " + cond.comprehendNotReachable() +
                                        ", NotRegistered = " + cond.comprehendNotRegistered());
                                mediaList = cond.getMedias();
                            } else {
                                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): " +
                                        "Empty cond (cond==null) for this rule=" + r);
                            }

                            if (reason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                                    ((cond != null && cond.comprehendBusy() == false &&
                                    cond.comprehendNoAnswer() == false &&
                                    cond.comprehendNotRegistered() == false &&
                                    cond.comprehendNotReachable() == false) &&
                                    cond.comprehendRuleDeactivated() == false || cond == null) &&
                                    containSpecificMedia(mediaList, serviceClass)) {
                                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): " +
                                        "CFU is enabled on server");
                                // [CFU]CFU is enabled, set queryStatus as 1
                                queryStatus = 1;
                                if (action.getFowardTo() != null) {
                                    cfPhoneNum = action.getFowardTo().getTarget();
                                }
                                // timeSeconds: This field is not required by CFU
                                // (Only required by CFNoAnswer)
                                noReplyTimer = cd.getNoReplyTimer();
                                if (cond != null) {
                                    timeSlot = convertToLocalTime(cond.comprehendTime());
                                }
                                break;
                            } else {
                                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot()from xcap: " +
                                        "Not matched this rule!");
                            }
                        }
                        CallForwardInfoEx item = new CallForwardInfoEx();
                        item.status = queryStatus;
                        item.reason = reason;
                        item.serviceClass = serviceClass;
                        item.toa = 0;
                        item.number = cfPhoneNum;
                        item.timeSeconds = noReplyTimer;
                        item.timeSlot = timeSlot;
                        Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): " +
                                "add one record with reason = " + reason +
                                ", serviceClass = " + serviceClass +
                                ", queryStatus = " + queryStatus +
                                ", timeSlot = " + Arrays.toString(timeSlot));
                        queriedCallForwardInfoList.add(item);

                        // Reset some variables for this matching result
                        queryStatus = 0; // 0: DISABLE, 1: ENABLE
                        cfPhoneNum = "";
                        noReplyTimer = 20;
                        timeSlot = null;
                    }
                    // end of for-loop(numOfComparision)

                    int queriedSize = queriedCallForwardInfoList.size();

                    infos = new CallForwardInfoEx[queriedSize];
                    for (int inx = 0; inx < queriedSize; inx++) {
                        infos[inx] = (CallForwardInfoEx) queriedCallForwardInfoList.get(inx);
                    }
                } else {
                    // Empty XML String:CF is disabled, set queryStatus as 0
                    Rlog.d(LOG_TAG, "handleGetCFInTimeSlot():get null ruleList");
                    infos = new CallForwardInfoEx[0];
                    queryStatus = 0;
                }
            } catch (UnknownHostException unknownHostException) {
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, unknownHostException);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (XcapException xcapException) {
                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): XcapException");
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                xcapException.printStackTrace();
                if (null != rr.mResult) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            } catch (Exception e) {
                Rlog.d(LOG_TAG, "handleGetCFInTimeSlot(): Start to Print Stack Trace");
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return;
                }
            }

            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, infos, null);
                rr.mResult.sendToTarget();
            }
            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }
        }

        public int handleSetCFInTimeSlot(MMTelSSRequest rr) {
            rr.mp.setDataPosition(0);
            int reqNo = rr.mp.readInt();
            int serialNo = rr.mp.readInt();

            int setCFAction = rr.mp.readInt();
            int setCFReason = rr.mp.readInt();
            int setCFServiceClass = rr.mp.readInt();
            String setCFNumber = rr.mp.readString();
            int setCFTimeSeconds = rr.mp.readInt();
            long[] timeSlot = new long[2];
            try {
                rr.mp.readLongArray(timeSlot);
            } catch (Exception e) {
                timeSlot = null;
            }
            String timeSlotString = convertToSeverTime(timeSlot);
            int phoneId = rr.mp.readInt();

            boolean addRuleForCFUWithAllMediaType = false;
            String cfuRuleID = "CFU";

            Rlog.d(LOG_TAG, "Read from CF parcel: req = " + requestToString(reqNo) +
                    ", cfAction = " + setCFAction + ", reason = " + setCFReason +
                    ", serviceClass = " + setCFServiceClass + ", number = " + setCFNumber +
                    ", timeSec = " + setCFTimeSeconds +
                    ", timsSlot = " + timeSlotString);

            String xcapCFNum = SystemProperties.get(PROP_SS_CFNUM, "");
            if (xcapCFNum.startsWith("sip:") ||
                    xcapCFNum.startsWith("sips:") ||
                    xcapCFNum.startsWith("tel:")) {
                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): " +
                        "get call forwarding num from EM setting: " + xcapCFNum);
                String ssMode = SystemProperties.get(PROP_SS_MODE, MODE_SS_XCAP);
                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): ssMode = " + ssMode);
                if (MODE_SS_XCAP.equals(ssMode)) {
                    setCFNumber = xcapCFNum;
                }
            }

            // Change the serviceClass (VIDEO + DATA_SYNC) to VIDEO directly
            if (setCFServiceClass == (CommandsInterface.SERVICE_CLASS_VIDEO |
                    CommandsInterface.SERVICE_CLASS_DATA_SYNC)) {
                setCFServiceClass = CommandsInterface.SERVICE_CLASS_VIDEO;
            }

            if (!MMTelSSUtils.isPreferXcap(phoneId)) {
                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): !isPreferXcap()");
                if (null != rr.mResult) {
                    AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    rr.mResult.sendToTarget();
                }
                return 0;
            }

            try {
                CommunicationDiversion cd;
                long curTime = System.currentTimeMillis();
                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): mCdCache = " + mCdCache + ", curTime = "
                        + curTime + ", mCdCacheLastQueried = " + mCdCacheLastQueried
                        + ", phoneId = " + phoneId);

                    mXui = MMTelSSUtils.getXui(phoneId, mContext);
                    mXcapRoot = MMTelSSUtils.getXcapRootUri(phoneId);
                    mXIntendedId = MMTelSSUtils.getXIntendedId(phoneId, mContext);
                    if (TextUtils.isEmpty(mXcapRoot)) {
                        Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): XcapRoot = " + mXcapRoot);
                        if (null != rr.mResult) {
                            AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                            rr.mResult.sendToTarget();
                        }
                        return 0;
                    }
                    requestXcapNetwork(phoneId);
                    setSimservsInitParameters(mXui, mXcapRoot, mXIntendedId, mUserName, mPassword,
                            phoneId);
                if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        (mCdCache.isSupportEtag())) {
                    Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): using ETAG mCdCache: " + mCdCache);
                    cd = mCdCache;
                    cd.setNetwork(mNetwork);
                    cd.refresh();
                    mCdCacheLastQueried = curTime;
                } else if ((mCdCache != null) && (phoneId == mCdCachePhoneId) &&
                        ((curTime >= mCdCacheLastQueried)
                        && ((curTime - mCdCacheLastQueried) < MMTEL_CACHE_VALID_TIME))) {
                    Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): using mCdCache: " + mCdCache);
                    cd = mCdCache;
                    cd.setNetwork(mNetwork);
                } else {
                    cd = mSimservs.getCommunicationDiversion(true, mNetwork);
                    mCdCache = cd;
                    mCdCachePhoneId = phoneId;
                    mCdCacheLastQueried = curTime;
                    Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): new mCdCache = " + mCdCache
                            + ", curTime = " + curTime);
                }

                RuleSet ruleSet = cd.getRuleSet();
                List<Rule> ruleList = null;
                RuleSet newRuleSet = cd.createNewRuleSet();
                boolean addedNewRule = false;

                if (ruleSet != null) {
                    ruleList = ruleSet.getRules();
                } else {
                    Rlog.d(LOG_TAG, "No CF related rules in remote server");
                }

                // Note that: If no ant configuration is stored in XCAP server
                // (e.g., empty xml string), ruleList will be null
                if (ruleList != null) {
                    // Check each rule & its corresponding condition/action
                    for (int i = 0; i < ruleList.size(); i++) {
                        Rule r = ruleList.get(i);
                        Conditions cond = r.getConditions();
                        Actions action = r.getActions();
                        List<String> mediaList = null;

                        if (cond != null) {
                            mediaList = cond.getMedias();
                            Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): " +
                                    "busy = " + cond.comprehendBusy() +
                                    ", NoAnswer = " + cond.comprehendNoAnswer() +
                                    ", NoReachable = " + cond.comprehendNotReachable() +
                                    ", NotRegistered = " + cond.comprehendNotRegistered());
                            if (cond.comprehendBusy()) {
                                Rlog.d(LOG_TAG, "The rule is CFB");
                            } else if (cond.comprehendNoAnswer()) {
                                Rlog.d(LOG_TAG, "The rule is CFNoAnswer");
                            } else if (cond.comprehendNotReachable()) {
                                Rlog.d(LOG_TAG, "The rule is CFNotReachable");
                            } else if (cond.comprehendNotRegistered()) {
                                Rlog.d(LOG_TAG, "The rule is CFNotRegistered");
                            } else {
                                cfuRuleID = r.mId;
                                Rlog.d(LOG_TAG, "Update cfuRuleID = " + cfuRuleID);
                            }
                        } else {
                            Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): " +
                                    "Empty cond (cond==null) for this rule = " + r);
                            if (cfuRuleID.equals("CFU")) {
                                //CFU rule
                                cfuRuleID = r.mId;
                                Rlog.d(LOG_TAG, "Update cfuRuleID = " + cfuRuleID);
                            }
                        }

                        if (setCFReason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                                ((cond != null && cond.comprehendBusy() == false &&
                                cond.comprehendNoAnswer() == false &&
                                cond.comprehendNotRegistered() == false &&
                                cond.comprehendNotReachable() == false) || cond == null) &&
                                containSpecificMedia(mediaList, setCFServiceClass)) {

                            if ((setCFServiceClass != CommandsInterface.SERVICE_CLASS_NONE) ||
                                    (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE &&
                                    addRuleForCFUWithAllMediaType == false)) {
                                if (setCFAction == CommandsInterface.CF_ACTION_ENABLE ||
                                        setCFAction == CommandsInterface.CF_ACTION_REGISTRATION) {
                                    addedNewRule = handleCreateNewRuleForCFInTimeSlot(
                                            cd, newRuleSet, setCFReason, setCFAction,
                                            setCFServiceClass, setCFNumber, setCFTimeSeconds,
                                            timeSlotString, cfuRuleID, mUpdateSingleRule);
                                } else {
                                    addedNewRule = handleCreateNewRuleForExistingCF(
                                            cd, newRuleSet, r, setCFReason, setCFAction,
                                            setCFServiceClass, setCFNumber, setCFTimeSeconds,
                                            cfuRuleID, mUpdateSingleRule, 1, phoneId);
                                }
                                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): CFU-addedNewRule = " +
                                        addedNewRule);
                                if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                                    addRuleForCFUWithAllMediaType = true;
                                }
                            } else if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE &&
                                    addRuleForCFUWithAllMediaType == true) {
                                Rlog.d(LOG_TAG, "Already add rule for CFU previously");
                            }
                        } else {
                            // Copy old rule into new rule set
                            Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): " +
                                    "Copy old rule to newRuleSet");
                            copyOldRuleToNewRuleSet(r, newRuleSet, setCFAction, setCFReason);
                        }
                    }
                    // end-of-for-loop (ruleList)
                }

                // Check if the new rule user wants to be modified is already added to or not
                if (addedNewRule == false &&
                        (setCFAction == CommandsInterface.CF_ACTION_ENABLE ||
                        setCFAction == CommandsInterface.CF_ACTION_REGISTRATION)) {
                    addedNewRule = true;
                    handleCreateNewRuleForCFInTimeSlot(
                            cd, newRuleSet, setCFReason, setCFAction,
                            setCFServiceClass, setCFNumber, setCFTimeSeconds,
                            timeSlotString, cfuRuleID, mUpdateSingleRule);
                }

                // Finally, update the new rule set back to remote XCAP server
                // Debug:
                if (newRuleSet.getRules() != null) {
                    Rlog.d(LOG_TAG, "Dump SetCF XML: " + newRuleSet.toXmlString());
                } else {
                    Rlog.d(LOG_TAG, "Dump SetCF XML: ruleset with empty rules");
                }

                if (!mUpdateSingleRule) {
                    cd.saveRuleSet();
                }
            } catch (XcapException xcapException) {
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;
                if (xcapException.getHttpErrorCode() == HTTP_ERROR_CODE_412) {
                    
                    Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): HTTP_ERROR_CODE_412");
                    return HTTP_ERROR_CODE_412;
                }
                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): XcapException");
                xcapException.printStackTrace();
                if (rr.mResult != null) {
                    if (xcapException.isConnectionError()) {
                        Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): isConnectionError()");
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else if (MMTelSSUtils.isOp06IccCard(phoneId)
                            && (xcapException.getHttpErrorCode() != 0)) {
                        Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): OP06 with http Error: "
                                + xcapException.getHttpErrorCode());
                        AsyncResult.forMessage(rr.mResult, null, new UnknownHostException());
                    } else {
                        AsyncResult.forMessage(rr.mResult, null, xcapException);
                    }
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            } catch (Exception e) {
                // Get XCAP's configuration failed or set new configuration failed
                // Generate an exception result callback to upper application
                Rlog.d(LOG_TAG, "handleSetCFInTimeSlot(): Start to Print Stack Trace");
                mCdCache = null;
                mCdCachePhoneId = -1;
                mCdCacheLastQueried = 0;

                e.printStackTrace();
                if (rr.mResult != null) {
                    CommandException ce =
                            CommandException.fromRilErrno(RILConstants.GENERIC_FAILURE);
                    AsyncResult.forMessage(rr.mResult, null, ce);
                    rr.mResult.sendToTarget();
                    if (mXcapMobileDataNetworkManager != null) {
                        mXcapMobileDataNetworkManager.releaseNetwork();
                    }
                    return 0;
                }
            }

            // [Notify upper's application about the SET_CF result - Success Case]
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }
            if (mXcapMobileDataNetworkManager != null) {
                mXcapMobileDataNetworkManager.releaseNetwork();
            }

            return 0;
        }

        public boolean handleCreateNewRuleForCFInTimeSlot(CommunicationDiversion cd,
                RuleSet newRuleSet, int setCFReason, int setCFAction,
                int setCFServiceClass, String setCFNumber, int setCFTimeSeconds,
                String timeSlot, String ruleID, boolean updateSingleRule) throws XcapException {
            // Create a new rule
            Rule cfRule = newRuleSet.createNewRule(ruleID);
            Conditions cfCond = cfRule.createConditions();
            Actions cfAction = cfRule.createActions();
            Rlog.d(LOG_TAG, "handleCreateNewRuleForCFInTimeSlot(): reason = " + setCFReason +
                    ", serviceClass = " + setCFServiceClass + ", number = " + setCFNumber +
                    ", cfTime = " + setCFTimeSeconds +
                    ", timeSlot = " + timeSlot);
            // Add media into this new rule
            if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_VOICE) {
                cfCond.addMedia("audio");
            } else if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                cfCond.addMedia("video");
            } else if (setCFServiceClass == CommandsInterface.SERVICE_CLASS_NONE) {
                cfCond.addMedia("audio");
                cfCond.addMedia("video");
            }

            if (setCFReason == CommandsInterface.CF_REASON_BUSY) {
                cfCond.addBusy();
            } else if (setCFReason == CommandsInterface.CF_REASON_NO_REPLY) {
                cfCond.addNoAnswer();
            } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REACHABLE) {
                cfCond.addNotReachable();
            } else if (setCFReason == CommandsInterface.CF_REASON_NOT_REGISTERED) {
                cfCond.addNotRegistered();
            } else if (setCFReason == CommandsInterface.CF_REASON_UNCONDITIONAL) {
                // Not set any conditions -> always evaluate the result as true
            }
            cfCond.addTime(timeSlot);
            if (MMTelSSUtils.isNotifyCallerTest()) {
                cfAction.setFowardTo(setCFNumber, false);
            } else {
                cfAction.setFowardTo(setCFNumber, true);
            }
            cfAction.getFowardTo().setRevealIdentityToCaller(true);
            cfAction.getFowardTo().setRevealIdentityToTarget(true);

            if (updateSingleRule) {
                cd.saveRule(ruleID);
            }
            return true;
        }

        public long[] convertToLocalTime(String timeSlotString) {
            long[] timeSlot = null;
            if (timeSlotString != null) {
                String[] timeArray = timeSlotString.split(",", 2);
                if (timeArray.length == 2) {
                    timeSlot = new long[2];
                    for (int i = 0; i < 2; i++) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                        try {
                            Date date = dateFormat.parse(timeArray[i]);
                            timeSlot[i] = date.getTime();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                }
            }
            return timeSlot;
        }

        public String convertToSeverTime(long[] timeSlot) {
            String timeSlotString = null;
            if (timeSlot == null || timeSlot.length != 2) {
                return null;
            }
            for (int i = 0; i < timeSlot.length; i++) {
                Date date = new Date(timeSlot[i]);
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                if (i == 0) {
                    timeSlotString = dateFormat.format(date);
                } else {
                    timeSlotString += "," + dateFormat.format(date);
                }
            }
            return timeSlotString;
        }
        /// @}

        //***** Handler implementation
        @Override public void
        handleMessage(Message msg) {
            MMTelSSRequest rr = (MMTelSSRequest) (msg.obj);
            MMTelSSRequest req = null;

            switch (msg.what) {
                case EVENT_SEND:
                    /**
                     * mRequestMessagePending++ already happened for every
                     * EVENT_SEND, thus we must make sure
                     * mRequestMessagePending-- happens once and only once
                     */
                    boolean alreadySubtracted = false;
                    int reqNo = -1;
                    int serialNo = -1;

                    Rlog.d(LOG_TAG, "handleMessage(): EVENT_SEND:"
                            + "mRequestMessagesPending = " + mRequestMessagesPending
                            + ", mRequestsList.size() = " + mRequestsList.size());
                    try {
                        synchronized (mRequestsList) {
                            mRequestsList.add(rr);
                        }

                        mRequestMessagesPending--;
                        alreadySubtracted = true;
                        //MTK-END [mtk04070][111121][ALPS00093395]MTK modified


                        //[MMTelSS] Because it always gets response from simServs immediately,
                        // it must invoke findAndRemoveRequestFromList() here instead of RIL's
                        // invoking at proceeResponse()
                        findAndRemoveRequestFromList(rr.mSerial);

                        //Rlog.d(LOG_TAG, "Receive MMTelSS Request:" + requestToString(rr.mRequest)
                        // + ", parcel dataLen=" + data.length);
                        Rlog.d(LOG_TAG, "Receive MMTelSS Request:" + requestToString(rr.mRequest));

                        switch (rr.mRequest) {
                            case MMTELSS_REQ_SET_CLIR:
                                if (HTTP_ERROR_CODE_412 == handleSetCLIR(rr)) {
                                    Rlog.d(LOG_TAG, "Cache out of date, handleSetCLIR() again");
                                    handleSetCLIR(rr);
                                }
                                break;
                            case MMTELSS_REQ_GET_CLIR:
                                handleGetCLIR(rr);
                                break;
                            case MMTELSS_REQ_GET_CLIP:
                                handleGetCLIP(rr);
                                break;
                            case MMTELSS_REQ_GET_COLP:
                                handleGetCOLP(rr);
                                break;
                            case MMTELSS_REQ_GET_COLR:
                                handleGetCOLR(rr);
                                break;
                            case MMTELSS_REQ_SET_CW:
                                if (HTTP_ERROR_CODE_412 == handleSetCW(rr)) {
                                    Rlog.d(LOG_TAG, "Cache out of date, handleSetCW() again");
                                    handleSetCW(rr);
                                }
                                break;
                            case MMTELSS_REQ_GET_CW:
                                handleGetCW(rr);
                                break;
                            case MMTELSS_REQ_SET_CB:
                                if (HTTP_ERROR_CODE_412 == handleSetCB(rr)) {
                                    Rlog.d(LOG_TAG, "Cache out of date, handleSetCB() again");
                                    handleSetCB(rr);
                                }
                                break;
                            case MMTELSS_REQ_GET_CB:
                                handleGetCB(rr);
                                break;
                            case MMTELSS_REQ_SET_CF:
                                if (HTTP_ERROR_CODE_412 == handleSetCF(rr)) {
                                    Rlog.d(LOG_TAG, "Cache out of date, handleSetCF() again");
                                    handleSetCF(rr);
                                }
                                break;
                            case MMTELSS_REQ_GET_CF:
                                handleGetCF(rr);
                                break;
                            case MMTELSS_REQ_SET_CLIP:
                                handleSetCLIP(rr);
                                break;
                            case MMTELSS_REQ_SET_COLP:
                                handleSetCOLP(rr);
                                break;
                            case MMTELSS_REQ_SET_COLR:
                                handleSetCOLR(rr);
                                break;
                            /// For OP01 UT @{
                            case MMTELSS_REQ_SET_CF_TIME_SLOT:
                                if (HTTP_ERROR_CODE_412 == handleSetCFInTimeSlot(rr)) {
                                    Rlog.d(LOG_TAG, "Cache out of date, handleSetCFInTimeSlot()"
                                            + " again");
                                    handleSetCFInTimeSlot(rr);
                                }
                                break;
                            case MMTELSS_REQ_GET_CF_TIME_SLOT:
                                handleGetCFInTimeSlot(rr);
                                break;
                            /// @}
                            default:
                                Rlog.d(LOG_TAG, "Invalid MMTelSS Request:" + rr.mRequest);
                                throw new RuntimeException("Unrecognized MMTelSS Request: "
                                        + rr.mRequest);
                        }

                        //Rlog.v(LOG_TAG, "writing packet: " + data.length + " bytes");

                    } catch (RuntimeException exc) {
                        Rlog.e(LOG_TAG, "Uncaught exception ", exc);
                        req = findAndRemoveRequestFromList(rr.mSerial);
                        // make sure this request has not already been handled,
                        // eg, if RILReceiver cleared the list.
                        Rlog.d(LOG_TAG, "handleMessage(): RuntimeException:"
                                + "mRequestMessagesPending = " + mRequestMessagesPending
                                + ", mRequestsList.size() = " + mRequestsList.size());
                        if (req != null || !alreadySubtracted) {
                            rr.onError(RILConstants.GENERIC_FAILURE, null);
                            rr.release();
                        }
                    } finally {
                        // Note: We are "Done" only if there are no outstanding
                        // requests or replies. Thus this code path will only release
                        // the wake lock on errors.
                        releaseWakeLockIfDone();
                    }

                    //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
                    if (!alreadySubtracted) {
                        Rlog.d(LOG_TAG, "handleMessage(): !alreadySubtracted:"
                                + "mRequestMessagesPending = " + mRequestMessagesPending
                                + ", mRequestsList.size() = " + mRequestsList.size());
                        mRequestMessagesPending--;
                    }
                    //MTK-END [mtk04070][111121][ALPS00093395]MTK modified

                    //Recycle the Parcel object back to the pool by mtk01411
                    if (rr.mp != null) {
                        rr.mp.recycle();
                        rr.mp = null;
                    }

                    if ((mRequestMessagesPending != 0) || (mRequestsList.size() != 0)) {
                        Rlog.d(LOG_TAG, "handleMessage(): ERROR wakeLock:"
                                + "mRequestMessagesPending = " + mRequestMessagesPending
                                + ", mRequestsList.size() = " + mRequestsList.size());
                    }
                    break;

                case EVENT_WAKE_LOCK_TIMEOUT:
                    // Haven't heard back from the last request.  Assume we're
                    // not getting a response and  release the wake lock.
                    // TODO should we clean up mRequestList and mRequestPending
                    synchronized (mWakeLock) {
                        if (mWakeLock.isHeld()) {
                            if (DBG) {
                                synchronized (mRequestsList) {
                                    int count = mRequestsList.size();
                                    Rlog.d(LOG_TAG, "WAKE_LOCK_TIMEOUT " +
                                            " mReqPending=" + mRequestMessagesPending +
                                            " mRequestList=" + count);

                                    for (int i = 0; i < count; i++) {
                                        rr = mRequestsList.get(i);
                                        Rlog.d(LOG_TAG, i + ": [" + rr.mSerial + "] " +
                                                requestToString(rr.mRequest));

                                    }
                                }
                            }
                            mWakeLock.release();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };


    /**
     * Get the Phone ID for Ut/XCAP request. If IMS is registered, use IMS Phone ID.
     * Ohterwise, use the CS domain Phone ID.
     */
    private int getUtXcapPhoneId() {
        int imsPhoneId = MMTelSSUtils.getDefaultImsPhoneId(mContext);

        if (imsPhoneId < 0) {
            int csDomainPhoneId = SystemProperties.getInt(PROPERTY_CS_CURRENT_PHONE_ID, 0);
            Rlog.d(LOG_TAG, "getUtXcapPhoneId(): use CS domain phoneId by SystemProperties = " + 
                csDomainPhoneId);
            return csDomainPhoneId;
        } else {
            Rlog.d(LOG_TAG, "getUtXcapPhoneId(): use IMS phoneId = " + imsPhoneId);
            return imsPhoneId;
        }
    }

    // Only Support 2/3G SS Feature Sets
    public void
    setCLIR(int clirMode, Message result) {
        setCLIR(clirMode, result, getUtXcapPhoneId());
    }

    /**
     * Set CLIR for the specific phoneId.
     * @param clirMode enable/disable CLIR
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    setCLIR(int clirMode, Message result, int phoneId) {
        //OriginatingIdentityPresentation oip =
        // SimServs.getOriginatingIdentityPresentation(xcapUri, TEST_USER, "password");
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_CLIR, result);
        //clirMode: 0-CommandsInterface.CLIR_DEFAULT , 1-CommandsInterface.CLIR_INVOCATION
        // (restrict CLI presentation), 2-CommandsInterface.CLIR_SUPPRESSION
        // (allow CLI presentation)
        rr.mp.writeInt(clirMode);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    getCLIR(Message result) {
        getCLIR(result, getUtXcapPhoneId());
    }

    /**
     * Get CLIR mode for the specific phoneId.
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    getCLIR(Message result, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_GET_CLIR, result);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    setCLIP(int clipEnable, Message result) {
        setCLIP(clipEnable, result, getUtXcapPhoneId());
    }

    /**
     * Set CLIP for the specific phoneId.
     * @param clipEnable enable/disable CLIP
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    setCLIP(int clipEnable, Message result, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_CLIP, result);
        rr.mp.writeInt(clipEnable);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    queryCLIP(Message result) {
        queryCLIP(result, getUtXcapPhoneId());
    }

    /**
     * Get CLIP for the specific phoneId.
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    queryCLIP(Message result, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_GET_CLIP, result);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    setCOLP(int colpEnable, Message result) {
        setCOLP(colpEnable, result, getUtXcapPhoneId());
    }

    /**
     * Set COLP for the specific phoneId.
     * @param colpEnable enable/disable COLP
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    setCOLP(int colpEnable, Message result, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_COLP, result);
        rr.mp.writeInt(colpEnable);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    getCOLP(Message result) {
        getCOLP(result, getUtXcapPhoneId());
    }

    /**
     * Get COLP for the specific phoneId.
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    getCOLP(Message result, int phoneId) {
        MMTelSSRequest rr
        = MMTelSSRequest.obtain(MMTELSS_REQ_GET_COLP, result);
        rr.mp.writeInt(phoneId);
        send(rr);
    }


    public void
    setCOLR(int colrMode, Message result) {
        setCOLR(colrMode, result, getUtXcapPhoneId());
    }

    /**
     * Set COLR for the specific phoneId.
     * @param colrMode enable/disable COLR
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    setCOLR(int colrMode, Message result, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_COLR, result);
        rr.mp.writeInt(colrMode);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    getCOLR(Message result) {
        getCOLR(result, getUtXcapPhoneId());
    }

    /**
     * Get COLR for the specific phoneId.
     * @param result Message callback
     * @param phoneId the phone index
     */
    public void
    getCOLR(Message result, int phoneId) {
        MMTelSSRequest rr
        = MMTelSSRequest.obtain(MMTELSS_REQ_GET_COLR, result);
        rr.mp.writeInt(phoneId);
        send(rr);
    }


    public void
    setCallWaiting(boolean enable, int serviceClass, Message response) {
        setCallWaiting(enable, serviceClass, response, getUtXcapPhoneId());
    }

    /**
     * Set Call Waiting for the specific phoneId.
     * @param enable enable/disable Call Waiting
     * @param serviceClass service class for Call Waiting
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    setCallWaiting(boolean enable, int serviceClass, Message response, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_CW, response);
        rr.mp.writeInt((enable == true) ? 1 : 0);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    queryCallWaiting(int serviceClass, Message response) {
        queryCallWaiting(serviceClass, response, getUtXcapPhoneId());
    }

    /**
     * Get Call Waiting mode for the specific phoneId.
     * @param serviceClass service class for Call Waiting
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    queryCallWaiting(int serviceClass, Message response, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_GET_CW, response);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(phoneId);
        send(rr);
    }


    public void
    setFacilityLock(String facility, boolean lockState, String password,
            int serviceClass, Message response) {
        setFacilityLock(facility, lockState, password,
                serviceClass, response, getUtXcapPhoneId());
    }

    /**
     * Set Call Barring for the specific phoneId.
     * @param facility Call Barring type
     * @param lockState enable/disable Call Barring
     * @param password password
     * @param serviceClass service class for Call Barring
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    setFacilityLock(String facility, boolean lockState, String password,
            int serviceClass, Message response, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_CB, response);
        rr.mp.writeString(facility);
        rr.mp.writeInt((lockState == true) ? 1 : 0);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(phoneId);
        send(rr);
    }


    public void
    queryFacilityLock(String facility, String password, int serviceClass,
            Message response) {
        queryFacilityLock(facility, password, serviceClass,
                response, getUtXcapPhoneId());
    }

    /**
     * Get Call Barring mode for the specific phoneId.
     * @param facility Call Barring type
     * @param password password
     * @param serviceClass service class for Call Barring
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    queryFacilityLock(String facility, String password, int serviceClass,
            Message response, int phoneId) {

        //[Example]Testing fake-result by mtk01411 2013-0904
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_GET_CB, response);
        rr.mp.writeString(facility);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    setCallForward(int action, int cfReason, int serviceClass,
            String number, int timeSeconds, Message response) {
        setCallForward(action, cfReason, serviceClass,
                number, timeSeconds, response, getUtXcapPhoneId());
    }

    /**
     * Set Call Forwarding for the specific phoneId.
     * @param action CommandsInterface Call Forwarding action
     * @param cfReason CommandsInterface Call Forwarding reason
     * @param serviceClass service class for Call Barring
     * @param number forwarded-to number
     * @param timeSeconds no-reply time
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    setCallForward(int action, int cfReason, int serviceClass,
            String number, int timeSeconds, Message response, int phoneId) {
        // target should be a SIP URI (IETF RFC 3261 [6]) or TEL URL (IETF RFC 3966 [7])
        if (number != null && !number.startsWith("sip:") && !number.startsWith("sips:")
                && !number.startsWith("tel:")) {
            number = "tel:" + number;
        }

        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_CF, response);
        rr.mp.writeInt(action);
        rr.mp.writeInt(cfReason);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeString(number);
        rr.mp.writeInt(timeSeconds);
        rr.mp.writeInt(phoneId);
        send(rr);
    }


    public void
    queryCallForwardStatus(int cfReason, int serviceClass,
            String number, Message response) {
        queryCallForwardStatus(cfReason, serviceClass,
                number, response, getUtXcapPhoneId());
    }

    /**
     * Get Call Forwarding status for the specific phoneId.
     * @param cfReason CommandsInterface Call Forwarding reason
     * @param serviceClass service class for Call Barring
     * @param number forwarded-to number
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    queryCallForwardStatus(int cfReason, int serviceClass,
            String number, Message response, int phoneId) {
        //[Example]Testing fake-result by mtk01411 2013-0904
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_GET_CF, response);

        rr.mp.writeInt(2); // cfAction filed: 2 is for query action, not in used anyway
                           // (See ril.h RIL_CallForwardInfo: 2 = interrogate)
        rr.mp.writeInt(cfReason);
        rr.mp.writeInt(serviceClass);
        //Only through GsmMMICode:It will carry the dialNumber
        if (number != null) {
            rr.mp.writeString(number);
        } else {
            rr.mp.writeString("");
        }
        rr.mp.writeInt(phoneId);

        send(rr);

    }

    /// For OP01 UT @{
    public void
    setCallForwardInTimeSlot(int action, int cfReason, int serviceClass,
                String number, int timeSeconds, long[] timeSlot, Message response) {
        setCallForwardInTimeSlot(action, cfReason, serviceClass,
                number, timeSeconds, timeSlot, response, getUtXcapPhoneId());
    }

    /**
     * Set Call Forwarding with timeSolt for the specific phoneId.
     * @param action CommandsInterface Call Forwarding action
     * @param cfReason CommandsInterface Call Forwarding reason
     * @param serviceClass service class for Call Barring
     * @param number forwarded-to number
     * @param timeSeconds no-reply time
     * @param timeSlot time slot for CFU
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    setCallForwardInTimeSlot(int action, int cfReason, int serviceClass,
                String number, int timeSeconds, long[] timeSlot, Message response, int phoneId) {
        // target should be a SIP URI (IETF RFC 3261 [6]) or TEL URL (IETF RFC 3966 [7])
        if (number != null && !number.startsWith("sip:") && !number.startsWith("sips:")
                && !number.startsWith("tel:")) {
            number = "tel:" + number;
        }

        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_SET_CF_TIME_SLOT, response);
        rr.mp.writeInt(action);
        rr.mp.writeInt(cfReason);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeString(number);
        rr.mp.writeInt(timeSeconds);
        rr.mp.writeLongArray(timeSlot);
        rr.mp.writeInt(phoneId);
        send(rr);
    }

    public void
    queryCallForwardInTimeSlotStatus(int cfReason,
            int serviceClass, Message response) {
        queryCallForwardInTimeSlotStatus(cfReason,
                serviceClass, response, getUtXcapPhoneId());
    }

    /**
     * Get Call Forwarding with timeSolt for the specific phoneId.
     * @param cfReason CommandsInterface Call Forwarding reason
     * @param serviceClass service class for Call Barring
     * @param response Message callback
     * @param phoneId the phone index
     */
    public void
    queryCallForwardInTimeSlotStatus(int cfReason,
            int serviceClass, Message response, int phoneId) {
        MMTelSSRequest rr = MMTelSSRequest.obtain(MMTELSS_REQ_GET_CF_TIME_SLOT, response);
        rr.mp.writeInt(cfReason);
        rr.mp.writeInt(serviceClass);
        rr.mp.writeInt(phoneId);
        send(rr);
    }
    /// @}

    private void
    acquireWakeLock() {
        Rlog.d(LOG_TAG, "=>wakeLock() "
                + "mRequestMessagesPending = " + mRequestMessagesPending
                + ", mRequestsList.size() = " + mRequestsList.size());
        synchronized (mWakeLock) {
            mWakeLock.acquire();
            mRequestMessagesPending++;

            mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            Message msg = mSender.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
            mSender.sendMessageDelayed(msg, mWakeLockTimeout);
        }
    }

    private void
    releaseWakeLockIfDone() {
        Rlog.d(LOG_TAG, "wakeLock()=> "
                + "mRequestMessagesPending = " + mRequestMessagesPending
                + ", mRequestsList.size() = " + mRequestsList.size());
        synchronized (mWakeLock) {
            if (mWakeLock.isHeld() &&
                    (mRequestMessagesPending == 0) &&
                    //MTK-START [mtk04070][111121][ALPS00093395]MTK modified
                    (mRequestsList.size() == 0)) {
                //MTK-END [mtk04070][111121][ALPS00093395]MTK modified
                mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
                mWakeLock.release();
            }
        }
    }

    private MMTelSSRequest findAndRemoveRequestFromList(int serial) {
        synchronized (mRequestsList) {
            for (int i = 0, s = mRequestsList.size() ; i < s ; i++) {
                MMTelSSRequest rr = mRequestsList.get(i);

                if (rr.mSerial == serial) {
                    mRequestsList.remove(i);
                    if (mRequestMessagesWaiting > 0)
                        mRequestMessagesWaiting--;
                    return rr;
                }
            }
        }

        return null;
    }

    static String
    requestToString(int request) {
        switch (request) {
            case MMTELSS_REQ_SET_CLIR: return "SET_CLIR";
            case MMTELSS_REQ_GET_CLIR: return "GET_CLIR";
            case MMTELSS_REQ_GET_CLIP: return "GET_CLIP";
            case MMTELSS_REQ_GET_COLP: return "GET_COLP";
            case MMTELSS_REQ_GET_COLR: return "GET_COLR";
            case MMTELSS_REQ_SET_CW: return "SET_CW";
            case MMTELSS_REQ_GET_CW: return "GET_CW";
            case MMTELSS_REQ_SET_CB: return "SET_CB";
            case MMTELSS_REQ_GET_CB: return "GET_CB";
            case MMTELSS_REQ_SET_CF: return "SET_CF";
            case MMTELSS_REQ_GET_CF: return "GET_CF";
            /// For OP01 UT @{
            case MMTELSS_REQ_SET_CF_TIME_SLOT: return "SET_CF_TIME_SLOT";
            case MMTELSS_REQ_GET_CF_TIME_SLOT: return "GET_CF_TIME_SLOT";
            /// @}
            default: return "UNKNOWN MMTELSS REQ";
        }

    }


    //[MMTelSS] Dump Call Forwarding Rule
    public void dumpCFRule(Rule rule) {
        Conditions cond = null;
        Actions action = null;
        ForwardTo forward = null;

        if (rule != null) {
            cond = rule.getConditions();
            action = rule.getActions();
            if (cond == null || action == null) {
                return;
            }
        } else {
            return;
        }

        forward = action.getFowardTo();
        Rlog.d(LOG_TAG, "Dump CF Rule:busy=" + cond.comprehendBusy() + ",noAns="
                + cond.comprehendNoAnswer() + ",noReachable=" + cond.comprehendNotReachable()
                + ",noRegistered=" + cond.comprehendNotRegistered() + ",forward_to_Target="
                + forward.getTarget() + ",isNotifyCaller=" + forward.isNotifyCaller());
        List<String> mediaList = cond.getMedias();
        String mediaTypeList = "";
        if (mediaList != null) {
            for (int i = 0; i < mediaList.size(); i++) {
                mediaTypeList += (" " + mediaList.get(i));
            }
            Rlog.d(LOG_TAG, "Dump CF Rule:mediaTypeList=" + mediaTypeList);
        }
    }

    public void dumpCBRule(Rule rule) {
        Conditions cond = null;
        Actions action = null;

        if (rule != null) {
            cond = rule.getConditions();
            action = rule.getActions();
            if (cond == null || action == null) {
                return;
            }
        } else {
            return;
        }

        Rlog.d(LOG_TAG, "Dump CB Rule: international=" + cond.comprehendInternational()
                + ",roaming=" + cond.comprehendRoaming());
        List<String> mediaList = cond.getMedias();
        String mediaTypeList = "";
        if (mediaList != null) {
            for (int i = 0; i < mediaList.size(); i++) {
                mediaTypeList += (" " + mediaList.get(i));
            }
            Rlog.d(LOG_TAG, "Dump CB Rule:mediaTypeList=" + mediaTypeList);
        }

    }

    private void
    send(MMTelSSRequest rr) {
        Message msg;
        msg = mSender.obtainMessage(EVENT_SEND, rr);
        acquireWakeLock();
        msg.sendToTarget();

    }
}

