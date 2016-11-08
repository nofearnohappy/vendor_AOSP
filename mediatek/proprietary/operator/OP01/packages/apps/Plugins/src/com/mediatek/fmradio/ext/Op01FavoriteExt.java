/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.fmradio.ext;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.android.fmradio.FmStation;
import com.android.fmradio.FmUtils;
import com.android.fmradio.R;
import com.android.fmradio.views.FmScroller;
import com.android.fmradio.views.FmSnackBar;

import com.mediatek.common.PluginImpl;

/**
 * Op01 implementation of IFavoriteExt.
 */
@PluginImpl(interfaceName = "com.mediatek.fmradio.ext.IFavoriteExt")
public class Op01FavoriteExt extends DefaultFavoriteExt
                             implements Op01PopupMenu.ChangeFrequencyItemClickListener,
                             ChangeFreqDialog.FreqChangedListener {

    private static final String TAG = "Op01FavoriteExt";
    private Activity mActivity;
    private Context mContext;
    private FragmentManager mFragmentManager;
    private TextView mTextStationValue;
    private TextView mTextStationName;
    private FmScroller mScroller;
    private int mCurrentFreq;
    private SharedPreferences mPrefs;
    private int mFavoriteFreq;
    private ImageButton mButtonAddToFavorite = null;

    /**
     * Constructor of Op01 implementation of IFavoriteExt.
     * @param context application context.
     */
    public Op01FavoriteExt(Context context) {
        super(context);
    }

    @Override
    public PopupMenu createPopupMenu(Context context, View anchor, int frequency) {
        Log.d(TAG, "createPopupMenu");
        Op01PopupMenu popupMenu = new Op01PopupMenu(context, anchor, frequency);
        popupMenu.setChangeFrequencyItemClickListener(this);
        return popupMenu;
    }

    @Override
    public void setActivity(Activity fmMainActivity) {
        Log.d(TAG, "setActivity " + fmMainActivity);
        if (fmMainActivity != null) {
            mActivity = fmMainActivity;
            mFragmentManager = mActivity.getFragmentManager();
            mScroller = (FmScroller) mActivity.findViewById(R.id.multiscroller);
            mTextStationValue = (TextView) mActivity.findViewById(R.id.station_value);
            mTextStationName = (TextView) mActivity.findViewById(R.id.station_name);
            mContext = mActivity.getApplicationContext();
            mButtonAddToFavorite =
                    (ImageButton) mActivity
                    .findViewById(R.id.button_add_to_favorite);
            mPrefs = mActivity.getSharedPreferences("OP01FmRadio", Context.MODE_PRIVATE);
        }
    }

    @Override
    public void onResume() {
        boolean showDiag = mPrefs.getBoolean("OP01showDialog", false);
        Log.d(TAG, "onResume() showDialog = " + showDiag);
        if (showDiag) {
            int frequency = mPrefs.getInt("OP01Freq", 0);
            showChangeFreqDialog(frequency);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        SharedPreferences.Editor ed = mPrefs.edit();
        int frequency = getFreq();
        ChangeFreqDialog newFragment = (ChangeFreqDialog) mFragmentManager
                .findFragmentByTag("TAG_EDIT_FREQ");
        if (null != newFragment) {
            Log.d(TAG, "onPause() dismiss dialog");
            newFragment.dismissAllowingStateLoss();
            ed.putBoolean("OP01showDialog", true);
            ed.putInt("OP01Freq", frequency);
        } else {
            ed.putBoolean("OP01showDialog", false);
        }
        ed.commit();
    }

    /**
     * Show the change frequency dialog.
     *
     * @param frequency the frequency want to change
     */
    public void showChangeFreqDialog(int frequency) {
        Log.d(TAG, "showChangeFreqDialog(" + frequency + ")");
        String name = FmStation.getStationName(mContext, frequency);
        ChangeFreqDialog newFragment = ChangeFreqDialog.newInstance(name, frequency);
        newFragment.show(mFragmentManager, "TAG_EDIT_FREQ");
        newFragment.setFreqChangedListener(this);
        mFragmentManager.executePendingTransactions();
    }

    @Override
    public void onChangeFrequencyItemClick(int frequency) {
        mFavoriteFreq = frequency;
        showChangeFreqDialog(frequency);
    }

    @Override
    public void onFrequencyChanged() {
        Log.d(TAG, "onFrequencyChanged");
        int newStation = getFreq();
        if (newStation == mFavoriteFreq) {
            Log.d(TAG, "Frequency is not changed");
            return;
        }

        if (FmUtils.isValidStation(newStation)) {
            if (FmStation.isStationExist(mContext, newStation)) {
                Log.d(TAG, "deleteStationInDb " + newStation);
                FmStation.deleteStationInDb(mContext, newStation);
            }
            final int size = 1;
            ContentValues values = new ContentValues(size);
            values.put(FmStation.Station.FREQUENCY, newStation);
            Log.d(TAG, "updateStationToDb " + newStation);
            FmStation.updateStationToDb(mContext, mFavoriteFreq, values);
            int currentStation = getCurrentFreq();
            if (currentStation == mFavoriteFreq) {
                 mButtonAddToFavorite.setImageResource(R.drawable.btn_fm_favorite_off_selector);
                 mTextStationName.setText("");
            }
            mScroller.notifyAdatperChange();
            String title = mContext.getString(R.string.station_frequency_changed);
            FmSnackBar.make(mActivity, title, null, null,
                    FmSnackBar.DEFAULT_DURATION).show();
        } else {
            String title = mContext.getString(R.string.invalid_frequency);
            FmSnackBar.make(mActivity, title, null, null,
                    FmSnackBar.DEFAULT_DURATION).show();
        }
    }

    private int getCurrentFreq() {
        String currStationStr = mTextStationValue.getText().toString().trim();
        int currStation = -1;
        float currStationFreq = 0;
        try {
            currStationFreq = Float.parseFloat(currStationStr);
            currStation = FmUtils.computeStation(currStationFreq);
            Log.d(TAG, "getCurrentFreq() = " + currStation);
        } catch (NumberFormatException e) {
            e.printStackTrace();

        }
        return currStation;
    }

    private int getFreq() {
        ChangeFreqDialog dialogFragment =
                (ChangeFreqDialog) mFragmentManager
                .findFragmentByTag("TAG_EDIT_FREQ");
        if (dialogFragment == null) {
            Log.d(TAG, "Cannot find the dialog fragment");
            return - 1;
        }
        Dialog dialog = dialogFragment.getDialog();
        if (null == dialog) {
            Log.d(TAG, "Cannot get the dialog");
            return -1;
        }
        EditText frequencyEditText = (EditText) dialog.findViewById(R.id.dlg_edit_change_freq_text);
        if (frequencyEditText == null) {
            Log.d(TAG, "EditText is null");
            return -1;
        }
        String newStationFreqStr = frequencyEditText.getText().toString().trim();
        int newStation = -1;
        float newStationFreq = 0;
        try {
            newStationFreq = Float.parseFloat(newStationFreqStr);
            newStation = FmUtils.computeStation(newStationFreq);
            Log.d(TAG, "getFreq() = " + newStation);
        } catch (NumberFormatException e) {
            e.printStackTrace();

        }
        return newStation;
    }
}

/**
 * New PopupMenu that will hook OnMenuItemClickListener.
 */
class Op01PopupMenu extends PopupMenu implements OnMenuItemClickListener {

    private OnMenuItemClickListener mMenuItemClickListener;
    private int mFrequency;
    private ChangeFrequencyItemClickListener mListener;

    /**
     * Listen for change frequency event.
     */
    public interface ChangeFrequencyItemClickListener {
        /**
         * Callback when click change frequency menu.
         *
         * @param frequency The frequency want to change
         */
        void onChangeFrequencyItemClick(int frequency);
    }

    /**
     * Constructer of Op01PopupMenu.
     *
     * @param context Context for the PopupMenu.
     * @param anchor Anchor view for this popup. The popup will appear below the anchor if there
     *               is room, or above it if there is not.
     * @param frequency the frequency of the favorite.
     */
    public Op01PopupMenu(Context context, View anchor, int frequency) {
        super(context, anchor);
        mFrequency = frequency;
    }

    /**
     * Set the event listener.
     */
    public void setChangeFrequencyItemClickListener(
        ChangeFrequencyItemClickListener l) {
        mListener = l;
    }

    @Override
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mMenuItemClickListener = listener;
        super.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.change_frequency) {
            if (mListener != null) {
                mListener.onChangeFrequencyItemClick(mFrequency);
            }
            return true;
        }
        if (mMenuItemClickListener != null) {
            return mMenuItemClickListener.onMenuItemClick(item);
        }
        return false;
    }
}

/**
 * Edit favorite station name and frequency.
 */
class ChangeFreqDialog extends DialogFragment
                       implements DialogInterface.OnClickListener {
    private static final String STATION_NAME = "station_name";
    private static final String STATION_FREQ = "station_freq";
    private EditText mEditTextFrequency = null;
    private FreqChangedListener mListener;

    /**
     * Listen for frequency changed event.
     */
    public interface FreqChangedListener {
        /**
         * Callback when click the save button of the dialog.
         *
         */
        void onFrequencyChanged();
    }

    /**
     * Create change frequency dialog instance.
     *
     * @param stationName The station name
     * @param stationFreq The station frequency
     *
     * @return edit favorite dialog
     */
    public static ChangeFreqDialog newInstance(String stationName, int stationFreq) {
        ChangeFreqDialog fragment = new ChangeFreqDialog();
        Bundle args = new Bundle(2);
        args.putString(STATION_NAME, stationName);
        args.putInt(STATION_FREQ, stationFreq);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Set the frequency changed listener.
     */
    void setFreqChangedListener(FreqChangedListener l) {
        mListener = l;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mListener != null) {
            mListener.onFrequencyChanged();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String stationName = getArguments().getString(STATION_NAME);
        int stationFreq = getArguments().getInt(STATION_FREQ);
        View v = View.inflate(getActivity(), R.layout.editfreq, null);
        mEditTextFrequency = (EditText) v.findViewById(R.id.dlg_edit_change_freq_text);

        final int lengthOf100Khz = 5;
        final int maxFrequencyLength = lengthOf100Khz;
        mEditTextFrequency.setFilters(new InputFilter[] {
                mFilter,
                new InputFilter.LengthFilter(maxFrequencyLength)
        });

        mEditTextFrequency.addTextChangedListener(mWatcher100KHZ);
        mEditTextFrequency.setText(FmUtils.formatStation(stationFreq));
        mEditTextFrequency.requestFocus();
        mEditTextFrequency.requestFocusFromTouch();
        Editable text = mEditTextFrequency.getText();
        Selection.setSelection(text, text.length());
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.change_frequency))
                .setView(v)
                .setPositiveButton(R.string.save, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();
        String toName = mEditTextFrequency.getText().toString();
        // empty or blank or white space only name is not allowed
        toggleSaveButton(toName != null && TextUtils.getTrimmedLength(toName) > 0);
    }

    InputFilter mFilter = new InputFilter() {

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            final int accuracy = 1;

            if ("".equals(source.toString())) {
                return null;
            }

            // according the point divide string
            String[] splitArray = dest.toString().split("\\.");
            // input have point, should delete the redundant
            if (splitArray.length > 1) {
                String fraction = splitArray[1];
                int deleteIndex = fraction.length() + 1 - accuracy;
                if (deleteIndex > 0) {
                    int dotIndex = dest.toString().indexOf(".") + 1;
                    if (dstart >= dotIndex) {
                        if ((end - deleteIndex) >= start) {
                            return source.subSequence(start, end - deleteIndex);
                        }
                    } else {
                        return dest.subSequence(dstart, dend) + source.toString();
                    }
                }
            }
            return null;
        }
    };

    // Add for 100khz
    // add for overwrite frequency feature
    private TextWatcher mWatcher100KHZ = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (null == getDialog()) {
                return;
            }
            toggleSaveButton(TextUtils.getTrimmedLength(s) > 0);
            CharSequence cs = mEditTextFrequency.getText();
            float frequency = 0;
            try {
                frequency = Float.parseFloat(cs.toString());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            final int convertData = 10;
            int station = Math.round(frequency * convertData);
            Button positiveButton = ((AlertDialog) getDialog())
                    .getButton(DialogInterface.BUTTON_POSITIVE);

            if (null != positiveButton) {
                if (FmStation.isStationExist(
                        getActivity().getApplicationContext(), station)) {
                    positiveButton.setText(R.string.edit_frequency_overwrite_text);
                } else {
                    positiveButton.setText(R.string.save);
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private void toggleSaveButton(boolean isEnabled) {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }
        final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setEnabled(isEnabled);
    }
}
