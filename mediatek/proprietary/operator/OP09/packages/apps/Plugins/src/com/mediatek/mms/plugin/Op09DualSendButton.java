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

package com.mediatek.mms.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mediatek.op09.plugin.R;

/**
 * Op09DualSendButton. This module manage the dual send button.
 * when sub count is two, show a big send button and a small send button;
 * when sub count is one, only show a big send button;
 * when sub count is zero, show origin button, caller can set current message type, if the message
 * is sms, show sms origin button, else show mms origin button.
 */
public class Op09DualSendButton {
    private static final String TAG = "Mms/Op09DualSendButton";

    private Context mUIContext = null;
    private Context mResourceContext;
    /// M: the big button for new dual btns;
    private ImageButton mSmallButton = null;
    /// M: the small button for new dual btns;
    private ImageButton mBigButton = null;
    private TextView mTextCounter = null;
    private OnClickListener mOnClickListener;
    private boolean mEnabled = false;

    /**
     * The Constructor.
     * @param context the resource Context.
     */
    public Op09DualSendButton(Context context) {
        mResourceContext = context;
    }

    private View.OnClickListener mDualButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int sendSubId = -1;
            Integer object = (Integer) v.getTag();
            sendSubId = object == null ? -1 : object;
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v, sendSubId);
            }
        }
    };

    /**
     * Init view.
     * @param context UI context, must not be null.
     * @param smallButton the small button, must not be null.
     * @param bigButton the big button, must not be null.
     * @param counter text counter
     */
    public void initView(Context context, ImageButton smallButton, ImageButton bigButton,
                            TextView counter) {
        if (context == null || smallButton == null || bigButton == null) {
            throw new RuntimeException("context or smallButton or bigButton is null");
        }
        mUIContext = context;
        mBigButton = bigButton;
        mSmallButton = smallButton;
        mTextCounter = counter;
        ///M: set dual send button initial default state. @{
        mBigButton.setVisibility(View.VISIBLE);
        mSmallButton.setVisibility(View.VISIBLE);
        mBigButton.setOnClickListener(mDualButtonOnClickListener);
        mSmallButton.setOnClickListener(mDualButtonOnClickListener);
        mBigButton.setEnabled(mEnabled);
        mSmallButton.setEnabled(mEnabled);
        /// @}
    }

    /**
     * Update Send button. That will redraw the send button.
     */
    public void updateSendButton() {
        int bigSendButtonBackgroundRes = 0;
        int smallSendButtonBackgroundRes = 0;
        Bitmap smallSendButtonBitmap = null;
        Bitmap bigSendButtonBitmap = null;
        SubscriptionInfo secondSimInfo = null;
        SubscriptionManager subManager = SubscriptionManager.from(mUIContext);

        int [] subIdList = subManager.getActiveSubscriptionIdList();
        Log.d(TAG, "updateSendButton, subIdList.length = " + subIdList.length);
        if (mEnabled) {
            bigSendButtonBackgroundRes = R.drawable.ct_send_big;
            smallSendButtonBackgroundRes = R.drawable.ct_send_small;
        } else {
            bigSendButtonBackgroundRes = R.drawable.ct_send_big_disable;
            smallSendButtonBackgroundRes = R.drawable.ct_send_small_disable;
        }
        if (subIdList.length == 0) {
            Log.d(TAG, "setDualSendButtonType Failed, as simCardSize = 0;");
            mBigButton.setVisibility(View.GONE);
            mSmallButton.setVisibility(View.GONE);
            return;
        } else if (subIdList.length == 1) {
            mBigButton.setVisibility(View.VISIBLE);
            mSmallButton.setVisibility(View.GONE);
            int defaultSubId = subIdList[0];
            bigSendButtonBitmap = Op09MmsUtils.createSendButtonBitMap(mResourceContext,
                    bigSendButtonBackgroundRes, defaultSubId, mEnabled);
            mBigButton.setTag(defaultSubId);
        } else if (subIdList.length == 2) {
            mBigButton.setVisibility(View.VISIBLE);
            mSmallButton.setVisibility(View.VISIBLE);
            int defaultSubId = SubscriptionManager.getDefaultSmsSubId();
            if (!SubscriptionManager.isValidSubscriptionId(defaultSubId) ||
                    subManager.getActiveSubscriptionInfo(defaultSubId) == null) {
                 defaultSubId = subIdList[0];
            }
            Log.d(TAG, "default SIM Sub Id:" + defaultSubId);
            bigSendButtonBitmap = Op09MmsUtils.createSendButtonBitMap(mResourceContext,
                    bigSendButtonBackgroundRes, defaultSubId, mEnabled);
            SubscriptionInfo defaultSimInfo = subManager.getActiveSubscriptionInfo(defaultSubId);
            int defaultSlotId = defaultSimInfo.getSimSlotIndex();
            if (defaultSlotId == 0) {
                secondSimInfo = subManager.getActiveSubscriptionInfoForSimSlotIndex(1);
            } else {
                secondSimInfo = subManager.getActiveSubscriptionInfoForSimSlotIndex(0);
            }
            int secondSubId = secondSimInfo.getSubscriptionId();
            smallSendButtonBitmap = Op09MmsUtils.createSendButtonBitMap(mResourceContext,
                    smallSendButtonBackgroundRes, secondSubId, mEnabled);
            mBigButton.setTag(defaultSubId);
            mSmallButton.setTag(secondSubId);
        }
        mBigButton.setImageBitmap(bigSendButtonBitmap);
        mSmallButton.setImageBitmap(smallSendButtonBitmap);
    }

    /**
     * Enable or disable send button.
     * @param enabled boolean
     */
    public void setEnabled(boolean enabled) {
        mBigButton.setEnabled(enabled);
        mSmallButton.setEnabled(enabled);
        if (mEnabled != enabled) {
            mEnabled = enabled;
            updateSendButton(); // when enable state changed, need redraw button as new state.
        }
    }

    /**
     * updateTextCounter.
     * @param isMms if isMms is true, the counter show mms, else show remain message.
     * @param remainingInCurrentMessage  how many chars the editing sms can add.
     * @param msgCount the editing SMS count
     */
    public void updateTextCounter(boolean isMms, int remainingInCurrentMessage, int msgCount) {
        if (mTextCounter == null) {
            return;
        }
        if (isMms) {
            mTextCounter.setText(mResourceContext.getResources().getString(R.string.mms));
            mTextCounter.setVisibility(View.VISIBLE);
            return;
        }
        String counterText = remainingInCurrentMessage + "/" + msgCount;
        mTextCounter.setText(counterText);
        mTextCounter.setVisibility(View.VISIBLE);
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    /**
     * OnClickListener.
     *
     */
    public interface OnClickListener {

        /**
         * OnClick.
         * @param view  click view
         * @param subId the subId for send message.
         */
        void onClick(View view, int subId);
    }
}
