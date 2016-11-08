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

#define DS_MIXER_OUTPUTS_TO_PROCESS (AUDIO_OUTPUT_FLAG_PRIMARY|AUDIO_OUTPUT_FLAG_DEEP_BUFFER)

namespace android {

/**
    Singleton instance of EffectDapController

    Lifetime of this instance is managed by AudioFlinger class. A new instance is created in
    AudioFlinger constructor and it is destroyed in AudioFlinger destructor.
*/
AudioFlinger::EffectDapController *AudioFlinger::EffectDapController::mInstance = NULL;

/**
    Constructor for EffectDapController
*/
AudioFlinger::EffectDapController::EffectDapController(const sp<AudioFlinger>& audioFlinger)
    : mAudioFlinger(audioFlinger)
{
    ALOGI("%s()", __FUNCTION__);
    mBypassed = false;
    mPassthroughBypass = false;
    mProcessedAudioTrackId = 0;
    mDapVol = 0;
    mMixerVol = 0;
    mDirectVol = 0;
    mOffloadVol = 0;
#ifdef DOLBY_AUDIO_DUMP
    mAudioDumpFile = NULL;
#endif
}

/**
    Capture the DS effect pointer on creation.

    This function is called when an effect is created in AudioFlinger. This function checks if
    the effect matches UUID of DS effect and captures pointer to the effect in mEffect.
*/
void AudioFlinger::EffectDapController::effectCreated(sp<AudioFlinger::EffectModule> &effect, const ThreadBase *thread)
{
    ALOGV("%s()", __FUNCTION__);
    if (isDapEffect(effect)) {
        ALOGI("%s() DAP effect created on thread %d", __FUNCTION__, thread->id());
        mEffect = effect;
        mBypassed = false;
        mDapVol = 0;
        mMixerVol = 0;
        mDirectVol = 0;
        mOffloadVol = 0;
// @@DOLBY_DAP_HW
#ifdef DOLBY_DAP_HW
        updateOffload(thread);
#endif
// @@DOLBY_DAP_HW_END
    }
}

/**
    Notify DS service when DS effect is suspended.
*/
void AudioFlinger::EffectDapController::effectSuspended(const sp<EffectModule> &effect, bool suspend)
{
    ALOGI("%s(suspend=%d)", __FUNCTION__, suspend);
    if (isDapEffect(effect)) {
        sendBroadcastMessage("DS_EFFECT_SUSPEND_ACTION", suspend);
    }
}

// @@DOLBY_DAP_HW
#ifdef DOLBY_DAP_HW
/**
    Set offload state for DS Effect.

    In AOSP offloaded effect is only used for offload thread. However, we have
    to ensure that offload effect is used for all outputs supported by DSP. This
    function is called whenever effect is attached to a thread.
*/
void AudioFlinger::EffectDapController::updateOffload(const ThreadBase *thread)
{
#ifndef DOLBY_DAP_HW_TEST_HAMMERHEAD // @@DOLBY_DAP_HW_TEST_HAMMERHEAD_LINE
    ALOGVV("%s()", __FUNCTION__);
    // Proceed if effect is valid and attached to the thread passed as argument.
    if (mEffect == NULL || mEffect->thread().promote()->id() != thread->id())
    {
        return;
    }
    // Enable offload if the thread is offload or the output is not connected
    // to a device requiring software DAP.
    bool offload = (thread->type() == ThreadBase::OFFLOAD)
                || ((thread->outDevice() & NO_OFFLOAD_DEVICES) == 0);
    ALOGV("%s() => %d for thread %d type %d and output %x", __FUNCTION__, offload,
        thread->id(), thread->type(), thread->outDevice());
    // Send the offload flag to DS effect
    mEffect->setOffloaded(offload, thread->id());
#endif // @@DOLBY_DAP_HW_TEST_HAMMERHEAD_LINE
}
#endif
// @@DOLBY_DAP_HW_END

#ifdef DOLBY_DAP_BYPASS_SOUND_TYPES
/**
    Return true if DS effect should be bypassed for the track.
*/
bool AudioFlinger::EffectDapController::bypassTrack(const sp<PlaybackThread::Track> &track) {
    ALOGVV("%s()", __FUNCTION__);
    return (track != NULL) && !track->isFastTrack() && (
        (track->streamType() == AUDIO_STREAM_SYSTEM) ||
        (track->streamType() == AUDIO_STREAM_RING) ||
        (track->streamType() == AUDIO_STREAM_ALARM) ||
        (track->streamType() == AUDIO_STREAM_NOTIFICATION) ||
        (track->streamType() == AUDIO_STREAM_DTMF) ||
        (track->streamType() == AUDIO_STREAM_CNT) // Special stream type used for duplicating threads.
    );
}

/**
    Bypass DS effect if any of the active tracks contain audio stream that should not be processed.
*/
void AudioFlinger::EffectDapController::checkForBypass(const SortedVector< wp<PlaybackThread::Track> > &activeTracks, audio_output_flags_t flags)
{
    ALOGVV("%s(#tracks=%d)", __FUNCTION__, activeTracks.size());
    bool bypass = false;
    if(activeTracks.size() == 0)
    {
        //activeTracks size is zero, Don't update current bypass state to false.
        return;
    }
    // Find the track that should be bypassed if not connected to fast output
    if ((flags & AUDIO_OUTPUT_FLAG_FAST) == 0) {
        for (unsigned int i=0; i < activeTracks.size(); i++) {
            sp<PlaybackThread::Track> track = activeTracks[i].promote();
            // Do not bypass if any music streams are active.
            if (track != NULL && track->streamType() == AUDIO_STREAM_MUSIC) {
                bypass = false;
                break;
            } else if (bypassTrack(track)) {
                bypass = true;
            }
        }
    }
    // Send bypass parameter to DAP when bypass state changes.
    if (bypass != mBypassed) {
        mBypassed = bypass;
        updateBypassState();
    }
}
#endif

// @@DOLBY_UDC
#ifdef DOLBY_UDC
status_t AudioFlinger::EffectDapController::setProcessedAudioState(int trackId, bool processed)
{
    ALOGD("%s(trackId=%d, processed=%d)", __FUNCTION__, trackId, processed);
    bool update = false;
    // A track has started outputting processed audio
    if (processed)
    {
        // If we did not had any other track outputting processed audio, then
        // mark this track and update bypassed state.
        if (mProcessedAudioTrackId == 0)
        {
            mProcessedAudioTrackId = trackId;
            update = true;
        }
        // If a second track has started processed audio while first track is active,
        // then return an error since multiple tracks with processed audio are not handled.
        else if (mProcessedAudioTrackId != trackId)
        {
            ALOGE("%s() Replacing processed audio track %d with %d.", __FUNCTION__, mProcessedAudioTrackId, trackId);
            return INVALID_OPERATION;
        }
    }
    // Check if the processed audio track is being destroyed
    else if (mProcessedAudioTrackId == trackId)
    {
        mProcessedAudioTrackId = 0;
        update = true;
    }

    if (update)
    {
        return updateBypassState();
    }
    return NO_ERROR;
}
#endif
// @@DOLBY_UDC_END

// @@DOLBY_DAP_PREGAIN
#ifdef DOLBY_DAP_PREGAIN
/**
    Send pregain value to DAP based on thread volumes.
*/
void AudioFlinger::EffectDapController::updatePregain(ThreadBase::type_t thread_type, audio_output_flags_t flags, uint32_t max_vol)
{
    ALOGVV("%s(thread_type = %d, flags = %d, max_vol = %u)", __FUNCTION__, thread_type, flags, max_vol);

    // Update correct thread's volume
    switch (thread_type) {
    case ThreadBase::MIXER:
        if (!(flags & DS_MIXER_OUTPUTS_TO_PROCESS) && flags != AUDIO_OUTPUT_FLAG_NONE) {
            ALOGVV("%s() mixer thread with output flags %d ignored.", __FUNCTION__, flags);
            return;
        }
        mMixerVol = max_vol;
        ALOGVV("%s() Mixer thread volume set to %u", __FUNCTION__, mMixerVol);
        break;
    case ThreadBase::DIRECT:
        mDirectVol = max_vol;
        ALOGVV("%s() Direct output thread volume set to %u", __FUNCTION__, mDirectVol);
        break;
    case ThreadBase::OFFLOAD:
        mOffloadVol = max_vol;
        ALOGVV("%s() Offload thread volume set to %u", __FUNCTION__, mOffloadVol);
        break;
    default:
        ALOGVV("%s() called with unknown thread type: %d", __FUNCTION__, thread_type);
    }
    // Update max volume of DAP
    max_vol = max(mMixerVol, max(mDirectVol, mOffloadVol));
    if (max_vol != 0 && max_vol != mDapVol) {
        status_t status = setParam(EFFECT_PARAM_SET_PREGAIN, static_cast<int>(max_vol));
        if (status == NO_ERROR) {
            mDapVol = max_vol;
            ALOGV("%s() Pregain set to %u", __FUNCTION__, max_vol);
        }
    }
}
#endif
// @@DOLBY_DAP_PREGAIN_END

// @@DOLBY_DAP_POSTGAIN
#ifdef DOLBY_DAP_POSTGAIN
/**
    Send postgain value to DAP.
*/
status_t AudioFlinger::EffectDapController::setPostgain(uint32_t max_vol)
{
    ALOGV("%s()", __FUNCTION__);
    return setParam(EFFECT_PARAM_SET_POSTGAIN, static_cast<int>(max_vol));
}
#endif
// @@DOLBY_DAP_POSTGAIN_END

// @@DOLBY_DAP_MOVE_EFFECT
#ifdef DOLBY_DAP_MOVE_EFFECT
/**
    Move DS thread from source thread to destination thread.
*/
status_t AudioFlinger::EffectDapController::moveEffect(int sessionId, PlaybackThread *srcThread, PlaybackThread *dstThread)
{
    ALOGV("%s() session %d, srcOutput %d, dstOutput %d", __FUNCTION__, sessionId, srcThread->id(), dstThread->id());
    // Move DS from a specific effect chain
    sp<EffectChain> chain = srcThread->getEffectChain_l(sessionId);
    if (chain == 0) {
        ALOGW("moveDolbyEffect() effect chain for session %d not on source thread %p",
                sessionId, srcThread);
        return INVALID_OPERATION;
    }

    sp<EffectChain> dstChain;
    sp<EffectModule> effect = chain->getEffectFromType_l(&EFFECT_SL_IID_DS);
    if (effect != 0) {
        srcThread->removeEffect_l(effect);
        status_t status = dstThread->addEffect_l(effect);
        if (status != NO_ERROR) {
            ALOGV("%s addEffect failed %d, moving back to srcThread", __FUNCTION__, status);
            srcThread->addEffect_l(effect);
            return NO_INIT;
        }

        // removeEffect_l() has stopped the effect if it was active so it must be restarted
        if (effect->state() == EffectModule::ACTIVE ||
                effect->state() == EffectModule::STOPPING) {
            effect->start();
        }
        dstChain = effect->chain().promote();
        if (dstChain == 0) {
            ALOGV("moveDolbyEffect() cannot get chain from effect %p", effect.get());
            srcThread->addEffect_l(effect);
            return NO_INIT;
        }
    }
    return NO_ERROR;
}
#endif
// @@DOLBY_DAP_MOVE_EFFECT_END

/**
    Send a broadcast intent to DS service with given action and value.
*/
bool AudioFlinger::EffectDapController::sendBroadcastMessage(const char* action, int value)
{
    ALOGV("%s(Action: %s, Value: %d)", __FUNCTION__, action, value);

    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> am = sm->getService(String16("activity"));
    if (am == NULL) {
        ALOGE("%s() couldn't find activity service!", __FUNCTION__);
        return false;
    }

    int msg[] = {0, -1, 0, -1, -1, 0, 0, 0, 0, -1, -1 };
    unsigned int i;
    Parcel data, reply;

    data.writeInterfaceToken(String16("android.app.IActivityManager"));
    data.writeStrongBinder(NULL);
    data.writeString16(String16(action));

    for (i = 0; i < sizeof(msg) / sizeof(msg[0]); i++) {
        data.writeInt32(msg[i]);
    }

    data.writeStrongBinder(NULL);
    data.writeInt32(value);
    data.writeInt32(-1);
    data.writeInt32(-1);
    data.writeInt32(-1);
    //<MTK_Added for m0 broadcast need an extra parameter
    data.writeInt32(0);
    //MTK_Added>>
    data.writeInt32(0);
    data.writeInt32(0);
    data.writeInt32(0);

    // BROADCAST_INTENT_TRANSACTION
    status_t ret = am->transact(IBinder::FIRST_CALL_TRANSACTION+13, data, &reply);

    if (ret == NO_ERROR) {
        int exceptionCode = reply.readExceptionCode();
        if (exceptionCode != 0) {
            ALOGE("%s(Action: %s) caught exception %d", __FUNCTION__, action, exceptionCode);
            return false;
        }
    } else {
        ALOGE("%s(Action: %s) received error %d", __FUNCTION__, action, ret);
        return false;
    }

    return true;
}

/**
    Send EFFECT_CMD_SET_PARAM to DS effect with given parameter id and value.
*/
status_t AudioFlinger::EffectDapController::setParam(int32_t paramId, int32_t value)
{
    ALOGVV("%s(id=%d, value=%d)", __FUNCTION__, paramId, value);
    if (mEffect == NULL) {
        return NO_INIT;
    }
    // Allocate space for SET_PARAMETER command structure
    uint8_t buffer[sizeof(effect_param_t) + sizeof(int32_t) + sizeof(int32_t)];
    uint32_t param_size = sizeof(buffer);
    effect_param_t *param = reinterpret_cast<effect_param_t*>(buffer);
    // Fill the parameter structure with all details
    param->status = NO_ERROR;
    param->psize = sizeof(int32_t);
    param->vsize = sizeof(int32_t);
    int32_t *data = reinterpret_cast<int32_t*>(param->data);
    data[0] = paramId;
    data[1] = value;
    // Create a buffer to hold reply data
    int reply = NO_ERROR;
    uint32_t reply_size = sizeof(reply);
    // Send the command to effect
    status_t status = mEffect->command(EFFECT_CMD_SET_PARAM, param_size, param, &reply_size, &reply);
    if (status == NO_ERROR) {
        if (reply_size != sizeof(reply)) {
            status = BAD_VALUE;
        }
        else if (reply != NO_ERROR) {
            status = reply;
        }
    }
    return status;
}

#ifdef DOLBY_AUDIO_DUMP
/**
    Open PCM dump file when system property is enabled.
*/
void AudioFlinger::EffectDapController::checkDumpEnable()
{
    ALOGVV("%s()", __FUNCTION__);
    // Get value of system property.
    char dolby_dump_enable_value[PROPERTY_VALUE_MAX];
    property_get(DOLBY_AF_PCM_DUMP_PROPERTY, dolby_dump_enable_value, "0");
    int dolby_dump_enable = atoi(dolby_dump_enable_value);
    // Open file if property is set
    if ((mAudioDumpFile == NULL) && (dolby_dump_enable != 0)) {
        ALOGI("DOLBY_AUDIOFLINGER_AUDIO_DUMP: Open the file for PCM dump");
        mAudioDumpFile = fopen(DOLBY_AF_PCM_DUMP_FILE, "wb");
        if (mAudioDumpFile == NULL) {
            ALOGE("DOLBY_AUDIOFLINGER_AUDIO_DUMP: Failed to open the file: %s", DOLBY_AF_PCM_DUMP_FILE);
            property_set(DOLBY_AF_PCM_DUMP_PROPERTY, "0");
        }
    }
    // Close the file when PCM dump is disabled.
    if ((mAudioDumpFile != NULL) && (dolby_dump_enable == 0)) {
            ALOGI("DOLBY_AUDIOFLINGER_AUDIO_DUMP: Close the raw file for PCM dump");
            fclose(mAudioDumpFile);
            mAudioDumpFile = NULL;
    }
}

/**
    Write audio buffer to PCM dump file.
*/
void AudioFlinger::EffectDapController::dumpBuffer(const void *buffer, ssize_t numBytes)
{
    ALOGVV("%s()", __FUNCTION__);
    if (mAudioDumpFile != NULL) {
        fwrite(buffer, 1, numBytes, mAudioDumpFile);
    }
}
#endif

#ifdef DOLBY_DAP_HW_QDSP_HAL_API
/**
    Bypass DAP on DSP for DD+ passthrough. This function will be called when DD+
    passthrough is active on the DSP.
*/
status_t AudioFlinger::EffectDapController::setPassthroughBypass(bool bypass)
{
    if (bypass != mPassthroughBypass)
    {
        mPassthroughBypass = bypass;
        return updateBypassState();
    }
    return NO_ERROR;
}
#endif

/**
    Send bypass command to DAP based on the bypass state and processed audio state
*/
status_t AudioFlinger::EffectDapController::updateBypassState()
{
    bool bypass = mBypassed || (mProcessedAudioTrackId != 0) || mPassthroughBypass;
    ALOGD("%s(bypass=%d)", __FUNCTION__, bypass);
    // TODO: Should we notify DS Service that effect is bypassed?
    return setParam(EFFECT_PARAM_SET_BYPASS, bypass);
}

}
