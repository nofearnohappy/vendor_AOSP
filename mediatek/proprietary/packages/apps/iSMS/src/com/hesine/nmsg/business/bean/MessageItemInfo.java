package com.hesine.nmsg.business.bean;

public class MessageItemInfo {

    private Integer id = 0;
    private Integer msgId = 0;
    private String subject = null;
    private String desc = null;
    private String shortLink = null;
    private String longLink = null;
    private String body = null;
    private AttachInfo attachInfo = null;
    private String uuid = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getMsgId() {
        return msgId;
    }

    public void setMsgId(Integer id) {
        this.msgId = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getShortLink() {
        return shortLink;
    }

    public void setShortLink(String shortLink) {
        this.shortLink = shortLink;
    }

    public String getLongLink() {
        return longLink;
    }

    public void setLongLink(String longLink) {
        this.longLink = longLink;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public AttachInfo getAttachInfo() {
        return attachInfo;
    }

    public void setAttachInfo(AttachInfo attachInfo) {
        this.attachInfo = attachInfo;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
