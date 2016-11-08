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

package com.mediatek.engineermode.audio;

import android.app.Activity;
import android.media.AudioSystem;
import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

/**
 * Audio Wake Lock main activity.
 * @author mtk
 */
public class AudioWakeLock extends Activity implements
        android.view.View.OnClickListener {
    private static final String TAG = "Audio/WakeLock";
    private static final String PARAM_WAKELOCK = "AudioTestWakelock";
    private static final String PARAM_VALUE_LOCK = "1";
    private static final String PARAM_VALUE_UNLOCK = "0";

    private ToggleButton mTbtnState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_wakelock);
        mTbtnState = (ToggleButton) findViewById(R.id.audio_wakelock_state_tbtn);
        boolean heldLock = isHeldWakeLock();
        updateWakeLockUi(heldLock);
        mTbtnState.setChecked(heldLock);
        mTbtnState.setOnClickListener(this);
    }

    private boolean isHeldWakeLock() {
        String param = AudioSystem.getParameters(PARAM_WAKELOCK);
        if (param == null) {
            Elog.d(TAG, "get null parameter of audio wake lock");
            return false;
        }
        String[] params = param.trim().split(" *; *");
        param = params[0];
        if (!param.contains(PARAM_WAKELOCK)) {
            Elog.d(TAG, "invalid audio wake lock parameter:" + param);
            return false;
        }
        String[] pairs = param.split(" *= *");
        if (pairs.length < 2) {
            Elog.d(TAG, "Invalid pairs length:" + param);
            return false;
        }
        String value = pairs[1].trim();
        if (PARAM_VALUE_LOCK.equals(value)) {
            return true;
        }
        return false;
    }

    private String buildWakeLockParam(boolean acquireLock) {
        String param = PARAM_WAKELOCK + "=";
        if (acquireLock) {
            param += PARAM_VALUE_LOCK;
        } else {
            param += PARAM_VALUE_UNLOCK;
        }
        return param;
    }

    private void enableAudioWakeLock(boolean acquireLock) {
        String param = buildWakeLockParam(acquireLock);
        Elog.d(TAG, "enableAudioWakelock " + acquireLock + " param:" + param);
        AudioSystem.setParameters(param);
    }

    private void updateWakeLockUi(boolean acquireLock) {
        int strId = 0;
        if (acquireLock) {
            strId = R.string.audio_wakelock_lock;
        } else {
            strId = R.string.audio_wakelock_unlock;
        }
        mTbtnState.setText(strId);
    }

    @Override
    public void onClick(View view) {
        if (view == mTbtnState) {
            boolean acquireLock = mTbtnState.isChecked();
            enableAudioWakeLock(acquireLock);
            updateWakeLockUi(acquireLock);
        }
    }
}
