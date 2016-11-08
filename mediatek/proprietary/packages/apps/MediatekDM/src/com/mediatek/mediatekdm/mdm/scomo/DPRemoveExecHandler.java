package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;

public class DPRemoveExecHandler extends ScomoExecHandler implements NodeExecuteHandler {
    private MdmScomoDp mDp;

    public DPRemoveExecHandler(MdmScomo scomo, MdmScomoDp dp) {
        super(scomo);
        mDp = dp;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DPRemoveExecHandler.execute()");
        /* data is ignored */

        // Set action bits.
        mScomo.getEngine().setSessionAction(MdmScomo.SESSION_ACTION_KEY,
                ScomoAction.DP_REMOVE_EXECUTED);

        try {
            mScomo.getTree().deleteNode(mDp.getDeliveredUri());
        } catch (MdmException e) {
            if (e.getError() != MdmError.NODE_MISSING) {
                mScomo.removeDp(mDp.getName());
                mDp.destroy();
                return MdmScomoResult.UNDEFINED_ERROR;
            }
        }
        mScomo.removeDp(mDp.getName());
        mDp.destroy();
        mScomo.logMsg(MdmLogLevel.DEBUG, "-DPRemoveExecHandler.execute()");
        return MdmScomoResult.SUCCESSFUL;
    }

}
