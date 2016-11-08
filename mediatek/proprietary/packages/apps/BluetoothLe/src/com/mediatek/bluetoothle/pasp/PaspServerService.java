package com.mediatek.bluetoothle.pasp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.mediatek.bluetoothle.IBleProfileServer;
import com.mediatek.bluetoothle.bleservice.BleSingleProfileServerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PaspServerService extends BleSingleProfileServerService {

    private static final String TAG = "[Pasp]PaspServerService";

    private Context mContext;
    private static int sOrigianalRingerValue = 0;
    private static PaspServerService sPtsInstance;
    private IBleProfileServer mBluetoothGattServer;

    private HashMap<Integer, List<Pair<Object, byte[]>>> mHashReliableData;

    private PhoneRingerController mPhoneRingerController;
    private PaspNotifyChangeFilter mNotifyChangeFilter;

    private RingerControllerMachine mRingerControllerMachine;

    private boolean mIsRingerChanged = false;
    private boolean mIsAlertChanged = false;

    private  BluetoothGattService mPaspService = null;
    private  BluetoothGattCharacteristic mRingerSettingChar = null;
    private  BluetoothGattCharacteristic mAlertStateChar = null;
    private  BluetoothGattCharacteristic mRingerControlPoint = null;

    private  BluetoothGattServerCallback mCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.v(TAG, "onConnectionStateChange- device:" + device + " status:" + status + " newState:" + newState);

            String address = device.getAddress();
            if (BluetoothGatt.GATT_SUCCESS == status) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.v(TAG, "onConnectionStateChange -state:connected");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    updateChangeFilter(device);
                } else {
                    Log.v(TAG, "onConnectionStateChange - ignore state:" + newState);
                }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.v(TAG, "onServiceAdded - status:" + status + " service:" + service);
            Log.v(TAG, "onServiceAdded - service  uuid=" + service.getUuid());

            if (BluetoothGatt.GATT_SUCCESS == status) {
                byte[] charData = { 0 };
                byte[] valDes = { 1, 0 };

                mPaspService = service;
                try {
                    mRingerSettingChar = getCharacteristic(PaspAttributes.RINGER_SETTING_CHAR_UUID);
                    mRingerSettingChar.setValue(charData);
                    mRingerSettingChar.getDescriptor(PaspAttributes.RINGER_CHANGE_DES_UUID).setValue(valDes);

                    mAlertStateChar = getCharacteristic(PaspAttributes.ALERT_STATE_CHAR_UUID);
                    mAlertStateChar.setValue(charData);
                    mAlertStateChar.getDescriptor(PaspAttributes.ALERT_CHANGE_DES_UUID).setValue(valDes);

                    mRingerControlPoint = getCharacteristic(PaspAttributes.RINGER_CONTROL_POINT_CHAR_UUID);
                    mRingerControlPoint.setValue(charData);
                } catch (NullPointerException e) {
                    Log.e(TAG, "onServiceAdded: ", e);
                }
                initializePhoneAlertStates();
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattCharacteristic characteristic) {

            byte[] data = characteristic.getValue();
            UUID srvUuid = characteristic.getUuid();
            Log.v(TAG, "onCharacteristicReadRequest - incoming request: " + device.getName());
            Log.v(TAG, "onCharacteristicReadRequest -        requestId: " + requestId);
            Log.v(TAG, "onCharacteristicReadRequest -           offset: " + offset);
            Log.v(TAG, "onCharacteristicReadRequest -             uuid: " + characteristic.getUuid().toString() + " or "
                    + srvUuid);
            Log.v(TAG, "onCharacteristicReadRequest -             data: " + data[0]);

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, Arrays.copyOfRange(
                    data, offset, data.length));
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattDescriptor descriptor) {

            byte[] data = descriptor.getValue();

            Log.v(TAG, "onDescriptorReadRequest - incoming request: " + device.getName());
            Log.v(TAG, "onDescriptorReadRequest -        requestId: " + requestId);
            Log.v(TAG, "onDescriptorReadRequest -           offset: " + offset);
            Log.v(TAG, "onDescriptorReadRequest -             uuid: " + descriptor.getUuid().toString());

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, Arrays.copyOfRange(
                    data, offset, data.length));
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset,
                byte[] value) {
            Log.v(TAG, "onCharacteristicWriteRequest - offset:" + offset + " " + "value.length:" + value.length + " "
                    + "preparedWrite:" + preparedWrite + " " + "responseNeeded:" + responseNeeded);

            if (characteristic.getUuid().equals(PaspAttributes.RINGER_CONTROL_POINT_CHAR_UUID)) {
                Log.i(TAG, "receive remote device ringer control command,and value=" + value[0] + "string value:"
                        + value.toString());

                byte[] newValue = null;
                byte[] oldValue = characteristic.getValue();
                if (oldValue != null) {
                    if (oldValue.length >= offset + value.length) {
                        newValue = new byte[offset + value.length];
                        System.arraycopy(oldValue, 0, newValue, 0, offset);
                        System.arraycopy(value, 0, newValue, offset, value.length);
                    } else {
                        newValue = new byte[offset + value.length];
                        System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
                        System.arraycopy(value, 0, newValue, offset, value.length);
                    }
                }
                if (preparedWrite) {
                    Log.v(TAG, "onCharacteristicWriteRequest - a prepare write (pending data)\n"
                            + "onCharacteristicWriteRequest - requestId=" + requestId + "\n");

                    List<Pair<Object, byte[]>> listPendingData = null;
                    listPendingData = mHashReliableData.get(requestId);
                    if (null == listPendingData) {
                        Log.v(TAG, "onCharacteristicWriteRequest - a new listPendingData");
                        listPendingData = new ArrayList<Pair<Object, byte[]>>();
                        mHashReliableData.put(requestId, listPendingData);
                    }
                    listPendingData.add(new Pair<Object, byte[]>(characteristic, newValue));
                } else {
                    Log.v(TAG, "onCharacteristicWriteRequest - a normal write\n");
                    characteristic.setValue(newValue);
                    updateRingerSettingChar(mRingerSettingChar, value);
                }
            }

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

            Log.v(TAG, "onCharacteristicWriteRequest - BluetoothGattServer.sendResponse()");
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
                boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.v(TAG, "onDescriptorWriteRequest - offset:" + offset + " " + "value.length:" + value.length + " "
                    + "preparedWrite:" + preparedWrite + " " + "responseNeeded:" + responseNeeded);
            byte[] newValue = null;
            byte[] oldValue = descriptor.getValue();

            if (oldValue != null) {
                if (oldValue.length >= offset + value.length) {
                    newValue = new byte[offset + value.length];
                    System.arraycopy(oldValue, 0, newValue, 0, offset);
                    System.arraycopy(value, 0, newValue, offset, value.length);
                } else {
                    newValue = new byte[offset + value.length];
                    System.arraycopy(oldValue, 0, newValue, 0, oldValue.length);
                    System.arraycopy(value, 0, newValue, offset, value.length);
                }
            }

            if (preparedWrite) {
                Log.v(TAG, "onDescriptorWriteRequest - a prepare write (pending data)\n"
                        + "onDescriptorWriteRequest - requestId=" + requestId + "\n");

                List<Pair<Object, byte[]>> listPendingData = null;
                listPendingData = mHashReliableData.get(requestId);
                if (null == listPendingData) {
                    Log.v(TAG, "onDescriptorWriteRequest - a new listPendingData");
                    listPendingData = new ArrayList<Pair<Object, byte[]>>();
                    mHashReliableData.put(requestId, listPendingData);
                }
                listPendingData.add(new Pair<Object, byte[]>(descriptor, newValue));
            } else {
                if (null == newValue) {
                    Log.w(TAG, "onDescriptorWriteRequest -newValue=null");
                    return;
                }
                Log.v(TAG, "onDescriptorWriteRequest - a normal write\n:value=" + value[0]);
                descriptor.setValue(newValue);
                if (newValue[0] == 1) {
                    Log.v(TAG, "onDescriptorWriteRequest - call addChangeFilter ");
                    mNotifyChangeFilter.addChangeFilter(device, descriptor, newValue);
                } else if (newValue[0] == 0) {
                    if (descriptor.getCharacteristic().getUuid().equals(PaspAttributes.ALERT_STATE_CHAR_UUID)) {
                        Log.v(TAG, "onDescriptorWriteRequest - call removeAlertChangeFilter");
                        mNotifyChangeFilter.removeAlertChangeFilter(device);
                    }
                    if (descriptor.getCharacteristic().getUuid().equals(PaspAttributes.RINGER_SETTING_CHAR_UUID)) {
                        Log.v(TAG, "onDescriptorWriteRequest - call removeRingerChangeFilter");
                        mNotifyChangeFilter.removeRingerChangeFilter(device);
                    }

                }
            }

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            Log.v(TAG, "onDescriptorWriteRequest - BluetoothGattServer.sendResponse()");
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.v(TAG, "onExecuteWrite - device:" + device + " " + "requestId:" + requestId + " " + "execute:" + execute);

            if (execute) {
                Log.v(TAG, "onExecuteWrite - execute write");

                List<Pair<Object, byte[]>> listPendingData = null;
                listPendingData = mHashReliableData.get(requestId);

                for (Pair<Object, byte[]> pair : listPendingData) {
                    if (pair.first instanceof BluetoothGattCharacteristic) {
                        BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) pair.first;
                        characteristic.setValue(pair.second);
                        if (characteristic.getUuid().equals(PaspAttributes.ALERT_STATE_CHAR_UUID)) {
                            Log.v(TAG, "onExecuteWrite - updateRingerSettingChar:");
                            updateRingerSettingChar(characteristic, pair.second);
                        }
                        Log.v(TAG, "onExecuteWrite - characteristic:" + characteristic);
                    } else if (pair.first instanceof BluetoothGattDescriptor) {
                        BluetoothGattDescriptor descriptor = (BluetoothGattDescriptor) pair.first;
                        descriptor.setValue(pair.second);
                        mNotifyChangeFilter.addChangeFilter(device, descriptor, pair.second);
                        Log.v(TAG, "onExecuteWrite - descriptor:" + descriptor);
                    } else {
                        Log.v(TAG, "onExecuteWrite - unexpect object:" + pair.first);
                    }
                }
            } else {
                Log.v(TAG, "onExecuteWrite - execute cancel");
                mHashReliableData.remove(requestId);
            }
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }
    };

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        super.onCreate();

        mContext = this;
        mPhoneRingerController = new PhoneRingerController(mPhoneListener, mRingerChangeListener, mContext);
        mNotifyChangeFilter = new PaspNotifyChangeFilter();
        mRingerControllerMachine = new RingerControllerMachine(mContext);
        mHashReliableData = new HashMap<Integer, List<Pair<Object, byte[]>>>();

        sOrigianalRingerValue = mPhoneRingerController.getOriginalRingerState();
        mBluetoothGattServer = PaspServerService.this.getBleProfileServer();
        sPtsInstance = this;
    }

    // initialize the phone alert states
    void initializePhoneAlertStates() {
        final byte[] ringerState = mPhoneRingerController.getRingerSetting()[0];

        if (mRingerSettingChar != null) {
            mRingerSettingChar.setValue(ringerState);
        }

        if (mAlertStateChar != null) {
            final byte[] alertState = { 0 };
            mAlertStateChar.setValue(alertState);
        }

    }

    // reset the alertState
    private void resetAlertState() {
        final byte[] alertState = { 0 };
        mAlertStateChar.setValue(alertState);
        notifyCharacteristicChange(mAlertStateChar);
    }

    private BluetoothGattCharacteristic getCharacteristic(final UUID uuid) {

        if (mPaspService != null) {
            return mPaspService.getCharacteristic(uuid);
        } else {
            Log.i(TAG, "Service not exsisting");
            return null;
        }
    }

    @Override
    public void onDestroy() {
        // when service not existed,we resume the original ringer setting in audio
        Log.i(TAG, "onDestroy() resume ringer mode:" + sOrigianalRingerValue);
        mPhoneRingerController.resumeRingerMode(sOrigianalRingerValue);
        mPhoneRingerController.unRegisterReceiver(mContext);
        super.onDestroy();
    }

    @Override
    protected int getProfileId() {
        return IBleProfileServer.PASP;
    }

    @Override
    protected BluetoothGattServerCallback getDefaultBleProfileServerHandler() {
        return mCallback;
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        Log.v(TAG, "initBinder");
        return null;
    }

    public void notifyCharacteristicChange(final BluetoothGattCharacteristic characteristic) {
        Log.v(TAG, "enter notifyCharacteristicChanged ::mBluetoothGattServer=" + mBluetoothGattServer);

        if (null != mBluetoothGattServer) {
            if (mIsRingerChanged) {
                final Set<BluetoothDevice> ringerFilterList = mNotifyChangeFilter
                        .getChangeFilter(PaspAttributes.RINGER_SETTING_CHAR_UUID);
                Log.v(TAG, "notifyCharacteristicChanged - mIsRingerChanged is true,and changeFilter is:" + ringerFilterList);
                if (null != ringerFilterList) {
                    final Iterator<BluetoothDevice> itr = ringerFilterList.iterator();
                    while (itr.hasNext()) {
                        final BluetoothDevice device = itr.next();
                        if (device == null) {
                            Log.v(TAG, "ringer-->notifyCharacteristicChange::device=null");
                            continue;
                        }
                        Log.v(TAG, "ringer change:call  mBluetoothGattServer.notifyCharacteristicChanged()");
                        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                    }
                }
            }
            if (mIsAlertChanged) {

                final Set<BluetoothDevice> alertFilterList = mNotifyChangeFilter
                        .getChangeFilter(PaspAttributes.ALERT_STATE_CHAR_UUID);
                Log.v(TAG, "notifyCharacteristicChanged - mIsAlertChanged is true,and changeFilter is:" + alertFilterList);
                if (null != alertFilterList) {
                    final Iterator<BluetoothDevice> itr2 = alertFilterList.iterator();
                    while (itr2.hasNext()) {
                        final BluetoothDevice device = itr2.next();
                        if (device == null) {
                            Log.v(TAG, "alert-->notifyCharacteristicChange::device=null");
                            continue;
                        }
                        Log.v(TAG, "alert change:call  mBluetoothGattServer.notifyCharacteristicChanged()");
                        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
                    }
                }
            }
        }

        mIsAlertChanged = false;
        mIsRingerChanged = false;
    }

    private void updateChangeFilter(final BluetoothDevice device) {
        Log.i(TAG, "updateChangeFilter::mNotifyChangeFilter=" + mNotifyChangeFilter);
        mNotifyChangeFilter.removeAllChangeFilter(device);
    }

    public void updateRingerSettingChar(final BluetoothGattCharacteristic ringerSettingChar, final byte[] value) {
        if (null != mBluetoothGattServer) {
            Log.v(TAG, "update RingerSetting Characteristic - new value:" + value[0]);

            final int newControllerState = value[0];
            final int oldRingerState = mRingerControllerMachine.getRingerState();
            Log.v(TAG, "update RingerControllerMachine - new value:" + newControllerState);
            mRingerControllerMachine.setRingerState(newControllerState);

            if (oldRingerState != mRingerControllerMachine.getRingerState()) {
                saveRingerState(ringerSettingChar, newControllerState);
                final byte[] ringerValue = ringerSettingChar.getValue();
                final UUID ringerUuid = ringerSettingChar.getUuid();
                Log.v(TAG, "after update RingerSetting - new value:" + ringerValue[0] + "and uuid=" + ringerUuid);
                mIsRingerChanged = true;
                notifyCharacteristicChange(ringerSettingChar);
            }
        }
    }

    private void saveRingerState(final BluetoothGattCharacteristic ringerSetting, final int value) {
        Log.v(TAG, "enter saveRingerState()");
        final byte[] ringerValue = { 0 };
        switch (value) {
        case RingerControllerMachine.SILENT_MODE_VALUE:
            ringerValue[0] = RingerControllerMachine.RINGER_SILENT_STATE;
            ringerSetting.setValue(ringerValue);
            break;
        case RingerControllerMachine.MUTE_ONCE_VALUE:
            break;
        case RingerControllerMachine.CANCEL_SILENT_VALUE:
            ringerValue[0] = RingerControllerMachine.RINGER_NORMAL_STATE;
            ringerSetting.setValue(ringerValue);
            break;
        default:
            Log.v(TAG, "saveRingerState():invalid value");
        }
    }

    PhoneRingerController.PhoneComingListener mPhoneListener = new PhoneRingerController.PhoneComingListener() {
        @Override
        public void onPhoneStateChanged(final boolean isRinging) {
            if (mAlertStateChar == null) {
                Log.i(TAG, "Alert Characteristic is not initialized succcess");
                return;
            }

            if (isRinging) {
                mIsAlertChanged = true;
                updateAlertStateChar(mAlertStateChar, mPhoneRingerController.getRingerSetting()[1]);
            } else {
                mIsAlertChanged = true;
                resetAlertState();
            }
        }

    };
    PhoneRingerController.RingerChangeListener mRingerChangeListener = new PhoneRingerController.RingerChangeListener() {
        @Override
        public void onRingerModeChanged(byte[] ringerState, byte[] alertState) {
            Log.i(TAG, "enter onRingerModeChanged()");
            syncRingerState(ringerState);
            syncAlertState(alertState);
        }
    };
    public void updateAlertStateChar(final BluetoothGattCharacteristic alertStateChar, final byte[] strValue) {
        if (null != mBluetoothGattServer && alertStateChar != null) {
            Log.v(TAG, "update AlertState Characteristic - new value:" + strValue[0]);

            alertStateChar.setValue(strValue);
            final byte[] alerttest = alertStateChar.getValue();
            final UUID testUuid = alertStateChar.getUuid();
            Log.v(TAG, "after update AlertState - new value:" + alerttest[0] + "and uuid=" + testUuid);
            notifyCharacteristicChange(alertStateChar);
        }

    }

    private void syncRingerState(final byte[] value) {

        if (mRingerSettingChar != null) {
            Log.v(TAG, "syncRingerState with phone new value=" + value[0]);
            mRingerSettingChar.setValue(value);
            mIsRingerChanged = true;
            notifyCharacteristicChange(mRingerSettingChar);
        }

    }

    private void syncAlertState(final byte[] value) {

        if (mAlertStateChar != null) {
            Log.v(TAG, "syncAlertState with phone new value=" + value[0]);
            mAlertStateChar.setValue(value);
            mIsAlertChanged = true;
            notifyCharacteristicChange(mAlertStateChar);
        }

    }

    // just for PTS: set DisplayState to active,and other non-active
    public void updateDisplayState() {
        Log.v(TAG, "pts::enter updateDisplayState()");
        final byte[] alertState = { 4 };
        syncAlertState(alertState);
    }

    public static synchronized PaspServerService getPaspInstance() {
        return sPtsInstance;
    }

}
