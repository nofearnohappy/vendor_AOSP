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

import com.mediatek.rcs.contacts.profileservice.profileservs.params.PortraitParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
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

public class Portrait extends ProfileServType {

    public static final String NODE_NAME = "content";  //the portrait root element is "content"
    public static final String NAME_SPACE_STR = "urn:oma:xml:prs:pres-content";
    public static final String NODE_MIME_TYPE = "mime-type";
    public static final String NODE_ENCODING = "encoding";
    public static final String NODE_DATA = "data";

    private String mMimeType;
    private String mEncoding;
    private String mData;

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
    public Portrait(XcapUri documentUri, String parentUri, String intendedId,
            Credentials credential, boolean fromXml, Object params, int portraitType)
            throws ProfileXcapException {
        super(documentUri, parentUri, intendedId, credential, fromXml, params, portraitType);
    }

    /**
     * this function is call by super class ProfileServType when set profile
     * format a xml docment, and set the node content by params
     * @ param params is the init value needed to set
     */
    @Override
    public void initServiceInstance(Object params) throws ProfileXcapException {
        Element root = null;
        Element mimeType = null;
        Element encoding = null;
        Element data = null;
        PortraitParams portraitParams = (PortraitParams)params;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElementNS(NAME_SPACE_STR,NODE_NAME); //<content>
            document.appendChild(root);
            mimeType = document.createElement(NODE_MIME_TYPE);
            mimeType.setTextContent(portraitParams.mMimeType);
            encoding = document.createElement(NODE_ENCODING);
            encoding.setTextContent(portraitParams.mEncoding);
            data = document.createElement(NODE_DATA);
            data.setTextContent(portraitParams.mData);
            //add every "profileServTpe" node to <person-details>
            root.appendChild(mimeType);
            root.appendChild(encoding);
            root.appendChild(data);
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
        //actually,the domDoc content is form network
        NodeList contentNode = domDoc.getElementsByTagName(NODE_NAME);
        if (contentNode.getLength() > 0) {
            Element contentElement = (Element) contentNode.item(0); //content node
            NodeList dataNode = contentElement.getElementsByTagName(NODE_DATA);
            if (dataNode.getLength() > 0) {
                Element dataElement = (Element)dataNode.item(0);
                mData = dataElement.getTextContent();
            } else {
                //no <data> node
            }
            NodeList mimeTypeNode = contentElement.getElementsByTagName(NODE_MIME_TYPE);
            if (mimeTypeNode.getLength() > 0) {
                Element mimeTypeElement = (Element)mimeTypeNode.item(0);
                mMimeType = mimeTypeElement.getTextContent();
            } else {
                //no <mime-type> node
            }
            NodeList encodingNode = contentElement.getElementsByTagName(NODE_ENCODING);
            if (encodingNode.getLength() > 0) {
                Element encodingElement = (Element)encodingNode.item(0);
                mEncoding = encodingElement.getTextContent();
            } else {
                //no <encoding> node
            }
        } else {
            //no <content> node, error
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
    public String getPortrait() {
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

    /**
     * Save portrait to xml on server.
     *
     * @throws ProfileXcapException if error
     */
    public void setPortrait() throws ProfileXcapException {
        String portraitXml = null;
        try {
            portraitXml = domToXmlText(mRoot);
            this.setContent(portraitXml, ProfileConstants.CONTENT_TYPE_PORTRAIT);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}

