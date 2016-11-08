/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.ims;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

//import com.mediatek.ims.ImsAdapter;
import com.mediatek.ims.ImsAdapter.VaEvent;
import com.mediatek.ims.ImsAdapter.VaSocketIO;

import com.mediatek.ims.internal.ImsSimservsDispatcher;
import com.mediatek.ims.internal.TimerDispatcher;
import com.mediatek.ims.internal.CallControlDispatcher;
import com.mediatek.ims.internal.DataDispatcher;

import static com.mediatek.ims.VaConstants.*;

import java.util.ArrayList;




public class ImsEventDispatcher extends Handler {

    private Context mContext;
    private VaSocketIO mSocket;
    private ArrayList<VaEventDispatcher> mVaEventDispatcher = new ArrayList<VaEventDispatcher>();
    private static final String TAG = "ImsEventDispatcher";

    public ImsEventDispatcher(Context context, VaSocketIO IO) {
        mContext = context;
        mSocket = IO;

        createDispatcher();
    }

    public interface VaEventDispatcher {
        void vaEventCallback(VaEvent event);
        void enableRequest();
        void disableRequest();
    }

    void enableRequest() {
        for (VaEventDispatcher dispatcher : mVaEventDispatcher) {
            dispatcher.enableRequest();
        }
    }

    void disableRequest() {
        for (VaEventDispatcher dispatcher : mVaEventDispatcher) {
            dispatcher.disableRequest();
        }
    }

    /* modify the following for domain owners */

    /* Event Dispatcher */
    private CallControlDispatcher mCallControlDispatcher;
    private DataDispatcher mDataDispatcher;
    private TimerDispatcher mTimerDispatcher;
    private ImsSimservsDispatcher mSimservsDispatcher;

    private void createDispatcher() {

        mCallControlDispatcher = new CallControlDispatcher(mContext, mSocket);
        mVaEventDispatcher.add(mCallControlDispatcher);

        mDataDispatcher = new DataDispatcher(mContext, mSocket);
        mVaEventDispatcher.add(mDataDispatcher);

        mTimerDispatcher = new TimerDispatcher(mContext, mSocket);
        mVaEventDispatcher.add(mTimerDispatcher);

        mSimservsDispatcher = new ImsSimservsDispatcher(mContext, mSocket);
        mVaEventDispatcher.add(mSimservsDispatcher);
    }

    @Override
    public void handleMessage(Message msg) {
        dispatchCallback((VaEvent) msg.obj);
    }

    /* dispatch Callback */
    void dispatchCallback(VaEvent event) {

        switch (event.getRequestID()) {
            case MSG_ID_NOTIFY_XUI_IND:
                mSimservsDispatcher.vaEventCallback(event);
                break;

            case MSG_ID_NOTIFY_SS_PROGRESS_INDICATION :
                mCallControlDispatcher.vaEventCallback(event);
                break;

            case MSG_ID_REQUEST_PCSCF_DISCOVERY:
            case MSG_ID_WRAP_IMSM_IMSPA_PDN_ACT_REQ:
            case MSG_ID_WRAP_IMSM_IMSPA_INFORMATION_REQ:
            case MSG_ID_WRAP_IMSM_IMSPA_PDN_DEACT_REQ:
                mDataDispatcher.vaEventCallback(event);
                break;

            case MSG_ID_REQUEST_TIMER_CREATE:
            case MSG_ID_REQUEST_TIMER_CANCEL:
                mTimerDispatcher.vaEventCallback(event);
                break;
        }
    }
}
