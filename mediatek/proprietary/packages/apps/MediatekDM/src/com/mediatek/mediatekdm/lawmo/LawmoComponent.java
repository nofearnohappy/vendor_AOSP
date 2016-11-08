package com.mediatek.mediatekdm.lawmo;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.KickoffActor;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

public class LawmoComponent implements IDmComponent {
    public static final String NAME = "LAWMO";

    private LawmoManager mManager = null;

    public static final String ROOT_URI = "./LAWMO";

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        if (initiator != null && mManager.isLawmoInitiator(initiator.getId())) {
            return true;
        } else if (operation != null
                && (operation.getProperty(KEY.TYPE).contains("LAWMO") || operation
                        .getBooleanProperty(KEY.LAWMO_TAG, false))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void attach(DmService service) {
        mManager = new LawmoManager(service);
    }

    @Override
    public void detach(DmService service) {
        mManager.destroy();
        mManager = null;
    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        return DispatchResult.IGNORE;
    }

    @Override
    public SessionHandler dispatchSessionStateChange(SessionType type, SessionState state,
            int lastError, SessionInitiator initiator, DmOperation operation) {
        if (acceptOperation(initiator, operation) || mManager.queryActions() != 0) {
            Log.d(TAG.LAWMO, "LAWMO session");
            operation.setProperty(KEY.LAWMO_TAG, true);
            operation.setProperty(KEY.ACTION_MASK, mManager.queryActions());
            return mManager;
        } else {
            return null;
        }
    }

    @Override
    public boolean forceSilentMode() {
        boolean isLocked = false;
        try {
            isLocked = PlatformManager.getInstance().isLockFlagSet();
        } catch (RemoteException e) {
            throw new Error(e);
        }
        Log.i(TAG.LAWMO, "LAWMO lock flag: " + isLocked);
        return isLocked;
    }

    @Override
    public IBinder getBinder(Intent intent) {
        // No binder.
        return null;
    }

    @Override
    public String getDlPackageFilename() {
        // nothing to return
        return null;
    }

    @Override
    public String getDlResumeFilename() {
        // nothing to return
        return null;
    }

    @Override
    public String getName() {
        return "LAWMO";
    }

    @Override
    public void kickoff(Context context) {
        KickoffActor.kickoff(new WipeRebootChecker(context));
    }

    @Override
    public void configureDmTree(MdmTree tree) {
        // TODO Auto-generated method stub

    }

    LawmoManager getLawmoManager() {
        return mManager;
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
