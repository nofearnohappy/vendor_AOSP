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

package com.mediatek.rcs.contacts.profileapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.cmcc.ccs.profile.ProfileService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by mtk81125 on 14-10-23.
 */
public class ProfileInfo implements Parcelable {

    public String accountNumber;
    public String name;
    public byte[] photo;
    public String photoFileName;
    public String workNumber;
    public String homeNumber;
    public String fixedNumber;
    public String homeAddress;
    public String email;
    public String birthday;
    public String companyName;
    public String companyJob;
    public String companyNumber;
    public String companyAddress;
    public String companyFax;
    public byte[] qrcode;

    public static final String PHONE_NUMBER = ProfileService.PHONE_NUMBER;
    public static final String FIRST_NAME = ProfileService.FIRST_NAME;
    public static final String LAST_NAME = ProfileService.LAST_NAME;
    public static final String PORTRAIT = ProfileService.PORTRAIT;
    public static final String PORTRAIT_TYPE = ProfileService.PORTRAIT_TYPE;
    public static final String ADDRESS = ProfileService.ADDRESS;
    public static final String PHONE_NUMBER_SECOND = ProfileService.PHONE_NUMBER_SECOND;
    public static final String EMAIL = ProfileService.EMAIL;
    public static final String BIRTHDAY = ProfileService.BIRTHDAY;
    public static final String COMPANY = ProfileService.COMPANY;
    public static final String COMPANY_TEL = ProfileService.COMPANY_TEL;
    public static final String TITLE = ProfileService.TITLE;
    public static final String COMPANY_ADDR = ProfileService.COMPANY_ADDR;
    public static final String COMPANY_FAX = ProfileService.COMPANY_FAX;
    public static final String QR_CODE = "PROFILE_QRCODE";

    public static final String NAME = "_name";
    public static final String OTHER_NUMBER_WORK = "PROFILE_WORK";
    public static final String OTHER_NUMBER_HOME = "PROFILE_HOME";
    public static final String OTHER_NUMBER_FIXED = "PROFILE_FIXED";
    public static final String OTHER_NUMBER_OTHER = "PROFILE_OTHER";

    public static final int VALUE_NUMBER_TYPE_HOME = 0;
    public static final int VALUE_NUMBER_TYPE_WORK = 1;
    public static final int VALUE_NUMBER_TYPE_FIXED = 2;
    public static final int VALUE_NUMBER_TYPE_OTHER = 3;

    public static String[] mPrimaryInfoKeySet = new String[] {
            PHONE_NUMBER, NAME, EMAIL, ADDRESS, BIRTHDAY};

    public static String[] mCompanyInfoKeySet = new String[] {
            COMPANY, TITLE, COMPANY_TEL, COMPANY_ADDR, COMPANY_FAX};

    public static String[] mAllProfileKeySet = new String[] {
            PORTRAIT, PORTRAIT_TYPE, PHONE_NUMBER, FIRST_NAME, LAST_NAME,
            EMAIL, ADDRESS, BIRTHDAY, PHONE_NUMBER_SECOND,
            COMPANY, TITLE, COMPANY_TEL, COMPANY_ADDR, COMPANY_FAX};

    public static String[] mProfileCommonKeySet = new String[] {
            PHONE_NUMBER, FIRST_NAME, LAST_NAME,
            EMAIL, ADDRESS, BIRTHDAY, PHONE_NUMBER_SECOND,
            COMPANY, TITLE, COMPANY_TEL, COMPANY_ADDR, COMPANY_FAX};

    public static String[] mPortraitKeySet = new String[] {PORTRAIT, PORTRAIT_TYPE};

    public static int mHomeNumberCount = 0;
    public static int mWorkNumberCount = 0;
    public static int mFixedNumberCount = 0;
    public static int mOtherNumberCount = 0;

    private static HashMap<String, String> mAllProfileInfoList = new HashMap<String, String>();
    private HashMap<String, String> mPrimaryInfoList = new HashMap<String, String>();
    //HashMap<String, String> mOtherNumberList = new HashMap<String, String>();
    private HashMap<String, String> mCompanyInfoList = new HashMap<String, String>();
    private ArrayList<OtherNumberInfo> mOtherNumberArrayList = new ArrayList<OtherNumberInfo>();

    public static class OtherNumberInfo {
        int type;
        String number;
        String key;

        public OtherNumberInfo(int type, String number, String key) {
            this.type = type;
            this.key = key;
            this.number = number;
        }
    }

    public ProfileInfo() {
        accountNumber = "";
        name = "";
        photo = null;
        workNumber = "";
        homeNumber = "";
        fixedNumber = "";
        homeAddress = "";
        email = "";
        birthday = "";
        companyName = "";
        companyJob = "";
        companyNumber = "";
        companyAddress = "";
        companyFax = "";
        updateMapList();
    }

    public ProfileInfo(Parcel profile) {
        accountNumber = profile.readString();
        name = profile.readString();
        profile.readByteArray(photo);
        workNumber = profile.readString();
        homeNumber = profile.readString();
        fixedNumber = profile.readString();
        homeAddress = profile.readString();
        email = profile.readString();
        birthday = profile.readString();
        companyName = profile.readString();
        companyJob = profile.readString();
        companyNumber = profile.readString();
        companyAddress = profile.readString();
        companyFax = profile.readString();
        updateMapList();
    }

    private void updateMapList() {
        mCompanyInfoList.clear();
        mCompanyInfoList.put(COMPANY, companyName);
        mCompanyInfoList.put(TITLE, companyJob);
        mCompanyInfoList.put(COMPANY_TEL, companyNumber);
        mCompanyInfoList.put(COMPANY_ADDR, companyAddress);
        mCompanyInfoList.put(COMPANY_FAX, companyFax);

        mPrimaryInfoList.clear();
        mPrimaryInfoList.put(PHONE_NUMBER, accountNumber);
        mPrimaryInfoList.put(NAME, name);
        mPrimaryInfoList.put(ADDRESS, homeAddress);
        mPrimaryInfoList.put(EMAIL, email);
        mPrimaryInfoList.put(BIRTHDAY, birthday);

        //mOtherNumberList.clear();

        mAllProfileInfoList.clear();
        mAllProfileInfoList.putAll(mPrimaryInfoList);
        mAllProfileInfoList.putAll(mCompanyInfoList);
        //mAllProfileInfoList.putAll(mOtherNumberList);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(accountNumber);
        dest.writeString(name);
        dest.writeByteArray(photo);
        dest.writeString(workNumber);
        dest.writeString(homeNumber);
        dest.writeString(fixedNumber);
        dest.writeString(homeAddress);
        dest.writeString(email);
        dest.writeString(birthday);
        dest.writeString(companyName);
        dest.writeString(companyJob);
        dest.writeString(companyNumber);
        dest.writeString(companyAddress);
        dest.writeString(companyFax);
    }
    public static final Creator<ProfileInfo> CREATOR = new Creator<ProfileInfo> () {
        @Override
        public ProfileInfo createFromParcel(android.os.Parcel parcel){
            return new ProfileInfo(parcel);
        }

        @Override
        public ProfileInfo[] newArray(int i){
            return new ProfileInfo[i];
        }
    };

    public void setAccountNumber(String number) {
        this.accountNumber = number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        StringBuffer buffer = new StringBuffer();
        String lastName = getContentByKey(LAST_NAME);
        String firstName = getContentByKey(FIRST_NAME);

        if (lastName != null) {
            buffer.append(lastName);
            buffer.append(" ");
        }
        if (firstName != null) {
            buffer.append(firstName);
        }
        return buffer.toString();
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public void setPhotoFileName(String fileName) {
        this.photoFileName = fileName;
    }

    public void setWorkNumber(String workNumber) {
        this.workNumber = workNumber;
    }

    public void setHomeNumber(String homeNumber) {
        this.homeNumber = homeNumber;
    }

    public void setFixedNumber(String fixedNumber) {
        this.fixedNumber = fixedNumber;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public void setCompanyJob(String companyJob) {
        this.companyJob = companyJob;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public void setCompanyFax(String companyFax) {
        this.companyFax = companyFax;
    }

    public void setQrcode(byte[] qrcode) {
        this.qrcode = qrcode;
    }

    public ProfileInfo setContentByKey(String key, String newContent) {

        if (mAllProfileInfoList.containsKey(key)) {
            mAllProfileInfoList.remove(key);
        }
        mAllProfileInfoList.put(key, newContent);
        return this;
    }

    public static String getOtherNumberKeyByType(String type) {
        String key = null;
        if (type.equals(OTHER_NUMBER_HOME)) {
            key = OTHER_NUMBER_HOME + (++mHomeNumberCount);
        } else if (type.equals(OTHER_NUMBER_WORK)) {
            key = OTHER_NUMBER_WORK + (++mWorkNumberCount);
        } else if (type.equals(OTHER_NUMBER_FIXED)) {
            key = OTHER_NUMBER_FIXED + (++mFixedNumberCount);
        } else {
            key = OTHER_NUMBER_OTHER + (++mOtherNumberCount);
        }
        return key;
    }

    public ProfileInfo setAllOtherNumber(ArrayList<OtherNumberInfo> list) {
        /* clear old data */
        mOtherNumberArrayList.clear();
        mOtherNumberArrayList.addAll(list);
        return this;
    };

    public ArrayList<OtherNumberInfo> getAllOtherNumber() {
        return mOtherNumberArrayList;
    }

    public String getOtherNumberToString() {
        StringBuffer buffer = new StringBuffer();
        int count = mOtherNumberArrayList.size();
        for (int i = 0; i < count; i++) {
            OtherNumberInfo info = mOtherNumberArrayList.get(i);
            buffer.append(info.key);
            buffer.append("=");
            buffer.append(info.number);
            buffer.append(";");
        }
        int size = buffer.length();
        if (size > 0) {
            buffer.deleteCharAt(size - 1);
        }
        return buffer.toString();
    }

    public static void clearOtherNumbers() {
        mHomeNumberCount = 0;
        mWorkNumberCount = 0;
        mFixedNumberCount = 0;
        mOtherNumberCount = 0;
    }

    public static int getOtherNumberTypeByKey(String key) {
        if (key.contains(ProfileInfo.OTHER_NUMBER_HOME)) {
            return VALUE_NUMBER_TYPE_HOME;
        } else if (key.contains(ProfileInfo.OTHER_NUMBER_WORK)) {
            return VALUE_NUMBER_TYPE_WORK;
        } else if (key.contains(ProfileInfo.OTHER_NUMBER_FIXED)) {
            return VALUE_NUMBER_TYPE_FIXED;
        } else {
            return VALUE_NUMBER_TYPE_OTHER;
        }
    }

    public void parseOtherNumberStringToMap(String numbers) {
        mOtherNumberArrayList.clear();
        if (numbers != null && !numbers.equals("")) {
            String[] numSet = numbers.split(";");
            for (String num : numSet) {
                String[] detailSet = num.split("=");
                String key = detailSet[0];
                String number = detailSet[1];
                int type = getOtherNumberTypeByKey(key);

                OtherNumberInfo info = new OtherNumberInfo(type, number, key);
                mOtherNumberArrayList.add(info);
            }
        }
    }

    public static String getContentByKey(String key) {
        return mAllProfileInfoList.get(key);
    }

    public void clearAll() {
        photo = null;
        mAllProfileInfoList.clear();
        mOtherNumberArrayList.clear();
        clearOtherNumbers();
    }

    @Override
    public String toString() {
        return " ";
    }


}
