package com.mediatek.miravision.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;

import com.mediatek.miravision.setting.MiraVisionJni;
import com.mediatek.miravision.utils.Utils;

public class PictureModeFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "Miravision/PictureModeFragment";
    public static final String SHARED_PREFERENCES_USER_MODE_KEY = "user_mode_notify";

    private static final String KEY_STANDARD_PREF = "standard_pref";
    private static final String KEY_VIVID_PREF = "vivid_pref";
    private static final String KEY_USER_PREF = "user_mode_pref";

    private Utils mUtils;
    private RadioButtonPreference mStandardPref;
    private RadioButtonPreference mVividPref;
    private RadioButtonPreference mUserModePref;
    private RadioButtonPreference mDefaultModePref;

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference == mUserModePref
                && !mUtils.getSharePrefBoolenValue(SHARED_PREFERENCES_USER_MODE_KEY)) {
            new RemiderDialogFragment().show(getFragmentManager(), "usermode reminder");
        }
        if (preference != null && preference instanceof RadioButtonPreference) {
            if (mDefaultModePref != null) {
                mDefaultModePref.setChecked(false);
            }
            mDefaultModePref = (RadioButtonPreference) preference;
            if (mDefaultModePref == mStandardPref) {
                MiraVisionJni.nativeSetPictureMode(MiraVisionJni.PIC_MODE_STANDARD);
                // Gamma will check mode, no need APP set
                // MiraVisionJni.setGammaIndex(MiraVisionJni.getGammaIndexRange().defaultValue);
            } else if (mDefaultModePref == mVividPref) {
                MiraVisionJni.nativeSetPictureMode(MiraVisionJni.PIC_MODE_VIVID);
                // Gamma will check mode, no need APP set
                // MiraVisionJni.setGammaIndex(MiraVisionJni.getGammaIndexRange().defaultValue);
            } else if (mDefaultModePref == mUserModePref) {
                MiraVisionJni.nativeSetPictureMode(MiraVisionJni.PIC_MODE_USER_DEF);
            } else {
                // log
            }
            MiraVisionActivity.sDrawerListAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "PictureModeFragment onCreate()");
        addPreferencesFromResource(R.xml.picture_mode_settings);
        mUtils = new Utils(getActivity());
        initializeAllPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "PictureModeFragment onResume()");
        updateAllPreferences();
    }

    private void initializeAllPreferences() {
        mStandardPref = (RadioButtonPreference) findPreference(KEY_STANDARD_PREF);
        mStandardPref.setOnPreferenceChangeListener(this);
        mVividPref = (RadioButtonPreference) findPreference(KEY_VIVID_PREF);
        mVividPref.setOnPreferenceChangeListener(this);
        mUserModePref = (RadioButtonPreference) findPreference(KEY_USER_PREF);
        mUserModePref.setOnPreferenceChangeListener(this);
    }

    private void updateAllPreferences() {
        int defaultMode = MiraVisionJni.nativeGetPictureMode();
        switch (defaultMode) {
        case MiraVisionJni.PIC_MODE_STANDARD:
            mDefaultModePref = mStandardPref;
            break;
        case MiraVisionJni.PIC_MODE_VIVID:
            mDefaultModePref = mVividPref;
            break;
        case MiraVisionJni.PIC_MODE_USER_DEF:
            mDefaultModePref = mUserModePref;
            break;
        default:
            mDefaultModePref = mStandardPref;
            break;
        }
        mDefaultModePref.setChecked(true);
    }

    class RemiderDialogFragment extends DialogFragment {

        private AlertDialog mAlertDialog;
        private CheckBox mNotRemindAgainCheck;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.d(TAG, "RemiderDialogFragment onCreateDialog()");
            mAlertDialog = new AlertDialog.Builder(getActivity()).setIcon(
                    android.R.drawable.ic_dialog_alert).setTitle(R.string.picture_mode_user)
                    .setView(createDialogView()).setPositiveButton(
                            com.android.internal.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mNotRemindAgainCheck.isChecked()) {
                                        mUtils.setSharePrefBoolenValue(
                                                SHARED_PREFERENCES_USER_MODE_KEY, true);
                                    }
                                }
                            }).setCancelable(false).create();
            mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

            return mAlertDialog;
        }

        private View createDialogView() {
            final LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.notify_dialog_view, null);
            mNotRemindAgainCheck = (CheckBox) view.findViewById(R.id.checkbox);
            mNotRemindAgainCheck.setChecked(true);
            return view;
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "RemiderDialogFragment onDestroy()");
            mAlertDialog = null;
            super.onDestroy();
        }
    }
}
