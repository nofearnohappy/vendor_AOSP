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

import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.widget.EditText;

/**
 * ProfileEditorUtils: Profile photo utils.
 */
public class ProfileEditorUtils {

    public static final String TAG = "ProfileEditorUtils";
    
    private static final int LENGTH_COMMON_LIMIT = 100;
    private static final int LENGTH_NAME_LIMIT = 10;
    private static final int LENGTH_EMAIL_LIMIT = 40;
    private static final int LENGTH_NUMBER_LIMIT = 15;
    private static final int LENGTH_TITLE_LIMIT = 40;

    public static void addNumberEditorLimit(EditText editor, String type) {
        int length;
        if (editor == null || type == null) {
            Log.d(TAG, "Invalid type or editor! return!");
            return;
        }
        Log.d(TAG, "Type = " + type);
        if (isNumberType(type)) {
            editor.setInputType(InputType.TYPE_CLASS_PHONE);
            editor.setKeyListener(PhoneNumberKeyListener.getInstance(type));
            length = LENGTH_NUMBER_LIMIT;
        } else if (type.equals(ProfileInfo.EMAIL)) {
            editor.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            length = LENGTH_EMAIL_LIMIT;
        } else {
            length = getLengthLimitByType(type);
        }
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(length)});
    }

    private static boolean isNumberType(String type) {
        
        if (type.equals(ProfileInfo.PHONE_NUMBER) 
                || type.equals(ProfileInfo.COMPANY_TEL) 
                || type.equals(ProfileInfo.COMPANY_FAX)
                || type.equals(ProfileInfo.PHONE_NUMBER_SECOND)) {
                return true;
        } else {
            return false;
        }
    }

    /**
       * Get type editor limitation length
       * @param type : editor type.
       * @return int : limitation.
       */
    private static int getLengthLimitByType(String type) {
        
        if (type.equals(ProfileInfo.NAME)) {
            return LENGTH_NAME_LIMIT;
        } else if (type.equals(ProfileInfo.TITLE)) {
            return LENGTH_TITLE_LIMIT;
        } else {
            return LENGTH_COMMON_LIMIT;
        }
    }
    
    public static class PhoneNumberKeyListener extends DialerKeyListener {
        private static PhoneNumberKeyListener sKeyListener;
        private static String sType;
        /**
         * The characters that are used.
         * 
         * @see KeyEvent#getMatch
         * @see #getAcceptedChars
         */
        public static final char[] CHARACTERS = new char[] { '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '+', '*', '#','P','W','p','w',',',';'};

        public static final char[] OTHER_NUMBER_CHARACTERS = new char[] { '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '+', '*', '#', 'P', 'W', 'p', 'w', ','};

        @Override
        protected char[] getAcceptedChars() {
            if (sType.equals(ProfileInfo.PHONE_NUMBER_SECOND)) {
                return OTHER_NUMBER_CHARACTERS;
            } else {
                return CHARACTERS;
            }
        }

    /**
       * Get key listener instance.
       * @param type : editor type.
       * @return PhoneNumberKeyListener : Key listener instance.
       */
        public static PhoneNumberKeyListener getInstance(String type) {
            sType = type;
            if (sKeyListener == null) {
                sKeyListener = new PhoneNumberKeyListener();
            }
            return sKeyListener;
        }

    }

}
