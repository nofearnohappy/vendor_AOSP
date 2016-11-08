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

/**
 * Chat event listener
 * 
 * @author 
 */
public interface ChatListener {

    /**
     * Callback called when a new message has been received
     * 
     * @Contact contact
     * @param message Chat message
     * @see ChatMessage
     */
    public void onNewChatMessage(String contact, ChatMessage message);

    /**
     * Callback called when a message has been delivered to the remote
     * 
     * @param msgId Message ID
     */
    public void onReportMessageDelivered(String msgId);

    /**
     * Callback called when a message has failed to be delivered to the remote
     * 
     * @param msgId Message ID
     */
    public void onReportMessageFailed(String msgId, int errType, String statusCode);
}
