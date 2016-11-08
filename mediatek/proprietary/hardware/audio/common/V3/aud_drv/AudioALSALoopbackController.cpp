#include "AudioALSALoopbackController.h"
#include "AudioBTCVSDControl.h"
#include "AudioALSADriverUtility.h"
#include "AudioALSADeviceParser.h"
#include "AudioALSAStreamManager.h"
#include "AudioALSAHardwareResourceManager.h"
#include "WCNChipController.h"


#define LOG_TAG "AudioALSALoopbackController"

static char const *const kBTCVSDDeviceName = "/dev/ebc";
#define AUD_DRV_IOC_MAGIC 'C'
#define ALLOCATE_FREE_BTCVSD_BUF _IOWR(AUD_DRV_IOC_MAGIC, 0xE0, unsigned int)
#define SET_BTCVSD_STATE         _IOWR(AUD_DRV_IOC_MAGIC, 0xE1, unsigned int)
#define GET_BTCVSD_STATE         _IOWR(AUD_DRV_IOC_MAGIC, 0xE2, unsigned int)

namespace android
{
static android_audio_legacy::AudioStreamOut *streamOutput;

AudioALSALoopbackController *AudioALSALoopbackController::mAudioALSALoopbackController = NULL;
AudioALSALoopbackController *AudioALSALoopbackController::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSALoopbackController == NULL)
    {
        mAudioALSALoopbackController = new AudioALSALoopbackController();
    }
    ASSERT(mAudioALSALoopbackController != NULL);
    return mAudioALSALoopbackController;
}


AudioALSALoopbackController::AudioALSALoopbackController() :
    mHardwareResourceManager(AudioALSAHardwareResourceManager::getInstance()),
    mPcmDL(NULL),
    mPcmUL(NULL),
    mBtLoopbackWithCodec(false),
    mBtLoopbackWithoutCodec(false),
    mUseBtCodec(false),
    mMixer(AudioALSADriverUtility::getInstance()->getMixer())
{

}


AudioALSALoopbackController::~AudioALSALoopbackController()
{

}


status_t AudioALSALoopbackController::open(const audio_devices_t output_devices, const audio_devices_t input_device)
{
    ALOGD("+%s(), output_devices = 0x%x, input_device = 0x%x", __FUNCTION__, output_devices, input_device);
    AudioAutoTimeoutLock _l(mLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // DL loopback setting
    memset(&mConfig, 0, sizeof(mConfig));
    mConfig.channels = 2;
    mConfig.rate = 48000;
    mConfig.period_size = 1024;
    mConfig.period_count = 2;
    mConfig.format = PCM_FORMAT_S16_LE;
    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;
    ALOGD("+%s(), mConfig.rate=%d", __FUNCTION__,mConfig.rate);

    int pcmInIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmUlDlLoopback);
    int pcmOutIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmUlDlLoopback);
    int cardIndex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmUlDlLoopback);

#ifdef MTK_MAXIM_SPEAKER_SUPPORT
    if (output_devices == AUDIO_DEVICE_OUT_SPEAKER)
    {
        pcmOutIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmUlDlLoopbackSpk);
    }
#endif

    ASSERT(mPcmUL == NULL && mPcmDL == NULL);
    mPcmUL = pcm_open(cardIndex, pcmInIdx, PCM_IN, &mConfig);
    mPcmDL = pcm_open(cardIndex, pcmOutIdx, PCM_OUT, &mConfig);
    ASSERT(mPcmUL != NULL && mPcmDL != NULL);
    ALOGV("%s(), mPcmUL = %p, mPcmDL = %p", __FUNCTION__, mPcmUL, mPcmDL);

    if (input_device == AUDIO_DEVICE_IN_WIRED_HEADSET)
    {
        if (mHardwareResourceManager->getNumOfHeadsetPole() == 5)
            setLoopbackUseLCh(false);
        else
            setLoopbackUseLCh(true);
    }

    pcm_start(mPcmUL);
    pcm_start(mPcmDL);


    mHardwareResourceManager->startInputDevice(input_device);
    mHardwareResourceManager->startOutputDevice(output_devices, mConfig.rate);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSALoopbackController::close()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    mHardwareResourceManager->stopOutputDevice();

    pcm_stop(mPcmDL);
    pcm_stop(mPcmUL);
    pcm_close(mPcmDL);
    pcm_close(mPcmUL);

    mPcmDL = NULL;
    mPcmUL = NULL;

    if (mHardwareResourceManager->getInputDevice() == AUDIO_DEVICE_IN_WIRED_HEADSET)
    {
        setLoopbackUseLCh(false);
    }

    mHardwareResourceManager->stopInputDevice(mHardwareResourceManager->getInputDevice());

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSALoopbackController::SetApBTCodec(bool enable_codec)
{
    ALOGD("+%s(), enable_codec = %d", __FUNCTION__, enable_codec);
    mUseBtCodec = enable_codec;
    return NO_ERROR;
}

bool AudioALSALoopbackController::IsAPBTLoopbackWithCodec(void)
{
    //ALOGD("+%s(), mBtLoopbackWithCodec = %d", __FUNCTION__, mBtLoopbackWithCodec);
    return mBtLoopbackWithCodec;
}

status_t AudioALSALoopbackController::OpenAudioLoopbackControlFlow(const audio_devices_t input_device, const audio_devices_t output_device)
{
    // check BT device
    const bool bt_device_on = android_audio_legacy::AudioSystem::isBluetoothScoDevice((android_audio_legacy::AudioSystem::audio_devices)output_device);
    // set sample rate
    int  sample_rate;

    if (bt_device_on == true)
    {
        if (WCNChipController::GetInstance()->BTChipSamplingRate() == 0)
        {
            sample_rate = 8000;

        }
        else
        {
            sample_rate = 16000;
        }
    }

    ALOGD("+%s(), input_device = 0x%x, output_device = 0x%x, sample_rate %d", __FUNCTION__, input_device, output_device, sample_rate);
    ALOGD("+%s(), bt_device_on = %d, mUseBtCodec = %d, mBtLoopbackWithoutCodec: %d, mBtLoopbackWithCodec: %d",
          __FUNCTION__, bt_device_on, mUseBtCodec, mBtLoopbackWithoutCodec, mBtLoopbackWithCodec);
    if (bt_device_on == true)
    {
        // DAIBT
        if (WCNChipController::GetInstance()->BTUseCVSDRemoval() == true)
        {
        if (!mUseBtCodec)
        {
            mBtLoopbackWithoutCodec = 1;
            mFd2 = 0;
            mFd2 = ::open(kBTCVSDDeviceName, O_RDWR);
            ALOGD("+%s(), CVSD AP loopback without codec, mFd2: %d, AP errno: %d", __FUNCTION__, mFd2, errno);
            ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 0); //allocate TX working buffers in kernel
            ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 2); //allocate TX working buffers in kernel
            ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_TXSTATE_DIRECT_LOOPBACK); //set state to kernel
        }
        else
        {
//#if defined(BTCVSD_LOOPBACK_WITH_CODEC)
#if 1 //0902
            int format = AUDIO_FORMAT_PCM_16_BIT;
            uint32_t channels = AUDIO_CHANNEL_OUT_MONO;
            uint32_t sampleRate = 8000;
            status_t status;
            mBtLoopbackWithCodec = 1;
            streamOutput = AudioALSAStreamManager::getInstance()->openOutputStream(output_device, &format, &channels, &sampleRate, &status);
            ALOGD("+%s(), CVSD AP loopback with codec, streamOutput: %d", __FUNCTION__, streamOutput);
            mBTCVSDLoopbackThread = new AudioMTKLoopbackThread();
            if (mBTCVSDLoopbackThread.get())
            {
                mBTCVSDLoopbackThread->run();
            }
#endif
        }
        }
        else
        {
            //mAudioDigitalInstance->SetinputConnection(AudioDigitalType::Connection, AudioDigitalType::I02, AudioDigitalType::O02); // DAIBT_IN -> DAIBT_OUT
            //SetDAIBTAttribute(dl_samplerate);
            //mAudioDigitalInstance->SetDAIBTEnable(true);
        }
    }

    ALOGD("-%s()",__FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSALoopbackController::CloseAudioLoopbackControlFlow(void)
{
    AudioAutoTimeoutLock _l(mLock);
    ALOGD("+%s()", __FUNCTION__);

    //const bool bt_device_on = android_audio_legacy::AudioSystem::isBluetoothScoDevice((android_audio_legacy::AudioSystem::audio_devices)output_device);
    bool bt_device_on = true;

    ALOGD("%s(), bt_device_on = %d, mBtLoopbackWithoutCodec: %d, mBtLoopbackWithCodec: %d",
          __FUNCTION__, bt_device_on, mBtLoopbackWithoutCodec, mBtLoopbackWithCodec);

    if (bt_device_on)
    {
        if (WCNChipController::GetInstance()->BTUseCVSDRemoval() == true)
        {
            if (mBtLoopbackWithoutCodec)
            {
                ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 1); //allocate TX working buffers in kernel
                ::ioctl(mFd2, ALLOCATE_FREE_BTCVSD_BUF, 3); //allocate TX working buffers in kernel
                ::ioctl(mFd2, SET_BTCVSD_STATE, BT_SCO_TXSTATE_IDLE); //set state to kernel
                mBtLoopbackWithoutCodec = 0;
            }
            else if (mBtLoopbackWithCodec)
            {
//#if defined(BTCVSD_LOOPBACK_WITH_CODEC)
#if 1 //0902
                streamOutput->standby();
                if (mBTCVSDLoopbackThread.get())
                {
                    int ret = 0;
                    //ret = mBTCVSDLoopbackThread->requestExitAndWait();
                    //if (ret == WOULD_BLOCK)
                    {
                        mBTCVSDLoopbackThread->requestExit();
                    }
                    mBTCVSDLoopbackThread.clear();
                }
                AudioALSAStreamManager::getInstance()->closeOutputStream(streamOutput);
                mBtLoopbackWithCodec = 0;
#endif
            }
        }
        else
        {
            //mAudioDigitalInstance->SetDAIBTEnable(false);
            //mAudioDigitalInstance->SetinputConnection(AudioDigitalType::DisConnect, AudioDigitalType::I02, AudioDigitalType::O02); // DAIBT_IN -> DAIBT_OUT
        }
    }

    ALOGD("-%s()", __FUNCTION__);

    return NO_ERROR;
}


void AudioALSALoopbackController::setLoopbackUseLCh(bool enable)
{
    enum mixer_ctl_type type;
    struct mixer_ctl *ctl;
    int retval = 0;

    ctl = mixer_get_ctl_by_name(mMixer, "LPBK_IN_USE_LCH");

    if (ctl == NULL)
    {
        ALOGE("LPBK_IN_USE_LCH not support");
        return;
    }

    if (enable == true)
    {
        retval = mixer_ctl_set_enum_by_string(ctl, "On");
        ASSERT(retval == 0);
    }
    else
    {
        retval = mixer_ctl_set_enum_by_string(ctl, "Off");
        ASSERT(retval == 0);
    }
}


#if 1 //0902

extern void CVSDLoopbackResetBuffer();
extern void CVSDLoopbackReadDataDone(uint32_t len);
extern void CVSDLoopbackGetReadBuffer(uint8_t **buffer, uint32_t *buf_len);
extern int32_t CVSDLoopbackGetDataCount();

AudioALSALoopbackController::AudioMTKLoopbackThread::AudioMTKLoopbackThread()
{
    ALOGD("BT_SW_CVSD AP loopback AudioMTKLoopbackThread constructor");
}

AudioALSALoopbackController::AudioMTKLoopbackThread::~AudioMTKLoopbackThread()
{
    ALOGD("BT_SW_CVSD AP loopback ~AudioMTKLoopbackThread");
}

void AudioALSALoopbackController::AudioMTKLoopbackThread::onFirstRef()
{
    ALOGD("BT_SW_CVSD AP loopback AudioMTKLoopbackThread::onFirstRef");
    run(mName, ANDROID_PRIORITY_URGENT_AUDIO);
}

status_t  AudioALSALoopbackController::AudioMTKLoopbackThread::readyToRun()
{
    ALOGD("BT_SW_CVSD AP loopback AudioMTKLoopbackThread::readyToRun()");
    return NO_ERROR;
}

bool AudioALSALoopbackController::AudioMTKLoopbackThread::threadLoop()
{
    uint8_t *pReadBuffer;
    uint32_t uReadByte, uWriteDataToBT;
    CVSDLoopbackResetBuffer();
    while (!(exitPending() == true))
    {
        ALOGD("BT_SW_CVSD AP loopback threadLoop(+)");
        uWriteDataToBT = 0;
        CVSDLoopbackGetReadBuffer(&pReadBuffer, &uReadByte);
        uReadByte &= 0xFFFFFFFE;
        if (uReadByte)
        {
            uWriteDataToBT = streamOutput->write(pReadBuffer, uReadByte);
            CVSDLoopbackReadDataDone(uWriteDataToBT);
        }
        else
        {
            usleep(5 * 1000); //5ms
        }
        ALOGD("BT_SW_CVSD AP loopback threadLoop(-), uReadByte: %d, uWriteDataToBT: %d", uReadByte, uWriteDataToBT);
    }
    ALOGD("BT_SW_CVSD AP loopback threadLoop exit");
    return false;
}

#endif



} // end of namespace android
