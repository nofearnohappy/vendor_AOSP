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

import com.mediatek.rcs.contacts.profileservice.profileservs.element.AddressEntry;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.AddressParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.uri.XcapUri;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.auth.Credentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Address extends ProfileServType {

    public static final String NODE_NAME = "address";
    private static final String TAG = "address";

    //should not new instance here, because it would be renew here after superClass' constuctor, just declared here
    //private List<AddressEntry> mAddressEnrtys = new ArrayList<AddressEntry>();
    private List<AddressEntry> mAddressEnrtys; 

    public static final int ADDR_TYPE_ALL = 0;
    public static final int ADDR_TYPE_HOME = 1;
    public static final int ADDR_TYPE_WORK = 2;

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
    public Address(XcapUri documentUri, String parentUri, String intendedId,
            Credentials credential, boolean fromXml, Object param) throws ProfileXcapException {
        super(documentUri, parentUri, intendedId, credential, fromXml, param,
                ProfileConstants.CONTENT_TYPE_PART);
    }

    /**
     * this function is call by super class ProfileServType when set profile
     * format a xml docment, and set the node content by params
     * @ param params is the init value needed to set
     */
    @Override
    public void initServiceInstance(Object params) throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "initServiceInstance 1");
        Element root = null;
        AddressParams addrParams = (AddressParams)params;
        int addrType = addrParams.mAddrType;

        AddressEntry homeAddr = null;
        AddressEntry workAddr = null;
        Element homeEntryElement = null;
        Element workEntryElement = null;
        String parentUri = mParentUri + "/" + NODE_NAME;
        if (mAddressEnrtys == null) {
            mAddressEnrtys = new ArrayList<AddressEntry>();
        } else {
            ProfileServiceLog.d(TAG, "error: mAddressEnrtys should null here");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME); //address
            document.appendChild(root);

            switch (addrType) {
                case ADDR_TYPE_ALL:
                    //home address entry
                    homeAddr = new AddressEntry(mXcapUri, parentUri, mIntendedId,
                            mCredentials, "Home");
                    mAddressEnrtys.add(homeAddr);
                    homeEntryElement = homeAddr.toXmlElement(document, addrParams);
                    root.appendChild(homeEntryElement);
                    //work address entry
                    workAddr = new AddressEntry(mXcapUri, parentUri, mIntendedId,
                            mCredentials, "Work");
                    mAddressEnrtys.add(workAddr);
                    workEntryElement = workAddr.toXmlElement(document, addrParams);
                    root.appendChild(workEntryElement);
                    break;

                case ADDR_TYPE_HOME:
                    homeAddr = new AddressEntry(mXcapUri, parentUri, mIntendedId,
                            mCredentials, "Home");
                    mAddressEnrtys.add(homeAddr);
                    homeEntryElement = homeAddr.toXmlElement(document, addrParams);
                    root.appendChild(homeEntryElement);
                    break;

                case ADDR_TYPE_WORK:
                    workAddr = new AddressEntry(mXcapUri, parentUri, mIntendedId,
                            mCredentials, "Work");
                    mAddressEnrtys.add(workAddr);
                    workEntryElement = workAddr.toXmlElement(document, addrParams);
                    root.appendChild(workEntryElement);
                    break;

                default:
                    break;
            }

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
    public void initServiceInstance(Document domDoc) throws ProfileXcapException{
        ProfileServiceLog.d(TAG, "initServiceInstance 2");

        if (mAddressEnrtys == null) {
            mAddressEnrtys = new ArrayList<AddressEntry>();
        } else {
            ProfileServiceLog.d(TAG, "error: mAddressEnrtys should null here");
        }

        AddressEntry addressEntry = null;
        NodeList addressNode = domDoc.getElementsByTagName(NODE_NAME);
        if (addressNode.getLength() > 0) {
            Element addressElement = (Element) addressNode.item(0);
            NodeList addressEntryNode = addressElement.getElementsByTagName(AddressEntry.NODE_NAME);
            if (addressEntryNode.getLength() > 0) {
                for (int i = 0; i < addressEntryNode.getLength(); i++ ){
                    Element entryElement = (Element)addressEntryNode.item(i);
                    //dont need to the type here, because it from remote server
                    //String addrTypeValue = entryElement.getAttribute(AddressEntry.ATTR_ADDR_TYPE);
                    addressEntry = new AddressEntry (mXcapUri, NODE_NAME, mIntendedId,
                            mCredentials, entryElement);
                    mAddressEnrtys.add(addressEntry);
                    ProfileServiceLog.d(TAG, "mAddressEnrtys.add");
                }
            }else {
            //error, should have <address-entry> node
                ProfileServiceLog.d(TAG, "error, no <address-entry> node");
            }
        } else {
            //error, should have <address> node
            ProfileServiceLog.d(TAG, "error, no <address> node");
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * getHomeAddress
     */
    public String getHomeAddress() {
        int addrCnt = mAddressEnrtys.size();
        ProfileServiceLog.d(TAG, "getHomeAddress, addrCnt: " + addrCnt);
        String homeAddress = null;
        for (int i = 0; i < mAddressEnrtys.size(); i++) {
            AddressEntry addrEntry = mAddressEnrtys.get(i);
            if (addrEntry.getAddressType().equals("Home")) {
                homeAddress = addrEntry.getAddressData().getAddress();
            }
        }
        return homeAddress;
    }

    /**
     * getWorkAddress
     */
    public String getWorkAddress() {
        ProfileServiceLog.d(TAG, "getWorkAddress");
        String workAddress = null;
        for (int i = 0; i < mAddressEnrtys.size(); i++) {
            AddressEntry addrEntry = mAddressEnrtys.get(i);
            if (addrEntry.getAddressType().equals("Work")) {
                workAddress = addrEntry.getAddressData().getAddress();
            }
        }
        return workAddress;
    }

    /**
     * Save setHomeAddress on server.
     *
     * @throws ProfileXcapException if error
     */
    public void setHomeAddress() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setHomeAddress");
        String homeAddrXml = null;
        AddressEntry homeAddrEntry = null;
        /*because setHomeAddress and setWorkAddress is done respectively,
            there are 2 Address instances for every type
            mAddressEnrtys should only hava homeAddrEntry here*/
        if (mAddressEnrtys.size() > 0)
        {
             homeAddrEntry = mAddressEnrtys.get(0);
            //check entry address-type
            if (homeAddrEntry.getAddressType().equals("Home")) {
                homeAddrXml = homeAddrEntry.toXmlString(mRoot);
            } else {
                ProfileServiceLog.d(TAG, "error: not home type");
                return;
            }
        } else {
            ProfileServiceLog.d(TAG, "error: not no addressEntry");
            return;
        }
        ProfileServiceLog.d(TAG, "homeAddrXml is : " + homeAddrXml);
        homeAddrEntry.setContent(homeAddrXml, ProfileConstants.CONTENT_TYPE_PART);
    }

    /**
     * Save setWorkAddress on server.
     *
     * @throws ProfileXcapException if error
     */
    public void setWorkAddress() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setWorkAddress");
        String workAddrXml = null;
        AddressEntry workAddrEntry = null;
        /*because setHomeAddress and setWorkAddress is done respectively,
            there are 2 Address instances for every type
            mAddressEnrtys should only hava workAddrEntry here*/
        if (mAddressEnrtys.size() > 0) {
            workAddrEntry = mAddressEnrtys.get(0);
            //check entry address-type
            if (workAddrEntry.getAddressType().equals("Work")) {
                workAddrXml = workAddrEntry.toXmlString(mRoot);
            } else {
                ProfileServiceLog.d(TAG, "error: not work type");
                return;
            }
        } else {
            ProfileServiceLog.d(TAG, "error: not no addressEntry");
            return;
        }
        ProfileServiceLog.d(TAG, "workAddrXml is : " + workAddrXml);
        workAddrEntry.setContent(workAddrXml, ProfileConstants.CONTENT_TYPE_PART);
    }
}
