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
 * MediaTek Inc. (C) 2015. All rights reserved.
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


package com.mediatek.bluetoothgatt.profile;

// Customized Start: Import ........................................................................
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.mediatek.bluetoothgatt.GattUuid;
import com.mediatek.bluetoothgatt.characteristic.BloodPressureFeature;
import com.mediatek.bluetoothgatt.characteristic.BloodPressureMeasurement;
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.FirmwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.HardwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.IntermediateCuffPressure;
import com.mediatek.bluetoothgatt.characteristic.ManufacturerNameString;
import com.mediatek.bluetoothgatt.characteristic.ModelNumberString;
import com.mediatek.bluetoothgatt.characteristic.PnpId;
import com.mediatek.bluetoothgatt.characteristic.RegCertDataList;
import com.mediatek.bluetoothgatt.characteristic.SerialNumberString;
import com.mediatek.bluetoothgatt.characteristic.SoftwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.SystemId;

import java.util.UUID;
//........................................................................ Customized End: Import //

/**
 * This class is used to implement {@link BlpCollector} callbacks.
 */
public class BlpCollectorCallback extends ClientBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "BlpCollectorCallback";

    @Override
    void dispatchCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_BLS)) {
             if (charUuid.equals(GattUuid.CHAR_BLOOD_PRESSURE_FEATURE)) {
                base = new BloodPressureFeature();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onBlsBloodPressureFeatureReadResponse(
                        (BloodPressureFeature) base, status);
                return;
            }
        }
        if (srvcUuid.equals(GattUuid.SRVC_DIS)) {
             if (charUuid.equals(GattUuid.CHAR_MANUFACTURER_NAME_STRING)) {
                base = new ManufacturerNameString();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisManufacturerNameStringReadResponse(
                        (ManufacturerNameString) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_MODEL_NUMBER_STRING)) {
                base = new ModelNumberString();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisModelNumberStringReadResponse(
                        (ModelNumberString) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_SERIAL_NUMBER_STRING)) {
                base = new SerialNumberString();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisSerialNumberStringReadResponse(
                        (SerialNumberString) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_HARDWARE_REVISION_STRING)) {
                base = new HardwareRevisionString();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisHardwareRevisionStringReadResponse(
                        (HardwareRevisionString) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_FIRMWARE_REVISION_STRING)) {
                base = new FirmwareRevisionString();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisFirmwareRevisionStringReadResponse(
                        (FirmwareRevisionString) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_SOFTWARE_REVISION_STRING)) {
                base = new SoftwareRevisionString();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisSoftwareRevisionStringReadResponse(
                        (SoftwareRevisionString) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_SYSTEM_ID)) {
                base = new SystemId();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisSystemIdReadResponse(
                        (SystemId) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_REG_CERT_DATA_LIST)) {
                base = new RegCertDataList();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisRegCertDataListReadResponse(
                        (RegCertDataList) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_PNP_ID)) {
                base = new PnpId();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onDisPnpIdReadResponse(
                        (PnpId) base, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;



        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchCharacteristicChanged(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_BLS)) {
             if (charUuid.equals(GattUuid.CHAR_BLOOD_PRESSURE_MEASUREMENT)) {
                base = new BloodPressureMeasurement(characteristic.getValue(), characteristic);
                this.onBlsBloodPressureMeasurementNotify(
                        (BloodPressureMeasurement) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_INTERMEDIATE_CUFF_PRESSURE)) {
                base = new IntermediateCuffPressure(characteristic.getValue(), characteristic);
                this.onBlsIntermediateCuffPressureNotify(
                        (IntermediateCuffPressure) base);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Characteristic UUID=" + charUuid);
        }
    }

    @Override
    void dispatchDescriptorRead(BluetoothGatt gatt,
            BluetoothGattDescriptor descriptor, int status) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();

        if (srvcUuid.equals(GattUuid.SRVC_BLS)) {
             if (charUuid.equals(GattUuid.CHAR_BLOOD_PRESSURE_MEASUREMENT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onBlsBloodPressureMeasurementCccdReadResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_INTERMEDIATE_CUFF_PRESSURE) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onBlsIntermediateCuffPressureCccdReadResponse(descriptor, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    @Override
    void dispatchDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
            int status) {
        final UUID charUuid = descriptor.getCharacteristic().getUuid();
        final UUID srvcUuid = descriptor.getCharacteristic().getService().getUuid();
        final UUID descrUuid = descriptor.getUuid();

        if (srvcUuid.equals(GattUuid.SRVC_BLS)) {
             if (charUuid.equals(GattUuid.CHAR_BLOOD_PRESSURE_MEASUREMENT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onBlsBloodPressureMeasurementCccdWriteResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_INTERMEDIATE_CUFF_PRESSURE) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onBlsIntermediateCuffPressureCccdWriteResponse(descriptor, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    /**
     * Callback reporting the result of a
     * Bls:BloodPressureFeature characteristic read operation.
     *
     * @param bloodPressureFeature Bls:BloodPressureFeature characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onBlsBloodPressureFeatureReadResponse(
            BloodPressureFeature bloodPressureFeature, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:ManufacturerNameString characteristic read operation.
     *
     * @param manufacturerNameString Dis:ManufacturerNameString characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisManufacturerNameStringReadResponse(
            ManufacturerNameString manufacturerNameString, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:ModelNumberString characteristic read operation.
     *
     * @param modelNumberString Dis:ModelNumberString characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisModelNumberStringReadResponse(
            ModelNumberString modelNumberString, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:SerialNumberString characteristic read operation.
     *
     * @param serialNumberString Dis:SerialNumberString characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisSerialNumberStringReadResponse(
            SerialNumberString serialNumberString, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:HardwareRevisionString characteristic read operation.
     *
     * @param hardwareRevisionString Dis:HardwareRevisionString characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisHardwareRevisionStringReadResponse(
            HardwareRevisionString hardwareRevisionString, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:FirmwareRevisionString characteristic read operation.
     *
     * @param firmwareRevisionString Dis:FirmwareRevisionString characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisFirmwareRevisionStringReadResponse(
            FirmwareRevisionString firmwareRevisionString, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:SoftwareRevisionString characteristic read operation.
     *
     * @param softwareRevisionString Dis:SoftwareRevisionString characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisSoftwareRevisionStringReadResponse(
            SoftwareRevisionString softwareRevisionString, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:SystemId characteristic read operation.
     *
     * @param systemId Dis:SystemId characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisSystemIdReadResponse(
            SystemId systemId, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:RegCertDataList characteristic read operation.
     *
     * @param regCertDataList Dis:RegCertDataList characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisRegCertDataListReadResponse(
            RegCertDataList regCertDataList, int status) {
    }

    /**
     * Callback reporting the result of a
     * Dis:PnpId characteristic read operation.
     *
     * @param pnpId Dis:PnpId characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onDisPnpIdReadResponse(
            PnpId pnpId, int status) {
    }




    /**
     * Callback reporting the result of a
     * Bls:BloodPressureMeasurement:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onBlsBloodPressureMeasurementCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback reporting the result of a
     * Bls:IntermediateCuffPressure:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onBlsIntermediateCuffPressureCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback indicating the result of a
     * Bls:BloodPressureMeasurement:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onBlsBloodPressureMeasurementCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback indicating the result of a
     * Bls:IntermediateCuffPressure:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onBlsIntermediateCuffPressureCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback triggered as a result of a remote
     * Bls:BloodPressureMeasurement characteristic notification.
     *
     * @param bloodPressureMeasurement Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onBlsBloodPressureMeasurementNotify(
            BloodPressureMeasurement bloodPressureMeasurement) {
    }

    /**
     * Callback triggered as a result of a remote
     * Bls:IntermediateCuffPressure characteristic notification.
     *
     * @param intermediateCuffPressure Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onBlsIntermediateCuffPressureNotify(
            IntermediateCuffPressure intermediateCuffPressure) {
    }

}

