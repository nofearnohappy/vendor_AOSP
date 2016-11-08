package com.mediatek.mediatekdm;

import android.os.Message;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService.IServiceMessage;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.NIAMsgHandler;
import com.mediatek.mediatekdm.mdm.SessionInitiator;

import java.nio.ByteBuffer;

class NiaSessionManager extends SessionHandler implements NIAMsgHandler, SessionInitiator {
    public static final String INITIATOR = "Network Inited";
    public static final int DEFAULT_NOTIFICATION_INTERACT_TIMEOUT = 10 * 60;
    public static final int DEFAULT_NOTIFICATION_VISIBLE_TIMEOUT = 10;

    NiaSessionManager(DmService service) {
        super(service);
    }

    @Override
    protected void dmComplete() {
        super.dmComplete();
        Message msg = mService.getHandler().obtainMessage(
                IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
        mService.getHandler().sendMessage(msg);
    }

    @Override
    protected void dmAbort(int lastError) {
        super.dmAbort(lastError);
        if (lastError != MdmError.COMMS_SOCKET_ERROR.val) {
            Message msg = mService.getHandler().obtainMessage(
                    IServiceMessage.MSG_OPERATION_PROCESS_NEXT);
            mService.getHandler().sendMessage(msg);
        }
    }

    @Override
    public void notify(UIMode uiMode, short dmVersion, byte[] vendorSpecificData,
            SessionInitiator initiator) throws MdmException {
        mService.getController().proceedNiaSession();
    }

    @Override
    public String getId() {
        return INITIATOR;
    }

    /**
     * Decode NIA message and return the UI mode.
     *
     * @param msg
     * @return
     */
    public static int extractUIModeFromNIA(byte[] msg) {
        Log.d(TAG.COMMON, "+extractUIModeFromNIA()");
        int uiMode = -1;
        if (msg == null || msg.length <= 0) {
            Log.d(TAG.COMMON, "-extractUIModeFromNIA()");
            return uiMode;
        }
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        if (buffer == null) {
            Log.d(TAG.COMMON, "-extractUIModeFromNIA()");
            return uiMode;
        }
        buffer.getDouble();
        buffer.getDouble();
        buffer.get(); // skip one byte
        byte b2 = buffer.get();
        uiMode = ((b2 << 2) >>> 6) & 3;
        Log.d(TAG.COMMON, "-extractUIModeFromNIA()");
        return uiMode;
    }
}