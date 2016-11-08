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

import org.apache.http.auth.Credentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TelNb extends ProfileXcapElement implements ProfileConfigureType {

    public static final String NODE_NAME = "tel-nb";
    private static final String TEL_STRING = "tel-string";
    private static final String TAG = "TelNb";
    private String mTelNumber = null;

    /**
     * Constructor.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     * @param credential    for authentication
     */
    public TelNb(XcapUri xcapUri, String parentUri, String intendedId,
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
    public TelNb(XcapUri xcapUri, String parentUri, String intendedId, Credentials credential,
                   Element domElement) {
        super(xcapUri, parentUri, intendedId, credential);
        instantiateFromXmlNode(domElement);
    }

    @Override
    public String getNodeName() {
        return NODE_NAME;
    }

    /**
     * implements the function of interface ProfileConfigureType
     * only private used
     * @param domNode is the current node: <tel-nb> node
     */
    @Override
    public void instantiateFromXmlNode(Node domNode) {
        ProfileServiceLog.d(TAG, "instantiateFromXmlNode");
        Element domElement = (Element) domNode; //<tel-nb> node
        NodeList telStringNode = domElement.getElementsByTagName(TEL_STRING);
        if (telStringNode.getLength() > 0) {
            Element telString = (Element) telStringNode.item(0);
            mTelNumber= telString.getTextContent();
        } else {
            ProfileServiceLog.d(TAG, "no <tel-string> node");
        }
    }

    /**
     * Convert to XML element.
     *
     * @param document DOM document
     * @return XML element
     */
    public Element toXmlElement(Document document) {
        Element telNbNode = (Element) document.createElement(NODE_NAME); //<tel-nb>
        Element telStringElement = (Element) document.createElement(TEL_STRING);
        telStringElement.setTextContent(mTelNumber);
        telNbNode.appendChild(telStringElement);
        return telNbNode;
    }

    public String getTelNumber() {
        return mTelNumber;
    }

    public void setTelNumber (String number) {
        mTelNumber = number;
    }
}
