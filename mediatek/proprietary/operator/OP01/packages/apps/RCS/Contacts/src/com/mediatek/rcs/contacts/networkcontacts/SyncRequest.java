/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.rcs.contacts.networkcontacts;

/**
 * Contacts Backup/Restore sync request.
 *
 * @author MTK81350
 *
 */
public class SyncRequest {
    public static final int SYNC_BACKUP = 1;
    public static final int SYNC_RESTORE = 2;
    public static final int CHECK_RESTORE_RESULT = 3;
    public int mSyncType = -1;
    SyncNotify mNotify;
    /**
     * Prefer to be removed when merge with the same operation in queue
     * if mMergePrefer is true..
     * The caller must care about the result of being canceled by merge
     * if mMergePrefer is false.
     */
    boolean    mMergePrefer = false;

    /**
     * Interface to notify the state of the request.
     * @author MTK81350
     *
     */
    public interface SyncNotify {
        public static final int SYNC_STATE_START = 1;
        public static final int SYNC_STATE_SUCCESS = 0;
        public static final int SYNC_STATE_ERROR = -1;

        /**
         * sync complete with success and do nothing
         */
        public static final int SYNC_STATE_SUCCESS_DONOTHING = -2;
        /**
         * Merged with other same request.
         */
        public static final int SYNC_STATE_MERGED = -3;

        /**
         * @param state current state.
         * @param request request send notify.
         */
        public void onStateChange(int state, SyncRequest request);
    }

    /**
     * Constructor.
     * @param syncType  type of sync.
     *  @see {@link #SYNC_BACKUP} and {@link #SYNC_RESTORE}
     * @param notify notify interface.
     */
    public SyncRequest(int syncType, SyncNotify notify) {
        mSyncType = syncType;
        mNotify = notify;
    }

    /**
     * Constructor.
     *
     * @param syncType
     *            type of sync.
     * @see {@link #SYNC_BACKUP} and {@link #SYNC_RESTORE}
     * @param notify
     *            notify interface.
     * @param mergePrefer
     *            Prefer to be removed when merge with the same operation in
     *            queue if mMergePrefer is true.. The caller must care about the
     *            result of being canceled by merge if mMergePrefer is false.
     */
    public SyncRequest(int syncType, SyncNotify notify, boolean mergePrefer) {
        mSyncType = syncType;
        mNotify = notify;
        mMergePrefer = mergePrefer;
    }

    /**
     * @param state current state
     */
    public void notifyState(int state) {
        if (mNotify != null) {
            mNotify.onStateChange(state, this);
        }
    }

}
