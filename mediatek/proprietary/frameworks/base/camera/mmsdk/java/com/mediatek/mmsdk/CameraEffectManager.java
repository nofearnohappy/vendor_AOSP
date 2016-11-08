/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * MediaTek Inc. (C) 2015. All rights reserved.
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
package com.mediatek.mmsdk;

import android.content.Context;
import android.hardware.camera2.utils.BinderHolder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.mmsdk.CameraEffect;
import com.mediatek.mmsdk.CameraEffectImpl;
import com.mediatek.mmsdk.CameraEffectHalRuntimeException;
import com.mediatek.mmsdk.EffectHalVersion;
import com.mediatek.mmsdk.CameraEffectHalException;
import com.mediatek.mmsdk.IEffectFactory;
import com.mediatek.mmsdk.IFeatureManager;
import com.mediatek.mmsdk.IMMSdkService;
import com.mediatek.mmsdk.IEffectListener;
import com.mediatek.mmsdk.IEffectHalClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A system service manager for detecting, and connecting to mmsdk service. and
 * open the target effect by openEffectHal(EffectHalVersion version,
 * CameraEffect.StateCallback callback, Handler handler);
 * @hide
 */
public class CameraEffectManager {

    private static final String TAG = "CameraEffectManager";
    private final Context mContext;
    private IMMSdkService mIMmsdkService;
    private IFeatureManager mIFeatureManager;
    private IEffectFactory mIEffectFactory;

    /**
     * According to the context get an CameraEffectManager object
     * @hide
     */
    public CameraEffectManager(Context context) {
        mContext = context;
    }

    /**
     * open the target effect which version is expected in the hander. the
     * effect client status will be notify by callback
     * @param version
     *            which effect need to open.
     * @param callback
     *            the effect status will be notified by this callback.
     * @param handler
     *            which handler will be triggered by the callback
     * @return when opened done, the CameraEffect object will be return.
     * @throws CameraEffectHalException
     *             will be throw exception when open the effect.
     * @hide
     */
    public CameraEffect openEffectHal(EffectHalVersion version,
            CameraEffect.StateCallback callback, Handler handler) throws CameraEffectHalException {
        if (version == null) {
            throw new IllegalArgumentException("effect version is null");
        } else if (handler == null) {
            if (Looper.myLooper() != null) {
                handler = new Handler();
            } else {
                throw new IllegalArgumentException("Looper doesn't exist in the calling thread");
            }
        }

        return openEffect(version, callback, handler);
    }

    /**
     * get which version of the effect supported.
     * @param effectName
     *            the effect name which version need to know
     * @return the valve which current project supported.the value has mName
     *         major version and minor version.
     * @throws CameraEffectHalException
     *             if error happened when get the supported version will throw
     *             this exception
     * @hide
     */
    public List<EffectHalVersion> getSupportedVersion(String effectName)
            throws CameraEffectHalException {
        List<EffectHalVersion> version = new ArrayList<EffectHalVersion>();
        getEffectFactory();
        try {
            mIEffectFactory.getSupportedVersion(effectName, version);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during getSupportedVersion", e);
        }
        return version;
    }

    /**
     * If you want to set the parameter for special feature's, but don't
     * want to open the Effect HAL Client, in some reason, the Effect HAL Client
     * maybe not exist. Such as need notify 3DNR value to native.
     * @param featureName
     *            which feature need to be configured. the name such as
     *            {@link BaseParameters#FEATURE_MASK_3DNR}
     * @param value
     *            the value of the feature want to configure. Such as "on" or "off".
     * @throws CameraEffectHalException
     *             exception will be throw if some error during set feature parameters.
     */
    public void setFeatureParameter(String featureName, String value)
            throws CameraEffectHalException {

        Log.i(TAG, "[setFeatureParameter] featureName = " + featureName + ",value = " + value);
        if (featureName == null || value == null) {
            throw new NullPointerException(
                    "setFeatureParameter exception,preferences is not be allowed to null");
        }
        getMmSdkService();

        try {
            getFeatureManager().setParameter(featureName, value);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during setFeatureParameter", e);
        }

    }

    /**
     * If you want to use effect Hal, Need first judge whether current project supported or not.
     * @return if get effect factory is null,means not support Effect Hal,So
     *         will return false. otherwise will return true.
     * @hide
     */
    public boolean isEffectHalSupported() {
        boolean isSupport = false;
        try {
            isSupport = getEffectFactory() != null;
        } catch (CameraEffectHalException e) {
            Log.i(TAG, "Current not support Effect HAl", e);
        }
        return isSupport;
    }

    private CameraEffect openEffect(EffectHalVersion version, CameraEffect.StateCallback callback,
            Handler handler) throws CameraEffectHalException {

        CameraEffect cameraEffect = null;

        // <Step1> first get the camera MM service
        getMmSdkService();
        // <Step2> get the Feature Manager
        getFeatureManager();

        // <Step3> get the Effect Factory
        getEffectFactory();

        // <Step4> get the effect HAL Client
        IEffectHalClient effectHalClient = createEffectHalClient(version);

        // <Step6> init the EffectHalClient
        int initValue = -1;
        try {
            // now native status: uninit -> init
            initValue = effectHalClient.init();
        } catch (RemoteException e1) {
            Log.e(TAG, "RemoteException during init", e1);

            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_INITIAL_ERROR);
        }
        // <Step5> create the Camera effect
        CameraEffectImpl cameraEffectImpl = new CameraEffectImpl(callback, handler);

        // <Step7> set effect listener
        IEffectListener effectListener = cameraEffectImpl.getEffectHalListener();
        int setListenerValue = -1;
        try {
            setListenerValue = effectHalClient.setEffectListener(effectListener);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during setEffectListener", e);

            CameraEffectHalRuntimeException exception = new CameraEffectHalRuntimeException(
                    CameraEffectHalException.EFFECT_HAL_LISTENER_ERROR);
            cameraEffectImpl.setRemoteCameraEffectFail(exception);

            throw exception.asChecked();
        }

        // <Step8> set remote effect camera
        cameraEffectImpl.setRemoteCameraEffect(effectHalClient);

        cameraEffect = cameraEffectImpl;

        Log.i(TAG, "[openEffect],version = " + version + ",initValue = " + initValue
                + ",setListenerValue = " + setListenerValue + ",cameraEffect = " + cameraEffect);

        return cameraEffect;
    }

    private IMMSdkService getMmSdkService() throws CameraEffectHalException {
        if (mIMmsdkService == null) {
            IBinder mmsdkService = ServiceManager.getService(BaseParameters.
                    CAMERA_MM_SERVICE_BINDER_NAME);
            if (mmsdkService == null) {
                throw new CameraEffectHalException(
                        CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR);
            }
            mIMmsdkService = IMMSdkService.Stub.asInterface(mmsdkService);
        }

        return mIMmsdkService;
    }

    private IFeatureManager getFeatureManager() throws CameraEffectHalException {
        getMmSdkService();
        if (mIFeatureManager == null) {
            BinderHolder featureManagerHolder = new BinderHolder();
            try {
                mIMmsdkService.connectFeatureManager(featureManagerHolder);
            } catch (RemoteException e) {
                throw new CameraEffectHalException(
                        CameraEffectHalException.EFFECT_HAL_FEATUREMANAGER_ERROR);
            }
            mIFeatureManager = IFeatureManager.Stub.asInterface(featureManagerHolder.getBinder());
        }
        return mIFeatureManager;
    }

    private IEffectFactory getEffectFactory() throws CameraEffectHalException {
        getFeatureManager();
        if (mIEffectFactory == null) {
            BinderHolder effectFactoryHolder = new BinderHolder();
            try {
                mIFeatureManager.getEffectFactory(effectFactoryHolder);
            } catch (RemoteException e) {
                throw new CameraEffectHalException(
                        CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR);
            }
            mIEffectFactory = IEffectFactory.Stub.asInterface(effectFactoryHolder.getBinder());
        }
        return mIEffectFactory;
    }

    private IEffectHalClient createEffectHalClient(EffectHalVersion version)
            throws CameraEffectHalException {
        getEffectFactory();
        BinderHolder effectHalClientHolder = new BinderHolder();
        try {
            mIEffectFactory.createEffectHalClient(version, effectHalClientHolder);
        } catch (RemoteException e) {
            throw new CameraEffectHalException(CameraEffectHalException.EFFECT_HAL_CLIENT_ERROR);
        }
        return IEffectHalClient.Stub.asInterface(effectHalClientHolder.getBinder());

    }
}
