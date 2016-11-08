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

import android.util.Log;

import java.util.Vector;

/**
 * Manage Contacts AutoSync request queue:FIFO .
 * |SyncRequest0|SyncRequest1|SyncRequest2|...|
 *    head^                             tail^
 * @author MTK81350
 *
 */
public class SyncRequestQueue {
    private static final String TAG = "NetworkContacts::SyncRequestQueue";
    private Vector<SyncRequest> mRequestQueue;
    /**
     * constructor.
     */
    public SyncRequestQueue() {
        mRequestQueue = new Vector<SyncRequest>();
    }
    /**
     * Add the Sync request object at the end of the vector.
     * @param request the object to add to the vector.
     * @param current the request currently being synchronized.
     *
     * @return {@code true}add successfully{@code false}add fail
     */
    public boolean addRequest(SyncRequest request, SyncRequest current) {
        Log.d(TAG, "addRequest");
        if (request == null) {
            Log.e(TAG, "request is null");
            return false;
        }

        Vector<SyncRequest> toBeRemoved = new Vector<SyncRequest>();

        for (SyncRequest q : mRequestQueue) {
            if (q.mSyncType == request.mSyncType) {
                /* cancel the new request */
                if (q.mSyncType == SyncRequest.SYNC_RESTORE || q.mMergePrefer == false) {
                    Log.d(TAG, "new request is canceled!");
                    request.notifyState(SyncRequest.SyncNotify.SYNC_STATE_MERGED);
                    return false;
                } else {
                    /* use new replace the old for old is auto backup for auto backup has no UI */
                    toBeRemoved.add(q);
                }
            }
        }

        for (SyncRequest r : toBeRemoved) {
            /* don't delete the request being synchronized currently */
            if (r == current) {
                continue;
            }

            Log.d(TAG, "old request is removed!");
            r.notifyState(SyncRequest.SyncNotify.SYNC_STATE_MERGED);
            mRequestQueue.remove(r);
        }

        mRequestQueue.add(request);
        Log.e(TAG, "mRequestQueue.add");
        return true;
    }

    /**
     * remove the first Request from Sync Request Queue.
     * @param request request has been completed.
     */
    public void completeRequest(SyncRequest request) {
        mRequestQueue.remove(request);
    }

    /**
     * get the first Request from Sync Request Queue.
     * @return SyncRequest
     */
    public SyncRequest getFirstRequest() {
        SyncRequest request = null;
        if (!mRequestQueue.isEmpty()) {
            //get the first request from queue
            request = mRequestQueue.get(0);
        }
        return request;
    }

    /**
     * remove all Requests from Sync Request Queue.
     */
    public void removeAllRequest() {
        if (!mRequestQueue.isEmpty()) {
            //remove request from queue
            mRequestQueue.removeAllElements();
        }
    }

    /**
     * Judging whether the RequestQueue is empty.
     * @return {@code true} the Queue is Empty{@code false}the Queue is not empty
     */
    public boolean isEmpty() {
        if (mRequestQueue.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
