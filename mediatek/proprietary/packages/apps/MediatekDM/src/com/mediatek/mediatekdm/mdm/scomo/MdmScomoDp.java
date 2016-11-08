package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.PLRegistry;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Initiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Registry;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

import java.util.HashMap;
import java.util.Map;

/**
 * Delivery Package Management Object.
 */
public class MdmScomoDp {
    /**
     * Installation status.
     */
    public static enum InstallStatus {
        INVALID, STARTED, START_POSTPONED, DONE,
    }

    private final String mName;
    private MdmScomoDpHandler mHandler;
    private final MdmScomo mScomo;
    private boolean mDelivered;
    private boolean mInstallActive = true;
    private InstallStatus mInstallStatus;
    private boolean mAlternativeDownload;

    /**
     * Create a SCOMO DP instance.
     *
     * @param dpName
     *        SCOMO DP name which is also the node name under Delivered.
     * @param handler
     *        SCOMO DP handler.
     * @param scomo
     *        SCOMO instance.
     */
    protected MdmScomoDp(String dpName, MdmScomoDpHandler handler, MdmScomo scomo) {
        this(dpName, handler, scomo, false);
    }

    protected MdmScomoDp(String dpName, MdmScomoDpHandler handler, MdmScomo scomo, boolean delivered) {
        mName = dpName;
        mHandler = handler;
        mScomo = scomo;
        mDelivered = delivered;
        mInstallStatus = InstallStatus.INVALID;
        mAlternativeDownload = true;
    }

    /**
     * Returns the dp name that was passed to the constructor.
     *
     * @return DP name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Set the MdmScomoDpHandler to be used by this instance.
     *
     * @param handler
     *        MdmScomoDpHandler instance.
     */
    public void setHandler(MdmScomoDpHandler handler) {
        mHandler = handler;
    }

    /**
     * Get the MdmScomoDpHandler which is used by this instance.
     *
     * @return MdmScomoDpHandler instance.
     */
    public MdmScomoDpHandler getHandler() {
        return mHandler;
    }

    /**
     * Destroy a SCOMO DP Instance. You must call this method to allow graceful cleanup of this
     * SCOMO DP instance.
     */
    public void destroy() {
        mScomo.removeDp(mName);
    }

    /**
     * Execute installation. Called by the client to execute the installation of a a delivery
     * package that is available on the device. (Asynchronous installation)
     *
     * @throws MdmException
     */
    public void executeInstall() throws MdmException {
        executeInstall(mInstallActive);
    }

    void executeInstall(boolean active) throws MdmException {
        if (mHandler != null) {
            /* We ignore the result according to the document. */
            ScomoOperationResult result = mHandler
                    .executeInstall(this, mScomo.getRootUri(), active);
            mScomo.logMsg(MdmLogLevel.INFO, "executeInstall result " + result + " is ignored");
        }
    }

    /**
     * Get SCOMO DP instance package id.
     *
     * @return value of /PgkID node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree.
     */
    public String getId() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()), Uri.PKGID));
    }

    /**
     * Get SCOMO DP instance package url.
     *
     * @return value of /PkgURL node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree .
     */
    public String getUrl() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()), Uri.PKGURL));
    }

    /**
     * Get SCOMO DP instance package name.
     *
     * @return value of /Name node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree.
     */
    public String getPkgName() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()), Uri.NAME));
    }

    /**
     * Get SCOMO DP instance package description.
     *
     * @return value of /Description node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree .
     */
    public String getDescription() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()),
                        Uri.DESCRIPTION));
    }

    /**
     * Get SCOMO DP instance package environment type.
     *
     * @return value of /EnvType node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree.
     */
    public String getEnvType() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()), Uri.ENVTYPE));
    }

    /**
     * Get SCOMO DP instance package type.
     *
     * @return value of /PkgType node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree.
     */
    public String getType() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()), Uri.PKGTYPE));
    }

    /**
     * Get SCOMO DP instance package install parameters.
     *
     * @return value of /InstallParams node in the DM tree.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, TREE_ACCESS_DENIED if access denied
     *         NODE_MISSING if PkgName node is missing from the DM tree.
     */
    public String getInstallParams() throws MdmException {
        return mScomo.getTree().getStringValue(
                MdmTree.makeUri((mDelivered ? getDeliveredUri() : getDownloadUri()),
                        Uri.INSTALLPARAMS));
    }

    /**
     * Get status of installation process.
     *
     * @return Current status of installation. IDLE if no installation invoked.
     * @throws MdmException
     */
    public InstallStatus getInstallStatus() throws MdmException {
        return mInstallStatus;
    }

    void setInstallStatus(InstallStatus status) {
        mInstallStatus = status;
    }

    /**
     * Get SCOMO DP instance delivery package path.
     *
     * @return path to the downloaded package path.
     * @throws MdmException
     */
    public String getDeliveryPkgPath() throws MdmException {
        MdmEngine engine = mScomo.getEngine();
        if (mAlternativeDownload) {
            return mScomo.getPLDlPkg().getFilename(
                    engine.getPLRegistry()
                            .getStringValue(
                                    Registry.makePath(Registry.ROOT, Registry.PACKAGE, mName,
                                            Registry.PATH)));
        } else {
            return engine.getPLDlPkg().getFilename(getName());
        }
    }

    /**
     * Trigger report session (generic alert). Should be called by the client application after the
     * installer has executed the installation or upon a failure in the steps of executing a SCOMO
     * DP session (e.g. upon download failure, user abort, etc.)
     *
     * @param resultCode
     *        Result code to be sent to the server. Should be one of VdmScomoResult values or in
     *        vendor-specific range: 1250-1299: Successful - Vendor Specified. 1450-1499: Client
     *        Error - Vendor Specified. 1550-1599: DL Server Error - Vendor Specified.
     * @throws MdmException
     */
    public void triggerReportSession(MdmScomoResult resultCode) throws MdmException {
        String data = "<ResultCode>" + resultCode.val + "</ResultCode><Identifier>" + getId()
                + "</Identifier>";
        MdmEngine engine = mScomo.getEngine();
        PLRegistry registry = engine.getPLRegistry();
        String account = engine.getCurrentAccount();
        String correlator = null;
        String correlatorsPath = Registry.makePath(Registry.ROOT, "exec", account, "correlators");
        String correlators = registry.getStringValue(correlatorsPath);
        // NOTE We only use the first correlator as this interface can only report one result. the
        // correlator information are
        // processed in an FIFO manner.
        if (correlators != null && correlators.length() != 0) {
            String[] correlatorList = correlators.split(Registry.SEPERATOR_REGEXP);
            correlator = correlatorList[0];
            if (correlatorList.length > 1) {
                registry.setStringValue(correlatorsPath,
                        correlators.substring(correlator.length() + 1));
            } else {
                registry.setStringValue(correlatorsPath, "");
            }
        } else {
            throw new MdmException(MdmError.INTERNAL);
        }
        String source = registry.getStringValue(Registry.makePath(Registry.ROOT, "exec", account,
                correlator, "source"));
        if (source == null || source.length() == 0) {
            throw new MdmException(MdmError.INTERNAL);
        }
        mScomo.getEngine().triggerReportSession(correlator, "text/plain", mScomo.getAlertType(),
                "informational", source, getDeliveredUri(), data, account,
                new SimpleSessionInitiator(Initiator.REPORT));
        engine.getPLRegistry().deleteKeysByPrefix(
                Registry.makePath(Registry.ROOT, "exec", account, correlator));
    }

    /**
     * Generate a map which contains the parameters needed by report session trigger. Client can use
     * this information to invoke MdmEngine.triggerReportSession() to report the result to DM server
     * directly. Keys: "report_correlator" "report_format" "report_type" "report_mark"
     * "report_source" "report_target" "report_data" "report_account" "report_initiator"
     *
     * @param resultCode
     * @param clearState
     * @return The map object contains the object.
     * @throws MdmException
     */
    public Map<String, String> generateReportInformation(MdmScomoResult resultCode,
            boolean clearState) throws MdmException {
        Map<String, String> map = new HashMap<String, String>();
        String data = "<ResultCode>" + resultCode.val + "</ResultCode><Identifier>" + getId()
                + "</Identifier>";
        MdmEngine engine = mScomo.getEngine();
        PLRegistry registry = engine.getPLRegistry();
        String account = engine.getCurrentAccount();
        String correlator = null;
        String correlatorsPath = Registry.makePath(Registry.ROOT, "exec", account, "correlators");
        String correlators = registry.getStringValue(correlatorsPath);
        // NOTE We only use the first correlator as this interface can only report one result. the
        // correlator information are
        // processed in an FIFO manner.
        if (correlators != null && correlators.length() != 0) {
            String[] correlatorList = correlators.split(Registry.SEPERATOR_REGEXP);
            correlator = correlatorList[0];
            if (correlatorList.length > 1) {
                registry.setStringValue(correlatorsPath,
                        correlators.substring(correlator.length() + 1));
            } else {
                registry.setStringValue(correlatorsPath, "");
            }
        } else {
            throw new MdmException(MdmError.INTERNAL);
        }
        String source = registry.getStringValue(Registry.makePath(Registry.ROOT, "exec", account,
                correlator, "source"));
        if (source == null || source.length() == 0) {
            throw new MdmException(MdmError.INTERNAL);
        }
        map.put("report_correlator", correlator);
        map.put("report_format", "text/plain");
        map.put("report_type", mScomo.getAlertType());
        map.put("report_mark", "informational");
        map.put("report_source", source);
        map.put("report_target", getDeliveredUri());
        map.put("report_data", data);
        map.put("report_account", account);
        map.put("report_initiator", Initiator.REPORT);
        if (clearState) {
            engine.getPLRegistry().deleteKeysByPrefix(
                    Registry.makePath(Registry.ROOT, "exec", account, correlator));
        }
        return map;
    }

    /**
     * Resume an aborted download session.
     *
     * @throws MdmException
     *         INVALID_CALL if SCOMO DP is not is in a middle of a download session, MEMORY if not
     *         enough memory for the action, MO_STORAGE if there was an error accessing MO external
     *         storage, or TREE_ACCESS_DENIED if access denied.
     */
    public void resumeDLSession() throws MdmException {
        // TODO add state checking
        MdmEngine engine = mScomo.getEngine();
        String sid = engine.getPLRegistry().getStringValue(Registry.DL_LAST_SESSION_ID);
        if (sid == null) {
            mScomo.logMsg(MdmLogLevel.ERROR, "No last DL session id found");
            throw new MdmException(MdmError.INTERNAL);
        }
        engine.triggerDLSession(null, // null for resume
                new DPDownloadPromptHandler(mScomo, this), new SimpleSessionInitiator(sid));
    }

    String getDownloadUri() {
        return MdmTree.makeUri(mScomo.getRootUri(), Uri.DOWNLOAD, mName);
    }

    String getDeliveredUri() {
        return MdmTree.makeUri(mScomo.getRootUri(), Uri.INVENTORY, Uri.DEPLOYED, mName);
    }

    boolean getInstallActive() {
        return mInstallActive;
    }

    void setInstallActive(boolean active) {
        mInstallActive = active;
    }

    public void setAlternativeDownload(boolean flag) {
        mAlternativeDownload = flag;
    }

    public boolean getAlternativeDownload() {
        return mAlternativeDownload;
    }
}
