package com.mediatek.mediatekdm.scomo;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

public class ScomoComponent implements IDmComponent {
    class ScomoBinder extends DmService.DmBinder {
        public ScomoBinder() {
            super(mService);
        }

        public ScomoManager getManager() {
            return mManager;
        }
    }

    static final String BIND_SCOMO = "com.mediatek.mediatekdm.BIND_SCOMO";
    static final String NAME = "SCOMO";
    static final String SCOMO_FILE_NAME = "scomo.zip";
    static final String SCOMO_RESUME_FILE_NAME = "scomoresume.dat";
    static final String SCOMO_SCAN_PACKAGE = "com.mediatek.mediatekdm.SCOMO_SCAN_PKG";

    private ScomoBinder mBinder = null;
    private ScomoManager mManager = null;
    private DmService mService = null;
    public static final String ROOT_URI = "./SCOMO";

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        if (initiator != null && mManager.isScomoInitiator(initiator.getId())) {
            return true;
        } else {
            return (operation != null && (operation.getProperty(KEY.TYPE).contains("SCOMO") || operation
                    .getBooleanProperty(KEY.SCOMO_TAG, false)));
        }
    }

    @Override
    public void attach(DmService service) {
        Log.d(TAG.SCOMO, "Attach to service " + service);
        mService = service;
        mManager = new ScomoManager(service);
        mBinder = new ScomoBinder();
    }

    @Override
    public void configureDmTree(MdmTree tree) {
    }

    @Override
    public void detach(DmService service) {
        mManager.destroy();
        mManager = null;
        mService = null;
        Log.d(TAG.SCOMO, "Detach from service " + service);
    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        String action = intent.getAction();
        if (action.equals(ScomoComponent.SCOMO_SCAN_PACKAGE)) {
            Log.d(TAG.SERVICE, "Scan package information for SCOMO");
            mManager.scomoScanPackage();
            return DispatchResult.ACCEPT;
        } else {
            return DispatchResult.IGNORE;
        }
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
        if (operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
            mManager.updateDownloadProgress(current, total);
        }
    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        switch (action) {
            case NEW:
                if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_SCOMO)) {
                    mManager.reportResult(operation);
                    return DispatchResult.ACCEPT;
                } else {
                    return DispatchResult.IGNORE;
                }
            case RECOVER:
                if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_SCOMO)) {
                    // We deploy the same logic for both trigger and recovery
                    mManager.reportResult(operation);
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                        && operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
                    mManager.recoverDlPkg();
                    return DispatchResult.ACCEPT;
                } else {
                    return DispatchResult.IGNORE;
                }
            case RETRY:
                if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_REPORT_SCOMO)) {
                    mManager.reportResult(operation);
                    return DispatchResult.ACCEPT;
                } else if (operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL)
                        && operation.getBooleanProperty(KEY.SCOMO_TAG, false)) {
                    mManager.recoverDlPkg();
                    return DispatchResult.ACCEPT;
                } else {
                    return DispatchResult.IGNORE;
                }
            default:
                return DispatchResult.IGNORE;
        }
    }

    @Override
    public SessionHandler dispatchSessionStateChange(SessionType type, SessionState state,
            int lastError, SessionInitiator initiator, DmOperation operation) {
        if (acceptOperation(initiator, operation) || mManager.queryActions() != 0) {
            Log.d(TAG.SCOMO, "SCOMO session");
            operation.setProperty(KEY.SCOMO_TAG, true);
            operation.setProperty(KEY.ACTION_MASK, mManager.queryActions());
            return mManager;
        } else {
            return null;
        }
    }

    @Override
    public boolean forceSilentMode() {
        return false;
    }

    @Override
    public IBinder getBinder(Intent intent) {
        final String action = intent.getAction();
        if (action != null && action.equals(BIND_SCOMO)) {
            return mBinder;
        } else {
            return null;
        }
    }

    @Override
    public String getDlPackageFilename() {
        return SCOMO_FILE_NAME;
    }

    @Override
    public String getDlResumeFilename() {
        return SCOMO_RESUME_FILE_NAME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void kickoff(Context context) {
    }

    ScomoManager getScomoManager() {
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
