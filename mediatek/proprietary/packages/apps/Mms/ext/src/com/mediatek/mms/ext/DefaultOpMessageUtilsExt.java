package com.mediatek.mms.ext;

import java.util.ArrayList;

import com.google.android.mms.pdu.PduPart;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mediatek.mms.callback.IMuteCacheCallback;

public class DefaultOpMessageUtilsExt extends ContextWrapper implements
        IOpMessageUtilsExt {
    private static final String TAG = "Mms/DefaultOpMessageUtilsExt";

    public DefaultOpMessageUtilsExt(Context base) {
        super(base);
    }

    @Override
    public String formatDateAndTimeStampString(String dateStr, Context context, long msgDate,
            long msgDateSent, boolean fullFormat) {
        return dateStr;
    }

    @Override
    public String formatTimeStampString(Context context, long time, int formatFlags) {
        return null;
    }

    @Override
    public String formatTimeStampStringExtend(Context context, long time, int formatFlags) {
        return null;
    }

    @Override
    public CharSequence[] getVisualTextName(CharSequence[] visualNames, Context context,
            boolean isSaveLocationChoices) {
        return visualNames;
    }

    @Override
    public void setExtendedAudioType(ArrayList<String> audioType) {
    }

    @Override
    public void checkThreadMuteTimeout(Uri uri, IMuteCacheCallback muteCache) {
    }

    @Override
    public boolean allowSafeDraft(final Activity activity, boolean deviceStorageIsFull,
            boolean isNofityUser, int toastType) {
        return true;
    }

    @Override
    public Uri startDeleteAll(Uri uri) {
        return uri;
    }

    @Override
    public boolean isSupportedFile(String contentType) {
        return false;
    }

    @Override
    public boolean isSupportedFile(PduPart part) {
        return false;
    }

}
