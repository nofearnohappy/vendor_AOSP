/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.v2.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.v2.ui.Rotatable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of utility functions used in this package.
 */
public class CameraUtil {
    private static final String TAG = "CameraUtil";

    public static final String RESET_STATE_VALUE_DISABLE = "disable-value";
    /** Orientation hysteresis amount used in rounding, in degrees. */
    public static final int ORIENTATION_HYSTERESIS = 5;

    private static final String ENABLE_LIST_HEAD = "[L];";
    private static final String ENABLE_LIST_SEPERATOR = ";";
    private static final float  ALPHA_ENABLE = 1.0F;
    private static final float ALPHA_DISABLE = 0.3F;
    private static final double[] RATIOS = new double[] { 1.3333, 1.5, 1.6667, 1.7778 };

    public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
    /** See android.hardware.Camera.ACTION_NEW_PICTURE. */
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
    /** See android.hardware.Camera.ACTION_NEW_VIDEO. */
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";

    public static final String ACTION_STEREO3D = "android.media.action.IMAGE_CAPTURE_3D";

    private CameraUtil() {
    }

    public static int[] getRelativeLocation(View reference, View view) {
        int location[] = new int[2];
        reference.getLocationInWindow(location);
        int referenceX = location[0];
        int referenceY = location[1];
        view.getLocationInWindow(location);
        location[0] -= referenceX;
        location[1] -= referenceY;
        return location;
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

    public static void setOrientation(View view, int orientation, boolean animation) {
        if (view == null) {
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(orientation, animation);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                setOrientation(group.getChildAt(i), orientation, animation);
            }
        }
    }

    /**
     * Given the device orientation and Camera2 characteristics, this returns
     * the required JPEG rotation for this camera.
     *
     * @param deviceOrientationDegrees the device orientation in degrees.
     * @return The JPEG orientation in degrees.
     */
    public static int getJpegRotation(int deviceOrientationDegrees,
            CameraCharacteristics characteristics) {
        if (deviceOrientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0;
        }
        int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (facing == CameraMetadata.LENS_FACING_FRONT) {
            return (sensorOrientation + deviceOrientationDegrees) % 360;
        } else {
            return (sensorOrientation - deviceOrientationDegrees + 360) % 360;
        }
    }

    public static int getDisplayRotation(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay()
                .getRotation();
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

    public static void setEnabledState(View view, boolean enabled) {
        if (view != null) {
            float alpha = enabled ? ALPHA_ENABLE : ALPHA_DISABLE;
            view.setAlpha(alpha);
        }
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a == null ? false : a.equals(b));
    }

    /**
     * Build a new string which is composed of "[L]:", the input value, ";" and
     * the input list string. For example, if the input list string is "auto;portrait"
     * the input value is "portrait", the returned string is "[L]:auto;auto;portrait".
     * @param listString The original string.
     * @param value The value which is placed in the first position.
     * @return Return the builded string.
     */
    public static String buildEnabledList(String listString, String value) {
        if (listString == null) {
            return null;
        }
        String[] temp = listString.split(ENABLE_LIST_SEPERATOR);
        if (temp.length < 2) {
            return listString;
        } else {
            return new StringBuilder().append(ENABLE_LIST_HEAD).append(value)
                    .append(ENABLE_LIST_SEPERATOR).append(listString).toString();
        }

    }

    public static boolean isBuiltList(String listString) {
        boolean isList = false;
        if (listString != null && listString.startsWith(ENABLE_LIST_HEAD)) {
            isList = true;
        }
        Log.d(TAG, "isBuiltList(" + listString + ") return " + isList);
        return isList;
    }

    public static List<String> getEnabledList(String listString) {
        ArrayList<String> list = new ArrayList<String>();
        if (isBuiltList(listString)) {
            String[] temp = listString.split(ENABLE_LIST_SEPERATOR);
            for (int i = 2, len = temp.length; i < len; i++) {
                if (!list.contains(temp[i])) {
                    list.add(temp[i]);
                }
            }
        }
        Log.d(TAG, "getEnabledList(" + listString + ") return " + list);
        return list;
    }

    public static String getDefaultValue(String listString) {
        String value = null;
        if (isBuiltList(listString)) {
            String[] temp = listString.split(ENABLE_LIST_SEPERATOR);
            if (temp != null && temp.length > 1) {
                value = temp[1];
            }
        }
        Log.i(TAG, "getDefaultValue(" + listString + ") return " + value);
        return value;
    }

    public static boolean isDisableValue(String value) {
        boolean reset = false;
        if (RESET_STATE_VALUE_DISABLE.equals(value)) {
            reset = true;
        }
        Log.d(TAG, "isResetValue(" + value + ") return " + reset);
        return reset;
    }

    public static int getMainColor(Context context) {
        // M: For 4.4 migration because resource remove interface
        // int themeColor = 0;
        int finalColor = 0;
        // if (FeatureSwitcher.isThemeEnabled()) {
        // Resources res = context.getResources();
        // themeColor = res.getThemeMainColor();
        // }
        // if (themeColor == 0) {
        finalColor = context.getResources().getColor(R.color.setting_item_text_color_highlight);
        // } else {
        // finalColor = themeColor;
        // }
        Log.d(TAG, "getMainColor" + finalColor);
        return finalColor;
    }

    /**
     * Calculate device screen ratio.
     * @param context The activity context.
     * @return Return the device screen ratio.
     */
    public static double findFullscreenRatio(Context context) {
        double find = 4d / 3;
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
        Log.d(TAG, "fullscreen = " + fullscreen + " x = " + point.x + " y = " + point.y);
        for (int i = 0; i < RATIOS.length; i++) {
            if (Math.abs(RATIOS[i] - fullscreen) < Math.abs(fullscreen - find)) {
                find = RATIOS[i];
            }
        }
        Log.d(TAG, "findFullscreenRatio, return ratio:" + find);
        return find;
    }

    public static int index(CharSequence[] list, String value) {
        int findIndex = -1;
        if (list != null && value != null) {
            for (int i = 0, len = list.length; i < len; i++) {
                if (value.equals(list[i])) {
                    findIndex = i;
                    break;
                }
            }
        }
        Log.d(TAG, "index(" + list + ", " + value + ") return " + findIndex);
        return findIndex;
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

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null) {
            return false;
        }

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Log.e(TAG, "Fail to open URI. URI=" + uri);
                return false;
            }
            pfd.close();
        } catch (IOException ex) {
            return false;
        }
        return isMountPointValid(uri, resolver);
    }

    /**
     * @param mimeType The MIME type to check.
     * @return Whether the MIME is a video type.
     */
    public static boolean isMimeTypeVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * @param mimeType The MIME type to check.
     * @return Whether the MIME is a image type.
     */
    public static boolean isMimeTypeImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
        // Keep compatibility
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }

    public static void broadcastNewVideo(Context context, Uri uri) {
        context.sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri));
    }

    private static boolean isMountPointValid(Uri uri, ContentResolver resolver) {
        String path = "";
        Cursor cursor = resolver.query(uri, new String[] { MediaStore.MediaColumns.DATA }, null,
                null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    path = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }

        String directory = Storage.getMountPoint() + Storage.FOLDER_PATH;
        File file = new File(path);
        String parent = file.getParent();
        boolean valid = directory.equals(parent);
        Log.d(TAG, "isMountPointValid(" + uri + ") path =" + path + ", Storage.MOUNT_POINT ="
                + Storage.getMountPoint() + ", return " + valid);
        return valid;
    }
}
