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

import com.mediatek.rcs.contacts.profileservice.profileservs.params.AddressParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileConfigureType;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.xcap.client.uri.XcapUri;

import org.apache.http.auth.Credentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;

public class AddressEntry extends ProfileXcapElement implements ProfileConfigureType {

    public static final String NODE_NAME = "address-entry";
    public static final String ATTR_PREF = "pref";
    public static final String ATTR_LANGUAGE = "xml:lang";
    public static final String ATTR_ADDR_TYPE = "address-type";
    public static final String NODE_LABEL = "label";
    

    private AddressData mAddressData;
    private String mAddrType;
    private String mLabel;
    /**
     * Constructor.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     * @param credential    for authentication
     */
    public AddressEntry(XcapUri xcapUri, String parentUri, String intendedId,
            Credentials credential, String addrType) {
        super(xcapUri, parentUri, intendedId, credential);
        mAddrType = addrType;
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
    public AddressEntry(XcapUri xcapUri, String parentUri, String intendedId,
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
     * @param domNode is the current node: <address-entry> node
     */
    @Override
    public void instantiateFromXmlNode(Node domNode) {

        Element domElement = (Element) domNode;  //node is "<address-entry>"
        mAddrType = domElement.getAttribute(ATTR_ADDR_TYPE);
        NodeList addressDataNodes = domElement.getElementsByTagName(AddressData.NODE_NAME);
        if(addressDataNodes.getLength() > 0) {
            Element addressData = (Element)addressDataNodes.item(0); //<address-data> node
            mAddressData = new AddressData (mXcapUri, NODE_NAME, mIntendedId,
                    mCredentials, addressData);
            //ProfileServiceLog.d(TAG, "new AddressData");
        } else {
            //error: no <address-data> node
        }
        NodeList labelNodes = domElement.getElementsByTagName(NODE_LABEL); //<label> node
        if(labelNodes.getLength() > 0) {
            Element labelElement = (Element)labelNodes.item(0);
            mLabel = labelElement.getTextContent();
        } else {
            //no label node
        }
        //check type
        if (mAddrType.equals("Home")) {
            //check mLabel
        }
    }

    /**
    * getAddressType.
    * @return addressType
    */
    public String getAddressType() {
        return mAddrType; //upset
    }

    /**
    * getAddressData.
    * @return addressData String
    */
   public AddressData getAddressData () {
        return mAddressData;
    }

    /**
     * Convert to XML element.
     *
     * @param document DOM document
     * @param params AddressParams contains address info
     * @return XML element
     */
    public Element toXmlElement(Document document, AddressParams params) {
        Element addressEntryNode = (Element) document.createElement(NODE_NAME);
        if (mAddrType.equals("Home")){
            addressEntryNode.setAttribute(ATTR_PREF, "1");
            addressEntryNode.setAttribute(ATTR_ADDR_TYPE, "Home");
        } else if (mAddrType.equals("Work")) {
            addressEntryNode.setAttribute(ATTR_PREF, "2");
            addressEntryNode.setAttribute(ATTR_ADDR_TYPE, "Work");
        }
        addressEntryNode.setAttribute(ATTR_LANGUAGE, "zh-CN");
        //construct <address-data> node
        mAddressData = new AddressData (mXcapUri, NODE_NAME, mIntendedId, mCredentials);
        if (mAddrType.equals("Home")){
            mAddressData.setAddress(params.mHomeAddress);   //set address string content
            mLabel = "Home"; //need conform, server is not accordance to spec, it is "address" in spec
        } else if (mAddrType.equals("Work")) {
            mAddressData.setAddress(params.mWorkAddress);   //set address string content
            mLabel = "Work"; //need conform, server is not accordance to spec, it is "office" in spec
        } else {
            //error
        }
        Element addressDataElement = mAddressData.toXmlElement(document);
        //add <address-data> to <address-entry>
        addressEntryNode.appendChild(addressDataElement);
        //construct <label> node
        Element labelElement = (Element) document.createElement(NODE_LABEL);
        labelElement.setTextContent(mLabel);
        addressEntryNode.appendChild(labelElement);
        return addressEntryNode;
    }

    /**
     * Convert current node  <address-entry> to XML string.
     *
     * @return  XML string
     */
    public String toXmlString(Element root) {
        //Element root = null;
        Element AddrEntryElement = null;
        String xmlString = null;
        try {
            NodeList AddrEntryNode = root.getElementsByTagName(NODE_NAME);
            if (AddrEntryNode.getLength() > 0) {
                AddrEntryElement = (Element)AddrEntryNode.item(0);
            }
            xmlString = domToXmlText(AddrEntryElement);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return xmlString;
    }
}
