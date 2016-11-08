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

package com.mediatek.engineermode.lte;

import android.app.Activity;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class LteNetworkInfoDetail extends Activity implements OnClickListener {
    private static final String TAG = "EM/LteNetworkInfo";
    private static final int MSG_NW_INFO = 1;
    private static final int MSG_NW_INFO_LTEDC = 2;
    private static final int MSG_NW_INFO_URC = 3;
    private static final int MSG_NW_INFO_OPEN = 4;
    private static final int MSG_NW_INFO_CLOSE = 5;
    private static final int MSG_UPDATE_UI = 5;
    private static final int TOTAL_TIMER = 1000;
    private static final int FLAG_OR_DATA = 0xFFFFFFF7;
    private static final int FLAG_OFFSET_BIT = 0x08;
    private static final int FLAG_DATA_BIT = 8;
    private static final int NWSEL_EM_TIMER_INFO = 358;

    private Button mPageUp;
    private Button mPageDown;
    private TextView mInfo;
    private Toast mToast = null;

    private int mItemCount = 0;
    private int mCurrentItem = 0;
    private int mSimType;
    private Phone mPhone = null;
    private Timer mTimer = new Timer();
    private int mFlag = 0;

    static class NetworkInfoPage {
        public int page;
        public String label;
        public int[] types;

        NetworkInfoPage(int page, String label, int[] types) {
            this.page = page;
            this.label = label;
            this.types = types;
        }
    }

    private HashMap<Integer, String> mNetworkInfo = new HashMap<Integer, String>();
    private NetworkInfoPage[] mItem = null;
    private String mTimerInfo = null;
    private long mTimerInfoStart = 0;

    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case MSG_NW_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    Log.v("@M_" + TAG, "data[0] is : " + data[0]);
                    Log.v("@M_" + TAG, "flag is : " + data[0].substring(FLAG_DATA_BIT));
                    mFlag = Integer.valueOf(data[0].substring(FLAG_DATA_BIT));
                    mFlag = mFlag | FLAG_OFFSET_BIT;
                    Log.v("@M_" + TAG, "flag change is : " + mFlag);
                    for (int i = 0; i < mItemCount; i++) {
                        for (int j = 0; j < mItem[i].types.length; j++) {
                            String[] atCommand = new String[2];
                            atCommand[0] = "AT+EINFO=" + mFlag + "," + mItem[i].types[j] + ",0";
                            atCommand[1] = "+EINFO";
                            sendATCommand(atCommand, MSG_NW_INFO_OPEN);
                        }
                    }
                } else {
                    showToast(getString(R.string.send_at_fail));
                }
                break;
            case MSG_NW_INFO_OPEN:
            case MSG_NW_INFO_CLOSE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    showToast(getString(R.string.send_at_fail));
                }
                break;
            default:
                break;
            }
        }
    };

    private final Handler mUrcHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_NW_INFO_URC) {
                AsyncResult ar = (AsyncResult) msg.obj;
                String[] data = (String[]) ar.result;
                if ((data.length <= 0) || (data[0] == null) || (data[1] == null)) {
                    Log.e("@M_" + TAG, "data is null");
                    return;
                }
                Log.v("@M_" + TAG, "Receive URC: " + data[0] + ", " + data[1]);

                int type = -1;
                try {
                    type = Integer.parseInt(data[0]);
                } catch (NumberFormatException e) {
                    showToast("Return type error");
                    return;
                }

                mNetworkInfo.put(type, data[1]);
                if (type == NWSEL_EM_TIMER_INFO) {
                    mTimerInfo = data[1];
                    mTimerInfoStart = System.currentTimeMillis();
                }
                int size = 2 * UrcParser.size(type);
                if (size != data[1].length()) {
                    Log.w("@M_" + TAG, "Wrong return length: " + data[1].length());
                }

                if (mCurrentItem == type) {
                    showNetworkInfo();
                }
            }
        }
    };

    private final Handler mUiHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_UI) {
                showNetworkInfo();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lte_networkinfo_detail);

        String[] pageLabels = getResources().getStringArray(R.array.lte_network_info_labels);

        mItem = new NetworkInfoPage[pageLabels.length];
        mSimType = getIntent().getIntExtra("mSimType", PhoneConstants.SIM_ID_1);
        int[] checked = getIntent().getIntArrayExtra("mChecked");
        if (null != checked) {
            for (int i = 0; i < checked.length; i++) {
                if (1 == checked[i]) {
                    mItem[mItemCount] =
                            new NetworkInfoPage(i, pageLabels[i], UrcParser.getTypes(i));
                    mItemCount++;
                }
            }
        } else { // Should not happen
            finish();
            return;
        }

        mInfo = (TextView) findViewById(R.id.NetworkInfo_Info);
        mPageUp = (Button) findViewById(R.id.NetworkInfo_PageUp);
        mPageDown = (Button) findViewById(R.id.NetworkInfo_PageDown);
        mPageUp.setOnClickListener(this);
        mPageDown.setOnClickListener(this);
        registerNetwork();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onStop() {
        mTimer.cancel();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        unregisterNetwork();
        super.onDestroy();
    }

    /*
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View arg0) {
        if (arg0.getId() == mPageUp.getId()) {
            mCurrentItem = (mCurrentItem - 1 + mItemCount) % mItemCount;
            updateUI();
        } else if (arg0.getId() == mPageDown.getId()) {
            mCurrentItem = (mCurrentItem + 1) % mItemCount;
            updateUI();
        }
    }

    public void updateUI() {
        showNetworkInfo();
        mTimer.cancel();
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mUiHandler.sendEmptyMessage(MSG_UPDATE_UI);
                // Special handling for NWSEL_EM_TIMER_INFO
                if (mTimerInfo != null) {
                    String info = mTimerInfo.substring(6, 8) + mTimerInfo.substring(4, 6)
                            + mTimerInfo.substring(2, 4) + mTimerInfo.substring(0, 2);
                    int timer = Integer.parseInt(info, 16);
                    timer -= (System.currentTimeMillis() - mTimerInfoStart) / 1000;
                    if (timer < 0) {
                        timer = 0;
                    }
                    info = Integer.toHexString(timer);
                    info = ("00000000" + info).substring(info.length());
                    info = info.substring(6, 8) + info.substring(4, 6)
                            + info.substring(2, 4) + info.substring(0, 2);
                    mNetworkInfo.put(NWSEL_EM_TIMER_INFO, info);
                }
            }
        }, TOTAL_TIMER, TOTAL_TIMER);
    }

    private void showNetworkInfo() {
        int page = mItem[mCurrentItem].page;
        String name = mItem[mCurrentItem].label;
        int[] types = mItem[mCurrentItem].types;

        String text = "<" + (mCurrentItem + 1) + "/" + mItemCount + "> " + "[" + name + "]\n";
        for (int i = 0; i < types.length; i++) {
            String raw = mNetworkInfo.get(types[i]);
            String info = new String(UrcParser.parse(page, types[i],
                    raw == null ? null : raw.toCharArray()));
            text += info;
        }
        mInfo.setText(text);
    }

    private void registerNetwork() {
        if (TelephonyManager.getDefault().getPhoneCount() <= 1) {
            mPhone = PhoneFactory.getDefaultPhone();
        } else {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        }

        if (FeatureSupport.isSupported(FeatureSupport.FK_MTK_C2K_SUPPORT)) {
            if (((FeatureSupport.isSupported(FeatureSupport.FK_MTK_SVLTE_SUPPORT))
                  || (FeatureSupport.isSupported(FeatureSupport.FK_SRLTE_SUPPORT)))
                    && mPhone instanceof LteDcPhoneProxy) {
                mPhone = ((LteDcPhoneProxy) mPhone).getLtePhone();
            }
            if (FeatureSupport.isSupported(FeatureSupport.FK_EVDO_DT_SUPPORT)
                    && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
            }
        }

        mPhone.registerForNetworkInfo(mUrcHandler, MSG_NW_INFO_URC, null);

        String[] atCommand = {"AT+EINFO?", "+EINFO"};
        sendATCommand(atCommand, MSG_NW_INFO);
    }

    private void unregisterNetwork() {
        mPhone.unregisterForNetworkInfo(mUrcHandler);

        mFlag = mFlag & FLAG_OR_DATA;
        Log.v("@M_" + TAG, "The close flag is :" + mFlag);
        String[] atCloseCmd = new String[2];
        atCloseCmd[0] = "AT+EINFO=" + mFlag;
        atCloseCmd[1] = "";
        sendATCommand(atCloseCmd, MSG_NW_INFO_CLOSE);
    }

    private void sendATCommand(String[] atCommand, int msg) {
        Log.v("@M_" + TAG, "sendATCommand :" + atCommand[0]);
        mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander.obtainMessage(msg));
    }

    private void showToast(String msg) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }
}
