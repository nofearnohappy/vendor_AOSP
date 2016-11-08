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
package com.cmcc.ccs.chat;

import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceListener;

import android.content.Context;
import android.util.Log;

/**
 * Chat service offers the main entry point to initiate chat 1-1 , 1-M ang group
 * conversations with contacts. Several applications may connect/disconnect
 * to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 */
public class ChatService extends JoynService {

    public static final String TAG = "DAPI-ChatService";

    /**
     * errType
     */
    public static final int TIMEOUT         = -1;
    public static final int UNKNOW          = -2;
    public static final int INTERNAL        = -3;
    public static final int OUTOFSIZE       = -4;
    
    /**
     * Type
     */
    public static final int SMS             = 1;
    public static final int MMS             = 2;
    public static final int IM              = 3;
    public static final int FT              = 4;
    public static final int XML             = 5;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public ChatService(Context ctx, JoynServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public void connect() {
        Log.i(TAG, "connect() entry");
        // TODO
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
        Log.i(TAG, "disconnect() entry");
        // TODO
    }

    /**
     * Registers a chat invitation listener
     * 
     * @param listener New chat listener
     */
    public void addChatListener(ChatListener listener) {
        Log.i(TAG, "addChatListener entry" + listener);
        // TODO
    }

    /**
     * Unregisters a chat invitation listener
     * 
     * @param listener New chat listener
     */
    public void removeChatListener(ChatListener listener) {
        Log.i(TAG, "removeEventListener entry" + listener);
        // TODO
    }

 /**
     * Sends a chat message t
     * 
     * @param Contact contact
     * @param Message message
     * @return Unique message ID or null in case of error
     */
    public String sendMessage(String contact,String message) {
        Log.i(TAG, "sendMessage Contact " + contact+ " message "+message);
        // TODO
        return null;
    }

    /**
     * get chat message
     * 
     * @param Message message
     * @return Unique message ID or null in case of error
     */
    public ChatMessage getChatMessage(String msgId) {
        Log.i(TAG, "getChatMessage msgId " + msgId);
        // TODO
        return null;
    }

    /**
     * Sends a chat message to multi contacts
     * 
     * @param Contacts contacts
     * @param Message message
     * @return Unique message ID or null in case of error
     */
    public String sendOTMMessage(Set<String> contacts,String message) {
        Log.i(TAG, "sendOTMMessage Contacts " + contacts+ " message "+message);
        // TODO
        return null;
    }
    
    /**
     * re-send message
     * 
     * @param Message message
     * @return Unique message ID or null in case of error
     */    
    public String resendMessage(String msgId) {
        Log.i(TAG, "resendMessage msgId " + msgId);
        // TODO
        return null;
    }
    
    /**
     * delete message
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     */
    public boolean deleteMessage(String msgId) {
        Log.i(TAG, "resendMessage msgId " + msgId);
        // TODO
        return true;
    }
    
    /**
     * set read message
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     */    
    public boolean setMessageRead(String msgId) {
        Log.i(TAG, "setMessageRead msgId " + msgId);
        // TODO
        return true;
    }
    
    /**
     * set favorite message
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     */    
    public boolean setMessageFavorite(String msgId) {
        Log.i(TAG, "setMessageFavorite msgId " + msgId);
        // TODO
        return true;
    }
    
    /**
     * move message to Inbox
     * 
     * @param Message message
     * @return if success ,return true; or not ,return false
     */    
    public boolean moveBlockMessagetoInbox(String msgId) {
        Log.i(TAG, "moveBlockMessagetoInbox msgId " + msgId);
        // TODO
        return true;
    }

}
