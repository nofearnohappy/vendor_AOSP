#include "AudioALSACaptureDataProviderTDM.h"

#include <pthread.h>

#include <linux/rtpm_prio.h>
#include <sys/prctl.h>

#include "AudioType.h"
#include "AudioALSADriverUtility.h"

#define LOG_TAG "AudioALSACaptureDataProviderTDM"

namespace android
{


/*==============================================================================
 *                     Constant
 *============================================================================*/

static const uint32_t kReadBufferSize = 0x2000; // 8k


/*==============================================================================
 *                     Implementation
 *============================================================================*/

AudioALSACaptureDataProviderTDM *AudioALSACaptureDataProviderTDM::mAudioALSACaptureDataProviderTDM = NULL;
AudioALSACaptureDataProviderTDM *AudioALSACaptureDataProviderTDM::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACaptureDataProviderTDM == NULL)
    {
        mAudioALSACaptureDataProviderTDM = new AudioALSACaptureDataProviderTDM();
    }
    ASSERT(mAudioALSACaptureDataProviderTDM != NULL);
    return mAudioALSACaptureDataProviderTDM;
}

AudioALSACaptureDataProviderTDM::AudioALSACaptureDataProviderTDM()
{
    ALOGD("%s()", __FUNCTION__);

    mConfig.channels = 2;
    mConfig.rate = 44100;

    // Buffer size: 2048(period_size) * 2(ch) * 2(byte) * 8(period_count) = 64 kb
    mConfig.period_size = 2048;
    mConfig.period_count = 8;
    mConfig.format = PCM_FORMAT_S16_LE;

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;

    mCaptureDataProviderType = CAPTURE_PROVIDER_TDM_RECORD;
}

AudioALSACaptureDataProviderTDM::~AudioALSACaptureDataProviderTDM()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureDataProviderTDM::open()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class attach
    AudioAutoTimeoutLock _l(mEnableLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    ASSERT(mEnable == false);

    // config attribute (will used in client SRC/Enh/... later) // TODO(Harvey): query this
    mStreamAttributeSource.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeSource.audio_channel_mask = AUDIO_CHANNEL_IN_STEREO;
    mStreamAttributeSource.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeSource.audio_channel_mask);
    mStreamAttributeSource.sample_rate = 44100;

    OpenPCMDump(LOG_TAG);

    // enable pcm
    ASSERT(mPcm == NULL);
    int pcmIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmTDMLoopback);
    int cardIdx = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmTDMLoopback);
    mPcm = pcm_open(cardIdx, pcmIdx, PCM_IN, &mConfig);

    ASSERT(mPcm != NULL && pcm_is_ready(mPcm) == true);
    ALOGV("%s(), mPcm = %p", __FUNCTION__, mPcm);

    pcm_start(mPcm);

    // create reading thread
    mEnable = true;
    int ret = pthread_create(&hReadThread, NULL, AudioALSACaptureDataProviderTDM::readThread, (void *)this);
    if (ret != 0)
    {
        ALOGE("%s() create thread fail!!", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    return NO_ERROR;
}

status_t AudioALSACaptureDataProviderTDM::close()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class detach

    mEnable = false;
    AudioAutoTimeoutLock _l(mEnableLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    ClosePCMDump();

    pcm_stop(mPcm);
    pcm_close(mPcm);
    mPcm = NULL;

    return NO_ERROR;
}


void *AudioALSACaptureDataProviderTDM::readThread(void *arg)
{
    pthread_detach(pthread_self());

    prctl(PR_SET_NAME, (unsigned long)__FUNCTION__, 0, 0, 0);

#ifdef MTK_AUDIO_ADJUST_PRIORITY
    // force to set priority
    struct sched_param sched_p;
    sched_getparam(0, &sched_p);
    sched_p.sched_priority = RTPM_PRIO_AUDIO_RECORD + 1;
    if (0 != sched_setscheduler(0, SCHED_RR, &sched_p))
    {
        ALOGE("[%s] failed, errno: %d", __FUNCTION__, errno);
    }
    else
    {
        sched_p.sched_priority = RTPM_PRIO_AUDIO_RECORD + 1;
        sched_getparam(0, &sched_p);
        ALOGD("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
    }
#endif
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());



    status_t retval = NO_ERROR;
    AudioALSACaptureDataProviderTDM *pDataProvider = static_cast<AudioALSACaptureDataProviderTDM *>(arg);

    uint32_t open_index = pDataProvider->mOpenIndex;

    // read raw data from alsa driver
    char linear_buffer[kReadBufferSize];
    while (pDataProvider->mEnable == true)
    {
        if (open_index != pDataProvider->mOpenIndex)
        {
            ALOGD("%s(), open_index(%d) != mOpenIndex(%d), return", __FUNCTION__, open_index, pDataProvider->mOpenIndex);
            break;
        }

        retval = pDataProvider->mEnableLock.lock_timeout(300);
        ASSERT(retval == NO_ERROR);
        if (pDataProvider->mEnable == false)
        {
            pDataProvider->mEnableLock.unlock();
            break;
        }

        ASSERT(pDataProvider->mPcm != NULL);
        int retval = pcm_read(pDataProvider->mPcm, linear_buffer, kReadBufferSize);
        if (retval != 0)
        {
            ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
        }

        // use ringbuf format to save buffer info
        pDataProvider->mPcmReadBuf.pBufBase = linear_buffer;
        pDataProvider->mPcmReadBuf.bufLen   = kReadBufferSize + 1; // +1: avoid pRead == pWrite
        pDataProvider->mPcmReadBuf.pRead    = linear_buffer;
        pDataProvider->mPcmReadBuf.pWrite   = linear_buffer + kReadBufferSize;
        pDataProvider->mEnableLock.unlock();

        pDataProvider->provideCaptureDataToAllClients(open_index);
    }

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}

} // end of namespace android
