package com.mediatek.mms.ext;

import java.util.ArrayList;

import com.google.android.mms.pdu.PduPart;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mediatek.mms.callback.IMuteCacheCallback;

public interface IOpMessageUtilsExt {
    /**
     * @internal
     */
    void checkThreadMuteTimeout(Uri uri, IMuteCacheCallback muteCache);

    /**
     * @internal
     */
    public String formatDateAndTimeStampString(String dateStr, Context context, long msgDate,
            long msgDateSent, boolean fullFormat);

    /**
     * @internal
     */
    public String formatTimeStampString(Context context, long time, int formatFlags);

    /**
     * @internal
     */
    public String formatTimeStampStringExtend(Context context, long time, int formatFlags);

    /**
     * @internal
     */
    public CharSequence[] getVisualTextName(CharSequence[] visualNames, Context context,
            boolean isSaveLocationChoices);

    /**
     * @internal
     */
    public void setExtendedAudioType(ArrayList<String> audioType);

    /**
     * @internal
     */
    public boolean allowSafeDraft(final Activity activity, boolean deviceStorageIsFull,
            boolean isNofityUser, int toastType);

    /**
     * @internal
     */
    public Uri startDeleteAll(Uri uri);

    /**
     * @internal
     */
    boolean isSupportedFile(final String contentType);
    /**
     * @internal
     */
    boolean isSupportedFile(final PduPart part);
}
