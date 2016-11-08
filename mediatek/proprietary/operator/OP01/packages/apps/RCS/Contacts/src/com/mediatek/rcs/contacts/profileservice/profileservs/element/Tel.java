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
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
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

public class Tel extends ProfileXcapElement implements ProfileConfigureType {

    private static final String TAG = "Tel";

    public static final String NODE_NAME = "tel";
    public static final String ATTR_PREF = "pref";
    public static final String ATTR_LANGUAGE = "xml:lang";
    public static final String ATTR_TEL_TYPE = "tel-type";
    public static final String NODE_LABEL = "label";

    private TelNb mTelNb = null;
    private String mTelType = null;
    private String mLabel = null;
    private int mPref = 0;

    /**
     * Constructor.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     * @param credential    for authentication
     * @param addrType    for addrType
     * @param perf    for perfnumber
     * @param otherLabel    for otherNumber's Label
     */
    public Tel(XcapUri xcapUri, String parentUri, String intendedId,
            Credentials credential, String addrType, int perf, String otherLabel) {
        super(xcapUri, parentUri, intendedId, credential);
        mTelType = addrType;
        mPref = perf;
        if (otherLabel != null) {
            mLabel = otherLabel;
        }
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
    public Tel(XcapUri xcapUri, String parentUri, String intendedId, Credentials credential,
                   Element domElement) {
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
     * @param domNode is the current node: <tel> node
     */
    @Override
    public void instantiateFromXmlNode(Node domNode) {
        Element domElement = (Element) domNode;  //node is "<tel>"
        String attrTelType = domElement.getAttribute(ATTR_TEL_TYPE);
        String perf = domElement.getAttribute(ATTR_PREF);
        mPref = Integer.parseInt(perf);

        NodeList telNbNodes = domElement.getElementsByTagName(TelNb.NODE_NAME);
        if(telNbNodes.getLength() > 0) {
            Element telNbElement = (Element)telNbNodes.item(0); //<tel-nb> node
            mTelNb = new TelNb (mXcapUri, NODE_NAME, mIntendedId, mCredentials, telNbElement);
        } else {
            ProfileServiceLog.d(TAG, "error: no <label> node");
        }
        NodeList labelNodes = domElement.getElementsByTagName(NODE_LABEL); //<label> node
        if(labelNodes.getLength() > 0) {
            Element labelElement = (Element)labelNodes.item(0);
            mLabel = labelElement.getTextContent();
        } else {
            ProfileServiceLog.d(TAG, "error: no <label> node");
        }
        if (attrTelType.equalsIgnoreCase("Fax")) {
            mTelType = ProfileConstants.TEL_TYPE_FAX;
        } else if (attrTelType.equalsIgnoreCase("Work")) {
            mTelType = ProfileConstants.TEL_TYPE_WORK;
        } else if (attrTelType.equalsIgnoreCase("Other")) {
            mTelType = ProfileConstants.TEL_TYPE_OTHER;
        } else {
            ProfileServiceLog.d(TAG, "error: wrong tel-attr-type");
        }
        if (mTelType == null) {
            ProfileServiceLog.d(TAG, "error: mTelType is not set correctly");
        }
    }

    public String getTelType () {
        return mTelType;
    }

    public TelNb getTelNb () {
        return mTelNb;
    }

    public String getLabel () {
        return mLabel;
    }

    /**
     * Convert to XML element.
     *
     * @param document DOM document
     * @param number  number string to set
     * @return XML element
     */
    public Element toXmlElement(Document document, String number) {
        Element TelNode = (Element) document.createElement(NODE_NAME);
        TelNode.setAttribute(ATTR_PREF, String.valueOf(mPref));

        if (mTelType.equals(ProfileConstants.TEL_TYPE_FAX)) {
            TelNode.setAttribute(ATTR_TEL_TYPE, "Fax");
            mLabel = "office Fax";
        } else if (mTelType.equals(ProfileConstants.TEL_TYPE_WORK)) {
            TelNode.setAttribute(ATTR_TEL_TYPE, "Work");
            mLabel = "office Phone";
        } else if (mTelType.equals(ProfileConstants.TEL_TYPE_OTHER)) {
            TelNode.setAttribute(ATTR_TEL_TYPE, "Other");
            //for other number, mLabel is already set in constructor;
        } else {
            ProfileServiceLog.d(TAG, "error: mTelType is not an expected one");
            TelNode.setAttribute(ATTR_TEL_TYPE, "Other");
            mLabel = "other";
        }
        //TelNode.setAttribute(ATTR_PREF, String.valueOf(mPref));
        //construct <tel-nb> node
        mTelNb = new TelNb(mXcapUri, NODE_NAME, mIntendedId, mCredentials);
        mTelNb.setTelNumber(number);
        Element telNbElement = mTelNb.toXmlElement(document);
        //add <address-data> to <address-entry>
        TelNode.appendChild(telNbElement);
        //construct <label> node
        Element labelElement = (Element) document.createElement(NODE_LABEL);
        labelElement.setTextContent(mLabel);
        TelNode.appendChild(labelElement);
        return TelNode;
    }

    /**
    * Convert current node  <tel> to XML string.
    *
    * @return  XML string
    */
     public String toXmlString(Element root) {
        //Element root = null;
        Element telElement = null;
        String xmlString = null;
        try {
            NodeList TelNode = root.getElementsByTagName(NODE_NAME);
            if (TelNode.getLength() > 0) {
                telElement = (Element)TelNode.item(0);
            }
            xmlString = domToXmlText(telElement);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return xmlString;
    }

    /**
    * Convert current node  <tel> to XML string.
    * special for other number, cause more than 1 other number
    * @param index indicate the number idex
    * @return  XML string
    */
    public String toXmlString(Element root, int index) {
        //Element root = null;
        int nodeCnt = 0;
        Element telElement = null;
        String xmlString = null;
        try {
            NodeList TelNode = root.getElementsByTagName(NODE_NAME);
            nodeCnt = TelNode.getLength();
            //check the count
            if (index < nodeCnt) {
                telElement = (Element)TelNode.item(index);
                xmlString = domToXmlText(telElement);
            } else {
                //error
                ProfileServiceLog.d(TAG, "index is exceed the count of <node>");
            }
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return xmlString;
    }
}
