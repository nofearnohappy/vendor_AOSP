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

import android.bluetooth.BluetoothGattDescriptor;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * Wrapper class for AOSP android.bluetooth.BluetoothGattDescriptor
 *
 * <p>
 * The wrapper class implements Parcelable interface, and can be used for inter-process
 * communication
 *
 * @hide
 */

public class ParcelBluetoothGattDescriptor implements Parcelable {

    private static final boolean VDGB = false;
    private static final String TAG = "ParcelBluetoothGattDescriptor";

    /**
     * The UUID of this descriptor.
     */
    protected ParcelUuid mUuid;

    /**
     * Instance ID for this descriptor.
     */
    protected int mInstance;

    /**
     * Permissions for this descriptor
     */
    protected int mPermissions;

    /**
     * Back-reference to the characteristic this descriptor belongs to.
     */
    protected ParcelBluetoothGattCharacteristic mCharacteristic;

    /**
     * The value for this descriptor.
     */
    protected byte[] mValue;

    /* package */ParcelBluetoothGattDescriptor(ParcelBluetoothGattCharacteristic characteristic,
            ParcelUuid uuid, int instance, int permissions) {
        mCharacteristic = characteristic;
        mUuid = uuid;
        mInstance = instance;
        mPermissions = permissions;
    }

    /**
      * @internal
      */
    public ParcelBluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
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

    public int getPermissions() {
        return mPermissions;
    }

    /**
      * @internal
      */
    public byte[] getValue() {
        return mValue;
    }

    public boolean setValue(byte[] value) {
        mValue = value;
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
        dest.writeInt(mPermissions);

        // Write Parent Characteristic
        dest.writeParcelable(mCharacteristic, flags);

        // Write mValue
        if (mValue != null && mValue.length > 0) {
            dest.writeInt(mValue.length);
            dest.writeByteArray(mValue);
        } else {
            dest.writeInt(0);
        }

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

        sb.append("Parcel Descriptor\n");
        sb.append("  ");
        sb.append(">UUID = ");
        sb.append(this.mUuid + "\n");
        sb.append("  ");
        sb.append(">InstanceID = ");
        sb.append(this.mInstance + "\n");
        sb.append("  ");
        sb.append(">Permissions = ");
        sb.append(this.mPermissions + "\n");
        sb.append("  ");
        sb.append(">Characteristic = \n");
        sb.append(this.mCharacteristic);

        return sb.toString();
    }

    /**
     * Returns a BluetoothGattDescriptor with all children elements
     *
     * @return an instance of BluetoothGattDescriptor
     */
    public BluetoothGattDescriptor unpack() {

        BluetoothGattDescriptor instance =
                new BluetoothGattDescriptor(this.mUuid.getUuid(), this.mPermissions);

        // Set instance id (only for 4.4)
        try {
            Field field = BluetoothGattDescriptor.class.getDeclaredField("mInstance");
            if (field != null) {
                field.setAccessible(true);
                field.set(instance, this.mInstance);
            }
        } catch (Exception e) {
            Log.e(TAG, "" + e);
        }

        instance.setValue(this.mValue);

        return instance;
    }

    /**
     * Create a ParcelBluetoothGattDescriptor without the parent/children elements
     *
     * @return an instance of ParcelBluetoothGattDescriptor
     *
     * @internal
     */
    public static ParcelBluetoothGattDescriptor from(BluetoothGattDescriptor gattDesc) {

        ParcelBluetoothGattDescriptor instance =
                new ParcelBluetoothGattDescriptor(null, new ParcelUuid(gattDesc.getUuid()), 0,
                        gattDesc.getPermissions());

        instance.mValue = gattDesc.getValue();
        // Add parent
        instance.mCharacteristic =
                ParcelBluetoothGattCharacteristic.from(gattDesc.getCharacteristic());

        return instance;
    }

    public static final Parcelable.Creator<ParcelBluetoothGattDescriptor> CREATOR =
            new Parcelable.Creator<ParcelBluetoothGattDescriptor>() {
                @Override
                public ParcelBluetoothGattDescriptor createFromParcel(Parcel in) {

                    if (VDGB) {
                        Log.d(TAG, "createFromParcel, class = "
                                + ParcelBluetoothGattDescriptor.class);
                    }

                    ClassLoader loader = ParcelBluetoothGattDescriptor.class.getClassLoader();

                    if (VDGB) {
                        Log.d(TAG, "loader = " + loader);
                    }

                    ParcelBluetoothGattDescriptor result = null;

                    ParcelUuid uuid = (ParcelUuid) in.readParcelable(null);
                    int instance = in.readInt();
                    int permissions = in.readInt();

                    // Read Parent Characteristic
                    ParcelBluetoothGattCharacteristic characteristic = in.readParcelable(loader);

                    // Read descriptor value
                    int valueLeng = in.readInt();
                    byte[] value = null;
                    if (valueLeng != 0) {
                        value = new byte[valueLeng];
                        in.readByteArray(value);
                    }

                    result = new ParcelBluetoothGattDescriptor(null, uuid, instance, permissions);
                    result.mCharacteristic = characteristic;
                    result.mValue = value;

                    return result;
                }

                @Override
                public ParcelBluetoothGattDescriptor[] newArray(int size) {
                    return new ParcelBluetoothGattDescriptor[size];
                }
            };
}
