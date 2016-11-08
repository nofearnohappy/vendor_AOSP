package com.mediatek.phone.callforward;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.op01.plugin.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Calendar;

public class CallForwardUtils {
    private static final String LOG_TAG = "CallForwardUtils";
    private static int flagsTime = DateUtils.FORMAT_SHOW_TIME;

    /**
     * Check whether there has active SubInfo indicated by given subId on the device.
     * @param context the current context
     * @param subId the sub id
     * @return true if the sub id is valid, else return false
     */
    public static boolean isValidSubId(Context context, int subId) {
        log("isValidSubId subId:" + subId);
        boolean isValid = false;
        List<SubscriptionInfo> activeSubInfoList = SubscriptionManager
                .from(context).getActiveSubscriptionInfoList();
        if (activeSubInfoList != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubInfoList) {
                if (subscriptionInfo.getSubscriptionId() == subId) {
                    isValid = true;
                    break;
                }
            }
        }
        return isValid;
    }

    /**
     * Get phone by sub id.
     * @param subId the sub id
     * @return phone according to the sub id
     */
    public static Phone getPhoneUsingSubId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    /**
     * 
     * @return true when support volte and ims
     */
    public static boolean isSupportVolteIms() {
        boolean isSupport = false;
        if (SystemProperties.get("ro.mtk_volte_support").equals("1") && 
                SystemProperties.get("ro.mtk_ims_support").equals("1") ) {
            isSupport = true;
        }
        log("isSupportVolteIms(): " + isSupport);
        return isSupport;
    }

    /**
     *switch px to sp
     * @param spValue
     * @param fontScale
     * @return sp
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * get time, HH:mm
     * @return time, HH:mm
     */
   public static String getHourMinute(long time, Context context) {
     log("getHourMinute(), time: " + time);
     String timeStr = DateUtils.formatDateTime(context, time, flagsTime);
     log("getHourMinute(), timeStr: " + timeStr);
     return timeStr;
   }

   /**
    * get time, HH:mm:ss
    * @return minute by milliseconds
    */
    public static int getMinute(long time) {
        log("getMinute(), time: " + time);
        Date dateTime = new Date(time);
        int minute = dateTime.getMinutes();
        return minute;
  }

    /**
     * get time, HH:mm:ss
     * @return hour by milliseconds
     */
     public static int getHour(long time) {
         log("getHour(), time: " + time);
         Date dateTime = new Date(time);
         int hour = dateTime.getHours();
         return hour;
   }

     /**
      * get time
      * @param hour hour
      * @param minute minute
      * @param second second
      * @return time by milliseconds
      */
      public static long getTime(int hour, int minute, int second) {
          log("getTime(), hour: " + hour + " minute: " + minute);
          //long time = ((long)hour*60*60 + (long)minute*60)*1000 - TimeZone.getDefault().getRawOffset();
          Date dateTime = new Date(System.currentTimeMillis());
          log("getTime(), dateTime: " + dateTime);
          dateTime.setHours(hour);
          dateTime.setMinutes(minute);
          dateTime.setSeconds(second);
          long gmtTime = dateTime.getTime();
          log("getTime(), gmtTime: " + gmtTime);
          return gmtTime;
    }

      /**
       * get now time
       * @return now time by milliseconds
       */
     public static long getNowTime() {
         long localTime = System.currentTimeMillis();
         log("getNowTime(), localTime: " + localTime);
         return localTime;
     }

   /**
    * compare fromtime and totime
    * @param timeSlot
    * @param context
    * @return true when fromtime and totime is equal
    */
    public static boolean compareTime(long[] timeSlot, Context context) {
        log("compareTime(), timeSlot: " + timeSlot);
        if(timeSlot != null && timeSlot.length == 2) {
            String strFromTime = getHourMinute(timeSlot[0], context);
            String strToTime = getHourMinute(timeSlot[1], context);
            if (strFromTime.compareTo(strToTime) == 0) {
                log("compareTime(), true");
                return true;
            }
        }
        log("compareTime(), false");
        return false;
    }

    /**
     * Log the message
     * @param msg the message will be printed
     */
   static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
