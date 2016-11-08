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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

//import com.mediatek.gba.GbaManager;
//import com.mediatek.gba.NafSessionKey;
import com.mediatek.rcs.contacts.networkcontacts.JsonUtil.JsonTag;

import org.gsma.joyn.JoynServiceConfiguration;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author MTK81359
 *
 */
public class ContactsSource implements SyncSource {
    private static final String TAG = "NetworkContacts::ContactsSource";

    // Contacts.INDICATE_PHONE_SIM
    private static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";
    private static final String PREFNAME = "ContactSourceData";
    private static final String PREF_KEY_LASTANCHOR = "last anchor";
    private String mNextAnchor = null;
    private DevInfo mDevInfo = new DevInfo();
    private Context mContext = null;
    private ContentResolver mResolver = null;
    private Cursor mAllCursor = null; // cursor for all items
    private DataCursor mAllDataCursor = null; // cursor for all items' data

    /**
     * All the results of the sync operations.
     */
    private ArrayList<ContentProviderResult> mResults =
            new ArrayList<ContentProviderResult>();
    /**
     * mProviderOps, mRawCtaInsertOpIndexs and mGrpInsertOpIndexMap
     * construct the context of one bulk operations.
     */
    private ArrayList<ContentProviderOperation> mProviderOps =
            new ArrayList<ContentProviderOperation>();
    /**
     * Index list of abstract operations. The value in the list item: >= 0,
     * index of add operation in contentResolver's batch operation: -1, replace
     * operation -2, delete operation
     *
     * We use it to get the id of new added item from batch operation results.
     */
    private ArrayList<Integer> mRawCtaInsertOpIndexs = new ArrayList<Integer>();
    private HashMap<String, Integer> mGrpInsertOpIndexMap = new HashMap<String, Integer>();
    private static final String ACCOUNT_NAME_PHONE = "Phone";
    private static final String ACCOUNT_TYPE_PHONE = "Local Phone Account";

    /* backup file for old and new added items */
    public static final String BACKUP_FILE_OLD = "old.bak";
    public static final String BACKUP_FILE_NEW = "new.bak";

    /**
     * Constructor.
     *
     * @param c
     *            context.
     */
    public ContactsSource(Context c) {
        mContext = c;
        mResolver = mContext.getContentResolver();
    }

    @Override
    public DevInfo getDevInfo() {
        return mDevInfo;
    }

    @Override
    public String getLastAnchor() {
        Log.i(TAG, "+getLastAnchor...");
        Context ctx = ContactsSyncEngine.getInstance(null).getContext();
        SharedPreferences sourceData = ctx.getSharedPreferences(PREFNAME,
                Context.MODE_PRIVATE);

        return sourceData.getString(PREF_KEY_LASTANCHOR, "0");
    }

    @Override
    public String getNextAnchor() {
        Log.i(TAG, "+getNextAnchor...");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT00:00"));

        mNextAnchor = sdf.format(System.currentTimeMillis());
        return mNextAnchor;
    }

    @Override
    public void updateAnchor() {
        Log.i(TAG, "+updateAnchor...");

        Context ctx = ContactsSyncEngine.getInstance(null).getContext();
        SharedPreferences sourceData = ctx.getSharedPreferences(PREFNAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sourceData.edit();
        editor.putString(PREF_KEY_LASTANCHOR, mNextAnchor);
        editor.commit();
    }

    @Override
    public String getServerUri() {
        JoynServiceConfiguration config = new JoynServiceConfiguration();
        String address = config.getNABAddress(mContext);
        String port = config.getNABAddressPort(mContext);
        String type = config.getNABAddressType(mContext);
        Log.d(TAG, "address: " + address);
        Log.d(TAG, "port: " + port);
        Log.d(TAG, "type: " + type);
        return address + ":" + port;
    }

    @Override
    public String getLocalUri() {
        return "local-contacts";
    }

    @Override
    public String getUserName() {
        return "f54b3171150b464936f92aee6a823698";
    }

    @Override
    public String getPassword() {
        if (Const.TEMP_DEBUG) {
            if (mContext.getFileStreamPath("rddebug").exists()) {
        return "18811047941";
    }
        }
        return "18810718474";
    }

    /*
     * @Override public String getUserName() { GbaManager gbaManager =
     * GbaManager.getDefaultGbaManager(mContext); NafSessionKey nafSessionKey =
     * gbaManager .runGbaAuthentication(getServerUri(),
     * gbaManager.getNafSecureProtocolId(false), false); if (nafSessionKey ==
     * null) { Log.e(TAG, "runGbaAuthentication return null"); return ""; }
     * return nafSessionKey.getBtid(); }
     *
     * @Override public String getPassword() { GbaManager gbaManager =
     * GbaManager.getDefaultGbaManager(mContext); NafSessionKey nafSessionKey =
     * gbaManager .runGbaAuthentication(getServerUri(),
     * gbaManager.getNafSecureProtocolId(false), false);
     *
     * if (nafSessionKey == null) { Log.e(TAG,
     * "runGbaAuthentication return null"); return ""; }
     *
     * return new String(nafSessionKey.getNafId()); }
     */

    @Override
    public String getSourceUri() {
        return "local-contacts";
    }

    @Override
    public String getTargetUri() {
        return "contacts";
    }

    @Override
    public String getMetaType() {
        return "text/json";
    }

    /**
     * For data cursor cache.
     *
     * @author MTK80963
     *
     */
    private class DataCursor {
        private Cursor mCursor;
        private int mRawIdField = -1;

        public DataCursor(String[] projection, String selection,
                String[] selectionArgs) {
            mCursor = mResolver.query(ContactsContract.Data.CONTENT_URI,
                    projection, selection, selectionArgs,
                    ContactsContract.Data.RAW_CONTACT_ID + " ASC");
            mCursor.moveToFirst();
            mRawIdField = mCursor
                    .getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID);
        }

        public void close() {
            /* close all the cached cursors */
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
                mRawIdField = -1;
            }
        }

        /**
         * Move to next data item of the specified raw contact.
         *
         * @param id
         *            raw contact id.
         * @return true or false.
         */
        public boolean nextData(int id) {
            Log.d(TAG, String.format("+nextData(%d)", id));
            if (mCursor == null) {
                Log.e(TAG, "netxtData:: Cursor is null!!");
                return false;
            }

            if (mRawIdField == -1) {
                Log.e(TAG, "netxtData:: mRawIdField is -1!!");
                return false;
            }
            if (!mCursor.moveToNext()) {
                Log.e(TAG, "netxtData:: moveToNext fail!!");
                return false;
            }

            int curid = mCursor.getInt(mRawIdField);

            if (curid != id) {
                Log.e(TAG, "netxtData:: no more data for the contacts!!");
                return false;
            }

            return true;
        }

        /**
         * Move cursor to the first data item of the raw contacts with specified
         * id.
         *
         * @param id
         *            : raw contact id will move to.
         * @return : data cursor.
         */
        public Cursor moveTo(int id) {
            Log.d(TAG, String.format("+moveTo(%d)", id));
            if (mCursor == null) {
                Log.e(TAG, "moveTo:: Cursor is null!!");
                return null;
            }

            if (mRawIdField == -1) {
                Log.e(TAG, "moveTo:: mRawIdField is -1!!");
                return null;
            }

            int curid = mCursor.getInt(mRawIdField);
            Log.d(TAG, String.format("moveTo:: curid=%d", curid));
            if (id > curid) {
                Log.d(TAG, "search forward...");
                do {
                    if (!mCursor.moveToNext()) {
                        Log.e(TAG, String.format("no item with id %d", id));
                        return null;
                    }

                    curid = mCursor.getInt(mRawIdField);
                    Log.d(TAG, String.format("moveTo:: curid=%d", curid));
                } while (id > curid);
            } else {
                Log.d(TAG, "search backward...");
                boolean found = false;
                do {
                    if (curid == id) {
                        found = true;
                    }
                    if (!mCursor.moveToPrevious()) {
                        if (!found) {
                            Log.e(TAG, String.format("not item with id %d", id));
                            return null;
                        }
                        Log.d(TAG, String.format("moveTo::%d is the first", id));
                        mCursor.moveToFirst();
                        return mCursor;
                    }
                    curid = mCursor.getInt(mRawIdField);
                    Log.d(TAG, String.format("moveTo:: curid=%d", curid));
                } while (curid >= id);

                if (!mCursor.moveToNext()) {
                    Log.e(TAG, "Cannot be here!!!");
                }
            }
            return mCursor;
        }
    }

    @Override
    public int getAllItemCount() {
        Log.d(TAG, "+getAllItemCount...");
        Cursor cursor = getAllItemCursor();
        if (null == cursor) {
            Log.e(TAG, "getAllItemCount() cursor is null.");
            return 0;
        }
        int count = cursor.getCount();
        Log.d(TAG, String.format("-getAllItemCount:%d", count));
        return count;
    }

    /**
     * get all item count but not cache the cursor.
     * @return Count of all the sync items on client.
     */
    public int getAllItemCountWithNoCache() {
        Log.d(TAG, "+getAllItemCountWithNoCache...");
        createAllCursor();

        if (null == mAllCursor) {
            Log.e(TAG, "getAllItemCountWithNoCache() cursor is null.");
            return 0;
        }
        int count = mAllCursor.getCount();
        Log.d(TAG, String.format("-getAllItemCountWithNoCache:%d", count));
        mAllCursor.close();
        mAllCursor = null;
        return count;
    }

    /**
     * Don't close the cursor get by this function for the cursor will be cached
     * and closed after the sync session end.
     *
     * @return cursor
     */
    protected Cursor getAllItemCursor() {
        Log.d(TAG, "+getAllItemCursor...");
        if (mAllCursor != null) {
            return mAllCursor;
        }
        createCursorCache();
        return mAllCursor;
    }

    private void createAllCursor() {
        String[] projection = {RawContacts._ID};
        String selection = getPhoneContactSelection();
        mAllCursor = mResolver.query(RawContacts.CONTENT_URI, projection,
                selection, null, null);
    }

    private void createCursorCache() {
        Log.d(TAG, "+createCursorCache...");
        if (mAllCursor != null) {
            Log.d(TAG, "cursor has been created!");
            return;
        }
        createAllCursor();
        mAllDataCursor = new DataCursor(null, null, null);
    }

    public String getPhoneContactSelection() {
        String selection = RawContacts.DELETED + "=0" + " AND "
                + INDICATE_PHONE_SIM + "=-1" + " AND "
                + RawContacts.ACCOUNT_TYPE + "=\'"
                + ACCOUNT_TYPE_PHONE + "\'" + " AND "
                + RawContacts.ACCOUNT_NAME + "=\'"
                + ACCOUNT_NAME_PHONE
                + "\'";
        return selection;
    }

    @Override
    public SyncItem getItem(int index, int type) {
        // now only implement the type of ITEM_TYPE_ALL
        Log.i(TAG, String.format("+getItem : %d", index));
        if (type != ITEM_TYPE_ALL) {
            Log.e(TAG, "unsupport yet!");
            return null;
        }
        return getItemInAll(index);
    }

    protected SyncItem getItemInAll(int index) {
        Log.d(TAG, String.format("+getItemInAll : %d", index));
        Cursor cursor = getAllItemCursor();
        if (null == cursor) {
            Log.e(TAG, "cursor is null.");
            return null;
        }
        Log.d(TAG, "getItemInAll : getCount");
        int count = cursor.getCount();
        if (count < 1 || index > count - 1) {
            Log.e(TAG, "index is invalid.");
            return null;
        }
        Log.d(TAG, "getItemInAll : moveToPosition");
        if (!cursor.moveToPosition(index)) {
            Log.e(TAG, "move to position failed.");
            return null;
        }
        Log.d(TAG, "getItemInAll : get id");
        String contactId = cursor.getString(cursor
                .getColumnIndexOrThrow(RawContacts._ID));
        Log.d(TAG, "getItemInAll : load data");
        ContactItem contact = new ContactItem();
        contact.setId(Integer.valueOf(contactId));
        loadContactData(contact, contactId);
        Log.d(TAG, "getItemInAll : close cursor");
        Log.d(TAG, "-getItemInAll");
        return contact;
    }

    /**
     * Load contact info by raw contact id.
     *
     * @param contact
     *            The instance of ContactItem to load.
     * @param contactId
     *            The raw contact id.
     */
    protected void loadContactData(ContactItem contact, String contactId) {
        int id = Integer.valueOf(contactId);
        Cursor cursor = mAllDataCursor.moveTo(id);
        if (cursor == null) {
            Log.e(TAG, "load data cursor error!");
            return;
        }

        do {
            String mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE));
            if (mimeType
                    .equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
                loadStructuredNameData(contact, cursor);
            } else if (mimeType
                    .equals(CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)) {
                loadNicknameData(contact, cursor);
            } else if (mimeType.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                loadPhoneData(contact, cursor);
            } else if (mimeType.equals(CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                loadEmailData(contact, cursor);
            } else if (mimeType
                    .equals(CommonDataKinds.Website.CONTENT_ITEM_TYPE)) {
                loadWebsiteData(contact, cursor);
            } else if (mimeType
                    .equals(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)) {
                loadStructuredPostalData(contact, cursor);
            } else if (mimeType
                    .equals(CommonDataKinds.Organization.CONTENT_ITEM_TYPE)) {
                loadOrganizationData(contact, cursor);
            } else if (mimeType.equals(CommonDataKinds.Note.CONTENT_ITEM_TYPE)) {
                loadNoteData(contact, cursor);
            } else if (mimeType.equals(CommonDataKinds.Event.CONTENT_ITEM_TYPE)) {
                loadEventData(contact, cursor);
            } else if (mimeType
                    .equals(CommonDataKinds.Relation.CONTENT_ITEM_TYPE)) {
                loadRelationData(contact, cursor);
            } else if (mimeType.equals(CommonDataKinds.Im.CONTENT_ITEM_TYPE)) {
                loadImData(contact, cursor);
            } else if (mimeType
                    .equals(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)) {
                loadGroupMembershipData(contact, cursor);
            }
        } while (mAllDataCursor.nextData(id));
    }

    private void loadGroupMembershipData(ContactItem cta, Cursor cur) {
        long groupRowId = cur
                .getLong(cur
                        .getColumnIndexOrThrow(CommonDataKinds.GroupMembership.GROUP_ROW_ID));
        String name = queryGroupTitle(groupRowId);
        cta.addGroupName(name);
    }

    private String queryGroupTitle(long rowId) {
        String[] projection = {ContactsContract.Groups.TITLE};
        Uri uri = ContentUris.withAppendedId(
                ContactsContract.Groups.CONTENT_URI, rowId);
        Cursor cursor = mResolver.query(uri, projection, null, null, null);
        if (null == cursor || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }

            return null;
        }
        String title = cursor.getString(cursor
                .getColumnIndexOrThrow(ContactsContract.Groups.TITLE));
        if (cursor != null) {
            cursor.close();
        }
        return title;
    }

    private void loadImData(ContactItem cta, Cursor cur) {
        String data = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Im.DATA));
        // int type =
        // cur.getInt(cur.getColumnIndexOrThrow(CommonDataKinds.Im.TYPE));
        String protocolStr = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Im.PROTOCOL));
        switch (Integer.valueOf(protocolStr).intValue()) {
        case CommonDataKinds.Im.PROTOCOL_QQ:
            cta.addQQ(data);
            break;
        case CommonDataKinds.Im.PROTOCOL_MSN:
            cta.addMsn(data);
            break;
        default:
            break;
        }
    }

    private void loadRelationData(ContactItem cta, Cursor cur) {
        String name = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Relation.NAME));
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.Relation.TYPE));
        switch (type) {
        case CommonDataKinds.Relation.TYPE_CHILD:
            cta.addChild(name);
            break;
        case CommonDataKinds.Relation.TYPE_SPOUSE:
            cta.addSpouse(name);
            break;
        default:
            break;
        }
    }

    private void loadEventData(ContactItem cta, Cursor cur) {
        String date = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Event.START_DATE));
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.Event.TYPE));
        switch (type) {
        case CommonDataKinds.Event.TYPE_ANNIVERSARY:
            cta.addAnniversary(date);
            break;
        case CommonDataKinds.Event.TYPE_BIRTHDAY:
            cta.addBirthday(date);
            break;
        case CommonDataKinds.Event.TYPE_OTHER:

            break;
        default:
            break;
        }
    }

    private void loadNoteData(ContactItem cta, Cursor cur) {
        String note = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Note.NOTE));
        cta.addNote(note);
    }

    private void loadOrganizationData(ContactItem cta, Cursor cur) {
        String company = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Organization.COMPANY));
        String department = cur
                .getString(cur
                        .getColumnIndexOrThrow(CommonDataKinds.Organization.DEPARTMENT));
        String position = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Organization.TITLE));

        Organization org = new Organization(company, department, position);
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.Organization.TYPE));
        Log.i(TAG, "type" + type);
        //Google default design: user add a contact, Organization.TYPE is null in database,
        //Ignore this type when backup or restore.
        cta.addAssembleOrg(org);
    }

    private void loadStructuredPostalData(ContactItem cta, Cursor cur) {
        String street = cur
                .getString(cur
                        .getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.STREET));
        // String pobox =
        // cur.getString(cur.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.POBOX));
        // String extAddress =
        // cur.getString(cur.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.NEIGHBORHOOD));
        String city = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.CITY));
        String region = cur
                .getString(cur
                        .getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.REGION));
        String poCode = cur
                .getString(cur
                        .getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.POSTCODE));
        String country = cur
                .getString(cur
                        .getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.COUNTRY));

        PostalAddress addr = new PostalAddress(country, region, city, street,
                poCode);
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.TYPE));
        switch (type) {
        case CommonDataKinds.StructuredPostal.TYPE_HOME:
            cta.addHomeAssembleAddr(addr);
            break;
        case CommonDataKinds.StructuredPostal.TYPE_WORK:
            cta.addWorkAssembleAddr(addr);
            break;
        case CommonDataKinds.StructuredPostal.TYPE_OTHER:
            cta.addOtherAssembleAddr(addr);
            break;
        default:
            break;
        }
    }

    private void loadWebsiteData(ContactItem cta, Cursor cur) {
        String url = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Website.URL));
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.Website.TYPE));
        switch (type) {
        case CommonDataKinds.Website.TYPE_HOMEPAGE:

            break;
        case CommonDataKinds.Website.TYPE_BLOG:

            break;
        case CommonDataKinds.Website.TYPE_PROFILE:

            break;
        case CommonDataKinds.Website.TYPE_HOME:
            cta.addHomeWebsite(url);
            break;
        case CommonDataKinds.Website.TYPE_WORK:
            cta.addWorkWebsite(url);
            break;
        case CommonDataKinds.Website.TYPE_FTP:

            break;
        case CommonDataKinds.Website.TYPE_OTHER:
            cta.addOtherWebsite(url);
            break;
        default:
            break;
        }
    }

    private void loadEmailData(ContactItem cta, Cursor cur) {
        String emailAddr = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Email.ADDRESS));
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.Email.TYPE));
        switch (type) {
        case CommonDataKinds.Email.TYPE_HOME:
            cta.addHomeMail(emailAddr);
            break;
        case CommonDataKinds.Email.TYPE_WORK:
            cta.addWorkMail(emailAddr);
            break;
        case CommonDataKinds.Email.TYPE_OTHER:
            cta.addOtherMail(emailAddr);
            break;
        case CommonDataKinds.Email.TYPE_MOBILE:

            break;
        default:
            break;
        }
    }

    private void loadPhoneData(ContactItem cta, Cursor cur) {
        String number = cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Phone.NUMBER));
        int type = cur.getInt(cur
                .getColumnIndexOrThrow(CommonDataKinds.Phone.TYPE));
        // ???how to use label
        // String label =
        // cur.getString(cur.getColumnIndexOrThrow(CommonDataKinds.Phone.LABEL));

        switch (type) {
        case CommonDataKinds.Phone.TYPE_HOME:
            cta.addHomeTel(number);
            break;
        case CommonDataKinds.Phone.TYPE_MOBILE:
            cta.addMobile(number);
            break;
        case CommonDataKinds.Phone.TYPE_WORK:
            cta.addWorkTel(number);
            break;
        case CommonDataKinds.Phone.TYPE_FAX_WORK:
            cta.addWorkFax(number);
            break;
        case CommonDataKinds.Phone.TYPE_FAX_HOME:
            cta.addHomeFax(number);
            break;
        case CommonDataKinds.Phone.TYPE_PAGER:
            cta.addPager(number);
            break;
        case CommonDataKinds.Phone.TYPE_OTHER:
            cta.addOtherTel(number);
            break;
        case CommonDataKinds.Phone.TYPE_CALLBACK:
            break;

        case CommonDataKinds.Phone.TYPE_CAR:
            cta.addCarTel(number);
            break;
        case CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
            cta.addCompanyTelExchange(number);
            break;
        case CommonDataKinds.Phone.TYPE_ISDN:
            break;
        case CommonDataKinds.Phone.TYPE_MAIN:

            break;
        case CommonDataKinds.Phone.TYPE_OTHER_FAX:
            cta.addOtherFax(number);
            break;
        case CommonDataKinds.Phone.TYPE_RADIO:

            break;
        case CommonDataKinds.Phone.TYPE_TELEX:
            cta.addTelTlx(number);
            break;
        case CommonDataKinds.Phone.TYPE_TTY_TDD:

            break;
        case CommonDataKinds.Phone.TYPE_WORK_MOBILE:
            cta.addWorkMobile(number);
            break;
        case CommonDataKinds.Phone.TYPE_WORK_PAGER:

            break;
        case CommonDataKinds.Phone.TYPE_ASSISTANT:

            break;
        case CommonDataKinds.Phone.TYPE_MMS:

            break;
        case CommonDataKinds.Phone.TYPE_CUSTOM:
            // ? deal with label that user define by self
            break;
        default:
            break;
        }
    }

    private void loadNicknameData(ContactItem cta, Cursor cur) {
        cta.setNickName(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.Nickname.NAME)));
    }

    private void loadStructuredNameData(ContactItem cta, Cursor cur) {
        cta.setName(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredName.DISPLAY_NAME)));
        cta.setFamilyName(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredName.FAMILY_NAME)));
        cta.setMiddleName(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredName.MIDDLE_NAME)));
        cta.setGivenName(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredName.GIVEN_NAME)));
        cta.setPrefix(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredName.PREFIX)));
        cta.setSuffix(cur.getString(cur
                .getColumnIndexOrThrow(CommonDataKinds.StructuredName.SUFFIX)));
    }

    @Override
    public boolean beginTransaction() {
        Log.d(TAG, "+beginTransaction()");
        mProviderOps.clear();
        mRawCtaInsertOpIndexs.clear();
        mGrpInsertOpIndexMap.clear();
        mResults.clear();
        Log.d(TAG, "-beginTransaction()");
        return true;
    }

    @Override
    public boolean endTransaction() {
        Log.d(TAG, "+endTransaction()");
        mProviderOps.clear();
        mRawCtaInsertOpIndexs.clear();
        mResults.clear();
        mGrpInsertOpIndexMap.clear();
        Log.d(TAG, "-endTransaction()");
        return true;
    }

    private boolean commit(boolean fromServer) {
        Log.d(TAG, "+commit(" + fromServer + ")");
        try {
            ContentProviderResult [] results;
            results = mResolver.applyBatch(ContactsContract.AUTHORITY,
                    mProviderOps);
            if (fromServer) {
                /*
                 * record each operation's result to mResults.
                 */
                for (int i = 0; i < mRawCtaInsertOpIndexs.size(); i++) {
                    int batchIndex = mRawCtaInsertOpIndexs.get(i);
                    if (batchIndex >= 0) {
                        mResults.add(results[batchIndex]); // add
                    } else {
                        mResults.add(null); // delete and replace
                    }
                }
                backupNewItems(results);
            }
            Log.d(TAG, "-commit(true)");
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "commit content provider operations failed.");
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            Log.e(TAG, "commit content provider operations failed.");
            e.printStackTrace();
        } finally {
            mProviderOps.clear();
            mRawCtaInsertOpIndexs.clear();
            mGrpInsertOpIndexMap.clear();
        }

        Log.d(TAG, "-commit(false)");
        return false;
    }

    @Override
    public boolean commit() {
        return commit(true);
    }

    @Override
    public int getTransResult(int index) {
        Log.d(TAG, String.format("+getTransResult(%d)", index));
        int ret = 0;
        ContentProviderResult result = mResults.get(index);
        if (result != null) {
            ret = (int) ContentUris.parseId(result.uri);
        }

        Log.d(TAG, "-getTransResult");
        return ret;
    }

    @Override
    public boolean addItem(SyncItem item) {
        Log.d(TAG, "+addItem()");

        if (null == item) {
            Log.i(TAG, "added item is null.");
            return false;
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        // add into raw contact table
        ContentProviderOperation rawContactOp = ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE_PHONE)
                .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME_PHONE)
                .withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED)
                .build();
        ops.add(rawContactOp);
        // add into data table
        // rawContactOpIndex will help get the right newly inserted raw contact
        // id
        int rawContactOpIndex = mProviderOps.size();
        addContactToData(ops, (ContactItem) item, rawContactOpIndex);

        if (mProviderOps.size() + ops.size() >= 500) {
            if (!commit(true)) {
                Log.e(TAG, "commit failed!");
                return false;
            }

            // re-create the operations with new backreference
            ops.clear();
            ops.add(rawContactOp);
            rawContactOpIndex = mProviderOps.size();
            addContactToData(ops, (ContactItem) item, rawContactOpIndex);
        }

        mRawCtaInsertOpIndexs.add(Integer.valueOf(rawContactOpIndex));
        mProviderOps.addAll(ops);
        Log.d(TAG, "-addItem()");
        return true;
    }

    /**
     * add ContactItem info into data table.
     *
     * @param rawCtaInsertOpIndex
     *            the index of raw contact insert operation, will helps get the
     *            right newly inserted raw contact id.
     */
    private void addContactToData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addStructuredNameData(ops, cta, rawCtaInsertOpIndex);
        addNicknameData(ops, cta, rawCtaInsertOpIndex);
        addPhoneData(ops, cta, rawCtaInsertOpIndex);
        addEmailData(ops, cta, rawCtaInsertOpIndex);
        addWebsiteData(ops, cta, rawCtaInsertOpIndex);
        addStructuredPostalData(ops, cta, rawCtaInsertOpIndex);
        addOrganizationData(ops, cta, rawCtaInsertOpIndex);
        addNoteData(ops, cta, rawCtaInsertOpIndex);
        addEventData(ops, cta, rawCtaInsertOpIndex);
        addRelationData(ops, cta, rawCtaInsertOpIndex);
        addImData(ops, cta, rawCtaInsertOpIndex);
        addGroupMembershipData(ops, cta, rawCtaInsertOpIndex);
    }

    /**
     * Create insert data builder.
     *
     * @param indexOrId
     *            If needBackReference is true, it represents the index of raw
     *            contact table insertion ContentProviderOperation in
     *            mProviderOps. Otherwise, it represents the raw contact id.
     * @param mimeType
     *            type of MIME.
     * @param needBackReference
     *            Decide the meaning of indexOrId.
     * @return The builder for insert data table.
     */
    private ContentProviderOperation.Builder createInsertDataBuilder(
            int indexOrId, String mimeType, boolean needBackReference) {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI).withValue(
                        ContactsContract.Data.MIMETYPE, mimeType);
        if (needBackReference) {
            builder.withValueBackReference(
                    ContactsContract.Data.RAW_CONTACT_ID, indexOrId);
        } else {
            builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, indexOrId);
        }

        return builder;
    }

    /**
     * Insert data of StructuredName MIME type into data table from ContactItem.
     *
     * @param cta
     *            The ContactItem.
     * @param indexOrId
     *            If needBackReference is true, it represents the index of raw
     *            contact table insertion ContentProviderOperation in
     *            mProviderOps. Otherwise, it represents the raw contact id.
     * @param needBackReference
     *            Decide the meaning of indexOrId.
     */
    private void addStructuredNameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int indexOrId, boolean needBackReference) {
        ContentProviderOperation.Builder builder = createInsertDataBuilder(
                indexOrId, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                needBackReference);

        if (cta.getName() != null) {
            builder.withValue(CommonDataKinds.StructuredName.DISPLAY_NAME,
                    cta.getName());
        }
        if (cta.getFamilyName() != null) {
            builder.withValue(CommonDataKinds.StructuredName.FAMILY_NAME,
                    cta.getFamilyName());
        }
        if (cta.getMiddleName() != null) {
            builder.withValue(CommonDataKinds.StructuredName.MIDDLE_NAME,
                    cta.getMiddleName());
        }
        if (cta.getGivenName() != null) {
            builder.withValue(CommonDataKinds.StructuredName.GIVEN_NAME,
                    cta.getGivenName());
        }
        if (cta.getPrefix() != null) {
            builder.withValue(CommonDataKinds.StructuredName.PREFIX,
                    cta.getPrefix());
        }
        if (cta.getSuffix() != null) {
            builder.withValue(CommonDataKinds.StructuredName.SUFFIX,
                    cta.getSuffix());
        }

        ops.add(builder.build());
    }

    /**
     * Insert data of StructuredName MIME type into data table from ContactItem
     * by an unknown raw contact id which should use
     * ContentProviderOperation.Builder's withValueBackReference() method to
     * get.
     *
     * @param cta
     * @param rawCtaInsertOpIndex
     */
    private void addStructuredNameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addStructuredNameData(ops, cta, rawCtaInsertOpIndex, true);
    }

    /**
     * Insert data of StructuredName MIME type into data table from ContactItem
     * by a known raw contact id.
     */
    private void addStructuredNameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        addStructuredNameData(ops, cta, cta.getId(), false);
    }

    private void addNicknameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int indexOrId, boolean needBackReference) {
        ContentProviderOperation.Builder builder = createInsertDataBuilder(
                indexOrId, CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
                needBackReference);

        if (cta.getNickName() != null) {
            builder.withValue(CommonDataKinds.Nickname.NAME, cta.getNickName());
        }

        ops.add(builder.build());
    }

    private void addNicknameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addNicknameData(ops, cta, rawCtaInsertOpIndex, true);
    }

    private void addNicknameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        addNicknameData(ops, cta, cta.getId(), false);
    }

    private void addPhoneDataByType(ArrayList<ContentProviderOperation> ops,
            int phoneType, List<String> numberList, int indexOrId,
            boolean needBackReference) {
        if (numberList != null && !numberList.isEmpty()) {
            for (String number : numberList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Phone.TYPE, phoneType)
                        .withValue(CommonDataKinds.Phone.NUMBER, number);
                ops.add(builder.build());
            }
        }
    }

    private void addPhoneData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_MOBILE,
                cta.getMobileList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_HOME,
                cta.getHomeTelList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_WORK,
                cta.getWorkTelList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_CAR,
                cta.getCarTelList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_WORK_MOBILE,
                cta.getWorkMobileList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_FAX_WORK,
                cta.getWorkFaxList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_FAX_HOME,
                cta.getHomeFaxList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_OTHER_FAX,
                cta.getOtherFaxList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_COMPANY_MAIN,
                cta.getCompanyTelExchangeList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_PAGER,
                cta.getPagerList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_TELEX,
                cta.getTelTlxList(), rawCtaInsertOpIndex, true);
        addPhoneDataByType(ops, CommonDataKinds.Phone.TYPE_OTHER,
                cta.getOtherTelList(), rawCtaInsertOpIndex, true);
    }

    private void addEmailDataByType(ArrayList<ContentProviderOperation> ops,
            int emailType, List<String> addrList, int indexOrId,
            boolean needBackReference) {
        if (addrList != null && !addrList.isEmpty()) {
            for (String addr : addrList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Email.TYPE, emailType)
                        .withValue(CommonDataKinds.Email.ADDRESS, addr);
                ops.add(builder.build());
            }
        }
    }

    private void addEmailData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addEmailDataByType(ops, CommonDataKinds.Email.TYPE_HOME,
                cta.getHomeMailList(), rawCtaInsertOpIndex, true);
        addEmailDataByType(ops, CommonDataKinds.Email.TYPE_WORK,
                cta.getWorkMailList(), rawCtaInsertOpIndex, true);
        addEmailDataByType(ops, CommonDataKinds.Email.TYPE_OTHER,
                cta.getOtherMailList(), rawCtaInsertOpIndex, true);
    }

    private void addWebsiteDataByType(ArrayList<ContentProviderOperation> ops,
            int websiteType, List<String> urlList, int indexOrId,
            boolean needBackReference) {
        if (urlList != null && !urlList.isEmpty()) {
            for (String url : urlList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Website.TYPE, websiteType)
                        .withValue(CommonDataKinds.Website.URL, url);
                ops.add(builder.build());
            }
        }
    }

    private void addWebsiteData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addWebsiteDataByType(ops, CommonDataKinds.Website.TYPE_HOME,
                cta.getHomeWebsiteList(), rawCtaInsertOpIndex, true);
        addWebsiteDataByType(ops, CommonDataKinds.Website.TYPE_WORK,
                cta.getWorkWebsiteList(), rawCtaInsertOpIndex, true);
        addWebsiteDataByType(ops, CommonDataKinds.Website.TYPE_OTHER,
                cta.getOtherWebsiteList(), rawCtaInsertOpIndex, true);
    }

    private void addGroupMembershipData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta) {
        addGroupMembershipData(ops, cta, cta.getId(), false);
    }

    private void addGroupMembershipData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta,
            int rawCtaInsertOpIndex) {
        addGroupMembershipData(ops, cta, rawCtaInsertOpIndex, true);
    }

    private void addGroupMembershipData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta,
            int indexOrId, boolean needBackReference) {
        List<String> titleList = cta.getGroupNameList();
        if (null == titleList || titleList.isEmpty()) {
            return;
        }
        for (String title : titleList) {
            ContentProviderOperation.Builder builder = createInsertDataBuilder(
                    indexOrId,
                    CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    needBackReference);

            // query db if group exist before transaction commit
            String selection = ContactsContract.Groups.TITLE + "=\'" + title
                    + "\'" + " AND " + ContactsContract.Groups.DELETED + "=0"
                    + " AND " + RawContacts.ACCOUNT_TYPE + "=\'"
                    + ACCOUNT_TYPE_PHONE + "\'" + " AND "
                    + RawContacts.ACCOUNT_NAME + "=\'" + ACCOUNT_NAME_PHONE
                    + "\'";
            Cursor cursor = mResolver.query(
                    ContactsContract.Groups.CONTENT_URI, null, selection, null,
                    null);
            if (null == cursor || !cursor.moveToFirst()) {
                // group does not exist, need to query local cache.
                int opIndex = -1;
                if (!mGrpInsertOpIndexMap.containsKey(title)) {
                    // not exist in cache too, insert this group into group db.
                    opIndex = insertGroupOperation(ops, title);
                    mGrpInsertOpIndexMap.put(title, opIndex);
                } else {
                    // exist in cache, represents it is a newly inserted group
                    // in
                    // current transaction.
                    opIndex = mGrpInsertOpIndexMap.get(title);
                }
                Log.d(TAG, String.format("Add group %s with bf %d", title, opIndex));
                builder.withValueBackReference(
                        CommonDataKinds.GroupMembership.GROUP_ROW_ID, opIndex);
            } else {
                // group exists
                int rowId = cursor.getInt(cursor
                        .getColumnIndexOrThrow(ContactsContract.Groups._ID));
                Log.e(TAG, "addGroupMembershipData(): " + title
                        + " group exists, row id is " + rowId);
                builder.withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                        rowId);
            }
            ops.add(builder.build());

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Insert a new group whose title is groupTitle.
     *
     * @param groupTitle
     *            The title of group.
     * @return The index of this insert group operation in mProviderOps.
     */
    private int insertGroupOperation(ArrayList<ContentProviderOperation> ops,
            String groupTitle) {
        ContentProviderOperation groupOp = ContentProviderOperation
                .newInsert(ContactsContract.Groups.CONTENT_URI)
                .withValue(ContactsContract.Groups.ACCOUNT_TYPE,
                        ACCOUNT_TYPE_PHONE)
                .withValue(ContactsContract.Groups.ACCOUNT_NAME,
                        ACCOUNT_NAME_PHONE)
                .withValue(ContactsContract.Groups.TITLE, groupTitle).build();
        ops.add(groupOp);
        Log.d(TAG, String.format("insert group %s, %s, index:%d", groupTitle,
                groupOp.toString(), mProviderOps.size() + ops.size() - 1));
        return mProviderOps.size() + ops.size() - 1;
    }

    private void addNoteData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int indexOrId, boolean needBackReference) {
        List<String> noteList = cta.getNoteList();
        if (noteList != null && !noteList.isEmpty()) {
            for (String note : noteList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Note.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Note.NOTE, note);
                ops.add(builder.build());
            }
        }
    }

    private void addNoteData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addNoteData(ops, cta, rawCtaInsertOpIndex, true);
    }

    private void addNoteData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        addNoteData(ops, cta, cta.getId(), false);
    }

    private void addEventDataByType(ArrayList<ContentProviderOperation> ops,
            int eventType, List<String> dateList, int indexOrId,
            boolean needBackReference) {
        if (dateList != null && !dateList.isEmpty()) {
            for (String date : dateList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Event.TYPE, eventType)
                        .withValue(CommonDataKinds.Event.START_DATE, date);
                ops.add(builder.build());
            }
        }
    }

    private void addEventData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addEventDataByType(ops, CommonDataKinds.Event.TYPE_ANNIVERSARY,
                cta.getAnniversaryList(), rawCtaInsertOpIndex, true);
        addEventDataByType(ops, CommonDataKinds.Event.TYPE_BIRTHDAY,
                cta.getBirthdayList(), rawCtaInsertOpIndex, true);
    }

    private void addRelationDataByType(ArrayList<ContentProviderOperation> ops,
            int relaType, List<String> nameList, int indexOrId,
            boolean needBackReference) {
        if (nameList != null && !nameList.isEmpty()) {
            for (String name : nameList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Relation.TYPE, relaType)
                        .withValue(CommonDataKinds.Relation.NAME, name);
                ops.add(builder.build());
            }
        }
    }

    private void addRelationData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addRelationDataByType(ops, CommonDataKinds.Relation.TYPE_CHILD,
                cta.getChildList(), rawCtaInsertOpIndex, true);
        addRelationDataByType(ops, CommonDataKinds.Relation.TYPE_SPOUSE,
                cta.getSpouseList(), rawCtaInsertOpIndex, true);
    }

    private void addImDataByProtocol(ArrayList<ContentProviderOperation> ops,
            int proType, List<String> dataList, int indexOrId,
            boolean needBackReference) {
        if (dataList != null && !dataList.isEmpty()) {
            for (String data : dataList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId, CommonDataKinds.Im.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Im.PROTOCOL, proType)
                        .withValue(CommonDataKinds.Im.DATA, data);
                ops.add(builder.build());
            }
        }
    }

    private void addImData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addImDataByProtocol(ops, CommonDataKinds.Im.PROTOCOL_QQ,
                cta.getQQList(), rawCtaInsertOpIndex, true);
        addImDataByProtocol(ops, CommonDataKinds.Im.PROTOCOL_MSN,
                cta.getMsnList(), rawCtaInsertOpIndex, true);
    }

    private void addStructuredPostalByType(
            ArrayList<ContentProviderOperation> ops, int postalType,
            List<PostalAddress> addrList, int indexOrId,
            boolean needBackReference) {
        if (addrList != null && !addrList.isEmpty()) {
            for (PostalAddress addr : addrList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId,
                        CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.StructuredPostal.TYPE,
                        postalType)
                        .withValue(CommonDataKinds.StructuredPostal.STREET,
                                addr.getStreet())
                        .withValue(CommonDataKinds.StructuredPostal.CITY,
                                addr.getCity())
                        .withValue(CommonDataKinds.StructuredPostal.REGION,
                                addr.getArea())
                        .withValue(CommonDataKinds.StructuredPostal.POSTCODE,
                                addr.getPostalCode())
                        .withValue(CommonDataKinds.StructuredPostal.COUNTRY,
                                addr.getState());
                ops.add(builder.build());
            }
        }
    }

    private void addStructuredPostalData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta,
            int rawCtaInsertOpIndex) {
        addStructuredPostalByType(ops,
                CommonDataKinds.StructuredPostal.TYPE_HOME,
                cta.getHomeAssembleAddrList(), rawCtaInsertOpIndex, true);
        addStructuredPostalByType(ops,
                CommonDataKinds.StructuredPostal.TYPE_WORK,
                cta.getWorkAssembleAddrList(), rawCtaInsertOpIndex, true);
        addStructuredPostalByType(ops,
                CommonDataKinds.StructuredPostal.TYPE_OTHER,
                cta.getOtherAssembleAddrList(), rawCtaInsertOpIndex, true);
    }

    private void addOrganizationByType(ArrayList<ContentProviderOperation> ops,
            List<Organization> orgList, int indexOrId,
            boolean needBackReference) {
        if (orgList != null && !orgList.isEmpty()) {
            for (Organization org : orgList) {
                ContentProviderOperation.Builder builder = createInsertDataBuilder(
                        indexOrId,
                        CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                        needBackReference);
                builder.withValue(CommonDataKinds.Organization.COMPANY,
                                org.getCompany())
                        .withValue(CommonDataKinds.Organization.DEPARTMENT,
                                org.getDepartment())
                        .withValue(CommonDataKinds.Organization.TITLE,
                                org.getPosition());
                ops.add(builder.build());
            }
        }
    }

    private void addOrganizationData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta, int rawCtaInsertOpIndex) {
        addOrganizationByType(ops,
                cta.getAssembleOrgList(), rawCtaInsertOpIndex, true);
    }

    @Override
    public boolean replaceItem(SyncItem item) {
        if (item.getId() <= ContactItem.INVALID_ID) {
            Log.i(TAG, "invalid raw contact id, updateItem() do nothing.");
            return false;
        }
        if (!isIdExist(item.getId())) {
            Log.i(TAG, "raw contact id do not exist, just do add operation.");
            addItem(item);
        } else {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            updateDataFromContact(ops, (ContactItem) item);
            if (!addOperations(ops, -1)) {
                return false;
            }
        }

        return true;
    }

    private boolean isIdExist(int id) {
        String[] projection = {RawContacts._ID};
        String selection = getPhoneContactSelection();
        Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, id);
        Cursor cursor = mResolver.query(uri, projection, selection, null, null);

        boolean exist;
        if (null == cursor || !cursor.moveToFirst()) {
            exist = false;
        } else {
            exist = true;
        }
        if (cursor != null) {
            cursor.close();
        }
        return exist;
    }

    /**
     * Update data table by MIME type.
     */
    private void updateDataFromContact(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updateStructuredNameData(ops, cta);
        updateNicknameData(ops, cta);
        updatePhoneData(ops, cta);
        updateEmailData(ops, cta);
        updateWebsiteData(ops, cta);
        updateStructuredPostalData(ops, cta);
        updateOrganizationData(ops, cta);
        updateNoteData(ops, cta);
        updateEventData(ops, cta);
        updateRelationData(ops, cta);
        updateImData(ops, cta);
        updateGroupMembershipData(ops, cta);
    }

    private void updateGroupMembershipData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta) {
        if (!needUpdateGroupMembership(cta)) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(new String[] {
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE },
                new String[] {String.valueOf(cta.getId()),
                        CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addGroupMembershipData(ops, cta);
    }

    private boolean needUpdateGroupMembership(ContactItem cta) {
        return cta.getGroupNameList() != null;
    }

    private void updateOrganizationData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta) {
        updateOrganizationByType(ops,
                cta.getAssembleOrgList(), cta.getId());
    }

    private void updateOrganizationByType(
            ArrayList<ContentProviderOperation> ops,
            List<Organization> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE},
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Organization.CONTENT_ITEM_TYPE});
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addOrganizationByType(ops, list, rawContactId, false);
    }

    private void updateStructuredPostalData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta) {
        updateStructuredPostalByType(ops,
                CommonDataKinds.StructuredPostal.TYPE_HOME,
                cta.getHomeAssembleAddrList(), cta.getId());
        updateStructuredPostalByType(ops,
                CommonDataKinds.StructuredPostal.TYPE_WORK,
                cta.getWorkAssembleAddrList(), cta.getId());
        updateStructuredPostalByType(ops,
                CommonDataKinds.StructuredPostal.TYPE_OTHER,
                cta.getOtherAssembleAddrList(), cta.getId());
    }

    private void updateStructuredPostalByType(
            ArrayList<ContentProviderOperation> ops, int type,
            List<PostalAddress> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(new String[] {
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                CommonDataKinds.StructuredPostal.TYPE },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                        String.valueOf(type) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addStructuredPostalByType(ops, type, list, rawContactId, false);
    }

    private void updateImData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updateImDataByProtocol(ops, CommonDataKinds.Im.PROTOCOL_QQ,
                cta.getQQList(), cta.getId());
        updateImDataByProtocol(ops, CommonDataKinds.Im.PROTOCOL_MSN,
                cta.getMsnList(), cta.getId());
    }

    private void updateImDataByProtocol(
            ArrayList<ContentProviderOperation> ops, int protocol,
            List<String> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Im.PROTOCOL },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Im.CONTENT_ITEM_TYPE,
                        String.valueOf(protocol) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addImDataByProtocol(ops, protocol, list, rawContactId, false);
    }

    private void updateRelationData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updateRelationDataByType(ops, CommonDataKinds.Relation.TYPE_CHILD,
                cta.getChildList(), cta.getId());
        updateRelationDataByType(ops, CommonDataKinds.Relation.TYPE_SPOUSE,
                cta.getSpouseList(), cta.getId());
    }

    private void updateRelationDataByType(
            ArrayList<ContentProviderOperation> ops, int type,
            List<String> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Relation.TYPE },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
                        String.valueOf(type) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addRelationDataByType(ops, type, list, rawContactId, false);
    }

    private void updateEventData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updateEventByType(ops, CommonDataKinds.Event.TYPE_ANNIVERSARY,
                cta.getAnniversaryList(), cta.getId());
        updateEventByType(ops, CommonDataKinds.Event.TYPE_BIRTHDAY,
                cta.getBirthdayList(), cta.getId());
    }

    private void updateEventByType(ArrayList<ContentProviderOperation> ops,
            int type, List<String> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Event.TYPE },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                        String.valueOf(type) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addEventDataByType(ops, type, list, rawContactId, false);
    }

    private void updateNoteData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        if (!needUpdateNote(cta)) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(new String[] {
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE },
                new String[] {String.valueOf(cta.getId()),
                        CommonDataKinds.Note.CONTENT_ITEM_TYPE });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addNoteData(ops, cta);
    }

    private boolean needUpdateNote(ContactItem cta) {
        return cta.getNoteList() != null;
    }

    private void updateWebsiteData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updateWebsiteDataByType(ops, CommonDataKinds.Website.TYPE_HOME,
                cta.getHomeWebsiteList(), cta.getId());
        updateWebsiteDataByType(ops, CommonDataKinds.Website.TYPE_WORK,
                cta.getWorkWebsiteList(), cta.getId());
        updateWebsiteDataByType(ops, CommonDataKinds.Website.TYPE_OTHER,
                cta.getOtherWebsiteList(), cta.getId());
    }

    private void updateWebsiteDataByType(
            ArrayList<ContentProviderOperation> ops, int type,
            List<String> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Website.TYPE },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                        String.valueOf(type) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addWebsiteDataByType(ops, type, list, rawContactId, false);
    }

    private void updateEmailData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updateEmailDataByType(ops, CommonDataKinds.Email.TYPE_HOME,
                cta.getHomeMailList(), cta.getId());
        updateEmailDataByType(ops, CommonDataKinds.Email.TYPE_WORK,
                cta.getWorkMailList(), cta.getId());
        updateEmailDataByType(ops, CommonDataKinds.Email.TYPE_OTHER,
                cta.getOtherMailList(), cta.getId());
    }

    private void updateEmailDataByType(ArrayList<ContentProviderOperation> ops,
            int type, List<String> list, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == list) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Email.TYPE },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                        String.valueOf(type) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addEmailDataByType(ops, type, list, rawContactId, false);
    }

    private void updatePhoneData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_MOBILE,
                cta.getMobileList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_HOME,
                cta.getHomeTelList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_WORK,
                cta.getWorkTelList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_CAR,
                cta.getCarTelList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_WORK_MOBILE,
                cta.getWorkMobileList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_FAX_WORK,
                cta.getWorkFaxList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_FAX_HOME,
                cta.getHomeFaxList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_OTHER_FAX,
                cta.getOtherFaxList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_COMPANY_MAIN,
                cta.getCompanyTelExchangeList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_PAGER,
                cta.getPagerList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_TELEX,
                cta.getTelTlxList(), cta.getId());
        updatePhoneDataByType(ops, CommonDataKinds.Phone.TYPE_OTHER,
                cta.getOtherTelList(), cta.getId());

    }

    private void updatePhoneDataByType(ArrayList<ContentProviderOperation> ops,
            int phoneType, List<String> numberList, int rawContactId) {
        // if numberList is empty, still need to update by empty
        if (null == numberList) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(
                new String[] {ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Phone.TYPE },
                new String[] {String.valueOf(rawContactId),
                        CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                        String.valueOf(phoneType) });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addPhoneDataByType(ops, phoneType, numberList, rawContactId, false);
    }

    private void updateNicknameData(ArrayList<ContentProviderOperation> ops,
            ContactItem cta) {
        if (!needUpdateNickname(cta)) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(new String[] {
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE },
                new String[] {String.valueOf(cta.getId()),
                        CommonDataKinds.Nickname.CONTENT_ITEM_TYPE });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addNicknameData(ops, cta);
    }

    private boolean needUpdateNickname(ContactItem cta) {
        return cta.getNickName() != null;
    }

    private void updateStructuredNameData(
            ArrayList<ContentProviderOperation> ops, ContactItem cta) {
        if (!needUpdateStructName(cta)) {
            return;
        }
        // delete old data if exist in data table
        String delSelect = composeSelection(new String[] {
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE },
                new String[] {String.valueOf(cta.getId()),
                        CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE });
        deleteData(ops, delSelect);
        // insert new data with ContactItem
        addStructuredNameData(ops, cta);
    }

    private boolean needUpdateStructName(ContactItem cta) {
        return (cta.getName() != null || cta.getFamilyName() != null
                || cta.getMiddleName() != null || cta.getGivenName() != null
                || cta.getPrefix() != null || cta.getSuffix() != null);
    }

    private String composeSelection(String[] selectKeys, String[] selectValues) {
        if (null == selectKeys || null == selectValues) {
            Log.i(TAG, "selection is null.");
            return null;
        }
        int keysLen = selectKeys.length;
        int valuesLen = selectValues.length;
        if (0 == keysLen || 0 == valuesLen || keysLen != valuesLen) {
            Log.i(TAG, "selection is null.");
            return null;
        }

        StringBuilder selection = new StringBuilder();
        for (int i = 0; i < keysLen; i++) {
            selection.append(selectKeys[i]).append("=\'")
                    .append(selectValues[i]).append("\'");
            if (i + 1 < keysLen) { // if has next
                selection.append(" AND ");
            }
        }
        return selection.toString();
    }

    /**
     * Delete data by selection.
     *
     * @param selection
     *            Where clause to delete.
     */
    protected void deleteData(ArrayList<ContentProviderOperation> ops,
            String selection) {
        ContentProviderOperation op = ContentProviderOperation
                .newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(selection, null).build();
        ops.add(op);
    }

    /*
     * Delete item from raw_contacts table by key, while related rows in Data
     * table are automatically deleted.
     *
     * @param key Raw contact id to delete.
     */
    @Override
    public boolean deleteItem(int key) {
        Log.d(TAG, String.format("deleteItem %d", key));
        Uri uri = setCallerIsSyncAdapterFlag(RawContacts.CONTENT_URI);
        uri = ContentUris.withAppendedId(uri, key);
        ContentProviderOperation op = ContentProviderOperation.newDelete(uri)
                .build();
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(op);

        return addOperations(ops, -2);
    }

    private boolean addOperations(ArrayList<ContentProviderOperation> ops, int type) {
        if (mProviderOps.size() + ops.size() >= 500) {
            if (!commit(true)) {
                Log.e(TAG, "commit failed!");
                return false;
            }
        }
        mRawCtaInsertOpIndexs.add(Integer.valueOf(type));
        mProviderOps.addAll(ops);
        return true;
    }

    private Uri setCallerIsSyncAdapterFlag(Uri uri) {
        Uri.Builder builder = uri.buildUpon();
        builder.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
                "true");
        return builder.build();
    }

    @Override
    public int getNewItemCount() {
        return 0;
    }

    @Override
    public int getUpdateItemCount() {
        return 0;
    }

    @Override
    public int getDeleteItemCount() {
        return 0;
    }

    @Override
    public SyncItem fromData(String jsonData) {
        if (null == jsonData || jsonData.isEmpty()) {
            Log.e(TAG, "param jsonData of fromJson() is nulll or empty.");
            return null;
        }
        ContactItem cta = new ContactItem();
        try {
            JSONObject jContact = new JSONObject(jsonData);
            // if jsonData has no some tag, just leaving corresponding field of
            // ContactItem as null.
            cta.setName(JsonUtil.parseJsonString(jContact, JsonTag.NAME));
            cta.setFamilyName(JsonUtil.parseJsonString(jContact,
                    JsonTag.FAMILY_NAME));
            cta.setMiddleName(JsonUtil.parseJsonString(jContact,
                    JsonTag.MIDDLE_NAME));
            cta.setGivenName(JsonUtil.parseJsonString(jContact,
                    JsonTag.GIVEN_NAME));
            cta.setNickName(JsonUtil.parseJsonString(jContact,
                    JsonTag.NICK_NAME));
            cta.setGender(JsonUtil.parseJsonString(jContact, JsonTag.GENDER));
            cta.setPrefix(JsonUtil.parseJsonString(jContact, JsonTag.PREFIX));
            cta.setSuffix(JsonUtil.parseJsonString(jContact, JsonTag.SUFFIX));
            cta.setCarTelList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.CAR_TEL));
            cta.setMobileList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.MOBILE));
            cta.setWorkMobileList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WORK_MOBILE));
            cta.setHomeMobileList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.HOME_MOBILE));
            cta.setOtherMobileList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.OTHER_MOBILE));
            cta.setIphoneList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.IPHONE));
            cta.setTelList(JsonUtil.parseJsonStringArray(jContact, JsonTag.TEL));
            cta.setWorkTelList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WORK_TEL));
            cta.setHomeTelList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.HOME_TEL));
            cta.setOtherTelList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.OTHER_TEL));
            cta.setShortTelNumList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.SHORT_TEL_NUM));
            cta.setFaxList(JsonUtil.parseJsonStringArray(jContact, JsonTag.FAX));
            cta.setWorkFaxList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WORK_FAX));
            cta.setHomeFaxList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.HOME_FAX));
            cta.setOtherFaxList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.OTHER_FAX));
            cta.setCompanyTelExchangeList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.COMPANY_TEL_EX));
            cta.setPagerList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.PAGER));
            cta.setTelTlx(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.TEL_TLX));
            cta.setEmailList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.EMAIL));
            cta.setWorkMailList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WORK_MAIL));
            cta.setHomeMailList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.HOME_MAIL));
            cta.setOtherMailList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.OTHER_MAIL));
            cta.setWebsiteList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WEBSITE));
            cta.setWorkWebsiteList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WORK_WEBSITE));
            cta.setHomeWebsiteList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.HOME_WEBSITE));
            cta.setOtherWebsiteList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.OTHER_WEBSITE));
            cta.setAddrList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.ADDRESS));
            cta.setWorkAddrList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WORK_ADDRESS));
            cta.setHomeAddrList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.HOME_ADDRESS));
            cta.setAssembleAddrList(JsonUtil.parseJsonPostalAddrArray(jContact,
                    JsonTag.ASSEMBLE_ADDR));
            cta.setWorkAssembleAddrList(JsonUtil.parseJsonPostalAddrArray(
                    jContact, JsonTag.WORK_ASSEMBLE_ADDR));
            cta.setHomeAssembleAddrList(JsonUtil.parseJsonPostalAddrArray(
                    jContact, JsonTag.HOME_ASSEMBLE_ADDR));
            cta.setOtherAssembleAddrList(JsonUtil.parseJsonPostalAddrArray(
                    jContact, JsonTag.OTHER_ASSEMBLE_ADDR));
            cta.setAssembleOrgList(JsonUtil.parseJsonOrgArray(jContact,
                    JsonTag.ASSEMBLE_ORG));
            cta.setNoteList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.NOTE));
            cta.setBirthdayList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.BIRTHDAY));
            cta.setAnniversaryList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.ANNIVERSARY));
            cta.setChildList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.CHILD));
            cta.setSpouseList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.SPOUSE));
            cta.setFetionList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.FETION));
            cta.setQQList(JsonUtil.parseJsonStringArray(jContact, JsonTag.QQ));
            cta.setMsnList(JsonUtil.parseJsonStringArray(jContact, JsonTag.MSN));
            cta.setWeiboList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.WEIBO));
            cta.setBlogList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.BLOG));
            cta.setGroupNameList(JsonUtil.parseJsonStringArray(jContact,
                    JsonTag.GROUP_NAME));
            return cta;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean backup() {
        Log.d(TAG, "+backup()");
        /* delete all backup file */
        mContext.deleteFile(BACKUP_FILE_NEW);
        mContext.deleteFile(BACKUP_FILE_OLD);

        Cursor cursor = null;
        FileOutputStream fo = null;
        /* get all items' id and save them */
        try {
            /* Create new file */
            FileOutputStream fn = mContext.openFileOutput(BACKUP_FILE_NEW,
                    Context.MODE_PRIVATE);
            fn.close();

            cursor = getAllItemCursor();
            if (null == cursor) {
                Log.e(TAG, "cursor is null.");
                return false;
            }

            fo = mContext.openFileOutput(BACKUP_FILE_OLD, Context.MODE_PRIVATE);

            ByteBuffer bb = ByteBuffer.allocate(1024);
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor
                            .getColumnIndexOrThrow(RawContacts._ID));
                    bb.putInt(id);
                    if (!bb.hasRemaining()) {
                        fo.write(bb.array(), 0, bb.capacity());
                        bb.clear();
                    }

                } while (cursor.moveToNext());
                fo.write(bb.array(), 0, bb.position());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        Log.d(TAG, "-backup() true");
        return true;
    }

    /**
     * Delete items recorded in file.
     *
     * @param file
     *            recorded file
     * @return true or false
     */
    private boolean deleteBackupItems(String file) {
        Log.d(TAG, String.format("+deleteBackupItems(%s)", file));
        FileInputStream fi = null;

        try {
            fi = mContext.openFileInput(file);
            FileChannel fn = fi.getChannel();

            ByteBuffer bb = ByteBuffer.allocate(1024);
            while (fn.read(bb) > 0) {
                bb.flip();
                /* delete all the items */
                beginTransaction();
                try {
                    while (true) {
                        int id = bb.getInt();
                        Log.d(TAG, String.format("delete %d", id));
                        deleteItem(id);
                    }
                } catch (BufferUnderflowException e) {
                    bb.clear();
                }
                commit(false);
                endTransaction();
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, file + " not exist!");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fi != null) {
                try {
                    fi.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "-deleteBackupItems() return true");
        return true;
    }

    /**
     * Save all the added item's id.
     * @param results batch operations results.
     * @return true or false.
     */
    private boolean backupNewItems(ContentProviderResult [] results) {
        Log.d(TAG, "+backupNewItems()");
        FileOutputStream fo = null;
        /* get all items' id added and save them */
        try {
            fo = mContext.openFileOutput(BACKUP_FILE_NEW, Context.MODE_APPEND);

            ByteBuffer bb = ByteBuffer.allocate(1024);
            for (int i = 0; i < mRawCtaInsertOpIndexs.size(); i++) {
                int batchIndex = mRawCtaInsertOpIndexs.get(i);
                if (batchIndex < 0) {
                    continue;
                }

                int id = (int) ContentUris.parseId(results[batchIndex].uri);
                if (id <= 0) {
                    continue;
                }

                bb.putInt(id);
                Log.d(TAG, String.format("save %d", id));
                if (!bb.hasRemaining()) {
                    fo.write(bb.array(), 0, bb.capacity());
                    bb.clear();
                }

            }

            fo.write(bb.array(), 0, bb.position());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        Log.d(TAG, "-backupNewItems() true");
        return true;
    }

    @Override
    public boolean clearBackup() {
        Log.d(TAG, "+clearBackup()");
        mContext.deleteFile(BACKUP_FILE_NEW);
        /* delete all the items in backup file */
        deleteBackupItems(BACKUP_FILE_OLD);
        /* delete all the backup files */
        mContext.deleteFile(BACKUP_FILE_OLD);
        Log.d(TAG, "-clearBackup()");
        return true;
    }

    @Override
    public boolean rollback() {
        Log.d(TAG, "+rollback()");
        mContext.deleteFile(BACKUP_FILE_OLD);
        /* delete all the items in new added file */
        deleteBackupItems(BACKUP_FILE_NEW);
        /* delete all the backup files */
        mContext.deleteFile(BACKUP_FILE_NEW);
        Log.d(TAG, "-rollback()");
        return true;
    }

    @Override
    public boolean checkBackup() {
        Log.d(TAG, "+checkBackup()");
        boolean ne = mContext.getFileStreamPath(BACKUP_FILE_NEW).exists();
        if (ne) {
            rollback();
        } else { // only old exists
            clearBackup();
        }

        Log.d(TAG, "-checkBackup() return " + !ne);
        return !ne;
    }

    @Override
    public void startSync() {
        createCursorCache();
    }

    @Override
    public void endSync() {
        /* close all the cached cursors */
        if (mAllCursor != null) {
            mAllCursor.close();
            mAllCursor = null;
        }

        if (mAllDataCursor != null) {
            mAllDataCursor.close();
            mAllDataCursor = null;
        }

    }

    /**
     * Is contacts empty refreshed from server
     * @return true or false.
     */
    @Override
    public boolean isEmptyRestoreFromServer() {
        return (mContext.getFileStreamPath(BACKUP_FILE_NEW).length() == 0) ? true : false ;
    }
}
