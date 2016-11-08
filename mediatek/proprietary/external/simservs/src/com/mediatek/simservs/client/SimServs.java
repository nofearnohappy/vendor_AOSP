
package com.mediatek.simservs.client;

import android.content.Context;
import android.net.Network;
import android.os.SystemProperties;
import android.util.Log;

import com.mediatek.simservs.capability.BarringServiceCapability;
import com.mediatek.simservs.capability.DiversionServiceCapability;
import com.mediatek.xcap.client.XcapDebugParam;
import com.mediatek.xcap.client.uri.XcapUri;
import com.mediatek.xcap.client.uri.XcapUri.XcapDocumentSelector;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simservs class.
 */
public class SimServs {
    public static boolean sDebug = false;
    public static final String TAG = "SimServs";

    public static final int OPERATOR_DEFAULT = 0;
    public static final int OPERATOR_OP01 = OPERATOR_DEFAULT + 1;
    public static final int OPERATOR_OP07 = OPERATOR_DEFAULT + 2;

    public static final boolean LIB_CONFIG_MULTIPLE_RULE_CONDITIONS = true;

    public static final int CARDTYPE_UNSPECIFIED = 0;
    public static final int CARDTYPE_USIM = 1;
    public static final int CARDTYPE_ISIM = 2;

    public static final String SIMSERVS_FILENAME = "simservs.xml";
    public static final String AUID_SIMSERVS = "simservs.ngn.etsi.org";

    public static SimServs sInstance = null;
    private int mCardType = CARDTYPE_UNSPECIFIED;
    private static String sXcapRoot = null;
    private static String sXui = null;
    private String mIntendedId = null;
    private String mImsi = null;
    private String mMnc = null;
    private String mMcc = null;
    private String mImpi = null;
    private String mImpu = null;
    private String mUsername = null;
    private String mPassword = null;
    public XcapDocumentSelector mDocumentSelector;
    public URI mDocumentUri;
    public XcapUri mXcapUri;
    private static XcapDebugParam sXcapDebugParam;
    private Context mContext;
    /**
     * Constructor.
     *
     */
    public SimServs() {
        String debugProperty = SystemProperties.get("mediatek.simserv.debug", "0");
        if (debugProperty.equals("1")) {
            sDebug = true;
            Log.d(TAG, "sDebug enabled.");
        }
    }

    /**
     * Get SimServs instance.
     *
     * @return Simservs instance
     */
    static public SimServs getInstance() {
        if (sInstance == null) {
            sInstance = new SimServs();
        }

        initializeDebugParam();
        return sInstance;
    }

    /**
     * Get XcapDebugParam.
     *
     * @return XcapDebugParam
     */
    public XcapDebugParam getXcapDebugParam() {
        return sXcapDebugParam;
    }

    /**
     * Set XCAP root URL.
     *
     * @param xcapRoot XCAP root directory URI
     */
    public void setXcapRoot(String xcapRoot) {
        sXcapRoot = xcapRoot;
        try {
            buildDocumentUri();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            sXcapRoot = null;
        }
    }

    /**
     * Set XCAP root URL by IMPI.
     *
     * @param impi IMPI
     */
    public void setXcapRootByImpi(String impi) {
        mCardType = CARDTYPE_ISIM;
        mImpi = impi;
        try {
            buildRootUri();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set XCAP root URL by PLMN.
     *
     * @param mcc MCC
     * @param mnc MNC
     */
    public void setXcapRootByMccMnc(String mcc, String mnc) {
        mCardType = CARDTYPE_USIM;
        mMcc = mcc;
        mMnc = mnc;
        try {
            buildRootUri();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void setXui(String xui) {
        sXui = xui;
    }

    /**
     * Set XUI by IMPU.
     *
     * @param impu IMPU
     */
    public void setXuiByImpu(String impu) {
        mCardType = CARDTYPE_ISIM;
        mImpu = impu;
        sXui = impu;
    }

    /**
     * Set XUI by IMSI and PLMN.
     *
     * @param imsi IMSI
     * @param mcc  MCC
     * @param mnc  MNC
     */
    public void setXuiByImsiMccMnc(String imsi, String mcc, String mnc) {
        mCardType = CARDTYPE_USIM;
        mImsi = imsi;
        mMcc = mcc;
        mMnc = mnc;

        sXui = String.format("sip:%s@ims.mnc%03d.mcc%03d.3gppnetwork.org", imsi,
                             Integer.parseInt(mnc), Integer.parseInt(mcc));
    }

    public void setIntendedId(String intendedId) {
        mIntendedId = intendedId;
    }

    public void setHttpCredential(String username, String password){
        // TODO: remove in the future
    }

    /**
     * Set to use HTTP protocol scheme.
     *
     * @param value true/false
     */
    public void setUseHttpProtocolScheme(boolean value) {
        if (value) {
            System.setProperty("xcap.protocol", "http");
        } else {
            System.setProperty("xcap.protocol", "https");
        }
    }

    /**
     * Set content-type while updating XML element.
     *
     * In 3GPP spec, "application/xcap-el+xml" is used.
     * Som operator will have it's own specific content-type,
     * ex: "application/vnd.etsi.simservs+xml"
     *
     * @param specdefined true to use "application/xcap-el+xml", false to use assigned type.
     * @param type content-type
     */
    public void setElementUpdateContentType(boolean specdefined, String type) {
        if (specdefined) {
            System.setProperty("xcap.putelcontenttype", "application/xcap-el+xml");
        } else {
            System.setProperty("xcap.putelcontenttype", type);
        }
    }

    /**
     * Set to handle HTTP 409 error message content.
     *
     * @param value true/false
     */
    public void setHandleError409(boolean value) {
        if (value) {
            System.setProperty("xcap.handl409", "true");
        } else {
            System.setProperty("xcap.handl409", "false");
        }
    }

    /**
     * Set to fill complete ForwardTo elements according to 3GPP TS 24.604
     *
     * Usually only <target>, phone number,  will be used in <forward-to> element,
     * Some operaters require to fill complete elements in <forward-to>.
     *
     * @param value true/false
     */
    public void setFillCompleteForwardTo(boolean value) {
        if (value) {
            System.setProperty("xcap.completeforwardto", "true");
        } else {
            System.setProperty("xcap.completeforwardto", "false");
        }
    }

    /**
     * Set to add namespace prefix "ss:".
     * as namespace prefix for http://uri.etsi.org/ngn/params/xml/simservs/xcap
     *
     * @param value true/false
     */
    public void setXcapNSPrefixSS(boolean value) {
        if (value) {
            System.setProperty("xcap.ns.ss", "true");
        } else {
            System.setProperty("xcap.ns.ss", "false");
        }
    }

    public String getXcapRoot() {
        return sXcapRoot;
    }

    public String getXui() {
        return sXui;
    }

    public String getIntendedId() {
        return mIntendedId;
    }

    /**
     * Set context.
     *
     * @param ctxt context to set
     */
    public void setContext(Context ctxt) {
        mContext = ctxt;
    }

    /**
     * Configure operator-specific requirement parameters.
     *
     * @param operator operator choice
     */
    public void setOperator(int operator) {
        switch(operator) {
            case OPERATOR_OP01:
                System.setProperty("xcap.op", "01");
                System.setProperty("xcap.protocol", "http");
                System.setProperty("xcap.putelcontenttype", "application/vnd.etsi.simservs+xml");
                System.setProperty("xcap.handl409", "false");
                break;
            case OPERATOR_OP07:
                System.setProperty("xcap.op", "07");
                System.setProperty("xcap.protocol", "https");
                System.setProperty("xcap.putelcontenttype", "application/xcap-el+xml");
                System.setProperty("xcap.handl409", "true");
                break;
            case OPERATOR_DEFAULT:
            default:
                System.setProperty("xcap.op", "00");
                System.setProperty("xcap.protocol", "https");
                System.setProperty("xcap.putelcontenttype", "application/xcap-el+xml");
                System.setProperty("xcap.handl409", "false");
                break;
        }
    }

    /**
     * Initialization debug utility.
     */
    static private void initializeDebugParam() {
        sXcapDebugParam = XcapDebugParam.getInstance();
        sXcapDebugParam.load();
        sXcapRoot = null;
        sXui = null;

        String xcapRoot = sXcapDebugParam.getXcapRoot();
        //systemproperty ex: http://xcap.ims.msg.t-mobile.com:8090/

        if (xcapRoot != null && !xcapRoot.isEmpty()) {
            sXcapRoot = xcapRoot;
        }

        String xui = sXcapDebugParam.getXcapXui();

        if (xui != null && !xui.isEmpty()) {
            sXui = xui;
        }
    }

    /**
     * Get domain from IMPI.
     */
    private String getImpiDomain() {
        if (mImpi != null && !mImpi.isEmpty() && mImpi.contains("@")) {
            String[] temp;
            String delimiter = "@";
            temp = mImpi.split(delimiter);
            return temp[1];
        } else {
            return null;
        }
    }

    /**
     * Build the XCAP root URI according to TS 23.003 13.9.
     *
     * @throws URISyntaxException
     */
    private void buildRootUri() throws URISyntaxException {
        StringBuilder xcapRoot = new StringBuilder();
        Log.d("Simservs", "xcap.protocol=" + System.getProperty("xcap.protocol"));

        String protocol = System.getProperty("xcap.protocol", "https");

        if (mCardType == CARDTYPE_USIM) {
            xcapRoot.append(protocol + "://xcap.ims.mnc")
                .append(mMnc)
                .append(".mcc")
                .append(mMcc)
                .append(".pub.3gppnetwork.org");
        } else if (mCardType == CARDTYPE_ISIM) {
            if (mImpi != null && mImpi.endsWith("3gppnetwork.org")) {
                String domain = getImpiDomain();
                xcapRoot.append(protocol + "://xcap.");
                xcapRoot.append(domain.substring(0, domain.indexOf(".3gppnetwork.org")));
                if (mImpi.contains(".pub")) {
                    xcapRoot.append(".3gppnetwork.org");
                } else {
                    xcapRoot.append(".pub.3gppnetwork.org");
                }
            } else {
                String domain = getImpiDomain();
                if (domain == null) {
                    return;
                }
                xcapRoot.append(protocol + "://xcap.")
                        .append(domain);
            }
        } else {
            return;
        }

        xcapRoot.append("/");
        sXcapRoot = xcapRoot.toString();
        buildDocumentUri();
    }

    /**
     * Build document URI.
     *
     * @throws URISyntaxException if URI syntax error
     */
    public void buildDocumentUri() throws URISyntaxException {
        String xcapDocumentName = sXcapDebugParam.getXcapDocumentName();
        mDocumentSelector = new XcapDocumentSelector(
                AUID_SIMSERVS, sXui, (xcapDocumentName != null && !xcapDocumentName.isEmpty()) ?
                xcapDocumentName : SIMSERVS_FILENAME);
        Log.d(TAG, "document selector is " + mDocumentSelector.toString());
        //[Fix bug by mtk01411 2013-1003]mXcapUri is member of class SimServs
        //XcapUri mXcapUri = new XcapUri(); //Only new instance for local variable mXcapUri
        //instead of class member mXcapUri
        mXcapUri = new XcapUri();
        Log.d(TAG, "buildDocumentUri():Create instance for mXcapUri");
        mXcapUri.setXcapRoot(sXcapRoot).setDocumentSelector(mDocumentSelector);

        mDocumentUri = mXcapUri.toURI();
    }

    public String getDocumentUri() {
        return mDocumentUri.toString();
    }

    /**
     * Get a CommunicationWaiting instance.
     *
     * @param  syncInstance not use
     * @return CommunicationWaiting instance
     * @throws Exception if error
     */
    public CommunicationWaiting getCommunicationWaiting(boolean syncInstance) throws Exception {
        CommunicationWaiting cw = new CommunicationWaiting(mXcapUri, null, mIntendedId);
        if (mContext != null) {
            cw.setContext(mContext);
        }

        cw.loadConfiguration();
        return cw;
    }

    /**
     * Get a CommunicationWaiting instance.
     *
     * @param  syncInstance not use
     * @param network dedicated network
     * @return CommunicationWaiting instance
     * @throws Exception if error
     */
    public CommunicationWaiting getCommunicationWaiting(boolean syncInstance, Network network)
            throws Exception {
        CommunicationWaiting cw = new CommunicationWaiting(mXcapUri, null, mIntendedId);
        if (network != null) {
            cw.setNetwork(network);
        }

        if (mContext != null) {
            cw.setContext(mContext);
        }

        cw.loadConfiguration();
        return cw;
    }

    /**
     * Get a CommunicationWaiting instance.
     * For test purpose.
     *
     * @param documentUri       XCAP document URI
     * @return CommunicationWaiting instance
     * @throws Exception if error
     */
    public CommunicationWaiting getCommunicationWaiting(XcapUri documentUri) throws Exception {
        return new CommunicationWaiting(documentUri, null, mIntendedId);
    }

    /**
     * Get a OriginatingIdentityPresentation instance.
     *
     * @param  syncInstance not use
     * @return OriginatingIdentityPresentation instance
     * @throws Exception if error
     */
    public OriginatingIdentityPresentation getOriginatingIdentityPresentation(boolean syncInstance)
            throws Exception {
        OriginatingIdentityPresentation oip = new OriginatingIdentityPresentation(mXcapUri, null,
                mIntendedId);
        if (mContext != null) {
            oip.setContext(mContext);
        }

        oip.loadConfiguration();
        return oip;
    }

    /**
     * Get a OriginatingIdentityPresentation instance.
     *
     * @param  syncInstance not use
     * @param network dedicated network
     * @return OriginatingIdentityPresentation instance
     * @throws Exception if error
     */
    public OriginatingIdentityPresentation getOriginatingIdentityPresentation(boolean syncInstance,
            Network network) throws Exception {
        OriginatingIdentityPresentation oip = new OriginatingIdentityPresentation(mXcapUri, null,
                mIntendedId);
        if (network != null) {
            oip.setNetwork(network);
        }

        if (mContext != null) {
            oip.setContext(mContext);
        }

        oip.loadConfiguration();
        return oip;
    }

    /**
     * Get a TerminatingIdentityPresentation instance.
     *
     * @param  syncInstance not use
     * @return TerminatingIdentityPresentation instance
     * @throws Exception if error
     */
    public TerminatingIdentityPresentation getTerminatingIdentityPresentation(boolean syncInstance)
            throws Exception {
        TerminatingIdentityPresentation tip = new TerminatingIdentityPresentation(mXcapUri, null,
                mIntendedId);

        if (mContext != null) {
            tip.setContext(mContext);
        }

        tip.loadConfiguration();
        return tip;
    }

    /**
     * Get a TerminatingIdentityPresentation instance.
     *
     * @param  syncInstance not use
     * @param network dedicated network
     * @return TerminatingIdentityPresentation instance
     * @throws Exception if error
     */
    public TerminatingIdentityPresentation getTerminatingIdentityPresentation(boolean syncInstance,
            Network network) throws Exception {
        TerminatingIdentityPresentation tip = new TerminatingIdentityPresentation(mXcapUri, null,
                mIntendedId);
        if (network != null) {
            tip.setNetwork(network);
        }

        if (mContext != null) {
            tip.setContext(mContext);
        }

        tip.loadConfiguration();
        return tip;
    }

    /**
     * Get a TerminatingIdentityPresentation instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     *
     * @return OriginatingIdentityPresentation instance
     * @throws Exception if error
     */
    public OriginatingIdentityPresentation getOriginatingIdentityPresentation(
            XcapUri documentUri) throws Exception {
        return new OriginatingIdentityPresentation(documentUri, null, mIntendedId);
    }

    /**
     * Get a TerminatingIdentityPresentation instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return TerminatingIdentityPresentation instance
     * @throws Exception if error
     */
    public TerminatingIdentityPresentation getTerminatingIdentityPresentation(
            XcapUri documentUri) throws Exception {
        return new TerminatingIdentityPresentation(documentUri, null, mIntendedId);
    }

    /**
     * Get a OriginatingIdentityPresentationRestriction instance.
     *
     * @param  syncInstance not use
     * @return OriginatingIdentityPresentationRestriction instance
     * @throws Exception if error
     */
    public OriginatingIdentityPresentationRestriction getOriginatingIdentityPresentationRestriction(
            boolean syncInstance) throws Exception {
        OriginatingIdentityPresentationRestriction oip =
                new OriginatingIdentityPresentationRestriction(mXcapUri, null, mIntendedId);
        if (mContext != null) {
            oip.setContext(mContext);
        }

        oip.loadConfiguration();
        return oip;
    }

    /**
     * Get a OriginatingIdentityPresentationRestriction instance.
     *
     * @param  syncInstance not use
     * @param network dedicated network
     * @return OriginatingIdentityPresentationRestriction instance
     * @throws Exception if error
     */
    public OriginatingIdentityPresentationRestriction getOriginatingIdentityPresentationRestriction(
            boolean syncInstance, Network network) throws Exception {
        OriginatingIdentityPresentationRestriction oipr =
                new OriginatingIdentityPresentationRestriction(mXcapUri, null, mIntendedId);
        if (network != null) {
            oipr.setNetwork(network);
        }

        if (mContext != null) {
            oipr.setContext(mContext);
        }

        oipr.loadConfiguration();
        return oipr;
    }

    /**
     * Get a TerminatingIdentityPresentationRestriction instance.
     *
     * @param  syncInstance not use
     * @return TerminatingIdentityPresentationRestriction instance
     * @throws Exception if error
     */
    public TerminatingIdentityPresentationRestriction getTerminatingIdentityPresentationRestriction(
            boolean syncInstance) throws Exception {
        TerminatingIdentityPresentationRestriction tipr =
                new TerminatingIdentityPresentationRestriction(mXcapUri, null, mIntendedId);

        if (mContext != null) {
            tipr.setContext(mContext);
        }

        tipr.loadConfiguration();
        return tipr;
    }

    /**
     * Get a TerminatingIdentityPresentationRestriction instance.
     *
     * @param  syncInstance not use
     * @param network dedicated network
     * @return TerminatingIdentityPresentationRestriction instance
     * @throws Exception if error
     */
    public TerminatingIdentityPresentationRestriction getTerminatingIdentityPresentationRestriction(
            boolean syncInstance, Network network) throws Exception {
        TerminatingIdentityPresentationRestriction tipr =
                new TerminatingIdentityPresentationRestriction(mXcapUri, null, mIntendedId);
        if (network != null) {
            tipr.setNetwork(network);
        }

        if (mContext != null) {
            tipr.setContext(mContext);
        }

        tipr.loadConfiguration();
        return tipr;
    }

    /**
     * Get a OriginatingIdentityPresentationRestriction instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return OriginatingIdentityPresentationRestriction instance
     * @throws Exception if error
     */
    public OriginatingIdentityPresentationRestriction getOriginatingIdentityPresentationRestriction(
            XcapUri documentUri) throws Exception {
        return new OriginatingIdentityPresentationRestriction(documentUri, null, mIntendedId);
    }

    /**
     * Get a TerminatingIdentityPresentationRestriction instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return TerminatingIdentityPresentationRestriction instance
     * @throws Exception if error
     */
    public TerminatingIdentityPresentationRestriction getTerminatingIdentityPresentationRestriction(
            XcapUri documentUri) throws Exception {
        return new TerminatingIdentityPresentationRestriction(documentUri, null, mIntendedId);
    }

    /**
     * Get a CommunicationDiversion instance.
     *
     * @param syncInstance not use
     * @return CommunicationDiversion instance
     * @throws Exception if error
     */
    public CommunicationDiversion getCommunicationDiversion(boolean syncInstance) throws
            Exception {
        CommunicationDiversion cd = new CommunicationDiversion(mXcapUri, null, mIntendedId);

        if (mContext != null) {
            cd.setContext(mContext);
        }

        cd.loadConfiguration();
        return cd;
    }

    /**
     * Get a CommunicationDiversion instance with specific network.
     *
     * @param syncInstance not use
     * @param network dedicated network
     * @return CommunicationDiversion instance
     * @throws Exception if error
     */
    public CommunicationDiversion getCommunicationDiversion(boolean syncInstance,
            Network network) throws Exception {
        CommunicationDiversion cd = new CommunicationDiversion(mXcapUri, null, mIntendedId);
        if (network != null) {
            cd.setNetwork(network);
        }

        if (mContext != null) {
            cd.setContext(mContext);
        }
        cd.loadConfiguration();
        return cd;
    }

    /**
     * Get a CommunicationDiversion instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return CommunicationDiversion instance
     * @throws Exception if error
     */
    public CommunicationDiversion getCommunicationDiversion(
            XcapUri documentUri) throws Exception {
        return new CommunicationDiversion(documentUri, null, mIntendedId);
    }

    /**
     * Get a IncomingCommunicationBarring instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return IncomingCommunicationBarring instance
     * @throws Exception if error
     */
    public IncomingCommunicationBarring getIncomingCommunicationBarring(
            XcapUri documentUri) throws Exception {
        return new IncomingCommunicationBarring(documentUri, null, mIntendedId);
    }

    /**
     * Get a IncomingCommunicationBarring instance.
     *
     * @param syncInstance not use
     * @return IncomingCommunicationBarring instance
     * @throws Exception if error
     */
    public IncomingCommunicationBarring getIncomingCommunicationBarring(boolean syncInstance) throws
            Exception {
        IncomingCommunicationBarring icb = new IncomingCommunicationBarring(mXcapUri, null,
                mIntendedId);

        if (mContext != null) {
            icb.setContext(mContext);
        }

        icb.loadConfiguration();
        return icb;
    }

    /**
     * Get a IncomingCommunicationBarring instance.
     *
     * @param syncInstance not use
     * @param network dedicated network
     * @return IncomingCommunicationBarring instance
     * @throws Exception if error
     */
    public IncomingCommunicationBarring getIncomingCommunicationBarring(
            boolean syncInstance, Network network) throws Exception {
        IncomingCommunicationBarring icb = new IncomingCommunicationBarring(mXcapUri, null,
                mIntendedId);
        if (network != null) {
            icb.setNetwork(network);
        }

        if (mContext != null) {
            icb.setContext(mContext);
        }

        icb.loadConfiguration();
        return icb;
    }

    /**
     * Get a OutgoingCommunicationBarring instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return OutgoingCommunicationBarring instance
     * @throws Exception if error
     */
    public OutgoingCommunicationBarring getOutgoingCommunicationBarring(
            XcapUri documentUri) throws Exception {
        return new OutgoingCommunicationBarring(documentUri, null, mIntendedId);
    }

    /**
     * Get a OutgoingCommunicationBarring instance.
     *
     * @param syncInstance not use
     * @return OutgoingCommunicationBarring instance
     * @throws Exception if error
     */
    public OutgoingCommunicationBarring getOutgoingCommunicationBarring(boolean syncInstance) throws
            Exception {
        OutgoingCommunicationBarring ocb = new OutgoingCommunicationBarring(mXcapUri, null,
                mIntendedId);

        if (mContext != null) {
            ocb.setContext(mContext);
        }

        ocb.loadConfiguration();
        return ocb;
    }

    /**
     * Get a OutgoingCommunicationBarring instance.
     *
     * @param syncInstance not use
     * @param network dedicated network
     * @return OutgoingCommunicationBarring instance
     * @throws Exception if error
     */
    public OutgoingCommunicationBarring getOutgoingCommunicationBarring(
            boolean syncInstance, Network network) throws Exception {
        OutgoingCommunicationBarring ocb = new OutgoingCommunicationBarring(mXcapUri, null,
                mIntendedId);
        if (network != null) {
            ocb.setNetwork(network);
        }

        if (mContext != null) {
            ocb.setContext(mContext);
        }

        ocb.loadConfiguration();
        return ocb;
    }

    /**
     * Get a DiversionServiceCapability instance.
     *
     * @param syncInstance not use
     * @return DiversionServiceCapability instance
     * @throws Exception if error
     */
    public DiversionServiceCapability getDiversionServiceCapability(boolean syncInstance) throws
            Exception {
        return new DiversionServiceCapability(mXcapUri, null, mIntendedId);
    }

    /**
     * Get a DiversionServiceCapability instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return DiversionServiceCapability instance
     * @throws Exception if error
     */
    public DiversionServiceCapability getDiversionServiceCapability(
            XcapUri documentUri) throws Exception {
        return new DiversionServiceCapability(documentUri, null, mIntendedId);
    }

    /**
     * Get a DiversionServiceCapability instance.
     *
     * @param syncInstance not use
     * @return BarringServiceCapability instance
     * @throws Exception if error
     */
    public BarringServiceCapability getBarringServiceCapability(boolean syncInstance) throws
            Exception {
        return new BarringServiceCapability(mXcapUri, null, mIntendedId);
    }

    /**
     * Get a BarringServiceCapability instance.
     * For test purpose.
     *
     * @param documentUri   XCAP document URI
     * @return BarringServiceCapability instance
     * @throws Exception if error
     */
    public BarringServiceCapability getBarringServiceCapability(
            XcapUri documentUri) throws Exception {
        return new BarringServiceCapability(documentUri, null, mIntendedId);
    }

}
