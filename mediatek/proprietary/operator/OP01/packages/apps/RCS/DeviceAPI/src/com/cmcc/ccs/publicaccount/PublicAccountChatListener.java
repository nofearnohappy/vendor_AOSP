package com.cmcc.ccs.publicaccount;

public interface PublicAccountChatListener {
    void onNewPublicAccountChat(String account, String msgId);

    void onNewCCPublicAccoutChat(String accountnumber, String msgId);

    void onPublicAccoutChatHistory(String publicaccount, long id);

    void onFollowPublicAccount(String account, int errType, String statusCode);

    void onUnfollowPublicAccount(String account, int errType, String statusCode);

    void onGetInfo(String account, int errType, String statusCode);

    void onSearch(int errType, String statusCode);

    void onGetFollowedPublicAccount(Integer errType, String statusCode);

    void onMenuConfigUpdated(String account, String configInfo, int errType, String statusCode);

    void onReportPublicAccount(String account, int errType, String statusCode);
}
