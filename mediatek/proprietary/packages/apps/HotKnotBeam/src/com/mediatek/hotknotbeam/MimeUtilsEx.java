package com.mediatek.hotknotbeam;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.mediatek.hotknotbeam.HotKnotBeamConstants.HotKnotFileType;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

public final class MimeUtilsEx {
    static protected final String TAG = "MimeUtilsEx";

    private static final String DATA_COLUMN = "_data";

    private static final Map<String, String> mimeTypeToExtensionMap = new HashMap<String, String>();

    private static final Map<String, String> extensionToMimeTypeMap = new HashMap<String, String>();

    static {
        // Note that this list is _not_ in alphabetical order and must not be sorted.
        // The "most popular" extension must come first, so that it's the one returned
        // by guessExtensionFromMimeType.

        add("image/mpo", "mpo");
    }

    static HotKnotFileType getFileType(String filename) {
        HotKnotFileType fileType = HotKnotFileType.COMMON;
        int pos = filename.lastIndexOf('.');

        if (pos != -1) {
            String ext = filename.substring(pos + 1);

            if (ext.equalsIgnoreCase("png")) {
                fileType = HotKnotFileType.IMAGE;
            } else if (ext.equalsIgnoreCase("jpg")) {
                fileType = HotKnotFileType.IMAGE;
            } else if (ext.equalsIgnoreCase("bmp")) {
                fileType = HotKnotFileType.IMAGE;
            } else if (ext.equalsIgnoreCase("jpe")) {
                fileType = HotKnotFileType.IMAGE;
            } else if (ext.equalsIgnoreCase("jpeg")) {
                fileType = HotKnotFileType.IMAGE;
            } else if (ext.equalsIgnoreCase("gif")) {
                fileType = HotKnotFileType.IMAGE;
            } else if (ext.equalsIgnoreCase("avi")) {
                fileType = HotKnotFileType.VIDEO;
            } else if (ext.equalsIgnoreCase("mpo")) {
                fileType = HotKnotFileType.VIDEO;
            } else if (ext.equalsIgnoreCase("3gp")) {
                fileType = HotKnotFileType.VIDEO;
            } else if (ext.equalsIgnoreCase("3gpp")) {
                fileType = HotKnotFileType.VIDEO;
            } else if (ext.equalsIgnoreCase("ts")) {
                fileType = HotKnotFileType.VIDEO;
            } else if (ext.equalsIgnoreCase("mp3")) {
                fileType = HotKnotFileType.MUSIC;
            } else if (ext.equalsIgnoreCase("wav")) {
                fileType = HotKnotFileType.MUSIC;
            } else if (ext.equalsIgnoreCase("aac")) {
                fileType = HotKnotFileType.MUSIC;
            } else if (ext.equalsIgnoreCase("amr")) {
                fileType = HotKnotFileType.MUSIC;
            } else if (ext.equalsIgnoreCase("mid")) {
                fileType = HotKnotFileType.MUSIC;
            } else if (ext.equalsIgnoreCase("midi")) {
                fileType = HotKnotFileType.MUSIC;
            }
        }

        return fileType;
    }

    public static boolean isGallerySupport(String filename) {
        boolean ret = false;
        int pos = filename.lastIndexOf('.');

        if (pos != -1) {
            String ext = filename.substring(pos + 1);

            if (ext.equalsIgnoreCase("png")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("jpg")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("jpe")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("jpeg")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("gif")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("avi")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("mpo")) {
                ret = true;
            } else if (ext.equalsIgnoreCase("3gp")) {
                ret = true;
            }
        }

        return ret;
    }

    private static void add(String mimeType, String extension) {
        //
        // if we have an existing x --> y mapping, we do not want to
        // override it with another mapping x --> ?
        // this is mostly because of the way the mime-type map below
        // is constructed (if a mime type maps to several extensions
        // the first extension is considered the most popular and is
        // added first; we do not want to overwrite it later).
        //
        if (!mimeTypeToExtensionMap.containsKey(mimeType)) {
            mimeTypeToExtensionMap.put(mimeType, extension);
        }

        extensionToMimeTypeMap.put(extension, mimeType);
    }

    /**
     * Returns the MIME type for the given extension.
     * @param extension A file extension without the leading '.'
     * @return The MIME type for the given extension or null iff there is none.
     */
    public static String guessMimeTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }

        return extensionToMimeTypeMap.get(extension);
    }

    //http://blog.csdn.net/huangyanan1989/article/details/17263203
    public static File getFilePathFromUri(Uri uri, Context context) {
        File inputFile = null;
        String filePath = "";

        if (uri == null) {
            Log.e(TAG, "File Uri must not be null");
            throw new IllegalArgumentException("File Uri must not be null");
        }

        String scheme = uri.getScheme();

        try {
           if (DocumentsContract.isDocumentUri(context, uri)) {
               // ExternalStorageProvider
               if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                       filePath = Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
               } else if (isDownloadsDocument(uri)) {
                   final String id = DocumentsContract.getDocumentId(uri);
                   final Uri contentUri = ContentUris.withAppendedId(
                       Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    filePath = getDataColumn(context, contentUri, null, DATA_COLUMN, null);
               } else if (isHotKnotDocument(uri)) {
                   final String docId = DocumentsContract.getDocumentId(uri);
                   final String[] split = docId.split(":");
                   Log.d(TAG, "docId:" + docId);
                   if (split.length == 2) {
                       filePath = split[0] + "/" + split[1];
                   }
               } else if (isMediaDocument(uri)) {
                   final String docId = DocumentsContract.getDocumentId(uri);
                   final String[] split = docId.split(":");
                   final String type = split[0];
                   Uri contentUri = null;
                   if ("image".equals(type)) {
                       contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                   } else if ("video".equals(type)) {
                       contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                   } else if ("audio".equals(type)) {
                       contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                   }
                   final String selection = "_id=?";
                   final String[] selectionArgs = new String[] {
                       split[1]
                   };
                    filePath = getDataColumn(context,
                            contentUri, selection, DATA_COLUMN, selectionArgs);
               }
           } else if (scheme != null && scheme.equalsIgnoreCase("content")) {
                filePath = getDataColumn(context, uri, null, DATA_COLUMN, null);
           } else if (scheme != null && scheme.equalsIgnoreCase("file")) {
               filePath = uri.getPath();
           }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "ex:" + e);
            return null;
        } catch (SecurityException se) {
            Log.e(TAG, "se:" + se);
            return null;
        }

        if (filePath == null || filePath.length() == 0) {
            Log.e(TAG, "File path is empty");
            return null;
        }

        Log.d(TAG, "The sending path is " + filePath);
        inputFile = new File(filePath);

        if (!inputFile.exists() || inputFile.isDirectory()) {
            if (scheme.equalsIgnoreCase("file")) {
                filePath = "/" + uri.getHost() + uri.getPath();
                inputFile = new File(filePath);
                if (inputFile.exists()) {
                    return inputFile;
                }
            }
            Log.e(TAG, "File is not existed or inputFile is a directory");
            return null;
        }

        return inputFile;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    static boolean isDownloadsDocument(Uri uri) {
       return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    static boolean isHotKnotDocument(Uri uri) {
        return "com.mediatek.hotknotbeam.documents".equals(uri.getAuthority());
    }

    static String getDataColumn(Context context, Uri uri, String selection,
        String column, String[] selectionArgs) {
        Cursor cursor = null;
        final String[] projection = {
             column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection,
                selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.e(TAG, "Cursor is null or other");

        return buildSrcFromPath(uri);
    }

    static int getIntColumn(Context context, Uri uri, String selection,
        String column, String[] selectionArgs) {
        Cursor cursor = null;
        final String[] projection = {
             column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection,
                selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getInt(columnIndex);
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.e(TAG, "Cursor is null or other");
        return -1;
    }

    private static String buildSrcFromPath(Uri uri) {
        String src = null;
        try {
            String path = uri.getPath();
            src = path.substring(path.lastIndexOf('/') + 1);
            if (src.startsWith(".") && src.length() > 1) {
               src = src.substring(1);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "err:" + e);
        } catch (NullPointerException e) {
            Log.e(TAG, "err:" + e);
        }
        return src;
    }

    static String getSaveRootPath(Context context, String folder) {
        String rootPath = StorageManagerEx.getDefaultPath() + File.separator + folder;
        final int myUser = UserManager.get(context).getUserHandle();

        if (UserHandle.USER_OWNER != myUser) {
            final StorageManager storageManager = StorageManager.from(context);
            final StorageVolume[] volumes = storageManager.getVolumeList();
            if (volumes != null) {
                    rootPath = volumes[0].getPath()
                    + File.separator + HotKnotBeamConstants.MAX_HOTKNOT_BEAM_FOLDER;
            }
        }

        Log.d(TAG, "The save path:" + rootPath);
        return rootPath;
    }

    static void startRxUiActivity(Context context, int id) {
        Log.d(TAG, "Start Rx Ui activity");
        Intent intent = new Intent();
        intent.putExtra(DownloadInfo.EXTRA_ITEM_ID, id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setClass(context, HotKnotBeamRxActivity.class);
        context.startActivity(intent);
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Contact data.
     */
    static boolean isContactUri(Uri uri) {
        String mimeType = uri.getQueryParameter(HotKnotBeamConstants.QUERY_MIMETYPE);
        if (mimeType != null && mimeType.equals(HotKnotBeamConstants.MIME_TYPE_VCARD)) {
            return true;
        } else if (uri.toString().indexOf("contacts/as_multi_vcard") != -1) {
            return true;
        } else if (uri.toString().indexOf("contacts/as_vcard") != -1) {
            return true;
        } else if (uri.toString().indexOf("profile/as_vcard") != -1) {
            return true;
        }
        return false;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Email attachment.
     */
    static boolean isEmailUri(Uri uri) {
        return "com.android.email.attachmentprovider".equals(uri.getAuthority());
    }

    /**
     * @param context application context.
     * @param uri The Uri to check.
     * @return file name of uri.
     */
    static String getEmailFilename(Context context, Uri uri) {
        return getDataColumn(context, uri, null, Images.Media.DISPLAY_NAME, null);
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is raw data.
     */
    static boolean isRawUri(Uri uri) {
        String authority = uri.getAuthority();
        Log.d(TAG, "uri:" + uri);
        if (authority == null) {
            authority = uri.getEncodedAuthority();
        }
        Log.d(TAG, "authority:" + authority);
        return "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority()) ||
                "com.android.email.attachmentprovider".equals(uri.getAuthority());
    }

    /**
     * @param context application context.
     * @param uri The Uri to check.
     * @return file name of uri.
     */
    static String getRawFilename(Context context, Uri uri) {
        return getDataColumn(context, uri, null, Images.Media.DISPLAY_NAME, null);
    }

    /**
     * @param context application context.
     * @param uri The Uri to check.
     * @return file size of uri.
     */
    static int getRawFilesize(Context context, Uri uri) {
        return getIntColumn(context, uri, null, Images.Media.SIZE, null);
    }

}