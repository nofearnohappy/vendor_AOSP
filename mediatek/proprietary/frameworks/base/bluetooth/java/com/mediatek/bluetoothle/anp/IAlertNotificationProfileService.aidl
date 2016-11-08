package com.mediatek.bluetoothle.anp;

interface IAlertNotificationProfileService{
    int[] getDeviceSettings(String address, in int[] categoryArray);
    int[] getRemoteSettings(String address, in int[] categoryArray);
    boolean updateDeviceSettings(String address, in int[] categoryArray, in int[] valueArray);
}
