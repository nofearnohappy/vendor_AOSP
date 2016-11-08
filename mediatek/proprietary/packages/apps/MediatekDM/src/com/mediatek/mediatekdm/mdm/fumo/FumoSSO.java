package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;

class FumoSSO implements SessionStateObserver {

    private MdmFumo mFumo;

    public FumoSSO(MdmFumo fumo) {
        mFumo = fumo;
    }

    public void notify(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator) {
        MdmEngine.getLogger().logMsg(
                MdmLogLevel.DEBUG,
                "[FUMO] +FumoSSO.notify(" + type + ", " + state + ", " + lastError + ", "
                        + initiator.getId() + ")");
        // If the first DM session failed, than we will not trigger the successive DL session. Only
        // DL session
        // initiator will be recorded in mScomo.mPendingSession set.
        if (type == SessionType.DM && state == SessionState.ABORTED) {
            for (SessionInitiator si : mFumo.mPendingSession) {
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                        "Remove fumo pending session " + si.getId());
                mFumo.getEngine().removePendingTrigger(si);
            }
        }
        if (initiator.getId().startsWith(MdmFumo.SESSION_INITIATOR_PREFIX)) {
            if (state == SessionState.STARTED) {
                MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                        "Pending session started, remove it from pending list.");
                mFumo.mPendingSession.remove(initiator);
            }
            switch (type) {
                case DM:
                    break;
                case DL:
                    try {
                        if (state == SessionState.STARTED) {
                            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                                    "[FUMO] DL session started");
                            mFumo.getPLRegistry().setStringValue(
                                    MdmFumo.Registry.DL_LAST_SESSION_ID, initiator.getId());
                        } else if (state == SessionState.COMPLETE) {
                            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                                    "[FUMO] DL session completed");
                            mFumo.setState(FumoState.DOWNLOAD_COMPLETE);
                            if (mFumo.getPLRegistry().getStringValue(MdmFumo.Registry.EXEC_URI)
                                    .endsWith(MdmFumo.Uri.DOWNLOADANDUPDATE)) {
                                mFumo.setState(FumoState.UPDATE_READY_TO_UPDATE);
                                if (mFumo.getHandler().confirmUpdate(mFumo)) {
                                    mFumo.executeFwUpdate();
                                } else {
                                    MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                                            "[FUMO] Update postponed");
                                }
                            }
                        } else if (state == SessionState.ABORTED) {
                            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                                    "[FUMO] DL session aborted with error " + lastError);
                            mFumo.setState(FumoState.DOWNLOAD_FAILED);
                        }
                    } catch (MdmException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    MdmEngine.getLogger().logMsg(MdmLogLevel.WARNING,
                            "Unsupported session type " + type.toString());
                    break;
            }
        }
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] -FumoSSO.notify()");
    }

}
