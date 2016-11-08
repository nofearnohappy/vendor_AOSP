package com.mediatek.rcs.message.cloudbackup.modules;

import android.util.Log;

import java.io.UnsupportedEncodingException;

import com.mediatek.rcs.common.RcsLog;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.RcsMessage;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.RcsMsgRecord;
import com.mediatek.rcs.message.cloudbackup.utils.FileUtils;
import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.BackupConstant;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MessageRecord;

/**
 * Build one ip text msg according to cmcc spec.
 * @author mtk81368
 *
 */
class Chat1To1BackupBuilder {
    private static final String CLASS_TAG = CloudBrUtils.MODULE_TAG + "Chat1To1BackupBuilder";

    /**
     * Wrap one of group chat message into according to cmcc spec.
     * @param msgRecord
     * @param rcsMsgRecord
     * @return
     */
    protected String buildOneChatMsg(MessageRecord msgRecord, RcsMsgRecord rcsMsgRecord) {
        StringBuilder cpim = new StringBuilder();
        cpim.append(BackupConstant.FROM + " ");
        cpim.append(rcsMsgRecord.getFrom());
        cpim.append(BackupConstant.LINE_BREAK);
        cpim.append(BackupConstant.TO + " ");
        cpim.append(rcsMsgRecord.getTo());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.DATE_TIME + " ");
        cpim.append(FileUtils.encodeDate(rcsMsgRecord.getTimestamp()));
        cpim.append(BackupConstant.LINE_BREAK);

        long sendTime = rcsMsgRecord.getDataSent();
        if (sendTime > 0) {
            cpim.append(BackupConstant.MTK_SEND_TIME + " ");
            cpim.append(FileUtils.encodeDate(sendTime));
            cpim.append(BackupConstant.LINE_BREAK);
        }

        cpim.append(BackupConstant.MTK_DIRECTION + " ");
        cpim.append(rcsMsgRecord.getDirection());
        cpim.append(BackupConstant.LINE_BREAK);

        //chat db message talbe status
        cpim.append(BackupConstant.MTK_STATUS + " ");
        cpim.append(msgRecord.getStatus());
        cpim.append(BackupConstant.LINE_BREAK);

        //rcs message table status
        cpim.append(BackupConstant.MTK_MSG_STATUS + " ");
        cpim.append(rcsMsgRecord.getStatus());
        cpim.append(BackupConstant.LINE_BREAK);

        //chat db message talbe status
        cpim.append(BackupConstant.MTK_CHAT_TYPE + " ");
        cpim.append(msgRecord.getType());
        cpim.append(BackupConstant.LINE_BREAK);

        //rcs message table class
        cpim.append(BackupConstant.MTK_CLASS + " ");
        cpim.append(rcsMsgRecord.getMsgClass());
        cpim.append(BackupConstant.LINE_BREAK);

        //rcs message table CHATMESSAGE_TYPE
        cpim.append(BackupConstant.MTK_TYPE + " ");
        cpim.append(rcsMsgRecord.getType());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_SUB_ID + " ");
        cpim.append(rcsMsgRecord.getSubID());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_LOCK + " ");
        cpim.append(rcsMsgRecord.getLocked());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_SEEN + " ");
        cpim.append(rcsMsgRecord.getSeen());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_FAV_FLAG + " ");
        cpim.append(rcsMsgRecord.getFlag());
        cpim.append(BackupConstant.LINE_BREAK);

        String body = rcsMsgRecord.getBody();

        int type = rcsMsgRecord.getMsgClass();
        // msg body store in rcsMsgRecord in favorite record,
        // others body instored in smsRecord.
        if (body == null && rcsMsgRecord.getBody() != null) {
            body = rcsMsgRecord.getBody().toString();
        }
        String content = buildMsgBody(type, body);
        cpim.append(content);
        return cpim.toString();
    }

    /**
     * Wrap one of group chat message into according to cmcc spec.
     * @param msgRecord
     * @param smsRecord
     * @return
     */
/*    protected String buildOneChatMsg(MessageRecord msgRecord, SmsRecord smsRecord) {
        StringBuilder cpim = new StringBuilder();
        cpim.append(BackupConstant.FROM + " ");
        cpim.append(msgRecord.getFrom());
        cpim.append(BackupConstant.LINE_BREAK);
        cpim.append(BackupConstant.TO + " ");
        cpim.append(msgRecord.getTo());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.DATE_TIME + " ");
        cpim.append(FileUtils.encodeDate(msgRecord.getTimestamp()));
        cpim.append(BackupConstant.LINE_BREAK);

        long sendTime = msgRecord.getSendTimestamp();
        if (sendTime > 0) {
            cpim.append(BackupConstant.MTK_SEND_TIME + " ");
            cpim.append(FileUtils.encodeDate(sendTime));
            cpim.append(BackupConstant.LINE_BREAK);
        }

        long delivTime = msgRecord.getDeliveredTimestamp();
        if (delivTime > 0) {
            cpim.append(BackupConstant.MTK_DELIVERED_TIME + " ");
            cpim.append(FileUtils.encodeDate(delivTime));
            cpim.append(BackupConstant.LINE_BREAK);
        }

        long dislayTime = msgRecord.getDisplayTimestamp();
        if (dislayTime > 0) {
            cpim.append(BackupConstant.MTK_DISPLAYED_TIME + " ");
            cpim.append(FileUtils.encodeDate(dislayTime));
            cpim.append(BackupConstant.LINE_BREAK);
        }

        cpim.append(BackupConstant.MTK_DIRECTION + " ");
        cpim.append(msgRecord.getDirection());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_STATUS + " ");
        cpim.append(msgRecord.getStatus());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_MSG_TYPE + " ");
        cpim.append(msgRecord.getType());
        cpim.append(BackupConstant.LINE_BREAK);

        String body = null;
        if (smsRecord != null) {
            cpim.append(BackupConstant.MTK_SUB_ID + " ");
            cpim.append(smsRecord.getSubId());
            cpim.append(BackupConstant.LINE_BREAK);

            cpim.append(BackupConstant.MTK_SMS_TYPE + " ");
            cpim.append(smsRecord.getType());
            cpim.append(BackupConstant.LINE_BREAK);

            cpim.append(BackupConstant.MTK_SMS_READ + " ");
            cpim.append(smsRecord.getRead());
            cpim.append(BackupConstant.LINE_BREAK);

            body = smsRecord.getBody();
        }

        String type = msgRecord.getMimeType();
        // msg body store in msgRecord in favorite record,
        // others body instored in smsRecord.
        if (body == null && msgRecord.getBody() != null) {
            body = msgRecord.getBody().toString();
        }
        String content = buildMsgBody(type, body);
        cpim.append(content);
        return cpim.toString();
    }*/

/*    private String buildMsgBody(String type, String body) {
        String content = null;
        String msgType = type;
        if (msgType.equals(CloudBrUtils.ContentType.TEXT_TYPE)) {
            Log.d(CLASS_TAG, "this msg is a text msg, begin backup text msg");
            content = createTextMsgBody(body);
        } else if (msgType.equals(CloudBrUtils.ContentType.VEMOTION_TYPE)) {
            content = createVemotionMsgBody(body);
        } else if (msgType.equals(CloudBrUtils.ContentType.CLOUDFILE_TYPE)) {
            content = createCloudFileMsgBody(body);
        }
        return content;
    }*/

    private String buildMsgBody(int type, String body) {
        String content = null;

        if (type == RcsLog.Class.NORMAL) {
            Log.d(CLASS_TAG, "this msg is a text msg, begin backup text msg");
            content = createTextMsgBody(body);
        } else if (type == RcsLog.Class.EMOTICON) {
            content = createVemotionMsgBody(body);
        } else if (type == RcsLog.Class.CLOUD) {
            content = createCloudFileMsgBody(body);
        }
        return content;
    }

    private String createTextMsgBody(String body) {
        StringBuilder content = new StringBuilder();
        content.append(BackupConstant.LINE_BREAK);
        content.append("Content-type: text/plain");
        content.append(BackupConstant.LINE_BREAK);
        if (body == null) {
            return content.toString();
        }

        String encodeBody = null;
        try {
            encodeBody = new String(body.getBytes(BackupConstant.CHARSET_UTF8));
        } catch (UnsupportedEncodingException e) {
            Log.e(CLASS_TAG, "createTextMsgBody UnsupportedEncodingException");
            e.printStackTrace();
            return null;
        }

        content.append(BackupConstant.LINE_BREAK);
        content.append(encodeBody);
        content.append(BackupConstant.LINE_BREAK);
        return content.toString();
    }

    private String createCloudFileMsgBody(String body) {
        Log.d(CLASS_TAG, "createCloudFileMsgBody begin");
        StringBuilder content = new StringBuilder();

        content.append(BackupConstant.LINE_BREAK);
        content.append("Content-type: application/cloudfile+xml");
        content.append(BackupConstant.LINE_BREAK);
        content.append(BackupConstant.LINE_BREAK);

        content.append(BackupConstant.LINE_BREAK);
        content.append(body);
        content.append(BackupConstant.LINE_BREAK);
        Log.d(CLASS_TAG, content.toString());
        return content.toString();
    }

    private String createVemotionMsgBody(String body) {
        Log.d(CLASS_TAG, "createVemotionMsgBody begin");
        StringBuilder content = new StringBuilder();

        content.append(BackupConstant.LINE_BREAK);
        content.append("Content-type: application/vemoticon+xml");
        content.append(BackupConstant.LINE_BREAK);
        content.append(BackupConstant.LINE_BREAK);

        content.append(BackupConstant.LINE_BREAK);
        content.append(body);
        content.append(BackupConstant.LINE_BREAK);
        Log.d(CLASS_TAG, content.toString());
        return content.toString();
    }

    /**
     * Wrap 1 to 1 chat message into according to cmcc spec.
     * @param msgRecord
     * @param smsRecord
     * @return
     */
    protected String build1To1ChatMsg(MessageRecord msgRecord, RcsMsgRecord rcsMsgRecord) {
        StringBuilder cpim = new StringBuilder();
        cpim.append(BackupConstant.FROM + " ");
        cpim.append(rcsMsgRecord.getFrom());
        cpim.append(BackupConstant.LINE_BREAK);
        cpim.append(BackupConstant.TO + " ");
        cpim.append(rcsMsgRecord.getTo());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.DATE + " ");
        cpim.append(FileUtils.encodeDate(rcsMsgRecord.getTimestamp()));
        cpim.append(BackupConstant.LINE_BREAK);

        if (msgRecord != null) {
            cpim.append(BackupConstant.CONVERST_ID + " ");
            cpim.append(msgRecord.getConversationId());
            cpim.append(BackupConstant.LINE_BREAK);

            //chat db message talbe status
            cpim.append(BackupConstant.MTK_STATUS + " ");
            cpim.append(msgRecord.getStatus());
            cpim.append(BackupConstant.LINE_BREAK);

            //chat db message talbe status
            cpim.append(BackupConstant.MTK_CHAT_TYPE + " ");
            cpim.append(msgRecord.getType());
            cpim.append(BackupConstant.LINE_BREAK);
        }

        cpim.append(BackupConstant.IMDN_MSG_ID + " ");
        cpim.append(rcsMsgRecord.getIpmsgId());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_SEND_TIME + " ");
        cpim.append(FileUtils.encodeDate(rcsMsgRecord.getDataSent()));
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_DIRECTION + " ");
        cpim.append(rcsMsgRecord.getDirection());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_STATUS + " ");
        cpim.append(rcsMsgRecord.getStatus());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_DIRECTION + " ");
        cpim.append(rcsMsgRecord.getDirection());
        cpim.append(BackupConstant.LINE_BREAK);

        //rcs message table status
        cpim.append(BackupConstant.MTK_MSG_STATUS + " ");
        cpim.append(rcsMsgRecord.getStatus());
        cpim.append(BackupConstant.LINE_BREAK);

        //rcs message table class
        cpim.append(BackupConstant.MTK_CLASS + " ");
        cpim.append(rcsMsgRecord.getMsgClass());
        cpim.append(BackupConstant.LINE_BREAK);

        //rcs message table CHATMESSAGE_TYPE
        cpim.append(BackupConstant.MTK_TYPE + " ");
        cpim.append(rcsMsgRecord.getType());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_SUB_ID + " ");
        cpim.append(rcsMsgRecord.getSubID());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_LOCK + " ");
        cpim.append(rcsMsgRecord.getLocked());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_SEEN + " ");
        cpim.append(rcsMsgRecord.getSeen());
        cpim.append(BackupConstant.LINE_BREAK);

        cpim.append(BackupConstant.MTK_FAV_FLAG + " ");
        cpim.append(rcsMsgRecord.getFlag());
        cpim.append(BackupConstant.LINE_BREAK);

        String body = rcsMsgRecord.getBody();
        int type = rcsMsgRecord.getMsgClass();

        String content = buildMsgBody(type, body);
        cpim.append(content);
        return cpim.toString();
    }
}
