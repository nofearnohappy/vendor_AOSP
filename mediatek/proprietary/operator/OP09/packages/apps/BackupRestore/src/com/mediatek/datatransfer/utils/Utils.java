package com.mediatek.datatransfer.utils;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {

    private static final String TAG = "DataTransfer/Utils";
    private static final int COLORNUM = 7;

    public static String getPhoneSearialNumber() {
        String serial = null;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.boot.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }

    public static void writeToFile(String content, String filePath) {
        try {
            FileOutputStream outStream = new FileOutputStream(filePath);
            byte[] buf = content.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readFromFile(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }
            is.close();
            return baos.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    /**
     * get a String like [2013/04/06 22:02:12 GMT-3] from the UTC time millis.
     * @param date UTC timeMillis
     * @return the time string as description.
     */
    public static String parseDate(long date) {
        MyLogger.logD(TAG, "[parseDate] time millis is " + date);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateString = "";
        try {
            dateString = dateFormat.format(new Date(date));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            dateString = dateFormat.format(new Date(System.currentTimeMillis()));
            e.printStackTrace();
        }
        int offset = TimeZone.getDefault().getOffset(date) / (1000 * 3600);
        String time = dateString + " GMT" + (offset > 0 ? "+" + offset : offset);
        return time;
    }
    /**
     * reverse method{@link #parseDate(long) }
     * @param dateString string like : 2013/04/06 22:02:12 GMT-3
     * @return UTC time millis.
     */
    public static long unParseDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || !dateString.contains("GMT")) {
            return System.currentTimeMillis();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        MyLogger.logD(TAG, "[unParseDate] time zone is " + dateString.substring(dateString.indexOf("GMT")));
        TimeZone tz = TimeZone.getTimeZone(dateString.substring(dateString.indexOf("GMT")));
        MyLogger.logD(TAG, "[unParseDate] time zone is " + tz.getDisplayName());
        dateFormat.setTimeZone(tz);
        long result = 0;
        try {
            String time = dateString.substring(0, dateString.indexOf("GMT"));
            MyLogger.logD(TAG, "[unParseDate] time is " + time.trim());
            result = dateFormat.parse(time).getTime();
            MyLogger.logD(TAG, "[unParseDate] time millis is " + result);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            result = System.currentTimeMillis();
            e.printStackTrace();
        }
        return result;
    }


    /**
     * get slot from simID
     * @param simId
     * @return
     */
    public static int simId2Slot(long simid, Context context) {
        if (Utils.isGeminiSupport() && simid >= 0) {

            /*
             * M: remove getSlotById method.
             */
            SubInfoRecord simInfo = SubscriptionManager.getSubInfoForSubscriber(simid);
            int slotId = -1;
            if (simInfo != null) {
                slotId = simInfo.slotId;
            }
            slotId++;
            return slotId;
        }
        return 0;
    }
    /**
     * get simID from slot
     * @param slot
     * @return
     */
    public static long slot2SimId(int slot, Context context) {
        if (!isSingleCard()) {
            slot--;
            if (slot >= 0) { }
                //return  SIMInfo.getIdBySlot(context, slot);
        }
        return 0;
    }

    /**
     * check the device is single card phone.
     * @return true single
     */
    private static boolean isSingleCard() {
        return !Utils.isGeminiSupport();
    }

    /**
     * @author mtk81346
     * @param timestamp the byte[] which Feature Phone saved in file PDU.o
     * @return Feature Phone sendbox's SMS send time.If failed, return System.currentTimeMillis.
     */
    public static long getFPsendSMSTime(byte[] timestamp) {
        if (timestamp.length != 4) {
            return System.currentTimeMillis();
        }
        long time = unsigned4BytesToInt(timestamp);
        return getFPsendSMSTime(time);
    }

    private static long getFPsendSMSTime(long time) {
        long sec = time % (3600 * 24);
        long hour = sec / 3600;
        sec %= 3600;
        long min = sec / 60;
        long secd = sec % 60;
        int d = 0;
        long day = time / (3600 * 24);
        int y = 1970;
        for (; day > 0; y++) {
            d = 365 + isLeakYeay(y);
            if (day >= d) {
                day -= d;
            } else {
                break;
            }
        }
        int m = 1;
        for (; m < 13; m++) {
            d = getLaseDayPerMonth(m, y);
            if (day >= d) {
                day -= d;
            } else {
                break;
            }
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            String sendtime = y + "-" + m + "-" + (day + 1) + " " + hour + ":" + min + ":" + secd;
            Date date = format.parse(sendtime);
            return date.getTime();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return System.currentTimeMillis();
        }

    }
    private static int getLaseDayPerMonth(int m, int y) {
        int days = 0;
        if (m == 2) {
            if (isLeakYeay(y) == 1) {
                days = 29;
            } else {
                days = 28;
            }
        } else {
            switch (m) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                days = 31;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                days = 30;
                break;
            }
        }
        return days;
    }

    private static int isLeakYeay(int y) {
        if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) {
            return 1;
        }
        return 0;
    }

    private static long unsigned4BytesToInt(byte[] buf) {
        int firstByte = 0;
        int secondByte = 0;
        int thirdByte = 0;
        int fourthByte = 0;
        int index = 0;
        firstByte = (0x000000FF & ((int) buf[index]));
        secondByte = (0x000000FF & ((int) buf[index + 1]));
        thirdByte = (0x000000FF & ((int) buf[index + 2]));
        fourthByte = (0x000000FF & ((int) buf[index + 3]));
        index = index + 4;
        return ((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFFL;
    }

    public static int getStatusResource(int state) {

        switch (state) {
          //temp
            /**
        case PhoneConstants.SIM_INDICATOR_RADIOOFF:
            return com.mediatek.internal.R.drawable.sim_radio_off;
        case PhoneConstants.SIM_INDICATOR_LOCKED:
            return com.mediatek.internal.R.drawable.sim_locked;
        case PhoneConstants.SIM_INDICATOR_INVALID:
            return com.mediatek.internal.R.drawable.sim_invalid;
        case PhoneConstants.SIM_INDICATOR_SEARCHING:
            return com.mediatek.internal.R.drawable.sim_searching;
        case PhoneConstants.SIM_INDICATOR_ROAMING:
            return com.mediatek.internal.R.drawable.sim_roaming;
        case PhoneConstants.SIM_INDICATOR_CONNECTED:
            return com.mediatek.internal.R.drawable.sim_connected;
        case PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
            return com.mediatek.internal.R.drawable.sim_roaming_connected;
            */
        default:
            return -1;
        }
    }

    public static int getSimColorResource(int color) {

        if ((color >= 0) && (color <= COLORNUM)) {
            return -1; // temp return
                      // SubscriptionManager.sSimBackgroundDarkRes[color];
            // SimInfoManager.SimBackgroundDarkRes[color];
        } else {
            return -1;
        }

    }


    public static boolean isGeminiSupport() {
        boolean ret = false;

        ret = SystemProperties.get("ro.mtk_gemini_support").equals("1");

        Log.d(MyLogger.LOG_TAG, "isGeminiSupport: " + ret);

        return ret;
    }
}
