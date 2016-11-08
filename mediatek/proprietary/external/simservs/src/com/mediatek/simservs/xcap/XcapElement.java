package com.mediatek.simservs.xcap;

import android.content.Context;
import android.net.Network;
import android.os.SystemProperties;
import android.util.Log;

import com.android.okhttp.Headers;

import com.mediatek.simservs.client.CommunicationDiversion.NoReplyTimer;
import com.mediatek.xcap.client.XcapClient;
import com.mediatek.xcap.client.XcapConstants;
import com.mediatek.xcap.client.XcapDebugParam;
import com.mediatek.xcap.client.uri.XcapUri;
import com.mediatek.xcap.client.uri.XcapUri.XcapNodeSelector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Validity abstract class.
 */
public abstract class XcapElement implements Attributable {
    public static final String TAG = "XcapElement";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    protected static final String AUTH_XCAP_3GPP_INTENDED = "X-3GPP-Intended-Identity";
    protected static final String XCAP_NAMESPACE =
            "http://uri.etsi.org/ngn/params/xml/simservs/xcap";
    protected static final String XCAP_ALIAS = "ss";
    protected static final String COMMON_POLICY_NAMESPACE = "urn:ietf:params:xml:ns:common-policy";
    protected static final String COMMON_POLICY_ALIAS = "cp";
    protected String mNodeUri = null;
    public XcapUri mXcapUri = null;
    public String mParentUri = null;
    public String mIntendedId = null;
    protected String mEtag = null;
    protected boolean mIsSupportEtag = false;
    public XcapDebugParam mDebugParams = XcapDebugParam.getInstance();
    protected Network mNetwork;
    protected Context mContext;

    /**
     * Constructs an instance without XML source.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     */
    public XcapElement(XcapUri xcapUri, String parentUri, String intendedId) {
        mXcapUri = xcapUri;
        mParentUri = parentUri;
        mIntendedId = intendedId;
    }

    /**
     * Set dedicated network.
     *
     * @param network dedicated network
     */
    public void setNetwork(Network network) {
        if (network != null) {
            Log.i(TAG, "XCAP dedicated network netid:" + network);
            mNetwork = network;
        }
    }

    /**
     * Set context.
     *
     * @param ctxt context to set
     */
    public void setContext(Context ctxt) {
        if (ctxt != null) {
            mContext = ctxt;
        }
    }

    /**
     * Set ETag value.
     *
     * @param etag value
     */
    public void setEtag(String etag) {
        mEtag = etag;
    }

    /**
     * Get ETag value.
     *
     * @return ETag value
     */
    public String getEtag() {
        return mEtag;
    }

    /**
     * Get node URI.
     *
     * @return URI
     * @throws IllegalArgumentException if illegal argument
     * @throws URISyntaxException if URI syntax error
     */
    public URI getNodeUri() throws IllegalArgumentException,
            URISyntaxException {
        URI elementURI;
        XcapNodeSelector elementSelector = new XcapNodeSelector(XcapConstants.ROOT_SIMSERVS)
                .queryByNodeName(mParentUri)
                .queryByNodeName(getNodeName());

        elementURI = mXcapUri.setNodeSelector(elementSelector).toURI();
        return elementURI;
    }

    /**
     * Get attribute URI .
     *
     * @return URI
     * @throws IllegalArgumentException if illegal argument
     */
    private URI getAttributeUri(String attribute) throws IllegalArgumentException,
            URISyntaxException {
        URI elementURI;
        XcapNodeSelector elementSelector = new XcapNodeSelector(XcapConstants.ROOT_SIMSERVS)
                .queryByNodeName(mParentUri)
                .queryByNodeName(getNodeName(), attribute);

        elementURI = mXcapUri.setNodeSelector(elementSelector).toURI();
        return elementURI;
    }

    /**
     * Gets attribute value by attribute name.
     *
     * @param  attribute attribute name
     * @return attribute value
     * @throws XcapException if XCAP error
     */
    @Override
    public String getByAttrName(String attribute) throws XcapException {
        XcapClient xcapClient;

        if (mNetwork != null) {
            xcapClient = new XcapClient(mNetwork);
        } else {
            xcapClient = new XcapClient();
        }

        HttpURLConnection conn = null;
        String ret = null;
        Headers.Builder headers = new Headers.Builder();

        try {
            if (mIntendedId != null && mEtag != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                headers.add("If-None-Match", "\"" + mEtag + "\"");
            } else if (mIntendedId != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
            }

            conn = xcapClient.get(getAttributeUri(attribute), headers.build());
            if (conn != null) {
                if (conn.getResponseCode() == 200) {
                    String etagValue = conn.getHeaderField("ETag");

                    if (etagValue != null) {
                        this.mEtag = etagValue;
                    }

                    InputStream is = conn.getInputStream();
                    // convert stream to string
                    ret = convertStreamToString(is);
                } else {
                    ret = null;
                    throw new XcapException(conn.getResponseCode());
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new XcapException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
        return ret;
    }

    /**
     * Sets attribute value by attribute name.
     *
     * @param  attrName attribute name
     * @param  attrValue attribute value
     * @throws XcapException if XCAP error
     */
    @Override
    public void setByAttrName(String attrName, String attrValue) throws XcapException {
        XcapClient xcapClient;

        if (mNetwork != null) {
            xcapClient = new XcapClient(mNetwork);
        } else {
            xcapClient = new XcapClient();
        }

        HttpURLConnection conn = null;
        Headers.Builder headers = new Headers.Builder();

        try {
            if (mIntendedId != null && mEtag != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                headers.add("If-Match", "\"" + mEtag + "\"");
            } else if (mIntendedId != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
            }

            conn = xcapClient.put(getAttributeUri(attrName), "application/xcap-att+xml",
                    attrValue, headers.build());
            // check put response
            if (conn != null) {
                if (conn.getResponseCode() == 200
                        || conn.getResponseCode() == 201) {
                    String etagValue = conn.getHeaderField("ETag");

                    if (etagValue != null) {
                        this.mEtag = etagValue;
                    }

                    Log.d("info", "document created in xcap server...");
                } else {
                    throw new XcapException(conn.getResponseCode());
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new XcapException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
    }

    /**
     * Deletes attribute by attribute name.
     *
     * @param  attribute attribute name
     * @throws XcapException if XCAP error
     */
    @Override
    public void deleteByAttrName(String attribute) throws XcapException {
        XcapClient xcapClient;

        if (mNetwork != null) {
            xcapClient = new XcapClient(mNetwork);
        } else {
            xcapClient = new XcapClient();
        }

        HttpURLConnection conn = null;
        Headers.Builder headers = new Headers.Builder();

        try {
            if (mIntendedId != null && mEtag != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                headers.add("If-Match", "\"" + mEtag + "\"");
            } else if (mIntendedId != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
            }

            conn = xcapClient.delete(getAttributeUri(attribute), headers.build());
            // check put response
            if (conn != null) {
                if (conn.getResponseCode() == 200) {
                    String etagValue = conn.getHeaderField("ETag");

                    if (etagValue != null) {
                        this.mEtag = etagValue;
                    }

                    Log.d("info", "document deleted in xcap server...");
                } else {
                    throw new XcapException(conn.getResponseCode());
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new XcapException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
    }

    /**
     * Sets the content of the current node.
     *
     * @param  xml XML string
     * @throws XcapException if XCAP error
     */
    public void setContent(String xml) throws XcapException {
        try {
            mNodeUri = getNodeUri().toString();
            saveContent(xml);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update current node through XCAP protocol.
     *
     * @param  xml XML string
     * @throws XcapException if XCAP error
     */
    protected void saveContent(String xml) throws XcapException {
        XcapClient xcapClient = null;
        HttpURLConnection conn = null;
        Headers.Builder headers = new Headers.Builder();

        try {
            URI uri = new URI(mNodeUri);

            if (mNetwork != null) {
                xcapClient = new XcapClient(mNetwork);

                if (xcapClient == null) {
                    throw new XcapException(500);
                }
            } else {
                xcapClient = new XcapClient();
            }

            if (mIntendedId != null && mEtag != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                headers.add("If-Match", "\"" + mEtag + "\"");
            } else if (mIntendedId != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
            }

            if (mDebugParams.getEnablePredefinedSimservSetting() &&
                    !getNodeName().equals(NoReplyTimer.NODE_NAME)) {
                String xMl = readXmlFromFile("/data/simservs.xml");

                if (xMl != null) {
                    xml = xMl;
                }
            }

            String putElementMime = null;

            if (mDebugParams.getXcapPutElementMime() != null &&
                    !mDebugParams.getXcapPutElementMime().isEmpty()) {
                putElementMime = mDebugParams.getXcapPutElementMime();
            } else {
                putElementMime = System.getProperty("xcap.putelcontenttype",
                        "application/xcap-el+xml");
            }

            conn = xcapClient.put(uri, putElementMime, xml, headers.build());
            // check put response
            if (conn != null) {
                if (conn.getResponseCode() == 200
                        || conn.getResponseCode() == 201) {
                    String etagValue = conn.getHeaderField("ETag");

                    if (etagValue != null) {
                        this.mEtag = etagValue;
                    }

                    Log.d("info", "document created in xcap server...");
                } else if (conn.getResponseCode() == 409) {
                    InputStream is = conn.getInputStream();

                    if (is != null) {
                        if ("true".equals(System.getProperty("xcap.handl409"))) {
                            throw new XcapException(409, parse409ErrorMessage("phrase", is));
                        } else {
                            throw new XcapException(409);
                        }
                    } else {
                        throw new XcapException(409);
                    }
                } else {
                    throw new XcapException(conn.getResponseCode());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new XcapException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
    }

    /**
     * Not supported.
     *
     * @return content type
     */
    public String getContentType() {
        return null;
    }

    /**
     * Gets the URI of the current node.
     *
     * @return URI
     */
    public String getUri() {
        StringBuilder pathUri = new StringBuilder();

        if (mParentUri != null) {
            pathUri.append(mParentUri)
                    .append("\\")
                    .append(getNodeName());
            return pathUri.toString();
        } else {
            return getNodeName();
        }
    }

    /**
     * Not supported.
     *
     * @return null
     */
    public XcapElement getParent() {
        return null;
    }

    /**
     * Not supported.
     *
     * @return null
     */
    public String getNodeSelector() {
        return null;
    }


    /**
     * Transfer the DOM object to XML string.
     *
     * @param  element DOM element
     * @return XML string
     * @throws TransformerException if tranformation error
     */
    public String domToXmlText(Element element) throws TransformerException {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(element),
                new StreamResult(buffer));
        return buffer.toString();
    }

    /**
     * Transfer input stream to string.
     *
     * @param  inputStream input stream
     * @return string
     * @throws IOException if I/O error
     */
    public String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    protected abstract String getNodeName();

    protected String readXmlFromFile(String file) {
        String text = "";

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(fis);

            String buf;
            while ((buf = dis.readLine()) != null) {
                Log.d(TAG, "Read:" + buf);
                text += buf;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text;
    }

    /**
     * Parse HTTP 409 error message.
     *
     * @param  xmlErrorTag HTTP 409 specific error message XML tag
     * @param  inputStream HTTP 409 content
     * @return string
     */
    protected String parse409ErrorMessage(String xmlErrorTag, InputStream content)
            throws XcapException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder db = factory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(convertStreamToString(content)));
            Document doc;
            doc = db.parse(is);

            NodeList currentNode = doc.getElementsByTagName(xmlErrorTag);

            if (currentNode.getLength() > 0) {
                Element activeElement = (Element) currentNode.item(0);
                String textContent = activeElement.getTextContent();
                Log.d(TAG, "parse409ErrorMessage:[" + textContent + "]");
                return textContent;
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new XcapException(500);
        } catch (IOException e) {
            e.printStackTrace();
            throw new XcapException(500);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new XcapException(500);
        }

        return null;
    }
}
