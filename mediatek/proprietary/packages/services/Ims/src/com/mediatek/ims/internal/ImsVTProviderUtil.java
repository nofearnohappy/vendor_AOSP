/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ims.internal;

import android.telecom.VideoProfile.CameraCapabilities;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.view.Surface;

import android.hardware.camera2.CameraCharacteristics;
import android.telecom.VideoProfile;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.lang.Integer;

import com.mediatek.ims.internal.ImsVTProvider;

public class ImsVTProviderUtil {

    private static final String TAG = "ImsVTProviderUtil";

    public static final int HIDE_ME_TYPE_NONE           = 0;
    public static final int HIDE_ME_TYPE_DISABLE        = 1;
    public static final int HIDE_ME_TYPE_FREEZE         = 2;
    public static final int HIDE_ME_TYPE_PICTURE        = 3;

    public static final int HIDE_YOU_TYPE_DISABLE       = 0;
    public static final int HIDE_YOU_TYPE_ENABLE        = 1;

    public static final int TURN_OFF_CAMERA             = -1;
    public static final int DUMMY_CAMERA                = 255; // for Loopback call end event

    public static class Size {
        /**
         * Sets the dimensions for pictures.
         *
         * @param w the photo width (pixels)
         * @param h the photo height (pixels)
         */
        public Size(int w, int h) {
            width = w;
            height = h;
        }

        /**
         * Compares {@code obj} to this size.
         *
         * @param obj the object to compare this size with.
         * @return {@code true} if the width and height of {@code obj} is the
         *         same as those of this size. {@code false} otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            return width == s.width && height == s.height;
        }

        @Override
        public int hashCode() {
            return width * 32713 + height;
        }

        /** width of the picture */
        public int width;
        /** height of the picture */
        public int height;
    };

    public class ParameterSet {
        // Parameter keys to communicate with the camera driver.
        public static final String KEY_PREVIEW_SIZE = "preview-size";
        public static final String KEY_PREVIEW_FORMAT = "preview-format";
        public static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
        public static final String KEY_PICTURE_SIZE = "picture-size";
        public static final String KEY_PICTURE_FORMAT = "picture-format";
        public static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        public static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        public static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        public static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        public static final String KEY_JPEG_QUALITY = "jpeg-quality";
        public static final String KEY_ROTATION = "rotation";
        public static final String KEY_GPS_LATITUDE = "gps-latitude";
        public static final String KEY_GPS_LONGITUDE = "gps-longitude";
        public static final String KEY_GPS_ALTITUDE = "gps-altitude";
        public static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        public static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
        public static final String KEY_WHITE_BALANCE = "whitebalance";
        public static final String KEY_EFFECT = "effect";
        public static final String KEY_ANTIBANDING = "antibanding";
        public static final String KEY_SCENE_MODE = "scene-mode";
        public static final String KEY_FLASH_MODE = "flash-mode";
        public static final String KEY_FOCUS_MODE = "focus-mode";
        public static final String KEY_FOCAL_LENGTH = "focal-length";
        public static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
        public static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
        public static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
        public static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
        public static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
        public static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        public static final String KEY_ZOOM = "zoom";
        public static final String KEY_MAX_ZOOM = "max-zoom";
        public static final String KEY_ZOOM_RATIOS = "zoom-ratios";
        public static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
        public static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
        public static final String KEY_FOCUS_METER = "focus-meter";
        public static final String KEY_ISOSPEED_MODE = "iso-speed";
        public static final String KEY_EXPOSURE = "exposure";
        public static final String KEY_EXPOSURE_METER = "exposure-meter";
        public static final String KEY_FD_MODE = "fd-mode";
        public static final String KEY_EDGE_MODE = "edge";
        public static final String KEY_HUE_MODE = "hue";
        public static final String KEY_SATURATION_MODE = "saturation";
        public static final String KEY_BRIGHTNESS_MODE = "brightness";
        public static final String KEY_CONTRAST_MODE = "contrast";
        public static final String KEY_CAPTURE_MODE = "cap-mode";
        public static final String KEY_CAPTURE_PATH = "capfname";
        public static final String KEY_BURST_SHOT_NUM = "burst-num";

        // Parameter key suffix for supported values.
        public static final String SUPPORTED_VALUES_SUFFIX = "-values";

        // Values for white balance settings.
        public static final String WHITE_BALANCE_AUTO = "auto";
        public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
        public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
        public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
        public static final String WHITE_BALANCE_TWILIGHT = "twilight";
        public static final String WHITE_BALANCE_SHADE = "shade";

        // Values for color effect settings.
        public static final String EFFECT_NONE = "none";
        public static final String EFFECT_MONO = "mono";
        public static final String EFFECT_NEGATIVE = "negative";
        public static final String EFFECT_SOLARIZE = "solarize";
        public static final String EFFECT_SEPIA = "sepia";
        public static final String EFFECT_POSTERIZE = "posterize";
        public static final String EFFECT_WHITEBOARD = "whiteboard";
        public static final String EFFECT_BLACKBOARD = "blackboard";
        public static final String EFFECT_AQUA = "aqua";

        // Values for antibanding settings.
        public static final String ANTIBANDING_AUTO = "auto";
        public static final String ANTIBANDING_50HZ = "50hz";
        public static final String ANTIBANDING_60HZ = "60hz";
        public static final String ANTIBANDING_OFF = "off";

        public static final String CONTRAST_HIGH = "high";
        public static final String CONTRAST_MIDDLE = "middle";
        public static final String CONTRAST_LOW = "low";

        // Values for flash mode settings.
        public static final String FLASH_MODE_OFF = "off";
        public static final String FLASH_MODE_AUTO = "auto";
        public static final String FLASH_MODE_ON = "on";
        public static final String FLASH_MODE_RED_EYE = "red-eye";
        public static final String FLASH_MODE_TORCH = "torch";

        // Values for scene mode settings.
        public static final String SCENE_MODE_AUTO = "auto";
        public static final String SCENE_MODE_ACTION = "action";
        public static final String SCENE_MODE_PORTRAIT = "portrait";
        public static final String SCENE_MODE_LANDSCAPE = "landscape";
        public static final String SCENE_MODE_NIGHT = "night";
        public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
        public static final String SCENE_MODE_THEATRE = "theatre";
        public static final String SCENE_MODE_BEACH = "beach";
        public static final String SCENE_MODE_SNOW = "snow";
        public static final String SCENE_MODE_SUNSET = "sunset";
        public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
        public static final String SCENE_MODE_FIREWORKS = "fireworks";
        public static final String SCENE_MODE_SPORTS = "sports";
        public static final String SCENE_MODE_PARTY = "party";
        public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
        public static final String SCENE_MODE_BARCODE = "barcode";

        // Values for focus mode settings.
        public static final String FOCUS_MODE_AUTO = "auto";
        public static final String FOCUS_MODE_INFINITY = "infinity";
        public static final String FOCUS_MODE_MACRO = "macro";
        public static final String FOCUS_MODE_FIXED = "fixed";
        public static final String FOCUS_MODE_EDOF = "edof";

        // Values for capture mode settings.
        public static final String CAPTURE_MODE_NORMAL = "normal";
        public static final String CAPTURE_MODE_BEST_SHOT = "bestshot";
        public static final String CAPTURE_MODE_EV_BRACKET_SHOT = "evbracketshot";
        public static final String CAPTURE_MODE_BURST_SHOT = "burstshot";

        // Formats for setPreviewFormat and setPictureFormat.
        private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
        private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
        private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
        private static final String PIXEL_FORMAT_RGB565 = "rgb565";
        private static final String PIXEL_FORMAT_JPEG = "jpeg";

        private HashMap<String, String> mMap;

        /**
         * Handles the picture size (dimensions).
         */


        public ParameterSet() {
            mMap = new HashMap<String, String>();
        }

        public void dump() {
            Log.e(TAG, "dump: size=" + mMap.size());
            for (String k : mMap.keySet()) {
                Log.e(TAG, "dump: " + k + "=" + mMap.get(k));
            }
        }

        public String flatten() {
            StringBuilder flattened = new StringBuilder();
            for (String k : mMap.keySet()) {
                flattened.append(k);
                flattened.append("=");
                flattened.append(mMap.get(k));
                flattened.append(";");
            }
            // chop off the extra semicolon at the end
            flattened.deleteCharAt(flattened.length() - 1);
            return flattened.toString();
        }

        public void unflatten(String flattened) {
            mMap.clear();

            StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
            while (tokenizer.hasMoreElements()) {
                String kv = tokenizer.nextToken();
                int pos = kv.indexOf('=');
                if (pos == -1) {
                    continue;
                }
                String k = kv.substring(0, pos);
                String v = kv.substring(pos + 1);
                mMap.put(k, v);
            }
        }

        public void remove(String key) {
            mMap.remove(key);
        }


        public void set(String key, String value) {
            if (key.indexOf('=') != -1 || key.indexOf(';') != -1) {
                Log.e(TAG, "Key \"" + key + "\" contains invalid character (= or ;)");
                return;
            }
            if (value.indexOf('=') != -1 || value.indexOf(';') != -1) {
                Log.e(TAG, "Value \"" + value + "\" contains invalid character (= or ;)");
                return;
            }

            mMap.put(key, value);
        }

        public void set(String key, int value) {
            mMap.put(key, Integer.toString(value));
        }

        public String get(String key) {
            return mMap.get(key);
        }

        public int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(mMap.get(key));
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        public float getFloat(String key, float defaultValue) {
            try {
                return Float.parseFloat(mMap.get(key));
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        public Size getSize(String key) {
            return strToSize(get(key));
        }

        public List<String> getStrList(String key) {
            return split(get(key));
        }

        public List<Integer> getIntList(String key) {
            return splitInt(get(key));
        }

        public List<Size> getSizeList(String key) {
            return splitSize(get(key));
        }


        // Splits a comma delimited string to an ArrayList of String.
        // Return null if the passing string is null or the size is 0.
        private ArrayList<String> split(String str) {
            if (str == null) {
                return null;
            }
            // Use StringTokenizer because it is faster than split.
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList<String> substrings = new ArrayList<String>();
            while (tokenizer.hasMoreElements()) {
                substrings.add(tokenizer.nextToken());
            }
            return substrings;
        }

        // Splits a comma delimited string to an ArrayList of Integer.
        // Return null if the passing string is null or the size is 0.
        private ArrayList<Integer> splitInt(String str) {
            if (str == null) {
                return null;
            }
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList<Integer> substrings = new ArrayList<Integer>();
            while (tokenizer.hasMoreElements()) {
                String token = tokenizer.nextToken();
                substrings.add(Integer.parseInt(token));
            }
            if (substrings.size() == 0) {
                return null;
            }
            return substrings;
        }

        // Splits a comma delimited string to an ArrayList of Size.
        // Return null if the passing string is null or the size is 0.
        private ArrayList<Size> splitSize(String str) {
            if (str == null) {
                return null;
            }
            StringTokenizer tokenizer = new StringTokenizer(str, ",");
            ArrayList<Size> sizeList = new ArrayList<Size>();
            while (tokenizer.hasMoreElements()) {
                Size size = strToSize(tokenizer.nextToken());
                if (size != null) {
                    sizeList.add(size);
                }
            }
            if (sizeList.size() == 0) {
                return null;
            }
            return sizeList;
        }

        // Parses a string (ex: "480x320") to Size object.
        // Return null if the passing string is null.
        private Size strToSize(String str) {
            if (str == null) {
                return null;
            }
            int pos = str.indexOf('x');
            if (pos != -1) {
                String width = str.substring(0, pos);
                String height = str.substring(pos + 1);
                return new Size(Integer.parseInt(width),
                                Integer.parseInt(height));
            }
            Log.e(TAG, "Invalid size parameter string=" + str);
            return null;
        }
    }

    public static class ImsVTMessagePacker {

        public String packFromVdoProfile(VideoProfile videoProfile) {

            StringBuilder flattened = new StringBuilder();

            flattened.append("mVideoState");
            flattened.append("=");
            flattened.append("" + videoProfile.getVideoState());
            flattened.append(";");
            flattened.append("mQuality");
            flattened.append("=");
            flattened.append("" + videoProfile.getQuality());
            flattened.append(";");

            // chop off the extra semicolon at the end
            flattened.deleteCharAt(flattened.length() - 1);

            Log.d(TAG, "[packFromVdoProfile] profile = " + flattened.toString());

            return flattened.toString();
        }

        public VideoProfile unPackToVdoProfile(String flattened) {

            Log.d(TAG, "[unPackToVdoProfile] flattened = " + flattened);

            StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
            int state = VideoProfile.STATE_BIDIRECTIONAL;
            int qty = VideoProfile.QUALITY_DEFAULT;

            while (tokenizer.hasMoreElements()) {
                String kv = tokenizer.nextToken();
                int pos = kv.indexOf('=');
                if (pos == -1) {
                    continue;
                }
                String k = kv.substring(0, pos);
                String v = kv.substring(pos + 1);

                Log.d(TAG, "[unPackToVdoProfile] k = " + k + ", v = " + v);

                if (k.equals("mVideoState")) {
                    state = Integer.valueOf(v).intValue();
                } else if (k.equals("mQuality")) {
                    qty = Integer.valueOf(v).intValue();
                }
            }
            Log.d(TAG, "[unPackToVdoProfile] state = " + state + ", qty = " + qty);
            return new VideoProfile(state, qty);
        }

    }

    private static ParameterSet                     mParamSet;
    private static ImsVTMessagePacker               mPacker = new ImsVTMessagePacker();
    private static Map<String, Object>              mProviderById = new HashMap<>();
    private static Map<String, Object>              mSurfaceStatusById = new HashMap<>();

    ImsVTProviderUtil() {
        mParamSet = new ParameterSet();
    }

    public static ParameterSet getSetting() {
        return mParamSet;
    }

    public static String packFromVdoProfile(VideoProfile VideoProfile) {
        return mPacker.packFromVdoProfile(VideoProfile);
    }

    public static VideoProfile unPackToVdoProfile(String flattened) {
        return mPacker.unPackToVdoProfile(flattened);
    }

    public static void surfaceSet(int Id, boolean isLocal, boolean isSet) {
        Integer status = (Integer) mSurfaceStatusById.get("" + Id);
        int statusInt;

        Log.d(TAG, "[surfaceSet] isLocal = " + isLocal + ", isSet = " + isSet);

        if (status != null) {
            statusInt = status.intValue();
            Log.d(TAG, "[surfaceSet] state (before) = " + statusInt);
            if (isLocal) {
                if (!isSet) {
                    statusInt &= ~0x1;
                } else {
                    statusInt |= 0x1;
                }
            } else {
                if (!isSet) {
                    statusInt &= ~0x2;
                } else {
                    statusInt |= 0x2;
                }
            }
        } else {
            Log.d(TAG, "[surfaceSet] state (before) = null");
            if (isLocal) {
                if (!isSet) {
                    statusInt = 0x0;
                } else {
                    statusInt = 0x1;
                }
            } else {
                if (!isSet) {
                    statusInt = 0x0;
                } else {
                    statusInt = 0x2;
                }
            }
        }
        Log.d(TAG, "[surfaceSet] state (after) = " + statusInt + ", Id = " + Id);

        mSurfaceStatusById.put("" + Id, new Integer(statusInt));
        return;
    }

    public static int surfaceGet(int Id) {
        Integer status = (Integer) mSurfaceStatusById.get("" + Id);
        if (status != null) {
            Log.d(TAG, "[surfaceGet] state = " + status.intValue() + ", Id = " + Id);
            return status.intValue();
        } else {
            Log.d(TAG, "[surfaceGet] state = 0" + ", Id = " + Id);
            return 0;
        }
    }

    public static void recordAdd(int Id, ImsVTProvider p) {
        Log.d(TAG, "recordAdd id = " + Id + ", size = " + recordSize());
        mProviderById.put("" + Id, p);
        return;
    }

    public static void recordRemove(int Id) {
        Log.d(TAG, "recordRemove id = " + Id + ", size = " + recordSize());
        mProviderById.remove("" + Id);
        return;
    }

    public static ImsVTProvider recordGet(int Id) {
        Log.d(TAG, "recordGet id = " + Id + ", size = " + recordSize());
        return (ImsVTProvider) mProviderById.get("" + Id);
    }

    public static int recordPopId() {

        if (mProviderById.size() != 0) {
            for (Object p : mProviderById.values()) {
                return ((ImsVTProvider) p).getId();
            }
        }
        return ImsVTProvider.VT_PROVIDER_INVALIDE_ID;
    }

    public static boolean recordContain(int Id) {
        return mProviderById.containsKey(Id);
    }

    public static int recordSize() {
        return mProviderById.size();
    }
}
