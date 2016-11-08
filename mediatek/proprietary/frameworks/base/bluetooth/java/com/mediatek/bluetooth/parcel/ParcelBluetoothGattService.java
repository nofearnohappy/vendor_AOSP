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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for AOSP android.bluetooth.BluetoothGattService
 *
 * <p>
 * The wrapper class implements Parcelable interface, and can be used for inter-process
 * communication
 *
 * @hide
 */

public class ParcelBluetoothGattService implements Parcelable {

    private static final boolean VDGB = false;
    private static final String TAG = "ParcelBluetoothGattService";

    /**
     * The remote device his service is associated with. This applies to client applications only.
     *
     */
    protected BluetoothDevice mDevice;

    /**
     * The UUID of this service.
     *
     */
    protected ParcelUuid mUuid;

    /**
     * Instance ID for this service.
     *
     */
    protected int mInstanceId;

    /**
     * Handle counter override (for conformance testing).
     *
     */
    protected int mHandles = 0;

    /**
     * Service type (Primary/Secondary).
     *
     */
    protected int mServiceType;

    /**
     * List of characteristics included in this service.
     */
    protected List<ParcelBluetoothGattCharacteristic> mCharacteristics;

    /**
     * List of included services for this service.
     */
    protected List<ParcelBluetoothGattService> mIncludedServices;

    /**
     * Create a new BluetoothGattService
     *
     */
    /* package */public ParcelBluetoothGattService(BluetoothDevice device, ParcelUuid uuid,
            int instanceId, int serviceType) {
        mDevice = device;
        mUuid = uuid;
        mInstanceId = instanceId;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<ParcelBluetoothGattCharacteristic>();
        mIncludedServices = new ArrayList<ParcelBluetoothGattService>();
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
        return mInstanceId;
    }

    public int getType() {
        return mServiceType;
    }

    /**
     * Returns a list of characteristics included in this service.
     *
     * @return Characteristics included in this service
     */
    public List<ParcelBluetoothGattCharacteristic> getCharacteristics() {
        return mCharacteristics;
    }

    public ParcelBluetoothGattCharacteristic getCharacteristic(ParcelUuid uuid) {
        for (ParcelBluetoothGattCharacteristic characteristic : mCharacteristics) {
            if (uuid.equals(characteristic.getUuid())) {
                return characteristic;
            }
        }
        return null;
    }

    public void addIncludedService(ParcelBluetoothGattService service) {
        this.mIncludedServices.add(service);
    }

    public void addCharacteristic(ParcelBluetoothGattCharacteristic gattChar) {
        this.mCharacteristics.add(gattChar);
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

        dest.writeParcelable(mDevice, flags);
        dest.writeParcelable(mUuid, flags);
        dest.writeInt(mInstanceId);
        dest.writeInt(mHandles);
        dest.writeInt(mServiceType);

        // Write children elements
        dest.writeList(mCharacteristics);
        dest.writeList(mIncludedServices);

        if (VDGB) {
            Log.d(TAG, "<<End>> writeToParcel, this = " + this.hashCode());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Parcel Service\n");
        sb.append("  ");
        sb.append(">UUID = ");
        sb.append(this.mUuid + "\n");
        sb.append("  ");
        sb.append(">InstanceID = ");
        sb.append(this.mInstanceId + "\n");
        sb.append("  ");
        sb.append(">ServiceType = ");
        sb.append(this.mServiceType + "\n");

        return sb.toString();
    }

    /**
     * Returns a BluetoothGattService with all children elements
     *
     * @return an instance of BluetoothGattService
     */
    public BluetoothGattService unpack() {
        BluetoothGattService instance =
                new BluetoothGattService(this.mUuid.getUuid(), this.mServiceType);

        instance.setInstanceId(this.mInstanceId);

        try {
            Field field = instance.getClass().getDeclaredField("mDevice");
            field.setAccessible(true);
            field.set(instance, this.mDevice);
        } catch (Exception e) {
            Log.e(TAG, "" + e);
        }

        // Unpack included services
        if (BluetoothGattService.SERVICE_TYPE_PRIMARY == this.mServiceType) {
            for (ParcelBluetoothGattService srv : this.mIncludedServices) {
                instance.addService(srv.unpack());
            }
        }

        // Unpack characteristic
        for (ParcelBluetoothGattCharacteristic parcelChar : this.mCharacteristics) {
            BluetoothGattCharacteristic gattChar = parcelChar.unpack();

            // Set parent service
            try {
                Method method =
                        gattChar.getClass().getDeclaredMethod("setService",
                                BluetoothGattService.class);
                method.setAccessible(true);
                method.invoke(gattChar, instance);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
            instance.addCharacteristic(gattChar);
        }

        return instance;
    }

    /**
     * Create a ParcelBluetoothGattService without the children elements
     *
     * @return an instance of ParcelBluetoothGattService
     *
     * @internal
     */
    public static ParcelBluetoothGattService from(BluetoothGattService gattService,
            boolean isDeep) {

        BluetoothDevice device = null;

        try {
            Method method = gattService.getClass().getDeclaredMethod("getDevice");
            method.setAccessible(true);
            device = (BluetoothDevice) method.invoke(gattService);
        } catch (Exception e) {
            Log.e(TAG, "" + e);
        }

        ParcelBluetoothGattService instance =
                new ParcelBluetoothGattService(device, new ParcelUuid(gattService.getUuid()),
                        gattService.getInstanceId(), gattService.getType());

        if (isDeep) {
            // Add included services
            if (BluetoothGattService.SERVICE_TYPE_PRIMARY == gattService.getType()) {
                List<BluetoothGattService> includedSrv = gattService.getIncludedServices();

                for (BluetoothGattService srv : includedSrv) {
                    instance.addIncludedService(ParcelBluetoothGattService.from(srv, isDeep));
                }
            }

            // Add characteristic
            List<BluetoothGattCharacteristic> charList = gattService.getCharacteristics();

            for (BluetoothGattCharacteristic gattChar : charList) {
                instance.addCharacteristic(
                        ParcelBluetoothGattCharacteristic.from(gattChar, isDeep));
            }
        }

        return instance;
    }

    public static ParcelBluetoothGattService from(BluetoothGattService gattService) {
        return ParcelBluetoothGattService.from(gattService, false);
    }

    public static final Parcelable.Creator<ParcelBluetoothGattService> CREATOR =
            new Parcelable.Creator<ParcelBluetoothGattService>() {
                @Override
                public ParcelBluetoothGattService createFromParcel(Parcel in) {

                    if (VDGB) {
                        Log.d(TAG, "createFromParcel, class = " + ParcelBluetoothGattService.class);
                    }

                    ClassLoader loader = ParcelBluetoothGattService.class.getClassLoader();

                    if (VDGB) {
                        Log.d(TAG, "loader = " + loader);
                    }

                    ParcelBluetoothGattService result = null;

                    BluetoothDevice device = in.readParcelable(null);
                    ParcelUuid uuid = in.readParcelable(null);
                    int instanceId = in.readInt();
                    int handles = in.readInt();
                    int serviceType = in.readInt();

                    result = new ParcelBluetoothGattService(device, uuid, instanceId, serviceType);
                    result.mHandles = handles;

                    // Read children elements
                    in.readList(result.mCharacteristics, loader);
                    in.readList(result.mIncludedServices, loader);

                    return result;
                }

                @Override
                public ParcelBluetoothGattService[] newArray(int size) {
                    return new ParcelBluetoothGattService[size];
                }
            };
}
