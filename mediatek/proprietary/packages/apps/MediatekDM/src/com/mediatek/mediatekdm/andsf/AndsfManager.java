package com.mediatek.mediatekdm.andsf;

import android.content.Intent;
import android.os.Message;

import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;

public class AndsfManager extends SessionHandler {
    private ANDSFComponent mComponent;

    public AndsfManager(DmService service, ANDSFComponent component) {
        super(service);
        mComponent = component;
    }

    @Override
    protected void dmComplete() {
        DmOperation operation = mOperationManager.current();
        super.dmComplete();
        mComponent.writeBackAndsfMO();
        sendResult(ANDSFComponent.RESULT_SUCCESS, operation.getProperty(ANDSFComponent.INTENT_KEY_REQUEST_ID));
    }

    @Override
    protected void dmAbort(int lastError) {
        DmOperation operation = mOperationManager.current();
        if (lastError == MdmError.COMMS_SOCKET_ERROR.val && operation.getRetry() > 0) {
            mOperationManager.notifyCurrentAborted();
            Message msg = mService.getHandler().obtainMessage(IServiceMessage.MSG_OPERATION_TIME_OUT, operation);
            mService.getHandler().sendMessageDelayed(msg, operation.timeout);
        } else {
            mOperationManager.finishCurrent();
            sendResult(ANDSFComponent.RESULT_FAIL, operation.getProperty(ANDSFComponent.INTENT_KEY_REQUEST_ID));
        }
    }

    private void sendResult(String result, String id) {
        Intent intent = new Intent(ANDSFComponent.INTENT_ACTION_RESULT);
        intent.putExtra(ANDSFComponent.INTENT_KEY_RESULT, result);
        intent.putExtra(ANDSFComponent.INTENT_KEY_REQUEST_ID, id);
        mService.sendBroadcast(intent);
    }
}
