package com.mediatek.bluetooth.fmOverBt;

// import android.bluetooth.BluetoothDevice;
import android.os.IBinder;

interface IFmOverBtService {
    int getState();
    boolean setAudioPathToAudioTrack(IBinder cb);
}
