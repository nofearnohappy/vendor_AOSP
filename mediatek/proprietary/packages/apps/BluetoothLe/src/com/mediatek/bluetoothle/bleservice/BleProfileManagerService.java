/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2014. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.bluetoothle.bleservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.mediatek.bluetooth.BleProfileServiceManager;
import com.mediatek.bluetoothle.R;
import com.mediatek.bluetoothle.provider.DeviceParameterRecorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * BleProfileManagerService class
 */
public class BleProfileManagerService extends Service {
    private static final boolean DBG = true;
    private static final String TAG = "BleProfileManagerService";
    private static final String SHARED_PREFERENCE = "settings";
    private static final String SHARED_PREFERENCE_BG_MODE = "bBackgroundEnabled";
    private static final boolean TRACE_REF = true;
    private static int sRefCount = 0;
    private static BleProfileManagerService sInstance;
    private static BluetoothAdapter sAdapter = BluetoothAdapter.getDefaultAdapter();
    private BleProfileManagerServiceBinder mBinder;
    private BleProfileManagerState mStateMachine;
    private Context mContext;
    private boolean mCleaningUp;
    private int[] mProfileIds = null;
    private final HashMap<String, Integer> mProfileServicesState = new HashMap<String, Integer>();
    private final SparseIntArray mProfileServerState = new SparseIntArray();
    private final SparseArray<IProfileServiceManagerCallback> mCallbacks =
            new SparseArray<IProfileServiceManagerCallback>();
    private final ClientDeathRecipient mClientDeathRecipient =
            new ClientDeathRecipient();
    private final HashMap<BluetoothDevice, Integer> mDeviceConnCount =
            new HashMap<BluetoothDevice, Integer>();
    private static final int MESSAGE_PROFILE_CONNECTION_STATE_CHANGED = 20;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (DBG) {
                Log.d(TAG, "Message: " + msg.what);
            }

            switch (msg.what) {
                case MESSAGE_PROFILE_CONNECTION_STATE_CHANGED:
                    if (DBG) {
                        Log.d(TAG, "MESSAGE_PROFILE_CONNECTION_STATE_CHANGED");
                    }

                    final BluetoothDevice device = (BluetoothDevice) msg.obj;
                    final int newState = msg.arg1;

                    Integer prevCount = mDeviceConnCount.get(device);
                    if (null == prevCount) {
                        prevCount = 0;
                    }

                    updateDeviceConnCounter(device, newState);

                    Integer newCount = mDeviceConnCount.get(device);
                    if (null == newCount) {
                        newCount = 0;
                    }

                    // Server is connected, try connect client
                    if (prevCount.intValue() == 0 &&
                            newCount.intValue() != prevCount.intValue()) {
                        final List<BluetoothDevice> deviceList = getSavedDevices();

                        // The device is recognized by BleManager
                        if (deviceList.contains(device)) {
                            final ClientDeviceConnector connector =
                                    ClientDeviceConnector.getInstance();
                            connector.connectDevice(device);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private List<BluetoothDevice> getSavedDevices() {
        return getSavedDevices(this);
    }

    private Context getContext() {
        return (mContext == null) ? this : mContext;
    }

    /* package */static List<BluetoothDevice> getSavedDevices(final Context ctx) {
        final List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

        final List<String>  addrList = DeviceParameterRecorder.getDeviceAddresses(ctx);

        if (addrList == null) {
            if (DBG) Log.d(TAG, "No devices in BleManager");
            return deviceList;
        }

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        for (String addr : addrList) {
            if (addr == null) continue;

            final BluetoothDevice device = adapter.getRemoteDevice(addr);

            if (device != null) {
                deviceList.add(device);
                if (DBG) Log.d(TAG, "BleManager-managed device : " + addr);
            }
        }

        return deviceList;
    }

    /**
     * Constructor for BleProfileManagerService
     */
    public BleProfileManagerService() {
        super();
        if (TRACE_REF) {
            synchronized (BleProfileManagerService.class) {
                sRefCount++;
                if (DBG) {
                    Log.d(TAG, "REFCOUNT: CREATED. INSTANCE_COUNT" + sRefCount);
                }
            }
        }
    }

    @Override
    protected void finalize() {
        if (TRACE_REF) {
            synchronized (BleProfileManagerService.class) {
                sRefCount--;
                if (DBG) {
                    Log.d(TAG, "REFCOUNT: FINALIZED. INSTANCE_COUNT= " + sRefCount);
                }
            }
        }
    }

    void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }

        if (mCleaningUp) {
            if (DBG) {
                Log.d(TAG, " service already starting to cleanup... Ignoring request.");
            }
            return;
        }

        mBinder = null;
        mCleaningUp = true;
        if (null != mStateMachine) {
            mStateMachine.doQuit();
            mStateMachine = null;
        }
        clearAdapterService();
        if (DBG) {
            Log.d(TAG, "cleanup() done");
        }
    }

    private static synchronized void clearAdapterService() {
        setBleProfileManagerService(null);
    }

    /**
     * Method to get the singleton of BleProfileManagerService object
     *
     * @return the singleton of BleProfileManagerService object
     */
    public static synchronized BleProfileManagerService getBleProfileManagerService() {
        if (sInstance != null && !sInstance.mCleaningUp) {
            if (DBG) {
                Log.d(TAG, "getBleProfileManagerService: returning " + sInstance);
            }
            return sInstance;
        }
        if (DBG) {
            if (sInstance == null) {
                if (DBG) {
                    Log.d(TAG, "getBleProfileManagerService: service not available");
                }
            } else if (sInstance.mCleaningUp) {
                if (DBG) {
                    Log.d(TAG, "getBleProfileManagerService: service is cleaning up");
                }
            }
        }
        return null;
    }

    void onProfileServiceStateChanged(final String serviceName, final int state) {
        if (DBG) {
            Log.d(TAG, "onProfileServiceStateChanged- serviceName:" + serviceName + " state:"
                    + state);
        }
        processProfileServiceStateChanged(serviceName, state);
    }

    void shutdown() {
        stopSelf();
        sendBgServiceNotification(false);
    }

    @SuppressWarnings("rawtypes")
    void startProfileServices() {
        if (DBG) {
            Log.d(TAG, "processStart()");
        }

        final Class[] supportedProfileServices = Config.getSupportedProfiles();
        if (supportedProfileServices.length > 0) {
            setProfileServiceState(supportedProfileServices, BluetoothAdapter.STATE_ON);
            sendBgServiceNotification(true);
        } else {
            if (DBG) {
                Log.d(TAG, "processStart(): Profile Services alreay started");
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void initProfileServicesState() {
        final Class[] supportedProfileServices = Config.getSupportedProfiles();
        for (int i = 0; i < supportedProfileServices.length; i++) {
            mProfileServicesState.put(supportedProfileServices[i].getName(),
                    BluetoothAdapter.STATE_OFF);
        }
    }

    @SuppressWarnings("rawtypes")
    boolean stopProfileServices() {
        final Class[] supportedProfileServices = Config.getSupportedProfiles();
        if (0 < supportedProfileServices.length) {
            setProfileServiceState(supportedProfileServices, BluetoothAdapter.STATE_OFF);
            return true;
        } else {
            if (DBG) {
                Log.d(TAG, "stopProfileServices(): "
                        + "No profiles services to stop or already stopped.");
            }
            return false;
        }
    }

    void dumpProfileServiceInfo() {
        if (DBG) {
            Log.d(TAG, "[dumpProfileServiceInfo]");
        }
        synchronized (mProfileServicesState) {
            final Iterator<Map.Entry<String, Integer>> i =
                    mProfileServicesState.entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry<String, Integer> entry = i.next();
                if (DBG) {
                    Log.d(TAG,
                            "Profile: " + entry.getKey() + " State: "
                                    + state2Str(entry.getValue()));
                }
            }
        }
        if (DBG) {
            Log.d(TAG, "[[dumpProfileServiceInfo]]");
        }
    }

    private String state2Str(final int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                return "On";
            case BluetoothAdapter.STATE_OFF:
                return "Off";
            case BluetoothAdapter.STATE_TURNING_OFF:
                return "Off ING";
            case BluetoothAdapter.STATE_TURNING_ON:
                return "On ING";
            default:
                return "STATE_UNKNOWN";
        }
    }

    void setProfileServerState(final int profileId, final int state) {
        mProfileServerState.put(profileId, state);
    }

    void onProfileConnectionStateChanged(final BluetoothDevice device, final int newState) {
        final Message m = mHandler.obtainMessage(MESSAGE_PROFILE_CONNECTION_STATE_CHANGED);
        m.obj = device;
        m.arg1 = newState;
        mHandler.sendMessage(m);
    }

    private void updateDeviceConnCounter(final BluetoothDevice device, final int newState) {
        if (DBG) {
            Log.d(TAG, "updateDeviceConnCounter: device = " + device + "," + newState);
        }

        Integer count = mDeviceConnCount.get(device);
        // Init counter
        if (count == null) {
            count = 0;
            mDeviceConnCount.put(device, count);
        }

        // Increase conn count
        if (BluetoothProfile.STATE_CONNECTED == newState) {
            final int newCount = ++count;
            mDeviceConnCount.put(device, newCount);
        }

        // Decrease conn count
        if (BluetoothProfile.STATE_DISCONNECTED == newState) {
            int newCount = --count;

            if (newCount < 0) {
                newCount = 0;
            }

            mDeviceConnCount.put(device, newCount);
        }

        if (DBG) {
            Log.d(TAG, "<<dump Server Connection counter>>:\n" + mDeviceConnCount);
        }
    }

    private void sendBgServiceNotification(final boolean bStarted) {
        final int notifyID = 1;
        final Resources r = getContext().getResources();
        final BleProfileServiceHelper srvHelper =
                new BleProfileServiceHelper(this);

        if (DBG) {
            Log.d(TAG, "sendBgServiceNotification: bStarted = " + bStarted);
        }
        if (bStarted) {
            Intent notificationIntent = new Intent();
            notificationIntent.setClassName("com.mediatek.blemanager", "com.mediatek.blemanager.ui.BleLauncherActivity");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
            
            final String title = (null != r) ? r.getString(R.string.bg_service_name) : "";
            final String text = (null != r) ? r.getString(R.string.bg_service_active) : "";
            final Notification.Builder notifyBuilder = new Notification.Builder(getContext()).setOngoing(true)
                    .setSmallIcon(R.drawable.app_ble_icon)
                    .setContentTitle(title)
                    .setContentIntent(intent)
                    .setContentText(text);
            srvHelper.startForegroundCompat(notifyID, notifyBuilder.build());
        } else {
            srvHelper.stopForegroundCompat(notifyID);
        }
    }

    private void processProfileServiceStateChanged(final String serviceName, final int state) {
        synchronized (mProfileServicesState) {
            final Integer prevState = mProfileServicesState.get(serviceName);
            if (DBG) {
                Log.d(TAG, "onProfileServiceStateChange: serviceName=" + serviceName + ", state = "
                        + state2Str(state) + ", prevState = " + prevState);
            }
            if (prevState != null && prevState != state) {
                mProfileServicesState.put(serviceName, state);
                if (isAllProfilesStateTheSame(state)) {
                    if (state == BluetoothAdapter.STATE_ON) {
                        mStateMachine.sendMessage(BleProfileManagerState.PROFILES_STARTED);
                    }
                    if (state == BluetoothAdapter.STATE_OFF) {
                        mStateMachine.sendMessage(BleProfileManagerState.PROFILES_STOPPED);
                    }
                }
            }
            dumpProfileServiceInfo();
        }
    }

    boolean isAllProfilesStateTheSame(final int state) {
        boolean bRet = true;
        synchronized (mProfileServicesState) {
            final Iterator<Map.Entry<String, Integer>> i =
                    mProfileServicesState.entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry<String, Integer> entry = i.next();
                if (DBG) {
                    Log.d(TAG,
                            "Profile: " + entry.getKey()
                            + " State: " + state2Str(entry.getValue()));
                }
                if (state != entry.getValue()) {
                    bRet = false;
                    break;
                }
            }
        }
        return bRet;
    }

    @SuppressWarnings("rawtypes")
    private void setProfileServiceState(final Class[] services, final int state) {
        if (state != BluetoothAdapter.STATE_ON && state != BluetoothAdapter.STATE_OFF) {
            if (DBG) {
                Log.d(TAG, "setProfileServiceState(): invalid state...Leaving...");
            }
            return;
        }

        int expectedCurrentState = BluetoothAdapter.STATE_OFF;
        int pendingState = BluetoothAdapter.STATE_TURNING_ON;
        if (state == BluetoothAdapter.STATE_OFF) {
            expectedCurrentState = BluetoothAdapter.STATE_ON;
            pendingState = BluetoothAdapter.STATE_TURNING_OFF;
        }

        synchronized (mProfileServicesState) {
            if (DBG) {
                Log.v(TAG, "service num:" + services.length);
            }
            for (int i = 0; i < services.length; i++) {
                final String serviceName = services[i].getName();
                final Integer serviceState = mProfileServicesState.get(serviceName);
                if (serviceState != null && serviceState != expectedCurrentState) {
                    if (DBG) {
                        Log.d(TAG, "Unable to "
                                + (state == BluetoothAdapter.STATE_OFF ? "start" : "stop")
                                + " service " + serviceName + ". Invalid state: " + serviceState);
                    }
                    continue;
                }

                if (DBG) {
                    Log.d(TAG, (state == BluetoothAdapter.STATE_OFF ? "Stopping" : "Starting")
                            + " service " + serviceName);
                }

                mProfileServicesState.put(serviceName, pendingState);
                final Intent intent = new Intent(getContext(), services[i]);
                intent.putExtra(BleApp.EXTRA_ACTION, BleApp.ACTION_SERVICE_STATE_CHANGED);
                intent.putExtra(BluetoothAdapter.EXTRA_STATE, state);
                getContext().startService(intent);
            }
        }
    }

    private static synchronized void setBleProfileManagerService(
            final BleProfileManagerService instance) {
        if (instance != null && !instance.mCleaningUp) {
            sInstance = instance;
            if (DBG) {
                Log.d(TAG, "setBleProfileManagerService: set to: " + sInstance);
            }
        } else {
            if (DBG) {
                if (sInstance == null) {
                    if (DBG) {
                        Log.d(TAG, "setBleProfileManagerService: service not available");
                    }
                } else if (sInstance.mCleaningUp) {
                    if (DBG) {
                        Log.d(TAG, "setBleProfileManagerService: service is cleaning up");
                    }
                }
            }
        }
    }

    @Override
    public void onCreate() {
        if (DBG) {
            Log.d(TAG, "onCreate");
        }
        mBinder = new BleProfileManagerServiceBinder();
        mStateMachine = BleProfileManagerState.make(this);
        sendBgServiceNotification(false);
        setBleProfileManagerService(this);
        initProfileServicesState();
    }

    @Override
    public void onDestroy() {
        if (DBG) {
            Log.d(TAG, "onDestroy");
        }
        dumpProfileServiceInfo();
        cleanup();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (DBG) {
            Log.d(TAG, "onStartCommand: intent=" + intent + " flags=" + flags + " startId="
                    + startId + " this=" + this + "getBleProfileManagerService()="
                    + getBleProfileManagerService());
        }
        if (null == sAdapter) {
            // it shouldn't happen
            Log.i(TAG, "onStartCommand: null == sAdapter!");
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        String action = null;
        int state = BluetoothAdapter.ERROR;
        if (null == intent) {
            if (!isBackgroundModeEnabled(getContext())) {
                Log.i(TAG, "onStartCommand: in non-background mode!");
                Log.i(TAG, "onStartCommand: wait caller to restart it!");
            } else {
                Log.i(TAG, "onStartCommand: restart in background mode!");
                broadcastToServices(true);
            }
            stopSelf();
            return Service.START_NOT_STICKY;
        } else {
            action = intent.getStringExtra(BleApp.EXTRA_ACTION);
            state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        }

        if (BleApp.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
            if (state == BluetoothAdapter.STATE_OFF) {
                if (DBG) {
                    Log.d(TAG, "Received stop request! SendMessage to state machine...");
                }
                mStateMachine.sendMessage(BleProfileManagerState.STOP_PROFILES);
            } else if (state == BluetoothAdapter.STATE_ON) {
                if (DBG) {
                    Log.d(TAG, "Received start request! SendMessage to state machine...");
                }
                mStateMachine.sendMessage(BleProfileManagerState.START_PROFILES);
            }
        } else {
            // ignore
            if (DBG) {
                Log.d(TAG, "Received unknown itent:" + intent);
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    /**
     * Get current supported profile ids
     *
     * @return an array of current supported profile ids
     */
    @SuppressWarnings("rawtypes")
    public int[] getCurSupportedServerProfiles() {
        if (DBG) {
            Log.d(TAG, "getCurSupportedServerProfiles");
        }
        if (null == mProfileIds) {
            final List<Integer> intList = new ArrayList<Integer>();
            final Class[] supportedProfileServices = Config.getSupportedProfiles();

            for (final Class clazz : supportedProfileServices) {
                if (BleProfileServerServiceBase.class.isAssignableFrom(clazz)) {
                    try {
                        // it must have a constructor without any arguments
                        final BleProfileServerServiceBase profileServerService =
                                (BleProfileServerServiceBase) clazz
                                        .newInstance();
                        final int[] profileIds = Objects.requireNonNull(profileServerService.getProfileIds());

                        for (final int profile : profileIds) {
                            if (!intList.contains(profile)) {
                                intList.add(profile);
                            }
                        }
                    } catch (final InstantiationException e) {
                        Log.e(TAG, "getCurSupportedServerProfiles:" + "InstantiationException - "
                                + e.toString());
                    } catch (final IllegalAccessException e) {
                        Log.e(TAG, "getCurSupportedServerProfiles: " + "IllegalAccessException - "
                                + e.toString());
                    } catch (NullPointerException e) {
                        Log.e(TAG, "getCurSupportedServerProfiles: " + "NullPointerException - "
                                + e.toString());
                    }
                }
            }

            mProfileIds = new int[intList.size()];
            for (int i = 0; i < intList.size(); i++) {
                mProfileIds[i] = intList.get(i);
            }
        }

        return mProfileIds;
    }

    int getProfileServerState(final int profile) {
        final Integer state = mProfileServerState.get(profile);
        if (DBG) {
            Log.d(TAG, "isServerProfileRegistered: profile=" + profile + " state=" + state);
        }
        if (null == state) {
            return BleProfileServiceManager.STATE_SERVER_IDLE;
        }
        return state;
    }

    static boolean setBackgroundMode(final Context ctxt, final boolean bEnabled) {
        boolean bSuccess = true;
        final SharedPreferences settings = ctxt.getSharedPreferences(SHARED_PREFERENCE, 0);

        bSuccess = settings.edit().putBoolean(SHARED_PREFERENCE_BG_MODE, bEnabled).commit();
        if (DBG) {
            Log.d(TAG, "setBackgroundMode: bEnabled=" + bEnabled + ",bSuccess=" + bEnabled);
        }
        return bSuccess;
    }

    static boolean isBackgroundModeEnabled(final Context ctxt) {
        boolean bEnabled = true;
        final SharedPreferences settings = ctxt.getSharedPreferences(SHARED_PREFERENCE, 0);

        bEnabled = settings.getBoolean(SHARED_PREFERENCE_BG_MODE, false);
        if (DBG) {
            Log.d(TAG, "isBackgroundModeEnabled: bEnabled=" + bEnabled);
        }

        return bEnabled;
    }

    private void broadcastToServices(final boolean bLaunch) {
        final int state = (bLaunch) ? BluetoothAdapter.STATE_ON : BluetoothAdapter.STATE_OFF;
        final Intent intent = new Intent();
        // only for BleReceiver
        intent.setClass(this, BleReceiver.class);
        intent.setAction(BleReceiver.ACTION_CHANGE_SERVICE_STATE);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, state);
        sendBroadcast(intent);
    }

    class ClientDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            Log.i(TAG, "binderDied: call shutdownServices");
            shutdownServices();
        }
    }

    private void linkOrUnlinkToBinderDeath(final boolean bLink) {
        final int id = Binder.getCallingPid();
        final IProfileServiceManagerCallback cb = mCallbacks.get(id);
        final IBinder binder = (null != cb) ? cb.asBinder() : null;

        Log.i(TAG, "linkOrUnlinkToBinderDeath: caller pid=" + id);
        if (null != binder) {
            if (bLink) {
                try {
                    binder.linkToDeath(mClientDeathRecipient, 0);
                } catch (final RemoteException e) {
                    Log.e(TAG, "linkOrUnlinkToBinderDeath:" + e);
                }
                Log.i(TAG, "linkOrUnlinkToBinderDeath: linkToDeath");
            } else {
                try {
                    binder.unlinkToDeath(mClientDeathRecipient, 0);
                } catch (final NoSuchElementException e) {
                    Log.e(TAG, "linkOrUnlinkToBinderDeath:" + e);
                }
                Log.i(TAG, "linkOrUnlinkToBinderDeath: unlinkToDeath");
            }
        } else {
            Log.e(TAG, "linkOrUnlinkToBinderDeath: binder is null");
        }
    }

    boolean launchServices(final boolean bShutdownWhenCrashed) {
        boolean isLaunch = false;

        if (BluetoothAdapter.STATE_ON != BluetoothAdapter.getDefaultAdapter().getState()) {
            if (DBG) {
                Log.d(TAG, "launchServices: BT is off!");
            }
            isLaunch = false;
        } else if (!isBackgroundModeEnabled(this)) {
            broadcastToServices(true);
            isLaunch = true;
            linkOrUnlinkToBinderDeath(bShutdownWhenCrashed);
        } else {
            if (DBG) {
                Log.d(TAG, "launchServices: it's in background mode!");
            }
            isLaunch = false;
        }

        return isLaunch;
    }

    boolean shutdownServices() {
        boolean isShutdown = false;

        if (BluetoothAdapter.STATE_ON != BluetoothAdapter.getDefaultAdapter().getState()) {
            if (DBG) {
                Log.d(TAG, "shutdownServices: BT is off!");
            }
            isShutdown = false;
        } else if (!isBackgroundModeEnabled(this)) {
            broadcastToServices(false);
            isShutdown = true;
            linkOrUnlinkToBinderDeath(false);
        } else {
            if (DBG) {
                Log.d(TAG, "shutdownServices: it's in background mode!");
            }
            isShutdown = false;
        }

        return isShutdown;
    }

    void registerCallback(final IProfileServiceManagerCallback callback) {
        final int id = Binder.getCallingPid();
        Log.i(TAG, "registerCallback: caller pid=" + id);
        mCallbacks.put(id, callback);
    }

    void unregisterCallback(final IProfileServiceManagerCallback callback) {
        final int id = Binder.getCallingPid();
        Log.i(TAG, "unregisterCallback: caller pid=" + Binder.getCallingPid());
        mCallbacks.remove(id);
    }

    private class BleProfileManagerServiceBinder extends IProfileServiceManager.Stub {
        @Override
        public int[] getCurSupportedServerProfiles() throws RemoteException {
            return BleProfileManagerService.this.getCurSupportedServerProfiles();
        }

        @Override
        public int getProfileServerState(final int profile) throws RemoteException {
            return BleProfileManagerService.this.getProfileServerState(profile);
        }

        @Override
        public boolean setBackgroundMode(final boolean bEnabled) throws RemoteException {
            return BleProfileManagerService.setBackgroundMode(BleProfileManagerService.this,
                    bEnabled);
        }

        @Override
        public boolean isBackgroundModeEnabled() throws RemoteException {
            return BleProfileManagerService.isBackgroundModeEnabled(BleProfileManagerService.this);
        }

        @Override
        public boolean launchServices() throws RemoteException {
            return BleProfileManagerService.this.launchServices(true);
        }

        @Override
        public boolean shutdownServices() throws RemoteException {
            return BleProfileManagerService.this.shutdownServices();
        }

        @Override
        public void registerCallback(final IProfileServiceManagerCallback callback)
                throws RemoteException {
            BleProfileManagerService.this.registerCallback(callback);
        }

        @Override
        public void unregisterCallback(final IProfileServiceManagerCallback callback)
                throws RemoteException {
            BleProfileManagerService.this.unregisterCallback(callback);
        }

    }
}
