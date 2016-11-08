package com.mediatek.rcs.pam.ui;

import java.io.File;

import com.mediatek.rcs.pam.util.Utils;

import android.content.Context;
import android.text.TextUtils;

public class RcsMessageConfig {
    private static final String TAG = "PA/RCSConfig";

    // M: add for ipmessage
    private static String sPicTempPath = "";
    private static String sAudioTempPath = "";
    private static String sVideoTempPath = "";
    private static String sVcardTempPath = "";
    private static String sCalendarTempPath = "";
    private static String sGeolocTempPath = "";
    private static long sSubId = 0;
    private Context mContext;

    public RcsMessageConfig(Context context) {
        mContext = context;
    }

    public int getMaxTextLimit(Context context) {
        // TODO; modify the max text
        return 3000;
    }

    public boolean onIpInit(Context context) {
        // initializeIpMessageFilePath(mContext);
        return false;
    }

    public static void setSubId(long subId) {
        sSubId = subId;
    }

    public static long getSubId() {
        return sSubId;
    }

    public static boolean isActivated() {
        return true;
    }

    public static boolean isActivated(Context context) {
        return true;
    }

    public static boolean isServiceEnabled(Context mContext) {
        return true;
    }

    private static boolean sDisplayBurned;

    public static void setEditingDisplayBurnedMsg(boolean isBurned) {
        sDisplayBurned = isBurned;
    }

    public static boolean isEditingDisplayBurnedMsg() {
        return sDisplayBurned;
    }

    private static void initializeIpMessageFilePath(Context context) {
        if (Utils.getSDCardStatus()) {
            sPicTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }

            sAudioTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }

            sVideoTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }

            sVcardTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }

            String cachePath = Utils.getCachePath(context);
            if (cachePath != null) {
                File f = new File(cachePath);
                if (!f.exists()) {
                    f.mkdirs();
                }
            }
        }
    }

    public static String getPicTempPath(Context context) {
        if (TextUtils.isEmpty(sPicTempPath)) {
            sPicTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "picture";
            File picturePath = new File(sPicTempPath);
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }
        return sPicTempPath;
    }

    public static String getAudioTempPath(Context context) {
        if (TextUtils.isEmpty(sAudioTempPath)) {
            sAudioTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "audio";
            File audioPath = new File(sAudioTempPath);
            if (!audioPath.exists()) {
                audioPath.mkdirs();
            }
        }
        return sAudioTempPath;
    }

    public static String getVideoTempPath(Context context) {
        if (TextUtils.isEmpty(sVideoTempPath)) {
            sVideoTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + "video";
            File videoPath = new File(sVideoTempPath);
            if (!videoPath.exists()) {
                videoPath.mkdirs();
            }
        }
        return sVideoTempPath;
    }

    public static String getVcardTempPath(Context context) {
        if (TextUtils.isEmpty(sVcardTempPath)) {
            sVcardTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "vcard";
            File vcardPath = new File(sVcardTempPath);
            if (!vcardPath.exists()) {
                vcardPath.mkdirs();
            }
        }
        return sVcardTempPath;
    }

    public static String getVcalendarTempPath(Context context) {
        if (TextUtils.isEmpty(sCalendarTempPath)) {
            sCalendarTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "calendar";
            File calendarPath = new File(sCalendarTempPath);
            if (!calendarPath.exists()) {
                calendarPath.mkdirs();
            }
        }
        return sCalendarTempPath;
    }

    public static String getGeolocTempPath(Context context) {
        if (TextUtils.isEmpty(sGeolocTempPath)) {
            sCalendarTempPath = Utils.getSDCardPath(context)
                    + Utils.PA_MESSAGE_CACHE_PATH + File.separator + "loc";
            File geolocPath = new File(sGeolocTempPath);
            if (!geolocPath.exists()) {
                geolocPath.mkdirs();
            }
        }
        return sGeolocTempPath;
    }
}
