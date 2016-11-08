package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.InventoryDeliveredState;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.InventoryDeliveredStatus;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDp.InstallStatus;

public class DPInstallExecHandler extends ScomoExecHandler implements NodeExecuteHandler {
    private MdmScomoDp mDp;

    public DPInstallExecHandler(MdmScomo scomo, MdmScomoDp dp) {
        super(scomo);
        mDp = dp;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DPInstallExecHandler.execute()");
        /* data is ignored */
        MdmScomoDpHandler handler = mDp.getHandler();

        // Set action bits.
        mScomo.getEngine().setSessionAction(MdmScomo.SESSION_ACTION_KEY,
                ScomoAction.INSTALL_EXECUTED);

        if (handler != null) {
            if (handler.confirmInstall(mDp)) {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Install proceeded");
                mDp.setInstallStatus(InstallStatus.STARTED);
                mScomo.getTree().replaceIntValue(
                        MdmTree.makeUri(mDp.getDeliveredUri(), Uri.STATUS),
                        InventoryDeliveredStatus.INSTALL_PROGRESSING.val);
                ScomoOperationResult result = handler.executeInstall(mDp, mDp.getDeliveryPkgPath(),
                        true);
                if (result.mAsync) {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Async installation");
                    saveExecInfo(correlator,
                            MdmTree.makeUri(mDp.getDeliveredUri(), Uri.OPERATIONS, Uri.INSTALL));
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DPInstallExecHandler.execute()");
                    return 0;
                } else {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Sync installation");
                    mDp.setInstallStatus(InstallStatus.DONE);
                    if (result.mResult.val != MdmScomoResult.SUCCESSFUL) {
                        mScomo.getTree().replaceIntValue(
                                MdmTree.makeUri(mDp.getDeliveredUri(), Uri.STATUS),
                                InventoryDeliveredStatus.INSTALL_FAILED_WITH_DATA.val);
                    } else {
                        mScomo.getTree().replaceIntValue(
                                MdmTree.makeUri(mDp.getDeliveredUri(), Uri.STATUS),
                                InventoryDeliveredStatus.IDLE.val);
                        mScomo.getTree().replaceIntValue(
                                MdmTree.makeUri(mDp.getDeliveredUri(), Uri.STATE),
                                InventoryDeliveredState.INSTALLED.val);
                    }
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DPInstallExecHandler.execute()");
                    return result.mResult.val;
                }
            } else {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Install postponed");
                mDp.setInstallStatus(InstallStatus.START_POSTPONED);
                saveExecInfo(correlator,
                        MdmTree.makeUri(mDp.getDeliveredUri(), Uri.OPERATIONS, Uri.INSTALL));
                mDp.setInstallActive(true);
                mScomo.logMsg(MdmLogLevel.DEBUG, "-DPInstallExecHandler.execute()");
                return 0;
            }
        } else {
            mScomo.logMsg(MdmLogLevel.WARNING, "No DP handler");
            mScomo.logMsg(MdmLogLevel.DEBUG, "-DPInstallExecHandler.execute()");
            return MdmScomoResult.NOT_IMPLEMENTED;
        }
    }

}
