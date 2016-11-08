package com.hesine.nmsg.business.bean;

public class LocalMessageInfo extends MessageInfo {

    public static final int STATUS_TO_DRAFT = 0;
    public static final int STATUS_TO_OUTBOX = 1;
    public static final int STATUS_TO_FAILED = 2;
    public static final int STATUS_TO_SENT = 3;
    public static final int STATUS_FROM_UNREAD = 4;
    public static final int STATUS_FROM_READ = 5;

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_VIDEO = 3;
    public static final int TYPE_SINGLE = 4;
    public static final int TYPE_MULTIPOLE = 5;
    public static final int TYPE_LOCATION = 6;
    public static final int TYPE_SINGLE_LINK = 7;
    public static final int TYPE_MULTI_LINK = 8;

    private int status = 0;

    public int getStatus() {
        return status;
    }

    public void setStatus(int msgStatus) {
        this.status = msgStatus;
    }
}
