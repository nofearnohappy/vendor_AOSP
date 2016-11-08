package com.mediatek.nmsg.util;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class NmsgProvider extends ContentProvider {

    private static final int FUNC_ID_INSERT = 1;
    private static final int FUNC_ID_DELETE = 2;
    private static final int FUNC_ID_UPDATE = 3;
    private static final int FUNC_ID_GET_OR_CREATE_THREADID = 4;
    private static final int FUNC_ID_SEND_MESSAGE = 5;

    private final String LOG_TAG = "NmsgProvider";
    /// M: add for ALPS01749707 for ipmessage plugin
    private final String NMSG_PACKAGE_NAME = "com.hesine.nmsg";

    private ContentResolver mContentResolver = null;

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (TextUtils.isEmpty(method)) {
            Log.e(LOG_TAG, "NmsgProvider call get method is null or empty");
            return null;
        }

        if (!NMSG_PACKAGE_NAME.equalsIgnoreCase(this.getCallingPackage())) {
            Log.e(LOG_TAG, "NmsgProvider call get unknown package name: " + this.getCallingPackage()) ;
            return null ;
        }

        Bundle retBundle = new Bundle();

        Log.d(LOG_TAG, "NmsgProvider call method: " + method);

        switch (Integer.parseInt(method)) {

        case FUNC_ID_INSERT:
            retBundle.putParcelable(method, handleInsertCall(method, extras));
            break;

        case FUNC_ID_DELETE:
            retBundle.putInt(method, handleDeleteCall(method, extras));
            break;

        case FUNC_ID_UPDATE:
            retBundle.putInt(method, handleUpdateCall(method, extras));
            break;

        case FUNC_ID_GET_OR_CREATE_THREADID:
            retBundle.putLong(method, handleGetOrCreateThreadIdCall(method, extras));
            break;

        case FUNC_ID_SEND_MESSAGE:
            retBundle.putInt(method, handleSendMessage(extras));
            break;

        default:
            Log.e(LOG_TAG, "unhandle method: " + method);
            break;
        }

        return retBundle;
    }

    private Uri handleInsertCall(String method, Bundle extras) {
        try {
            return mContentResolver.insert((Uri) extras.getParcelable(method + 1),
                    (ContentValues) extras.getParcelable(method + 2));
        } catch (Exception e) {
            Log.e(LOG_TAG, "handleInsertCall got execption" + e.toString());
            return null;
        }
    }

    private int handleDeleteCall(String method, Bundle extras) {

        try {
            return mContentResolver.delete((Uri) extras.getParcelable(method + 1),
                    extras.getString(method + 2), extras.getStringArray(method + 3));
        } catch (Exception e) {
            Log.e(LOG_TAG, "handleDeleteCall got execption" + e.toString());
            return 0;
        }
    }

    private int handleUpdateCall(String method, Bundle extras) {
        try {
            return mContentResolver.update((Uri) extras.getParcelable(method + 1),
                    (ContentValues) extras.getParcelable(method + 2), extras.getString(method + 3),
                    extras.getStringArray(method + 4));
        } catch (Exception e) {
            Log.e(LOG_TAG, "handleUpdateCall got execption" + e.toString());
            return 0;
        }
    }

    private long handleGetOrCreateThreadIdCall(String method, Bundle extras) {
        Cursor cursor = null;
        try {
            String[] proj = { "_id" };
            cursor = mContentResolver.query((Uri) extras.getParcelable(method + 1), proj, null,
                    null, null);
            Log.d(LOG_TAG, "handleGetOrCreateThreadIdCall cursor cnt: " + cursor.getCount());
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    long ret = cursor.getLong(0);
                    cursor.close();
                    return ret;
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "handleGetOrCreateThreadIdCall got execption" + e.toString());
        }

        if (cursor != null)
            cursor.close();

        Log.e(LOG_TAG, "handleGetOrCreateThreadIdCall, Unable to find or create a thread ID.");
        return -1;
    }

    private int handleSendMessage(Bundle extras) {
        if (extras != null) {
            /*
            ArrayList<PendingIntent> sentIntents = extras.getParcelableArrayList("sentIntents");
            ArrayList<PendingIntent> deliveryIntents = extras.getParcelableArrayList("deliveryIntents");
            EncapsulatedSmsManager.sendMultipartTextMessageWithEncodingTypeGemini(extras
                            .getString("destAddr"), extras.getString("scAddr"),
                            extras.getStringArrayList("parts"), extras
                                    .getInt("encodingType"), extras
                                    .getInt("slotId"), sentIntents,
                            deliveryIntents);
            //please using
             SmsManager.getDefault().sendMultipartTextMessageWithEncodingType(
                     140destAddr, scAddr, parts, encodingType, sentIntents, deliveryIntents, slotId);*/
        }
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        Log.d(LOG_TAG, "NmsgProvider got package name: " + getContext().getPackageName());
        mContentResolver = getContext().getContentResolver();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

}
