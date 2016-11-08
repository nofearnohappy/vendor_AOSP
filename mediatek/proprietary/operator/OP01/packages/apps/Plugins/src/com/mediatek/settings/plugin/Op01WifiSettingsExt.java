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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.System;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.settings.ext.DefaultWifiSettingsExt;

import java.util.List;

/**
 * OP01 plugin implementation of WifiSettings feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IWifiSettingsExt")
public class Op01WifiSettingsExt extends DefaultWifiSettingsExt {

    private static final String TAG = "Op01WifiSettingsExt";
    private static final int WIFI_AP_MAX_ALLOWED_PRIORITY = 100000;
    private static final String KEY_PROP_WFA_TEST_SUPPORT = "persist.radio.wifi.wpa2wpaalone";

    private ContentResolver mContentResolver;
    private Context mContext;
    private WifiManager mWifiManager;
    private List<WifiConfiguration> mConfigs;
    private boolean mIsAutoPriority;
    private int mOldPriorityOrder;
    private int mNewPriorityOrder;
    private WifiConfiguration mLastConnectedConfig;
    private int mConfiguredApCount;
    // Array to store the right order of each AP
    private int[] mPriorityOrder;

    private PreferenceCategory mCmccConfigedAP = null;
    private PreferenceCategory mCmccNewAP = null;

    /**
     * Op01WifiSettingsExt.
     * @param context Context
     */
    public Op01WifiSettingsExt(Context context) {
        super();
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        Log.d("@M_" + TAG, "WifiSettingsExt mContext = " + mContext);
    }

    @Override
    public void registerPriorityObserver(ContentResolver contentResolver) {
        Log.d("@M_" + TAG, "unregisterPriorityObserver()");
        mContentResolver = contentResolver;
        contentResolver.registerContentObserver(System
                .getUriFor(System.WIFI_PRIORITY_TYPE), false, mPriorityObserver);
        mIsAutoPriority = System.getInt(mContentResolver,
                System.WIFI_PRIORITY_TYPE, System.WIFI_PRIORITY_TYPE_DEFAULT)
                    == System.WIFI_PRIORITY_TYPE_DEFAULT;
    }

    @Override
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
        Log.d("@M_" + TAG, "unregisterPriorityObserver()");
        if (mPriorityObserver != null) {
            contentResolver.unregisterContentObserver(mPriorityObserver);
        }
    }

    ContentObserver mPriorityObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean newValue) {
            Log.d("@M_" + TAG, "mPriorityObserver onChange()");
            mLastConnectedConfig = null;
            mIsAutoPriority = System.getInt(mContentResolver,
                    System.WIFI_PRIORITY_TYPE,
                    System.WIFI_PRIORITY_TYPE_DEFAULT) == System.WIFI_PRIORITY_TYPE_DEFAULT;
            // if change from manually priority to auto priority, current
            // connected AP will have highest priority(100010)
            if (mIsAutoPriority) {
                WifiInfo currentConnInfo = mWifiManager.getConnectionInfo();
                if (currentConnInfo != null) {
                    int curNetworkId = currentConnInfo.getNetworkId();
                    mConfigs = mWifiManager.getConfiguredNetworks();
                    mConfiguredApCount = mConfigs == null ? 0 : mConfigs.size();
                    for (int i = 0; i < mConfiguredApCount; i++) {
                        WifiConfiguration config = mConfigs.get(i);
                        if (config != null && config.networkId == curNetworkId) {
                            config.priority = WIFI_AP_MAX_ALLOWED_PRIORITY + 10;
                            Log.d("@M_" + TAG, "config.SSID=" + config.SSID
                                    + "config.priority=" + config.priority);
                            updateConfig(config);
                        }
                    }
                }
            }
            Log.d("@M_" + TAG, "mPriorityObserver onChange() end");
            updatePriority();
        }
    };

    @Override
    public void setLastConnectedConfig(WifiConfiguration config) {
        mLastConnectedConfig = config;
    }

    @Override
    public void updatePriority() {
        Log.d("@M_" + TAG, "updatePriority()");
        mConfigs = mWifiManager.getConfiguredNetworks();
        if (mConfigs != null) {
            mPriorityOrder = calculateInitPriority(mConfigs);
            // adjust priority order of each AP
            mConfigs = mWifiManager.getConfiguredNetworks();
            if (mConfigs != null) {
                mConfiguredApCount = mConfigs.size();
                for (int i = 0; i < mConfiguredApCount; i++) {
                    WifiConfiguration config = mConfigs.get(i);
                    Log.d("@M_" + TAG, "before ssid=" + config.SSID
                        + ",priority=" + config.priority + ",order=" + mPriorityOrder[i]);
                    if (config.priority != mConfiguredApCount
                            - mPriorityOrder[i] + 1) {
                        config.priority = mConfiguredApCount
                                - mPriorityOrder[i] + 1;
                        updateConfig(config);
                        Log.d("@M_" + TAG, "after ssid=" + config.SSID + ",priority="
                                + config.priority);
                    }
                }
                mWifiManager.saveConfiguration();
            }
        }
    }

    @Override
    public  void updateContextMenu(ContextMenu menu, int menuId, DetailedState state) {
        Log.d("@M_" + TAG, "updateContextMenu state = " + state);

        if (state != null
            && mWifiManager.getConnectionInfo().getSupplicantState()
                    == SupplicantState.COMPLETED) {
            Log.d("@M_" + TAG, "updateContextMenu string = "
                + mContext.getString(R.string.wifi_menu_disconnect));
            menu.add(Menu.NONE, menuId, 0, mContext.getString(R.string.wifi_menu_disconnect));
        }
    }

    @Override
    public void emptyCategory(PreferenceScreen screen) {
        Log.d("@M_" + TAG, "emptyCategory()");
        screen.addPreference(mCmccConfigedAP);
        screen.addPreference(mCmccNewAP);
        mCmccConfigedAP.removeAll();
        mCmccNewAP.removeAll();
    }

    @Override
    public void emptyScreen(PreferenceScreen screen) {
        screen.removePreference(mCmccConfigedAP);
        screen.removePreference(mCmccNewAP);
    }

    @Override
    public void refreshCategory(PreferenceScreen screen) {

        if (mCmccConfigedAP != null
                && mCmccConfigedAP.getPreferenceCount() == 0) {
            screen.removePreference(mCmccConfigedAP);
        }
        if (mCmccNewAP != null && mCmccNewAP.getPreferenceCount() == 0) {
            screen.removePreference(mCmccNewAP);
        }
    }

    /**
     * Adjust other access point's priority if one of them changed.
     */
    private void adjustPriority() {
        if (mIsAutoPriority) {
            Log.d("@M_" + TAG, "adjustPriority(), autoPriority");
            return;
        }
        if (mOldPriorityOrder == mNewPriorityOrder) {
            Log.d("@M_" + TAG, "adjustPriority(), AP priority does not change, keep ["
                    + mOldPriorityOrder + "]");
            return;
        }

        mConfigs = mWifiManager.getConfiguredNetworks();
        mConfiguredApCount = mConfigs == null ? 0 : mConfigs.size();
        mPriorityOrder = calculateInitPriority(mConfigs);
        if (mConfigs != null && mPriorityOrder != null) {
            if (mOldPriorityOrder > mNewPriorityOrder) {
                // selected AP will have a higher priority, but smaller order
                for (int i = 0; i < mConfiguredApCount; i++) {
                    WifiConfiguration config = mConfigs.get(i);
                    if (mPriorityOrder[i] >= mNewPriorityOrder
                            && mPriorityOrder[i] < mOldPriorityOrder) {
                        mPriorityOrder[i]++;
                        config.priority = mConfiguredApCount
                                - mPriorityOrder[i] + 1;
                        updateConfig(config);
                    } else if (mPriorityOrder[i] == mOldPriorityOrder) {
                        mPriorityOrder[i] = mNewPriorityOrder;
                        config.priority = mConfiguredApCount
                                - mNewPriorityOrder + 1;
                        updateConfig(config);
                    }
                }
            } else {
                // selected AP will have a lower priority, but bigger order
                for (int i = 0; i < mConfiguredApCount; i++) {
                    WifiConfiguration config = mConfigs.get(i);
                    if (mPriorityOrder[i] <= mNewPriorityOrder
                            && mPriorityOrder[i] > mOldPriorityOrder) {
                        mPriorityOrder[i]--;
                        config.priority = mConfiguredApCount
                                - mPriorityOrder[i] + 1;
                        updateConfig(config);
                    } else if (mPriorityOrder[i] == mOldPriorityOrder) {
                        mPriorityOrder[i] = mNewPriorityOrder;
                        config.priority = mConfiguredApCount
                                - mNewPriorityOrder + 1;
                        updateConfig(config);
                    }
                }
            }
        }
    }

    /**
     * Adjust other access point's priority before add new AP.
     */
    private void adjustPriorityBeforeAdd() {
        Log.d("@M_" + TAG, "adjustPriorityBeforeAdd(),"
                   + mOldPriorityOrder + " to " + mNewPriorityOrder);
        mConfigs = mWifiManager.getConfiguredNetworks();
        Log.d("@M_" + TAG, "adjustPriorityBeforeAdd() " + mConfigs);
        mConfiguredApCount = mConfigs == null ? 0 : mConfigs.size();
        mPriorityOrder = calculateInitPriority(mConfigs);
        if (mConfigs != null && mPriorityOrder != null) {
            // selected AP will have a lower priority, but bigger order
            for (int i = 0; i < mConfiguredApCount; i++) {
                WifiConfiguration config = mConfigs.get(i);
                Log.d("@M_" + TAG, "adjustPriority() before ssid=" + config.SSID
                    + ",order=" + mPriorityOrder[i]);
                if (mPriorityOrder[i] < mNewPriorityOrder) {
                    mPriorityOrder[i]--;
                    config.priority = mConfiguredApCount
                            - mPriorityOrder[i] + 1;
                    updateConfig(config);
                }
                Log.d("@M_" + TAG, "adjustPriority() after order ="
                    + mPriorityOrder[i] + ",priority =" + config.priority);
            }
        }
    }

    @Override
    public void recordPriority(int selectPriority) {
        // store the former priority value before user modification
        if (selectPriority != -1) {
            mOldPriorityOrder = mConfiguredApCount - selectPriority + 1;
        } else {
            // the last added AP will have highest priority, mean all other AP's
            // priority will be adjusted,
            // the same as adjust this new added one's priority from lowest to
            // highest
            mOldPriorityOrder = mConfiguredApCount + 1;
        }
    }

    @Override
    public void setNewPriority(WifiConfiguration config) {
        if (!mIsAutoPriority) {
            mNewPriorityOrder = config.priority;
            adjustPriority();
        }
        Log.d("@M_" + TAG, "setNewPriority, mConfiguredApCount="
            + mConfiguredApCount + ", priority=" + config.priority);
        // this change is to modify host app config priority
        config.priority = mConfiguredApCount - config.priority + 1;
        Log.d("@M_" + TAG, "setNewPriority, priority=" + config.priority);
    }

    @Override
    public void updatePriorityAfterSubmit(WifiConfiguration config) {
        Log.d("@M_" + TAG, "updatePriorityAfterSubmit() " + config.SSID
                + ",priority=" + config.priority);
        mNewPriorityOrder = config.priority;
        int networkId = lookupConfiguredNetwork(config);
        if (networkId != -1) {
            config.networkId = networkId;
            config.priority = mConfiguredApCount - config.priority + 1;
            Log.d("@M_" + TAG, "exist network: " + config.networkId);
            if (!mIsAutoPriority) {
                adjustPriority();
            }
        } else {
            // add a configured AP, need to adjust already-exist AP's priority
            config.priority = mConfiguredApCount + 1 - mNewPriorityOrder + 1;
            Log.d("@M_" + TAG, "new config");
            if (!mIsAutoPriority) {
                adjustPriorityBeforeAdd();
            }
        }
        Log.d("@M_" + TAG, "updated priority = " + config.priority);
    }

    @Override
    public void disconnect(WifiConfiguration wifiConfig) {
        Log.d("@M_" + TAG, "disconnect() from current active AP");
        if (wifiConfig != null) {
            int networkId = wifiConfig.networkId;
            if (networkId != WifiConfiguration.INVALID_NETWORK_ID) {
                mWifiManager.disable(networkId, null);
            }
        }
    }

    private String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    /**
     * calculate priority order of input ap list, each ap's right order is
     * stored in a int array.
     * @param configs
     * @return
     */
    private int[] calculateInitPriority(List<WifiConfiguration> configs) {
        if (configs == null) {
            return null;
        }
        for (WifiConfiguration config : configs) {
            if (config == null) {
                config = new WifiConfiguration();
                config.SSID = "ERROR";
                config.priority = 0;
            } else if (config.priority < 0) {
                config.priority = 0;
            }
        }

        int totalSize = configs.size();
        int[] result = new int[totalSize];
        for (int i = 0; i < totalSize; i++) {
            int biggestPoz = 0;
            for (int j = 1; j < totalSize; j++) {
                if (!formerHasHigherPriority(configs.get(biggestPoz), configs
                        .get(j))) {
                    biggestPoz = j;
                }
            }
            // this is the [i+1] biggest one, so give such order to it
            result[biggestPoz] = i + 1;
            configs.get(biggestPoz).priority = -1; // don't count this one in any
            // more
        }
        return result;
    }

    /**
     * compare priority of two AP.
     * @param former
     * @param backer
     * @return true if former one has higher priority, otherwise return false
     */
    private boolean formerHasHigherPriority(WifiConfiguration former,
            WifiConfiguration backer) {
        if (former == null) {
            return false;
        } else if (backer == null) {
            return true;
        } else {
            if (former.priority > backer.priority) {
                return true;
            } else if (former.priority < backer.priority) {
                return false;
            } else { // have the same priority, so default trusted AP go first
                String formerSSID = (former.SSID == null ? ""
                        : removeDoubleQuotes(former.SSID));
                String backerSSID = (backer.SSID == null ? ""
                        : removeDoubleQuotes(backer.SSID));

                Log.d("@M_" + TAG, "formerHasHigherPriority(), same case");
                return formerSSID.compareTo(backerSSID) <= 0;
            }
        }
    }

    /**
     * update configured networks.
     * @param config WifiConfiguration
     * */
    private void updateConfig(WifiConfiguration config) {
        if (config == null) {
            return;
        }
        WifiConfiguration newConfig = new WifiConfiguration();
        newConfig.networkId = config.networkId;
        newConfig.priority = config.priority;
        mWifiManager.updateNetwork(newConfig);
    }

    /**
     * Check the new profile against the configured networks.
     * If none existing is matched, return -1.
     * */
    private int lookupConfiguredNetwork(WifiConfiguration newProfile) {
        mConfigs = mWifiManager.getConfiguredNetworks();
        mConfiguredApCount = mConfigs == null ? 0 : mConfigs.size();
        if (mConfigs != null) {
            // add null judgement to avoid NullPointerException
            for (WifiConfiguration config : mConfigs) {
                if (config != null
                        && config.SSID != null
                        && config.SSID.equals(newProfile.SSID)
                        && config.allowedAuthAlgorithms != null
                        && config.allowedAuthAlgorithms
                                .equals(newProfile.allowedAuthAlgorithms)
                        && config.allowedKeyManagement != null
                        && config.allowedKeyManagement
                                .equals(newProfile.allowedKeyManagement)) {
                    String isWFATest = SystemProperties.get(
                            KEY_PROP_WFA_TEST_SUPPORT, "");
                    if ("true".equals(isWFATest)) {
                        if (config.allowedPairwiseCiphers != null
                                && !config.allowedPairwiseCiphers
                                        .equals(newProfile.allowedPairwiseCiphers)) {
                            return -1;
                        }
                    }
                    return config.networkId;
                }
            }
        }
        return -1;
    }

    @Override
    public void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged) {
        if (isConfiged) {
            mCmccConfigedAP.addPreference(preference);
        } else {
            mCmccNewAP.addPreference(preference);
        }
    }

    @Override
    public void addCategories(PreferenceScreen screen) {
        if (mCmccConfigedAP == null) {
            mCmccConfigedAP = new PreferenceCategory(mContext);
            mCmccConfigedAP.setTitle(R.string.configed_access_points);
            screen.addPreference(mCmccConfigedAP);
        }
        if (mCmccNewAP == null) {
            mCmccNewAP = new PreferenceCategory(mContext);
            mCmccNewAP.setTitle(R.string.new_access_points);
            screen.addPreference(mCmccNewAP);
        }
        emptyScreen(screen);
    }

}