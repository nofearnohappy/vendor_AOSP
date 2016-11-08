package com.mediatek.engineermode.voicesettings;

import android.media.AudioSystem;

import com.mediatek.engineermode.Elog;

public class VoiceSettingWrapper {
    private static final String TAG = "VoiceSettingWrapper";
    public static final int RECOGNITION_CLEAN = 1;
    public static final int RECOGNITION_NOISY = 2;
    public static final int RECOGNITION_TRAINING = 3;
    public static final int RECOGNITION_TESTING = 4;
    public static final int WAKEUP_MODE_KEYWORD = 1;
    public static final int WAKEUP_MODE_KEYWORD_SPEAKER = 2;
    private static final String DETECTOR_KEY_PREFIX = "VOWParameters=";
    private static final String KEYWORD_RECOGNITION_PREFIX = "VOWKRParameters=";
    private static final String KEYWORD_SPEAKER_RECOGNITION_PREFIX = "VOWKRSRParameters=";

    public static void setWakeupDetectorParam(int index, int value) {
        Elog.d(TAG, "setWakeupDetectorParam");
        if (index < 1 || index > 10) {
            Elog.d(TAG, "Invalid index:" + index);
            return;
        }
        if (value < 0 || value > 15) {
            Elog.d(TAG, "invalid value:" + value);
            return;
        }
        String targetVal = index + "," + value;
        String targetParam = DETECTOR_KEY_PREFIX + targetVal;
        Elog.d(TAG, "targetParam:" + targetParam);
        AudioSystem.setParameters(targetParam);
    }

    public static int getWakeupDetectorParam(int index) {
        Elog.d(TAG, "getWakeupDetectorParam");
        if (index < 1 || index > 10) {
            Elog.d(TAG, "Invalid index:" + index);
            return -1;
        }
        String key = DETECTOR_KEY_PREFIX + index;
        Elog.d(TAG, "key:" + key);
        String result = AudioSystem.getParameters(key);
        return resolveIntValue(result);
    }

    public static void setWakeupRecognitionParam(int wakeupMode, int type, int value) {
        Elog.d(TAG, "setWakeupRecognitionParam");
        if (type < 1 || type > 4) {
            Elog.d(TAG, "Invalid Recognition type:" + type);
            return;
        }
        if (wakeupMode != WAKEUP_MODE_KEYWORD &&
                wakeupMode != WAKEUP_MODE_KEYWORD_SPEAKER) {
            Elog.d(TAG, "Invalid wake-up mode:" + wakeupMode);
            return;
        }
        String targetVal = type + "," + value;
        String targetParam = null;
        if (wakeupMode == WAKEUP_MODE_KEYWORD) {
            targetParam = KEYWORD_RECOGNITION_PREFIX + targetVal;
        } else if (wakeupMode == WAKEUP_MODE_KEYWORD_SPEAKER) {
            targetParam = KEYWORD_SPEAKER_RECOGNITION_PREFIX + targetVal;
        }
        Elog.d(TAG, "targetParam:" + targetParam);
        AudioSystem.setParameters(targetParam);
    }

    public static int getWakeupRecognitionParam(int wakeupMode, int type) {
        String result;
        Elog.d(TAG, "getWakeupRecognitionParam");
        if (type < 1 || type > 4) {
            Elog.d(TAG, "Invalid Recognition type:" + type);
            return -1;
        }
        if (wakeupMode != WAKEUP_MODE_KEYWORD &&
                wakeupMode != WAKEUP_MODE_KEYWORD_SPEAKER) {
            Elog.d(TAG, "Invalid wake-up mode:" + wakeupMode);
            return -1;
        }
        String key = null;
        if (wakeupMode == WAKEUP_MODE_KEYWORD) {
            key = KEYWORD_RECOGNITION_PREFIX + type;
        } else if (wakeupMode == WAKEUP_MODE_KEYWORD_SPEAKER) {
            key = KEYWORD_SPEAKER_RECOGNITION_PREFIX + type;
        }
        Elog.d(TAG, "key:" + key);
        result = AudioSystem.getParameters(key);
        return resolveIntValue(result);
    }

    private static int resolveIntValue(String str) {
        int target = -1;
        if (str == null) {
            Elog.d(TAG, "resolve fail; input string is null");
            return -1;
        }
        String[] pairs = str.split(";");
        String[] keyVal = pairs[0].split("=");
        if (keyVal.length < 2) {
            Elog.d(TAG, "resolve fail; invalid input string:" + pairs[0]);
            return -1;
        }
        String val = keyVal[1].trim();
        try {
            target = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            Elog.e(TAG, "resolve fail; NumberFormatException:" + e.getMessage());
        }
        return target;
    }
}
