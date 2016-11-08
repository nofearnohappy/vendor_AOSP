/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.mmsdk;

import android.hardware.camera2.utils.BinderHolder;

/** @hide */
interface IMMSdkService
{
    /**
     * Keep up-to-date with vendor/mediatek/proprietary/frameworks/av/include/mmsdk/IMMSdkService.h	
     */
    // rest of 'int' return values in this file are actually status_t
    int connectImageTransformUser(out BinderHolder client);
    int connectEffect(String clientName, out BinderHolder client);
    int connectGesture(out BinderHolder client);
    int connectHeartrate(out BinderHolder client);
    int disconnectHeartrate();    
    int connectFeatureManager(out BinderHolder featureManager);

    //@todo un-comment later
    //int registerCamera1Device(NSCam::ICamDevice device);
    //int unRegisterCamera1Device(NSCam::ICamDevice device);
}
