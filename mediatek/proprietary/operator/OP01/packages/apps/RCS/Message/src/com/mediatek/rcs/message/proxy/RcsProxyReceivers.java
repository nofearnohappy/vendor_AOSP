package com.mediatek.rcs.message.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;

import com.mediatek.mms.ipmessage.DefaultIpEmptyReceiverExt;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.service.IRCSChatServiceListener;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.Logger;

/**
 * RcsProxyReceivers.
 *
 */
public class RcsProxyReceivers extends DefaultIpEmptyReceiverExt {

    private static final String TAG = "RcsProxyReceivers";

    @Override
    public void onReceive(BroadcastReceiver receiver, final Context context, final Intent intent) {
        //
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                asyncOnReceive(context, intent);
                return null;
            }
        }.execute();
    }

    private void asyncOnReceive(Context context, Intent intent) {
        GroupChatCache.createInstance(context);
        String action = intent.getAction();
        Logger.v(TAG, "asyncOnReceive() entry, the action is " + action);
        String chatId = intent.getStringExtra(IpMessageConsts.ServiceNotification.KEY_CHAT_ID);
        long smsId = intent.getLongExtra(IpMessageConsts.ServiceNotification.KEY_MSG_ID, 0);
        String contact = intent.getStringExtra(IpMessageConsts.ServiceNotification.KEY_CONTACT);
        IRCSChatServiceListener listener = null;
        while (listener == null) {
            listener = RCSServiceManager.getInstance().getServiceListener();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            if (action.equalsIgnoreCase(
                                IpMessageConsts.ServiceNotification.BROADCAST_RCS_NEW_MESSAGE)) {
                listener.onNewMessage(smsId);
            } else if (action.equalsIgnoreCase(
                    IpMessageConsts.ServiceNotification.BROADCAST_RCS_GROUP_NEW_MESSAGE)) {
                listener.onNewGroupMessage(chatId, smsId, contact);
            } else if (action.equalsIgnoreCase(
                    IpMessageConsts.ServiceNotification.BROADCAST_RCS_GROUP_BEEN_KICKED_OUT)) {
                listener.onMeRemoved(chatId, contact);
            } else if (action.equalsIgnoreCase(
                    IpMessageConsts.ServiceNotification.BROADCAST_RCS_GROUP_ABORTED)) {
                listener.onAbort(chatId);
            } else if (action.equalsIgnoreCase(
                    IpMessageConsts.ServiceNotification.BROADCAST_RCS_GROUP_INVITATION)) {
                String subject = intent.getStringExtra(
                                        IpMessageConsts.ServiceNotification.KEY_SUBJECT);
                Participant participant = (Participant) intent.getParcelableExtra(
                                            IpMessageConsts.ServiceNotification.KEY_PARTICIPANT);
                listener.onNewInvite(participant, subject, chatId);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
