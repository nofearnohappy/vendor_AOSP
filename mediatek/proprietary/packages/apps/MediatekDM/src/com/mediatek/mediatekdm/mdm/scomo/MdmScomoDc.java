package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.PLRegistry;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Initiator;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.InventoryDeployedStatus;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Registry;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Uri;

/**
 * Deployment Component Management Object.
 */
public class MdmScomoDc {
    /**
     * Deployment status.
     */
    public static enum DeployStatus {
        INVALID, ACTIVATION_DONE, ACTIVATION_START_POSTPONED, ACTIVATION_STARTED,
        DEACTIVATION_DONE, DEACTIVATION_STARTED, DEACTIVATION_START_POSTPONED, REMOVAL_DONE,
        REMOVAL_STARTED, REMOVAL_START_POSTPONED,
    }

    private final String mName;
    private MdmScomoDcHandler mHandler;
    private MdmScomo mScomo;
    private MdmScomoResult mDeploymentResult;
    private DeployStatus mDeployStatus;

    protected MdmScomoDc(String dcName, MdmScomoDcHandler handler, MdmScomo scomo) {
        mName = dcName;
        mHandler = handler;
        mScomo = scomo;
        mDeployStatus = DeployStatus.INVALID;
        mDeploymentResult = null;
    }

    public String getName() {
        return mName;
    }

    public void setHandler(MdmScomoDcHandler handler) {
        mHandler = handler;
    }

    public MdmScomoDcHandler getHandler() {
        return mHandler;
    }

    public void destroy() {
        mScomo.removeDc(mName);
    }

    /**
     * Add a DC. Called by the client to add a software component to the inventory branch.
     *
     * @param id
     * @param name
     * @param pkgId
     * @param version
     * @param description
     * @param envType
     * @param isActive
     * @throws MdmException
     */
    public void addToInventory(String id, String name, String pkgId, String version,
            String description, String envType, boolean isActive) throws MdmException {
        mScomo.logMsg(MdmLogLevel.DEBUG, "+MdmScomoDc.addToInventory");
        mScomo.logMsg(MdmLogLevel.DEBUG, "name = " + name + ", pkgId = " + pkgId + ", version ="
                + version + ", description = " + description + ", envType = " + envType);
        MdmTree tree = mScomo.getTree();
        String uri = getUri();
        byte[] empty = { 0 };
        tree.addInteriorNode(uri, null);
        tree.addChildLeafNode(uri, Uri.ID, "chr", null, (id == null ? empty : id.getBytes()));
        tree.addChildLeafNode(uri, Uri.PKGIDREF, "chr", null,
                (pkgId == null ? empty : pkgId.getBytes()));
        tree.addChildLeafNode(uri, Uri.NAME, "chr", null, (name == null ? empty : name.getBytes()));
        tree.addChildLeafNode(uri, Uri.DESCRIPTION, "chr", null, (description == null ? empty
                : description.getBytes()));
        tree.addChildLeafNode(uri, Uri.VERSION, "chr", null,
                (version == null ? empty : version.getBytes()));
        tree.addChildLeafNode(uri, Uri.STATE, "int", null, empty);
        tree.addChildLeafNode(uri, Uri.STATUS, "int", null, empty);
        tree.replaceIntValue(MdmTree.makeUri(uri, Uri.STATUS), InventoryDeployedStatus.IDLE.val);
        tree.addChildLeafNode(uri, Uri.ENVTYPE, "chr", null,
                (envType == null ? empty : envType.getBytes()));
        uri = MdmTree.makeUri(uri, Uri.OPERATIONS);
        tree.addInteriorNode(uri, null);
        tree.addChildLeafNode(uri, Uri.ACTIVATE, null, null, empty);
        tree.addChildLeafNode(uri, Uri.DEACTIVATE, null, null, empty);
        tree.addChildLeafNode(uri, Uri.REMOVE, null, null, empty);
        tree.registerExecute(MdmTree.makeUri(uri, Uri.ACTIVATE), new DCActivateExecHandler(mScomo,
                this));
        tree.registerExecute(MdmTree.makeUri(uri, Uri.DEACTIVATE), new DCDeactivateExecHandler(
                mScomo, this));
        tree.registerExecute(MdmTree.makeUri(uri, Uri.REMOVE),
                new DCRemoveExecHandler(mScomo, this));
        mScomo.logMsg(MdmLogLevel.DEBUG, "-MdmScomoDc.addToInventory");
    }

    String getId() throws MdmException {
        return mScomo.getTree().getStringValue(MdmTree.makeUri(getUri(), Uri.ID));
    }

    /**
     * Trigger report session (generic alert). Should be called by the client application after
     * remove, activate, deactivate, were executed. The report will send the result set by
     * setDeploymentResult(MdmScomoResult)
     *
     * @throws MdmException
     */
    public void triggerReportSession() throws MdmException {
        String data = "<ResultCode>" + mDeploymentResult.val + "</ResultCode><Identifier>"
                + getId() + "</Identifier>";
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
                "informational", source, null, data, account,
                new SimpleSessionInitiator(Initiator.REPORT));
        engine.getPLRegistry().deleteKeysByPrefix(
                Registry.makePath(Registry.ROOT, "exec", account, correlator));
    }

    /**
     * Set the result to be used by the report.
     *
     * @param resultCode
     *        Result code to be sent to the server. Should be one of VdmScomoResult values or in
     *        vendor-specific range: 1250-1299: Successful - Vendor Specified. 1450-1499: Client
     *        Error - Vendor Specified.
     * @throws MdmException
     *         MEMORY if not enough memory for the action, or BAD_INPUT if invalid input.
     */
    public void setDeploymentResult(MdmScomoResult resultCode) throws MdmException {
        mDeploymentResult = resultCode;
    }

    /**
     * Get status of deployment process.
     *
     * @return Status of deployment process. INVALD if no activation/deactivation invoked.
     * @throws MdmException
     */
    public DeployStatus getDeployStatus() {
        return mDeployStatus;
    }

    void setDeployStatus(DeployStatus status) {
        mDeployStatus = status;
    }

    /**
     * Execute uninstallation. Called by the client to execute the uninstallation of a a deployment
     * component that is available on the device. (Asynchronous uninstallation)
     *
     * @throws MdmException
     */
    public void executeRemove() throws MdmException {
        if (mHandler != null) {
            ScomoOperationResult result = mHandler.executeRemove(this);
            if (!result.mAsync) {
                setDeploymentResult(result.mResult);
                triggerReportSession();
                // update status & state node in tree
            }
        }
    }

    /**
     * Execute activation. Called by the client to execute the activation of a a deployment
     * component that is available on the device. (Asynchronous activation)
     *
     * @throws MdmException
     */
    public void executeActivate() throws MdmException {
        if (mHandler != null) {
            setDeployStatus(DeployStatus.ACTIVATION_STARTED);
            ScomoOperationResult result = mHandler.executeActivate(this);
            if (!result.mAsync) {
                setDeployStatus(DeployStatus.ACTIVATION_DONE);
                setDeploymentResult(result.mResult);
                triggerReportSession();
                // TODO update status & state node in tree
            }
        }
    }

    /**
     * Execute deactivation. Called by the client to execute the deactivation of a a deployment
     * component that is available on the device. (Asynchronous deactivation)
     *
     * @throws MdmException
     */
    public void executeDeactivate() throws MdmException {
        if (mHandler != null) {
            setDeployStatus(DeployStatus.DEACTIVATION_STARTED);
            ScomoOperationResult result = mHandler.executeDeactivate(this);
            if (!result.mAsync) {
                setDeployStatus(DeployStatus.DEACTIVATION_DONE);
                setDeploymentResult(result.mResult);
                triggerReportSession();
                // TODO update status & state node in tree
            }
        }
    }

    /**
     * Deletes a DC.
     *
     * @throws MdmException
     */
    public void deleteFromInventory() throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, " + deleteFromInventory");
        try {
            MdmTree tree = mScomo.getTree();
            String dcName = tree.findFirstNodeByName(
                    MdmTree.makeUri(mScomo.getRootUri(), Uri.INVENTORY, Uri.DEPLOYED), mName);
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, " dc name is " + dcName);
            if (dcName != null) {
                String uri = MdmTree.makeUri(getUri(), Uri.OPERATIONS);
                tree.unregisterExecute(MdmTree.makeUri(uri, Uri.ACTIVATE));
                tree.unregisterExecute(MdmTree.makeUri(uri, Uri.DEACTIVATE));
                tree.unregisterExecute(MdmTree.makeUri(uri, Uri.REMOVE));
                tree.deleteNode(getUri());
            }
        } catch (MdmException e) {
            e.printStackTrace();
            if (e.getError() != MdmError.NODE_MISSING) {
                throw e;
            }
        }
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, " - deleteFromInventory");
    }

    String getUri() {
        return MdmTree.makeUri(mScomo.getRootUri(), Uri.INVENTORY, Uri.DEPLOYED, mName);
    }
}
