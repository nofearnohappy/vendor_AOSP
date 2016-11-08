/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *               Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

#include "ds_config.h"

// System property shared with dolby codec
#define DOLBY_AUDIO_SINK_PROPERTY "dolby.audio.sink.info"

#ifdef ANDROID_AUDIOPOLICYINTERFACE_H
#define AUDIO_POLICY_MANAGER_CLASS AudioPolicyManagerBase
#define USE_LEGACY_AUDIOPOLICY
namespace android_audio_legacy {
#else
#define AUDIO_POLICY_MANAGER_CLASS AudioPolicyManager
namespace android {
#endif

AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::DolbyAudioPolicy()
{
    ALOGI("DolbyAudioPolicy() constructor");
    mDapEffectDesc = NULL;
    // Set dolby system property to speaker while booting,
    // if any other device is plugged-in setDeviceConnectionState will be called which
    // should set appropriate system property.
    property_set(DOLBY_AUDIO_SINK_PROPERTY, "speaker");
}

AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::~DolbyAudioPolicy()
{

}

/**
    Return the capabilities of connected HDMI endpoint

    This function goes through profiles of all hardware devices and
    gets the maximum number of channels supported by connected HDMI
    device. Since HDMI device has "dynamic" channel mask, it is updated
    when a device is connected.
*/

#ifdef USE_LEGACY_AUDIOPOLICY
const char* AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::getHdmiEndpoint(const Vector<HwModule*>& hwModules)
#else
const char* AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::getHdmiEndpoint(Vector < sp<HwModule> > hwModules)
#endif
{
    // Stereo is always supported
    const char* endpoint = "hdmi2";

    // Go through all hardware modules to find the HDMI profile
    for (size_t i = 0; i < hwModules.size(); i++) {
        for (size_t j = 0; j < hwModules[i]->mOutputProfiles.size(); j++) {
#ifdef USE_LEGACY_AUDIOPOLICY
            const IOProfile *profile = hwModules[i]->mOutputProfiles[j];
#else
            const sp<IOProfile> profile = hwModules[i]->mOutputProfiles[j];
#endif
            // HDMI profile supports AUX_DIGITAL device with DIRECT flag
#ifdef USE_LEGACY_AUDIOPOLICY
            if ((profile->mSupportedDevices & AUDIO_DEVICE_OUT_AUX_DIGITAL) &&
#else
            if ((profile->mSupportedDevices.types() & AUDIO_DEVICE_OUT_AUX_DIGITAL) &&
#endif
                (profile->mFlags & AUDIO_OUTPUT_FLAG_DIRECT)) {
                // Check all channel masks of the profile
                for (size_t k = 0; k < profile->mChannelMasks.size(); ++k) {
                    // If we have a mask with 7.1 channels then stop
                    if (profile->mChannelMasks[k] == AUDIO_CHANNEL_OUT_7POINT1) {
                        endpoint = "hdmi8";
                        break;
                    // Mark any mask with 5.1 channel support
                    } else if (profile->mChannelMasks[k] == AUDIO_CHANNEL_OUT_5POINT1) {
                        endpoint = "hdmi6";
                    }
                }
            }
        }
    }

    return endpoint;
}

/**
    Set the dolby system property dolby.audio.sink.info

    At present we are only setting system property for Headphone/Headset/HDMI/Speaker
    and the same is supported in DDPDecoder.cpp EndpointConfig table. If a new device
    is available eg. bluetooth or usb_audio, then system property must set in this
    function and also its downmix configuration should be set in DDPDecoder.cpp
    EndpointConfig table.
*/
#ifdef USE_LEGACY_AUDIOPOLICY
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::setEndpointSystemProperty(audio_devices_t device, const Vector<HwModule*>& hwModules)
#else
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::setEndpointSystemProperty(audio_devices_t device, Vector < sp<HwModule> > hwModules)
#endif
{
    ALOGV("DolbyAudioPolicy::%s(device=0x%08x)", __FUNCTION__, device);
    const char *endpoint = NULL;
    switch(device) {
    case AUDIO_DEVICE_OUT_WIRED_HEADSET:
    case AUDIO_DEVICE_OUT_WIRED_HEADPHONE:
        endpoint = "headset";
        break;
    case AUDIO_DEVICE_OUT_AUX_DIGITAL:
        endpoint = getHdmiEndpoint(hwModules);
        break;
    case AUDIO_DEVICE_OUT_SPEAKER:
        endpoint = "speaker";
        break;
    case AUDIO_DEVICE_OUT_REMOTE_SUBMIX:
        endpoint = "hdmi2";
        break;
    default:
        endpoint = "invalid";
    }
    ALOGI("DOLBY_ENDPOINT = %s", endpoint);
    property_set(DOLBY_AUDIO_SINK_PROPERTY, endpoint);
}

/**
    Capture the handle to DAP effect descriptor when the effect is registered.
*/
#ifdef USE_LEGACY_AUDIOPOLICY
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::effectRegistered(EffectDescriptor *desc)
#else
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::effectRegistered(sp<EffectDescriptor> desc)
#endif
{
#ifdef USE_LEGACY_AUDIOPOLICY
    ALOGV("%s(desc=%p)", __FUNCTION__, desc);
#else
    ALOGV("%s(desc=%p)", __FUNCTION__, desc->mDesc.name);
#endif
    if (memcmp(&desc->mDesc.uuid, &EFFECT_UUID_DS, sizeof(effect_uuid_t)) == 0) {
#ifdef USE_LEGACY_AUDIOPOLICY
        ALOGV("%s(desc=%p) Registered DAP Effect.", __FUNCTION__, desc->mDesc);
#else
        ALOGV("%s(desc=%p) Registered DAP Effect.", __FUNCTION__, desc->mDesc.name);
#endif
        mDapEffectDesc = desc;
    }
}

/**
    Release the handle to DAP effect descriptor when the effect is removed.
*/
#ifdef USE_LEGACY_AUDIOPOLICY
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::effectRemoved(EffectDescriptor *desc)
#else
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::effectRemoved(sp<EffectDescriptor> desc)
#endif
{
#ifdef USE_LEGACY_AUDIOPOLICY
    ALOGV("%s(desc=%p)", __FUNCTION__, desc);
#else
    ALOGV("%s(desc=%p)", __FUNCTION__, desc->mDesc.name);
#endif
    if (mDapEffectDesc == desc) {
#ifdef USE_LEGACY_AUDIOPOLICY
        ALOGV("%s(desc=%p) Removed DAP Effect.", __FUNCTION__, desc);
#else
        ALOGV("%s(desc=%p) Removed DAP Effect.", __FUNCTION__, desc->mDesc.name);
#endif
        mDapEffectDesc = NULL;
    }
}

/**
    Returns true if DAP effect is attached to specified output.
*/
bool AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::isAttachedToOutput(audio_io_handle_t output)
{
#ifdef USE_LEGACY_AUDIOPOLICY
    return (mDapEffectDesc != NULL) && (mDapEffectDesc->mIo == output);
#else
    sp<EffectDescriptor> dapEffectDesc = mDapEffectDesc.promote();
    return (dapEffectDesc != 0) && (dapEffectDesc->mIo == output);
#endif

}

/**
    Returns true if DAP effect should be moved to the specified output.
*/
bool AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::shouldMoveToOutput(audio_io_handle_t output, audio_output_flags_t flags)
{
    ALOGV("DolbyAudioPolicy::%s(output=%d, flags=0x%08x)", __FUNCTION__, output, flags);
    return (mDapEffectDesc != NULL) && !isAttachedToOutput(output) && !(
        (flags & AUDIO_OUTPUT_FLAG_DIRECT) ||
        (flags & AUDIO_OUTPUT_FLAG_FAST)
    );
}

/**
    Updates DAP effect descriptor with given output.

    This function must be called every time DAP effect is moved to a new output.
*/
void AUDIO_POLICY_MANAGER_CLASS::DolbyAudioPolicy::movedToOutput(audio_io_handle_t output)
{
#ifdef USE_LEGACY_AUDIOPOLICY
    ALOGV("DolbyAudioPolicy::%s() from %d to %d", __FUNCTION__, mDapEffectDesc->mIo, output);
    mDapEffectDesc->mIo = output;
#else
    sp<EffectDescriptor> dapEffectDesc = mDapEffectDesc.promote();
    if (dapEffectDesc != 0) {
        ALOGV("DolbyAudioPolicy::%s() from %d to %d", __FUNCTION__, dapEffectDesc->mIo, output);
        dapEffectDesc->mIo = output;
    }
#endif
}

}
