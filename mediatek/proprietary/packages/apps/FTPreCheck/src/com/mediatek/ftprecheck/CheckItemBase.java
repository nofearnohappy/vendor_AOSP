/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ftprecheck;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;


public abstract class CheckItemBase {

    private Context mContext;
    private String mCheckType;

    private String mValue;
    private CheckResult mResult = CheckResult.UNKNOWN;

    enum CheckResult {
        SATISFIED,
        UNSATISFIED,
        UNKNOWN
    }

    public CheckItemBase(Context c, String checkType) {
        mContext = c;
        mCheckType = checkType;
    }

    protected Context getContext() {
        return mContext;
    }

    protected String getCheckType() {
        return mCheckType;
    }

    public String getValue() {
        return mValue;
    }

    protected void setValue(String value) {
        mValue = value;
    }

    protected void setValue(int strId) {
        mValue = mContext.getString(strId);
    }

    public CheckResult getCheckResult() {
        return mResult;
    }

    protected void setCheckResult(CheckResult result) {
        mResult = result;
    }

    /**
     * Called when UI starts checking. You should implement your check function here.
     */
    public abstract void onCheck();

    /**
     * Called when UI stops checking(timer over).
     * After call this method, UI will get mValue and mResult soon to display.
     * So, You should confirm mValue and mResult are ready now.
     * If necessary, you may uses this method to do some thing, for example:
     * - If your check item's value & result depends on the UI timer,
     * you only can calculate and set the final mValue & mResult here.
     * - If your check item notifies modem to release some information when UI start checking,
     * you should notifies modem here to stop release when UI stop checking.
     */
    public abstract void onStopCheck();

}

/**
 * ENWINFO URC check item base
 */
abstract class EnwinfoItemBase extends CheckItemBase {

    private final String mTag;

    private int mUrcType = -1; //which is defined in em_info_enum of em_public_struct.h
    private static final int MSG_NW_INFO_URC = 1;
    private NetworkInfoManager mNwInfoManager = null;
    private boolean mUrcArrived = false;

    private final Handler mUrcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FTPCLog.d(mTag, "handleMessage what = " + msg);
            switch (msg.what) {
            case MSG_NW_INFO_URC:
                AsyncResult ar = (AsyncResult) msg.obj;
                String[] data = (String[]) ar.result;
                //data[1] is empty(not null) when URC struct has no member
                FTPCLog.v(mTag, "Receive URC: " + data[0] + ", " + data[1]);
                try {
                    if (Integer.parseInt(data[0]) != mUrcType) {
                        FTPCLog.d(mTag, "Return type error!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    FTPCLog.d(mTag, "Return type error!");
                    return;
                }
                mUrcArrived = true;
                onUrcDataParse(data[1]);
                break;

            default:
                break;
            }
        }
    };

    public EnwinfoItemBase(Context c, String checkType, String tag) {
        super(c, checkType);
        mTag = tag;
        onUrcTypeConfig();
        mNwInfoManager = new NetworkInfoManager(getContext(), mTag);
    }

    public boolean isUrcArrived() {
        return mUrcArrived;
    }

    public void setUrcType(int type) {
        mUrcType = type;
    }

    @Override
    public void onCheck() {
        mNwInfoManager.registerNetwork(mUrcType, mUrcHandler, MSG_NW_INFO_URC);
    }

    @Override
    public void onStopCheck() {
        mNwInfoManager.unregisterNetwork();
        onValueAndResultSet();
    }

    /**
     * set the URC type that you care.
     */
    public abstract void onUrcTypeConfig();

    /**
     * Parse the URC data for your check item.
     * @param data the URC data.
     */
    public abstract void onUrcDataParse(String data);

    /**
     * calculate and set the final value & result here.
     */
    public abstract void onValueAndResultSet();
}



