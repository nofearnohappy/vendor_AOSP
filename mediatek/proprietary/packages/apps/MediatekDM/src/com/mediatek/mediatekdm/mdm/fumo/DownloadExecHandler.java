/**
 *
 */

package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;

/**
 *
 */
class DownloadExecHandler implements NodeExecuteHandler {
    private MdmFumo mFumo;

    public DownloadExecHandler(MdmFumo fumo) {
        mFumo = fumo;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "Execute DownloadExecHandler");
        // Reset state to Idle/Start
        mFumo.setState(FumoState.IDLE);
        // Save correlator
        mFumo.getPLRegistry().setStringValue(MdmFumo.Registry.EXEC_CORRELATOR, correlator);
        // Save uri
        mFumo.getPLRegistry().setStringValue(MdmFumo.Registry.EXEC_URI,
                MdmTree.makeUri(mFumo.getRootUri(), MdmFumo.Uri.DOWNLOAD));
        // Enqueue a DL session to engine.
        MdmEngine engine = mFumo.getEngine();
        MdmTree tree = mFumo.getTree();
        SessionInitiator initiator = new SimpleSessionInitiator(MdmFumo.SESSION_INITIATOR_DL + "|"
                + "Download");
        mFumo.mPendingSession.add(initiator);
        engine.triggerDLSession(tree.getStringValue(MdmTree.makeUri(mFumo.getRootUri(),
                MdmFumo.Uri.DOWNLOAD_PKGURL)), new FumoDownloadPromptHandler(mFumo), initiator);
        // Set action bits.
        engine.setSessionAction(MdmFumo.SESSION_ACTION_KEY, FumoAction.DOWNLOAD_EXECUTED);
        // Return 0 to inform the engine this is an asynchronous execution.
        return 0;
    }
}
