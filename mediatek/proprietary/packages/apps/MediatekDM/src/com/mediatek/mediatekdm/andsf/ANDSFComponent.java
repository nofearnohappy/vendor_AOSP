
package com.mediatek.mediatekdm.andsf;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.common.collaboradio.CollaboRadioManager;
import com.mediatek.common.collaboradio.andsf.AndsfMO;
import com.mediatek.common.collaboradio.andsf.AndsfMORoot;
import com.mediatek.common.collaboradio.andsf.Node3GPPLocation;
import com.mediatek.common.collaboradio.andsf.NodeAppID;
import com.mediatek.common.collaboradio.andsf.NodeArea;
import com.mediatek.common.collaboradio.andsf.NodeCircular;
import com.mediatek.common.collaboradio.andsf.NodeDiscoveryInfo;
import com.mediatek.common.collaboradio.andsf.NodeForFlowBased;
import com.mediatek.common.collaboradio.andsf.NodeForServiceBased;
import com.mediatek.common.collaboradio.andsf.NodeIPFlow;
import com.mediatek.common.collaboradio.andsf.NodeISRP;
import com.mediatek.common.collaboradio.andsf.NodePolicy;
import com.mediatek.common.collaboradio.andsf.NodeRoutingCriteria;
import com.mediatek.common.collaboradio.andsf.NodeRoutingRule;
import com.mediatek.common.collaboradio.andsf.NodeTimeOfDay;
import com.mediatek.common.collaboradio.andsf.NodeUELocation;
import com.mediatek.common.collaboradio.andsf.NodeWLANLocation;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;
import com.mediatek.mediatekdm.mdm.andsf.MdmAndsf;
import com.mediatek.mediatekdm.util.Utilities;

import java.util.List;

public class ANDSFComponent implements IDmComponent {
    static final String NAME = "ANDSF";
    public static final String ROOT_URI = "./ANDSF";
    static final int URI_OFFSET = ROOT_URI.split(MdmTree.URI_SEPERATOR).length;

    public static final String INTENT_ACTION_REQUEST = "com.mediatek.mediatedm.andsf.REQUEST";
    public static final String INTENT_ACTION_RESULT = "com.mediatek.mediatedm.andsf.RESULT";
    public static final String INTENT_KEY_TYPE = "type";
    public static final String INTENT_KEY_RESULT = "result";
    public static final String INTENT_KEY_REQUEST_ID = "id";
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAIL = "fail";

    AndsfMORoot mAndsfMORoot = null;

    private MdmAndsf mAndsf;
    private AndsfManager mManager;
    private CollaboRadioManager mCRManager;

    public static final class RequestType {
        public static final String UE_LOCATION = "CI_ANDSF_UE_LOCATION";
        public static final String UE_PROFILE = "CI_ANDSF_UE_PROFILE";
        public static final String PROVISION = "CI_ANDSF_PROVISION";
        public static final String PROVISION_SINGLE_IF = "CI_ANDSF_PROVISION_SINGLE_IF";
        public static final String PROVISION_MULTIPLE_IF = "CI_ANDSF_PROVISION_MULTIPLE_IF";
        public static final String PROVISION_DISC_INFO = "CI_ANDSF_PROVISION_DISC_INFO";
    }

    public static final class TopPath {
        public static final String POLICY = "Policy";
        public static final String UE_LOCATION = "UE_Location";
        public static final String DISCOVERY_INFORMATION = "DiscoveryInformation";
        public static final String ISRP = "ISRP";
        public static final String UE_PROFILE = "UE_Profile";
    }

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        return false;
    }

    @Override
    public void attach(DmService service) {
        mCRManager = (CollaboRadioManager) service.getSystemService(Context.MTK_COLLABO_RADIO_SERVICE);
        Log.d(NAME, "attach: CollaboRadioManager = " + mCRManager);
        mManager = new AndsfManager(service, this);
        try {
            mAndsf = new MdmAndsf(ROOT_URI);
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    @Override
    public void detach(DmService service) {
        mCRManager = null;
        mManager = null;
        mAndsf.destroy();
        mAndsf = null;
    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        if (INTENT_ACTION_REQUEST.equals(intent.getAction())) {
            String requestType = intent.getStringExtra(INTENT_KEY_TYPE);
            if (requestType == null) {
                throw new Error("[ANDSF] Request " + INTENT_ACTION_REQUEST + " must has an string extra "
                        + INTENT_KEY_TYPE);
            } else if (requestType.equals(RequestType.UE_LOCATION) ||
                    requestType.equals(RequestType.UE_PROFILE) ||
                    requestType.equals(RequestType.PROVISION) ||
                    requestType.equals(RequestType.PROVISION_SINGLE_IF) ||
                    requestType.equals(RequestType.PROVISION_MULTIPLE_IF) ||
                    requestType.equals(RequestType.PROVISION_DISC_INFO)) {
                DmOperation operation = new DmOperation();
                operation.setProperty(KEY.TYPE, requestType);
                operation.setProperty(INTENT_KEY_REQUEST_ID, intent.getStringExtra(INTENT_KEY_REQUEST_ID));
                DmOperationManager.getInstance().enqueue(operation, true);
                return DispatchResult.ACCEPT_AND_TRIGGER;
            } else {
                throw new Error("[ANDSF] Unsupported request type " + INTENT_KEY_TYPE);
            }
        } else {
            return DispatchResult.IGNORE;
        }
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        String type = operation.getProperty(KEY.TYPE);
        switch (action) {
            case NEW:
            case RECOVER:
            case RETRY:
                try {
                    if (type.equals(RequestType.UE_LOCATION)) {
                        mAndsf.triggerUELocationSession();
                        return DispatchResult.ACCEPT;
                    } else if (type.equals(RequestType.UE_PROFILE)) {
                        mAndsf.triggerUEProfileSession();
                        return DispatchResult.ACCEPT;
                    } else if (type.equals(RequestType.PROVISION)) {
                        mAndsf.triggerProvisionSession();
                        return DispatchResult.ACCEPT;
                    } else if (type.equals(RequestType.PROVISION_SINGLE_IF)) {
                        mAndsf.triggerProvisionSingleIfSession();
                        return DispatchResult.ACCEPT;
                    } else if (type.equals(RequestType.PROVISION_MULTIPLE_IF)) {
                        mAndsf.triggerProvisionMultipleIfSession();
                        return DispatchResult.ACCEPT;
                    } else if (type.equals(RequestType.PROVISION_DISC_INFO)) {
                        mAndsf.triggerProvisionDiscInfoSession();
                        return DispatchResult.ACCEPT;
                    } else {
                        throw new Error("[ANDSF] Unsupported operation type " + type);
                    }
                } catch (MdmException e) {
                    throw new Error(e);
                }
            default:
                return DispatchResult.IGNORE;
        }
    }

    @Override
    public SessionHandler dispatchSessionStateChange(
            SessionType type,
            SessionState state,
            int lastError,
            SessionInitiator initiator,
            DmOperation operation) {
        if (operation.getProperty(KEY.TYPE).contains("ANDSF")) {
            return mManager;
        } else {
            return null;
        }
    }

    @Override
    public boolean forceSilentMode() {
        return false;
    }

    @Override
    public IBinder getBinder(Intent intent) {
        return null;
    }

    @Override
    public String getDlPackageFilename() {
        return null;
    }

    @Override
    public String getDlResumeFilename() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void kickoff(Context context) {
    }

    @Override
    public void configureDmTree(MdmTree tree) {
        Log.d(TAG.NODEIOHANDLER, "[ANDSF] configureDmTree");
        try {
            mAndsfMORoot = (AndsfMORoot) mCRManager.readAndsfMO("/");

            // clear old nodes
            String[] children = tree.listChildren(ROOT_URI);
            Log.d(NAME, "children length  " + children.length);
            for (String child : children) {
                Log.d(NAME, "child is  " + child);
                if (!"".equals(child)) {
                    tree.deleteNode(MdmTree.makeUri(ROOT_URI, child));
                }
            }

            if (mAndsfMORoot.policy != null) {
                registerPolicy(tree);
            }
            if (mAndsfMORoot.discoveryInfo != null) {
                registerDiscovery(tree);
            }
            if (mAndsfMORoot.ueLocation != null) {
                registerUELocation(tree);
            }
            if (mAndsfMORoot.isrp != null) {
                registerISRP(tree);
            }
            if (mAndsfMORoot.ueProfile != null) {
                registerUEProfile(tree);
            }
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    void writeBackAndsfMO() {
        mCRManager.writeAndsfMO("/", mAndsfMORoot);
    }

    private void registerPolicy(MdmTree tree) throws MdmException {
        // 1. create new node
        final String subrootUri = MdmTree.makeUri(ROOT_URI, TopPath.POLICY);
        tree.addInteriorNode(subrootUri, null);
        tree.replaceACL(subrootUri, "Get=*&Replace=*");

        // 2. build nodes
        List<NodePolicy.X> nodes = mAndsfMORoot.policy.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodePolicy.X node = nodes.get(i);

            String uri = MdmTree.makeUri(subrootUri, nodeXId, "PrioritizedAccess");
            tree.addInteriorNode(uri, null);
            registerRoutingRule(tree, uri, node.prioritizedAccess);

            if (node.validityArea != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "ValidityArea");
                tree.addInteriorNode(uri, null);
                registerValidityArea(tree, uri, node.validityArea);
            }

            if (node.timeOfDay != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "TimeOfDay");
                tree.addInteriorNode(uri, null);
                registerTimeOfDay(tree, uri, node.timeOfDay);
            }

            // For leaf nodes
            uri = MdmTree.makeUri(subrootUri, nodeXId, "RulePriority");
            Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
            tree.registerNodeIoHandler(uri, new PolicyRulePriorityGenericHandler(uri, node));

            if (node.roaming != AndsfMORoot.ABSENT_FIELD_BYTE_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "Roaming");
                Utilities.addLeafNodeChecked(tree, uri, "bool", null, null);
                tree.registerNodeIoHandler(uri, new PolicyRoamingHandler(uri, node));
            }

            uri = MdmTree.makeUri(subrootUri, nodeXId, "PLMN");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new PolicyPLMNHandler(uri, node));

            // Always add node; if node not exits, default value is false
            uri = MdmTree.makeUri(subrootUri, nodeXId, "UpdatePolicy");
            Utilities.addLeafNodeChecked(tree, uri, "bool", null, null);
            tree.registerNodeIoHandler(uri, new PolicyUpdatePolicyHandler(uri, node));
        }
    }

    private void registerDiscovery(MdmTree tree) throws MdmException {

        // 1. Create new node
        final String subrootUri = MdmTree.makeUri(ROOT_URI, TopPath.DISCOVERY_INFORMATION);
        tree.addInteriorNode(subrootUri, null);
        tree.replaceACL(subrootUri, "Get=*&Replace=*");

        // 2. build nodes
        List<NodeDiscoveryInfo.X> nodes = mAndsfMORoot.discoveryInfo.x;

        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeDiscoveryInfo.X node = nodes.get(i);

            String uri = MdmTree.makeUri(subrootUri, nodeXId, "AccessNetworkArea");
            tree.addInteriorNode(uri, null);
            registerValidityArea(tree, uri, node.accessNetworkArea);

            // For leaf nodes
            uri = MdmTree.makeUri(subrootUri, nodeXId, "AccessNetworkType");
            Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
            tree.registerNodeIoHandler(uri, new DiscoveryNetworkTypeHandler(uri, node));

            if (node.accessNetworkInfoRef != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "AccessNetworkInformationRef");
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new DiscoveryNetworkInfoRefHandler(uri, node));
            }

            uri = MdmTree.makeUri(subrootUri, nodeXId, "PLMN");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new DiscoveryPLMNHandler(uri, node));
        }
    }

    private void registerUELocation(MdmTree tree) throws MdmException {
        // 1. create new node
        final String subrootUri = MdmTree.makeUri(ROOT_URI, TopPath.UE_LOCATION);
        tree.addInteriorNode(subrootUri, null);
        tree.replaceACL(subrootUri, "Get=*");

        NodeUELocation node = mAndsfMORoot.ueLocation;
        String uri = null;

        if (node.location3GPP != null) {
            uri = MdmTree.makeUri(subrootUri, "3GPP_Location");
            tree.addInteriorNode(uri, null);
            register3GPPLocation(tree, uri, node.location3GPP);
        }

        if (node.locationWLAN != null) {
            uri = MdmTree.makeUri(subrootUri, "WLAN_Location");
            tree.addInteriorNode(uri, null);
            registerWLANLocation(tree, uri, node.locationWLAN);
        }

        // Leaf nodes
        if (node.locationGeo != null)  {
            uri = MdmTree.makeUri(subrootUri, "Geo_Location", "Latitude");
            tree.registerNodeIoHandler(uri, new LocationUELatitudeHandler(uri, node));

            uri = MdmTree.makeUri(subrootUri, "Geo_Location", "Longtitude");
            tree.registerNodeIoHandler(uri, new LocationUELongtitudeHandler(uri, node));
        }

        if (node.rplmn != null) {
            uri = MdmTree.makeUri(subrootUri, "RPLMN");
            tree.registerNodeIoHandler(uri, new LocatioUERPLMNHandler(uri, node));
        }
    }

    // <X>/ISRP
    private void registerISRP(MdmTree tree) throws MdmException {
        // 1. create new node
        final String subrootUri = MdmTree.makeUri(ROOT_URI, TopPath.ISRP);
        tree.addInteriorNode(subrootUri, null);
        tree.replaceACL(subrootUri, "Get=*&Replace=*");

        // 2. build nodes
        List<NodeISRP.X> nodes = mAndsfMORoot.isrp.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeISRP.X node = nodes.get(i);
            String uri = null;

            if (node.flowBased != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "ForFlowBased");
                tree.addInteriorNode(uri, null);
                registerForFlowBased(tree, uri, node.flowBased);
            }

            if (node.serviceBased != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "ForServiceBased");
                tree.addInteriorNode(uri, null);
                registerForServiceBased(tree, MdmTree.makeUri(subrootUri, nodeXId, "ForServiceBased"), node.serviceBased);
            }

            if (node.nonSeamlessOffload != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "ForNonSeamlessOffload");
                tree.addInteriorNode(uri, null);
                registerForNonSeamlessOffload(
                        tree,
                        MdmTree.makeUri(subrootUri, nodeXId, "ForNonSeamlessOffload"),
                        node.nonSeamlessOffload);
            }

            if (node.roaming != AndsfMO.ABSENT_FIELD_BYTE_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "Roaming");
                Utilities.addLeafNodeChecked(tree, uri, "bool", null, null);
                tree.registerNodeIoHandler(uri, new RoamingHandler(uri, node));
            }

            uri = MdmTree.makeUri(subrootUri, nodeXId, "PLMN");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new PLMNHandler(uri, node));

            uri = MdmTree.makeUri(subrootUri, nodeXId, "UpdatePolicy");
            Utilities.addLeafNodeChecked(tree, uri, "bool", null, null);
            tree.registerNodeIoHandler(uri, new UpdatePolicyHandler(uri, node));
        }
    }

    // <X>/ISRP/<X>/ForFlowBased
    private void registerForFlowBased(MdmTree tree, String subrootUri, NodeForFlowBased nodeForFlowBased)
            throws MdmException {
        List<NodeForFlowBased.X> nodes = nodeForFlowBased.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeForFlowBased.X node = nodes.get(i);
            String uri = null;

            uri = MdmTree.makeUri(subrootUri, nodeXId, "IPFlow");
            tree.addInteriorNode(uri, null);
            registerIPFlow(tree, uri, node.ipFlow);

            if (node.routingCriteria != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "RoutingCriteria");
                tree.addInteriorNode(uri, null);
                registerRoutingCriteria(tree, uri, node.routingCriteria);
            }

            uri = MdmTree.makeUri(subrootUri, nodeXId, "RoutingRule");
            tree.addInteriorNode(uri, null);
            registerRoutingRule(tree, uri, node.routingRule);

            uri = MdmTree.makeUri(subrootUri, nodeXId, "RoutingPriority");
            Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
            tree.registerNodeIoHandler(uri, new RulePriorityGenericHandler(uri, node));
        }
    }

    // <X>/ISRP/<X>/ForFlowBased/<X>/IPFlow
    private void registerIPFlow(MdmTree tree, String subrootUri, NodeIPFlow nodeIPFlow) throws MdmException {
        List<NodeIPFlow.X> nodes = nodeIPFlow.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeIPFlow.X node = nodes.get(i);
            String uri = null;

            if (node.appID != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "App-ID");
                tree.addInteriorNode(uri, null);
                registerAppID(tree, uri, node.appID);
            }

            if (node.addressType != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.ADDRESS_TYPE);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.startSrcIPAddress != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.START_SOURCE_IP_ADDRESS);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.endSrcIPAddress != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.END_SOURCE_IP_ADDRESS);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.startDestIPAddress != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.START_DEST_IP_ADDRESS);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.endDestIPAddress != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.END_DEST_IP_ADDRESS);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.protocolType != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.PROTOCOL_TYPE);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.startSrcPortNumber != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.START_SOURCE_PORT_NUMBER);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.endSrcPortNumber != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.END_SOURCE_PORT_NUMBER);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.startDestPortNumber != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.START_DEST_PORT_NUMBER);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.endDestPortNumber != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.END_DEST_PORT_NUMBER);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.qos != AndsfMO.ABSENT_FIELD_BYTE_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.QOS);
                Utilities.addLeafNodeChecked(tree, uri, "bin", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }

            if (node.domainName != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, IPFlowGenericHandler.DOMAIN_NAME);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new IPFlowGenericHandler(uri, node));
            }
        }
    }

    // <X>/ISRP/<X>/ForFlowBased/<X>/IPFlow/<X>/App-ID
    private void registerAppID(MdmTree tree, String subrootUri, NodeAppID nodeAppID) throws MdmException {
        List<NodeAppID.X> nodes = nodeAppID.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeAppID.X node = nodes.get(i);

            String uri = MdmTree.makeUri(subrootUri, nodeXId, "OSId");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new OSIdHandler(uri, node));

            uri = MdmTree.makeUri(subrootUri, nodeXId, "OSApps");
            tree.addInteriorNode(uri, null);
            registerOSApps(tree, uri, node);
        }
    }

    // <X>/ISRP/<X>/ForFlowBased/<X>/IPFlow/<X>/App-ID/<X>/OSApps
    private void registerOSApps(MdmTree tree, String subrootUri, NodeAppID.X xNode) throws MdmException {
        List<String> nodes = xNode.listOSAppId;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);

            String uri = MdmTree.makeUri(subrootUri, nodeXId, "OSAppId");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new OSAppIdHandler(uri, xNode, i));
        }
    }

    private void registerRoutingCriteria(MdmTree tree, String subrootUri, NodeRoutingCriteria nodeRoutingCriteria)
            throws MdmException {
        List<NodeRoutingCriteria.X> nodes = nodeRoutingCriteria.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeRoutingCriteria.X node = nodes.get(i);
            String uri = null;

            if (node.validityArea != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "ValidityArea");
                tree.addInteriorNode(uri, null);
                registerValidityArea(tree, uri, node.validityArea);
            }

            if (node.timeOfDay != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "TimeOfDay");
                tree.addInteriorNode(uri, null);
                registerTimeOfDay(tree, uri, node.timeOfDay);
            }

            if (node.apn != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "APN");
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new APNHandler(uri, node));
            }
        }
    }

    private void registerValidityArea(MdmTree tree, String subrootUri, NodeArea nodeArea) throws MdmException {
        String uri = null;

        if (nodeArea.location3GPP != null) {
            uri = MdmTree.makeUri(subrootUri, "3GPP_Location");
            tree.addInteriorNode(uri, null);
            register3GPPLocation(tree, uri, nodeArea.location3GPP);
        }

        if (nodeArea.locationWLAN != null) {
            uri = MdmTree.makeUri(subrootUri, "WLAN_Location");
            tree.addInteriorNode(uri, null);
            registerWLANLocation(tree, uri, nodeArea.locationWLAN);
        }

        if (nodeArea.locationGeo != null) {
            uri = MdmTree.makeUri(subrootUri, "Geo_Location");
            tree.addInteriorNode(uri, null);
            registerGeoLocation(tree, uri, nodeArea.locationGeo);
        }
    }

    private void register3GPPLocation(MdmTree tree, String subrootUri, Node3GPPLocation node3gppLocation)
            throws MdmException {
        List<Node3GPPLocation.X> nodes = node3gppLocation.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            Node3GPPLocation.X node = nodes.get(i);

            String uri = MdmTree.makeUri(subrootUri, nodeXId, Location3GPPGenericHandler.PLMN);
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new Location3GPPGenericHandler(uri, node));

            if (node.tac != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, Location3GPPGenericHandler.TAC);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new Location3GPPGenericHandler(uri, node));
            }

            if (node.lac != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, Location3GPPGenericHandler.LAC);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new Location3GPPGenericHandler(uri, node));
            }

            if (node.geranCI != AndsfMORoot.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, Location3GPPGenericHandler.GERAN_CI);
                Utilities.addLeafNodeChecked(tree, uri, "bin", null, null);
                tree.registerNodeIoHandler(uri, new Location3GPPGenericHandler(uri, node));
            }

            if (node.utranCI != AndsfMORoot.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, Location3GPPGenericHandler.UTRAN_CI);
                Utilities.addLeafNodeChecked(tree, uri, "bin", null, null);
                tree.registerNodeIoHandler(uri, new Location3GPPGenericHandler(uri, node));
            }

            if (node.eutraCI != AndsfMORoot.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, Location3GPPGenericHandler.EUTRA_CI);
                Utilities.addLeafNodeChecked(tree, uri, "bin", null, null);
                tree.registerNodeIoHandler(uri, new Location3GPPGenericHandler(uri, node));
            }
        }
    }

    private void registerWLANLocation(MdmTree tree, String subrootUri, NodeWLANLocation nodeWLANLocation) throws MdmException {
        List<NodeWLANLocation.X> nodes = nodeWLANLocation.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeWLANLocation.X node = nodes.get(i);

            String uri = null;
            if (node.hessid != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, LocationWlanHandler.HESSID);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new LocationWlanHandler(uri, node));
            }

            if (node.ssid != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, LocationWlanHandler.SSID);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new LocationWlanHandler(uri, node));
            }

            if (node.bssid != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, LocationWlanHandler.BSSID);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new LocationWlanHandler(uri, node));
            }
        }
    }

    private void registerGeoLocation(MdmTree tree, String subrootUri, NodeCircular nodeCircular) throws MdmException {
        List<NodeCircular.X> nodes = nodeCircular.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeCircular.X node = nodes.get(i);

            String uri = MdmTree.makeUri(subrootUri, nodeXId, LocationGeoHandler.ANCHOR_LATITUDE);
            Utilities.addLeafNodeChecked(tree, uri, "bin", null, null);
            tree.registerNodeIoHandler(uri, new LocationGeoHandler(uri, node));

            uri = MdmTree.makeUri(subrootUri, nodeXId, LocationGeoHandler.ANCHOR_LONGTITUDE);
            Utilities.addLeafNodeChecked(tree, uri, "bin", null, null);
            tree.registerNodeIoHandler(uri, new LocationGeoHandler(uri, node));

            uri = MdmTree.makeUri(subrootUri, nodeXId, LocationGeoHandler.RADIUS);
            Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
            tree.registerNodeIoHandler(uri, new LocationGeoHandler(uri, node));
        }
    }

    private void registerTimeOfDay(MdmTree tree, String subrootUri, NodeTimeOfDay nodeTimeOfDay) throws MdmException {
        List<NodeTimeOfDay.X> nodes = nodeTimeOfDay.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeTimeOfDay.X node = nodes.get(i);
            String uri = null;

            if (node.timeStart != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, TimeOfDayGenericHandler.TIME_START);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new TimeOfDayGenericHandler(uri, node));
            }

            if (node.timeStop != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, TimeOfDayGenericHandler.TIME_STOP);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new TimeOfDayGenericHandler(uri, node));
            }

            if (node.dateStart != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, TimeOfDayGenericHandler.DATE_START);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new TimeOfDayGenericHandler(uri, node));
            }

            if (node.dateStop != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, TimeOfDayGenericHandler.DATE_STOP);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new TimeOfDayGenericHandler(uri, node));
            }
        }
    }

    // <X>/ISRP/<X>/ForFlowBased/<X>/RoutingRule
    private void registerRoutingRule(MdmTree tree, String subrootUri, NodeRoutingRule nodeRoutingRule)
            throws MdmException {
        List<NodeRoutingRule.X> nodes = nodeRoutingRule.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeRoutingRule.X node = nodes.get(i);
            String uri = null;

            if (node.accessTechnology != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, RoutingRuleGenericHandler.ACCESS_TECHNOLOGY);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new RoutingRuleGenericHandler(uri, node));
            }

            if (node.accessId != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, RoutingRuleGenericHandler.ACCESS_ID);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new RoutingRuleGenericHandler(uri, node));
            }

            if (node.secondaryAccessId != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, RoutingRuleGenericHandler.SECONDARY_ACCESS_ID);
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new RoutingRuleGenericHandler(uri, node));
            }

            if (node.accessNetworkPriority != AndsfMO.ABSENT_FIELD_INT_VALUE) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, RoutingRuleGenericHandler.ACCESS_NETWORK_PRIORITY);
                Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
                tree.registerNodeIoHandler(uri, new RoutingRuleGenericHandler(uri, node));
            }
        }
    }

    private void registerForServiceBased(MdmTree tree, String subrootUri, NodeForServiceBased nodeForServiceBased)
            throws MdmException {
        List<NodeForServiceBased.X> nodes = nodeForServiceBased.x;
        for (int i = 0; i < nodes.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            NodeForServiceBased.X node = nodes.get(i);
            String uri = null;

            if (node.routingCriteria != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "RoutingCriteria");
                tree.addInteriorNode(uri, null);
                registerRoutingCriteria(tree, uri, node.routingCriteria);
            }

            uri = MdmTree.makeUri(subrootUri, nodeXId, "RoutingRule");
            tree.addInteriorNode(uri, null);
            registerRoutingRule(tree, uri, node.routingRule);

            if (node.apn != null) {
                uri = MdmTree.makeUri(subrootUri, nodeXId, "APN");
                Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
                tree.registerNodeIoHandler(uri, new FSBAPNHandler(uri, node));
            }

            uri = MdmTree.makeUri(subrootUri, nodeXId, "RoutingPriority");
            Utilities.addLeafNodeChecked(tree, uri, "int", null, null);
            tree.registerNodeIoHandler(uri, new FSBRulePriorityHandler(uri, node));
        }

    }

    private void registerForNonSeamlessOffload(MdmTree tree, String subrootUri, NodeForFlowBased node)
            throws MdmException {
        registerForFlowBased(tree, subrootUri, node);
    }

    private void registerUEProfile(MdmTree tree) throws MdmException {
        // 1. create new node
        final String subrootUri = MdmTree.makeUri(ROOT_URI, TopPath.UE_PROFILE);
        tree.addInteriorNode(subrootUri, null);
        tree.replaceACL(subrootUri, "Get=*");

        // 2. build nodes
        List<String> idList = mAndsfMORoot.ueProfile.listOSId;
        for (int i = 0; i < idList.size(); ++i) {
            String nodeXId = Integer.toString(i + 1);
            tree.addInteriorChildNode(subrootUri, nodeXId, null);
            String uri = MdmTree.makeUri(subrootUri, nodeXId, "OSId");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new UEProfileHandler(uri, mAndsfMORoot.ueProfile, i));
        }

        if (mAndsfMORoot.ueProfile.devCapability != null) {
            String uri = MdmTree.makeUri(subrootUri, "DevCapability");
            Utilities.addLeafNodeChecked(tree, uri, "chr", null, null);
            tree.registerNodeIoHandler(uri, new DevCapabilityHandler(uri, mAndsfMORoot.ueProfile));
        }
    }

    @Override
    public DispatchResult validateWapPushMessage(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public boolean checkPrerequisites() {
        return true;
    }
}
