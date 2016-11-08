package com.mediatek.rcs.message.utils;

import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;

import com.mediatek.rcs.common.GroupManager;
import com.mediatek.rcs.common.GroupManager.IInitGroupListener;
import com.mediatek.rcs.common.binder.RCSServiceManager.INotifyListener;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.RCSDataBaseUtils;
import com.mediatek.rcs.common.provider.ReceiveMessageStruct;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;
import com.mediatek.rcs.common.utils.Logger;
import com.mediatek.rcs.common.utils.RCSUtils;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.plugin.RcsComposeActivity;
import com.mediatek.rcs.message.plugin.RcsMessagingNotification;
import com.mediatek.rcs.message.plugin.RcsUtilsPlugin;

import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.SendModeChangedNotifyService;

public class NewMessageReceiver implements INotifyListener {

    private static final String TAG = "NewMessageReceiver";

    private static NewMessageReceiver sInstance;
    private Context mContext;
    private Context mPluginContext;

    private String[] mFaiedMsgProjection = { Sms.THREAD_ID, Sms.ADDRESS, Sms.BODY,
            Sms.IPMSG_ID, Sms.PROTOCOL, Sms.SUBSCRIPTION_ID};
    private static final int MAX_LENGTH_OF_TRANSFER_TO_SMS_WHEN_SEND_FAILED = 450;

    private NewMessageReceiver(Context context) {
        mContext = context;
//        RcsMessageUtils.addIpMsgNotificationListeners(mContext, this);
        RCSServiceManager.getInstance().registNotifyListener(this);
        mPluginContext = ContextCacher.getPluginContext();
    }

    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new NewMessageReceiver(context);
        }
    }

    public static NewMessageReceiver getInstance() {
        if (sInstance == null) {
            throw new RuntimeException("NewMessageReceiver not inited");
        }
        return sInstance;
    }

    public void notificationsReceived(Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        if (action.equals(IpMessageConsts.MessageAction.ACTION_NEW_MESSAGE)) {
            long threadId = intent.getLongExtra(IpMessageConsts.MessageAction.KEY_THREAD_ID, 0);
            long msgId = intent.getLongExtra(IpMessageConsts.MessageAction.KEY_MSG_ID, 0);
            long ipMessageId = intent.getLongExtra(IpMessageConsts.MessageAction.KEY_IPMSG_ID, 0);
            RcsUtilsPlugin.unblockingNotifyNewIpMessage(mContext, threadId, msgId, ipMessageId);
            String number = intent.getStringExtra(IpMessageConsts.MessageAction.KEY_NUMBER);
            String chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, threadId);
            if (chatId != null) {
                //TODO: set member need to query profile
                PortraitManager.getInstance().updateMemberRequeryState(chatId, number, true);
            }
        } else if (action.equals(IpMessageConsts.GroupActionList.ACTION_GROUP_OPERATION_RESULT)) {
            int actionType = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, 0);
            int result = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_RESULT,
                                                IpMessageConsts.GroupActionList.VALUE_FAIL);
            long threadId = intent.getLongExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, 0);
            String chatId = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_CHAT_ID);
            Logger.d(TAG, "notificationsReceived, actionType=" + actionType + ", result=" + result);
            String body = null;
            switch (actionType) {
                case IpMessageConsts.GroupActionList.VALUE_INIT_GROUP:
                    String[] participants = intent.getStringArrayExtra(
                                            IpMessageConsts.GroupActionList.KEY_PARTICIPANT_LIST);
                    String contact = "";
                    for (String participant : participants) {
                        contact = contact + participant + ",";
                    }
                    contact = contact.substring(0, contact.length() - 1);
                    body = mPluginContext.getString(R.string.group_invite_to_group, contact);
                    break;
                case IpMessageConsts.GroupActionList.VALUE_ACCEPT_GROUP_INVITE:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_REJECT_GROUP_INVITE:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_ADD_PARTICIPANTS:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_REMOVE_PARTICIPANTS:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_TRANSFER_CHAIRMEN:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_MODIFY_NICK_NAME:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_MODIFY_SELF_NICK_NAME:
                    body = null;
                    break;
                case IpMessageConsts.GroupActionList.VALUE_MODIFY_SUBJECT:
                    String subject = intent.getStringExtra(
                                            IpMessageConsts.GroupActionList.KEY_SUBJECT);
                    body = mPluginContext.getString(R.string.group_subject_modified, subject);
                    break;
                case IpMessageConsts.GroupActionList.VALUE_EXIT_GROUP:
                    body = mPluginContext.getString(R.string.group_i_quit);
                    break;
                case IpMessageConsts.GroupActionList.VALUE_DESTROY_GROUP:
                    body = mPluginContext.getString(R.string.group_aborted);
                    break;
                default:
                    body = null;
                    break;
            }
            if (result == IpMessageConsts.GroupActionList.VALUE_SUCCESS && body != null) {
                addGroupSysMessage(chatId, body, false);
            }
        } else if (action.equals(IpMessageConsts.GroupActionList.ACTION_GROUP_NOTIFY)) {
            boolean invite = false;
            int actionType = intent.getIntExtra(IpMessageConsts.GroupActionList.KEY_ACTION_TYPE, 0);
            long threadId = intent.getLongExtra(IpMessageConsts.GroupActionList.KEY_THREAD_ID, 0);
            String chatId = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_CHAT_ID);
            Logger.d(TAG, "notificationsReceived, actionType=" + actionType + ", threadId="
                        + threadId + ", chatId=" + chatId);
            String stringArg = null;
            String body = null;
            Participant participant = null;
            switch (actionType) {
                case IpMessageConsts.GroupActionList.VALUE_NEW_INVITE_RECEIVED:
                    invite = true;
                    participant = intent.getParcelableExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT);
                    stringArg = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_SUBJECT);
                    if (intent.getBooleanExtra(IpMessageConsts.GroupActionList.KEY_ADD_SYS_EVENT, true)) {
                        body = mPluginContext.getString(R.string.group_invitation_received, participant.getDisplayName());
                    } else {
                        body = null;
                    }
                    break;
                case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_JOIN:
                    participant = intent.getParcelableExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT);
                    body = mPluginContext.getString(R.string.group_participant_join, participant.getDisplayName());
                    break;
                case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_LEFT:
                    participant = intent.getParcelableExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT);
                    body = mPluginContext.getString(R.string.group_participant_left, participant.getDisplayName());
                    break;
                case IpMessageConsts.GroupActionList.VALUE_PARTICIPANT_REMOVED:
                    participant = intent.getParcelableExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT);
                    body = mPluginContext.getString(R.string.group_participant_removed, participant.getDisplayName());
                    break;
                case IpMessageConsts.GroupActionList.VALUE_CHAIRMEN_CHANGED:
                    participant = intent.getParcelableExtra(IpMessageConsts.GroupActionList.KEY_PARTICIPANT);
                    if (PhoneNumberUtils.compare(participant.getContact(), RCSServiceManager.getInstance().getMyNumber())) {
                        body = mPluginContext.getString(R.string.group_i_become_chairmen);
                    } else {
                        body = mPluginContext.getString(R.string.group_chairmen_transferred, participant.getDisplayName());
                    }
                    break;
                case IpMessageConsts.GroupActionList.VALUE_SUBJECT_MODIFIED:
                    stringArg = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_SUBJECT);
                    body = mPluginContext.getString(R.string.group_subject_modified, stringArg);
                    break;
                case IpMessageConsts.GroupActionList.VALUE_ME_REMOVED:
                    stringArg = intent.getStringExtra(IpMessageConsts.GroupActionList.KEY_CONTACT_NUMBER);
                    body = mPluginContext.getString(R.string.group_me_removed, stringArg);
                    break;
                case IpMessageConsts.GroupActionList.VALUE_GROUP_ABORTED:
                    body = mPluginContext.getString(R.string.group_aborted);
                    break;
                default:
                    break;
            }
            if (body != null) {
                addGroupSysMessage(chatId, body, invite);
            }
            if (actionType == IpMessageConsts.GroupActionList.VALUE_NEW_INVITE_RECEIVED) {
                RcsComposeActivity rcsCompose = RcsComposeActivity.getRcsComposer();
                if (rcsCompose == null || !rcsCompose.processNewInvitation(threadId)) {
                    RcsMessagingNotification.updateNewGroupInvitation(participant, stringArg, threadId);
                }
            }
        } else if (action.equals(IpMessageConsts.MessageAction.ACTION_SEND_FAIL)) {
            processFailedMessage(intent);
        }
    }

    private void addGroupSysMessage(String chatId, String body, boolean invite) {
        Logger.d(TAG, "addGroupSysMessage, body=" + body);
        int msgType = invite ? Class.INVITATION : Class.SYSTEM;
        MessageStruct struct = new ReceiveMessageStruct(mContext, chatId, null, body,
                                        null, 0, msgType);
        struct.saveMessage();
    }

    @Override
    public void finalize() {
        Logger.d(TAG, "finalize() called");
        try {
            RCSServiceManager.getInstance().unregistNotifyListener(this);
            super.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Process failed item
     * @param intent Intent
     */
    private void processFailedMessage(Intent intent) {
        long msgId = intent.getLongExtra(IpMessageConsts.MessageAction.KEY_MSG_ID, 0);
        Cursor cursor = RCSDataBaseUtils.getMessage(mContext, msgId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                long threadId = cursor.getLong(cursor.getColumnIndex(MessageColumn.CONVERSATION));
                long ipmsgId = cursor.getLong(cursor.getColumnIndex(MessageColumn.IPMSG_ID));
                String body = cursor.getString(cursor.getColumnIndex(MessageColumn.BODY));
                int msgType = cursor.getInt(cursor.getColumnIndex(MessageColumn.CLASS));
                int subId = cursor.getInt(cursor.getColumnIndex(MessageColumn.SUB_ID));
                if (body.length() > MAX_LENGTH_OF_TRANSFER_TO_SMS_WHEN_SEND_FAILED ||
                        msgType != Class.NORMAL) {
                    RCSDataBaseUtils.updateMessageStatus(mContext, msgId, MessageStatus.FAILED);
                } else if (RcsMessageUtils.isNeedNotifyUserWhenToSms(mPluginContext, subId)) {
                    // notify
                    String address = cursor.getString(
                            cursor.getColumnIndex(MessageColumn.CONTACT_NUMBER));
                    Intent service = SendModeChangedNotifyService.createSmsNotifyIntent(msgId,
                            ipmsgId, threadId, subId, body, address);
                    mContext.startService(service);
                } else {
                    if (RcsMessageUtils.isTransferToSMSWhenSendFailed(mPluginContext, subId)) {
                        String address = cursor.getString(
                                cursor.getColumnIndex(MessageColumn.CONTACT_NUMBER));
                        RcsMessageUtils.transferToSMSFromFailedRcsMessage(mContext, threadId,
                                msgId, ipmsgId, subId, address, body);
                    } else {
                        // send failed.
                        RCSDataBaseUtils.updateMessageStatus(mContext, msgId, MessageStatus.FAILED);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
