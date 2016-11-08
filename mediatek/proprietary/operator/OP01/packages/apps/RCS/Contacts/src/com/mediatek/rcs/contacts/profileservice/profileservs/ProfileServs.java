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

import com.mediatek.rcs.contacts.profileservice.profileservs.xcap.ProfileXcapException;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileConstants;
import com.mediatek.rcs.contacts.profileservice.utils.ProfileServiceLog;
import com.mediatek.xcap.client.uri.XcapUri;
import com.mediatek.xcap.client.uri.XcapUri.XcapDocumentSelector;
import com.mediatek.xcap.client.XcapConstants;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;


public class ProfileServs {
    private static final String TAG = "ProfileServs";
    private static final String AUTH_XCAP_3GPP_INTENDED = "X-3GPP-Intended-Identity";
    private static final String PROFILE_AUID = "org.openmobilealliance.cab-pcc";
    private static final String PORTRAIT_AUID = "org.openmobilealliance.pres-content";
    private static final String TWODIMENSIONCODE_AUID = "pcc-TwoDimensionCodeCard";
    private static final String PARENT = "pcc/person-details";
    private static String sDocName = "PCC.xml";
    private static String sPortraitName = "oma_status-icon/rcs_status_icon";
    private static String sTwoDimensionCodeName = "pcc_twodimensioncode_card";

    private static ProfileServs sInstance = null;
    private static String sXcapRoot = null;
    private static String sXui = null;
    private static String sContactXui = null;

    public XcapDocumentSelector mDocumentSelector;
    private XcapUri mXcapUri;  //actually, is document uri
    private Credentials mCredential = null;
    private URI mDocumentUri = null;
    private String mIntendedId = null;

    /**
    * contractor
    *
    * private for singleton
    */
    private ProfileServs (){

    }

    /*Get SimServs instance.
        @return Simservs instance*/
    static public ProfileServs getInstance() {
        if (sInstance == null) {
            sInstance = new ProfileServs();
        }
        //initialize();
        return sInstance;
    }

    /* do initialzation */
    private void initialize (){
        //for further use
    }

    /**set intendedId,
    * for the http header X-3GPP-Intended-Identity
        the value is user telephne number*/
    public void setIntendedId(String intendedId) {
        mIntendedId = intendedId;
    }

    /*get intendedId, the value is user telephne number*/
    public String getIntendedId() {
        return mIntendedId;
    }

    /**
      * ild the uri for http request
      * @throws URISyntaxException if URI syntax error
      */
    public void buildDocumentUri(int contentType) throws URISyntaxException {
        ProfileServiceLog.d(TAG, "buildDocumentUri, contentType: " + contentType);

        switch (contentType) {
            case ProfileConstants.CONTENT_TYPE_PORTRAIT:
                mDocumentSelector = new XcapDocumentSelector(
                PORTRAIT_AUID, sXui, sPortraitName);
                break;

            case ProfileConstants.CONTENT_TYPE_PCC:
            case ProfileConstants.CONTENT_TYPE_PART:
                mDocumentSelector = new XcapDocumentSelector(
                PROFILE_AUID, sXui, sDocName);
                break;

            case ProfileConstants.CONTENT_TYPE_CONTACT_PORTRAIT:
                mDocumentSelector = new XcapDocumentSelector(
                PORTRAIT_AUID, sContactXui, sPortraitName);
                break;

            case ProfileConstants.CONTENT_TYPE_QRCODE:
            case ProfileConstants.CONTENT_TYPE_QRCODE_MODE:
                mDocumentSelector = new XcapDocumentSelector(
                TWODIMENSIONCODE_AUID, sXui, sTwoDimensionCodeName);
                break;

            default:
                //error log
                break;
        }
        mXcapUri = new XcapUri();
        if (contentType == ProfileConstants.CONTENT_TYPE_QRCODE ||
                contentType == ProfileConstants.CONTENT_TYPE_QRCODE_MODE) {
            mXcapUri.setXcapRoot("http://221.179.192.78:8186/services/")
                    .setDocumentSelector(mDocumentSelector);
        } else {
            mXcapUri.setXcapRoot(sXcapRoot).setDocumentSelector(mDocumentSelector);
        }
        //ProfileServiceLog.d(TAG, "DocumentUri: " + mXcapUri.toURI().toString());
        ProfileServiceLog.d(TAG, "XcapRoot: "  + mXcapUri.getXcapRoot() +
                " Document: " + mXcapUri.getDocumentSelector());
    }

    /**
     * set the xcap root,
     */
    public void setXcapRoot(String xcapRoot) {
        sXcapRoot = xcapRoot; //192.168.246.174:8090/services/
        ProfileServiceLog.d(TAG, "setXcapRoot, sXcapRoot: " + sXcapRoot);
    }

    /**
     * set the xui,
     */
    public void setXui(String xui) {
        sXui = xui;
    }

    /**
     * set the setContactXui, for get contact portrait
     *@param contactXui, contactXui, actually is the contact phonenumber
     */
    public void setContactXui(String contactXui) {
        sContactXui = contactXui;
    }

    /**
     * set xml document name,
     @ param docName is xml document name
     */
    public void setDocumentName(String docName) {
        sDocName = docName;
    }

    /**
     * Set username/password Credential.
     *
     * @param username username
     * @param password password
     */
    public void setHttpCredential(String username, String password) {
        mCredential = new UsernamePasswordCredentials(username, password);
    }

    /**
     * setGbaCredential.
     *
     * @param credential credential
     */
    public void setGbaCredential(Credentials credential) {
        mCredential = credential;
    }

    /**
     * getPccInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @return Pcc instance
     * @throws ProfileXcapException if error
     */
    public Pcc getPccInstance(boolean fromXml, Object params) throws ProfileXcapException {
        Pcc pcc = null;
        pcc = new Pcc(mXcapUri, null, mIntendedId, mCredential, fromXml, params);
        return pcc;
    }

    /**
     * getCommAddrInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @return CommAddr instance
     * @throws ProfileXcapException if error
     */
    public CommAddr getCommAddrInstance(boolean fromXml, Object params) throws
            ProfileXcapException {
        return new CommAddr (mXcapUri, "person-details", mIntendedId, mCredential, fromXml, params);
    }

    /**
     * getBirthInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @return Birth instance
     * @throws ProfileXcapException if error
     */
    public Birth getBirthInstance(boolean fromXml, Object params) throws
            ProfileXcapException {
        return new Birth(mXcapUri, "person-details", mIntendedId, mCredential, fromXml, params);
    }

    /**
     * getNameInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @return Name instance
     * @throws ProfileXcapException if error
     */
    public Name getNameInstance(boolean fromXml, Object params) throws
            ProfileXcapException {
        return new Name(mXcapUri, "person-details", mIntendedId, mCredential, fromXml, params);
    }

    /**
     * getAddressInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @return Address instance
     * @throws ProfileXcapException if error
     */
    public Address getAddressInstance(boolean fromXml, Object params) throws
            ProfileXcapException {
        return new Address(mXcapUri, "person-details", mIntendedId, mCredential, fromXml, params);
    }

    /**
     * getCareerInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @return Career instance
     * @throws ProfileXcapException if error
     */
    public Career getCareerInstance(boolean fromXml, Object params) throws
            ProfileXcapException {
        return new Career(mXcapUri, "person-details", mIntendedId, mCredential, fromXml, params);
    }

    /**
     * getPortraitInstance.
     *
     * @param fromXml whether is from existed xml or not
     * @param params param to init xml
     * @param portraitType portrait or contactportrait,
     *   actully, it not necessory, because in ProfileServType, they are handle in same way
     * @return Portrait instance
     * @throws ProfileXcapException if error
     */
    public Portrait getPortraitInstance(boolean fromXml, Object params, int portraitType)
            throws ProfileXcapException {
        return new Portrait(mXcapUri, null, mIntendedId, mCredential,
                fromXml, params, portraitType);
    }

    public QRCode getQRCodeInstance(boolean fromXml, Object params, int qrCodeType)
            throws ProfileXcapException {
        QRCode qrCode = null;
        qrCode = new QRCode(mXcapUri, null, mIntendedId, mCredential, fromXml, params, qrCodeType);
        return qrCode;
    }
}
