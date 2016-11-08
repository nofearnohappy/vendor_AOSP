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

package com.mediatek.bluetoothgatt.service;

import android.bluetooth.BluetoothGattService;

import com.mediatek.bluetoothgatt.characteristic.CharacteristicBase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public APIs for GATT service configuration.
 */
public class ServiceCfg {
    public static final int TYPE_UNDEFINED = 0;
    public static final int TYPE_SERVICE = 1;
    public static final int TYPE_CHARACTERISTIC = 2;
    public static final int TYPE_DESCRIPTOR = 3;

    final UUID mUuid;
    boolean mSupport = true;
    int mServiceType = BluetoothGattService.SERVICE_TYPE_PRIMARY;
    int mInstanceId = 0;
    private List<Entry> mEntries = null;

    /**
     * GATT Include service/Characteristic/Descriptor configuration entry.
     */
    public class Entry {
        final int mType;
        final UUID mUuid;
        boolean mSupport = false;
        int mServiceType = BluetoothGattService.SERVICE_TYPE_SECONDARY;
        int mInstanceId = 0;
        int mProperties = 0;
        int mPermissions = 0;
        byte[] mValue = null;

        /**
         * Create a include service entry.
         *
         * @param support True, if GATT service support this entry.
         * @param uuid The UUID for this include service
         * @param serviceType Service type (Primary/Secondary)
         * @param instanceId Instance ID for this include service.
         */
        public Entry(boolean support, UUID uuid, int serviceType, int instanceId) {
            mType = TYPE_SERVICE;
            mSupport = support;
            mUuid = uuid;
            mServiceType = serviceType;
            mInstanceId = instanceId;
        }

        /**
         * Create a Characteristic/Descriptor entry.
         *
         * @param type TYPE_CHARACTERISTIC or TYPE_DESCRIPTOR
         * @param support True, if GATT service support this entry
         * @param uuid The UUID for this entry
         * @param properties Properties for the entry
         * @param permissions Permissions for this entry
         */
        public Entry(int type, boolean support, UUID uuid, int properties, int permissions) {
            mType = type;
            mSupport = support;
            mUuid = uuid;
            mProperties = properties;
            mPermissions = permissions;
        }

        /**
         * Create a Characteristic/Descriptor entry and init value.
         *
         * @param type TYPE_CHARACTERISTIC or TYPE_DESCRIPTOR
         * @param support True, if GATT service support this entry
         * @param uuid The UUID for this entry
         * @param properties Properties for the entry
         * @param permissions Permissions for this entry
         * @param value Initial value
         */
        public Entry(int type, boolean support, UUID uuid, int properties, int permissions,
                     byte[] value) {
            mType = type;
            mSupport = support;
            mUuid = uuid;
            mProperties = properties;
            mPermissions = permissions;
            mValue = value;
        }

        /**
         * Set support for this entry.
         *
         * @param support True, if service support this entry.
         * @return Entry
         */
        public Entry setSupport(boolean support) {
            mSupport = support;
            return this;
        }

        /**
         * Set service type (Primary/Secondary) for this include service entry.
         *
         * @param serviceType Service type (Primary/Secondary)
         * @return Entry
         */
        public Entry setServiceType(int serviceType) {
            if (mType == TYPE_SERVICE) {
                mServiceType = serviceType;
            }
            return this;
        }

        /**
         * Set instance id for this include service entry.
         *
         * @param instanceId Service instance id
         * @return Entry
         */
        public Entry setInstanceId(int instanceId) {
            if (mType == TYPE_SERVICE) {
                mInstanceId = instanceId;
            }
            return this;
        }

        /**
         * Set properties for this characteristic entry.
         *
         * @param properties Characteristic properties
         * @return Entry
         */
        public Entry setProperties(int properties) {
            if (mType == TYPE_CHARACTERISTIC) {
                mProperties = properties;
            }
            return this;
        }

        /**
         * Set permissions for this Characteristic/Descriptor entry.
         *
         * @param permissions Characteristic/Descriptor permissions.
         * @return Entry
         */
        public Entry setPermissions(int permissions) {
            mPermissions = permissions;
            return this;
        }

        /**
         * Set initial value for this Characteristic/Descriptor entry.
         *
         * @param value Initial value
         * @return Entry
         */
        public Entry setInitValue(byte[] value) {
            if (mType == TYPE_CHARACTERISTIC || mType == TYPE_DESCRIPTOR) {
                mValue = value;
            }
            return this;
        }

        /**
         * Set initial value for this Characteristic/Descriptor entry.
         *
         * @param value Initial value
         * @return Entry
         */
        public Entry setInitValue(CharacteristicBase value) {
            if (mType == TYPE_CHARACTERISTIC || mType == TYPE_DESCRIPTOR) {
                mValue = value.getValue();
            }
            return this;
        }

        /**
         * Returns the service type (Primary/Secondary) of this include service entry.
         *
         * @return Service type (Primary/Secondary)
         */
        public int getType() {
            return mType;
        }

        /**
         * Returns the UUID of this entry (Include service/Characteristic/Descriptor).
         *
         * @return UUID of this entry (Include service/Characteristic/Descriptor).
         */
        public UUID getUuid() {
            return mUuid;
        }

        /**
         * Returns the properties of this characteristic entry.
         *
         * @return Properties of this characteristic entry
         */
        public int getProperties() {
            return mProperties;
        }

        /**
         * Returns the supported flag of this entry (Include service/Characteristic/Descriptor).
         *
         * @return Supported flag of this entry (Include service/Characteristic/Descriptor)
         */
        public boolean getSupport() {
            return mSupport;
        }
    }

    /**
     * Create service configuration object.
     *
     * @param uuid The UUID for this service
     * @param instanceId Instance ID for this service
     * @param serviceType Service type (Primary/Secondary)
     */
    public ServiceCfg(UUID uuid, int instanceId, int serviceType) {
        mUuid = uuid;
        mInstanceId = instanceId;
        mServiceType = serviceType;
        mEntries = new ArrayList<>();
    }

    void clear() {
        mEntries.clear();
    }

    /**
     * Set supported flag for this service.
     *
     * @param support True, if this GATT service is supported
     * @return ServiceCfg
     */
    public ServiceCfg setSupport(boolean support) {
        mSupport = support;
        return this;
    }

    /**
     * Set service type (Primary/Secondary) for this GATT service.
     *
     * @param serviceType Service type (Primary/Secondary)
     * @return ServiceCfg
     */
    public ServiceCfg setServiceType(int serviceType) {
        mServiceType = serviceType;
        return this;
    }

    /**
     * Set instance id for this service entry.
     *
     * @param instanceId Service instance id
     * @return ServiceCfg
     */
    public ServiceCfg setInstanceId(int instanceId) {
        mInstanceId = instanceId;
        return this;
    }

    void addIncludeService(boolean support, UUID uuid, int serviceType, int instanceId) {
        mEntries.add(new Entry(support, uuid, serviceType, instanceId));
    }

    void addCharacteristic(boolean support, UUID uuid, int properties, int permissions) {
        mEntries.add(new Entry(TYPE_CHARACTERISTIC, support, uuid, properties, permissions));
    }

    void addCharacteristic(boolean support, UUID uuid, int properties, int permissions,
            byte[] value) {
        mEntries.add(new Entry(TYPE_CHARACTERISTIC, support, uuid, properties, permissions, value));
    }

    void addDescriptor(boolean support, UUID uuid, int permissions) {
        mEntries.add(new Entry(TYPE_DESCRIPTOR, support, uuid, 0, permissions));
    }

    void addDescriptor(boolean support, UUID uuid, int permissions, byte[] value) {
        mEntries.add(new Entry(TYPE_DESCRIPTOR, support, uuid, 0, permissions, value));
    }

    /**
     * Returns the include service entry.
     *
     * @param srvcUuid Include service UUID
     * @return Include service entry or null if no include service with the given UUID was found
     */
    public Entry cfgIncludeService(UUID srvcUuid) {
        for (Entry entry : mEntries) {
            if (entry.mUuid.equals(srvcUuid) && entry.mType == TYPE_SERVICE) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Returns the characteristic entry.
     *
     * @param charUuid Characteristic UUID
     * @return Characteristic entry or null if no characteristic with the given UUID was found
     */
    public Entry cfgCharacteristic(UUID charUuid) {
        for (Entry entry : mEntries) {
            if (entry.mUuid.equals(charUuid) && entry.mType == TYPE_CHARACTERISTIC) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Returns the descriptor entry.
     *
     * @param charUuid Characteristic UUID UUID
     * @param descrUuid Descriptor UUID
     * @return Descriptor entry or null if no descriptor with the given UUID was found
     */
    public Entry cfgDescriptor(UUID charUuid, UUID descrUuid) {
        boolean findCharacteristic = false;
        for (Entry entry : mEntries) {
            if (entry.mType == TYPE_CHARACTERISTIC) {
                if (entry.mUuid.equals(charUuid)) {
                    findCharacteristic =  true;
                } else {
                    findCharacteristic = false;
                }
            } else if (entry.mType == TYPE_DESCRIPTOR) {
                if (entry.mUuid.equals(descrUuid) && findCharacteristic) {
                    return entry;
                }
            } else {
                findCharacteristic = false;
            }
        }
        return null;
    }

    /**
     * Returns the UUID of this service.
     *
     * @return UUID of this service.
     */
    public UUID getUuid() {
        return mUuid;
    }

    /**
     * Returns a list of entry (Include service/Characteristic/Descriptor) include in the
     * service.
     *
     * @return A list of entry (Include service/Characteristic/Descriptor)
     */
    public List<Entry> getEntries() {
        return mEntries;
    }

}
