package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Initiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

public class DLDownloadInstallExecHandler extends ScomoExecHandler implements NodeExecuteHandler {
    private MdmScomoDp mDp;

    public DLDownloadInstallExecHandler(MdmScomo scomo, MdmScomoDp dp) {
        super(scomo);
        mDp = dp;
    }

    public int execute(byte[] data, String correlator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DLDownloadInstallExecHandler.execute()");
        MdmEngine engine = mScomo.getEngine();
        saveExecInfo(correlator,
                MdmTree.makeUri(mDp.getDownloadUri(), Uri.OPERATIONS, Uri.DOWNLOADINSTALL));
        // post trigger DL session with DownloadPromptHandler
        SessionInitiator initiator = new SimpleSessionInitiator(Initiator.makePath(Initiator.DL,
                mDp.getName(), Uri.DOWNLOADINSTALL));
        mScomo.mPendingSession.add(initiator);
        engine.triggerDLSession(mDp.getUrl(), new DPDownloadPromptHandler(mScomo, mDp), initiator);
        // Set action bits.
        engine.setSessionAction(MdmScomo.SESSION_ACTION_KEY, ScomoAction.DOWNLOAD_INSTALL_EXECUTED);
        // Return 0 to inform the engine this is an asynchronous execution.
        mScomo.logMsg(MdmLogLevel.DEBUG, "-DLDownloadInstallExecHandler.execute()");
        return 0;
    }
}
