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

import android.util.Log;

/**
 * Group chat
 * 
 * @author
 */
public class GroupChat {
    
    public static final String TAG = "DAPI-GroupChat";

    /**
     * Constructor
     */
    GroupChat() {
    }

    /**
     * delete message
     * 
     * @param msgId
     * @return if success ,return true; or not ,return false
     */ 
    public boolean deleteMessage(String msgId) {
        Log.i(TAG, "deleteMessage msgId " + msgId);
        // TODO
        return true;
    }

    /**
     * set read message
     * 
     * @param msgId
     * @return if success ,return true; or not ,return false
     */ 
    public boolean setMessageRead(String msgId) {
        Log.i(TAG, "setMessageRead msgId " + msgId);
        // TODO
        return true;
    }

    /**
     * 
     * @param msgId
     * @return
     */
    public boolean setMessageFavorite(String msgId) {
        Log.i(TAG, "setMessageFavorite msgId " + msgId);
        // TODO
        return true;
    }

    /**
     * 
     * @param msgId
     */
    public void resendMessage(String msgId) {
        Log.i(TAG, "resendMessage msgId " + msgId);
        // TODO
    }

    /**
     * Returns the chat ID
     * 
     * @return Chat ID
     */
    public String getChatId() {
        Log.i(TAG, "getChatId entry " );
        // TODO
        return null;
    }

    /**
     * Returns the subject of the group chat
     * 
     * @return Subject
     */
    public String getSubject() {
        Log.i(TAG, "getSubject() entry.");
        // TODO
        return null;
    }

    /**
     * Returns the list of connected participants. A participant is identified
     * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @return List of participants
     */
    public Set<String> getParticipants() {
        Log.i(TAG, "getParticipants() entry");
        // TODO
        return null;
    }

    /**
     * get the chairmen of the group
     * 
     * @return chairmen string
     */
    public String getChairmen() {
        Log.i(TAG, "getChairmen() entry");
        // TODO
        return null;
    }

    /**
     * transfer the role of chairmen to other participent
     * 
     * @param contact contact
     */
    public void setChairmen(String contact) {
        Log.i(TAG, "setChairmen() contact " + contact);
        // TODO
    }

    /**
     * 
     * @param subject
     */
    public void modifySubject(String subject) {
        Log.i(TAG, "modifySubject() subject " + subject);
        // TODO
    }

    /**
        * modify own nick name in group
        */
    public void modifyNickName(String nickname) {
        Log.i(TAG, "modifyNickName() nickname " + nickname);
        // TODO
    }

    /**
     * Returns the state of the group chat
     * 
     * @return State
     */
    public int getState() {
        Log.i(TAG, "getState() entry ");
        // TODO
        return 0;
    }

    /**
     * Accepts chat invitation
     *  
     */
    public void acceptInvitation() {
        Log.i(TAG, "acceptInvitation() entry ");
        // TODO
    }

    /**
     * Rejects chat invitation
     * 
     */
    public void rejectInvitation() {
        Log.i(TAG, "rejectInvitation() entry ");
        // TODO
    }

    /**
     * Sends a text message to the group
     * 
     * @param text Message
     * @return Unique message ID or null in case of error
     */
    public String sendMessage(String text) {
        Log.i(TAG, "sendMessage() text " + text);
        // TODO
        return null;
    }

    public GroupChatMessage getGroupChatMessage(String msgId) {
        Log.i(TAG, "getGroupChatMessage() msgId " + msgId);
        // TODO
        return null;
    }
    /**
     * Adds participants to a group chat
     * 
     * @param participants List of participants
     */
    public void addParticipants(Set<String> participants) {
        Log.i(TAG, "addParticipants() entry " + participants );
        // TODO
    }

    /**
     * Quits a group chat conversation. The conversation will continue between
     * other participants if there are enough participants.
     */
    public void quitConversation() {
        Log.i(TAG, "quitConversation() entry ");
        // TODO
    }

    /**
     * remove participants to a group chat
     * 
     * @param participants List of participants
     */
    public void removeParticipants(Set<String> participants) {
        Log.i(TAG, "removeParticipants() entry " + participants );
        // TODO
    }

    /**
     * return wether me is chairmen
     */

    public boolean isChairmen() {
        Log.i(TAG, "isChairmen() entry ");
        // TODO
        return false;
    }

    /**
     * get the last portrait of the participants. after get them, will callback 
     * by GroupChatListener.onPortTraitUpdate
     * 
     * @param participants List of participants
     */
    public void getPortrait(Set<String>participants) {
        Log.i(TAG, "getPortrait() entry ");
        // TODO
    }

    /** 
        * dissolve the groupchat, only chairmen can do it
        */
    public void abortConversation() {
        Log.i(TAG, "abortConversation() entry ");
        // TODO
    }

    /**
     * Adds a listener on chat events
     * 
     * @param listener Group chat event listener 
     */
    public void addEventListener(GroupChatListener listener) {
        Log.i(TAG, "addEventListener() entry " + listener);
        // TODO
    }
    
    /**
     * Removes a listener on chat events
     * 
     * @param listener Group chat event listener 
     */
    public void removeEventListener(GroupChatListener listener) {
        Log.i(TAG, "removeEventListener() entry " + listener);
        // TODO
    }

}
