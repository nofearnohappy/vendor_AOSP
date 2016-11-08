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

package com.mediatek.rcs.contacts.profileservice.profileservs;

import android.net.Uri;

import com.android.okhttp.Headers;

import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.XcapClient;
import com.mediatek.xcap.client.uri.XcapUri;

import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.auth.Credentials;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * ProfileServType (PS type) abstract class.
 */
public abstract class ProfileServType extends ProfileXcapElement {

    private static final String TAG = "ProfileServType";
    private static Document mCurrDoc = null;  //static, only on belongs to ProfileServType, actually, it is the pcc.xml 
    private static Document mPortraitDoc = null;
    private static Document mQRCodeDoc = null;
    private static Document mQRCodeMode = null;
    private static final int TYPE_PCC = 0;
    private static final int TYPE_PORTRAIT = 1;
    private static final int TYPE_CONTACT_PORTRAIT = 2;
    private static final int TYPE_QRCODE = 3;
    private static final int TYPE_QRCODE_MODE = 4;

    protected Element mRoot = null;

    //public static final String AUTH_XCAP_3GPP_INTENDED = "X-3GPP-Intended-Identity";

    /**
     * Constructor.
     *
     * @param documentUri       XCAP document URI
     * @param parentUri         XCAP root directory URI
     * @param intendedId        X-3GPP-Intended-Id
     * @param credential        for authentication
     * @param fromXml whether from existed xml document or not
     * @param param params to init a xml document
     * @param param contentType illustrate pcc, or particial or portrait
     * @throws ProfileXcapException    if XCAP error
     * @throws ParserConfigurationException if parser configuration error
     */
    public ProfileServType(XcapUri xcapUri, String parentUri, String intendedId,
            Credentials credential, boolean fromXml, Object params, int contentType)
            throws ProfileXcapException {
        super(xcapUri, parentUri, intendedId, credential);
        /*init the serv in a xml format, get content from db,but not remote server
            the pre-condition is the format is defined by spec and keep constantly*/
        if (fromXml){
            ProfileServiceLog.d(TAG, "ProfileServType, contentType: " + contentType);
            switch (contentType) {
                case ProfileConstants.CONTENT_TYPE_PORTRAIT:
                    loadContentDocument(TYPE_PORTRAIT); //load portrait xml from server
                    if (mPortraitDoc != null) {
                        initServiceInstance(mPortraitDoc);
                        mPortraitDoc = null;
                    }
                    break;

                case ProfileConstants.CONTENT_TYPE_CONTACT_PORTRAIT:
                    loadContentDocument(TYPE_CONTACT_PORTRAIT); //load portrait xml from server
                    if (mPortraitDoc != null) {
                        initServiceInstance(mPortraitDoc);
                        mPortraitDoc = null;
                    }
                    break;

                case ProfileConstants.CONTENT_TYPE_PCC:
                    loadContentDocument(TYPE_PCC); //load pcc xml from server
                    break;

                case ProfileConstants.CONTENT_TYPE_QRCODE:
                    loadContentDocument(TYPE_QRCODE);//load QRCode xml from server
                    initServiceInstance(mQRCodeDoc);
                    mQRCodeDoc = null;
                    break;

                case ProfileConstants.CONTENT_TYPE_QRCODE_MODE:
                    //only get <flag>
                    loadContentDocument(TYPE_QRCODE_MODE);//load QRCode xml from server
                    initServiceInstance(mQRCodeMode);
                    mQRCodeMode = null;
                    break;

                case ProfileConstants.CONTENT_TYPE_PART:
                    if (mCurrDoc != null) {
                        initServiceInstance(mCurrDoc);
                    } else {
                        //error log
                    }
                    break;

                default:
                    //error log
                    break;
            }
        }  else {
            initServiceInstance(params);
        }
    }

    private void loadContentDocument(int contentType) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "loadContentDocument, contentType: " + contentType);
        String xmlContent = getContent(contentType);
        if (xmlContent == null || xmlContent.length() == 0) {
            ProfileServiceLog.d(TAG, "no content gotten");
            throw new ProfileXcapException(500);
        }
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlContent));

            switch (contentType) {
                case TYPE_PORTRAIT:
                case TYPE_CONTACT_PORTRAIT:
                    mPortraitDoc = db.parse(is);
                    break;

                case TYPE_PCC:
                    mCurrDoc = db.parse(is);
                    break;

                case TYPE_QRCODE:
                    mQRCodeDoc = db.parse(is);
                    break;

                case TYPE_QRCODE_MODE:
                    mQRCodeMode = db.parse(is);
                    break;

                default:
                    ProfileServiceLog.d(TAG, "error: unexpected content type");
                    break;
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new ProfileXcapException(500);
        } catch (SAXException e) {
            e.printStackTrace();
            // Throws a server error
            throw new ProfileXcapException(500);
        } catch (IOException e) {
            e.printStackTrace();
            // Throws a server error
            throw new ProfileXcapException(500);
        }
    }

    private String getContent(int contentType) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "ProfileServType getContent");
        String targetUri = null;
        XcapClient xcapClient = new XcapClient();
        //HttpResponse response = null;
        String ret = null;
        //HttpEntity entity;
        HttpURLConnection conn = null;
        Headers.Builder headers = new Headers.Builder();

        try {
            switch (contentType) {
                case TYPE_PCC:
                    headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                    headers.add(HOST_NAME, ProfileXcapElement.mHostName);
                    headers.add(USER_AGENT, "XDM-client/OMA1.0");
                    headers.add(IF_NONE_MATCH, mPccETag);
                    headers.add(CONTENT_TYPE, "application/vnd.oma.cab-pcc+xml");
                    break;

                case TYPE_PORTRAIT:
                    headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                    headers.add(HOST_NAME, ProfileXcapElement.mHostName);
                    headers.add(USER_AGENT, "XDM-client/OMA1.0");
                    headers.add(CONTENT_TYPE, "application/vnd.oma.pres-content+xml");
                    headers.add(X_RESOLUTION, "640");
                    headers.add(IF_NONE_MATCH, mPortraitETag);
                    break;

                case TYPE_CONTACT_PORTRAIT:
                    headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                    headers.add(HOST_NAME, ProfileXcapElement.mHostName);
                    headers.add(USER_AGENT, "XDM-client/OMA1.0");
                    headers.add(CONTENT_TYPE, "application/vnd.oma.pres-content+xml");
                    headers.add(X_RESOLUTION, "640");
                    break;

                case TYPE_QRCODE:
                    headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                    headers.add(HOST_NAME, ProfileXcapElement.mHostName);
                    headers.add(USER_AGENT, "XDM-client/OMA1.0");
                    headers.add(IF_NONE_MATCH, mQRCodeETag);
                    headers.add(CONTENT_TYPE, "application/pcc-content+xml");
                    break;

                case TYPE_QRCODE_MODE:
                    headers.add(AUTH_XCAP_3GPP_INTENDED, "\"" + mIntendedId + "\"");
                    headers.add(HOST_NAME, ProfileXcapElement.mHostName);
                    headers.add(USER_AGENT, "XDM-client/OMA1.0");
                    headers.add(CONTENT_TYPE, "application/xcap-el+xml");
                    //dont put eatag for qrcode mode
                    break;

                default:
                    ProfileServiceLog.d(TAG, "error: unexpected type");
                    break;
            }
            if (mCredentials == null) {
                ProfileServiceLog.d(TAG, "mCredentials is null");
            }

            //xcapClient.setAuthenticationCredentials(mCredentials);

            /*actually, the pcc, portrait and qrcode are same, because they are
                    both get the whole xml document file,no node information is needed
                    but for qrcode mode, we need direct to the node <pcc-content>, so use getNodeUri*/
            if (contentType == TYPE_QRCODE_MODE) {
                //targetUri = getNodeUri(true).toString();
                targetUri = getNodePath(true);
            } else if (contentType == TYPE_PORTRAIT || contentType == TYPE_CONTACT_PORTRAIT ||
                    contentType == TYPE_QRCODE) {
                targetUri = toURIString();
            } else {
                //targetUri = mXcapUri.toURI().toString();
                targetUri = toURIString();
            }
            //begin
            conn = xcapClient.get(new URI(targetUri), headers.build());
            if (conn != null) {
                if (conn.getResponseCode() == 200) {
                    InputStream is = null;
                    String eTagValue = null;
                    try {
                        int contentLen = conn.getContentLength();
                        if (contentLen == -1) {
                            ProfileServiceLog.d(TAG, "contentLen is -1");
                            ret = null;
                        } else {
                            is = conn.getInputStream();
                            // convert stream to string
                            ret = convertStreamToString(is);
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                    eTagValue = conn.getHeaderField(ETAG);
                    if (eTagValue != null) {
                        switch (contentType) {
                            case TYPE_PCC:
                                mPccETag = eTagValue;
                                ProfileServiceLog.d(TAG, "mPccETag: " + mPccETag);
                                break;
                            case TYPE_PORTRAIT:
                                mPortraitETag = eTagValue;
                                ProfileServiceLog.d(TAG, "mPortraitETag: " + mPortraitETag);
                                break;
                            case TYPE_QRCODE:
                                mQRCodeETag = eTagValue;
                                ProfileServiceLog.d(TAG, "mQRCodeETag: " + mQRCodeETag);
                                break;
                            default:
                                ProfileServiceLog.d(TAG, "error: unexpected type for ETag");
                                break;
                        }
                    }
                } else {
                    ret = null;
                    throw new ProfileXcapException(conn.getResponseCode());
                }
            }else {
                ProfileServiceLog.d(TAG, "conn is null");
                throw new ProfileXcapException(new ConnectException());
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            ProfileServiceLog.d(TAG, "catch IOException on getContent");
            e.printStackTrace();
            throw new ProfileXcapException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            xcapClient.shutdown();
        }
        return ret;
    }

    protected Element getRootElement () {
        return mRoot;
    }

    /**
     * this function is call by super class ProfileServType when set profile
     * format a xml docment, and set the node content by params
     * abstract function, every subclass need implement this function
     * @ param params is the init value needed to set
     */
    public abstract void initServiceInstance(Object params) throws ProfileXcapException ;

    /**
     * this function is call by super class ProfileServType when get profile
     * parse the pcc.xml gotten from network
     * abstract function, every subclass need implement this function
     * @ param domDoc is the pcc.xml gotten from network
     */
    public abstract void initServiceInstance(Document domDoc) throws ProfileXcapException;
}
