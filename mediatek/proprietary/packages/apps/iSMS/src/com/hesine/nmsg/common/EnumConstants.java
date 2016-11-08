package com.hesine.nmsg.common;

import android.annotation.SuppressLint;
import java.text.SimpleDateFormat;

public class EnumConstants {

    /**
     * message type.
     */
    public static final byte SINGLE_CHAT = 0;
    public static final byte ADMIN_MSG = 2;
    public static final String ROOT_DIR = "nmsg";

    @SuppressLint("SdCardPath")
    public static final String STORAGE_PATH = "/data/data/com.hesine.nmsg";
    public static final String NMSG_INTENT_ACTION = "com.hesine.nmsg.broadcast";
    public static final String NMSG_INTENT_EXTRA_ACCOUNT = "com.hesine.nmsg.account";
    public static final String NMSG_INTENT_EXTRA_THREADID = "com.hesine.nmsg.threadid";
    public static final SimpleDateFormat SDF3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat SDF2 = new SimpleDateFormat("MM-dd HH:mm:ss");
    public static final SimpleDateFormat SDF1 = new SimpleDateFormat("MM-dd_HH-mm-ss");

    public static final long ONE_DAY = (1 * 24 * 60 * 60 * 1000);
    public static final int CHANNEL_ID = 000;
    public static final boolean NETWORK_SETTING_SWITCH = false;

    /*
     * Official:app.nmsg.hesine.com 
     * verify:app-verify.nmsg.hesine.com
     * test:app-test.nmsg.hesine.com
     */
    public static final String BASE_URL = "http://app.nmsg.hesine.com:8080";
    public static final String SHARED_PREFERENCE_NAME = "hesine_nmsg";
    public static final int SAVE_IMG_SUCCESS = 1;
    public static final int SAVE_IMG_FOR_SHOW_SUCCESS = 2;
    public static final String DOMAIN_NAME = "nmsg.hesine.com";
    public static final int MAX_RETRY_TIME = 3;
    public static final long TWENTY_MINUTES = (20 * 60 * 1000);
}
