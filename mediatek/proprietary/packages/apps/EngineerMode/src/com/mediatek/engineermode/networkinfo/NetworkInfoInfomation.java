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

package com.mediatek.engineermode.networkinfo;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.engineermode.FeatureSupport;
import com.mediatek.engineermode.ModemCategory;
import com.mediatek.engineermode.R;
import com.mediatek.internal.telephony.ltedc.LteDcPhoneProxy;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkInfoInfomation extends Activity implements OnClickListener {
    private static final String TAG = "NetworkInfo";
    private static final int MSG_NW_INFO = 1;
    private static final int MSG_NW_INFO_URC = 2;
    private static final int MSG_NW_INFO_OPEN = 3;
    private static final int MSG_NW_INFO_CLOSE = 4;
    private static final int MSG_UPDATE_UI = 5;
    private static final int MSG_NW_INFO_URC_CDMA = 6;
    private static final int TOTAL_TIMER = 1000;
    private static final int FLAG_OR_DATA = 0xFFFFFFF7;
    private static final int FLAG_OFFSET_BIT = 0x08;
    private static final int FLAG_DATA_BIT = 8;
    private static final String SERVICE_NO_CHANGE = "04";
    private static final String RRC_NO_CHANGE = "06";
    private static final String NO_CHANGE = "FEFF";
    private static final int CDMA_INTERVAL = 2;

    private Button mPageUp;
    private Button mPageDown;
    private TextView mInfo;
    private Toast mToast = null;

    private int mItemCount = 0;
    private int mCurrentItem = 0;
    private int[] mItem;
    private int mSimType;
    private NetworkInfoUrcParser mUrcParser;
    private Phone mPhone = null;
    private Timer mTimer = new Timer();
    private int mFlag = 0;
    private HashMap<Integer, NetworkInfoItem> mNetworkInfo =
            new HashMap<Integer, NetworkInfoItem>();
    private int mCdmaItems = 0;
    private CdmaInfoAdapter mAdapter;
    private TextView mHeader;
    private ListView mList;

    /**
     * List adapter for cdma info.
     */
    private class CdmaInfoAdapter extends ArrayAdapter<String[]> {
        /**
         * Constructor.
         */
        public CdmaInfoAdapter(Activity activity) {
            super(activity, 0);
        }

        /**
         * ViewHolder.
         */
        private class ViewHolder {
            public TextView[] texts;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater inflater = NetworkInfoInfomation.this.getLayoutInflater();
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.cdma_info_entry, null);
                holder = new ViewHolder();
                holder.texts = new TextView[4];
                holder.texts[0] = (TextView) convertView.findViewById(R.id.info1);
                holder.texts[1] = (TextView) convertView.findViewById(R.id.info2);
                holder.texts[2] = (TextView) convertView.findViewById(R.id.info3);
                holder.texts[3] = (TextView) convertView.findViewById(R.id.info4);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            String[] entry = getItem(position);
            for (int i = 0; i < holder.texts.length; i++) {
                if (entry.length > i) {
                    holder.texts[i].setVisibility(View.VISIBLE);
                    holder.texts[i].setText(entry[i]);
                } else {
                    holder.texts[i].setVisibility(View.GONE);
                }
            }
            return convertView;
        }
    }

    static class NetworkInfoItem {
        public String name;
        public String info;
        public int size;    // Only for error checking

        NetworkInfoItem(String name, int size) {
            this.name = name;
            this.info = new String();
            this.size = size;
        }
    }

    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case MSG_NW_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    if ((data.length <= 0) || (data[0] == null)) {
                        Log.v("@M_" + TAG, "data[0] is null");
                        return;
                    }
                    Log.v("@M_" + TAG, "data[0] is : " + data[0]);
                    Log.v("@M_" + TAG, "flag is : " + data[0].substring(FLAG_DATA_BIT));
                    mFlag = Integer.valueOf(data[0].substring(FLAG_DATA_BIT));
                    mFlag = mFlag | FLAG_OFFSET_BIT;
                    Log.v("@M_" + TAG, "flag change is : " + mFlag);
                    for (int j = 0; j < mItemCount; j++) {
                        if (mItem[j] < Content.CDMA_INDEX_BASE) {
                            String[] atCommand = new String[2];
                            atCommand[0] = "AT+EINFO=" + mFlag + "," + mItem[j] + ",0";
                            atCommand[1] = "+EINFO";
                            sendATCommand(atCommand, MSG_NW_INFO_OPEN);
                        }
                    }
                }
                // fall through
            case MSG_NW_INFO_OPEN:
            case MSG_NW_INFO_CLOSE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Toast.makeText(NetworkInfoInfomation.this, getString(R.string.send_at_fail),
                            Toast.LENGTH_SHORT);
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
                    Log.v("@M_" + TAG, "data[0] is null");
                    return;
                }

                Log.v("@M_" + TAG, "Receive URC: " + data[0] + ", " + data[1]);

                int type = -1;
                try {
                    type  = Integer.parseInt(data[0]);
                } catch (NumberFormatException e) {
                    Toast.makeText(NetworkInfoInfomation.this,
                            "Return type error", Toast.LENGTH_SHORT).show();
                    return;
                }

                NetworkInfoItem item = mNetworkInfo.get(type);
                if (item == null) {
                    Log.e("@M_" + TAG, "Invalid return type: " + type);
                    return;
                }

                if (type == Content.URR_3G_GENERAL_INDEX) {
                    // Special handling for URR 3G General Info
                    item.info = handle3gGeneralStatusInfo(item.info, data[1]);
                } else {
                    item.info = data[1];
                }

                if (item.size != data[1].length()) {
                    Log.w("@M_" + TAG, "Wrong return length: " + data[1].length());
                }

                if (mItem[mCurrentItem] == type) {
                    showNetworkInfo();
                }
            } else if (msg.what == MSG_NW_INFO_URC_CDMA) {
                AsyncResult ar = (AsyncResult) msg.obj;
                String[] data = (String[]) ar.result;
                if ((data.length <= 0) || (data[1] == null)) {
                    Log.v("@M_" + TAG, "data[0] is null");
                    return;
                }
                Log.v("@M_" + TAG, "Receive URC: " + data[1]);
                int type = getCdmaInfoTypeForUrc(data[1].trim());
                Log.v("@M_" + TAG, "type: " + type);
                NetworkInfoItem item = mNetworkInfo.get(type);
                if (item == null) {
                    Log.e("@M_" + TAG, "Invalid return type: " + type);
                    return;
                }
                item.info = data[1];
                if (mItem[mCurrentItem] == type) {
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
        setContentView(R.layout.networkinfo_info);

        // get the selected item and store its ID into the mItem array
        mItem = new int[NetworkInfo.TOTAL_ITEM_NUM];
        Intent intent = getIntent();
        mSimType = intent.getIntExtra("mSimType", PhoneConstants.SIM_ID_1);
        int[] checked = intent.getIntArrayExtra("mChecked");
        if (null != checked) {
            for (int i = 0; i < checked.length; i++) {
                if (1 == checked[i]) {
                    mItem[mItemCount] = i;
                    mItemCount++;
                }
            }
        }

        int modemType = NetworkInfo.getModemType(mSimType);

        mNetworkInfo.put(Content.CELL_INDEX,
                new NetworkInfoItem("RR Cell Sel", Content.CELL_SEL_SIZE));
        mNetworkInfo.put(Content.CHANNEL_INDEX,
                new NetworkInfoItem("RR Ch Dscr", Content.CH_DSCR_SIZE));
        mNetworkInfo.put(Content.CTRL_INDEX,
                new NetworkInfoItem("RR Ctrl chan", Content.CTRL_CHAN_SIZE));
        mNetworkInfo.put(Content.RACH_INDEX,
                new NetworkInfoItem("RR RACH Ctrl", Content.RACH_CTRL_SIZE));
        mNetworkInfo.put(Content.LAI_INDEX,
                new NetworkInfoItem("RR LAI Info", Content.LAI_INFO_SIZE));
        mNetworkInfo.put(Content.RADIO_INDEX,
                new NetworkInfoItem("RR Radio Link", Content.RADIO_LINK_SIZE));
        mNetworkInfo.put(Content.MEAS_INDEX,
                new NetworkInfoItem("RR Meas Rep", Content.MEAS_REP_SIZE));
        mNetworkInfo.put(Content.CA_INDEX,
                new NetworkInfoItem("RR Ca List", Content.CAL_LIST_SIZE));
        mNetworkInfo.put(Content.CONTROL_INDEX,
                new NetworkInfoItem("RR Control Msg", Content.CONTROL_MSG_SIZE));
        mNetworkInfo.put(Content.SI2Q_INDEX,
                new NetworkInfoItem("RR SI2Q Info", Content.SI2Q_INFO_SIZE));
        mNetworkInfo.put(Content.MI_INDEX,
                new NetworkInfoItem("RR MI Info", Content.MI_INFO_SIZE));
        mNetworkInfo.put(Content.BLK_INDEX,
                new NetworkInfoItem("RR BLK Info", Content.BLK_INFO_SIZE));
        mNetworkInfo.put(Content.TBF_INDEX,
                new NetworkInfoItem("RR TBF Info", Content.TBF_INFO_SIZE));
        mNetworkInfo.put(Content.GPRS_INDEX,
                new NetworkInfoItem("RR GPRS Gen", Content.GPRS_GEN_SIZE));
        mNetworkInfo.put(Content.SM_INFO_INDEX,
                new NetworkInfoItem("SM EM Info", Content.SM_EM_INFO_SIZE));
        mNetworkInfo.put(Content.GMM_INFO_INDEX,
                new NetworkInfoItem("GMM EM Info", Content.GMM_EM_INFO_SIZE));
        mNetworkInfo.put(Content.MM_INFO_INDEX,
                new NetworkInfoItem("RR 3G MM EM Info", Content.M3G_MM_EMINFO_SIZE));
        mNetworkInfo.put(Content.URR_3G_GENERAL_INDEX,
                new NetworkInfoItem("URR 3G General Status", Content.URR_3G_GENERAL_SIZE));
        mNetworkInfo.put(Content.TCM_MMI_INDEX,
                new NetworkInfoItem("RR 3G TCM MMI EM Info", Content.M_3G_TCMMMI_INFO_SIZE));
        mNetworkInfo.put(Content.CSCE_SERV_CELL_STATUS_INDEX,
                new NetworkInfoItem("RR 3G CsceEMServCellSStatusInd",
                        Content.CSCE_SERV_CELL_STATUS_SIZE));
        mNetworkInfo.put(Content.CSCE_NEIGH_CELL_STATUS_INDEX,
                new NetworkInfoItem("RR xG CsceEMNeighCellSStatusIndStructSize",
                        Content.XGCSCE_NEIGH_CELL_STATUS_SIZE));
        mNetworkInfo.put(Content.CSCE_MULTIPLMN_INDEX,
                new NetworkInfoItem("RR 3G CsceEmInfoMultiPlmn", Content.CSCE_MULTI_PLMN_SIZE));
        mNetworkInfo.put(Content.PERIOD_IC_BLER_REPORT_INDEX,
                new NetworkInfoItem("RR 3G MemeEmPeriodicBlerReportInd",
                        Content.PERIOD_IC_BLER_REPORT_SIZE));
        mNetworkInfo.put(Content.URR_UMTS_SRNC_INDEX,
                new NetworkInfoItem("RR 3G UrrUmtsSrncId", Content.URR_UMTS_SRNC_SIZE));
        mNetworkInfo.put(Content.HSERV_CELL_INDEX,
                new NetworkInfoItem("RR 3G MemeEmInfoHServCellInd", Content.MEME_HSERV_CELL_SIZE));
        // add for LGE
        mNetworkInfo.put(Content.SLCE_VOICE_INDEX,
                new NetworkInfoItem("3G speech codec", Content.SLCE_VOICE_SIZE));
        mNetworkInfo.put(Content.SECURITY_CONFIGURATION_INDEX,
                new NetworkInfoItem("Security Configuration",
                        Content.SECURITY_CONFIGURATION_SIZE));

        if (modemType == NetworkInfo.MODEM_FDD) {
            mNetworkInfo.put(Content.UMTS_CELL_STATUS_INDEX,
                    new NetworkInfoItem("RR 3G MemeEmInfoUmtsCellStatus",
                            Content.UMTS_CELL_STATUS_SIZE));
            mNetworkInfo.put(Content.PSDATA_RATE_STATUS_INDEX,
                    new NetworkInfoItem("RR 3G SlceEmPsDataRateStatusInd",
                            Content.SLCE_PS_DATA_RATE_STATUS_SIZE));
        } else if (modemType == NetworkInfo.MODEM_TD) {
            mNetworkInfo.put(Content.HANDOVER_SEQUENCE_INDEX,
                    new NetworkInfoItem("RR 3G HandoverSequenceIndStuct",
                            Content.HANDOVER_SEQUENCE_SIZE));
            mNetworkInfo.put(Content.UL_ADM_POOL_STATUS_INDEX,
                    new NetworkInfoItem("RR 3G Ul2EmAdmPoolStatusIndStruct",
                            Content.ADM_POOL_STATUS_SIZE));
            mNetworkInfo.put(Content.UL_PSDATA_RATE_STATUS_INDEX,
                    new NetworkInfoItem("RR 3G Ul2EmPsDataRateStatusIndStruct",
                            Content.UL2_PSDATA_RATE_STATUS_SIZE));
            mNetworkInfo.put(Content.UL_HSDSCH_RECONFIG_STATUS_INDEX,
                    new NetworkInfoItem("RR 3G Ul2EmHsdschReconfigStatusIndStruct",
                        Content.UL_HSDSCH_RECONFIG_STATUS_SIZE));
            mNetworkInfo.put(Content.UL_URLC_EVENT_STATUS_INDEX,
                    new NetworkInfoItem("RR 3G Ul2EmUrlcEventStatusIndStruct",
                            Content.URLC_EVENT_STATUS_SIZE));
            mNetworkInfo.put(Content.UL_PERIOD_IC_BLER_REPORT_INDEX,
                    new NetworkInfoItem("RR 3G Ul2EmPeriodicBlerReportInd",
                            Content.UL_PERIOD_IC_BLER_REPORT_SIZE));
        }

        // add for CDMA
        if (ModemCategory.isCdma()) {
            mNetworkInfo.put(Content.CDMA_1XRTT_RADIO_INDEX, new NetworkInfoItem("1xRTT Radio", 0));
            mNetworkInfo.put(Content.CDMA_1XRTT_INFO_INDEX, new NetworkInfoItem("1xRTT Info", 0));
            mNetworkInfo.put(Content.CDMA_1XRTT_SCH_INFO_INDEX,
                    new NetworkInfoItem("1xRTT SCH Info", 0));
            mNetworkInfo.put(Content.CDMA_1XRTT_STATISTICS_INDEX,
                    new NetworkInfoItem("1xRTT Statistics", 0));
            mNetworkInfo.put(Content.CDMA_1XRTT_SERVING_INDEX,
                    new NetworkInfoItem("1xRTT Serving/Neighbor", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_SERVING_INFO_INDEX,
                    new NetworkInfoItem("EVDO Serving Info", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_ACTIVE_SET_INDEX,
                    new NetworkInfoItem("EVDO Active Set", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_CANDICATE_SET_INDEX,
                    new NetworkInfoItem("EVDO Candidate Set", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_NEIGHBOR_SET_INDEX,
                    new NetworkInfoItem("EVDO Neighbour Set", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_FL_INDEX, new NetworkInfoItem("EVDO FL", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_RL_INDEX, new NetworkInfoItem("EVDO RL", 0));
            mNetworkInfo.put(Content.CDMA_EVDO_STATE_INDEX, new NetworkInfoItem("EVDO State", 0));
        }

        mNetworkInfo.get(Content.URR_3G_GENERAL_INDEX).info = "FFFFFFFFFFFF";

        mInfo = (TextView) findViewById(R.id.NetworkInfo_Info);
        mList = (ListView) findViewById(R.id.NetworkInfo_List);
        mHeader = (TextView) findViewById(R.id.NetworkInfo_List_Header);
        mAdapter = new CdmaInfoAdapter(this);
        mList.setAdapter(mAdapter);
        mPageUp = (Button) findViewById(R.id.NetworkInfo_PageUp);
        mPageDown = (Button) findViewById(R.id.NetworkInfo_PageDown);
        mPageUp.setOnClickListener(this);
        mPageDown.setOnClickListener(this);
        mUrcParser = new NetworkInfoUrcParser(this);
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
            }
        }, TOTAL_TIMER, TOTAL_TIMER);
    }

    private void showNetworkInfo() {
        int type = mItem[mCurrentItem];
        String name = mNetworkInfo.get(type).name;
        String info = mNetworkInfo.get(type).info;
        if (type >= Content.CDMA_INDEX_BASE) {
            mInfo.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
            mHeader.setVisibility(View.VISIBLE);
            mHeader.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n" + "[" + name + "]");
            mAdapter.clear();
            mAdapter.addAll(mUrcParser.parseCdmaInfo(type, info));
            mAdapter.notifyDataSetChanged();
        } else {
            mInfo.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
            mHeader.setVisibility(View.GONE);
            mInfo.setText("<" + (mCurrentItem + 1) + "/" + mItemCount + ">\n"
                    + "[" + name + "]\n" + mUrcParser.parseInfo(type, info, mSimType));
        }
    }

    private void registerNetwork() {
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
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

        if (ModemCategory.isCdma()) {
            registerNetworkCdma();
        }
    }

    private void unregisterNetwork() {
        mPhone.unregisterForNetworkInfo(mUrcHandler);

        mFlag = mFlag & FLAG_OR_DATA;
        Log.v("@M_" + TAG, "The close flag is :" + mFlag);
        String[] atCloseCmd = new String[2];
        atCloseCmd[0] = "AT+EINFO=" + mFlag;
        atCloseCmd[1] = "";
        sendATCommand(atCloseCmd, MSG_NW_INFO_CLOSE);

        if (ModemCategory.isCdma()) {
            unregisterNetworkCdma();
        }
    }

    private void sendATCommand(String[] atCommand, int msg) {
        mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander.obtainMessage(msg));
    }

    private String handle3gGeneralStatusInfo(String oldValue, String newValue) {
        if (!Content.IS_MOLY) {
            if (newValue != null && newValue.length() > 8) {
                newValue = newValue.substring(8); // skip the 4-byte header
            } else {
                newValue = ""; // Should not happen, just for error handling
            }
        }
        if (oldValue.length() < 12 || newValue.length() < 12) {
            return newValue; // Should not happen
        }
        String oldServiceStatus = oldValue.substring(0, 2);
        String oldUmtsRrcState = oldValue.substring(2, 4);
        String oldUarfcn = oldValue.substring(4, 8);
        String oldPsc = oldValue.substring(8, 12);
        String serviceStatus = newValue.substring(0, 2);
        String umtsRrcState = newValue.substring(2, 4);
        String uarfcn = newValue.substring(4, 8);
        String psc = newValue.substring(8, 12);
        String info = SERVICE_NO_CHANGE.equals(serviceStatus) ? oldServiceStatus : serviceStatus;
        info += RRC_NO_CHANGE.equals(umtsRrcState) ? oldUmtsRrcState : umtsRrcState;
        info += NO_CHANGE.equals(uarfcn) ? oldUarfcn : uarfcn;
        info += NO_CHANGE.equals(psc) ? oldPsc : psc;
        return info;
    }

    private void registerNetworkCdma() {
        mCdmaItems = 0;
        for (int i = 0; i < mItemCount; i++) {
            if (mItem[i] >= Content.CDMA_INDEX_BASE) {
                mCdmaItems |= (1 << (mItem[i] - Content.CDMA_INDEX_BASE));
            }
        }

        String[] atCommand = {"AT+ECENGINFO=2," + mCdmaItems + "," + CDMA_INTERVAL, ""};
        sendATCommandCdma(atCommand, MSG_NW_INFO_OPEN);
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            PhoneFactory.getPhone(PhoneConstants.SIM_ID_1).registerForEngModeNetworkInfo(
                    mUrcHandler, MSG_NW_INFO_URC_CDMA, null);
        } else {
            PhoneFactory.getDefaultPhone().registerForEngModeNetworkInfo(
                    mUrcHandler, MSG_NW_INFO_URC_CDMA, null);
        }
    }

    private void unregisterNetworkCdma() {
        String[] atCommand = {"AT+ECENGINFO=0," + mCdmaItems, ""};
        sendATCommandCdma(atCommand, MSG_NW_INFO_CLOSE);
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            PhoneFactory.getPhone(PhoneConstants.SIM_ID_1)
                    .unregisterForEngModeNetworkInfo(mUrcHandler);
        } else {
            PhoneFactory.getDefaultPhone().unregisterForEngModeNetworkInfo(mUrcHandler);
        }
    }

    private void sendATCommandCdma(String[] atCommand, int msg) {
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            PhoneFactory.getPhone(PhoneConstants.SIM_ID_1).invokeOemRilRequestStrings(
                    atCommand, mATCmdHander.obtainMessage(msg));
        } else {
            PhoneFactory.getDefaultPhone().invokeOemRilRequestStrings(atCommand,
                    mATCmdHander.obtainMessage(msg));
        }
    }

    private int getCdmaInfoTypeForUrc(String urc) {
        if (urc.startsWith("\"1xRTT_Radio_Info")) {
            return Content.CDMA_1XRTT_RADIO_INDEX;
        }
        if (urc.startsWith("\"1xRTT_Info")) {
            return Content.CDMA_1XRTT_INFO_INDEX;
        }
        if (urc.startsWith("\"1xRTT_SCH_Info")) {
            return Content.CDMA_1XRTT_SCH_INFO_INDEX;
        }
        if (urc.startsWith("\"1xRTT_Stat_Info")) {
            return Content.CDMA_1XRTT_STATISTICS_INDEX;
        }
        if (urc.startsWith("\"1xRTT_Serving_Neighbr_Set_Info")) {
            return Content.CDMA_1XRTT_SERVING_INDEX;
        }
        if (urc.startsWith("\"EVDO_Serving_Info")) {
            return Content.CDMA_EVDO_SERVING_INFO_INDEX;
        }
        if (urc.startsWith("\"EVDO_Active_Set_Info")) {
            return Content.CDMA_EVDO_ACTIVE_SET_INDEX;
        }
        if (urc.startsWith("\"EVDO_Cand_Set_Info")) {
            return Content.CDMA_EVDO_CANDICATE_SET_INDEX;
        }
        if (urc.startsWith("\"EVDO_Nghbr_Set_Info")) {
            return Content.CDMA_EVDO_NEIGHBOR_SET_INDEX;
        }
        if (urc.startsWith("\"EVDO_FL_Info")) {
            return Content.CDMA_EVDO_FL_INDEX;
        }
        if (urc.startsWith("\"EVDO_RL_Info")) {
            return Content.CDMA_EVDO_RL_INDEX;
        }
        if (urc.startsWith("\"EVDO_State_Info")) {
            return Content.CDMA_EVDO_STATE_INDEX;
        }
        return -1;
    }
}

