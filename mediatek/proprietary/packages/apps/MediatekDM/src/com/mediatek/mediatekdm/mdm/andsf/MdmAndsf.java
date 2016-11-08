
package com.mediatek.mediatekdm.mdm.andsf;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;

public class MdmAndsf {
    static final class AlertType {
        public static final String UE_LOCATION = "urn:oma:at:ext-3gpp-andsf:1.0:ue_location";
        public static final String UE_PROFILE = "urn:oma:at:ext-3gpp-andsf:1.0:ue_profile";
        public static final String PROVISION = "urn:oma:at:ext-3gpp-andsf:1.0:provision";
        public static final String PROVISION_SINGLE_IF = "urn:oma:at:ext-3gpp-andsf:1.0:provision-single-if";
        public static final String PROVISION_MULTIPLE_IF = "urn:oma:at:ext-3gpp-andsf:1.0:provision-multiple-if";
        public static final String PROVISION_DISC_INFO = "urn:oma:at:ext-3gpp-andsf:1.0:provision-disc-info";
    }

    static final String ROOT_URI_TYPE = "urn:oma:mo:ext-3gpp-andsf:1.0";

    public static final String SESSION_INITIATOR_PREFIX = "MDM_ANDSF";
    public static final String SESSION_INITIATOR_UE_LOCATION = SESSION_INITIATOR_PREFIX + "|UE_LOCATION";
    public static final String SESSION_INITIATOR_UE_PROFILE = SESSION_INITIATOR_PREFIX + "|UE_PROFILE";
    public static final String SESSION_INITIATOR_PROVISION = SESSION_INITIATOR_PREFIX + "|PROVISION";
    public static final String SESSION_INITIATOR_PROVISION_SINGLE_IF = SESSION_INITIATOR_PREFIX
            + "|PROVISION_SINGLE_IF";
    public static final String SESSION_INITIATOR_PROVISION_MULTIPLE_IF = SESSION_INITIATOR_PREFIX
            + "|PROVISION_MULTIPLE_IF";
    public static final String SESSION_INITIATOR_PROVISION_DISC_INFO = SESSION_INITIATOR_PREFIX
            + "|PROVISION_DISC_INFO";

    private static final String TNDS_FORMAT = "xml";
    public static final String TNDS_TYPE = "application/vnd.syncml.dmtnds+xml";

    private static MdmAndsf sInstance;

    private final String mRootUri;
    private final MdmEngine mEngine;
    private final MdmTree mTree;

    public MdmAndsf(String rootUri) throws MdmException {
        synchronized (MdmAndsf.class) {
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[ANDSF] Create ANDSF MO instance with uri " + rootUri);

            if (sInstance != null) {
                throw new MdmException(MdmError.INTERNAL);
            }

            mEngine = MdmEngine.getInstance();
            mTree = new MdmTree();
            if (rootUri.charAt(rootUri.length() - 1) == '/') {
                mRootUri = rootUri.substring(0, rootUri.length() - 1);
            } else {
                mRootUri = rootUri;
            }
            sInstance = this;
        }
    }

    public void triggerUELocationSession() throws MdmException {
        mEngine.triggerGenericAlertSession(
                null,
                TNDS_FORMAT,
                AlertType.UE_LOCATION,
                null,
                mRootUri,
                null,
                mTree.buildTndsString(MdmTree.makeUri(mRootUri, "UE_Location")),
                null,
                new SessionInitiator() {
                    public String getId() {
                        return SESSION_INITIATOR_UE_LOCATION;
                    }
                });
    }

    public void triggerUEProfileSession() throws MdmException {
        mEngine.triggerGenericAlertSession(
                null,
                TNDS_FORMAT,
                AlertType.UE_PROFILE,
                null,
                mRootUri,
                null,
                mTree.buildTndsString(MdmTree.makeUri(mRootUri, "ISRP")),
                null,
                new SessionInitiator() {
                    public String getId() {
                        return SESSION_INITIATOR_UE_PROFILE;
                    }
                });
    }

    public void triggerProvisionSession() throws MdmException {
        mEngine.triggerGenericAlertSession(
                null,
                null,
                AlertType.PROVISION,
                null,
                mRootUri,
                null,
                null,
                null,
                new SessionInitiator() {
                    public String getId() {
                        return SESSION_INITIATOR_PROVISION;
                    }
                });
    }

    public void triggerProvisionSingleIfSession() throws MdmException {
        mEngine.triggerGenericAlertSession(
                null,
                null,
                AlertType.PROVISION_SINGLE_IF,
                null,
                mRootUri,
                null,
                null,
                null,
                new SessionInitiator() {
                    public String getId() {
                        return SESSION_INITIATOR_PROVISION_SINGLE_IF;
                    }
                });
    }

    public void triggerProvisionMultipleIfSession() throws MdmException {
        mEngine.triggerGenericAlertSession(
                null,
                null,
                AlertType.PROVISION_MULTIPLE_IF,
                null,
                mRootUri,
                null,
                null,
                null,
                new SessionInitiator() {
                    public String getId() {
                        return SESSION_INITIATOR_PROVISION_MULTIPLE_IF;
                    }
                });
    }

    public void triggerProvisionDiscInfoSession() throws MdmException {
        mEngine.triggerGenericAlertSession(
                null,
                null,
                AlertType.PROVISION_DISC_INFO,
                null,
                mRootUri,
                null,
                null,
                null,
                new SessionInitiator() {
                    public String getId() {
                        return SESSION_INITIATOR_PROVISION_DISC_INFO;
                    }
                });
    }

    public void destroy() {
        synchronized (MdmAndsf.class) {
            MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG, "[ANDSF] ANDSF MO instance with uri " + mRootUri + "destroyed.");
            sInstance = null;
        }
    }
}
