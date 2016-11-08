package com.mediatek.rcs.pam.model;

public class GeneralInfo {
    public String messageName;
    public String version = "1.0.0";
    public String userId; // request only
    public int result; // response only

    public static GeneralInfo buildRequestInfo(String messageName, String userId) {
        GeneralInfo info = new GeneralInfo();
        info.messageName = messageName;
        info.userId = userId;
        return info;
    }

    public static GeneralInfo buildResponseInfo(String messageName, int result) {
        GeneralInfo info = new GeneralInfo();
        info.messageName = messageName;
        info.result = result;
        return info;
    }
}
