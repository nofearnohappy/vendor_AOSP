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

package com.mediatek.rcs.incallui.ext;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.vcard.VCardEntry;
import com.mediatek.common.PluginImpl;
import com.mediatek.contacts.ext.DefaultRcsRichUiExtension;
import com.mediatek.rcs.incallui.RichScrnObjDownloader;

import org.gsma.joyn.JoynServiceConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin implement for Contacts.
 */
@PluginImpl(interfaceName = "com.mediatek.contacts.ext.IRcsRichUiExtension")
public class RCSRichUiUpdateExtension extends DefaultRcsRichUiExtension {

    private static final String TAG = "RCSRichUiContactUpdateExtension";

    private Context mContext;

    /**
     * constructed function.
     * @param context  Context.
     */
    public RCSRichUiUpdateExtension(Context context) {
        mContext = context;
    }

    /**
    * OP01 RCS load rich call screen from server when import vcard.
    * @param isFirst  If is first vcard number.
    * @param entry  VCard entry.
    * @param context  Context.
    */
    public void loadRichScrnByVcardEntry(boolean isFirst, VCardEntry entry, Context context) {
        Log.d(TAG, "loadRichScrnByVcardEntry: isFirst = " + isFirst);
        if (isFirst && JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "RCS ON: loadRichScrnByVcardEntry");
            List<VCardEntry.PhoneData> phoneList = entry.getPhoneList();
            if (phoneList != null) {
                ArrayList<String> numbers = new ArrayList<String>();
                for (VCardEntry.PhoneData phone : phoneList) {
                   numbers.add(phone.getNumber());
                }
                if (numbers.size() > 0) {
                   RichScrnObjDownloader.getInstance()
                           .loadRichScrnByNumbers(isFirst, numbers, context);
                }
            }
        }
    }

    /**
    * Check if it is insert action or edit action.
    * @param context  Context
    * @return true or false
    */
    private boolean isInsert(Context context) {
        Activity activity = (Activity) context;
        Intent intent = activity.getIntent();
        boolean result = false;
        if (intent != null) {
            result = Intent.ACTION_INSERT.equals(intent.getAction());
        }
        return result;
    }

    /**
    * OP01 RCS load rich call screen from server when new/edit contact.
    * @param lookupUri  lookup uri.
    * @param context  Context.
    */
    public void loadRichScrnByContactUri(Uri lookupUri, Context context) {
        Log.d(TAG, "loadRichScrnByContactUri: " + lookupUri);
        if (isInsert(context) && JoynServiceConfiguration.isServiceActivated(mContext)) {
            Log.i(TAG, "RCS ON: loadRichScrnByContactUri");
            long id = ContentUris.parseId(lookupUri);
            RichScrnObjDownloader.getInstance()
                    .loadRichScrnByContactId(id, lookupUri, context);
        }
    }

}

