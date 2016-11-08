package com.mediatek.bluetoothle.pxp;

import android.bluetooth.BluetoothDevice;
import com.mediatek.bluetoothle.pxp.IProximityProfileServiceCallback;

interface IProximityProfileService{
    int getPathLoss(in BluetoothDevice device);
    boolean isAlertOn(in BluetoothDevice device);
    boolean stopRemoteAlert(in BluetoothDevice device);
    boolean registerStatusChangeCallback(in BluetoothDevice device,
            in IProximityProfileServiceCallback callback);
    boolean unregisterStatusChangeCallback(in BluetoothDevice device,
            in IProximityProfileServiceCallback callback);
    boolean setPxpParameters(in BluetoothDevice device, in int alertEnabler,
            in int rangeAlertEnabler, in int rangeType, in int rangeValue, in int disconnectEnabler);
    boolean getPxpParameters(in BluetoothDevice device, out int[] alertEnabler,
            out int[] rangeAlertEnabler, out int[] rangeType, out int[] rangeValue,
            out int[] disconnectEnabler);
}
