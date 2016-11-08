/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.mms.plugin;

import com.android.mms.data.ContactList;
import com.mediatek.mms.callback.IWorkingMessageCallback;
import com.mediatek.mms.ext.DefaultOpWorkingMessageExt;
import com.mediatek.mms.ext.IOpMmsDraftDataExt;
import com.mediatek.mms.plugin.MessageUtils;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.SendReq;

public class Op09WorkingMessageExt extends DefaultOpWorkingMessageExt {

    /// M: support mms cc feature, OP09 requested.
    private static final int HAS_CC = (1 << 6);                     // 64

    /// M: support mms cc feature, OP09 requested. this list may contains invalid ones"
    private ContactList mMmsCc;
    private IWorkingMessageCallback mHost;

    public Op09WorkingMessageExt() {

    }

    public void initOpWorkingMessage(IWorkingMessageCallback host) {
        mHost = host;
    }

    @Override
    public void opLoadFinished(IOpMmsDraftDataExt mdd) {
        /// M: add for mms cc feature. OP09 requested.
        String cc = ((Op09MmsDraftDataExt)mdd).getCc();
        if (cc != null && cc.length() != 0) {
            ((Op09MmsDraftDataExt)mdd).setCc(cc);
        }
        setMmsCc(cc);
    }

    @Override
    public SendReq opSendThreadRun(SendReq sendReq) {
        return makeSendReq(sendReq, mMmsCc);
    }

    @Override
    public SendReq opAsyncUpdateDraftMmsMessage(SendReq sendReq) {
        return makeSendReq(sendReq, mMmsCc);
    }

    @Override
    public SendReq opSaveAsMms(SendReq sendReq) {
        return makeSendReq(sendReq, mMmsCc);
    }

    private static SendReq makeSendReq(SendReq sendReq, ContactList ccList) {
        SendReq req = sendReq;
        /// M: support mms cc feature. OP09 requested
        if (MessageUtils.isSupportSendMmsWithCc() && ccList != null && ccList.size() > 0) {
            String[] cc = ccList.getNumbers(false);
            req.setCc(EncodedStringValue.encodeStrings(cc));
        }
        return req;
    }

    /** M: support mms cc feature, OP09 requested.
     *  the Cc list string should like this: "13812343456;testmail@xmail.com;100086;"
     *  attention: I simulate subject methods, so there is no lock object too.
     *  that means these methods need be called in a single thread.
     *  the general logic of mms cc is similar with mms subject.
     */
    public void setMmsCc(String list) {
        if (list != null) {
            ContactList contacts = ContactList.getByNumbers(list, false, false);
            setMmsCc(contacts, false);
        }
    }

    public void setMmsCc(ContactList list, boolean notify) {
        boolean flag = ((list != null) && list.size() > 0);
        mMmsCc = list;
        if (mMmsCc == null) {
            mMmsCc = new ContactList();
        }
        mHost.updateStateExt(HAS_CC, flag, notify);
    }

    public ContactList getMmsCc() {
        /// M: always return a list.
        if (mMmsCc == null) {
            mMmsCc = new ContactList();
        }
        return mMmsCc;
    }

    public boolean hasMmsCc() {
        if (mMmsCc != null && mMmsCc.size() > 0) {
            return true;
        }
        return false;
    }

}

