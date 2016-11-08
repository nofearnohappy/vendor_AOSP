package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.DownloadDescriptor;
import com.mediatek.mediatekdm.mdm.DownloadPromptHandler;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Initiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Registry;

public class DPDownloadPromptHandler implements DownloadPromptHandler {
    private MdmScomoDp mDp;
    private MdmScomo mScomo;

    public DPDownloadPromptHandler(MdmScomo scomo, MdmScomoDp dp) {
        mDp = dp;
        mScomo = scomo;
    }

    public void notify(DownloadDescriptor dd, SessionInitiator initiator) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+DPDownloadPromptHandler.notify()");
        if (initiator.getId().startsWith(Initiator.PREFIX)) {
            if (mDp.getHandler().confirmDownload(mDp, dd)) {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Download proceeded");
                mScomo.getEngine().notifyDLSessionProceed();
                // TODO set state
                mScomo.getEngine()
                        .getPLRegistry()
                        .setStringValue(
                                Registry.makePath(Registry.ROOT, Registry.PACKAGE, mDp.getName(),
                                        Registry.PATH), dd.getField(DownloadDescriptor.Field.NAME));
            } else {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Download postponed");
            }
        } else {
            throw new MdmException(MdmError.BAD_INPUT);
        }
        mScomo.logMsg(MdmLogLevel.DEBUG, "-DPDownloadPromptHandler.notify()");
    }

}
