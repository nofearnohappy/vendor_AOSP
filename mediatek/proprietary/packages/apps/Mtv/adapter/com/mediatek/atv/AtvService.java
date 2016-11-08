/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.atv;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.view.Surface;
import android.media.MediaRecorder;
import android.os.SystemProperties;
import android.util.Log;

public class AtvService {

    static final boolean IS_MATV_ANALOG_SUPPORT = SystemProperties.getBoolean("ro.mtk_matv_analog_support", false);

    public static final int ATV_SCAN_PROGRESS = 0xf0002001;
    public static final int ATV_SCAN_FINISH = 0xf0002002;
    public static final int ATV_AUDIO_FORMAT_CHANGED = 0xf0002003;
    public static final int ATV_CHIP_SHUTDOWN = 0xf0002005;

    private static String TAG = "ATV/ATVService";


    private int mNativeContext; // accessed by native methods
    public class Parameters {
        // Parameter keys to communicate with the camera driver.
        private static final String KEY_PREVIEW_SIZE = "preview-size";
        private HashMap<String, String> mMap;

        private Parameters() {
            mMap = new HashMap<String, String>();
        }
        /**
         * Creates a single string with all the parameters set in
         * this Parameters object.
         * <p>The {@link #unflatten(String)} method does the reverse.</p>
         *
         * @return a String with all values from this Parameters object, in
         *         semi-colon delimited key-value pairs
         */
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

        /**
         * Takes a flattened string of parameters and adds each one to
         * this Parameters object.
         * <p>The {@link #flatten()} method does the reverse.</p>
         *
         * @param flattened a String of parameters (key-value paired) that
         *                  are semi-colon delimited
         */

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

        /**
         * Sets a String parameter.
         *
         * @param key   the key name for the parameter
         * @param value the String value of the parameter
         */

        public void set(String key, String value) {
            if (key.indexOf('=') != -1 || key.indexOf(';') != -1) {
                Log.e("@M_" + TAG, "Key \"" + key + "\" contains invalid character (= or ;)");
                return;
            }
            if (value.indexOf('=') != -1 || value.indexOf(';') != -1) {
                Log.e("@M_" + TAG, "Value \"" + value + "\" contains invalid character (= or ;)");
                return;
            }

            mMap.put(key, value);
        }

        /**
         * Sets the dimensions for preview pictures.
         *
         * @param width  the width of the pictures, in pixels
         * @param height the height of the pictures, in pixels
         */

        public void setDisplaySize(int width, int height) {
            String v = Integer.toString(width) + "x" + Integer.toString(height);
            set(KEY_PREVIEW_SIZE, v);
        }
    }
    /**
     * Sets the Parameters for pictures from this Camera service.
     *
     * @param params the Parameters to use for this Camera service
     */

    public void setParameters(Parameters params) {
        native_setParameters(params.flatten());
    }

    /**
     * Returns the picture Parameters for this Camera service.
     */

    public Parameters getParameters() {
        Parameters p = new Parameters();
        String s = native_getParameters();
        p.unflatten(s);
        return p;
    }

    /*Open the camera service.*/
    public void  openVideo(boolean connect) {
        _openVideo(connect);
        Parameters parameters = getParameters();
        parameters.setDisplaySize(320, 240);
        parameters.set("tv-delay", IS_MATV_ANALOG_SUPPORT ? "0" : "240");
        //parameters.set("sensor-dev","atv");
        setParameters(parameters);
        startVideo();
    }

    private native void  _openVideo(boolean connect);
    private native void  startVideo();

    public native void  init() throws IOException;

    /*Initializes TV chip*/
    public native  void  setup(Object weak_this);

    /*Shutdown TV chip*/
    public native  void  shutdown(boolean shutdownHardware);

    /*release camera*/
    public native  void  closeVideo(boolean disconnect);

    /*sh: the SurfaceHolder to use for displaying the video portion of the media.*/
    public native  void  setSurface(Surface sh) throws IOException ;

    /*mr: the MediaRecorder to use for tv recording.*/
    public native  void  setRecorder(MediaRecorder mr);

    public native  boolean  previewEnabled();

    public native  void  capture();

    public native  void  native_setParameters(String params);

    public native  String native_getParameters();

    public native  void  reconnect() throws IOException ;
    public native  void  lock();
    public native  void  unlock();

    /*Scan channel*/
    public native  void  channelScan(int mode, int area);

    /*Stop channel scan.*/
    public native  void  stopChannelScan();

    /*Get channel entry*/
    public native  long getChannelTable(int ch);

    /*Get channel entry.*/
    public native  void setChannelTable(int ch, long entry);

    /*Clear channel table in driver.*/
    public native  void  clearChannelTable();

    /*Change channel.*/
    public native  void  changeChannel(int ch);

    /*set location area code for scanning.*/
    public native  void  setLocation(int loc);

    /*configure video parameters.*/
    public native  void  adjustSetting(byte item, int val);

    public native  void  setChipDep(int item, int val);


    public native  void  setAudioFormat(int format);

    public native  void  setAudioCallback(boolean set);

    public native  int getAudioFormat();

    public native  int getSignalStrength();


    private static EventCallback mEventCallback;

    public AtvService(EventCallback e) {
        mEventCallback = e;
        setup(new WeakReference<AtvService>(this));
    }


    static {
        System.loadLibrary("JniAtvService");
    }

    public interface EventCallback {
        void callOnEvent(int what, int arg1, int arg2, Object obj);
    }

    private static void postEventFromNative(Object service_ref,
            int what, int arg1, long arg2, Object obj)
    {
        //make sure the object is still alive.
        AtvService as = (AtvService) ((WeakReference<AtvService>) service_ref).get();
        Log.i("@M_" + TAG, "what = " + what + " arg1 = " + arg1 + " arg2 = " + arg2 + " obj = " + obj +
            " service_ref = " + service_ref + " as = " + as);
        if (as == null) {
            return;
        }

        if (what == ATV_SCAN_PROGRESS || what == ATV_SCAN_FINISH) {
            mEventCallback.callOnEvent(what, arg1, 0, (Long) arg2);
        } else {
            mEventCallback.callOnEvent(what, arg1, (int) arg2, obj);
        }
    }
}


