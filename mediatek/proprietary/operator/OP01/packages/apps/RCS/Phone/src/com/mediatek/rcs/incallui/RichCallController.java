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

package com.mediatek.rcs.incallui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.telecom.Call;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.mediatek.rcs.incallui.RichCallAdapter.RichCallInfo;
import com.mediatek.rcs.incallui.ext.RCSInCallUIPlugin;
import com.mediatek.rcs.incallui.service.CallStatusController;
import com.mediatek.rcs.incallui.utils.PhoneUtils;
import com.mediatek.rcs.incallui.utils.RichCallUtils;
import com.mediatek.rcs.phone.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RichCallController implements
    CallStatusController.OnCallItemChanged,
    RichCallUtils.CallInfoFetchedListener,
    RichCallUtils.CallInfoUpdatedListener,
    RichCallUtils.GeoInfoUptatedListener,
    RichCallUtils.SysLoginListener {

    private static final String TAG                     = "RichCallController";
    public  static final String NEW_OUTGOING_CALL_EXTRA = "InCallActivity.new_outgoing_call";

    private static final int QUEUE_INFO_UPDATE = 0;
    private static final int QUEUE_INFO_FETCH = 1;
    private static final int QUEUE_GEO_FETCH = 2;
    private static final int QUEUE_VT_PROCESSING = 3;

    private Context mContext;
    private Context mHostContext;
    private Context mHostApplicationContext;

    //To control call status and caculate call event according to vendor sdk
    private CallStatusController           mCallStatusController;
    //To record host layout params and rcs switch
    private RCSInCallUIPlugin              mRCSInCallUIPlugin;
    //For info query, login and so on
    private RichCallUtils                  mRichCallUtils;
    //Panel controller for gif && video show
    RichCallPanelController                mRichCallPanelController;
    //Listen for data connection change
    private TelephonyManager               mTelephonyManager;
    //Listen for subinfo change, like sim hot plugin-out
    private SubscriptionManager            mSubscriptionManager;
    private OnSubscriptionsChangedListener mSubscriptionsChangedListener;

    //To mark current call, because query is async, and when query complete, need to
    // check if call is changed
    private Call mCurrentCall;

    //Queue cache to queue the request before connect to server
    private ArrayList<QueueItem> mRequestQueue = new ArrayList<QueueItem>();
    //Listener for data connection changed
    private HashMap<Integer, RichCallStateListener> mRichCallStateListeners =
            new HashMap<Integer, RichCallStateListener>();

    //For vilte display, if vt is ring or dialing but not active, we should ask user to decide if
    //to show rich screen
    private AlertDialog mConfirmDialog;

    //Login status, to mark if it is in quering status or query result
    private int         mLoginStatus;

    //InCall activity visibility
    private boolean     mVisible;

    //Maybe for future use, no need now...
    private boolean     mIsVoLTESupport;

    public RichCallController(RCSInCallUIPlugin incallPlugin) {
        Log.d(TAG, "RichCallController, construct!");
        mContext = incallPlugin.getPluginContext();
        mRCSInCallUIPlugin = incallPlugin;
        mCallStatusController = new CallStatusController(mContext);
    }

    private void preStart() {
        Log.d(TAG, "preStart");

        mVisible = true;
        mLoginStatus = RichCallUtils.LOGIN_INIT;

        if (mCallStatusController != null) {
            mCallStatusController.addCallItemChangListener(this);
        }

        mRichCallUtils = new RichCallUtils();

        mRichCallPanelController = new RichCallPanelController(mContext, mRCSInCallUIPlugin);

        mTelephonyManager = TelephonyManager.from(mContext);
        mSubscriptionManager = SubscriptionManager.from(mContext);

        mSubscriptionsChangedListener = new OnSubscriptionsChangedListener() {
                                                @Override
                                                public void onSubscriptionsChanged() {
                                                    updateStateListener();
                                                }
                                            };
    }

    public void onStart() {
        preStart();

        mHostContext = (Context) mRCSInCallUIPlugin.getInCallActivity();
        mHostApplicationContext = mRCSInCallUIPlugin.getHostApplicationContext();
        //mRichCallUtils.initRichCallSystem(mHostContext);
        mRichCallUtils.initRichCallSystem(mHostApplicationContext);
        mRichCallUtils.loginRichCallSystem(mHostApplicationContext, this);
        mIsVoLTESupport = PhoneUtils.isVoLTESupport(mHostContext);
        registerStateListener();
    }

    public void clearResources() {
        //mIsVoLTESupport = false;
        //mCurrentCall = null;
        mHostContext = null;
        //mHostApplicationContext = null;
        //mRichCallPanelController = null;
        //mTelephonyManager = null;
        //mSubscriptionManager = null;
        //mSubscriptionsChangedListener = null;
        mCallStatusController.removeCallItemChangListener(this);

        mRichCallUtils.clearRichCallInfos();
        mRichCallUtils.clearCallbacks();
    }

    @Override
    public void onPhotoRefreshed(boolean isRefresh, String number) {
        Log.d(TAG, "onPhotoRefreshed, isRefresh = " + isRefresh);
        if (isRefresh) {
            mRichCallPanelController.refreshPhoto(mRCSInCallUIPlugin.getDrawable(number));
        }
    }

    @Override
    public void onCallChanged(boolean isChanged, Call call, String event) {
        Log.d(TAG, "onCallChanged, status = " + call.getState() + ", isChanged = " + isChanged);
        if (!mRCSInCallUIPlugin.isRCSEnable() || !mRCSInCallUIPlugin.checkHostLayout()) {
            return;
        }

        //Disconnected call should pause panel, or else when we dial a phone call without a sim.
        // If cached gif file, will show gif pic updated in incallactivity, this seem to be wrong.
        if (PhoneUtils.isDisconnect(call)) {
            mRichCallPanelController.pause();
            if (PhoneUtils.isVideoCall(call, mContext) && isVideoCallChanged(call)) {
                clearQueueItem();
                mRichCallPanelController.closePanel();
                dismissRichScreenConfirmDialog();
            }
            return;
        }

        if (call == null || PhoneUtils.isConferenceCall(call)) {
            //Conference call need to close panels
            mRichCallPanelController.closePanel();
            return;
        }

        if (!mIsVoLTESupport ||
                PhoneNumberUtils.isEmergencyNumber(PhoneUtils.parseNumber(call))) {
            //If no volte or no RCS or is Emergency number, just return and close the panels
            mRichCallPanelController.closePanel();
            return;
        }

        if (mLoginStatus == RichCallUtils.LOGIN_FAILED && mHostContext != null) {
            //Log.d(TAG, "onCallChanged, retry login!");
            //Maybe we should retry login?????
            //mRichCallUtils.loginRichCallSystem(/*mContext*/mHostContext, this);
        }

        String number = PhoneUtils.parseNumber(call);
        if (PhoneUtils.isDialing(call) || PhoneUtils.isIncoming(call)) {
            //Need to update call info when dialing and incoming, we should queue the request before
            //we logged in the richcall server
            if (mLoginStatus == RichCallUtils.LOGIN_SUCCESS) {
                mRichCallUtils.updatedRichCallInfo(number, event, this);
            } else if (mLoginStatus != RichCallUtils.LOGIN_FAILED) {
                enQueueItem(call, call.getState(), event, number, QUEUE_INFO_UPDATE);
            } else {
                //Maybe we should not retry login, so do nothing, need we do error handling???
            }
        }

        if (PhoneUtils.isIncoming(call) && PhoneUtils.isVideoCall(call, mContext)) {
            //To do, show dialog fragment if need to show RichScreen.
            //Add queue item, and process queue item later.
            //clearQueueItem();
            enQueueItem(call, call.getState(), event, number, QUEUE_INFO_FETCH);
            showRichScreenConfirmDialog();
            return;
        } else if ((PhoneUtils.isActive(call) || PhoneUtils.isDialing(call)) &&
                PhoneUtils.isVideoCall(call, mContext)) {
            Log.d(TAG, "onCallChanged, video call, close panel!");
            if (isVideoCallChanged(call)) {
                mRichCallPanelController.closePanel();
                clearQueueItem();
                dismissRichScreenConfirmDialog();
            }
            return;
        } else if (PhoneUtils.isConferenceCall(call)) {
            Log.d(TAG, "onCallChanged, isConferenceCall, close panel!");
            mRichCallPanelController.closePanel();
            return;
        }

        boolean cachedOrQuering = mRichCallUtils.isCallInfoCached(number)
                || mRichCallUtils.isCallInfoQuring(number);
        if (isChanged) {
            mCurrentCall = call;
            if (!TextUtils.isEmpty(number)) {
                Log.d(TAG, "onCallChanged, get call info!");
                //We need to set image view as visible, or else will get height and weith as 0
                mRichCallPanelController.openIconPanel(call);
                mRichCallPanelController.openDefaultPanelEx(cachedOrQuering);

                //Maybe we should not get the callInfo after logged in,
                //we should use this api as soon as we can.
                //if (mLoginStatus == RichCallUtils.LOGIN_SUCCESS) {
                    mRichCallUtils.getRichCallInfo(call, number, event, this);
                //} else if (mLoginStatus != RichCallUtils.LOGIN_FAILED){
                    //enQueueItem(call, call.getState(), event, number, QUEUE_INFO_FETCH);
                //} else {
                    //Maybe we should not retry login, so do nothing
                //}
            } else {
                //Log, the number is not valid.
                Log.d(TAG, "onCallChanged, the number is not valid!");
            }
        } else {
            if (!cachedOrQuering) {
                //We need to set image view as visible, or else will get height and weith as 0
                //And if no cached or it is in quering status, we should query again
                mRichCallPanelController.openIconPanel(call);
                mRichCallPanelController.openDefaultPanelEx(cachedOrQuering);

                //Maybe we should not get the callInfo after logged in,
                //we should use this api as soon as we can.
                //if (mLoginStatus == RichCallUtils.LOGIN_SUCCESS) {
                    mRichCallUtils.getRichCallInfo(call, number, event, this);
                //} else if (mLoginStatus != RichCallUtils.LOGIN_FAILED){
                    //enQueueItem(call, call.getState(), event, number, QUEUE_INFO_FETCH);
                //} else {
                    //Maybe we should not retry login, so do nothing
                //}
            }
        }
    }

    @Override
    public void onRichCallInfoFetched(Call call,
            String str, RichCallInfo info, boolean result) {
        Log.d(TAG, "onRichCallInfoFetched, info = " + info + ", result = " + result);
        if (call == null || result == false) {
            Log.d(TAG, "onRichCallInfoFetched, error!");
            return;
        }

        Call currCall = call;
        //We should check if call has been changed during the query process
        if (mCurrentCall != null) {
            if (!call.getCallId().equals(mCurrentCall.getCallId())) {
                Log.d(TAG, "onRichCallInfoFetched, current call already changed!");
                //Maybe for SRVCC, call id may changed, but number will not be changed
                String currNumber = PhoneUtils.parseNumber(mCurrentCall);
                String number = PhoneUtils.parseNumber(call);
                if (!currNumber.equals(number)) {
                    Log.d(TAG, "onRichCallInfoFetched, current number already changed!");
                    return;
                }
                //call number remain the same, so just use the newest call
                currCall = mCurrentCall;
            }
        } else {
            Log.d(TAG, "onRichCallInfoFetched, but current call is null!");
            return;
        }

        if (PhoneUtils.isConferenceCall(currCall)) {
            //Changed to conference call, maybe will not enter this logic?????
            return;
        }

        if (PhoneUtils.isVideoCall(currCall, mContext) &&
                (PhoneUtils.isActive(currCall) || PhoneUtils.isHeld(currCall))) {
            //Upgrade to a video call, need to close Video or Image panel
            mRichCallPanelController.closePanel();
            return;
        }

        if (result == true) {
            mRichCallPanelController.loadPanel(currCall, info);
        } else {
            //the RichCallInfo is not correct, we should show default panel
            mRichCallPanelController.openDefaultPanel();
            Log.d(TAG, "onRichCallInfoFetched, info is null");
        }
    }

    @Override
    public void onRichCallInfoUpdated(String str, boolean result) {
        Log.d(TAG, "onRichCallInfoUpdated, number = " + str + ", result = " + result);
    }

    @Override
    public void onRichCallGeoUpdated(boolean result) {
        Log.d(TAG, "onRichCallGeoUpdated, result = " + result);
    }

    @Override
    public void onRichCallSyncLogin(int status) {
        Log.d(TAG, "onRichCallSyncLogin, status = " + status);
        mLoginStatus = status;
        if (mLoginStatus == RichCallUtils.LOGIN_PROCESSING) {
            Log.d(TAG, "onRichCallSyncLogin, we are in quering status");
        } else if (mLoginStatus == RichCallUtils.LOGIN_SUCCESS) {
            deQueueItem();
        } else if (mLoginStatus == RichCallUtils.LOGIN_FAILED) {
            //We should clear queue when login failed, Maybe error handling???
            clearQueueItem();
        } else {
            //Maybe some invalid login status
        }
    }

    public void onVanish() {
        Log.d(TAG, "onVanish");
        if (mRCSInCallUIPlugin.isRCSEnable()) {
            mVisible = false;
            mLoginStatus = RichCallUtils.LOGIN_INIT;
            clearQueueItem();
            unregisterStateListener();
            //We need to release the adapter resource, so we send message
            //as soon as we can.
            mRichCallUtils.releaseAdapterResource();
            if (mRCSInCallUIPlugin.checkHostLayout()) {
                mRichCallPanelController.releaseResource();
            }
            clearResources();
        }
        mCallStatusController.onVanish();
        mCallStatusController = null;
        System.gc();
    }

    public boolean isNeedShowMenuItem() {
        return mCallStatusController.isNeedShowMenuItem();
    }

    public void onViewSetup(HashMap<Integer, TextView> map) {
        Log.d(TAG, "onViewSetup");
        mCallStatusController.onViewSetup(map);

        if (mRCSInCallUIPlugin.isRCSEnable() && mRCSInCallUIPlugin.checkHostLayout()) {
            mRichCallPanelController.init();
        }
    }

    public Call onMenuItemSelected() {
        Log.d(TAG, "onMenuItemSelected");
        //Click send message overflow menu item
        return mCallStatusController.onMenuItemSelected();
    }

    public void onCallStatusChange(HashMap<String, Call> call) {
        mCallStatusController.onCallStatusChange(call);
    }

    public void onCallPhotoChanged(Call call) {
        Log.d(TAG, "onCallPhotoChanged, call = " + call);
        //When host image query complete, will notice our plugin and call this
        mCallStatusController.onCallPhotoChanged(call);
    }

    public void onRecordStatusUpdated(boolean visible) {
        //Log.d(TAG, "onRecordStatusUpdated, visible = " + visible);
        //Updated for audio record state.
        mRichCallPanelController.updateAudioState(visible);
    }

    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");
        //We need to close video panel, or else the surface will show the background
        if (mRichCallPanelController.isPanelOpen(RichCallPanel.RCS_PANEL_VIDEO)) {
            //If it is add call mode, we should dismiss the video panel
            boolean newOutgoingCall = intent.getBooleanExtra(NEW_OUTGOING_CALL_EXTRA, false);
            if (newOutgoingCall) {
                mRichCallPanelController.closePanelByType(RichCallPanel.RCS_PANEL_VIDEO);
            }
        }
    }

    private void showRichScreenConfirmDialog() {
        Log.i(TAG, "showRichScreenConfirmDialog");
        AlertDialog mConfirmDialog = new AlertDialog.Builder(mContext)
            .setTitle(mContext.getString(R.string.rich_dialog_title))
            .setMessage(mContext.getString(R.string.rich_dialog_message))
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            deQueueItem();
                        }
                }).setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            clearQueueItem();
                        }
            }).create();
        mConfirmDialog.show();
    }

    private void dismissRichScreenConfirmDialog() {
        Log.i(TAG, "dismissRichScreenConfirmDialog");
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
            mConfirmDialog = null;
        }
    }

    private void updateStateListener() {
        Log.d(TAG, "updateStateListener");
        List<SubscriptionInfo> subInfos = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfos == null) {
            return;
        }

        //We should remove all the item in mRichCallStateListener, and then add the listener again.
        for (Integer id : mRichCallStateListeners.keySet()) {
            int subId = id.intValue();
            RichCallStateListener listener = mRichCallStateListeners.get(id);
            mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
        }
        mRichCallStateListeners.clear();

        for (int i = 0; i < subInfos.size(); i++) {
            int subId = subInfos.get(i).getSubscriptionId();
            if (!mRichCallStateListeners.containsKey(subId)) {
                RichCallStateListener listener = new RichCallStateListener(subId);
                mRichCallStateListeners.put(subId, listener);
                mTelephonyManager.listen(listener,
                        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            }
        }
    }

    private void registerStateListener() {
        Log.d(TAG, "registerStateListener");
        List<SubscriptionInfo> infos =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (infos == null) {
            return;
        }

        for (SubscriptionInfo info : infos) {
            int subId = info.getSubscriptionId();
            if (!mRichCallStateListeners.containsKey(subId)) {
                RichCallStateListener listener = new RichCallStateListener(subId);
                mRichCallStateListeners.put(subId, listener);
            }
        }

        //Listen for phone state changed
        for (RichCallStateListener listener : mRichCallStateListeners.values()) {
            mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        }

        //Listen for sim changed.
        mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
    }

    private void unregisterStateListener() {
        Log.d(TAG, "unregisterStateListener");
        //Remove for sim changed listener
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
        for (RichCallStateListener listener : mRichCallStateListeners.values()) {
            //Listen for nothing, unregister the phone state listener
            mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
        }
        mRichCallStateListeners.clear();
    }

    private class RichCallStateListener extends PhoneStateListener {
        private int mPreState;
        public RichCallStateListener(int subId) {
            super(subId);
            mPreState = TelephonyManager.DATA_UNKNOWN;
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.d(TAG, "onDataConnectionStateChanged, state = " + state +
                    ", type = " + networkType);
            //When data connection changed, we should notice sdk the change.
            if ((mPreState == TelephonyManager.DATA_CONNECTED ||
                 mPreState == TelephonyManager.DATA_UNKNOWN) &&
                state != TelephonyManager.DATA_CONNECTED) {
                mRichCallUtils.notifyDataConnectionChange();
            }
            mPreState = state;
        }
    }

    //Add queue to cache the video call query and the message before sdk system logged in
    private class QueueItem {
        int    mCallStatus;
        Call   mCall;
        String mEvent;
        String mNumber;
        int    mType;
        public boolean isStatusChanged() {
            if (mCall != null) {
                return !(mCall.getState() == mCallStatus);
            } else {
                return true;
            }
        }
        public void process() {
            Log.d(TAG, "[QueueItem]:process(), mNumber = "
                                    + mNumber + ", mType = " + mType);
            if (!mVisible) {
                Log.d(TAG, "[QueueItem]:process(), not visible!");
                return;
            }

            switch(mType) {
                case QUEUE_GEO_FETCH:
                    break;
                case QUEUE_INFO_UPDATE:
                    mRichCallUtils.updatedRichCallInfo(mNumber,
                            mEvent, RichCallController.this);
                    break;
                case QUEUE_INFO_FETCH:
                    mRichCallUtils.getRichCallInfo(mCall,
                            mNumber, mEvent, RichCallController.this);
                    break;
                case QUEUE_VT_PROCESSING:
                    if (!isStatusChanged()) {
                        mRichCallPanelController.openIconPanel(mCall);
                        mRichCallPanelController.openDefaultPanelEx(false);
                        mRichCallUtils.getRichCallInfo(mCall,
                                mNumber, mEvent, RichCallController.this);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void enQueueItem(Call call, int status,
                            String event, String number, int type) {
        Log.d(TAG, "enQueueItem");
        QueueItem item = new QueueItem();
        item.mCall = call;
        item.mCallStatus = status;
        item.mEvent = event;
        item.mNumber = number;
        item.mType = type;
        mRequestQueue.add(item);
    }

    private void deQueueItem() {
        Log.d(TAG, "deQueueItem, count = " + mRequestQueue.size());
        for (QueueItem item : mRequestQueue) {
            item.process();
        }
        mRequestQueue.clear();
    }

    private void clearQueueItem() {
        Log.d(TAG, "clearQueueItem, count = " + mRequestQueue.size());
        mRequestQueue.clear();
    }

    private QueueItem getVTQueueItem() {
        for (QueueItem item : mRequestQueue) {
            if (item.mType == QUEUE_VT_PROCESSING) {
                return item;
            }
        }
        return null;
    }

    private boolean isVideoCallChanged(Call call) {
        QueueItem item = getVTQueueItem();
        if (item == null) {
            return false;
        }

        if (item.mCall == call) {
            return true;
        }
        return false;
    }
}
