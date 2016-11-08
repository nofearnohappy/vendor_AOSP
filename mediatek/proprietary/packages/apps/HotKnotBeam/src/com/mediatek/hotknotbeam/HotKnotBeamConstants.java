package com.mediatek.hotknotbeam;


public class HotKnotBeamConstants {

    public enum State {
        CONNECTING, RUNNING, COMPLETE
    }

    public enum FailureReason {
        NONE, LOW_STORAGE, CONNECTION_ISSUE, USER_CANCEL_RX, USER_CANCEL_TX, UNKNOWN_ERROR
    }

    public enum HotKnotFileType {
        COMMON, IMAGE, VIDEO, MUSIC, CONTACT, EMAIL, RAW
    }

    //Transfer File attribute
    static protected final String   QUERY_NUM = "num";
    static protected final String   QUERY_ORDER         = "order";
    //Specify how to save the file. No change file name or rename auto.
    static protected final String   QUERY_FORMAT = "format";
    //Specify where to seave.
    static protected final String   QUERY_FOLDER = "folder";
    static protected final String   QUERY_SHOW   = "show";      //Specify whether to display notificaiton
    static protected final String   QUERY_ZIP           = "zip";      //Specify whether to zip
    static protected final String   QUERY_MIMETYPE      = "mimetype";      //Specify which file mimetype
    static protected final String   QUERY_GROUPID       = "groupid";      //For group transfer usage
    static protected final String   QUERY_INTENT        = "intent";      //Specify which intent to launch
    static protected final String   QUERY_IS_MIMETYPE   = "isMimeType";      //Specify whether to decide mimetype of file by extension name
    static protected final String   QUERY_TOTAL_SIZE    = "totalsize";          //Specify

    static protected final String   QUERY_VALUE_YES   = "yes";      //
    static protected final String   QUERY_VALUE_NO   = "no";      //

    static protected final int      MAX_FIRST_WAIT_COUNTER = -120;
    static protected final int      MAX_IDLE_COUNTER = 10;

    static protected final int      NON_GROUP_ID       = -1;
    static protected final int      MAX_GROUP_MAX_ID   = 1000000;
    static protected final int      MAX_GROUP_MIN_ID   = 10000;
    static protected final int      MAX_MAX_ID         = 9999;
    static protected final int      MAX_MIN_ID         = 0;

    //File transfer
    static protected final long     MAX_FILE_UPLOAD_SIZE = 2 * 1024 * 1024 * 1024L;  //Only support 2G bytes
    static protected final long     MAX_FILE_DISPLAY_SIZE = 1 * 1024 * 1024; // 2 Mbytes
    static protected final String   MAX_HOTKNOT_BEAM_FOLDER = "HotKnot";
    static protected final String   MAX_HOTKNOT_BEAM_TEMP_ZIP = "tmp.zip";
    static protected final long     MAX_DISPLAY_OEPN__SIZE = 2 * 1024 * 1024;

    //Support phone contact transfer.
    static protected final String   MIME_TYPE_VCARD = "text/vcard";
    static protected final String   CONTACT_FILE_NAME = "_contact_";
    static protected final String   CONTACT_INTENT =
                                    "com.mediatek.hotknot.action.VCARD_DISCOVERD";

    //Socket layer
    // The default timer value is 60 seconds.
    static protected final int      MAX_TIMEOUT_VALUE = 60 * 1000;
    static protected final int      MAX_RETRY_COUNT = 3;
    static protected final int      RETRY_SLEEP_TIMER = 5 * 1000;
    static protected final int      SERVICE_PORT = 19273;

    //Notficiation progress
    static protected final int      FILE_PROGRESS_POLL = 1 * 1000;
    static protected final int      FILE_UI_PROGRESS_POLL = 500;
    static protected final int      MAX_KB_SIZE = 1024;
    static protected final int      MAX_MB_SIZE = 1024 * 1024;
    static protected final String   MAX_MB_FORMAT = "%1dMB / %2dMB";
    static protected final String   MAX_KB_FORMAT = "%1dKB / %2dKB";
    static protected final String   MAX_FORMAT = "%1dB / %2dB";
    static protected final long     MAX_NOTIFY_TIMEOUT_VALUE = 90 * 1000L;
                                    //The default timer value is 90 seconds

    static protected final String   BEAM_FINISH_COMMAND = "__Finish__";

    //UI Receiver activity
    static protected final int      START_UI_DELAY = 1 * 1000;

    //Test configuration
    static protected final String   TEST_FILE_URI = "file://mnt/sdcard/testimg.jpg";

    public static final String STORAGE_AUTHORITY = "com.mediatek.hotknotbeam.documents";
    public static final String STORAGE_ROOT_ID = "HotKnot";

    static protected final String  DEFAULT_DEVICE_NAME = "HotKnot";

}
