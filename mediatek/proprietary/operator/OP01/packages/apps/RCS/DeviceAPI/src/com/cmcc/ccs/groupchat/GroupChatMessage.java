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

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Chat message
 * 
 * @author
 */
public class GroupChatMessage implements Parcelable {
    
    public static final String TAG = "DAPI-GroupChatMessage";
    /**
     * Unique message Id
     */
    private String mId;

    /**
     * Contact who has sent the message
     */
    private String mContact;

    /**
     * Receipt date of the message
     */
    private Date mReceiptAt;

    /**
     * message content
     */
    private String mMessageContent;

    /**
     * Constructor for outgoing message
     * 
     * @param contact Contact
     * @param messageId Message Id
     * @param receiptAt Receipt date
     * @param messageContent Message content
     * @hide
     */
    public GroupChatMessage(String contact, String id, Date receiptAt, String messageContent) {
        Log.i(TAG, "GroupChatMessage entry" + "messageId=" + id + " contact=" + contact + " message=" + messageContent + 
                " receiptAt=" + receiptAt);
        this.mContact = contact;
        this.mId = id;
        this.mReceiptAt = receiptAt;
        this.mMessageContent = messageContent;
    }

    /**
     * Constructor
     * 
     * @param source Parcelable source
     * @hide
     */
    public GroupChatMessage(Parcel source) {
        this.mContact = source.readString();
        this.mId = source.readString();
        this.mReceiptAt = new Date(source.readLong());
        this.mMessageContent = source.readString();
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
        dest.writeString(mContact);
        dest.writeString(mId);
        dest.writeLong(mReceiptAt.getTime());
        dest.writeString(mMessageContent);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<GroupChatMessage> CREATOR
            = new Parcelable.Creator<GroupChatMessage>() {
        public GroupChatMessage createFromParcel(Parcel source) {
            return new GroupChatMessage(source);
        }

        public GroupChatMessage[] newArray(int size) {
            return new GroupChatMessage[size];
        }
    };

    /**
     * Returns the contact
     * 
     * @return Contact
     */
    public String getContact() {
        return mContact;
    }

    /**
     * Returns the message ID
     * 
     * @return ID
     */
    public String getId(){
        return mId;
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
     * Returns the display name
     * 
     * @return String
     * @hide
     */
    public String getMessageContent() {
        return mMessageContent;
    }
}
