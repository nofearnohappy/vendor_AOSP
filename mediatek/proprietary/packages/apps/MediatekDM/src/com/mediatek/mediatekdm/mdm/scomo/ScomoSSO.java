package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.DownloadStatus;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Initiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

public class ScomoSSO implements SessionStateObserver {

    private MdmScomo mScomo;

    public ScomoSSO(MdmScomo scomo) {
        mScomo = scomo;
    }

    public void notify(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator) {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+ScomoSSO.notify()");
        // If the first DM session failed, than we will not trigger the successive DL session. Only
        // DL session
        // initiator will be recorded in mScomo.mPendingSession set.
        if (type == SessionType.DM && state == SessionState.ABORTED) {
            for (SessionInitiator si : mScomo.mPendingSession) {
                mScomo.logMsg(MdmLogLevel.DEBUG, "Remove scomo pending session " + si.getId());
                mScomo.getEngine().removePendingTrigger(si);
            }
        }
        if (initiator.getId().startsWith(Initiator.PREFIX)) {
            if (state == SessionState.STARTED) {
                mScomo.logMsg(MdmLogLevel.DEBUG,
                        "Pending session started, remove it from pending list.");
                mScomo.mPendingSession.remove(initiator);
            }
            switch (type) {
                case DM:
                    break;
                case DL:
                    String[] initiatorComponents = initiator.getId().split(
                            Initiator.SEPERATOR_REGEXP);
                    String dpName = initiatorComponents[initiatorComponents.length - 2];
                    String command = initiatorComponents[initiatorComponents.length - 1];
                    MdmScomoDp dp = mScomo.searchDp(dpName);
                    try {
                        if (state == SessionState.STARTED) {
                            mScomo.logMsg(MdmLogLevel.DEBUG, "DL session for DP " + dpName
                                    + " started");
                            mScomo.getTree().replaceIntValue(
                                    MdmTree.makeUri(dp.getDownloadUri(), Uri.STATUS),
                                    DownloadStatus.DOWNLOAD_PROGRESSING.val);
                            mScomo.getPLRegistry().setStringValue(
                                    MdmScomo.Registry.DL_LAST_SESSION_ID, initiator.getId());
                        } else if (state == SessionState.COMPLETE) {
                            mScomo.logMsg(MdmLogLevel.DEBUG, "DL session for DP " + dpName
                                    + " completed");
                            mScomo.getTree().replaceIntValue(
                                    MdmTree.makeUri(dp.getDownloadUri(), Uri.STATUS),
                                    DownloadStatus.DOWNLOAD_COMPLETE.val);
                            if (command.equals(Uri.DOWNLOAD)) {
                                mScomo.logMsg(MdmLogLevel.DEBUG, "No successive install action");
                                // TODO update Delivered tree
                            } else if (command.equals(Uri.DOWNLOADINSTALL)
                                    || command.equals(Uri.DOWNLOADINSTALLINACTIVE)) {
                                MdmScomoDpHandler handler = dp.getHandler();
                                if (handler != null) {
                                    if (handler.confirmInstall(dp)) {
                                        mScomo.getTree().replaceIntValue(
                                                MdmTree.makeUri(dp.getDownloadUri(), Uri.STATUS),
                                                DownloadStatus.INSTALL_PROGRESSING.val);
                                        ScomoOperationResult result = handler.executeInstall(dp,
                                                dp.getDeliveryPkgPath(),
                                                command.equals(Uri.DOWNLOADINSTALL));
                                        if (!result.mAsync) {
                                            if (result.mResult.val != MdmScomoResult.SUCCESSFUL) {
                                                mScomo.getTree()
                                                        .replaceIntValue(
                                                                MdmTree.makeUri(
                                                                        dp.getDownloadUri(),
                                                                        Uri.STATUS),
                                                                DownloadStatus.INSTALL_FAILED_WITH_DATA.val);
                                            } else {
                                                mScomo.getTree().replaceIntValue(
                                                        MdmTree.makeUri(dp.getDownloadUri(),
                                                                Uri.STATUS),
                                                        DownloadStatus.IDLE.val);
                                                mScomo.logMsg(MdmLogLevel.DEBUG,
                                                        "We do not remove Download/<x> sub-tree");
                                            }
                                            dp.triggerReportSession(result.mResult);
                                        } else {
                                            mScomo.logMsg(MdmLogLevel.DEBUG,
                                                    "Install asynchronously");
                                        }
                                    } else {
                                        dp.setInstallActive(command.equals(Uri.DOWNLOADINSTALL));
                                        mScomo.logMsg(MdmLogLevel.DEBUG, "Install for DP " + dpName
                                                + " postponed");
                                    }
                                } else {
                                    dp.triggerReportSession(new MdmScomoResult(
                                            MdmScomoResult.NOT_IMPLEMENTED));
                                }
                            }
                        } else if (state == SessionState.ABORTED) {
                            mScomo.logMsg(MdmLogLevel.DEBUG, "DL session for DP " + dpName
                                    + " aborted with error " + lastError);
                            mScomo.getTree().replaceIntValue(
                                    MdmTree.makeUri(dp.getDownloadUri(), Uri.STATUS),
                                    DownloadStatus.DOWNLOAD_FAILED.val);
                        }
                    } catch (MdmException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    mScomo.logMsg(MdmLogLevel.WARNING,
                            "Unsupported session type " + type.toString());
                    break;
            }

        }
        mScomo.logMsg(MdmLogLevel.DEBUG, "-ScomoSSO.notify()");
    }
}
