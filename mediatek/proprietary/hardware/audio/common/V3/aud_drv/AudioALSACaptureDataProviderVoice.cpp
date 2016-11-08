#include "AudioALSACaptureDataProviderVoice.h"

#include <pthread.h>

#include <linux/rtpm_prio.h>
#include <sys/prctl.h>

#include "AudioType.h"

#include "SpeechDriverFactory.h"

#define LOG_TAG "AudioALSACaptureDataProviderVoice"

namespace android
{


/*==============================================================================
 *                     Constant
 *============================================================================*/

static const uint32_t kReadBufferSize = 0x2000; // 8k


/*==============================================================================
 *                     Implementation
 *============================================================================*/

AudioALSACaptureDataProviderVoice *AudioALSACaptureDataProviderVoice::mAudioALSACaptureDataProviderVoice = NULL;
AudioALSACaptureDataProviderVoice *AudioALSACaptureDataProviderVoice::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACaptureDataProviderVoice == NULL)
    {
        mAudioALSACaptureDataProviderVoice = new AudioALSACaptureDataProviderVoice();
    }
    ASSERT(mAudioALSACaptureDataProviderVoice != NULL);
    return mAudioALSACaptureDataProviderVoice;
}

AudioALSACaptureDataProviderVoice::AudioALSACaptureDataProviderVoice()
{
    ALOGD("%s()", __FUNCTION__);
    mCaptureDataProviderType = CAPTURE_PROVIDER_FM_RADIO;
}

AudioALSACaptureDataProviderVoice::~AudioALSACaptureDataProviderVoice()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureDataProviderVoice::open()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class attach
    AudioAutoTimeoutLock _l(mEnableLock);

    ASSERT(mEnable == false);

    // config attribute (will used in client SRC/Enh/... later)
    SpeechDriverInterface *pSpeechDriver = SpeechDriverFactory::GetInstance()->GetSpeechDriver();

    mStreamAttributeSource.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeSource.num_channels = pSpeechDriver->GetRecordChannelNumber();
    mStreamAttributeSource.audio_channel_mask = (mStreamAttributeSource.num_channels == 1) ? AUDIO_CHANNEL_IN_MONO : AUDIO_CHANNEL_IN_STEREO;
    mStreamAttributeSource.sample_rate = pSpeechDriver->GetRecordSampleRate();

    mEnable = true;

    OpenPCMDump(LOG_TAG);

    return SpeechDriverFactory::GetInstance()->GetSpeechDriver()->RecordOn();
}

status_t AudioALSACaptureDataProviderVoice::close()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class detach

    mEnable = false;
    AudioAutoTimeoutLock _l(mEnableLock);

    ClosePCMDump();
    return SpeechDriverFactory::GetInstance()->GetSpeechDriver()->RecordOff();
}

status_t AudioALSACaptureDataProviderVoice::provideModemRecordDataToProvider(RingBuf modem_record_buf)
{
    status_t retval = mEnableLock.lock_timeout(300);
    ASSERT(retval == NO_ERROR);

    if (mEnable == false)
    {
        ALOGW("%s(), mEnable == false, return", __FUNCTION__);
        mEnableLock.unlock();
        return NO_INIT;
    }

    mPcmReadBuf = modem_record_buf;
    ALOGD("%s(), pBufBase(%p), bufLen(%d), pRead(%p), pWrite(%p)",
          __FUNCTION__,
          mPcmReadBuf.pBufBase,
          mPcmReadBuf.bufLen,
          mPcmReadBuf.pRead,
          mPcmReadBuf.pWrite);

    mEnableLock.unlock();

    provideCaptureDataToAllClients(mOpenIndex);

    return NO_ERROR;
}


} // end of namespace android
