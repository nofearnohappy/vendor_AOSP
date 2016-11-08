package com.hesine.nmsg.business.bo;

import java.util.List;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.bean.LocalMessageInfo;
import com.hesine.nmsg.business.bean.MessageInfo;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.thirdparty.PNMessageHandler;
import com.hesine.nmsg.thirdparty.Statistics;

public class SendMsg extends
        Base<com.hesine.nmsg.business.bean.request.SendMsg, com.hesine.nmsg.business.bean.response.SendMsg> {

    private MessageInfo mMessageInfo = null;
    private Long mThreadId;
    private String mAccount = null;

    // if need be called when launcher send msg
    public void setMessageInfo(MessageInfo mMessageInfo) {
        this.mMessageInfo = mMessageInfo;
    }

    @Override
    public com.hesine.nmsg.business.bean.request.SendMsg contentObject() {
        com.hesine.nmsg.business.bean.request.SendMsg obj = new com.hesine.nmsg.business.bean.request.SendMsg();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_SEND_MSG);
        obj.setMessageInfo(mMessageInfo);
        obj.getActionInfo().setServiceAccount(mAccount);
        return obj;
    }

    @Override
    public void procRequestDataStore(com.hesine.nmsg.business.bean.request.SendMsg submitData) {
        String imsi = GlobalData.instance().getSystemInfo().getImsi();
        String imei = GlobalData.instance().getSystemInfo().getImei();
        String msgUuid = null;
        if (null == imsi) {
            msgUuid = ((imei == null) ? "" : imei) + System.currentTimeMillis();
        } else {
            msgUuid = imsi + System.currentTimeMillis();
        }
        mMessageInfo.setMsgUuid(msgUuid);
        int status = LocalMessageInfo.STATUS_TO_OUTBOX;
        DBUtils.insertMessage(mAccount, mThreadId, null, mMessageInfo, status);
    }

    @Override
    public com.hesine.nmsg.business.bean.response.SendMsg parseObject() {
        return new com.hesine.nmsg.business.bean.response.SendMsg();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.SendMsg parseData, int success) {
        int statusCode = LocalMessageInfo.STATUS_TO_FAILED;
        if (success == Pipe.NET_SUCCESS) {
            String srvAccount = parseData.getServiceAccount();
            List<String> msgIds = parseData.getMessageIds();
            if (srvAccount != null && msgIds != null && DBUtils.getServiceInfo(srvAccount) != null
                    && DBUtils.getServiceInfo(srvAccount).getStatus() == 1) {
                PNMessageHandler.procRequestMsgs(parseData.getServiceAccount(), msgIds);
            }
            statusCode = LocalMessageInfo.STATUS_TO_SENT;
            Statistics.getInstance().msgSendSuccess(mMessageInfo.getMsgUuid(), String.valueOf(mMessageInfo.getType()));
        }else{
            Statistics.getInstance().msgSendFail(mMessageInfo.getMsgUuid(), String.valueOf(mMessageInfo.getType())); 
        }
        
        DBUtils.updateMessageStatus(mMessageInfo.getMsgUuid(), statusCode);
        // BroadCastUtils.sendUpdateMessagesBroadcast();
    }

    public void setAccountAndThreadId(String account, Long threadId) {
        this.mAccount = account;
        this.mThreadId = threadId;
    }

}
