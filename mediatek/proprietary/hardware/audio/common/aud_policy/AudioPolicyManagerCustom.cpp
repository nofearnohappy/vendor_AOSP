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

#define LOG_TAG "AudioPolicyManagerCustom"

#define LOG_NDEBUG 0
#define VERY_VERBOSE_LOGGING

//#define VERY_VERBOSE_LOGGING
#ifdef VERY_VERBOSE_LOGGING
#define ALOGVV ALOGV
#else
#define ALOGVV(a...) do { } while(0)
#endif

#include <inttypes.h>
#include <math.h>

#include <cutils/properties.h>
#include <utils/Log.h>
#include <hardware/audio.h>
#include <hardware/audio_effect.h>
#include <media/AudioParameter.h>
#include <soundtrigger/SoundTrigger.h>
#include "AudioPolicyManagerCustom.h"
#include <policy.h>
#include <Volume.h>
//#include "audio_policy_conf.h"
#include <dlfcn.h>
#include <cutils/properties.h>
#include "hardware_legacy/AudioMTKHardwareInterface.h"


#define HAL_LIBRARY_PATH1 "/system/lib/hw"
#define HAL_LIBRARY_PATH2 "/vendor/lib/hw"
#define AUDIO_HAL_PREFIX "audio.primary"
#define PLATFORM_ID "ro.board.platform"
#define BOARD_PLATFORM_ID "ro.board.platform"

static android_audio_legacy::AudioMTKHardwareInterface *gAudioMTKHardware = NULL;
static void *AudioHwhndl = NULL;

namespace android {

void AudioPolicyManagerCustom::setPhoneState(audio_mode_t state)
{
    ALOGD("setPhoneState() state %d", state);

    if (state < 0 || state >= AUDIO_MODE_CNT) {
        ALOGW("setPhoneState() invalid state %d", state);
        return ;
    }

    if (state == mEngine->getPhoneState()) {
        ALOGW("setPhoneState() setting same state %d", state);
        return ;
    }

    // store previous phone state for management of sonification strategy below
    int oldState = mEngine->getPhoneState();
    // are we entering or starting a call
    if (!is_state_in_call(oldState) && is_state_in_call(state)) {
        ALOGV("  Entering call in setPhoneState()");
        mVoiceCurveReplaceDTMFCurve = true;
    } else if (is_state_in_call(oldState) && !is_state_in_call(state)) {
        ALOGV("  Exiting call in setPhoneState()");
        mVoiceCurveReplaceDTMFCurve = false;
    }

    AudioPolicyManager::setPhoneState(state);
}

AudioPolicyManagerCustom::AudioPolicyManagerCustom(AudioPolicyClientInterface *clientInterface)
    :AudioPolicyManager(clientInterface)
{
    ALOGD("AudioPolicyManagerCustom Construct");
    mVoiceCurveReplaceDTMFCurve = false;
    mUseCustomVolume = false;
    //load volume
    if (gAudioMTKHardware == NULL ||AudioHwhndl == NULL) {
        char prop[PATH_MAX];
        char path[PATH_MAX];
        do {
            if (property_get(PLATFORM_ID, prop, NULL) == 0) {
                snprintf(path, sizeof(path), "%s/%s.default.so",
                         HAL_LIBRARY_PATH1, prop);
                if (access(path, R_OK) == 0) break;

                snprintf(path, sizeof(path), "%s/%s.default.so",
                     HAL_LIBRARY_PATH2, prop);
                if (access(path, R_OK) == 0) break;
            } else {
                snprintf(path, sizeof(path), "%s/%s.%s.so",
                         HAL_LIBRARY_PATH1, AUDIO_HAL_PREFIX, prop);
                if (access(path, R_OK) == 0) break;

                snprintf(path, sizeof(path), "%s/%s.%s.so",
                     HAL_LIBRARY_PATH2, AUDIO_HAL_PREFIX, prop);
                if (access(path, R_OK) == 0) break;

                if (property_get(BOARD_PLATFORM_ID, prop, NULL) == 0) {
                    snprintf(path, sizeof(path), "%s/%s.default.so",
                             HAL_LIBRARY_PATH1, prop);
                    if (access(path, R_OK) == 0) break;

                    snprintf(path, sizeof(path), "%s/%s.default.so",
                         HAL_LIBRARY_PATH2, prop);
                    if (access(path, R_OK) == 0) break;
                } else {
                    snprintf(path, sizeof(path), "%s/%s.%s.so",
                         HAL_LIBRARY_PATH1, AUDIO_HAL_PREFIX, prop);
                     if (access(path, R_OK) == 0) break;

                     snprintf(path, sizeof(path), "%s/%s.%s.so",
                          HAL_LIBRARY_PATH2, AUDIO_HAL_PREFIX, prop);
                     if (access(path, R_OK) == 0) break;
                }

            }
        } while(0);

        ALOGD ("Load %s",path);

        AudioHwhndl = dlopen(path, RTLD_NOW);

        if (AudioHwhndl == NULL) {
            ALOGE("-DL open AudioHwhndl path [%s] fail",path);
            return;
        } else {
            create_AudioMTKHw* func1 = (create_AudioMTKHw*)dlsym(AudioHwhndl, "createMTKAudioHardware");
            ALOGD("%s %d func1 %p",__FUNCTION__,__LINE__,func1);
            const char* dlsym_error1 = dlerror();
            if (func1 == NULL) {
                ALOGE("-dlsym pfuncGetAudioMTKHardwareExtByDL fail");
                dlclose(AudioHwhndl);
                AudioHwhndl = NULL;
                return ;
            }
            gAudioMTKHardware = func1();
            ALOGD("%s %d gAudioMTKHardware %p",__FUNCTION__,__LINE__,gAudioMTKHardware);
        }
    }

    mAudioCustVolumeTable.bRev = CUSTOM_VOLUME_REV_1;
    mAudioCustVolumeTable.bReady = 0;
    gAudioMTKHardware->GetAudioCustomVol(&mAudioCustVolumeTable,sizeof(mAudioCustVolumeTable));
    if (mAudioCustVolumeTable.bReady != 0) {
        ALOGD("mUseCustomVolume true");
        mUseCustomVolume = true;
    } else {
        ALOGD("mUseCustomVolume false");
    }
    if (AudioHwhndl != NULL) {
        dlclose(AudioHwhndl);
        AudioHwhndl = NULL;
        gAudioMTKHardware = NULL;
    }
    //apply all outputs
    for (size_t i = 0; i < mOutputs.size(); i++) {
        ALOGD("%d/%d",i,mOutputs.size());
        audio_io_handle_t output = mOutputs.keyAt(i);
        if (output != 0) {
            sp<SwAudioOutputDescriptor> desc = mOutputs.valueAt(i);
            setOutputDevice(desc,
                            desc->mDevice,
                            true);
        }
    }
}

AudioPolicyManagerCustom::~AudioPolicyManagerCustom()
{
}

float AudioPolicyManagerCustom::computeVolume(audio_stream_type_t stream,
                                            int index,
                                            audio_devices_t device)
{
    float volumeDb = 0.0;
    StreamDescriptor &streamDesc = mStreams.editValueFor(stream);

    if (mUseCustomVolume) {
        volumeDb = Volume::AmplToDb(computeCustomVolume(stream, index, device));
    } else {
        volumeDb = mEngine->volIndexToDb(Volume::getDeviceCategory(device), stream, index);
    }

    // if a headset is connected, applyolume the following rules to ring tones and notifications
    // to avoid sound level bursts in user's ears:
    // - always attenuate ring tones and notifications volume by 6dB
    // - if music is playing, always limit the volume to current music volume,
    // with a minimum threshold at -36dB so that notification is always perceived.
    const routing_strategy stream_strategy = getStrategy(stream);
    if ((device & (AUDIO_DEVICE_OUT_BLUETOOTH_A2DP |
            AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES |
            AUDIO_DEVICE_OUT_WIRED_HEADSET |
            AUDIO_DEVICE_OUT_WIRED_HEADPHONE)) &&
        ((stream_strategy == STRATEGY_SONIFICATION)
                || (stream_strategy == STRATEGY_SONIFICATION_RESPECTFUL)
                || (stream == AUDIO_STREAM_SYSTEM)
                || ((stream_strategy == STRATEGY_ENFORCED_AUDIBLE) &&
                    (getForceUse(AUDIO_POLICY_FORCE_FOR_SYSTEM) == AUDIO_POLICY_FORCE_NONE))) &&
        streamDesc.canBeMuted()) {
        volumeDb += SONIFICATION_HEADSET_VOLUME_FACTOR_DB;
        // when the phone is ringing we must consider that music could have been paused just before
        // by the music application and behave as if music was active if the last music track was
        // just stopped
        if (isStreamActive(AUDIO_STREAM_MUSIC, SONIFICATION_HEADSET_MUSIC_DELAY) ||
                mLimitRingtoneVolume) {
            audio_devices_t musicDevice = getDeviceForStrategy(STRATEGY_MEDIA, true /*fromCache*/);
            float musicVolDB = computeVolume(AUDIO_STREAM_MUSIC,
                    mStreams.editValueFor(AUDIO_STREAM_MUSIC).getVolumeIndex(musicDevice),
                    musicDevice);
            float minVolDB = (musicVolDB > SONIFICATION_HEADSET_VOLUME_MIN_DB) ?
                    musicVolDB : SONIFICATION_HEADSET_VOLUME_MIN_DB;
            if (volumeDb > minVolDB) {
                volumeDb = minVolDB;
                ALOGV("computeVolume limiting volume to %f musicVol %f", minVolDB, musicVolDB);
            }
        }
    }

    return volumeDb;
}


status_t AudioPolicyManagerCustom::checkAndSetVolume(audio_stream_type_t stream,
                                                   int index,
                                                   const sp<AudioOutputDescriptor>& outputDesc,
                                                   audio_devices_t device,
                                                   int delayMs,
                                                   bool force)
{

    // do not change actual stream volume if the stream is muted
    if (outputDesc->mMuteCount[stream] != 0) {
        ALOGVV("checkAndSetVolume() stream %d muted count %d",
              stream, outputDesc->mMuteCount[stream]);
        return NO_ERROR;
    }

    audio_policy_forced_cfg_t forceUseForComm =
            mEngine->getForceUse(AUDIO_POLICY_FORCE_FOR_COMMUNICATION);
    // do not change in call volume if bluetooth is connected and vice versa
    if ((stream == AUDIO_STREAM_VOICE_CALL && forceUseForComm == AUDIO_POLICY_FORCE_BT_SCO) ||
        (stream == AUDIO_STREAM_BLUETOOTH_SCO && forceUseForComm != AUDIO_POLICY_FORCE_BT_SCO)) {
        ALOGV("checkAndSetVolume() cannot set stream %d volume with force use = %d for comm",
             stream, forceUseForComm);
        return INVALID_OPERATION;
    }

    if (device == AUDIO_DEVICE_NONE) {
        device = outputDesc->device();
    }

    float volumeDb = computeVolume(stream, index, device);
    if (outputDesc->isFixedVolume(device)) {
        volumeDb = 0.0f;
    }

    if (outputDesc->setVolume(volumeDb, stream, device, delayMs, force)) {
        if (stream == AUDIO_STREAM_MUSIC) {
            for (ssize_t i = 0; i < (ssize_t)mAudioPatches.size(); i++) {
            ALOGV("%s size %d/%d",__FUNCTION__,i,mAudioPatches.size());
            sp<AudioPatch> patchDesc = mAudioPatches.valueAt(i);
            if (IsFMDirectMode(patchDesc)) {
                    ALOGV("Do modify audiopatch volume");
                    struct audio_port_config *config;
                    sp<AudioPortConfig> audioPortConfig;
                    sp<DeviceDescriptor> deviceDesc;
                    config = &(patchDesc->mPatch.sinks[0]);
                    if (config->role == AUDIO_PORT_ROLE_SINK) {
                        deviceDesc = mAvailableOutputDevices.getDeviceFromId(config->id);
                    } else {
                        break;
                    }
                    if (deviceDesc == NULL) {
                        break;
                    }
                    audioPortConfig = deviceDesc;
                    struct audio_port_config newConfig;
                    audioPortConfig->toAudioPortConfig(&newConfig, config);
                    newConfig.config_mask = AUDIO_PORT_CONFIG_GAIN|newConfig.config_mask;
                    newConfig.gain.mode = AUDIO_GAIN_MODE_JOINT|newConfig.gain.mode;
                    newConfig.gain.values[0] = -300*(getStreamMaxLevels(stream)-index);
                    mpClientInterface->setAudioPortConfig(&newConfig, delayMs);

                }
            }
        }
    }

    if (stream == AUDIO_STREAM_VOICE_CALL ||
        stream == AUDIO_STREAM_BLUETOOTH_SCO) {
        float voiceVolume;
        // Force voice volume to max for bluetooth SCO as volume is managed by the headset
        if (stream == AUDIO_STREAM_VOICE_CALL) {
            if (mUseCustomVolume) {
                voiceVolume = computeCustomVolume(stream, index, device);
            } else {
                voiceVolume = (float)index/(float)mStreams.valueFor(stream).getVolumeIndexMax();
            }
        } else {
            voiceVolume = 1.0;
        }

        if (voiceVolume != mLastVoiceVolume && outputDesc == mPrimaryOutput) {
            mpClientInterface->setVoiceVolume(voiceVolume, delayMs);
            mLastVoiceVolume = voiceVolume;
        }
    }

    return NO_ERROR;
}

float AudioPolicyManagerCustom::linearToLog(int volume)
{
    return volume ? exp(float(fCUSTOM_VOLUME_MAPPING_STEP - volume) * fBConvert) : 0;
}

int AudioPolicyManagerCustom::logToLinear(float volume)
{
    return volume ? fCUSTOM_VOLUME_MAPPING_STEP - int(fBConvertInverse * log(volume) + 0.5) : 0;
}

int AudioPolicyManagerCustom::mapVol(float &vol, float unitstep)
{
    int index = (vol+0.5)/unitstep;
    vol -= (index*unitstep);
    return index;
}

int AudioPolicyManagerCustom::mapping_Voice_vol(float &vol, float unitstep)
{
    if (vol < unitstep) {
        return 1;
    } else if (vol < unitstep*2) {
        vol -= unitstep;
        return 2;
    } else if(vol < unitstep*3) {
        vol -= unitstep*2;
        return 3;
    } else if(vol < unitstep*4) {
        vol -= unitstep*3;
        return 4;
    } else if(vol < unitstep*5) {
        vol -= unitstep*4;
        return 5;
    } else if(vol < unitstep*6) {
        vol -= unitstep*5;
        return 6;
    } else if(vol < unitstep*7) {
        vol -= unitstep*6;
        return 7;
    } else {
        ALOGW("vole = %f unitstep = %f",vol,unitstep);
        return 0;
    }
}

int AudioPolicyManagerCustom::getStreamMaxLevels(int stream)
{
    return (int) mAudioCustVolumeTable.audiovolume_level[stream];
}

// this function will map vol 0~100 , base on customvolume map to 0~255 , and do linear calculation to set mastervolume
float AudioPolicyManagerCustom::mapVoltoCustomVol(unsigned char array[], int volmin, int volmax,float &vol , int stream)
{
    ALOGV("+MapVoltoCustomVol vol = %f stream = %d volmin = %d volmax = %d",vol,stream,volmin,volmax);

    if (stream == AUDIO_STREAM_VOICE_CALL) {
        return mapVoiceVoltoCustomVol(array,volmin,volmax,vol);
    }

    float volume =0.0;
    StreamDescriptor &streamDesc = mStreams.editValueFor((audio_stream_type_t)stream);
    if (vol == 0) {
        volume = vol;
        return 0;
    } else {    // map volume value to custom volume
        float unitstep = fCUSTOM_VOLUME_MAPPING_STEP/getStreamMaxLevels(stream);
        if (vol < fCUSTOM_VOLUME_MAPPING_STEP/streamDesc.getVolumeIndexMax()) {
            volume = array[0];
            vol = volume;
            return volume;
        }
        int Index = mapVol(vol, unitstep);
        float Remind = (1.0 - (float)vol/unitstep);
        if (Index != 0) {
            volume = ((array[Index]  - (array[Index] - array[Index-1]) * Remind)+0.5);
        } else {
            volume = 0;
        }
    }
    // -----clamp for volume
    if ( volume > 253.0) {
        volume = fCUSTOM_VOLUME_MAPPING_STEP;
    } else if ( volume <= array[0]) {
        volume = array[0];
    }
    vol = volume;
    return volume;
}

// this function will map vol 0~100 , base on customvolume map to 0~255 , and do linear calculation to set mastervolume
float AudioPolicyManagerCustom::mapVoiceVoltoCustomVol(unsigned char array[], int volmin, int volmax, float &vol)
{
    vol = (int)vol;
    float volume = 0.0;
    StreamDescriptor &streamDesc = mStreams.editValueFor(AUDIO_STREAM_VOICE_CALL);
    if (vol == 0) {
        volume = array[0];
    } else {
        int dMaxIndex = getStreamMaxLevels(AUDIO_STREAM_VOICE_CALL)-1;
        if (vol >= fCUSTOM_VOLUME_MAPPING_STEP) {
            volume = array[dMaxIndex];
        } else {
            double unitstep = fCUSTOM_VOLUME_MAPPING_STEP /dMaxIndex;
            int Index = mapping_Voice_vol(vol, unitstep);
            // boundary for array
            if (Index >= dMaxIndex) {
                Index = dMaxIndex;
            }
            float Remind = (1.0 - (float)vol/unitstep) ;
            if (Index != 0) {
                volume = (array[Index]  - (array[Index] - array[Index- 1]) * Remind)+0.5;
            } else {
                volume =0;
            }
        }
    }

     if( volume > CUSTOM_VOICE_VOLUME_MAX){
         volume = CUSTOM_VOICE_VOLUME_MAX;
     }
     else if( volume <= array[0]){
         volume = array[0];
     }

     vol = volume;
     float degradeDb = (CUSTOM_VOICE_VOLUME_MAX-vol)/CUSTOM_VOICE_ONEDB_STEP;
     vol = fCUSTOM_VOLUME_MAPPING_STEP - (degradeDb*4);
     return volume;
}

float AudioPolicyManagerCustom::computeCustomVolume(int stream, int index, audio_devices_t device)
{
    // check if force use exist , get output device for certain mode
    int OutputDevice = device;
    // compute custom volume
    float volume =0.0;
    int volmax=0 , volmin =0,volumeindex =0;
    int custom_vol_device_mode,audiovolume_steamtype;

    if (mVoiceCurveReplaceDTMFCurve && stream == AUDIO_STREAM_DTMF) {
            stream = AUDIO_STREAM_VOICE_CALL;
    }

    StreamDescriptor &streamDesc = mStreams.editValueFor((audio_stream_type_t)stream);
    float volInt = (fCUSTOM_VOLUME_MAPPING_STEP * (index - streamDesc.getVolumeIndexMin()))
            / (streamDesc.getVolumeIndexMax() - streamDesc.getVolumeIndexMin());

    if (OutputDevice == AUDIO_DEVICE_OUT_SPEAKER) {
        custom_vol_device_mode = CUSTOM_VOLUME_SPEAKER_MODE;
    } else if ((OutputDevice == AUDIO_DEVICE_OUT_WIRED_HEADSET) || (OutputDevice == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)) {
        custom_vol_device_mode = CUSTOM_VOLUME_HEADSET_MODE;
    } else if (OutputDevice == AUDIO_DEVICE_OUT_EARPIECE) {
        custom_vol_device_mode = CUSTOM_VOLUME_NORMAL_MODE;
    } else {
        custom_vol_device_mode = CUSTOM_VOLUME_HEADSET_SPEAKER_MODE;
    }

    if ((stream == AUDIO_STREAM_VOICE_CALL)
            && (AudioPolicyManager::getPhoneState() == AUDIO_MODE_IN_COMMUNICATION)) {
        audiovolume_steamtype = CUSTOM_VOL_TYPE_SIP;
    } else {
        audiovolume_steamtype = stream;
    }

    volmax =mAudioCustVolumeTable.audiovolume_steamtype[audiovolume_steamtype][custom_vol_device_mode][getStreamMaxLevels(stream)-1];
    volmin = mAudioCustVolumeTable.audiovolume_steamtype[audiovolume_steamtype][custom_vol_device_mode][0];
    volume = mapVoltoCustomVol(mAudioCustVolumeTable.audiovolume_steamtype[audiovolume_steamtype][custom_vol_device_mode],volmin,volmax,volInt,stream);

    volume = linearToLog(volInt);
    ALOGV("stream = %d after computeCustomVolume , volInt = %f volume = %f",stream,volInt,volume);
    return volume;
}

uint32_t AudioPolicyManagerCustom::checkDeviceMuteStrategies(sp<AudioOutputDescriptor> outputDesc,
                                                       audio_devices_t prevDevice,
                                                       uint32_t delayMs)
{
    // mute/unmute strategies using an incompatible device combination
    // if muting, wait for the audio in pcm buffer to be drained before proceeding
    // if unmuting, unmute only after the specified delay
    if (outputDesc->isDuplicated()) {
        return 0;
    }
    uint32_t muteWaitMs = 0;
    audio_devices_t device = outputDesc->device();
    bool shouldMute = outputDesc->isActive() && (popcount(device) >= 2);
    for (size_t i = 0; i < NUM_STRATEGIES; i++) {
        audio_devices_t curDevice = getDeviceForStrategy((routing_strategy)i, false /*fromCache*/);
        curDevice = curDevice & outputDesc->supportedDevices();
        bool mute = shouldMute && (curDevice & device) && (curDevice != device);
        bool doMute = false;

        if (mute && !outputDesc->mStrategyMutedByDevice[i]) {
            doMute = true;
            outputDesc->mStrategyMutedByDevice[i] = true;
        } else if (!mute && outputDesc->mStrategyMutedByDevice[i]){
            doMute = true;
            outputDesc->mStrategyMutedByDevice[i] = false;
        }
        if (doMute) {
            for (size_t j = 0; j < mOutputs.size(); j++) {
                sp<AudioOutputDescriptor> desc = mOutputs.valueAt(j);
                // skip output if it does not share any device with current output
                if ((desc->supportedDevices() & outputDesc->supportedDevices())
                        == AUDIO_DEVICE_NONE) {
                    continue;
                }
                ALOGV("checkDeviceMuteStrategies() %s strategy %d (curDevice %04x)",
                      mute ? "muting" : "unmuting", i, curDevice);
                setStrategyMute((routing_strategy)i, mute, desc, mute ? 0 : delayMs);
                if (isStrategyActive(desc, (routing_strategy)i)) {
                    if (mute) {
                        // FIXME: should not need to double latency if volume could be applied
                        // immediately by the audioflinger mixer. We must account for the delay
                        // between now and the next time the audioflinger thread for this output
                        // will process a buffer (which corresponds to one buffer size,
                        // usually 1/2 or 1/4 of the latency).
                        if (muteWaitMs < desc->latency() * 2) {
                            muteWaitMs = desc->latency() * 2;
                        }
                    }
                }
            }
        }
    }

    // temporary mute output if device selection changes to avoid volume bursts due to
    // different per device volumes
    if (outputDesc->isActive() && (device != prevDevice)) {
        if (muteWaitMs < outputDesc->latency() * 2) {
            muteWaitMs = outputDesc->latency() * 2;
        }
        for (size_t i = 0; i < NUM_STRATEGIES; i++) {
            if (isStrategyActive(outputDesc, (routing_strategy)i)) {
                uint32_t MuteFactor = 2;
                if (i == STRATEGY_MEDIA && IsFMActive()) {
                    MuteFactor = 8;//FM callback to app latency
                }
                setStrategyMute((routing_strategy)i, true, outputDesc);
                // do tempMute unmute after twice the mute wait time
                setStrategyMute((routing_strategy)i, false, outputDesc,
                                muteWaitMs *MuteFactor, device);
            }
        }
    }

    // wait for the PCM output buffers to empty before proceeding with the rest of the command
    if (muteWaitMs > delayMs) {
        muteWaitMs -= delayMs;
        usleep(muteWaitMs * 1000);
        return muteWaitMs;
    }
    return 0;
}


status_t AudioPolicyManagerCustom::addAudioPatch(audio_patch_handle_t handle,
                                           const sp<AudioPatch>& patch)
{
    ssize_t index = mAudioPatches.indexOfKey(handle);
    bool bFMeable = false;
    status_t status;

    if (index >= 0) {
        ALOGW("addAudioPatch() patch %d already in", handle);
        return ALREADY_EXISTS;
    }

    if (IsFMDirectMode(patch)) {
        if (mPrimaryOutput != 0) {
            ALOGV("audiopatch Music+");
            mPrimaryOutput->changeRefCount(AUDIO_STREAM_MUSIC, 1);
            bFMeable = true;
        }
    }

    status = AudioPolicyManager::addAudioPatch(handle,patch);

    if (bFMeable) {
        applyStreamVolumes(mPrimaryOutput,
                           patch->mPatch.sinks[0].ext.device.type,
                           mPrimaryOutput->latency()*2, true);
    }

    return status;
}


status_t AudioPolicyManagerCustom::removeAudioPatch(audio_patch_handle_t handle)
{
    ssize_t index = mAudioPatches.indexOfKey(handle);
    bool bFMeable = false;
    status_t status;

    if (index < 0) {
        ALOGW("removeAudioPatch() patch %d not in", handle);
        return ALREADY_EXISTS;
    }

    const sp<AudioPatch> patch = mAudioPatches.valueAt(index);

    if (IsFMDirectMode(patch)) {
        if (mPrimaryOutput != 0) {
            if (mPrimaryOutput->mRefCount[AUDIO_STREAM_MUSIC] > 0) {
                ALOGV("audiopatch Music-");
                mPrimaryOutput->changeRefCount(AUDIO_STREAM_MUSIC, -1);
                bFMeable = true;
            }
        }
    }

    status = AudioPolicyManager::removeAudioPatch(handle);

    if (bFMeable) {
        audio_devices_t newDevice = getNewOutputDevice(mPrimaryOutput, false /*fromCache*/);
        applyStreamVolumes(mPrimaryOutput, newDevice, mPrimaryOutput->latency()*2);
    }
    return status;
}


bool AudioPolicyManagerCustom::IsFMDirectMode(const sp<AudioPatch>& patch)
{
    if (patch->mPatch.sources[0].type == AUDIO_PORT_TYPE_DEVICE &&
        patch->mPatch.sinks[0].type == AUDIO_PORT_TYPE_DEVICE &&
        (patch->mPatch.sinks[0].ext.device.type == AUDIO_DEVICE_OUT_WIRED_HEADSET ||
        patch->mPatch.sinks[0].ext.device.type == AUDIO_DEVICE_OUT_WIRED_HEADPHONE) &&
        (patch->mPatch.sources[0].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER)) {
        return true;
    }
    else {
        return false;
    }
}

bool AudioPolicyManagerCustom::IsFMActive(void)
{

    for (ssize_t i = 0; i < (ssize_t)mAudioPatches.size(); i++) {
        ALOGV("%s size %d/%d",__FUNCTION__,i,mAudioPatches.size());
        sp<AudioPatch> patchDesc = mAudioPatches.valueAt(i);
        if (IsFMDirectMode(patchDesc)||
            (patchDesc->mPatch.sources[0].type == AUDIO_PORT_TYPE_DEVICE
            &&patchDesc->mPatch.sources[0].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER)) {
            ALOGV("FM Active");
            return true;
        }
    }

    return false;
}

uint32_t AudioPolicyManagerCustom::setOutputDevice(const sp<AudioOutputDescriptor>& outputDesc,
                                             audio_devices_t device,
                                             bool force,
                                             int delayMs,
                                             audio_patch_handle_t *patchHandle,
                                             const char* address)
{
    ALOGV("setOutputDevice() device %04x delayMs %d force %d", device, delayMs, force);
    AudioParameter param;
    uint32_t muteWaitMs;

    if (outputDesc->isDuplicated()) {
        muteWaitMs = setOutputDevice(outputDesc->subOutput1(), device, force, delayMs);
        muteWaitMs += setOutputDevice(outputDesc->subOutput2(), device, force, delayMs);
        return muteWaitMs;
    }
    // no need to proceed if new device is not AUDIO_DEVICE_NONE and not supported by current
    // output profile
    if ((device != AUDIO_DEVICE_NONE) &&
            ((device & outputDesc->supportedDevices()) == 0) &&
            (!force)) {
        return 0;
    }

    // filter devices according to output selected
    device = (audio_devices_t)(device & outputDesc->supportedDevices());

    audio_devices_t prevDevice = outputDesc->mDevice;

    ALOGV("setOutputDevice() prevDevice %04x", prevDevice);

    if (device != AUDIO_DEVICE_NONE) {
        outputDesc->mDevice = device;
    }
    muteWaitMs = checkDeviceMuteStrategies(outputDesc, prevDevice, delayMs);

    // Do not change the routing if:
    //      the requested device is AUDIO_DEVICE_NONE
    //      OR the requested device is the same as current device
    //  AND force is not specified
    //  AND the output is connected by a valid audio patch.
    // Doing this check here allows the caller to call setOutputDevice() without conditions
    if ((device == AUDIO_DEVICE_NONE || device == prevDevice) && !force &&
            outputDesc->mPatchHandle != 0) {
        ALOGV("setOutputDevice() setting same device %04x or null device", device);
        return muteWaitMs;
    }

    ALOGV("setOutputDevice() changing device");

    // do the routing
    if (device == AUDIO_DEVICE_NONE) {
        resetOutputDevice(outputDesc, delayMs, NULL);
    } else {
        DeviceVector deviceList = (address == NULL) ?
                mAvailableOutputDevices.getDevicesFromType(device)
                : mAvailableOutputDevices.getDevicesFromTypeAddr(device, String8(address));
        if (!deviceList.isEmpty()) {
            struct audio_patch patch;
            outputDesc->toAudioPortConfig(&patch.sources[0]);
            patch.num_sources = 1;
            patch.num_sinks = 0;
            for (size_t i = 0; i < deviceList.size() && i < AUDIO_PATCH_PORTS_MAX; i++) {
                deviceList.itemAt(i)->toAudioPortConfig(&patch.sinks[i]);
                patch.num_sinks++;
            }
            ssize_t index;
            if (patchHandle && *patchHandle != AUDIO_PATCH_HANDLE_NONE) {
                index = mAudioPatches.indexOfKey(*patchHandle);
            } else {
                index = mAudioPatches.indexOfKey(outputDesc->mPatchHandle);
            }
            sp< AudioPatch> patchDesc;
            audio_patch_handle_t afPatchHandle = AUDIO_PATCH_HANDLE_NONE;
            if (index >= 0) {
                patchDesc = mAudioPatches.valueAt(index);
                afPatchHandle = patchDesc->mAfPatchHandle;
            }

            status_t status = mpClientInterface->createAudioPatch(&patch,
                                                                   &afPatchHandle,
                                                                   delayMs);
            ALOGV("setOutputDevice() createAudioPatch returned %d patchHandle %d"
                    "num_sources %d num_sinks %d",
                                       status, afPatchHandle, patch.num_sources, patch.num_sinks);
            if (status == NO_ERROR) {
                if (index < 0) {
                    patchDesc = new AudioPatch(&patch, mUidCached);
                    addAudioPatch(patchDesc->mHandle, patchDesc);
                } else {
                    patchDesc->mPatch = patch;
                }
                patchDesc->mAfPatchHandle = afPatchHandle;
                patchDesc->mUid = mUidCached;
                if (patchHandle) {
                    *patchHandle = patchDesc->mHandle;
                }
                outputDesc->mPatchHandle = patchDesc->mHandle;
                nextAudioPortGeneration();
                mpClientInterface->onAudioPatchListUpdate();
            }
        }

        // inform all input as well
        for (size_t i = 0; i < mInputs.size(); i++) {
            const sp<AudioInputDescriptor>  inputDescriptor = mInputs.valueAt(i);
            if (!is_virtual_input_device(inputDescriptor->mDevice)) {
                AudioParameter inputCmd = AudioParameter();
                ALOGV("%s: inform input %d of device:%d", __func__,
                      inputDescriptor->mIoHandle, device);
                inputCmd.addInt(String8(AudioParameter::keyRouting),device);
                mpClientInterface->setParameters(inputDescriptor->mIoHandle,
                                                 inputCmd.toString(),
                                                 delayMs);
            }
        }
    }

    // update stream volumes according to new device
    applyStreamVolumes(outputDesc, device, delayMs);

    return muteWaitMs;
}



}; // namespace android
