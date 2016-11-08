package com.mediatek.rcs.message.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.content.DialogInterface.OnClickListener;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.mediatek.rcs.message.cloudbackup.CloudMsgBackupRestore;
import com.mediatek.rcs.message.R;

public class RcsSettingsActivity extends PreferenceActivity {

    private static final String TAG = "RcsSettingsActivity";
    private static final String SEND_ORG_PIC = "pref_key_send_org_pic";
    //private static final String SEND_MSG_MODE = "pref_key_send_mode";
    private static final String WLAN_BACKUP = "pref_key_wlan_backup";
    private static final String RCS_PREFERENCE = "com.mediatek.rcs.message_preferences";

    private static final String BACKUPTOSERVER = "pref_key_backup_to_server";
    private static final String RESTORE = "pref_key_restore_backup";
    CheckBoxPreference mSendOrgPic;
    //CheckBoxPreference mSendMode;

    private Preference mReportNumCenterPref;
    private EditText mNumberText;
    private AlertDialog mNumberTextDialog;
    private Context mContext;
    private static final int MAX_EDITABLE_LENGTH = 50;
    private static final String REPORT_NUMBER_CENTER = "pref_key_report_number_center";
    private static final String RCS_REPORT_NUM = "+86100869999";

    SwitchPreference mWlanBackup;
    Preference mBackupToServer;
    Preference mRestore;
    private CloudMsgBackupRestore mcloudBr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.rcs_setting));
        actionBar.setDisplayHomeAsUpEnabled(true);
        setMessagePreferences();
    }

    private void setMessagePreferences() {
        addPreferencesFromResource(R.xml.rcspreferences);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mSendOrgPic = (CheckBoxPreference) findPreference(SEND_ORG_PIC);
        if (mSendOrgPic != null) {
            mSendOrgPic.setChecked(sp.getBoolean(mSendOrgPic.getKey(), false));
        }
/*        mSendMode = (CheckBoxPreference) findPreference(SEND_MSG_MODE);
        if (mSendMode != null) {
            mSendMode.setChecked(sp.getBoolean(mSendMode.getKey(), true));
        }*/
        mWlanBackup = (SwitchPreference) findPreference(WLAN_BACKUP);
        if (mWlanBackup != null) {
            mWlanBackup.setChecked(sp.getBoolean(mWlanBackup.getKey(), true));
        }

        mReportNumCenterPref = findPreference(REPORT_NUMBER_CENTER);
        //mReportNumCenterPref.setEnabled(true);

        mBackupToServer = findPreference(BACKUPTOSERVER);
        mRestore = findPreference(RESTORE);

    }

    private boolean isWilanConnected() {
        boolean isWilanConnect = false;
        ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        State wifiState = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (wifiState == State.CONNECTED) {
            isWilanConnect = true;
        }
        Log.d(TAG, "wifiState isWilanConnect = " + isWilanConnect);
        return isWilanConnect;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean wilanSwitch = sp.getBoolean(mWlanBackup.getKey(), true);
        Log.d(TAG, "wilanSwitch = " + wilanSwitch);
        String key = preference.getKey();
        if (key != null && key.equals(BACKUPTOSERVER)) {
            Log.d(TAG, "onPreferenceTreeClick backup");
            if (mcloudBr == null) {
                mcloudBr = new CloudMsgBackupRestore(RcsSettingsActivity.this);
            } else {
                mcloudBr.init();
            }
            Builder dialogBuilder = null;
            if (mcloudBr.isCloudBrFeatureAvalible()) {
                if (!isWilanConnected() && wilanSwitch) {
                    dialogBuilder = mcloudBr.createQueryBackupDialog();
                    dialogBuilder.show();
                } else {
                    mcloudBr.startBackup();
                }
            }
        } else if (key != null && key.equals(RESTORE)) {
            Log.d(TAG, "onPreferenceTreeClick restore");
            if (mcloudBr == null) {
                mcloudBr = new CloudMsgBackupRestore(RcsSettingsActivity.this);
            } else {
                mcloudBr.init();
            }
            if (mcloudBr.isCloudBrFeatureAvalible()) {
                mcloudBr.createRestoreDialog(!isWilanConnected() && wilanSwitch);
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if (mcloudBr != null) {
            mcloudBr.destroy();
        }
        mcloudBr = null;
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

    public static boolean getSendMSGStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        boolean sendOrgPic = sp.getBoolean(SEND_ORG_PIC, false);
        Log.d(TAG, "getSendMSGStatus : sendOrgPic = "+ sendOrgPic);
        return sendOrgPic;
    }

/*    public static boolean getSendMSGMode(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        boolean sendMode = sp.getBoolean(SEND_MSG_MODE, true);
        Log.d(TAG, "getSendMSGMode : sendMode = "+ sendMode);
        return sendMode;
    }
    */
    /**
     *
     * @param c
     * @return
     */
    public static boolean getWlanBackup(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        boolean wlanBackup = sp.getBoolean(WLAN_BACKUP, true);
        Log.d(TAG, "getWlanBackup : wlanBackup = "+ wlanBackup);
        return wlanBackup;
    }

//    public static boolean getAutoAcceptGroupChatInvitation(Context c) {
//        SharedPreferences sp = c.getSharedPreferences(RCS_PREFERENCE, MODE_WORLD_READABLE);
//        boolean autoAccept = sp.getBoolean("pref_key_auto_accept_group_invitate", false);
//        Log.d(TAG, "getAutoAcceptGroupChatInvitation : autoAccept = "+ autoAccept);
//        return autoAccept;
//    }

    public static String getReportNum(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        String number = sp.getString(REPORT_NUMBER_CENTER,RCS_REPORT_NUM);
        Log.d(TAG, "getReportNum : number = "+ number);
        return number;
    }

    // @Override
    // public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    //     if (preference == mReportNumCenterPref) {
    //         AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    //         mNumberText = new EditText(dialog.getContext());
    //         mNumberText.setHint(R.string.type_to_compose_text_enter_to_send);
    //         mNumberText.computeScroll();
    //         mNumberText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
    //                 MAX_EDITABLE_LENGTH) });
    //         mNumberText.setInputType(EditorInfo.TYPE_CLASS_PHONE);

    //         SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    //         String number = sp.getString(REPORT_NUMBER_CENTER,RCS_REPORT_NUM);
    //         mNumberText.setText(number);
    //         mNumberText.setTextColor(R.color.black);
    //         mNumberTextDialog = dialog.setTitle(
    //                 R.string.report_num_center).setView(mNumberText).setPositiveButton(
    //                 R.string.OK, new PositiveButtonListener()).setNegativeButton(
    //                 R.string.Cancel, new NegativeButtonListener()).show();
    //     }
    //     return super.onPreferenceTreeClick(preferenceScreen, preference);
    // }

    private class PositiveButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // write to the SIM Card.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor prefs = sp.edit();
            prefs.putString(REPORT_NUMBER_CENTER,mNumberText.getText().toString());
            prefs.commit();

//            if (!isValidAddr(mNumberText.getText().toString())) {
//                String num = mNumberText.getText().toString();
//                String strUnSpFormat = getResources().getString(R.string.unsupported_media_format,
//                        "");
//                Toast.makeText(getApplicationContext(), strUnSpFormat, Toast.LENGTH_SHORT).show();
//                return;
//            }

        }
    }

    private class NegativeButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // cancel
            dialog.dismiss();
        }
    }
}
