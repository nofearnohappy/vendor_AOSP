package com.hesine.nmsg.business.bean.response;

import java.util.List;

public class SendMsg extends Base {

    private String serviceAccount;
    private List<String> messageIds;

    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

}
