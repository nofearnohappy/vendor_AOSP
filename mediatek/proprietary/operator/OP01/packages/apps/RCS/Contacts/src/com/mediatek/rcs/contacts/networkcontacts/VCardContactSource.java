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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fake source for test.
 * @author MTK80963
 *
 */
public class VCardContactSource extends ContactsSource {
    private static final String TAG = "NetworkContacts::VCardContactSource";

    /**
     * @param c context
     */
    public VCardContactSource(Context c) {
        super(c);
    }

    @Override
    public String getMetaType() {
        return "text/x-vcard";
    }

    @Override
    public String getServerUri() {
        return "http://172.27.122.15:8080/funambol/ds";
    }

    @Override
    public String getLocalUri() {
        return "18810718474";
    }

    @Override
    public String getUserName() {
        return "guest";
    }

    @Override
    public String getPassword() {
        return "guest";
    }

    /**
     * @author MTK80963
     *
     */
    public static class VCardObject {
        public String mFN; // family name
        public String mGN; // given name
        public List<String> mTelCell = new ArrayList<String>();
        public List<String> mTelVoice = new ArrayList<String>();

        private String utf8ToString(String s) {
            try {
                String[] chars = s.split("=");
                byte[] bytes = new byte[chars.length + 1];
                int i = 0;
                for (String c : chars) {
                    if (c.length() > 0) {

                        bytes[i++] = (byte) Integer.parseInt(c, 16);
                    }
                }
                bytes[i] = 0;

                return new String(bytes, "UTF-8");
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return null;
        }

        private String toUtf8(String s) {
            if (s == null) {
                return "";
            }

            try {
                byte[] bytes = s.getBytes("UTF-8");
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    if (b != 0) {
                        sb.append(String.format("=%2X", b));
                    }
                }

                return sb.toString();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return "";
        }

        private void abstractName(String line) {
            String[] array = line.split(":");
            String[] names = array[1].split(";");
            if (line.contains("UTF-8")) {
                mFN = utf8ToString(names[0]);
                mGN = utf8ToString(names[1]);
            } else {
                mFN = names[0];
                mGN = names[1];
            }
        }

        private void abstractTel(String line) {
            String[] array = line.split(":");
            if (array.length > 1) {
                if (array[0].equalsIgnoreCase("TEL;CELL")) {
                    mTelCell.add(array[1]);
                } else if (array[0].startsWith("TEL;VOICE")) {
                    mTelVoice.add(array[1]);
                }
            }
        }

        /**
         * @param data vCard data.
         */
        public VCardObject(String data) {
            String[] a = data.split("\n");
            for (String s : a) {
                if (s.startsWith("N:") || s.startsWith("N;")) {
                    abstractName(s);
                } else if (s.startsWith("TEL:") || s.startsWith("TEL;")) {
                    abstractTel(s);
                }
            }
        }

        /**
         * Constructor.
         */
        public VCardObject() {

        }

        private String convertTel(String prefix, List<String> tel) {
            if (tel == null) {
                return "";
            }

            if (tel.size() == 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (String s : tel) {
                sb.append(prefix + s);
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            String start = "BEGIN:VCARD" + "\nVERSION:2.1"
                    + "\nN;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:";
            String name = toUtf8(mFN) + ";" + toUtf8(mGN) + ";;;";
            String mobile = convertTel("\nTEL;CELL:", mTelCell);
            String voice = convertTel("\nTEL;VOICE:", mTelVoice);
            String end = "\nEND:VCARD";

            return start + name + mobile + voice + end;
        }

    }

    @Override
    public SyncItem fromData(String jsonData) {
        if (null == jsonData || jsonData.isEmpty()) {
            Log.e(TAG, "param jsonData of fromJson() is nulll or empty.");
            return null;
        }
        ContactItem cta = new VCardItem();
        VCardObject jContact = new VCardObject(jsonData);
        // if jsonData has no some tag, just leaving corresponding field of
        // ContactItem as null.
        cta.setFamilyName(jContact.mFN);
        cta.setGivenName(jContact.mGN);
        cta.setMobileList(jContact.mTelCell);
        cta.setTelList(jContact.mTelVoice);
        return cta;
    }

    @Override
    protected SyncItem getItemInAll(int index) {
        Log.i(TAG, "+getItem");

        Cursor cursor = getAllItemCursor();
        if (null == cursor) {
            Log.e(TAG, "cursor is null.");
            return null;
        }
        if (cursor.getCount() < 1
                || (index < 0 || index > cursor.getCount() - 1)
                || !cursor.moveToPosition(index)) {
            Log.e(TAG, "something not satisfied.");
            return null;
        }
        String contactId = cursor.getString(cursor
                .getColumnIndexOrThrow(RawContacts._ID));
        ContactItem contact = new VCardItem();
        contact.setId(Integer.valueOf(contactId));
        loadContactData(contact, contactId);

        Log.i(TAG, "-getItem success");
        return contact;

    }

}
