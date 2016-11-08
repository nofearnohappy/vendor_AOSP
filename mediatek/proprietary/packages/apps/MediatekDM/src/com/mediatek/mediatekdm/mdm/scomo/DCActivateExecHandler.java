package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.InventoryDeployedStatus;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDc.DeployStatus;

public class DCActivateExecHandler extends ScomoExecHandler implements NodeExecuteHandler {
    private MdmScomoDc mDc;

    public DCActivateExecHandler(MdmScomo scomo, MdmScomoDc dc) {
        super(scomo);
        mDc = dc;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DCActivateExecHandler.execute()");
        /* data is ignored */
        MdmScomoDcHandler handler = mDc.getHandler();
        // Set action bits.
        mScomo.getEngine().setSessionAction(MdmScomo.SESSION_ACTION_KEY,
                ScomoAction.ACTIVATE_EXECUTED);

        if (handler != null) {
            if (handler.confirmActivate(mDc)) {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Activate proceeded");
                mDc.setDeployStatus(DeployStatus.ACTIVATION_STARTED);
                mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                        InventoryDeployedStatus.ACTIVATE_PROGRESSING.val);
                ScomoOperationResult result = handler.executeActivate(mDc);
                if (result.mAsync) {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Async activation");
                    saveExecInfo(correlator,
                            MdmTree.makeUri(mDc.getUri(), Uri.OPERATIONS, Uri.ACTIVATE));
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DCActivateExecHandler.execute()");
                    return 0;
                } else {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Sync activation");
                    mDc.setDeployStatus(DeployStatus.ACTIVATION_DONE);
                    if (result.mResult.val == MdmScomoResult.SUCCESSFUL) {
                        mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                                InventoryDeployedStatus.IDLE.val);
                    } else {
                        mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                                InventoryDeployedStatus.ACTIVATE_FAILED.val);
                    }
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DCActivateExecHandler.execute()");
                    return result.mResult.val;
                }
            } else {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Activate postponed");
                mDc.setDeployStatus(DeployStatus.ACTIVATION_START_POSTPONED);
                saveExecInfo(correlator,
                        MdmTree.makeUri(mDc.getUri(), Uri.OPERATIONS, Uri.ACTIVATE));
                mScomo.logMsg(MdmLogLevel.DEBUG, "-DCActivateExecHandler.execute()");
                return 0;
            }
        } else {
            mScomo.logMsg(MdmLogLevel.WARNING, "No DC handler");
            mScomo.logMsg(MdmLogLevel.DEBUG, "-DCActivateExecHandler.execute()");
            return MdmScomoResult.NOT_IMPLEMENTED;
        }
    }
}
