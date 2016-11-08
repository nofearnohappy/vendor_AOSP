#include "SpeechDriverLAD.h"
#include "SpeechMessengerECCCI.h"
#include "SpeechMessengerCCCI.h"

#if defined (MTK_C2K_SUPPORT)
#include "SpeechMessengerEVDO.h"
#elif defined (DSDA_SUPPORT)
#include "SpeechDriverDSDA.h"
#include "SpeechMessengerDSDA.h"
#endif

#include "SpeechEnhancementController.h"
#include "SpeechVMRecorder.h"

#include "AudioUtility.h"

#include "CFG_AUDIO_File.h"
#include "AudioCustParam.h"
#include "SpeechANCController.h"
#if defined(MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT)
#include "SpeechParamParser.h"
#endif

//#if defined(MTK_VIBSPK_SUPPORT)
#include "AudioCompFltCustParam.h"
#include "AudioVIBSPKControl.h"
//#endif

#include "AudioLock.h"

#define LOG_TAG "SpeechDriverLAD"
//#define SPH_SKIP_A2M_BUFF_MSG
//Phone Call Test without buffer message transfer

#if defined(MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT)
#define MODEM_DYNAMIC_PARAM
#endif

namespace android
{

/*==============================================================================
 *                     Const Value
 *============================================================================*/
static const int16_t kUnreasonableGainValue = 0x8000;

static const uint16_t kSpeechOnWaitModemAckMaxTimeMs = 0; // 0: dont't wait
static const uint16_t kSpeechOffWaitModemAckMaxTimeMs = 500;

static const uint32_t kDefaultAcousticLoopbackDelayFrames = 12;
static const uint32_t kMaxAcousticLoopbackDelayFrames     = 64;

/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

SpeechDriverLAD *SpeechDriverLAD::mLad1 = NULL;
SpeechDriverLAD *SpeechDriverLAD::mLad2 = NULL;
SpeechDriverLAD *SpeechDriverLAD::mLad3 = NULL;

SpeechDriverLAD *SpeechDriverLAD::GetInstance(modem_index_t modem_index)
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    SpeechDriverLAD *pLad = NULL;
    ALOGD("%s(), modem_index=%d", __FUNCTION__, modem_index);

    switch (modem_index)
    {
        case MODEM_1:
            if (mLad1 == NULL)
            {
                mLad1 = new SpeechDriverLAD(modem_index);
            }
            pLad = mLad1;
            break;
        case MODEM_2:
            if (mLad2 == NULL)
            {
                mLad2 = new SpeechDriverLAD(modem_index);
            }
            pLad = mLad2;
            break;
        case MODEM_EXTERNAL:
            if (mLad3 == NULL)
            {
                mLad3 = new SpeechDriverLAD(modem_index);
            }
            pLad = mLad3;
            break;
        default:
            ALOGE("%s: no such modem_index %d", __FUNCTION__, modem_index);
            break;
    }

    ASSERT(pLad != NULL);
    return pLad;
}

/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

SpeechDriverLAD::SpeechDriverLAD(modem_index_t modem_index)
{
    ALOGD("%s(), modem_index = %d", __FUNCTION__, modem_index);
    mModemIndex = modem_index;

    if (mModemIndex == MODEM_1 || mModemIndex == MODEM_2)
    {
        ALOGD("%s(), SpeechMessengerCCCI modem_index = %d", __FUNCTION__, modem_index);
#ifdef CCCI_FORCE_USE
        pCCCI = new SpeechMessengerCCCI(mModemIndex, this);
#else
        pCCCI = new SpeechMessengerECCCI(mModemIndex, this);
#endif
    }
    else if (mModemIndex == MODEM_EXTERNAL)
    {
#if defined (MTK_C2K_SUPPORT)
        pCCCI = new SpeechMessengerEVDO(mModemIndex, this);
#elif defined (DSDA_SUPPORT)
        pCCCI = new SpeechMessengerDSDA(mModemIndex, this);
#endif
    }

    ASSERT(pCCCI != NULL);

    status_t ret = pCCCI->Initial();

    if (ret == NO_ERROR)
    {
        RecoverModemSideStatusToInitState();
        //SetAllSpeechEnhancementInfoToModem(); // only for debug
#ifndef CCCI_FORCE_USE
        QueryModemRFInfo();
#endif
    }

    // Speech mode
    mSpeechMode = SPEECH_MODE_NORMAL;

    // Record capability
    mRecordSampleRateType = RECORD_SAMPLE_RATE_08K;
    mRecordChannelType    = RECORD_CHANNEL_MONO;
    mRecordType = RECORD_TYPE_UL;

    // Clean gain value and mute status
    CleanGainValueAndMuteStatus();

    mUseBtCodec = 1;

    // BT Headset NREC
    mBtHeadsetNrecOn = SpeechEnhancementController::GetInstance()->GetBtHeadsetNrecOn();

    // loopback delay frames (1 frame = 20 ms)
    mAcousticLoopbackDelayFrames = kDefaultAcousticLoopbackDelayFrames;

#if defined(MODEM_DYNAMIC_PARAM)
    SpeechParamParser::getInstance();
#endif
    mVolumeIndex = 0x3;

}


SpeechDriverLAD::~SpeechDriverLAD()
{
    pCCCI->Deinitial();
    delete pCCCI;
}

/*==============================================================================
 *                     Speech Control
 *============================================================================*/
speech_mode_t SpeechDriverLAD::GetSpeechModeByOutputDevice(const audio_devices_t output_device)
{
    speech_mode_t speech_mode = SPEECH_MODE_NORMAL;
    if (output_device == AUDIO_DEVICE_OUT_BLUETOOTH_SCO ||
        output_device == AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET ||
        output_device == AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT)
    {
#if defined(MTK_AUDIO_BT_NREC_WO_ENH_MODE) && !defined(MODEM_DYNAMIC_PARAM)
        if (mBtHeadsetNrecOn == true)
        {
            speech_mode = SPEECH_MODE_BT_EARPHONE;
        }
        else
        {
            //use SPEECH_MODE 4 speech parameters
            speech_mode = SPEECH_MODE_BT_CORDLESS;
        }
#else
        speech_mode = SPEECH_MODE_BT_EARPHONE;
#endif
    }
    else if (output_device == AUDIO_DEVICE_OUT_SPEAKER)
    {
#if defined(MODEM_DYNAMIC_PARAM)
#if defined(MTK_MAGICONFERENCE_SUPPORT) && defined(MTK_DUAL_MIC_SUPPORT)
        if (SpeechEnhancementController::GetInstance()->GetMagicConferenceCallOn() == true)
        {
            speech_mode = SPEECH_MODE_MAGIC_CON_CALL;
        }
        else
#endif
#endif
        {
            speech_mode = SPEECH_MODE_LOUD_SPEAKER;
        }
    }
    else if (output_device == AUDIO_DEVICE_OUT_WIRED_HEADSET ||
             output_device == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
    {
        speech_mode = SPEECH_MODE_EARPHONE;
    }
    else if (output_device == AUDIO_DEVICE_OUT_EARPIECE)
    {
#if defined(MTK_HAC_SUPPORT)
        if (SpeechEnhancementController::GetInstance()->GetHACOn() == true)
        {
            speech_mode = SPEECH_MODE_HAC;
        }
        else
#endif
        {
            speech_mode = SPEECH_MODE_NORMAL;
        }
    }

    return speech_mode;
}

status_t SpeechDriverLAD::SetSpeechMode(const audio_devices_t input_device, const audio_devices_t output_device)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    speech_mode_t speech_mode = GetSpeechModeByOutputDevice(output_device);
    ALOGD("%s(), input_device = 0x%x, output_device = 0x%x, speech_mode = %d", __FUNCTION__, input_device, output_device, speech_mode);

#ifdef MTK_ACTIVE_NOISE_CANCELLATION_SUPPORT
    if (speech_mode == SPEECH_MODE_NORMAL)
    {
        SpeechANCController::getInstance()->SwapANC(true);
        if (SpeechANCController::getInstance()->GetApplyANC() == true)
        {
            SpeechANCController::getInstance()->EanbleANC(true);
            if (SpeechANCController::getInstance()->GetEanbleANCLog() == true)
            {
                SpeechANCController::getInstance()->StartANCLog();
            }
        }
    }
    else
    {
        if (SpeechANCController::getInstance()->GetApplyANC() == true)
        {
            SpeechANCController::getInstance()->EanbleANC(false);
            if (SpeechANCController::getInstance()->GetEanbleANCLog() == true)
            {
                SpeechANCController::getInstance()->StopANCLog();
            }
        }
        SpeechANCController::getInstance()->SwapANC(false);
    }
#endif

    // AP side have to set speech mode before speech/record/loopback on,
    // hence we check whether modem side get all necessary speech enhancement parameters here
    // if not, re-send it !!
    if (pCCCI->CheckSpeechParamAckAllArrival() == false)
    {
        ALOGW("%s(), Do SetAllSpeechEnhancementInfoToModem() done. Start set speech_mode = %d", __FUNCTION__, speech_mode);
    }

#if defined (DSDA_SUPPORT)
    // using DSP STF for DSDA proposal 1
    if (mModemIndex == MODEM_EXTERNAL)
    {
        if (SpeechDriverDSDA::GetDSDAProposalType() == DSDA_PROPOSAL_1)
        {
            SetDSPSidetoneFilter(true);
        }
    }
#endif

    mSpeechMode = speech_mode;
#if !defined(MODEM_DYNAMIC_PARAM)//no need to set BtNrecSwitch, use mode param
#if !defined(MTK_AUDIO_BT_NREC_WO_ENH_MODE)
    // enable/disable enhancement when before speech mode
    if (mSpeechMode == SPEECH_MODE_BT_EARPHONE)
    {
        SetBtHeadsetNrecOn(SpeechEnhancementController::GetInstance()->GetBtHeadsetNrecStatus());
    }
    else
    {
        SetBtHeadsetNrecOn(true);
    }
#endif
#endif

    // set a unreasonable gain value s.t. the reasonable gain can be set to modem next time
    mDownlinkGain   = kUnreasonableGainValue;
    mUplinkGain     = kUnreasonableGainValue;
    mSideToneGain   = kUnreasonableGainValue;
#if defined(MODEM_DYNAMIC_PARAM)
    int param_arg[3];
    param_arg[0] = (int) mSpeechMode;
    param_arg[1] = mVolumeIndex;
    param_arg[2] = mBtHeadsetNrecOn;
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH, param_arg);

    return NO_ERROR;
#else

    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SET_SPH_MODE, speech_mode, 0));
#endif
}

status_t SpeechDriverLAD::setMDVolumeIndex(int stream, int device, int index)
{
    bool bSpeechStatus = GetApSideModemStatus(SPEECH_STATUS_MASK);
    ALOGD("+%s() stream= %x, device = %x, index =%x, bSpeechStatus=%d", __FUNCTION__, stream, device, index, bSpeechStatus);
    //Android M Voice volume index: available index 1~7, 0 for mute
    //Android L Voice volume index: available index 0~6
    if (index <= 0)
    {
        return NO_ERROR;
    }
    else
    {
        mVolumeIndex = index - 1;
    }
    // set a unreasonable gain value s.t. the reasonable gain can be set to modem next time
#if defined(MODEM_DYNAMIC_PARAM)
    int size_byte;
    int param_arg[3];

    if (bSpeechStatus)
    {
        param_arg[0] = (int) mSpeechMode;
        param_arg[1] = mVolumeIndex;
        param_arg[2] = mBtHeadsetNrecOn;
        SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH, param_arg);
    }
#endif
    return NO_ERROR;

}

status_t SpeechDriverLAD::SpeechOn()
{
    ALOGD("%s()", __FUNCTION__);
#if defined(MODEM_DYNAMIC_PARAM)
    int size_byte;
    int param_arg[2];
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_GENERAL, param_arg);
#ifdef MTK_SPH_MAGICLARITY_SHAPEFIR_SUPPORT
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_MAGICLARITY, param_arg);
#endif
#ifdef MTK_DUAL_MIC_SUPPORT
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_DMNR, param_arg);
#endif
#endif

    CheckApSideModemStatusAllOffOrDie();
    SetApSideModemStatus(SPEECH_STATUS_MASK);

    status_t retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_ON, RAT_2G_MODE, 0));

    if (retval == NO_ERROR) // In queue or had sent to modem side => wait ack
    {
        WaitUntilSignaledOrTimeout(kSpeechOnWaitModemAckMaxTimeMs);
    }

    return retval;
}

status_t SpeechDriverLAD::SpeechOff()
{
    ALOGD("%s()", __FUNCTION__);

    ResetApSideModemStatus(SPEECH_STATUS_MASK);
    CheckApSideModemStatusAllOffOrDie();

    // Clean gain value and mute status
    CleanGainValueAndMuteStatus();

    status_t retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_OFF, 0, 0));

    if (retval == NO_ERROR) // In queue or had sent to modem side => wait ack
    {
        WaitUntilSignaledOrTimeout(kSpeechOffWaitModemAckMaxTimeMs);
    }
    mVolumeIndex = 0x3;

    return retval;
}

status_t SpeechDriverLAD::VideoTelephonyOn()
{
    ALOGD("%s()", __FUNCTION__);

    CheckApSideModemStatusAllOffOrDie();
    SetApSideModemStatus(VT_STATUS_MASK);

    status_t retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_ON, RAT_3G324M_MODE, 0));

    if (retval == NO_ERROR) // In queue or had sent to modem side => wait ack
    {
        WaitUntilSignaledOrTimeout(kSpeechOnWaitModemAckMaxTimeMs);
    }

    return retval;
}

status_t SpeechDriverLAD::VideoTelephonyOff()
{
    ALOGD("%s()", __FUNCTION__);

    ResetApSideModemStatus(VT_STATUS_MASK);
    CheckApSideModemStatusAllOffOrDie();

    // Clean gain value and mute status
    CleanGainValueAndMuteStatus();

    status_t retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_OFF, 0, 0));

    if (retval == NO_ERROR) // In queue or had sent to modem side => wait ack
    {
        WaitUntilSignaledOrTimeout(kSpeechOffWaitModemAckMaxTimeMs);
    }

    return retval;
}

status_t SpeechDriverLAD::SpeechRouterOn()
{
    ALOGD("%s()", __FUNCTION__);

    CheckApSideModemStatusAllOffOrDie();
    SetApSideModemStatus(SPEECH_ROUTER_STATUS_MASK);

    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_ROUTER_ON, true, 0));
}

status_t SpeechDriverLAD::SpeechRouterOff()
{
    ALOGD("%s()", __FUNCTION__);

    ResetApSideModemStatus(SPEECH_ROUTER_STATUS_MASK);
    CheckApSideModemStatusAllOffOrDie();

    // Clean gain value and mute status
    CleanGainValueAndMuteStatus();

    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_ROUTER_ON, false, 0));
}


/*==============================================================================
 *                     Recording Control
 *============================================================================*/

status_t SpeechDriverLAD::RecordOn()
{
    ALOGD("%s(), sample_rate = %d, channel = %d, MSG_A2M_PCM_REC_ON", __FUNCTION__, mRecordSampleRateType, mRecordChannelType);

    SetApSideModemStatus(RECORD_STATUS_MASK);

    // Note: the record capability is fixed in constructor
    uint16_t param_16bit = mRecordSampleRateType  | (mRecordChannelType << 4);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PCM_REC_ON, param_16bit, 0));
}

status_t SpeechDriverLAD::RecordOn(record_type_t type_record)
{
    ALOGD("%s(), sample_rate = %d, channel = %d, type_record = %d, MSG_A2M_RECORD_RAW_PCM_ON", __FUNCTION__, mRecordSampleRateType, mRecordChannelType, type_record);
    uint16_t param_16bit;

    SetApSideModemStatus(RAW_RECORD_STATUS_MASK);
    mRecordType = type_record;
    pCCCI->SetPcmRecordType(type_record);
    param_16bit = mRecordSampleRateType  | (mRecordChannelType << 4);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_RECORD_RAW_PCM_ON, param_16bit, 0));
}

status_t SpeechDriverLAD::RecordOff()
{
    ALOGD("%s()", __FUNCTION__);

    ResetApSideModemStatus(RECORD_STATUS_MASK);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PCM_REC_OFF, 0, 0));
}

status_t SpeechDriverLAD::RecordOff(record_type_t type_record)
{
    ALOGD("%s(), type_record = %d, MSG_A2M_RECORD_RAW_PCM_OFF", __FUNCTION__, type_record);

    ResetApSideModemStatus(RAW_RECORD_STATUS_MASK);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_RECORD_RAW_PCM_OFF, 0, 0));
}

status_t SpeechDriverLAD::SetPcmRecordType(record_type_t type_record)
{
    ALOGD("%s(), type_record = %d", __FUNCTION__, type_record);
    pCCCI->SetPcmRecordType(type_record);
    return NO_ERROR;
}

status_t SpeechDriverLAD::VoiceMemoRecordOn()
{
    ALOGD("%s(), MSG_A2M_VM_REC_ON", __FUNCTION__);

    SetApSideModemStatus(VM_RECORD_STATUS_MASK);

#if defined (DSDA_SUPPORT)
    // bypass VM log for DSDA proposal 1
    if (mModemIndex == MODEM_EXTERNAL)
    {
        if (SpeechDriverDSDA::GetDSDAProposalType() == DSDA_PROPOSAL_1)
        {
            return INVALID_OPERATION;
        }
    }
#endif

    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_VM_REC_ON, RECORD_FORMAT_VM, 0));
}

status_t SpeechDriverLAD::VoiceMemoRecordOff()
{
    ALOGD("%s()", __FUNCTION__);

    ResetApSideModemStatus(VM_RECORD_STATUS_MASK);

#if defined (DSDA_SUPPORT)
    // bypass VM log for DSDA proposal 1
    if (mModemIndex == MODEM_EXTERNAL)
    {
        if (SpeechDriverDSDA::GetDSDAProposalType() == DSDA_PROPOSAL_1)
        {
            return INVALID_OPERATION;
        }
    }
#endif

    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_VM_REC_OFF, 0, 0));
}

uint16_t SpeechDriverLAD::GetRecordSampleRate() const
{
    // Note: the record capability is fixed in constructor

    uint16_t num_sample_rate;

    switch (mRecordSampleRateType)
    {
        case RECORD_SAMPLE_RATE_08K:
            num_sample_rate = 8000;
            break;
        case RECORD_SAMPLE_RATE_16K:
            num_sample_rate = 16000;
            break;
        case RECORD_SAMPLE_RATE_32K:
            num_sample_rate = 32000;
            break;
        case RECORD_SAMPLE_RATE_48K:
            num_sample_rate = 48000;
            break;
        default:
            num_sample_rate = 8000;
            break;
    }

    ALOGD("%s(), num_sample_rate = %u", __FUNCTION__, num_sample_rate);
    return num_sample_rate;
}

uint16_t SpeechDriverLAD::GetRecordChannelNumber() const
{
    // Note: the record capability is fixed in constructor

    uint16_t num_channel;

    switch (mRecordChannelType)
    {
        case RECORD_CHANNEL_MONO:
            num_channel = 1;
            break;
        case RECORD_CHANNEL_STEREO:
            num_channel = 2;
            break;
        default:
            num_channel = 1;
            break;
    }

    ALOGD("%s(), num_channel = %u", __FUNCTION__, num_channel);
    return num_channel;
}


/*==============================================================================
 *                     Background Sound
 *============================================================================*/

status_t SpeechDriverLAD::BGSoundOn()
{
    ALOGD("%s()", __FUNCTION__);
    SetApSideModemStatus(BGS_STATUS_MASK);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_BGSND_ON, 0, 0));
}

status_t SpeechDriverLAD::BGSoundConfig(uint8_t ul_gain, uint8_t dl_gain)
{
    ALOGD("%s(), ul_gain = 0x%x, dl_gain = 0x%x", __FUNCTION__, ul_gain, dl_gain);
    uint16_t param_16bit = (ul_gain << 8) | dl_gain;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_BGSND_CONFIG, param_16bit, 0));
}

status_t SpeechDriverLAD::BGSoundOff()
{
    ALOGD("%s()", __FUNCTION__);
    ResetApSideModemStatus(BGS_STATUS_MASK);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_BGSND_OFF, 0, 0));
}

/*==============================================================================
 *                     PCM 2 Way
 *============================================================================*/
status_t SpeechDriverLAD::PCM2WayPlayOn()
{
    ALOGD("%s(), old mPCM2WayState = 0x%x", __FUNCTION__, mPCM2WayState);

    status_t retval;
    if (mPCM2WayState == 0)
    {
        // nothing is on, just turn it on
        SetApSideModemStatus(P2W_STATUS_MASK);
        mPCM2WayState |= SPC_PNW_MSG_BUFFER_SPK;
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_SPK)
    {
        // only play on, return
        retval = INVALID_OPERATION;
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_MIC)
    {
        // only rec is on, turn off, modify state and turn on again
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
        mPCM2WayState |= SPC_PNW_MSG_BUFFER_SPK;
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
    }
    else if (mPCM2WayState == (SPC_PNW_MSG_BUFFER_MIC | SPC_PNW_MSG_BUFFER_SPK))
    {
        // both on, return
        retval = INVALID_OPERATION;
    }
    else
    {
        retval = INVALID_OPERATION;
    }

    return retval;
}


status_t SpeechDriverLAD::PCM2WayPlayOff()
{
    ALOGD("%s(), current mPCM2WayState = 0x%x", __FUNCTION__, mPCM2WayState);

    status_t retval;
    if (mPCM2WayState == 0)
    {
        // nothing is on, return
        retval = INVALID_OPERATION;
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_SPK)
    {
        // only play on, just turn it off
        ResetApSideModemStatus(P2W_STATUS_MASK);
        mPCM2WayState &= (~SPC_PNW_MSG_BUFFER_SPK);
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_MIC)
    {
        // only rec on, return
        retval = INVALID_OPERATION;
    }
    else if (mPCM2WayState == (SPC_PNW_MSG_BUFFER_MIC | SPC_PNW_MSG_BUFFER_SPK))
    {
        // both rec and play on, turn off, modify state and turn on again
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
        mPCM2WayState &= (~SPC_PNW_MSG_BUFFER_SPK);
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
    }
    else
    {
        retval = INVALID_OPERATION;
    }

    return retval;
}


status_t SpeechDriverLAD::PCM2WayRecordOn()
{
    ALOGD("%s(), old mPCM2WayState = 0x%x", __FUNCTION__, mPCM2WayState);

    status_t retval;
    if (mPCM2WayState == 0)
    {
        //nothing is on, just turn it on
        SetApSideModemStatus(P2W_STATUS_MASK);
        mPCM2WayState |= SPC_PNW_MSG_BUFFER_MIC;
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_SPK)
    {
        // only play is on, turn off, modify state and turn on again
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
        mPCM2WayState |= SPC_PNW_MSG_BUFFER_MIC;
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_MIC)
    {
        // only rec on, return
        retval = INVALID_OPERATION;
    }
    else if (mPCM2WayState == (SPC_PNW_MSG_BUFFER_MIC | SPC_PNW_MSG_BUFFER_SPK))
    {
        retval = INVALID_OPERATION;
    }
    else
    {
        retval = INVALID_OPERATION;
    }

    return retval;
}


status_t SpeechDriverLAD::PCM2WayRecordOff()
{
    ALOGD("%s(), current mPCM2WayState = 0x%x", __FUNCTION__, mPCM2WayState);

    status_t retval;
    if (mPCM2WayState == 0)
    {
        //nothing is on, return
        retval = INVALID_OPERATION;
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_SPK)
    {
        // only play on, return
        retval = INVALID_OPERATION;
    }
    else if (mPCM2WayState == SPC_PNW_MSG_BUFFER_MIC)
    {
        // only rec on, just turn it off
        ResetApSideModemStatus(P2W_STATUS_MASK);
        mPCM2WayState &= (~SPC_PNW_MSG_BUFFER_MIC);
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
    }
    else if (mPCM2WayState == (SPC_PNW_MSG_BUFFER_MIC | SPC_PNW_MSG_BUFFER_SPK))
    {
        // both rec and play on, turn off, modify state and turn on again
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
        mPCM2WayState &= (~SPC_PNW_MSG_BUFFER_MIC);
        retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
    }
    else
    {
        retval = INVALID_OPERATION;
    }

    return retval;
}


status_t SpeechDriverLAD::PCM2WayOn(const bool wideband_on)
{
    mPCM2WayState = (SPC_PNW_MSG_BUFFER_SPK | SPC_PNW_MSG_BUFFER_MIC | (wideband_on << 4));
    ALOGD("%s(), mPCM2WayState = 0x%x", __FUNCTION__, mPCM2WayState);
    SetApSideModemStatus(P2W_STATUS_MASK);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_ON, mPCM2WayState, 0));
}

status_t SpeechDriverLAD::PCM2WayOff()
{
    mPCM2WayState = 0;
    ALOGD("%s(), mPCM2WayState = 0x%x", __FUNCTION__, mPCM2WayState);
    ResetApSideModemStatus(P2W_STATUS_MASK);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PNW_OFF, 0, 0));
}

status_t SpeechDriverLAD::DualMicPCM2WayOn(const bool wideband_on, const bool record_only)
{
#if defined(MTK_DUAL_MIC_SUPPORT) || defined(MTK_AUDIO_HD_REC_SUPPORT)
    ALOGD("%s(), wideband_on = %d, record_only = %d", __FUNCTION__, wideband_on, record_only);

    if (mPCM2WayState) // prevent 'on' for second time cause problem
    {
        ALOGW("%s(), mPCM2WayState(%d) > 0, return.", __FUNCTION__, mPCM2WayState);
        return INVALID_OPERATION;
    }

    SetApSideModemStatus(P2W_STATUS_MASK);

    dualmic_pcm2way_format_t dualmic_calibration_format =
        (wideband_on == false) ? P2W_FORMAT_NB_CAL : P2W_FORMAT_WB_CAL;

    if (record_only == true) // uplink only
    {
        mPCM2WayState = SPC_PNW_MSG_BUFFER_MIC;
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_DMNR_REC_ONLY_ON, dualmic_calibration_format, 0));
    }
    else // downlink + uplink
    {
        mPCM2WayState = SPC_PNW_MSG_BUFFER_SPK | SPC_PNW_MSG_BUFFER_MIC;
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_DMNR_RECPLAY_ON, dualmic_calibration_format, 0));
    }

#else
    ALOGE("%s() unsupport", __FUNCTION__);
    return INVALID_OPERATION;
#endif

}

status_t SpeechDriverLAD::DualMicPCM2WayOff()
{
#if defined(MTK_DUAL_MIC_SUPPORT) || defined(MTK_AUDIO_HD_REC_SUPPORT)
    ALOGD("%s(), mPCM2WayState = %d", __FUNCTION__, mPCM2WayState);

    if (mPCM2WayState == 0) // already turn off
    {
        ALOGW("%s(), mPCM2WayState(%d) == 0, return.", __FUNCTION__, mPCM2WayState);
        return INVALID_OPERATION;
    }

    ResetApSideModemStatus(P2W_STATUS_MASK);

    if (mPCM2WayState == SPC_PNW_MSG_BUFFER_MIC)
    {
        mPCM2WayState &= ~(SPC_PNW_MSG_BUFFER_MIC);
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_DMNR_REC_ONLY_OFF, 0, 0));
    }
    else
    {
        mPCM2WayState &= ~(SPC_PNW_MSG_BUFFER_SPK | SPC_PNW_MSG_BUFFER_MIC);
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_DMNR_RECPLAY_OFF, 0, 0));
    }
#else
    ALOGE("%s() unsupport", __FUNCTION__);
    return INVALID_OPERATION;
#endif
}

/*==============================================================================
 *                     TTY-CTM Control
 *============================================================================*/
status_t SpeechDriverLAD::TtyCtmOn(ctm_interface_t ctm_interface)
{
    ALOGD("%s(), ctm_interface = %d, force set to BAUDOT_MODE = %d", __FUNCTION__, ctm_interface, BAUDOT_MODE);
    status_t retval;
    const bool uplink_mute_on_copy = mUplinkMuteOn;
    SetUplinkMute(true);
    SetApSideModemStatus(TTY_STATUS_MASK);
    SpeechVMRecorder *pSpeechVMRecorder = SpeechVMRecorder::GetInstance();
    TtyCtmDebugOn(pSpeechVMRecorder->GetVMRecordCapabilityForCTM4Way());
    retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_CTM_ON, BAUDOT_MODE, 0));
    SetUplinkMute(uplink_mute_on_copy);
    return retval;
}

status_t SpeechDriverLAD::TtyCtmOff()
{
    ALOGD("%s()", __FUNCTION__);
    ResetApSideModemStatus(TTY_STATUS_MASK);
    TtyCtmDebugOn(false);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_CTM_OFF, 0, 0));
}

status_t SpeechDriverLAD::TtyCtmDebugOn(bool tty_debug_flag)
{
    ALOGD("%s(), tty_debug_flag = %d", __FUNCTION__, tty_debug_flag);
    SpeechVMRecorder *pSpeechVMRecorder = SpeechVMRecorder::GetInstance();
    if (tty_debug_flag)
    {
        pSpeechVMRecorder->StartCtmDebug();
    }
    else
    {
        pSpeechVMRecorder->StopCtmDebug();
    }
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_CTM_DUMP_DEBUG_FILE, tty_debug_flag, 0));
}

/*==============================================================================
 *                     Acoustic Loopback
 *============================================================================*/

status_t SpeechDriverLAD::SetAcousticLoopback(bool loopback_on)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s(), loopback_on = %d", __FUNCTION__, loopback_on);

    if (loopback_on == true)
    {
        CheckApSideModemStatusAllOffOrDie();
        SetApSideModemStatus(LOOPBACK_STATUS_MASK);

#if defined(MODEM_DYNAMIC_PARAM)
        int size_byte;
        int param_arg[2];
        SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_GENERAL, param_arg);
#ifdef MTK_SPH_MAGICLARITY_SHAPEFIR_SUPPORT
        SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_MAGICLARITY, param_arg);
#endif
#ifdef MTK_DUAL_MIC_SUPPORT
        SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_DMNR, param_arg);
#endif
#endif

    }
    else
    {
        ResetApSideModemStatus(LOOPBACK_STATUS_MASK);
        CheckApSideModemStatusAllOffOrDie();

        // Clean gain value and mute status
        CleanGainValueAndMuteStatus();
        mUseBtCodec = 1;
#if defined(MODEM_DYNAMIC_PARAM)&&defined(MTK_AUDIO_SPH_LPBK_PARAM)
        SpeechParamParser::getInstance()->SetParamInfo(String8("ParamSphLpbk=0;"));
#endif
    }

    const bool use_loopback_delay_control = true;
    bool disable_btcodec = !mUseBtCodec;
    int16_t param16 = (use_loopback_delay_control << 2) | (disable_btcodec << 1) | loopback_on;
    status_t retval = pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SET_ACOUSTIC_LOOPBACK, param16, mAcousticLoopbackDelayFrames));

    if (retval == NO_ERROR) // In queue or had sent to modem side => wait ack
    {
        WaitUntilSignaledOrTimeout(loopback_on == true ?
                                   kSpeechOnWaitModemAckMaxTimeMs :
                                   kSpeechOffWaitModemAckMaxTimeMs);
    }

    return retval;
}

status_t SpeechDriverLAD::SetAcousticLoopbackBtCodec(bool enable_codec)
{
    mUseBtCodec = enable_codec;
    return NO_ERROR;
}

status_t SpeechDriverLAD::SetAcousticLoopbackDelayFrames(int32_t delay_frames)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s(), delay_frames = %d", __FUNCTION__, delay_frames);

    if (delay_frames < 0)
    {
        ALOGE("%s(), delay_frames(%d) must >= 0 !! Set 0 instead.", __FUNCTION__, delay_frames);
        delay_frames = 0;
    }
    else if (delay_frames > kMaxAcousticLoopbackDelayFrames)
    {
        ALOGE("%s(), delay_frames(%d) must <= %d !! Set %d instead.", __FUNCTION__, delay_frames, kMaxAcousticLoopbackDelayFrames, kMaxAcousticLoopbackDelayFrames);
        delay_frames = kMaxAcousticLoopbackDelayFrames;
    }

    mAcousticLoopbackDelayFrames = delay_frames;

    if (GetApSideModemStatus(LOOPBACK_STATUS_MASK) == true)
    {
        ALOGW("Loopback is enabled now! The new delay_frames will be applied next time.");
    }

    return NO_ERROR;
}

/*==============================================================================
 *                     Volume Control
 *============================================================================*/

//param gain: data range is 0~0xFF00, which is mapping to 0dB to -64dB. The effective interval is 0.25dB by data increasing/decreasing 1.
status_t SpeechDriverLAD::SetDownlinkGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old mDownlinkGain = 0x%x", __FUNCTION__, gain, mDownlinkGain);
    if (gain == mDownlinkGain) { return NO_ERROR; }

    mDownlinkGain = gain;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_DL_DIGIT_VOLUME, gain, 0));
}

status_t SpeechDriverLAD::SetEnh1DownlinkGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old SetEnh1DownlinkGain = 0x%x", __FUNCTION__, gain, mDownlinkenh1Gain);
    if (gain == mDownlinkenh1Gain) { return NO_ERROR; }

    mDownlinkenh1Gain = gain;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_DL_ENH_REF_DIGIT_VOLUME, gain, 0));
}

//param gain: data range is 0~120, which is mapping to 0dB to 30dB. The effective interval is 1dB by data increasing/decreasing 4.
status_t SpeechDriverLAD::SetUplinkGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old mUplinkGain = 0x%x", __FUNCTION__, gain, mUplinkGain);
    if (gain == mUplinkGain) { return NO_ERROR; }

    mUplinkGain = gain;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SPH_UL_DIGIT_VOLUME, gain, 0));
}

status_t SpeechDriverLAD::SetDownlinkMute(bool mute_on)
{
    ALOGD("%s(), mute_on = %d, old mDownlinkMuteOn = %d", __FUNCTION__, mute_on, mDownlinkMuteOn);
    if (mute_on == mDownlinkMuteOn) { return NO_ERROR; }

    mDownlinkMuteOn = mute_on;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_MUTE_SPH_DL, mute_on, 0));
}

status_t SpeechDriverLAD::SetUplinkMute(bool mute_on)
{
    ALOGD("%s(), mute_on = %d, old mUplinkMuteOn = %d", __FUNCTION__, mute_on, mUplinkMuteOn);
    if (mute_on == mUplinkMuteOn) { return NO_ERROR; }

    mUplinkMuteOn = mute_on;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_MUTE_SPH_UL, mute_on, 0));
}

status_t SpeechDriverLAD::SetUplinkSourceMute(bool mute_on)
{
    ALOGD("%s(), mute_on = %d, old mUplinkSourceMuteOn = %d", __FUNCTION__, mute_on, mUplinkSourceMuteOn);
    if (mute_on == mUplinkSourceMuteOn) { return NO_ERROR; }

    mUplinkSourceMuteOn = mute_on;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_MUTE_SPH_UL_SOURCE, mute_on, 0));
}

status_t SpeechDriverLAD::SetSidetoneGain(int16_t gain)
{
    ALOGD("%s(), gain = 0x%x, old mSideToneGain = 0x%x", __FUNCTION__, gain, mSideToneGain);
    if (gain == mSideToneGain) { return NO_ERROR; }

    mSideToneGain = gain;
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SIDETONE_VOLUME, gain, 0));
}


status_t SpeechDriverLAD::SetDSPSidetoneFilter(const bool dsp_stf_on)
{
    ALOGD("%s(), dsp_stf_on = %d", __FUNCTION__, dsp_stf_on);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SIDETONE_CONFIG, dsp_stf_on, 0));
}


status_t SpeechDriverLAD::CleanGainValueAndMuteStatus()
{
    ALOGD("%s()", __FUNCTION__);

    // set a unreasonable gain value s.t. the reasonable gain can be set to modem next time
    mDownlinkGain   = kUnreasonableGainValue;
    mUplinkGain     = kUnreasonableGainValue;
    mSideToneGain   = kUnreasonableGainValue;
    mUplinkMuteOn   = false;
    mUplinkSourceMuteOn   = false;
    mDownlinkMuteOn = false;

    return NO_ERROR;
}

/*==============================================================================
 *                     Device related Config
 *============================================================================*/

status_t SpeechDriverLAD::SetModemSideSamplingRate(uint16_t sample_rate)
{
    ALOGD("%s(), sample_rate = %d", __FUNCTION__, sample_rate);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_SET_SAMPLE_RATE, sample_rate, 0));
}

/*==============================================================================
 *                     Speech Enhancement Control
 *============================================================================*/
status_t SpeechDriverLAD::SetSpeechEnhancement(bool enhance_on)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s(), enhance_on = %d, mForceDisableSpeechEnhancement = %d", __FUNCTION__, enhance_on, mForceDisableSpeechEnhancement);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_CTRL_SPH_ENH, enhance_on & !mForceDisableSpeechEnhancement, 0));
}

status_t SpeechDriverLAD::SetSpeechEnhancementMask(const sph_enh_mask_struct_t &mask)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s(), main_func = 0x%x, dynamic_func = 0x%x", __FUNCTION__, mask.main_func, mask.dynamic_func);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_CONFIG_SPH_ENH, mask.main_func, mask.dynamic_func));
}

status_t SpeechDriverLAD::SetBtHeadsetNrecOn(const bool bt_headset_nrec_on)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s(), bt_headset_nrec_on = %d, mSpeechMode = %d", __FUNCTION__, bt_headset_nrec_on, mSpeechMode);
    mBtHeadsetNrecOn = bt_headset_nrec_on;

    status_t retval = NO_ERROR;
    bool bNrecSwitchNeed = false;
    sph_enh_mask_struct_t mask;
#if !defined(MODEM_DYNAMIC_PARAM)//no need to set BtNrecSwitch, use mode param
#if defined(MTK_AUDIO_BT_NREC_WO_ENH_MODE)
    if (mBtHeadsetNrecOn == false && mSpeechMode == SPEECH_MODE_BT_EARPHONE)
    {
        SpeechEnhancementController::GetInstance()->SetBtNrecSwitchNeed(true);
    }
    else if (mBtHeadsetNrecOn == true && mSpeechMode == SPEECH_MODE_BT_CORDLESS)
    {
        SpeechEnhancementController::GetInstance()->SetBtNrecSwitchNeed(true);
    }
    else
    {
        SpeechEnhancementController::GetInstance()->SetBtNrecSwitchNeed(false);
    }

#else
    //Turn on/off only NR&EC by switch mask

    if (mBtHeadsetNrecOn == false && mSpeechMode == SPEECH_MODE_BT_EARPHONE)
    {
        mask = SpeechEnhancementController::GetInstance()->GetNRECMask(false, &bNrecSwitchNeed);
        // Set speech enhancement parameters' mask to modem side
        if (bNrecSwitchNeed)
        {
            ALOGD("-%s(), Set Nrec off to MD", __FUNCTION__);
            retval = SetSpeechEnhancementMask(mask);
        }
    }
    else
    {
        mask = SpeechEnhancementController::GetInstance()->GetNRECMask(true, &bNrecSwitchNeed);
        // Set speech enhancement parameters' mask to modem side
        if (bNrecSwitchNeed)
        {
            ALOGD("-%s(), Set Nrec on to MD", __FUNCTION__);
            retval = SetSpeechEnhancementMask(mask);
        }
    }

#endif
#endif
    return retval;
}


/*==============================================================================
 *                     Speech Enhancement Parameters
 *============================================================================*/
status_t SpeechDriverLAD::SetDynamicSpeechParameters(const int type, const void *param_arg)
{
    int u4I, size_byte;
    char *pPackedSphParamUnit = new char [2048];
    ALOGD("+%s(), type[%d]", __FUNCTION__, type);
    int *u4ParamArg = (int *)param_arg;
#if defined(MTK_AUDIO_HIERARCHICAL_PARAM_SUPPORT)
    switch (type)
    {
        case AUDIO_TYPE_SPEECH:
            memset(pPackedSphParamUnit, 0, 2048);
            //return array with size
            size_byte = SpeechParamParser::getInstance()->GetSpeechParamUnit(pPackedSphParamUnit, u4ParamArg);
#if 0
            for (u4I = 0; u4I<size_byte >> 1; u4I++)
            {
                ALOGV("%s(), pPackedSphParamUnit[%d] = 0x%x", __FUNCTION__, u4I, *((uint16_t *)pPackedSphParamUnit + u4I));
            }
#endif
            SetVariousKindsOfSpeechParameters(pPackedSphParamUnit, size_byte, MSG_A2M_EM_DYNAMIC_SPH);
            break;
        case AUDIO_TYPE_SPEECH_DMNR:
            memset(pPackedSphParamUnit, 0, 2048);
            //return array with size
            size_byte = SpeechParamParser::getInstance()->GetDmnrParamUnit(pPackedSphParamUnit);

#if 0
            for (u4I = 0; u4I<size_byte >> 1; u4I++)
            {
                ALOGV("%s(), DMNR pPackedSphParamUnit[%d] = 0x%x", __FUNCTION__, u4I, *((uint16_t *)pPackedSphParamUnit + u4I));
            }
#endif
            SetVariousKindsOfSpeechParameters(pPackedSphParamUnit, size_byte, MSG_A2M_EM_DYNAMIC_SPH);
            break;
        case AUDIO_TYPE_SPEECH_GENERAL:
            memset(pPackedSphParamUnit, 0, 2048);
            //return array with size
            size_byte = SpeechParamParser::getInstance()->GetGeneralParamUnit(pPackedSphParamUnit);

#if 0
            for (u4I = 0; u4I<size_byte >> 1; u4I++)
            {
                ALOGV("%s(), General pPackedSphParamUnit[%d] = 0x%x", __FUNCTION__, u4I, *((uint16_t *)pPackedSphParamUnit + u4I));
            }
#endif
            SetVariousKindsOfSpeechParameters(pPackedSphParamUnit, size_byte, MSG_A2M_EM_DYNAMIC_SPH);
            break;
        case AUDIO_TYPE_SPEECH_MAGICLARITY:
            memset(pPackedSphParamUnit, 0, 2048);
            //return array with size
            size_byte = SpeechParamParser::getInstance()->GetMagiClarityParamUnit(pPackedSphParamUnit);
#if 0
            for (u4I = 0; u4I<size_byte >> 1; u4I++)
            {
                ALOGV("%s(), MagiClairity pPackedSphParamUnit[%d] = 0x%x", __FUNCTION__, u4I, *((uint16_t *)pPackedSphParamUnit + u4I));
            }
#endif
            SetVariousKindsOfSpeechParameters(pPackedSphParamUnit, size_byte, MSG_A2M_EM_DYNAMIC_SPH);
            break;
        default:
            break;

    }
#endif
    if (pPackedSphParamUnit != NULL)
    {
        delete[] pPackedSphParamUnit;
    }
    return NO_ERROR;

}

status_t SpeechDriverLAD::SetVariousKindsOfSpeechParameters(const void *param, const uint16_t data_length, const uint16_t ccci_message_id)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    if (pCCCI->GetMDResetFlag() == false) // check MD Reset Flag
    {
        if (pCCCI->A2MBufLock() == false) // get buffer lock to prevent overwrite other's data
        {
            ALOGE("%s() fail due to unalbe get A2MBufLock, ccci_message_id = 0x%x", __FUNCTION__, ccci_message_id);
            return TIMED_OUT;
        }
        else
        {
            // get share buffer address
            uint16_t offset = A2M_SHARED_BUFFER_SPH_PARAM_BASE;
            char *p_header_address = pCCCI->GetA2MShareBufBase() + offset;
            char *p_data_address = p_header_address + CCCI_SHARE_BUFF_HEADER_LEN;
            share_buff_data_type_t type = SHARE_BUFF_DATA_TYPE_CCCI_EM_PARAM;

            switch (ccci_message_id)
            {
#if defined(MODEM_DYNAMIC_PARAM)
                case MSG_A2M_EM_DYNAMIC_SPH:
                    type = SHARE_BUFF_DATA_TYPE_CCCI_DYNAMIC_PARAM_TYPE;
                    break;

                default:
                    type = SHARE_BUFF_DATA_TYPE_CCCI_DYNAMIC_PARAM_TYPE;
                    break;
#else
                case MSG_A2M_EM_MAGICON:
                    type = SHARE_BUFF_DATA_TYPE_CCCI_MAGICON_PARAM;
                    break;
                case MSG_A2M_EM_HAC:
                    type = SHARE_BUFF_DATA_TYPE_CCCI_HAC_PARAM;
                    break;

                default:
                    type = SHARE_BUFF_DATA_TYPE_CCCI_EM_PARAM;
                    break;
#endif
            }

            ALOGD("%s() type = %d, ccci_message_id = 0x%x", __FUNCTION__, type, ccci_message_id);

            // fill header info
            pCCCI->SetShareBufHeader((uint16_t *)p_header_address,
                                     CCCI_A2M_SHARE_BUFF_HEADER_SYNC,
                                     type,
                                     data_length);

            // fill speech enhancement parameter
            memcpy((void *)p_data_address, (void *)param, data_length);

            // send data notify to modem side
            const uint16_t payload_length = CCCI_SHARE_BUFF_HEADER_LEN + data_length;
#if defined(MODEM_DYNAMIC_PARAM)
            if (ccci_message_id != MSG_A2M_EM_DYNAMIC_SPH)
            {
                ALOGE("%s(), only support Speech Dynamic Parameters", __FUNCTION__);
                pCCCI->A2MBufUnLock();
                return NO_ERROR;
            }
#endif

            return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(ccci_message_id, payload_length, offset));
        }
    }
    else
    {

        ALOGD("%s(), SKIP Speech Parameters setting because MD Reset", __FUNCTION__);
        return NO_ERROR;

    }
}


status_t SpeechDriverLAD::SetNBSpeechParameters(const AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s()", __FUNCTION__);
    return SetVariousKindsOfSpeechParameters(pSphParamNB, sizeof(AUDIO_CUSTOM_PARAM_STRUCT), MSG_A2M_EM_NB);
}

#if defined(MTK_DUAL_MIC_SUPPORT)
status_t SpeechDriverLAD::SetDualMicSpeechParameters(const AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s()", __FUNCTION__);

#if defined(MTK_WB_SPEECH_SUPPORT)
    // NVRAM always contain(44+76), for WB we send full (44+76)
    uint16_t data_length = sizeof(unsigned short) * (NUM_ABF_PARAM + NUM_ABFWB_PARAM); // NB + WB

    // Check if support Loud Speaker Mode DMNR
    if (sizeof(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT) >= (data_length * 2))
    {
        data_length *= 2; // 1 for receiver mode DMNR, 1 for loud speaker mode DMNR
    }
#else
    // for NB we send (44) only
    uint16_t data_length = sizeof(unsigned short) * (NUM_ABF_PARAM); // NB Only
#endif

    return SetVariousKindsOfSpeechParameters(pSphParamDualMic, data_length, MSG_A2M_EM_DMNR);
}
#else
status_t SpeechDriverLAD::SetDualMicSpeechParameters(const AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic)
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}
#endif

status_t SpeechDriverLAD::SetMagiConSpeechParameters(const AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT *pSphParamMagiCon)
{
#if defined(MTK_MAGICONFERENCE_SUPPORT) && defined(MTK_DUAL_MIC_SUPPORT)
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s()", __FUNCTION__);

    uint16_t data_length = sizeof(AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT); // NB + WB

    return SetVariousKindsOfSpeechParameters(pSphParamMagiCon, data_length, MSG_A2M_EM_MAGICON);
#else
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
#endif
}

status_t SpeechDriverLAD::SetHACSpeechParameters(const AUDIO_CUSTOM_HAC_PARAM_STRUCT *pSphParamHAC)
{
#if defined(MTK_HAC_SUPPORT)
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    AUDIO_CUSTOM_HAC_SPEECH_PARAM_STRUCT mSphParamHAC;
    memcpy(&mSphParamHAC, pSphParamHAC, sizeof(AUDIO_CUSTOM_HAC_SPEECH_PARAM_STRUCT));

    uint16_t data_length = sizeof(AUDIO_CUSTOM_HAC_SPEECH_PARAM_STRUCT); // NB + WB
    ALOGD("%s(), data_length=%d", __FUNCTION__, data_length);
    return SetVariousKindsOfSpeechParameters(&mSphParamHAC, data_length, MSG_A2M_EM_HAC);

#else
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
#endif
}

#if defined(MTK_WB_SPEECH_SUPPORT)
status_t SpeechDriverLAD::SetWBSpeechParameters(const AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB)
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    ALOGD("%s()", __FUNCTION__);
    return SetVariousKindsOfSpeechParameters(pSphParamWB, sizeof(AUDIO_CUSTOM_WB_PARAM_STRUCT), MSG_A2M_EM_WB);
}
#else
status_t SpeechDriverLAD::SetWBSpeechParameters(const AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB)
{
    ALOGE("%s()", __FUNCTION__);
    return INVALID_OPERATION;
}
#endif

//#if defined(MTK_VIBSPK_SUPPORT)
const int16_t SPH_VIBR_FILTER_COEF_Table[VIBSPK_FILTER_NUM][VIBSPK_SPH_PARAM_SIZE] =
{
    DEFAULT_SPH_VIBR_FILTER_COEF_141,
    DEFAULT_SPH_VIBR_FILTER_COEF_144,
    DEFAULT_SPH_VIBR_FILTER_COEF_147,
    DEFAULT_SPH_VIBR_FILTER_COEF_150,
    DEFAULT_SPH_VIBR_FILTER_COEF_153,
    DEFAULT_SPH_VIBR_FILTER_COEF_156,
    DEFAULT_SPH_VIBR_FILTER_COEF_159,
    DEFAULT_SPH_VIBR_FILTER_COEF_162,
    DEFAULT_SPH_VIBR_FILTER_COEF_165,
    DEFAULT_SPH_VIBR_FILTER_COEF_168,
    DEFAULT_SPH_VIBR_FILTER_COEF_171,
    DEFAULT_SPH_VIBR_FILTER_COEF_174,
    DEFAULT_SPH_VIBR_FILTER_COEF_177,
    DEFAULT_SPH_VIBR_FILTER_COEF_180,
    DEFAULT_SPH_VIBR_FILTER_COEF_183,
    DEFAULT_SPH_VIBR_FILTER_COEF_186,
    DEFAULT_SPH_VIBR_FILTER_COEF_189,
    DEFAULT_SPH_VIBR_FILTER_COEF_192,
    DEFAULT_SPH_VIBR_FILTER_COEF_195,
    DEFAULT_SPH_VIBR_FILTER_COEF_198,
    DEFAULT_SPH_VIBR_FILTER_COEF_201,
    DEFAULT_SPH_VIBR_FILTER_COEF_204,
    DEFAULT_SPH_VIBR_FILTER_COEF_207,
    DEFAULT_SPH_VIBR_FILTER_COEF_210,
    DEFAULT_SPH_VIBR_FILTER_COEF_213,
    DEFAULT_SPH_VIBR_FILTER_COEF_216,
    DEFAULT_SPH_VIBR_FILTER_COEF_219,
    DEFAULT_SPH_VIBR_FILTER_COEF_222,
    DEFAULT_SPH_VIBR_FILTER_COEF_225,
    DEFAULT_SPH_VIBR_FILTER_COEF_228,
    DEFAULT_SPH_VIBR_FILTER_COEF_231,
    DEFAULT_SPH_VIBR_FILTER_COEF_234,
    DEFAULT_SPH_VIBR_FILTER_COEF_237,
    DEFAULT_SPH_VIBR_FILTER_COEF_240,
    DEFAULT_SPH_VIBR_FILTER_COEF_243,
    DEFAULT_SPH_VIBR_FILTER_COEF_246,
    DEFAULT_SPH_VIBR_FILTER_COEF_249,
    DEFAULT_SPH_VIBR_FILTER_COEF_252,
    DEFAULT_SPH_VIBR_FILTER_COEF_255,
    DEFAULT_SPH_VIBR_FILTER_COEF_258,
    DEFAULT_SPH_VIBR_FILTER_COEF_261,
    DEFAULT_SPH_VIBR_FILTER_COEF_264,
    DEFAULT_SPH_VIBR_FILTER_COEF_267,
    DEFAULT_SPH_VIBR_FILTER_COEF_270,
    DEFAULT_SPH_VIBR_FILTER_COEF_273,
    DEFAULT_SPH_VIBR_FILTER_COEF_276,
    DEFAULT_SPH_VIBR_FILTER_COEF_279,
    DEFAULT_SPH_VIBR_FILTER_COEF_282,
    DEFAULT_SPH_VIBR_FILTER_COEF_285,
    DEFAULT_SPH_VIBR_FILTER_COEF_288,
    DEFAULT_SPH_VIBR_FILTER_COEF_291,
    DEFAULT_SPH_VIBR_FILTER_COEF_294,
    DEFAULT_SPH_VIBR_FILTER_COEF_297,
    DEFAULT_SPH_VIBR_FILTER_COEF_300,
    DEFAULT_SPH_VIBR_FILTER_COEF_303,
    DEFAULT_SPH_VIBR_FILTER_COEF_306,
    DEFAULT_SPH_VIBR_FILTER_COEF_309,
    DEFAULT_SPH_VIBR_FILTER_COEF_312,
    DEFAULT_SPH_VIBR_FILTER_COEF_315,
    DEFAULT_SPH_VIBR_FILTER_COEF_318,
    DEFAULT_SPH_VIBR_FILTER_COEF_321,
    DEFAULT_SPH_VIBR_FILTER_COEF_324,
    DEFAULT_SPH_VIBR_FILTER_COEF_327,
    DEFAULT_SPH_VIBR_FILTER_COEF_330,
};

typedef struct
{
    short pParam[15];
    bool  flag2in1;
} PARAM_VIBSPK;

#ifndef VIBSPK_DEFAULT_FREQ
#define VIBSPK_DEFAULT_FREQ (156) //141~330 Hz
#endif

status_t SpeechDriverLAD::GetVibSpkParam(void *eVibSpkParam)
{
    int32_t frequency;
    AUDIO_ACF_CUSTOM_PARAM_STRUCT audioParam;
    GetAudioCompFltCustParamFromNV(AUDIO_COMP_FLT_VIBSPK, &audioParam);
    PARAM_VIBSPK *pParamVibSpk = (PARAM_VIBSPK *)eVibSpkParam;
    int dTableIndex;

    if (audioParam.bes_loudness_WS_Gain_Max != VIBSPK_CALIBRATION_DONE && audioParam.bes_loudness_WS_Gain_Max != VIBSPK_SETDEFAULT_VALUE)
    {
        frequency = VIBSPK_DEFAULT_FREQ;
    }
    else
    {
        frequency = audioParam.bes_loudness_WS_Gain_Min;
    }

    if (frequency < VIBSPK_FREQ_LOWBOUND)
    {
        dTableIndex = 0;
    }
    else
    {
        dTableIndex = (frequency - VIBSPK_FREQ_LOWBOUND + 1) / VIBSPK_FILTER_FREQSTEP;
    }

    if (dTableIndex < VIBSPK_FILTER_NUM && dTableIndex >= 0)
    {
        memcpy(pParamVibSpk->pParam, &SPH_VIBR_FILTER_COEF_Table[dTableIndex], sizeof(uint16_t)*VIBSPK_SPH_PARAM_SIZE);
    }

    if (IsAudioSupportFeature(AUDIO_SUPPORT_2IN1_SPEAKER))
    {
        pParamVibSpk->flag2in1 = false;
    }
    else
    {
        pParamVibSpk->flag2in1 = true;
    }

    return NO_ERROR;
}

status_t SpeechDriverLAD::SetVibSpkParam(void *eVibSpkParam)
{
    if (pCCCI->A2MBufLock() == false)
    {
        ALOGE("%s() fail due to unalbe get A2MBufLock, ccci_message_id = 0x%x", __FUNCTION__, MSG_A2M_VIBSPK_PARAMETER);
        ALOGD("VibSpkSetSphParam Fail!");
        return TIMED_OUT;
    }
    else
    {
        PARAM_VIBSPK paramVibspk;
        uint16_t offset = A2M_SHARED_BUFFER_SPH_PARAM_BASE;
        char *p_header_address = pCCCI->GetA2MShareBufBase() + offset;
        char *p_data_address = p_header_address + CCCI_SHARE_BUFF_HEADER_LEN;
        uint16_t data_length = sizeof(PARAM_VIBSPK);
        ALOGD("VibSpkSetSphParam Success!");
        // fill header info
        pCCCI->SetShareBufHeader((uint16_t *)p_header_address,
                                 CCCI_A2M_SHARE_BUFF_HEADER_SYNC,
                                 SHARE_BUFF_DATA_TYPE_CCCI_VIBSPK_PARAM,
                                 data_length);

        // fill speech enhancement parameter
        memcpy((void *)p_data_address, (void *)eVibSpkParam, data_length);

        // send data notify to modem side
        const uint16_t message_length = CCCI_SHARE_BUFF_HEADER_LEN + data_length;
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_VIBSPK_PARAMETER, message_length, offset));
    }

}
//#endif //defined(MTK_VIBSPK_SUPPORT)

typedef struct
{
    bool isNxpFeatureOptOn;
    short switch_and_delay;   // bit8: switch; bit7~0: delay
    short mic_index;   // bit wise definition ordered from main mic to reference mic. Only one bit is set!! bit 0: o17, bit 1: o18, bit 2: o23, bit 3: o24, bit 4: o25
} PARAM_NXP_SMARTPA;

#define NXP_SMARTPA_SUPPORT_BIT    8
#define MIC_INDEX_O17              1<<0
#define MIC_INDEX_O18              1<<1
#define MIC_INDEX_O23              1<<2
#define MIC_INDEX_O24              1<<3
#define MIC_INDEX_O25              1<<4

status_t SpeechDriverLAD::GetNxpSmartpaParam(void *eParamNxpSmartpa)
{
    PARAM_NXP_SMARTPA *pParamNxpSmartpa = (PARAM_NXP_SMARTPA *)eParamNxpSmartpa;;

#if defined(NXP_SMARTPA_SUPPORT)
    pParamNxpSmartpa->isNxpFeatureOptOn = 1;
    pParamNxpSmartpa->switch_and_delay = ((pParamNxpSmartpa->isNxpFeatureOptOn << NXP_SMARTPA_SUPPORT_BIT) | CHIP_DELAY);
    pParamNxpSmartpa->mic_index = MIC_INDEX_O24;
#elif defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
    char property_value[PROPERTY_VALUE_MAX];
    property_get("streamout.speech_stream.enable", property_value, "1");
    int speech_stream = atoi(property_value);
    pParamNxpSmartpa->isNxpFeatureOptOn = 1;
    if (speech_stream == 0)
    {
        pParamNxpSmartpa->switch_and_delay = (pParamNxpSmartpa->isNxpFeatureOptOn << NXP_SMARTPA_SUPPORT_BIT) ;
        pParamNxpSmartpa->mic_index = MIC_INDEX_O17;
    }
    else
    {
        pParamNxpSmartpa->switch_and_delay = ((pParamNxpSmartpa->isNxpFeatureOptOn << NXP_SMARTPA_SUPPORT_BIT) | CHIP_DELAY);
        pParamNxpSmartpa->mic_index = MIC_INDEX_O24;
    }
#else
    pParamNxpSmartpa->isNxpFeatureOptOn = 0;
    pParamNxpSmartpa->switch_and_delay = (pParamNxpSmartpa->isNxpFeatureOptOn << NXP_SMARTPA_SUPPORT_BIT) ;
    pParamNxpSmartpa->mic_index = MIC_INDEX_O17;
#endif

    ALOGD("%s, isNxpFeatureOptOn=%d, switch_and_delay=%d, mic_index=%d", __FUNCTION__, pParamNxpSmartpa->isNxpFeatureOptOn, pParamNxpSmartpa->switch_and_delay, pParamNxpSmartpa->mic_index);

    return NO_ERROR;
}

status_t SpeechDriverLAD::SetNxpSmartpaParam(void *eParamNxpSmartpa)
{
    if (pCCCI->A2MBufLock() == false)
    {
        ALOGE("%s() fail due to unalbe get A2MBufLock, ccci_message_id = 0x%x", __FUNCTION__, MSG_A2M_NXP_SMARTPA_PARAMETER);
        ALOGD("NxpSmartpaSetSphParam Fail!");
        return TIMED_OUT;
    }
    else
    {
        //PARAM_NXP_SMARTPA paramNxpSmartpa;
        uint16_t offset = A2M_SHARED_BUFFER_SPH_PARAM_BASE;
        char *p_header_address = pCCCI->GetA2MShareBufBase() + offset;
        char *p_data_address = p_header_address + CCCI_SHARE_BUFF_HEADER_LEN;
        uint16_t data_length = sizeof(PARAM_NXP_SMARTPA);
        ALOGD("NxpSmartpaSetSphParam Success!");
        // fill header info
        pCCCI->SetShareBufHeader((uint16_t *)p_header_address,
                                 CCCI_A2M_SHARE_BUFF_HEADER_SYNC,
                                 SHARE_BUFF_DATA_TYPE_CCCI_NXP_SMARTPA_PARAM,
                                 data_length);

        // fill speech enhancement parameter
        memcpy((void *)p_data_address, (void *)eParamNxpSmartpa, data_length);

        // send data notify to modem side
        const uint16_t message_length = CCCI_SHARE_BUFF_HEADER_LEN + data_length;
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_NXP_SMARTPA_PARAMETER, message_length, offset));
    }

}

status_t SpeechDriverLAD::SetAllSpeechEnhancementInfoToModem()
{
    // Wait until modem ready
    if (pCCCI->WaitUntilModemReady() == TIMED_OUT)
    {
        ALOGD("%s() time out", __FUNCTION__);
        return NO_ERROR;
    }

    // Lock
    static AudioLock _mutex;
    _mutex.lock_timeout(10000); // wait 10 sec


#if defined(MODEM_DYNAMIC_PARAM)

    int param_arg[2];
#ifdef DMNR_GENERAL_SET_DURING_BOOT
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_GENERAL, param_arg);

#ifdef MTK_SPH_MAGICLARITY_SHAPEFIR_SUPPORT
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_MAGICLARITY, param_arg);
#endif
#if defined(MTK_DUAL_MIC_SUPPORT)
    SetDynamicSpeechParameters((int)AUDIO_TYPE_SPEECH_DMNR, param_arg);
#endif
#endif

#else
    // NB Speech Enhancement Parameters
    AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
    GetNBSpeechParamFromNVRam(&eSphParamNB);
    SetNBSpeechParameters(&eSphParamNB);
    ALOGD("NVRAM debug: speech_mode_para[0][0]=%d should not be zero", eSphParamNB.speech_mode_para[0][0]);

#if defined(MTK_DUAL_MIC_SUPPORT)
    // Dual Mic Speech Enhancement Parameters
    AUDIO_CUSTOM_EXTRA_PARAM_STRUCT eSphParamDualMic;
    GetDualMicSpeechParamFromNVRam(&eSphParamDualMic);
    SetDualMicSpeechParameters(&eSphParamDualMic);

#if defined(MTK_MAGICONFERENCE_SUPPORT)
    AUDIO_CUSTOM_MAGI_CONFERENCE_STRUCT eSphParamMagiCon;
    GetMagiConSpeechParamFromNVRam(&eSphParamMagiCon);
    SetMagiConSpeechParameters(&eSphParamMagiCon);
#endif
#endif

#if defined(MTK_HAC_SUPPORT)
    AUDIO_CUSTOM_HAC_PARAM_STRUCT eSphParamHAC;
    GetHACSpeechParamFromNVRam(&eSphParamHAC);
    SetHACSpeechParameters(&eSphParamHAC);
#endif

#if defined(MTK_WB_SPEECH_SUPPORT)
    // WB Speech Enhancement Parameters
    AUDIO_CUSTOM_WB_PARAM_STRUCT eSphParamWB;
    GetWBSpeechParamFromNVRam(&eSphParamWB);
    SetWBSpeechParameters(&eSphParamWB);
#endif
    //#if defined(MTK_VIBSPK_SUPPORT)
    if (IsAudioSupportFeature(AUDIO_SUPPORT_VIBRATION_SPEAKER))
    {
        PARAM_VIBSPK eVibSpkParam;
        GetVibSpkParam((void *)&eVibSpkParam);
        SetVibSpkParam((void *)&eVibSpkParam);
    }
    //#endif

#if defined(NXP_SMARTPA_SUPPORT) || defined(MTK_SPEAKER_MONITOR_SPEECH_SUPPORT)
    PARAM_NXP_SMARTPA eNxpSmartpaParam;
    GetNxpSmartpaParam((void *)&eNxpSmartpaParam);
    SetNxpSmartpaParam((void *)&eNxpSmartpaParam);
#endif
#endif

    // Set speech enhancement parameters' mask to modem side
    SetSpeechEnhancementMask(SpeechEnhancementController::GetInstance()->GetSpeechEnhancementMask());

    //    // Use lock to ensure the previous command with share buffer control is completed
    //    if (pCCCI->A2MBufLock() == true)
    //    {
    //        pCCCI->A2MBufUnLock();
    //    }
    //    else
    //    {
    //        ALOGE("%s() fail to get A2M Buffer Lock!!", __FUNCTION__);
    //    }

    // Unock
    _mutex.unlock();
    return NO_ERROR;
}


/*==============================================================================
 *                     Recover State
 *============================================================================*/

void SpeechDriverLAD::RecoverModemSideStatusToInitState()
{
    // Record
    if (pCCCI->GetModemSideModemStatus(RECORD_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, record_on = true",  __FUNCTION__, mModemIndex);
        SetApSideModemStatus(RECORD_STATUS_MASK);
        RecordOff();
    }

    // Raw Record
    if (pCCCI->GetModemSideModemStatus(RAW_RECORD_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, raw_record_on = true",  __FUNCTION__, mModemIndex);
        SetApSideModemStatus(RAW_RECORD_STATUS_MASK);
        RecordOff(mRecordType);
    }

    // VM Record
    if (pCCCI->GetModemSideModemStatus(VM_RECORD_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, vm_on = true",  __FUNCTION__, mModemIndex);
        SetApSideModemStatus(VM_RECORD_STATUS_MASK);
        VoiceMemoRecordOff();
    }

    // BGS
    if (pCCCI->GetModemSideModemStatus(BGS_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, bgs_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(BGS_STATUS_MASK);
        BGSoundOff();
    }

    // TTY
    if (pCCCI->GetModemSideModemStatus(TTY_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, tty_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(TTY_STATUS_MASK);
        TtyCtmOff();
    }

    // P2W
    if (pCCCI->GetModemSideModemStatus(P2W_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, p2w_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(P2W_STATUS_MASK);
        PCM2WayOff();
    }

    // Phone Call / Loopback
    if (pCCCI->GetModemSideModemStatus(VT_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, vt_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(VT_STATUS_MASK);
        VideoTelephonyOff();
    }
    else if (pCCCI->GetModemSideModemStatus(SPEECH_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, speech_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(SPEECH_STATUS_MASK);
        SpeechOff();
    }
    else if (pCCCI->GetModemSideModemStatus(SPEECH_ROUTER_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, speech_router_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(SPEECH_ROUTER_STATUS_MASK);
        SpeechRouterOff();
    }
    else if (pCCCI->GetModemSideModemStatus(LOOPBACK_STATUS_MASK) == true)
    {
        ALOGD("%s(), modem_index = %d, loopback_on = true", __FUNCTION__, mModemIndex);
        SetApSideModemStatus(LOOPBACK_STATUS_MASK);
        SetAcousticLoopback(false);
    }
}

/*==============================================================================
 *                     Check Modem Status
 *============================================================================*/
bool SpeechDriverLAD::CheckModemIsReady()
{
    return pCCCI->CheckModemIsReady();
};

bool SpeechDriverLAD::QueryModemRFInfo()
{
#ifdef SPH_SKIP_A2M_BUFF_MSG
    return NO_ERROR;
#endif
    // Wait until modem ready
    if (pCCCI->WaitUntilModemReady() == NO_ERROR)
    {
        ALOGD("%s()", __FUNCTION__);
        return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_QUERY_RF_INFO, 0, 0));
    }
    else
    {
        ALOGD("%s() time out", __FUNCTION__);
        return NO_ERROR;
    }
};

/*==============================================================================
 *                     Debug Info
 *============================================================================*/
status_t SpeechDriverLAD::ModemDumpSpeechParam()
{
    ALOGD("%s()", __FUNCTION__);
    return pCCCI->SendMessageInQueue(pCCCI->InitCcciMailbox(MSG_A2M_PRINT_SPH_PARAM, 0, 0));
}

} // end of namespace android
