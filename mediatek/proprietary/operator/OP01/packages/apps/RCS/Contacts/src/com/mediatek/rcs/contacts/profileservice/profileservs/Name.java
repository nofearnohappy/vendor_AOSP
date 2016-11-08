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

import com.mediatek.rcs.contacts.profileservice.profileservs.element.NameEntry;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.NameParams;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapElement;
import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.uri.XcapUri;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.auth.Credentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Name extends ProfileServType {

    public static final String NODE_NAME = "name";
    public static final String TAG = "Name";
    //should not set initialiazed to null, because it will new instance at superclass' constructor
    //superclass' constructor will be run befor here, so it should only declared here but not set to null
    public NameEntry mNameEntry;

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
    public Name(XcapUri documentUri, String parentUri, String intendedId,
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
    public void initServiceInstance(Object params) throws ProfileXcapException {
        //actually,the domDoc content is form network
        //should be modified, init should get info from local db
        ProfileServiceLog.d(TAG, "initServiceInstance 1");
        Element root = null;
        NameParams nameParams = (NameParams)params;
        String firstName = nameParams.mFirstName; //first string content
        String familyName = nameParams.mfamilyName; //family string content
        String parentUri = mParentUri + "/" + NODE_NAME;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME); //name
            document.appendChild(root);
            if (mNameEntry == null) {
                mNameEntry = new NameEntry(mXcapUri, parentUri, mIntendedId, mCredentials);
            }
            mNameEntry.setName(firstName, familyName);
            Element nameEntryElement = mNameEntry.toXmlElement(document);
            root.appendChild(nameEntryElement);
            //save <name> node as root for futher use
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
        ProfileServiceLog.d(TAG, "initServiceInstance 2");
        //actually,the domDoc content is form network
        NodeList nameNode = domDoc.getElementsByTagName(NODE_NAME);
        if (nameNode.getLength() > 0) {
            Element nameElement = (Element) nameNode.item(0); //<name> node
            NodeList nameEntryNode = nameElement.getElementsByTagName(NameEntry.NODE_NAME);
            if (nameEntryNode.getLength() > 0) {
                Element nameEntryElement = (Element)nameEntryNode.item(0);
                mNameEntry = new NameEntry(mXcapUri, NODE_NAME, mIntendedId,
                        mCredentials, nameEntryElement);
                ProfileServiceLog.d(TAG, "new NameEntry, save to mNameEntry");
                if (mNameEntry != null) {
                    ProfileServiceLog.d(TAG, "mNameEntry is set successfully, not null");
                    ProfileServiceLog.d(TAG, "mNameEntry is: " + mNameEntry.toString());
                }
            } else {
                //no <name-entry> node
                ProfileServiceLog.d(TAG, "no <name-entry> node");
                mNameEntry = new NameEntry(mXcapUri, NODE_NAME, mIntendedId, mCredentials);
            }
        } else {
            //no <name> node, error
            ProfileServiceLog.d(TAG, "no <name> node");
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * getName content.
     *
     * @return name string
     */
    public String getFirstName() {
        ProfileServiceLog.d(TAG, "getFirstName");
        if (mNameEntry == null) {
            ProfileServiceLog.d(TAG, "mNameEntry is null");
            return null;
        }
        return mNameEntry.getFirstName();
    }

    /**
     * getName content.
     *
     * @return name string
     */
    public String getFamilyName() {
        ProfileServiceLog.d(TAG, "getFamilyName");
        if (mNameEntry == null) {
            ProfileServiceLog.d(TAG, "mNameEntry is null");
            return null;
        }
        return mNameEntry.getFamilyName();
    }

    /**
     * Save Name to configuration on server.
     *
     * @throws ProfileXcapException if error
     */
    public void setName() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setName");
        if (mRoot != null) {
            if (mNameEntry == null) {
                ProfileServiceLog.d(TAG, "mNameEntry is null");
                return;
            }
            String nameEntryXml = mNameEntry.toXmlString(mRoot);
            ProfileServiceLog.d(TAG, "nameEntryXml is : " + nameEntryXml);
            mNameEntry.setContent(nameEntryXml, ProfileConstants.CONTENT_TYPE_PART);
        } else {
            //error: no root
        }
    }
}

