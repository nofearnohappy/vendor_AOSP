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

package com.mediatek.rcs.contacts.profileservice.utils;

import android.net.Uri;

/**
 * ProfileConstants class.
 */
public class ProfileConstants {

    public static final int RES_NOUPDATE = 1;
    public static final int RES_PART_OK = 1;
    public static final int RES_OK = 0;
    public static final int RES_TIMEOUT = -1;
    public static final int RES_UNKNOW = -2;
    public static final int RES_UNAUTHORIZED = -3;
    public static final int RES_FORBIDEN = -4;
    public static final int RES_NOTFOUND = -5;
    public static final int RES_INTERNEL_ERROR = -6;
    //public static final int RES_NOT_MODIFIED = 7;

    public static final int CONTENT_TYPE_UNKNOWN = -1;
    public static final int CONTENT_TYPE_PCC = 1;
    public static final int CONTENT_TYPE_PART = 2;
    public static final int CONTENT_TYPE_PORTRAIT = 3;
    public static final int CONTENT_TYPE_CONTACT_PORTRAIT = 4;
    public static final int CONTENT_TYPE_QRCODE = 5;
    public static final int CONTENT_TYPE_QRCODE_MODE = 6;

    public static final String PROVIDER_AUTHORITY = "com.cmcc.ccs.profile";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);

    //the string follow must same to profileservice fully
    public static final String PHONE_NUMBER = "PROFILE_PHONENUMBER";
    public static final String FIRST_NAME = "PROFILE_FIRST_NAME";
    public static final String LAST_NAME = "PROFILE_LAST_NAME";
    public static final String PORTRAIT = "PROFILE_PORTRAIT";
    public static final String PORTRAIT_TYPE = "PROFILE_PORTRAIT_TYPE";
    public static final String ADDRESS = "PROFILE_ADDRESS";
    public static final String PHONE_NUMBER_SECOND = "PROFILE_PHONE_NUMBER_SECOND";
    public static final String EMAIL = "PROFILE_EMAIL";
    public static final String BIRTHDAY = "PROFILE_BIRTHDAY";
    public static final String COMPANY = "PROFILE_COMPANY";
    public static final String COMPANY_TEL = "PROFILE_COMPANY_TEL";
    public static final String TITLE = "PROFILE_TITLE";
    public static final String COMPANY_ADDR = "PROFILE_COMPANY_ADDR";
    public static final String COMPANY_FAX = "PROFILE_COMPANY_FAX";
    public static final String PCC_ETAG = "PROFILE_PCC_ETAG";
    public static final String PORTRAIT_ETAG = "PROFILE_PORTRAIT_ETAG";
    public static final String QRCODE = "PROFILE_QRCODE";
    public static final String QRCODE_ETAG = "PROFILE_QRCODE_ETAG";

    public static final String JPEG = "JPEG";
    public static final String BMP = "BMP";
    public static final String PNG = "PNG";
    public static final String GIF = "GIF";


    public static final String HOME = "PROFILE_HOME";
    public static final String WORK = "PROFILE_WORK";
    public static final String FIXED = "PROFILE_FIXED";
    public static final String OTHER = "PROFILE_OTHER";

    //no use
    public static final String[] PROFILE_PROJECTION = {
        PHONE_NUMBER,
        FIRST_NAME,
        LAST_NAME,
        PORTRAIT,
        PORTRAIT_TYPE,
        ADDRESS,
        PHONE_NUMBER_SECOND,
        EMAIL,
        BIRTHDAY,
        COMPANY,
        COMPANY_TEL,
        TITLE,
        COMPANY_ADDR,
        COMPANY_FAX,
    };
    //public static final String TEL_TYPE_MOBILE = "Mobile";
    public static final String TEL_TYPE_WORK = "Work";
    public static final String TEL_TYPE_FAX = "Fax";
    public static final String TEL_TYPE_OTHER = "Other";
    //no use further
    public static final String TEL_TYPE_OTHER_OTHER = "Other-other";
    public static final String TEL_TYPE_OTHER_WORK = "Other-work";
    public static final String TEL_TYPE_OTHER_HOME = "Other-home";
    public static final String TEL_TYPE_OTHER_FIX = "Other-fix";
    //public static final String TEL_LABEL_MOBILE = "mobile";
    public static final String TEL_LABEL_WORK = "office Phone";
    public static final String TEL_LABEL_FAX = "offiec Fax";
    public static final String TEL_LABEL_OTHER_OTHER = "other";
    public static final String TEL_LABEL_OTHER_WORK = "other";
    public static final String TEL_LABEL_OTHER_HOME = "home Phone";
    public static final String TEL_LABEL_FIX = "fix Phone";
}
