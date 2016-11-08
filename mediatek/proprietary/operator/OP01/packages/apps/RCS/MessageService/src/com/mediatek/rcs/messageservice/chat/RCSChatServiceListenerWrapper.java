package com.mediatek.rcs.messageservice.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.utils.Logger;

import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.service.Participant;

public class RCSChatServiceListenerWrapper extends IRCSChatServiceListener.Stub implements
        IBinder.DeathRecipient {
    public static final String TAG = "RCSChatServiceListenerWrapper";

    IRCSChatServiceListener mListenerImpl = null;
    Handler mHandler = null;
    Context mContext = null;

    RCSChatServiceListenerWrapper(Context context, Handler notifyHandler) {
        mContext = context;
        mHandler = notifyHandler;
    }

    void addListener(IRCSChatServiceListener listener) {
        Logger.d(TAG, "addListener:" + listener);
        mListenerImpl = listener;
    }

    void removeListener(IRCSChatServiceListener listener) {
        Logger.d(TAG, "removeListener:" + listener);
        if (mListenerImpl == listener) {
            mListenerImpl = null;
        }
    }

    IRCSChatServiceListener getListener() {
        return mListenerImpl;
    }

    @Override
    public void binderDied() {
        Logger.d(TAG, "RCS Plug in App binderDied");
        mListenerImpl = null;
    }

    @Override
    public void onNewMessage(final long msgId) {
        Logger.d(TAG, "onNewMessage:" + msgId);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onNewMessage(msgId);
                    } else {
                        Intent intent = new Intent(
                                IpMessageConsts.ServiceNotification.BROADCAST_RCS_NEW_MESSAGE);
                        intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyReceiver");
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_MSG_ID, msgId);
                        mContext.sendBroadcast(intent);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onNewGroupMessage(final String chatId, final long msgId, final String number) {
        Logger.d(TAG, "onNewGroupMessage chatId/msgId: " + chatId + "/" + msgId);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onNewGroupMessage(chatId, msgId, number);
                    } else {
                        Intent intent = new Intent(
                                IpMessageConsts.ServiceNotification.
                                BROADCAST_RCS_GROUP_NEW_MESSAGE);
                        intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyReceiver");
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_CHAT_ID, chatId);
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_MSG_ID, msgId);
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_CONTACT, number);
                        mContext.sendBroadcast(intent);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSendO2OMessageFailed(final long msgId) {
        Logger.d(TAG, "onSendO2OMessageFailed:" + msgId);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onSendO2OMessageFailed(msgId);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSendO2MMessageFailed(final long msgId) {
        Logger.d(TAG, "onSendO2MMessageFailed:" + msgId);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onSendO2MMessageFailed(msgId);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRequestBurnMessageCapabilityResult(final String contact, final boolean result) {
        Logger.d(TAG, "onRequestBurnMessageCapabilityResult:" + contact);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onRequestBurnMessageCapabilityResult(contact, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSendGroupMessageFailed(final long msgId) {
        Logger.d(TAG, "onSendGroupMessageFailed:" + msgId);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onSendGroupMessageFailed(msgId);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onParticipantJoined(final String chatId, final Participant participant) {
        Logger.d(TAG, "onParticipantJoined: #" + chatId + " ,#" + participant);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onParticipantJoined(chatId, participant);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onParticipantLeft(final String chatId, final Participant participant) {
        Logger.d(TAG, "onParticipantLeft: #" + chatId + " ,#" + participant);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onParticipantLeft(chatId, participant);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onParticipantRemoved(final String chatId, final Participant participant) {
        Logger.d(TAG, "onParticipantRemoved: #" + chatId + " ,#" + participant);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onParticipantRemoved(chatId, participant);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onChairmenChanged(final String chatId, final Participant participant,
            final boolean isMe) {
        Logger.d(TAG, "onChairmenChanged: #" + chatId + " ,#" + participant);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onChairmenChanged(chatId, participant, isMe);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSubjectModified(final String chatId, final String subject) {
        Logger.d(TAG, "onSubjectModified: #" + chatId + " ,#" + subject);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onSubjectModified(chatId, subject);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onParticipantNickNameModified(final String chatId, final String contact,
            final String nickName) {
        Logger.d(TAG, "onParticipantNickNameModified: #" + chatId + " ,#" + contact);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onParticipantNickNameModified(chatId, contact, nickName);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onMeRemoved(final String chatId, final String contact) {
        Logger.d(TAG, "onMeRemoved: #" + chatId + " ,#" + contact);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onMeRemoved(chatId, contact);
                    } else {
                        Intent intent = new Intent(
                                IpMessageConsts.ServiceNotification.
                                BROADCAST_RCS_GROUP_BEEN_KICKED_OUT);
                        intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyReceiver");
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_CHAT_ID, chatId);
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_CONTACT, contact);
                        mContext.sendBroadcast(intent);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAbort(final String chatId) {
        Logger.d(TAG, "onAbort: #" + chatId);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onAbort(chatId);
                    } else {
                        Intent intent = new Intent(
                                IpMessageConsts.ServiceNotification.BROADCAST_RCS_GROUP_ABORTED);
                        intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyReceiver");
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_CHAT_ID, chatId);
                        mContext.sendBroadcast(intent);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onNewInvite(
            final Participant participant, final String subject, final String chatId) {
        Logger.d(TAG, "onNewInvite:" + chatId);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onNewInvite(participant, subject, chatId);
                    } else {
                        Intent intent = new Intent(
                                IpMessageConsts.ServiceNotification.BROADCAST_RCS_GROUP_INVITATION);
                        intent.setClassName("com.android.mms", "com.mediatek.rcs.EmptyReceiver");
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_CHAT_ID, chatId);
                        intent.putExtra(IpMessageConsts.ServiceNotification.KEY_SUBJECT, subject);
                        intent.putExtra(
                                IpMessageConsts.ServiceNotification.KEY_PARTICIPANT, participant);
                        mContext.sendBroadcast(intent);
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onInvitationTimeout(final String chatId) {
        Logger.d(TAG, "onInvitationTimeout:" + chatId);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onInvitationTimeout(chatId);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAddParticipantFail(final Participant participant, final String chatId) {
        Logger.d(TAG, "onAddParticipantFail: #" + chatId + " ,#" + participant);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onAddParticipantFail(participant, chatId);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAddParticipantsResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onAddParticipantsResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onAddParticipantsResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRemoveParticipantsResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onRemoveParticipantsResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onRemoveParticipantsResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onTransferChairmenResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onTransferChairmenResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onTransferChairmenResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onMyNickNameModifiedResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onMyNickNameModifiedResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onMyNickNameModifiedResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSubjectModifiedResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onSubjectModifiedResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onSubjectModifiedResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onQuitConversationResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onQuitConversationResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onQuitConversationResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAbortResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onAbortResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onAbortResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onInitGroupResult(final boolean result, final String chatId) {
        Logger.d(TAG, "onInitGroupResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onInitGroupResult(result, chatId);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onAcceptInvitationResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onAcceptInvitationResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onAcceptInvitationResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRejectInvitationResult(final String chatId, final boolean result) {
        Logger.d(TAG, "onRejectInvitationResult: #" + chatId + " ,#" + result);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onRejectInvitationResult(chatId, result);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onUpdateFileTransferStatus(final long ipMsgId, final int stat, final int status) {
        Logger.d(TAG, "onUpdateFileTransferStatus: #" + ipMsgId + " ,#" + stat);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.onUpdateFileTransferStatus(ipMsgId, stat, status);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void setFilePath(final long ipMsgId, final String filePath) {
        Logger.d(TAG, "setFilePath: #" + ipMsgId + " ,#" + filePath);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mListenerImpl != null) {
                        mListenerImpl.setFilePath(ipMsgId, filePath);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void handleSpamReportResult(
            final String contact, final String msgId, final int errorcode) {
        Logger.d(TAG, " [spam-report] handleSpamReportResult: #" +
                contact + " ,#" + msgId + " ,#" + errorcode);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mListenerImpl.handleSpamReportResult(contact, msgId, errorcode);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void handleFileSpamReportResult(
            final String contact, final String ftId, final int errorcode) {
        Logger.d(TAG, " [spam-report] handleFileSpamReportResult: #" +
                contact + " ,#" + ftId + " ,#" + errorcode);
        if (mListenerImpl == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mListenerImpl.handleFileSpamReportResult(contact, ftId, errorcode);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
