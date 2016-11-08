package com.mediatek.bluetoothle.pasp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class PaspNotifyChangeFilter {
    private static final String TAG = "[Pasp]PaspServerService";
    public Set<BluetoothDevice> mPhoneAlertChangeFilter = new HashSet<BluetoothDevice>();
    private Set<BluetoothDevice> mRingerSettingChangeFilter = new HashSet<BluetoothDevice>();

    public void addChangeFilter(BluetoothDevice device, BluetoothGattDescriptor descriptor, byte[] value) {
        // String address=device.getAddress();
        if (device == null || descriptor == null) {
            Log.i(TAG, "the device or descriptor is null");
            return;
        }

        UUID uuid = descriptor.getCharacteristic().getUuid();
        if (uuid.equals(PaspAttributes.ALERT_STATE_CHAR_UUID)) {
            Log.i(TAG, "add request alert change notify");
            mPhoneAlertChangeFilter.add(device);
        } else if (uuid.equals(PaspAttributes.RINGER_SETTING_CHAR_UUID)) {
            Log.i(TAG, "add request ringer  change notify");
            mRingerSettingChangeFilter.add(device);
        } else {
            Log.i(TAG, "the descriptor to be set is invalid");
        }

    }

    public PaspNotifyChangeFilter() {
        Log.i(TAG, "get PaspNotifyChangeFilter instance");
    }

    public void removeAllChangeFilter(BluetoothDevice device) {
        if (device == null) {
            Log.i(TAG, "the device is null");
            return;
        }
        removeAlertChangeFilter(device);
        removeRingerChangeFilter(device);
    }

    public void removeAlertChangeFilter(BluetoothDevice device) {
        if (device == null) {
            Log.i(TAG, "the device is null");
            return;
        }
        // traversal the device in changeFilter,if existing,remove it
        Iterator<BluetoothDevice> phoneAlertIterator = mPhoneAlertChangeFilter.iterator();
        BluetoothDevice tmpDevice = null;
        while (phoneAlertIterator.hasNext()) {
            tmpDevice = phoneAlertIterator.next();
            if (tmpDevice.equals(device)) {
                mPhoneAlertChangeFilter.remove(device);
                break;
            }
        }

    }

    public void removeRingerChangeFilter(BluetoothDevice device) {
        if (device == null) {
            Log.i(TAG, "the device is null");
            return;
        }
        // traversal the device in changeFilter,if existing,remove it
        Iterator<BluetoothDevice> ringerIterator = mRingerSettingChangeFilter.iterator();
        BluetoothDevice tmpDevice = null;
        while (ringerIterator.hasNext()) {
            tmpDevice = ringerIterator.next();
            if (tmpDevice.equals(device)) {
                mRingerSettingChangeFilter.remove(device);
                break;
            }
        }

    }

    public Set<BluetoothDevice> getChangeFilter(UUID flag) {
        if (flag.equals(PaspAttributes.ALERT_STATE_CHAR_UUID)) {
            return mPhoneAlertChangeFilter;
        } else if (flag.equals(PaspAttributes.RINGER_SETTING_CHAR_UUID)) {
            return mRingerSettingChangeFilter;
        } else {
            Log.i(TAG, "flag is invalid and return null");
            return null;
        }
    }

}
