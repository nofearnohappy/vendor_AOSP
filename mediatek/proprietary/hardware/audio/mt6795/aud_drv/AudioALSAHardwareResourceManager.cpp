#include "AudioALSAHardwareResourceManager.h"

#include <utils/threads.h>

#include "AudioType.h"
#include "AudioLock.h"
#include "AudioAssert.h"

#include "AudioALSADriverUtility.h"

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
#include "AudioALSASpeakerMonitor.h"
#endif

// TMP
#include "AudioALSACodecDeviceOutEarphonePMIC.h"
#include "AudioALSACodecDeviceOutReceiverPMIC.h"
#include "AudioALSACodecDeviceOutSpeakerNXP.h"
#include "AudioALSACodecDeviceOutSpeakerPMIC.h"
#include "AudioALSACodecDeviceOutSpeakerEarphonePMIC.h"
#include "AudioALSADeviceConfigManager.h"

static   const char PROPERTY_KEY_EXTDAC[PROPERTY_KEY_MAX]  = "af.resouce.extdac_support";

#define LOG_TAG "AudioALSAHardwareResourceManager"

namespace android
{

static const char *SIDEGEN[] =
{
    "I0I1",   "I2",     "I3I4",   "I5I6",
    "I7I8",   "I9",     "I10I11", "I12I13",
    "I14",    "I15I16", "I17I18", "I19I20",
    "I21I22", "O0O1",   "O2",     "O3O4",
    "O5O6",   "O7O8",   "O9O10",  "O11",
    "O12",    "O13O14", "O15O16", "O17O18",
    "O19O20", "O21O22", "O23O24", "OFF"
};

AudioALSAHardwareResourceManager *AudioALSAHardwareResourceManager::mAudioALSAHardwareResourceManager = NULL;
AudioALSAHardwareResourceManager *AudioALSAHardwareResourceManager::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSAHardwareResourceManager == NULL)
    {
        mAudioALSAHardwareResourceManager = new AudioALSAHardwareResourceManager();
    }
    ASSERT(mAudioALSAHardwareResourceManager != NULL);
    return mAudioALSAHardwareResourceManager;
}

status_t AudioALSAHardwareResourceManager::ResetDevice(void)
{
	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Amp_R_Switch"), "Off"))
	{
		ALOGE("Error: Audio_Amp_R_Switch invalid value");
	}
	
	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Amp_L_Switch"), "Off"))
	{
		ALOGE("Error: Audio_Amp_L_Switch invalid value");
	}

	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Voice_Amp_Switch"), "Off"))
	{
		ALOGE("Error: Voice_Amp_Switch invalid value");
	}

	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Speaker_Amp_Switch"), "Off"))
	{
		ALOGE("Error: Speaker_Amp_Switch invalid value");
	}

	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Headset_Speaker_Amp_Switch"), "Off"))
	{
		ALOGE("Error: Headset_Speaker_Amp_Switch invalid value");
	}

	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Ext_Speaker_Amp_Switch"), "Off"))
	{
		ALOGE("Error: Ext_Speaker_Amp_Switch invalid value");
	}

	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_1_Switch"), "Off"))
	{
		ALOGE("Error: Headset_Speaker_Amp_Switch invalid value");
	}

	if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_2_Switch"), "Off"))
	{
		ALOGE("Error: Headset_Speaker_Amp_Switch invalid value");
	}
	
    return NO_ERROR;

}

AudioALSAHardwareResourceManager::AudioALSAHardwareResourceManager() :
    mMixer(AudioALSADriverUtility::getInstance()->getMixer()),
    mPcmDL(NULL),
    mOutputDevices(AUDIO_DEVICE_NONE),
    mInputDevice(AUDIO_DEVICE_NONE),
    mOutputDeviceSampleRate(44100),
    mInputDeviceSampleRate(48000),
    mIsChangingInputDevice(false),
    mStartOutputDevicesCount(0),
    mStartInputDeviceCount(0),
    mMicInverse(false),
    mBuiltInMicSpecificType(BUILTIN_MIC_DEFAULT),
    mNumHSPole(4),
    mHeadchange(false)
{
    ALOGD("%s()", __FUNCTION__);
	ResetDevice();	
    AudioALSACodecDeviceOutEarphonePMIC::getInstance();
    mDeviceConfigManager = AudioALSADeviceConfigManager::getInstance();
    setMicType();
#ifdef NXP_SMARTPA_SUPPORT
    AudioALSACodecDeviceOutSpeakerNXP::getInstance();
#endif
}


AudioALSAHardwareResourceManager::~AudioALSAHardwareResourceManager()
{
    ALOGD("%s()", __FUNCTION__);
}


/**
 * output devices
 */
status_t AudioALSAHardwareResourceManager::setOutputDevice(const audio_devices_t new_devices, const uint32_t sample_rate)
{
    ALOGD("+%s(), new_devices = 0x%x, mStartOutputDevicesCount = %u", __FUNCTION__, new_devices, mStartOutputDevicesCount);

    ASSERT(mStartOutputDevicesCount == 0);

    mOutputDevices = new_devices;
    mOutputDeviceSampleRate = sample_rate;

    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::startOutputDevice(const audio_devices_t new_devices, const uint32_t SampleRate)
{
    ALOGD("+%s(), new_devices = 0x%x, mOutputDevices = 0x%x, mStartOutputDevicesCount = %u SampleRate = %d",
          __FUNCTION__, new_devices, mOutputDevices, mStartOutputDevicesCount, SampleRate);

    AudioAutoTimeoutLock _l(mLock);

    if (new_devices == mOutputDevices)
    {
        // don't need to do anything
    }
    else if(AUDIO_DEVICE_NONE != mOutputDevices)
    {
        changeOutputDevice_l(new_devices, SampleRate);
    }
    else
    {
        startOutputDevice_l(new_devices, SampleRate);
    }

    mStartOutputDevicesCount++;

    ALOGD("-%s(), mOutputDevices = 0x%x, mStartOutputDevicesCount = %u", __FUNCTION__, mOutputDevices, mStartOutputDevicesCount);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::stopOutputDevice()
{
    ALOGD("+%s(), mOutputDevices = 0x%x, mStartOutputDevicesCount = %u", __FUNCTION__, mOutputDevices, mStartOutputDevicesCount);

    AudioAutoTimeoutLock _l(mLock);

    if (mStartOutputDevicesCount > 1)
    {
        // don't need to do anything
    }
    else
    {
        ASSERT(mStartOutputDevicesCount == 1 && mOutputDevices != AUDIO_DEVICE_NONE);
        stopOutputDevice_l();
    }

    mStartOutputDevicesCount--;

    ALOGD("-%s(), mOutputDevices = 0x%x, mStartOutputDevicesCount = %u", __FUNCTION__, mOutputDevices, mStartOutputDevicesCount);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::changeOutputDevice(const audio_devices_t new_devices)
{
    ALOGD("+%s(), mOutputDevices: 0x%x => 0x%x", __FUNCTION__, mOutputDevices, new_devices);

    AudioAutoTimeoutLock _l(mLock);
    changeOutputDevice_l(new_devices, mOutputDeviceSampleRate);

    ALOGD("-%s(), mOutputDevices: 0x%x", __FUNCTION__, mOutputDevices);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::startOutputDevice_l(const audio_devices_t new_devices, const uint32_t SampleRate)
{
    if (new_devices == (AUDIO_DEVICE_OUT_SPEAKER | AUDIO_DEVICE_OUT_WIRED_HEADSET) ||
        new_devices == (AUDIO_DEVICE_OUT_SPEAKER | AUDIO_DEVICE_OUT_WIRED_HEADPHONE))
    {
        OpenHeadphoneSpeakerPath(SampleRate);
    }
    else if (new_devices == AUDIO_DEVICE_OUT_EARPIECE)
    {
        OpenReceiverPath(SampleRate);
    }
    else if (new_devices == AUDIO_DEVICE_OUT_SPEAKER)
    {
        OpenSpeakerPath(SampleRate);
    }
    else if (new_devices == AUDIO_DEVICE_OUT_WIRED_HEADSET ||
             new_devices == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
    {
        OpenHeadphonePath(SampleRate);
    }

    mOutputDevices = new_devices;
    mOutputDeviceSampleRate = SampleRate;

    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::stopOutputDevice_l()
{
    if (mOutputDevices == (AUDIO_DEVICE_OUT_SPEAKER | AUDIO_DEVICE_OUT_WIRED_HEADSET) ||
        mOutputDevices == (AUDIO_DEVICE_OUT_SPEAKER | AUDIO_DEVICE_OUT_WIRED_HEADPHONE))
    {
        CloseHeadphoneSpeakerPath();
    }
    else if (mOutputDevices == AUDIO_DEVICE_OUT_EARPIECE)
    {
        CloseReceiverPath();
    }
    else if (mOutputDevices == AUDIO_DEVICE_OUT_SPEAKER)
    {
        CloseSpeakerPath();
    }
    else if (mOutputDevices == AUDIO_DEVICE_OUT_WIRED_HEADSET ||
             mOutputDevices == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
    {
        CloseHeadphonePath();
    }


    mOutputDevices = AUDIO_DEVICE_NONE;

    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::changeOutputDevice_l(const audio_devices_t new_devices, const uint32_t SampleRate)
{
    stopOutputDevice_l();
    startOutputDevice_l(new_devices, SampleRate);

    return NO_ERROR;
}

bool AudioALSAHardwareResourceManager::isSharedOutDevice(audio_devices_t device)
{
    // these devices cannot exist at the same time
    audio_devices_t sharedDevice = AUDIO_DEVICE_OUT_EARPIECE |
                                   AUDIO_DEVICE_OUT_SPEAKER |
                                   AUDIO_DEVICE_OUT_WIRED_HEADSET |
                                   AUDIO_DEVICE_OUT_WIRED_HEADPHONE;

    if ((device & ~sharedDevice) != 0)
    {
        return false;
    }

    return true;
}

/**
 * input devices
 */
status_t AudioALSAHardwareResourceManager::setInputDevice(const audio_devices_t new_devices)
{
    ALOGD("+%s(), new_devices = 0x%x, mStartInputDeviceCount = %u", __FUNCTION__, new_devices, mStartInputDeviceCount);

    return NO_ERROR;
}

void AudioALSAHardwareResourceManager::setMIC1Mode(bool isphonemic)
{

}

void AudioALSAHardwareResourceManager::setMIC2Mode(bool isphonemic)
{

}

status_t AudioALSAHardwareResourceManager::startInputDevice(const audio_devices_t new_device)
{
    ALOGD("+%s(), new_device = 0x%x, mInputDevice = 0x%x, mStartInputDeviceCount = %d", __FUNCTION__, new_device, mInputDevice, mStartInputDeviceCount);

    AudioAutoTimeoutLock _l(mLockin);

    if (((mInputDevice & new_device) & ~AUDIO_DEVICE_BIT_IN) != 0)
    {
        ALOGW("%s(), input device already opened", __FUNCTION__);
        if (new_device != AUDIO_DEVICE_IN_SPK_FEED)
        {
            mStartInputDeviceCount++;
        }
        ALOGD("-%s(), mInputDevice = 0x%x, mStartInputDeviceCount = %d", __FUNCTION__, mInputDevice, mStartInputDeviceCount);
        return NO_ERROR;
    }


    int retval = 0;
    setMicType();  // first set mic type
    if (new_device == AUDIO_DEVICE_IN_BUILTIN_MIC)
    {
        /*
        if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC3_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC3);

            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_3_Switch"), "On");
            ASSERT(retval == 0);

        }
        else
        {
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_1_Switch"), "On");
            ASSERT(retval == 0);
        #ifdef MTK_DUAL_MIC_SUPPORT
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_2_Switch"), "On");
            ASSERT(retval == 0);
        #endif
        }

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "IN_ADC1");
        ASSERT(retval == 0);
        */

        if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC1_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC1);
            /*
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC1");
            ASSERT(retval == 0);
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC1");
            ASSERT(retval == 0);
            */
        }
        else if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC2_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC2);
            /*
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "IN_ADC2");
            ASSERT(retval == 0);
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp2_Switch"), "IN_ADC2");
            ASSERT(retval == 0);
            */
        }
        else if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC3_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC3);
            /*
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC3");
            ASSERT(retval == 0);
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC3");
            ASSERT(retval == 0);
            */
        }
        else if (mBuiltInMicSpecificType == BUILTIN_MIC_DEFAULT)
        {
#ifdef MTK_DUAL_MIC_SUPPORT
            mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_DUAL_MIC);
            if (mMicInverse == true)
            {
                ALOGD("%s(), need MicInverse ", __FUNCTION__);
                mDeviceConfigManager->ApplyDeviceSettingByName(AUDIO_MIC_INVERSE);
                /*
                retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC2");
                ASSERT(retval == 0);
                retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC1");
                ASSERT(retval == 0);
                */
            }
            else
            {
                mDeviceConfigManager->ApplyDeviceSettingByName(AUDIO_MIC_NOINVERSE);
                /*
                retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC1");
                ASSERT(retval == 0);
                retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC2");
                ASSERT(retval == 0);
                */
            }
#else
            mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_SINGLE_MIC);
            /*
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC1");
            ASSERT(retval == 0);
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC1");
            ASSERT(retval == 0);
            */
#endif
        }
    }
#ifdef MTK_DUAL_MIC_SUPPORT
    else if (new_device == AUDIO_DEVICE_IN_BACK_MIC)
    {
        mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_BUILTIN_BACK_MIC);
        /*
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_2_Switch"), "On");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "IN_ADC1");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC2");
        ASSERT(retval == 0);
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC2");
        ASSERT(retval == 0);
        */
    }
#endif
    else if (new_device == AUDIO_DEVICE_IN_WIRED_HEADSET)
    {
        mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_HEADSET_MIC);
        /*
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_1_Switch"), "On");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "IN_ADC2");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource1_Setting"), "ADC1");
        ASSERT(retval == 0);
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource2_Setting"), "ADC1");
        ASSERT(retval == 0);
        */
    }
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
    else if (new_device == AUDIO_DEVICE_IN_SPK_FEED)
    {
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_3_Switch"), "On");
        ASSERT(retval == 0);
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_4_Switch"), "On");
        ASSERT(retval == 0);
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource3_Setting"), "ADC3");
        ASSERT(retval == 0);
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MicSource4_Setting"), "ADC4");
        ASSERT(retval == 0);
    }
#endif

    mInputDevice |= new_device;
    if (new_device != AUDIO_DEVICE_IN_SPK_FEED)
    {
        mStartInputDeviceCount++;
    }

    ALOGD("-%s(), mInputDevice = 0x%x, mStartInputDeviceCount = %d", __FUNCTION__, mInputDevice, mStartInputDeviceCount);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::stopInputDevice(const audio_devices_t stop_device)
{
    ALOGD("+%s(), mInputDevice = 0x%x, stop_device = 0x%x, mStartInputDeviceCount = %d", __FUNCTION__, mInputDevice, stop_device, mStartInputDeviceCount);

    AudioAutoTimeoutLock _l(mLockin);

    if (((mInputDevice & stop_device) & ~AUDIO_DEVICE_BIT_IN) == 0)
    {
        ALOGW("%s(), input device not opened yet, do nothing", __FUNCTION__);
        return NO_ERROR;
    }

    if (stop_device != AUDIO_DEVICE_IN_SPK_FEED)
    {
        mStartInputDeviceCount--;
        if (mStartInputDeviceCount > 0)
        {
            ALOGD("-%s(), mInputDevice = 0x%x, mStartInputDeviceCount = %d", __FUNCTION__, mInputDevice, mStartInputDeviceCount);
            return NO_ERROR;
        }
    }

    int retval = 0;

    if (stop_device == AUDIO_DEVICE_IN_BUILTIN_MIC)
    {
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "OPEN");
        ASSERT(retval == 0);

        if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC1_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC1);
        }
        else if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC2_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC2);
        }
        else if (mBuiltInMicSpecificType == BUILTIN_MIC_MIC3_ONLY)
        {
            mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_BUILTIN_MIC_MIC3);
        }
        else if (mBuiltInMicSpecificType == BUILTIN_MIC_DEFAULT)
        {
            /*
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_1_Switch"), "Off");
            ASSERT(retval == 0);
            */
#ifdef MTK_DUAL_MIC_SUPPORT
            mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_BUILTIN_DUAL_MIC);
#else
            mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_BUILTIN_SINGLE_MIC);
            /*
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_2_Switch"), "Off");
            ASSERT(retval == 0);
            */
#endif
        }
    }
#ifdef MTK_DUAL_MIC_SUPPORT
    else if (stop_device == AUDIO_DEVICE_IN_BACK_MIC)
    {
        mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_BUILTIN_BACK_MIC);
        /*
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "OPEN");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_2_Switch"), "Off");
        ASSERT(retval == 0);
        */
    }
#endif
    else if (stop_device == AUDIO_DEVICE_IN_WIRED_HEADSET)
    {
        mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_HEADSET_MIC);
        /*
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Preamp1_Switch"), "OPEN");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_1_Switch"), "Off");
        ASSERT(retval == 0);
        */
    }
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
    else if (stop_device == AUDIO_DEVICE_IN_SPK_FEED)
    {
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_3_Switch"), "Off");
        ASSERT(retval == 0);

        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_ADC_4_Switch"), "Off");
        ASSERT(retval == 0);
    }
#endif

    mInputDevice &= ((~stop_device) | AUDIO_DEVICE_BIT_IN);
    if (mInputDevice == AUDIO_DEVICE_BIT_IN) { mInputDevice = AUDIO_DEVICE_NONE; }

    ALOGD("-%s(), mInputDevice = 0x%x, mStartInputDeviceCount = %d", __FUNCTION__, mInputDevice, mStartInputDeviceCount);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::changeInputDevice(const audio_devices_t new_device)
{
    ALOGD("+%s(), mInputDevice: 0x%x => 0x%x", __FUNCTION__, mInputDevice, new_device);

    stopInputDevice(getInputDevice());
    startInputDevice(new_device);

    ALOGD("-%s(), mInputDevice: 0x%x", __FUNCTION__, mInputDevice);
    return NO_ERROR;
}



status_t AudioALSAHardwareResourceManager::setHWGain2DigitalGain(const uint32_t gain)
{
    ALOGD("%s(), gain = 0x%x", __FUNCTION__, gain);

    const uint32_t kMaxAudioHWGainValue = 0x80000;

    if (gain > kMaxAudioHWGainValue)
    {
        ALOGE("%s(), gain(0x%x) > kMaxAudioHWGainValue(0x%x)!! return!!", __FUNCTION__, gain, kMaxAudioHWGainValue);
        return BAD_VALUE;
    }

    int retval = mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio Mrgrx Volume"), 0, gain);
    ASSERT(retval == 0);

    return (retval == 0) ? NO_ERROR : UNKNOWN_ERROR;
}


status_t AudioALSAHardwareResourceManager::setInterruptRate(const uint32_t rate)
{
    if (rate <= 0 || rate >= 65535)
    {
        ALOGE("%s, rate is not in range", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    int retval = mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio IRQ1 CNT"), 0, rate);
    ASSERT(retval == 0);

    return (retval == 0) ? NO_ERROR : UNKNOWN_ERROR;
}


status_t AudioALSAHardwareResourceManager::setInterruptRate2(const uint32_t rate)
{
    if (rate <= 0 || rate >= 65535)
    {
        ALOGE("%s, rate is not in range", __FUNCTION__);
        return UNKNOWN_ERROR;
    }

    int retval = mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio IRQ2 CNT"), 0, rate);
    ASSERT(retval == 0);

    return (retval == 0) ? NO_ERROR : UNKNOWN_ERROR;
}


status_t AudioALSAHardwareResourceManager::openAddaOutput(const uint32_t sample_rate)
{
    ALOGD("+%s(), sample_rate = 0x%x", __FUNCTION__, sample_rate);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());
    struct pcm_config mConfig;

    // DL  setting
    memset(&mConfig, 0, sizeof(mConfig));
    mConfig.channels = 2;
    mConfig.rate = sample_rate;
    mConfig.period_size = 1024;
    mConfig.period_count = 2;
    mConfig.format = PCM_FORMAT_S16_LE;
    mConfig.start_threshold = 0;
    mConfig.stop_threshold = 0;
    mConfig.silence_threshold = 0;

    ASSERT(mPcmDL == NULL);
    mPcmDL = pcm_open(0, 8, PCM_OUT, &mConfig);
    ASSERT(mPcmDL != NULL);
    ALOGV("%s(), mPcmDL = %p", __FUNCTION__, mPcmDL);

    pcm_start(mPcmDL);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::closeAddaOutput()
{
    ALOGD("+%s()", __FUNCTION__);
    AudioAutoTimeoutLock _l(*AudioALSADriverUtility::getInstance()->getStreamSramDramLock());

    pcm_stop(mPcmDL);
    pcm_close(mPcmDL);
    mPcmDL = NULL;

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::setSgenMode(const sgen_mode_t sgen_mode)
{
    if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_SideGen_Switch"), SIDEGEN[sgen_mode]))
    {
        ALOGE("Error: Audio_SideGen_Switch invalid value");
    }

    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::setSgenSampleRate(const sgen_mode_samplerate_t sample_rate)
{
    ALOGW("%s()", __FUNCTION__);
    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::EnableSideToneFilter(const bool enable)
{

#ifdef ENABLE_SW_SIDETONE
    ALOGD("%s(), SW Sidetone Applied, skip setting HW Sidetone", __FUNCTION__);

#else
    ALOGD("EnableSideToneFilter enable = %d", enable);
    if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Sidetone_Switch"), enable ? "On" : "Off"))
    {
        ALOGE("Error: Audio_Sidetone_Switch invalid value");
    }
#endif

    return NO_ERROR;
}

bool AudioALSAHardwareResourceManager::GetExtDacPropertyEnable()
{
    return AudioALSADriverUtility::getInstance()->GetPropertyValue(PROPERTY_KEY_EXTDAC);
}

status_t AudioALSAHardwareResourceManager::SetExtDacGpioEnable(bool bEnable)
{
    int retval = 0;
    // check for property first
    if (GetExtDacPropertyEnable() == true)
    {
        ALOGD("%s GetExtDacPropertyEnable bEnable = true", __FUNCTION__, bEnable);
        if (bEnable == true)
        {
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_I2S1_Setting"), "On");
            ASSERT(retval == 0);
        }
        else
        {
            retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_I2S1_Setting"), "Off");
            ASSERT(retval == 0);
        }
        return NO_ERROR;
    }

    // if supprot for extdac
#ifdef EXT_DAC_SUPPORT
    if (bEnable == true)
    {
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_I2S1_Setting"), "On");
        ASSERT(retval == 0);
    }
    else
    {
        retval = mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_I2S1_Setting"), "Off");
        ASSERT(retval == 0);
    }
#endif
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::setMicType(void)
{
    if (IsAudioSupportFeature(AUDIO_SUPPORT_DMIC))
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC1_Mode_Select"), "DMIC"))
        {
            ALOGE("Error: Audio_MIC1_Mode_Select invalid value");
        }
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC2_Mode_Select"), "DMIC"))
        {
            ALOGE("Error: Audio_MIC2_Mode_Select invalid value");
        }
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC3_Mode_Select"), "DMIC"))
        {
            ALOGE("Error: Audio_MIC3_Mode_Select invalid value");
        }
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC4_Mode_Select"), "DMIC"))
        {
            ALOGE("Error: Audio_MIC4_Mode_Select invalid value");
        }
    }
    else
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC1_Mode_Select"), "ACCMODE"))
        {
            ALOGE("Error: Audio_MIC1_Mode_Select invalid value");
        }
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC2_Mode_Select"), "ACCMODE"))
        {
            ALOGE("Error: Audio_MIC2_Mode_Select invalid value");
        }
#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC3_Mode_Select"), "DCCMODE"))
        {
            ALOGE("Error: Audio_MIC3_Mode_Select invalid value");
        }
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC4_Mode_Select"), "DCCMODE"))
        {
            ALOGE("Error: Audio_MIC4_Mode_Select invalid value");
        }
#else
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC3_Mode_Select"), "ACCMODE"))
        {
            ALOGE("Error: Audio_MIC3_Mode_Select invalid value");
        }
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_MIC4_Mode_Select"), "ACCMODE"))
        {
            ALOGE("Error: Audio_MIC4_Mode_Select invalid value");
        }
#endif
    }
    return NO_ERROR;
}


status_t AudioALSAHardwareResourceManager::setSPKCurrentSensor(bool bSwitch)
{
    struct mixer_ctl *ctl;
    struct mixer_ctl *ctl2;

    enum mixer_ctl_type type;
    unsigned int num_values;
    static AUDIO_SPEAKER_MODE eSpeakerMode;
    ALOGD("%s(), bSwitch = %d", __FUNCTION__, bSwitch);
    ctl = mixer_get_ctl_by_name(mMixer, "Audio_Speaker_CurrentSensing");

    if (ctl == NULL)
    {
        ALOGE("Kernel unsupport Audio_Speaker_CurrentSensing");
        return UNKNOWN_ERROR;
    }

    if (mixer_ctl_set_enum_by_string(ctl, bSwitch ? "On" : "Off"))
    {
        ALOGE("Error: Audio_Speaker_CurrentSensing invalid value : On");
    }


    ctl2 = mixer_get_ctl_by_name(mMixer, "Audio_Speaker_class_Switch");

    if (true == bSwitch)
    {
        //Save Speaker Mode
        ALOGD("Enable+ value [%d] [%s]", mixer_ctl_get_value(ctl2, 0), mixer_ctl_get_enum_string(ctl2, mixer_ctl_get_value(ctl2, 0)));

        if (strcmp(mixer_ctl_get_enum_string(ctl2, mixer_ctl_get_value(ctl2, 0)), "CLASSAB"))
        {
            eSpeakerMode = AUDIO_SPEAKER_MODE_D;
        }
        else
        {
            eSpeakerMode = AUDIO_SPEAKER_MODE_AB;
        }

        ALOGD("Current Mode [%d]", eSpeakerMode);

        if (mixer_ctl_set_enum_by_string(ctl2, "CLASSAB"))
        {
            ALOGE("Error: Audio_Speaker_CurrentPeakDetector invalid value");
        }

        ALOGD("Enable- [%s]", mixer_ctl_get_enum_string(ctl2, mixer_ctl_get_value(ctl2, 0)));

    }
    else
    {
        //restore Speaker Mode

        if (mixer_ctl_set_enum_by_string(ctl2, eSpeakerMode ? "CLASSAB" : "CALSSD"))
        {
            ALOGE("Error: Audio_Speaker_CurrentPeakDetector invalid value");
        }

        ALOGD("RollBack to [%s]", mixer_ctl_get_enum_string(ctl2, 0));
    }


    ALOGD("Audio_Speaker_CurrentSensing Get value [%d] [%s]", mixer_ctl_get_value(ctl, 0), mixer_ctl_get_enum_string(ctl, mixer_ctl_get_value(ctl, 0)));

    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::setSPKCurrentSensorPeakDetectorReset(bool bSwitch)
{
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    unsigned int num_values;
    ALOGD("%s(), bSwitch = %d", __FUNCTION__, bSwitch);
    ctl = mixer_get_ctl_by_name(mMixer, "Audio_Speaker_CurrentPeakDetector");

    if (ctl == NULL)
    {
        ALOGE("Kernel unsupport Audio_Speaker_CurrentPeakDetector");
        return UNKNOWN_ERROR;
    }

    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    if (mixer_ctl_set_enum_by_string(ctl, bSwitch ? "On" : "Off"))
    {
        ALOGE("Error: Audio_Speaker_CurrentPeakDetector invalid value");
    }

    return NO_ERROR;
}


audio_devices_t AudioALSAHardwareResourceManager::getOutputDevice()
{
    return mOutputDevices;
}

audio_devices_t AudioALSAHardwareResourceManager::getInputDevice()
{
    return mInputDevice & ((~AUDIO_DEVICE_IN_SPK_FEED) | AUDIO_DEVICE_BIT_IN);
}


status_t AudioALSAHardwareResourceManager::setMicInverse(bool bMicInverse)
{
    ALOGD("%s(), bMicInverse = %d", __FUNCTION__, bMicInverse);
#ifdef MTK_DUAL_MIC_SUPPORT
    mMicInverse = bMicInverse;
#else
    ALOGD("%s(), not support", __FUNCTION__);
#endif
    return NO_ERROR;
}

void AudioALSAHardwareResourceManager::EnableAudBufClk(bool bEanble)
{
    ALOGD("%s(), bEanble = %d, mStartOutputDevicesCount %d", __FUNCTION__, bEanble, mStartOutputDevicesCount);

    if(mStartOutputDevicesCount)
        return;

    enum mixer_ctl_type type;
    struct mixer_ctl *ctl;
    int retval = 0;
    ctl = mixer_get_ctl_by_name(mMixer, "AUD_CLK_BUF_Switch");

    if (ctl == NULL)
    {
        ALOGE("EnableAudBufClk not support");
        return ;
    }

    if (bEanble == true)
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

status_t AudioALSAHardwareResourceManager::OpenReceiverPath(const uint32_t SampleRate)
{
    //AudioALSACodecDeviceOutReceiverPMIC::getInstance()->open();
    mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_RECEIVER);
    SetExtDacGpioEnable(true);
    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::CloseReceiverPath()
{
    ALOGD("%s(), AUDIO_DEVICE_OUT_EARPIECE", __FUNCTION__);
    //AudioALSACodecDeviceOutReceiverPMIC::getInstance()->close();
    mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_RECEIVER);
    SetExtDacGpioEnable(false);
    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::OpenHeadphonePath(const uint32_t SampleRate)
{
    SetExtDacGpioEnable(true);
    //AudioALSACodecDeviceOutEarphonePMIC::getInstance()->open();
    mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_HEADPHONE);
    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::CloseHeadphonePath()
{
    ALOGD("%s(), AUDIO_DEVICE_OUT_WIRED_HEADSET | AUDIO_DEVICE_OUT_WIRED_HEADPHONE", __FUNCTION__);
    //AudioALSACodecDeviceOutEarphonePMIC::getInstance()->close();
    mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_HEADPHONE);
    SetExtDacGpioEnable(false);
    return NO_ERROR;
}

status_t AudioALSAHardwareResourceManager::OpenSpeakerPath(const uint32_t SampleRate)
{
    SetExtDacGpioEnable(true);
#ifdef NXP_SMARTPA_SUPPORT
    AudioALSACodecDeviceOutSpeakerNXP::getInstance()->open(SampleRate);
#else
    mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_SPEAKER);
#endif

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
    AudioALSASpeakerMonitor::getInstance()->Activate();
#endif

    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::CloseSpeakerPath()
{
    ALOGD("%s(), AUDIO_DEVICE_OUT_SPEAKER", __FUNCTION__);
#ifdef NXP_SMARTPA_SUPPORT
    AudioALSACodecDeviceOutSpeakerNXP::getInstance()->close();
#else
    mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_SPEAKER);
#endif

#if defined(MTK_SPEAKER_MONITOR_SUPPORT)
    AudioALSASpeakerMonitor::getInstance()->Deactivate();
#endif

    SetExtDacGpioEnable(false);
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::OpenHeadphoneSpeakerPath(const uint32_t SampleRate)
{
    SetExtDacGpioEnable(true);
#ifdef NXP_SMARTPA_SUPPORT
    AudioALSACodecDeviceOutSpeakerNXP::getInstance()->open(SampleRate);
    mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_HEADPHONE);
    //AudioALSACodecDeviceOutEarphonePMIC::getInstance()->open();
#else
    mDeviceConfigManager->ApplyDeviceTurnonSequenceByName(AUDIO_DEVICE_EARPHONE_SPEAKER);
#endif
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::CloseHeadphoneSpeakerPath()
{
#ifdef NXP_SMARTPA_SUPPORT
    AudioALSACodecDeviceOutSpeakerNXP::getInstance()->close();
    mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_HEADPHONE);
#else
    //AudioALSACodecDeviceOutSpeakerEarphonePMIC::getInstance()->close();
    mDeviceConfigManager->ApplyDeviceTurnoffSequenceByName(AUDIO_DEVICE_EARPHONE_SPEAKER);
#endif
    SetExtDacGpioEnable(false);
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::OpenBuiltInMicPath()
{
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::CloseBuiltInMicPath()
{
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::OpenBackMicPath()
{
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::CloseBackMicPath()
{
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::OpenWiredHeadsetMicPath()
{
    return NO_ERROR;
}

status_t  AudioALSAHardwareResourceManager::CloseWiredHeadsetMicPath()
{
    return NO_ERROR;
}



bool AudioALSAHardwareResourceManager::getMicInverse(void)
{
    ALOGD("%s(), mMicInverse = %d", __FUNCTION__, mMicInverse);
    return mMicInverse;
}


void AudioALSAHardwareResourceManager::setAudioDebug(const bool enable)
{
    if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Debug_Setting"), enable ? "On" : "Off"));
    {
        ALOGE("Error: Audio_Debug_Setting invalid value");
    }
}


} // end of namespace android
