package com.hesine.nmsg.business.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.hesine.nmsg.business.bean.AttachInfo;
import com.hesine.nmsg.business.bean.LocalMessageInfo;
import com.hesine.nmsg.business.bean.MessageInfo;
import com.hesine.nmsg.business.bean.MessageItemInfo;
import com.hesine.nmsg.business.bean.PicInfo;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.bean.UserInfo;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.MLog;

public class DBUtils {

    // for msg
    public static final String ID = "_id";
    public static final String MSG_ACCOUNT = "account";
    public static final String MSG_THREAD_ID = "thread_id";
    public static final String MSG_SMS_ID = "sms_id";
    public static final String MSG_UUID = "msg_uuid";
    public static final String MSG_TYPE = "msg_type";
    public static final String MSG_FROM = "sender";
    public static final String MSG_TO = "receiver";
    public static final String MSG_SMS = "sms";
    public static final String MSG_STATUS = "status";
    public static final String MSG_TIME = "update_time";

    public static final String MSG_ITEM_ID = "item_id";
    public static final String MSG_ID = "msg_id";
    public static final String MSG_ITEM_SUBJECT = "subject";
    public static final String MSG_ITEM_DESC = "desc";
    public static final String MSG_ITEM_SHORT_LINK = "short_link";
    public static final String MSG_ITEM_LONG_LINK = "long_link";
    public static final String MSG_ITEM_BODY = "body";
    public static final String MSG_ITEM_ATTACH_TYPE = "attach_type";
    public static final String MSG_ITEM_ATTACH_NAME = "attach_name";
    public static final String MSG_ITEM_ATTACH_SIZE = "attach_size";
    public static final String MSG_ITEM_ATTACH_URL = "attach_url";
    public static final String MSG_ITEM_ATTACHMENT = "attachment";

    public static final String MSG_RETRY_TIME = "retry_time";
    public static final String MSG_PENDING_STATUS = "pending";
    public static final String MSG_CREATE_TIME = "create_time";
    // private static final String TAG = "DBUtils";

    public static Long insertServiceInfo(ServiceInfo si, PicInfo pi) {
        ContentValues values = new ContentValues();
        values.put("account", si.getAccount());
        values.put("account_name", si.getName());
        values.put("email", si.getEmail());
        values.put("phone_number", si.getPhoneNum());
        values.put("desc", si.getDesc());
        values.put("update_time", si.getTimeStamp());
        values.put("icon", PicInfo.getPicPath(pi));
        String table = LocalStore.TABLE_ACCOUNTS;

        return LocalStore.instance().insert(table, null, values);
    }

    public static int updateServiceInfo(ServiceInfo si, PicInfo pi) {
        ContentValues values = new ContentValues();
        values.put("account", si.getAccount());
        values.put("account_name", si.getName());
        values.put("email", si.getEmail());
        values.put("phone_number", si.getPhoneNum());
        values.put("desc", si.getDesc());
        values.put("update_time", si.getTimeStamp());
        values.put("icon", PicInfo.getPicPath(pi));
        String table = LocalStore.TABLE_ACCOUNTS;

        return LocalStore.instance().update(table, values, "account = '" + si.getAccount() + "'", null);
    }

    public static ServiceInfo getServiceInfo(String serviceAccount) {
        String[] columns = { "_id", "account", "account_name", "email", "phone_number", "desc",
            "update_time", "icon", "status", "is_exist" };
        String selection = "account = '" + serviceAccount + "'";
        String table = LocalStore.TABLE_ACCOUNTS;
        Cursor c = LocalStore.instance().query(table, columns, selection, null, null, null, null);

        if (c != null && c.moveToFirst()) {
            if (c.getCount() > 0) {
                ServiceInfo si = new ServiceInfo();
                si.setId(c.getLong(c.getColumnIndex("_id")));
                si.setDesc(c.getString(c.getColumnIndex("desc")));
                si.setAccount(c.getString(c.getColumnIndex("account")));
                si.setEmail(c.getString(c.getColumnIndex("email")));
                si.setName(c.getString(c.getColumnIndex("account_name")));
                si.setPhoneNum(c.getString(c.getColumnIndex("phone_number")));
                si.setTimeStamp(c.getLong(c.getColumnIndex("update_time")));
                si.setIcon(c.getString(c.getColumnIndex("icon")));
                si.setStatus(c.getInt(c.getColumnIndex("status")));
                si.setIsExist(c.getInt(c.getColumnIndex("is_exist")));
                if (null != c) {
                    c.close();
                }
                return si;
            }
        }

        if (null != c) {
            c.close();
        }
        return null;
    }

    public static boolean updateServiceInfo(String account, boolean isExist) {
        boolean ret = false;
        ContentValues values = new ContentValues();
        values.put("is_exist", isExist ? 1 : 0);
        // values.put("update_time", System.currentTimeMillis());
        String table = LocalStore.TABLE_ACCOUNTS;
        int update = LocalStore.instance().update(table, values, "account = '" + account + "'",
                null);
        if (update > 0) {
            ret = true;
        }
        return ret;
    }

    public static boolean updateServiceInfoStatus(String account, boolean isOn) {
        boolean ret = false;
        ContentValues values = new ContentValues();
        values.put("status", isOn ? 1 : 0);
        // values.put("update_time", System.currentTimeMillis());
        String table = LocalStore.TABLE_ACCOUNTS;
        int update = LocalStore.instance().update(table, values, "account = '" + account + "'",
                null);
        if (update > 0) {
            ret = true;
        }
        return ret;
    }

    public static ArrayList<ServiceInfo> getServiceInfos() {
        String[] columns = { "_id", "account", "account_name", "email", "phone_number", "desc",
                "update_time", "icon", "status" };
        String selection = "";
        String table = LocalStore.TABLE_ACCOUNTS;
        Cursor c = LocalStore.instance().query(table, columns, selection, null, null, null, null);
        ArrayList<ServiceInfo> serviceInfos = new ArrayList<ServiceInfo>();
        if (c != null && c.moveToFirst()) {
            do {
                ServiceInfo si = new ServiceInfo();
                si.setDesc(c.getString(c.getColumnIndex("desc")));
                si.setEmail(c.getString(c.getColumnIndex("email")));
                si.setName(c.getString(c.getColumnIndex("account_name")));
                si.setPhoneNum(c.getString(c.getColumnIndex("phone_number")));
                si.setTimeStamp(c.getLong(c.getColumnIndex("update_time")));
                si.setIcon(c.getString(c.getColumnIndex("icon")));
                si.setStatus(c.getInt(c.getColumnIndex("status")));
                si.setAccount(c.getString(c.getColumnIndex("account")));
                serviceInfos.add(si);
            } while (c.moveToNext());
        }
        if (null != c) {
            c.close();
        }
        return serviceInfos;
    }

    public static boolean ifServiceAccountExisted(String serviceAccount) {
        String[] columns = { "_id", "account" };
        String selection = "account = '" + serviceAccount + "'";
        String table = LocalStore.TABLE_ACCOUNTS;
        Cursor c = LocalStore.instance().query(table, columns, selection, null, null, null, null);

        if (c != null && c.moveToFirst()) {
            if (c.getCount() > 0) {
                c.close();
                return true;
            }
        }
        if (null != c) {
            c.close();
        }
        return false;
    }

    public static Long insertMessage(String account, Long threadId, Long smsId, MessageInfo info) {
        int status = LocalMessageInfo.STATUS_FROM_UNREAD;
        if (CommonUtils.currentActivityIsNmsg(info.getFrom())) {
            status = LocalMessageInfo.STATUS_FROM_READ;
        }
        return insertMessage(account, threadId, smsId, info, status);
    }

    public static Long insertMessage(String account, Long threadId, Long smsId, MessageInfo info,
            int status) {
        Long id = null;
        ContentValues values = new ContentValues();
        values.put("account", account);
        values.put("thread_id", threadId);
        values.put("sms_id", smsId);
        values.put("msg_uuid", info.getMsgUuid());
        values.put("msg_type", info.getType());
        values.put("sender", info.getFrom());
        values.put("receiver", info.getTo());
        values.put("sms", info.getSms());
        values.put("status", status);
        values.put("update_time", System.currentTimeMillis());
        id = LocalStore.instance().insert(LocalStore.TABLE_MESSAGES, null, values);
        MLog.info("insertMessage account "+account+" thread_id"+threadId+" msg_uuid"+info.getMsgUuid()+" id"+id);
        if(id > 0){
            info.setId(id.intValue());
            List<MessageItemInfo> iis = info.getMessageItems();
            if (iis != null && iis.size() > 0) {
                for (MessageItemInfo messageItemInfo : iis) {
                    insertMessageItem(info, messageItemInfo);
                }
            }
        }
        return id;
    }

    public static boolean updateMessageStatus(String msgId, int statusCode) {
        boolean ret = false;
        ContentValues values = new ContentValues();
        values.put("status", statusCode);
        String table = LocalStore.TABLE_MESSAGES;
        int update = LocalStore.instance()
                .update(table, values, "msg_uuid = '" + msgId + "'", null);
        if (update > 0) {
            ret = true;
        }
        return ret;
    }

    public static void updateMessageStatusViaThread(Long threadId) {
        Cursor cr = getMessageCursorViaThreadId(threadId);
        while (null != cr && cr.moveToNext()) {
            if (cr.getCount() > 0) {
                if (cr.getInt(cr.getColumnIndex(MSG_STATUS)) == LocalMessageInfo.STATUS_FROM_UNREAD) {
                    updateMessageStatus(cr.getString(cr.getColumnIndex(MSG_UUID)),
                            LocalMessageInfo.STATUS_FROM_READ);
                }
            }
        }
        if (null != cr) {
            cr.close();
        }
    }

    public static Long insertMessageItem(MessageInfo msgInfo, MessageItemInfo info) {
        Long id = null;
        ContentValues values = new ContentValues();
        values.put("msg_id",msgInfo.getId());
        values.put("msg_uuid", msgInfo.getMsgUuid());
        values.put("item_id", info.getId());
        values.put("subject", info.getSubject());
        values.put("desc", info.getDesc());
        values.put("short_link", info.getShortLink());
        values.put("long_link", info.getLongLink());
        values.put("body", info.getBody());
        AttachInfo ai = info.getAttachInfo();

        if (ai != null) {
            values.put("attach_name", ai.getName());
            values.put("attach_size", ai.getSize());
            values.put("attach_type", ai.getType());
            values.put("attach_url", ai.getUrl());
            AttachInfo.saveAttachment(ai);
            values.put("attachment", AttachInfo.getAttachmentAbsPath(ai));
        }
        id = LocalStore.instance().insert(LocalStore.TABLE_MESSAGE_ITEM, null, values);
        return id;
    }

    public static Long insertUser(UserInfo info) {
        Long id = null;
        ContentValues values = new ContentValues();
        values.put("account", info.getAccount());
        values.put("email", info.getEmail());
        values.put("user_name", info.getName());
        values.put("phone_number", info.getPhoneNum());
        values.put("sign", info.getSign());
        values.put("user_sex", info.getSex());
        values.put("icon", info.getIcon());
        values.put("update_time", System.currentTimeMillis());
        values.put("age", info.getAge());
        String table = LocalStore.TABLE_USERINFO;
        id = LocalStore.instance().replaceOrThrow(table, null, values);
        MLog.info("insertUser account "+info.getAccount()+" user_name"+info.getName()+" id"+id);
        return id;
    }

    public static Long updateUser(UserInfo info) {
        Long id = null;
        ContentValues values = new ContentValues();
        values.put("account", info.getAccount());
        values.put("email", info.getEmail());
        values.put("user_name", info.getName());
        values.put("phone_number", info.getPhoneNum());
        values.put("sign", info.getSign());
        values.put("user_sex", info.getSex());
        values.put("icon", info.getIcon());
        values.put("age", info.getAge());
        values.put("update_time", System.currentTimeMillis());
        String table = LocalStore.TABLE_USERINFO;
        // id = (long) LocalStore.instance().update(table, values,
        // "phone_number =?", new String[]{info.getPhoneNum()});
        id = (long) LocalStore.instance().update(table, values, "account =?",
                new String[] { info.getAccount() });
        MLog.info("updateUser account "+info.getAccount()+" user_name"+info.getName()+" id"+id);
        if (id <= 0) {
            insertUser(info);
        }
        return id;
    }

    public static UserInfo getUser(String account) {
        String[] columns = { "_id", "account", "email", "user_name", "age", "phone_number", "sign",
                "user_sex", "icon", "update_time" };
        String table = LocalStore.TABLE_USERINFO;
        Cursor c = LocalStore.instance().query(table, columns, "account =?",
                new String[] { account }, null, null, null);
        if (c != null && c.moveToFirst()) {
            UserInfo ui = new UserInfo();
            ui.setAccount(c.getString(c.getColumnIndex("account")));
            ui.setEmail(c.getString(c.getColumnIndex("email")));
            ui.setName(c.getString(c.getColumnIndex("user_name")));
            ui.setPhoneNum(c.getString(c.getColumnIndex("phone_number")));
            ui.setAge(c.getInt(c.getColumnIndex("age")));
            ui.setIcon(c.getInt(c.getColumnIndex("icon")));
            ui.setSign(c.getString(c.getColumnIndex("sign")));
            ui.setSex(c.getInt(c.getColumnIndex("user_sex")));
            c.close();
            return ui;
        }

        if (c != null) {
            c.close();
        }
        return null;
    }

    public static Cursor getMessageCursor(String account) {
        String[] columns = { "_id", "account", "thread_id", "sms_id", "msg_uuid", "msg_type",
                "sender", "receiver", "sms", "status", "update_time" };
        String selection = "account = '" + account + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);
        return c;
    }

    public static Cursor getMessageCursorViaMsgId(int msgId) {
        String[] columns = { "_id", "account", "thread_id", "sms_id", "msg_uuid", "msg_type",
                "sender", "receiver", "sms", "status", "update_time" };
        String selection = "_id = '" + msgId + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);
        return c;
    }

    public static int getMessageStatusViaMsgId(int msgId) {
        String[] columns = { "_id", "status" };
        String selection = "_id = '" + msgId + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);

        int ret = -1;
        if (c != null && c.moveToFirst()) {
            if (c.getCount() > 0) {
                ret = c.getInt(c.getColumnIndex(MSG_STATUS));
            }
        }

        if (null != c) {
            c.close();
        }
        return ret;
    }

    public static long getThreadIdViaAccount(String account) {
        String[] columns = { "_id", "thread_id" };
        String selection = "account = '" + account + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);

        long ret = -1;
        if (c != null && c.moveToFirst()) {
            if (c.getCount() > 0) {
                ret = c.getLong(c.getColumnIndex(MSG_THREAD_ID));
            }
        }

        if (null != c) {
            c.close();
        }
        return ret;
    }

    public static int getMsgIdViaAccount(String account) {
        String[] columns = { "min(_id)" };
        String selection = "account = '" + account + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);

        int ret = 0;
        if (c != null && c.moveToFirst()) {
            if (c.getCount() > 0) {
                ret = c.getInt(c.getColumnIndex("min(_id)"));
            }
        }

        if (null != c) {
            c.close();
        }
        return ret;
    }

    public static int getUnreadNumViaAccount(String account) {
        String[] columns = { "_id", "account", "thread_id", "sms_id", "msg_uuid", "msg_type",
                "sender", "receiver", "sms", "status", "update_time" };
        String selection = "account = '" + account + "' and status = '"
                + LocalMessageInfo.STATUS_FROM_UNREAD + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);
        int ret = c.getCount();

        if (null != c) {
            c.close();
        }
        return ret;
    }

    public static Cursor getMessageCursorViaThreadId(long threadID) {
        String[] columns = { "_id", "account", "thread_id", "sms_id", "msg_uuid", "msg_type",
                "sender", "receiver", "sms", "status", "update_time" };
        String selection = "thread_id = '" + threadID + "'";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, selection, null,
                null, null, null);
        return c;
    }

    public static HashSet<Long> getMessageThreads() {
        String[] columns = { "thread_id" };
        HashSet<Long> threads = new HashSet<Long>();
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, columns, null, null,
                null, null, null);

        while (c != null && c.moveToNext()) {
            if (c.getCount() > 0) {
                Long thread = c.getLong(c.getColumnIndex("thread_id"));
                threads.add(thread);
            }
        }

        if (c != null) {
            c.close();
        }

        return threads;
    }

    public static Cursor getMessageItemCursor(int id) {
        String[] columns = { "_id", "msg_id", "msg_uuid", "item_id", "subject", "desc", "short_link",
                "long_link", "body", "attach_type", "attach_name", "attach_size", "attach_url",
                "attachment" };
        String selection = "msg_id = '" + id + "'";
        String orderBy = "item_id";
        Cursor c = LocalStore.instance().query(LocalStore.TABLE_MESSAGE_ITEM, columns, selection,
                null, null, null, orderBy);
        return c;
    }

    public static LocalMessageInfo getMsgViaCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        LocalMessageInfo msg = new LocalMessageInfo();

        msg.setId(cursor.getInt(cursor.getColumnIndex(ID)));
        msg.setMsgUuid(cursor.getString(cursor.getColumnIndex(MSG_UUID)));
        msg.setTime(cursor.getLong(cursor.getColumnIndex(MSG_TIME)));
        msg.setStatus(cursor.getInt(cursor.getColumnIndex(MSG_STATUS)));
        msg.setTo(cursor.getString(cursor.getColumnIndex(MSG_TO)));
        msg.setFrom(cursor.getString(cursor.getColumnIndex(MSG_FROM)));
        msg.setSms(cursor.getString(cursor.getColumnIndex(MSG_SMS)));
        msg.setType(cursor.getInt(cursor.getColumnIndex(MSG_TYPE)));
        Cursor cursorItem = getMessageItemCursor(msg.getId());
        List<MessageItemInfo> msgItems = new ArrayList<MessageItemInfo>();
        if (null != cursorItem && cursorItem.moveToFirst()) {
            do {
                msgItems.add(getMsgItemViaCursor(cursorItem));
            } while (cursorItem.moveToNext());
            cursorItem.close();
        }
        msg.setMessageItems(msgItems);
        return msg;
    }

    public static MessageItemInfo getMsgItemViaCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        MessageItemInfo msgItem = new MessageItemInfo();

        msgItem.setId(cursor.getInt(cursor.getColumnIndex(MSG_ITEM_ID)));
        msgItem.setMsgId(cursor.getInt(cursor.getColumnIndex(MSG_ID)));
        msgItem.setUuid(cursor.getString(cursor.getColumnIndex(MSG_UUID)));
        msgItem.setBody(cursor.getString(cursor.getColumnIndex(MSG_ITEM_BODY)));
        msgItem.setDesc(cursor.getString(cursor.getColumnIndex(MSG_ITEM_DESC)));
        msgItem.setLongLink(cursor.getString(cursor.getColumnIndex(MSG_ITEM_LONG_LINK)));
        msgItem.setShortLink(cursor.getString(cursor.getColumnIndex(MSG_ITEM_SHORT_LINK)));
        msgItem.setSubject(cursor.getString(cursor.getColumnIndex(MSG_ITEM_SUBJECT)));

        String attachType = cursor.getString(cursor.getColumnIndex(MSG_ITEM_ATTACH_TYPE));

        if (attachType != null && attachType.length() > 0) {
            AttachInfo ai = new AttachInfo();
            ai.setType(attachType);
            ai.setName(cursor.getString(cursor.getColumnIndex(MSG_ITEM_ATTACH_NAME)));
            ai.setSize(cursor.getString(cursor.getColumnIndex(MSG_ITEM_ATTACH_SIZE)));
            ai.setUrl(cursor.getString(cursor.getColumnIndex(MSG_ITEM_ATTACH_URL)));
            ai.setAttachment(cursor.getString(cursor.getColumnIndex(MSG_ITEM_ATTACHMENT)));
            msgItem.setAttachInfo(ai);
        }
        return msgItem;
    }

//    public static void deleteMsgViaId(int msgId) {
//        LocalStore.instance().del(LocalStore.TABLE_MESSAGES, msgId);
//    }

    public static void deleteMsgViaMsgId(int msgId) {
        Long smsId = getSMSIDViaMsgId(msgId);
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGES, "_id = ?",
                new String[] { String.valueOf(msgId) });
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGE_ITEM, "msg_id = ?",
                new String[] { String.valueOf(msgId) });
        NmsMtkBinderApi.getInstance().delete(Uri.parse("content://sms/" + smsId), null, null);
    }

    public static long getSMSIDViaMsgId(int msgId) {
        String[] columns = { "_id", "sms_id" };
        String selection = "_id = '" + msgId + "'";
        String table = LocalStore.TABLE_MESSAGES;
        Cursor c = LocalStore.instance().query(table, columns, selection, null, null, null, null);
        long ret = -1;
        if (c != null && c.moveToFirst()) {
            if (c.getCount() > 0) {
                ret = c.getLong(c.getColumnIndex("sms_id"));
            }
        }

        if (null != c) {
            c.close();
        }
        return ret;
    }

    public static void deleteMsgViaAccount(String account) {
        Cursor cr = getMessageCursor(account);
        while (cr != null && cr.moveToNext()) {
            if (cr.getCount() > 0) {
                String uuid = cr.getString(cr.getColumnIndex("msg_uuid"));
                LocalStore.instance().delete(LocalStore.TABLE_MESSAGE_ITEM, "msg_uuid = ?",
                        new String[] { uuid });
            }
        }
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGES, "account = ?",
                new String[] { account });
        if (cr != null) {
            cr.close();
        }

    }

    public static void deleteMsgViaThreadId(long threadId) {
        Cursor cr = getMessageCursorViaThreadId(threadId);
        while (cr != null && cr.moveToNext()) {
            if (cr.getCount() > 0) {
                int id = cr.getInt(cr.getColumnIndex("_id"));
                LocalStore.instance().delete(LocalStore.TABLE_MESSAGE_ITEM, "msg_id = ?",
                        new String[] { String.valueOf(id) });
            }
        }
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGES, "thread_id = ?",
                new String[] { String.valueOf(threadId) });
        if (cr != null) {
            cr.close();
        }

    }

    /* delete message in MMS and nmsg */
    public static int deleteAllMsgViaThreadId(long threadId) {
        deleteMsgViaThreadId(threadId);
        return NmsMtkBinderApi.getInstance().delete(NmsSMSMMS.SMS_CONTENT_URI, "thread_id = ?",
                new String[] { String.valueOf(threadId) });
    }

    public static String getSenderViaThreadId(long threadId) {
        Cursor cr = DBUtils.getMessageCursorViaThreadId(threadId);
        String accountName = null;
        while (null != cr && cr.moveToNext()) {
            if (cr.getCount() > 0) {
                if (cr.getInt(cr.getColumnIndex("status")) == LocalMessageInfo.STATUS_FROM_READ
                        || cr.getInt(cr.getColumnIndex("status")) == LocalMessageInfo.STATUS_FROM_UNREAD) {
                    accountName = cr.getString(cr.getColumnIndex("sender"));
                    break;
                }
            }
        }
        if (cr != null) {
            cr.close();
        }
        return accountName;
    }

    public static void deleteAllData() {
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGE_ITEM, null, null);
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGES, null, null);
        // LocalStore.instance().delete(LocalStore.TABLE_ACCOUNTS,null,null);
        // LocalStore.instance().delete(LocalStore.TABLE_USERINFO,null,null);
    }
    
    public static void insertMessageQueue(String account, String msgUuid) {
        ContentValues cv = new ContentValues();
        cv.put(MSG_ACCOUNT, account);
        cv.put(MSG_UUID, msgUuid);
        cv.put(MSG_PENDING_STATUS, 0);
        cv.put(MSG_CREATE_TIME, System.currentTimeMillis());
        LocalStore.instance().insert(LocalStore.TABLE_MESSAGES_QUEUE, null, cv);
    }
    
    public static Cursor queryMessageQueue() {
        return LocalStore.instance().query(LocalStore.TABLE_MESSAGES_QUEUE, null, null, null,
                null, null, null);
    }
    
    public static Cursor queryPendingStatusMsg() {
        String selection = MSG_PENDING_STATUS + " = '" + 1 + "'";
        return LocalStore.instance().query(LocalStore.TABLE_MESSAGES_QUEUE, null, selection, null,
                null, null, null);
    }
    
    public static void updateMsgQueue(int msgQueueId,ContentValues cv) {
        LocalStore.instance().update(LocalStore.TABLE_MESSAGES_QUEUE, cv, ID + " = ?",
                new String[] { String.valueOf(msgQueueId) });
    }

    public static void deleteMessageQueueViaMsgQueueId(int msgQueueId) {
        LocalStore.instance().delete(LocalStore.TABLE_MESSAGES_QUEUE, ID + " = ?",
                new String[] { String.valueOf(msgQueueId) });
    }
    
    public static boolean queryMsgUuidIsExistInMessageQueue(String[] msgUuids, String account) {
        String selection = MSG_UUID + " = '" + TextUtils.join(",", msgUuids) + "' and " + MSG_ACCOUNT + " = '"
                + account + "'";
        Cursor cr = LocalStore.instance().query(LocalStore.TABLE_MESSAGES_QUEUE, null, selection,
                null, null, null, null);
        if (null != cr && cr.moveToNext() && cr.getCount() > 0) {
            cr.close();
            cr = null;
            return true;
        }
        if(null != cr){
            cr.close();
            cr = null;
        }
        return false;
    }
    
    public static boolean queryMsgUuidIsExistInMsgs(String[] msgUuids, String account) {
        Cursor cr = null;
        for (String msgUuid : msgUuids) {
            String selection = MSG_UUID + " = '" + msgUuid + "' and " + MSG_ACCOUNT + " = '"
                    + account + "'";
            cr = LocalStore.instance().query(LocalStore.TABLE_MESSAGES, null, selection, null,
                    null, null, null);
            if (null != cr && cr.moveToNext() && cr.getCount() > 0) {
                cr.close();
                cr = null;
            } else {
                if (null != cr) {
                    cr.close();
                    cr = null;
                }
                return false;
            }
        }

        return true;
    }
}
