package com.mediatek.calendar.plugin.lunar;


import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.mediatek.common.plugin.R;

import java.util.ArrayList;

public class TcLunar {

    private static final String TAG = "TcLunar";

    private String[] mSolarTermNames;
    private String mLunarFestChunjie;
    private String mLunarFestDuanwu;
    private String mLunarFestZhongqiu;

    /// M: word "閏".
    private static String mLunarTextLeap;
    /// M: an index refer to word "閏".
    private static final int LUNAR_WORD_RUN = 1;

    /**
     * mContext will hold the Plugin's Context
     */
    private Context mContext;

    public TcLunar(Context context) {
        Log.d(TAG, "in constructor");
        mContext = context;
        loadResources();
        Log.d(TAG, "load resources done");
    }

    public String getSolarTermNameByIndex(int index) {
        if (canShowTCLunar()) {
            if (index < 1 || index > mSolarTermNames.length) {
                Log.e(TAG, "SolarTerm should between [1, 24]");
                return null;
            }
            return mSolarTermNames[index - 1];
        }
        return "";
    }

    public String getLunarFestival(int lunarMonth, int lunarDay) {
        if (canShowTCLunar()) {
            if ((lunarMonth == 1) && (lunarDay == 1)) {
                return mLunarFestChunjie;
            } else if ((lunarMonth == 5) && (lunarDay == 5)) {
                return mLunarFestDuanwu;
            } else if ((lunarMonth == 8) && (lunarDay == 15)) {
                return mLunarFestZhongqiu;
            }
        }
        return "";
    }

    /**
     * M: Get the special word in TC mode.
     * @param index refer to the special word.
     * @return the word needed,like: LUNAR_WORD_RUN refer to "閏" in TC.
     */
    public String getSpecialWord(int index) {
        if (canShowTCLunar()) {
            if (index == LUNAR_WORD_RUN) {
                return mLunarTextLeap;
            }
        }
        return "";
    }

    public String getGregFestival(int gregorianMonth, int gregorianDay) {
        if (canShowTCLunar()) {

        }
        return "";
    }

    public String formatLunarDateRange(boolean showYear, int startYear, String lunarTextYear,
            ArrayList<String>lunarStartDate) {

        if (canShowTCLunar()) {
            String string = mContext.getString(R.string.tc_lunar_detail_info_fmt1, (showYear ? startYear + lunarTextYear : "")
                    + lunarStartDate.get(LunarUtil.MONTH) + lunarStartDate.get(LunarUtil.MONTH_DAY));
            return string;
        }
        return "";
    }

    public String formatLunarDateRange(String lunarStartDateStr, String lunarEndDateStr) {
        if (canShowTCLunar()) {
            String string = mContext.getString(R.string.tc_lunar_detail_info_fmt2, lunarStartDateStr, lunarEndDateStr);
            return string;
        }
        return "";
    }

    /**
     * M: whether in current env can TC Lunar be shown
     * @return traditional chinese can show TC Lunar, return true
     */
    public boolean canShowTCLunar() {
        return false;
    }

    /**
     * M: load the Traditional Chinese resources for displaying
     */
    private void loadResources() {
        final Resources res = mContext.getResources();
        mSolarTermNames = res.getStringArray(R.array.tc_solar_terms);
        mLunarTextLeap = res.getString(R.string.tc_lunar_leap);
        mLunarFestChunjie = res.getString(R.string.tc_lunar_fest_chunjie);
        mLunarFestDuanwu = res.getString(R.string.tc_lunar_fest_duanwu);
        mLunarFestZhongqiu = res.getString(R.string.tc_lunar_fest_zhongqiu);
    }
}
