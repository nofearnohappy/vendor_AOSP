/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.bluetoothle.bleservice;

import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An internal utility class to record each operations in a queue Thus, the GATT callback can be
 * dispatched to a profile instance in a client by the order according to what's saved in the queue.
 */

public class ClientReqQueue {

    private static final String TAG = "ClientReqQueue";
    private static final boolean DBG = true;

    private final SparseArray<ConcurrentLinkedQueue<Pair<Integer, Integer>>> mQueueMap =
            new SparseArray<ConcurrentLinkedQueue<Pair<Integer, Integer>>>();

    // Operation IDs
    static final int REQ_READ_CHAR = 0;
    static final int REQ_READ_DESC = 1;
    static final int REQ_READ_RSSI = 2;
    static final int REQ_SET_CHAR_NOTIFY = 3;
    static final int REQ_WRITE_CHAR = 4;
    static final int REQ_WRITE_DESC = 5;
    static final int REQ_RELIABLE_WRITE = 6;

    public ClientReqQueue() {
        // Init queues for each operation
        mQueueMap.put(REQ_READ_CHAR, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
        mQueueMap.put(REQ_READ_DESC, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
        mQueueMap.put(REQ_READ_RSSI, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
        mQueueMap.put(REQ_SET_CHAR_NOTIFY, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
        mQueueMap.put(REQ_WRITE_CHAR, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
        mQueueMap.put(REQ_WRITE_DESC, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
        mQueueMap.put(REQ_RELIABLE_WRITE, new ConcurrentLinkedQueue<Pair<Integer, Integer>>());
    }

    public void onClientReq(final int reqId, final int clientId, final int profileId) {
        final Pair<Integer, Integer> compositeId = new Pair<Integer, Integer>(clientId, profileId);

        final ConcurrentLinkedQueue<Pair<Integer, Integer>> clientQueue = mQueueMap.get(reqId);
        clientQueue.add(compositeId);

        if (DBG) {
            Log.d(TAG, "onClientReq: operation = " + reqId + ", clientID = " + compositeId.first
                    + ", profileID = " + compositeId.second + ", queue = " + clientQueue);
        }
    }

    public Pair<Integer, Integer> onClientRsp(final int reqId) {
        final ConcurrentLinkedQueue<Pair<Integer, Integer>> clientQueue = mQueueMap.get(reqId);

        if (DBG) {
            Log.d(TAG, "onClientRsp: operation = " + reqId + ", queue = " + clientQueue);
        }

        return clientQueue.remove();
    }
}
