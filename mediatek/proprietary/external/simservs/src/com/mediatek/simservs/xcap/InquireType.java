package com.mediatek.simservs.xcap;

import android.util.Log;

import com.mediatek.xcap.client.XcapClient;
import com.mediatek.xcap.client.XcapDebugParam;
import com.mediatek.xcap.client.uri.XcapUri;

import com.android.okhttp.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * InquireType abstract class.
 *
 */
public abstract class InquireType extends XcapElement {

    /**
     * Constructor.
     *
     * @param xcapUri           XCAP document URI
     * @param parentUri         XCAP root directory URI
     * @param intendedId        X-3GPP-Intended-Id
     * @throws XcapException    if XCAP error
     * @throws ParserConfigurationException if parser configuration error
     */
    public InquireType(XcapUri xcapUri, String parentUri, String intendedId)
            throws XcapException, ParserConfigurationException {
        super(xcapUri, parentUri, intendedId);
    }

    /**
     * Gets the content of the current node through XCAP protocol.
     *
     * @return configuration XML
     * @throws XcapException if XCAP error
     */
    public String getContent() throws XcapException {
        XcapClient xcapClient = null;
        HttpURLConnection conn = null;
        String ret = null;
        Headers.Builder headers = new Headers.Builder();

        try {
            String nodeUri = getNodeUri().toString();
            XcapDebugParam debugParam = XcapDebugParam.getInstance();

            if (debugParam.getEnableSimservQueryWhole()) {
                nodeUri = nodeUri.substring(0, nodeUri.lastIndexOf("simservs") +
                        "simservs".length());
            }

            URI uri = new URI(nodeUri);

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
                headers.add("If-None-Match", "\"" + mEtag + "\"");
            } else if (mIntendedId != null) {
                headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
            }

            if (mContext != null) {
                xcapClient.setContext(mContext);
            }
            conn = xcapClient.get(uri, headers.build());

            if (conn != null) {
                if (conn.getResponseCode() == 200 ||
                        conn.getResponseCode() == 304) {
                    String etagValue = conn.getHeaderField("ETag");

                    if (etagValue != null) {
                        this.mIsSupportEtag = true;
                        this.mEtag = etagValue;
                    } else {
                        this.mIsSupportEtag = false;
                        this.mEtag = null;
                    }

                    InputStream is = null;
                    try {
                        is = conn.getInputStream();
                        // convert stream to string
                        ret = convertStreamToString(is);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                } else if (conn.getResponseCode() == 409) {
                    InputStream is = conn.getInputStream();

                    if (is != null) {
                        if ("true".equals(
                                System.getProperty("xcap.handl409"))) {
                            ret = null;
                            throw new XcapException(409,
                                parse409ErrorMessage("phrase", is));
                        } else {
                            ret = null;
                            throw new XcapException(409);
                        }
                    } else {
                        ret = null;
                        throw new XcapException(409);
                    }
                } else {
                    ret = null;
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

        Log.d(TAG, "Response XML:");
        Log.d(TAG, ret);
        return ret;
    }
}
