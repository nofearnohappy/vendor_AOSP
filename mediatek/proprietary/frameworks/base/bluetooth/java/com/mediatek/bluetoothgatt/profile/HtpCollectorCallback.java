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
import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;
import com.mediatek.bluetoothgatt.characteristic.FirmwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.HardwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.IntermediateTemperature;
import com.mediatek.bluetoothgatt.characteristic.ManufacturerNameString;
import com.mediatek.bluetoothgatt.characteristic.MeasurementInterval;
import com.mediatek.bluetoothgatt.characteristic.ModelNumberString;
import com.mediatek.bluetoothgatt.characteristic.PnpId;
import com.mediatek.bluetoothgatt.characteristic.RegCertDataList;
import com.mediatek.bluetoothgatt.characteristic.SerialNumberString;
import com.mediatek.bluetoothgatt.characteristic.SoftwareRevisionString;
import com.mediatek.bluetoothgatt.characteristic.SystemId;
import com.mediatek.bluetoothgatt.characteristic.TemperatureMeasurement;
import com.mediatek.bluetoothgatt.characteristic.TemperatureType;

import java.util.UUID;
//........................................................................ Customized End: Import //

/**
 * This class is used to implement {@link HtpCollector} callbacks.
 */
public class HtpCollectorCallback extends ClientBaseCallback {
    private static final boolean DBG = true;
    private static final String TAG = "HtpCollectorCallback";

    @Override
    void dispatchCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
            int status) {
        final UUID charUuid = characteristic.getUuid();
        final UUID srvcUuid = characteristic.getService().getUuid();
        CharacteristicBase base;

        if (srvcUuid.equals(GattUuid.SRVC_HTS)) {
             if (charUuid.equals(GattUuid.CHAR_TEMPERATURE_TYPE)) {
                base = new TemperatureType();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onHtsTemperatureTypeReadResponse(
                        (TemperatureType) base, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_MEASUREMENT_INTERVAL)) {
                base = new MeasurementInterval();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onHtsMeasurementIntervalReadResponse(
                        (MeasurementInterval) base, status);
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

        if (srvcUuid.equals(GattUuid.SRVC_HTS)) {
             if (charUuid.equals(GattUuid.CHAR_MEASUREMENT_INTERVAL)) {
                base = new MeasurementInterval();
                base.setCharacteristic(characteristic);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    base.setValue(characteristic.getValue());
                }
                this.onHtsMeasurementIntervalWriteResponse(
                        (MeasurementInterval) base, status);
                return;
            }
        }

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

        if (srvcUuid.equals(GattUuid.SRVC_HTS)) {
             if (charUuid.equals(GattUuid.CHAR_TEMPERATURE_MEASUREMENT)) {
                base = new TemperatureMeasurement(characteristic.getValue(), characteristic);
                this.onHtsTemperatureMeasurementNotify(
                        (TemperatureMeasurement) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_INTERMEDIATE_TEMPERATURE)) {
                base = new IntermediateTemperature(characteristic.getValue(), characteristic);
                this.onHtsIntermediateTemperatureNotify(
                        (IntermediateTemperature) base);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_MEASUREMENT_INTERVAL)) {
                base = new MeasurementInterval(characteristic.getValue(), characteristic);
                this.onHtsMeasurementIntervalNotify(
                        (MeasurementInterval) base);
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

        if (srvcUuid.equals(GattUuid.SRVC_HTS)) {
             if (charUuid.equals(GattUuid.CHAR_TEMPERATURE_MEASUREMENT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onHtsTemperatureMeasurementCccdReadResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_INTERMEDIATE_TEMPERATURE) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onHtsIntermediateTemperatureCccdReadResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_MEASUREMENT_INTERVAL) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onHtsMeasurementIntervalCccdReadResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_MEASUREMENT_INTERVAL) &&
                    descrUuid.equals(GattUuid.DESCR_VALID_RANGE)) {
                this.onHtsMeasurementIntervalVrdReadResponse(descriptor, status);
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

        if (srvcUuid.equals(GattUuid.SRVC_HTS)) {
             if (charUuid.equals(GattUuid.CHAR_TEMPERATURE_MEASUREMENT) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onHtsTemperatureMeasurementCccdWriteResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_INTERMEDIATE_TEMPERATURE) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onHtsIntermediateTemperatureCccdWriteResponse(descriptor, status);
                return;
            } else if (charUuid.equals(GattUuid.CHAR_MEASUREMENT_INTERVAL) &&
                    descrUuid.equals(GattUuid.DESCR_CLIENT_CHARACTERISTIC_CONFIGURATION)) {
                this.onHtsMeasurementIntervalCccdWriteResponse(descriptor, status);
                return;
            }
        }

        if (DBG) {
            Log.d(TAG, "Unknown Descriptor UUID=" + descrUuid);
        }
    }

    /**
     * Callback reporting the result of a
     * Hts:TemperatureType characteristic read operation.
     *
     * @param temperatureType Hts:TemperatureType characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onHtsTemperatureTypeReadResponse(
            TemperatureType temperatureType, int status) {
    }

    /**
     * Callback reporting the result of a
     * Hts:MeasurementInterval characteristic read operation.
     *
     * @param measurementInterval Hts:MeasurementInterval characteristic
     *                       that was read from the associated remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully.
     */
    public void onHtsMeasurementIntervalReadResponse(
            MeasurementInterval measurementInterval, int status) {
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
     * Callback indicating the result of a
     * Hts:MeasurementInterval characteristic write operation.
     *
     * @param measurementInterval Hts:MeasurementInterval characteristic
     *                       that was written to the associated remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onHtsMeasurementIntervalWriteResponse(
            MeasurementInterval measurementInterval, int status) {
    }


    /**
     * Callback reporting the result of a
     * Hts:TemperatureMeasurement:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onHtsTemperatureMeasurementCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback reporting the result of a
     * Hts:IntermediateTemperature:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onHtsIntermediateTemperatureCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback reporting the result of a
     * Hts:MeasurementInterval:Cccd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onHtsMeasurementIntervalCccdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback reporting the result of a
     * Hts:MeasurementInterval:Vrd descriptor read operation.
     *
     * @param descriptor Descriptor that was read from the associated
     *                   remote device.
     * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
     *               was completed successfully
     */
    public void onHtsMeasurementIntervalVrdReadResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback indicating the result of a
     * Hts:TemperatureMeasurement:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onHtsTemperatureMeasurementCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback indicating the result of a
     * Hts:IntermediateTemperature:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onHtsIntermediateTemperatureCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }

    /**
     * Callback indicating the result of a
     * Hts:MeasurementInterval:Cccd descriptor write operation.
     *
     * @param descriptor Descriptor that was written to the associated
     *                   remote device.
     * @param status The result of the write operation
     *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
     */
    public void onHtsMeasurementIntervalCccdWriteResponse(
            BluetoothGattDescriptor descriptor, int status) {
    }


    /**
     * Callback triggered as a result of a remote
     * Hts:TemperatureMeasurement characteristic notification.
     *
     * @param temperatureMeasurement Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onHtsTemperatureMeasurementNotify(
            TemperatureMeasurement temperatureMeasurement) {
    }

    /**
     * Callback triggered as a result of a remote
     * Hts:IntermediateTemperature characteristic notification.
     *
     * @param intermediateTemperature Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onHtsIntermediateTemperatureNotify(
            IntermediateTemperature intermediateTemperature) {
    }

    /**
     * Callback triggered as a result of a remote
     * Hts:MeasurementInterval characteristic notification.
     *
     * @param measurementInterval Characteristic that has been updated as a result
     *                       of a remote notification event.
     */
    public void onHtsMeasurementIntervalNotify(
            MeasurementInterval measurementInterval) {
    }

}

