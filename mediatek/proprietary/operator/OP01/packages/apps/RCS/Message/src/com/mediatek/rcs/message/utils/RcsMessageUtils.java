package com.mediatek.rcs.message.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.rcs.message.R;
import com.mediatek.storage.StorageManagerEx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;

import com.mediatek.rcs.message.data.RcsProfile;

import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageStatus;
import com.mediatek.rcs.common.IpMessageConsts.IpMessageType;
import com.mediatek.rcs.common.IpAttachMessage;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpTextMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;

import android.graphics.Rect;
import android.media.ExifInterface;

import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import java.lang.reflect.Method;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

/**
 * RcsMessageUtils
 *
 */
public class RcsMessageUtils {

    public static final String TAG = "RcsMessageUtils";
    public static final int UNCONSTRAINED = -1;
    public static final String IP_MESSAGE_FILE_PATH = File.separator + ".Rcse" + File.separator;
    public static final String CACHE_PATH = File.separator + ".Rcse" + "/Cache/";

    private static final String PREFERENCE_NETWORK_CHANGED_NOTIFY = "network_changed_notify_pre";
    private static final String PREFERENCE_NETWORK_CHANGED_SMS_KEY = "network_changed_notify_sms_";
    private static final String PREFERENCE_NETWORK_CHANGED_RCS_KEY = "network_changed_notify_rcs_";
    private static final String PREFERENCE_SUBSCRIBE_OFFLINE_GROUP_INFO_TIME = "subcribe_groupinfo";
    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get(
            "ro.mtk_ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get(
            "ro.mtk_volte_support").equals("1");
    public static final boolean MTK_ENHANCE_VOLTE_CONF_CALL = true;

    public static int getVideoCaptureDurationLimit() {
        return 90;
    }


    /**
     * blockingGetGroupChatIdByThread. This action need to query database, it's better call it in
     * sub thread.
     * @param context Context
     * @param threadId thread id
     * @return return group chat id if the thread is group thread, else return null
     */
    public static String blockingGetGroupChatIdByThread(Context context, long threadId) {
        String chatId = null;
        Uri uri = RcsLog.ThreadsColumn.CONTENT_URI;
        String[] projection = new String[] {RcsLog.ThreadsColumn.FLAG,
                                          RcsLog.ThreadsColumn.RECIPIENTS};
        String selection = "rcs_threads._id=" + threadId;
        Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                                                            null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int chatFlag = cursor.getInt(0);
                    if (chatFlag == RcsLog.ThreadFlag.MTM) {
                        chatId = cursor.getString(1);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        Log.d(TAG, "blockingGetGroupChatIdByThread: chatId = " + chatId);
        return chatId;
    }

    public static Drawable getGroupDrawable(long threadId) {
        return ContextCacher.getPluginContext().getResources()
                .getDrawable(R.drawable.group_example);
    }

    public static boolean getSDCardStatus() {
        boolean ret = false;
        String sdStatus = Environment.getExternalStorageState();
        Log.d(TAG, "getSDCardStatus(): sdStatus = " + sdStatus);
        if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
            ret = true;
        }
        return ret;
    }

    public static String getSDCardPath(Context c) {
        File sdDir = null;
        String sdStatus = Environment.getExternalStorageState();

        if (TextUtils.isEmpty(sdStatus)) {
            return c.getFilesDir().getAbsolutePath();
        }

        boolean sdCardExist = sdStatus.equals(android.os.Environment.MEDIA_MOUNTED);

        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }
        return c.getFilesDir().getAbsolutePath();
    }

    public static File getStorageFile(String filename) {
        String dir = "";
        String path = StorageManagerEx.getDefaultPath();
        if (path == null) {
            Log.e(TAG, "default path is null");
            return null;
        }
        dir = path + "/" + Environment.DIRECTORY_DOWNLOADS + "/";
        Log.i(TAG, "copyfile,  file full path is " + dir + filename);
        File file = getUniqueDestination(dir + filename);

        // make sure the path is valid and directories created for this file.
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            Log.i(TAG, "[RCS] copyFile: mkdirs for " + parentFile.getPath());
            parentFile.mkdirs();
        }
        return file;
    }

    public static File getUniqueDestination(String fileName) {
        File file;
        final int index = fileName.indexOf(".");
        if (index > 0) {
            final String extension = fileName.substring(index + 1, fileName.length());
            final String base = fileName.substring(0, index);
            file = new File(base + "." + extension);
            for (int i = 2; file.exists(); i++) {
                file = new File(base + "_" + i + "." + extension);
            }
        } else {
            file = new File(fileName);
            for (int i = 2; file.exists(); i++) {
                file = new File(fileName + "_" + i);
            }
        }
        return file;
    }

    public static String getUniqueFileName(String fileName) {
        String mName = fileName;
        File file;
        final int index = fileName.lastIndexOf(".");
        if (index > 0) {
            final String extension = fileName.substring(index + 1, fileName.length());
            final String base = fileName.substring(0, index);
            file = new File(base + "." + extension);
            for (int i = 1; file.exists(); i++) {
                mName = base + "(" + i + ")." + extension;
                file = new File(mName);
            }
        } else {
            file = new File(fileName);
            for (int i = 1; file.exists(); i++) {
                mName = fileName + "(" + i + ")";
                file = new File(mName);
            }
        }
        return mName;
    }

    public static void copy(File src, File dest) {
        InputStream is = null;
        OutputStream os = null;

        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dest);
            byte[] b = new byte[256];
            int len = 0;
            try {
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);

                }
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    public static void copy(String src, String dest) {
        InputStream is = null;
        OutputStream os = null;

        File out = new File(dest);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        try {
            is = new BufferedInputStream(new FileInputStream(src));
            os = new BufferedOutputStream(new FileOutputStream(dest));

            byte[] b = new byte[256];
            int len = 0;
            try {
                while ((len = is.read(b)) != -1) {
                    os.write(b, 0, len);

                }
                os.flush();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }

    public static byte[] resizeImg(String path, float maxLength) {
        // int d = getExifOrientation(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;

        byte[] tempArry;

        //int l = Math.max(options.outHeight, options.outWidth);
        //int be = (int) (l / maxLength);
        //if (be <= 0) {
        //    be = 1;
        //}
        options.inSampleSize = 1;
        int compressCounts = 0;

        do {
            options.inSampleSize = options.inSampleSize * 2;
            compressCounts++;
            Log.d(TAG, "resizeImg(): options.inSampleSize" + options.inSampleSize);

            bitmap = BitmapFactory.decodeFile(path, options);
            if (null == bitmap) {
                return null;
            }
            /*
            if (d != 0) {
                bitmap = rotate(bitmap, d);
            }
            */

            String[] tempStrArry = path.split("\\.");
            String filePostfix = tempStrArry[tempStrArry.length - 1];
            CompressFormat formatType = null;
            if (filePostfix.equalsIgnoreCase("PNG")) {
                formatType = Bitmap.CompressFormat.PNG;
            } else if (filePostfix.equalsIgnoreCase("JPG") ||
                            filePostfix.equalsIgnoreCase("JPEG")) {
                formatType = Bitmap.CompressFormat.JPEG;
                // } else if (filePostfix.equalsIgnoreCase("GIF")) {
                // formatType = Bitmap.CompressFormat.PNG;
            } else if (filePostfix.equalsIgnoreCase("BMP")) {
                formatType = Bitmap.CompressFormat.PNG;
            } else {
                Log.d(TAG, "resizeImg(): Can't compress the image,because can't support the format:"
                            + filePostfix);
                return null;
            }

            int quality = 30;
            //if (be == 1) {
            //    if (getFileSize(path) > 50 * 1024) {
            //        quality = 30;
            //    }
            //} else {
            //    quality = 30;
            //}

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(formatType, quality, baos);
            tempArry = baos.toByteArray();
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                baos = null;
            }
            Log.d(TAG, "resizeImg(): tempArry.length " + tempArry.length
                                 + " maxLength = " + maxLength);
        } while(tempArry.length > maxLength && compressCounts <= 3);

        return tempArry;
    }

    /**
     *
     *
     * @param stream
     * @return
     */
    public static void nmsStream2File(byte[] stream, String filepath) throws Exception {
        FileOutputStream outStream = null;
        try {
            File f = new File(filepath);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            outStream = new FileOutputStream(f);
            outStream.write(stream);
            outStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "nmsStream2File():", e);
            throw new RuntimeException(e.getMessage());
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                    outStream = null;
                } catch (IOException e) {
                    Log.e(TAG, "nmsStream2File():", e);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    public static boolean isPic(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".bmp") || path.endsWith(".gif")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidAttach(String path, boolean inspectSize) {
        if (!isExistsFile(path) || getFileSize(path) == 0) {
            Log.e(TAG, "isValidAttach: file is not exist, or size is 0");
            return false;
        }
        return true;
    }

    public static boolean isExistsFile(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return false;
            }
            File file = new File(filepath);
            return file.exists();
        } catch (Exception e) {
            Log.e(TAG, "isExistsFile():", e);
            return false;
        }
    }

    public static int getFileSize(String filepath) {
        try {
            if (TextUtils.isEmpty(filepath)) {
                return -1;
            }
            File file = new File(filepath);
            return (int) file.length();
        } catch (Exception e) {
            Log.e(TAG, "getFileSize():", e);
            return -1;
        }
    }

    public static long getCompressLimit() {
        return 1024 * 300; // 300K
    }

    public static long getPhotoSizeLimit() {
        return 10 * 1024 * 1024;
    }

    public static String getPhotoResolutionLimit() {
        return "4000x4000";
    }

    public static String getVideoResolutionLimit() {
        return "1920x1080";
    }

    public static long getAudioDurationLimit() {
        return 180000L;
    }

    public static boolean isVideo(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".mp4") || path.endsWith(".3gp")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAudio(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        // /M: add 3gpp audio file {@
        String extArrayString[] = { ".amr", ".ogg", ".mp3", ".aac", ".ape", ".flac", ".wma",
                ".wav", ".mp2", ".mid", ".3gpp" };
        // /@}
        String path = name.toLowerCase();
        for (String ext : extArrayString) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFileStatusOk(Context context, String path) {
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(context, ContextCacher.getPluginContext().getString(R.string.ipmsg_no_such_file), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isExistsFile(path)) {
            Toast.makeText(context, ContextCacher.getPluginContext().getString(R.string.ipmsg_no_such_file), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static String formatFileSize(int size) {
        String result = "";
        int oneMb = 1024 * 1024;
        int oneKb = 1024;
        if (size > oneMb) {
            int s = size % oneMb / 100;
            if (s == 0) {
                result = size / oneMb + "MB";
            } else {
                result = size / oneMb + "." + s + "MB";
            }
        } else if (size > oneKb) {
            int s = size % oneKb / 100;
            if (s == 0) {
                result = size / oneKb + "KB";
            } else {
                result = size / oneKb + "." + s + "KB";
            }
        } else if (size > 0) {
            result = size + "B";
        } else {
            result = ContextCacher.getPluginContext().getString(R.string.unknown_size);
        }
        return result;
    }

    public static String formatAudioTime(int duration) {
        String result = "";
        if (duration > 60) {
            if (duration % 60 == 0) {
                result = duration / 60 + "'";
            } else {
                result = duration / 60 + "'" + duration % 60 + "\"";
            }
        } else if (duration > 0) {
            result = duration + "\"";
        } else {
            // TODO IP message replace this string with resource
            result = ContextCacher.getPluginContext().getString(R.string.unknown_duration);
        }
        return result;
    }

    /**
     * Get bitmap.
     *
     * @param path
     * @param options
     * @return
     */
    public static Bitmap getBitmapByPath(String path, Options options, int width, int height) {
        if (TextUtils.isEmpty(path) || width <= 0 || height <= 0) {
            Log.w(TAG, "parm is error.");
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            Log.w(TAG, "file not exist!");
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:" + e.toString());
        }
        if (options != null) {
            Rect r = getScreenRegion(width, height);
            int w = r.width();
            int h = r.height();
            int maxSize = w > h ? w : h;
            int inSimpleSize = computeSampleSize(options, maxSize, w * h);
            options.inSampleSize = inSimpleSize;
            options.inJustDecodeBounds = false;
        }
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeStream(in, null, options);
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "bitmap decode failed, catch outmemery error");
        }
        try {
            in.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.toString());
        }
        return bm;
    }

    private static Rect getScreenRegion(int width, int height) {
        return new Rect(0, 0, width, height);
    }

    /**
     * Compute sample size.
     * @param options BitmapFactory.Options
     * @param minSideLength Min length
     * @param maxNumOfPixels Max number by pixels
     * @return int sample size.
     */
    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math.ceil(Math.sqrt(w * h
                / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED) && (minSideLength == UNCONSTRAINED)) {
            return 1;
        } else if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            Log.e(TAG, "getExifOrientation():", ex);
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                default:
                    break;
                }
            }
        }

        return degree;
    }

    public static Options getOptions(String path) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options;
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
                Log.w(TAG, "OutOfMemoryError.");
            }
        }
        return b;
    }

    /**
     * This method is from message app.
     *
     * @param context It used to get string from res.
     * @param when time.
     * @return string.
     */
    public static String formatTimeStampStringExtend(Context context, long when) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFags = DateUtils.FORMAT_NO_NOON_MIDNIGHT
                | DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            if ((now.yearDay - then.yearDay) == 1) {
                return context.getString(R.string.str_ipmsg_yesterday);
            } else {
                formatFags |= DateUtils.FORMAT_SHOW_DATE;
            }
        } else if ((now.toMillis(false) - then.toMillis(false)) < 60000) {
            return context.getString(R.string.time_now);
        } else {
            // Otherwise, if the message is from today, show the time.
            formatFags |= DateUtils.FORMAT_SHOW_TIME;
        }
        return DateUtils.formatDateTime(context, when, formatFags);
    }

    /**
     * this method is from message app.
     * @param context It used to get string from res.
     * @param when time.
     * @param fullFormat flag.
     * @return time string.
     */
    public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int formatFags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
        // / M: Fix ALPS00419488 to show 12:00, so mark
        // DateUtils.FORMAT_ABBREV_ALL
        // DateUtils.FORMAT_ABBREV_ALL |
                DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            formatFags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            formatFags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            formatFags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make
        // showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            formatFags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return DateUtils.formatDateTime(context, when, formatFags);
    }

    /**
     * na.
     * @param bytes file size.
     * @param context caller of this method.
     * @return file size string.
     */
    public static String getDisplaySize(long bytes, Context context) {
        String displaySize = context.getString(R.string.unknown);
        long iKb = bytes / 1024;
        if (iKb == 0 && bytes >= 0) {
            // display "less than 1KB"
            displaySize = context.getString(R.string.less_1K);
        } else if (iKb >= 1024) {
            // diplay MB
            double iMb = ((double) iKb) / 1024;
            iMb = round(iMb, 2, BigDecimal.ROUND_UP);
            StringBuilder builder = new StringBuilder(new Double(iMb).toString());
            builder.append("MB");
            displaySize = builder.toString();
        } else {
            // display KB
            StringBuilder builder = new StringBuilder(new Long(iKb).toString());
            builder.append("KB");
            displaySize = builder.toString();
        }
        return displaySize;
    }

    private static double round(double value, int scale, int roundingMode) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, roundingMode);
        double d = bd.doubleValue();
        bd = null;
        return d;
    }

    /**
     * get the 3G/4G Capability subId.
     * @return the 3G/4G Capability subId
     */
    public static long get34GCapabilitySubId() {
        ITelephony iTelephony = ITelephony.Stub
                .asInterface(ServiceManager.getService("phone"));
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        long subId = -1;
        /*
         * if (iTelephony != null) { for (int i = 0; i <
         * telephonyManager.getPhoneCount(); i++) { try { Log.d(TAG,
         * "get34GCapabilitySubId, iTelephony.getPhoneRat(" + i + "): " +
         * iTelephony.getPhoneRat(i)); if (((iTelephony.getPhoneRat(i) &
         * (PhoneRatFamily.PHONE_RAT_FAMILY_3G |
         * PhoneRatFamily.PHONE_RAT_FAMILY_4G)) > 0)) { subId =
         * PhoneFactory.getPhone(i).getSubId(); Log.d(TAG,
         * "get34GCapabilitySubId success, subId: " + subId); return subId; } }
         * catch (RemoteException e) { Log.d(TAG,
         * "get34GCapabilitySubId FAIL to getPhoneRat i" + i + " error:" +
         * e.getMessage()); } } }
         */
        return subId;
    }

    /**
     * Get current send sub id. Used for forward
     * @param context Context
     * @return sub id.
     */
    public static int getSendSubid(Context context) {
        List<SubscriptionInfo> mSubInfoList = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        int mSubCount = (mSubInfoList == null || mSubInfoList.isEmpty()) ? 0 : mSubInfoList
                .size();
        Log.v(TAG, "getSimInfoList(): mSubCount = " + mSubCount);
        if (mSubCount > 1) {
            int subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
            if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
                // getZhuKa
                int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
                if (!SubscriptionManager.isValidSubscriptionId(mainCardSubId)) { // data unset
                    return mSubInfoList.get(0).getSubscriptionId(); // SIM1
                }
                return mainCardSubId;
            } else { // SIM1/SIM2
                return subIdinSetting;
            }
        } else if (mSubCount == 1) {
            return mSubInfoList.get(0).getSubscriptionId();
        } else {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
    }

    // add for forward
    public static int getForwardSubid(Context context) {
        List<SubscriptionInfo> mSubInfoList = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        int mSubCount = (mSubInfoList == null || mSubInfoList.isEmpty()) ? 0 : mSubInfoList
                 .size();
        Log.v(TAG, "getSimInfoList(): mSubCount = " + mSubCount);
        if (mSubCount > 1) {
            int subIdinSetting = SubscriptionManager.getDefaultSmsSubId();
            if (subIdinSetting == Settings.System.SMS_SIM_SETTING_AUTO) {
                // getZhuKa
                int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
                if (!SubscriptionManager.isValidSubscriptionId(mainCardSubId)) { // data unset
                    return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                }
                return mainCardSubId;
            } else { // SIM1/SIM2
                return subIdinSetting;
            }
        } else if (mSubCount == 1) {
            return mSubInfoList.get(0).getSubscriptionId();
        } else {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
    }

    public static boolean isSupportRcsForward(Context context) {
        if (!RCSServiceManager.getInstance().serviceIsReady()) {
            Log.d(TAG, "isSupprotRcsForward() result = false");
            return false;
        }
        boolean result;
        int subid = RcsMessageUtils.getForwardSubid(context);
        Log.d(TAG, "subid = " + subid);
        int mainCardSubId = SubscriptionManager.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(subid)
                && SubscriptionManager.isValidSubscriptionId(mainCardSubId)
                && subid == mainCardSubId) {
            result = true;
        } else {
            result = false;
        }
        Log.d(TAG, "isSupprotRcsForward = " + result);
        return result;
    }

    public static Intent createForwardIntentFromMms(Context context, Uri uri) {
        if (isSupportRcsForward(context)) {
            Intent sendIntent = new Intent();
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
            sendIntent.setType("mms/pdu");
            return sendIntent;
        }
        return null;
    }

    public static Intent createForwordIntentFromSms(Context context, String mBody) {
        if (isSupportRcsForward(context)) {
            Intent sendIntent = new Intent();
            sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_STREAM, mBody);
            return sendIntent;
        }
        return null;
    }

    public static Intent createForwordIntentFromIpmessage(Context context, IpMessage ipMessage) {
        if (isSupportRcsForward(context)) {
            Intent sendIntent = new Intent();
            sendIntent.setAction("android.intent.action.ACTION_RCS_MESSAGING_SEND");
            if (ipMessage.getType() == IpMessageType.TEXT) {
                IpTextMessage textMessage = (IpTextMessage) ipMessage;
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_STREAM, textMessage.getBody());
                return sendIntent;
            } else if (ipMessage.getType() == IpMessageType.EMOTICON) {
                IpTextMessage textMessage = (IpTextMessage) ipMessage;
                sendIntent.setType("text/vemoticon");
                sendIntent.putExtra(Intent.EXTRA_STREAM, textMessage.getBody());
                return sendIntent;
            } else {
                if (RCSServiceManager.getInstance().serviceIsReady()) {
                    IpAttachMessage attachMessage = (IpAttachMessage) ipMessage;
                    if (ipMessage.getType() == IpMessageType.PICTURE) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("image/jpeg");
                    } else if (ipMessage.getType() == IpMessageType.VIDEO) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("video/mp4");
                    } else if (ipMessage.getType() == IpMessageType.VCARD) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("text/x-vcard");
                    } else if (ipMessage.getType() == IpMessageType.GEOLOC) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("geo/*");
                    } else if (ipMessage.getType() == IpMessageType.VOICE) {
                        sendIntent.putExtra(Intent.EXTRA_STREAM, attachMessage.getPath());
                        sendIntent.setType("audio/*");
                    }
                    return sendIntent;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public static int getMainCardSendCapability() {
        if (RCSServiceManager.getInstance().serviceIsReady()) {
            Log.v(TAG, "getMainCardSendCapability(): mSendSubid SubCapatibily=7");
            return 7; // send all
        } else {
            Log.v(TAG, "getMainCardSendCapability(): mSendSubid SubCapatibily=3 ");
            return 3; // send Mms\Sms
        }
    }

    public static boolean getConfigStatus() {
        boolean isActive = RCSServiceManager.getInstance().isServiceEnabled();
        Log.v(TAG, "getConfigStatus: isActive" + isActive);
        return isActive;
    }

    public static int getUserSelectedId(Context context) {
        int subIdinSetting = (int) SubscriptionManager.getDefaultSmsSubId();
        return subIdinSetting;
    }

    public static int getRcsSubId(Context context) {
        int rcsSubId = SubscriptionManager.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(rcsSubId)) {////data unset
            rcsSubId = -1;
        }
        return rcsSubId;
    }

    /**
     * Get Photo Destinator path. Used for choose a picture.
     * @param filePath String
     * @param context Context
     * @return String file path
     */
    public static String getPhotoDstFilePath(String filePath, Context context) {
        // for choose a picture
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        return getPicTempPath(context) + File.separator + fileName;
    }

    public static String getPhotoDstFilePath(Context context) {
        // only for take photo
        String fileName = System.currentTimeMillis() + ".jpg";
        return getPicTempPath(context) + File.separator + fileName;
    }

    public static String getVideoDstFilePath(String filePath, Context context) {
        // for choose a video
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        return getVideoTempPath(context) + File.separator + fileName;
    }

    public static String getVideoDstFilePath(Context context) {
        // only for record a video
        String fileName = System.currentTimeMillis() + ".3gp";
        return getVideoTempPath(context) + File.separator + fileName;
    }

    public static String getAudioDstPath(String filePath, Context context) {
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        return getAudioTempPath(context) + File.separator + fileName;
    }

    public static String getGeolocPath(Context context) {
        return getGeolocTempPath(context) + File.separator;
    }

    public static void copyFileToDst(String src, String dst) {
        if (src == null || dst == null) {
            return;
        }
        RcsMessageUtils.copy(src, dst);
    }

    public static boolean isGif(String filePath) {
        if (filePath != null && filePath.contains(".gif")) {
            return true;
        }
        return false;
    }

    public static String getFavoritePath(Context context, String folder) {
        String favTempPath = null;
        if (getSDCardStatus()) {
            favTempPath = getSDCardPath(context)
                                  + IP_MESSAGE_FILE_PATH + folder;
            File favoritePath = new File(favTempPath);
            if (!favoritePath.exists()) {
                favoritePath.mkdirs();
            }
        }
        return favTempPath;
    }

    public static String getPicTempPath(Context context) {
        String picTempPath = null;
        if (RcsMessageUtils.getSDCardStatus()) {
            picTempPath = RcsMessageUtils.getSDCardPath(context)
                                  + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "picture";
            File picturePath = new File(picTempPath);
            if (!picturePath.exists()) {
              picturePath.mkdirs();
            }
        }
        return picTempPath;
    }

    public static String getAudioTempPath(Context context) {
        String sAudioTempPath = null;
        if (RcsMessageUtils.getSDCardStatus()) {
            sAudioTempPath = RcsMessageUtils.getSDCardPath(context)
                + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }
        }
        return sAudioTempPath;
    }

    public static String getVideoTempPath(Context context) {
        String sVideoTempPath = null;
        if (RcsMessageUtils.getSDCardStatus()) {
            sVideoTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }
        }
        return sVideoTempPath;
    }

    public static String getVcardTempPath(Context context) {
        String sVcardTempPath = null;
        if (RcsMessageUtils.getSDCardStatus()) {
            sVcardTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }
        }
        return sVcardTempPath;
    }

    public static String getGeolocTempPath(Context context) {
        String  sGeoloTempPath = null;
        if (RcsMessageUtils.getSDCardStatus()) {
            sGeoloTempPath = RcsMessageUtils.getSDCardPath(context)
                    + RcsMessageUtils.IP_MESSAGE_FILE_PATH + "loc";
            File geolocPath = new File(sGeoloTempPath);
            if (!geolocPath.exists()) {
                geolocPath.mkdirs();
            }
        }
        return sGeoloTempPath;
    }

    /**
     * Start rcs setting activity
     * @param context Activity
     * @param requestCode requestCode
     */
    public static void startRcsSettingActivity(Activity context, int requestCode) {
        Intent it = new Intent("com.mediatek.rcs.genericui.RcsSettingsActivity");
        it.setPackage("com.mediatek.rcs.genericui");
        context.startActivityForResult(it, requestCode);
    }

    /**
     * Whether need notify user when transfer to sms when rcs message send failed.
     * @param context plugin context
     * @param subId subid
     * @return return true if needed, or return false
     */
    public static boolean isNeedNotifyUserWhenToSms(Context context, int subId) {
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NETWORK_CHANGED_NOTIFY,
                Context.MODE_WORLD_READABLE);
        boolean ret = sp.getBoolean(PREFERENCE_NETWORK_CHANGED_SMS_KEY + subId, true);
        Log.d(TAG, "[isNeedNotifyUserWhenToSms]: subId = " + subId + ", result= " + ret);
        return ret;
    }

    /**
     * Update new value of whether transfer to sms when rcs message send failed.
     * @param context plugin context
     * @param subId subid
     * @param value new value of whether transfer to sms
     * @return true if update successfully
     */
    public static boolean updateNeedNotifyUserWhenToSmsValue(Context context,
                                                                int subId, boolean value) {
        Log.d(TAG, "[updateNeedNotifyUserWhenToSmsValue]:subId =" + subId + ",value= " + value);
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NETWORK_CHANGED_NOTIFY,
                Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREFERENCE_NETWORK_CHANGED_SMS_KEY + subId, value);
        return editor.commit();
    }

    /**
     * Whether notify user when send rcs message.
     * @param context plugin context
     * @param subId subid
     * @return true if needed, or return false
     */
    public static boolean isNeedNotifyUserWhenToRCS(Context context, int subId) {
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NETWORK_CHANGED_NOTIFY,
                Context.MODE_WORLD_READABLE);
        boolean ret = sp.getBoolean(PREFERENCE_NETWORK_CHANGED_RCS_KEY + subId, true);
        Log.d(TAG, "[isNeedNotifyUserWhenToRCS]: subId = " + subId + ", result= " + ret);
        return ret;
    }

    /**
     * Update the boolean value of whether notify to user when send rcs message.
     * @param context plugin context
     * @param subId subid
     * @param value new value
     * @return update result, return true when success
     */
    public static boolean updateNeedNotifyUserWhenToRcsValue(Context context,
                                                                    int subId, boolean value) {
        Log.d(TAG, "[updateNeedNotifyUserWhenToRcsValue]:subId =" + subId + ",value= " + value);
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_NETWORK_CHANGED_NOTIFY,
                Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREFERENCE_NETWORK_CHANGED_RCS_KEY + subId, value);
        return editor.commit();
    }

    /**
     * Return whether to transfer to sms when rcs message send fail.
     * @param context plugin context
     * @param subId subid
     * @return return true when transfer to or return false
     */
    public static boolean isTransferToSMSWhenSendFailed(Context context, int subId) {
        Context prefContext = null;
        boolean ret = true;
        try {
            prefContext = context.createPackageContext("com.mediatek.rcs.genericui",
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can not find package:com.mediatek.rcs.genericui.");
        }
        if (prefContext != null) {
            SharedPreferences sp = prefContext.getSharedPreferences("rcs_sms_mode",
                                Context.MODE_WORLD_READABLE |Context.MODE_MULTI_PROCESS);
            if (sp != null) {
                ret = sp.getBoolean("enable", true);
                Log.d(TAG, "isTransferToSMSWhenSendFailed: ret = " + ret);
            }
        }
        return ret;
    }

    /**
     * Set new value of whether transfer to sms when send rcs message fail.
     * @param context Context
     * @param subId subid
     * @param value new value
     * @return true if set successfully
     */
    public static boolean setTransferToSMSWhenSendFailed(Context context, int subId,
                                                                boolean value) {
        Log.d(TAG, "setTransferToSMSWhenSendFailed: " + subId + ", value = " + value);
        if (isTransferToSMSWhenSendFailed(context, subId) != value) {
            Intent intent = new Intent("com.mediatek.rcs.genericui.SMS_MODE_CHANGE");
            intent.setPackage("com.mediatek.rcs.genericui");
            intent.putExtra("enable", value);
            context.sendBroadcast(intent);
        }
        return true;
    }

    /**
     * Transfer To SMS when rcs message send failed.
     * @param context Context
     * @param smsId the id in sms table in mms provider
     * @param ipmsgId the id in message table in stack provider
     * @param subId send subid
     */
    public static void transferToSMSFromFailedRcsMessage(Context context, long threadId, long msgId,
                long ipmsgId, int subId, String recipients, String body) {
      //transfer to sms
        boolean requestDeliveryReport =  getRequestSmsDeliveryReport(context, subId);

        String[] contacts = recipients.split(RCSMessageManager.SEMICOLON);
        for (int i = 0; i < contacts.length; i++) {
            Sms.addMessageToUri(subId,
                    context.getContentResolver(),
                    Uri.parse("content://sms/queued"), contacts[i],
                    body, null, System.currentTimeMillis(),
                    true /* read */,
                    requestDeliveryReport,
                    threadId);
        }
        RCSDataBaseUtils.deleteMessage(context, msgId);

        Intent intent = new Intent(RCSUtils.ACTION_SEND_MESSAGE);
        intent.setClassName("com.android.mms",
                        "com.android.mms.transaction.SmsReceiver");
        context.sendBroadcast(intent);
    }

    /**
     * Get Request SMS Delivery report. Must input Host Context.
     * @param context
     * @param subId
     * @return true if need delivery report
     */
    private static boolean getRequestSmsDeliveryReport(Context context, long subId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(subId + "_" + RCSUtils.SMS_DELIVERY_REPORT_MODE,
                RCSUtils.DEFAULT_DELIVERY_REPORT_MODE);
    }

    /**
     * Is support VoLTE Conference multi call.
     * @param context Context
     * @return true if support
     */
    public static boolean isVoLTEConfCallEnable(Context context) {
        if (!isVolteEnhancedConfCallSupport() || context == null) {
            return false;
        }
        final TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccount> phoneAccouts = telecomManager.getAllPhoneAccounts();
        for (PhoneAccount phoneAccount : phoneAccouts) {
            if (phoneAccount.hasCapabilities(
                    PhoneAccount.CAPABILITY_VOLTE_CONFERENCE_ENHANCED)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVolteEnhancedConfCallSupport() {
        return MTK_ENHANCE_VOLTE_CONF_CALL && MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
    }

    /**
     * If contain Emoji in string.
     * @param source String
     * @return return true if containl or return false.
     */
    public static boolean containsEmoji(String source) {
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);
            if (isEmojiCharacter(codePoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is Emoji Character.
     * @param codePoint the compare char
     * @return true if the character is emoji character, else return false.
     */
    private static boolean isEmojiCharacter(char codePoint) {
        boolean nonEmoji = (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA) ||
                (codePoint == 0xD) || ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) || ((codePoint >= 0x10000)
                && (codePoint <= 0x10FFFF));
       return !nonEmoji;
    }

    /**
     * Is Sms Enabled.
     * @param context Context
     * @return true if com.android.mms is default sms application, or return false.
     */
    public static boolean isSmsEnabled(Context context) {
        String defaultSmsApplication = Sms.getDefaultSmsPackage(context);

        if (defaultSmsApplication != null && defaultSmsApplication.equals("com.android.mms")) {
            return true;
        }
        return false;
    }

    /**
     * When enter one group message list, need to subscribe lasted group info. In order to avoid
     * subscribe frequently, so set the interval time between  two subscription must bigger than
     * 30 seconds. If the interval is bigger than 30 seconds return true, else return false.
     *
     * @param context Context
     * @param chatId group chat id
     * @return boolean
     */
    public static boolean isNeedSubscribeOfflineGroupInfo(Context context, String chatId) {
        boolean ret =false;
        long lastTime = getLastSubscribeOfflineGroupTime(context, chatId);
        long now = System.currentTimeMillis();
        long diff = now - lastTime;
        if (diff < 0) {
            diff = - diff;
        }
        //if diff bigger than 30 seconds, need to subscribe group info when enter group.
        ret = diff >= 30 * 60 * 1000;
        Log.d(TAG, "isNeedSubscribeOfflineGroupInfo: diff = " + diff + ", ret = " + ret);
        return ret;
    }
    private static long getLastSubscribeOfflineGroupTime(Context context, String chatId) {
        SharedPreferences sp = context.getSharedPreferences(
                    PREFERENCE_SUBSCRIBE_OFFLINE_GROUP_INFO_TIME, Context.MODE_WORLD_READABLE);
        long time = sp.getLong(chatId, 0);
        Log.d(TAG, "getLastSubscribeOfflineGroupTime, time = " + time);
        return time;
    }

    /**
     * Update subscribe off line group time. Please see
     * {@link #isNeedSubscribeOfflineGroupInfo(Context, String)}
     * @param context Context
     * @param chatId group chat id
     * @return boolean
     */
    public static boolean updateSubscribeOfflineGroupTime(Context context, String chatId) {
        SharedPreferences sp = context.getSharedPreferences(
                PREFERENCE_SUBSCRIBE_OFFLINE_GROUP_INFO_TIME, Context.MODE_WORLD_WRITEABLE);
        long timeNow = System.currentTimeMillis();
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(chatId, timeNow);
        Log.d(TAG, "updateSubscribeOfflineGroupTime, timeNow = " + timeNow);
        return editor.commit();
    }

    /**
     * Combine two String arrays to one.
     * @param src1 String array 1
     * @param length1 the number to copy from index 0 from src1
     * @param src2 String array 2
     * @param length2 the number to copy from index 0 from src2
     * @return combined array
     */
    public static String[] combineTwoStringArrays(String[] src1, int length1,
                                                    String[] src2, int length2) {
        length1 = length1 > src1.length ? src1.length : length1;
        length2 = length2 > src2.length ? src1.length : length2;
        String[] dst = new String[length1 + length2];
        System.arraycopy(src1, 0, dst, 0, length1);
        System.arraycopy(src2, 0, dst, length1, length2);
        return dst;
    }

    /**
     * Combine two String arrays to one.
     * @param src1 String array 1
     * @param src2 String array 2
     * @return combined array
     */
    public static String[] combineTwoStringArrays(String[] src1, String[] src2) {
        if (src1 == null || src2 == null) {
            throw new RuntimeException("src1 or src2 is null");
        }
        int length1 = src1.length;
        int length2 = src2.length;
        return combineTwoStringArrays(src1, length1, src2, length2);
    }
}
