package com.mediatek.mms.plugin;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.mms.pdu.PduPart;

import com.mediatek.mms.callback.IMuteCacheCallback;
import com.mediatek.mms.ext.DefaultOpMessageUtilsExt;


/**
 * Op01MessageUtilsExt.
 *
 */
public class Op01MessageUtilsExt extends DefaultOpMessageUtilsExt {
    private static final String TAG = "Mms/Op01MessageUtilsExt";

    /**
     * Construction.
     * @param context Context
     */
    public Op01MessageUtilsExt(Context context) {
        super(context);
    }

    @Override
    public void checkThreadMuteTimeout(Uri uri, IMuteCacheCallback muteCache) {
        long threadId = Long.parseLong(uri.getLastPathSegment());
        muteCache.setMuteCacheCallback(threadId, 0, 0, true);
    }

    @Override
    public Uri startDeleteAll(Uri uri) {
        Log.d(TAG, "startDeleteAll uri groupDeleteParts: yes");
        return uri.buildUpon().appendQueryParameter("groupDeleteParts", "yes").build();
    }

    @Override
    public boolean isSupportedFile(String contentType) {
        return Op01MmsUtils.isSupportedFile(contentType);
    }

    @Override
    public boolean isSupportedFile(PduPart part) {
        return Op01MmsUtils.isOtherAttachment(part);
    }
}
