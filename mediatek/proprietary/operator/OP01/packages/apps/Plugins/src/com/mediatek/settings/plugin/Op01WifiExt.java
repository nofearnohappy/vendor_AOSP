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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;

import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settingslib.wifi.AccessPoint;
import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;
import com.mediatek.settings.ext.DefaultWifiExt;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * OP01 plugin implementation of WifiExt feature.
 */
@PluginImpl(interfaceName = "com.mediatek.settings.ext.IWifiExt")
public class Op01WifiExt extends DefaultWifiExt
        implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "Op01WifiExt";
    static final String CMCC_SSID = "CMCC";
    static final String CMCC_AUTO_SSID = "CMCC-AUTO";
    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;

    /* These values from from "wifi_eap_method" resource array */
    static final int WIFI_EAP_METHOD_PEAP = 0;
    static final int WIFI_EAP_METHOD_SIM = 4;

    static final int WIFI_EAP_METHOD_NUM = 2;
    static final int WIFI_EAP_LAYOUT_OFF = 4;
    static final int WIFI_EAP_SKIP_NUM = 2;

    //remind user if connect to access point
    private static final int WIFI_CONNECT_REMINDER_ALWAYS = 0;
    private static final String KEY_CONNECT_TYPE = "connect_type";
    private static final String KEY_CONNECT_REMINDER = "connect_reminder";
    private static final String KEY_PRIORITY_TYPE = "priority_type";
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";
    private static final String KEY_SELECT_SSID_TYPE = "select_ssid_type";

    private Context mContext;

    //here priority means order of its priority, the smaller value, the higher priority
    private int mPriority = -1;
    private Spinner mPrioritySpinner;
    private TextView mNetworkNetmaskView;
    private String[] mPriorityArray;

    private int mNetworkId = WifiConfiguration.INVALID_NETWORK_ID;
    private int mCurrentPriority;

    private ListPreference mConnectTypePref;
    private ListPreference mConnectReminderPref;
    private SwitchPreference mPriorityTypePref;
    private Preference mPrioritySettingPref;

    private Preference mGatewayPref;
    private Preference mNetmaskPref;
    private WifiManager mWifiManager;

    /**
     * Op01WifiExt.
     * @param context Context
     */
    public Op01WifiExt(Context context) {
        super(context);
        mContext = context;
        mContext.setTheme(R.style.SettingsPluginBase);
        mWifiManager = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE));
        Log.d("@M_" + TAG, "Op01WifiExt");
    }

    @Override
    public void setAPNetworkId(WifiConfiguration wifiConfig) {
        if (wifiConfig != null) {
            mNetworkId = wifiConfig.networkId;
        } else {
            mNetworkId = WifiConfiguration.INVALID_NETWORK_ID;
        }
    }

    @Override
    public void setAPPriority(int apPriority) {
        mPriority = apPriority;
    }

    @Override
    public void setPriorityView(LinearLayout priorityLayout,
                    WifiConfiguration wifiConfig, boolean isEdit) {
        int networkId = WifiConfiguration.INVALID_NETWORK_ID;
        Log.d("@M_" + TAG, "setPriorityView(),isEdit:" + isEdit);
        Log.d("@M_" + TAG, "setPriorityView(),wifiConfig:" + wifiConfig);
        if (wifiConfig != null) {
            networkId = wifiConfig.networkId;
            Log.d("@M_" + TAG, "setPriorityView(),networkId" + networkId);
        }
        if (networkId != WifiConfiguration.INVALID_NETWORK_ID && !isEdit) {
            priorityLayout.setVisibility(View.GONE);
            return;
        }
        LayoutInflater inflater =
            (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.wifi_priority_cmcc, null);
        int priorityType = System.getInt(mContext.getContentResolver(),
                System.WIFI_PRIORITY_TYPE, System.WIFI_PRIORITY_TYPE_DEFAULT);

        List<WifiConfiguration> mConfigs = mWifiManager.getConfiguredNetworks();
        int configuredApCount = mConfigs == null ? 0 : mConfigs.size();
        if (priorityType == System.WIFI_PRIORITY_TYPE_DEFAULT) {
            if (mNetworkId != WifiConfiguration.INVALID_NETWORK_ID) {
                mPriority = configuredApCount - mPriority + 1;
            } else {
              //new configured AP will have highest priority by default
                mPriority = 1;
            }
            view.setVisibility(View.GONE);
        } else {
            //view.findViewById(priorityId).setVisibility(View.VISIBLE);
            mPrioritySpinner = (Spinner) view.findViewById(R.id.cmcc_priority_setter);
            if (mPrioritySpinner != null) {
                mPrioritySpinner.setOnItemSelectedListener(this);
                if (mNetworkId != WifiConfiguration.INVALID_NETWORK_ID) {
                    mPriorityArray = new String[configuredApCount];
                } else {
                    //new configured AP, have highest priority by default
                    mPriorityArray = new String[configuredApCount + 1];
                }
                for (int i = 0; i < mPriorityArray.length; i++) {
                    mPriorityArray[i] = String.valueOf(i + 1);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        mContext, android.R.layout.simple_spinner_item, mPriorityArray);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mPrioritySpinner.setAdapter(adapter);
                int priorityCount = mPrioritySpinner.getCount();
                int priorityOrder;
                if (mNetworkId != WifiConfiguration.INVALID_NETWORK_ID) {
                    priorityOrder = priorityCount - mPriority + 1;
                    mPriority = priorityOrder;
                } else {
                  //new configured AP will have highest priority by default
                    priorityOrder = 1;
                    mPriority = 1;
                }
                Log.d("@M_" + TAG, "setPriorityView(), priorityOrder="
                    + priorityOrder + ", mPriority=" + mPriority);
                Log.d("@M_" + TAG, " " + priorityCount);
                mPrioritySpinner.setSelection(
                    priorityCount < priorityOrder ? (priorityCount - 1) : (priorityOrder - 1));
            }
        }

        if (priorityLayout != null) {
              priorityLayout.addView(view, new LinearLayout.LayoutParams(
                      LinearLayout.LayoutParams.MATCH_PARENT,
                      LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(mPrioritySpinner)) {
            mPriority = position + 1;
            Log.d("@M_" + TAG, "onItemSelected(), " + mPriority);
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    @Override
    public void setSecurityText(TextView view) {
        view.setText(mContext.getString(R.string.wifi_security_cmcc));
        Log.d("@M_" + TAG, "set wifi_security_cmcc");
    }

    private boolean shouldSetDisconnectButton() {
        if (mWifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) {
            return true;
        }
        return false;
    }

    @Override
    public void addDisconnectButton(AlertDialog dialog, boolean edit,
                    DetailedState state, WifiConfiguration wifiConfig) {
        if (wifiConfig == null) {
            return;
        }
        final int networkId = wifiConfig.networkId;
        Log.d("@M_" + TAG, "addDisconnectButton, edit = " + edit
            + ", state = " + state + ", networkId = " + networkId);
        if (!edit && state != null && shouldSetDisconnectButton()) {
            //set disconnect button
            dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                mContext.getString(R.string.wifi_disconnect),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d("@M_" + TAG, "addDisconnectButton, onClick, networkId = "
                                + networkId);
                            mWifiManager.disable(networkId, null);
                        }
                    });
        }
    }

    @Override
    public int getPriority(int priority) {
        Log.d("@M_" + TAG, "getPriority" + mPriority);
        return mPriority;
    }

    @Override
    public void setProxyText(TextView view) {
        view.setText(mContext.getString(R.string.proxy_exclusionlist_label_cmcc));
    }

    @Override
    public void initConnectView(Activity activity, PreferenceScreen screen) {
        mConnectTypePref = new ListPreference(activity);
        mConnectTypePref.setTitle(mContext.getString(R.string.wifi_connect_type_title));
        mConnectTypePref.setDialogTitle(mContext.getString(R.string.wifi_connect_type_title));
        mConnectTypePref.setEntries(
            mContext.getResources().getTextArray(R.array.wifi_connect_type_entries));
        mConnectTypePref.setEntryValues(
            mContext.getResources().getTextArray(R.array.wifi_connect_type_values));
        mConnectTypePref.setKey(KEY_CONNECT_TYPE);
        mConnectTypePref.setOnPreferenceChangeListener(mPreferenceChangeListener);
        screen.addPreference(mConnectTypePref);

        mConnectReminderPref = new ListPreference(activity);
        mConnectReminderPref.setTitle(mContext.getString(R.string.wifi_reminder_frequency_title));
        mConnectReminderPref.setDialogTitle(
            mContext.getString(R.string.wifi_reminder_frequency_title));
        mConnectReminderPref.setEntries(
            mContext.getResources().getTextArray(R.array.wifi_reminder_entries));
        mConnectReminderPref.setEntryValues(
            mContext.getResources().getTextArray(R.array.wifi_reminder_values));
        mConnectReminderPref.setKey(KEY_CONNECT_REMINDER);
        mConnectReminderPref.setOnPreferenceChangeListener(mPreferenceChangeListener);
        screen.addPreference(mConnectReminderPref);

        mPriorityTypePref = new SwitchPreference(mContext);
        mPriorityTypePref.setTitle(R.string.wifi_priority_type_title);
        mPriorityTypePref.setSummary(R.string.wifi_priority_type_summary);
        mPriorityTypePref.setKey(KEY_PRIORITY_TYPE);
        mPriorityTypePref.setOnPreferenceChangeListener(mPreferenceChangeListener);
        screen.addPreference(mPriorityTypePref);

        mPrioritySettingPref = new Preference(mContext);
        mPrioritySettingPref.setTitle(R.string.wifi_priority_settings_title);
        mPrioritySettingPref.setSummary(R.string.wifi_priority_settings_summary);
        mPrioritySettingPref.setKey(KEY_PRIORITY_SETTINGS);
        mPrioritySettingPref.setOnPreferenceClickListener(mPreferenceclickListener);
        screen.addPreference(mPrioritySettingPref);

        mPrioritySettingPref.setDependency(KEY_PRIORITY_TYPE);
    }

    @Override
    public void initNetworkInfoView(PreferenceScreen screen) {
        mGatewayPref = new Preference(mContext);
        mGatewayPref.setTitle(mContext.getString(R.string.wifi_gateway));
        screen.addPreference(mGatewayPref);

        mNetmaskPref = new Preference(mContext);
        mNetmaskPref.setTitle(mContext.getString(R.string.wifi_network_netmask));
        screen.addPreference(mNetmaskPref);
    }

    @Override
    public void refreshNetworkInfoView() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        String gateway = null;
        String netmask = null;
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        Log.d("@M_" + TAG, "refreshNetworkInfoView() dhcpInfo = " + dhcpInfo);
        Log.d("@M_" + TAG, "refreshNetworkInfoView() wifiInfo = " + wifiInfo);
        if (wifiInfo != null) {
            if (dhcpInfo != null) {
                int netmaskInt = 0;
                gateway = ipTransfer(dhcpInfo.gateway);
                Log.d("@M_" + TAG, "refreshNetworkInfoView() dhcpInfo.netmask = "
                    + dhcpInfo.netmask);
                ConnectivityManager cm =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                LinkProperties prop = cm.getLinkProperties(ConnectivityManager.TYPE_WIFI);
                if (prop != null) {
                    Log.d("@M_" + TAG, "prop not null");
                    Collection<LinkAddress> linkAddresses = prop.getLinkAddresses();
                    for (LinkAddress addr : linkAddresses) {
                        if (addr.getAddress() instanceof Inet4Address) {
                            netmaskInt = NetworkUtils.prefixLengthToNetmaskInt(
                                addr.getNetworkPrefixLength());
                        }
                    }
                    netmask = ipTransfer(netmaskInt);
                }
            }
        }
        String defaultText = mContext.getString(R.string.status_unavailable);
        mGatewayPref.setSummary(gateway == null ? defaultText : gateway);
        mNetmaskPref.setSummary(netmask == null ? defaultText : netmask);
    }

    private OnPreferenceChangeListener mPreferenceChangeListener =
            new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            Log.d("@M_" + TAG, "key=" + key);

            if (KEY_CONNECT_TYPE.equals(key)) {
                Log.d("@M_" + TAG, "Wifi connect type is " + newValue);
                try {
                    System.putInt(mContext.getContentResolver(),
                            System.WIFI_CONNECT_TYPE,
                            Integer.parseInt(((String) newValue)));
                    if (mConnectTypePref != null) {
                        CharSequence[] array =
                            mContext.getResources().
                            getTextArray(R.array.wifi_connect_type_entries);
                        mConnectTypePref.setSummary(
                            (String) array[Integer.parseInt(((String) newValue))]);
                    }
                } catch (NumberFormatException e) {
                    Log.d("@M_" + TAG, "set Wifi connect type error ");
                    return false;
                }
                try {
                    System.putInt(mContext.getContentResolver(),
                            System.WIFI_SELECT_SSID_TYPE, Integer.parseInt(((String) newValue)));
                } catch (NumberFormatException e) {
                    Log.d("@M_" + TAG, "set Wifi SSID reselect type error ");
                    return false;
                }
            } else if (KEY_PRIORITY_TYPE.equals(key)) {
                boolean checked = ((Boolean) newValue).booleanValue();
                System.putInt(mContext.getContentResolver(), System.WIFI_PRIORITY_TYPE,
                    checked ?
                    System.WIFI_PRIORITY_TYPE_MAMUAL : System.WIFI_PRIORITY_TYPE_DEFAULT);
            } else if (KEY_CONNECT_REMINDER.equals(key)) {
                try {
                    System.putInt(mContext.getContentResolver(),
                            System.WIFI_CONNECT_REMINDER, Integer.parseInt(((String) newValue)));
                    if (mConnectReminderPref != null) {
                        CharSequence[] array =
                            mContext.getResources().getTextArray(R.array.wifi_reminder_entries);
                        mConnectReminderPref.setSummary(
                            (String) array[Integer.parseInt(((String) newValue))]);
                    }
                } catch (NumberFormatException e) {
                    Log.d("@M_" + TAG, "set Wifi connect type error ");
                    return false;
                }
            }
            return true;
        }
    };

    private Preference.OnPreferenceClickListener mPreferenceclickListener =
            new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent("com.mediatek.OP01.PRIORITY_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(mContext,
                    R.string.wifi_priority_missing_app, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    };

    @Override
    public void initPreference(ContentResolver contentResolver) {
        if (mConnectTypePref != null) {
            int value = System.getInt(contentResolver,
                System.WIFI_CONNECT_TYPE,
                System.WIFI_CONNECT_TYPE_AUTO);
            mConnectTypePref.setValue(String.valueOf(value));
            CharSequence[] array =
                mContext.getResources().getTextArray(R.array.wifi_connect_type_entries);
            mConnectTypePref.setSummary((String) array[value]);

            int value1 = System.getInt(contentResolver,
                System.WIFI_SELECT_SSID_TYPE,
                System.WIFI_SELECT_SSID_AUTO);
            if (value != value1) {
                System.putInt(contentResolver, System.WIFI_SELECT_SSID_TYPE, value);
            }
        }
        if (mPriorityTypePref != null) {
            mPriorityTypePref.setChecked(System.getInt(contentResolver,
                    System.WIFI_PRIORITY_TYPE,
                    System.WIFI_PRIORITY_TYPE_DEFAULT) == System.WIFI_PRIORITY_TYPE_MAMUAL);
        }

        if (mConnectReminderPref != null) {
            int value = System.getInt(contentResolver,
                System.WIFI_CONNECT_REMINDER,
                WIFI_CONNECT_REMINDER_ALWAYS);
            mConnectReminderPref.setValue(String.valueOf(value));
            CharSequence[] array =
                mContext.getResources().getTextArray(R.array.wifi_reminder_entries);
            mConnectReminderPref.setSummary((String) array[value]);
        }
    }

    private String ipTransfer(int value) {
        String result = null;
        if (value != 0) {
            if (value < 0) {
                value += 0x100000000L;
            }
            result = String.format("%d.%d.%d.%d",
                    value & 0xFF, (value >> 8) & 0xFF, (value >> 16) & 0xFF, (value >> 24) & 0xFF);
        }
        return result;
    }

    private static String addQuote(String s) {
        return "\"" + s + "\"";
    }
    private int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }

        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

     @Override
     public void setEapMethodArray(ArrayAdapter adapter, String ssid, int security) {
        Log.d("@M_" + TAG, "setEapMethodArray()");
        if (ssid != null
            && ((CMCC_SSID.equals(ssid) && SECURITY_EAP == security)
                || (CMCC_AUTO_SSID.equals(ssid) && SECURITY_EAP == security))) {
            String[] eapString =
                mContext.getResources().getStringArray(R.array.wifi_eap_method_values);
            adapter.clear();
            for (int i = 0; i < WIFI_EAP_METHOD_NUM; i++) {
                adapter.insert(eapString[i], i);
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

    @Override
    public void hideWifiConfigInfo(Builder builder , Context context) {
        if (builder != null) {
            AccessPoint accessPoint = builder.getAccessPoint();
            View view = (View) builder.getViews();
            Log.d("@M_" + TAG, "hideWifiConfigInfo():" + accessPoint);
            if (accessPoint == null || view == null) {
                return;
            }
            String ssid = accessPoint.getSsidStr();
            int security = accessPoint.getSecurity();
            Log.d("@M_" + TAG, "hideWifiConfigInfo():ssid" + ssid + "," + security);
           if (ssid == null
              || !((CMCC_SSID.equals(ssid) && SECURITY_EAP == security)
                    || (CMCC_AUTO_SSID.equals(ssid)
                        && SECURITY_EAP == security))) {
               return;
            }

            int networkId = WifiConfiguration.INVALID_NETWORK_ID;
            WifiConfiguration wifiConfig = accessPoint.getConfig();
            if (wifiConfig != null) {
                networkId = wifiConfig.networkId;
            }
            boolean edit = builder.getEdit();
            List<View> lists = new ArrayList<View>();
            Resources res = null;
            res = context.getResources();
            String packageName = context.getPackageName();
            lists.add(view.findViewById(
                res.getIdentifier("info", "id", packageName)));
            lists.add(view.findViewById(
                res.getIdentifier("priority_field", "id", packageName)));
            lists.add(view.findViewById(
                res.getIdentifier("proxy_settings_fields", "id", packageName)));
            lists.add(view.findViewById(
                res.getIdentifier("ip_fields", "id", packageName)));

            if ((networkId == WifiConfiguration.INVALID_NETWORK_ID)
                || (networkId != WifiConfiguration.INVALID_NETWORK_ID && edit)) {
                for (int i = 0; i < lists.size(); i++) {
                    ((View) lists.get(i)).setVisibility(View.GONE);
                }
                LinearLayout eap = (LinearLayout) view.findViewById(
                    res.getIdentifier("eap", "id", packageName));
                int count = eap.getChildCount();
                for (int j = WIFI_EAP_SKIP_NUM; j < count; j++) {
                    ((View) eap.getChildAt(j)).setVisibility(View.GONE);
                }
                LinearLayout identity = (LinearLayout) view.findViewById(
                    res.getIdentifier("l_identity", "id", packageName));
                identity.setVisibility(View.VISIBLE);
            }
        }
    }

     @Override
     public int getEapMethodbySpinnerPos(int spinnerPos, String ssid, int security) {
        Log.d("@M_" + TAG, "getEapMethodbySpinnerPos() spinnerPos = " + spinnerPos);
        Log.d("@M_" + TAG, "getEapMethodbySpinnerPos() ssid = " + ssid);
        Log.d("@M_" + TAG, "getEapMethodbySpinnerPos() security = " + security);
        if (ssid != null
            && ((CMCC_SSID.equals(ssid) && SECURITY_EAP == security)
                || (CMCC_AUTO_SSID.equals(ssid) && SECURITY_EAP == security))) {
            if (spinnerPos == 1) {
                spinnerPos = WIFI_EAP_METHOD_SIM;
            } else {
                spinnerPos = WIFI_EAP_METHOD_PEAP;
            }
        }
        Log.d("@M_" + TAG, "getEapMethodbySpinnerPos() EapMethod = " + spinnerPos);
        if (spinnerPos < 0) {
            spinnerPos = 0;
        }
        return spinnerPos;
    }

    @Override
    public int getPosByEapMethod(int spinnerPos, String ssid, int security) {
        Log.d("@M_" + TAG, "getPosByEapMethod() EapMethod = " + spinnerPos);
        Log.d("@M_" + TAG, "getPosByEapMethod() ssid = " + ssid);
        Log.d("@M_" + TAG, "getPosByEapMethod() security = " + security);
        if (ssid != null
            && ((CMCC_SSID.equals(ssid) && SECURITY_EAP == security)
                || (CMCC_AUTO_SSID.equals(ssid) && SECURITY_EAP == security))) {
            if (spinnerPos == WIFI_EAP_METHOD_SIM) {
                spinnerPos = 1;
            } else {
                spinnerPos = 0;
            }
        }

        Log.d("@M_" + TAG, "getPosByEapMethod() spinnerPos = " + spinnerPos);
        if (spinnerPos < 0) {
            spinnerPos = 0;
        }
        return spinnerPos;
    }
}

