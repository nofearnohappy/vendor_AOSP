package com.mediatek.backuprestore.utils;

public class Constants {
    public static final String BACKUP = "backup";
    public static final String RESTORE = "restore";

    public static final String BACKUP_FILE_EXT = "zip";
    public static final String BACKUP_FOLDER_NAME = ".backup";
    public static final String BACKUP_XML = "backup.xml";
    public static final String SETTINGINFO = "setttings";

    public static final int TIME_SLEEP_WHEN_COMPOSE_ONE = 200;

    public static final String KEY_SAVED_DATA = "data";
    // public static final String SDCARD2 = "%/mnt/sdcard2/%";

    public static final String ANDROID = "Android ";
    public static final String DATE = "date";
    public static final String SIZE = "size";
    public static final String FILE = "file";
    public static final String FILENAME = "filename";
    public static final String ITEM_TEXT = "text";
    public static final String ITEM_NAME = "name";
    public static final String ITEM_RESULT = "result";
    public static final String ITEM_PACKAGENAME = "packageName";
    public static final String RESULT_KEY = "result";
    public static final String INTENT_SD_SWAP = "com.mediatek.SD_SWAP";
    public static final String ACTION_SD_EXIST = "SD_EXIST";
    public static final String SCAN_RESULT_KEY_PERSONAL_DATA = "personalData";
    public static final String SCAN_RESULT_KEY_OLD_DATA = "oldData";
    public static final String SCAN_RESULT_KEY_APP_DATA = "appData";
    public static final String WARING_DIALOG_MSG = "message";

    public static final String URI_CALENDAR_IMPORTER_EVENTS = "content://com.mediatek.calendarimporter/events";
    public static final String URI_MMS_SMS = "content://mms-sms/conversations/";
    public static final String URI_MMS = "content://mms/";
    public static final String URI_SMS = "content://sms";
    public static final String URI_NOTEBOOK = "content://com.mediatek.notebook.NotePad/notes";

    public static final int NUMBER_IMPORT_CONTACTS_ONE_SHOT = 1500;
    public static final int NUMBER_IMPORT_CONTACTS_EACH = 480;
    public static final int NUMBER_IMPORT_MMS_EACH = 10;
    public static final int NUMBER_IMPORT_SMS_EACH = 40;
    public static final int NUMBER_SEND_BROCAST_MUSIC_EACH = 10;

    public static final String MESSAGE_BOX_TYPE_INBOX = "1";
    public static final String MESSAGE_BOX_TYPE_SENT = "2";
    public static final String MESSAGE_BOX_TYPE_DRAFT = "3";
    public static final String MESSAGE_BOX_TYPE_OUTBOX = "4";

    public static final String PERSON_DATA = "PersonData";
    public static final String APP_DATA = "AppData";

    public static final int RESULT_PERSON_DATA = 100;
    public static final int RESULT_APP_DATA = 200;
    public static final int RESULT_RESTORE_APP = 300;

    public static final int REPLACE_DATA = 1;
    public static final int STARTFORGROUND = 1;

    public static final String ONLY_APP = "App";
    public static final String APP_AND_DATA = "App + Data";
    public static final String DATA_TITLE = "title";

    public static final String MESSAGE_CURRENT_PROGRESS = "1000";
    public static final String MESSAGE_CONTENT = "1001";
    public static final String MESSAGE_MAX_PROGRESS = "1002";
    public static final String MESSAGE_RESULT_RECORD = "1003";
    public static final String MESSAGE_RESULT_TYPE = "1004";
    public static final String MESSAGE_IS_UPDATA_MSG = "1005";
    public static final String LOGIN = "Login";

    public static final float DISABLE_ALPHA = 0.4f;
    public static final float ENABLE_ALPHA = 1.0f;

    public static final String NOSDCARD_CHANGE_NOTICE = "pre_nosdcard_change_notice";
    public static final String SDCARD_UNMOUNT_BACKUP = "pre_sdcard_unmount_backup";
    public static final String ARRAYDATA = "dataArray";
    public static final String NOTICE_BACKUP_PATH = "pre_notice_backup_path";
    public static final String NO_SDCARD_RESTORE_INFO = "pre_no_sdcard_restore_info";
    public static final String SDCARD_UNMOUNT_RESTORE_INFO = "pre_sdcard_unmount_restore_info";

    public static final String PHONE_STOTAGE = "pref_key_phone_storage";
    public static final String SDCARD_STOTAGE = "pref_key_sdcard_storage";
    public static final String CUSTOMIZE_STOTAGE = "pref_key_customize";

    public class ModulePath {
        public static final String FOLDER_BACKUP = "backup";
        public static final String FOLDER_APP = "App";
        public static final String FOLDER_DATA = "Data";
        public static final String FOLDER_CALENDAR = "Calendar";
        public static final String FOLDER_TEMP = "temp";
        public static final String FOLDER_CONTACT = "Contact";
        public static final String FOLDER_MMS = "Mms";
        public static final String FOLDER_SMS = "Sms";
        public static final String FOLDER_MUSIC = "Music";
        public static final String FOLDER_PICTURE = "Picture";
        public static final String FOLDER_NOTEBOOK = "Notebook";

        public static final String NAME_CALENDAR = "calendar.vcs";
        public static final String NAME_CONTACT = "contact.vcf";
        public static final String NAME_MMS = "mms";
        public static final String NAME_SMS = "sms";

        public static final String FILE_EXT_APP = ".apk";
        // public static final String FILE_EXT_CALENDAR = ".vcs";
        // public static final String FILE_EXT_CONTACT = ".vcf";
        public static final String FILE_EXT_PDU = ".pdu";

        public static final String ALL_APK_FILES = ".*\\.apk";
        public static final String SCHEMA_ALL_APK = "apps/.*\\.apk";
        public static final String SCHEMA_ALL_CALENDAR = "calendar/calendar[0-9]+\\.vcs";
        public static final String SCHEMA_ALL_CONTACT = "contacts/contact[0-9]+\\.vcf";
        // public static final String SCHEMA_ALL_MMS = "mms/[0-9]+\\.pdu";
        public static final String SCHEMA_ALL_SMS = "sms/sms[0-9]+";
        public static final String SCHEMA_ALL_MUSIC = "music/.*";
        public static final String SCHEMA_ALL_PICTURE = "picture/.*";

        public static final String PICTUREZIP = "picture.zip";
        public static final String MUSICZIP = "music.zip";

        public static final String SMS_VMSG = "sms.vmsg";
        public static final String MMS_XML = "mms_backup.xml";
        public static final String NOTEBOOK_XML = "notebook.xml";

    }

    public class DialogID {
        public static final int DLG_RESTORE_CONFIRM = 2000;
        public static final int DLG_SDCARD_REMOVED = 2001;
        public static final int DLG_WARNING_BACKUP = 2001;
        public static final int DLG_SDCARD_FULL = 2002;
        public static final int DLG_WARNING_RESTORE = 2002;
        public static final int DLG_RESULT = 2004;
        public static final int DLG_LOADING = 2005;
        public static final int DLG_DELETE_AND_WAIT = 2006;
        public static final int DLG_NO_SDCARD = 2007;
        public static final int DLG_CANCEL_CONFIRM = 2008;
        public static final int DLG_CONTACT_CONFIG = 2009;
        public static final int DLG_EDIT_FOLDER_NAME = 2010;
        public static final int DLG_CREATE_FOLDER_FAILED = 2011;
        public static final int DLG_BACKUP_CONFIRM_OVERWRITE = 2012;
        public static final int DLG_LOGIN_STATUES = 2014;
        public static final int DLG_BACKUP_PATH_SETTING = 2015;
        public static final int DLG_CHANGE_NOTICE = 2016;
        public static final int DLG_SDCARD_UNMOUNT_BACKUP = 2017;
        public static final int DLG_RESET_BACKUP_PATH = 2018;
        public static final int DLG_NOTICE_BACKUP_PATH = 2019;
        public static final int DLG_SCANN_INFO = 2020;
    }

    public class MessageID {
        public static final int PRESS_BACK = 0X501;
        public static final int SCANNER_FINISH = 0X502;
        public static final int BACKUP_ERROR = 0X503;
        public static final int SCANNER_ONE_ITEM = 0X510;
        // onComposerChanged
        public static final int COMPOSER_CHANGED = 0X504;
        // ProgressChanged
        public static final int PROGRESS_CHANGED = 0X505;
        // BackupEnd
        public static final int BACKUP_END = 0X506;

        // RestoreErr
        public static final int RESTORE_ERROR = 0x507;
        public static final int RESTORE_END = 0X508;
        public static final int APP_PROGRESS_CHANGED = 0X509;

    }

    public class State {
        public static final int INIT = 0X00;
        public static final int RUNNING = 0X01;
        public static final int PAUSE = 0X02;
        public static final int CANCEL_CONFIRM = 0X03;
        public static final int CANCELLING = 0X04;
        public static final int FINISH = 0X05;
        public static final int ERR_HAPPEN = 0X06;
    }

    public class LogTag {
        public static final String LOG_TAG = "BackRestoreLogTag";
        public static final String CONTACT_TAG = "contact";
        public static final String MESSAGE_TAG = "message";
        public static final String MUSIC_TAG = "music";
        public static final String NOTEBOOK_TAG = "notebook";
        public static final String PICTURE_TAG = "picture";
        public static final String SMS_TAG = "sms";
        public static final String MMS_TAG = "mms";
        public static final String BACKUP_ENGINE_TAG = "backupEngine";
    }

    public class ContactType {
        public static final String ALL = "all";
        public static final String PHONE = "phone";
        public static final String SIM1 = "sim1";
        public static final String SIM2 = "sim2";
        public static final int SIMID_PHONE = -1;
        public static final int SIMID_SIM1 = 0;
        public static final int SIMID_SIM2 = 1;
        public static final int SIMID_SIM3 = 2;
        public static final int SIMID_SIM4 = 3;
        public static final int DEFAULT = 100;
    }

    public class ErrorType {
        public static final int SDCARD_REMOVED = 0;
        public static final int SDCARD_FULL = 1;
        public static final int WARNING = 2;
    }
}
