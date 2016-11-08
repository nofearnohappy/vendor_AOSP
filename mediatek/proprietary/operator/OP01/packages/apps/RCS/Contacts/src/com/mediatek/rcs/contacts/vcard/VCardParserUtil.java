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

package com.mediatek.rcs.contacts.vcard;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;

import com.mediatek.rcs.contacts.vcard.VCardParserResult;
import com.mediatek.rcs.contacts.vcard.VCardData;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardVersionException;
import com.android.vcard.exception.VCardNotSupportedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

public class VCardParserUtil {

    private static final String TAG = VCardParserUtil.class.getSimpleName();

    public static final String ACCOUNT_NAME = "Phone";
    public static final String ACCOUNT_TYPE = "Local Phone Account";

    public static final int VCARD_NO_ERROR = 0;
    public static final int VCARD_IO_ERROR = -1;
    public static final int VCARD_VERSION_ERROR = -2;
    public static final int VCARD_PARSE_ERROR = -3;
    public static final int VCARD_COUNT_ERROR = -4;
    public static final int VCARD_NO_SUPPORT_ERROR = -5;
    public static final int VCARD_FILE_FORMAT_ERROR = -6;
    public static final int VCARD_NO_FILE_ERROR = -7;

    public static final int SAVE_CONATCT_SUCCESS = 1;
    public static final int SAVE_CONATCT_FAIL = -8;

    final static int VCARD_VERSION_V21 = 1;
    final static int VCARD_VERSION_V30 = 2;

    final static int LABEL_PHONE = 0;
    final static int LABEL_EMAIL = 1;

    public static int ParseVCard(final Uri localDataUri, VCardEntryHandler handler, 
            ContentResolver resolver) {
        VCardEntryCounter counter = null;
        VCardSourceDetector detector = null;
        VCardParser parser;
        int vcardVersion = VCARD_VERSION_V21;
        InputStream is;
        int vcardType = 0;
        int vcardCount = 0;
         
         try {           
            is = resolver.openInputStream(localDataUri);   
            parser = new VCardParser_V21();
            try {
                counter = new VCardEntryCounter();
                detector = new VCardSourceDetector();
                parser.addInterpreter(counter);
                parser.addInterpreter(detector);
                parser.parse(is);
            } catch (VCardVersionException e1) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "detect close IOException", e);
                }
                is = resolver.openInputStream(localDataUri);
                
                vcardVersion = VCARD_VERSION_V30;
                parser = new VCardParser_V30();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    parser.addInterpreter(counter);
                    parser.addInterpreter(detector);
                    parser.parse(is);
                } catch (VCardVersionException e2) {
                    Log.e(TAG, "vcard version Exception", e2);
                    return VCARD_VERSION_ERROR;
                }
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    Log.e(TAG, "detect close IOException", e);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "detect wrong Format", e);
            return VCARD_FILE_FORMAT_ERROR;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "detect no File", e);
            return VCARD_NO_FILE_ERROR;
        } catch (IOException e) {
            Log.e(TAG, "detect IOException", e);
            return VCARD_IO_ERROR;
        } catch (VCardException e) {
            Log.e(TAG, "detect vcard Exception", e);
            return VCARD_PARSE_ERROR;
        }
      
        vcardCount = counter.getCount();
        if (vcardCount != 1) {
            Log.d(TAG, "Error count: " + vcardCount);
            return VCARD_COUNT_ERROR;
        }
        
        vcardType = detector.getEstimatedType();
        Log.d(TAG, "vcard type: " + vcardType);
        final VCardEntryConstructor constructor = new VCardEntryConstructor(vcardType);
        constructor.addEntryHandler(handler);

        try {
            is = resolver.openInputStream(localDataUri);
            Log.d(TAG, "vcard version: " + vcardVersion);
            parser = (vcardVersion == VCARD_VERSION_V30 ? new VCardParser_V30(vcardType) :
                    new VCardParser_V21(vcardType));
            parser.parse(is, constructor);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return VCARD_IO_ERROR;
        } catch (VCardNotSupportedException e) {
            Log.e(TAG, "VCardNotSupportedException", e);
            return VCARD_NO_SUPPORT_ERROR;
        } catch (VCardVersionException e) {
            Log.e(TAG, "VCardVersionException", e);
            return VCARD_VERSION_ERROR;
        } catch (VCardException e) {
            Log.e(TAG, "VCardException", e);
            return VCARD_PARSE_ERROR;
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                Log.e(TAG, "detect close IOException", e);
            }
        }
         
        return VCARD_NO_ERROR;
    }

    public static String getLabelType(int element, int type, Context context) {
        String result = null;
        int res = 0;

        if (type != 0) {
            switch (element) {
                case LABEL_PHONE:
                    res = Phone.getTypeLabelResource(type);
                    break;

                case LABEL_EMAIL:            
                    res = Email.getTypeLabelResource(type);
                    break;
                    
                default:
                    Log.e(TAG, "getEmailType error: " + element);
                    break;
            }
            result = context.getString(res);
        }
        Log.d(TAG, "getType: " + result);
        return result;
    }

    public static VCardParserResult ParseVCardEntry(VCardEntry entry, Context context) {
        String name = null;
        String organization = null;    
        String title = null;;
        List<VCardData> number = null;
        List<VCardData> email = null;
        byte[] photo = null;
        String photoFormat = null;
        int size = 0;
        int i = 0;
        String type = null;

        name = entry.getDisplayName();
        Log.d(TAG, "ParseVCardEntry name: " + name);

        if (entry.getOrganizationList() != null) {
            organization = entry.getOrganizationList().get(0).getOrganizationName();
            Log.d(TAG, "ParseVCardEntry organization: " + organization);

            title = entry.getOrganizationList().get(0).getTitle();
            Log.d(TAG, "ParseVCardEntry title: " + title);
        }

        if (entry.getEmailList() != null) {
            email = new ArrayList<VCardData>();
            size = entry.getEmailList().size();
            Log.d(TAG, "ParseVCardEntry email size: " + size);
            for (i = 0; i < size; i++) {
                type =  getLabelType(LABEL_EMAIL, entry.getEmailList().get(i).getType(), context);
                if (type == null) {
                    type = entry.getEmailList().get(i).getLabel();    
                }
                email.add(new VCardData(entry.getEmailList().get(i).getAddress(), type));
                Log.d(TAG, "ParseVCardEntry email: " + email.get(email.size() - 1).toString());
            }          
        }

        if (entry.getPhotoList() != null) {
            photo = entry.getPhotoList().get(0).getBytes();
            photoFormat = entry.getPhotoList().get(0).getFormat();
            Log.d(TAG, "ParseVCardEntry photo: " + photoFormat);
        }

        if (entry.getPhoneList() != null) {
            number = new ArrayList<VCardData>();
            size = entry.getPhoneList().size();
            Log.d(TAG, "ParseVCardEntry number size: " + size);
            for (i = 0; i < size; i++) {
                type =  getLabelType(LABEL_PHONE, entry.getPhoneList().get(i).getType(), context);
                if (type == null) {
                    type = entry.getPhoneList().get(i).getLabel();    
                }
                number.add(new VCardData(entry.getPhoneList().get(i).getNumber(), type));
                Log.d(TAG, "ParseVCardEntry number: " + number.get(number.size() - 1).toString());
            }
        }

        return new VCardParserResult(name, organization, title, number, email, photo, photoFormat);
    }

}
