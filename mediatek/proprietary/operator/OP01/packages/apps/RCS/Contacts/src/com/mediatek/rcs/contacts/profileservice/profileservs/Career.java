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

import com.mediatek.rcs.contacts.profileservice.profileservs.element.Duty;
import com.mediatek.rcs.contacts.profileservice.profileservs.element.Employer;
import com.mediatek.rcs.contacts.profileservice.profileservs.params.CareerParams;
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

public class Career extends ProfileServType {

    public static final String NODE_NAME = "career";
    private static final String EMPLOYER = "employer";
    private static final String DUTY = "duty";
    private static final String TAG = "Career";

    //should not set to null here
    private Employer mEmployerIns;
    private Duty mDutyIns;

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
    public Career(XcapUri documentUri, String parentUri, String intendedId,
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
        ProfileServiceLog.d(TAG, "initServiceInstance 1");
        //actually,the domDoc content is form network
        //should be modified, init should get info from local db
        Element root = null;
        CareerParams careerParams = (CareerParams)params;
        String employerContent = careerParams.mCompany; //employer string content
        String dutyContent = careerParams.mDutyTitle; //duty string content
        String parentUri = mParentUri + "/" + NODE_NAME;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME); //<career>
            document.appendChild(root);
            /*athough employer & duty are nodes without child, they are items in content provider,
                    we need the instances of the to get their's content specific*/
            mEmployerIns = new Employer (mXcapUri, parentUri, mIntendedId, mCredentials);
            mDutyIns = new Duty(mXcapUri, parentUri, mIntendedId, mCredentials);

            Element employerElement = mEmployerIns.toXmlElement(document, employerContent);
            Element dutyElement = mDutyIns.toXmlElement(document, dutyContent);

            root.appendChild(employerElement);
            root.appendChild(dutyElement);
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
    public void initServiceInstance(Document domDoc)  throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "initServiceInstance 2");
        //actually,the domDoc content is form network
        NodeList careerNode = domDoc.getElementsByTagName(NODE_NAME);
        if (careerNode.getLength() > 0) {
            Element careerElement = (Element) careerNode.item(0); //<career> node
            /*employer node*/
            NodeList employerNode = careerElement.getElementsByTagName(Employer.NODE_NAME);
            if (employerNode.getLength() > 0) {
                Element employerElement = (Element)employerNode.item(0);  //<employer> node
                mEmployerIns = new Employer (mXcapUri, NODE_NAME, mIntendedId,
                        mCredentials, employerElement);
            } else {
                //no <employer> node
                ProfileServiceLog.d(TAG, "no <employer> node");
                mEmployerIns = new Employer (mXcapUri, NODE_NAME, mIntendedId, mCredentials);
            }
            /*duty node*/
            NodeList dutyNode = careerElement.getElementsByTagName(Duty.NODE_NAME);
            if (dutyNode.getLength() > 0) {
                Element dutyElement = (Element)dutyNode.item(0);  //<duty> node
                mDutyIns= new Duty (mXcapUri, NODE_NAME, mIntendedId, mCredentials, dutyElement);
            } else {
                //no <duty> node
                ProfileServiceLog.d(TAG, "no <duty> node");
                mDutyIns= new Duty (mXcapUri, NODE_NAME, mIntendedId, mCredentials);
            }
        } else {
            //no <career> node, error
            ProfileServiceLog.d(TAG, "error: no <career> node");
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * getEmployer.
     */
    public String getEmployer() {
        if(mEmployerIns != null) {
            return mEmployerIns.getEmployer();
        } else {
            ProfileServiceLog.d(TAG, "error: mEmployerIns is null");
        }
        return null;
    }

    /**
     * getDuty.
     */
    public String getDuty() {
        if (mDutyIns != null) {
            return mDutyIns.getDuty();
        } else {
            ProfileServiceLog.d(TAG, "error: mDutyIns is null");
        }
        return null;
    }

    /**
     * setCareer,have employer and duty together.
     *
     * @throws ProfileXcapException if error happened when save data on server
     */
    public void setCareer() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setCareer");
        try {
            String careerXml = domToXmlText(mRoot);
            this.setContent(careerXml, ProfileConstants.CONTENT_TYPE_PART);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * setEmployer.
     *
     * @throws ProfileXcapException if error happened when save data on server
     */
    public void setEmployer() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setEmployer");
        if (mEmployerIns == null) {
            ProfileServiceLog.d(TAG, "error: mEmployerIns is null");
            return;
        }
        String employerXml = mEmployerIns.toXmlString(mRoot);
        ProfileServiceLog.d(TAG, "employerXml is : " + employerXml);
        mEmployerIns.setContent(employerXml, ProfileConstants.CONTENT_TYPE_PART);
    }

    /**
     * setDuty.
     * set Duty content on server
     *@throws ProfileXcapException if error happend on save to server
     */
    public void setDuty() throws ProfileXcapException {
        ProfileServiceLog.d(TAG, "setDuty");
        if (mDutyIns == null) {
            ProfileServiceLog.d(TAG, "error: mDutyIns is null");
            return;
        }
        String dutyXml = mDutyIns.toXmlString(mRoot);
        ProfileServiceLog.d(TAG, "dutyXml is : " + dutyXml);
        mDutyIns.setContent(dutyXml, ProfileConstants.CONTENT_TYPE_PART);
    }
}
