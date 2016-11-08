/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * MediaTek Inc. (C) 2015. All rights reserved.
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

package com.mediatek.mmsdk;

import java.util.HashMap;
import android.os.Parcel;
import android.os.Parcelable;

import android.util.Log;
import android.text.TextUtils;

/**
 * This class used for application which keys need configure. also how to
 * convert the parameters to native formats and Framework formats.
 * @hide
 */

public class BaseParameters implements Parcelable {
    private static final String TAG = "BaseParameters";

    public static final String CAMERA_MM_SERVICE_BINDER_NAME = "media.mmsdk";
    public static final String KEY_EFFECT_NAME_CFB = "capture_face_beauty";
    public static final String KEY_EFFECT_NAME_HDR = "hdr";
    public static final String KEY_PICTURE_SIZE = "picture-size";
    public static final String KEY_PICTURE_WIDTH = "picture-width";
    public static final String KEY_PICTURE_HEIGHT = "picture-height";

    public static final String KEY_IMAGE_FORMAT = "picture-format";

    public static final String KEY_FACE_BEAUTY_SHAPE = "fb-sharp";
    public static final String KEY_FACE_BEAUTY_SHAPE_MAX = "fb-sharp-max";
    public static final String KEY_FACE_BEAUTY_SHAPE_MIN = "fb-sharp-min";

    public static final String KEY_FACE_BEAUTY_SKIN_COLOR = "fb-skin-color";
    public static final String KEY_FACE_BEAUTY_SKIN_COLOR_MAX = "fb-skin-color-max";
    public static final String KEY_FACE_BEAUTY_SKIN_COLOR_MIN = "fb-skin-color-min";

    public static final String KEY_FACE_BEAUTY_SMOOTH = "fb-smooth-level";
    public static final String KEY_FACE_BEAUTY_SMOOTH_MAX = "fb-smooth-level-max";
    public static final String KEY_FACE_BEAUTY_SMOOTH_MIN = "fb-smooth-level-min";

    public static final String KEY_FACE_BEAUTY_SLIM_FACE = "fb-slim-face";
    public static final String KEY_FACE_BEAUTY_SLIM_FACE_MAX = "fb-slim-face-max";
    public static final String KEY_FACE_BEAUTY_SLIM_FACE_MIN = "fb-slim-face-min";

    public static final String KEY_OUT_PUT_CAPTURE_NUMBER = "picture-number";

    public static final String KEY_PICTURE_ROTATION = "rotation";

    //Feature Mask Name
    public static final String FEATURE_MASK_3DNR = "FeatureMask";
    public static final String FEATURE_MASK_3DNR_ON = "1";
    public static final String FEATURE_MASK_3DNR_OFF = "0";

    /**
     * Order matters: Keys that are {@link #set(String, String) set} later will
     * take precedence over keys that are set earlier (if the two keys conflict
     * with each other).
     * <p>
     * One example is {@link #setPreviewFpsRange(int, int)} , since it conflicts
     * with {@link #setPreviewFrameRate(int)} whichever key is set later is the
     * one that will take precedence.
     * </p>
     */
    private HashMap<String, String> mMap;

    // -----------------------------------------------------------------------------
    // parcelable
    // -----------------------------------------------------------------------------
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        Log.i(TAG, "writeToParcel");
        out.writeString(flatten());
    }

    public void readFromParcel(Parcel in) {
        Log.i(TAG, "readFromParcel");
        mMap = new HashMap<String, String>(128);

        int dataSize = in.dataSize();
        int dataPosition = in.dataPosition();
        Log.i(TAG, "readFromParcel - in.dataSize " + dataSize);
        Log.i(TAG, "readFromParcel - in.dataPosition " + dataPosition);
        byte[] marshell = in.createByteArray();
        for (int i = 0; i < marshell.length; i++) {
            char c = (char) marshell[i];
            Log.i(TAG, i + " - " + marshell[i] + ", " + c);
        }

        in.setDataPosition(dataPosition);
        Log.i(TAG, "readFromParcel - in.dataPosition2 " + in.dataPosition());
        int totalSize = in.readInt();
        Log.i(TAG, "totalSize=" + totalSize);
        String string = in.readString();
        if (string != null) {
            Log.i(TAG, "readFromParcel - string=" + string);
            unflatten(string);
        } else {
            Log.e(TAG, "can't read string from parcel");
        }
    }

    /**
     * create a parcelable creator
     */
    public static final Parcelable.Creator<BaseParameters> CREATOR =
      new Parcelable.Creator<BaseParameters>() {
        @Override
        public BaseParameters createFromParcel(Parcel in) {
            Log.i(TAG, "createFromParcel");
            return new BaseParameters(in);
        }

        @Override
        public BaseParameters[] newArray(int size) {
            Log.i(TAG, "newArray");
            return new BaseParameters[size];
        }
    };

    // -----------------------------------------------------------------------------
    // BaseParameters
    // -----------------------------------------------------------------------------
    /**
     * Construct a baseparameters object, the main purpose is create a hashmap
     * for store the parameters which want to set
     * @hide
     */
    public BaseParameters() {
        mMap = new HashMap<String, String>(128);
    }

    /**
     * Clone parameter from current settings.
     * @hide
     * @return the clone parameter
     */
    public BaseParameters copy() {
        BaseParameters para = new BaseParameters();
        para.mMap = new HashMap<String, String>(mMap);
        return para;
    }

    /**
     * Overwrite existing BaseParameters with a copy of the ones from
     * {@code other}.
     * <b>For use by the legacy shim only.</b>
     * @hide
     */
    public void copyFrom(BaseParameters other) {
        if (other == null) {
            throw new NullPointerException("other must not be null");
        }
        mMap.putAll(other.mMap);
    }

    /**
     * Value equality check.
     * @hide
     */
    public boolean same(BaseParameters other) {
        if (this == other) {
            return true;
        }
        return other != null && BaseParameters.this.mMap.equals(other.mMap);
    }

    /**
     * Writes the current BaseParameters to the log.
     * @hide
     * @deprecated
     */
    @Deprecated
    public void dump() {
        Log.e(TAG, "dump: size=" + mMap.size());
        for (String k : mMap.keySet()) {
            Log.e(TAG, "dump: " + k + "=" + mMap.get(k));
        }
    }

    /**
     * Creates a single string with all the BaseParameters set in this
     * BaseParameters object.
     * <p>
     * The {@link #unflatten(String)} method does the reverse.
     * </p>
     * @return a String with all values from this BaseParameters object, in
     *         semi-colon delimited key-value pairs
     * @hide
     */
    public String flatten() {
        StringBuilder flattened = new StringBuilder(128);
        for (String k : mMap.keySet()) {
            flattened.append(k);
            flattened.append("=");
            flattened.append(mMap.get(k));
            flattened.append(";");
        }
        // chop off the extra semicolon at the end
        if (flattened.length() > 0) {
            flattened.deleteCharAt(flattened.length() - 1);
        }
        return flattened.toString();
    }

    /**
     * Takes a flattened string of BaseParameters and adds each one to this
     * BaseParameters object.
     * <p>
     * The {@link #flatten()} method does the reverse.
     * </p>
     * @param flattened
     *            a String of BaseParameters (key-value paired) that are
     *            semi-colon delimited
     * @hide
     */
    public void unflatten(String flattened) {
        mMap.clear();

        TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(';');
        splitter.setString(flattened);
        for (String kv : splitter) {
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

    /**
     * Sets a String parameter.
     * @param key
     *            the key name for the parameter
     * @param value
     *            the String value of the parameter
     * @hide
     */
    public void set(String key, String value) {
        if (key.indexOf('=') != -1 || key.indexOf(';') != -1 || key.indexOf(0) != -1) {
            Log.e(TAG, "Key \"" + key + "\" contains invalid character (= or ; or \\0)");
            return;
        }
        if (value.indexOf('=') != -1 || value.indexOf(';') != -1 || value.indexOf(0) != -1) {
            Log.e(TAG, "Value \"" + value + "\" contains invalid character (= or ; or \\0)");
            return;
        }

        put(key, value);
    }

    /**
     * Sets an integer parameter.
     * @param key
     *            the key name for the parameter
     * @param value
     *            the int value of the parameter
     * @hide
     */
    public void set(String key, int value) {
        put(key, Integer.toString(value));
    }

    /**
     * Returns the value of a String parameter.
     * @param key
     *            the key name for the parameter
     * @return the String value of the parameter
     * @hide
     */
    public String get(String key) {
        return mMap.get(key);
    }

    /**
     * Returns the value of an integer parameter.
     * @param key
     *            the key name for the parameter
     * @return the int value of the parameter
     * @hide
     */
    public int getInt(String key) {
        return Integer.parseInt(mMap.get(key));
    }

    private void put(String key, String value) {
        /*
         * Remove the key if it already exists.
         * This way setting a new value for an already existing key will always
         * move that key to be ordered the latest in the map.
         */
        mMap.remove(key);
        mMap.put(key, value);
    }

    private BaseParameters(
            Parcel in) {
        Log.i(TAG, "BaseParameters(Parcel in)");
        readFromParcel(in);
    }

};
