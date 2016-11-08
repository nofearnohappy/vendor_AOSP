#include "AudioALSASampleRateController.h"

#include "AudioLock.h"


#define LOG_TAG "AudioALSASampleRateController"

namespace android
{

AudioALSASampleRateController *AudioALSASampleRateController::mAudioALSASampleRateController = NULL;
AudioALSASampleRateController *AudioALSASampleRateController::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSASampleRateController == NULL)
    {
        mAudioALSASampleRateController = new AudioALSASampleRateController();
    }
    ASSERT(mAudioALSASampleRateController != NULL);
    return mAudioALSASampleRateController;
}


AudioALSASampleRateController::AudioALSASampleRateController() :
    mPrimaryStreamOutSampleRate(44100)
{
    ALOGD("%s()", __FUNCTION__);
    memset(&mScenarioReference, 0, sizeof(mScenarioReference));
}


AudioALSASampleRateController::~AudioALSASampleRateController()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSASampleRateController::setPrimaryStreamOutSampleRate(const uint32_t sample_rate)
{
    AudioAutoTimeoutLock _l(mLock);

    ALOGD("+%s(), mPrimaryStreamOutSampleRate: %u => %u", __FUNCTION__, mPrimaryStreamOutSampleRate, sample_rate);

    if (hasActiveScenario())
    {
        ALOGW("-%s() some other scenatio is active", __FUNCTION__);
        return INVALID_OPERATION;
    }
    else if (sample_rate == mPrimaryStreamOutSampleRate)
    {
        ALOGW("-%s(), sample_rate == mPrimaryStreamOutSampleRate, return", __FUNCTION__);
        return ALREADY_EXISTS;
    }


    mPrimaryStreamOutSampleRate = sample_rate;


    ALOGD("-%s(), mPrimaryStreamOutSampleRate: %u", __FUNCTION__, mPrimaryStreamOutSampleRate);
    return NO_ERROR;
}


uint32_t AudioALSASampleRateController::getPrimaryStreamOutSampleRate()
{
    AudioAutoTimeoutLock _l(mLock);
    return mPrimaryStreamOutSampleRate;
}


void AudioALSASampleRateController::setScenarioStatus(const playback_scenario_mask_t playback_scenario_mask)
{
    AudioAutoTimeoutLock _l(mLock);

    mScenarioReference[playback_scenario_mask]++;
}

void AudioALSASampleRateController::resetScenarioStatus(const playback_scenario_mask_t playback_scenario_mask)
{
    AudioAutoTimeoutLock _l(mLock);

    mScenarioReference[playback_scenario_mask]--;

    if (mScenarioReference[playback_scenario_mask] < 0) {
        ALOGW("%s unexpected operation for scenario %d", __FUNCTION__, playback_scenario_mask);
        mScenarioReference[playback_scenario_mask] = 0;
    }
}

bool AudioALSASampleRateController::hasActiveScenario()
{
    for (int i = 0; i < PLAYBACK_SCENARIO_COUNT; i++)
    {
        if (mScenarioReference[i] > 0) {
            ALOGV("%s scenario %d is active", __FUNCTION__, i);
            return true;
        }
    }

    return false;
}

} // end of namespace android
