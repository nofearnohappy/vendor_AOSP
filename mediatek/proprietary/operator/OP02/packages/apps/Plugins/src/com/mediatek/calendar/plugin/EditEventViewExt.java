package com.mediatek.calendar.plugin;

import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener;

import java.util.TimeZone;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.android.calendar.CalendarEventModel;
import com.android.calendar.R;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.ext.DefaultEditEventViewExt;
import com.mediatek.calendar.plugin.lunar.LunarDatePickerDialog;
import com.mediatek.calendar.plugin.lunar.LunarUtil;
import com.mediatek.common.PluginImpl;

@PluginImpl(interfaceName="com.mediatek.calendar.ext.IEditEventViewExt")
public class EditEventViewExt extends DefaultEditEventViewExt {

    private static final String TAG = "EditEventViewExt";
    private Context mContext;
    private LunarUtil mLunarUtil;

    public EditEventViewExt(Context context) {
        mContext = context;
        mLunarUtil = LunarUtil.getInstance(context);
    }

    /**
     * Sets additional Date Picker Switch UI Elements for EditEventView
     * @param model model can provide some info
     */
    @Override
    public void setDatePickerSwitchUi(final Activity activity, Object model,
            final Button startDateButton, final Button endDateButton, final String timezone,
            final Time startTime, final Time endTime) {

        final CalendarEventModel calendarEventModel = (CalendarEventModel) model;
        Log.d(TAG, "setDatePickerSwitchUi model = " + model);

        RadioGroup radioGroup = (RadioGroup) activity.findViewById(R.id.switch_date_picker);
        if (radioGroup == null) {
            return;
        }

        if (mLunarUtil.canShowLunarCalendar()) {
            radioGroup.setVisibility(View.VISIBLE);
            RadioButton radioBtn = (RadioButton) activity.findViewById(
                    calendarEventModel.mIsLunar ? R.id.switch_lunar : R.id.switch_gregorian);
            if (radioBtn == null) {
                LogUtil.d(TAG, "radio button is null, do nothing here.");
            } else {
                radioBtn.setChecked(true);
            }
            resetDateButton(activity, startDateButton, endDateButton, timezone, startTime, endTime);

            //set the listener.
            radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch(checkedId) {
                    case R.id.switch_lunar:
                        Log.d(TAG, "radioGroup set switch_lunar");
                        resetDateButton(activity, startDateButton, endDateButton, timezone, startTime, endTime);
                        calendarEventModel.mIsLunar = true;
                        break;
                    case R.id.switch_gregorian:
                    default:
                        Log.d(TAG, "radioGroup set switch_gregorian");
                        resetDateButton(activity, startDateButton, endDateButton, timezone, startTime, endTime);
                        calendarEventModel.mIsLunar = false;
                        break;
                    }
                }
            });
        } else {
            /// M: Reset button state when no lunar available @{
            RadioButton radioBtn = (RadioButton) activity.findViewById(R.id.switch_gregorian);
            if (radioBtn == null) {
                LogUtil.d(TAG, "radio button is null, do nothing here.");
            } else {
                radioBtn.setChecked(true);
            }
            resetDateButton(activity, startDateButton, endDateButton, timezone, startTime, endTime);
            /// @}
            radioGroup.setVisibility(View.GONE);
        }
    }

    /**
     * Gets the extended string such as lunar string to tell the Date
     * @param millis the millis time
     * @return "" means the extension won't handle the translation,
     * other means the extension had changed the millis to lunar string.
     */
    @Override
    public String getDateStringFromMillis(Activity activity, long millis) {
        String dateString = "";
        if (isEditingLunarDate(activity)) {
            Time time = new Time();
            time.set(millis);
            dateString = mLunarUtil.getLunarDateString(time.year, time.month + 1, time.monthDay);
        }
        Log.d(TAG, "getDateStringFromMillis, millis = " + millis + " to dateString = " + dateString);
        return dateString;
    }

    /**
     * Constructs a new DatePickerDialog instance with the given initial field
     * @param callBack    How the parent is notified that the date is set
     * @param year        The initial year of the dialog
     * @param monthOfYear The initial month of the dialog
     * @param dayOfMonth  The initial day of the dialog
     * @return a instance of DatePickerDialog
     */
    @Override
    public DatePickerDialog createDatePickerDialog(Activity activity, OnDateSetListener listener,
            int year, int monthOfYear, int dayOfMonth) {
        Log.d(TAG, "createDatePickerDialog");
        LunarDatePickerDialog dialog = LunarDatePickerDialog.newInstance(listener, year,
                monthOfYear, dayOfMonth, mContext);
        dialog.setShowLunarHeader(isEditingLunarDate(activity));
        return dialog;
    }

    private void resetDateButton(Activity activity, Button startDateButton, Button endDateButton,
            String timezone, Time startTime, Time endTime) {
        setDate(activity, startDateButton, timezone, startTime.toMillis(false /* use isDst */));
        setDate(activity, endDateButton, timezone, endTime.toMillis(false /* use isDst */));
    }

    private void setDate(Activity activity, TextView view, String timezone, long millis) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH
                | DateUtils.FORMAT_ABBREV_WEEKDAY;

        // Unfortunately, DateUtils doesn't support a timezone other than the
        // default timezone provided by the system, so we have this ugly hack
        // here to trick it into formatting our time correctly. In order to
        // prevent all sorts of craziness, we synchronize on the TimeZone class
        // to prevent other threads from reading an incorrect timezone from
        // calls to TimeZone#getDefault()
        // TODO fix this if/when DateUtils allows for passing in a timezone
        String dateString = null;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(timezone));
            ///M: #extension# the date string is extended
            ///TODO: the whole "setDate" should be extended @{
            dateString = getDateStringFromMillis(activity, millis);
            ///@}
            if (TextUtils.isEmpty(dateString)) {
                dateString = DateUtils.formatDateTime(activity, millis, flags);
            }
            // setting the default back to null restores the correct behavior
            TimeZone.setDefault(null);
        }
        view.setText(dateString);
    }

    private boolean isEditingLunarDate(Activity activity) {
        RadioGroup radioGroup = (RadioGroup) activity.findViewById(R.id.switch_date_picker);
        // Just make it more robust, radioGroup can't be null here.
        if (radioGroup == null) {
            Log.w(TAG, "isEditingLunarDate RadioGroup is null, how could be?");
            return false;
        }
        boolean isLunarChecked = radioGroup.getCheckedRadioButtonId() == R.id.switch_lunar ? true : false;
        Log.d(TAG, "isEditingLunarDate: " + isLunarChecked);
        return isLunarChecked;
    }

}
