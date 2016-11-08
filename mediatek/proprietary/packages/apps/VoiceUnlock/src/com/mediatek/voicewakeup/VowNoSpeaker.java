package com.mediatek.voicewakeup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.mediatek.voiceunlock.R;
import com.mediatek.voiceunlock.SettingsPreferenceFragment;

/**
 * For voice wake up's no speaker id mode
 * 
 */
public class VowNoSpeaker extends PreferenceActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, VowNoSpeakerFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return (VowNoSpeakerFragment.class.getName().equals(fragmentName));
    }

    public static class VowNoSpeakerFragment extends SettingsPreferenceFragment {

        static final String TAG = "VowNoSpeaker";
        // / { @ Same with voice ui
        // wakeup id 0: camera, 1: contacts, 2: googleNow
        public static final String KEY_COMMAND_ID = "command_id";
        // wakeup package name
        public static final String KEY_COMMAND_PACKAGENAME = "command_packagename";
        // wakeup class name
        public static final String KEY_COMMAND_CLASSNAME = "command_classname";
        // wakeup keyword
        public static final String KEY_COMMAND_KEYWORD = "command_keyword";
        // / @ }

        private static final String PREF_KEY_PLAY_COMMAND = "play_command";
        private static final String PREF_KEY_INFORCE_COMMAND = "reinforece_command";

        private Context mContext;
        private Utils mUtils;
        private Preference mPlayPref;

        private int mCommandId;
        private String mCommandKeyword;
        private String[] mCmdKeywordArray;
        private String mComponetStr;
        private String mAppLabel;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Activity activity = getActivity();
            mContext = activity.getBaseContext();
            mUtils = Utils.getInstance();
            mUtils.setContext(mContext);

            mCommandId = activity.getIntent().getIntExtra(KEY_COMMAND_ID, 0);
            mCmdKeywordArray = activity.getIntent().getStringArrayExtra(KEY_COMMAND_KEYWORD);
            mCommandKeyword = mUtils.getKeywords(mCmdKeywordArray, ",");
            Log.d("@M_" + TAG, "mCommandId = " + mCommandId + ", mCommandKeyword = " + mCommandKeyword);
            String packageStr = activity.getIntent().getStringExtra(KEY_COMMAND_PACKAGENAME);
            String classStr = activity.getIntent().getStringExtra(KEY_COMMAND_CLASSNAME);
            ComponentName component = new ComponentName(packageStr, classStr);
            mAppLabel = mUtils.getAppLabel(component);
            mComponetStr = component.flattenToShortString();
            Log.d("@M_" + TAG, "component = " + mComponetStr + "mAppLabel = " + mAppLabel);

            addPreferencesFromResource(R.xml.voice_wakeup);
            // set title
            CharSequence msg = mContext.getString(R.string.voice_wake_up_command_title, mAppLabel);
            ((PreferenceActivity) activity).showBreadCrumbs(msg, msg);
            initPreferences();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                Preference preference) {
            final String key = preference.getKey();
            Log.d("@M_" + TAG, "onPreferenceTreeClick commandId = " + key);

            if (PREF_KEY_PLAY_COMMAND.equals(key)) {
                mUtils.playCommand(this, mCommandId, mContext.getString(
                        R.string.voice_wake_up_command_summary, mAppLabel),
                        Utils.VOW_NO_SPEAKER_MODE);
            } else if (PREF_KEY_INFORCE_COMMAND.equals(key)) {
                // no speaker id mode
                mUtils.updateCommand(this, mCommandId, mComponetStr,
                        mPlayPref.isEnabled() ? Utils.COMMAND_TYPE_MODIFY
                                : Utils.COMMAND_TYPE_RECORD, Utils.VOW_NO_SPEAKER_MODE,
                        mCmdKeywordArray);
            }
            return true;
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePlayStatus();
        }

        private void initPreferences() {
            Log.d("@M_" + TAG, "initPreferences ");
            mPlayPref = new Preference(mContext);
            mPlayPref.setKey(PREF_KEY_PLAY_COMMAND);
            mPlayPref.setTitle(getString(R.string.play_command_action));
            mPlayPref.setSummary(mCommandKeyword);
            getPreferenceScreen().addPreference(mPlayPref);
            Preference preference = new Preference(mContext);
            preference.setKey(PREF_KEY_INFORCE_COMMAND);
            preference.setTitle(getString(R.string.reinforce_command_action));
            preference.setSummary(getString(R.string.reinforce_command_action_summary));
            getPreferenceScreen().addPreference(preference);
        }

        private void updatePlayStatus() {
            mPlayPref.setEnabled(Settings.System.getVoiceCommandValue(getContentResolver(),
                    Settings.System.BASE_VOICE_WAKEUP_COMMAND_KEY, mCommandId) != null);
        }
    }
}
