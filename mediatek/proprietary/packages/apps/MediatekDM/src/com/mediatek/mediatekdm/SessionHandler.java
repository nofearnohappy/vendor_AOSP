package com.mediatek.mediatekdm;

import android.os.Message;

import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

public abstract class SessionHandler {
    protected final DmService mService;
    protected final DmOperationManager mOperationManager;

    /**
     * @param dmService
     */
    public SessionHandler(DmService service) {
        mService = service;
        mOperationManager = DmOperationManager.getInstance();
    }

    protected void dmStart() {
    }

    /**
     * The default implementation will clear DmFumoNotification and finish current operation.
     */
    protected void dmComplete() {
        mService.clearDmNotification();
        mOperationManager.finishCurrent();
    }

    protected void dmAbort(int lastError) {
        DmOperation operation = mOperationManager.current();
        if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() > 0) {
            mOperationManager.notifyCurrentAborted();
            Message msg = mService.getHandler().obtainMessage(
                    IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
            mService.getHandler().sendMessageDelayed(msg, operation.timeout);
        } else {
            mOperationManager.finishCurrent();
        }
    }

    protected void dlStart() {
        // do nothing
    }

    protected void dlComplete() {
        mOperationManager.finishCurrent();
    }

    protected void dlAbort(int lastError) {
        DmOperation operation = mOperationManager.current();
        if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() > 0) {
            mOperationManager.notifyCurrentAborted();
            Message msg = mService.getHandler().obtainMessage(
                    IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
            mService.getHandler().sendMessageDelayed(msg, operation.timeout);
        } else {
            mOperationManager.finishCurrent();
        }
    }

    /**
     * Dispatcher method.
     *
     * @param type
     * @param state
     * @param lastError
     */
    public final void onSessionStateChange(SessionType type, SessionState state, int lastError) {
        if (type == SessionType.DM) {
            if (state == SessionState.STARTED) {
                dmStart();
            } else if (state == SessionState.COMPLETE) {
                dmComplete();
            } else if (state == SessionState.ABORTED) {
                dmAbort(lastError);
            }
        } else if (type == SessionType.DL) {
            if (state == SessionState.STARTED) {
                dlStart();
            } else if (state == SessionState.ABORTED) {
                dlAbort(lastError);
            } else if (state == SessionState.COMPLETE) {
                dlComplete();
            }
        }
    }
}