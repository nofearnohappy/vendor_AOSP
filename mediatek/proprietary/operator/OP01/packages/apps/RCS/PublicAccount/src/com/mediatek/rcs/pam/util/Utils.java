package com.mediatek.rcs.pam.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.MediaFolder;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.storage.StorageManagerEx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * this class contants some useful methods.
 */
public class Utils {
    private static final DateTimeZone GMT8 = DateTimeZone
            .forID("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = ISODateTimeFormat
            .dateTimeNoMillis();
    public static final int UNCONSTRAINED = -1;
    public static final String PA_MESSAGE_CACHE_PATH = File.separator
            + MediaFolder.ROOT_DIR + File.separator + ".cache";
    public static final String PA_FILE_PREFIX_IMG = "IMG";
    public static final String PA_FILE_PREFIX_VDO = "VDO";
    public static final String PA_FILE_SUFFIX_JPG = ".jpg";
    public static final String PA_FILE_SUFFIX_3GP = ".3gp";

    private static final String TAG = Constants.TAG_PREFIX + "Utils";

    /**
     * @return current time stamp
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis() / 1000 * 1000;
    }

    // Convert GMT+8 time string to UTC timestamp
    public static long convertStringToTimestamp(String s) {
        if (TextUtils.isEmpty(s)) {
            return Constants.INVALID;
        }
        try {
            DateTime dateTime = FORMATTER.parseDateTime(s);
            return dateTime.getMillis();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return Constants.INVALID;
        }
    }

    // Convert UTC timestamp to GMT+8 time string
    public static String covertTimestampToString(long timestamp) {
        DateTime dt = new DateTime(timestamp, DateTimeZone.UTC);
        dt = dt.toDateTime(GMT8);
        return FORMATTER.print(dt);
    }

    public static String formatTimeStampString(Context context, long when,
            boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        int formatFlags = DateUtils.FORMAT_NO_NOON_MIDNIGHT
                | DateUtils.FORMAT_CAP_AMPM;

        if (then.year != now.year) {
            formatFlags |= DateUtils.FORMAT_SHOW_YEAR
                    | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            formatFlags |= DateUtils.FORMAT_SHOW_DATE;
        }

        formatFlags |= DateUtils.FORMAT_SHOW_TIME;

        if (fullFormat) {
            formatFlags |= (DateUtils.FORMAT_SHOW_DATE);
        }
        return DateUtils.formatDateTime(context, when, formatFlags);
    }

    public static void throwIf(int resultCode, boolean predicate)
            throws PAMException {
        if (predicate) {
            throw new PAMException(resultCode);
        }
    }

    public static void copyFile(String src, String dest) throws IOException {
        copyFile(new File(src), new File(dest));
    }

    public static void copyFile(File src, File dest) throws IOException {
        int byteSum = 0;
        int byteRead = 0;
        byte[] buffer = new byte[4096];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            while ((byteRead = fis.read(buffer)) != -1) {
                byteSum += byteRead;
                System.out.println(byteSum);
                fos.write(buffer, 0, byteRead);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void storeToFile(String content, String dest)
            throws IOException {
        int byteSum = 0;
        int byteRead = 0;
        byte[] buffer = new byte[4096];
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = IOUtils.toInputStream(content, "UTF8");
            fos = new FileOutputStream(dest);
            while ((byteRead = is.read(buffer)) != -1) {
                byteSum += byteRead;
                System.out.println(byteSum);
                fos.write(buffer, 0, byteRead);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void deleteFile(String mediaPath) {
        File f = new File(mediaPath);
        if (f.exists()) {
            f.delete();
        }
    }

    public static String extractUuidFromSipUri(String sipUri) {
        if (sipUri == null) {
            return null;
        } else if (sipUri.startsWith(Constants.QUOTED_SIP_PREFIX)) {
            int index = sipUri.indexOf(">");
            if (index == -1) {
                Log.e(TAG, "Invalid SIP URI format: " + sipUri);
                return null;
            }
            return sipUri
                    .substring(Constants.QUOTED_SIP_PREFIX.length(), index);
        } else if (sipUri.startsWith(Constants.SIP_PREFIX)) {
            return sipUri.substring(Constants.SIP_PREFIX.length());
        } else {
            Log.w(TAG, "Invalid SIP URI format: " + sipUri
                    + ", use it directly.");
            return sipUri;
        }
    }

    public static String extractNumberFromUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        int index = uuid.indexOf("@");
        if (index != -1) {
            return uuid.substring(0, index);
        } else {
            Log.e(TAG, "Invalid UUID format: " + uuid);
            return null;
        }
    }

    private static final Pattern FILE_SIZE_PATTERN = Pattern
            .compile("^[0-9]+(b|k|kb|m|mb|g|gb|t|tb)?$");

    @SuppressLint("DefaultLocale")
    public static int extractSize(String fileSize) {
        if (fileSize == null) {
            return -1;
        }
        fileSize = fileSize.toLowerCase();
        if (!FILE_SIZE_PATTERN.matcher(fileSize).matches()) {
            return -1;
        }
        try {
            if (fileSize.endsWith("kb")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 2)) * 1024;
            } else if (fileSize.endsWith("k")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 1)) * 1024;
            } else if (fileSize.endsWith("mb")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 2)) * 1024 * 1024;
            } else if (fileSize.endsWith("m")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 1)) * 1024 * 1024;
            } else if (fileSize.endsWith("gb")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 2)) * 1024 * 1024 * 1024;
            } else if (fileSize.endsWith("g")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 1)) * 1024 * 1024 * 1024;
            } else if (fileSize.endsWith("tb")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 2)) * 1024 * 1024 * 1024 * 1024;
            } else if (fileSize.endsWith("t")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 1)) * 1024 * 1024 * 1024 * 1024;
            } else if (fileSize.endsWith("b")) {
                return Integer.parseInt(fileSize.substring(0,
                        fileSize.length() - 1));
            } else {
                return Integer.parseInt(fileSize);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG,
                    "Failed to parse int, the input number may be too large: "
                            + fileSize);
            return -1;
        }
    }

    public static String formatFileSize(int size, int scale) {
        Log.d(TAG, "formatFileSize:" + size);
        String result = "";
        float oneMb = 1024f * 1024f;
        float oneKb = 1024f;
        if (size > oneMb) {
            float s = size / oneMb;
            float ss = new BigDecimal(s).setScale(scale,
                    BigDecimal.ROUND_HALF_UP).floatValue();
            result = ss + "MB";
        } else if (size > oneKb) {
            double s = size / oneKb;
            Double ss = new BigDecimal(s).setScale(scale,
                    BigDecimal.ROUND_HALF_UP).doubleValue();
            result = ss + "KB";
        } else if (size > 0) {
            result = size + "B";
        } else {
            result = String.valueOf(size);
        }
        return result;
    }

    public static CharSequence formatTextMessage(CharSequence inputChars,
            boolean showImg, CharSequence inputBuf) {
        Log.d(TAG, "formatTextMessage(): inputChars = " + inputChars);
        if (inputChars == null) {
            return "";
        }

        EmojiImpl emoji = EmojiImpl.getInstance(ContextCacher
                .getPluginContext());
        CharSequence outChars = emoji.getEmojiExpression(inputChars, showImg);
        if (inputBuf == null) {
            return outChars;
        } else {
            String bufStr = inputBuf.toString();
            String inputStr = inputChars.toString();
            int start = bufStr.indexOf(inputStr);
            if (start == -1) {
                return inputBuf;
            }
            CharSequence bufChars = emoji.getEmojiExpression(inputBuf, showImg);
            return bufChars;
        }
    }

    public static int getVideoCaptureDurationLimit() {
        return 500;
    }

    public static String getTempFilePath(String prefix, String suffix) {

        MediaFolder.getRootDir();

        String fileName = prefix + System.currentTimeMillis() + suffix;
        String pathName = Environment.getExternalStorageDirectory()
                + PA_MESSAGE_CACHE_PATH;
        File path = new File(pathName);
        if (!path.exists()) {
            path.mkdirs();
        }
        String fullPath = pathName + File.separator + fileName;
        Log.d(TAG, "getTempFilePath=" + fullPath);
        return fullPath;
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

        boolean sdCardExist = sdStatus
                .equals(android.os.Environment.MEDIA_MOUNTED);

        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
            return sdDir.toString();
        }
        return c.getFilesDir().getAbsolutePath();
    }

    public static String getCachePath(Context c) {
        String path = null;
        String sdCardPath = getSDCardPath(c);
        if (!TextUtils.isEmpty(sdCardPath)) {
            path = sdCardPath + PA_MESSAGE_CACHE_PATH + File.separator;
        }
        return path;
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
            final String extension = fileName.substring(index + 1,
                    fileName.length());
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

    public static Bitmap resizeImage(Bitmap bitmap, int w, int h,
            boolean needRecycle) {
        if (null == bitmap) {
            return null;
        }

        Bitmap bitmapOrg = bitmap;
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width,
                height, matrix, true);
        if (needRecycle && !bitmapOrg.isRecycled()
                && bitmapOrg != resizedBitmap) {
            bitmapOrg.recycle();
        }
        return resizedBitmap;
    }

    public static byte[] resizeImg(String path, float maxLength) {
        // int d = getExifOrientation(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;

        int l = Math.max(options.outHeight, options.outWidth);
        int be = (int) (l / maxLength);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;

        bitmap = BitmapFactory.decodeFile(path, options);
        if (null == bitmap) {
            return null;
        }
        Log.d(TAG, "resizeImg() after decodeFile. w=" + bitmap.getWidth()
                + ". h=" + bitmap.getHeight());
        /*
         * if (d != 0) { bitmap = rotate(bitmap, d); }
         */

        String[] tempStrArry = path.split("\\.");
        String filePostfix = tempStrArry[tempStrArry.length - 1];
        CompressFormat formatType = null;
        if (filePostfix.equalsIgnoreCase("PNG")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("JPG")
                || filePostfix.equalsIgnoreCase("JPEG")) {
            formatType = Bitmap.CompressFormat.JPEG;
            // } else if (filePostfix.equalsIgnoreCase("GIF")) {
            // formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("BMP")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else {
            Log.d(TAG,
                    "resizeImg(): Can't compress the image,because can't support the format:"
                            + filePostfix);
            return null;
        }

        int quality = 100;
        if (be == 1) {
            if (getFileSize(path) > 50 * 1024) {
                quality = 30;
            }
        } else {
            quality = 30;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(formatType, quality, baos);
        final byte[] tempArry = baos.toByteArray();
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            baos = null;
        }

        return tempArry;
    }

    public static void nmsStream2File(byte[] stream, String filepath)
            throws IOException {
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
            throw e;
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                    outStream = null;
                } catch (IOException e) {
                    Log.e(TAG, "nmsStream2File():", e);
                    throw e;
                }
            }
        }
    }

    public static boolean isPic(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String path = name.toLowerCase();
        if (path.endsWith(".png") || path.endsWith(".jpg")
                || path.endsWith(".jpeg") || path.endsWith(".bmp")
                || path.endsWith(".gif")) {
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

    public static String getFileExtension(String fileName) {
        Log.d(TAG, "getFileExtension() entry, the fileName is " + fileName);
        String extension = null;
        if (TextUtils.isEmpty(fileName)) {
            Log.d(TAG, "getFileExtension() entry, the fileName is null");
            return null;
        }
        int lastDot = fileName.lastIndexOf(".");
        extension = fileName.substring(lastDot + 1).toLowerCase();
        Log.d(TAG, "getFileExtension() entry, the extension is " + extension);
        return extension;
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
        // /M: add 3gpp audio file{@
        String extArrayString[] = { ".amr", ".ogg", ".mp3", ".aac", ".ape",
                ".flac", ".wma", ".wav", ".mp2", ".mid", ".3gpp" };
        // /@}
        String path = name.toLowerCase();
        for (String ext : extArrayString) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static String formatAudioTime(int duration) {
        Log.d(TAG, "formatAudioTime:" + duration);
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
            result = String.valueOf(duration);
        }
        return result;
    }

    /**
     * Get bitmap
     *
     * @param path
     * @param options
     * @return
     */
    public static Bitmap getBitmapByPath(String path, Options options,
            int width, int height) {
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

    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

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

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 : (int) Math
                .ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == UNCONSTRAINED) ? 128 : (int) Math
                .min(Math.floor(w / minSideLength),
                        Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if ((maxNumOfPixels == UNCONSTRAINED)
                && (minSideLength == UNCONSTRAINED)) {
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
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, -1);
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
            m.setRotate(degrees, (float) b.getWidth() / 2,
                    (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
                        b.getHeight(), m, true);
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
     * Get the current available storage size in byte;
     *
     * @return available storage size in byte; -1 for no external storage
     *         detected
     */
    public static long getFreeStorageSize() {
        boolean isExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (isExist) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            int availableBlocks = stat.getAvailableBlocks();
            int blockSize = stat.getBlockSize();
            long result = (long) availableBlocks * blockSize;
            Log.d(TAG, "getFreeStorageSize() blockSize: " + blockSize
                    + " availableBlocks: " + availableBlocks + " result: "
                    + result);
            return result;
        }
        return -1;
    }

    public static Size getFineImageSize(Size size, Context context) {
        double MAX_SCALE = 0.4;
        double MIN_SCALE = 0.3;
        int screenWidth = 0;
        int width = size.getWidth();
        int height = size.getHeight();
        int w = height;
        int h = 0;

        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wmg = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        wmg.getDefaultDisplay().getMetrics(dm);
        if (dm.heightPixels > dm.widthPixels) {
            screenWidth = dm.widthPixels;
        } else {
            screenWidth = dm.heightPixels;
        }
        if (width > screenWidth * MAX_SCALE) {
            w = (int) (screenWidth * MAX_SCALE);
            h = height * w / width;
        } else if (width > screenWidth * MIN_SCALE) {
            w = (int) (screenWidth * MIN_SCALE);
            h = height * w / width;
        } else {
            w = width;
            h = height;
        }

        return new Size(w, h);
    }
}
