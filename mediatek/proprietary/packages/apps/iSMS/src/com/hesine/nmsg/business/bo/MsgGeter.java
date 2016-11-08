package com.hesine.nmsg.business.bo;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Statistics;

public class MsgGeter implements Pipe {

    private Pipe listener = null;

    public void setListener(Pipe listener) {
        this.listener = listener;
    }

    public void request(String serviceAccount, List<String> messageIds) {
        DBUtils.insertMessageQueue(serviceAccount, TextUtils.join(",", messageIds.toArray()));
        addToGetMsgQueueIfExist();
    }

    @Override
    public void complete(Object owner, Object data, int code) {
        if (null != listener) {
            listener.complete(this, data, code);
        }
        GetMsg api = (GetMsg) owner;
        int msgQueueId = api.getMessageQueueId();
        List<String> msgUuids = api.getMessageUuids();
//        MLog.info("receive msg result msgUuids: " + TextUtils.join(",", msgUuids.toArray())
//                + " code : " + code);
        if (code == Pipe.NET_SUCCESS) {
            DBUtils.deleteMessageQueueViaMsgQueueId(msgQueueId);
        } else if (code == Pipe.NET_FAIL) {
            ContentValues cv = new ContentValues();
            cv.put(DBUtils.MSG_PENDING_STATUS, 0);
            DBUtils.updateMsgQueue(msgQueueId, cv);
        } else {
            DBUtils.deleteMessageQueueViaMsgQueueId(msgQueueId);
            Statistics.getInstance().receiveMsgFail(msgUuids, code, api.getRetryTime());
        }
    }

    public synchronized void addToGetMsgQueueIfExist() {
        Cursor cr = DBUtils.queryMessageQueue();
        while (null != cr && cr.moveToNext()) {
            if (cr.getCount() > 0) {
                int retryTime = cr.getInt(cr.getColumnIndex(DBUtils.MSG_RETRY_TIME));
                int msgQueueId = cr.getInt(cr.getColumnIndex(DBUtils.ID));
                String msgUuid = cr.getString(cr.getColumnIndex(DBUtils.MSG_UUID));
                int pending = cr.getInt(cr.getColumnIndex(DBUtils.MSG_PENDING_STATUS));
                long updateTime = cr.getLong(cr.getColumnIndex(DBUtils.MSG_TIME));
                long createTime = cr.getLong(cr.getColumnIndex(DBUtils.MSG_CREATE_TIME));
                String account = cr.getString(cr.getColumnIndex(DBUtils.MSG_ACCOUNT));
                if (retryTime >= EnumConstants.MAX_RETRY_TIME
                        && System.currentTimeMillis() - createTime > EnumConstants.ONE_DAY) {
                    DBUtils.deleteMessageQueueViaMsgQueueId(msgQueueId);
                    Statistics.getInstance().receiveMsgFail(stringToList(msgUuid), Pipe.NET_FAIL,
                            retryTime);
                } else if (pending == 0
                        || (pending == 1 && System.currentTimeMillis() - updateTime > EnumConstants.TWENTY_MINUTES)) {
                    GetMsg api = new GetMsg();
                    api.setMessageUuids(stringToList(msgUuid));
                    api.setMessageQueueId(msgQueueId);
                    api.setServiceAccount(account);
                    api.setRetryTime(retryTime + 1);
                    api.setListener(this);
                    api.request();
                    ContentValues cv = new ContentValues();
                    cv.put(DBUtils.MSG_PENDING_STATUS, 1);
                    cv.put(DBUtils.MSG_RETRY_TIME, retryTime + 1);
                    cv.put(DBUtils.MSG_TIME, System.currentTimeMillis());
                    DBUtils.updateMsgQueue(msgQueueId, cv);
                }
            }
        }
        if (null != cr) {
            cr.close();
            cr = null;
        }
    }

    public List<String> stringToList(String msgUuid) {
        List<String> msgUuids = new ArrayList<String>();
        String[] strs = msgUuid.split("[,]");
        for (String msgUuidTemp : strs) {
            msgUuids.add(msgUuidTemp);
        }
        return msgUuids;
    }

    public void updatePendingStatusWhenApplicaitonStart() {
        Cursor cr = DBUtils.queryPendingStatusMsg();
        while (null != cr && cr.moveToNext()) {
            if (cr.getCount() > 0) {
                int msgQueueId = cr.getInt(cr.getColumnIndex(DBUtils.ID));
                String msgUuid = cr.getString(cr.getColumnIndex(DBUtils.MSG_UUID));
                ContentValues cv = new ContentValues();
                cv.put(DBUtils.MSG_PENDING_STATUS, 0);
                DBUtils.updateMsgQueue(msgQueueId, cv);
                MLog.info("update pending status when application start msgId: " + msgQueueId
                        + " msgUuid: " + msgUuid);
            }
        }
        if (null != cr) {
            cr.close();
            cr = null;
        }
    }
}
