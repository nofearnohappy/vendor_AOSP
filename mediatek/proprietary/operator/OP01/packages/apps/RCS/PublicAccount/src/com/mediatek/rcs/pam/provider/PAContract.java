package com.mediatek.rcs.pam.provider;

import android.net.Uri;

public interface PAContract {

    public interface AccountColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + ACCOUNTS;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);
        String TABLE = PAProvider.Tables.ACCOUNT;

        // Basics
        String ID = "_id";
        String UUID = "uuid";
        String NAME = "name";
        String ID_TYPE = "id_type";
        String INTRODUCTION = "introduction";
        String RECOMMEND_LEVEL = "recommend_level";
        String LOGO_ID = "logo_id";
        String SUBSCRIPTION_STATUS = "subscription_status";
        // Details
        String COMPANY = "company";
        String TYPE = "account_type";
        String UPDATE_TIME = "update_time";
        String MENU_TYPE = "menu_type";
        String MENU_TIMESTAMP = "menu_timestamp";
        String ACCEPT_STATUS = "accept_status";
        String ACTIVE_STATUS = "active_status";
        String TELEPHONE = "telephone";
        String EMAIL = "email";
        String ZIPCODE = "zipcode";
        String ADDRESS = "address";
        String FIELD = "field";
        String QRCODE_URL = "qrcode_url";
        // Menu in XML format
        String MENU = "menu";
        // Link to the last message
        String LAST_MESSAGE = "last_message";
        // Joined from media table
        String LOGO_PATH = "logo_path";
        String LOGO_URL = "logo_url";
    }

    public interface MessageColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + MESSAGES;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "_id";
        String UUID = "uuid";
        String SOURCE_ID = "source_id";
        String SOURCE_TABLE = "source_table";
        String CHAT_ID = "chat_id";
        String ACCOUNT_ID = "account_id";
        String ACCOUNT_UUID = "account_uuid";
        String TYPE = "type";
        String MIME_TYPE = "mime_type";
        String CHAT_TYPE = "chat_type";
        String TIMESTAMP = "timestamp";
        String CREATE_TIME = "create_time";
        String SMS_DIGEST = "sms_digest";
        String BODY = "body";
        String TEXT = "text";
        String DIRECTION = "direction";
        String FORWARDABLE = "forwardable";
        String STATUS = "status";
        String DATA1 = "data1";
        String DATA2 = "data2";
        String DATA3 = "data3";
        String DATA4 = "data4";
        String DATA5 = "data5";
        String SYSTEM = "system";
        String DELETED = "deleted";
    }

    String[] MESSAGE_DATA_COLUMN_LIST = new String[] {
            MessageColumns.DATA1,
            MessageColumns.DATA2,
            MessageColumns.DATA3,
            MessageColumns.DATA4,
            MessageColumns.DATA5
    };

    public interface MessageHistorySummaryColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + MESSAGE_HISTORY_SUMMARIES;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = AccountColumns.ID;
        String UUID = AccountColumns.UUID;
        String NAME = AccountColumns.NAME;
        String LOGO_ID = AccountColumns.LOGO_ID;
        String LOGO_PATH = AccountColumns.LOGO_PATH;
        String LOGO_URL = AccountColumns.LOGO_URL;
        String LAST_MESSAGE_ID = AccountColumns.LAST_MESSAGE;
        String LAST_MESSAGE_SUMMARY = "last_message_summary";
        String LAST_MESSAGE_TYPE = MessageColumns.TYPE;
        String LAST_MESSAGE_TIMESTAMP = MessageColumns.TIMESTAMP;
    }

    public interface MediaColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + MEDIAS;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "_id";
        String TYPE = "type";
        String TIMESTAMP = "timestamp";
        String PATH = "path";
        String URL = "url";
        String REF_COUNT = "ref_count";
    }

    public interface StateColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + STATE;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "_id";
        String INITIALIZED = "initialized";
    }

    public interface MediaBasicColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + MEDIA_BASICS;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "_id";
        String TITLE = "title";
        String FILE_SIZE = "file_size";
        String DURATION = "duration";
        String FILE_TYPE = "file_type";
        String ACCOUNT_ID = "account_id";
        String CREATE_TIME = "create_time";
        String MEDIA_UUID = "media_uuid";
        String THUMBNAIL_ID = "thumbnail_id";
        String ORIGINAL_ID = "original_id";
        String DESCRIPTION = "description";
    }

    public interface MediaArticleColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + MEDIA_ARTICLES;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);
        String ID = "_id";
        String TITLE = "title";
        String AUTHOR = "author";
        String THUMBNAIL_ID = "thumbnail_id";
        String ORIGINAL_ID = "original_id";
        String SOURCE_URL = "source_url";
        String BODY_URL = "body_url";
        String TEXT = "text";
        String FILE_TYPE = "file_type";
        String MEDIA_UUID = "media_uuid";
    }

    // TODO join media table for logo url
    public interface SearchColumns {
        String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + SEARCH;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "_id";
        String ROWID = "rowid";
        String MESSAGE_ID = ROWID; // use message id as rowid
        String ACCOUNT_ID = "account_id";
        String ACCOUNT_NAME = "account_name";
        String ACCOUNT_LOGO_ID = "account_logo_id";
        String ACCOUNT_LOGO_PATH = "account_logo_path";
        String ACCOUNT_LOGO_URL = "account_logo_url";
        // This timestamp is stored as string for searching
        String MESSAGE_TIMESTAMP = "message_timestamp";
        String MESSAGE_TYPE = "message_type";
        String MESSAGE_TEXT = "message_text";
        String MESSAGE_SUMMARY = "message_summary"; // data from message.sms_digest
        String MESSAGE_MEDIA_TITLE1 = "message_media_title1";
        String MESSAGE_MEDIA_TITLE2 = "message_media_title2";
        String MESSAGE_MEDIA_TITLE3 = "message_media_title3";
        String MESSAGE_MEDIA_TITLE4 = "message_media_title4";
        String MESSAGE_MEDIA_TITLE5 = "message_media_title5";
        String MESSAGE_ARTICLE_TEXT1 = "message_article_text1";
        String MESSAGE_ARTICLE_TEXT2 = "message_article_text2";
        String MESSAGE_ARTICLE_TEXT3 = "message_article_text3";
        String MESSAGE_ARTICLE_TEXT4 = "message_article_text4";
        String MESSAGE_ARTICLE_TEXT5 = "message_article_text5";
    }

    String AUTHORITY = "com.mediatek.publicaccounts";

    String ACCOUNTS = "accounts";
    String MESSAGES = "messages";
    String MESSAGES_PARAM_INCLUDING_SYSTEM = "including_system";
    String MESSAGES_PARAM_INCLUDING_DELETED = "including_deleted";
    String MEDIA_BASICS = "media_basics";
    String MEDIA_ARTICLES = "media_articles";
    String MEDIAS = "medias";
    String STATE = "state";
    String MESSAGE_HISTORY_SUMMARIES = "message_history_summaries";
    String SEARCH = "search";
    String SEARCH_PARAM_KEYWORD = "keyword";

    /* -------------- CMCC only -------------- */
    public interface CcsMessageColumns {
        String CONTENT_URI_STRING = "content://" + CCS_ACCOUNT_MESSAGE_AUTHORITY;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "_id";
        String MESSAGE_ID = "PUBLICACCOUNT_MESSAGE_ID";
        String ACCOUNT = "PUBLICACCOUNT_ACCOUNT";
        String BODY = "PUBLICACCOUNT_BODY";
        String TIMESTAMP = "PUBLICACCOUNT_TIMESTAMP";
        String MIME_TYPE = "PUBLICACCOUNT_MIME_TYPE";
        String MESSAGE_STATUS = "PUBLICACCOUNT_MESSAGE_STATUS";
        String DIRECTION = "PUBLICACCOUNT_DIRECTION";
        String TYPE = "PUBLICACCOUNT_TYPE";
    }

    public interface CcsHistoryColumns {
        String CONTENT_URI_STRING = "content://" + CCS_ACCOUNT_HISTORY_AUTHORITY;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String MESSAGE_ID = "PUBLICACCOUNT_MESSAGE_ID";
        String ACCOUNT = "PUBLICACCOUNT_ACCOUNT";
        String BODY = "PUBLICACCOUNT_BODY";
        String TIMESTAMP = "PUBLICACCOUNT_TIMESTAMP";
        String MIME_TYPE = "PUBLICACCOUNT_MIME_TYPE";
        String MESSAGE_STATUS = "PUBLICACCOUNT_MESSAGE_STATUS";
        String DIRECTION = "PUBLICACCOUNT_DIRECTION";
        String TYPE = "PUBLICACCOUNT_TYPE";
        String ID = "PUBLICACCOUNT_ID";
    }

    public interface CcsAccountColumns {
        String CONTENT_URI_STRING = "content://" + CCS_ACCOUNT_AUTHORITY;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "ID";
        String ACCOUNT = "PUBLICACCOUNT_ACCOUNT";
        String NAME = "PUBLICACCOUNT_NAME";
        String PORTRAIT = "PUBLICACCOUNT_PORTRAIT";
        String PORTRAIT_PATH = "PUBLICACCOUNT_PORTRAIT_PATH";
        String PORTRAIT_TYPE = "PUBLICACCOUNT_PORTRAIT_TYPE";
        String BREIF_INTRODUCTION = "PUBLICACCOUNT_BREIF_INTRODUCTION";
    }

    public interface CcsAccountInfoColumns {
        String CONTENT_URI_STRING = "content://" + CCS_ACCOUNT_INFO_AUTHORITY;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ID = "ID";
        String ACCOUNT = "PUBLICACCOUNT_ACCOUNT";
        String NAME = "PUBLICACCOUNT_NAME";
        String PORTRAIT = "PUBLICACCOUNT_PORTRAIT";
        String PORTRAIT_PATH = "PUBLICACCOUNT_PORTRAIT_PATH";
        String PORTRAIT_TYPE = "PUBLICACCOUNT_PORTRAIT_TYPE";
        String BREIF_INTRODUCTION = "PUBLICACCOUNT_BREIF_INTRODUCTION";
        String STATE = "PUBLICACCOUNT_STATE";
        String CONFIG = "PUBLICACCOUNT_CONFIG";
    }

    public interface CcsSearchColumns {
        String CONTENT_URI_STRING = "content://" + CCS_ACCOUNT_SEARCH_AUTHORITY;
        Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

        String ACCOUNT = "PUBLICACCOUNT_ACCOUNT";
        String NAME = "PUBLICACCOUNT_NAME";
        String PORTRAIT = "PUBLICACCOUNT_PORTRAIT";
        String PORTRAIT_TYPE = "PUBLICACCOUNT_PORTRAIT_TYPE";
        String BREIF_INTRODUCTION = "PUBLICACCOUNT_BREIF_INTRODUCTION";
        String ID = "PUBLICACCOUNT_ID";
    }

    String CCS_ACCOUNT_MESSAGE_AUTHORITY = "com.cmcc.ccs.public_account_message";
    String CCS_ACCOUNT_HISTORY_AUTHORITY = "com.cmcc.ccs.public_account_history_message";
    String CCS_ACCOUNT_AUTHORITY = "com.cmcc.ccs.public_account";
    String CCS_ACCOUNT_INFO_AUTHORITY = "com.cmcc.ccs.public_account_info";
    String CCS_ACCOUNT_SEARCH_AUTHORITY = "com.cmcc.ccs.public_account_search";

    //String CCS_ACCOUNTS = "public_account";
    //String CCS_ACCOUNT_INFOES = "public_account_info";
    //String CCS_SEARCH_RESULTS = "public_account_search";
}
