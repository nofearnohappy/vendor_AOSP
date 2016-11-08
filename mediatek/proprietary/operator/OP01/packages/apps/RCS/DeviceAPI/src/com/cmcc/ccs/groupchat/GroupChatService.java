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
package com.cmcc.ccs.groupchat;

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
public class GroupChatService extends JoynService {

    public static final String TAG = "DAPI-GroupChatService";

    /**
     * group chat state
     */
    public static final int INVITED                 = 1;
    public static final int INITIATED               = 2;
    public static final int STARTED                 = 3;
    public static final int TERMINATED              = 4;
    public static final int CLOSED_BY_USER          = 5;
    public static final int ABORTED                 = 6;
    public static final int FAILED                  = 7;

    /**
     * errType
     */
    public static final int OK                      = 0;
    public static final int TIMEOUT                 = -1;
    public static final int UNKNOW                  = -2;
    public static final int INTERNAL_ERROR          = -3;
    public static final int OFFLINE                 = -4;

    /**
     * session error type
     */
    public static final int CHAT_NOT_FOUND          = -1;
    public static final int INVITATION_DECLINED     = -2;
    public static final int CHAT_FAILED             = -3;

    /**
     * direction
     */
    public static final int INCOMING                = 0;
    public static final int OUTCOMING               = 1;

    /**
     * database column
     */
    public static final String CHAT_ID              = "GROUPCHATSERVICE_CHAT_ID";
    public static final String STATE                = "GROUPCHATSERVICE_STATE";
    public static final String SUBJECT              = "GROUPCHATSERVICE_SUBJECT";
    public static final String CHAIRMEN             = "GROUPCHATSERVICE_CHAIRMEN";
    public static final String NICK_NAME            = "GROUPCHATSERVICE_NICK_NAME";
    public static final String TIME_STAMP           = "GROUPCHATSERVICE_TIME_STAMP";
    public static final String DIRECTION            = "GROUPCHATSERVICE_DIRECTION";
    public static final String PHONE_NUMBER         = "GROUPCHATSERVICE_PHONE_NUMBER";
    public static final String MEMBER_NAME          = "GROUPCHATSERVICE_MEMBER_NAME";
    public static final String PORTRAIT             = "GROUPCHATSERVICE_PORTRAIT";

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public GroupChatService(Context ctx, JoynServiceListener listener) {
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
     * Initiates a group chat with a group of contact and returns a GroupChat
     * instance. The subject is optional and may be null.
     * 
     * @param contact List of contacts
     * @param subject Subject
     * @param listener Chat event listener
     */
    public GroupChat initiateGroupChat(Set<String> contacts, String subject, GroupChatListener listener) {
        Log.i(TAG, "initiateGroupChat entry= " + contacts + " subject =" + subject );
        // TODO
        return null;
    }

    
    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @hide
     */
    public GroupChat rejoinGroupChat(String chatId) {
        Log.i(TAG, "rejoinGroupChat entry= " + chatId );
        return null;
    }

    /**
     * Returns a group chat in progress from its unique ID
     * 
     * @param chatId Chat ID
     * @return Group chat or null if not found
     */
    public GroupChat getGroupChat(String chatId) {
        Log.i(TAG, "getGroupChat entry " + chatId);
        // TODO
        return null;
    }
   
    
    /**
     * Registers a chat invitation listener
     * 
     * @param listener New chat listener
     */
    public void addChatListener(NewGroupChatListener listener) {
        Log.i(TAG, "addChatListener entry" + listener);
        // TODO
    }

    /**
     * Unregisters a chat invitation listener
     * 
     * @param listener New chat listener
     */
    public void removeChatListener(NewGroupChatListener listener) {
        Log.i(TAG, "removeChatListener entry" + listener);
        // TODO
    }
}
