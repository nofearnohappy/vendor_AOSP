package com.hesine.nmsg.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.text.TextUtils;

public class Image {

    private static final int ONE_HUNDRED = 100;

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
                ex.printStackTrace();
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }

    public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            MLog.error(MLog.getStactTrace(e));
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

    public static Bitmap resizeImage(Bitmap bitmap, int w, int h, boolean needRecycle) {
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

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        if (needRecycle && !bitmapOrg.isRecycled()) {
            bitmapOrg.recycle();
        }
        return resizedBitmap;
    }

    public static byte[] resizeImg(String path, float maxLength) {
        int d = getExifOrientation(path);
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
        if (null != bitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        bitmap = BitmapFactory.decodeFile(path, options);
        if (d != 0) {
            bitmap = rotate(bitmap, d);
        }
        String[] tempStrArry = path.split("\\.");
        String filePostfix = tempStrArry[tempStrArry.length - 1];
        CompressFormat formatType = null;
        if (filePostfix.equalsIgnoreCase("PNG")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("JPG") || filePostfix.equalsIgnoreCase("JPEG")) {
            formatType = Bitmap.CompressFormat.JPEG;
        } else if (filePostfix.equalsIgnoreCase("GIF")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else if (filePostfix.equalsIgnoreCase("BMP")) {
            formatType = Bitmap.CompressFormat.PNG;
        } else {
            MLog.error("Can't compress the image,because can't support the format:" + filePostfix);
            return null;
        }

        int quality = 100;
        if (be == 1) {
            if (FileEx.getFileSize(path) > 100 * 1024) {
                quality = 80;
            }
        } else {
            quality = 80;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(formatType, quality, baos);
        final byte[] tempArry = baos.toByteArray();
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException e) {
                MLog.error(MLog.getStactTrace(e));
            }
            baos = null;
        }
        return tempArry;
    }

    public static byte[] resizeImg(String path, int resize, int changesize) {
        MLog.info("request to resize image,path:" + path + " , size:" + resize);

        if (resize <= 1024) {
            MLog.error("cancel to compress image,the resize is too small");
            return null;
        }

        File tempFile = new File(path);
        if (!tempFile.exists()) {
            MLog.error( "failed to find image file by the path:" + path);
            return null;
        }

        int fileSize = (int) tempFile.length();
        MLog.info("successful to find image file,length:" + fileSize);

        byte[] targetBytes;
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        Bitmap photo = null;
        String filePostfix = null;
        CompressFormat formatType = null;
        try {
            if (resize >= fileSize) {
                inputStream = new FileInputStream(tempFile);
                targetBytes = new byte[fileSize];
                if (inputStream.read(targetBytes) <= -1) {
                    MLog.error("can't read the file to byet[]");
                } else {
                    MLog.info("successful to resize the image,after resize length is:" + targetBytes.length);
                    if (null != inputStream) {
                        inputStream.close();
                        inputStream = null;
                    }
                    return targetBytes;
                }
                if (null != inputStream) {
                    inputStream.close();
                    inputStream = null;
                }
            } else {
                int multiple = fileSize / resize;
                if (fileSize % resize >= 0 && fileSize % resize >= ((fileSize / multiple + 1) / 2)) {
                    multiple++;
                }

                if (multiple > 3) {
                    if (resize == 200 * 1024) {
                        multiple = 3;
                    } else if (resize == 100 * 1024) {
                        multiple = 6;
                    } else if (resize <= 50 * 1024) {
                        multiple = 10;
                    }
                }

                MLog.info("prepare to press sacle:" + multiple);
                Options options = new Options();
                options.inScaled = true;
                if (changesize == 0) {
                    options.inSampleSize = 1;
                } else {
                    options.inSampleSize = multiple;
                }

                int compressCount = 1;
                do {
                    if (null != photo && !photo.isRecycled()) {
                        photo.recycle();
                        photo = null;
                    }
                    photo = BitmapFactory.decodeFile(path, options);
                    options.inSampleSize = multiple + compressCount;
                    MLog.info("try to encondw image " + compressCount + " times");
                    compressCount++;
                } while (photo == null && compressCount <= 5);

                String[] tempStrArry = path.split("\\.");
                filePostfix = tempStrArry[tempStrArry.length - 1];
                tempStrArry = null;
                MLog.info("filePostfix:" + filePostfix);
                if (filePostfix.equals("PNG") || filePostfix.equals("png")) {
                    formatType = Bitmap.CompressFormat.PNG;
                } else if (filePostfix.equals("JPG") || filePostfix.equals("jpg") || filePostfix.equals("JPEG")
                        || filePostfix.equals("jpeg")) {
                    formatType = Bitmap.CompressFormat.JPEG;
                } else if (filePostfix.equalsIgnoreCase("GIF")) {
                    formatType = Bitmap.CompressFormat.PNG;
                } else if (filePostfix.equalsIgnoreCase("BMP")) {
                    formatType = Bitmap.CompressFormat.PNG;
                } else {
                    MLog.error("Can't compress the image,because can't support the format:" + filePostfix);
                    return null;
                }

                int quality = 100;
                while (quality > 0) {
                    baos = new ByteArrayOutputStream();
                    photo.compress(formatType, quality, baos);
                    final byte[] tempArry = baos.toByteArray();
                    MLog.info("successful to resize the image,after resize length is:" + tempArry.length
                            + " ,quality:" + quality);

                    if (tempArry.length <= resize) {
                        targetBytes = tempArry;
                        MLog.info("successful to resize the image,after resize length is:" + targetBytes.length);

                        if (null != inputStream) {
                            inputStream.close();
                            inputStream = null;
                        }
                        baos.flush();
                        baos.close();
                        baos = null;
                        return targetBytes;
                    }
                    if (tempArry.length >= 1000000) {
                        quality = quality - 10;
                    } else if (tempArry.length >= 260000) {
                        quality = quality - 5;
                    } else {
                        quality = quality - 1;
                    }

                    if (baos != null) {
                        baos.flush();
                        baos.close();
                        baos = null;
                    }
                }
                MLog.error("can't compress the photo with the scale size:" + multiple);
            }
        } catch (FileNotFoundException e) {
            MLog.error("FileNotFoundException ,when reading file:" + MLog.getStactTrace(e));
        } catch (IOException e) {
            MLog.error(MLog.getStactTrace(e));
        } finally {
            try {
                filePostfix = null;
                formatType = null;

                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }

                if (baos != null) {
                    baos.flush();
                    baos.close();
                    baos = null;
                }

                if (photo != null) {
                    if (!photo.isRecycled()) {
                        photo.recycle();
                    }
                    photo = null;
                }
            } catch (IOException e) {
                MLog.error(MLog.getStactTrace(e));
            }
        }
        return null;
    }
    
    public static byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, ONE_HUNDRED, bas);
        return bas.toByteArray();
    }

    public static Bitmap getBitmapFromFile(String filePath) {
        Bitmap bp = null;
        if (TextUtils.isEmpty(filePath)) {
            MLog.info("filePath is empty");
            return null;
        } else if (!FileEx.isFileExisted(filePath)) {
            MLog.info("image is not existed");
            return null;
        }
        bp = BitmapFactory.decodeFile(filePath);
        if (null == bp) {
            MLog.info("can not parse avatar fileOath: " + filePath);
        }
        return bp;
    }

}
