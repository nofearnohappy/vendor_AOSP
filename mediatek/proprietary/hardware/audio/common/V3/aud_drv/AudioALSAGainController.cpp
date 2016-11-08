
#define LOG_TAG "AudioMTKGainController"
#include <math.h>
#include "audio_custom_exp.h"
#include <media/AudioSystem.h>
#include "SpeechDriverFactory.h"
#include "AudioALSAGainController.h"
#include "AudioALSASpeechPhoneCallController.h"
#include "SpeechEnhancementController.h"
//#include "AudioAMPControlInterface.h"
#include "AudioALSADriverUtility.h"
#include "AudioALSAStreamManager.h"
#include "AudioALSAHardwareResourceManager.h"

#ifdef MTK_BASIC_PACKAGE
#include "AudioTypeExt.h"
#endif

#define _countof(_Array) (sizeof(_Array) / sizeof(_Array[0]))

namespace android
{
AudioMTKGainController *AudioMTKGainController::UniqueVolumeInstance = NULL;

// here can change to match audiosystem

// total 64 dB
static const float keydBPerStep = 0.25f;
static const float keyvolumeStep = 255.0f;


// shouldn't need to touch these
static const float keydBConvert = -keydBPerStep * 2.302585093f / 20.0f;
static const float keydBConvertInverse = 1.0f / keydBConvert;

//static const char *PGA_Gain_String[] = {"0Db", "6Db", "12Db" , "18Db" , "24Db" , "30Db"};

//hw spec db
//const int keyAudioBufferStep       =   19;
//const int KeyAudioBufferGain[]     =  { -5, -3, -1, 1, 3, 5, 7, 9};
//const int KeyAudioBufferGain[]     =  { 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -40}; // move to xml
/*static const char *DL_PGA_Headset_GAIN[] = {"8Db", "7Db", "6Db", "5Db", "4Db", "3Db", "2Db", "1Db", "0Db", "-1Db", "-2Db", "-3Db",
                                            "-4Db", "-5Db", "-6Db", "-7Db", "-8Db", "-9Db", "-10Db" , "-40Db"
                                           };*/

//const int keyVoiceBufferStep       =   19;
//const int KeyVoiceBufferGain[]     =  { -21, -19, -17, -15, -13, -11, -9, -7, -5, -3, -1, 1, 3, 5, 7, 9};
//const int KeyVoiceBufferGain[]     =  { 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -40}; move to xml
/*static const char *DL_PGA_Handset_GAIN[] = {"8Db", "7Db", "6Db", "5Db", "4Db", "3Db", "2Db", "1Db", "0Db", "-1Db", "-2Db", "-3Db",
                                            "-4Db", "-5Db", "-6Db", "-7Db", "-8Db", "-9Db", "-10Db" , "-40Db"
                                           };*/


//const int keyULStep                =   5;
//const int KeyULGain[]              =  { -6, 0, 6, 12, 18, 24};
//const int keyULGainOffset          = 2;

//const int keySPKStep               =   15;
//const int KeySPKgain[]             =  { -60, 0, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
/*static const char *DL_PGA_SPEAKER_GAIN[] = {"MUTE", "0Db", "4Db", "5Db", "6Db", "7Db", "8Db", "9Db", "10Db",  // move to xml
                                            "11Db", "12Db", "13Db", "14Db", "15Db", "16Db", "17Db"
                                           };*/



//const int keyDLDigitalDegradeMax   = 63;

//const int keyULDigitalIncreaseMax  = 32;

// static const int keySidetoneSize   = 47; // move to xml

/*static const uint16_t SwAgc_Gain_Map[AUDIO_SYSTEM_UL_GAIN_MAX + 1] =  // move to xml
{
    25, 24, 23, 22, 21, 20, 19, 18, 17,
    16, 15, 14, 13, 12 , 11,
    16, 15, 14, 13, 12 , 11,
    16, 15, 14, 13, 12 , 11,
    16, 15, 14, 13, 12 , 11,
    16, 15, 14, 13, 12 , 11,
    10, 9, 8, 7, 6, 5, 4
};

static const uint16_t PGA_Gain_Map[AUDIO_SYSTEM_UL_GAIN_MAX + 1] =
{
    6, 6, 6, 6, 6, 6, 6, 6, 6,
    6, 6, 6, 6, 6, 6,
    12, 12, 12, 12, 12, 12,
    18, 18, 18, 18, 18, 18,
    24, 24, 24, 24, 24, 24,
    30, 30, 30, 30, 30, 30,
    30, 30, 30, 30, 30, 30, 30
};*/

//static const uint16_t keySideTone[] =     // move to xml
//{
//    32767, 29204, 26027, 23196, 20674, 18426, 16422, 14636, 13044, 11625,  /*1dB per step*/
//    10361, 9234,  8230,  7335,  6537,  5826,  5193,  4628,  4125,  3676,
//    3276,  2919,  2602,  2319,  2066,  1841,  1641,  1463,  1304,  1162,
//    1035,  923,   822,   733,   653,   582,   519,   462,   412,   367,
//    327,   291,   260,   231,   206,   183,   163,   145
//};

// HW Gain mappring
static const uint32_t kHWGainMap[] =
{
    0x00000, //   0, -64.0 dB (mute)
    0x0015E, //   1, -63.5 dB
    0x00173, //   2, -63.0 dB
    0x00189, //   3, -62.5 dB
    0x001A0, //   4, -62.0 dB
    0x001B9, //   5, -61.5 dB
    0x001D3, //   6, -61.0 dB
    0x001EE, //   7, -60.5 dB
    0x0020C, //   8, -60.0 dB
    0x0022B, //   9, -59.5 dB
    0x0024C, //  10, -59.0 dB
    0x0026F, //  11, -58.5 dB
    0x00294, //  12, -58.0 dB
    0x002BB, //  13, -57.5 dB
    0x002E4, //  14, -57.0 dB
    0x00310, //  15, -56.5 dB
    0x0033E, //  16, -56.0 dB
    0x00370, //  17, -55.5 dB
    0x003A4, //  18, -55.0 dB
    0x003DB, //  19, -54.5 dB
    0x00416, //  20, -54.0 dB
    0x00454, //  21, -53.5 dB
    0x00495, //  22, -53.0 dB
    0x004DB, //  23, -52.5 dB
    0x00524, //  24, -52.0 dB
    0x00572, //  25, -51.5 dB
    0x005C5, //  26, -51.0 dB
    0x0061D, //  27, -50.5 dB
    0x00679, //  28, -50.0 dB
    0x006DC, //  29, -49.5 dB
    0x00744, //  30, -49.0 dB
    0x007B2, //  31, -48.5 dB
    0x00827, //  32, -48.0 dB
    0x008A2, //  33, -47.5 dB
    0x00925, //  34, -47.0 dB
    0x009B0, //  35, -46.5 dB
    0x00A43, //  36, -46.0 dB
    0x00ADF, //  37, -45.5 dB
    0x00B84, //  38, -45.0 dB
    0x00C32, //  39, -44.5 dB
    0x00CEC, //  40, -44.0 dB
    0x00DB0, //  41, -43.5 dB
    0x00E7F, //  42, -43.0 dB
    0x00F5B, //  43, -42.5 dB
    0x01044, //  44, -42.0 dB
    0x0113B, //  45, -41.5 dB
    0x01240, //  46, -41.0 dB
    0x01355, //  47, -40.5 dB
    0x0147A, //  48, -40.0 dB
    0x015B1, //  49, -39.5 dB
    0x016FA, //  50, -39.0 dB
    0x01857, //  51, -38.5 dB
    0x019C8, //  52, -38.0 dB
    0x01B4F, //  53, -37.5 dB
    0x01CED, //  54, -37.0 dB
    0x01EA4, //  55, -36.5 dB
    0x02075, //  56, -36.0 dB
    0x02261, //  57, -35.5 dB
    0x0246B, //  58, -35.0 dB
    0x02693, //  59, -34.5 dB
    0x028DC, //  60, -34.0 dB
    0x02B48, //  61, -33.5 dB
    0x02DD9, //  62, -33.0 dB
    0x03090, //  63, -32.5 dB
    0x03371, //  64, -32.0 dB
    0x0367D, //  65, -31.5 dB
    0x039B8, //  66, -31.0 dB
    0x03D24, //  67, -30.5 dB
    0x040C3, //  68, -30.0 dB
    0x04499, //  69, -29.5 dB
    0x048AA, //  70, -29.0 dB
    0x04CF8, //  71, -28.5 dB
    0x05188, //  72, -28.0 dB
    0x0565D, //  73, -27.5 dB
    0x05B7B, //  74, -27.0 dB
    0x060E6, //  75, -26.5 dB
    0x066A4, //  76, -26.0 dB
    0x06CB9, //  77, -25.5 dB
    0x0732A, //  78, -25.0 dB
    0x079FD, //  79, -24.5 dB
    0x08138, //  80, -24.0 dB
    0x088E0, //  81, -23.5 dB
    0x090FC, //  82, -23.0 dB
    0x09994, //  83, -22.5 dB
    0x0A2AD, //  84, -22.0 dB
    0x0AC51, //  85, -21.5 dB
    0x0B687, //  86, -21.0 dB
    0x0C157, //  87, -20.5 dB
    0x0CCCC, //  88, -20.0 dB
    0x0D8EF, //  89, -19.5 dB
    0x0E5CA, //  90, -19.0 dB
    0x0F367, //  91, -18.5 dB
    0x101D3, //  92, -18.0 dB
    0x1111A, //  93, -17.5 dB
    0x12149, //  94, -17.0 dB
    0x1326D, //  95, -16.5 dB
    0x14496, //  96, -16.0 dB
    0x157D1, //  97, -15.5 dB
    0x16C31, //  98, -15.0 dB
    0x181C5, //  99, -14.5 dB
    0x198A1, // 100, -14.0 dB
    0x1B0D7, // 101, -13.5 dB
    0x1CA7D, // 102, -13.0 dB
    0x1E5A8, // 103, -12.5 dB
    0x2026F, // 104, -12.0 dB
    0x220EA, // 105, -11.5 dB
    0x24134, // 106, -11.0 dB
    0x26368, // 107, -10.5 dB
    0x287A2, // 108, -10.0 dB
    0x2AE02, // 109,  -9.5 dB
    0x2D6A8, // 110,  -9.0 dB
    0x301B7, // 111,  -8.5 dB
    0x32F52, // 112,  -8.0 dB
    0x35FA2, // 113,  -7.5 dB
    0x392CE, // 114,  -7.0 dB
    0x3C903, // 115,  -6.5 dB
    0x4026E, // 116,  -6.0 dB
    0x43F40, // 117,  -5.5 dB
    0x47FAC, // 118,  -5.0 dB
    0x4C3EA, // 119,  -4.5 dB
    0x50C33, // 120,  -4.0 dB
    0x558C4, // 121,  -3.5 dB
    0x5A9DF, // 122,  -3.0 dB
    0x5FFC8, // 123,  -2.5 dB
    0x65AC8, // 124,  -2.0 dB
    0x6BB2D, // 125,  -1.5 dB
    0x72148, // 126,  -1.0 dB
    0x78D6F, // 127,  -0.5 dB
    0x80000, // 128,   0.0 dB
};

// callback function
void xmlChangedCallback(AppHandle *_appHandle, const char *_audioTypeName)
{
    // reload XML file
    if (appHandleReloadAudioType(_appHandle, _audioTypeName) == APP_ERROR)
    {
        ALOGE("%s(), Reload xml fail!(audioType = %s)", __FUNCTION__, _audioTypeName);
    }
    else
    {
        AudioMTKGainController::getInstance()->updateXmlParam(_audioTypeName);
    }
}

float AudioMTKGainController::linearToLog(int volume)
{
    //ALOGD("linearToLog(%d)=%f", volume, v);
    return volume ? exp(float(keyvolumeStep - volume) * keydBConvert) : 0;
}

int AudioMTKGainController::logToLinear(float volume)
{
    //ALOGD("logTolinear(%d)=%f", v, volume);
    return volume ? keyvolumeStep - int(keydBConvertInverse * log(volume) + 0.5) : 0;
}

AudioMTKGainController *AudioMTKGainController::getInstance()
{
    static AudioLock mGetInstanceLock;
    AudioAutoTimeoutLock _l(mGetInstanceLock);

    if (UniqueVolumeInstance == 0)
    {
        ALOGV("+UniqueVolumeInstance\n");
        UniqueVolumeInstance = new AudioMTKGainController();
        ALOGV("-UniqueVolumeInstance\n");
    }

    return UniqueVolumeInstance;
}

AudioMTKGainController::AudioMTKGainController()
{
    ALOGD("AudioMTKGainController contructor");
//    mAmpControl          = AudioDeviceManger::createInstance();
    mAudioSpeechEnhanceInfoInstance = AudioSpeechEnhanceInfo::getInstance();
    mHardwareResourceManager = AudioALSAHardwareResourceManager::getInstance();
    mVoiceVolume = 1.0f;
    mMasterVolume = 1.0f;
    //mFmVolume = 0xFF;
    //mFmChipVolume = 0xFFFFFFFF;
    mSpeechNB = true;
    mSupportBtVol = false;
    memset(&mHwVolume, 0xFF, sizeof(mHwVolume));
    memset(&mHwStream, 0xFF, sizeof(mHwStream));
    memset(&mHwCaptureInfo, 0, sizeof(mHwCaptureInfo));
    initVolumeController();
    mULTotalGain = 184;
#ifdef MTK_NEW_VOL_CONTROL
    mHpImpedanceIdx = mSpec.hpImpDefaultIdx;
#endif
#ifdef MTK_AUDIO_SW_DRE
    mSWDREMute = false;
    mMutedHandlerVector.clear();
    mHasMuteHandler = false;
    mNumHandler = 0;
#endif
    mInitDone = true;
    mMixer = NULL;
    mMixer = AudioALSADriverUtility::getInstance()->getMixer();
    ALOGD("mMixer = %p", mMixer);
    ASSERT(mMixer != NULL);

    /* XML changed callback process */
    appHandleRegXmlChangedCb(appHandleGetInstance(), xmlChangedCallback);
}

status_t AudioMTKGainController::initVolumeController()
{
#ifdef MTK_NEW_VOL_CONTROL
    GainTableParamParser::getInstance()->getGainTableParam(&mGainTable);
    GainTableParamParser::getInstance()->getGainTableSpec(&mSpec);

    initUlTotalGain();
#endif
    return NO_ERROR;
}

status_t AudioMTKGainController::initUlTotalGain()
{
    for (int mode = 0; mode < NUM_GAIN_MIC_MODE; mode++)
    {
        for (int device = 0; device < NUM_GAIN_DEVICE; device++)
        {
            unsigned char gain = mGainTable.micGain[mode][device].gain;
            if (gain > mSpec.micIdxMax)
            {
                gain = mSpec.micIdxMax;
            }
            mULTotalGainTable[mode][device] = UPLINK_GAIN_MAX - ((mSpec.micIdxMax - gain) * UPLINK_ONEDB_STEP);
        }
    }

    return NO_ERROR;
}

status_t AudioMTKGainController::initCheck()
{
    return mInitDone;
}

void AudioMTKGainController::updateXmlParam(const char *_audioTypeName)
{
    ALOGD("%s(), audioType = %s", __FUNCTION__, _audioTypeName);

    bool needResetDlGain = false;
    bool isMicGainChanged = false;

    if (strcmp(_audioTypeName, PLAY_DIGI_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->updatePlaybackDigitalGain(&mGainTable);
    }
    else if (strcmp(_audioTypeName, PLAY_ANA_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->updatePlaybackAnalogGain(&mGainTable);
        needResetDlGain = true;
    }
    else if (strcmp(_audioTypeName, SPEECH_VOL_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->updateSpeechVol(&mGainTable);
        isMicGainChanged = true;
        needResetDlGain = true;
    }
    else if (strcmp(_audioTypeName, REC_VOL_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->updateRecordVol(&mGainTable);
        isMicGainChanged = true;
    }
    else if (strcmp(_audioTypeName, VOIP_VOL_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->updateVoIPVol(&mGainTable);
        isMicGainChanged = true;
        needResetDlGain = true;
    }
    else if (strcmp(_audioTypeName, VOLUME_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->loadGainTableSpec();
        GainTableParamParser::getInstance()->getGainTableParam(&mGainTable);
        GainTableParamParser::getInstance()->getGainTableSpec(&mSpec);
        isMicGainChanged = true;
        needResetDlGain = true;
    }
    else if (strcmp(_audioTypeName, GAIN_MAP_AUDIOTYPE_NAME) == 0)
    {
        GainTableParamParser::getInstance()->loadGainTableMapDl();
        GainTableParamParser::getInstance()->getGainTableParam(&mGainTable);
        isMicGainChanged = true;
        needResetDlGain = true;
    }

    // reset mic gain immediately
    if (isMicGainChanged)
    {
        initUlTotalGain();

        if (!isInVoiceCall(mHwStream.mode))
            SetCaptureGain(mHwCaptureInfo.mode, mHwCaptureInfo.source, mHwCaptureInfo.input_device, mHwCaptureInfo.output_devices);
    }

    if (needResetDlGain)
        setAnalogVolume_l(mHwStream.stream,
                          mHwStream.devices,
                          mHwStream.index,
                          mHwStream.mode);
}

status_t AudioMTKGainController::SetCaptureGain(audio_mode_t mode, audio_source_t source, audio_devices_t input_device, audio_devices_t output_devices)
{
    ALOGD("+%s(), mode=%d, source=%d, input device=0x%x, output device=0x%x", __FUNCTION__, mode, source, input_device, output_devices);

    mHwCaptureInfo.mode = mode;
    mHwCaptureInfo.source = source;
    mHwCaptureInfo.input_device = input_device;
    mHwCaptureInfo.output_devices = output_devices;

    switch (mode)
    {
        case AUDIO_MODE_NORMAL:
        case AUDIO_MODE_RINGTONE:
        {
            switch (source)
            {
                case AUDIO_SOURCE_CAMCORDER:
                case AUDIO_SOURCE_VOICE_RECOGNITION:
                case AUDIO_SOURCE_VOICE_UNLOCK:
                case AUDIO_SOURCE_CUSTOMIZATION1:
                case AUDIO_SOURCE_CUSTOMIZATION2:
                case AUDIO_SOURCE_CUSTOMIZATION3:
                    ApplyMicGain(getGainMicMode(source, mode),
                                 getGainDevice(input_device),
                                 mode);
                    break;
                default:
                    //for audio tuning tool tuning case.
                    if (mAudioSpeechEnhanceInfoInstance->IsAPDMNRTuningEnable())    //for DMNR tuning
                    {
                        if (mAudioSpeechEnhanceInfoInstance->GetAPTuningMode() == HANDSFREE_MODE_DMNR)
                        {
                            ApplyMicGain(GAIN_MIC_VOICE_CALL_NB, GAIN_DEVICE_SPEAKER, mode);
                        }
                        else if (mAudioSpeechEnhanceInfoInstance->GetAPTuningMode() == NORMAL_MODE_DMNR)
                        {
                            ApplyMicGain(GAIN_MIC_VOICE_CALL_NB, GAIN_DEVICE_EARPIECE, mode);
                        }
                        else
                        {
                            ApplyMicGain(GAIN_MIC_NORMAL, GAIN_DEVICE_EARPIECE, mode);
                        }
                    }
                    else
                    {
                        if (input_device == AUDIO_DEVICE_IN_WIRED_HEADSET)
                        {
                            ApplyMicGain(GAIN_MIC_NORMAL, GAIN_DEVICE_HEADSET, mode);
                        }
                        else
                        {
                            ApplyMicGain(GAIN_MIC_NORMAL, GAIN_DEVICE_EARPIECE, mode);
                        }
                    }
                    break;
            }
            break;
        }
        case AUDIO_MODE_IN_CALL:
        case AUDIO_MODE_IN_CALL_2:
        case AUDIO_MODE_IN_CALL_EXTERNAL:
        {
            if (AudioALSASpeechPhoneCallController::getInstance()->checkTtyNeedOn() == false)
            {
                ApplyMicGain(getGainMicMode(source, mode),
                             getGainDevice(output_devices),
                             mode);
            }
            break;
        }
        case AUDIO_MODE_IN_COMMUNICATION:
        {
            ApplyMicGain(getGainMicMode(source, mode),
                         getGainDevice(output_devices),
                         mode);
            break;
        }
        default:
        {
            break;
        }
    }
    return NO_ERROR;
}


status_t AudioMTKGainController::speechBandChange(bool nb)
{
    ALOGD("speechBandChange nb %d", nb);

    AutoMutex lock(mLock);
    if (mSpeechNB != nb)
    {
        mSpeechNB = nb;
        setAnalogVolume_l(mHwStream.stream, mHwStream.devices, mHwStream.index, AUDIO_MODE_IN_CALL);
    }
    return NO_ERROR;
}

bool AudioMTKGainController::isNbSpeechBand(void)
{
    AutoMutex lock(mLock);
    return mSpeechNB;
}

status_t AudioMTKGainController::setBtVolumeCapability(bool support)
{
    AutoMutex lock(mLock);
    mSupportBtVol = !support; // if bt device do not support volume , we should
    return NO_ERROR;
}

status_t AudioMTKGainController::setAnalogVolume_l(int stream, int devices, int index, audio_mode_t mode)
{
    ALOGD("setAnalogVolume(), stream %d, devices 0x%x, index %d, mode %d", stream, devices, index, mode);
    mHwStream.stream = stream;
    mHwStream.devices = devices;
    mHwStream.index  = index;
    mHwStream.mode = mode;
    if (isInVoiceCall(mode))
    {
        setVoiceVolume(index, devices, mode);
    }
    else
    {
        setNormalVolume(stream, index, devices, mode);
    }

    return NO_ERROR;
}

status_t AudioMTKGainController::setAnalogVolume(int stream, int devices, int index, audio_mode_t mode)
{
    AutoMutex lock(mLock);
    return setAnalogVolume_l(stream, devices, index, mode);
}

status_t AudioMTKGainController::setNormalVolume(int stream, int index, int devices, audio_mode_t mode)
{
    ALOGD("setNormalVolume(), stream %d, devices 0x%x, index %d, mode 0x%x", stream, devices, index, mode);
    // get gain device
    GAIN_DEVICE gainDevice = getGainDevice(devices);

    // check stream/index range
    if (!isValidStreamType((audio_stream_type_t)stream))
    {
        ALOGW("error, stream %d is invalid, use %d instead", stream, AUDIO_STREAM_MUSIC);
        stream = AUDIO_STREAM_MUSIC;
    }
    if (!isValidVolIdx(index, mode))
    {
        ALOGW("error, index %d is invalid, use max %d instead", index, GAIN_MAX_VOL_INDEX);
        index = GAIN_MAX_VOL_INDEX;
    }

    if (isSpeakerCategory(gainDevice))
    {
        unsigned char gain = mGainTable.streamGain[stream][gainDevice][index].analog[mSpec.spkAnaType];
        setSpeakerGain(gain);
    }

    if (isHeadsetCategory(gainDevice))
    {
        unsigned char gain = mGainTable.streamGain[stream][gainDevice][index].analog[GAIN_ANA_HEADPHONE];
        ApplyAudioGain(gain, mode, gainDevice);
//        setAMPGain(streamGain->stream[gainDevice].amp, AMP_CONTROL_POINT, devices);
    }

    if (isEarpieceCategory(gainDevice))
    {
        unsigned char gain = mGainTable.streamGain[stream][gainDevice][index].analog[GAIN_ANA_HANDSET];
        ApplyAudioGain(gain, mode, gainDevice);
    }

    return NO_ERROR;
}

status_t AudioMTKGainController::setVoiceVolume(int index, int devices, audio_mode_t mode)
{
    ALOGD("setVoiceVolume index %d, devices 0x%x, mode %d, mSpeechNB %d", index, devices, mode, mSpeechNB);

    bool force_mute = false;

    // for voice, index will be 0~7
    if (index == 0) { // index == 0, means mute
        force_mute = true;
    } else {
        // index will be 1~7, minus 1 to map gain table index 0 ~ 6 in xml
        index--;
    }

    // check stream/index range
    if (!isValidVolIdx(index, mode))
    {
        ALOGW("error, index %d is invalid, use max %d instead", index, GAIN_MAX_SPEECH_VOL_INDEX);
        index = GAIN_MAX_SPEECH_VOL_INDEX;
    }


    if (audio_is_bluetooth_sco_device(devices))  // TODO: KC: this is weird
    {
#ifdef MTK_AUDIO_GAIN_TABLE_BT
        // BT Headset NREC
/*        bool bt_headset_nrec_on =  SpeechEnhancementController::GetInstance()->GetBtHeadsetNrecStatus();
        if (bt_headset_nrec_on)
        {
            streamGain =  &gainTable->blueToothSco.stream[gainDevice];
        }
        else
        {
//            streamGain =  &gainTableBT->blueToothNrec.stream[gainDevice];
        }
*/
#else
//        digitalDegradeDb =  mGainTable.streamGain[AUDIO_STREAM_BLUETOOTH_SCO][GAIN_DEVICE_HEADSET][index].digital;

#endif
        ALOGD("audio_is_bluetooth_sco_device = %d, mSupportBtVol is %d", true, mSupportBtVol);
        uint8_t digitalDegradeDb = 0;
        if (mSupportBtVol)
        {
            digitalDegradeDb = mGainTable.streamGain[AUDIO_STREAM_BLUETOOTH_SCO][GAIN_DEVICE_HEADSET][index].digital;
        }
        else
        {
            digitalDegradeDb = mGainTable.streamGain[AUDIO_STREAM_BLUETOOTH_SCO][GAIN_DEVICE_HEADSET][GAIN_MAX_VOL_INDEX].digital;
        }
        ApplyMdDlGain(digitalDegradeDb);  // modem dl gain
        ApplyMdUlGain(0);

        return NO_ERROR;
    }
    else
    {
    // get gain device
    GAIN_DEVICE gainDevice = getGainDevice(devices);
    GAIN_SPEECH_BAND band = mSpeechNB ? GAIN_SPEECH_NB : GAIN_SPEECH_WB;

#ifdef MTK_HAC_SUPPORT
    // change gain device if HAC On
    if (gainDevice == GAIN_DEVICE_EARPIECE)
    {
        bool mHACon = SpeechEnhancementController::GetInstance()->GetHACOn();
        if (mHACon)
        {
            ALOGD("%s(): HAC ON = %d, gain device change from EarPiece to HAC", __FUNCTION__, mHACon);
            gainDevice = GAIN_DEVICE_HAC;
        }
    }
#endif
    // TODO: KC: change gain device if HS is 5pole

    // set analog gain
    if (isSpeakerCategory(gainDevice))
    {
        unsigned char gain = mGainTable.speechGain[band][gainDevice][index].analog[mSpec.spkAnaType];
        setSpeakerGain(gain);
    }

    if (isHeadsetCategory(gainDevice))
    {
        unsigned char gain = mGainTable.speechGain[band][gainDevice][index].analog[GAIN_ANA_HEADPHONE];
        ApplyAudioGain(gain, mode, gainDevice);
    }

    if (isEarpieceCategory(gainDevice))
    {
        unsigned char gain = mGainTable.speechGain[band][gainDevice][index].analog[GAIN_ANA_HANDSET];
        ApplyAudioGain(gain, mode, gainDevice);
    }

    // set digital gain
    if (force_mute)
    {
        ApplyMdDlGain(255); // mute
    }
    else
    {
        //setAMPGain(ampgain, AMP_CONTROL_POINT, device);
        uint8_t digitalDegradeDb = mGainTable.speechGain[band][gainDevice][index].digital;
        ApplyMdDlGain(digitalDegradeDb);  // modem dl gain
    }

    // mic gain & modem UL gain
    ApplyMicGainByDevice(devices, mode);
    ApplySideTone(gainDevice);
    }

    return NO_ERROR;
}

void  AudioMTKGainController::ApplyMicGainByDevice(uint32_t device, audio_mode_t mode)
{
    if (device & AUDIO_DEVICE_OUT_EARPIECE ||
        device & AUDIO_DEVICE_OUT_WIRED_HEADSET ||
        device & AUDIO_DEVICE_OUT_WIRED_HEADPHONE ||
        device & AUDIO_DEVICE_OUT_SPEAKER)
    {
        GAIN_MIC_MODE micMode = getGainMicMode(AUDIO_SOURCE_DEFAULT, mode);
        GAIN_DEVICE gainDevice = getGainDevice(device);

        ApplyMicGain(micMode, gainDevice, mode);
    }
    else if ((device & AUDIO_DEVICE_OUT_BLUETOOTH_SCO) || (device & AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET) || (device & AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET))
    {
        //when use BT_SCO , apply digital to 0db.
        ApplyMdUlGain(0);
    }
}

GAIN_DEVICE AudioMTKGainController::getGainDevice(audio_devices_t devices)
{
    GAIN_DEVICE gainDevice = GAIN_DEVICE_NONE;

    if (devices & AUDIO_DEVICE_BIT_IN)
    {   /* input device */
        if (devices & AUDIO_DEVICE_IN_WIRED_HEADSET)
        {
            gainDevice = GAIN_DEVICE_HEADSET;
        }
        else if (devices & AUDIO_DEVICE_IN_BUILTIN_MIC)
        {
            gainDevice = GAIN_DEVICE_SPEAKER;
        }
        else
        {
            ALOGE("%s(), error, devices (0x%x) not support, return GAIN_DEVICE_SPEAKER", __FUNCTION__, devices);
            gainDevice = GAIN_DEVICE_SPEAKER;
        }
    }
    else
    {   /* output device */
        if (devices & AUDIO_DEVICE_OUT_SPEAKER)
        {
            gainDevice = GAIN_DEVICE_SPEAKER;
            if ((devices & AUDIO_DEVICE_OUT_WIRED_HEADSET)||
                (devices & AUDIO_DEVICE_OUT_WIRED_HEADPHONE))
                gainDevice = GAIN_DEVICE_HSSPK;
        }
        else if (devices & AUDIO_DEVICE_OUT_WIRED_HEADSET)
        {
            if (mHardwareResourceManager->getNumOfHeadsetPole() == 5)
                gainDevice = GAIN_DEVICE_HEADSET_5POLE;
            else
                gainDevice = GAIN_DEVICE_HEADSET;
        }
        else if (devices & AUDIO_DEVICE_OUT_WIRED_HEADPHONE)
        {
            gainDevice = GAIN_DEVICE_HEADSET;
        }
        else if (devices & AUDIO_DEVICE_OUT_EARPIECE)
        {
            gainDevice = GAIN_DEVICE_EARPIECE ;
        }
        else
        {
            ALOGE("%s(), error, devices (%d) not support, return GAIN_DEVICE_SPEAKER", __FUNCTION__, devices);
            gainDevice = GAIN_DEVICE_SPEAKER;
        }
    }

    ALOGD("%s(), input devices = 0x%x, return gainDevice = %d", __FUNCTION__, devices, gainDevice);
    return gainDevice;
}

GAIN_MIC_MODE AudioMTKGainController::getGainMicMode(audio_source_t _source, audio_mode_t _mode)
{
    GAIN_MIC_MODE micMode = GAIN_MIC_NORMAL;

    switch (_mode)
    {
        case AUDIO_MODE_NORMAL:
        case AUDIO_MODE_RINGTONE:
            switch (_source)
            {
                case AUDIO_SOURCE_MIC:
                    micMode = GAIN_MIC_NORMAL;
                    break;
                case AUDIO_SOURCE_VOICE_UPLINK:
                case AUDIO_SOURCE_VOICE_DOWNLINK:
                case AUDIO_SOURCE_VOICE_CALL:
                    micMode = mSpeechNB ? GAIN_MIC_VOICE_CALL_NB : GAIN_MIC_VOICE_CALL_WB;
                    break;
                case AUDIO_SOURCE_CAMCORDER:
                    micMode = GAIN_MIC_CAMCORDER;
                    break;
                case AUDIO_SOURCE_VOICE_RECOGNITION:
                    micMode = GAIN_MIC_VOICE_RECOGNITION;
                    break;
                case AUDIO_SOURCE_VOICE_COMMUNICATION:
                    micMode = GAIN_MIC_VOICE_COMMUNICATION;
                    break;
                case AUDIO_SOURCE_VOICE_UNLOCK:
                    micMode = GAIN_MIC_VOICE_UNLOCK;
                    break;
                case AUDIO_SOURCE_CUSTOMIZATION1:
                    micMode = GAIN_MIC_CUSTOMIZATION1;
                    break;
                case AUDIO_SOURCE_CUSTOMIZATION2:
                    micMode = GAIN_MIC_CUSTOMIZATION2;
                    break;
                case AUDIO_SOURCE_CUSTOMIZATION3:
                    micMode = GAIN_MIC_CUSTOMIZATION3;
                    break;
                default:
                    micMode = GAIN_MIC_NORMAL;
                    break;
            }
            break;
        case AUDIO_MODE_IN_CALL:
        case AUDIO_MODE_IN_CALL_2:
        case AUDIO_MODE_IN_CALL_EXTERNAL:
            micMode = mSpeechNB ? GAIN_MIC_VOICE_CALL_NB : GAIN_MIC_VOICE_CALL_WB;
            break;
        case AUDIO_MODE_IN_COMMUNICATION:
            micMode = GAIN_MIC_VOICE_COMMUNICATION;
            break;
        default:
            ALOGE("%s(), not handled mode %d", __FUNCTION__, _mode);
            micMode = GAIN_MIC_NORMAL;
            break;
    }
    return micMode;
}

void AudioMTKGainController::ApplyMdDlGain(int32_t degradeDb)
{
    // set degarde db to mode side, DL part, here use degrade dbg
    ALOGD("ApplyMdDlGain degradeDb = %d", degradeDb);
#if 0
    if (degradeDb >= keyDLDigitalDegradeMax)
    {
        degradeDb = keyDLDigitalDegradeMax;
    }

    SpeechDriverFactory::GetInstance()->GetSpeechDriver()->SetDownlinkGain((-1 * degradeDb) << 2); // degrade db * 4
#endif
    SpeechDriverFactory::GetInstance()->GetSpeechDriver()->SetDownlinkGain((-1 * degradeDb));
}

void AudioMTKGainController::ApplyMdDlEhn1Gain(int32_t Gain)
{
    // set degarde db to mode side, DL part, here use degrade dbg
    ALOGD("ApplyMdDlEhn1Gain degradeDb = %d", Gain);
    //SpeechDriverFactory::GetInstance()->GetSpeechDriver()->SetEnh1DownlinkGain(-1 * (Gain) << 2); // degrade db * 4
    SpeechDriverFactory::GetInstance()->GetSpeechDriver()->SetEnh1DownlinkGain(-1 * (Gain));
}

void AudioMTKGainController::ApplyMdUlGain(int32_t IncreaseDb)
{
    // set degarde db to mode side, UL part, here use positive gain becasue SW_agc always positive
    ALOGD("ApplyMdUlGain degradeDb = %d", IncreaseDb);

    //if (mHwVolume.swAgc != IncreaseDb)
    {
        mHwVolume.swAgc = IncreaseDb;
        SpeechDriverFactory::GetInstance()->GetSpeechDriver()->SetUplinkGain(IncreaseDb << 2); // degrade db * 4
    }
}


void AudioMTKGainController::ApplyAudioGain(int gain, audio_mode_t mode, GAIN_DEVICE gainDevice)
{
    ALOGD("ApplyAudioGain  gain = %d mode= %d gainDevice = %d", gain, mode, gainDevice);
    int bufferGain = gain;

#ifdef MTK_NEW_VOL_CONTROL
    if (bufferGain >= mSpec.bufferGainString.size())
    {
        bufferGain = mSpec.bufferGainString.size() - 1;
    }

    // adjust gain according to master volume
    bufferGain = tuneGainForMasterVolume(bufferGain, mode, gainDevice);

    // adjust gain according to hp impedance when using HP
    bufferGain = tuneGainForHpImpedance(bufferGain, mode, gainDevice);
#endif

    if (isEarpieceCategory(gainDevice))
    {
        setVoiceBufferGain(bufferGain);
    }
    else if (isHeadsetCategory(gainDevice))
    {
        setAudioBufferGain(bufferGain);
    }
}

int AudioMTKGainController::GetReceiverGain(void)
{
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    unsigned int num_values, i ;
    int index;
    ALOGD("GetReceiverGain");
    ctl = mixer_get_ctl_by_name(mMixer, "Handset_PGA_GAIN");
    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    for (i = 0; i < num_values; i++)
    {
        index = mixer_ctl_get_value(ctl, i);
        ALOGD("GetReceiverGain i = %d index = %d ", i , index);
    }
    return index;
}

int AudioMTKGainController::GetHeadphoneRGain(void)
{
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    unsigned int num_values, i ;
    int index;
    ALOGD("GetHeadphoneRGain");
    ctl = mixer_get_ctl_by_name(mMixer, "Headset_PGAR_GAIN");
    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    for (i = 0; i < num_values; i++)
    {
        index = mixer_ctl_get_value(ctl, i);
        ALOGD("GetHeadphoneRGain i = %d index = %d ", i , index);
    }
    return index;
}


int AudioMTKGainController::GetHeadphoneLGain(void)
{
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    unsigned int num_values, i ;
    int index;
    ALOGD("GetHeadphoneLGain");
    ctl = mixer_get_ctl_by_name(mMixer, "Headset_PGAL_GAIN");
    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    for (i = 0; i < num_values; i++)
    {
        index = mixer_ctl_get_value(ctl, i);
        ALOGD("GetHeadphoneLGain i = %d index = %d ", i , index);
    }
    return index;
}

void AudioMTKGainController::SetReceiverGain(int index)
{
    ALOGD("SetReceiverGain = %d", index);
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    ctl = mixer_get_ctl_by_name(mMixer, "Handset_PGA_GAIN");
    type = mixer_ctl_get_type(ctl);

    if (index < 0)
        index = 0;

    if ((uint32_t)index >= mSpec.bufferGainString.size())
        index = mSpec.bufferGainString.size() - 1;

    if (mixer_ctl_set_enum_by_string(ctl, mSpec.bufferGainString[index].c_str()))
    {
        ALOGE("Error: Handset_PGA_GAIN invalid value");
    }
}

void AudioMTKGainController::SetHeadPhoneLGain(int index)
{
    ALOGD("SetHeadPhoneLGain = %d", index);
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    ctl = mixer_get_ctl_by_name(mMixer, "Headset_PGAL_GAIN");
    type = mixer_ctl_get_type(ctl);

    if (index < 0)
        index = 0;

    if ((uint32_t)index >= mSpec.bufferGainString.size())
        index = mSpec.bufferGainString.size() - 1;

    if (mixer_ctl_set_enum_by_string(ctl, mSpec.bufferGainString[index].c_str()))
    {
        ALOGE("Error: Headset_PGAL_GAIN invalid value");
    }
}

void AudioMTKGainController::SetHeadPhoneRGain(int index)
{
    ALOGD("SetHeadPhoneRGain = %d", index);
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    ctl = mixer_get_ctl_by_name(mMixer, "Headset_PGAR_GAIN");
    type = mixer_ctl_get_type(ctl);

    if (index < 0)
        index = 0;

    if ((uint32_t)index >= mSpec.bufferGainString.size())
        index = mSpec.bufferGainString.size() - 1;
    if (mixer_ctl_set_enum_by_string(ctl, mSpec.bufferGainString[index].c_str()))
    {
        ALOGE("Error: Headset_PGAR_GAIN invalid value");
    }
}

void   AudioMTKGainController::setAudioBufferGain(int gain)
{
#ifdef MTK_AUDIO_SW_DRE
    if (mSWDREMute)
    {
        ALOGD("%s(), bypass when SWDRE mute", __FUNCTION__);
        return;
    }
#endif

    if (gain >= mSpec.bufferGainString.size())
    {
        gain = mSpec.bufferGainString.size() - 1;
    }

    ALOGD("setAudioBufferGain, gain %d, mHwVolume.audioBuffer %d", gain, mHwVolume.audioBuffer);

    mHwVolume.audioBuffer = gain;
    SetHeadPhoneLGain(gain);
    SetHeadPhoneRGain(gain);
}

void  AudioMTKGainController::setVoiceBufferGain(int gain)
{
    if (gain >= mSpec.bufferGainString.size())
    {
        gain = mSpec.bufferGainString.size() - 1;
    }

    ALOGD("setVoiceBufferGain, gain %d, mHwVolume.voiceBuffer %d", gain, mHwVolume.voiceBuffer);

    mHwVolume.voiceBuffer = gain;
    SetReceiverGain(gain);
}

int AudioMTKGainController::GetSPKGain(void)
{
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    unsigned int num_values, i ;
    int index;
    ALOGD("GetSPKGain");
    ctl = mixer_get_ctl_by_name(mMixer, mSpec.spkLMixerName.c_str());
    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    for (i = 0; i < num_values; i++)
    {
        index = mixer_ctl_get_value(ctl, i);
        ALOGD("GetSPKGain i = %d index = %d ", i , index);
    }

    return index;
}

void AudioMTKGainController::SetSpeakerGain(int index)
{
    ALOGD("SetSpeakerGain,index=%d \n", index);

    std::vector<std::string> *enumString;
    switch (mSpec.spkAnaType)
    {
        case GAIN_ANA_LINEOUT:
            enumString = &mSpec.bufferGainString;

            if (index < 0)
                index = 0;

            break;
        case GAIN_ANA_SPEAKER:
        default:
            enumString = &mSpec.spkGainString;

            if (index < 0)
                index = 1;

            break;
    }

    if ((uint32_t)index >= enumString->size())
        index = enumString->size() - 1;

    struct mixer_ctl *ctl;
    // left chanel
    ctl = mixer_get_ctl_by_name(mMixer, mSpec.spkLMixerName.c_str());
    if (mixer_ctl_set_enum_by_string(ctl, enumString->at(index).c_str()))
    {
        ALOGE("Error: %s invalid value", mSpec.spkLMixerName.c_str());
    }
#ifdef ENABLE_STEREO_SPEAKER
    // right channel
    ctl = mixer_get_ctl_by_name(mMixer, mSpec.spkRMixerName.c_str());
    if (mixer_ctl_set_enum_by_string(ctl, enumString->at(index).c_str()))
    {
        ALOGE("Error: %s invalid value", mSpec.spkRMixerName.c_str());
    }
#endif
}

void   AudioMTKGainController::setSpeakerGain(int gain)
{
    ALOGD("%s(), gain = %d", __FUNCTION__, gain);

#ifdef MTK_NEW_VOL_CONTROL
    // adjust gain according to master volume
    if (mSpec.spkAnaType == GAIN_ANA_LINEOUT)
    {
        gain = tuneGainForMasterVolume(gain, mHwStream.mode, GAIN_DEVICE_SPEAKER);
    }
    else if (mSpec.spkAnaType == GAIN_ANA_SPEAKER)
    {
        if (!isInVoiceCall(mHwStream.mode)) // mMasterVolume don't affect voice call
        {
            int degradedB = (keyvolumeStep - logToLinear(mMasterVolume)) * keydBPerStep;
            ALOGD("%s(), degraded gain of mMasterVolume = %d dB", __FUNCTION__, degradedB);
            gain -= degradedB;
            if (gain <= 0) //avoid mute
                gain = 1;
        }
    }
#endif

    ALOGD("setSpeakerGain, gain_idx %d, mHwVolume.speaker %d", gain, mHwVolume.speaker);

    mHwVolume.speaker = gain;
    SetSpeakerGain(gain);
}

void   AudioMTKGainController::setAMPGain(void *points, int num, int device)
{
    ALOGD("setAMPGain, device %d", device);
#ifdef USING_EXTAMP_TC1
//    if (mAmpControl && points)
//    {
//        mAmpControl->setVolume(points, num, device);
//    }
#endif
}

void AudioMTKGainController::SetAdcPga1(int gain)
{
    ALOGD("SetAdcPga1 = %d", gain);
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    int num_values = 0;
    ctl = mixer_get_ctl_by_name(mMixer, "Audio_PGA1_Setting");
    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    if (mixer_ctl_set_enum_by_string(ctl, mSpec.ulPgaGainString[gain].c_str()))
    {
        ALOGE("Error: Audio_PGA1_Setting invalid value");
    }
}
void AudioMTKGainController::SetAdcPga2(int gain)
{
    ALOGD("SetAdcPga2 = %d", gain);
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    int num_values = 0;

    ctl = mixer_get_ctl_by_name(mMixer, "Audio_PGA2_Setting");
    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);
    if (mixer_ctl_set_enum_by_string(ctl, mSpec.ulPgaGainString[gain].c_str()))
    {
        ALOGE("Error: Audio_PGA2_Setting invalid value");
    }
}

status_t AudioMTKGainController::ApplyMicGain(uint32_t MicType, int mode)
{
    /* deprecated !!! DO NOT USE !!! */
    /* The following will be removed */
    GAIN_MIC_MODE micMode = GAIN_MIC_NORMAL;
    GAIN_DEVICE gainDevice = GAIN_DEVICE_EARPIECE;

    if (MicType == Normal_Mic)
    {
        micMode = mSpeechNB ? GAIN_MIC_VOICE_CALL_NB : GAIN_MIC_VOICE_CALL_WB;
        gainDevice = GAIN_DEVICE_EARPIECE;
    }
    else if (MicType == Handfree_Mic)
    {
        micMode = mSpeechNB ? GAIN_MIC_VOICE_CALL_NB : GAIN_MIC_VOICE_CALL_WB;
        gainDevice = GAIN_DEVICE_SPEAKER;
    }
    else if (MicType == Headset_Mic)
    {
        micMode = mSpeechNB ? GAIN_MIC_VOICE_CALL_NB : GAIN_MIC_VOICE_CALL_WB;
        gainDevice = GAIN_DEVICE_HEADSET;
    }
    else if (MicType == TTY_CTM_Mic)
    {
        micMode = mSpeechNB ? GAIN_MIC_VOICE_CALL_NB : GAIN_MIC_VOICE_CALL_WB;
        gainDevice = GAIN_DEVICE_TTY;
    }
    else
    {
        ALOGE("%s(), MicType not supported", __FUNCTION__);
        return BAD_VALUE;
    }

    return ApplyMicGain(micMode, gainDevice, (audio_mode_t)mode);
}

status_t AudioMTKGainController::ApplyMicGain(GAIN_MIC_MODE _micMode, GAIN_DEVICE _gainDevice, audio_mode_t _mode)
{
    uint8_t analogidx;
    uint8_t analog = mGainTable.micGain[_micMode][_gainDevice].gain;
    uint8_t degradedb = mSpec.micIdxMax- analog;
    uint8_t analogdegrade = mSpec.ulPgaGainMap[degradedb];
    uint8_t swagcmap = mSpec.swagcGainMap[degradedb];

    mULTotalGain = mULTotalGainTable[_micMode][_gainDevice];

    ALOGD("ApplyMicGain(), _mic_mode = %d, _gain_device = %d, mode = %d, micgain = %d, mULTotalGain = %d",
          _micMode, _gainDevice, _mode, analog, mULTotalGain);

    mHwVolume.micGain = analog;
    analogidx = (mSpec.ulPgaGainMapMax- analogdegrade) / mSpec.ulHwPgaIdxMax;
    SetAdcPga1(analogidx);
    SetAdcPga2(analogidx);

    mHwVolume.swAgc = swagcmap;
    if (isInVoiceCall(_mode))
    {
        ApplyMdUlGain(swagcmap);
    }

    return NO_ERROR;
}

uint8_t  AudioMTKGainController::GetSWMICGain()
{
    return mHwVolume.swAgc ;
}

status_t AudioMTKGainController::ApplySideTone(uint32_t Mode)
{
    if (Mode < GAIN_DEVICE_EARPIECE || Mode >= NUM_GAIN_DEVICE)
    {
        ALOGW("error, invalid gainDevice = %d, do nothing", Mode);
        return BAD_VALUE;
    }

    // here apply side tone gain, need base on UL and DL analog gainQuant
    GAIN_SPEECH_BAND band = mSpeechNB ? GAIN_SPEECH_NB : GAIN_SPEECH_WB;

#if defined(MTK_HAC_SUPPORT)
    // change gain device if HAC On
    if (Mode == GAIN_DEVICE_EARPIECE)
    {
        bool mHACon = SpeechEnhancementController::GetInstance()->GetHACOn();
        if (mHACon)
        {
            ALOGD("%s(): HAC ON = %d, gain device change from EarPiece to HAC", __FUNCTION__, mHACon);
            Mode = GAIN_DEVICE_HAC;
        }
    }
#endif

    uint8_t sidetone =  mGainTable.sidetoneGain[band][Mode].gain;
    if (sidetone > mSpec.sidetoneIdxMax)
    {
        sidetone = mSpec.sidetoneIdxMax;
    }

    uint16_t updated_sidetone = 0;

    if (isEarpieceCategory((enum GAIN_DEVICE)Mode))
    {
        updated_sidetone = updateSidetone(mSpec.bufferGainDb[GetReceiverGain()], sidetone, mHwVolume.swAgc);
    }
    else if (isHeadsetCategory((enum GAIN_DEVICE)Mode))
    {
        updated_sidetone = updateSidetone(mSpec.bufferGainDb[GetHeadphoneRGain()], sidetone, mHwVolume.swAgc);
    }
    else if (Mode == GAIN_DEVICE_SPEAKER)
    {
        //value = updateSidetone(KeyVoiceBufferGain[GetReceiverGain()], sidetone, mHwVolume.swAgc);
        // mute sidetone gain when speaker mode.
        updated_sidetone = 0;
    }

    if (mHwVolume.sideTone != updated_sidetone)
    {
        mHwVolume.sideTone = updated_sidetone;

        ALOGD("ApplySideTone gainDevice %d, sidetone %u, updated_sidetone %u", Mode, sidetone, updated_sidetone);
        SpeechDriverFactory::GetInstance()->GetSpeechDriver()->SetSidetoneGain(updated_sidetone);
    }

    return NO_ERROR;
}

uint16_t AudioMTKGainController::updateSidetone(int dlPGAGain, int  sidetone, uint8_t ulGain)
{

    int vol = 0;
    uint16_t DSP_ST_GAIN = 0;
    ALOGD("updateSidetone dlPGAGain %d, sidetone %d, ulGain %u", dlPGAGain, sidetone, ulGain);

    if (sidetone == 0)
    {
        DSP_ST_GAIN = 0 ;
    }
    else
    {
        vol = sidetone + ulGain; //1dB/step
        vol = dlPGAGain - vol + 67 - mSpec.ulGainOffset;
        if (vol < 0)
        {
            vol = 0;
        }
        if (vol > mSpec.sidetoneIdxMax)
        {
            vol = mSpec.sidetoneIdxMax;
        }
        DSP_ST_GAIN = mSpec.stfGainMap[vol];
    }
    ALOGD("DSP_ST_GAIN=%d", DSP_ST_GAIN);
    return DSP_ST_GAIN;
}

bool AudioMTKGainController::isInVoiceCall(audio_mode_t mode)
{
    return (mode == AUDIO_MODE_IN_CALL ||
            mode == AUDIO_MODE_IN_CALL_2 ||
            mode == AUDIO_MODE_IN_CALL_EXTERNAL);
}

bool AudioMTKGainController::isInVoipCall(audio_mode_t mode)
{
    return mode == AUDIO_MODE_IN_COMMUNICATION;
}

bool AudioMTKGainController::isInCall(audio_mode_t mode)
{
    return (isInVoiceCall(mode) || isInVoipCall(mode));
}


//static functin to get FM power state
#define BUF_LEN 1
static char rbuf[BUF_LEN] = {'\0'};
static char wbuf[BUF_LEN] = {'1'};
static const char *FM_POWER_STAUTS_PATH = "/proc/fm";
static const char *FM_DEVICE_PATH = "dev/fm";

status_t AudioMTKGainController::setFmVolume(const float fm_volume)
{
    ALOGV("%s(), fm_volume = %f", __FUNCTION__, fm_volume);

    // Calculate HW Gain Value
    uint32_t volume_index = logToLinear(fm_volume); // 0 ~ 256
    uint32_t hw_gain = kHWGainMap[volume_index >> 1]; // 0 ~ 0x80000

    // Set HW Gain
    return mHardwareResourceManager->setHWGain2DigitalGain(hw_gain);
}

bool AudioMTKGainController::SetFmChipVolume(int volume)
{
    ALOGD("%s is deprecated and not used", __FUNCTION__);
    return false;
}
int  AudioMTKGainController::GetFmVolume(void)
{
    ALOGD("%s is deprecated and not used", __FUNCTION__);
    return 0;
}

#ifdef MTK_NEW_VOL_CONTROL
/*
status_t AudioMTKGainController::SetDigitalHwGain(int _digitalLinearGain, enum GAINTABLE_HWGAIN_TYPE _hwGainType)
{
    ALOGD("%s(), _digitalLinearGain = %d, _hwGainType = %d", __FUNCTION__, _digitalLinearGain, _hwGainType);

    uint32_t hw_gain = kHWGainMap[_digitalLinearGain >> 1]; // 0 ~ 0x80000

    switch (_hwGainType)
    {
        case GAINTABLE_HWGAIN1:
            return mHardwareResourceManager->setHWGain2DigitalGain(hw_gain);
        case GAINTABLE_HWGAIN2:
            ALOGW("%s(), error, HW Gain2 not supported", __FUNCTION__);
            return BAD_VALUE;
        default:
            ALOGW("%s(), error, undefined _HwGainType = %d", __FUNCTION__, _hwGainType);
            break;
    }
    return BAD_VALUE;
}

enum GAINTABLE_HWGAIN_TYPE AudioMTKGainController::GetHWGainTypeForFM()
{
    return GAINTABLE_HWGAIN1;
}
*/
int AudioMTKGainController::GetDigitalLinearGain(int _volIdx, audio_devices_t _device, audio_stream_type_t _streamType)
{
    ALOGD("%s(), _volIdx = %d, _device = %d, _streamType = %d", __FUNCTION__, _volIdx, _device, _streamType);

    GAIN_DEVICE gainDevice = getGainDevice(_device);

    // convert _volIdx to linear(0~255)
    if (!isValidStreamType(_streamType))
    {
        ALOGE("error, Invalid stream type = %d", _streamType);
        _streamType = AUDIO_STREAM_MUSIC;
    }
    int linearGain = mGainTable.streamGain[_streamType][gainDevice][_volIdx].digital;
    return keyvolumeStep - linearGain;
}

float AudioMTKGainController::GetDigitalLogGain(int _volIdx, audio_devices_t _device, audio_stream_type_t _streamType)
{
    return linearToLog(GetDigitalLinearGain(_volIdx, _device, _streamType));
}

bool AudioMTKGainController::isValidStreamType(audio_stream_type_t _streamType)
{
    return (_streamType >= GAIN_MIN_STREAM_TYPE && _streamType <= GAIN_MAX_STREAM_TYPE);
}

bool AudioMTKGainController::isValidVolIdx(int _idx, audio_mode_t _mode)
{
    if (isInVoiceCall(_mode))
        return (_idx >= 0 && _idx <= GAIN_MAX_SPEECH_VOL_INDEX);
    else
        return (_idx >= 0 && _idx <= GAIN_MAX_VOL_INDEX);
}

bool AudioMTKGainController::isHeadsetCategory(enum GAIN_DEVICE _gainDevice)
{
    return _gainDevice == GAIN_DEVICE_HEADSET || _gainDevice == GAIN_DEVICE_HSSPK || _gainDevice == GAIN_DEVICE_HEADSET_5POLE;
}

bool AudioMTKGainController::isEarpieceCategory(enum GAIN_DEVICE _gainDevice)
{
    return _gainDevice == GAIN_DEVICE_EARPIECE || _gainDevice == GAIN_DEVICE_HAC;
}

bool AudioMTKGainController::isSpeakerCategory(enum GAIN_DEVICE _gainDevice)
{
    return _gainDevice == GAIN_DEVICE_SPEAKER || _gainDevice == GAIN_DEVICE_HSSPK;
}

uint32_t AudioMTKGainController::getHpImpedanceIdx(uint32_t impedance)
{
    for (unsigned int i = 0; i < mSpec.hpImpThresholdList.size(); i++)
    {
        if (impedance <= mSpec.hpImpThresholdList[i])
        {
            return i;
        }
    }

    return mSpec.hpImpThresholdList.size() - 1;
}

int  AudioMTKGainController::getHpImpedanceCompesateValue(void)
{
    ASSERT(mHpImpedanceIdx >= 0 && mHpImpedanceIdx < mSpec.hpImpThresholdList.size());

    ALOGD("%s(), mHpImpedanceIdx = %d, compensate value = %d",
          __FUNCTION__,
          mHpImpedanceIdx,
          mSpec.hpImpCompensateList[mHpImpedanceIdx]);

    return mSpec.hpImpCompensateList[mHpImpedanceIdx];
}

int AudioMTKGainController::tuneGainForMasterVolume(int gain, audio_mode_t mode, GAIN_DEVICE gainDevice)
{
    // adjust gain according to master volume
    if (!isInVoiceCall(mode) && // mMasterVolume don't affect voice call
        gain <= mSpec.bufferGainPreferMaxIdx) // don't change gain if already mute(-40dB)
    {
        int degradedB = (keyvolumeStep - logToLinear(mMasterVolume)) * keydBPerStep;
        ALOGD("%s(), degraded gain of mMasterVolume = %d dB", __FUNCTION__, degradedB);
        if (gain + degradedB <= mSpec.bufferGainPreferMaxIdx)
            gain += degradedB;
        else
            gain = mSpec.bufferGainPreferMaxIdx;
    }

    return gain;
}

int AudioMTKGainController::tuneGainForHpImpedance(int gain, audio_mode_t mode, GAIN_DEVICE gainDevice)
{
    if (isHeadsetCategory(gainDevice) &&
        mSpec.hpImpEnable &&
        gain <= mSpec.bufferGainPreferMaxIdx)    // don't change gain if already mute(-40dB)
    {
        ALOGV("%s(), before compesate HP impedance, bufferGain = %d", __FUNCTION__, gain);
        gain += getHpImpedanceCompesateValue();
        // prevent set to mute(-40dB) when change gain for HP impedance
        if (gain > mSpec.bufferGainPreferMaxIdx)
            gain = mSpec.bufferGainPreferMaxIdx;
        else if (gain < 0)
            gain = 0;

        ALOGD("%s(), after compesate HP impedance idx (%d), bufferGain = %d",
              __FUNCTION__,
              mHpImpedanceIdx,
              gain);
    }

    return gain;
}

#endif
/********************************************************************************
*
*
*
*                                                            UnUsed API
*
*
*
***********************************************************************************/

uint16_t AudioMTKGainController::MappingToDigitalGain(unsigned char Gain)
{
    return 0;
}

uint16_t AudioMTKGainController::MappingToPGAGain(unsigned char Gain)
{
    return 0;
}

status_t AudioMTKGainController::setMasterVolume(float v, audio_mode_t mode, uint32_t devices)
{
    ALOGD("%s(), mMasterVolume = %f, mode = %d, devices = 0x%x", __FUNCTION__, v, mode, devices);
    mMasterVolume = v;
    mHwStream.mode = mode;
    if(!isInVoiceCall(mode))
    {
        ALOGD("setMasterVolume call setNormalVolume");
        setNormalVolume(mHwStream.stream, mHwStream.index, devices, mode);
    }
    return NO_ERROR;
}

float AudioMTKGainController::getMasterVolume()
{
    ALOGD("AudioMTKGainController getMasterVolume");
    return mMasterVolume;
}


status_t AudioMTKGainController::setVoiceVolume(float v, audio_mode_t mode, uint32_t device)
{
    ALOGD("%s(), mVoiceVolume = %f, mode = %d, device = 0x%x", __FUNCTION__, v, mode, device);
    mVoiceVolume = v;
    mHwStream.mode = mode;
    ALOGD("call setVoiceVolume");
    setVoiceVolume(mHwStream.index, device, mode);
    return NO_ERROR;
}

float AudioMTKGainController::getVoiceVolume(void)
{
    ALOGD("AudioMTKGainController getVoiceVolume");
    return mVoiceVolume;
}

#ifdef MTK_NEW_VOL_CONTROL
status_t AudioMTKGainController::setVoiceVolume(int MapVolume, uint32_t device)
{
    return INVALID_OPERATION;
}
status_t AudioMTKGainController::ApplyVoiceGain(int degradeDb, audio_mode_t mode, uint32_t device)
{
    return INVALID_OPERATION;
}
#endif

status_t AudioMTKGainController::setStreamVolume(int stream, float v)
{
    return INVALID_OPERATION;
}

status_t AudioMTKGainController::setStreamMute(int stream, bool mute)
{
    return INVALID_OPERATION;
}

float AudioMTKGainController::getStreamVolume(int stream)
{
    return 1.0;
}

// should depend on different usage , FM ,MATV and output device to setline in gain
status_t AudioMTKGainController::SetLineInPlaybackGain(int type)
{
    return INVALID_OPERATION;
}

status_t AudioMTKGainController::SetLineInRecordingGain(int type)
{
    return INVALID_OPERATION;
}

status_t AudioMTKGainController::SetSideTone(uint32_t Mode, uint32_t Gain)
{

    return INVALID_OPERATION;
}

uint32_t AudioMTKGainController::GetSideToneGain(uint32_t device)
{

    return INVALID_OPERATION;
}


status_t AudioMTKGainController::SetMicGain(uint32_t Mode, uint32_t Gain)
{
    return INVALID_OPERATION;
}

status_t AudioMTKGainController::SetULTotalGain(uint32_t Mode, unsigned char Gain)
{
    /* deprecated */
    return NO_ERROR;
}

status_t AudioMTKGainController::SetDigitalHwGain(uint32_t Mode, uint32_t Gain , uint32_t routes)
{
    return INVALID_OPERATION;
}

status_t AudioMTKGainController::SetMicGainTuning(uint32_t Mode, uint32_t Gain)
{
    return INVALID_OPERATION;
}

bool AudioMTKGainController::GetHeadPhoneImpedance(void)
{
#ifdef MTK_NEW_VOL_CONTROL
    ALOGD("%s(), mSpec.hpImpEnable = %d mHpImpedanceIdx= %d",
          __FUNCTION__, mSpec.hpImpEnable, mHpImpedanceIdx);

    if (mSpec.hpImpEnable)
    {
        uint32_t newHPImpedance;

        struct mixer_ctl *ctl;
        unsigned int num_values, i;
        ctl = mixer_get_ctl_by_name(mMixer, "Audio HP ImpeDance Setting");

        num_values = mixer_ctl_get_num_values(ctl);
        for (i = 0; i < num_values; i++)
        {
            newHPImpedance = mixer_ctl_get_value(ctl, i);
            ALOGD("GetHeadPhoneImpedance, i = %d, newHPImpedance = %d", i , newHPImpedance);
        }

        uint32_t newHpImpedanceIdx = getHpImpedanceIdx(newHPImpedance);
        ALOGD("newHpImpedanceIdx = %d ", newHpImpedanceIdx);

        // update volume setting
        if (mHpImpedanceIdx != newHpImpedanceIdx)
        {
            mHpImpedanceIdx = newHpImpedanceIdx;
            setAnalogVolume_l(mHwStream.stream, mHwStream.devices, mHwStream.index, mHwStream.mode);
        }
    }
#endif
    return true;
}

int AudioMTKGainController::ApplyAudioGainTuning(int Gain, uint32_t mode, uint32_t device)
{
    return 0;
}

void AudioMTKGainController::SetLinoutRGain(int DegradedBGain)
{

}

void AudioMTKGainController::SetLinoutLGain(int DegradedBGain)
{

}

uint32_t AudioMTKGainController::GetOffloadGain(float vol_f)
{
    return 0;
}

#ifdef MTK_AUDIO_SW_DRE
void AudioMTKGainController::registerPlaybackHandler(uint32_t _identity)
{
    if (mMutedHandlerVector.indexOfKey(_identity) >= 0)
    {
        ALOGW("%s(), warn, playback handler already exist, _identity = %d", __FUNCTION__, _identity);
    }
    else
    {
        {
            AudioAutoTimeoutLock _l(mSWDRELock);
            mMutedHandlerVector.add(_identity, false);
        }
        updateSWDREState(true, false);
    }
}

void AudioMTKGainController::removePlaybackHandler(uint32_t _identity)
{
    if (mMutedHandlerVector.indexOfKey(_identity) < 0)
    {
        ALOGW("%s(), warn, playback handler not found, _identity = %d", __FUNCTION__, _identity);
    }
    else
    {
        {
            AudioAutoTimeoutLock _l(mSWDRELock);
            mMutedHandlerVector.removeItem(_identity);
        }
        updateSWDREState(true, false);
    }
}

void AudioMTKGainController::requestMute(uint32_t _identity, bool _mute)
{
    if (mMutedHandlerVector.indexOfKey(_identity) < 0)
    {
        ALOGW("%s(), warn, playback handler not found, _identity = %d", __FUNCTION__, _identity);
    }
    else
    {
        if (mMutedHandlerVector.valueFor(_identity) != _mute)
        {
            {
                AudioAutoTimeoutLock _l(mSWDRELock);
                mMutedHandlerVector.replaceValueFor(_identity, _mute);
            }
            updateSWDREState(false, true);
        }
    }
}

void AudioMTKGainController::updateSWDREState(bool _numChanged, bool _muteChanged)
{
    bool curHasMuteHandler = false;
    size_t curNumHandler;

    if (_numChanged == false && _muteChanged == true)
    {
        {
            AudioAutoTimeoutLock _l(mSWDRELock);
            for (size_t i = 0; i < mMutedHandlerVector.size(); i++)
            {
                if (mMutedHandlerVector.valueAt(i) == true)
                {
                    curHasMuteHandler = true;
                    break;
                }
            }
        }

        if (mNumHandler == 1)
        {
            if (mHasMuteHandler == false && curHasMuteHandler == true)
            {
                ALOGD("%s(), num = 1, hasMuteHandler false -> true", __FUNCTION__);
                mSWDREMute = true;
                // call ramp to mute
                SWDRERampToMute();
            }
            else if (mHasMuteHandler == true && curHasMuteHandler == false)
            {
                ALOGD("%s(), num = 1, hasMuteHandler true -> false", __FUNCTION__);
                mSWDREMute = false;
                // call ramp to normal
                SWDRERampToNormal();
            }
        }

        mHasMuteHandler = curHasMuteHandler;
    }
    else if (_numChanged == true && _muteChanged == false)
    {
        {
            AudioAutoTimeoutLock _l(mSWDRELock);
            curNumHandler = mMutedHandlerVector.size();
        }

        if (mHasMuteHandler)
        {
            if (mNumHandler > 1 && curNumHandler == 1)
            {
                ALOGD("%s(), hasMuteHandler, numHandler >1 -> 1", __FUNCTION__);
                mSWDREMute = true;
                // call ramp to mute
                SWDRERampToMute();
            }
            else if (mNumHandler == 1 && curNumHandler > 1)
            {
                ALOGD("%s(), hasMuteHandler, numHandler 1 -> >1", __FUNCTION__);
                mSWDREMute = false;
                // call ramp to normal
                SWDRERampToNormal();
            }
            else if (curNumHandler == 0)
            {
                ALOGD("%s(), hasMuteHandler, numHandler 1 -> 0", __FUNCTION__);
                mSWDREMute = false;
                mHasMuteHandler = false;
                // call ramp to normal
                SWDRERampToNormal();
            }
        }

        mNumHandler = curNumHandler;
    }
    else
    {
        return;
    }
}

void AudioMTKGainController::SWDRERampToMute()
{
    int cur_idx = GetHeadphoneLGain();
    int target_idx = (int)(mSpec.bufferGainString.size() - 1);

    ALOGD("%s(), cur_idx = %d, target_idx = %d", __FUNCTION__, cur_idx, target_idx);
    while (cur_idx < target_idx)
    {
        cur_idx++;
        SetHeadPhoneLGain(cur_idx);
        SetHeadPhoneRGain(cur_idx);
        usleep(1000);
    }
}

void AudioMTKGainController::SWDRERampToNormal()
{
    int cur_idx = GetHeadphoneLGain();
    if (cur_idx != mSpec.bufferGainString.size() - 1)
        ALOGW("%s(), cur_idx %d != mute", __FUNCTION__, cur_idx);

    GAIN_DEVICE gainDevice = GAIN_DEVICE_HEADSET;

    unsigned char bufferGain = mGainTable.streamGain[mHwStream.stream][gainDevice][mHwStream.index].analog[GAIN_ANA_HEADPHONE];

    if (bufferGain >= mSpec.bufferGainString.size())
    {
        bufferGain = mSpec.bufferGainString.size() - 1;
    }

    // adjust gain according to master volume
    bufferGain = tuneGainForMasterVolume(bufferGain, mHwStream.mode, gainDevice);

    // adjust gain according to hp impedance when using HP
    bufferGain = tuneGainForHpImpedance(bufferGain, mHwStream.mode, gainDevice);

    int target_idx = bufferGain;

    ALOGD("%s(), cur_idx = %d, target_idx = %d", __FUNCTION__, cur_idx, target_idx);
    while (target_idx < cur_idx)
    {
        cur_idx--;
        SetHeadPhoneLGain(cur_idx);
        SetHeadPhoneRGain(cur_idx);
        usleep(1000);
    }
}

#endif
}
