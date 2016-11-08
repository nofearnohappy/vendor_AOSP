package com.mediatek.rcs.contacts.networkcontacts.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.mediatek.rcs.contacts.R;

/**
 * @author MTK80963
 *
 */
public class TimePreference extends DialogPreference {
    private TimePicker mTime;

    /**
     * @param context .
     * @param attrs .
     */
    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.time_preference);
    }

    /**
     * @param context .
     * @param attrs .
     * @param style .
     */
    public TimePreference(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        setDialogLayoutResource(R.layout.time_preference);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mTime = (TimePicker) view.findViewById(R.id.timePicker);
        mTime.setIs24HourView(true);

        String timestr = getSharedPreferences().getString(getKey(), "09:00");
        String[] times = timestr.split(":");

        int h = Integer.parseInt(times[0]);
        int m = Integer.parseInt(times[1]);
        mTime.setCurrentHour(h);
        mTime.setCurrentMinute(m);
        setSummary(timestr);

    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        String value = String.format("%02d:%02d",
                mTime.getCurrentHour(), mTime.getCurrentMinute());
        setSummary(value);
        if (callChangeListener(value)) {
            getSharedPreferences().edit().putString(getKey(), value).commit();
        }
    }
}
