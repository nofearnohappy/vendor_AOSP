package com.mediatek.rcs.pam;

interface IDeviceApiServiceCallback {
    void onServiceConnected();

    void onServiceDisconnected(in int reason);

    void onServiceRegistered();

    void onServiceUnregistered();

    void onNewPublicAccountChat(in String account, in String msgId);

    void onNewCCPublicAccoutChat(in String accountnumber, in String msgId);

    void onPublicAccoutChatHistory(in String publicaccount, in long id);

    void onFollowPublicAccount(in String account, in int errType, in String statusCode);

    void onUnfollowPublicAccount(in String account, in int errType, in String statusCode);

    void onGetInfo(in String account, in int errType, in String statusCode);

    void onSearch(in int errType, in String statusCode);

    void onGetFollowedPublicAccount(in int errType, in String statusCode);

    void onMenuConfigUpdated(in String account, in String configInfo, in int errType,
            in String statusCode);

    void onReportPublicAccount(in String account, in int errType, in String statusCode);
}