#include "AudioALSACaptureDataProviderBTSCO.h"

#include <pthread.h>

#include <linux/rtpm_prio.h>
#include <sys/prctl.h>

#include "AudioALSADriverUtility.h"
#include "AudioType.h"

#include "WCNChipController.h"

#define LOG_TAG "AudioALSACaptureDataProviderBTSCO"

#define calc_time_diff(x,y) ((x.tv_sec - y.tv_sec )+ (double)( x.tv_nsec - y.tv_nsec ) / (double)1000000000)

namespace android
{


/*==============================================================================
 *                     Constant
 *============================================================================*/

static const uint32_t kReadBufferSize = 0x400; // 1k


/*==============================================================================
 *                     Implementation
 *============================================================================*/

AudioALSACaptureDataProviderBTSCO *AudioALSACaptureDataProviderBTSCO::mAudioALSACaptureDataProviderBTSCO = NULL;
AudioALSACaptureDataProviderBTSCO *AudioALSACaptureDataProviderBTSCO::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACaptureDataProviderBTSCO == NULL)
    {
        mAudioALSACaptureDataProviderBTSCO = new AudioALSACaptureDataProviderBTSCO();
    }
    ASSERT(mAudioALSACaptureDataProviderBTSCO != NULL);
    return mAudioALSACaptureDataProviderBTSCO;
}

AudioALSACaptureDataProviderBTSCO::AudioALSACaptureDataProviderBTSCO() :
    mWCNChipController(WCNChipController::GetInstance())
{
    ALOGD("%s()", __FUNCTION__);

    mCaptureDataProviderType = CAPTURE_PROVIDER_BT_SCO;
}

AudioALSACaptureDataProviderBTSCO::~AudioALSACaptureDataProviderBTSCO()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureDataProviderBTSCO::open()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class attach
    AudioAutoTimeoutLock _l(mEnableLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    ASSERT(mEnable == false);

    // config attribute (will used in client SRC/Enh/... later) // TODO(Harvey): query this
    mStreamAttributeSource.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeSource.audio_channel_mask = AUDIO_CHANNEL_IN_MONO;
    mStreamAttributeSource.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeSource.audio_channel_mask);
    mStreamAttributeSource.sample_rate = mWCNChipController->GetBTCurrentSamplingRateNumber();


    // pcm config
    mConfig.channels = mStreamAttributeSource.num_channels;
    mConfig.rate = mStreamAttributeSource.sample_rate;

    // Buffer size: 2048(period_size) * 1(ch) * 2(byte) * 4(period_count) = 16 kb
    mConfig.period_size = 2048;
    mConfig.period_count = 4;
    mConfig.format = PCM_FORMAT_S16_LE;

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;

    ALOGD("%s(), audio_format = %d, audio_channel_mask=%x, num_channels=%d, sample_rate=%d", __FUNCTION__,
          mStreamAttributeSource.audio_format, mStreamAttributeSource.audio_channel_mask, mStreamAttributeSource.num_channels, mStreamAttributeSource.sample_rate);

    ALOGD("%s(), format = %d, channels=%d, rate=%d", __FUNCTION__,
          mConfig.format, mConfig.channels, mConfig.rate);

    OpenPCMDump(LOG_TAG);

    // enable pcm
    ASSERT(mPcm == NULL);
    int pcmIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVOIPCallBTCapture);
    int cardIdx = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmVOIPCallBTCapture);
    mPcm = pcm_open(cardIdx, pcmIdx, PCM_IN | PCM_MONOTONIC, &mConfig);
    ASSERT(mPcm != NULL && pcm_is_ready(mPcm) == true);
    ALOGV("%s(), mPcm = %p", __FUNCTION__, mPcm);

    pcm_start(mPcm);

    // create reading thread
    mEnable = true;
    int ret = pthread_create(&hReadThread, NULL, AudioALSACaptureDataProviderBTSCO::readThread, (void *)this);
    if (ret != 0)
    {
        ALOGE("%s() create thread fail!!", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    return NO_ERROR;
}

status_t AudioALSACaptureDataProviderBTSCO::close()
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

void *AudioALSACaptureDataProviderBTSCO::readThread(void *arg)
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
    AudioALSACaptureDataProviderBTSCO *pDataProvider = static_cast<AudioALSACaptureDataProviderBTSCO *>(arg);

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

        clock_gettime(CLOCK_REALTIME, &pDataProvider->mNewtime);
        pDataProvider->timerec[0] = calc_time_diff(pDataProvider->mNewtime, pDataProvider->mOldtime);
        pDataProvider->mOldtime = pDataProvider->mNewtime;

        int retval = pcm_read(pDataProvider->mPcm, linear_buffer, kReadBufferSize);
        if (retval != 0)
        {
            ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
        }

        clock_gettime(CLOCK_REALTIME, &pDataProvider->mNewtime);
        pDataProvider->timerec[1] = calc_time_diff(pDataProvider->mNewtime, pDataProvider->mOldtime);
        pDataProvider->mOldtime = pDataProvider->mNewtime;


        //struct timespec tempTimeStamp;
        pDataProvider->GetCaptureTimeStamp(&pDataProvider->mStreamAttributeSource.Time_Info, kReadBufferSize);

        // use ringbuf format to save buffer info
        pDataProvider->mPcmReadBuf.pBufBase = linear_buffer;
        pDataProvider->mPcmReadBuf.bufLen   = kReadBufferSize + 1; // +1: avoid pRead == pWrite
        pDataProvider->mPcmReadBuf.pRead    = linear_buffer;
        pDataProvider->mPcmReadBuf.pWrite   = linear_buffer + kReadBufferSize;
        pDataProvider->mEnableLock.unlock();

        pDataProvider->provideCaptureDataToAllClients(open_index);

        clock_gettime(CLOCK_REALTIME, &pDataProvider->mNewtime);
        pDataProvider->timerec[2] = calc_time_diff(pDataProvider->mNewtime, pDataProvider->mOldtime);
        pDataProvider->mOldtime = pDataProvider->mNewtime;
        ALOGD("%s, latency_in_us,%1.6lf,%1.6lf,%1.6lf", __FUNCTION__, pDataProvider->timerec[0], pDataProvider->timerec[1], pDataProvider->timerec[2]);
    }

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}

} // end of namespace android
