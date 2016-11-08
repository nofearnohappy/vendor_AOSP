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

package com.mediatek.rcs.contacts;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.widget.Toast;

import org.gsma.joyn.JoynServiceConfiguration;

/**
 * Test activity.
 */
public class TestActivity extends PreferenceActivity {

    private static final String TAG = "TestActivity";
    private static final String KEY_ITEM1 = "item1";
    private static final String KEY_ITEM2 = "item2";
    private static final String KEY_ITEM3 = "item3";
    private static final String KEY_ITEM4 = "item4";
    private static final String KEY_ITEM5 = "item5";
    private static final String KEY_ITEM6 = "item6";
    private static final String KEY_ITEM7 = "item7";
    private static final String KEY_ITEM8 = "item8";

    private static final int REQUEST_CODE_GROUP_VCARD = 111;
    private static final int REQUEST_CODE_NUMBER_PICK = 222;

    private Preference mItem1;
    private Preference mItem2;
    private Preference mItem3;
    private Preference mItem4;
    private Preference mItem5;
    private Preference mItem6;
    private Preference mItem7;
    private Preference mItem8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefer);
        mItem1 = findPreference(KEY_ITEM1);
        mItem2 = findPreference(KEY_ITEM2);
        mItem3 = findPreference(KEY_ITEM3);
        mItem4 = findPreference(KEY_ITEM4);
        mItem5 = findPreference(KEY_ITEM5);
        mItem6 = findPreference(KEY_ITEM6);
        mItem7 = findPreference(KEY_ITEM7);
        mItem8 = findPreference(KEY_ITEM8);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference == mItem1) {
            Log.i(TAG, "launch GroupListActivity");
            Intent intent = new Intent("android.intent.action.rcs.contacts.GroupListActivity");
            startActivityForResult(intent, REQUEST_CODE_GROUP_VCARD);
        } else if (preference == mItem2) {
            Log.i(TAG, "launch GroupChatActivity");
            Intent intent = new Intent("android.intent.action.rcs.contacts.GroupChatActivity");
            startActivity(intent);
        } else if (preference == mItem3) {
            Log.i(TAG, "launch mItem3");
            try {
                Intent intent = new Intent("android.intent.action.contacts.list.PICKMULTIPHONES");
                intent.setType(Phone.CONTENT_TYPE);
                String[] numbers = new String[7];
                numbers[0] = "123";
                numbers[1] = "+123556";
                numbers[2] = "123,;";
                numbers[3] = "pw*#";
                numbers[4] = "15010001000";
                numbers[5] = "150 1000 1000";
                //numbers[6] = "+86 150 1000 1000";
                intent.putExtra("ExistNumberArray", numbers);
                intent.putExtra("Group", true);
                intent.putExtra("NUMBER_BALANCE", 3);
                startActivityForResult(intent, REQUEST_CODE_NUMBER_PICK);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            }
        } else if (preference == mItem4) {
            Log.i(TAG, "launch mItem4");
            Intent intent = new Intent("android.intent.action.rcs.contacts.GroupMemberActivity");
            startActivityForResult(intent, REQUEST_CODE_NUMBER_PICK);
        } else if (preference == mItem5) {
            Log.i(TAG, "launch mItem5");
            final Uri uri = Uri.parse("file:///storage/sdcard0/00001.vcf");
            Intent intent = new Intent("android.intent.action.rcs.contacts.VCardViewActivity");
            intent.setDataAndType(uri, "text/x-vCard".toLowerCase());
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else if (preference == mItem6) {
            Log.i(TAG, "launch mItem6");
            Intent intent = new Intent("android.intent.action.rcs.contacts.qrcode.SCAN");
            startActivity(intent);
        } else if (preference == mItem7) {
            Log.i(TAG, "launch mItem7");
            JoynServiceConfiguration config = new JoynServiceConfiguration();
            String myNumber = config.getPublicUri(this);
            Log.i(TAG, "myNumber: " + myNumber);
            Toast.makeText(this, myNumber, Toast.LENGTH_SHORT).show();
        } else if (preference == mItem8) {
            Log.i(TAG, "launch mItem8");
            try {
                Intent intent = new Intent(
                        this, cmcc.barcode.lib.iot.barcode.decode.CaptureActivity.class);
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_GROUP_VCARD:
                long[] contactIds = data.getLongArrayExtra(
                        "com.mediatek.contacts.list.pickcontactsresult");
                if (contactIds != null) {
                    for (int i = 0; i < contactIds.length; i++) {
                        Log.i(TAG, "onActivityResult contact id: " + contactIds[i]);
                    }
                }
                break;

            case REQUEST_CODE_NUMBER_PICK:
                long[] dataIds = data.getLongArrayExtra(
                        "com.mediatek.contacts.list.pickdataresult");
                if (dataIds != null) {
                    for (int i = 0; i < dataIds.length; i++) {
                        Log.i(TAG, "onActivityResult data id: " + dataIds[i]);
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}

