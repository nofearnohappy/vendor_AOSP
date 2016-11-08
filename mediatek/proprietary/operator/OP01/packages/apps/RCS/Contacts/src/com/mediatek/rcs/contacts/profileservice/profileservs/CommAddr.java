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

import com.mediatek.rcs.contacts.profileservice.profileservs.element.Tel;
import com.mediatek.rcs.contacts.profileservice.profileservs.element.UriEntry;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.CommAddrParams;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CommAddr extends ProfileServType {

    public static final String NODE_NAME = "comm-addr";
    private static final String TAG = "CommAddr";
    private List<Tel> mTels; //should not set to null
    private List<Tel> mOtherTels; //should not set to null
    private UriEntry mUriEntry;  //should not set to null

    //private String mNodeUri = mParentUri + "/" + NODE_NAME; //not contain document name
    public static final int NUMBER_NONE = 0;
    public static final int NUMBER_TYPE_OFFICE = 1;
    public static final int NUMBER_TYPE_FAX = 2;
    public static final int NUMBER_TYPE_OTHER = 3;
    public static final int NUMBER_TYPE_MULTI = 4;

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
    public CommAddr(XcapUri documentUri, String parentUri, String intendedId,
            Credentials credential, boolean fromXml, Object param) throws ProfileXcapException {
        super(documentUri, parentUri, intendedId, credential,
                fromXml, param, ProfileConstants.CONTENT_TYPE_PART);
    }

    /**
     * this function is call by super class ProfileServType when set profile
     * format a xml docment, and set the node content by params
     * @ param params is the init value needed to set
     */
    @Override
    public void initServiceInstance(Object params) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "initServiceInstance 1");
        Element root = null;
        CommAddrParams commAddrParams = (CommAddrParams)params;
        int numberType = NUMBER_NONE;

        if (commAddrParams.mTelNumber != null){
            numberType = commAddrParams.mTelNumber.mNumberType;
        }
        if (numberType > 0 && mTels == null) {
            //should run into here
            mTels = new ArrayList<Tel>();
        } else {
            ProfileServiceLog.d(TAG, "error, mTels should be null here");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME); //<comm-addr>
            root.setAttribute("xml:lang", "en");
            document.appendChild(root);
            String parentUri = mParentUri + "/" + NODE_NAME;
            switch (numberType) {
                case NUMBER_TYPE_MULTI:
                    //office phone
                    if (commAddrParams.mTelNumber.mOfficeNumber != null) {
                        Tel telWork = new Tel(mXcapUri, parentUri, mIntendedId, mCredentials,
                                ProfileConstants.TEL_TYPE_WORK, 1, null); //work perf default is 1
                        mTels.add(telWork);
                        Element telWorkElement = telWork.toXmlElement(document,
                                commAddrParams.mTelNumber.mOfficeNumber);
                        root.appendChild(telWorkElement);
                    }
                    //fax
                    if (commAddrParams.mTelNumber.mCompanyFax != null) {
                        Tel telFax = new Tel(mXcapUri, parentUri, mIntendedId, mCredentials,
                                ProfileConstants.TEL_LABEL_FAX, 2, null); //fax perf default is 2
                        mTels.add(telFax);
                        Element telFaxElement = telFax.toXmlElement(document,
                                commAddrParams.mTelNumber.mCompanyFax);
                        root.appendChild(telFaxElement);
                    }
                    //others number
                    if (commAddrParams.mTelNumber.mOtherNumber != null) {
                        if (mOtherTels == null) {
                            mOtherTels = new ArrayList<Tel>();
                        } else {
                            ProfileServiceLog.d(TAG, "error, mOtherTels should be null here");
                        }
                        int pref = 3;
                        Tel telOther = null;
                        //Iterator iter = commAddrParams.mTelNumber.mOtherNumber.entrySet().iterator();
                        //List otherNumList = commAddrParams.mTelNumber.mOtherNumber;
                        //LinkedList<String[]> otherNumList =
                                //LinkedList<String[]>(commAddrParams.mTelNumber.mOtherNumber);
                        int cnt = commAddrParams.mTelNumber.mOtherNumber.size();
                        for (int i = 0; i < cnt; i++) {
                            String[]key_value =
                                    (String[])commAddrParams.mTelNumber.mOtherNumber.get(i);
                            String key = key_value[0];
                            String value = key_value[1];
                            ProfileServiceLog.d(TAG, "NUMBER_TYPE_OTHER, key: " + key +
                                    " value: " + value + " pref: " + pref);
                            //for other number, key is considered as the label
                            telOther = new Tel(mXcapUri, parentUri, mIntendedId, mCredentials,
                                    ProfileConstants.TEL_TYPE_OTHER, pref, key);
                            pref++;
                            mOtherTels.add(telOther);
                            if (telOther != null) {
                                Element telOtherElement = telOther.toXmlElement(document, value);
                                root.appendChild(telOtherElement);
                            } else {
                                ProfileServiceLog.d(TAG, "damn, why null here?");
                            }
                        }
                    } else {
                        ProfileServiceLog.d(TAG, "error: mOtherNumber should not be null here");
                    }
                    break;

                case NUMBER_TYPE_OFFICE:
                    if (commAddrParams.mTelNumber.mOfficeNumber != null) {
                        Tel telWork = new Tel(mXcapUri, parentUri, mIntendedId, mCredentials,
                                ProfileConstants.TEL_TYPE_WORK, 1, null);
                        mTels.add(telWork);
                        Element telWorkElement = telWork.toXmlElement(document,
                                commAddrParams.mTelNumber.mOfficeNumber);
                        root.appendChild(telWorkElement);
                    }
                    break;

                case NUMBER_TYPE_FAX:
                    if (commAddrParams.mTelNumber.mCompanyFax != null) {
                        Tel telFax = new Tel(mXcapUri, parentUri, mIntendedId, mCredentials,
                                ProfileConstants.TEL_TYPE_FAX, 2, null);
                        mTels.add(telFax);
                        Element telFaxElement = telFax.toXmlElement(document,
                                commAddrParams.mTelNumber.mCompanyFax);
                        root.appendChild(telFaxElement);
                    }
                    break;

                case NUMBER_TYPE_OTHER:
                    if (commAddrParams.mTelNumber.mOtherNumber != null) {
                        if (mOtherTels == null) {
                            mOtherTels = new ArrayList<Tel>();
                        } else {
                            ProfileServiceLog.d(TAG, "error, mOtherTels should be null here");
                        }
                        int pref = 3;
                        Tel telOther = null;
                        //Iterator iter = commAddrParams.mTelNumber.mOtherNumber.entrySet().iterator();
                        //LinkedList<String[]> otherNumList =
                                //LinkedList<String[]>(commAddrParams.mTelNumber.mOtherNumber);
                        int cnt = commAddrParams.mTelNumber.mOtherNumber.size();
                        for (int i = 0; i < cnt; i++) {
                            String[]key_value =
                                    (String[])commAddrParams.mTelNumber.mOtherNumber.get(i);
                            String key = key_value[0];
                            String value = key_value[1];
                            ProfileServiceLog.d(TAG, "NUMBER_TYPE_OTHER, key: " + key +
                                    " value: " + value + " pref: " + pref);
                            telOther = new Tel(mXcapUri, parentUri, mIntendedId, mCredentials,
                                    ProfileConstants.TEL_TYPE_OTHER, pref, key);
                            pref++;
                            mOtherTels.add(telOther);
                            if (telOther != null) {
                                Element telOtherElement = telOther.toXmlElement(document, value);
                                root.appendChild(telOtherElement);
                            } else {
                                ProfileServiceLog.d(TAG, "damn, why null here?");
                            }
                        }
                    } else {
                        ProfileServiceLog.d(TAG, "error: mOtherNumber should not be null here");
                    }
                    break;

                default:
                    //error
                    break;
            }

            if (commAddrParams.mEmail != null &&
                commAddrParams.mEmail.mEmail != null) {
                ProfileServiceLog.d(TAG, "have email to set");
                mUriEntry = new UriEntry(mXcapUri, parentUri, mIntendedId, mCredentials, "email");
                mUriEntry.setAddrUri(commAddrParams.mEmail.mEmail);
                Element emailNode = mUriEntry.toXmlElement(document);
                root.appendChild(emailNode);
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
    public void initServiceInstance(Document domDoc) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "initServiceInstance 2");
        //actually,the domDoc content is form network
        Tel tel = null;
        if (mTels == null) {
            mTels = new ArrayList<Tel>();
        } else {
            ProfileServiceLog.d(TAG, "error, mTels should be null here");
        }
        if (mOtherTels == null) {
            mOtherTels = new ArrayList<Tel>();
        } else {
            ProfileServiceLog.d(TAG, "error, mOtherTels should be null here");
        }
        NodeList commAddrNode = domDoc.getElementsByTagName(NODE_NAME);
        if (commAddrNode.getLength() > 0) {
            Element commAddrElement = (Element) commAddrNode.item(0);
            NodeList telNode = commAddrElement.getElementsByTagName(Tel.NODE_NAME);
            for (int i = 0; i < telNode.getLength(); i++ ){
                    Element telElement = (Element)telNode.item(i);
                    tel = new Tel (mXcapUri, NODE_NAME, mIntendedId, mCredentials, telElement);
                    if (tel.getTelType().startsWith("Other")) {
                        ProfileServiceLog.d(TAG, "this tel is an other type tel");
                        mOtherTels.add(tel);
                    } else {
                        ProfileServiceLog.d(TAG, "this tel is an basic type tel");
                        mTels.add(tel);
                    }
            }
            //for email
            NodeList uriEntryNode = commAddrElement.getElementsByTagName(UriEntry.NODE_NAME);
            for (int i = 0; i < uriEntryNode.getLength(); i++ ){
                    Element uriEntryElement = (Element)uriEntryNode.item(i);
                    mUriEntry = new UriEntry (mXcapUri, NODE_NAME, mIntendedId,
                            mCredentials, uriEntryElement);
            }
        } else {
            ProfileServiceLog.d(TAG, "error, no <comm-addr> node");
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    public String getTelNumber(String type) {
        int telCnt = mTels.size();
        ProfileServiceLog.d(TAG, "getTelNumber, type: " + type + " telCnt: " + telCnt);
        Tel tel = null;
        boolean hasTel = false;
        for (int i = 0; i < telCnt; i++) {
            tel = mTels.get(i);
            if (tel.getTelType().equals(type)) {
                hasTel = true;
                ProfileServiceLog.d(TAG, "get tel type");
                break;
            }
        }
        if (hasTel == true && tel.getTelNb() != null) {
            return tel.getTelNb().getTelNumber();
        } else {
            ProfileServiceLog.d(TAG, "error: <tel-nb> is null");
            return null;
        }
    }

    public String getOtherNumber() {
        int otherTelCnt = mOtherTels.size();
        ProfileServiceLog.d(TAG, "getOtherNumber, otherTelCnt: " + otherTelCnt);
        Tel tel = null;
        String otherNumber = null;
        String otherLabel = null;
        String keyValue = null;
        String finalStr = null;
        for (int i = 0; i < otherTelCnt; i++) {
            tel = mOtherTels.get(i);
            if ((tel.getTelType().equals("Other")) && (tel.getTelNb() != null)) {
                otherNumber = tel.getTelNb().getTelNumber();
                otherLabel = tel.getLabel();
                if (otherLabel == null) {
                    ProfileServiceLog.d(TAG, "error: label is null");
                    otherLabel = "Other";
                }
                keyValue = otherLabel + "=" + otherNumber;
                if (i == 0) {
                    finalStr = new String (keyValue);
                } else {
                    finalStr = finalStr.concat(";" + keyValue);
                }
            } else {
                ProfileServiceLog.d(TAG,
                        "error: other type should be \"Other\", nb should not be null neither ");
                break;
            }
        }
        ProfileServiceLog.d(TAG, "finalStr is: " + finalStr);
        return finalStr;
    }

    public void setTelNumber(String type) throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setTelNumber, type: " + type);
        String telXml = null;
        Tel tel = null;
        boolean hasTel = false;
        int telCnt = mTels.size();
        for (int i = 0; i < telCnt; i++) {
             tel = mTels.get(i);
            //check entry address-type
            if(tel == null) {
                ProfileServiceLog.d(TAG, "damn! tel is null here");
                return;
            }
            if (tel.getTelType().equals(type)) {
                hasTel = true;
                telXml = tel.toXmlString(mRoot);
                break;
            }
        }
        if (hasTel) {
            ProfileServiceLog.d(TAG, "telXml is : " + telXml);
            tel.setContent(telXml, ProfileConstants.CONTENT_TYPE_PART);
        } else {
            ProfileServiceLog.d(TAG, "error, no this type to set");
        }
    }

    public void setOtherNumber() throws ProfileXcapException {
        int otherTelCnt = mOtherTels.size();
        ProfileServiceLog.d(TAG, "setOtherNumber, otherTelCnt: " + otherTelCnt);
        String telXml = null;
        Tel tel = null;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otherTelCnt; i++) {
            tel = mOtherTels.get(i);
            if (tel == null) {
                ProfileServiceLog.d(TAG, "error: tel is null");
                break;
            }
            if (tel.getTelType().startsWith("Other")) {
                telXml = tel.toXmlString(mRoot, i);  //mRoot is <comm-addr>
                sb.append(telXml);
                ProfileServiceLog.d(TAG, "telXml is : " + telXml);
                //tel.setContent(telXml, ProfileConstants.CONTENT_TYPE_PART);
            } else {
                ProfileServiceLog.d(TAG, "error: other type should starts with prifix \"Other\"");
                break;
            }
            /*othernumber is special, when we set othernumber, we need set all othernumber by one time
            so we add all othernumber xml string*/
            tel.setContent(sb.toString(), ProfileConstants.CONTENT_TYPE_PART);
        }
    }

    public String getEmail() {
        ProfileServiceLog.d(TAG, "getEmail");
        if (mUriEntry != null) {
            return mUriEntry.getAddrUri();
        } else {
            ProfileServiceLog.d(TAG, "error: mUriEntry is null");
        }
        return null;
    }

    public void setEmail() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setEmail");
        String emailXml = null;
        if (mUriEntry != null) {
            emailXml = mUriEntry.toXmlString(mRoot);
            ProfileServiceLog.d(TAG, "emailXml is : " + emailXml);
            mUriEntry.setContent(emailXml, ProfileConstants.CONTENT_TYPE_PART);
        } else {
            ProfileServiceLog.d(TAG, "error: mUriEntry is null");
        }
    }
}

