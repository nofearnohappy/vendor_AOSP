
package com.mediatek.mediatekdm.volte;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsConfig.ConfigConstants;
import com.android.ims.mo.ImsIcsi;
import com.android.ims.mo.ImsLboPcscf;
import com.android.ims.mo.ImsPhoneCtx;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;
import com.mediatek.mediatekdm.util.Utilities;
import com.mediatek.mediatekdm.volte.imsio.BooleanHandler;
import com.mediatek.mediatekdm.volte.imsio.ICSIHandler;
import com.mediatek.mediatekdm.volte.imsio.ICSIModeHandler;
import com.mediatek.mediatekdm.volte.imsio.IntegerHandler;
import com.mediatek.mediatekdm.volte.imsio.PCSCFAddressHandler;
import com.mediatek.mediatekdm.volte.imsio.PCSCFAddressTypeHandler;
import com.mediatek.mediatekdm.volte.imsio.PhoneContextHandler;
import com.mediatek.mediatekdm.volte.imsio.PhoneContextIdentityHandler;
import com.mediatek.mediatekdm.volte.imsio.RCSAuthTypeHandler;
import com.mediatek.mediatekdm.volte.imsio.RCSRealmHandler;
import com.mediatek.mediatekdm.volte.imsio.RCSUserNameHandler;
import com.mediatek.mediatekdm.volte.imsio.RCSUserPwdHandler;
import com.mediatek.mediatekdm.volte.imsio.StringHandler;

public class IMSComponent implements IDmComponent {

    static final String NAME = "IMS";
    private ImsManager mImsManager;
    private static final String ROOT_URI = "./IMSMO";
    private static final String PUBLIC_USER_IDENTITY_LIST_PATH = MdmTree.makeUri(ROOT_URI, "Public_user_identity_List");
    private static final String ICSI_LIST_PATH = MdmTree.makeUri(ROOT_URI, "ICSI_List");
    private static final String LBO_PCSCF_ADDRESS_PATH = MdmTree.makeUri(ROOT_URI, "LBO_P-CSCF_Address");
    private static final String PHONE_CONTEXT_LIST_PATH = MdmTree.makeUri(ROOT_URI, "PhoneContext_List");
    private static final String RCS_PATH = MdmTree.makeUri(ROOT_URI, "Ext", "RCS");

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        return false;
    }

    @Override
    public void attach(DmService service) {
        mImsManager = ImsManager.getInstance(service, SubscriptionManager.getDefaultSubId());
        Log.d(NAME, "attach: mImsManager = " + mImsManager);
    }

    @Override
    public void detach(DmService service) {
        mImsManager = null;
    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        return DispatchResult.IGNORE;
    }

    @Override
    public SessionHandler dispatchSessionStateChange(
            SessionType type,
            SessionState state,
            int lastError,
            SessionInitiator initiator,
            DmOperation operation) {
        return null;
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
        ImsConfig imsConfig = null;
        try {
            imsConfig = mImsManager.getConfigInterface();
        } catch (ImsException e) {
            e.printStackTrace();
        }

        // Normal Nodes
        registerNodeIoHandler(tree, "P-CSCF_Address", ConfigConstants.IMS_MO_PCSCF, true, StringHandler.class);
        registerNodeIoHandler(tree, "Timer_T1", ConfigConstants.SIP_T1_TIMER, true, IntegerHandler.class);
        registerNodeIoHandler(tree, "Timer_T2", ConfigConstants.SIP_T2_TIMER, true, IntegerHandler.class);
        registerNodeIoHandler(tree, "Timer_T4", ConfigConstants.SIP_TF_TIMER, true, IntegerHandler.class);
        registerNodeIoHandler(tree, "Private_user_identity", ConfigConstants.IMS_MO_IMPI, false, StringHandler.class);
        registerNodeIoHandler(tree, "Home_network_domain_name", ConfigConstants.IMS_MO_DOMAIN, false, StringHandler.class);
        registerNodeIoHandler(tree, "Resource_Allocation_Mode", ConfigConstants.IMS_MO_RESOURCE, true,
                BooleanHandler.class);
        registerNodeIoHandler(tree, "Voice_Domain_Preference_E_UTRAN", ConfigConstants.IMS_MO_VOICE_E, true,
                IntegerHandler.class);
        registerNodeIoHandler(tree, "SMS_Over_IP_Networks_Indication", ConfigConstants.IMS_MO_SMS, true,
                BooleanHandler.class);
        registerNodeIoHandler(tree, "Keep_Alive_Enabled", ConfigConstants.IMS_MO_KEEPALIVE, true, BooleanHandler.class);
        registerNodeIoHandler(tree, "Voice_Domain_Preference_UTRAN", ConfigConstants.IMS_MO_VOICE_U, true,
                IntegerHandler.class);
        registerNodeIoHandler(tree, "Mobility_Management_IMS_Voice_Termination", ConfigConstants.IMS_MO_MOBILITY, true,
                BooleanHandler.class);
        registerNodeIoHandler(tree, "RegRetryBaseTime", ConfigConstants.IMS_MO_REG_BASE, true, IntegerHandler.class);
        registerNodeIoHandler(tree, "RegRetryMaxTime", ConfigConstants.IMS_MO_REG_MAX, true, IntegerHandler.class);
        // Public User Identity List
        try {
            // 1. clear old nodes
            for (String child : tree.listChildren(PUBLIC_USER_IDENTITY_LIST_PATH)) {
                tree.deleteNode(MdmTree.makeUri(PUBLIC_USER_IDENTITY_LIST_PATH, child));
            }
            // 2. create new nodes
            String[] publicUserIdentities = imsConfig.getMasterStringArrayValue(ConfigConstants.IMS_MO_IMPU);
            for (int i = 0; i < publicUserIdentities.length; ++i) {
                Log.d(TAG.NODEIOHANDLER, "publicUserIdentities[i] is " + publicUserIdentities[i]);
                if (publicUserIdentities[i] == null) {
                    continue;
                }
                String nodeXId = Integer.toString(i + 1);
                tree.addInteriorChildNode(PUBLIC_USER_IDENTITY_LIST_PATH, nodeXId, null);
                String leafUri = MdmTree.makeUri(PUBLIC_USER_IDENTITY_LIST_PATH, nodeXId, "Public_user_identity");
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "chr",
                        null,
                        publicUserIdentities[i].getBytes());
                tree.replaceACL(leafUri, "Get=*");
            }
        } catch (MdmException e) {
            throw new Error(e);
        } catch (ImsException e) {
            throw new Error(e);
        }

        // ICSI List
        try {
            // 1. clear old nodes
            for (String child : tree.listChildren(ICSI_LIST_PATH)) {
                tree.deleteNode(MdmTree.makeUri(ICSI_LIST_PATH, child));
            }
            // 2. create new nodes & register handlers
            ImsIcsi[] icsiList = imsConfig.getMasterIcsiValue();
            for (int i = 0; i < icsiList.length; ++i) {
                if (icsiList[i] == null) {
                    continue;
                }
                String nodeXId = Integer.toString(i + 1);
                tree.addInteriorChildNode(ICSI_LIST_PATH, nodeXId, null);
                String leafUri = MdmTree.makeUri(ICSI_LIST_PATH, nodeXId, "ICSI");
                String data = icsiList[i].getIcsi();
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "chr",
                        null,
                        (data == null ? null : data.getBytes()));
                tree.replaceACL(leafUri, "Get=*&Replace=*");
                tree.registerNodeIoHandler(leafUri, new ICSIHandler(leafUri, mImsManager, i));
                leafUri = MdmTree.makeUri(ICSI_LIST_PATH, nodeXId, "ICSI_Resource_Allocation_Mode");
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "bool",
                        null,
                        (icsiList[i].getIsAllocated() ? "1" : "0").getBytes());
                tree.replaceACL(leafUri, "Get=*&Replace=*");
                tree.registerNodeIoHandler(leafUri, new ICSIModeHandler(leafUri, mImsManager, i));
            }
        } catch (MdmException e) {
            throw new Error(e);
        } catch (ImsException e) {
            throw new Error(e);
        }

        // LBO P-CSCF Address
        try {
            // 1. clear old nodes
            for (String child : tree.listChildren(LBO_PCSCF_ADDRESS_PATH)) {
                tree.deleteNode(MdmTree.makeUri(LBO_PCSCF_ADDRESS_PATH, child));
            }
            // 2. create new nodes & register handlers
            ImsLboPcscf[] addressList = imsConfig.getMasterLboPcscfValue();
            for (int i = 0; i < addressList.length; ++i) {
                if (addressList[i] == null) {
                    continue;
                }
                String nodeXId = Integer.toString(i + 1);
                tree.addInteriorChildNode(LBO_PCSCF_ADDRESS_PATH, nodeXId, null);
                String leafUri = MdmTree.makeUri(LBO_PCSCF_ADDRESS_PATH, nodeXId, "Address");
                String data = addressList[i].getLboPcscfAddress();
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "chr",
                        null,
                        (data == null ? null : data.getBytes()));
                tree.replaceACL(leafUri, "Get=*&Replace=*");
                tree.registerNodeIoHandler(leafUri, new PCSCFAddressHandler(leafUri, mImsManager, i));
                leafUri = MdmTree.makeUri(LBO_PCSCF_ADDRESS_PATH, nodeXId, "AddressType");
                data = addressList[i].getLboPcscfAddressType();
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "chr",
                        null,
                        (data == null ? null : data.getBytes()));
                tree.replaceACL(leafUri, "Get=*&Replace=*");
                tree.registerNodeIoHandler(leafUri, new PCSCFAddressTypeHandler(leafUri, mImsManager, i));
            }
        } catch (MdmException e) {
            throw new Error(e);
        } catch (ImsException e) {
            throw new Error(e);
        }

        // Phone Context List
        try {
            // 1. clear old nodes
            for (String child : tree.listChildren(PHONE_CONTEXT_LIST_PATH)) {
                tree.deleteNode(MdmTree.makeUri(PHONE_CONTEXT_LIST_PATH, child));
            }
            // 2. create new nodes & register handlers
            ImsPhoneCtx[] contextList = imsConfig.getMasterImsPhoneCtxValue();
            for (int i = 0; i < contextList.length; ++i) {
                if (contextList[i] == null) {
                    continue;
                }
                String nodeXId = Integer.toString(i + 1);
                tree.addInteriorChildNode(PHONE_CONTEXT_LIST_PATH, nodeXId, null);
                String leafUri = MdmTree.makeUri(PHONE_CONTEXT_LIST_PATH, nodeXId, "PhoneContext");
                String data = contextList[i].getPhoneCtx();
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "chr",
                        null,
                        (data == null ? null : data.getBytes()));
                tree.replaceACL(leafUri, "Get=*&Replace=*");
                tree.registerNodeIoHandler(leafUri, new PhoneContextHandler(leafUri, mImsManager, i));
                leafUri = MdmTree.makeUri(PHONE_CONTEXT_LIST_PATH, nodeXId, "Public_user_identity");
                data = contextList[i].getPhoneCtxIpuis()[0];
                Utilities.addLeafNodeChecked(
                        tree,
                        leafUri,
                        "chr",
                        null,
                        (data == null ? null : data.getBytes()));
                tree.replaceACL(leafUri, "Get=*&Replace=*");
                tree.registerNodeIoHandler(leafUri, new PhoneContextIdentityHandler(leafUri, mImsManager, i));
            }
        } catch (MdmException e) {
            throw new Error(e);
        } catch (ImsException e) {
            throw new Error(e);
        }

        // RCS-related
        try {
            String uri = MdmTree.makeUri(RCS_PATH, "AuthType");
            tree.registerNodeIoHandler(uri, new RCSAuthTypeHandler(uri, mImsManager));
            uri = MdmTree.makeUri(RCS_PATH, "Realm");
            tree.registerNodeIoHandler(uri, new RCSRealmHandler(uri, mImsManager));
            uri = MdmTree.makeUri(RCS_PATH, "UserName");
            tree.registerNodeIoHandler(uri, new RCSUserNameHandler(uri, mImsManager));
            uri = MdmTree.makeUri(RCS_PATH, "UserPwd");
            tree.registerNodeIoHandler(uri, new RCSUserPwdHandler(uri, mImsManager));
        } catch (MdmException e) {
            throw new Error(e);
        }

        // Write back
        try {
            tree.writeToPersistentStorage();
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private void registerNodeIoHandler(MdmTree tree, String uri, int imsId, boolean writable, Class type) {
        uri = MdmTree.makeUri(ROOT_URI, uri);
        try {
            if (type == StringHandler.class) {
                tree.registerNodeIoHandler(uri, new StringHandler(uri, imsId, mImsManager, writable));
            } else if (type == BooleanHandler.class) {
                tree.registerNodeIoHandler(uri, new BooleanHandler(uri, imsId, mImsManager, writable));
            } else if (type == IntegerHandler.class) {
                tree.registerNodeIoHandler(uri, new IntegerHandler(uri, imsId, mImsManager, writable));
            } else {
                throw new Error("Invalid handler class: " + type);
            }
        } catch (MdmException e) {
            throw new Error(e);
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
