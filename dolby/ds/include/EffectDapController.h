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

#ifndef INCLUDING_FROM_AUDIOFLINGER_H
    #error This header file should only be included from AudioFlinger.h
#endif

#ifndef DOLBY_EFFECT_DAP_CONTROLLER_H_
#define DOLBY_EFFECT_DAP_CONTROLLER_H_

#define max(x, y) (((x) > (y)) ? (x) : (y))

//
// Audio dump usage: Set DOLBY_AUDIO_DUMP=true before build.
// Set property dolby.debug.af_dump to 1 to start dump. Change it back
// to 0 to stop dump. This can be accomplished by following commands:
//      adb root
//      adb remount
//      adb shell setprop dolby.debug.af_dump 1
//      adb shell setprop dolby.debug.af_dump 0
//
// When dump is restarted next time, existing files will be overwritten!
// Be sure to pull /data/dolby/DsMixerOutput.pcm before starting a new dump.
#define DOLBY_AF_PCM_DUMP_PROPERTY "dolby.debug.af_dump"
#define DOLBY_AF_PCM_DUMP_FILE "/data/dolby/DsMixerOutput.pcm"

class EffectDapController
{
public:
    static EffectDapController *instance()
    { return mInstance; }

    static bool isDapEffect(const sp<EffectModule> &effect)
    { return (memcmp(&effect->desc().type, &EFFECT_SL_IID_DS, sizeof(effect_uuid_t)) == 0); }

    void effectCreated(sp<EffectModule> &effect, const ThreadBase *thread);
    void effectSuspended(const sp<EffectModule> &effect, bool suspend);
// @@DOLBY_DAP_HW
#ifdef DOLBY_DAP_HW
    void updateOffload(const ThreadBase *thread);
#endif
// @@DOLBY_DAP_HW_END
#ifdef DOLBY_DAP_BYPASS_SOUND_TYPES
    void checkForBypass(const SortedVector< wp<PlaybackThread::Track> > &tracks, audio_output_flags_t flags);
#endif
// @@DOLBY_UDC
#ifdef DOLBY_UDC
    status_t setProcessedAudioState(int trackId, bool processed);
#endif
// @@DOLBY_UDC_END
// @@DOLBY_DAP_PREGAIN
#ifdef DOLBY_DAP_PREGAIN
    void updatePregain(ThreadBase::type_t thread_type, audio_output_flags_t flags, uint32_t max_vol);
#endif
// @@DOLBY_DAP_PREGAIN_END
// @@DOLBY_DAP_POSTGAIN
#ifdef DOLBY_DAP_POSTGAIN
    status_t setPostgain(uint32_t max_vol);
#endif
// @@DOLBY_DAP_POSTGAIN_END
// @@DOLBY_DAP_MOVE_EFFECT
#ifdef DOLBY_DAP_MOVE_EFFECT
    status_t moveEffect(int sessionId, PlaybackThread *srcThread, PlaybackThread *dstThread);
#endif
// @@DOLBY_DAP_MOVE_EFFECT_END
#ifdef DOLBY_AUDIO_DUMP
    void checkDumpEnable();
    void dumpBuffer(const void *buffer, ssize_t numBytes);
#endif

#ifdef DOLBY_DAP_HW_QDSP_HAL_API
    status_t setPassthroughBypass(bool bypass);
#endif
private:
    EffectDapController(const sp<AudioFlinger>& audioFlinger);
    EffectDapController(const EffectDapController&);
    EffectDapController& operator=(const EffectDapController&);

    static EffectDapController *mInstance;
    // This will allow AudioFlinger to instantiate this class.
    friend class AudioFlinger;

protected:
    bool sendBroadcastMessage(const char* action, int value);
    status_t setParam(int32_t paramId, int32_t value);
    status_t updateBypassState();
#ifdef DOLBY_DAP_BYPASS_SOUND_TYPES
    bool bypassTrack(const sp<PlaybackThread::Track> &track);
#endif

    const sp<AudioFlinger> mAudioFlinger;
    sp<EffectModule> mEffect;
    bool mBypassed;
    bool mPassthroughBypass;
    int mProcessedAudioTrackId;
    uint32_t mDapVol;
    uint32_t mMixerVol;
    uint32_t mDirectVol;
    uint32_t mOffloadVol;

#ifdef DOLBY_AUDIO_DUMP
    FILE* mAudioDumpFile;
#endif
};

#endif//DOLBY_EFFECT_DAP_CONTROLLER_H_
