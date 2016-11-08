package com.mediatek.phone.callforward;

import java.util.Calendar;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TimePicker;

import com.mediatek.op01.plugin.R;

public class CallForwardTimeActivity extends Activity implements TimePickerDialog.OnTimeSetListener{
    private static final String LOG_TAG = "CallForwardTimeActivity";

    public TimePickerDialog mTimePickerDialog;
    private OnTimeSetListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        long time = this.getIntent().getLongExtra("time", 0);
        log("onCreate() time: " + time);
        int hour = CallForwardUtils.getHour(time);
        int minute = CallForwardUtils.getMinute(time);
        log("onCreate() hour: " + hour + " minute:" + minute);
        mTimePickerDialog = new TimePickerDialog(this, R.style.TimePickerTheme, this, hour, minute, true);
        mTimePickerDialog.show();
        //Button timeCancelButton = mTimePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE);
        //timeCancelButton.setOnClickListener(OnClickTimeCancelListener());
        //mTimePickerDialog.setOnCancelListener(OnCancelListener());
        mTimePickerDialog.setOnDismissListener(OnDismissTimeDialogListener());
        setOnTimeSetListener(this);
    }

    public OnDismissListener OnDismissTimeDialogListener() {
        return new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
                return;
            }
        };
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // TODO Auto-generated method stub
        log("onTimeSet()");
        Intent intent = new Intent();
        String setTimeLab = "timeLabel";
        long gmtTime = CallForwardUtils.getTime(hourOfDay, minute, 0);
        intent.putExtra(setTimeLab, true);
        intent.putExtra("time", gmtTime);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void setOnTimeSetListener(OnTimeSetListener listener) {
        mListener = listener;
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed()");
        if(mTimePickerDialog != null) {
            mTimePickerDialog.dismiss();
        }
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
        if(mTimePickerDialog != null) {
            mTimePickerDialog.dismiss();
        }
    }

    /**
     * Log the message
     * @param msg the message will be printed
     */
    void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
