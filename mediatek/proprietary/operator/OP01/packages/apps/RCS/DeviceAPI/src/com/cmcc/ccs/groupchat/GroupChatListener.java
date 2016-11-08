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

import com.cmcc.ccs.chat.ChatMessage;
/**
 * Group chat event listener
 */
public interface GroupChatListener {
	/**
	 * Callback called when the session is well established and messages
	 * may be exchanged with the group of participants
	 */
	public void onSessionStarted();
	
	/**
	 * Callback called when a new message has been received
	 * 
	 * @param message New chat message
	 * @see ChatMessage
	 */
	public void onNewMessage(GroupChatMessage message);
	
	/**
	 * Callback called when a message has failed to be delivered to the remote
	 * 
	 * @param msgId Message ID
	 */
	public void onReportMessageFailed(String msgId, int errType, String statusCode);
	
	/**
	 * Callback called when a message has been delivered to the remote
	 * 
	 * @param msgId Message ID
	 */
	public void onReportMessageDelivered(String msgId);
	
	/**
	 * Callback called when the session has been aborted or terminated
	 */
	public void onSessionAborted(int errType, String statusCode);

	/**
	 * Callback called when the session has failed
	 * 
	 * @param error Error
	 * @see GroupChat.Error
	 */
	public void onSessionError(int error);

	/**
	 * Callback called when a new participant has joined the group chat
	 *  
	 * @param contact Contact
	 */
	public void onParticipantJoined(String contact);
	
	/**
	 * Callback called when a participant has left voluntary the group chat
	 *  
	 * @param contact Contact
	 */
	public void onParticipantLeft(String contact);

	/**
	 * Callback called when Chairmen changed
	 * 
	 * @param contact Contact
	 */
	public void onChairmenChanged(String contact, int errType, String statusCode);

	/**
	 * 
	 */
    public void onMeRemoved();

    /**
     * 
     * @param errType
     * @param statusCode
     */
    public void onPortraitUpdate(int errType, String statusCode);

    /**
     * 
     * @param errType
     * @param statusCode
     */
    public void onInviteParticipants(int errType, String statusCode);

    /**
     * 
     * @param errType
     * @param statusCode
     */
    public void onRemoveParticipants(int errType, String statusCode);

    /**
     * 
     * @param errType
     * @param statusCode
     */
    public void onQuitCoversation(int errType, String statusCode);

}
