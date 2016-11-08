package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.PLDlPkg;
import com.mediatek.mediatekdm.mdm.PLLogger;
import com.mediatek.mediatekdm.mdm.PLRegistry;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SCOMO manager. There can be multiple instances of MdmScomo as long as the sub-root URI of any
 * instance are not the prefix of every other instances.
 */
public class MdmScomo {

    static final class AlertType {
        public static final String OPERATION_COMPLETE = "urn:oma:at:scomo:1.0:OperationComplete";
    }

    static final class Registry {
        public static final String SEPERATOR = "|";
        public static final String SEPERATOR_REGEXP = "\\|";
        public static final String PACKAGE = "package";
        public static final String PATH = "path";
        public static final String ROOT = "mdm|scomo";
        public static final String DL_LAST_SESSION_ID = "mdm|scomo|dl|last_session_id";

        static String makePath(String... segments) {
            return Utils.join(SEPERATOR, segments);
        }
    }

    static final class Uri {
        public static final String DOWNLOAD = "Download";
        public static final String INVENTORY = "Inventory";
        public static final String DELIVERED = "Delivered";
        public static final String DEPLOYED = "Deployed";
        public static final String PKGID = "PkgID";
        public static final String NAME = "Name";
        public static final String PKGURL = "PkgURL";
        public static final String INSTALLPARAMS = "InstallParams";
        public static final String DESCRIPTION = "Description";
        public static final String STATUS = "Status";
        public static final String ENVTYPE = "EnvType";
        public static final String PKGTYPE = "PkgType";
        public static final String OPERATIONS = "Operations";
        public static final String DOWNLOADINSTALL = "DownloadInstall";
        public static final String DOWNLOADINSTALLINACTIVE = "DownloadInstallInactive";
        public static final String PKGVERSION = "PkgVersion";
        public static final String UPDATE = "Update";
        public static final String DOWNLOADANDUPDATE = "DownloadAndUpdate";
        public static final String STATE = "State";
        public static final String PKGDATA = "PkgData";
        public static final String DATA = "Data";
        public static final String INSTALL = "Install";
        public static final String INSTALLINACTIVE = "InstallInactive";
        public static final String REMOVE = "Remove";
        public static final String ACTIVATE = "Activate";
        public static final String DEACTIVATE = "Deactive";
        public static final String ID = "ID";
        public static final String PKGIDREF = "PkgIDRef";
        public static final String VERSION = "Version";
    }

    public static final class Initiator {
        public static final String SEPERATOR = "|";
        public static final String SEPERATOR_REGEXP = "\\|";
        public static final String PREFIX = "MDM_SCOMO";
        public static final String CI = PREFIX + SEPERATOR + "CI";
        public static final String REPORT = PREFIX + SEPERATOR + "REPORT";
        public static final String DL = PREFIX + SEPERATOR + "DL";

        static String makePath(String... segments) {
            return Utils.join(SEPERATOR, segments);
        }
    }

    /**
     * Status code for \<scomo root\>/Inventory/Delivered/\<x\>/Status.
     */
    static enum InventoryDeliveredStatus {
        /** There is no data available and download is about to start. */
        IDLE(10), REMOVE_FAILED(20), REMOVE_PROGRESSING(30), INSTALL_PROGRESSING(40),
        INSTALL_FAILED_WITH_DATA(50), INSTALL_FAILED_WITHOUT_DATA(60);

        public final int val;

        private InventoryDeliveredStatus(int value) {
            val = value;
        }

        public static InventoryDeliveredStatus fromInt(int value) {
            for (InventoryDeliveredStatus s : InventoryDeliveredStatus.values()) {
                if (s.val == value) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * State code for \<scomo root\>/Inventory/Delivered/\<x\>/State.
     */
    static enum InventoryDeliveredState {
        /** There is no data available and download is about to start. */
        DELIVERED(10), INSTALLED(20);

        public final int val;

        private InventoryDeliveredState(int value) {
            val = value;
        }

        public static InventoryDeliveredState fromInt(int value) {
            for (InventoryDeliveredState s : InventoryDeliveredState.values()) {
                if (s.val == value) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * Status code for \<scomo root\>/Inventory/Delivered/\<x\>/Status.
     */
    static enum InventoryDeployedStatus {
        /** There is no data available and download is about to start. */
        IDLE(10), REMOVE_FAILED(20), REMOVE_PROGRESSING(30), ACTIVATE_FAILED(40),
        ACTIVATE_PROGRESSING(50), DEACTIVATE_FAILED(60), DEACTIVATE_PROGRESSING(70);

        public final int val;

        private InventoryDeployedStatus(int value) {
            val = value;
        }

        public static InventoryDeployedStatus fromInt(int value) {
            for (InventoryDeployedStatus s : InventoryDeployedStatus.values()) {
                if (s.val == value) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * State code for \<scomo root\>/Inventory/Delivered/\<x\>/State.
     */
    static enum InventoryDeployedState {
        /** There is no data available and download is about to start. */
        INACTIVE(10), ACTIVE(20);

        public final int val;

        private InventoryDeployedState(int value) {
            val = value;
        }

        public static InventoryDeployedState fromInt(int value) {
            for (InventoryDeployedState s : InventoryDeployedState.values()) {
                if (s.val == value) {
                    return s;
                }
            }
            return null;
        }
    }

    static final String ROOT_URI_TYPE = "urn:oma:mo:oma-scomo:1.0";
    public static final String SESSION_ACTION_KEY = "SCOMO";
    public static final String LOG_TAG = "[SCOMO] ";

    private static Map<String, MdmScomo> sInstances = new HashMap<String, MdmScomo>();
    private Map<String, MdmScomoDp> mDps;
    private Map<String, MdmScomoDc> mDcs;
    private boolean mAutoAddDPChildNodes;
    private String mAlertType;
    private String mRootUri;
    private MdmScomoHandler mHandler;
    private MdmTree mTree;
    private PLDlPkg mPLDlPkg;
    private PLRegistry mPLRegistry;
    private PLLogger mLogger;
    private MdmEngine mEngine;
    Set<SessionInitiator> mPendingSession = new HashSet<SessionInitiator>();
    private ScomoSSO mSSO;

    /**
     * Status code for \<scomo root\>/Download/\<x\>/Status.
     */
    static enum DownloadStatus {
        /** There is no data available and download is about to start. */
        IDLE(10), DOWNLOAD_FAILED(20), DOWNLOAD_PROGRESSING(30), DOWNLOAD_COMPLETE(40),
        INSTALL_PROGRESSING(50), INSTALL_FAILED_WITH_DATA(60), INSTALL_FAILED_WITHOUT_DATA(70);

        public final int val;

        private DownloadStatus(int value) {
            val = value;
        }

        public static DownloadStatus fromInt(int value) {
            for (DownloadStatus s : DownloadStatus.values()) {
                if (s.val == value) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * Get the single instance of SCOMO manager.
     *
     * @param scomoRootUri
     *        Root URI in DM tree.
     * @param handler
     *        MdmScomoHandler instance.
     * @return Single instance of SCOMO manager.
     * @throws MdmException
     */
    public static MdmScomo getInstance(String scomoRootUri, MdmScomoHandler handler)
            throws MdmException {
        synchronized (sInstances) {
            if (!sInstances.containsKey(scomoRootUri)) {
                // sanity test
                for (String uri : sInstances.keySet()) {
                    if (uri.startsWith(scomoRootUri) || scomoRootUri.startsWith(uri)) {
                        throw new MdmException(MdmError.BAD_INPUT,
                                "Sub-root URI cannot prefix or be the prefix of existing instances' URI");
                    }
                }
                sInstances.put(scomoRootUri, new MdmScomo(scomoRootUri, handler));
            }
            return sInstances.get(scomoRootUri);
        }
    }

    private MdmScomo(String scomoRootUri, MdmScomoHandler handler) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                LOG_TAG + "Create SCOMO instance with uri " + scomoRootUri);
        if (scomoRootUri == null) {
            throw new MdmException(MdmException.MdmError.BAD_INPUT, "scomoRootURI can NOT be null.");
        }
        if (scomoRootUri.charAt(scomoRootUri.length() - 1) == '/') {
            mRootUri = scomoRootUri.substring(0, scomoRootUri.length() - 1);
        } else {
            mRootUri = scomoRootUri;
        }
        mHandler = handler;
        mDps = new HashMap<String, MdmScomoDp>();
        mDcs = new HashMap<String, MdmScomoDc>();
        mAutoAddDPChildNodes = false;
        mAlertType = AlertType.OPERATION_COMPLETE;
        mTree = new MdmTree();
        mEngine = MdmEngine.getInstance();
        mPLRegistry = mEngine.getPLRegistry();
        mPLDlPkg = mEngine.getPLDlPkg();
        mLogger = MdmEngine.getLogger();
        buildDPList();
        buildDCList();
        mSSO = new ScomoSSO(this);
        mEngine.registerSessionStateObserver(mSSO);
        mTree.registerSubtreeOnAddHandler(MdmTree.makeUri(mRootUri, Uri.DOWNLOAD),
                new DPDownloadSubtreeAddHandler(this));
        mTree.registerSubtreeOnAddHandler(MdmTree.makeUri(mRootUri, Uri.INVENTORY, Uri.DELIVERED),
                new DPDeliveredSubtreeAddHandler(this));
    }

    /*
     * Build DP list from DM tree under Download and Inventory/Delivered.
     * @throws MdmException
     */
    private void buildDPList() throws MdmException {
        logMsg(MdmLogLevel.DEBUG, "+MdmScomo.buildDPList()");
        String[] dpNodeList = mTree.listChildren(MdmTree.makeUri(mRootUri, Uri.INVENTORY,
                Uri.DELIVERED));
        for (String dpNodeName : dpNodeList) {
            if (dpNodeName != null && dpNodeName.length() != 0) {
                logMsg(MdmLogLevel.WARNING, "Add DP " + dpNodeName + " from Delivered");
                mDps.put(dpNodeName, new MdmScomoDp(dpNodeName, null, this, true));
            }
        }
        dpNodeList = mTree.listChildren(MdmTree.makeUri(mRootUri, Uri.DOWNLOAD));
        for (String dpNodeName : dpNodeList) {
            if (dpNodeName != null && dpNodeName.length() != 0) {
                if (!mDps.containsKey(dpNodeName)) {
                    logMsg(MdmLogLevel.WARNING, "Add DP " + dpNodeName + " from Download");
                    mDps.put(dpNodeName, new MdmScomoDp(dpNodeName, null, this, false));
                } else {
                    logMsg(MdmLogLevel.WARNING, "DP " + dpNodeName
                            + " already exists in Delivered, ignore duplication");
                }
            }
        }
        logMsg(MdmLogLevel.DEBUG, "-MdmScomo.buildDPList()");
    }

    /*
     * Build DC list from DM tree under Inventory/Deployed.
     * @throws MdmException
     */
    private void buildDCList() throws MdmException {
        logMsg(MdmLogLevel.DEBUG, "+MdmScomo.buildDCList()");
        String[] dcNodeList = mTree.listChildren(MdmTree.makeUri(mRootUri, Uri.INVENTORY,
                Uri.DEPLOYED));
        for (String dcNodeName : dcNodeList) {
            if (dcNodeName != null && dcNodeName.length() != 0) {
                MdmScomoDc dc = new MdmScomoDc(dcNodeName, null, this);
                mDcs.put(dcNodeName, dc);
                String uri = MdmTree.makeUri(getRootUri(), Uri.INVENTORY, Uri.DEPLOYED, dcNodeName,
                        Uri.OPERATIONS);
                mTree.registerExecute(MdmTree.makeUri(uri, Uri.ACTIVATE),
                        new DCActivateExecHandler(this, dc));
                mTree.registerExecute(MdmTree.makeUri(uri, Uri.DEACTIVATE),
                        new DCDeactivateExecHandler(this, dc));
                mTree.registerExecute(MdmTree.makeUri(uri, Uri.REMOVE), new DCRemoveExecHandler(
                        this, dc));
            }
        }
        logMsg(MdmLogLevel.DEBUG, "-MdmScomo.buildDCList()");
    }

    public void destroy() throws MdmException {
        synchronized (sInstances) {
            mEngine.unregisterSessionStateObserver(mSSO);
            mTree.unregisterSubtreeOnAddHandler(MdmTree.makeUri(mRootUri, Uri.DOWNLOAD));
            mTree.unregisterSubtreeOnAddHandler(MdmTree.makeUri(mRootUri, Uri.INVENTORY,
                    Uri.DELIVERED));
            mDps = null;
            mDcs = null;
            mSSO = null;
            mTree = null;
            mEngine = null;
            sInstances.remove(mRootUri);
            // TODO check this method, destroy something
        }
    }

    /**
     * Create a MdmScomoDp instance.
     *
     * @param dpName
     *        SCOMO DP name
     * @param handler
     *        SCOMO DP Handler
     * @return
     */
    public MdmScomoDp createDP(String dpName, MdmScomoDpHandler handler) {
        MdmScomoDp dp = searchDp(dpName);
        if (dp == null) {
            dp = new MdmScomoDp(dpName, handler, this);
            mDps.put(dpName, dp);
        }
        return dp;
    }

    /**
     * Create a MdmScomoDc instance. This constructor allows the user to use a different
     * implementation for ScomoFactory. <b>Note</b>: You must call MDMScomoDc.destroy() when the
     * client terminates to allow for graceful exit the SCOMO DC instance.
     *
     * @param dcName
     * @param handler
     * @param inventory
     * @return
     */
    public MdmScomoDc createDC(String dcName, MdmScomoDcHandler handler) {
        MdmScomoDc dc = searchDc(dcName);
        if (dc == null) {
            dc = new MdmScomoDc(dcName, handler, this);
            mDcs.put(dcName, dc);
        }
        return dc;
    }

    protected MdmScomoDc searchDc(String dcName) {
        return mDcs.get(dcName);
    }

    /**
     * return the first DP with name equals to dpName.
     *
     * @param dpName
     * @return The DP found or null if nothing found.
     */
    protected MdmScomoDp searchDp(String dpName) {
        return mDps.get(dpName);
    }

    protected void removeDp(String dpName) {
        mDps.remove(dpName);
    }

    protected void removeDc(String dcName) {
        mDcs.remove(dcName);
    }

    /**
     * Set whether, when a new delivery package is added to the DM Tree, its child nodes should be
     * added automatically by SCOMO. Default is false (the server is expected to add the nodes).
     *
     * @param autoAdd
     * @throws MdmException
     */
    public void setAutoAddDPChildNodes(boolean autoAdd) throws MdmException {
        mAutoAddDPChildNodes = autoAdd;
    }

    /**
     * Get whether, when a new delivery package is added to the DM Tree, its child nodes should be
     * added automatically by SCOMO.
     *
     * @return
     * @throws MdmException
     */
    public boolean getAutoAddDPChildNodes() throws MdmException {
        return mAutoAddDPChildNodes;
    }

    /**
     * Set alert type string to be used on SCOMO generic alerts.
     *
     * @param alertType
     *        Alert type string.
     * @throws MdmException
     */
    public void setAlertType(String alertType) throws MdmException {
        mAlertType = alertType;
    }

    /**
     * Get alert type string to be used on SCOMO generic alerts.
     *
     * @return Alert type string.
     * @throws MdmException
     */
    public String getAlertType() throws MdmException {
        return mAlertType;
    }

    /**
     * Get an ArrayList of currently available delivery packages.
     *
     * @return ArrayList of DPs.
     */
    public ArrayList<MdmScomoDp> getDps() {
        return new ArrayList<MdmScomoDp>(mDps.values());
    }

    /**
     * Get an ArrayList of currently available deployment components.
     *
     * @return ArrayList of DCs.
     */
    public ArrayList<MdmScomoDc> getDcs() {
        return new ArrayList<MdmScomoDc>(mDcs.values());
    }

    /**
     * Query which actions that are relevant for this SCOMO instance were performed last session.
     *
     * @return Bit flags describing the session actions.
     */
    public int querySessionActions() {
        return mEngine.getSessionActions(SESSION_ACTION_KEY) & ScomoAction.ALL;
    }

    public String getRootUri() {
        return mRootUri;
    }

    MdmTree getTree() {
        return mTree;
    }

    MdmEngine getEngine() {
        return MdmEngine.getInstance();
    }

    PLDlPkg getPLDlPkg() {
        return mPLDlPkg;
    }

    MdmScomoHandler getHandler() {
        return mHandler;
    }

    /**
     * Called when new DP is added under ./\<scomo\>/Download/
     *
     * @TODO Modify the ACL for children
     * @param name
     *        new node name under ./\<scomo\>/Download/
     */
    void onNewDP(String name) throws MdmException {
        logMsg(MdmLogLevel.DEBUG, "+MdmScomo.onNewDP(" + name + ")");
        final String uri = MdmTree.makeUri(mRootUri, Uri.DOWNLOAD, name);
        MdmScomoDp dp = createDP(name, null);
        /* register add/delete handlers */
        DPOperationsAddDelHandler handler = new DPOperationsAddDelHandler(this, dp);
        mTree.registerOnAddHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.DOWNLOAD), handler);
        mTree.registerOnDeleteHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.DOWNLOAD), handler);
        mTree.registerOnAddHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.DOWNLOADINSTALL),
                handler);
        mTree.registerOnDeleteHandler(MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.DOWNLOADINSTALL),
                handler);
        mTree.registerOnAddHandler(
                MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.DOWNLOADINSTALLINACTIVE), handler);
        mTree.registerOnDeleteHandler(
                MdmTree.makeUri(uri, Uri.OPERATIONS, Uri.DOWNLOADINSTALLINACTIVE), handler);

        if (mAutoAddDPChildNodes) {
            logMsg(MdmLogLevel.DEBUG, "add children automatically");
            final byte[] empty = { 0 };
            mTree.addChildLeafNode(uri, Uri.PKGID, "chr", null, empty);
            mTree.addChildLeafNode(uri, Uri.NAME, "chr", null, empty);
            mTree.addChildLeafNode(uri, Uri.PKGURL, "chr", null, empty);
            mTree.addChildLeafNode(uri, Uri.INSTALLPARAMS, "chr", null, empty);
            mTree.addChildLeafNode(uri, Uri.DESCRIPTION, "chr", null, empty);
            mTree.addChildLeafNode(uri, Uri.STATUS, "int", null,
                    Integer.toString(DownloadStatus.IDLE.val).getBytes());
            mTree.addChildLeafNode(uri, Uri.ENVTYPE, "chr", null, empty);
            mTree.addChildLeafNode(uri, Uri.PKGTYPE, "chr", null, empty);
            mTree.addInteriorChildNode(uri, Uri.OPERATIONS, null);
            final String operationUri = MdmTree.makeUri(uri, Uri.OPERATIONS);
            mTree.addChildLeafNode(operationUri, Uri.DOWNLOAD, null, null, empty);
            mTree.addChildLeafNode(operationUri, Uri.DOWNLOADINSTALL, null, null, empty);
            mTree.addChildLeafNode(operationUri, Uri.DOWNLOADINSTALLINACTIVE, null, null, empty);
        }

        if (mHandler != null) {
            logMsg(MdmLogLevel.DEBUG, "invoke handler newDpAdded(" + name + ")");
            mHandler.newDpAdded(name);
        }
        logMsg(MdmLogLevel.DEBUG, "-MdmScomo.onNewDP()");
    }

    PLRegistry getPLRegistry() {
        return mPLRegistry;
    }

    void logMsg(MdmLogLevel level, String message) {
        mLogger.logMsg(level, LOG_TAG + message);
    }
}
