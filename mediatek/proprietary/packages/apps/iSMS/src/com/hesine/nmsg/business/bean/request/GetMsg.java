package com.hesine.nmsg.business.bean.request;

import java.util.List;

public class GetMsg extends Base {

    private List<String> messageIds = null;

    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }

}
