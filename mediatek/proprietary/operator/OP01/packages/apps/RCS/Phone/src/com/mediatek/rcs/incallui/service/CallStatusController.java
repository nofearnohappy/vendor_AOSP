/*
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
package com.mediatek.rcs.incallui.service;

import android.content.Context;
import android.telecom.Call;
import android.telecom.Call.Details;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.mediatek.rcs.incallui.utils.PhoneUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class CallStatusController {

    private static final String TAG = "CallStatusController";
    //Presenter to control callstatusservice
    private CallStatusServicePresenter mCallStatusServicePresenter;
    //Hash call map from host
    private HashMap<String, Call> mCallMap = new HashMap<String, Call>();
    //The call status map for call instatus, used for sdk
    private HashMap<String, Integer> mStatusMap = new HashMap<String, Integer>();

    private final Set<OnCallChangedListener> mStatusListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<OnCallChangedListener, Boolean>(5, 0.9f, 1));
    private final Set<OnCallItemChanged> mItemListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<OnCallItemChanged, Boolean>(5, 0.9f, 1));

    public static final int PONE_STATE_NEW = 0;
    public static final int PONE_STATE_DIALING = 1;
    public static final int PONE_STATE_RINGING = 2;
    public static final int PONE_STATE_HOLDING = 3;
    public static final int PONE_STATE_ACTIVE = 4;
    public static final int PONE_STATE_DISCONNECTED = 7;
    public static final int PONE_STATE_PRE_DIAL_WAIT = 8;
    public static final int PONE_STATE_CONNECTING = 9;
    public static final int PONE_STATE_DISCONNECTING = 10;

    public static final int VO_PHONE_STATUS_INCOMING = 101;
    public static final int VO_PHONE_STATUS_OUTGOING = 102;
    public static final int VO_PHONE_STATUS_MT_ACTIVE = 103;
    public static final int VO_PHONE_STATUS_MO_ACTIVE = 104;
    public static final int VO_PHONE_STATUS_MT_DISCONNECT = 105;
    public static final int VO_PHONE_STATUS_MO_DISCONNECT = 106;

    public static final int VT_PHONE_STATUS_INCOMING = 201;
    public static final int VT_PHONE_STATUS_OUTGOING = 202;
    public static final int VT_PHONE_STATUS_MT_ACTIVE = 203;
    public static final int VT_PHONE_STATUS_MO_ACTIVE = 204;
    public static final int VT_PHONE_STATUS_MT_DISCONNECT = 205;
    public static final int VT_PHONE_STATUS_MO_DISCONNECT = 206;

    //Call event for sdk api using
    public static final String EVT_VO_PHONE_STATUS_INCOMING = "1220018000";
    public static final String EVT_VO_PHONE_STATUS_OUTGOING = "1110000000";
    public static final String EVT_VO_PHONE_STATUS_MT_ACTIVE = "1230020000";
    public static final String EVT_VO_PHONE_STATUS_MO_ACTIVE = "1130020000";
    public static final String EVT_VO_PHONE_STATUS_MT_DISCONNECT = "1240020000";
    public static final String EVT_VO_PHONE_STATUS_MO_DISCONNECT = "1140020000";

    public static final String EVT_VT_PHONE_STATUS_INCOMING = "2220000000";
    public static final String EVT_VT_PHONE_STATUS_OUTGOING = "2110000000";
    public static final String EVT_VT_PHONE_STATUS_MT_ACTIVE = "2230020000";
    public static final String EVT_VT_PHONE_STATUS_MO_ACTIVE = "2130020000";
    public static final String EVT_VT_PHONE_STATUS_MT_DISCONNECT = "2240020000";
    public static final String EVT_VT_PHONE_STATUS_MO_DISCONNECT = "2140020000";

    private String  mCurrentCallId;
    private int     mPreviousCallStatus;
    private Context mContext;

    public CallStatusController(Context cnx) {
        mCallStatusServicePresenter = new CallStatusServicePresenter(cnx);
        mStatusListeners.add(mCallStatusServicePresenter);

        mCurrentCallId = "";
        mContext = cnx;
    }

    //Interface for CallStatusServicePresenter
    public interface OnCallChangedListener {
        public void onCallStatusChange(HashMap<String, Call> call);
        public void onMenuItemSelected(Call call);
        public void onViewSetup(HashMap<Integer, TextView> map);
        public void onVanish();
    }

    public interface OnCallItemChanged {
        //Use for check if need to callback to RichCallController to update panel
        public void onCallChanged(boolean isChanged, Call call, String event);
        //Use for check if need to callback to RichCallController to update contact icon
        public void onPhotoRefreshed(boolean isRefresh, String number);
    }

    public void onVanish() {
        for (OnCallChangedListener listener : mStatusListeners) {
            listener.onVanish();
        }

        mStatusListeners.remove(mCallStatusServicePresenter);
        mCallStatusServicePresenter = null;
    }

    public void onViewSetup(HashMap<Integer, TextView> map) {
        for (OnCallChangedListener listener : mStatusListeners) {
            listener.onViewSetup(map);
        }
    }

    public Call onMenuItemSelected() {
        Log.i(TAG, "onMenuItemSelected");
        Call currCall = null;
        currCall = getDialingCall();

        if (currCall == null) {
            currCall = getActiveCall();
        }

        if (currCall == null) {
            currCall = getHoldingCall();
        }

        for (OnCallChangedListener listener : mStatusListeners) {
            listener.onMenuItemSelected(currCall);
        }
        return currCall;
    }

    public boolean isNeedShowMenuItem() {
        List<Call> list = null;

        Call currCall = null;
        currCall = getDialingCall();

        if (currCall == null) {
            currCall = getActiveCall();
        }

        if (currCall == null) {
            currCall = getHoldingCall();
        }

        if (currCall != null) {
            if (PhoneUtils.isConferenceCall(currCall)) {
                return false;
            }

            Details details = currCall.getDetails();
            if (details != null && details.getHandle() != null) {
                String scheme = details.getHandle().getScheme();
                String uriString = details.getHandle().getSchemeSpecificPart();

                if ("tel".equals(scheme) && (!PhoneNumberUtils.isEmergencyNumber(uriString))) {
                    return true;
                }
            } else {
                Log.i(TAG, "isNeedShowMenuItem, details or handle is null!");
                return false;
            }
        }
        return false;
    }

    private Call getCallById(String index) {
        for (Call call : mCallMap.values()) {
            String id = call.getCallId();
            if (id.equals(index)) {
                return call;
            }
        }
        return null;
    }

    private Call getCurrentCall() {
        Call call = getRingingCall();

        if (call == null) {
            call = getDialingCall();
        }

        if (call == null) {
            call = getActiveCall();
        }

        if (call == null) {
            call = getDisconnectCall();
        }

        if (call == null) {
            call = getHoldingCall();
        }

        if (PhoneUtils.isVideoCall(call, mContext)) {
            return null;
        }
        return call;
    }

    private Call getActiveCall() {
        for (Call call : mCallMap.values()) {
            if (call != null && call.getState() == PONE_STATE_ACTIVE) {
                return call;
            }
        }
        return null;
    }

    private Call getDisconnectCall() {
        for (Call call : mCallMap.values()) {
            if (call != null) {
                if (call.getState() == PONE_STATE_DISCONNECTING ||
                    call.getState() == PONE_STATE_DISCONNECTED) {
                    return call;
                }
            }
        }
        return null;
    }

    private Call getHoldingCall() {
        for (Call call : mCallMap.values()) {
            if (call != null && call.getState() == PONE_STATE_HOLDING) {
                return call;
            }
        }
        return null;
    }

    private Call getDialingCall() {
        for (Call call : mCallMap.values()) {
            if (call != null && call.getState() == PONE_STATE_DIALING) {
                return call;
            }
        }
        return null;
    }

    private Call getRingingCall() {
        for (Call call : mCallMap.values()) {
            if (call != null && call.getState() == PONE_STATE_RINGING) {
                return call;
            }
        }
        return null;
    }

    public void onCallStatusChange(HashMap<String, Call> callMap) {
        mCallMap = callMap;
        //Update the call event for sdk
        updateStatusMap();

        Call call = getCurrentCall();

        //This for send message part during the phone call
        for (OnCallChangedListener listener : mStatusListeners) {
            if (call != null && call.getState() != PONE_STATE_RINGING) {
                listener.onCallStatusChange(callMap);
            }
        }

        if (mItemListeners == null || mItemListeners.size() == 0) {
            Log.d(TAG, "mItemListeners is empty, just return!");
            return;
        }

        if (call != null) {
            if (!call.getCallId().equals(mCurrentCallId)) {
                mCurrentCallId = call.getCallId();
                mPreviousCallStatus = call.getState();
                //Should we check the number change? Maybe this can increase performance???
                if (mItemListeners != null) {
                    for (OnCallItemChanged listener : mItemListeners) {
                        listener.onCallChanged(true, call, getCallEvent(call));
                    }
                }
            } else {
                //If status is not changed, we should skip it
                if (mPreviousCallStatus == call.getState()) {
                    return;
                }

                //Status changed, but the call id is not changed
                if (mItemListeners != null) {
                    for (OnCallItemChanged listener : mItemListeners) {
                        listener.onCallChanged(false, call, getCallEvent(call));
                    }
                }
                mPreviousCallStatus = call.getState();
            }
        }
    }

    private String getCallEvent(Call call) {
        if (call != null) {
            int status = mStatusMap.get(call.getCallId());
            String event = "";
            switch (status) {
                case VO_PHONE_STATUS_INCOMING:
                    event = EVT_VO_PHONE_STATUS_INCOMING;
                    break;
                case VO_PHONE_STATUS_OUTGOING:
                    event = EVT_VO_PHONE_STATUS_OUTGOING;
                    break;
                case VO_PHONE_STATUS_MT_ACTIVE:
                    event = EVT_VO_PHONE_STATUS_MT_ACTIVE;
                    break;
                case VO_PHONE_STATUS_MO_ACTIVE:
                    event = EVT_VO_PHONE_STATUS_MO_ACTIVE;
                    break;
                case VO_PHONE_STATUS_MT_DISCONNECT:
                    event = EVT_VO_PHONE_STATUS_MT_DISCONNECT;
                    break;
                case VO_PHONE_STATUS_MO_DISCONNECT:
                    event = EVT_VO_PHONE_STATUS_MO_DISCONNECT;
                    break;
                case VT_PHONE_STATUS_INCOMING:
                    event = EVT_VT_PHONE_STATUS_INCOMING;
                    break;
                case VT_PHONE_STATUS_OUTGOING:
                    event = EVT_VT_PHONE_STATUS_OUTGOING;
                    break;
                case VT_PHONE_STATUS_MT_ACTIVE:
                    event = EVT_VT_PHONE_STATUS_MT_ACTIVE;
                    break;
                case VT_PHONE_STATUS_MO_ACTIVE:
                    event = EVT_VT_PHONE_STATUS_MO_ACTIVE;
                    break;
                case VT_PHONE_STATUS_MT_DISCONNECT:
                    event = EVT_VT_PHONE_STATUS_MT_DISCONNECT;
                    break;
                case VT_PHONE_STATUS_MO_DISCONNECT:
                    event = EVT_VT_PHONE_STATUS_MO_DISCONNECT;
                    break;
                default:
                    //Maybe we should check for SRVCC changed
                    break;
            }
            return event;
        }
        return "";
    }

    private void updateStatusMap() {
        for (Call call : mCallMap.values()) {
            //Log.d(TAG, "updateStatusMap, call status = " + call.getState());
            switch (call.getState()) {
                case PONE_STATE_DIALING:
                case PONE_STATE_CONNECTING:
                case PONE_STATE_PRE_DIAL_WAIT:
                    mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_OUTGOING);
                    break;
                case PONE_STATE_RINGING:
                    mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_INCOMING);
                    break;
                case PONE_STATE_ACTIVE:
                    int status = mStatusMap.get(call.getCallId());
                    switch(status) {
                        case VO_PHONE_STATUS_OUTGOING:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MO_ACTIVE);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MO_ACTIVE);
                            }
                            break;
                        case VO_PHONE_STATUS_INCOMING:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MT_ACTIVE);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MT_ACTIVE);
                            }
                            break;
                        case VT_PHONE_STATUS_OUTGOING:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MO_ACTIVE);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MO_ACTIVE);
                            }
                            break;
                        case VT_PHONE_STATUS_INCOMING:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MT_ACTIVE);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MT_ACTIVE);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case PONE_STATE_DISCONNECTED:
                    status = mStatusMap.get(call.getCallId());
                    switch(status) {
                        case VO_PHONE_STATUS_MT_ACTIVE:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MT_DISCONNECT);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MT_DISCONNECT);
                            }
                            break;
                        case VO_PHONE_STATUS_MO_ACTIVE:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MO_DISCONNECT);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MO_DISCONNECT);
                            }
                            break;
                        case VT_PHONE_STATUS_MT_ACTIVE:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MT_DISCONNECT);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MT_DISCONNECT);
                            }
                            break;
                        case VT_PHONE_STATUS_MO_ACTIVE:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MO_DISCONNECT);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MO_DISCONNECT);
                            }
                            break;
                        default:
                            if (PhoneUtils.isVideoCall(call, mContext)) {
                                mStatusMap.put(call.getCallId(), VT_PHONE_STATUS_MO_DISCONNECT);
                            } else {
                                mStatusMap.put(call.getCallId(), VO_PHONE_STATUS_MO_DISCONNECT);
                            }
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void onCallPhotoChanged(Call call) {
        Call currCall = getCurrentCall();
        if (currCall != null && call != null) {
            String currNumber = PhoneUtils.parseNumber(currCall);
            String number = PhoneUtils.parseNumber(call);
            boolean isChanged = !TextUtils.isEmpty(currNumber) && currNumber.equals(number);
            //Notice the call photo changed, need to set to the icon panel
            for (OnCallItemChanged listener : mItemListeners) {
                if (isChanged) {
                    listener.onPhotoRefreshed(true, number);
                } else {
                    listener.onPhotoRefreshed(false, currNumber);
                }
            }

        }
    }

    public void clearStatusMap() {
        mCallMap.clear();
        mStatusMap.clear();
    }

    public void addCallItemChangListener(OnCallItemChanged listener) {
        Log.d(TAG, "addCallItemChangListener, listener = " + listener);
        mItemListeners.add(listener);
    }

    public void removeCallItemChangListener(OnCallItemChanged listener) {
        Log.d(TAG, "removeCallItemChangListener, listener = " + listener);
        mItemListeners.remove(listener);
    }
}
