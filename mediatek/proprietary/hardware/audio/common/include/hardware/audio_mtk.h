/*
 * Copyright (C) 2011 The Android Open Source Project
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


#ifndef ANDROID_AUDIO_HAL_INTERFACE_MTK_H
#define ANDROID_AUDIO_HAL_INTERFACE_MTK_H

#define AUDIO_PARAMETER_KEY_TIME_STRETCH "time_stretch"
#define AUDIO_PARAMETER_KEY_HDMI_BITWIDCH "HDMI_bitwidth"
#define AUDIO_PARAMETER_KEY_HDMI_CHANNEL "HDMI_channel"
#define AUDIO_PARAMETER_KEY_HDMI_MAXSAMPLERATE "HDMI_maxsamplingrate"
#define AUDIO_PARAMETER_KEY_BESSURROUND_ONOFF "BesSurround_OnOff"
#define AUDIO_PARAMETER_KEY_BESSURROUND_MODE "BesSurround_Mode"

#define AUDIO_PARAMETER_KEY_HDMI_MAXSAMPLERATE "HDMI_maxsamplingrate"
#define AUDIO_PARAMETER_KEY_BESSURROUND_ONOFF "BesSurround_OnOff"
#define AUDIO_PARAMETER_KEY_BESSURROUND_MODE "BesSurround_Mode"

// LosslessBT related
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_VOLUME_SATISFIED "LosslessBT_VolumeSatisfied"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_PCM_PLAYING "Pcm_Playing"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_OFFLOAD_PLAYING "Offload_Playing"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_STANDBY_WHEN_MUTE "LosslessBT_Do_Standby_When_Mute"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_OFFLOAD_SUPPORT "Offload_Support"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_STATUS "LosslessBT_Status"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_WORKING "LosslessBT_Working"
#define AUDIO_PARAMETER_KEY_LOSSLESS_BT_ABSOLUTE_VOLUME "LosslessBT_AbsoluteVolume"

#define AUDIO_PARAMETER_KEY_ROUTING_TO_NONE "ROUTING_TO_NONE"
#define AUDIO_PARAMETER_KEY_FM_DIRECT_CONTROL "FM_DIRECT_CONTROL"

#define AUDIO_PARAMETER_KEY_OFFLOAD_AUDIO_STANDBY_WHEN_MUTE "OffloadAudio_Do_Standby_When_Mute"


#include <hardware/audio.h>

struct audio_hw_device_mtk: audio_hw_device{

    int (*SetEMParameter)(struct audio_hw_device *dev,void *ptr , int len);
    int (*GetEMParameter)(struct audio_hw_device *dev,void *ptr , int len);
    int (*SetAudioCommand)(struct audio_hw_device *dev,int par1 , int par2);
    int (*GetAudioCommand)(struct audio_hw_device *dev,int par1);
    int (*SetAudioData)(struct audio_hw_device *dev,int par1,size_t len,void *ptr);
    int (*GetAudioData)(struct audio_hw_device *dev,int par1,size_t len,void *ptr);
    int (*SetACFPreviewParameter)(struct audio_hw_device *dev,void *ptr , int len);
    int (*SetHCFPreviewParameter)(struct audio_hw_device *dev,void *ptr , int len);

    int (*xWayPlay_Start)(struct audio_hw_device *dev,int sample_rate);
    int (*xWayPlay_Stop)(struct audio_hw_device *dev);
    int (*xWayPlay_Write)(struct audio_hw_device *dev,void* buffer ,int size_bytes);
    int (*xWayPlay_GetFreeBufferCount)(struct audio_hw_device *dev);
    int (*xWayRec_Start)(struct audio_hw_device *dev,int smple_rate);
    int (*xWayRec_Stop)(struct audio_hw_device *dev);
    int (*xWayRec_Read)(struct audio_hw_device *dev,void* buffer , int size_bytes);

    int (*ReadRefFromRing)(struct audio_hw_device* dev, void*buf, uint32_t datasz, void* DLtime);
    int (*GetVoiceUnlockULTime)(struct audio_hw_device* dev, void* ULtime);
    int (*SetVoiceUnlockSRC)(struct audio_hw_device* dev, uint outSR, uint outChannel);
    bool (*startVoiceUnlockDL)(struct audio_hw_device* dev);
    bool (*stopVoiceUnlockDL)(struct audio_hw_device* dev);
    void (*freeVoiceUnlockDLInstance)(struct audio_hw_device* dev);
    bool (*getVoiceUnlockDLInstance)(struct audio_hw_device* dev);
    int (* GetVoiceUnlockDLLatency)(struct audio_hw_device* dev);
};
typedef struct audio_hw_device_mtk audio_hw_device_mtk_t;

#endif  // ANDROID_AUDIO_HAL_INTERFACE_MTK_H
