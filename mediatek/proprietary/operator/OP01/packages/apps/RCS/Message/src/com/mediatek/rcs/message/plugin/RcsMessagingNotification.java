/*
 * Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are protected under
 * relevant copyright laws. The information contained herein is confidential and proprietary to
 * MediaTek Inc. and/or its licensors. Without the prior written permission of MediaTek inc. and/or
 * its licensors, any reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES THAT THE
 * SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED FROM MEDIATEK AND/OR ITS
 * REPRESENTATIVES ARE PROVIDED TO RECEIVER ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS
 * ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK
 * PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED
 * BY, INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO
 * SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT
 * IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN
 * MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE
 * TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM. RECEIVER'S SOLE
 * AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK
 * SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK
 * SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software") have been
 * modified by MediaTek Inc. All revisions are subject to any receiver's applicable license
 * agreements with MediaTek Inc.
 */
package com.mediatek.rcs.message.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.data.Contact;
import com.android.mms.data.WorkingMessage;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.MessagingNotification.NotificationInfo;

import com.mediatek.mms.ipmessage.DefaultIpMessagingNotificationExt;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.group.PortraitManager;
import com.mediatek.rcs.message.group.PortraitManager.GroupThumbnail;
import com.mediatek.rcs.message.utils.RcsMessageUtils;

import com.mediatek.rcs.common.IpGeolocMessage;
import com.mediatek.rcs.common.IpMessage;
import com.mediatek.rcs.common.IpMessageConsts;
import com.mediatek.rcs.common.IpImageMessage;
import com.mediatek.rcs.common.IpVCardMessage;
import com.mediatek.rcs.common.IpVideoMessage;
import com.mediatek.rcs.common.IpVoiceMessage;
import com.mediatek.rcs.common.RcsLog.Direction;
import com.mediatek.rcs.common.RcsLog.MessageColumn;
import com.mediatek.rcs.common.RcsLog.MessageStatus;
import com.mediatek.rcs.common.RcsLog.MessageType;
import com.mediatek.rcs.common.RcsLog.Class;
import com.mediatek.rcs.common.RCSMessageManager;
import com.mediatek.rcs.common.provider.MessageStruct;
import com.mediatek.rcs.common.provider.GroupChatCache;
import com.mediatek.rcs.common.provider.GroupChatCache.ChatInfo;
import com.mediatek.rcs.common.service.Participant;
import com.mediatek.rcs.common.utils.ContextCacher;

/**
 * Plugin implements. Response file is MessagingNotification in MMS host.
 *
 */
public class RcsMessagingNotification extends DefaultIpMessagingNotificationExt {
    private static final String TAG = "RcseMessagingNotification";
    private static final String NEW_MESSAGE_ACTION = "com.mediatek.mms.ipmessage.newMessage";
    private static int NEW_GROUP_INVITATION_NOTIFY_ID = 321;
    private static final String NOTIFICATION_RINGTONE = "pref_key_ringtone";
    private static final String NOTIFICATION_MUTE = "pref_key_mute";
    private static final String MUTE_START = "mute_start";
    private static final String DEFAULT_RINGTONE = "content://settings/system/notification_sound";

    private Context mContext;
    private Context mPluginContext;
    private IntentFilter mIntentFilter;

    public RcsMessagingNotification(Context pluginContext) {
        mPluginContext = pluginContext;
    }

    @Override
    public boolean IpMessagingNotificationInit(Context context) {
        mContext = context;
        return super.IpMessagingNotificationInit(context);
    }

    @Override
    public Bitmap getIpBitmap(long msgId, Cursor cursor) {
        IpMessage ipMessage = RCSMessageManager.getInstance().getRCSMessageInfo(msgId);
        Log.d(TAG, "getIpBitmap: ipMessage = " + ipMessage);
        if (null != ipMessage) {
            int ipMessageType = ipMessage.getType();
            if (ipMessageType == IpMessageConsts.IpMessageType.PICTURE) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                String filePath = ((IpImageMessage) ipMessage).getPath();
                return BitmapFactory.decodeFile(filePath);
            }
        }
        return null;
    }

    @Override
    public String getIpNotificationTitle(String number, long threadId, String title) {
        String chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, threadId);
        Log.d(TAG, "getIpNotificationTitle: chatId = " + chatId);
        if (!TextUtils.isEmpty(chatId)) {
            ChatInfo info = GroupChatCache.getInstance().getInfoByChatId(chatId);
            if (info != null) {
                String groupName = info.getNickName();
                if (TextUtils.isEmpty(groupName)) {
                    groupName = info.getSubject();
                }
                if (TextUtils.isEmpty(groupName)) {
                    groupName = title;
                }
                return groupName;
            } else {
                Log.e(TAG, "[getIpNotificationTitle]: group chat is exist, but chatInfo is null");
            }
        }
        return title;
    }

    public BitmapDrawable getIpNotificationDrawable(Context context, String number, long threadId,
            BitmapDrawable drawable) {
        String chatId = RcsMessageUtils.blockingGetGroupChatIdByThread(mContext, threadId);
        BitmapDrawable bitmapDrawable = drawable;
        if (!TextUtils.isEmpty(chatId)) {
            bitmapDrawable = PortraitManager.getGroupBitmapDrawable(context, chatId);
        }
        Log.d(TAG, "[getIpNotificationDrawable]: chatId = " + chatId + "drawable = " + drawable);
        return bitmapDrawable;
    }

    /**
     * Called when want to cancel group invitation notifications.
     * @param context Context
     */
    public static void cancelNewGroupInviations(Context context) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NEW_GROUP_INVITATION_NOTIFY_ID);
    }

    /**
     * Called when a group invitation arrived.
     * @param p inviter
     * @param subject  group's subject
     * @param threadId  thread id
     */
    public static void updateNewGroupInvitation(Participant p, String subject, long threadId) {
        asynUpdateNewGroupInvitation(p, subject, threadId);
    }

    private static void asynUpdateNewGroupInvitation(final Participant p, final String subject,
                                            final long threadId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Context hostContext = ContextCacher.getHostContext();
                Context pluginContext = ContextCacher.getPluginContext();
                int count = 0;
                boolean isCurrentThreadExist = false;
                if (hostContext != null) {
                    Cursor cursor = hostContext.getContentResolver().query(
                            RcsConversation.URI_CONVERSATION,
                            RcsConversation.PROJECTION_NEW_GROUP_INVITATION,
                            RcsConversation.SELECTION_NEW_GROUP_INVITATION_BY_STATUS,
                            null, null);
                    if (cursor != null) {
                        try {
                            count = cursor.getCount();
                            cursor.moveToFirst();
                            do {
                                long thread_id = cursor.getLong(0);
                                if (thread_id == threadId) {
                                    isCurrentThreadExist = true;
                                    break;
                                }
                            } while(cursor.moveToNext());
                        } catch (Exception e) {
                            // TODO: handle exception
                        } finally {
                            cursor.close();
                        }
                    }
                } else {
                    throw new RuntimeException("asynUpdateNewGroupInvitation, context is null");
                }
                if (!isCurrentThreadExist) {
                    count ++;
                }
                notifyNewGroupInvitation(p, subject, threadId, count);
            }
        }, "asynUpdateNewGroupInvitation").start();
    }

    private static void notifyNewGroupInvitation(final Participant p, final String subject,
            final long threadId, int count) {
        long timeMillis = System.currentTimeMillis();
        Context pluginContext = ContextCacher.getPluginContext();
        Context hostContext = ContextCacher.getHostContext();
        int smallIcon = RcsUtilsPlugin.getNotificationResourceId();
        if (smallIcon == 0) {
            return;
        }
        String contentTitle = null;
        String contentText = null;
        Bitmap largeIcon = BitmapFactory.decodeResource(pluginContext.getResources(),
                R.drawable.group_example);
        String tiker = subject + pluginContext.getString(R.string.group_invite_tiker);
        if (count == 1) {
            contentTitle = subject;
            if (TextUtils.isEmpty(contentTitle)) {
                contentTitle = pluginContext.getString(R.string.group_chat);
            }
            String invitee = p.getDisplayName();
            if (TextUtils.isEmpty(invitee)) {
                String number = p.getContact();
                if (!TextUtils.isEmpty(number)) {
                    Log.e(TAG, "[notifyNewGroupInvitation]: number is null");
                    Contact contact = Contact.get(number, false);
                    RcsContact rcsContact = (RcsContact) contact.getIpContact(hostContext);
                    invitee = rcsContact.getName();
                    if (TextUtils.isEmpty(invitee)) {
                        invitee = number;
                    }
//                    invitee = RcsUtilsPlugin.getContactNameByNumber(number);
                }
            }
            if (TextUtils.isEmpty(invitee)) {
                invitee = "Unknown";
            }
            contentText = pluginContext.getString(R.string.one_group_invite_content, invitee);
        } else {
            contentTitle = pluginContext.getString(R.string.group_chat);
            contentText = pluginContext.getString(R.string.more_group_invite_content, count);
        }
        int defaults = getNotificationDefaults(hostContext);
        //content intent
        Intent clickIntent = null;
        clickIntent = new Intent();
        clickIntent.setClassName(hostContext,
                          "com.android.mms.transaction.MessagingNotificationProxyReceiver");
        clickIntent.putExtra("thread_count", count);
        clickIntent.putExtra("thread_id", threadId);
        PendingIntent pIntent = PendingIntent.getBroadcast(hostContext, 0,
                                                clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // notify sound
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(hostContext);
        String muteStr = sp.getString(NOTIFICATION_MUTE, Integer.toString(0));
        long appMute = Integer.parseInt(muteStr);
        long appMuteStart = sp.getLong(MUTE_START, 0);
        if (appMuteStart > 0 && appMute > 0) {
            long currentTime = (System.currentTimeMillis() / 1000);
            if ((appMute * 3600 + appMuteStart / 1000) <= currentTime) {
                appMute = 0;
                appMuteStart = 0;
                SharedPreferences.Editor editor =
                                PreferenceManager.getDefaultSharedPreferences(hostContext).edit();
                editor.putLong(MUTE_START, 0);
                editor.putString(NOTIFICATION_MUTE, String.valueOf(appMute));
                editor.apply();
            }
        }
        Uri ringtone = null;
        if (appMute == 0) {
            String ringtoneStr = sp.getString(NOTIFICATION_RINGTONE, null);
            ringtoneStr = checkRingtone(hostContext, ringtoneStr);
            ringtone = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
        }
        final Notification.Builder notifBuilder = new Notification.Builder(hostContext)
        .setWhen(timeMillis)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setLargeIcon(largeIcon)
        .setDefaults(defaults)
        .setContentIntent(pIntent)
        .setSmallIcon(smallIcon)
        .setSound(ringtone);

        Notification notification = notifBuilder.build();
        NotificationManager nm = (NotificationManager)
                hostContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NEW_GROUP_INVITATION_NOTIFY_ID, notification);
    }

    private static final String checkRingtone(Context context, String ringtoneUri) {
        if (!TextUtils.isEmpty(ringtoneUri)) {
            InputStream inputStream = null;
            boolean invalidRingtone = true;
            try {
                inputStream = context.getContentResolver().openInputStream(Uri.parse(ringtoneUri));
            } catch (FileNotFoundException ex) {
            } finally {
                if (inputStream != null) {
                    invalidRingtone = false;
                    try {
                        inputStream.close();
                    } catch (IOException ex) {
                    }
                }
            }
            if (invalidRingtone) {
                ringtoneUri = DEFAULT_RINGTONE;
            }
        }
        return ringtoneUri;
    }

    protected static void processNotificationSound(Context context, Notification notification,
                                                        Uri ringtone) {
        int state = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                                                        .getCallState();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            /* vibrate on */
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.sound = ringtone;
    }

    protected static int getNotificationDefaults(Context context) {
        int defaults = 0;
        int state = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                                                                                   .getCallState();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            /* vibrate on */
            defaults |= Notification.DEFAULT_VIBRATE;
        }
        defaults |= Notification.DEFAULT_LIGHTS;
        return defaults;
    }

    private static final String[] RCS_STATUS_PROJECTION = new String[] {
        MessageColumn.ID,
        MessageColumn.CONVERSATION,
        MessageColumn.DATE_SENT,
        MessageColumn.CONTACT_NUMBER,
        MessageColumn.TYPE,
        MessageColumn.CLASS,
        MessageColumn.BODY,
        MessageColumn.IPMSG_ID,
        MessageColumn.MIME_TYPE,
        MessageColumn.FILE_PATH
    };

    private static final int COLUMN_ID          = 0;
    private static final int COLUMN_THREAD_ID   = 1;
    private static final int COLUMN_DATE        = 2;
    private static final int COLUMN_ADDRESS     = 3;
    private static final int COLUMN_TYPE        = 4;
    private static final int COLUMN_CLASS       = 5;
    private static final int COLUMN_BODY        = 6;
    private static final int COLUMN_IPMSG_ID    = 7;
    private static final int COLUMN_MIME_TYPE   = 8;
    private static final int COLUMN_FILE_PATH   = 9;
    private static final String NEW_INCOMING_RCS_CONSTRAINT =
            "(" + MessageColumn.DIRECTION + " = " + Direction.INCOMING
            + " AND " + MessageColumn.SEEN + " = 0"
            + " AND " + MessageColumn.MESSAGE_STATUS + " = " + MessageStatus.UNREAD + ")";
    private static final int MAX_MESSAGES_TO_SHOW = 8;

    @Override
    public int blockingUpdateNewMessageIndicator(Context context, Set<Long> threads,
            SortedSet notifSet, Object objectLock) {
        SortedSet<NotificationInfo> notificationSet = (SortedSet<NotificationInfo>) notifSet;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MessageColumn.CONTENT_URI,
                                        RCS_STATUS_PROJECTION,
                                        NEW_INCOMING_RCS_CONSTRAINT,
                                        null, MessageColumn.DATE_SENT + " desc");

        if (cursor == null) {
            return 0;
        }

        int addNotifyCount = 0;
        Set<Long> rcsThreads = new HashSet<Long>(4);
        try {
            int count = 0;
            while (cursor.moveToNext()) {

                long threadId = cursor.getLong(COLUMN_THREAD_ID);
                long ipMessageId = cursor.getLong(COLUMN_IPMSG_ID);
                if (ipMessageId == 0) {
                    continue;
                }
                count ++;
                 /*
                 * our Notification show has 3 case:
                 * 1. 1 thread and 1 item => show the item
                 * 2. 1 thread and many items => show all items of this thread
                 * 3. many threads => show 1 item for every thread
                 * But total items can show out is <= MAX_MESSAGES_TO_SHOW
                 */
                if (rcsThreads.size() > MAX_MESSAGES_TO_SHOW) {
                    //has enough item of many thread, no need check cursor anymore
                    break;
                } else if (rcsThreads.contains(threadId) && rcsThreads.size() > 1) {
                    //threads count > 1, only need 1 item for every thread
                    continue;
                } else if (rcsThreads.contains(threadId) && addNotifyCount > MAX_MESSAGES_TO_SHOW) {
                    //only 1 thread, but has enough items for this thread
                    continue;
                }

                String address = cursor.getString(COLUMN_ADDRESS);

                Contact contact = Contact.get(address, false);
                if (contact.getSendToVoicemail()) {
                    // don't notify, skip this one
                    continue;
                }

                String message = getMsgContent(mPluginContext, cursor);
                long timeMillis = cursor.getLong(COLUMN_DATE);
                int msgId = cursor.getInt(COLUMN_ID);
                Bitmap attachmentBitmap = getAttachmentBitmap(cursor);
                Uri uri = MessageColumn.CONTENT_URI.buildUpon()
                            .appendPath(Long.toString(cursor.getLong(COLUMN_ID)))
                            .build();
                NotificationInfo info = MessagingNotification.getNewMessageNotificationInfo(context,
                        false/* isSms */, address, message, null /* subject */, threadId,
                        timeMillis, attachmentBitmap /* attachmentBitmap */, contact,
                        WorkingMessage.TEXT, uri);
                synchronized (objectLock) {
                    notificationSet.add(info);
                }
                addNotifyCount ++;

                threads.add(threadId);
//                threads.add(cursor.getLong(COLUMN_THREAD_ID));
                rcsThreads.add(threadId);
            }
            return count;
        } finally {
            cursor.close();
        }
    }

    private Bitmap getAttachmentBitmap(Cursor cursor) {
        int msg_type = cursor.getInt(COLUMN_CLASS);
        int categoryType = cursor.getInt(COLUMN_TYPE);
        String mimeType = cursor.getString(COLUMN_MIME_TYPE);
        if (msg_type == Class.NORMAL && categoryType == MessageType.FT
                && mimeType != null && mimeType.contains("image")) {
            String filePath = cursor.getString(COLUMN_FILE_PATH);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            return BitmapFactory.decodeFile(filePath);
        }
        return null;
    }

    private String getMsgContent(Context context, Cursor cursor) {
        int msg_type = cursor.getInt(COLUMN_CLASS);
        if (msg_type == Class.BURN) {
            return context.getString(R.string.menu_burned_msg);
        } else if (msg_type == Class.EMOTICON) {
            return context.getString(R.string.emoticons);
        } else if (msg_type == Class.NORMAL) {
            int categoryType = cursor.getInt(COLUMN_TYPE);
            if (categoryType == MessageType.IM) {
                String body = cursor.getString(COLUMN_BODY);
                return body;
            } else if (categoryType == MessageType.FT) {
                String mimeType = cursor.getString(COLUMN_MIME_TYPE);
                if (TextUtils.isEmpty(mimeType)) {
                    Log.e(TAG, "[getMsgContent]: mime type is empty");
                    return "";
                }
                if (mimeType.contains("image")) {
                    return context.getString(R.string.ft_snippet_image);
                } else if (mimeType.contains("audio")
                        || mimeType.contains("application/ogg")) {
                    return context.getString(R.string.ft_snippet_voice);
                } else if (mimeType.contains("video")) {
                    return context.getString(R.string.ft_snippet_video);
                } else if (mimeType.contains("text/x-vcard")) {
                    return context.getString(R.string.ft_snippet_vcard);
                } else if (mimeType.contains("application/vnd.gsma.rcspushlocation+xml")) {
                    return context.getString(R.string.ft_snippet_geolocation);
                }
            }
        }
        return "";
    }

    private static String[] RCS_THREADS_PROJECTIONS = new String[] {
        MessageColumn.CONVERSATION
    };
    private static String RCS_UNDELIVERED_SELECTION =
                    "(" + MessageColumn.MESSAGE_STATUS + "=" + MessageStatus.FAILED + ")";

    @Override
    public int getUndeliveredMessageCount(Context context, long[] threadIdResult) {
        Cursor cursor = context.getContentResolver().query(MessageColumn.CONTENT_URI,
                                                            RCS_THREADS_PROJECTIONS,
                                                            RCS_UNDELIVERED_SELECTION,
                                                            null, null);
        if (cursor == null) {
            return 0;
        }
        int count = cursor.getCount();
        try {
            if (threadIdResult != null && cursor.moveToFirst()) {
                threadIdResult[0] = cursor.getLong(0);

                if (threadIdResult.length >= 2) {
                    // Test to see if all the undelivered messages belong to the same thread.
                    long firstId = threadIdResult[0];
                    while (cursor.moveToNext()) {
                        if (cursor.getLong(0) != firstId) {
                            firstId = 0;
                            break;
                        }
                    }
                    threadIdResult[1] = firstId;    // non-zero if all ids are the same
                }
            }
        } finally {
            cursor.close();
        }
        return count;
    }
}