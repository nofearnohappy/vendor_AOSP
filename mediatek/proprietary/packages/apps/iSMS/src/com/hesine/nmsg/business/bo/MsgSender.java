package com.hesine.nmsg.business.bo;

import java.util.ArrayList;
import java.util.List;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.MessageInfo;

public class MsgSender implements Pipe {

    private List<SendMsg> mSendQueue = new ArrayList<SendMsg>();
    private MessageInfo mMessageInfo = null;
    private Pipe listener = null;

    public void setListener(Pipe listener) {
        this.listener = listener;
    }

    public void setMessageInfo(MessageInfo msgInfo) {
        this.mMessageInfo = msgInfo;
    }

    public void request(String account, Long threadId) {
        SendMsg api = new SendMsg();
        api.setMessageInfo(mMessageInfo);
        api.setAccountAndThreadId(account, threadId);
        api.setListener(this);
//        if (mSendQueue.size() <= 0) {
            api.request();
//        }
        mSendQueue.add(api);
    }

    @Override
    public void complete(Object owner, Object data, int success) {
        if (null != listener) {
            listener.complete(this, data, success);
        }
        SendMsg api = (SendMsg) owner;
        mSendQueue.remove(api);
//        if (mSendQueue.size() > 0) {
//            mSendQueue.get(0).request();
//        }
    }
}
