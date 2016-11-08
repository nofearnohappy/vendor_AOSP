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

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.Set;

/**
 * Chat message
 * 
 * @author
 */
public class ChatMessage implements Parcelable {

    /**
     * Direction
     */
    public static final int INCOMING            = -5;
    public static final int OUTCOMING           = -6;

    /**
     * status
     */
    public static final int UNREAD              = 1;
    public static final int READ                = 2;
    public static final int SENDING             = 3;
    public static final int SENT                = 4;
    public static final int FAILED              = 5;
    public static final int TO_SEND             = 6;

    /**
     * FLAG
     */
    public static final int OTO                 = 1;
    public static final int OTM                 = 2;
    public static final int MTM                 = 3;
    public static final int PUBLIC              = 4;

    /**
     * database column
     */
    public static final String MESSAGE_ID       = "CHATMESSAGE_MESSAGE_ID";
    public static final String CONTACT_NUMBER   = "CHATMESSAGE_CONTACT_NUMBER";
    public static final String BODY             = "CHATMESSAGE_BODY";
    public static final String TIMESTAMP        = "CHATMESSAGE_TIMESTAMP";
    public static final String MIME_TYPE        = "CHATMESSAGE_MIME_TYPE";
    public static final String MESSAGE_STATUS   = "CHATMESSAGE_MESSAGE_STATUS";
    public static final String DIRECTION        = "CHATMESSAGE_DIRECTION";
    public static final String FAVORITE         = "CHATMESSAGE_FAVORITE";
    public static final String TYPE             = "CHATMESSAGE_TYPE";
    public static final String FLAG             = "CHATMESSAGE_FLAG";
    public static final String FILENAME         = "CHATMESSAGE_FILENAME";
    public static final String FILEICON         = "CHATMESSAGE_FILEICON";
    public static final String FILESIZE         = "CHATMESSAGE_FILESIZE";

    public static final String TAG = "DAPI-ChatMessage";
    /**
     * Unique message Id
     */
    private String mId;

    /**
     * Contact who has sent the message
     */
    private String mContact;
    
    /**
     * Message content
     */
    private String mMessage;
    
    /**
     * Receipt date of the message
     */
    private Date mReceiptAt;

    /**
     * Contacts who has sent the message
     */
    private Set<String> mContacts;
    
    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param contact Contact
     * @param message Message content
     * @param receiptAt Receipt date
     * @param displayName displayName
     * @hide
     */
    public ChatMessage(String messageId, String remote, String message, Date receiptAt) {
        Log.i(TAG, "ChatMessage entry" + "messageId=" + messageId + " remote=" + remote + " message=" + message + 
                " receiptAt=" + receiptAt);
        this.mId = messageId;
        this.mContact = remote;
        this.mMessage = message;
        this.mReceiptAt = receiptAt;
    }
    
    /**
     * Constructor
     * 
     * @param source Parcelable source
     * @hide
     */
    public ChatMessage(Parcel source) {
        this.mId = source.readString();
        this.mContact = source.readString();
        this.mMessage = source.readString();
        this.mReceiptAt = new Date(source.readLong());
    }
    
    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation
     * 
     * @return Integer
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Write parcelable object
     * 
     * @param dest The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mContact);
        dest.writeString(mMessage);
        dest.writeLong(mReceiptAt.getTime());
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<ChatMessage> CREATOR
            = new Parcelable.Creator<ChatMessage>() {
        public ChatMessage createFromParcel(Parcel source) {
            return new ChatMessage(source);
        }

        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };    
    
    /**
     * Returns the message ID
     * 
     * @return ID
     */
    public String getId(){
        return mId;
    }

    /**
     * Returns the contact
     * 
     * @return Contact
     */
    public String getContact() {
        return mContact;
    }

    /**
     * Returns the message content
     * 
     * @return String
     * @hide
     */

    public String getMessage() {
        return mMessage;
    }

    /**
     * Returns the receipt date
     * 
     * @return Date
     */
    public Date getReceiptDate() {
        return mReceiptAt;
    }

    /**
     * Returns the contacts
     * 
     * @return Contacts
     */
    public Set<String> getContacts() {
        return mContacts;
    }

}
