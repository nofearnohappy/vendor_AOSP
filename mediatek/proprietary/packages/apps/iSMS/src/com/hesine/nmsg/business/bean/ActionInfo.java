package com.hesine.nmsg.business.bean;

public class ActionInfo {

    public static final int ACTION_ID_ACTIVATE = 101;
    public static final int ACTION_ID_SEND_PNTOKEN = 102;
    public static final int ACTION_ID_GET_MSG = 103;
    public static final int ACTION_ID_SEND_MSG = 104;
    public static final int ACTION_ID_GET_DATA = 105;
    public static final int ACTION_ID_SEND_DATA = 106;
    public static final int ACTION_ID_UPGRADE = 107;
    public static final int ACTION_ID_DEACTIVATE = 108;
    private Integer actionid;
    private String userAccount = null;
    private String serviceAccount = null;

    public Integer getActionid() {
        if (actionid == null) {
            return 0;
        }
        return actionid;
    }

    public void setActionid(Integer actionid) {
        this.actionid = actionid;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

}
