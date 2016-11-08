package com.mediatek.rcs.message.utils;

import android.content.Context;


/**
 * RcsMessageConfig.
 *
 */
public class RcsMessageConfig {
    private static final String TAG = "RCSConfig";

    // M: add for ipmessage
//     private static String sPicTempPath = "";
//     private static String sAudioTempPath = "";
//     private static String sVideoTempPath = "";
//     private static String sVcardTempPath = "";
//     private static String sCalendarTempPath = "";
//     private static String sGeoloTempPath = "";
     private  static long sSubId = 0;
     private Context mContext;

     /**
      * Construction.
      * @param context Context
      */
     public RcsMessageConfig(Context context) {
         mContext = context;
     }

//    public int getMaxTextLimit(Context context) {
//        //TODO; modify the max text
//        return 3000;
//    }

    /**
     * will delete it.
     * @param subId
     */
    public static void setSubId(long subId) {
        sSubId = subId;
    }

    /**
     * will delete it.
     * @return long
     */
    public static long getSubId() {
        return sSubId;
    }

    /**
     * isActivated.
     * @return true
     */
    public static boolean isActivated() {
        return true;
    }

    /**
     * isActivated.
     * @param context Context
     * @return true
     */
    public static boolean isActivated(Context context) {
        return true;
    }

    /**
     * isServiceEnabled.
     * @param mContext Context
     * @return true
     */
    public static boolean isServiceEnabled(Context mContext) {
        return true;
    }


    private static boolean sDisplayBurned;
    /**
     * setEditingDisplayBurnedMsg.
     * @param isBurned boolean
     */
    public static void setEditingDisplayBurnedMsg(boolean isBurned) {
        sDisplayBurned = isBurned;
    }

    /**
     * isEditingDisplayBurnedMsg.
     * @return true if is burned
     */
    public static boolean isEditingDisplayBurnedMsg() {
        return sDisplayBurned;
    }
}
