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

package com.mediatek.rcs.common.utils;

import com.mediatek.rcs.common.utils.RcsVcardData;


import java.util.List;

public final class RcsVcardParserResult {

    private final String mName;
    private final String mOrganization;
    private final String mTitle;
    private final List<RcsVcardData> mNumber;
    private final List<RcsVcardData> mEmail;
    private final byte[] mPhoto;
    private final String mPhotoFormat;

    /**
    * constructor.
    * @param
    * @return null.
    */
    public RcsVcardParserResult(String name, String organization, String title,
            List<RcsVcardData> number, List<RcsVcardData> email, byte[] photo, String photoFormat) {
        mName = name;
        mOrganization = organization;
        mTitle = title;
        mNumber = number;
        mEmail = email;
        mPhoto = photo;
        mPhotoFormat = photoFormat;
    }

    /**
    * get vcard Name.
    * @param null
    * @return name.
    */
    public String getName() {
        return mName;
    }

    /**
    * get Organization.
    * @param null
    * @return Organization.
    */
    public String getOrganization() {
        return mOrganization;
    }

    /**
    * get title.
    * @param null
    * @return title.
    */
    public String getTitle() {
        return mTitle;
    }

    /**
    * get number.
    * @param null
    * @return number list.
    */
    public List<RcsVcardData> getNumber() {
        return mNumber;
    }

    /**
    * get emails.
    * @param null
    * @return emails list.
    */
    public List<RcsVcardData> getEmail() {
        return mEmail;
    }

    /**
    * get photo.
    * @param null
    * @return bytes[] photo.
    */
    public byte[] getPhoto() {
        return mPhoto;
    }

    /**
    * get photo format.
    * @param null
    * @return  photo format.
    */
    public String getPhotoFormat() {
        return mPhotoFormat;
    }
}

