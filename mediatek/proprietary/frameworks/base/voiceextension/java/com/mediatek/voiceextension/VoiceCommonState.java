package com.mediatek.voiceextension;

/**
 * @hide
 */
public class VoiceCommonState {

    // ==========================RetCode Common==========================//
    private static final int RET_COMMON_INDEX = 0;

    public static final int RET_COMMON_FAILURE = RET_COMMON_INDEX - 1;

    public static final int RET_COMMON_SUCCESS = RET_COMMON_INDEX + 1;

    public static final int RET_COMMON_STORAGE_WRITE_FAILED = RET_COMMON_INDEX + 2;

    public static final int RET_COMMON_MIC_INIT_FAILED = RET_COMMON_INDEX + 3;

    public static final int RET_COMMON_MIC_OCCUPIED = RET_COMMON_INDEX + 4;

    public static final int RET_COMMON_LISTENER_ILLEGAL = RET_COMMON_INDEX + 5;

    public static final int RET_COMMON_LISTENER_NEVER_SET = RET_COMMON_INDEX + 6;

    public static final int RET_COMMON_LISTENER_ALREADY_SET = RET_COMMON_INDEX + 7;

    public static final int RET_COMMON_RECOGNITION_NEVER_STARTED = RET_COMMON_INDEX + 8;

    public static final int RET_COMMON_RECOGNITION_NEVER_PAUSED = RET_COMMON_INDEX + 9;

    public static final int RET_COMMON_RECOGNITION_ALREADY_STARTED = RET_COMMON_INDEX + 10;

    public static final int RET_COMMON_RECOGNITION_ALREADY_PAUSED = RET_COMMON_INDEX + 11;

    public static final int RET_COMMON_SERVICE_NOT_EXIST = RET_COMMON_INDEX + 12;

    public static final int RET_COMMON_SERVICE_DISCONNECTTED = RET_COMMON_INDEX + 13;

    public static final int RET_COMMON_PROCESS_ILLEGAL = RET_COMMON_INDEX + 14;

    // ===========================RetCode Set==========================//
    private static final int RET_SET_INDEX = 200;

    public static final int RET_SET_ALREADY_EXIST = RET_SET_INDEX + 1;

    public static final int RET_SET_NOT_EXIST = RET_SET_INDEX + 2;

    public static final int RET_SET_ILLEGAL = RET_SET_INDEX + 3;

    public static final int RET_SET_EXCEED_LIMIT = RET_SET_INDEX + 4;

    public static final int RET_SET_SELECTED = RET_SET_INDEX + 5;

    public static final int RET_SET_NOT_SELECTED = RET_SET_INDEX + 6;

    public static final int RET_SET_OCCUPIED = RET_SET_INDEX + 7;

    // ===========================RetCode Command==========================//
    private static final int RET_COMMAND_INDEX = 300;

    public static final int RET_COMMAND_DATA_INVALID = RET_COMMAND_INDEX + 1;

    public static final int RET_COMMAND_FILE_ILLEGAL = RET_COMMAND_INDEX + 2;

    public static final int RET_COMMAND_NUM_EXCEED_LIMIT = RET_COMMAND_INDEX + 3;

    // ===========================RetCode Search==========================//

    // =========================RetCode Passphrase==========================//

    // ============================Feature Type=============================//
    public static final int FEATURE_TYPE_COMMAND = 1;
    public static final int FEATURE_TYPE_SEARCH = 2;
    public static final int FEATURE_TYPE_PASSPHRASE = 3;

    public static final String FEATURE_TYPE_COMMAND_NAME = "Command";
    public static final String FEATURE_TYPE_SEARCH_NAME = "Search";
    public static final String FEATURE_TYPE_PASSPHRASE_NAME = "Passphrase";

    // ===========================API Type Command========================//
    public static final int API_TYPE_COMMAND_IDLE = 1;
    public static final int API_TYPE_COMMAND_RECOGNITION_START = API_TYPE_COMMAND_IDLE + 1;
    public static final int API_TYPE_COMMAND_RECOGNITION_STOP = API_TYPE_COMMAND_IDLE + 2;
    public static final int API_TYPE_COMMAND_RECOGNITION_PAUSE = API_TYPE_COMMAND_IDLE + 3;
    public static final int API_TYPE_COMMAND_RECOGNITION_RESUME = API_TYPE_COMMAND_IDLE + 4;
    public static final int API_TYPE_COMMAND_RECOGNITION_RESULT = API_TYPE_COMMAND_IDLE + 5;
    public static final int API_TYPE_COMMAND_NOTIFY_ERROR = API_TYPE_COMMAND_IDLE + 6;
    public static final int API_TYPE_COMMAND_COMMANDS_SET = API_TYPE_COMMAND_IDLE + 7;

    // ============================API Type Search===========================//

    // ===========================API Type Passphrase=========================//

}
