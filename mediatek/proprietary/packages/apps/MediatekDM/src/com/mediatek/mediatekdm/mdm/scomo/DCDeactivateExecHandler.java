package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.InventoryDeployedStatus;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDc.DeployStatus;

public class DCDeactivateExecHandler extends ScomoExecHandler implements NodeExecuteHandler {
    private MdmScomoDc mDc;

    public DCDeactivateExecHandler(MdmScomo scomo, MdmScomoDc dc) {
        super(scomo);
        mDc = dc;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DCDeactivateExecHandler.execute()");
        /* data is ignored */
        MdmScomoDcHandler handler = mDc.getHandler();
        // Set action bits.
        mScomo.getEngine().setSessionAction(MdmScomo.SESSION_ACTION_KEY,
                ScomoAction.DEACTIVATE_EXECUTED);

        if (handler != null) {
            if (handler.confirmDeactivate(mDc)) {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Deactivate proceeded");
                mDc.setDeployStatus(DeployStatus.DEACTIVATION_STARTED);
                mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                        InventoryDeployedStatus.DEACTIVATE_PROGRESSING.val);
                ScomoOperationResult result = handler.executeDeactivate(mDc);
                if (result.mAsync) {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Async deactivation");
                    saveExecInfo(correlator,
                            MdmTree.makeUri(mDc.getUri(), Uri.OPERATIONS, Uri.DEACTIVATE));
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DCDeactivateExecHandler.execute()");
                    return 0;
                } else {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Sync deactivation");
                    mDc.setDeployStatus(DeployStatus.DEACTIVATION_DONE);
                    if (result.mResult.val == MdmScomoResult.SUCCESSFUL) {
                        mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                                InventoryDeployedStatus.IDLE.val);
                    } else {
                        mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                                InventoryDeployedStatus.DEACTIVATE_FAILED.val);
                    }
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DCDeactivateExecHandler.execute()");
                    return result.mResult.val;
                }
            } else {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Deactivate postponed");
                mDc.setDeployStatus(DeployStatus.DEACTIVATION_START_POSTPONED);
                saveExecInfo(correlator,
                        MdmTree.makeUri(mDc.getUri(), Uri.OPERATIONS, Uri.DEACTIVATE));
                mScomo.logMsg(MdmLogLevel.DEBUG, "-DCDeactivateExecHandler.execute()");
                return 0;
            }
        } else {
            mScomo.logMsg(MdmLogLevel.WARNING, "No DC handler");
            mScomo.logMsg(MdmLogLevel.DEBUG, "-DCDeactivateExecHandler.execute()");
            return MdmScomoResult.NOT_IMPLEMENTED;
        }
    }

}
