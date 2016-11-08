package com.mediatek.calendar.plugin.lunar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.mediatek.common.plugin.R;


public class LunarAlgorithm {

    private static final String TAG = "LunarAlgorithm";

    private static final int LEAP_MONTH = 0;
    private static final int NORMAL_MONTH = 1;

    /* the lnuar calculate based on the year 1900 */
    private static final int LUNAR_YEAR_BASE = 1900;
    private static final int LUNAR_YEAR_END = 2038;

    /* Lunar info consts, for calculating leap month. */
    private final int[] mLunarInfoArray;

    public LunarAlgorithm(Context context) {
        Resources res = context.getResources();
        mLunarInfoArray = res.getIntArray(R.array.lunar_info);
    }

    /**
     * get the total number days of a lunar year.
     *
     * @param lunarYear which lunar year days number to return.
     * @return A lunar year days total number.
     */
    private int daysOfLunarYear(int lunarYear) {
        int i;
        int sum = 348;
        for (i = 0x8000; i > 0x8; i >>= 1) {
            if ((mLunarInfoArray[lunarYear - 1900] & i) != 0) {
                sum += 1;
            }
        }
        return (sum + daysOfLeapMonthInLunarYear(lunarYear));
    }

    /**
     * get a lunar year's leap month  total days number.
     *
     * @param lunarYear which lunar year
     * @return the total days number of this lunar year's leap month. if this
     *         luanr year hasn't leap,will return 0.
     */
    private int daysOfLeapMonthInLunarYear(int lunarYear) {
        if (leapMonth(lunarYear) != 0) {
            if ((mLunarInfoArray[lunarYear - 1900] & 0x10000) != 0) {
                return 30;
            } else {
                return 29;
            }
        }
        return 0;
    }

    /**
     * get the total days number of a month
     * @param luanrYear which lunar year.
     * @param lunarMonth which lunar month
     * @return the total days of this month
     */
    private int daysOfALunarMonth(int luanrYear, int lunarMonth) {
        if ((mLunarInfoArray[luanrYear - 1900] & (0x10000 >> lunarMonth)) == 0) {
            return 29;
        }
        return 30;
    }

    /**
     * get the leap month of lunar year.
     * @param lunarYear which lunar year to return.
     * @return the number of the leapMonth.if hasn't leap
     *         month will return 0.
     */
    private int leapMonth(int lunarYear) {
        if (lunarYear < 1900 || lunarYear > 2100) {
            Log.e(TAG, "get leapMonth:" + lunarYear + "is out of range.return 0.");
            return 0;
        }
        return (int) (mLunarInfoArray[lunarYear - 1900] & 0xf);
    }

    /**
     * parse gregorian date as 'yyyy-MM-dd' into Date object
     */
    private Date parseDate(int gregorianYear, int gregorianMonth, int gregorianDay) {
        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = sdf.parse(gregorianYear + "-" + gregorianMonth + "-" + gregorianDay);
        } catch (ParseException e) {
            Log.e(TAG, "parseDate,parse date error.");
            e.printStackTrace();
        }
        return date;
    }

    /**
     * calculate the lunar year for currentDate which is a gregorian date.
     * @param baseDate which is helpful to calculate. Usually it is the gregorian date of 1900.1.31.
     * @param currentDate which gregorian date to convert
     * @return the lunar year of the current date refers to
     */
    private int calculateLunarYear(int offsetDaysFromBaseDate, int[] lunar) {
        int lunarYear;
        int tempLunaryear;
        int daysOfTempLunaryear = 0;
        //start calculator the lunar year.
        //loop use (offsetDaysFromBaseDate - daysOfTempLunaryear) until (offsetDaysFromBaseDate <= 0)
        //daysOfTempLunaryear is the days of 1900,1901,1902,1903.......
        //when loop end,daysOfTempLunaryear will <= 0
        //if offsetDaysFromBaseDate = 0,tempLunaryear is the right lunar year
        //if offsetDaysFromBaseDate < 0,tempLunaryear + 1 is the right lunar year.
        for (tempLunaryear = LUNAR_YEAR_BASE;
                tempLunaryear < LUNAR_YEAR_END && offsetDaysFromBaseDate > 0;
                tempLunaryear++) {
            daysOfTempLunaryear = daysOfLunarYear(tempLunaryear);
            offsetDaysFromBaseDate -= daysOfTempLunaryear;
        }
        //if offsetDaysFromBaseDate < 0,calculate the previous year
        if (offsetDaysFromBaseDate < 0) {
            offsetDaysFromBaseDate += daysOfTempLunaryear;
            tempLunaryear--;
        }
        lunar[0] = tempLunaryear;
        return offsetDaysFromBaseDate;
    }

    /**
     * calculate the lunar month and lunar day according luanrYear and offsetDaysFromBaseDate.
     */
    private void calculateLunarMonthAndDay(int offsetDaysFromBaseDate, int[] lunar) {
        int lunarYear = lunar[0];
        // get which month is leap month,if none 0.
        int leapMonth = leapMonth(lunarYear);
        //represent if minus the leap month days
        boolean isMinusLeapMonthDays = false;

        int tempLunarMonth;
        int daysOfTempLunarMonth = 0;
        //start calculate the lunar month
        //now the value of offsetDaysFromBaseDate equals the day  of the lunar year,like:111/365
        //when offsetDaysFromBaseDate <= 0,then tempLunarMonth <= the right lunar month
        //so if offsetDaysFromBaseDate < 0,the previous lunar month is the right lunar month
        //if offsetDaysFromBaseDate = 0,the tempLunarMonth si the right lunar month
        for (tempLunarMonth = 1; tempLunarMonth < 13 && offsetDaysFromBaseDate > 0; tempLunarMonth++) {
            // leap month
            if (leapMonth > 0 && tempLunarMonth == (leapMonth + 1) && !isMinusLeapMonthDays) {
                --tempLunarMonth;
                isMinusLeapMonthDays = true;
                daysOfTempLunarMonth = daysOfLeapMonthInLunarYear(lunarYear);
            } else {
                daysOfTempLunarMonth = daysOfALunarMonth(lunarYear, tempLunarMonth);
            }
            //Minus a the days of a month
            offsetDaysFromBaseDate -= daysOfTempLunarMonth;

            //reset isMinusLeapMonthDays status
            if (isMinusLeapMonthDays && tempLunarMonth == (leapMonth + 1)) {
                isMinusLeapMonthDays = false;
            }
        }
        //if offsetDaysFromBaseDate == 0,it says  the tempLunarMonth is the leap month
        //But now the value of tempLunarMonth = leapMonth + 1,so we should minus 1.
        if (offsetDaysFromBaseDate == 0 && leapMonth > 0 && tempLunarMonth == leapMonth + 1) {
            if (isMinusLeapMonthDays) {
                isMinusLeapMonthDays = false;
            } else {
                isMinusLeapMonthDays = true;
                --tempLunarMonth;
            }
        }
        //if offsetDaysFromBaseDate < 0,calculate the previous lunar month
        if (offsetDaysFromBaseDate < 0) {
            offsetDaysFromBaseDate += daysOfTempLunarMonth;
            --tempLunarMonth;
        }
        int lunarMonth = tempLunarMonth;

        //start calculate the lunar day.
        //now the value of the offsetDaysFromBaseDate equals the lunar day + 1,like:11/31
        //only plus 1.
        int lunarDay = offsetDaysFromBaseDate + 1;

        lunar[1] = lunarMonth;
        lunar[2] = lunarDay;
        lunar[3] = isMinusLeapMonthDays ? LEAP_MONTH : NORMAL_MONTH;
    }

    /**
     * convert gregorian date into lunar date
     * @return lunar date,int[], 0: luanr year , 1: luanr month, 2 lunar day,
     *         3 whether lunar month is leap month
     */
    public int[] calculateLunarByGregorian(int gregorianYear, int gregorianMonth, int gregorianDay) {
        if (gregorianYear > LUNAR_YEAR_END) {
            Log.e(TAG, "the gregorianYear is out of range, limit the year to " + LUNAR_YEAR_END);
            gregorianYear = LUNAR_YEAR_END;
        }
        // default lunar date is : 2000.1.1
        int lunar[] = { 2000, 1, 1, NORMAL_MONTH };

        // The Gregorian date of 1900.1.31
        Date baseDate = null;
        // The Gregorian date of current Time
        Date currentDate = null;

        //parse baseDate
        baseDate = parseDate(1900, 1, 31);
        if (baseDate == null) {
            Log.e(TAG, "baseDate is null,return lunar date:2000.1.1");
            return lunar;
        }
        //parse currentDate
        currentDate = parseDate(gregorianYear, gregorianMonth, gregorianDay);
        if (currentDate == null) {
            return lunar;
        }

        //Calculate the number of days offset from current date to 1990.1.31
        int offsetDaysFromBaseDate = Math.round(((currentDate.getTime() - baseDate.getTime())
                / 86400000.0f));
        offsetDaysFromBaseDate = calculateLunarYear(offsetDaysFromBaseDate, lunar);
        calculateLunarMonthAndDay(offsetDaysFromBaseDate, lunar);

        return lunar;
    }
}
