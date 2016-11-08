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


import android.content.Context;
import android.os.AsyncResult;
import android.os.RegistrantList;
import android.os.Registrant;
import android.os.Handler;

/**
 * {@hide}
 */
public abstract class ImsBaseCommands implements ImsCommandsInterface {
    //***** Instance Variables
    protected Context mContext;
    protected RadioState mState = RadioState.RADIO_UNAVAILABLE;
    protected Object mStateMonitor = new Object();

    protected RegistrantList mRadioStateChangedRegistrants = new RegistrantList();
    protected RegistrantList mOnRegistrants = new RegistrantList();
    protected RegistrantList mAvailRegistrants = new RegistrantList();
    protected RegistrantList mOffOrNotAvailRegistrants = new RegistrantList();
    protected RegistrantList mNotAvailRegistrants = new RegistrantList();
    protected RegistrantList mCallStateRegistrants = new RegistrantList();
    protected Registrant mRingRegistrant;
    protected RegistrantList mRingbackToneRegistrants = new RegistrantList();

    /* M: CC part start */
    protected RegistrantList mCallForwardingInfoRegistrants = new RegistrantList();
    protected Registrant mCallRelatedSuppSvcRegistrant;
    protected Registrant mIncomingCallIndicationRegistrant;
    protected Registrant mCnapNotifyRegistrant;
    protected RegistrantList mCipherIndicationRegistrant = new RegistrantList();
    protected Registrant mSpeechCodecInfoRegistrant;
    /* M: CC part end */

    // IMS VoLTE
    protected RegistrantList mEpsNetworkFeatureSupportRegistrants = new RegistrantList();
    protected RegistrantList mEpsNetworkFeatureInfoRegistrants = new RegistrantList();
    protected RegistrantList mSrvccHandoverInfoIndicationRegistrants = new RegistrantList();

    //VoLTE
    protected RegistrantList mImsEnableStartRegistrants = new RegistrantList();
    protected RegistrantList mImsDisableStartRegistrants = new RegistrantList();
    protected RegistrantList mImsEnableDoneRegistrants = new RegistrantList();
    protected RegistrantList mImsDisableDoneRegistrants = new RegistrantList();
    protected RegistrantList mImsRegistrationInfoRegistrants = new RegistrantList();
    protected RegistrantList mDedicateBearerActivatedRegistrant = new RegistrantList();
    protected RegistrantList mDedicateBearerModifiedRegistrant = new RegistrantList();
    protected RegistrantList mDedicateBearerDeactivatedRegistrant = new RegistrantList();

    /// M: IMS feature. @{
    /* Register for updating call ids for conference call after SRVCC is done. */
    protected RegistrantList mEconfSrvccRegistrants = new RegistrantList();
    /* Register for updating conference call merged/added result. */
    protected RegistrantList mEconfResultRegistrants = new RegistrantList();
    /* Register for updating call mode and pau. */
    protected RegistrantList mCallInfoRegistrants = new RegistrantList();
    /// @}

    protected Registrant mSsnRegistrant;
    protected RegistrantList mSrvccStateRegistrants = new RegistrantList();
    protected RegistrantList mCallProgressIndicatorRegistrants = new RegistrantList();
    /// M: ViLTE feature, call mode changed event. @{
    protected RegistrantList mCallModeChangeIndicatorRegistrants = new RegistrantList();
    protected RegistrantList mVideoCapabilityIndicatorRegistrants = new RegistrantList();
    /// @}

    public ImsBaseCommands(Context context) {
        mContext = context;
    }

    public void registerForNotAvailable(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);

        synchronized (mStateMonitor) {
            mNotAvailRegistrants.add(r);

            if (!mState.isAvailable()) {
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    public void unregisterForNotAvailable(Handler h) {
        synchronized (mStateMonitor) {
            mNotAvailRegistrants.remove(h);
        }
    }

    public void registerForCallStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCallStateRegistrants.add(r);
    }

    public void unregisterForCallStateChanged(Handler h) {
        mCallStateRegistrants.remove(h);
    }

    public void setOnCallRing(Handler h, int what, Object obj) {
        mRingRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnCallRing(Handler h) {
        if (mRingRegistrant != null && mRingRegistrant.getHandler() == h) {
            mRingRegistrant.clear();
            mRingRegistrant = null;
        }
    }

    public void registerForRingbackTone(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mRingbackToneRegistrants.add(r);
    }

    public void unregisterForRingbackTone(Handler h) {
        mRingbackToneRegistrants.remove(h);
    }

    public void registerForCallForwardingInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCallForwardingInfoRegistrants.add(r);
    }

    public void unregisterForCallForwardingInfo(Handler h) {
        mCallForwardingInfoRegistrants.remove(h);
    }

    public void setOnCallRelatedSuppSvc(Handler h, int what, Object obj) {
        mCallRelatedSuppSvcRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnCallRelatedSuppSvc(Handler h) {
        mCallRelatedSuppSvcRegistrant.clear();
    }

    public void setOnIncomingCallIndication(Handler h, int what, Object obj) {
        mIncomingCallIndicationRegistrant = new Registrant(h, what, obj);
    }

    public void unsetOnIncomingCallIndication(Handler h) {
        mIncomingCallIndicationRegistrant.clear();
    }

    public void setCnapNotify(Handler h, int what, Object obj) {
        mCnapNotifyRegistrant = new Registrant(h, what, obj);
    }

    public void unSetCnapNotify(Handler h) {
        mCnapNotifyRegistrant.clear();
    }

    public void registerForCipherIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCipherIndicationRegistrant.add(r);
    }

    public void unregisterForCipherIndication(Handler h) {
        mCipherIndicationRegistrant.remove(h);
    }

    public void setOnSpeechCodecInfo(Handler h, int what, Object obj) {
        mSpeechCodecInfoRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnSpeechCodecInfo(Handler h) {
        if (mSpeechCodecInfoRegistrant != null && mSpeechCodecInfoRegistrant.getHandler() == h) {
            mSpeechCodecInfoRegistrant.clear();
            mSpeechCodecInfoRegistrant = null;
        }
    }

    public void registerForEpsNetworkFeatureSupport(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEpsNetworkFeatureSupportRegistrants.add(r);
    }

    public void unregisterForEpsNetworkFeatureSupport(Handler h) {
        mEpsNetworkFeatureSupportRegistrants.remove(h);
    }

    public void registerForEpsNetworkFeatureInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEpsNetworkFeatureInfoRegistrants.add(r);
    }

    public void unregisterForEpsNetworkFeatureInfo(Handler h) {
        mEpsNetworkFeatureInfoRegistrants.remove(h);
    }

    public void registerForSrvccHandoverInfoIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSrvccHandoverInfoIndicationRegistrants.add(r);
    }
    public void unregisterForSrvccHandoverInfoIndication(Handler h) {
        mSrvccHandoverInfoIndicationRegistrants.remove(h);
    }

    public void registerForEconfSrvcc(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEconfSrvccRegistrants.add(r);
    }

    public void unregisterForEconfSrvcc(Handler h) {
        mEconfSrvccRegistrants.remove(h);
    }

    public void registerForEconfResult(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mEconfResultRegistrants.add(r);
    }

    public void unregisterForEconfResult(Handler h) {
        mEconfResultRegistrants.remove(h);
    }

    public void registerForCallInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCallInfoRegistrants.add(r);
    }

    public void unregisterForCallInfo(Handler h) {
        mCallInfoRegistrants.remove(h);
    }

    // IMS
    public void registerForImsEnableStart(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsEnableStartRegistrants.add(r);
    }

    public void unregisterForImsEnableStart(Handler h) {
        mImsEnableStartRegistrants.remove(h);
    }

    public void registerForImsDisableStart(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsDisableStartRegistrants.add(r);
    }

    public void unregisterForImsDisableStart(Handler h) {
        mImsDisableStartRegistrants.remove(h);
    }

    public void registerForImsEnableComplete(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsEnableDoneRegistrants.add(r);
    }

    public void unregisterForImsEnableComplete(Handler h) {
        mImsEnableDoneRegistrants.remove(h);
    }

    public void registerForImsDisableComplete(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsDisableDoneRegistrants.add(r);
    }

    public void unregisterForImsDisableComplete(Handler h) {
        mImsDisableDoneRegistrants.remove(h);
    }

    public void registerForImsRegistrationInfo(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mImsRegistrationInfoRegistrants.add(r);
    }

    public void unregisterForImsRegistrationInfo(Handler h) {
        mImsRegistrationInfoRegistrants.remove(h);
    }

    public void registerForDedicateBearerActivated(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDedicateBearerActivatedRegistrant.add(r);
    }

    public void unregisterForDedicateBearerActivated(Handler h) {
        mDedicateBearerActivatedRegistrant.remove(h);
    }

    public void registerForDedicateBearerModified(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDedicateBearerModifiedRegistrant.add(r);
    }

    public void unregisterForDedicateBearerModified(Handler h) {
        mDedicateBearerModifiedRegistrant.remove(h);
    }

    public void registerForDedicateBearerDeactivated(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDedicateBearerDeactivatedRegistrant.add(r);
    }

    public void unregisterForDedicateBearerDeactivated(Handler h) {
        mDedicateBearerDeactivatedRegistrant.remove(h);
    }

    public void setOnSuppServiceNotification(Handler h, int what, Object obj) {
        mSsnRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnSuppServiceNotification(Handler h) {
        if (mSsnRegistrant != null && mSsnRegistrant.getHandler() == h) {
            mSsnRegistrant.clear();
            mSsnRegistrant = null;
        }
    }

    public void registerForSrvccStateChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mSrvccStateRegistrants.add(r);
    }

    public void unregisterForSrvccStateChanged(Handler h) {
        mSrvccStateRegistrants.remove(h);
    }

    public void registerForCallProgressIndicator(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mCallProgressIndicatorRegistrants.add(r);
    }

    public void unregisterForCallProgressIndicator(Handler h) {
        mCallProgressIndicatorRegistrants.remove(h);
    }

    /// M: For ViLTE feature, register for call mode, video capability change. @{
    /**
     * Register for call mode change event. (RIL_UNSOL_CALLMOD_CHANGE_INDICATOR)
     *
     * @param h handler
     * @param what message
     * @param obj object
     * @hide
     */
    public void registerForCallModeChangeIndicator(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mCallModeChangeIndicatorRegistrants.add(r);
    }

    /**
     * Un-register for call mode change event.
     *
     * @param h handler
     * @hide
     */
    public void unregisterForCallModeChangeIndicator(Handler h) {
        mCallModeChangeIndicatorRegistrants.remove(h);
    }

    /**
     * Register for video capability change event. (RIL_UNSOL_CALLMOD_CHANGE_INDICATOR)
     *
     * @param h handler
     * @param what message
     * @param obj object
     * @hide
     */
    public void registerForVideoCapabilityIndicator(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mVideoCapabilityIndicatorRegistrants.add(r);
    }

    /**
     * Un-register for video capability change event.
     *
     * @param h handler
     * @hide
     */
    public void unregisterForVideoCapabilityIndicator(Handler h) {
        mVideoCapabilityIndicatorRegistrants.remove(h);
    }
    /// @}

    //***** Protected Methods
    /**
     * Store new RadioState and send notification based on the changes
     *
     * This function is called only by RIL.java when receiving unsolicited
     * RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED
     *
     * RadioState has 3 values : RADIO_OFF, RADIO_UNAVAILABLE, RADIO_ON.
     *
     * @param newState new RadioState decoded from RIL_UNSOL_RADIO_STATE_CHANGED
     */
    protected void setRadioState(RadioState newState) {
        RadioState oldState;

        synchronized (mStateMonitor) {
            oldState = mState;
            mState = newState;

            if (oldState == mState) {
                // no state transition
                return;
            }

            mRadioStateChangedRegistrants.notifyRegistrants();

            if (mState.isAvailable() && !oldState.isAvailable()) {
                mAvailRegistrants.notifyRegistrants();
                onRadioAvailable();
            }

            if (!mState.isAvailable() && oldState.isAvailable()) {
                mNotAvailRegistrants.notifyRegistrants();
            }

            if (mState.isOn() && !oldState.isOn()) {
                mOnRegistrants.notifyRegistrants();
            }

            if ((!mState.isOn() || !mState.isAvailable())
                && !((!oldState.isOn() || !oldState.isAvailable()))
            ) {
                mOffOrNotAvailRegistrants.notifyRegistrants();
            }
        }
    }

    protected void onRadioAvailable() {
    }

    /// M: IMS ViLTE feature. @{
    /**
     * accept video call.
     * @param videoMode accept video as video, voice, video_rx or video_tx.
     * @param callId  indication to accept which call.
     * @param result the command result.
     * @Override
     */
    public void acceptVideoCall(int videoMode, int callId) {}
    /// @}
}
