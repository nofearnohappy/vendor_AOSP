/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2014. All rights reserved.
*
* BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
* THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
* RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
* AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
* NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
* SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
* SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
* THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
* THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
* CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
* SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
* STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
* CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
* AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
* OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
* MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
* The following software/firmware and/or related documentation ("MediaTek Software")
* have been modified by MediaTek Inc. All revisions are subject to any receiver's
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.rcs.contacts.profileservice.profileservs.xcap;

import com.android.okhttp.Headers;

import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.uri.XcapUri;
import com.mediatek.xcap.client.uri.XcapUri.XcapNodeSelector;
import com.mediatek.xcap.client.XcapClient;
import com.mediatek.xcap.client.XcapConstants;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.apache.http.auth.Credentials;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.w3c.dom.Element;

/**
 * Validity abstract class.
 */
public abstract class ProfileXcapElement {
    private static final String TAG = "ProfileXcapElement";
    private static final String RESOURCE_SELECTOR_SEPARATOR = "/~~/";
    private static final char PATH_SEPARATOR = '/';

    public XcapUri mXcapUri = null;  //actually, is the document uri
    public String mParentUri = null;
    public String mIntendedId = null;
    public Credentials mCredentials = null;
    public static String mHostName = null;
    public static final String AUTH_XCAP_3GPP_INTENDED = "X-3GPP-Intended-Identity";
    public static final String HOST_NAME = "Host";
    public static final String USER_AGENT = "User-Agent";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String IF_NONE_MATCH = "If-None-Match";
    public static final String ETAG = "ETag";
    public static final String X_RESOLUTION = "X-Resolution";
    public static String mPccETag;
    public static String mPortraitETag;
    public static String mQRCodeETag;


    public ProfileXcapElement (XcapUri xcapUri, String parentUri, String intendedId,
            Credentials credential) {
        mXcapUri = xcapUri;
        mParentUri = parentUri;
        mIntendedId = intendedId;
        mCredentials = credential;
    }

    /**
     * Sets the content of the current node through XCAP protocol.
     *
     * @param  xml XML string
     * @throws ProfileXcapException if XCAP error
     */
    public int setContent(String xml, int contentType) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setContent");

        XcapClient xcapClient = new XcapClient();
        //HttpResponse response = null;
        HttpURLConnection conn = null;
        Headers.Builder headers = new Headers.Builder();
        //Header[] headers = null;
        String targetUri = null;
        try {
            //xcapClient.setAuthenticationCredentials(mCredentials);
            headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
            headers.add(HOST_NAME, mHostName);
            headers.add(USER_AGENT, "XDM-client/OMA1.0");
            //do not put "if-none-match" when set profile or portrait
            switch (contentType) {
                case ProfileConstants.CONTENT_TYPE_PCC:
                    //actually, we have no chance to handle PCC type on setting profile
                    //targetUri = mXcapUri.toURI().toString();
                    targetUri = toURIString();
                    break;

                case ProfileConstants.CONTENT_TYPE_PORTRAIT:
                    //targetUri = mXcapUri.toURI().toString();
                    targetUri = toURIString();
                    break;

                case ProfileConstants.CONTENT_TYPE_PART:
                    targetUri = getNodePath(false);
                    //targetUri = getNodeUri(false).toString();
                    break;

                case ProfileConstants.CONTENT_TYPE_QRCODE:
                case ProfileConstants.CONTENT_TYPE_QRCODE_MODE :
                    //for set qrcode, we can only set the <flag>, so set qrcode is same to set qrcode_mode
                    targetUri = getNodePath(true);
                    //targetUri = getNodeUri(true).toString();
                    break;

                default:
                    ProfileServiceLog.d(TAG, "error: unexpected type");
                    break;
            }
            if (contentType == ProfileConstants.CONTENT_TYPE_PORTRAIT) {
                conn = xcapClient.put(new URI(targetUri),
                        "application/vnd.oma.pres-content+xml; charset=\"utf-8\"",
                        xml, headers.build());
            } else {
                conn = xcapClient.put(new URI(targetUri),
                        "application/xcap-el+xml", xml, headers.build());
            }
            // check put response
            if (conn != null) {
                if (conn.getResponseCode() == 200 || conn.getResponseCode() == 201) {
                    ProfileServiceLog.d(TAG, "document created in profile server...");
                    String eTagValue = null;
                    eTagValue = conn.getHeaderField(ETAG);
                    //get the etag if set successfully
                    if(eTagValue != null) {
                        switch (contentType) {
                            case ProfileConstants.CONTENT_TYPE_PORTRAIT:
                                mPortraitETag = eTagValue;
                                break;

                            case ProfileConstants.CONTENT_TYPE_PCC:
                            case ProfileConstants.CONTENT_TYPE_PART:
                                mPccETag = eTagValue;
                                break;

                            case ProfileConstants.CONTENT_TYPE_QRCODE:
                            case ProfileConstants.CONTENT_TYPE_QRCODE_MODE:
                                /*cause we can only set the mode flag,  after set it,
                                no qrcode gotten automaticlly, so we should not get or save etag here,
                                and etag is static and maybe reused by next getting operation
                                result in the reposne is 304, but not the new qrcode,
                                it means that we dont not get etag here*/
                                //mQRCodeETag = eTagValue;
                                break;

                            default:
                                ProfileServiceLog.d(TAG, "error: unexpected type");
                                break;
                        }
                    }
                    return ProfileConstants.RES_OK;
                } else {
                    throw new ProfileXcapException(conn.getResponseCode());
                }
            } else {
                ProfileServiceLog.d(TAG, "response is null");
                throw new ProfileXcapException(new ConnectException());
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProfileXcapException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
        return ProfileConstants.RES_UNKNOW;
    }

    public String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
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

    public URI getPccUri() throws IllegalArgumentException,
            URISyntaxException {
        return mXcapUri.toURI();
    }

    /**
     * Get node URI.
     *
     * @return URI
     * @throws IllegalArgumentException if illegal argument
     * @throws URISyntaxException if URI syntax error
     */
    protected URI getNodeUri(boolean isQRCode) throws IllegalArgumentException,
            URISyntaxException {
        URI elementURI;
        ProfileServiceLog.d(TAG, "getNodeUri, mParentUri: " + mParentUri);
        XcapNodeSelector elementSelector;
        if (isQRCode) {
            /*for qrcode, the root node is <pcc-content>, it is the current node,
                    so we get it through "getNodeName()"*/
            elementSelector = new XcapNodeSelector(getNodeName());
        } else {
            elementSelector = new XcapNodeSelector("pcc")
                    .queryByNodeName(mParentUri)
                    .queryByNodeName(getNodeName());
            //mParentUri is "person-details"
            //so it is "pcc/person-details/nodename"
        }
        ProfileServiceLog.d(TAG, "elementSelector: " + elementSelector.toString());
        elementURI = mXcapUri.setNodeSelector(elementSelector).toURI();
        return elementURI;
    }

    /**
     * Get node path.
     *
     * @return Node path String
     * @throws IllegalArgumentException if illegal argument
     * @throws URISyntaxException if URI syntax error
     */
    protected String getNodePath(boolean isQRCode) throws IllegalArgumentException,
            URISyntaxException {
        String elementPath;
        ProfileServiceLog.d(TAG, "getNodePath, mParentUri: " + mParentUri);
        XcapNodeSelector elementSelector;

        if (isQRCode) {
            /*for qrcode, the root node is <pcc-content>, it is the current node,
                    so we get it through "getNodeName()"*/
            elementSelector = new XcapNodeSelector(getNodeName());
        } else {
            elementSelector = new XcapNodeSelector("pcc")
                    .queryByNodeName(mParentUri)
                    .queryByNodeName(getNodeName());
            //mParentUri is "person-details"
            //so it is "pcc/person-details/nodename"
        }
        mXcapUri.setNodeSelector(elementSelector);
        elementPath = toURIString();
        ProfileServiceLog.d(TAG, "elementPath: " + elementPath);
        return elementPath;
    }

    protected String toURIString() throws URISyntaxException {
        ProfileServiceLog.d(TAG, "toURIString");
        final StringBuilder sb = new StringBuilder(mXcapUri.getXcapRoot());
        sb.append(mXcapUri.getDocumentSelector());
        if (mXcapUri.getNodeSelector() != null) {
            sb.append(RESOURCE_SELECTOR_SEPARATOR).append(mXcapUri.getNodeSelector());
        }
        return sb.toString();
    }

    /**
     * Get node URI.
     *
     * @return URI
     * @throws IllegalArgumentException if illegal argument
     * @throws URISyntaxException if URI syntax error
     */
    public URI getParentUri() throws IllegalArgumentException,
            URISyntaxException {
        URI elementURI;
        XcapNodeSelector elementSelector = new XcapNodeSelector("pcc")
                .queryByNodeName(mParentUri);
        ProfileServiceLog.d(TAG, "getParentUri, elementSelector: " + elementSelector.toString());
        //mParentUri is "person-details"
        //so it is "pcc/person-details/nodename"
        elementURI = mXcapUri.setNodeSelector(elementSelector).toURI();
        return elementURI;
    }

    public URI getPortraitUri() throws IllegalArgumentException,
            URISyntaxException {
        return mXcapUri.toURI();
    }

    protected abstract String getNodeName();
}
