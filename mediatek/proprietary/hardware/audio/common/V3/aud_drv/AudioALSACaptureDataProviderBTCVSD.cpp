#include "AudioALSACaptureDataProviderBTCVSD.h"

#include <pthread.h>

#include <linux/rtpm_prio.h>
#include <sys/prctl.h>

#include "AudioType.h"

#include "WCNChipController.h"

#include "AudioBTCVSDDef.h"
#include "AudioBTCVSDControl.h"


#define LOG_TAG "AudioALSACaptureDataProviderBTCVSD"

namespace android
{

static bool mBTMode_Open;
/*==============================================================================
 *                     Implementation
 *============================================================================*/

AudioALSACaptureDataProviderBTCVSD *AudioALSACaptureDataProviderBTCVSD::mAudioALSACaptureDataProviderBTCVSD = NULL;
AudioALSACaptureDataProviderBTCVSD *AudioALSACaptureDataProviderBTCVSD::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACaptureDataProviderBTCVSD == NULL)
    {
        mAudioALSACaptureDataProviderBTCVSD = new AudioALSACaptureDataProviderBTCVSD();
    }
    ASSERT(mAudioALSACaptureDataProviderBTCVSD != NULL);
    return mAudioALSACaptureDataProviderBTCVSD;
}

AudioALSACaptureDataProviderBTCVSD::AudioALSACaptureDataProviderBTCVSD() :
    mWCNChipController(WCNChipController::GetInstance()),
    mAudioBTCVSDControl(AudioBTCVSDControl::getInstance()),
    mReadBufferSize(0)
#ifndef MTK_SUPPORT_BTCVSD_ALSA
    , mFd2(mAudioBTCVSDControl->getFd())
#endif
{
    ALOGD("%s()", __FUNCTION__);

    mCaptureDataProviderType = CAPTURE_PROVIDER_BT_CVSD;
}

AudioALSACaptureDataProviderBTCVSD::~AudioALSACaptureDataProviderBTCVSD()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACaptureDataProviderBTCVSD::open()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class attach
    AudioAutoTimeoutLock _l(mEnableLock);

    ASSERT(mEnable == false);

    // config attribute (will used in client SRC/Enh/... later)
    mStreamAttributeSource.audio_format = AUDIO_FORMAT_PCM_16_BIT;
    mStreamAttributeSource.audio_channel_mask = AUDIO_CHANNEL_IN_MONO;
    mStreamAttributeSource.num_channels = android_audio_legacy::AudioSystem::popCount(mStreamAttributeSource.audio_channel_mask);
    mStreamAttributeSource.sample_rate = mWCNChipController->GetBTCurrentSamplingRateNumber();

    if (mAudioBTCVSDControl->BT_SCO_isWideBand() == true)
    {
        mReadBufferSize = MSBC_PCM_FRAME_BYTE * 6 * 2; // 16k mono->48k stereo
    }
    else
    {
        mReadBufferSize = SCO_RX_PCM8K_BUF_SIZE * 12 * 2; // 8k mono->48k stereo
    }

    mBTMode_Open = mAudioBTCVSDControl->BT_SCO_isWideBand();

    ALOGD("%s(), audio_format = %d, audio_channel_mask=%x, num_channels=%d, sample_rate=%d", __FUNCTION__,
          mStreamAttributeSource.audio_format, mStreamAttributeSource.audio_channel_mask, mStreamAttributeSource.num_channels, mStreamAttributeSource.sample_rate);

    OpenPCMDump(LOG_TAG);

    // enable bt cvsd driver
#ifdef MTK_SUPPORT_BTCVSD_ALSA
#if 0
	int pcmindex = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmBTCVSDCapture);
	int cardindex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmBTCVSDCapture);

	struct pcm_params *params;
	params = pcm_params_get(cardindex, pcmindex,  PCM_OUT);
	if (params == NULL)
	{
		ALOGD("Device does not exist.\n");
	}

	// HW pcm config
	mConfig.channels = mStreamAttributeSource.num_channels;
	mConfig.rate = mStreamAttributeSource.sample_rate;
	mConfig.period_count = 2;
	mConfig.period_size = 1024;//(mStreamAttributeTarget.buffer_size / (mConfig.channels * mConfig.period_count)) / ((mStreamAttributeTarget.audio_format == AUDIO_FORMAT_PCM_16_BIT) ? 2 : 4);
	mConfig.format = PCM_FORMAT_S16_LE;//transferAudioFormatToPcmFormat(mStreamAttributeTarget.audio_format);
	mConfig.start_threshold = 0;
	mConfig.stop_threshold = 0;
	mConfig.silence_threshold = 0;
	ALOGD("%s(), mConfig: channels = %d, rate = %d, period_size = %d, period_count = %d, format = %d, pcmindex=%d",
		  __FUNCTION__, mConfig.channels, mConfig.rate, mConfig.period_size, mConfig.period_count, mConfig.format, pcmindex);

	// open pcm driver
	openPcmDriver(pcmindex);
	//openPcmDriver(25);
#else
	memset(&mConfig, 0, sizeof(mConfig));
	mConfig.channels = mStreamAttributeSource.num_channels;
	mConfig.rate = mStreamAttributeSource.sample_rate;
	mConfig.period_size = 1024;
	mConfig.period_count = 2;
	mConfig.format = PCM_FORMAT_S16_LE;
	mConfig.start_threshold = 0;
	mConfig.stop_threshold = 0;
	mConfig.silence_threshold = 0;

	ASSERT(mPcm == NULL);
    int pcmIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmBTCVSDCapture);
    int cardIdx = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmBTCVSDCapture);
	mPcm = pcm_open(cardIdx, pcmIdx, PCM_IN, &mConfig);
	ASSERT(mPcm != NULL && pcm_is_ready(mPcm) == true);
	mAudioBTCVSDControl->BT_SCO_RX_Begin(mFd2);
	pcm_start(mPcm);
#endif
#else
    mAudioBTCVSDControl->BT_SCO_RX_Begin(mFd2);
#endif

    // create reading thread
    mEnable = true;
    int ret = pthread_create(&hReadThread, NULL, AudioALSACaptureDataProviderBTCVSD::readThread, (void *)this);
    if (ret != 0)
    {
        ALOGE("%s() create thread fail!!", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    return NO_ERROR;
}

status_t AudioALSACaptureDataProviderBTCVSD::close()
{
    ALOGD("%s()", __FUNCTION__);
    ASSERT(mClientLock.tryLock() != 0); // lock by base class detach

    mEnable = false;
    AudioAutoTimeoutLock _l(mEnableLock);

    ClosePCMDump();

    mAudioBTCVSDControl->BT_SCO_RX_End(mFd2);

#ifdef MTK_SUPPORT_BTCVSD_ALSA
    pcm_stop(mPcm);
    pcm_close(mPcm);
    mPcm = NULL;
#endif
    return NO_ERROR;
}


void *AudioALSACaptureDataProviderBTCVSD::readThread(void *arg)
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
    AudioALSACaptureDataProviderBTCVSD *pDataProvider = static_cast<AudioALSACaptureDataProviderBTCVSD *>(arg);

    uint32_t open_index = pDataProvider->mOpenIndex;

    // read raw data from alsa driver
    uint32_t read_size = 0;
    char linear_buffer[MSBC_PCM_FRAME_BYTE * 6 * 2]; // fixed at size for WB
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
            ALOGE("%s(), pDataProvider->mEnable == false", __FUNCTION__);
            pDataProvider->mEnableLock.unlock();
            break;
        }

#ifdef MTK_SUPPORT_BTCVSD_ALSA
	ASSERT(pDataProvider->mPcm != NULL);
	uint8_t *cvsd_raw_data = pDataProvider->mAudioBTCVSDControl->BT_SCO_RX_GetCVSDTempInBuf();
	int retval = pcm_read(pDataProvider->mPcm, cvsd_raw_data, BTSCO_CVSD_RX_TEMPINPUTBUF_SIZE);
	if (retval != 0)
	{
		ALOGE("%s(), pcm_read() error, retval = %d", __FUNCTION__, retval);
		pDataProvider->mEnableLock.unlock();
		continue;
	}
#endif
        read_size = pDataProvider->readDataFromBTCVSD(linear_buffer);
        if (read_size == 0)
        {
            ALOGE("%s(), read_size == 0", __FUNCTION__);
            pDataProvider->mEnableLock.unlock();
            continue;
        }

        // use ringbuf format to save buffer info
        pDataProvider->mPcmReadBuf.pBufBase = linear_buffer;
        pDataProvider->mPcmReadBuf.bufLen   = read_size + 1; // +1: avoid pRead == pWrite
        pDataProvider->mPcmReadBuf.pRead    = linear_buffer;
        pDataProvider->mPcmReadBuf.pWrite   = linear_buffer + read_size;
        pDataProvider->mEnableLock.unlock();

        pDataProvider->provideCaptureDataToAllClients(open_index);
    }

    ALOGD("-%s(), pid: %d, tid: %d", __FUNCTION__, getpid(), gettid());
    pthread_exit(NULL);
    return NULL;
}


uint32_t AudioALSACaptureDataProviderBTCVSD::readDataFromBTCVSD(void *linear_buffer)
{
    ALOGV("+%s()", __FUNCTION__);

    uint8_t *cvsd_raw_data = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDTempInBuf();
#ifndef MTK_SUPPORT_BTCVSD_ALSA
    uint32_t raw_data_size = ::read(mFd2, cvsd_raw_data, BTSCO_CVSD_RX_TEMPINPUTBUF_SIZE);
    ALOGV("%s(), cvsd_raw_data = %p, raw_data_size = %d", __FUNCTION__, cvsd_raw_data, raw_data_size);

    if (raw_data_size == 0)
    {
        ALOGE("%s(), raw_data_size == 0", __FUNCTION__);
        return 0;
    }
#endif

    uint8_t *inbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDInBuf();
    uint32_t insize = SCO_RX_PLC_SIZE;

    uint8_t *outbuf = NULL;
    uint32_t outsize = 0;
    if (mAudioBTCVSDControl->BT_SCO_isWideBand() == true)
    {
        outbuf = mAudioBTCVSDControl->BT_SCO_RX_GetMSBCOutBuf();
        outsize = MSBC_PCM_FRAME_BYTE;
    }
    else
    {
        outbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDOutBuf();
        outsize = SCO_RX_PCM8K_BUF_SIZE;
    }

    uint8_t *workbuf = mAudioBTCVSDControl->BT_SCO_RX_GetCVSDWorkBuf();
    uint32_t workbufsize = SCO_RX_PCM64K_BUF_SIZE;


    uint8_t packetvalid = 0;
    uint32_t total_read_size = 0;
    uint32_t bytes = BTSCO_CVSD_RX_INBUF_SIZE;
    do
    {
        memcpy(inbuf, cvsd_raw_data, SCO_RX_PLC_SIZE);
        cvsd_raw_data += SCO_RX_PLC_SIZE;

        packetvalid = *cvsd_raw_data; // parser packet valid info for each 30-byte packet
        //packetvalid = 1; // force packvalid to 1 for test
        cvsd_raw_data += BTSCO_CVSD_PACKET_VALID_SIZE;

        insize = SCO_RX_PLC_SIZE;
        
        if(mBTMode_Open != mAudioBTCVSDControl->BT_SCO_isWideBand())
        {            
            ALOGD("BTSCO change mode after RX_Begin!!!");
            mAudioBTCVSDControl->BT_SCO_RX_End(mFd2);
            mAudioBTCVSDControl->BT_SCO_RX_Begin(mFd2); 
            mBTMode_Open = mAudioBTCVSDControl->BT_SCO_isWideBand();                    

            if (mAudioBTCVSDControl->BT_SCO_isWideBand() == true)
            {
                mReadBufferSize = MSBC_PCM_FRAME_BYTE * 6 * 2; // 16k mono->48k stereo
            }
            else
            {
                mReadBufferSize = SCO_RX_PCM8K_BUF_SIZE * 12 * 2; // 8k mono->48k stereo
            }

            return 0;
        }

        outsize = (mAudioBTCVSDControl->BT_SCO_isWideBand() == true) ? MSBC_PCM_FRAME_BYTE : SCO_RX_PCM8K_BUF_SIZE;
        ALOGV("btsco_process_RX_CVSD/MSBC(+), insize = %d, outsize = %d, packetvalid = %u", insize, outsize, packetvalid);

        if (mAudioBTCVSDControl->BT_SCO_isWideBand() == true)
        {
            mAudioBTCVSDControl->btsco_process_RX_MSBC(inbuf, &insize, outbuf, &outsize, workbuf, workbufsize, packetvalid);
        }
        else
        {
            mAudioBTCVSDControl->btsco_process_RX_CVSD(inbuf, &insize, outbuf, &outsize, workbuf, workbufsize, packetvalid);
        }
        inbuf += SCO_RX_PLC_SIZE;
        bytes -= insize;
        ALOGV("btsco_process_RX_CVSD/MSBC(-), insize = %d, outsize = %d, bytes = %d", insize, outsize, bytes);


        if (outsize > 0)
        {
            ASSERT(total_read_size + outsize <= mReadBufferSize);
            memcpy(linear_buffer, outbuf, outsize);
            linear_buffer += outsize;
            total_read_size += outsize;
        }
    }
    while (bytes > 0);


    ALOGV("+%s(), total_read_size = %u", __FUNCTION__, total_read_size);
    return total_read_size;
}


} // end of namespace android
