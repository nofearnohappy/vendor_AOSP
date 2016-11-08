package com.mediatek.mms.plugin;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.mediatek.op09.plugin.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * M: AdvancedSearchActivity ; ADD FOR OP09;
 *
 */
public class AdvancedSearchActivity extends Activity {
    private static final String TAG = "Mms/AdvancedSearchActivity";
    private static final int DATA_CERTAIN_ONE_DAY = 0;
    private static final int DATA_CERTAIN_SEVEN_DAY = 1;
    private static final int DATA_CERTAIN_THIRTY_DAY = 2;

    private static final int DATA_PICKER_FROM = 1;
    private static final int DATA_PICKER_TO = 2;
    private static final int DATA_CERTAIN_TIME = 3;

    private static final String DATE_FROM = "from_date";
    private static final String DATE_TO = "to_date";
    private static final String DATE_CERTAIN = "certain_date";

    private static final int BASE_YEAR = 1900;
    private static final long ONE_DAY_MILLI_SECOND = 24 * 60 * 60 * 1000L;

    private EditText mCertainTimeText;
    private AlertDialog mCertainTimePickerDialog;
    private Button mCertainTimeSearchButton;
    private EditText mFromText;
    private EditText mToText;
    private Button mSearchButton;
    private DatePickerDialog mFromPickerDialog;
    private DatePickerDialog mToPickerDialog;
    private ActionBar mActionBar = null;

    private boolean mIsClearDate = false;
    private int mCurrentCertainDate = DATA_CERTAIN_SEVEN_DAY;
    private int mCurrentDatePicker = 0;
    private long mFromDate = 0;
    private long mToDate = 0;

    private boolean mEverSet = false;
    private static final String DATE_EVER_SET = "date_ever_set";

    private java.text.DateFormat mDateFormater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_search_activity);
        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(false);
        initResource();
        setTitle(R.string.search_by_time_period);
        if (savedInstanceState != null) {
            setSaveInstanceState(savedInstanceState);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return false;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        initContent();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        dismissDialog();
        super.onDestroy();
    }

    private void initContent() {
        mDateFormater = DateFormat.getDateFormat(this);
        setCertainTimeText();

        if (!mEverSet) {
            Date date = new Date(System.currentTimeMillis());
            setDate(date.getYear() + BASE_YEAR, date.getMonth(), date.getDate(), DATA_PICKER_FROM);
            setDate(date.getYear() + BASE_YEAR, date.getMonth(), date.getDate(), DATA_PICKER_TO);
            return;
        }

        if (mFromText != null) {
            mFromText.setText(mIsClearDate ? "" :
                    mDateFormater.format(new Date(mFromDate)));
        }
        if (mToText != null) {
            mToText.setText(mIsClearDate ? "" :
                    mDateFormater.format(new Date(mToDate - ONE_DAY_MILLI_SECOND)));
        }
    }

    private void initResource() {
        mCertainTimeText = (EditText) findViewById(R.id.certain_time_text);
        /// M: deal with the touch event;
        mCertainTimeText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                Log.e(TAG, "mCertainTimeText.onTouch()");
                if (MotionEvent.ACTION_UP == arg1.getActionMasked()) {
                    showCertainTimePickerDialog();
                    return true;
                }
                return false;
            }
        });
        mCertainTimeSearchButton = (Button) findViewById(R.id.certain_time_search_button);
        /// M: set click listener for certainTimeSearchBtn;
        mCertainTimeSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long toDate = getTodayDate();
                long fromDate = getFromDate(toDate);
                Log.d(TAG, "mCertainTimeText.onFocusChange(): fromDate = " + fromDate +
                        ", toDate = " + toDate);
                Intent intent = new Intent();
                intent.setClassName("com.android.mms", "com.android.mms.ui.SearchActivity");
                intent.putExtra(Op09SearchActivityExt.ADVANCED_SEARCH_QUERY, true);
                intent.putExtra(Op09SearchActivityExt.ADVANCED_SEARCH_BEGIN_DATE,
                                                (fromDate > 0L) ? fromDate : 0L);
                intent.putExtra(Op09SearchActivityExt.ADVANCED_SEARCH_END_DATE, toDate);
                startActivity(intent);
            }
        });

        mFromText = (EditText) findViewById(R.id.from_date_text);
        /// M: set on touch listener for : if the text's focused has not got. then user click it ,
        ///it will not show the date picker dialog.
        mFromText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (MotionEvent.ACTION_UP == arg1.getActionMasked()) {
                    processEditTouch(DATA_PICKER_FROM);
                    return true;
                }
                return false;
            }
        });

        mToText = (EditText) findViewById(R.id.to_date_text);
        mToText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (MotionEvent.ACTION_UP == arg1.getActionMasked()) {
                    processEditTouch(DATA_PICKER_TO);
                    return true;
                }
                return false;
            }
        });

        mSearchButton = (Button) findViewById(R.id.search_button);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFromDate > 0 && mToDate > 0 && mFromDate >= mToDate) {
                    Toast.makeText(AdvancedSearchActivity.this, R.string.search_error_wrong_date,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent();
                intent.setClassName("com.android.mms", "com.android.mms.ui.SearchActivity");
                intent.putExtra(Op09SearchActivityExt.ADVANCED_SEARCH_QUERY, true);
                intent.putExtra(Op09SearchActivityExt.ADVANCED_SEARCH_BEGIN_DATE, mFromDate);
                intent.putExtra(Op09SearchActivityExt.ADVANCED_SEARCH_END_DATE, mToDate);
                startActivity(intent);
            }
        });
    }

    /**
     *
     * @param pickerType
     */
    private void processEditTouch(int pickerType) {
        mCurrentDatePicker = pickerType;
        if (mFromPickerDialog == null || mToPickerDialog == null) {
            initDatePickerDialog();
        }
        if (mFromPickerDialog != null && pickerType == DATA_PICKER_FROM) {
            showDatePickerDialog(mFromPickerDialog, DATE_FROM);
        } else if (mToPickerDialog != null && pickerType == DATA_PICKER_TO) {
            showDatePickerDialog(mToPickerDialog, DATE_TO);
        } else {
            Log.e(TAG, "onClick(): init date picker failed; type:" + pickerType);
        }
    }

    private void initCertainTimePickerDialog() {
        if (mCertainTimePickerDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.search_certain_time_dialog_title);
            builder.setSingleChoiceItems(
                getResources().getTextArray(R.array.certain_search_string_array),
                mCurrentCertainDate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "mCertainTimePickerDialog.onClick(): which = " + which);
                        mCurrentCertainDate = which;
                        setCertainTimeText();
                        dialog.dismiss();
                    }
                });
            builder.setNegativeButton(android.R.string.cancel,
                                                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            mCertainTimePickerDialog = builder.create();
        }
    }

    private void showCertainTimePickerDialog() {
        if (mCertainTimePickerDialog == null) {
            initCertainTimePickerDialog();
        }
        if (mCertainTimePickerDialog.isShowing()) {
            mCertainTimePickerDialog.dismiss();
        }
        mCertainTimePickerDialog.show();
    }

    private long getTodayDate() {
        int year, month, day;
        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        return c.getTimeInMillis() + ONE_DAY_MILLI_SECOND;
    }

    private long getFromDate(long toDate) {
        Log.d(TAG, "getFromDate(): mCurrentCertainDate = " + mCurrentCertainDate +
                ", toDate = " + toDate);
        switch (mCurrentCertainDate) {
            case DATA_CERTAIN_ONE_DAY:
                return toDate - ONE_DAY_MILLI_SECOND;
            case DATA_CERTAIN_SEVEN_DAY:
                return toDate - (ONE_DAY_MILLI_SECOND * 7);
            case DATA_CERTAIN_THIRTY_DAY:
                return toDate - (ONE_DAY_MILLI_SECOND * 30);
            default:
                Log.e(TAG, "getFromDate(): error case!", new Exception());
        }
        return 0;
    }

    private void initDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        mFromPickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.d(TAG, "onDateSet(): set begin date to text.");
                setDate(year, monthOfYear, dayOfMonth, DATA_PICKER_FROM);
                mEverSet = true;
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH));
        setDateRange(mFromPickerDialog);

        mToPickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.d(TAG, "onDateSet(): set end date to text.");
                setDate(year, monthOfYear, dayOfMonth, DATA_PICKER_TO);
                mEverSet = true;
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
        setDateRange(mToPickerDialog);
    }

    private void showDatePickerDialog(DatePickerDialog datePickerDialog, String type) {
        if (datePickerDialog.isShowing()) {
            datePickerDialog.dismiss();
        }
        Calendar c = Calendar.getInstance();;
        if (type == DATE_FROM) {
            c.setTime(new Date(mFromDate));
        } else {
            c.setTime(new Date(mToDate - ONE_DAY_MILLI_SECOND));
        }
        datePickerDialog.show();
        if (c != null) {
            datePickerDialog.updateDate(c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
        }
    }

    /**
     * set date range.
     * @param dialog
     */
    private void setDateRange(DatePickerDialog dialog) {
        if (dialog != null) {
            Time minTime = new Time();
            Time maxTime = new Time();
            /// M: 1970/1/1
            minTime.set(0, 0, 0, 1, 0, 1970);
            /// M: 2037/12/31
            maxTime.set(59, 59, 23, 31, 11, 2037);
            long maxDate = maxTime.toMillis(false);
            /// M: in millsec
            maxDate = maxDate + 999;
            long minDate = minTime.toMillis(false);
            /// M: set min date
            dialog.getDatePicker().setMinDate(minDate);
            /// M: set max date;
            dialog.getDatePicker().setMaxDate(maxDate);
        }
    }

    private void setDate(int year, int month, int day, int currentDateType) {
        Log.d(TAG, "setDate(): year = " + year + ", month = " + month + ", day = " + day +
                ", mCurrentDatePicker = "
            + mCurrentDatePicker + ", mIsClearDate = " + mIsClearDate);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = mIsClearDate ? 0 : c.getTimeInMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        switch (currentDateType) {
            case DATA_PICKER_FROM:
                mFromDate = when;
                mFromText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                Log.d(TAG, "setDate(): mFromDate = " + mFromDate + ", " +
                sdf.format(new Date(mFromDate)));
                if (mFromDate >= mToDate) {
                    mToDate = when + (when == 0 ? 0 : ONE_DAY_MILLI_SECOND);
                    mToText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                }
                break;
            case DATA_PICKER_TO:
                /// M: make the end date from 00:00:00 today to 00:00:00 tomorrow
                mToDate = when + (when == 0 ? 0 : ONE_DAY_MILLI_SECOND);
                mToText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                Log.d(TAG, "setDate(): mToDate = " + mToDate + ", " +
                sdf.format(new Date(mToDate)));
                if (mFromDate >= mToDate) {
                    mFromDate = when;
                    mFromText.setText(mIsClearDate ? "" : mDateFormater.format(new Date(when)));
                }
                break;
            default:
                break;
        }
        mIsClearDate = false;
    }

    private void dismissDialog() {
        if (mFromPickerDialog != null && mFromPickerDialog.isShowing()) {
            mFromPickerDialog.dismiss();
        }
        if (mToPickerDialog != null && mToPickerDialog.isShowing()) {
            mToPickerDialog.dismiss();
        }
        if (mCertainTimePickerDialog != null && mCertainTimePickerDialog.isShowing()) {
            mCertainTimePickerDialog.dismiss();
        }
    }

    private void setCertainTimeText() {
        switch (mCurrentCertainDate) {
            case DATA_CERTAIN_ONE_DAY:
                mCertainTimeText.setText(R.string.search_within_one_day);
                break;
            case DATA_CERTAIN_SEVEN_DAY:
                mCertainTimeText.setText(R.string.search_within_seven_days);
                break;
            case DATA_CERTAIN_THIRTY_DAY:
                mCertainTimeText.setText(R.string.search_within_thirty_days);
                break;
            default:
                break;
        }
    }

    /**
     * set saved instance state.
     * @param savedInstanceState
     */
    private void setSaveInstanceState(Bundle savedInstanceState) {
        int type = savedInstanceState.getInt("type");
        Log.d(TAG, "SetSaveInstanceState Restore disappear state = " + type);
        mFromDate = savedInstanceState.getLong(DATE_FROM);
        mToDate = savedInstanceState.getLong(DATE_TO);
        mCurrentCertainDate = savedInstanceState.getInt(DATE_CERTAIN);
        mEverSet = savedInstanceState.getBoolean(DATE_EVER_SET);

        if (type == 0) {
            return;
        }
        if (type == DATA_CERTAIN_TIME) {
            showCertainTimePickerDialog();
            return;
        }
        int year = savedInstanceState.getInt("year");
        int month = savedInstanceState.getInt("month");
        int day = savedInstanceState.getInt("day");
        processEditTouch(type);
        if (type == DATA_PICKER_FROM && mFromPickerDialog != null) {
            mFromPickerDialog.updateDate(year, month, day);
        } else if (type == DATA_PICKER_TO && mToPickerDialog != null) {
            mToPickerDialog.updateDate(year, month, day);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        Log.d(TAG, "saveDatePickerStatus: currentDatePicker = " + mCurrentDatePicker);
        super.onSaveInstanceState(b);

        b.putLong(DATE_FROM, mFromDate);
        b.putLong(DATE_TO, mToDate);
        b.putInt(DATE_CERTAIN, mCurrentCertainDate);
        b.putBoolean(DATE_EVER_SET, mEverSet);

        DatePickerDialog dd =
                mCurrentDatePicker == DATA_PICKER_FROM ? mFromPickerDialog : mToPickerDialog;
        if (dd != null && dd.isShowing()) {
            DatePicker dp = dd.getDatePicker();
            b.putInt("type", mCurrentDatePicker);
            b.putInt("year", dp.getYear());
            b.putInt("month", dp.getMonth());
            b.putInt("day", dp.getDayOfMonth());
            return;
        }
        if (mCertainTimePickerDialog != null && mCertainTimePickerDialog.isShowing()) {
            b.putInt("type", DATA_CERTAIN_TIME);
        }
    }
}
