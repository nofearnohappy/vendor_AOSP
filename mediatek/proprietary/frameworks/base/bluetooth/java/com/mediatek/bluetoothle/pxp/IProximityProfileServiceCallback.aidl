package com.mediatek.bluetoothle.pxp;

import android.bluetooth.BluetoothDevice;

interface IProximityProfileServiceCallback{
    void onDistanceValueChange(String address, int value);
    void onAlertStatusChange(String address, boolean isAlert);
}