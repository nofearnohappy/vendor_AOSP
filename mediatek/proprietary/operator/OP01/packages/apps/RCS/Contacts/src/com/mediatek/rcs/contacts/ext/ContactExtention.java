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

package com.mediatek.rcs.contacts.ext;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.mediatek.rcs.contacts.PluginApiManager;
import com.mediatek.rcs.contacts.R;

import java.util.HashMap;
import java.util.List;

/**
 * This class defined to implement the function interface of IContactExtention,
 * and achieve the main function here
 */
public class ContactExtention extends ContextWrapper {

    private static final String TAG = "ContactExtention";
    private static final int RCS_PRESENCE = 1;
    private final HashMap<OnPresenceChangedListener, Long> mOnPresenceChangedListenerList =
            new HashMap<OnPresenceChangedListener, Long>();
    private PluginApiManager mInstance = null;
    private Context mContext = null;

    public ContactExtention(Context context) {
        super(context);
        mContext = context;
        Log.d(TAG, "ContactExtention entry");
        PluginApiManager.initialize(context);
        mInstance = PluginApiManager.getInstance();
        Log.d(TAG, "ContactExtention exit");
    }

    public Context getContext() {
        return mContext;
    }

    public Drawable getAppIcon(boolean isLight) {
        Resources resources = getResources();
        Drawable drawable = null;
        if (resources != null) {
            if (isLight) {
                drawable = resources.getDrawable(R.drawable.ic_rcs_list_light);
            } else {
                drawable = resources.getDrawable(R.drawable.ic_rcs_list);
            }
        } else {
            Log.d(TAG, "getAppIcon() resources is null");
        }
        return drawable;
    }

    public Drawable getAppIcon(final long contactId, boolean isLight) {
        Resources resources = getResources();
        Drawable drawable = null;
        if (resources != null) {
            if (isReadBurnSupported(contactId)) {
                if (isLight) {
                    drawable = resources.getDrawable(R.drawable.ic_rcs_detail_readburn_light);
                } else {
                    drawable = resources.getDrawable(R.drawable.ic_rcs_list_readburn);
                }
            } else {
                if (isLight) {
                    drawable = resources.getDrawable(R.drawable.ic_rcs_list_light);
                } else {
                    drawable = resources.getDrawable(R.drawable.ic_rcs_list);
                }
            }
        } else {
            Log.d(TAG, "getAppIcon() resources is null");
        }
        return drawable;
    }

    public List<String> getNumbersByContactId(long contactId) {
        return mInstance.getNumbersByContactId(contactId);
    }

    public boolean isEnabled() {
        boolean isEnable = mInstance.getRegistrationStatus();
        Log.d(TAG, "isEnabled() return: " + isEnable);
        return isEnable;
    }

    public boolean getContactPresencebyId(long contactId) {
        int presence = mInstance.getContactPresencebyId(contactId);
        if (presence == RCS_PRESENCE) {
            return true;
        }
        return false;
    }

    public boolean getContactPresencebyNumber(String number) {
        int presence = mInstance.getContactPresencebyNumber(number);
        if (presence == RCS_PRESENCE) {
            return true;
        }
        return false;
    }

    public boolean isReadBurnSupported(final long contactId) {
        if(mInstance != null) {
            return mInstance.isReadBurnSupported(contactId);
        } else {
            return false;
        }
    }

    public void addOnPresenceChangedListener(OnPresenceChangedListener listener, long contactId) {
        mInstance.addOnPresenceChangedListener(listener, contactId);
    }

    public void onContactDetailOpen(final Uri contactLookUpUri) {
        if (null != contactLookUpUri) {
            final ContentResolver contentResolver = getContentResolver();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Cursor cursor = null;
                    long contactId = -1;
                    try {
                        Log.d(TAG, "onContactDetailOpen() contactLookUpUri: " + contactLookUpUri);
                        cursor = contentResolver.query(contactLookUpUri, new String[] {
                            Contacts._ID}, null, null, null);
                        if (null == cursor || !cursor.moveToFirst()) {
                            Log.e(TAG, "onContactDetailOpen() error when loading cursor");
                            return;
                        }
                        int indexContactId = cursor.getColumnIndex(Contacts._ID);
                        do {
                            contactId = cursor.getLong(indexContactId);
                        } while (cursor.moveToNext());
                    } finally {
                        if (null != cursor) {
                            cursor.close();
                        }
                    }
                    if (-1 != contactId) {
                        Log.d(TAG, "onContactDetailOpen() contactId: " + contactId);
                        mInstance.queryNumbersPresence(mInstance.getNumbersByContactId(contactId));
                    } else {
                        Log.w(TAG, "onContactDetailOpen() contactLookUpUri " + contactLookUpUri);
                    }
                }
            });
        } else {
            Log.w(TAG, "onContactDetailOpen() contactLookUpUri is null");
        }
    }

    public void updateNumbersByContactId(long contactId) {
        Log.d(TAG, "updateNumbersByContactId() contactId: " + contactId);
        mInstance.getNumbersByContactId(contactId);
    }

    /**
     * Interface for plugin to call back host that presence has changed.
     */
    public interface OnPresenceChangedListener {
        /**
         * Call back when presence changed.
         *
         * @param contactId The contact id.
         * @param presence The presence.
         */
        void onPresenceChanged(long contactId, int presence);
    }

}
