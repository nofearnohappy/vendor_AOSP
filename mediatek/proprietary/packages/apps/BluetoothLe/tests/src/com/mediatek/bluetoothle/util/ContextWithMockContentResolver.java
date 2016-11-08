package com.mediatek.bluetoothle.util;

import android.content.ContentResolver;
import android.content.Context;
import android.test.RenamingDelegatingContext;
import android.util.Log;

// Mocked Content Resolver
public class ContextWithMockContentResolver extends RenamingDelegatingContext {

    private static final String TAG = "ContextWithMockContentResolver";

    private ContentResolver mContentResolver;

    public ContextWithMockContentResolver(final Context targetContext) {
        super(targetContext, "test");
    }

    public void setContentResolver(final ContentResolver contentResolver) {
        Log.d(TAG, "setContentResolver:" + contentResolver);
        this.mContentResolver = contentResolver;
    }

    @Override
    public ContentResolver getContentResolver() {
        Log.d(TAG, "getContentResolver:" + mContentResolver);
        return mContentResolver;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    } // Added in-case my class called getApplicationContext()
}