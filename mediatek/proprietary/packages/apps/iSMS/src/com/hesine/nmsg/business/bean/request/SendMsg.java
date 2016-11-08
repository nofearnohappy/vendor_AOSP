package com.hesine.nmsg.business.bean.request;

import com.hesine.nmsg.business.bean.MessageInfo;

public class SendMsg extends Base {

    private MessageInfo messageInfo;

    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo) {
        this.messageInfo = messageInfo;
    }
}
