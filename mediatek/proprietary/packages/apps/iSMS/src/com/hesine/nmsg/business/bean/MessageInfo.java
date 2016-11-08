package com.hesine.nmsg.business.bean;

import java.util.List;

public class MessageInfo {

    private int id = 0;//msg_id
    private String msgUuid = null;//msg_uuid
    private int type = 0;
    private String from = null;
    private String to = null;
    private String sms = null;
    private Long time = (long) 0;
    private List<MessageItemInfo> messageItems = null;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsgUuid() {
        return msgUuid;
    }

    public void setMsgUuid(String msgUuid) {
        this.msgUuid = msgUuid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSms() {
        return sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public List<MessageItemInfo> getMessageItems() {
        return messageItems;
    }

    public void setMessageItems(List<MessageItemInfo> messageItems) {
        this.messageItems = messageItems;
    }

}
