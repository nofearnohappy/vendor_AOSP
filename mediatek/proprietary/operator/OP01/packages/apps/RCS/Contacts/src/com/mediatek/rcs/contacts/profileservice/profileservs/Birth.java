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

import com.mediatek.rcs.contacts.profileservice.profileservs.element.BirthDate;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.BirthParams;
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

public class Birth extends ProfileServType {

    private static final String TAG = "Birth";
    public static final String NODE_NAME = "birth";

    BirthDate mBirthDate;

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
    public Birth(XcapUri documentUri, String parentUri, String intendedId,
            Credentials credential, boolean fromXml, Object params) throws ProfileXcapException {
        super(documentUri, parentUri, intendedId, credential,
                fromXml, params, ProfileConstants.CONTENT_TYPE_PART);
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
        BirthParams birthParams = (BirthParams)params;
        String date = birthParams.mBirthDate; //birthdate string content
        String parentUri = mParentUri + "/" + NODE_NAME;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME); //birth
            root.setAttribute("xml:lang", "en");
            document.appendChild(root);
            if (mBirthDate == null) {
                mBirthDate = new BirthDate(mXcapUri, parentUri, mIntendedId, mCredentials);
            }
            mBirthDate.setDate(date);
            Element birthDateElement = mBirthDate.toXmlElement(document);  //add birth-date as birth' child
            root.appendChild(birthDateElement);
            //save <birth> node as root for futher use
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
        ProfileServiceLog.d(TAG, "initServiceInstance 2");
        NodeList birthNode = domDoc.getElementsByTagName(NODE_NAME);
        if (birthNode.getLength() > 0) {
            Element birthElement = (Element) birthNode.item(0); //birth node
            NodeList birthDateNode = birthElement.getElementsByTagName(BirthDate.NODE_NAME);
            if (birthDateNode.getLength() > 0) {
                Element birthDateElement = (Element)birthDateNode.item(0);
                mBirthDate = new BirthDate(mXcapUri, NODE_NAME, mIntendedId,
                        mCredentials, birthDateElement);
                if (mBirthDate != null) {
                    ProfileServiceLog.d(TAG, "mBirthDate is new, not null here");
                }
            } else {
                ProfileServiceLog.d(TAG, "error: no <birth-date> node");
                mBirthDate = new BirthDate(mXcapUri, NODE_NAME, mIntendedId, mCredentials);
            }
        } else {
            ProfileServiceLog.d(TAG, "error: no <birth> node");
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * getBirthDate.
     *
     * @return birth date
     */
    public String getBirthDate() {
        if(mBirthDate != null) {
            return mBirthDate.getDate();
        } else {
            ProfileServiceLog.d(TAG, "damn! why null here?");
        }
        return null;
    }

    /**
     * setBirthDate.
     *
     * @throws ProfileXcapException if error happened when save data on server
     */
    public void setBirthDate() throws ProfileXcapException {
        String birthDateXml = mBirthDate.toXmlString(mRoot);
        ProfileServiceLog.d(TAG, "setBirthDate, birthDateXml is : " + birthDateXml);
        mBirthDate.setContent(birthDateXml, ProfileConstants.CONTENT_TYPE_PART);
    }
}
