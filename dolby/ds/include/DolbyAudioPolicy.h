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

#ifndef DOLBY_AUDIO_POLICY_H_
#define DOLBY_AUDIO_POLICY_H_

#ifdef ANDROID_AUDIOPOLICYINTERFACE_H
#define AUDIO_POLICY_MANAGER_CLASS AudioPolicyManagerBase
#define USE_LEGACY_AUDIOPOLICY
#else
#define AUDIO_POLICY_MANAGER_CLASS AudioPolicyManager
#endif

/**
    This class is used to minimize Dolby code changes in AUDIO_POLICY_MANAGER_CLASS
*/
class DolbyAudioPolicy
{
public:
#ifdef USE_LEGACY_AUDIOPOLICY
    void setEndpointSystemProperty(audio_devices_t device, const Vector<HwModule*>& hwModules);
    void effectRegistered(EffectDescriptor *desc);
    void effectRemoved(EffectDescriptor *desc);
#else
    void setEndpointSystemProperty(audio_devices_t device, Vector < sp<HwModule> > hwModules);
    void effectRegistered(sp<EffectDescriptor> desc);
    void effectRemoved(sp<EffectDescriptor> desc);
#endif
    bool isAttachedToOutput(audio_io_handle_t output);
    bool shouldMoveToOutput(audio_io_handle_t output, audio_output_flags_t flags);
    void movedToOutput(audio_io_handle_t output);
    audio_io_handle_t output() const {
#ifdef USE_LEGACY_AUDIOPOLICY
        return (mDapEffectDesc != NULL) ? mDapEffectDesc->mIo : -1;
#else
        sp<EffectDescriptor> dapEffectDesc = mDapEffectDesc.promote();
        return (dapEffectDesc != 0) ? dapEffectDesc->mIo : -1;
#endif
    }

protected:
#ifdef USE_LEGACY_AUDIOPOLICY
    const char* getHdmiEndpoint(const Vector<HwModule*>& hwModules);
#else
    const char* getHdmiEndpoint(Vector < sp<HwModule> > hwModules);
#endif

private:
    friend class AUDIO_POLICY_MANAGER_CLASS;
    DolbyAudioPolicy();
    ~DolbyAudioPolicy();

protected:
#ifdef USE_LEGACY_AUDIOPOLICY
    EffectDescriptor *mDapEffectDesc;
#else
    wp<EffectDescriptor> mDapEffectDesc;
#endif
};

#endif//DOLBY_AUDIO_POLICY_H_
