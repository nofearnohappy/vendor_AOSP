package com.hesine.nmsg.business.bo;

import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.ActionInfo;
import com.hesine.nmsg.business.bean.LocalMessageInfo;
import com.hesine.nmsg.business.bean.MessageInfo;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.business.dao.NmsSMSMMSManager;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.thirdparty.Statistics;
import com.hesine.nmsg.ui.NmsgNotification;

public class GetMsg
        extends
        com.hesine.nmsg.business.bo.Base<com.hesine.nmsg.business.bean.request.GetMsg, 
                                    com.hesine.nmsg.business.bean.response.GetMsg> {

    private String serviceAccount;
    private List<String> messageUuids;
    private int msgQueueId;
    private int retryTime;

    @Override
    public com.hesine.nmsg.business.bean.request.GetMsg contentObject() {
        com.hesine.nmsg.business.bean.request.GetMsg obj = new com.hesine.nmsg.business.bean.request.GetMsg();
        obj.getActionInfo().setActionid(ActionInfo.ACTION_ID_GET_MSG);
        obj.getActionInfo().setServiceAccount(serviceAccount);
        obj.setMessageIds(messageUuids);
        return obj;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    @Override
    public com.hesine.nmsg.business.bean.response.GetMsg parseObject() {
        return new com.hesine.nmsg.business.bean.response.GetMsg();
    }

    @Override
    public void procReplyDataStore(com.hesine.nmsg.business.bean.response.GetMsg parseData, int success) {
        if (success == Pipe.NET_SUCCESS) {
            boolean ifServiceAccountExisted = DBUtils.ifServiceAccountExisted(serviceAccount);
            long lastestTimeStamp = parseData.getServiceAccountTS();
            boolean ifNeedUpdateSeviceAccount = false;
            Listener notListener = null;
            if (!ifServiceAccountExisted) {
                ifNeedUpdateSeviceAccount = true;
                notListener = new Listener();
            } else {
                long timeStamp = DBUtils.getServiceInfo(serviceAccount).getTimeStamp();
                if (lastestTimeStamp > timeStamp) {
                    ifNeedUpdateSeviceAccount = true;
                }
            }
            if (ifNeedUpdateSeviceAccount) {
                GetData getData = new GetData();
                getData.setServiceAccount(serviceAccount);
                getData.setListener(notListener);
                getData.start();
            }
            
            Iterator<MessageInfo> iter = parseData.getMessages().iterator();
            Upgrade.procUpgrade(parseData.getVersion());
            long threadId = -1;
            String from = null;
            String sms = null;
            while (iter.hasNext()) {
                MessageInfo info = iter.next();
                Statistics.getInstance().receiveMsgSuccess(info.getMsgUuid(),
                        String.valueOf(info.getType()));
                from = info.getFrom();
                sms = info.getSms();
                String ret = NmsSMSMMSManager.saveSmsToDb(info.getSms(), from, from,
                        System.currentTimeMillis(), info.getTime());
                long smsId = -1;
                if (null != ret) {
                    String[] fields = ret.split(",");
                    if (fields != null && fields.length == 2) {
                        threadId = Long.parseLong(fields[0]);
                        smsId = Long.parseLong(fields[1]);
                    }
                }

                long msgId = DBUtils.insertMessage(from, threadId, smsId, info);
                if(null != notListener) {
                    notListener.setMsgId((int)msgId);
                }
                // send broadcast for conversationactivity
                Intent intent = new Intent();
                intent.setAction(EnumConstants.NMSG_INTENT_ACTION);
                intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, from);
                Application.getInstance().sendBroadcast(intent);
                if (ifServiceAccountExisted
                        && DBUtils.getMessageStatusViaMsgId((int)msgId) == LocalMessageInfo.STATUS_FROM_UNREAD) {
                    NmsgNotification.getInstance(com.hesine.nmsg.Application.getInstance())
                            .showNmsgNotification(threadId, from, sms);
                }
            }
        }
    }

    class Listener implements Pipe {
        private int msgId;

        public void setMsgId(int msgId) {
            this.msgId = msgId;
        }

        @Override
        public void complete(Object owner, Object data, int success) {
            long threadId = 0;
            String smsContent = null;

            com.hesine.nmsg.business.bean.response.GetData getData = 
                    (com.hesine.nmsg.business.bean.response.GetData) data;
            String account = getData.getServiceInfo().getAccount();

            Cursor cr = DBUtils.getMessageCursorViaMsgId(msgId);
            while (cr != null && cr.moveToNext()) {
                if (cr.getCount() > 0) {
                    threadId = cr.getLong(cr.getColumnIndex("thread_id"));
                    smsContent = cr.getString(cr.getColumnIndex("sms"));
                }
            }

            if (cr != null) {
                cr.close();
            }

            if (DBUtils.getMessageStatusViaMsgId(msgId) == LocalMessageInfo.STATUS_FROM_UNREAD) {
                NmsgNotification.getInstance(com.hesine.nmsg.Application.getInstance())
                        .showNmsgNotification(threadId, account, smsContent);
            }
        }

    }

    public void setMessageUuids(List<String> messageUuids) {
        this.messageUuids = messageUuids;
    }
    
    public String getServiceAccount() {
        return serviceAccount;
    }
    
    public List<String> getMessageUuids() {
        return messageUuids;
    }
    
    public void setMessageQueueId(int msgQueueId) {
        this.msgQueueId = msgQueueId;
    }
    
    public int getMessageQueueId() {
        return msgQueueId;
    }
    
    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }
    
    public int getRetryTime() {
        return retryTime;
    }
    
}
