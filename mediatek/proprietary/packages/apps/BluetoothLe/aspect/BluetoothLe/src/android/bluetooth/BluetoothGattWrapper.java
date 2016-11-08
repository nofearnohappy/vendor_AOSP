package android.bluetooth;

import java.util.List;
import java.util.UUID;

public class BluetoothGattWrapper {

    public boolean connect(Boolean autoConnect, BluetoothGattCallback callback) {
        return false;
    }

    public void disconnect() {

    }

    public BluetoothDevice getDevice() {
        return null;
    }

    public boolean discoverServices() {
        return false;
    }

    public List<BluetoothGattService> getServices() {
        return null;
    }

    public BluetoothGattService getService(UUID uuid) {
        return null;
    }

    ///Request APIs
    public boolean readRemoteRssi() {
        return true;
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        return true;
    }
}
