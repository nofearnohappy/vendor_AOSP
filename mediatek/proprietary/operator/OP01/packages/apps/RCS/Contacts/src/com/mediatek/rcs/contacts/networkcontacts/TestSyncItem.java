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

/**
 * An temperary class for test.
 * @author MTK80963
 *
 */
public class TestSyncItem extends SyncItem {

    private int mId = 0;

    /**
     * Constructor.
     */
    public TestSyncItem() {
        super();
    }

    /**
     * @param data data
     */
    public TestSyncItem(String data) {
        super(data);
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public String toData() {
        return "BEGIN:VCARD"
            + "\nVERSION:2.1"
            + "\nN;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:=E7=AD=96;=E7=95=A5;;;"
            + "\nTEL;CELL:13520988416"
            + "\nNICKNAME:"
            + "\nBDAY:"
            + "\nTITLE:"
            + "\nORG:;"
            + "\nNOTE:"
            + "\nX-ANNIVERSARY:"
            + "\nX-FUNAMBOL-CHILDREN:"
            + "\nX-SPOUSE:"
            + "\nUID:"
            + "\nTZ:"
            + "\nREV:"
            + "\nGEO:"
            + "\nTEL;VOICE;HOME:"
            + "\nTEL;VOICE;WORK:"
            + "\nTEL;VOICE:"
            + "\nTEL;FAX;HOME:"
            + "\nTEL;FAX;WORK:"
            + "\nTEL;PAGER:"
            + "\nTEL;WORK;PREF:"
            + "\nTEL;FAX:"
            + "\nTEL;PREF;VOICE:"
            + "\nEMAIL;INTERNET:"
            + "\nEMAIL;INTERNET;HOME:"
            + "\nEMAIL;INTERNET;WORK:"
            + "\nEMAIL;INTERNET;HOME;X-FUNAMBOL-INSTANTMESSENGER:"
            + "\nADR:;;;;;;"
            + "\nADR;HOME:;;;;;;"
            + "\nADR;WORK:;;;;;;"
            + "\nURL:"
            + "\nURL;HOME:"
            + "\nURL;WORK:"
            + "\nPHOTO:"
            + "\nEND:VCARD";
    }

}
