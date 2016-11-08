package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;

/**
 * @TODO
 */
class UpdateExecHandler implements NodeExecuteHandler {
    private MdmFumo mFumo;

    public UpdateExecHandler(MdmFumo fumo) {
        mFumo = fumo;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] +UpdateExecHandler.execute()");
        // Save correlator
        mFumo.getPLRegistry().setStringValue(MdmFumo.Registry.EXEC_CORRELATOR, correlator);
        // Save uri
        mFumo.getPLRegistry().setStringValue(MdmFumo.Registry.EXEC_URI,
                MdmTree.makeUri(mFumo.getRootUri(), MdmFumo.Uri.UPDATE));
        if (mFumo.getHandler().confirmUpdate(mFumo)) {
            mFumo.executeFwUpdate();
        } else {
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] Update postponed");
        }
        mFumo.getEngine().setSessionAction(MdmFumo.SESSION_ACTION_KEY, FumoAction.UPDATE_EXECUTED);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] -UpdateExecHandler.execute()");
        return 0;
    }

}
