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

package com.mediatek.mediatekdm;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.MmiConfirmation;
import com.mediatek.mediatekdm.mdm.MmiObserver;
import com.mediatek.mediatekdm.mdm.MmiResult;
import com.mediatek.mediatekdm.mdm.MmiViewContext;

/**
 * TODO: This class is only used by DmService. Should it be an internal class of DmService?
 */
public class DmAlertConfirm implements MmiConfirmation {

    private DmService mService;
    private MmiObserver mObserver;

    public DmAlertConfirm(DmService service, MmiObserver observer) {
        mService = service;
        mObserver = observer;
        Log.i(TAG.MMI, "MmiConfirmation constructed");
    }

    /**
     * Notify UI to display alert1101
     *
     * @param context
     *        view context, text message and proposed display time, etc.
     * @param command
     *        Default result.
     */
    public MmiResult display(MmiViewContext context, ConfirmCommand command) {
        Log.i(TAG.MMI, "+MmiConfirmation.display()");
        Log.i(TAG.MMI, "text: " + context.displayText);
        if (DmApplication.getInstance().forceSilentMode()) {
            Log.w(TAG.MMI, "Alert is skipped in silent mode");
            mService.getHandler().post(new Runnable() {
                public void run() {
                    confirm();
                }
            });
        } else {
            mService.showAlertConfirm(context);
        }
        Log.i(TAG.MMI, "-MmiConfirmation.display()");
        return MmiResult.OK;
    }

    /**
     * Pass the confirm result from UI to engine.
     *
     * @param result
     */
    public void confirm() {
        mObserver.notifyConfirmationResult(true);
    }

    public void cancel() {
        mObserver.notifyConfirmationResult(false);
    }

    public void timeout() {
        mObserver.notifyTimeoutEvent();
    }
}
