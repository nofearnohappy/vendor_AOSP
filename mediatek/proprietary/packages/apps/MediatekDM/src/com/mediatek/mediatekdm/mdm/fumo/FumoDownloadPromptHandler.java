package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.DownloadDescriptor;
import com.mediatek.mediatekdm.mdm.DownloadPromptHandler;
import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.fumo.MdmFumo.Registry;

/**
 * FUMO implementation of DownloadPromptHandler.
 */
class FumoDownloadPromptHandler implements DownloadPromptHandler {

    private MdmFumo mFumo;

    public FumoDownloadPromptHandler(MdmFumo fumo) {
        mFumo = fumo;
    }

    public void notify(DownloadDescriptor dd, SessionInitiator initiator) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[FUMO] +FumoDownloadPromptHandler.notify()");
        if (initiator.getId().startsWith(MdmFumo.SESSION_INITIATOR_PREFIX)) {
            if (mFumo.getHandler().confirmDownload(dd, mFumo)) {
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] Download proceeded");
                mFumo.getEngine().notifyDLSessionProceed();
                mFumo.setState(FumoState.DOWNLOAD_PROGRESSING);
                mFumo.getEngine()
                        .getPLRegistry()
                        .setStringValue(Registry.DOWNLOAD_PACKAGE_PATH,
                                dd.getField(DownloadDescriptor.Field.NAME));
            } else {
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] Download postponed");
            }
        } else {
            throw new MdmException(MdmError.BAD_INPUT);
        }
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[FUMO] -FumoDownloadPromptHandler.notify()");
    }

}
