package com.mediatek.engineermode.voicesettings;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.mediatek.engineermode.R;

public class VoiceCommonSetting extends Activity implements OnClickListener {
    private Spinner mSpnClean;
    private Spinner mSpnNoisy;
    private Spinner mSpnTraining;
    private Spinner mSpnTesting;
    private int mWakeupMode = 0;
    private Button mBtnSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_common_setting);
        mWakeupMode = Settings.System.getInt(getContentResolver(),
                Settings.System.VOICE_WAKEUP_MODE, 0);
        if (mWakeupMode != VoiceSettingWrapper.WAKEUP_MODE_KEYWORD
                && mWakeupMode != VoiceSettingWrapper.WAKEUP_MODE_KEYWORD_SPEAKER) {
            Toast.makeText(this, "Invalid voice wake-up mode:"
                    + mWakeupMode, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initUiComponent();
    }

    private void initUiComponent() {
        if (mWakeupMode == VoiceSettingWrapper.WAKEUP_MODE_KEYWORD) {
            setTitle(R.string.voice_wakeup_switch_1);
        } else if (mWakeupMode == VoiceSettingWrapper.WAKEUP_MODE_KEYWORD_SPEAKER) {
            setTitle(R.string.voice_wakeup_switch_2);
        }
        mSpnClean = (Spinner) findViewById(R.id.voice_settings_clean_spn);
        mSpnNoisy = (Spinner) findViewById(R.id.voice_settings_noisy_spn);
        mSpnTraining = (Spinner) findViewById(R.id.voice_settings_training_spn);
        mSpnTesting = (Spinner) findViewById(R.id.voice_settings_testing_spn);
        ArrayAdapter<String> adapter;
        adapter = createEmptySpinnerAdapter();
        for (int i = 0; i <= 15; i++) {
            adapter.add(String.valueOf(i));
        }
        mSpnClean.setAdapter(adapter);

        adapter = createEmptySpinnerAdapter();
        for (int i = 0; i <= 15; i++) {
            adapter.add(String.valueOf(i));
        }
        mSpnNoisy.setAdapter(adapter);

        adapter = createEmptySpinnerAdapter();
        for (int i = 0; i <= 7; i++) {
            adapter.add(String.valueOf(i));
        }
        mSpnTraining.setAdapter(adapter);

        adapter = createEmptySpinnerAdapter();
        for (int i = 0; i <= 7; i++) {
            adapter.add(String.valueOf(i));
        }
        mSpnTesting.setAdapter(adapter);

        mBtnSet = (Button) findViewById(R.id.voice_settings_set_btn);
        mBtnSet.setOnClickListener(this);

        initUiByData();
    }

    private void initUiByData() {
        int clean = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_CLEAN);
        int noisy = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_NOISY);
        int training = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_TRAINING);
        int testing = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_TESTING);
        mSpnClean.setSelection(clean);
        mSpnNoisy.setSelection(noisy);
        mSpnTraining.setSelection(training);
        mSpnTesting.setSelection(testing);
    }

    private ArrayAdapter<String> createEmptySpinnerAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void setRecognitionSetting(int wakeMode, int clean,
            int noisy, int training, int testing) {
        VoiceSettingWrapper.setWakeupRecognitionParam(wakeMode,
                VoiceSettingWrapper.RECOGNITION_CLEAN, clean);
        VoiceSettingWrapper.setWakeupRecognitionParam(wakeMode,
                VoiceSettingWrapper.RECOGNITION_NOISY, noisy);
        VoiceSettingWrapper.setWakeupRecognitionParam(wakeMode,
                VoiceSettingWrapper.RECOGNITION_TRAINING, training);
        VoiceSettingWrapper.setWakeupRecognitionParam(wakeMode,
                VoiceSettingWrapper.RECOGNITION_TESTING, testing);
    }

    private boolean checkSetResult() {
        int clean = mSpnClean.getSelectedItemPosition();
        int noisy = mSpnNoisy.getSelectedItemPosition();
        int training = mSpnTraining.getSelectedItemPosition();
        int testing = mSpnTesting.getSelectedItemPosition();
        int clean2 = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_CLEAN);
        int noisy2 = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_NOISY);
        int training2 = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_TRAINING);
        int testing2 = VoiceSettingWrapper.getWakeupRecognitionParam(mWakeupMode,
                VoiceSettingWrapper.RECOGNITION_TESTING);
        if (clean != clean2 || noisy != noisy2 ||
                training != training2 || testing != testing2) {
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == mBtnSet) {
            int clean = mSpnClean.getSelectedItemPosition();
            int noisy = mSpnNoisy.getSelectedItemPosition();
            int training = mSpnTraining.getSelectedItemPosition();
            int testing = mSpnTesting.getSelectedItemPosition();
            setRecognitionSetting(mWakeupMode, clean, noisy, training, testing);
            int msgid = -1;
            if (checkSetResult()) {
                msgid = R.string.voice_set_success_msg;
            } else {
                msgid = R.string.voice_set_fail_msg;
            }
            Toast.makeText(this, msgid, Toast.LENGTH_SHORT).show();
        }
    }
}
