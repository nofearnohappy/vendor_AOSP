/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.camera.v2.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.Image.Plane;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import junit.framework.Assert;

import com.android.camera.R;

import com.mediatek.camcorder.CamcorderProfileEx;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import com.mediatek.camera.v2.stream.pip.pipwrapping.PipEGLConfigWrapper;
import com.mediatek.camera.v2.ui.Rotatable;

public class Utils {
    public static final int VIDEO_QUALITY_LOW = CamcorderProfileEx.QUALITY_LOW;
    public static final int VIDEO_QUALITY_MEDIUM = CamcorderProfileEx.QUALITY_MEDIUM;
    public static final int VIDEO_QUALITY_HIGH = CamcorderProfileEx.QUALITY_HIGH;
    public static final int VIDEO_QUALITY_FINE = CamcorderProfileEx.QUALITY_FINE;
    public static final int VIDEO_QUALITY_FINE_4K2K = CamcorderProfileEx.QUALITY_FINE_4K2K;
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 90;
    public static final int ROTATION_180 = 180;
    public static final int ROTATION_270 = 270;
    private static final String              TAG = "Utils";
    private static final int                 UNKNOWN = -1;
    private static final String              ENABLE_LIST_HEAD = "[L];";
    private static final String              ENABLE_LIST_SEPERATOR = ";";
    public static final String               RESET_STATE_VALUE_DISABLE = "disable-value";
    /** Orientation hysteresis amount used in rounding, in degrees. */
    public static final int                  ORIENTATION_HYSTERESIS = 5;
    public static final double[]             RATIOS = new double[] { 1.3333, 1.5, 1.6667, 1.7778 };
    public static final double               ASPECT_TOLERANCE = 0.02;
    private static ImageFileNamer            sImageFileNamer;

    /* use estimated values for picture size (in Bytes)*/
    static final DefaultHashMap<String, Integer>
            PICTURE_SIZE_TABLE = new DefaultHashMap<String, Integer>();

    static {
        PICTURE_SIZE_TABLE.put("512x288-normal", 30720);
        PICTURE_SIZE_TABLE.put("512x288-fine", 30720);
        PICTURE_SIZE_TABLE.put("512x288-superfine", 30720);

        PICTURE_SIZE_TABLE.put("256x144-normal", 13312);
        PICTURE_SIZE_TABLE.put("256x144-fine", 13312);
        PICTURE_SIZE_TABLE.put("256x144-superfine", 13312);

        PICTURE_SIZE_TABLE.put("1280x720-normal", 122880);
        PICTURE_SIZE_TABLE.put("1280x720-fine", 147456);
        PICTURE_SIZE_TABLE.put("1280x720-superfine", 196608);

        PICTURE_SIZE_TABLE.put("2560x1440-normal", 245760);
        PICTURE_SIZE_TABLE.put("2560x1440-fine", 368640);
        PICTURE_SIZE_TABLE.put("2560x1440-superfine", 460830);

        PICTURE_SIZE_TABLE.put("3328x1872-normal", 542921);
        PICTURE_SIZE_TABLE.put("3328x1872-fine", 542921);
        PICTURE_SIZE_TABLE.put("3328x1872-superfine", 678651);

        PICTURE_SIZE_TABLE.put("4096x2304-normal", 822412);
        PICTURE_SIZE_TABLE.put("4096x2304-fine", 822412);
        PICTURE_SIZE_TABLE.put("4096x2304-superfine", 1028016);

        PICTURE_SIZE_TABLE.put("4608x2592-normal", 1040866);
        PICTURE_SIZE_TABLE.put("4608x2592-fine", 1040866);
        PICTURE_SIZE_TABLE.put("4608x2592-superfine", 1301083);

        PICTURE_SIZE_TABLE.put("5120x2880-normal", 1285020);
        PICTURE_SIZE_TABLE.put("5120x2880-fine", 1285020);
        PICTURE_SIZE_TABLE.put("5120x2880-superfine", 1606275);

        PICTURE_SIZE_TABLE.put("560x336-normal", 30720);
        PICTURE_SIZE_TABLE.put("560x336-fine", 30720);
        PICTURE_SIZE_TABLE.put("560x336-superfine", 30720);

        PICTURE_SIZE_TABLE.put("400x240-normal", 13312);
        PICTURE_SIZE_TABLE.put("400x240-fine", 13312);
        PICTURE_SIZE_TABLE.put("400x240-superfine", 13312);

        PICTURE_SIZE_TABLE.put("1280x768-normal", 131072);
        PICTURE_SIZE_TABLE.put("1280x768-fine", 157286);
        PICTURE_SIZE_TABLE.put("1280x768-superfine", 209715);

        PICTURE_SIZE_TABLE.put("2880x1728-normal", 331776);
        PICTURE_SIZE_TABLE.put("2880x1728-fine", 497664);
        PICTURE_SIZE_TABLE.put("2880x1728-superfine", 622080);

        PICTURE_SIZE_TABLE.put("3600x2160-normal", 677647);
        PICTURE_SIZE_TABLE.put("3600x2160-fine", 677647);
        PICTURE_SIZE_TABLE.put("3600x2160-superfine", 847059);

        PICTURE_SIZE_TABLE.put("3600x2700-normal", 847059);
        PICTURE_SIZE_TABLE.put("3600x2700-fine", 847059);
        PICTURE_SIZE_TABLE.put("3600x2700-superfine", 1058824);

        PICTURE_SIZE_TABLE.put("3672x2754-normal", 881280);
        PICTURE_SIZE_TABLE.put("3672x2754-fine", 881280);
        PICTURE_SIZE_TABLE.put("3672x2754-superfine", 12640860);

        PICTURE_SIZE_TABLE.put("4096x3072-normal", 1096550);
        PICTURE_SIZE_TABLE.put("4096x3072-fine", 1096550);
        PICTURE_SIZE_TABLE.put("4096x3072-superfine", 1370688);

        PICTURE_SIZE_TABLE.put("4160x3120-normal", 1131085);
        PICTURE_SIZE_TABLE.put("4160x3120-fine", 1131085);
        PICTURE_SIZE_TABLE.put("4160x3120-superfine", 1413857);

        PICTURE_SIZE_TABLE.put("4608x3456-normal", 1387821);
        PICTURE_SIZE_TABLE.put("4608x3456-fine", 1387821);
        PICTURE_SIZE_TABLE.put("4608x3456-superfine", 1734777);

        PICTURE_SIZE_TABLE.put("5120x3840-normal", 1713359);
        PICTURE_SIZE_TABLE.put("5120x3840-fine", 1713359);
        PICTURE_SIZE_TABLE.put("5120x3840-superfine", 2141700);

        PICTURE_SIZE_TABLE.put("3264x2448-normal", 696320);
        PICTURE_SIZE_TABLE.put("3264x2448-fine", 696320);
        PICTURE_SIZE_TABLE.put("3264x2448-superfine", 870400);

        PICTURE_SIZE_TABLE.put("2592x1944-normal", 327680);
        PICTURE_SIZE_TABLE.put("2592x1944-fine", 491520);
        PICTURE_SIZE_TABLE.put("2592x1944-superfine", 614400);

        PICTURE_SIZE_TABLE.put("2560x1920-normal", 327680);
        PICTURE_SIZE_TABLE.put("2560x1920-fine", 491520);
        PICTURE_SIZE_TABLE.put("2560x1920-superfine", 614400);

        PICTURE_SIZE_TABLE.put("2048x1536-normal", 262144);
        PICTURE_SIZE_TABLE.put("2048x1536-fine", 327680);
        PICTURE_SIZE_TABLE.put("2048x1536-superfine", 491520);

        PICTURE_SIZE_TABLE.put("1600x1200-normal", 204800);
        PICTURE_SIZE_TABLE.put("1600x1200-fine", 245760);
        PICTURE_SIZE_TABLE.put("1600x1200-superfine", 368640);

        PICTURE_SIZE_TABLE.put("1280x960-normal", 163840);
        PICTURE_SIZE_TABLE.put("1280x960-fine", 196608);
        PICTURE_SIZE_TABLE.put("1280x960-superfine", 262144);

        PICTURE_SIZE_TABLE.put("1024x768-normal", 102400);
        PICTURE_SIZE_TABLE.put("1024x768-fine", 122880);
        PICTURE_SIZE_TABLE.put("1024x768-superfine", 163840);

        PICTURE_SIZE_TABLE.put("640x480-normal", 30720);
        PICTURE_SIZE_TABLE.put("640x480-fine", 30720);
        PICTURE_SIZE_TABLE.put("640x480-superfine", 30720);

        PICTURE_SIZE_TABLE.put("320x240-normal", 13312);
        PICTURE_SIZE_TABLE.put("320x240-fine", 13312);
        PICTURE_SIZE_TABLE.put("320x240-superfine", 13312);
        //start add

        PICTURE_SIZE_TABLE.put("1600x912-normal", 163840);
        PICTURE_SIZE_TABLE.put("1600x912-fine", 196608);
        PICTURE_SIZE_TABLE.put("1600x912-superfine", 262144);

        PICTURE_SIZE_TABLE.put("2048x1152-normal", 196608);
        PICTURE_SIZE_TABLE.put("2048x1152-fine", 245760);
        PICTURE_SIZE_TABLE.put("2048x1152-superfine", 368640);

        PICTURE_SIZE_TABLE.put("1600x960-normal", 163840);
        PICTURE_SIZE_TABLE.put("1600x960-fine", 196608);
        PICTURE_SIZE_TABLE.put("1600x960-superfine", 294912);
        PICTURE_SIZE_TABLE.put("1920x1088-normal", 222822);
        PICTURE_SIZE_TABLE.put("1920x1088-fine", 196608);
        PICTURE_SIZE_TABLE.put("1920x1088-superfine", 401080);

        PICTURE_SIZE_TABLE.put("1024x688-normal", 102400);
        PICTURE_SIZE_TABLE.put("1024x688-fine", 122880);
        PICTURE_SIZE_TABLE.put("1024x688-superfine", 163840);

        PICTURE_SIZE_TABLE.put("1280x864-normal", 131072);
        PICTURE_SIZE_TABLE.put("1280x864-fine", 157286);
        PICTURE_SIZE_TABLE.put("1280x864-superfine", 209715);

        PICTURE_SIZE_TABLE.put("1440x960-normal", 184320);
        PICTURE_SIZE_TABLE.put("1440x960-fine", 221184);
        PICTURE_SIZE_TABLE.put("1440x960-superfine", 294912);

        PICTURE_SIZE_TABLE.put("1728x1152-normal", 221184);
        PICTURE_SIZE_TABLE.put("1728x1152-fine", 265420);
        PICTURE_SIZE_TABLE.put("1728x1152-superfine", 353894);

        PICTURE_SIZE_TABLE.put("2048x1360-normal", 232107);
        PICTURE_SIZE_TABLE.put("2048x1360-fine", 290133);
        PICTURE_SIZE_TABLE.put("2048x1360-superfine", 435199);

        PICTURE_SIZE_TABLE.put("2560x1712-normal", 292181);
        PICTURE_SIZE_TABLE.put("2560x1712-fine", 438271);
        PICTURE_SIZE_TABLE.put("2560x1712-superfine", 547840);

        PICTURE_SIZE_TABLE.put("3072x1728-normal", 353539);
        PICTURE_SIZE_TABLE.put("3072x1728-fine", 530307);
        PICTURE_SIZE_TABLE.put("3072x1728-superfine", 662886);
        PICTURE_SIZE_TABLE.put("5280x2992-normal", 1053182);
        PICTURE_SIZE_TABLE.put("5280x2992-fine", 1579772);
        PICTURE_SIZE_TABLE.put("5280x2992-superfine", 1974720);

        PICTURE_SIZE_TABLE.put("5312x2944-normal", 1042566);
        PICTURE_SIZE_TABLE.put("5312x3944-fine", 159908);
        PICTURE_SIZE_TABLE.put("5312x3944-superfine", 1945927);

        PICTURE_SIZE_TABLE.put("5632x4224-normal", 1585958);
        PICTURE_SIZE_TABLE.put("5632x4224-fine", 2378934);
        PICTURE_SIZE_TABLE.put("5632x4224-superfine", 2973675);

        //end add
        PICTURE_SIZE_TABLE.put("autorama", 163840);

        PICTURE_SIZE_TABLE.putDefault(1500000);
    }

    private Utils() {

    }

    public static int getImageSize(String format) {
        return PICTURE_SIZE_TABLE.get(format);
    }

    public static void initialize(Context context) {
        sImageFileNamer = new ImageFileNamer(
                context.getString(R.string.image_file_name_format));
    }

    public static double findFullscreenRatio(Context context) {
        double find = 1.3333;
        if (context != null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);

            double fullscreen;
            if (point.x > point.y) {
                fullscreen = (double) point.x / point.y;
            } else {
                fullscreen = (double) point.y / point.x;
            }
            Log.i(TAG, "fullscreen = " + fullscreen + " x = " + point.x + " y = " + point.y);
            for (int i = 0; i < RATIOS.length; i++) {
                if (Math.abs(RATIOS[i] - fullscreen) < Math.abs(fullscreen - find)) {
                    find = RATIOS[i];
                }
            }
        }
        return find;
    }

    public static CameraCharacteristics getCameraCharacteristics(Activity activity,
            String cameraId) {
        CameraManager camManager = (CameraManager)
                activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            return camManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.i(TAG, "CameraCharacteristics exception : ");
            e.printStackTrace();
        }
        return null;
    }

    public static Size filterSupportedSize(List<Size> supportedSizes, Size targetSize, Size bound) {
        if (supportedSizes == null || supportedSizes.size() <= 0) {
            return null;
        }
        Comparator<Size> comparator = new SizeComparator();
        Size selectedSize = targetSize;
        if (bound == null) {
            bound = targetSize;
        }
        for (Size sz : supportedSizes) {
            if (comparator.compare(sz, bound) <= 0 && checkAspectRatiosMatch(sz, selectedSize)) {
                selectedSize = sz;
            }
        }
        return selectedSize;
    }

    public static boolean isSameAspectRatio(Size sizeLeft, Size sizeRight) {
        if (sizeLeft == null || sizeRight == null) {
            return false;
        }
        float leftRatio = ((float) sizeLeft.getWidth()) / sizeLeft.getHeight();
        float rightRatio = ((float) sizeRight.getWidth()) / sizeRight.getHeight();
        return Math.abs(leftRatio - rightRatio) < 0.0001;
    }

    /**
     * Shared size comparison method used by size comparators.
     *
     * <p>Compares the number of pixels it covers.If two the areas of two sizes are same, compare
     * the widths.</p>
     */
    public static boolean compareSize(Size sizeLeft, Size sizeRight) {
        Comparator<Size> comparator = new SizeComparator();
        return comparator.compare(sizeLeft, sizeRight) >= 0;
    }

    public static List<Size> getSizeList(List<String> sizeStringList) {
        if (sizeStringList == null || sizeStringList.size() <= 0) {
            return null;
        }
        List<Size> sizeList = new ArrayList<Size>(sizeStringList.size());
        for (String size:sizeStringList) {
            sizeList.add(getSize(size));
        }
        return sizeList;
    }

    public static String buildSize(Size size) {
        if (size != null) {
            return "" + size.getWidth() + "x" + size.getHeight();
        } else {
            return "null";
        }
    }

    public static Size getSize(String sizeString) {
        Size size = null;
        int index = sizeString.indexOf('x');
        if (index != UNKNOWN) {
            int width = Integer.parseInt(sizeString.substring(0, index));
            int height = Integer.parseInt(sizeString.substring(index + 1));
            size = new Size(width, height);
        }
        Log.d(TAG, "getSize(" + sizeString + ") return " + size);
        return size;
    }

    public static Size getOptimalPreviewSize(Context context,
            List<Size> sizes, double targetRatio) {
        if (sizes == null) {
            return null;
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        int targetHeight = Math.min(point.x, point.y);
        int targetWidth = Math.max(point.x, point.y);

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        double minDiffWidth = Double.MAX_VALUE;
        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
                minDiffWidth = Math.abs(size.getWidth() - targetWidth);
            } else if ((Math.abs(size.getHeight() - targetHeight) == minDiff)
                    && Math.abs(size.getWidth() - targetWidth) < minDiffWidth) {
                optimalSize = size;
                minDiffWidth = Math.abs(size.getWidth() - targetWidth);
            }
        }

        return optimalSize;
    }

    /**
     * Given the device orientation and Camera2 characteristics, this returns
     * the required JPEG rotation for this camera.
     *
     * @param deviceOrientation the device orientation in degrees.
     * @return The JPEG orientation in degrees.
     */
    public static int getJpegRotation(int deviceOrientation,
            CameraCharacteristics characteristics) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Reverse device orientation for front-facing cameras
        boolean facingFront = characteristics.get(
                CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        Log.i(TAG, "getJpegRotation : " + jpegOrientation);
        return jpegOrientation;
    }

    public static int getRecordingRotation(int orientation,
            CameraCharacteristics characteristics) {
        int rotation = -1;
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // Reverse device orientation for front-facing cameras
        boolean facingFront = characteristics.get(CameraCharacteristics.LENS_FACING)
                == CameraCharacteristics.LENS_FACING_FRONT;

        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            if (facingFront) {
                rotation = (sensorOrientation - orientation + 360) % 360;
            } else { // back-facing camera
                rotation = (sensorOrientation + orientation) % 360;
            }
        } else {
            // Get the right original orientation
            rotation = sensorOrientation;
        }
        return rotation;
    }

    public static void setRotatableOrientation(View view, int orientation, boolean animation) {
        if (view == null) {
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(orientation, animation);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                setRotatableOrientation(group.getChildAt(i), orientation, animation);
            }
        }
    }

    public static int getDisplayRotation(Context context) {
        WindowManager windowManager = (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    /**
     * Given an image reader, extracts the JPEG image bytes and then closes the
     * reader.
     *
     * @param reader the reader to read the JPEG data from.
     * @return The bytes of the JPEG image. Newly allocated.
     */
    public static byte[] acquireJpegBytesAndClose(Image image) {
        Assert.assertNotNull(image);
        ByteBuffer buffer;
        if (image.getFormat() == ImageFormat.JPEG) {
            Image.Plane plane0 = image.getPlanes()[0];
            buffer = plane0.getBuffer();
        } else {
            throw new RuntimeException("Unsupported image format.");
        }
        byte[] imageBytes = new byte[buffer.remaining()];
        buffer.get(imageBytes);
        buffer.rewind();
        image.close();
        return imageBytes;
    }

    /**
     * conver the raw image data to dng format
     */
    public static ByteArrayOutputStream convertRawToDng(Image rawImage,
            CameraCharacteristics cs, TotalCaptureResult captureResult) {
        ByteArrayOutputStream outputStream = null;
        try {
            DngCreator dngCreator = new DngCreator(cs, captureResult);
            outputStream = new ByteArrayOutputStream();
            dngCreator.writeImage(outputStream, rawImage);
        } catch (IOException ex) {
            Log.e(TAG, "convertRawToDng, dng write error");
        } finally {
            Log.i(TAG, "convertRawToDng");
        }
        return outputStream;
    }

    /**
     * Read continuous byte from image when rowStride != pixelStride * width
     */
    public static byte[] getContinuousRGBADataFromImage(Image image) {
        Log.i(TAG, "getContinuousRGBADataFromImage begin");
        if (image.getFormat() != PipEGLConfigWrapper.getInstance().getPixelFormat()) {
            Log.i(TAG, "error format = " + image.getFormat());
            return null;
        }
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride, pixelStride;
        byte[] data = null;
        Plane[] planes = image.getPlanes();
        if (format == PipEGLConfigWrapper.getInstance().getPixelFormat()) {
            PixelFormat pixelInfo = new PixelFormat();
            PixelFormat.getPixelFormatInfo(format, pixelInfo);
            ByteBuffer buffer = planes[0].getBuffer();
            rowStride = planes[0].getRowStride();
            pixelStride = planes[0].getPixelStride();
            data = new byte[width * height * pixelInfo.bitsPerPixel / 8];
            int offset = 0;
            int rowPadding = rowStride - pixelStride * width;
            // this format, pixelStride == bytesPerPixel, so read of the entire
            // row
            for (int y = 0; y < height; y++) {
                int length = width * pixelStride;
                buffer.get(data, offset, length);
                // Advance buffer the remainder of the row stride
                buffer.position(buffer.position() + rowPadding);
                offset += length;
            }
        }
        Log.i(TAG, "getContinuousRGBADataFromImage end");
        return data;
    }

    public static CamcorderProfile getVideoProfile(int cameraId, int quality) {
        return CamcorderProfileEx.getProfile(cameraId, quality);
    }

    public static String createJpegName(long dateTaken) {
        synchronized (sImageFileNamer) {
            return sImageFileNamer.generateName(dateTaken);
        }
    }

    public static String createDngName(long dateTaken) {
        synchronized (sImageFileNamer) {
            return sImageFileNamer.generateDngName(dateTaken);
        }
    }

    /**
     * Build string which is composed of elements of the list with semicolon
     * between them. For example, if the input list array is "[auto,portrait]",
     * the returned string is "auto;portrait".
     * @param list The array of values.
     * @return Return the builded string.
     */
    public static String buildEnableList(String[] list) {
        if (list == null) {
            return null;
        }
        String listStr = null;
        if (list != null) {
            listStr = "";
            List<String> uniqueList = new ArrayList<String>();
            for (int i = 0, len = list.length; i < len; i++) {
                if (uniqueList.contains(list[i])) {
                    continue;
                }
                uniqueList.add(list[i]);
                if (i == (len - 1)) {
                    listStr += list[i];
                } else {
                    listStr += (list[i] + ENABLE_LIST_SEPERATOR);
                }
            }
        }
        Log.d(TAG, "buildEnableList, return " + listStr);
        return listStr;
    }

    public static String buildSize(int width, int height) {
        return "" + width + "x" + height;
    }

    public static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
            t.printStackTrace();
        }
    }

    /**
     * Calculates sensor crop region for a zoom level (zoom >= 1.0).
     *
     * @return Crop region.
     */
    public static Rect cropRegionForZoom(Activity activity, String cameraId, float zoom) {
        CameraCharacteristics characteristics = getCameraCharacteristics(activity, cameraId);
        Rect sensor = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int xCenter = sensor.width() / 2;
        int yCenter = sensor.height() / 2;
        int xDelta = (int) (0.5f * sensor.width() / zoom);
        int yDelta = (int) (0.5f * sensor.height() / zoom);
        return new Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
            if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = computeSampleSize(options, -1, maxNumOfPixels);
            options.inJustDecodeBounds = false;

            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
        } catch (OutOfMemoryError ex) {
            Log.e(TAG, "Got oom exception ", ex);
            return null;
        }
    }

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

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength,
            int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels < 0) ? 1 : (int) Math.ceil(Math.sqrt(w * h
                / maxNumOfPixels));
        int upperBound = (minSideLength < 0) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
                Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound;
        }

        if (maxNumOfPixels < 0 && minSideLength < 0) {
            return 1;
        } else if (minSideLength < 0) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }

    // Rotates the bitmap by the specified degree.
    // If a new bitmap is created, the original bitmap is recycled.
    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    // Rotates and/or mirrors the bitmap. If a new bitmap is created, the
    // original bitmap is recycled.
    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees != 0 || mirror) && b != null) {
            Matrix m = new Matrix();
            // Mirror first.
            // horizontal flip + rotation = -rotation + horizontal flip
            if (mirror) {
                m.postScale(-1, 1);
                degrees = (degrees + 360) % 360;
                if (degrees == 0 || degrees == 180) {
                    m.postTranslate(b.getWidth(), 0);
                } else if (degrees == 90 || degrees == 270) {
                    m.postTranslate(b.getHeight(), 0);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + degrees);
                }
            }
            if (degrees != 0) {
                // clockwise
                m.postRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            }

            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
                ex.printStackTrace();
            }
        }
        return b;
    }

    private static class ImageFileNamer {
        private final SimpleDateFormat mFormat;

        // The date (in milliseconds) used to generate the last name.
        private long mLastDate;

        // Number of names generated for the same second.
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);

            // If the last name was generated for the same second,
            // we append _1, _2, etc to the name.
            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }

        public String generateDngName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);
            return result;
        }
    }
    public static Bitmap createBitmapFromVideo(String filePath,
            FileDescriptor fd, int targetWidth) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Log.i(TAG, "filePath = " + filePath + " : fd = " + fd
                + " : targetWidth  = " + targetWidth);
        try {
            if (filePath != null) {
                retriever.setDataSource(filePath);
            } else {
                retriever.setDataSource(fd);
            }
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
            Log.i("mmTAG", "IllegalArgumentException");
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            Log.i("mmTAG", "RuntimeException");
            // Assume this is a corrupt video file.
            ex.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
                ex.printStackTrace();
            }
        }
        if (bitmap == null) {
            Log.i("mmTAG", "bitmap = null");
            return null;
        }

        // Scale down the bitmap if it is bigger than we need.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.v("mmTAG", "bitmap = " + width + "x" + height + "   targetWidth=" + targetWidth);
        if (width > targetWidth) {
            float scale = (float) targetWidth / width;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            Log.v(TAG, "w = " + w + "h" + h);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return rotateAndMirror(bitmap, 0, false);
    }
    @SuppressWarnings("serial")
    private static class DefaultHashMap<K, V> extends HashMap<K, V> {
        private V mDefaultValue;

        public void putDefault(V defaultValue) {
            mDefaultValue = defaultValue;
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            return (value == null) ? mDefaultValue : value;
        }
    }

    /**
     * Size comparator that compares the number of pixels it covers.
     *
     * <p>If two the areas of two sizes are same, compare the widths.</p>
     */
    private static class SizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return compareSizes(lhs.getWidth(), lhs.getHeight(), rhs.getWidth(), rhs.getHeight());
        }
    }

    private static int compareSizes(int widthA, int heightA, int widthB, int heightB) {
        long left = widthA * (long) heightA;
        long right = widthB * (long) heightB;
        if (left == right) {
            left = widthA;
            right = widthB;
        }
        return (left < right) ? -1 : (left > right ? 1 : 0);
    }

    private static boolean checkAspectRatiosMatch(Size a, Size b) {
        float aAspect = a.getWidth() / (float) a.getHeight();
        float bAspect = b.getWidth() / (float) b.getHeight();
        return Math.abs(aAspect - bAspect) < ASPECT_TOLERANCE;
    }
}
