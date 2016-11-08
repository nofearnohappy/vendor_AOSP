package com.mediatek.voiceextension.common;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mediatek.voiceextension.VoiceCommonState;
import com.mediatek.voiceextension.common.ProcessRecord.ListenerRecord;
import com.mediatek.voiceextension.common.ProcessRecord.RequestState;
import com.mediatek.voiceextension.swip.ISwipCallback;

/**
 * Manage common information for all voice feature. All voice features need
 * implement it.
 *
 */
public abstract class FeatureManager implements ISetHandler, ISwipCallback {

    public int mFeatureType;
    public String mFeatureName;
    public final CommonManager mCommonMgr;

    public static final int SWIP_REQUEST_MSG = 1;
    public static final int SWIP_NOTIFY_MSG = 2;

    /**
     * FeatureManager constructor.
     *
     * @param featureType
     *            feature type
     * @param featureName
     *            feature name
     */
    public FeatureManager(int featureType, String featureName) {
        mFeatureType = featureType;
        mFeatureName = featureName;
        mCommonMgr = CommonManager.getInstance();
        mCommonMgr.getSwipInteractionLocked().registerCallback(mFeatureType,
                this);
    }

    /**
     * Handler the async operation.
     */
    public final Handler mCmdHander = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SWIP_REQUEST_MSG:
                handleSwipRequest((ProcessRecord) msg.obj);
                break;
            case SWIP_NOTIFY_MSG:
                handleSwipNotification((ProcessRecord) msg.obj, msg.arg1, msg.arg2);
                break;
            default:
                break;
            }
        }
    };

    /**
     * Get voice handler.
     *
     * @return voice handler
     */
    public Handler getHandler() {
        return mCmdHander;
    }

    /**
     * Handle swip request message.
     *
     * @param processRecord
     *            process record
     */
    public void handleSwipRequest(ProcessRecord processRecord) {

        synchronized (mCommonMgr) {
            if (mCommonMgr.isProcessExist(processRecord)) {
                ListenerRecord listenerRecord = processRecord.getListenerRecord(mFeatureName);
                if (listenerRecord != null) {
                    int result = listenerRecord.switchToNextReqState();
                    if (CommonManager.DEBUG) {
                        Log.d(CommonManager.TAG, "handleSwipRequest result=" + result
                                + " listenerRecord=" + listenerRecord + " isReqStateIdle="
                                + listenerRecord.isReqStateIdle() + " isWaitingSwipResponse="
                                + listenerRecord.isWaitingSwipResponse());
                    }
                    // If request state is idle , do nothing
                    if (!listenerRecord.isReqStateIdle()) {
                        RequestState state = listenerRecord.getCurReqState();
                        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                            // listenerRecord.startWaitingSwipResponse();
                            handleSwipRequestLocked(listenerRecord);
                        } else {
                            handleSwipNotificationLocked(listenerRecord,
                                    getApiType(state.getMainState(), state.getSubState()), result,
                                    0, null);
                            // Need to handle the next request
                            changeReqStateFromSwipLocked(processRecord,
                                    getApiType(state.getMainState(), state.getSubState()));
                        }
                    } else {
                        listenerRecord.stopWaitingSwipResponse();
                    }
                }
            } else {
                Log.e(CommonManager.TAG, "handleSwipRequest " + processRecord.toString()
                        + " not exist");
            }
        }
    }

    /**
     * Handle swip notification message.
     *
     * @param processRecord
     *            process record
     * @param apiType
     *            api tpye
     * @param result
     *            swip result
     */
    public void handleSwipNotification(ProcessRecord processRecord,
            int apiType, int result) {

        synchronized (mCommonMgr) {
            if (mCommonMgr.isProcessExist(processRecord)) {
                ListenerRecord listenerRecord = processRecord == null ? null
                        : processRecord.getListenerRecord(mFeatureName);
                if (listenerRecord != null) {
                    handleSwipNotificationLocked(listenerRecord, apiType,
                            result, 0, null);
                    // Need to handle the next request
                    changeReqStateFromSwipLocked(processRecord, apiType);
                }
            } else {
                Log.e(CommonManager.TAG, "handleSwipNotification "
                        + processRecord.toString() + " not exist");
            }
        }
    }

    private void changeReqStateFromSwipLocked(ProcessRecord processRecord,
            int apiType) {

        ListenerRecord listenerRecord = processRecord
                .getListenerRecord(mFeatureName);

        if (listenerRecord != null) {
            if (apiType == getApiType(
                    listenerRecord.getCurReqState().mMainState, listenerRecord
                            .getCurReqState().mSubState)) {
                listenerRecord.stopWaitingSwipResponse();
                scheduleSwipReqestMsgLocked(processRecord);
                if (CommonManager.DEBUG) {
                    Log.d(CommonManager.TAG,
                            "changeReqStateFromSwipLocked stopWaitingSwipResponse isWaitingSwipResponse="
                                    + listenerRecord.isWaitingSwipResponse());
                }
            }
        }
    }

    /**
     * Schedule swip request message.
     *
     * @param processRecord
     *            process record
     */
    public void scheduleSwipReqestMsgLocked(ProcessRecord processRecord) {
        ListenerRecord listenerRecord = processRecord
                .getListenerRecord(mFeatureName);
        if (CommonManager.DEBUG) {
            Log.d(CommonManager.TAG,
                    "scheduleSwipReqestMsgLocked processRecord "
                            + processRecord.getProcssName()
                            + " listenerRecord=" + listenerRecord);
        }
        if (listenerRecord != null && !listenerRecord.isWaitingSwipResponse()) {
            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG,
                        "scheduleSwipReqestMsgLocked processRecord success");
            }
            listenerRecord.startWaitingSwipResponse();
            Message msg = mCmdHander.obtainMessage(SWIP_REQUEST_MSG,
                    processRecord);
            mCmdHander.sendMessage(msg);
        }
    }

    /**
     * Schedule swip notify message.
     *
     * @param processRecord
     *            process record
     * @param apiType
     *            api type
     * @param result
     *            swip result
     */
    public void scheduleSwipNotifyMsgLocked(ProcessRecord processRecord,
            int apiType, int result) {
        if (CommonManager.DEBUG) {
            Log.d(CommonManager.TAG,
                    "scheduleSwipNotifyMsgLocked processRecord "
                            + processRecord.getProcssName()
                            + " listenerRecord="
                            + processRecord.getListenerRecord(mFeatureName)
                            + " isWaitingSwipResponse="
                            + processRecord.getListenerRecord(mFeatureName)
                                    .isWaitingSwipResponse());
        }
        Message msg = mCmdHander.obtainMessage(SWIP_NOTIFY_MSG, apiType,
                result, processRecord);
        mCmdHander.sendMessage(msg);
    }

    /**
     * Register listener from voice extension service.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param listener
     *            a callback that receive swip notification
     * @return result
     */
    public int registerListener(int pid, int uid, Object listener) {
        // TODO Auto-generated method stub
        int result = VoiceCommonState.RET_COMMON_SUCCESS;
        if (listener == null) {
            result = VoiceCommonState.RET_COMMON_LISTENER_ILLEGAL;
        } else {
            synchronized (mCommonMgr) {
                result = mCommonMgr.registerListenerLocked(pid, uid, listener,
                        mFeatureType, CommonManager.STATE_IDLE,
                        CommonManager.SUBSTATE_IDLE, this);
            }
        }
        return result;
    }

    @Override
    public int selectSet(int pid, int uid, String setName) {
        // TODO Auto-generated method stub
        int result = checkSetFormat(setName);
        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            synchronized (mCommonMgr) {
                result = mCommonMgr.selectSetLocked(pid, uid, setName,
                        mFeatureType);
            }
        }

        return result;
    }

    @Override
    public int createSet(int pid, int uid, String setName) {
        // TODO Auto-generated method stub
        int result = checkSetFormat(setName);
        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            synchronized (mCommonMgr) {
                result = mCommonMgr.createSetLocked(pid, uid, setName,
                        mFeatureType);
            }
        }

        return result;
    }

    @Override
    public int deleteSet(int pid, int uid, String setName) {
        // TODO Auto-generated method stub
        int result = checkSetFormat(setName);
        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            synchronized (mCommonMgr) {
                result = mCommonMgr.deleteSetLocked(pid, uid, setName,
                        mFeatureType);
            }
        }
        return result;
    }

    @Override
    public int isSetCreated(int pid, int uid, String setName) {
        // TODO Auto-generated method stub
        int result = checkSetFormat(setName);
        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            synchronized (mCommonMgr) {
                result = mCommonMgr.isSetCreatedLocked(pid, uid, setName,
                        mFeatureType);
            }
        }
        return result;
    }

    @Override
    public String getSetSelected(int pid, int uid) {
        // TODO Auto-generated method stub
        synchronized (mCommonMgr) {
            return mCommonMgr.getSetSelected(pid, uid, mFeatureType);
        }
    }

    @Override
    public String[] getAllSets(int pid, int uid) {
        synchronized (mCommonMgr) {
            String[] swipSets = mCommonMgr.getAllSetsLocked(pid, uid,
                    mFeatureType);
//            if (swipSets != null) {
//                for (int i = 0; i < swipSets.length; i++) {
//                    swipSets[i] = ProcessRecord
//                            .getSetNameFromSwipSet(swipSets[i]);
//                }
//            }
            return swipSets;
        }
    }

    private int checkSetFormat(String setName) {
        int result = setName == null ? VoiceCommonState.RET_SET_ILLEGAL
                : VoiceCommonState.RET_COMMON_SUCCESS;
        // Need to check the setName length , character format and so on
        return result;
    }

    /**
     * Notify message from native swip.
     *
     * @param swipSetName
     *            swip command set name
     * @param apiType
     *            api type
     * @param result
     *            swip result
     * @param extraMsg
     *            command id
     * @param extraObj
     *            commands string list
     */
    @Override
    public void onSwipMessageNotify(String swipSetName, int apiType,
            int result, int extraMsg, Object extraObj) {

        synchronized (mCommonMgr) {
            String processName = ProcessRecord
                    .getProcessNameFromSwipSet(swipSetName);
            // int pid = ProcessRecord.getProcessPidFromSwipSet(swipSetName);
            String setName = ProcessRecord.getSetNameFromSwipSet(swipSetName);

            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG, "onSwipMessageNotify processName = "
                        + processName + " setName=" + setName);
            }

            ProcessRecord processRecord = mCommonMgr
                    .getProcessRecordLocked(processName);
            ListenerRecord listenerRecord = processRecord == null ? null
                    : processRecord.getListenerRecord(mFeatureName);
            if (listenerRecord != null) {
                handleSwipNotificationLocked(listenerRecord, apiType, result,
                        extraMsg, extraObj);
                // Need to handle the next request
                changeReqStateFromSwipLocked(processRecord, apiType);
            }
        }
    }

    /**
     * Get api type from mainstate and substate.
     *
     * @param mainState
     *            main state
     * @param subState
     *            sub state
     * @return api type
     */
    public abstract int getApiType(int mainState, int subState);

    // public abstract boolean swipNotifyMeetCurRequest(int apiType);

    /**
     * Handle swip request message.
     *
     * @param record
     *            listener record
     */
    public abstract void handleSwipRequestLocked(ListenerRecord record);

    /**
     * Handle swip notification message.
     *
     * @param listenerRecord
     *            listener record
     * @param apiType
     *            api type
     * @param result
     *            swip result
     * @param extraMsg
     *            command id
     * @param extraObj
     *            command string list
     */
    public abstract void handleSwipNotificationLocked(
            ListenerRecord listenerRecord, int apiType, int result,
            int extraMsg, Object extraObj);

    /**
     * Check request main state/ sub state if legal.
     *
     * @param curMainState
     *            current main state
     * @param curSubState
     *            current sub state
     * @param swipMainState
     *            swip main state
     * @param swipSubState
     *            swip sub state
     * @param reqMainState
     *            request main state
     * @param reqSubState
     *            request sub state
     * @return result
     */
    public abstract int checkRequestMutexState(int curMainState,
            int curSubState, int swipMainState, int swipSubState,
            int reqMainState, int reqSubState);

    /**
     * The callback result of process dead.
     *
     * @param record
     *            process record
     */
    public abstract void onProcessDiedLocked(ProcessRecord record);

}
