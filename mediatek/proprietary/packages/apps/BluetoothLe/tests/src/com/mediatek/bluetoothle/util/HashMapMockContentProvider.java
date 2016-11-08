package com.mediatek.bluetoothle.util;

import android.database.Cursor;
import android.net.Uri;
import android.test.mock.MockContentProvider;
import android.util.Log;

import java.util.HashMap;

// Mocked Content Provider
public class HashMapMockContentProvider extends MockContentProvider {

    private static final String TAG = "HashMapMockContentProvider";

    private final HashMap<Uri, Cursor> mExpectedResults = new HashMap<Uri, Cursor>();

    public void addQueryResult(final Uri uriIn, final Cursor expectedResult) {
        Log.d(TAG, "expectedResults for:" + uriIn);
        mExpectedResults.put(uriIn, expectedResult);
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        Log.d(TAG, "Query:" + uri);
        return mExpectedResults.get(uri);
    }
}