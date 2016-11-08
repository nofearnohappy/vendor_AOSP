package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.InventoryDeployedStatus;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDc.DeployStatus;

public class DCRemoveExecHandler extends ScomoExecHandler implements NodeExecuteHandler {
    private MdmScomoDc mDc;

    public DCRemoveExecHandler(MdmScomo scomo, MdmScomoDc dc) {
        super(scomo);
        mDc = dc;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DCRemoveExecHandler.execute()");
        /* data is ignored */
        MdmScomoDcHandler handler = mDc.getHandler();
        // Set action bits.
        mScomo.getEngine().setSessionAction(MdmScomo.SESSION_ACTION_KEY,
                ScomoAction.DC_REMOVE_EXECUTED);

        if (handler != null) {
            if (handler.confirmRemove(mDc)) {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Remove proceeded");
                mDc.setDeployStatus(DeployStatus.REMOVAL_STARTED);
                mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                        InventoryDeployedStatus.REMOVE_PROGRESSING.val);
                ScomoOperationResult result = handler.executeRemove(mDc);
                if (result.mAsync) {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Async removal");
                    saveExecInfo(correlator,
                            MdmTree.makeUri(mDc.getUri(), Uri.OPERATIONS, Uri.REMOVE));
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DCRemoveExecHandler.execute()");
                    return 0;
                } else {
                    mScomo.logMsg(MdmLogLevel.DEBUG, "Sync removal");
                    mDc.setDeployStatus(DeployStatus.REMOVAL_DONE);
                    if (result.mResult.val == MdmScomoResult.SUCCESSFUL) {
                        mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                                InventoryDeployedStatus.IDLE.val);
                    } else {
                        mScomo.getTree().replaceIntValue(MdmTree.makeUri(mDc.getUri(), Uri.STATUS),
                                InventoryDeployedStatus.REMOVE_FAILED.val);
                    }
                    mScomo.logMsg(MdmLogLevel.DEBUG, "-DCRemoveExecHandler.execute()");
                    return result.mResult.val;
                }
            } else {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Remove postponed");
                mDc.setDeployStatus(DeployStatus.REMOVAL_START_POSTPONED);
                saveExecInfo(correlator, MdmTree.makeUri(mDc.getUri(), Uri.OPERATIONS, Uri.REMOVE));
                mScomo.logMsg(MdmLogLevel.DEBUG, "-DCRemoveExecHandler.execute()");
                return 0;
            }
        } else {
            mScomo.logMsg(MdmLogLevel.WARNING, "No DC handler");
            mScomo.logMsg(MdmLogLevel.DEBUG, "-DCRemoveExecHandler.execute()");
            return MdmScomoResult.NOT_IMPLEMENTED;
        }
    }

}
