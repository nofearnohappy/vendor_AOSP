package com.mediatek.rcs.pam;

import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;

public class MediaFolder {
    private static final String TAG = "PAM/MediaFolder";

    public static final String PICTURE_DIR_NAME = "picture";
    public static final String AUDIO_DIR_NAME = "audio";
    public static final String VIDEO_DIR_NAME = "video";
    public static final String GEOLOC_DIR_NAME = "geoloc";
    public static final String VCARD_DIR_NAME = "vcard";
    public static final String TEMP_PREFIX = "media_";
    public static final String ROOT_DIR = ".rcspa";

    private static final int MAX_FILE_SEQUENCE = 100;

    private static File sPictureMediaDir = null;
    private static File sAudioMediaDir = null;
    private static File sVideoMediaDir = null;
    private static File sGeoLocMediaDir = null;
    private static File sVcardMediaDir = null;

    public static File getRootDir() {
        File result = Environment.getExternalStoragePublicDirectory(ROOT_DIR);
        if (!result.isDirectory()) {
            if (result.exists()) {
                result.delete();
            }
            result.mkdir();
        }
        return result;
    }

    public static synchronized File getMediaDir(int type) {
        File root = getRootDir();
        switch(type) {
        case Constants.MEDIA_TYPE_PICTURE:
            if (sPictureMediaDir == null) {
                sPictureMediaDir = new File(
                        root.getAbsolutePath() + File.separator + MediaFolder.PICTURE_DIR_NAME);
                if (!sPictureMediaDir.exists()) {
                    sPictureMediaDir.mkdir();
                }
            }
            return sPictureMediaDir;
        case Constants.MEDIA_TYPE_AUDIO:
            if (sAudioMediaDir == null) {
                sAudioMediaDir = new File(
                        root.getAbsolutePath() + File.separator + MediaFolder.AUDIO_DIR_NAME);
                if (!sAudioMediaDir.exists()) {
                    sAudioMediaDir.mkdir();
                }
            }
            return sAudioMediaDir;
        case Constants.MEDIA_TYPE_VIDEO:
            if (sVideoMediaDir == null) {
                sVideoMediaDir = new File(
                        root.getAbsolutePath() + File.separator + MediaFolder.VIDEO_DIR_NAME);
                if (!sVideoMediaDir.exists()) {
                    sVideoMediaDir.mkdir();
                }
            }
            return sVideoMediaDir;
        case Constants.MEDIA_TYPE_GEOLOC:
            if (sGeoLocMediaDir == null) {
                sGeoLocMediaDir = new File(
                        root.getAbsolutePath() + File.separator + MediaFolder.GEOLOC_DIR_NAME);
                if (!sGeoLocMediaDir.exists()) {
                    sGeoLocMediaDir.mkdir();
                }
            }
            return sGeoLocMediaDir;
        case Constants.MEDIA_TYPE_VCARD:
            if (sVcardMediaDir == null) {
                sVcardMediaDir = new File(
                        root.getAbsolutePath() + File.separator + MediaFolder.VCARD_DIR_NAME);
                if (!sVcardMediaDir.exists()) {
                    sVcardMediaDir.mkdir();
                }
            }
            return sVcardMediaDir;
        default:
            throw new Error("Invalid type: " + type);
        }
    }


    /**
     *
     * @param mediaId The mediaId of the media or Constant.INVALID for tempfile.
     * @param type Media type.
     * @param extName Should contain dot ("."), for example, ".jpg"
     * @return
     */
    public static String generateMediaFileName(long mediaId, int type, String extension) {
        if (mediaId == Constants.INVALID) {
            long timestamp = System.currentTimeMillis();
            File f = new File(getMediaDir(type).getAbsolutePath()
                    + File.separator + TEMP_PREFIX + timestamp + extension);
            if (f.exists()) {
                int i = 0;
                for (; i < MAX_FILE_SEQUENCE; ++i) {
                    f = new File(
                            getMediaDir(type).getAbsolutePath() +
                            File.separator + TEMP_PREFIX +
                            timestamp + "_" + i + extension);
                    if (!f.exists()) {
                        break;
                    }
                }
                if (i == MAX_FILE_SEQUENCE) {
                    Log.e(TAG, "MAX_FILE_SEQUENCE reached." +
                            " Cannot find a temp filename in reasonable time.");
                    return null;
                } else {
                    return f.getAbsolutePath();
                }
            }
            return f.getAbsolutePath();
        } else if (mediaId < 0) {
            throw new Error("Invalid mediaId: " + mediaId);
        } else {
            return getMediaDir(type).getAbsolutePath() + File.separator + mediaId + extension;
        }
    }

    public static String extractExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return (index == -1) ? null : filename.substring(index, filename.length());
    }

    private static MimeTypeMap sMimeTypeMap = MimeTypeMap.getSingleton();

    public static String getExtensionFromMimeType(String mimeType) {
        return "." + sMimeTypeMap.getExtensionFromMimeType(mimeType);
    }
}
