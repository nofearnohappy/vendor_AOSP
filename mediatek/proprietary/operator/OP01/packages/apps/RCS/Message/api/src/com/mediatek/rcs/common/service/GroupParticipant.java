/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.rcs.common.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.mediatek.rcs.common.utils.Logger;

/**
 * The Group chat participant information
 */
public class GroupParticipant implements Parcelable {
    public static final String TAG = "GroupChat";

    public final static String STATE_UNKNOWN = "unknown";
    public final static String STATE_CONNECTING = "connecting";
    public final static String STATE_CONNECTED = "connected";
    public final static String STATE_DISCONNECTING = "disconnecting";
    public final static String STATE_DISCONNECTED = "disconnected";
    public final static String STATE_DEPARTED = "departed";
    public final static String STATE_DEPARTING = "departing";
    public final static String STATE_BOOTED = "booted";
    public final static String STATE_FAILED = "failed";
    public final static String STATE_BUSY = "busy";
    public final static String STATE_DECLINED = "declined";
    public final static String STATE_PENDING = "pending";

    Participant mParticipant;
    String mState = STATE_UNKNOWN;
    String mOriginalState = STATE_UNKNOWN;
    String mChatID = "";

    public String getChatID() {
        return mChatID;
    }

    public void setChatID(String mChatID) {
        this.mChatID = mChatID;
    }

    public GroupParticipant(Participant participant, String state) {
        if (null == participant) {
            Logger.w(TAG, "GroupParticipant participant is null");
        }
        mParticipant = participant;
        mState = state;
    }

    public GroupParticipant(Parcel source) {
        mParticipant = new Participant(source.readString(), source.readString());
        mState = source.readString();
    }

    /**
     * Get participant Return group chat participant
     */
    public Participant getParticipant() {
        return mParticipant;
    }

    /**
     * Get contact Return group chat participant contact
     */
    public String getContact() {
        if (null != mParticipant) {
            return mParticipant.getContact();
        } else {
            Logger.w(TAG, "ParticipantInfo#getContact() mParticipant is null");
            return null;
        }
    }

    /**
     * Get state Return group chat participant state
     */
    public String getState() {
        return mState;
    }

    public void setState(String state){
        mState = state;
    }

    public String getOriginalState(){
        return mOriginalState;
    }

    public void setOriginalState(String state){
        mOriginalState = state;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mParticipant.getContact());
        dest.writeString(mParticipant.getDisplayName());
        dest.writeString(mState);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<GroupParticipant> CREATOR =
            new Parcelable.Creator<GroupParticipant>() {
        public GroupParticipant createFromParcel(Parcel source) {
            return new GroupParticipant(source);
        }

        public GroupParticipant[] newArray(int size) {
            return new GroupParticipant[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Contact: ");
        builder.append(mParticipant.getContact());
        builder.append("    DisplayName: ");
        builder.append(mParticipant.getDisplayName());
        builder.append("    State: ");
        builder.append(mState);
        return builder.toString();
    }
}

