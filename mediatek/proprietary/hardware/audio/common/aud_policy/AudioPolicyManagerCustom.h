/*
 * Copyright (C) 2009 The Android Open Source Project
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


#include "managerdefault/AudioPolicyManager.h"
#include "AudioCustomVolume.h"

namespace android {

// ----------------------------------------------------------------------------
// AudioPolicyManagerCustom implements audio policy manager behavior common to all platforms.
// ----------------------------------------------------------------------------

class AudioPolicyManagerCustom: public AudioPolicyManager
{

public:
        AudioPolicyManagerCustom(AudioPolicyClientInterface *clientInterface);
        virtual ~AudioPolicyManagerCustom();
        virtual void setPhoneState(audio_mode_t state);
protected:

        // compute the actual volume for a given stream according to the requested index
        // and a particular device
        virtual float computeVolume(audio_stream_type_t stream,
                                    int index,
                                    audio_devices_t device);

        // check that volume change is permitted, compute and send new volume to audio hardware
        virtual status_t checkAndSetVolume(audio_stream_type_t stream, int index,
                                           const sp<AudioOutputDescriptor>& outputDesc,
                                           audio_devices_t device, int delayMs = 0,
                                           bool force = false);

        bool mVoiceCurveReplaceDTMFCurve;
        bool mUseCustomVolume;
        // mute/unmute strategies using an incompatible device combination
        // if muting, wait for the audio in pcm buffer to be drained before proceeding
        // if unmuting, unmute only after the specified delay
        // Returns the number of ms waited
        virtual uint32_t  checkDeviceMuteStrategies(sp<AudioOutputDescriptor> outputDesc,
                                            audio_devices_t prevDevice,
                                            uint32_t delayMs);
        virtual status_t addAudioPatch(audio_patch_handle_t handle,
                               const sp<AudioPatch>& patch);
        virtual status_t removeAudioPatch(audio_patch_handle_t handle);
        // change the route of the specified output. Returns the number of ms we have slept to
        // allow new routing to take effect in certain cases.
        virtual uint32_t setOutputDevice(const sp<AudioOutputDescriptor>& outputDesc,
                             audio_devices_t device,
                             bool force = false,
                             int delayMs = 0,
                             audio_patch_handle_t *patchHandle = NULL,
                             const char* address = NULL);
private:
        float linearToLog(int volume);
        int logToLinear(float volume);
        int mapVol(float &vol, float unitstep);
        int mapping_Voice_vol(float &vol, float unitstep);
        float computeCustomVolume(int stream, int index, audio_devices_t device);
        int getStreamMaxLevels(int  stream);
        float mapVoiceVoltoCustomVol(unsigned char array[], int volmin, int volmax, float &vol);
        float mapVoltoCustomVol(unsigned char array[], int volmin, int volmax,float &vol ,
            int stream);
        AUDIO_CUSTOM_VOLUME_STRUCT mAudioCustVolumeTable;
        bool IsFMDirectMode(const sp<AudioPatch>& patch);
        bool IsFMActive(void);
};

};
