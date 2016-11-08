/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.rcs.contacts.networkcontacts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils for Json parser.
 * @author MTK81359
 *
 */
public class JsonUtil {
    /**
     * Json tags for contacts.
     * @author MTK81359
     *
     */
    public static class JsonTag {
        public static final String NAME = "name";
        public static final String FAMILY_NAME = "familyName";
        public static final String MIDDLE_NAME = "middleName";
        public static final String GIVEN_NAME = "givenName";
        public static final String NICK_NAME = "nickName";
        public static final String GENDER = "gender";
        public static final String PREFIX = "prefix";
        public static final String SUFFIX = "suffix";
        public static final String CAR_TEL = "carTel";
        public static final String MOBILE = "mobile";
        public static final String WORK_MOBILE = "workMobile";
        public static final String HOME_MOBILE = "homeMobile";
        public static final String OTHER_MOBILE = "otherMobile";
        public static final String IPHONE = "iphone";
        public static final String TEL = "tel";
        public static final String WORK_TEL = "workTel";
        public static final String HOME_TEL = "homeTel";
        public static final String OTHER_TEL = "otherTel";
        public static final String SHORT_TEL_NUM = "shortTelNum";
        public static final String FAX = "fax";
        public static final String WORK_FAX = "workFax";
        public static final String HOME_FAX = "homeFax";
        public static final String OTHER_FAX = "otherFax";
        public static final String COMPANY_TEL_EX = "companyTelExchange";
        public static final String PAGER = "BP";
        public static final String TEL_TLX = "pager";
        public static final String EMAIL = "email";
        public static final String WORK_MAIL = "workMail";
        public static final String HOME_MAIL = "homeMail";
        public static final String OTHER_MAIL = "otherMail";
        public static final String WEBSITE = "website";
        public static final String WORK_WEBSITE = "workWebsite";
        public static final String HOME_WEBSITE = "homeWebsite";
        public static final String OTHER_WEBSITE = "otherWebsite";
        public static final String ADDRESS = "address";
        public static final String HOME_ADDRESS = "homeAddress";
        public static final String WORK_ADDRESS = "workAddress";
        public static final String ASSEMBLE_ADDR = "assembleAddress";
        public static final String WORK_ASSEMBLE_ADDR = "workAssembleAddress";
        public static final String HOME_ASSEMBLE_ADDR = "homeAssembleAddress";
        public static final String OTHER_ASSEMBLE_ADDR = "otherAssembleAddress";
        public static final String ASSEMBLE_ORG = "assembleOrg";
        public static final String NOTE = "note";
        public static final String BIRTHDAY = "birthday";
        public static final String ANNIVERSARY = "anniversary";
        public static final String CHILD = "child";
        public static final String SPOUSE = "spouse";
        public static final String FETION = "fetion";
        public static final String QQ = "qq";
        public static final String MSN = "msn";
        public static final String WEIBO = "weibo";
        public static final String BLOG = "blog";
        public static final String GROUP_NAME = "groupName";

        public static final String STATE = "state";
        public static final String AREA = "area";
        public static final String CITY = "city";
        public static final String STREET = "street";
        public static final String POSTAL_CODE = "postalCode";

        public static final String COMPANY = "company";
        public static final String DEPARTMENT = "department";
        public static final String POSITION = "position";
    }

    /**
     * Get contents of specified tag.
     * @param jsonSrc Json object
     * @param jsonTag tag specified.
     * @return Contents of specified tag.
     */
    public static String parseJsonString(JSONObject jsonSrc, String jsonTag) {
        try {
            if (jsonSrc.has(jsonTag)) {
                return jsonSrc.getString(jsonTag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get array contents of specified tag.
     * @param jsonSrc Json object
     * @param jsonTag tag specified.
     * @return Array contents of specified tag.
     */
    public static List<String> parseJsonStringArray(JSONObject jsonSrc, String jsonTag) {
        try {
            if (jsonSrc.has(jsonTag)) {
                JSONArray jsonArray = jsonSrc.getJSONArray(jsonTag);
                int length = jsonArray.length();
                List<String> result = new ArrayList<String>(length);
                for (int i = 0; i < length; i++) {
                    result.add(jsonArray.getString(i));
                }
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get PostalAddress array of specified tag.
     * @param jsonSrc Json object
     * @param jsonTag tag for PostalAddress.
     * @return PostalAddress array of specified tag.
     */
    public static List<PostalAddress> parseJsonPostalAddrArray(JSONObject jsonSrc, String jsonTag) {
        try {
            if (jsonSrc.has(jsonTag)) {
                JSONArray jsonArray = jsonSrc.getJSONArray(jsonTag);
                int length = jsonArray.length();
                List<PostalAddress> result = new ArrayList<PostalAddress>(length);
                for (int i = 0; i < length; i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    PostalAddress addr = new PostalAddress();
                    addr.setState(parseJsonString(jsonObj, JsonTag.STATE));
                    addr.setArea(parseJsonString(jsonObj, JsonTag.AREA));
                    addr.setCity(parseJsonString(jsonObj, JsonTag.CITY));
                    addr.setStreet(parseJsonString(jsonObj, JsonTag.STREET));
                    addr.setPostalCode(parseJsonString(jsonObj, JsonTag.POSTAL_CODE));
                    result.add(addr);
                }
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get Organization array of specified tag.
     * @param jsonSrc Json object
     * @param jsonTag tag for Organization.
     * @return Organization array of specified tag.
     */
    public static List<Organization> parseJsonOrgArray(JSONObject jsonSrc, String jsonTag) {
        try {
            if (jsonSrc.has(jsonTag)) {
                JSONArray jsonArray = jsonSrc.getJSONArray(jsonTag);
                int length = jsonArray.length();
                List<Organization> result = new ArrayList<Organization>(length);
                for (int i = 0; i < length; i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    Organization org = new Organization();
                    org.setCompany(parseJsonString(jsonObj, JsonTag.COMPANY));
                    org.setDepartment(parseJsonString(jsonObj, JsonTag.DEPARTMENT));
                    org.setPosition(parseJsonString(jsonObj, JsonTag.POSITION));
                    result.add(org);
                }
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
