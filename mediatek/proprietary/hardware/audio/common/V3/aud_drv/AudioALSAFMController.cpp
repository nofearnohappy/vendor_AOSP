#define LOG_TAG "AudioALSAFMController"

#include "AudioALSAFMController.h"

#include <linux/ioctl.h>
#include <cutils/properties.h>

#include "WCNChipController.h"

#include "AudioLock.h"
#include "AudioALSADriverUtility.h"
#include "AudioALSAHardwareResourceManager.h"
//#include "AudioALSAVolumeController.h"
//#include "AudioVolumeInterface.h"

#include "AudioVolumeFactory.h"


#include "AudioALSASampleRateController.h"

#include "AudioALSADeviceParser.h"

namespace android
{

/*==============================================================================
 *                     Property keys
 *============================================================================*/

const char PROPERTY_KEY_FM_FORCE_DIRECT_MODE_TYPE[PROPERTY_KEY_MAX]  = "af.fm.force_direct_mode_type";

/*==============================================================================
 *                     Const Value
 *============================================================================*/

/*==============================================================================
 *                     Enumerator
 *============================================================================*/

enum fm_force_direct_mode_t
{
    FM_FORCE_NONE           = 0x0,
    FM_FORCE_DIRECT_MODE    = 0x1,
    FM_FORCE_INDIRECT_MODE  = 0x2,
};

/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

AudioALSAFMController *AudioALSAFMController::mAudioALSAFMController = NULL;

AudioALSAFMController *AudioALSAFMController::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSAFMController == NULL)
    {
        mAudioALSAFMController = new AudioALSAFMController();
    }
    ASSERT(mAudioALSAFMController != NULL);
    return mAudioALSAFMController;
}

/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

AudioALSAFMController::AudioALSAFMController() :
    mFmDeviceCallback(NULL),
    mHardwareResourceManager(AudioALSAHardwareResourceManager::getInstance()),
    mAudioALSAVolumeController(AudioVolumeFactory::CreateAudioVolumeController()),
    mFmEnable(false),
    mIsFmDirectConnectionMode(true),
    mFmVolume(0.0), // valid volume value: 0.0 ~ 1.0
    mPcm(NULL),
    mUseFmDirectConnectionMode(true),
    mOuput_device(AUDIO_DEVICE_NONE)
{
    ALOGD("%s()", __FUNCTION__);

    memset(&mConfig, 0, sizeof(mConfig));
}

AudioALSAFMController::~AudioALSAFMController()
{
    ALOGD("%s()", __FUNCTION__);
}

/*==============================================================================
 *                     FM Control
 *============================================================================*/

bool AudioALSAFMController::checkFmNeedUseDirectConnectionMode()
{
    return mUseFmDirectConnectionMode;
}


bool AudioALSAFMController::getFmEnable()
{
    AudioAutoTimeoutLock _l(mLock);
    ALOGV("%s(), mFmEnable = %d", __FUNCTION__, mFmEnable);
    return mFmEnable;
}

status_t AudioALSAFMController::setFmEnable(const bool enable, const audio_devices_t output_device, bool bForceControl, bool bForce2DirectConn, bool bNeedSyncVolume) // TODO(Harvey)
{
    // Lock to Protect HW Registers & AudioMode
    // TODO(Harvey): get stream manager lock here?

    AudioAutoTimeoutLock _l(mLock);

    ALOGD("+%s(), mFmEnable = %d => enable = %d, bForceControl = %d,  bForce2DirectConn = %d",
          __FUNCTION__, mFmEnable, enable, bForceControl, bForce2DirectConn);



    // Check Current Status
    if (enable == mFmEnable)
    {
            ALOGW("-%s(), enable == mFmEnable, return.", __FUNCTION__);
            return NO_ERROR;
    }

    // Update Enable Status
    mFmEnable = enable;
    // update output  device
    mOuput_device = output_device;

    // get current device
    ALOGD("%s(), output_device = 0x%x", __FUNCTION__, output_device);

    AudioALSASampleRateController *pAudioALSASampleRateController = AudioALSASampleRateController::getInstance();

    if (mFmEnable == true) // Open
    {
#if 0 // local print only
        ALOGD("IsFMMergeInterfaceSupported = %d", WCNChipController::GetInstance()->IsFMMergeInterfaceSupported());
        ALOGD("IsFmChipPadSelConnSys = %d", WCNChipController::GetInstance()->IsFmChipPadSelConnSys());
        ALOGD("IsFmChipUseSlaveMode = %d", WCNChipController::GetInstance()->IsFmChipUseSlaveMode());
        ALOGD("GetFmChipSamplingRate = %d", WCNChipController::GetInstance()->GetFmChipSamplingRate());

        ALOGD("IsBTMergeInterfaceSupported = %d", WCNChipController::GetInstance()->IsBTMergeInterfaceSupported());
        ALOGD("BTChipHWInterface = %d", WCNChipController::GetInstance()->BTChipHWInterface());
        ALOGD("BTUseCVSDRemoval = %d", WCNChipController::GetInstance()->BTUseCVSDRemoval());
        ALOGD("BTChipSamplingRate = %d", WCNChipController::GetInstance()->BTChipSamplingRate());
        ALOGD("BTChipSamplingRateNumber = %d", WCNChipController::GetInstance()->BTChipSamplingRateNumber());
        ALOGD("BTChipSyncFormat = %d", WCNChipController::GetInstance()->BTChipSyncFormat());
        ALOGD("BTChipSyncLength = %d", WCNChipController::GetInstance()->BTChipSyncLength());
        ALOGD("BTChipSecurityHiLo = %d", WCNChipController::GetInstance()->BTChipSecurityHiLo());

        ALOGD("GetBTCurrentSamplingRateNumber = %d", WCNChipController::GetInstance()->GetBTCurrentSamplingRateNumber());
#endif

#ifdef MTK_BASIC_PACKAGE
        if (!isPreferredSampleRate(getFmDownlinkSamplingRate()))
#endif
        {
            // set default 44100 Hz
            pAudioALSASampleRateController->setPrimaryStreamOutSampleRate(44100);
        }

        if (true != bForceControl || false != bForce2DirectConn)//If indirect mode, normal handler will set PLAYBACK_SCENARIO_STREAM_OUT
        {
            pAudioALSASampleRateController->setScenarioStatus(PLAYBACK_SCENARIO_FM);
        }

        if (WCNChipController::GetInstance()->IsFMMergeInterfaceSupported() == true)
        {   //I2S fixed at 32K Hz
            WCNChipController::GetInstance()->SetFmChipSampleRate(getFmDownlinkSamplingRate());
        }


        // Set Audio Digital/Analog HW Register
        // if (checkFmNeedUseDirectConnectionMode(output_device) == true)
        // {
            if (true == bForceControl && false == bForce2DirectConn)
            {
                mIsFmDirectConnectionMode = false;
            }
            else
            {
                setFmDirectConnection(true, true);
                mHardwareResourceManager->startOutputDevice(output_device, getFmDownlinkSamplingRate());
            }
        // }
        // else
        // {
            // mIsFmDirectConnectionMode = false;
        // }

        // Set Direct/Indirect Mode to FMAudioPlayer
        // doDeviceChangeCallback();
    }
    else // Close
    {
        // Disable Audio Digital/Analog HW Register
        if (mIsFmDirectConnectionMode == true)
        {
            mHardwareResourceManager->stopOutputDevice();
        }
        setFmDirectConnection(false, true);

        // reset FM playback status
        pAudioALSASampleRateController->resetScenarioStatus(PLAYBACK_SCENARIO_FM);
    }

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSAFMController::routing(const audio_devices_t pre_device, const audio_devices_t new_device)
{
    AudioAutoTimeoutLock _l(mLock);

    ASSERT(mFmEnable == true);

    ALOGD("+%s(), pre_device = 0x%x, new_device = 0x%x", __FUNCTION__, pre_device, new_device);

    // Close
    if (mIsFmDirectConnectionMode == true) // Direct mode, close it directly
    {
        // wait until HW Gain stable
        setFmVolume(0.0);// make sure
        usleep(430000); // -74.5/0.25 * 64 / 44100 = 430 ms
        mHardwareResourceManager->stopOutputDevice();
    }

    mOuput_device = new_device;

    // Open
    setFmDirectConnection(checkFmNeedUseDirectConnectionMode(), false);

    // Set Direct/Indirect Mode for FM Chip
    // doDeviceChangeCallback();

    // Enable PMIC Analog Part
    if (mIsFmDirectConnectionMode == true) // Direct mode, open it directly
    {
        mHardwareResourceManager->startOutputDevice(new_device, getFmDownlinkSamplingRate());
        setFmVolume(mFmVolume);
    }

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}

void AudioALSAFMController::setFmDeviceCallback(const AUDIO_DEVICE_CHANGE_CALLBACK_STRUCT *callback_data)
{
    if (callback_data == NULL)
    {
        mFmDeviceCallback = NULL;
    }
    else
    {
        mFmDeviceCallback = callback_data->callback;
        ASSERT(mFmDeviceCallback != NULL);
    }
}

status_t AudioALSAFMController::doDeviceChangeCallback()
{
    ALOGD("+%s(), mIsFmDirectConnectionMode = %d, callback = %p", __FUNCTION__, mIsFmDirectConnectionMode, mFmDeviceCallback);

    ASSERT(mFmEnable == true);

    if (mFmDeviceCallback == NULL) // factory mode might not set mFmDeviceCallback
    {
        ALOGE("-%s(), mFmDeviceCallback == NULL", __FUNCTION__);
        return NO_INIT;
    }


    if (mIsFmDirectConnectionMode == true)
    {
        mFmDeviceCallback((void *)false); // Direct Mode, No need to create in/out stream
        ALOGD("-%s(), mFmDeviceCallback(false)", __FUNCTION__);
    }
    else
    {
        mFmDeviceCallback((void *)true);  // Indirect Mode, Need to create in/out stream
        ALOGD("-%s(), mFmDeviceCallback(true)", __FUNCTION__);
    }

    return NO_ERROR;
}

bool AudioALSAFMController::isPreferredSampleRate(uint32_t rate) const
{
    switch (rate)
    {
    case 44100:
    case 48000:
        return true;
    default:
        return false;
    }
}

/*==============================================================================
 *                     Audio HW Control
 *============================================================================*/

uint32_t AudioALSAFMController::getFmUplinkSamplingRate() const
{
    uint32_t Rate = AudioALSASampleRateController::getInstance()->getPrimaryStreamOutSampleRate();

    if ( Rate != 48000 && Rate != 44100)
    {
        return 44100;
    }
    else
    {
        return Rate;
    }
}

uint32_t AudioALSAFMController::getFmDownlinkSamplingRate() const
{
    return AudioALSASampleRateController::getInstance()->getPrimaryStreamOutSampleRate();
}

status_t AudioALSAFMController::setFmDirectConnection(const bool enable, const bool bforce)
{
    ALOGD("+%s(), enable = %d, bforce = %d", __FUNCTION__, enable, bforce);

    // Check Current Status
    if (mIsFmDirectConnectionMode == enable && bforce == false)
    {
        ALOGW("-%s(), enable = %d, bforce = %d", __FUNCTION__, enable, bforce);
        return INVALID_OPERATION;
    }


    // Apply
    if (enable == true)
    {
		memset(&mConfig, 0, sizeof(mConfig));    
        mConfig.channels = 2;
        mConfig.rate = getFmDownlinkSamplingRate();
        mConfig.period_size = 3072;
        mConfig.period_count = 2;
        mConfig.format = PCM_FORMAT_S16_LE;
        mConfig.start_threshold = 0;
        mConfig.stop_threshold = 0;
        mConfig.silence_threshold = 0;

        // Get pcm open Info
        int card_index = -1;
        int pcm_index = -1;

        if (mPcm == NULL)
        {
            AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
            if (WCNChipController::GetInstance()->IsFMMergeInterfaceSupported() == true)
            {
                card_index = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmMRGrxPlayback);
                pcm_index = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmMRGrxPlayback);
            }
            else
            {
                card_index = AudioALSADeviceParser::getInstance()->GetCardIndexByString(keypcmFMI2SPlayback);
                if(mOuput_device == AUDIO_DEVICE_OUT_SPEAKER)
                {
#ifdef MTK_MAXIM_SPEAKER_SUPPORT
                    pcm_index = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmFmPlaybackextSpk);
#else
                    pcm_index = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmFMI2SPlayback);
#endif
                }
                else
                {
                    pcm_index = AudioALSADeviceParser::getInstance()->GetPcmIndexByString(keypcmFMI2SPlayback);
                }
            }

            ALOGD("%s(), card_index = %d, pcm_index = %d", __FUNCTION__, card_index, pcm_index);
            mPcm = pcm_open(card_index, pcm_index , PCM_OUT, &mConfig);
            ALOGD("%s(), pcm_open mPcm = %p", __FUNCTION__, mPcm);
        }
        if (mPcm == NULL || pcm_is_ready(mPcm) == false)
        {
            ALOGE("%s(), Unable to open mPcm device %u (%s)", __FUNCTION__, pcm_index , pcm_get_error(mPcm));
        }
        pcm_start(mPcm);
    }
    else
    {
        if (mPcm != NULL)
        {
            AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

            pcm_stop(mPcm);
            pcm_close(mPcm);
            mPcm = NULL;
        }
    }


    // Update Direct Mode Status
    mIsFmDirectConnectionMode = enable;

    // Update (HW_GAIN2) Volume for Direct Mode Only
    if (mIsFmDirectConnectionMode == true)
    {
        setFmVolume(mFmVolume);
    }


    ALOGD("-%s(), enable = %d, bforce = %d", __FUNCTION__, enable, bforce);
    return NO_ERROR;
}

status_t AudioALSAFMController::setFmVolume(const float fm_volume)
{
    ALOGD("+%s(), mFmVolume = %f => fm_volume = %f", __FUNCTION__, mFmVolume, fm_volume);

    const float kMaxFmVolume = 1.0;
    ASSERT(0 <= fm_volume && fm_volume <= kMaxFmVolume); // valid volume value: 0.0 ~ 1.0

    mFmVolume = fm_volume;

    // Set HW Gain for Direct Mode // TODO(Harvey): FM Volume
    if (mFmEnable == true && mIsFmDirectConnectionMode == true)
    {
        mAudioALSAVolumeController->setFmVolume(mFmVolume);
    }
    else
    {
        ALOGD("%s(), Do nothing. mFMEnable = %d, mIsFmDirectConnectionMode = %d", __FUNCTION__, mFmEnable, mIsFmDirectConnectionMode);
    }

    ALOGD("-%s(), mFmVolume = %f", __FUNCTION__, mFmVolume);
    return NO_ERROR;
}

/*==============================================================================
 *                     WCN FM Chip Control
 *============================================================================*/

bool AudioALSAFMController::getFmChipPowerInfo()
{
    return WCNChipController::GetInstance()->GetFmChipPowerInfo();
}

} // end of namespace android
