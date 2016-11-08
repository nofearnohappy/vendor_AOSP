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

package com.mediatek.bluetooth.service;

import android.bluetooth.BluetoothAdapter;
import com.mediatek.bluetooth.BluetoothBipi;
import com.mediatek.bluetooth.BluetoothBipr;
import android.bluetooth.BluetoothDevice;
import com.mediatek.bluetooth.BluetoothDun;
import com.mediatek.bluetooth.BluetoothFtp;
import android.bluetooth.BluetoothPbap;
import com.mediatek.bluetooth.BluetoothProfileManager;
import com.mediatek.bluetooth.BluetoothProfileManager.BluetoothProfileBehavior;
import com.mediatek.bluetooth.BluetoothProfileManager.Profile;
import com.mediatek.bluetooth.BluetoothPrxm;
import com.mediatek.bluetooth.BluetoothPrxr;
import com.mediatek.bluetooth.BluetoothSimap;
import com.mediatek.bluetooth.IBluetoothProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.os.UserHandle;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import java.util.Set;


/**
 * @hide
 */
public class BluetoothProfileManagerService extends IBluetoothProfileManager.Stub implements
/* BluetoothHeadset.ServiceListener, */BluetoothPbap.ServiceListener {
    private static final String TAG = "BluetoothProfileManagerService";

    private static final boolean DBG = true;

    private static final int MESSAGE_MONITOR_TIMEOUT = 1;

    private long mBluetoothEnableTime = 0; // store the time when BT succeeds to turn on

    public static final String BLUETOOTH_PROFILEMANAGER_SERVICE = "bluetooth_profile_manager";

    private static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;

    private final Context mContext;

    private final IntentFilter mIntentFilter;

    private HashMap<Profile, BluetoothProfileBehavior> mServiceList;

    private static Set<Profile> sConenctedProfileList;

    // private Set<Profile> mMonitoringProfiles = new HashSet<Profile>();

    private BroadcastReceiver mBroadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("action:" + action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        onBluetoothEnable();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        onBluetoothDisable();
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        // EventHandler(Event.BluetoothTurnOff);
                    default:
                }
            } else if (action.equals(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE)) {
                Profile profile = (Profile) intent.getSerializableExtra(BluetoothProfileManager.EXTRA_PROFILE);
                int state = intent.getIntExtra(BluetoothProfileManager.EXTRA_NEW_STATE,
                        BluetoothProfileManager.STATE_ABNORMAL);
                onProfileStateUpdate(profile, state);
            } else if (action.equals(BluetoothProfileManager.ACTION_STATE_CHANGED)) {
                Profile profile = (Profile) intent.getSerializableExtra(BluetoothProfileManager.EXTRA_PROFILE);
                int state = intent.getIntExtra(BluetoothProfileManager.EXTRA_NEW_STATE,
                        BluetoothProfileManager.STATE_ABNORMAL);
                updateProfileState(profile, state);
            }

        }
    };

    public BluetoothProfileManagerService(Context context) {

        mContext = context;
        mServiceList = new HashMap<Profile, BluetoothProfileBehavior>();
        sConenctedProfileList = new HashSet<Profile>();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE);
        mIntentFilter.addAction(BluetoothProfileManager.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastreceiver, mIntentFilter);
    }

    private void onBluetoothEnable() {
        log("onBluetoothEnable");

        /* A2DP will NOT update state to profile manager, so create A2DP proxy when Bluetooth is enabled */
        updateProfileServiceList(Profile.A2DP);

        /* when phone boot up, the intent that headset service broadcasts will NOT be received by any receiver */
        /* so just create Headset proxy to avoid it. */
        updateProfileServiceList(Profile.HEADSET);

        Log.e("PRX", "hard code service registered.");
        updateProfileServiceList(Profile.PRXM);
        updateProfileServiceList(Profile.PRXR);
    }

    private void onBluetoothDisable() {
        log("onBluetoothDisable");
        Iterator service = mServiceList.entrySet().iterator();
        while (service.hasNext()) {

            // ((Map.Entry)service.next()).getValue().close();
            // ((BluetoothProfileBehavior)service.next().getValue()).close();
            Map.Entry entry = (Map.Entry) service.next();
            ((BluetoothProfileBehavior) entry.getValue()).close();
            service.remove();
        }
        log("mServiceList size is " + mServiceList.size());

        sendMonitorMessage(BluetoothProfileManager.ACTION_DISABLE_PROFILES);

    }

    public boolean connect(String profileName, BluetoothDevice device) {
        log("connect:" + profileName + " device:" + device.getName());

        /** enqueue the command */
        /***/
        Profile profile = Profile.valueOf(profileName);
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService == null) {
            log("connect():mService is null");
            return false;
        }

        if (!isConnectableToMultiDevices(profile)) {
            Set<BluetoothDevice> sinks = null;
            if (profile.equals(Profile.A2DP)) {
                log("in connect profile equals Bluetooth_A2DP");
                // sinks = ((BluetoothA2dp)mService).getNonDisconnectedSinks();
            } else {
                sinks = mService.getConnectedDevices();
            }
            if (sinks != null) {
                for (BluetoothDevice sink : sinks) {
                    // try {
                    mService.disconnect(sink);
                    // } catch (Exception e) {
                    // log(e.toString());
                    // e.printStackTrace();
                    // }
                }
            }
        }

        // try {
        mService.connect(device);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        return true;

    }

    private boolean isConnectableToMultiDevices(Profile profile) {
        log("isConnectableToMultiDevices:" + profile.name());

        return (profile.equals(Profile.HID));
    }

    public boolean disconnect(String profileName, BluetoothDevice device) {
        log("disconnect:" + profileName + " device:" + device.getName());

        Profile profile = Profile.valueOf(profileName);
        Set<BluetoothDevice> devices = null;
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService == null) {
            log("disconnect():mService is null");
            return false;
        }

        // try {
        devices = mService.getConnectedDevices();
        // } catch (Exception e) {
        // log(e.toString());
        // e.printStackTrace();
        // }
        if ((devices == null) || (!devices.contains(device))) {
            return false;
        }

        /*
         * if (profile.equals(Profile.HEADSET)) { // Downgrade prority as user is disconnecting the headset. if
         * (((BluetoothHeadset)mService).getPriority(device) > BluetoothHeadset.PRIORITY_ON) {
         * ((BluetoothHeadset)mService).setPriority(device, BluetoothHeadset.PRIORITY_ON); } } else if
         * (profile.equals(Profile.A2DP)) { if (((BluetoothA2dp)mService).getSinkPriority(device) >
         * BluetoothA2dp.PRIORITY_ON) { ((BluetoothA2dp)mService).setSinkPriority(device, BluetoothA2dp.PRIORITY_ON); } }
         */

        // try {
        mService.disconnect(device);
        // } catch (Exception e) {
        // log(e.toString());
        // e.printStackTrace();
        // }

        return true;
    }

    public BluetoothDevice[] getConnectedDevices(String profileName) {
        log("getConnectedDevices:" + profileName);

        Profile profile = Profile.valueOf(profileName);
        Set<BluetoothDevice> devices = null;
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService != null) {
            // try {
            devices = mService.getConnectedDevices();
            // } catch (Exception e) {
            // log(e.toString());
            // e.printStackTrace();
            // }
        } else {
            log("getConnectedDevices():mService is null");
        }

        devices = (devices == null) ? (new HashSet<BluetoothDevice>()) : devices;

        return devices.toArray(new BluetoothDevice[devices.size()]);
    }

    public int getState(String profileName, BluetoothDevice device) {
        // log("getState:"+profileName+" device:"+device.getName());

        Profile profile = Profile.valueOf(profileName);
        int state = BluetoothProfileManager.STATE_UNKNOWN;
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService != null) {
            // try {
            state = mService.getState(device);
            // } catch (Exception e) {
            // log(e.toString());
            // e.printStackTrace();
            // }
        } else {
            state = -1;
        }
        return state;
    }

    public boolean isPreferred(String profileName, BluetoothDevice device) {
        log("isPreferred:" + profileName + " device:" + device.getName());

        Profile profile = Profile.valueOf(profileName);
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService == null) {
            log("isPreferred():mService is null");
            return false;
        }
        /*
         * switch (profile) { case Bluetooth_A2DP: return ((BluetoothA2dp)mService).getSinkPriority(device) >
         * BluetoothA2dp.PRIORITY_OFF; case Bluetooth_HEADSET: return ((BluetoothHeadset)mService).getPriority(device) >
         * BluetoothHeadset.PRIORITY_OFF; default: return false; }
         */ return false;
    }

    public boolean setPreferred(String profileName, BluetoothDevice device, boolean preferred) {
        log("setPreferred:" + profileName + " device:" + device.getName() + " value" + preferred);

        Profile profile = Profile.valueOf(profileName);
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService == null) {
            log("setPreferred():mService is null");
            return false;
        }
        /*
         * if (profile == Profile.A2DP) { BluetoothA2dp mA2dpService=(BluetoothA2dp)mService; if (preferred) { if
         * (mA2dpService.getSinkPriority(device) < BluetoothA2dp.PRIORITY_ON) { mA2dpService.setSinkPriority(device,
         * BluetoothA2dp.PRIORITY_ON); } } else { mA2dpService.setSinkPriority(device, BluetoothA2dp.PRIORITY_OFF); } } if
         * (profile == Profile.HEADSET) { BluetoothHeadset mHeadsetService=(BluetoothHeadset)mService; if
         * (preferred) { if (mHeadsetService.getPriority(device) < BluetoothHeadset.PRIORITY_ON) {
         * mHeadsetService.setPriority(device, BluetoothHeadset.PRIORITY_ON); } } else { mHeadsetService.setPriority(device,
         * BluetoothHeadset.PRIORITY_OFF); } }
         */
        return true;
    }

    public int getPreferred(String profileName, BluetoothDevice device) {
        log("getPreferred:" + profileName + " device:" + device.getName());

        Profile profile = Profile.valueOf(profileName);
        BluetoothProfileBehavior mService = checkProfileService(profile);
        if (mService == null) {
            log("getPreferred():mService is null");
            return -1;
        }
        /*
         * if(profile == Profile.A2DP) { BluetoothA2dp mA2dpService=(BluetoothA2dp)mService; return
         * mA2dpService.getSinkPriority(device); } else if (profile == Profile.HEADSET) { BluetoothHeadset
         * mHeadsetService=(BluetoothHeadset)mService; return mHeadsetService.getPriority(device); }
         */

        return -1;
    }

    private BluetoothProfileBehavior checkProfileService(Profile profile) {
        // if ((mManagerPhase == ManagerPhase.MonitorTurnOff) || (mManagerPhase == ManagerPhase.Idle)) {
        // return null;
        // }
        if (mServiceList.containsKey(profile)) {
            return mServiceList.get(profile);
        } else {
            return null;
        }
    }

    private void onProfileStateUpdate(Profile profile, int state) {
        log("onProfileStateUpdate():profile->" + profile + ",state->" + state);

        if (state == BluetoothProfileManager.STATE_ENABLED) {
            updateProfileServiceList(profile);
        }

    }

    private void updateProfileServiceList(Profile profile) {

        BluetoothProfileBehavior profileBehavior = null;

        if (mServiceList.containsKey(profile)) {
            return;
        }

        profileBehavior = getProfileBehavior(profile);

        if (profileBehavior != null) {
            mServiceList.put(profile, profileBehavior);
        }
    }

    private BluetoothProfileBehavior getProfileBehavior(Profile profile) {
        BluetoothProfileBehavior profileBehavior = null;
        switch (profile) {
            case HEADSET:
                // profileBehavior = new BluetoothHeadset(mContext,this);
                break;
            case A2DP:
                // profileBehavior = new BluetoothA2dp(mContext);
                break;
            case HID:
                // profileBehavior = new BluetoothHid(mContext);
                break;
            case FTP_CLIENT:
                profileBehavior = new BluetoothFtp.Client(mContext);
                break;
            case FTP_SERVER:
                profileBehavior = new BluetoothFtp.Server(mContext);
                break;
            case BIP_INITIATOR:
                profileBehavior = new BluetoothBipi(mContext);
                break;
            case BIP_RESPONDER:
                profileBehavior = new BluetoothBipr(mContext);
                break;
            // case Bluetooth_BPP_Sender:
            case SIMAP:
                profileBehavior = new BluetoothSimap(mContext);
                break;
            case PBAP:
                // profileBehavior = new BluetoothPbap(mContext,this);
                break;
            case OPP_SERVER:
                // profileBehavior = new BluetoothOpp.Server(mContext);
                break;
            case OPP_CLIENT:
                // profileBehavior = new BluetoothOpp.Client(mContext);
                break;
            case DUN:
                profileBehavior = new BluetoothDun(mContext);
                break;
            case PRXM:
                profileBehavior = new BluetoothPrxm(mContext);
                break;
            case PRXR:
                profileBehavior = new BluetoothPrxr(mContext);
                break;
            case PAN_NAP:
                // profileBehavior = new BluetoothPan.NAP(mContext);
                break;
            case PAN_GN:
                // profileBehavior = new BluetoothPan.GN(mContext);
            default:
                log("unexpected profile");
        }

        return profileBehavior;

    }

    private void sendMonitorMessage(String action) {
        log("sendMonitorMessage():action->" + action);
        Intent intent = new Intent(action);
        //mContext.sendBroadcast(intent, BLUETOOTH_PERM);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    private void updateProfileState(Profile profile, int state) {
        log("updateProfileState:" + profile.name() + ", new state is " + state);

        /** if a profile is connected, add it to profile list */
        /** if a profile is not connected, fisrt check whether it allows to connnect multi devices */
        int mProfileSize = sConenctedProfileList.size();
        if (state == BluetoothProfileManager.STATE_CONNECTED || state == BluetoothProfileManager.STATE_ACTIVE) {
            if (!sConenctedProfileList.contains(profile)) {
                sConenctedProfileList.add(profile);
                log("add profile:" + profile + " is added to connected list");
            } else {
                log("add profile:the profile(" + profile.name() + ") has been connected");
            }
        } else if (state == BluetoothProfileManager.STATE_DISCONNECTED) {
            if (sConenctedProfileList.contains(profile)) {
                if (!profile.equals(Profile.HID)) {
                    sConenctedProfileList.remove(profile);
                    log("remove profile:" + profile);
                } else if (getConnectedDevices(Profile.HID.name()).length == 0) {
                    sConenctedProfileList.remove(profile);
                    log("remove profile:" + profile);
                } else {
                    log("remove profile:" + profile + " still has connected device");
                }
            } else {
                log("remove profile failure:the profile(" + profile.name() + ") is not in connected profile list");
            }
        }

        if ((mProfileSize > 0) && (sConenctedProfileList.size() == 0)) {
            updateBluetoothNotification(false);
        }
        if ((mProfileSize == 0) && (sConenctedProfileList.size() > 0)) {
            updateBluetoothNotification(true);
        }
    }

    private void updateBluetoothNotification(boolean enable) {
        log("updateBluetoothNotification:" + enable);

        Intent intent = new Intent(BluetoothProfileManager.ACTION_UPDATE_NOTIFICATION);
        if (enable) {
            intent.putExtra(BluetoothProfileManager.EXTRA_NEW_STATE, BluetoothProfileManager.STATE_CONNECTED);
            intent.putExtra(BluetoothProfileManager.EXTRA_PREVIOUS_STATE, BluetoothProfileManager.STATE_DISCONNECTED);
        } else {
            intent.putExtra(BluetoothProfileManager.EXTRA_NEW_STATE, BluetoothProfileManager.STATE_DISCONNECTED);
            intent.putExtra(BluetoothProfileManager.EXTRA_PREVIOUS_STATE, BluetoothProfileManager.STATE_CONNECTED);
        }
        // mContext.sendBroadcast(intent, BLUETOOTH_PERM);
        //mContext.sendBroadcast(intent, BLUETOOTH_PERM);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    public void onServiceConnected(BluetoothPbap proxy) {

    }

    public void onServiceDisconnected() {

    }

    private static void log(String msg) {
        Log.d("@M_" + TAG, "[BT][profile manager]" + msg);
    }

}
