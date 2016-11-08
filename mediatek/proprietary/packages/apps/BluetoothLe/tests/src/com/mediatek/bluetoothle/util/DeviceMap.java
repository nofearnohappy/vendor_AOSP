package com.mediatek.bluetoothle.util;

import android.bluetooth.BluetoothDevice;

import com.mediatek.bluetooth.IBleDeviceManagerCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceMap {

    private static final DeviceMap SINSTANCE = new DeviceMap();

    //<Client : Callback> Map for each device
    private Map<BluetoothDevice, Map<Integer, IBleDeviceManagerCallback.Stub>> mDeviceClientMap =
            new HashMap<BluetoothDevice, Map<Integer, IBleDeviceManagerCallback.Stub>>();

    public static DeviceMap getInstance() {
        return SINSTANCE;
    }

    public void addDeviceCallback(final BluetoothDevice device,
            final Map<Integer, IBleDeviceManagerCallback.Stub> clientCbMap) {
        mDeviceClientMap.put(device, clientCbMap);
    }

    public List<IBleDeviceManagerCallback.Stub> getDeviceCallbacks(final BluetoothDevice device) {
        Map<Integer, IBleDeviceManagerCallback.Stub> cbMap = mDeviceClientMap.get(device);

        if (cbMap != null) {
            return new ArrayList<IBleDeviceManagerCallback.Stub>(cbMap.values());
        }

        return null;
    }

    public IBleDeviceManagerCallback.Stub getDeviceClientCallback(
            final BluetoothDevice device, final int clientID) {
        Map<Integer, IBleDeviceManagerCallback.Stub> cbMap = mDeviceClientMap.get(device);

        if (cbMap != null) {
            return cbMap.get(clientID);
        }

        return null;
    }

    public List<Integer> getDeviceClients(final BluetoothDevice device) {
        Map<Integer, IBleDeviceManagerCallback.Stub> cbMap = mDeviceClientMap.get(device);

        if (cbMap != null) {
            return new ArrayList<Integer>(cbMap.keySet());
        }

        return null;
    }

    public void clear() {
        mDeviceClientMap.clear();
    }
}
