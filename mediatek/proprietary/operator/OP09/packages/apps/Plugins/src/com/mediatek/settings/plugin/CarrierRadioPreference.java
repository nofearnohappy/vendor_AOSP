package com.mediatek.settings.plugin;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Used in ManualNetworkSelection to show GSM available network as a list.
 *
 */
public class CarrierRadioPreference extends RadioPreference {
    private String mCarrierNumeric;
    private int mCarrierRate;

    /**
     * Constructor with Context.
     * @param context Input Context
     */
    public CarrierRadioPreference(Context context) {
        super(context, "title", "summary");
    }

    /**
     * Constructor with Context, AttributeSet.
     * @param context Input Context
     * @param attrs Input AttributeSet
     */
    public CarrierRadioPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor with Context, title string, summary string.
     * @param context Input Context
     * @param title Input title string
     * @param summary Input summary string
     */
    public CarrierRadioPreference(Context context, String title, String summary) {
        super(context, title, summary, false);
    }

    /**
     * Constructor with Context, title string, summary string, if checked.
     * @param context Input Context
     * @param title Input title string
     * @param summary Input summary string
     * @param isChecked Input if checked
     */
    public CarrierRadioPreference(Context context, String title, String summary,
            boolean isChecked) {
        super(context);
    }

    public void setCarrierNumeric(String numeric) {
        mCarrierNumeric = numeric;
    }

    public String getCarrierNumeric() {
        return mCarrierNumeric;
    }

    public void setCarrierRate(int rate) {
        mCarrierRate = rate;
    }

    public int getCarrierRate() {
        return mCarrierRate;
    }
}
