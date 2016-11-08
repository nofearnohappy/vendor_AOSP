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
package com.mediatek.settings.plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardTile;
import com.android.settings.dashboard.DashboardTileView;

import com.mediatek.common.PluginImpl;
import com.mediatek.op09.plugin.R;
import com.mediatek.settings.ext.DefaultSettingsMiscExt;

import java.util.Map;


/**
 * For settings small feature misc.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.ISettingsMiscExt")
public class OP09SettingsMiscExtImp extends DefaultSettingsMiscExt {

    private static final String TAG = "SettingsMiscExt";
    private static final String TAB_SIM_1 = "sim1";
    private static final String TAB_SIM_2 = "sim2";
    private static final String TAB_SIM_1_INDEX = "1";
    private static final String TAB_SIM_2_INDEX = "2";
    private static final String TAB_MOBILE = "mobile";

    ///Dashboard three intent
    private static final String INTERNATIONAL_ROAMING = "com.mediatek.OP09.INTERNATIONAL_ROAMING";
    private static final String DATA_CONNECTION_ACTIVITY_INTENT =
        "com.mediatek.OP09.DATA_CONNECTION_SETTING";
    private static final String USE_LTE_DATA_INTENT = "com.mediatek.OP09.USE_LTE_DATA";

    ///Dashboard intent.extra.name Key and three value
    private static final String CUSTOMIZE_ITEM_NAME = "customize_item_name";
    private static final String INTERNATIONAL_ROAMING_ITEM_NAME = "international_roaming";
    private static final String MOBILE_DATA_ITEM_NAME = "mobile_data";
    private static final String UISE_LTE_ITEM_NAME = "lte";

  ///Dashboard intent.extra.index Key
    private static final String CUSTOMIZE_ITEM_INDEX = "customize_item_index";

    private static final String DATA_CONNECTION_ITEM_NAME = "data_connection";

    private static final int COLORNUM = 2;

    private Context mContext;
    private DataConnectionEnabler mDataConnecitonEnabler;
    boolean mCTtestcardInsert;

    /**
     * Constructor method.
     * @param context Settings's context
     */
    public OP09SettingsMiscExtImp(Context context) {
        super(context);
        mContext = context;
        Log.d(TAG, "SettingsMiscExt this=" + this);
    }

    /**
     * Customize strings which contains 'SIM', replace 'SIM' by
     * 'UIM/SIM','UIM','card' etc.
     * @param simString sim String
     * @param subId sub id
     * @return new String
     */
    @Override
    public String customizeSimDisplayString(String simString, int subId) {
        Log.i(TAG, " op09settingsMiscExt customizeSimDisplayString method start slotId  = "
             + subId);
        if (simString == null) {
            return null;
        } else if (simString != null) {
            if (SubscriptionManager.INVALID_SUBSCRIPTION_ID == subId) {
                return replaceSimToSimUim(simString.toString());
            }
            if (PhoneConstants.SIM_ID_1 == SubscriptionManager.getSlotId(subId)) {
                return replaceSimBySlotInner(simString.toString());
            }
        }
        return simString;
    }

      /**
       * replace Sim String by SlotInner.
       * @param simString which will be replaced
       * @return new String
      */
      public static String replaceSimBySlotInner(String simString) {
          if (simString.contains("SIM")) {
            simString = simString.replaceAll("SIM", "UIM");
          }
          if (simString.contains("sim")) {
              simString = simString.replaceAll("sim", "uim");
          }
          return simString;
      }

    private String replaceSimToSimUim(String simString) {
        if (simString.contains("SIM")) {
            simString = simString.replaceAll("SIM", "UIM/SIM");
        }
        if (simString.contains("Sim")) {
            simString = simString.replaceAll("Sim", "Uim/Sim");
        }
        Log.d(TAG, "op09 replace string: " + simString);
        return simString;
    }

    /**
     * Create MobiledataDashboardTile,Use4GDashboardTile,InternationalRoamingDashboardTile.
     * @param targetDashboardCategory which will be add this there DashboardTile.
     * @param add whether add dashboard title.
     */
    //TODO: Remove to fix build error, should add API in plugin interface @Override
    public void addCustomizedItem(Object targetDashboardCategory, Boolean add) {
      Log.d(TAG, "OP09 addCustomizedItem");
      if (!add) {
          return;
      }
      if (!(targetDashboardCategory instanceof DashboardCategory)) {
          Log.e(TAG, "addCustomizedItem targetDashboardCategory " +
              "is not instance of DashboardCategory return");
          return;
      }
      DashboardCategory target = (DashboardCategory) targetDashboardCategory;
        target.addTile(createIRDashboardTile(0));
        target.addTile(createMobiledataDashboardTile(1));
        target.addTile(createUse4GDashboardTile(2));
    }

    // Create MobiledataDashboard,Use4GDashboardTile,International Roaming header
    private DashboardTile createIRDashboardTile(int index) {
        Log.d(TAG, "OP09 createIRDashboardTile");
        DashboardTile dashboardTile = new DashboardTile();
        dashboardTile.title = mContext
                .getString(R.string.international_roaming_summary_title);
        dashboardTile.intent = new Intent(INTERNATIONAL_ROAMING);
        return dashboardTile;
    }

    ///create Mobile data DataDashboardTile
    private DashboardTile createMobiledataDashboardTile(int index) {
        Log.d(TAG, "OP09 createMobiledataDashboardTile");
        DashboardTile mobileDashboardTile = new DashboardTile();
        mobileDashboardTile.title = mContext.getString(R.string.data_connection_summary_title);
        mobileDashboardTile.intent = new Intent(DATA_CONNECTION_ACTIVITY_INTENT);
        return mobileDashboardTile;
    }

    ///create Use4G DataDashboardTile
    private DashboardTile createUse4GDashboardTile(int index) {
        Log.d(TAG, "OP09 createUse4GDashboardTile");
        DashboardTile use4GDashboardTile = new DashboardTile();
        use4GDashboardTile.title = mContext.getString(R.string.enable_lte_data);
        use4GDashboardTile.intent = new Intent(USE_LTE_DATA_INTENT);
        return use4GDashboardTile;
    }
    /**
    * Customize dashboard tile.
    * @param tile dashboard tile
    * @param tileIcon dashboard icon
    */
    //TODO: Remove to fix build error, should add API in plugin interface @Override
    public void customizeDashboardTile(Object tile, ImageView tileIcon) {
        Log.i(TAG, "tile = " + tile.toString());
        DashboardTile dashBoardTile = (DashboardTile) tile;
        String romingTitle = mContext
                .getString(R.string.international_roaming_summary_title);
        String mobileDataTitle = mContext.getString(R.string.data_connection_summary_title);
        String useLteTitle = mContext.getString(R.string.enable_lte_data);
        String customizeItemName = dashBoardTile.title.toString();
                Log.d(TAG, "getCustomizedItemIcon: customizeItemName = " + customizeItemName);

        if (romingTitle.equals(customizeItemName)) {
            // We need to use setImageDrawable instead of setImageResource to
            // use the right context.
            tileIcon.setImageDrawable(mContext.getResources().getDrawable(
                   R.drawable.ic_international_roaming_set));
        } else if (mobileDataTitle.equals(customizeItemName)) {
            tileIcon.setImageDrawable(mContext.getResources().getDrawable(
                   R.drawable.perm_group_turn_on_data_connection));
        } else if (useLteTitle.equals(customizeItemName)) {
            tileIcon.setImageDrawable(mContext.getResources().getDrawable(
                   R.drawable.ic_lte_data_set));
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
        }
    }

    //@Override
    public void updataDefaultDataConnection(Map<String, Boolean> dataEnableMap, Context context) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        TelephonyManager mTelephonyManager;
        int subId = 0;
        int subIdTmp;
        int simCount;
        Boolean result;
        Boolean dataEnable;

        mTelephonyManager = TelephonyManager.from(context);
        //subId = SubscriptionManager.getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1);
        subId = SubscriptionManager.getDefaultDataSubId();
        simCount = mTelephonyManager.getSimCount();
        Log.d(TAG, "updataDefaultDataConnection: subId = " + subId);
        Log.d(TAG, "updataDefaultDataConnection: simCount = " + simCount);

        dataEnable = mTelephonyManager.getDataEnabled();
        Log.d(TAG, "updataDefaultDataConnection: data_enable = " + dataEnable);
        ///test begin
        dataEnable = true;
        ///test end

        if (dataEnable == true) {
            for (int i = 0; i < simCount; i++) {
                final SubscriptionInfo sir = Utils.findRecordBySlotId(context, i);
                if (sir != null) {
                    subIdTmp = sir.getSubscriptionId();
                    Log.d(TAG, "updataDefaultDataConnection: sir subId_t = " + subIdTmp);
                    if (subIdTmp == subId) {
                         //mTelephonyManager.setDataEnabled(subId, true);
                         result = dataEnableMap.put(String.valueOf(subIdTmp), true);
                         Log.d(TAG, "updataDefaultDataConnection: subId_t = " + subIdTmp +
                             "> set true result=" + result);
                    } else {
                         //mTelephonyManager.setDataEnabled(subId, false);
                         result = dataEnableMap.put(String.valueOf(subIdTmp), false);
                         Log.d(TAG, "updataDefaultDataConnection: subId_t = " + subIdTmp +
                             "> set false result=" + result);
                    }
                }
            }
        } else {
            for (int i = 0; i < simCount; i++) {
                final SubscriptionInfo sir = Utils.findRecordBySlotId(context, i);
                if (sir != null) {
                    subIdTmp = sir.getSubscriptionId();
                    Log.d(TAG, "updataDefaultDataConnection: sir subId_t = " + subIdTmp);
                     result = dataEnableMap.put(String.valueOf(subIdTmp), false);
                     Log.d(TAG, "updataDefaultDataConnection: subId_t = " + subIdTmp +
                         "> set false result=" + result);
                }
            }
        }
    }
}
