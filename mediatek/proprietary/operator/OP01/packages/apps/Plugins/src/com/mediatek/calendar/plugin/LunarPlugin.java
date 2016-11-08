package com.mediatek.calendar.plugin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.format.Time;
import android.util.Log;

import com.mediatek.calendar.ext.DefaultLunarExtension;
import com.mediatek.calendar.plugin.lunar.LunarMonthViewExt;
import com.mediatek.calendar.plugin.lunar.LunarUtil;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.calendar.ext.ILunarExt")
public class LunarPlugin extends DefaultLunarExtension {

    private static final String TAG = "LunarPlugin";
    private Context mContext;
    private LunarUtil mLunarUtil;

    public LunarPlugin(Context context) {
        Log.d(TAG, "LunarPlugin constuctor");
        mContext = context;
        mLunarUtil = LunarUtil.getInstance(context);
    }

     /**
     * Gets lunar date or lunar date range of a event according to the given startMillis
     * and endMillis of this event
     * @param localTimezone the current time zone
     * @param startMillis   the start time of a event
     * @param endMillis     the and time of a event
     * @param allDay        whether the event is all day event
     * @return lunar date like: 农历[闰]xx月xx
     *         or lunar date range like:农历[闰]xx月(初|十|廿|卅)x - [闰]xx月(初|十|廿|卅)x
     */
    @Override
    public String getLunarDisplayedDate(String localTimezone, long startMillis,
            long endMillis, boolean allDay) {
        return mLunarUtil.getLunarDisplayedDate(mContext, localTimezone, startMillis, endMillis,
                allDay);
    }

    /**
     * Build lunar date according the given gregorian date or time
     * @param date if it is null, timezone and milliTime must be assigned
     * @return lunar date string like : [闰]xx月(初|十|廿|卅)x
     */
    @Override
    public String buildLunarDate(Time date, String timeZone, long milliTime) {
        return mLunarUtil.buildLunarDate(mContext, date, timeZone, milliTime);
    }

    /** Draw lunar in the box(x ,y) of the month view if needed
     * @param canvas        the canvas to draw
     * @param monthNumPaint the paint of the month number
     * @param x             the x of the number's right-bottom
     * @param y             the y of the number's right-bottom
     */
    @Override
    public void drawLunarString(Context hostContext, Canvas canvas, Paint monthNumPaint,
            int x, int y, Time weekDay) {
        LunarMonthViewExt.drawLunarString(hostContext, mContext, canvas, monthNumPaint, x, y, weekDay);
    }

}
