package com.mediatek.mediatekdm.wfhs;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

public class WiFiHotSpotComponent implements IDmComponent {

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void attach(DmService service) {
        // TODO Auto-generated method stub

    }

    @Override
    public void detach(DmService service) {
        // TODO Auto-generated method stub

    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
        // TODO Auto-generated method stub

    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SessionHandler dispatchSessionStateChange(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator, DmOperation operation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean forceSilentMode() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IBinder getBinder(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDlPackageFilename() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDlResumeFilename() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void kickoff(Context context) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureDmTree(MdmTree tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public DispatchResult validateWapPushMessage(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public boolean checkPrerequisites() {
        return true;
    }

}
