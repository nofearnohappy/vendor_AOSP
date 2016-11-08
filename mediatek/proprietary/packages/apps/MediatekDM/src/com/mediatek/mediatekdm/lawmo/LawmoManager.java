package com.mediatek.mediatekdm.lawmo;

import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoAction;
import com.mediatek.mediatekdm.mdm.lawmo.LawmoState;
import com.mediatek.mediatekdm.mdm.lawmo.MdmLawmo;

public class LawmoManager extends SessionHandler {

    private int mPendingAction = LawmoAction.NONE;
    private MdmLawmo mLawmo;

    public LawmoManager(DmService service) {
        super(service);
        mLawmo = new MdmLawmo(LawmoComponent.ROOT_URI, new DmLawmoHandler(mService));
        syncLawmoStatus();
    }

    @Override
    protected void dmComplete() {
        Log.i(TAG.LAWMO, "+dmComplete");
        super.dmComplete();
        // If we need to support other actions, revise this.
        if (mPendingAction == LawmoAction.FACTORY_RESET_EXECUTED) {
            clearPendingAction();
            // Erase SD card & Factory reset
            Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            intent.putExtra("lawmo_wipe", true);
            mService.startService(intent);
            Log.i(TAG.LAWMO, "Start Service ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET");
        }
        Log.i(TAG.LAWMO, "-dmComplete");
    }

    public void setPendingAction(int action) {
        mPendingAction = action;
    }

    public void clearPendingAction() {
        mPendingAction = LawmoAction.NONE;
    }

    public SessionHandler getSessionHandler() {
        return this;
    }

    public int queryActions() {
        return mLawmo.querySessionActions();
    }

    public boolean isLawmoInitiator(String initiator) {
        return initiator.startsWith(MdmLawmo.SESSION_INITIATOR_PREFIX);
    }

    public void destroy() {
        mLawmo.destroy();
    }

    private void syncLawmoStatus() {
        Log.i(TAG.LAWMO, "+syncLawmoStatus");

        String lawmoUri = MdmTree.makeUri(LawmoComponent.ROOT_URI, "State");
        try {
            Log.i(TAG.LAWMO, "The device lock status is "
                    + PlatformManager.getInstance().isLockFlagSet());
            if (PlatformManager.getInstance().isLockFlagSet()) {
                // Get lock value from agent
                int lockStatus = -1;
                boolean isFullyLock = (PlatformManager.getInstance().getLockType() == 1);
                if (isFullyLock) {
                    lockStatus = LawmoState.FULLY_LOCKED.val;
                } else {
                    lockStatus = LawmoState.PARTIALLY_LOCKED.val;
                }
                Log.i(TAG.LAWMO, "Lock status is " + lockStatus);

                // Get LAWMO state from tree
                MdmTree tree = new MdmTree();
                int treeLawmoStatus = tree.getIntValue(lawmoUri);
                Log.i(TAG.LAWMO, "In tree, lawmo staus is " + treeLawmoStatus);

                // Sync LAWMO status to tree
                if (lockStatus != treeLawmoStatus) {
                    tree.replaceIntValue(lawmoUri, lockStatus);
                    tree.writeToPersistentStorage();
                    Log.i(TAG.LAWMO, "Update staus in tree to " + tree.getIntValue(lawmoUri));
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG.LAWMO, "RemoteException:" + e);
            e.printStackTrace();
        } catch (MdmException e) {
            Log.e(TAG.LAWMO, "MdmException:" + e);
            e.printStackTrace();
        }
        Log.i(TAG.LAWMO, "-syncLawmoStatus");
    }
}
