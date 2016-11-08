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

package com.mediatek.cb.cbsettings;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import static com.mediatek.cb.cbsettings.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;

import com.android.mms.R;
import com.android.mms.util.MmsLog;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import android.telephony.SmsManager;

import java.util.ArrayList;

public class CellBroadcastCheckBox extends CheckBoxPreference {
    private static final String LOG_TAG = "Mms/CellBroadcastCheckBox";
    private static final boolean DBG = true;
    private static final int QUERY_CBSMS_ACTIVATION = 100;

    public TimeConsumingPreferenceListener mListener;
    public boolean mIsCbEanbled = false;
    int mSubId;

    public CellBroadcastCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case QUERY_CBSMS_ACTIVATION:
                setChecked(mIsCbEanbled);
                setSummary(mIsCbEanbled ? R.string.sum_cell_broadcast_control_on
                        : R.string.sum_cell_broadcast_control_off);
                break;
            default:
                MmsLog.d(LOG_TAG, "mUiHandler msg unhandled.");
                break;
            }
        }
    };

    @Override
    protected void onClick() {
        super.onClick();
        boolean state = isChecked();
        setCBState(state);
        setChecked(state);
    }

    public void init(int subId, TimeConsumingPreferenceListener listener, boolean skipReading) {
        MmsLog.d(LOG_TAG, "init, subId = " + subId);
        mListener = listener;
        mSubId = subId;

        if (!skipReading) {
            boolean hasIccCard;
            hasIccCard = hasIccCard(mSubId);
            MmsLog.d(LOG_TAG, "hasIccCard = " + hasIccCard);
            if (hasIccCard) {
                getCBState();
                setEnabled(true);
            } else {
                setChecked(false);
                setEnabled(false);
            }
        }
    }

    private void getCBState() {
        MmsLog.d(LOG_TAG, "getCBState start");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mIsCbEanbled = SmsManager.getSmsManagerForSubscriptionId(mSubId)
                        .queryCellBroadcastSmsActivation();
                mUiHandler.sendEmptyMessage(QUERY_CBSMS_ACTIVATION);
                MmsLog.d(LOG_TAG, "getCBState end mIsCbEanbled = " + mIsCbEanbled);
            }
        }).start();
    }

    private void setCBState(final boolean state) {
        MmsLog.d(LOG_TAG, "setCBState start");
        if (mListener != null) {
            mListener.onStarted(CellBroadcastCheckBox.this, false);
        }
        MmsLog.d(LOG_TAG, "activateCellBroadcastSms start");
        boolean isSetSuccess = SmsManager.getSmsManagerForSubscriptionId(mSubId)
                .activateCellBroadcastSms(state);
        MmsLog.d(LOG_TAG, "activateCellBroadcastSms end isSetSuccess = " + isSetSuccess);
        if (!isSetSuccess) {
            handleSetStateResponse();
        } else {
            new Thread(new Runnable() {
                public void run() {
                        RecoverChannelSettings setting =
                                new RecoverChannelSettings(mSubId,
                                        getContext().getContentResolver());
                        setting.updateChannelStatus();
                        if (mListener != null) {
                            mListener.onFinished(CellBroadcastCheckBox.this, false);
                        }
                    MmsLog.d(LOG_TAG, "setCBState end");
                }
            }).start();
        }
    }

    private void handleSetStateResponse() {
        if (mListener != null) {
            mListener.onFinished(CellBroadcastCheckBox.this, false);
            mListener.onError(CellBroadcastCheckBox.this, EXCEPTION_ERROR);
        }
        getCBState();
        return;
    }

    /**
     * @return true if a ICC card is present
     *
     * @param slotId
     * @return
     */
    public boolean hasIccCard(int subId) {
        boolean hasIccCard;
        if (subId > 0) {
            int slotId = SubscriptionManager.getSlotId(subId);
            TelephonyManager manager = TelephonyManager.getDefault();
            hasIccCard = manager.hasIccCard(slotId);
        } else {
            hasIccCard = TelephonyManager.getDefault().hasIccCard();
        }
        MmsLog.d(LOG_TAG, "[hasIccCard], subId = " + subId + "; hasIccCard = " + hasIccCard);
        return hasIccCard;
    }
}

class RecoverChannelSettings {

    private static final String LOG_TAG = "RecoverChannelSettings";
    private static final String KEYID = "_id";
    private static final String NAME = "name";
    private static final String NUMBER = "number";
    private static final String ENABLE = "enable";
    private static final String SUBID = "sub_id";
    private static final Uri CHANNEL_URI = Uri.parse("content://cb/channel");

    private Uri mUri = CHANNEL_URI;
    private int mSubId;
    private ContentResolver mResolver = null;

    public RecoverChannelSettings(int subId, ContentResolver resolver) {
        mSubId = subId;
        this.mResolver = resolver;
        mUri = Uri.parse("content://cb/channel" + mSubId);;
    }

    private ArrayList<CellBroadcastChannel> mChannelArray = new ArrayList<CellBroadcastChannel>();

    private boolean updateChannelToDatabase(int index) {
        MmsLog.d(LOG_TAG, "updateChannelToDatabase start");
        final CellBroadcastChannel channel = mChannelArray.get(index);
        final int id = channel.getKeyId();
        final String name = channel.getChannelName();
        final boolean enable = channel.getChannelState();
        final int number = channel.getChannelId();
        final int subId = channel.getChannelSubId();
        ContentValues values = new ContentValues();
        values.put(KEYID, id);
        values.put(NAME, name);
        values.put(NUMBER, number);
        values.put(ENABLE, Integer.valueOf(enable ? 1 : 0));
        values.put(SUBID, subId);
        String where = KEYID + "=" + channel.getKeyId();
        final int rows = mResolver.update(mUri, values, where, null);
        MmsLog.d(LOG_TAG, "updateChannelToDatabase end rows =" + rows);
        return rows > 0;
    }

    boolean queryChannelFromDatabase() {
        MmsLog.d(LOG_TAG, "queryChannelFromDatabase start");
        String[] projection = new String[] { KEYID, NAME, NUMBER, ENABLE, SUBID };
        Cursor cursor = null;
        try {
            cursor = mResolver.query(mUri, projection, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    CellBroadcastChannel channel = new CellBroadcastChannel();
                    channel.setChannelId(cursor.getInt(2));
                    channel.setKeyId(cursor.getInt(0)); // keyid for delete or edit
                    channel.setChannelName(cursor.getString(1));
                    channel.setChannelState(cursor.getInt(3) == 1);
                    channel.setChannelSubId(cursor.getInt(4));
                    mChannelArray.add(channel);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        MmsLog.d(LOG_TAG, "queryChannelFromDatabase end");
        return true;
    }

    /**
     * when enable channels, we set once that these channels which are enable
     * and channelId neighboring in the DB to reduce times to reduce API.
     * eg: the channel id maybe is 1(true),2(true),3(false) ,4(true), 5(false),6(true)
     * we send three times (1,2; 4; 6)
      */
    public void updateChannelStatus() {
        MmsLog.d(LOG_TAG, "updateChannelStatus start");
        if (!queryChannelFromDatabase()) {
            MmsLog.d(LOG_TAG, "queryChannelFromDatabase failure!");
            return ;
        }
        int length = mChannelArray.size();
        MmsLog.d(LOG_TAG, "updateChannelStatus length: " + length);
        SmsBroadcastConfigInfo infoList = null;
        int channelId = -1;
        boolean channelState;
        for (int i = 0; i < length; i++) {
            channelId = mChannelArray.get(i).getChannelId();
            channelState = mChannelArray.get(i).getChannelState();
            if (channelState) {
                if (infoList == null) {
                    infoList = new SmsBroadcastConfigInfo(channelId, channelId, -1, -1, true);
                } else if (infoList.getToServiceId() != (channelId - 1)) {
                    SmsBroadcastConfigInfo[] info = new SmsBroadcastConfigInfo[1];
                    info[0] = infoList;
                    setCellBroadcastConfig(info,
                            infoList.getFromServiceId(), infoList.getToServiceId());
                    infoList = new SmsBroadcastConfigInfo(channelId, channelId, -1, -1, true);
                } else {
                    infoList.setToServiceId(channelId);
                }
            }
        }
        if (infoList != null) {
            MmsLog.d(LOG_TAG, "updateChannelStatus last times");
            SmsBroadcastConfigInfo[] info = new SmsBroadcastConfigInfo[1];
            info[0] = infoList;
            setCellBroadcastConfig(info, infoList.getFromServiceId(), infoList.getToServiceId());
        }
        MmsLog.d(LOG_TAG, "updateChannelStatus end");
    }

    private void setCellBroadcastConfig(SmsBroadcastConfigInfo[] objectList, int fromId, int toId) {
        MmsLog.d(LOG_TAG, "setCellBroadcastConfig start");
        boolean isSetConfigSuccess = SmsManager.getSmsManagerForSubscriptionId(mSubId)
                .setCellBroadcastSmsConfig(objectList, objectList);
        MmsLog.d(LOG_TAG, "setCellBroadcastConfig end isSetConfigSuccess =" + isSetConfigSuccess);
        if (isSetConfigSuccess) {
            handleSetCellBroadcastConfigResponse(fromId, toId);
        }
    }

    private void handleSetCellBroadcastConfigResponse(int fromId, int toId) {
        MmsLog.d(LOG_TAG, "handleSetCellBroadcastConfigResponse: exception");
        int fromIndex = -1;
        int toIndex = -1;
        int length = mChannelArray.size();
        int channelId;
        boolean channelState;
        // find the exception team begin channel id index and last channel id index
        for (int i = 0; i < length; i++) {
            channelId = mChannelArray.get(i).getChannelId();
            if (channelId == toId) {
                toIndex = i;
            }
            if (channelId == fromId) {
                fromIndex = i;
            }
        }
        if (fromIndex == -1 || toIndex == -1) {
            return;
        }
        for (int i = toIndex; i >= fromIndex; i--) {
            this.updateChannelToDatabase(i);
        }
    }
}
