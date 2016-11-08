#include "AudioALSACaptureDataProviderModemDai.h"

#include <pthread.h>

#include <linux/rtpm_prio.h>
#include <sys/prctl.h>
#include "AudioALSADeviceParser.h"
#include "AudioALSADriverUtility.h"
#include "AudioType.h"


#define LOG_TAG "AudioALSACaptureDataProviderModemDai"

namespace android
{


/*==============================================================================
 *                     Constant
 *============================================================================*/

//static const uint32_t kReadBufferSize = 0x2000; // 8k
static const uint32_t kReadBufferSize = 0x400; // 1k

/*==============================================================================
 *                     Implementation
 *============================================================================*/

AudioALSACaptureDataProviderModemDai *AudioALSACaptureDataProviderModemDai::mAudioALSACaptureDataProviderModemDai = NULL;
AudioALSACaptureDataProviderModemDai *AudioALSACaptureDataProviderModemDai::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACaptureDataProviderModemDai == NULL)
    {
        mAudioALSACaptureDataProviderModemDai = new AudioALSACaptureDataProviderModemDai();
    }
    ASSERT(mAudioALSACaptureDataProviderModemDai != NULL);
    return mAudioALSACaptureDataProviderModemDai;
}

AudioALSACaptureDataProviderModemDai::AudioALSACaptureDataProviderModemDai()
{
    ALOGD("%s()", __FUNCTION__);

    // TODO(Harvey): query this
    mConfig.channels = 1;
    mConfig.rate = 16000;

    mConfig.period_size = 512; // 512 * 2(Byte) * 1(channel)
    mConfig.period_count = 8;
    mConfig.format = PCM_FORMAT_S16_LE;

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;

    mCaptureDataProviderType = CAPTURE_PROVIDER_NORMAL;
}

AudioALSACaptureDataProviderModemDai::~AudioALSACaptureDataProviderModemDai()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureDataProviderModemDai::open()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class attach
    AudioAutoTimeoutLock _l(mEnableLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    ASSERT(mEnable == false);

    mStreamAttributeSource.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeSource.audio_channel_mask = AUDIO_CHANNEL_IN_MONO;
    mStreamAttributeSource.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeSource.audio_channel_mask);
    mStreamAttributeSource.sample_rate = 16000;

    OpenPCMDump(LOG_TAG);

    // enable pcm
    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmModomDaiCapture);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmModomDaiCapture);
    ALOGD("AudioALSACaptureDataProviderModemDai::open() pcmindex = %d", pcmindex);
    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_IN);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
    int buffer_size = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    ALOGD("buffersizemax = %d chanel %d, rate %d, period %d, period_count %d", buffer_size, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count);
    pcm_params_free(params);

    openPcmDriver(pcmindex);

    // create reading thread
    mEnable = true;
    int ret = pthread_create(&hReadThread, NULL, AudioALSACaptureDataProviderModemDai::readThread, (void *)this);
    if (ret != 0)
    {
        ALOGE("%s() create thread fail!!", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    return NO_ERROR;
}

status_t AudioALSACaptureDataProviderModemDai::close()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class detach

    mEnable = false;
    AudioAutoTimeoutLock _l(mEnableLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    ClosePCMDump();

    closePcmDriver();

    return NO_ERROR;
}


void *AudioALSACaptureDataProviderModemDai::readThread(void *arg)
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
        sched_p.sched_priority = RTPM_PRIO_AUDIO_CCCI_THREAD;
        sched_getparam(0, &sched_p);
        ALOGD("sched_setscheduler ok, priority: %d", sched_p.sched_priority);
    }
#endif
    ALOGD("+%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());

    status_t retval = NO_ERROR;
    AudioALSACaptureDataProviderModemDai *pDataProvider = static_cast<AudioALSACaptureDataProviderModemDai *>(arg);

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
    return NULL;
}

} // end of namespace android
