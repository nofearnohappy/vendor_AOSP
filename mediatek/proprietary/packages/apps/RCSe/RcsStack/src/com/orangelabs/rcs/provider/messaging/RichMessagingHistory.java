/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.provider.messaging;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.List;

import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatLog.Message.Type;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferLog;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.service.api.PauseResumeFileObject;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Rich messaging history for chats and file transfers.
 *
 * @author Jean-Marc AUFFRET
 */
public class RichMessagingHistory {
    /**
     * Current instance
     */
    private static RichMessagingHistory instance = null;

    /**
     * Content resolver
     */
    private ContentResolver cr;

    /**
     * Chat database URI
     */
    private Uri chatDatabaseUri = ChatData.CONTENT_URI;

    /**
     * Message database URI
     */
    private Uri msgDatabaseUri = MessageData.CONTENT_URI;

    /*
     * GROUP MEMBER DATABASE URI
     */
    private Uri grpMemberDatabaseUri = GroupMemberData.CONTENT_URI;

    //URI for adding conversation id column in the database in case of CPM
    private final Uri alterChatDatabaseUri = Uri.parse(
            "content://com.orangelabs.rcs.chat/add_chat_conv_id");
    private final Uri alterMsgDatabaseUri = Uri.parse(
            "content://com.orangelabs.rcs.chat/add_msg_conv_id");

    /**
     * File transfer database URI
     */
    private Uri ftDatabaseUri = FileTransferData.CONTENT_URI;

    /**
     * Multi File transfer database URI
     */
    private Uri multiFtDatabaseUri = MultiFileTransferData.CONTENT_URI;

    private static final String SELECTION_FILE_BY_FT_ID =
            new StringBuilder(FileTransferData.KEY_FT_ID).append("=?").toString();
    /**
     * Multi participant message  database URI
     */
    private Uri multiParticipantDatabaseUri = MultiMessageData.CONTENT_URI;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Create instance
     *
     * @param ctx Context
     */
    public static synchronized void createInstance(Context ctx) {
        if (instance == null) {
            instance = new RichMessagingHistory(ctx);
        }
    }

    /**
     * Returns instance
     *
     * @return Instance
     */
    public static RichMessagingHistory getInstance() {
        return instance;
    }

    /**
     * Constructor
     *
     * @param ctx Application context
     */
    private RichMessagingHistory(Context ctx) {
        super();

        this.cr = ctx.getContentResolver();

        if(true) {
            addColumnConversationID();
        }
    }

    /*--------------------- Group chat methods -----------------------*/

    /**
     * Add group chat session
     *
     * @param chatId Chat ID
     * @param subject Subject
     * @param participants List of participants
     * @param status Status
     * @param direction Direction
     */
    public void addGroupChat(
            String chatId, String subject, List<String> participants, int status, int direction) {
        if (logger.isActivated()) {
            logger.debug("Add group chat entry: chatID=" + chatId);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_CHAT_ID, chatId);
        values.put(ChatData.KEY_STATUS, status);
        values.put(ChatData.KEY_SUBJECT, subject);
        values.put(ChatData.KEY_PARTICIPANTS, RichMessagingHistory.getParticipants(participants));
        values.put(ChatData.KEY_DIRECTION, direction);
        values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        cr.insert(chatDatabaseUri, values);
    }

    /**
     * Add group member detail for a chat
     *
     */
    public void addGroupMember(
            String chatId, String memberName, String memberNumber, String eType) {
        if (logger.isActivated()){
            logger.debug("Add group member: chatID=" + chatId
                    + "  and memeber : " + memberName + " and memberNumber : " + memberNumber);
        }
        ContentValues values = new ContentValues();
        values.put(GroupMemberData.KEY_CHAT_ID, chatId);
        values.put(GroupMemberData.KEY_CONTACT_NUMBER, memberNumber);
        values.put(GroupMemberData.KEY_MEMBER_NAME, memberName);
        values.put(GroupMemberData.KEY_CONTACT_TYPE, eType);

        cr.insert(grpMemberDatabaseUri, values);
    }

    public void removeGroupMember(String chatId, String memberNumber) {

        if (logger.isActivated()) {
            logger.debug("removeGroupMember() : for chatId = "+ chatId
                    + "; and memberNumber : "+memberNumber);
        }
        // Delete entries
        int deletedRows = cr.delete(
                grpMemberDatabaseUri,
                GroupMemberData.KEY_CHAT_ID+ " = '" + chatId +"' AND " +
                        GroupMemberData.KEY_CONTACT_NUMBER+ " = '" + memberNumber + "'",
                null);

    }

    public void updateGroupMemberEtype(String chatId, String memberNumber, String eType){
        if (logger.isActivated()) {
            logger.debug("updateGroupMemberEtype for  chat id : " + chatId
                    + "; member_number : "+ memberNumber + " ; etype : " + eType);
        }
        ContentValues values = new ContentValues();
        values.put(GroupMemberData.KEY_CONTACT_TYPE, eType);
        cr.update(grpMemberDatabaseUri,
                values,
                GroupMemberData.KEY_CHAT_ID + " = '" + chatId +
                "'  AND " +
                GroupMemberData.KEY_CONTACT_NUMBER + " = '"+memberNumber+"'",
                null);
    }

    public void updateGroupMemberName(String chatId, String memberNumber, String memberName){
        if (logger.isActivated()) {
            logger.debug("updateGroupMemberName for  chat id : " + chatId
                    + "; member_number : "+ memberNumber + " ; memberName : " + memberName);
        }
        ContentValues values = new ContentValues();
        values.put(GroupMemberData.KEY_MEMBER_NAME, memberName);
        cr.update(grpMemberDatabaseUri,
                values,
                GroupMemberData.KEY_CHAT_ID + " = '" + chatId +
                "'  AND " +
                GroupMemberData.KEY_CONTACT_NUMBER + " = '"+memberNumber+"'",
                null);
    }

    /**
     * Get list of participants into a string
     *
     * @param participants List of participants
     * @return String (contacts are comma separated)
     */
    private static String getParticipants(List<String> participants) {
        StringBuffer result = new StringBuffer();
        for (String contact : participants) {
            if (contact != null) {
                result.append(PhoneUtils.extractNumberFromUri(contact) + ";");
            }
        }
        return result.toString();
    }

    /**
     * Get list of participants from a string
     *
     * @param String participants (contacts are comma separated)
     * @return String[] contacts or null if
     */
    private static List<String> getParticipants(String participants) {
        ArrayList<String> result = new ArrayList<String>();
        if (participants != null && participants.trim().length() > 0) {
            String[] items = participants.split(";", 0);
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    result.add(items[i]);
                }
            }
        }
        return result;
    }

    /**
     * Get list of participants from a string
     *
     * @param String participants (contacts are comma separated)
     * @return String[] contacts or null if
     */
    private static List<String> getMultiFileParticipants(String participants) {
        ArrayList<String> result = new ArrayList<String>();
        if (participants != null && participants.trim().length() > 0) {
            String[] items = participants.split(",", 0);
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    result.add(items[i]);
                }
            }
        }
        return result;
    }

    /**
     * Update group chat status
     *
     * @param chatId Chat ID
     * @param status Status
     */
    public void updateGroupChatStatus(String chatId, int status) {
        if (logger.isActivated()) {
            logger.debug("Update group chat status to " + status);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_STATUS, status);
        cr.update(chatDatabaseUri,
                values,
                ChatData.KEY_CHAT_ID + " = '" + chatId + "'",
                null);
    }

    /**
     * Update group chat rejoin ID
     *
     * @param chatId Chat ID
     * @param rejoingId Rejoin ID
     * @param status Status
     */
    public void updateGroupChatRejoinId(String chatId, String rejoingId) {
        if (logger.isActivated()) {
            logger.debug("Update group chat rejoin ID to " + rejoingId);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_REJOIN_ID, rejoingId);
        cr.update(chatDatabaseUri,
                values,
                ChatData.KEY_CHAT_ID + " = '" + chatId + "'",
                null);
    }

    /**
     * Get the group chat info
     *
     * @param chatId Chat ID
     * @result Group chat info
     */
    public GroupChatInfo getGroupChatInfo(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info for " + chatId);
        }
        GroupChatInfo result = null;
        Cursor cursor = cr.query(chatDatabaseUri,
                new String[] {
                    ChatData.KEY_CHAT_ID,
                    ChatData.KEY_REJOIN_ID,
                    ChatData.KEY_PARTICIPANTS,
                    ChatData.KEY_SUBJECT
                },
                "(" + ChatData.KEY_CHAT_ID + "='" + chatId + "')",
                null,
                ChatData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String participants = cursor.getString(2);
            List<String> list = RichMessagingHistory.getParticipants(participants);
            result = new GroupChatInfo(
                    cursor.getString(0),
                    cursor.getString(1),
                    chatId,
                    list,
                    cursor.getString(3));
        }
        cursor.close();
        return result;
    }

    /**
     * Get the group chat info
     *
     * @param chatId Chat ID
     * @result Group chat info
     */
    public GroupChatInfo getGroupChatInfoByRejoinId(String rejoinId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info by RejoinId for " + rejoinId);
        }
        GroupChatInfo result = null;
        Cursor cursor = cr.query(chatDatabaseUri,
                new String[] {
                    ChatData.KEY_CHAT_ID,
                    ChatData.KEY_REJOIN_ID,
                    ChatData.KEY_PARTICIPANTS,
                    ChatData.KEY_SUBJECT
                },
                "(" + ChatData.KEY_REJOIN_ID + "='" + rejoinId + "')",
                null,
                ChatData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String participants = cursor.getString(2);
            List<String> list = RichMessagingHistory.getParticipants(participants);
            result = new GroupChatInfo(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(0),
                    list,
                    cursor.getString(3));
        }
        cursor.close();
        return result;
    }

    /**
     * Get chat id by rejoin id
     * @param rejoin
     *            id
     * @result chat id
     */
    public String getChatIdbyRejoinId(String rejoinId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat id by: " + rejoinId);
        }
        String chatId = null;
        Cursor cursor = cr.query(chatDatabaseUri,
                new String[] {
                ChatData.KEY_CHAT_ID,
                },
                "(" + ChatData.KEY_REJOIN_ID + "='" + rejoinId + "')",
                null,
                ChatData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            chatId = cursor.getString(cursor.getColumnIndex(ChatData.KEY_CHAT_ID));
        }
        cursor.close();
        return chatId;
    }

    /**
     * Get the group member info
     *
     * @param chatId Chat ID
     * @result Group member info
     */
    public boolean isGroupMemberExist(String chatId, String contact) {
        if (logger.isActivated()) {
            logger.debug("Get group member exist  for " + chatId);
        }
        Cursor cursor = cr.query(grpMemberDatabaseUri,
                new String[] {
                    GroupMemberData.KEY_CHAT_ID
                },
                "(" + GroupMemberData.KEY_CHAT_ID + "='" + chatId + "')"
                        + " AND (" + GroupMemberData.KEY_CONTACT_NUMBER + "=" + contact + ")",
                null,
                null);
        if (cursor == null) {
            return false;
        }

        if ((cursor.moveToFirst())) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }


	/**
     * Get the group chat info
     *
     * @param chatId Chat ID
     * @result Group chat info
     */
    public int getMessageStatus(String msgId) {
        if (logger.isActivated()) {
            logger.debug("getMessageStatus " + msgId);
        }
        int result = ChatLog.Message.Status.Content.FAILED;
        Cursor cursor = cr.query(msgDatabaseUri,
                new String[] {
                    MessageData.KEY_MSG_ID,
                    MessageData.KEY_STATUS
                },
                "(" + MessageData.KEY_MSG_ID + "='" + msgId + "')",
                null,
                ChatData.KEY_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(0);
            result = status;
        }
        cursor.close();
        return result;
    }

    public String getMessageText(String msgId) {
        if (logger.isActivated()) {
            logger.debug("getMessageText " + msgId);
        }
        String result = null;
        Cursor cursor = cr.query(msgDatabaseUri,
                new String[] {
                    MessageData.KEY_MSG_ID,
                    MessageData.KEY_CONTENT
                },
                "(" + MessageData.KEY_MSG_ID + "='" + msgId + "')",
                null,
                ChatData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            byte[] blobText = null;
            blobText = cursor.getBlob(1);

            if (blobText != null) {
                String status = new String(blobText);
                result = status;
            }
        }

        cursor.close();
        return result;
    }

    /**
     * IS GROUPCHAT EXISTS
     *
     * @param chatId Chat ID
     * @result boolean status
     */
    public boolean isGroupChatExists(String chatId) {

        if (logger.isActivated()) {
            logger.debug("isGroupChatExists for : " + chatId);
        }
        boolean result = false;
        //Cursor cursor = cr.query(chatDatabaseUri,
        //new String[] {
        //ChatData.KEY_CHAT_ID
        //},
        ///"(" + ChatData.KEY_CHAT_ID + "='" + chatId + "')",
        //null,
        //ChatData.KEY_TIMESTAMP + " DESC");

        Cursor cursor = cr.query(chatDatabaseUri,
                new String[] { ChatData.KEY_CHAT_ID },
                "( " + ChatData.KEY_CHAT_ID + " = '" + chatId + "')",
                null, null);

        if ((cursor!=null) && (cursor.moveToFirst())) {
            if (logger.isActivated()) {
                logger.info("isGroupChatExists for : returns true");
            }
            result = true;
            cursor.close();
        }

        return result;
    }

    /**
     * Get the group chat participants who have been connected to the chat
     *
     * @param chatId Chat ID
     * @result List of contacts
     */
    public List<String> getGroupChatConnectedParticipants(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get connected participants for " + chatId);
        }
        List<String> result = new ArrayList<String>();
        Cursor cursor =
                cr.query(
                        msgDatabaseUri,
                        new String[] {
                                MessageData.KEY_CONTACT
                        },
                        "(" + MessageData.KEY_CHAT_ID + "='" + chatId + "') AND ("
                            + MessageData.KEY_TYPE + "=" + ChatLog.Message.Type.SYSTEM + ")",
                        null,
                        MessageData.KEY_TIMESTAMP + " DESC");
        while (cursor.moveToNext()) {
            String participant = cursor.getString(0);
            if ((participant != null) && (!result.contains(participant))) {
                result.add(participant);
            }
        }
        cursor.close();
        return result;
    }

    /*--------------------- Chat messages methods -----------------------*/

    /**
     * Add a spam message
     *
     * @param msg Chat message
     */
    public void addSpamMessage(InstantMessage msg) {
        addChatMessage(msg, ChatLog.Message.Type.SPAM, ChatLog.Message.Direction.INCOMING);
    }

    /**
     * Add a chat message
     *
     * @param msg Chat message
     * @param direction Direction
     */
    public void addChatMessage(InstantMessage msg, int direction) {
        addChatMessage(msg, ChatLog.Message.Type.CONTENT, direction);
    }

    /**
     * Add a chat message
     *
     * @param msg Chat message
     * @param direction Direction
     */
    public void addChatSystemMessage(InstantMessage msg, int direction) {
        addChatMessage(msg, ChatLog.Message.Type.SYSTEM, direction);
    }

    /**
     * Add a burn message
     *
     * @param msg Chat message
     * @param direction Direction
     */
    public void addBurnChatMessage(InstantMessage msg, int direction) {
        addChatMessage(msg, ChatLog.Message.Type.BURN, direction);
    }

    /**
     * Add a cloud message
     *
     * @param msg Chat message
     * @param direction Direction
     */
    public void addCloudMessage(InstantMessage msg, int direction) {
        addChatMessage(msg, ChatLog.Message.Type.CLOUD, direction);
    }

    /**
     * Add an emoticon message
     *
     * @param msg Chat message
     * @param direction Direction
     */
    public void addEmoticonMessage(InstantMessage msg, int direction) {
        addChatMessage(msg, ChatLog.Message.Type.EMOTICON, direction);
    }

    /**
     * Add a burn message
     *
     * @param msg Chat message
     * @param direction Direction
     */
    public void addPublicChatMessage(InstantMessage msg, int direction) {
        addChatMessage(msg, ChatLog.Message.Type.PUBLIC, direction);
    }

    /**
     * Add a chat message
     *
     * @param msg Chat message
     * @param type Message type
     * @param direction Direction
     */
    private void addChatMessage(InstantMessage msg, int type, int direction) {
        String contact = null;
        if (logger.isActivated()) {
            logger.debug("Add chat message: contact= " + msg.getRemote() + "; type:" + type);
        }

        if (msg.isPublicMessage()) {
            contact = msg.getRemote();
        } else if (msg.getRemote().contains(",")) {
            contact = msg.getRemote();
        } else if (msg.getRemote().contains("pa")) {
            contact = msg.getRemote();
        } else {
            contact = PhoneUtils.extractNumberFromUri(msg.getRemote());
        }
        if (logger.isActivated()) {
            logger.debug("Add chat message: contact=" + contact + ", msg=" + msg.getMessageId());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, contact);
        values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
        values.put(MessageData.KEY_CONTACT, contact);
        values.put(MessageData.KEY_DIRECTION, direction);
        values.put(MessageData.KEY_TYPE, type);
        values.put(MessageData.KEY_DISPLAY_NAME, msg.getDisplayName());

        byte[] blob = null;
        if (msg instanceof GeolocMessage) {
            values.put(MessageData.KEY_CONTENT_TYPE, org.gsma.joyn.chat.GeolocMessage.MIME_TYPE);
            GeolocPush geoloc = ((GeolocMessage) msg).getGeoloc();
            Geoloc geolocApi =
                    new Geoloc(geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(),
                            geoloc.getExpiration(), geoloc.getAccuracy());
            blob = serializeGeoloc(geolocApi);
        } else {
            if ((msg.isPublicMessage()) && (direction == ChatLog.Message.Direction.INCOMING))
                values.put(MessageData.KEY_CONTENT_TYPE, "application/xml");
            // TODO Define a place to put the value, get from xml, not hardcode
            else
                values.put(MessageData.KEY_CONTENT_TYPE, org.gsma.joyn.chat.ChatMessage.MIME_TYPE);
            blob = serializePlainText(msg.getTextMessage());
        }
        if (blob != null) {
            values.put(MessageData.KEY_CONTENT, blob);
        }

        if (direction == ChatLog.Message.Direction.INCOMING) {
            // Receive message
            values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
            values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
            values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
            if (msg.isImdnDisplayedRequested()) {
                values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD_REPORT);
            } else {
                values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD);
            }
        } else {
            // Send message
            values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
            values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
            values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
            values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.SENT);
        }
        cr.insert(msgDatabaseUri, values);
    }

    /**
     * Add a group chat message
     *
     * @param chatId Chat ID
     * @param msg Chat message
     * @param direction Direction
     */
    public void addGroupChatMessage(String chatId, InstantMessage msg, int direction) {
        if (logger.isActivated()) {
            logger.debug("Add group chat message: chatID=" + chatId
                    + ", msg=" + msg.getMessageId());
        }

        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_MSG_ID, msg.getMessageId());
        values.put(MessageData.KEY_CONTACT, PhoneUtils.extractNumberFromUri(msg.getRemote()));
        values.put(MessageData.KEY_DIRECTION, direction);
        if (msg.isCloudMessage()) {
            values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CLOUD);
        } else if (msg.isEmoticonMessage()) {
            values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.EMOTICON);
        } else {
            values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.CONTENT);
        }
        values.put(MessageData.KEY_DISPLAY_NAME, msg.getDisplayName());

        byte[] blob = null;
        if (msg instanceof GeolocMessage) {
            values.put(MessageData.KEY_CONTENT_TYPE, org.gsma.joyn.chat.GeolocMessage.MIME_TYPE);
            GeolocPush geoloc = ((GeolocMessage) msg).getGeoloc();
            Geoloc geolocApi =
                    new Geoloc(geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(),
                            geoloc.getExpiration(), geoloc.getAccuracy());
            blob = serializeGeoloc(geolocApi);
        } else {
            values.put(MessageData.KEY_CONTENT_TYPE, org.gsma.joyn.chat.ChatMessage.MIME_TYPE);
            blob = serializePlainText(msg.getTextMessage());
        }
        if (blob != null) {
            values.put(MessageData.KEY_CONTENT, blob);
        }

        if (direction == ChatLog.Message.Direction.INCOMING) {
            // Receive message
            values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
            values.put(MessageData.KEY_TIMESTAMP_SENT, 0);
            values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
            if (msg.isImdnDisplayedRequested()) {
                values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD_REPORT);
            } else {
                values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.UNREAD);
            }
        } else {
            // Send message
            values.put(MessageData.KEY_TIMESTAMP, msg.getDate().getTime());
            values.put(MessageData.KEY_TIMESTAMP_SENT, msg.getDate().getTime());
            values.put(MessageData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(MessageData.KEY_TIMESTAMP_DISPLAYED, 0);
            values.put(MessageData.KEY_STATUS, ChatLog.Message.Status.Content.SENT);
        }
        cr.insert(msgDatabaseUri, values);
    }

    /**
     * Add group chat system message
     *
     * @param chatId Chat ID
     * @param contact Contact
     * @param status Status
     */
    public void addGroupChatSystemMessage(String chatId, String contact, int status) {
        if (logger.isActivated()) {
            logger.debug("Add group chat system message: chatID=" + chatId
                    + ", contact=" + contact + ", status=" + status);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_CHAT_ID, chatId);
        values.put(MessageData.KEY_CONTACT, contact);
        values.put(MessageData.KEY_TYPE, ChatLog.Message.Type.SYSTEM);
        values.put(MessageData.KEY_STATUS, status);
        values.put(MessageData.KEY_DIRECTION, ChatLog.Message.Direction.IRRELEVANT);
        values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        cr.insert(msgDatabaseUri, values);
    }

    /**
     * Update chat message status
     *
     * @param msgId Message ID
     * @param status Message status
     */
    public void updateChatMessageStatus(String msgId, int status) {
        if (logger.isActivated()) {
            logger.debug("Update chat message: msgID=" + msgId + ", status=" + status);
        }
        ContentValues values = new ContentValues();
        values.put(MessageData.KEY_STATUS, status);
        if (status == ChatLog.Message.Status.Content.UNREAD) {
            // Delivered
            values.put(MessageData.KEY_TIMESTAMP_DELIVERED,
                    Calendar.getInstance().getTimeInMillis());
        } else if (status == ChatLog.Message.Status.Content.READ) {
            // Displayed
            values.put(MessageData.KEY_TIMESTAMP_DISPLAYED,
                    Calendar.getInstance().getTimeInMillis());
        }
        cr.update(msgDatabaseUri, values, MessageData.KEY_MSG_ID + " = '" + msgId + "'", null);
    }

    /**
     * Update chat message delivery status
     *
     * @param msgId Message ID
     * @param status Delivery status
     */
    public void updateChatMessageDeliveryStatus(String msgId, String status) {
        if (logger.isActivated()) {
            logger.debug("Update chat delivery status: msgID=" + msgId + ", status=" + status);
        }
        if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
            RichMessagingHistory.getInstance().updateChatMessageStatus(
                    msgId, ChatLog.Message.Status.Content.UNREAD);
        } else if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
            RichMessagingHistory.getInstance().updateChatMessageStatus(
                    msgId, ChatLog.Message.Status.Content.READ);
        } else if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
            RichMessagingHistory.getInstance().updateChatMessageStatus(
                    msgId, ChatLog.Message.Status.Content.FAILED);
        }
    }

    /**
     * Check if it's a new message
     *
     * @param chatId chat ID
     * @param msgId message ID
     * @return true if new message
     */
    public boolean isNewMessage(String chatId, String msgId) {
        boolean result = true;
        Cursor cursor =cr.query(
                msgDatabaseUri,
                new String[] {
                    MessageData.KEY_MSG_ID
                },
                "(" + MessageData.KEY_CHAT_ID + " = '" + chatId + "') AND ("
                    + MessageData.KEY_MSG_ID + " = '" + msgId + "')",
                null,
                null);
        if (cursor.moveToFirst()) {
            result = false;
        } else {
            result = true;
        }
        cursor.close();
        return result;
    }

    public boolean isOne2OneMessageExists(String msgId) {

        if (logger.isActivated()) {
            logger.debug("isOne2OneMessageExists , msgid : " + msgId);
        }

        boolean result = false;

        Cursor cursor = cr.query(
                msgDatabaseUri,
                new String[] {
                        MessageData.KEY_MSG_ID
                },
                "( " + MessageData.KEY_MSG_ID + " = '" + msgId + "')",
                null, null);
        if (cursor.moveToFirst()) {
            if (logger.isActivated()) {
                logger.debug("cursor.moveToFirst() is true");
            }
            result = true;
        } else {

            if (logger.isActivated()) {
                logger.debug("cursor.moveToFirst() is false");
            }
            result = false;
        }
        cursor.close();
        return result;
    }

	/*--------------------- File transfer methods ----------------------*/

    /**
     * Add outgoing file transfer
     *
     * @param contact Contact
     * @param sessionId Session ID
     * @param direction Direction
     * @param content File content
     */
    public void addFileTransfer(String contact, String fileTransferId, int direction,
            MmContent content) {
        /*
         * if(contact.contains("")){//multimessage }else
         */
        contact = PhoneUtils.extractNumberFromUri(contact);
        if (logger.isActivated()) {
            logger.debug(
                    new StringBuilder("Add file transfer entry: fileTransferId=")
                            .append(fileTransferId)
                            .append(", contact=")
                            .append(contact)
                            .append(", filename=")
                            .append(content.getName())
                            .append(", size=")
                            .append(content.getSize()).append(", MIME=")
                            .append(content.getEncoding())
                            .toString());
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, contact);
        values.put(FileTransferData.KEY_CONTACT, contact);
        values.put(FileTransferData.KEY_NAME, content.getUrl());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, direction);
        values.put(FileTransferData.KEY_SIZE, 0);
        values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());

        long date = Calendar.getInstance().getTimeInMillis();
        if (direction == FileTransfer.Direction.INCOMING) {
            // Receive file
            values.put(FileTransferData.KEY_TIMESTAMP, date);
            values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
            values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
            values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);
        } else {
            // Send file
            values.put(FileTransferData.KEY_TIMESTAMP, date);
            values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
            values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
            values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
        }

        cr.insert(ftDatabaseUri, values);
    }

    /**
     * Add outgoing file transfer
     *
     * @param contact Contact
     * @param sessionId Session ID
     * @param direction Direction
     * @param content File content
     */
    public void addBurnFileTransfer(String contact, String fileTransferId, int direction,
            MmContent content) {
        contact = PhoneUtils.extractNumberFromUri(contact);
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Add burn file transfer entry: fileTransferId=")
                    .append(fileTransferId)
                    .append(", contact=")
                    .append(contact)
                    .append(", filename=")
                    .append(content.getName())
                    .append(", size=")
                    .append(content.getSize())
                    .append(", MIME=")
                    .append(content.getEncoding())
                    .toString());
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, contact);
        values.put(FileTransferData.KEY_CONTACT, contact);
        values.put(FileTransferData.KEY_NAME, content.getUrl());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, direction);
        values.put(FileTransferData.KEY_SIZE, 0);
        values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());

        // add burn message
        values.put(FileTransferData.KEY_SESSION_TYPE, FileTransferLog.Type.BURN);

        long date = Calendar.getInstance().getTimeInMillis();
        if (direction == FileTransfer.Direction.INCOMING) {
            // Receive file
            values.put(FileTransferData.KEY_TIMESTAMP, date);
            values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
            values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
            values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);
        } else {
            // Send file
            values.put(FileTransferData.KEY_TIMESTAMP, date);
            values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
            values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
            values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
            values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
        }

        cr.insert(ftDatabaseUri, values);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IFileTransferLog#addOutgoingGroupFileTransfer
     * (java.lang.String, java.lang.String, com.orangelabs.rcs.core.content.MmContent,
     * com.orangelabs.rcs.core.content.MmContent)
     */

    public void addOutgoingGroupFileTransfer(String chatId, String fileTransferId,
            MmContent content, MmContent thumbnail, String remoteContact) {
        if (logger.isActivated()) {
            logger.debug("addOutgoingGroupFileTransfer: fileTransferId=" + fileTransferId
                    + ", chatId=" + chatId + " filename=" + content.getName() + ", size="
                    + content.getSize() + ", MIME=" + content.getEncoding());
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, chatId);
        values.put(FileTransferData.KEY_CONTACT, remoteContact);
        values.put(FileTransferData.KEY_NAME, content.getUrl());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, FileTransfer.Direction.OUTGOING);
        values.put(FileTransferData.KEY_SIZE, 0);
        values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
        long date = Calendar.getInstance().getTimeInMillis();
        // values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.UNREAD);
        // Send file
        values.put(FileTransferData.KEY_TIMESTAMP, date);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, date);
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
        /*
         * if (thumbnail != null) { values.put(FileTransferData.KEY_FILEICON,
         * Uri.fromFile(new File(thumbnail.getUrl())).toString()); }
         */
        cr.insert(ftDatabaseUri, values);

    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IFileTransferLog#addIncomingGroupFileTransfer
     * (java.lang.String, java.lang.String, java.lang.String,
     * com.orangelabs.rcs.core.content.MmContent,
     * com.orangelabs.rcs.core.content.MmContent)
     */

    public void addIncomingGroupFileTransfer(String chatId, String contact, String fileTransferId,
            MmContent content) {
        contact = PhoneUtils.extractNumberFromUri(contact);
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Add incoming file transfer entry: fileTransferId=")
                    .append(fileTransferId)
                    .append(", chatId=")
                    .append(chatId)
                    .append(", contact=")
                    .append(contact)
                    .append(", filename=")
                    .append(content.getName())
                    .append(", size=")
                    .append(content.getSize())
                    .append(", MIME=")
                    .append(content.getEncoding()).toString());
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, fileTransferId);
        values.put(FileTransferData.KEY_CHAT_ID, chatId);
        values.put(FileTransferData.KEY_CONTACT, contact);
        values.put(FileTransferData.KEY_NAME, content.getUrl());
        values.put(FileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(FileTransferData.KEY_DIRECTION, FileTransfer.Direction.INCOMING);
        values.put(FileTransferData.KEY_SIZE, 0);
        values.put(FileTransferData.KEY_TOTAL_SIZE, content.getSize());
        // values.put(FileTransferData.KEY_READ_STATUS, FileTransfer.ReadStatus.UNREAD);

        long date = Calendar.getInstance().getTimeInMillis();
        values.put(FileTransferData.KEY_TIMESTAMP, date);
        values.put(FileTransferData.KEY_TIMESTAMP_SENT, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(FileTransferData.KEY_STATUS, FileTransfer.State.INVITED);
        // if (thumbnail != null) {
        // values.put(FileTransferData.KEY_FILEICON, Uri.fromFile(new
        // File(thumbnail.getUrl())).toString());
        // }

        cr.insert(ftDatabaseUri, values);
    }

    /**
     * Update file transfer Icon
     *
     * @param fileTransferId fileTransferId
     * @param Url new Url
     */

    public void updateFileIcon(String fileTransferId, String url) {
        if (logger.isActivated()) {
            logger.debug("updateFileIcon (File transfer ID" + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FILEICON, url);

        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = "
                + fileTransferId, null);

    }

    /**
     * Update file transfer status
     *
     * @param sessionId Session ID
     * @param status New status
     */

    public void updateFileTransferStatus(String fileTransferId, int status) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferStatus (status=" + status + ") (fileTransferId="
                    + fileTransferId + ")");
        }
        if (!isMultiFT(fileTransferId)) {
            ContentValues values = new ContentValues();
            values.put(FileTransferData.KEY_STATUS, status);
            if (status == FileTransfer.State.DELIVERED) {
                // Delivered
                values.put(FileTransferData.KEY_TIMESTAMP_DELIVERED,
                        Calendar.getInstance().getTimeInMillis());
            } else if (status == FileTransfer.State.DISPLAYED) {
                // Displayed
                values.put(FileTransferData.KEY_TIMESTAMP_DISPLAYED,
                        Calendar.getInstance().getTimeInMillis());
            }
            cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = " + fileTransferId,
                    null);
        } else {
            updateMultiFileTransferStatus(fileTransferId, status);
        }
    }

    /**
     * Update file transfer duration
     *
     * @param sessionId Session ID
     * @param status New status
     */

    public void updateFileTransferDuration(String fileTransferId, int duration) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferStatus (duration=" + duration + ") (fileTransferId="
                    + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_DURATION, duration);
        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = " + fileTransferId, null);
    }

    /**
     * Update file transfer Id
     *
     * @param fileTransferId fileTransferId
     * @param snewFileTransferId
     */

    public void updateFileTransferId(String fileTransferId, String newFileTransferId) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferId (New=" + newFileTransferId + ") (Old="
                    + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FT_ID, newFileTransferId);

        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = " + fileTransferId, null);
    }

    /**
     * Update file Id
     *
     * @param fileTransferId fileTransferId
     * @param snewFileTransferId
     */
    public void updateFileId(String fileTransferId, String file_id) {
        if (logger.isActivated()) {
            logger.debug("updateFileId (file_id=" + file_id + ") (fileTransferId ="
                    + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_FILE_ID, file_id);

        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = "
                + fileTransferId, null);
    }

    /**
     * Get the file transfer direction
     *
     * @param fileTransferId fileTransferId
     * @result Direction
     */
    public String getFtDirection(String fileTransferId) {
        if (logger.isActivated()) {
            logger.debug("getFtDirection " + fileTransferId);
        }
        String result = null;
        Cursor cursor = cr.query(
                ftDatabaseUri,
                new String[] {
                        FileTransferData.KEY_DIRECTION
                },
                "(" + FileTransferData.KEY_FT_ID + "='" + fileTransferId + "')",
                null, FileTransferData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String direction = cursor.getString(0);
            result = direction;
        }
        cursor.close();
        return result;
    }

    /**
     * Get the file transfer Duration
     *
     * @param fileTransferId fileTransferId
     * @result Duration
     */
    public String getFtDuration(String fileTransferId) {
        if (logger.isActivated()) {
            logger.debug("getFtDuration " + fileTransferId);
        }
        String result = null;
        Cursor cursor = cr.query(
                ftDatabaseUri,
                new String[] {
                        FileTransferData.KEY_DURATION
                },
                "(" + FileTransferData.KEY_FT_ID + "='" + fileTransferId + "')",
                null, FileTransferData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String direction = cursor.getString(0);
            result = direction;
        }
        cursor.close();
        return result;
    }

    /**
     * Update file transfer download progress
     *
     * @param sessionId Session ID
     * @param size Downloaded size
     * @param totalSize Total size to download
     */
    public void updateFileTransferProgressCode(
            String fileTransferId, long size, long totalSize,
            String path, String contact, String hash) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferProgressCode fileTransferId" + fileTransferId
                    + " size-" + size + " path-" + path + " contact-" + contact + " hash-" + hash);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_SIZE, size);
        values.put(FileTransferData.KEY_TOTAL_SIZE, totalSize);
        values.put(FileTransferData.KEY_CONTACT, contact);
        values.put(FileTransferData.KEY_HASH, hash);
        values.put(FileTransferData.KEY_NAME, path);
        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = "
                + fileTransferId, null);
    }

    public PauseResumeFileObject getPauseInfo(String transferId) {
        if (logger.isActivated()) {
            logger.debug("Get pause file transfer info for " + transferId);
        }
        PauseResumeFileObject result = null;
        Cursor cursor = cr.query(
                ftDatabaseUri,
                new String[] {
                        FileTransferData.KEY_SIZE,
                        FileTransferData.KEY_TOTAL_SIZE,
                        FileTransferData.KEY_CONTACT,
                        FileTransferData.KEY_HASH,
                        FileTransferData.KEY_NAME,
                        FileTransferData.KEY_FILE_ID
                },
                "(" + FileTransferData.KEY_FT_ID + "='" + transferId + "')",
                null, ChatData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            // String participants = cursor.getString(2);
            // List<String> list = RichMessagingHistory.getParticipants(participants);
            result = new PauseResumeFileObject();
            result.mFileTransferId = transferId;
            result.bytesTransferrred = cursor.getLong(0);
            result.mSize = cursor.getLong(1);
            result.mContact = cursor.getString(2);
            result.hashSelector = cursor.getString(3);
            result.mPath = cursor.getString(4);
            result.mOldFileTransferId = cursor.getString(5);
            result.pausedStream = null;
        }
        cursor.close();
        return result;
    }

    /**
     * Update file transfer download progress
     *
     * @param sessionId Session ID
     * @param size Downloaded size
     * @param totalSize Total size to download
     */
    public void updateFileTransferProgress(String fileTransferId, long size, long totalSize) {
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_SIZE, size);
        values.put(FileTransferData.KEY_TOTAL_SIZE, totalSize);
        values.put(FileTransferData.KEY_STATUS, FileTransfer.State.STARTED);
        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = "
                + fileTransferId, null);
    }

    /**
     * Update file transfer URL
     *
     * @param sessionId Session ID
     * @param url File URL
     */
    public void updateFileTransferUrl(String fileTransferId, String url) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferUrl (fileTransferId=" + fileTransferId + ") (url="
                    + url + ")");
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_NAME, url);
        values.put(FileTransferData.KEY_STATUS, FileTransfer.State.TRANSFERRED);
        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = "
                + fileTransferId, null);
    }

    /**
     * Get file transfer ID from a received message
     *
     * @param msgId Message ID
     * @return Chat session ID of the file transfer
     */
    public String getFileTransferId(String msgId) {
        String result = null;
        Cursor cursor = cr.query(
                ftDatabaseUri,
                new String[] {
                        FileTransferData.KEY_FT_ID
                },
                "(" + FileTransferData.KEY_MSG_ID + "='" + msgId + "')",
                null, FileTransferData.KEY_TIMESTAMP + " ASC");
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    /**
     * Get file transfer ID from a received message
     *
     * @param msgId Message ID
     * @return Chat session ID of the file transfer
     */
    public String getFileMessageId(String ftId) {
        String result = null;
        Cursor cursor = cr.query(
                ftDatabaseUri,
                new String[] {
                        FileTransferData.KEY_MSG_ID
                },
                "(" + FileTransferData.KEY_FT_ID + "='" + ftId + "')",
                null, FileTransferData.KEY_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    /**
     * Get file transfer contact
     *
     * @param ftID filetransferID
     * @return Contacts
     */
    public List<String> getFileTransferContacts(String ftId) {
        String result = null;

        Cursor cursor =cr.query(
                ftDatabaseUri,
                new String[] {
                        FileTransferData.KEY_CONTACT
                },
                "(" + FileTransferData.KEY_FT_ID + "='" + ftId + "')",
                null, FileTransferData.KEY_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        if (logger.isActivated()) {
            logger.debug("getFileTransferContacts result  + fileTransferId " + result + "&" + ftId);
        }
        cursor.close();
        List<String> list = getMultiFileParticipants(result);
        return list;
    }

    /**
     * Update file transfer ChatId
     *
     * @param sessionId Session Id
     * @param chatId chat Id
     * @param msgId msgId of the corresponding chat
     */
    public void updateFileTransferChatId(String fileTransferId, String chatId, String msgId) {
        /*
         * TODO ContentValues values = new ContentValues();
         * values.put(RichMessagingData.KEY_CHAT_ID, chatId);
         * values.put(RichMessagingData.KEY_MESSAGE_ID , msgId); cr.update(databaseUri,
         * values, RichMessagingData.KEY_CHAT_SESSION_ID + " = " + sessionId, null);
         */
        if (logger.isActivated()) {
            logger.debug("updateFileTransferChatId (chatId=" + chatId + ") (fileTransferId="
                    + fileTransferId + ") MsgID = " + msgId);
        }
        ContentValues values = new ContentValues();
        values.put(FileTransferData.KEY_CHAT_ID, chatId);
        values.put(FileTransferData.KEY_MSG_ID, msgId);
        cr.update(ftDatabaseUri, values, FileTransferData.KEY_FT_ID + " = " + fileTransferId, null);

    }

    /**
     * Serialize a geoloc to bytes array
     *
     * @param geoloc Geoloc info
     * @return Byte array
     */
    private byte[] serializeGeoloc(Geoloc geoloc) {
        byte[] blob = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(geoloc);
            blob = bos.toByteArray();
            bos.close();
            os.close();
        } catch (Exception e) {
            blob = null;
        }
        return blob;
    }

    /**
     * Serialize a text message to bytes array
     *
     * @param msg Message
     * @return Byte array
     */
    private byte[] serializePlainText(String msg) {
        if (msg != null) {
            return msg.getBytes();
        } else {
            return null;
        }
    }

    /**
     * M: add conversation ID column in Chat and Message database
     */

    public void addColumnConversationID() {

        if (logger.isActivated()) {
            logger.debug("addColumnConversationID");
        }

        if (isConversationIDColumnExists()) {
            if (logger.isActivated()) {
                logger.debug("Conversation ID column already exists. so no need to add columns");
            }
        }

        try {
            // update in chat table
            cr.query(alterChatDatabaseUri, null, null, null, null);

            // update in msg table
            cr.query(alterMsgDatabaseUri, null, null, null, null);

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.debug("exception");
            }
        }
    }

    // type 1: o2o and tyep =2 grphcat
    public String getCoversationID(String chatID, int type) {

        String conversationID = "";

        if (logger.isActivated()) {
            logger.debug("getCoversationID chatID: " + chatID);
        }

        //
        if (type == 1) {
            // o2o chat

            Cursor cursor = cr.query(
                    msgDatabaseUri,
                    new String[] {
                            MessageData.KEY_CONVERSATION_ID
                    },
                    "( " + MessageData.KEY_CHAT_ID + " = '" + chatID + "'  AND "
                         + MessageData.KEY_TYPE + " = " + Type.SYSTEM + " )",
                    null, null);

            if ((cursor != null) && (cursor.moveToFirst())) {
                if (logger.isActivated()) {
                    logger.info("Conversation ID found for chat id : " + chatID);
                }

                conversationID = cursor.getString(0);

                if (logger.isActivated()) {
                    logger.info("Conversation ID : " + conversationID);
                }
                // cursor.close();
            }
            if (cursor != null) {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }

        } else if (type == 2) {
            // grp chat

            Cursor cursor = cr.query(
                    chatDatabaseUri,
                    new String[] {
                            ChatData.KEY_CONVERSATION_ID
                    },
                    "( " + ChatData.KEY_CHAT_ID + " = '" + chatID + "')",
                    null, null);

            if ((cursor != null) && (cursor.moveToFirst())) {
                if (logger.isActivated()) {
                    logger.info("Conversation ID found for chat id : " + chatID);
                }

                conversationID = cursor.getString(0);

                if (logger.isActivated()) {
                    logger.info("Conversation ID : " + conversationID);
                }
                // cursor.close();
            }
            if (cursor != null) {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (logger.isActivated()) {
            logger.debug("getCoversationID Id: " + conversationID + " chatid: " + chatID);
        }
        return conversationID;
    }

    // update the conversation ID
    // type 1: o2o and type =2 grphcat
    public void UpdateCoversationID(String chatID, String conversationID, int type) {
        if (logger.isActivated()) {
            logger.debug("UpdateCoversationID Id: " + conversationID + " type: " + type);
        }
        if (type == 1) {
            // o2o chat
            ContentValues values = new ContentValues();
            values.put(MessageData.KEY_CONVERSATION_ID, conversationID);
            cr.update(msgDatabaseUri, values, MessageData.KEY_CHAT_ID + " = '" + chatID + "' AND "
                    + MessageData.KEY_TYPE + " = " + Type.SYSTEM, null);
        } else if (type == 2) {
            // grp chat
            ContentValues values = new ContentValues();
            values.put(ChatData.KEY_CONVERSATION_ID, conversationID);
            cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatID + "'", null);
        }

    }

    private boolean isConversationIDColumnExists() {
        if (logger.isActivated()) {
            logger.debug("isConversationIDColumnExists");
        }
        boolean status = false;
        try {

            Cursor cursor = cr.query(
                    chatDatabaseUri,
                    new String[] {
                            ChatData.KEY_CONVERSATION_ID
                    },
                    null, null, null);
            if ((cursor != null)) {
                cursor.close();
            }
            status = true;
        } catch (Exception e) {
            status = false;
            if (logger.isActivated()) {
                logger.debug("exception no column as ChatData.KEY_CONVERSATION_ID present");
            }
        }

        return status;
    }

    public boolean isEmoticonMessage(String msgID) {
        boolean status = false;

        if (logger.isActivated()) {
            logger.debug("isEmoticon message msgid : " + msgID);
        }
        List<String> result = new ArrayList<String>();
        Cursor cursor = cr.query(
                msgDatabaseUri,
                new String[] {
                        MessageData.KEY_CONTACT
                },
                "( " + MessageData.KEY_MSG_ID + " = '" + msgID + "'  AND "
                     + MessageData.KEY_TYPE + " = " + ChatLog.Message.Type.EMOTICON + " )",
                null, MessageData.KEY_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            status = true;
        }
        if (cursor != null) {
            cursor.close();
        }

        return status;
    }

    public boolean isBurnMessage(String msgID) {
        boolean status = false;

        if (logger.isActivated()) {
            logger.debug("isBurn message msgid : " + msgID);
        }
        List<String> result = new ArrayList<String>();
        Cursor cursor = cr.query(
                msgDatabaseUri,
                new String[] {
                        MessageData.KEY_CONTACT
                },
                "( " + MessageData.KEY_MSG_ID + " = '" + msgID + "'  AND "
                     + MessageData.KEY_TYPE + " = " + ChatLog.Message.Type.BURN + " )",
                null, MessageData.KEY_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            status = true;
        }
        if (cursor != null) {
            cursor.close();
        }

        return status;
    }

    public boolean isPublicMessage(String msgID) {
        boolean status = false;

        if (logger.isActivated()) {
            logger.debug("IsPublic message msgid : " + msgID);
        }
        List<String> result = new ArrayList<String>();
        Cursor cursor = cr.query(
                msgDatabaseUri,
                new String[] {
                        MessageData.KEY_CONTACT
                },
                "( " + MessageData.KEY_MSG_ID + " = '" + msgID + "'  AND "
                     + MessageData.KEY_TYPE + " = " + ChatLog.Message.Type.PUBLIC + " )",
                null, MessageData.KEY_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            status = true;
        }
        if (cursor != null) {
            cursor.close();
        }

        return status;
    }

    public boolean isCloudMessage(String msgID) {
        boolean status = false;

        if (logger.isActivated()) {
            logger.debug("IsCloud message msgid : " + msgID);
        }
        List<String> result = new ArrayList<String>();
        Cursor cursor = cr.query(
                msgDatabaseUri,
                new String[] {
                        MessageData.KEY_CONTACT
                },
                "( " + MessageData.KEY_MSG_ID + " = '" + msgID + "'  AND "
                     + MessageData.KEY_TYPE + " = " + ChatLog.Message.Type.CLOUD + " )",
                null, MessageData.KEY_TIMESTAMP + " DESC");

        while (cursor.moveToNext()) {
            status = true;
        }
        if (cursor != null) {
            cursor.close();
        }

        return status;
    }

    /**
     * Update group chat sybject
     *
     * @param chatId Chat ID
     * @param subject subject
     */
    public void updateGroupChatSubject(String chatId, String subject) {
        if (logger.isActivated()) {
            logger.debug("Update group chat subject to " + subject);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_SUBJECT, subject);
        cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
    }

    /**
     * Update group chat chairman
     *
     * @param chatId Chat ID
     * @param status Status
     */
    public void updateGroupChairman(String chatId, String chairman) {
        if (logger.isActivated()) {
            logger.debug("Update group chairman  to " + chairman);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_CHAIRMAN, chairman);
        cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
    }

    /**
     * Get Chairman of the group
     *
     * @param chatId chat ID of group
     * @return String chairman number
     */
    public String getGroupChairman(String chatId) {
        String chairman = null;
        Cursor cursor = cr.query(
                chatDatabaseUri,
                new String[] {
                        ChatData.KEY_CHAIRMAN
                },
                "(" + ChatData.KEY_CHAT_ID + "='" + chatId + "')",
                null, ChatData.KEY_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            chairman = cursor.getString(0);
        }
        cursor.close();
        return chairman;
    }

    /**
     * Update group chat chairman nickname
     *
     * @param chatId Chat ID
     * @param status Status
     */
    public void updateGroupNickName(String chatId, String chairmanNickName) {
        if (logger.isActivated()) {
            logger.debug("Update group chairman nickname to " + chairmanNickName);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_NICKNAME, chairmanNickName);
        cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
    }

    /*---------------------Multi File transfer methods ----------------------*/

    public boolean isMultiFT(String fileTransferId) {
        boolean status = false;

        if (logger.isActivated()) {
            logger.debug("isMultiFile Transfer : " + fileTransferId);
        }
        List<String> result = new ArrayList<String>();
        try {
            Cursor cursor = cr.query(
                    multiFtDatabaseUri,
                    new String[] {
                            MultiFileTransferData.KEY_PARTICIPANTS_LIST
                    },
                    "(" + MultiFileTransferData.KEY_FT_ID + "=" + fileTransferId + ")",
                    null, MultiFileTransferData.KEY_TIMESTAMP + " DESC");

            while (cursor != null && cursor.moveToNext()) {
                status = true;
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (logger.isActivated()) {
            logger.debug("isMultiFile Transfer : " + fileTransferId + " status: " + status);
        }
        return status;
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IMultiFileTransferLog#
     * addOutgoingMultiFileTransfer(java.lang.String, java.lang.String,
     * com.orangelabs.rcs.core.content.MmContent,
     * com.orangelabs.rcs.core.content.MmContent)
     */

    public void addOutgoingMultiFileTransfer(String fileTransferId, MmContent content,
            MmContent thumbnail, List<String> participants) {
        if (logger.isActivated()) {
            logger.debug("addOutgoingMultiFileTransfer: "
                    + "fileTransferId=" + fileTransferId
                    + "filename=" + content.getName()
                    + ", size=" + content.getSize()
                    + ", MIME=" + content.getEncoding());
        }
        ContentValues values = new ContentValues();

        values.put(MultiFileTransferData.KEY_PARTICIPANTS_LIST,
                RichMessagingHistory.getParticipants(participants));

        values.put(MultiFileTransferData.KEY_FT_ID, fileTransferId);

        values.put(MultiFileTransferData.KEY_NAME, content.getUrl());
        values.put(MultiFileTransferData.KEY_MIME_TYPE, content.getEncoding());
        values.put(MultiFileTransferData.KEY_DIRECTION, FileTransfer.Direction.OUTGOING);
        values.put(MultiFileTransferData.KEY_SIZE, 0);
        values.put(MultiFileTransferData.KEY_TOTAL_SIZE, content.getSize());
        long date = Calendar.getInstance().getTimeInMillis();
        // values.put(MessageData.KEY_READ_STATUS, ChatLog.Message.ReadStatus.UNREAD);
        // Send file
        values.put(MultiFileTransferData.KEY_TIMESTAMP, date);
        values.put(MultiFileTransferData.KEY_TIMESTAMP_SENT, date);
        values.put(MultiFileTransferData.KEY_TIMESTAMP_DELIVERED, 0);
        values.put(MultiFileTransferData.KEY_TIMESTAMP_DISPLAYED, 0);
        values.put(MultiFileTransferData.KEY_STATUS, FileTransfer.State.INITIATED);
        /*
         * if (thumbnail != null) { values.put(FileTransferData.KEY_FILEICON,
         * Uri.fromFile(new File(thumbnail.getUrl())).toString()); }
         */
        cr.insert(multiFtDatabaseUri, values);
    }

    /**
     * Update Multi file transfer status
     *
     * @param sessionId Session ID
     * @param status New status
     */

    public void updateMultiFileTransferStatus(String fileTransferId, int status) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferStatus (status=" + status + ") (fileTransferId="
                    + fileTransferId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(MultiFileTransferData.KEY_STATUS, status);
        if (status == FileTransfer.State.DELIVERED) {
            // Delivered
            values.put(MultiFileTransferData.KEY_TIMESTAMP_DELIVERED, Calendar.getInstance()
                    .getTimeInMillis());
        } else if (status == FileTransfer.State.DISPLAYED) {
            // Displayed
            values.put(MultiFileTransferData.KEY_TIMESTAMP_DISPLAYED, Calendar.getInstance()
                    .getTimeInMillis());
        }
        cr.update(multiFtDatabaseUri, values, MultiFileTransferData.KEY_FT_ID + " = "
                + fileTransferId, null);
    }

    /**
     * Update multiple file transfer URL
     * 
     * @param sessionId Session ID
     * @param url File URL
     */
    public void updateMultiFileTransferUrl(String fileTransferId, String url) {
        if (logger.isActivated()) {
            logger.debug("updateFileTransferUrl (fileTransferId=" + fileTransferId + ") (url="
                    + url + ")");
        }
        if (isMultiFT(fileTransferId)) {
            ContentValues values = new ContentValues();
            values.put(MultiFileTransferData.KEY_NAME, url);
            values.put(MultiFileTransferData.KEY_STATUS, FileTransfer.State.TRANSFERRED);
            cr.update(multiFtDatabaseUri, values, MultiFileTransferData.KEY_FT_ID + " = "
                    + fileTransferId, null);
        } else {
            updateMultiFileTransferUrl(fileTransferId, url);
        }
    }

    /*
     * START :MULTI PARTICIPANT MESSAGE DATABASE API
     */
    public boolean isMultiParticipantMessage(String chatID) {
        boolean status = false;

        if (logger.isActivated()) {
            logger.debug("isMultiParticipantMessage : " + chatID);
        }
        List<String> result = new ArrayList<String>();
        try {
            Cursor cursor = cr.query(
                    multiParticipantDatabaseUri,
                    new String[] {
                            MultiMessageData.KEY_PARTICIPANTS
                    },
                    "(" + MultiMessageData.KEY_CHAT_ID + "='" + chatID + "')",
                    null, MultiMessageData.KEY_TIMESTAMP + " DESC");

            while (cursor.moveToNext()) {
                status = true;
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (logger.isActivated()) {
            logger.debug("isMultiParticipantMessage : " + chatID + " status: " + status);
        }
        return status;
    }

    public void addMultiMessageChat(String chatId, String message, String messageId,
            List<String> Participants, int status, int direction) {
        if (logger.isActivated()) {
            logger.debug("addMultiMessageChat: chatID=" + chatId);
        }

        String participantList = "";
        participantList = TextUtils.join(",", Participants);
        ContentValues values = new ContentValues();
        values.put(MultiMessageData.KEY_CHAT_ID, chatId);
        values.put(MultiMessageData.KEY_MESSAGE_ID, messageId);
        values.put(MultiMessageData.KEY_STATUS, status);
        values.put(MultiMessageData.KEY_SUBJECT, message);
        values.put(MultiMessageData.KEY_PARTICIPANTS, participantList);
        values.put(MultiMessageData.KEY_DIRECTION, direction);
        values.put(MultiMessageData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        cr.insert(multiParticipantDatabaseUri, values);
    }

    public String getMultiMessageText(String chatId) {

        if (logger.isActivated()) {
            logger.debug("getMultiMessageText " + chatId);
        }
        String result = null;
        Cursor cursor = cr.query(
                multiParticipantDatabaseUri,
                new String[] {
                        MultiMessageData.KEY_CHAT_ID,
                        MultiMessageData.KEY_SUBJECT
                },
                "(" + MultiMessageData.KEY_CHAT_ID + "='" + chatId + "')",
                null, MultiMessageData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String status = cursor.getString(1);
            result = status;
        }
        cursor.close();
        return result;
    }

    public void updateMultiMessageChatStatus(String chatId, int status) {
        if (logger.isActivated()) {
            logger.debug("UpdateMultiMessageChatStatus for chat :" + chatId + "  to " + status);
        }
        ContentValues values = new ContentValues();
        values.put(MultiMessageData.KEY_STATUS, status);
        cr.update(multiParticipantDatabaseUri, values,
                MultiMessageData.KEY_CHAT_ID + " = '" + chatId + "'", null);
    }

    public List<String> getMultiMessageParticipants(String msgId) {
        if (logger.isActivated()) {
            logger.debug("getMultiMesageChatParticipants " + msgId);
        }
        List<String> partList = null;
        Cursor cursor = cr.query(
                msgDatabaseUri,
                new String[] {
                        MessageData.KEY_MSG_ID,
                        MessageData.KEY_CONTACT
                },
                "(" + MessageData.KEY_MSG_ID + "='" + msgId + "')",
                null, MessageData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String Allparticipant = cursor.getString(1);
            if (Allparticipant != "") {

                if (logger.isActivated()) {
                    logger.debug("getMultiMesageChatParticipants :  " + Allparticipant);
                }

                partList = new ArrayList<String>(Arrays.asList(Allparticipant.split(" , ")));
            }

        }
        cursor.close();
        return partList;
    }

    public List<String> getMultiMesageChatParticipants(String chatId) {
        if (logger.isActivated()) {
            logger.debug("getMultiMesageChatParticipants " + chatId);
        }
        List<String> partList = null;
        Cursor cursor = cr.query(
                multiParticipantDatabaseUri,
                new String[] {
                        MultiMessageData.KEY_CHAT_ID,
                        MultiMessageData.KEY_PARTICIPANTS
                },
                "(" + MultiMessageData.KEY_CHAT_ID + "='" + chatId + "')",
                null, MultiMessageData.KEY_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            String Allparticipant = cursor.getString(1);
            if (Allparticipant != "") {

                if (logger.isActivated()) {
                    logger.debug("getMultiMesageChatParticipants :  " + Allparticipant);
                }

                partList = new ArrayList<String>(Arrays.asList(Allparticipant.split(" , ")));
            }

        }
        cursor.close();
        return partList;
    }

    /*
     * END :MULTI PARTICIPANT MESSAGE DATABASE API
     */

    /**
     * Update group chat block status
     *
     * @param chatId Chat ID
     * @param flag block flag [0 : NOT_BLOCKED , 1: BLOCKED]
     */
    public void updateGroupBlockedStatus(String chatId, int flag) {
        if (logger.isActivated()) {
            logger.debug("Update group block status to " + flag);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_ISBLOCKED, flag);
        cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
    }

    /**
     * Get Chairman of the group
     *
     * @param chatId chat ID of group
     * @return String chairman number
     */
    public int getGroupBlockedStatus(String chatId) {
        int isblocked = 0;
        Cursor cursor = cr.query(
                chatDatabaseUri,
                new String[] {
                        ChatData.KEY_ISBLOCKED
                },
                "(" + ChatData.KEY_CHAT_ID + "='" + chatId + "')",
                null, ChatData.KEY_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            isblocked = cursor.getInt(0);
        }
        cursor.close();
        return isblocked;
    }

}
