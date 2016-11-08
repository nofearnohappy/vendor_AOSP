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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetooth.parcel;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for AOSP android.bluetooth.BluetoothGattCharacteristic
 *
 * <p>
 * The wrapper class implements Parcelable interface, and can be used for inter-process
 * communication
 *
 * @hide
 */

public class ParcelBluetoothGattCharacteristic implements Parcelable {

    private static final boolean VDGB = false;
    private static final String TAG = "ParcelBluetoothGattCharacteristic";

    /**
     * The UUID of this characteristic.
     *
     */
    protected ParcelUuid mUuid;

    /**
     * Instance ID for this characteristic.
     *
     */
    protected int mInstance;

    /**
     * Characteristic properties.
     *
     */
    protected int mProperties;

    /**
     * Characteristic permissions.
     *
     */
    protected int mPermissions;

    /**
     * Key size (default = 16).
     *
     */
    protected int mKeySize = 16;

    /**
     * Write type for this characteristic. See WRITE_TYPE_* constants.
     *
     */
    protected int mWriteType;

    /**
     * Back-reference to the service this characteristic belongs to.
     *
     */
    protected ParcelBluetoothGattService mService;

    /**
     * The cached value of this characteristic.
     *
     */
    protected byte[] mValue;

    /**
     * List of descriptors included in this characteristic.
     */
    protected List<ParcelBluetoothGattDescriptor> mDescriptors;

    /**
     * Create a new BluetoothGattCharacteristic
     *
     */
    /* package */public ParcelBluetoothGattCharacteristic(ParcelBluetoothGattService service,
            ParcelUuid uuid, int instanceId, int properties, int permissions) {
        mUuid = uuid;
        mInstance = instanceId;
        mProperties = properties;
        mPermissions = permissions;
        mService = service;
        mValue = null;
        mDescriptors = new ArrayList<ParcelBluetoothGattDescriptor>();
    }

    /**
     * Returns the deisred key size.
     *
     */
    /* package */int getKeySize() {
        return mKeySize;
    }

    /**
      * @internal
      */
    public ParcelBluetoothGattService getService() {
        return mService;
    }

    /**
      * @internal
      */
    public ParcelUuid getUuid() {
        return mUuid;
    }

    /**
      * @internal
      */
    public int getInstanceId() {
        return mInstance;
    }

    public int getProperties() {
        return mProperties;
    }

    public int getPermissions() {
        return mPermissions;
    }

    /**
      * @internal
      */
    public int getWriteType() {
        return mWriteType;
    }

    public List<ParcelBluetoothGattDescriptor> getDescriptors() {
        return mDescriptors;
    }

    public ParcelBluetoothGattDescriptor getDescriptor(ParcelUuid uuid) {
        for (ParcelBluetoothGattDescriptor descriptor : mDescriptors) {
            if (descriptor.getUuid().equals(uuid)) {
                return descriptor;
            }
        }
        return null;
    }

    public boolean setValue(byte[] value) {
        mValue = value;
        return true;
    }

    /**
      * @internal
      */
    public byte[] getValue() {
        return mValue;
    }

    public boolean addDescriptor(ParcelBluetoothGattDescriptor descriptor) {
        mDescriptors.add(descriptor);
        return true;
    }

    @Override
    public int describeContents() {
        // TODO: Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        if (VDGB) {
            Log.d(TAG, "<<Start>> writeToParcel, this = " + this.hashCode());
            Log.d(TAG, this.toString());
        }

        dest.writeParcelable(mUuid, flags);
        dest.writeInt(mInstance);
        dest.writeInt(mProperties);
        dest.writeInt(mPermissions);
        dest.writeInt(mKeySize);
        dest.writeInt(mWriteType);

        // Write parent
        dest.writeParcelable(mService, flags);

        // Write mValue
        if (mValue != null && mValue.length > 0) {
            dest.writeInt(mValue.length);
            dest.writeByteArray(mValue);
        } else {
            dest.writeInt(0);
        }

        // Write children elements
        dest.writeList(mDescriptors);

        if (VDGB) {
            Log.d(TAG, "<<End>> writeToParcel, this = " + this.hashCode());
        }
    }

    /**
      * @internal
      */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("Parcel Characteristic\n");
        sb.append("  ");
        sb.append(">UUID = ");
        sb.append(this.mUuid + "\n");
        sb.append("  ");
        sb.append(">InstanceID = ");
        sb.append(this.mInstance + "\n");
        sb.append("  ");
        sb.append(">Properties = ");
        sb.append(this.mProperties + "\n");
        sb.append("  ");
        sb.append(">Permissions = ");
        sb.append(this.mPermissions + "\n");
        sb.append("  ");
        sb.append(">Service = \n");
        sb.append(this.mService);

        return sb.toString();
    }

    /**
     * Returns a BluetoothGattCharacteristic with all children elements
     *
     * @return an instance of BluetoothGattCharacteristic
     */
    public BluetoothGattCharacteristic unpack() {

        BluetoothGattCharacteristic instance =
                new BluetoothGattCharacteristic(this.mUuid.getUuid(), this.mProperties,
                        this.mPermissions);

        try {
            Field field = BluetoothGattCharacteristic.class.getDeclaredField("mInstance");
            field.setAccessible(true);
            field.set(instance, this.mInstance);
        } catch (Exception e) {
            Log.e(TAG, "" + e);
        }

        instance.setValue(this.mValue);
        instance.setWriteType(this.mWriteType);
        instance.setKeySize(this.mKeySize);

        // Unpack descriptors
        for (ParcelBluetoothGattDescriptor parcelDesc : this.mDescriptors) {
            BluetoothGattDescriptor gattDesc = parcelDesc.unpack();

            // Set parent characteristic
            try {
                Method method =
                        gattDesc.getClass().getDeclaredMethod("setCharacteristic",
                                BluetoothGattCharacteristic.class);
                method.setAccessible(true);
                method.invoke(gattDesc, instance);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
            instance.addDescriptor(gattDesc);
        }

        return instance;
    }

    /**
     * Create a ParcelBluetoothGattCharacteristic without the parent/children elements
     *
     * @return an instance of ParcelBluetoothGattCharacteristic
     *
     * @internal
     */
    public static ParcelBluetoothGattCharacteristic from(BluetoothGattCharacteristic gattChar,
            boolean isDeep) {

        ParcelBluetoothGattCharacteristic instance =
                new ParcelBluetoothGattCharacteristic(null, new ParcelUuid(gattChar.getUuid()),
                        gattChar.getInstanceId(), gattChar.getProperties(),
                        gattChar.getPermissions());

        instance.mValue = gattChar.getValue();
        instance.mWriteType = gattChar.getWriteType();
        // Add parent
        instance.mService = ParcelBluetoothGattService.from(gattChar.getService());

        try {
            Field field = instance.getClass().getDeclaredField("mKeySize");
            field.setAccessible(true);
            instance.mKeySize = (Integer) field.get(instance);
        } catch (Exception e) {
            Log.e(TAG, "" + e);
        }

        if (isDeep) {
            // Add descriptors
            List<BluetoothGattDescriptor> descList = gattChar.getDescriptors();
            for (BluetoothGattDescriptor gattDesc : descList) {
                instance.addDescriptor(ParcelBluetoothGattDescriptor.from(gattDesc));
            }
        }

        return instance;
    }

    public static ParcelBluetoothGattCharacteristic from(BluetoothGattCharacteristic gattChar) {
        return ParcelBluetoothGattCharacteristic.from(gattChar, false);
    }

    public static final Parcelable.Creator<ParcelBluetoothGattCharacteristic> CREATOR =
            new Parcelable.Creator<ParcelBluetoothGattCharacteristic>() {
                @Override
                public ParcelBluetoothGattCharacteristic createFromParcel(Parcel in) {

                    if (VDGB) {
                        Log.d(TAG, "createFromParcel, class = "
                                + ParcelBluetoothGattCharacteristic.class);
                    }

                    ClassLoader loader = ParcelBluetoothGattCharacteristic.class.getClassLoader();

                    if (VDGB) {
                        Log.d(TAG, "loader = " + loader);
                    }

                    ParcelBluetoothGattCharacteristic result = null;

                    ParcelUuid uuid = in.readParcelable(null);
                    int instance = in.readInt();
                    int properties = in.readInt();
                    int permissions = in.readInt();
                    int keySize = in.readInt();
                    int writeType = in.readInt();

                    result =
                            new ParcelBluetoothGattCharacteristic(null, uuid, instance, properties,
                                    permissions);

                    result.mKeySize = keySize;
                    result.mWriteType = writeType;

                    // Read parent
                    ParcelBluetoothGattService service = in.readParcelable(loader);
                    result.mService = service;

                    // Read characteristic value
                    int valueLeng = in.readInt();
                    byte[] value = null;
                    if (valueLeng != 0) {
                        value = new byte[valueLeng];
                        in.readByteArray(value);
                    }

                    // Read children elements
                    in.readList(result.mDescriptors, loader);

                    result.setValue(value);

                    return result;
                }

                @Override
                public ParcelBluetoothGattCharacteristic[] newArray(int size) {
                    return new ParcelBluetoothGattCharacteristic[size];
                }
            };
}
