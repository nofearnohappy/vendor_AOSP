/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2012. All rights reserved.
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

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.rcs.pam.util;

import android.database.Cursor;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.text.format.DateFormat;
import android.util.Log;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;

import com.android.vcard.VCardComposer;
import com.android.vcard.exception.VCardException;
//import com.android.mms.R;
//import com.android.mms.MmsConfig;
//import com.mediatek.encapsulation.MmsLog;
//import com.mediatek.ipmsg.util.IpMessageUtils;

//import com.mediatek.rcs.message.R;


import java.io.File;
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;



/** M: Support to add text vcard or vcard attachment to compose activity.*/
public class RCSVCardAttachment {
    private Context mContext;
    private String mVCardFileName = "";
    private String mName = "";
    private List<String> mNumbers = new ArrayList<String>();
    private List<String> mEmails = new ArrayList<String>();
    private List<String> mOrganizations = new ArrayList<String>();
    private static final String TAG = "RCSVCardAttachment";

    public RCSVCardAttachment(Context context) {
        this.mContext = context;
    }

    public void reset() {
        mName = "";
        mNumbers.clear();
        mEmails.clear();
        mOrganizations.clear();
    }

    public String getVCardFileNameByContactsId(
                long[] contactsIds, boolean isAddingIpMessageVCardFile) {
        Log.d(TAG, "getVCardFileNameByContactsId(): isAddingIpMessageVCardFile = "
                    + isAddingIpMessageVCardFile);
        // make contacts' id string
        StringBuilder contactsIdsStr = new StringBuilder("");
        for (int i = 0; i < contactsIds.length - 1; i++) {
            contactsIdsStr.append(contactsIds[i]);
            contactsIdsStr.append(',');
        }
        contactsIdsStr.append(contactsIds[contactsIds.length - 1]);
        final String ids = contactsIdsStr.toString();
        String fileName = "";
        if (!ids.equals("")) {
            if (isAddingIpMessageVCardFile) {
                mVCardFileName = getVCardFileNameForIpMessage(contactsIds);
            }
            fileName = attachVCard(ids, isAddingIpMessageVCardFile);
        }
        return fileName;
    }

    /**
     * M: turn contacts id into *.vcf file attachment
     *
     * @param mContactIds
     */
    public String attachVCard(String mContactIds, boolean isAddingIpMessageVCardFile) {
        if (mContactIds == null || mContactIds.equals("")) {
            return new String();
        }
        String fileName = isAddingIpMessageVCardFile ? mVCardFileName : createVCardFileName();
        String tempPath = Utils.getCachePath(mContext) + fileName;
        String path = null;
        VCardComposer composer = null;
        Writer writer = null;
        try {
            //VCardComposer composer = null;
            //Writer writer = null;
            //try {
                if (isAddingIpMessageVCardFile) {
                    File file = new File(tempPath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
                        //Utils.getCachePath(mContext) + fileName)));
                } else {
                    writer = new BufferedWriter(new OutputStreamWriter(
                        mContext.openFileOutput(fileName, Context.MODE_PRIVATE)));
                }
                composer = new VCardComposer(mContext);
                if (!composer.init(Contacts._ID + " IN (" + mContactIds + ")", null)) {
                    // fall through to catch clause
                    throw new VCardException("Canot initialize " + composer.getErrorReason());
                }
                while (!composer.isAfterLast()) {
                    writer.write(composer.createOneEntry());
                }
            //} finally {
            //    if (composer != null) {
            //        composer.terminate();
            //    }
            //    if (writer != null) {
            //        writer.close();
            //    }
            //}
        } catch (VCardException e) {
            Log.e(TAG, "export vcard file, vcard exception " + e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e(TAG, "export vcard file, file not found exception " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "export vcard file, IO exception " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "export vcard file, exception " + e.getMessage());
        } finally {
            if (composer != null) {
                composer.terminate();
            }
            if (writer != null) {
                try {
                    writer.close();
                    path = tempPath;
                } catch (IOException e) {
                    Log.e(TAG, "close vcard file, IO exception " + e.getMessage());
                }
            }
        }

        Log.d(TAG, "write vCard file done!");
        return path;
        // setFileAttachment(fileName, WorkingMessage.VCARD, false);
    }

    private String createVCardFileName() {
        final String fileExtension = ".vcf";
        // base on time stamp
        String name = DateFormat.format("yyyyMMdd_hhmmss", new Date(System.currentTimeMillis()))
                .toString();
        name = name.trim();
        return name + fileExtension;
    }

    // the uri must be a vcard uri created by Contacts
    public String getVCardFileNameByUri(Uri uri) {
        if (uri == null) {
            return new String();
        }
        final String filename = createVCardFileName();
        try {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = mContext.getContentResolver().openInputStream(uri);
                out = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
                byte[] buf = new byte[8096];
                int size = 0;
                while ((size = in.read(buf)) != -1) {
                    out.write(buf, 0, size);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception getVCardFileNameByUri ", e);
        }
        return filename;
    }

    private String getVCardFileNameForIpMessage(long[] contactIds) {
        Log.d(TAG, "getVCardFileNameForIpMessage()");
        String fileName = "";
        if (contactIds.length == 1) {
            long contactId = contactIds[0];
            String selection = Data.CONTACT_ID + " = " + contactId;

            Cursor cursor = mContext.getContentResolver().query(
                Data.CONTENT_URI, //dataUri, // URI
                new String[]{/*Data.CONTACT_ID, */Data.MIMETYPE, Data.DATA1}, // projection
                selection, // selection
                null, // selection args
                RawContacts.SORT_KEY_PRIMARY); // sortOrder
            if (null != cursor) {
                String mimeType;
                while (cursor.moveToNext()) {
                    if (CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.
                            equals(cursor.getString(0))) {
                        fileName = cursor.getString(1);
                        break;
                    }
                }
                cursor.close();
            }
        } else {
            //fileName = IpMessageUtils.getResourceManager(mContext)
            //    .getSingleString(IpMessageConsts.string.ipmsg_vcard_file_name, contactIds.length);

            fileName = "multi-contacts";//mContext.getString(R.string.multi_cantacts_name);
        }
        return fileName + ".vcf";
    }
}
