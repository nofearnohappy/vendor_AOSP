package com.cmcc.ccs.publicaccount;

import android.net.Uri;

public class PublicAccount {
    public static final String CONTENT_URI_STRING = "content://com.cmcc.ccs.public_account";
    public static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);

    public static final String CHAT_ID = "PUBLICACCOUNT_CHAT_ID";
    public static final String ACCOUNT = "PUBLICACCOUNT_ACCOUNT";
    public static final String NAME = "PUBLICACCOUNT_NAME";
    public static final String PORTRAIT = "PUBLICACCOUNT_PORTRAIT";
    public static final String PORTRAIT_TYPE = "PUBLICACCOUNT_PORTRAIT_TYPE";
    public static final String STATE = "PUBLICACCOUNT_STATE";
    public static final String BRIEF_INTRODUCTION = "PUBLICACCOUNT_BRIEF_INTRODUCTION";
    public static final String CONFIG = "PUBLICACCOUNT_CONFIG";

    public static final String JPEG = "JPEG";
    public static final String BMP = "BMP";
    public static final String PNG = "PNG";
    public static final String GIF = "GIF";
}
