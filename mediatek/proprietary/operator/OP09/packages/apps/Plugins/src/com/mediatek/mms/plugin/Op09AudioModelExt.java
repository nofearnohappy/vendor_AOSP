package com.mediatek.mms.plugin;

import com.google.android.mms.ContentType;
import com.mediatek.mms.ext.DefaultOpAudioModelExt;

/**
 * Op09AudioModelExt.
 *
 */
public class Op09AudioModelExt extends DefaultOpAudioModelExt {
    @Override
    public String initModelFromFileUri(String contentType, String extension) {
        if (contentType == null && extension.equalsIgnoreCase("3ga")) {
            return ContentType.AUDIO_3GPP;
        }
        return null;
    }
}
