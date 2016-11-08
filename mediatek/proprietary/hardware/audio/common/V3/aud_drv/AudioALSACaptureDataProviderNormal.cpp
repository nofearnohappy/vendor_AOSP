#include "AudioALSACaptureDataProviderNormal.h"

#include <pthread.h>

#include <linux/rtpm_prio.h>
#include <sys/prctl.h>

#include "AudioALSADriverUtility.h"
#include "AudioType.h"

#if !defined(MTK_BASIC_PACKAGE)
#include <audio_utils/pulse.h>
#endif

#define LOG_TAG "AudioALSACaptureDataProviderNormal"

#define calc_time_diff(x,y) ((x.tv_sec - y.tv_sec )+ (double)( x.tv_nsec - y.tv_nsec ) / (double)1000000000)

namespace android
{


/*==============================================================================
 *                     Constant
 *============================================================================*/

//static const uint32_t kReadBufferSize = 0x2000; // 8k
#ifdef RECORD_INPUT_24BITS // 24bit record
static const uint32_t kReadBufferSize_lowlatency = (((uint32_t)(48*5*2*4)) & 0xFFFFFFC0); // (UL)48K\5ms data\stereo\4byte\(Align64byte)
static const uint32_t kReadBufferSize_normal = (((uint32_t)(48*20*2*4)) & 0xFFFFFFC0); // (UL)48K\20ms data\stereo\4byte\(Align64byte)
#else
static const uint32_t kReadBufferSize_lowlatency = (((uint32_t)(48*5*2*2)) & 0xFFFFFFC0); // (UL)48K\5ms data\stereo\2byte\(Align64byte)
static const uint32_t kReadBufferSize_normal = (((uint32_t)(48*20*2*2)) & 0xFFFFFFC0); // (UL)48K\20ms data\stereo\2byte\(Align64byte)
#endif

static const uint32_t kDCRReadBufferSize = 0x2EE00; //48K\stereo\1s data , calculate 1time/sec

static uint32_t kReadBufferSize = kReadBufferSize_normal;

//static FILE *pDCCalFile = NULL;


/*==============================================================================
 *                     Implementation
 *============================================================================*/

AudioALSACaptureDataProviderNormal *AudioALSACaptureDataProviderNormal::mAudioALSACaptureDataProviderNormal = NULL;
AudioALSACaptureDataProviderNormal *AudioALSACaptureDataProviderNormal::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACaptureDataProviderNormal == NULL)
    {
        mAudioALSACaptureDataProviderNormal = new AudioALSACaptureDataProviderNormal();
    }
    ASSERT(mAudioALSACaptureDataProviderNormal != NULL);
    return mAudioALSACaptureDataProviderNormal;
}

AudioALSACaptureDataProviderNormal::AudioALSACaptureDataProviderNormal()
{
    ALOGD("%s()", __FUNCTION__);
}

AudioALSACaptureDataProviderNormal::~AudioALSACaptureDataProviderNormal()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureDataProviderNormal::open()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class attach
    AudioAutoTimeoutLock _l(mEnableLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    ASSERT(mEnable == false);

    int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmUl1Capture);
    int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmUl1Capture);
    ALOGD("%s cardindex = %d  pcmindex = %d", __FUNCTION__, cardindex, pcmindex);

    struct pcm_params *params;
    params = pcm_params_get(cardindex, pcmindex,  PCM_IN);
    if (params == NULL)
    {
        ALOGD("Device does not exist.\n");
    }
    unsigned int buffersizemax = pcm_params_get_max(params, PCM_PARAM_BUFFER_BYTES);
    ALOGD("buffersizemax = %d", buffersizemax);
    pcm_params_free(params);

    // TODO(Harvey): query this
    mConfig.channels = 2;
    mConfig.rate = 48000;
    #ifdef MTK_DMIC_SR_LIMIT
    mConfig.rate = 32000;
    #endif


    // Buffer size: 2048(period_size) * 2(ch) * 2(byte) * 8(period_count) = 64 kb
#ifdef RECORD_INPUT_24BITS // 24bit record
#ifdef UPLINK_LOW_LATENCY
    if (HasLowLatencyCapture())
    {
        mConfig.period_count = 2 * kReadBufferSize_normal / kReadBufferSize_lowlatency;
        //period size will impact the interrupt interval
        // audio low latency param - record - interrupt rate
        mConfig.period_size = (kReadBufferSize_lowlatency / mConfig.channels) / 4;  //interrupt 5ms
        mConfig.format = PCM_FORMAT_S32_LE;
        // audio low latency param - record - read from kernel
        kReadBufferSize = kReadBufferSize_lowlatency;
    }
    else
    {
        mConfig.period_count = 2;
        //period size will impact the interrupt interval
        mConfig.period_size = (kReadBufferSize_normal / mConfig.channels) / 4;  //interrupt 20ms
        mConfig.format = PCM_FORMAT_S32_LE;
        kReadBufferSize = kReadBufferSize_normal;
    }
#else
    mConfig.period_count = 2;
    //period size will impact the interrupt interval
    mConfig.period_size = (buffersizemax / mConfig.channels) / 4 / mConfig.period_count;
    mConfig.format = PCM_FORMAT_S32_LE;
#endif
#else
#ifdef UPLINK_LOW_LATENCY
    if (HasLowLatencyCapture())
    {
        mConfig.period_count = 2;
        //period size will impact the interrupt interval
        mConfig.period_size = (kReadBufferSize_lowlatency / mConfig.channels) / 2;  //interrupt 5ms
        mConfig.format = PCM_FORMAT_S16_LE;
        kReadBufferSize = kReadBufferSize_lowlatency;
    }
    else
    {
        mConfig.period_count = 2;
        //period size will impact the interrupt interval
        mConfig.period_size = (kReadBufferSize_normal / mConfig.channels) / 2;  //interrupt 20ms
        mConfig.format = PCM_FORMAT_S16_LE;
        kReadBufferSize = kReadBufferSize_normal;
    }
#else
    mConfig.period_count = 2;
    //period size will impact the interrupt interval
    mConfig.period_size = (buffersizemax / mConfig.channels) / 2 / mConfig.period_count;
    mConfig.format = PCM_FORMAT_S16_LE;
#endif
#endif

    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;

    mCaptureDataProviderType = CAPTURE_PROVIDER_NORMAL;

    mCaptureDropSize = 0;

#ifdef MTK_VOW_DCCALI_SUPPORT
    //DC cal
    memset((void *)&mDCCalBuffer, 0, sizeof(mDCCalBuffer));
    mDCCalEnable = false;
    mDCCalBufferFull = false;
    mDCCalDumpFile = NULL;
#endif

    // config attribute (will used in client SRC/Enh/... later) // TODO(Harvey): query this
    mStreamAttributeSource.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeSource.audio_channel_mask = AUDIO_CHANNEL_IN_STEREO;
    mStreamAttributeSource.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeSource.audio_channel_mask);
    mStreamAttributeSource.sample_rate = 48000;
    #ifdef MTK_DMIC_SR_LIMIT
    mStreamAttributeSource.sample_rate = 32000;
    #endif

#ifndef UPLINK_LOW_LATENCY  //no need to drop data
#ifdef RECORD_INPUT_24BITS // 24bit record
    mCaptureDropSize = ((mStreamAttributeSource.sample_rate * CAPTURE_DROP_MS << 3) / 1000);    //32bit, drop data which get from kernel
#else
    mCaptureDropSize = ((mStreamAttributeSource.sample_rate * CAPTURE_DROP_MS << 2) / 1000);    //16bit
#endif
#endif

    ALOGD("%s(), mCaptureDropSize=%d, CAPTURE_DROP_MS=%d", __FUNCTION__, mCaptureDropSize, CAPTURE_DROP_MS);
    ALOGD("%s(), period_count=%d, period_size=%d", __FUNCTION__, mConfig.period_count, mConfig.period_size);


    OpenPCMDump(LOG_TAG);

    // enable pcm
    ASSERT(mPcm == NULL);
    int pcmIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmUl1Capture);
    int cardIdx = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmUl1Capture);
    mPcm = pcm_open(cardIdx, pcmIdx, PCM_IN | PCM_MONOTONIC, &mConfig);
    ASSERT(mPcm != NULL && pcm_is_ready(mPcm) == true);
    ALOGV("%s(), mPcm = %p", __FUNCTION__, mPcm);

    pcm_start(mPcm);

    // create reading thread
    mEnable = true;
    int ret = pthread_create(&hReadThread, NULL, AudioALSACaptureDataProviderNormal::readThread, (void *)this);
    if (ret != 0)
    {
        ALOGE("%s() create thread fail!!", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

#ifdef MTK_VOW_DCCALI_SUPPORT
    //create DC cal thread
    ret = pthread_create(&hDCCalThread, NULL, AudioALSACaptureDataProviderNormal::DCCalThread, (void *)this);
    if (ret != 0)
    {
        ALOGE("%s() create DCCal thread fail!!", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    ret = pthread_mutex_init(&mDCCal_Mutex, NULL);
    if (ret != 0)
    {
        ALOGE("%s, Failed to initialize mDCCal_Mutex!", __FUNCTION__);
    }

    ret = pthread_cond_init(&mDCCal_Cond, NULL);
    if (ret != 0)
    {
        ALOGE("%s, Failed to initialize mDCCal_Cond!", __FUNCTION__);
    }

    mDCCalEnable = true;

    mDCCalBuffer.pBufBase = new char[kDCRReadBufferSize];
    mDCCalBuffer.bufLen   = kDCRReadBufferSize;
    mDCCalBuffer.pRead    = mDCCalBuffer.pBufBase;
    mDCCalBuffer.pWrite   = mDCCalBuffer.pBufBase;
    ASSERT(mDCCalBuffer.pBufBase != NULL);

    AudioAutoTimeoutLock _lDC(mDCCalEnableLock);
    mDCCalEnable = true;

    OpenDCCalDump();
#if 0
    pDCCalFile = fopen("/sdcard/mtklog/DCCalFile.pcm", "wb");
    if (pDCCalFile == NULL)
    {
        ALOGW("%s(), create pDCCalFile fail ", __FUNCTION__);
    }
#endif
    //create DC cal ---
#endif

    return NO_ERROR;
}

status_t AudioALSACaptureDataProviderNormal::close()
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

#ifdef MTK_VOW_DCCALI_SUPPORT
    mDCCalEnable = false;
    AudioAutoTimeoutLock _lDC(mDCCalEnableLock);
    pthread_cond_signal(&mDCCal_Cond);

    CloseDCCalDump();

    if (mDCCalBuffer.pBufBase != NULL) { delete[] mDCCalBuffer.pBufBase; }
#endif

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

void *AudioALSACaptureDataProviderNormal::readThread(void *arg)
{
    pthread_detach(pthread_self());

    status_t retval = NO_ERROR;
    AudioALSACaptureDataProviderNormal *pDataProvider = static_cast<AudioALSACaptureDataProviderNormal *>(arg);

    uint32_t open_index = pDataProvider->mOpenIndex;

    char nameset[32];
    sprintf(nameset, "%s%d", __FUNCTION__, pDataProvider->mCaptureDataProviderType);
    prctl(PR_SET_NAME, (unsigned long)nameset, 0, 0, 0);

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
    ALOGD("+%s(), pid: %d, tid: %d, kReadBufferSize=0x%x, open_index=%d", __FUNCTION__, getpid(), gettid(), kReadBufferSize, open_index);


    // read raw data from alsa driver
    char linear_buffer[kReadBufferSize];
    uint32_t Read_Size = kReadBufferSize;
    uint32_t kReadBufferSize_new;
    while (pDataProvider->mEnable == true)
    {
        if (open_index != pDataProvider->mOpenIndex)
        {
            ALOGD("%s(), open_index(%d) != mOpenIndex(%d), return", __FUNCTION__, open_index, pDataProvider->mOpenIndex);
            break;
        }

        retval = pDataProvider->mEnableLock.lock_timeout(500);
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

        if (pDataProvider->mCaptureDropSize > 0)
        {
            Read_Size = (pDataProvider->mCaptureDropSize > kReadBufferSize) ? kReadBufferSize : pDataProvider->mCaptureDropSize;
            int retval = pcm_read(pDataProvider->mPcm, linear_buffer, Read_Size);

            if (retval != 0)
            {
                ALOGE("%s(), pcm_read() drop error, retval = %d", __FUNCTION__, retval);
            }
            ALOGV("%s(), mCaptureDropSize = %d, Read_Size=%d", __FUNCTION__, pDataProvider->mCaptureDropSize, Read_Size);
            pDataProvider->mCaptureDropSize -= Read_Size;
            pDataProvider->mEnableLock.unlock();
            continue;
        }
        else
        {
            int retval = pcm_read(pDataProvider->mPcm, linear_buffer, kReadBufferSize);
            if (retval != 0)
            {
                ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
            }
        }
        clock_gettime(CLOCK_REALTIME, &pDataProvider->mNewtime);
        pDataProvider->timerec[1] = calc_time_diff(pDataProvider->mNewtime, pDataProvider->mOldtime);
        pDataProvider->mOldtime = pDataProvider->mNewtime;

        //struct timespec tempTimeStamp;
        pDataProvider->GetCaptureTimeStamp(&pDataProvider->mStreamAttributeSource.Time_Info, kReadBufferSize);

#ifdef RECORD_INPUT_24BITS // 24bit record
        uint32_t *ptr32bit_r = (uint32_t *)linear_buffer;
        int16_t *ptr16bit_w = (int16_t *)linear_buffer;
        int i;
        ALOGV("24bit record, kReadBufferSize=%d, init ptr32bit_r=0x%x, ptr16bit_w =0x%x", kReadBufferSize, ptr32bit_r, ptr16bit_w);
        for (i = 0; i < kReadBufferSize / 4; i++)
        {
            *(ptr16bit_w + i) = (int16_t)(*(ptr32bit_r + i) >> 8);
        }
        kReadBufferSize_new = kReadBufferSize >> 1;
#else
        kReadBufferSize_new = kReadBufferSize;
#endif

#ifdef MTK_LATENCY_DETECT_PULSE
        detectPulse(0, 800, 0, (void *)linear_buffer, kReadBufferSize_new/pDataProvider->mStreamAttributeSource.num_channels/((pDataProvider->mStreamAttributeSource.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4),
                 pDataProvider->mStreamAttributeSource.audio_format, pDataProvider->mStreamAttributeSource.num_channels, pDataProvider->mStreamAttributeSource.sample_rate);
#endif

#ifdef MTK_VOW_DCCALI_SUPPORT
        //copy data to DC Cal
        pDataProvider->copyCaptureDataToDCCalBuffer(linear_buffer, kReadBufferSize_new);
#endif

        // use ringbuf format to save buffer info
        pDataProvider->mPcmReadBuf.pBufBase = linear_buffer;
        pDataProvider->mPcmReadBuf.bufLen   = kReadBufferSize_new + 1; // +1: avoid pRead == pWrite
        pDataProvider->mPcmReadBuf.pRead    = linear_buffer;
        pDataProvider->mPcmReadBuf.pWrite   = linear_buffer + kReadBufferSize_new;
        pDataProvider->mEnableLock.unlock();

        pDataProvider->provideCaptureDataToAllClients(open_index);

        clock_gettime(CLOCK_REALTIME, &pDataProvider->mNewtime);
        pDataProvider->timerec[2] = calc_time_diff(pDataProvider->mNewtime, pDataProvider->mOldtime);
        pDataProvider->mOldtime = pDataProvider->mNewtime;

        if (pDataProvider->mPCMDumpFile)
        {
            ALOGD("%s, latency_in_us,%1.6lf,%1.6lf,%1.6lf", __FUNCTION__, pDataProvider->timerec[0], pDataProvider->timerec[1], pDataProvider->timerec[2]);
        }
    }

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}

#ifdef MTK_VOW_DCCALI_SUPPORT
void AudioALSACaptureDataProviderNormal::copyCaptureDataToDCCalBuffer(void *buffer, size_t size)
{
    size_t copysize = size;
    uint32_t freeSpace = RingBuf_getFreeSpace(&mDCCalBuffer);
    ALOGV("%s(), freeSpace(%u), dataSize(%u),mDCCalBufferFull=%d", __FUNCTION__, freeSpace, size, mDCCalBufferFull);

    if (mDCCalBufferFull == false)
    {
        if (freeSpace > 0)
        {
            if (freeSpace < size)
            {
                ALOGD("%s(), freeSpace(%u) < dataSize(%u), buffer full!!", __FUNCTION__, freeSpace, size);
                //ALOGD("%s before,pBase = 0x%x pWrite = 0x%x  bufLen = %d  pRead = 0x%x",__FUNCTION__,
                //mDCCalBuffer.pBufBase,mDCCalBuffer.pWrite, mDCCalBuffer.bufLen,mDCCalBuffer.pRead);

                RingBuf_copyFromLinear(&mDCCalBuffer, (char *)buffer, freeSpace);

                //ALOGD("%s after,pBase = 0x%x pWrite = 0x%x  bufLen = %d  pRead = 0x%x",__FUNCTION__,
                //mDCCalBuffer.pBufBase,mDCCalBuffer.pWrite, mDCCalBuffer.bufLen,mDCCalBuffer.pRead);
            }
            else
            {
                //ALOGD("%s before,pBase = 0x%x pWrite = 0x%x  bufLen = %d  pRead = 0x%x",__FUNCTION__,
                //mDCCalBuffer.pBufBase,mDCCalBuffer.pWrite, mDCCalBuffer.bufLen,mDCCalBuffer.pRead);

                RingBuf_copyFromLinear(&mDCCalBuffer, (char *)buffer, size);

                //ALOGD("%s after,pBase = 0x%x pWrite = 0x%x  bufLen = %d  pRead = 0x%x",__FUNCTION__,
                //mDCCalBuffer.pBufBase,mDCCalBuffer.pWrite, mDCCalBuffer.bufLen,mDCCalBuffer.pRead);
            }
        }
        else
        {
            mDCCalBufferFull = true;
            pthread_cond_signal(&mDCCal_Cond);
        }
    }
}

size_t AudioALSACaptureDataProviderNormal::CalulateDC(short *buffer , size_t size)
{
    //ALOGV("%s()+,Size(%u)", __FUNCTION__, size);
    int checksize = size >> 2;  //stereo, 16bits
    int count = checksize;
    int accumulateL = 0, accumulateR = 0;
    short DCL = 0, DCR = 0;

#if 0
    if (pDCCalFile != NULL)
    {
        fwrite(buffer, sizeof(char), size, pDCCalFile);
    }
#endif
    WriteDCCalDumpData((void *)buffer, size);

    while (count)
    {
        accumulateL += *(buffer);
        accumulateR += *(buffer + 1);
        buffer += 2;
        count--;
    }
    DCL = (short)(accumulateL / checksize);
    DCR = (short)(accumulateR / checksize);

    ALOGD("%s()- ,checksize(%d),accumulateL(%d),accumulateR(%d), DCL(%d), DCR(%d)", __FUNCTION__, checksize, accumulateL, accumulateR, DCL, DCR);
    return size;
}

void *AudioALSACaptureDataProviderNormal::DCCalThread(void *arg)
{
    prctl(PR_SET_NAME, (unsigned long)__FUNCTION__, 0, 0, 0);

    ALOGD("+%s(), pid: %d, tid: %d, kDCRReadBufferSize=%x", __FUNCTION__, getpid(), gettid(), kDCRReadBufferSize);

    status_t retval = NO_ERROR;
    AudioALSACaptureDataProviderNormal *pDataProvider = static_cast<AudioALSACaptureDataProviderNormal *>(arg);


    //char linear_buffer[kDCRReadBufferSize];
    char *plinear_buffer = new char[kDCRReadBufferSize];
    uint32_t Read_Size = kDCRReadBufferSize;
    while (pDataProvider->mDCCalEnable == true)
    {
        pthread_mutex_lock(&pDataProvider->mDCCal_Mutex);
        pthread_cond_wait(&pDataProvider->mDCCal_Cond, &pDataProvider->mDCCal_Mutex);
        //ALOGD("%s(), signal get", __FUNCTION__);

        retval = pDataProvider->mDCCalEnableLock.lock_timeout(300);
        ASSERT(retval == NO_ERROR);
        if (pDataProvider->mDCCalEnable == false)
        {
            pDataProvider->mDCCalEnableLock.unlock();
            pthread_mutex_unlock(&pDataProvider->mDCCal_Mutex);
            break;
        }

        if (pDataProvider->mDCCalBufferFull)
        {
            Read_Size = RingBuf_getDataCount(&pDataProvider->mDCCalBuffer);
            //ALOGD("%s(), Read_Size =%u, kDCRReadBufferSize=%u", __FUNCTION__,Read_Size,kDCRReadBufferSize);
            if (Read_Size > kDCRReadBufferSize)
            {
                Read_Size = kDCRReadBufferSize;
            }

            //ALOGD("%s,pBase = 0x%x pWrite = 0x%x  bufLen = %d  pRead = 0x%x",__FUNCTION__,
            //pDataProvider->mDCCalBuffer.pBufBase,pDataProvider->mDCCalBuffer.pWrite, pDataProvider->mDCCalBuffer.bufLen,pDataProvider->mDCCalBuffer.pRead);

            RingBuf_copyToLinear(plinear_buffer, &pDataProvider->mDCCalBuffer, Read_Size);
            //ALOGD("%s after copy,pBase = 0x%x pWrite = 0x%x  bufLen = %d  pRead = 0x%x",__FUNCTION__,
            //pDataProvider->mDCCalBuffer.pBufBase,pDataProvider->mDCCalBuffer.pWrite, pDataProvider->mDCCalBuffer.bufLen,pDataProvider->mDCCalBuffer.pRead);
            pDataProvider->CalulateDC((short *)plinear_buffer, Read_Size);

            pDataProvider->mDCCalBufferFull = false;
        }

        pDataProvider->mDCCalEnableLock.unlock();
        pthread_mutex_unlock(&pDataProvider->mDCCal_Mutex);
    }

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    delete[] plinear_buffer;
    pthread_exit(NULL);
    return NULL;
}


void AudioALSACaptureDataProviderNormal::OpenDCCalDump()
{
    ALOGV("%s()", __FUNCTION__);
    char DCCalDumpFileName[128];
    sprintf(DCCalDumpFileName, "%s.pcm", "/sdcard/mtklog/audio_dump/DCCalData");

    mDCCalDumpFile = NULL;
    mDCCalDumpFile = AudioOpendumpPCMFile(DCCalDumpFileName, streamin_propty);

    if (mDCCalDumpFile != NULL)
    {
        ALOGD("%s, DCCalDumpFileName = %s", __FUNCTION__, DCCalDumpFileName);
    }
}

void AudioALSACaptureDataProviderNormal::CloseDCCalDump()
{
    if (mDCCalDumpFile)
    {
        AudioCloseDumpPCMFile(mDCCalDumpFile);
        ALOGD("%s()", __FUNCTION__);
    }
}

void  AudioALSACaptureDataProviderNormal::WriteDCCalDumpData(void *buffer , size_t size)
{
    if (mDCCalDumpFile)
    {
        //ALOGD("%s()", __FUNCTION__);
        AudioDumpPCMData((void *)buffer , size, mDCCalDumpFile);
    }
}
#else
void AudioALSACaptureDataProviderNormal::copyCaptureDataToDCCalBuffer(void *buffer, size_t size)
{
    ALOGE("%s() unsupport", __FUNCTION__);
}

size_t AudioALSACaptureDataProviderNormal::CalulateDC(short *buffer , size_t size)
{
    ALOGE("%s() unsupport", __FUNCTION__);
    return 0;
}

void *AudioALSACaptureDataProviderNormal::DCCalThread(void *arg)
{
    ALOGE("%s() unsupport", __FUNCTION__);
    return NULL;
}

void AudioALSACaptureDataProviderNormal::OpenDCCalDump()
{
    ALOGE("%s() unsupport", __FUNCTION__);
}

void AudioALSACaptureDataProviderNormal::CloseDCCalDump()
{
    ALOGE("%s() unsupport", __FUNCTION__);
}

void  AudioALSACaptureDataProviderNormal::WriteDCCalDumpData(void *buffer , size_t size)
{
    ALOGE("%s() unsupport", __FUNCTION__);
}

#endif

} // end of namespace android
