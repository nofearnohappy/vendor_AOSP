#include "AudioGainTableParamParser.h"

#include <utils/Log.h>
#include "AudioUtility.h"//Mutex/assert
#include <system/audio.h>

#include <string>

#define LOG_TAG "GainTableParamParser"

namespace android
{

// Stream Type
const std::string gppStreamTypeXmlName[GAIN_STREAM_TYPE_SIZE] = {"Voice",
                                                                 "System",
                                                                 "Ring",
                                                                 "Music",
                                                                 "Alarm",
                                                                 "Notification",
                                                                 "Bluetooth_sco",
                                                                 "Enforced_Audible",
                                                                 "DTMF",
                                                                 "TTS",
#ifndef MTK_BASIC_PACKAGE
                                                                 "Boot",
                                                                 "VIBSPK",
#endif
                                                                 "Accessibility"
                                                                };
// Device
const std::string gppDeviceXmlName[NUM_GAIN_DEVICE] = {"RCV",
                                                       "HS",
                                                       "SPK",
                                                       "HSSPK",
                                                       "HS5POLE",
                                                       "HAC",
                                                       "BT",
                                                       "TTY"
                                                      };
// Speech
const std::string gppBandXmlName[NUM_GAIN_SPEECH_BAND] = {"NB",
                                                          "WB"
                                                         };

const GAIN_MIC_MODE gppSpeechMicMap[NUM_GAIN_SPEECH_BAND] = {GAIN_MIC_VOICE_CALL_NB, GAIN_MIC_VOICE_CALL_WB};

// MIC
const std::string gppMicModeXmlName[NUM_GAIN_MIC_MODE] = {"Sound recording",
                                                          "Speech NB",
                                                          "Speech WB",
                                                          "Camera recording",
                                                          "VR",
                                                          "VoIP",
                                                          "VOICE_UNLOCK",
                                                          "CUSTOMIZATION1",
                                                          "CUSTOMIZATION2",
                                                          "CUSTOMIZATION3"
                                                         };
/*==============================================================================
 *                     Singleton Pattern
 *============================================================================*/

GainTableParamParser *GainTableParamParser::mGainTableParamParser = NULL;

GainTableParamParser *GainTableParamParser::getInstance()
{
    static Mutex mGetInstanceLock;
    Mutex::Autolock _l(mGetInstanceLock);
    ALOGD("%s()", __FUNCTION__);

    if (mGainTableParamParser == NULL)
    {
        mGainTableParamParser = new GainTableParamParser();
    }
    ASSERT(mGainTableParamParser != NULL);
    return mGainTableParamParser;
}
/*==============================================================================
 *                     Constructor / Destructor / Init / Deinit
 *============================================================================*/

GainTableParamParser::GainTableParamParser() :
    mAppHandle(appHandleGetInstance())
{
    ALOGD("%s()", __FUNCTION__);
    loadGainTableParam();
}

GainTableParamParser::~GainTableParamParser()
{
    for (unsigned int i = 0; i < NUM_GAIN_DEVICE; i++)
    {
        mMapDlDigital[i].clear();
        mMapDlAnalog[i].clear();
    }
}

status_t GainTableParamParser::getGainTableParam(GainTableParam *_gainTable)
{
    ALOGD("%s()", __FUNCTION__);
    memset((void *)_gainTable, 0, sizeof(GainTableParam));
    status_t status = NO_ERROR;
    status |= updatePlaybackDigitalGain(_gainTable);
    status |= updatePlaybackAnalogGain(_gainTable);
    status |= updateSpeechVol(_gainTable);
    status |= updateRecordVol(_gainTable);
    status |= updateVoIPVol(_gainTable);

    if (status != NO_ERROR)
    {
        ALOGE("error, %s() failed, status=%d", __FUNCTION__, status);
        return status;
    }

    return NO_ERROR;
}

status_t GainTableParamParser::getGainTableSpec(GainTableSpec *_gainTableSpec)
{
    *_gainTableSpec = mSpec;
    return NO_ERROR;
}


status_t GainTableParamParser::updatePlaybackDigitalGain(GainTableParam *_gainTable)
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    const char audioTypeName[] = PLAY_DIGI_AUDIOTYPE_NAME;
    const char paramName[] = "digital_gain";
    const std::string *profileName = gppDeviceXmlName;
    const std::string *volumeTypeName = gppStreamTypeXmlName;

    // extract parameters from xml
    AudioType *audioType;
    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    for (int stream = GAIN_MIN_STREAM_TYPE; stream <= GAIN_MAX_STREAM_TYPE; stream++)
    {
        for (int device = 0; device < NUM_GAIN_DEVICE; device++)
        {
            // get param unit using param path
            std::string paramPath = "Volume type," +
                                    volumeTypeName[stream] +
                                    ",Profile," +
                                    profileName[device];
            ALOGD("paramPath = %s", paramPath.c_str());

            ParamUnit *paramUnit;
            paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
            if (!paramUnit)
            {
                ALOGW("error: get paramUnit fail, paramPath = %s", paramPath.c_str());
                continue;
            }

            Param *param;
            param = paramUnitGetParamByName(paramUnit, paramName);
            if (!param)
            {
                ALOGW("error: get param fail");
                continue;
            }

            // convert xml param to gain table
            short* shortArray = (short*)param->data;
            int arraySize = param->arraySize;
            if (arraySize > GAIN_VOL_INDEX_SIZE)
            {
                ALOGW("error, param->arraySize %d exceed digital array size %d", arraySize, GAIN_VOL_INDEX_SIZE);
                arraySize = GAIN_VOL_INDEX_SIZE;
            }

            for (int i = 0; i < arraySize; i++)
            {
                unsigned char *digital = &_gainTable->streamGain[stream][device][i].digital;

                // convert 0~-64 dB to 0~255
                if (shortArray[i] > mSpec.digiDbMax)
                {
                    ALOGW("error, param out of range, val %d > %d", shortArray[i], mSpec.digiDbMax);
                    *digital = 0;
                }
                else if (shortArray[i] <= mSpec.digiDbMin)
                {
                    ALOGW("error, param out of range, val %d <= %d", shortArray[i], mSpec.digiDbMin);
                    *digital = mSpec.keyVolumeStep;
                }
                else
                {
                    *digital = (shortArray[i] * -1 * mSpec.keyStepPerDb);
                }

                ALOGD("\tarray[%d] = %d, convert result = %d\n", i, shortArray[i], *digital);
            }
        }
    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;
}

status_t GainTableParamParser::updatePlaybackAnalogGain(GainTableParam *_gainTable)
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    const char audioTypeName[] = PLAY_ANA_AUDIOTYPE_NAME;
    const char paramHsName[] = "headset_pga";
    const char paramSpkName[] = "speaker_pga";
    const char paramRcvName[] = "receiver_pga";
    const std::string *profileName = gppDeviceXmlName;
    const std::string *volumeTypeName = gppStreamTypeXmlName;

    // extract parameters from xml
    AudioType *audioType;
    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    for (int stream = GAIN_MIN_STREAM_TYPE; stream <= GAIN_MAX_STREAM_TYPE; stream++)
    {
        for (int device = 0; device < NUM_GAIN_DEVICE; device++)
        {
            // get param unit using param path
            std::string paramPath = "Volume type," +
                                    volumeTypeName[stream] +
                                    ",Profile," +
                                    profileName[device];
            ALOGD("paramPath = %s", paramPath.c_str());

            ParamUnit *paramUnit;
            paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
            if (!paramUnit)
            {
                ALOGW("warn: get paramUnit fail, paramPath = %s", paramPath.c_str());
                continue;
            }
            Param *param_hs;
            param_hs = paramUnitGetParamByName(paramUnit, paramHsName);
            if (!param_hs)
            {
                ALOGW("warn: get param_hs fail");
                continue;
            }
            Param *param_spk;
            param_spk = paramUnitGetParamByName(paramUnit, paramSpkName);
            if (!param_spk)
            {
                ALOGW("warn: get param_spk fail");
                continue;
            }
            Param *param_rcv;
            param_rcv = paramUnitGetParamByName(paramUnit, paramRcvName);
            if (!param_rcv)
            {
                ALOGW("warn: get param_rcv fail");
                continue;
            }

            // TODO: check if param is in range using checlist field
            if (param_hs->arraySize != 1 || param_spk->arraySize != 1 || param_rcv->arraySize != 1)
            {
                ALOGW("warn: %s arraySize(%d) != 1 || %s arraySize(%d) != 1|| %s arraySize(%d) != 1",
                      paramHsName,
                      param_hs->arraySize,
                      paramSpkName,
                      param_spk->arraySize,
                      paramRcvName,
                      param_rcv->arraySize);
            }

            // analog is the same for different volume level for normal playbck
            for (int i = 0; i < GAIN_VOL_INDEX_SIZE; i++)
            {
                unsigned char *analog = _gainTable->streamGain[stream][device][i].analog;

                if (*(short*)param_rcv->data >= 0)
                    analog[GAIN_ANA_HANDSET] = *(short*)param_rcv->data;
                if (*(short*)param_hs->data >= 0)
                    analog[GAIN_ANA_HEADPHONE] = *(short*)param_hs->data;
                if (*(short*)param_spk->data >= 0)
                    analog[mSpec.spkAnaType] = *(short*)param_spk->data;
            }
        }
    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;
}

status_t GainTableParamParser::updateSpeechVol(GainTableParam *_gainTable)
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    char audioTypeName[] = SPEECH_VOL_AUDIOTYPE_NAME;
    char paramStfName[] = "stf_gain";
    char paramUlName[] = "ul_gain";
    char paramDlName[] = "dl_gain";

    const std::string *profileName = gppDeviceXmlName;
    const std::string *bandName = gppBandXmlName;

    // extract parameters from xml
    AudioType *audioType;

    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    for (int band = GAIN_SPEECH_NB; band <= GAIN_SPEECH_WB; band++)
    {
        for (int device = 0; device < NUM_GAIN_DEVICE; device++)
        {
            // get param unit using param path
            std::string paramPath = "Band," +
                                    bandName[band] +
                                    ",Profile," +
                                    profileName[device];
            ALOGD("paramPath = %s", paramPath.c_str());

            ParamUnit *paramUnit;
            paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
            if (!paramUnit)
            {
                ALOGW("error: get paramUnit fail, paramPath = %s", paramPath.c_str());
                continue;
            }

            // Sidetone gain
            Param *param_stf_gain;
            param_stf_gain = paramUnitGetParamByName(paramUnit, paramStfName);
            if (!param_stf_gain)
            {
                ALOGW("error: get param_stf_gain fail");
                continue;
            }

            if (*(short*)param_stf_gain->data > mSpec.sidetoneIdxMax ||
                *(short*)param_stf_gain->data < mSpec.sidetoneIdxMin )
            {
                ALOGW("error, band %d, device %d, stf_gain = %d out of bound", band, device, *(short*)param_stf_gain->data);
            }
            _gainTable->sidetoneGain[band][device].gain = *(short*)param_stf_gain->data;

            // Uplink gain
            const GAIN_MIC_MODE micType = gppSpeechMicMap[band];
            if (micType >= 0 && micType < NUM_GAIN_MIC_MODE)
            {
                Param *param_ul_gain;
                param_ul_gain = paramUnitGetParamByName(paramUnit, paramUlName);
                if (!param_ul_gain)
                {
                    ALOGW("error: get param_ul_gain fail");
                    continue;
                }

                if (*(int*)param_ul_gain->data > mSpec.micIdxMax ||
                    *(int*)param_ul_gain->data < mSpec.micIdxMin )
                {
                    ALOGW("error, ul_gain = %d out of bound, band %d, device %d", *(int*)param_ul_gain->data, band, device);
                }
                _gainTable->micGain[micType][device].gain = *(int*)param_ul_gain->data;
            }

            // Downlink gain
            Param *param_dl_gain;
            param_dl_gain = paramUnitGetParamByName(paramUnit, paramDlName);
            if (!param_dl_gain)
            {
                ALOGW("error: get param_dl_gain fail");
                continue;
            }

            short* shortArray = (short*)param_dl_gain->data;
            int arraySize = param_dl_gain->arraySize;
            if (arraySize > GAIN_VOL_INDEX_SIZE)
            {
                ALOGW("error, param->arraySize %d exceed digital array size %d", arraySize, GAIN_VOL_INDEX_SIZE);
                arraySize = GAIN_VOL_INDEX_SIZE;
            }

            if (mMapDlDigital[device].size() == 0 ||
                mMapDlAnalog[device].size() == 0 ||
                mMapDlDigital[device].size() != mMapDlAnalog[device].size())
            {
                ALOGE("error, digi & analog map size = %d & %d", mMapDlDigital[device].size(),
                                                                 mMapDlAnalog[device].size());
                continue;
            }

            for (int i = 0; i < arraySize; i++)
            {
                short digital = mMapDlDigital[device][shortArray[i]];
                short analog = mMapDlAnalog[device][shortArray[i]];

                // set digital gain

                // convert 0~-64 dB to 0~255
                if (digital > mSpec.digiDbMax)
                {
                    ALOGW("error, param out of range, val %d > %d", digital, mSpec.digiDbMax);
                    _gainTable->speechGain[band][device][i].digital= 0;
                }
                else if (digital <= mSpec.digiDbMin)
                {
                    ALOGW("error, param out of range, val %d <= %d", digital, mSpec.digiDbMin);
                    _gainTable->speechGain[band][device][i].digital = mSpec.keyVolumeStep;
                }
                else
                {
                    _gainTable->speechGain[band][device][i].digital = (digital * -1 * mSpec.keyStepPerDb);
                }

                // set analog gain
                if (mMapDlAnalogType[device] == GAIN_ANA_SPEAKER)
                {
                    _gainTable->speechGain[band][device][i].analog[mMapDlAnalogType[device]]= spkGainDb2Idx(analog);
                }
                else
                {
                    _gainTable->speechGain[band][device][i].analog[mMapDlAnalogType[device]] = bufferGainDb2Idx(analog);
                }

                ALOGD("\tvol_idx=%d, dl_gain_idx = %d, digital = %d, analog = %d\n",
                    i,
                    shortArray[i],
                    _gainTable->speechGain[band][device][i].digital,
                    _gainTable->speechGain[band][device][i].analog[mMapDlAnalogType[device]]);
                ALOGD("\tvol_idx=%d, dl_gain_idx = %d, digitaldB = %d, analogdB = %d\n", i, shortArray[i], digital, analog);
            }
        }
    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;
}

status_t GainTableParamParser::updateRecordVol(GainTableParam *_gainTable)
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    char audioTypeName[] = REC_VOL_AUDIOTYPE_NAME;
    const std::string *micModeName = gppMicModeXmlName;
    const std::string *profileName = gppDeviceXmlName;

    // extract parameters from xml
    AudioType *audioType;
	ParamUnit *paramUnit;

    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    for (int mode = 0; mode < NUM_GAIN_MIC_MODE; mode++)
    {
        for (int device = 0; device < NUM_GAIN_DEVICE; device++)
        {
            // get param unit using param path
            std::string paramPath = "Application," +
                                    micModeName[mode] +
                                    ",Profile," +
                                    profileName[device];
            ALOGD("paramPath = %s", paramPath.c_str());

            paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
            if (!paramUnit)
            {
                ALOGW("error: get paramUnit fail, paramPath = %s", paramPath.c_str());
                continue;
            }

            // Uplink gain
            Param *param_ul_gain;
            param_ul_gain = paramUnitGetParamByName(paramUnit, "ul_gain");
            if (!param_ul_gain)
            {
                ALOGW("error: get param_ul_gain fail");
                continue;
            }

            if (*(int*)param_ul_gain->data > mSpec.micIdxMax ||
                *(int*)param_ul_gain->data < mSpec.micIdxMin )
            {
                ALOGW("error, ul_gain = %d out of bound, paramPath = %s", *(int*)param_ul_gain->data, paramPath.c_str());
            }
            _gainTable->micGain[mode][device].gain = *(int*)param_ul_gain->data;
            ALOGD("\tgain = %d", _gainTable->micGain[mode][device].gain);
        }
    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;
}

status_t GainTableParamParser::updateVoIPVol(GainTableParam *_gainTable)
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    char audioTypeName[] = VOIP_VOL_AUDIOTYPE_NAME;
    char paramUlName[] = "ul_gain";
    char paramDlName[] = "dl_gain";
    const std::string *profileName = gppDeviceXmlName;

    // extract parameters from xml
    AudioType *audioType;
    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    for (int device = 0; device < NUM_GAIN_DEVICE; device++)
    {
        // get param unit using param path
        std::string paramPath = "Profile," +
                                profileName[device];
        ALOGD("paramPath = %s", paramPath.c_str());

      	ParamUnit *paramUnit;
        paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
        if (!paramUnit)
        {
            ALOGW("error: get paramUnit fail, paramPath = %s", paramPath.c_str());
            continue;
        }

        // Uplink gain
        const GAIN_MIC_MODE micType = GAIN_MIC_VOICE_COMMUNICATION;

        if (micType >= 0 && micType < NUM_GAIN_MIC_MODE)
        {
            Param *param_ul_gain;
            param_ul_gain = paramUnitGetParamByName(paramUnit, paramUlName);
            if (!param_ul_gain)
            {
                ALOGW("error: get param_ul_gain fail, param name = %s", paramUlName);
                continue;
            }

            if (*(int*)param_ul_gain->data > mSpec.micIdxMax ||
                *(int*)param_ul_gain->data < mSpec.micIdxMin )
            {
                ALOGW("error, ul_gain = %d out of bound, device %d", *(int*)param_ul_gain->data, device);
            }
            _gainTable->micGain[micType][device].gain= *(int*)param_ul_gain->data;
        }

        // Downlink gain
        // convert xml param to gain table
        Param *param_dl_gain;
        param_dl_gain = paramUnitGetParamByName(paramUnit, paramDlName);
        if (!param_dl_gain)
        {
            ALOGW("error: get param_dl_gain fail, param name = %s", paramDlName);
            continue;
        }

        short* shortArray = (short*)param_dl_gain->data;
        int arraySize = param_dl_gain->arraySize;
        if (arraySize > GAIN_VOL_INDEX_SIZE)
        {
            ALOGW("error, param->arraySize %d exceed digital array size %d", arraySize, GAIN_VOL_INDEX_SIZE);
            arraySize = GAIN_VOL_INDEX_SIZE;
        }

        if (mMapDlDigital[device].size() == 0 ||
            mMapDlAnalog[device].size() == 0 ||
            mMapDlDigital[device].size() != mMapDlAnalog[device].size())
        {
            ALOGE("error, digi & analog map size = %d & %d", mMapDlDigital[device].size(),
                                                             mMapDlAnalog[device].size());
            continue;
        }

        for (int i = 0; i < arraySize; i++)
        {
            short digital = mMapDlDigital[device][shortArray[i]];
            short analog = mMapDlAnalog[device][shortArray[i]];

            // set digital gain
            // convert 0~-64 dB to 0~255
            if (digital > mSpec.digiDbMax)
            {
                ALOGW("error, param out of range, val %d > %d", digital, mSpec.digiDbMax);
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].digital= 0;
            }
            else if (digital <= mSpec.digiDbMin)
            {
                ALOGW("error, param out of range, val %d <= %d", digital, mSpec.digiDbMin);
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].digital = mSpec.keyVolumeStep;
            }
            else
            {
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].digital = (digital * -1 * mSpec.keyStepPerDb);
            }

            // set analog gain
            if (mMapDlAnalogType[device] == GAIN_ANA_SPEAKER)
            {
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].analog[mMapDlAnalogType[device]]= spkGainDb2Idx(analog);
            }
            else
            {
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].analog[mMapDlAnalogType[device]] = bufferGainDb2Idx(analog);
            }

            ALOGD("\tvol_idx=%d, dl_gain_idx = %d, digital = %d, analog = %d\n",
                i,
                shortArray[i],
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].digital,
                _gainTable->streamGain[AUDIO_STREAM_VOICE_CALL][device][i].analog[mMapDlAnalogType[device]]);
            ALOGD("\tvol_idx=%d, dl_gain_idx = %d, digitaldB = %d, analogdB = %d\n", i, shortArray[i], digital, analog);

        }
    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;
}

status_t GainTableParamParser::loadGainTableParam()
{
    loadGainTableSpec();
    loadGainTableMapDl();
    return NO_ERROR;
}

status_t GainTableParamParser::loadGainTableSpec()
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    char audioTypeName[] = VOLUME_AUDIOTYPE_NAME;

    // extract parameters from xml
    AudioType *audioType;
    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    std::string paramPath = "VolumeParam,Common";

    ParamUnit *paramUnit;
    paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
    if (!paramUnit)
    {
        ALOGW("error: get paramUnit fail, paramPath = %s", paramPath.c_str());
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    // spec
    getParam<int>(paramUnit, &mSpec.keyStepPerDb, "step_per_db");
    ALOGD("mSpec.keyStepPerDb = %d", mSpec.keyStepPerDb);
    getParam<float>(paramUnit, &mSpec.keyDbPerStep, "db_per_step");
    ALOGD("mSpec.keyDbPerStep = %f", mSpec.keyDbPerStep);
    getParam<float>(paramUnit, &mSpec.keyVolumeStep, "volume_step");
    ALOGD("mSpec.keyVolumeStep = %f", mSpec.keyVolumeStep);

    getParam<int>(paramUnit, &mSpec.digiDbMax, "play_digi_range_max");
    ALOGD("mSpec.digiDbMax = %d", mSpec.digiDbMax);
    getParam<int>(paramUnit, &mSpec.digiDbMin, "play_digi_range_min");
    ALOGD("mSpec.digiDbMin = %d", mSpec.digiDbMin);
    getParam<int>(paramUnit, &mSpec.sidetoneIdxMax, "stf_idx_range_max");
    ALOGD("mSpec.sidetoneIdxMax = %d", mSpec.sidetoneIdxMax);
    getParam<int>(paramUnit, &mSpec.sidetoneIdxMin, "stf_idx_range_min");
    ALOGD("mSpec.sidetoneIdxMin = %d", mSpec.sidetoneIdxMin);
    getParam<int>(paramUnit, &mSpec.micIdxMax, "mic_idx_range_max");
    ALOGD("mSpec.micIdxMax = %d", mSpec.micIdxMax);
    getParam<int>(paramUnit, &mSpec.micIdxMin, "mic_idx_range_min");
    ALOGD("mSpec.micIdxMin = %d", mSpec.micIdxMin);

    getParam<int>(paramUnit, &mSpec.ulGainOffset, "ul_gain_offset");
    ALOGD("mSpec.ulGainOffset = %d", mSpec.ulGainOffset);
    getParam<int>(paramUnit, &mSpec.ulPgaGainMapMax, "ul_pga_gain_map_max");
    ALOGD("mSpec.ulPgaGainMapMax = %d", mSpec.ulPgaGainMapMax);
    getParam<int>(paramUnit, &mSpec.ulHwPgaIdxMax, "ul_hw_pga_max_idx");
    ALOGD("mSpec.ulHwPgaIdxMax = %d", mSpec.ulHwPgaIdxMax);

    // buffer gain spec
    getParamVector<short>(paramUnit, &mSpec.bufferGainDb, "buffer_gain_db");
    getParamVector<short>(paramUnit, &mSpec.bufferGainIdx, "buffer_gain_idx");
    getParamVector(paramUnit, &mSpec.bufferGainString, "buffer_gain_string");
    getParam<int>(paramUnit, &mSpec.bufferGainPreferMaxIdx, "buffer_gain_prefer_max_idx");
    ALOGD("mSpec.bufferGainPreferMaxIdx = %d", mSpec.bufferGainPreferMaxIdx);

    size_t db_size = mSpec.bufferGainDb.size();
    size_t idx_size = mSpec.bufferGainIdx.size();
    size_t str_size = mSpec.bufferGainString.size();

    if (db_size != idx_size || db_size != str_size)
    {
        ALOGW("warn: db & idx & str_size mapping array size is not the same, db.size()=%d, idx.size()=%d, str_size()=%d",
            db_size,
            idx_size,
            str_size);
    }

    mSpec.numBufferGainLevel = (db_size <= idx_size) ? db_size : idx_size;

    for(unsigned int i = 0; i < mSpec.numBufferGainLevel; i++)
    {
        ALOGD("buffer, db = %d, idx = %d", mSpec.bufferGainDb[i], mSpec.bufferGainIdx[i]);
    }

    // spk gain spec
    getParamVector<short>(paramUnit, &mSpec.spkGainDb, "spk_gain_db");
    getParamVector<short>(paramUnit, &mSpec.spkGainIdx, "spk_gain_idx");
    getParamVector(paramUnit, &mSpec.spkGainString, "spk_gain_string");
    getParam<GAIN_ANA_TYPE>(paramUnit, &mSpec.spkAnaType, "spk_analog_type");
    ALOGD("mSpec.spkAnaType = %d", mSpec.spkAnaType);
    getParam(paramUnit, &mSpec.spkLMixerName,"spk_l_mixer_name");
    ALOGD("mSpec.spkLMixerName = %s", mSpec.spkLMixerName.c_str());
    getParam(paramUnit, &mSpec.spkRMixerName,"spk_r_mixer_name");
    ALOGD("mSpec.spkRMixerName = %s", mSpec.spkRMixerName.c_str());

    db_size = mSpec.spkGainDb.size();
    idx_size = mSpec.spkGainIdx.size();
    str_size = mSpec.spkGainString.size();

    if (db_size != idx_size || db_size != str_size)
    {
        ALOGW("warn: db & idx & str_size mapping array size is not the same, db.size()=%d, idx.size()=%d, str_size()=%d",
            db_size,
            idx_size,
            str_size);
    }

    mSpec.numSpkGainLevel = (db_size <= idx_size) ? db_size : idx_size;

    for(unsigned int i = 0; i < mSpec.numSpkGainLevel; i++)
    {
        ALOGD("spk, db = %d, idx = %d", mSpec.spkGainDb[i], mSpec.spkGainIdx[i]);
    }

    // ul gain map
    getParamVector<short>(paramUnit, &mSpec.swagcGainMap, "swagc_gain_map");
    getParamVector<short>(paramUnit, &mSpec.ulPgaGainMap, "ul_pga_gain_map");
    getParamVector(paramUnit, &mSpec.ulPgaGainString, "ul_pga_gain_string");

    if (mSpec.swagcGainMap.size() != mSpec.micIdxMax + 1 ||
        mSpec.ulPgaGainMap.size() != mSpec.micIdxMax + 1)
    {
        ALOGW("warn: swagcGainMap.size %d, ulPgaGainMap.size %d != micIdxMax %d + 1",
            mSpec.swagcGainMap.size(),
            mSpec.ulPgaGainMap.size(),
            mSpec.micIdxMax);
    }

    // stf gain map
    getParamVector<short>(paramUnit, &mSpec.stfGainMap, "stf_gain_map");
    if (mSpec.stfGainMap.size() != mSpec.sidetoneIdxMax + 1)
    {
        ALOGW("warn: stfGainMap.size %d != sidetoneIdxMax %d + 1",
            mSpec.stfGainMap.size(),
            mSpec.sidetoneIdxMax);
    }

    // headphone impedance related
    getParam<int>(paramUnit, &mSpec.hpImpEnable, "hp_impedance_enable");
    ALOGD("mSpec.hpImpEnable = %d", mSpec.hpImpEnable);
    if (mSpec.hpImpEnable)
    {
        getParam<int>(paramUnit, &mSpec.hpImpDefaultIdx, "hp_impedance_default_idx");
        ALOGD("mSpec.hpImpDefaultIdx = %d", mSpec.hpImpDefaultIdx);
        getParamVector<short>(paramUnit, &mSpec.hpImpThresholdList, "hp_impedance_threshold_list");
        getParamVector<short>(paramUnit, &mSpec.hpImpCompensateList, "hp_impedance_compensate_list");
        ASSERT(mSpec.hpImpThresholdList.size() == mSpec.hpImpCompensateList.size());
        ASSERT(mSpec.hpImpThresholdList.size() != 0);
        ASSERT(mSpec.hpImpDefaultIdx >= 0 && mSpec.hpImpDefaultIdx < mSpec.hpImpThresholdList.size());
    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;
}


status_t GainTableParamParser::loadGainTableMapDl()
{
    ALOGD("%s()", __FUNCTION__);

    // define xml names
    char audioTypeName[] = GAIN_MAP_AUDIOTYPE_NAME;
    char paramTotalName[] = "dl_total_gain";
    char paramDigitalName[] = "dl_digital_gain";
    char paramAnalogName[] = "dl_analog_gain";
    char paramAnaTypeName[] = "dl_analog_type";

    const std::string *profileName = gppDeviceXmlName;

    // extract parameters from xml
    AudioType *audioType;
    audioType = appHandleGetAudioTypeByName(mAppHandle, audioTypeName);
    if (!audioType)
    {
        ALOGW("error: get audioType fail, audioTypeName = %s", audioTypeName);
        return BAD_VALUE;
    }

    // Read lock
    audioTypeReadLock(audioType, __FUNCTION__);

    for (int device = 0; device < NUM_GAIN_DEVICE; device++)
    {
        // get param unit using param path
        std::string paramPath = "Profile," +
                                profileName[device];
        ALOGD("paramPath = %s", paramPath.c_str());

        ParamUnit *paramUnit;
        paramUnit = audioTypeGetParamUnit(audioType, paramPath.c_str());
        if (!paramUnit)
        {
            ALOGW("error: get paramUnit fail, paramPath = %s", paramPath.c_str());
            continue;
        }
        Param *param_total;
        param_total = paramUnitGetParamByName(paramUnit, paramTotalName);
        if (!param_total)
        {
            ALOGW("error: get param_total fail, param_name = %s", paramTotalName);
            continue;
        }

        Param *param_digital;
        param_digital = paramUnitGetParamByName(paramUnit, paramDigitalName);
        if (!param_digital)
        {
            ALOGW("error: get param_digital fail, param_name = %s", paramDigitalName);
            continue;
        }
        Param *param_analog;
        param_analog = paramUnitGetParamByName(paramUnit, paramAnalogName);
        if (!param_analog)
        {
            ALOGW("error: get param_analog fail, param_name = %s", paramAnalogName);
            continue;
        }
        Param *param_ana_type;
        param_ana_type = paramUnitGetParamByName(paramUnit, paramAnaTypeName);
        if (!param_analog)
        {
            ALOGW("error: get param_ana_type fail, param_name = %s", paramAnaTypeName);
            continue;
        }
        mMapDlAnalogType[device] = (GAIN_ANA_TYPE)*(int*)param_ana_type->data;

        if (param_digital->arraySize != param_analog->arraySize)
        {
            ALOGE("error: digi & ana mapping array size is not the same, digi.size()=%d, ana.size()=%d", param_digital->arraySize, param_analog->arraySize);
            continue;
        }

        if (param_total->arraySize != param_digital->arraySize)
        {
            ALOGW("error, total gain && digi & ana array size does not match, total.size()=%d, digi.size()=%d", param_total->arraySize, param_digital->arraySize);
        }

        short *digital_raw = (short*)param_digital->data;
        mMapDlDigital[device].assign(digital_raw, digital_raw + param_digital->arraySize);

        short *analog_raw = (short*)param_analog->data;
        mMapDlAnalog[device].assign(analog_raw, analog_raw + param_analog->arraySize);

        for(unsigned int i = 0; i < mMapDlDigital[device].size(); i++)
        {
            ALOGD("digi = %d, ana = %d", mMapDlDigital[device][i], mMapDlAnalog[device][i]);
        }

    }

    // Unlock
    audioTypeUnlock(audioType);

    return NO_ERROR;

}

/*
 * Utility functions
 */
unsigned int GainTableParamParser::bufferGainDb2Idx(int dB)
{
    for (int i = 0; i < mSpec.numBufferGainLevel; i++)
    {
        if (dB == mSpec.bufferGainDb[i])
            return mSpec.bufferGainIdx[i];
    }

    ALOGW("error, %s(), cannot find corresponding BufferGainIdx, return idx 0, %ddB", __FUNCTION__, mSpec.bufferGainDb[0]);
    return 0;
}

unsigned int GainTableParamParser::spkGainDb2Idx(int dB)
{
    for (int i = 0; i < mSpec.numSpkGainLevel; i++)
    {
        if (dB == mSpec.spkGainDb[i])
            return mSpec.spkGainIdx[i];
    }

    ALOGW("error, %s(), cannot find corresponding BufferGainIdx, return idx 1, %ddB", __FUNCTION__, mSpec.spkGainDb[1]);
    return 1;
}

template<class T>
status_t GainTableParamParser::getParam(ParamUnit *_paramUnit, T *_param, const char *_paramName)
{
    Param *param;
    param = paramUnitGetParamByName(_paramUnit, _paramName);
    if (!param)
    {
        ALOGE("error: get param fail, param_name = %s", _paramName);
        return BAD_VALUE;
    }
    else
    {
        *_param = *(T *)param->data;
    }

    return NO_ERROR;
}

status_t GainTableParamParser::getParam(ParamUnit *_paramUnit, std::string *_param, const char *_paramName)
{
    Param *param;
    param = paramUnitGetParamByName(_paramUnit, _paramName);
    if (!param)
    {
        ALOGE("error: get param fail, param_name = %s", _paramName);
        return BAD_VALUE;
    }
    else
    {
        if (param->paramInfo->dataType == TYPE_STR)
        {
            *_param = (char *)param->data;
        }
        else
        {
            ALOGW("warn, param->paramInfo->dataType %d != TYPE_STR", param->paramInfo->dataType, TYPE_STR);
            return BAD_VALUE;
        }
    }

    return NO_ERROR;
}


template<class T>
status_t GainTableParamParser::getParamVector(ParamUnit *_paramUnit, std::vector<T> *_param, const char *_paramName)
{
    Param *param;
    param = paramUnitGetParamByName(_paramUnit, _paramName);
    if (!param)
    {
        ALOGE("error: get param fail, param_name = %s", _paramName);
        return BAD_VALUE;
    }
    else
    {
        T *raw = (T *)param->data;
        _param->assign(raw, raw + param->arraySize);
    }

    return NO_ERROR;
}

status_t GainTableParamParser::getParamVector(ParamUnit *_paramUnit, std::vector<std::string> *_param, const char *_paramName)
{
    Param *param;
    param = paramUnitGetParamByName(_paramUnit, _paramName);
    if (!param)
    {
        ALOGE("error: get param fail, param_name = %s", _paramName);
        return BAD_VALUE;
    }
    else
    {
        if (param->paramInfo->dataType == TYPE_STR)
        {
            _param->clear();
            std::string raw((char *)param->data);
            ALOGD("%s = %s", _paramName, raw.c_str());

            ASSERT(!raw.empty());

            int pre_pos = -1;
            size_t find_pos = raw.find(',', pre_pos + 1);

            std::string sub_str = raw.substr(pre_pos + 1, find_pos - pre_pos - 1);
            do
            {
                _param->push_back(sub_str);
                ALOGD("\t%s", _param->back().c_str());
                if (find_pos == std::string::npos)
                {
                    break;
                }
                pre_pos = find_pos;
                find_pos = raw.find(',', pre_pos + 1);
                sub_str = raw.substr(pre_pos + 1, find_pos - pre_pos - 1);
            } while (!sub_str.empty());
        }
        else
        {
            ALOGW("warn, param->paramInfo->dataType %d != %d", param->paramInfo->dataType, TYPE_STR);
            return BAD_VALUE;
        }
    }

    return NO_ERROR;
}


}
