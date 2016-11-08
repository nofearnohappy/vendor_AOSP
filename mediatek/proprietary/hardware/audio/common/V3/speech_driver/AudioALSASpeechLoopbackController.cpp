#include "AudioALSASpeechLoopbackController.h"

#include "AudioALSADriverUtility.h"
#include "AudioALSAHardwareResourceManager.h"

#include "SpeechDriverInterface.h"
#include "SpeechDriverFactory.h"
#include "SpeechVMRecorder.h"


#define LOG_TAG "AudioALSASpeechLoopbackController"

namespace android
{

AudioALSASpeechLoopbackController *AudioALSASpeechLoopbackController::mSpeechLoopbackController = NULL;
AudioALSASpeechLoopbackController *AudioALSASpeechLoopbackController::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mSpeechLoopbackController == NULL)
    {
        mSpeechLoopbackController = new AudioALSASpeechLoopbackController();
    }
    ASSERT(mSpeechLoopbackController != NULL);
    return mSpeechLoopbackController;
}


AudioALSASpeechLoopbackController::AudioALSASpeechLoopbackController() :
    mHardwareResourceManager(AudioALSAHardwareResourceManager::getInstance()),
    mSpeechDriverFactory(SpeechDriverFactory::GetInstance()),
    mPcmUL(NULL),
    mPcmDL(NULL)
{

}


AudioALSASpeechLoopbackController::~AudioALSASpeechLoopbackController()
{

}

status_t AudioALSASpeechLoopbackController::open(const audio_devices_t output_devices, const audio_devices_t input_device)
{
    ALOGD("+%s(), output_devices = 0x%x, input_device = 0x%x", __FUNCTION__, output_devices, input_device);
    AudioAutoTimeoutLock _l(mLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // get speech driver instance
    SpeechDriverInterface *pSpeechDriver = mSpeechDriverFactory->GetSpeechDriver();

    // check BT device // TODO(Harvey): BT Loopback?
    const uint32_t sample_rate = 16000;
    ALOGD("%s(), sample_rate = %d", __FUNCTION__, sample_rate);


    //--- here to test pcm interface platform driver_attach
    memset(&mConfig, 0, sizeof(mConfig));
    mConfig.channels = 2;
    mConfig.rate = sample_rate;
    mConfig.period_size = 1024;
    mConfig.period_count = 2;
    mConfig.format = PCM_FORMAT_S16_LE;
    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;

    ASSERT(mPcmUL == NULL && mPcmDL == NULL);


    int pcmInIdx, pcmOutIdx, cardIndex;
    if( mSpeechDriverFactory->GetActiveModemIndex() == MODEM_1 )
    {
        pcmInIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVoiceMD1);
        pcmOutIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVoiceMD1);
        cardIndex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmVoiceMD1);
#ifdef MTK_MAXIM_SPEAKER_SUPPORT
        if (output_devices == AUDIO_DEVICE_OUT_SPEAKER)
        {
            pcmOutIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVoiceSpkMD1);
        }
#endif
    }
    else
    {
        pcmInIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVoiceMD2);
        pcmOutIdx = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmVoiceMD2);
        cardIndex = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmVoiceMD2);
    }

    mPcmUL = pcm_open(cardIndex, pcmInIdx, PCM_IN, &mConfig);
    mPcmDL = pcm_open(cardIndex, pcmOutIdx, PCM_OUT, &mConfig);
    ASSERT(mPcmUL != NULL && mPcmDL != NULL);
    ALOGV("%s(), mPcmUL = %p, mPcmDL = %p", __FUNCTION__, mPcmUL, mPcmDL);

    pcm_start(mPcmUL);
    pcm_start(mPcmDL);


    // Set PMIC digital/analog part - uplink has pop, open first
    mHardwareResourceManager->startInputDevice(input_device);


    // Clean Side Tone Filter gain
    pSpeechDriver->SetSidetoneGain(0);

    // Set MD side sampling rate
    pSpeechDriver->SetModemSideSamplingRate(sample_rate);

    // Set speech mode
    pSpeechDriver->SetSpeechMode(input_device, output_devices);

    // Loopback on
    pSpeechDriver->SetAcousticLoopback(true);

    mHardwareResourceManager->startOutputDevice(output_devices, sample_rate);

    // check VM need open
    SpeechVMRecorder *pSpeechVMRecorder = SpeechVMRecorder::GetInstance();
    if (pSpeechVMRecorder->GetVMRecordCapability() == true)
    {
        ALOGD("%s(), Open VM/EPL record", __FUNCTION__);
        pSpeechVMRecorder->Open();
    }

    ALOGD("-%s(), output_devices = 0x%x, input_device = 0x%x", __FUNCTION__, output_devices, input_device);
    return NO_ERROR;
}


status_t AudioALSASpeechLoopbackController::close()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(mLock);
    AudioAutoTimeoutLock _l2(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    // check VM need close
    SpeechVMRecorder *pSpeechVMRecorder = SpeechVMRecorder::GetInstance();
    if (pSpeechVMRecorder->GetVMRecordStatus() == true)
    {
        ALOGD("%s(), Close VM/EPL record", __FUNCTION__);
        pSpeechVMRecorder->Close();
    }

    mHardwareResourceManager->stopOutputDevice();

    // Stop MODEM_PCM
    pcm_stop(mPcmDL);
    pcm_stop(mPcmUL);
    pcm_close(mPcmDL);
    pcm_close(mPcmUL);

    mPcmDL = NULL;
    mPcmUL = NULL;

    mHardwareResourceManager->stopInputDevice(mHardwareResourceManager->getInputDevice());

    // Get current active speech driver
    SpeechDriverInterface *pSpeechDriver = mSpeechDriverFactory->GetSpeechDriver();

    // Loopback off
    pSpeechDriver->SetAcousticLoopback(false);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSASpeechLoopbackController::SetModemBTCodec(bool enable_codec)
{
    ALOGD("+%s(), enable_codec = %d", __FUNCTION__, enable_codec);
    mUseBtCodec = enable_codec;
    return NO_ERROR;
}

status_t AudioALSASpeechLoopbackController::OpenModemLoopbackControlFlow(const audio_devices_t input_device, const audio_devices_t output_device)
{
    ALOGD("+%s(), output_device = 0x%x, input_device = 0x%x", __FUNCTION__, output_device, input_device);
    AudioAutoTimeoutLock _l(mLock);

    // get speech driver instance
    SpeechDriverInterface *pSpeechDriver = mSpeechDriverFactory->GetSpeechDriver();

    // check BT device // TODO(Harvey): BT Loopback?
    const bool bt_device_on = android_audio_legacy::AudioSystem::isBluetoothScoDevice((android_audio_legacy::AudioSystem::audio_devices)output_device);
    const int  sample_rate  = (bt_device_on == true) ? 8000 : 16000;
    ALOGD("%s(), sample_rate = %d", __FUNCTION__, sample_rate);

    // Clean Side Tone Filter gain
    pSpeechDriver->SetSidetoneGain(0);

    // Set MD side sampling rate
    pSpeechDriver->SetModemSideSamplingRate(sample_rate);

    // Set speech mode
    pSpeechDriver->SetSpeechMode(input_device, output_device);

    // BT Loopback on
    pSpeechDriver->SetAcousticLoopbackBtCodec(mUseBtCodec);    
    pSpeechDriver->SetAcousticLoopback(true);

    ALOGD("-%s(), output_devices = 0x%x, input_device = 0x%x", __FUNCTION__, output_device, input_device);
    return NO_ERROR;    
}


status_t AudioALSASpeechLoopbackController::CloseModemLoopbackControlFlow(void)
{
    AudioAutoTimeoutLock _l(mLock);
    ALOGD("+%s()", __FUNCTION__);

    // Get current active speech driver
    SpeechDriverInterface *pSpeechDriver = mSpeechDriverFactory->GetSpeechDriver();

    // Loopback off
    pSpeechDriver->SetAcousticLoopback(false);

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


} // end of namespace android
