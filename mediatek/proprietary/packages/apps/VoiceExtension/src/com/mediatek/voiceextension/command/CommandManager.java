package com.mediatek.voiceextension.command;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.common.voiceextension.IVoiceExtCommandListener;
import com.mediatek.voiceextension.VoiceCommonState;
import com.mediatek.voiceextension.common.CommonManager;
import com.mediatek.voiceextension.common.FeatureManager;
import com.mediatek.voiceextension.common.ProcessRecord;
import com.mediatek.voiceextension.common.ProcessRecord.ListenerRecord;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Manage voice command information for voice command feature.
 *
 */
public class CommandManager extends FeatureManager {

    // Sub state for commands
    private static final int SUBSTATE_COMMANDS_SETUP = (CommonManager.SUBSTATE_IDLE << 1) & 0xFF;
    // Sub state for recognition
    private static final int SUBSTATE_RECOGNITION_START = (CommonManager.SUBSTATE_IDLE << 1) & 0xFF;
    private static final int SUBSTATE_RECOGNITION_STARTED = (CommonManager.SUBSTATE_IDLE << 2) & 0xFF;
    private static final int SUBSTATE_RECOGNITION_PAUSE = (CommonManager.SUBSTATE_IDLE << 3) & 0xFF;
    private static final int SUBSTATE_RECOGNITION_PAUSED = (CommonManager.SUBSTATE_IDLE << 4) & 0xFF;
    private static final int SUBSTATE_RECOGNITION_RESUME = (CommonManager.SUBSTATE_IDLE << 5) & 0xFF;
    private static final int SUBSTATE_RECOGNITION_STOP = (CommonManager.SUBSTATE_IDLE << 6) & 0xFF;

    CommandManager() {
        super(VoiceCommonState.FEATURE_TYPE_COMMAND, VoiceCommonState.FEATURE_TYPE_COMMAND_NAME);
    }

    @Override
    public int getApiType(int mainState, int subState) {
        int type = VoiceCommonState.API_TYPE_COMMAND_IDLE;
        if (mainState == CommonManager.STATE_FEATURE_INTERNAL) {
            switch (subState) {
            case SUBSTATE_COMMANDS_SETUP:
                return VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET;
            default:
                break;
            }
        } else if (mainState == CommonManager.STATE_FEATURE_SWIP) {
            switch (subState) {
            case SUBSTATE_RECOGNITION_START:
                return VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_START;
            case SUBSTATE_RECOGNITION_STOP:
                return VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_STOP;
            case SUBSTATE_RECOGNITION_PAUSE:
                return VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_PAUSE;
            case SUBSTATE_RECOGNITION_RESUME:
                return VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESUME;
            default:
                break;
            }
        }
        return type;
    }

    @Override
    public void handleSwipRequestLocked(ListenerRecord record) {
        int mainState = record.getCurReqState().getMainState();
        int subState = record.getCurReqState().getSubState();
        if (CommonManager.DEBUG) {
            Log.d(CommonManager.TAG, "handleSwipRequestLocked mainState:" + mainState
                    + ", subState:" + subState);
        }
        if (mainState == CommonManager.STATE_FEATURE_INTERNAL) {
            switch (subState) {
            case SUBSTATE_COMMANDS_SETUP:
                if (record.getCurReqState().getExtraData1() instanceof String) {
                    mCommonMgr.getSwipInteractionLocked().setCommands(record.getSwipSetName(),
                            (String[]) record.getCurReqState().getExtraData2());
                } else {
                    mCommonMgr.getSwipInteractionLocked().setCommands(record.getSwipSetName(),
                            (byte[]) record.getCurReqState().getExtraData2(),
                            (Boolean) record.getCurReqState().getExtraData1());
                }
                break;
            default:
                break;
            }
        } else if (mainState == CommonManager.STATE_FEATURE_SWIP) {
            if (record.isWaitingSwipResponse()) {
                switch (subState) {
                case SUBSTATE_RECOGNITION_START:
                    mCommonMgr.getSwipInteractionLocked().startRecognition(record.getSwipSetName(),
                            mFeatureType);
                    break;
                case SUBSTATE_RECOGNITION_STOP:
                    mCommonMgr.getSwipInteractionLocked().stopRecognition(record.getSwipSetName(),
                            mFeatureType);
                    break;
                case SUBSTATE_RECOGNITION_PAUSE:
                    mCommonMgr.getSwipInteractionLocked().pauseRecognition(record.getSwipSetName(),
                            mFeatureType);
                    break;
                case SUBSTATE_RECOGNITION_RESUME:
                    mCommonMgr.getSwipInteractionLocked().resumeRecognition(
                            record.getSwipSetName(), mFeatureType);
                    break;
                default:
                    break;
                }
            }
        }
    }

    @Override
    public void handleSwipNotificationLocked(ListenerRecord listenerRecord, int apiType,
            int result, int extraMsg, Object extraObj) {
        if (CommonManager.DEBUG) {
            Log.d(CommonManager.TAG, "handleSwipNotificationLocked :" + "apiType=" + apiType
                    + " result=" + result);

        }
        switch (apiType) {
        case VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET:
            onSetCommandsNotifyLocked(listenerRecord, result);
            break;
        case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_START:
            onStartRecognitionNotifyLocked(listenerRecord, result);
            break;
        case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_PAUSE:
            onPauseRecognitionNotifyLocked(listenerRecord, result);
            break;
        case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESUME:
            onResumeRecognitionNotifyLocked(listenerRecord, result);
            break;
        case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_STOP:
            onStopRecognitionNotifyLocked(listenerRecord, result);
            break;
        case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESULT:
            onCommandRecognizedLocked(listenerRecord, result, extraMsg, (String[]) extraObj);
            break;
        case VoiceCommonState.API_TYPE_COMMAND_NOTIFY_ERROR:
            onSwipErrorNotifyLocked(listenerRecord, result);
            break;
        default:
            break;
        }
    }

    @Override
    public int checkRequestMutexState(int curMainState, int curSubState, int swipMainState,
            int swipSubState, int reqMainState, int reqSubState) {
        // TODO Auto-generated method stub
        int result = VoiceCommonState.RET_COMMON_SUCCESS;
        if (CommonManager.DEBUG) {
            Log.d(CommonManager.TAG, "checkRequestMutexState curMainState=" + curMainState
                    + " curSubState=" + curSubState + " swipMainState=" + swipMainState
                    + " swipSubState=" + swipSubState + " reqMainState=" + reqMainState
                    + " reqSubState=" + reqSubState);
        }

        // Filter action of pause, resume, stop before start.
        if (swipMainState == CommonManager.STATE_IDLE) {
            if (reqMainState == CommonManager.STATE_FEATURE_SWIP
                    && reqSubState != SUBSTATE_RECOGNITION_START) {
                result = VoiceCommonState.RET_COMMON_RECOGNITION_NEVER_STARTED;
            }
        }

        // Start to check swip state
        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            if (swipMainState == CommonManager.STATE_FEATURE_SWIP) {
                // Used for action of pausing , resuming , stopping.
                if (reqMainState < swipMainState && reqMainState != CommonManager.STATE_IDLE) {
                    result = VoiceCommonState.RET_COMMON_MIC_OCCUPIED;
                } else if (reqMainState == swipMainState) {
                    if (swipSubState == SUBSTATE_RECOGNITION_STARTED) {
                        if (reqSubState == SUBSTATE_RECOGNITION_START) {
                            result = VoiceCommonState.RET_COMMON_RECOGNITION_ALREADY_STARTED;
                        }
                        if (reqSubState == SUBSTATE_RECOGNITION_RESUME) {
                            result = VoiceCommonState.RET_COMMON_RECOGNITION_NEVER_PAUSED;
                        }
                    } else if (swipSubState == SUBSTATE_RECOGNITION_PAUSED) {
                        if (reqSubState == SUBSTATE_RECOGNITION_PAUSE
                                || reqSubState == SUBSTATE_RECOGNITION_START) {
                            result = VoiceCommonState.RET_COMMON_RECOGNITION_ALREADY_PAUSED;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void onProcessDiedLocked(ProcessRecord record) {
        // TODO Auto-generated method stub
        if (record.getSwipListenerRecord().getCurSwipState().getMainState() == CommonManager.STATE_FEATURE_SWIP) {
            // Tell swip to stop recognition
            mCommonMgr.getSwipInteractionLocked().stopRecognition(
                    record.getSwipListenerRecord().getSwipSetName(), mFeatureType);
        }
    }

    /**
     * Get commands list from swip.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @return commands list
     */
    public String[] getCommands(int pid, int uid) {
        String[] commands = null;
        synchronized (mCommonMgr) {
            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            ListenerRecord listenerRecord = processRecord == null ? null : processRecord
                    .getListenerRecord(mFeatureName);
            if (listenerRecord != null) {
                // Ask swip to get the commands
                commands = mCommonMgr.getSwipInteractionLocked().getCommands(
                        listenerRecord.getSwipSetName());
            }
        }
        return commands;
    }

    /**
     * Start command recognition.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     */
    public void startRecognition(int pid, int uid) {
        synchronized (mCommonMgr) {
            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            // Switch to the swip process & listener
            int result = mCommonMgr.switchSwipOccupiedProcessLocked(processRecord, mFeatureType);
            if (CommonManager.DEBUG) {
                Log.d(CommonManager.TAG, "startRecognition result = " + result);
            }
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                // Cache the request for handling in the main handler
                processRecord.getSwipListenerRecord().cacheRequestState(
                        CommonManager.STATE_FEATURE_SWIP, SUBSTATE_RECOGNITION_START, 0, null);
                scheduleSwipReqestMsgLocked(processRecord);
            } else if (result != VoiceCommonState.RET_COMMON_PROCESS_ILLEGAL) {
                scheduleSwipNotifyMsgLocked(processRecord,
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_START, result);
            }
        }
    }

    /**
     * Stop command recognition.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     */
    public void stopRecognition(int pid, int uid) {
        synchronized (mCommonMgr) {
            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            // Switch to the swip process & listener
            int result = mCommonMgr.switchSwipOccupiedProcessLocked(processRecord, mFeatureType);
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                // Cache the request for handling in the main handler
                processRecord.getSwipListenerRecord().cacheRequestState(
                        CommonManager.STATE_FEATURE_SWIP, SUBSTATE_RECOGNITION_STOP, 0, null);
                scheduleSwipReqestMsgLocked(processRecord);
            } else if (result != VoiceCommonState.RET_COMMON_PROCESS_ILLEGAL) {
                scheduleSwipNotifyMsgLocked(processRecord,
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_STOP, result);
            }
        }
    }

    /**
     * Pause command recognition.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     */
    public void pauseRecognition(int pid, int uid) {
        synchronized (mCommonMgr) {
            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            // Switch to the swip process & listener
            int result = mCommonMgr.switchSwipOccupiedProcessLocked(processRecord, mFeatureType);
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                // Cache the request for handling in the main handler
                processRecord.getSwipListenerRecord().cacheRequestState(
                        CommonManager.STATE_FEATURE_SWIP, SUBSTATE_RECOGNITION_PAUSE, 0, null);
                scheduleSwipReqestMsgLocked(processRecord);
            } else if (result != VoiceCommonState.RET_COMMON_PROCESS_ILLEGAL) {
                scheduleSwipNotifyMsgLocked(processRecord,
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_PAUSE, result);
            }
        }
    }

    /**
     * Resume command recognition.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     */
    public void resumeRecognition(int pid, int uid) {
        synchronized (mCommonMgr) {
            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            // Switch to the swip process & listener
            int result = mCommonMgr.switchSwipOccupiedProcessLocked(processRecord, mFeatureType);
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                // Cache the request for handling in the main handler
                processRecord.getSwipListenerRecord().cacheRequestState(
                        CommonManager.STATE_FEATURE_SWIP, SUBSTATE_RECOGNITION_RESUME, 0, null);
                scheduleSwipReqestMsgLocked(processRecord);
            } else if (result != VoiceCommonState.RET_COMMON_PROCESS_ILLEGAL) {
                scheduleSwipNotifyMsgLocked(processRecord,
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESUME, result);
            }
        }
    }

    /**
     * Sets up the commands list.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param fd
     *            ParcelFileDescriptor data
     * @param offset
     *            offset
     * @param length
     *            ParcelFileDescriptor data length
     */
    public void setCommands(int pid, int uid, ParcelFileDescriptor fd, int offset, int length) {
        // Use fd to read the file data and send to swip
        synchronized (mCommonMgr) {

            if (CommonManager.DEBUG) {
                Log.i(CommonManager.TAG, "setCommands fd=" + fd + " offset=" + offset + " length="
                        + length);
            }

            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            ListenerRecord listenerRecord = processRecord == null ? null : processRecord
                    .getListenerRecord(mFeatureName);
            if (listenerRecord != null) {
                if (processRecord != mCommonMgr.getOccupiedSwipProcessLocked()) {
                    if (listenerRecord.getSetName() != null) {
                        if (fd == null) {
                            scheduleSwipNotifyMsgLocked(processRecord,
                                    VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                                    VoiceCommonState.RET_COMMAND_FILE_ILLEGAL);
                        } else {
                            FileInputStream fis = null;
                            try {
                                fis = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                                int fisLength = fis.available();
                                if (offset + length > fisLength) {
                                    scheduleSwipNotifyMsgLocked(processRecord,
                                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                                            VoiceCommonState.RET_COMMAND_DATA_INVALID);
                                } else {
                                    fis.skip(offset);
                                    byte[] buffer = new byte[length];
                                    int index = 0;
                                    int readNum = 0;
                                    while (index < length) {
                                        readNum = fis.read(buffer, index, length - index);
                                        if (CommonManager.DEBUG) {
                                            Log.i(CommonManager.TAG, "setCommands read length="
                                                    + length + " index=" + index + " redNum="
                                                    + readNum + " fis.available=" + fisLength);
                                        }
                                        if (readNum < 0) {
                                            break;
                                        }
                                        index += readNum;
                                    }

                                    if (CommonManager.DEBUG) {
                                        String buf = null;
                                        if (length >= 100) {
                                            buf = new String(buffer, length - 100, 99, "utf-8");
                                        } else {
                                            buf = new String(buffer, "utf-8");
                                        }

                                        Log.i(CommonManager.TAG, "Fd Buffer=" + buf);
                                    }

                                    listenerRecord.cacheRequestState(
                                            CommonManager.STATE_FEATURE_INTERNAL,
                                            SUBSTATE_COMMANDS_SETUP, true, buffer);
                                    scheduleSwipReqestMsgLocked(processRecord);
                                }
                                fis.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                scheduleSwipNotifyMsgLocked(processRecord,
                                        VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                                        VoiceCommonState.RET_COMMAND_DATA_INVALID);
                                Log.e(CommonManager.TAG,
                                        "SetCommonads read fd error " + e.toString());
                            }
                        }
                    } else {
                        scheduleSwipNotifyMsgLocked(processRecord,
                                VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                                VoiceCommonState.RET_SET_NOT_SELECTED);
                    }

                } else {
                    scheduleSwipNotifyMsgLocked(processRecord,
                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                            VoiceCommonState.RET_SET_OCCUPIED);
                }
            }
            try {
                fd.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG, "SetCommonads close fd error " + e.toString());
            }
        }
    }

    /**
     * Sets up the commands list.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param commands
     *            commands list
     */
    public void setCommands(int pid, int uid, String[] commands) {
        synchronized (mCommonMgr) {
            ProcessRecord processRecord = mCommonMgr.getProcessRecordLocked(pid, uid);
            ListenerRecord listenerRecord = processRecord == null ? null : processRecord
                    .getListenerRecord(mFeatureName);
            if (listenerRecord != null) {
                if (processRecord != mCommonMgr.getOccupiedSwipProcessLocked()) {
                    if (listenerRecord.getSetName() != null) {
                        // Cache the request for handling in the main handler
                        // processRecord.switchSwipListenerRecord(listenerRecord);
                        listenerRecord.cacheRequestState(CommonManager.STATE_FEATURE_INTERNAL,
                                SUBSTATE_COMMANDS_SETUP, "s", commands);
                        scheduleSwipReqestMsgLocked(processRecord);
                    } else {
                        scheduleSwipNotifyMsgLocked(processRecord,
                                VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                                VoiceCommonState.RET_SET_NOT_SELECTED);
                    }
                } else {
                    scheduleSwipNotifyMsgLocked(processRecord,
                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                            VoiceCommonState.RET_SET_OCCUPIED);
                }
            }
        }
    }

    private void onSetCommandsNotifyLocked(ListenerRecord listenerRecord, int result) {
        if (listenerRecord != null) {
            listenerRecord.switchSwipState(CommonManager.STATE_IDLE, CommonManager.SUBSTATE_IDLE);
            // listenerRecord.getProcessRecord().releaseSwipListenerRecord();
            try {
                ((IVoiceExtCommandListener) listenerRecord.getListener()).onSetCommands(result);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG, "onSetCommandsNotify " + listenerRecord.getSwipSetName()
                        + " IPC fail");
            }
        }
    }

    private void onStartRecognitionNotifyLocked(ListenerRecord listenerRecord, int result) {
        if (listenerRecord != null) {
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                listenerRecord.switchSwipState(CommonManager.STATE_FEATURE_SWIP,
                        SUBSTATE_RECOGNITION_STARTED);
            } else {
                mCommonMgr.releaseSwipOccupiedProcessLocked();
            }
            try {
                ((IVoiceExtCommandListener) listenerRecord.getListener())
                        .onStartRecognition(result);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG,
                        "onStartRecognitionNotify " + listenerRecord.getSwipSetName() + " IPC fail");
            }
        }
    }

    private void onPauseRecognitionNotifyLocked(ListenerRecord listenerRecord, int result) {
        if (listenerRecord != null) {
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                listenerRecord.switchSwipState(CommonManager.STATE_FEATURE_SWIP,
                        SUBSTATE_RECOGNITION_PAUSED);
            }
            try {
                ((IVoiceExtCommandListener) listenerRecord.getListener())
                        .onPauseRecognition(result);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG,
                        "onPauseRecognitionNotify " + listenerRecord.getSwipSetName() + " IPC fail");
            }
        }
    }

    private void onResumeRecognitionNotifyLocked(ListenerRecord listenerRecord, int result) {
        if (listenerRecord != null) {
            if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
                listenerRecord.switchSwipState(CommonManager.STATE_FEATURE_SWIP,
                        SUBSTATE_RECOGNITION_STARTED);
            }
            try {
                ((IVoiceExtCommandListener) listenerRecord.getListener())
                        .onResumeRecognition(result);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG,
                        "onResumeRecognitionNotify " + listenerRecord.getSwipSetName()
                                + " IPC fail");
            }
        }
    }

    private void onStopRecognitionNotifyLocked(ListenerRecord listenerRecord, int result) {
        if (listenerRecord != null) {
            if (result != VoiceCommonState.RET_COMMON_FAILURE) {
                // Switch the swip process to null , other application can
                // access swip now
                mCommonMgr.releaseSwipOccupiedProcessLocked();
            }
            try {
                ((IVoiceExtCommandListener) listenerRecord.getListener()).onStopRecognition(result);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG,
                        "onStopRecognitionNotify " + listenerRecord.getSwipSetName() + " IPC fail");
            }
        }
    }

    private void onCommandRecognizedLocked(
            ListenerRecord listenerRecord, int result, int commandId, String[] commands) {
        if (listenerRecord != null) {
            try {
                String cmdString = (commands == null ? null : commands[0]);
                ((IVoiceExtCommandListener) listenerRecord.getListener()).onCommandRecognized(
                        result, commandId, cmdString);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG,
                        "onCommandRecognizedLocked " + listenerRecord.getSwipSetName()
                                + " IPC fail");
            }
        }
    }

    private void onSwipErrorNotifyLocked(ListenerRecord listenerRecord, int error) {
        if (listenerRecord != null) {
            try {
                ((IVoiceExtCommandListener) listenerRecord.getListener()).onError(error);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                Log.e(CommonManager.TAG, "onSwipErrorNotify " + listenerRecord.getSwipSetName()
                        + " IPC fail");
            }
        }
    }
}
