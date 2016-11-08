#include "AudioALSACodecDeviceOutEarphonePMIC.h"

#include "AudioLock.h"


#define LOG_TAG "AudioALSACodecDeviceOutEarphonePMIC"

namespace android
{

AudioALSACodecDeviceOutEarphonePMIC *AudioALSACodecDeviceOutEarphonePMIC::mAudioALSACodecDeviceOutEarphonePMIC = NULL;
AudioALSACodecDeviceOutEarphonePMIC *AudioALSACodecDeviceOutEarphonePMIC::getInstance()
{
    AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (mAudioALSACodecDeviceOutEarphonePMIC == NULL)
    {
        mAudioALSACodecDeviceOutEarphonePMIC = new AudioALSACodecDeviceOutEarphonePMIC();
    }
    ASSERT(mAudioALSACodecDeviceOutEarphonePMIC != NULL);
    return mAudioALSACodecDeviceOutEarphonePMIC;
}

status_t AudioALSACodecDeviceOutEarphonePMIC::DeviceDoDcCalibrate()
{
    ALOGD("%s()", __FUNCTION__);
    GetDcCalibrationParamFromNV(&mAudioBufferDcCalibrate);
    int HplOffset = 0 , HprOffset = 0;
    ALOGD("cali_flag = %d cali_val_hp_left = %d cali_val_hp_right = %d ",
          mAudioBufferDcCalibrate.cali_flag, mAudioBufferDcCalibrate.cali_val_hp_left, mAudioBufferDcCalibrate.cali_val_hp_right);

    if (mAudioBufferDcCalibrate.cali_flag  != true)
    {
        // do dc calibrate
        struct mixer_ctl *ctl;
        enum mixer_ctl_type type;
        struct mixer_ctl *ctl2;
        ctl = mixer_get_ctl_by_name(mMixer, "Audio HPL Offset");
        HplOffset = mixer_ctl_get_value(ctl, 0);
        ctl2 = mixer_get_ctl_by_name(mMixer, "Audio HPR Offset");
        HprOffset = mixer_ctl_get_value(ctl2, 0);
        mAudioBufferDcCalibrate.cali_flag = true;
        mAudioBufferDcCalibrate.cali_val_hp_left = HplOffset;
        mAudioBufferDcCalibrate.cali_val_hp_right = HprOffset;
        SetDcCalibrationParamToNV(&mAudioBufferDcCalibrate);
        ALOGD("calibrate cali_flag = %d cali_val_hp_left = %d cali_val_hp_right = %d ",
              mAudioBufferDcCalibrate.cali_flag, mAudioBufferDcCalibrate.cali_val_hp_left, mAudioBufferDcCalibrate.cali_val_hp_right);
    }
    else
    {
        int retval = 0;
        HplOffset = (short)mAudioBufferDcCalibrate.cali_val_hp_left;
        ALOGD("cali_val_hp_left = %d, short cali_val_hp_left = %d, HplOffset=%d ",
            mAudioBufferDcCalibrate.cali_val_hp_left, (short)mAudioBufferDcCalibrate.cali_val_hp_left,HplOffset);
        HprOffset = (short)mAudioBufferDcCalibrate.cali_val_hp_right;
        ALOGD("cali_val_hp_right = %d, short cali_val_hp_right = %d, HplOffset=%d ",
            mAudioBufferDcCalibrate.cali_val_hp_right, (short)mAudioBufferDcCalibrate.cali_val_hp_right,HprOffset);
        retval = mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio HPL Offset"), 0, HplOffset);
        //ASSERT(retval == 0);
        retval = mixer_ctl_set_value(mixer_get_ctl_by_name(mMixer, "Audio HPR Offset"), 0, HprOffset);
        //ASSERT(retval == 0);
    }

    GetDcCalibrationParamFromNV(&mAudioBufferDcCalibrate);
    ALOGD("cali_flag = %d cali_val_hp_left = %d cali_val_hp_right = %d, HplOffset=%d ",
          mAudioBufferDcCalibrate.cali_flag, mAudioBufferDcCalibrate.cali_val_hp_left, mAudioBufferDcCalibrate.cali_val_hp_right,HplOffset);
    return NO_ERROR;
}

AudioALSACodecDeviceOutEarphonePMIC::AudioALSACodecDeviceOutEarphonePMIC()
{
    ALOGD("%s()", __FUNCTION__);
    DeviceDoDcCalibrate();
}


AudioALSACodecDeviceOutEarphonePMIC::~AudioALSACodecDeviceOutEarphonePMIC()
{
    ALOGD("%s()", __FUNCTION__);
}


status_t AudioALSACodecDeviceOutEarphonePMIC::open()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    if (mClientCount == 0)
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Amp_R_Switch"), "On"))
        {
            ALOGE("Error: Audio_Amp_R_Switch invalid value");
        }

        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Amp_L_Switch"), "On"))
        {
            ALOGE("Error: Audio_Amp_L_Switch invalid value");
        }
    }

    mClientCount++;

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


status_t AudioALSACodecDeviceOutEarphonePMIC::close()
{
    ALOGD("+%s(), mClientCount = %d", __FUNCTION__, mClientCount);

    mClientCount--;

    if (mClientCount == 0)
    {
        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Amp_R_Switch"), "Off"))
        {
            ALOGE("Error: Audio_Amp_R_Switch invalid value");
        }

        if (mixer_ctl_set_enum_by_string(mixer_get_ctl_by_name(mMixer, "Audio_Amp_L_Switch"), "Off"))
        {
            ALOGE("Error: Audio_Amp_L_Switch invalid value");
        }
    }

    ALOGD("-%s(), mClientCount = %d", __FUNCTION__, mClientCount);
    return NO_ERROR;
}


} // end of namespace android
