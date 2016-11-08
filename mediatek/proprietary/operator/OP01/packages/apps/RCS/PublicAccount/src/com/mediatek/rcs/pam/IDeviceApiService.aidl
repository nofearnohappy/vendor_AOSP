package com.mediatek.rcs.pam;

import com.mediatek.rcs.pam.IDeviceApiServiceCallback;

interface IDeviceApiService {

    long addDeviceApiCallback(in IDeviceApiServiceCallback listener);

    void removeDeviceApiCallback(in long token);

    boolean isServiceConnected(in long token);
    /**
     * Synchronous API. 6.7.3.5
     * @param account UUID of the account
     * @param message Body of message
     * @return
     */
    String sendMessage(in long token, in String accountnumber,in String message);

    /**
     * 6.7.3.6
     *
     */
    boolean deleteMessage(in long token, in String msgId);

    /**
     * 6.7.3.7
     *
     */
    String sendMenuMessage(in long token, in String accountnumber, in String menuID);

    /**
     * 6.7.3.8
     */
    boolean setMessageRead(in long token, in String msgId);

    /**
     * 6.7.3.9
     * @param account
     */
    void getPublicAccountInfo(in long token, in String account);

    /**
     * 6.7.3.10
     *
     */
    boolean getPublicAccountHistory(in long token, in String accountnumber,
            in String timestamp, in int order, in int pageno, in int pagesize, in long id);

    /**
     * 6.7.3.11
     * @param account
     */
    boolean getFollowedPublicAccount(in long token, in int pageno,
            in int order, in int pagesize);

    /**
     * 6.7.12
     * @param accountnumber)
     */
    void followPublicAccount(in long token, in String accountnumber);

    /**
     * 6.7.3.13
     */
    void searchPublicAccount(in long token, in String keyword,
            in int pageNum, in int order, in int pageSize);

    /**
     * 6.7.3.14
     * @param account
     */
    void unfollowPublicAccount(in long token, in String accountnumber);

    /**
     * 6.7.3.15
     * @param accountnumber
     */
    boolean getPublicAccountStatus(in long token, in String accountnumber);

    /**
     * 6.7.3.16
     * @param account
     * @param reason
     * @param description
     * @param type
     * @param data
     */
    void reportPublicAccount(in long token, in String account,in String reason,
            in String description, in int type, in String data);

    /**
     * 6.7.3.17
     * @param account
     */
    void updateMenuConfig(in long token, in String accountnumber);
}