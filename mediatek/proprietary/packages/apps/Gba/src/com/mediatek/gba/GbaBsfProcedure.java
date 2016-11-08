package com.mediatek.gba;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.mediatek.gba.auth.AkaDigestAuth;
import com.mediatek.gba.auth.KeyCalculator;

import com.mediatek.gba.element.AkaResponse;
import com.mediatek.gba.element.GbaBsfResponse;
import com.mediatek.gba.element.NafId;
import com.mediatek.gba.element.Nonce;
import com.mediatek.gba.element.SresResponse;
import com.mediatek.gba.header.WwwAuthHeader;
import com.mediatek.gba.telephony.TelephonyUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * implementation for GbaBsfProcedure.
 *
 * @hide
 */
public class GbaBsfProcedure {
    private static final String TAG = "GbaBsfProcedure";
    private static final int MAX_BSF_SESSION_TIMES = 2;
    private static final int SOCKET_OPERATION_TIMEOUT = 30 * 1000;

    private static final String USER_AGENT_HDR = "User-agent";
    private static final String TARGET_HOST_HDR = "Host";
    private static final String ACCEPT_HDR = "Accept";
    private static final String X_IMEI_HDR = "X-TMUS-IMEI";

    private static final String USER_AGENT = "Bootstrapping Client Agent ";
    private static final String USER_AGENT_GBA_ME   = " 3gpp-gba";
    private static final String USER_AGENT_GBA_UICC = " 3gpp-gba-uicc";
    private static final String USER_AGENT_TMPI_INDICATOR = "3gpp-gba-tmpi";


    private static final String WWW_AUTH_RESP = "Authorization";

    private final static String AUTHORIZATION_HEADER_VALUE_INITIAL = ""
            + "Digest username=\"%s\", realm=\"%s\", nonce=\"\", uri=\"/\", response=\"\"";

    private final static String ALL_NAMESPACES = "*";
    private final static String BOOTSTRAPPING_INFO_NODE = "BootstrappingInfo";
    private final static String BTID_NODE = "btid";
    private final static String LIFETIME_NODE = "lifetime";

    private static final String DEFAULT_BSF_PREFIX = "bsf.";
    private static final String DEFAULT_BSF_POSTFIX = ".pub.3gppnetwork.org";

    private static final String IMPI_DOMAIN = "@ims.";
    private static final String IMPI_POSTFIX = ".3gppnetwork.org";

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    private URI mBsfUri = null;

    private int mGbaType;
    private int mSubId;
    private Context mContext;
    private String mUserName;
    private String mBsfHost;
    private String mImpi;

    /**
    * Bootstarp procedure.
    * @param gbaType the type of GBA mode.
    * @param cardType the type of card.
    * @param context the application context.
    *
    */
    protected GbaBsfProcedure(int gbaType, int subId, Context context) {
        mContext = context;
        mGbaType = gbaType;
        mSubId = subId;

        Log.i(TAG, "GbaBsfProcedure: gbaType:" + gbaType + " subId:" + subId);

        try {
            mImpi = TelephonyUtils.getPhoneImpi(subId);
            Log.i(TAG, "mImpi:" + mImpi);

            if (mImpi != null && mImpi.contains("@")) {
                mUserName = mImpi;
            } else {
                mUserName = getLocalImpi(subId);
            }

            mBsfHost = resolveBsfAddress(mImpi, subId);

            if (mBsfHost != null) {
                Log.i(TAG, "use ssl:" + context.getResources().getBoolean(
                            R.bool.use_tls_transport));

                int port = context.getResources().getInteger(R.integer.config_bsf_port);
                Log.i(TAG, "port:" + port);

                if (port == 80) {
                    mBsfUri = new URI(HTTP_SCHEME, mBsfHost, "/", null);
                } else if (port > 0) {
                    mBsfUri = new URI(HTTP_SCHEME, null, mBsfHost, port, "/", null, null);
                } else {
                    if (context.getResources().getBoolean(R.bool.use_tls_transport)) {
                        mBsfUri = new URI(HTTPS_SCHEME, mBsfHost, "/", null);
                    } else {
                        mBsfUri = new URI(HTTP_SCHEME, mBsfHost, "/", null);
                    }
                }
            }

            GbaDebugParam gbaDebugParam = GbaDebugParam.getInstance();
            String testUrl = gbaDebugParam.getBsfServerUrl();
            if (testUrl != null && testUrl.length() > 0) {
                mBsfUri = new URI(testUrl);
                Log.i(TAG, "Connect with testUrl:" + mBsfUri);
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    protected NafSessionKey perform(NafId nafId) {
        NafSessionKey nafSessionKey = null;
        int runBsfRequest = 0;
        boolean isFirstRequest = true;
        GbaBsfResponse firstBsfResponse = null;
        GbaBsfResponse secondBsfResponse = null;
        WwwAuthHeader firstWwwAutHeader = null;
        int cardType = GbaConstant.GBA_CARD_UNKNOWN;

        Log.d(TAG, "GBA with okHttp");

        if (mBsfUri == null) {
            Log.e(TAG, "Error No BSF URI");
            return null;
        }

        if (mGbaType == GbaConstant.GBA_NONE) {
            Log.e(TAG, "mGbaType is none");
            return null;
        }

        cardType = TelephonyUtils.getIccCardType(mSubId);

        if (cardType == GbaConstant.GBA_CARD_UNKNOWN) {
            Log.e(TAG, "cardType is unknown");
            return null;
        }

        do {
            runBsfRequest++;
            Log.i(TAG, "run GBA procedure with BSF:" + runBsfRequest);

            if (isFirstRequest) {
                firstBsfResponse = sendFirstRequest();

                if (firstBsfResponse != null && firstBsfResponse.getStatusCode() ==
                    HttpURLConnection.HTTP_UNAUTHORIZED) {
                    firstWwwAutHeader = firstBsfResponse.getWwwAuthenticateHeader();
                } else {
                    isFirstRequest = true;
                    Log.e(TAG, "Fail to get the first response from BSF");

                    if (firstBsfResponse != null &&
                            firstBsfResponse.getStatusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                        nafSessionKey = new NafSessionKey();
                        nafSessionKey.setException(new IllegalStateException("HTTP 403 Forbidden"));
                        break;
                    }

                    continue;
                }
            }

            Log.i(TAG, "Process the GBA first response");

            Nonce nonce = null;
            AkaResponse akaResponse = null;
            SresResponse sresResponse = null;

            try {
                if (GbaConstant.GBA_U == mGbaType || GbaConstant.GBA_ME == mGbaType) {
                    nonce = Nonce.decodeNonce(firstWwwAutHeader.getNonce());
                    Log.d(TAG, "nonce dump:" + nonce);
                    akaResponse = runAkaAlgorithm(nonce);

                    isFirstRequest = false;
                    String auts = "";
                    String passwd = "";

                    Log.d(TAG, "response dump:" + akaResponse);

                    if (akaResponse != null && akaResponse.getAuts() != null) {
                        auts = new String(Base64.encode(akaResponse.getAuts(), Base64.DEFAULT));
                    }

                    if (auts == null || auts.length() == 0) {
                        if (akaResponse != null) {
                            passwd = TelephonyUtils.bytesToHex(akaResponse.getRes());
                        }
                    }

                    secondBsfResponse = sendSecondRequest(passwd, auts, firstWwwAutHeader,
                                            runBsfRequest);
                } else {
                    sresResponse = SresResponse.getSresResponse(mContext,
                            firstWwwAutHeader.getNonce(), mSubId);

                    if (sresResponse == null) {
                        isFirstRequest = true;
                        Log.e(TAG, "Fail to get 2G SIM response. Try again new");
                        continue;
                    }

                    isFirstRequest = false;
                    String passwd = "";
                    final KeyCalculator keyCalculator = KeyCalculator.getInstance();

                    try {
                        byte[] res = keyCalculator.calculateRes(sresResponse.getKc(),
                                sresResponse.getRand(), sresResponse.getSres());
                        passwd = TelephonyUtils.bytesToHex(res);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    secondBsfResponse = sendSecondRequest(passwd, "", firstWwwAutHeader,
                                                            runBsfRequest);
                }
            } catch (NullPointerException ee) {
                ee.printStackTrace();
            }

            if (secondBsfResponse.getStatusCode() == HttpURLConnection.HTTP_OK) {
                nafSessionKey = parseBsfXmlFromResponse(secondBsfResponse);

                if (mGbaType == GbaConstant.GBA_U) { //stores NAF key parameters into UICC
                    String dummy = "";
                    storeNafKeyParameters(cardType, nonce.getRand(),
                                nafSessionKey.getBtid(),
                                nafSessionKey.getKeylifetime(), mSubId);
                    nafSessionKey.setKey(TelephonyUtils.calculateNafExternalKey(
                                nafId.getNafIdBin(), null, mSubId));
                    nafSessionKey.setNafKeyName(GbaConstant.GBA_KS_EXT_NAF);
                    nafSessionKey.setNafId(nafId.getNafIdBin());
                } else if (cardType == GbaConstant.GBA_CARD_USIM ||
                        cardType == GbaConstant.GBA_CARD_ISIM) {
                    calculateNafKey(nafId, nonce.getRand(), nafSessionKey, akaResponse);
                } else if (cardType == GbaConstant.GBA_CARD_SIM) {
                    calculateNafKeyBySim(nafId, nafSessionKey, sresResponse);
                }

                break;
            } else if (secondBsfResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.e(TAG, "Response is 401. Retry");
                firstWwwAutHeader = secondBsfResponse.getWwwAuthenticateHeader();
            } else {
                Log.e(TAG, "Fail to get the second response from BSF");

                if (secondBsfResponse.getStatusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                    Log.e(TAG, "Hit HTTP 403 Forbidden");
                    nafSessionKey = new NafSessionKey();
                    nafSessionKey.setException(new IllegalStateException("HTTP 403 Forbidden"));
                    break;
                }

                continue;
            }
        } while (runBsfRequest < MAX_BSF_SESSION_TIMES);

        Log.i(TAG, "[Done]run GBA procedure with BSF");

        return nafSessionKey;
    }

    private AkaResponse runAkaAlgorithm(Nonce nonce) {
        byte[] rand = nonce.getRand();
        byte[] autn = nonce.getAutn();
        byte[] res  = null;
        AkaResponse response = null;

        if (mGbaType == GbaConstant.GBA_ME) {
            res = TelephonyUtils.calculateAkaAuthAndRes(mContext, rand, autn, mSubId);
        } else if (mGbaType == GbaConstant.GBA_U) {
            res = TelephonyUtils.calculateGbaAuthAndRes(mContext, rand, autn, mSubId);
        } else {
            Log.e(TAG, "Non-support type:" + mGbaType);
        }

        if (res != null) {
            response = new AkaResponse(res, mGbaType); //Todo: configure auts as emtpy string
        }

        return response;
    }

    private GbaBsfResponse sendFirstRequest() {
        GbaBsfResponse bsfResponse = null;

        Log.i(TAG, "Connect to BSF:" + mBsfUri.toString());

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = createHttpConnection(mBsfUri.toString());
            if (urlConnection != null) {
                createHttpRequest(urlConnection, true);
                bsfResponse = GbaBsfResponse.parseResponse(urlConnection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return bsfResponse;
    }

    private GbaBsfResponse sendSecondRequest(String passwd, String auts,
                            WwwAuthHeader header, int runCount) {
        GbaBsfResponse bsfResponse = null;

        Log.i(TAG, "passwd:" + passwd + "  auts:" + auts);
        HttpURLConnection urlConnection = null;
        String nc = String.format("0000000%d", runCount);

        AkaDigestAuth akaDigestAuth = new AkaDigestAuth(header, mUserName, auts, passwd,
                                            mBsfUri.getPath(), nc);
        akaDigestAuth.calculateRequestDigest();

        try {
            urlConnection = createHttpConnection(mBsfUri.toString());
            if (urlConnection != null) {
                createHttpRequest(urlConnection, false);
                urlConnection.setRequestProperty(WWW_AUTH_RESP,
                                        akaDigestAuth.createAuthorHeaderValue());
                bsfResponse = GbaBsfResponse.parseResponse(urlConnection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return bsfResponse;
    }

    private void createHttpRequest(HttpURLConnection urlConn, boolean isFirstRequest) {

        if (mBsfUri.getPort() != -1) {
            urlConn.setRequestProperty(TARGET_HOST_HDR,
                        mBsfUri.getHost() + ":" + mBsfUri.getPort());
        }

        urlConn.setRequestProperty(WWW_AUTH_RESP, getAuthorHeaderValue(isFirstRequest));
        urlConn.setRequestProperty(ACCEPT_HDR, "*/*");

        String imeiHeader = mContext.getResources().getString(R.string.config_imei_header);
        Log.i(TAG, "imeiHeader:" + imeiHeader);

        if (X_IMEI_HDR.equals(imeiHeader)) {
            urlConn.setRequestProperty(X_IMEI_HDR, TelephonyUtils.getImei(mContext, mSubId));
        }

        //final Date now = new Date();
        //urlConn.setRequestProperty("Date", DateUtils.formatDate(now));
    }

    private String getAuthorHeaderValue(boolean isFristRequest) {
        String value = null;

        if (isFristRequest) {
            String realm = mBsfUri.getHost();
            value = String.format(AUTHORIZATION_HEADER_VALUE_INITIAL, mUserName, realm,
                                mBsfUri.getPath());
        }

        return value;
    }

    private HttpURLConnection createHttpConnection(String host) {
        HttpURLConnection urlConnection = null;
        boolean isTrustAll = true;

        isTrustAll = GbaDebugParam.getInstance().getEnableGbaTrustAll();
        if (isTrustAll) {
            Log.i(TAG, "Trust all is enabled");
        }

        if (isTrustAll) {
            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, mTrustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (GeneralSecurityException se) {
                se.printStackTrace();
            }

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
        }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }

        try {
            URL url = new URL(host);
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (GbaConstant.GBA_ME == mGbaType) {
            urlConnection.setRequestProperty(USER_AGENT_HDR, USER_AGENT
                                + Build.MANUFACTURER + USER_AGENT_GBA_ME);
        } else if (GbaConstant.GBA_U == mGbaType) {
            urlConnection.setRequestProperty(USER_AGENT_HDR, USER_AGENT
                                + Build.MANUFACTURER + USER_AGENT_GBA_UICC);
        } else {
            urlConnection.setRequestProperty(USER_AGENT_HDR, USER_AGENT);
        }

        //Configure timeout value.
        urlConnection.setConnectTimeout(SOCKET_OPERATION_TIMEOUT);
        urlConnection.setReadTimeout(SOCKET_OPERATION_TIMEOUT);
        urlConnection.setWriteTimeout(SOCKET_OPERATION_TIMEOUT);

        return urlConnection;
    }

    private NafSessionKey parseBsfXmlFromResponse(GbaBsfResponse bsfResponse) {
        NafSessionKey nafSessionKey = new NafSessionKey();
        String xmlFromResponse = bsfResponse.getXmlContent();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                                                        xmlFromResponse.getBytes());
            Document document = documentBuilder.parse(byteArrayInputStream);
            NodeList bootstrappingInfoNodeList = document.getElementsByTagNameNS(ALL_NAMESPACES,
                                                BOOTSTRAPPING_INFO_NODE);

            if (bootstrappingInfoNodeList == null || bootstrappingInfoNodeList.getLength() != 1) {
                Log.e(TAG, "Error content in " + BOOTSTRAPPING_INFO_NODE);
                return null;
            }

            Node bootstrappingInfoNode = bootstrappingInfoNodeList.item(0);

            if (bootstrappingInfoNode.getNodeType() == Node.ELEMENT_NODE) {
                Element bootstrappingInfoElement = (Element) bootstrappingInfoNode;
                nafSessionKey.setBtid(parseBtidNode(bootstrappingInfoElement));;
                nafSessionKey.setKeylifetime(parseKeylifetimeNode(bootstrappingInfoElement));
            } else {
                Log.e(TAG, "Error node in " + bootstrappingInfoNode.getNodeType());
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException ee) {
            ee.printStackTrace();
        } catch (SAXException eee) {
            eee.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }

        return nafSessionKey;
    }

    private String parseBtidNode(Element bootstrappingInfoElement) {
        NodeList btidNodeList = bootstrappingInfoElement.getElementsByTagNameNS(ALL_NAMESPACES,
                                                                                BTID_NODE);

        if (btidNodeList == null || btidNodeList.getLength() != 1) {
            Log.e(TAG, "Something wrong with parsing " + BTID_NODE);
            return null;
        }

        Node btidNode = btidNodeList.item(0);

        if (btidNode == null) {
            Log.e(TAG, "Something is null with parsing " + BTID_NODE);
            return null;
        }

        return btidNode.getFirstChild().getNodeValue();
    }

    private String parseKeylifetimeNode(Element bootstrappingInfoElement) {
        NodeList keylifetimeNodeList =
            bootstrappingInfoElement.getElementsByTagNameNS(ALL_NAMESPACES, LIFETIME_NODE);

        if (keylifetimeNodeList == null || keylifetimeNodeList.getLength() != 1) {
            Log.e(TAG, "Something wrong with parsing " + LIFETIME_NODE);
            return null;
        }

        Node keylifetimeNode = keylifetimeNodeList.item(0);

        if (keylifetimeNode == null) {
            Log.e(TAG, "Something is null with parsing " + LIFETIME_NODE);
            return null;
        }

        return keylifetimeNode.getFirstChild().getNodeValue();
    }

    private void storeNafKeyParameters(int cardType, byte[] rand,
            String btid, String keyLifetime, int subId) {
        Log.i(TAG, "storeNafKeyParameters()btid=" + btid + ";keyLifetime=" + keyLifetime);
        TelephonyUtils.setGbaBootstrappingParameters(
                cardType, rand, btid, keyLifetime, subId);
    }

    private void calculateNafKeyBySim(NafId nafId, NafSessionKey nafSessionKey,
                                        SresResponse sresResponse) {
        if (nafSessionKey == null) {
            throw new IllegalArgumentException("nafSessionKey must be not null.");
        }

        if (sresResponse == null) {
            throw new IllegalStateException(
                "Aka response is null. Please calculate if before using.");
        }

        final KeyCalculator keyCalculator = KeyCalculator.getInstance();

        try {
            //@User mUserName instead of impi
            final byte[] ks = keyCalculator.calculateKsBySres(
                    sresResponse.getKc(),  sresResponse.getRand(),
                    sresResponse.getKsInput(), sresResponse.getSres());
            final byte[] keyKsNaf = keyCalculator.calculateKsNaf(ks, sresResponse.getRand(),
                            mUserName, nafId);

            nafSessionKey.setKey(keyKsNaf);
            nafSessionKey.setNafKeyName(GbaConstant.GBA_KS_NAF);
            nafSessionKey.setNafId(nafId.getNafIdBin());
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void calculateNafKey(NafId nafId, byte[] rand, NafSessionKey nafSessionKey,
                            AkaResponse akaResponse) {

        if (nafSessionKey == null) {
            throw new IllegalArgumentException("nafSessionKey must be not null.");
        }

        if (akaResponse == null) {
            throw new IllegalStateException(
                "Aka response is null. Please calculate if before using.");
        }


        final byte[] ck = akaResponse.getCk();
        final byte[] ik = akaResponse.getIk();

        final KeyCalculator keyCalculator = KeyCalculator.getInstance();

        try {
                //@User mUserName instead of impi
                final byte[] ks = keyCalculator.calculateKsByGbaMe(ck, ik);
                final byte[] keyKsNaf = keyCalculator.calculateKsNaf(ks, rand, mUserName, nafId);

                nafSessionKey.setKey(keyKsNaf);
                nafSessionKey.setNafKeyName(GbaConstant.GBA_KS_NAF);
                nafSessionKey.setNafId(nafId.getNafIdBin());
        } catch (NullPointerException e) {
                e.printStackTrace();
        } catch (IOException ioe) {
                ioe.printStackTrace();
        }
        Log.i(TAG, "[done]nafSessionKey=" + nafSessionKey);
    }

    //3GPP TS 23.003
    //16.2 BSF address
    //
    private String resolveBsfAddress(String impi, int subId) {
        String bsfHost = null;

        try {
            if (impi != null) { //ISIM support
                int offset = impi.indexOf("@");

                if (offset != -1) {
                    String domain = impi.substring(offset + 1);
                    if (domain.endsWith("3gppnetwork.org")) {
                        bsfHost = DEFAULT_BSF_PREFIX +
                                domain.substring(0, domain.indexOf(".3gppnetwork.org")) +
                                ".pub.3gppnetwork.org";
                    } else {
                        bsfHost = DEFAULT_BSF_PREFIX + domain;
                    }
                }
            } else { //IMSI support
                String operator = TelephonyUtils.getSimOperator(mContext, subId);

                if (operator != null) {
                    String mcc = operator.substring(0, 3);
                    String mnc = operator.substring(3);

                    if (mnc.length() == 2) {
                        mnc = "0" + mnc;
                    }
                    bsfHost = DEFAULT_BSF_PREFIX + "mnc" + mnc + ".mcc"
                                    + mcc + DEFAULT_BSF_POSTFIX;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ee) {
            ee.printStackTrace();
        }

        return bsfHost;
    }

    private String getLocalImpi(int subId) {
        String newImpi = "";
        String operator = TelephonyUtils.getSimOperator(mContext, subId);
        String imsi = TelephonyUtils.getImsi(subId, mContext);

        if (operator != null) {
            String mcc = operator.substring(0, 3);
            String mnc = operator.substring(3);

            if (mnc.length() == 2) {
                mnc = "0" + mnc;
            }

            newImpi = imsi + IMPI_DOMAIN + "mnc" + mnc + ".mcc" + mcc + IMPI_POSTFIX;
        }

        return newImpi;
    }

    private TrustManager[] mTrustAllCerts = new TrustManager[] {new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
        }



        public void checkClientTrusted(X509Certificate[] certs, String authType) {

        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {

        }
    } };
}
