#include "AudioALSAVoiceWakeUpController.h"

#include "AudioLock.h"
#include "AudioALSADriverUtility.h"

#include "AudioMTKHeadsetMessager.h"

#include <AudioCustParam.h>

#define LOG_TAG "AudioALSAVoiceWakeUpController"
#define VOW_POWER_ON_SLEEP_MS 50
namespace android
{

AudioALSAVoiceWakeUpController *AudioALSAVoiceWakeUpController::mAudioALSAVoiceWakeUpController = NULL;
AudioALSAVoiceWakeUpController *AudioALSAVoiceWakeUpController::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSAVoiceWakeUpController == NULL)
    {
        mAudioALSAVoiceWakeUpController = new AudioALSAVoiceWakeUpController();
    }
    ASSERT(mAudioALSAVoiceWakeUpController != NULL);
    return mAudioALSAVoiceWakeUpController;
}

AudioALSAVoiceWakeUpController::AudioALSAVoiceWakeUpController() :
    mMixer(AudioALSADriverUtility::getInstance()->getMixer()),
    mEnable(false),
    mIsUseHeadsetMic(false),
    mIsNeedToUpdateParamToKernel(true)
{
    ALOGV("%s()", __FUNCTION__);

    mIsUseHeadsetMic = AudioMTKHeadSetMessager::getInstance()->isHeadsetPlugged();

    GetVoiceRecogCustParamFromNV(&mVRParam);
    updateParamToKernel();
}

AudioALSAVoiceWakeUpController::~AudioALSAVoiceWakeUpController()
{
    ALOGV("%s()", __FUNCTION__);
}


bool AudioALSAVoiceWakeUpController::getVoiceWakeUpEnable()
{
    AudioAutoTimeoutLock _l(mLock);
    return mEnable;
}


status_t AudioALSAVoiceWakeUpController::setVoiceWakeUpEnable(const bool enable)
{
    ALOGD("+%s(), mEnable: %d => %d, mIsUseHeadsetMic = %d", __FUNCTION__, mEnable, enable, mIsUseHeadsetMic);
    AudioAutoTimeoutLock _l(mLock);

    if (mEnable == enable)
    {
        ALOGW("-%s(), enable(%d) == mEnable(%d), return", __FUNCTION__, enable, mEnable);
        return INVALID_OPERATION;
    }

    if (enable == true)
    {
        updateParamToKernel();

        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Vow_ADC_Func_Switch"), "On"))
        {
            ALOGE("Error: Audio_Vow_ADC_Func_Switch invalid value");
        }

        usleep(VOW_POWER_ON_SLEEP_MS * 1000);

        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Vow_Digital_Func_Switch"), "On"))
        {
            ALOGE("Error: Audio_Vow_Digital_Func_Switch invalid value");
        }
    }
    else
    {

        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Vow_Digital_Func_Switch"), "Off"))
        {
            ALOGE("Error: Audio_Vow_Digital_Func_Switch invalid value");
        }


        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Vow_ADC_Func_Switch"), "Off"))
        {
            ALOGE("Error: Audio_Vow_ADC_Func_Switch invalid value");
        }
    }

    mEnable = enable;

    ALOGD("-%s()", __FUNCTION__);
    return NO_ERROR;
}


status_t AudioALSAVoiceWakeUpController::updateDeviceInfoForVoiceWakeUp()
{
    ALOGD("+%s(), mIsUseHeadsetMic = %d", __FUNCTION__, mIsUseHeadsetMic);

    bool bIsUseHeadsetMic = AudioMTKHeadSetMessager::getInstance()->isHeadsetPlugged();

    if (bIsUseHeadsetMic != mIsUseHeadsetMic)
    {
        if (mEnable == false)
        {
            mIsUseHeadsetMic = bIsUseHeadsetMic;
        }
        else
        {
            setVoiceWakeUpEnable(false);
            mIsUseHeadsetMic = bIsUseHeadsetMic;
            setVoiceWakeUpEnable(true);
        }
    }

    ALOGD("-%s(), mIsUseHeadsetMic = %d", __FUNCTION__, mIsUseHeadsetMic);
    return NO_ERROR;
}


status_t AudioALSAVoiceWakeUpController::SetVOWCustParam(int index, int value)
{
    AudioAutoTimeoutLock _l(mLock);
    int vow_index = 5;
    mIsNeedToUpdateParamToKernel = true;

    if (SetVOWCustParamToNV(index, value) == 0)
    {
        mVRParam.cust_param[vow_index][index] = value;
    }
    return NO_ERROR;
}


status_t AudioALSAVoiceWakeUpController::updateParamToKernel()
{
    if (mIsNeedToUpdateParamToKernel == true)
    {
        mIsNeedToUpdateParamToKernel = false;

        int vow_index = 5;
        int mVOW_CFG2 = 0;
        int mVOW_CFG3 = 0;
        int mVOW_CFG4 = 0;
        mVOW_CFG2 = ((mVRParam.cust_param[vow_index][5] & 0x0007) << 12) |
                    ((mVRParam.cust_param[vow_index][6] & 0x0007) << 8)  |
                    ((mVRParam.cust_param[vow_index][7] & 0x0007) << 4)  |
                    ((mVRParam.cust_param[vow_index][8] & 0x0007));
        mVOW_CFG3 = ((mVRParam.cust_param[vow_index][0] & 0x000f) << 12) |
                    ((mVRParam.cust_param[vow_index][1] & 0x000f) << 8)  |
                    ((mVRParam.cust_param[vow_index][2] & 0x000f) << 4)  |
                    ((mVRParam.cust_param[vow_index][3] & 0x000f));
        mVOW_CFG4 = mVRParam.cust_param[vow_index][4];

        //reenable when NVRAM's initial value is correct
        if (mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio VOWCFG2 Data"), 0, mVOW_CFG2))
        {
            ALOGE("Error: Audio VOWCFG2 Data invalid value");
        }

        if (mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio VOWCFG3 Data"), 0, mVOW_CFG3))
        {
            ALOGE("Error: Audio VOWCFG3 Data invalid value");
        }

        if (mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio VOWCFG4 Data"), 0, mVOW_CFG4))
        {
            ALOGE("Error: Audio VOWCFG4 Data invalid value");
        }

    }
    return NO_ERROR;
}

bool AudioALSAVoiceWakeUpController::getVoiceWakeUpStateFromKernel()
{
    ALOGD("%s(), not support", __FUNCTION__);

    return false;
}

} // end of namespace android
