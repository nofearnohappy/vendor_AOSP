package com.mediatek.rcs.common.provider;

import android.net.Uri;

public class GroupMemberData {

    public static final Uri CONTENT_URI = Uri.parse("content://rcs-groupmember");

    public static final String AUTHORITY = "rcs-groupmember";

    public static final String COLUMN_ID                   = "_id";
    public static final String COLUMN_CHAT_ID              = "GROUPCHATSERVICE_CHAT_ID";
    public static final String COLUMN_CONTACT_NUMBER       = "GROUPCHATSERVICE_PHONE_NUMBER";
    public static final String COLUMN_CONTACT_NAME         = "GROUPCHATSERVICE_MEMBER_NAME";
    public static final String COLUMN_TYPE                 = "type";
    public static final String COLUMN_PORTRAIT             = "GROUPCHATSERVICE_PORTRAIT";
    public static final String COLUMN_STATE                = "state";
    public static final String COLUMN_PORTRAIT_NAME        = "GROUPCHATSERVICE_PORTRAIT_NAME";

    public static final class STATE {
        /**
         *
         */
        public static final int STATE_PENDING         = 0;

        /**
         *
         */
        public static final int STATE_CONNECTED       = 1;

        /**
         *
         */
        public static final int STATE_DISCONNECTED    = 2;
    }
}
