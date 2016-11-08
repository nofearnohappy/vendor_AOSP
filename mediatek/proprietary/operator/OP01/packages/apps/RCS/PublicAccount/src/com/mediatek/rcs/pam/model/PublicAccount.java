package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.util.Utils;


public class PublicAccount implements SanityCheck {

    private static final String TAG = "PAM/PublicAccount";

    public static final String[] BASIC_PROJECTION = {
        AccountColumns.ID,
        AccountColumns.UUID,
        AccountColumns.NAME,
        AccountColumns.ID_TYPE,
        AccountColumns.INTRODUCTION,
        AccountColumns.RECOMMEND_LEVEL,
        AccountColumns.LOGO_ID,
        AccountColumns.LOGO_URL,
        AccountColumns.LOGO_PATH,
        AccountColumns.SUBSCRIPTION_STATUS,
    };

    public static String[] sDetailProjection = {
        AccountColumns.ID,
        AccountColumns.UUID,
        AccountColumns.NAME,
        AccountColumns.ID_TYPE,
        AccountColumns.INTRODUCTION,
        AccountColumns.RECOMMEND_LEVEL,
        AccountColumns.LOGO_ID,
        AccountColumns.LOGO_URL,
        AccountColumns.LOGO_PATH,
        AccountColumns.SUBSCRIPTION_STATUS,

        AccountColumns.COMPANY,
        AccountColumns.TYPE,
        AccountColumns.UPDATE_TIME,
        AccountColumns.MENU_TYPE,
        AccountColumns.MENU_TIMESTAMP,
        AccountColumns.ACTIVE_STATUS,
        AccountColumns.ACCEPT_STATUS,
        AccountColumns.TELEPHONE,
        AccountColumns.EMAIL,
        AccountColumns.ZIPCODE,
        AccountColumns.ADDRESS,
        AccountColumns.FIELD,
        AccountColumns.QRCODE_URL,

        AccountColumns.MENU,

        AccountColumns.LAST_MESSAGE,
    };

    // basic
    public String uuid;
    public String name;
    public int idtype;
    public String introduction;
    public int recommendLevel;
    public String logoUrl;
    public int subscribeStatus;

    // detail
    public String company;
    public String type;
    public long updateTime;
    public int menuType;
    public long menuTimestamp;
    public String qrcode;
    public int activeStatus;
    public int acceptStatus;
    public String telephone;
    public String email;
    public String zipcode;
    public String address;
    public String field;

    // Android Specific
    public long id = Constants.INVALID;
    public String logoPath = null;
    public long logoId = Constants.INVALID;
    public Bitmap logoImage = null;
    public String menu = null;
    private MenuInfo mMenuInfo = null;
    public long lastMessageId = Constants.INVALID;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"PublicAccountDetail\", uuid:\"")
        .append(uuid)
        .append("\", name:\"")
        .append(name)
        .append("\", recommendLevel:")
        .append(recommendLevel)
        .append(", logoUrl:\"")
        .append(logoUrl)
        .append("\", company:\"")
        .append(company)
        .append("\", introduction:\"")
        .append(introduction)
        .append("\", type:")
        .append(type)
        .append("\", idtype:")
        .append(idtype)
        .append(", updateTime:\"")
        .append(updateTime)
        .append("\", menuType:")
        .append(menuType)
        .append(", menuTimestamp:\"")
        .append(menuTimestamp)
        .append("\", subscribeStatus:")
        .append(subscribeStatus)
        .append("\", activeStatus:")
        .append(activeStatus)
        .append("\", acceptStatus:")
        .append(acceptStatus)
        .append("\", telephone:")
        .append(telephone)
        .append("\", email:")
        .append(email)
        .append("\", zipcode:")
        .append(zipcode)
        .append("\", address:")
        .append(address)
        .append("\", field:")
        .append(field)
        .append(", qrcode:\"")
        .append(qrcode)
        .append("\"}");
        return sb.toString();
    }

    public void checkBasicSanity() throws PAMException {
           Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(uuid) ||
                 TextUtils.isEmpty(name) ||
                 TextUtils.isEmpty(logoUrl) ||
                 // FIXME CMCC Workaround
//                 TextUtils.isEmpty(introduction) ||
                 /* Work around for CMCC server issue */
//                 idtype == Constants.INVALID ||
                 subscribeStatus == Constants.INVALID));
   }

    @Override
    public void checkSanity() throws PAMException {
        checkBasicSanity();

        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                // FIXME CMCC Workaround
                (/* Work around for CMCC server issue *//* company == null || */
                 TextUtils.isEmpty(type) ||
                 updateTime < 0));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (menuType != Constants.MENU_CONF_YES && menuType != Constants.MENU_CONF_NO));
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (menuType == Constants.MENU_CONF_YES && menuTimestamp < 0));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (acceptStatus != Constants.ACCEPT_STATUS_YES &&
                acceptStatus != Constants.ACCEPT_STATUS_NO));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (activeStatus != Constants.ACTIVE_STATUS_CLOSED &&
                 activeStatus != Constants.ACTIVE_STATUS_NORMAL &&
                 activeStatus != Constants.ACTIVE_STATUS_SUSPENDED));
    }

    public MenuInfo menuInfo() {
        if (mMenuInfo == null) {
            mMenuInfo = new MenuInfo();
            mMenuInfo.parseMenuInfoString(menu);
        }
        return mMenuInfo;
    }

    public void loadFullInfoFromCursor(Cursor c) {
        loadBasicInfoFromCursor(c);
        company = c.getString(c.getColumnIndexOrThrow(AccountColumns.COMPANY));
        type = c.getString(c.getColumnIndexOrThrow(AccountColumns.TYPE));
        updateTime = c.getLong(c.getColumnIndexOrThrow(AccountColumns.UPDATE_TIME));
        menuType = c.getInt(c.getColumnIndexOrThrow(AccountColumns.MENU_TYPE));
        menuTimestamp = c.getLong(c.getColumnIndexOrThrow(AccountColumns.MENU_TIMESTAMP));
        activeStatus = c.getInt(c.getColumnIndexOrThrow(AccountColumns.ACTIVE_STATUS));
        acceptStatus = c.getInt(c.getColumnIndexOrThrow(AccountColumns.ACCEPT_STATUS));
        telephone = c.getString(c.getColumnIndexOrThrow(AccountColumns.TELEPHONE));
        email = c.getString(c.getColumnIndexOrThrow(AccountColumns.EMAIL));
        zipcode = c.getString(c.getColumnIndexOrThrow(AccountColumns.ZIPCODE));
        address = c.getString(c.getColumnIndexOrThrow(AccountColumns.ADDRESS));
        field = c.getString(c.getColumnIndexOrThrow(AccountColumns.FIELD));
        qrcode = c.getString(c.getColumnIndexOrThrow(AccountColumns.QRCODE_URL));
        menu = c.getString(c.getColumnIndexOrThrow(AccountColumns.MENU));
        lastMessageId = c.getLong(c.getColumnIndexOrThrow(AccountColumns.LAST_MESSAGE));
    }


    public void loadBasicInfoFromCursor(Cursor c) {
        id = c.getLong(c.getColumnIndexOrThrow(AccountColumns.ID));
        uuid = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
        name = c.getString(c.getColumnIndexOrThrow(AccountColumns.NAME));
        idtype = c.getInt(c.getColumnIndexOrThrow(AccountColumns.ID_TYPE));
        introduction = c.getString(c.getColumnIndexOrThrow(AccountColumns.INTRODUCTION));
        recommendLevel = c.getInt(c.getColumnIndexOrThrow(AccountColumns.RECOMMEND_LEVEL));
        logoId = c.getLong(c.getColumnIndexOrThrow(AccountColumns.LOGO_ID));
        logoUrl = c.getString(c.getColumnIndexOrThrow(AccountColumns.LOGO_URL));
        logoPath = c.getString(c.getColumnIndexOrThrow(AccountColumns.LOGO_PATH));
        subscribeStatus = c.getInt(c.getColumnIndexOrThrow(AccountColumns.SUBSCRIPTION_STATUS));
    }

    public void storeBasicInfoToContentValues(ContentValues cv) {
        if (id != Constants.INVALID) {
            cv.put(AccountColumns.ID, id);
        }
        cv.put(AccountColumns.UUID, uuid);
        cv.put(AccountColumns.NAME, name);
        cv.put(AccountColumns.ID_TYPE, idtype);
        cv.put(AccountColumns.INTRODUCTION, introduction);
        cv.put(AccountColumns.RECOMMEND_LEVEL, recommendLevel);
        cv.put(AccountColumns.LOGO_ID, logoId);
        cv.put(AccountColumns.LOGO_URL, logoUrl);
        cv.put(AccountColumns.LOGO_PATH, logoPath);
        cv.put(AccountColumns.SUBSCRIPTION_STATUS, subscribeStatus);
    }

    public void storeFullInfoToContentValues(ContentValues cv) {
        storeBasicInfoToContentValues(cv);
        cv.put(AccountColumns.COMPANY, company);
        cv.put(AccountColumns.TYPE, type);
        cv.put(AccountColumns.UPDATE_TIME, updateTime);
        cv.put(AccountColumns.MENU_TYPE, menuType);
        cv.put(AccountColumns.MENU_TIMESTAMP, menuTimestamp);
        cv.put(AccountColumns.ACTIVE_STATUS, activeStatus);
        cv.put(AccountColumns.ACCEPT_STATUS, acceptStatus);
        cv.put(AccountColumns.TELEPHONE, telephone);
        cv.put(AccountColumns.EMAIL, email);
        cv.put(AccountColumns.ZIPCODE, zipcode);
        cv.put(AccountColumns.ADDRESS, address);
        cv.put(AccountColumns.FIELD, field);
        cv.put(AccountColumns.QRCODE_URL, qrcode);
        cv.put(AccountColumns.MENU, menu);
        cv.put(AccountColumns.LAST_MESSAGE, lastMessageId);
    }

    public ContentValues storeBasicInfoToContentValues() {
        ContentValues cv = new ContentValues();
        storeBasicInfoToContentValues(cv);
        return cv;
    }

    public ContentValues storeFullInfoToContentValues() {
        ContentValues cv = new ContentValues();
        storeFullInfoToContentValues(cv);
        return cv;
    }

    public static long queryAccountId(Context context, String uuid, boolean subscribedOnly) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = null;
        long result = Constants.INVALID;
        try {
            if (subscribedOnly) {
                c = cr.query(
                        AccountColumns.CONTENT_URI,
                        new String[]{AccountColumns.ID},
                        AccountColumns.UUID + "=? AND " + AccountColumns.SUBSCRIPTION_STATUS + "=?",
                        new String[]{uuid, Integer.toString(Constants.SUBSCRIPTION_STATUS_YES)},
                        null);
            } else {
                c = cr.query(
                        AccountColumns.CONTENT_URI,
                        new String[]{AccountColumns.ID},
                        AccountColumns.UUID + "=?",
                        new String[]{uuid},
                        null);
            }
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = c.getLong(c.getColumnIndexOrThrow(AccountColumns.ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    public static String queryAccountUuid(Context context, long accountId) {
        String result = null;
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    AccountColumns.CONTENT_URI,
                    new String[] {AccountColumns.UUID},
                    AccountColumns.ID + "=?",
                    new String[]{Long.toString(accountId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = c.getString(c.getColumnIndexOrThrow(AccountColumns.UUID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return result;
    }

    /**
     * Insert a row into media table if the URL does not exist.
     * @param cr
     * @param logoUrl
     * @return ID of this row
     */
    public static long insertLogoUrl(ContentResolver cr, String logoUrl) {
        Cursor c = null;
        ContentValues mediaValues = new ContentValues();
        long mediaId = Constants.INVALID;

        try {
            c = cr.query(
                    MediaColumns.CONTENT_URI,
                    new String[]{
                            MediaColumns.ID,
                            MediaColumns.URL,
                            MediaColumns.REF_COUNT},
                    MediaColumns.URL + "=?",
                    new String[] {logoUrl},
                    null);
            if (c == null || c.getCount() == 0) {
                mediaValues.put(MediaColumns.TYPE, Constants.MEDIA_TYPE_PICTURE);
                mediaValues.put(MediaColumns.TIMESTAMP, Utils.currentTimestamp());
                mediaValues.put(MediaColumns.URL, logoUrl);
                mediaValues.put(MediaColumns.REF_COUNT, 1);
                Uri uri = cr.insert(MediaColumns.CONTENT_URI, mediaValues);
                mediaId = Long.parseLong(uri.getLastPathSegment());
            } else {
                c.moveToFirst();
                mediaId = c.getLong(c.getColumnIndexOrThrow(MediaColumns.ID));
                int count = c.getInt(c.getColumnIndexOrThrow(MediaColumns.REF_COUNT));
                count ++;
                mediaValues.put(MediaColumns.REF_COUNT, count);
                int result = cr.update(
                    MediaColumns.CONTENT_URI,
                    mediaValues,
                    MediaColumns.URL + "=?",
                    new String[] {logoUrl});
                //Ruoyao: Add log
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return mediaId;
    }

    public long insertOrUpdateAccount(ContentResolver cr, boolean fullInfo) {
        Cursor c = null;
        ContentValues cv = null;
        try {
            c = cr.query(
                    AccountColumns.CONTENT_URI,
                    new String[]{AccountColumns.ID},
                    AccountColumns.UUID + "=?",
                    new String[]{uuid},
                    null);
            if (c == null || c.getCount() == 0) {
                // insert
                long logoId = PublicAccount.insertLogoUrl(cr, logoUrl);
                if (fullInfo) {
                    cv = storeFullInfoToContentValues();
                } else {
                    cv = storeBasicInfoToContentValues();
                }
                cv.remove(AccountColumns.LOGO_URL);
                cv.remove(AccountColumns.LOGO_PATH);
                cv.put(AccountColumns.LOGO_ID, logoId);
                Uri uri = cr.insert(AccountColumns.CONTENT_URI, cv);
                id = Long.parseLong(uri.getLastPathSegment());
            } else {
                // update
                c.moveToFirst();
                id = c.getLong(c.getColumnIndexOrThrow(AccountColumns.ID));
                long logoId = getLogoId(cr, logoUrl);
                if (logoId == Constants.INVALID) {
                    logoId = PublicAccount.insertLogoUrl(cr, logoUrl);
                }
                if (fullInfo) {
                    cv = storeFullInfoToContentValues();
                } else {
                    cv = storeBasicInfoToContentValues();
                }
                cv.put(AccountColumns.LOGO_ID, logoId);
                cv.remove(AccountColumns.LOGO_URL);
                cv.remove(AccountColumns.LOGO_PATH);
                cv.remove(AccountColumns.LAST_MESSAGE);
                cr.update(
                        AccountColumns.CONTENT_URI,
                        cv,
                        AccountColumns.ID + "=?",
                        new String[]{Long.toString(id)});
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        Log.d(TAG, "insertOrUpdateAccount() accountId=" + id);

        return id;
    }

    private long getLogoId(ContentResolver cr, String logoUrl) {
        long logoId = Constants.INVALID;
        Cursor c = null;
        try {
            c = cr.query(
                    MediaColumns.CONTENT_URI,
                    new String[]{ MediaColumns.ID },
                    MediaColumns.URL + "=? AND " + MediaColumns.TYPE + "=?",
                    new String[]{
                        logoUrl,
                        Integer.toString(Constants.MEDIA_TYPE_PICTURE)
                    },
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                logoId = c.getLong(c.getColumnIndexOrThrow(MediaColumns.ID));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return logoId;
    }
}
