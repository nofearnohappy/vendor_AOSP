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

package com.mediatek.rcs.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract.Groups;

public final class GroupListLoader extends CursorLoader {

    private final static String[] GROUP_LIST_PROJECT = new String[] {
        Groups.ACCOUNT_NAME,
        Groups.ACCOUNT_TYPE,        
        Groups._ID,
        Groups.TITLE,
        Groups.DATA_SET,
        Groups.SUMMARY_COUNT,
    };

    public final static int GROUP_LIST_ACCOUNT_NAME = 0;
    public final static int GROUP_LIST_ACCOUNT_TYPE = 1;    
    public final static int GROUP_LIST_ID = 2;
    public final static int GROUP_LIST_TITLE = 3;
    public final static int GROUP_LIST_DATA_SET = 4;
    public final static int GROUP_LIST_MEMBER_COUNT = 5;

    private static final Uri GROUP_LIST_URI = Groups.CONTENT_SUMMARY_URI;

    public GroupListLoader(Context context) {
        super(context);
        setUri(GROUP_LIST_URI);
        setProjection(GROUP_LIST_PROJECT);
        setSelection(Groups.ACCOUNT_TYPE + " NOT NULL AND " + 
                Groups.ACCOUNT_NAME + " NOT NULL AND " + Groups.AUTO_ADD + "=0 AND " +
                Groups.FAVORITES + "=0 AND " + Groups.DELETED + "=0");
        setSelectionArgs(null);
        setSortOrder(Groups.ACCOUNT_TYPE + ", " + Groups.ACCOUNT_NAME + ", " +
                Groups.DATA_SET + ", " + Groups.TITLE + " COLLATE LOCALIZED ASC");       
    }
}
