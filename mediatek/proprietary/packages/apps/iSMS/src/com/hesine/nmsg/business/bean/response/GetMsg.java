package com.hesine.nmsg.business.bean.response;

import java.util.List;

import com.hesine.nmsg.business.bean.MessageInfo;

public class GetMsg extends Base {

    private long serviceAccountTS;
    private List<MessageInfo> messages;
    private String version;

    public List<MessageInfo> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageInfo> messages) {
        this.messages = messages;
    }

    public long getServiceAccountTS() {
        return serviceAccountTS;
    }

    public void setServiceAccountTS(long serviceAccountTS) {
        this.serviceAccountTS = serviceAccountTS;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
