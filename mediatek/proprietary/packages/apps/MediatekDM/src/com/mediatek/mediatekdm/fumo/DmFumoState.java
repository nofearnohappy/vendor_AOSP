package com.mediatek.mediatekdm.fumo;

public final class DmFumoState {
    public static final int IDLE = 0;
    public static final int QUERY_NEW_VERSION = 1;
    public static final int NEW_VERSION_FOUND = 2;
    // Server didn't trigger FUMO action or user chose not to download
    public static final int NO_NEW_VERSION_FOUND = 3;
    public static final int DOWNLOAD_STARTED = 4;
    public static final int DOWNLOADING = 5;
    public static final int DOWNLOAD_COMPLETE = 6;
    public static final int DOWNLOAD_CANCELED = 7;
    public static final int DOWNLOAD_FAILED = 8;
    public static final int DOWNLOAD_PAUSED = 9;
    public static final int UPDATE_COMPLETE = 10;
    public static final int UPDATE_FAILED = 11;

    // Volatile states
    public static final int WAP_MARK_START = 100;
    public static final int WAP_CONNECTING = 101; // DmClient specific
    public static final int WAP_CONNECT_SUCCESS = 102;
    public static final int WAP_CONNECT_TIMEOUT = 103;
    public static final int WAP_MARK_END = 199;

    public static boolean isWAPState(int state) {
        return (state > WAP_MARK_START && state < WAP_MARK_END);
    }
}
