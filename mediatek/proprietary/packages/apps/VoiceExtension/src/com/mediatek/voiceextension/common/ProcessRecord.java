package com.mediatek.voiceextension.common;

import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;

import com.mediatek.voiceextension.VoiceCommonState;

import java.util.ArrayList;

/**
 * Record the process info and the registered listeners from one application.
 *
 */
public class ProcessRecord implements IBinder.DeathRecipient {

    private int mPid;
    private int mUid;
    private String mProcessName;

    ListenerRecord mSwipOccupiedListener = null;
    private static final String sProcessMark = ":=";
    // private static final String sPidMark = "&";
    private static final String sFeatureMark = "_";

    // Key means feature name
    ArrayMap<String, ListenerRecord> mListeners = new ArrayMap<String, ListenerRecord>();

    /**
     * ProcessRecord constructor.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param processName
     *            process name
     */
    public ProcessRecord(int pid, int uid, String processName) {
        mPid = pid;
        mUid = uid;
        mProcessName = processName;
    }

    /**
     * Get process name of the process record.
     *
     * @return process name
     */
    public String getProcssName() {
        return mProcessName;
    }

    /**
     * Get process id of the process record.
     *
     * @return process id
     */
    public int getPid() {
        return mPid;
    }

    /**
     * Get user id of the process record.
     *
     * @return user id
     */
    public int getUid() {
        return mUid;
    }

    /**
     * Get the listener record name.
     * @param name
     *            the feature name
     * @return the listener record name
     */
    public ListenerRecord getListenerRecord(String name) {
        return mListeners.get(name);
    }

    /**
     * Add listener record to voice service.
     *
     * @param name
     *            the feature name
     * @param record
     *            listener record
     */
    public void addListenerRecord(String name, ListenerRecord record) {
        mListeners.put(name, record);
    }

    /**
     * Create a new listener record.
     *
     * @return listener record
     */
    public ListenerRecord createListenerRecord() {
        return new ListenerRecord();
    }

    /**
     * Switch internal listener in the same process.
     *
     * @param record
     *            listener record
     * @return result
     */
    public int switchSwipListenerRecord(ListenerRecord record) {
        int result = VoiceCommonState.RET_COMMON_SUCCESS;

        if (!mListeners.containsValue(record)) {
            result = VoiceCommonState.RET_COMMON_PROCESS_ILLEGAL;
        } else if (mSwipOccupiedListener != null
                && mSwipOccupiedListener != record) {
            result = VoiceCommonState.RET_COMMON_MIC_OCCUPIED;
        } else if (record.getSetName() == null) {
            result = VoiceCommonState.RET_SET_NOT_SELECTED;
        } else {
            mSwipOccupiedListener = record;
        }
        return result;
    }

    /**
     * Release the swip occupied listener.
     */
    public void releaseSwipListenerRecord() {
        if (mSwipOccupiedListener != null) {
            mSwipOccupiedListener.idle();
            mSwipOccupiedListener = null;

        }
    }

    /**
     * Check if the listener is the swip occupied listener.
     *
     * @param record
     *            the new create listener
     * @return true if the listener is the swip occupied listener, otherwise
     *         false
     */
    public boolean isListenerOccupiedSwip(ListenerRecord record) {
        return mSwipOccupiedListener != null && mSwipOccupiedListener == record;
    }

    /**
     * Get the swip occupied listener.
     *
     * @return the swip occupied listener
     */
    public ListenerRecord getSwipListenerRecord() {
        return mSwipOccupiedListener;
    }

    @Override
    public void binderDied() {
        // TODO Auto-generated method stub
        synchronized (CommonManager.getInstance()) {
            // Need to notify swip that process died if occupied swip
            if (mSwipOccupiedListener != null) {
                mSwipOccupiedListener.mCommonHandler.onProcessDiedLocked(this);
                mSwipOccupiedListener = null;
            }
            mListeners.clear();
            CommonManager.getInstance().onProcessDiedLocked(this);
        }
    }

    /**
     * Join together the ProcessRecord information.
     *
     * @return String information of this process record
     */
    public String toString() {
        return "ProcessName = " + mProcessName + " Pid=" + mPid + " mUid="
                + mUid;
    }

    /**
     * Get process name from set name of swip callback.
     *
     * @param swipSetName
     *            set name of swip callback
     * @return process name
     */
    public static String getProcessNameFromSwipSet(String swipSetName) {

        if (swipSetName != null) {
            return swipSetName.split(sProcessMark)[0];
        }
        return null;
    }

//    public static int getProcessPidFromSwipSet(String swipSetName) {
//        if (swipSetName != null) {
//            return Integer.parseInt(swipSetName.split(sProcessMark)[0].split(sPidMark)[1]);
//        }
//        return -1;
//    }

    /**
     * Get command set name from set name of swip callback.
     *
     * @param swipSetName
     *            set name of swip callback
     * @return command set name
     */
    public static String getSetNameFromSwipSet(String swipSetName) {
        if (swipSetName != null) {
            return swipSetName.split(sFeatureMark)[1];
        }
        return null;
    }

    /**
     * Get concatenation set name for swip.
     *
     * @param processName
     *            process name
     * @param pid
     *            process id
     * @param featureName
     *            feature name
     * @param setName
     *            command set name
     * @return set name
     */
    public static String getSetNameForSwip(String processName, int pid, String featureName,
            String setName) {
        if (processName != null && pid > 0 && featureName != null && setName != null) {
            return processName + sProcessMark + featureName + sFeatureMark + setName;
        }
        return null;
    }

    /**
     * Application request message information.
     *
     */
    public class RequestState {
        int mMainState;
        int mSubState;
        long mReqTime;
        Object mExtraObj1;
        Object mExtraObj2;

        RequestState(int mainState, int subState, Object obj1, Object obj2) {
            mMainState = mainState;
            mSubState = subState;
            mExtraObj1 = obj1;
            mExtraObj2 = obj2;
            mReqTime = System.currentTimeMillis();
        }

        public int getMainState() {
            return mMainState;
        }

        public int getSubState() {
            return mSubState;
        }

        public Object getExtraData1() {
            return mExtraObj1;
        }

        public Object getExtraData2() {
            return mExtraObj2;
        }
    }

    /**
     * Listener record information of this process.
     *
     */
    public class ListenerRecord {

        private Object mCurListener = null;
        private String mSetName;
        private RequestState mCurReqState;
        private RequestState mCurSwipState;
        private RequestState mInitState;
        private ArrayList<RequestState> mReqStateCaches = new ArrayList<RequestState>();
        private int mFeatureType;
        private String mFeatureName;
        private FeatureManager mCommonHandler;
        private boolean mWaitingSwipResponse;

        /**
         * Init listener record information of this process.
         *
         * @param listener
         *            a callback that receive asynchronous notification from
         *            swip
         * @param featureType
         *            feature type
         * @param featureName
         *            feature name
         * @param mainState
         *            main state
         * @param subState
         *            sub state
         * @param handler
         *            FeatureManager instance
         */
        public void init(Object listener, int featureType, String featureName,
                int mainState, int subState, FeatureManager handler) {

            mCurListener = listener;
            mFeatureType = featureType;
            mFeatureName = featureName;
            mInitState = new RequestState(mainState, subState, null, null);
            mCommonHandler = handler;
            idle();
        }

        private void idle() {
            mWaitingSwipResponse = false;
            mCurReqState = mCurSwipState = mInitState;
        }

        public boolean isWaitingSwipResponse() {
            return mWaitingSwipResponse;
        }

        /**
         * Start waiting swip response, at this stage, service can't not handle
         * another request state.
         */
        public void startWaitingSwipResponse() {
            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG, "startWaitingSwipResponse");
            }
            mWaitingSwipResponse = true;
        }

        /**
         * Stop waiting swip response, at this stage, service can handle the
         * next state from cache request.
         */
        public void stopWaitingSwipResponse() {
            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG, "stopWaitingSwipResponse");
            }
            mWaitingSwipResponse = false;
        }

        public Object getListener() {
            return mCurListener;
        }

        public int getFeatureType() {
            return mFeatureType;
        }

        public String getSetName() {
            return mSetName;
        }

        public void setSetName(String name) {
            mSetName = name;
        }

        public String getSwipSetName() {
            return getSetNameForSwip(mProcessName, mPid, mFeatureName, mSetName);
        }

        /**
         * Cache the request state for service state switch.
         *
         * @param mainState
         *            the request main state
         * @param subState
         *            the request stub state
         * @param extra1
         *            extra data1
         * @param extra2
         *            extra data2
         */
        public void cacheRequestState(int mainState, int subState,
                Object extra1, Object extra2) {
            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG, "cacheRequestState mainState:"
                        + mainState + ", subState" + subState + ", extra1:"
                        + extra1 + ", extra2:" + extra2);
            }
            mReqStateCaches.add(new RequestState(mainState, subState, extra1,
                    extra2));
        }

        /**
         * Switch the next request state if handle over the current state.
         *
         * @return result
         */
        public int switchToNextReqState() {
            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG,
                        "switchToNextReqState mReqStateCaches.size():"
                                + mReqStateCaches.size());
            }
            RequestState state = mReqStateCaches.size() > 0 ? mReqStateCaches
                    .remove(0) : mInitState;

            int result = mCommonHandler.checkRequestMutexState(
                    mCurReqState.mMainState, mCurReqState.mSubState,
                    mCurSwipState.mMainState, mCurSwipState.mSubState,
                    state.mMainState, state.mSubState);

            mCurReqState = state;

            return result;
        }

        /**
         * Switch the swip state if receive the notification from native swip.
         *
         * @param notifyMainState
         *            main state from native swip
         * @param notifySubState
         *            sub state from native swip
         */
        public void switchSwipState(int notifyMainState, int notifySubState) {
            mCurSwipState = new RequestState(notifyMainState, notifySubState,
                    null, null);
        }

        public RequestState getCurReqState() {
            return mCurReqState;
        }

        public RequestState getCurSwipState() {
            return mCurSwipState;
        }

        public boolean isReqStateIdle() {
            return mCurReqState == mInitState;
        }

        public boolean isSwipStateIdle() {
            return mCurSwipState == mInitState;
        }

        public ProcessRecord getProcessRecord() {
            return ProcessRecord.this;
        }
    }

}
