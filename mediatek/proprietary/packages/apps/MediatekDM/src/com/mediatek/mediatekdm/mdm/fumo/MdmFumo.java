package com.mediatek.mediatekdm.mdm.fumo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.PLDlPkg;
import com.mediatek.mediatekdm.mdm.PLRegistry;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MdmFumo {
    /**
     * Type of client initiated session.
     */
    public static enum ClientType {
        /** FUMO session initiated by device. */
        DEVICE,
        /**
         * FUMO session initiated by user. According to OMA FUMO specification section 7.1, if the
         * alert type is user initiated, then server MAY send user interaction commands in the same
         * session to inform the user how the server will handle the firmware update request.
         */
        USER,
    }

    /**
     * Firmware update status.
     */
    public static enum FwUpdateStatus {
        /** Firmware update complete. */
        DONE,
        /** Firmware update postponed. */
        START_POSTPONED,
        /** Firmware update has started. */
        STARTED,
    }

    static final class AlertType {
        public static final String DEVICE_REQUEST = "org.openmobilealliance.dm.firmwareupdate.devicerequest";
        public static final String USER_REQUEST = "org.openmobilealliance.dm.firmwareupdate.userrequest";
        public static final String DOWNLOAD = "org.openmobilealliance.dm.firmwareupdate.download";
        public static final String UPDATE = "org.openmobilealliance.dm.firmwareupdate.update";
        public static final String DOWNLOAD_AND_UPDATE = "org.openmobilealliance.dm.firmwareupdate.downloadandupdate";
    }

    static final class Uri {
        public static final String PKGNAME = "PkgName";
        public static final String PKGVERSION = "PkgVersion";
        public static final String DOWNLOAD = "Download";
        public static final String UPDATE = "Update";
        public static final String DOWNLOADANDUPDATE = "DownloadAndUpdate";
        public static final String STATE = "State";
        public static final String DOWNLOAD_PKGURL = DOWNLOAD + "/PkgURL";
        public static final String UPDATE_PKGDATA = UPDATE + "/PkgData";
        public static final String DOWNLOADANDUPDATE_PKGURL = DOWNLOADANDUPDATE + "/PkgURL";
    }

    static final class Registry {
        public static final String PATH_SEPERATOR = "|";
        public static final String EXEC_CORRELATOR = "mdm|fumo|exec|correlator";
        public static final String EXEC_URI = "mdm|fumo|exec|source";
        public static final String EXEC_ACCOUNT = "mdm|fumo|exec|account";
        public static final String DOWNLOAD_PACKAGE_PATH = "mdm|fumo|package|path";
        public static final String DL_LAST_SESSION_ID = "mdm|fumo|dl|last_session_id";
    }

    static final String ROOT_URI_TYPE = "urn:oma:mo:oma-fumo:1.0";

    public static final String SESSION_INITIATOR_PREFIX = "MDM_FUMO";
    public static final String SESSION_INITIATOR_CI = SESSION_INITIATOR_PREFIX + "|CI";
    public static final String SESSION_INITIATOR_REPORT = SESSION_INITIATOR_PREFIX + "|REPORT";
    public static final String SESSION_INITIATOR_DL = SESSION_INITIATOR_PREFIX + "|DL";

    public static final String SESSION_ACTION_KEY = "FUMO";

    private static MdmFumo sInstance;
    private String mRootUri;
    private MdmEngine mEngine;
    private FumoHandler mHandler;
    private MdmTree mTree;
    private PLRegistry mPLRegistry;
    private PLDlPkg mPLDlPkg;
    private boolean mNeedDownloadConfirmationInResume = false;
    private boolean mIsReportLocUriRoot = true;
    Set<SessionInitiator> mPendingSession = new HashSet<SessionInitiator>();

    public MdmFumo(String fumoRootURI, FumoHandler handler) throws MdmException {
        synchronized (MdmFumo.class) {
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                    "[FUMO] Create FUMO instance with uri " + fumoRootURI);

            if (sInstance != null) {
                throw new MdmException(MdmError.INTERNAL);
            }

            if (fumoRootURI.charAt(fumoRootURI.length() - 1) == '/') {
                mRootUri = fumoRootURI.substring(0, fumoRootURI.length() - 1);
            } else {
                mRootUri = fumoRootURI;
            }
            mHandler = handler;
            mTree = new MdmTree();
            mEngine = MdmEngine.getInstance();
            mPLRegistry = mEngine.getPLRegistry();
            mPLDlPkg = mEngine.getPLDlPkg();

            // We do NOT check FUMO tree structure here. Application should ensure it's correctness.
            mTree.registerExecute(MdmTree.makeUri(mRootUri, Uri.DOWNLOAD), new DownloadExecHandler(
                    this));
            mTree.registerExecute(MdmTree.makeUri(mRootUri, Uri.UPDATE),
                    new UpdateExecHandler(this));
            mTree.registerExecute(MdmTree.makeUri(mRootUri, Uri.DOWNLOADANDUPDATE),
                    new DownloadAndUpdateExecHandler(this));
            mTree.registerNodeIoHandler(MdmTree.makeUri(mRootUri, Uri.UPDATE_PKGDATA),
                    new PkgDataIoHandler(this));

            sInstance = this;

            // Register here or FumoSSO will get an incomplete object
            mEngine.registerSessionStateObserver(new FumoSSO(this));
        }
    }

    public void destroy() {
        synchronized (MdmFumo.class) {
            // TODO clear resources
            sInstance = null;
        }
    }

    /**
     * Start a client initiated FUMO session.
     *
     * @param message
     *        FUMO message content - vendor specific data. CURRENTLY NOT SUPPORTED
     * @param clientType
     *        Type of client - could be user or device.
     * @throws MdmException
     */
    public void triggerSession(byte[] message, ClientType clientType) throws MdmException {
        triggerSession(message, clientType, null);
    }

    /**
     * Start a client-initiated FUMO session for a specific DM account.
     *
     * @param message
     *        FUMO message content - vendor specific data. CURRENTLY NOT SUPPORTED
     * @param clientType
     *        Type of client - could be user or device.
     * @param account
     *        DM account to start session for. If account is null, engine will use the value in DM
     *        tree.
     * @throws MdmException
     */
    public void triggerSession(byte[] message, ClientType clientType, String account)
            throws MdmException {
        String alertType = (clientType == ClientType.DEVICE) ? AlertType.DEVICE_REQUEST
                : AlertType.USER_REQUEST;
        if (account == null) {
            account = mEngine.getCurrentAccount();
        }
        mEngine.triggerDMSession(account, alertType, message, new SessionInitiator() {
            public String getId() {
                return SESSION_INITIATOR_CI;
            }
        });
    }

    /**
     * Resume an aborted download session.
     *
     * @throws MdmException
     *         INVALID_CALL if FUMO is not is in a middle of a download session.
     */
    public void resumeDLSession() throws MdmException {
        FumoState state = getState();
        if (state != FumoState.DOWNLOAD_FAILED) {
            throw new MdmException(MdmError.INVALID_CALL);
        }
        String sid = getPLRegistry().getStringValue(Registry.DL_LAST_SESSION_ID);
        if (sid == null) {
            MdmEngine.getLogger().logMsg(MdmLogLevel.ERROR, "No last DL session id found");
            throw new MdmException(MdmError.INTERNAL);
        }
        mEngine.triggerDLSession(null, new FumoDownloadPromptHandler(this),
                new SimpleSessionInitiator(sid));
    }

    /**
     * Get FUMO instance package name.
     *
     * @return value of /PkgName node in the DM tree.
     * @throws MdmException
     */
    public String getPkgName() throws MdmException {
        return mTree.getStringValue(MdmTree.makeUri(mRootUri, Uri.PKGNAME));
    }

    /**
     * Get FUMO instance package version.
     *
     * @return value of /PkgVersion node in the DM tree.
     * @throws MdmException
     */
    public String getPkgVersion() throws MdmException {
        return mTree.getStringValue(MdmTree.makeUri(mRootUri, Uri.PKGVERSION));
    }

    /**
     * Get FUMO instance state.
     *
     * @return value of /State node in the DM tree.
     * @throws MdmException
     */
    public FumoState getState() throws MdmException {
        int state = mTree.getIntValue(MdmTree.makeUri(mRootUri, Uri.STATE));
        return FumoState.fromInt(state);
    }

    public void setState(FumoState s) throws MdmException {
        mTree.replaceIntValue(MdmTree.makeUri(mRootUri, Uri.STATE), s.val);
    }

    /**
     * Get FUMO instance update package path.
     *
     * @return path to the downloaded package path.
     * @throws MdmException
     */
    public String getUpdatePkgPath() throws MdmException {
        return mPLDlPkg.getFilename(mEngine.getPLRegistry().getStringValue(
                Registry.DOWNLOAD_PACKAGE_PATH));
    }

    /**
     * Execute firmware update. Called by the client to execute the update package that is available
     * on the device. (Asynchronous firmware update)
     *
     * @throws MdmException
     */
    public void executeFwUpdate() throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] +MdmFumo.executeFwUpdate()");
        setState(FumoState.UPDATE_PROGRESSING);
        mHandler.executeUpdate(getUpdatePkgPath(), this);
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[FUMO] -MdmFumo.executeFwUpdate()");
    }

    /**
     * Trigger report session (generic alert). Should be called by the client application after the
     * update agent has executed the firmware update or upon a failure in the steps of executing a
     * FUMO session (e.g. upon download failure, user abort, etc.)
     *
     * @param resultCode
     *        Result code to be sent to the server. Should be one of ResultCode values or in
     *        vendor-specific range: 250-299: Successful - Vendor Specified. 450-499: Client Error -
     *        Vendor Specified. 550-599: DL Server Error - Vendor Specified.
     * @throws MdmException
     */
    public void triggerReportSession(MdmFumoUpdateResult.ResultCode resultCode) throws MdmException {
        String uri = mEngine.getPLRegistry().getStringValue(Registry.EXEC_URI);
        String account = mEngine.getPLRegistry().getStringValue(Registry.EXEC_ACCOUNT);
        String correlator = mEngine.getPLRegistry().getStringValue(Registry.EXEC_CORRELATOR);
        String genericAlertType = null;
        if (uri.endsWith(Uri.DOWNLOAD)) {
            genericAlertType = AlertType.DOWNLOAD;
        } else if (uri.endsWith(Uri.UPDATE)) {
            genericAlertType = AlertType.UPDATE;
        } else if (uri.endsWith(Uri.DOWNLOADANDUPDATE)) {
            genericAlertType = AlertType.DOWNLOAD_AND_UPDATE;
        } else {
            throw new MdmException(MdmError.INTERNAL,
                    "Cannot find an approperiate alert type for FUMO");
        }
        mEngine.triggerReportSession(uri, resultCode.val, account, genericAlertType, correlator,
                new SimpleSessionInitiator(SESSION_INITIATOR_REPORT));
    }

    /**
     * Generate a map which contains the parameters needed by report session trigger. Client can use
     * this information to invoke MdmEngine.triggerReportSession() to report the result to DM server
     * directly. Keys: "report_correlator" "report_format" "report_type" "report_source"
     * "report_data" "report_account" "report_initiator"
     *
     * @return The map object contains the object.
     * @throws MdmException
     */
    public Map<String, String> generateReportInformation(MdmFumoUpdateResult.ResultCode resultCode,
            boolean clearState) throws MdmException {
        Map<String, String> map = new HashMap<String, String>();
        String uri = mEngine.getPLRegistry().getStringValue(Registry.EXEC_URI);
        String account = mEngine.getPLRegistry().getStringValue(Registry.EXEC_ACCOUNT);
        String correlator = mEngine.getPLRegistry().getStringValue(Registry.EXEC_CORRELATOR);
        String genericAlertType = null;
        if (uri.endsWith(Uri.DOWNLOAD)) {
            genericAlertType = AlertType.DOWNLOAD;
        } else if (uri.endsWith(Uri.UPDATE)) {
            genericAlertType = AlertType.UPDATE;
        } else if (uri.endsWith(Uri.DOWNLOADANDUPDATE)) {
            genericAlertType = AlertType.DOWNLOAD_AND_UPDATE;
        } else {
            throw new MdmException(MdmError.INTERNAL,
                    "Cannot find an approperiate alert type for FUMO");
        }
        map.put("report_correlator", correlator);
        map.put("report_format", "int");
        map.put("report_type", genericAlertType);
        map.put("report_source", uri);
        map.put("report_data", Integer.toString(resultCode.val));
        map.put("report_account", account);
        map.put("report_initiator", SESSION_INITIATOR_REPORT);
        if (clearState) {
            MdmEngine.getLogger().logMsg(MdmLogLevel.ERROR, "Clear FUMO report state.");
        }
        return map;
    }

    /**
     * Get status of update process. Called by the client to find out if the update package is
     * available on the device and is ready to be updated.
     *
     * @return DownloadStatus of update process.
     * @throws MdmException
     */
    public FwUpdateStatus getUpdateStatus() throws MdmException {
        // TODO
        return null;
    }

    /**
     * Query which actions that are relevant for this FUMO instance were performed last session.
     *
     * @return Bit flags describing the session actions.
     */
    public int querySessionActions() {
        return mEngine.getSessionActions(SESSION_ACTION_KEY) & FumoAction.ALL;
    }

    /**
     * Set a flag indicating whether the operation path is included in the LocUri. Default is TRUE.
     *
     * @param isReportLocUriRoot
     *        a flag indicating whether the operation path is included in the LocUri.TRUE if it is
     *        included, FALSE otherwise
     * @throws MdmException
     */
    public void setIsReportLocUriRoot(boolean isReportLocUriRoot) throws MdmException {
        mIsReportLocUriRoot = isReportLocUriRoot;
    }

    /**
     * Get a flag indicating whether the operation path is included in the LocUri.
     *
     * @return TRUE if operation path is included in the LocUri, FALSE otherwise.
     * @throws MdmException
     */
    public boolean getIsReportLocUriRoot() throws MdmException {
        return mIsReportLocUriRoot;
    }

    /**
     * Set a flag indicating whether confirm download callback should be called * in a resumed FUMO
     * DL session. Default is FALSE.
     *
     * @param isConfirmDownloadCalledInResume
     *        a flag indicating whether the confirm download callback should be called. in a FUMO DL
     *        resumed session.TRUE if it should be called, FALSE otherwise.
     * @throws MdmException
     */
    public void setIsConfirmDownloadCalledInResume(boolean isConfirmDownloadCalledInResume)
            throws MdmException {
        mNeedDownloadConfirmationInResume = isConfirmDownloadCalledInResume;
    }

    /**
     * Get a flag indicating whether the confirm download callback should be called in a resumed
     * FUMO DL session.
     *
     * @return TRUE if confirm download callback should be called in a FUMO DL resumed session,
     *         FALSE otherwise.
     * @throws MdmException
     */
    public boolean getIsConfirmDownloadCalledInResume() throws MdmException {
        return mNeedDownloadConfirmationInResume;
    }

    String getRootUri() {
        return mRootUri;
    }

    MdmEngine getEngine() {
        return mEngine;
    }

    MdmTree getTree() {
        return mTree;
    }

    PLRegistry getPLRegistry() {
        return mPLRegistry;
    }

    FumoHandler getHandler() {
        return mHandler;
    }

}
