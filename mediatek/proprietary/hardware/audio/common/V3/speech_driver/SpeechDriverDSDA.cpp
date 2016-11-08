#include "SpeechDriverDSDA.h"

#include "AudioLock.h"

#include "SpeechDriverLAD.h"


#include <utils/threads.h>
#include <cutils/properties.h>


#define LOG_TAG "SpeechDriverDSDA"

namespace android
{

dsda_proposal_t SpeechDriverDSDA::GetDSDAProposalType()
{
    char property_value[PROPERTY_VALUE_MAX];
    property_get("persist.af.dsda_proposal_type", property_value, "0");
    int dsda_proposal_type = atoi(property_value);
    ALOGD("%s(), persist.af.dsda_proposal_type = %d", __FUNCTION__, dsda_proposal_type);

    if (dsda_proposal_type == 1)
    {
        ALOGD("%s(), force set DSDA_PROPOSAL_1", __FUNCTION__);
        return DSDA_PROPOSAL_1;
    }
    else if (dsda_proposal_type == 2)
    {
        ALOGD("%s(), force set DSDA_PROPOSAL_2", __FUNCTION__);
        return DSDA_PROPOSAL_2;
    }
    else
    {
#ifdef MTK_INT_MD_SPE_FOR_EXT_MD
        ALOGD("%s(), DSDA_PROPOSAL_2", __FUNCTION__);
        return DSDA_PROPOSAL_2;
#else
        ALOGD("%s(), DSDA_PROPOSAL_1", __FUNCTION__);
        return DSDA_PROPOSAL_1;
#endif
    }
}


/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

SpeechDriverDSDA *SpeechDriverDSDA::mDSDA = NULL;

SpeechDriverInterface *SpeechDriverDSDA::GetInstance(modem_index_t modem_index)
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    SpeechDriverInterface *pSpeechDriver = NULL;

    const dsda_proposal_t dsda_proposal_type = GetDSDAProposalType();
    if (dsda_proposal_type == DSDA_PROPOSAL_1) // proposal I
    {
        pSpeechDriver = SpeechDriverLAD::GetInstance(modem_index);
    }
    else if (dsda_proposal_type == DSDA_PROPOSAL_2) // proposal II
    {
        if (mDSDA == NULL)
        {
            mDSDA = new SpeechDriverDSDA(modem_index);
        }

        pSpeechDriver = mDSDA;
    }

    ASSERT(pSpeechDriver != NULL);
    return pSpeechDriver;
}

/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

SpeechDriverDSDA::SpeechDriverDSDA(modem_index_t modem_index)
{
    ALOGD("%s(), modem_index = %d", __FUNCTION__, modem_index);

    pSpeechDriverInternal = SpeechDriverLAD::GetInstance(MODEM_1);
    pSpeechDriverExternal = SpeechDriverLAD::GetInstance(MODEM_EXTERNAL);

    // force disable external modem speech enhancement, which will be done in internal modem
    pSpeechDriverExternal->SetForceDisableSpeechEnhancement(true);
}

SpeechDriverDSDA::~SpeechDriverDSDA()
{
    ALOGD("%s()", __FUNCTION__);

    pSpeechDriverInternal = NULL;
    pSpeechDriverExternal = NULL;
}

/*==============================================================================
 *                     Speech Control
 *============================================================================*/

status_t SpeechDriverDSDA::SetSpeechMode(const audio_devices_t input_device, const audio_devices_t output_device)
{
    ALOGD("%s()", __FUNCTION__);

    pSpeechDriverExternal->SetSpeechEnhancement(false);

    pSpeechDriverInternal->SetSpeechMode(input_device, output_device);
    pSpeechDriverExternal->SetSpeechMode(input_device, output_device);

    return NO_ERROR;
}

status_t SpeechDriverDSDA::setMDVolumeIndex(int stream, int device, int index)
{
    ALOGD("+%s() stream, device, index", __FUNCTION__, stream, device, index);
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::SpeechOn()
{
    ALOGD("%s()", __FUNCTION__);

    CheckApSideModemStatusAllOffOrDie();
    SetApSideModemStatus(SPEECH_STATUS_MASK);

    pSpeechDriverInternal->SpeechRouterOn(); // TODO
    pSpeechDriverExternal->SpeechOn();

    return NO_ERROR;
}

status_t SpeechDriverDSDA::SpeechOff()
{
    ALOGD("%s()", __FUNCTION__);

    ResetApSideModemStatus(SPEECH_STATUS_MASK);
    CheckApSideModemStatusAllOffOrDie();


    pSpeechDriverInternal->SpeechRouterOff(); // TODO
    pSpeechDriverExternal->SpeechOff();

    return NO_ERROR;
}

status_t SpeechDriverDSDA::VideoTelephonyOn()
{
    ALOGE("%s()", __FUNCTION__);
    CheckApSideModemStatusAllOffOrDie();
    SetApSideModemStatus(VT_STATUS_MASK);
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::VideoTelephonyOff()
{
    ALOGE("%s()", __FUNCTION__);
    ResetApSideModemStatus(VT_STATUS_MASK);
    CheckApSideModemStatusAllOffOrDie();
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::SpeechRouterOn() // should not call this
{
    ALOGE("%s()", __FUNCTION__);
    CheckApSideModemStatusAllOffOrDie();
    SetApSideModemStatus(SPEECH_ROUTER_STATUS_MASK);
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::SpeechRouterOff() // should not call this
{
    ALOGE("%s()", __FUNCTION__);
    ResetApSideModemStatus(SPEECH_ROUTER_STATUS_MASK);
    CheckApSideModemStatusAllOffOrDie();
    return INVALID_OPERATION;
}

/*==============================================================================
 *                     Recording Control
 *============================================================================*/

status_t SpeechDriverDSDA::RecordOn()
{
    ALOGD("%s()", __FUNCTION__);
    SetApSideModemStatus(RECORD_STATUS_MASK);
    return pSpeechDriverInternal->RecordOn();
}

status_t SpeechDriverDSDA::RecordOn(record_type_t type_record)
{
    ALOGD("%s()", __FUNCTION__);
    SetApSideModemStatus(RECORD_STATUS_MASK);
    return pSpeechDriverInternal->RecordOn();
}

status_t SpeechDriverDSDA::RecordOff()
{
    ALOGD("%s()", __FUNCTION__);
    ResetApSideModemStatus(RECORD_STATUS_MASK);
    return pSpeechDriverInternal->RecordOff();
}

status_t SpeechDriverDSDA::RecordOff(record_type_t type_record)
{
    ALOGD("%s()", __FUNCTION__);
    ResetApSideModemStatus(RECORD_STATUS_MASK);
    return pSpeechDriverInternal->RecordOff();
}

status_t SpeechDriverDSDA::SetPcmRecordType(record_type_t type_record)
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetPcmRecordType(type_record);
}

status_t SpeechDriverDSDA::VoiceMemoRecordOn()
{
    ALOGD("%s()", __FUNCTION__);
    SetApSideModemStatus(VM_RECORD_STATUS_MASK);
    return pSpeechDriverInternal->VoiceMemoRecordOn(); // TODO
}

status_t SpeechDriverDSDA::VoiceMemoRecordOff()
{
    ALOGD("%s()", __FUNCTION__);
    ResetApSideModemStatus(VM_RECORD_STATUS_MASK);
    return pSpeechDriverInternal->VoiceMemoRecordOff(); // TODO
}

uint16_t SpeechDriverDSDA::GetRecordSampleRate() const
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->GetRecordSampleRate();
}

uint16_t SpeechDriverDSDA::GetRecordChannelNumber() const
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->GetRecordChannelNumber();
}


/*==============================================================================
 *                     Background Sound
 *============================================================================*/

status_t SpeechDriverDSDA::BGSoundOn()
{
    ALOGD("%s()", __FUNCTION__);
    SetApSideModemStatus(BGS_STATUS_MASK);
    return pSpeechDriverInternal->BGSoundOn();
}

status_t SpeechDriverDSDA::BGSoundConfig(uint8_t ul_gain, uint8_t dl_gain)
{
    ALOGD("%s(), ul_gain = 0x%x, dl_gain = 0x%x", __FUNCTION__, ul_gain, dl_gain);
    return pSpeechDriverInternal->BGSoundConfig(ul_gain, dl_gain);
}

status_t SpeechDriverDSDA::BGSoundOff()
{
    ALOGD("%s()", __FUNCTION__);
    ResetApSideModemStatus(BGS_STATUS_MASK);
    return pSpeechDriverInternal->BGSoundOff();
}

/*==============================================================================
*                     PCM 2 Way
*============================================================================*/
status_t SpeechDriverDSDA::PCM2WayPlayOn()
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}


status_t SpeechDriverDSDA::PCM2WayPlayOff()
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}


status_t SpeechDriverDSDA::PCM2WayRecordOn()
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}


status_t SpeechDriverDSDA::PCM2WayRecordOff()
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}


status_t SpeechDriverDSDA::PCM2WayOn(const bool wideband_on)
{
    ALOGE("%s()", __FUNCTION__);
    SetApSideModemStatus(P2W_STATUS_MASK);
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::PCM2WayOff()
{
    ALOGE("%s()", __FUNCTION__);
    ResetApSideModemStatus(P2W_STATUS_MASK);
    return INVALID_OPERATION;
}

#if defined(MTK_DUAL_MIC_SUPPORT) || defined(MTK_AUDIO_HD_REC_SUPPORT)
status_t SpeechDriverDSDA::DualMicPCM2WayOn(const bool wideband_on, const bool record_only)
{
    ALOGE("%s(), wideband_on = %d, record_only = %d", __FUNCTION__, wideband_on, record_only);
    SetApSideModemStatus(P2W_STATUS_MASK);
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::DualMicPCM2WayOff()
{
    ALOGE("%s()", __FUNCTION__);
    ResetApSideModemStatus(P2W_STATUS_MASK);
    return INVALID_OPERATION;
}
#else
status_t SpeechDriverDSDA::DualMicPCM2WayOn(const bool wideband_on, const bool record_only)
{
    ALOGE("%s() unsupport", __FUNCTION__);
    return INVALID_OPERATION;
}

status_t SpeechDriverDSDA::DualMicPCM2WayOff()
{
    ALOGE("%s() unsupport", __FUNCTION__);
    return INVALID_OPERATION;
}
#endif

/*==============================================================================
 *                     TTY-CTM Control
 *============================================================================*/
status_t SpeechDriverDSDA::TtyCtmOn(ctm_interface_t ctm_interface)
{
    ALOGE("%s(), ctm_interface = %d, force set to BAUDOT_MODE = %d", __FUNCTION__, ctm_interface, BAUDOT_MODE);
    SetApSideModemStatus(TTY_STATUS_MASK);
    return NO_ERROR;
}

status_t SpeechDriverDSDA::TtyCtmOff()
{
    ALOGE("%s()", __FUNCTION__);
    ResetApSideModemStatus(TTY_STATUS_MASK);
    return NO_ERROR;
}

status_t SpeechDriverDSDA::TtyCtmDebugOn(bool tty_debug_flag)
{
    ALOGE("%s(), tty_debug_flag = %d", __FUNCTION__, tty_debug_flag);
    return NO_ERROR;
}

/*==============================================================================
 *                     Acoustic Loopback
 *============================================================================*/

status_t SpeechDriverDSDA::SetAcousticLoopback(bool loopback_on)
{
    ALOGD("%s(), loopback_on = %d", __FUNCTION__, loopback_on);

    if (loopback_on == true)
    {
        CheckApSideModemStatusAllOffOrDie();
        SetApSideModemStatus(LOOPBACK_STATUS_MASK);

        pSpeechDriverInternal->SpeechRouterOn();
        pSpeechDriverExternal->SetAcousticLoopback(true);
    }
    else
    {
        ResetApSideModemStatus(LOOPBACK_STATUS_MASK);
        CheckApSideModemStatusAllOffOrDie();

        pSpeechDriverInternal->SpeechRouterOff();
        pSpeechDriverExternal->SetAcousticLoopback(false);
    }

    return NO_ERROR;
}

status_t SpeechDriverDSDA::SetAcousticLoopbackBtCodec(bool enable_codec)
{
    ALOGD("%s(), enable_codec = %d", __FUNCTION__, enable_codec);
    return pSpeechDriverExternal->SetAcousticLoopbackBtCodec(enable_codec);
}

status_t SpeechDriverDSDA::SetAcousticLoopbackDelayFrames(int32_t delay_frames)
{
    ALOGD("%s(), delay_frames = %d", __FUNCTION__, delay_frames);
    return pSpeechDriverExternal->SetAcousticLoopbackDelayFrames(delay_frames);
}

/*==============================================================================
 *                     Volume Control
 *============================================================================*/

status_t SpeechDriverDSDA::SetDownlinkGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old mDownlinkGain = 0x%x", __FUNCTION__, gain, mDownlinkGain);
    return pSpeechDriverInternal->SetDownlinkGain(gain);
}

status_t SpeechDriverDSDA::SetEnh1DownlinkGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old SetEnh1DownlinkGain = 0x%x", __FUNCTION__, gain, mDownlinkenh1Gain);
    return pSpeechDriverInternal->SetEnh1DownlinkGain(gain);
}

status_t SpeechDriverDSDA::SetUplinkGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old mUplinkGain = 0x%x", __FUNCTION__, gain, mUplinkGain);
    return pSpeechDriverInternal->SetUplinkGain(gain);
}

status_t SpeechDriverDSDA::SetDownlinkMute(bool mute_on)
{
    ALOGD("%s(), mute_on = %d, old mDownlinkMuteOn = %d", __FUNCTION__, mute_on, mDownlinkMuteOn);
    return pSpeechDriverInternal->SetDownlinkMute(mute_on);
}

status_t SpeechDriverDSDA::SetUplinkMute(bool mute_on)
{
    ALOGD("%s(), mute_on = %d, old mUplinkMuteOn = %d", __FUNCTION__, mute_on, mUplinkMuteOn);
    return pSpeechDriverInternal->SetUplinkMute(mute_on);
}

status_t SpeechDriverDSDA::SetUplinkSourceMute(bool mute_on)
{
    ALOGD("%s(), mute_on = %d", __FUNCTION__, mute_on);
    return pSpeechDriverInternal->SetUplinkSourceMute(mute_on);
}

status_t SpeechDriverDSDA::SetSidetoneGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old mSideToneGain = 0x%x", __FUNCTION__, gain, mSideToneGain);
    return pSpeechDriverInternal->SetSidetoneGain(gain);
}

/*==============================================================================
 *                     Device related Config
 *============================================================================*/

status_t SpeechDriverDSDA::SetModemSideSamplingRate(uint16_t sample_rate)
{
    ALOGD("%s(), sample_rate = %d", __FUNCTION__, sample_rate);

    pSpeechDriverInternal->SetModemSideSamplingRate(sample_rate);
    pSpeechDriverExternal->SetModemSideSamplingRate(sample_rate);

    return NO_ERROR;
}

/*==============================================================================
 *                     Speech Enhancement Control
 *============================================================================*/
status_t SpeechDriverDSDA::SetSpeechEnhancement(bool enhance_on)
{
    ALOGD("%s(), enhance_on = %d", __FUNCTION__, enhance_on);
    return pSpeechDriverInternal->SetSpeechEnhancement(enhance_on);
}

status_t SpeechDriverDSDA::SetSpeechEnhancementMask(const sph_enh_mask_struct_t &mask)
{
    ALOGD("%s(), main_func = 0x%x, dynamic_func = 0x%x", __FUNCTION__, mask.main_func, mask.dynamic_func);
    return pSpeechDriverInternal->SetSpeechEnhancementMask(mask);
}

status_t SpeechDriverDSDA::SetBtHeadsetNrecOn(const bool bt_headset_nrec_on)
{
    ALOGD("%s(), bt_headset_nrec_on = %d", __FUNCTION__, bt_headset_nrec_on);
    return pSpeechDriverInternal->SetBtHeadsetNrecOn(bt_headset_nrec_on);
}
/*==============================================================================
 *                     Speech Enhancement Parameters
 *============================================================================*/

status_t SpeechDriverDSDA::SetNBSpeechParameters(const AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB)
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetNBSpeechParameters(pSphParamNB);
}

#if defined(MTK_DUAL_MIC_SUPPORT)
status_t SpeechDriverDSDA::SetDualMicSpeechParameters(const AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic)
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetDualMicSpeechParameters(pSphParamDualMic);
}
#else
status_t SpeechDriverDSDA::SetDualMicSpeechParameters(const AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic)
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}
#endif

status_t SpeechDriverDSDA::SetMagiConSpeechParameters(const AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT *pSphParamMagiCon)
{
#if defined(MTK_MAGICONFERENCE_SUPPORT) && defined(MTK_DUAL_MIC_SUPPORT)
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetMagiConSpeechParameters(pSphParamMagiCon);
#else
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
#endif
}

status_t SpeechDriverDSDA::SetHACSpeechParameters(const AUDIO_CUSTOM_HAC_PARAM_STRUCT *pSphParamHAC)
{
#if defined(MTK_HAC_SUPPORT)
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetHACSpeechParameters(pSphParamHAC);
#else
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
#endif
}

#if defined(MTK_WB_SPEECH_SUPPORT)
status_t SpeechDriverDSDA::SetWBSpeechParameters(const AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB)
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetWBSpeechParameters(pSphParamWB);
}
#else
status_t SpeechDriverDSDA::SetWBSpeechParameters(const AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB)
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}

#endif

//#if defined(MTK_VIBSPK_SUPPORT)
status_t SpeechDriverDSDA::GetVibSpkParam(void *eVibSpkParam)
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->GetVibSpkParam(eVibSpkParam);
}

status_t SpeechDriverDSDA::SetVibSpkParam(void *eVibSpkParam)
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->SetVibSpkParam(eVibSpkParam);
}
//#endif //defined(MTK_VIBSPK_SUPPORT)


/*==============================================================================
 *                     Recover State
 *============================================================================*/

void SpeechDriverDSDA::RecoverModemSideStatusToInitState()
{
    ALOGW("%s()", __FUNCTION__);
}

/*==============================================================================
 *                     Check Modem Status
 *============================================================================*/
bool SpeechDriverDSDA::CheckModemIsReady()
{
    ALOGD("%s()", __FUNCTION__);
    return pSpeechDriverInternal->CheckModemIsReady() & pSpeechDriverExternal->CheckModemIsReady();
};

/*==============================================================================
 *                     Debug Info
 *============================================================================*/
status_t SpeechDriverDSDA::ModemDumpSpeechParam()
{
    ALOGD("%s()", __FUNCTION__);

    pSpeechDriverInternal->ModemDumpSpeechParam();
    pSpeechDriverExternal->ModemDumpSpeechParam();

    return NO_ERROR;
}
/*==============================================================================
 *                     Speech Enhancement Parameters
 *============================================================================*/
status_t SpeechDriverDSDA::SetDynamicSpeechParameters(const int type, const void* param_arg)
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}

} // end of namespace android

