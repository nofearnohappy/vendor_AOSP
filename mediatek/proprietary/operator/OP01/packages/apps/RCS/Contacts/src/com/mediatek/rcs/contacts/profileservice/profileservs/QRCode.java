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

import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.uri.XcapUri;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.auth.Credentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class QRCode extends ProfileServType {

    private static final String TAG = "QRCode";
    private static final String NODE_NAME = "pcc-content";  //the portrait root element is "content"
    //public static final String NAME_SPACE_STR = "urn:oma:xml:prs:prs-content";
    private static final String NODE_CONTENT = "content";
    private static final String NODE_MIME_TYPE = "mime-type";
    private static final String NODE_ENCODING = "encoding";
    private static final String NODE_DESCRIPTION = "description";
    private static final String NODE_DATA = "data";
    private static final String NODE_FLAG = "flag";

    private String mMimeType;
    private String mEncoding;
    private String mDescription;
    private String mData;
    private String mFlag;
    private int mCurrentType;

    /**
     * Constructor.
     *
     * @param documentUri       XCAP document URI
     * @param parentUri         XCAP root directory URI
     * @param intendedId        X-3GPP-Intended-Id
     * @param credential        for authentication
     * @param fromXml whether from existed xml document or not
     * @param param params to init a xml document
     * @throws ProfileXcapException    if XCAP error
     * @throws ParserConfigurationException if parser configuration error
     */
    public QRCode(XcapUri documentUri, String parentUri, String intendedId,
            Credentials credential, boolean fromXml, Object params, int qrCodeType)
                throws ProfileXcapException {
        super(documentUri, parentUri, intendedId, credential, fromXml, params, qrCodeType);
    }

    /**
     * this function is call by super class ProfileServType when set profile
     * format a xml docment, and set the node content by params
     * @ param params is the init value needed to set
     */
    @Override
    public void initServiceInstance(Object params) throws ProfileXcapException {
        Element root = null;
        Element flag = null;
        String flagValue = String.valueOf((int)params);  //the mode to set
        //QRCode is special, it only need <flag> node, and other part is generalized by server
        //so no need to make up the whole xml, just format "<flag></flag>" is ok
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME); //<pcc-content>
            document.appendChild(root);
            flag = (Element) document.createElement(NODE_FLAG); //<flag>
            flag.setTextContent(flagValue);
            root.appendChild(flag);
            mRoot = root;
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
            throw new ProfileXcapException(500);
        }
    }

    /**
     * this function is call by super class ProfileServType when get profile
     * parse the pcc.xml gotten from network
     * @ param domDoc is the pcc.xml gotten from network
     */
    @Override
    public void initServiceInstance(Document domDoc) throws ProfileXcapException {
        //we need check  the document first, because it may be the whole qrcode xml or <flag> only
        NodeList pccContentNode = domDoc.getElementsByTagName(NODE_NAME);
        if (pccContentNode.getLength() > 0) {
            Element pccContentElement = (Element) pccContentNode.item(0); //<pcc-content> node
            //<content>
            NodeList contentNode = pccContentElement.getElementsByTagName(NODE_CONTENT);
            if (contentNode.getLength() > 0) {
                Element contentElement = (Element) contentNode.item(0); //<content> node
                /*<data>*/
                NodeList dataNode = contentElement.getElementsByTagName(NODE_DATA);
                if (dataNode.getLength() > 0) {
                    Element dataElement = (Element)dataNode.item(0);
                    mData = dataElement.getTextContent();
                } else {
                    //no <data> node
                }
                /*<mime-type>*/
                NodeList mimeTypeNode = contentElement.getElementsByTagName(NODE_MIME_TYPE);
                if (mimeTypeNode.getLength() > 0) {
                    Element mimeTypeElement = (Element)mimeTypeNode.item(0);
                    mMimeType = mimeTypeElement.getTextContent();
                } else {
                    //no <mime-type> node
                }
                /*<encoding>*/
                NodeList encodingNode = contentElement.getElementsByTagName(NODE_ENCODING);
                if (encodingNode.getLength() > 0) {
                    Element encodingElement = (Element)encodingNode.item(0);
                    mEncoding = encodingElement.getTextContent();
                } else {
                    //no <encoding> node
                }
                /*<description>*/
                NodeList descriptionNode = contentElement.getElementsByTagName(NODE_DESCRIPTION);
                if (descriptionNode.getLength() > 0) {
                    Element descriptionElement = (Element)descriptionNode.item(0);
                    mDescription = descriptionElement.getTextContent();
                } else {
                    //no <description> node
                }
            } else {
                //no <content> node
            }
            //<flag>
            NodeList flagNode = pccContentElement.getElementsByTagName(NODE_FLAG);
            if (flagNode.getLength() > 0) {
                Element flagElement = (Element) flagNode.item(0); //<flag> node
                mFlag = flagElement.getTextContent();
            } else {
                //no <flag> node
            }
        } else {
            //no <pcc-content> node, maybe only the <flag> node, so check it
            NodeList flagNode = domDoc.getElementsByTagName(NODE_FLAG);
            if (flagNode.getLength() > 0) {
                Element flagElement = (Element) flagNode.item(0); //<flag> node
                mFlag = flagElement.getTextContent();
            } else {
                //no <pcc-content>, no <flag> neither, error
            }
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * get portrait string content.
     *
     * @throws ProfileXcapException if error
     */
    public String getQRCodeData() {
        return mData;
    }

    /**
     * get portrait string content.
     *
     * @throws ProfileXcapException if error
     */
    public String getMimeType() {
        return mMimeType;
    }

    public int getFlag() {
        int flagValue = 0;
        if (mFlag == null) {
            ProfileServiceLog.d(TAG, "mFlag is null");
            return 0;
        } else if (mFlag.isEmpty()) {
            ProfileServiceLog.d(TAG, "mFlag is empty");
            return 0;
        }
        ProfileServiceLog.d(TAG, "mFlag is: " + mFlag);
        try{
            flagValue = Integer.parseInt(mFlag);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        } finally {
            return flagValue;
        }
    }

    /**
     * Save QRCode flag to xml on server.
     *
     * @throws ProfileXcapException if error
     */
    public void setFlag() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setFlag");
        Element flagElement = null;
        String xmlString = null;

        if (mRoot != null) {
            try {
                NodeList flagNode = mRoot.getElementsByTagName(NODE_FLAG);
                if (flagNode.getLength() > 0) {
                    flagElement = (Element)flagNode.item(0);
                } else {
                    ProfileServiceLog.d(TAG, "error: no flag node to set");
                }
                xmlString = domToXmlText(flagElement);
                ProfileServiceLog.d(TAG, "xmlString is : " + xmlString);
                setContent(xmlString, ProfileConstants.CONTENT_TYPE_QRCODE_MODE);
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        } else {
            ProfileServiceLog.d(TAG, "error: no root");
        }
    }
}

