#ifndef ANDROID_AUDIO_MTK_HARDWARE_COMMONCOMMAND_CPP
#define ANDROID_AUDIO_MTK_HARDWARE_COMMONCOMMAND_CPP

/*==============================================================================
 *                     setParameters() keys for common
 *============================================================================*/

// Phone Call Related
static String8 keySetVTSpeechCall     = String8("SetVTSpeechCall");
static String8 keySetFlightMode     = String8("SetFlightMode");

static String8 keyBtHeadsetNrec       = String8("bt_headset_nrec");
static String8 keyBtSupportVolume = String8("bt_headset_vgs");
// Set BGS Mute
static String8 keySet_BGS_DL_Mute = String8("Set_BGS_DL_Mute");
static String8 keySet_BGS_UL_Mute    = String8("Set_BGS_UL_Mute");
// Set Phone Call Mute
static String8 keySet_SpeechCall_DL_Mute = String8("Set_SpeechCall_DL_Mute");
static String8 keySet_SpeechCall_UL_Mute = String8("Set_SpeechCall_UL_Mute");

// FM Related
static String8 keyAnalogFmEnable      = String8("AudioSetFmEnable");
static String8 keyDigitalFmEnable     = String8("AudioSetFmDigitalEnable");
static String8 keyGetFmEnable         = String8("GetFmEnable");

static String8 keySetFmVolume         = String8("SetFmVolume");

static String8 keySetFmForceToSpk     = String8("AudioSetForceToSpeaker");

static String8 keyGetIsWiredHeadsetOn = String8("AudioFmIsWiredHeadsetOn");

//mATV Related
static String8 keyMatvAnalogEnable    = String8("AtvAudioLineInEnable");;
static String8 keyMatvDigitalEnable   = String8("AudioSetMatvDigitalEnable");
static String8 keySetMatvVolume       = String8("SetMatvVolume");
static String8 keySetMatvMute         = String8("SetMatvMute");

//Analog volume
static String8 keyVolumeStreamType    = String8("volumeStreamType");;
static String8 keyVolumeDevice        = String8("volumeDevice");
static String8 keyVolumeIndex         = String8("volumeIndex");
static String8 keySpeechBand          = String8("getSpeechBand");

//record left/right channel switch
//only support on dual MIC for switch LR input channel for video record when the device rotate
static String8 keyLR_ChannelSwitch = String8("LRChannelSwitch");
//force use Min MIC or Ref MIC data
//only support on dual MIC for only get main Mic or Ref Mic data
static String8 keyForceUseSpecificMicData = String8("ForceUseSpecificMic");

#ifdef MTK_AUDIO_HD_REC_SUPPORT
//static String8 key_HD_REC_MODE = String8("HDRecordMode");
static String8 keyHDREC_SET_VOICE_MODE = String8("HDREC_SET_VOICE_MODE");
static String8 keyHDREC_SET_VIDEO_MODE = String8("HDREC_SET_VIDEO_MODE");
#endif

//HDMI command
static String8 key_GET_HDMI_AUDIO_STATUS = String8("GetHDMIAudioStatus");
static String8 key_SET_HDMI_AUDIO_ENABLE = String8("SetHDMIAudioEnable");


// Audio Tool related
//<---for audio tool(speech/ACF/HCF/DMNR/HD/Audiotaste calibration) and HQA
static String8 keySpeechParams_Update = String8("UpdateSpeechParameter");
static String8 keySpeechVolume_Update = String8("UpdateSphVolumeParameter");
static String8 keyACFHCF_Update = String8("UpdateACFHCFParameters");
#if defined(MTK_DUAL_MIC_SUPPORT) || defined(MTK_AUDIO_HD_REC_SUPPORT)
static String8 keyDualMicParams_Update = String8("UpdateDualMicParameters");
static String8 keyDualMicRecPly = String8("DUAL_MIC_REC_PLAY");
static String8 keyDUALMIC_IN_FILE_NAME = String8("DUAL_MIC_IN_FILE_NAME");
static String8 keyDUALMIC_OUT_FILE_NAME = String8("DUAL_MIC_OUT_FILE_NAME");
static String8 keyDUALMIC_GET_GAIN = String8("DUAL_MIC_GET_GAIN");
static String8 keyDUALMIC_SET_UL_GAIN = String8("DUAL_MIC_SET_UL_GAIN");
static String8 keyDUALMIC_SET_DL_GAIN = String8("DUAL_MIC_SET_DL_GAIN");
static String8 keyDUALMIC_SET_HSDL_GAIN = String8("DUAL_MIC_SET_HSDL_GAIN");
static String8 keyDUALMIC_SET_UL_GAIN_HF = String8("DUAL_MIC_SET_UL_GAIN_HF");
#endif
static String8 keyMusicPlusSet      = String8("SetMusicPlusStatus");
static String8 keyMusicPlusGet      = String8("GetMusicPlusStatus");
static String8 keyHiFiDACSet      = String8("SetHiFiDACStatus");
static String8 keyHiFiDACGet      = String8("GetHiFiDACStatus");
static String8 keyHDRecTunningEnable    = String8("HDRecTunningEnable");
static String8 keyHDRecVMFileName   = String8("HDRecVMFileName");

static String8 keyBesLoudnessSet      = String8("SetBesLoudnessStatus");
static String8 keyBesLoudnessGet      = String8("GetBesLoudnessStatus");

static String8 keyGainTable_Update = String8("UpdateHALCustGainTable");
static String8 keyMicGain_Update = String8("UpdateMicGain");

static String8 keyHFPParam_Update = String8("UpdateHFPParameters");
static String8 keyhfp_volume = String8("hfp_volume");

//--->




// Dual Mic Noise Reduction, DMNR for Receiver
static String8 keyEnable_Dual_Mic_Setting = String8("Enable_Dual_Mic_Setting");
static String8 keyGet_Dual_Mic_Setting    = String8("Get_Dual_Mic_Setting");

// Dual Mic Noise Reduction, DMNR for Loud Speaker
static String8 keySET_LSPK_DMNR_ENABLE = String8("SET_LSPK_DMNR_ENABLE");
static String8 keyGET_LSPK_DMNR_ENABLE = String8("GET_LSPK_DMNR_ENABLE");

// Voice Clarity Engine, VCE
static String8 keySET_VCE_ENABLE = String8("SET_VCE_ENABLE");
static String8 keyGET_VCE_ENABLE = String8("GET_VCE_ENABLE");
static String8 keyGET_VCE_STATUS = String8("GET_VCE_STATUS"); // old name, rename to GET_VCE_ENABLE, but still reserve it

// Magic Conference Call
static String8 keySET_MAGIC_CON_CALL_ENABLE = String8("SET_MAGIC_CON_CALL_ENABLE");
static String8 keyGET_MAGIC_CON_CALL_ENABLE = String8("GET_MAGIC_CON_CALL_ENABLE");

//VoIP
//VoIP Dual Mic Noise Reduction, DMNR for Receiver
static String8 keySET_VOIP_RECEIVER_DMNR_ENABLE = String8("SET_VOIP_RECEIVER_DMNR_ENABLE");
static String8 keyGET_VOIP_RECEIVER_DMNR_ENABLE    = String8("GET_VOIP_RECEIVER_DMNR_ENABLE");

//VoIP Dual Mic Noise Reduction, DMNR for Loud Speaker
static String8 keySET_VOIP_LSPK_DMNR_ENABLE = String8("SET_VOIP_LSPK_DMNR_ENABLE");
static String8 keyGET_VOIP_LSPK_DMNR_ENABLE = String8("GET_VOIP_LSPK_DMNR_ENABLE");

static String8 keyGET_AUDIO_VOLUME_VER = String8("GET_AUDIO_VOLUME_VERSION");

// Loopbacks
static String8 keySET_LOOPBACK_USE_LOUD_SPEAKER = String8("SET_LOOPBACK_USE_LOUD_SPEAKER");
static String8 keySET_LOOPBACK_TYPE = String8("SET_LOOPBACK_TYPE");
static String8 keySET_LOOPBACK_MODEM_DELAY_FRAMES = String8("SET_LOOPBACK_MODEM_DELAY_FRAMES");
static String8 keySET_AT_ACS = String8("SET_AT_ACS");  //for TC1 AT%ACS
static String8 keySET_AT_ACSVolume = String8("SET_AT_ACSVolume");  //for ACS volume

// TTY
static String8 keySetTtyMode     = String8("tty_mode");
#ifdef EVDO_DT_VEND_SUPPORT
//VEND EVDO
static String8 keySET_WARNING_TONE = String8("SetWarningTone");
static String8 keySTOP_WARNING_TONE = String8("StopWarningTone");
static String8 keySET_VOICE_VOLUME_INDEX = String8("SetVoiceVolumeIndex");
#endif
//#if defined(MTK_VIBSPK_SUPPORT)
static String8 keySET_VIBSPK_ENABLE = String8("SET_VIBSPK_ENABLE");
static String8 keySET_VIBSPK_RAMPDOWN = String8("SET_VIBSPK_RAMPDOWN");
//#endif

static String8 keySCREEN_STATE = String8("screen_state");
static String8 keySET_KERNEL_DEBUG_MODE = String8("SET_KERNEL_DEBUG_MODE");

// BT WB
static String8 keySetBTMode     = String8("bt_wbs");

// for stereo output
static String8 keyEnableStereoOutput = String8("EnableStereoOutput");

//audiohfp relate
static String8 keyEnableHfp = String8("hfp_enable");
static String8 keyHfpSampleRate = String8("hfp_set_sampling_rate");

static String8 keyMTK_AUDENH_SUPPORT = String8("MTK_AUDENH_SUPPORT");
static String8 keyMTK_TTY_SUPPORT = String8("MTK_TTY_SUPPORT");
static String8 keyMTK_WB_SPEECH_SUPPORT = String8("MTK_WB_SPEECH_SUPPORT");
static String8 keyMTK_DUAL_MIC_SUPPORT = String8("MTK_DUAL_MIC_SUPPORT");
static String8 keyMTK_AUDIO_HD_REC_SUPPORT = String8("MTK_AUDIO_HD_REC_SUPPORT");
static String8 keyMTK_BESLOUDNESS_SUPPORT = String8("MTK_BESLOUDNESS_SUPPORT");
static String8 keyMTK_BESSURROUND_SUPPORT = String8("MTK_BESSURROUND_SUPPORT");
static String8 keyMTK_HDMI_MULTI_CHANNEL_SUPPORT = String8("MTK_HDMI_MULTI_CHANNEL_SUPPORT");
static String8 keyAUDIO_DUMP_OUT_STREAMOUT = String8("AUDIO_DUMP_OUT_STREAMOUT");
static String8 keyAUDIO_DUMP_IN_STREAMIN = String8("AUDIO_DUMP_IN_STREAMIN");



status_t AudioMTKHardware::SetAudioCommonCommand(int par1, int par2)
{
    ALOGD("%s(), par1 = 0x%x, par2 = %d", __FUNCTION__, par1, par2);
    char value[PROPERTY_VALUE_MAX];
    switch (par1)
    {
        case SETOUTPUTFIRINDEX:
        {
            UpdateOutputFIR(Normal_Coef_Index, par2);
            break;
        }
        case START_FMTX_SINEWAVE:
        {
            return NO_ERROR;
        }
        case STOP_FMTX_SINEWAVE:
        {
            return NO_ERROR;
        }
        case SETNORMALOUTPUTFIRINDEX:
        {
            UpdateOutputFIR(Normal_Coef_Index, par2);
            break;
        }
        case SETHEADSETOUTPUTFIRINDEX:
        {
            UpdateOutputFIR(Headset_Coef_Index, par2);
            break;
        }
        case SETSPEAKEROUTPUTFIRINDEX:
        {
            UpdateOutputFIR(Handfree_Coef_Index, par2);
            break;
        }
        case SET_LOAD_VOLUME_SETTING:
        {
            mAudioVolumeInstance->initVolumeController();
            setMasterVolume(mAudioVolumeInstance->getMasterVolume());
#ifndef MTK_BASIC_PACKAGE
            //Do it at AudioSystem, avoid deadlock, Hochi
//          const sp<IAudioPolicyService> &aps = AudioSystem::get_audio_policy_service();
//          aps->SetPolicyManagerParameters(POLICY_LOAD_VOLUME, 0, 0, 0);
#endif
            break;
        }
        case SET_SPEECH_VM_ENABLE:
        {
            ALOGD("+SET_SPEECH_VM_ENABLE(%d)", par2);
            AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
            GetNBSpeechParamFromNVRam(&eSphParamNB);
            if (par2 == 0) // normal VM
            {
                eSphParamNB.debug_info[0] = 0;
            }
            else // EPL
            {
                eSphParamNB.debug_info[0] = 3;
                if (eSphParamNB.speech_common_para[0] == 0) // if not assign EPL debug type yet, set a default one
                {
                    eSphParamNB.speech_common_para[0] = 6;
                }
            }
            SetNBSpeechParamToNVRam(&eSphParamNB);
            SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(&eSphParamNB);
            ALOGD("-SET_SPEECH_VM_ENABLE(%d)", par2);
            break;
        }
        case SET_DUMP_SPEECH_DEBUG_INFO:
        {
            ALOGD(" SET_DUMP_SPEECH_DEBUG_INFO(%d)", par2);
            mSpeechDriverFactory->GetSpeechDriver()->ModemDumpSpeechParam();
            break;
        }
        case SET_DUMP_AUDIO_DEBUG_INFO:
        {
            ALOGD(" SET_DUMP_AUDIO_DEBUG_INFO(%d)", par2);
            ::ioctl(mFd, AUDDRV_LOG_PRINT, 0);
            mAudioDigitalInstance->EnableSideToneHw(AudioDigitalType::O03 , false, true);
            sleep(3);
            mAudioDigitalInstance->EnableSideToneHw(AudioDigitalType::O03 , false, false);
            break;
        }
        case SET_DUMP_AUDIO_AEE_CHECK:
        {
            ALOGD(" SET_DUMP_AUDIO_AEE_CHECK(%d)", par2);
            if (par2 == 0)
            {
                property_set("streamout.aee.dump", "0");
            }
            else
            {
                property_set("streamout.aee.dump", "1");
            }
            break;
        }
        case SET_DUMP_AUDIO_STREAM_OUT:
        {
            ALOGD(" SET_DUMP_AUDIO_STREAM_OUT(%d)", par2);
            if (par2 == 0)
            {
                property_set("streamout.pcm.dump", "0");
                //::ioctl(mFd, AUDDRV_AEE_IOCTL, 0);
            }
            else
            {
                property_set("streamout.pcm.dump", "1");
                //::ioctl(mFd, AUDDRV_AEE_IOCTL, 1);
            }
            break;
        }
        case SET_DUMP_AUDIO_MIXER_BUF:
        {
            ALOGD(" SET_DUMP_AUDIO_MIXER_BUF(%d)", par2);
            if (par2 == 0)
            {
                property_set("af.mixer.pcm", "0");
            }
            else
            {
                property_set("af.mixer.pcm", "1");
            }
            break;
        }
        case SET_DUMP_AUDIO_TRACK_BUF:
        {
            ALOGD(" SET_DUMP_AUDIO_TRACK_BUF(%d)", par2);
            if (par2 == 0)
            {
                property_set("af.track.pcm", "0");
            }
            else
            {
                property_set("af.track.pcm", "1");
            }
            break;
        }
        case SET_DUMP_A2DP_STREAM_OUT:
        {
            ALOGD(" SET_DUMP_A2DP_STREAM_OUT(%d)", par2);
            if (par2 == 0)
            {
                property_set("a2dp.streamout.pcm", "0");
            }
            else
            {
                property_set("a2dp.streamout.pcm", "1");
            }
            break;
        }
        case SET_DUMP_AUDIO_STREAM_IN:
        {
            ALOGD(" SET_DUMP_AUDIO_STREAM_IN(%d)", par2);
            if (par2 == 0)
            {
                property_set("streamin.pcm.dump", "0");
            }
            else
            {
                property_set("streamin.pcm.dump", "1");
            }
            break;
        }
        case SET_DUMP_IDLE_VM_RECORD:
        {
            ALOGD(" SET_DUMP_IDLE_VM_RECORD(%d)", par2);
#if defined(MTK_AUDIO_HD_REC_SUPPORT)
            if (par2 == 0)
            {
                property_set("streamin.vm.dump", "0");
            }
            else
            {
                property_set("streamin.vm.dump", "1");
            }
#endif
            break;
        }
        case AUDIO_USER_TEST:
        {
            if (par2 == 0)
            {
                mAudioFtmInstance->EarphoneTest(true);
            }
            else if (par2 == 1)
            {
                mAudioFtmInstance->EarphoneTest(false);
            }
            else if (par2 == 2)
            {
                mAudioFtmInstance->FTMPMICLoopbackTest(true);
            }
            else if (par2 == 3)
            {
                mAudioFtmInstance->FTMPMICLoopbackTest(false);
            }
            else if (par2 == 4)
            {
                mAudioFtmInstance->LouderSPKTest(true, true);
            }
            else if (par2 == 5)
            {
                mAudioFtmInstance->LouderSPKTest(false, false);
            }
            else if (par2 == 6)
            {
                mAudioFtmInstance->RecieverTest(true);
            }
            else if (par2 == 7)
            {
                mAudioFtmInstance->RecieverTest(false);
            }
            else if (par2 == 8)
            {
                mAudioFtmInstance->FTMPMICEarpieceLoopbackTest(true);
            }
            else if (par2 == 9)
            {
                mAudioFtmInstance->FTMPMICEarpieceLoopbackTest(false);
            }
            else if (par2 == 0x10)
            {
                mAudioFtmInstance->FTMPMICDualModeLoopbackTest(true);
            }
            else if (par2 == 0x11)
            {
                mAudioFtmInstance->FTMPMICDualModeLoopbackTest(false);
            }
            else if (par2 == 0x12)
            {
                //mAudioFtmInstance->PhoneMic_EarphoneLR_Loopback(true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_MAIN_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_EARPHONE);
            }
            else if (par2 == 0x13)
            {
                //mAudioFtmInstance->PhoneMic_EarphoneLR_Loopback(false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x14)
            {
                mAudioFtmInstance->Pmic_I2s_out(true);
            }
            else if (par2 == 0x15)
            {
                mAudioFtmInstance->Pmic_I2s_out(false);
            }
            else if (par2 == 0x16)
            {
                mAudioFtmInstance->FMLoopbackTest(true);
            }
            else if (par2 == 0x17)
            {
                mAudioFtmInstance->FMLoopbackTest(false);
            }
            else if (par2 == 0x18)
            {
                //mAudioFtmInstance->PhoneMic_Receiver_Loopback(true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_MAIN_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_RECEIVER);
            }
            else if (par2 == 0x19)
            {
                //mAudioFtmInstance->PhoneMic_Receiver_Loopback(false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x20) // same as 0x12 ??
            {
                //mAudioFtmInstance->PhoneMic_EarphoneLR_Loopback(true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_MAIN_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_EARPHONE);
            }
            else if (par2 == 0x21) // same as 0x13 ??
            {
                //mAudioFtmInstance->PhoneMic_EarphoneLR_Loopback(false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x22)
            {
                //mAudioFtmInstance->PhoneMic_SpkLR_Loopback(true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_MAIN_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_SPEAKER);
            }
            else if (par2 == 0x23)
            {
                //mAudioFtmInstance->PhoneMic_SpkLR_Loopback(false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x24)
            {
                //mAudioFtmInstance->HeadsetMic_EarphoneLR_Loopback(true, true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_HEADSET_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_EARPHONE);
            }
            else if (par2 == 0x25)
            {
                //mAudioFtmInstance->HeadsetMic_EarphoneLR_Loopback(false, false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x26)
            {
                //mAudioFtmInstance->HeadsetMic_SpkLR_Loopback(true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_HEADSET_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_SPEAKER);
            }
            else if (par2 == 0x27)
            {
                //mAudioFtmInstance->HeadsetMic_SpkLR_Loopback(false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x28)
            {
                //mAudioFtmInstance->HeadsetMic_Receiver_Loopback(true, true);
                LoopbackManager::GetInstance()->SetLoopbackOn(AP_HEADSET_MIC_AFE_LOOPBACK, LOOPBACK_OUTPUT_RECEIVER);
            }
            else if (par2 == 0x29)
            {
                //mAudioFtmInstance->HeadsetMic_Receiver_Loopback(false, false);
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else if (par2 == 0x30)
            {
                mAudioResourceManager->StartInputDevice();
                mAudioResourceManager->StartOutputDevice();
                ALOGD("2 0x30 sleep");
                sleep(3);
            }
            else if (par2 == 0x31)
            {
                mAudioResourceManager->StopOutputDevice();
                mAudioResourceManager->StopInputDevice();
            }
            break;
        }
        case SET_DUMP_AP_SPEECH_EPL:
        {
            ALOGD(" SET_DUMP_AP_SPEECH_EPL(%d)", par2);
            if (par2 == 0)
            {
                property_set("streamin.epl.dump", "0");
            }
            else
            {
                property_set("streamin.epl.dump", "1");
            }
            break;
        }
        case SET_MagiASR_TEST_ENABLE:
        {
            ALOGD(" SET_MagiASR_TEST_ENABLE(%d)", par2);
            if (par2 == 0)
            {
                //disable MagiASR verify mode
                mAudioSpeechEnhanceInfoInstance->SetForceMagiASR(false);
            }
            else
            {
                //enable MagiASR verify mode
                mAudioSpeechEnhanceInfoInstance->SetForceMagiASR(true);
            }
            break;
        }
        case SET_AECREC_TEST_ENABLE:
        {
            ALOGD(" SET_AECREC_TEST_ENABLE(%d)", par2);
            if (par2 == 0)
            {
                //disable AECRec verify mode
                mAudioSpeechEnhanceInfoInstance->SetForceAECRec(false);
            }
            else
            {
                //enable AECRec verify mode
                mAudioSpeechEnhanceInfoInstance->SetForceAECRec(true);
            }
            break;
        }
        case AUDIO_FM_RADIO_TEST:
        {
            static int fd_FM = -1;
            ALOGD("FMR test command is (%d)", par2);
            if (par2 == 0)
            {
                AudioFMController::GetInstance()->SetFmEnable(false);
                if (fd_FM != -1)
                {
                    close(fd_FM);
                    fd_FM = -1;
                }
            }
            else if (par2 == 1 || par2 == 2)
            {
                struct fm_tune_parm fm_tune;//defined in fm.h
                fm_tune.err = 0;
                fm_tune.band = 0;
                fm_tune.space = 0;
                fm_tune.hilo = 0;
                if (par2 == 1)
                {
                    fm_tune.freq = 901;    // 90.1MHz
                }
                else
                {
                    fm_tune.freq = 1063;    // 106.3MHz
                }
                if (fd_FM == -1)
                {
                    fd_FM = open(FM_DEVICE_NAME, O_RDWR);
                    if (fd_FM < 0)
                    {
                        ALOGD("Open 'dev/fm' Fail !fd_FM(%d)", fd_FM);
                    }
                    else
                    {
                        ::ioctl(fd_FM, FM_IOCTL_POWERUP, &fm_tune);
                        AudioFMController::GetInstance()->SetFmEnable(true);
                    }
                }
                else
                {
                    ::ioctl(fd_FM, FM_IOCTL_TUNE, &fm_tune);
                }
            }

            break;
        }
        default:
            break;
    }
    return NO_ERROR;
}

status_t AudioMTKHardware::GetAudioCommonCommand(int parameters1)
{
    ALOGD("GetAudioCommonCommand parameters1 = %d ", parameters1);
    int result = 0 ;
    char value[PROPERTY_VALUE_MAX];
    switch (parameters1)
    {
        case GETOUTPUTFIRINDEX:
        {
            AUDIO_PARAM_MED_STRUCT pMedPara;
            GetMedParamFromNV(&pMedPara);
            result = pMedPara.select_FIR_output_index[Normal_Coef_Index];
            break;
        }
        case GETAUDIOCUSTOMDATASIZE:
        {
            int AudioCustomDataSize = sizeof(AUDIO_VOLUME_CUSTOM_STRUCT);
            ALOGD("GETAUDIOCUSTOMDATASIZE  AudioCustomDataSize = %d", AudioCustomDataSize);
            return AudioCustomDataSize;
        }
        case GETNORMALOUTPUTFIRINDEX:
        {
            AUDIO_PARAM_MED_STRUCT pMedPara;
            GetMedParamFromNV(&pMedPara);
            result = pMedPara.select_FIR_output_index[Normal_Coef_Index];
            break;
        }
        case GETHEADSETOUTPUTFIRINDEX:
        {
            AUDIO_PARAM_MED_STRUCT pMedPara;
            GetMedParamFromNV(&pMedPara);
            result = pMedPara.select_FIR_output_index[Headset_Coef_Index];
            break;
        }
        case GETSPEAKEROUTPUTFIRINDEX:
        {
            AUDIO_PARAM_MED_STRUCT pMedPara;
            GetMedParamFromNV(&pMedPara);
            result = pMedPara.select_FIR_output_index[Handfree_Coef_Index];
            break;
        }
        case GET_DUMP_AUDIO_AEE_CHECK:
        {
            property_get("streamout.aee.dump", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_AUDIO_STREAM_OUT=%d", result);
            break;
        }
        case GET_DUMP_AUDIO_STREAM_OUT:
        {
            property_get("streamout.pcm.dump", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_AUDIO_STREAM_OUT=%d", result);
            break;
        }
        case GET_DUMP_AUDIO_MIXER_BUF:
        {
            property_get("af.mixer.pcm", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_AUDIO_MIXER_BUF=%d", result);
            break;
        }
        case GET_DUMP_AUDIO_TRACK_BUF:
        {
            property_get("af.track.pcm", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_AUDIO_TRACK_BUF=%d", result);
            break;
        }
        case GET_DUMP_A2DP_STREAM_OUT:
        {
            property_get("a2dp.streamout.pcm", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_A2DP_STREAM_OUT=%d", result);
            break;
        }
        case GET_DUMP_AUDIO_STREAM_IN:
        {
            property_get("streamin.pcm.dump", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_AUDIO_STREAM_IN=%d", result);
            break;
        }
        case GET_DUMP_IDLE_VM_RECORD:
        {
#if defined(MTK_AUDIO_HD_REC_SUPPORT)
            property_get("streamin.vm.dump", value, "0");
            result = atoi(value);
#else
            result = 0;
#endif
            ALOGD(" GET_DUMP_IDLE_VM_RECORD=%d", result);
            break;
        }
        case GET_DUMP_AP_SPEECH_EPL:
        {
            property_get("streamin.epl.dump", value, "0");
            result = atoi(value);
            ALOGD(" GET_DUMP_AP_SPEECH_EPL=%d", result);
            break;
        }
        case GET_MagiASR_TEST_ENABLE:
        {
            //get the MagiASR verify mode status
            result = mAudioSpeechEnhanceInfoInstance->GetForceMagiASRState();
            ALOGD(" GET_MagiASR_TEST_ENABLE=%d", result);
            break;
        }
        case GET_AECREC_TEST_ENABLE:
        {
            //get the AECRec verify mode status
            result = 0;
            if (mAudioSpeechEnhanceInfoInstance->GetForceAECRecState())
            {
                result = 1;
            }
            ALOGD(" GET_AECREC_TEST_ENABLE=%d", result);
            break;
        }

#ifdef MTK_ACF_AUTO_GEN_SUPPORT
        case AUDIO_ACF_FO:
        {
            result = mAudioTuningInstance->getFOValue();
            ALOGD("getFOValue result is %d", result);
            break;
        }
#endif

        default:
        {
            ALOGD(" GetAudioCommonCommand: Unknown command\n");
            break;
        }
    }
    // call fucntion want to get status adn return it.
    return result;
}

status_t AudioMTKHardware::SetAudioCommonData(int par1, size_t len, void *ptr)
{
    ALOGD("%s(), par1 = 0x%x, len = %d", __FUNCTION__, par1, len);
    switch (par1)
    {
        case HOOK_BESLOUDNESS_CONTROL_CALLBACK:
        {
            mAudioMTKStreamManager->SetBesLoudnessControlCallback((BESLOUDNESS_CONTROL_CALLBACK_STRUCT *)ptr);
            break;
        }
        case UNHOOK_BESLOUDNESS_CONTROL_CALLBACK:
        {
            mAudioMTKStreamManager->SetBesLoudnessControlCallback(NULL);
            break;
        }
        case SETMEDDATA:
        {
            ASSERT(len == sizeof(AUDIO_PARAM_MED_STRUCT));
            SetMedParamToNV((AUDIO_PARAM_MED_STRUCT *)ptr);
            break;
        }
        case SETAUDIOCUSTOMDATA:
        {
            ASSERT(len == sizeof(AUDIO_VOLUME_CUSTOM_STRUCT));
            SetAudioCustomParamToNV((AUDIO_VOLUME_CUSTOM_STRUCT *)ptr);
            mAudioVolumeInstance->initVolumeController();
            setMasterVolume(mAudioVolumeInstance->getMasterVolume());
            break;
        }
#if defined(MTK_DUAL_MIC_SUPPORT)
        case SET_DUAL_MIC_PARAMETER:
        {
            ASSERT(len == sizeof(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT));
            SetDualMicSpeechParamToNVRam((AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *)ptr);
            mAudioVolumeInstance->initVolumeController();
            SpeechEnhancementController::GetInstance()->SetDualMicSpeechParametersToAllModem((AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *)ptr);
            break;
        }
#endif

#if defined(MTK_WB_SPEECH_SUPPORT)
        case SET_WB_SPEECH_PARAMETER:
        {
            ASSERT(len == sizeof(AUDIO_CUSTOM_WB_PARAM_STRUCT));
            SetWBSpeechParamToNVRam((AUDIO_CUSTOM_WB_PARAM_STRUCT *)ptr);
            SpeechEnhancementController::GetInstance()->SetWBSpeechParametersToAllModem((AUDIO_CUSTOM_WB_PARAM_STRUCT *)ptr);
            mAudioVolumeInstance->initVolumeController(); // for DRC2.0 need volume to get speech mode
            break;
        }
#endif
        case SET_AUDIO_VER1_DATA:
        {
            ASSERT(len == sizeof(AUDIO_VER1_CUSTOM_VOLUME_STRUCT));
            SetVolumeVer1ParamToNV((AUDIO_VER1_CUSTOM_VOLUME_STRUCT *)ptr);
            mAudioVolumeInstance->initVolumeController();
            setMasterVolume(mAudioVolumeInstance->getMasterVolume());
#ifndef MTK_BASIC_PACKAGE
            //Do it at AudioSystem, avoid deadlock, Hochi

//            const sp<IAudioPolicyService> &aps = AudioSystem::get_audio_policy_service();
//          aps->SetPolicyManagerParameters(POLICY_LOAD_VOLUME, 0, 0, 0);
#endif
            break;
        }

        // for Audio Taste Tuning
        case AUD_TASTE_TUNING:
        {
#if 1
            status_t ret = NO_ERROR;
            AudioTasteTuningStruct audioTasteTuningParam;
            memcpy((void *)&audioTasteTuningParam, ptr, sizeof(AudioTasteTuningStruct));

            switch (audioTasteTuningParam.cmd_type)
            {
                case AUD_TASTE_STOP:
                {

                    mAudioTuningInstance->enableModemPlaybackVIASPHPROC(false);
                    audioTasteTuningParam.wb_mode = mAudioTuningInstance->m_bWBMode;
                    mAudioTuningInstance->updataOutputFIRCoffes(&audioTasteTuningParam);

                    break;
                }
                case AUD_TASTE_START:
                {

                    mAudioTuningInstance->setMode(audioTasteTuningParam.phone_mode);
                    ret = mAudioTuningInstance->setPlaybackFileName(audioTasteTuningParam.input_file);
                    if (ret != NO_ERROR)
                    {
                        return ret;
                    }
                    ret = mAudioTuningInstance->setDLPGA((uint32) audioTasteTuningParam.dlPGA);
                    if (ret != NO_ERROR)
                    {
                        return ret;
                    }
                    mAudioTuningInstance->updataOutputFIRCoffes(&audioTasteTuningParam);
                    ret = mAudioTuningInstance->enableModemPlaybackVIASPHPROC(true, audioTasteTuningParam.wb_mode);
                    if (ret != NO_ERROR)
                    {
                        return ret;
                    }

                    break;
                }
                case AUD_TASTE_DLDG_SETTING:
                case AUD_TASTE_INDEX_SETTING:
                {
                    mAudioTuningInstance->updataOutputFIRCoffes(&audioTasteTuningParam);
                    break;
                }
                case AUD_TASTE_DLPGA_SETTING:
                {
                    mAudioTuningInstance->setMode(audioTasteTuningParam.phone_mode);
                    ret = mAudioTuningInstance->setDLPGA((uint32) audioTasteTuningParam.dlPGA);
                    if (ret != NO_ERROR)
                    {
                        return ret;
                    }

                    break;
                }
                default:
                    break;
            }
#endif
            break;
        }
        default:
            break;
    }
    return NO_ERROR;
}

status_t AudioMTKHardware::GetAudioCommonData(int par1, size_t len, void *ptr)
{
    ALOGD("%s par1=%d, len=%d", __FUNCTION__, par1, len);
    switch (par1)
    {
        case GETMEDDATA:
        {
            ASSERT(len == sizeof(AUDIO_PARAM_MED_STRUCT));
            GetMedParamFromNV((AUDIO_PARAM_MED_STRUCT *)ptr);
            break;
        }
        case GETAUDIOCUSTOMDATA:
        {
            ASSERT(len == sizeof(AUDIO_VOLUME_CUSTOM_STRUCT));
            GetAudioCustomParamFromNV((AUDIO_VOLUME_CUSTOM_STRUCT *)ptr);
            break;
        }
#if defined(MTK_DUAL_MIC_SUPPORT)
        case GET_DUAL_MIC_PARAMETER:
        {
            ASSERT(len == sizeof(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT));
            GetDualMicSpeechParamFromNVRam((AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *)ptr);
            break;
        }
#endif
#if defined(MTK_WB_SPEECH_SUPPORT)
        case GET_WB_SPEECH_PARAMETER:
        {
            ASSERT(len == sizeof(AUDIO_CUSTOM_WB_PARAM_STRUCT));
            GetWBSpeechParamFromNVRam((AUDIO_CUSTOM_WB_PARAM_STRUCT *) ptr);
            break;
        }
#endif
#if defined(MTK_AUDIO_GAIN_TABLE)
#if 0
        case GET_GAIN_TABLE_CTRPOINT_NUM:
        {
            int *p = (int *)ptr ;
            if (mAuioDevice != NULL)
            {
                *p = mAuioDevice->getParameters(AUD_AMP_GET_CTRP_NUM, 0, NULL);
            }
            break;
        }
        case GET_GAIN_TABLE_CTRPOINT_BITS:
        {
            int *point = (int *)ptr ;
            int *value = point + 1;
            if (mAuioDevice != NULL)
            {
                *value = mAuioDevice->getParameters(AUD_AMP_GET_CTRP_BITS, *point, NULL);
            }
            LOG_HARDWARE("GetAudioData GET_GAIN_TABLE_CTRPOINT_BITS point %d, value %d", *point, *value);
            break;
        }
        case GET_GAIN_TABLE_CTRPOINT_TABLE:
        {
            char *point = (char *)ptr ;
            int value = *point;
            if (mAuioDevice != NULL)
            {
                mAuioDevice->getParameters(AUD_AMP_GET_CTRP_TABLE, value, ptr);
            }
            break;
        }
#endif        
        case GET_TC1_DISP:
        {
            ASSERT(len == sizeof(PCDispTotolStru));
            if (NULL != mAudioTuningInstance)
            {
                mAudioTuningInstance->getGainInfoForDisp(ptr);
            }
            break;
        }        
#endif
        case GET_AUDIO_VER1_DATA:
        {
            GetVolumeVer1ParamFromNV((AUDIO_VER1_CUSTOM_VOLUME_STRUCT *) ptr);
            break;
        }
        case GET_AUDIO_POLICY_VOL_FROM_VER1_DATA:
        {
            AUDIO_CUSTOM_VOLUME_STRUCT* pTarget = (AUDIO_CUSTOM_VOLUME_STRUCT*) ptr;

            if ((pTarget->bRev == CUSTOM_VOLUME_REV_1) && (len == sizeof(AUDIO_CUSTOM_VOLUME_STRUCT)))
            {
                AUDIO_VER1_CUSTOM_VOLUME_STRUCT Source;
                GetVolumeVer1ParamFromNV(&Source);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_VOICE_CALL],(void*)Source.audiovolume_sph, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_SIP],(void*)Source.audiovolume_sip, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);

                memcpy((void*)Source.audiovolume_ring[VOLUME_NORMAL_MODE],(void*)Source.audiovolume_ring[VOLUME_HEADSET_SPEAKER_MODE],sizeof(unsigned char)*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)Source.audiovolume_ring[VOLUME_HEADSET_MODE],(void*)Source.audiovolume_ring[VOLUME_HEADSET_SPEAKER_MODE],sizeof(unsigned char)*AUDIO_MAX_VOLUME_STEP);

                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_RING],(void*)Source.audiovolume_ring, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_ALARM],(void*)Source.audiovolume_ring, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_NOTIFICATION],(void*)Source.audiovolume_ring, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);

                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_MUSIC],(void*)Source.audiovolume_media, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_ENFORCED_AUDIBLE],(void*)Source.audiovolume_media, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void *)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_TTS], (void *)audiovolume_tts_nonspeaker, sizeof(unsigned char)*NUM_OF_VOL_MODE * AUDIO_MAX_VOLUME_STEP);
                memcpy((void *)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_TTS][CUSTOM_VOLUME_SPEAKER_MODE], (void *)Source.audiovolume_media[VOLUME_SPEAKER_MODE], sizeof(unsigned char)* AUDIO_MAX_VOLUME_STEP);
                

                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_BLUETOOTH_SCO],(void*)Source.audiovolume_media, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_BOOT],(void*)Source.audiovolume_media, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_VIBSPK],(void*)Source.audiovolume_media, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);

                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_SYSTEM],(void*)audiovolume_system, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void*)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_DTMF],(void*)audiovolume_dtmf, sizeof(unsigned char)*NUM_OF_VOL_MODE*AUDIO_MAX_VOLUME_STEP);
                memcpy((void *)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_ACCESSIBILITY], (void *)Source.audiovolume_media, sizeof(unsigned char)*NUM_OF_VOL_MODE * AUDIO_MAX_VOLUME_STEP);
                memcpy((void *)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_REROUTING], (void *)audiovolume_rerouting, sizeof(unsigned char)*NUM_OF_VOL_MODE * AUDIO_MAX_VOLUME_STEP);
                memcpy((void *)pTarget->audiovolume_steamtype[CUSTOM_VOL_TYPE_PATCH], (void *)audiovolume_patch, sizeof(unsigned char)*NUM_OF_VOL_MODE * AUDIO_MAX_VOLUME_STEP);

                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_VOICE_CALL] = Source.audiovolume_level[VER1_VOL_TYPE_SPH];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_SYSTEM] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_RING] = Source.audiovolume_level[VER1_VOL_TYPE_RING];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_MUSIC] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_ALARM] = Source.audiovolume_level[VER1_VOL_TYPE_RING];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_NOTIFICATION] = Source.audiovolume_level[VER1_VOL_TYPE_RING];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_BLUETOOTH_SCO] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_ENFORCED_AUDIBLE] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_DTMF] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_TTS] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_BOOT] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_VIBSPK] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_SIP] = Source.audiovolume_level[VER1_VOL_TYPE_SIP];
                pTarget->audiovolume_level[CUSTOM_VOL_TYPE_ACCESSIBILITY] = pTarget->audiovolume_level[CUSTOM_VOL_TYPE_REROUTING] = pTarget->audiovolume_level[CUSTOM_VOL_TYPE_PATCH] = Source.audiovolume_level[VER1_VOL_TYPE_MEDIA];
                pTarget->bReady = 1;
                mUseTuningVolume = true;
                memcpy((void*)&VolCache,(void*)pTarget,sizeof(AUDIO_CUSTOM_VOLUME_STRUCT));
                ALOGD("Get PolicyCustomVolume");
            }
            else
            {
                ALOGD("pTarget->bRev %d/%d len %d/%d",pTarget->bRev,CUSTOM_VOLUME_REV_1,len,sizeof(AUDIO_CUSTOM_VOLUME_STRUCT));
                ALOGD("Get PolicyCustomVolume Fail");
            }
            break;
        }
        case GET_VOICE_CUST_PARAM:
        {
            GetVoiceRecogCustParamFromNV((VOICE_RECOGNITION_PARAM_STRUCT *)ptr);
            break;
        }
        case GET_VOICE_FIR_COEF:
        {
            AUDIO_HD_RECORD_PARAM_STRUCT custHDRECParam;
            GetHdRecordParamFromNV(&custHDRECParam);
            ASSERT(len == sizeof(custHDRECParam.hd_rec_fir));
            memcpy(ptr, (void *)custHDRECParam.hd_rec_fir, len);
            break;
        }
        case GET_VOICE_GAIN:
        {
            AUDIO_VER1_CUSTOM_VOLUME_STRUCT custGainParam;
            GetVolumeVer1ParamFromNV(&custGainParam);
            uint16_t *pGain = (uint16_t *)ptr;
            *pGain = mAudioVolumeInstance->MappingToDigitalGain(custGainParam.audiovolume_mic[VOLUME_NORMAL_MODE][7]);
            *(pGain + 1) = mAudioVolumeInstance->MappingToDigitalGain(custGainParam.audiovolume_mic[VOLUME_HEADSET_MODE][7]);
            break;
        }
        default:
            break;
    }
    return NO_ERROR;
}
status_t AudioMTKHardware::setCommonParameters(const String8 &keyValuePairs)
{
    status_t status = NO_ERROR;
    int value = 0;
    String8 value_str;
    float value_float = 0.0;
    AudioParameter param = AudioParameter(keyValuePairs);
    ALOGD("+setCommonParameters(): %s ", keyValuePairs.string());

    do
    {
        if (param.getInt(keyVolumeStreamType, value) == NO_ERROR)
        {
            int device = 0;
            int index = 0;
            if (param.getInt(keyVolumeDevice, device) == NO_ERROR)
            {
                if (param.getInt(keyVolumeIndex, index) == NO_ERROR)
                {
                    //mSetModeLock.lock();
                    mAudioVolumeInstance->setAnalogVolume(value, device, index, mMode);
                    // mSetModeLock.unlock();
                }
            }
            param.remove(keyVolumeStreamType);
            param.remove(keyVolumeDevice);
            param.remove(keyVolumeIndex);
            //Do nothing for this command .
            break;
        }
        // VT call (true) / Voice call (false)
        if (param.getInt(keySetVTSpeechCall, value) == NO_ERROR)
        {
            param.remove(keySetVTSpeechCall);
            SpeechPhoneCallController::GetInstance()->SetVtNeedOn((bool)value);
            break;
        }
        // Set BGS DL Mute (true) / Unmute(false)
        if (param.getInt(keySet_BGS_DL_Mute, value) == NO_ERROR)
        {
            param.remove(keySet_BGS_DL_Mute);
            ALOGD("%s(), %s", __FUNCTION__, keyValuePairs.string());
            mAudioMTKStreamManager->setParametersToStreamOut(keyValuePairs);
            break;
        }

        // Set BGS UL Mute (true) / Unmute(false)
        if (param.getInt(keySet_BGS_UL_Mute, value) == NO_ERROR)
        {
            param.remove(keySet_BGS_UL_Mute);
            ALOGD("%s(), %s", __FUNCTION__, keyValuePairs.string());
            mAudioMTKStreamManager->setParametersToStreamOut(keyValuePairs);
            break;
        }

        // Set SpeechCall DL Mute (true) / Unmute(false)
        if (param.getInt(keySet_SpeechCall_DL_Mute, value) == NO_ERROR)
        {
            param.remove(keySet_SpeechCall_DL_Mute);
            ALOGD("%s(), %s", __FUNCTION__, keyValuePairs.string());
            SpeechPhoneCallController::GetInstance()->SetDlMute((bool)value);
            break;
        }

        // Set SpeechCall UL Mute (true) / Unmute(false)
        if (param.getInt(keySet_SpeechCall_UL_Mute, value) == NO_ERROR)
        {
            param.remove(keySet_SpeechCall_UL_Mute);
            ALOGD("%s(), %s", __FUNCTION__, keyValuePairs.string());
            SpeechPhoneCallController::GetInstance()->SetUlMute((bool)value);
            break;
        }

#ifdef BTNREC_DECIDED_BY_DEVICE
        // BT NREC on/off
        if (param.get(keyBtHeadsetNrec, value_str) == NO_ERROR)
        {
            param.remove(keyBtHeadsetNrec);
            if (value_str == "on")
            {
                SpeechEnhancementController::GetInstance()->SetBtHeadsetNrecOnToAllModem(true);
            }
            else if (value_str == "off")
            {
                SpeechEnhancementController::GetInstance()->SetBtHeadsetNrecOnToAllModem(false);
            }
            break;
        }
#endif

        //BT VGS feature +
        if (param.get(keyBtSupportVolume, value_str) == NO_ERROR)
        {
            param.remove(keyBtSupportVolume);
            if (value_str == "on")
            {
                mAudioVolumeInstance->setBtVolumeCapability(true);
            }
            else if (value_str == "off")
            {
                mAudioVolumeInstance->setBtVolumeCapability(false);
            }
            break;
        }
        //-
        //Analog FM enable
        if (param.getInt(keyAnalogFmEnable, value) == NO_ERROR)
        {
            param.remove(keyAnalogFmEnable);
            WARNING("Not Support FM Analog Line In Path Anymore. Please Use Merge_I2S / 2nd_I2S_In");
            break;
        }
        //Digital FM enable
        if (param.getInt(keyDigitalFmEnable, value) == NO_ERROR)
        {
            param.remove(keyDigitalFmEnable);
            if (mUseAudioPatchForFm == false)
            {
                AudioFMController::GetInstance()->SetFmEnable((bool)value);
            }
            break;
        }
        //Set FM volume
        if (param.getFloat(keySetFmVolume, value_float) == NO_ERROR)
        {
            param.remove(keySetFmVolume);
            if (mUseAudioPatchForFm == false)
            {
                AudioFMController::GetInstance()->SetFmVolume(value_float);
            }
            break;
        }
        //Force FM to loudspeaker
        if (param.getInt(keySetFmForceToSpk, value) == NO_ERROR)
        {
            param.remove(keySetFmForceToSpk);
            WARNING("Do nothing for this command: AudioSetForceToSpeaker");
            break;
        }
        //Analog mATV Enable
        if (param.getInt(keyMatvAnalogEnable, value) == NO_ERROR)
        {
            param.remove(keyMatvAnalogEnable);
#if defined(MATV_AUDIO_SUPPORT)
            AudioMATVController::GetInstance()->SetMatvEnable((bool)value, MATV_ANALOG);
#else
            WARNING("Do nothing for this command: AtvAudioLineInEnable");
#endif
            break;
        }
        //Digital mATV Enable
        if (param.getInt(keyMatvDigitalEnable, value) == NO_ERROR)
        {
            param.remove(keyMatvDigitalEnable);
#if defined(MATV_AUDIO_SUPPORT)
            AudioMATVController::GetInstance()->SetMatvEnable((bool)value, MATV_DIGITAL);
#else
            WARNING("Do nothing for this command: AudioSetMatvDigitalEnable");
#endif
            break;
        }
        //Set mATV Volume
        if (param.getInt(keySetMatvVolume, value) == NO_ERROR)
        {
            param.remove(keySetMatvVolume);
            WARNING("Do nothing for this command: AtvAudioLineInEnable");
            break;
        }
        //mute mATV
        if (param.getInt(keySetMatvMute, value) == NO_ERROR)
        {
            param.remove(keySetMatvMute);
            WARNING("Do nothing for this command: AtvAudioLineInEnable");
            break;
        }

        //MusicPlus enable
        if (param.getInt(keyMusicPlusSet, value) == NO_ERROR)
        {
#ifndef HIFI_SWITCH_BY_AUDENH  //HP switch use AudEnh setting
            mAudioMTKStreamManager->SetMusicPlusStatus(value ? true : false);
#else
            bool prev_HiFiDACStatus, cur_HiFiDACStatus;
            prev_HiFiDACStatus = mAudioMTKStreamManager->GetHiFiDACStatus();
            cur_HiFiDACStatus = (value ? true : false);
            ALOGD("prev_HiFiDACStatus=%d, cur_HiFiDACStatus=%d", prev_HiFiDACStatus, cur_HiFiDACStatus);

            // update HiFiDACStatus
            mAudioMTKStreamManager->SetHiFiDACStatus(value ? true : false);

            if ((prev_HiFiDACStatus == true && cur_HiFiDACStatus == false) ||
                (prev_HiFiDACStatus == false && cur_HiFiDACStatus == true))
            {
                if (mAudioMTKStreamManager->IsOutPutStreamActive() == true ||
                    AudioFMController::GetInstance()->GetFmEnable() == true ||
                    AudioMATVController::GetInstance()->GetMatvEnable() == true)
                {
                    //Close original DAC path
                    mAudioResourceManager->StopOutputDevice();

                    //usleep(2000000); //experiment: let circuit to release voltage

                    //Open new DAC path
                    mAudioResourceManager->StartOutputDevice();
                }
            }
#endif
            param.remove(keyMusicPlusSet);
            break;
        }
        if (param.getInt(keyBesLoudnessSet, value) == NO_ERROR)
        {
            mAudioMTKStreamManager->SetBesLoudnessStatus(value ? true : false);
            param.remove(keyBesLoudnessSet);
            break;
        }

        //HiFiDAC enable
        if (param.getInt(keyHiFiDACSet, value) == NO_ERROR)
        {
            bool prev_HiFiDACStatus, cur_HiFiDACStatus;
            prev_HiFiDACStatus = mAudioMTKStreamManager->GetHiFiDACStatus();
            cur_HiFiDACStatus = (value ? true : false);
            ALOGD("prev_HiFiDACStatus=%d, cur_HiFiDACStatus=%d", prev_HiFiDACStatus, cur_HiFiDACStatus);

            if ((prev_HiFiDACStatus == true && cur_HiFiDACStatus == false) ||
                (prev_HiFiDACStatus == false && cur_HiFiDACStatus == true))
            {
                if (mAudioMTKStreamManager->IsOutPutStreamActive() == true ||
                    AudioFMController::GetInstance()->GetFmEnable() == true ||
                    AudioMATVController::GetInstance()->GetMatvEnable() == true)
                {
                    //Close original DAC path
                    mAudioResourceManager->StopOutputDevice();

                    // update HiFiDACStatus
                    mAudioMTKStreamManager->SetHiFiDACStatus(value ? true : false);

                    //Open new DAC path
                    mAudioResourceManager->StartOutputDevice();
                }
            }

            param.remove(keyHiFiDACSet);
            break;
        }

        if (param.getInt(keyLR_ChannelSwitch, value) == NO_ERROR)
        {
#ifdef MTK_DUAL_MIC_SUPPORT
            ALOGD("keyLR_ChannelSwitch=%d", value);
            bool bIsLRSwitch = value;
            mAudioSpeechEnhanceInfoInstance->SetRecordLRChannelSwitch(bIsLRSwitch);
#else
            ALOGD("only support in dual MIC");
#endif
            param.remove(keyLR_ChannelSwitch);
            //goto EXIT_SETPARAMETERS;
            //Because parameters will send two strings, we need to parse another.(HD Record info and Channel Switch info)
        }

        if (param.getInt(keyForceUseSpecificMicData, value) == NO_ERROR)
        {
#ifdef MTK_DUAL_MIC_SUPPORT
            ALOGD("keyForceUseSpecificMicData=%d", value);
            int32 UseSpecificMic = value;
            mAudioSpeechEnhanceInfoInstance->SetUseSpecificMIC(UseSpecificMic);
#else
            ALOGD("only support in dual MIC");
#endif
            param.remove(keyForceUseSpecificMicData);
            break;
        }

#ifdef MTK_AUDIO_HD_REC_SUPPORT
        if (param.getInt(keyHDREC_SET_VOICE_MODE, value) == NO_ERROR)
        {
            ALOGD("HDREC_SET_VOICE_MODE=%d", value); // Normal, Indoor, Outdoor,
            param.remove(keyHDREC_SET_VOICE_MODE);
            //Get and Check Voice/Video Mode Offset
            AUDIO_HD_RECORD_SCENE_TABLE_STRUCT hdRecordSceneTable;
            GetHdRecordSceneTableFromNV(&hdRecordSceneTable);
            if (value < hdRecordSceneTable.num_voice_rec_scenes)
            {
                int32 HDRecScene = value + 1;//1:cts verifier offset
                mAudioSpeechEnhanceInfoInstance->SetHDRecScene(HDRecScene);
            }
            else
            {
                ALOGE("HDREC_SET_VOICE_MODE=%d exceed max value(%d)\n", value, hdRecordSceneTable.num_voice_rec_scenes);
            }
            break;
        }

        if (param.getInt(keyHDREC_SET_VIDEO_MODE, value) == NO_ERROR)
        {
            ALOGD("HDREC_SET_VIDEO_MODE=%d", value); // Normal, Indoor, Outdoor,
            param.remove(keyHDREC_SET_VIDEO_MODE);
            //Get and Check Voice/Video Mode Offset
            AUDIO_HD_RECORD_SCENE_TABLE_STRUCT hdRecordSceneTable;
            GetHdRecordSceneTableFromNV(&hdRecordSceneTable);
            if (value < hdRecordSceneTable.num_video_rec_scenes)
            {
                uint32 offset = hdRecordSceneTable.num_voice_rec_scenes + 1;//1:cts verifier offset
                int32 HDRecScene = value + offset;
                mAudioSpeechEnhanceInfoInstance->SetHDRecScene(HDRecScene);
            }
            else
            {
                ALOGE("HDREC_SET_VIDEO_MODE=%d exceed max value(%d)\n", value, hdRecordSceneTable.num_video_rec_scenes);
            }
            break;
        }
#endif
        //<---for audio tool(speech/ACF/HCF/DMNR/HD/Audiotaste calibration)
        // calibrate speech parameters
        if (param.getInt(keySpeechParams_Update, value) == NO_ERROR)
        {
            ALOGD("setParameters Update Speech Parames");
//speech_band: 0:Narrow Band, 1: Wide Band, 2: Super Wideband, ..., 8: All 
            if (value == 0)//Narrow Band
            {
                AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
                GetNBSpeechParamFromNVRam(&eSphParamNB);
                SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(&eSphParamNB);
            }
#if defined(MTK_WB_SPEECH_SUPPORT)
            else if (value == 1)//Wide Band
            {
                AUDIO_CUSTOM_WB_PARAM_STRUCT eSphParamWB;
                GetWBSpeechParamFromNVRam(&eSphParamWB);
                SpeechEnhancementController::GetInstance()->SetWBSpeechParametersToAllModem(&eSphParamWB);
            }
#endif
            else if (value == 8)//set all mode parameters
            {
                AUDIO_CUSTOM_PARAM_STRUCT eSphParamNB;
                GetNBSpeechParamFromNVRam(&eSphParamNB);
                SpeechEnhancementController::GetInstance()->SetNBSpeechParametersToAllModem(&eSphParamNB);
#if defined(MTK_WB_SPEECH_SUPPORT)
                AUDIO_CUSTOM_WB_PARAM_STRUCT eSphParamWB;
                GetWBSpeechParamFromNVRam(&eSphParamWB);
                SpeechEnhancementController::GetInstance()->SetWBSpeechParametersToAllModem(&eSphParamWB);
#endif
            }

            if (ModeInCall(mMode) == true) // get output device for in_call, and set speech mode
            {
                const audio_devices_t output_device = (audio_devices_t)mAudioResourceManager->getDlOutputDevice();
                const audio_devices_t input_device  = (audio_devices_t)mAudioResourceManager->getUlInputDevice();
                mSpeechDriverFactory->GetSpeechDriver()->SetSpeechMode(input_device, output_device);
            }
            param.remove(keySpeechParams_Update);
            break;
        }
#if defined(MTK_DUAL_MIC_SUPPORT)
        if (param.getInt(keyDualMicParams_Update, value) == NO_ERROR)
        {
            param.remove(keyDualMicParams_Update);
            AUDIO_CUSTOM_EXTRA_PARAM_STRUCT eSphParamDualMic;
            GetDualMicSpeechParamFromNVRam(&eSphParamDualMic);
            SpeechEnhancementController::GetInstance()->SetDualMicSpeechParametersToAllModem(&eSphParamDualMic);

            if (ModeInCall(mMode) == true) // get output device for in_call, and set speech mode
            {
                const audio_devices_t output_device = (audio_devices_t)mAudioResourceManager->getDlOutputDevice();
                const audio_devices_t input_device  = (audio_devices_t)mAudioResourceManager->getUlInputDevice();
                mSpeechDriverFactory->GetSpeechDriver()->SetSpeechMode(input_device, output_device);
            }
            break;
        }
#endif
        // calibrate speech volume
        if (param.getInt(keySpeechVolume_Update, value) == NO_ERROR)
        {
            ALOGD("setParameters Update Speech volume");
            mAudioVolumeInstance->initVolumeController();
            if (ModeInCall(mMode) == true)
            {
                int32_t outputDevice = mAudioResourceManager->getDlOutputDevice();
                SpeechPhoneCallController *pSpeechPhoneCallController = SpeechPhoneCallController::GetInstance();
#ifndef MTK_AUDIO_GAIN_TABLE
                mAudioVolumeInstance->setVoiceVolume(mAudioVolumeInstance->getVoiceVolume(), mMode, (uint32)outputDevice);
#endif
                switch (outputDevice)
                {
                    case AUDIO_DEVICE_OUT_WIRED_HEADSET :
                    {
#ifdef  MTK_TTY_SUPPORT
                        if (pSpeechPhoneCallController->GetTtyCtmMode() == AUD_TTY_VCO)
                        {
                            mAudioVolumeInstance->ApplyMicGain(Normal_Mic, mMode);
                        }
                        else if (pSpeechPhoneCallController->GetTtyCtmMode() == AUD_TTY_HCO || pSpeechPhoneCallController->GetTtyCtmMode() == AUD_TTY_FULL)
                        {
                            mAudioVolumeInstance->ApplyMicGain(TTY_CTM_Mic, mMode);
                        }
                        else
                        {
                            mAudioVolumeInstance->ApplyMicGain(Headset_Mic, mMode);
                        }
#else
                        mAudioVolumeInstance->ApplyMicGain(Headset_Mic, mMode);
#endif
                        break;
                    }
                    case AUDIO_DEVICE_OUT_WIRED_HEADPHONE :
                    {
#ifdef  MTK_TTY_SUPPORT
                        if (pSpeechPhoneCallController->GetTtyCtmMode() == AUD_TTY_VCO)
                        {
                            mAudioVolumeInstance->ApplyMicGain(Normal_Mic, mMode);
                        }
                        else if (pSpeechPhoneCallController->GetTtyCtmMode() == AUD_TTY_HCO || pSpeechPhoneCallController->GetTtyCtmMode() == AUD_TTY_FULL)
                        {
                            mAudioVolumeInstance->ApplyMicGain(TTY_CTM_Mic, mMode);
                        }
                        else
                        {
                            mAudioVolumeInstance->ApplyMicGain(Handfree_Mic, mMode);
                        }
#else
                        mAudioVolumeInstance->ApplyMicGain(Handfree_Mic, mMode);
#endif
                        break;
                    }
                    case AUDIO_DEVICE_OUT_SPEAKER:
                    {
                        mAudioVolumeInstance->ApplyMicGain(Handfree_Mic, mMode);
                        break;
                    }
                    case AUDIO_DEVICE_OUT_EARPIECE:
                    {
                        mAudioVolumeInstance->ApplyMicGain(Normal_Mic, mMode);
                        break;
                    }
                    default:
                    {
                        break;
                    }
                }
            }
            else
            {
                setMasterVolume(mAudioVolumeInstance->getMasterVolume());
            }
            param.remove(keySpeechVolume_Update);
            break;
        }
        if (param.getInt(keyGainTable_Update, value) == NO_ERROR)
        {
            ALOGD("setParameters Update Gain Table");
            mAudioVolumeInstance->initVolumeController();
            param.remove(keyGainTable_Update);
            break;
        }
        if (param.getInt(keyMicGain_Update, value) == NO_ERROR)
        {
            ALOGD("setParameters Update Mic Gain");
            mAudioResourceManager->SetInputDeviceGain();
            param.remove(keyMicGain_Update);
            break;
        }
        if (param.getInt(keyHFPParam_Update, value) == NO_ERROR)
        {
            ALOGD("setParameters keyHFPParam_Update");
            param.remove(keyHFPParam_Update);
            break;
        }
        if (param.getInt(keyhfp_volume, value) == NO_ERROR)
        {
            ALOGD("setParameters keyhfp_volume");
            param.remove(keyhfp_volume);
            break;
        }
        // ACF/HCF parameters calibration
        if (param.getInt(keyACFHCF_Update, value) == NO_ERROR)
        {
            mAudioMTKStreamManager->UpdateACFHCF(value);
            param.remove(keyACFHCF_Update);
            break;
        }
        // HD recording and DMNR calibration
#if defined(MTK_DUAL_MIC_SUPPORT) || defined(MTK_AUDIO_HD_REC_SUPPORT)
        if (param.getInt(keyDualMicRecPly, value) == NO_ERROR)
        {
            unsigned short cmdType = value & 0x000F;
            bool bWB = (value >> 4) & 0x000F;
            status_t ret = NO_ERROR;
            switch (cmdType)
            {
#ifdef DMNR_TUNNING_AT_MODEMSIDE
                case DUAL_MIC_REC_PLAY_STOP:
                    ret = mAudioTuningInstance->enableDMNRModem2Way(false, bWB, P2W_RECEIVER_OUT, P2W_NORMAL);
                    break;
                case DUAL_MIC_REC:
                    ret = mAudioTuningInstance->enableDMNRModem2Way(true, bWB, P2W_RECEIVER_OUT, P2W_RECONLY);
                    break;
                case DUAL_MIC_REC_PLAY:
                    ret = mAudioTuningInstance->enableDMNRModem2Way(true, bWB, P2W_RECEIVER_OUT, P2W_NORMAL);
                    break;
                case DUAL_MIC_REC_PLAY_HS:
                    ret = mAudioTuningInstance->enableDMNRModem2Way(true, bWB, P2W_HEADSET_OUT, P2W_NORMAL);
                    break;
#else//dmnr tunning at ap side
                case DUAL_MIC_REC_PLAY_STOP:
                    ret = mAudioTuningInstance->enableDMNRAtApSide(false, bWB, OUTPUT_DEVICE_RECEIVER, RECPLAY_MODE);
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(false);
                    break;
                case DUAL_MIC_REC:
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(true);
                    ret = mAudioTuningInstance->enableDMNRAtApSide(true, bWB, OUTPUT_DEVICE_RECEIVER, RECONLY_MODE);
                    break;
                case DUAL_MIC_REC_PLAY:
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(true);
                    ret = mAudioTuningInstance->enableDMNRAtApSide(true, bWB, OUTPUT_DEVICE_RECEIVER, RECPLAY_MODE);
                    break;
                case DUAL_MIC_REC_PLAY_HS:
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(true);
                    ret = mAudioTuningInstance->enableDMNRAtApSide(true, bWB, OUTPUT_DEVICE_HEADSET, RECPLAY_MODE);
                    break;
                case DUAL_MIC_REC_HF:
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(true);
                    ret = mAudioTuningInstance->enableDMNRAtApSide(true, bWB, OUTPUT_DEVICE_RECEIVER, RECONLY_HF_MODE);
                    break;
                case DUAL_MIC_REC_PLAY_HF:
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(true);
                    ret = mAudioTuningInstance->enableDMNRAtApSide(true, bWB, OUTPUT_DEVICE_RECEIVER, RECPLAY_HF_MODE);
                    break;
                case DUAL_MIC_REC_PLAY_HF_HS:
                    mAudioSpeechEnhanceInfoInstance->SetAPDMNRTuningEnable(true);
                    ret = mAudioTuningInstance->enableDMNRAtApSide(true, bWB, OUTPUT_DEVICE_HEADSET, RECPLAY_HF_MODE);
                    break;
#endif
                default:
                    ret = BAD_VALUE;
                    break;
            }
            if (ret == NO_ERROR)
            {
                param.remove(keyDualMicRecPly);
            }
            break;
        }

        if (param.get(keyDUALMIC_IN_FILE_NAME, value_str) == NO_ERROR)
        {
            if (mAudioTuningInstance->setPlaybackFileName(value_str.string()) == NO_ERROR)
            {
                param.remove(keyDUALMIC_IN_FILE_NAME);
            }
            break;
        }

        if (param.get(keyDUALMIC_OUT_FILE_NAME, value_str) == NO_ERROR)
        {
            if (mAudioTuningInstance->setRecordFileName(value_str.string()) == NO_ERROR)
            {
#ifndef DMNR_TUNNING_AT_MODEMSIDE
                if (mAudioSpeechEnhanceInfoInstance->SetHDRecVMFileName(value_str.string()) == NO_ERROR)
#endif
                    param.remove(keyDUALMIC_OUT_FILE_NAME);
            }
            break;
        }

        if (param.getInt(keyDUALMIC_SET_UL_GAIN, value) == NO_ERROR)
        {
            if (mAudioTuningInstance->setDMNRGain(AUD_MIC_GAIN, value) == NO_ERROR)
            {
                param.remove(keyDUALMIC_SET_UL_GAIN);
            }
            break;
        }

        if (param.getInt(keyDUALMIC_SET_DL_GAIN, value) == NO_ERROR)
        {
            if (mAudioTuningInstance->setDMNRGain(AUD_RECEIVER_GAIN, value) == NO_ERROR)
            {
                param.remove(keyDUALMIC_SET_DL_GAIN);
            }
            break;
        }

        if (param.getInt(keyDUALMIC_SET_HSDL_GAIN, value) == NO_ERROR)
        {
            if (mAudioTuningInstance->setDMNRGain(AUD_HS_GAIN, value) == NO_ERROR)
            {
                param.remove(keyDUALMIC_SET_HSDL_GAIN);
            }
            break;
        }

        if (param.getInt(keyDUALMIC_SET_UL_GAIN_HF, value) == NO_ERROR)
        {
            if (mAudioTuningInstance->setDMNRGain(AUD_MIC_GAIN_HF, value) == NO_ERROR)
            {
                param.remove(keyDUALMIC_SET_UL_GAIN_HF);
            }
            break;
        }
#endif

        if (param.getInt(keyHDRecTunningEnable, value) == NO_ERROR)
        {
            ALOGD("keyHDRecTunningEnable=%d", value);
            bool bEnable = value;
            mAudioSpeechEnhanceInfoInstance->SetHDRecTunningEnable(bEnable);
            param.remove(keyHDRecTunningEnable);
            break;
        }

        if (param.get(keyHDRecVMFileName, value_str) == NO_ERROR)
        {
            ALOGD("keyHDRecVMFileName=%s", value_str.string());
            if (mAudioSpeechEnhanceInfoInstance->SetHDRecVMFileName(value_str.string()) == NO_ERROR)
            {
                param.remove(keyHDRecVMFileName);
            }
            break;
        }

        // --->for audio tool(speech/ACF/HCF/DMNR/HD/Audiotaste calibration)

#if defined(MTK_DUAL_MIC_SUPPORT)
        // Dual Mic Noise Reduction, DMNR for Receiver
        if (param.getInt(keyEnable_Dual_Mic_Setting, value) == NO_ERROR)
        {
            param.remove(keyEnable_Dual_Mic_Setting);
            SpeechEnhancementController::GetInstance()->SetDynamicMaskOnToAllModem(SPH_ENH_DYNAMIC_MASK_DMNR, (bool)value);
            break;
        }

        // Dual Mic Noise Reduction, DMNR for Loud Speaker
        if (param.getInt(keySET_LSPK_DMNR_ENABLE, value) == NO_ERROR)
        {
            param.remove(keySET_LSPK_DMNR_ENABLE);
            SpeechEnhancementController::GetInstance()->SetDynamicMaskOnToAllModem(SPH_ENH_DYNAMIC_MASK_LSPK_DMNR, (bool)value);

            if (SpeechEnhancementController::GetInstance()->GetMagicConferenceCallOn() == true &&
                SpeechEnhancementController::GetInstance()->GetDynamicMask(SPH_ENH_DYNAMIC_MASK_LSPK_DMNR) == true)
            {
                ALOGE("Cannot open MagicConCall & LoudSpeaker DMNR at the same time!!");
            }

            break;
        }

        // VoIP Dual Mic Noise Reduction, DMNR for Receiver
        if (param.getInt(keySET_VOIP_RECEIVER_DMNR_ENABLE, value) == NO_ERROR)
        {
            param.remove(keySET_VOIP_RECEIVER_DMNR_ENABLE);
            mAudioSpeechEnhanceInfoInstance->SetDynamicVoIPSpeechEnhancementMask(VOIP_SPH_ENH_DYNAMIC_MASK_DMNR, (bool)value);
            break;
        }

        // VoIP Dual Mic Noise Reduction, DMNR for Loud Speaker
        if (param.getInt(keySET_VOIP_LSPK_DMNR_ENABLE, value) == NO_ERROR)
        {
            param.remove(keySET_VOIP_LSPK_DMNR_ENABLE);
            mAudioSpeechEnhanceInfoInstance->SetDynamicVoIPSpeechEnhancementMask(VOIP_SPH_ENH_DYNAMIC_MASK_LSPK_DMNR, (bool)value);
            break;
        }

#endif

        // Voice Clarity Engine, VCE
        if (param.getInt(keySET_VCE_ENABLE, value) == NO_ERROR)
        {
            param.remove(keySET_VCE_ENABLE);
            SpeechEnhancementController::GetInstance()->SetDynamicMaskOnToAllModem(SPH_ENH_DYNAMIC_MASK_VCE, (bool)value);
            break;
        }

        // Magic Conference Call
        if (param.getInt(keySET_MAGIC_CON_CALL_ENABLE, value) == NO_ERROR)
        {
            param.remove(keySET_MAGIC_CON_CALL_ENABLE);

            const bool magic_conference_call_on = (bool)value;

            // enable/disable flag
            SpeechEnhancementController::GetInstance()->SetMagicConferenceCallOn(magic_conference_call_on);

            // apply speech mode
            if (ModeInCall(mMode) == true)
            {
                const audio_devices_t output_device = (audio_devices_t)mAudioResourceManager->getDlOutputDevice();
                const audio_devices_t input_device  = (audio_devices_t)mAudioResourceManager->getUlInputDevice();
                if (output_device == AUDIO_DEVICE_OUT_SPEAKER)
                {
                    mSpeechDriverFactory->GetSpeechDriver()->SetSpeechMode(input_device, output_device);
                }
            }

            if (SpeechEnhancementController::GetInstance()->GetMagicConferenceCallOn() == true &&
                SpeechEnhancementController::GetInstance()->GetDynamicMask(SPH_ENH_DYNAMIC_MASK_LSPK_DMNR) == true)
            {
                ALOGE("Cannot open MagicConCall & LoudSpeaker DMNR at the same time!!");
            }

            break;
        }


        // Loopback use speaker or not
        static bool bForceUseLoudSpeakerInsteadOfReceiver = false;
        if (param.getInt(keySET_LOOPBACK_USE_LOUD_SPEAKER, value) == NO_ERROR)
        {
            param.remove(keySET_LOOPBACK_USE_LOUD_SPEAKER);
            bForceUseLoudSpeakerInsteadOfReceiver = value & 0x1;
            break;
        }

        // Assign delay frame for modem loopback // 1 frame = 20ms
        if (param.getInt(keySET_LOOPBACK_MODEM_DELAY_FRAMES, value) == NO_ERROR)
        {
            param.remove(keySET_LOOPBACK_MODEM_DELAY_FRAMES);
            SpeechDriverInterface *pSpeechDriver = NULL;
            for (int modem_index = MODEM_1; modem_index < NUM_MODEM; modem_index++)
            {
                pSpeechDriver = mSpeechDriverFactory->GetSpeechDriverByIndex((modem_index_t)modem_index);
                if (pSpeechDriver != NULL) // Might be single talk and some speech driver is NULL
                {
                    pSpeechDriver->SetAcousticLoopbackDelayFrames((int32_t)value);
                }
            }

            break;
        }

        // Loopback
        if (param.get(keySET_LOOPBACK_TYPE, value_str) == NO_ERROR)
        {
            param.remove(keySET_LOOPBACK_TYPE);

            // parse format like "SET_LOOPBACK_TYPE=1" / "SET_LOOPBACK_TYPE=1+0"
            int type_value = NO_LOOPBACK;
            int device_value = -1;
            sscanf(value_str.string(), "%d,%d", &type_value, &device_value);
            ALOGV("type_value = %d, device_value = %d", type_value, device_value);

            const loopback_t loopback_type = (loopback_t)type_value;
            loopback_output_device_t loopback_output_device;

            if (loopback_type == NO_LOOPBACK) // close loopback
            {
                LoopbackManager::GetInstance()->SetLoopbackOff();
            }
            else // open loopback
            {
                if (device_value == LOOPBACK_OUTPUT_RECEIVER ||
                    device_value == LOOPBACK_OUTPUT_EARPHONE ||
                    device_value == LOOPBACK_OUTPUT_SPEAKER) // assign output device
                {
                    loopback_output_device = (loopback_output_device_t)device_value;
                }
                else // not assign output device
                {
                    if (AudioSystem::getDeviceConnectionState(AUDIO_DEVICE_OUT_WIRED_HEADSET,   "") == android_audio_legacy::AudioSystem::DEVICE_STATE_AVAILABLE ||
                        AudioSystem::getDeviceConnectionState(AUDIO_DEVICE_OUT_WIRED_HEADPHONE, "") == android_audio_legacy::AudioSystem::DEVICE_STATE_AVAILABLE)
                    {
                        loopback_output_device = LOOPBACK_OUTPUT_EARPHONE;
                    }
                    else if (bForceUseLoudSpeakerInsteadOfReceiver == true)
                    {
                        loopback_output_device = LOOPBACK_OUTPUT_SPEAKER;
                    }
                    else
                    {
                        loopback_output_device = LOOPBACK_OUTPUT_RECEIVER;
                    }
                }
                LoopbackManager::GetInstance()->SetLoopbackOn(loopback_type, loopback_output_device);
            }
            break;
        }


        // TC1 AT%ACS
        if (param.get(keySET_AT_ACS , value_str) == NO_ERROR)
        {
            param.remove(keySET_AT_ACS);

            int acs_value = -1;
            sscanf(value_str.string(), "%d", &acs_value);
            uint32_t current_device;

            switch (acs_value)
            {
                case 0:  // turn off loopback
                    LoopbackManager::GetInstance()->SetLoopbackOff();
                    break;
                case 1:   // turn on loopback by current device status
                    current_device = mAudioResourceManager->getDlOutputDevice();
                    if (current_device == AUDIO_DEVICE_OUT_WIRED_HEADSET)
                    {
                        LoopbackManager::GetInstance()->SetLoopbackOn(MD_HEADSET_MIC_ACOUSTIC_LOOPBACK, LOOPBACK_OUTPUT_EARPHONE);
                    }
                    else if (current_device == AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
                    {
                        LoopbackManager::GetInstance()->SetLoopbackOn(MD_MAIN_MIC_ACOUSTIC_LOOPBACK, LOOPBACK_OUTPUT_EARPHONE);
                    }
                    else
                    {
                        LoopbackManager::GetInstance()->SetLoopbackOn(MD_MAIN_MIC_ACOUSTIC_LOOPBACK, LOOPBACK_OUTPUT_RECEIVER);
                    }
                    break;
                case 2:
                    LoopbackManager::GetInstance()->SetLoopbackOn(MD_HEADSET_MIC_ACOUSTIC_LOOPBACK, LOOPBACK_OUTPUT_EARPHONE);
                    break;
                case 3:
                    LoopbackManager::GetInstance()->SetLoopbackOn(MD_MAIN_MIC_ACOUSTIC_LOOPBACK, LOOPBACK_OUTPUT_SPEAKER);
                    break;
                case 5:
                    mAudioResourceManager->SelectOutputDevice(AUDIO_DEVICE_OUT_WIRED_HEADSET);
                    break;
                case 6:
                    mAudioResourceManager->SelectOutputDevice(AUDIO_DEVICE_OUT_EARPIECE);
                    break;
                case 7:
                    mAudioResourceManager->SelectOutputDevice(AUDIO_DEVICE_OUT_SPEAKER);
                    break;
            }
        }


        // Set ACS volume
        if (param.get(keySET_AT_ACSVolume, value_str) == NO_ERROR)
        {
            param.remove(keySET_AT_ACSVolume);

            sscanf(value_str.string(), "%d", &value);
#ifdef MTK_AUDIO_GAIN_TABLE
            AudioMTKGainController::getInstance()->setAnalogVolume(0, mAudioResourceManager->getDlOutputDevice(), value, (audio_mode_t)AUDIO_MODE_IN_CALL);
#endif
            break;
        }


#ifdef  MTK_TTY_SUPPORT
        // Set TTY mode
        if (param.get(keySetTtyMode, value_str) == NO_ERROR)
        {
            param.remove(keySetTtyMode);
            tty_mode_t tty_mode;

            if (value_str == "tty_full")
            {
                tty_mode = AUD_TTY_FULL;
            }
            else if (value_str == "tty_vco")
            {
                tty_mode = AUD_TTY_VCO;
            }
            else if (value_str == "tty_hco")
            {
                tty_mode = AUD_TTY_HCO;
            }
            else if (value_str == "tty_off")
            {
                tty_mode = AUD_TTY_OFF;
            }
            else
            {
                ALOGD("setParameters tty_mode error !!");
                tty_mode = AUD_TTY_ERR;
            }

            SpeechPhoneCallController::GetInstance()->SetTtyCtmMode(tty_mode, mMode);
            break;
        }
#endif
#if defined(EVDO_DT_VEND_SUPPORT)
        //Analog FM enable
        if (param.getInt(keySET_WARNING_TONE, value) == NO_ERROR)
        {
            param.remove(keySET_WARNING_TONE);
            ALOGD("keySET_WARNING_TONE=%d, GetActiveModemIndex=%d \n", value, mSpeechDriverFactory->GetActiveModemIndex());

            mSpeechDriverFactory->GetSpeechDriver()->SetWarningTone(value);
            break;
        }
        if (param.getInt(keySTOP_WARNING_TONE, value) == NO_ERROR)
        {
            param.remove(keySTOP_WARNING_TONE);
            mSpeechDriverFactory->GetSpeechDriver()->StopWarningTone();
            break;
        }
        if (param.getInt(keySET_VOICE_VOLUME_INDEX, value) == NO_ERROR)
        {
            param.remove(keySET_VOICE_VOLUME_INDEX);
            ALOGD("keySET_VOICE_VOLUME_INDEX=%d, GetActiveModemIndex=%d \n", value, mSpeechDriverFactory->GetActiveModemIndex());

            modem_index_t modem_index = SpeechDriverFactory::GetInstance()->GetActiveModemIndex();
            if (modem_index == MODEM_EXTERNAL)
            {
                ALOGD("SpeechDriver ModemType=VEND_EVDO");
                mSpeechDriverFactory->GetSpeechDriverByIndex(modem_index)->SetDownlinkGain(value);
            }
            break;
        }
#endif
        if (param.get(keySetBTMode, value_str) == NO_ERROR)
        {
            param.remove(keySetBTMode);
            if (value_str == "on")
            {
                mAudioBTCVSDControl->BT_SCO_SetMode(true);
                SpeechPhoneCallController::GetInstance()->SetBTMode(true);
            }
            else if (value_str == "off")
            {
                mAudioBTCVSDControl->BT_SCO_SetMode(false);
                SpeechPhoneCallController::GetInstance()->SetBTMode(false);
            }

            break;
        }

        if (param.getInt(keyEnableStereoOutput, value) == NO_ERROR)
        {
            ALOGD("keyEnableStereoOutput=%d", value);
            mAudioMTKStreamManager->setParametersToStreamOut(keyValuePairs);
            param.remove(keyEnableStereoOutput);
            break;
        }

        if (param.getInt(keyAUDIO_DUMP_OUT_STREAMOUT, value) == NO_ERROR)
        {
            param.remove(keyAUDIO_DUMP_OUT_STREAMOUT);
            bDumpStreamOutFlg = value;
            break;
        }

        if (param.getInt(keyAUDIO_DUMP_IN_STREAMIN, value) == NO_ERROR)
        {
            param.remove(keyAUDIO_DUMP_IN_STREAMIN);
            bDumpStreamInFlg = value;
            break;
        }

	if (param.getInt(keySetFlightMode, value) == NO_ERROR)
        {
            param.remove(keySetFlightMode);
            if (value == 1)
            {
               ALOGD("flignt mode=1");
            }
            else if (value == 0)
            {
               ALOGD("flight mode=0");
            }
            break;
        }


    }
    while (0);

    if (param.size())
    {
        ALOGE("%s() still have param.size() = %d, remain param = \"%s\"", __FUNCTION__, param.size(), param.toString().string());
        status = BAD_VALUE;
    }
    ALOGD("-setCommonParameters(): %s ", keyValuePairs.string());

    return status;
}


String8 AudioMTKHardware::getCommonParameters(AudioParameter &param, AudioParameter &returnParam)
{
    String8 value;
    int cmdType = 0;

    do
    {
        if (param.get(keyMTK_AUDENH_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_AUDENH_SUPPORT);
#ifdef MTK_AUDENH_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_AUDENH_SUPPORT, value);
            break;
        }
        if (param.get(keyMTK_TTY_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_TTY_SUPPORT);
#ifdef MTK_TTY_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_TTY_SUPPORT, value);
            break;
        }
        if (param.get(keyMTK_WB_SPEECH_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_WB_SPEECH_SUPPORT);
#ifdef MTK_WB_SPEECH_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_WB_SPEECH_SUPPORT, value);
            break;
        }
        if (param.get(keyMTK_DUAL_MIC_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_DUAL_MIC_SUPPORT);
#ifdef MTK_DUAL_MIC_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_DUAL_MIC_SUPPORT, value);
            break;
        }
        if (param.get(keyMTK_AUDIO_HD_REC_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_AUDIO_HD_REC_SUPPORT);
#ifdef MTK_AUDIO_HD_REC_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_AUDIO_HD_REC_SUPPORT, value);
            break;
        }

        if (param.get(keyMTK_BESLOUDNESS_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_BESLOUDNESS_SUPPORT);
#ifdef MTK_BESLOUDNESS_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_BESLOUDNESS_SUPPORT, value);
            break;
        }
        if (param.get(keyMTK_BESSURROUND_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_BESSURROUND_SUPPORT);
#ifdef MTK_BESSURROUND_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_BESSURROUND_SUPPORT, value);
            break;
        }
        if (param.get(keyMTK_HDMI_MULTI_CHANNEL_SUPPORT, value) == NO_ERROR)
        {
            param.remove(keyMTK_HDMI_MULTI_CHANNEL_SUPPORT);
#ifdef MTK_HDMI_MULTI_CHANNEL_SUPPORT
            value = "true";
#else
            value = "false";
#endif
            returnParam.add(keyMTK_HDMI_MULTI_CHANNEL_SUPPORT, value);
            break;
        }

        if (param.get(keyGetIsWiredHeadsetOn, value) == NO_ERROR)
        {
            bool isWiredHeadsetOn = mAudioResourceManager->IsWiredHeadsetOn();
            value = (isWiredHeadsetOn) ? "true" : "false";
            param.remove(keyGetIsWiredHeadsetOn);
            returnParam.add(keyGetIsWiredHeadsetOn, value);
            break;
        }

        if (param.get(keyGetFmEnable, value) == NO_ERROR) // TODO(Harvey): check use it or not for FM dev-to-dev
        {
            AudioFMController *pAudioFMController = AudioFMController::GetInstance();
            const bool rx_status       = pAudioFMController->GetFmEnable();
            const bool fm_power_status = pAudioFMController->GetFmChipPowerInfo();
            value = (rx_status && fm_power_status) ? "true" : "false";
            param.remove(keyGetFmEnable);
            returnParam.add(keyGetFmEnable, value);
            break;
        }

        if (param.get(keySpeechBand, value) == NO_ERROR)
        {
            param.remove(keySpeechBand);
            bool nb = mAudioVolumeInstance->isNbSpeechBand();
            value = nb ? "1" : "0";
            returnParam.add(keySpeechBand, value);
            break;
        }

#if defined(MTK_DUAL_MIC_SUPPORT)
        if (param.getInt(keyDUALMIC_GET_GAIN, cmdType) == NO_ERROR)
        {
            unsigned short gain = 0;
            char buf[32];

            if (mAudioTuningInstance->getDMNRGain((unsigned short)cmdType, &gain) == NO_ERROR)
            {
                sprintf(buf, "%d", gain);
                returnParam.add(keyDUALMIC_GET_GAIN, String8(buf));
                param.remove(keyDUALMIC_GET_GAIN);
            }
            break;
        }
#endif

        if (param.get(keyMusicPlusGet, value) == NO_ERROR)
        {
#ifndef HIFI_SWITCH_BY_AUDENH  //HP switch use AudEnh setting
            bool musicplus_status = mAudioMTKStreamManager->GetMusicPlusStatus();
#else
            bool musicplus_status = mAudioMTKStreamManager->GetHiFiDACStatus();
#endif
            value = (musicplus_status) ? "1" : "0";
            param.remove(keyMusicPlusGet);
            returnParam.add(keyMusicPlusGet, value);
            break;
        }
        if (param.get(keyBesLoudnessGet, value) == NO_ERROR)
        {

            bool besloudness_status = mAudioMTKStreamManager->GetBesLoudnessStatus();
            value = (besloudness_status) ? "1" : "0";
            param.remove(keyBesLoudnessGet);
            returnParam.add(keyBesLoudnessGet, value);
            break;
        }

        if (param.get(keyHiFiDACGet, value) == NO_ERROR)
        {
            bool hifidac_status = mAudioMTKStreamManager->GetHiFiDACStatus();
            value = (hifidac_status) ? "1" : "0";
            param.remove(keyHiFiDACGet);
            returnParam.add(keyHiFiDACGet, value);
            break;
        }


        // Dual Mic Noise Reduction, DMNR for Receiver
        if (param.get(keyGet_Dual_Mic_Setting, value) == NO_ERROR) // new name
        {
            param.remove(keyGet_Dual_Mic_Setting);
            value = (SpeechEnhancementController::GetInstance()->GetDynamicMask(SPH_ENH_DYNAMIC_MASK_DMNR) > 0) ? "1" : "0";
            returnParam.add(keyGet_Dual_Mic_Setting, value);
            break;
        }

        // Dual Mic Noise Reduction, DMNR for Loud Speaker
        if (param.get(keyGET_LSPK_DMNR_ENABLE, value) == NO_ERROR) // new name
        {
            param.remove(keyGET_LSPK_DMNR_ENABLE);
            value = (SpeechEnhancementController::GetInstance()->GetDynamicMask(SPH_ENH_DYNAMIC_MASK_LSPK_DMNR) > 0) ? "1" : "0";
            returnParam.add(keyGET_LSPK_DMNR_ENABLE, value);
            break;
        }


        // Voice Clarity Engine, VCE
        if (param.get(keyGET_VCE_ENABLE, value) == NO_ERROR) // new name
        {
            param.remove(keyGET_VCE_ENABLE);
            value = (SpeechEnhancementController::GetInstance()->GetDynamicMask(SPH_ENH_DYNAMIC_MASK_VCE) > 0) ? "1" : "0";
            returnParam.add(keyGET_VCE_ENABLE, value);
            break;
        }
        if (param.get(keyGET_VCE_STATUS, value) == NO_ERROR) // old name
        {
            param.remove(keyGET_VCE_STATUS);
            value = (SpeechEnhancementController::GetInstance()->GetDynamicMask(SPH_ENH_DYNAMIC_MASK_VCE) > 0) ? "1" : "0";
            returnParam.add(keyGET_VCE_STATUS, value);
            break;
        }

        // Magic Conference Call
        if (param.get(keyGET_MAGIC_CON_CALL_ENABLE, value) == NO_ERROR) // new name
        {
            param.remove(keyGET_MAGIC_CON_CALL_ENABLE);
            value = (SpeechEnhancementController::GetInstance()->GetMagicConferenceCallOn() == true) ? "1" : "0";
            returnParam.add(keyGET_MAGIC_CON_CALL_ENABLE, value);
            break;
        }

        // VoIP Dual Mic Noise Reduction, DMNR for Receiver
        if (param.get(keyGET_VOIP_RECEIVER_DMNR_ENABLE, value) == NO_ERROR)
        {
            param.remove(keyGET_VOIP_RECEIVER_DMNR_ENABLE);
            value = (mAudioSpeechEnhanceInfoInstance->GetDynamicSpeechEnhancementMaskOnOff(VOIP_SPH_ENH_DYNAMIC_MASK_DMNR) > 0) ? "1" : "0";
            returnParam.add(keyGET_VOIP_RECEIVER_DMNR_ENABLE, value);
            break;
        }

        // VoIP Dual Mic Noise Reduction, DMNR for Loud Speaker
        if (param.get(keyGET_VOIP_LSPK_DMNR_ENABLE, value) == NO_ERROR)
        {
            param.remove(keyGET_VOIP_LSPK_DMNR_ENABLE);
            value = (mAudioSpeechEnhanceInfoInstance->GetDynamicSpeechEnhancementMaskOnOff(VOIP_SPH_ENH_DYNAMIC_MASK_LSPK_DMNR) > 0) ? "1" : "0";
            returnParam.add(keyGET_VOIP_LSPK_DMNR_ENABLE, value);
            break;
        }

        // Audio Volume version
        if (param.get(keyGET_AUDIO_VOLUME_VER, value) == NO_ERROR)
        {
            param.remove(keyGET_AUDIO_VOLUME_VER);
            value = "1";
            returnParam.add(keyGET_AUDIO_VOLUME_VER, value);
            break;
        }

        // check if the LR channel switched
        if (param.get(keyLR_ChannelSwitch, value) == NO_ERROR)
        {
#ifdef MTK_DUAL_MIC_SUPPORT
            char buf[32];
            bool bIsLRSwitch = mAudioSpeechEnhanceInfoInstance->GetRecordLRChannelSwitch();
            sprintf(buf, "%d", bIsLRSwitch);
            returnParam.add(keyLR_ChannelSwitch, String8(buf));
            ALOGD("LRChannelSwitch=%d", bIsLRSwitch);
#else
            ALOGD("only support in dual MIC");
#endif
            param.remove(keyLR_ChannelSwitch);
            break;
        }

        if (param.get(keyAUDIO_DUMP_OUT_STREAMOUT, value) == NO_ERROR)
        {
            param.remove(keyAUDIO_DUMP_OUT_STREAMOUT);
            value = bDumpStreamOutFlg ? "1" : "0";
            returnParam.add(keyAUDIO_DUMP_OUT_STREAMOUT, value);
            break;
        }

        if (param.get(keyAUDIO_DUMP_IN_STREAMIN, value) == NO_ERROR)
        {
            param.remove(keyAUDIO_DUMP_IN_STREAMIN);
            value = bDumpStreamInFlg ? "1" : "0";
            returnParam.add(keyAUDIO_DUMP_IN_STREAMIN, value);
            break;
        }
    }
    while (0);

    String8 keyValuePairs = returnParam.toString();
    ALOGD("-%s(), keyValuePairs = %s", __FUNCTION__, keyValuePairs.string());
    return keyValuePairs;
}

status_t AudioMTKHardware::setMasterMute(bool muted)
{
    return INVALID_OPERATION;
}


#define FM_DEVICE_TO_DEVICE_SUPPORT_OUTPUT_DEVICES (AUDIO_DEVICE_OUT_SPEAKER | AUDIO_DEVICE_OUT_WIRED_HEADSET | AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
const char* strAudioPatchRole[]={"AUDIO_PORT_ROLE_NONE","AUDIO_PORT_ROLE_SOURCE","AUDIO_PORT_ROLE_SINK"};
const char* strAudioPatchType[]={"AUDIO_PORT_TYPE_NONE","AUDIO_PORT_TYPE_DEVICE","AUDIO_PORT_TYPE_MIX","AUDIO_PORT_TYPE_SESSION"};

int AudioMTKHardware::createAudioPatch(unsigned int num_sources,
                                       const struct audio_port_config *sources,
                                       unsigned int num_sinks,
                                       const struct audio_port_config *sinks,
                                       audio_patch_handle_t *handle)
{
    int status = NO_ERROR;
    ALOGD("+%s num_sources [%d] , num_sinks [%d]", __FUNCTION__, num_sources, num_sinks);
#if 1 //Debug
    //ALOGD("+%s num_sources [%d] , num_sinks [%d]", __FUNCTION__, num_sources, num_sinks);
    if (handle == NULL || sources == NULL || sinks == NULL)
    {
        ALOGW("Ptr is null");
        return BAD_VALUE;
    }
    ALOGD("handlecheck %s handle [0x%x] current size %d",__FUNCTION__, *handle,mAudioHalPatchVector.size());
    int i = 0, j = 0;

    for (i = 0; i < num_sources ; i++)
    {
        ALOGD("== source [%d]/[%d] ==", i, num_sources);
        ALOGD("id 0x%x", sources[i].id);
        ALOGD("role 0x%x %s", sources[i].role,strAudioPatchRole[sources[i].role]);
        ALOGD("type 0x%x %s", sources[i].type,strAudioPatchType[sources[i].type]);
        ALOGD("config_mask 0x%x", sources[i].config_mask);
        ALOGD("sample_rate 0x%x", sources[i].sample_rate);
        ALOGD("channel_mask 0x%x", sources[i].channel_mask);
        ALOGD("gain.index 0x%x", sources[i].gain.index);
        ALOGD("gain.mode 0x%x", sources[i].gain.mode);
        ALOGD("gain.channel_mask 0x%x", sources[i].gain.channel_mask);
        ALOGD("gain.ramp_duration_ms 0x%x", sources[i].gain.ramp_duration_ms);
#if 0 //When gain check , enable        
        for (j = 0; j < sizeof(audio_channel_mask_t) * 8; j++)
        {
            ALOGD("gain.values[%d] 0x%x", j, sources[i].gain.values[j]);
        }
#endif
        if (sources[i].type == AUDIO_PORT_TYPE_DEVICE)
        {
            ALOGD("device.hw_module %x", sources[i].ext.device.hw_module);
            ALOGD("device.type %x", sources[i].ext.device.type);
            ALOGD("device.address %s", sources[i].ext.device.address);
        }
        else if (sources[i].type == AUDIO_PORT_TYPE_MIX)
        {
            ALOGD("mix.hw_module %x", sources[i].ext.mix.hw_module);
            ALOGD("mix.handle %x", sources[i].ext.mix.handle);
            ALOGD("mix.usecase.stream %x", sources[i].ext.mix.usecase.stream);
            ALOGD("mix.usecase.source %x", sources[i].ext.mix.usecase.source);
        }
        else if (sources[i].type == AUDIO_PORT_TYPE_SESSION)
        {

        }

    }

    for (i = 0; i < num_sinks ; i++)
    {
        ALOGD("== sinks [%d]/[%d] ==", i, num_sinks);
        ALOGD("id 0x%x", sinks[i].id);
        ALOGD("role 0x%x %s", sinks[i].role,strAudioPatchRole[sinks[i].role]);
        ALOGD("type 0x%x %s", sinks[i].type,strAudioPatchType[sinks[i].type]);
        ALOGD("config_mask 0x%x", sinks[i].config_mask);
        ALOGD("sample_rate 0x%x", sinks[i].sample_rate);
        ALOGD("channel_mask 0x%x", sinks[i].channel_mask);
        ALOGD("gain.index 0x%x", sinks[i].gain.index);
        ALOGD("gain.mode 0x%x", sinks[i].gain.mode);
        ALOGD("gain.channel_mask 0x%x", sinks[i].gain.channel_mask);
        ALOGD("gain.ramp_duration_ms 0x%x", sinks[i].gain.ramp_duration_ms);
#if 0 //When gain check , enable       
        for (j = 0; j < sizeof(audio_channel_mask_t) * 8; j++)
        {
            ALOGD("gain.values[%d] 0x%x", j, sinks[i].gain.values[j]);
        }
#endif
        if (sinks[i].type == AUDIO_PORT_TYPE_DEVICE)
        {
            ALOGD("device.hw_module %x", sinks[i].ext.device.hw_module);
            ALOGD("device.type %x", sinks[i].ext.device.type);
            ALOGD("device.address %s", sinks[i].ext.device.address);
        }
        else if (sinks[i].type == AUDIO_PORT_TYPE_MIX)
        {
            ALOGD("mix.hw_module %x", sinks[i].ext.mix.hw_module);
            ALOGD("mix.handle %x", sinks[i].ext.mix.handle);
            ALOGD("mix.usecase.stream %x", sinks[i].ext.mix.usecase.stream);
            ALOGD("mix.usecase.source %x", sinks[i].ext.mix.usecase.source);
        }
        else if (sinks[i].type == AUDIO_PORT_TYPE_SESSION)
        {

        }
    }


#endif
#if 1
//    ALOGD("+%s num_sources [%d] , num_sinks [%d]", __FUNCTION__, num_sources, num_sinks);
    audio_devices_t eOutDeviceList = AUDIO_DEVICE_NONE;
    audio_devices_t eInputDeviceList = AUDIO_DEVICE_NONE;
    audio_source_t eInputSource = AUDIO_SOURCE_DEFAULT;
    do
    {
        if (handle == NULL || sources == NULL || sinks == NULL)
        {
            ALOGW("Ptr is null");
            status = BAD_VALUE;
            break;
        }
        // We can support legacy routing with setting single source or single sink
        if ((!num_sources && !num_sinks) || (num_sources > 1) || (num_sinks > AUDIO_PATCH_PORTS_MAX))
        {
            ALOGW("num is invalid");
            status = BAD_VALUE;
            break;
        }

        if (sources[0].type == AUDIO_PORT_TYPE_MIX)
        {

            if (sinks[0].type != AUDIO_PORT_TYPE_DEVICE)
            {
                ALOGW("sinks[0].type != AUDIO_PORT_TYPE_DEVICE");
                status = BAD_VALUE;
                break;
            }

            int dDeviceIndex;

            for (dDeviceIndex = 0; dDeviceIndex < num_sinks; dDeviceIndex++)
            {
                eOutDeviceList |= sinks[dDeviceIndex].ext.device.type;
            }
            if (eOutDeviceList == AUDIO_DEVICE_NONE)
            {
                ALOGW("Mixer->Device Routing to AUDIO_DEVICE_NONE");
                status = BAD_VALUE;
                break;
            }
#if 0
            if (eOutDeviceList == AUDIO_DEVICE_OUT_SPEAKER)
            {
                for (ssize_t index = mAudioHalPatchVector.size()-1; index >= 0; index--)
                {
                    for (int sink_i=0;sink_i<mAudioHalPatchVector[index]->num_sources;sink_i++)
                    {
                        for (int source_i=0;source_i<mAudioHalPatchVector[index]->num_sources;source_i++)
                        {
                            if ((mAudioHalPatchVector[index]->sources[source_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                                (mAudioHalPatchVector[index]->sources[source_i].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER) &&
                                (mAudioHalPatchVector[index]->sinks[sink_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                                (mAudioHalPatchVector[index]->sinks[sink_i].ext.device.type == AUDIO_DEVICE_OUT_WIRED_HEADPHONE
                                ||mAudioHalPatchVector[index]->sinks[sink_i].ext.device.type == AUDIO_DEVICE_OUT_WIRED_HEADSET) )
                            {
                                ALOGD("Mute FM");
                                //AudioFMController::GetInstance()->SetFmVolume(0.0,true);//For be sure mFmVolume doesn't equal to -1.0
                                break;
                            }
                        }
                    }
                }
            }
#endif
            ALOGD("+routing createAudioPatch Mixer->%x",eOutDeviceList);
            AudioParameter param;
            param.addInt(String8(AudioParameter::keyRouting), (int)eOutDeviceList);
            status = mAudioMTKStreamManager->setParameters(param.toString(),
                sources[0].ext.mix.handle);
            if (status == NO_ERROR)
            {
                ssize_t index;
                ssize_t total = mAudioHalPatchVector.size();
                for (index = total-1;index >= 0; index--)
                {

                    if (mAudioHalPatchVector[index]->sources[0].type == AUDIO_PORT_TYPE_MIX &&
                        mAudioHalPatchVector[index]->sinks[0].type == AUDIO_PORT_TYPE_DEVICE &&
                        sources[0].ext.mix.handle == mAudioHalPatchVector[index]->sources[0].ext.mix.handle)
                    {
                        AudioHalPatch *patch;
                        patch = mAudioHalPatchVector[index];
                        ALOGD("handlecheck createAudioPatch() removing patch handle %d index %u DL", mAudioHalPatchVector[index]->mHalHandle,index);
                        mAudioHalPatchVector.removeAt(index);
                        delete(patch);
                        break;
                    }
                }
            }
            else
            {
                ALOGE("Err %s %d",__FUNCTION__,__LINE__);
            }

        }
        else if (sources[0].type == AUDIO_PORT_TYPE_DEVICE)
        {

            if (sinks[0].type == AUDIO_PORT_TYPE_MIX)
            {

                eInputDeviceList = sources[0].ext.device.type;
                eInputSource = sinks[0].ext.mix.usecase.source;
                ALOGD("+routing createAudioPatch %x->Mixer Src %x",
                        eInputDeviceList,eInputSource);
                AudioParameter param;
                param.addInt(String8(AudioParameter::keyRouting), (int)eInputDeviceList);
                param.addInt(String8(AudioParameter::keyInputSource),
                             (int)eInputSource);

                status = mAudioMTKStreamManager->setParameters(param.toString(),
                    sinks[0].ext.mix.handle);

                if (status == NO_ERROR)
                {
                    ssize_t index;
                    ssize_t total = mAudioHalPatchVector.size();
                    for (index = total-1;index >= 0; index--)
                    {

                        if (mAudioHalPatchVector[index]->sources[0].type == AUDIO_PORT_TYPE_DEVICE &&
                            mAudioHalPatchVector[index]->sinks[0].type == AUDIO_PORT_TYPE_MIX &&
                            sinks[0].ext.mix.handle == mAudioHalPatchVector[index]->sinks[0].ext.mix.handle)
                        {
                            AudioHalPatch *patch;
                            patch = mAudioHalPatchVector[index];
                            ALOGD("handlecheck createAudioPatch() removing patch handle %d index %u UL", mAudioHalPatchVector[index]->mHalHandle,index);
                            mAudioHalPatchVector.removeAt(index);
                            delete(patch);
                            break;
                        }
                    }

                    if (eInputDeviceList == AUDIO_DEVICE_IN_FM_TUNER)
                    {
                        if (mUseAudioPatchForFm == true)
                        {
                            status = AudioFMController::GetInstance()->SetFmEnable(true,true,false);
                        }
                    }
                }
                else
                {
                    ALOGE("Err %s %d",__FUNCTION__,__LINE__);
                }

            }
            else if (sinks[0].type == AUDIO_PORT_TYPE_DEVICE)
            {
                ALOGW("sinks[0].type == AUDIO_PORT_TYPE_DEVICE");
                // DO Device to Device
                eInputDeviceList = sources[0].ext.device.type;
                int dDeviceIndex;

                for (dDeviceIndex = 0; dDeviceIndex < num_sinks; dDeviceIndex++)
                {
                    eOutDeviceList |= sinks[dDeviceIndex].ext.device.type; //should be only one device , limited by frameworks
                }

                if (eInputDeviceList != AUDIO_DEVICE_IN_FM_TUNER ||
                    !(eOutDeviceList & FM_DEVICE_TO_DEVICE_SUPPORT_OUTPUT_DEVICES))
                {
                    ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                    status = INVALID_OPERATION;
                    break;
                }
                else if (eInputDeviceList == AUDIO_DEVICE_IN_FM_TUNER)
                {
                    if (AudioFMController::GetInstance()->CheckFmNeedUseDirectConnectionMode() == false)
                    {
                        ALOGW("[%s] [%d] InDirectConnectionMode", __FUNCTION__, __LINE__);
                        status = INVALID_OPERATION;
                        break;
                    }

                    if (mUseAudioPatchForFm == false)
                    {
                        ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                        status = INVALID_OPERATION;
                        break;
                    }

                    // routing
                    for (dDeviceIndex = 0; dDeviceIndex < num_sinks; dDeviceIndex++)
                    {
                        eOutDeviceList |= sinks[dDeviceIndex].ext.device.type;
                    }

                    AudioParameter param;
                    param.addInt(String8(AudioParameter::keyRouting), (int)eOutDeviceList);
                    //status = mAudioMTKStreamManager->setParameters(param.toString(), sources[0].ext.mix.handle);
                    //The sources[0].ext.mix.handle doesn't record IOport
                    status = mAudioMTKStreamManager->setParametersToStreamOut(param.toString());

                    if (status != NO_ERROR)
                    {
                        ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                        break;
                    }

                    ALOGD("+routing createAudioPatch %x->%x",eInputDeviceList,eOutDeviceList);
                    AudioFMController::GetInstance()->SetFmVolume(0);//initial value is -1 , should change it first
                    // FM enable
                    AudioFMController::GetInstance()->SetFmEnable(false);// make sure FM disable first (FM App non-sync issue)
                    status = AudioFMController::GetInstance()->SetFmEnable(true,true,true);
                    //FMTODO : Gain setting
                    break;
                }
            }

        }



    }
    while (0);

    if (status == NO_ERROR)
    {
        *handle = android_atomic_inc(&mNextUniqueId);
        AudioHalPatch *newPatch = new AudioHalPatch(*handle);
        newPatch->num_sources = num_sources;
        newPatch->num_sinks = num_sinks;
        for (unsigned int index = 0; index < num_sources ; index++)
        {
            memcpy((void *)&newPatch->sources[index], (void *)&sources[index],
                sizeof(struct audio_port_config));
        }
        for (unsigned int index = 0; index < num_sinks ; index++)
        {
            memcpy((void *)&newPatch->sinks[index], (void *)&sinks[index],
                sizeof(struct audio_port_config));
        }
        mAudioHalPatchVector.add(newPatch);

        ALOGD("handlecheck %s sucess new *handle 0x%x",__FUNCTION__, (int)(*handle));
    }
    else
    {
        ALOGD("Fail status %d", (int)(status));
    }
    ALOGD("-%s num_sources [%d] , num_sinks [%d]", __FUNCTION__, num_sources, num_sinks);
#endif
    return status;
}

int AudioMTKHardware::releaseAudioPatch(audio_patch_handle_t handle)
{
    //TODO
    int status = NO_ERROR;
    ssize_t index;
    bool bReturnFlag = false;
    AudioHalPatch *patch;
    ALOGD("handlecheck %s handle [0x%x]", __FUNCTION__, handle);
    do
    {

        if (handle == AUDIO_PATCH_HANDLE_NONE)
        {
            ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
            status = BAD_VALUE;
            return status;
        }

        for (index = 0; index < mAudioHalPatchVector.size(); index++)
        {
            if (handle == mAudioHalPatchVector[index]->mHalHandle)
            {
                break;
            }
        }
        if (index == mAudioHalPatchVector.size())
        {
            ALOGW("handlecheck %s [%d] null %d", __FUNCTION__, __LINE__,mAudioHalPatchVector.size());
            status = INVALID_OPERATION;
            return status;
        }

        patch = mAudioHalPatchVector[index];
        mAudioHalPatchVector.removeAt(index);

        if (patch->sources[0].type == AUDIO_PORT_TYPE_MIX)
        {

            if (patch->sinks[0].type != AUDIO_PORT_TYPE_DEVICE)
            {
                ALOGW("sinks[0].type != AUDIO_PORT_TYPE_DEVICE");
                status = BAD_VALUE;
                break;
            }
            ALOGD("+routing releaseAudioPatch Mixer->%x",patch->sinks[0].ext.device.type);
            for (index = mAudioHalPatchVector.size()-1; index >= 0; index--)
            {
                for (int sink_i=0;sink_i<mAudioHalPatchVector[index]->num_sinks;sink_i++)
                {
                    if ((mAudioHalPatchVector[index]->sinks[sink_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                        (mAudioHalPatchVector[index]->sinks[sink_i].ext.device.type != AUDIO_DEVICE_NONE))
                    {
                            ALOGD("Still have AudioPatches routing to outputDevice, Don't routing null Size %d",mAudioHalPatchVector.size());
                            status = NO_ERROR;
                            bReturnFlag = true;
                            break;
                    }
                }
            }
            if (bReturnFlag)
                break;
#if 0   //Policy doesn't change to none , it will non-sync
            AudioParameter param;
            param.addInt(String8(AudioParameter::keyRouting), (int)AUDIO_DEVICE_NONE);
            status = mAudioMTKStreamManager->setParameters(param.toString(),
                patch->sources[0].ext.mix.handle);
#endif
        }
        else if (patch->sources[0].type == AUDIO_PORT_TYPE_DEVICE)
        {

            if (patch->sinks[0].type == AUDIO_PORT_TYPE_MIX)
            {
                // close FM if need (indirect)
                    ALOGD("+routing releaseAudioPatch %x->Mixer",patch->sources[0].ext.device.type);
                    if (mUseAudioPatchForFm == true)
                    {
                        if (patch->sources[0].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER)
                        {
                                for (index = mAudioHalPatchVector.size()-1; index >= 0; index--)
                                {
                                    for (int source_i=0;source_i<mAudioHalPatchVector[index]->num_sources;source_i++)
                                    {
                                        if ((mAudioHalPatchVector[index]->sources[source_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                                            (mAudioHalPatchVector[index]->sources[source_i].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER))
                                        {
                                                ALOGD("Still have AudioPatches need  AUDIO_DEVICE_IN_FM_TUNER, Don't Disable FM [%d]",
                                                    mAudioHalPatchVector.size());
                                                status = NO_ERROR;
                                                bReturnFlag = true;
                                                break;
                                        }
                                    }
                                }
                                if (!bReturnFlag)
                                    status = AudioFMController::GetInstance()->SetFmEnable(false);
                        }
                    }

                audio_devices_t eInDeviceList = AUDIO_DEVICE_NONE;
                for (index = mAudioHalPatchVector.size()-1; index >= 0; index--)
                {
                    for (int source_i=0;source_i<mAudioHalPatchVector[index]->num_sources;source_i++)
                    {
                        if ((mAudioHalPatchVector[index]->sources[source_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                            (mAudioHalPatchVector[index]->sources[source_i].ext.device.type != AUDIO_DEVICE_NONE))
                        {
                                eInDeviceList = mAudioHalPatchVector[index]->sources[source_i].ext.device.type;
                                ALOGD("Still have AudioPatches need  routing input device, Don't change routing [%d]"
                                    ,mAudioHalPatchVector.size());
                                status = NO_ERROR;
                                //bReturnFlag = true;
                                break;
                        }
                    }
                }
                //if (bReturnFlag)
                     //break;
                AudioParameter param;
                param.addInt(String8(AudioParameter::keyRouting), (int)eInDeviceList);
                status = mAudioMTKStreamManager->setParameters(param.toString(),
                    patch->sinks[0].ext.mix.handle);
            }
            else if (patch->sinks[0].type == AUDIO_PORT_TYPE_DEVICE)
            {
                ALOGD("+routing releaseAudioPatch %x->%x",patch->sources[0].ext.device.type,patch->sinks[0].ext.device.type);
                if ((patch->sources[0].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER) &&
                    (patch->sinks[0].ext.device.type & FM_DEVICE_TO_DEVICE_SUPPORT_OUTPUT_DEVICES))
                {
                    // close FM if need (direct)
                    if (mUseAudioPatchForFm == false)
                    {
                        ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                        status = INVALID_OPERATION;
                    }
                    else
                    {
#if 0
                        for (index = mAudioHalPatchVector.size()-1; index >= 0; index--)
                        {
                            for (int source_i=0;source_i<mAudioHalPatchVector[index]->num_sources;source_i++)
                            {
                                if ((mAudioHalPatchVector[index]->sources[source_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                                    (mAudioHalPatchVector[index]->sources[source_i].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER))
                                {
                                        ALOGD("Still have AudioPatches need  AUDIO_DEVICE_IN_FM_TUNER, Don't Disable FM [%d]",
                                            mAudioHalPatchVector.size());
                                        status = NO_ERROR;
                                        bReturnFlag = true;
                                        break;
                                }
                            }
                        }
                        if (!bReturnFlag)
                        {
                            AudioFMController::GetInstance()->SetFmVolume(0);
                            status = AudioFMController::GetInstance()->SetFmEnable(false);
                        }
#else
                        //always disable fm for direct/indirect setting pass
                        AudioFMController::GetInstance()->SetFmVolume(0);
                        status = AudioFMController::GetInstance()->SetFmEnable(false);
#endif

//                        bReturnFlag = false;
                        audio_devices_t eOutDeviceList = AUDIO_DEVICE_NONE;
                        //Restore previous output device setting
                        for (index = mAudioHalPatchVector.size()-1; index >= 0; index--)
                        {
                            for (int sink_i=0;sink_i<mAudioHalPatchVector[index]->num_sinks;sink_i++)
                            {
                                if ((mAudioHalPatchVector[index]->sinks[sink_i].type == AUDIO_PORT_TYPE_DEVICE) &&
                                    (mAudioHalPatchVector[index]->sinks[sink_i].ext.device.type != AUDIO_DEVICE_NONE) &&
                                    (mAudioHalPatchVector[index]->sources[0].type == AUDIO_PORT_TYPE_MIX))
                                {
                                        eOutDeviceList = eOutDeviceList|mAudioHalPatchVector[index]->sinks[sink_i].ext.device.type;
                                        ALOGD("Still have AudioPatches routing to outputDevice, Don't routing null sink_i/Size %d/%d , device 0x%x handle %x",
                                            sink_i,mAudioHalPatchVector.size(),eOutDeviceList,mAudioHalPatchVector[index]->mHalHandle);
                                        status = NO_ERROR;
//                                        bReturnFlag = true;
                                }
                            }
                            if (eOutDeviceList)
                                    break;
                        }
//                        if (bReturnFlag)
//                            break;
                        AudioParameter param;
                        param.addInt(String8(AudioParameter::keyRouting), (int)eOutDeviceList);
                        status = mAudioMTKStreamManager->setParametersToStreamOut(param.toString());

                    }
                    break;
                }
                else
                {
                    ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                    status = INVALID_OPERATION;//TODO
                    break;
                }

            }
        }
       } while (0);

    if (status == NO_ERROR)
    {
        ALOGD("handlecheck %s remove handle [%x] OK",__FUNCTION__,handle);
        delete(patch);
    }
    else
    {
        ALOGD("handlecheck %s remove handle [%x] NG",__FUNCTION__,handle);
        mAudioHalPatchVector.add(patch);
    }
    ALOGD("-%s handle [0x%x] status [%d]", __FUNCTION__, handle, status);
    return status;
}


int AudioMTKHardware::getAudioPort(struct audio_port *port)
{
    //TODO , I think the implementation is designed in aps.
    ALOGW("-%s Unsupport", __FUNCTION__);
    return INVALID_OPERATION;
}

// We limit valid for existing Audio port of AudioPatch
int AudioMTKHardware::setAudioPortConfig(const struct audio_port_config *config)
{
    int status = NO_ERROR;

    do
    {

        if (config == NULL)
        {
            ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
            status = BAD_VALUE;
            break;
        }

        if ((config->config_mask & AUDIO_PORT_CONFIG_GAIN) == 0)
        {
            ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
            status = INVALID_OPERATION;
            break;
        }
        ALOGD("%s",__FUNCTION__);
        ALOGD("config->type [0x%x]",config->type);
        ALOGD("config->role [0x%x]",config->role);
        ALOGD("config->gain.mode [0x%x]",config->gain.mode);
        ALOGD("config->gain.values[0] [0x%x]",config->gain.values[0]);
        ALOGD("config->gain.ramp_duration_ms [0x%x]",config->gain.ramp_duration_ms);

        if (config->type == AUDIO_PORT_TYPE_MIX)
        {
            if (config->role == AUDIO_PORT_ROLE_SOURCE)
            {
                //Apply Gain to MEMIF , don't support it so far
                ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                status = INVALID_OPERATION;
                break;
            }
            if (config->role == AUDIO_PORT_ROLE_SINK)
            {
                ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                status = INVALID_OPERATION;
                break;
            }

            ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
            status = BAD_VALUE;
            break;

        }
        else if (config->type == AUDIO_PORT_TYPE_DEVICE)
        {
            if (mUseAudioPatchForFm == false)
            {
                ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                status = INVALID_OPERATION;
                break;
            }

            if (config->role == AUDIO_PORT_ROLE_SINK || config->role == AUDIO_PORT_ROLE_SOURCE)
            {
                //Support specific device eg. headphone/speaker
                size_t indexOfPatch;
                size_t indexOfSink;
                audio_port_config *pstCurConfig = NULL;
                bool bhit = false;
                for (indexOfPatch = 0; indexOfPatch < mAudioHalPatchVector.size() && !bhit; indexOfPatch++)
                {
                    for (indexOfSink = 0; indexOfSink < mAudioHalPatchVector[indexOfPatch]->num_sinks; indexOfSink++)
                    {
                        if ((config->ext.device.type == mAudioHalPatchVector[indexOfPatch]->sinks[indexOfSink].ext.device.type)
                            && (mAudioHalPatchVector[indexOfPatch]->sources[indexOfSink].ext.device.type == AUDIO_DEVICE_IN_FM_TUNER)
                            && (mAudioHalPatchVector[indexOfPatch]->sinks[indexOfSink].ext.device.type & FM_DEVICE_TO_DEVICE_SUPPORT_OUTPUT_DEVICES))
                        {
                            bhit = true;
                            pstCurConfig = &(mAudioHalPatchVector[indexOfPatch]->sinks[indexOfSink]);
                            break;
                        }
                    }
                }

                if (!bhit || pstCurConfig == NULL)
                {
                    ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                    status = INVALID_OPERATION;
                    break;
                }

                if (!config->gain.mode)
                {
                    ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                    status = INVALID_OPERATION;
                    break;
                }

                int dGainDB = 0;
                unsigned int ramp_duration_ms = 0;
                float fFMVolume;
                if (config->gain.mode & AUDIO_GAIN_MODE_JOINT | AUDIO_GAIN_MODE_CHANNELS) //Hw support joint only
                {
                    fFMVolume = MappingFMVolofOutputDev(config->gain.values[0],pstCurConfig->ext.device.type);
                }
                else
                {
#ifndef MTK_AUDIO_GAIN_TABLE
                    fFMVolume = AudioMTKVolumeController::linearToLog(dGainDB);
#else
                    fFMVolume = AudioMTKGainController::linearToLog(dGainDB);
#endif
                }

                if (config->gain.mode & AUDIO_GAIN_MODE_RAMP)
                {
                    ramp_duration_ms = config->gain.ramp_duration_ms;
                }

                ALOGD("fFMVolume %f",fFMVolume);
                if (fFMVolume >=0 && fFMVolume <=1.0)
                {
                    AudioFMController::GetInstance()->SetFmVolume(fFMVolume);
                }
                else
                {
                    ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                    status = BAD_VALUE;
                    break;
                }

            }
#if 0
            else if (config->role == AUDIO_PORT_ROLE_SOURCE)
            {
                ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                status = INVALID_OPERATION;
                break;

            }
#endif
            else
            {
                ALOGW("[%s] [%d]", __FUNCTION__, __LINE__);
                status = BAD_VALUE;
                break;
            }
        }


    }
    while (0);

    //TODO
    return status;
}


float AudioMTKHardware::MappingFMVolofOutputDev(int Gain, audio_devices_t eOutput)
{
    float fFMVolume;
    if ((eOutput & FM_DEVICE_TO_DEVICE_SUPPORT_OUTPUT_DEVICES) == 0)
    {
        ALOGE("Error FM createAudioPatch direct mode fail device [0x%x]",eOutput);
        return 1.0;
    }

    if (mUseTuningVolume == false)
    {
        int dGainDB = 0;
        dGainDB = Gain / 100;
        if (dGainDB >= 0)//Support degrade only
            dGainDB = 0;
        else
            dGainDB = (-1)*dGainDB;
        dGainDB = dGainDB<<2;
        if (dGainDB > 256)
            dGainDB = 256;
        dGainDB = 256- dGainDB;

#ifndef MTK_AUDIO_GAIN_TABLE
        fFMVolume = AudioMTKVolumeController::linearToLog(dGainDB);
#else
        fFMVolume = AudioMTKGainController::linearToLog(dGainDB);
#endif

        ALOGD("default f fFMVolume %f",fFMVolume);
        if (fFMVolume < 0)
                fFMVolume = 0;
        else if (fFMVolume > 1.0)
                fFMVolume = 1.0;
    }
    else
    {
        const float fCUSTOM_VOLUME_MAPPING_STEP = 256.0f;
        unsigned char* array;

        if (eOutput & AUDIO_DEVICE_OUT_SPEAKER)
            array = VolCache.audiovolume_steamtype[CUSTOM_VOL_TYPE_MUSIC][CUSTOM_VOLUME_SPEAKER_MODE];
        else
            array = VolCache.audiovolume_steamtype[CUSTOM_VOL_TYPE_MUSIC][CUSTOM_VOLUME_HEADSET_MODE];

        int dIndex = 15- (((-1)*Gain)/300);
        int dMaxIndex = VolCache.audiovolume_level[CUSTOM_VOL_TYPE_MUSIC];
        ALOGD("FM index %d",dIndex);
        if (dIndex > 15)
            dIndex = 15;
        else if (dIndex < 0)
            dIndex = 0;
        float vol = (fCUSTOM_VOLUME_MAPPING_STEP * dIndex) / dMaxIndex;
        float volume =0.0;
        if (vol == 0) {
            volume = vol;
        } else {    // map volume value to custom volume
            float unitstep = fCUSTOM_VOLUME_MAPPING_STEP/dMaxIndex;
            if (vol < fCUSTOM_VOLUME_MAPPING_STEP/dMaxIndex) {
                volume = array[0];
            } else {
                int Index = (vol+0.5)/unitstep;
                vol -= (Index*unitstep);
                float Remind = (1.0 - (float)vol/unitstep);
                if (Index != 0) {
                    volume = ((array[Index]  - (array[Index] - array[Index-1]) * Remind)+0.5);
                } else {
                    volume = 0;
                }
            }
            // -----clamp for volume
            if ( volume > 253.0) {
                volume = fCUSTOM_VOLUME_MAPPING_STEP;
            } else if ( volume <= array[0]) {
                volume = array[0];
            }
        }

#ifndef MTK_AUDIO_GAIN_TABLE
        fFMVolume = AudioMTKVolumeController::linearToLog(volume);
#else
        fFMVolume = AudioMTKGainController::linearToLog(volume);
#endif

    }
    ALOGD("Final fFMVolume %f",fFMVolume);
    return fFMVolume;
}

#endif

