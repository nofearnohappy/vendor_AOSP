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

package com.mediatek.rcs.contacts.profileservice.profileservs.element;

import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileConfigureType;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.uri.XcapUri;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import org.apache.http.auth.Credentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NameEntry extends ProfileXcapElement implements ProfileConfigureType {

    public static final String NODE_NAME = "name-entry";
    private static final String FIRST_NAME = "first";
    private static final String FAMILY_NAME = "family";
    private static final String ATTR_PREF = "pref";
    private static final String ATTR_LANGUAGE = "xml:lang";
    private static final String ATTR_NAME_TYPE = "name-type";
    public static final String TAG = "NameEntry";
    private String mFirst = null;
    private String mFamily = null;

    /**
     * Constructor.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     * @param credential    for authentication
     */
    public NameEntry(XcapUri xcapUri, String parentUri, String intendedId,
            Credentials credential) {
        super(xcapUri, parentUri, intendedId, credential);
    }

    /**
     * Constructor with XML element.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     * @param credential    for authentication
     * @param domElement    DOM element
     */
    public NameEntry(XcapUri xcapUri, String parentUri, String intendedId,
            Credentials credential, Element domElement) {
        super(xcapUri, parentUri, intendedId, credential);
        instantiateFromXmlNode(domElement);
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * implements the function of interface ProfileConfigureType
     * only private used
     * @param domNode is the current node: <name-entry> node
     */
    @Override
    public void instantiateFromXmlNode(Node domNode) {
        ProfileServiceLog.d(TAG, "instantiateFromXmlNode");
        Element domElement = (Element) domNode; //node is nameEntry
        NodeList firstNode = domElement.getElementsByTagName(FIRST_NAME);
        if (firstNode.getLength() > 0) {
            Element firstElement= (Element)firstNode.item(0);
            mFirst= firstElement.getTextContent();
            ProfileServiceLog.d(TAG, "mFirst is " + mFirst);
        } else {
            //no <first> node
        }
        NodeList familyNode = domElement.getElementsByTagName(FAMILY_NAME);
        if (familyNode.getLength() > 0) {
            Element familyElement = (Element)familyNode.item(0);
            mFamily= familyElement.getTextContent();
            ProfileServiceLog.d(TAG, "mFamily is " + mFamily);
        } else {
            //no <family> node
        }
    }

    /**
     * Convert to XML element.
     *
     * @param document DOM document
     * @return XML element
     */
    public Element toXmlElement(Document document) {
        /*param "document" is element "name"*/
        Element nameEntryNode = (Element) document.createElement(NODE_NAME); //<name-entry>
        nameEntryNode.setAttribute(ATTR_PREF, "1");
        nameEntryNode.setAttribute(ATTR_LANGUAGE, "zh-CN");
        nameEntryNode.setAttribute(ATTR_NAME_TYPE, "LegalName");
        Element firstElement = (Element) document.createElement(FIRST_NAME);
        Element familyElement = (Element) document.createElement(FAMILY_NAME);
        firstElement.setTextContent(mFirst);
        familyElement.setTextContent(mFamily);
        nameEntryNode.appendChild(firstElement);
        nameEntryNode.appendChild(familyElement);
        return nameEntryNode;
    }

    /**
    * getName.
    *
    * @return  name string
    */
    public String getFirstName() {
        //need to confirm
        return mFirst;
    }

    public String getFamilyName() {
        //need to confirm
        return mFamily;
    }

    /**
    * setName.
    * @param firstName
    * @param familyName
    */
    public void setName(String firstName, String familyName) {
        mFirst = firstName;
        mFamily = familyName;
    }

    /**
    * Convert current node  <name-entry> to XML string.
    *
    * @return  XML string
    */
    public String toXmlString(Element root) {
        //Element root = null;
        Element NameEntryElement = null;
        String xmlString = null;
        try {
            NodeList NameEntryNode = root.getElementsByTagName(NODE_NAME);
            if (NameEntryNode.getLength() > 0) {
                NameEntryElement = (Element)NameEntryNode.item(0);
            }
            xmlString = domToXmlText(NameEntryElement);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return xmlString;
    }
}

