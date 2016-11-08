package com.mediatek.calendar.plugin.lunar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.format.Time;
import android.util.Log;


/**
 * M: Lunar Extension for MonthView
 * this extension would draw lunar text in the cell
 */
public class LunarMonthViewExt {

    private static final String TAG = "LunarMonthViewExt";

    private static final int PADDING_LUNAR_OFFSET = 2;
    private static int sLunarOffsetXFromMonthNumber = -10;
    private static int sLunarTextSize = 11;
    private static boolean sIsScaled = false;

    // A singleton
    private static LunarMonthViewExt sInstance = null;

    private final Paint mLunarTextPaint;
    private final float mScale;
    private final LunarUtil mLunarUtil;

    /*
     * A singleton.
     * @param context the context of the view
     */
    private LunarMonthViewExt(Context hosContext, Context pluginContext) {
        mScale = hosContext.getResources().getDisplayMetrics().density;
        mLunarTextPaint = new Paint();
        mLunarUtil = LunarUtil.getInstance(pluginContext);
        initDimens();
    }

    /*
     * A singleton.
     */
    synchronized private static void makeInstance(Context hosContext, Context pluginContext) {
        sInstance = new LunarMonthViewExt(hosContext, pluginContext);
    }

    /**
     * M: init the dimens, so that the lunar text size and location
     * would self-suit to different screen sizes
     */
    private void initDimens() {
        if (!sIsScaled) {
            sLunarTextSize *= mScale;
            sLunarOffsetXFromMonthNumber *= mScale;

            sIsScaled = true;
        }
    }

    public static void drawLunarString(Context hostContext, Context pluginContext, Canvas canvas, Paint monthNumPaint, int numX,
            int numY, Time weekDay) {
        if (sInstance == null) {
            makeInstance(hostContext, pluginContext);
        }

        if (sInstance.canShowLunarCalendar()) {
            sInstance.drawLunarString(hostContext, canvas, monthNumPaint, numX, numY, weekDay);
        }
    }

    /** M: #Lunar# draw lunar in the box(x ,y) if needed.
     * Actually, this function only retrieve the Lunar string with specific dilemma(;)
     *
     * @param canvas canvas to draw
     * @param monthNumPaint the paint of the month number
     * @param x the x of the number's right-bottom
     * @param y the y of the number's right-bottom
     */
    private void drawLunarString(Context hostContext, Canvas canvas, Paint monthNumPaint, int x, int y, Time weekDay) {
        if (weekDay == null) {
            Log.e(TAG, "drawLunar(),time from (" + x + ") is null, return");
            return;
        }

        String lunarText = mLunarUtil.getLunarFestivalChineseString(
                weekDay.year, weekDay.month + 1, weekDay.monthDay);

        mLunarTextPaint.set(monthNumPaint);
        mLunarTextPaint.setTextSize(sLunarTextSize);

        int orientation = hostContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLunarTextPaint.setTextAlign(Align.CENTER);
            String[] words = lunarText.split(LunarUtil.DELIM);
            int wordX = x + sLunarOffsetXFromMonthNumber;
            int wordY = y + sLunarTextSize;
            for (String word : words) {
                canvas.drawText(word, wordX, wordY, mLunarTextPaint);
                wordY += (sLunarTextSize + PADDING_LUNAR_OFFSET);
            }
        } else {
            final String landDelim = " ";
            mLunarTextPaint.setTextAlign(Align.RIGHT);
            canvas.drawText(lunarText.replace(LunarUtil.DELIM, landDelim).trim(),
                    x + PADDING_LUNAR_OFFSET,
                    y + sLunarTextSize, mLunarTextPaint);
        }
    }

    private boolean canShowLunarCalendar() {
        return mLunarUtil.canShowLunarCalendar();
    }
}
