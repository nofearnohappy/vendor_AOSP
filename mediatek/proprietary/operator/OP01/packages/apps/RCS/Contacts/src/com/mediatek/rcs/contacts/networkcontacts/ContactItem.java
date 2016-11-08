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

import android.util.Log;

import com.mediatek.rcs.contacts.networkcontacts.JsonUtil.JsonTag;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Contacts source item.
 * @author MTK81359
 *
 */
/**
 * @author MTK80963
 *
 */
public class ContactItem extends SyncItem {
    private static final String TAG = "NetworkContacts::ContactItem";
    public static final int INVALID_ID = -1;
    /**
     * Raw contact id for client.
     */
    private int mId = INVALID_ID;

    /*  Note: below field which is marked with question mark means no such info in Data table! */

    //MIME type: CommonDataKinds.StructuredName
    private String mName;
    private String mFamilyName;
    private String mMiddleName;
    private String mGivenName;
    private String mPrefix;
    private String mSuffix;

    //MIME type: CommonDataKinds.Nickname
    private String mNickName;

    //?
    private String mGender;

    //MIME type: CommonDataKinds.Phone
    private List<String> mCarTelList;
    private List<String> mMobileList;
    private List<String> mWorkMobileList;
    private List<String> mHomeMobileList; //? only has TYPE_HOME
    private List<String> mOtherMobileList; //? only has TYPE_OTHER
    private List<String> mTelList; //?
    private List<String> mWorkTelList; //?
    private List<String> mHomeTelList; //?
    private List<String> mOtherTelList; //?
    private List<String> mFaxList; //?
    private List<String> mWorkFaxList;
    private List<String> mHomeFaxList;
    private List<String> mOtherFaxList;
    private List<String> mCompanyTelExchange;
    private List<String> mPager;
    private List<String> mTelTlx;
  //? use CommonDataKinds.Phone.LABEL to define a phone type by myself?
    private List<String> mIphoneList;
    private List<String> mShortTelNumList; //?

    //MIME type: CommonDataKinds.Email
    private List<String> mEmailList; //?
    private List<String> mWorkMailList;
    private List<String> mHomeMailList;
    private List<String> mOtherMailList;

    //MIME type: CommonDataKinds.Website
    private List<String> mWebsiteList; //?
    private List<String> mWorkWebsiteList;
    private List<String> mHomeWebsiteList;
    private List<String> mOtherWebsiteList;

    //MIME type: CommonDataKinds.StructuredPostal
    private List<String> mAddrList; //?
    private List<String> mWorkAddrList; //?
    private List<String> mHomeAddrList; //?
    private List<PostalAddress> mAssembleAddrList; //?
    private List<PostalAddress> mWorkAssembleAddrList;
    private List<PostalAddress> mHomeAssembleAddrList;
    private List<PostalAddress> mOtherAssembleAddrList;

    //MIME type: CommonDataKinds.Organization
    private List<Organization> mAssembleOrgList;

    //MIME type: CommonDataKinds.Note
    private List<String> mNoteList;

    //MIME type: CommonDataKinds.Event
    private List<String> mBirthdayList;
    private List<String> mAnniversaryList;

    //MIME type: CommonDataKinds.Relation
    private List<String> mChildList;
    private List<String> mSpouseList;

    //MIME type: CommonDataKinds.Im
    private List<String> mFetionList; //?
    private List<String> mQQList;
    private List<String> mMsnList;
    private List<String> mWeiboList; //?
    private List<String> mBlogList; //?

    private List<String> mGroupNameList; //?is name should query groups table?

    /**
     * Constructor.
     */
    public ContactItem() {
        super();
        //do nothing, leaving every field as null.
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    /**
     * @param name contact name.
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return name
     */
    public String getName() {
        return mName;
    }

    /**
     * @param familyName family name.
     */
    public void setFamilyName(String familyName) {
        mFamilyName = familyName;
    }

    /**
     * @return family name
     */
    public String getFamilyName() {
        return mFamilyName;
    }

    /**
     * @param middleName middle name.
     */
    public void setMiddleName(String middleName) {
        mMiddleName = middleName;
    }

    /**
     * @return middle name.
     */
    public String getMiddleName() {
        return mMiddleName;
    }

    /**
     * @param givenName given Name
     */
    public void setGivenName(String givenName) {
        mGivenName = givenName;
    }

    /**
     * @return given name.
     */
    public String getGivenName() {
        return mGivenName;
    }

    /**
     * @param prefix prefix
     */
    public void setPrefix(String prefix) {
        mPrefix = prefix;
    }

    /**
     * @return prefix
     */
    public String getPrefix() {
        return mPrefix;
    }

    /**
     * @param suffix suffix
     */
    public void setSuffix(String suffix) {
        mSuffix = suffix;
    }

    /**
     * @return suffix
     */
    public String getSuffix() {
        return mSuffix;
    }

    /**
     * @param nickName nick name
     */
    public void setNickName(String nickName) {
        mNickName = nickName;
    }

    /**
     * @return nick name
     */
    public String getNickName() {
        return mNickName;
    }

    /**
     * @param gender gender
     */
    public void setGender(String gender) {
        mGender = gender;
    }

    /**
     * @return gender
     */
    public String getGender() {
        return mGender;
    }

    /**
     * @param carTelList car tels
     */
    public void setCarTelList(List<String> carTelList) {
        mCarTelList = carTelList;
    }

    /**
     * @return car tels
     */
    public List<String> getCarTelList() {
        return mCarTelList;
    }

    /**
     * @param str car tel
     */
    public void addCarTel(String str) {
        if (null == mCarTelList) {
            mCarTelList = new ArrayList<String>();
        }
        mCarTelList.add(str);
    }

    /**
     * @param mobileList mobile tels
     */
    public void setMobileList(List<String> mobileList) {
        mMobileList = mobileList;
    }

    /**
     * @return mobile tels
     */
    public List<String> getMobileList() {
        return mMobileList;
    }

    /**
     * @param str mobile tel
     */
    public void addMobile(String str) {
        if (null == mMobileList) {
            mMobileList = new ArrayList<String>();
        }
        mMobileList.add(str);
    }

    /**
     * @param workMobileList work mobile tels
     */
    public void setWorkMobileList(List<String> workMobileList) {
        mWorkMobileList = workMobileList;
    }

    /**
     * @return work mobile tels
     */
    public List<String> getWorkMobileList() {
        return mWorkMobileList;
    }

    /**
     * @param str work mobile
     */
    public void addWorkMobile(String str) {
        if (null == mWorkMobileList) {
            mWorkMobileList = new ArrayList<String>();
        }
        mWorkMobileList.add(str);
    }

    /**
     * @param homeMobileList home mobile tels
     */
    public void setHomeMobileList(List<String> homeMobileList) {
        mHomeMobileList = homeMobileList;
    }

    /**
     * @return company tel exchanges.
     */
    public List<String> getCompanyTelExchangeList() {
        return mCompanyTelExchange;
    }

    /**
     * @param str company tel exchanges
     */
    public void addCompanyTelExchange(String str) {
        if (null == mCompanyTelExchange) {
            mCompanyTelExchange = new ArrayList<String>();
        }
        mCompanyTelExchange.add(str);
    }

    /**
     * @param list company tel exchanges list
     */
    public void setCompanyTelExchangeList(List<String> list) {
        mCompanyTelExchange = list;
    }

    /**
     * @return pagers.
     */
    public List<String> getPagerList() {
        return mPager;
    }

    /**
     * @param str pagers
     */
    public void addPager(String str) {
        if (null == mPager) {
            mPager = new ArrayList<String>();
        }
        mPager.add(str);
    }

    /**
     * @param list pager list.
     */
    public void setPagerList(List<String> list) {
        mPager = list;
    }

    /**
     * @return tel tlxs.
     */
    public List<String> getTelTlxList() {
        return mTelTlx;
    }

    /**
     * @param str tel tlxs.
     */
    public void addTelTlx(String str) {
        if (null == mTelTlx) {
            mTelTlx = new ArrayList<String>();
        }
        mTelTlx.add(str);
    }

    /**
     * @param list tel tlx list.
     */
    public void setTelTlx(List<String> list) {
        mTelTlx = list;
    }

    /**
     * @return home mobiles
     */
    public List<String> getHomeMobileList() {
        return mHomeMobileList;
    }

    /**
     * @param str home mobile
     */
    public void addHomeMobile(String str) {
        if (null == mHomeMobileList) {
            mHomeMobileList = new ArrayList<String>();
        }
        mHomeMobileList.add(str);
    }

    /**
     * @param otherMobileList other mobile tels
     */
    public void setOtherMobileList(List<String> otherMobileList) {
        mOtherMobileList = otherMobileList;
    }

    /**
     * @return other mobile tels
     */
    public List<String> getOtherMobileList() {
        return mOtherMobileList;
    }

    /**
     * @param str other mobile
     */
    public void addOtherMobile(String str) {
        if (null == mOtherMobileList) {
            mOtherMobileList = new ArrayList<String>();
        }
        mOtherMobileList.add(str);
    }

    /**
     * @param iphoneList IP phones
     */
    public void setIphoneList(List<String> iphoneList) {
        mIphoneList = iphoneList;
    }

    /**
     * @return IP phones
     */
    public List<String> getIphoneList() {
        return mIphoneList;
    }

    /**
     * @param str IP phone
     */
    public void addIphone(String str) {
        if (null == mIphoneList) {
            mIphoneList = new ArrayList<String>();
        }
        mIphoneList.add(str);
    }

    /**
     * @param telList telList
     */
    public void setTelList(List<String> telList) {
        mTelList = telList;
    }

    /**
     * @return getTelList
     */
    public List<String> getTelList() {
        return mTelList;
    }

    /**
     * @param str tel
     */
    public void addTel(String str) {
        if (null == mTelList) {
            mTelList = new ArrayList<String>();
        }
        mTelList.add(str);
    }

    /**
     * @param workTelList workTelList
     */
    public void setWorkTelList(List<String> workTelList) {
        mWorkTelList = workTelList;
    }

    /**
     * @return workTelList
     */
    public List<String> getWorkTelList() {
        return mWorkTelList;
    }

    /**
     * @param str data
     */
    public void addWorkTel(String str) {
        if (null == mWorkTelList) {
            mWorkTelList = new ArrayList<String>();
        }
        mWorkTelList.add(str);
    }

    /**
     * @param homeTelList homeTelList
     */
    public void setHomeTelList(List<String> homeTelList) {
        mHomeTelList = homeTelList;
    }

    /**
     * @return homeTelList
     */
    public List<String> getHomeTelList() {
        return mHomeTelList;
    }

    /**
     * @param str data
     */
    public void addHomeTel(String str) {
        if (null == mHomeTelList) {
            mHomeTelList = new ArrayList<String>();
        }
        mHomeTelList.add(str);
    }

    /**
     * @param otherTelList otherTelList
     */
    public void setOtherTelList(List<String> otherTelList) {
        mOtherTelList = otherTelList;
    }

    /**
     * @return otherTelList
     */
    public List<String> getOtherTelList() {
        return mOtherTelList;
    }

    /**
     * @param str data
     */
    public void addOtherTel(String str) {
        if (null == mOtherTelList) {
            mOtherTelList = new ArrayList<String>();
        }
        mOtherTelList.add(str);
    }

    /**
     * @param shortTelNumList shortTelNumList
     */
    public void setShortTelNumList(List<String> shortTelNumList) {
        mShortTelNumList = shortTelNumList;
    }

    /**
     * @return shortTelNumList
     */
    public List<String> getShortTelNumList() {
        return mShortTelNumList;
    }

    /**
     * @param str data
     */
    public void addShortTelNum(String str) {
        if (null == mShortTelNumList) {
            mShortTelNumList = new ArrayList<String>();
        }
        mShortTelNumList.add(str);
    }

    /**
     * @param faxList faxList
     */
    public void setFaxList(List<String> faxList) {
        mFaxList = faxList;
    }

    /**
     * @return faxList
     */
    public List<String> getFaxList() {
        return mFaxList;
    }

    /**
     * @param str data
     */
    public void addFax(String str) {
        if (null == mFaxList) {
            mFaxList = new ArrayList<String>();
        }
        mFaxList.add(str);
    }

    /**
     * @param workFaxList workFaxList
     */
    public void setWorkFaxList(List<String> workFaxList) {
        mWorkFaxList = workFaxList;
    }

    /**
     * @return workFaxList
     */
    public List<String> getWorkFaxList() {
        return mWorkFaxList;
    }

    /**
     * @param str data
     */
    public void addWorkFax(String str) {
        if (null == mWorkFaxList) {
            mWorkFaxList = new ArrayList<String>();
        }
        mWorkFaxList.add(str);
    }

    /**
     * @param homeFaxList homeFaxList
     */
    public void setHomeFaxList(List<String> homeFaxList) {
        mHomeFaxList = homeFaxList;
    }

    /**
     * @return homeFaxList
     */
    public List<String> getHomeFaxList() {
        return mHomeFaxList;
    }

    /**
     * @param str data
     */
    public void addHomeFax(String str) {
        if (null == mHomeFaxList) {
            mHomeFaxList = new ArrayList<String>();
        }
        mHomeFaxList.add(str);
    }

    /**
     * @param otherFaxList otherFaxList
     */
    public void setOtherFaxList(List<String> otherFaxList) {
        mOtherFaxList = otherFaxList;
    }

    /**
     * @return otherFaxList
     */
    public List<String> getOtherFaxList() {
        return mOtherFaxList;
    }

    /**
     * @param str data
     */
    public void addOtherFax(String str) {
        if (null == mOtherFaxList) {
            mOtherFaxList = new ArrayList<String>();
        }
        mOtherFaxList.add(str);
    }

    /**
     * @param emailList emailList
     */
    public void setEmailList(List<String> emailList) {
        mEmailList = emailList;
    }

    /**
     * @return emailList
     */
    public List<String> getEmailList() {
        return mEmailList;
    }

    /**
     * @param str data
     */
    public void addEmail(String str) {
        if (null == mEmailList) {
            mEmailList = new ArrayList<String>();
        }
        mEmailList.add(str);
    }

    /**
     * @param workMailList workMailList
     */
    public void setWorkMailList(List<String> workMailList) {
        mWorkMailList = workMailList;
    }

    /**
     * @return workMailList
     */
    public List<String> getWorkMailList() {
        return mWorkMailList;
    }

    /**
     * @param str data
     */
    public void addWorkMail(String str) {
        if (null == mWorkMailList) {
            mWorkMailList = new ArrayList<String>();
        }
        mWorkMailList.add(str);
    }

    /**
     * @param homeMailList homeMailList
     */
    public void setHomeMailList(List<String> homeMailList) {
        mHomeMailList = homeMailList;
    }

    /**
     * @return homeMailList
     */
    public List<String> getHomeMailList() {
        return mHomeMailList;
    }

    /**
     * @param str data
     */
    public void addHomeMail(String str) {
        if (null == mHomeMailList) {
            mHomeMailList = new ArrayList<String>();
        }
        mHomeMailList.add(str);
    }

    /**
     * @param otherMailList otherMailList
     */
    public void setOtherMailList(List<String> otherMailList) {
        mOtherMailList = otherMailList;
    }

    /**
     * @return otherMailList
     */
    public List<String> getOtherMailList() {
        return mOtherMailList;
    }

    /**
     * @param str data
     */
    public void addOtherMail(String str) {
        if (null == mOtherMailList) {
            mOtherMailList = new ArrayList<String>();
        }
        mOtherMailList.add(str);
    }

    /**
     * @param websiteList websiteList
     */
    public void setWebsiteList(List<String> websiteList) {
        mWebsiteList = websiteList;
    }

    /**
     * @return websiteList
     */
    public List<String> getWebsiteList() {
        return mWebsiteList;
    }

    /**
     * @param str data
     */
    public void addWebsite(String str) {
        if (null == mWebsiteList) {
            mWebsiteList = new ArrayList<String>();
        }
        mWebsiteList.add(str);
    }

    /**
     * @param workWebsiteList workWebsiteList
     */
    public void setWorkWebsiteList(List<String> workWebsiteList) {
        mWorkWebsiteList = workWebsiteList;
    }

    /**
     * @return workWebsiteList
     */
    public List<String> getWorkWebsiteList() {
        return mWorkWebsiteList;
    }

    /**
     * @param str data
     */
    public void addWorkWebsite(String str) {
        if (null == mWorkWebsiteList) {
            mWorkWebsiteList = new ArrayList<String>();
        }
        mWorkWebsiteList.add(str);
    }

    /**
     * @param homeWebsiteList homeWebsiteList
     */
    public void setHomeWebsiteList(List<String> homeWebsiteList) {
        mHomeWebsiteList = homeWebsiteList;
    }

    /**
     * @return homeWebsiteList
     */
    public List<String> getHomeWebsiteList() {
        return mHomeWebsiteList;
    }

    /**
     * @param str data
     */
    public void addHomeWebsite(String str) {
        if (null == mHomeWebsiteList) {
            mHomeWebsiteList = new ArrayList<String>();
        }
        mHomeWebsiteList.add(str);
    }

    /**
     * @param otherWebsiteList otherWebsiteList
     */
    public void setOtherWebsiteList(List<String> otherWebsiteList) {
        mOtherWebsiteList = otherWebsiteList;
    }

    /**
     * @return otherWebsiteList
     */
    public List<String> getOtherWebsiteList() {
        return mOtherWebsiteList;
    }

    /**
     * @param str data
     */
    public void addOtherWebsite(String str) {
        if (null == mOtherWebsiteList) {
            mOtherWebsiteList = new ArrayList<String>();
        }
        mOtherWebsiteList.add(str);
    }

    /**
     * @param addrList addrList
     */
    public void setAddrList(List<String> addrList) {
        mAddrList = addrList;
    }

    /**
     * @return addrList
     */
    public List<String> getAddrList() {
        return mAddrList;
    }

    /**
     * @param str data
     */
    public void addAddr(String str) {
        if (null == mAddrList) {
            mAddrList = new ArrayList<String>();
        }
        mAddrList.add(str);
    }

    /**
     * @param workAddrList workAddrList
     */
    public void setWorkAddrList(List<String> workAddrList) {
        mWorkAddrList = workAddrList;
    }

    /**
     * @return workAddrList
     */
    public List<String> getWorkAddrList() {
        return mWorkAddrList;
    }

    /**
     * @param str data
     */
    public void addWorkAddr(String str) {
        if (null == mWorkAddrList) {
            mWorkAddrList = new ArrayList<String>();
        }
        mWorkAddrList.add(str);
    }

    /**
     * @param homeAddrList homeAddrList
     */
    public void setHomeAddrList(List<String> homeAddrList) {
        mHomeAddrList = homeAddrList;
    }

    /**
     * @return homeAddrList
     */
    public List<String> getHomeAddrList() {
        return mHomeAddrList;
    }

    /**
     * @param str data
     */
    public void addHomeAddr(String str) {
        if (null == mHomeAddrList) {
            mHomeAddrList = new ArrayList<String>();
        }
        mHomeAddrList.add(str);
    }

    /**
     * @param assembleAddrList assembleAddrList
     */
    public void setAssembleAddrList(List<PostalAddress> assembleAddrList) {
        mAssembleAddrList = assembleAddrList;
    }

    /**
     * @return assembleAddrList
     */
    public List<PostalAddress> getAssembleAddrList() {
        return mAssembleAddrList;
    }

    /**
     * @param addr addr
     */
    public void addAssembleAddr(PostalAddress addr) {
        if (null == mAssembleAddrList) {
            mAssembleAddrList = new ArrayList<PostalAddress>();
        }
        mAssembleAddrList.add(addr);
    }

    /**
     * @param workAssembleAddrList workAssembleAddrList
     */
    public void setWorkAssembleAddrList(List<PostalAddress> workAssembleAddrList) {
        mWorkAssembleAddrList = workAssembleAddrList;
    }

    /**
     * @return workAssembleAddrList
     */
    public List<PostalAddress> getWorkAssembleAddrList() {
        return mWorkAssembleAddrList;
    }

    /**
     * @param addr addr
     */
    public void addWorkAssembleAddr(PostalAddress addr) {
        if (null == mWorkAssembleAddrList) {
            mWorkAssembleAddrList = new ArrayList<PostalAddress>();
        }
        mWorkAssembleAddrList.add(addr);
    }

    /**
     * @param homeAssembleAddrList homeAssembleAddrList
     */
    public void setHomeAssembleAddrList(List<PostalAddress> homeAssembleAddrList) {
        mHomeAssembleAddrList = homeAssembleAddrList;
    }

    /**
     * @return homeAssembleAddrList
     */
    public List<PostalAddress> getHomeAssembleAddrList() {
        return mHomeAssembleAddrList;
    }

    /**
     * @param addr addr
     */
    public void addHomeAssembleAddr(PostalAddress addr) {
        if (null == mHomeAssembleAddrList) {
            mHomeAssembleAddrList = new ArrayList<PostalAddress>();
        }
        mHomeAssembleAddrList.add(addr);
    }

    /**
     * @param otherAssembleAddrList otherAssembleAddrList
     */
    public void setOtherAssembleAddrList(List<PostalAddress> otherAssembleAddrList) {
        mOtherAssembleAddrList = otherAssembleAddrList;
    }

    /**
     * @return otherAssembleAddrList
     */
    public List<PostalAddress> getOtherAssembleAddrList() {
        return mOtherAssembleAddrList;
    }

    /**
     * @param addr addr
     */
    public void addOtherAssembleAddr(PostalAddress addr) {
        if (null == mOtherAssembleAddrList) {
            mOtherAssembleAddrList = new ArrayList<PostalAddress>();
        }
        mOtherAssembleAddrList.add(addr);
    }

    /**
     * @param assembleOrgList assembleOrgList
     */
    public void setAssembleOrgList(List<Organization> assembleOrgList) {
        mAssembleOrgList = assembleOrgList;
    }

    /**
     * @return assembleOrgList
     */
    public List<Organization> getAssembleOrgList() {
        return mAssembleOrgList;
    }

    /**
     * @param org org
     */
    public void addAssembleOrg(Organization org) {
        if (null == mAssembleOrgList) {
            mAssembleOrgList = new ArrayList<Organization>();
        }
        mAssembleOrgList.add(org);
    }

    /**
     * @param noteList noteList
     */
    public void setNoteList(List<String> noteList) {
        mNoteList = noteList;
    }

    /**
     * @return noteList
     */
    public List<String> getNoteList() {
        return mNoteList;
    }

    /**
     * @param str data
     */
    public void addNote(String str) {
        if (null == mNoteList) {
            mNoteList = new ArrayList<String>();
        }
        mNoteList.add(str);
    }

    /**
     * @param birthdayList birthdayList
     */
    public void setBirthdayList(List<String> birthdayList) {
        mBirthdayList = birthdayList;
    }

    /**
     * @return birthdayList
     */
    public List<String> getBirthdayList() {
        return mBirthdayList;
    }

    /**
     * @param str data
     */
    public void addBirthday(String str) {
        if (null == mBirthdayList) {
            mBirthdayList = new ArrayList<String>();
        }
        mBirthdayList.add(str);
    }

    /**
     * @param anniversaryList anniversaryList
     */
    public void setAnniversaryList(List<String> anniversaryList) {
        mAnniversaryList = anniversaryList;
    }

    /**
     * @return anniversaryList
     */
    public List<String> getAnniversaryList() {
        return mAnniversaryList;
    }

    /**
     * @param str data
     */
    public void addAnniversary(String str) {
        if (null == mAnniversaryList) {
            mAnniversaryList = new ArrayList<String>();
        }
        mAnniversaryList.add(str);
    }

    /**
     * @param childList childList
     */
    public void setChildList(List<String> childList) {
        mChildList = childList;
    }

    /**
     * @return childList
     */
    public List<String> getChildList() {
        return mChildList;
    }

    /**
     * @param str data
     */
    public void addChild(String str) {
        if (null == mChildList) {
            mChildList = new ArrayList<String>();
        }
        mChildList.add(str);
    }

    /**
     * @param spouseList spouseList
     */
    public void setSpouseList(List<String> spouseList) {
        mSpouseList = spouseList;
    }

    /**
     * @return spouseList
     */
    public List<String> getSpouseList() {
        return mSpouseList;
    }

    /**
     * @param str data
     */
    public void addSpouse(String str) {
        if (null == mSpouseList) {
            mSpouseList = new ArrayList<String>();
        }
        mSpouseList.add(str);
    }

    /**
     * @param fetionList fetionList
     */
    public void setFetionList(List<String> fetionList) {
        mFetionList = fetionList;
    }

    /**
     * @return fetionList
     */
    public List<String> getFetionList() {
        return mFetionList;
    }

    /**
     * @param str data
     */
    public void addFetion(String str) {
        if (null == mFetionList) {
            mFetionList = new ArrayList<String>();
        }
        mFetionList.add(str);
    }

    /**
     * @param qqList qqList
     */
    public void setQQList(List<String> qqList) {
        mQQList = qqList;
    }

    /**
     * @return qqList
     */
    public List<String> getQQList() {
        return mQQList;
    }

    /**
     * @param str data
     */
    public void addQQ(String str) {
        if (null == mQQList) {
            mQQList = new ArrayList<String>();
        }
        mQQList.add(str);
    }

    /**
     * @param msnList msnList
     */
    public void setMsnList(List<String> msnList) {
        mMsnList = msnList;
    }

    /**
     * @return msnList
     */
    public List<String> getMsnList() {
        return mMsnList;
    }

    /**
     * @param str data
     */
    public void addMsn(String str) {
        if (null == mMsnList) {
            mMsnList = new ArrayList<String>();
        }
        mMsnList.add(str);
    }

    /**
     * @param weiboList weiboList
     */
    public void setWeiboList(List<String> weiboList) {
        mWeiboList = weiboList;
    }

    /**
     * @return weiboList
     */
    public List<String> getWeiboList() {
        return mWeiboList;
    }

    /**
     * @param str data
     */
    public void addWeibo(String str) {
        if (null == mWeiboList) {
            mWeiboList = new ArrayList<String>();
        }
        mWeiboList.add(str);
    }

    /**
     * @param blogList blogList
     */
    public void setBlogList(List<String> blogList) {
        mBlogList = blogList;
    }

    /**
     * @return blogList
     */
    public List<String> getBlogList() {
        return mBlogList;
    }

    /**
     * @param str data
     */
    public void addBlog(String str) {
        if (null == mBlogList) {
            mBlogList = new ArrayList<String>();
        }
        mBlogList.add(str);
    }

    /**
     * @param groupNameList groupNameList
     */
    public void setGroupNameList(List<String> groupNameList) {
        mGroupNameList = groupNameList;
    }

    /**
     * @return groupNameList
     */
    public List<String> getGroupNameList() {
        return mGroupNameList;
    }

    /**
     * @param str data
     */
    public void addGroupName(String str) {
        if (null == mGroupNameList) {
            mGroupNameList = new ArrayList<String>();
        }
        mGroupNameList.add(str);
    }


    @Override
    public String toData() {
        //Should take care to handle the null fields.
        Log.i(TAG, "+toData");
        try {
            JSONObject jContact = new JSONObject();
            putJsonString(jContact, JsonTag.NAME, getName());
            putJsonString(jContact, JsonTag.FAMILY_NAME, getFamilyName());
            putJsonString(jContact, JsonTag.MIDDLE_NAME, getMiddleName());
            putJsonString(jContact, JsonTag.GIVEN_NAME, getGivenName());
            putJsonString(jContact, JsonTag.NICK_NAME, getNickName());
            putJsonString(jContact, JsonTag.GENDER, getGender());
            putJsonString(jContact, JsonTag.PREFIX, getPrefix());
            putJsonString(jContact, JsonTag.SUFFIX, getSuffix());
            putJsonStringArray(jContact, JsonTag.CAR_TEL, getCarTelList());
            putJsonStringArray(jContact, JsonTag.MOBILE, getMobileList());
            putJsonStringArray(jContact, JsonTag.WORK_MOBILE, getWorkMobileList());
            putJsonStringArray(jContact, JsonTag.HOME_MOBILE, getHomeMobileList());
            putJsonStringArray(jContact, JsonTag.OTHER_MOBILE, getOtherMobileList());
            putJsonStringArray(jContact, JsonTag.IPHONE, getIphoneList());
            putJsonStringArray(jContact, JsonTag.TEL, getTelList());
            putJsonStringArray(jContact, JsonTag.WORK_TEL, getWorkTelList());
            putJsonStringArray(jContact, JsonTag.HOME_TEL, getHomeTelList());
            putJsonStringArray(jContact, JsonTag.OTHER_TEL, getOtherTelList());
            putJsonStringArray(jContact, JsonTag.SHORT_TEL_NUM, getShortTelNumList());
            putJsonStringArray(jContact, JsonTag.FAX, getFaxList());
            putJsonStringArray(jContact, JsonTag.WORK_FAX, getWorkFaxList());
            putJsonStringArray(jContact, JsonTag.HOME_FAX, getHomeFaxList());
            putJsonStringArray(jContact, JsonTag.OTHER_FAX, getOtherFaxList());
            putJsonStringArray(jContact, JsonTag.COMPANY_TEL_EX, getCompanyTelExchangeList());
            putJsonStringArray(jContact, JsonTag.PAGER, getPagerList());
            putJsonStringArray(jContact, JsonTag.TEL_TLX, getTelTlxList());
            putJsonStringArray(jContact, JsonTag.EMAIL, getEmailList());
            putJsonStringArray(jContact, JsonTag.WORK_MAIL, getWorkMailList());
            putJsonStringArray(jContact, JsonTag.HOME_MAIL, getHomeMailList());
            putJsonStringArray(jContact, JsonTag.OTHER_MAIL, getOtherMailList());
            putJsonStringArray(jContact, JsonTag.WEBSITE, getWebsiteList());
            putJsonStringArray(jContact, JsonTag.WORK_WEBSITE, getWorkWebsiteList());
            putJsonStringArray(jContact, JsonTag.HOME_WEBSITE, getHomeWebsiteList());
            putJsonStringArray(jContact, JsonTag.OTHER_WEBSITE, getOtherWebsiteList());
            putJsonStringArray(jContact, JsonTag.ADDRESS, getAddrList());
            putJsonStringArray(jContact, JsonTag.WORK_ADDRESS, getWorkAddrList());
            putJsonStringArray(jContact, JsonTag.HOME_ADDRESS, getHomeAddrList());
            putJsonPostalAddrArray(jContact, JsonTag.ASSEMBLE_ADDR, getAssembleAddrList());
            putJsonPostalAddrArray(jContact, JsonTag.WORK_ASSEMBLE_ADDR, getWorkAssembleAddrList());
            putJsonPostalAddrArray(jContact, JsonTag.HOME_ASSEMBLE_ADDR, getHomeAssembleAddrList());
            putJsonPostalAddrArray(jContact, JsonTag.OTHER_ASSEMBLE_ADDR,
                    getOtherAssembleAddrList());
            putJsonOrgArray(jContact, JsonTag.ASSEMBLE_ORG, getAssembleOrgList());
            putJsonStringArray(jContact, JsonTag.NOTE, getNoteList());
            putJsonStringArray(jContact, JsonTag.BIRTHDAY, getBirthdayList());
            putJsonStringArray(jContact, JsonTag.ANNIVERSARY, getAnniversaryList());
            putJsonStringArray(jContact, JsonTag.CHILD, getChildList());
            putJsonStringArray(jContact, JsonTag.SPOUSE, getSpouseList());
            putJsonStringArray(jContact, JsonTag.FETION, getFetionList());
            putJsonStringArray(jContact, JsonTag.QQ, getQQList());
            putJsonStringArray(jContact, JsonTag.MSN, getMsnList());
            putJsonStringArray(jContact, JsonTag.WEIBO, getWeiboList());
            putJsonStringArray(jContact, JsonTag.BLOG, getBlogList());
            putJsonStringArray(jContact, JsonTag.GROUP_NAME, getGroupNameList());
            return jContact.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void putJsonString(JSONObject jsonTarget, String jsonTag,
            String value) throws JSONException {
        // if value is null, putOpt() will do nothing.
        jsonTarget.putOpt(jsonTag, value);
    }

    private void putJsonStringArray(JSONObject jsonTarget, String jsonTag,
            List<String> valueList) throws JSONException {
        if (valueList != null) {
            JSONArray jsonArray = new JSONArray(valueList);
            jsonTarget.putOpt(jsonTag, jsonArray);
        }
    }

    private void putJsonPostalAddrArray(JSONObject jsonTarget, String jsonTag,
            List<PostalAddress> valueList) throws JSONException {
        if (valueList != null) {
            JSONArray jArray = new JSONArray();
            for (PostalAddress addr : valueList) {
                JSONObject jAddr = new JSONObject();
                jAddr.put(JsonTag.STATE, addr.getState());
                jAddr.put(JsonTag.AREA, addr.getArea());
                jAddr.put(JsonTag.CITY, addr.getCity());
                jAddr.put(JsonTag.STREET, addr.getStreet());
                jAddr.put(JsonTag.POSTAL_CODE, addr.getPostalCode());
                jArray.put(jAddr);
            }
            jsonTarget.put(jsonTag, jArray);
        }
    }

    private void putJsonOrgArray(JSONObject jsonTarget, String jsonTag,
            List<Organization> valueList) throws JSONException {
        if (valueList != null) {
            JSONArray jArray = new JSONArray();
            for (Organization org : valueList) {
                JSONObject jOrg = new JSONObject();
                jOrg.put(JsonTag.COMPANY, org.getCompany());
                jOrg.put(JsonTag.DEPARTMENT, org.getDepartment());
                jOrg.put(JsonTag.POSITION, org.getPosition());
                jArray.put(jOrg);
            }
            jsonTarget.put(jsonTag, jArray);
        }
    }


    @Override
    public String toString() {
        return "ContactItem(" + hashCode() + "): "
                + "{"
                + "mId: " + mId + ", "
//                + "mGlobleId: " + mGlobleId + ", "
                + "mName: " + (null != mName ? mName : "null") + ", "
                + "mFamilyName: " + (null != mFamilyName ? mFamilyName : "null") + ", "
                + "mMiddleName: " + (null != mMiddleName ? mMiddleName : "null") + ", "
                + "mGivenName: " + (null != mGivenName ? mGivenName : "null") + ", "
                + "mPrefix: " + (null != mPrefix ? mPrefix : "null") + ", "
                + "mSuffix: " + (null != mSuffix ? mSuffix : "null") + ", "
                + "mNickName: " + (null != mNickName ? mNickName : "null") + ", "
                + "mGender: " + (null != mGender ? mGender : "null") + ", "
                + "mCarTelList: " + (null != mCarTelList ? mCarTelList : "null") + ", "
                + "mMobileList: " + (null != mMobileList ? mMobileList : "null") + ", "
                + "mWorkMobileList: " + (null != mWorkMobileList ? mWorkMobileList : "null") + ", "
                + "mHomeMobileList: " + (null != mHomeMobileList ? mHomeMobileList : "null") + ", "
                + "mOtherMobileList: "
                + (null != mOtherMobileList ? mOtherMobileList : "null")
                + ", "
                + "mTelList: " + (null != mTelList ? mTelList : "null") + ", "
                + "mWorkTelList: " + (null != mWorkTelList ? mWorkTelList : "null") + ", "
                + "mHomeTelList: " + (null != mHomeTelList ? mHomeTelList : "null") + ", "
                + "mOtherTelList: " + (null != mOtherTelList ? mOtherTelList : "null") + ", "
                + "mFaxList: " + (null != mFaxList ? mFaxList : "null") + ", "
                + "mWorkFaxList: " + (null != mWorkFaxList ? mWorkFaxList : "null") + ", "
                + "mHomeFaxList: " + (null != mHomeFaxList ? mHomeFaxList : "null") + ", "
                + "mOtherFaxList: " + (null != mOtherFaxList ? mOtherFaxList : "null") + ", "
                + "mCompanyTelExchangeList: " + (null != mCompanyTelExchange ? mCompanyTelExchange : "null") + ", "
                + "mPagerList: " + (null != mPager ? mPager : "null") + ", "
                + "mTelTlxList: " + (null != mTelTlx ? mTelTlx : "null") + ", "
                + "mIphoneList: " + (null != mIphoneList ? mIphoneList : "null") + ", "
                + "mShortTelNumList: "
                + (null != mShortTelNumList ? mShortTelNumList : "null")
                + ", "
                + "mEmailList: "
                + (null != mEmailList ? mEmailList : "null")
                + ", "
                + "mWorkMailList: "
                + (null != mWorkMailList ? mWorkMailList : "null")
                + ", "
                + "mHomeMailList: "
                + (null != mHomeMailList ? mHomeMailList : "null")
                + ", "
                + "mOtherMailList: "
                + (null != mOtherMailList ? mOtherMailList : "null")
                + ", "
                + "mWebsiteList: "
                + (null != mWebsiteList ? mWebsiteList : "null")
                + ", "
                + "mWorkWebsiteList: "
                + (null != mWorkWebsiteList ? mWorkWebsiteList : "null")
                + ", "
                + "mHomeWebsiteList: "
                + (null != mHomeWebsiteList ? mHomeWebsiteList : "null")
                + ", "
                + "mOtherWebsiteList: "
                + (null != mOtherWebsiteList ? mOtherWebsiteList : "null")
                + ", "
                + "mAddrList: "
                + (null != mAddrList ? mAddrList : "null")
                + ", "
                + "mWorkAddrList: "
                + (null != mWorkAddrList ? mWorkAddrList : "null")
                + ", "
                + "mHomeAddrList: "
                + (null != mHomeAddrList ? mHomeAddrList : "null")
                + ", "
                + "mAssembleAddrList: "
                + (null != mAssembleAddrList ? mAssembleAddrList : "null")
                + ", "
                + "mWorkAssembleAddrList: "
                + (null != mWorkAssembleAddrList ? mWorkAssembleAddrList
                        : "null")
                + ", "
                + "mHomeAssembleAddrList: "
                + (null != mHomeAssembleAddrList ? mHomeAssembleAddrList
                        : "null")
                + ", "
                + "mOtherAssembleAddrList: "
                + (null != mOtherAssembleAddrList ? mOtherAssembleAddrList
                        : "null") + ", " + "mAssembleOrgList: "
                + (null != mAssembleOrgList ? mAssembleOrgList : "null") + ", "
                + "mNoteList: " + (null != mNoteList ? mNoteList : "null")
                + ", " + "mBirthdayList: "
                + (null != mBirthdayList ? mBirthdayList : "null") + ", "
                + "mAnniversaryList: "
                + (null != mAnniversaryList ? mAnniversaryList : "null") + ", "
                + "mChildList: " + (null != mChildList ? mChildList : "null") + ", "
                + "mSpouseList: " + (null != mSpouseList ? mSpouseList : "null") + ", "
                + "mFetionList: " + (null != mFetionList ? mFetionList : "null") + ", "
                + "mQQList: " + (null != mQQList ? mQQList : "null") + ", "
                + "mMsnList: " + (null != mMsnList ? mMsnList : "null") + ", "
                + "mWeiboList: " + (null != mWeiboList ? mWeiboList : "null") + ", "
                + "mBlogList: " + (null != mBlogList ? mBlogList : "null") + ", "
                + "mGroupNameList: " + (null != mGroupNameList ? mGroupNameList : "null") + ", "
                + "}";
    }
}

/**
 * @author MTK81359
 *
 */
class PostalAddress {
    private String mState;
    private String mArea;
    private String mCity;
    private String mStreet;
    private String mPostalCode;

    public PostalAddress() {

    }

    public PostalAddress(String state, String area, String city,
            String street, String postalCode) {
        mState = state;
        mArea = area;
        mCity = city;
        mStreet = street;
        mPostalCode = postalCode;
    }

    public String getState() {
        return mState;
    }

    public String getArea() {
        return mArea;
    }

    public String getCity() {
        return mCity;
    }

    public String getStreet() {
        return mStreet;
    }

    public String getPostalCode() {
        return mPostalCode;
    }

    public void setState(String state) {
        mState = state;
    }

    public void setArea(String area) {
        mArea = area;
    }

    public void setCity(String city) {
        mCity = city;
    }

    public void setStreet(String street) {
        mStreet = street;
    }

    public void setPostalCode(String postalCode) {
        mPostalCode = postalCode;
    }

    @Override
    public String toString() {
        return "PostalAddress(" + hashCode() + "): "
                + "{"
                + "mState: " + (null != mState ? mState : "null") + ", "
                + "mArea: " + (null != mArea ? mArea : "null") + ", "
                + "mCity: " + (null != mCity ? mCity : "null") + ", "
                + "mStreet: " + (null != mStreet ? mStreet : "null") + ", "
                + "mPostalCode: " + (null != mPostalCode ? mPostalCode : "null") + ", "
                + "}";
    }
}

/**
 * @author MTK81359
 *
 */
class Organization {
    private String mCompany;
    private String mDepartment;
    private String mPosition;

    public Organization() {

    }

    public Organization(String company, String department, String position) {
        mCompany = company;
        mDepartment = department;
        mPosition = position;
    }

    public String getCompany() {
        return mCompany;
    }

    public String getDepartment() {
        return mDepartment;
    }

    public String getPosition() {
        return mPosition;
    }

    public void setCompany(String company) {
        mCompany = company;
    }

    public void setDepartment(String department) {
        mDepartment = department;
    }

    public void setPosition(String position) {
        mPosition = position;
    }

    @Override
    public String toString() {
        return "Organization(" + hashCode() + "): "
                + "{"
                + "mCompany: " + (null != mCompany ? mCompany : "null") + ", "
                + "mDepartment: " + (null != mDepartment ? mDepartment : "null") + ", "
                + "mPosition: " + (null != mPosition ? mPosition : "null") + ", "
                + "}";
    }
}
